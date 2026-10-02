(ns futon2.aif.meta-adapter-discovery-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.meta-adapter-discovery :as discovery]
            [futon2.aif.meta-field-observation :as field]
            [futon2.aif.meta-policy-constructor :as constructor]))

(def texts
  {"/code/repo/holes/M-one.md"
   "# M-one\n\n**Status:** OPEN\n\n- [ ] First explicit task\n\n## Acceptance\n- Criterion two. **Not started.**\n"
   "/code/repo/holes/excursions/E-one.md"
   "# E-one\n\n**Status:** OPEN\n\n## Acceptance\n- Excursion result exists. **Not met.**\n"
   "/code/repo/holes/tickets/T-one.md"
   "# T-one\n\n**Status:** OPEN\n\nUnstructured prose is not a checkable want.\n"})

(defn- utf8 [s] (.getBytes s "UTF-8"))
(defn- row [id path]
  {:id id :status-class :live :status-line "OPEN"
   :requisition {:absent :no-requisition}
   :source {:path path :sha256 (field/sha256 (utf8 (texts path)))}})

(def registry
  {:missions {:missions [(row "M-one" "/code/repo/holes/M-one.md")]}
   :excursions {:excursions [(row "E-one" "/code/repo/holes/excursions/E-one.md")]}
   :tickets {:tickets [(row "T-one" "/code/repo/holes/tickets/T-one.md")]}})

(defn- fixture-input []
  (let [observation (field/observe {:registry-snapshot registry})]
    {:field-observation observation :expected-field-pin (:source-pin observation)
     :code-root "/code"
     :read-bytes #(utf8 (or (texts %) (throw (java.io.FileNotFoundException. %))))
     :read-head-bytes (fn [repo path]
                        (utf8 (or (texts (str "/code/" repo "/" path))
                                  (throw (java.io.FileNotFoundException. path)))))}))

(deftest discovers-canonical-checkbox-and-verdict-locators-once
  (let [in (fixture-input)
        reads (atom [])
        read-bytes (:read-bytes in)
        authority (assoc in :read-bytes #(do (swap! reads conj %) (read-bytes %)))
        receipt (discovery/discover authority)
        by-id (into {} (map (juxt :id identity)) (:adapters receipt))]
    (is (= :discovered (:status receipt)))
    (is (= 3 (:read-count receipt)))
    (is (= 3 (count @reads)))
    (is (= 3 (count (distinct @reads))) "each admitted source is read once")
    (is (= {:field 3 :adapters 2 :excluded 1
            :adapters-by-kind {:excursion 1 :mission 1}
            :excluded-by-kind {:ticket 1}}
           (:counts receipt)))
    (is (= :unchecked-checkbox
           (get-in by-id ["M-one" :evidence :observations 0 :origin])))
    (is (= :verdict-aware-criterion
           (get-in by-id ["M-one" :evidence :observations 1 :origin])))
    (is (= (get-in by-id ["M-one" :evidence :observations 0 :locator])
           (get-in by-id ["M-one" :locator]))
        "unchecked checkbox precedes verdict criterion")
    (is (= #{:C4} (set (map :class (mapcat #(map :locator
                                                  (get-in % [:evidence :observations]))
                                            (:adapters receipt))))))
    (is (= :no-current-false-checkable-want
           (:reason (first (:exclusions receipt)))))
    (is (= :verified (:status (discovery/verify receipt authority))))))

(deftest discovered-current-wants-join-to-advance-not-unblock
  (let [source-authority (fixture-input)
        discovered (discovery/discover source-authority)
        resources {:time-budget-ms 1000 :token-budget 100
                   :author-seat "author" :reviewer-seat "reviewer"}
        construction (constructor/construct
                      {:field-observation (:field-observation source-authority)
                       :expected-field-pin (:expected-field-pin source-authority)
                       :resource-envelope resources
                       :adapters (:adapters discovered)})
        moves (into {} (map (fn [template]
                              [(get-in template [:slots :target])
                               (get-in template [:slots :next-move])]))
                    (:templates construction))]
    (is (= {"E-one" :advance "M-one" :advance} moves)
        "unchecked checkbox and unmet verdict criterion are actionable")
    (is (= ["T-one"] (mapv :id (:exclusions construction)))
        "prose with no checkable locator remains excluded")
    (is (= :evidence-channel-unavailable
           (get-in construction [:exclusions 0 :reason])))))

