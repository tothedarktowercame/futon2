#!/usr/bin/env bb
;; a_matrix_simulation.bb — M-aif-a-matrix-faithfulness Stage 2 simulation spike.
;;
;; SYNTHETIC SIMULATION: generate entity histories with varied event types,
;; non-uniform priors, and known ground-truth statuses. Compare belief tracking
;; under three configurations: :legacy, :aif (observation-model-v1), and
;; :aif + carry. The current real corpus cannot distinguish the models (shadow
;; null: 0 flips, KL ≈ 0.001). This simulation constructs the corpus the real
;; system lacks and tests whether off-diagonal A produces measurably better
;; beliefs.
;;
;; READ-ONLY: no trace writes. Deterministic: same seed ⇒ same output.
;; Run: bb --classpath src scripts/aif/a_matrix_simulation.bb [out-edn]
(require '[clojure.pprint :as pp]
         '[futon2.aif.belief :as belief])

;; ---------------------------------------------------------------------------
;; 1. Ground-truth entity generation
;; ---------------------------------------------------------------------------

(defn- rng
  "Simple deterministic RNG (linear congruential). Returns [next-state value]."
  [seed]
  (let [next-seed (mod (+ (* seed 1103515245) 12345) (bit-shift-left 1 31))
        v (/ (double next-seed) (bit-shift-left 1 31))]
    [next-seed v]))

(defn- sample-categorical
  "Sample from a categorical distribution {key probability}. Returns [seed key]."
  [seed dist]
  (let [[s1 u] (rng seed)
        cumulative (reductions (fn [[_ acc] [k p]] [k (+ acc p)])
                               [nil 0.0] dist)]
    [s1 (first (some (fn [[k c]] (when (> c u) [k c])) (rest cumulative)))]))

(defn- lifecycle-transition
  "Rough lifecycle transition kernel for ground-truth status.
   Not uniform — models the directional lifecycle:
   spawned → refined → strengthened → addressed (with branching)."
  [seed current-status]
  (let [transitions
        {:spawned     {:spawned 0.3 :refined 0.5 :strengthened 0.1 :falsified 0.1}
         :refined     {:refined 0.3 :strengthened 0.4 :addressed 0.15 :reopened 0.15}
         :strengthened {:strengthened 0.5 :addressed 0.35 :falsified 0.15}
         :addressed   {:addressed 0.6 :reopened 0.2 :foreclosed 0.2}
         :falsified   {:falsified 0.9 :reopened 0.1}
         :foreclosed  {:foreclosed 0.9 :reopened 0.1}
         :reopened    {:reopened 0.2 :refined 0.5 :strengthened 0.3}}
        dist (or (get transitions current-status)
                 {current-status 1.0})]
    (sample-categorical seed dist)))

;; ---------------------------------------------------------------------------
;; 2. Observation generation from a noisy "true" A
;; ---------------------------------------------------------------------------

(defn- observation-for-status
  "Generate a noisy observation given true status. Uses a realistic
   confusion matrix: observations usually match the true status but can
   be lifecycle-adjacent or contradictory with small probability."
  [seed true-status]
  (let [confusion
        {:spawned      {:spawned 0.70 :refined 0.20 :strengthened 0.10}
         :refined      {:refined 0.60 :spawned 0.15 :strengthened 0.15 :addressed 0.10}
         :strengthened {:strengthened 0.65 :refined 0.15 :falsified 0.05 :addressed 0.15}
         :addressed    {:addressed 0.65 :strengthened 0.15 :refined 0.10 :foreclosed 0.10}
         :falsified    {:falsified 0.70 :strengthened 0.10 :foreclosed 0.20}
         :foreclosed   {:foreclosed 0.70 :addressed 0.10 :falsified 0.20}
         :reopened     {:reopened 0.55 :refined 0.25 :addressed 0.20}}]
    (sample-categorical seed (or (get confusion true-status)
                                  {true-status 1.0}))))

;; ---------------------------------------------------------------------------
;; 3. Run the simulation
;; ---------------------------------------------------------------------------

(defn- simulate-entity
  "Simulate one entity over T ticks. Returns a sequence of
   {:tick :true-status :observation :weight} records."
  [seed n-ticks]
  (loop [s seed
         t 0
         status :spawned
         history []]
    (if (>= t n-ticks)
      history
      (let [[s1 new-status] (if (zero? t)
                              [s :spawned]
                              (lifecycle-transition s status))
            [s2 obs] (observation-for-status s1 new-status)]
        (recur s2
               (inc t)
               new-status
               (conj history {:tick t :true-status new-status
                              :observation obs :weight 1.0}))))))

(defn- track-belief
  "Track belief for one entity's history under a given mode.
   Returns the posterior at each tick."
  [history mode-opts]
  (loop [posterior (belief/uniform-prior)
         remaining history
         posteriors []]
    (if (empty? remaining)
      posteriors
      (let [record (first remaining)
            event {:type (:observation record) :weight (:weight record)}
            new-posterior (belief/update-entity-belief posterior event mode-opts)]
        (recur new-posterior
               (rest remaining)
               (conj posteriors new-posterior))))))
;; ---------------------------------------------------------------------------
;; 4. Metrics
;; ---------------------------------------------------------------------------

(defn- kl-divergence
  "KL(p || q) in nats. Returns 0 if p is zero where q is nonzero."
  [p q]
  (reduce +
          (for [[k pk] p
                :when (pos? pk)]
            (let [qk (double (get q k 0.0))]
              (if (pos? qk)
                (* pk (Math/log (/ pk qk)))
                0.0)))))

(defn- status-accuracy
  "Fraction of ticks where argmax of posterior matches true status."
  [posteriors history]
  (/ (count (filter identity
                    (map (fn [post record]
                           (= (belief/most-likely-status post)
                              (:true-status record)))
                         posteriors history)))
     (double (count history))))

(defn- mean-posterior-entropy
  "Mean entropy of posteriors (nats). Lower = more informative."
  [posteriors]
  (/ (reduce + (map belief/entropy posteriors))
     (double (count posteriors))))

(defn- true-status-probability
  "Mean probability assigned to the true status across ticks."
  [posteriors history]
  (/ (reduce + (map (fn [post record]
                      (double (get post (:true-status record) 0.0)))
                    posteriors history))
     (double (count history))))

(defn- contradiction-sensitivity
  "When observing :strengthened for an entity whose true status is :falsified,
   does the model correctly suppress :falsified? Measures mean (:falsified
   posterior mass) in those ticks vs the legacy."
  [posteriors history]
  (let [contradiction-ticks (keep-indexed
                              (fn [i record]
                                (when (and (= (:observation record) :strengthened)
                                           (= (:true-status record) :falsified))
                                  i))
                              history)]
    (if (empty? contradiction-ticks)
      nil
      (/ (reduce + (map (fn [i] (double (get (nth posteriors i) :falsified 0.0)))
                        contradiction-ticks))
         (double (count contradiction-ticks))))))

;; ---------------------------------------------------------------------------
;; 5. Main
;; ---------------------------------------------------------------------------

(defn -main
  [& args]
  (let [n-entities 50
        n-ticks 10
        base-seed 42
        out-file (or (first args)
                     "holes/labs/M-aif-faithfulness/a-matrix-simulation.edn")

        ;; Generate synthetic entities
        entities (loop [seed base-seed
                        i 0
                        acc []]
                   (if (>= i n-entities)
                     acc
                     (let [history (simulate-entity seed n-ticks)]
                       (recur (first (rng seed))
                              (inc i)
                              (conj acc history)))))

        ;; Event-type census (confirming the corpus has variation)
        event-type-census
        (let [all-events (mapcat (fn [h] (map :observation h)) entities)]
          (frequencies all-events))

        ;; Track beliefs under three configurations
        results
        (for [mode-label [[:legacy {}]
                          [:aif {:likelihood-mode :aif}]
                          [:aif-identity {:likelihood-mode :aif
                                          :observation-model belief/observation-model-identity}]]]
          (let [[label opts] mode-label
                per-entity (for [history entities]
                             (let [posteriors (track-belief history opts)
                                   acc (status-accuracy posteriors history)
                                   ent (mean-posterior-entropy posteriors)
                                   tsp (true-status-probability posteriors history)
                                   cs  (contradiction-sensitivity posteriors history)]
                               {:accuracy acc :entropy ent
                                :true-status-prob tsp
                                :contradiction-falsified-mass cs}))
                n (count per-entity)]
            {:mode label
             :status-accuracy (/ (reduce + (map :accuracy per-entity)) n)
             :mean-entropy (/ (reduce + (map :entropy per-entity)) n)
             :mean-true-status-prob (/ (reduce + (map :true-status-prob per-entity)) n)
             :contradiction-falsified-mass
             (let [cs-values (keep :contradiction-falsified-mass per-entity)]
               (when (seq cs-values)
                 (/ (reduce + cs-values) (count cs-values))))}))

        ;; KL divergence: :aif vs :legacy posteriors on the same histories
        kl-aif-vs-legacy
        (let [pairs (for [history entities]
                      (let [p-aif (last (track-belief history {:likelihood-mode :aif}))
                            p-leg (last (track-belief history {}))]
                        (kl-divergence p-aif p-leg)))]
          {:mean (/ (reduce + pairs) (count pairs))
           :max (reduce max pairs)
           :median (nth (sort pairs) (int (/ (count pairs) 2)))})

        acc-legacy (->> results (filter #(= :legacy (:mode %))) first :status-accuracy)
        acc-aif    (->> results (filter #(= :aif (:mode %))) first :status-accuracy)
        ent-legacy (->> results (filter #(= :legacy (:mode %))) first :mean-entropy)
        ent-aif    (->> results (filter #(= :aif (:mode %))) first :mean-entropy)
        headline   (format "Accuracy: legacy %.3f vs aif %.3f | Entropy: legacy %.3f vs aif %.3f | KL(aif‖legacy) mean %.4f"
                           acc-legacy acc-aif ent-legacy ent-aif (:mean kl-aif-vs-legacy))
        artifact
        {:method
         "Synthetic simulation: 50 entities × 10 ticks, lifecycle transition kernel,
            noisy observation confusion matrix. Compare :legacy, :aif (observation-model-v1),
            and :aif-identity. Metrics: status accuracy, posterior entropy, true-status
            probability, contradiction sensitivity, KL divergence."
         :parameters {:n-entities n-entities :n-ticks n-ticks :seed base-seed}
         :event-type-census event-type-census
         :results results
         :kl-aif-vs-legacy kl-aif-vs-legacy
         :headline headline
         :sim-only true
         :generated-by "scripts/aif/a_matrix_simulation.bb"
         :read-only true}]

    (pp/pprint artifact)
    (spit out-file (with-out-str (pp/pprint artifact)))
    (println "\nArtifact written to" out-file)
    (println "Headline:" headline)))

(-main *command-line-args*)
