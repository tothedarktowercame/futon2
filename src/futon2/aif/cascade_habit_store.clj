(ns futon2.aif.cascade-habit-store
  "Persist warranted cascade reinforcement and feed its habit to selection.
   Counts occupy one entry per distinct policy; no per-tick history is retained."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.cascade-prior :as prior]
            [futon2.aif.scoring-input-receipts :as receipts])
  (:import [java.io RandomAccessFile]
           [java.nio.file Files StandardCopyOption]
           [java.nio.file.attribute FileAttribute]))

(def default-path
  (str (System/getProperty "user.home") "/code/futon2/data/wm-habit/cascade-prior.edn"))

(def selection-basis :first-ranked-sharing-chosen-action)
(defonce ^:private monitor (Object.))

(defn policy-view
  "Map the receipted live representation to the ruled habit identity. Empty
   precedence is a policy; absent precedence is not. Real topology, if present,
   is retained. A flat precedence contributes no invented topology."
  [candidate]
  (when (and (= :cascade-candidate (:kind candidate))
             (map? (:construction-receipt candidate))
             (vector? (:precedence candidate)))
    {:mission (:target candidate)
     :shown (mapv :id (:precedence candidate))
     :semilattice (get candidate :semilattice {})}))

(defn read-snapshot [path]
  (let [file (io/file path)
        bytes (when (.exists file) (Files/readAllBytes (.toPath file)))
        text (when bytes (str (.decode (.newDecoder java.nio.charset.StandardCharsets/UTF_8)
                                      (java.nio.ByteBuffer/wrap bytes))))
        state (prior/coerce-state (when text (edn/read-string text)))
        base-receipt {:status (if bytes :present :absent)
                 :reason (when-not bytes :store-missing)
                 :path (.getAbsolutePath file) :snapshot-edn text
                 :sha256 (when text (receipts/sha text)) :state state}
        receipt (if receipts/*habit-reads*
                    (:receipt
                     (peek (swap! receipts/*habit-reads*
                                  (fn [reads]
                                    (conj reads {:purpose receipts/*habit-read-purpose*
                                                 :receipt (assoc base-receipt :occurrence-index (count reads))})))))
                    base-receipt)]
    {:state state :receipt receipt}))

(defn read-state [path] (:state (read-snapshot path)))

(defn attach-habits
  "Read one snapshot and attach E at the selector's joint-menu boundary.
   Missing identities fall back for the whole menu with an explicit reason;
   malformed stored history still refuses. No fabricated policy is counted."
  [path ranked]
  (let [{:keys [state receipt]}
        (binding [receipts/*habit-read-purpose*
                  (if (= :unspecified receipts/*habit-read-purpose*)
                    :selection-scoring receipts/*habit-read-purpose*)]
          (read-snapshot path))
        views (mapv (comp policy-view :action) ranked)]
    (try
      (let [masses (prior/habit-masses state views)
            keys (mapv prior/policy-key views)
            _ (when receipts/*habit-reads*
                (swap! receipts/*habit-reads* assoc-in
                       [(:occurrence-index receipt) :consumption]
                       {:candidate-ids (mapv :action ranked) :policy-keys keys :masses masses}))]
        (mapv (fn [entry key mass]
                (assoc entry :habit mass
                       :habit-provenance
                       {:source :cascade-prior :policy-key key
                        :count (get (:counts state) key 0)
                        :alpha (:alpha state) :samples (:samples state)
                        :multiplicity (get (frequencies keys) key)
                        :unit :probability-mass}))
              ranked keys masses))
      (catch clojure.lang.ExceptionInfo e
        (if (contains? #{:missing-policy-identity :mixed-pattern-id-types}
                       (get-in (ex-data e) [:refusal :kind]))
          (mapv #(assoc % :habit 1
                        :habit-provenance
                        {:source :neutral-fallback
                         :reason (get-in (ex-data e) [:refusal :kind])
                         :scope :whole-menu}) ranked)
          (throw e))))))

(defn- publish! [file state]
  (let [parent (.toPath (.getParentFile file))
        temporary (Files/createTempFile parent "cascade-prior-" ".edn"
                                        (make-array FileAttribute 0))]
    (try
      (with-open [out (java.io.FileOutputStream. (.toFile temporary))]
        (.write out (.getBytes (str (pr-str state) "\n") "UTF-8"))
        (.sync (.getFD out)))
      (Files/move temporary (.toPath file)
                  (into-array StandardCopyOption
                              [StandardCopyOption/ATOMIC_MOVE
                               StandardCopyOption/REPLACE_EXISTING]))
      (finally (Files/deleteIfExists temporary)))))

(defn- record-policy! [path decision purpose basis-field basis]
  (let [view (policy-view (:action decision))]
    (when-let [key (and view (prior/policy-key view))]
      (locking monitor
        (let [file (.getAbsoluteFile (io/file path))]
          (.mkdirs (.getParentFile file))
          (with-open [lock-file (RandomAccessFile. (str file ".lock") "rw")
                      _lock (.lock (.getChannel lock-file))]
            (let [state (-> (prior/observe-policy
                             (binding [receipts/*habit-read-purpose* purpose]
                               (read-state path)) view)
                            (assoc-in [basis-field key] basis))]
              (publish! file state))))))
    decision))

(defn record-selection!
  "Legacy offline-history writer, retained for historical replay fixtures.
   Live selection must not call this: reinforcement belongs to the close rule."
  ([decision] (record-selection! default-path decision))
  ([path decision]
   (record-policy! path decision :selection-update :selection-bases selection-basis)))

(defn record-reinforcement!
  "Persist one warranted policy observation under the declared close rule.
   The monitor and sidecar lock serialize read/fold/replace across processes.
   Invalid stored state refuses rather than resetting history."
  [path decision rule-id]
  (record-policy! path decision :outcome-reinforcement :reinforcement-bases rule-id))
