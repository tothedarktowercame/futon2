(ns futon2.aif.machine-forward-influence-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.machine-forward-influence :as e6a]
            [futon2.aif.machine-slow-prior-evidence :as e5]
            [futon2.aif.machine-slow-prior-evidence-test :as e5-fixture]
            [futon2.aif.temporal-hierarchy :as hierarchy])
  (:import (java.nio.charset StandardCharsets)
           (java.nio.file Files)
           (java.security MessageDigest)))

(defn- sha [bytes]
  (apply str (map #(format "%02x" (bit-and 0xff %))
                  (.digest (doto (MessageDigest/getInstance "SHA-256") (.update bytes))))))
(defn- write-authority [records]
  (let [root (Files/createTempDirectory "e6a-" (make-array java.nio.file.attribute.FileAttribute 0))]
    {:authority-root (str root)
     :sources (into {} (for [[label record] records
                             :let [name (str (name label) ".edn")
                                   bytes (.getBytes (pr-str record) StandardCharsets/UTF_8)]]
                         (do (Files/write (.resolve root name) bytes (make-array java.nio.file.OpenOption 0))
                             [label {:relative-path name :sha256 (sha bytes)}])))}))
(defn- arm-config [mode] (e5-fixture/config (e5-fixture/records mode 3)))
(defn- shaped [rows mode]
  (mapv (fn [move] (-> move (assoc :candidate/occurrence-id (:move/id move)) (dissoc :move/id)))
        (hierarchy/apply-slow-prior
         (mapv #(-> % (assoc :move/id (:candidate/occurrence-id %))
                    (dissoc :candidate/occurrence-id)) rows) mode)))
(defn- base-config []
  (let [a-config (arm-config :exploitation) b-config (arm-config :exploration)
        a (e5/verify-shaping a-config) b (e5/verify-shaping b-config)
        authority (write-authority
                   {:fixed-context
                    {:schema/version :wm/e6a-fixed-comparison-context-v1 :scope :isolated-test
                     :identity (:identity a) :unshaped (:unshaped a) :depth (:depth a)
                     :arm/id-a :a :arm/id-b :b
                     :slow/modes [(:slow/mode a) (:slow/mode b)]}
                    :r6-scoring-a
                    {:schema/version :wm/e6a-r6-correspondence-status-v1 :scope :isolated-test
                     :identity (:identity a) :shaped-input (:shaped a)
                     :correspondence/status :unavailable
                     :reason :canonical-r6-score-and-posterior-proof-absent}
                    :r6-scoring-b
                    {:schema/version :wm/e6a-r6-correspondence-status-v1 :scope :isolated-test
                     :identity (:identity b) :shaped-input (:shaped b)
                     :correspondence/status :unavailable
                     :reason :canonical-r6-score-and-posterior-proof-absent}})]
    (merge {:mode :isolated-test :e5-resolvers {:a a-config :b b-config}} authority)))
(defn- refusal-data [cfg]
  (try (e6a/verify-forward-influence cfg) nil
       (catch clojure.lang.ExceptionInfo e (ex-data e))))

(deftest distinct-arms-stop-at-missing-canonical-r6-correspondence
  (let [data (refusal-data (base-config))]
    (is (= :e6a/r6-scoring-correspondence-unavailable (:refusal data)))
    (is (true? (:changed? data)))
    (is (= [:run-e5 8 0] (:first-changed-occurrence data)))
    (is (= {:e2a :not-reached :e3 :not-reached :e2b :not-reached} (:downstream data)))))

(deftest asserted-or-borrowed-scoring-cannot-cross-boundary
  (testing "asserted proof label is not authority"
    (let [cfg (base-config)
          root (:authority-root cfg)
          path (.resolve (java.nio.file.Path/of root (make-array String 0)) "r6-scoring-a.edn")
          record (assoc (edn/read-string (slurp (str path)))
                        :correspondence/status :verified)
          bytes (.getBytes (pr-str record) StandardCharsets/UTF_8)]
      (Files/write path bytes (make-array java.nio.file.OpenOption 0))
      (is (= :e6a/r6-status-subject-mismatch
             (:refusal (refusal-data (assoc-in cfg [:sources :r6-scoring-a :sha256] (sha bytes))))))))
  (testing "other arm's shaped bytes are rejected"
    (let [cfg (base-config) root (:authority-root cfg)
          a-path (java.nio.file.Path/of root (into-array String ["r6-scoring-a.edn"]))
          b-record (edn/read-string (slurp (str root "/r6-scoring-b.edn")))
          bytes (.getBytes (pr-str (assoc b-record :schema/version
                                         :wm/e6a-r6-correspondence-status-v1)) StandardCharsets/UTF_8)]
      (Files/write a-path bytes (make-array java.nio.file.OpenOption 0))
      (is (= :e6a/r6-status-subject-mismatch
             (:refusal (refusal-data (assoc-in cfg [:sources :r6-scoring-a :sha256] (sha bytes)))))))))

(deftest fixed-context-and-pin-controls
  (is (= :e6a/source-set-incomplete
         (:refusal (refusal-data (update (base-config) :sources dissoc :fixed-context)))))
  (is (= :e6a/source-pin-mismatch
         (:refusal (refusal-data (assoc-in (base-config) [:sources :fixed-context :sha256]
                                           (apply str (repeat 64 "0")))))))
  (let [cfg (base-config)]
    (is (= :e6a/fixed-context-mismatch
           (:refusal (refusal-data (assoc-in cfg [:e5-resolvers :b]
                                             (arm-config :consolidation)))))))
  (is (= :e6a/production-authority-unavailable
         (:refusal (refusal-data {:mode :production})))))

(deftest no-change-is-retained-as-nonqualifying
  (let [rows [(last e5-fixture/candidates)]
        arm-records (fn [mode]
                      (-> (e5-fixture/records mode 3)
                          (assoc-in [:context :unshaped-candidates] rows)
                          (assoc-in [:unshaped :candidates] rows)
                          (assoc-in [:shaped :candidates] (shaped rows mode))))
        a-config (e5-fixture/config (arm-records :exploitation))
        b-config (e5-fixture/config (arm-records :exploration))
        a (e5/verify-shaping a-config) b (e5/verify-shaping b-config)
        authority (write-authority
                   {:fixed-context {:schema/version :wm/e6a-fixed-comparison-context-v1
                                    :scope :isolated-test :identity (:identity a)
                                    :unshaped rows :depth (:depth a) :arm/id-a :a :arm/id-b :b
                                    :slow/modes [:exploitation :exploration]}
                    :r6-scoring-a {:schema/version :wm/e6a-r6-correspondence-status-v1
                                   :scope :isolated-test :identity (:identity a)
                                   :shaped-input (:shaped a) :correspondence/status :unavailable
                                   :reason :canonical-r6-score-and-posterior-proof-absent}
                    :r6-scoring-b {:schema/version :wm/e6a-r6-correspondence-status-v1
                                   :scope :isolated-test :identity (:identity b)
                                   :shaped-input (:shaped b) :correspondence/status :unavailable
                                   :reason :canonical-r6-score-and-posterior-proof-absent}})
        out (e6a/verify-forward-influence
             (merge {:mode :isolated-test :e5-resolvers {:a a-config :b b-config}} authority))]
    (is (= :no-behavioral-influence (:qualification out)))
    (is (= :shaped-tables-identical (:reason out)))))
