(ns futon2.aif.sorry-registry-test
  "Tests for the hand-curated sorry registry — the v1 substrate adapter
   that flips `forward-model/can-propose? :address-sorry` to true."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.action-proposer :as ap]
            [futon2.aif.forward-model :as fm]
            [futon2.aif.sorry-registry :as sr]))

(deftest load-sorrys-default-path-test
  (testing "default-path load returns a document with :schema-version and :sorrys"
    (let [doc (sr/load-sorrys)]
      (is (map? doc))
      (is (= 1 (:schema-version doc)))
      (is (sequential? (:sorrys doc))))))

(deftest open-sorrys-filters-status-test
  (testing "open-sorrys filters to :status :open entries"
    (let [doc {:schema-version 1
               :sorrys [{:id :a :status :open :title "open one"}
                        {:id :b :status :addressed :title "addressed one"}
                        {:id :c :status :foreclosed :title "foreclosed one"}
                        {:id :d :status :open :title "another open"}]}
          openes (sr/open-sorrys doc)]
      (is (= 2 (count openes)))
      (is (= #{:a :d} (set (map :id openes)))))))

(deftest default-registry-loads-and-contains-meta-sorry-test
  ;; The meta-sorry was DESIGNED to be discharged, and was — resolved
  ;; 2026-05-25 by pilot claude-1 (cg-5b03db29, :acknowledged-v1-in-force,
  ;; committed 2026-05-30). The old assertion here pinned it as permanently
  ;; :open, failing the suite from that commit onward. The durable contract:
  ;; the default registry file loads, the meta-sorry entry exists (any
  ;; status), and open-sorrys returns only genuinely-:open entries —
  ;; possibly none: an all-discharged registry is a real state (it is WHY
  ;; the WM surfaces learn-action-class :address-sorry — the substrate ran
  ;; dry on 2026-05-25 and was never refilled).
  (testing "the default-path registry loads and carries the meta-sorry entry"
    (let [doc (sr/load-sorrys)
          ids (set (map :id (:sorrys doc)))]
      (is (seq (:sorrys doc)) "default registry must not be empty")
      (is (contains? ids :sorry/wm-aif-substrate-addressability)
          "meta-sorry entry must exist in the default registry")
      (is (every? #(= :open (:status %)) (sr/open-sorrys))
          "open-sorrys must return only :open entries"))))

(deftest can-propose-address-sorry-when-state-has-sorrys-test
  (testing "can-propose? :address-sorry returns true iff state carries non-empty :sorrys"
    (is (false? (fm/can-propose? {:sorrys []} :address-sorry))
        "empty :sorrys → not proposable")
    (is (false? (fm/can-propose? {} :address-sorry))
        "missing :sorrys → not proposable")
    (is (true? (fm/can-propose? {:sorrys [{:id :x :status :open}]} :address-sorry))
        "one open sorry → proposable")))

(deftest sorry-enumerator-proposer-emits-candidates-test
  (testing "one :address-sorry candidate per sorry in state"
    (let [state {:sorrys [{:id :sorry/a :title "A" :status :open}
                          {:id :sorry/b :title "B" :status :open}]}
          candidates (ap/propose sr/sorry-enumerator-proposer state)]
      (is (= 2 (count candidates)))
      (is (every? #(= :address-sorry (:type %)) candidates))
      (is (= #{:sorry/a :sorry/b} (set (map :target candidates))))))
  (testing "candidates carry :rationale derived from sorry title"
    (let [state {:sorrys [{:id :sorry/m :title "meta question" :status :open}]}
          [c] (ap/propose sr/sorry-enumerator-proposer state)]
      (is (.contains (:rationale c) "meta question")))))

(deftest proposer-id-test
  (testing "sorry-enumerator-proposer has stable identifier"
    (is (= :sorry-enumerator (ap/proposer-id sr/sorry-enumerator-proposer)))))

(deftest empty-state-yields-no-candidates-test
  (testing "no sorrys in state → enumerator emits no candidates"
    (is (empty? (ap/propose sr/sorry-enumerator-proposer {:sorrys []})))
    (is (empty? (ap/propose sr/sorry-enumerator-proposer {})))))

(deftest integration-bootstrap-no-longer-surfaces-address-sorry-gap-test
  (testing "when state has open sorrys, bootstrap-proposer no longer emits :learn-action-class for :address-sorry"
    (let [state-with-sorrys {:sorrys [{:id :sorry/x :status :open :title "x"}]
                             :observation {} :belief {}}
          proposed (ap/propose ap/bootstrap-proposer state-with-sorrys)
          learn-targets (->> proposed
                             (filter #(= :learn-action-class (:type %)))
                             (map :target-class)
                             set)]
      (is (not (contains? learn-targets :address-sorry))
          ":address-sorry should not appear in gaps when state has sorrys")
      (is (seq learn-targets)
          "other action classes are still gated, still surfaced as gaps"))))

(deftest intrinsic-value-for-sorry-per-kind-test
  (testing "intrinsic-value-for-sorry maps :kind to per-kind defaults"
    (is (= 0.4  (sr/intrinsic-value-for-sorry {:kind :meta})))
    (is (= 0.25 (sr/intrinsic-value-for-sorry {:kind :technical-debt})))
    (is (= 0.25 (sr/intrinsic-value-for-sorry {:kind :decision-debt})))
    (is (= 0.15 (sr/intrinsic-value-for-sorry {:kind :external-dependency})))
    (is (= 0.1  (sr/intrinsic-value-for-sorry {:kind :prototyping-forward}))))
  (testing "sorries with no :kind get the neutral default"
    (is (= 0.15 (sr/intrinsic-value-for-sorry {})))
    (is (= 0.15 (sr/intrinsic-value-for-sorry {:id :x :status :open}))))
  (testing "unknown :kind values get the neutral default"
    (is (= 0.15 (sr/intrinsic-value-for-sorry {:kind :totally-unknown})))))

(deftest sorry-candidates-carry-intrinsic-value-per-kind-test
  (testing "§2.E.1.a — :address-sorry candidates differ in intrinsic-value
            based on :kind, breaking the G=-4.208 tie surfaced by VSATARCS Q2 chrome"
    (let [state {:sorrys [{:id :sorry/m :title "meta sorry" :status :open :kind :meta}
                          {:id :sorry/p :title "proto sorry" :status :open :kind :prototyping-forward}
                          {:id :sorry/d :title "debt sorry" :status :open :kind :technical-debt}
                          {:id :sorry/n :title "no-kind sorry" :status :open}]}
          candidates (ap/propose sr/sorry-enumerator-proposer state)
          by-target (into {} (map (juxt :target :intrinsic-value) candidates))]
      (is (= 0.4  (get by-target :sorry/m)))
      (is (= 0.1  (get by-target :sorry/p)))
      (is (= 0.25 (get by-target :sorry/d)))
      (is (= 0.15 (get by-target :sorry/n)))
      (testing "the G-tie is broken: distinct intrinsic-values produce distinct EFE"
        (let [values (set (map :intrinsic-value candidates))]
          (is (= 4 (count values))
              "four kinds → four distinct intrinsic-values"))))))
