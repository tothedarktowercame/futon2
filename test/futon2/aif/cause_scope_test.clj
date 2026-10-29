(ns futon2.aif.cause-scope-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.cause-scope :as cause-scope]
            [futon2.aif.load-identity :as identity]))

(defn- pinned! [value]
  (let [file (java.io.File/createTempFile "cause-scope-" ".edn")
        bytes (.getBytes (pr-str value) "UTF-8")]
    (spit file (String. bytes "UTF-8"))
    {:file file :pin {:path (.getCanonicalPath file)
                      :sha256 (identity/sha256 bytes)}}))

(deftest retained-bad-proposals-are-mechanically-refused
  (let [scope {:cause-entry "futon2.aif.reviewer-falsifier/receipt"
               :input-pin {:path "fixture" :sha256 (apply str (repeat 64 "a"))}
               :input-path [:earliest-carrier]
               :input-digest (apply str (repeat 64 "b"))
               :healthy-predicate {:op :result-path-equals :path [:status]
                                   :value :verified}}
        ;; Byte-for-byte retained proposal receipts copied into this hermetic
        ;; fixture directory. e43 has four tests; f7 has six and fabricates the
        ;; intermediate want/falsifier inputs.
        e43 (edn/read-string (slurp "test/fixtures/cause-scope/acceptance-e43a348c.edn"))
        f7 (edn/read-string (slurp "test/fixtures/cause-scope/acceptance-f7e97cc6.edn"))]
    (is (= :cause-scope/designated-test-count
           (cause-scope/designated-test-defect
            (get-in e43 [:acceptance-test :source :text]) scope)))
    (is (= :cause-scope/designated-test-count
           (cause-scope/designated-test-defect
            (get-in f7 [:acceptance-test :source :text]) scope)))))

(deftest f7-is-cause-scope-unretained
  (let [record-path "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-29-5ba921b9-0431-4b85-95a7-6606a01111b8.edn"
        record-bytes (java.nio.file.Files/readAllBytes (.toPath (java.io.File. record-path)))
        _ (is (= "5b53bad4a0c243fec48e928930d708f5729212335a70f6b0a6f73e1d671e0d2b"
                 (identity/sha256 record-bytes)))
        record (edn/read-string (String. record-bytes "UTF-8"))
        {:keys [file pin]} (pinned! record)
        entry {:var "futon2.aif.reviewer-falsifier/receipt"
               ;; The run retains the empty selected-wants result, but not the
               ;; complete receipt input consumed by this public boundary.
               :input {:record-pin pin :path [:reviewer-falsifier-inputs]}
               :healthy-predicate {:op :result-path-equals :path [:status]
                                   :value :verified}}]
    (try
      (is (= [] (get-in record [:cascade-feedback :mission-state :selected-wants])))
      (is (= :cause-scope-unretained
             (try (cause-scope/seal entry) nil
                  (catch clojure.lang.ExceptionInfo e (:kind (ex-data e))))))
      (finally (.delete file)))))

(deftest real-recorded-injury-carrier-is-sealed-and-admitted-red
  (let [retained-record
        (edn/read-string
         (slurp "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-29-5ba921b9-0431-4b85-95a7-6606a01111b8.edn"))
        {:keys [file pin]} (pinned! retained-record)
        entry {:var "futon2.aif.wm.terminal-receipt/terminal-receipt"
               :input {:record-pin pin :path []}
               :healthy-predicate {:op :result-path-equals :path [:kind]
                                   :value :action-receipt}}
        scope (cause-scope/seal entry)
        source (str "(ns retained.correct (:require [clojure.test :refer [deftest]]))\n"
                    "(deftest cause (futon2.aif.cause-scope/assert-repair "
                    (pr-str scope) "))")
        receipt {:schema :wm/cause-scope-receipt-v1 :scope scope
                 :observed-entry (:cause-entry scope) :invoked true
                 :predicate-observed-result true :healthy? false
                 :failures 1 :errors 0}
        retained (pinned! receipt)]
    (try
      (is (nil? (cause-scope/designated-test-defect source scope)))
      (is (thrown-with-msg? AssertionError #"cause remains unhealthy"
                            (cause-scope/assert-repair scope)))
      (is (cause-scope/red-receipt? scope receipt (:pin retained)))
      (finally (.delete file) (.delete (:file retained))))))

(deftest predicates-must-observe-the-production-result
  (testing "the closed vocabulary has no constant true/false predicate"
    (is (false? (cause-scope/predicate? {:op :constant :value true})))
    (is (false? (cause-scope/predicate? {:op :constant :value false})))))
