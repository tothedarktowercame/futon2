#!/usr/bin/env clojure
;; U2 -- adjudicate the tau arms by RETRODICTION on the recorded fields.
;;
;; The question (SPEC-dormant-wiring.md U2): three arms of the eq. 2.7 beta
;; solve run and separate on S2 -- three different gammas, all converged and
;; bracketed on 20/20 ticks -- and nothing measured says which is better.
;; :choices :pi-zero-form :values says exactly that in the registry.
;;
;; THE SCORING RULE, stated before any number is read. Each arm produces, at
;; tick t, a policy posterior pi_t over the aligned candidate set. The record
;; at tick t+1 carries, per candidate, F_pi = the horizon-one Gaussian
;; observed-data free energy of THAT candidate's tick-t prediction against
;; tick-(t+1)'s actual observation (policy_free_energy.clj:41-120; lower is a
;; better fit, and exp(-F_pi) is the Gaussian likelihood of the observation).
;; So F at t+1 is out-of-sample for a posterior formed at t, and the natural
;; score is the log evidence of the observed next observation under the arm's
;; posterior used as mixture weights:
;;
;;     L_mix(a,t) = ln SUM_i pi_t^a(i) exp(-F_pi^{t+1}(i))      (higher better)
;;
;; reported beside two readings that do not depend on the mixture assumption:
;;
;;     E_F(a,t)   = SUM_i pi_t^a(i) F_pi^{t+1}(i)               (lower better)
;;     F@top(a,t) = F_pi^{t+1}(argmax_i pi_t^a(i))              (lower better)
;;
;; and bracketed by the two degenerate arms that bound every score on the
;; field: ORACLE (all mass on argmin F^{t+1}) and ANTI-ORACLE (all mass on
;; argmax F^{t+1}). An arm outside that bracket means the scoring is wrong,
;; not that the arm is good -- checked, not assumed.
;;
;; NO LIVE RUN. Everything here is a replay of committed records.
;;
;; Usage: clojure -M holes/labs/wm-contract/u2_retrodiction.clj
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[futon2.aif.habit-prior :as habit-prior]
         '[futon2.aif.policy-precision :as pp-solver]
         '[futon2.aif.policy-free-energy :as pfe])

(def beta-0 1.0)
(def beta-floor 1.0e-6)
(def beta-ceiling 1.0e6)
(def tolerance 1.0e-9)

(def fields
  [{:label "S2" :path "holes/labs/wm-contract/runs/2026-09-01-s2/wm-trace-s2.edn"}
   {:label "S3" :path :absent}
   {:label "S4" :path "holes/labs/wm-contract/runs/2026-09-01-s4/wm-trace-s4.edn"}])

(defn read-records [path]
  (with-open [r (java.io.PushbackReader. (io/reader path))]
    (loop [out []]
      (let [form (edn/read {:eof ::eof :default (fn [t v] {:trace/edn-tag t :trace/value v})} r)]
        (if (= ::eof form) out (recur (conj out form)))))))

