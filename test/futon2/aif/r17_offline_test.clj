(ns futon2.aif.r17-offline-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.r17-offline :as r17]))

(def accepted-corpus
  {:capabilities ["A" "B" "C"]
   :edges (vec (concat
                (repeat 10 ["A" "m1"])
                (repeat 10 ["A" "m2"])
                (repeat 1 ["A" "m3"])
                (repeat 10 ["B" "m1"])
                (repeat 10 ["B" "m2"])
                (repeat 1 ["B" "m3"])
                (repeat 1 ["C" "m1"])
                (repeat 1 ["C" "m2"])
                (repeat 20 ["C" "m3"])))
   :discharges []})

(def rejected-corpus
  {:capabilities ["A" "C"]
   :edges (vec (concat
                (repeat 20 ["A" "m1"])
                (repeat 1 ["A" "m2"])
                (repeat 1 ["C" "m1"])
                (repeat 20 ["C" "m2"])))
   :discharges []})

(def parent-model
  {:id "capability-model/v17"
   :revision "sha256:parent"
   :kind :capability-outcome-dirichlet})

(defn- run-envelope
  [run-id corpus]
  (r17/run {:run-id run-id
            :parent-model parent-model
            :corpus corpus}))

(deftest accepted-reduction-records-auditable-result
  (let [envelope (run-envelope "campaign-17/tick-a" accepted-corpus)
        proposals (:r17/proposals envelope)
        accepted (filterv #(= :accept (:decision %)) proposals)]
    (is (= 1 (:r17/envelope-version envelope)))
    (is (= parent-model (get-in envelope [:r17/parent :model])))
    (is (= :structure-reduced (get-in envelope [:r17/decision :outcome])))
    (is (= [["A" "B"]] (mapv #(get-in % [:reduction :members]) accepted)))
    (is (= {:statistic :delta-F
            :threshold -3.0
            :accept-when :less-than-or-equal}
           (dissoc (:evidence (first accepted)) :value)))
    (is (<= (get-in (first accepted) [:evidence :value]) -3.0))
    (is (= {"A" ["A" "B"], "C" ["C"]}
           (get-in envelope [:r17/resulting-structure :equivalence-classes])))
    (is (= {"A" "A", "B" "A", "C" "C"}
           (get-in envelope [:r17/resulting-structure :capability->concept])))))

(deftest rejected-reduction-records-principled-non-change
  (let [envelope (run-envelope "campaign-17/tick-b" rejected-corpus)
        parent-structure (get-in envelope [:r17/parent :structure])
        result (:r17/resulting-structure envelope)]
    (is (= {:outcome :principled-no-change
            :reason :no-reduction-met-evidence-threshold}
           (:r17/decision envelope)))
    (is (= 1 (count (:r17/proposals envelope))))
    (is (= :reject (get-in envelope [:r17/proposals 0 :decision])))
    (is (> (get-in envelope [:r17/proposals 0 :evidence :value]) -3.0))
    (is (= (:concepts parent-structure) (:concepts result)))
    (is (= (:equivalence-classes parent-structure)
           (:equivalence-classes result)))
    (is (= (:concept-concentrations parent-structure)
           (:concept-concentrations result)))))

(deftest recorded-input-replays-to-equality
  (doseq [envelope [(run-envelope "campaign-17/accepted" accepted-corpus)
                    (run-envelope "campaign-17/rejected" rejected-corpus)]]
    (testing (name (get-in envelope [:r17/decision :outcome]))
      (is (= 'futon2.aif.r17-offline/replay
             (get-in envelope [:r17/replay :entrypoint])))
      (is (= envelope (r17/replay envelope))))))

(deftest parent-identity-is-required
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"require a parent model"
                        (r17/run {:run-id "missing-parent"
                                  :parent-model {}
                                  :corpus rejected-corpus}))))
