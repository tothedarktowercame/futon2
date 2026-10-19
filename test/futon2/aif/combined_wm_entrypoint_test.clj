(ns futon2.aif.combined-wm-entrypoint-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.combined-wm-entrypoint :as entry]))

(defn- temp-provider []
  (let [f (java.io.File/createTempFile "wm-evidence-provider" ".edn")]
    (spit f "{:schema :wm/test-provider}")
    (.deleteOnExit f)
    (.getCanonicalPath f)))

(defn- config [mode]
  (entry/seal-config
   (cond-> {:schema entry/schema :mode mode
            :trigger ({:scheduled :wallclock-cron
                       :on-demand :duree-click-on-demand}
                      mode)}
     (#{:scheduled :on-demand} mode)
     (merge {:author "author-1" :reviewer "reviewer-1"
             :repair-reviewer "repair-reviewer-1"
             :ordinary-click-budget 0
             :opportunity-authority
             {:path "deploy/wm-opportunity-authority.edn"
              :sha256 (apply str (repeat 64 "a"))}
             :outer-policy-evidence-provider-path (temp-provider)}))
   "deploy/wm-combined-config.edn"))

(deftest scheduled-combined-dispatches-exactly-once
  (let [calls (atom [])
        result (entry/execute! :scheduled (config :scheduled)
                               {:identity-fn (constantly "run-one")
                                :runtime-fn #(do (swap! calls conj %)
                                                 {:outcome :closed})})
        opts (first @calls)]
    (is (= 1 (count @calls)))
    (is (= [:wallclock-cron :scheduled "run-one" "run-one"]
           [(:trigger opts) (:combined-entrypoint-mode opts)
            (:run-id opts) (:opportunity-id opts)]))
    (is (= ["author-1" "reviewer-1" "repair-reviewer-1"]
           ((juxt :author :reviewer :repair-reviewer) opts)))
    (is (zero? @(:click-budget/state opts)))
    (is (= (:run-binding result) (:combined-run-binding opts)))
    (is (= 0 (:exit-code result)))))

(deftest diagnostic-is-an-explicitly-separate-mode
  (let [live (atom 0) diagnostic (atom 0)
        cfg (entry/seal-config
             (assoc (dissoc (config :diagnostic) :config-pin)
                    :diagnostic-days 9)
             "deploy/wm-combined-config.edn")
        result (entry/execute! :diagnostic cfg
                               {:runtime-fn #(swap! live inc)
                                :diagnostic-fn #(do (swap! diagnostic inc)
                                                    {:days %})})]
    (is (zero? @live))
    (is (= 1 @diagnostic))
    (is (= {:days 9} (:result result)))
    (is (= :diagnostic-complete (:status result)))))

(deftest missing-authority-refuses-without-fallback
  (doseq [[mutation absent]
          [[#(dissoc % :outer-policy-evidence-provider-path)
            :outer-policy-evidence-provider/path]
           [#(dissoc % :author) :casts/explicit]
           [#(assoc % :ordinary-click-budget 1)
            :ordinary-click-budget/explicit-zero]
           [#(assoc % :trigger :duree-click-on-demand)
            :scheduling/trigger]]]
    (let [calls (atom 0)
          cfg (entry/seal-config (dissoc (mutation (config :scheduled))
                                         :config-pin)
                                 "deploy/wm-combined-config.edn")
          result (entry/execute! :scheduled cfg
                                 {:runtime-fn #(swap! calls inc)})]
      (is (= :refused (:status result)))
      (is (some #{absent} (:absent-terms result)))
      (is (zero? @calls))
      (is (pos? (:exit-code result))))))

(deftest config-pin-and-mode-are-binding
  (let [cfg (config :scheduled)]
    (is (= :refused (:status (entry/execute! :on-demand cfg))))
    (is (= :refused (:status (entry/execute!
                              :scheduled (assoc cfg :author "forged")))))))

(deftest runtime-refusal-is-faithful-and-nonzero
  (let [result (entry/execute! :on-demand (config :on-demand)
                               {:identity-fn (constantly "one-opportunity")
                                :runtime-fn (constantly
                                             {:outcome :no-selection
                                              :reason :policy-evidence-refused})})]
    (is (= :runtime-incomplete (:status result)))
    (is (= {:outcome :no-selection :reason :policy-evidence-refused}
           (:runtime-result result)))
    (is (= 1 (:exit-code result)))
    (is (= (:run-id result) (:opportunity-id result)))))

(deftest cli-missing-config-is-a-typed-nonzero-result
  (let [result (entry/run-cli! ["scheduled"] {})]
    (is (= :combined-entrypoint-config-required (:reason result)))
    (is (= 2 (:exit-code result)))))
