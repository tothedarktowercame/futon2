(ns futon2.aif.effective-run-history-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.effective-run-history :as history]
            [futon2.aif.meta-terminal-injury :as terminal-injury]
            [futon2.aif.outer-refusal-history :as outer-history]
            [futon2.aif.wm.authored.req-f0ba279a :as retained-checks])
  (:import [java.math BigInteger]
           [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]
           [java.security MessageDigest]))

(def prior-path
  "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-29-b1a049d7-d4f8-4f63-be69-6872d86e8827.edn")

(def incomplete-path
  "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-29-9e2103c1-c48f-496f-b8d5-2421fb7f68f0.edn")

(def m-typed-memories-record-path
  "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-31-4955b6e6-b818-42ea-b00e-7354a71d88e2.edn")

(def m-typed-memories-run-id "2026-10-31-4955b6e6-b818-42ea-b00e-7354a71d88e2")

(def canonical-index-path
  "/home/joe/code/futon2/data/wm-runs/outer-refusal-index.ednl")

(defn- retained-row [run-id]
  (->> (line-seq (io/reader canonical-index-path))
       (map #(try (edn/read-string %) (catch Exception _ nil)))
       (filter #(= run-id (:run-id %)))
       first))

(defn- temp-index-dir
  "Write ROWS as a private refusal index and return its directory, so no
  canonical store is touched while compare-index reads exactly what the
  serving path reads."
  [rows]
  (let [dir (Files/createTempDirectory "effective-run-history"
                                       (make-array FileAttribute 0))]
    (spit (io/file (.toString dir) "outer-refusal-index.ednl")
          (apply str (map #(str (pr-str %) "\n") rows)))
    (.toString dir)))

(defn- sha256 [x]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (.update digest (.getBytes (pr-str x) "UTF-8"))
    (format "%064x" (BigInteger. 1 (.digest digest)))))

(def ordered-checks
  (->> retained-checks/clauses
       (map (fn [[occurrence clause]] [occurrence (:completion-check clause)]))
       (sort-by pr-str)
       vec))

(defn- effective-identity [record]
  {:schema :wm/effective-run-identity-v1
   :target (get-in record [:selection-event :target])
   :giraffe-manifest-sha256
   (get-in record [:outer/selection :giraffe/manifest-sha256])
   :policy-id (get-in record [:selection-event :policy-key 2])
   :compiled-check-set-sha256 (sha256 ordered-checks)
   :mission-source-sha256
   "e1b01eb3e0c1cef716174978e63a6b9ac100e968032bb59adbdf31929d805cf0"
   :base-head "cfb1408629826c6d825e2ddc06aad84ee5ac6108"})

(defn- row [record id failure-kind finished-work?]
  {:schema :wm/outer-refusal-index-entry-v1 :status :indexed
   :run-id (:run/id record) :record-path prior-path
   :record-sha256 (apply str (repeat 64 "a"))
   :failure {:kind failure-kind}
   :finished-work? finished-work?
   :effective-run-identity id})

(deftest index-rows-reads-what-the-guard-reads
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "effective-run-index"
                      (make-array java.nio.file.attribute.FileAttribute 0)))
        row (fn [target finished-work? run-id]
              {:schema :wm/outer-refusal-index-entry-v1 :status :indexed
               :run-id run-id :target target :finished-work? finished-work?
               :effective-run-identity
               {:schema :wm/effective-run-identity-v1 :target target
                :giraffe-manifest-sha256 "manifest-1" :policy-id "p"
                :compiled-check-set-sha256 "c" :mission-source-sha256 "m"
                :base-head "h"}})
        rows [(row "M-a" true "run-1") (row "M-a" false "run-2")
              (row "M-b" false "run-3")]
        write-row (fn [r] (spit (io/file dir "outer-refusal-index.ednl")
                                (str (pr-str r) "\n") :append true))]
    (is (nil? (history/index-rows (io/file dir "absent"))))
    (doseq [r rows] (write-row r))
    (is (= rows (history/index-rows dir)))
    ;; blocking-rows-for-target keeps the guard's own rule: any blocking row
    ;; blocks, and a newer non-blocking row does not unblock the target.
    (is (= ["run-1"] (mapv :run-id (history/blocking-rows-for-target rows "M-a"))))
    (is (= [] (history/blocking-rows-for-target rows "M-b")))
    (is (= [] (history/blocking-rows-for-target rows "M-untargeted")))))

(defn- measured [row]
  (assoc row :satisfaction {:schema :wm/satisfaction-evidence-v1
                            :evidence :claim-mode-observation
                            :wants-measured true}))

(deftest retained-qualified-close-blocks-equal-next-run
  (let [record (edn/read-string (slurp prior-path))
        id (effective-identity record)
        comparison (history/compare-rows
                    id [(measured (row record id :already-satisfied true))])
        refusal (try (history/require-novel! comparison) nil
                     (catch clojure.lang.ExceptionInfo e e))]
    (is (= :duplicate-effective-run (:status comparison)))
    (is (= (:run/id record) (get-in comparison [:matched-run :run-id])))
    (is (= :outer/duplicate-effective-run (:failure-kind (ex-data refusal))))
    (is (= :effective-run-comparison (:failure-stage (ex-data refusal))))))

(deftest every-identity-component-is-material
  (let [record (edn/read-string (slurp prior-path))
        id (effective-identity record)
        prior (measured (row record id :already-satisfied true))]
    (doseq [k history/required-keys]
      (testing (name k)
        (is (= :no-duplicate
               (:status (history/compare-rows (update id k str "-changed") [prior]))))))
    (is (= :no-duplicate
           (:status (history/compare-rows (assoc id :base-head "changed") [prior]))))))

