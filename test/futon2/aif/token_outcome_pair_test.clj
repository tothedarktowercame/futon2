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
     :comparison-not-supplied}, judgment :failure-kind :agent-unavailable);
   - machinery-76 attempt-002's kernel example (OBS-D Revision 2, 508a410e):
     [:payload :judgment :kernel-example], :wm/aligned-kernel-example-v1,
     :observation-source = futon3c/data/wm-d-task-enactment/
     action-5c1163d2-3e45-4fea-8919-6e2b41c6acfe.edn, raw sha256
     161d0c1c9afa30e927d4d08a110256820ee3563d227aac7b7c38dc1f921cdc66,
     projection :admitted with six C4-true observations, revision pair
     a1957b7c... -> 97e17e10...;
   - the same close's :learning-trial-receipt (:wm/learning-trial-receipt-v2,
     two trials, one :counted?), which must be refused as either leg."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.interpretation-evidence :as evidence]
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

;; Revision 2 §R2.2's concrete 76/002 source, as the close records it.
(def kernel-source
  {:path "/home/joe/code/futon3c/data/wm-d-task-enactment/action-5c1163d2-3e45-4fea-8919-6e2b41c6acfe.edn"
   :sha256 "161d0c1c9afa30e927d4d08a110256820ee3563d227aac7b7c38dc1f921cdc66"})

(def live-revision-pair
  {:before "a1957b7cb871a752927aa98ee2340ed91c64812f"
   :after "97e17e10f2695c481c55ddaebe9026f2d245663f"
   :before-evidence :not-measured})

(def live-occurrence-identity
  {:run/id "2026-09-23-1790199409"
   :cohort/id ":wm-contract-machinery-76-v1"
   :attempt/id "attempt-002"
   :transition/id "transition-8de59450-b5ef-4326-bb1a-12d4b97201ae"
   :action/id "action-5c1163d2-3e45-4fea-8919-6e2b41c6acfe"
   :action/value-sha256 "1a7220e756b4c75f5ed3c9707041eb3d26dacc1256afcd4c8283c557ffe25bbf"})

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
             (get-in p [:observation :evidence :resolved-sha])))
      (is (= (:measurement-source cmp) (get-in p [:observation :source])))
      (is (= kernel-source (get-in p [:observation :source]))
          "the comparison was read from the same enactment file the kernel example was aligned from"))
    (testing "revision pair: only :after known here; :before typed missing"
      (is (= 2 (:schema-version p)))
      (is (= {:before {:status :missing :reason :before-revision-not-recorded}
              :after reviewed-revision}
             (:revision-pair p))))
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

;; ===========================================================================
;; OBS-D Revision 2 amendment (508a410e). Every test below is pinned from
;; machinery-76 attempt-002's live close; each requirement has a
;; deliberately wrong builder whose output pair-ok? rejects, with a
;; comment naming which check does the rejecting.
;; ===========================================================================

(def live-judgment
  (delay (get-in (read-record close-76-002) [:payload :judgment])))

(def target
  "T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade")

;; ---------------------------------------------------------------------------
;; Requirement (1): the kernel example's per-token observations are an
;; admissible observation-leg source, with the source path and sha on
;; the pair.
;; ---------------------------------------------------------------------------

