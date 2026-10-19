(ns futon2.aif.outer-policy-evidence
  "Pure digest-keyed evidence join for sealed outer Cascade Live policies."
  (:require [futon2.aif.load-identity :as identity]
            [futon2.aif.meta-outcome-dataset :as outcomes]
            [futon2.aif.parameter-novelty :as novelty]))

(identity/register! *ns* *file*)

(def schema :wm/outer-policy-evidence-v1)
(def component-names [:resource :cadence :continuation])

(defn- pin? [x] (and (string? (:path x)) (re-matches #"[0-9a-f]{64}" (:sha256 x))))
(defn- finite? [x] (and (number? x) (Double/isFinite (double x))))
(defn- declaration-pin [d]
  (identity/sha256 (.getBytes (pr-str (dissoc d :pin)) "UTF-8")))
(defn- dataset-pin [rows] (identity/sha256 (.getBytes (pr-str rows) "UTF-8")))

(defn join
  "Join SEALED policy entries to explicit DECLARATION and durable RECORDS.
  This function computes evidence only; it never scores or selects."
  [sealed declaration records]
  (let [entries (:entries sealed)
        vocab (:outcome-vocabulary declaration)
        by-digest (:policies declaration)
        global (:global-prior declaration)
        declared-pin (:pin declaration)
        actual-pin (declaration-pin declaration)
        malformed-declaration
        (cond
          (not= :wm/outer-policy-evidence-declaration-v1 (:schema declaration)) :declaration-schema
          (not (and (vector? vocab) (seq vocab) (= (count vocab) (count (distinct vocab))))) :outcome-vocabulary
          (not (and (finite? (:gamma declaration)) (pos? (:gamma declaration)))) :gamma
          (not (pin? (:preference-authority declaration))) :preference-authority
          (not= actual-pin (:sha256 declared-pin)) :declaration-pin
          :else nil)
        rows
        (mapv
         (fn [entry]
           (let [digest (:cascade-digest entry)
                 spec (get by-digest digest)
                 prior (or (:prior-alpha spec) (:alpha global))
                 components (:ln-e spec)
                 absent (cond-> []
                          (nil? spec) (conj :policy-declaration)
                          (not= (count vocab) (count prior)) (conj :prior-alpha)
                          (not-every? #(and (finite? %) (pos? %)) prior) (conj :prior-alpha)
                          (not-every? #(and (contains? components %)
                                           (finite? (get-in components [% :value]))
                                           (= :nats (get-in components [% :units]))
                                           (pin? (get-in components [% :source-pin])))
                                      component-names) (conj :ln-e-components))
                 matching (filter #(= digest (:selected-cascade-digest %)) records)
                 classified (map #(assoc % ::cause (outcomes/terminal-cause %)) matching)
                 task (filter #(= :task-outcome (get-in % [::cause :class])) classified)
                 unknown (remove (set vocab) (map :observed-outcome task))
                 counts (frequencies (map :observed-outcome task))
                 posterior (mapv + (or prior []) (mapv #(get counts % 0) vocab))]
             {:policy-id (:policy-id entry) :target-id (:target-id entry)
              :cascade-digest digest :absent-terms (cond-> absent (seq unknown) (conj :unknown-outcome))
              :prior-alpha prior :task-counts (mapv #(get counts % 0) vocab)
              :posterior-alpha posterior
              :eig (when (empty? absent) (novelty/categorical-information vocab posterior))
              :risk (:risk spec) :ambiguity (:ambiguity spec)
              :ln-e-components components
              :ln-e (when (every? #(finite? (get-in components [% :value])) component-names)
                      (reduce + (map #(get-in components [% :value]) component-names)))
              :excluded (mapv #(select-keys % [:run/id :selected-cascade-digest ::cause])
                              (remove #(= :task-outcome (get-in % [::cause :class])) classified))}))
         entries)
        missing (filterv #(seq (:absent-terms %)) rows)]
    (if (or malformed-declaration (not= :sealed (:status sealed)) (seq missing))
      {:schema schema :status :refused
       :reason (or malformed-declaration :policy-evidence-incomplete)
       :missing (mapv #(select-keys % [:cascade-digest :absent-terms]) missing)}
      {:schema schema :status :computed :menu-pin (:menu-pin sealed)
       :declaration-pin declared-pin :dataset-pin (dataset-pin records)
       :outcome-vocabulary vocab :gamma (:gamma declaration) :entries rows})))
