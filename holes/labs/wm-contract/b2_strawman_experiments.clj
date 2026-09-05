#!/usr/bin/env clojure
;; :B2 -- the CHEAPEST refuting experiment for each strawman hole that needs a
;; JVM. Two holes are answered here; the other five are answered by
;; b2_strawman_reads.bb (a read/replay, no JVM).
;;
;;   clojure -M holes/labs/wm-contract/b2_strawman_experiments.clj [outdir]
;;
;; E4 -- "Specify typed policy-to-Q and Q-to-consumer delivery", whose acceptance
;; evidence is "the same constructed Q is accepted by BOTH risk and EIG
;; fixtures, and action-grain Gaussian telemetry or Q(pi) is rejected at the
;; boundary". :F1 slice 3 landed the rejection half (machine_q.clj:138). The
;; refutation this runs is the acceptance half: construct the machine Q that
;; :F1 pinned, hand ONE ROW to each of the two consumers, and record what each
;; one does with it. If both accept, the hole is refuted.
;;
;; E3 -- "Unify simulated and realised posterior updates ... through one
;; A4a/BMR updater". The refutation is: name the updater and call it twice.
;; This probes futon2.aif.a4a and futon2.aif.bmr for a function that takes a
;; posterior state and one observation and returns the updated state -- the
;; object both paths would share. If one exists the hole is refuted.
;;
;; NOTHING IS WRITTEN UNDER data/. No tick, no run lock, no network.

