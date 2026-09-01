#!/usr/bin/env clojure
;; RUN7 / stage S2 -- the J4 arms and the pi_0 arms, re-run on OUR field.
;;
;; WHAT IS NEW HERE, against C464 which asked the same two questions in July.
;; C464 could only order candidates by :G-total, because the July records did
;; not carry the :controller-score policy selection actually consumes, and it
;; had to RECONSTRUCT each candidate's horizon-one prediction with the forward
;; model because those records predate I3. Both limits are gone: S2's records
;; carry :controller-score per candidate and the F_pi in them was scored
;; against PERSISTED predictions by the tick itself. So every rank below is the
;; machine's own rank, not a proxy for it.
;;
;; Usage: clojure -M holes/labs/wm-contract/run7_beta_arms.clj <records.edn>
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[futon2.aif.habit-prior :as habit-prior]
         '[futon2.aif.policy-precision :as pp-solver])

(def beta-zeros [0.5 1.0 2.0 5.0])
(def beta-floor 1.0e-6)
(def beta-ceiling 1.0e6)
(def tolerance 1.0e-9)

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

;; ---------------------------------------------------------------------------
;; A generalised eq. 2.7 residual, so ln E can be put in pi, in pi_0, in both,
;; or in neither. `converge-beta` in src does the "neither" arm; this is the
;; only way to ask what the other three would have solved to, and its "neither"
;; setting is checked against src below rather than assumed to agree.

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
      {:bracketed? false :beta beta-prior :residual-at-floor rlo :residual-at-ceiling rhi}
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

;; ---------------------------------------------------------------------------
;; The field of one record: exactly the join the tick performed.

(defn field
  [record]
  (let [envelope (:f-pi-by-candidate-id record)
        ranked (:ranked-actions record)
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
                 (update :action conj (:action cand)))
             acc)))
       {:g [] :f [] :ln-e [] :machine-rank [] :action []}
       (sort-by key (:by-candidate-id envelope))))))

(defn- argmax-index [xs] (first (apply max-key second (map-indexed vector xs))))

(defn- rank-of
  "Descending order of pi -> a 1-based rank per candidate index."
  [pi]
  (let [order (map first (sort-by (comp - second) (map-indexed vector pi)))]
    (into {} (map-indexed (fn [r i] [i (inc r)]) order))))

(defn- moved
  "How many candidates the two orderings place differently, and the largest move."
  [a b]
  (let [ra (rank-of a) rb (rank-of b)
        deltas (map (fn [i] (Math/abs (long (- (ra i) (rb i))))) (range (count a)))]
    {:moved (count (filter pos? deltas)) :max-move (apply max 0 deltas)}))

;; ---------------------------------------------------------------------------

