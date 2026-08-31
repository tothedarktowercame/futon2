#!/usr/bin/env bb
;; work_units.bb — generate the executable plan from measured state, scored against
;; delivery-lifecycle §0.14's seven automatability criteria.
;;
;; NOT a list: every unit carries its own executable acceptance and falsifier, so a
;; unit can be dispatched, run unattended, and validated without a reader.
;; Criteria (§0.14): 1 typed ports · 2 acceptance dry-run satisfiable · 3 executable
;; falsifier · 4 evidence has a named consumer · 5 reads pinned / absences loud ·
;; 6 blast radius bounded+reversible without arming · 7 decision surface pre-covered.
(require '[cheshire.core :as json] '[clojure.edn :as edn]
         '[clojure.string :as str] '[clojure.pprint :as pp])

(def F2 "/home/joe/code/futon2/")
(def contract (json/parse-string (slurp "/home/joe/code/mathlib4/DarkTower/WarMachine/holes-contract.json") true))
(def registry (edn/read-string (slurp (str F2 "checks/witness-registry.edn"))))
(def census   (edn/read-string (slurp (str F2 "holes/labs/wm-contract/edge-census.edn"))))

(defn wit [e] (let [w (:witnesses e)] (cond (string? w) [w] (coll? w) (vec w) :else [])))
(def bound (set (mapcat wit registry)))
(def holes (filter #(= "hole" (:kind %)) (:declarations contract)))
(def unbound (remove #(bound (:name %)) holes))

;; --- unit constructors -------------------------------------------------------
(defn bind-unit [h]
  (let [has-ev (boolean (:evidence h))]
    {:id (str "BIND-" (:name h))
     :kind :bind-hole
     :target (:name h)
     :owner-record (:owner h)
     :acceptance (str "checks/witness-registry.edn gains an entry naming " (:name h)
                      "; contract_lint passes at the current authority sha")
     :falsifier "the --negative control must exit 1 with the entry removed"
     :criteria {1 (if has-ev :met :gap)      ;; typed ports: evidence type names the input
                2 :met                        ;; acceptance is lint-pass, dry-run satisfiable
                3 :met                        ;; --negative control exists
                4 :met                        ;; consumer = contract_lint
                5 :met                        ;; entry pins contract-sha + run-sha
                6 :met                        ;; adds a row; reversible; no arming
                7 (if has-ev :met :split)}    ;; without an evidence type, someone must CHOOSE the witness
     :split-at (when-not has-ev "choose which check witnesses this hole — needs the record's owner")}))

(defn edge-unit [r]
  (let [sources (count (filter true? [(:drawn? r) (:derived? r) (:measured? r) (:sim? r)]))
        [a b] (:edge r)
        live? (:measured? r)]
    {:id (str "EDGE-" (name a) "-" (name b))
     :kind :specify-edge
     :target [a b]
     :sources sources
     :live? live?
     :acceptance "a hyper-edge instance whose ports are 0-freehand and which passes hyper_edge_exemplar_check"
     :falsifier "exemplar check --negative exits 1; freehand count > 0 fails the unit"
     :criteria {1 :met 2 :met 3 :met 4 :met
                5 (if live? :met :gap)        ;; a live edge can be READ; a dead one can only be proposed
                6 :met
                7 (if live? :met :split)}     ;; dead edges hit semantic forks (cf. R16->R2) -> operator ruling
     :split-at (when-not live?
                 "dead edge: if the pair finds a semantic fork (does the thing exist?), that is an operator ruling")}))

(def units
  (concat
   [{:id "HOLD-contract" :kind :governance
     :target "all 80 declarations" :acceptance "no declaration names a dead session as holder"
     :falsifier "grep the contract for the retired holder; must return 0"
     :criteria {1 :met 2 :met 3 :met 4 :met 5 :met 6 :met 7 :met}
     :note "mechanical; the single point of failure Joe named"}
    {:id "APEX-D1" :kind :standard
     :target "the evidence apex" :acceptance "UNDEFINED — this unit's job is to define it"
     :falsifier "none yet — that is the point"
     :criteria {1 :gap 2 :gap 3 :gap 4 :met 5 :met 6 :met 7 :gap}
     :note "cannot be automated: it must SAY what counts as evidence. Operator work, not agent work."}]
   (map bind-unit unbound)
   (map edge-unit (filter #(and (not (:specified? %))
                                (< 1 (count (filter true? [(:drawn? %) (:derived? %) (:measured? %) (:sim? %)]))))
                          (:rows census)))))

(defn score [u] (count (filter #(= :met %) (vals (:criteria u)))))
(def scored (map #(assoc % :score (score %)) units))
(def ready  (filter #(= 7 (:score %)) scored))
(def splits (filter #(and (< (:score %) 7) (:split-at %)) scored))

(spit (str F2 "holes/labs/wm-contract/work-units.edn")
      (with-out-str (pp/pprint {:as-of "2026-08-31" :generated-by "futon2/scripts/work_units.bb"
                                :criteria-source "futon4/holes/delivery-lifecycle.md §0.14"
                                :totals {:units (count scored) :ready-7of7 (count ready)
                                         :needs-split (count splits)
                                         :operator-only (count (filter #(= :standard (:kind %)) scored))}
                                :units (vec (sort-by (juxt (comp - :score) :id) scored))})))

(println "work units:" (count scored))
(println "  dispatchable unattended (7/7):" (count ready))
(println "  need a split at a decision point:" (count splits))
(println)
(println "by kind:" (pr-str (frequencies (map :kind scored))))
(println "score histogram:" (pr-str (into (sorted-map) (frequencies (map :score scored)))))
