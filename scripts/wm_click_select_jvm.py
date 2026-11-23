#!/usr/bin/env python3
"""Select candidate JVM PIDs whose process cwd is the expected checkout."""

import os
import sys
from pathlib import Path


def matching_pids(candidates, expected_cwd, proc_root="/proc"):
    expected = Path(expected_cwd).resolve()
    matches = []
    for candidate in candidates:
        try:
            cwd = (Path(proc_root) / candidate / "cwd").resolve(strict=True)
        except (OSError, RuntimeError):
            continue
        if cwd == expected:
            matches.append(candidate)
    return matches


def main():
    if len(sys.argv) != 2:
        raise SystemExit("usage: wm_click_select_jvm.py EXPECTED_CWD")
    candidates = [line.strip() for line in sys.stdin if line.strip()]
    proc_root = os.environ.get("WM_CLICK_PROC_ROOT", "/proc")
    print("\n".join(matching_pids(candidates, sys.argv[1], proc_root)))


if __name__ == "__main__":
    main()
