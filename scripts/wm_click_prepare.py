#!/usr/bin/env python3
"""Idle-only source preparation. No click, budget, repair, abort or retry port.

The canonical load_identity.clj declaration is the inventory, not this script.
Verification resolves existing Vars only; it never requires/registers code.
Serving activation of the verifier itself is a separate reviewed operation.
"""
import argparse
import json
import re
import sys

import wm_click_repair_loop as reload


class PreparationRefused(RuntimeError):
    pass


def declaration():
    text = reload.SOURCE_IDENTITY_INVENTORY.read_text()
    # This declaration is deliberately a literal vector of quoted symbols.
    # Refuse changes of representation rather than guessing an inventory.
    match = re.search(r'\(def source-composition\s+"[^"]*"\s+\[([^\]]+)\]\)', text)
    if not match:
        raise PreparationRefused("composition-declaration-unavailable")
    body = match.group(1)
    names = re.findall(r"'([\w.\-]+)", body)
    if not names or re.sub(r"'[\w.\-]+|\s+", "", body) or len(set(names)) != len(names):
        raise PreparationRefused("composition-declaration-invalid")
    inventory = reload.source_identity_inventory()
    if any(n not in inventory for n in names):
        raise PreparationRefused("composition-path-unavailable")
    return names, inventory


def idle():
    if reload.get_json("/api/alpha/wm/click").get("running?") is not False:
        raise PreparationRefused("click-running-or-status-unavailable")


def clean():
    # require may load a not-yet-loaded dependency: inspect all tracked files
    # in both canonical repositories, not only a stale parent file.
    for repo in [reload.F2, reload.F3C]:
        result = reload.sh("git", "-C", repo, "status", "--porcelain", "--untracked-files=no")
        if result.returncode or result.stdout.strip():
            raise PreparationRefused(f"canonical-tree-dirty-or-unavailable: {repo}")
        sources = reload.sh("git", "-C", repo, "status", "--porcelain",
                            "--untracked-files=all", "--", "src", "scripts/futon2")
        if sources.returncode or sources.stdout.strip():
            raise PreparationRefused(f"canonical-source-uncommitted-or-unavailable: {repo}")


def snapshot(names):
    form = """(let [n (find-ns 'futon2.aif.load-identity)
                       v (when n (ns-resolve n 'composition-status))]
                   (if v (v) {:status :unavailable :reason :verifier-not-loaded}))"""
    result = reload.jvm_eval(form)
    if result["rc"]:
        raise PreparationRefused("composition-read-unavailable")
    # proof-eval returns EDN with :value. Parse locally, never eval its output.
    decoded = reload.sh("bb", "-e", "(require '[clojure.edn :as edn] '[cheshire.core :as json]) (print (json/generate-string (:value (edn/read-string (slurp *in*)))))", input=result["out"])
    if decoded.returncode:
        raise PreparationRefused("composition-response-unreadable")
    value = json.loads(decoded.stdout)
    rows = value.get("namespaces", {}) if isinstance(value, dict) else {}
    if set(rows) != set(names):
        raise PreparationRefused("composition-verifier-unavailable-or-declaration-mismatch")
    return rows


def prepare(dry_run=False):
    if dry_run:
        return {"status": "not-prepared", "reason": "dry-run"}
    idle()
    clean()
    names, inventory = declaration()
    before = snapshot(names)
    if any(row.get("status") not in ("current", "stale", "unregistered") for row in before.values()):
        raise PreparationRefused("composition-identity-unavailable")
    files = [inventory[n] for n in names if before[n]["status"] != "current"]
    order = reload.reload_order(files)
    if {n for n, _ in order} != {n for n in names if before[n]["status"] != "current"}:
        raise PreparationRefused("reload-plan-incomplete")
    print(json.dumps({"status": "planned", "before": before,
                      "loads": [{"namespace": n, "path": str(p)} for n, p in order]}), flush=True)
    for ns, _ in order:
        idle()
        clean()
        result = reload.reload_namespace(ns)
        if result["rc"] or ":reloaded" not in result["out"]:
            raise PreparationRefused(f"reload-failed: {ns}: {result}")
    idle()
    clean()
    if declaration()[0] != names:
        raise PreparationRefused("composition-changed-during-preparation")
    after = snapshot(names)
    if any(row.get("status") != "current" for row in after.values()):
        raise PreparationRefused("composition-still-stale-or-unavailable")
    return {"status": "current", "namespaces": after, "reloaded": [n for n, _ in order]}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    try:
        print(json.dumps(prepare(args.dry_run)))
        return 0
    except Exception as exc:
        print(json.dumps({"status": "launch-unavailable", "reason": str(exc)}), file=sys.stderr)
        return 3


if __name__ == "__main__":
    sys.exit(main())
