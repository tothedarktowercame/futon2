#!/usr/bin/env bb
(require '[clojure.string :as str]
         '[clojure.java.shell :as sh]
         '[clojure.pprint :as pp])

(def root "/home/joe/code")
(def ruled-path (str root "/mathlib4/DarkTower/WarMachine/F12RuledCarrier.lean"))
(def arms-path (str root "/mathlib4/DarkTower/WarMachine/F12D1Arms.lean"))
(def support-path (str root "/mathlib4/DarkTower/WarMachine/F12SupportArm.lean"))
(def out-path (str root "/futon2/holes/labs/wm-contract/runs/F12-organise/17-ruled-carrier.edn"))
(def expected-clauses #{"osel" "oauth" "oattr" "o1" "o2" "o3" "o4"})

(defn body-between [s start end]
  (second (re-find (re-pattern (str "(?s)" start "(.*?)" end)) s)))

(defn signature [s name]
  (some-> (re-find (re-pattern (str "(?s)abbrev\\s+" name ".*?:=\\s*(.*?)(?:\\n\\n|structure)")) s)
          second str/trim (str/replace #"\s+" " ")))

(defn verdict [s]
  (let [body (body-between s "structure ConformantOrganiseRuled" "theorem noBootstrap")
        clauses (set (map second (re-seq #"(?m)^\s{2}(o(?:sel|auth|attr|[1-4]))\s*:" (or body ""))))]
    (and (= expected-clauses clauses)
         (str/includes? body ".nodes \\ (f t sel repo adm).addedByOrganise")
         (not (str/includes? body "fastForward (f t sel repo adm).nodes repo.standsOn"))
         (not (re-find #"\bsorry\b|^[ \t]*axiom\b|\bnative_decide\b" s)))))

(let [ruled (slurp ruled-path)
      arms (slurp arms-path)
      support (slurp support-path)
      conformant (body-between ruled "structure ConformantOrganiseRuled" "theorem noBootstrap")
      clauses (->> (re-seq #"(?m)^\s{2}(o(?:sel|auth|attr|[1-4]))\s*:" conformant)
                   (map second) sort vec)
      decls (count (re-seq #"(?m)^(?:abbrev|structure|def|theorem)\s+" ruled))
      holes-log (sh/sh "git" "-C" (str root "/mathlib4") "log" "-1" "--format=%H" "--"
                       "DarkTower/WarMachine/Holes.lean")
      plants [{:plant :remove-oattr
               :verdict (verdict (str/replace-first ruled #"(?m)^\s{2}oattr\s*:.*\n" ""))}
              {:plant :node-set-o3
               :verdict (verdict (str/replace ruled
                 ".nodes \\ (f t sel repo adm).addedByOrganise" ".nodes"))}
              {:plant :insert-sorry
               :verdict (verdict (str ruled "\ntheorem planted : True := by sorry\n"))}]
      report (sorted-map
               :check :F12-ruled-carrier
               :verdict (verdict ruled)
               :signatures (sorted-map
                 :ruled (signature ruled "RuledOrganiseType")
                 :arm-six (signature arms "armTwoOrganiseType")
                 :arm-four (signature support "SupportOrganiseType"))
               :clauses clauses
               :declaration-count decls
               :forbidden-counts (sorted-map
                 :sorry (count (re-seq #"\bsorry\b" ruled))
                 :axiom (count (re-seq #"(?m)^[ \t]*axiom\b" ruled))
                 :native-decide (count (re-seq #"\bnative_decide\b" ruled)))
               :o3 (sorted-map :no-bootstrap-difference? (str/includes? conformant "\\ (f t sel repo adm).addedByOrganise")
                               :node-set-reading? (str/includes? conformant "fastForward (f t sel repo adm).nodes repo.standsOn"))
               :holes-lean-head (str/trim (:out holes-log))
               :plants plants)]
  (when-not (and (:verdict report) (every? (comp false? :verdict) plants)
                 (zero? (:exit holes-log)))
    (binding [*out* *err*] (pp/pprint report))
    (System/exit 1))
  (.mkdirs (.getParentFile (java.io.File. out-path)))
  (spit out-path (with-out-str (pp/pprint report))))
