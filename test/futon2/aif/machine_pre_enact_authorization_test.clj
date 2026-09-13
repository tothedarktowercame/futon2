(ns futon2.aif.machine-pre-enact-authorization-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.machine-pre-enact-authorization :as e3]
            [futon2.aif.machine-budget-authority :as authority]
            [futon2.aif.r9-checker :as r9])
  (:import (java.nio.charset StandardCharsets)
           (java.nio.file Files)
           (java.security MessageDigest)))

(def e1-root "holes/labs/wm-contract/runs/row-22-e1-authority-resolution-2026-09-13/fixtures")
(def e1-files {:ranked-support "ranked-support.edn" :field-membership "field-membership.edn"
               :costs "costs.edn" :utilities "utilities.edn" :budgets "budgets.edn"})
(def ids {:model/id :wm-e1-fixture :model/revision "model-v3" :run/id "e1-authority-run"
          :cohort/id "cohort-1" :tick/index 4 :event/id "pending-7"})
(def subject {:candidate/occurrence-id [:e1-authority-run 4 0]
              :action {:type :advance-mission :target "M-alpha"}
              :construction {:policy/id "p2" :missions ["m1"]}
              :field-pins []
              :producer/id "codex-22" :claim/id "claim-e3"
              :artifact/ref "artifact-e3" :trace/id "trace-producer"})
(def pending (merge {:schema/version :wm/e3-pending-construction-v1 :scope :isolated-test
                     :phase :pending-pre-enact :authorization-at "2026-09-13T12:00:00Z"
                     :subject subject} ids))
(defn- sha [bytes]
  (apply str (map #(format "%02x" (bit-and 0xff %))
                  (.digest (doto (MessageDigest/getInstance "SHA-256") (.update bytes))))))
(defn- file-sha [path] (sha (Files/readAllBytes (.toPath (io/file path)))))
(def e1-config
  {:resolver/version authority/resolver-version :mode :isolated-test :root e1-root
   :sources (into {} (map (fn [[label filename]]
                            [label {:relative-path filename
                                    :sha256 (file-sha (io/file e1-root filename))}]) e1-files))})
(alter-var-root #'subject assoc :field-pins
                (mapv (fn [[label filename]] {:label label :sha256 (file-sha (io/file e1-root filename))})
                      e1-files))
