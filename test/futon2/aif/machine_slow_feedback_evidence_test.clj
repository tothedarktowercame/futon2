(ns futon2.aif.machine-slow-feedback-evidence-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [futon2.aif.machine-slow-feedback-evidence :as e6b]
            [futon2.aif.machine-enactment-correspondence :as canonical-e2b-verifier]
            [futon2.aif.machine-pre-enact-authorization :as canonical-e3-verifier]
            [futon2.aif.machine-enactment-correspondence-test :as e2bt]
            [futon2.aif.machine-pre-enact-authorization-test :as e3t]
            [futon2.aif.temporal-hierarchy :as hierarchy])
  (:import [java.nio.file Files]
           [java.security MessageDigest]))

(def common {:scope :isolated-test :model/id :wm-e1-fixture :model/revision "model-v3"
             :run/id "e1-authority-run" :tick/index 4})
(defn- digest [^bytes bs]
  (apply str (map #(format "%02x" (bit-and 0xff %))
                  (.digest (doto (MessageDigest/getInstance "SHA-256") (.update bs))))))
(defn- vd [x] (digest (.getBytes (pr-str x) "UTF-8")))
(def context
  (merge common {:schema/version :wm/e6b-transition-context-v1
                 :destination/tick-index 5 :prior-state/revision "slow-4"
                 :next-state/revision "slow-5" :feedback/event-id "feedback-1"
                 :application/id "apply-1" :candidate/occurrence-id [:e1-authority-run 4 0]
                 :action {:type :advance-mission :target "M-alpha"}
                 :fast/action-class :advance-capability
                 :outcome-reviewer/id "reviewer-1" :outcome-observer/id "observer-1"
                 :destination/as-of "2026-09-13T05:00:00Z"}))
(def prior-entry {:alpha 2.0 :beta 2.0 :intrinsic-value 0.5
                  :n-emissions 2 :n-followthrough 1 :as-of "2026-09-13T04:00:00Z"})
(def prior
  (merge common {:schema/version :wm/e6b-prior-slow-state-v1 :state/revision "slow-4"
                 :slow/mode :exploitation
                 :slow/intrinsics {:advance-capability prior-entry :explore prior-entry}}))
