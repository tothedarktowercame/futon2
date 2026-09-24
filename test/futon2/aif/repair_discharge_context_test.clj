(ns futon2.aif.repair-discharge-context-test
  "H-PUBLISH-A1 (E-cascade-real): the discharge context is mandatory at
  write time. Discovery: holes/labs/wm-contract/proof2/packets/
  H-PUBLISH-D.md — all 68 resolutions in data/wm-repair-obligations lack
  :repair/discharge-context, so repair-discharge-receipt/derive refuses
  every one :resolution-context-unavailable on every tick; nine were
  written AFTER the context schema by a context-free authorized writer.
  The store writers (resolve!, successor-resolution!,
  record-implementation!) must refuse :discharge-context-missing before
  writing, so an unpublishable record can never be written again.

  Tests 1-4 use a temp store root; the pin reads fixture copies of the
  class-B records (test/fixtures/repair-obligations/class-b), not data/."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.repair-obligation :as repair])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- temp-root []
  (str (Files/createTempDirectory "discharge-context-" (make-array FileAttribute 0))))

(defn- obligation [id]
  {:repair/id id
   :repair/status :open
   :repair/class :machine-failure
   :attempt-id "failed-attempt"
   :discharge-contract {:artifact-shape :code-commit}})

(defn- valid-context
  [phase id attempt-id review-job]
  {:schema :wm/repair-discharge-context-v1
   :phase phase :repair/id id
   :close {:attempt/id attempt-id}
   :review-job {:job-id review-job}})

(defn- implementation-value [id]
  {:attempt-id "repair-attempt" :commit "abc1234"
   :reviewer "reviewer" :review-job "review-2"
   :witness {:resolved? true :dial-moved? true}
   :repair/discharge-context
   (valid-context :implementation id "repair-attempt" "review-2")})

(defn- resolution-value [id]
  {:attempt-id "successor-attempt" :commit "abc1234"
   :reviewer "reviewer" :review-job "review-3"
   :witness {:resolved? true :dial-moved? true}
   :validation {:production-shaped? true}
   :repair/discharge-context
   (valid-context :successor-validation id "successor-attempt" "review-3")})

(def successor-sha (apply str (repeat 64 "a")))

(defn- successor-fixture [root id]
  {:obligation (assoc (obligation id)
                      :repair/implementation
                      {:implementation-attempt "repair-attempt"
                       :replacement-commit "abc1234"})
   :repair-close
   {:attempt/id "repair-attempt" :run/id "repair-run"
    :repair/id id :closed-at "2026-09-14T20:00:00Z"
    :commit "abc1234" :review-receipt-ids ["review-r.edn"]
    :review-sha256 successor-sha :grounded? true}
   :successor-close
   {:attempt/id "successor-attempt" :run/id "successor-run"
    :repair/id id :closed-at "2026-09-14T21:00:00Z"
    :witness-ref "successor-witness.edn" :witness-sha256 successor-sha
    :grounded? true :production-shaped? true
    :witness {:resolved? true :dial-moved? true}}
   :authority {:decided-by "reviewer" :review-job "review-3"}
   :resolution-read-fn (constantly nil)
   :resolve-fn (partial repair/resolve! root)})

(deftest resolve-without-context-refuses-and-writes-nothing
  (let [root (temp-root)
        id "repair-ctx-missing-resolve"
        obl (assoc (obligation id)
                   :repair/implementation
                   {:implementation-attempt "repair-attempt"})
        value (dissoc (resolution-value id) :repair/discharge-context)]
    (is (= :discharge-context-missing
           (try (repair/resolve! root obl value) nil
                (catch clojure.lang.ExceptionInfo e
                  (:repair-discharge/refusal (ex-data e))))))
    (is (= :successor-validation
           (try (repair/resolve! root obl value) nil
                (catch clojure.lang.ExceptionInfo e
                  (:phase (ex-data e))))))
    (is (= id (try (repair/resolve! root obl value) nil
                   (catch clojure.lang.ExceptionInfo e
                     (:repair/id (ex-data e))))))
    (is (not (.exists (io/file root "resolutions" (str id ".edn"))))
        "the refusal precedes the write")))

(deftest implementation-without-context-refuses-and-writes-nothing
  (let [root (temp-root)
        id "repair-ctx-missing-impl"
        value (dissoc (implementation-value id) :repair/discharge-context)]
    (is (= :discharge-context-missing
           (try (repair/record-implementation! root (obligation id) value) nil
                (catch clojure.lang.ExceptionInfo e
                  (:repair-discharge/refusal (ex-data e))))))
    (is (= :implementation
           (try (repair/record-implementation! root (obligation id) value) nil
                (catch clojure.lang.ExceptionInfo e
                  (:phase (ex-data e))))))
    (is (not (.exists (io/file root "implementations" (str id ".edn"))))
        "the refusal precedes the write")))

