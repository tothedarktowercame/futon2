;; Live C, first cut — before/after of the tick's two-target choice.
;; Lab replica: production wiring is out of scope for this slice. The
;; BEFORE goes through the REAL production cascade-decision. The AFTER
;; replicates cascade-decision's joint scoring tail (same functions:
;; efe/rank-actions + policy/select-action-cascades) with the live-C spec.
(require '[futon2.aif.cascade-sources :as cs]
         '[futon2.aif.cascade-problems :as cp]
         '[futon2.aif.live-c :as lc]
         '[futon2.aif.efe :as efe]
         '[futon2.aif.policy :as policy]
         '[futon2.aif.cascade-selection :as csel]
         '[futon2.aif.cascade-model-manifest :as manifest]
         '[futon2.aif.cascade-policy :as cascade-policy]
         '[futon2.report.war-machine :as wm]
         '[clojure.set :as set])



(defn -main []
  ;; ---- BEFORE: the production path, exactly as the tick runs it
  (let [declared (cs/with-context-fn (cs/load-declared))
        horizon {:value (or (:horizon-steps declared) 2)
                 :authority (if (:horizon-steps declared) :cascade-sources
                                "p4ng 462aa79 (Joe 2026-09-17: initial T=2)")}
        assembled (cp/assemble
                   {:targets (vec (distinct (concat (cp/substrate-targets)
                                                    (keys (:universes declared)))))
                    :sources (assoc declared :horizon-steps (:value horizon))})
        before (try (wm/cascade-decision assembled {}) 
                    (catch Exception e {:threw (ex-data e)}))
        problems (:problems assembled)
        ;; ---- the live C, derived now
        sources-now (lc/read-sources)
        live (lc/derive-live-c sources-now)
        ;; ---- AFTER: replicate cascade-decision's scoring tail
        qualification (fn [target token] [target token])
        T (:value horizon)
        beta 1
        patterns-per-target
        (into {}
              (map (fn [problem]
                     (let [t (:target problem)
                           cpb (:cascade-problem problem)
                           qual (partial qualification t)]
                       [t (into {}
                                (map (fn [[id {:keys [guard produces]}]]
                                       [id (-> (cascade-policy/token-interpretation
                                                id {:guard {:needs (set (map qual (:needs guard)))
                                                           :forbids (set (map qual (:forbids guard)))}
                                                      :produces (set (map qual produces))})
                                               (assoc :target t))]))
                                (:interpretations cpb))])))
              problems)
        candidates
        (mapcat (fn [problem]
                  (let [t (:target problem)]
                    (map-indexed (fn [i order]
                                   {:kind :cascade-candidate :id (keyword (str "C" i))
                                    :target t
                                    :precedence (mapv (get patterns-per-target t) order)})
                                 (:precedences (:cascade-problem problem)))))
                problems)
        joint-q0 (manifest/observed-belief
                  (reduce (fn [acc p]
                            (let [t (:target p)]
                              (set/union acc
                                         (set (for [[f v] (get-in p [:cascade-problem :facts])
                                                    :when (true? v)]
                                                [t f])))))
                          #{} problems))
        joint-want (reduce (fn [acc p]
                             (let [t (:target p)]
                               (into acc (map (fn [w] [t w]))
                                     (get-in p [:cascade-problem :want]))))
                           #{} problems)
        reachable (set (concat joint-want
                               (mapcat (fn [c] (mapcat (fn [p]
                                                         (concat (mapcat :present (get-in p [:guard :clauses]))
                                                                 (mapcat :absent (get-in p [:guard :clauses]))
                                                                 (:produces p)))
                                                       (:precedence c)))
                                       candidates)
                               joint-q0))
        live-spec (lc/cascade-spec live reachable)
        rank (fn [spec] (efe/rank-actions {:cascade-belief joint-q0} (vec candidates)
                                          {:horizon-steps T :cascade-spec spec}))
        before-replica (rank {:want joint-want})
        after (if (:refusal live-spec)
                {:refused live-spec}
                (rank (dissoc live-spec :live-c)))
        decide (fn [r] (try (policy/select-action-cascades r {:beta beta})
                            (catch Exception e {:threw (ex-data e)})))
        result {:ran-at (java.time.Instant/now)
                :before {:production-decision (select-keys (:decision before)
                                                           [:status :action :mass :selection-law :horizon-steps])
                         :replica-ranked (count before-replica)
                         :replica-decision (select-keys (decide before-replica)
                                                        [:status :action :mass])}
                :live-c {:n-want (count (:want live))
                         :n-gaps (count (:gaps live))
                         :deduped (:deduped-entries live)
                         :signature (:signature live)
                         :stale-check (lc/stale? live sources-now)}
                :after {:reachable-tokens (count reachable)
                        :live-tokens-in-domain (count (:want live-spec))
                        :spec (if (:refusal live-spec)
                                (:refusal live-spec)
                                {:want (count (:want live-spec))
                                 :weights-sample (into {} (take 3 (:weights live-spec)))
                                 :live-c (:live-c live-spec)})
                        :decision (if (:refusal live-spec)
                                    {:unchanged :live-c-refused-to-speak
                                     :refusal (:refusal live-spec)}
                                    (select-keys (decide after) [:status :action :mass]))}}]
    (spit "/home/joe/code/futon2/holes/labs/wm-contract/runs/live-c-first-cut-2026-09-17/before-after.edn"
          (with-out-str (clojure.pprint/pprint result)))
    (clojure.pprint/pprint result)))
(-main)
