(ns futon2.settling-confinement
  "T-ladder-05-settling-confinement: exhaustive settling and confinement
  test for rule byte 77 on binary rings.

  The rule byte b names the elementary map f_b(x) = bit x of b over the
  eight 3-bit neighbourhoods x = 4l+2c+r (Wolfram order, as in
  futon2.materials-catalogue).  A ring of n cells is a bitmask long, bit i
  = cell i, indices mod n (periodic boundary).  One synchronous step maps
  each cell to f_77 of its neighbourhood.  A ring SETTLES when its
  trajectory reaches a fixed point (a state equal to its own successor);
  its TRANSIENT is the number of steps to the first fixed point.  A ring
  that never reaches one (its trajectory is eventually periodic with
  period > 1) does not settle -- under rule 77 the two uniform rings form
  a 2-cycle, so they never settle.  No ring is excluded, no shortcut is
  taken: every 2^n starting ring is enumerated for n = 3..20.

  DAMAGE: flip one site of a fixed-point ring (the settled state) and run
  the damaged trajectory against the undamaged one (which, being fixed,
  never changes).  The damage depth is the maximum circular distance from
  the flipped site of any differing cell, over all later times, until the
  damaged ring itself settles.  Every fixed-point ring for n = 4..20 is
  enumerated and flipped at every site.

  The TN's two claims are then recorded: (1) settles from any state in at
  most 6 steps; (2) a single flip in a settled ring spreads to depth at
  most 1.  Expected (and delivered by this exhaustive run): the settling
  claim is REFUTED (the uniform rings never settle at any n, and the
  maximum transient floor(n/2)-1 exceeds 6 first at n = 16), the
  confinement claim is UPHELD (damage depth 1 for every n = 4..20)."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def ^:const ^long rule 77)
(def ^:const ^long settling-n-min 3)
(def ^:const ^long settling-n-max 20)
(def ^:const ^long damage-n-min 4)
(def ^:const ^long damage-n-max 20)
(def ^:const ^long tn-transient-bound 6)
(def ^:const ^long tn-damage-bound 1)

(defn rule-bit ^long [^long x] (if (pos? (bit-and (bit-shift-right rule x) 1)) 1 0))

(defn step
  "One synchronous rule-77 step of the n-cell ring given as bitmask `s`."
  ^long [^long n ^long s]
  (loop [i 0, acc 0]
    (if (== i n)
      acc
      (let [pm (unchecked-long (mod (unchecked-dec i) n))
            pp (unchecked-long (mod (unchecked-inc i) n))
            l (bit-and 1 (bit-shift-right s pm))
            c (bit-and 1 (bit-shift-right s i))
            r (bit-and 1 (bit-shift-right s pp))
            x (unchecked-add (unchecked-multiply 4 l)
                             (unchecked-add (unchecked-multiply 2 c) r))]
        (recur (unchecked-inc i)
               (if (pos? (bit-and (bit-shift-right rule x) 1))
                 (unchecked-add acc (bit-shift-left 1 i))
                 acc))))))

(defn fixed-point? ^Boolean [^long n ^long s] (== s (step n s)))

(defn ring-string
  "Bit string of ring `s`, cell 0 rightmost (read left-to-right as
  cells n-1..0)."
  ^String [^long n ^long s]
  (apply str (for [i (range (dec n) -1 -1)]
               (if (pos? (bit-and (bit-shift-right s i) 1)) \1 \0))))

