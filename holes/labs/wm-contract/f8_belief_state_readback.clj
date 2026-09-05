;; F8 leg 1 slice 6: production stored-belief readback.
;; Every expected value below is transcribed from a named theorem in
;; MachineBeliefStateWitness, not recomputed from the production result.
(require '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[futon2.aif.belief :as belief])

(def status-order [:spawned :refined :strengthened :addressed
                   :falsified :foreclosed :reopened])
(def uniform-expected (zipmap status-order (repeat (/ 1.0 7.0))))
(def uniform-entropy-expected (- (* 7.0 (/ 1.0 7.0) (Math/log (/ 1.0 7.0)))))
(def peaked {:spawned 1.0 :refined 0.0 :strengthened 0.0 :addressed 0.0
             :falsified 0.0 :foreclosed 0.0 :reopened 0.0})
(def collision-a {:spawned 0.5 :refined 0.3 :strengthened 0.2 :addressed 0.0
                  :falsified 0.0 :foreclosed 0.0 :reopened 0.0})
(def collision-b {:spawned 0.5 :refined 0.2 :strengthened 0.3 :addressed 0.0
                  :falsified 0.0 :foreclosed 0.0 :reopened 0.0})
(def collision-entropy-expected
  (- (+ (* 0.5 (Math/log 0.5)) (* 0.3 (Math/log 0.3))
        (* 0.2 (Math/log 0.2)))))

(def uniform (belief/uniform-prior))
(def fresh {0 uniform 1 uniform})
(def carried {0 peaked 2 peaked})
(def reconciled (belief/reconcile-belief-carry fresh carried))
(def cold (belief/reconcile-belief-carry fresh nil))
(def t0 {0 peaked})
(def t1 (belief/reconcile-belief-carry {} t0))
(def t2 (belief/reconcile-belief-carry {0 uniform} t1))
(defn delta [a b] (- (double a) (double b)))
(def uniform-max-delta
  (apply max (map #(Math/abs (delta (get uniform %) (get uniform-expected %)))
                  status-order)))
(def entropy-a (belief/entropy collision-a))
(def entropy-b (belief/entropy collision-b))
(defn mass [posterior] (reduce + (vals posterior)))

(def lines
  ["F8 leg 1 slice 6 -- production stored-belief readback"
   (str "uniform coordinates actual=" (mapv uniform status-order)
        " Lean=seven copies of 1/7 max-delta=" uniform-max-delta)
   (format "uniform entropy actual %.17g Lean symbolic -7*(1/7*log(1/7)) %.17g delta %.17g"
           (belief/entropy uniform) uniform-entropy-expected
           (delta (belief/entropy uniform) uniform-entropy-expected))
   (str "collision argmax A=" (belief/most-likely-status collision-a)
        " B=" (belief/most-likely-status collision-b) " Lean=:spawned")
   (format "collision entropy A %.17g B %.17g Lean %.17g deltas %.17g %.17g"
           entropy-a entropy-b collision-entropy-expected
           (delta entropy-a collision-entropy-expected)
           (delta entropy-b collision-entropy-expected))
   ;; The collision IS the A-vs-B difference; measuring each against the
   ;; reference leaves it to be inferred. Lean: collisionSameEntropy.
   (format "collision entropy A-B actual %.17g Lean 0 (collisionSameEntropy)"
           (delta entropy-a entropy-b))
   (str "collision posteriors distinct actual=" (not= collision-a collision-b)
        " Lean=true (collisionDistinct)")
   (format "collision masses A %.17g B %.17g Lean 1 1 (collisionA/BIsNormalised)"
           (mass collision-a) (mass collision-b))
   (str "carry survivor[0] actual=" (= peaked (get reconciled 0)) " Lean=true")
   (str "carry new[1] actual=" (= uniform (get reconciled 1)) " Lean=true")
   (str "carry vanished[2] actual=" (contains? reconciled 2) " Lean=false")
   (str "carry equals fresh actual=" (= reconciled fresh) " Lean=false")
   (str "carry equals carried actual=" (= reconciled carried) " Lean=false")
   (str "cold-start equals fresh actual=" (= cold fresh) " Lean=true")
   (str "three ticks t0-peaked=" (= peaked (get t0 0))
        " t1-absent=" (not (contains? t1 0))
        " t2-uniform=" (= uniform (get t2 0)) " Lean=true,true,true")
   (if (and (< uniform-max-delta 1.0e-12)
            (< (Math/abs (delta (belief/entropy uniform)
                                uniform-entropy-expected)) 1.0e-12)
            (= :spawned (belief/most-likely-status collision-a)
                        (belief/most-likely-status collision-b))
            (< (Math/abs (delta entropy-a collision-entropy-expected)) 1.0e-12)
            (< (Math/abs (delta entropy-b collision-entropy-expected)) 1.0e-12)
            (zero? (delta entropy-a entropy-b))
            (not= collision-a collision-b)
            (< (Math/abs (delta (mass collision-a) 1.0)) 1.0e-12)
            (< (Math/abs (delta (mass collision-b) 1.0)) 1.0e-12)
            (= peaked (get reconciled 0)) (= uniform (get reconciled 1))
            (not (contains? reconciled 2)) (not= reconciled fresh)
            (not= reconciled carried) (= cold fresh)
            (= peaked (get t0 0)) (not (contains? t1 0)) (= uniform (get t2 0)))
     "VERDICT: production matches every Lean reference."
     "VERDICT: MISMATCH.")])

(let [out (io/file "holes/labs/wm-contract/runs/F8-belief-state/clojure-readback.txt")]
  (io/make-parents out)
  (spit out (str (str/join "\n" lines) "\n"))
  (println (slurp out)))