(deftest drift-and-authority-mutations-refuse
  (let [authority (fixture-input)
        receipt (discovery/discover authority)]
    (testing "bytes are checked before parsing"
      (is (= :source-drift
             (:reason (discovery/discover
                       (assoc authority :read-bytes
                              #(if (= % "/code/repo/holes/M-one.md")
                                 (utf8 "changed") ((:read-bytes authority) %))))))))
    (is (= :external-adapter-source-authority-required
           (:reason (discovery/verify receipt))))
    (testing "omission"
      (is (= :adapter-discovery-does-not-match-authority
             (:reason (discovery/verify (update receipt :adapters pop) authority)))))
    (testing "forged locator"
      (is (= :adapter-discovery-does-not-match-authority
             (:reason (discovery/verify
                       (assoc-in receipt [:adapters 0 :locator :decl] "lie") authority)))))
    (testing "reordered choice"
      (let [index (first (keep-indexed #(when (= "M-one" (:id %2)) %1)
                                       (:adapters receipt)))
            observations (get-in receipt [:adapters index :evidence :observations])
            forged (-> receipt
                       (assoc-in [:adapters index :evidence :observations]
                                 (vec (reverse observations)))
                       (assoc-in [:adapters index :locator] (:locator (last observations))))]
        (is (= :adapter-discovery-does-not-match-authority
               (:reason (discovery/verify forged authority))))))
    (testing "forged exclusion evidence"
      (is (= :adapter-discovery-does-not-match-authority
             (:reason (discovery/verify
                       (assoc-in receipt [:exclusions 0 :evidence]
                                 {:ordering [:forged] :observations []})
                       authority)))))))

(deftest actual-current-excursion-row-uses-its-pinned-bytes
  (let [path "/home/joe/code/futon2/holes/E-wm-algorithms.md"
        bs (java.nio.file.Files/readAllBytes (.toPath (java.io.File. path)))
        row {:id "E-wm-algorithms" :status-class :live :status-line "OPEN"
             :requisition {:absent :no-requisition}
             :source {:path path :sha256 (field/sha256 bs)}}
        registry {:missions {:missions []} :tickets {:tickets []}
                  :excursions {:excursions [row]}}
        observation (field/observe {:registry-snapshot registry})
        reads (atom 0)
        receipt (discovery/discover
                 {:field-observation observation
                  :expected-field-pin (:source-pin observation)
                  :code-root "/home/joe/code"
                  :read-bytes (fn [p] (swap! reads inc)
                                (java.nio.file.Files/readAllBytes (.toPath (java.io.File. p))))})]
    (is (= 1 @reads))
    (is (= 1 (get-in receipt [:counts :adapters])))
    (is (= :C4 (get-in receipt [:adapters 0 :locator :class])))
    (is (= :unchecked-checkbox
           (get-in receipt [:adapters 0 :evidence :observations 0 :origin])))))

(defn- one-row-discovery [text]
  (let [path "/code/repo/holes/M-edge.md"
        bs (utf8 text)
        registry {:missions {:missions [{:id "M-edge" :status-class :live
                                         :status-line "OPEN"
                                         :requisition {:absent :no-requisition}
                                         :source {:path path :sha256 (field/sha256 bs)}}]}
                  :excursions {:excursions []} :tickets {:tickets []}}
        observation (field/observe {:registry-snapshot registry})]
    (discovery/discover {:field-observation observation
                         :expected-field-pin (:source-pin observation)
                         :code-root "/code" :read-bytes (constantly bs)
                         :read-head-bytes (fn [_ _] bs)})))

