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
(def channels [{:channel/id "health" :value 0.25}
               {:channel/id "latency" :value 0.75}])
(def prediction {:mean {"health" 0.2 "latency" 0.8}
                 :variance {"health" 0.1 "latency" 0.2}})
(defn- at [id i m] (merge common {:tick/id id :tick/index i} m))
(defn- rows [produced]
  [{:candidate/id "candidate-a" :action {:type :a} :action/model-revision "model-v1"
    :produced-at/tick-id produced :prediction prediction}
   {:candidate/id "candidate-b" :action {:type :b} :action/model-revision "model-v1"
    :produced-at/tick-id produced :prediction prediction}])

(def records
  {:commission {:schema :wm/e4-commission-v1 :node :R10 :commission/id "commission-1"
                :tick/plan plan}
   :dispatch {:schema :wm/e4-dispatch-v1 :node :R10 :commission/id "commission-1"
              :dispatch/id "dispatch-1" :execution/outcome :success}
   :run-launch {:schema :wm/e4-run-launch-v1 :commission/id "commission-1"
                :dispatch/id "dispatch-1" :launch/id "launch-1"
                :launch/semantics :idempotent :tick/plan plan
                :run/id "run-1" :model/id "wm" :model/revision "model-v1"}
   :launch-history [{:schema :wm/e4-launch-history-v1 :commission/id "commission-1"
                     :dispatch/id "dispatch-1" :launch/id "launch-1" :run/id "run-1"}]
   :tick-entries (mapv (fn [i id] (at id i {:schema :wm/e4-tick-entry-v1
                                             :launch/id "launch-1"})) (range) plan)
   :observations (mapv (fn [i id] (at id i {:schema :wm/e4-r2-observation-v1
                                             :observation/channels channels})) (range) plan)
   :occurrence-universe
   (mapv (fn [i id] (at id i {:schema :wm/e4-occurrence-universe-v1
                               :candidate/support support})) (range) plan)
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

(defn- with-input-refs [rs]
  (let [obs-by-id (into {} (map (juxt :tick/id identity)) (:observations rs))
        pred-by-id (into {} (map (juxt :tick/id identity)) (:predecessor-predictions rs))
        obs-bytes (.getBytes (str (pr-str (:observations rs)) "\n") "UTF-8")
        pred-bytes (.getBytes (str (pr-str (:predecessor-predictions rs)) "\n") "UTF-8")]
    (update rs :r8-occurrences
            (fn [ticks]
              (mapv (fn [tick]
                      (if (= :complete (:coverage tick))
                        (let [obs (obs-by-id (:tick/id tick))
                              pred (pred-by-id (:tick/id tick))
                              pmap (into {} (map (juxt :candidate/id identity)) (:rows pred))]
                          (update tick :rows
                                  (fn [rows]
                                    (mapv (fn [row]
                                            (let [cid (:candidate/id row)]
                                              (assoc row
                                                     :observation/ref {:source-role :observations
                                                                       :tick/id (:tick/id tick)
                                                                       :sha256 (sha obs-bytes)}
                                                     :prediction/ref {:source-role :predecessor-predictions
                                                                      :tick/id (:tick/id tick)
                                                                      :candidate/id cid
                                                                      :sha256 (sha pred-bytes)}
                                                     :observation/input (:observation/channels obs)
                                                     :prediction/input (:prediction (pmap cid))))) rows))))
                        tick)) ticks)))))

(defn- fixture [overrides]
  (let [dir (.toFile (Files/createTempDirectory "e4-route" (make-array java.nio.file.attribute.FileAttribute 0)))
        rs (with-input-refs (merge records overrides))
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
         (refusal (assoc (fixture {}) :scope :candidate-asserted-production))))
  (is (= :e4/production-authority-unavailable
         (refusal (assoc (fixture {}) :scope :independently-retained-production))))
  (is (= :e4/source-schema-mismatch
         (refusal (fixture {:observations
                            (assoc-in (:observations records) [1 :schema] :candidate/schema)})))))

(deftest semantic-payload-and-independent-membership-controls
  (testing "the three independently commissioned review counterexamples"
    (is (= :e4/invalid-observation-payload
           (refusal (fixture {:observations
                              (mapv #(dissoc % :observation/channels) (:observations records))}))))
    (is (= :e4/prediction-occurrence-mismatch
           (refusal (fixture
                     (into {} (for [role [:predecessor-predictions :r8-occurrences]]
                                [role (mapv #(if (:rows %)
                                               (update % :rows
                                                       (fn [rs] (mapv (fn [r] (dissoc r :action)) rs))) %)
                                            (get records role))]))))))
    (is (= :e4/candidate-coverage-mismatch
           (refusal (fixture
                     (into {} (for [role [:predecessor-predictions :r8-occurrences]]
                                [role (mapv #(cond-> (assoc % :candidate/support ["candidate-a"])
                                              (:rows %) (update :rows (fn [rs] [(first rs)])))
                                            (get records role))])))))))
  (testing "a complete universe preserves duplicate semantic actions as occurrences"
    (let [same-actions (mapv #(if (:rows %)
                               (assoc % :rows
                                      (mapv (fn [r] (assoc r :action {:type :same})) (:rows %))) %)
                             (:predecessor-predictions records))
          same-r8 (mapv #(if (:rows %)
                           (assoc % :rows
                                  (mapv (fn [r] (assoc r :action {:type :same})) (:rows %))) %)
                         (:r8-occurrences records))]
      (is (= :verified-causal-route
             (:status (e4/verify-route! (fixture {:predecessor-predictions same-actions
                                                  :r8-occurrences same-r8})))))))
  (testing "an R8 occurrence cannot borrow another tick's observation reference"
    (let [authority (fixture {})
          original ((get-in authority [:sources :r8-occurrences :resolve]))
          rows (read-string (String. original "UTF-8"))
          changed (assoc-in rows [1 :rows 0 :observation/ref :tick/id] "tick-2")
          bs (.getBytes (str (pr-str changed) "\n") "UTF-8")
          authority' (-> authority
                         (assoc-in [:sources :r8-occurrences :sha256] (sha bs))
                         (assoc-in [:sources :r8-occurrences :resolve] (constantly bs)))]
      (is (= :e4/prediction-occurrence-mismatch (refusal authority')))))
  (testing "resolved prediction input mutation refuses"
    (let [authority (fixture {})
          original ((get-in authority [:sources :r8-occurrences :resolve]))
          rows (read-string (String. original "UTF-8"))
          changed (assoc-in rows [1 :rows 0 :prediction/input :mean "health"] 99.0)
          bs (.getBytes (str (pr-str changed) "\n") "UTF-8")]
      (is (= :e4/prediction-occurrence-mismatch
             (refusal (-> authority
                          (assoc-in [:sources :r8-occurrences :sha256] (sha bs))
                          (assoc-in [:sources :r8-occurrences :resolve] (constantly bs))))))))
  (testing "declared idempotence without a unique resolved launch is insufficient"
    (is (= :e4/launch-uniqueness-unwitnessed
           (refusal (fixture {:launch-history
                              (conj (:launch-history records)
                                    {:schema :wm/e4-launch-history-v1
                                     :commission/id "commission-1" :dispatch/id "dispatch-1"
                                     :launch/id "launch-2" :run/id "run-2"})}))))))

(deftest strict-utf8-reporting-decoder
  (let [authority (fixture {})
        malformed (byte-array [(byte 0xc3) (byte 0x28)])]
    (is (= :e4/invalid-utf8
           (refusal (-> authority
                        (assoc-in [:sources :commission :sha256] (sha malformed))
                        (assoc-in [:sources :commission :resolve] (constantly malformed)))))))
  (testing "a genuine replacement character is valid UTF-8, then fails only schema semantics"
    (let [authority (fixture {})
          valid (.getBytes (pr-str {:schema :wrong :text "\ufffd"}) "UTF-8")]
      (is (= :e4/source-schema-mismatch
             (refusal (-> authority
                          (assoc-in [:sources :commission :sha256] (sha valid))
                          (assoc-in [:sources :commission :resolve] (constantly valid)))))))))
