(ns futon2.aif.job-text-retention-runner-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runner-test :as fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]
           [java.security MessageDigest]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)
(use-fixtures :each (fn [f] (binding [runner/*wm-status-reporting?* false] (f))))

(deftest runner-checkpoints-reference-author-and-reviewer-texts
  ;; Run on the canonical checkout after merge. Do not stub source authority.
  (let [root (.toFile (Files/createTempDirectory "runner-job-texts-" (make-array FileAttribute 0)))
        prompts (atom {})]
    (try
      (let [cohort (#'fixture/retention-cohort "job-text-retention")
            reviewer-text (str "FULL_LOOP_REVIEW: APPROVE\n"
                               "FULL_LOOP_REVIEWER_NOTE: Replay steps verified.")
            {:keys [result artifact-sha]}
            (#'fixture/run-token-feature-card-attempt
             cohort (#'fixture/token-loop-exercised-decision)
             {:author-result (fn [sha] (str "FULL_LOOP_AUTHOR: DONE " sha))
              :reviewer-result reviewer-text
              :dispatch-observer
              (fn [agent prompt]
                (swap! prompts assoc
                       (if (= agent "zai-5") "feature-author" "feature-review")
                       prompt))
              :runner-options
              {:run-record-dir (.getPath root) :run-id "text-retention"}})
            replies {"feature-author" (str "FULL_LOOP_AUTHOR: DONE "
                                            artifact-sha)
                     "feature-review" reviewer-text}
            dispatch (get-in result [:checkpoints :dispatch :judgment :job-texts])
            build (get-in result [:checkpoints :build :judgment :job-texts])
            attempt-dir (io/file (:root cohort) "test-cohort-exhaustion"
                                 (:attempt-id result) "retained")]
        (is (= :grounded-progress (:outcome result)))
        (is (= :verified
               (get-in result [:checkpoints :build :judgment :validation
                               :reviewer-falsifier-verification :status])))
        (is (= 1 (count dispatch)))
        (is (= #{"feature-author" "feature-review"} (set (map :job-id build))))
        (is (string? (get-in dispatch [0 :prompt :sha256])))
        (is (= :absent (get-in dispatch [0 :reply :status])))
        (doseq [record build field [:prompt :reply]]
          (let [ref (get record field) file (io/file (:path ref))]
            (is (= (.getCanonicalPath attempt-dir) (.getCanonicalPath (.getParentFile file))))
            (is (= (get (if (= field :prompt) @prompts replies) (:job-id record))
                   (slurp file :encoding "UTF-8")))
            (is (= (:sha256 ref)
                   (.formatHex (java.util.HexFormat/of)
                               (.digest (MessageDigest/getInstance "SHA-256")
                                        (Files/readAllBytes (.toPath file)))))))))
      (finally
        (doseq [file (reverse (file-seq root))] (io/delete-file file true))))))
