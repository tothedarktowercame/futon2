(ns futon2.aif.budget-observation-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.meta-field-observation :as field]))

(defn- pin [path ch]
  {:path path :sha256 (apply str (repeat 64 ch))})

(defn- availability [available]
  {:schema :wm/ordinary-click-availability-v1
   :authorization {:path "authority.md" :sha "fixture"}
   :allocated 2 :consumed (- 2 available) :available available :unit :ordinary-click
   :ledger-source (pin "/budget-ledger.jsonl" "a")})

(defn- field-from-budget [budget]
  (let [body (.getBytes "algorithm" "UTF-8")
        source {:path "/algorithm.clj" :sha256 (field/sha256 body)}
        catalog (.getBytes
                 (pr-str {:schema :wm/approved-algorithm-catalog-v1
                          :entries [{:id "A" :status :approved :source source
                                     :repairs-capability :repair-capability
                                     :evidence-locator {:kind :fixture :id :A}
                                     :resource-requirements {:ordinary-clicks 1}}]})
                 "UTF-8")]
    (field/observe {:registry-snapshot {:missions {:missions []}
                                        :excursions {:excursions []}
                                        :tickets {:tickets []}}
                    :catalog-path "/catalog.edn"
                    :click-availability budget
                    :read-bytes #(case % "/catalog.edn" catalog "/algorithm.clj" body)})))

(deftest budget-reader-states-remain-distinct-at-the-meta-field-boundary
  (let [zero (runtime/observe-ordinary-click-budget
              {:ordinary-click-availability-fn (constantly (availability 0))
               :budget-observed-at "2026-10-19T09:00:00Z"})
        unavailable (runtime/observe-ordinary-click-budget
                     {:ordinary-click-availability-fn (fn [] (throw (Exception. "ledger read")))})
        positive (runtime/observe-ordinary-click-budget
                  {:ordinary-click-availability-fn (constantly (availability 2))
                   :budget-observed-at "2026-10-19T09:01:00Z"})
        malformed (runtime/observe-ordinary-click-budget
                   {:ordinary-click-availability-fn (constantly {:available 0})})
        zero-field (field-from-budget zero)
        unavailable-field (field-from-budget unavailable)
        positive-field (field-from-budget positive)
        zero-exclusion (first (:exclusions zero-field))
        unavailable-exclusion (first (:exclusions unavailable-field))]
    (is (= :exhausted (:status zero)))
    (is (= :unavailable (:status unavailable)))
    (is (= :available (:status positive)))
    (is (= :unavailable (:status malformed)))
    (is (= {:status :present :ledger-source (pin "/budget-ledger.jsonl" "a")}
           (:provenance zero)))
    (is (= {:status :present :value "2026-10-19T09:01:00Z"}
           (:observation-time positive)))
    (is (= {:status :absent} (:provenance unavailable)))
    (is (= :algorithm/click-resource-unavailable (:ineligible-reason zero-exclusion)))
    (is (= :exhausted (get-in zero-exclusion [:ineligibility-evidence :availability :status])))
    (is (= :algorithm/click-resource-unavailable (:ineligible-reason unavailable-exclusion)))
    (is (= :unavailable
           (get-in unavailable-exclusion [:ineligibility-evidence :availability :status])))
    ;; Positive pre-admission observation clears only the resource condition;
    ;; it still cannot admit an algorithm without distinct injury authority.
    (is (= :algorithm/injury-capability-unavailable
           (:ineligible-reason (first (:exclusions positive-field)))))))
