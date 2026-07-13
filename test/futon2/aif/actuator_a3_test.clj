(ns futon2.aif.actuator-a3-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.actuator-a3 :as a3]
            [futon2.aif.substrate-fixture :as fixture])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(deftest extracts-all-a3-corpus-deposits
  (let [by-id (a3/deposits-by-id)
        packages (mapv #(a3/extract-build-package (get by-id %)) a3/a3-corpus-ids)]
    (is (= 4 (count packages)))
    (is (every? :ok? packages)
        (pr-str (map #(select-keys % [:fold-turn/id :missing]) packages)))
    (is (every? #(seq (:policy-holes %)) packages))
    (is (every? #(get-in % [:structure-spec :wiring :nodes]) packages))
    (is (= a3/a3-corpus-ids (mapv :fold-turn/id packages)))))

(deftest reports-spec-quality-when-typespec-or-structure-missing
  (testing "missing typespec is reported, not silently skipped"
    (let [pkg (a3/extract-build-package
               {:fold-turn/id "ft-weak"
                :mission "futonx-d/mission/weak"
                :wiring {:nodes [{:id :b1}]
                         :terminals [{:id :b1 :discharges :want-signature-missing}]}
                :turn {:answer {:policy-holes []}}})]
      (is (false? (:ok? pkg)))
      (is (= :missing-typespec (-> pkg :missing first :reason)))))
  (testing "missing structure is reported, not silently skipped"
    (let [pkg (a3/extract-build-package
               {:fold-turn/id "ft-weak"
                :mission "futonx-d/mission/weak"
                :arrow-candidate {:have "h" :want "w"}
                :turn {:answer {:policy-holes []}}})]
      (is (false? (:ok? pkg)))
      (is (= :missing-structure (-> pkg :missing first :reason))))))

(deftest derives-proof-query-from-endpoint-binding
  (is (= '{:find [e]
           :where [[e :entity/type :capability]]
           :limit 1}
         (a3/proof-query {:endpoint "CapabilityVocabulary"
                          :kind :entity
                          :type :capability})))
  (is (= '{:find [e]
           :where [[e :hx/type :capability/*]
                   [e :hx/endpoints ep0]
                   [ep0 :entity/type :capability]
                   [e :hx/endpoints ep1]
                   [ep1 :entity/type :mission/doc]]
           :limit 1}
         (a3/proof-query {:endpoint "CapabilityHypergraph"
                          :kind :hyperedge
                          :type :capability/*
                          :endpoint-types [:capability :mission/doc]}))))

(defn- a3-opts []
  (fixture/make-opts
   [{:xt/id "cap" :entity/id "cap" :entity/type :capability}
    {:xt/id "mission" :entity/id "mission" :entity/type :mission/doc}]))

(deftest inhabitation-checks-use-executor-derived-semantic-query
  (let [opts (a3-opts)
        capability (a3/endpoint-inhabitation {:endpoint "CapabilityVocabulary"
                                              :kind :entity
                                              :type :capability}
                                             opts)
        capability-hypergraph (a3/endpoint-inhabitation
                               {:endpoint "CapabilityHypergraph"
                                :kind :hyperedge
                                :type :capability/*
                                :endpoint-types [:capability :mission/doc]}
                               opts)]
    (is (true? (:inhabited? capability)))
    (is (false? (:inhabited? capability-hypergraph)))))

(deftest m-learning-loop-endpoint-dial-is-one-of-two
  (let [deposit (get (a3/deposits-by-id) "ft-learning-loop-010")
        bindings (a3/endpoint-bindings deposit)
        snapshot (a3/endpoint-snapshot bindings (a3-opts))
        dial (a3/endpoint-dial snapshot)]
    (is (= ["CapabilityVocabulary" "CapabilityHypergraph"]
           (mapv :endpoint bindings)))
    (is (= 1 (:inhabited dial)))
    (is (= 2 (:total dial)))
    (is (false? (:discharged? dial)))))

(deftest builder-claim-cannot-inhabit-absent-binding
  (let [absent {:endpoint "BuilderClaimedEndpoint"
                :kind :entity
                :type :a3-provable-witness/absent-test}
        builder-claim {:closures [{:hole-id :h1
                                   :evidence-ref {:kind :file-exists
                                                  :path "/tmp/builder-claim"}}]}
        snapshot (a3/endpoint-snapshot [absent] (a3-opts))
        dial (a3/endpoint-dial snapshot)]
    (is (seq (:closures builder-claim)))
    (is (= 0 (:inhabited dial)))
    (is (= 1 (:total dial)))
    (is (false? (:discharged? dial)))
    (is (false? (-> snapshot first :inhabited?)))))

(deftest review-partial-reports-endpoint-breakdown
  (let [before [{:endpoint "CapabilityVocabulary" :inhabited? false}
                {:endpoint "CapabilityHypergraph" :inhabited? false}]
        after [{:endpoint "CapabilityVocabulary"
                :kind :entity
                :type :capability
                :query (a3/proof-query {:kind :entity :type :capability})
                :inhabited? true}
               {:endpoint "CapabilityHypergraph"
                :kind :hyperedge
                :type :capability/*
                :endpoint-types [:capability :mission/doc]
                :query (a3/proof-query {:kind :hyperedge
                                         :type :capability/*
                                         :endpoint-types [:capability :mission/doc]})
                :inhabited? false
                :reason :not-inhabited}]
        r (a3/review-partial {:before before :after after} {})]
    (is (true? (:dial-moved? r)))
    (is (= 1 (:inhabited r)))
    (is (= 2 (:total r)))
    (is (= ["CapabilityVocabulary"] (:newly-inhabited r)))
    (is (= ["CapabilityVocabulary"] (mapv :endpoint (:resolved r))))
    (is (= [{:endpoint "CapabilityHypergraph"
             :kind :hyperedge
             :type :capability/*
             :endpoint-types [:capability :mission/doc]
             :query (a3/proof-query {:kind :hyperedge
                                      :type :capability/*
                                      :endpoint-types [:capability :mission/doc]})
             :reason :not-inhabited}]
           (:rejected r)))
    (is (= 1 (:endpoints-remaining r)))
    (is (str/includes? (:next-feedback r) "CapabilityHypergraph"))))

(defn- tmp-doc! [text]
  (let [dir (.toFile (Files/createTempDirectory "a3-doc-test"
                                                (make-array FileAttribute 0)))
        f (io/file dir "M-fixture.md")]
    (spit f text)
    (.getPath f)))

(defn- tmp-dir-path! [prefix]
  (.getPath (.toFile (Files/createTempDirectory prefix
                                                (make-array FileAttribute 0)))))

(defn- uuid-suffix []
  (str (java.util.UUID/randomUUID)))

(defn- write-test-entity! [opts entity-id type]
  (a3/drawbridge-submit-tx!
   [[:xtdb.api/put {:xt/id entity-id
                    :entity/id entity-id
                    :entity/name entity-id
                    :entity/type type
                    :entity/source "actuator-a3-test"
                    :entity/props {:a4-test? true
                                   :evict-after-proof true}}]]
   opts))

(defn- write-test-hyperedge! [opts hx-id type endpoints]
  (a3/drawbridge-submit-tx!
   [[:xtdb.api/put {:xt/id hx-id
                    :hx/id hx-id
                    :hx/type type
                    :hx/endpoints endpoints
                    :a4-test? true
                    :a4-test/evict-after-proof true}]]
   opts))

(defn- learning-loop-clean []
  (edn/read-string (slurp "/home/joe/code/futon6/holes/clean/M-learning-loop.clean.edn")))

(defn- learning-loop-deposit-with-clean []
  (assoc (get (a3/deposits-by-id) "ft-learning-loop-010")
         :clean (learning-loop-clean)))

(deftest build-match-learning-loop-dial-is-one-of-two
  (let [match (a3/build-match (learning-loop-deposit-with-clean) (a3-opts))
        by-box (into {} (map (juxt :box identity) (:box-snapshot match)))]
    (is (true? (:clean-pass? match)))
    (is (= {:inhabited 1
            :bound 2
            :clean-pass? true
            :discharged? false}
           (:dial match)))
    (is (true? (get-in by-box [:b1 :inhabited?])))
    (is (= :capability (get-in by-box [:b1 :type])))
    (is (false? (get-in by-box [:b7 :inhabited?])))
    (is (= [:b7] (:missing-boxes match)))))

(deftest build-match-catches-missing-interior-when-terminal-inhabited
  (let [suffix (uuid-suffix)
        opts (a3-opts)
        hx-id (str "a4-test/hyperedge/build-match-" suffix)
        missing-type (keyword "a4-test" (str "missing-interior-" suffix))
        deposit (assoc (learning-loop-deposit-with-clean)
                       :box-bindings {:b1 {:kind :entity :type missing-type}
                                      :b7 {:kind :hyperedge
                                           :type :capability/*
                                           :endpoint-types [:capability :mission/doc]}})]
    (write-test-hyperedge! opts hx-id :capability/* ["cap" "mission"])
      (let [terminal (a3/endpoint-inhabitation {:kind :hyperedge
                                                :type :capability/*
                                                :endpoint-types [:capability :mission/doc]}
                                               opts)
            match (a3/build-match deposit opts)
            by-box (into {} (map (juxt :box identity) (:box-snapshot match)))]
        (is (true? (:inhabited? terminal)) "boundary-only terminal proof would pass")
        (is (true? (get-in by-box [:b7 :inhabited?])))
        (is (false? (get-in by-box [:b1 :inhabited?])))
        (is (= [:b1] (:missing-boxes match)))
        (is (false? (get-in match [:dial :discharged?]))))))

(deftest build-match-refuses-invalid-clean
  (let [bad-clean (assoc-in (learning-loop-clean)
                            [:clean/wires 0 :carries]
                            :not-produced-or-consumed)
        match (a3/build-match (assoc (get (a3/deposits-by-id) "ft-learning-loop-010")
                                     :clean bad-clean))]
    (is (false? (:clean-pass? match)))
    (is (true? (:refused? match)))
    (is (= :clean-invalid (:reason match)))
    (is (str/includes? (:message match) "G5"))
    (is (false? (get-in match [:dial :discharged?])))))

(deftest record-discharge-writes-provable-entity
  (let [suffix (uuid-suffix)
        opts (a3-opts)
        mission "futon-test-d/mission/a4-record"
        binding {:endpoint (str "Endpoint-" suffix)
                 :kind :entity
                 :type (keyword "a4-test" (str "record-" suffix))}
        recorded (a3/record-discharge! mission binding opts)
        discharge-id (get-in recorded [:doc :xt/id])]
      (is (a3/discharge-recorded? mission (:endpoint binding) opts))
      (is (= :discharge (get-in recorded [:doc :entity/type])))
      (is (= (pr-str (a3/proof-query binding))
             (get-in recorded [:doc :discharge/proof-query])))
      (is (string? (str discharge-id)))))

(deftest finalize-discharge-records-only-newly-inhabited-endpoint
  (let [suffix (uuid-suffix)
        opts (assoc (a3-opts) :before-dir (tmp-dir-path! "a4-before-test"))
        mission (str "futon-test-d/mission/a4-finalize-" suffix)
        endpoint (str "Endpoint-" suffix)
        type (keyword "a4-test" (str "finalize-" suffix))
        entity-id (str "a4-test/entity/" suffix)
        binding {:endpoint endpoint :kind :entity :type type}
        before (a3/capture-before-snapshot! mission [binding] opts)]
        (is (false? (-> before :before first :inhabited?)))
        (write-test-entity! opts entity-id type)
        (let [first-finalize (a3/finalize-discharge! mission opts)
              second-finalize (a3/finalize-discharge! mission opts)]
          (is (= [endpoint] (get-in first-finalize [:dial-review :newly-inhabited])))
          (is (= 1 (count (:recorded first-finalize))))
          (is (empty? (:already-recorded first-finalize)))
          (is (empty? (:recorded second-finalize)))
          (is (= [endpoint] (:already-recorded second-finalize))))))

(deftest finalize-discharge-does-not-record-already-inhabited-before
  (let [suffix (uuid-suffix)
        opts (assoc (a3-opts) :before-dir (tmp-dir-path! "a4-before-test"))
        mission (str "futon-test-d/mission/a4-already-" suffix)
        endpoint (str "Endpoint-" suffix)
        type (keyword "a4-test" (str "already-" suffix))
        entity-id (str "a4-test/entity/" suffix)
        binding {:endpoint endpoint :kind :entity :type type}]
    (write-test-entity! opts entity-id type)
      (let [before (a3/capture-before-snapshot! mission [binding]
                                                opts)
            finalized (a3/finalize-discharge! mission opts)]
        (is (true? (-> before :before first :inhabited?)))
        (is (empty? (get-in finalized [:dial-review :newly-inhabited])))
        (is (empty? (:recorded finalized)))
        (is (empty? (:already-recorded finalized))))))

(deftest finalize-discharge-rejects-missing-before-snapshot
  (let [mission (str "futon-test-d/mission/a4-missing-" (uuid-suffix))
        before-dir (tmp-dir-path! "a4-before-test")]
    (try
      (a3/finalize-discharge! mission {:before-dir before-dir})
      (is false "expected missing before-snapshot failure")
      (catch clojure.lang.ExceptionInfo e
        (is (= :missing-before-snapshot (-> e ex-data :reason)))
        (is (str/includes? (ex-message e) "missing A4 before snapshot"))))))

(def marker-a "build the capability vocabulary")
(def marker-b "wire typed capability observations")

(defn closure
  ([hole-id marker] (closure hole-id marker true))
  ([hole-id marker resolved?]
   {:hole-id hole-id
    :evidence-ref {:kind :file-exists :path (str "/tmp/" (name hole-id))}
    :closes-mission-hole marker
    :resolved? resolved?}))

(defn resolver-from-closure-flag [ref]
  (boolean (:resolved? ref)))

(deftest advance-mission-doc-closes-only-on-resolving-witness-and-present-hole
  (let [doc (tmp-doc! (str "# Fixture\n\n- [ ] " marker-a "\n- [ ] " marker-b "\n"))
        r (a3/review-partial
           {:mission-doc-path doc
            :closures [(closure :h1 marker-a true)]}
           {:resolver (fn [_] true)})]
    (is (true? (:dial-moved? r)))
    (is (= 2 (:before-open-hole-count r)))
    (is (= 1 (:after-open-hole-count r)))
    (is (= [marker-a] (mapv :closes-mission-hole (:holes-closed r))))
    (is (str/includes? (slurp doc) (str "- [x] " marker-a)))))

(deftest resolving-witness-with-absent-hole-does-not-move-dial
  (let [doc (tmp-doc! (str "# Fixture\n\n- [ ] " marker-a "\n"))
        r (a3/review-partial
           {:mission-doc-path doc
            :closures [(closure :h1 "not present in doc" true)]}
           {:resolver (fn [_] true)})]
    (is (false? (:dial-moved? r)))
    (is (= 1 (:before-open-hole-count r)))
    (is (= 1 (:after-open-hole-count r)))
    (is (= :hole-text-not-in-doc (-> r :rejected first :reason)))
    (is (str/includes? (slurp doc) (str "- [ ] " marker-a)))))

(deftest missing-or-unresolved-witness-with-present-hole-does-not-move-dial
  (testing "missing evidence"
    (let [doc (tmp-doc! (str "# Fixture\n\n- [ ] " marker-a "\n"))
          r (a3/review-partial
             {:mission-doc-path doc
              :closures [{:hole-id :h1 :closes-mission-hole marker-a}]}
             {:resolver (fn [_] true)})]
      (is (false? (:dial-moved? r)))
      (is (= :missing-evidence-ref (-> r :rejected first :reason)))
      (is (str/includes? (slurp doc) (str "- [ ] " marker-a)))))
  (testing "unresolved evidence"
    (let [doc (tmp-doc! (str "# Fixture\n\n- [ ] " marker-a "\n"))
          r (a3/review-partial
             {:mission-doc-path doc
              :closures [(closure :h1 marker-a false)]}
             {:resolver (fn [_] false)})]
      (is (false? (:dial-moved? r)))
      (is (= :unresolved-evidence-ref (-> r :rejected first :reason)))
      (is (str/includes? (slurp doc) (str "- [ ] " marker-a))))))

(deftest advance-mission-doc-is-idempotent
  (let [doc (tmp-doc! (str "# Fixture\n\n- [ ] " marker-a "\n"))
        c [(closure :h1 marker-a true)]
        first-run (a3/review-partial {:mission-doc-path doc :closures c}
                                     {:resolver (fn [_] true)})
        second-run (a3/review-partial {:mission-doc-path doc :closures c}
                                      {:resolver (fn [_] true)})]
    (is (true? (:dial-moved? first-run)))
    (is (false? (:dial-moved? second-run)))
    (is (= 0 (:after-open-hole-count first-run)))
    (is (= 0 (:after-open-hole-count second-run)))
    (is (= :hole-already-closed (-> second-run :rejected first :reason)))))

(deftest review-partial-breaks-down-mixed-set
  (let [doc (tmp-doc! (str "# Fixture\n\n- [ ] " marker-a "\n- [ ] " marker-b "\n"))
        r (a3/review-partial
           {:mission-doc-path doc
            :closures [(closure :h1 marker-a true)
                       (closure :h2 "absent marker" true)
                       (closure :h3 marker-b false)]}
           {:resolver (fn [ref] (not= "/tmp/h3" (:path ref)))})]
    (is (true? (:dial-moved? r)))
    (is (= [marker-a] (mapv :closes-mission-hole (:resolved r))))
    (is (= [:hole-text-not-in-doc :unresolved-evidence-ref]
           (mapv :reason (:rejected r))))
    (is (= [(str "- [ ] " marker-b)] (:holes-remaining r)))
    (is (str/includes? (:next-feedback r) ":hole-text-not-in-doc"))
    (is (str/includes? (:next-feedback r) marker-b))))

(deftest dry-run-over-corpus-is-deterministic
  (let [a (with-out-str
            (doseq [pkg (:packages (a3/run-a3! {:dry-run? true :all-corpus? true}))]
              (println (a3/render-package pkg))))
        b (with-out-str
            (doseq [pkg (:packages (a3/run-a3! {:dry-run? true :all-corpus? true}))]
              (println (a3/render-package pkg))))]
    (is (= a b))
    (is (str/includes? a "ft-learning-loop-010"))
    (is (str/includes? a "ft-pattern-mining-011"))))

(deftest result-truthy-rejects-empty-witness-values
  ;; the Drawbridge gaming-vector fix: a query that "ran fine" but found nothing
  ;; (count 0, empty seq, nil, false, blank) must NOT count as a resolved witness.
  (testing "falsy / empty results do not witness existence"
    (is (false? (a3/result-truthy? nil)))
    (is (false? (a3/result-truthy? false)))
    (is (false? (a3/result-truthy? 0)))
    (is (false? (a3/result-truthy? [])))
    (is (false? (a3/result-truthy? "")))
    (is (false? (a3/result-truthy? "  "))))
  (testing "genuine existence results witness"
    (is (true? (a3/result-truthy? 1)))
    (is (true? (a3/result-truthy? true)))
    (is (true? (a3/result-truthy? [:x])))
    (is (true? (a3/result-truthy? "found")))))
