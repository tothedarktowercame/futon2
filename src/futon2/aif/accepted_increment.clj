(ns futon2.aif.accepted-increment
  "PROOF-wm-works ⟨1⟩4: the accepted-increment predicate. Answers, for ONE
   occurrence, whether the TARGET'S OWN acceptance was met by the reviewed
   work. Three conjuncts, each reusing an existing producer:

   (a) fresh binding — the reviewed commits are bound to this occurrence and
       are fresh descendants of the pre-dispatch head (reuses
       task-execution-evidence/fresh-artifact-binding's verdict fields);
   (b) declared products observed — the selected candidate's DECLARED
       produced tokens read TRUE at the after-revision through their own
       C3/C4 locators (the observation checker, the same checks the
       measurement producer uses);
   (c) the target's own acceptance declaration observed TRUE at that
       revision — for a ticket target, the acceptance token's locator; for
       a mission target, its completion-criteria locator.

   A target with no mechanical acceptance declaration is
   :no-acceptance-declared — never true. verify-close is untouched: it
   checks the record, not the work; this predicate checks the work."
  (:require [futon2.aif.load-identity :as load-identity]
            [futon2.aif.observation-checks :as checks]))

(load-identity/register! *ns* *file*)

(defn- locator-observed-true?
  "Read one C3/C4 locator at the after-revision. Returns the check's result
   map so evidence travels with the verdict."
  [locator]
  (case (:class locator)
    :C3 (checks/check-path-exists locator)
    :C4 (checks/check-decl-in-file locator)
    {:status :missing :kind :unsupported-locator-class :class (:class locator)}))

(defn accepted-increment
  "The predicate. Inputs:

     {:binding          the artifact binding for this occurrence
                        (from the close/build checkpoint: :repo :commit
                        :pre-dispatch-head plus fresh-artifact-binding's
                        verdict fields — :descendant? :corroborates?
                        :observed-valid? :claim-in-author-window?)
      :produced-tokens  {token locator} — the measured rows' tokens with
                        their locators (from the measurement producer)
      :declared-tokens  (optional) the enacted step's :produces — the
                        tokens the candidate DECLARED it would produce.
                        When tokens are declared but produced-tokens is
                        empty (nothing was measured), (b) fails with
                        :no-declared-product-measured naming the declared
                        tokens. A candidate genuinely declaring nothing is
                        unaffected.
      :acceptance       the target's own acceptance declaration as
                        {:token t :locator l}, or nil
      :after-revision   the reviewed after-revision sha (nil when the
                        occurrence never produced one)}

   Returns {:accepted? true :evidence …} or
   {:accepted? false :failed :a|:b|:c :evidence …} or
   {:accepted? :no-acceptance-declared :target …}. Each failure names its
   conjunct and carries the evidence it used."
  [{:keys [binding produced-tokens declared-tokens acceptance after-revision]}]
  (let [;; (a) fresh binding: the binding's own verdict fields, exactly the
        ;; ones task-execution-evidence/fresh-artifact-binding computes.
        a-ok (and (map? binding)
                  (string? (:commit binding))
                  (string? (:pre-dispatch-head binding))
                  (true? (:descendant? binding))
                  (true? (:corroborates? binding))
                  (true? (:claim-in-author-window? binding)))
        ;; (b) each declared produced token observed TRUE at the
        ;; after-revision through its own locator.
        b-results (into {}
                        (for [[token locator] produced-tokens]
                          [token (if (nil? after-revision)
                                   {:status :missing :kind :no-after-revision}
                                   (locator-observed-true? locator))]))
        b-bad (into {} (filter (fn [[_ r]] (not (true? (:observed r)))) b-results))
        ;; (c) the target's own acceptance declaration observed TRUE.
        c-result (when acceptance
                   (if (nil? after-revision)
                     {:status :missing :kind :no-after-revision}
                     (locator-observed-true? (:locator acceptance))))
        c-ok (true? (:observed c-result))]
    (cond
      (nil? acceptance)
      {:accepted? :no-acceptance-declared
       :reason :target-has-no-mechanical-acceptance-declaration
       :evidence {:produced-token-results b-results}}

      (not a-ok)
      {:accepted? false :failed :a
       :reason (cond (nil? binding) :no-artifact-binding
                     (nil? (:commit binding)) :no-reviewed-commit
                     :else :binding-not-fresh)
       :evidence {:binding binding}}

      (seq b-bad)
      {:accepted? false :failed :b
       :reason :declared-product-not-observed-true
       :evidence {:failed-tokens b-bad :all-results b-results}}

      ;; ⟨1⟩6 (claude-5 review finding 3): a candidate that DECLARED
      ;; products but whose produced-tokens is EMPTY measured nothing —
      ;; (b) must fail rather than fall through to (c). Evidence, never
      ;; a gate: the close still gets written.
      (and (seq declared-tokens) (empty? produced-tokens))
      {:accepted? false :failed :b
       :reason :no-declared-product-measured
       :evidence {:declared-tokens (vec declared-tokens)
                  :measured-tokens 0
                  :note "the enacted step declared these products and no measurement row existed for any of them"}}

      (not c-ok)
      {:accepted? false :failed :c
       :reason (or (:kind c-result) :acceptance-not-observed)
       :evidence {:acceptance-token (:token acceptance)
                  :acceptance-result c-result}}

      :else
      {:accepted? true
       :evidence {:binding {:commit (:commit binding)
                            :pre-dispatch-head (:pre-dispatch-head binding)}
                  :produced-token-results b-results
                  :acceptance-token (:token acceptance)
                  :acceptance-result c-result}})))

(defn evaluate-close
  "Runner adapter for token-outcome/compare-outcomes' vector of row maps.
   Keep each producer's complete after-locator, including its bound revision.
   Missing measurements remain missing; evaluation errors are evidence, never
   exceptions that prevent writing the close."
  [{:keys [binding token-rows acceptance after-revision]}]
  (try
    (accepted-increment
     {:binding binding
      :produced-tokens
      (into {} (map (fn [row]
                      (when-not (and (map? row) (contains? row :token))
                        (throw (ex-info "Expected a token comparison row map"
                                        {:row row})))
                      [(:token row) (get-in row [:measurement :after-locator])]))
            token-rows)
      :acceptance acceptance
      :after-revision after-revision})
    (catch Exception e
      {:accepted? :refused
       :reason :predicate-evaluation-failed
       :message (.getMessage e)})))
