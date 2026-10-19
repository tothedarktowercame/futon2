(ns futon2.aif.outer-policy-evidence
  "Pure digest-keyed evidence join for sealed outer Cascade Live policies."
  (:require [futon2.aif.load-identity :as identity]
            [futon2.aif.meta-outcome-dataset :as outcomes]
            [futon2.aif.parameter-novelty :as novelty]
            [futon2.aif.wm.executable-cascade :as xc]
            [futon2.aif.wm.family-selection :as family-selection]))

(identity/register! *ns* *file*)

(def schema :wm/outer-policy-evidence-v1)
(def component-names [:resource :cadence :continuation])
(def evidence-body-keys
  [:schema :status :menu-pin :declaration-pin :dataset-pin
   :outcome-vocabulary :preference :preference-authority :gamma :entries])

(defn- pin? [x] (and (string? (:path x)) (string? (:sha256 x))
                     (boolean (re-matches #"[0-9a-f]{64}" (:sha256 x)))))
(defn- finite? [x] (and (number? x) (Double/isFinite (double x))))
(defn- declaration-pin [d]
  (identity/sha256 (.getBytes (pr-str (dissoc d :pin)) "UTF-8")))
(defn- dataset-pin [rows]
  {:path "wm://outer-policy-terminal-dataset-v1" :authority :durable-run-records
   :sha256 (identity/sha256 (.getBytes (pr-str rows) "UTF-8"))})

(defn evidence-pin [carrier]
  {:path "wm://outer-policy-evidence-v1"
   :sha256 (identity/sha256
            (.getBytes (pr-str (select-keys carrier evidence-body-keys)) "UTF-8"))})

(defn verify-evidence [carrier]
  (try
    (let [entries (:entries carrier) vocab (:outcome-vocabulary carrier)
          preference (:preference carrier) c (:distribution preference)
          ids (mapv :policy-id entries) digests (mapv :cascade-digest entries)
          row-error
          (some
           (fn [row]
             (let [alpha (:posterior-alpha row)
                   q (get-in row [:posterior-predictive :distribution])
                   eig (:eig row) components (:ln-e-components row)
                   values (mapv #(get-in components [% :value]) component-names)
                   recomputed (when (and (vector? alpha) (every? #(and (finite? %) (pos? %)) alpha))
                                (novelty/categorical-information vocab alpha))]
               (cond
                 (not (and (string? (:policy-id row)) (string? (:target-id row))
                           (string? (:cascade-digest row)))) :row-identity
                 (not (and (vector? alpha) (= (count vocab) (count alpha))
                           (every? #(and (finite? %) (pos? %)) alpha))) :posterior-alpha
                 (not= vocab (get-in row [:posterior-predictive :vocabulary])) :posterior-vocabulary
                 (not (and (vector? q) (= (count vocab) (count q))
                           (every? #(and (finite? %) (pos? %)) q)
                           (<= (abs (- 1.0 (reduce + q))) 1.0e-10))) :posterior-predictive
                 (let [n (double (reduce + alpha))]
                   (some true? (map #(> (abs (- (double %1) (/ (double %2) n))) 1.0e-10)
                                    q alpha))) :posterior-alpha-mismatch
                 (not (and (= :computed (:status eig)) (= :nats (:units eig))
                           (finite? (:nats eig)) (= :computed (:status recomputed))
                           (<= (abs (- (:nats eig) (:nats recomputed))) 1.0e-10))) :eig
                 (not-every? #(and (contains? components %)
                                   (finite? (get-in components [% :value]))
                                   (= :nats (get-in components [% :units]))
                                   (pin? (get-in components [% :source-pin]))) component-names)
                 :ln-e-components
                 (not (and (finite? (:ln-e row))
                           (<= (abs (- (:ln-e row) (reduce + values))) 1.0e-10))) :ln-e
                 :else nil))) entries)]
      (cond
        (not= schema (:schema carrier)) :evidence-schema
        (not= :computed (:status carrier)) :evidence-not-computed
        (not (pin? (:menu-pin carrier))) :menu-pin
        (not (pin? (:declaration-pin carrier))) :declaration-pin
        (not (pin? (:dataset-pin carrier))) :dataset-pin
        (not (pin? (:evidence-pin carrier))) :evidence-pin
        (not= (evidence-pin carrier) (:evidence-pin carrier)) :evidence-pin-mismatch
        (not (and (vector? vocab) (seq vocab)
                  (= (count vocab) (count (distinct vocab))))) :outcome-vocabulary
        (not (and (map? preference) (= vocab (:vocabulary preference))
                  (= :probability (:units preference)))) :preference
        (not (and (vector? c) (= (count vocab) (count c))
                  (every? #(and (finite? %) (pos? %)) c)
                  (<= (abs (- 1.0 (reduce + c))) 1.0e-10))) :preference-distribution
        (not (pin? (:preference-authority carrier))) :preference-authority
        (not= (:preference-authority carrier) (:authority preference)) :preference-authority-mismatch
        (not (and (finite? (:gamma carrier)) (pos? (:gamma carrier)))) :gamma
        (not (and (vector? entries) (seq entries))) :empty-policy-evidence
        (not= (count ids) (count (distinct ids))) :duplicate-policy-id
        (not= (count digests) (count (distinct digests))) :duplicate-cascade-digest
        row-error row-error
        :else nil))
    (catch Throwable _ :malformed-policy-evidence)))

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
        preference (:preference declaration)
        masses (:distribution preference)
        mass-sum (reduce + 0.0 (filter number? masses))
        sealed-error
        (cond
          (family-selection/verify-sealed-menu sealed)
          (family-selection/verify-sealed-menu sealed)
          (not (pin? (:field-source-pin sealed))) :field-source-pin
          (some (fn [e]
                  (let [record (:canonical-cascade e)]
                    (or (not (string? (:policy-id e)))
                        (not (string? (:target-id e)))
                        (not (string? (:cascade-digest e)))
                        (not= (:cascade-digest e) (:digest record))
                        (not= (:cascade-digest e) (xc/digest record))
                        (not (xc/certified? (xc/validate-cascade record)))))) entries)
          :sealed-entry-invalid
          :else nil)
        malformed-declaration
        (cond
          (not= :wm/outer-policy-evidence-declaration-v1 (:schema declaration)) :declaration-schema
          (not (and (vector? vocab) (seq vocab) (= (count vocab) (count (distinct vocab))))) :outcome-vocabulary
          (not (and (finite? (:gamma declaration)) (pos? (:gamma declaration)))) :gamma
          (not (pin? declared-pin)) :declaration-pin
          (not (pin? (:preference-authority declaration))) :preference-authority
          (not= (:preference-authority declaration) (:authority preference)) :preference-authority-mismatch
          (not= :probability (:units preference)) :preference-units
          (not= vocab (:vocabulary preference)) :preference-vocabulary
          (not (and (vector? masses) (= (count vocab) (count masses))
                    (every? #(and (finite? %) (pos? %)) masses))) :preference-mass
          (> (abs (- mass-sum 1.0)) 1.0e-10) :preference-normalization
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
              :posterior-predictive (when (seq posterior)
                                      (let [n (double (reduce + posterior))]
                                        {:vocabulary vocab :distribution (mapv #(/ (double %) n) posterior)
                                         :units :probability}))
              :eig (when (empty? absent) (novelty/categorical-information vocab posterior))
              :ln-e-components components
              :ln-e (when (every? #(finite? (get-in components [% :value])) component-names)
                      (reduce + (map #(get-in components [% :value]) component-names)))
              :excluded (mapv #(select-keys % [:run/id :selected-cascade-digest ::cause])
                              (remove #(= :task-outcome (get-in % [::cause :class])) classified))}))
         entries)
        missing (filterv #(seq (:absent-terms %)) rows)]
    (if (or sealed-error malformed-declaration (seq missing))
      {:schema schema :status :refused
       :reason (or sealed-error malformed-declaration :policy-evidence-incomplete)
       :missing (mapv #(select-keys % [:cascade-digest :absent-terms]) missing)}
      (let [body {:schema schema :status :computed :menu-pin (:menu-pin sealed)
                  :declaration-pin declared-pin :dataset-pin (dataset-pin records)
                  :outcome-vocabulary vocab :preference preference
                  :preference-authority (:preference-authority declaration)
                  :gamma (:gamma declaration) :entries rows}]
        (assoc body :evidence-pin (evidence-pin body))))))
