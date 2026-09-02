#!/usr/bin/env clojure
;; U4 -- does the ambiguity term DISCRIMINATE, or is its inertness a property
;; of something else? (SPEC-dormant-wiring.md U4; worklist row :U4, class :RUN.)
;;
;; THE QUESTION. aif-equations.edn :ambiguity is theory-defined (dacosta2020
;; eq. 45-48), Lean-closed, and carries a badge saying its influence was
;; "MEASURED 0% flips / 674 ticks" (R5-glossary-formalisation.md:29;
;; facts-R5.md:458). No row asks whether that zero is a property of the TERM
;; or of OUR A. SPEC U4 names the vehicle: on every recorded field, re-score
;; with the ambiguity term dropped and count argmax changes, typed three ways
;;   (a) changes exist -> the term discriminates, the old zero was field poverty
;;   (b) zero, H(P(o|s)) near-constant across candidates -> the inertness is in
;;       our A, and the row says so WITH THE VARIANCE
;;   (c) zero, C flat over the outcomes ambiguity distinguishes -> the
;;       deficiency is the channel-grain C, and the fix belongs to the
;;       outcome carrier, not to G.
;; An untyped zero is a failed acceptance.
;;
;; TWO GRAINS, because only one of them is checkable on every record.
;;
;;   GRAIN A (R5, the theory grain). arg-best over G-risk + G-ambiguity vs
;;   arg-best over G-risk alone. Both legs are recorded per candidate in every
;;   era, so this needs no reconstruction of the controller blend: it is
;;   exactly the question eq. 42 asks. Available on every tick.
;;
;;   GRAIN B (selection grain). The live rule is "first entry of the
;;   score-ordered list whose action type is not :no-op" (policy.clj:497-499),
;;   over :controller-score. Re-ordering by (score - G-ambiguity) is licensed
;;   only where the record proves ambiguity enters the score with coefficient
;;   1, i.e. where :G-core is recorded and G-core = G-risk + G-ambiguity
;;   (efe.clj:842-849). Records before that key exists are NOT measurable at
;;   this grain and are counted as such rather than reconstructed at guessed
;;   weights -- the legacy leg weights drifted inside the corpus and the
;;   4-leg identity at today's declared weights (efe.clj:83-84) leaves
;;   residuals up to 1.4 on 2026-06-10 records.
;;
;; TIES. Lower score = more preferred (efe.clj:904-919), so arg-best is an
;; argmin. Recorded candidate sets are full of exact ties, so "the index moved"
;; is not "the term discriminated": every count below is reported twice, once
;; as a change of the chosen ACTION and once STRICT -- the with-ambiguity
;; winner is strictly worse on the ambiguity-free score, so ambiguity
;; overturned a strict preference rather than breaking a tie.
;;
;; CANDIDATE CLASS. efe.clj:680-682 writes a DIFFERENT quantity into
;; :G-ambiguity for :learn-action-class candidates -- the capability zone's
;; Beta :predictive-variance, not E_Q(s|pi)[H(P(o|s))]. Every measurement is
;; therefore reported both over all candidates and over the entropy-carrying
;; subset alone.
;;
;; NO LIVE RUN, NO RUN LOCK. Every number is a replay of committed records.
;;
;; Usage: clojure -M holes/labs/wm-contract/u4_ambiguity_sweep.clj
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[futon2.aif.efe :as efe]
         '[futon2.aif.preferences])

