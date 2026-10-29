(ns futon2.aif.reading-policy-class-free-energy-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.reading-policy-class-free-energy :as f]))

(def key-a [:wm/reading-policy-v1 "M-a" "M-a/policy-a" {:nodes [] :edges [] :order []}])
(def key-b [:wm/reading-policy-v1 "M-b" "M-b/policy-b" {:nodes [] :edges [] :order []}])

(defn run-source [path key class]
  {:path path
   :record {:run/id path
            :terminal {:schema :wm/q6-terminal-v1
                       :choice {:status :present :policy-key key}}
            :run-ending-classification {:class class}}})

(deftest empty-history-is-computed-zero
  (is (= {:status :computed :value 0.0 :policy-key key-a :window 4 :history []}
         (f/evaluate key-a 4 [] {:focused 1.0}))))

(deftest retained-q6-key-selects-the-history
  (let [a (run-source "run-a" key-a :focused)
        b (run-source "run-b" key-b :related)
        result (f/evaluate key-a 4 [a b] {:focused 0.25 :related 0.75})]
    (is (= :computed (:status result)))
    (is (= [{:policy-key key-a :class :focused
             :pin {:path "run-a" :run-id "run-a"}}]
           (:history result)))
    (is (< (Math/abs (- (Math/log 4.0) (:value result))) 1.0e-12))))

(deftest impossible-retained-class-is-typed
  (let [result (f/evaluate key-a 2 [(run-source "run-zero" key-a :unrelated)]
                           {:focused 1.0 :unrelated 0.0})]
    (is (= :observation-impossible-under-policy (:status result)))
    (is (= {:run {:path "run-zero" :run-id "run-zero"}
            :class :unrelated :evidence 0.0}
           (:failure result)))))
