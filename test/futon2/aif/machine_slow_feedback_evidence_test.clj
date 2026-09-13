(ns futon2.aif.machine-slow-feedback-evidence-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [futon2.aif.machine-slow-feedback-evidence :as e6b]
            [futon2.aif.temporal-hierarchy :as hierarchy])
  (:import [java.nio.file Files]
           [java.security MessageDigest]))

(def common {:scope :isolated-test :model/id :wm :model/revision "m1"
             :run/id "run-1" :tick/index 4})
(def context
  (merge common {:schema/version :wm/e6b-transition-context-v1
                 :destination/tick-index 5 :prior-state/revision "slow-4"
                 :next-state/revision "slow-5" :feedback/event-id "feedback-1"
                 :application/id "apply-1" :candidate/occurrence-id "candidate-7"
                 :action {:type :close-hole :target "H7"}
                 :destination/as-of "2026-09-13T05:00:00Z"}))
(def prior-entry {:alpha 2.0 :beta 2.0 :intrinsic-value 0.5
                  :n-emissions 2 :n-followthrough 1 :as-of "2026-09-13T04:00:00Z"})
(def prior
  (merge common {:schema/version :wm/e6b-prior-slow-state-v1 :state/revision "slow-4"
                 :slow/mode :exploitation
                 :slow/intrinsics {:close-hole prior-entry :explore prior-entry}}))
(def e2b
  (merge common {:schema/version :wm/e6b-e2b-subject-v1
                 :candidate/occurrence-id "candidate-7"
                 :action {:type :close-hole :target "H7"} :status :enacted
                 :r9/pre-enact-decision :mechanism-authorized
                 :r9/authorization-ref "r9-authorization-1"}))
(def outcome
  (merge common {:schema/version :wm/e6b-outcome-authority-v1
                 :candidate/occurrence-id "candidate-7"
                 :action {:type :close-hole :target "H7"}
                 :terminal/status :succeeded :fast/action-class :close-hole
                 :fast/witnessed? true :fast/succeeded? true
                 :outcome/evidence-id "outcome-1" :outcome/authority-ref "review-1"
                 :outcome/producer-id "worker-1" :outcome/reviewer-id "reviewer-1"}))

(defn- digest [^bytes bs]
  (apply str (map #(format "%02x" (bit-and 0xff %))
                  (.digest (doto (MessageDigest/getInstance "SHA-256") (.update bs))))))
(defn- vd [x] (digest (.getBytes (pr-str x) "UTF-8")))
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
               :input/digests {:context (vd context) :prior (vd prior)
                               :e2b (vd e2b) :outcome (vd outcome)}
               :output/digest (vd next)}]
    {:context context :prior-state prior :e2b-subject e2b :outcome outcome :next-state next
     :application-ledger {:schema/version :wm/e6b-application-ledger-v1 :scope :isolated-test
                          :entries [entry]}
     :application-universe {:schema/version :wm/e6b-application-universe-v1
                            :scope :isolated-test :feedback/event-id "feedback-1"
                            :complete/application-ids ["apply-1"]
                            :authority/status :independently-configured-complete
                            :authority/owner "fixture-authority"}}))

(defn- fixture [f]
  (let [dir (.toFile (Files/createTempDirectory "e6b" (make-array java.nio.file.attribute.FileAttribute 0)))
        records (f (base-records))
        sources (into {}
                      (for [label e6b/source-order
                            :let [file (io/file dir (str (name label) ".edn"))
                                  bytes (.getBytes (str (pr-str (records label)) "\n") "UTF-8")]]
                        (do (Files/write (.toPath file) bytes (make-array java.nio.file.OpenOption 0))
                            [label {:relative-path (.getName file) :sha256 (digest bytes)}])))]
    {:mode :isolated-test :evidence-root (.getPath dir) :sources sources}))
(defn- refusal [cfg]
  (try (e6b/verify-feedback cfg) nil
       (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))

(deftest replays-exact-feedback-without-storage-claim
  (let [out (e6b/verify-feedback (fixture identity))]
    (is (:replay/identical? out))
    (is (= 3.0 (get-in out [:state :slow/intrinsics :close-hole :alpha])))
    (is (= 2.0 (get-in out [:state :slow/intrinsics :close-hole :beta])))
    (is (= prior-entry (get-in out [:state :slow/intrinsics :explore])))
    (is (false? (:storage-enforcement? out)))
    (is (false? (:production-edge-fired? out)))
    (is (= (set e6b/source-order) (set (map :label (:sources out)))))))

(deftest outcome-and-prior-refusals
  (doseq [[label f expected]
          [[:missing-success #(update % :outcome dissoc :fast/succeeded?) :e6b/outcome-authority-invalid]
           [:nonterminal #(assoc-in % [:outcome :terminal/status] :pending) :e6b/outcome-authority-invalid]
           [:unwitnessed #(assoc-in % [:outcome :fast/witnessed?] false) :e6b/outcome-authority-invalid]
           [:missing-class #(update-in % [:prior-state :slow/intrinsics] dissoc :close-hole)
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
  (is (= :e6b/e2b-subject-mismatch
         (refusal (fixture #(assoc-in % [:e2b-subject :r9/pre-enact-decision] :missing))))))

(deftest replay-and-byte-authority-controls
  (let [cfg (fixture identity)
        a (e6b/verify-feedback cfg) b (e6b/verify-feedback cfg)]
    (is (= a b) "pure replay does not increment again"))
  (let [cfg (fixture identity)]
    (is (= :e6b/source-pin-mismatch
           (refusal (assoc-in cfg [:sources :outcome :sha256] (apply str (repeat 64 "0")))))))
  (is (= :e6b/production-authority-unavailable
         (refusal (assoc (fixture identity) :mode :production)))))
