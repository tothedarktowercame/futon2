#!/usr/bin/env bb
;; h_value_undominated.clj — H-VALUE-CAL-D §8: is instance 4 undominated
;; under the calendar value term?
;;
;; claude-11, 2026-09-25. Deterministic (seeded), reads only, no repo writes.
;; Run: bb h_value_undominated.clj
;;
;; Value term: H-VALUE-CAL-D §3, single-pick form, unchanged:
;;   V_i(w) = SUM_o w_o * a_i(o; cell) - lam * cost_i
;;   a_i(o) = SUM over (token -> o) links of d * f_o(T_i)
;;   f_o(T) = (H - T)+ / H for R S D C; V flows from E_vs only if T < s-vs;
;;   7 landing at or after s-vs carries kappa on every service.
;;   T_i = E[attempts_i] at theta 0.8 (target-cost.edn).
;; Cells: H x s-vs x kappa x d5-rob, the grid of H-VALUE-CAL-D; lam on the
;; H-VALUE-D grid.
;;
;; Outcomes: all SIX mission-C states. :joe-can-use-robs-work is served by no
;; scored instance (instance 8 is :prospective, no cascade), so its
;; coefficient is 0 for every candidate. It is kept in the simplex because it
;; is stated; at lam = 0 it cannot change any argmax.
;;
;; DOMINANCE (definition, from the requisition): x dominates 4 iff
;; V_x(w) >= V_4(w) for every w in the simplex and > for some w. V_x - V_4 is
;; affine in w, so over the simplex it is >= 0 everywhere iff >= 0 at every
;; vertex, and > 0 somewhere iff > 0 at some vertex. The check below is
;; therefore EXACT (vertices), not sampled.
;;
;; TOP-PICK SHARE: uniform Dirichlet(1,...,1) over the six outcomes, N samples
;; from a seeded java.util.Random (normalised exponentials); 4 is the top pick
;; iff V_4 is strictly greater than each of V_5, V_6, V_7.

