(ns futon2.aif.cascade-order-check-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-order-check :as sut]))

(deftest both-producer-shapes-use-from-to-convention
  (let [pairs (sut/check-cascade-order
               {:descent [["a" "b"] ["b" "a"]]})
        maps (sut/check-cascade-order
              [{:from "a" :to "b"} {:from "b" :to "a"}])]
    (is (= :failed (get-in pairs [:acyclic-descent :result])))
    (is (= ["a" "b" "a"] (get-in pairs [:acyclic-descent :cycle])))
    (is (= (:acyclic-descent pairs) (:acyclic-descent maps)))
    (is (= :unwitnessable (get-in pairs [:has-meets :result])))))

(deftest order-and-meet-axes-remain-independent
  (let [disconnected (sut/check-cascade-order
                      {:elements ["lower" "upper" "isolated" "isolated-top"]
                       :descent [["lower" "upper"]
                                 ["isolated" "isolated-top"]]})
        chain (sut/check-cascade-order
               {:elements ["a" "b"] :descent [["a" "b"]]})]
    (is (= :passed (get-in disconnected [:acyclic-descent :result])))
    (is (= :failed (get-in disconnected [:has-meets :result])))
    (is (= :passed (get-in chain [:acyclic-descent :result])))
    (is (= :passed (get-in chain [:has-meets :result])))))

(defn- cascade-with-descent [record]
  (some #(when (and (map? (:semilattice %))
                    (contains? (:semilattice %) :descent))
           %)
        (filter map? (tree-seq coll? seq record))))

(deftest recorded-constructions-reproduce-the-cycle-census
  (let [files (filter #(and (.isFile %)
                            (= "003-construction.edn" (.getName %)))
                      (file-seq (io/file "data/wm-full-loop")))
        descents (keep (fn [file]
                         (when-some [cascade
                                     (cascade-with-descent
                                      (edn/read-string (slurp file)))]
                           {:file (.getPath file)
                            :elements (:patterns cascade)
                            :descent (get-in cascade [:semilattice :descent])}))
                       files)
        checked (map #(assoc % :check
                             (sut/check-cascade-order
                              {:elements (:elements %)
                               :descent (:descent %)}))
                     descents)
        cyclic (filter #(= :failed
                           (get-in % [:check :acyclic-descent :result]))
                       checked)]
    (is (= 25 (count descents)))
    (is (= 4 (count cyclic)))
    (is (= 5 (count (filter (comp empty? :descent) descents))))
    (is (every? seq (map #(get-in % [:check :acyclic-descent :cycle])
                         cyclic)))))
