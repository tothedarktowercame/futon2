;; G' (want free energy): Clojure readback, F8 certificate form.
;;
;; Every `-expected` below is TRANSCRIBED FROM A LEAN STATEMENT, not derived a
;; second time from the inputs -- otherwise a delta of 0.0 would only say that
;; Clojure agrees with Clojure. The transcriptions, by name, in
;; mathlib4/DarkTower/WarMachine/WantFreeEnergyWitness.lean (worktree
;; c14-cascade-outcome, branch c14/cascade-outcome-type), with the carriers in
;; WantFreeEnergy.lean and WantFreeEnergyBenefit.lean:
;;   nine-in-ten-expected      chance_nine_in_ten            = 41/50
;;   certain-expected          chance_certain                = 9/10
;;   mix-half-expected         chance_mix_half               = 11/20
;;   uniform-expected          chance_uniform                = 12/25
;;   uniform-climb-expected    chance_uniform_climb          = 57/100
;;   b17-realised-expected     b17_noTrace_realised          = 3/34
;;                             (re-exported from R1DeepTemporalB17.noTrace_realised;
;;                             the channel is R1DeepTemporalB17.traceProb, and the
;;                             update runs in futon2.aif.r1-belief, b389f2fe2's port)
;;   per-step-expected         perStep (the horizon total divided by H)
;; G''s want term is -log chance; log is transcendental, so, as in C521, Lean
;; states the chance exactly (a rational) and keeps log symbolic; this readback
;; evaluates only the log (Math/log) and compares want terms at 1e-12.
;;
;; WHICH CALLS ARE PRODUCTION: since E-kimi-task-438 the production
;; comparison (futon2.aif.cascade-coapply-g/compare-at-horizon, both lanes)
;; scores by G' -- the wiring section below runs it. The first section still
;; measures the reference-port surface itself: chance, want-term, push,
;; step-g', total-g', per-step, score-cascade-co', score-cascade-co-n'. The
;; agreement case measures G' against the EXISTING production risk function
;; (futon2.aif.cascade-g/step-g, the CascadeEFE.risk behind the canonical
;; cascade G) on one real-shaped candidate: the five R1 rungs, a point
;; prediction on :realised, a deterministic observation, and the uniform want
;; 1/5 per rung read as the preference distribution (it sums to 1, which
;; stepG'_eq_stepG_of_point requires of C).
(require '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[futon2.aif.cascade-coapply-g :as coapply]
         '[futon2.aif.cascade-g :as canonical-g]
         '[futon2.aif.cascade-policy :as policy]
         '[futon2.aif.cascade-shape-g :as shape]
         '[futon2.aif.r1-belief :as r1]
         '[futon2.aif.want-free-energy :as wfe])

(def tolerance 1.0e-12)
(defn delta [actual expected] (- (double actual) (double expected)))
(defn close? [a b] (< (Math/abs (delta a b)) tolerance))

(def rungs r1/rungs)
(def rung-want wfe/rung-want)
(def met-want wfe/met-want)
(def uniform-r (wfe/uniform-rung-prediction))

;; Transcribed rationals (see header).
(def nine-in-ten-expected 41/50)
(def certain-expected 9/10)
(def mix-half-expected 11/20)
(def uniform-expected 12/25)
(def uniform-climb-expected 57/100)
(def b17-realised-expected 3/34)

(def nine-in-ten (wfe/chance (wfe/bern 9/10) met-want))
(def certain (wfe/chance (wfe/point [true false] true) met-want))
(def mix-half (wfe/chance (wfe/mix-kernel 1/2 (wfe/point rungs :realised)
                                            (wfe/point rungs :none))
                          rung-want))
(def uniform (wfe/chance uniform-r rung-want))
(def uniform-climb (wfe/chance (wfe/push uniform-r (r1/climb 1/2) rungs) rung-want))

;; b17: the no-trace update runs in r1-belief; traceProb transcribed from
;; R1DeepTemporalB17.traceProb.
(def trace-prob {:none 1/10 :available 1/10 :used 3/10 :witnessed 2/5 :realised 7/10})
(def trace-channel (fn [rung kind] (case kind :trace (get trace-prob rung)
                                              :no-trace (- 1 (get trace-prob rung)))))
(def b17-realised (get (r1/update-belief (r1/uniform-belief) trace-channel [:no-trace])
                       :realised))

;; Want terms: log evaluated here, chances transcribed exactly.
(def wt-nine-in-ten (wfe/want-term (wfe/bern 9/10) met-want))
(def wt-nine-in-ten-expected (- (Math/log (double nine-in-ten-expected))))
(def wt-certain (wfe/want-term (wfe/point [true false] true) met-want))
(def wt-certain-expected (- (Math/log (double certain-expected))))
(def wt-uniform-climb (wfe/want-term (wfe/push uniform-r (r1/climb 1/2) rungs) rung-want))
(def wt-uniform-climb-expected (- (Math/log (double uniform-climb-expected))))

;; The agreement case (stepG'_eq_stepG_of_point), measured on the real-shaped
;; rung candidate against the existing production risk (cascade-g/step-g).
(def agreement-want
  "Uniform want over the five rungs; sums to 1, as the agreement theorem
  requires of the preference distribution C."
  (into {} (map (fn [k] [k 1/5]) rungs)))
(def agreement-model {:id "gprime-readback-agreement" :revision "1"})
(def agreement-q {:model agreement-model :state-support [:s] :mass {:s 1}
                  :authority :observed-estimate})
(def agreement-a {:model agreement-model :state-support [:s] :outcome-support rungs
                  :rows {:s {:support rungs :mass (wfe/point rungs :realised)}}
                  :authority :observed-estimate})
(def agreement-c {:model agreement-model :support rungs :mass agreement-want
                  :provenance {:source :uniform-want-as-distribution}})
(def agreement-canonical
  (canonical-g/step-g {:q agreement-q :a agreement-a :c agreement-c
                       :context {:policy/id :readback :occurrence/id 0 :point 0}}))
(def agreement-g' (wfe/step-g' {:s 1} {:s (wfe/point rungs :realised)} agreement-want))
(def agreement-mid (/ (+ (double (first (:g agreement-canonical)))
                         (double (second (:g agreement-canonical)))) 2))

;; perStep: a two-step horizon total divided by H.
(def candidate
  {:id :readback-two-step
   :initial {:s 1}
   :transition {:s {:s 1}}
   :observation {:s {true 9/10 false 1/10}}
   :horizon 2
   :wants [met-want met-want]})
(def scored (wfe/score-cascade-co' candidate))
(def scored-n (wfe/score-cascade-co-n' candidate 1/4))
(def per-step-expected (/ (:g' scored) 2))

;;; ## Wiring section (E-kimi-task-438): G' inside compare-at-horizon
;;
;; The canonical co-application G now scores by the want term on BOTH lanes
;; (decision 4: the lane's C read as a want effect). The fixtures below run
;; the production entry point futon2.aif.cascade-coapply-g/compare-at-horizon
;; at horizon 1 over one-pattern candidates; the expected chances are the
;; transcribed WantFreeEnergyWitness rationals above.
;;
;;   wiring-nine-in-ten   bern(9/10) under C 9/10:1/10   -> chance 41/50
;;   wiring-certain       point prediction under the same C -> chance 9/10
;;   wiring-agreement     on the point prediction, the wired :G-efe equals
;;                        the old risk-based G for the same entry
;;                        (stepG'_eq_stepG_of_point), reported side by side.

(def wiring-token ["T" :c])
(defn- wiring-pattern [theta]
  (assoc (policy/token-interpretation :c {:guard {:needs #{} :forbids #{wiring-token}}
                                          :produces #{wiring-token}})
         :theta theta))
(defn- wiring-candidate [id theta]
  {:kind :cascade-candidate :id id :target "T"
   :precedence {:co-apply {:units [:c] :descent []
                           :patterns {:c (wiring-pattern theta)}}}})
(def wiring-c {:focused 9/10 :stop-the-line 1/10})
(def wiring-model
  (shape/class-emission-model "T" #{wiring-token} #{wiring-token} 1 :focused
    {:status :synthetic :source "G' readback wiring fixture"}
    wiring-c))
(defn- wiring-opts [id]
  {:horizon-steps 1 :pattern-counts {id {:c [1 1]}}
   :observation-model wiring-model
   :prediction-context {:occurrence-id "gprime-readback" :tau 1}
   :cascade-spec {:want #{wiring-token} :evidence #{} :zeroed #{}}})
(defn- wire [id theta]
  (let [c (wiring-candidate id theta)
        r (coapply/compare-at-horizon {:cascade-belief {#{} 1}} [c] (wiring-opts id))]
    (when-not (and (vector? r) (= :scored (:status (first r))))
      (throw (ex-info "wiring fixture refused" {:result r})))
    (first r)))

(def wired-bern (wire :wiring-bern 9/10))
(def wired-certain (wire :wiring-certain 1))
(def wiring-bern-chance (get-in wired-bern [:certificate :steps 0 :chance]))
(def wiring-certain-chance (get-in wired-certain [:certificate :steps 0 :chance]))
;; The old risk-based G for the same entry, recomputed from the REPORTED
;; fields (:risk per step, :g-terms :risk) and the novelty total: risk +
;; ambiguity - novelty. This is what compare-at-horizon ranked on before
;; E-kimi-task-438.
(defn- legacy-risk-g [entry]
  (let [steps (get-in entry [:certificate :steps])
        novelty (get-in entry [:certificate :parameter-novelty :total])]
    (- (+ (reduce + 0.0 (map :risk steps))
          (reduce + 0.0 (map :ambiguity steps)))
       (double novelty))))
(def wiring-certain-legacy-g (legacy-risk-g wired-certain))
(def wiring-bern-legacy-g (legacy-risk-g wired-bern))

(def lines
  ["G' want free energy -- Clojure readback"
   "production calls: the wiring section runs compare-at-horizon (:g-law :want-free-energy/v1 since E-kimi-task-438); the first section measures the reference-port surface chance, want-term, push, step-g', total-g', per-step, score-cascade-co', score-cascade-co-n'"
   "agreement measured against the EXISTING production risk: futon2.aif.cascade-g/step-g (CascadeEFE.risk)"
   "Lean keeps log symbolic; rational chances compare EXACTLY, want terms at tolerance 1e-12"
   "references transcribed from WantFreeEnergyWitness (see header)"
   (format "chance nine-in-ten actual %s expected %s exact=%s"
           nine-in-ten nine-in-ten-expected (== nine-in-ten nine-in-ten-expected))
   (format "chance certain actual %s expected %s exact=%s"
           certain certain-expected (== certain certain-expected))
   (format "chance mix-half actual %s expected %s exact=%s"
           mix-half mix-half-expected (== mix-half mix-half-expected))
   (format "chance uniform actual %s expected %s exact=%s"
           uniform uniform-expected (== uniform uniform-expected))
   (format "chance uniform-climb actual %s expected %s exact=%s"
           uniform-climb uniform-climb-expected (== uniform-climb uniform-climb-expected))
   (format "b17 no-trace realised actual %s expected %s exact=%s"
           b17-realised b17-realised-expected (== b17-realised b17-realised-expected))
   (format "want-term nine-in-ten actual %.17g expected %.17g delta %.17g"
           wt-nine-in-ten wt-nine-in-ten-expected (delta wt-nine-in-ten wt-nine-in-ten-expected))
   (format "want-term certain actual %.17g expected %.17g delta %.17g"
           wt-certain wt-certain-expected (delta wt-certain wt-certain-expected))
   (format "want-term uniform-climb actual %.17g expected %.17g delta %.17g"
           wt-uniform-climb wt-uniform-climb-expected (delta wt-uniform-climb wt-uniform-climb-expected))
   (format "agreement (point prediction, rung candidate): g' %.17g canonical G enclosure [%.17g %.17g] delta-from-midpoint %.17g"
           agreement-g' (double (first (:g agreement-canonical)))
           (double (second (:g agreement-canonical))) (delta agreement-g' agreement-mid))
   (format "score-cascade-co' two-step total %.17g per-step actual %.17g expected %.17g delta %.17g"
           (:g' scored) (:per-step scored) per-step-expected
           (delta (:per-step scored) per-step-expected))
   (format "score-cascade-co-n' novelty 1/4: g'-novelty actual %.17g expected %.17g delta %.17g"
           (:g'-novelty scored-n) (- (:g' scored) 0.25)
           (delta (:g'-novelty scored-n) (- (:g' scored) 0.25)))
   "wiring (E-kimi-task-438): the cases below run the PRODUCTION futon2.aif.cascade-coapply-g/compare-at-horizon, both lanes' law (:g-law :want-free-energy/v1)"
   (format "wiring chance nine-in-ten (bern 9/10 under C 9/10:1/10) actual %s expected %s exact=%s"
           wiring-bern-chance nine-in-ten-expected (== wiring-bern-chance nine-in-ten-expected))
   (format "wiring chance certain (point prediction) actual %s expected %s exact=%s"
           wiring-certain-chance certain-expected (== wiring-certain-chance certain-expected))
   (format "wiring agreement (point prediction, stepG'_eq_stepG_of_point): :G-efe %.17g old risk-based G %.17g delta %.17g"
           (:G-efe wired-certain) wiring-certain-legacy-g
           (delta (:G-efe wired-certain) wiring-certain-legacy-g))
   (format "wiring non-point contrast (bern): :G-efe %.17g old risk-based G %.17g delta %.17g (differs: not a point mass)"
           (:G-efe wired-bern) wiring-bern-legacy-g
           (delta (:G-efe wired-bern) wiring-bern-legacy-g))
   (format "wiring g-law %s g-per-step %.17g"
           (:g-law wired-certain) (:g-per-step wired-certain))
   (if (and (== nine-in-ten nine-in-ten-expected)
            (== certain certain-expected)
            (== mix-half mix-half-expected)
            (== uniform uniform-expected)
            (== uniform-climb uniform-climb-expected)
            (== b17-realised b17-realised-expected)
            (close? wt-nine-in-ten wt-nine-in-ten-expected)
            (close? wt-certain wt-certain-expected)
            (close? wt-uniform-climb wt-uniform-climb-expected)
            (close? agreement-g' agreement-mid)
            (close? (:per-step scored) per-step-expected)
            (close? (:g'-novelty scored-n) (- (:g' scored) 0.25))
            (== wiring-bern-chance nine-in-ten-expected)
            (== wiring-certain-chance certain-expected)
            (close? (:G-efe wired-certain) wiring-certain-legacy-g)
            (not (close? (:G-efe wired-bern) wiring-bern-legacy-g)))
     "VERDICT: every measured case matches its transcribed Lean reference."
     "VERDICT: MISMATCH.")])

(let [out (io/file "holes/labs/wm-contract/runs/G-prime-want-free-energy/clojure-readback.txt")]
  (io/make-parents out)
  (spit out (str (str/join "\n" lines) "\n"))
  (println (slurp out)))
