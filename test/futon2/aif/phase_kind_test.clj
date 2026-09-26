(ns futon2.aif.phase-kind-test
  "WM-PHASE-KIND-I: an exception reaching the selection phase's catch whose
  thrower typed it with a bare :kind, and which carries neither
  :failure-kind nor :outcome, closes with that kind as its :failure-kind,
  never :untyped-failure. The eighth flight (flight-ada87008) closed its
  click :untyped-failure with {:kind :substrate-unreachable} on the finding
  as data; live pin: that finding (fixture header: path and sha). Thrown
  through the same seam the gate test uses (run-opportunity!'s :judge-fn),
  not a copy of the catch."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runner-test :as fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)
(use-fixtures :each (fn [f] (binding [runner/*wm-status-reporting?* false] (f))))

(def live (edn/read-string (slurp "test/fixtures/phase-kind/eighth-flight-failure.edn")))

(defn- run [judge-throws & [flight]]
  (let [findings (atom [])
        result (runner/run-opportunity!
                (merge (fixture/isolated-runner-opts)
                       {:judge-fn (fn [_] (throw judge-throws))
                        :repair-system-record-fn (fn [m] (swap! findings conj m)
                                                   {:repair/id (str "repair-test-" (count @findings))
                                                    :repair/class (:repair-class m)})
                        :dispatch-fn (fn [& _] (throw (ex-info "Unexpected dispatch" {})))}
                       (when flight {:flight flight})))]
    {:result result :findings @findings
     :record (edn/read-string (slurp (:run-record result)))}))

(defn- kind-of [r] (get-in r [:result :data :failure-kind]))

(deftest the-eighth-flights-registry-throw-replayed
  (let [{:keys [findings] :as r}
        (run (ex-info (:failure-error live) (:failure-data live) (RuntimeException. "x"))
             {:target "M-autoclock-in"})
        [f] findings]
    (is (= :untyped-failure (:failure-kind live)) "what the eighth flight recorded")
    (is (= {:kind :substrate-unreachable} (:failure-data live)))
    (is (= :substrate-unreachable (kind-of r)))
    (is (= :selection (get-in r [:result :data :failure-stage])))
    (is (= (:failure-error live) (get-in r [:result :data :error])))
    (is (= :substrate-unreachable (get-in r [:result :data :error-data :kind]))
        "the thrower's ex-data is kept")
    (is (= 1 (count findings)))
    (is (= [:machine-failure] (mapv :repair-class findings))
        "the class the finding got before: repair-class-for names no one of these kinds")
    (is (some? f))))

(def registered-shapes
  ;; each thrower's message and ex-data, from its site
  [[:substrate-mission-registry-empty "substrate-2 mission registry returned no missions"
    {:kind :substrate-mission-registry-empty}]                                 ; mission_registry.clj:454
   [:invalid-policy-prefix "Invalid policy prefix"
    {:kind :invalid-policy-prefix :prefix {:policy [:a] :status :bogus}}]      ; policy.clj:240
   [:precision-consumption-mismatch "precision carry consumption mismatch"
    {:kind :precision-consumption-mismatch}]                                   ; policy.clj:401
   [:no-acting-cascade-candidate "Cascade selection refused"
    {:refusal {:kind :no-acting-cascade-candidate :detail {:candidates 3}}}]   ; policy.clj:423
   [:invalid-temperature "Cascade selection refused"
    {:refusal {:kind :invalid-temperature :detail {:beta 0}}}]                 ; cascade_selection.clj:36
   [:invalid-habit "Cascade selection refused"
    {:refusal {:kind :invalid-habit :detail {}}}]
   [:invalid-free-energy "Cascade selection refused"
    {:refusal {:kind :invalid-free-energy :detail {}}}]
   [:no-admissible-candidate "Cascade selection refused"
    {:refusal {:kind :no-admissible-candidate :detail {}}}]
   [:unmapped-candidate "Cascade selection refused"
    {:refusal {:kind :unmapped-candidate :detail {}}}]])

(deftest each-registered-selection-kind-lands-typed
  (doseq [[kind msg data] registered-shapes]
    (testing kind
      (let [r (run (ex-info msg data))]
        (is (= kind (kind-of r)))
        (is (= [:machine-failure] (mapv :repair-class (:findings r))))))))

(deftest a-kind-beneath-an-untyped-wrapper-is-read
  (is (= :substrate-unreachable
         (kind-of (run (ex-info "wrapper" {:phase :selection}
                                (ex-info (:failure-error live) (:failure-data live))))))))

(deftest explicit-typing-wins
  (is (= :abstained (kind-of (run (ex-info "x" {:kind :a :outcome :abstained})))))
  (is (= :b (kind-of (run (ex-info "x" {:kind :a :failure-kind :b})))))
  (is (= :b (kind-of (run (ex-info "x" {:kind :a} (ex-info "y" {:failure-kind :b})))))
      "a typed key deeper in the chain still wins"))

(deftest what-the-branch-must-not-lift
  ;; substrate.clj:69-73's non-2xx shape: no :kind at all
  (is (= :untyped-failure
         (kind-of (run (ex-info "substrate request failed"
                                {:method :get :url "http://127.0.0.1:7073/api/entities"
                                 :status 503 :body "unavailable"})))))
  (is (= :untyped-failure (kind-of (run (ex-info "x" {:kind "substrate-unreachable"}))))
      "a string :kind is not lifted")
  (is (= :untyped-failure (kind-of (run (ex-info "x" {:refusal :accumulation-identity-missing}))))
      "a keyword :refusal (war_machine.clj:1529) is not a {:refusal {:kind ...}} map")
  (is (= :untyped-failure (kind-of (run (ex-info "x" {:status :missing :reason :r}))))
      "other keywords in ex-data are not lifted"))

(deftest judge-and-gate-refusals-unchanged
  (let [jr (run (ex-info "cascade decision refused" {:kind :live-c-stale}) {:target "M"})
        gr (run (ex-info "Inadmissible decision" {:error :inadmissible-decision :reason :flat-action})
                {:target "M"})]
    (is (= :abstained (kind-of jr)))
    (is (= :live-c-stale (get-in jr [:record :decision :abstention :targets 0 :kind])))
    (is (= [:environmental-hold] (mapv :repair-class (:findings jr))))
    (is (= :abstained (kind-of gr)))
    (is (= :flat-action (get-in gr [:record :decision :abstention :targets 0 :kind])))))