(def canonical-config
  {:e3 (#'e3t/config {:pending e3t/pending :verdict e3t/verdict :review e3t/review})
   :e2b (#'e2bt/config)})
(def canonical-e3 (canonical-e3-verifier/verify-pre-enact (:e3 canonical-config)))
(def canonical-e2b (canonical-e2b-verifier/verify-correspondence (:e2b canonical-config)))
(alter-var-root
 #'context assoc
 :canonical/e3-context
 (select-keys (:identity canonical-e3)
              [:model/id :model/revision :run/id :cohort/id :tick/index :event/id])
 :canonical/e2b-context
 (merge (:identity canonical-e2b) (select-keys canonical-e2b [:cohort/id :event/id]))
 :canonical/field-subject
 {:e3/field-pins (get-in canonical-e3 [:subject :field-pins])
  :e2b/e1-source-pins (get-in canonical-e2b [:subject :e1-source-pins])
  :e2b/approved-domain (get-in canonical-e2b [:subject :approved-domain])})
(def e2b
  (merge common {:schema/version :wm/e6b-e2b-subject-v1
                 :candidate/occurrence-id [:e1-authority-run 4 0]
                 :action {:type :advance-mission :target "M-alpha"}
                 :fast/action-class :advance-capability
                 :canonical/e3-digest (vd canonical-e3)
                 :canonical/e2b-digest (vd canonical-e2b)}))
(def outcome
  (merge common {:schema/version :wm/e6b-outcome-authority-v1
                 :candidate/occurrence-id [:e1-authority-run 4 0]
                 :action {:type :advance-mission :target "M-alpha"}
                 :terminal/status :succeeded :fast/action-class :advance-capability
                 :terminal/at "2026-09-13T04:30:00Z"
                 :fast/witnessed? true :fast/succeeded? true
                 :outcome/evidence-id "outcome-1" :outcome/authority-ref "review-1"
                 :outcome/producer-id "worker-1" :outcome/reviewer-id "reviewer-1"}))

(defn- base-records []
  (let [state (hierarchy/advance-slow-state
               (select-keys prior [:slow/mode :slow/intrinsics])
               (select-keys outcome [:fast/action-class :fast/witnessed? :fast/succeeded?])
               {:as-of (:destination/as-of context) :run-id (:run/id context)
                :evidence-ref (:outcome/evidence-id outcome)})
        next (merge (select-keys common [:scope :model/id :model/revision :run/id])
                    {:schema/version :wm/e6b-next-slow-state-v1 :tick/index 5
                     :state/revision "slow-5" :predecessor/revision "slow-4"
                     :feedback/event-id "feedback-1" :state state})
        entry {:application/id "apply-1" :status :committed
               :feedback/event-id "feedback-1" :prior-state/revision "slow-4"
               :input/digests {:context (vd context) :prior (vd prior)
                               :e2b (vd e2b) :outcome (vd outcome)}
               :output/digest (vd next)}
        outcome-subject (select-keys outcome
                                     [:model/id :model/revision :run/id :tick/index
                                      :candidate/occurrence-id :action :fast/action-class
                                      :terminal/status :fast/witnessed? :fast/succeeded?
                                      :outcome/evidence-id :outcome/producer-id])
        transition-subject {:model/id (:model/id context) :model/revision (:model/revision context)
                            :run/id (:run/id context) :source/tick-index 4 :destination/tick-index 5
                            :candidate/occurrence-id (:candidate/occurrence-id context)
                            :action (:action context) :fast/action-class :advance-capability
                            :prior-state/revision "slow-4" :next-state/revision "slow-5"
                            :feedback/event-id "feedback-1"}]
    {:context context :prior-state prior :e2b-subject e2b :outcome outcome :next-state next
     :outcome-review {:schema/version :wm/e6b-outcome-review-v1 :scope :isolated-test
                      :review/outcome :accepted :subject outcome-subject
                      :reviewer/id "reviewer-1" :review/id "review-1"
                      :observer/id "observer-1" :reviewed-at "2026-09-13T04:45:00Z"
                      :review/artifact-sha256 nil}
     :outcome-review-artifact
     {:schema/version :wm/e6b-outcome-review-artifact-v1 :scope :isolated-test
      :subject outcome-subject :review/id "review-1" :reviewer/id "reviewer-1"
      :observer/id "observer-1" :reviewed-at "2026-09-13T04:45:00Z" :executed? true}
     :application-ledger {:schema/version :wm/e6b-application-ledger-v1 :scope :isolated-test
                          :entries [entry]}
     :application-universe {:schema/version :wm/e6b-application-universe-v1
                            :scope :isolated-test :transition/subject transition-subject
                            :transition/subject-digest (vd transition-subject)
                            :complete/application-ids ["apply-1"]
                            :authority/status :independently-configured-complete
                            :authority/owner "fixture-authority"}}))

(defn- fixture [f]
  (let [dir (.toFile (Files/createTempDirectory "e6b" (make-array java.nio.file.attribute.FileAttribute 0)))
        records0 (f (base-records))
        ledger-bytes (.getBytes (str (pr-str (:application-ledger records0)) "\n") "UTF-8")
        artifact-bytes (.getBytes (str (pr-str (:outcome-review-artifact records0)) "\n") "UTF-8")
        records (-> records0
                    (assoc-in [:application-universe :ledger/sha256] (digest ledger-bytes))
                    (update-in [:outcome-review :review/artifact-sha256]
                               #(or % (digest artifact-bytes))))
        sources (into {}
                      (for [label e6b/source-order
                            :let [file (io/file dir (str (name label) ".edn"))
                                  bytes (.getBytes (str (pr-str (records label)) "\n") "UTF-8")]]
                        (do (Files/write (.toPath file) bytes (make-array java.nio.file.OpenOption 0))
                            [label {:relative-path (.getName file) :sha256 (digest bytes)}])))]
    {:mode :isolated-test :evidence-root (.getPath dir) :sources sources
     :canonical canonical-config}))
(defn- refusal [cfg]
  (try (e6b/verify-feedback cfg) nil
       (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))

(deftest replays-exact-feedback-without-storage-claim
  (let [out (e6b/verify-feedback (fixture identity))]
    (is (:replay/identical? out))
    (is (= 3.0 (get-in out [:state :slow/intrinsics :advance-capability :alpha])))
    (is (= 2.0 (get-in out [:state :slow/intrinsics :advance-capability :beta])))
    (is (= prior-entry (get-in out [:state :slow/intrinsics :explore])))
    (is (false? (:storage-enforcement? out)))
    (is (false? (:production-edge-fired? out)))
    (is (= (set e6b/source-order) (set (map :label (:sources out)))))))

(deftest outcome-and-prior-refusals
  (doseq [[label f expected]
          [[:missing-success #(update % :outcome dissoc :fast/succeeded?) :e6b/outcome-authority-invalid]
           [:nonterminal #(assoc-in % [:outcome :terminal/status] :pending) :e6b/outcome-authority-invalid]
           [:unwitnessed #(assoc-in % [:outcome :fast/witnessed?] false) :e6b/outcome-authority-invalid]
           [:missing-class #(update-in % [:prior-state :slow/intrinsics] dissoc :advance-capability)
            :e6b/prior-state-incomplete-or-stale]
           [:stale-prior #(assoc-in % [:prior-state :state/revision] "slow-3")
            :e6b/prior-state-incomplete-or-stale]
           [:cross-occurrence #(assoc-in % [:outcome :candidate/occurrence-id] "other")
            :e6b/outcome-authority-invalid]]]
    (testing (name label) (is (= expected (refusal (fixture f)))))))

(deftest ledger-and-transition-refusals
  (is (= :e6b/feedback-not-applied
         (refusal (fixture #(assoc-in % [:application-ledger :entries] [])))))
  (is (= :e6b/duplicate-feedback
         (refusal (fixture #(update-in % [:application-ledger :entries]
                                      (fn [xs] (conj xs (first xs))))))))
  (is (= :e6b/feedback-conflict
         (refusal (fixture #(assoc-in % [:application-ledger :entries 0 :output/digest]
                                      (apply str (repeat 64 "0")))))))
  (is (= :e6b/application-universe-incomplete
         (refusal (fixture #(assoc-in % [:application-universe :complete/application-ids] [])))))
  (is (= :e6b/transition-context-invalid
         (refusal (fixture #(assoc-in % [:context :destination/tick-index] 7)))))
  (is (= :e6b/canonical-context-mismatch
         (refusal (fixture #(assoc-in % [:e2b-subject :canonical/e3-digest]
                                      (apply str (repeat 64 "0")))))))
  (is (= :e6b/outcome-review-unresolved
         (refusal (fixture #(assoc-in % [:outcome-review :review/id] "nonexistent-review")))))
  (is (= :e6b/outcome-authority-invalid
         (refusal (fixture #(assoc-in % [:outcome :fast/action-class] :explore))))))

(deftest canonical-context-and-review-chronology-controls
  (doseq [[label path value]
          [[:model [:context :model/revision] "borrowed-model"]
           [:run [:context :run/id] "borrowed-run"]
           [:tick [:context :tick/index] 3]
           [:event [:context :canonical/e3-context :event/id] "borrowed-event"]
           [:field [:context :canonical/field-subject :e3/field-pins] []]]]
    (testing (name label)
      (is (#{:e6b/canonical-context-mismatch :e6b/transition-context-invalid}
           (refusal (fixture #(assoc-in % path value)))))))
  (testing "missing canonical expected subject"
    (is (= :e6b/transition-context-invalid
           (refusal (fixture #(update % :context dissoc :canonical/field-subject))))))
  (testing "stale or implausible review chronology"
    (is (= :e6b/outcome-review-unresolved
           (refusal (fixture #(assoc-in % [:outcome-review :reviewed-at]
                                      "2026-09-13T03:00:00Z"))))))
  (testing "plausible reference without artifact pin is not authority"
    (is (= :e6b/outcome-review-unresolved
           (refusal (fixture #(assoc-in % [:outcome-review :review/artifact-sha256]
                                      (apply str (repeat 64 "f")))))))))

(deftest replay-and-byte-authority-controls
  (let [cfg (fixture identity)
        a (e6b/verify-feedback cfg) b (e6b/verify-feedback cfg)]
    (is (= a b) "pure replay does not increment again"))
  (let [cfg (fixture identity)]
    (is (= :e6b/source-pin-mismatch
           (refusal (assoc-in cfg [:sources :outcome :sha256] (apply str (repeat 64 "0")))))))
  (is (= :e6b/canonical-e3-unavailable
         (refusal (assoc-in (fixture identity) [:canonical :e3] nil))))
  (is (= :e6b/production-authority-unavailable
         (refusal (assoc (fixture identity) :mode :production)))))
