;; CASCADE * ARM — what is the multiplication rule, actually?
;;
;; Joe, 2026-07-17: "we haven't yet tried composing the cascade with a propagator
;; — e.g. cascade*rotate+2 and cascade*rotate+1 ... once we figure out what the
;; multiplication rule actually is."
;;
;; check_tokamak_cascade.clj already has A rule: g[s(bin)] := nu(g[bin]), with
;; nu : ACTS -> ACTS. But `cascade*rotate+2` cannot use it as written, because
;; rotate+2 is an ELEMENT of ACTS, not a map on ACTS. That is a type error, and
;; the repair is the interesting part:
;;
;;   every arm is a PERMUTATION of the 8 neighbourhood strings
;;   (generator.clj:729 — a neighbourhood sigma is a map nbhd -> nbhd).
;;   So arms compose, and an arm `a` induces a map on arms:
;;
;;       nu_a(sigma) = a . sigma          (left multiplication)
;;
;; That is the regular representation of S_8 acting on itself. Which makes a
;; prediction BEFORE we run anything (Cayley): sigma |-> a.sigma has a fixed
;; point iff a = identity. So EVERY cascade*a is fixed-point-free except
;; cascade*identity. settles_if_nu_has_fixed_point then says: every cascade*a is
;; alive, and cascade*identity settles instantly. Checked below, not assumed.
;;
;; Run: bb check_cascade_times_arm.clj

