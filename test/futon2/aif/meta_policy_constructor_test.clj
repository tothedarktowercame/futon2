(ns futon2.aif.meta-policy-constructor-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.meta-field-observation :as field]
            [futon2.aif.meta-policy-constructor :as constructor]))

(defn- pin [id ch]
  {:path (str "/code/holes/" id ".md") :sha256 (apply str (repeat 64 ch))})

(defn- task [id kind ch]
  {:id id :kind kind :status-class :live :status-line "OPEN"
   :requisition {:absent :no-requisition} :source (pin id ch)})

(def registry-snapshot
  {:missions {:missions [(dissoc (task "M-one" :mission "a") :kind)]}
   :excursions {:excursions [(dissoc (task "E-one" :excursion "b") :kind)]}
   :tickets {:tickets [(dissoc (task "T-one" :ticket "c") :kind)]}})

(def resources {:time-budget-ms 5000 :token-budget 800
                :author-seat "codex-16" :reviewer-seat "codex-10"})

(defn- observed-field [] (field/observe {:registry-snapshot registry-snapshot}))

(defn- adapter [row next-step locator]
  {:id (:id row) :source (:source row) :adapter :target-field
   :next-step next-step :locator locator})

(defn- input []
  (let [observation (observed-field)
        rows (into {} (map (juxt :id identity)) (:rows observation))]
    {:field-observation observation :expected-field-pin (:source-pin observation)
     :resource-envelope resources
     :adapters [(adapter (rows "M-one") :ready
                         {:class :C4 :repo "repo" :path "M-one.md" :decl "criterion-1"})
                (adapter (rows "E-one") :read-criteria nil)
                (adapter (rows "T-one") :observe
                         {:kind :registered-run :id "ticket-observation"})]}))

(deftest constructs-or-excludes-every-verified-field-row
  (let [receipt (constructor/construct (input))
        templates (into {} (map (juxt #(get-in % [:slots :target]) identity))
                        (:templates receipt))
        exclusion (first (:exclusions receipt))]
    (is (= :constructed (:status receipt)))
    (is (= {:field 3 :constructed 2 :excluded 1
            :constructed-by-kind {:mission 1 :ticket 1}
            :excluded-by-kind {:excursion 1}}
           (:counts receipt)))
    (is (= :advance (get-in templates ["M-one" :slots :next-move])))
    (is (= :unblock (get-in templates ["T-one" :slots :next-move])))
    (is (= resources (get-in templates ["M-one" :slots :resource-envelope])))
    (is (nil? (:g-input (templates "M-one")))
        "construction does not invent model terms")
    (is (= {:id "E-one" :kind :excursion :source (pin "E-one" "b")
            :reason :evidence-channel-unavailable
            :evidence {:adapter {:adapter :target-field :next-step :read-criteria
                                 :locator nil}}}
           exclusion))
    (is (= :verified
           (:status (constructor/verify receipt
                                        (assoc (select-keys (input)
                                                            [:field-observation
                                                             :expected-field-pin])
                                               :expected-resource-envelope resources)))))))

(deftest construction-refuses-unverified-field-bad-resources-and-source-swap
  (let [base (input)]
    (is (= :field-not-verified
           (:reason (constructor/construct
                     (assoc base :expected-field-pin
                            {:path "wm://meta-field-observation-v1"
                             :sha256 (apply str (repeat 64 "f"))})))))
    (is (= :resource-envelope-invalid
           (:reason (constructor/construct
                     (assoc-in base [:resource-envelope :token-budget] 0)))))
    (is (= :adapter-source-mismatch
           (:reason (constructor/construct
                     (assoc-in base [:adapters 0 :source] (pin "M-one" "9"))))))))

(deftest construction-verification-detects-omission-source-swap-and-duplicates
  (let [base (input)
        receipt (constructor/construct base)
        authority (assoc (select-keys base [:field-observation :expected-field-pin])
                         :expected-resource-envelope resources)]
    (testing "omitted typed exclusion"
      (is (= :construction-coverage-incomplete
             (:reason (constructor/verify
                       (assoc receipt :exclusions []
                              :counts {:field 3 :constructed 2 :excluded 0
                                       :constructed-by-kind {:mission 1 :ticket 1}
                                       :excluded-by-kind {}})
                       authority)))))
    (testing "constructed source changed"
      (is (= :construction-source-or-kind-mismatch
             (:reason (constructor/verify
                       (assoc-in receipt [:templates 0 :slots :evidence-channel :source]
                                 (pin "forged" "9"))
                       authority)))))
    (testing "one field identity appears on both sides"
      (is (= :construction-identities-duplicated
             (:reason (constructor/verify
                       (-> receipt
                           (update :exclusions conj
                                   {:id "M-one" :kind :mission :source (pin "M-one" "a")
                                    :reason :evidence-channel-unavailable
                                    :evidence {:adapter {}}})
                           (assoc :counts
                                  {:field 3 :constructed 2 :excluded 2
                                   :constructed-by-kind {:mission 1 :ticket 1}
                                   :excluded-by-kind {:excursion 1 :mission 1}}))
                       authority)))))))

(deftest adapter-identity-mutations-refuse-before-construction
  (let [base (input)]
    (is (= :adapter-identities-duplicated
           (:reason (constructor/construct
                     (update base :adapters conj (first (:adapters base)))))))
    (is (= :adapter-target-not-in-field
           (:reason (constructor/construct
                     (update base :adapters conj
                             {:id "T-phantom" :source (pin "T-phantom" "4")})))))))
