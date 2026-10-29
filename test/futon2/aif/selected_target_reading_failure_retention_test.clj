(ns futon2.aif.selected-target-reading-failure-retention-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.selected-target-reading :as selected-target-reading])
  (:import [java.nio.file Files]
           [java.security MessageDigest]))

(defn- temp-dir []
  (.toFile (Files/createTempDirectory "selected-target-reading-failure-"
                                      (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- sha256-file [path]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                        (Files/readAllBytes (.toPath (io/file path))))]
    (apply str (map #(format "%02x" (bit-and 0xff %)) digest))))

(defn- failure-files [dir]
  (vec (filter #(re-find #"selected-target-reading-failure[.]edn$" (.getName %))
               (or (seq (.listFiles dir)) []))))

(def reading-record
  {:schema :wm/selected-target-reading-request-v1
   :status :published
   :published true
   :target "M-selected"
   :records [{:target "M-a" :status :attempted}
             {:target "M-b" :status :refused}]
   :pending [{:target "M-c" :reason :awaiting-author}]})

(defn- run-record-for-failure
  ([dir run-id error-data]
   (run-record-for-failure dir run-id error-data nil))
  ([dir run-id error-data reading-state]
   (#'runner/persist-run-record!
    (cond-> {:run-record-dir (.getPath dir)
             :scan-render-fn (fn [& _] nil)}
      reading-state (assoc :selected-target-reading/state reading-state))
    run-id "2026-10-28T00:00:00Z"
    {:outcome :incomplete
     :wm/route [{:fromNode "RUNNER" :toNode "FULL_LOOP_CLOSE"
                 :via :incomplete}]
     :data {:failure-kind :selected-target-reading-redecision-failed
            :failure-stage :selected-target-reading-redecision
            :error "redecision failed"
            :error-data error-data}})))

(def retained-r28-observation
  ;; Copied from the retained production record below.  The record is live
  ;; evidence and intentionally untracked, so the regression keeps its exact
  ;; discriminating projection rather than depending on the operator checkout.
  ;; data/wm-runs/tick-run-record-2026-10-26-46f8cac7-1541-4f1f-83f5-aa8f6bac2a2a.edn
  {:selected-target-reading
   {:status :not-applicable
    :reason :selected-target-reading-not-required}
   :reading-frontiers
   {:selection-frontier
    {:status :unavailable :reason :target-policy-field-not-retained}
    :preparation-backlog
    {:status :not-applicable
     :reason :selected-target-reading-not-required}}})

(deftest reading-phase-failure-retains-complete-record-and-terminal-reference
  (let [dir (temp-dir)
        opts {:run-id "reading-phase-failure" :run-record-dir (.getPath dir)}
        failure (with-redefs [selected-target-reading/preparation-needed? (constantly true)]
                  (try
                    (runner/selected-target-reading-step
                     opts {:decision {}}
                     (fn [_ _] reading-record)
                     (fn [_]
                       (throw (ex-info "reading phase failed"
                                       {:failure-kind :reading-phase-failed}))))
                    (catch clojure.lang.ExceptionInfo e e)))
        reference (:selected-target-reading/failure-record (ex-data failure))
        retained (edn/read-string (slurp (:path reference)))
        persisted (run-record-for-failure dir "reading-terminal" (ex-data failure))
        run-record (edn/read-string (slurp (:run-record persisted)))]
    (is (= 1 (count (failure-files dir))))
    (is (= (:records reading-record) (:records retained)))
    (is (= (:pending reading-record) (:pending retained)))
    (is (= reading-record retained))
    (is (= (str (pr-str reading-record) "\n") (slurp (:path reference)))
        "the durable artifact contains the unprojected record bytes")
    (is (= (sha256-file (:path reference)) (:sha256 reference)))
    (is (= reference
           (get-in run-record [:failure :detail
                               :selected-target-reading/failure-record])))))

(deftest redecision-wrapper-retains-once-without-masking-original
  (let [dir (temp-dir)
        opts {:run-id "redecision-failure" :run-record-dir (.getPath dir)}
        original (ex-info "original redecision failure" {:kind :redecision-broke})
        inner-data (#'runner/selected-target-reading-failure-data
                    opts reading-record
                    {:failure-kind :selected-target-reading-redecision-failed
                     :selected-target-reading reading-record})
        wrapped (ex-info "wrapped redecision failure" inner-data original)
        failure (with-redefs [selected-target-reading/preparation-needed? (constantly true)]
                  (try
                    (runner/selected-target-reading-step
                     opts {:decision {}}
                     (fn [_ _] reading-record)
                     (fn [_] (throw wrapped)))
                    (catch clojure.lang.ExceptionInfo e e)))]
    (is (= 1 (count (failure-files dir)))
        "the phase boundary must not rewrite the redecision catch's file")
    (is (= "wrapped redecision failure" (ex-message failure)))
    (is (identical? wrapped (.getCause failure)))
    (is (= :redecision-broke (-> failure .getCause .getCause ex-data :kind)))
    (is (= reading-record
           (edn/read-string
            (slurp (get-in (ex-data failure)
                           [:selected-target-reading/failure-record :path])))))))

(deftest successful-reading-writes-no-failure-file
  (let [dir (temp-dir)
        result (with-redefs [selected-target-reading/preparation-needed? (constantly true)]
                 (runner/selected-target-reading-step
                  {:run-id "reading-success" :run-record-dir (.getPath dir)}
                  {:decision {}}
                  (fn [_ _] reading-record)
                  (fn [_] {:decision {:status :selected}})))]
    (is (= reading-record (:record result)))
    (is (empty? (failure-files dir)))))

(deftest failure-record-write-error-does-not-mask-reading-failure
  (let [dir (temp-dir)
        not-a-directory (io/file dir "ordinary-file")
        _ (spit not-a-directory "occupied")
        original (ex-info "original reading failure" {:kind :reading-broke})
        failure (with-redefs [selected-target-reading/preparation-needed? (constantly true)]
                  (try
                    (runner/selected-target-reading-step
                     {:run-id "unwritable" :run-record-dir (.getPath not-a-directory)}
                     {:decision {}}
                     (fn [_ _] reading-record)
                     (fn [_] (throw original)))
                    (catch clojure.lang.ExceptionInfo e e)))]
    (is (identical? original (.getCause failure)))
    (is (= :reading-broke (-> failure .getCause ex-data :kind)))
    (is (map? (:selected-target-reading/failure-record-write-error
               (ex-data failure))))
    (is (nil? (:selected-target-reading/failure-record (ex-data failure))))))

(deftest failed-redecision-observes-the-reading-that-actually-ran
  (let [state (atom {:status :not-started})
        dir (temp-dir)
        failure (with-redefs [selected-target-reading/preparation-needed? (constantly true)]
                  (try
                    (runner/selected-target-reading-step
                     {:run-id "r28-replay"
                      :run-record-dir (.getPath dir)
                      :selected-target-reading/state state}
                     {:decision {}}
                     (fn [_ _] reading-record)
                     (fn [_] (throw (ex-info "retained R28 redecision failure" {}))))
                    (catch clojure.lang.ExceptionInfo e e)))
        persisted (run-record-for-failure dir "r28-observer"
                                          (ex-data failure) state)
        observed (edn/read-string (slurp (:run-record persisted)))]
    (is (= {:status :not-applicable
            :reason :selected-target-reading-not-required}
           (:selected-target-reading retained-r28-observation))
        "the retained R28 record is the discriminating red witness")
    (is (= {:status :completed :record reading-record} @state))
    (is (= reading-record (:selected-target-reading observed)))
    (is (= (:records reading-record)
           (get-in observed [:reading-frontiers :preparation-backlog :records])))
    (is (= (:pending reading-record)
           (get-in observed [:reading-frontiers :preparation-backlog :pending])))))

(deftest reading-observer-distinguishes-not-started-from-missing-result
  (let [no-checkpoint {:checkpoints {}}]
    (is (= {:status :not-applicable
            :reason :selected-target-reading-not-required}
           (#'runner/retained-selected-target-reading
            (atom {:status :not-started}) no-checkpoint)))
    (is (= {:status :absent
            :reason :selected-target-reading-started-without-result}
           (#'runner/retained-selected-target-reading
            (atom {:status :started}) no-checkpoint)))
    (is (= {:status :absent
            :reason :selection-checkpoint-without-request}
           (#'runner/retained-selected-target-reading nil no-checkpoint)))))