(require '[clojure.edn :as edn] '[clojure.string :as str] '[clojure.set :as set])

(def C (edn/read-string (slurp "cascade-tokamak-v0.edn")))
(def BINS (get-in C [:cascade/carrier :N :elements]))
(def g0 (:cascade/policy C))

;; The arms, as POSITIONAL sigmas, verbatim from futon5 tokamak_advantage.clj:72-78
;; (also cascade_run.clj:28-33). Positional form composes the same way as the
;; neighbourhood form — the elisp-table relabelling is a conjugation applied to
;; both sides — so we compose here and note the caveat below.
(def positional
  {:rotate+2   [2 3 4 5 6 7 0 1]
   :three+five [1 2 0 4 5 6 7 3]
   :sigma-5127 [5 1 2 7 6 0 4 3]
   :rotate+1   [1 2 3 4 5 6 7 0]
   :identity   [0 1 2 3 4 5 6 7]})
(def ACTS (vec (keys positional)))

(defn compose [a b] (mapv #(nth a %) b))          ; (a.b)(k) = a(b(k))
(defn perm? [p] (= (set p) (set (range 8))))
(def name-of (into {} (map (fn [[k v]] [v k]) positional)))
(defn pname [p] (or (name-of p) (str "sigma" (str/join p))))

(println "=== sanity: every arm is a permutation of 0..7")
(doseq [[k v] positional] (println (format "    %-12s %s  %s" k (pr-str v) (if (perm? v) "perm" "NOT A PERM"))))

;; ---- order of each arm -----------------------------------------------------
(defn ord [p] (loop [q p n 1] (if (= q (positional :identity)) n (recur (compose p q) (inc n)))))
(println "\n=== order of each arm (how long cascade*a takes to return)")
(doseq [a ACTS] (println (format "    %-12s order %d" a (ord (positional a)))))

;; ---- THE CAYLEY PREDICTION -------------------------------------------------
(println "\n=== nu_a(sigma) = a.sigma  — fixed-point-free?")
(println "    PREDICTED (Cayley): fixed-point-free for every a /= identity.")
(doseq [a ACTS]
  (let [pa (positional a)
        fixed (filter (fn [b] (= (compose pa (positional b)) (positional b))) ACTS)]
    (println (format "    cascade*%-12s fixed arms: %-28s %s"
                     a (pr-str (vec fixed))
                     (if (empty? fixed) "FIXED-POINT-FREE -> alive"
                         "HAS FIXED POINT -> settles for EVERY s -> DEAD")))))

;; ---- CLOSURE: does cascade*a stay in the vocabulary? -----------------------
(println "\n=== does cascade*a stay inside P = the five named arms?")
(doseq [a ACTS]
  (let [imgs (map #(compose (positional a) (positional %)) ACTS)
        in-P (filter name-of imgs)]
    (println (format "    cascade*%-12s %d/5 images are named arms  %s"
                     a (count in-P)
                     (if (= 5 (count in-P)) "CLOSED" "<- LEAVES THE VOCABULARY")))))

;; ---- the two readings of "cascade * a" -------------------------------------
;; (B) pointwise:  g'(bin) = a . g(bin)                      [s = identity]
;; (A) propagator: g'[s(bin)] = a . g(bin)                   [s = emacsShift]
;; (B) is (A)'s s=identity case. identity is SURJECTIVE, so FREE = empty:
;; pointwise multiplication is EXACTLY the case free_nonempty_iff_not_surjective
;; calls scaffold-less. The shape map is what makes a bin unwritable.
(def order-bins (vec BINS))
(def s-shift (zipmap order-bins (cons (first order-bins) (butlast order-bins))))
(def s-id    (zipmap order-bins order-bins))
(defn FREE [s] (vec (remove (set (vals s)) BINS)))

(println "\n=== the two readings, and what separates them")
(println (format "    (B) pointwise   s = identity    FREE = %s" (pr-str (FREE s-id))))
(println (format "    (A) propagator  s = emacsShift  FREE = %s" (pr-str (FREE s-shift))))
(println "    ^ pointwise has NO scaffold. free_nonempty_iff_not_surjective is")
(println "      not decoration here: it is the difference between the two rules.")

;; ---- run both, for rotate+2 and rotate+1 -----------------------------------
(defn step [s a g]
  (reduce (fn [acc b] (assoc acc (s b) (compose (positional a) (g b)))) g BINS))

(defn show-orbit [label s a]
  (println (format "\n=== %s" label))
  (println (format "    %-4s %s" "t" (str/join "  " (map #(format "%-14s" (name %)) BINS))))
  (loop [g (into {} (map (fn [[b arm]] [b (positional arm)]) g0)) seen {} t 0]
    (let [row (mapv g BINS)]
      (if-let [prev (seen row)]
        (println (format "    t=%-2d %s  <- CYCLE t=%d, PERIOD %d" t
                         (str/join "  " (map #(format "%-14s" (str (pname %))) row)) prev (- t prev)))
        (do (println (format "    t=%-2d %s" t (str/join "  " (map #(format "%-14s" (str (pname %))) row))))
            (when (< t 10) (recur (step s a g) (assoc seen row t) (inc t))))))))

(show-orbit "cascade*rotate+2, POINTWISE (s = identity)" s-id :rotate+2)
(show-orbit "cascade*rotate+1, POINTWISE (s = identity)" s-id :rotate+1)
(show-orbit "cascade*rotate+2, PROPAGATOR (s = emacsShift)" s-shift :rotate+2)
(show-orbit "cascade*rotate+1, PROPAGATOR (s = emacsShift)" s-shift :rotate+1)
(show-orbit "cascade*identity, POINTWISE — the dead one" s-id :identity)

(println "\n=== CAVEAT, stated because it is load-bearing")
(println "    Composed in POSITIONAL form. The engine runs NEIGHBOURHOOD sigmas")
(println "    (generator.clj:729 maps positional -> neighbourhood THROUGH the elisp")
(println "    table). That relabelling is a bijection applied to both operands, so")
(println "    it commutes with composition — but nothing above CHECKS that, and the")
(println "    docstring says passing the wrong table is 'the silent-port bug this")
(println "    function exists to prevent'. Before any of these orbits is RUN on the")
(println "    tokamak, compose in neighbourhood form and confirm the two agree.")
