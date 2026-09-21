(ns futon2.aif.token-initialization-policy
  "Declared, deterministic observation initialization; no transition or causal claim."
  (:require [futon2.aif.load-identity :as load-identity]
            [futon2.aif.interpretation-evidence :as evidence]))

(load-identity/register! *ns* *file*)

(def disabled {:schema :wm/token-initialization-policy-v1 :enabled false
               :placement :next-selection :unknown :fresh-initialization
               :temporal-order :same-revision-only
               :historical :declared-revision-only})

(defn valid-policy? [p]
  (and (boolean? (:enabled p)) (= disabled (assoc p :enabled false))))

(defn enabled? [context]
  (some #(true? (get-in % [:policy :enabled])) (vals context)))

(defn- refusal [kind & [detail]]
  {:status :refused :kind kind :detail detail})

(defn- token-update [token row current]
  (let [locator (get-in row [:meaning :locator])
        historical? (not= "HEAD" (:sha locator))
        old (if historical? (:declared-revision-observation row) (:artifact-observation row))
        result (if historical? (:result old) (get-in old [:measurement :result]))
        fresh (get-in current [:observations (second token)])
        revision (get-in result [:evidence :resolved-sha])
        fresh-revision (get-in fresh [:evidence :resolved-sha])]
    (cond
      (not= {:token token :declaration-sha256 (:declaration-sha256 current)
             :locator (get-in current [:locators (second token)])} (:meaning row))
      (refusal :token-meaning-changed)
      (or (not= (:meaning-sha256 row) (evidence/value-digest (:meaning row)))
          (not= (:schedule-sha256 row) (evidence/value-digest (:schedule row)))
          (not= (:schedule current) (:schedule row)))
      (refusal :observation-binding-changed)
      (not (#{:C3 :C4} (:class locator))) (refusal :observation-class-not-checkable)
      (not (boolean? (:observed old)))
      {:status :not-updated :kind :observation-missing :observation (:observed old)}
      (or (nil? revision) (not= (:observed old) (:observed result))
          (not= (:evidence-sha256 old) (evidence/value-digest result))
          (not= (:class locator) (:check result))
          (and (not historical?)
               (or (not= revision (:artifact-sha old))
                   (not= token (get-in old [:measurement :token]))
                   (not= (:declaration-sha256 current) (get-in old [:measurement :declaration-sha256]))
                   (not= locator (get-in old [:measurement :declared-locator])))))
      (refusal :observation-evidence-changed)
      ;; This first policy deliberately admits no cross-revision ordering.
      ;; Fresh evidence at another revision remains the initializer; the old
      ;; observation is refused, never silently declared newer or older.
      (and fresh-revision (not= revision fresh-revision))
      (refusal :stale-or-unordered-observation {:artifact revision :fresh fresh-revision})
      (and (boolean? (:observed fresh)) (not= (:observed fresh) (:observed old)))
      (refusal :same-revision-observation-conflict)
      :else {:status :updated :observed (:observed old)
             :temporal-scope (:temporal-scope old) :revision revision
             :meaning-sha256 (:meaning-sha256 row) :schedule (:schedule row)
             :evidence-sha256 (:evidence-sha256 old)})))

(defn apply-observations
  "Replay admitted observations against the exact previous occurrence/domain.
   The caller supplies verification, not predictions. Refusals retain fresh q0."
  [stage inspection projection]
  (let [context (:observation-initialization stage)
        universe (get-in stage [:prospective-carry :universe])
        expected (:task-context inspection)
        previous (:prospective-prior stage)
        bad (cond
              (not (every? #(valid-policy? (:policy %)) (vals context))) :invalid-initialization-policy
              (not (enabled? context)) :observation-initialization-disabled
              (not= :admitted (:status projection)) (or (:kind projection) :observation-authority-unavailable)
              (not= (:occurrence expected) (:occurrence projection)) :observation-occurrence-mismatch
              (or (nil? previous)
                  (not= (:occurrence-id previous) (:carry-occurrence-id projection)
                        (:carry-occurrence-id expected))) :observation-predecessor-mismatch
              (not= universe (:universe previous) (:universe projection) (:universe expected)) :observation-domain-changed)
        updates (if bad []
                    (mapv (fn [token]
                            (let [current (get context (first token))]
                              (merge {:token token}
                                     (cond
                                       (not (true? (get-in current [:policy :enabled])))
                                       {:status :not-updated :kind :observation-initialization-disabled}
                                       (nil? (get-in projection [:observations token]))
                                       {:status :not-updated :kind :observation-missing}
                                       :else (token-update token (get-in projection [:observations token]) current)))))
                          (sort-by pr-str universe)))
        accepted (filter #(= :updated (:status %)) updates)
        belief (reduce (fn [q {:keys [token observed]}]
                         (reduce-kv (fn [out state mass]
                                      (update out ((if observed conj disj) state token) (fnil + 0) mass)) {} q))
                       (get-in stage [:initialization :value]) accepted)]
    {:status (if bad :refused :processed) :kind bad
     :placement :next-selection :temporal-order :same-revision-only
     :unknown-policy :fresh-initialization :observation-updates updates
     :continuation-belief belief
     :source (select-keys projection [:occurrence :carry-occurrence-id :record-sha256 :source])}))
