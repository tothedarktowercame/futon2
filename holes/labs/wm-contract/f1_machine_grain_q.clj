(ns f1-machine-grain-q
  "F1 slice 2 -- Q(o|pi) BUILT FROM THE MACHINE'S OWN CARRIERS, over the two
   cascades F7 constructed.

   WHAT IS NEW HERE, and it is one thing. Slice 1 proved the composition in
   Lean and slice 3 transcribed it into src/futon2/aif/machine_q.clj; both ran
   on carriers DECLARED for the demonstration, which under FUNDAMENTALS.edn's
   criterion makes them fixtures and inhabits nothing. This slice runs the same
   composition on values the shipped code computes from machine data:

     states       futon2.aif.belief/status-set          (belief.clj:42)
     alphabet     the observed-event keys of A          (belief.clj:199-206)
     A            futon2.aif.belief/observation-model-v1 (belief.clj:199-206)
     belief       the :mu-post of recorded run
                  c149f9de-669c-4817-9b0e-ed4aad77db79   (data/wm-trace)
     pi           the two cascades of
                  runs/F7-cascade-policy/f7-cascade-policy-decision.edn
     B            futon2.aif.belief/transition-model-v1  (belief.clj:216-231)
                  LIFTED to an action index -- see below

   THE LIFT IS THE WHOLE FINDING. machine_q wants B(s,u); the machine has only
   B(s) -- transition-model-v1 is 7x7 IDENTITY and carries no action index at
   all (belief.clj:216-231, and its own docstring says so: \"no explicit
   dynamics; the prior is the previous posterior\"). The only lift that adds no
   information is the constant one, B(s,u) = B(s) for every u. So the machine's
   Q(o|pi) is CONSTANT IN pi, and that is not a defect of this script: it is
   FUNDAMENTALS.edn :fundamental/controlled-transition-kernel, measured on a
   run instead of read off the code.

   THREE ARMS, and they separate two different reasons a row could fail to move.
     1 :arm/machine-plan -- plan(pi) is the machine's OWN action identity for
       the recorded decision (futon2.aif.habit-prior/policy-key, which excludes
       cascade payloads by design, habit_prior.clj:29-38). Both cascades plan
       the same step, so the rows must coincide by rowsEqualOfEqualPlans.
     2 :arm/cascade-plan -- each cascade plans ITSELF (u = its cascade-grain
       policy-key, cascade_prior.clj:48-65), so the plans DIFFER. The rows still
       coincide, because B ignores u. Arm 2 minus arm 1 is the isolation: the
       missing object is B's dependence on u, not the plan's on pi.
     3 :arm/declared-B -- a DECLARED control, not an inhabitant, in which B
       reads u. The rows separate. It shows the composition is capable of the
       policy conditioning arms 1 and 2 do not exhibit, so their coincidence is
       a fact about the machine and not about machine_q.

   NO RULING. Arm 3's kernel is labelled :declared-control everywhere it
   appears; nothing here writes aif-equations.edn :choices or
   control-map-edges.edn :decisions, and no FUNDAMENTALS.edn verdict moves.

   READ-ONLY. Reads data/wm-trace and two committed receipts; writes one
   artifact under runs/F1-machine-q/. No tick, no run lock, nothing under data/
   written, no substrate call, no network. DETERMINISM: no wall-clock field;
   every number is computed from a committed file or a shipped def.

   Usage (from the futon2 repo root):
     clojure -M holes/labs/wm-contract/f1_machine_grain_q.clj OUT.edn
   Optional: --run-id UUID --trace PATH --menu PATH --single-menu PATH."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [futon2.aif.belief :as belief]
            [futon2.aif.cascade-prior :as cascade-prior]
            [futon2.aif.habit-prior :as habit-prior]
            [futon2.aif.machine-q :as machine-q]))

(def default-trace "data/wm-trace/wm-trace-2026-09-04.edn")

(def default-run-id
  "The decision F7 constructed its menu against, pinned by :run/id so appending
   to the trace cannot move which decision this is."
  "c149f9de-669c-4817-9b0e-ed4aad77db79")

(def default-menu
  "holes/labs/wm-contract/runs/F7-cascade-policy/f7-cascade-policy-decision.edn")

