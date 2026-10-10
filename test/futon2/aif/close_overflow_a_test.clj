(ns futon2.aif.close-overflow-a-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.full-loop-runner :as runner])
  (:import [java.io Writer]
           [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn temp-dir []
  (.toFile (Files/createTempDirectory
            "close-overflow-a-" (make-array FileAttribute 0))))

(defn virtual-counting-writer [counter]
  (proxy [Writer] []
    (write
      ([x] (swap! counter + (if (integer? x) 1 (count (str x)))))
      ([x off len] (swap! counter + len)))
    (flush [])
    (close [])))

(defn over-jvm-limit-printer [value]
  ;; The Writer contract is the relevant boundary: neither printer receives
  ;; nor constructs the virtual payload.  This models click 50's selection
  ;; cell while making any accidental pr-str/getBytes implementation fail the
  ;; asserted count.
  (let [^Writer out *out*]
    (.write out "x" 0 Integer/MAX_VALUE)
    (.write out "x" 0 32)))

(deftest both-durable-writers-cross-the-jvm-string-limit-incrementally
  (doseq [[label emit] [[:cohort cohort/write-edn-to-writer!]
                        [:run-record runner/write-edn-to-writer!]]]
    (testing (name label)
      (let [n (atom 0)
            out (virtual-counting-writer n)]
        (emit out {:selection :click-50-shape} over-jvm-limit-printer)
        ;; +1 is the durable trailing newline.
        (is (= (+ (long Integer/MAX_VALUE) 33) @n))))))

(deftest cohort-stream-is-byte-identical-and-round-trips
  (let [root (temp-dir)
        path (io/file root "selection.edn")
        value {:checkpoint :selection
               :candidates [{:id [:T-1 :p-1] :g 1.25}
                            {:id [:T-2 :p-2] :g -0.5}]}]
    (#'cohort/write-new! path value)
    (is (= (str (pr-str value) "\n") (slurp path)))
    (is (= value (cohort/read-edn path)))))

(deftest caught-non-persist-close-error-goes-directly-to-bounded-record
  (let [root (temp-dir)
        normal-called? (atom false)
        result {:outcome :build-failed
                :checkpoints {:selection {:judgment {:decision {:id :kept}}}
                              :construction {:judgment {:cascade [:kept]}}}
                :data {:failure-kind :delivery-qa-failed
                       :failure-stage :close
                       :exception-class "java.io.IOException"
                       :error "delivery QA exploded"}}
        saved (runner/persist-with-close-fallback!
               (fn [& _] (reset! normal-called? true))
               {:run-record-dir (.getPath root)}
               "run-close-stage" "start" result)
        record (edn/read-string (slurp (:run-record saved)))]
    (is (false? @normal-called?))
    (is (= :typed-small-close-failure (:run-record-form saved)))
    (is (= [:construction :selection] (:checkpoint-keys record)))
    (is (= "java.io.IOException" (get-in record [:failure :exception-class])))
    (is (= "delivery QA exploded" (get-in record [:failure :error])))))
