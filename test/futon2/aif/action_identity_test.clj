(ns futon2.aif.action-identity-test
  (:require [clojure.edn :as edn]
            [clojure.walk :as walk]
            [clojure.test :refer [deftest is]]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.close-retention :as retention]))

(def historical (edn/read-string (slurp "test/fixtures/occurrence-1789964661.edn")))
(def action (:action/value historical))
(defn mint [a]
  (retention/mint-occurrence
   {:run-id "test" :cohort-id "test" :attempt-id "test" :selected-action a
    :now (constantly "2026-09-21T04:24:00Z")
    :uuid-fn (constantly "00000000-0000-0000-0000-000000000001")}))
(defn refusal [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (:close-retention/refusal (ex-data e)))))

(deftest opposite-printers-round-trip-and-hostile-settings
  (doseq [mode [false true]]
    (let [o (binding [*print-namespace-maps* mode *print-length* 1 *print-level* 1
                     *print-meta* true *print-dup* true *print-readably* false]
              (mint action))
          reread (edn/read-string (identity/printed mode o))]
      (is (= :wm/action-transition-occurrence-v2 (:schema o)))
      (is (= o (binding [*print-namespace-maps* (not mode)]
                 (retention/validate-occurrence reread))))
      (is (= (:action/value-sha256 (mint action)) (:action/value-sha256 o))))))

(deftest unordered-controls-and-full-field-drift
  (let [reordered (walk/postwalk
                   #(cond (map? %) (into (sorted-map-by (fn [a b] (compare (pr-str b) (pr-str a)))) %)
                          (set? %) (into (sorted-set-by (fn [a b] (compare (pr-str b) (pr-str a)))) %)
                          :else %) action)
        o (mint action)
        pattern :apparatus/one-authority-per-question
        token ["M-aif-policy-conditioned-eig" :hole/h6378c65a4012]]
    (is (= action reordered))
    (is (= (identity/digest action) (identity/digest reordered)))
    (doseq [changed [(assoc-in action [:precedence 0 :produces] #{:different-token})
                     (assoc-in action [:precedence 0 :guard :operator] :or)
                     (assoc-in action [:observation-locators token :path] "different.md")
                     (assoc-in action [:interpretation-receipts pattern :reading] "different")
                     (assoc action :new-semantic-field :new-value)]]
      (is (= :occurrence-action-drift
             (refusal #(retention/validate-occurrence (assoc o :action/value changed)))))
      (is (not= (:action/value-sha256 o) (:action/value-sha256 (mint changed)))))))

(deftest explicit-type-number-and-unsupported-rules
  (doseq [x [nil true "text" \newline :ns/key 'ns/symbol 4 4N 1/3 1.00M 0.25
             -0.0 #uuid "00000000-0000-0000-0000-000000000001"
             #inst "2026-09-21T00:00:00Z" (list 1 :a) [1 :a] #{1 :a} {:a [1]}]]
    (is (= (identity/digest x) (identity/digest (edn/read-string (identity/printed false x))))))
  (is (= (identity/digest 4) (identity/digest 4N)))
  (is (= (identity/digest 1/2)
         (identity/digest (clojure.lang.Ratio. (biginteger 2) (biginteger 4)))))
  (is (= (identity/digest 1.00M) (identity/digest 1M)))
  (is (not= (identity/digest (list 1 2)) (identity/digest [1 2])))
  (is (not= (identity/digest [1 2]) (identity/digest [2 1])))
  (doseq [x [(Object.) (float 0.5) ##NaN ##Inf (map identity [1 2])
             (with-meta {:a 1} {:hidden :identity})]]
    (is (= :action-identity-value-unsupported (refusal #(identity/digest x))))))

(deftest immutable-legacy-fixture-and-two-evidenced-modes
  (doseq [ambient [false true]]
    (binding [*print-namespace-maps* ambient]
      (is (= historical (retention/validate-occurrence historical)))
      (is (= [false] (:matched-print-namespace-maps
                     (retention/occurrence-identity-receipt historical))))))
  (let [other (assoc historical :action/value-sha256 (identity/sha256 (identity/printed true action)))]
    (is (= [true] (:matched-print-namespace-maps (retention/occurrence-identity-receipt other)))))
  (is (= :occurrence-action-drift
         (refusal #(retention/validate-occurrence
                    (assoc-in historical [:action/value :target] "changed")))))
  (is (= :occurrence-action-drift
         (refusal #(retention/validate-occurrence
                    (assoc-in historical [:action/value :new-semantic-field] :new)))))
  (is (not= (:action/value-sha256 historical) (:action/value-sha256 (mint action)))))
