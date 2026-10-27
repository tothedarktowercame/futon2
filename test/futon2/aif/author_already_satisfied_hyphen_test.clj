(ns futon2.aif.author-already-satisfied-hyphen-test
  "Run 2026-10-27-8c8ee35f (self-heal child of click wm-click-126261d1):
  the author followed the author prompt's own token, wrote the card as
  {:built \"already-satisfied-at-base-head\"} with :proof-ref naming the
  existing futon5a commit 09a7cb8d, and the classifier closed
  :invalid-author-refusal because it accepted only whitespace after
  \"already\".  The reply below is that author's reply, trimmed."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]))

(def base-sha "09a7cb8d3468031ed3fa2beb6db21c602617a572")

(def run-8c8ee35f-reply
  (str "All twelve completion checks are header/cross-ref facts of the "
       "mission file itself, and each is satisfied verbatim at base HEAD `"
       base-sha "`.\n\n"
       "FULL_LOOP_FEATURE_CARD: {:built \"already-satisfied-at-base-head\" "
       ":want-coverage \"all 12 cascade checks hold in mission header at 09a7cb8d\" "
       ":matches-intent? true "
       ":things-to-try [\"sed -n 1,16p holes/missions/M-recommendation-bindings.md\"] "
       ":proof-ref \"" base-sha "\"}\n\n"
       "FULL_LOOP_AUTHOR: REFUSE :already-satisfied-at-base-head"))

(deftest hyphenated-already-satisfied-card-with-existing-proof-ref-closes-already-satisfied
  (is (= {:outcome :already-satisfied
          :grounded-commit {:repo "futon5a" :sha base-sha}}
         (runner/author-refusal-classification
          {:result run-8c8ee35f-reply :artifact-ref base-sha}
          "/home/joe/code/futon5a"
          (fn [repo sha] (and (= "/home/joe/code/futon5a" repo)
                              (= base-sha sha)))))))

(deftest hyphenated-card-naming-no-existing-commit-stays-invalid
  (is (= {:outcome :invalid-author-refusal}
         (runner/author-refusal-classification
          {:result run-8c8ee35f-reply :artifact-ref base-sha}
          "/home/joe/code/futon5a"
          (constantly false)))))

(deftest already-without-a-satisfied-word-is-not-an-already-satisfied-card
  (is (= {:outcome :invalid-author-refusal}
         (runner/author-refusal-classification
          {:result (str "FULL_LOOP_FEATURE_CARD: {:built \"already-started\" "
                        ":proof-ref \"" base-sha "\"}\n\n"
                        "FULL_LOOP_AUTHOR: REFUSE :already-started")
           :artifact-ref base-sha}
          "/home/joe/code/futon5a"
          (constantly true)))))
