(ns futon2.aif.meta-live-outer-selector-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.meta-field-observation :as field]
            [futon2.aif.meta-live-outer-selector :as live]))

(defn pin [path ch] {:path path :sha256 (apply str (repeat 64 ch))})
(def snapshot
  {:schema :wm/pipeline-cascade-snapshot-v1
   :summary-source (pin "summary" "a") :graph-source (pin "graph" "b")
   :summary {:consistent? true
             :standards {:s1-regenerates true :s2-evidence true
                         :s3-reconstitution true :s4-honest-holes true
                         :s5-composed true}}
   :graph {:section-status {:clusters {:status :ok}}
           :lineage [] :clusters [{:mission "M-a"} {:mission "M-b"}]
           :arrows [] :held []
           :tickets {:items [{:stem "M-a" :mtime-ms 200}
                             {:stem "M-b" :mtime-ms 100}]}
           :patterns {:edges []}}})
(def tasks
  [{:id "M-a" :kind :mission :priority 2 :source (pin "M-a.md" "c")}
   {:id "M-b" :kind :mission :priority 8 :source (pin "M-b.md" "d")}])

(deftest live-adapter-selects-and-translates-without-a-draw
  (let [receipt (live/select-live {:tasks tasks :fetch-snapshot (constantly snapshot)})]
    (is (= :wm/outer-task-selection-v1 (:schema receipt)))
    (is (= :meta-live-pipeline-task-state (get-in receipt [:policy :kind])))
    (is (= "M-a" (get-in receipt [:chosen :id])))
    (is (= {:type :advance-mission :target "M-a"} (:action receipt)))
    (is (= {:absent :deterministic-meta-policy} (:draw receipt)))
    (is (= :minimum-pairwise-task-state-G
           (get-in receipt [:policy :meta-selection :reason])))
    (is (contains? (set (get-in receipt [:policy :meta-selection
                                         :pairwise 0 :shared-channels]))
                   :pipeline-freshness-cost))
    (is (= ["M-a" "M-b"]
           (mapv :id (get-in receipt [:policy :meta-selection :ranking]))))))

