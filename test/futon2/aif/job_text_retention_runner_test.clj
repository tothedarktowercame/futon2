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
        prompts (atom {})
        replies {"feature-author" "FULL_LOOP_AUTHOR: DONE feature123\n"
                 "feature-review" "FULL_LOOP_REVIEW: APPROVE\nFULL_LOOP_REVIEWER_NOTE: Replay verified.\n"}]
    (try
      (let [{:keys [result]}
            (#'fixture/run-feature-card-attempt
             {:author-card fixture/feature-card-claim
              :runner-options
              {:run-record-dir (.getPath root) :run-id "text-retention"
               :dispatch-fn
               (fn [_ agent _ _ prompt]
                 (let [id (if (= agent "zai-5") "feature-author" "feature-review")]
                   (swap! prompts assoc id prompt)
                   {:job-id id :prompt prompt}))
               :poll-fn
               (fn [_ id]
                 {:job-id id :state "done" :result (get replies id)
                  :feature-card fixture/feature-card-claim :artifact-ref "feature123"
                  :execution fixture/successful-execution})}})
            dispatch (get-in result [:checkpoints :dispatch :judgment :job-texts])
            build (get-in result [:checkpoints :build :judgment :job-texts])
            attempt-dir (io/file root "text-retention" (:attempt-id result))]
        (is (= :grounded-change (:outcome result)))
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
