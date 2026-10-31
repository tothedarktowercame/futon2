(ns futon2.aif.wm.objection-round-test
  "The objection round for an explicit reasoned reviewer refusal
  (REVIEW-REFUSAL-NO-FORMATIVE-LOOP). Every reviewer fixture is taken from
  the real retained reply of job invoke-1793473761172-8882-652e3bf0 (run
  adc46631): prose verification report plus one fenced JSON refusal with
  accepted:false, accepted-by:null and the hole-0 reason. The author answer
  mirrors the real divergence — hole 0's consumes/produces claim diverges
  from the committed source futon2.aif.wm.authored.req-b9e453fd (clauses
  loaded from that real namespace), exactly as the reviewer found."
  (:require [cheshire.core :as json]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.cascade-feedback :as cascade-feedback]
            [futon2.aif.wm.authored.req-b9e453fd :as authored]
            [futon2.aif.wm.cascade-patch-delivery :as delivery]))

(def cascade-digest "f2b33ac45c1c87b240070acf4c6a41e2cc5683a75a1e1522be8b276e7ea49c8c")
(def real-requisition-id "b9e453fdf6d6253fcefa8c686e4ee5387c1e949f3a4c8d8fc42f4bbd1d57d3ad")
(def real-source-digest "7f241c3614a71a0278c687756537a670e344a13c31449f12cb9f255eacdfb38c")

;; The 18 holes of the real requisition, in the authored source's order.
(def hole-ids (keys authored/clauses))

(defn held-error []
  {:kind :wm/open-cascade-execution-error
   :cascade-digest cascade-digest
   :phase :compile
   :holes (mapv (fn [id] {:hole-id id :owner :wm/enactment-compiler
                          :question :operational-clause-owed})
                hole-ids)
   :attempts []
   :resume-port {:retry (fn [ctx] {:cascade-digest (:cascade-digest ctx)})}})

;; The reviewer's reason, from the retained reply's fenced JSON; the
;; retention bound truncated the tail ("produ …[trimmed]"), completed here
;; from the reviewer's own prose bullets in the same reply.
(def real-reason
  "Hole [\"cascade-construction/read-what-exists-first\" 0] in the author answer declares consumes=unrecorded-cascade-repair-transition / produces=auditable-mission-state-transition, but the committed source at 7f241c36 declares consumes=bare-mission-identifier / produces=read-anchored-target-identity")

;; The retained reply of job invoke-1793473761172-8882-652e3bf0: prose
;; report, then exactly one fenced JSON refusal object with the real pins.
(defn real-first-review-text [req]
  (str "I verified the committed source and every hole decision, and I'm refusing acceptance on one concrete discrepancy.\n\n"
       "**What I verified** (repo `/home/joe/code/futon2`, commit `5327b48f…` on `main`, file `src/futon2/aif/wm/authored/req_b9e453fd.clj`):\n"
       "- File content SHA-256 = `7f241c36…cdfb38c`, exactly the `source-digest` claimed. ✅\n"
       "- `futon2.aif.wm.authored.req-b9e453fd/decisions` is a `defn` destructuring `{:keys [cascade-digest phase]}` — a function of the context map, not a data var. `clj-kondo` on the file: 0 errors, 0 warnings. ✅\n"
       "- All 18 hole ids in the file match the requisition exactly (0–17, pattern names identical). ✅\n"
       "- Operations, capabilities, consumes, and produces match the author answer verbatim for holes 1–17. ✅\n\n"
       "**Discrepancy — hole 0** (`cascade-construction/read-what-exists-first`, 0):\n"
       "- Author answer: consumes `unrecorded-cascade-repair-transition`, produces `auditable-mission-state-transition`\n"
       "- Committed source: consumes `bare-mission-identifier`, produces `read-anchored-target-identity`\n\n"
       "Since acceptance binds the author's decisions to the verified source digest, and decision 0 in the answer does not match the source it would be bound to, I am not accepting:\n\n"
       "```json\n"
       (json/generate-string
        {:accepted false :accepted-by nil
         :requisition-id (or (:requisition-id req) real-requisition-id)
         :cascade-digest cascade-digest
         :patch {:namespace "futon2.aif.wm.authored.req-b9e453fd"
                 :var "decisions"
                 :source-digest real-source-digest
                 :commit "5327b48f94f882b43b73fc49b86b15785dc0b54e"}
         :reason real-reason})
       "\n```\n\n"
       "Resend with hole 0 matching the committed source (or amend the source) and I will accept."))

(defn real-refusal-response [req]
  {:state "done" :job-id "reviewer-job-adc46631"
   :result (real-first-review-text req)})

