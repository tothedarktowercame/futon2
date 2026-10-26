(ns futon2.aif.q2-construction
  "Requirements Q2 on the production route (codex-68 ruling (c), bounded by
  Q8 eligibility, 2026-10-16). Two layers, on either side of the outer
  choice.

  Outer action coverage. meta-live-outer-selector/select-live calls
  `outer-actions` with the exact candidates its effective selection
  compared: every candidate the selector did not exclude as infeasible
  carries one task-level action, as the receipt's chosen :action is formed.
  These are the tasks Q8's census calls eligible. A task the selector
  excluded carries no action; Q8's census accounts for it.

  Selected-task construction. After the outer choice the inner field is
  formed for the selected task only (war_machine: \"inner cascade
  construction is target-local\"). `construction-census` records, per
  family in that field, the reading's excerpt and analysis digests, the
  graph pin, the retraction and kernel parameters, the query-time slice
  (each pattern with the library sha256 the reading cited), every
  structurally distinct policy's arranged key and every construction
  failure, together with the whole pattern library pinned by bytes at
  construction. Nothing is recorded for an unselected task, because
  nothing was constructed for one.

  `certificate` joins the two with the receipt's chosen action. It derives
  the pool (the patterns the keys arrange), the policy count (distinct
  valid keys) and slice-from-whole-library (every slice pin equal to the
  library manifest's) from the retained pins, never from a stored verdict.
  The runner seals it into the record; `verify-record` recomputes it from
  the record alone and the exporter reads only a verified carrier."
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.previous-run :as previous-run]
            [futon2.aif.wm.family-selection :as family-selection])
  (:import [java.nio.file Files]))

(def outer-schema :wm/q2-outer-actions-v1)
(def census-schema :wm/q2-construction-census-v1)
(def certificate-schema :wm/q2-certificate-v1)
(def library-schema :wm/pattern-library-manifest-v1)

(def seal previous-run/seal)
(def intact? previous-run/intact?)
(def digest evidence/value-digest)

;; -- outer action coverage, at selection ----------------------------------------

(defn- infeasible? [candidate]
  (= :infeasible (get-in candidate [:support :automated-feasibility])))

(defn outer-actions
  "Sealed outer action coverage for one outer selection. CANDIDATES are the
  exact candidates the effective SELECTION compared; ACTION-OF forms a
  candidate's task-level action as the receipt forms its chosen :action."
  [{:keys [candidates selection action-of]}]
  (seal {:schema outer-schema
         :support (vec (sort (map (comp str :id) candidates)))
         :excluded (vec (sort (map (comp str :id) (filter infeasible? candidates))))
         :actions (vec (sort-by :task
                                (for [c (remove infeasible? candidates)
                                      :let [a (action-of c)]]
                                  {:task (str (:id c)) :action a :action-id (digest a)})))
         :selection (select-keys selection [:status :selected :reason])}))

;; -- the chosen task's outer action beside the menu row that chose it ---------

(def provenance-schema :wm/q2-outer-action-provenance-v1)

