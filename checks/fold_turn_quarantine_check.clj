#!/usr/bin/env bb
(ns checks.fold-turn-quarantine-check
  (:require [futon2.aif.fold-escrow :as escrow]))

(defn -main [& args]
  (let [negative? (some #{"--negative"} args)
        opts (when negative? {:quarantine-entries []})
        {:keys [deposits quarantined rejected]} (escrow/load-deposits
                                                 escrow/default-deposit-dir
                                                 opts)
        positive-pass? (and (= 8 (count deposits))
                            (= 10 (count quarantined))
                            (empty? rejected))
        negative-rejected? (and (= 8 (count deposits))
                                (empty? quarantined)
                                (= 10 (count rejected)))]
    (println (pr-str {:accepted (count deposits)
                      :quarantined (count quarantined)
                      :rejected (count rejected)}))
    (if negative?
      ;; Removing the quarantine source must fail loudly. If it somehow yields
      ;; the positive disposition, the mutation slipped.
      (if (not negative-rejected?)
        (do (println "fold-turn-quarantine-check: negative-control FAIL (mutation slipped) exit-convention=0-pass/1-fail/2-mutation-slipped")
            (System/exit 2))
        (do (println "fold-turn-quarantine-check: negative-control PASS (absent quarantine rejected) exit-convention=0-pass/1-fail/2-mutation-slipped")
            (System/exit 0)))
      (do (println (str "fold-turn-quarantine-check: "
                        (if positive-pass? "PASS" "FAIL")
                        " exit-convention=0-pass/1-fail/2-mutation-slipped"))
          (System/exit (if positive-pass? 0 1))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
