(ns futon2.aif.wm.construction-inputs
  "Construction inputs shared by the report and flight assembly."
  (:require [futon2.aif.cascade-problems :as cascade-problems]
            [futon2.aif.mission-registry :as mission-registry]
            [futon2.aif.want-interpretation :as want-interpretation]))

(defn flight-assembly-input
  "The tick's assembly input inside a flight (futon2.aif.flight): the
  flight's target is the only target, and its wants are the flight's.
  Later clicks continue the chosen target and do not re-select among all
  targets (PROOF-2a flight rule 4). With no FLIGHT, INPUT is unchanged."
  [flight input]
  (if flight
    (cond-> (-> input
        (assoc :targets [(:target flight)])
        (assoc-in [:sources :wants (:target flight)] (vec (:wants flight)))
        ;; wants the flight's source located itself (A-exits criteria):
        ;; their locators and observed values join the target's sources
        (update-in [:sources :locators (:target flight)] merge (:locators flight))
        (update-in [:sources :universes (:target flight)] merge (:universe flight))
        ;; a flight target the sources never declared (no hand source, no
        ;; checkbox) has no context, so beta-for would refuse it
        ;; :beta-not-declared. It gets the context mission_hole_wants gives
        ;; every mission-stated target (:WM), or the flight's own :context;
        ;; a context the sources already give it wins.
        (update :sources (fn [srcs]
                           (let [cf (:context-of srcs) t (:target flight)]
                             (assoc srcs :context-of
                                    (fn [x] (or (when (ifn? cf) (cf x))
                                                (when (= x t) (or (:context flight) :WM)))))))))
      (contains? flight :c) (assoc-in [:sources :c (:target flight)] (:c flight)))
    input))

(defn assemble-cascade-problems
  "Assemble the tick's sources, including per-target :query-time-slices,
  and retain its mission-hole census unchanged. A slice remains available to
  cascade-problems even when its target has no interpretation operators yet.
  Supplied sources without a census are marked absent, never recomputed from
  today's mission files. Admission refusals remain on the same assembly."
  [{:keys [sources] :as input}]
  (assoc (cascade-problems/assemble input)
         :target-sources (vec (or (:target-sources input) []))
         :mission-hole-coverage
         (or (:mission-hole-coverage sources)
             {:status :absent :reason :source-coverage-not-supplied})))

(defn target-source-declarations
  "Describe the already-enumerated TARGETS from the records which enumerated
  them. Sources are copied from records already read by the report; no path or
  item line is guessed from a target id. Conflicting byte authorities produce
  one typed conflict row retaining every claim."
  [targets {:keys [loaded-missions loaded-tickets declared-files proposals
                   declared-targets proposal-targets ticket-targets]}]
  (let [missions (into {} (map (juxt :id identity)
                               (mission-registry/open-missions loaded-missions)))
        tickets-by-id (group-by :id (filter mission-registry/live-ticket?
                                            (:tickets loaded-tickets)))
        declared-by-id (group-by :target declared-files)
        proposals-by-id (group-by :target proposals)
        declared (set declared-targets)
        proposals (set proposal-targets)
        tickets (set ticket-targets)]
    (mapv (fn [target]
            (let [mission (get missions target)
                  ticket-claims (mapv (fn [ticket]
                                        {:source-kind :item-section
                                         :source-path (:path ticket)
                                         :item-line (:item-line ticket)})
                                      (get tickets-by-id target))
                  declared-claims (mapv (fn [{:keys [path sha256]}]
                                          {:source-kind :inline-bytes :source-path path
                                           :source-sha256 sha256 :source-origin :declared})
                                        (get declared-by-id target))
                  proposal-claims
                  (vec (for [proposal (get proposals-by-id target)
                             :let [{:keys [path sha256]} (get-in proposal [:evidence :finding-source])]
                             :when path]
                         {:source-kind :inline-bytes :source-path path
                          :source-sha256 sha256 :source-origin :repair-proposal}))
                  ;; A target's enumerating category says which text defines it.
                  ;; Other records may describe a cascade or repair concerning
                  ;; the same target without becoming a competing source claim.
                  claims (cond
                           mission [{:source-kind :head :source-path (:path mission)}]
                           (and (tickets target) (seq ticket-claims)) ticket-claims
                           (and (declared target) (seq declared-claims)) declared-claims
                           (and (proposals target) (seq proposal-claims)) proposal-claims
                           :else [])
                  authorities (set (map (juxt :source-path :source-sha256) claims))]
              (cond
                (> (count authorities) 1)
                {:target-id target :source-kind :conflict :source-path nil
                 :source-absent :target-source-conflict :claims claims}

                (seq claims)
                (let [claim (first claims)]
                  (assoc claim :target-id target :source-absent
                         (cond
                           (nil? (:source-path claim)) :target-source-path-absent
                           (and (= :item-section (:source-kind claim))
                                (nil? (:item-line claim))) :target-item-line-absent
                           :else nil)))

                :else
                (let [kind (cond (tickets target) :item-section
                                 (declared target) :inline-bytes
                                 (proposals target) :inline-bytes
                                 mission :head
                                 :else :unknown)]
                  {:target-id target :source-kind kind :source-path nil
                   :source-absent :target-source-path-absent}))))
          targets)))

