#!/usr/bin/env bb
(require '[checks.preemptive-repair-lint :as lint])
(System/exit (lint/main :absence *command-line-args*))