(require '[clojure.edn :as edn]
         '[babashka.fs :as fs])
(import '[java.security MessageDigest])

(def item6 "/home/joe/code/futon3c/holes/labs/M-futon-seams/item6/")
(defn sha256 [path]
  (let [d (MessageDigest/getInstance "SHA-256")]
    (apply str (map #(format "%02x" %) (.digest d (fs/read-all-bytes path))))))

(def mission-c (edn/read-string (slurp (str item6 "mission-C.edn"))))
(def cost (into {} (for [t (:targets (edn/read-string (slurp (str item6 "target-cost.edn"))))]
                     [(:instance t) (double (:expected-attempts t))])))

(def OUTS [:rob-can-run-the-stack :second-implementation-is-cheap :no-drifting-forks
           :coupling-visible-to-tooling :vs-code-implementation-possible :joe-can-use-robs-work])
(assert (= (set OUTS) (set (keys (:outcomes mission-c)))) "every stated outcome, no other")
(def CANDS ["4" "5" "6" "7"])

(def links
  "[instance token outcome] for every scorable :served-by entry."
  (vec (for [{:keys [instance want outcome status]} (:served-by mission-c)
             :when (and want (not= :prospective status))]
         [instance want outcome])))

(defn degree [i token o d5r]
  (if (and (= i "5") (= token :impersonation-retired) (= o :rob-can-run-the-stack)) d5r 1.0))

(defn factor [{:keys [H s-vs kappa timing?]} i o T]
  (if-not timing?
    1.0
    (let [flow (fn [from] (/ (max 0.0 (- H from)) H))
          late? (and (= i "7") (>= T s-vs))]
      (* (if late? kappa 1.0)
         (if (= o :vs-code-implementation-possible)
           (if (< T s-vs) (if (Double/isInfinite s-vs) 0.0 (flow s-vs)) (flow T))
           (flow T))))))

(defn coeffs
  "Per candidate, a double vector over OUTS."
  [p]
  (into {} (for [i CANDS]
             [i (reduce (fn [v [inst token o]]
                          (if (= inst i)
                            (update v (.indexOf ^java.util.List OUTS o) +
                                    (* (degree i token o (:d5r p)) (factor p i o (get cost i))))
                            v))
                        (vec (repeat (count OUTS) 0.0)) links)])))

(def LAMS [0.0 0.02 0.05 0.1 0.2 0.4])
(def cells (vec (for [H [30.0 60.0 120.0] s-vs [5.0 15.0 25.0 40.0 ##Inf]
                      kappa [0.0 0.5] d5r [0.25 0.5 0.75 1.0]]
                  {:H H :s-vs s-vs :kappa kappa :d5r d5r :timing? true})))
(def static-cells (vec (for [d5r [0.25 0.5 0.75 1.0]] {:d5r d5r :timing? false})))

(defn vertex-diff
  "V_x - V_4 at the vertex e_o."
  [co lam x oi]
  (- (- (nth (get co x) oi) (* lam (get cost x)))
     (- (nth (get co "4") oi) (* lam (get cost "4")))))

(defn dominators
  "Candidates dominating 4 in this cell, and candidates tying 4 at every vertex."
  [co lam]
  (let [ds (fn [x] (map #(vertex-diff co lam x %) (range (count OUTS))))]
    {:dominates (vec (for [x (rest CANDS) :let [d (ds x)]
                           :when (and (every? #(>= % 0.0) d) (some pos? d))] x))
     :ties-everywhere (vec (for [x (rest CANDS) :when (every? zero? (ds x))] x))}))

;; ------------------------------------------------------------ Dirichlet
(def N 20000)
(def samples
  (let [r (java.util.Random. 20260925)]
    (vec (repeatedly N (fn [] (let [e (mapv (fn [_] (- (Math/log (- 1.0 (.nextDouble r))))) OUTS)
                                    s (reduce + e)]
                                (double-array (map #(/ % s) e))))))))

(defn dotv [^doubles w v] (loop [k 0 acc 0.0] (if (= k (alength w)) acc (recur (inc k) (+ acc (* (aget w k) (double (nth v k))))))))

(defn top4-count [co lam]
  (let [c4 (get co "4") others (mapv #(vector (get co %) (* lam (get cost %))) (rest CANDS))
        l4 (* lam (get cost "4"))]
    (count (filter (fn [w] (let [v4 (- (dotv w c4) l4)]
                             (every? (fn [[c l]] (> v4 (- (dotv w c) l))) others)))
                   samples))))

(defn r4 [x] (/ (Math/round (* 10000.0 x)) 10000.0))

(defn share [cs lam]
  (let [counts (map #(top4-count (coeffs %) lam) cs)]
    {:cells (count cs) :pooled (r4 (/ (double (reduce + counts)) (* N (count cs))))
     :min-cell (r4 (/ (double (apply min counts)) N)) :max-cell (r4 (/ (double (apply max counts)) N))}))

;; ------------------------------------------------------------ report
(println ";; h_value_undominated.clj — H-VALUE-CAL-D §8, claude-11, 2026-09-25")
(println ";; inputs (sha256):")
(doseq [f ["mission-C.edn" "target-cost.edn"]]
  (println (str ";;   " (sha256 (str item6 f)) "  " item6 f)))
(prn {:links links :cost (select-keys cost CANDS) :outcomes OUTS :N N :seed 20260925})

(println "\n;; bad case: 5 against 4 on the Rob outcome alone (w = e_rob), max over every cell")
(let [oi 0
      diffs (for [lam LAMS p (concat cells static-cells)]
              [(vertex-diff (coeffs p) lam "5" oi) lam p])
      [best lam p] (apply max-key first diffs)]
  (prn {:max-V5-minus-V4-at-e-rob (r4 best) :at {:lambda lam :cell p}
        :any-cell-where-5-beats-4-on-rob (boolean (some #(pos? (first %)) diffs))
        :timed-cells-max (r4 (apply max (for [lam LAMS p cells] (vertex-diff (coeffs p) lam "5" oi))))}))

;; Planted bad cases for the detector: a copy of 4 must tie 4 everywhere, and
;; 4 plus one unit of drift service must dominate it. Same cost as 4.
(let [co4 (get (coeffs (first cells)) "4")
      planted {"4" co4 "5" co4 "6" (update co4 2 + 1.0) "7" (vec (repeat (count OUTS) 0.0))}]
  (with-redefs [cost (assoc cost "5" (get cost "4") "6" (get cost "4"))]
    (let [r (dominators planted 0.1)]
      (prn {:planted-check r})
      (assert (= ["6"] (:dominates r)) "detector misses a planted dominator")
      (assert (= ["5"] (:ties-everywhere r)) "detector misses a planted tie"))))

(println "\n;; (i) and (iv): exact dominance at the simplex vertices")
(let [rows (for [lam LAMS p (concat cells static-cells)] (dominators (coeffs p) lam))]
  (prn {:cells-checked (count rows)
        :cells-where-some-x-dominates-4 (count (filter (comp seq :dominates) rows))
        :dominators-seen (vec (distinct (mapcat :dominates rows)))
        :cells-where-some-x-ties-4-everywhere (count (filter (comp seq :ties-everywhere) rows))}))
(println ";; 4 at the S vertex, per candidate, lam = 0 (any timed cell):")
(let [co (coeffs (first cells))]
  (prn (into {} (for [x CANDS] [x (r4 (nth (get co x) 1))]))))

(println "\n;; (ii) and (iii): share of the Dirichlet simplex on which 4 is the strict top pick")
(doseq [lam LAMS]
  (prn {:lambda lam
        :all-timed-cells (share cells lam)
        :vs-before-any-instance-s-vs-5 (share (filter #(= 5.0 (:s-vs %)) cells) lam)
        :deadline-reachable (share (filter #(> (:s-vs %) (get cost "7")) cells) lam)
        :static-no-timing (share static-cells lam)}))
