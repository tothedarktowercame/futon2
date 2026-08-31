#!/usr/bin/env bb
(require '[checks.preemptive-repair-lint :as lint])
(System/exit (lint/main :era *command-line-args*))
