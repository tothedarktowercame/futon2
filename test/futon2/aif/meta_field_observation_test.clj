(ns futon2.aif.meta-field-observation-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.meta-field-observation :as field]))

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
    (is (= :verified (:status (field/verify observation))))
    (is (= observation
           (field/observe {:registry-snapshot current-registry
                           :read-bytes #(throw (ex-info "unexpected read" {}))})))))

(deftest algorithms-require-an-explicit-pinned-approved-catalog
  (let [algorithm-bytes (.getBytes "approved algorithm body" "UTF-8")
        source {:path "/algorithms/A-approved.md"
                :sha256 (field/sha256 algorithm-bytes)}
        catalog-value {:schema :wm/approved-algorithm-catalog-v1
                       :entries [{:id "A-approved" :status :approved :source source}
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
                                    :read-bytes read-bytes})]
    (is (= ["/catalog.edn" "/algorithms/A-approved.md"] @reads)
        "catalog and approved algorithm source are each read once")
    (is (= {:id "A-approved" :kind :algorithm :source source :approved true
            :catalog-source {:path "/catalog.edn" :sha256 (field/sha256 catalog-bytes)}}
           (some #(when (= "A-approved" (:id %)) %) (:rows observation))))
    (is (= :algorithm/not-approved
           (:ineligible-reason
            (some #(when (= "A-candidate" (:id %)) %) (:exclusions observation)))))
    (is (= :verified (:status (field/verify observation))))))

(deftest catalog-and-source-adversarial-mutations-refuse
  (let [bytes (.getBytes "body" "UTF-8")
        catalog (fn [entries] (.getBytes
                               (pr-str {:schema :wm/approved-algorithm-catalog-v1
                                        :entries entries}) "UTF-8"))
        entry {:id "A-one" :status :approved
               :source {:path "/A-one.md" :sha256 (field/sha256 bytes)}}
        run (fn [catalog-bytes algorithm-bytes]
              (field/observe
               {:registry-snapshot current-registry :catalog-path "/catalog.edn"
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
      (is (= :source-drift (:reason (field/verify mutated)))))
    (let [dropped (update observation :rows pop)
          resigned (assoc-in dropped [:source-pin :sha256]
                             (field/digest (dissoc dropped :status :source-pin)))]
      (is (= :registry-row-dropped (:reason (field/verify resigned)))
          "even a recomputed snapshot digest cannot erase registry accounting"))))

(deftest selectable-registry-row-without-source-identity-fails-closed
  (let [broken (assoc-in current-registry [:missions :missions 0 :source]
                         {:path nil :sha256 nil})
        receipt (field/observe {:registry-snapshot broken})]
    (is (= :registry-source-unpinned (:reason receipt)))
    (is (= [{:id "M-open" :kind :mission :source {:path nil :sha256 nil}}]
           (get-in receipt [:details :rows])))))

(deftest current-self-heal-document-is-not-approval-authority
  (let [text (slurp "holes/labs/A-self-heal.md")
        excursion (slurp "holes/E-wm-algorithms.md")]
    (is (re-find #"approved-algorithm candidate" text))
    (is (re-find #"\[ \] \*\*Approved registry" excursion))
    (is (not (.exists (java.io.File. field/default-algorithm-catalog))))))
