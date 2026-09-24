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

(def live-judgment
  (delay (get-in (read-record close-76-002) [:payload :judgment])))

(deftest kernel-example-complete-live-observation-population
  (let [j @live-judgment
        k (:kernel-example j)
        ps (pair/pairs-from-kernel-example {:kernel-example k :occurrence (:occurrence j)})
        source (:observation-source k)
        bytes (java.nio.file.Files/readAllBytes (.toPath (io/file (:path source))))
        raw-hash (format "%064x" (java.math.BigInteger. 1
                                  (.digest (java.security.MessageDigest/getInstance "SHA-256") bytes)))]
    (is (= (:sha256 source) raw-hash) "verify the real source file, not its hash string alone")
    (is (= 1 (count (:tokens k))))
    (is (= 6 (count ps)) "the full projection includes five non-wanted tokens")
    (is (= (set (keys (get-in k [:observation-projection :observations])))
           (set (map :token ps))))
    (doseq [p ps]
      (is (true? (get-in p [:observation :observed])))
      (is (= :C4 (get-in p [:observation :check])))
      (is (= source (get-in p [:observation :source])))
      (is (= :kernel-example (get-in p [:observation :observation-source])))
      (is (= (get-in k [:observation-projection :revision-pair]) (:revision-pair p)))
      (is (= {:status :missing :reason :no-independent-truth-channel} (:truth p)))
      (is (false? (:estimable? p)))
      (is (pair/pair-ok? p)))))

(deftest learning-receipt-and-trials-refused-from-both-legs
  ;; Exact bad input from the real close, including the counted B trial.
  (let [receipt (:learning-trial-receipt @live-judgment)]
    (is (= :wm/learning-trial-receipt-v2 (:schema receipt)))
    (is (some :counted? (:trials receipt)))
    (doseq [x (conj (:trials receipt) receipt)
            supplied [x (assoc x :observed true :truth true)]]
      (let [p (pair/build-pair {:occurrence occurrence :token ["T" :done]
                                :reviewed-revision reviewed-revision
                                :token-row supplied :truth supplied})]
        (doseq [leg [:observation :truth]]
          (is (= :refused (get-in p [leg :status])))
          (is (= :learning-trial-receipt-not-a-leg (get-in p [leg :reason]))))
        (is (false? (:estimable? p)))
        (is (pair/pair-ok? p))))))

(deftest kernel-missingness-and-join-failures-stay-typed
  (let [j @live-judgment k (:kernel-example j)
        token (first (keys (get-in k [:observation-projection :observations])))
        input {:occurrence (:occurrence j) :token token :kernel-example k}]
    (doseq [[changed reason]
            [[(assoc input :occurrence (assoc (:occurrence j) :run/id "other")) :occurrence-mismatch]
             [(assoc input :reviewed-revision "other") :artifact-revision-mismatch]
             [(update input :kernel-example dissoc :observation-source) :observation-source-not-recorded]
             [(assoc-in input [:kernel-example :observation-projection :observations token
                               :artifact-observation :observed] false) :observation-evidence-mismatch]]]
      (let [p (pair/build-pair changed)]
        (is (= :missing (get-in p [:observation :status])))
        (is (= reason (get-in p [:observation :reason])))
        (is (not (contains? (:observation p) :observed)))
        (is (false? (:estimable? p)))
        (is (pair/pair-ok? p)))))
  (let [j (get-in (read-record (io/file repo-root
                   "data/wm-full-loop-machinery-70/wm-contract-machinery-70-v1/attempt-001/007-closed.edn"))
                  [:payload :judgment])
        ps (pair/pairs-from-kernel-example {:occurrence (:occurrence j) :kernel-example (:kernel-example j)})]
    (is (= 2 (count ps)))
    (doseq [p ps]
      (is (= :missing (get-in p [:observation :status])))
      (is (= :task-execution-incomplete (get-in p [:observation :reason])))
      (is (not (contains? (:observation p) :observed)))
      (is (pair/pair-ok? p)))))

(deftest accepted-increment-refused-from-observation-leg
  (let [verdict (:accepted-increment @live-judgment)
        p (pair/build-pair {:occurrence occurrence :token ["T" :done]
                            :token-row (assoc verdict :observed true)})]
    (is (contains? verdict :accepted?))
    (is (= :refused (get-in p [:observation :status])))
    (is (= :accepted-increment-source-refused (get-in p [:observation :reason])))
    (is (pair/pair-ok? p))))

(deftest cert-s-canonical-bytes-preserve-types-and-numeric-values
  (let [x (with-meta (array-map :z #{:b :a} :r 2/3 :d 0.5 :v [:b :a]) {:ignored true})]
    (is (= "{:d #wm/double \"0x1.0p-1\" :r 2/3 :v [:b :a] :z #{:a :b}}"
           (binding [*print-meta* true *print-length* 1 *print-level* 1]
             (pair/canonical-edn x))))
    (is (= (pair/canonical-edn x)
           (pair/canonical-edn (into {} (reverse x))))))
  (is (not= (pair/canonical-edn #{:a :b}) (pair/canonical-edn [:a :b])))
  (is (= "{#{:a :b} 1 #{:c :d} 2}"
         (pair/canonical-edn {#{:d :c} 2 #{:b :a} 1}))))

(deftest pair-hash-is-extracted-value-not-record-identity
  (let [p (first (pair/pairs-from-kernel-example
                  {:kernel-example (:kernel-example @live-judgment)
                   :occurrence (:occurrence @live-judgment)}))
        digest (:pair-sha256 p)]
    (is (= 2 (:schema-version p)))
    (is (= :wm/token-outcome-pair-value-v2 (:pair-hash-domain p)))
    (is (= #{:occurrence :token :revision-pair :observation :truth}
           (set (keys (pair/pair-value p)))))
    (is (= digest (pair/pair-digest (assoc p :estimable? true :schema :other
                                           :pair-sha256 "self-attested"))))
    (is (= digest (pair/pair-digest (assoc-in p [:occurrence :action/value] {:ignored true}))))
    (doseq [changed [(assoc-in p [:observation :observed] false)
                     (assoc p :truth {:truth false :truth-source :reviewer-adjudication})
                     (assoc-in p [:occurrence :run/id] "other")
                     (assoc-in p [:revision-pair :before] "other")
                     (assoc-in p [:revision-pair :after] "other")
                     (assoc-in p [:observation :source :sha256] "other")]]
      (is (not= digest (pair/pair-digest changed)))
      (is (not (pair/pair-ok? changed))))
    (is (not (pair/pair-ok? (assoc p :pair-sha256 "self-attested")))))
  (let [p (pair/build-pair {:occurrence occurrence :token ["T" :done]
                            :reviewed-revision reviewed-revision})]
    (is (= {:status :missing :reason :before-revision-not-recorded}
           (get-in p [:revision-pair :before])))
    (is (pair/pair-ok? p))))
