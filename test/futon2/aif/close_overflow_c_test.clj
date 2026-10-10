(ns futon2.aif.close-overflow-c-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.policy :as policy])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn printed-chars [value]
  (let [n (atom 0)
        out (proxy [java.io.Writer] []
              (write
                ([x] (swap! n + (if (integer? x) 1 (count (str x)))))
                ([x _ len] (swap! n + len)))
              (flush []) (close []))]
    (binding [*out* out *print-length* nil *print-level* nil] (pr value))
    @n))

(deftest click-51-shape-compacts-only-the-durable-value-below-100mb
  (let [receipt (apply str (repeat 24576 "r"))
        make-policy (fn [i]
                      {:kind :cascade-candidate :id (keyword (str "p" i))
                       :target (str "T-" (mod i 425))
                       :precedence [{:id :first :target (str "T-" (mod i 425))
                                     :guard {:needs #{} :forbids #{}}
                                     :produces #{:advance}}]
                       :construction-receipt {:evidence receipt}
                       :interpretation-receipts [{:reading receipt}]})
        policies (mapv (fn [i] {:id (make-policy i) :g (double i)}) (range 5365))
        chosen (get-in policies [1729 :id])
        live {:action chosen
              :selection-certificate {:candidates policies}
              :cascade-lanes (mapv (fn [n] {:target (str "T-" n)
                                             :candidates policies})
                                   (range 20))}
        live-before live
        durable (policy/compact-cascade-carriers live)
        bytes (printed-chars durable)
        durable-candidates (get-in durable [:selection-certificate :candidates])
        chosen-after (first (filter #(= (:id chosen) (get-in % [:id :id]))
                                    durable-candidates))]
    (println "[click-51-durable-bytes]" bytes)
    (is (= live-before live) "the decision supplied to consumers is unchanged")
    (is (< bytes 100000000) (str "durable bytes=" bytes))
    (is (= chosen (:id chosen-after)) "chosen receipt remains complete")
    (is (every? (fn [{:keys [id g]}]
                  (and (:target id) (:id id) (seq (:precedence id)) (number? g)))
                durable-candidates))))

(deftest emergency-record-keeps-click-and-original-exception-identity
  (let [root (.toFile (Files/createTempDirectory
                       "close-c-identity-" (make-array FileAttribute 0)))
        caught {:class "java.lang.OutOfMemoryError"
                :message "Required array length 2147483638 + 10 is too large"
                :ex-data {} :stack ["futon2.aif.full_loop_cohort/read_edn"]}
        result {:attempt-id "attempt-001" :opportunity-id "opp-51"
                :outcome :build-failed :checkpoints {:selection {:sorry {:kind :kept}}}
                :data {:failure-kind :close-exception :failure-stage :close
                       :exception-class "java.lang.OutOfMemoryError"
                       :error (:message caught) :caught-exception caught}}
        saved (runner/persist-with-close-fallback!
               (fn [& _] (throw (AssertionError. "normal writer called")))
               {:run-record-dir (.getPath root) :click-id "wm-click-4202817c"}
               "2026-10-10-9eda8ab1" "start" result)
        record (edn/read-string (slurp (:run-record saved)))]
    (is (= "2026-10-10-9eda8ab1" (:run/id record)))
    (is (= "wm-click-4202817c" (:click/id record)))
    (is (= "opp-51" (:opportunity-id record)))
    (is (= caught (:caught-exception record)))))

(deftest append-order-reader-does-not-replay-selection-payload
  (let [dir (.toFile (Files/createTempDirectory
                      "close-c-headers-" (make-array FileAttribute 0)))
        first-event {:attempt/id "attempt-001" :attempt/ordinal 1
                     :event/sequence 1 :checkpoint/type :time-step
                     :payload {:judgment {:opportunity-id "opp"}}}]
    (spit (io/file dir "001-time-step.edn") (pr-str first-event))
    ;; Deliberately unreadable as EDN: append authorization must consult the
    ;; immutable filename/header authority, never allocate this payload.
    (spit (io/file dir "002-selection.edn") "{gigantic-selection-payload")
    (is (= [:time-step :selection]
           (mapv :checkpoint/type (cohort/attempt-event-headers dir))))
    (is (thrown? RuntimeException (cohort/attempt-events dir)))))
