(ns futon2.aif.wm.q8-frontier-route-test
  "Requirements Q8 on the production route (codex-68 rulings 1a, 2b, 3):
  the outer frontier certified by select-live in live (:complete-union)
  mode, the run's predecessor frontier from the run stream, the reading-
  family decision's policy stages and Q6 carrier, sealed by the runner into
  the record and accepted by the exporter only when the record recomputes
  it."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runtime]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.meta-live-outer-selector :as live]
            [futon2.aif.outer-target-belief :as outer-belief]
            [futon2.aif.previous-run :as previous-run]
            [futon2.aif.q8-frontier :as q8]
            [futon2.test-support.reading-route :as route]
            [futon2.test-support.runner-fixture :as fixture]
            [wm-run-facts :as facts]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)
(use-fixtures :each
  (fn [run]
    (binding [runner/*wm-status-reporting?* false]
      (route/with-empty-learning-ledger run))))

;; -- the outer field -------------------------------------------------------------

(defn- pin [path ch] {:path path :sha256 (apply str (repeat 64 ch))})

(def mtimes
  "Freshest is cheapest; the range is fixed by the base tasks, so adding or
  removing M-new or M-third leaves the others' normalised values alone."
  {"M-current" 900 "M-other" 100 "M-third" 500 "M-new" 300})

(defn- snapshot [on-map]
  {:schema :wm/pipeline-cascade-snapshot-v1
   :summary-source (pin "summary" "a") :graph-source (pin "graph" "b")
   :summary {:consistent? true
             :standards {:s1-regenerates true :s2-evidence true
                         :s3-reconstitution true :s4-honest-holes true
                         :s5-composed true}}
   :graph {:section-status {:clusters {:status :ok}}
           :lineage [] :clusters (mapv #(hash-map :mission %) on-map)
           :arrows [] :held []
           :tickets {:items (mapv (fn [t] {:stem t :mtime-ms (mtimes t)}) on-map)}
           :patterns {:edges []}}})

(def base-tasks
  {route/target 1 "M-other" 8 "M-third" 5 "M-off" 3})

(def base-map [route/target "M-other" "M-third"])

(def reading-field
  (route/field (zipmap (keys base-tasks) (repeat ["p/a" "p/b"]))))

(defn- tasks [priorities]
  (vec (for [[id p] (sort priorities)]
         (let [source (some->> (:rows reading-field)
                               (some #(when (= id (:target-id %)) (:source-path %)))
                               io/file)]
           {:id id :kind :mission :priority p
            :source {:path (.getPath source)
                     :sha256 (load-identity/sha256
                              (.getBytes (slurp source) "UTF-8"))}}))))

(def outer-learning
  {:mode :live
   :belief (outer-belief/build
            {:prior (outer-belief/prior 1 :operator)
             :dataset {:status :produced :source-pin (pin "runs" "6")
                       :rows [{:run/id "r" :click/id "c" :task {:id "M-elsewhere"}
                               :terminal-outcome {:class :grounded-progress}
                               :selected-want-effects
                               {:status :present
                                :counts {:reached 0 :progressed 0 :blocked 0 :untouched 1}}
                               :collateral-progress {:status :present}
                               :blocker {:status :absent}}]}})
   :preference-authority :operator
   :preference-transform {:kind :softmax-negative-cost :inverse-temperature 1.0}
   :preference-costs {:closure 0.0 :selected-want-progress 0.2
                      :grounded-collateral-progress 0.5 :typed-blocker 0.9
                      :abstention-or-failure 1.0}
   :controller {:missing-channel-cost 1.0 :recent-selection-cost 0.5
                :continuation-credit 0.7}})

(defn- outer-receipt
  ([] (outer-receipt base-tasks base-map))
  ([priorities on-map]
   (let [selection-as-of "2026-10-26T05:00:00Z"]
     (live/select-live {:tasks (tasks priorities)
                        :fetch-snapshot (constantly (snapshot on-map))
                        :outer-learning outer-learning
                        :selection-as-of selection-as-of
                        :focus-inputs {}
                        :focus-context {:as-of selection-as-of
                                        :status :retained
                                        :focus "WM"}
                        :focus-classify-fn
                        (fn [_ _ _ target _] {:target target :class :focus})
                        :seed 14
                        :policy-field-config
                        {:target-reading-root (.getPath (io/file (:root reading-field)))
                         :pattern-graph-path (.getPath (io/file (:graph reading-field)))}}))))

;; -- the production run -------------------------------------------------------------

(defn- judgement-for [decision receipt]
  {:decision decision :belief {} :belief-pre {} :observation {} :free-energy {}
   :prediction-errors {} :precision-state {} :micro-step-trace [] :mode :maintain
   :outer-task-selection receipt})

(defn- click!
  "One production run: the outer selection is select-live's, the inner
  decision the real reading-family decision for the selected task."
  ([fx dir] (click! fx dir base-tasks base-map))
  ([fx dir priorities on-map]
   (let [calls (atom 0)
         result (runner/run-opportunity!
                 (merge (fixture/isolated-runner-opts)
                        {:run-record-dir (str dir)
                         :judge-fn (fn [_]
                                     (let [receipt (outer-receipt priorities on-map)
                                           r (route/decide fx)]
                                       (swap! calls + (:selector-calls r))
                                       {:judgement (judgement-for (:decision r) receipt)}))
                         :repair-system-record-fn (fn [m] {:repair/id "repair-test"
                                                           :repair/class (:repair-class m)})
                         :dispatch-fn (fn [& _] (throw (ex-info "Unexpected dispatch" {})))}))]
     {:record (edn/read-string {:default tagged-literal} (slurp (:run-record result)))
      :calls @calls})))

(defn- open-of [record]
  (into #{} (mapcat #(get-in record [:world-at-selection :open-tasks % :ids]))
        [:missions :excursions :tickets]))

(defn- q6-of [record]
  (let [f (facts/q6-facts record)]
    {:refused? (= "refused" (f "previousOutcome"))
     :same-digest? (= (f "previousInputDigest") (f "currentInputDigest"))
     :previous-cascade (get-in record [:q6 :predecessor :previous :choice :policy-key])}))

(defn- q8-holds?
  "Requirements.Q8 (mirrored) over RECORD as the exporter accepts it:
  :nr when the carrier is not accepted."
  [record]
  (let [v (q8/verify-record record)]
    (if (= :verified (:status v))
      (q8/lean-q8 (:carrier v) (open-of record) (q6-of record))
      :nr)))

(defn- reseal-q8
  "RECORD with its :q8 carrier recomputed after F rewrites the record's
  inputs, as a consistent forger would."
  [record f]
  (let [r (f record)]
    (assoc r :q8 (q8/certificate (q8/record-inputs r)))))

(defn- reseal-outer [record f]
  (reseal-q8 record #(update-in % [:outer-task-selection :q8-frontier]
                                (fn [o] (q8/seal (f (dissoc o :sha256)))))))

;; -- positive -------------------------------------------------------------------------

(deftest second-run-certifies-q8-with-the-q6-exclusion
  (let [fx (route/fixture)
        dir (route/temp-dir "q8-stream-")
        one (click! fx dir)
        two (click! fx dir)
        rec (:record two)
        q (:q8 rec)
        k1 (get-in (:record one) [:q6 :terminal :choice :policy-key])]
    (testing "outer: the effective selection was the complete-union comparison"
      (let [o (get-in rec [:outer-task-selection :q8-frontier])]
        (is (= :certified (:status o)))
        (is (= route/target (:selected o) (get-in rec [:outer-task-selection :chosen :id])))
        (is (= {:law q8/ranking-law :version 1 :comparison :complete-union
                :missing-channel-cost 1.0} (:ranking o)))
        (is (= 3 (get-in o [:pairwise :pairs])) "every eligible pair, once")))
    (testing "run one has no prior frontier: not recomputable, frontier persisted"
      (is (= [:not-recomputable [:no-prior-frontier]]
             ((juxt :status :reasons) (get-in one [:record :q8]))))
      (is (= 0 (get-in one [:record :q8 :frontier :epoch])))
      (is (= :nr (q8-holds? (:record one)))))
    (testing "run two: certified, bound to the record"
      (is (= 1 (:calls one) (:calls two)))
      (is (= :certified (:status q)))
      (is (= {:status :verified :carrier q} (q8/verify-record rec))))
    (testing "census: every open task eligible or excluded with its reason"
      (is (= [route/target "M-other" "M-third"] (get-in q [:census :eligible])))
      (is (= [{:task "M-off" :reason :pipeline/not-on-current-map}]
             (get-in q [:census :excluded])))
      (is (= #{route/target "M-other" "M-third" "M-off"} (open-of rec)))
      (is (not-any? #(= "M-off" (:task %)) (get-in q [:refresh :current]))
          "an excluded task has no comparison key"))
    (testing "refresh: nothing changed, everything retained from epoch 0"
      (is (= 3 (count (get-in q [:refresh :retained]))))
      (is (empty? (get-in q [:refresh :recomputed])))
      (is (every? #(= 0 (:freshness-epoch %)) (get-in q [:refresh :current])))
      (is (= 1 (get-in q [:frontier :epoch]))))
    (testing "cascade: truthful stages, Q6's exclusion, posterior of the rest"
      (let [c (:cascade q)]
        (is (= route/target (:selected-task c)))
        (is (= 2 (count (:scored c))))
        (is (= (:constructed c) (:admitted c) (:scored c)))
        (is (= #{k1} (:requirement-excluded c)))
        (is (= (disj (:scored c) k1) (:posterior c)))))
    (testing "Requirements.Q8 holds, and the exporter emits it"
      (is (true? (q8-holds? rec)))
      (let [f (:facts (facts/facts-for-record rec "r" {}))]
        (is (every? #(not (contains? (f %) "not-recomputable"))
                    ["frontierCensus" "frontierRefresh" "frontierSelection" "selectedCascade"]))))))

(deftest refresh-classifies-changed-added-and-removed-tasks
  (let [fx (route/fixture)
        dir (route/temp-dir "q8-stream-")
        _ (click! fx dir)
        ;; M-third's priority moves inside the range: only its channel value
        ;; changes; M-new joins the map; nothing else moves
        changed (click! fx dir (assoc base-tasks "M-third" 6) base-map)
        added (click! fx dir (assoc base-tasks "M-third" 6 "M-new" 4)
                      (conj base-map "M-new"))
        removed (click! fx dir (assoc base-tasks "M-new" 4) [route/target "M-other" "M-new"])
        r (fn [run] (get-in run [:record :q8 :refresh]))
        tasks-of (fn [es] (set (map :task es)))]
    (testing "a changed observation invalidates exactly that task"
      (is (= #{"M-third"} (tasks-of (:invalidated (r changed)))))
      (is (= #{"M-third"} (tasks-of (:recomputed (r changed)))))
      (is (= #{route/target "M-other"} (tasks-of (:retained (r changed)))))
      (is (true? (q8-holds? (:record changed)))))
    (testing "an added task is recomputed, the rest retained"
      (is (= ["M-new"] (get-in (r added) [:delta :added-tasks])))
      (is (= #{"M-new"} (tasks-of (:recomputed (r added)))))
      (is (= #{route/target "M-other" "M-third"} (tasks-of (:retained (r added)))))
      (is (true? (q8-holds? (:record added)))))
    (testing "a task leaving the frontier is removed"
      (is (= ["M-third"] (:removed (r removed))))
      (is (true? (q8-holds? (:record removed)))))
    (testing "epochs: recomputed entries carry the current epoch"
      (is (every? #(= (get-in changed [:record :q8 :frontier :epoch]) (:freshness-epoch %))
                  (:recomputed (r changed)))))))

;; -- adversarial ----------------------------------------------------------------------

(defn- certified-record []
  (let [fx (route/fixture)
        dir (route/temp-dir "q8-stream-")]
    (click! fx dir)
    {:dir dir :fx fx :record (:record (click! fx dir))}))

(deftest census-forgeries-fail-q8
  (let [{:keys [record]} (certified-record)]
    (is (true? (q8-holds? record)))
    (doseq [[label f]
            {:omitted-open-task #(assoc-in % [:census :excluded] [])
             :invented-eligible-task #(update-in % [:census :eligible] conj "M-ghost")
             :unreasoned-exclusion #(assoc-in % [:census :excluded 0 :reason] nil)}]
      (testing (name label)
        (let [forged (reseal-outer record f)]
          (is (= :verified (:status (q8/verify-record forged))) "consistently resealed")
          (is (false? (q8-holds? forged)) "and Requirements.Q8 refuses it"))))))

(deftest pairwise-key-disagreement-is-not-recomputable
  (let [captured (atom nil)
        produce q8/outer-frontier]
    (with-redefs [q8/outer-frontier (fn [m] (reset! captured m) (produce m))]
      (outer-receipt))
    (let [m @captured
          flip (fn [p] (assoc p :winner (if (= (:winner p) (:left p)) (:right p) (:left p))))]
      (is (= :certified (:status (produce m))))
      (testing "one recorded winner flipped"
        (is (= [:not-recomputable :pairwise-key-order-mismatch]
               ((juxt :status :reason) (produce (update-in m [:selection :pairwise 0] flip))))))
      (testing "a pair missing"
        (is (= :pairwise-incomplete
               (:reason (produce (update-in m [:selection :pairwise] (comp vec rest)))))))
      (testing "a shared-only comparison has no scalar key"
        (is (= :ranking-law-not-scalar (:reason (produce (assoc m :comparison :shared-only))))))
      (testing "a non-numeric channel value"
        (is (= :channel-value-not-exact
               (:reason (produce (assoc-in m [:candidates 0 :channels :declared-priority-cost
                                               :value] "0.5"))))))
      (testing "the selected task is not the key minimum"
        (is (= :selected-not-strict-key-minimum
               (:reason (produce (assoc-in m [:selection :selected] "M-other"))))))
      (testing "a key that disagrees with the recorded pairs, kept in the record"
        (let [{:keys [record]} (certified-record)
              forged (reseal-outer record #(update-in % [:entries 0 :comparison-key] + 1))]
          (is (= :nr (q8-holds? forged))
              "the record's carrier no longer equals one recomputed from it"))))))

(deftest q6-exclusion-must-be-justified
  (let [{:keys [record]} (certified-record)
        q6 (get-in record [:decision :selection-certificate :q6])
        other (first (disj (get-in record [:q8 :cascade :scored])
                           (first (:excluded-keys q6))))
        reseal-q6 (fn [f] (reseal-q8 record #(assoc-in % [:decision :selection-certificate :q6]
                                                       (q8/seal (f (dissoc q6 :sha256))))))]
    (doseq [[label forged reason]
            [[:another-policy (reseal-q6 #(assoc % :excluded-keys [other])) :q6-exclusion-unjustified]
             [:extra-exclusion (reseal-q6 #(update % :excluded-keys conj other)) :q6-exclusion-unjustified]
             [:stale (reseal-q6 #(assoc-in % [:predecessor :sha256] "0000")) :q6-carrier-stale]
             [:tampered (assoc-in record [:decision :selection-certificate :q6 :excluded-keys] [other])
              :q8-carrier-not-bound-to-record]
             [:removal-without-exclusion (reseal-q6 #(assoc % :status :not-triggered))
              :q6-removal-without-exclusion]]]
      (testing (name label)
        (is (= :nr (q8-holds? forged)))
        (is (some #{reason} (cons (:reason (q8/verify-record forged))
                                  (get-in forged [:q8 :reasons]))))))))

(deftest forged-stale-and-omitting-carriers-are-not-accepted
  (let [{:keys [record dir fx]} (certified-record)
        three (:record (click! fx dir))
        q (:q8 record)]
    (testing "an edited carrier"
      (is (= :q8-carrier-tampered
             (:reason (q8/verify-record (assoc-in record [:q8 :selection :current-epoch] 9))))))
    (testing "a resealed carrier the record does not recompute"
      (is (= :q8-carrier-not-bound-to-record
             (:reason (q8/verify-record
                       (assoc record :q8 (q8/seal (-> q (dissoc :sha256)
                                                      (assoc-in [:selection :current-epoch] 9)))))))))
    (testing "the previous run's carrier in this run's record"
      (is (= :q8-carrier-not-bound-to-record (:reason (q8/verify-record (assoc three :q8 q))))))
    (testing "a refresh that omits a recomputed entry"
      (let [forged (assoc record :q8 (q8/seal (-> q (dissoc :sha256)
                                                  (update-in [:refresh :retained] (comp vec rest)))))]
        (is (= :nr (q8-holds? forged)))
        (is (false? (q8/lean-q8 (:q8 forged) (open-of record) (q6-of record)))
            "and Requirements.Q8 refuses its content")))
    (testing "absent"
      (is (= :q8-carrier-absent (:reason (q8/verify-record (dissoc record :q8))))))))

(deftest tampered-prior-frontier-is-not-recomputable
  (let [fx (route/fixture)
        dir (route/temp-dir "q8-stream-")
        _ (click! fx dir)
        f (previous-run/stream-file dir)]
    (spit f (.replace ^String (slurp f) ":epoch 0" ":epoch 7"))
    (let [two (:record (click! fx dir))]
      (is (= [:prior-frontier-tampered] (get-in two [:q8 :reasons])))
      (is (= :nr (q8-holds? two))))))
