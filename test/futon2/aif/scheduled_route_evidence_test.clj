(ns futon2.aif.scheduled-route-evidence-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [futon2.aif.scheduled-route-evidence :as e4])
  (:import [java.nio.file Files]
           [java.security MessageDigest]
           [java.math BigInteger]))

(defn- sha [^bytes bs]
  (format "%064x" (BigInteger. 1 (.digest (MessageDigest/getInstance "SHA-256") bs))))

(def plan ["tick-0" "tick-1" "tick-2"])
(def support ["candidate-a" "candidate-b"])
(def common {:run/id "run-1" :model/id "wm" :model/revision "model-v1"})
(defn- at [id i m] (merge common {:tick/id id :tick/index i} m))
(defn- rows [produced]
  [{:candidate/id "candidate-a" :action {:type :a} :action/model-revision "model-v1"
    :produced-at/tick-id produced}
   {:candidate/id "candidate-b" :action {:type :b} :action/model-revision "model-v1"
    :produced-at/tick-id produced}])

(def records
  {:commission {:schema :wm/e4-commission-v1 :node :R10 :commission/id "commission-1"
                :tick/plan plan}
   :dispatch {:schema :wm/e4-dispatch-v1 :node :R10 :commission/id "commission-1"
              :dispatch/id "dispatch-1" :execution/outcome :success}
   :run-launch {:schema :wm/e4-run-launch-v1 :commission/id "commission-1"
                :dispatch/id "dispatch-1" :launch/id "launch-1"
                :launch/semantics :idempotent :tick/plan plan
                :run/id "run-1" :model/id "wm" :model/revision "model-v1"}
   :tick-entries (mapv (fn [i id] (at id i {:schema :wm/e4-tick-entry-v1
                                             :launch/id "launch-1"})) (range) plan)
   :observations (mapv (fn [i id] (at id i {:schema :wm/e4-r2-observation-v1
                                             :observation/channels [0.1 0.2]})) (range) plan)
   :predecessor-predictions
   [(at "tick-0" 0 {:schema :wm/e4-predecessor-v1 :coverage :off :reason :initial-tick
                     :candidate/support support})
    (at "tick-1" 1 {:schema :wm/e4-predecessor-v1 :coverage :complete
                     :predecessor/tick-id "tick-0" :candidate/support support :rows (rows "tick-0")})
    (at "tick-2" 2 {:schema :wm/e4-predecessor-v1 :coverage :complete
                     :predecessor/tick-id "tick-1" :candidate/support support :rows (rows "tick-1")})]
   :r8-occurrences
   [(at "tick-0" 0 {:schema :wm/e4-r8-occurrences-v1 :coverage :off :reason :initial-tick
                     :candidate/support support})
    (at "tick-1" 1 {:schema :wm/e4-r8-occurrences-v1 :coverage :complete
                     :candidate/support support :rows (rows "tick-0")})
    (at "tick-2" 2 {:schema :wm/e4-r8-occurrences-v1 :coverage :complete
                     :candidate/support support :rows (rows "tick-1")})]})

(defn- fixture [overrides]
  (let [dir (.toFile (Files/createTempDirectory "e4-route" (make-array java.nio.file.attribute.FileAttribute 0)))
        rs (merge records overrides)
        entries (into {}
                      (for [role e4/source-roles
                            :let [f (io/file dir (str (name role) ".edn"))
                                  bs (.getBytes (str (pr-str (get rs role)) "\n") "UTF-8")]]
                        (do (Files/write (.toPath f) bs (make-array java.nio.file.OpenOption 0))
                            [role {:path (.getPath f) :sha256 (sha bs)}])))]
    (e4/file-authority entries)))

(defn- refusal [authority]
  (try (e4/verify-route! authority) nil
       (catch clojure.lang.ExceptionInfo x (:refusal (ex-data x)))))

(deftest verifies-one-multi-tick-causal-route
  (let [out (e4/verify-route! (fixture {}))]
    (is (= :verified-causal-route (:status out)))
    (is (= plan (:tick/plan out)) "one commission retains all ordered ticks")
    (is (false? (:scheduler-is-f-pi-operand? out)))
    (is (false? (:production-edge-fired? out)))
    (is (= (set e4/source-roles) (set (keys (:source-pins out)))))))

(deftest tick-plan-and-run-join-controls
  (testing "cross-run receipt"
    (is (= :e4/run-tick-model-identity-mismatch
           (refusal (fixture {:observations (assoc-in (:observations records) [1 :run/id] "other-run")})))))
  (doseq [[label ticks]
          [[:missing (pop (:tick-entries records))]
           [:duplicate (conj (:tick-entries records) (second (:tick-entries records)))]
           [:extra (conj (:tick-entries records) (at "tick-extra" 3 {:launch/id "launch-1"}))]
           [:reordered (vec (reverse (:tick-entries records)))]]]
    (testing (name label)
      (is (keyword? (refusal (fixture {:tick-entries ticks}))))))
  (testing "undeclared tick in bounded run"
    (is (= :e4/tick-plan-coverage-mismatch
           (refusal (fixture {:tick-entries (assoc-in (:tick-entries records) [2 :tick/id] "tick-x")}))))))

(deftest predecessor-and-occurrence-controls
  (testing "missing predecessor row"
    (is (= :e4/missing-stale-or-reordered-predecessor
           (refusal (fixture {:predecessor-predictions
                              (assoc-in (:predecessor-predictions records) [1 :rows]
                                        [(first (rows "tick-0"))])})))))
  (testing "stale predecessor"
    (is (= :e4/missing-stale-or-reordered-predecessor
           (refusal (fixture {:predecessor-predictions
                              (assoc-in (:predecessor-predictions records) [2 :predecessor/tick-id]
                                        "tick-0")})))))
  (testing "changed action"
    (is (= :e4/prediction-occurrence-mismatch
           (refusal (fixture {:r8-occurrences
                              (assoc-in (:r8-occurrences records) [1 :rows 0 :action]
                                        {:type :changed})})))))
  (testing "changed model"
    (is (= :e4/prediction-occurrence-mismatch
           (refusal (fixture {:predecessor-predictions
                              (assoc-in (:predecessor-predictions records)
                                        [1 :rows 0 :action/model-revision] "model-v2")})))))
  (testing "dropped candidate coverage"
    (is (= :e4/missing-stale-or-reordered-predecessor
           (refusal (fixture {:r8-occurrences
                              (assoc-in (:r8-occurrences records) [1 :rows]
                                        [(first (rows "tick-0"))])})))))
  (testing "no invented initial prediction"
    (is (= :e4/initial-tick-prediction-required-off
           (refusal (fixture {:predecessor-predictions
                              (assoc-in (:predecessor-predictions records) [0 :coverage]
                                        :complete)}))))))

(deftest byte-authority-and-production-scope-controls
  (let [authority (fixture {})
        bad (assoc-in authority [:sources :commission :sha256] (apply str (repeat 64 "0")))]
    (is (= :e4/source-digest-mismatch (refusal bad))))
  (is (= :e4/untrusted-source-scope
         (refusal (assoc (fixture {}) :scope :candidate-asserted-production)))))