(deftest successor-resolution-without-context-refuses-and-writes-nothing
  (let [root (temp-root)
        id "repair-ctx-missing-successor"
        fixture (successor-fixture root id)]
    (is (= :discharge-context-missing
           (try (repair/successor-resolution! fixture) nil
                (catch clojure.lang.ExceptionInfo e
                  (:repair-discharge/refusal (ex-data e))))))
    (is (= :successor-validation
           (try (repair/successor-resolution! fixture) nil
                (catch clojure.lang.ExceptionInfo e
                  (:phase (ex-data e))))))
    (is (not (.exists (io/file root "resolutions" (str id ".edn"))))
        "the refusal precedes the write")))

(deftest valid-context-writes-and-is-carried
  (let [root (temp-root)
        id "repair-ctx-valid"
        impl (repair/record-implementation! root (obligation id)
                                          (implementation-value id))]
    (is (= :implementation (:repair/phase impl)))
    (is (= (valid-context :implementation id "repair-attempt" "review-2")
           (:repair/discharge-context impl)))
    (let [resolved (repair/resolve!
                    root (assoc (obligation id)
                                :repair/implementation impl)
                    (resolution-value id))]
      (is (= :successor-validation (:repair/phase resolved)))
      (is (= (valid-context :successor-validation id "successor-attempt" "review-3")
             (:repair/discharge-context resolved)))
      (is (= :resolved (:repair/status resolved))))))

(deftest successor-resolution-with-context-writes-and-is-carried
  (let [root (temp-root)
        id "repair-ctx-valid-successor"
        context (valid-context :successor-validation id "successor-attempt" "review-3")
        result (repair/successor-resolution!
                (assoc (successor-fixture root id)
                       :discharge-context context))]
    (is (= :resolved (:status result)))
    (is (= context
           (:repair/discharge-context
            (edn/read-string
             (slurp (io/file root "resolutions" (str id ".edn")))))))))

(deftest present-but-invalid-context-still-refuses
  (testing "present but not binding the transition is not a bypass"
    (let [root (temp-root)
          id "repair-ctx-invalid"
          bad (assoc (implementation-value id)
                     :repair/discharge-context
                     ;; Wrong phase: an implementation write carrying a
                     ;; successor-validation context fails the validator.
                     (valid-context :successor-validation id
                                    "repair-attempt" "review-2"))]
      (is (thrown? clojure.lang.ExceptionInfo
                   (repair/record-implementation! root (obligation id) bad)))
      (is (not (= :discharge-context-missing
                  (try (repair/record-implementation! root (obligation id) bad) nil
                       (catch clojure.lang.ExceptionInfo e
                         (:repair-discharge/refusal (ex-data e))))))
          "the validator's own refusal, not the missing-context one")
      (is (not (.exists (io/file root "implementations" (str id ".edn"))))))))

(def class-b-ids
  "The nine H-PUBLISH-D class-B ids: resolutions written 2026-09-24T04:04Z
  by runs/stop-line-discharge-2026-09-24/discharge.clj through context-free
  callers — the writes this change would have refused
  :discharge-context-missing."
  ["repair-attempt-051-feature-card-missing-or-invalid"
   "repair-attempt-052-strategic-selection-unavailable"
   "repair-ea1-3f4cac241e58afd9b6eae48e78a2ac7f63925aa3fc05c7e3a3fd6d789d4637a9--attempt-001-untyped-failure"
   "repair-ea1-3f4cac241e58afd9b6eae48e78a2ac7f63925aa3fc05c7e3a3fd6d789d4637a9--attempt-003-artifact-binding-mismatch"
   "repair-ea1-504ad8630070adf67604aab739c0c0184b5ccf632f552ec6d2b55fac7ad71c87--attempt-001-feature-card-missing-or-invalid"
   "repair-ea1-7093b8fbb1ca8fc99a18899b69ca39f1e5a1a129e76739ebddbbee974299f94f--attempt-001-untyped-failure"
   "repair-ea1-b0eeafa0e59b4dc0dd9e0abe1cbbed0e687c5827b3ade8320c98c7f82a79032b--attempt-001-machine-repair-lacks-grounded-review-evidence"
   "repair-initialization-6d5da36a-04f0-42ee-ba91-55bee5801a01-initialization-failed"
   "repair-initialization-a9177cab-e783-464e-83e2-21a6371484a4-initialization-failed"])

(deftest pin-class-b-records-lack-the-context-today
  ;; The A1 change refuses future context-free writes; the legacy records
  ;; themselves are A2's disposition decision and are deliberately untouched.
  ;; Reads verbatim fixture copies (see the README there), so a pinned
  ;; worktree, which has no data/, can run this.
  (doseq [id class-b-ids]
    (let [f (io/file "test/fixtures/repair-obligations/class-b" (str id ".edn"))]
      (is (.isFile f) (str id " exists on record"))
      (is (nil? (:repair/discharge-context (edn/read-string (slurp f))))
          (str id " lacks :repair/discharge-context today")))))
