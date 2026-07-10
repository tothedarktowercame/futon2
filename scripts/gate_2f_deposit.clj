;; gate_2f_deposit.clj — E-live-loop-2 gate 2f (standing evidence check).
;; PASS iff every ft-*.edn in the real escrow dir:
;;   (a) loads with ZERO rejections (pins 2+3 run inside load-deposits:
;;       arming record present + stored ΔG == coverage-delta-g recomputed);
;;   (b) sha-MATCHES: the prompt REBUILT from the record's own cascade
;;       pattern-ids + circumstance {:mission :psi} + the verbatim flexiarg
;;       prose files hashes to the stored :prompt :sha256 (pin 1) — so the
;;       deposit is replayable against a reproducible prompt, not just
;;       self-consistent;
;;   (c) replays: escrow-turn-fn returns the recorded answer on that prompt.
;; NOTE circumstance is the literal {:mission … :psi …} (insertion order —
;; fold-prompt pr-str's it); 2g's caller must construct it the same way.
;; Run: cd /home/joe/code/futon2 && clojure -M scripts/gate_2f_deposit.clj
(ns gate-2f-deposit
  (:require [futon2.aif.fold-escrow :as esc]
            [futon2.aif.fold-llm :as fl]))

(defn -main []
  (let [{:keys [deposits rejected]} (esc/load-deposits)]
    (when (seq rejected)
      (println "GATE 2f FAIL — rejected deposits:" (mapv :reason rejected))
      (System/exit 1))
    (when (empty? deposits)
      (println "GATE 2f FAIL — no deposits in" esc/default-deposit-dir)
      (System/exit 1))
    (let [turn-fn (esc/escrow-turn-fn deposits)]
      (doseq [d deposits
              :let [cascade (vec (get-in d [:cascade :pattern-ids]))
                    circumstance {:mission (:mission d)
                                  :psi (get-in d [:cascade :psi])}
                    proses (into {} (for [p cascade]
                                      [p (slurp (str "/home/joe/code/futon3/library/" p ".flexiarg"))]))
                    prompt (fl/fold-prompt cascade circumstance proses)]]
        (when-not (= (get-in d [:prompt :sha256]) (esc/prompt-sha prompt))
          (println "GATE 2f FAIL — prompt-sha mismatch for" (:fold-turn/id d)
                   "(cascade/psi/prose drifted from the recorded turn)")
          (System/exit 1))
        (when-not (= (get-in d [:turn :answer]) (turn-fn prompt))
          (println "GATE 2f FAIL — replay did not return the recorded answer for" (:fold-turn/id d))
          (System/exit 1))
        (println (format "  %s: sha-match + replay + dG re-assert OK (dG %s; arming word %s)"
                         (:fold-turn/id d)
                         (get-in d [:eval :delta-g])
                         (pr-str (subs (get-in d [:arming :word]) 0
                                       (min 40 (count (get-in d [:arming :word])))))))))
    (println "GATE 2f PASS —" (count deposits) "deposit(s) load cleanly, sha-matched, replayable")
    (System/exit 0)))

(-main)