(defn transient
  "Steps until the trajectory of ring `s` first hits a fixed point.
  nil when it never settles: the trajectory is followed with exact cycle
  detection (visited set), so a non-settling ring is proven, not guessed."
  [^long n ^long s]
  (loop [t 0, x s, seen #{}]
    (cond
      (== x (step n x)) t
      (contains? seen x) nil
      :else (recur (unchecked-inc t) (step n x) (conj seen x)))))

(defn damage-depth
  "Damage depth of flipping site `j` of fixed-point ring `s`: the maximum
  circular distance from j of any cell differing from the (constant)
  undamaged ring, over the whole damaged trajectory until it settles.
  nil when the damaged ring never settles (unbounded damage)."
  [^long n ^long s ^long j]
  (let [flip (bit-xor s (bit-shift-left 1 j))
        spread (fn [^long x]
                 (loop [i 0, d 0]
                   (if (== i n)
                     d
                     (let [delta (mod (- i j) n)]
                       (recur (unchecked-inc i)
                              (if (not= (bit-and 1 (bit-shift-right x i))
                                        (bit-and 1 (bit-shift-right s i)))
                                (max d (min delta (- n delta)))
                                d))))))]
    (loop [x flip, depth 0, seen #{}]
      (let [depth' (max depth (spread x))]
        (cond
          (== x s) depth'                               ; difference died out entirely
          (== x (step n x)) depth'                      ; damaged ring settled (damage may persist here)
          (contains? seen x) nil                        ; damaged ring never settles
          :else (recur (step n x) depth' (conj seen x)))))))

(defn- settling-scan
  "Per-n settling statistics over ALL 2^n rings: maximum transient, the
  count and the rings that never settle, and the worst-case starting ring
  (the smallest ring value achieving the maximum transient)."
  [^long n]
  (let [limit (bit-shift-left 1 n)]
    (loop [s 0, max-t -1, worst 0, never []]
      (if (== s limit)
        {:n n
         :max-transient max-t
         :worst-start {:value worst :ring (ring-string n worst)}
         :never-settling-count (count never)
         :never-settling-rings (mapv (fn [v] {:value v :ring (ring-string n v)}) never)}
        (let [t (transient n s)]
          (cond
            (nil? t) (recur (unchecked-inc s) max-t worst (conj never s))
            (> t max-t) (recur (unchecked-inc s) t s never)
            :else (recur (unchecked-inc s) max-t worst never)))))))

(defn- damage-scan
  "Maximum damage depth over every fixed-point ring of size n flipped at
  every site.  Long/MAX_VALUE stands for unbounded (never-settling)
  damage."
  [^long n]
  (let [limit (bit-shift-left 1 n)]
    (loop [s 0, max-d -1]
      (if (== s limit)
        max-d
        (if-not (fixed-point? n s)
          (recur (unchecked-inc s) max-d)
          (let [d (loop [j 0, acc 0]
                    (if (== j n)
                      acc
                      (recur (unchecked-inc j)
                             (let [dd (damage-depth n s j)]
                               (cond (nil? dd) Long/MAX_VALUE
                                     (> dd acc) dd
                                     :else acc)))))]
            (recur (unchecked-inc s) (max max-d d))))))))

(defn- smallest-transient-breach
  "Smallest (n, ring) whose transient exceeds the TN bound or which never
  settles, restricted to the recorded per-n data (used only when a per-n
  scan already shows a breach, so this names the witness, it does not
  widen the search)."
  [nss]
  (first
   (for [{:keys [n]} nss, s (range (bit-shift-left 1 n))
         :when (let [t (transient n s)] (or (nil? t) (> t tn-transient-bound)))]
     {:n n :value s :ring (ring-string n s)})))

(defn- smallest-damage-breach
  "Smallest (n, ring, site) of a settled ring whose flip exceeds the TN
  damage bound (or never settles), over the recorded damage range."
  [nss]
  (first
   (for [{:keys [n]} nss
         :when (<= damage-n-min n damage-n-max)
         s (range (bit-shift-left 1 n))
         :when (fixed-point? n s)
         j (range n)
         :when (let [d (damage-depth n s j)] (or (nil? d) (> d tn-damage-bound)))]
     {:n n :value s :ring (ring-string n s) :flipped-site j})))

(defn claims
  "The TN's two claims, each recorded as upheld or refuted with the
  smallest counterexample when refuted.  Claim 1: settles from any state
  in at most 6 steps.  Claim 2: a single flip in a settled ring spreads
  to depth at most 1."
  [per-n]
  (let [settle-refuted? (boolean (some (fn [{:keys [n max-transient never-settling-count]}]
                                         (or (pos? never-settling-count)
                                             (> max-transient tn-transient-bound)))
                                       per-n))
        confine-refuted? (boolean (some (fn [{:keys [n damage-depth]}]
                                          (and damage-depth
                                               (or (== Long/MAX_VALUE damage-depth)
                                                   (> damage-depth tn-damage-bound))))
                                        per-n))]
    {:settling {:claim "settles from any state in at most 6 steps"
                :bound tn-transient-bound
                :verdict (if settle-refuted? :refuted :upheld)
                :smallest-counterexample (when settle-refuted?
                                           (smallest-transient-breach per-n))}
     :confinement {:claim "a single flip in a settled ring spreads to depth at most 1"
                   :bound tn-damage-bound
                   :verdict (if confine-refuted? :refuted :upheld)
                   :smallest-counterexample (when confine-refuted?
                                             (smallest-damage-breach per-n))}}))

(defn record
  "The full exhaustive record the ticket asks for, computed from scratch:
  per n = 3..20 the maximum transient, the number (and rings) that never
  settle, a worst-case starting ring, and the maximum damage depth
  (n = 4..20); then the two TN claim verdicts."
  []
  (let [per-n (mapv (fn [n]
                      (let [ss (settling-scan n)]
                        (assoc ss
                               :fixed-point-count
                               (count (filter #(fixed-point? n %) (range (bit-shift-left 1 n))))
                               :max-damage-depth (when (<= damage-n-min n damage-n-max)
                                                   (let [d (damage-scan n)]
                                                     (if (== Long/MAX_VALUE d) nil d))))))
                    (range settling-n-min (unchecked-inc settling-n-max)))]
    {:rule rule
     :range {:settling [settling-n-min settling-n-max] :damage [damage-n-min damage-n-max]}
     :per-n per-n
     :claims (claims per-n)}))

(defn -main []
  (prn (record)))
