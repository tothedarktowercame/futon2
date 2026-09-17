(ns futon2.aif.c-fold-config-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.edn :as edn]
            [futon2.aif.c-fold-config :as config]
            [futon2.aif.efe :as efe]))

(def sheet "holes/labs/wm-contract/runs/F2-run4-readiness/RUN4-config-2026-09-09.edn")
(def base {:ambiguity-mode :variance-sum :risk-mode :hinge})
(def state {:belief {:x 0.5} :observation {:mission-health 0.5}})
(defn refusal [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (:reason (ex-data e)))))

(deftest absent-and-explicit-false-preserve-behavior
  (let [no-read (fn [_] (throw (Exception. "unexpected read")))
        legacy (efe/compute-efe state {:type :no-op} base)
        off (config/resolve-opts (assoc base :ruled-outcome-c-enabled? false) "missing" no-read)]
    (is (= base (config/resolve-opts base nil no-read)))
    (is (= (pr-str legacy) (pr-str (efe/compute-efe state {:type :no-op} off))))
    (is (not (contains? legacy :c-fold-provenance)))))

(deftest invalid-pins-and-selectors-refuse
  (let [original (edn/read-string (slurp sheet))
        try-sheet (fn [value read-artifact]
                    (config/resolve-opts {} sheet
                      #(if (= % sheet) (pr-str value) (read-artifact %))))]
    (doseq [[value reason]
            [[(assoc-in original [:c-fold :seed :sha256] "wrong") :source-pin-mismatch]
             [(assoc-in original [:c-fold :kernel :sha256] "wrong") :source-pin-mismatch]
             [(assoc-in original [:c-fold :seed :id] :unknown) :unknown-c-selector]
             [(assoc-in original [:c-fold :enabled?] "true") :invalid-enabled-flag]
             [(dissoc original :c-fold) :unmaterialized-legacy-fold-config]]]
      (is (= reason (refusal #(try-sheet value slurp)))))
    (is (= :unreadable-source (refusal #(try-sheet original (fn [_] (throw (Exception.)))))))
    (is (= {:ruled-outcome-c-enabled? false}
           (try-sheet (assoc original :c-fold {:enabled? false})
                      (fn [_] (throw (Exception. "must not load"))))))))
