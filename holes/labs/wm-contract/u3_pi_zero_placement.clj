#!/usr/bin/env clojure
;; U3 -- pi_0 placement: does WHERE ln E goes in the eq. 2.7 beta solve change
;; what the machine SELECTS?
;;
;;   clojure -M:test holes/labs/wm-contract/u3_pi_zero_placement.clj [out.txt]
;;
;; THE QUESTION, and why it is not the one U2 answered. `:choices
;; :pi-zero-form` has three ln E placements in the beta solve -- :none (what
;; src computes by default), :pi-only (friston2017.txt:684 as printed) and
;; :both (spm_MDP_VB_X.m:963-964, the authors' own code). RUN7 measured the
;; three GAMMAS on S2 and the entry's :values says "separated is not
;; adjudicated". C486 (U2) scored the beta solve's OWN posterior pi by
;; retrodiction. Neither pushed the arms through the seam where they could
;; reach a decision. The entry's :measurement :what-would-adjudicate names
;; that seam: "a difference in what the machine SELECTS under each arm".
;;
;; THE CHAIN THIS SCRIPT RUNS, end to end, using src functions and not
;; re-implementations at every step it can:
;;
;;   arm a  ->  beta_a(t)            eq. 2.7 root, carried tick to tick
;;          ->  tau_a(t) = beta_a(t) policy/effective-temperature under
;;                                   :tau-mode :variational-beta-gamma
;;                                   (policy.clj:135-137: tau = beta exactly)
;;          ->  P_a(t) = softmax(ln E - G/tau_a - F_pi)
;;                                   policy/softmax-weights (policy.clj:148)
;;          ->  argmax P_a(t)        what the machine would select
;;
;; and it asks whether the argmax differs between arms, on every recorded
;; field that carries the inputs, under each of the four selection laws the
;; flag chain can produce.
;;
;; THE ARMS REACH SELECTION ONLY THROUGH tau, so the answer has an arithmetic
;; floor this script states before it reports anything: under the ln-E-free,
;; F_pi-free law the score is -G/tau and argmax(-G/tau) = argmin G for EVERY
;; tau > 0. The pi_0 placement therefore cannot move that law's choice at any
;; beta whatsoever. It is a fact about the expression, not about the field,
;; and CONTROL 4 checks it numerically rather than asserting it.
;;
;; TYPING THE ZERO, if it is a zero. The score is linear in u = 1/tau:
;; s_i(u) = (ln E_i - F_i) - G_i * u. The argmax as a function of u is the
;; upper envelope of straight lines, so the exact interval of tau over which a
;; tick's choice is constant is computable in closed form (the nearest line
;; crossing above and below u = 1/tau). This script reports, per tick, the
;; multiplicative margin lambda_break -- the factor by which tau would have to
;; move to change the choice -- beside rho, the factor by which the arms
;; actually differ. "0 argmax differences" then carries the number that says
;; how far from a difference the field is.
;;
;; REPLAY ONLY. No live run, no run lock, nothing written under data/.

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[futon2.aif.habit-prior :as habit-prior]
         '[futon2.aif.policy :as policy]
         '[futon2.aif.policy-precision :as pp-solver])

(def out-path (or (first *command-line-args*)
                  "holes/labs/wm-contract/U3-PI-ZERO-PLACEMENT.txt"))

(def fields
  [["S2" "holes/labs/wm-contract/runs/2026-09-01-s2/wm-trace-s2.edn"]
   ["S4" "holes/labs/wm-contract/runs/2026-09-01-s4/wm-trace-s4.edn"]])

(def beta-zeros [0.5 1.0 2.0 5.0])
(def beta-floor 1.0e-6)
(def beta-ceiling 1.0e6)
(def tolerance 1.0e-9)

;; The three arms of :choices :pi-zero-form, in the registry's own order.
(def arms
  [[:none     {:e-in-pi? false :e-in-pi-0? false}]
   [:pi-only  {:e-in-pi? true  :e-in-pi-0? false}]
   [:both     {:e-in-pi? true  :e-in-pi-0? true}]])

