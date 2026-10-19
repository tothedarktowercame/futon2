(ns futon2.aif.outer-policy-provider-runtime-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.outer-policy-provider-runtime :as sut]
            [futon2.aif.outer-policy-evidence-provider :as provider]
            [futon2.aif.outer-target-belief :as belief]
            [futon2.aif.outer-target-score :as score]
            [futon2.aif.outer-policy-evidence :as evidence]
            [futon2.aif.outer-policy-objective :as objective]
            [futon2.aif.selected-outer-cascade-policy :as selected]
            [futon2.aif.selected-outer-cascade-policy-test]))

(defn state []
  (let [path "resources/wm/outer-learning-authority.edn"
        text (slurp path) authority (edn/read-string text)
        source {:path path :sha256 (identity/sha256 (.getBytes text "UTF-8"))}]
    {:authority authority
     :belief (belief/build {:dataset {:status :produced :rows []}
                            :prior (belief/prior (get-in authority [:prior :concentration])
                                                 (get-in authority [:prior :authority]))})
     :preference-authority {:source source}
     :preference-costs (get-in authority [:preferences :costs])
     :preference-transform (get-in authority [:preferences :transform])
     :controller (:controller authority)}))

(defn menu []
  (:menu ((var-get (ns-resolve 'futon2.aif.selected-outer-cascade-policy-test 'production-carriers)))))

(deftest no-history-can-produce-and-execute-a-verified-policy
  (let [m (menu) p (sut/provide (state) m [])
        e (evidence/join m (:declaration p) (:records p))
        result (objective/evaluate e m 0.8)
        carrier (selected/seal m e result)]
    (is (nil? (provider/verify p)))
    (is (= p (provider/resolve-provider p m)) "a map provider is data, not an IFn invocation")
    (is (= :computed (:status e)))
    (is (every? #(every? zero? (:task-counts %)) (:entries e)))
    (is (every? #(= [1 1 1 1 1] (:posterior-alpha %)) (:entries e)))
    (is (= :selected (:status result)))
    (is (= :uniform-over-equal-minima (get-in result [:draw :law])))
    (is (nil? (selected/verify carrier)))
    (is (= :selected (:status (selected/decision carrier))))))

(deftest available-policy-outcomes-update-only-the-matching-policy
  (let [m (menu) digest (:cascade-digest (first (:entries m)))
        record {:run/id "observed" :selected-cascade-digest digest
                :observed-outcome :closure :terminal-receipt {:outcome :grounded-change}}
        p (sut/provide (state) m [record])
        e (evidence/join m (:declaration p) (:records p))]
    (is (= :computed (:status e)))
    (is (= [2 1 1 1 1] (:posterior-alpha (first (:entries e)))))
    (is (= [1 1 1 1 1] (:posterior-alpha (second (:entries e)))))))

(deftest scored-policies-survive-unscorable-neighbours
  (let [m (menu)
        entries (:entries m)
        first-entry (first entries)
        second-entry (second entries)
        first-digest (:cascade-digest first-entry)
        second-digest (:cascade-digest second-entry)
        real-score score/score
        score-calls (atom 0)
        unscorable {:schema :wm/outer-target-score-v1 :status :refused
                    :reason :preference-transform-invalid}]
    (with-redefs [score/score (fn [args]
                                (if (= 1 (swap! score-calls inc))
                                  (real-score args)
                                  unscorable))]
      (let [result (sut/provide (state) m [])]
        (is (nil? (provider/verify result)))
        (is (= [first-digest second-digest]
               (get-in result [:score-frontier :menu :policy-digests])))
        (is (= {:count 1 :policy-digests [first-digest]}
               (get-in result [:score-frontier :scored])))
        (is (= {:count 1
                :policies [{:policy-id (:policy-id second-entry)
                            :target-id (:target-id second-entry)
                            :cascade-digest second-digest
                            :reason :preference-transform-invalid}]}
               (get-in result [:score-frontier :score-absent])))
        (is (= :computed (get-in result [:score-records 0 :status])))
        (is (= :absent (get-in result [:score-records 1 :status])))
        (is (= [first-digest] (vec (keys (get-in result [:declaration :policies])))))))))

(deftest all-unscorable-policies-produce-an-empty-assessed-frontier
  (let [m (menu)]
    (with-redefs [score/score (constantly {:schema :wm/outer-target-score-v1
                                            :status :refused
                                            :reason :belief-not-produced})]
      (let [result (sut/provide (state) m [])]
        (is (nil? (provider/verify result)))
        (is (= 0 (get-in result [:score-frontier :scored :count])))
        (is (= (count (:entries m))
               (get-in result [:score-frontier :score-absent :count])))
        (is (= :no-policy-scores (get-in result [:declaration :reason])))))))