(deftest irrelevant-world-changes-do-not-authorize-a-repeat
  (let [record (edn/read-string (slurp prior-path))
        id (effective-identity record)
        prior (assoc (measured (row record id :already-satisfied true))
                     :seat-roster :changed :open-tasks :changed :cascade-scores [99])]
    (is (= :duplicate-effective-run
           (:status (history/compare-rows id [prior]))))))

(deftest missing-identity-is-incomparable
  (let [record (edn/read-string (slurp prior-path))
        id (dissoc (effective-identity record) :mission-source-sha256)]
    (is (= :incomparable (:status (history/compare-rows id []))))
    (is (= [:mission-source-sha256]
           (:missing (history/compare-rows id []))))))

(deftest non-finished-priors-do-not-block
  (let [record (edn/read-string (slurp prior-path))
        id (effective-identity record)]
    (doseq [kind [:agent-unavailable :incomplete :guardrail-refusal
                  :already-satisfied]]
      (is (= :no-duplicate
             (:status (history/compare-rows id [(row record id kind false)])))))))

(deftest changed-prior-blocks
  (let [record (edn/read-string (slurp prior-path))
        id (effective-identity record)]
    (is (= :duplicate-effective-run
           (:status (history/compare-rows id [(row record id nil true)]))))))

(deftest retained-unmeasured-already-satisfied-row-stops-blocking-on-read
  "The retained 4955b6e6 row cannot be rewritten: it recorded
  :finished-work? true for a producer-qualified already-satisfied close
  whose selected wants were never measured (mission-observation absent).
  Reading that exact row, compare-index must not report a duplicate for
  the M-typed-memories identity."
  (let [retained (retained-row m-typed-memories-run-id)
        identity (:effective-run-identity retained)]
    (is (some? retained))
    (is (true? (:finished-work? retained)))
    (is (= :already-satisfied (get-in retained [:failure :kind])))
    (is (not (contains? retained :satisfaction)))
    (is (= :no-duplicate
           (:status (history/compare-index (temp-index-dir [retained]) identity))))))

(deftest measured-already-satisfied-row-still-blocks
  (let [record (edn/read-string (slurp prior-path))
        id (effective-identity record)]
    (is (= :duplicate-effective-run
           (:status (history/compare-rows
                     id [(measured (row record id :already-satisfied true))]))))))

(deftest recorded-unmeasured-satisfaction-does-not-block
  (let [record (edn/read-string (slurp prior-path))
        id (effective-identity record)
        unmeasured (assoc (row record id :already-satisfied true)
                          :satisfaction {:schema :wm/satisfaction-evidence-v1
                                         :evidence :declaration-presence
                                         :wants-measured false})]
    (is (= :no-duplicate
           (:status (history/compare-rows id [unmeasured]))))))

(deftest existing-terminal-semantics-decide-finished-work
  (let [qualified (edn/read-string (slurp prior-path))
        measured-qualified (assoc-in qualified
                                     [:failure :detail :mission-observation]
                                     {:status :admitted})
        incomplete (edn/read-string (slurp incomplete-path))
        m-typed-memories (edn/read-string (slurp m-typed-memories-record-path))]
    ;; The retained b1a049d7 close retained no mission-observation: its
    ;; producer-qualified already-satisfied was declaration presence, so
    ;; under measured semantics it is not finished work.
    (is (false? (terminal-injury/finished-work? qualified)))
    (is (false? (:finished-work?
                 (outer-history/index-entry qualified prior-path (apply str (repeat 64 "a"))))))
    (is (= {:schema :wm/satisfaction-evidence-v1
            :evidence :declaration-presence
            :wants-measured false
            :observation-status nil}
           (:satisfaction
            (outer-history/index-entry qualified prior-path (apply str (repeat 64 "a"))))))
    ;; The same close with an admitted retained measurement is finished work.
    (is (true? (terminal-injury/finished-work? measured-qualified)))
    (is (= {:evidence :claim-mode-observation :wants-measured true :observation-status :admitted}
           (dissoc (terminal-injury/satisfaction-evidence measured-qualified) :schema)))
    ;; The retained 4955b6e6 close is likewise unmeasured.
    (is (false? (terminal-injury/finished-work? m-typed-memories)))
    (is (false? (:wants-measured
                 (:satisfaction
                  (outer-history/index-entry m-typed-memories
                                            m-typed-memories-record-path
                                            (apply str (repeat 64 "c")))))))
    (is (false? (terminal-injury/finished-work? incomplete)))
    (is (false? (:finished-work?
                 (outer-history/index-entry incomplete incomplete-path
                                            (apply str (repeat 64 "b"))))))
    (is (true? (terminal-injury/finished-work?
                {:terminal-receipt {:outcome :grounded-change}})))
    (is (true? (:wants-measured
                (terminal-injury/satisfaction-evidence
                 {:terminal-receipt {:outcome :grounded-change}}))))
    (is (false? (terminal-injury/finished-work?
                 {:terminal-receipt {:outcome :already-satisfied}})))))

(deftest non-mission-target-without-wants-measures-as-satisfied
  (let [qualified (edn/read-string (slurp prior-path))
        non-mission (-> qualified
                        (assoc-in [:failure :target] "T-terminal-want")
                        (assoc-in [:failure :detail :target] "T-terminal-want"))]
    (is (true? (terminal-injury/finished-work? non-mission)))
    (is (true? (:wants-measured
                (terminal-injury/satisfaction-evidence non-mission))))))

(deftest an-already-satisfied-close-with-no-target-is-not-measured
  (let [record {:failure {:kind :already-satisfied :detail {}}
                :outcome :already-satisfied}]
    (is (false? (:wants-measured (terminal-injury/satisfaction-evidence record))))))
