"""Shared hermeticity guard for tests that load scripts/wm_click_repair_loop.py.

On zone the serving JVM (:7070's proof-eval path) and the Agency HTTP API are
production, not a sandbox. A test that reaches jvm_eval(), get_json(),
post_json() or ensure_seat() without supplying its own fake contacts them —
observed 2026-11-13 as a stack dump from
test_acknowledged_rows_are_recorded_but_never_pause blocked inside jvm_eval
(run -> injury_limit_breach -> active_injuries).

Every load_loop() calls harden(module) after loading. The seams below fail
loudly with AssertionError until the test patches in an explicit fake
(mock.patch.object(loop, "jvm_eval", ...) or plain assignment). reset_cast_
sessions and the repair seat path are covered transitively through the same
seams; the click launcher (subprocess.Popen in fire) is always patched by any
test that gets near it, and git()/sh() stay real because local git is not a
network seam.
"""


def harden(module):
    """Refuse, loudly, any contact with the serving JVM or the Agency."""
    def _refuse_jvm_eval(form, timeout=600):
        raise AssertionError(
            f"test reached the serving JVM: jvm_eval({form!r}); "
            "supply an explicit fake for this test")

    def _refuse_get_json(path, timeout=30):
        raise AssertionError(
            f"test called the Agency API: get_json({path!r}); "
            "supply an explicit fake for this test")

    def _refuse_post_json(path, timeout=30):
        raise AssertionError(
            f"test posted to the Agency API: post_json({path!r}); "
            "supply an explicit fake for this test")

    def _refuse_ensure_seat(seat, model):
        raise AssertionError(
            f"test registered a repair seat with the Agency: {seat}; "
            "supply an explicit fake for this test")

    module.jvm_eval = _refuse_jvm_eval
    module.get_json = _refuse_get_json
    module.post_json = _refuse_post_json
    module.ensure_seat = _refuse_ensure_seat
    # write_click_manifest consults Agency job previews; tests never may.
    module.fetch_agency_jobs_in_window = lambda started, ended: ([], None)
    # Nor the serving debugger: debugger_stops() would see the live loop's
    # held runs, and retry/abort would act on them. Tests that need these
    # patch them.
    module.debugger_stops = lambda: []

    def _refuse_held_run(run_id):
        raise AssertionError(f"test tried to continue live held run {run_id}")
    module.retry_held_run = _refuse_held_run
    module.abort_held_run = _refuse_held_run
    return module


def healthy_jvm_eval(form, timeout=600):
    """An explicit fake serving JVM for tests that drive loop.run(): no active
    injuries, debugger attached, nothing stopped. Anything else is refused —
    extend this only when a test genuinely needs another answer."""
    if "ACTIVE-INJURIES" in form:
        return {"rc": 0, "out": '{:ok true, :value "ACTIVE-INJURIES 0 []"}', "err": ""}
    if "attached?" in form:
        return {"rc": 0, "out": "{:ok true, :value true}", "err": ""}
    if "attach!" in form:
        return {"rc": 0, "out": "{:ok true, :value :attached}", "err": ""}
    if "wm.debugger/stopped" in form:
        return {"rc": 0, "out": "{}", "err": ""}
    raise AssertionError(
        f"healthy_jvm_eval has no fake for the serving-JVM form {form[:160]!r}; "
        "add one in the test that needs it")

