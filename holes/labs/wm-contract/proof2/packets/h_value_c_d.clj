#!/usr/bin/env bb
;; h_value_c_d.clj — H-VALUE-C-D: a time-indexed C-schedule with drift
;; interest and graded token preferences, swept against M-futon-seams' own
;; ranking 4 > 5 > 7.
;;
;; claude-4, 2026-09-24. PROOF-2 packet H-VALUE-C-D (claude-8's requisition).
;; Deterministic, no repo writes, reads only. Run: bb h_value_c_d.clj
;;
;; SUCCESSOR TO h_value_d.py (kimi-7), which showed no static scalar V per
;; instance reproduces the mission's ranking. The narrower fix under test
;; here (PROOF-2a, claude-10's read): timing enters through the lane's
;; time-indexed preference :c-schedule as a per-step decay, and degree enters
;; as graded preferences over the TOKENS a sub-goal produces rather than
;; binary wants.
;;
;;   V(i) = SUM over tokens t in want(i), outcomes o that t serves, of
;;            w_o * d_i(t,o) * u_o * exp(-rho * step_i(t))
;;                             * exp(+delta * step_i(t))  [o = no-drifting-forks only]
;;        - lam * E[attempts_i]
;;
;; The lane minimises G; V here is the negated pragmatic term, so the
;; mission's 4 > 5 > 7 is V4 > V5 > V7, i.e. G4 < G5 < G7.
;;
;; AUTHORITY OF EVERY PARAMETER (nothing below is a guessed constant):
;;
;; step_i(t)  MEASURED from the instance cascades themselves. Not transcribed:
;;            this script reads instance-{4,5,6,7}.edn and computes the
;;            rollout level of each want token from the :guard/:needs ->
;;            :produces DAG (initial tokens at step 0; a pattern fires at
;;            1 + max step of its needs). Input shas are printed with the
;;            result so the run is pinned.
;; served-by  mission-C.edn (claude-1, owner) at TOKEN grain: each entry is
;;            (instance, want token, outcome). This is finer than
;;            h_value_d.py's instance -> outcome set, and it is the grain the
;;            graded-preference fix asks for.
;; cost       MEASURED, target-cost.edn (claude-10, theta 0.8):
;;            4 = 8.75, 5 = 10, 6 = 10, 7 = 8.75.
;; w_o        mission states NO weights (mission-C :preference :status
;;            :unstated, span [17969 18029]). Swept over the simplex at step
;;            1/10, 1001 points — h_value_d.py's grid, unchanged, so the
;;            fractions are comparable.
;; d          degree of attainment. Text states one: 4 "unblocks provider
;;            substitution for Rob immediately", unqualified, so
;;            d_4(prefix-routing-retired, rob) = 1. 5 only "retires
;;            matrix-ircd" (span [18337 18358]) without saying how fully that
;;            serves Rob's four-facet outcome: UNSTATED, a typed absence,
;;            swept {0.25,0.5,0.75,1.0} on d_5(impersonation-retired, rob).
;;            No other degree is stated anywhere; those terms carry 1, which
;;            is the absence-of-degree reading and not a claim.
;; u_o        outcome-level deferral. Text states one: the vs-code outcome is
;;            explicitly NOT wanted now ("He is **not** asking for those
;;            adapters to be built now", span [14618 14667]). u_V swept
;;            {0.1,0.25,0.5,0.75,1.0}; all other u = 1.
;; lam        cost weight. No ruling. Swept {0,.02,.05,.1,.2,.4}, h_value_d's.
;; rho        THE DECAY RATE. **No authority found for a C-schedule decay.**
;;            The lane's only per-step rate is rollout-discount
;;            (src/futon2/aif/rollout.clj:481-485, option :temporal-discount
;;            aliased :gamma, default 0.9), which discounts the ROLLOUT
;;            ACCUMULATOR S(pi) = SUM gamma^t g(s_t), not C. C535
;;            (holes/labs/wm-contract/C535-U64-rollout-parameter-provenance.md)
;;            is the census: the SHAPE is commissioned
;;            (holes/E-policy-rollout-engine.md), the VALUE 0.9 is "not found"
;;            — it restates a library default carried from a smoke-test
;;            witness, and C535 writes no ruling. So rho is swept
;;            {0, 0.105, 0.2, 0.4, 0.8}, with 0.105 = -ln(0.9) marked as the
;;            lane's inherited rate, not as an authorised one.
;; delta      DRIFT INTEREST, a preference for drift-serving tokens that GROWS
;;            with t, compounding. No authority; no rate is stated anywhere.
;;            Swept on the same grid as rho so the two are comparable.
;;            The mission's drift claim is qualitative: "Two implementations
;;            that have already drifted cost more to unify than one
;;            implementation plus a stub" (span [3227 3327]).
;;
;; Instance 6 carries no rank ("Instance 6 is the warning", span
;; [18901 18926]); the mission's order is over {4,5,7} and 6's position is a
;; typed absence, so 4>5>7 is counted irrespective of where 6 lands — exactly
;; as h_value_d.py counted it.

(require '[clojure.edn :as edn]
         '[clojure.set :as set]
         '[clojure.string :as str]
         '[babashka.fs :as fs])
(import '[java.security MessageDigest])

(def proto "/home/joe/code/futon3c/holes/labs/M-futon-seams/proto/")
(def item6 "/home/joe/code/futon3c/holes/labs/M-futon-seams/item6/")
(def instances ["4" "5" "6" "7"])

(defn sha256 [path]
  (let [d (MessageDigest/getInstance "SHA-256")]
    (->> (.digest d (fs/read-all-bytes path))
         (map #(format "%02x" %))
         (apply str))))

;; ---------------------------------------------------------------- steps
(defn want-steps
  "Rollout step at which each want token of INSTANCE is first produced.
   Initial tokens are at step 0; a pattern fires at the first step by which
   every token in its :guard/:needs has been produced. This is the same DAG
   target_cost.clj counts patterns over, read for depth instead of count."
  [cascade]
  (let [pats (:patterns cascade)]
    (loop [reached (zipmap (set (:initial cascade)) (repeat 0)), step 1, guard 0]
      (let [firing (for [[_ i] pats
                         :when (and (every? reached (get-in i [:guard :needs]))
                                    (not-every? reached (:produces i)))]
                     (:produces i))]
        (if (or (empty? firing) (> guard 50))
          (select-keys reached (:want cascade))
          (recur (into reached (for [prods firing, t prods] [t step]))
                 (inc step) (inc guard)))))))

;; ------------------------------------------------------- mission inputs
(def mission-c (edn/read-string (slurp (str item6 "mission-C.edn"))))
(def cost (into {} (for [t (:targets (edn/read-string (slurp (str item6 "target-cost.edn"))))]
                     [(:instance t) (double (:expected-attempts t))])))

(def outcome-key
  "The five outcomes any scored instance serves, in h_value_d.py's order."
  {:rob-can-run-the-stack :R
   :second-implementation-is-cheap :S
   :no-drifting-forks :D
   :coupling-visible-to-tooling :C
   :vs-code-implementation-possible :V})
(def OUTS [:R :S :D :C :V])

(def token-links
  "(instance, want token) -> set of outcome keys, from mission-C :served-by.
   Prospective entries (instance 8, :want nil) are dropped: no cascade, no
   tokens, not scorable — mission-C says so itself."
  (reduce (fn [m {:keys [instance want outcome status]}]
            (if (or (nil? want) (= :prospective status))
              m
              (update m [instance want] (fnil conj #{}) (outcome-key outcome))))
          {} (:served-by mission-c)))

(def cascades (into {} (for [n instances]
                         [n (edn/read-string (slurp (str proto "instance-" n ".edn")))])))
(def steps (into {} (for [n instances] [n (want-steps (get cascades n))])))

;; --------------------------------------------------------------- sweep
(def N 10)
(def W-GRID (for [a (range (inc N)) b (range (inc (- N a))) c (range (inc (- N a b)))
                  d (range (inc (- N a b c)))
                  :let [e (- N a b c d)]]
              (mapv #(/ (double %) N) [a b c d e])))
(def LAMS [0 0.02 0.05 0.1 0.2 0.4])
(def RHOS [0.0 0.105 0.2 0.4 0.8])      ; 0.105 = -ln 0.9, the lane's inherited rate
(def DELTAS [0.0 0.105 0.2 0.4 0.8])
(def U-V [0.1 0.25 0.5 0.75 1.0])
(def D5R [0.25 0.5 0.75 1.0])

(defn degree
  "Stated degree, else 1.0 (the absence-of-degree reading). The single swept
   degree is 5's service of rob through :impersonation-retired."
  [i token o d5r]
  (if (and (= i "5") (= token :impersonation-retired) (= o :R)) d5r 1.0))

(defn coefficients
  "Per instance, the vector a_i(o) such that V(i) = SUM_o a_i(o)*w_o - lam*cost_i.
   Linear in w, so the 1001-point simplex is a dot product per instance."
  [rho delta u-v d5r]
  (into {}
        (for [i instances]
          [i (mapv (fn [o]
                     (reduce + 0.0
                             (for [[[inst token] outs] token-links
                                   :when (and (= inst i) (contains? outs o))
                                   :let [t (double (get (get steps i) token 0))]]
                               (* (degree i token o d5r)
                                  (if (= o :V) u-v 1.0)
                                  (Math/exp (- (* rho t)))
                                  (if (= o :D) (Math/exp (* delta t)) 1.0)))))
                   OUTS)])))

(defn dot [a w] (reduce + 0.0 (map * a w)))

(defn cell-fractions
  "Shares over the w-simplex for one (lam, rho, delta, u_V, d5r) cell."
  [lam coeff]
  (let [v (fn [i w] (- (dot (get coeff i) w) (* lam (get cost i))))]
    (reduce (fn [[ok seven] w]
              (let [v4 (v "4" w) v5 (v "5" w) v6 (v "6" w) v7 (v "7" w)]
                [(if (and (> v4 v5) (> v5 v7)) (inc ok) ok)
                 (if (and (> v7 v4) (> v7 v5) (> v7 v6)) (inc seven) seven)]))
            [0 0] W-GRID)))

(defn r4 [x] (/ (Math/round (* 10000.0 x)) 10000.0))

(defn aggregate
  "Sum the per-cell counts over the cells a reading admits."
  [lam cells]
  (let [[ok seven n] (reduce (fn [[o s n] [_ coeff]]
                               (let [[a b] (cell-fractions lam coeff)]
                                 [(+ o a) (+ s b) (+ n (count W-GRID))]))
                             [0 0 0] cells)]
    {:points n :share-4>5>7 (r4 (/ (double ok) n)) :share-7-first (r4 (/ (double seven) n))}))

(def all-cells
  (for [rho RHOS delta DELTAS u-v U-V d5r D5R]
    [{:rho rho :delta delta :u-v u-v :d5r d5r} (coefficients rho delta u-v d5r)]))

(defn where [pred] (filter (comp pred first) all-cells))

(println ";; h_value_c_d.clj — H-VALUE-C-D, claude-4, 2026-09-24")
(println ";; inputs (sha256):")
(doseq [p (concat (for [n instances] (str proto "instance-" n ".edn"))
                  [(str item6 "mission-C.edn") (str item6 "target-cost.edn")])]
  (println (str ";;   " (sha256 p) "  " p)))
(println)
(prn {:want-steps steps
      :token-links (into (sorted-map) (for [[k v] token-links] [(str (first k) " " (second k)) (vec (sort v))]))
      :cost cost
      :swept {:w-simplex-step (/ 1.0 N) :w-points (count W-GRID)
              :lambda LAMS :rho RHOS :delta DELTAS :u-vs-code U-V :d5-rob D5R}})
(println)
(doseq [lam LAMS]
  (prn {:lambda lam
        :no-timing-rho0-delta0   (aggregate lam (where #(and (zero? (:rho %)) (zero? (:delta %)))))
        :decay-only-delta0       (aggregate lam (where #(and (pos? (:rho %)) (zero? (:delta %)))))
        :drift-only-rho0         (aggregate lam (where #(and (zero? (:rho %)) (pos? (:delta %)))))
        :both-positive           (aggregate lam (where #(and (pos? (:rho %)) (pos? (:delta %)))))
        :full-sweep              (aggregate lam all-cells)
        :text-reading            (aggregate lam (where #(and (<= (:u-v %) 0.5) (<= (:d5r %) 0.5))))
        :lane-rate-rho-0.105     (aggregate lam (where #(= 0.105 (:rho %))))}))

;; =====================================================================
;; H-VALUE-CAL-D (claude-11, 2026-09-25): C over a CALENDAR index,
;; separate from rollout step. The sections above are unchanged, so their
;; output still reproduces H-VALUE-C-D; this section is one more reading.
;; Design and every span: H-VALUE-CAL-D.md.
;;
;; Clock: calendar time t in attempt units, t = 0 at the mission's record
;; date. DECLARED mapping: rollout step contributes no calendar time. Every
;; want token of instance i is attained together at the instance's calendar
;; completion time T_i, because the IDENTIFY exit is conjunctive (interface
;; declared AND a caller converted AND a redirect test). T_i = E[attempts_i]
;; at theta 0.8 (target-cost.edn), one attempt = one unit.
;;
;; Events: E_now (t = 0); E_vs, "a VS Code implementation exists" (date
;; UNSTATED, swept as s-vs; ##Inf = not within any horizon).
;;
;; Attainment of outcome o by instance i landing at T, as a share of the
;; horizon H (H unstated, swept):
;;   R, S, D, C : flow from T to H             (H - T)+ / H
;;   V          : flow from E_vs to H, only if the seam precedes E_vs;
;;                a VS Code client landing first leaves the retrofit
;;                degree kappa (unstated, swept)
;;   instance 7 : "do before a VS Code implementation exists, not after":
;;                if T_7 >= s-vs, every service of 7 carries kappa.
;; Degree d as above (d5-rob swept, others 1). u_V is gone: the deferral
;; of V and the deadline on 7 are both stated against E_vs.
;;
;; Two rankings:
;;   single-pick  V(i) = SUM a_i(o) w_o f_o(T_i) - lam*cost_i; "Pick one
;;                instance" (span [17969 18029]); 4>5>7 as before.
;;   sequence     the order of {4,5,7} worked one after another; V(order)
;;                sums each instance's value at its cumulative completion
;;                time. lam*cost is the same for every order and drops out.
;;                4>5>7 = (4 5 7) is the strict best of the six orders;
;;                7-first = the best order starts with 7.

(def CAL-H [30.0 60.0 120.0])
(def CAL-SVS [5.0 15.0 25.0 40.0 ##Inf])
(def CAL-KAPPA [0.0 0.5])
(def cal-T (into {} (for [i instances] [i (get cost i)])))

(defn cal-token-coeffs
  "Per instance: [[outcome-index degree] ...] for every (token, outcome) link.
   Degree only; timing is applied at a completion time."
  [d5r]
  (into {} (for [i instances]
             [i (vec (for [[[inst token] outs] token-links :when (= inst i)
                           o outs]
                       [(.indexOf ^java.util.List OUTS o) (degree i token o d5r)]))])))

(defn cal-factor
  "Attainment share for outcome O of instance I landing at calendar time T."
  [{:keys [H s-vs kappa]} i o T]
  (let [flow (fn [from] (/ (max 0.0 (- H from)) H))
        late? (and (= i "7") (>= T s-vs))]
    (* (if late? kappa 1.0)
       (if (= o :V)
         (if (< T s-vs) (if (Double/isInfinite s-vs) 0.0 (flow s-vs)) (flow T))
         (flow T)))))

(defn cal-coeff
  "Vector a_i(o)*f_o(T) for instance I landing at T."
  [p tc i T]
  (reduce (fn [v [oi d]] (update v oi + (* d (cal-factor p i (nth OUTS oi) T))))
          [0.0 0.0 0.0 0.0 0.0] (get tc i)))

(def cal-cells
  (vec (for [H CAL-H s-vs CAL-SVS kappa CAL-KAPPA d5r D5R]
         {:H H :s-vs s-vs :kappa kappa :d5r d5r})))

(def ORDERS (for [a ["4" "5" "7"] b ["4" "5" "7"] c ["4" "5" "7"]
                  :when (= 3 (count (set [a b c])))] [a b c]))

(defn cal-cell-data
  "Precomputed coefficients for one cell: single-pick per instance, and per
   order the summed coefficient vector."
  [p]
  (let [tc (cal-token-coeffs (:d5r p))]
    {:single (into {} (for [i instances] [i (cal-coeff p tc i (cal-T i))]))
     :orders (into {} (for [ord ORDERS]
                        [ord (apply mapv + (map (fn [i t] (cal-coeff p tc i t))
                                                ord (reductions + (map cal-T ord))))]))}))

(def cal-data (mapv (fn [p] [p (cal-cell-data p)]) cal-cells))

(defn cal-aggregate
  [lam pred]
  (let [cells (filter (comp pred first) cal-data)
        [ok7 sev ok-seq sev-seq n]
        (reduce
         (fn [acc [_ {:keys [single orders]}]]
           (reduce
            (fn [[a b c d n] w]
              (let [v (fn [i] (- (dot (get single i) w) (* lam (get cost i))))
                    v4 (v "4") v5 (v "5") v6 (v "6") v7 (v "7")
                    ov (into {} (for [[ord co] orders] [ord (dot co w)]))
                    best (apply max (vals ov))
                    top (filter #(= best (ov %)) ORDERS)]
                [(if (and (> v4 v5) (> v5 v7)) (inc a) a)
                 (if (and (> v7 v4) (> v7 v5) (> v7 v6)) (inc b) b)
                 (if (= [["4" "5" "7"]] top) (inc c) c)
                 (if (and (= 1 (count top)) (= "7" (ffirst top))) (inc d) d)
                 (inc n)]))
            acc W-GRID))
         [0 0 0 0 0] cells)]
    {:points n
     :single-4>5>7 (r4 (/ (double ok7) n)) :single-7-first (r4 (/ (double sev) n))
     :sequence-4>5>7 (r4 (/ (double ok-seq) n)) :sequence-7-first (r4 (/ (double sev-seq) n))}))

(println)
(println ";; H-VALUE-CAL-D (claude-11): calendar index, completion times T_i =" (pr-str cal-T))
(prn {:swept {:H CAL-H :s-vs CAL-SVS :kappa CAL-KAPPA :d5-rob D5R :w-points (count W-GRID)
              :orders (count ORDERS)}})

;; Cross-check: with every attainment share forced to 1 the assembly must
;; reproduce the static token-grain cells at u_V = 1, rho = delta = 0.
(let [static (aggregate 0 (where #(and (zero? (:rho %)) (zero? (:delta %)) (= 1.0 (:u-v %)))))
      forced (let [cells (for [d5r D5R]
                           (let [tc (cal-token-coeffs d5r)]
                             [nil (into {} (for [i instances]
                                             [i (reduce (fn [v [oi d]] (update v oi + d))
                                                        [0.0 0.0 0.0 0.0 0.0] (get tc i))]))]))]
               (aggregate 0 cells))]
  (prn {:cross-check-static-uV1 static :calendar-assembly-with-shares-1 forced
        :equal (= static forced)}))

(doseq [lam LAMS]
  (prn {:lambda lam
        :calendar-full          (cal-aggregate lam (constantly true))
        :vs-not-in-horizon      (cal-aggregate lam #(Double/isInfinite (:s-vs %)))
        :vs-after-all-three     (cal-aggregate lam #(= 40.0 (:s-vs %)))
        :vs-mid-sequence        (cal-aggregate lam #(#{15.0 25.0} (:s-vs %)))
        :vs-before-any-lands    (cal-aggregate lam #(= 5.0 (:s-vs %)))
        :deadline-reachable     (cal-aggregate lam #(> (:s-vs %) (cal-T "7")))
        :text-reading-d5r<=0.5  (cal-aggregate lam #(<= (:d5r %) 0.5))}))
