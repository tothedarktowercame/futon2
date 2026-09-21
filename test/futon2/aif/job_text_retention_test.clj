(ns futon2.aif.job-text-retention-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.job-text-retention :as retention])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]
           [java.security MessageDigest]))

(def ^:dynamic *attempt-dir* nil)
(use-fixtures :each
  (fn [f]
    (let [dir (.toFile (Files/createTempDirectory "job-texts-" (make-array FileAttribute 0)))]
      (try (binding [*attempt-dir* dir] (f))
           (finally (doseq [file (reverse (file-seq dir))] (io/delete-file file true)))))))

(defn assert-text [text ref]
  (let [file (io/file (:path ref)) bytes (Files/readAllBytes (.toPath file))]
    (is (= :present (:status ref)))
    (is (= text (slurp file :encoding "UTF-8")))
    (is (= (.getCanonicalPath *attempt-dir*) (.getCanonicalPath (.getParentFile file))))
    (is (= (:sha256 ref)
           (.formatHex (java.util.HexFormat/of)
                       (.digest (MessageDigest/getInstance "SHA-256") bytes))))))

(deftest exact-unicode-and-long-replies-are-retained
  (let [prompt "  Build π\n\nkeep whitespace\t"
        reply (str (apply str (repeat 9000 "λ")) "\nDONE\n")
        result (retention/retain-job-texts! *attempt-dir* :author "job-1" prompt reply)]
    (assert-text prompt (:prompt result))
    (assert-text reply (:reply result))
    (is (not= (get-in result [:prompt :path]) (get-in result [:reply :path])))))

(deftest missing-text-is-an-absence-not-a-file
  (doseq [[text reason] [[nil :text-not-recorded] ["" :text-empty]
                        [" \n" :text-empty] [{:summary "x"} :text-not-a-string]]]
    (let [result (retention/retain-job-texts! *attempt-dir* :reviewer "failed" text text)]
      (is (= {:status :absent :reason reason} (:prompt result) (:reply result)))))
  (is (= 1 (count (file-seq *attempt-dir*)))))

(deftest repeated-retention-is-idempotent-and-content-addressed
  (let [a (retention/retain-job-texts! *attempt-dir* :author "same" "prompt" "reply")
        b (retention/retain-job-texts! *attempt-dir* :author "same" "prompt" "reply")
        c (retention/retain-job-texts! *attempt-dir* :author "same" "new prompt" "reply")]
    (is (= a b))
    (is (= (:reply a) (:reply c)))
    (is (not= (:prompt a) (:prompt c)))
    (assert-text "prompt" (:prompt a))
    (assert-text "new prompt" (:prompt c))
    (is (= 4 (count (file-seq *attempt-dir*))))))

(deftest corrupted-existing-artifact-is-refused
  (let [a (retention/retain-job-texts! *attempt-dir* :author "same" "prompt" nil)]
    (spit (get-in a [:prompt :path]) "corrupt")
    (is (= :job-text-content-mismatch
           (try (retention/retain-job-texts! *attempt-dir* :author "same" "prompt" nil)
                nil
                (catch clojure.lang.ExceptionInfo e (:failure-kind (ex-data e))))))))

(deftest job-and-role-identities-cannot-escape-or-collide
  (let [a (retention/retain-job-texts! *attempt-dir* "../../author" "../job" "same" "same")
        b (retention/retain-job-texts! *attempt-dir* :reviewer "../job" "same" "same")]
    (assert-text "same" (:prompt a))
    (is (not= (get-in a [:prompt :path]) (get-in b [:prompt :path])))))

