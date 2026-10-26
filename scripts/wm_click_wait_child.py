#!/usr/bin/env python3
"""Wait for the exact retry child durably bound to a completed WM click."""

import argparse
import json
import sys
import time
import urllib.error
import urllib.parse
import urllib.request


def arguments(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", required=True)
    parser.add_argument("--click-id", required=True)
    parser.add_argument("--poll-s", type=float, default=20)
    parser.add_argument("--max-s", type=float, default=5400)
    return parser.parse_args(argv)


def status_url(base, click_id):
    query = urllib.parse.urlencode({"click-id": click_id})
    return f"{base.rstrip('/')}/api/alpha/wm/click?{query}"


def fetch(url):
    with urllib.request.urlopen(url, timeout=20) as response:
        return json.load(response)


def run(argv=None):
    args = arguments(argv)
    url = status_url(args.base, args.click_id)
    started = time.monotonic()
    consecutive_failures = 0

    while True:
        try:
            state = fetch(url)
            consecutive_failures = 0
        except (OSError, urllib.error.HTTPError, json.JSONDecodeError,
                UnicodeDecodeError) as error:
            consecutive_failures += 1
            print(f"child status route failed: {error}", file=sys.stderr)
            if consecutive_failures >= 2:
                return 1
            time.sleep(args.poll_s)
            continue

        child = state.get("child")
        if child is None:
            print("no child bound")
            return 0

        completion = child.get("completion") or {}
        status = completion.get("status")
        elapsed = time.monotonic() - started
        elapsed_s = int(elapsed)
        print(f"[{elapsed_s}s] child {child.get('click-id')} {status}")
        if status == "complete":
            return 0
        if elapsed >= args.max_s:
            print(f"child still pending after {int(args.max_s)}s", file=sys.stderr)
            return 1
        time.sleep(args.poll_s)


if __name__ == "__main__":
    raise SystemExit(run())
