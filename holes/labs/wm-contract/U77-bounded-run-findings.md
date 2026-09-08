# U77 bounded futon3 run

The bounded-test job `bounded-1788886563555-U77-futon3` ran
`clojure -X:test` against futon3 commit `72d134b8`. The repository basis was
dirty but byte-stable from admission through completion
(`runs/U77-bounded-futon3-2026-09-08.json:25-42`).

The runner distinguished the environmental abort from the command's ordinary
test failures: the command exited 1, while the bounded wrapper returned 125
with reason `resource-limit-failure` (`runs/U77-bounded-futon3-2026-09-08.json:3-10,19-24`).
The receipt records `pthread_create failed (EAGAIN)`, failure to start
`Thread-1038`, a PID peak of exactly 1280, and four increments of
`pids.events:max` (`runs/U77-bounded-futon3-2026-09-08.json:15-24`).

The exact remaining limit is the bounded job's `TasksMax=1280`: the receipt
records that admitted ceiling at
`runs/U77-bounded-futon3-2026-09-08.json:46`, and the runner default and the
systemd property assignment are at `futon3c/scripts/bg.py:40` and
`futon3c/scripts/bg.py:177-184`. This run does not establish a clean resource
setting. Its receipt explicitly limits correlation to the whole run, so it
does not attribute the resource pressure to an individual test
(`runs/U77-bounded-futon3-2026-09-08.json:3-10`).
