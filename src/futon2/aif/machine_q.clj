(ns futon2.aif.machine-q
  "The runtime half of Q(o|pi): the R4 seam's predictive outcome kernel,
   composed rather than proxied.

   THE OBJECT. `Q(o|pi)` is a distribution over the DECLARED OUTCOME ALPHABET,
   conditioned on a policy. It is not `Q(pi)` (the selection distribution
   `policy/select-action` returns as `:softmax-weights`), it is not the
   per-channel Gaussian `forward-model/predict` emits, and it is not the
   two-point map `mission-epistemic-value/binary-latent-model` hand-builds for
   one binary latent. All three are refused at `predictive-outcome-row!` with a
   typed reason, which is the point of this namespace as much as the
   composition is: `Q-interface-completeness.edn` `:Q/to-EIG` records that the
   consumer end is currently occupied by a fixture Q, and a composition that
   silently accepted one would leave that finding true and unreadable.

   THE COMPOSITION is the one `DarkTower.WarMachine.MachineQ` proves
   (`mathlib4/DarkTower/WarMachine/MachineQ.lean:187-198`), transcribed:

     Q(s'|pi) = SUM_s  beliefMass(b, s) * B(s, plan(pi))(s')     [:139-146]
     Q(o|pi)  = SUM_s' Q(s'|pi)         * A(s')(o)               [:187-198]

   WHAT IS PROVED AND WHAT IS CHECKED, kept apart. Lean proves every row sums
   to one for every model, reading and belief (`MachineQ.lean:168-181`). This
   namespace CHECKS the sum on each constructed row and reports the residual as
   `:normalisation-residual`. A check is a receipt about one call; the proof is
   the general statement, and it lives in Lean. Nothing here re-proves it and
   nothing here should be read as doing so.

   THE READING IS AN OBLIGATION, NOT A DEFAULT. `QReading`'s runtime form
   (`q-reading!` below) demands the same four things the Lean structure demands
   -- the state carrier, the outcome alphabet, the policy's planned step, and
   the belief reading -- because the machine has none of them: its policy
   carrier, state space, belief reading and preference distribution are four
   separately uninhabited fundamentals (`holes/labs/wm-contract/FUNDAMENTALS.edn`
   `:fundamental/machine-policy-carrier`,
   `:fundamental/controlled-transition-kernel`,
   `:fundamental/belief-to-state-distribution`,
   `:fundamental/machine-preference-distribution`). Choosing any of them is a
   modelling ruling and is Joe's. So this namespace ships with NO reading, and
   `*seam-reading*` is nil: what a caller had to supply is readable from the
   call, exactly as in Lean.

   THE LAW. `rows-equal-for-equal-plans?` is `MachineQ.rowsEqualOfEqualPlans`
   (`MachineQ.lean:210-218`) as an executable check: pi reaches Q through
   `plan pi` and nowhere else, so an exhibited policy-conditioned difference is
   evidence about the planned step and not about the machinery. The negative
   control that flattens `plan` and watches the rows coincide is in
   `machine_q_test`.

   DEFAULT OFF, and off is byte-identical. `enabled?` is false unless
   FUTON_WM_MACHINE_Q=1, and with it clear `seam-attachment` returns nil
   before reading anything, so `forward-model/predict` returns the map it
   returned before this namespace existed -- no key, no arithmetic, no
   substrate read. Flipping the default is a J-gated ruling, not this
   namespace's."
  (:require [clojure.string :as str]))

(def tolerance
  "Row-sum tolerance for the normalisation receipt. The same 1e-9 that
   `epistemic-value/probability-distribution!` uses, so a distribution this
   namespace accepts is one that kernel accepts."
  1.0e-9)

;; ---------------------------------------------------------------------------
;; The flag
;; ---------------------------------------------------------------------------

(def ^:dynamic *enabled*
  "Test-only override of the environment flag. nil means \"read the
   environment\"; true/false force it. Dynamic binding exists only so a test
   can exercise the on-path without setting a process environment variable."
  nil)

