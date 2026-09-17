(ns futon2.aif.zaif-full-loop-test
  "U6 (worklist.edn :U6) — ONE recorded zaif v0 decision driven through the War
   Machine's R nodes, one deftest per node, with planted expectations at every
   boundary. A regression names the node that broke.

   THE SUBJECT. zaif v0 is `futon3c/src/futon3c/agents/zaif_controller.clj`
   (270 lines) with its hydrator `zaif_inputs.clj` (209 lines): a pure
   per-decision argmax over four arms — :retrieve :act :ask :yield — scored by
   fixed documented constants. futon3c is NOT on futon2's classpath
   (futon2/deps.edn :deps), so this namespace cannot call `decide` live. It
   carries a FROZEN fixture instead, and closes that gap in two ways: the
   fixture's numbers were produced by the shipped controller
   (`u6_zaif_decision.clj`, artifact `U6-ZAIF-DECISION.txt`), and its SHAPE is
   re-derived here from the controller SOURCE on every run, so a drift in the
   constants or in the arm set fails this suite rather than passing silently.

   WHAT IS MEASURED AND WHAT IS PLANTED, stated before any number is read.
   MEASURED, from the shipped controller over all 114 recorded sessions:
   the decision itself, the arm distribution, the 83 tie-broken choices, the
   identically-zero :act term, and the ln E this suite feeds R6. PLANTED: the
   per-arm Q(o|pi) at R4 and the realised observation at R8. The plant is
   necessary because zaif v0 declares NO observation model — its arm values are
   dimensionless preference scores — so ANY placement of them into the WM's
   observation space is a declared choice and not a derivation. That choice is
   made in exactly one place, `plant-per-arm-prediction`, and
   `plant-dependence-negative-control-test` measures how much of the outcome it
   carries: on three different channels the R5 arm order is three different
   orders. NOTHING here may be read as a measurement of which arm is right.

   RE-GROUNDED BY U25 (wm-contract worklist :U25, from zaif :S7 remainder). The
   plant above is four separate declarations, and three of them now have a real
   counterpart in the S7/U12 live corpus. `plant-real-split` types each one
   :re-grounded or :plant-kept with its pointer or its reason, and the
   `u25-*` deftests below run the same walk on the grounded values and pin what
   moves. The two that stay planted stay planted for a stated reason, and
   `u25-grounded-plant-dependence-negative-control-test` shows the outcome is
   still theirs.

   Replay only: no live run, no run lock, nothing written under data/."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.belief :as belief]
            [futon2.aif.efe :as efe]
            [futon2.aif.forward-model :as fm]
            [futon2.aif.habit-prior :as habit]
            [futon2.aif.observation :as obs]
            [futon2.aif.policy :as policy]
            [futon2.aif.policy-free-energy :as pfe]
            [futon2.aif.preferences :as pref]))

;; ---------------------------------------------------------------------------
;; The frozen decision. Produced by the SHIPPED controller, not reconstructed:
;; holes/labs/wm-contract/u6_zaif_decision.clj against futon3c 0252995, output
;; frozen at holes/labs/wm-contract/U6-ZAIF-DECISION.txt (byte-identical on two
;; runs). A real recorded operator turn: gold-judged, labelled a correction,
;; attributed to the one mission whose B1 gamma cell is burned in.

(def ^:private recorded-decision
  {:session-id "e-ce907fcf-c7e1-4272-a4a2-13def7aaaa50"
   :session-at "2026-06-08T15:26:51.274850972Z"
   :is-correction true
   :arm :retrieve
   :g-terms {:retrieve 0.7622675616071356 :act 0.0 :ask 0.35 :yield 0.0}
   :gamma-used 0.7071067811865476
   :mission "M-futon-forward-model"
   :operator-attention-cost 0.65
   :inputs {:mission "M-futon-forward-model"
            :gamma {"M-futon-forward-model" {:policy-precision 0.7071067811865476}}
            :task-belief {}
            :c-belief {:operator-c-uncertainty 1.0}
            :observations {:posting-stats {:total-docs 80
                                           :dfs [1 1 1 1 1 1 1 1 1 1]
                                           :estimated-tokens 160}}}})

(def ^:private recorded-corpus
  "The corpus facts U6-ZAIF-DECISION.txt records, over all 114 recorded
   sessions replayed through the shipped controller and the live hydrator."
  {:sessions 114
   :arm-distribution {:retrieve 31 :act 83}
   :chosen-by-tie-break 83
   :tie-broken-arms {:act 83}
   :act-term-non-zero-on 0
   :task-belief-non-empty-on 0
   :distinct-gamma-used [0.7071067811865476 1.0]
   :distinct-ask-terms [-0.35000000000000003 0.35]})

(def ^:private modelled-arms
  "The three arms that carry a declared yield/cost model. :yield is the fourth
   arm and is EXCLUDED here on purpose: it is the bare constant
   `:yield-baseline` 0.0 (zaif_controller.clj:132) with no model behind it, so
   it has no Q(o|pi) to plant and no place at R4."
  [:retrieve :act :ask])

;; ---------------------------------------------------------------------------
;; The controller source, read on every run. This is what makes the frozen
;; fixture a pin rather than a copy.

(def ^:private zaif-controller-path
  "../futon3c/src/futon3c/agents/zaif_controller.clj")

(defn- controller-forms
  "Every top-level form of the controller source, as data. `read-string` and
   not `edn/read`, because the source carries #(...) and #{} literals."
  []
  (let [text (slurp zaif-controller-path)]
    (binding [*read-eval* false]
      (loop [rdr (java.io.PushbackReader. (java.io.StringReader. text))
             out []]
        (let [form (read {:eof ::eof :read-cond :preserve} rdr)]
          (if (= ::eof form) out (recur rdr (conj out form))))))))

(defn- source-constants
  "The `constants` map as the controller source declares it."
  [forms]
  (some (fn [form]
          (when (and (seq? form) (= 'def (first form)) (= 'constants (second form)))
            (last form)))
        forms))

