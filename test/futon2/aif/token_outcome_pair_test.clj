(ns futon2.aif.token-outcome-pair-test
  "OBS-P acceptance tests. Pinning sources (read-only, parsed with a
   tagged-literal-tolerant EDN reader):

   - machinery-76 attempt-002's live comparison:
     data/wm-full-loop-machinery-76/wm-contract-machinery-76-v1/attempt-002/007-closed.edn
     at [:payload :judgment :route-attestation :token-outcome-comparison]
     (token [T-repair-occ-444fb018… :restoration-accepted],
      :predicted 175/256, :observed true via :check :C4 at resolved sha
     97e17e10f2695c481c55ddaebe9026f2d245663f);
   - machinery-75 attempt-002's absent comparison:
     data/wm-full-loop-machinery-75/wm-contract-machinery-75-v1/attempt-002/007-closed.edn
     (:token-outcome-comparison {:status :absent :reason
     :comparison-not-supplied}, judgment :failure-kind :agent-unavailable)."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.token-outcome-pair :as pair]))

(def repo-root "/home/joe/code/futon2")

(def close-76-002
  (io/file repo-root "data/wm-full-loop-machinery-76/wm-contract-machinery-76-v1/attempt-002/007-closed.edn"))

(def close-75-002
  (io/file repo-root "data/wm-full-loop-machinery-75/wm-contract-machinery-75-v1/attempt-002/007-closed.edn"))

(defn- read-record [f]
  (edn/read-string {:default (fn [_tag value] value)} (slurp f)))

(defn- comparison [close]
  (get-in close [:payload :judgment :route-attestation :token-outcome-comparison]))

(def occurrence
  {:run/id "2026-09-23-1790199409"
   :cohort/id ":wm-contract-machinery-76-v1"
   :attempt/id "attempt-002"
   :transition/id "transition-8de59450-b5ef-4326-bb1a-12d4b97201ae"
   :action/id "action-5c1163d2-3e45-4fea-8919-6e2b41c6acfe"})

(def reviewed-revision "97e17e10f2695c481c55ddaebe9026f2d245663f")

;; ---------------------------------------------------------------------------
;; Acceptance (1): builder pinned from machinery-76 attempt-002's live
;; comparison. The observation leg must carry the live boolean and its
;; check/evidence; with no independent adjudication available the truth
;; leg is the typed absence, and the pair is FIELD-marked ineligible.
;; ---------------------------------------------------------------------------

(deftest pinned-live-comparison-76-002
  (let [cmp (comparison (read-record close-76-002))
        _ (is (= :compared (:status cmp)))
        row (first (:tokens cmp))
        _ (is (= 175/256 (:predicted row)))
        _ (is (true? (:observed row)))
        pairs (pair/pairs-from-comparison
               {:comparison cmp :occurrence occurrence
                :reviewed-revision reviewed-revision})
        p (first pairs)]
    (is (= 1 (count pairs)))
    (is (= (:token row) (:token p)))
    (is (= occurrence (:occurrence p)))
    (is (= reviewed-revision (:reviewed-revision p)))
    (testing "observation leg: the measured boolean, with check + evidence"
      (is (true? (get-in p [:observation :observed])))
      (is (= :C4 (get-in p [:observation :check])))
      (is (= reviewed-revision
             (get-in p [:observation :evidence :resolved-sha]))))
    (testing "truth leg: typed absence, no channel existing today"
      (is (= {:status :missing :reason :no-independent-truth-channel}
             (:truth p))))
    (testing "ineligibility is a field, not a convention"
      (is (false? (:estimable? p)))
      (is (= [:truth-not-a-boolean] (:ineligibility-reasons p))))
    (is (pair/pair-ok? p))))

;; ---------------------------------------------------------------------------
;; Acceptance (2a), OBS-D falsifier 1: machinery-75 attempt-002's absent
;; comparison must produce a typed absence, never a measured false.
;; ---------------------------------------------------------------------------

(deftest absent-comparison-stays-missing-75-002
  (let [cmp (comparison (read-record close-75-002))
        _ (is (= {:status :absent :reason :comparison-not-supplied} cmp))
        pairs (pair/pairs-from-comparison
               {:comparison cmp
                :occurrence {:run/id "2026-09-23-eee9f1be-731f-46c2-941d-11b94d30187e"
                             :attempt/id "attempt-002"}
                :reviewed-revision nil})
        p (first pairs)]
    (is (= 1 (count pairs)))
    (is (= :missing (get-in p [:observation :status])))
    (is (= :comparison-not-supplied (get-in p [:observation :kind])))
    (testing "the missing observation is NOT a boolean, above all not false"
      (is (not (contains? (:observation p) :observed)))
      (is (not (false? (get-in p [:observation :observed])))))
    (is (false? (:estimable? p)))
    (is (= [:observation-not-a-boolean :truth-not-a-boolean]
           (:ineligibility-reasons p)))
    (is (pair/pair-ok? p))))

;; A deliberately wrong builder: coerces missing to false. The real
;; builder's output must differ, and the wrong output must fail pair-ok?
;; — i.e. these tests actually kill the falsifier-shaped builder.
(defn- wrong-builder-missing-as-false [cmp]
  {:schema pair/schema
   :observation {:observed (boolean (get-in cmp [:tokens 0 :observed]))}
   :truth {:status :missing :reason :no-independent-truth-channel}
   :estimable? false})

