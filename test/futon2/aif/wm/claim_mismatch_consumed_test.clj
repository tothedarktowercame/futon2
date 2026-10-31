(ns futon2.aif.wm.claim-mismatch-consumed-test
  "Row CLAIM-MATCHES-FALSE-UNCONSUMED: validate-product computes
  :claim-matches? and :author-response-defects, and nothing consumed them.
  On run ebbbe42a a retained :author-response/decisions-mismatch grounded
  anyway; on run adc46631 the same divergence cost a full reviewer turn
  before the reviewer refused it by hand.

  Fixed behavior under test, from the real retained artifacts of both runs:

  - a claim/source mismatch goes back to the author through a correction
    round BEFORE the reviewer is contacted, once;
  - a corrected claim proceeds clean (no reviewer-visible defect);
  - an unresolved mismatch reaches the reviewer stated as a defect the
    reviewer must acknowledge (acknowledged-defects in the reply), and the
    acknowledgment is retained in the delivery record;
  - acceptance that does not acknowledge every defect kind is a typed
    close (:open-cascade-review-defect-unacknowledged), not a ground."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [cheshire.core :as json]
            [futon2.aif.handoff-contract :as handoff]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.wm.cascade-patch-delivery :as delivery]
            [futon2.aif.wm.open-cascade-resume :as resume]
            [futon2.aif.wm.authored.req-f7374bcce :as adc46631-patch]
            [futon2.aif.wm.authored.req-6a8fe374 :as ebbbe42a-patch]
            [futon2.test-support.runner-fixture :as fixture]))

(use-fixtures :once fixture/with-hermetic-traces hermetic/with-hermetic-stores)

;; ---------------------------------------------------------------------------
;; Real run adc46631 (requisition f7374bcce…, cascade 8d37d377…): the author
;; answer retained in test/resources/m2c/author-reply.txt claims decisions
;; that diverge from the committed source req_f7374bcce.clj; the reviewer of
;; record caught that divergence by hand and refused.
;; ---------------------------------------------------------------------------
(def adc46631-requisition
  (edn/read-string (slurp "test/resources/m2c/requisition.edn")))
(def adc46631-digest (:cascade-digest adc46631-requisition))
(def adc46631-answer
  (handoff/response-json-object (slurp "test/resources/m2c/author-reply.txt")))
;; The adc46631-era pinned source bytes (digest daf082eb…, commit fc7356769)
;; are no longer present in any worktree — req_f7374bcce.clj evolved after
;; that run — so the retained descriptor no longer passes check-delivery
;; today. The DECISIONS CLAIM (the artifact this row is about, and the thing
;; the reviewer of record caught) is kept verbatim; only the digest pin is
;; repointed at the current canonical bytes of the same function, so the
;; delivery machinery still runs for real.
(def adc46631-patch-now
  {:namespace "futon2.aif.wm.authored.req-f7374bcce" :var "decisions"
   :source-digest "43dfe3012e99cc07bdadc0c3b0b0a5810b3d7c7414fc8506fca00dd92a910eda"
   :commit "8d0e24dca7a305eb8a34652d4898daba910584e6"})
(defn adc46631-bound [rid]
  (-> adc46631-answer
      (assoc :requisition-id rid :patch adc46631-patch-now)))
(defn adc46631-corrected [rid]
  {:requisition-id rid
   :cascade-digest adc46631-digest :by (:by adc46631-answer)
   :patch adc46631-patch-now
   :decisions (mapv (fn [[id clause]] (assoc clause :hole id))
                    (adc46631-patch/decisions {:cascade-digest adc46631-digest
                                               :phase :compile}))})

(defn adc46631-held []
  {:kind :wm/open-cascade-execution-error :cascade-digest adc46631-digest
   :phase :compile :holes (:holes adc46631-requisition)
   :resume-port {:retry (fn [ctx] {:status :cascade-ready
                                   :cascade-digest adc46631-digest
                                   :delivered (:authoring-product ctx)})}})