(defn acceptance-response [_req answer]
  {:state "done" :job-id "reviewer-job-accept"
   :result (str "Every original finding is resolved without regression:\n\n"
                "```json\n"
                (json/generate-string
                 {:accepted true :accepted-by "wm-reviewer"
                  :requisition-id (:requisition-id answer)
                  :cascade-digest (:cascade-digest answer)
                  :patch {:source-digest (get-in answer [:patch :source-digest])}})
                "\n```")})

;; The author answer as the reviewer found it: the real clauses, with
;; hole 0's consumes/produces claim diverging from the committed source.
(defn first-author-answer [req]
  {:requisition-id (:requisition-id req)
   :cascade-digest (:cascade-digest req)
   :by "wm-author"
   :decisions (assoc authored/clauses
                     ["cascade-construction/read-what-exists-first" 0]
                     (assoc (get authored/clauses
                                 ["cascade-construction/read-what-exists-first" 0])
                            :consumes :unrecorded-cascade-repair-transition
                            :produces :auditable-mission-state-transition))
   :patch {:namespace "futon2.aif.wm.authored.req-b9e453fd"
           :var "decisions"
           :source-digest real-source-digest}})

(defn revised-author-answer [req]
  ;; The corrected resubmission: hole 0 now matches the committed source.
  (assoc (first-author-answer req) :decisions authored/clauses))

(defn exercise
  "Run the handler against the real-shaped fixtures. revision-mode:
  :corrected, :unchanged (resubmit the first answer verbatim), or :none.
  second-review: :accept or :refuse. Returns the result plus fixture
  telemetry: author/reviewer requests seen and contact counts."
  [{:keys [revision-mode second-review cascade-feedback-path
           click-budget selection-counter]}]
  (let [author-requests (atom [])
        reviewer-requests (atom [])
        click-budget (or click-budget (atom 3))
        selection-counter (or selection-counter (atom 7))
        handler (runtime/open-cascade-resume-handler
                 {:author "wm-author" :reviewer "wm-reviewer"
                  :run-id "objection-round-test"
                  :cascade-feedback-path cascade-feedback-path
                  :click-budget click-budget
                  :selection-counter selection-counter
                  :open-cascade-author-answer-fn
                  (fn [issued]
                    (swap! author-requests conj issued)
                    (let [req (or (:requisition issued) issued)]
                      (if (:objection issued)
                        (case (or revision-mode :corrected)
                          :unchanged (first-author-answer req)
                          (revised-author-answer req))
                        (first-author-answer req))))
                  :open-cascade-reviewer-answer-fn
                  (fn [issued]
                    (swap! reviewer-requests conj issued)
                    (if (:first-review issued)
                      (if (= :refuse second-review)
                        (real-refusal-response issued)
                        (acceptance-response
                         issued
                         (revised-author-answer (or (:requisition issued) issued))))
                      (real-refusal-response issued)))
                  :open-cascade-patch-loader
                  (fn [_] (fn [_] authored/clauses))})]
    (with-redefs [delivery/check-delivery (fn [_] {:status :delivered})]
      (let [result (handler (held-error))]
        {:result result
         :author-requests @author-requests
         :reviewer-requests @reviewer-requests
         :click-budget click-budget
         :selection-counter selection-counter}))))

(deftest second-author-prompt-carries-the-objection-verbatim
  (let [{:keys [author-requests reviewer-requests]}
        (exercise {:second-review :accept})]
    (is (= 2 (count author-requests)) "first answer + one revision")
    (let [revision-request (second author-requests)]
      (is (= real-reason (:objection revision-request))
          "the revision request carries the reviewer's reason verbatim")
      (is (= 1 (:round revision-request)))
      (is (= (:requisition-id (first author-requests))
             (:requisition-id (:requisition revision-request)))
          "same requisition returns to the same author"))
    ;; The production prompt (what a live author seat would receive) embeds
    ;; the objection verbatim too.
    (is (.contains
         (#'runtime/open-cascade-objection-revision-prompt
          {:requisition (first author-requests) :objection real-reason})
         real-reason))
    ;; The second reviewer prompt contains the first review verbatim.
    (let [second-review-request (second reviewer-requests)]
      (is (= 2 (count reviewer-requests)))
      ;; The first review is retained verbatim: the reason appears in the
      ;; retained JSON exactly as the decoded review encodes it.
      (is (.contains ^String (:first-review second-review-request)
                     (json/generate-string real-reason))
          "first review retained verbatim in the second review request")
      (is (.contains
           ^String
           (#'runtime/open-cascade-revision-review-prompt
            {:requisition (first author-requests)
             :author-answer (revised-author-answer (first author-requests))
             :first-review (:first-review second-review-request)})
           (json/generate-string real-reason))
          "the production second-review prompt quotes the first review verbatim"))))