(def construction-move-cost
  {:value 0
   :authority {:by "claude-10" :date "2026-09-24" :ruling :none-found
               :reason "a construction move is computation inside the tick, not in G's units; cost 1 made the constructor decline M-aif-eig's plan (gain 0.33, :acting-worth-more)"
               :commit "891b4af6"}})

(defn construction-budget
  "The constructor's search bounds: the sources' declaration, else
  claude-10's bounds (no ruling found)."
  [sources]
  (if-let [b (:construction-budget sources)]
    {:value b :authority {:source :cascade-sources}}
    {:value {:max-moves 4 :max-expansions 20000}
     :authority {:by "claude-10" :date "2026-09-24" :ruling :none-found :commit "891b4af6"}}))

(defn resolve-cascade-horizon
  "The tick's common horizon over TARGETS (E-cascade-real D14/D16). A
  declared :horizon-steps wins. Otherwise it is computed: the largest number
  of admitted interpretations on any target, or the longest declared
  candidate order if that is longer, and at least 1. A plan applies each
  interpretation at most once, so at that horizon no plan over the admitted
  interpretations is cut short (:beyond-horizon cannot arise). One value for
  the whole family, since differing T in one family is :incommensurable-family.
  Replaces the fallback literal T=2 (p4ng 462aa79), which refused 4-step chains
  as unreachable and pre-empted the typed absence."
  [sources targets]
  (if-let [h (:horizon-steps sources)]
    {:value h :authority {:source :cascade-sources
                          :declarations (:horizon-steps-declarations sources)}}
    (let [per-target (into (sorted-map)
                           (for [t targets
                                 :let [n (count (get-in sources [:interpretations t :patterns]))
                                       longest (reduce max 0 (map (comp count :precedence)
                                                                  (get-in sources [:candidates t])))]
                                 :when (pos? (max n longest))]
                             [t (max n longest)]))]
      {:value (max 1 (reduce max 0 (vals per-target)))
       :authority {:source :computed
                   :rule "max over the tick's targets of admitted interpretations (or longest declared order); a plan applies each at most once"
                   :by "claude-10" :date "2026-09-24" :ruling :none-found
                   :per-target per-target}})))

(defn assemble-cascade-problems-with-published
  "assemble-cascade-problems after merging the interpretations the machine
  published for the input's targets (D11 part 3, futon2.aif.want-interpretation)
  and resolving the common horizon over the merged sources: a want answered
  through the request seam constructs on the next tick without anyone
  promoting it by hand, and the horizon counts its interpretation. The
  result carries :cascade-horizon."
  [store input]
  (let [merged (update input :sources want-interpretation/merge-published store (:targets input))
        horizon (resolve-cascade-horizon (:sources merged) (:targets merged))]
    (assoc (assemble-cascade-problems (assoc-in merged [:sources :horizon-steps] (:value horizon)))
           :cascade-horizon horizon)))