(def src-ambiguity
  "efe.clj's own private ambiguity fn, for the reproduction control C2."
  @#'efe/ambiguity)

(def eps 1.0e-9)
(def strict-eps 1.0e-12)

;; ---------------------------------------------------------------------------
;; Fields

(defn archive-files []
  (->> (file-seq (io/file "data/wm-trace"))
       (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" (.getName %)))
       (map str)
       sort
       vec))

(def s2-path "holes/labs/wm-contract/runs/2026-09-01-s2/wm-trace-s2.edn")
(def s4-path "holes/labs/wm-contract/runs/2026-09-01-s4/wm-trace-s4.edn")

(defn read-records [path]
  (with-open [r (java.io.PushbackReader. (io/reader path))]
    (loop [out []]
      (let [form (edn/read {:eof ::eof
                            :default (fn [t v] {:trace/edn-tag t :trace/value v})}
                           r)]
        (if (= ::eof form) out (recur (conj out form)))))))

;; ---------------------------------------------------------------------------
;; helpers

(defn- arg-best
  "Index of the minimum of XS, first index on a tie (the tie rule `sort-by`
   gives the recorded ranking, which is stable)."
  [xs]
  (first (reduce (fn [[bi bv] [i v]] (if (< v bv) [i v] [bi bv]))
                 [0 (first xs)]
                 (map-indexed vector xs))))

(defn- order-by-score [xs]
  (vec (map first (sort-by second (map-indexed vector xs)))))

(defn- ranks-of [xs]
  (into {} (map-indexed (fn [r i] [i (inc r)]) (order-by-score xs))))

(defn- first-non-no-op
  "policy.clj:497-499 -- the live rule."
  [order types]
  (or (first (remove #(= :no-op (nth types %)) order)) (first order)))

(defn- sd [xs]
  (let [n (count xs)]
    (if (< n 2)
      0.0
      (let [m (/ (reduce + xs) (double n))]
        (Math/sqrt (/ (reduce + (map #(let [d (- % m)] (* d d))  xs)) (double n)))))))

(defn- spread [xs] (if (seq xs) (- (apply max xs) (apply min xs)) 0.0))

(defn- n-tied-at-min [xs]
  (let [m (apply min xs)] (count (filter #(<= (Math/abs (- % m)) strict-eps) xs))))

(defn- channel-entropy-legs
  "The per-channel additive decomposition of :gaussian-entropy ambiguity
   (efe.clj ambiguity-by-channel), or nil when the record carries no
   :prediction-variance."
  [c]
  (when-let [pv (:prediction-variance c)]
    (into {} (map (fn [[ch v]]
                    [ch (* 0.5 (Math/log (* 2.0 Math/PI Math/E (max (double v) 1e-9))))]))
          pv)))

;; ---------------------------------------------------------------------------
;; one tick

(defn lambda-break
  "The smallest scale factor lambda > 1 at which arg-best over
   (risk + lambda * ambiguity) stops being the recorded arg-best over
   (risk + ambiguity). The winner w loses to j exactly when
   lambda*(amb_j - amb_w) < risk_w - risk_j, and because w wins at lambda = 1
   only candidates with amb_j < amb_w can ever overtake, each at the threshold
   (risk_w - risk_j)/(amb_j - amb_w), which is >= 1 by that same fact. So this
   is 'how many times its own size would the ambiguity term have to be before
   it moved the winner' -- +Inf when no candidate can overtake at any scale.
   Reported because a bare 0 flips says nothing about how close the term came."
  [risk amb core]
  (let [w (arg-best core)
        aw (nth amb w) rw (nth risk w)
        ts (for [j (range (count amb))
                 :let [d (- (nth amb j) aw)]
                 :when (< d (- strict-eps))]
             (/ (- rw (nth risk j)) d))]
    (if (seq ts) (apply min ts) Double/POSITIVE_INFINITY)))

(defn- drop-term-measures
  "Compare arg-best under WITH against arg-best under WITHOUT, on the same
   candidate vector. Returns the action-change flag, the strict flag, and how
   much of the order moved."
  [with without actions]
  (let [i-with (arg-best with)
        i-without (arg-best without)
        o-with (order-by-score with)
        o-without (order-by-score without)
        r-with (ranks-of with)
        r-without (ranks-of without)]
    {:changed? (not= (nth actions i-with) (nth actions i-without))
     ;; STRICT: the with-term winner is strictly worse WITHOUT the term, so
     ;; the term overturned a strict preference instead of breaking a tie.
     :strict? (> (- (nth without i-with) (nth without i-without)) strict-eps)
     :ranks-moved (count (filter #(not= (r-with %) (r-without %))
                                 (range (count with))))
     :order-with o-with :order-without o-without}))

(defn tick-row
  "One record -> one measurement row, or nil when no candidate carries both
   :G-risk and :G-ambiguity as numbers."
  [record]
  (let [ranked (vec (:ranked-actions record))
        usable (filterv #(and (number? (:G-risk %)) (number? (:G-ambiguity %))) ranked)]
    (when (seq usable)
      (let [actions (mapv :action usable)
            types (mapv #(get-in % [:action :type]) usable)
            risk (mapv #(double (:G-risk %)) usable)
            amb (mapv #(double (:G-ambiguity %)) usable)
            core (mapv + risk amb)
            entropy-idx (vec (keep-indexed
                              (fn [i t] (when-not (= :learn-action-class t) i)) types))
            ;; --- GRAIN A, all candidates
            a (drop-term-measures core risk actions)
            ;; --- GRAIN A, entropy-carrying candidates only
            a-e (when (seq entropy-idx)
                  (drop-term-measures (mapv #(nth core %) entropy-idx)
                                      (mapv #(nth risk %) entropy-idx)
                                      (mapv #(nth actions %) entropy-idx)))
            ;; --- positive control: drop RISK instead of AMBIGUITY
            c4 (drop-term-measures core amb actions)
            ;; --- GRAIN B, only where :G-core licenses the subtraction
            g-core (mapv :G-core usable)
            cs (mapv :controller-score usable)
            b-ok? (and (every? number? g-core) (every? number? cs))
            c1-residual (when b-ok?
                          (apply max (map (fn [gc r a] (Math/abs (- (double gc) (+ r a))))
                                          g-core risk amb)))
            score (when b-ok? (mapv double cs))
            without (when b-ok? (mapv - score amb))
            b (when (and b-ok? (<= c1-residual eps))
                (let [o-with (order-by-score score)
                      o-without (order-by-score without)
                      sel-with (first-non-no-op o-with types)
                      sel-without (first-non-no-op o-without types)
                      r-with (ranks-of score)
                      r-without (ranks-of without)
                      ;; the ordering-independent bound: which candidates
                      ;; overtake the RECORDED decision's candidate?
                      d-action (get-in record [:decision :action])
                      d-idx (first (keep-indexed (fn [i x] (when (= x d-action) i)) actions))
                      overtakers (when d-idx
                                   (count (filter (fn [j]
                                                    (and (> (r-with j) (r-with d-idx))
                                                         (< (r-without j) (r-without d-idx))))
                                                  (range (count actions)))))]
                  {:changed? (not= (nth actions sel-with) (nth actions sel-without))
                   :strict? (> (- (nth without sel-with) (nth without sel-without)) strict-eps)
                   :ranks-moved (count (filter #(not= (r-with %) (r-without %))
                                               (range (count score))))
                   :decision-found? (some? d-idx)
                   :decision-reproduced? (and d-idx (= d-idx sel-with))
                   :decision-overtaken? (and overtakers (pos? overtakers))
                   :overtakers overtakers}))
            legs (keep channel-entropy-legs usable)
            legs-e (keep #(channel-entropy-legs (nth usable %)) entropy-idx)
            e-risk (mapv #(nth risk %) entropy-idx)
            e-amb (mapv #(nth amb %) entropy-idx)
            e-core (mapv + e-risk e-amb)
            lam (lambda-break e-risk e-amb e-core)
            ;; how big a risk difference did ambiguity overturn, when it did?
            overturned (when (and a-e (:changed? a-e))
                         (let [iw (arg-best e-core) ir (arg-best e-risk)]
                           {:risk-gap (- (nth e-risk iw) (nth e-risk ir))
                            :amb-gap (- (nth e-amb ir) (nth e-amb iw))}))]
        {:n (count usable)
         :n-entropy (count entropy-idx)
         :n-learn (- (count usable) (count entropy-idx))
         :mode (or (:ambiguity-mode (first usable)) :unrecorded)
         :timestamp (:timestamp record)
         :a a :a-e a-e :c4 c4 :b b
         :b-measurable? (boolean b)
         :c1-residual c1-residual
         :risk-tied-at-min (n-tied-at-min risk)
         ;; M4 -- the variance the type-(b) reading needs, split by class
         :amb-sd (sd amb) :amb-range (spread amb) :amb-distinct (count (distinct amb))
         :amb-sd-e (if (seq entropy-idx) (sd (mapv #(nth amb %) entropy-idx)) 0.0)
         :amb-range-e (if (seq entropy-idx) (spread (mapv #(nth amb %) entropy-idx)) 0.0)
         :amb-distinct-e (if (seq entropy-idx)
                           (count (distinct (mapv #(nth amb %) entropy-idx))) 0)
         :risk-range (spread risk)
         :risk-range-e (if (seq entropy-idx) (spread (mapv #(nth risk %) entropy-idx)) 0.0)
         :lambda-break lam
         :overturned overturned
         :observation (:observation record)
         ;; channel decomposition over entropy-carrying candidates only
         :channel-spread
         (when (seq legs-e)
           (into {} (map (fn [ch]
                           [ch (let [vs (mapv #(get % ch 0.0) legs-e)]
                                 {:range (spread vs) :distinct (count (distinct vs))})]))
                 (keys (first legs-e))))
         :channel-spread-all
         (when (seq legs)
           (into {} (map (fn [ch]
                           [ch (let [vs (mapv #(get % ch 0.0) legs)]
                                 {:range (spread vs) :distinct (count (distinct vs))})]))
                 (keys (first legs))))}))))

;; ---------------------------------------------------------------------------
;; aggregation

(defn- pct [a b] (if (zero? b) Double/NaN (* 100.0 (/ (double a) (double b)))))

(defn print-block [label rows]
  (let [n (count rows)
        brows (filterv :b-measurable? rows)
        cnt (fn [f coll] (count (filter f coll)))]
    (println (format "%-26s ticks %5d   candidates %7d  (entropy-carrying %7d, learn-action-class %6d)"
                     label n
                     (reduce + (map :n rows)) (reduce + (map :n-entropy rows))
                     (reduce + (map :n-learn rows))))
    (println (format "  A   arg-best(risk+amb) vs arg-best(risk), ALL candidates      changed %5d / %-5d (%.2f%%)   STRICT %5d (%.2f%%)   ranks moved %d"
                     (cnt #(:changed? (:a %)) rows) n (pct (cnt #(:changed? (:a %)) rows) n)
                     (cnt #(:strict? (:a %)) rows) (pct (cnt #(:strict? (:a %)) rows) n)
                     (reduce + (map #(:ranks-moved (:a %)) rows))))
    (let [er (filterv :a-e rows)]
      (println (format "  A'  same, ENTROPY-CARRYING candidates only                   changed %5d / %-5d (%.2f%%)   STRICT %5d (%.2f%%)   ranks moved %d"
                       (cnt #(:changed? (:a-e %)) er) (count er)
                       (pct (cnt #(:changed? (:a-e %)) er) (count er))
                       (cnt #(:strict? (:a-e %)) er)
                       (pct (cnt #(:strict? (:a-e %)) er) (count er))
                       (reduce + (map #(:ranks-moved (:a-e %)) er)))))
    (println (format "  C4  POSITIVE CONTROL, drop RISK instead of ambiguity        changed %5d / %-5d (%.2f%%)   STRICT %5d (%.2f%%)"
                     (cnt #(:changed? (:c4 %)) rows) n (pct (cnt #(:changed? (:c4 %)) rows) n)
                     (cnt #(:strict? (:c4 %)) rows) (pct (cnt #(:strict? (:c4 %)) rows) n)))
    (if (seq brows)
      (do
        (println (format "  B   live rule (first non-:no-op) with amb dropped            changed %5d / %-5d (%.2f%%)   STRICT %5d   ranks moved %d"
                         (cnt #(:changed? (:b %)) brows) (count brows)
                         (pct (cnt #(:changed? (:b %)) brows) (count brows))
                         (cnt #(:strict? (:b %)) brows)
                         (reduce + (map #(:ranks-moved (:b %)) brows))))
        (println (format "  B'  recorded :decision candidate OVERTAKEN (upper bound)     %5d / %-5d   [decision located on %d ticks; live rule reproduces it on %d]"
                         (cnt #(:decision-overtaken? (:b %)) brows) (count brows)
                         (cnt #(:decision-found? (:b %)) brows)
                         (cnt #(:decision-reproduced? (:b %)) brows)))
        (println (format "  C1  max |G-core - (G-risk + G-ambiguity)| over those ticks  %.3e" 
                         (apply max (map :c1-residual brows)))))
      (println "  B   NOT MEASURABLE on this group: no record carries :G-core beside :controller-score"))
    (println (format "  B?  ticks measurable at grain B                              %5d / %-5d" (count brows) n))
    (println (format "  M4  ambiguity within-tick, ALL cands:      max sd %.6g  max range %.6g  max distinct %d  constant on %d/%d ticks"
                     (apply max (map :amb-sd rows)) (apply max (map :amb-range rows))
                     (apply max (map :amb-distinct rows))
                     (cnt #(zero? (:amb-range %)) rows) n))
    (println (format "      ambiguity within-tick, ENTROPY only:   max sd %.6g  max range %.6g  max distinct %d  constant on %d/%d ticks"
                     (apply max (map :amb-sd-e rows)) (apply max (map :amb-range-e rows))
                     (apply max (map :amb-distinct-e rows))
                     (cnt #(zero? (:amb-range-e %)) rows) n))
    (println (format "      risk within-tick range: min %.6g  max %.6g   |   ticks whose risk argmin is TIED %d / %d"
                     (apply min (map :risk-range rows)) (apply max (map :risk-range rows))
                     (cnt #(> (:risk-tied-at-min %) 1) rows) n))
    (let [ls (remove #(Double/isInfinite %) (map :lambda-break rows))
          inf (count (filter #(Double/isInfinite %) (map :lambda-break rows)))
          ov (keep :overturned rows)]
      (println "  M6  HEADROOM lambda_break (scale ambiguity by this to move the entropy-set winner):")
      (println (format "      min %s   median %s   ticks where NO scale can move it %d / %d"
                       (if (seq ls) (format "%.4f" (apply min ls)) "n/a")
                       (if (seq ls) (format "%.4f" (nth (vec (sort ls)) (quot (count ls) 2))) "n/a")
                       inf n))
      (when (seq ov)
        (println (format "  M7  when ambiguity DID move the winner: risk gap it overturned  min %.6g  max %.6g  median %.6g"
                         (apply min (map :risk-gap ov)) (apply max (map :risk-gap ov))
                         (nth (vec (sort (map :risk-gap ov))) (quot (count ov) 2))))))))

(defn -main [& _]
  (println ";; U4 -- ambiguity discrimination sweep. Replay of committed records only;")
  (println ";; no live run, no run lock, nothing written under data/.")
  (println ";; Lower score = more preferred (efe.clj:904-919), so arg-best = argmin.")
  (println ";; STRICT = the with-term winner is strictly worse without the term, so the")
  (println ";; term overturned a strict preference rather than breaking an exact tie.")
  (println)

  (let [archive (archive-files)
        per-file (into {} (map (fn [p]
                                 (let [d (subs (.getName (io/file p)) 9 19)]
                                   [p {:records (count (read-records p))
                                       :rows (mapv #(assoc % :date d)
                                                   (keep tick-row (read-records p)))}])))
                       archive)
        s2 (vec (keep tick-row (read-records s2-path)))
        s4 (vec (keep tick-row (read-records s4-path)))
        arch-rows (vec (mapcat #(:rows (per-file %)) archive))
        all-rows (vec (concat arch-rows s2 s4))]

    (println ";; ---------------------------------------------------------------")
    (println ";; FIELDS AND DENOMINATORS")
    (println (format ";;   ARCHIVE  data/wm-trace/wm-trace-YYYY-MM-DD.edn, %d files, %d records, %d usable ticks"
                     (count archive)
                     (reduce + (map #(:records (per-file %)) archive))
                     (count arch-rows)))
    (println (format ";;   S2       %s -- %d ticks" s2-path (count s2)))
    (println ";;   S3       NOT FOUND. runs/2026-09-01-s3/ holds ARMS.txt and README.md only;")
    (println ";;            its README's first line says it is a REPLAY, not a 20-tick stage")
    (println ";;            run. S3 contributes 0 ticks (the same absence C486 §3 recorded).")
    (println (format ";;   S4       %s -- %d ticks" s4-path (count s4)))
    (let [a-ts (set (map :timestamp arch-rows))]
      (println (format ";;   OVERLAP: S2 timestamps also present in ARCHIVE %d/%d; S4 %d/%d -- S2 and S4 are"
                       (count (filter a-ts (map :timestamp s2))) (count s2)
                       (count (filter a-ts (map :timestamp s4))) (count s4)))
      (println ";;            copies of ticks the per-date archive already holds, so ALL FIELDS"))
    (println ";;            double-counts them and the per-field rows are the honest denominators.")
    (println)

    (println ";; ---------------------------------------------------------------")
    (println ";; C2  SRC REPRODUCTION -- recompute :G-ambiguity from the record's own")
    (println ";;     :prediction-variance with efe.clj's OWN ambiguity fn, split by class.")
    (let [rows (for [p (concat archive [s2-path s4-path])
                     r (read-records p)
                     c (:ranked-actions r)
                     :when (and (:prediction-variance c)
                                (= :gaussian-entropy (:ambiguity-mode c))
                                (number? (:G-ambiguity c)))]
                 [(= :learn-action-class (get-in c [:action :type]))
                  (Math/abs (- (double (:G-ambiguity c))
                               (double (src-ambiguity (:prediction-variance c)
                                                      :gaussian-entropy))))
                  (double (:G-ambiguity c))])
          by (group-by first rows)]
      (doseq [[learn? xs] (sort-by key by)]
        (println (format ";;     %-26s candidates %6d   max |delta| %.4e   recorded values %s"
                         (if learn? ":learn-action-class" "entropy-carrying (other)")
                         (count xs) (apply max (map second xs))
                         (pr-str (sort (distinct (map #(nth % 2) xs))))))))
    (println ";;     efe.clj:680-682 is why: for :learn-action-class, g-ambig is")
    (println ";;     (:predictive-variance zone-evidence) -- the capability zone's Beta")
    (println ";;     predictive variance -- and ambiguity-terms is {} (efe.clj:678-679).")
    (println)

    (println ";; ---------------------------------------------------------------")
    (println ";; MEASUREMENTS")
    (println ";; The era boundary below is 2026-07-03, the first archived date whose")
    (println ";; recorded :G-ambiguity is on the gaussian-entropy scale (per-file table:")
    (println ";; max entropy-only range 0.03 on 2026-07-02, 16.4647 on 2026-07-03). The")
    (println ";; :ambiguity-mode KEY only appears from 2026-07-14, so splitting on the key")
    (println ";; would misfile 2026-07-03..07-09.")
    (println)
    (doseq [[label rows] [["ARCHIVE (all)" arch-rows]
                          ["ARCHIVE .. 2026-07-02" (filterv #(neg? (compare (:date %) "2026-07-03")) arch-rows)]
                          ["ARCHIVE 2026-07-03 .." (filterv #(not (neg? (compare (:date %) "2026-07-03"))) arch-rows)]
                          ["ARCHIVE 2026-09-01 only" (filterv #(= "2026-09-01" (:date %)) arch-rows)]
                          ["S2" s2]
                          ["S4" s4]
                          ["ALL FIELDS (double-counts)" all-rows]]]
      (when (seq rows) (print-block label rows) (println)))

    (println ";; ---------------------------------------------------------------")
    (println ";; ARCHIVE per file: ticks | A changed | A STRICT | A' STRICT | B changed | C4 STRICT | max amb range (entropy only)")
    (doseq [p archive]
      (let [rows (:rows (per-file p))]
        (when (seq rows)
          (println (format ";;   %-24s %4d %5d %5d %5d %5s %5d   %.6g"
                           (.getName (io/file p)) (count rows)
                           (count (filter #(:changed? (:a %)) rows))
                           (count (filter #(:strict? (:a %)) rows))
                           (count (filter #(:strict? (:a-e %)) rows))
                           (if (some :b-measurable? rows)
                             (str (count (filter #(:changed? (:b %)) rows))) "n/a")
                           (count (filter #(:strict? (:c4 %)) rows))
                           (apply max (map :amb-range-e rows)))))))
    (println)

    (println ";; ---------------------------------------------------------------")
    (println ";; TYPE-(c) TEST -- is C flat over the outcomes ambiguity distinguishes?")
    (println ";; C is the channel-grain [lo hi] floor at preferences.clj:9-23, read")
    (println ";; through pref/current-C (preferences.clj:60-71). For each channel:")
    (println ";; whether C names it, and how much the OBSERVATION moved across the")
    (println ";; ticks of each field -- the second half is what an outcome carrier")
    (println ";; would have to vary for ambiguity's distinctions to be about anything.")
    (let [C (futon2.aif.preferences/current-C)]
      (doseq [[flabel rows] [["ARCHIVE" arch-rows] ["S2" s2] ["S4" s4]]]
        (let [obs (keep :observation rows)
              chans (sort (distinct (mapcat keys (map #(select-keys % (keys (first obs))) obs))))]
          (println (format ";;   -- %s (%d ticks with an :observation)" flabel (count obs)))
          (doseq [ch chans]
            (let [vs (keep #(let [v (get % ch)] (when (number? v) (double v))) obs)]
              (println (format ";;      %-22s C %-14s observed distinct %4d  range %.6g"
                               (name ch)
                               (if-let [r (get C ch)] (pr-str r) "ABSENT")
                               (count (distinct vs))
                               (if (seq vs) (spread vs) 0.0))))))))
    (println)

    (println ";; ---------------------------------------------------------------")
    (println ";; CHANNEL DECOMPOSITION -- ambiguity = SUM_ch 1/2 ln(2 pi e sigma^2_ch).")
    (println ";; Over ENTROPY-CARRYING candidates only. Per channel: on how many ticks")
    (println ";; does that channel's contribution differ across candidates at all?")
    (let [rows (filterv :channel-spread all-rows)]
      (println (format ";;   decomposable ticks (records carrying :prediction-variance): %d" (count rows)))
      (doseq [ch (sort (keys (:channel-spread (first rows))))]
        (let [xs (map #(get-in % [:channel-spread ch]) rows)]
          (println (format ";;   %-22s varies on %4d / %-4d ticks   max spread %.10g   max distinct %d"
                           (name ch) (count (filter #(pos? (:range %)) xs)) (count rows)
                           (apply max (map :range xs)) (apply max (map :distinct xs)))))))
    (println)
    (println ";; end")))

(-main)
