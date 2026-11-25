;; R1 belief: Clojure readback, F8 certificate form.
;;
;; Every `-expected` below is TRANSCRIBED FROM A LEAN STATEMENT, not derived a
;; second time from the inputs -- otherwise an exact match would only say that
;; Clojure agrees with Clojure. The transcriptions, by theorem name, in
;; mathlib4/DarkTower/WarMachine (worktree c14-cascade-outcome, branch
;; c14/cascade-outcome-type):
;;   b17-no-trace-realised-expected   R1DeepTemporalB17.noTrace_realised  = 3/34
;;                                    (uniform prior, the :no-trace record under
;;                                    R1DeepTemporalB17.traceProb)
;;   witness-expected                 R1ArguedRecords.witness_three_quarters_realised
;;                                                                  = 3/7
;;                                    (uniform prior, a reader with reliability
;;                                    3/4 claiming REALISED, Pearl's rule via
;;                                    R1ArguedRecords.readingChannel)
;; Both updates run in futon2.aif.r1-belief (the b389f2fe2 port plus
;; E-kimi-task-439's reading-channel). All arithmetic is exact ratios, as the
;; Lean uses real literals; comparisons are exact (==), no tolerance.
;;
;; Run from the futon2 repo root:
;;   clojure -M -e "(load-file \"holes/labs/wm-contract/r1_belief_readback.clj\")"
;; writes holes/labs/wm-contract/runs/R1-belief/clojure-readback.txt
;; (byte-for-byte reproducible: no timestamps, no randomness, exact ratios).
(require '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[futon2.aif.r1-belief :as r1])

;; Transcribed rationals (see header).
(def b17-no-trace-realised-expected 3/34)
(def witness-expected 3/7)

;; b17: the no-trace update; traceProb transcribed from
;; R1DeepTemporalB17.traceProb.
(def trace-prob {:none 1/10 :available 1/10 :used 3/10 :witnessed 2/5 :realised 7/10})
(def trace-channel (fn [rung kind] (case kind :trace (get trace-prob rung)
                                              :no-trace (- 1 (get trace-prob rung)))))
(def b17-no-trace-realised
  (get (r1/update-belief (r1/uniform-belief) trace-channel [:no-trace])
       :realised))

;; R1ArguedRecords.witness_three_quarters_realised: uniform prior, reading
;; channel k0 = :realised, r = 3/4, record :says-true.
(def witness-realised
  (get (r1/update-belief (r1/uniform-belief)
                         (r1/reading-channel :realised 3/4)
                         [:says-true])
       :realised))

(def lines
  ["R1 belief -- Clojure readback"
   "references transcribed from R1DeepTemporalB17.noTrace_realised and R1ArguedRecords.witness_three_quarters_realised (see header)"
   "exact ratio comparison (==), no tolerance"
   (format "b17 no-trace realised actual %s expected %s exact=%s"
           b17-no-trace-realised b17-no-trace-realised-expected
           (== b17-no-trace-realised b17-no-trace-realised-expected))
   (format "witness three-quarters realised actual %s expected %s exact=%s"
           witness-realised witness-expected
           (== witness-realised witness-expected))
   (if (and (== b17-no-trace-realised b17-no-trace-realised-expected)
            (== witness-realised witness-expected))
     "VERDICT: every measured case matches its transcribed Lean reference."
     "VERDICT: MISMATCH.")])

(let [out (io/file "holes/labs/wm-contract/runs/R1-belief/clojure-readback.txt")]
  (io/make-parents out)
  (spit out (str (str/join "\n" lines) "\n"))
  (println (slurp out)))