;; ---------------------------------------------------------------------------
;; Real run ebbbe42a (requisition 6a8fe374…, cascade 5a0fa2ad…): the retained
;; run record carries :author-response/decisions-mismatch with
;; :claim-matches? false — the author claimed completion-check [\"checked\"]
;; for every hole while the committed function req_6a8fe374.clj returns the
;; full two-element completion-checks. The claim below reproduces exactly
;; that retained claim (same operations, consumes and produces;
;; completion-check abbreviated to [\"checked\"]).
;; ---------------------------------------------------------------------------
(def ebbbe42a-digest "5a0fa2adeb101b6bc644bf26511b22f5b88463ac1ef0490e941fd24b22de6995")
(def ebbbe42a-patch {:namespace "futon2.aif.wm.authored.req-6a8fe374"
                     :var "decisions"
                     :source-digest "ac1e6f7b705b1d98f1d8272ce5e2409a7385f8b251c1365226cb33fc700da5a3"
                     :commit "4560d631b443d51e385c1bd32fb5d5aff247c2a2"})
(def ebbbe42a-requisition-id
  "6a8fe374958818d6246ee46acc6dbdb851c7cdf66930626395572519879d195c")
(defn ebbbe42a-claim []
  (mapv (fn [[id clause]]
          (assoc clause :hole id :completion-check ["checked"]))
        (ebbbe42a-patch/decisions {:cascade-digest ebbbe42a-digest :phase :compile})))
(defn ebbbe42a-corrected []
  (mapv (fn [[id clause]] (assoc clause :hole id))
        (ebbbe42a-patch/decisions {:cascade-digest ebbbe42a-digest :phase :compile})))
(defn ebbbe42a-answer [claim]
  {:requisition-id ebbbe42a-requisition-id :cascade-digest ebbbe42a-digest
   :by "zai-2" :patch ebbbe42a-patch :decisions claim})
(defn ebbbe42a-held []
  {:kind :wm/open-cascade-execution-error :cascade-digest ebbbe42a-digest
   :phase :compile
   :holes (mapv (fn [id] {:hole-id id :owner :wm/enactment-compiler
                          :question :operational-clause-owed})
                (keys ebbbe42a-patch/clauses))
   :resume-port {:retry (fn [ctx] {:status :cascade-ready
                                   :cascade-digest ebbbe42a-digest
                                   :delivered (:authoring-product ctx)})}})

(defn worktree-delivery []
  (let [check delivery/check-delivery]
    (fn [a] (check delivery/canonical-repository
                   #(-> (io/file delivery/canonical-repository "src" %) .toURI .toURL)
                   a))))

(defn run-handler
  "Drive open-cascade-resume-handler with scripted contacts. The author fn
  receives each request (requisition, and the correction round's defects);
  the reviewer fn receives the review request. Returns [result contacts
  reviews]."
  [held author-fn reviewer-fn]
  (let [contacts (atom []) reviews (atom [])
        handler (runtime/open-cascade-resume-handler
                 {:author "zai-2" :reviewer "wm-reviewer"
                  :open-cascade-author-answer-fn
                  (fn [req] (let [r (author-fn req)] (swap! contacts conj req) r))
                  :open-cascade-reviewer-answer-fn
                  (fn [req] (swap! reviews conj req) (reviewer-fn req))})]
    [(with-redefs [delivery/check-delivery (worktree-delivery)]
       (handler held))
     @contacts @reviews]))

(deftest real-answers-mismatch-is-computed-not-consumed
  ;; The bad case, on the real retained inputs: both runs' author claims
  ;; diverge from the committed sources, and validate-product reports it
  ;; while still accepting — the field nothing read.
  (let [req (resume/authoring-requisition (resume/recognize (ebbbe42a-held))
                                          "zai-2" "wm-reviewer")
        product (ebbbe42a-patch/decisions {:cascade-digest ebbbe42a-digest
                                           :phase :compile})]
    (is (map? (get adc46631-answer :decisions)))
    (let [rq (assoc req :holes (:holes adc46631-requisition))
          product (adc46631-patch/decisions {:cascade-digest adc46631-digest
                                              :phase :compile})
          bound (adc46631-bound (:requisition-id req))]
      (is (= :accepted (:status (resume/validate-product rq bound product))))
      (is (= false (-> (resume/validate-product rq bound product)
                       :answer-delivery :claim-matches?))))
    (is (= false (-> (resume/validate-product
                      (assoc req :holes (:holes adc46631-requisition))
                      adc46631-answer
                      (adc46631-patch/decisions
                       {:cascade-digest adc46631-digest :phase :compile}))
                     :answer-delivery :claim-matches?)))
    (is (= false (-> (resume/validate-product req (ebbbe42a-answer (ebbbe42a-claim)) product)
                     :answer-delivery :claim-matches?)))
    (is (= :author-response/decisions-mismatch
           (get-in (resume/validate-product req (ebbbe42a-answer (ebbbe42a-claim)) product)
                   [:answer-delivery :author-response-defects 0 :kind])))))


(deftest adc46631-mismatch-corrects-once-before-reviewer
  ;; The reviewer of adc46631 must NOT be spent on a correctable claim
  ;; divergence: the author gets one correction round first, and only the
  ;; corrected answer reaches the reviewer.
  (let [[result contacts reviews]
        (run-handler (adc46631-held)
                     (fn [req]
                       (let [rid (or (:requisition-id req)
                                     (:requisition-id (:requisition req)))]
                         (if (:defects req)
                           (let [corrected (adc46631-corrected rid)]
                             {:state "done" :job-id "claim-correction"
                              :result (json/generate-string corrected)})
                           {:state "done" :job-id "author-1"
                            :result (json/generate-string
                                     (adc46631-bound rid))})))
                     (fn [{:keys [author-answer]}]
                       (assoc author-answer :accepted-by "wm-reviewer")))]
    ;; after the correction round the delivery the machine consumes is clean
    (is (true? (get-in result [:answer-delivery :claim-matches?])))
    (is (= 2 (count contacts)))
    (is (= :author-response/decisions-mismatch
           (get-in contacts [1 :defects 0 :kind])))
    (is (contains? (contacts 1) :delivered-decisions))
    (is (= 1 (count reviews)))
    ;; reviewer sees the corrected answer, clean, with no stated defect
    (is (not (contains? (reviews 0) :claim-defects)))
    ;; the corrected claim travels as JSON, so compare in the transport's
    ;; value domain
    (is (= (json/parse-string
            (json/generate-string
             (mapv (fn [[id clause]] (assoc clause :hole id))
                   (adc46631-patch/decisions {:cascade-digest adc46631-digest
                                              :phase :compile}))))
           (json/parse-string
            (json/generate-string (:decisions (:author-answer (reviews 0)))))))
    (is (= :resumed (:status result)))
    (is (empty? (get-in result [:answer-delivery :author-response-defects])))
    (is (= :resolved (get-in result [:answer-delivery :claim-correction :outcome])))
    (is (= 1 (get-in result [:answer-delivery :claim-correction :round])))))

(deftest ebbbe42a-unresolved-mismatch-is-stated-to-the-reviewer
  ;; The exact retained ebbbe42a claim: the author will not correct it, so
  ;; the reviewer is contacted with the defect stated, must acknowledge it,
  ;; and the acknowledgment is retained in the delivery record while the run
  ;; still grounds.
  (let [[result contacts reviews]
        (run-handler (ebbbe42a-held)
                     (fn [req]
                       (let [rid (or (:requisition-id req)
                                     (:requisition-id (:requisition req)))]
                         {:state "done" :job-id "author-ebbbe42a"
                          :result (json/generate-string
                                   (assoc (ebbbe42a-answer (ebbbe42a-claim))
                                          :requisition-id rid))}))
                     (fn [{:keys [author-answer]}]
                       (assoc author-answer :accepted-by "wm-reviewer"
                              :acknowledged-defects
                              ["author-response/decisions-mismatch"])))]
    (is (= 2 (count contacts))) ;; requisition + the one correction round
    (is (= 1 (count reviews)))
    (is (= :author-response/decisions-mismatch
           (get-in reviews [0 :claim-defects 0 :kind])))
    (is (.contains (#'runtime/open-cascade-review-prompt (reviews 0))
                   "author-response/decisions-mismatch"))
    (is (.contains (#'runtime/open-cascade-review-prompt (reviews 0))
                   "acknowledged-defects"))
    (is (= :resumed (:status result)))
    (is (= false (get-in result [:result :answer-delivery :claim-matches?])))
    (is (= [:author-response/decisions-mismatch]
           (get-in result [:result :answer-delivery
                           :claim-defects-acknowledgment :acknowledged])))
    (is (= "wm-reviewer"
           (get-in result [:result :answer-delivery
                           :claim-defects-acknowledgment :acknowledged-by])))
    (is (= :unresolved (get-in result [:result :answer-delivery :claim-correction :outcome])))))

(deftest unacknowledged-defect-close-is-typed
  ;; A reviewer that accepts without acknowledging the stated defect kinds
  ;; does not produce a valid ground: typed close, everything retained.
  (let [[result _ _]
        (run-handler (ebbbe42a-held)
                     (fn [req]
                       (let [rid (or (:requisition-id req)
                                     (:requisition-id (:requisition req)))]
                         {:state "done" :job-id "author-ebbbe42a"
                          :result (json/generate-string
                                   (assoc (ebbbe42a-answer (ebbbe42a-claim))
                                          :requisition-id rid))}))
                     (fn [{:keys [author-answer]}]
                       (assoc author-answer :accepted-by "wm-reviewer")))]
    (is (= :error (:status result)))
    (is (= :open-cascade-review-defect-unacknowledged (:kind result)))
    (is (= [:author-response/decisions-mismatch] (:unacknowledged result)))
    (is (= "wm-reviewer" (:reviewer result)))))