(defn enabled?
  "Whether the R4 machine-Q seam is live. Default false."
  []
  (if (some? *enabled*)
    (boolean *enabled*)
    (= "1" (System/getenv "FUTON_WM_MACHINE_Q"))))

(def ^:dynamic *seam-reading*
  "The `{:model .. :reading ..}` a caller exhibits at the R4 seam, or nil.

   nil is the shipped value and the honest one: the War Machine has no policy
   carrier, state space or belief reading to build one from (see the ns
   docstring). A caller that has declared those for its own scope binds this;
   nothing in `src/` binds it."
  nil)

;; ---------------------------------------------------------------------------
;; Typed refusals
;; ---------------------------------------------------------------------------

(defn- refuse!
  [reason message data]
  (throw (ex-info message (assoc data :refusal reason))))

(defn- action-map?
  [x]
  (and (map? x) (keyword? (:type x))))

(defn- gaussian-proxy?
  "The R4 action-grain Gaussian: `forward-model/predict`'s whole return, or its
   `:next-observation` alone (`forward_model.clj:334-336`). Recognised by
   structure so this namespace does not depend on the forward model."
  [x]
  (and (map? x)
       (or (contains? x :next-observation)
           (contains? x :variance-status)
           (and (contains? x :mean) (contains? x :variance)))))

(defn- fixture-q?
  "The hand-built survey-path Q: `mission-epistemic-value/binary-latent-model`'s
   kernel input (`mission_epistemic_value.clj:186-205`), whose
   `:predicted-observations` is a two-point map set equal to the prior of one
   binary latent. `Q-interface-completeness.edn` `:Q/to-EIG` records it as the
   fixture now occupying the consumer end; `:F1` says reject it at the
   boundary rather than couple to it."
  [x]
  (and (map? x)
       (contains? x :predicted-observations)
       (or (contains? x :prior) (contains? x :posteriors))))

(defn- policy-distribution?
  "`Q(pi)`: `policy/select-action`'s `:softmax-weights`
   (`policy.clj:669-671`), or a bare distribution whose support elements are
   action maps. Downstream of G, which itself needs `Q(o|pi)` -- so offering it
   here would be circular, not merely wrong."
  [x]
  (and (map? x)
       (or (contains? x :softmax-weights)
           (and (seq x) (every? action-map? (keys x))))))

(defn- finite-nonneg?
  [p]
  (and (number? p) (Double/isFinite (double p)) (not (neg? (double p)))))

(defn predictive-outcome-row!
  "THE BOUNDARY. Accept `row` as one policy's Q(o|pi) row over `alphabet`, or
   refuse it with a typed reason.

   `alphabet` is the declared outcome alphabet as a collection; a row may not
   name an outcome outside it, and may not omit one silently -- an outcome the
   model gives no mass must appear at 0.0, so that an alphabet gap and a zero
   are distinguishable. Returns the row unchanged when accepted.

   Refusal reasons, each an `:refusal` key on the thrown ex-info:
     :refusal/action-grain-gaussian-proxy
     :refusal/fixture-q-hand-built
     :refusal/policy-selection-distribution
     :refusal/not-a-map
     :refusal/empty-support
     :refusal/non-probability-mass
     :refusal/outcome-outside-declared-alphabet
     :refusal/outcome-missing-from-row
     :refusal/unnormalised"
  [row alphabet]
  (let [declared (set alphabet)]
    (cond
      (gaussian-proxy? row)
      (refuse! :refusal/action-grain-gaussian-proxy
               "the R4 action-grain Gaussian prediction is not Q(o|pi)"
               {:got (if (contains? row :next-observation)
                       :forward-model-prediction
                       :mean-variance-pair)
                :why (str "per-channel means and variances over observation "
                          "channels, conditioned on an action, not a "
                          "distribution over the declared outcome alphabet")})

      (fixture-q? row)
      (refuse! :refusal/fixture-q-hand-built
               "a hand-built latent fixture is not a composed Q(o|pi)"
               {:got :binary-latent-model-shape
                :why (str "predicted observations set equal to a declared "
                          "prior; nothing composes a transition and an "
                          "observation kernel to produce them")})

      (policy-distribution? row)
      (refuse! :refusal/policy-selection-distribution
               "Q(pi) is not Q(o|pi)"
               {:got :selection-distribution
                :why (str "a distribution over policies, downstream of G, "
                          "which itself consumes Q(o|pi)")})

      (not (map? row))
      (refuse! :refusal/not-a-map "a Q(o|pi) row is a map from outcome to mass"
               {:got (type row)})

      (empty? row)
      (refuse! :refusal/empty-support "a Q(o|pi) row has no support" {})

      :else
      (let [bad (remove (comp finite-nonneg? val) row)
            unknown (remove declared (keys row))
            missing (remove (set (keys row)) declared)
            total (reduce + 0.0 (map (comp double val) row))]
        (cond
          (seq bad)
          (refuse! :refusal/non-probability-mass
                   "a Q(o|pi) row carries a non-finite or negative mass"
                   {:entries (vec bad)})

          (seq unknown)
          (refuse! :refusal/outcome-outside-declared-alphabet
                   "a Q(o|pi) row names an outcome outside the declared alphabet"
                   {:outcomes (vec unknown) :alphabet (vec alphabet)})

          (seq missing)
          (refuse! :refusal/outcome-missing-from-row
                   "a Q(o|pi) row omits a declared outcome; zero mass must be stated"
                   {:outcomes (vec missing)})

          (>= (Math/abs (- 1.0 total)) tolerance)
          (refuse! :refusal/unnormalised "a Q(o|pi) row does not sum to one"
                   {:sum total :residual (Math/abs (- 1.0 total))})

          :else row)))))

