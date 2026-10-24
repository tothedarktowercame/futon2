(ns futon2.aif.clean-click-composition-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.full-loop-runner :as runner]))

(deftest scorer-registers-at-load-and-verification-does-not-recapture
  (let [saved @identity/registry]
    (try
      (swap! identity/registry dissoc 'futon2.aif.wm.family-canonical-g)
      (require 'futon2.aif.wm.family-canonical-g :reload)
      (let [entry (get @identity/registry 'futon2.aif.wm.family-canonical-g)]
        (is (some? (:sha256 entry)) "real scorer load captures its bytes")
        (is (= :stale (:status (identity/check entry "fixture" (constantly (.getBytes "changed")))))))
      (finally (reset! identity/registry saved)))))

(deftest declared-composition-rejects-a-stale-dependency
  (let [verify (ns-resolve 'futon2.aif.load-identity 'composition-status)
        scope (some-> (ns-resolve 'futon2.aif.load-identity 'source-composition) deref)]
    (is (some? verify))
    (is (contains? (set scope) 'futon2.aif.wm.family-canonical-g))
    (when verify
      (doseq [bad [:stale :unregistered :unavailable]]
        (let [reports (assoc (zipmap scope (repeat {:status :current}))
                             'futon2.aif.wm.family-canonical-g {:status bad})
              before @identity/registry]
          (with-redefs [identity/report (fn [& _] reports)]
            (is (= :unavailable (:status (verify))))
            (is (= bad (get-in (verify) [:defects 'futon2.aif.wm.family-canonical-g :status])))
            (is (= before @identity/registry))))))))

(deftest runtime-race-guard-uses-the-same-dependency-scope
  (let [reports (zipmap identity/source-composition (repeat {:status :current}))
        guard (ns-resolve 'futon2.aif.full-loop-runner 'refuse-on-runner-source-drift!)]
    (is (= :current (:status (identity/composition-status reports))))
    (with-redefs [runner/runner-source-drift
                  (constantly {:runner/source-check :current
                               :namespaces (assoc reports 'futon2.aif.wm.family-canonical-g
                                                  {:status :stale})})]
      (is (= :stale-outer-selection-composition
             (try (guard) nil (catch clojure.lang.ExceptionInfo e (:failure-kind (ex-data e)))))))))
