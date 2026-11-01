(ns futon2.aif.author-refusal-prose-ref-test
  "L5-25 (click wm-click-8ed79a0b, run 2026-11-01-0e6ff636): the author's
  typed REFUSE named the futon7 base HEAD it had inspected, the Agency
  extracted that sha as the job's :artifact-ref, and the guardrail branch of
  author-refusal-classification read the extracted ref as an artifact claim
  and closed :invalid-author-refusal (ledger entry 152, injury d984347f).
  The repaired branch reads the author's typed claim, the card's :proof-ref;
  prose refs do not condemn a refusal.  Lean:
  DarkTower.WarMachine.OrdinaryRefusalProseRef (mathlib4 darktower 8851fb083a).

  The fixture is the retained author job verbatim (the repair finding's
  [:failure-data :author-job], job invoke-1793571759556-9840-92c617ea),
  pinned by sha256; the run record itself omits :result."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.load-identity :as identity]))

(def fixture-path "test/resources/wm/injury-d984347f/author-job.edn")
(def fixture-sha256 "d7c1500005d4c5a9bf165d96c61ff87dd9747232e4eb564467261464ab0692ca")
(def base-head "51af00b44628945adca1fb15f97c92ca878c279d")
(def futon7-root "/home/joe/code/futon7")

(defn- retained-job []
  (let [bytes (java.nio.file.Files/readAllBytes (.toPath (io/file fixture-path)))]
    (is (= fixture-sha256 (identity/sha256 bytes)) "fixture pin")
    (edn/read-string (String. bytes "UTF-8"))))

(deftest click-2-refusal-naming-the-inspected-base-head-is-a-guardrail-refusal
  (let [job (retained-job)
        result (runner/author-refusal-classification job futon7-root)]
    (testing "the retained job is the witness: typed REFUSE, no card :proof-ref, sha in prose"
      (is (= base-head (:artifact-ref job)))
      (is (str/includes? (:result job) "FULL_LOOP_AUTHOR: REFUSE :wants-operator-gated-no-honest-parcel"))
      (is (not (str/includes? (:result job) ":proof-ref"))))
    (is (= :guardrail-refusal (:outcome result)))
    (is (str/starts-with? (:refusal-reason result) ":wants-operator-gated-no-honest-parcel"))))

(deftest the-same-answer-without-the-prose-sha-classifies-the-same
  ;; control: mention alone must not change the close
  (let [job (retained-job)
        quiet (-> job
                  (update :result str/replace base-head "the base HEAD")
                  (dissoc :artifact-ref))]
    (is (= (runner/author-refusal-classification quiet futon7-root)
           (runner/author-refusal-classification job futon7-root)))))

(deftest a-refusal-whose-card-names-a-proof-ref-is-still-contradictory
  (let [reply (str "FULL_LOOP_FEATURE_CARD: {:built \"nothing\" :want-coverage \"0/1\" "
                   ":matches-intent? true :proof-ref \"" base-head "\"}\n"
                   "FULL_LOOP_AUTHOR: REFUSE :out-of-scope\n")]
    (is (= {:outcome :invalid-author-refusal}
           (runner/author-refusal-classification
            {:result reply :artifact-ref base-head} futon7-root (constantly true))))))

(deftest a-refusal-without-a-reason-stays-invalid-with-or-without-a-prose-ref
  (is (= {:outcome :invalid-author-refusal}
         (runner/author-refusal-classification
          {:result "FULL_LOOP_AUTHOR: REFUSE\n" :artifact-ref base-head} futon7-root)))
  (is (= {:outcome :invalid-author-refusal}
         (runner/author-refusal-classification
          {:result "FULL_LOOP_AUTHOR: REFUSE\n"} futon7-root))))

(deftest the-already-satisfied-branch-is-untouched
  ;; the seam's branch: an already-satisfied card with an existing commit
  ;; still grounds through the extracted ref
  (is (= {:outcome :already-satisfied :grounded-commit {:repo "futon7" :sha base-head}}
         (runner/author-refusal-classification
          {:result (str "FULL_LOOP_AUTHOR: REFUSE already satisfied\n"
                        "FULL_LOOP_FEATURE_CARD: {:built \"already landed\" :want-coverage \"already true\"}\n")
           :artifact-ref base-head}
          futon7-root (constantly true)))))
