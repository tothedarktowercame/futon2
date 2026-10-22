(ns futon2.aif.outer-policy-objective
  "Pure policy-grain G/posterior objective over computed outer evidence."
  (:require [futon2.aif.parameter-novelty :as novelty]
            [futon2.aif.outer-policy-evidence :as policy-evidence]
            [futon2.aif.wm.family-selection :as family-selection]
            [futon2.aif.wm.selection-cascade-identity :as identity]))

(def schema :wm/outer-policy-objective-v1)

(defn- finite? [x] (and (number? x) (Double/isFinite (double x))))

(defn- kl [q c]
  (reduce + (map (fn [x y] (* (double x) (Math/log (/ (double x) (double y))))) q c)))

(defn ambiguity-nats [alpha]
  (let [a0 (double (reduce + alpha)) q (mapv #(/ (double %) a0) alpha)]
    (- (novelty/digamma (inc a0))
       (reduce + (map * q (map #(novelty/digamma (inc %)) alpha))))))

(defn- softmax [xs]
  (let [m (apply max xs) ws (mapv #(Math/exp (- (double %) m)) xs)
        z (reduce + ws)]
    (mapv #(/ % z) ws)))

(defn precision-conformance
  "Check the observed live-route precision fields against the part of Lean
  `PrecisionFromObservation.TickRecord.Conforms` that this route actually
  computes.  The live outer route has no F term or precision posterior, so a
  complete TickRecord never claims conformance here."
  [{:keys [E G F prior posterior beta-prior beta-posterior]}]
  (let [ids (vec (sort (keys E)))
        missing (vec (concat
                      (when (= :absent (:status F)) [:F])
                      (when (= :absent (:status posterior)) [:posterior])
                      (when (= :absent (:status beta-posterior)) [:beta-posterior])))
        failures (cond-> []
                   (not (and (finite? beta-prior) (pos? beta-prior)))
                   (conj :beta-prior-not-positive)
                   (some #(not (and (finite? (get E %)) (pos? (get E %)))) ids)
                   (conj :E-not-positive)
                   (some #(not (finite? (get G %))) ids)
                   (conj :G-not-finite)
                   (some #(not (finite? (get prior %))) ids)
                   (conj :prior-not-finite))
        expected (when (and (seq ids) (empty? failures))
                   (softmax (mapv #(- (Math/log (double (get E %)))
                                      (/ (double (get G %)) (double beta-prior)))
                                  ids)))
        failures (cond-> failures
                   (and expected
                        (some true? (map #(> (Math/abs (- (double (get prior %1))
                                                            (double %2)))
                                            1.0e-9)
                                         ids expected)))
                   (conj :prior-not-softmax))]
    (cond-> {:conforms false :missing missing}
      (seq failures) (assoc :failures failures))))

(defn precision-row
  "Project the outer objective's observed E/G/prior values into the R20 row.
  `:lnE` is what evaluate computes; E is retained here as `exp(lnE)` and that
  conversion is named so consumers never mistake the two quantities."
  [policies gamma]
  (let [by-id (fn [f] (into {} (map (juxt :policy-id f) policies)))
        row {:E (by-id #(Math/exp (double (get-in % [:terms :lnE]))))
             :E-source :exp-of-lnE-held-by-outer-policy-objective
             :G (by-id #(get-in % [:terms :G]))
             :F {:status :absent :reason :live-route-has-no-F-term}
             :prior (by-id :posterior)
             :posterior {:status :absent :reason :no-precision-update-on-live-route}
             :beta-prior (when (and (finite? gamma) (not (zero? (double gamma))))
                           (/ 1.0 (double gamma)))
             :beta-posterior {:status :absent :reason :no-precision-update-on-live-route}}]
    (assoc row :conforms (precision-conformance row))))

(defn evaluate
  "Compute policy terms and select a minimum, recording a uniform draw on ties.
  EVIDENCE must be computed; SEALED is used only for canonical identity
  transport and post-selection verification."
  ([evidence sealed] (evaluate evidence sealed nil))
  ([evidence sealed draw]
  (let [vocab (:outcome-vocabulary evidence)
        c (get-in evidence [:preference :distribution])
        gamma (:gamma evidence)
        entries-by-digest (into {} (map (juxt :cascade-digest clojure.core/identity)) (:entries sealed))
        evidence-population (set (map (juxt :policy-id :cascade-digest) (:entries evidence)))
        sealed-population (set (map (juxt :policy-id :cascade-digest) (:entries sealed)))
        malformed-top (cond
                        (policy-evidence/verify-evidence evidence)
                        (policy-evidence/verify-evidence evidence)
                        (family-selection/verify-sealed-menu sealed)
                        (family-selection/verify-sealed-menu sealed)
                        (not= (:menu-pin evidence) (:menu-pin sealed)) :menu-pin-mismatch
                        (not= evidence-population sealed-population) :policy-population-mismatch
                        (not (and (vector? vocab) (= vocab (get-in evidence [:preference :vocabulary]))))
                        :vocabulary-mismatch
                        (not (and (finite? gamma) (pos? gamma))) :gamma
                        :else nil)
        rows (when-not malformed-top (mapv
              (fn [row]
                (if (= :absent (:status row))
                  (assoc (select-keys row [:policy-id :target-id :cascade-digest
                                           :absent-terms :score-absence])
                         :absent-terms (:absent-terms row))
                  (let [q (get-in row [:posterior-predictive :distribution])
                      alpha (:posterior-alpha row) eig (get-in row [:eig :nats])
                      ln-e (:ln-e row) bound (get entries-by-digest (:cascade-digest row))
                      absent (cond-> []
                               (not (and (string? (:policy-id row)) (string? (:target-id row))
                                         (string? (:cascade-digest row)))) (conj :policy-identity)
                               (nil? bound) (conj :sealed-policy-binding)
                               (not= vocab (get-in row [:posterior-predictive :vocabulary]))
                               (conj :vocabulary)
                               (not (and (= (count vocab) (count q) (count alpha))
                                         (every? #(and (finite? %) (pos? %)) q)
                                         (every? #(and (finite? %) (pos? %)) c)
                                         (every? #(and (finite? %) (pos? %)) alpha)
                                         (finite? eig) (finite? ln-e))) (conj :finite-objective-input)
                               (and (seq q) (> (abs (- 1.0 (reduce + q))) 1.0e-10))
                               (conj :posterior-not-normalized)
                               (and (seq alpha) (seq q)
                                    (let [n (double (reduce + alpha))]
                                      (some true? (map #(> (abs (- (double %1) (/ (double %2) n))) 1.0e-10)
                                                       q alpha))))
                               (conj :posterior-alpha-mismatch)
                               (and (finite? ln-e)
                                    (not= ln-e (reduce + (map #(get-in row [:ln-e-components % :value])
                                                               [:resource :cadence :continuation]))))
                               (conj :ln-e-mismatch)
                               (and (seq alpha) (finite? eig)
                                    (> (abs (- eig (:nats (novelty/categorical-information vocab alpha))))
                                       1.0e-10))
                               (conj :eig-mismatch))]
                  (if (seq absent)
                    (assoc (select-keys row [:policy-id :target-id :cascade-digest])
                           :absent-terms absent)
                    (let [risk (kl q c) ambiguity (ambiguity-nats alpha)
                          g (- (+ risk ambiguity) eig)
                          objective (- (* gamma g) ln-e)]
                      {:policy-id (:policy-id row) :target-id (:target-id row)
                       :cascade-digest (:cascade-digest row)
                       :canonical-cascade (:canonical-cascade bound)
                       :terms {:risk risk :ambiguity ambiguity :novelty eig :G g
                               :lnE ln-e :lnE-components (:ln-e-components row)
                               :gamma gamma :objective objective :logit (- objective)
                               :units :nats}})))))
              (:entries evidence)))
        invalid (filterv #(seq (:absent-terms %)) rows)]
    (if malformed-top
      {:schema schema :status :refused
       :reason malformed-top
       :invalid invalid}
      (if (or (seq invalid) (empty? rows))
        (let [assessed (vec (remove #(seq (:absent-terms %)) rows))
              logits (mapv #(get-in % [:terms :logit]) assessed)
              posterior (when (seq assessed) (softmax logits))
              scored (mapv #(assoc %1 :posterior %2) assessed posterior)
              minimum (when (seq scored) (apply min (map #(get-in % [:terms :objective]) scored)))
              tied (when minimum (filterv #(== (double minimum)
                                                (double (get-in % [:terms :objective]))) scored))
              tie? (> (count tied) 1)
              u (when tie? (if (nil? draw) (rand) draw))
              selected (when (and (seq tied) (or (not tie?) (and (finite? u) (<= 0.0 u) (< u 1.0))))
                         (nth tied (if tie? (int (* u (count tied))) 0)))
              decision (when selected {:action {:type :advance-cascade-live-policy
                                                 :target (:target-id selected)
                                                 :policy-id (:policy-id selected)
                                                 :cascade-digest (:cascade-digest selected)}
                                       :selection-certificate {:selected-canonical-cascade (:canonical-cascade selected)}})
              check (when decision (identity/verify-selected-identity decision (:entries sealed)))]
         {:schema schema :status :partial
         :strict-verdict {:status :absent
                          :reason (or (first (:absent-terms (first invalid)))
                                      :objective-input-invalid)
                          :missing invalid}
         :policies scored
         :precision-row (precision-row scored gamma)
         :assessed-selection
         (cond
           (empty? assessed) {:status :no-choice :reason :preparation-needed
                              :scope :assessed-population :assessed-ids []}
           (and tie? (not (and (finite? u) (<= 0.0 u) (< u 1.0))))
           {:status :no-choice :reason :invalid-tie-draw :scope :assessed-population}
           (not= :verified (:status check))
           {:status :refused :reason :cascade-identity-mismatch :identity-check check
            :scope :assessed-population}
           :else {:status :selected :scope :assessed-population
                  :basis :gamma-times-G-minus-lnE-over-assessed-population
                  :action (:action decision) :selected selected :identity-check check
                  :assessed-ids (mapv #(select-keys % [:policy-id :target-id :cascade-digest]) assessed)
                  :absent (mapv #(select-keys % [:policy-id :target-id :cascade-digest :absent-terms :score-absence]) invalid)
                  :posterior-denominator (reduce + (map #(Math/exp (- (double %) (apply max logits))) logits))
                  :draw (when tie? {:law :uniform-over-equal-minima :u u
                                    :candidates (mapv :cascade-digest tied)})})
         :frontier {:strict-assessed (count assessed)
                    :strict-absent (count invalid)
                    :ordinary-assessed 0
                    :ordinary-basis :absent-no-comparable-policy-comparison}
         :menu-pin (:menu-pin evidence)
         :declaration-pin (:declaration-pin evidence)
         :dataset-pin (:dataset-pin evidence)})
      (let [logits (mapv #(get-in % [:terms :logit]) rows)
            probabilities (softmax logits)
            scored (mapv #(assoc %1 :posterior %2) rows probabilities)
            minimum (apply min (map #(get-in % [:terms :objective]) scored))
            tied (filterv #(== (double minimum)
                               (double (get-in % [:terms :objective]))) scored)
            tie? (> (count tied) 1)
            u (when tie? (if (nil? draw) (rand) draw))]
        (if (and tie? (not (and (finite? u) (<= 0.0 u) (< u 1.0))))
            {:schema schema :status :refused :reason :invalid-tie-draw}
          (let [selected (nth tied (if tie? (int (* u (count tied))) 0))
                decision {:action {:type :advance-cascade-live-policy
                                   :target (:target-id selected)
                                   :policy-id (:policy-id selected)
                                   :cascade-digest (:cascade-digest selected)}
                          :selection-certificate
                          {:selected-canonical-cascade (:canonical-cascade selected)}}
                check (identity/verify-selected-identity decision (:entries sealed))]
            (if (not= :verified (:status check))
              {:schema schema :status :refused :reason :cascade-identity-mismatch
               :identity-check check}
              {:schema schema :status :selected :action (:action decision)
               :selected selected :policies scored :precision-row (precision-row scored gamma)
               :identity-check check
               :draw (when tie? {:law :uniform-over-equal-minima :u u
                                  :candidates (mapv :cascade-digest tied)})
               :menu-pin (:menu-pin evidence) :declaration-pin (:declaration-pin evidence)
               :dataset-pin (:dataset-pin evidence)
               :comparison {:law (if tie? :uniform-over-equal-minima :unique-strict-objective-minimum)
                            :objective :gamma-times-G-minus-lnE
                            :posterior :softmax-lnE-minus-gamma-G}})))))))))