(require '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[futon2.aif.machine-q :as mq]
         '[futon2.aif.epistemic-value :as ev]
         '[futon2.aif.core-efe :as core-efe]
         ;; Required for their side effect -- loading the namespaces so
         ;; `ns-publics` below can enumerate them. clj-kondo reads the quoted
         ;; symbol in `arity-names` as data and reports these as unused; the
         ;; probe would return two empty maps without them.
         '[futon2.aif.a4a]
         '[futon2.aif.bmr])

;; --------------------------------------------------------------------------
;; The carriers. Copied from test/futon2/aif/machine_q_test.clj:17-70, which is
;; MachineQWitness.lean:56-75/:113-126/:140-147/:200-215 transcribed. Copied
;; rather than required because the test namespace is not on the default
;; classpath; the row masses below are checked against the F1 run record.
;; --------------------------------------------------------------------------

(def alphabet [:ordinary :no-result :failure :timeout :conflict :missing])
(def states [:informative :opaque])

(def model
  {:states states
   :outcomes alphabet
   :transition (into {}
                     (for [s states
                           [u row] {:acquire {:informative 3/4 :opaque 1/4}
                                    :review {:informative 1/4 :opaque 3/4}}]
                       [[s u] row]))
   :observation {:informative {:ordinary 1/2 :no-result 1/8 :failure 1/8
                               :timeout 1/8 :conflict 1/16 :missing 1/16}
                 :opaque {:ordinary 1/16 :no-result 1/2 :failure 1/8
                          :timeout 1/8 :conflict 1/8 :missing 1/16}}})

(defn- logistic [x] (/ 1.0 (+ 1.0 (Math/exp (- (double x))))))

(def reading
  {:id :demo/evidence-acquisition
   :plan {:acquisition :acquire :review :review}
   :belief-mass (fn [belief s]
                  (let [w (logistic (get belief :support-coverage 0.0))]
                    (case s :informative w :opaque (- 1.0 w))))})

(def belief {:support-coverage 0.0})

(defn- outcome-of
  "Call F, returning what it did rather than throwing: either the value or the
   typed reason it refused with."
  [f]
  (try {:accepted? true :value (f)}
       (catch clojure.lang.ExceptionInfo e
         {:accepted? false
          :refusal (or (:refusal/reason (ex-data e)) (:type (ex-data e)))
          :reason (or (ex-message e) "")
          :ex-data (dissoc (ex-data e) :row)})
       (catch Throwable e
         {:accepted? false
          :refusal :threw
          :class (.getName (class e))
          :reason (or (ex-message e) "")})))

;; --------------------------------------------------------------------------
;; E4
;; --------------------------------------------------------------------------

(def kernel (mq/predictive-outcome-kernel model reading belief))
(def q-row (get-in kernel [:rows :acquisition]))

(def eig-attempt
  "The EIG consumer. Q(o|pi) occupies :predicted-observations; the kernel also
   demands :prior over theta and :posteriors {o -> Q(theta|o,pi)}. The machine
   Q supplies exactly one of the three."
  (outcome-of #(ev/expected-information-gain {:predicted-observations q-row})))

(def eig-observations-only
  "Does the row at least PASS the EIG kernel's own distribution gate? Called
   through the private validator's public consequence: a model whose prior and
   posteriors are the degenerate ones that reconstruct themselves, so any
   failure is about the row and not about the rest."
  (let [prior {:theta 1.0}
        posteriors (into {} (map (fn [o] [o {:theta 1.0}])) (keys q-row))]
    (outcome-of #(ev/expected-information-gain
                  {:prior prior
                   :predicted-observations q-row
                   :posteriors posteriors}))))

(def risk-attempt-map
  "The risk consumer, handed the row as an object."
  (outcome-of #(core-efe/risk q-row q-row q-row q-row)))

(def risk-attempt-vals
  "The risk consumer, handed the row the only way its signature admits: as
   parallel seqs of per-channel scalars. This is the coupling the hole exists
   to prevent, so what matters is whether it is REFUSED or silently computed."
  (outcome-of #(core-efe/risk (vals q-row) (vals q-row) (vals q-row) (vals q-row))))

(def risk-attempt-shuffled
  "The same call with the outcome order permuted. core-efe/risk takes seqs, so
   if it computes at all it computes over positions; a value that is invariant
   under permutation of a MAP's vals would mean the outcome identity survived."
  (outcome-of #(core-efe/risk (reverse (vals q-row)) (vals q-row)
                              (vals q-row) (vals q-row))))

;; --------------------------------------------------------------------------
;; E3 -- name the shared updater
;; --------------------------------------------------------------------------

(defn- arity-names [ns-sym]
  (into (sorted-map)
        (map (fn [[sym v]]
               [(str sym) (mapv #(mapv str %) (:arglists (meta v)))]))
        (ns-publics ns-sym)))

(def a4a-publics (arity-names 'futon2.aif.a4a))
(def bmr-publics (arity-names 'futon2.aif.bmr))

(def updater-probe
  "An updater is a function of (posterior-state, observation) -> posterior-state.
   Probed by name over both namespaces' public vars: nothing in either is named
   for an update, and the census below is the whole public surface, so the
   absence is established by enumeration rather than by a grep that could have
   been anchored wrong."
  {:a4a-public-count (count a4a-publics)
   :bmr-public-count (count bmr-publics)
   :a4a-publics (vec (keys a4a-publics))
   :bmr-publics (vec (keys bmr-publics))
   :named-for-update
   (vec (sort (filter #(re-find #"(?i)update|observe|posterior|fold" %)
                      (concat (keys a4a-publics) (keys bmr-publics)))))})

;; --------------------------------------------------------------------------

(def record
  {:schema :wm/b2-strawman-experiments-v1
   :row :B2
   :at "2026-09-05"
   :what-this-is
   "Measured output of two refutation attempts, produced by running them. Every
    verdict below is read off a call."
   :E4
   {:hole "Specify typed policy-to-Q and Q-to-consumer delivery (STRAWMAN-M-aif-policy-conditioned-eig.md hole 4)"
    :refutation-attempted
    "Hand ONE row of the machine Q that :F1 pinned to each of the two consumers the acceptance names. Refuted if both accept it."
    :q-source "futon2.aif.machine-q/predictive-outcome-kernel over the MachineQWitness carriers"
    :q-row-policy :acquisition
    :q-row q-row
    :normalisation-residual (:normalisation-residual kernel)
    :agrees-with-F1-record
    {:claim "the row reproduces runs/F1-machine-q/01-runtime-seam.edn :rows :acquisition"
     :ordinary (get q-row :ordinary)
     :f1-ordinary 0.390625
     :max-abs-difference (Math/abs (- (double (get q-row :ordinary)) 0.390625))}
    :eig-consumer
    {:with-only-the-row eig-attempt
     :with-degenerate-prior-and-posteriors eig-observations-only}
    :risk-consumer
    {:as-a-map risk-attempt-map
     :as-parallel-seqs risk-attempt-vals
     :as-parallel-seqs-permuted risk-attempt-shuffled}}
   :E3
   {:hole "Unify simulated and realised posterior updates through one A4a/BMR updater (STRAWMAN-M-aif-policy-conditioned-eig.md hole 3)"
    :refutation-attempted "Name the updater both paths would share, by enumerating the public surface of both namespaces."
    :probe updater-probe}})

(defn -main [& [outdir]]
  (let [dir (io/file (or outdir "holes/labs/wm-contract/runs/B2-strawman"))
        f (io/file dir "01-consumer-and-updater-probe.edn")]
    (.mkdirs dir)
    (spit f (with-out-str (pp/pprint record)))
    (println "wrote" (str f))
    (pp/pprint (-> record
                   (update :E4 dissoc :q-row)
                   (update-in [:E3 :probe] dissoc :a4a-publics :bmr-publics)))))

(-main (first *command-line-args*))
