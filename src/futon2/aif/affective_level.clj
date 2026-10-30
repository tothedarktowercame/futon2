(ns futon2.aif.affective-level
  "Pure mirror of R20Run's slow valence level (Hesp et al. 2021 §3.3).
  The identity B in the supplied levels is an example choice: Hesp do not
  print their affective transition matrix.  This namespace has no runtime
  wiring.")

(def ^:private valences [:pos :neg])
(def ^:private tolerance 1.0e-9)

(def hesp-level
  {:rate {:pos 0.5 :neg 2.0}
   :B {[:pos :pos] 1.0 [:pos :neg] 0.0
       [:neg :pos] 0.0 [:neg :neg] 1.0}})

(def flat-level
  {:rate {:pos 1.0 :neg 1.0}
   :B {[:pos :pos] 1.0 [:pos :neg] 0.0
       [:neg :pos] 0.0 [:neg :neg] 1.0}})

(defn- finite? [x] (and (number? x) (Double/isFinite (double x))))
(defn- absent [reason] {:status :absent :reason reason})
(defn- failure? [x] (= :absent (:status x)))
(defn- close? [x y] (<= (Math/abs (- (double x) (double y))) tolerance))

(defn- valid-belief? [q]
  (and (map? q)
       (= (set valences) (set (keys q)))
       (every? #(and (finite? %) (not (neg? (double %)))) (vals q))
       (close? 1.0 (reduce + (map double (vals q))))))

(defn- valid-transition? [level]
  (let [B (:B level)]
    (and (map? B)
         (every? (fn [j]
                   (let [column (map #(get B [% j]) valences)]
                     (and (every? finite? column)
                          (close? 1.0 (reduce + (map double column))))))
                 valences))))

(defn- valid-rates? [level]
  (and (map? (:rate level))
       (every? #(and (finite? %) (pos? (double %))) (vals (:rate level)))
       (= (set valences) (set (keys (:rate level))))))

(defn- validate-level-and-belief [level q]
  (cond
    (not (valid-rates? level)) :rate-invalid
    (not (valid-transition? level)) :transition-column-not-stochastic
    (not (valid-belief? q)) :valence-not-probability
    :else nil))

(defn- beta-reason [beta-prior beta-posterior]
  (cond
    (or (nil? beta-posterior) (= :absent (:status beta-posterior)))
    :beta-posterior-absent
    (not (and (finite? beta-prior) (pos? (double beta-prior))))
    :beta-prior-invalid
    (not (and (finite? beta-posterior) (pos? (double beta-posterior))))
    :beta-posterior-invalid
    :else nil))

(defn descend
  "Lean `descend`: the valence-weighted next-click beta prior."
  [level q]
  (if-let [reason (validate-level-and-belief level q)]
    (absent reason)
    (reduce + (map #(* (double (get-in level [:rate %])) (double (get q %))) valences))))

(defn charge
  "Lean `charge`, AC = beta-prior - beta-posterior."
  [beta-prior beta-posterior]
  (if-let [reason (beta-reason beta-prior beta-posterior)]
    (absent reason)
    (- (double beta-prior) (double beta-posterior))))

(defn affective-evidence
  "Lean `affectiveEvidence` for one valence state."
  [level beta-prior beta-posterior k]
  (let [reason (or (when-not (and (valid-rates? level) (some #{k} valences)) :rate-invalid)
                   (beta-reason beta-prior beta-posterior))]
    (if reason
      (absent reason)
      (let [ac (charge beta-prior beta-posterior)
            rate (double (get-in level [:rate k]))
            denominator (- rate ac)]
        (if (not (pos? denominator))
          (absent :affective-evidence-undefined)
          (* (/ rate denominator)
             (/ (- (double beta-prior) ac) (double beta-prior))))))))

(defn valence-update
  "Lean `valenceUpdate`: normalize q_k times its affective evidence."
  [level q beta-prior beta-posterior]
  (if-let [reason (or (validate-level-and-belief level q)
                      (beta-reason beta-prior beta-posterior))]
    (absent reason)
    (let [evidence (into {} (map (fn [k] [k (affective-evidence level beta-prior beta-posterior k)])
                                  valences))]
      (if-let [failure (first (filter failure? (vals evidence)))]
        failure
        (let [weighted (into {} (map (fn [k] [k (* (double (get q k)) (get evidence k))])
                                      valences))
              z (reduce + (vals weighted))]
          (if (not (pos? z))
            (absent :valence-normalization-undefined)
            (into {} (map (fn [k] [k (/ (get weighted k) z)]) valences))))))))

(defn predict
  "Lean `predict`: B k j times the updated valence q j."
  [level q]
  (if-let [reason (validate-level-and-belief level q)]
    (absent reason)
    (into {} (map (fn [k]
                    [k (reduce + (map #(* (double (get-in level [:B [k %]]))
                                           (double (get q %))) valences))])
                  valences))))

(defn step
  "One pure affective-level transition. Invalid data is typed absent and never
  changes valence; no runtime state is read or written."
  [level q beta-prior beta-posterior]
  (if-let [reason (or (validate-level-and-belief level q)
                      (beta-reason beta-prior beta-posterior))]
    (absent reason)
    (let [after (valence-update level q beta-prior beta-posterior)]
      (if (failure? after)
        after
        (let [next (predict level after)
              next-beta (descend level next)]
          (if (or (failure? next) (failure? next-beta))
            (or (when (failure? next) next) next-beta)
            {:valence-after-update after
             :valence-next next
             :beta-prior-next next-beta
             :charge (charge beta-prior beta-posterior)}))))))

(defn carried-valence-prior
  "Resolve this run's Q6 valence and provenance once, shared by selection and close."
  [predecessor]
  (let [cold? (and (= :absent (:status predecessor))
                   (= :no-earlier-started-run (:reason predecessor)))
        prior-carry (get-in predecessor [:previous :valence-carry])
        carried-q (:q-after prior-carry)
        after-reason (cond
                       cold? nil
                       (= :present (:status predecessor))
                       (if (failure? carried-q)
                         (or (:reason carried-q) :carried-q-after-unreadable)
                         (when-not carried-q :predecessor-has-no-valence-carry))
                       :else (or (:reason predecessor) :predecessor-status-unknown))
        redeclared? (some? after-reason)
        q-before (if (or cold? redeclared?) {:pos 0.5 :neg 0.5} carried-q)
        source (cond
                 cold? {:cold-start :declared :provenance :declared-cold-start-r20-packet}
                 redeclared? {:cold-start :redeclared :after after-reason}
                 :else {:previous-run/id (:previous-run/id predecessor)})
        beta-prior (if (failure? q-before) q-before (descend hesp-level q-before))]
    {:q q-before :source source :level :hesp-level :beta-prior beta-prior}))

(defn valence-carry
  "Construct the sealed-Q6 payload for the next ordinary run.  This records
  the current absence of beta-posterior; it does not alter gamma or selection."
  [predecessor precision-row]
  (let [{:keys [q source beta-prior]} (carried-valence-prior predecessor)
        q-before q
        beta-posterior (or (:beta-posterior precision-row)
                           (absent :no-precision-row))
        base {:schema :wm/r20-valence-carry-v1 :level :hesp-level :source source
              :q-before q-before :beta-prior beta-prior
              :beta-posterior beta-posterior :gamma-applied false}]
    (if (failure? q-before)
        (assoc base :update {:status :not-performed :reason (:reason q-before)}
               :q-after q-before)
        (let [s (step hesp-level q-before beta-prior beta-posterior)]
          (if (failure? s)
            (assoc base :update {:status :not-performed :reason (:reason s)} :q-after q-before)
            (assoc base :update {:status :performed :charge (:charge s)}
                   :q-after (:valence-next s)))))))