(deftest kernel-example-complete-live-observation-population
  (let [j @live-judgment
        k (:kernel-example j)
        _ (is (= :wm/aligned-kernel-example-v1 (:schema k)))
        _ (is (= :recorded (:status k)))
        _ (is (= kernel-source (:observation-source k)))
        _ (is (= live-revision-pair (get-in k [:observation-projection :revision-pair])))
        _ (is (= live-occurrence-identity (pair/occurrence-identity (:occurrence j))))
        ps (pair/pairs-from-kernel-example {:kernel-example k :occurrence (:occurrence j)})
        source-file (io/file (:path kernel-source))]
    (testing "the recorded sha is the raw hash of the file it names"
      (is (.exists source-file))
      (is (= (:sha256 kernel-source)
             (format "%064x" (java.math.BigInteger.
                              1 (.digest (java.security.MessageDigest/getInstance "SHA-256")
                                         (java.nio.file.Files/readAllBytes (.toPath source-file))))))))
    (is (= 1 (count (:tokens k))) "the wanted projection is one token")
    (is (= 6 (count ps)) "the admitted projection carries six observed tokens")
    (is (= (sorted-set [target :admission/task-stated]
                       [target :repair/calibration-evidence-present]
                       [target :repair/held-out-observations-collected]
                       [target :repair/obstruction-observed-cleared]
                       [target :repair/split-declared-valid]
                       [target :restoration-accepted])
           (set (map :token ps))))
    (doseq [p ps]
      (is (true? (get-in p [:observation :observed])))
      (is (= :C4 (get-in p [:observation :check])))
      (is (= reviewed-revision (get-in p [:observation :evidence :resolved-sha])))
      (is (= kernel-source (get-in p [:observation :source])))
      (is (= :kernel-example (get-in p [:observation :observation-source])))
      (is (= [:observation-projection :observations (:token p) :artifact-observation]
             (get-in p [:observation :source-key-path])))
      (is (= live-revision-pair (:revision-pair p)))
      (is (= reviewed-revision (:reviewed-revision p)))
      (is (= live-occurrence-identity (:occurrence (pair/pair-value p))))
      (is (= {:status :missing :reason :no-independent-truth-channel} (:truth p)))
      (is (false? (:estimable? p)))
      (is (= [:truth-not-a-boolean] (:ineligibility-reasons p)))
      (is (pair/pair-ok? p)))))

;; Deliberately wrong builder for (1): cites the kernel row's boolean but
;; drops the raw-file source reference, then hashes honestly. Without the
;; source-ref clause in pair-ok? (a measured :kernel-example leg must carry
;; :source {:path :sha256}) this pair would pass every other invariant.
(defn- wrong-builder-kernel-without-source [p]
  (let [q (update p :observation dissoc :source)]
    (assoc q :pair-sha256 (pair/pair-digest q))))

(deftest wrong-builder-kernel-without-source-is-killed
  (let [p (first (pair/pairs-from-kernel-example
                  {:kernel-example (:kernel-example @live-judgment)
                   :occurrence (:occurrence @live-judgment)}))
        wrong (wrong-builder-kernel-without-source p)]
    (is (true? (get-in wrong [:observation :observed])))
    (is (= :kernel-example (get-in wrong [:observation :observation-source])))
    (is (= (:pair-sha256 wrong) (pair/pair-digest wrong)) "hash is honest; only the source is gone")
    (is (not (pair/pair-ok? wrong)))
    (is (pair/pair-ok? p))))

