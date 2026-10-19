(ns futon2.aif.outer-policy-runtime-selection-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.target-reading-preparation :as preparation]
            [futon2.aif.meta-live-outer-selector :as live]
            [futon2.aif.outer-policy-evidence-provider :as provider]
            [futon2.aif.selected-outer-cascade-policy :as selected]
            [futon2.aif.wm.cascade-decision :as cascade]
            [futon2.aif.selected-outer-cascade-policy-test]))

(defn- fixture-var [sym]
  (var-get (ns-resolve 'futon2.aif.selected-outer-cascade-policy-test sym)))
(defn- pin [path c] {:path path :sha256 (apply str (repeat 64 c))})

(def snapshot
  {:schema :wm/pipeline-cascade-snapshot-v1
   :summary-source (pin "summary" "a") :graph-source (pin "graph" "b")
   :summary {:consistent? true
             :standards {:s1-regenerates true :s2-evidence true
                         :s3-reconstitution true :s4-honest-holes true
                         :s5-composed true}}
   :graph {:section-status {:clusters {:status :ok}}
           :lineage [] :clusters [{:mission "T-occ"}] :arrows [] :held []
           :tickets {:items [{:stem "T-occ" :mtime-ms 1}]} :patterns {:edges []}}})
(def tasks [{:id "T-occ" :kind :mission :priority 1
             :source (pin "T-occ.md" "c")}])

(defn- fixture []
  (let [carriers ((fixture-var 'production-carriers))
        menu (:menu carriers)
        declaration ((fixture-var 'declaration)
                     (mapv :cascade-digest (:entries menu)) [0.0 1.0])]
    {:menu menu :declaration declaration}))

(defn- run-live
  ([evidence-provider] (run-live evidence-provider {}))
  ([evidence-provider extra]
   (let [{:keys [menu]} (fixture)]
     (live/select-live
      (merge {:tasks tasks :fetch-snapshot (constantly snapshot)
              :policy-field-config {:authority :production-shaped-test}
              :policy-field-fn
              (fn [rows _]
                {:schema :wm/pre-choice-policy-field-v1
                 :source-pin (:field-source-pin menu)
                 :coverage {:targets (count rows) :current (count rows)}
                 :families [{:target-id "T-occ" :status :computed
                             :policies [{:policy-id "T-occ/policy-1"}
                                        {:policy-id "T-occ/policy-2"}]
                             :failures []}]})
              :policy-menu-fn (fn [_ _] menu)
              :outer-policy-evidence-provider evidence-provider}
             extra)))))

(defn- explicit-provider [ln-es records]
  (fn [menu]
    (provider/provided
     ((fixture-var 'declaration) (mapv :cascade-digest (:entries menu)) ln-es)
     records
     {:kind :committed-production-authority
      :source-pin (pin "wm://authority" "d")})))

(deftest pre-choice-reading-refusal-is-not-pinned-as-a-selection
  (let [calls (atom 0)
        refusal {:chosen {:absent :incomplete-policy-menu-coverage}
                 :policy {:status :refused :reason :incomplete-policy-menu-coverage
                          :absent-terms [:no-current-target-reading]}}
        chosen (run-live (explicit-provider [0.0 1.0] []))
        select (runtime/retained-outer-selector
                (fn [_] (if (= 1 (swap! calls inc)) refusal chosen)) {})]
    (is (= refusal (select {:tasks tasks})))
    (is (= chosen (select {:tasks tasks})))
    (is (identical? chosen (select {:tasks tasks})))
    (is (= 2 @calls))))

(deftest missing-reading-prepares-and-redecides-through-runner-seam
  (let [published (atom false)
        field {:target-sources [{:target-id "T-occ" :source-path "T-occ.md"}]
               :families [{:target-id "T-occ" :status :refused
                           :failures [{:kind :no-current-target-reading
                                       :expected-digest "pinned-excerpt"}]}]}
        refusal {:chosen {:absent :incomplete-policy-menu-coverage}
                 :policy {:reason :incomplete-policy-menu-coverage}
                 :policy-field field}
        select (runtime/retained-outer-selector
                (fn [_] (if @published
                          (run-live (explicit-provider [0.0 1.0] [])) refusal)) {})
        result (runner/selected-target-reading-step
                {} {:outer-task-selection (select {:tasks tasks})}
                (fn [opts judgement]
                  (let [r (preparation/prepare!
                           opts (get-in judgement [:outer-task-selection :policy-field])
                           {:deadline-ms 100 :now-fn (constantly 0)
                            :repair-fn (fn [_ need]
                                         (is (= "pinned-excerpt" (:excerpt-digest need)))
                                         (reset! published true)
                                         {:published true})})]
                    (assoc r :published (= :prepared (:status r)))))
                (fn [_] {:outer-task-selection (select {:tasks tasks})}))]
    (is @published)
    (is (= :selected (get-in result [:judgement :outer-task-selection :policy :status])))
    (is (nil? (selected/verify
               (get-in result [:judgement :outer-task-selection :policy
                               :selected-outer-cascade-policy]))))))

(deftest missing-and-refused-provider-fail-closed
  (let [missing (run-live nil)
        refused (run-live (fn [_] {:schema provider/schema :status :refused
                                   :reason :authority-offline}))]
    (is (= :outer-policy-evidence-provider-required
           (get-in missing [:policy :reason])))
    (is (seq (get-in missing [:policy :absent-terms])))
    (is (nil? (:action missing)))
    (is (= :authority-offline (get-in refused [:policy :reason])))
    (is (nil? (:action refused)))))

(deftest unique-minimum-seals-and-consumes-without-reselection
  (let [legacy-calls (atom 0) target-selector-calls (atom 0)
        receipt (run-live (explicit-provider [0.0 1.0] [])
                          {:candidate-fn (fn [& _]
                                           (swap! target-selector-calls inc)
                                           [])})
        carrier (get-in receipt [:policy :selected-outer-cascade-policy])
        retained-calls (atom 0)
        retain (runtime/retained-outer-selector
                (fn [_] (swap! retained-calls inc) receipt) {:graph :pinned})
        first-pass (retain {:tasks tasks})
        second-pass (retain {:tasks tasks :selected-reading-repaired true})
        field-var (ns-resolve 'futon2.aif.wm.cascade-decision 'target-policy-field)
        select-var (ns-resolve 'futon2.aif.wm.cascade-decision 'reading-family-decision)
        downstream (with-redefs-fn
                     {field-var (fn [& _] (swap! legacy-calls inc))
                      select-var (fn [& _] (swap! legacy-calls inc))}
                     #(cascade/cascade-decision
                       {} {:selected-outer-cascade-policy carrier
                           :outer-policy-selection-claimed? true}))]
    (is (= :selected (get-in receipt [:policy :status])))
    (is (nil? (selected/verify carrier)))
    (is (= (:target-id carrier) (get-in receipt [:action :target])))
    (is (= (:policy-id carrier) (get-in receipt [:action :policy-id])))
    (is (= (:cascade-digest carrier) (get-in receipt [:action :cascade-digest])))
    (is (= (:canonical-cascade (:selected-entry carrier))
           (get-in downstream [:decision :selection-certificate
                               :selected-canonical-cascade])))
    (is (zero? @legacy-calls))
    (is (zero? @target-selector-calls))
    (is (= 1 @retained-calls))
    (is (identical? first-pass second-pass))
    (is (identical? carrier
                    (get-in second-pass [:policy :selected-outer-cascade-policy])))))

(deftest exact-objective-tie-can-begin-a-cycle
  (let [receipt (run-live (explicit-provider [0.0 0.0] []))]
    (is (= :selected (get-in receipt [:policy :status])))
    (is (= :uniform-over-equal-minima
           (get-in receipt [:policy :policy-objective :draw :law])))
    (is (some? (:action receipt)))
    (is (nil? (selected/verify (get-in receipt [:policy :selected-outer-cascade-policy]))))))

(deftest incomplete-authority-and-apparatus-records-do-not-sneak-through
  (let [{:keys [menu declaration]} (fixture)
        incomplete (update declaration :policies dissoc
                           (:cascade-digest (second (:entries menu))))
        incomplete (assoc-in incomplete [:pin :sha256]
                             (identity/sha256
                              (.getBytes (pr-str (dissoc incomplete :pin)) "UTF-8")))
        refusal (run-live (constantly (provider/provided incomplete [] {:kind :test})))
        apparatus {:run/id "apparatus" :selected-cascade-digest
                   (:cascade-digest (first (:entries menu)))
                   :terminal {:outcome :incomplete}
                   :failure {:kind :phase-failure :stage :build}
                   :observed-outcome :failure}
        accepted (run-live (explicit-provider [0.0 1.0] [apparatus]))]
    (is (= :refused (get-in refusal [:policy :status])))
    (is (seq (get-in refusal [:policy :policy-evidence :missing])))
    (is (= [0 0] (get-in accepted [:policy :policy-evidence :entries 0 :task-counts])))
    (is (= "apparatus"
           (get-in accepted [:policy :policy-evidence :entries 0 :excluded 0 :run/id])))))

(deftest forged-authority-seal-refuses-before-objective
  (let [good (explicit-provider [0.0 1.0] [])
        forged (fn [menu]
                 (update-in (good menu) [:declaration :gamma] inc))
        receipt (run-live forged)]
    (is (= :provider-source-pin-mismatch (get-in receipt [:policy :reason])))
    (is (nil? (get-in receipt [:policy :policy-objective])))))
