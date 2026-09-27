#!/usr/bin/env python3
"""Publish the checkpoint summary and plan from the same committed revision.

The HTML template contains historical material only. Pandoc renders the marked
current section of PROOF-2a-PLAN.md; no second status table is maintained here.
This publishes documentation, not generated diagrams or an acceptance decision.
"""
import argparse
import hashlib
import os
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
BASE = "holes/labs/wm-contract/"
START = "<!-- PROOF-2a-current:start -->"
END = "<!-- PROOF-2a-current:end -->"
PLACEHOLDER = "<!-- PROOF-2a-current:rendered -->"


def git(*args):
    return subprocess.check_output(["git", "-C", str(ROOT), *args])


def render(revision):
    revision = git("rev-parse", "--verify", revision + "^{commit}").decode().strip()
    script_path = Path(__file__).resolve().relative_to(ROOT).as_posix()
    if git("show", f"{revision}:{script_path}") != Path(__file__).read_bytes():
        raise ValueError("publisher must match its committed version at --rev")
    plan = git("show", f"{revision}:{BASE}PROOF-2a-PLAN.md")
    template = git("show", f"{revision}:{BASE}proof-2a-checkpoint.html").decode()
    source = plan.decode()
    if source.count(START) != 1 or source.count(END) != 1:
        raise ValueError("plan must contain exactly one current-section marker pair")
    start, end = source.index(START) + len(START), source.index(END)
    if end <= start or template.count(PLACEHOLDER) != 1:
        raise ValueError("invalid current section or template placeholder")
    body = subprocess.run(
        ["pandoc", "--from=gfm", "--to=html5"],
        input=source[start:end], text=True, capture_output=True, check=True,
    ).stdout
    digest = hashlib.sha256(plan).hexdigest()
    block = (
        '<section id="current-build"><h2>Current build state</h2>\n'
        f'<p>Published from futon2 <code>{revision}</code>. '
        '<a href="PROOF-2a-PLAN.md">The plan</a> is the source of this section; '
        'the older checkpoint and figures are below.</p>\n'
        f'<!-- plan-sha256: {digest} -->\n{body}</section>'
    )
    outputs = {
        "PROOF-2a-PLAN.md": plan,
        "proof-2a-checkpoint.html": template.replace(PLACEHOLDER, block).encode(),
    }
    # Keep the audit and historical slice links on the checkpoint resolvable at
    # the same documentation revision. Their own evidence pins are unchanged.
    for name in ("PREFERENCE-AUDIT.md", "PROOF-2a-CODEX-CONTINUATION-2026-09-26.md",
                 "PROOF-2a-CODEX-SLICE-2-2026-09-26.md", "PROOF-2a-CODEX-ITEMS-1-5.md"):
        outputs[name] = git("show", f"{revision}:{BASE}{name}")
    return revision, digest, outputs


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--rev", required=True, help="committed source revision")
    parser.add_argument("--output-dir", type=Path,
                        default=Path("/var/www/zone.hyperreal.enterprises/wip"))
    args = parser.parse_args()
    revision, digest, outputs = render(args.rev)
    if not args.output_dir.is_dir():
        raise ValueError("output directory must already exist")
    # Render and stage every file before replacing any public file. Each rename
    # is atomic; the pair is not a filesystem transaction. The embedded digest
    # lets a reader detect a mixed pair during the brief publication interval.
    staged = []
    try:
        for name, content in outputs.items():
            with tempfile.NamedTemporaryFile(dir=args.output_dir, delete=False) as f:
                staged.append((Path(f.name), args.output_dir / name))
                f.write(content)
            os.chmod(f.name, 0o644)
        for temporary, target in staged:
            os.replace(temporary, target)
    finally:
        for temporary, _ in staged:
            temporary.unlink(missing_ok=True)
    print(f"Published {len(outputs)} files from {revision}; plan sha256 {digest}")


if __name__ == "__main__":
    main()