(defn- softmax [xs]
  (let [m (apply max xs)
        es (mapv #(Math/exp (- % m)) xs)
        z (reduce + es)]
    (mapv #(/ % z) es)))

(defn- log-sum-exp [xs]
  (let [m (apply max xs)]
    (+ m (Math/log (reduce + (map #(Math/exp (- % m)) xs))))))

;; ---------------------------------------------------------------------------
;; The generalised eq. 2.7 residual (identical in form to run7_beta_arms.clj,
;; whose :none setting is checked against src/converge-beta below).

(defn residual
  [beta beta-prior g f-pi ln-e {:keys [e-in-pi? e-in-pi-0?]}]
  (let [gamma (/ 1.0 beta)
        pi (softmax (mapv (fn [gi fi ei]
                            (+ (if e-in-pi? ei 0.0) (- fi) (* (- gamma) gi)))
                          g f-pi ln-e))
        pi-0 (softmax (mapv (fn [gi ei]
                              (+ (if e-in-pi-0? ei 0.0) (* (- gamma) gi)))
                            g ln-e))
        policy-error (reduce + (map (fn [p q gi] (* (- p q) gi)) pi pi-0 g))]
    {:residual (+ (- beta-prior beta) policy-error) :pi pi :pi-0 pi-0
     :gamma gamma :policy-error policy-error}))

(defn solve
  "Bisect the same bracket src/futon2/aif/policy_precision.clj uses."
  [beta-prior g f-pi ln-e arm]
  (let [r #(:residual (residual % beta-prior g f-pi ln-e arm))
        lo beta-floor hi beta-ceiling
        rlo (r lo) rhi (r hi)]
    (if (pos? (* rlo rhi))
      {:bracketed? false :beta beta-prior :converged? false
       :pi (:pi (residual beta-prior beta-prior g f-pi ln-e arm))
       :gamma (/ 1.0 beta-prior)}
      (loop [lo lo hi hi rlo rlo n 0]
        (let [mid (/ (+ lo hi) 2.0)
              rm (r mid)]
          (cond
            (or (<= (Math/abs rm) tolerance) (> n 4096))
            (let [full (residual mid beta-prior g f-pi ln-e arm)]
              (assoc full :bracketed? true :beta mid :iterations n
                     :converged? (<= (Math/abs rm) tolerance)))
            (neg? (* rlo rm)) (recur lo mid rlo (inc n))
            :else (recur mid hi rm (inc n))))))))

(defn one-step
  "The eq. 2.7 update applied ONCE from beta-prior, without iterating to the
   fixed point: pi and pi_0 are built at gamma = 1/beta_prior and
   beta = beta_prior + (pi - pi_0).G. This is the arm the gamma-fixed-point
   entry's first question rules out ('usually one would iterate the equalities
   in equation 2.7 until convergence', friston2017.txt:683-685); it is run
   here so that 'the source settles it' has a number beside it."
  [beta-prior g f-pi ln-e arm]
  (let [at-prior (residual beta-prior beta-prior g f-pi ln-e arm)
        beta (+ beta-prior (:policy-error at-prior))
        beta (min beta-ceiling (max beta-floor beta))]
    (assoc (residual beta beta-prior g f-pi ln-e arm)
           :beta beta :converged? false :bracketed? true :one-step? true)))

;; ---------------------------------------------------------------------------
;; The field of one record: exactly the join the tick performed, plus the
;; candidate identity, which is what carries a candidate from tick t to t+1.

(defn- n-predicted-channels
  "How many channels this candidate's action model actually predicts. Summing
   |mean - observation| over only those channels is NOT comparable between
   candidates -- a candidate that predicts nothing scores 0 there while its
   F_pi is computed with every channel at the variance floor -- so the count is
   reported beside the change, and the change is taken over ALL channels."
  [cand]
  (count (filter #(= :present (:status (val %))) (:prediction-variance-status cand))))

(defn- predicted-change
  "Sum over ALL channels of |predicted mean - THIS tick's observation|: how far
   the candidate's prediction sits from the world as it already is."
  [cand observation]
  (reduce-kv (fn [tot ch m]
               (+ tot (Math/abs (- (double m) (double (get observation ch 0.0))))))
             0.0
             (:prediction-mean cand)))

(defn field
  [record]
  (let [envelope (:f-pi-by-candidate-id record)
        ranked (:ranked-actions record)
        observation (:observation record)
        identity-fn #(habit-prior/policy-key (:action %))
        by-identity (group-by identity-fn ranked)]
    (when (= :present (:status envelope))
      (reduce
       (fn [acc [_ entry]]
         (let [matches (get by-identity (:candidate-identity entry))
               cand (when (= 1 (count matches)) (first matches))
               g (:controller-score cand)
               v (:value entry)]
           (if (and (= :present (:status entry)) (number? v) (number? g))
             (-> acc
                 (update :g conj (double g))
                 (update :f conj (double v))
                 (update :ln-e conj (double (or (:habit-prior-bias cand) 0.0)))
                 (update :machine-rank conj (long (:rank cand)))
                 (update :delta conj (predicted-change cand observation))
                 (update :n-pred conj (n-predicted-channels cand))
                 (update :identity conj (:candidate-identity entry)))
             acc)))
       {:g [] :f [] :ln-e [] :machine-rank [] :identity [] :delta [] :n-pred []}
       (sort-by key (:by-candidate-id envelope))))))

(defn next-f-by-identity
  "candidate-identity -> F_pi at the NEXT tick, i.e. how that candidate's
   tick-t prediction scored against tick-(t+1)'s observation."
  [record]
  (let [envelope (:f-pi-by-candidate-id record)]
    (when (= :present (:status envelope))
      (into {} (keep (fn [[_ e]]
                       (when (and (= :present (:status e)) (number? (:value e)))
                         [(:candidate-identity e) (double (:value e))]))
                     (:by-candidate-id envelope))))))

(defn- argmax-index [xs] (first (apply max-key second (map-indexed vector xs))))

;; ---------------------------------------------------------------------------
;; Scoring one arm's posterior on one transition.

(defn score
  "pi over the tick-t field, next-f over candidate identities at t+1.
   Restricted to the candidates present on BOTH sides; pi is renormalised over
   that subset and the subset size is reported, so a shrinking join shows up
   as a number instead of silently changing the score."
  [pi ids next-f]
  (let [keep-idx (vec (keep-indexed (fn [i id] (when (contains? next-f id) i)) ids))
        w0 (mapv #(nth pi %) keep-idx)
        z (reduce + w0)
        w (mapv #(/ % z) w0)
        f (mapv #(get next-f (nth ids %)) keep-idx)]
    {:n (count keep-idx)
     :mass-kept z
     :l-mix (log-sum-exp (mapv (fn [wi fi] (+ (Math/log (max wi 1.0e-300)) (- fi))) w f))
     :e-f (reduce + (map * w f))
     :f-at-top (nth f (argmax-index w))
     :top-weight (apply max w)
     :top-identity (nth (mapv #(nth ids %) keep-idx) (argmax-index w))
     :entropy (- (reduce + (map (fn [wi] (if (pos? wi) (* wi (Math/log wi)) 0.0)) w)))}))

;; ---------------------------------------------------------------------------

(def ln-e-arms
  [[:none            {:e-in-pi? false :e-in-pi-0? false}]
   [:pi-only         {:e-in-pi? true  :e-in-pi-0? false}]
   [:both            {:e-in-pi? true  :e-in-pi-0? true}]])

(defn trajectory
  "Solve the whole field under one (ln E arm, prior rule, solver) combination.
   Returns one entry per usable tick with the arm's posterior."
  ([usable arm prior-rule solver] (trajectory usable arm prior-rule solver beta-0))
  ([usable arm prior-rule solver b0]
  (reduce (fn [acc [i f]]
            (let [prior (case prior-rule
                          :carried (:beta (peek acc) b0)
                          :fixed   b0)
                  s (solver prior (:g f) (:f f) (:ln-e f) arm)]
              (conj acc {:tick i :beta (:beta s) :gamma (:gamma s)
                         :converged? (:converged? s) :bracketed? (:bracketed? s)
                         :pi (:pi s)})))
          [] usable)))

(defn mean [xs] (/ (reduce + xs) (double (count xs))))

(defn- ranks-of [xs]
  (let [order (map first (sort-by second (map-indexed vector xs)))]
    (reduce (fn [m [r i]] (assoc m i (inc r))) {} (map-indexed vector order))))

(defn spearman
  "Rank correlation, ties broken by position (adequate here: reported only to
   two decimals and used to answer a yes/no about direction)."
  [xs ys]
  (let [rx (ranks-of xs) ry (ranks-of ys)
        n (count xs)
        ax (mean (vals rx)) ay (mean (vals ry))
        num (reduce + (map (fn [i] (* (- (rx i) ax) (- (ry i) ay))) (range n)))
        dx (Math/sqrt (reduce + (map (fn [i] (let [d (- (rx i) ax)] (* d d))) (range n))))
        dy (Math/sqrt (reduce + (map (fn [i] (let [d (- (ry i) ay)] (* d d))) (range n))))]
    (if (or (zero? dx) (zero? dy)) Double/NaN (/ num (* dx dy)))))

(defn -main [& _]
  (println ";; U2 -- tau arm adjudication by retrodiction. Replay only; no live run.")
  ;; No timestamp in the output: the artifact must be byte-reproducible from
  ;; the committed records, so that "re-run it and diff" is a check anyone can
  ;; make (the standard run7_beta_arms.clj / ARMS.txt set).
  (println)

  (doseq [{:keys [label path]} fields]
    (println (str "\n;; ==================== FIELD " label " ===================="))
    (if (= :absent path)
      (println (str ";; NOT FOUND: stage S3 recorded no trace of its own. "
                    "runs/2026-09-01-s3/ holds ARMS.txt and README.md only "
                    "(README.md:1-6: \"This directory holds a REPLAY, not a 20-tick stage run\"); "
                    "RUN8's two live ticks are pre-flight ticks in the shared per-date trace and "
                    "are not a consecutive pair, so no S3 transition can be scored. "
                    "S3 contributes 0 transitions to every table below."))
      (let [records (read-records path)
            flds (mapv field records)
            usable (vec (keep-indexed (fn [i f] (when (seq (:g f)) [i f])) flds))]
        (println (format ";; records %d, usable aligned fields %d" (count records) (count usable)))

        ;; ---- CONTROL 1: the generalised solver's :none arm reproduces src.
        (let [[_ f] (first usable)
              mine (solve beta-0 (:g f) (:f f) (:ln-e f) {:e-in-pi? false :e-in-pi-0? false})
              theirs (pp-solver/converge-beta beta-0 (:g f) (:f f))]
          (println (format ";; CONTROL 1 solver: local %.12f  src converge-beta %.12f  delta %.2e"
                           (:beta mine) (:beta-posterior theirs)
                           (Math/abs (- (:beta mine) (:beta-posterior theirs))))))

        ;; ---- CONTROL 2: every transition is a genuine consecutive pair.
        (let [bad (for [k (range (dec (count records)))
                        :let [t (:timestamp (nth records k))
                              p (get-in (nth records (inc k)) [:f-pi-provenance :previous-trace-timestamp])]
                        :when (not= t p)]
                    [(inc k) t p])]
          (println (format ";; CONTROL 2 alignment: %d of %d transitions have F_pi(t+1) scored against tick t's own prediction%s"
                           (- (dec (count records)) (count bad)) (dec (count records))
                           (if (seq bad) (str " -- MISALIGNED: " (pr-str bad)) "")))
          (when (seq bad) (throw (ex-info "misaligned transition" {:bad bad}))))

        ;; ---- the transitions, and the two degenerate arms that bracket them.
        (let [transitions
              (vec (for [k (range (dec (count usable)))
                         :let [[i f] (nth usable k)
                               [j _] (nth usable (inc k))]
                         :when (= (inc i) j)]
                     {:k k :from i :to j :field f
                      :next-f (next-f-by-identity (nth records j))
                      ;; the SAME candidates scored against the observation the
                      ;; machine had ALREADY SEEN at t. Everything an arm can
                      ;; know before the outcome arrives is in here.
                      :cf-f (into {} (keep (fn [c]
                                             (try
                                               [(habit-prior/policy-key (:action c))
                                                (pfe/f-pi-for-candidate
                                                 {:prediction-mean (:prediction-mean c)
                                                  :prediction-variance (:prediction-variance c)
                                                  :variance-status (:prediction-variance-status c)}
                                                 (:observation (nth records i))
                                                 {:absent-variance :floor})]
                                               (catch clojure.lang.ExceptionInfo _ nil)))
                                           (:ranked-actions (nth records i))))}))
              _ (println (format ";; scorable transitions %d" (count transitions)))

              bracket
              (mapv (fn [{:keys [field next-f]}]
                      (let [ids (:identity field)
                            common (filterv #(contains? next-f %) ids)
                            fs (mapv next-f common)]
                        {:n (count common) :min (apply min fs) :max (apply max fs)
                         :mean (mean fs)}))
                    transitions)

              ;; -------- the arms --------
              arms
              (concat
               (for [[lbl arm] ln-e-arms
                     prior [:carried :fixed]]
                 {:name (str (name lbl) "/" (name prior))
                  :traj (trajectory usable arm prior solve)})
               (for [[lbl arm] ln-e-arms]
                 {:name (str (name lbl) "/carried/one-step")
                  :traj (trajectory usable arm :carried one-step)}))

              ;; -------- the controls --------
              control-default
              {:name "CONTROL default-law"
               :traj (mapv (fn [[i f]]
                             (let [tau (double (or (get-in (nth records i) [:decision :tau]) 1.0))]
                               {:tick i :beta tau :gamma (/ 1.0 tau)
                                :pi (softmax (mapv #(/ (- %) tau) (:g f)))}))
                           usable)}
              control-uniform
              {:name "CONTROL uniform"
               :traj (mapv (fn [[i f]]
                             {:tick i :beta Double/NaN :gamma Double/NaN
                              :pi (vec (repeat (count (:g f)) (/ 1.0 (count (:g f)))))})
                           usable)}
              control-machine
              {:name "CONTROL machine-choice"
               :traj (mapv (fn [[i f]]
                             (let [chosen (habit-prior/policy-key
                                           (get-in (nth records i) [:decision :action]))
                                   idx (.indexOf ^java.util.List (:identity f) chosen)]
                               {:tick i :beta Double/NaN :gamma Double/NaN
                                :chosen chosen :chosen-index idx
                                :pi (if (neg? idx)
                                      (vec (repeat (count (:g f)) (/ 1.0 (count (:g f)))))
                                      (assoc (vec (repeat (count (:g f)) 0.0)) idx 1.0))}))
                           usable)}
              oracle
              {:name "ORACLE argmin F(t+1)"
               :traj nil}
              anti
              {:name "ANTI-ORACLE argmax F(t+1)"
               :traj nil}

              score-arm
              (fn score-arm
                ([a] (score-arm a :next-f))
                ([{:keys [name traj]} f-key]
                (let [rows (mapv (fn [{:keys [from field] :as tr}]
                                   (let [t (first (filter #(= from (:tick %)) traj))]
                                     (assoc (score (:pi t) (:identity field) (get tr f-key))
                                            :tick (inc from)
                                            :beta (:beta t) :gamma (:gamma t)
                                            :converged? (:converged? t))))
                                 transitions)]
                  {:name name :rows rows})))

              scored (mapv score-arm (concat arms [control-default control-uniform control-machine]))

              degenerate (fn [nm pick]
                           {:name nm
                            :rows (mapv (fn [{:keys [field next-f]}]
                                          (let [ids (filterv #(contains? next-f %) (:identity field))
                                                fs (mapv next-f ids)
                                                v (pick fs)]
                                            {:tick nil :n (count ids) :l-mix (- v) :e-f v :f-at-top v
                                             :top-weight 1.0 :entropy 0.0}))
                                        transitions)})
              scored (concat [(degenerate (:name oracle) #(apply min %))]
                             scored
                             [(degenerate (:name anti) #(apply max %))])]

          ;; ---- per-tick table, primary score ----
          (println "\n;; --- L_mix = ln SUM_i pi_i exp(-F_pi(t+1)_i), per transition (HIGHER IS BETTER)")
          (println (format "%-30s %s" "arm \\ transition t->t+1"
                           (str/join " " (map #(format "%8s" (str (inc (:from %)) "->" (inc (:to %))))
                                              transitions))))
          (doseq [{:keys [name rows]} scored]
            (println (format "%-30s %s" name
                             (str/join " " (map #(format "%8.4f" (:l-mix %)) rows)))))

          (println "\n;; --- E_F = SUM_i pi_i F_pi(t+1)_i, per transition (LOWER IS BETTER)")
          (doseq [{:keys [name rows]} scored]
            (println (format "%-30s %s" name
                             (str/join " " (map #(format "%8.4f" (:e-f %)) rows)))))

          (println "\n;; --- F_pi(t+1) at the arm's argmax (LOWER IS BETTER)")
          (doseq [{:keys [name rows]} scored]
            (println (format "%-30s %s" name
                             (str/join " " (map #(format "%8.4f" (:f-at-top %)) rows)))))

          ;; ---- summary ----
          (println (str "\n;; --- SUMMARY over " (count transitions) " transitions"))
          (println (format "%-30s %12s %12s %12s %10s %10s"
                           "arm" "mean L_mix" "mean E_F" "mean F@top" "mean maxw" "mean H"))
          (doseq [{:keys [name rows]} scored]
            (println (format "%-30s %12.6f %12.6f %12.6f %10.6f %10.4f"
                             name
                             (mean (map :l-mix rows)) (mean (map :e-f rows))
                             (mean (map :f-at-top rows))
                             (mean (map :top-weight rows)) (mean (map :entropy rows)))))

          ;; ---- THE COUNTERFACTUAL SCORING ------------------------------
          ;; Re-score every arm against the observation the machine had ALREADY
          ;; SEEN at t. Whatever survives this difference is the only part of
          ;; the retrodiction that measures the outcome.
          (println "\n;; --- COUNTERFACTUAL: the same arms scored against observation(t), not observation(t+1)")
          (println (format "%-30s %14s %14s %14s"
                           "arm" "mean L_mix" "counterfactual" "difference"))
          (let [real (mapv score-arm (concat arms [control-default control-uniform control-machine]))
                cf (mapv #(score-arm % :cf-f) (concat arms [control-default control-uniform control-machine]))]
            (doseq [[a b] (map vector real cf)]
              (println (format "%-30s %14.6f %14.6f %+14.3e"
                               (:name a) (mean (map :l-mix (:rows a)))
                               (mean (map :l-mix (:rows b)))
                               (- (mean (map :l-mix (:rows a))) (mean (map :l-mix (:rows b)))))))
            (println (format ";; largest outcome-dependent movement of any arm's mean score: %.3e"
                             (apply max (map (fn [[a b]] (Math/abs (- (mean (map :l-mix (:rows a)))
                                                                      (mean (map :l-mix (:rows b))))))
                                             (map vector real cf)))))
            (println (format ";; for comparison, the largest gap BETWEEN arms on the realised score: %.3e"
                             (let [ms (map #(mean (map :l-mix (:rows %))) real)]
                               (- (apply max ms) (apply min ms)))))
            ;; The comparison that decides whether the outcome adjudicates
            ;; anything: does swapping the outcome change any arm-against-arm
            ;; gap? A movement common to every arm changes no comparison.
            (let [ds (mapv (fn [[a b]] (- (mean (map :l-mix (:rows a)))
                                          (mean (map :l-mix (:rows b)))))
                           (map vector real cf))
                  gaps (fn [xs] (for [i (range (count xs)) j (range (count xs)) :when (< i j)]
                                  (- (nth xs i) (nth xs j))))
                  gr (gaps (mapv #(mean (map :l-mix (:rows %))) real))
                  gc (gaps (mapv #(mean (map :l-mix (:rows %))) cf))]
              (println (format ";; spread of that movement ACROSS arms: %.3e (a common shift changes no comparison)"
                               (- (apply max ds) (apply min ds))))
              (println (format ";; largest change in ANY arm-against-arm gap when the outcome is swapped: %.3e"
                               (apply max (map #(Math/abs (- %1 %2)) gr gc))))))

          ;; ---- pairwise separation among the three ln E arms, carried ----
          (println "\n;; --- SEPARATION among the three ln E arms (carried prior)")
          (let [by-name (into {} (map (juxt :name identity) scored))
                a (get by-name "none/carried") b (get by-name "pi-only/carried")
                c (get by-name "both/carried")]
            (doseq [[x y] [[a b] [a c] [b c]]]
              (let [d (mapv (fn [p q] (- (:l-mix p) (:l-mix q))) (:rows x) (:rows y))
                    dt (mapv (fn [p q] (if (= (:top-identity p) (:top-identity q)) 0 1)) (:rows x) (:rows y))]
                (println (format "%-22s vs %-22s  max |dL_mix| %.3e   ticks with a different argmax: %d of %d"
                                 (:name x) (:name y) (apply max (map #(Math/abs %) d))
                                 (reduce + dt) (count dt))))))

          ;; ---- carried vs fixed, per ln E arm ----
          (println "\n;; --- CARRIED vs FIXED prior (the gamma-fixed-point across-tick question)")
          (let [by-name (into {} (map (juxt :name identity) scored))]
            (doseq [[lbl _] ln-e-arms]
              (let [x (get by-name (str (name lbl) "/carried"))
                    y (get by-name (str (name lbl) "/fixed"))
                    d (mapv (fn [p q] (- (:l-mix p) (:l-mix q))) (:rows x) (:rows y))
                    dt (mapv (fn [p q] (if (= (:top-identity p) (:top-identity q)) 0 1)) (:rows x) (:rows y))]
                (println (format "%-10s carried - fixed:  mean dL_mix %+.3e  max |dL_mix| %.3e  ticks with a different argmax: %d of %d"
                                 (name lbl) (mean d) (apply max (map #(Math/abs %) d))
                                 (reduce + dt) (count dt))))))

          ;; ---- iterate vs one-step ----
          (println "\n;; --- ITERATED fixed point vs ONE-STEP update (the within-tick question)")
          (let [by-name (into {} (map (juxt :name identity) scored))]
            (doseq [[lbl _] ln-e-arms]
              (let [x (get by-name (str (name lbl) "/carried"))
                    y (get by-name (str (name lbl) "/carried/one-step"))
                    d (mapv (fn [p q] (- (:l-mix p) (:l-mix q))) (:rows x) (:rows y))
                    dt (mapv (fn [p q] (if (= (:top-identity p) (:top-identity q)) 0 1)) (:rows x) (:rows y))]
                (println (format "%-10s iterated - one-step:  mean dL_mix %+.3e  max |dL_mix| %.3e  ticks with a different argmax: %d of %d"
                                 (name lbl) (mean d) (apply max (map #(Math/abs %) d))
                                 (reduce + dt) (count dt))))))

          ;; ---- CONTROL 3: no arm may beat the oracle or lose to the anti-oracle
          (let [by-name (into {} (map (juxt :name identity) scored))
                o (:rows (get by-name "ORACLE argmin F(t+1)"))
                w (:rows (get by-name "ANTI-ORACLE argmax F(t+1)"))
                viol (for [{:keys [name rows]} scored
                           :when (not (str/starts-with? name "ORACLE"))
                           :when (not (str/starts-with? name "ANTI-ORACLE"))
                           [r oo ww] (map vector rows o w)
                           :when (or (> (:l-mix r) (+ (:l-mix oo) 1.0e-9))
                                     (< (:l-mix r) (- (:l-mix ww) 1.0e-9)))]
                       [name (:tick r) (:l-mix r) (:l-mix oo) (:l-mix ww)])]
            (println (format "\n;; CONTROL 3 bracket: %d arm-tick scores outside [anti-oracle, oracle]%s"
                             (count viol) (if (seq viol) (str " -- " (pr-str (take 5 viol))) " (none)"))))

          ;; ---- what the arms' argmax actually is, in machine-rank terms ----
          (println "\n;; --- what each arm puts on top, as the machine's own rank at tick t")
          (let [by-name (into {} (map (juxt :name identity) scored))
                at (fn [rows k]
                     (mapv (fn [{:keys [field]} r]
                             (let [idx (.indexOf ^java.util.List (:identity field) (:top-identity r))]
                               (if (neg? idx) nil (nth (get field k) idx))))
                           transitions rows))]
            (doseq [nm ["none/carried" "pi-only/carried" "both/carried"
                        "CONTROL default-law" "CONTROL machine-choice"]]
              (let [rows (:rows (get by-name nm))]
                (println (format "%-30s machine-rank of argmax per tick: %s"
                                 nm (str/join " " (map str (at rows :machine-rank)))))
                (println (format "%-30s |prediction - observation(t)| there: %s"
                                 "" (str/join " " (map #(format "%.4f" (double %)) (at rows :delta)))))
                (println (format "%-30s channels that candidate predicts:    %s"
                                 "" (str/join " " (map str (at rows :n-pred))))))))

          ;; ---- CARRIED vs FIXED over a beta_0 sweep -------------------
          ;; C468 found the J4 arms' rank movement concentrated at beta_0 = 0.5;
          ;; a carried-vs-fixed result at beta_0 = 1.0 alone would be a result
          ;; about one initial condition.
          (println "\n;; --- CARRIED vs FIXED over a beta_0 sweep")
          (doseq [b0 [0.5 1.0 2.0 5.0]]
            (doseq [[lbl arm] ln-e-arms]
              (let [x (score-arm {:name "c" :traj (trajectory usable arm :carried solve b0)})
                    y (score-arm {:name "f" :traj (trajectory usable arm :fixed solve b0)})
                    d (mapv (fn [p q] (- (:l-mix p) (:l-mix q))) (:rows x) (:rows y))
                    dt (mapv (fn [p q] (if (= (:top-identity p) (:top-identity q)) 0 1)) (:rows x) (:rows y))]
                (println (format "beta_0 %-4s %-10s carried - fixed:  mean dL_mix %+.3e  max |dL_mix| %.3e  ticks with a different argmax: %d of %d"
                                 b0 (name lbl) (mean d) (apply max (map #(Math/abs %) d))
                                 (reduce + dt) (count dt))))))

          ;; ---- DIAGNOSTIC: what is the retrodiction score actually made of?
          ;; F_pi = SUM_ch 1/2 ln(2 pi v_ch)  +  SUM_ch 1/2 (o_ch - m_ch)^2 / v_ch
          ;;        \______ A: declared variance ______/   \____ B: residual ____/
          ;; A depends only on the candidate's own declared variances; B is the
          ;; only part that can depend on what was observed. If A carries the
          ;; between-candidate variation, then ranking arms by retrodiction
          ;; ranks them by whose favourite candidate declares the tightest
          ;; variances -- which is not retrodiction.
          (println "\n;; --- DIAGNOSTIC: F_pi decomposed into A (log-variance) and B (residual)")
          (doseq [{:keys [from to]} transitions]
            (let [cands (:ranked-actions (nth records from))
                  o-next (:observation (nth records to))
                  o-now (:observation (nth records from))
                  parts (fn [c obs]
                          (reduce-kv
                           (fn [acc ch m]
                             (let [raw (double (get-in c [:prediction-variance ch] 0.0))
                                   absent? (= :absent (get-in c [:prediction-variance-status ch :status]))
                                   v (if (and (zero? raw) absent?) 0.01 raw)
                                   r (- (double (get obs ch 0.0)) (double m))]
                               (-> acc
                                   (update :a + (* 0.5 (Math/log (* 2.0 Math/PI v))))
                                   (update :b + (* 0.5 (/ (* r r) v))))))
                           {:a 0.0 :b 0.0}
                           (:prediction-mean c)))
                  ps (mapv #(parts % o-next) cands)
                  ps-cf (mapv #(parts % o-now) cands)
                  as (mapv :a ps) bs (mapv :b ps) bs-cf (mapv :b ps-cf)
                  spread #(- (apply max %) (apply min %))]
              (println (format "t %2d->%2d  A spread %.6f  B spread %.6f  B mean %.6f | counterfactual (score against observation at t instead): B spread %.6f  max |B - B_cf| %.3e"
                               (inc from) (inc to) (spread as) (spread bs) (mean bs)
                               (spread bs-cf)
                               (apply max (map #(Math/abs (- %1 %2)) bs bs-cf))))))

          ;; ---- CONTROL 4: the decomposition reproduces the RECORDED F_pi ----
          ;; Without this the decomposition above would be a second, unchecked
          ;; implementation of the score rather than a reading of the one the
          ;; run used.
          (println "\n;; --- CONTROL 4: recomputed F_pi(t+1) vs the value the run recorded")
          (doseq [{:keys [from to next-f]} transitions]
            (let [cands (:ranked-actions (nth records from))
                  o-next (:observation (nth records to))
                  ds (keep (fn [c]
                             (let [id (habit-prior/policy-key (:action c))]
                               (when-let [rec (get next-f id)]
                                 (Math/abs (- rec (pfe/f-pi-for-candidate
                                                   {:prediction-mean (:prediction-mean c)
                                                    :prediction-variance (:prediction-variance c)
                                                    :variance-status (:prediction-variance-status c)}
                                                   o-next
                                                   {:absent-variance :floor}))))))
                           cands)]
              (println (format "t %2d->%2d  %3d candidates re-scored, max |recomputed - recorded| = %.3e"
                               (inc from) (inc to) (count ds) (apply max 0.0 ds)))))

          ;; ---- how far the world actually moved on the predicted channels ---
          (println "\n;; --- observed change on the channels the action model predicts")
          (doseq [{:keys [from to]} transitions]
            (let [chs (into (sorted-set)
                            (for [c (:ranked-actions (nth records from))
                                  [ch st] (:prediction-variance-status c)
                                  :when (= :present (:status st))] ch))
                  o1 (:observation (nth records from))
                  o2 (:observation (nth records to))]
              (println (format "t %2d->%2d  channels %s  |delta| %s"
                               (inc from) (inc to) (pr-str (vec chs))
                               (pr-str (mapv #(format "%.6f" (Math/abs (- (double (get o2 % 0.0))
                                                                          (double (get o1 % 0.0)))))
                                             chs))))))

          ;; ---- which channels move, and which the action model predicts ----
          (println "\n;; --- OBSERVATION CHANNELS: what moves over the field, and what is predicted")
          (let [obs (mapv #(:observation (nth records (first %))) usable)
                predicted (into (sorted-set)
                                (for [[i _] usable
                                      c (:ranked-actions (nth records i))
                                      [ch st] (:prediction-variance-status c)
                                      :when (= :present (:status st))] ch))]
            (println (format ";; channels ANY candidate predicts: %s" (pr-str (vec predicted))))
            (doseq [ch (sort (keys (first obs)))]
              (let [vs (mapv #(double (get % ch 0.0)) obs)]
                (println (format "  %-20s predicted? %-3s  distinct %2d  range %.9f"
                                 (name ch) (if (contains? predicted ch) "YES" "no")
                                 (count (distinct vs))
                                 (- (apply max vs) (apply min vs)))))))

          ;; ---- how much of the F(t+1) range is in play at all ----
          (println "\n;; --- the range the score can move in, per transition")
          (doseq [[t b] (map vector transitions bracket)]
            (println (format "t %2d->%2d  n %3d  F(t+1) min %.6f  max %.6f  spread %.6f  mean %.6f"
                             (inc (:from t)) (inc (:to t)) (:n b) (:min b) (:max b)
                             (- (:max b) (:min b)) (:mean b))))))))
  (println "\n;; end."))

(apply -main *command-line-args*)
