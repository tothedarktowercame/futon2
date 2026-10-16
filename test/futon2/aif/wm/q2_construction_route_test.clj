(ns futon2.aif.wm.q2-construction-route-test
  "Requirements Q2 on the production route (codex-68 ruling (c), bounded by
  Q8 eligibility): outer actions certified by select-live for exactly the
  tasks its effective selection could choose, and the construction census
  of the selected task alone, made after the outer choice by the reading-
  family decision, sealed by the runner and accepted by the exporter only
  when the record recomputes it. The runs are the Q8 route test's: two
  production clicks on one run stream, select-live in live mode."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.meta-live-outer-selector :as live]
            [futon2.aif.q2-construction :as q2]
            [futon2.aif.q8-frontier :as q8]
            [futon2.aif.wm.q8-frontier-route-test :as q8-route]
            [futon2.test-support.reading-route :as route]
            [futon2.test-support.runner-fixture :as fixture]
            [wm-check-run-facts :as lean-term]
            [wm-report-card :as card]
            [wm-run-facts :as facts]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)
(use-fixtures :each
  (fn [run]
    (binding [runner/*wm-status-reporting?* false]
      (route/with-empty-learning-ledger run))))

(def click! @#'q8-route/click!)
(def base-tasks q8-route/base-tasks)
(def base-map q8-route/base-map)

(defn- compared [record]
  (get-in record [:decision :selection-certificate :q4 :compared]))

(defn- q2-holds?
  "Requirements.Q2 (mirrored) over RECORD as the exporter accepts it: :nr
  when the Q2 carrier, or the Q8 carrier whose census and selection Q2
  reads, is not accepted."
  [record]
  (let [v (q2/verify-record record)
        v8 (q8/verify-record record)]
    (if (and (= :verified (:status v)) (= :verified (:status v8)))
      (q2/lean-q2 (:carrier v) (:carrier v8) (compared record))
      :nr)))

(defn- reseal-q2
  "RECORD with its :q2 carrier recomputed after F rewrites the record's
  inputs, as a consistent forger would."
  [record f]
  (let [r (f record)]
    (assoc r :q2 (q2/certificate (q2/record-inputs r)))))

(defn- reseal-outer [record f]
  (reseal-q2 record #(update-in % [:outer-task-selection :q2-outer-actions]
                                (fn [o] (q2/seal (f (dissoc o :sha256)))))))

(defn- reseal-census [record f]
  (reseal-q2 record #(update-in % [:decision :selection-certificate :q2-construction]
                                (fn [c] (q2/seal (f (dissoc c :sha256)))))))

(defn- certified-record
  ([] (certified-record (route/fixture)))
  ([fx]
   (let [dir (route/temp-dir "q2-stream-")]
     (click! fx dir)
     (:record (click! fx dir)))))

(defn- verdict [record]
  (let [export (facts/facts-for-record record "r" {})]
    (:status (first (filter #(= :Q2 (:requirement %)) (card/verdict-rows export))))))

(defn- nr? [v] (and (map? v) (contains? v "not-recomputable")))

;; -- positive -------------------------------------------------------------------------

(deftest outer-actions-and-the-selected-construction-are-certified
  (let [fx (route/fixture)
        dir (route/temp-dir "q2-stream-")
        one (:record (click! fx dir))
        rec (:record (click! fx dir))
        c (:q2 rec)
        [entry & more] (:entries c)
        receipt (:outer-task-selection rec)]
    (testing "run one: the carrier is certified, but Q8's census is not yet"
      (is (= :certified (get-in one [:q2 :status])))
      (is (= :nr (q2-holds? one)) "Q2 reads Q8's census and selection"))
    (testing "the carrier is bound to the record"
      (is (= :certified (:status c)))
      (is (= {:status :verified :carrier c} (q2/verify-record rec))))
    (testing "outer: one action per eligible task, none for the excluded one"
      (is (= (get-in rec [:q8 :census :eligible]) (mapv :task (:outer-actions c))))
      (is (= [route/target "M-other" "M-third"] (mapv :task (:outer-actions c))))
      (is (= [{:task "M-off" :reason :pipeline/not-on-current-map}]
             (get-in rec [:q8 :census :excluded])))
      (is (not-any? #(= "M-off" (:task %)) (:outer-actions c)))
      (is (= {:type :advance-mission :target route/target} (:action receipt)))
      (is (= (evidence/value-digest (:action receipt))
             (:action-id (first (filter #(= route/target (:task %)) (:outer-actions c)))))
          "the selected task's outer action is the receipt's chosen action"))
    (testing "inner: exactly the selected task was constructed"
      (is (nil? more) "no unselected task is said to have inner policies")
      (is (= route/target (:task entry) (get-in rec [:q8 :selection :selected :task])))
      (is (= (evidence/value-digest (:action receipt)) (:action-id entry))))
    (testing "the construction: pins, slice from the library, pool = slice"
      (is (every? string? ((juxt :reading-id :library-id :graph-id :kernel-id) entry)))
      (is (= {:count 4} (select-keys (:library c) [:count])))
      (is (= ["p/a" "p/b"] (:slice entry) (:pool entry)))
      (is (true? (:slice-from-whole-library entry)))
      (is (= [] (:failures entry))))
    (testing "policies: the selected cascade's constructed set, holding the compared"
      (is (= 2 (:policy-count entry) (count (set (:policies entry)))))
      (is (= (get-in rec [:q8 :cascade :constructed]) (set (:policies entry))))
      (is (every? (set (:policies entry)) (compared rec))))
    (testing "Requirements.Q2 holds; the exporter, Lean term and report card agree"
      (is (true? (q2-holds? rec)))
      (let [f (:facts (facts/facts-for-record rec "r" {}))]
        (is (not-any? (comp nr? f)
                    ["outerActions" "targetConstruction" "constructorPatternCount"
                     "libraryPatternCount"]))
        (is (= 4 (f "libraryPatternCount")) "the library pinned at construction")
        (is (= 3 (count (f "outerActions"))))
        (is (= (vec (sort (map pr-str (get-in rec [:q8 :cascade :constructed]))))
               (get-in f ["targetConstruction" 0 "policies"]))
            "the Q8 exporter's policy identity")
        (let [source (lean-term/lean-source
                      (json/parse-string (json/generate-string {:facts f})) "q2")]
          (is (str/includes? source "  q2 := .known { Requirements.click20 with outerActions := ["))))
      (is (= :pass (verdict rec))))))

(deftest a-reading-failure-is-retained-beside-the-family
  ;; p/d is in the library but not the graph: it cannot seed a retraction
  (let [rec (certified-record (route/fixture ["p/a" "p/b" "p/d"]))
        entry (first (get-in rec [:q2 :entries]))]
    (is (= [{:kind :seed-not-in-graph :seeds ["p/d"]}] (:failures entry)))
    (is (true? (:slice-from-whole-library entry)))
    (is (true? (q2-holds? rec)) "a counted failure beside a positive family")))

(deftest an-infeasible-candidate-is-excluded-and-carries-no-action
  ;; select-live's own boundary: the selector excludes an infeasible
  ;; candidate; it is excluded in Q8's census and has no outer action
  (let [tasks (mapv #(cond-> % (= "M-third" (:id %)) (assoc :automated-feasibility :infeasible))
                    (@#'q8-route/tasks base-tasks))
        receipt (live/select-live {:tasks tasks
                                   :fetch-snapshot (constantly (@#'q8-route/snapshot base-map))
                                   :outer-learning q8-route/outer-learning})
        actions (:q2-outer-actions receipt)
        census (get-in receipt [:q8-frontier :census])]
    (is (q2/intact? actions))
    (is (some #{{:task "M-third" :reason :automated-feasibility/infeasible}} (:excluded census)))
    (is (= ["M-third"] (:excluded actions)))
    (is (= (:eligible census) (mapv :task (:actions actions))))
    (is (not-any? #(= "M-third" (:task %)) (:actions actions)))
    (is (= (evidence/value-digest (:action receipt))
           (:action-id (first (filter #(= (get-in receipt [:chosen :id]) (:task %))
                                      (:actions actions))))))))

;; -- production cases Requirements.Q2 refuses ----------------------------------------

(deftest a-steiner-point-outside-the-slice-breaks-pool-equals-slice
  ;; the p/a + p/c retraction runs through p/b, which the reading did not name
  (let [rec (certified-record (route/fixture ["p/a" "p/c"]))
        entry (first (get-in rec [:q2 :entries]))]
    (is (= :certified (get-in rec [:q2 :status])))
    (is (= ["p/a" "p/c"] (:slice entry)))
    (is (= ["p/a" "p/b" "p/c"] (:pool entry)))
    (is (false? (q2-holds? rec)))
    (is (= :fail (verdict rec)))))

(deftest library-bytes-changed-since-the-reading-is-not-from-the-library
  (let [fx (route/fixture)]
    (spit (io/file (:library fx) "p/b.flexiarg") "@flexiarg p/b\n! conclusion: edited\n")
    (let [rec (certified-record fx)
          entry (first (get-in rec [:q2 :entries]))]
      (is (= :certified (get-in rec [:q2 :status])))
      (is (false? (:slice-from-whole-library entry)))
      (is (false? (q2-holds? rec))))))

(deftest an-unpinned-library-proves-no-slice
  (let [fx (assoc (route/fixture) :library (io/file (route/temp-dir "q2-") "absent"))
        rec (certified-record fx)
        census (get-in rec [:decision :selection-certificate :q2-construction])]
    (is (= [:absent :library-root-absent] ((juxt :status :reason) (:library census))))
    (is (false? (:slice-from-whole-library (first (get-in rec [:q2 :entries])))))
    (is (false? (q2-holds? rec)))))

(deftest a-stale-reading-leaves-a-zero-policy-family-with-its-failure
  (let [fx (route/fixture)]
    (spit (io/file (:root fx) (str route/target ".md")) "# Current\n\n## HEAD\nChanged work.\n")
    (let [rec (certified-record fx)
          entry (first (get-in rec [:q2 :entries]))]
      (is (= :abstained (get-in rec [:decision :abstention :status])))
      (is (= :certified (get-in rec [:q2 :status])) "the construction is kept")
      (is (= 0 (:policy-count entry)))
      (is (= [:stale-target-reading] (map :kind (:failures entry))))
      (is (not (true? (q2-holds? rec))))
      (is (false? (q2/lean-q2 (:q2 rec) (:q8 (certified-record)) [])) "zero policies"))))

;; -- forgeries --------------------------------------------------------------------------

(deftest outer-action-forgeries-fail-q2
  (let [rec (certified-record)
        ghost (fn [t] (let [a {:type :advance-mission :target t}]
                        {:task t :action a :action-id (evidence/value-digest a)}))]
    (is (true? (q2-holds? rec)))
    (doseq [[label f]
            {:omitted #(update % :actions (comp vec rest))
             :duplicate #(update % :actions (fn [as] (conj as (last as))))
             :foreign #(update % :actions conj (ghost "M-ghost"))
             :for-an-excluded-task #(update % :actions conj (ghost "M-off"))
             :shared-identity #(assoc-in % [:actions 2 :action-id]
                                         (get-in % [:actions 1 :action-id]))}]
      (testing (name label)
        (let [forged (reseal-outer rec f)]
          (is (= :verified (:status (q2/verify-record forged))) "consistently resealed")
          (is (false? (q2-holds? forged)) "and Requirements.Q2 refuses it"))))))

(deftest selected-binding-forgeries-are-refused
  (let [rec (certified-record)
        other {:type :advance-mission :target "M-other"}]
    (testing "the receipt's action names another task"
      (let [forged (reseal-q2 rec #(assoc-in % [:outer-task-selection :action] other))]
        (is (some #{:decision-target-not-the-outer-action's} (get-in forged [:q2 :reasons])))
        (is (= :nr (q2-holds? forged)))))
    (testing "the construction names another task than the field"
      (let [forged (reseal-census rec #(assoc-in % [:entries 0 :task] "M-other"))]
        (is (= [:construction-tasks-not-the-field's] (get-in forged [:q2 :reasons])))))
    (testing "a carrier whose construction serves another action"
      (let [forged (assoc rec :q2 (q2/seal (-> (:q2 rec) (dissoc :sha256)
                                              (assoc-in [:entries 0 :action-id]
                                                        (evidence/value-digest other)))))]
        (is (= :nr (q2-holds? forged)))
        (is (false? (q2/lean-q2 (:q2 forged) (:q8 rec) (compared rec)))
            "and Requirements.Q2 refuses its content")))))

(deftest policy-forgeries-are-refused
  (let [rec (certified-record)
        [k1 k2] (get-in rec [:q2 :entries 0 :policies])
        invented (assoc k1 2 (str route/target "/policy-invented"))
        content (fn [f] (q2/lean-q2 (update-in (:q2 rec) [:entries 0] f) (:q8 rec) (compared rec)))]
    (doseq [[label f reason]
            [[:omitted #(update % :policies (comp vec rest)) :construction-policy-omitted]
             [:invented #(update % :policies conj invented) :construction-policy-not-in-field]
             [:duplicated #(update % :policies conj k2) :construction-policy-omitted]]]
      (testing (name label)
        (let [forged (reseal-census rec #(update-in % [:entries 0] f))]
          (is (= [reason] (get-in forged [:q2 :reasons])))
          (is (= :nr (q2-holds? forged))))))
    (testing "the same rewrites of the carrier's content fail Requirements.Q2"
      (is (false? (content #(assoc % :policies [k1] :policy-count 1))))
      (is (false? (content #(assoc % :policies [k1 k2 invented] :policy-count 3))))
      (is (false? (content #(assoc % :policy-count 3))))
      (is (false? (content #(assoc % :policies [] :policy-count 0)))))))

(deftest hidden-failures-and-stale-pins-are-refused
  (let [rec (certified-record (route/fixture ["p/a" "p/b" "p/d"]))
        stale (apply str (repeat 64 "0"))]
    (doseq [[label forged reason]
            [[:hidden-failure (reseal-census rec #(assoc-in % [:entries 0 :failures] []))
              :construction-failure-hidden]
             [:stale-reading (reseal-census rec #(assoc-in % [:entries 0 :reading :excerpt-digest] stale))
              :construction-reading-not-the-field's]
             [:reading-not-the-chosen-action's
              (reseal-q2 rec (fn [r]
                               (let [id (get-in r [:decision :chosen :id])]
                                 (update-in r [:decision :selection-law :posterior]
                                            #(into {} (map (fn [[a p]]
                                                             [(if (= id (:id a))
                                                                (assoc-in a [:interpretation-debt :analysis-digest] stale)
                                                                a)
                                                              p]))
                                                   %)))))
              :construction-reading-not-the-chosen-action's]
             [:chosen-not-compared
              (reseal-q2 rec #(assoc-in % [:decision :chosen :id] "M-current/policy-x"))
              :chosen-action-not-compared]
             [:stale-graph-pin (reseal-census rec #(assoc-in % [:graph :pin :sha256] stale))
              :construction-graph-pin-not-the-field's]
             [:stale-graph-digest (reseal-census rec #(assoc-in % [:entries 0 :graph-digest] stale))
              :construction-graph-not-the-field's]
             [:stale-slice (reseal-census rec #(assoc-in % [:entries 0 :slice 0 :source-sha256] stale))
              :construction-slice-not-the-field's]
             [:kernel (reseal-census rec #(assoc-in % [:entries 0 :kernel :retraction :k] 9))
              :construction-kernel-not-the-field's]]]
      (testing (name label)
        (is (some #{reason} (get-in forged [:q2 :reasons])))
        (is (= :nr (q2-holds? forged)))))
    (testing "a stale library pin, consistently resealed, is not from the library"
      (let [forged (reseal-census rec #(assoc-in % [:library :patterns "p/a"] stale))]
        (is (= :certified (get-in forged [:q2 :status])))
        (is (false? (q2-holds? forged)))))
    (testing "a pool that is not the slice, in the carrier's content"
      (is (false? (q2/lean-q2 (update-in (:q2 rec) [:entries 0 :pool] conj "p/c")
                              (:q8 rec) (compared rec)))))))

(deftest edited-and-unbound-carriers-are-not-accepted
  (let [rec (certified-record)
        q (:q2 rec)]
    (testing "edited"
      (is (= :q2-carrier-tampered
             (:reason (q2/verify-record (assoc-in rec [:q2 :entries 0 :policy-count] 9))))))
    (testing "resealed, not what the record recomputes"
      (is (= :q2-carrier-not-bound-to-record
             (:reason (q2/verify-record
                       (assoc rec :q2 (q2/seal (-> q (dissoc :sha256)
                                                   (assoc-in [:entries 0 :slice-from-whole-library] true)
                                                   (assoc-in [:entries 0 :pool] ["p/a"])))))))))
    (testing "absent, and the exporter's reading"
      (let [r (dissoc rec :q2)
            f (:facts (facts/facts-for-record r "r" {}))]
        (is (= :q2-carrier-absent (:reason (q2/verify-record r))))
        (is (every? (comp nr? f)
                    ["outerActions" "targetConstruction" "constructorPatternCount"
                     "libraryPatternCount"]))))))