(deftest wrong-builder-missing-as-false-is-killed
  (let [cmp (comparison (read-record close-75-002))
        wrong (wrong-builder-missing-as-false cmp)]
    (testing "the wrong builder manufactures a measured false"
      (is (false? (get-in wrong [:observation :observed]))))
    (testing "the real builder does not"
      (is (not= :missing (get-in wrong [:observation :status])))
      (is (= :missing
             (get-in (first (pair/pairs-from-comparison
                             {:comparison cmp :occurrence {} :reviewed-revision nil}))
                     [:observation :status]))))))

;; ---------------------------------------------------------------------------
;; Acceptance (2b), OBS-D falsifier 2: a pair whose two legs are the same
;; verdict source must be refused. Two shapes: identical evidence maps,
;; and an accepted-increment verdict offered as the truth.
;; ---------------------------------------------------------------------------

(deftest same-verdict-source-refused
  (let [row {:token ["T" :restoration-accepted]
             :observed true
             :measurement {:result {:check :C4
                                    :evidence {:repo "futon2"
                                               :resolved-sha "abc123"
                                               :path "p" :decl "d"
                                               :file-present true}}}}
        p (pair/build-pair
           {:occurrence occurrence :token ["T" :restoration-accepted]
            :token-row row :reviewed-revision "abc123"
            :truth {:truth true
                    :truth-source :reviewer-adjudication
                    :adjudicator "reviewer-x"
                    :evidence {:repo "futon2"
                               :resolved-sha "abc123"
                               :path "p" :decl "d"
                               :file-present true}}})]
    (is (= :refused (:status p)))
    (is (= :same-verdict-source (:kind p)))
    (is (false? (:estimable? p)))
    (is (= [:same-verdict-source] (:ineligibility-reasons p)))
    (is (pair/pair-ok? p))))

(deftest accepted-increment-refused-from-truth-leg
  (let [row {:token ["T" :restoration-accepted]
             :observed true
             :measurement {:result {:check :C4
                                    :evidence {:resolved-sha "abc123"}}}}
        by-shape (pair/build-pair
                  {:occurrence occurrence :token ["T" :restoration-accepted]
                   :token-row row :reviewed-revision "abc123"
                   :truth {:accepted? true :evidence {:binding {}}}})
        by-source (pair/build-pair
                   {:occurrence occurrence :token ["T" :restoration-accepted]
                    :token-row row :reviewed-revision "abc123"
                    :truth {:truth true
                            :truth-source :accepted-increment-conjunct-b
                            :adjudicator "accepted-increment"
                            :evidence {:resolved-sha "xyz789"}}})]
    (testing "verdict-shaped truth refused"
      (is (= :refused (get-in by-shape [:truth :status])))
      (is (= :verdict-not-a-truth (get-in by-shape [:truth :kind])))
      (is (false? (:estimable? by-shape)))
      (is (pair/pair-ok? by-shape)))
    (testing "accepted-increment-sourced truth refused"
      (is (= :refused (get-in by-source [:truth :status])))
      (is (= :accepted-increment-source-refused
             (get-in by-source [:truth :reason]))))
    (is (pair/pair-ok? by-source))))

;; ---------------------------------------------------------------------------
;; Acceptance (3): a pair with any typed-absent leg is ineligible by field.
;; And the positive control: a genuinely independent two-legged pair IS
;; estimable and hash-consistent.
;; ---------------------------------------------------------------------------

(deftest eligibility-field-and-positive-control
  (let [row {:token ["T" :restoration-accepted]
             :observed {:status :missing :kind :measurement-unavailable}
             :measurement {:result {:check :C4 :evidence {}}}}
        p (pair/build-pair
           {:occurrence occurrence :token ["T" :restoration-accepted]
            :token-row row :reviewed-revision "abc123"
            :truth {:truth true :truth-source :reviewer-adjudication
                    :adjudicator "reviewer-x"
                    :evidence {:resolved-sha "abc123" :via :ticket-review}}})]
    (is (false? (:estimable? p)))
    (is (= [:observation-not-a-boolean] (:ineligibility-reasons p)))
    (is (pair/pair-ok? p)))
  (let [row {:token ["T" :restoration-accepted]
             :observed true
             :measurement {:result {:check :C4
                                    :evidence {:resolved-sha "abc123"
                                               :path "p" :decl "d"}}}}
        p (pair/build-pair
           {:occurrence occurrence :token ["T" :restoration-accepted]
            :token-row row :reviewed-revision "abc123"
            :truth {:truth true :truth-source :reviewer-adjudication
                    :adjudicator "reviewer-x"
                    :evidence {:resolved-sha "abc123" :via :ticket-review}}})]
    (is (true? (:estimable? p)))
    (is (= [] (:ineligibility-reasons p)))
    (is (string? (:pair-sha256 p)))
    (is (pair/pair-ok? p))))

(deftest revision-mismatch-demotes-to-typed-absence
  (let [row {:token ["T" :restoration-accepted]
             :observed true
             :measurement {:result {:check :C4
                                    :evidence {:resolved-sha "DIFFERENT"}}}}
        p (pair/build-pair
           {:occurrence occurrence :token ["T" :restoration-accepted]
            :token-row row :reviewed-revision "abc123" :truth nil})]
    (is (= :missing (get-in p [:observation :status])))
    (is (= :artifact-revision-mismatch (get-in p [:observation :kind])))
    (is (false? (:estimable? p)))
    (is (pair/pair-ok? p))))
