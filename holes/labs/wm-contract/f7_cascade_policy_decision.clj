(ns f7-cascade-policy-decision
  "F7 -- inhabit the policy carrier at the CASCADE grain, on recorded state.

   WHAT THIS IS. FUNDAMENTALS.edn's :fundamental/machine-policy-carrier names
   its own falsifier: \"A runtime decision in which two or more cascades for
   one selected mission are constructed and scored\". f1_policy_family_census.bb
   ran that falsifier over the 889-decision corpus and it did not fire
   (runs/F1-machine-q/02-policy-family-census.edn, leg 2: 234 decisions carry an
   :apply-cascade candidate, 0 carry two distinct cascades for one target).
   This script MAKES it fire, using the machine's own code and no fixture:

     1. read one RECORDED decision out of data/wm-trace by its :run/id, and
        take the mission the machine actually committed to
        (:decision :action :target);
     2. build that mission's circumstance |psi> with the live lane's own
        builder, futon2.report.cascade-lane/mission->psi
        (scripts/futon2/report/cascade_lane.clj:306);
     3. construct the same-mission menu with
        futon2.report.cascade-lane/cascade-policy-menu-for (:102), which shells
        out to the real Python constructor cascade_serve.py once per
        coverage-saturation epsilon and admits only COMPLETE (untruncated),
        DISTINCT policies at the constructor's pool ceiling;
     4. score the menu with futon2.aif.cascade-prior/shadow-rank
        (src/futon2/aif/cascade_prior.clj:163), the existing dark softmax over
        -cascade-score with ln E(pi) from a COLD prior;
     5. write the decision record.

   READ-ONLY WITH RESPECT TO THE MACHINE. data/wm-trace is read and nothing
   under data/ is written, so this takes no RUN12 run lock; it is not a tick and
   no mission is enacted. The record's :enacted? is false and stays false: the
   production lane still constructs ONE cascade (cascade_lane.clj:80-91 is called
   once per target), and nothing here changes that or writes cascade-prior state.

   --single is the NEGATIVE CONTROL and it is not a plant: it runs the same
   pipeline restricted to the incumbent epsilon 0.15, which is the production
   lane's own shape, and so produces a genuine one-cascade decision that
   f7_cascade_choice_check.bb must refuse.

   Usage (from the futon2 repo root):
     clojure -M holes/labs/wm-contract/f7_cascade_policy_decision.clj OUT.edn
     clojure -M holes/labs/wm-contract/f7_cascade_policy_decision.clj OUT.edn --single
   Optional: --run-id UUID --trace PATH."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [futon2.aif.cascade-prior :as cascade-prior]
            [futon2.report.cascade-lane :as lane]))

(def default-trace
  "The most recent trace file in the corpus at the time of writing."
  "data/wm-trace/wm-trace-2026-09-04.edn")

(def default-run-id
  "The last decision recorded in that file. Pinned by :run/id rather than by
   index so appending to the file cannot silently move which decision this is."
  "c149f9de-669c-4817-9b0e-ed4aad77db79")

(def incumbent-epsilon
  "The production lane's coverage-saturation threshold (cascade_lane.clj:70-72,
   the 3-arity default). --single constructs at this threshold only."
  0.15)

(def reader-opts
  ;; Old records carry tagged literals; keep the tag visible rather than throw.
  {:default (fn [_tag value] value)})

(defn- read-records [path]
  (edn/read-string reader-opts (str "[" (slurp path) "]")))

