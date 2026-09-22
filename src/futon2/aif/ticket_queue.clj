(ns futon2.aif.ticket-queue
  "Declared front placement for any ordinary ticket. The declaration orders
   eligibility; it supplies neither candidates nor model quantities."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.interoceptive-store-lock :as store-lock]
            [futon2.aif.ticket-publication-io :as publication])
  (:import [java.time Instant]))

(load-identity/register! *ns* *file*)

(def empty-declaration
  {:schema :wm/ticket-queue-v1
   :placement :front
   :order [:inserted-at :ticket]
   :within-stratum :cascade-selection-posterior
   :entries []})

(defn validate!
  "Reject ambiguous declarations, including duplicate ticket identities.
   Times are Instants, compared chronologically rather than lexically."
  [declaration]
  (when-not
   (and (= (dissoc empty-declaration :entries) (dissoc declaration :entries))
        (vector? (:entries declaration))
        (= (count (:entries declaration)) (count (set (map :ticket (:entries declaration)))))
        (every? (fn [{:keys [ticket inserted-at] :as entry}]
                  (and (= #{:ticket :inserted-at} (set (keys entry)))
                       (string? ticket) (boolean (re-matches #"T-[A-Za-z0-9][A-Za-z0-9._-]*" ticket))
                       (string? inserted-at)
                       (try (Instant/parse inserted-at) true (catch Exception _ false))))
                (:entries declaration)))
    (throw (ex-info "Invalid ticket queue declaration"
                    {:kind :invalid-ticket-queue :declaration declaration})))
  declaration)

;; The live queue is runtime state written by enqueue!, so it lives under the
;; untracked data/ root. resources/wm/ticket-queue.edn stays the versioned empty
;; declaration: writing entries (and a .lock) into a tracked resource dirtied
;; the shared checkout and made every default-reading test depend on live data.
(def live-path "/home/joe/code/futon2/data/wm-ticket-queue/queue.edn")

(defn read-declaration
  "Read one declaration; missing or malformed configuration refuses."
  ([] (read-declaration (let [live (io/file live-path)]
                          (if (.isFile live) live (io/resource "wm/ticket-queue.edn")))))
  ([source]
   (with-open [reader (java.io.PushbackReader. (io/reader source))]
     (let [declaration (edn/read {:eof ::eof} reader)]
       (when-not (= ::eof (edn/read {:eof ::eof} reader))
         (throw (ex-info "Multiple ticket queue forms" {:kind :invalid-ticket-queue})))
       (validate! declaration)))))

(defonce ^:private writer-monitors (atom {}))

(defn enqueue!
  "Append an ordinary ticket once, preserving its original insertion time.
   Conflicting retries refuse. The caller supplies the declaration path;
   a new isolated queue starts from the versioned empty declaration."
  [path entry]
  (validate! (assoc empty-declaration :entries [entry]))
  (let [file (publication/checked-file! path)
        key (.getCanonicalPath file)
        monitor (get (swap! writer-monitors #(if (contains? % key) % (assoc % key (Object.)))) key)]
    (io/make-parents file)
    (locking monitor
      (store-lock/with-lock-at
        (str key ".lock")
        (fn []
          (publication/checked-file! file)
          (let [declaration (if (.exists file) (read-declaration file) empty-declaration)
                existing (some #(when (= (:ticket entry) (:ticket %)) %) (:entries declaration))]
            (when (and existing (not= existing entry))
              (throw (ex-info "Ticket already has a different insertion time"
                              {:kind :ticket-queue-entry-conflict :existing existing :entry entry})))
            (if existing declaration
                (let [updated (validate! (update declaration :entries conj entry))]
                  (publication/publish-text! file (str (pr-str updated) "\n") true)
                  updated))))))))

(defn supported?
  "Selection's existing support constraints, before floating-point underflow."
  [candidate]
  (and (seq (get-in candidate [:id :precedence]))
       (not= :zero-support (:f-status candidate))
       (number? (:g candidate)) (Double/isFinite (double (:g candidate)))))

(defn plan
  "Return nil for an empty declaration, preserving historical decision bytes.
   Refused entries remain recorded and never block the next admitted stratum."
  [declaration candidates refusals]
  (validate! declaration)
  (when (seq (:entries declaration))
    (let [admitted (set (map (comp :target :id) (filter supported? candidates)))
          present (set (map (comp :target :id) candidates))
          entries (mapv (fn [entry]
                          (let [target (:ticket entry)]
                            (if (contains? admitted target)
                              (assoc entry :status :admitted)
                              (assoc entry :status :not-admitted
                                     :refusals (vec (or (seq (filter #(= target (:target %)) refusals))
                                                   [{:target target :kind (if (contains? present target)
                                                                           :no-policy-support
                                                                           :not-in-admitted-candidates)}]))))))
                        (sort-by (juxt #(Instant/parse (:inserted-at %)) :ticket) (:entries declaration)))
          eligible (first (filter #(= :admitted (:status %)) entries))]
      {:schema :wm/ticket-queue-selection-v1
       :declaration declaration
       :entries entries
       :eligible-targets (if eligible [(:ticket eligible)] [])
       :status (if eligible :front-stratum :no-admitted-front-entry)})))
