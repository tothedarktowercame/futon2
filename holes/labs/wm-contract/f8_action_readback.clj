;; F8 leg 1 slice 9: production selected-action readback.
;;
;; PROVENANCE RULE: EVERY expected value below is transcribed from a NAMED Lean
;; theorem, not derived a second time from the production inputs -- otherwise a
;; delta of 0.0 would only say that Clojure agrees with Clojure.
;;
;; Every DISAGREEMENT is measured on BOTH arms. A one-sided line named after a
;; two-sided theorem measures nothing (review finding, this slice).
;;
;; Lean theorem (MachineAction / ...Witness)     expected here
;; defaultRuleIsNotTheRegistryRule               default law /= :full-score-posterior
;; defaultSelectionIsTheHead                     no boundary opt => the G-head
;; controllerHeadReference                       head law -> :a
;; posteriorReference                            posterior law -> :b
;; posteriorFallbackReference                    posterior law, no F_pi -> :a
;; requestedPosteriorCanBecomeHead               ... and the record NAMES the fallback
;; requestedPosteriorDependsOnFPiEntered         the two F_pi cases differ
;; tiedArgmaxesDisagree                          tie: first-max :a, last-max :b
;; habitPriorAloneMovesTheChoice                 strict max: no prior :a, prior :b
;; noOpExclusionMovesTheChoice                   no-op wins: strategic :a
;; noOpArgmaxAbstains                            ... actuation abstains instead
;; selectionScore / fullScoreIsPosteriorArgmax   scores 0.0 and 2.0
;; posteriorOrderIsScoreOrder                    argmax Q = argmax scores
(require '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[futon2.aif.policy :as policy])

(def temperature {:tau-mode :selection-gain-only})   ; the live arena default (C526)
(def out (atom []))
(defn line [theorem label expected actual]
  (swap! out conj
         (format "%-38s %-30s expected %-26s actual %-26s %s"
                 theorem label (pr-str expected) (pr-str actual)
                 (if (= expected actual) "MATCH" "MISMATCH"))))
(defn note [theorem label text] (swap! out conj (format "%-38s %-30s %s" theorem label text)))
(defn act [r] (get-in r [:action :type] (:action r)))

;; --- the production defaults ----------------------------------------------
(line "defaultRuleIsNotTheRegistryRule" "default-selection-law"
      :controller-head policy/default-selection-law)
(line "defaultRuleIsNotTheRegistryRule" "default /= registry law"
      true (not= :full-score-posterior policy/default-selection-law))
(line "defaultRuleIsNotTheRegistryRule" "selection-laws (closed set)"
      [:controller-head :full-score-posterior] (vec (sort policy/selection-laws)))

;; --- the strategic fixture: alpha G 0 lnE 0, beta G 1 lnE 3 ---------------
(def ranked [{:rank 1 :action {:type :a} :controller-score 0.0 :habit-prior-bias 0.0}
             {:rank 2 :action {:type :b} :controller-score 1.0 :habit-prior-bias 3.0}])
(def plain (mapv #(assoc % :habit-prior-bias 0.0) ranked))
(defn strategic [law entered?]
  (policy/select-action
   ranked {:selection-boundary :strategic-recommendation
           :selection-law law :selection-gain 1.0
           :temperature-opts temperature
           :f-pi-opts (if entered?
                        {:f-pi-policy-posterior? true
                         :f-pi-values [0.0 0.0] :f-pi-scaling :unscaled}
                        {:f-pi-policy-posterior? false})}))
(def head      (strategic :controller-head true))
(def posterior (strategic :full-score-posterior true))
(def fallback  (strategic :full-score-posterior false))

(line "defaultSelectionIsTheHead" "no boundary opt, no prior -> head"
      :a (act (policy/select-action plain {:temperature-opts temperature})))
(line "controllerHeadReference" "head law -> action" :a (act head))
(line "posteriorReference" "posterior law -> action" :b (act posterior))
(line "headAndPosteriorDisagree" "the two laws differ" true (not= (act head) (act posterior)))
(line "posteriorFallbackReference" "posterior law, no F_pi" :a (act fallback))
(line "requestedPosteriorDependsOnFPiEntered" "F_pi in vs out differ"
      true (not= (act fallback) (act posterior)))
(line "requestedPosteriorCanBecomeHead" "the record NAMES the fallback"
      {:requested :full-score-posterior :applied :controller-head
       :reason :no-f-pi-opts :effect :fell-back-to-controller-head}
      (let [sl (:selection-law fallback)]
        {:requested (:requested sl) :applied (:applied sl)
         :reason (get-in sl [:refusal :reason])
         :effect (get-in sl [:refusal :effect])}))

;; --- the score vector and its posterior -----------------------------------
(def g-totals (mapv :controller-score ranked))
(def log-priors (mapv :habit-prior-bias ranked))
(def scores (policy/selection-scores g-totals 1.0 log-priors {}))
(def weights (policy/softmax-weights g-totals 1.0 log-priors {}))
(defn argmax-idx [v] (first (apply max-key (fn [[_ x]] (double x)) (map-indexed vector v))))
(line "selectionScore" "lnE - G/tau - F_pi at tau=1" [0.0 2.0] (vec scores))
(line "posteriorOrderIsScoreOrder" "argmax Q = argmax scores"
      (argmax-idx scores) (argmax-idx weights))
(line "fullScoreIsPosteriorArgmax" "score 0 < score 1 and Q 0 < Q 1"
      [true true] [(< (nth scores 0) (nth scores 1)) (< (nth weights 0) (nth weights 1))])

;; --- BOTH arms of the tie --------------------------------------------------
(def tied [{:rank 1 :action {:type :a} :controller-score 0.0 :habit-prior-bias 0.0}
           {:rank 2 :action {:type :b} :controller-score 1.0 :habit-prior-bias 1.0}])
(line "tiedArgmaxesDisagree" "the scores really tie"
      [0.0 0.0] (vec (policy/selection-scores (mapv :controller-score tied) 1.0
                                              (mapv :habit-prior-bias tied) {})))
(line "tiedArgmaxesDisagree" "first-max arm (strategic)"
      :a (act (policy/select-action
               tied {:selection-boundary :strategic-recommendation
                     :selection-law :full-score-posterior :selection-gain 1.0
                     :temperature-opts temperature
                     :f-pi-opts {:f-pi-policy-posterior? true
                                 :f-pi-values [0.0 0.0] :f-pi-scaling :unscaled}})))
(line "tiedArgmaxesDisagree" "last-max arm (actuation)"
      :b (act (policy/select-action tied {:selection-boundary :actuation
                                          :selection-gain 1.0
                                          :temperature-opts temperature})))

;; --- BOTH arms of the habit prior, with a STRICT maximum -------------------
(line "habitPriorAloneMovesTheChoice" "all priors zero"
      :a (act (policy/select-action plain {:selection-boundary :actuation
                                           :selection-gain 1.0
                                           :temperature-opts temperature})))
(line "habitPriorAloneMovesTheChoice" "one prior nonzero"
      :b (act (policy/select-action ranked {:selection-boundary :actuation
                                            :selection-gain 1.0
                                            :temperature-opts temperature})))

;; --- BOTH arms of the :no-op exclusion -------------------------------------
(def with-no-op [{:rank 1 :action {:type :a} :controller-score 0.0 :habit-prior-bias 0.0}
                 {:rank 2 :action {:type :no-op} :controller-score 1.0 :habit-prior-bias 9.0}])
(line "noOpExclusionMovesTheChoice" "strategic cannot see :no-op"
      :a (act (policy/select-action with-no-op
                                    {:selection-boundary :strategic-recommendation
                                     :selection-gain 1.0
                                     :temperature-opts temperature})))
(line "noOpArgmaxAbstains" "actuation argmax IS :no-op -> abstain"
      [:abstain :no-action-beats-no-op]
      (let [r (policy/select-action with-no-op {:selection-boundary :actuation
                                                :selection-gain 1.0
                                                :temperature-opts temperature})]
        [(act r) (:reason r)]))

;; --- what this readback does NOT measure -----------------------------------
(note "selectedAndEnactedDisagree" "NOT MEASURED"
      (str "close-loop! is the mutating enactment boundary and is not called here; "
           "the first-passing-gate rule is stated from enact.clj:305,320 and "
           "enact.clj contains no reference to the judgement's :action (grep, 0 hits)."))

(let [text (str (str/join "\n" (cons "F8 leg 1 slice 9 -- production selected-action readback" @out)) "\n")
      f (io/file "holes/labs/wm-contract/runs/F8-action/clojure-readback.txt")]
  (io/make-parents f)
  (spit f text)
  (print text)
  (println (format "MISMATCH count: %d" (count (filter #(str/includes? % "MISMATCH") @out)))))
