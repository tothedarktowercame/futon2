(ns futon2.aif.injury-visibility-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-self-heal-production :as production]))

(def injury-id "injury/visibility")

(defn- transition [ordinal state]
  (ledger/seal-entry
   {:schema ledger/transition-schema
    :ordinal ordinal
    :injury-id injury-id
    :state state}))

(defn- correction [ordinal corrected-ordinal]
  (ledger/seal-entry
   {:schema ledger/observation-schema
    :ordinal ordinal
    :observation-kind :medic-repaired
    :medic-operation
    {:request {:candidate {:injury-id injury-id
                           :entry-id (str corrected-ordinal)}}}}))

(defn- projected [entries]
  (ledger/effective-active-injuries-from-validated-ledger
   (ledger/seal-ledger
    (assoc ledger/empty-ledger :entries entries))))

(defn- observed [active]
  (production/carried-bindings-observation
   {} {:durable-ledger {:status :loaded :ledger ledger/empty-ledger}
       :active-injuries active}))

(deftest corrected-active-injuries-remain-visible
  (let [opened (transition 1 :open)
        corrected (correction 2 1)
        masked-active (projected [opened corrected])
        masked-observation (observed masked-active)
        reopened-active (projected [opened corrected (transition 3 :reopened)])
        reopened-observation (observed reopened-active)
        empty-observation (observed (projected []))]
    (testing "an exact correction masks the effective occurrence, not the raw injury"
      (is (= {:raw-active-count 1
              :effective-active-count 0
              :masked [{:injury-id injury-id
                        :ordinal 1
                        :correction-ordinal 2
                        :correction-pin (:entry-pin corrected)}]
              :selected nil
              :reason :all-active-occurrences-evidence-corrected}
             (select-keys masked-observation
                          [:raw-active-count :effective-active-count :masked
                           :selected :reason])))
      (is (not (str/includes? (pr-str masked-observation) "no-active-injury")))
      (is (= :all-active-occurrences-evidence-corrected
             (:reason (#'production/prepare-diagnosis
                       {} {:durable-ledger {:status :loaded}
                           :active-injuries masked-active})))))
    (testing "a later occurrence reopens the same injury effectively"
      (is (= 1 (:raw-active-count reopened-observation)))
      (is (= 1 (:effective-active-count reopened-observation)))
      (is (= [] (:masked reopened-observation)))
      (is (= injury-id (:selected reopened-observation)))
      (is (= :effective-active-selected (:reason reopened-observation))))
    (testing "an empty ledger is distinguishable from a fully corrected ledger"
      (is (= 0 (:raw-active-count empty-observation)))
      (is (= 0 (:effective-active-count empty-observation)))
      (is (= [] (:masked empty-observation)))
      (is (nil? (:selected empty-observation)))
      (is (= :no-raw-active-injury (:reason empty-observation))))))