(deftest accepted-revision-resumes-with-counters-unchanged
  (let [{:keys [result click-budget selection-counter]}
        (exercise {:second-review :accept})]
    ;; The fixture retry port cannot compile a cascade, so an ACCEPTED
    ;; round stops typed at :resume-incomplete AFTER acceptance — the stop
    ;; line proves the round accepted and reached resume.
    (is (= :error (:status result)) (pr-str result))
    (is (= :resume-incomplete (:kind result)) (pr-str result))
    (is (= 3 @click-budget) "zero click credit spent")
    (is (= 7 @selection-counter) "selection counter unchanged")
    (is (= real-reason (get-in result [:objection-round :objection])))
    (is (= "wm-reviewer" (get-in result [:objection-round :verdict :accepted-by])))))

(deftest second-refusal-closes-typed-with-all-three-legs-and-no-third-contact
  (let [{:keys [result author-requests reviewer-requests]}
        (exercise {:second-review :refuse})]
    (is (= :error (:status result)) (pr-str result))
    (is (= :open-cascade-review-refused (:kind result)) (pr-str result))
    (is (= real-reason (get-in result [:objection :reason])))
    (is (= real-reason (:reason result)))
    (is (= :submitted (:status (:revision result))) (pr-str (:revision result)))
    (is (= false (get-in result [:verdict :accepted])))
    (is (= "reviewer-job-adc46631" (:reviewer-job result)))
    (is (= 2 (count author-requests)) "no third author contact")
    (is (= 2 (count reviewer-requests)) "no third reviewer contact")))

(deftest unchanged-resubmission-does-not-pass
  ;; The bad case: the author resubmits the refused answer unchanged. The
  ;; round must not spend a second reviewer contact and must not accept;
  ;; the close is the typed refusal with the unchanged revision retained.
  (let [{:keys [result author-requests reviewer-requests]}
        (exercise {:revision-mode :unchanged :second-review :accept})]
    (is (= :error (:status result)) (pr-str result))
    (is (= :open-cascade-review-refused (:kind result)) (pr-str result))
    (is (= :unchanged (:status (:revision result))) (pr-str (:revision result)))
    (is (= :revision-unchanged (get-in result [:verdict :reason])))
    (is (= 2 (count author-requests)))
    (is (= 1 (count reviewer-requests))
        "an unchanged resubmission never reaches a second review")))

(deftest the-round-writes-one-feedback-event-the-prior-reader-sees
  (let [path (str (java.nio.file.Files/createTempDirectory "objection-feedback"
                   (make-array java.nio.file.attribute.FileAttribute 0))
                  "/events.edn")
        {:keys [result]} (exercise {:second-review :refuse
                                    :cascade-feedback-path path})
        _ (is (= :error (:status result)))
        event (:cascade-feedback-event result)
        _ (is (= :recorded (:status event)) (pr-str event))
        snapshot (cascade-feedback/read-snapshot path)]
    (is (= 1 (count (:events snapshot))))
    (is (= :wm/open-cascade-objection-round-v1
           (get-in (first (:events snapshot)) [:objection-round :schema])))
    ;; The reader: cascade-feedback/construction-metadata feeds
    ;; pattern-evidence-prior, which both consumers use — cascade-revision's
    ;; revision scoring (cascade_revision.clj) and policy's selection menu
    ;; (policy.clj attach-pattern-evidence-menu). Show the prior sees the
    ;; round: hole 0's pattern now has one incomplete application and a
    ;; sub-neutral factor.
    (let [metadata (cascade-feedback/construction-metadata snapshot)
          global (cascade-feedback/pattern-evidence-prior
                  (:wm/global metadata)
                  {:precedence [{:id ["cascade-construction/read-what-exists-first" 0]}]})
          row (first (:patterns global))]
      (is (= 1 (:incomplete row)) (pr-str row))
      (is (= 0 (:successful row)))
      (is (< (:factor global) 1.0) "the refusal evidence lowers the prior"))))

(deftest a-resolved-objection-penalises-no-pattern
  (let [path (str (java.nio.file.Files/createTempDirectory "objection-resolved"
                   (make-array java.nio.file.attribute.FileAttribute 0))
                  "/events.edn")
        {:keys [result]} (exercise {:second-review :accept
                                    :cascade-feedback-path path})
        event (:cascade-feedback-event result)
        patterns (:patterns (first (:events (cascade-feedback/read-snapshot path))))]
    (is (= :recorded (:status event)) (pr-str event))
    (is (= [] (:applications patterns) (:selected patterns))
        "only a second refusal counts against the patterns")))
