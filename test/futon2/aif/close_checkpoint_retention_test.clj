(ns futon2.aif.close-checkpoint-retention-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner])
  (:import [java.nio.charset StandardCharsets]
           [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn temp-dir []
  (.toFile (Files/createTempDirectory
            "persist-close-fallback-" (make-array FileAttribute 0))))

(deftest streamed-edn-is-byte-identical-to-pr-str-format
  (let [root (temp-dir)
        out (io/file root "streamed.edn")
        value {:z [1 nil "lambda\n"] :a [:x :y] :nested {:k true}}
        expected (.getBytes (str (pr-str value) "\n") StandardCharsets/UTF_8)]
    (runner/write-edn-stream! out value)
    (is (= (vec expected) (vec (Files/readAllBytes (.toPath out)))))))

(deftest mid-stream-failure-persists-small-checkpoint-digests
  (let [root (temp-dir)
        selection {:judgment {:selected-mission "T-1"
                              :decision {:selection-certificate
                                         {:g-term-decomposition {:policies [:p1]}
                                          :policy-pair-census
                                          {:different-arrangement-pairs 1}}}}}
        construction {:judgment {:cascade {:patterns [:a :b]}}}
        result {:outcome :build-failed
                :checkpoints {:selection selection :construction construction}
                :data {:target "T-1" :failure-kind :build-failed
                       :failure-stage :build :error "fixture close"}}
        partial-writer
        (fn [file value]
          (with-open [out (java.io.FileOutputStream. file)]
            (.write out (.getBytes (subs (pr-str value) 0 64)
                                   StandardCharsets/UTF_8)))
          (throw (java.io.IOException. "disk full after 64 bytes")))
        opts {:run-record-dir (.getPath root)
              :scan-render-fn (fn [& _] nil)
              :run-record-write-fn partial-writer}
        saved (runner/persist-with-close-fallback!
               #'runner/persist-run-record! opts "run-close" "start" result)
        record (edn/read-string (slurp (:run-record saved)))]
    (is (= :present (:run-record-status saved)))
    (is (= :typed-small-close-failure (:run-record-form saved)))
    (is (= [:construction :selection] (:checkpoint-keys record)))
    (is (= #{:construction :selection} (set (keys (:checkpoint-digests record)))))
    (is (every? #(re-matches #"[0-9a-f]{64}" (:sha256 %))
                (vals (:checkpoint-digests record))))
    (is (= "java.io.IOException" (get-in record [:failure :exception-class])))
    (is (= "disk full after 64 bytes" (get-in record [:failure :error])))
    (is (= :failure (get-in record [:terminal-receipt :kind])))))

(deftest double-write-failure-is-returned-not-thrown
  (let [root (temp-dir)
        fail (fn [& _] (throw (java.io.IOException. "unwritable device")))
        result {:outcome :build-failed
                :checkpoints {:selection {:judgment {:decision {:id :kept}}}
                              :construction {:judgment {:cascade [:kept]}}}
                :data {:failure-kind :build-failed :failure-stage :build}}
        saved (runner/persist-with-close-fallback!
               #'runner/persist-run-record!
               {:run-record-dir (.getPath root)
                :scan-render-fn (fn [& _] nil)
                :run-record-write-fn fail
                :emergency-run-record-write-fn fail}
               "run-double-failure" "start" result)]
    (is (= :absent (:run-record-status saved)))
    (is (= :terminal-persistence-double-failure
           (get-in saved [:run-record-error :kind])))
    (is (= "java.io.IOException"
           (get-in saved [:run-record-error :first :class])))
    (is (= "java.io.IOException"
           (get-in saved [:run-record-error :second :class])))))
