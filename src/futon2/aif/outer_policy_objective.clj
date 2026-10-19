(ns futon2.aif.outer-policy-objective
  "Pure policy-grain G/posterior objective over computed outer evidence."
  (:require [futon2.aif.parameter-novelty :as novelty]
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
  "Compute policy terms and select the unique strict objective minimum.
  EVIDENCE must be computed; SEALED is used only for canonical identity
  transport and post-selection verification."
  [evidence sealed]
  (let [vocab (:outcome-vocabulary evidence)
        c (get-in evidence [:preference :distribution])
        gamma (:gamma evidence)
        entries-by-digest (into {} (map (juxt :cascade-digest clojure.core/identity)) (:entries sealed))
        malformed-top (cond
                        (not= :wm/outer-policy-evidence-v1 (:schema evidence)) :evidence-schema
                        (not= :computed (:status evidence)) :evidence-not-computed
                        (not (and (vector? vocab) (= vocab (get-in evidence [:preference :vocabulary]))))
                        :vocabulary-mismatch
                        (not (and (finite? gamma) (pos? gamma))) :gamma
                        :else nil)
        rows (mapv
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
                                         (finite? eig) (finite? ln-e))) (conj :finite-objective-input))]
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
              (:entries evidence))
        invalid (filterv #(seq (:absent-terms %)) rows)]
    (if (or malformed-top (seq invalid) (empty? rows))
      {:schema schema :status :refused :reason (or malformed-top :objective-input-invalid)
       :invalid invalid}
      (let [logits (mapv #(get-in % [:terms :logit]) rows)
            probabilities (softmax logits)
            scored (mapv #(assoc %1 :posterior %2) rows probabilities)
            minimum (apply min (map #(get-in % [:terms :objective]) scored))
            tied (filterv #(== (double minimum)
                               (double (get-in % [:terms :objective]))) scored)]
        (if (not= 1 (count tied))
          {:schema schema :status :abstained :reason :no-unique-policy-objective-minimum
           :verdict :fail :tied (mapv #(select-keys % [:policy-id :cascade-digest]) tied)
           :absent-terms [:strict-policy-discrimination] :policies scored}
          (let [selected (first tied)
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
               :menu-pin (:menu-pin evidence) :declaration-pin (:declaration-pin evidence)
               :dataset-pin (:dataset-pin evidence)
               :comparison {:law :unique-strict-objective-minimum
                            :objective :gamma-times-G-minus-lnE
                            :posterior :softmax-lnE-minus-gamma-G}})))))))