(deftest kernel-missingness-and-join-failures-stay-typed
  (let [j @live-judgment k (:kernel-example j)
        token [target :admission/task-stated]
        input {:occurrence (:occurrence j) :token token :kernel-example k}]
    (doseq [[changed reason]
            [[(assoc input :occurrence (assoc (:occurrence j) :run/id "other")) :occurrence-mismatch]
             [(assoc input :reviewed-revision "other") :artifact-revision-mismatch]
             [(update input :kernel-example dissoc :observation-source) :observation-source-not-recorded]
             [(assoc-in input [:kernel-example :observation-source :sha256] "not-a-sha") :observation-source-not-recorded]
             [(assoc input :token [target :not-observed]) :token-observation-not-recorded]
             [(assoc-in input [:kernel-example :observation-projection :observations token
                               :artifact-observation :observed] false) :observation-evidence-mismatch]
             [(assoc input :kernel-example nil) :carrier-not-recorded]]]
      (let [p (pair/build-pair (update changed :kernel-example
                                       #(or % {:status :missing :reason :carrier-not-recorded})))]
        (is (= :missing (get-in p [:observation :status])) (pr-str reason))
        (is (= reason (get-in p [:observation :reason])))
        (is (not (contains? (:observation p) :observed)))
        (is (false? (:estimable? p)))
        (is (pair/pair-ok? p)))))
  (testing "machinery-70 attempt-001: projection refused, wanted tokens stay typed-missing"
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
  (testing "machinery-75 attempt-002: carrier nil -> one carrier-level absence, no token"
    (let [ps (pair/pairs-from-kernel-example
              {:occurrence {:run/id "2026-09-23-eee9f1be-731f-46c2-941d-11b94d30187e"
                            :attempt/id "attempt-002"}
               :kernel-example (get-in (read-record close-75-002)
                                       [:payload :judgment :kernel-example])})]
      (is (= 1 (count ps)))
      (is (= :carrier-not-recorded (get-in (first ps) [:observation :reason])))
      (is (= {:status :missing :reason :token-not-recorded} (:token (first ps))))
      (is (pair/pair-ok? (first ps))))))

;; ---------------------------------------------------------------------------
;; Requirement (2): the learning-trial receipt is refused as a leg by an
;; executable check with a reason.
;; ---------------------------------------------------------------------------

(deftest learning-receipt-and-trials-refused-from-both-legs
  ;; Exact bad input from the real close: the receipt itself and each of
  ;; its two trials, including the counted B trial, offered as each leg,
  ;; both bare and with an :observed/:truth boolean spliced on.
  (let [receipt (:learning-trial-receipt @live-judgment)]
    (is (= :wm/learning-trial-receipt-v2 (:schema receipt)))
    (is (= 2 (count (:trials receipt))))
    (is (= 1 (count (filter :counted? (:trials receipt)))))
    (is (every? #(contains? % :trial-grain) (:trials receipt)))
    (doseq [x (conj (:trials receipt) receipt)
            supplied [x (assoc x :observed true :truth true)]]
      (let [p (pair/build-pair {:occurrence occurrence :token [target :restoration-accepted]
                                :reviewed-revision reviewed-revision
                                :token-row supplied :truth supplied})]
        (doseq [leg [:observation :truth]]
          (is (= :refused (get-in p [leg :status])))
          (is (= :learning-trial-receipt-not-a-leg (get-in p [leg :reason])))
          (is (not (contains? (get p leg) :observed)))
          (is (not (contains? (get p leg) :truth))))
        (is (false? (:estimable? p)))
        (is (pair/pair-ok? p))))))

;; Deliberately wrong builder for (2): reads the counted trial's
;; :signed-observation boolean and passes the trial wrapper through as the
;; observation leg. Without learning-trial-carrier? (in observation-leg
;; and in pair-ok?) the wrapper's :observed true is a boolean and the pair
;; reads as a measured observation sourced from the B trial.
(defn- wrong-builder-trial-as-observation [trial]
  (let [q {:schema pair/schema :occurrence occurrence
           :token [target :repair/obstruction-observed-cleared]
           :revision-pair live-revision-pair
           :observation (assoc trial :observed
                               (get-in trial [:signed-observation :artifact-observation :observed]))
           :truth {:status :missing :reason :no-independent-truth-channel}
           :estimable? false :ineligibility-reasons [:truth-not-a-boolean]}]
    (assoc q :pair-sha256 (pair/pair-digest q))))

(deftest wrong-builder-trial-as-observation-is-killed
  (let [trial (first (filter :counted? (get-in @live-judgment [:learning-trial-receipt :trials])))
        wrong (wrong-builder-trial-as-observation trial)]
    (is (true? (get-in wrong [:observation :observed])))
    (is (= (:pair-sha256 wrong) (pair/pair-digest wrong)))
    (is (not (pair/pair-ok? wrong)))))

(deftest accepted-increment-refused-from-observation-leg
  (let [verdict (:accepted-increment @live-judgment)
        p (pair/build-pair {:occurrence occurrence :token [target :restoration-accepted]
                            :token-row (assoc verdict :observed true)})]
    (is (true? (:accepted? verdict)))
    (is (= :refused (get-in p [:observation :status])))
    (is (= :accepted-increment-source-refused (get-in p [:observation :reason])))
    (is (pair/pair-ok? p))))

;; ---------------------------------------------------------------------------
;; Requirement (3): :pair-sha256 is the canonical extracted-value hash
;; over the five pair-value keys (Revision 2 §R2.3), never a record hash.
;; ---------------------------------------------------------------------------

(deftest cert-s-canonical-bytes-preserve-types-and-numeric-values
  (let [x (with-meta (array-map :z #{:b :a} :r 2/3 :d 0.5 :v [:b :a]) {:ignored true})]
    (is (= "{:d #wm/double \"0x1.0p-1\" :r 2/3 :v [:b :a] :z #{:a :b}}"
           (binding [*print-meta* true *print-length* 1 *print-level* 1]
             (pair/canonical-edn x))))
    (is (= (pair/canonical-edn x)
           (pair/canonical-edn (into {} (reverse x))))))
  (is (not= (pair/canonical-edn #{:a :b}) (pair/canonical-edn [:a :b])))
  (is (= "{#{:a :b} 1 #{:c :d} 2}"
         (pair/canonical-edn {#{:d :c} 2 #{:b :a} 1})))
  (is (= "{:s \"a\\\"b\" :t nil}" (pair/canonical-edn {:t nil :s "a\"b"}))))

(deftest pair-hash-is-extracted-value-not-record-identity
  (let [p (first (pair/pairs-from-kernel-example
                  {:kernel-example (:kernel-example @live-judgment)
                   :occurrence (:occurrence @live-judgment)}))
        digest (:pair-sha256 p)]
    (is (= 2 (:schema-version p)))
    (is (= :wm/token-outcome-pair-value-v2 (:pair-hash-domain p)))
    (is (re-matches #"sha256:[0-9a-f]{64}" digest))
    (is (= #{:occurrence :token :revision-pair :observation :truth}
           (set (keys (pair/pair-value p)))))
    (is (= live-occurrence-identity (:occurrence (pair/pair-value p)))
        "the full :action/value payload is projected out")
    (testing "outside the byte domain: eligibility fields, schema, the hash, the action payload"
      (is (= digest (pair/pair-digest (assoc p :estimable? true :schema :other
                                             :reviewed-revision "other"
                                             :pair-sha256 "self-attested"))))
      (is (= digest (pair/pair-digest (assoc-in p [:occurrence :action/value] {:ignored true})))))
    (testing "inside the byte domain: both legs, identity, revisions, the source sha"
      (doseq [changed [(assoc-in p [:observation :observed] false)
                       (assoc p :truth {:truth false :truth-source :reviewer-adjudication})
                       (assoc-in p [:occurrence :run/id] "other")
                       (assoc-in p [:occurrence :action/value-sha256] "other")
                       (assoc p :token [target :other])
                       (assoc-in p [:revision-pair :before] "other")
                       (assoc-in p [:revision-pair :after] "other")
                       (assoc-in p [:observation :source :sha256] "other")]]
        (is (not= digest (pair/pair-digest changed)))
        (is (not (pair/pair-ok? changed)))))
    (is (not (pair/pair-ok? (assoc p :pair-sha256 "self-attested"))))
    (testing "the raw record sha stays a separate identity on the leg"
      (is (= (:sha256 kernel-source) (get-in p [:observation :source :sha256])))
      (is (not= digest (str "sha256:" (:sha256 kernel-source))))))
  (let [p (pair/build-pair {:occurrence occurrence :token [target :restoration-accepted]
                            :reviewed-revision reviewed-revision})]
    (is (= {:status :missing :reason :before-revision-not-recorded}
           (get-in p [:revision-pair :before])))
    (is (pair/pair-ok? p))))

;; Deliberately wrong builder for (3): the OBS-P v1 hash -- value-digest
;; over the whole pair map minus the hash. That is a record hash: it moves
;; when :estimable? or :schema moves and when the :action/value payload
;; moves, so it is neither the Revision 2 byte domain nor stable under
;; the fields Revision 2 excludes. Without pair-ok?'s
;; (= :pair-sha256 (pair-digest pair)) clause it would pass.
(defn- wrong-builder-record-hash [p]
  (assoc p :pair-sha256 (evidence/value-digest (dissoc p :pair-sha256))))

(deftest wrong-builder-record-hash-is-killed
  (let [p (first (pair/pairs-from-kernel-example
                  {:kernel-example (:kernel-example @live-judgment)
                   :occurrence (:occurrence @live-judgment)}))
        wrong (wrong-builder-record-hash p)]
    (is (not= (:pair-sha256 wrong) (:pair-sha256 p)))
    (is (not (pair/pair-ok? wrong)))
    (testing "the record hash moves under a field Revision 2 excludes; the value hash does not"
      (is (not= (:pair-sha256 wrong)
                (:pair-sha256 (wrong-builder-record-hash (assoc p :estimable? true)))))
      (is (= (:pair-sha256 p)
             (:pair-sha256 (pair/build-pair
                            {:kernel-example (:kernel-example @live-judgment)
                             :occurrence (:occurrence @live-judgment)
                             :token (:token p)})))))))