(deftest missing-or-equal-evidence-refuses-with-complete-support
  (testing "missing is absence"
    (let [receipt (live/select-live {:tasks (mapv #(dissoc % :priority) tasks)
                                     :fetch-snapshot (constantly snapshot)
                                     :candidate-fn
                                     (fn [task-rows _]
                                       (mapv (fn [{:keys [id kind]}]
                                               {:id id :kind kind
                                                :support {:automated-feasibility :unknown}
                                                :channels {}})
                                             task-rows))})]
      (is (= :shared-current-channel-unavailable
             (get-in receipt [:policy :meta-selection :reason])))
      (is (= 2 (count (:support receipt))))
      (is (nil? (:action receipt)))))
  (testing "ties do not acquire a hidden first-item rule"
    (let [equal-snapshot (assoc-in snapshot [:graph :tickets :items 1 :mtime-ms] 200)
          receipt (live/select-live {:tasks (mapv #(assoc % :priority 1) tasks)
                                     :fetch-snapshot (constantly equal-snapshot)})]
      (is (= :no-unique-task-state-minimum
             (get-in receipt [:policy :meta-selection :reason])))
      (is (nil? (:action receipt))))))

(deftest partial-live-snapshot-remains-a-typed-outer-refusal
  (let [receipt (live/select-live
                 {:tasks tasks
                  :fetch-snapshot #(assoc-in snapshot
                                             [:graph :section-status :clusters :status]
                                             :failed)})]
    (is (= :pipeline-snapshot-invalid
           (get-in receipt [:policy :meta-selection :reason])))
    (is (= 2 (count (:support receipt))))
    (is (nil? (:action receipt)))))

(deftest off-map-tasks-are-accounted-as-typed-exclusions
  (let [off-map {:id "M-z" :kind :mission :priority 0
                 :source (pin "M-z.md" "e")}
        receipt (live/select-live {:tasks (conj tasks off-map)
                                   :fetch-snapshot (constantly snapshot)})]
    (is (= "M-a" (get-in receipt [:chosen :id])))
    (is (= ["M-z"] (mapv :id (:excluded receipt))))
    (is (= :pipeline/not-on-current-map
           (get-in receipt [:excluded 0 :ineligible-reason])))
    (is (= 3 (+ (count (:support receipt)) (count (:excluded receipt)))))))

(defn- repair-ticket [dir suffix target parent]
  (let [finding-id (str "repair-occ-" suffix)
        finding-path (.getAbsolutePath (io/file dir (str finding-id ".edn")))
        finding-text (pr-str {:repair/id finding-id :repair/status :open
                              :repair/class :environmental-hold
                              :failure-kind :agent-unavailable
                              :failure-stage :agent-readiness :target target})
        finding-sha (field/sha256 (.getBytes finding-text "UTF-8"))
        ticket-id (str "T-" finding-id)
        ticket-path (.getAbsolutePath (io/file dir (str ticket-id ".md")))
        ticket-text (str "# Repair\n\n**Status:** OPEN\n\n"
                         (when parent (str "Parent: " parent "\n\n"))
                         "## Provenance\n\nFinding: [" finding-id "](" finding-path ")\n\n"
                         "Finding SHA-256: `" finding-sha "`\n")]
    (spit finding-path finding-text)
    (spit ticket-path ticket-text)
    {:task {:id ticket-id :kind :ticket :path ticket-path :parent parent
            :source {:path ticket-path
                     :sha256 (field/sha256 (.getBytes ticket-text "UTF-8"))}}
     :finding-path finding-path}))

(deftest repair-findings-are-root-observations-not-competing-tasks
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "meta-repair-observation"
                      (make-array java.nio.file.attribute.FileAttribute 0)))
        suffix-a (apply str (repeat 64 "1"))
        suffix-b (apply str (repeat 64 "2"))
        a (repair-ticket dir suffix-a "M-a" "M-a")
        b (repair-ticket dir suffix-b "M-a" "M-a")
        run #(live/select-live {:tasks (into tasks [(:task a) (:task b)])
                                :fetch-snapshot (constantly snapshot)})
        first-receipt (run)
        root (first (:support first-receipt))]
    (is (= ["M-a" "M-b"] (mapv :id (:support first-receipt))))
    (is (= [(str "T-repair-occ-" suffix-a) (str "T-repair-occ-" suffix-b)]
           (mapv :id (:repair-observations root))))
    (is (every? #(= :repair-finding/attached-to-root (:ineligible-reason %))
                (:excluded first-receipt)))
    (testing "a finding rewrite changes evidence but cannot promote a ticket"
      (spit (:finding-path a) "{:repair/id \"changed\"}")
      (let [replayed (run)]
        (is (= ["M-a" "M-b"] (mapv :id (:support replayed))))
        (is (not-any? #(re-matches #"T-repair-occ-.*" (:id %))
                      (:support replayed)))))))

(deftest unresolved-repair-roots-fail-closed-with-evidence
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "meta-repair-unresolved"
                      (make-array java.nio.file.attribute.FileAttribute 0)))
        missing (repair-ticket dir (apply str (repeat 64 "3")) nil nil)
        ambiguous (repair-ticket dir (apply str (repeat 64 "4")) "M-a" "M-b")
        receipt (live/select-live
                 {:tasks (into tasks [(:task missing) (:task ambiguous)])
                  :fetch-snapshot (constantly snapshot)})
        reasons (set (map :ineligible-reason (:excluded receipt)))]
    (is (= #{:repair-finding/root-missing :repair-finding/root-ambiguous}
           reasons))
    (is (every? #(get-in % [:ineligibility-evidence :ticket-source :sha256])
                (:excluded receipt)))
    (is (= ["M-a" "M-b"] (mapv :id (:support receipt))))))

(deftest browser-projection-preserves-order-and-census-without-proof-bulk
  (let [receipt (live/select-live {:tasks (conj tasks
                                                {:id "M-z" :kind :mission
                                                 :priority 0
                                                 :source (pin "M-z.md" "e")})
                                   :fetch-snapshot (constantly snapshot)})
        preview (live/browser-receipt receipt)]
    (is (= (mapv :id (:support receipt)) (mapv :id (:support preview))))
    (is (= (mapv :id (:excluded receipt)) (mapv :id (:excluded preview))))
    (is (= (get-in receipt [:policy :meta-selection :ranking])
           (get-in preview [:policy :meta-selection :ranking])))
    (is (nil? (get-in preview [:policy :meta-selection :pairwise])))
    (is (nil? (get-in preview [:excluded 0 :ineligibility-evidence])))))
