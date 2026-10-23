(ns futon2.aif.outer-canonical-g
  "Diagnostic outer-menu producer for the canonical co-application G.

   It consumes the already bound menu actions; it does not choose an action.
   O3-2 is the only packet permitted to make this frontier selection-active."
  (:require [futon2.aif.cascade-coapply-g :as canonical]
            [futon2.aif.cascade-observation-scoring :as scorer]
            [futon2.aif.cascade-pattern-counts :as counts]
            [futon2.aif.cascade-shape-g :as shape]
            [futon2.aif.load-identity :as identity]))

(identity/register! *ns* *file*)

(def schema :wm/outer-canonical-g-frontier-v1)

(defn- digest [value] (identity/sha256 (.getBytes (pr-str value) "UTF-8")))

(defn- refusal [entry reason]
  {:policy-id (:policy-id entry) :target-id (:target-id entry)
   :cascade-digest (get-in entry [:action :cascade-digest])
   :status :refused :reason reason})

(defn- candidate-and-model [entry h]
  (let [action (:action entry)
        target (:target-id entry)
        model (get-in entry [:class-emission :model])]
    (cond
      (not (and (map? action) (= (:policy-id entry) (:id action))))
      {:refusal (refusal entry :canonical-action-unavailable)}

      (not= target (:target action))
      {:refusal (refusal entry :canonical-action-target-mismatch)}

      (not (and (map? model) (= :class-emission (:kind model))))
      {:refusal (refusal entry :class-emission-model-unavailable)}

      (not (contains? (:target-class model) target))
      {:refusal (refusal entry :class-emission-target-class-unavailable)}

      :else
      {:candidate action
       :model (shape/class-emission-model target (:universe model) (:acceptance model) h
                                           (get (:target-class model) target)
                                           (:provenance model))})))

(defn produce
  "Produce one sealed, diagnostic canonical-G frontier for a sealed outer menu.
   Actions are the entry-aligned retained cascade candidates.  The shared H rule
   is the already declared family rescore rule, max unit count clamped by the
   scorer limit.  Per-entry refusal never becomes a numeric score or rank."
  [sealed-menu]
  (let [entries (vec (:entries sealed-menu))
        actions (mapv :action entries)
        unit-counts (into {}
                          (map (fn [[entry action]]
                                 [(:policy-id entry)
                                  (count (get-in action [:precedence :co-apply :units]))]))
                          (map vector entries actions))
        h (max 1 (min scorer/max-horizon (reduce max 0 (vals unit-counts))))
        horizon-choice {:horizon h :rule :max-field-unit-count-clamped
                        :limit scorer/max-horizon :unit-counts unit-counts
                        :entry-horizon-steps
                        (into {} (map (juxt :policy-id #(get-in % [:class-emission :model :horizon])))
                              entries)}
        prepared (mapv #(candidate-and-model % h) entries)
        candidates (mapv :candidate (filter :candidate prepared))
        models (into {} (map (juxt (comp :id :candidate) :model)) (filter :candidate prepared))
        count-result (counts/family-pattern-counts candidates)
        options {:horizon-steps h :pattern-counts (:pattern-counts count-result)
                 :observation-model-of models :require-ruled-class-kernel? true
                 :prediction-context {:occurrence-id [:outer-menu (mapv :policy-id entries)] :tau h}}
        ;; The frontier is diagnostic in O3-1: a scorer exception is recorded
        ;; as a typed refusal rather than ending the outer selection.
        thrown (volatile! nil)
        result (when (and (seq candidates) (not= :refused (:status count-result)))
                 (try (canonical/compare-at-horizon {:cascade-belief {#{} 1}} candidates options)
                      (catch Exception e
                        (vreset! thrown {:class (.getName (class e)) :message (.getMessage e)})
                        nil)))
        by-id (into {} (map (juxt :cascade-id identity))
                       (if (vector? result) result (:candidates result)))
        pre-refusals (into {} (map (juxt (comp :policy-id :refusal) :refusal))
                           (filter :refusal prepared))
        results
        (mapv (fn [entry]
                (let [id (:policy-id entry)
                      action (:action entry)
                      r (or (get pre-refusals id) (get by-id id))]
                  (cond
                    r (cond-> (assoc (select-keys r [:status])
                                     :policy-id id :target-id (:target-id entry)
                                     :cascade-digest (:cascade-digest action)
                                     :reason (or (:reason r) (get-in r [:refusal :reason])
                                                 (get-in r [:refusal :kind]))
                                     :action-pin {:cascade-digest (:cascade-digest action)})
                        (= :scored (:status r))
                        (assoc :G (:G-efe r) :rank (:rank r)))
                    @thrown
                    (assoc (refusal entry :canonical-score-threw)
                           :exception @thrown
                           :action-pin {:cascade-digest (:cascade-digest action)})
                    (= :refused (:status count-result))
                    (assoc (refusal entry (:kind count-result))
                           :action-pin {:cascade-digest (:cascade-digest action)})
                    :else
                    (assoc (refusal entry :canonical-score-unavailable)
                           :action-pin {:cascade-digest (:cascade-digest action)}))))
              entries)
        body {:schema schema :status (if (seq candidates) :produced :refused)
              :menu-pin (:menu-pin sealed-menu)
              :horizon-choice horizon-choice
              :count-table (:table count-result) :count-table-digest (digest (:table count-result))
              :class-model-ids (into {} (map (fn [[id model]] [id (digest model)]) models))
              :entries results
              :comparison (select-keys (meta result) [:formal-contract :lean-reduction :horizon-steps :refused-count])}]
    (assoc body :frontier-pin {:path "wm://outer-canonical-g-frontier-v1"
                               :sha256 (digest body)})))
