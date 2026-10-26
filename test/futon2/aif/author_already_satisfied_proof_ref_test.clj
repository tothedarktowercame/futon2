(ns futon2.aif.author-already-satisfied-proof-ref-test
  "R24 / AlreadySatisfiedArtifactRefSeam (Lean 087e5f4985): Agency may
  extract the first ref-like prose token as :artifact-ref.  When that token is
  not a commit, an already-satisfied card's sha-shaped :proof-ref is the
  grounded commit.  The retained click-3 reply below has sha256
  af4cc4f5ba4d2f901edaef96d0559b9b6749207f20f8328c4e87aaba409ee413."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]))

(def futon5a-root "/home/joe/code/futon5a")
(def proof-sha "09a7cb8d3468031ed3fa2beb6db21c602617a572")

(def retained-author-reply
  (str "Inspection result: the selected construction's twelve acceptance criteria are all mechanically satisfied at the base head — they are quoted spans of the mission record's title/status/owner/cross-ref block, every one present verbatim in `holes/missions/M-recommendation-bindings.md` at HEAD `09a7cb8d` (grep-verified all 12), and the operational produces were already landed by that very commit (`:binds-to` populated: 14 occurrences in `THE-STACK.aif.edn`, 5 in `leaf-invariants.aif.edn`; `scripts/check_bindings.clj` added). Working tree is clean, HEAD equals the stated base head, mission sha256 matches the mission record (`4aae9e60…`). Per requirement 7: no cosmetic commit, no re-presentation of base HEAD as a new DONE artifact.\n\n"
       "FULL_LOOP_FEATURE_CARD: {:built \"all 12 cascade completion checks already satisfied at base head 09a7cb8d\" :want-coverage \"each check names a mission-record span present verbatim at HEAD; :binds-to landed (14+5)\" :matches-intent? true :things-to-try [\"grep -c :binds-to holes/stories/THE-STACK.aif.edn -> 14\"] :proof-ref \"09a7cb8d3468031ed3fa2beb6db21c602617a572\"}\n\n"
       "Validations performed:\n"
       "- `git rev-parse HEAD` = `09a7cb8d3468031ed3fa2beb6db21c602617a572` (equals target base head)\n"
       "- `sha256sum holes/missions/M-recommendation-bindings.md` = `4aae9e60e48a25cd5134250c6fb99580038fc8cca83a8c1e290a13cf6111ff80` (matches mission record)\n"
       "- All 12 completion-check quotings grep-verified present in the mission record at HEAD (title, status, owner, and each of the nine cross-ref lines)\n"
       "- `grep -c :binds-to` → `THE-STACK.aif.edn` 14, `leaf-invariants.aif.edn` 5; `wc -l` leaf-invariants 224 lines\n"
       "- `git status --short` clean (no unrelated dirty files; no commit made)\n\n"
       "Patterns enacted (already in force at base head, not newly by this attempt): `contracts/resolve-pointers-at-head` (steps 0/5/7), `futon-theory/mission-lifecycle` (1), `agent/handoff-preserves-context` (2), `agent/evidence-over-assertion` (3/4/6/8/12), `measurement/warrant-travels-with-the-number` (10/11) — the compiled cascade f1bd2242…/13c0068d… whose clauses I authored under requisition 1899a1d3.\n\n"
       "This reports only the selected construction's existing evidence; the mission itself remains open (7 retained open holes: `:serves-spine` annotations, web-surface live counts, the \"Close S6\" demo, criteria 8–11 checks, and three DERIVE-lifecycle holes) — the runtime `:not-executed` effect and those holes are not claimed closed.\n\n"
       "FULL_LOOP_AUTHOR: REFUSE :already-satisfied-at-base-head"))

(defn- job [& {:as extra}]
  (merge {:result retained-author-reply
          :artifact-ref "/missions/M-recommendation-bindings.md"}
         extra))

(defn- known-proof? [repo sha]
  (and (= futon5a-root repo) (= proof-sha sha)))

(defn- result-data [f]
  (try
    (f)
    nil
    (catch clojure.lang.ExceptionInfo e
      (ex-data e))))

(deftest path-artifact-ref-falls-back-to-the-card-proof-ref
  (let [author-job (job)
        expected {:outcome :already-satisfied
                  :grounded-commit {:repo "futon5a" :sha proof-sha}}]
    (is (= expected
           (runner/author-refusal-classification author-job futon5a-root known-proof?)))
    (let [data (result-data #(runner/throw-if-author-refused!
                              (assoc author-job :artifact-binding {:repo futon5a-root})
                              "M-recommendation-bindings" :author-wait))]
      (is (= :already-satisfied (:outcome data)))
      (is (= :already-satisfied (:failure-kind data)))
      (is (= (:grounded-commit expected) (:grounded-commit data))))))

(deftest extracted-commit-sha-remains-the-first-choice
  (is (= {:outcome :already-satisfied
          :grounded-commit {:repo "futon5a" :sha proof-sha}}
         (runner/author-refusal-classification
          (job :artifact-ref proof-sha) futon5a-root known-proof?))))

(deftest unknown-card-proof-ref-remains-invalid
  (let [unknown "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"]
    (is (= {:outcome :invalid-author-refusal}
           (runner/author-refusal-classification
            (job :result (str/replace retained-author-reply proof-sha unknown))
            futon5a-root known-proof?)))))

(deftest an-extracted-unknown-sha-does-not-fall-through-to-proof-ref
  (is (= {:outcome :invalid-author-refusal}
         (runner/author-refusal-classification
          (job :artifact-ref "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")
          futon5a-root known-proof?))))

(deftest non-already-card-does-not-enter-the-proof-ref-branch
  (let [reply (str "FULL_LOOP_FEATURE_CARD: {:built \"work remains\" "
                   ":want-coverage \"pending evidence\" :matches-intent? true "
                   ":things-to-try [\"inspect -> observation\"] :proof-ref \"" proof-sha "\"}\n"
                   "FULL_LOOP_AUTHOR: REFUSE out of scope\n")]
    (is (= {:outcome :guardrail-refusal :refusal-reason "out of scope"}
           (runner/author-refusal-classification
            {:result reply} futon5a-root known-proof?)))))

(deftest commit-existence-distinguishes-the-real-commit-from-the-path
  (let [exists-in? @#'runner/commit-exists-in?]
    (is (true? (exists-in? futon5a-root proof-sha)))
    (is (false? (exists-in? futon5a-root "/missions/M-recommendation-bindings.md")))))
