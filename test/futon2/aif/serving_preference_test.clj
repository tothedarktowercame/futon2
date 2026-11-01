(ns futon2.aif.serving-preference-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [clojure.edn :as edn]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.preference-registry :as registry]
            [futon2.report.war-machine :as wm]
            [futon2.aif.wm.reading-family-decision-test :as rfd]
            [futon2.aif.wm.family-selection :as families]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.test-support.runner-fixture :as runner-fixture]
            [wm-run-facts :as facts]))

(use-fixtures :once hermetic/with-hermetic-stores runner-fixture/with-hermetic-traces)

(defn fixture-record
  "Real runtime judge composition, family scoring/decision, runner, and tick writer.
   The report/field assembly port supplies a small pinned reading fixture;
   no Agency contact or work execution is exercised. The worktree source-drift
   guard alone is bypassed; the runner stops at cascade-enactment-open."
  [dir]
  (let [fx (#'rfd/fixture ["p/a" "p/c"])
        loads (atom 0)
        load-registry registry/load!
        generated (atom nil)
        persisted
        (with-redefs [registry/load! (fn [path] (swap! loads inc) (load-registry path))
                      wm/accumulation-config (constantly {})
                      wm/generate-war-machine
                      (fn [_ opts]
                        (let [r (#'rfd/decide fx [(get-in fx [:rows :current])]
                                            {"M-current" 1} opts)]
                          (reset! generated r)
                          {:judgement {:decision (:decision r) :belief {} :belief-pre {}
                                       :observation {} :free-energy {} :prediction-errors {}
                                       :precision-state {} :micro-step-trace [] :mode :maintain}}))]
          (let [judge (:judge-fn (runtime/production-defaults {:task {:id "M-current"}}))
                first-result (judge 1)
                _ (judge 1)]
            (with-redefs [runner/refuse-on-runner-source-drift! (fn [] nil)]
              (runner/run-opportunity!
               (merge (runner-fixture/isolated-runner-opts)
                      {:run-id "serving-preference-fixture"
                       :run-record-dir dir
                       :on-prod-history-root (str dir "/checkpoints")
                       :observation-labels-path (str dir "/labels.edn")
                       :judge-fn (fn [_] first-result)
                       :selected-reading-ask-fn
                       (fn [_] (throw (ex-info "fixture stops before external reading"
                                              {:kind :fixture-reading-unavailable})))
                       :dispatch-fn (fn [& _] (throw (ex-info "no external dispatch" {})))})))))

        record (edn/read-string (slurp (:run-record persisted)))
        export (facts/facts-for-record record (:run-record persisted)
                                     {:open-missions #{"M-current"} :open-excursions #{}
                                      :open-tickets #{} :patterns #{"p/a" "p/b" "p/c"}
                                      :seats #{} :pins {:fixture true}})]
    {:record record :export export :loads @loads :generated @generated
     :path (:run-record persisted)}))

(deftest runtime-carrier-survives-the-real-tick-writer
  (let [dir (str (java.nio.file.Files/createTempDirectory
                  "serving-preference-" (make-array java.nio.file.attribute.FileAttribute 0)))
        {:keys [record export loads generated]} (fixture-record dir)]
    (is (= 1 loads))
    (is (= (registry/load!) (get-in record [:decision :selection-certificate :factorized-preference])) (pr-str generated))
    (is (not (contains? (get-in export [:facts "factorizedPreference"]) "not-recomputable")))
    (is (seq (get-in export [:facts "calibrationAuthorizations"])))
    (is (seq (get-in export [:facts "factorizationAuthorizations"])))))

(deftest missing-registry-refuses-before-selection
  (let [calls (atom 0)
        judge (:judge-fn (runtime/production-defaults
                          {:preference-registry-path "/nonexistent/registry.edn"}))]
    (with-redefs [wm/generate-war-machine (fn [& _] (swap! calls inc))]
      (dotimes [_ 2]
        (is (= :production-registries-unconfigured
               (get-in (judge 1) [:judgement :decision :refusals 0 :kind])))))
    (is (zero? @calls))))

(deftest corrupted-consumed-kernel-is-refused-at-decision-boundary
  (let [fx (#'rfd/fixture ["p/a" "p/c"])
        select families/select-over-families]
    (with-redefs [families/select-over-families
                  (fn [& args]
                    (update (apply select args) :ranked
                            #(mapv (fn [row] (assoc-in row [:class-emission :certificate :consumed-g :C :steps] [])) %)))]
      (let [result (#'rfd/decide fx [(get-in fx [:rows :current])] {"M-current" 1}
                               {:task {:id "M-current"} :factorized-preference (registry/load!)})]
        (is (= :computed-kernel-not-consumed
               (get-in result [:decision :refusals 0 :kind])) (pr-str result))))))

(deftest forged-terminal-never-falls-back
  (is (= :computed-kernel-not-consumed
         (try (registry/terminal-distribution! (assoc (registry/load!) :terminal-distribution nil))
              nil (catch clojure.lang.ExceptionInfo e (:kind (ex-data e)))))))