(deftest wrapped-ports-cover-roles-and-revisions-without-extra-calls
  (let [state (atom []) calls (atom []) next-id (atom 0)
        opts (retention/wrap-ports
              {:author "author" :reviewer "reviewer" :repair-reviewer "repair"}
              *attempt-dir* state
              (fn [_ actor _ _ prompt]
                (let [id (str (swap! next-id inc))]
                  (swap! calls conj [:dispatch id actor])
                  (with-meta {:job-id id} {::retention/dispatched-prompt (str "binding\n" prompt)})))
              (fn [_ id] (swap! calls conj [:poll id]) {:job-id id :state "done" :result (str "reply-" id)})
              (fn [_ id] (swap! calls conj [:read id]) {:job-id id :state "failed" :result-summary "summary"}))]
    (doseq [actor ["author" "reviewer" "repair" "author" "reviewer"]]
      (let [response ((:dispatch-fn opts) opts actor "caller" "mission" "prompt")]
        ((:poll-fn opts) opts (:job-id response))))
    (is (= 10 (count @calls)))
    (is (= [:author :reviewer :repair-reviewer :author :reviewer] (mapv :role @state)))
    (doseq [record @state]
      (assert-text "binding\nprompt" (:prompt record))
      (assert-text (str "reply-" (:job-id record)) (:reply record)))
    ((:read-job-fn opts) opts "recovered")
    (is (= 11 (count @calls)))
    (is (= :text-not-recorded (get-in (last @state) [:reply :reason])))))

(deftest dispatch-prompt-wins-over-agency-preview-and-summary-is-not-a-reply
  (let [state (atom [])
        opts (retention/wrap-ports
              {} *attempt-dir* state
              (fn [& _] {:job-id "job" :prompt "dispatch text"})
              (fn [& _] {:job-id "job" :state "failed" :result-summary "partial summary"
                         :events [{:type "prompt" :text "Agency preview …[trimmed]"}]})
              (fn [& _] nil))]
    ((:dispatch-fn opts) opts "author" "caller" "mission" "input")
    (let [dispatch-cell (retention/checkpoint-cell {:judgment {} :ground :test} @state)]
      ((:poll-fn opts) opts "job")
      (assert-text "dispatch text" (get-in dispatch-cell [:judgment :job-texts 0 :prompt]))
      (assert-text "dispatch text" (:prompt (first @state)))
      (is (= :dispatch (:prompt-source (first @state)))))
    (is (= {:status :absent :reason :text-not-recorded} (:reply (first @state))))))

(deftest checkpoint-references-cover-terms-and-failure-sorries
  (let [record (retention/retain-job-texts! *attempt-dir* :author "job" "prompt" "reply")]
    (is (= [record] (get-in (retention/checkpoint-cell {:judgment {} :ground :test} [record])
                            [:judgment :job-texts])))
    (is (= [record] (get-in (retention/checkpoint-cell {:sorry {:kind :failed}} [record])
                            [:sorry :job-texts])))))

(deftest recovered-prompt-is-typed-when-agency-only-has-a-preview
  (doseq [trimmed? [false true]]
    (let [state (atom [])
          text (if trimmed? "partial …[trimmed]" "Full recovered prompt")
          opts (retention/wrap-ports
                {} *attempt-dir* state (fn [& _] nil) (fn [& _] nil)
                (fn [& _] {:events [{:type "prompt" :text text}] :result "reply"}))]
      ((:read-job-fn opts) opts "recovered")
      (if trimmed?
        (is (= {:status :absent :reason :agency-prompt-truncated} (:prompt (first @state))))
        (assert-text text (:prompt (first @state)))))))

(deftest a-later-compacted-record-cannot-erase-a-retained-reply
  (let [state (atom [])
        opts (retention/wrap-ports
              {} *attempt-dir* state (fn [& _] {:job-id "job"})
              (fn [& _] {:state "done" :result "complete reply"})
              (fn [& _] {:state "done" :events-trimmed :d13/rolling-expiry}))]
    ((:dispatch-fn opts) opts "author" "caller" "mission" "prompt")
    ((:poll-fn opts) opts "job")
    (let [before @state]
      ((:read-job-fn opts) opts "job")
      (is (= (:reply (first before)) (:reply (first @state))))
      (assert-text "complete reply" (:reply (first @state))))))
