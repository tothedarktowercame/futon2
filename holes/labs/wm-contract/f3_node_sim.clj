#!/usr/bin/env clojure
;; :F3 -- the per-node simulation harness, piloted on R5.
;;
;;   clojure -M holes/labs/wm-contract/f3_node_sim.clj [outdir]
;;
;; Default outdir: holes/labs/wm-contract/runs/F3-node-sim.
;;
;; NO LIVE TICK, NO RUN LOCK, NOTHING UNDER data/. The node is run against
;; declared carriers (sim/R5-carriers.edn) and recorded inputs; the only files
;; read are that carriers file, aif-equations.edn, and F1's own receipt. No
;; wall-clock field is written, so two runs over an unchanged tree produce a
;; byte-identical receipt.
;;
;; WHAT THE RUN ASSERTS, and the order matters:
;;
;;   1. The kernel R5 is fed reproduces the numbers F1 pinned -- the Lean
;;      witness's 25/64 and 11/64 (MachineQWitness.lean:234-241) AND every row
;;      of runs/F1-machine-q/01-runtime-seam.edn. R5 is not allowed to read a
;;      kernel that has not been checked against both.
;;   2. The node agrees with the reference route and satisfies its laws
;;      (futon2.aif.node-sim/simulate-node).
;;   3. FIVE PLANTED WRONG-FORMULA NODES FAIL. Each is a formula someone could
;;      plausibly write for R5; each must be caught, and the receipt records
;;      WHICH check caught it. A planted node that passed would mean the
;;      harness is not a gate, so the script exits non-zero if any of them
;;      passes -- the negative control fails loudly, not silently.
;;
;; WHAT IT DOES NOT ASSERT. This measures a TRANSCRIPTION against declared
;; carriers. It is not a measurement of a shipped call path: the three R5 rows
;; of aif-equations.edn (:116-142) carry no :code pointer, the live scorer runs
;; at the per-channel Gaussian grain (src/futon2/aif/core_efe.clj:56-92) and
;; cannot consume a distribution over the six declared outcomes. Nothing here
;; is written to :choices or :decisions; nothing here is a ruling.
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[futon2.aif.node-sim :as ns-sim])

(def lab (io/file (System/getProperty "user.dir") "holes/labs/wm-contract"))
(def outdir (io/file (or (first *command-line-args*) (str (io/file lab "runs/F3-node-sim")))))

(defn- read-edn [f] (edn/read-string {:default (fn [_ v] v)} (slurp f)))

(def carriers-decl (read-edn (io/file lab "sim/R5-carriers.edn")))
(def equations (read-edn (io/file lab "aif-equations.edn")))
(def f1-receipt (read-edn (io/file lab "runs/F1-machine-q/01-runtime-seam.edn")))

