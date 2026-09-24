(ns futon2.aif.repair-discharge-receipt
  "Deterministic publication derived from immutable resolution-store records.
   The store wins; publication failure never reopens or re-resolves a finding."
  (:refer-clojure :exclude [derive])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.observation-checks :as observation]
            [futon2.aif.repair-discharge-evidence :as evidence]
            [futon2.aif.repair-obligation :as repair])
  (:import [java.nio.file Files StandardOpenOption]))

(def directory "holes/labs/wm-contract/discharges")
(defn receipt-path [id] (str directory "/" (evidence/safe-id! id) ".edn"))

(defn derive
  "Reconstruct exclusively from store records. Legacy resolutions without the
   retained context refuse explicitly; missing provenance is never invented."
  [root id]
  (let [records (into {} (map (fn [[k child]] [k (repair/discharge-record root child id)]))
                      [[:finding "findings"] [:implementation "implementations"] [:resolution "resolutions"]])
        finding (get-in records [:finding :value])
        implementation (get-in records [:implementation :value])
        resolution (get-in records [:resolution :value])
        context (:repair/discharge-context resolution)
        implementation-context (:repair/discharge-context implementation)
        relation (:successor-relation resolution)
        intent (:intent context)]
    (evidence/require! (= :resolved (:repair/status resolution)) :resolution-not-successful {:repair/id id})
    (evidence/require! (and context implementation-context) :resolution-context-unavailable {:repair/id id})
    (evidence/require!
     (and (= :implementation (:repair/phase implementation))
          (= :successor-validation (:repair/phase resolution))
          (= id (:repair/id context) (:repair/id implementation-context) (:repair/id relation))
          (= (:implementation-attempt implementation) (:repair-attempt/id relation)
             (get-in implementation-context [:close :attempt/id]))
          (= (:validation-attempt resolution) (:successor-attempt/id relation)
             (get-in context [:close :attempt/id]))
          (= (:replacement-commit implementation) (:replacement-commit resolution)
             (:repair-commit relation))
          (= (:witness resolution) (:witness context))
          (= (:path intent) (:successor-witness-ref relation))
          (= (:sha256 intent) (:successor-witness-sha256 relation))
          (= (:sha256 intent) (evidence/sha256 (evidence/text-bytes (:edn-text intent))))
          (= (:witness context) (get-in (evidence/read-one (:edn-text intent)) [:value :witness]))
          (not= (:repair-attempt/id relation) (:successor-attempt/id relation))
          (not= (:repair-run/id relation) (:successor-run/id relation)))
     :resolution-binding-invalid {:repair/id id})
    {:schema :wm/repair-discharge-v1 :repair/id id :target (str "T-" id)
     :status :resolved :discharge-contract (:discharge-contract finding)
     :artifact (:artifact implementation-context)
     :implementation {:attempt/id (:repair-attempt/id relation) :run/id (:repair-run/id relation)}
     :validation {:attempt/id (:successor-attempt/id relation) :run/id (:successor-run/id relation)
                  :production-shaped? true :successor-relation relation}
     :review (:review-job context) :artifact-binding (:artifact-binding implementation-context)
     :witness (:witness context)
     :store-records (into {} (map (fn [[k v]] [k (dissoc v :value)])) records)
     :producer (:producer context)}))

(defn verify!
  "Presence is insufficient. Verify the entire derived value and exact embedded
   store bytes before returning a C3 locator."
  [root id text]
  (let [value (evidence/read-one text)]
    (evidence/require! (= (derive root id) value) :receipt-store-mismatch {:repair/id id})
    (doseq [[kind record] (:store-records value)]
      (evidence/require! (and (= id (:repair/id (evidence/read-one (:edn-text record))))
                             (= (:sha256 record) (evidence/sha256 (evidence/text-bytes (:edn-text record)))))
                        :receipt-record-hash-mismatch {:kind kind}))
    value))

(defn- git-value [repo & args] (str/trim (apply evidence/git! repo args)))

(defn- published [repo commit relative text id]
  (let [repo-name (str (.relativize (.toPath (.getCanonicalFile (io/file observation/repo-root)))
                                   (.toPath (.getCanonicalFile (io/file repo)))))]
    {:status :receipt-committed :repair/id id :repair/discharged? true
     :receipt {:repo repo :sha commit :path relative :sha256 (evidence/sha256 (evidence/text-bytes text))}
     :c3-locator {:class :C3 :repo repo-name :sha commit :path relative}}))

