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
                               :units :nats}}))))
              (:entries evidence)))
        invalid (filterv #(seq (:absent-terms %)) rows)]
    (if (or malformed-top (seq invalid) (empty? rows))
      {:schema schema :status :refused
       :reason (or malformed-top (first (:absent-terms (first invalid))) :objective-input-invalid)
       :invalid invalid}
      (let [logits (mapv #(get-in % [:terms :logit]) rows)
            probabilities (softmax logits)
            scored (mapv #(assoc %1 :posterior %2) rows probabilities)
            minimum (apply min (map #(get-in % [:terms :objective]) scored))
            tied (filterv #(== (double minimum)
                               (double (get-in % [:terms :objective]))) scored)]
        (let [tie? (> (count tied) 1)
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
               :selected selected :policies scored :identity-check check
               :draw (when tie? {:law :uniform-over-equal-minima :u u
                                  :candidates (mapv :cascade-digest tied)})
               :menu-pin (:menu-pin evidence) :declaration-pin (:declaration-pin evidence)
               :dataset-pin (:dataset-pin evidence)
               :comparison {:law (if tie? :uniform-over-equal-minima :unique-strict-objective-minimum)
                            :objective :gamma-times-G-minus-lnE
                            :posterior :softmax-lnE-minus-gamma-G}})))))))))
