(ns futon2.aif.meta-field-observation-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.meta-field-observation :as field]
            [futon2.aif.meta-injury-observation :as injury]))

(defn- pin [path ch]
  {:path path :sha256 (apply str (repeat 64 ch))})

(defn- task [id status-class requisition ch]
  {:id id :status-class status-class :status-line (name status-class)
   :requisition requisition :source (pin (str "/code/holes/" id ".md") ch)})

(def current-registry
  {:missions
   {:missions [(task "M-open" :live {:absent :no-requisition} "a")
               (task "M-done" :complete {:absent :no-requisition} "b")]}
   :excursions
   {:excursions [(task "E-open" :live {:state :pending :raw "pending"} "c")
                 (task "E-running" :live {:state :in-progress :raw "in-progress"} "d")]}
   :tickets
   {:tickets [(task "T-open" :live {:absent :no-requisition} "e")]}})

(defn- resign [observation]
  (assoc-in observation [:source-pin :sha256]
            (field/digest (dissoc observation :status :source-pin))))

(defn- verify-produced [observation]
  (field/verify observation {:expected-snapshot-pin (:source-pin observation)}))

(def positive-clicks
  {:schema :wm/ordinary-click-availability-v1
   :authorization {:path "authority.md" :sha "deadbeef"}
   :allocated 5 :consumed 3 :available 2 :unit :ordinary-click
   :ledger-source (pin "/data/consumption.jsonl" "8")})

(def injury-record
  {:schema :wm/injury-evidence-v1
   :source-record (pin "/data/wm-runs/source.edn" "5")
   :run-id "run-1" :click-id "click-1"
   :failure {:kind :abstained :stage :selection
             :detail-kind :wm/selection-terminal-abstention}
   :terminal-receipt {:kind :failure :failure-kind :abstained}
   :outer-task-selection {:schema :wm/outer-task-selection-v1
                          :status :absent :reason :receipt-not-retained}
   :loop-node-exercise {:schema :wm/loop-node-exercise-v1 :status :incomplete
                        :counts {:present 0 :bypassed 0 :refused 4}}
   :run-output {:schema :wm/run-output-v1 :status :absent
                :reason :run-not-grounded :outcome :abstained}
   :trace-written false})
(def injury-bytes (.getBytes (pr-str injury-record) "UTF-8"))
(def injury-pin {:path "/data/wm-runs/injured.edn"
                 :sha256 (field/sha256 injury-bytes)})
(def injury-authority {:source-bytes injury-bytes :expected-source-pin injury-pin})