(defn -main [& [path]]
  (let [records (read-records (or path "holes/labs/wm-contract/runs/2026-09-01-s2/wm-trace-s2.edn"))
        fields (mapv field records)
        usable (keep-indexed (fn [i f] (when (seq (:g f)) [i f])) fields)]
    (println ";; records:" (count records) " with a usable aligned field:" (count usable))

    ;; CONTROL, before any arm is read: the generalised solver with ln E in
    ;; neither place must reproduce src's converge-beta. Without this the three
    ;; ln E arms would rest on an unchecked reimplementation.
    (let [[_ f] (first usable)
          mine (solve 1.0 (:g f) (:f f) (:ln-e f) {:e-in-pi? false :e-in-pi-0? false})
          theirs (pp-solver/converge-beta 1.0 (:g f) (:f f))]
      (println ";; solver control: local" (:beta mine) " src" (:beta-posterior theirs)
               " delta" (Math/abs (- (:beta mine) (:beta-posterior theirs)))))

    ;; --- J4 arms: carried prior vs fixed prior, at four beta_0 -------------
    (println "\n;; === J4 ARMS (carry vs reset), on the machine's own controller scores")
    (doseq [b0 beta-zeros]
      (let [carried (reduce (fn [acc [i f]]
                              (let [prior (:beta (peek acc) b0)
                                    s (solve prior (:g f) (:f f) (:ln-e f)
                                             {:e-in-pi? false :e-in-pi-0? false})]
                                (conj acc {:tick i :beta (:beta s) :gamma (:gamma s)
                                           :converged? (:converged? s) :pi (:pi s)})))
                            [] usable)
            fixed (mapv (fn [[i f]]
                          (let [s (solve b0 (:g f) (:f f) (:ln-e f)
                                         {:e-in-pi? false :e-in-pi-0? false})]
                            {:tick i :beta (:beta s) :gamma (:gamma s)
                             :converged? (:converged? s) :pi (:pi s)}))
                        usable)
            argmax-differs (count (filter true?
                                          (map (fn [c f] (not= (argmax-index (:pi c))
                                                               (argmax-index (:pi f))))
                                               carried fixed)))
            rank-moves (map (fn [c f] (moved (:pi c) (:pi f))) carried fixed)]
        (pp/pprint
         {:beta-0 b0
          :ticks (count usable)
          :carried-beta (mapv #(-> % :beta (* 1e6) Math/round (/ 1e6)) carried)
          :carried-converged (count (filter :converged? carried))
          :fixed-beta-first (:beta (first fixed))
          :fixed-converged (count (filter :converged? fixed))
          :argmax-differs-on-ticks argmax-differs
          :rank-moved-per-tick (mapv :moved rank-moves)
          :max-rank-move (apply max 0 (map :max-move rank-moves))})))

    ;; --- pi_0 arms: where ln E goes ---------------------------------------
    (println "\n;; === PI_0 ARMS (where ln E enters), beta_0 = 1.0, carried")
    (doseq [[label arm] [["neither (as run, and as src computes it)"
                          {:e-in-pi? false :e-in-pi-0? false}]
                         ["pi only (friston2017 text, pi_0 = sigma(-gamma G))"
                          {:e-in-pi? true :e-in-pi-0? false}]
                         ["both (SPM spm_MDP_VB_X.m:964 code form)"
                          {:e-in-pi? true :e-in-pi-0? true}]]]
      (let [traj (reduce (fn [acc [i f]]
                           (let [prior (:beta (peek acc) 1.0)
                                 s (solve prior (:g f) (:f f) (:ln-e f) arm)]
                             (conj acc {:tick i :beta (:beta s) :gamma (:gamma s)
                                        :converged? (:converged? s)
                                        :bracketed? (:bracketed? s) :pi (:pi s)})))
                         [] usable)]
        (pp/pprint {:arm label
                    :beta (mapv #(-> % :beta (* 1e6) Math/round (/ 1e6)) traj)
                    :gamma-first (:gamma (first traj))
                    :gamma-last (:gamma (peek traj))
                    :converged (count (filter :converged? traj))
                    :bracketed (count (filter :bracketed? traj))
                    :argmax-vs-machine-rank-1
                    (mapv (fn [t [_ f]]
                            (= 1 (nth (:machine-rank f) (argmax-index (:pi t)))))
                          traj usable)})))

    ;; --- what the machine recorded, beside what this reproduces ------------
    (println "\n;; === RECORDED vs REPRODUCED (the run's own :policy-precision-state)")
    (doseq [[i f] usable]
      (let [state (:policy-precision-state (nth records i))
            s (solve (get-in state [:solve :beta-prior] 1.0)
                     (:g f) (:f f) (:ln-e f) {:e-in-pi? false :e-in-pi-0? false})]
        (println (format "tick %2d recorded beta %.9f gamma %.9f n=%d | reproduced beta %.9f delta %.2e"
                         (inc i)
                         (double (get-in state [:solve :beta-posterior] Double/NaN))
                         (double (get-in state [:solve :gamma] Double/NaN))
                         (long (:f-pi-present-count state 0))
                         (double (:beta s))
                         (Math/abs (- (double (get-in state [:solve :beta-posterior] Double/NaN))
                                      (:beta s)))))))))

(apply -main *command-line-args*)
