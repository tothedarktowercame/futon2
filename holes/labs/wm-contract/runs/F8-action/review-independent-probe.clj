;; :F8 leg 1 slice 9 -- reviewing seat's INDEPENDENT probe of the selected
;; action u, run BEFORE codex-9 reported. Nothing here reads codex-9's Lean or
;; its readback; it calls production `futon2.aif.policy` directly, so the two
;; derivations can be compared rather than one trusted.
(require '[futon2.aif.policy :as policy])

(defn p [label v] (println label "=" (pr-str v)))

;; A three-candidate ranked list in production shape: `rank-actions` sorts by
;; :controller-score ASCENDING, so the head (rank 1) is the LOWEST G.
(defn entry [t g bias]
  {:action {:type t :target (name t)} :controller-score g :habit-prior-bias bias})

(def plain   [(entry :alpha 0.0 0.0) (entry :beta 1.0 0.0) (entry :no-op 5.0 0.0)])
(def biased  [(entry :alpha 0.0 0.0) (entry :beta 1.0 3.0) (entry :no-op 5.0 0.0)])
(def topts   {:tau-mode :selection-gain-only})   ; the live arena default (slice 8)

;; --- 1. three branches of select-action, one shared input ------------------
(def act-plain
  (policy/select-action plain {:temperature-opts topts :selection-gain 1.0}))
(def act-biased
  (policy/select-action biased {:temperature-opts topts :selection-gain 1.0}))
(def strat-head
  (policy/select-action biased {:temperature-opts topts :selection-gain 1.0
                                :selection-boundary :strategic-recommendation
                                :selection-law :controller-head}))
(p ":actuation, no priors        -> action" (:action act-plain))
(p ":actuation, habit prior on beta -> action" (:action act-biased))
(p ":strategic :controller-head  -> action" (:action strat-head))
(p "actuation-priors differs from actuation-plain ?"
   (not= (:action act-biased) (:action act-plain)))
(p "strategic head equals actuation-plain head ?"
   (= (:action strat-head) (:action act-plain)))

;; --- 2. the default law never reads the posterior it records ---------------
(p "default-selection-law" policy/default-selection-law)
(p "selection-laws (closed set)" (sort policy/selection-laws))
(def f-pi-opts {:f-pi-policy-posterior? true
                :f-pi-values [0.0 0.0 0.0]
                :f-pi-scaling :unscaled})
(def strat-full
  (policy/select-action biased {:temperature-opts topts :selection-gain 1.0
                                :selection-boundary :strategic-recommendation
                                :selection-law :full-score-posterior
                                :f-pi-opts f-pi-opts}))
(p ":strategic :full-score-posterior -> action" (:action strat-full))
(p "head law and posterior law DISAGREE on this input ?"
   (not= (:action strat-head) (:action strat-full)))
(p "head law's recorded :softmax-weights" (:softmax-weights strat-head))

;; --- 3. argmax of Q equals argmax of the score vector ----------------------
(def g-totals (mapv :controller-score biased))
(def log-priors (mapv :habit-prior-bias biased))
(def tau 1.0)
(def scores (policy/selection-scores g-totals tau log-priors {}))
(def weights (policy/softmax-weights g-totals tau log-priors {}))
(defn argmax-idx [v] (first (apply max-key (fn [[_ x]] (double x)) (map-indexed vector v))))
(p "selection-scores" scores)
(p "softmax-weights (Q)" weights)
(p "argmax index of scores" (argmax-idx scores))
(p "argmax index of Q" (argmax-idx weights))
(p "argmax Q = argmax scores ?" (= (argmax-idx scores) (argmax-idx weights)))

;; --- 4. TWO argmaxes in one file that break ties OPPOSITELY ----------------
(def first-argmax @#'policy/first-argmax)
(def tied [0.0 7.0 7.0 1.0])
(p "first-argmax on tied [0 7 7 1] over all idxs" (first-argmax tied (vec (range 4))))
(p "max-key   on tied [0 7 7 1] over all idxs" (apply max-key tied (range 4)))
(p "the two argmaxes DISAGREE on a tie ?"
   (not= (first-argmax tied (vec (range 4))) (apply max-key tied (range 4))))
;; and the same disagreement reached through select-action: two candidates with
;; equal prior-adjusted score, one branch keeps the first and the other the last.
(def tie-list [(entry :alpha 0.0 0.0) (entry :beta 1.0 1.0) (entry :no-op 9.0 0.0)])
(p "tie scores (-G/tau + lnE)"
   (policy/selection-scores (mapv :controller-score tie-list) 1.0
                            (mapv :habit-prior-bias tie-list) {}))
(p ":actuation-priors on the tie -> action"
   (:action (policy/select-action tie-list {:temperature-opts topts :selection-gain 1.0})))
(p ":strategic :full-score-posterior on the tie -> action"
   (:action (policy/select-action tie-list {:temperature-opts topts :selection-gain 1.0
                                  :selection-boundary :strategic-recommendation
                                  :selection-law :full-score-posterior
                                  :f-pi-opts {:f-pi-policy-posterior? true
                                              :f-pi-values [0.0 0.0 0.0]
                                              :f-pi-scaling :unscaled}})))

;; --- 5. a requested law can silently become the head law -------------------
(def strat-full-no-fpi
  (policy/select-action biased {:temperature-opts topts :selection-gain 1.0
                                :selection-boundary :strategic-recommendation
                                :selection-law :full-score-posterior
                                :f-pi-opts {:f-pi-policy-posterior? false}}))
(p ":full-score-posterior asked, F_pi absent -> action" (:action strat-full-no-fpi))
(p "  ... equals the HEAD law's action ?"
   (= (:action strat-full-no-fpi) (:action strat-head)))
(p "  ... what the record says ran"
   (select-keys strat-full-no-fpi [:selection-law :selection-law-applied
                                   :selection-law-requested :selection-law-fallback]))
(p "  ... full decision keys" (sort (keys strat-full-no-fpi)))

;; --- 6. :no-op is a candidate in one branch and not the other --------------
(def noop-best [(entry :alpha 0.0 0.0) (entry :no-op 1.0 9.0)])
(p "no-op carries the winning prior-adjusted score ?"
   (policy/selection-scores (mapv :controller-score noop-best) 1.0
                            (mapv :habit-prior-bias noop-best) {}))
(p ":actuation-priors with no-op winning -> " 
   (select-keys (policy/select-action noop-best {:temperature-opts topts :selection-gain 1.0})
                [:action :reason]))
(p ":strategic head with no-op winning -> "
   (select-keys (policy/select-action noop-best {:temperature-opts topts :selection-gain 1.0
                                      :selection-boundary :strategic-recommendation})
                [:action :reason]))

;; --- 7. closed sets refuse, they do not default ----------------------------
(p "unknown :selection-law refuses ?"
   (try (policy/select-action plain {:selection-boundary :strategic-recommendation
                                    :selection-law :nonsense})
        :NO-REFUSAL
        (catch Exception e (ex-message e))))
(p ":full-score-posterior on :actuation refuses ?"
   (try (policy/select-action plain {:selection-law :full-score-posterior})
        :NO-REFUSAL
        (catch Exception e (ex-message e))))
(p "empty candidate list ->"
   (select-keys (policy/select-action [] {:temperature-opts topts}) [:action :reason]))
