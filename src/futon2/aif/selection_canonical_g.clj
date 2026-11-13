(ns futon2.aif.selection-canonical-g
  "Selection-run G over cascades: the canonical cascade G the inner loop already
  uses (cascade-coapply-g/compare-at-horizon), applied to task-selection cascades.
  Lean: DarkTower.WarMachine.SelectionGOverCascades (selectionG := cascadeGrainG).

  Joe, 2026-11-13: \"it is G over policies, so a different cascade (including a
  different pattern) should have a different G even if it chooses the same
  ticket\"; class route. The pick is unchanged (outer-score/cascade-pick); what
  changes is that each cascade's patterns, interpreted from their @guard and
  @predicts, drive a token trajectory that the shared class-emission channel
  (cascade-shape-g/class-emission-model) observes: the pick's ending class once
  [pick :can-begin] holds, not-yet-evaluated before. Stopping is the empty
  policy: one unit whose guard never holds, so no transition and no novelty
  (Lean: a candidate with no patterns; the Clojure candidate check requires a
  unit). theta 1 and Beta(1,1) counts are documented defaults, fixed (Joe: \"We
  don't need it to self-improve\").

  Tokens are target-qualified, [target token], in one shared state, as the work
  run's family scoring does; each cascade's patterns read and write only its
  pick's tokens, which equals the Lean per-candidate initial state."
  (:require [clojure.set :as set]
            [futon2.aif.cascade-coapply-g :as canonical]
            [futon2.aif.cascade-observation-scoring :as scorer]
            [futon2.aif.cascade-shape-g :as shape]))

