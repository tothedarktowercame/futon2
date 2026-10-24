(ns futon2.aif.wm.family-theta-coherence-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.learning-trial-ledger :as ledger]
            [futon2.aif.cascade-shape-g :as shape]
            [futon2.aif.cascade-coapply-g :as raw]
            [futon2.aif.wm.family-canonical-g :as sut]))

(def policy {:target "T" :policy-id :p :kind :fixture
             :analysis {:sentences []}
             :cascade {:nodes [{:pattern :p :occurrence-id :p}] :edges []}})

(defn rows [record]
  ;; Real arranged producer, isolated from the live learning ledger.
  (with-redefs [ledger/pattern-theta (constantly record)]
    [{:target-id "T" :failures [] :scored [[policy (shape/score-policy policy)]]}]))

(def learned {:status :recorded-trials :trials-count 1 :successes 1
              :identities ["supplied-attempt"] :theta 3/4})
(defn score [rs] (get-in (sut/rescore rs) [:rows 0 :scored 0 1]))
(defn change-theta [rs theta]
  (assoc-in rs [0 :scored 0 1 :candidate :precedence :co-apply :patterns :p :theta] theta))

(defn forged-score [good theta]
  ;; Recompute ALL score/certificate fields via the unchanged raw API. This
  ;; is not merely a stale certificate or a tampered g scalar.
  (let [{:keys [candidate state options]} (:canonical-inputs good)
        candidate (assoc-in candidate [:precedence :co-apply :patterns :p :theta] theta)
        entry (first (raw/compare-at-horizon state [candidate] options))]
    (#'sut/replace-score (assoc good :candidate candidate) entry
                        (get-in good [:class-emission :model])
                        {:candidate candidate :state state :options options}
                        (:horizon-choice good))))

(deftest same-posterior-family-boundary
  (let [before (score (rows {:status :no-recorded-trials}))
        after (score (rows learned))
        default (score (rows {:status :defaulted :reason :ledger-unreadable}))]
    (doseq [s [before after default]]
      (is (= :certified (:status (sut/certify-score s))))
      (is (= 1 (:horizon s))))
    (is (= learned (get-in after [:candidate :precedence :co-apply :patterns :p :theta-record])))
    (is (= [1/2 1/2] (get-in before [:canonical-inputs :options :pattern-counts ["T" :p] :p])))
    (is (= [3/2 1/2] (get-in after [:canonical-inputs :options :pattern-counts ["T" :p] :p])))
    (is (= [1/2 3/4]
           (mapv #(get-in % [:scorer-result 0 :certificate :steps 0 :belief #{["T" :pattern-done :p]}])
                 [before after])))
    (is (> (:information-gain before) (:information-gain after)))
    (let [bad (sut/rescore (change-theta (rows learned) 1))]
      (is (empty? (get-in bad [:rows 0 :scored])))
      (is (= :incoherent-pattern-theta (get-in bad [:rows 0 :failures 0 :kind]))))
    (is (= :incoherent-pattern-theta (:kind (sut/certify-score (forged-score after 1)))))))

(deftest unsupported-theta-is-typed-not-coerced
  (doseq [theta [nil 0.75 Double/NaN Double/POSITIVE_INFINITY "3/4" 0.75M]]
    (let [r (sut/rescore (change-theta (rows learned) theta))]
      (is (empty? (get-in r [:rows 0 :scored])))
      (is (= :unsupported-pattern-theta (get-in r [:rows 0 :failures 0 :kind]))))))

(deftest count-domain-and-independent-certification
  (let [s (score (rows learned))
        candidate (get-in s [:canonical-inputs :candidate])]
    (doseq [pair [nil [] [0 1] [-1 2] [1.5 0.5] [Double/NaN 1] ["a" 1]]]
      (let [bad (assoc-in s [:canonical-inputs :options :pattern-counts ["T" :p] :p] pair)]
        (is (= :unsupported-pattern-counts (:kind (sut/coherence-finding candidate {:p pair}))))
        (is (= :unsupported-pattern-counts (:kind (sut/certify-score bad))))))
    (is (= :pattern-coherence-input-missing (:kind (sut/coherence-finding candidate {}))))
    (is (= :unsupported-pattern-theta
           (:kind (sut/certify-score
                   (assoc-in s [:canonical-inputs :candidate :precedence :co-apply :patterns :p :theta] nil)))))))

(deftest incoherent-candidate-never-reaches-raw-scoring
  (let [good (rows learned)
        bad (-> (change-theta good 1)
                (assoc-in [0 :scored 0 0 :policy-id] :bad))
        seen (atom [])
        original raw/compare-at-horizon
        r (with-redefs [raw/compare-at-horizon
                        (fn [state candidates opts]
                          (swap! seen into (map :id candidates))
                          (original state candidates opts))]
            (sut/rescore (into good bad)))]
    (is (= [["T" :p]] @seen))
    (is (= 1 (count (mapcat :scored (:rows r)))))
    (is (= :incoherent-pattern-theta (get-in r [:rows 1 :failures 0 :kind])))))
