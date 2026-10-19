(ns futon2.aif.p37-reading-preparation-runtime-test
  "P37's production-shaped pre-choice preparation caller path."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.selected-target-reading :as selected-target-reading]
            [futon2.aif.selected-outer-cascade-policy :as selected]
            [futon2.aif.outer-policy-runtime-selection-test]))

(defn- outer-var [sym]
  (var-get (ns-resolve 'futon2.aif.outer-policy-runtime-selection-test sym)))

(defn- pin [path c] {:path path :sha256 (apply str (repeat 64 c))})

(deftest bounded-prechoice-preparation-retains-frontier-and-resumes-an-assessed-choice
  ;; production-defaults supplies the actual runtime port; runner owns the
  ;; publication/redecision handoff; run-live is the real outer P05 selector.
  (let [field {:schema :wm/pre-choice-policy-field-v1
               :source-pin (pin "wm://prechoice-field" "e")
               :target-sources (vec (concat [{:target-id "T-occ" :source-path "T-occ.md"}]
                                            (for [n (range 1 5)]
                                              {:target-id (str "T-" n)
                                               :source-path (str "T-" n ".md")})))
               :families (vec (concat
                               [{:target-id "T-occ" :status :computed :failures []}]
                               (for [n (range 1 5)]
                                 {:target-id (str "T-" n) :status :refused
                                  :failures [{:kind :no-current-target-reading
                                              :expected-digest (str "digest-" n)}]})))}
        refusal {:outer-task-selection
                 {:chosen {:absent :incomplete-policy-menu-coverage}
                  :policy {:reason :incomplete-policy-menu-coverage}
                  :policy-field field}}
        run-live (outer-var 'run-live)
        explicit-provider (outer-var 'explicit-provider)
        tasks (outer-var 'tasks)
        selected-receipt (run-live (explicit-provider [0.0 1.0] []))
        select (runtime/retained-outer-selector (fn [_] selected-receipt) {})
        defaults (runtime/production-defaults
                  {:run-id "p37-prechoice" :author "wm-author"})]
    (with-redefs [selected-target-reading/repair-need!
                  (fn [_ need]
                    (case (:target need)
                      "T-1" {:schema :wm/selected-target-reading-request-v1
                               :status :published :published true :target "T-1"
                               :publication-address {:registry-key ["T-1" "digest-1"]}}
                      "T-2" {:schema :wm/selected-target-reading-request-v1
                               :status :refused :published false :target "T-2"
                               :kind :selected-target-reading-publication-refused}))]
      (let [result (runner/selected-target-reading-step
                    {} refusal (:selected-target-reading-fn defaults)
                    (fn [_] {:outer-task-selection (select {:tasks tasks})}))
            record (:record result)
            frontier (:frontier record)]
        (is (= :refused (:status record)))
        (is (= :publication-refused (:reason record)))
        (is (= [["T-1" "digest-1"]] (:published-ids frontier)))
        (is (= [["T-2" "digest-2"]] (:refused-ids frontier)))
        (is (= [["T-2" "digest-2"] ["T-3" "digest-3"] ["T-4" "digest-4"]]
               (:pending-ids frontier)))
        (is (= ["T-occ" "T-1" "T-2" "T-3" "T-4"]
               (:eligible-target-ids frontier)))
        (is (= ["T-occ"] (:assessed-target-ids frontier)))
        (is (= ["T-1" "T-2" "T-3" "T-4"] (:reading-absent-ids frontier)))
        (is (= {:registry-key ["T-1" "digest-1"]}
               (get-in record [:records 0 :publication-address])))
        (is (= :selected (get-in result [:judgement :outer-task-selection :policy :status])))
        (is (= "T-occ" (get-in result [:judgement :outer-task-selection :chosen :id])))
        (is (nil? (selected/verify
                   (get-in result [:judgement :outer-task-selection :policy
                                   :selected-outer-cascade-policy]))))))))
