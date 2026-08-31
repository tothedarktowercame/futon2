#!/usr/bin/env bb
(ns checks.preemptive-repair-suite
  (:require [checks.preemptive-repair-lint :as lint]))

(def hard-kinds [:acceptance :artefact :stale-baseline :era :record])
(def report-kinds [:absence])
(def all-kinds (into hard-kinds report-kinds))

(defn gate-result
  "Run the corpus gate. Five extinct defect classes are blocking; absence
   coercion is reported until C81 reduces it to zero. `mutation?` tests this
   gate's consumption of a lint result, not the lint's own falsifier."
  ([] (gate-result false))
  ([mutation?]
   (let [results (into {} (map (juxt identity #(lint/run % false)) all-kinds))
         results (if mutation?
                   (update-in results [:acceptance :findings]
                              conj {:repo :gate-control :path "mutation.clj"
                                    :finding :nonzero-finding-zero-exit})
                   results)
         hard-findings (vec (mapcat #(get-in results [% :findings]) hard-kinds))
         absence-findings (vec (get-in results [:absence :findings]))]
     {:pass? (empty? hard-findings)
      :policy {:hard-fail hard-kinds :report-only report-kinds
               :absence-hard-fails-when :promoted-after-C81}
      :hard-findings hard-findings
      :absence-count (count absence-findings)
      :absence-findings absence-findings
      :counts (into (sorted-map)
                    (map (fn [[kind result]] [kind (count (:findings result))]) results))})))

(defn -main [& args]
  (let [negative? (some #{"--negative-gate"} args)
        result (gate-result negative?)
        rejected? (not (:pass? result))]
    (println (pr-str result))
    (if negative?
      (if rejected?
        (do (println "preemptive-repair-gate: PASS gate mutation rejected exit-convention=0-pass/1-fail/2-mutation-slipped")
            (System/exit 0))
        (do (println "preemptive-repair-gate: FAIL mutation slipped exit-convention=0-pass/1-fail/2-mutation-slipped")
            (System/exit 2)))
      (if (:pass? result)
        (do (println "preemptive-repair-gate: PASS exit-convention=0-pass/1-fail")
            (System/exit 0))
        (do (println "preemptive-repair-gate: FAIL exit-convention=0-pass/1-fail")
            (System/exit 1))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
