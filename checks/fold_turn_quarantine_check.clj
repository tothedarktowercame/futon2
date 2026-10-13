#!/usr/bin/env bb
(ns checks.fold-turn-quarantine-check
  (:require [clojure.edn :as edn]
            [futon2.aif.fold-escrow :as escrow]))

(defn -main [& args]
  (let [negative? (some #{"--negative"} args)
        reconstructible-negative? (some #{"--negative-reconstructible-member"} args)
        recorded-entries (:entries (edn/read-string (slurp escrow/default-quarantine-path)))
        ;; Keep the historical option name for callers. The control now injects
        ;; a fabricated member and proves membership alone cannot create a
        ;; quarantined record or hide a rejection.
        valid-key {:fold-turn/id "ft-nonexistent-control"
                   :prompt-sha256 "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                   :reason :prompt-not-reconstructable}
        opts (cond negative? {:quarantine-entries []}
                   reconstructible-negative? {:quarantine-entries (conj recorded-entries valid-key)}
                   :else nil)
        {:keys [deposits quarantined rejected]} (escrow/load-deposits
                                                 escrow/default-deposit-dir
                                                 opts)
        quarantined-property? (every? #(= :prompt-not-reconstructable (:reason %)) quarantined)
        positive-pass? (and (empty? deposits)
                            (= 18 (count quarantined))
                            quarantined-property?
                            (empty? rejected))
        negative-rejected? (and (empty? deposits)
                                (empty? quarantined)
                                (= 18 (count rejected)))]
    (println (pr-str {:accepted (count deposits)
                      :quarantined (count quarantined)
                      :rejected (count rejected)}))
    (cond
      reconstructible-negative?
      (let [fabricated-quarantined? (some #(= "ft-nonexistent-control" (:fold-turn/id %)) quarantined)
            pass? (and positive-pass? (not fabricated-quarantined?))]
        (println (str "fold-turn-quarantine-check: "
                      (if pass? "negative-control PASS (fabricated quarantine member ignored)"
                          "negative-control FAIL (fabricated quarantine member affected disposition)")
                      " exit-convention=0-pass/1-fail/2-mutation-slipped"))
        (System/exit (if pass? 0 2)))

      negative?
      ;; Removing the quarantine source must fail loudly. If it somehow yields
      ;; the positive disposition, the mutation slipped.
      (if (not negative-rejected?)
        (do (println "fold-turn-quarantine-check: negative-control FAIL (mutation slipped) exit-convention=0-pass/1-fail/2-mutation-slipped")
            (System/exit 2))
        (do (println "fold-turn-quarantine-check: negative-control PASS (absent quarantine rejected) exit-convention=0-pass/1-fail/2-mutation-slipped")
            (System/exit 0)))
      :else
      (do (println (str "fold-turn-quarantine-check: "
                        (if positive-pass? "PASS" "FAIL")
                        " exit-convention=0-pass/1-fail/2-mutation-slipped"))
          (System/exit (if positive-pass? 0 1))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
