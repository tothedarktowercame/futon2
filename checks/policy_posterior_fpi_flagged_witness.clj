#!/usr/bin/env bb
(ns checks.policy-posterior-fpi-flagged-witness
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pp])
  (:import [java.math BigInteger] [java.security MessageDigest]))

(def fixture "holes/labs/wm-contract/runs/2026-09-01-s4/wm-trace-s4.edn")
(def fixture-sha "aaeccaf477dfd16bcc73064aa979f1fefa9753e2eec4ccd053e5e17acf8efdbf")
(def receipt "holes/labs/wm-contract/policy-posterior-fpi-flagged-witness.edn")
(def source-pins {"scripts/futon2/report/war_machine.clj" "e91f9a9776a34600cfd4db1592c75c3f3ff800e8be0c120d95795e2fe87356f4"
                  "src/futon2/aif/policy.clj" "3980d203d4997d9fd3019540652119a6ecc295d045d89bbcf5d5922e2695bd17"
                  "src/futon2/aif/policy_precision.clj" "2e7164ecf965fe9553822a537fb543d212ec1641daeca6652b05a47a62ad094b"})
(def mutation-causes
  {"--negative-remove-f" :eligible-record-lacks-identity-joined-candidate-f
   "--negative-misjoin" :eligible-record-lacks-identity-joined-candidate-f})

(defn sha256 [s] (let [m (MessageDigest/getInstance "SHA-256")]
  (.update m (.getBytes s "UTF-8"))
  (format "%064x" (BigInteger. 1 (.digest m)))))
(defn read-forms [text]
  (with-open [r (java.io.PushbackReader. (java.io.StringReader. text))]
    (loop [xs []] (let [x (edn/read {:eof ::eof} r)]
      (if (= ::eof x) xs (recur (conj xs x)))))))
(defn policy-key [a] (when (and (map? a) (keyword? (:type a)))
  [(:type a) (cond (some? (:target a)) [:target (:target a)]
                   (some? (:target-class a)) [:target-class (:target-class a)]
                   :else [:unscoped nil])]))
(defn record-facts [i x]
  (let [ranked (:ranked-actions x) p (get-in x [:decision :f-pi-posterior])
        by (get-in x [:f-pi-by-candidate-id :by-candidate-id])
        expected-identities (mapv (comp policy-key :action) ranked)
        actual-identities (mapv :candidate-identity (vals by))
        expected-frequency (frequencies expected-identities)
        actual-frequency (frequencies actual-identities)
        joined (mapv (fn [[candidate-id entry]]
                       {:candidate-id candidate-id
                        :identity-matches-one-current?
                        (= 1 (get expected-frequency (:candidate-identity entry) 0))
                        :finite-value? (and (number? (:value entry))
                                            (Double/isFinite (double (:value entry))))})
                     by)
        eligible? (= :present (:status p))]
    {:index i :eligible? eligible? :status (:status p) :reason (:reason p)
     :candidate-count (count ranked) :declared-candidate-count (:candidate-count p)
     :applied? (:applied? p) :coverage (:coverage p)
     :candidate-map-cardinality? (= (count expected-identities) (count by))
     :identity-join? (and (= expected-frequency actual-frequency)
                          (every? #(= 1 %) (vals expected-frequency))
                          (every? :identity-matches-one-current? joined))
     :all-values-present? (every? :finite-value? joined)
     :honest-incomplete? (or eligible? (and (= :absent (:status p))
       (= :incomplete-coverage (:reason p)) (false? (:applied? p))
       (pos? (:uncovered-count p))))}))
(defn validate-records [xs]
  (let [facts (mapv record-facts (range) xs) eligible (filterv :eligible? facts)
        eligible-ok? (every? #(and (= 145 (:candidate-count %))
                                   (= (:candidate-count %) (:declared-candidate-count %))
                                   (:candidate-map-cardinality? %)
                                   (:identity-join? %) (:all-values-present? %)
                                   (:applied? %) (= :complete (:coverage %))) eligible)
        ok? (and (= 4 (count facts)) (= 3 (count eligible))
                 eligible-ok?
                 (every? :honest-incomplete? facts))]
    {:pass? ok?
     :reason (when-not ok? :eligible-record-lacks-identity-joined-candidate-f)
     :claim :policyPosteriorImportsPolicyF
     :evidence-scope :flagged-path-witness-held-open
     :denominator (count facts) :eligible-positive-count (count eligible) :records facts}))
(defn mutate [xs mode]
  (case mode
    "--negative-remove-f" (assoc-in xs [0 :f-pi-by-candidate-id :by-candidate-id "rank/1" :value] nil)
    "--negative-misjoin" (assoc-in xs [0 :f-pi-by-candidate-id :by-candidate-id "rank/1" :candidate-identity] [:wrong [:target "wrong"]])
    nil))
(defn preflight [text]
  (let [digest (sha256 text)
        source-drift (into {}
                           (keep (fn [[p pin]]
                                   (let [actual (sha256 (slurp p))]
                                     (when (not= pin actual)
                                       [p {:expected pin :actual actual}]))))
                           source-pins)]
    (cond (seq source-drift) {:pass? false :reason :source-digest-mismatch :drift source-drift}
      (not= digest fixture-sha)
      {:pass? false :reason :fixture-digest-mismatch :expected fixture-sha :actual digest}
      :else (let [result (validate-records (read-forms text))]
              (if (:pass? result) result
                  (assoc result :reason :invalid-unmutated-baseline))))))
(defn run [text mode]
  (let [baseline (preflight text)]
    (cond
      (not (:pass? baseline)) baseline
      (nil? mode) baseline
      (not (contains? mutation-causes mode))
      {:pass? false :reason :unknown-negative-control :mode mode}
      :else
      (let [forms (read-forms text) changed (mutate forms mode)
            result (validate-records changed)]
        (assoc result :mode mode :mutation-changed? (not= forms changed))))))
(defn -main [& args]
  (let [mode (first args) negative? (some? mode) result (run (slurp fixture) mode)
        detected? (and negative?
                       (false? (:pass? result))
                       (:mutation-changed? result)
                       (= (mutation-causes mode) (:reason result)))]
    (if negative?
      (do (println (if detected? "negative-control PASS" "negative-control FAIL") (pr-str result))
          (System/exit (if detected? 0 2)))
      (do (spit receipt (with-out-str (pp/pprint (assoc result :schema :policy-posterior-fpi-flagged-witness/v1 :fixture fixture :fixture-sha256 fixture-sha :source-pins source-pins))))
          (println (if (:pass? result) "PASS" "FAIL") (pr-str result))
          (System/exit (if (:pass? result) 0 1))))))
(when (= *file* (System/getProperty "babashka.file")) (apply -main *command-line-args*))
