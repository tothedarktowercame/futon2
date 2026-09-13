(ns futon2.aif.machine-slow-prior-evidence-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.machine-slow-prior-evidence :as e5]
            [futon2.aif.temporal-hierarchy :as hierarchy])
  (:import (java.nio.charset StandardCharsets)
           (java.nio.file Files)
           (java.security MessageDigest)))

(def binding-id {:model/id :wm-e5 :model/revision "model-5" :run/id "run-e5" :tick/index 8})
(def candidates
  [{:candidate/occurrence-id [:run-e5 8 0] :action {:type :work :target "same"}
    :move/class :close-hole :prior 0.5 :step-score-delta -0.25}
   {:candidate/occurrence-id [:run-e5 8 1] :action {:type :work :target "same"}
    :move/class :advance-capability :prior 0.25 :step-score-delta -0.5}
   {:candidate/occurrence-id [:run-e5 8 2] :action {:type :wait}
    :move/class :unclassified :prior 0.25 :step-score-delta 0.0}])
(defn- production-shaped [rows mode]
  (mapv (fn [move] (-> move (assoc :candidate/occurrence-id (:move/id move)) (dissoc :move/id)))
        (hierarchy/apply-slow-prior
         (mapv #(-> % (assoc :move/id (:candidate/occurrence-id %)) (dissoc :candidate/occurrence-id)) rows)
         mode)))
(defn records
  ([] (records :exploitation 3))
  ([mode depth]
   (let [weights (hierarchy/mode-prior-weights mode)
         weight-authority {:id :futon2.aif.temporal-hierarchy/strategic-modes
                           :revision "temporal-hierarchy-source-v1"}]
   {:context (merge {:schema/version :wm/e5-expected-context-v1 :scope :isolated-test
                     :identity binding-id :unshaped-candidates candidates
                     :slow/mode mode :weight-table/authority weight-authority
                     :weight-table weights} binding-id)
    :unshaped (merge {:schema/version :wm/e5-unshaped-candidates-v1 :scope :isolated-test
                      :candidates candidates} binding-id)
    :slow-state (merge {:schema/version :wm/e5-slow-state-authority-v1 :scope :isolated-test
                        :slow/mode mode :slow/intrinsics {:close-hole {:alpha 4.0 :beta 2.0}}
                        :weight-table/authority weight-authority
                        :weight-table weights} binding-id)
    :shaped (merge {:schema/version :wm/e5-shaped-candidates-v1 :scope :isolated-test
                    :candidates (production-shaped candidates mode)
                    :depth/unchanged {:requested depth :effective depth}} binding-id)
    :depth (merge {:schema/version :wm/e5-independent-depth-authority-v1 :scope :isolated-test
                   :horizon/requested depth :horizon/effective depth} binding-id)})))
(defn- sha [bytes]
  (apply str (map #(format "%02x" (bit-and 0xff %))
                  (.digest (doto (MessageDigest/getInstance "SHA-256") (.update bytes))))))
(defn config [rs]
  (let [root (Files/createTempDirectory "e5-" (make-array java.nio.file.attribute.FileAttribute 0))]
    {:mode :isolated-test :root (str root)
     :sources (into {} (for [[label record] rs
                             :let [name (str (name label) ".edn")
                                   bytes (.getBytes (pr-str record) StandardCharsets/UTF_8)]]
                         (do (Files/write (.resolve root name) bytes (make-array java.nio.file.OpenOption 0))
                             [label {:relative-path name :sha256 (sha bytes)}])))}))
(defn refusal [cfg]
  (try (e5/verify-shaping cfg) nil
       (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))

(deftest production-function-is-replayed-over-complete-occurrence-domain
  (let [out (e5/verify-shaping (config (records)))]
    (is (= candidates (:unshaped out)))
    (is (= 3 (count (:shaped out))))
    (is (= (mapv :candidate/occurrence-id candidates)
           (mapv :candidate/occurrence-id (:shaped out))))
    (is (= 0.35 (get-in out [:shaped 0 :prior])))
    (is (= -0.25 (get-in out [:shaped 0 :step-score-delta-base])))
    (is (= true (get-in out [:depth :unchanged])))
    (is (= :not-performed (get-in out [:external-dependencies :r6-scoring-and-selection])))))

(deftest slow-mode-affects-shaping-while-depth-does-not
  (let [exploit (e5/verify-shaping (config (records :exploitation 3)))
        explore (e5/verify-shaping (config (records :exploration 3)))
        deeper (e5/verify-shaping (config (records :exploitation 7)))]
    (is (not= (:shaped exploit) (:shaped explore)))
    (is (= (:shaped exploit) (:shaped deeper)))
    (is (= 3 (get-in exploit [:depth :effective])))
    (is (= 7 (get-in deeper [:depth :effective])))))

(deftest authority-and-shape-refusals
  (testing "stale weights, invalid values, missing base fields and cross-run"
    (is (= :e5/slow-authority-invalid
           (refusal (config (assoc-in (records) [:slow-state :weight-table "close-hole"] 0.2)))))
    (is (= :e5/invalid-weight
           (with-redefs [hierarchy/mode-prior-weights (fn [_] {"close-hole" -0.1})]
             (refusal (config (-> (records)
                                  (assoc-in [:slow-state :weight-table] {"close-hole" -0.1})
                                  (assoc-in [:context :weight-table] {"close-hole" -0.1})))))))
    (doseq [field [:prior :step-score-delta]]
      (is (= :e5/unshaped-domain-invalid
             (refusal (config (update-in (records) [:unshaped :candidates 0] dissoc field))))))
    (is (= :e5/cross-run-or-scope
           (refusal (config (assoc-in (records) [:shaped :run/id] "other-run"))))))
  (testing "partial/reordered/mutated domain and horizon rewrite"
    (is (= :e5/domain-mismatch
           (refusal (config (update-in (records) [:shaped :candidates] pop)))))
    (is (= :e5/domain-mismatch
           (refusal (config (update-in (records) [:shaped :candidates] #(vec (reverse %)))))))
    (is (= :e5/domain-mismatch
           (refusal (config (assoc-in (records) [:shaped :candidates 0 :candidate/occurrence-id] [:other])))))
    (is (= :e5/shaping-mismatch
           (refusal (config (assoc-in (records) [:shaped :candidates 0 :action] {:type :tampered})))))
    (is (= :e5/horizon-rewrite
           (refusal (config (assoc-in (records) [:shaped :depth/unchanged :effective] 9)))))))

(deftest source-and-production-boundary-refusals
  (is (= :e5/source-set-incomplete
         (refusal (update (config (records)) :sources dissoc :slow-state))))
  (is (= :e5/source-pin-mismatch
         (refusal (assoc-in (config (records)) [:sources :unshaped :sha256]
                            (apply str (repeat 64 "0"))))))
  (is (= :e5/mode-unknown (refusal (assoc (config (records)) :mode :unknown))))
  (is (= :e5/production-authority-unavailable (refusal {:mode :production}))))

(deftest fixed-context-rejects-borrowed-or-stale-source-cohorts
  (let [borrowed (into {} (map (fn [[k v]] [k (assoc v :run/id "borrowed-run")]) (records)))]
    (is (= :e5/expected-context-mismatch
           (refusal (config (assoc-in borrowed [:context :identity :run/id] "run-e5"))))))
  (is (= :e5/expected-context-mismatch
         (refusal (config (assoc-in (records) [:unshaped :candidates 0 :prior] 0.6)))))
  (is (= :e5/expected-context-mismatch
         (refusal (config (assoc-in (records) [:slow-state :weight-table/authority :revision]
                                    "stale-revision")))))
  (let [missing-id (into {} (map (fn [[k v]] [k (dissoc v :model/id)]) (records)))]
    (is (= :e5/cross-run-or-scope (refusal (config missing-id)))))
  (is (= :e5/slow-authority-invalid
         (refusal (config (records :not-a-strategic-mode 3))))))
