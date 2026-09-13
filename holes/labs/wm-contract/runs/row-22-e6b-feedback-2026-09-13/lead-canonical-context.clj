(require '[futon2.aif.machine-slow-feedback-evidence :as e]
 '[futon2.aif.machine-slow-feedback-evidence-test :as t] '[clojure.walk :as walk])
(let [cfg (#'t/fixture
 (fn [rs]
  (let [changed (walk/postwalk #(if (= % "model-v3") "borrowed-model" %) rs)]
   (-> changed
    (assoc-in [:application-ledger :entries 0 :input/digests]
      (into {} (for [[out in] [[:context :context] [:prior :prior-state] [:e2b :e2b-subject] [:outcome :outcome]]]
                 [out (#'t/vd (in changed))])))
    (assoc-in [:application-ledger :entries 0 :output/digest] (#'t/vd (:next-state changed)))
    (assoc-in [:application-universe :transition/subject-digest]
              (#'t/vd (get-in changed [:application-universe :transition/subject])))))))
 out (e/verify-feedback cfg)]
 (assert (true? (:replay/identical? out)))
 (prn {:control :borrowed-model-with-unchanged-canonical-inputs :identity (:identity out)
       :replay (:replay/identical? out)}))
