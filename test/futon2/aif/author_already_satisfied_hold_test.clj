(ns futon2.aif.author-already-satisfied-hold-test
  "Click wm-click-1d974b05 (repair child run fd5d1d5f): the author's card
  said the checks \"already hold verbatim\" and named the existing futon5a
  commit 09a7cb8d, and the classifier closed :invalid-author-refusal
  because its completion words had \"holds\" but not \"hold\".  The card
  below is that author's card, trimmed."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]))

(def base-sha "09a7cb8d3468031ed3fa2beb6db21c602617a572")

(def click-1d974b05-reply
  (str "All twelve completion checks are mechanically satisfied by the "
       "mission record at base HEAD.\n\n"
       "FULL_LOOP_FEATURE_CARD: {:built \"nothing new — selected "
       "construction's 12 mission-record completion checks already hold "
       "verbatim at base HEAD\" "
       ":want-coverage \"all 12 :operational-clause-owed checks satisfied "
       "by M-recommendation-bindings.md header at 09a7cb8d\" "
       ":matches-intent? true "
       ":proof-ref \"" base-sha "\"}\n\n"
       "FULL_LOOP_AUTHOR: REFUSE :already-satisfied-at-base-head"))

(deftest already-hold-card-with-existing-proof-ref-closes-already-satisfied
  (is (= {:outcome :already-satisfied
          :grounded-commit {:repo "futon5a" :sha base-sha}}
         (runner/author-refusal-classification
          {:result click-1d974b05-reply :artifact-ref base-sha}
          "/home/joe/code/futon5a"
          (fn [repo sha] (and (= "/home/joe/code/futon5a" repo)
                              (= base-sha sha)))))))

(deftest already-hold-card-naming-no-existing-commit-stays-invalid
  (is (= {:outcome :invalid-author-refusal}
         (runner/author-refusal-classification
          {:result click-1d974b05-reply :artifact-ref base-sha}
          "/home/joe/code/futon5a"
          (constantly false)))))