(defn publish!
  "Commit only the derived success receipt. git commit --only uses Git's
   temporary commit index and ref compare-and-swap; unrelated staged entries
   stay staged. Never amend, reset, stash, or force-update a ref. A conflict
   remains a typed, retryable publication refusal."
  ([root repo id] (publish! root repo id nil))
  ([root repo id expected-head]
   (let [relative (receipt-path id)
         text (evidence/canonical-text (derive root id))
         file (io/file repo relative)
         head (git-value repo "rev-parse" "HEAD")
         canonical-root (.getCanonicalFile (io/file repo))]
     (when expected-head
       (evidence/require! (= expected-head head) :publication-head-moved {:expected expected-head :actual head}))
     (evidence/require! (.startsWith (.toPath (.getCanonicalFile file)) (.toPath canonical-root))
                       :publication-path-outside-repository {:path relative})
     (verify! root id text)
     ;; An already committed receipt is a stable no-op, including crash recovery
     ;; after commit but before the terminal run record was written.
     (let [existing (try (evidence/git! repo "show" (str head ":" relative)) (catch Exception _ nil))]
       (if existing
         (do (evidence/require! (= text existing) :committed-receipt-conflict {:path relative})
             (published repo head relative text id))
         (do
           (when-not (str/blank? (evidence/git! repo "diff" "--cached" "--name-only" "--" relative))
             (evidence/require! (= text (evidence/git! repo "show" (str ":" relative)))
                               :receipt-index-conflict {:path relative}))
           (io/make-parents file)
           (if (.exists file)
             (evidence/require! (= text (slurp file)) :receipt-worktree-conflict {:path relative})
             (Files/write (.toPath file) (evidence/text-bytes text)
                          (into-array StandardOpenOption [StandardOpenOption/CREATE_NEW StandardOpenOption/WRITE])))
           ;; This exact path is the whole write capability. Git's own locks
           ;; arbitrate other index writers, rather than a private lock they
           ;; would not observe.
           (evidence/git! repo "add" "--" relative)
           (evidence/git! repo "commit" "--only" "-m" (str "Publish repair discharge " id) "--" relative)
           (let [commit (git-value repo "rev-parse" "HEAD")
                 readback (evidence/git! repo "show" (str commit ":" relative))]
             (evidence/require! (= text readback) :committed-receipt-readback-mismatch {:path relative})
             (verify! root id readback)
             (published repo commit relative text id))))))))

(defn publication-result! [root repo id]
  ;; Retry publication only, never the authoritative store transition.
  ;; H-PUBLISH-A2: a :wm/publication-unreachable-v1 marker does NOT suppress
  ;; publication — derive runs first; when it succeeds the receipt publishes
  ;; and the marker is reported :superseded (the disposition is reversible).
  ;; Only when publication still refuses does the marker speak, reporting
  ;; :publication-unreachable with its class and ground instead of the raw
  ;; refusal the tick would otherwise re-derive forever.
  (let [marker (repair/publication-unreachable-marker root id)]
    (loop [tries 2]
      (let [result (try (publish! root repo id)
                        (catch Exception e
                          (if marker
                            {:status :publication-unreachable :repair/id id
                             :repair/discharged? false
                             :class (:class marker) :ground (:ground marker)
                             :reason (:reason marker)}
                            {:status :publication-refused :repair/id id :repair/discharged? false
                             :reason (or (:repair-discharge/refusal (ex-data e)) :publication-error)
                             :error (.getMessage e) :error-data-edn (pr-str (ex-data e))})))]
        (cond (and (= :publication-refused (:status result)) (> tries 1))
              (recur (dec tries))

              (and marker (= :receipt-committed (:status result)))
              (assoc result :marker :superseded :marker-class (:class marker)
                     :store/status
                     (try (get-in (repair/discharge-record root "resolutions" id) [:value :repair/status])
                          (catch Exception _ :unavailable)))

              :else
              (assoc result :store/status
                     (try (get-in (repair/discharge-record root "resolutions" id) [:value :repair/status])
                          (catch Exception _ :unavailable))))))))

(defn catch-up!
  "Called at tick start independently of the T queue. Invalid/legacy records
   are reported individually; one cannot hide the remaining publication work."
  [root repo]
  (try
    (mapv #(publication-result! root repo %) (repair/discharge-resolution-ids root))
    (catch Exception e
      [{:status :publication-refused :reason :resolution-scan-unavailable
        :error (.getMessage e) :error-data-edn (pr-str (ex-data e))}])))