(defn outer-action-provenance
  "Record only (decision sheet 2026-10-26 item 2, option (a); the R5
  residual). The chosen task's outer action is recorded beside the action pin
  of the sealed-menu row the canonical selection chose, so a reader can see
  which policy row stood behind the task-level action. No constraint and no
  refusal: a typed absence is the whole answer when there is no canonical
  selection, and the one comparison made is written down as a fact
  (:q2-names-the-menu-row?), never acted on.

  OUTER is the receipt's :q2-outer-actions in either form it takes: on the
  ordinary route `{:status :certified :carrier recomputation}`, whose :q2 is
  the carrier's task-level outer action (selected-outer-cascade-policy
  seal-canonical); on the injury route the sealed per-task census from
  `outer-actions` above. SELECTION is the receipt's
  :canonical-policy-selection (outer-canonical-selection/evaluate), MENU-PIN
  the sealed menu's pin."
  [{:keys [outer selection menu-pin]}]
  (if-not (= :selected (:status selection))
    {:schema provenance-schema :status :absent
     :reason :no-canonical-policy-selection
     :selection-status (:status selection)
     :selection-reason (:reason selection)}
    (let [target (str (get-in selection [:action :target]))
          menu-row (select-keys (:action selection) [:target :policy-id :cascade-digest])
          base {:schema provenance-schema :status :recorded :relation :record-only
                :task target
                :menu-action-pin {:policy-id (:policy-id menu-row)
                                  :cascade-digest (:cascade-digest menu-row)
                                  :action-pin (get-in selection [:selected :action-pin])}
                :menu-pin menu-pin}]
      (cond
        (= :certified (:status outer))
        (let [q2 (get-in outer [:carrier :q2])]
          (assoc base :outer-form :certified-carrier
                 :outer-action q2 :action-id (some-> q2 digest)
                 :q2-names-the-menu-row? (= q2 menu-row)))

        (and (= outer-schema (:schema outer)) (intact? outer))
        (if-let [row (first (filter #(= target (:task %)) (:actions outer)))]
          (assoc base :outer-form :sealed-outer-actions
                 :outer-action (:action row) :action-id (:action-id row)
                 :outer-sha256 (:sha256 outer))
          {:schema provenance-schema :status :absent
           :reason :chosen-task-not-in-outer-actions :task target})

        :else
        {:schema provenance-schema :status :absent
         :reason :outer-actions-absent-or-tampered
         :outer-status (:status outer)}))))

;; -- the whole library, pinned at construction ----------------------------------

(defn library-manifest
  "Every pattern file under ROOT (<root>/<pattern>.flexiarg, the file
  selected-reading pins), by pattern id, with its bytes' sha256; or a typed
  absence."
  [root]
  (let [dir (io/file (str root))]
    (if-not (.isDirectory dir)
      {:schema library-schema :status :absent :reason :library-root-absent :root (str root)}
      (let [prefix (str (.getCanonicalPath dir) "/")
            patterns (into (sorted-map)
                           (for [^java.io.File f (file-seq dir)
                                 :when (and (.isFile f) (str/ends-with? (.getName f) ".flexiarg"))
                                 :let [path (.getCanonicalPath f)]]
                             [(str/replace (subs path (count prefix)) #"\.flexiarg$" "")
                              (identity/sha256 (Files/readAllBytes (.toPath f)))]))]
        (if (empty? patterns)
          {:schema library-schema :status :absent :reason :library-empty :root (str root)}
          {:schema library-schema :status :pinned :root (str root)
           :count (count patterns) :sha256 (digest patterns) :patterns patterns})))))

;; -- the selected task's construction census ------------------------------------

(defn- keyed? [k] (= :wm/reading-policy-v1 (first k)))

(defn- census-entry [family]
  (let [prov (:provenance family)
        ks (map #(family-selection/constructed-key % nil) (:policies family))]
    {:task (str (:target-id family))
     :status (:status family)
     :reading (select-keys prov [:excerpt-digest :analysis-digest])
     :graph-digest (:graph-digest prov)
     :kernel {:retraction (:retraction prov)
              :transition-kernel family-selection/kernel-version}
     :slice (vec (:slice prov))
     :policies (vec (sort-by pr-str (distinct (filter keyed? ks))))
     :failures (into (vec (:failures family))
                     (for [k (remove keyed? ks)]
                       {:kind :policy-arrangement-not-keyable :policy-id (nth k 2)}))}))

(defn construction-census
  "The sealed construction census of the inner FIELD (families-for-field's
  result, policies included) with the LIBRARY manifest pinned at
  construction. A field that was not formed is recorded as such."
  [field library]
  (seal (if (not= :formed (:status field))
          {:schema census-schema :status :absent
           :reason (or (:reason field) :target-policy-field-not-formed)}
          {:schema census-schema :status :recorded
           :library library
           :graph (:graph field)
           :entries (mapv census-entry (:families field))})))

;; -- the record carrier ----------------------------------------------------------

(defn- pool-of [policies]
  (vec (sort (distinct (for [k policies [_ pattern] (:nodes (nth k 3))] pattern)))))

(defn- from-library? [library slice]
  (boolean (and (= :pinned (:status library)) (seq slice)
                (every? (fn [{:keys [pattern source-sha256]}]
                          (and (some? source-sha256)
                               (= source-sha256 (get-in library [:patterns pattern]))))
                        slice))))

(defn- field-binding
  "Why CENSUS does not record the field the decision kept (FIELD-CENSUS,
  family-selection/field-census), or nil."
  [census field-census]
  (let [families (:families field-census)
        entries (:entries census)]
    (cond
      (not= (:graph census) (:graph field-census)) :construction-graph-pin-not-the-field's
      (not= (map (comp str :target-id) families) (map :task entries)) :construction-tasks-not-the-field's
      :else
      (first
       (for [[f e] (map vector families entries)
             :let [prov (:provenance f)
                   reported (set (map :policy-id (:policies f)))
                   recorded (set (map #(nth % 2) (:policies e)))]
             reason [(when-not (= (select-keys prov [:excerpt-digest :analysis-digest]) (:reading e))
                       :construction-reading-not-the-field's)
                     (when-not (= (:graph-digest prov) (:graph-digest e))
                       :construction-graph-not-the-field's)
                     (when-not (= (vec (:slice prov)) (:slice e)) :construction-slice-not-the-field's)
                     (when-not (= (:retraction prov) (get-in e [:kernel :retraction]))
                       :construction-kernel-not-the-field's)
                     (when-not (set/subset? (set (:failures f)) (set (:failures e)))
                       :construction-failure-hidden)
                     (when-not (set/subset? recorded reported) :construction-policy-not-in-field)
                     (when-not (= (count reported)
                                  (+ (count (:policies e))
                                     (count (filter #(= :policy-arrangement-not-keyable (:kind %))
                                                    (:failures e)))))
                       :construction-policy-omitted)]
             :when reason]
         reason)))))

(defn chosen-action
  "The action the decision chose: the compared (posterior) entry whose id and
  target the record's :chosen names. Not the posterior's mode
  (:per-policy-argmax): the selector picks by the action marginal, which
  pools cascades sharing a co-apply root and can overrule the mode. nil
  when nothing was chosen; {:status :not-compared ...} when :chosen names
  no compared entry."
  [decision]
  (let [{:keys [id target]} (:chosen decision)]
    (when (some? id)
      (or (some #(when (and (= id (:id %)) (= target (:target %))) %)
                (keys (get-in decision [:selection-law :posterior])))
          {:status :not-compared :id id :target target}))))

(defn- not-compared? [action] (= :not-compared (:status action)))

(defn- chosen-reading
  "The excerpt and analysis digests the chosen action's interpretation debt
  carries, or nil."
  [decision]
  (some-> (:interpretation-debt (chosen-action decision))
          (select-keys [:excerpt-digest :analysis-digest])))

(defn certificate
  "The sealed Q2 carrier from OUTER (the outer receipt's :q2-outer-actions),
  RECEIPT-ACTION (the receipt's chosen :action) and DECISION (its
  :q2-construction census and :target-policy-field census, and its chosen
  action)."
  [{:keys [outer receipt-action decision]}]
  (let [cert (:selection-certificate decision)
        census (:q2-construction cert)
        outer-ok? (and (= outer-schema (:schema outer)) (intact? outer))
        census-ok? (and (= census-schema (:schema census)) (intact? census))
        recorded? (and census-ok? (= :recorded (:status census)))
        library (:library census)
        chosen-target (:target receipt-action)
        entry-of (first (filter #(= (str chosen-target) (:task %)) (:entries census)))
        reasons (cond-> []
                  (not outer-ok?) (conj :outer-actions-absent-or-tampered)
                  (not census-ok?) (conj :construction-census-absent-or-tampered)
                  (and census-ok? (not recorded?)) (conj (:reason census))
                  (nil? receipt-action) (conj :no-outer-action-chosen)
                  recorded? (into (keep identity [(field-binding census (:target-policy-field cert))]))
                  (and receipt-action (chosen-action decision)
                       (not= (str chosen-target) (str (:target (chosen-action decision)))))
                  (conj :decision-target-not-the-outer-action's)
                  (not-compared? (chosen-action decision))
                  (conj :chosen-action-not-compared)
                  (and recorded? entry-of (chosen-reading decision)
                       (not= (chosen-reading decision) (:reading entry-of)))
                  (conj :construction-reading-not-the-chosen-action's))]
    (seal (cond-> {:schema certificate-schema
                   :status (if (empty? reasons) :certified :not-recomputable)
                   :bindings {:outer (:sha256 outer) :census (:sha256 census)
                              :field (digest (:target-policy-field cert))
                              :receipt-action (some-> receipt-action digest)}}
            (seq reasons) (assoc :reasons reasons)
            (empty? reasons)
            (assoc :outer-actions (mapv #(select-keys % [:task :action-id]) (:actions outer))
                   :library {:count (:count library) :sha256 (:sha256 library)}
                   :entries
                   (vec (for [e (:entries census)]
                          {:task (:task e)
                           ;; the chosen action, for the chosen task's
                           ;; construction only; anything else serves none
                           :action-id (when (= (str chosen-target) (:task e))
                                        (digest receipt-action))
                           :reading-id (when (every? some? (vals (:reading e)))
                                         (digest (:reading e)))
                           :library-id (:sha256 library)
                           :graph-id (get-in census [:graph :pin :sha256])
                           :kernel-id (when (some? (get-in e [:kernel :retraction]))
                                        (digest (:kernel e)))
                           :slice (vec (sort (distinct (map :pattern (:slice e)))))
                           :pool (pool-of (:policies e))
                           :slice-from-whole-library (from-library? library (:slice e))
                           :policies (:policies e)
                           ;; what the census recorded; the census dedups
                           :policy-count (count (:policies e))
                           :failures (:failures e)})))))))

(defn record-inputs
  "The certificate's inputs, read from a persisted RECORD alone."
  [record]
  {:outer (get-in record [:outer-task-selection :q2-outer-actions])
   :receipt-action (get-in record [:outer-task-selection :action])
   :decision (:decision record)})

(defn verify-record
  "The record's own :q2 carrier when it is intact and equals the carrier
  recomputed from the record; else a typed not-recomputable reason."
  [record]
  (let [q2 (:q2 record)]
    (cond
      (nil? q2) {:status :not-recomputable :reason :q2-carrier-absent}
      (not (and (= certificate-schema (:schema q2)) (intact? q2)))
      {:status :not-recomputable :reason :q2-carrier-tampered}
      (not= q2 (certificate (record-inputs record)))
      {:status :not-recomputable :reason :q2-carrier-not-bound-to-record}
      (not= :certified (:status q2))
      {:status :not-recomputable :reason (first (:reasons q2))}
      :else {:status :verified :carrier q2})))

;; -- a mirror of Requirements.Q2, for tests ------------------------------------

(defn lean-q2
  "Requirements.Q2 over Clojure values: CARRIER (a verified :q2 carrier),
  Q8 (a verified :q8 carrier) and COMPARED (the Q4 carrier's compared
  keys). Mirrors the Lean definition clause by clause; Lean remains the
  authority."
  [carrier q8 compared]
  (let [eligible (set (get-in q8 [:census :eligible]))
        selected (get-in q8 [:selection :selected :task])
        actions (:outer-actions carrier)
        entries (:entries carrier)]
    (boolean
     (and
      (every? #(eligible (:task %)) actions)
      (every? (fn [t] (some #(= t (:task %)) actions)) eligible)
      (= (count actions) (count eligible))
      (apply distinct? (or (seq (map :action-id actions)) [nil]))
      (eligible selected)
      (= (get-in q8 [:cascade :selected-task]) selected)
      (= 1 (count entries))
      (every? (fn [e]
                (and (= #{selected} #{(:task e)})
                     (some #(and (= selected (:task %)) (= (:action-id %) (:action-id e))) actions)
                     (every? some? ((juxt :reading-id :library-id :graph-id :kernel-id) e))
                     (= (set (:pool e)) (set (:slice e)))
                     (true? (:slice-from-whole-library e))
                     (<= (count (:slice e)) (get-in carrier [:library :count] 0))
                     (pos? (:policy-count e))
                     (= (count (set (:policies e))) (:policy-count e))
                     (= (set (:policies e)) (set (get-in q8 [:cascade :constructed])))
                     (set/subset? (set compared) (set (:policies e)))))
              entries)))))
