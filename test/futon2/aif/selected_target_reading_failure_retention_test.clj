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

(defn- run-record-for-failure [dir run-id error-data]
  (#'runner/persist-run-record!
   {:run-record-dir (.getPath dir)
    :scan-render-fn (fn [& _] nil)}
   run-id "2026-10-28T00:00:00Z"
   {:outcome :incomplete
    :wm/route [{:fromNode "RUNNER" :toNode "FULL_LOOP_CLOSE"
                :via :incomplete}]
    :data {:failure-kind :selected-target-reading-redecision-failed
           :failure-stage :selected-target-reading-redecision
           :error "redecision failed"
           :error-data error-data}}))

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