(alter-var-root #'pending assoc :subject subject)
(def checker-sha (apply str (repeat 64 "c")))
(def verification {:path "isolated-verification.edn" :sha256 (apply str (repeat 64 "d"))})
(def commission {:agent-id "claude-15" :prompt "review exact E3 subject"
                 :caller "fixture-owner" :surface "isolated-test"})
(def r9-input
  {:role-binding {:author "codex-22" :reviewer "claude-15"}
   :producer-job {:job-id "producer-job" :agent-id "codex-22"
                  :artifact-ref "artifact-e3" :trace-id "trace-producer"}
   :reviewer-job {:job-id "review-job" :agent-id "claude-15"
                  :request-digest (r9/request-digest commission)
                  :trace-id "trace-review" :finished-at "2026-09-13T11:59:00Z"
                  :execution {:executed true :tool-events 1}}
   :subject {:boundary :e3/pre-enact :artifact-ref "artifact-e3"
             :digest (sha (.getBytes (pr-str (select-keys pending
                                                          [:model/id :model/revision :run/id :cohort/id
                                                           :tick/index :event/id :phase :authorization-at :subject]))
                                    StandardCharsets/UTF_8))}
   :review-commission commission :verification-receipt verification
   :review-receipt {:reviewer "claude-15" :verification verification}
   :trace->job {"trace-producer" "producer-job" "trace-review" "review-job"}
   :checker-source-sha256 checker-sha
   :bootstrap-anchor {:schema :wm/r9-bootstrap-anchor-v1 :status :anchored
                      :authority "isolated-fixture-only" :checker-source-sha256 checker-sha}
   :admission-at "2026-09-13T11:59:00Z"
   :ledger-source {:scope :isolated-fixture}})
(def canonical-admission (r9/check-independence r9-input))
(def verdict (merge {:schema/version :wm/e3-independence-verdict-v1 :scope :isolated-test
                     :reviewer/id "claude-15" :review-trace/id "trace-review"
                     :canonical-admission canonical-admission :subject subject} ids))
(def review (merge {:schema/version :wm/e3-independent-review-v1 :scope :isolated-test
                    :reviewer/id "claude-15" :claim/id "claim-e3"
                    :artifact/ref "artifact-e3" :producer-trace/id "trace-producer"
                    :review-trace/id "trace-review" :authority/ref "isolated-fixture-authority"
                    :completed-at "2026-09-13T11:59:00Z" :subject subject :r9/input r9-input} ids))
(defn- config [records]
  (let [root (Files/createTempDirectory "e3-" (make-array java.nio.file.attribute.FileAttribute 0))]
    {:mode :isolated-test :evidence-root (str root) :e2a-resolver e1-config
     :evidence (into {} (for [[label record] records
                             :let [bytes (.getBytes (pr-str record) StandardCharsets/UTF_8)
                                   name (str (name label) ".edn")]]
                         (do (Files/write (.resolve root name) bytes (make-array java.nio.file.OpenOption 0))
                             [label {:relative-path name :sha256 (sha bytes)}])))}))
(defn- refusal [cfg]
  (try (e3/verify-pre-enact cfg) nil
       (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))

(deftest exact-pending-subject-is-mechanically-authorized
  (let [out (e3/verify-pre-enact (config {:pending pending :verdict verdict :review review}))]
    (is (= :mechanism-authorized (:decision out)))
    (is (= subject (:subject out)))
    (is (= :unavailable (get-in out [:external-dependencies :production-authority])))
    (is (= :required (get-in out [:external-dependencies :r6-scoring-and-posterior])))))

(deftest closed-controls
  (testing "missing, self and unknown verdicts"
    (is (= :e3/evidence-set-incomplete (refusal (config {:pending pending :review review}))))
    (is (= :e3/canonical-r9-admission-mismatch
           (refusal (config {:pending pending :review review
                             :verdict (assoc verdict :canonical-admission {:decision :self})}))))
    (is (= :e3/canonical-r9-admission-mismatch
           (refusal (config {:pending pending :review review
                             :verdict (assoc verdict :canonical-admission {:decision :unknown})})))))
  (testing "borrowed candidate, producer, trace, stale field and context"
    (doseq [changed [(assoc subject :candidate/occurrence-id "occ-other")
                     (assoc subject :producer/id "other-producer")
                     (assoc subject :trace/id "other-trace")
                     (assoc subject :field-pins [{:label :ranked :sha256 (apply str (repeat 64 "b"))}])]]
      (is (= :e3/subject-mismatch
             (refusal (config {:pending pending :review review :verdict (assoc verdict :subject changed)})))))
    (is (= :e3/context-mismatch
           (refusal (config {:pending pending :review review
                             :verdict (assoc verdict :event/id "borrowed-event")})))))
  (testing "review and temporal controls"
    (let [self-input (-> r9-input
                         (assoc-in [:role-binding :reviewer] "codex-22")
                         (assoc-in [:reviewer-job :agent-id] "codex-22"))]
      (is (= :r9/author-equals-reviewer
             (refusal (config {:pending pending
                               :verdict (assoc verdict :reviewer/id "codex-22")
                               :review (assoc review :reviewer/id "codex-22" :r9/input self-input)})))))
    (is (= :e3/canonical-r9-provenance-mismatch
           (refusal (config {:pending pending :verdict verdict
                             :review (assoc review :review-trace/id "wrong")}))))
    (is (= :e3/canonical-r9-time-mismatch
           (refusal (config {:pending pending :verdict verdict
                             :review (assoc review :completed-at "2026-09-13T12:01:00Z")}))))
    (is (= :e3/not-pending-pre-enact
           (refusal (config {:pending (assoc pending :phase :enacted)
                             :verdict verdict :review review})))))
  (testing "canonical chronology and complete event subject"
    (let [late (-> r9-input
                   (assoc-in [:reviewer-job :finished-at] "2026-09-13T12:30:00Z")
                   (assoc :admission-at "2026-09-13T12:30:00Z"))]
      (is (= :e3/canonical-r9-time-mismatch
             (refusal (config {:pending pending
                               :verdict (assoc verdict :canonical-admission
                                               (r9/check-independence late))
                               :review (assoc review :completed-at "2026-09-13T12:30:00Z"
                                             :r9/input late)})))))
    (let [changed (fn [record] (assoc record :run/id "borrowed-run" :event/id "borrowed-event"))]
      (is (= :e3/e2a-identity-mismatch
             (refusal (config {:pending (changed pending) :verdict (changed verdict)
                               :review (changed review)}))))))
  (testing "same action at another occurrence is not interchangeable"
    (is (= :e3/occurrence-not-approved
           (refusal (config {:pending (assoc-in pending [:subject :candidate/occurrence-id]
                                               [:e1-authority-run 4 1])
                             :verdict verdict :review review})))))
  (is (= :e3/production-authority-unavailable
         (refusal {:mode :production}))))

(deftest malformed-and-laundered-subjects-refuse
  (doseq [bad [(assoc subject :candidate/occurrence-id nil)
               (assoc subject :action {}) (assoc subject :construction {})
               (assoc subject :claim/id "") (assoc subject :artifact/ref nil)
               (assoc subject :trace/id "") (assoc subject :field-pins [])
               (assoc subject :field-pins [{:label :ranked :sha256 "bad"}])]]
    (is (= :e3/pending-context-invalid
           (refusal (config {:pending (assoc pending :subject bad)
                             :verdict (assoc verdict :subject bad)
                             :review (assoc review :subject bad)})))))
  (is (= :e3/scope-laundering
         (refusal (config {:pending pending :verdict (assoc verdict :scope :production)
                           :review review}))))
  (is (= :e3/scope-laundering
         (refusal (config {:pending pending :verdict verdict
                           :review (assoc review :scope :production)}))))
  (is (= :r9/review-execution-missing
         (refusal (config {:pending pending :verdict verdict
                           :review (assoc-in review [:r9/input :reviewer-job :execution]
                                             {:executed false :tool-events 0})})))))
