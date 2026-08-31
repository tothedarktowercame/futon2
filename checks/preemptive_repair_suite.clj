#!/usr/bin/env bb
(ns checks.preemptive-repair-suite
  (:require [checks.preemptive-repair-lint :as lint]))

;; Gate the instruments themselves. Corpus findings remain visible through the
;; six canonical positive invocations; known debt is not silently made green.
(def kinds [:acceptance :artefact :stale-baseline :absence :era :record])
(def failures
  (for [kind kinds :when (empty? (:findings (lint/run kind true)))] kind))
(if (seq failures)
  (do (println "preemptive-repair-suite: FAIL mutations slipped" (pr-str (vec failures))
               "exit-convention=0-pass/1-fail/2-mutation-slipped")
      (System/exit 2))
  (do (println "preemptive-repair-suite: PASS six negative controls rejected"
               "exit-convention=0-pass/1-fail/2-mutation-slipped")
      (System/exit 0)))
