;; RUN8 / stage S3 arms: rank and argmax movement, tau_live against tau = beta.
;;
;;   clojure -M:test holes/labs/wm-contract/run8_tau_arms.clj [out.txt]
;;
;; WHAT THIS MEASURES. RUN8 :acceptance asks for rank and argmax movement
;; against the S1 field. The two arms differ ONLY in the temperature law:
;;
;;   live   tau_eff = 1/g, g = :selection-gain off the record (S1/S1b/S2 all
;;          ran :selection-gain-only with g = 1.0, so tau_live = 1.0)
;;   S3     tau_eff = beta, the friston2017 eq. 2.7 root
;;
;; and it is run over the RECORDED field of each stage -- the same G vector and
;; the same ln E vector the tick actually selected on, read from the decision's
;; own :controller-ranking rather than recomputed from anything.
;;
;; THE BETA SERIES IS TRANSPLANTED ONTO S1 AND S1b, AND THAT IS STATED RATHER
;; THAN HIDDEN. Only S2 solved a beta per tick (RUN7 turned the carry on), so
;; the S1 and S1b arms take S2's 20 betas tick-for-tick by index. That makes the
;; S1 arm "the S1 field under the temperatures S2 measured", NOT "the beta S1
;; would have solved" -- S1 persisted no F_pi readback, so the second is not
;; recoverable from its records at all. The S2 arm is the native pairing, where
;; each tick's beta was solved on that tick's own field, and it is the one to
;; read if the two disagree.
;;
;; THE CONTROL COMES FIRST. Before any arm is reported, the script recomputes
;; the recorded selection at tau_live and checks it against what the record
;; says: the -G/tau scores in :controller-ranking, the order of
;; :habit-adjusted-ranking, and the posterior in :softmax-weights-by-candidate-id.
;; If those do not reproduce, the arm below them is measuring the wrong thing
;; and the script says so instead of printing numbers.
;;
;; It uses futon2.aif.policy's OWN softmax-weights and effective-temperature,
;; not a re-implementation, so what is measured is the code that shipped.
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[futon2.aif.policy :as policy])

(def out-path (or (first *command-line-args*)
                  "holes/labs/wm-contract/runs/2026-09-01-s3/ARMS.txt"))

(def fields
  [["S1"  "holes/labs/wm-contract/runs/2026-09-01/wm-trace-2026-09-01.edn"]
   ["S1b" "holes/labs/wm-contract/runs/2026-09-01-s1b/wm-trace-s1b.edn"]
   ["S2"  "holes/labs/wm-contract/runs/2026-09-01-s2/wm-trace-s2.edn"]])