;; ---------------------------------------------------------------------------
;; The model and the reading -- QReading's runtime form
;; ---------------------------------------------------------------------------

(defn- row-sums-to-one?
  [row]
  (and (map? row)
       (seq row)
       (every? (comp finite-nonneg? val) row)
       (< (Math/abs (- 1.0 (reduce + 0.0 (map (comp double val) row)))) tolerance)))

(defn generative-model!
  "Validate the runtime form of `Holes.GenerativeModel`
   (`Holes.lean:6787-6790`) and return it.

     {:states      [s ..]              the finite state carrier
      :outcomes    [o ..]              the declared outcome alphabet
      :transition  {[s u] {s' mass}}   B : S x U ~> S
      :observation {s {o mass}}}       A : S ~> O

   `:actions` is derived from the transition keys. The support obligations
   `QReading` states as hypotheses (`MachineQ.lean:98-101`) are checked here
   instead: every transition row is over exactly `:states`, every observation
   row over exactly `:outcomes`. Lean can take them as hypotheses because a
   caller must prove them; the runtime has to look."
  [{:keys [states outcomes transition observation] :as model}]
  (when-not (and (seq states) (seq outcomes) (map? transition) (map? observation))
    (refuse! :refusal/malformed-model
             "a generative model needs states, outcomes, a transition and an observation kernel"
             {:missing (into [] (remove #(seq (get model %)))
                             [:states :outcomes :transition :observation])}))
  (let [state-set (set states)
        outcome-set (set outcomes)]
    (doseq [[k row] transition]
      (when-not (and (vector? k) (= 2 (count k)) (contains? state-set (first k)))
        (refuse! :refusal/transition-key-not-state-action
                 "a transition row is keyed by [state action] over the declared states"
                 {:key k}))
      (when-not (= (set (keys row)) state-set)
        (refuse! :refusal/transition-support-mismatch
                 "a transition row is not stated over the declared state carrier"
                 {:key k :row-support (set (keys row)) :states state-set}))
      (when-not (row-sums-to-one? row)
        (refuse! :refusal/transition-row-unnormalised
                 "a transition row is not a distribution over the declared states"
                 {:key k :sum (reduce + 0.0 (map (comp double val) row))})))
    (doseq [s states]
      (let [row (get observation s)]
        (when-not (map? row)
          (refuse! :refusal/observation-row-missing
                   "the observation kernel has no row for a declared state" {:state s}))
        (when-not (= (set (keys row)) outcome-set)
          (refuse! :refusal/observation-support-mismatch
                   "an observation row is not stated over the declared alphabet"
                   {:state s :row-support (set (keys row)) :outcomes outcome-set}))
        (when-not (row-sums-to-one? row)
          (refuse! :refusal/observation-row-unnormalised
                   "an observation row is not a distribution over the declared alphabet"
                   {:state s :sum (reduce + 0.0 (map (comp double val) row))}))))
    model))

(defn q-reading!
  "Validate `MachineQ.QReading` (`MachineQ.lean:84-101`) against `model` and
   return it.

     {:id          <keyword naming what was declared, for the receipt>
      :plan        {pi u}                 the controlled step pi commits to
      :belief-mass (fn [belief s] mass)}  the belief reading

   `:plan` is a MAP and not a function on purpose: it is the field a reader of
   a receipt has to be able to see, and `rowsEqualOfEqualPlans` is a statement
   about it. `:belief-mass` has to be a function, because the runtime belief is
   an open map of channel moments.

   The belief-reading obligations (`MachineQ.lean:94-97`) are stated for EVERY
   belief in Lean and cannot be checked that way here, so they are checked per
   call in `predicted-state-distribution` against the belief actually held --
   and a reading that fails there is refused, not silently renormalised."
  [{:keys [id plan belief-mass] :as reading} model]
  (when-not (keyword? id)
    (refuse! :refusal/reading-unnamed
             "a reading carries an :id, so a receipt can say what was declared" {}))
  (when-not (and (map? plan) (seq plan))
    (refuse! :refusal/reading-without-plan
             "a reading names the controlled step each policy commits to" {:id id}))
  (when-not (ifn? belief-mass)
    (refuse! :refusal/reading-without-belief-mass
             "a reading maps the belief onto the model's states" {:id id}))
  (doseq [[pi u] plan]
    (doseq [s (:states model)]
      (when-not (contains? (:transition model) [s u])
        (refuse! :refusal/planned-action-outside-transition-kernel
                 "a policy plans a step the transition kernel has no row for"
                 {:policy pi :action u :state s}))))
  reading)

;; ---------------------------------------------------------------------------
;; The composition
;; ---------------------------------------------------------------------------

(defn belief-distribution!
  "The reading applied to `belief`, checked as a distribution over the model's
   states. This is the join `Q-interface-completeness.edn` `:Q/in-belief`
   records as absent -- fourteen channel moments to a mass on each model state
   -- and it is a caller's obligation, so a failure here is a refusal naming
   the reading rather than a repair."
  [model reading belief]
  (let [mass (into {} (map (fn [s] [s (double ((:belief-mass reading) belief s))]))
                   (:states model))]
    (when-not (row-sums-to-one? mass)
      (refuse! :refusal/belief-reading-not-normalised
               "the belief reading is not a distribution over the model's states"
               {:reading (:id reading)
                :sum (reduce + 0.0 (vals mass))
                :mass mass}))
    mass))

(defn predicted-state-distribution
  "Q(s|pi): the belief pushed one controlled step through the transition kernel
   under pi's planned action. `MachineQ.machinePredictedStateKernel`
   (`MachineQ.lean:139-146`)."
  [model reading belief policy]
  (let [u (get (:plan reading) policy)
        b (belief-distribution! model reading belief)]
    (when (nil? u)
      (refuse! :refusal/policy-not-in-plan
               "the reading names no controlled step for this policy"
               {:policy policy :reading (:id reading)}))
    (into {}
          (map (fn [s']
                 [s' (reduce (fn [acc s]
                               (+ acc (* (get b s)
                                         (double (get-in model [:transition [s u] s'] 0.0)))))
                             0.0
                             (:states model))]))
          (:states model))))

(defn predictive-outcome-distribution
  "Q(o|pi): the predicted state distribution against the observation kernel's
   rows. `MachineQ.machinePredictiveOutcomeKernel` (`MachineQ.lean:187-198`).

   The returned row has already passed `predictive-outcome-row!`, so a caller
   that received one holds something the boundary accepted."
  [model reading belief policy]
  (let [q-s (predicted-state-distribution model reading belief policy)
        row (into {}
                  (map (fn [o]
                         [o (reduce (fn [acc s']
                                      (+ acc (* (get q-s s')
                                                (double (get-in model [:observation s' o] 0.0)))))
                                    0.0
                                    (:states model))]))
                  (:outcomes model))]
    (predictive-outcome-row! row (:outcomes model))))

(defn predictive-outcome-kernel
  "Every policy row of Q(o|pi), with the normalisation residual each row was
   accepted at.

     {:reading  <the reading's :id>
      :policies [pi ..]
      :rows     {pi {o mass}}
      :normalisation-residual {pi <|1 - sum|>}}

   The residual is a RECEIPT about these rows, not a proof: the general
   statement is `MachineQ.predictiveOutcomeMass_sum` (`MachineQ.lean:168-181`)."
  [model reading belief]
  (let [policies (vec (keys (:plan reading)))
        rows (into {} (map (fn [pi]
                             [pi (predictive-outcome-distribution model reading belief pi)]))
                   policies)]
    {:reading (:id reading)
     :policies policies
     :rows rows
     :normalisation-residual
     (into {} (map (fn [[pi row]]
                     [pi (Math/abs (- 1.0 (reduce + 0.0 (vals row))))]))
           rows)}))

(defn rows-equal-for-equal-plans?
  "`MachineQ.rowsEqualOfEqualPlans` (`MachineQ.lean:210-218`) as a check: for
   every pair of policies committing to the same controlled step, the rows
   agree. This is the falsifiable content of \"policy-conditioned\" here -- it
   says what a difference must come FROM. False means the runtime composition
   sees something about the policy that the Lean composition proves it cannot,
   which would mean the two are not the same object."
  [kernel plan]
  (every? (fn [[pi rho]]
            (let [a (get-in kernel [:rows pi])
                  b (get-in kernel [:rows rho])]
              (every? (fn [o] (< (Math/abs (- (double (get a o 0.0))
                                              (double (get b o 0.0))))
                                 tolerance))
                      (into #{} (concat (keys a) (keys b))))))
          (for [pi (keys plan)
                rho (keys plan)
                :when (= (get plan pi) (get plan rho))]
            [pi rho])))

;; ---------------------------------------------------------------------------
;; The R4 seam
;; ---------------------------------------------------------------------------

(defn seam-attachment
  "What the R4 seam attaches to a prediction, or nil.

   nil whenever the flag is clear or no caller has exhibited a reading, and nil
   is checked BEFORE anything is read or computed -- which is what makes the
   default path byte-identical to the one that existed before this namespace.

   When both are present, the attachment carries the whole kernel plus the row
   for `action`: at R4 the runtime's action IS `plan pi`, so the policies that
   reach this prediction are exactly those planning it, and by
   `rowsEqualOfEqualPlans` they share one row. When no policy plans this
   action, `:row` is absent with `:row-status :no-policy-plans-this-action` --
   a typed absence, never a flat default row."
  [belief action]
  (when (and (enabled?) (some? *seam-reading*))
    (let [{:keys [model reading]} *seam-reading*
          model (generative-model! model)
          reading (q-reading! reading model)
          kernel (predictive-outcome-kernel model reading belief)
          planners (filterv #(= (get (:plan reading) %) action) (:policies kernel))]
      (merge kernel
             {:planned-action action
              :policies-planning-action planners
              :law/rows-equal-for-equal-plans
              (rows-equal-for-equal-plans? kernel (:plan reading))}
             (if (seq planners)
               {:row (get-in kernel [:rows (first planners)])}
               {:row-status :no-policy-plans-this-action})))))

(defn refusal-reason
  "The `:refusal` key of a boundary refusal, or nil for any other throwable.
   Callers that want to record a refusal rather than propagate it read it
   through this so the reason stays a keyword and not a parsed message."
  [t]
  (when (instance? clojure.lang.ExceptionInfo t)
    (:refusal (ex-data t))))

(defn describe-refusal
  "A one-line rendering of a refusal, for receipts."
  [t]
  (when-let [reason (refusal-reason t)]
    (str/join " -- " [(name reason) (ex-message t)])))
