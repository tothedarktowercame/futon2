#!/usr/bin/env python3
"""Carry a click's requirements verdict file into its bound self-heal child.

The service (`POST /api/alpha/wm/click/child-verdict`) refuses with a typed
reason when the file names another run record, the child is not complete, or
a verdict is already recorded; a refusal is exit 1 so that an unrecorded
child verdict never reads as a passed click.  An ordinary click (no child
bound) makes no POST and exits 0.
"""

import argparse
import json
import sys
import urllib.error
import urllib.parse
import urllib.request


def arguments(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", required=True)
    parser.add_argument("--click-id", required=True)
    parser.add_argument("--verdict-file", required=True)
    return parser.parse_args(argv)


def status_url(base, click_id):
    query = urllib.parse.urlencode({"click-id": click_id})
    return f"{base.rstrip('/')}/api/alpha/wm/click?{query}"


def record_url(base):
    return f"{base.rstrip('/')}/api/alpha/wm/click/child-verdict"


def get_json(url):
    with urllib.request.urlopen(url, timeout=20) as response:
        return json.load(response)


def post_json(url, payload):
    body = json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(
        url, data=body, method="POST",
        headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(request, timeout=20) as response:
        return json.load(response)


def run(argv=None):
    args = arguments(argv)
    try:
        state = get_json(status_url(args.base, args.click_id))
    except (OSError, urllib.error.HTTPError, json.JSONDecodeError,
            UnicodeDecodeError) as error:
        print(f"child status route failed: {error}", file=sys.stderr)
        return 1
    if state.get("child") is None:
        print("no child bound; nothing to record")
        return 0

    try:
        result = post_json(record_url(args.base),
                           {"click-id": args.click_id,
                            "verdict-file": args.verdict_file})
    except (OSError, urllib.error.HTTPError, json.JSONDecodeError,
            UnicodeDecodeError) as error:
        print("ALERT FOR JOE")
        print(f"child verdict NOT RECORDED: route failed: {error}")
        return 1

    refusal = result.get("verdict-refusal")
    if refusal:
        print("ALERT FOR JOE")
        print(f"child verdict NOT RECORDED: {refusal.get('reason')} "
              f"{json.dumps(refusal, sort_keys=True)}")
        return 1
    landed = result.get("child-verdict") or {}
    print(f"child verdict recorded on {args.click_id}: {landed.get('status')}"
          f" (parent verdict {(result.get('parent-verdict') or {}).get('status')})")
    return 0


if __name__ == "__main__":
    raise SystemExit(run())
