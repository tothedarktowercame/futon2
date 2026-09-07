#!/usr/bin/env bb
(require '[clojure.edn :as edn]
         '[clojure.set :as set]
         '[clojure.string :as str]
         '[clojure.pprint :as pp])

(def root "/home/joe/code")
(def lean-path (str root "/mathlib4/DarkTower/WarMachine/F10RuledCarrier.lean"))
(def cohort-path (str root "/futon2/src/futon2/aif/full_loop_cohort.clj"))
(def seed-path (str root "/futon2/holes/labs/wm-contract/runs/D1-evidence/kl-worked-example.edn"))
(def out-path (str root "/futon2/holes/labs/wm-contract/runs/F10-outcome-domain/01-ruled-carrier.edn"))

(defn kebab->camel [k]
  (let [[x & xs] (str/split (name k) #"-")]
    (apply str x (map str/capitalize xs))))

(defn body-between [s a b]
  (second (re-find (re-pattern (str "(?s)" a "(.*?)" b)) s)))

(defn source-outcomes [s]
  (->> (second (re-find #"(?s)\(def outcome-kinds.*?#\{(.*?)\}\)" s))
       (re-seq #":[a-z][a-z0-9-]*")
       (map (comp keyword #(subs % 1))) set))

(defn lean-constructors [s]
  (->> (body-between s "inductive FlightDisposition where" "deriving DecidableEq, Repr")
       (re-seq #"\|\s*([A-Za-z][A-Za-z0-9]*)") (map second) set))

(defn list-members [s decl]
  (->> (body-between s (str "def " decl ".*?:=") "\n\n")
       (re-seq #"\.([A-Za-z][A-Za-z0-9]*)") (map second) vec))

(defn support-members
  "The support's members, resolved through whichever declared list the `support`
   field maps over.  Without this the support's WIDTH is ungated: narrowing it to
   the five observed dispositions keeps `normalised` true (the five masses still
   sum to 1), so neither this checker nor `lake env lean` objected."
  [s]
  (if-let [decl (second (re-find #"support := fun _ => ([A-Za-z][A-Za-z0-9.]*)\.map organisationOutcome" s))]
    (set (list-members s decl))
    #{}))

(defn mass-map [s]
  (into {} (for [[_ d n] (re-seq #"⟨\.organisations, \.([A-Za-z]+)⟩ => 1 / ([0-9]+)" s)]
             [d (double (/ 1 (parse-long n)))])))

(defn facts [lean cohort seed]
  (let [authority (source-outcomes cohort)
        expected (set (map kebab->camel authority))
        observed-kw (set (:outcomes seed))
        observed (set (map kebab->camel observed-kw))
        seeded (get-in seed [:c-candidates :C-seeded])
        expected-masses (into {} (map (fn [[k v]] [(kebab->camel k) v]) seeded))
        constructors (lean-constructors lean)
        all-members (set (list-members lean "FlightDisposition.all"))
        observed-decl (set (list-members lean "observedDispositions"))
        zeros-decl (set (list-members lean "namedZeroDispositions"))
        masses (mass-map lean)
        support-decl (support-members lean)]
    (sorted-map
      :authority-keywords (vec (sort authority))
      :expected-constructors (vec (sort expected))
      :constructors (vec (sort constructors))
      :all-members (vec (sort all-members))
      :support-members (vec (sort support-decl))
      :observed-source (vec (sort observed))
      :observed-declared (vec (sort observed-decl))
      :named-zeros-derived (vec (sort (set/difference expected observed)))
      :named-zeros-declared (vec (sort zeros-decl))
      :expected-positive-masses (into (sorted-map) expected-masses)
      :declared-positive-masses (into (sorted-map) masses)
      :forbidden-counts (sorted-map
                          :sorry (count (re-seq #"\bsorry\b" lean))
                          :axiom (count (re-seq #"(?m)^[ \t]*axiom\b" lean))
                          :native-decide (count (re-seq #"\bnative_decide\b" lean))))))

(defn verdict [f]
  (and (= (:constructors f) (:expected-constructors f))
       (= (:all-members f) (:expected-constructors f))
       (= (:support-members f) (:expected-constructors f))
       (= (:observed-declared f) (:observed-source f))
       (= (:named-zeros-declared f) (:named-zeros-derived f))
       (= (:declared-positive-masses f) (:expected-positive-masses f))
       (every? zero? (vals (:forbidden-counts f)))))

(let [lean (slurp lean-path) cohort (slurp cohort-path) seed (edn/read-string (slurp seed-path))
      base (facts lean cohort seed)
      plants [{:plant :drop-constructor
               :landed? (str/includes? lean "| cancelled")
               :verdict (verdict (facts (str/replace-first lean " | cancelled" "") cohort seed))}
              {:plant :zero-into-positive-set
               :landed? (str/includes? lean ".groundedChange, .incomplete")
               :verdict (verdict (facts (str/replace-first lean ".groundedChange, .incomplete"
                                     ".groundedChange, .groundedNoChange, .incomplete") cohort seed))}
              {:plant :change-mass
               :landed? (str/includes? lean "⟨.organisations, .groundedChange⟩ => 1 / 2")
               :verdict (verdict (facts (str/replace-first lean
                                     "⟨.organisations, .groundedChange⟩ => 1 / 2"
                                     "⟨.organisations, .groundedChange⟩ => 1 / 3") cohort seed))}
              {:plant :narrow-support
               :landed? (str/includes? lean "support := fun _ => FlightDisposition.all.map organisationOutcome")
               :verdict (verdict (facts (str/replace-first lean
                                     "support := fun _ => FlightDisposition.all.map organisationOutcome"
                                     "support := fun _ => observedDispositions.map organisationOutcome")
                                        cohort seed))}
              {:plant :append-sorry :landed? true
               :verdict (verdict (facts (str lean "\ntheorem planted : True := by sorry\n") cohort seed))}]
      report (sorted-map :check :F10-ruled-carrier :facts base :plants plants
                         :verdict (and (verdict base)
                                       (every? :landed? plants)
                                       (every? (comp false? :verdict) plants)))]
  (when-not (:verdict report)
    (binding [*out* *err*] (pp/pprint report))
    (System/exit 1))
  (.mkdirs (.getParentFile (java.io.File. out-path)))
  (spit out-path (with-out-str (pp/pprint report))))
