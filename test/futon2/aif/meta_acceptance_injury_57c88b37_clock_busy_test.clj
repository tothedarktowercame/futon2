(ns futon2.aif.meta-acceptance-injury-57c88b37-clock-busy-test
  "Out-of-band acceptance for injury/57c88b37ed879eb3eccbdce1af3710aecd276a4e.

  The occurrence-square contact received Agency's typed pre-dispatch
  clock-store-busy refusal. The repaired handoff boundary preserves that
  throwable for the surrounding restartable phase; the counterfactual turns
  it into a :contact-threw result, so these assertions fail without error."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.handoff-contract :as handoff]))

(deftest injury-57c88b37-pre-dispatch-clock-busy-is-rethrown
  (let [busy (ex-info "Agency clock store busy; dispatch not created"
                      {:outcome :clock-store-busy
                       :failure-kind :clock/store-busy
                       :error/code :clock/store-busy
                       :dispatch/not-created? true
                       :status 503})
        thrown (try
                 (handoff/exchange
                  {:contact (fn [_] (throw busy)) :request {}})
                 nil
                 (catch clojure.lang.ExceptionInfo e e))]
    (is (identical? busy thrown))
    (is (= (ex-data busy) (ex-data thrown)))))