;; The four score expressions, with what each is in the code beside it. The
;; :where line is read off the call sites, not inferred:
;;
;;   :G      policy.clj:678 -- the actuation boundary with every ln E zero.
;;           Posterior only; the CHOICE there is (first ranked-actions), so tau
;;           reaches no decision on this path.
;;   :E+G    policy.clj:693-696 -- THE ONE SITE WHERE tau DECIDES: chosen-idx =
;;           argmax(ln E - G/tau). Also the strategic boundary's counterfactual
;;           ordering (policy.clj:494-496) and its recorded posterior.
;;   :G+F    NOT REACHABLE. F_pi enters the posterior at the strategic boundary
;;           and nowhere else (policy.clj:634-638 throws otherwise), and that
;;           site always passes the full log-priors vector (policy.clj:553-556;
;;           built at policy.clj:656-657, zeros rather than nil). So F_pi
;;           without ln E requires an ln E that is identically zero, in which
;;           case this law IS :E+G+F. Kept as an arm because it is the one
;;           expression where the placement separates -- which is what shows
;;           the detector is not dead.
;;   :E+G+F  policy.clj:553-556 -- the strategic boundary's recorded posterior
;;           under f-pi-opts (what S4's applied ticks wrote), and U1's flip
;;           target. Recorded; never read for the choice.
(def laws
  [[:G     {:ln-e? false :f-pi? false :where "policy.clj:678 (posterior only; choice is tau-free)"}]
   [:E+G   {:ln-e? true  :f-pi? false :where "policy.clj:693-696 (the one site where tau DECIDES)"}]
   [:G+F   {:ln-e? false :f-pi? true  :where "NOT REACHABLE: F_pi and ln E arrive at one call site together"}]
   [:E+G+F {:ln-e? true  :f-pi? true  :where "policy.clj:553-556 (recorded posterior; U1's flip target)"}]])

(def lines (atom []))
(defn emit [& parts] (swap! lines conj (str/join "" parts)) nil)
(defn fmt [^String f & args] (apply format f args))

;; ---------------------------------------------------------------------------
;; Records and the aligned field

(defn read-records [path]
  (with-open [r (java.io.PushbackReader. (io/reader path))]
    (loop [out []]
      (let [form (edn/read {:eof ::eof
                            :default (fn [t v] {:trace/edn-tag t :trace/value v})} r)]
        (if (= ::eof form) out (recur (conj out form)))))))

(defn field
  "The aligned candidate field of one record: exactly the join the tick's beta
   solve performed -- the F_pi readback envelope joined to :ranked-actions BY
   :candidate-identity, not by the envelope's rank key. (The rank keys are the
   PRODUCING tick's ranks; joining on them transposes F_pi between candidates
   whose ordering moved -- SPEC-dormant-wiring.md U1, the negative control in
   selection_score_readiness_test.clj.)"
  [record]
  (let [envelope (:f-pi-by-candidate-id record)
        ranked (:ranked-actions record)
        by-identity (group-by #(habit-prior/policy-key (:action %)) ranked)]
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
                 (update :target conj (get-in cand [:action :target])))
             (update acc :dropped inc))))
       {:g [] :f [] :ln-e [] :machine-rank [] :target [] :dropped 0}
       (sort-by key (:by-candidate-id envelope))))))

;; ---------------------------------------------------------------------------
;; The generalised eq. 2.7 residual. src's converge-beta does the :none arm
;; only from a :log-prior-placement of :none; this puts ln E in pi, in pi_0, in
;; both or in neither, and CONTROL 1 checks its :none setting against src
;; before any arm is read.