(defn- record-by-run-id [records run-id]
  (first (filter #(= run-id (:run/id %)) records)))

(defn- candidate-row
  "One menu candidate as it goes into the record: the constructor's own output
   plus the identity futon2.aif.cascade-prior computes for it. Construction
   telemetry keeps the constructor's names and is never renamed into a score."
  [candidate]
  (assoc candidate :policy-key (cascade-prior/policy-key candidate)))

(defn- selection-row [ranked]
  (select-keys ranked [:policy-key :coverage-saturation-epsilon :cascade-score
                       :cascade-cost :cascade-prior-bias :cascade-selection-weight
                       :cascade-selection-score]))

(defn- score-menu
  "shadow-rank refuses a menu of fewer than two policies by design
   (cascade_prior.clj:176-179), which is the same refusal this script must be
   able to record rather than crash on."
  [state candidates]
  (when (>= (count candidates) 2)
    (let [ranked (cascade-prior/shadow-rank state candidates)]
      {:status (:status ranked)
       :tau (:tau ranked)
       :candidate-count (:candidate-count ranked)
       :ranked-candidates (mapv (comp selection-row candidate-row)
                                (:ranked-candidates ranked))
       :shadow-winner-policy-key (cascade-prior/policy-key (:shadow-winner ranked))
       :score-only-winner-policy-key (cascade-prior/policy-key (:score-only-winner ranked))
       :governed-by (:governed-by ranked)})))

(defn build-record [{:keys [trace run-id single?]}]
  (let [records (read-records trace)
        record (record-by-run-id records run-id)
        _ (when-not record
            (throw (ex-info "no recorded decision with that :run/id" {:trace trace :run-id run-id})))
        action (get-in record [:decision :action])
        target (:target action)
        _ (when-not (string? target)
            (throw (ex-info "the recorded decision committed to no mission target"
                            {:run-id run-id :action action})))
        psi (lane/mission->psi target)
        epsilons (if single? [incumbent-epsilon] lane/default-policy-menu-epsilons)
        menu (lane/cascade-policy-menu-for target psi {:epsilons epsilons})
        candidates (mapv candidate-row (:candidates menu))
        state (cascade-prior/initial-state)]
    {:schema :wm/cascade-policy-decision-v1
     :producer "futon2/holes/labs/wm-contract/f7_cascade_policy_decision.clj"
     :producer-contract
     {:constructs-cascades? true
      :scores-cascades? (>= (count candidates) 2)
      :selects-or-enacts? false
      :writes-machine-state? false
      :run-lock-taken? false
      :reads "data/wm-trace, read-only"}
     :basis
     {:trace-file trace
      :run-id run-id
      :recorded-timestamp (:timestamp record)
      :decision-target target
      :decision-type (:type action)
      :decision-rationale (:rationale action)
      :trace-schema-version (get-in record [:wm-version :trace-schema-version])
      :recorded-git-sha (get-in record [:wm-version :git-sha])}
     :psi psi
     :constructor
     {:via "futon2.report.cascade-lane/cascade-policy-menu-for"
      :at "futon2/scripts/futon2/report/cascade_lane.clj:102"
      :psi-builder "futon2.report.cascade-lane/mission->psi"
      :psi-builder-at "futon2/scripts/futon2/report/cascade_lane.clj:306"
      :script "futon3a/holes/labs/M-memes-arrows/cascade_serve.py"
      :epsilons epsilons
      :pool-budget 40
      :candidate-source (:candidate-source menu)}
     :scorer
     {:via "futon2.aif.cascade-prior/shadow-rank"
      :at "futon2/src/futon2/aif/cascade_prior.clj:163"
      :prior-state (cascade-prior/state-stats state)
      :prior-note "COLD. No cascade-prior state has ever been persisted, so ln E(pi) is uniform over the menu and the ranking is governed by the engineering score alone. That is honest: the habit leg has nothing to say yet."}
     :policy-grain :pattern-cascade
     :candidate-count (:candidate-count menu)
     :policy-choice? (:policy-choice? menu)
     :menu-status (:status menu)
     :candidates candidates
     :selection (score-menu state (:candidates menu))
     :enacted? false
     :not-done
     ["Nothing is selected or enacted from this menu: the record is DARK."
      "No cascade-prior state is persisted; the prior stays cold."
      "The production lane is unchanged and still constructs one cascade per target."
      "No ruling written: aif-equations.edn :choices and control-map-edges.edn :decisions untouched."]}))

(defn -main [& args]
  (let [out (first (remove #(re-find #"^--" %) args))
        opts {:trace (or (second (drop-while #(not= "--trace" %) args)) default-trace)
              :run-id (or (second (drop-while #(not= "--run-id" %) args)) default-run-id)
              :single? (boolean (some #{"--single"} args))}]
    (when-not out
      (binding [*out* *err*]
        (println "usage: f7_cascade_policy_decision.clj OUT.edn [--single] [--trace PATH] [--run-id UUID]"))
      (System/exit 2))
    (let [record (build-record opts)]
      (io/make-parents out)
      (with-open [w (io/writer out)]
        (binding [*out* w] (pprint/pprint record)))
      (printf "f7_cascade_policy_decision: %s | target %s | candidates %d | status %s | governed-by %s -> %s%n"
              (if (:single? opts) "single-epsilon" "frontier")
              (get-in record [:basis :decision-target])
              (:candidate-count record)
              (:menu-status record)
              (get-in record [:selection :governed-by])
              out)
      (flush))))

(apply -main *command-line-args*)
