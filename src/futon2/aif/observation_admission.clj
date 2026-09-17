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

(defn admit
  "Admit a token label from SUBJECT + ADJUDICATION + REVIEW, or refuse typed.

  Admitted: {:status :admitted :label :present|:absent ...}.
  Refusals (each {:status :missing :kind ...}):
    :invalid-finding / :invalid-verdict     — vocabulary violations
    :observer-is-author / :observer-is-enactor / :observer-is-reviewer
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

      (contains? #{:insufficient :ambiguous :conflicting} finding)
      (refuse :no-label {:finding finding})

      (not= :concur verdict)
      (refuse :review-not-concur {:verdict verdict})

      :else {:status :admitted :label finding
             :token (:token subject)
             :observer observer :reviewer reviewer
             :cutoff (:cutoff adjudication)
             :view-digest (:view-digest adjudication)})))