(defn- softmax [xs]
  (let [m (apply max xs)
        es (mapv #(Math/exp (- % m)) xs)
        z (reduce + es)]
    (mapv #(/ % z) es)))

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
        rlo (r beta-floor) rhi (r beta-ceiling)]
    (if (pos? (* rlo rhi))
      {:bracketed? false :converged? false :beta beta-prior
       :residual-at-floor rlo :residual-at-ceiling rhi}
      (loop [lo beta-floor hi beta-ceiling rlo rlo n 0]
        (let [mid (/ (+ lo hi) 2.0)
              rm (r mid)]
          (cond
            (or (<= (Math/abs rm) tolerance) (> n 4096))
            (assoc (residual mid beta-prior g f-pi ln-e arm)
                   :bracketed? true :beta mid :iterations n
                   :converged? (<= (Math/abs rm) tolerance))
            (neg? (* rlo rm)) (recur lo mid rlo (inc n))
            :else (recur mid hi rm (inc n))))))))

(defn carried-betas
  "One arm's beta series over a field, carried tick to tick from beta-0."
  [flds beta-0 arm]
  (reduce (fn [acc f]
            (let [prior (:beta (peek acc) beta-0)]
              (conj acc (solve prior (:g f) (:f f) (:ln-e f) arm))))
          [] flds))

;; ---------------------------------------------------------------------------
;; Selection: src's own softmax-weights at src's own effective-temperature.

(defn tau-of-beta
  "tau_eff handed to selection under :tau-mode :variational-beta-gamma. Goes
   through policy/effective-temperature so the wiring measured is the shipped
   one, not tau := beta asserted here."
  [g-totals beta]
  (policy/effective-temperature g-totals 1.0
                                {:tau-mode :variational-beta-gamma
                                 :variational-beta beta
                                 :variational-beta-source :converged-posterior}))

(defn selection-weights
  [f tau {:keys [ln-e? f-pi?]}]
  (policy/softmax-weights (:g f) tau
                          (when ln-e? (:ln-e f))
                          (if f-pi?
                            {:f-pi-policy-posterior? true
                             :f-pi-values (:f f)
                             :f-pi-scaling :unscaled}
                            {})))

(defn argmax-index [xs]
  (first (reduce (fn [[bi bv] [i v]] (if (> v bv) [i v] [bi bv]))
                 [0 (first xs)] (map-indexed vector xs))))

(defn- rank-of [pi]
  (let [order (map first (sort-by (comp - second) (map-indexed vector pi)))]
    (into {} (map-indexed (fn [r i] [i (inc r)]) order))))

(defn ordering-moves
  "How many candidates the two orderings place differently, and the largest move."
  [a b]
  (let [ra (rank-of a) rb (rank-of b)
        deltas (map (fn [i] (Math/abs (long (- (ra i) (rb i))))) (range (count a)))]
    {:moved (count (filter pos? deltas)) :max-move (apply max 0 deltas)}))

(defn tv-distance [a b]
  (* 0.5 (reduce + (map (fn [x y] (Math/abs (- (double x) (double y)))) a b))))

;; ---------------------------------------------------------------------------
;; The margin, in closed form.
;;
;; s_i(u) = a_i - b_i * u   with u = 1/tau, a_i = [ln E_i] - [F_i], b_i = G_i.
;; The winner at u is the top line of the upper envelope; it changes only where
;; two lines cross. So the exact interval of tau over which THIS tick's choice
;; is this candidate is bounded by the nearest crossing of the winner with any
;; other line, above and below u.

(defn tau-window
  "[tau-lo tau-hi] over which the argmax at TAU is unchanged, and the
   multiplicative margin to the nearer edge. Infinite edges are reported as
   nil, and lambda-break as nil, which is the :G law's answer at every tick."
  [f tau {:keys [ln-e? f-pi?]}]
  (let [n (count (:g f))
        a (mapv (fn [i] (- (if ln-e? (nth (:ln-e f) i) 0.0)
                           (if f-pi? (nth (:f f) i) 0.0)))
                (range n))
        b (:g f)
        u (/ 1.0 (double tau))
        s (fn [i uu] (- (nth a i) (* (nth b i) uu)))
        top (first (reduce (fn [[bi bv] i] (let [v (s i u)] (if (> v bv) [i v] [bi bv])))
                           [0 (s 0 u)] (range n)))
        ;; Only crossings at u > 0 are reachable: u = 1/tau and tau > 0. A
        ;; crossing at u <= 0 is a property of the two lines extended past the
        ;; domain, and treating it as a window edge would report a temperature
        ;; that cannot exist.
        crossings (keep (fn [j]
                          (when (not= j top)
                            (let [db (- (double (nth b top)) (double (nth b j)))]
                              (when-not (zero? db)
                                (let [uc (/ (- (double (nth a top)) (double (nth a j))) db)]
                                  (when (and (pos? uc) (Double/isFinite uc)) uc))))))
                        (range n))
        above (filter #(> % u) crossings)
        below (filter #(< % u) crossings)
        u-hi (when (seq above) (apply min above))
        u-lo (when (seq below) (apply max below))
        ;; u = 1/tau, so a crossing ABOVE u bounds tau from BELOW.
        tau-lo (when u-hi (/ 1.0 u-hi))
        tau-hi (when u-lo (/ 1.0 u-lo))
        ratios (remove nil? [(when tau-hi (/ tau-hi (double tau)))
                             (when tau-lo (/ (double tau) tau-lo))])]
    {:winner top
     :tau-lo tau-lo
     :tau-hi tau-hi
     :lambda-break (when (seq ratios) (apply min ratios))}))

;; ---------------------------------------------------------------------------

(defn- fmt-opt [x] (if (nil? x) "        inf" (fmt "%11.6f" (double x))))

(defn run-field [label path]
  (let [records (read-records path)
        flds (vec (keep field records))
        n (count flds)]
    (emit "")
    (emit "=== " label " -- " path)
    (emit (fmt "%d records; %d with a usable aligned field; candidates per tick %s"
               (count records) n
               (str/join "," (map #(count (:g %)) flds))))
    (emit (fmt "dropped from the aligned field (F_pi absent or identity not unique): %s"
               (str/join "," (map :dropped flds))))
    (when (pos? n)
      ;; --- FIELD: ln E, and what the record's own decision chose ----------
      ;; The arms separate only where ln E is non-uniform over the aligned
      ;; field (:pi-zero-form :measurement :withdrawal), so the spread is a
      ;; precondition of the whole study and is measured here rather than
      ;; carried over from RUN7.
      (let [dv (map #(count (distinct (:ln-e %))) flds)
            sp (map #(- (apply max (:ln-e %)) (apply min (:ln-e %))) flds)
            zeros (map #(count (filter zero? (:ln-e %))) flds)]
        (emit (fmt "FIELD ln E over the aligned candidates: distinct values %s; spread %.10f..%.10f; candidates with ln E = 0: %d over %d ticks"
                   (str/join "," (distinct dv)) (apply min sp) (apply max sp)
                   (reduce + zeros) n)))
      (let [recs (filter #(some? (field %)) records)
            boundaries (frequencies (map #(get-in % [:decision :selection-boundary]) recs))
            agree (count (filter (fn [[r f]]
                                   (let [tau (tau-of-beta (:g f) 1.0)
                                         amx (argmax-index (selection-weights f tau {:ln-e? true :f-pi? false}))]
                                     (= (get-in r [:decision :action :target])
                                        (nth (:target f) amx))))
                                 (map vector recs flds)))]
        (emit (fmt "FIELD the record's own :decision -- :selection-boundary %s; its :action equals the argmax of ln E - G/tau on %d of %d ticks"
                   (pr-str boundaries) agree n)))

      ;; --- CONTROL 1: the generalised solver's :none arm against src --------
      (let [deltas (map (fn [f]
                          (let [mine (solve 1.0 (:g f) (:f f) (:ln-e f)
                                            {:e-in-pi? false :e-in-pi-0? false})
                                theirs (pp-solver/converge-beta 1.0 (:g f) (:f f))]
                            (Math/abs (- (double (:beta mine))
                                         (double (:beta-posterior theirs))))))
                        flds)]
        (emit (fmt "CONTROL 1 solver: local :none arm vs src converge-beta, beta_prior 1.0 -- max |delta| %.3e over %d ticks"
                   (apply max deltas) n)))

      ;; --- CONTROL 2: the recorded beta, where the run solved one -----------
      (let [pairs (keep (fn [[r f]]
                          (let [state (:policy-precision-state r)]
                            (when (number? (get-in state [:solve :beta-posterior]))
                              (let [s (solve (double (get-in state [:solve :beta-prior]))
                                             (:g f) (:f f) (:ln-e f)
                                             {:e-in-pi? false :e-in-pi-0? false})]
                                (Math/abs (- (double (get-in state [:solve :beta-posterior]))
                                             (double (:beta s))))))))
                        (map vector (filter #(some? (field %)) records) flds))]
        (if (seq pairs)
          (emit (fmt "CONTROL 2 recorded beta: run's own :beta-posterior reproduced from its own :beta-prior -- max |delta| %.3e over %d ticks"
                     (apply max pairs) (count pairs)))
          (emit "CONTROL 2 recorded beta: NOT APPLICABLE -- this field recorded no :policy-precision-state, so there is no beta to reproduce")))

      ;; --- CONTROL 3: the enactment leg the SPEC names as discriminator -----
      (let [gated (count (filter :act-gate-verdicts records))
            realized (count (filter :realized-outcome records))]
        (emit (fmt "CONTROL 3 enactment: records carrying :act-gate-verdicts %d of %d; :realized-outcome %d of %d -- NOT FOUND means the H1b bracket cannot be evaluated on this field"
                   gated (count records) realized (count records))))

      ;; --- CONTROL 4: tau-invariance of the ln-E-free, F_pi-free law -------
      (let [grid [1.0e-3 1.0e-2 0.1 0.5 1.0 2.0 10.0 1.0e2 1.0e3]
            per-tick (map (fn [f]
                            (let [amx (map #(argmax-index (selection-weights f % {:ln-e? false :f-pi? false})) grid)
                                  amin (argmax-index (mapv - (:g f)))]
                              (and (apply = amx) (= (first amx) amin))))
                          flds)]
        (emit (fmt "CONTROL 4 tau-invariance of the :G law: argmax equals argmin G at every tau in {1e-3..1e3} on %d of %d ticks (the arithmetic says all)"
                   (count (filter true? per-tick)) n)))

      ;; --- CONTROL 5: the detector has teeth -------------------------------
      ;; For each law, move tau to just outside the closed-form window and
      ;; check the argmax actually changes. A detector that cannot fire is not
      ;; evidence of no difference.
      (doseq [[law-name law] laws]
        (let [tested (keep (fn [f]
                             (let [w (tau-window f 1.0 law)]
                               (when (:tau-hi w)
                                 (let [inside (argmax-index (selection-weights f (* 0.999 (:tau-hi w)) law))
                                       outside (argmax-index (selection-weights f (* 1.001 (:tau-hi w)) law))]
                                   (not= inside outside)))))
                           flds)]
          (emit (fmt "CONTROL 5 detector teeth, law %-6s: a tau stepped across the closed-form window edge changes the argmax on %d of %d ticks that HAVE an edge%s"
                     (name law-name) (count (filter true? tested)) (count tested)
                     (if (zero? (count tested))
                       " (no tick has a finite edge -- the law's argmax is tau-invariant)"
                       "")))))

      ;; --- THE ARMS: beta, then tau, then the selection --------------------
      (doseq [b0 beta-zeros]
        (emit "")
        (emit (fmt "--- beta_0 = %.1f, carried" b0))
        (let [by-arm (into {} (map (fn [[a-name arm]]
                                     [a-name (carried-betas flds b0 arm)])
                                   arms))]
          (doseq [[a-name _] arms]
            (let [traj (get by-arm a-name)]
              (emit (fmt "  arm %-8s beta first %.10f last %.10f | gamma first %.10f last %.10f | converged %d/%d bracketed %d/%d"
                         (name a-name)
                         (double (:beta (first traj))) (double (:beta (peek traj)))
                         (double (/ 1.0 (:beta (first traj)))) (double (/ 1.0 (:beta (peek traj))))
                         (count (filter :converged? traj)) n
                         (count (filter :bracketed? traj)) n))))
          ;; tau spread between arms, per tick
          (let [rhos (map (fn [i]
                            (let [taus (map (fn [[a-name _]]
                                              (tau-of-beta (:g (nth flds i))
                                                           (:beta (nth (get by-arm a-name) i))))
                                            arms)]
                              (/ (apply max taus) (apply min taus))))
                          (range n))]
            (emit (fmt "  tau spread across arms (max/min): mean %.9f  max %.9f  (1.0 = the arms hand selection the same temperature)"
                       (/ (reduce + rhos) n) (apply max rhos))))
          ;; the selection comparison, per law
          (doseq [[law-name law] laws]
            (let [per-tick
                  (map (fn [i]
                         (let [f (nth flds i)
                               ws (into {} (map (fn [[a-name _]]
                                                  (let [tau (tau-of-beta (:g f) (:beta (nth (get by-arm a-name) i)))]
                                                    [a-name {:tau tau
                                                             :w (selection-weights f tau law)}]))
                                                arms))
                               amx (into {} (map (fn [[a-name _]]
                                                   [a-name (argmax-index (:w (get ws a-name)))])
                                                 arms))
                               taus (map (comp :tau val) ws)
                               win (tau-window f (:tau (get ws :both)) law)]
                           {:tick i
                            :argmax amx
                            :distinct (count (distinct (vals amx)))
                            :moves-none-vs-both (ordering-moves (:w (get ws :none)) (:w (get ws :both)))
                            :moves-pi-vs-both (ordering-moves (:w (get ws :pi-only)) (:w (get ws :both)))
                            :tv-none-vs-both (tv-distance (:w (get ws :none)) (:w (get ws :both)))
                            :tv-pi-vs-both (tv-distance (:w (get ws :pi-only)) (:w (get ws :both)))
                            :rho (/ (apply max taus) (apply min taus))
                            :lambda-break (:lambda-break win)
                            :tau-lo (:tau-lo win) :tau-hi (:tau-hi win)
                            :machine-rank-of-winner (nth (:machine-rank f) (get amx :both))}))
                       (range n))
                  differing (count (filter #(> (:distinct %) 1) per-tick))
                  lbs (keep :lambda-break per-tick)]
              (emit (fmt "  law %-6s argmax DIFFERS between arms on %d of %d ticks | rank moves none-vs-both total %d (max displacement %d) | pi-only-vs-both total %d (max %d) | max TV none-vs-both %.3e pi-only-vs-both %.3e"
                         (name law-name) differing n
                         (reduce + (map (comp :moved :moves-none-vs-both) per-tick))
                         (apply max 0 (map (comp :max-move :moves-none-vs-both) per-tick))
                         (reduce + (map (comp :moved :moves-pi-vs-both) per-tick))
                         (apply max 0 (map (comp :max-move :moves-pi-vs-both) per-tick))
                         (apply max (map :tv-none-vs-both per-tick))
                         (apply max (map :tv-pi-vs-both per-tick))))
              (if (seq lbs)
                (let [lb-min (apply min lbs)
                      rho-max (apply max (map :rho per-tick))]
                  (emit (fmt "         margin: lambda_break min %.6f mean %.6f (the factor tau must move to change the choice) against rho max %.9f (the factor the arms differ by) -- headroom %.2fx%s"
                             lb-min (/ (reduce + lbs) (count lbs)) rho-max
                             (/ (- lb-min 1.0) (max 1.0e-300 (- rho-max 1.0)))
                             (if (< lb-min rho-max)
                               "  <-- the arms move tau FURTHER than the choice can survive"
                               ""))))
                (emit "         margin: NO finite window edge on any tick -- this law's argmax is tau-invariant, so no beta can move it"))
              (when (= 1.0 b0)
                (doseq [t per-tick]
                  (emit (fmt "         t%-3d argmax %s%s | machine rank of winner %3d | rho %.9f | tau window [%s , %s] lambda_break %s"
                             (inc (:tick t))
                             (str/join "/" (map (fn [[a-name _]] (get (:argmax t) a-name)) arms))
                             (if (> (:distinct t) 1) "  <-- DIFFERS" "")
                             (:machine-rank-of-winner t)
                             (:rho t)
                             (fmt-opt (:tau-lo t)) (fmt-opt (:tau-hi t))
                             (if (:lambda-break t) (fmt "%.6f" (:lambda-break t)) "inf"))))))))))
    {:label label :ticks n}))

(defn -main [& _]
  (emit "U3 -- pi_0 placement (:choices :pi-zero-form): does the arm change what the machine SELECTS?")
  (emit "")
  (emit "Arms: :none (src default, and what S2 recorded) | :pi-only (friston2017.txt:684 as printed)")
  (emit "      | :both (spm_MDP_VB_X.m:963-964, the authors' own code; the entry's :interim)")
  (emit "Chain: arm -> beta (eq. 2.7 root, carried) -> tau = beta (policy.clj:135-137,")
  (emit "       :tau-mode :variational-beta-gamma) -> softmax(ln E - G/tau - F_pi) (policy.clj:148)")
  (emit "argmax column is none/pi-only/both, as indices into the aligned field.")
  (emit "")
  (emit "The four score expressions, and where each one is in the code:")
  (doseq [[law-name law] laws]
    (emit (fmt "  %-6s %s" (name law-name) (:where law))))
  (let [results (doall (map (fn [[label path]] (run-field label path)) fields))]
    (emit "")
    (emit (fmt "TOTAL scorable ticks: %d" (reduce + (map :ticks results)))))
  (let [text (str (str/join "\n" @lines) "\n")]
    (spit out-path text)
    (print text)
    (flush)))

(apply -main *command-line-args*)