(def default-single-menu
  "F7's own negative control: the same pipeline at the production incumbent
   epsilon, which yields one cascade and so no policy choice."
  "holes/labs/wm-contract/runs/F7-cascade-policy/f7-single-cascade-decision.edn")

(def reader-opts {:default (fn [_tag value] value)})

(defn- read-records [path]
  (edn/read-string reader-opts (str "[" (slurp path) "]")))

(defn- record-by-run-id [records run-id]
  (first (filter #(= run-id (:run/id %)) records)))

;; ---------------------------------------------------------------------------
;; The carriers, each read from the machine

(defn- states [] (vec (sort belief/status-set)))

(defn- alphabet
  "The declared outcome alphabet: the observed events A has rows for."
  []
  (vec (sort (keys belief/observation-model-v1))))

(defn- observation-kernel
  "A as machine_q wants it. belief/observation-model-v1 is {o {s P(o|s)}} --
   column-normalised over o for fixed s (belief.clj:176-197) -- and machine_q's
   :observation is {s {o mass}}, so this is a transpose and nothing else."
  []
  (into {}
        (map (fn [s]
               [s (into {} (map (fn [o] [o (double (get-in belief/observation-model-v1 [o s] 0.0))]))
                        (alphabet))]))
        (states)))

(defn- machine-belief
  "The belief the machine held at the recorded decision. :mu-post is
   {entity-id {status mass}}; in this record all entities carry ONE
   distribution, so there is no entity to choose and none is chosen. Refuses if
   that ever stops being true, rather than picking one."
  [record]
  (let [posteriors (set (vals (:mu-post record)))]
    (when-not (= 1 (count posteriors))
      (throw (ex-info "the recorded belief carries more than one posterior; which entity's is Q(o|pi) about is a modelling choice this script does not make"
                      {:distinct-posteriors (count posteriors)
                       :entities (count (:mu-post record))})))
    {:distribution (into {} (map (fn [[k v]] [k (double v)])) (first posteriors))
     :entities (count (:mu-post record))
     :distinct-posteriors (count posteriors)}))

(defn- constant-lift
  "B(s,u) = transition-model-v1(s) for every u in `actions`. The machine's B
   has no action index (belief.clj:216-231); the constant lift is the only one
   that adds no information."
  [actions]
  (into {}
        (for [s (states) u actions]
          [[s u] (into {} (map (fn [s'] [s' (double (get-in belief/transition-model-v1 [s s'] 0.0))]))
                       (states))])))

(defn- declared-lift
  "THE DECLARED CONTROL, and not an inhabitant of anything. `u-identity` keeps
   the machine's identity B; every other u gets the column of A at s read as a
   state->state row -- machine numbers in a slot the machine does not fill,
   which is a category error and is the point: it exists only to show that when
   B reads u the rows separate."
  [actions u-identity]
  (into {}
        (for [s (states) u actions]
          [[s u]
           (if (= u u-identity)
             (into {} (map (fn [s'] [s' (double (get-in belief/transition-model-v1 [s s'] 0.0))])) (states))
             (into {} (map (fn [s'] [s' (double (get-in belief/observation-model-v1 [s' s] 0.0))])) (states)))])))

;; ---------------------------------------------------------------------------
;; The arms

(defn- run-arm
  "Build the model and reading, run the composition, and report what it did."
  [{:keys [id plan transition belief-map policies note]}]
  (let [model (machine-q/generative-model!
               {:states (states)
                :outcomes (alphabet)
                :transition transition
                :observation (observation-kernel)})
        reading (machine-q/q-reading! {:id id :plan plan :belief-mass (fn [b s] (get b s 0.0))} model)
        kernel (machine-q/predictive-outcome-kernel model reading belief-map)
        [pi-a pi-b] policies
        row-a (get-in kernel [:rows pi-a])
        row-b (get-in kernel [:rows pi-b])
        q-s (machine-q/predicted-state-distribution model reading belief-map pi-a)]
    {:arm id
     :note note
     :plans-differ (not= (get plan pi-a) (get plan pi-b))
     :rows {:pi-1 row-a :pi-2 row-b}
     :normalisation-residual (into {} (map-indexed (fn [i pi] [(if (zero? i) :pi-1 :pi-2)
                                                               (get (:normalisation-residual kernel) pi)]))
                                   policies)
     :max-abs-row-difference (reduce max 0.0 (map (fn [o] (Math/abs (- (double (get row-a o 0.0))
                                                                       (double (get row-b o 0.0)))))
                                                  (alphabet)))
     :rows-coincide (every? (fn [o] (< (Math/abs (- (double (get row-a o 0.0))
                                                    (double (get row-b o 0.0))))
                                       machine-q/tolerance))
                            (alphabet))
     :law/rows-equal-for-equal-plans (machine-q/rows-equal-for-equal-plans? kernel plan)
     :predicted-state-equals-belief
     (every? (fn [s] (< (Math/abs (- (double (get q-s s 0.0)) (double (get belief-map s 0.0))))
                        machine-q/tolerance))
             (states))}))

;; ---------------------------------------------------------------------------
;; The boundary, fed machine objects

(defn- feed
  "Offer `row` to the boundary and record what it said. `:accepted` is a
   verdict, not an absence of one, and `:refusal/untyped-throw` is recorded
   rather than smoothed away: a boundary that promises typed reasons and throws
   an untyped one has failed, and the receipt has to be able to say so."
  ([what row] (feed what row nil))
  ([what row reads]
   (cond-> {:fed what
            :refusal (try (machine-q/predictive-outcome-row! row (alphabet)) :accepted
                          (catch Exception t (or (machine-q/refusal-reason t)
                                                 :refusal/untyped-throw)))}
     reads (assoc :reads reads))))

(defn- boundary-controls [record menu accepted-row]
  [(feed "the F7 menu's own selection weights {policy-key -> cascade-selection-weight} -- Q(pi) at the cascade grain, runs/F7-cascade-policy/f7-cascade-policy-decision.edn :selection"
         (into {} (map (fn [c] [(:policy-key c) (:cascade-selection-weight c)]))
               (get-in menu [:selection :ranked-candidates])))
   (feed "one F7 cascade candidate map (a policy, offered as if it were a distribution over outcomes)"
         (first (:candidates menu)))
   (feed "the recorded :mu-post -- {entity-id -> distribution over states}, the belief itself"
         (:mu-post record))
   (feed "the recorded :observation -- the 14-channel observation vector"
         (:observation record))
   (feed "the recorded belief's single posterior -- a genuine distribution over the STATES, offered as a row over the OUTCOMES"
         (:distribution (machine-belief record))
         (str "ACCEPTED, and that is a limit of the boundary worth stating rather than a pass. "
              "The machine names its 7 observed events with the same 7 keywords as its lifecycle statuses "
              "(belief.clj:42 and belief.clj:199-206 have equal key sets), so a state distribution and an "
              "outcome row are indistinguishable to predictive-outcome-row!. The boundary checks shape, "
              "support and normalisation; it cannot check which carrier a well-formed row is over."))
   (feed "the machine-grain row this script computed (POSITIVE CONTROL)" accepted-row)])

;; ---------------------------------------------------------------------------

(defn- policy-summary [candidate recomputed]
  {:size (:size candidate)
   :coverage-saturation-epsilon (:coverage-saturation-epsilon candidate)
   :cascade-score (:cascade-score candidate)
   :policy-key-recorded (:policy-key candidate)
   :policy-key-recomputed recomputed
   :policy-key-agrees (= (:policy-key candidate) recomputed)})

(defn build-record [{:keys [trace run-id menu-path single-menu-path]}]
  (let [record (or (record-by-run-id (read-records trace) run-id)
                   (throw (ex-info "no recorded decision with that :run/id" {:trace trace :run-id run-id})))
        menu (edn/read-string reader-opts (slurp menu-path))
        single (edn/read-string reader-opts (slurp single-menu-path))
        candidates (:candidates menu)
        _ (when-not (= 2 (count candidates))
            (throw (ex-info "this slice needs the two-cascade menu" {:candidates (count candidates)})))
        recomputed (mapv cascade-prior/policy-key candidates)
        _ (when-not (= (mapv :policy-key candidates) recomputed)
            (throw (ex-info "a recorded cascade policy-key does not match the one cascade-prior computes for it"
                            {:recorded (mapv :policy-key candidates) :recomputed recomputed})))
        policies recomputed
        action (get-in record [:decision :action])
        u-machine (habit-prior/policy-key action)
        {:keys [distribution entities distinct-posteriors]} (machine-belief record)
        plan-machine (zipmap policies (repeat u-machine))
        plan-cascade (zipmap policies policies)
        arm-1 (run-arm {:id :machine-grain/machine-plan
                        :plan plan-machine
                        :transition (constant-lift [u-machine])
                        :belief-map distribution
                        :policies policies
                        :note "plan(pi) is the machine's own action identity for this decision; both cascades plan the same step"})
        arm-2 (run-arm {:id :machine-grain/cascade-plan
                        :plan plan-cascade
                        :transition (constant-lift policies)
                        :belief-map distribution
                        :policies policies
                        :note "each cascade plans itself; the plans differ and the machine's B ignores them"})
        arm-3 (run-arm {:id :machine-grain/declared-B
                        :plan plan-cascade
                        :transition (declared-lift policies (first policies))
                        :belief-map distribution
                        :policies policies
                        :note "DECLARED CONTROL. B reads u. Not an inhabitant of anything."})]
    {:schema :wm/machine-grain-q-v1
     :row :F1
     :slice "slice 2 of :F1 -- the machine-grain Q(o|pi) instance over the cascades F7 constructed. Slices 1 (Lean) and 3 (runtime seam) are landed; this slice changes no src/, no Lean and no census verdict."
     :at "2026-09-05"
     :producer "futon2/holes/labs/wm-contract/f1_machine_grain_q.clj"
     :producer-contract
     {:constructs-cascades? false
      :selects-or-enacts? false
      :writes-machine-state? false
      :run-lock-taken? false
      :reads "data/wm-trace and two committed F7 receipts, read-only"}
     :basis
     {:trace-file trace
      :run-id run-id
      :recorded-timestamp (:timestamp record)
      :decision-target (:target action)
      :decision-type (:type action)
      :menu menu-path
      :menu-candidate-count (:candidate-count menu)
      :menu-policy-choice? (:policy-choice? menu)}
     :carriers
     [{:carrier :state
       :provenance :machine-data
       :value "futon2.aif.belief/status-set -- 7 lifecycle statuses"
       :at "futon2/src/futon2/aif/belief.clj:42"}
      {:carrier :outcome-alphabet
       :provenance :machine-data
       :value "the 7 observed events A has rows for"
       :at "futon2/src/futon2/aif/belief.clj:199-206"}
      {:carrier :observation-kernel-A
       :provenance :machine-data
       :value "futon2.aif.belief/observation-model-v1, transposed to {s {o mass}}"
       :at "futon2/src/futon2/aif/belief.clj:199-206"
       :census "FUNDAMENTALS.edn :out/observation-kernel-instance -- runtime :inhabited"}
      {:carrier :belief
       :provenance :machine-data
       :value (str "the :mu-post of run " run-id " -- " entities
                   " entities carrying " distinct-posteriors " distinct posterior, so no entity is chosen")
       :at (str trace " :mu-post")}
      {:carrier :policy
       :provenance :machine-data
       :value "the two cascades of F7's menu, identified by futon2.aif.cascade-prior/policy-key recomputed here and compared with the recorded key"
       :at "futon2/holes/labs/wm-contract/runs/F7-cascade-policy/f7-cascade-policy-decision.edn"
       :census "FUNDAMENTALS.edn :fundamental/machine-policy-carrier -- runtime :inhabited by F7"}
      {:carrier :transition-kernel-B
       :provenance :machine-data-lifted
       :value "futon2.aif.belief/transition-model-v1 (7x7 identity, no action index) under the constant lift B(s,u) = B(s)"
       :at "futon2/src/futon2/aif/belief.clj:216-231"
       :census "FUNDAMENTALS.edn :fundamental/controlled-transition-kernel -- :in, uninhabited on BOTH legs"
       :reads "THIS IS THE ONE CARRIER THE MACHINE DOES NOT SUPPLY AT THE REQUIRED SIGNATURE. The lift is not a choice among several: it is the only one that adds no information to what belief.clj holds."}]
     :policies (mapv policy-summary candidates recomputed)
     :machine-action-identity
     {:policy-key u-machine
      :via "futon2.aif.habit-prior/policy-key"
      :at "futon2/src/futon2/aif/habit_prior.clj:29-38"
      :reads "Both cascades collapse to this one identity: the action grain excludes cascade payloads by design, which the docstring states."}
     :belief distribution
     :arms [arm-1 arm-2 arm-3]
     :finding
     {:machine-grain-rows-differ (not (:rows-coincide arm-2))
      :max-abs-row-difference-machine (:max-abs-row-difference arm-2)
      :max-abs-row-difference-declared-control (:max-abs-row-difference arm-3)
      :reads "Arm 1 and arm 2 give the same rows for both cascades. Arm 2 is the isolation: the plans DIFFER there and the rows still coincide, so what is missing is B's dependence on u, not the plan's on pi. Arm 3 shows the composition separates policies as soon as B reads u."}
     :negative-control
     {:what "F7's single-cascade record -- the production lane's own shape"
      :at single-menu-path
      :candidate-count (:candidate-count single)
      :menu-status (:menu-status single)
      :policy-conditioning :unavailable-single-policy-menu
      :reads "One policy is not a policy-conditioned anything. This is the receipt's classification of the record, not a machine_q refusal."}
     :boundary-refusals (boundary-controls record menu (get-in arm-1 [:rows :pi-1]))
     :boundary-repair
     {:what "predictive-outcome-row! threw ClassCastException instead of naming a reason, for any row carrying a non-numeric value."
      :found-by "feeding the recorded :mu-post and an F7 cascade candidate to the boundary in this slice"
      :cause "the row sum was computed eagerly in the let, so `double` ran before the :refusal/non-probability-mass branch could fire"
      :fix "src/futon2/aif/machine_q.clj:204 -- the sum is a delay, forced only in the :refusal/unnormalised branch at :221-223"
      :test "test/futon2/aif/machine_q_test.clj:159 -- malformed-rows-are-refused-with-typed-reasons-test, two added assertions (a map mass and a string mass)"
      :reads "The two :refusal/untyped-throw verdicts this receipt would have recorded before the fix are why :F1's acceptance says TYPED refusal."}
     :not-done
     ["No src/, no Lean, no test change: the composition is used as slice 3 shipped it."
      "No FUNDAMENTALS.edn verdict moves. :fundamental/controlled-transition-kernel stays :in, and this record is evidence for that entry rather than against it."
      "No entry written to aif-equations.edn :choices or control-map-edges.edn :decisions. Nothing here is a ruling."
      "Nothing is selected, scored or enacted; the cascade prior is untouched and stays cold."
      "gen_aif_dag.bb and gen_q_interface_table.bb not run; nothing regenerated into a publish (TN 9a)."
      "*seam-reading* is still bound nowhere in src/: this script binds nothing and the R4 seam stays dark."]}))

(defn -main [& args]
  (let [out (first (remove #(re-find #"^--" %) args))
        opt (fn [flag default] (or (second (drop-while #(not= flag %) args)) default))
        opts {:trace (opt "--trace" default-trace)
              :run-id (opt "--run-id" default-run-id)
              :menu-path (opt "--menu" default-menu)
              :single-menu-path (opt "--single-menu" default-single-menu)}]
    (when-not out
      (binding [*out* *err*]
        (println "usage: f1_machine_grain_q.clj OUT.edn [--trace PATH] [--run-id UUID] [--menu PATH] [--single-menu PATH]"))
      (System/exit 2))
    (let [record (build-record opts)]
      (io/make-parents out)
      (with-open [w (io/writer out)]
        (binding [*out* w] (pprint/pprint record)))
      (printf "f1_machine_grain_q: policies %d | machine-grain rows differ %s | machine max|diff| %s | declared-control max|diff| %s -> %s%n"
              (count (:policies record))
              (get-in record [:finding :machine-grain-rows-differ])
              (get-in record [:finding :max-abs-row-difference-machine])
              (get-in record [:finding :max-abs-row-difference-declared-control])
              out)
      (flush))))

(apply -main *command-line-args*)
