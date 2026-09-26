(ns futon2.aif.observation-admission
  "WM-04 S-2: observation admission records for the token observation carrier
  (P5, TokenObservation.lean; runtime carrier cascade_model_manifest.clj).

  Four pure steps — observer-view, adjudication, review, admit — that keep
  blinding by construction and turn a reviewed adjudication into an admitted
  token label or a typed refusal. No storage, no IO, no defaults: a missing
  input is a typed refusal, and an unobserved token is never an absent one —
  only an adjudicated, concurred :absent finding yields an :absent label."
  (:require [futon2.aif.cascade-model-manifest :as manifest]))

(def findings #{:present :absent :insufficient :ambiguous :conflicting})
(def verdicts #{:concur :dispute :insufficient})

(defn observer-view
  "The view an observer may see of SUBJECT: the token, the application text
  and the evidence pointers. The subject's :recorded-verdict and every author
  claim (:author, :enactor) are excluded by construction, so blinding cannot
  leak through the view."
  [subject]
  (select-keys subject [:token :application :evidence-pointers]))

(defn view-digest
  "The digest a later admission step recomputes over the observer view."
  [view]
  (manifest/sha256 (pr-str view)))

(defn adjudication
  "OBSERVER-ID's finding about VIEW, recorded with the view's digest and the
  evidence cutoff ({repo sha}). FINDING must be one of the findings above."
  [observer-id view finding cutoff]
  {:observer observer-id :view-digest (view-digest view)
   :finding finding :cutoff cutoff})

(defn review
  "REVIEWER-ID's verdict on ADJUDICATION (:concur, :dispute, :insufficient)."
  [reviewer-id adjudication verdict]
  {:reviewer reviewer-id :of (:view-digest adjudication)
   :finding (:finding adjudication) :verdict verdict})

(defn- refuse [kind data]
  {:status :missing :kind kind :reason (name kind) :data data})

(defn mechanical-review
  "Review only mechanism identity, observer-view digest and cutoff. SUBJECT
  declares :check-mechanism (a nonempty code identity string) and :check-cutoff
  (the same repo/sha map carried by adjudication). No evidence is read and no
  recorded verdict is inspected.

  Declaration/reviewer refusals precede the three comparisons: undeclared
  mechanism, missing reviewer, reviewer equal to observer, reviewer equal to
  check mechanism. They return a disputed review with typed :status/:kind.
  Otherwise dispute the FIRST failure: :self-truthed, :view-mismatch,
  :cutoff-mismatch, in that order; concur only if all three pass.
  The view check duplicates admit's earlier :view-digest-mismatch refusal
  deliberately: this review must stand on its own when read without admit."
  [reviewer-id subject adjudication]
  (let [mechanism (:check-mechanism subject)
        observer (:observer adjudication)
        invalid (cond
                  (not (and (string? mechanism) (seq mechanism))) :check-mechanism-undeclared
                  (not (and (string? reviewer-id) (seq reviewer-id))) :reviewer-missing
                  (= reviewer-id observer) :observer-is-reviewer
                  (= reviewer-id mechanism) :reviewer-is-check-mechanism)
        reason (or invalid
                   (cond
                     (= observer mechanism) :self-truthed
                     (not= (:view-digest adjudication) (view-digest (observer-view subject))) :view-mismatch
                     (not= (:cutoff adjudication) (:check-cutoff subject)) :cutoff-mismatch))]
    (cond-> (assoc (review reviewer-id adjudication (if reason :dispute :concur))
                   :mechanical true)
      reason (assoc :reason reason)
      invalid (assoc :status :missing :kind invalid))))

(defn admit
  "Admit a token label from SUBJECT + ADJUDICATION + REVIEW, or refuse typed.

  Admitted: {:status :admitted :label :present|:absent ...}.
  Refusals (each {:status :missing :kind ...}):
    :invalid-finding / :invalid-verdict     — vocabulary violations
    :observer-missing / :reviewer-missing / :authorship-undeclared
    :observer-is-author / :observer-is-enactor / :observer-is-reviewer
    :review-of-other-adjudication          — the review is not of this adjudication
    :cutoff-missing                        — the adjudication carries no cutoff
    :view-digest-mismatch                  — the observer saw something else
    :no-label                              — insufficient/ambiguous/conflicting
                                            finding: no label, reason recorded
    :review-not-concur                     — dispute or insufficient review
  UNOBSERVED IS NOT ABSENT: no code path derives :absent from a missing
  adjudication; only an adjudicated :absent with a :concur review admits one."
  [subject adjudication review-record]
  (let [observer (:observer adjudication)
        reviewer (:reviewer review-record)
        author (:author subject)
        enactor (:enactor subject)
        finding (:finding adjudication)
        verdict (:verdict review-record)]
    (cond
      (not (contains? findings finding))
      (refuse :invalid-finding {:finding finding})

      (not (contains? verdicts verdict))
      (refuse :invalid-verdict {:verdict verdict})

      (not (and (string? observer) (seq observer)))
      (refuse :observer-missing {:adjudication adjudication})

      (not (and (string? reviewer) (seq reviewer)))
      (refuse :reviewer-missing {:review review-record})

      ;; independence is checkable only if the subject declares who authored
      ;; and enacted it (a string, or :none); an undeclared role is a refusal,
      ;; not a pass (claude-4 review)
      (not (and (contains? subject :author) (contains? subject :enactor)))
      (refuse :authorship-undeclared {:declared (select-keys subject [:author :enactor])})

      (and author (= observer author))
      (refuse :observer-is-author {:observer observer :author author})

      (and enactor (= observer enactor))
      (refuse :observer-is-enactor {:observer observer :enactor enactor})

      (= observer reviewer)
      (refuse :observer-is-reviewer {:observer observer :reviewer reviewer})

      (not (seq (:cutoff adjudication)))
      (refuse :cutoff-missing {:adjudication adjudication})

      (not= (:view-digest adjudication) (view-digest (observer-view subject)))
      (refuse :view-digest-mismatch
              {:recorded (:view-digest adjudication)
               :recomputed (view-digest (observer-view subject))})

      ;; the review must be of THIS adjudication (claude-4 review)
      (not (and (= (:of review-record) (:view-digest adjudication))
                (= (:finding review-record) finding)))
      (refuse :review-of-other-adjudication
              {:review-of (:of review-record) :adjudication (:view-digest adjudication)
               :review-finding (:finding review-record) :finding finding})

      (contains? #{:insufficient :ambiguous :conflicting} finding)
      (refuse :no-label {:finding finding})

      (not= :concur verdict)
      (refuse :review-not-concur (cond-> {:verdict verdict}
                                  (:mechanical review-record)
                                  (assoc :reason (:reason review-record))))

      :else {:status :admitted :label finding
             :token (:token subject)
             ;; the recorded verdict is carried AFTER admission so rates
             ;; (S-3) can compare it with the admitted label; the observer never saw it
             :recorded-verdict (:recorded-verdict subject)
             :observer observer :reviewer reviewer
             :cutoff (:cutoff adjudication)
             :view-digest (:view-digest adjudication)})))

(defn label-key
  "Identity of one located subject under one check mechanism, independent of
  tick/run ids. :token-class is on SUBJECT; its first :evidence-pointers entry
  is the subject locator, with :repo, :resolved-sha (or an already resolved
  :sha), :path or :entry, and optional :decl. Callers supply resolved commits;
  this pure helper never resolves a symbolic git ref."
  [subject]
  (let [{:keys [repo resolved-sha sha path entry decl]} (first (:evidence-pointers subject))]
    [(:token-class subject) repo (or resolved-sha sha) (or path entry) decl
     (:check-mechanism subject)]))

(defn label-record
  "Return a rates label only for an admitted result; any refusal yields nil.
  Carry the recorded verdict unchanged (boolean or absent for rates), alongside
  the full admission and the stable subject/check identity."
  [subject admit-result]
  (when (= :admitted (:status admit-result))
    {:token-class (:token-class subject)
     :recorded (:recorded-verdict admit-result)
     :admitted (:label admit-result)
     :label-key (label-key subject)
     :admission admit-result}))
