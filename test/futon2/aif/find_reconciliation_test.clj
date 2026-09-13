(ns futon2.aif.find-reconciliation-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.find-reconciliation :as reconciliation]))

(def recorded
  (edn/read-string
   (slurp "holes/labs/wm-contract/runs/F11-find/01-find-snatch-live.edn")))

(defn shift-lines [snapshot]
  (update snapshot :scenarios
          (fn [scenarios]
            (mapv (fn [scenario]
                    (update scenario :round-results
                            (fn [rounds]
                              (mapv (fn [round]
                                      (update-in round [:find :receipts]
                                                 (fn [receipts]
                                                   (into {} (map (fn [[id receipt]]
                                                                   [id (update-in receipt [:warrant :if-lines]
                                                                                  #(mapv inc %))])
                                                                 receipts)))))
                                    rounds))))
                  scenarios))))

(deftest recorded-population-and-order
  (let [shifted (shift-lines recorded)
        reordered (update shifted :scenarios
                          #(mapv (fn [s] (update s :round-results (comp vec reverse)))
                                 (reverse %)))
        expected (reconciliation/report recorded shifted)]
    (is (= 24 (count (:repository recorded))))
    (is (= 96 (:receipts-compared expected)))
    (is (true? (:difference-is-line-coordinates-only? expected)))
    (is (= expected (reconciliation/report recorded reordered)))
    (is (false? (:difference-is-line-coordinates-only?
                 (reconciliation/report recorded recorded))))))

(deftest structural-loss-and-duplication-cannot-hide-behind-lines
  (let [shifted (shift-lines recorded)
        receipt-id (first (keys (get-in shifted [:scenarios 0 :round-results 0 :find :receipts])))]
    (doseq [[kind mutated]
            [[:scenarios (update shifted :scenarios #(vec (rest %)))]
             [:rounds (update-in shifted [:scenarios 0 :round-results] #(vec (rest %)))]
             [:receipts (update-in shifted [:scenarios 0 :round-results 0 :find :receipts]
                                   dissoc receipt-id)]
             [:scenarios (update shifted :scenarios #(conj % (first %)))]
             [:rounds (update-in shifted [:scenarios 0 :round-results] #(conj % (first %)))]]]
      (doseq [[pin live] [[recorded mutated] [mutated recorded]]]
        (let [report (reconciliation/report pin live)]
          (is (false? (:difference-is-line-coordinates-only? report)))
          (is (false? (:receipt-populations-identical? report)))
          (is (some #(= kind (:kind %)) (:structural-differences report))))))))

(deftest clause-text-control
  (let [shifted (shift-lines recorded)
        id (first (keys (get-in shifted [:scenarios 0 :round-results 0 :find :receipts])))
        mutated (assoc-in shifted [:scenarios 0 :round-results 0 :find :receipts id
                                   :warrant :if-text] "not the authored clause")
        result (reconciliation/report recorded mutated)]
    (is (true? (:receipt-populations-identical? result)))
    (is (false? (:difference-is-line-coordinates-only? result)))
    (is (= 1 (get-in result [:differing-fields :warrant/if-text])))))
