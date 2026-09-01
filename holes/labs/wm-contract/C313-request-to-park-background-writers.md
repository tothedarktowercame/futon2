# C313 — paste-ready request to Joe for the quiet run

**Execution consolidated in `C319-quiet-run-execution.md`.** This file remains
the source request and command-verification record.

Date: 2026-09-01. Prepared by `wm-organization`; to be sent and executed by
`claude-20` and Joe. Nothing below was executed while preparing this request.

## Request

> Joe — may I have a fenced War Machine operator window? Please keep Futon3c
> running, but park the background writers below. I need a **60-minute planning
> reservation**, beginning only after every item reports parked. Preparation to
> READY measured 6m24s and is budgeted at 10 minutes. Reload and the production
> click have never been timed together and live author/reviewer latency is
> unbounded, so 60 minutes is not a completion bound: I will explicitly send
> `FENCE-RELEASE` when certification or an orderly abort has completed. Please
> do not resume anything before that message.
>
> First, durably stop these currently enabled coordinators inside the serving
> JVM:
>
> - `jit-queue:jit-m94A03-retry-v3`
> - `jit-queue:jit-all-open-v2`
> - `ftriangle-live-smoke-v1`
>
> From `/home/joe/code/futon3c`, use the canonical serving-JVM evaluation
> surface (not a second application JVM):
>
> ```sh
> cd /home/joe/code/futon3c
> for id in \
>   'jit-queue:jit-m94A03-retry-v3' \
>   'jit-queue:jit-all-open-v2' \
>   'ftriangle-live-smoke-v1'
> do
>   printf '%s\n' "(do (require 'futon3c.apm.durable-coordinator) (futon3c.apm.durable-coordinator/stop! \"/home/joe/code/futon3c/data/apm-coordinators/registry.edn\" \"$id\"))" \
>     | scripts/proof-eval.sh -
> done
> ```
>
> Each result must say `:ok true`, `:durably-disabled? true`, and ultimately
> `:status :stopped` with a quiescence witness. `:status :draining` means an
> in-flight tick remains: wait and re-observe; it is not parked yet.
>
> Then stop only the background units that can dispatch, restart campaign work,
> or overlap the resource window:
>
> ```sh
> systemctl --user stop apm-campaign-babysit-jit-all-open-v2.service
> systemctl --user stop apm-watchdog.timer
> systemctl --user stop apm-axiom-audit.timer
> systemctl --user stop futon-pattern-index.timer
> ```
>
> Let an already-running `apm-closer.service`, audit, or pattern-index job
> finish rather than killing it. Once terminal, `apm-closer` must remain down
> while `apm-watchdog.timer` is parked. Do not stop `futon3c-zone.service`—that
> is the JVM Joe will reload and click.
>
> Please reply with the three stop results and the unit-state output below. I
> will independently rerun the same observations plus C292 before declaring
> `FENCE-HELD`:
>
> ```sh
> systemctl --user is-active \
>   apm-campaign-babysit-jit-all-open-v2.service \
>   apm-watchdog.timer apm-axiom-audit.timer futon-pattern-index.timer \
>   apm-closer.service
> systemctl --user list-timers --all --no-pager
> python3 /home/joe/code/futon3c/scripts/bg.py list
> python3 /home/joe/code/futon3c/scripts/bg.py test-list
> ```
>
> I will also verify twice that all three registry entries read
> `:coordinator/enabled? false`, their state files contain no tick claim and do
> contain a quiescence witness, all four WM lanes are idle and acknowledged,
> the five repositories are clean, no bounded/ordinary job is active, and no
> replacement writer process appeared.
>
> A pulse or write before reload closes the window and costs a complete rerun of
> gate, suites, preflight, and readiness. A breach after reload is recoverable:
> the reload remains usable only if its recorded Futon2 runner identity still
> equals the newly tested settled commit; otherwise Joe reloads again. A breach
> during/after the click is preserved as evidence and the click is not repeated.
>
> After `FENCE-RELEASE`, restore **only the pre-fence enabled set**, from the
> recorded manifest. For the three coordinators that were enabled above:
>
> ```sh
> cd /home/joe/code/futon3c
> for id in \
>   'jit-queue:jit-m94A03-retry-v3' \
>   'jit-queue:jit-all-open-v2' \
>   'ftriangle-live-smoke-v1'
> do
>   printf '%s\n' "(do (require 'futon3c.apm.durable-coordinator) (futon3c.apm.durable-coordinator/resume! \"/home/joe/code/futon3c/data/apm-coordinators/registry.edn\" \"$id\"))" \
>     | scripts/proof-eval.sh -
> done
> systemctl --user start apm-watchdog.timer
> systemctl --user start apm-axiom-audit.timer
> systemctl --user start futon-pattern-index.timer
> systemctl --user start apm-campaign-babysit-jit-all-open-v2.service
> ```
>
> Joe owns the coordinator/timer park and restoration. `claude-20` owns the
> dispatch freeze, verification, `FENCE-RELEASE`, and confirming restoration.
> Do not blindly resume a unit/coordinator that was already inactive at the
> actual pre-fence observation; the captured manifest, not this draft's
> observation, is restoration authority.

## Remaining unknowns

The C309 observed process set has no unresolved `write-set-unknown`: every
observed timer, watcher, cron entry, and running user service was classified.
This is not a claim that future processes are safe. Any new PID/unit, changed
command line, newly enabled coordinator, or session absent from the
acknowledgement roster is `write-set-unknown` and blocks the request until
classified.

The read-only mana/metaspace samplers and shell watchers need not be parked.
They remain visible in the census so a replacement process cannot inherit the
old disposition merely by using a similar name.
