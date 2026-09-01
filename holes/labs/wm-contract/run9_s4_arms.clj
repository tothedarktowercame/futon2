;; RUN9 / stage S4 arms: what F_pi in the LIVE policy posterior moved.
;;
;;   clojure -M:test holes/labs/wm-contract/run9_s4_arms.clj
;;
;; Replayed with `policy/softmax-weights` ITSELF, not a re-implementation, from
;; each record's own persisted inputs -- the controller ranking's
;; :controller-score and :habit-prior-bias, the decision's :tau, and the F_pi
;; readback joined by the same action identity the live tick joined on.
;;
;; CONTROL FIRST, and read it before any arm line: the replay must reproduce
;; each record's own :softmax-weights-by-candidate-id at delta 0.0 under the
;; setting that record says it ran with. If it does not, the arms below are
;; measuring the wrong thing.
;;
;; Two fields:
;;   S4  runs/2026-09-01-s4/wm-trace-s4.edn  -- F_pi LIVE in the posterior
;;   S2  runs/2026-09-01-s2/wm-trace-s2.edn  -- F_pi dark; the arm is the
;;       counterfactual, what S4's wiring would have done to that field
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[futon2.aif.habit-prior :as habit-prior]
         '[futon2.aif.policy :as policy])

(defn- read-trace [path]
  (with-open [r (java.io.PushbackReader. (io/reader path))]
    (doall (take-while #(not= ::eof %)
                       (repeatedly #(edn/read {:eof ::eof
                                               :default (fn [_ v] v)} r))))))

(defn- f-pi-by-identity
  "identity -> F_pi, from the present entries of the readback envelope. An
   identity claimed by two entries is dropped, exactly as the live
   `f-pi-posterior-opts` drops it."
  [record]
  (let [envelope (:f-pi-by-candidate-id record)
        entries (when (= :present (:status envelope))
                  (vals (:by-candidate-id envelope)))
        grouped (reduce (fn [acc e]
                          (if (and (some? (:candidate-identity e))
                                   (= :present (:status e))
                                   (number? (:value e)))
                            (update acc (:candidate-identity e) (fnil conj []) (double (:value e)))
                            acc))
                        {} entries)]
    (into {} (keep (fn [[k vs]] (when (= 1 (count vs)) [k (first vs)])) grouped))))

(defn- recorded-weights
  "The posterior the record itself carries, in controller-ranking order.
   :softmax-weights-by-candidate-id is keyed by the rank/N of the OUTER
   :ranked-actions field, so the join back to the selector's own order is by
   action identity, not by index."
  [record order]
  (let [by-id (:softmax-weights-by-candidate-id (:decision record))
        id->identity (into {} (map (fn [ra]
                                     [(str "rank/" (:rank ra))
                                      (habit-prior/policy-key (:action ra))]))
                           (:ranked-actions record))
        identity->w (reduce (fn [acc [id w]]
                              (let [k (get id->identity id)]
                                (if k (update acc k (fnil conj []) (double w)) acc)))
                            {} by-id)]
    (mapv (fn [k] (let [vs (get identity->w k)]
                    (when (= 1 (count vs)) (first vs))))
          order)))

(defn- tick-inputs [record]
  (let [ranking (get-in record [:decision :controller-ranking])
        order (mapv #(habit-prior/policy-key (:action %)) ranking)
        f-pi-map (f-pi-by-identity record)]
    {:timestamp (:timestamp record)
     :order order
     :unique-order? (= (count order) (count (set order)))
     :tau (get-in record [:decision :tau])
     :g (mapv :controller-score ranking)
     :ln-e (mapv :habit-prior-bias ranking)
     :f-pi (mapv #(get f-pi-map %) order)
     :envelope (get-in record [:decision :f-pi-posterior])
     :recorded (recorded-weights record order)}))

(defn- posterior [{:keys [g ln-e tau f-pi]} with-f-pi?]
  (policy/softmax-weights g tau ln-e
                          (if with-f-pi?
                            {:f-pi-policy-posterior? true
                             :f-pi-values f-pi
                             :f-pi-scaling :unscaled}
                            {})))

(defn- tv [a b]
  (* 0.5 (reduce + (map #(Math/abs (- (double %1) (double %2))) a b))))

(defn- ranks-of [xs]
  ;; descending order position of each index
  (let [order (sort-by #(- (double (nth xs %))) (range (count xs)))]
    (reduce (fn [acc [pos idx]] (assoc acc idx pos)) {} (map-indexed vector order))))

(defn- rank-moves [a b]
  (let [ra (ranks-of a) rb (ranks-of b)]
    (reduce (fn [acc idx]
              (let [d (Math/abs (- (int (get ra idx)) (int (get rb idx))))]
                (-> acc (update :moved + (if (pos? d) 1 0)) (update :max-move max d))))
            {:moved 0 :max-move 0}
            (range (count a)))))

(defn- report [label path]
  (println)
  (println (str "=== " label " -- " path))
  (let [records (read-trace path)
        rows (map tick-inputs records)]
    (println (format "%d records" (count rows)))
    ;; ---- control ----
    (let [deltas (keep (fn [{:keys [recorded envelope] :as t}]
                         (when (every? some? recorded)
                           (let [replayed (posterior t (boolean (:applied? envelope)))]
                             (reduce max 0.0 (map #(Math/abs (- (double %1) (double %2)))
                                                  recorded replayed)))))
                       rows)
          uniq (every? :unique-order? rows)
          joined (count deltas)]
      (println (format "CONTROL: %d/%d ticks re-joined; identities unique on every tick: %s; max |delta| %s"
                       joined (count rows) uniq
                       (if (seq deltas) (format "%.3e" (reduce max 0.0 deltas)) "n/a")))
      (when (or (not uniq) (< joined (count rows))
                (> (reduce max 0.0 deltas) 1.0e-12))
        (println "CONTROL FAILED -- the arm lines below are not trustworthy")
        (System/exit 1)))
    ;; ---- arms ----
    (let [covered (filter #(every? some? (:f-pi %)) rows)
          applied (filter #(get-in % [:envelope :applied?]) rows)]
      (println (format "coverage: F_pi complete on %d/%d ticks; the tick APPLIED it on %d"
                       (count covered) (count rows) (count applied)))
      (doseq [{:keys [timestamp envelope] :as t} covered]
        (let [with (posterior t true)
              without (posterior t false)
              {:keys [moved max-move]} (rank-moves with without)
              argmax-with (apply max-key #(nth with %) (range (count with)))
              argmax-without (apply max-key #(nth without %) (range (count without)))
              spread (fn [xs] (- (apply max xs) (apply min xs)))]
          (println (format "  %s applied?=%s n=%d | TV %.6f | ranks moved %d/%d max %d | argmax %s | F_pi spread %.4f, G spread %.4f"
                           timestamp (boolean (:applied? envelope)) (count with)
                           (tv with without) moved (count with) max-move
                           (if (= argmax-with argmax-without) "UNCHANGED" "MOVED")
                           (spread (:f-pi t)) (spread (:g t)))))))))

(report "S4 (F_pi live in the posterior)"
        "holes/labs/wm-contract/runs/2026-09-01-s4/wm-trace-s4.edn")
(report "S2 (F_pi dark; arm = the counterfactual)"
        "holes/labs/wm-contract/runs/2026-09-01-s2/wm-trace-s2.edn")
(println)
(println "run9-s4-arms: done")