(def registry-rows
  "The node's own rows of the equations registry -- what the node is checked
   against, rather than a list restated here."
  (filterv #(= :R5 (:node %)) (:equations equations)))

;; ---------------------------------------------------------------------------
;; Cross-file agreement: the carriers' pinned rows ARE F1's recorded rows
;; ---------------------------------------------------------------------------

(def f1-agreement
  (let [f1-rows (:rows f1-receipt)
        pinned (get-in carriers-decl [:pinned :rows])
        deviations (for [[pi row] pinned
                         [o m] row]
                     (Math/abs (- (double m) (double (get-in f1-rows [pi o] Double/NaN)))))]
    {:source "runs/F1-machine-q/01-runtime-seam.edn"
     :policies (vec (sort (keys pinned)))
     :max-deviation (reduce max 0.0 deviations)
     :agrees (every? #(< % ns-sim/tolerance) deviations)}))

;; ---------------------------------------------------------------------------
;; The pilot
;; ---------------------------------------------------------------------------

(def carriers (ns-sim/carriers! carriers-decl))

(def pilot (ns-sim/simulate-node {:carriers carriers :registry-rows registry-rows}))

;; ---------------------------------------------------------------------------
;; Negative controls: five planted wrong-formula nodes
;; ---------------------------------------------------------------------------

(defn- planted
  "Run the harness with `f` substituted for the R5 node."
  [id statement f]
  (let [r (ns-sim/simulate-node {:carriers carriers :registry-rows registry-rows :node-fn f})]
    {:id id :statement statement :verdict (:verdict r) :caught-by (:failed r)}))

(def controls
  [(planted
    :reversed-kl
    "risk := KL[C || Q(o|pi)] -- the KL taken the other way round. Both are non-negative and both vanish when C = Q, so neither sign nor the Gibbs equality case catches it."
    (fn [{:keys [policies outcome-rows state-rows observation preference]}]
      (let [risk (into {} (map (fn [pi] [pi (ns-sim/kl-divergence preference (get outcome-rows pi))])) policies)
            amb (into {} (map (fn [pi] [pi (ns-sim/expected-observation-entropy (get state-rows pi) observation)])) policies)]
        {:risk risk :ambiguity amb
         :G (into {} (map (fn [pi] [pi (+ (get risk pi) (get amb pi))])) policies)})))

   (planted
    :cross-entropy-for-risk
    "risk := -SUM_o Q(o|pi) ln C(o) -- cross-entropy instead of KL, i.e. the -SUM Q ln Q term dropped. It orders policies the same way whenever the predictive entropies coincide, which is why dropping it is an easy mistake to make and a hard one to see."
    (fn [{:keys [policies outcome-rows state-rows observation preference]}]
      (let [risk (into {} (map (fn [pi]
                                 [pi (- (reduce (fn [acc [o m]]
                                                  (+ acc (* (double m) (Math/log (double (get preference o))))))
                                                0.0
                                                (sort-by key (get outcome-rows pi))))]))
                       policies)
            amb (into {} (map (fn [pi] [pi (ns-sim/expected-observation-entropy (get state-rows pi) observation)])) policies)]
        {:risk risk :ambiguity amb
         :G (into {} (map (fn [pi] [pi (+ (get risk pi) (get amb pi))])) policies)})))

   (planted
    :ambiguity-as-predictive-entropy
    "ambiguity := H(Q(o|pi)) -- the entropy of the PREDICTIVE row instead of the expected entropy of the observation rows. The standard confusion: the two differ by the mutual information, so the planted term is the true one plus a non-negative quantity and stays inside the entropy range."
    (fn [{:keys [policies outcome-rows preference]}]
      (let [risk (into {} (map (fn [pi] [pi (ns-sim/kl-divergence (get outcome-rows pi) preference)])) policies)
            amb (into {} (map (fn [pi] [pi (ns-sim/row-entropy (get outcome-rows pi))])) policies)]
        {:risk risk :ambiguity amb
         :G (into {} (map (fn [pi] [pi (+ (get risk pi) (get amb pi))])) policies)})))

   (planted
    :ambiguity-unweighted
    "ambiguity := SUM_s H(P(o|s)) -- the observation rows' entropies summed rather than weighted by Q(s|pi). Q(s|pi) sums to one, so on a two-state carrier this is exactly twice the truth whenever the rows have equal entropy, and the planted value is still policy-independent."
    (fn [{:keys [policies outcome-rows state-rows observation preference]}]
      (let [risk (into {} (map (fn [pi] [pi (ns-sim/kl-divergence (get outcome-rows pi) preference)])) policies)
            amb (into {} (map (fn [pi]
                                [pi (reduce (fn [acc [s _]] (+ acc (ns-sim/row-entropy (get observation s))))
                                            0.0
                                            (sort-by key (get state-rows pi)))]))
                      policies)]
        {:risk risk :ambiguity amb
         :G (into {} (map (fn [pi] [pi (+ (get risk pi) (get amb pi))])) policies)})))

   (planted
    :g-as-risk-minus-ambiguity
    "G := risk - ambiguity, with both terms correct. This is the sign the OTHER decomposition carries -- aif-equations.edn's :expected-free-energy row records the conflict as `Holes.G := risk - eig` -- so a node written from the wrong one of the two readings gets both terms right and the sum wrong."
    (fn [{:keys [policies outcome-rows state-rows observation preference]}]
      (let [risk (into {} (map (fn [pi] [pi (ns-sim/kl-divergence (get outcome-rows pi) preference)])) policies)
            amb (into {} (map (fn [pi] [pi (ns-sim/expected-observation-entropy (get state-rows pi) observation)])) policies)]
        {:risk risk :ambiguity amb
         :G (into {} (map (fn [pi] [pi (- (get risk pi) (get amb pi))])) policies)})))])

;; ---------------------------------------------------------------------------
;; The receipt
;; ---------------------------------------------------------------------------

(def all-controls-caught (every? #(= :fail (:verdict %)) controls))

(def receipt
  {:schema :wm/node-sim-receipt-v1
   :row :F3
   :node :R5
   :slice "the harness, and its first node. Later nodes proceed in aif-equations dependency order under separate slices."
   :what-this-is
   (str "One control-stages node (control-stages.edn:22, EVALUATE band) run alone against declared "
        "carriers and recorded inputs. Every number below is read off a call. NOT a measurement of a "
        "shipped call path: aif-equations.edn's three R5 rows carry no :code pointer and the live "
        "scorer runs at the per-channel Gaussian grain (src/futon2/aif/core_efe.clj:56-92), which "
        "cannot consume a distribution over the six declared outcomes.")
   :code {:harness "src/futon2/aif/node_sim.clj"
          :node "src/futon2/aif/node_sim.clj:241 (r5) -- kl-divergence :206, row-entropy :222, expected-observation-entropy :232"
          :reference "src/futon2/aif/node_sim.clj:278 (reference-r5) -- exact rationals, prime-factorised logarithms"
          :kernel "src/futon2/aif/machine_q.clj:336 (Q(s|pi)), :356 (Q(o|pi)) -- the F1 composition, unchanged"
          :carriers "holes/labs/wm-contract/sim/R5-carriers.edn"
          :runner "holes/labs/wm-contract/f3_node_sim.clj"
          :test "test/futon2/aif/node_sim_test.clj"}
   :equations-registry
   {:rows (mapv (fn [r] {:id (:id r) :formal (:formal r) :eq (:eq r)
                         :imports (:imports r) :lean (:lean r) :lean-status (:lean-status r)
                         :code (:code r)})
                registry-rows)
    :source "holes/labs/wm-contract/aif-equations.edn:116-142"}
   :carriers-are-declared-not-inhabited
   (str "The state space, the policy family, the belief reading and C are four separately uninhabited "
        "fundamentals (FUNDAMENTALS.edn). Declaring them in sim/R5-carriers.edn closes none of them. "
        "Nothing here is written to :choices or :decisions and nothing here is a ruling.")
   :f1-agreement f1-agreement
   :pilot pilot
   :negative-controls
   {:planted-nodes controls
    :all-caught all-controls-caught
    :reads (str "Each is a formula someone could write for R5. The reference route and every law are "
                "computed from the carriers, never from the node, so a planted formula has nothing to "
                "hide behind. :caught-by names the checks that fired.")}
   :not-done
   ["Only R5. The harness runs one node; the other control-stages nodes are later slices of their own, in aif-equations dependency order."
    "No live tick, no run lock, nothing written under data/."
    "gen_aif_dag.bb not run; nothing regenerated into a publish (TN 9a)."
    "aif-equations.edn is READ and not written. The :ambiguity row's declared :imports disagreement (below) is recorded as a finding, not repaired: the registry is Joe's."
    "No claim that the machine computes these values. The pilot's numbers come from declared carriers; the live R5 runs at another grain."]
   :findings
   [{:id :ambiguity-import-names-the-wrong-symbol
     :statement (str "aif-equations.edn:119-120 declares :ambiguity :imports [:Q-o-pi :A], but its own formal "
                     "line is ambiguity(pi) := E_{Q(s|pi)}[H(P(o|s))] -- which reads Q(s|pi), the predicted "
                     "STATE distribution, and never touches Q(o|pi). Q(s|pi) is an intermediate of the R4 "
                     "composition (src/futon2/aif/machine_q.clj:336, MachineQ.lean:139-146) that the registry "
                     "gives no symbol, so there is nothing else the row could have named. Recorded, not "
                     "repaired: a registry edit is Joe's.")
     :basis "holes/labs/wm-contract/aif-equations.edn:119-120; src/futon2/aif/node_sim.clj:322 (:consumes); the :registry-imports check of the pilot above"}
    {:id :ambiguity-does-not-separate-the-two-policies
     :statement (str "On these carriers ambiguity is IDENTICAL for both policies, so the whole "
                     "policy-conditioned difference in the two-term core is risk. The reason is in the "
                     "carriers and is exact, not numerical: the two observation rows of aRow "
                     "(MachineQWitness.lean:115-127) are permutations of one another -- the same multiset "
                     "{1/2,1/8,1/8,1/8,1/16,1/16} -- so they have equal entropy, and any weighting of two "
                     "equal numbers by a distribution returns that number. A carrier with rows of differing "
                     "entropy would separate them; this one cannot, whatever the plan is.")
     :basis "the :ambiguity values and their exact prime coefficients in :pilot above; sim/R5-carriers.edn :observation"}
    {:id :r5-has-no-code-pointer
     :statement (str "None of the three R5 rows carries a :code field, unlike :forward-model (efe.clj:601-609; "
                     "forward_model.clj:279-324) and :policy-posterior (policy.clj:196-201). The alphabet-grain "
                     "R5 was not wired in src/ before this row: futon2.aif.node-sim is its first "
                     "implementation, and it is a harness, not a call path.")
     :basis "holes/labs/wm-contract/aif-equations.edn:116-142; src/futon2/aif/core_efe.clj:56-92; src/futon2/aif/efe.clj:842-872"}]})

(.mkdirs outdir)
(def receipt-file (io/file outdir "00-r5-pilot.edn"))
(with-open [w (io/writer receipt-file)]
  (binding [*out* w] (pp/pprint receipt)))

(println "F3 node-sim -- node R5, carriers" (:id carriers-decl))
(println "  F1 agreement:" (:agrees f1-agreement) "max deviation" (:max-deviation f1-agreement))
(doseq [c (:checks pilot)]
  (println (format "  %-32s %s" (name (:check c)) (name (:status c)))))
(println "  risk      " (pr-str (:risk (:values pilot))))
(println "  ambiguity " (pr-str (:ambiguity (:values pilot))))
(println "  G         " (pr-str (:G (:values pilot))))
(doseq [c controls]
  (println (format "  planted %-32s %s caught-by %s" (name (:id c)) (name (:verdict c)) (pr-str (:caught-by c)))))
(println "  verdict:" (name (:verdict pilot)) "| all planted nodes caught:" all-controls-caught)
(println "  receipt:" (str receipt-file))

(System/exit (if (and (= :pass (:verdict pilot)) (:agrees f1-agreement) all-controls-caught) 0 1))
