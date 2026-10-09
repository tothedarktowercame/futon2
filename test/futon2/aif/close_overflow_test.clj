(ns futon2.aif.close-overflow-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.policy :as policy]
            [futon2.aif.wm.cascade-decision :as decision])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn printed-chars [value]
  (let [n (atom 0)
        out (proxy [java.io.Writer] []
              (write
                ([x] (swap! n + (if (integer? x) 1 (count (str x)))))
                ([x off len] (swap! n + len)))
              (flush [])
              (close []))]
    (binding [*out* out *print-length* nil *print-level* nil] (pr value))
    @n))

(deftest cohort-checkpoint-writer-streams-large-edn
  (let [root (.toFile (Files/createTempDirectory
                       "cohort-stream-" (make-array FileAttribute 0)))
        path (io/file root "selection.edn")
        value {:checkpoint :selection
               :candidates (vec (repeat 20000 {:id {:target "T-1"
                                                    :precedence [:a :b]}
                                               :g 1.25}))}]
    (#'cohort/write-new! path value)
    (is (= value (edn/read-string (slurp path))))
    (is (> (.length path) 1000000))))

(deftest numeric-g-count-uses-production-candidate-shape
  (let [lanes [{:target "T-a"} {:target "T-b"} {:target "T-c"}]
        candidates [{:action {:target "T-a"} :controller-score 1.0}
                    {:id {:target "T-b"} :G-efe 2.0}
                    {:action {:target "T-c"} :controller-score :infinite}
                    {:action {:target "outside"} :controller-score 3.0}]]
    (is (= 2 (decision/numeric-g-target-count lanes candidates)))))

(deftest click-50-shaped-compaction-is-bounded-and-keeps-chosen-whole
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
        decision {:action chosen
                  :selection-certificate {:candidates policies}
                  :cascade-lanes [{:target (:target chosen)
                                   :candidates policies}]}
        compact (policy/compact-cascade-carriers decision)
        before (printed-chars decision)
        after (printed-chars compact)
        compact-policies (get-in compact [:selection-certificate :candidates])
        chosen-after (first (filter #(= (:id chosen) (get-in % [:id :id]))
                                    compact-policies))]
    (println "[click-50-shape-bytes]" {:before before :after after})
    (is (> before 250000000))
    (is (< after 100000000))
    (is (= chosen (:id chosen-after)))
    (is (every? (fn [{:keys [id g]}]
                  (and (:target id) (:id id) (seq (:precedence id)) (number? g)))
                compact-policies))))

(deftest non-persist-close-failure-goes-directly-to-bounded-record
  (let [root (.toFile (Files/createTempDirectory
                       "close-step-failure-" (make-array FileAttribute 0)))
        full-writer-called? (atom false)
        result {:outcome :build-failed
                :checkpoints {:selection {:judgment {:decision {:id :selection}}}
                              :construction {:judgment {:cascade {:id :construction}}}}
                :data {:failure-kind :close-exception :failure-stage :close
                       :exception-class "java.io.IOException"
                       :error "delivery QA stream broke"}}
        saved (runner/persist-with-close-fallback!
               (fn [& _] (reset! full-writer-called? true))
               {:run-record-dir (.getPath root)} "run-close-step" "start" result)
        record (edn/read-string (slurp (:run-record saved)))]
    (is (false? @full-writer-called?))
    (is (= [:construction :selection] (:checkpoint-keys record)))
    (is (= #{:construction :selection} (set (keys (:checkpoint-digests record)))))
    (is (= "java.io.IOException" (get-in record [:failure :exception-class])))
    (is (= "delivery QA stream broke" (get-in record [:failure :error])))))
