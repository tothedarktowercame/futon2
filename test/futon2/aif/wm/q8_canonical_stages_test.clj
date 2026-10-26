(ns futon2.aif.wm.q8-canonical-stages-test
  (:require [clojure.edn :as edn]
            [clojure.set :as set]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.interpretation-evidence :as interpretation-evidence]
            [futon2.aif.previous-run :as previous-run]
            [futon2.aif.q8-frontier :as q8]
            [futon2.aif.selected-outer-cascade-policy :as selected]))

(def carrier
  (edn/read-string
   (slurp "test/resources/q8-canonical-frontier/attempt-002-carrier.edn")))

(def target "M-war-machine-wiring")
(def selected-policy-id "M-war-machine-wiring/policy-2629917c87a61978be8f")

(defn- valid-stages? [stages]
  (let [constructed (set (:constructed stages))
        admitted (set (:admitted stages))
        scored (set (:scored stages))
        posterior (set (:posterior stages))]
    (and (= constructed admitted scored)
         (= posterior (set/difference scored #{})))))

(deftest retained-canonical-carrier-projects-selected-task-stages
  (let [r (selected/canonical-policy-stages carrier)
        stages (get-in r [:stages target])
        policy-key (first (:constructed stages))]
    (is (= #{target} (set (keys (:stages r)))))
    (is (= selected-policy-id (nth policy-key 2)))
    (is (= [policy-key] (:constructed stages) (:admitted stages)
           (:scored stages) (:posterior stages)))
    (is (= "8f01bdcdcb3bfdde6c90fa688aef00b50c1f2632bd32d1e59d3fae9ffce3195f"
           (get-in r [:provenance :menu-pin])))
    (is (= "d20f36b10d394b694a2bf0c0a07190e22e7d9a5a4caba2240ec818b96d0651ed"
           (get-in r [:provenance :frontier-pin])))
    (is (re-matches #"[0-9a-f]{64}" (get-in r [:provenance :q4-pin])))
    (is (= (interpretation-evidence/value-digest
            (vec (get-in carrier [:recomputation :q4])))
           (get-in r [:provenance :q4-pin])))
    (is (valid-stages? stages))))

(deftest cascade-consumes-a-present-canonical-q6-carrier
  (let [r (selected/canonical-policy-stages carrier)
        predecessor (previous-run/seal
                     {:schema previous-run/predecessor-schema
                      :status :present
                      :previous-run/id "previous"
                      :previous {:outcome {:status :present :outcome :already-satisfied}}})
        q6 (previous-run/seal
            {:schema previous-run/exclusion-schema
             :input-digest "canonical-digest"
             :predecessor (select-keys predecessor
                                       [:status :reason :sha256 :previous-run/id])
             :menu 1 :removed 0
             :status :not-triggered :reason :previous-outcome-not-refused})
        decision {:selection-certificate
                  {:policy-stages (:stages r)
                   :stage-provenance (:provenance r)
                   :q6 q6}
                  :selection-law {:candidate "x"}}]
    (is (= [:present #{}]
           ((juxt :status :requirement-excluded)
            (q8/cascade decision predecessor target))))
    (is (= [:not-recomputable :policy-stages-absent]
           ((juxt :status :reason)
            (q8/cascade (update decision :selection-certificate
                                dissoc :policy-stages)
                        nil target))))))

(deftest incomplete-canonical-stages-fail-the-valid-mirror
  (let [selected-entry (first (filter #(= target (:target-id %))
                                      (get-in carrier [:sealed-menu :entries])))
        second-key (assoc (get-in selected-entry [:action :policy-key])
                          2 "M-war-machine-wiring/policy-unscored")
        second-entry (-> selected-entry
                         (assoc :policy-id "M-war-machine-wiring/policy-unscored")
                         (assoc-in [:action :policy-key] second-key)
                         (assoc-in [:action :cascade-digest] "unscored"))
        with-unscored (-> carrier
                          (update-in [:sealed-menu :entries] conj second-entry)
                          (update-in [:canonical-frontier :entries]
                                     conj {:policy-id (:policy-id second-entry)
                                           :target-id target :status :refused
                                           :cascade-digest "unscored"
                                           :action-pin {:cascade-digest "unscored"}}))
        stages (get-in (selected/canonical-policy-stages with-unscored)
                       [:stages target])
        without-q4 (update-in carrier [:recomputation :q4]
                              #(vec (remove (fn [row] (= target (:target-id row))) %)))
        missing-posterior (get-in (selected/canonical-policy-stages without-q4)
                                  [:stages target])]
    (testing "constructed but refused is not scored"
      (is (= 2 (count (:constructed stages))))
      (is (= 1 (count (:scored stages))))
      (is (false? (valid-stages? stages))))
    (testing "a missing q4 row makes posterior a strict subset"
      (is (set/subset? (set (:posterior missing-posterior))
                       (set (:scored missing-posterior))))
      (is (not= (set (:posterior missing-posterior))
                (set (:scored missing-posterior))))
      (is (false? (valid-stages? missing-posterior))))))

(deftest noncanonical-carrier-has-no-canonical-stages
  (is (nil? (selected/canonical-policy-stages
             (assoc carrier :basis-kind :strict-objective)))))
