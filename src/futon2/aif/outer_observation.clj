(ns futon2.aif.outer-observation
  "Build the task-level `R20Step.Observation` from verified Giraffe evidence.

  The serving JVM supplies futon3c's verifier dynamically.  This namespace
  does not duplicate its manifest law and refuses when that authority is not
  available."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.focus-receipt :as focus-receipt]
            [futon2.aif.mission-registry :as mission-registry]
            [futon2.aif.outer-state :as outer-state]
            [futon2.aif.policy-satisfaction :as policy-satisfaction]
            [futon2.aif.wm.construction-inputs :as construction-inputs])
  (:import [java.nio.file Files]
           [java.security MessageDigest]))

(def ^:dynamic *resolve-var* requiring-resolve)

(defn- refuse! [kind data]
  (throw (ex-info "Outer observation refused" (assoc data :kind kind))))

(defn- sha256 [^bytes bytes]
  (let [digest (.digest (doto (MessageDigest/getInstance "SHA-256")
                          (.update bytes)))]
    (apply str (map #(format "%02x" (bit-and 255 %)) digest))))

(defn- read-edn-file [file kind]
  (try
    (edn/read-string (slurp file))
    (catch Exception e
      (refuse! kind {:path (str file) :message (.getMessage e)}))))

(defn- verifier []
  (try
    (or (*resolve-var* 'futon3c.giraffe.run/verify-run)
        (refuse! :giraffe-verifier-unavailable {}))
    (catch clojure.lang.ExceptionInfo e (throw e))
    (catch Exception e
      (refuse! :giraffe-verifier-unavailable {:message (.getMessage e)}))))

(defn- target-authority []
  (let [loaded-tickets (mission-registry/load-tickets)
        loaded-excursions (mission-registry/load-excursions)]
    {:loaded-missions (mission-registry/load-missions)
     :loaded-tickets loaded-tickets
     :loaded-excursions loaded-excursions
     :ticket-targets (mapv :id (:tickets loaded-tickets))
     :excursion-targets (mapv :id (:excursions loaded-excursions))}))

(defn- registry-entries [{:keys [loaded-missions loaded-tickets loaded-excursions]}]
  (into {}
        (map (juxt :id identity))
        (concat (:missions loaded-missions)
                (:tickets loaded-tickets)
                (:excursions loaded-excursions))))

(defn- excluded-candidate [target declaration registry-entry]
  (cond-> {:target target
           :reason (or (:status-class registry-entry)
                       (:source-absent declaration)
                       :source-unavailable)}
    (:status-line registry-entry) (assoc :status-line (:status-line registry-entry))))

(defn- declaration-sha [declaration]
  (or (:source-sha256 declaration)
      (when-let [path (:source-path declaration)]
        (let [file (io/file path)]
          (when (.isFile file)
            (sha256 (Files/readAllBytes (.toPath file))))))))

(defn giraffe-field
  "Build Lean `Observation.field` and `Observation.facts` from a verified run.

  Returns `{:field ... :facts ... :giraffe/run-id ...
  :giraffe/manifest-sha256 ... :outer-field/excluded ...}`. Candidate reach
  is the makeup's explicit `:default-reach`, because task reach is not yet
  produced by the runtime. `target-source-declarations` is the shared inner
  resolver authority for M-, E-, T-, and explicitly declared target kinds."
  ([run-dir] (giraffe-field run-dir (outer-state/load-makeup)))
  ([run-dir makeup] (giraffe-field run-dir makeup (target-authority)))
  ([run-dir makeup authority]
   (let [dir (io/file run-dir)
         verification ((verifier) dir)]
     (when-not (:ok? verification)
       (refuse! :giraffe-run-unverified
                {:run-dir (.getPath dir) :verification verification}))
     (let [manifest-file (io/file dir "manifest.edn")
           pass-file (io/file dir "pass.edn")]
       (when-not (and (.isFile manifest-file) (.isFile pass-file))
         (refuse! :giraffe-run-incomplete {:run-dir (.getPath dir)}))
       (let [manifest-bytes (Files/readAllBytes (.toPath manifest-file))
             manifest (read-edn-file manifest-file :giraffe-manifest-malformed)
             pass (read-edn-file pass-file :giraffe-pass-malformed)
             rows (:order pass)]
         (when-not (and (vector? rows)
                        (every? #(and (:id %) (contains? % :status)
                                      (or (and (nat-int? (:unblocks %)) (nat-int? (:cone %)))
                                          (and (= :unknown (get-in % [:graph-terms :status]))
                                               (nil? (:cone %)) (nil? (:unblocks %))))) rows))
           (refuse! :giraffe-pass-invalid {:run-dir (.getPath dir)}))
         (let [targets (mapv (comp name :id) rows)
               declarations (construction-inputs/target-source-declarations targets authority)
               registry-by-id (registry-entries authority)
               entries (mapv (fn [{:keys [id status unblocks cone graph-terms completion dependency-reading]} declaration]
                               (let [target (name id)]
                                 {:candidate {:target target
                                              :reach (:default-reach makeup)}
                                  :target target
                                  :declaration declaration
                                  :fact {:status status
                                         :unblocks unblocks
                                         :cone cone :graph-terms graph-terms
                                         :completion completion :dependency-reading dependency-reading}}))
                             rows declarations)
               available (filterv #(nil? (get-in % [:declaration :source-absent])) entries)
               satisfied (into {}
                               (keep (fn [{:keys [target declaration]}]
                                       (when-let [exclusion
                                                  (policy-satisfaction/all-satisfied-exclusion
                                                   nil target (declaration-sha declaration))]
                                         [target exclusion])))
                               available)
               admitted (filterv #(nil? (get satisfied (:target %))) available)
               excluded (into (vec (vals satisfied))
                              (comp (remove #(nil? (get-in % [:declaration :source-absent])))
                                    (map (fn [{:keys [target declaration]}]
                                           (excluded-candidate target declaration
                                                               (get registry-by-id target)))))
                              entries)
               ;; Q1-ENUMERATION-DUPLICATE-DROP (2026-10-31): a ticket can be
               ;; open in the registry while the retained verified Giraffe
               ;; field predates it (T-repair-ea1-19929445...attempt-002 was
               ;; minted 2026-10-31 01:13Z; the field the click consumed was
               ;; the 2026-10-30 21:39Z run).  Such a ticket is not a row in
               ;; the pass, so before this fix it vanished from the outer
               ;; population with no typed account while the census counted
               ;; it (Q1 read 608/609).  Every registry-open ticket the field
               ;; never saw is now carried as an explicit exclusion: the
               ;; selector did not admit it, and the record names why.  No
               ;; graph facts are invented for it.
               field-targets (set (map :target entries))
               unseen-open-tickets (->> (get-in authority [:loaded-tickets :tickets])
                                        (filter mission-registry/live-ticket?)
                                        (remove #(contains? field-targets (:id %))))
               absent-excluded (mapv (fn [ticket]
                                       {:target (:id ticket)
                                        :reason :registry-open-not-in-giraffe-field
                                        :status-line (:status-line ticket)
                                        :registry-path (:path ticket)})
                                     unseen-open-tickets)]
           {:field (mapv :candidate admitted)
            :facts (into {} (map (juxt :target :fact)) admitted)
            :outer-field/excluded (vec (concat excluded absent-excluded))
            :giraffe/run-id (:run-id manifest)
            :giraffe/manifest-sha256 (sha256 manifest-bytes)}))))))

(defn outer-observation
  "Build the input read by `outer-score`, aligned with Lean `R20Step.Observation`.

  Required keys are `:run-dir`, `:makeup`, `:focus-inputs`, and
  `:decision-as-of`.  Optional `:previous-focus` and `:relation-context` are
  passed to the shared focus producer.  Unknown relations remain `:unknown`."
  [{:keys [run-dir makeup focus-inputs decision-as-of previous-focus relation-context]}]
  (let [giraffe (giraffe-field run-dir makeup)
        focus-result (focus-receipt/decision-focus focus-inputs decision-as-of previous-focus)
        focus-info (:info focus-result)
        relation (fn [focus target]
                   (:class (focus-receipt/classify-target
                            focus-inputs
                            (assoc focus-info :focus focus)
                            decision-as-of target relation-context)))]
    (merge giraffe
           {:focus (:focus focus-info)
            :relation relation
            :focus/source (select-keys focus-info
                                       [:status :focus-origin :as-of
                                        :retained-evidence-as-of])})))