(deftest current-registry-is-accounted-without-a-second-lifecycle-policy
  (let [reads (atom [])
        observation (field/observe
                     {:registry-snapshot current-registry
                      :read-bytes #(do (swap! reads conj %) (throw (ex-info "unexpected read" {})))})]
    (is (= :observed (:status observation)))
    (is (= {:algorithm 0 :excursion 1 :mission 1 :ticket 1} (:counts observation)))
    (is (= #{"M-open" "E-open" "T-open"} (set (map :id (:rows observation)))))
    (is (= #{:registry/lifecycle-ineligible :requisition/in-progress
             :algorithm/catalog-unavailable}
           (set (map :ineligible-reason (:exclusions observation)))))
    (is (= 5 (get-in observation [:registry-proof :observed-total])))
    (is (= 5 (get-in observation [:registry-proof :accounted-total])))
    (is (= [] @reads) "registry pins are retained; task files are not reread")
    (is (= :external-snapshot-authority-required
           (:reason (field/verify observation))))
    (is (= :verified (:status (verify-produced observation))))
    (is (= observation
           (field/observe {:registry-snapshot current-registry
                           :read-bytes #(throw (ex-info "unexpected read" {}))})))))

(deftest algorithms-require-an-explicit-pinned-approved-catalog
  (let [algorithm-bytes (.getBytes "approved algorithm body" "UTF-8")
        source {:path "/algorithms/A-approved.md"
                :sha256 (field/sha256 algorithm-bytes)}
        catalog-value {:schema :wm/approved-algorithm-catalog-v1
                       :entries [{:id "A-approved" :status :approved :source source
                                  :repairs-capability
                                  :wm-click-completes-with-reviewable-receipts
                                  :evidence-locator {:kind :debugger-stop-and-terminal-run-record
                                                     :id :wm-click-completes-with-reviewable-receipts}
                                  :resource-requirements {:ordinary-clicks 1}}
                                 {:id "A-candidate" :status :candidate
                                  :source (pin "/algorithms/A-candidate.md" "9")}]}
        catalog-bytes (.getBytes (pr-str catalog-value) "UTF-8")
        reads (atom [])
        read-bytes (fn [path]
                     (swap! reads conj path)
                     (case path
                       "/catalog.edn" catalog-bytes
                       "/algorithms/A-approved.md" algorithm-bytes
                       (throw (java.io.FileNotFoundException. path))))
        observation (field/observe {:registry-snapshot current-registry
                                    :catalog-path "/catalog.edn"
                                    :click-availability positive-clicks
                                    :injury-authority injury-authority
                                    :read-bytes read-bytes})]
    (is (= ["/catalog.edn" "/algorithms/A-approved.md"] @reads)
        "catalog and approved algorithm source are each read once")
    (is (= positive-clicks
           (get-in (some #(when (= "A-approved" (:id %)) %) (:rows observation))
                   [:resource-admission :availability])))
    (is (= :algorithm/not-approved
           (:ineligible-reason
            (some #(when (= "A-candidate" (:id %)) %) (:exclusions observation)))))
    (is (= 2 (get-in observation [:algorithm-catalog-proof :entry-total])))
    (is (= :verified (:status (verify-produced observation))))
    (testing "re-signed dropped approved algorithm is caught by catalog accounting"
      (let [authority (:source-pin observation)
            mutated (-> observation
                        (update :rows #(filterv (fn [row] (not= "A-approved" (:id row))) %))
                        (assoc-in [:counts :algorithm] 0)
                        resign)]
        (is (= :external-snapshot-pin-mismatch
               (:reason (field/verify mutated {:expected-snapshot-pin authority}))))))
    (testing "re-signed dropped unapproved algorithm is caught by catalog accounting"
      (let [authority (:source-pin observation)
            mutated (-> observation
                        (update :exclusions
                                #(filterv (fn [row] (not= "A-candidate" (:id row))) %))
                        resign)]
        (is (= :external-snapshot-pin-mismatch
               (:reason (field/verify mutated {:expected-snapshot-pin authority}))))))
    (testing "rewriting the complete catalog proof and re-signing is not authority"
      (let [authority (:source-pin observation)
            forged (-> observation
                       (update :rows #(filterv (fn [row] (not= "A-approved" (:id row))) %))
                       (update :exclusions #(filterv (fn [row] (not= "A-candidate" (:id row))) %))
                       (assoc :algorithm-catalog-proof {:entry-total 0 :entries []})
                       (assoc-in [:counts :algorithm] 0)
                       resign)]
        (is (= :external-snapshot-pin-mismatch
               (:reason (field/verify forged {:expected-snapshot-pin authority}))))))))

(deftest catalog-and-source-adversarial-mutations-refuse
  (let [bytes (.getBytes "body" "UTF-8")
        catalog (fn [entries] (.getBytes
                               (pr-str {:schema :wm/approved-algorithm-catalog-v1
                                        :entries entries}) "UTF-8"))
        entry {:id "A-one" :status :approved
               :repairs-capability :wm-click-completes-with-reviewable-receipts
               :evidence-locator {:kind :debugger-stop-and-terminal-run-record
                                  :id :wm-click-completes-with-reviewable-receipts}
               :resource-requirements {:ordinary-clicks 1}
               :source {:path "/A-one.md" :sha256 (field/sha256 bytes)}}
        run (fn [catalog-bytes algorithm-bytes]
              (field/observe
               {:registry-snapshot current-registry :catalog-path "/catalog.edn"
                :click-availability positive-clicks
                :injury-authority injury-authority
                :read-bytes #(case % "/catalog.edn" catalog-bytes
                                    "/A-one.md" algorithm-bytes)}))]
    (testing "algorithm file drift"
      (is (= :source-drift
             (:reason (run (catalog [entry]) (.getBytes "changed" "UTF-8"))))))
    (testing "duplicate approved identity"
      (is (= :duplicate-identities
             (:reason (run (catalog [entry entry]) bytes)))))
    (testing "catalog/file shape mismatch"
      (is (= :algorithm-catalog-file-mismatch
             (:reason (run (catalog [(dissoc entry :source)]) bytes)))))
    (testing "algorithm identity cannot collide with a task identity"
      (let [collision (assoc entry :id "M-open")]
        (is (= :duplicate-identities
               (:reason (run (catalog [collision]) bytes))))))))

(deftest whole-snapshot-pin-detects-dropped-and-altered-rows
  (let [observation (field/observe {:registry-snapshot current-registry})]
    (doseq [mutated [(update observation :rows pop)
                     (assoc-in observation [:rows 0 :id] "M-forged")
                     (assoc observation :counts {:mission 999})]]
      (is (= :source-drift
             (:reason (field/verify mutated
                                    {:expected-snapshot-pin (:source-pin observation)})))))
    (let [dropped (-> observation
                      (update :rows pop)
                      (update-in [:counts :ticket] dec))
          resigned (resign dropped)]
      (is (= :external-snapshot-pin-mismatch
             (:reason (field/verify resigned
                                    {:expected-snapshot-pin (:source-pin observation)})))
          "even a recomputed snapshot digest cannot erase registry accounting"))
    (let [bad-counts (resign (assoc observation :counts {:mission 999}))]
      (is (= :external-snapshot-pin-mismatch
             (:reason (field/verify bad-counts
                                    {:expected-snapshot-pin (:source-pin observation)})))))
    (testing "rewriting the entire registry proof and re-signing is not authority"
      (let [authority (:source-pin observation)
            forged (-> observation
                       (update :rows #(filterv (fn [row] (not= "T-open" (:id row))) %))
                       (assoc-in [:counts :ticket] 0)
                       (assoc :registry-proof
                              {:observed-total 4 :accounted-total 4
                               :all-task-identities
                               ["E-open" "E-running" "M-done" "M-open"]})
                       resign)]
        (is (= :external-snapshot-pin-mismatch
               (:reason (field/verify forged {:expected-snapshot-pin authority}))))))))

(deftest selectable-pathless-substrate-identity-is-retained-as-an-exclusion
  (let [broken (assoc-in current-registry [:missions :missions 0 :source]
                         {:path nil :sha256 nil})
        observation (field/observe {:registry-snapshot broken})
        excluded (some #(when (= "M-open" (:id %)) %) (:exclusions observation))]
    (is (= :observed (:status observation)))
    (is (= :registry/source-unavailable (:ineligible-reason excluded)))
    (is (= {:path nil :sha256 nil}
           (get-in excluded [:ineligibility-evidence :retained-source])))
    (is (not (some #(= "M-open" (:id %)) (:rows observation))))
    (is (= 5 (get-in observation [:registry-proof :observed-total])
           (get-in observation [:registry-proof :accounted-total])))
    (is (= :verified (:status (verify-produced observation))))))

(deftest duplicate-identities-include-registry-exclusions
  (let [duplicate (task "M-open" :complete {:absent :no-requisition} "f")
        broken (update-in current-registry [:missions :missions] conj duplicate)]
    (is (= :duplicate-identities
           (:reason (field/observe {:registry-snapshot broken}))))))

(deftest current-self-heal-approval-is-catalogued
  (let [text (slurp "holes/labs/A-self-heal.md")
        excursion (slurp "holes/E-wm-algorithms.md")
        catalog (edn/read-string (slurp field/default-algorithm-catalog))]
    (is (re-find #"approved algorithm" text))
    (is (re-find #"\[x\] \*\*Approved registry" excursion))
    (is (= :wm/approved-algorithm-catalog-v1 (:schema catalog)))
    (is (= [{:id "A-self-heal" :status :approved}]
           (mapv #(select-keys % [:id :status]) (:entries catalog))))))

(deftest approved-algorithm-requires-positive-source-pinned-click-ration
  (let [body (.getBytes "algorithm" "UTF-8")
        source {:path "/A.md" :sha256 (field/sha256 body)}
        catalog (.getBytes
                 (pr-str {:schema :wm/approved-algorithm-catalog-v1
                          :entries [{:id "A" :status :approved :source source
                                     :repairs-capability
                                     :wm-click-completes-with-reviewable-receipts
                                     :evidence-locator
                                     {:kind :debugger-stop-and-terminal-run-record
                                      :id :wm-click-completes-with-reviewable-receipts}
                                     :resource-requirements {:ordinary-clicks 1}}]})
                 "UTF-8")
        run (fn [availability]
              (field/observe
               {:registry-snapshot current-registry :catalog-path "/catalog.edn"
                :click-availability availability
                :injury-authority injury-authority
                :read-bytes #(case % "/catalog.edn" catalog "/A.md" body)}))]
    (doseq [bad [nil
                 (assoc positive-clicks :available 0 :consumed 5)
                 (assoc positive-clicks :available 1 :consumed 3)
                 (assoc positive-clicks :ledger-source {:path "/ledger"})]]
      (let [observation (run bad)
            excluded (some #(when (= "A" (:id %)) %) (:exclusions observation))]
        (is (zero? (get-in observation [:counts :algorithm])))
        (is (= :algorithm/click-resource-unavailable
               (:ineligible-reason excluded)))
        (is (= :verified (:status (verify-produced observation))))))
    (let [observation (run positive-clicks)]
      (is (= 1 (get-in observation [:counts :algorithm])))
      (is (= positive-clicks
             (get-in (first (filter #(= "A" (:id %)) (:rows observation)))
                     [:resource-admission :availability]))))))

(deftest approved-algorithm-requires-exact-reconstructed-injury
  (let [body (.getBytes "algorithm" "UTF-8")
        source {:path "/A.md" :sha256 (field/sha256 body)}
        catalog (.getBytes
                 (pr-str {:schema :wm/approved-algorithm-catalog-v1
                          :entries [{:id "A" :status :approved :source source
                                     :repairs-capability
                                     :wm-click-completes-with-reviewable-receipts
                                     :evidence-locator
                                     {:kind :debugger-stop-and-terminal-run-record
                                      :id :wm-click-completes-with-reviewable-receipts}
                                     :resource-requirements {:ordinary-clicks 1}}]})
                 "UTF-8")
        run (fn [authority]
              (field/observe
               {:registry-snapshot current-registry :catalog-path "/catalog.edn"
                :click-availability positive-clicks
                :injury-authority authority
                :read-bytes #(case % "/catalog.edn" catalog "/A.md" body)}))]
    (doseq [authority
            [nil
             (assoc injury-authority :source-bytes
                    (.getBytes (pr-str (assoc injury-record :trace-written true)) "UTF-8"))
             (assoc injury-authority :source-bytes
                    (.getBytes (pr-str (assoc-in injury-record
                                                [:failure :detail-kind]
                                                :wm/network-unavailable)) "UTF-8"))
             (assoc injury-authority :expected-source-pin (pin "/other.edn" "6"))]]
      (let [observation (run authority)
            excluded (some #(when (= "A" (:id %)) %) (:exclusions observation))]
        (is (zero? (get-in observation [:counts :algorithm])))
        (is (= :algorithm/injury-capability-unavailable
               (:ineligible-reason excluded)))
        (is (= :verified (:status (verify-produced observation))))))
    (let [observation (run injury-authority)
          row (some #(when (= "A" (:id %)) %) (:rows observation))]
      (is (= 1 (get-in observation [:counts :algorithm])))
      (is (= :wm-click-completes-with-reviewable-receipts
             (:repairs-capability row)))
      (is (= :active
             (get-in row [:algorithm-admission :injury-observation :status])))
      (is (= injury-pin
             (get-in row [:algorithm-admission :injury-observation :source-pin]))))))

(deftest injury-observation-reconstruction-rejects-self-signed-claims
  (let [observation (injury/produce injury-authority)
        resign (fn [x]
                 (assoc x :observation-pin
                        {:path "wm://meta-injury-observation-v1"
                         :sha256 (field/digest (dissoc x :observation-pin))}))]
    (is (= :verified (:status (injury/verify observation injury-authority))))
    (testing "changing capability and re-signing"
      (is (= :injury-observation-does-not-match-source
             (:reason (injury/verify (resign (assoc observation :capability :network))
                                     injury-authority)))))
    (testing "substituting the META cascade pin"
      (let [cascade-pin {:path "/home/joe/code/futon3/library/meta/meta-outer-policy-cascade.edn"
                         :sha256 "b1eaaa09a7f16fff9e4ac1c8e43e584b7d187ce348a25458237549983ddb6b86"}]
        (is (= :injury-observation-does-not-match-source
               (:reason (injury/verify observation
                                       (assoc injury-authority
                                              :expected-source-pin cascade-pin)))))))
    (testing "active status without retained failure evidence"
      (is (= :injury-observation-does-not-match-source
             (:reason (injury/verify
                       (resign (assoc observation :evidence {})) injury-authority)))))
    (testing "dropping one required injury evidence field"
      (is (= :injury-observation-does-not-match-source
             (:reason (injury/verify
                       (resign (update observation :evidence dissoc :run-output))
                       injury-authority)))))))
