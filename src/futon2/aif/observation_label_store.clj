(ns futon2.aif.observation-label-store
  "Explicit, durable ownership of the admitted labels and all located subjects seen."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.observation-labels :as labels])
  (:import [java.io FileOutputStream PushbackReader RandomAccessFile StringReader]
           [java.nio.file Files StandardCopyOption]
           [java.nio.file.attribute FileAttribute]
           [java.time Instant]))

(identity/register! *ns* *file*)

(def schema :wm/observation-labels-v1)
(defonce ^:private monitor (Object.))

(defn- fail! [kind data cause]
  (throw (ex-info (name kind) (merge {:status :missing :kind kind} data) cause)))

(defn- file-at [path]
  (when (or (nil? path) (and (string? path) (str/blank? path)))
    (fail! :path-required {} nil))
  (.getCanonicalFile (io/file path)))

(defn- timestamp? [s]
  (and (string? s) (try (Instant/parse s) true (catch Exception _ false))))

(defn- located-key? [k]
  (and (vector? k) (= 6 (count k)) (contains? #{:C3 :C4} (first k))
       (every? #(and (string? %) (not (str/blank? %)))
               (map #(nth k %) [1 2 3]))
       (if (= :C3 (first k)) (nil? (nth k 4)) (string? (nth k 4)))))

(defn- label-key? [k]
  (and (located-key? k) (string? (nth k 5)) (not (str/blank? (nth k 5)))))

(defn- unwitnessed-key? [k]
  ;; Absence is a seen-only identity, never an admitted label mechanism.
  (and (located-key? k) (= labels/unwitnessed-mechanism (nth k 5))))

(defn- envelope? [e]
  (and (map? e) (= schema (:schema e)) (map? (:labels e)) (map? (:seen e))
       (timestamp? (:updated-at e))
       (every? (fn [[k v]]
                 (and (label-key? k) (map? v) (= k (:label-key v))
                      (= (first k) (:token-class v))
                      (contains? #{:present :absent} (:admitted v))
                      (boolean? (:recorded v))
                      (= :admitted (get-in v [:admission :status]))
                      (contains? (:seen e) k))) (:labels e))
       (every? (fn [[k v]]
                 (and (or (label-key? k)
                          (and (unwitnessed-key? k)
                               (= :refused (:last-outcome v))
                               (= :check-mechanism-unwitnessed (:last-refusal v))))
                      (map? v)
                      (timestamp? (:first-seen v)) (timestamp? (:last-seen v))
                      (pos-int? (:times v))
                      (contains? #{:written :skipped :refused} (:last-outcome v))
                      (contains? v :last-refusal)
                      (if (= :refused (:last-outcome v))
                        (keyword? (:last-refusal v))
                        (and (nil? (:last-refusal v)) (contains? (:labels e) k)))))
               (:seen e))))

(defn snapshot
  "One immutable byte read, parsed as exactly one envelope and hashed as read."
  [path]
  (let [file (file-at path)]
    (when-not (.exists file) (fail! :store-uninitialized {:path (str file)} nil))
    (try
      (let [bytes (Files/readAllBytes (.toPath file))
            e (with-open [r (PushbackReader. (StringReader. (String. bytes "UTF-8")))]
                (let [end (Object.) value (edn/read {:eof end} r)]
                  (when-not (and (identical? end (edn/read {:eof end} r)) (envelope? value))
                    (fail! :invalid-label-store {:path (str file)} nil))
                  value))]
        {:envelope e :sha256 (identity/sha256 bytes)})
      (catch Exception e (fail! :invalid-label-store {:path (str file)} e)))))

(defn- publish! [file envelope]
  ;; Copied from cascade-habit-store/publish!: private there, and this owner
  ;; publishes an envelope and returns the digest of these exact bytes.
  (try
    (let [bytes (.getBytes (str (pr-str envelope) "\n") "UTF-8")
          temp (Files/createTempFile (.toPath (.getParentFile file)) "observation-labels-" ".edn"
                                     (make-array FileAttribute 0))]
      (try
        (with-open [out (FileOutputStream. (.toFile temp))]
          (.write out bytes)
          (.sync (.getFD out)))
        (Files/move temp (.toPath file)
                    (into-array StandardCopyOption [StandardCopyOption/ATOMIC_MOVE
                                                    StandardCopyOption/REPLACE_EXISTING]))
        (identity/sha256 bytes)
        (finally (Files/deleteIfExists temp))))
    (catch Exception e (fail! :publish-failed {:path (str file) :cause (ex-message e)} e))))

(defn- locked [file f]
  ;; Same JVM monitor + cross-process sidecar file lock as cascade-habit-store.
  (locking monitor
    (with-open [lock-file (RandomAccessFile. (str file ".lock") "rw")
                _lock (.lock (.getChannel lock-file))]
      (f))))

(defn init!
  "Explicit initialization. Existing stores are validated, never overwritten."
  [path]
  (let [file (file-at path)]
    (io/make-parents file)
    (locked file
            #(if (.exists file)
               (snapshot file)
               (let [e {:schema schema :labels {} :seen {} :updated-at (str (Instant/now))}]
                 {:envelope e :sha256 (publish! file e)})))))

(defn record!
  "Locked read/merge/publish. OPTS is reserved. Located C3/C4 outcomes count as
   seen even when admission refuses. Upstream refusals without a resolved
   subject have no label-key and cannot invent a seen identity. Identity-reader
   refusals pass through unchanged; no population is written in that case."
  [path check-results identities _opts]
  (let [file (file-at path)]
    (if (:status identities)
      identities
      (do
        (when-not (.exists file) (fail! :store-uninitialized {:path (str file)} nil))
        (locked
         file
         (fn []
           (let [before (:envelope (snapshot file))
                 now (str (Instant/now))
                 result
                 (reduce
                  (fn [{:keys [envelope] :as acc} check]
                    (let [r (labels/write-labels [check] (:labels envelope) identities)
                          outcome (cond (seq (:written r)) :written
                                        (seq (:skipped r)) :skipped :else :refused)
                          row (first (get r outcome))
                          key (or (:label-key row) (:key row))
                          e (assoc envelope :labels (:store r))
                          e (if key
                              (assoc-in e [:seen key]
                                        {:first-seen (or (get-in e [:seen key :first-seen]) now)
                                         :last-seen now :times (inc (get-in e [:seen key :times] 0))
                                         :last-outcome outcome :last-refusal (when (= :refused outcome) (:kind row))})
                              e)]
                      (cond-> (-> acc (assoc :envelope e) (update outcome inc))
                        (= :refused outcome)
                        (update :refusals conj {:key key :kind (:kind row)
                                               :reason (or (get-in row [:data :reason]) (:reason row))}))))
                  {:envelope before :written 0 :skipped 0 :refused 0 :refusals []}
                  check-results)
                 e (assoc (:envelope result) :updated-at now)]
             (when-not (envelope? e) (fail! :invalid-label-store {:path (str file)} nil))
             (assoc (dissoc result :envelope)
                    :path (str file) :seen-total (count (:seen e)) :labels-total (count (:labels e))
                    :snapshot-sha256 (publish! file e) :identities identities))))))))
