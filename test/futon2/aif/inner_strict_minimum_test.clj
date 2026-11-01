(ns futon2.aif.inner-strict-minimum-test
  "The law on a real certificate: Requirements Q11, Joe's ruling of 2026-11-01."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.policy :as policy]))

;; Run 376d76ff: each candidate's identity, G and scored predictions, verbatim.
;; The action marginal enacted policy-7d91 at G 1.6584; policy-34c8 had G 1.4034.
(def recorded
  (edn/read-string {:default tagged-literal}
                   (slurp "test/fixtures/wm-run-facts/376d76ff-candidates.edn")))

(def decision
  (policy/select-action-cascades (:ranked recorded) {:beta (:beta recorded)}))

(defn- short-id [id] (last (str/split (str id) #"/")))

(deftest the-minimiser-is-enacted-on-the-real-certificate
  (is (str/ends-with? (:recorded-chosen recorded) "policy-7d91338b4da14feb2f06"))
  (is (= "policy-34c85cf68e52022bd8ae" (short-id (get-in decision [:action :id]))))
  (is (= 1.4034185419566536 (:controller-score decision))))

(deftest the-record-says-what-ran
  (let [law (:selection-law decision)
        comparison (:policy-comparison law)]
    (is (= :strict-G-minimum-over-prediction-classes (:law law)))
    (is (= :G (:decided-by comparison)))
    (is (= :prediction-class (:comparison-domain comparison)))
    (is (= 1 (count (:class-members law))) "the minimum class has one policy")
    (is (= "policy-7d91338b4da14feb2f06"
           (short-id (get-in comparison [:runner-up :id :id])))
        "the runner-up is the class's first in ranked order")))

(deftest same-prediction-variants-are-one-class-on-the-real-certificate
  ;; 7d91 and 80cf have one prediction and one G: they do not tie each other,
  ;; and the choice between them would be an arrangement, not a policy.
  (let [without-minimum (policy/select-action-cascades
                         (vec (take 2 (:ranked recorded))) {:beta (:beta recorded)})
        law (:selection-law without-minimum)]
    (is (= 2 (count (:class-members law))))
    (is (= (:enacted law) (first (:class-members law))))))
