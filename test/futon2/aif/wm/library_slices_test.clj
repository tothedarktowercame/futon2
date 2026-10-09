(ns futon2.aif.wm.library-slices-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.wm.library-slices :as slices]))

(def manifest
  {:schema :wm/pinned-pattern-library-v1 :digest "library-digest" :size 12
   :patterns (mapv (fn [n]
                     {:id (str "family/p" n ".flexiarg")
                      :sha256 (str "sha" n)
                      :tokens (if (< n 2) #{"alpha"} #{"other"})})
                   (range 12))})

(deftest batch-retrieves-a-minimum-ten-from-the-whole-pin
  (let [result (slices/batch manifest [["M-a" "alpha work"]]
                             {:k 10 :nano-time-fn (constantly 0)})
        slice (get-in result [:slices "M-a"])]
    (is (= 10 (:slice-size slice)))
    (is (= 12 (:library-size slice)))
    (is (true? (:slice-from-whole-library slice)))
    (is (= "library-digest" (:library-manifest-digest slice)))
    (is (= 40 (get-in result [:slice-budget :value])))
    (is (= 10 (get-in result [:slice-budget :effective-value])))
    (is (= (:slice-budget result) (:retrieval-budget slice)))
    (is (= ["family/p0" "family/p1"]
           (mapv :pattern (take 2 (:candidates slice)))))))

(deftest missing-query-and-budget-exhaustion-are-explicit
  (let [ticks (atom [0 0 2000000 2000000 2000000])
        now #(let [x (first @ticks)] (swap! ticks subvec 1) x)
        result (slices/batch manifest [["M-empty" nil] ["M-late" "alpha"]]
                             {:k 10 :max-millis 1 :nano-time-fn now})]
    (is (= :target-query-absent
           (get-in result [:refusals "M-empty" :kind])))
    (is (= :budget-exhausted
           (get-in result [:refusals "M-late" :kind])))
    (is (empty? (:slices result)))))
