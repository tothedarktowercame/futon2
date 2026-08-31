#!/usr/bin/env bb
(require '[checks.preemptive-repair-lint :as lint])
(System/exit (lint/main :acceptance *command-line-args*))
