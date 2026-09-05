;; F8 leg 1 slice 9: production selected-action readback.
;;
;; PROVENANCE RULE: EVERY expected value below is transcribed from a NAMED Lean
;; theorem, not derived a second time from the production inputs -- otherwise a
;; delta of 0.0 would only say that Clojure agrees with Clojure.
;;
;; Lean theorem                                   expected
;; controllerHeadReference                       :a
;; posteriorReference                            :b
;; posteriorFallbackReference                    :a
;; tiedArgmaxesDisagree                           first :a, last :b
;; noOpCandidateSetsDiffer                        strategic excludes :no-op
(require '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[futon2.aif.policy :as policy])

(def ranked [{:rank 1 :action {:type :a} :controller-score 0.0}
             {:rank 2 :action {:type :b} :controller-score 1.0}])
(def temperature {:tau-mode :selection-gain-only})
(defn strategic [law entered?]
  (policy/select-action
   ranked {:selection-boundary :strategic-recommendation
           :selection-law law :selection-gain 1.0
           :temperature-opts temperature
           :f-pi-opts {:f-pi-policy-posterior? entered?
                       :f-pi-values [10.0 0.0] :f-pi-scaling :unscaled
                       :f-pi-posterior {:status :present}}}))
(def head (strategic :controller-head true))
(def posterior (strategic :full-score-posterior true))
(def fallback (strategic :full-score-posterior false))
(def tied [{:rank 1 :action {:type :a} :controller-score 0.0 :habit-prior-bias 1.0}
           {:rank 2 :action {:type :b} :controller-score 0.0 :habit-prior-bias 1.0}])
(def last-tie (policy/select-action tied {:selection-boundary :actuation
                                          :temperature-opts temperature}))
(def no-op-ranked [{:rank 1 :action {:type :no-op} :controller-score -10.0}
                   {:rank 2 :action {:type :a} :controller-score 0.0}])
(def strategic-no-op
  (policy/select-action no-op-ranked {:selection-boundary :strategic-recommendation
                                      :selection-law :controller-head
                                      :temperature-opts temperature}))

(defn actual-type [result] (get-in result [:action :type] (:action result)))
(def lines
  ["F8 leg 1 slice 9 -- production selected-action readback"
   (str "controllerHeadReference expected=:a actual=" (actual-type head)
        " " (if (= :a (actual-type head)) "MATCH" "MISMATCH"))
   (str "posteriorReference expected=:b actual=" (actual-type posterior)
        " " (if (= :b (actual-type posterior)) "MATCH" "MISMATCH"))
   (str "posteriorFallbackReference expected=:a actual=" (actual-type fallback)
        " " (if (= :a (actual-type fallback)) "MATCH" "MISMATCH"))
   (str "tiedArgmaxesDisagree last-max expected=:b actual=" (actual-type last-tie)
        " " (if (= :b (actual-type last-tie)) "MATCH" "MISMATCH"))
   (str "noOpCandidateSetsDiffer strategic expected=:a actual="
        (actual-type strategic-no-op) " "
        (if (= :a (actual-type strategic-no-op)) "MATCH" "MISMATCH"))
   (if (= [:a :b :a :b :a]
          (mapv actual-type [head posterior fallback last-tie strategic-no-op]))
     "VERDICT: production matches every measured Lean action witness."
     "VERDICT: MISMATCH.")])

(let [out (io/file "holes/labs/wm-contract/runs/F8-action/clojure-readback.txt")]
  (io/make-parents out)
  (spit out (str (str/join "\n" lines) "\n"))
  (println (slurp out)))
