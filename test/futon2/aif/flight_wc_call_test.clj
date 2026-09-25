(ns futon2.aif.flight-wc-call-test
  "M-wm-wiring step 11: the flight runs the W_c checker on the click's run
  record and the enactment record and hands its verdict to the habit fold's
  increment unchanged. Fixture checkers are bb scripts in a temp dir; one run
  uses the real checker on click-001's two records, read from futon3c by
  absolute path (outside this namespace's warrant)."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.flight-runner :as fr])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- temp-dir [] (.toFile (Files/createTempDirectory "wc-call" (make-array FileAttribute 0))))

(defn- checker-printing
  "A bb script that prints S and exits EXIT, whatever it is given."
  [s & [exit]]
  (let [f (io/file (temp-dir) "checker.clj")]
    (spit f (str "(println " (pr-str s) ")\n"
                 (when exit (str "(binding [*out* *err*] (println \"fixture failure\"))\n(System/exit " exit ")\n"))))
    (str f)))

(def enactment
  {:click "run-1" :candidate :cand/x
   :attempts [{:n 1 :pattern :p/a :success true} {:n 2 :pattern :p/b :success true}]
   :conformance {:deviations []}})

(defn- enacted []
  (let [f (io/file (temp-dir) "enactment.edn")]
    (spit f (pr-str enactment))
    {:enactment enactment :record-path (str f)}))

(defn- call [checker & [increment!]]
  ((fr/wc-verdict-fn (cond-> {:click-record-path (constantly "/nonexistent/click.edn")}
                       checker (assoc :checker checker)
                       increment! (assoc :increment! increment!)))
   {:target "M-t"} (enacted)))

(deftest a-pass-reaches-increment-and-counts-one
  (let [r (call (checker-printing "[]"))]
    (is (= [] (get-in r [:wc :verdict])))
    (is (= 1 (get-in r [:increment :delta])))
    (is (= [:pattern-cascade "M-t" [:p/a :p/b] {}] (get-in r [:increment :policy-key])))))

(deftest a-failure-vector-counts-zero-with-its-failures
  (let [v ["W_c: chosen patterns with no successful attempt: [:p/c]"]
        r (call (checker-printing (pr-str v)))]
    (is (= v (get-in r [:wc :verdict])) "byte-identical as EDN read back")
    (is (= 0 (get-in r [:increment :delta])))
    (is (= v (get-in r [:increment :wc-failures])))))

(deftest join-unverifiable-counts-zero-with-its-status
  (let [v {:status :join-unverifiable :failures []}
        r (call (checker-printing (pr-str v)))]
    (is (= v (get-in r [:wc :verdict])))
    (is (= 0 (get-in r [:increment :delta])))
    (is (= {:status :join-unverifiable} (get-in r [:increment :wc-verdict])) "531cfaaa's pass-through")))

(deftest no-checker-is-a-typed-absence-and-no-increment
  (let [called (atom 0)
        r (call nil (fn [& _] (swap! called inc) :called))]
    (is (= {:wc {:absent :no-wc-checker-configured}} r))
    (is (zero? @called) "never a default pass")))

(deftest a-failing-checker-is-a-typed-failure-not-a-verdict
  (let [called (atom 0)
        r (call (checker-printing "[]" 1) (fn [& _] (swap! called inc)))]
    (is (= :checker-failed (get-in r [:wc :refused])))
    (is (= 1 (get-in r [:wc :exit])))
    (is (re-find #"fixture failure" (get-in r [:wc :stderr])))
    (is (zero? @called)))
  (is (= :checker-failed (get-in (call (checker-printing "not edn at all (")) [:wc :refused]))))

(deftest the-real-checker-on-click-001
  (let [dir "/home/joe/code/futon3c/holes/labs/M-futon-seams/exemplar"
        seen (atom nil)
        r ((fr/wc-verdict-fn {:checker (str dir "/proof2a_check.clj")
                              :click-record-path (constantly (str dir "/click-001.edn"))
                              :increment! (fn [_ _ v] (reset! seen v) {:delta 0})})
           {:target "M-futon-seams"}
           {:enactment (edn/read-string (slurp (str dir "/click-001-enactment.edn")))
            :record-path (str dir "/click-001-enactment.edn")})]
    (is (= ["W_c: no successful attempt names a G_c pass (X_c(d)): the grain attempt's check is not grain-gate"]
           (get-in r [:wc :verdict]) @seen)
        "per 4bc95005: the recorded grain attempt names no G_c pass")))

(deftest the-real-checker-on-a-tick-run-record
  ;; WM-PRESPIKE-I (futon3c d85b5941): the checker reads the tick's run-record
  ;; shape; the reduced record and its enactment are futon3c fixtures, read
  ;; by absolute path (outside this namespace's warrant)
  (let [fx "/home/joe/code/futon3c/test/futon3c/exemplar/fixtures/"
        enactment-path (str fx "enactment-machine@a4b4fc78.edn")
        seen (atom nil)
        r ((fr/wc-verdict-fn {:checker "/home/joe/code/futon3c/holes/labs/M-futon-seams/exemplar/proof2a_check.clj"
                              :click-record-path (constantly (str fx "tick-run-record-reduced@a4b4fc78.edn"))
                              :increment! (fn [_ _ v] (reset! seen v) {:delta (if (= [] v) 1 0)})})
           {:target "M-aif-policy-conditioned-eig"}
           {:enactment (edn/read-string (slurp enactment-path)) :record-path enactment-path})]
    (is (= [] (get-in r [:wc :verdict]) @seen) "the verdict reaches increment")
    (is (= 1 (get-in r [:increment :delta])))))