(defn read-records [path]
  (with-open [r (java.io.PushbackReader. (io/reader path))]
    (doall (take-while #(not= ::eof %)
                       (repeatedly #(edn/read {:eof ::eof :default (fn [_ v] v)} r))))))

(defn tick-field
  "The selection field as the record has it: G and ln E in rank order, plus the
   temperature and the recorded orderings to check against."
  [t]
  (let [d (:decision t)
        cr (vec (sort-by :rank (:controller-ranking d)))]
    {:tau-live (some-> (:tau d) double)
     :selection-gain (some-> (:selection-gain d) double)
     :beta (some-> (get-in t [:policy-precision-state :beta]) double)
     :beta-source (get-in t [:policy-precision-state :beta-source])
     :gs (mapv #(double (:controller-score %)) cr)
     :lps (mapv #(double (or (:habit-prior-bias %) 0.0)) cr)
     :recorded-selection-scores (mapv #(double (:selection-score %)) cr)
     ;; NB :rank in :habit-adjusted-ranking is the entry's NEW position, not the
     ;; candidate's index (policy/ranking-entry takes the position), so the
     ;; identity to compare an order against is the ACTION, not the rank.
     :actions (mapv :action cr)
     :recorded-habit-actions (mapv :action (:habit-adjusted-ranking d))
     :recorded-habit-scores (mapv #(double (:selection-score %))
                                  (:habit-adjusted-ranking d))
     :recorded-softmax (:softmax-weights-by-candidate-id d)
     :recorded-action-target (get-in d [:action :target])
     :controller-rank1-target (get-in (first cr) [:action :target])}))

(defn habit-scores [gs lps tau]
  (mapv (fn [g lp] (+ (/ (- g) tau) lp)) gs lps))

(defn order-by-score
  "Indices sorted by DESCENDING score -- the same comparator
   strategic-recommendation uses for :habit-adjusted-ranking."
  [scores]
  (vec (sort-by (fn [i] (- (nth scores i))) (range (count scores)))))

(defn argmax-idx [scores]
  (first (apply max-key second (map-indexed vector scores))))

(defn entropy [ps]
  (- (reduce + (map (fn [p] (if (pos? p) (* p (Math/log p)) 0.0)) ps))))

(defn tv-distance [a b]
  (* 0.5 (reduce + (map (fn [x y] (Math/abs (- x y))) a b))))

(defn max-abs-delta [a b]
  (reduce max 0.0 (map (fn [x y] (Math/abs (- (double x) (double y)))) a b)))

(defn control
  "Reproduce the recorded selection at tau_live. Returns a map of deltas; all
   must be 0 (or the softmax check must be :key-shape-mismatch, reported)."
  [{:keys [gs lps tau-live recorded-selection-scores actions
           recorded-habit-actions recorded-habit-scores recorded-softmax]}]
  (let [scores (mapv #(/ (- %) tau-live) gs)
        habit (habit-scores gs lps tau-live)
        order (order-by-score habit)
        replay-habit-actions (mapv #(nth actions %) order)
        replay-habit-scores (mapv #(nth habit %) order)
        w (vec (policy/softmax-weights gs tau-live lps))
        recorded-w (when (map? recorded-softmax)
                     (let [ks (mapv #(str "rank/" (inc %)) (range (count gs)))]
                       (when (every? #(contains? recorded-softmax %) ks)
                         (mapv #(double (get recorded-softmax %)) ks))))]
    {:selection-score-delta (max-abs-delta scores recorded-selection-scores)
     :habit-order-match? (= replay-habit-actions recorded-habit-actions)
     :habit-score-delta (max-abs-delta replay-habit-scores recorded-habit-scores)
     :softmax-delta (if recorded-w (max-abs-delta w recorded-w) :key-shape-mismatch)}))

(defn arm [{:keys [gs lps tau-live] :as f} beta]
  (let [tau-s3 (policy/effective-temperature
                gs 1.0 {:tau-mode :variational-beta-gamma :variational-beta beta})
        ;; controller ranking: argmin G, no ln E, no tau
        ctl-live (vec (sort-by #(nth gs %) (range (count gs))))
        ctl-s3 ctl-live
        h-live (habit-scores gs lps tau-live)
        h-s3 (habit-scores gs lps tau-s3)
        o-live (order-by-score h-live)
        o-s3 (order-by-score h-s3)
        pos-live (into {} (map-indexed (fn [r i] [i r]) o-live))
        pos-s3 (into {} (map-indexed (fn [r i] [i r]) o-s3))
        moves (filterv (fn [i] (not= (pos-live i) (pos-s3 i))) (range (count gs)))
        max-move (reduce max 0 (map (fn [i] (Math/abs (- (pos-live i) (pos-s3 i))))
                                    (range (count gs))))
        w-live (vec (policy/softmax-weights gs tau-live lps))
        w-s3 (vec (policy/softmax-weights gs tau-s3 lps))]
    {:tau-live tau-live
     :tau-s3 tau-s3
     :n (count gs)
     ;; (1) the controller ranking -- argmin G, tau-invariant by construction
     :controller-rank-moves (count (filterv (fn [i] (not= (nth ctl-live i) (nth ctl-s3 i)))
                                            (range (count gs))))
     ;; (2) the habit-adjusted (ln E - G/tau) ordering -- NOT tau-invariant
     :habit-rank-moves (count moves)
     :habit-max-move max-move
     :habit-argmax-live (first o-live)
     :habit-argmax-s3 (first o-s3)
     :habit-argmax-moved? (not= (first o-live) (first o-s3))
     ;; (3) the softmax posterior
     :softmax-argmax-moved? (not= (argmax-idx w-live) (argmax-idx w-s3))
     :softmax-tv (tv-distance w-live w-s3)
     :softmax-entropy-live (entropy w-live)
     :softmax-entropy-s3 (entropy w-s3)
     ;; WHY a zero, rather than a bare zero. Ordering is by
     ;; score = ln E - G/tau, so raising tau adds +G(1/tau_live - 1/tau_s3) to
     ;; every score -- MONOTONE in G, so within a group of candidates sharing an
     ;; ln E value the order cannot change at all. A pair can only cross the
     ;; boundary between two ln E groups, and only if its G gap sits within the
     ;; perturbation of the group gap. These two numbers say how close the field
     ;; came: the smallest adjacent margin in the live order evaluated at
     ;; tau_s3 (negative would BE a flip), and how many distinct ln E values
     ;; there are to cross between.
     :ln-e-distinct (count (distinct lps))
     :ln-e-spread (- (apply max lps) (apply min lps))
     :min-adjacent-margin-live (reduce min Double/MAX_VALUE
                                       (map (fn [a b] (- (nth h-live a) (nth h-live b)))
                                            o-live (rest o-live)))
     :min-adjacent-margin-s3 (reduce min Double/MAX_VALUE
                                     (map (fn [a b] (- (nth h-s3 a) (nth h-s3 b)))
                                          o-live (rest o-live)))
     :min-cross-group-margin-s3
     (reduce min Double/MAX_VALUE
             (keep (fn [[a b]] (when (not= (nth lps a) (nth lps b))
                                 (- (nth h-s3 a) (nth h-s3 b))))
                   (map vector o-live (rest o-live))))
     ;; (4) what the record's :action actually was
     :action-is-controller-rank1? (= (:recorded-action-target f)
                                     (:controller-rank1-target f))}))

(defn fmt [x] (if (number? x) (format "%.10f" (double x)) (str x)))

(def sb (StringBuilder.))
(defn emit [& xs] (let [s (str/join " " xs)] (println s) (.append sb (str s "\n"))))

(emit "RUN8 / stage S3 arms -- rank and argmax movement, tau_live vs tau = beta")
(emit (str "generated " (java.time.Instant/now)))
(emit "")

;; the beta series: S2 is the only run that solved one
(def s2-records (read-records (second (nth fields 2))))
(def beta-series
  (mapv #(double (get-in % [:policy-precision-state :beta])) s2-records))
(def beta-sources
  (mapv #(get-in % [:policy-precision-state :beta-source]) s2-records))
(emit (format "beta series (S2, %d ticks): %s ... %s"
              (count beta-series) (fmt (first beta-series)) (fmt (last beta-series))))
(emit (format "beta sources: %s" (pr-str (frequencies beta-sources))))
(emit "")

(def all-ok (atom true))

(doseq [[label path] fields]
  (emit (str "=== " label " -- " path))
  (let [records (read-records path)
        fs (mapv tick-field records)]
    (emit (format "%d ticks; tau_live %s (selection-gain %s); native beta: %s"
                  (count fs)
                  (pr-str (distinct (map :tau-live fs)))
                  (pr-str (distinct (map :selection-gain fs)))
                  (pr-str (frequencies (map :beta-source fs)))))
    ;; --- control ---
    (let [cs (mapv control fs)
          worst-score (reduce max (map :selection-score-delta cs))
          orders-ok (every? :habit-order-match? cs)
          smx (map :softmax-delta cs)
          worst-smx (if (every? number? smx) (reduce max smx) (first (remove number? smx)))]
      (emit (format "CONTROL replay at tau_live: max |-G/tau delta| %s; habit order reproduced %s/%d (max |habit score delta| %s); max |softmax delta| %s"
                    (fmt worst-score) (count (filter :habit-order-match? cs)) (count cs)
                    (fmt (reduce max (map :habit-score-delta cs)))
                    (fmt worst-smx)))
      (when-not (and (zero? worst-score) orders-ok
                     (or (not (number? worst-smx)) (< (double worst-smx) 1e-12)))
        (reset! all-ok false)
        (emit "CONTROL FAILED -- the arm below is not measuring the recorded selection")))
    ;; --- arms ---
    (let [arms (mapv (fn [f i]
                       (arm f (or (:beta f) (nth beta-series (min i (dec (count beta-series)))))))
                     fs (range))
          native? (every? :beta fs)]
      (emit (format "beta source for this field: %s"
                    (if native? "NATIVE (each tick's own solve)"
                        "TRANSPLANTED from S2 by tick index")))
      (emit (format "tau_s3 range: %s .. %s"
                    (fmt (reduce min (map :tau-s3 arms))) (fmt (reduce max (map :tau-s3 arms)))))
      (emit (format "CONTROLLER rank moves (argmin G): %d over %d ticks x %d candidates"
                    (reduce + (map :controller-rank-moves arms)) (count arms)
                    (:n (first arms))))
      (emit (format "HABIT-ADJUSTED rank moves: total %d, ticks with any %d/%d, max displacement %d"
                    (reduce + (map :habit-rank-moves arms))
                    (count (filter #(pos? (:habit-rank-moves %)) arms)) (count arms)
                    (reduce max (map :habit-max-move arms))))
      (emit (format "HABIT-ADJUSTED argmax moved on %d/%d ticks"
                    (count (filter :habit-argmax-moved? arms)) (count arms)))
      (emit (format "  ln E: %s distinct values, spread %s -- within one value the order is tau-invariant (the shift is monotone in G), so only cross-group pairs can move"
                    (pr-str (distinct (map :ln-e-distinct arms)))
                    (fmt (reduce max (map :ln-e-spread arms)))))
      (emit (format "  near-miss at tau_s3: smallest adjacent margin %s; smallest CROSS-GROUP adjacent margin %s (negative = a flip)"
                    (fmt (reduce min (map :min-adjacent-margin-s3 arms)))
                    (fmt (reduce min (map :min-cross-group-margin-s3 arms)))))
      (emit (format "SOFTMAX argmax moved on %d/%d ticks; mean TV distance %s; mean entropy %s -> %s"
                    (count (filter :softmax-argmax-moved? arms)) (count arms)
                    (fmt (/ (reduce + (map :softmax-tv arms)) (count arms)))
                    (fmt (/ (reduce + (map :softmax-entropy-live arms)) (count arms)))
                    (fmt (/ (reduce + (map :softmax-entropy-s3 arms)) (count arms)))))
      (emit (format "RECORDED :action equals controller rank 1 on %d/%d ticks (the strategic selector replaces it)"
                    (count (filter :action-is-controller-rank1? arms)) (count arms)))
      (emit "per-tick:")
      (doseq [[i a] (map-indexed vector arms)]
        (emit (format "  t%-3d tau_live %s tau_s3 %s | ctl-moves %d | habit-moves %3d (max %2d, argmax %s) | softmax argmax %s TV %s dH %s"
                      (inc i) (fmt (:tau-live a)) (fmt (:tau-s3 a))
                      (:controller-rank-moves a)
                      (:habit-rank-moves a) (:habit-max-move a)
                      (if (:habit-argmax-moved? a) "MOVED" "same")
                      (if (:softmax-argmax-moved? a) "MOVED" "same")
                      (fmt (:softmax-tv a))
                      (fmt (- (:softmax-entropy-s3 a) (:softmax-entropy-live a)))))))
    (emit "")))

(emit (str "CONTROLS: " (if @all-ok "ALL PASSED" "FAILED -- see above")))
(io/make-parents out-path)
(spit out-path (str sb))
(println "wrote" out-path)
(when-not @all-ok (System/exit 1))