(deftest canonical-c4-anchoring-controls-current-observation
  (testing "an inline future declaration does not satisfy the criterion locator"
    (let [receipt (one-row-discovery
                   "# M-edge\n\n**Status:** OPEN\n\n## Acceptance\n- Done. **Not started.**\n\nThe future line will be `- Done. **Met.**`.\n")]
      (is (= 1 (get-in receipt [:counts :adapters])))
      (is (false? (get-in receipt [:adapters 0 :evidence :observations 0 :observed])))))
  (testing "a checked form starting its own line makes the duplicate unchecked want true"
    (let [receipt (one-row-discovery
                   "# M-edge\n\n**Status:** OPEN\n\n- [ ] Same task\n- [x] Same task\n")]
      (is (= 0 (get-in receipt [:counts :adapters])))
      (is (= :no-current-false-checkable-want (get-in receipt [:exclusions 0 :reason])))
      (is (true? (get-in receipt [:exclusions 0 :evidence :observations 0 :observed]))))))

(deftest head-source-mismatch-excludes-without-emitting-head-locator
  (let [authority (assoc (fixture-input) :read-head-bytes
                         (fn [_ _] (utf8 "different HEAD bytes")))
        receipt (discovery/discover authority)]
    (is (= 0 (get-in receipt [:counts :adapters])))
    (is (= 3 (get-in receipt [:counts :excluded])))
    (is (= #{:head-source-mismatch} (set (map :reason (:exclusions receipt)))))))

(deftest actual-self-heal-injury-joins-field-discovery-and-construction
  (let [injury-pin
        {:path "/home/joe/code/futon3/library/meta/meta-outer-policy-cascade.edn"
         :sha256 "b1eaaa09a7f16fff9e4ac1c8e43e584b7d187ce348a25458237549983ddb6b86"}
        injury {:schema :wm/injury-observation-v1 :status :active
                :capability :wm-click-completes-with-reviewable-receipts
                :source-pin injury-pin}
        clicks {:schema :wm/ordinary-click-availability-v1
                :authorization {:path "authority.md" :sha "review-fixture"}
                :allocated 2 :consumed 1 :available 1 :unit :ordinary-click
                :ledger-source {:path "/data/consumption.jsonl"
                                :sha256 (apply str (repeat 64 "8"))}}
        empty-registry {:missions {:missions []} :excursions {:excursions []}
                        :tickets {:tickets []}}
        observation (field/observe
                     {:registry-snapshot empty-registry
                      :catalog-path field/default-algorithm-catalog
                      :click-availability clicks
                      :injury-observation injury
                      :expected-injury-pin injury-pin})
        authority {:field-observation observation
                   :expected-field-pin (:source-pin observation)
                   :code-root "/home/joe/code"
                   :read-bytes #(java.nio.file.Files/readAllBytes
                                 (.toPath (java.io.File. %)))}
        discovered (discovery/discover authority)
        resources {:time-budget-ms 1000 :token-budget 100
                   :author-seat "author" :reviewer-seat "reviewer"}
        constructed (constructor/construct
                     {:field-observation observation
                      :expected-field-pin (:source-pin observation)
                      :resource-envelope resources
                      :adapters (:adapters discovered)})]
    (is (= {:algorithm 1 :excursion 0 :mission 0 :ticket 0}
           (:counts observation)))
    (is (= :verified (:status (discovery/verify discovered authority))))
    (is (= :approved-injury-repair-algorithm
           (get-in discovered [:adapters 0 :adapter])))
    (is (= :wm-click-completes-with-reviewable-receipts
           (get-in discovered [:adapters 0 :repairs-capability])))
    (is (= :run-algorithm
           (get-in constructed [:templates 0 :slots :next-move])))
    (is (= :debugger-stop
           (get-in constructed [:templates 0 :slots :stopping-rule])))
    (is (nil? (get-in constructed [:templates 0 :g-input]))
        "the join supplies no prediction, preference, EIG, or G input")))
