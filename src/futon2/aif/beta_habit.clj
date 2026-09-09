(ns futon2.aif.beta-habit
  "RUN4's ruled habit-in-both adapter at the existing beta carry boundary.
   This neither promotes strategic E nor changes a selector's tie rule."
  (:require [clojure.edn :as edn]
            [futon2.aif.policy-precision :as precision]))

(defn run-sheet []
  (when-let [path (System/getenv "FUTON_WM_RUN_CONFIG")]
    (edn/read-string (slurp path))))

(defn enabled?
  "Explicit false wins over the sheet and environment. No default flip."
  ([opts] (enabled? opts run-sheet (System/getenv "FUTON_WM_BETA_HABIT_IN_BOTH")))
  ([opts read-sheet env]
   (let [value (if (contains? opts :beta-habit-in-both?)
                 (:beta-habit-in-both? opts)
                 (let [sheet (read-sheet)]
                   (if (contains? sheet :beta-habit-in-both?)
                     (:beta-habit-in-both? sheet)
                     (= "1" env))))]
     (when-not (boolean? value)
       (throw (ex-info "beta habit opt must be boolean"
                       {:error :invalid-beta-habit-opt :value value})))
     value)))

(defn preconditions!
  "An opted-in beta path requires its existing producer/persistence chain."
  [enabled f-pi? beta? details?]
  (when enabled
    (let [missing (cond-> []
                    (not f-pi?) (conj "FUTON_WM_FPI_DARK=1")
                    (not beta?) (conj "FUTON_WM_BETA_DARK=1")
                    (not details?) (conj "FUTON_WM_TRACE_POLICY_DETAILS=1"))]
      (when (seq missing)
        (throw (ex-info "Habit-in-both requires the beta evidence chain"
                        {:error :beta-habit-producer-missing :missing missing}))))))

(defn carry
  "Attach ln E using the carry's exact identity-resolved candidate subset.
   No missing bias is treated as zero. Persist the arm and actual bias sources
   even when the solver holds; the existing convergence/hold law is unchanged."
  [previous by-candidate ranked {:keys [identity-fn score-fn] :as opts}]
  (let [aligned (precision/align-f-pi-and-g by-candidate ranked identity-fn
                                           (or score-fn :controller-score))
        by-identity (group-by identity-fn ranked)
        entries (mapv (fn [id]
                        (first (get by-identity
                                    (:candidate-identity (get by-candidate id)))))
                      (:candidate-ids aligned))
        evidence (mapv (fn [id entry]
                         (let [bias (:habit-prior-bias entry)
                               source (:habit-prior-source entry)]
                           (when-not (and (number? bias)
                                          (Double/isFinite (double bias)) source)
                             (throw (ex-info "Aligned candidate lacks finite sourced ln E"
                                             {:error :missing-sourced-beta-habit
                                              :candidate-id id})))
                           {:candidate-id id :identity (identity-fn entry)
                            :ln-e (double bias) :source source}))
                       (:candidate-ids aligned) entries)
        state (precision/carry-beta previous by-candidate ranked
                                    (assoc opts :log-prior-placement :both
                                           :log-priors (mapv :ln-e evidence)))]
    (assoc state :habit-provenance
           {:arm :habit-prior-in-both :placement :both
            :boundary :policy-precision-beta-carry
            :source :ranked-candidate-habit-prior
            :candidates evidence
            :scope :scheduler-candidate-field
            :strategic-prior-promoted? false})))
