(ns futon2.aif.outer-canonical-g-terms-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.outer-canonical-g :as sut]
            [futon2.aif.outer-canonical-selection :as sel]))

;; Same live pin as outer-canonical-g-frontier-test: b5's input record
;; (fc464172), values verbatim.
(def fixture (edn/read-string (slurp "test/fixtures/outer-canonical-g/b5-input-fc464172.edn")))

(def stack-geometry "M-stack-geometry/policy-c7d7518e1e185c2e2318")

(defn- by-id [frontier] (into {} (map (juxt :policy-id identity)) (:entries frontier)))

(deftest scored-rows-carry-bounded-g-terms
  (let [menu (:sealed-menu fixture)
        F (sut/produce menu)
        H (get-in F [:horizon-choice :horizon])
        scored (filter #(= :scored (:status %)) (:entries F))
        rows (by-id F)
        sg (rows stack-geometry)
        {:keys [g-terms parameter-novelty steps]} (:terms sg)]
    (testing "every scored row has terms with the three parts"
      (is (pos? (count scored)))
      (doseq [row scored
              :let [t (:terms row)]]
        (is (map? t))
        (is (contains? t :g-terms))
        (is (contains? t :parameter-novelty))
        (is (contains? t :steps))
        (is (= H (count (:steps t))))
        (is (= (vec (range 1 (inc H))) (mapv :tau (:steps t))))
        (doseq [step (:steps t)]
          (is (= #{:tau :g :expected-information-gain} (set (keys step)))))))
    (testing "stack-geometry row: values verbatim, sum of steps equals G, totals agree"
      (is (= -2.004267726446009 (:G sg)))          ; pinned from a run
      (is (= 5N (:total parameter-novelty)))        ; pinned from a run
      (is (< (Math/abs (- (reduce + (map :g steps)) (:G sg))) 1e-9))
      (is (= (:total parameter-novelty)
             (get-in g-terms [:expected-information-gain])))
      (is (= #{:law :lean :total :counts} (set (keys parameter-novelty)))))
    (testing "refused rows carry no terms"
      (let [refused-menu (update menu :entries
                                 #(assoc % 0 (update (first %) :class-emission dissoc :model)))
            refused-F (sut/produce refused-menu)
            refused-rows (:entries refused-F)]
        (is (some #(= :refused (:status %)) refused-rows))
        (doseq [row refused-rows]
          (when (= :refused (:status row))
            (is (not (contains? row :terms)))))))
    (testing "terms do not change what is selected, four draws"
      (let [F' (update F :entries #(mapv (fn [e] (dissoc e :terms)) %))]
        (doseq [u [0.0 0.3 0.7 0.999]]
          (let [a (sel/evaluate F menu u)
                b (sel/evaluate F' menu u)]
            (is (= (:status a) (:status b)))
            (is (= (:action a) (:action b)))
            (is (= (:basis a) (:basis b)))
            (is (= (:draw a) (:draw b)))
            (is (= (:policy-id (:selected a)) (:policy-id (:selected b))))
            (is (= (dissoc (:selected a) :terms) (:selected b)))))))))

;; Review addition (claude-9, 2026-10-26): the typed-absence path the helper is
;; named for, constructed directly.  A scorer result with no certificate, or a
;; certificate missing a part, must yield the marker, never nil.
(deftest nil-certificate-parts-are-typed-absences
  (let [terms #'sut/certificate-terms
        absent {:absent :certificate-term-not-retained}]
    (is (= {:g-terms absent :parameter-novelty absent :steps absent} (terms {})))
    (is (= {:g-terms absent :parameter-novelty absent :steps absent}
           (terms {:status :scored :G-efe -1.0 :rank 1})))
    (let [t (terms {:certificate {:g-terms {:expected-information-gain 1N :raw {}}
                                  :parameter-novelty {:law :l :lean :n :total 1N :counts {}
                                                      :steps [{:tau 1 :before {} :expected-novelty 1N}]}}})]
      (is (= {:expected-information-gain 1N :raw {}} (:g-terms t)))
      (is (= {:law :l :lean :n :total 1N :counts {}} (:parameter-novelty t))
          "the before-belief steps are dropped")
      (is (= absent (:steps t)) "a missing part is the marker, not nil")
      (is (not-any? nil? (vals t))))))