(defn- source-arm-order
  "The arm vector `choose-arm` ranges over, in source order. This is also the
   tie-break order: `choose-arm` sorts by [(- value) position] and takes the
   first, so at equal values the earliest arm wins."
  [forms]
  (some (fn [form]
          (when (and (seq? form) (= 'choose-arm (second form)))
            (first (filter #(and (vector? %) (every? keyword? %) (seq %))
                           (tree-seq coll? seq form)))))
        forms))

;; ---------------------------------------------------------------------------
;; THE PLANT. One place, named, with its rule written out.

(def ^:private plant-channel
  "The single WM observation channel the arms' values are placed on. A DECLARED
   CHOICE. :mission-health is picked because it is the channel of
   `pref/current-C` whose preference range [0.5 1.0] a turn-level outcome on a
   mission can be said to bear on — but nothing in zaif_controller.clj licenses
   this or any other channel, which is the R4 gap this row records."
  :mission-health)

(def ^:private plant-base
  "The current value of the plant channel: the bottom of its preference range
   (pref/preferences :mission-health = [0.5 1.0]). Declared, not observed."
  0.5)

(def ^:private plant-variance
  "The predicted variance planted on every arm — THE SAME VALUE for all three.
   zaif v0 declares no per-arm variance anywhere in its source, so there is
   nothing to differentiate. 0.01 is `policy-free-energy`'s own
   `:variance-floor` default (policy_free_energy.clj:76-77), reused rather than
   invented. The consequence is measured in
   `r5-expected-free-energy-over-the-three-arms-test`: R5's ambiguity term is
   identical across the arms, so G's ordering is the risk ordering exactly."
  0.01)

(defn- clamp01 [x] (max 0.0 (min 1.0 (double x))))

(defn- plant-per-arm-prediction
  "Q(o|pi) for one arm, from that arm's declared yield/cost model.

   THE RULE, stated so it can be attacked: the arm's recorded G-term — which IS
   its declared yield minus its declared cost (zaif_controller.clj:63-96,
   129-132) — is added to `plant-base` on `plant-channel`, clamped to [0,1],
   and paired with `plant-variance`. The addition is dimensionally arbitrary:
   the G-term is a preference score and the channel is a health ratio. It is a
   plant, and `plant-dependence-negative-control-test` is what keeps that from
   being forgotten."
  ([arm] (plant-per-arm-prediction arm plant-channel plant-base plant-variance))
  ([arm channel base] (plant-per-arm-prediction arm channel base plant-variance))
  ([arm channel base variance]
   {:prediction-mean {channel (clamp01 (+ (double base)
                                          (double (get-in recorded-decision
                                                          [:g-terms arm]))))}
    :prediction-variance {channel (double variance)}}))

(defn- arm-risk
  "R5a. The SAME call efe.clj:659-668 makes: KL of the predicted Gaussian
   against the channel's preference density at the default C temperature."
  [prediction channel]
  (pref/kl {:kind :gaussian
            :mu (get-in prediction [:prediction-mean channel])
            :sigma2 (get-in prediction [:prediction-variance channel])}
           (pref/c-distribution (get (pref/current-C) channel)
                                :temperature pref/default-c-temperature)))

(defn- arm-ambiguity
  "R5b. The SAME private functional efe.clj:679 calls, reached through its var
   rather than reimplemented — the discipline C487 control 2 used."
  [prediction]
  (#'efe/ambiguity (:prediction-variance prediction) :gaussian-entropy))

(defn- arm-g
  "R5. G = risk + ambiguity (invariant I3, efe.clj:843-844)."
  [prediction channel]
  (+ (arm-risk prediction channel) (arm-ambiguity prediction)))

(def ^:private recorded-habit-state
  "R6's ln E, from the arm history the corpus actually recorded: 83 :act and
   31 :retrieve over 114 sessions, folded by the WM's own
   `habit-prior/observe-action`. :ask was never chosen at the shipped constant,
   so its ln E is the alpha-only floor — which is a fact about the recorded
   history, not a plant."
  (reduce habit/observe-action
          (habit/initial-state)
          (concat (repeat (get-in recorded-corpus [:arm-distribution :act])
                          {:type :act})
                  (repeat (get-in recorded-corpus [:arm-distribution :retrieve])
                          {:type :retrieve}))))

(defn- pipeline
  "The whole walk, for one plant. Returns every node's output so a test can
   assert on the node it is named for."
  ([] (pipeline plant-channel plant-base plant-variance plant-base))
  ([channel base] (pipeline channel base plant-variance base))
  ([channel base variance realised-value]
   (let [predictions (into {} (for [a modelled-arms]
                                [a (plant-per-arm-prediction a channel base variance)]))
         g (into {} (for [a modelled-arms] [a (arm-g (predictions a) channel)]))
         ;; G-ascending: lower controller-score is more preferred (efe.clj:904-919).
         order (vec (sort-by g modelled-arms))
         g-vec (mapv g order)
         log-e (habit/log-priors recorded-habit-state
                                 (mapv (fn [a] {:type a}) order))
         ;; R8. Under the plant, `realised-value` defaults to `base`: the
         ;; recorded turn is labelled a CORRECTION, so the planted realised
         ;; outcome is "no gain on the channel". U25's grounded walk passes the
         ;; live observation instead — see `plant-real-split`.
         realised {channel realised-value}
         f-pi (mapv #(pfe/f-pi-for-candidate (predictions %) realised) order)
         tau (let [spread (- (apply max g-vec) (apply min g-vec))]
               ;; planted τ, historically policy/effective-temperature's
               ;; :spread mode at g=1 (deleted with the flat selector, H6b
               ;; 2026-09-17); kept inline here as a FIXTURE constant so the
               ;; surviving seam tests (selection-scores/softmax-weights)
               ;; still have a temperature to run at.
               (max 0.01 (/ spread 5.0)))
         ranked (vec (map-indexed (fn [i a]
                                    {:action {:type a}
                                     :rank (inc i)
                                     :controller-score (g a)
                                     :habit-prior-bias (nth log-e i)})
                                  order))
         f-pi-opts {:f-pi-policy-posterior? true
                    :f-pi-values f-pi
                    :f-pi-scaling :unscaled
                    :f-pi-posterior {:status :applied
                                     :coverage :complete-by-construction
                                     :provenance :u6-planted-realised-outcome}}]
     {:channel channel :base base :variance variance :realised realised
      :predictions predictions :g g :order order :g-vec g-vec
      :log-e log-e :f-pi f-pi :tau tau :ranked ranked :f-pi-opts f-pi-opts
      :scores-flags-on (policy/selection-scores g-vec tau log-e f-pi-opts)
      :scores-flags-off (policy/selection-scores g-vec tau log-e)
      :posterior (policy/softmax-weights g-vec tau log-e f-pi-opts)
      ;; H6b (2026-09-17): the flat decisions (select-action at the strategic
      ;; boundary) were deleted; the walk's decision is the cascade selector
      ;; over the same ranked field at a declared β=1 (argmax over the same
      ;; min-G head for these single-action candidates).
      :cascade-decision (policy/select-action-cascades ranked {:beta 1.0})})))

(def ^:private run (memoize pipeline))

(defn- within? [tol a b] (< (Math/abs (- (double a) (double b))) (double tol)))

(defn- first-argmax-idx
  "The index of the FIRST maximal element — the same tie rule
   `policy/first-argmax` (policy.clj:525-536) applies, so a tie here cannot
   read as a law change either."
  [xs]
  (reduce (fn [best i] (if (> (double (nth xs i)) (double (nth xs best))) i best))
          0 (range (count xs))))

;; ---------------------------------------------------------------------------
;; The fixture is pinned against the source it came from.

(deftest zaif-fixture-shape-is-pinned-against-the-controller-source-test
  (testing "the controller source is where this suite says it is"
    (is (.exists (io/file zaif-controller-path))
        "U6's subject is futon3c's zaif v0; without the source the fixture is a copy, not a pin"))
  (when (.exists (io/file zaif-controller-path))
    (let [forms (controller-forms)
          consts (source-constants forms)]
      (testing "the shipped constants are exactly the seven the fixture assumes"
        (is (= {:retrieve-eig-scale 1.0
                :retrieve-token-cost 5.0E-4
                :default-retrieve-tokens 800
                :act-pragmatic-scale 1.0
                :ask-eig-scale 1.0
                :operator-attention-cost 0.65
                :yield-baseline 0.0}
               consts)))
      (testing "the fixture's operator-attention-cost IS the shipped constant"
        (is (= (:operator-attention-cost consts)
               (:operator-attention-cost recorded-decision))))
      (testing "the arm set and its tie-break order come from the source"
        (is (= [:retrieve :act :ask :yield] (source-arm-order forms)))
        (is (= #{:retrieve :act :ask :yield}
               (set (keys (:g-terms recorded-decision))))))
      (testing "the three modelled arms are the arm set minus the constant :yield"
        (is (= (set modelled-arms)
               (disj (set (source-arm-order forms)) :yield)))))))

;; ---------------------------------------------------------------------------
;; R2 — o. observation.clj:103-146 (`observe`), :11-33 (the channel list).

(deftest r2-observation-zaif-observation-space-is-disjoint-from-the-wm-channels-test
  (testing "zaif's observation channel is not one of the WM's"
    (let [zaif-keys (set (keys (get-in recorded-decision [:inputs :observations])))
          zaif-inner (set (keys (get-in recorded-decision
                                        [:inputs :observations :posting-stats])))]
      (is (= #{:posting-stats} zaif-keys))
      (is (= #{:total-docs :dfs :estimated-tokens} zaif-inner))
      (is (empty? (set/intersection
                   (set obs/observation-channels)
                   (set/union zaif-keys zaif-inner)))
          "R2 is STUBBED by zaif v0: nothing it observes is a WM channel")))
  (testing "the plant channel is a real WM channel with a real preference range"
    (is (contains? (set obs/observation-channels) plant-channel))
    (is (= [0.5 1.0] (get (pref/current-C) plant-channel)))
    (is (= plant-base (first (get (pref/current-C) plant-channel))))))

;; ---------------------------------------------------------------------------
;; R1 / R3 — mu and its update. belief.clj:62-92, :423-433.

(deftest r1-r3-belief-zaif-carries-no-belief-state-test
  (testing "the WM's belief node runs on the fixture's mission"
    (let [b (belief/initial-belief-state [(:mission recorded-decision)])]
      (is (contains? b (:mission recorded-decision)))
      (is (belief/valid-distribution? (get b (:mission recorded-decision))))))
  (testing "zaif's :task-belief is empty here and on every recorded session"
    (is (= {} (get-in recorded-decision [:inputs :task-belief])))
    (is (zero? (:task-belief-non-empty-on recorded-corpus))
        "R1/R3 are STUBBED: 0 of 114 recorded sessions carry any task belief"))
  (testing "the consequence at R14: gamma is read and then multiplied by nothing"
    ;; `act-value` (zaif_controller.clj:76-85) is the only consumer of
    ;; gamma-for-mission, and its belief term falls back to 0.0 on an empty
    ;; :task-belief. So gamma is READ on every decision and enters none of them.
    (is (= 0.0 (get-in recorded-decision [:g-terms :act])))
    (is (zero? (:act-term-non-zero-on recorded-corpus)))
    (is (not= 1.0 (:gamma-used recorded-decision))
        "the fixture's gamma is the burned-in cell, so this is not a uniform-prior artefact")))

;; ---------------------------------------------------------------------------
;; R4 — Q(o|pi). forward_model.clj:25-31 (the closed action-type set),
;; :311-355 (`predict`).

(deftest r4-forward-model-refuses-every-zaif-arm-test
  (testing "the WM's forward model is closed to the zaif arms"
    (is (empty? (set/intersection fm/action-types
                                          #{:retrieve :act :ask :yield}))))
  (testing "and says so by throwing, rather than scoring an unknown action"
    (doseq [arm [:retrieve :act :ask :yield]]
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo #"Invalid action for forward model"
           (fm/predict {:observation {} :belief {}} {:type arm :target :t}))
          (str "R4 is STUBBED for " arm ": no predict-effects arm exists")))))

(deftest r4-planted-per-arm-predictions-are-derived-only-from-the-declared-arm-model-test
  (let [{:keys [predictions]} (run)]
    (testing "one channel, one variance, three means"
      (is (= 3 (count predictions)))
      (doseq [a modelled-arms]
        (is (= #{plant-channel} (set (keys (get-in predictions [a :prediction-mean])))))
        (is (= plant-variance (get-in predictions [a :prediction-variance plant-channel])))))
    (testing "each mean is base + that arm's own recorded G-term, clamped"
      (is (= 1.0 (get-in predictions [:retrieve :prediction-mean plant-channel]))
          "0.5 + 0.762 clamps at the top of the channel")
      (is (= 0.5 (get-in predictions [:act :prediction-mean plant-channel])))
      (is (within? 1e-12 0.85 (get-in predictions [:ask :prediction-mean plant-channel]))))
    (testing "no per-arm variance exists to plant"
      (is (= 1 (count (distinct (map #(get-in % [:prediction-variance plant-channel])
                                     (vals predictions)))))))))

;; ---------------------------------------------------------------------------
;; R5 — G = risk + ambiguity. efe.clj:659-668 (risk), :37-61 (ambiguity),
;; :843-844 (the core identity).

(deftest r5-expected-free-energy-over-the-three-arms-test
  (let [{:keys [predictions g order]} (run)]
    (testing "G is exactly risk + ambiguity on every arm — no residual"
      (doseq [a modelled-arms]
        (is (= 0.0 (- (double (g a))
                      (+ (arm-risk (predictions a) plant-channel)
                         (arm-ambiguity (predictions a))))))))
    (testing "risk discriminates the three arms"
      (is (= 3 (count (distinct (map #(arm-risk (predictions %) plant-channel)
                                     modelled-arms))))))
    (testing "ambiguity does NOT, and the reason is in zaif's source"
      (is (= 1 (count (distinct (map #(arm-ambiguity (predictions %)) modelled-arms))))
          "zaif v0 declares no per-arm predicted variance, so R5b is inert for it")
      (is (within? 1e-12 -0.883646559789373 (arm-ambiguity (predictions :retrieve)))))
    (testing "the G ordering is therefore the risk ordering exactly"
      (is (= order (vec (sort-by #(arm-risk (predictions %) plant-channel)
                                 modelled-arms))))
      (is (= [:ask :act :retrieve] order)))
    (testing "and it is NOT zaif's own ordering"
      (is (= :retrieve (:arm recorded-decision)))
      (is (= :retrieve (last order))
          "the arm zaif chose is the WM's least preferred under this plant"))))

;; ---------------------------------------------------------------------------
;; R14 — tau. policy.clj:33-46 (spread), :77-146 (effective).

;; ---------------------------------------------------------------------------
;; R6 — ln E. habit_prior.clj:27-37 (identity), :100-119 (log-priors).

(deftest r6-habit-prior-ln-e-from-the-recorded-arm-history-test
  (let [{:keys [order log-e]} (run)]
    (testing "the folded history is the corpus arm distribution"
      (is (= 114 (:samples recorded-habit-state)))
      (is (= {[:act [:unscoped nil]] 83 [:retrieve [:unscoped nil]] 31}
             (:counts recorded-habit-state))))
    (testing "ln E is a proper log-distribution over the three arms"
      (is (within? 1e-12 1.0 (reduce + (map #(Math/exp %) log-e)))))
    (testing "the never-chosen arm carries the alpha-only floor"
      (let [by-arm (zipmap order log-e)]
        (is (= (apply min log-e) (by-arm :ask)))
        (is (= (apply max log-e) (by-arm :act)))
        (is (within? 1e-12 -4.762173934797756 (by-arm :ask)))))))

;; ---------------------------------------------------------------------------
;; R8 — F_pi. policy_free_energy.clj:41-144.

(deftest r8-policy-free-energy-per-arm-against-the-recorded-outcome-test
  (let [{:keys [order f-pi]} (run)
        by-arm (zipmap order f-pi)]
    (testing "every arm scores against the same realised observation"
      (is (= 3 (count f-pi)))
      (is (every? #(and (number? %) (Double/isFinite (double %))) f-pi)))
    (testing "the arm that predicted no change fits the corrected turn best"
      (is (= :act (key (apply min-key val by-arm))))
      (is (within? 1e-12 -1.383646559789373 (by-arm :act))))
    (testing "and the arm zaif chose fits it worst"
      (is (= :retrieve (key (apply max-key val by-arm))))
      (is (within? 1e-12 11.116353440210627 (by-arm :retrieve))))
    (testing "FORCED BY THE PLANT, not measured — stated so it is not read as a result"
      ;; The realised value IS `plant-base` and every planted mean is
      ;; `plant-base + a non-negative G-term`, so F_pi is monotone in the
      ;; planted gain by construction. What this node establishes is that the
      ;; WM's F_pi runs on zaif-shaped candidates at all, not which arm fits.
      (is (= (vec (sort-by by-arm modelled-arms))
             (vec (sort-by #(get-in recorded-decision [:g-terms %]) modelled-arms)))))))

;; ---------------------------------------------------------------------------
;; R6 — Q(pi). policy.clj:157-213 (the score), :215-231 (the posterior).

(deftest r6-policy-posterior-flags-on-test
  (let [{:keys [posterior scores-flags-on scores-flags-off order]} (run)]
    (testing "the posterior is a distribution over the three arms"
      (is (= 3 (count posterior)))
      (is (within? 1e-12 1.0 (reduce + posterior)))
      (is (every? #(<= 0.0 (double %) 1.0) posterior)))
    (testing "F_pi moves it: the flagged score is not the unflagged score"
      (is (not= scores-flags-on scores-flags-off))
      (is (= 3 (count (remove zero? (map - scores-flags-on scores-flags-off))))))
    (testing "the flagged argmax is :act; the unflagged argmax is :act too, and the reason differs"
      (let [amax (fn [ss] (nth order (first-argmax-idx ss)))]
        (is (= :act (amax scores-flags-on)))
        (is (= :act (amax scores-flags-off)))))
    (testing "the flags-on score subtracts F_pi unscaled at the one seam"
      (let [{:keys [g-vec tau log-e f-pi]} (run)]
        (is (= scores-flags-on
               (mapv (fn [s f] (- (double s) (double f)))
                     (policy/selection-scores g-vec tau log-e) f-pi)))))))

;; ---------------------------------------------------------------------------
;; R16 — u. policy.clj:503-519 (the laws), :594-596 (the argmax), :672-...

(deftest r16-action-the-three-laws-choose-three-different-arms-test
  ;; H6b (2026-09-17): the first two testing blocks pinned the two FLAT laws
  ;; (controller-head vs :full-score-posterior through policy/select-action)
  ;; and were deleted with the selector. What survives is the corpus fact the
  ;; node is named for: zaif's own R16 choice rule.
  (testing "zaif's own R16 is an argmax too — but 83 of 114 were tie-breaks"
      (is (= 83 (:chosen-by-tie-break recorded-corpus)))
      (is (= {:act 83} (:tie-broken-arms recorded-corpus))
          "at equal G-terms `choose-arm` takes the first arm in source order")
      (is (= 31 (- (:sessions recorded-corpus) (:chosen-by-tie-break recorded-corpus)))
          "31 of 114 recorded decisions were settled by a score")))

;; ---------------------------------------------------------------------------
;; R17 — a-conc, Delta-F. bmr.clj:108-138; a4a.clj:85-113. STUBBED BY DESIGN.

(deftest r17-constants-do-not-drift-and-nothing-learns-them-test
  (when (.exists (io/file zaif-controller-path))
    (let [text (slurp zaif-controller-path)
          consts (source-constants (controller-forms))]
      (testing "v0's constants are a literal map, fixed by design"
        (is (= 7 (count consts)))
        (is (every? number? (vals consts))))
      (testing "the fixture's decision was taken at those constants"
        (is (= (:operator-attention-cost consts)
               (:operator-attention-cost recorded-decision))))
      (testing "no learning touches them: no Dirichlet, no concentration, no update"
        (doseq [term ["dirichlet" "concentration" "posterior" "bayesian-model-reduction"]]
          (is (not (str/includes? (str/lower-case text) term))
              (str "R17 is STUBBED BY DESIGN; a '" term "' in the source would change that"))))
      (testing "the one thing beside the shipped constant is a RECORDED sweep, not a fold"
        ;; `dual-constants` (zaif_controller.clj:143-158) records a second cost
        ;; 0.15 for paired comparison. It is written into evidence and never
        ;; read back to move the shipped value — the R17 shape of "recorded but
        ;; never consulted".
        (is (str/includes? text "dual-constants"))
        (is (str/includes? text "NOT shipped"))))))

;; ---------------------------------------------------------------------------
;; The negative control: how much of the outcome is the plant carrying?

(deftest plant-dependence-negative-control-test
  (let [runs (for [[ch base] [[:mission-health 0.5]
                              [:support-coverage 0.8]
                              [:sorry-count-norm 0.0]]]
               (assoc (select-keys (run ch base) [:order])
                      :channel ch
                      :head (get-in (run ch base) [:cascade-decision :action :type])))]
    (testing "the arm ordering at R5 is a different order on each plant channel"
      (is (= [[:ask :act :retrieve]
              [:act :retrieve :ask]
              [:act :ask :retrieve]]
             (mapv :order runs)))
      (is (= 3 (count (distinct (map :order runs))))))
    (testing "so is the head law's choice"
      (is (= [:ask :act :act] (mapv :head runs))))
    (testing "the conclusion this licenses"
      ;; Which arm the WM prefers is a property of where the plant puts the arm
      ;; values, not of the arms. What survives the control is the STRUCTURE:
      ;; the pipeline runs end to end on zaif-shaped candidates, and the nodes
      ;; that cannot run are named in the coverage map below.
      (is (< 1 (count (distinct (map :order runs))))))))

;; ---------------------------------------------------------------------------
;; U25 — THE RE-GROUNDING. Which parts of the plant have a real counterpart.
;;
;; The plant above is not one declaration, it is four: the CHANNEL the arm
;; values land on, the channel's LEVEL, the predicted DISPERSION on it, and the
;; per-arm DELTA added to the level. U6 declared all four because zaif v0
;; declares no observation model. Three of them are quantities the WM itself
;; produces every tick, and the S7/U12 corpus has them measured on live records
;; — so those three are re-grounded here and only the two that have no
;; counterpart stay planted.

(def ^:private s7-corpus-dir
  "The S7 node corpus, harvested from live `data/wm-trace/` tick records by
   holes/labs/wm-contract/u12_c_mis_falsifier.clj. Tracked in this repo (39
   fixtures + README), so this suite reads records and not a re-derivation."
  "holes/labs/wm-contract/runs/U12-c-mis-falsifier/node-fixtures")

(def ^:private s7-run-ids
  "The three tick records the corpus covers, oldest first."
  ["0a18c4f7" "4abad68c" "801976e7"])

(def ^:private grounding-run-id
  "The run the pinned grounded numbers below are taken from. Fixed rather than
   'latest' so a fourth tick landing in the corpus cannot silently move a pin;
   `u25-grounding-values-are-stable-across-the-corpus-test` checks the other two
   agree to the digit that matters."
  "0a18c4f7")

(defn- s7-fixture
  "One (run-id, node) fixture as data, or nil if the corpus does not carry it."
  [run-id node]
  (let [f (io/file s7-corpus-dir (str run-id "-" (name node) ".edn"))]
    (when (.exists f) (edn/read-string (slurp f)))))

(defn- grounding-for
  "The three live quantities for one channel, from one tick record.
   :level and :variance come from the R8 fixture's own `:predicted-mean` /
   `:predicted-variance` (what the WM predicted for the channel before the
   observation landed); :realised is the observation that landed."
  [run-id channel]
  (let [pe (get-in (s7-fixture run-id :R8) [:value channel])]
    (when pe
      {:level (:predicted-mean pe)
       :variance (:predicted-variance pe)
       :realised (:observed pe)
       :error (:error pe)
       :producer-contract (:producer-contract pe)})))

(def ^:private grounding
  "The re-grounded values for the plant channel, at `grounding-run-id`."
  (grounding-for grounding-run-id plant-channel))

(def ^:private plant-real-split
  "U25's deliverable, as data: every declaration U6's plant makes, typed either
   :re-grounded (a real counterpart exists and this suite now uses it) or
   :plant-kept (none exists; the reason is a pointer, not a preference).
   :was is the U6 value; :now is the grounded value or ::plant."
  {:r4-channel-placement
   {:status :plant-kept
    :node :R4
    :was :mission-health
    :now ::plant
    :reason "no counterpart: nothing in zaif_controller.clj:1-270 names an observation channel, and its own observation input is :posting-stats (zaif_inputs.clj:153-167), disjoint from the WM's 14 channels (observation.clj:11-33). Which channel the arm values land on is still a declared choice."}
   :r4-level
   {:status :re-grounded
    :node :R4
    :was 0.5
    :now (:level grounding)
    :counterpart "S7 corpus 0a18c4f7-R8.edn :value :mission-health :predicted-mean — the WM's own predicted mean for this channel on a live tick, produced by futon2.aif.free-energy/compute-prediction-error under :prediction-error/v1"
    :note "U6 used the BOTTOM of the channel's preference range (pref/current-C :mission-health = [0.5 1.0]) because no measurement was to hand. One is."}
   :r4-dispersion
   {:status :re-grounded
    :node :R4
    :was 0.01
    :now (:variance grounding)
    :counterpart "S7 corpus 0a18c4f7-R8.edn :value :mission-health :predicted-variance"
    :note "U6 reused policy_free_energy.clj:76-77's :variance-floor default 0.01. The live predicted variance is ~94x that, and the sign of R5's ambiguity term turns over between the two — measured in u25-regrounded-r5-layer-test."}
   :r4-arm-conditional-delta
   {:status :plant-kept
    :node :R4
    :was :g-term-added-to-level
    :now ::plant
    :reason "no counterpart: forward_model.clj:25-31 excludes all four zaif arms and fm/predict throws on each, and no record in the S7 corpus is conditioned on a zaif arm — the corpus's R5 fixtures rank the WM's own 146 candidates, none of which is :retrieve/:act/:ask. Q(o|pi) for a zaif arm does not exist anywhere to be read."}
   :r8-realised-observation
   {:status :re-grounded
    :node :R8
    :was 0.5
    :now (:realised grounding)
    :counterpart "S7 corpus 0a18c4f7-R8.edn :value :mission-health :observed, equal to 0a18c4f7-R2.edn :value :mission-health — cross-checked in u25-regrounded-r8-layer-test"
    :note "U6 planted 'no gain on the channel' = the base. The live observation is not the base: it sits 0.383 BELOW the live prediction (:error -0.3829036175044988)."}})

(defn- grounded-run
  "The same walk as `run`, on the re-grounded level, dispersion and realised
   observation. The channel and the per-arm delta are still the plant."
  []
  (run plant-channel (:level grounding) (:variance grounding) (:realised grounding)))

(deftest u25-s7-corpus-is-where-this-suite-says-it-is-test
  (testing "the corpus directory carries a fixture for every (run, node) it claims"
    (is (.isDirectory (io/file s7-corpus-dir))
        "without the S7 corpus the re-grounding is a re-plant")
    (is (.exists (io/file s7-corpus-dir "README.md")))
    (is (= 39 (count (filter #(.endsWith (.getName ^java.io.File %) ".edn")
                             (.listFiles (io/file s7-corpus-dir))))))
    (doseq [rid s7-run-ids
            node [:R2 :R8]]
      (is (= :present (:status (s7-fixture rid node)))
          (str rid "-" (name node) " is the fixture this row grounds on"))))
  (testing "and the fixtures are records, not plants: each names its producer"
    (is (= "futon2.aif.observation/observe" (:via (s7-fixture grounding-run-id :R2))))
    (is (= "futon2.aif.free-energy/compute-prediction-error"
           (:via (s7-fixture grounding-run-id :R8))))
    (is (= :prediction-error/v1 (:producer-contract grounding)))))

(deftest u25-grounding-values-are-stable-across-the-corpus-test
  (testing "all three ticks carry the plant channel at R8"
    (is (every? some? (map #(grounding-for % plant-channel) s7-run-ids))))
  (let [gs (map #(grounding-for % plant-channel) s7-run-ids)]
    (testing "the observation is IDENTICAL on all three ticks"
      ;; The three records are three ticks of one quiet run; the mission-health
      ;; channel did not move between them. Pinned so a corpus that grows with
      ;; a moving channel is visible rather than averaged away.
      (is (= 1 (count (distinct (map :realised gs)))))
      (is (within? 1e-15 0.023376623376623377 (:realised grounding))))
    (testing "the predicted mean and variance drift only in the 4th decimal"
      (is (every? #(within? 1e-3 (:level grounding) (:level %)) gs))
      (is (every? #(within? 1e-3 (:variance grounding) (:variance %)) gs))
      (is (< 1 (count (distinct (map :level gs))))
          "they DO differ — the pin is to one record, not to a constant"))
    (testing "the pinned grounding values"
      (is (within? 1e-15 0.4062802408811222 (:level grounding)))
      (is (within? 1e-15 0.941315784435399 (:variance grounding)))
      (is (within? 1e-15 -0.3829036175044988 (:error grounding))))))

(deftest u25-plant-real-split-is-complete-and-typed-test
  (testing "every declaration the plant makes is in the split, and typed"
    (is (= #{:r4-channel-placement :r4-level :r4-dispersion
             :r4-arm-conditional-delta :r8-realised-observation}
           (set (keys plant-real-split))))
    (doseq [[k {:keys [status node was now reason counterpart]}] plant-real-split]
      (is (contains? #{:re-grounded :plant-kept} status) (str k " is untyped"))
      (is (contains? #{:R4 :R8} node))
      (is (some? was))
      (if (= :re-grounded status)
        (do (is (number? now) (str k " claims a counterpart but has no value"))
            (is (and (string? counterpart) (< 40 (count counterpart)))
                (str k " must cite the record it was grounded on"))
            (is (nil? reason)))
        (do (is (= ::plant now))
            (is (and (string? reason) (< 40 (count reason)))
                (str k " must say WHY no counterpart exists, with a pointer"))
            (is (nil? counterpart))))))
  (testing "the split is a split: three re-grounded, two kept"
    (is (= #{:r4-level :r4-dispersion :r8-realised-observation}
           (set (for [[k {:keys [status]}] plant-real-split
                      :when (= :re-grounded status)] k))))
    (is (= #{:r4-channel-placement :r4-arm-conditional-delta}
           (set (for [[k {:keys [status]}] plant-real-split
                      :when (= :plant-kept status)] k)))))
  (testing "the re-grounded values are the ones the grounded walk actually uses"
    (let [{:keys [base variance realised]} (grounded-run)]
      (is (= (get-in plant-real-split [:r4-level :now]) base))
      (is (= (get-in plant-real-split [:r4-dispersion :now]) variance))
      (is (= {plant-channel (get-in plant-real-split [:r8-realised-observation :now])}
             realised)))))

(deftest u25-regrounded-r5-layer-test
  (let [planted (run)
        grounded (grounded-run)]
    (testing "R5b still cannot discriminate — the finding survives re-grounding"
      ;; zaif declares no per-arm variance, so the grounded variance is the SAME
      ;; number on every arm just as the planted floor was. This is the U6
      ;; finding that does not depend on the plant.
      (is (= 1 (count (distinct (map #(arm-ambiguity (get-in grounded [:predictions %]))
                                     modelled-arms))))))
    (testing "but its VALUE turns over: the ambiguity term changes sign"
      (is (within? 1e-12 -0.883646559789373
                   (arm-ambiguity (get-in planted [:predictions :act]))))
      (is (within? 1e-12 1.38870022730075
                   (arm-ambiguity (get-in grounded [:predictions :act]))))
      (is (neg? (arm-ambiguity (get-in planted [:predictions :act]))))
      (is (pos? (arm-ambiguity (get-in grounded [:predictions :act])))))
    (testing "and no arm keeps its R5 rank between plant and grounding"
      (is (= [:ask :act :retrieve] (:order planted)))
      (is (= [:retrieve :ask :act] (:order grounded)))
      ;; Not a reversal — a rotation: :retrieve last -> first, :act middle ->
      ;; last, :ask first -> middle. Every rank moves, which is the point.
      (is (empty? (filter #(= (.indexOf ^java.util.List (:order planted) %)
                              (.indexOf ^java.util.List (:order grounded) %))
                          modelled-arms))))
    (testing ":retrieve's predicted mean saturates the clamp under BOTH"
      ;; 0.5 + 0.762 and 0.406 + 0.762 both exceed 1.0, so the re-grounding does
      ;; not recover a distinction the clamp had destroyed.
      (is (= 1.0 (get-in planted [:predictions :retrieve :prediction-mean plant-channel])))
      (is (= 1.0 (get-in grounded [:predictions :retrieve :prediction-mean plant-channel]))))))

(deftest u25-regrounded-r8-layer-test
  (let [{:keys [realised order f-pi]} (grounded-run)
        by-arm (zipmap order f-pi)]
    (testing "the realised observation is the record's, and the two records agree"
      (is (= (get-in (s7-fixture grounding-run-id :R2) [:value plant-channel])
             (get-in (s7-fixture grounding-run-id :R8) [:value plant-channel :observed]))
          "R8's :observed IS R2's observation — the corpus is internally coherent")
      (is (= {plant-channel 0.023376623376623377} realised)))
    (testing "F_pi runs on the grounded layer"
      (is (= 3 (count f-pi)))
      (is (every? #(and (number? %) (Double/isFinite (double %))) f-pi))
      (is (within? 1e-12 0.9665780142890833 (by-arm :act)))
      (is (within? 1e-12 1.1740177166241454 (by-arm :ask)))
      (is (within? 1e-12 1.3953278837887004 (by-arm :retrieve))))
    (testing "STILL monotone in the planted gain — but now for a stated reason"
      ;; Under the plant this was forced by construction (realised = base, every
      ;; mean = base + a non-negative gain). Under grounding it is a FACT about
      ;; the tick: the live observation 0.0234 fell 0.383 BELOW the live
      ;; prediction 0.4063, so every arm's planted gain moves the prediction
      ;; further from what happened. The ordering is still not a measurement of
      ;; which arm is right — the gain is still planted.
      (is (neg? (:error grounding)))
      (is (< (:realised grounding) (:level grounding)))
      (is (= (vec (sort-by by-arm modelled-arms))
             (vec (sort-by #(get-in recorded-decision [:g-terms %]) modelled-arms)))))))

(deftest u25-grounded-plant-dependence-negative-control-test
  ;; The control U6 ran, re-run with the plant REMOVED from level and dispersion
  ;; — every channel now carries its own live predicted mean and variance from
  ;; the same tick record. If the outcome were now a property of the arms, the
  ;; order would be the same on every channel. It is not: the channel placement
  ;; that `plant-real-split` keeps as a plant is still carrying the result.
  (let [r8 (:value (s7-fixture grounding-run-id :R8))
        channels (sort (keys r8))
        runs (for [ch channels
                   :let [{:keys [level variance realised]} (grounding-for grounding-run-id ch)]]
               {:channel ch
                :order (:order (run ch level variance realised))
                :head (get-in (run ch level variance realised)
                              [:cascade-decision :action :type])})]
    (testing "the corpus carries seven channels at R8, all grounded"
      (is (= 7 (count channels)))
      (is (= [:active-repo-ratio :annotation-health :attack-coverage :coupling-density
              :mission-health :support-coverage :ticks-firing-ratio]
             (vec channels))))
    (testing "three different R5 orders and head choices remain"
      (is (= 3 (count (distinct (map :order runs)))))
      (is (= #{[:retrieve :ask :act] [:act :retrieve :ask] [:act :ask :retrieve]}
             (set (map :order runs))))
      ;; H6b: heads are now the cascade posterior's choice (β=1, tie-break
      ;; :action-name-ascending) instead of the flat controller-head order,
      ;; so a G-tie resolves to :ask here where the old head took the first
      ;; ranked arm — still a plant-dependence result, one arm per placement.
      (is (= #{:retrieve :act :ask} (set (map :head runs)))))
    (testing "the conclusion this licenses, and its limit"
      ;; Re-grounding removed two invented numbers and left the arm-conditional
      ;; delta and the channel placement planted. So the U6 prohibition stands
      ;; unchanged: nothing here is a measurement of which arm is right. What
      ;; U25 adds is that U6's specific R16 result was an artefact of the two
      ;; numbers that did have counterparts.
      (is (< 1 (count (distinct (map :order runs))))))))

;; ---------------------------------------------------------------------------
;; The honest gap list, as data.

(def r-node-coverage
  "Which R nodes zaif v0 genuinely exercises and which it stubs. Every :basis
   is a pointer into source. :exercises means the node's quantity exists in
   zaif v0 and does work; :partial means it exists and is measurably inert;
   :stub means it does not exist."
  {:R2 {:status :stub
        :quantity :o
        :basis "zaif_inputs.clj:153-167 emits :posting-stats; observation.clj:11-33 lists the WM's 14 channels; the two sets are disjoint"}
   :R1 {:status :stub
        :quantity :mu
        :basis "zaif_inputs.clj:190-194 sets :task-belief {}; 0 of 114 recorded sessions carry one"}
   :R3 {:status :stub
        :quantity :mu-next
        :basis "no belief update exists in zaif_controller.clj; decide() is stateless across turns"}
   :R4 {:status :stub
        :quantity :Q-o-pi
        :basis "forward_model.clj:25-31 excludes all four arms; fm/predict throws on each"}
   :R5 {:status :partial
        :quantity :G
        :basis "zaif_controller.clj:129-132 builds a scalar yield-minus-cost per arm, not risk+ambiguity; no per-arm variance is declared anywhere in the source, so R5b cannot discriminate"}
   :R7 {:status :stub
        :quantity :Pi
        :basis "no prediction-error history and no variance exist to take a precision of"}
   :R8 {:status :stub
        :quantity :F-pi
        :basis "nothing in zaif_controller.clj scores a previous prediction against a later observation"}
   :R14 {:status :partial
         :quantity :tau
         :basis "gamma-for-mission (zaif_controller.clj:34-44) IS read every decision, but its only consumer is act-value (:76-85), whose belief term is absent on 114 of 114 sessions, so gamma multiplies 0.0 and enters no decision; there is no tau"}
   :R6 {:status :stub
        :quantity :Q-pi
        :basis "choose-arm (zaif_controller.clj:98-103) is a raw argmax; no softmax, no ln E, no habit prior"}
   :R16 {:status :exercises
         :quantity :u
         :basis "choose-arm (zaif_controller.clj:98-103) is a deterministic argmax with a fixed tie order, which is the same class of rule as dacosta2020 eq. 11; on the recorded corpus 83 of 114 choices were settled by that tie order and 31 by a score"}
   :R17 {:status :stub-by-design
         :quantity :a-conc
         :basis "constants are a literal map (zaif_controller.clj:11-20) with no update path; dual-constants (:143-158) records a second cost for paired comparison and never folds it back"}})

(deftest zaif-r-node-coverage-is-complete-and-typed-test
  (testing "every node U6's statement names is in the coverage map"
    (is (= #{:R1 :R2 :R3 :R4 :R5 :R6 :R7 :R8 :R14 :R16 :R17}
           (set (keys r-node-coverage)))))
  (testing "every entry is typed and carries a basis pointer"
    (doseq [[node {:keys [status quantity basis]}] r-node-coverage]
      (is (contains? #{:exercises :partial :stub :stub-by-design} status)
          (str node " has an untyped status"))
      (is (keyword? quantity))
      (is (and (string? basis) (< 40 (count basis)))
          (str node " has no basis"))))
  (testing "the gap list is a gap list: exactly one node is exercised"
    (is (= [:R16] (vec (for [[n {:keys [status]}] r-node-coverage
                             :when (= :exercises status)] n))))
    (is (= #{:R5 :R14} (set (for [[n {:keys [status]}] r-node-coverage
                                  :when (= :partial status)] n))))
    (is (= :stub-by-design (get-in r-node-coverage [:R17 :status]))
        "U6's statement predicted R17 on the gap list; it is there and typed")))