(def interpretations
  "Lean SelectionGOverCascades.interpret, from each pattern's @guard and @predicts."
  {"only-what-can-start-now"   {:consumes #{:ready} :forbids #{:unmet-need} :produces #{:can-begin}}
   "what-others-wait-on-first" {:consumes #{:ready :unblocks} :forbids #{} :produces #{:ready-count-rises}}
   "prepare-the-ground"        {:consumes #{:cone :named-later-item} :forbids #{} :produces #{:later-item-reshaped}}
   "find-out-before-building"  {:consumes #{:undetermined} :forbids #{} :produces #{:design-decided}}
   "serves-a-way"              {:consumes #{:ready :placed-work} :forbids #{:non-work-placement} :produces #{:reason-names-way}}})

(def ^:dynamic *extra-interpretations*
  "Test seam: fixtures bind interpretations for their synthetic patterns here,
  stated in the test file. Production leaves it empty."
  {})

(defn- tag [target ks] (into #{} (map #(vector target %)) ks))

(defn- status-of [fact] (some-> (:status fact) name))

(defn fact-tokens
  "Lean initialTokens: the selected task's whole-task facts as tokens."
  [target fact]
  (let [placement (:placement fact)
        placed? (and (seq (:ways placement))
                     (= "work" (some-> (:action placement) name))
                     (not= "absent" (some-> (:status placement) name)))
        present? (and placement (not= "absent" (some-> (:status placement) name)))]
    (tag target
         (cond-> #{}
           (= "ready" (status-of fact)) (conj :ready)
           (= "undetermined" (status-of fact)) (conj :undetermined)
           (pos? (or (:unblocks fact) 0)) (conj :unblocks)
           (pos? (or (:cone fact) 0)) (conj :cone)
           placed? (conj :placed-work)
           (and present? (not placed?)) (conj :non-work-placement)
           (some #(not= :met (:resolution %)) (:prerequisites fact)) (conj :unmet-need)))))

(defn- pattern-record [target {:keys [consumes forbids produces]}]
  (let [present (tag target consumes) absent (tag target forbids) out (tag target produces)]
    {:guard {:status :interpreted :operator :and :clauses [{:present present :absent absent}]}
     :transition {:status :interpreted :operator :union :produces out}
     :produces out :forbids absent :theta 1 :theta-source :documented-default}))

(def ^:private stop-unit [::stop :inert])
(def ^:private stop-pattern
  {:guard {:status :interpreted :operator :and :clauses [{:present #{[::stop :never]} :absent #{}}]}
   :transition {:status :interpreted :operator :union :produces #{[::stop :never]}}
   :produces #{[::stop :never]} :forbids #{} :theta 1 :theta-source :documented-default})

(def class-of-relation
  {:focus :focused :associated :related :useful-elsewhere :unrelated})

(def ^:private stop-acceptance
  "The empty policy never establishes this, so at the horizon its channel emits
  :stop-the-line (observation-model class emission: no own acceptance token)."
  [:no-pick :can-begin])

(defn candidate
  "One menu cascade (:id :patterns :pick :relation-class) as a co-application candidate,
  under INTERPS (pattern id -> {:consumes :forbids :produces})."
  [interps {:keys [id patterns pick]}]
  (if (or (nil? pick) (= :no-pick pick) (empty? patterns))
    ;; No pick, or a cascade enacting no pattern: the empty policy (Lean: no
    ;; patterns). Nothing fires, the pick never can-begin, the channel emits
    ;; stop-the-line at the horizon.
    {:id id :kind :cascade-candidate :target (if (or (nil? pick) (= :no-pick pick)) :no-pick pick)
     :patterns [stop-unit]
     :precedence {:co-apply {:units [stop-unit] :descent [] :patterns {stop-unit stop-pattern}}}}
    (let [units (mapv #(vector % pick) (distinct patterns))]
      (when-let [missing (seq (remove interps (distinct patterns)))]
        (throw (ex-info "Task-selection pattern has no interpretation"
                        {:status :refused :reason :missing-interpretation :patterns (vec missing)})))
      {:id id :kind :cascade-candidate :target pick :patterns units
       :precedence {:co-apply {:units units :descent []
                               :patterns (into {} (map (fn [[p t :as u]] [u (pattern-record t (interps p))]))
                                               units)}}})))

(defn score
  "Canonical G for every menu cascade and for stopping.
  ENTRIES: [{:id :patterns :pick :relation-class}]; FACTS: target -> fact map.
  INTERPS defaults to the library's executable patterns; a makeup may add its own
  (fixtures state their synthetic patterns' interpretations explicitly).
  Returns {:by-id {id entry} :stopping entry :horizon h :refusal r}."
  ([entries facts] (score entries facts interpretations))
  ([entries facts interps]
  (let [stop-id ::stopping
        all (conj (vec entries) {:id stop-id :patterns [] :pick :no-pick})
        interps (merge interps *extra-interpretations*)
        cands (mapv #(candidate interps %) all)
        picks (distinct (keep #(let [p (:pick %)] (when-not (or (nil? p) (= :no-pick p)) p)) entries))
        state (into #{} (mapcat #(fact-tokens % (get facts %))) picks)
        h (max 1 (min scorer/max-horizon
                      (reduce max 1 (map #(count (get-in % [:precedence :co-apply :units])) cands))))
        universe (set/union state #{stop-acceptance}
                            ;; every pick's acceptance, even when no pattern of its
                            ;; cascade produces it (the channel requires acceptance in U)
                            (into #{} (map #(vector % :can-begin)) picks)
                            (into #{} (mapcat (fn [c] (mapcat (fn [p] (concat (get-in p [:guard :clauses 0 :present])
                                                                              (:produces p) (:forbids p)))
                                                              (vals (get-in c [:precedence :co-apply :patterns])))))
                                  cands))
        model-for (fn [{:keys [id pick relation-class]}]
                    (if (or (nil? pick) (= :no-pick pick))
                      [id (shape/class-emission-model :no-pick universe #{stop-acceptance} h :unrelated
                                                      {:status :synthetic :source "selection stopping: the empty policy observes stop-the-line"})]
                      [id (shape/class-emission-model pick universe #{[pick :can-begin]} h
                                                      (get class-of-relation relation-class :unknown)
                                                      {:status :synthetic :source "selection class route: pick class once can-begin holds (Joe 2026-11-13)"})]))
        schedule (into {} (for [tau (range 1 (inc h))]
                            [tau (if (= tau h) shape/terminal-class-preference {:ending/not-yet-evaluated 1})]))
        opts {:horizon-steps h
              :pattern-counts (into {} (map (fn [c] [(:id c) (into {} (map #(vector % [1 1]))
                                                                   (get-in c [:precedence :co-apply :units]))]))
                                    cands)
              :observation-model-of (into {} (map model-for all))
              :authorized-class-preference schedule
              :require-ruled-class-kernel? true
              :prediction-context {:occurrence-id [:selection-comparison (mapv :id all)] :tau h}}
        results (canonical/compare-at-horizon {:cascade-belief {state 1}} cands opts)
        rows (if (vector? results) results (:candidates results))
        by-id (into {} (map (juxt :cascade-id identity)) rows)]
    {:by-id (dissoc by-id stop-id) :stopping (get by-id stop-id) :horizon h
     :refusal (when-not (vector? results) results)})))
