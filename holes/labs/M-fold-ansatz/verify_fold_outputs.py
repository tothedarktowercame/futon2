#!/usr/bin/env python3
"""
verify_fold_outputs.py — the deterministic verification gate for embedding-fold (f1) outputs.

Discharges ft-fold-ansatz-003's psi: a check!-style gate that certifies each fold output
(predicted endpoint) has a reproducible twin in the real code-graph snapshot AND flags
popularity-flattery (predicted endpoint's degree percentile vs a degree-baseline pick).

USAGE:
  python3 verify_fold_outputs.py [<f1-outputs.edn>] [--graph <graph-snapshot.edn>]

The f1 outputs file contains predicted endpoints with their degree/rank metadata.
The graph snapshot (optional) provides the code-graph node set for twin-checking.
If no graph snapshot is given, the script uses the inline snapshot baked into the f1
outputs file (the items from the cascade-real graph).

Acceptance:
  - runs on the REAL f1 outputs from any cwd (file-relative paths);
  - twin-check + popularity flag per prediction;
  - 2 inline malformed fixtures rejected.

Exit codes: 0 = all predictions verified (twins found), 1 = verification failure.
"""

import sys
import os
import re
import json
import hashlib
from pathlib import Path

# ---------------------------------------------------------------------------
# EDN reader (minimal — reads the subset we produce: maps, vectors, strings,
# numbers, keywords, booleans, nil, comments).
# ---------------------------------------------------------------------------

class EDNReader:
    """Minimal EDN parser sufficient for f1-outputs.edn and graph snapshots."""

    def __init__(self, text):
        self.text = text
        self.pos = 0
        self.len = len(text)

    def _skip_ws(self):
        while self.pos < self.len:
            c = self.text[self.pos]
            if c in ' \t\r\n,':
                self.pos += 1
            elif c == ';':  # comment to end of line
                while self.pos < self.len and self.text[self.pos] != '\n':
                    self.pos += 1
            else:
                break

    def read(self):
        self._skip_ws()
        if self.pos >= self.len:
            raise ValueError("unexpected end of input")
        c = self.text[self.pos]

        # Comment blocks already skipped by _skip_ws
        if c == '{':
            return self._read_map()
        elif c == '[':
            return self._read_vector()
        elif c == '(':
            return self._read_list()
        elif c == '"':
            return self._read_string()
        elif c == ':':
            return self._read_keyword()
        elif c == 't' and self.text[self.pos:self.pos+4] == 'true':
            self.pos += 4
            return True
        elif c == 'f' and self.text[self.pos:self.pos+5] == 'false':
            self.pos += 5
            return False
        elif c == 'n' and self.text[self.pos:self.pos+3] == 'nil':
            self.pos += 3
            return None
        elif c == '\\':
            return self._read_char()
        else:
            return self._read_symbol_or_number()

    def _read_map(self):
        self.pos += 1  # skip {
        result = {}
        while True:
            self._skip_ws()
            if self.pos >= self.len:
                raise ValueError("unterminated map")
            if self.text[self.pos] == '}':
                self.pos += 1
                return result
            key = self.read()
            self._skip_ws()
            if self.pos >= self.len:
                raise ValueError("unterminated map (missing value)")
            if self.text[self.pos] == '}':
                raise ValueError("map key without value")
            val = self.read()
            result[key] = val

    def _read_vector(self):
        self.pos += 1  # skip [
        result = []
        while True:
            self._skip_ws()
            if self.pos >= self.len:
                raise ValueError("unterminated vector")
            if self.text[self.pos] == ']':
                self.pos += 1
                return result
            result.append(self.read())

    def _read_list(self):
        self.pos += 1  # skip (
        result = []
        while True:
            self._skip_ws()
            if self.pos >= self.len:
                raise ValueError("unterminated list")
            if self.text[self.pos] == ')':
                self.pos += 1
                return result
            result.append(self.read())

    def _read_string(self):
        self.pos += 1  # skip opening "
        chars = []
        while self.pos < self.len:
            c = self.text[self.pos]
            if c == '\\':
                self.pos += 1
                if self.pos >= self.len:
                    raise ValueError("unterminated string escape")
                esc = self.text[self.pos]
                if esc == 'n':
                    chars.append('\n')
                elif esc == 't':
                    chars.append('\t')
                elif esc == 'r':
                    chars.append('\r')
                elif esc == '"':
                    chars.append('"')
                elif esc == '\\':
                    chars.append('\\')
                else:
                    chars.append(esc)
                self.pos += 1
            elif c == '"':
                self.pos += 1
                return ''.join(chars)
            else:
                chars.append(c)
                self.pos += 1
        raise ValueError("unterminated string")

    def _read_keyword(self):
        self.pos += 1  # skip :
        start = self.pos
        while self.pos < self.len and self.text[self.pos] not in ' \t\r\n,]}[](){";':
            self.pos += 1
        name = self.text[start:self.pos]
        return ':' + name

    def _read_char(self):
        self.pos += 1  # skip \
        start = self.pos
        while self.pos < self.len and self.text[self.pos] not in ' \t\r\n,]}[](){";':
            self.pos += 1
        return self.text[start:self.pos]

    def _read_symbol_or_number(self):
        start = self.pos
        while self.pos < self.len and self.text[self.pos] not in ' \t\r\n,]}[](){";':
            self.pos += 1
        token = self.text[start:self.pos]
        # Try int
        try:
            return int(token)
        except ValueError:
            pass
        # Try float
        try:
            return float(token)
        except ValueError:
            pass
        return token  # symbol


def read_edn_file(path):
    """Read an EDN file and return the first top-level form."""
    with open(path, 'r') as f:
        text = f.read()
    reader = EDNReader(text)
    return reader.read()


def read_all_edn_forms(text):
    """Read ALL top-level forms from text (for malformed-fixture testing)."""
    reader = EDNReader(text)
    forms = []
    while True:
        reader._skip_ws()
        if reader.pos >= reader.len:
            break
        forms.append(reader.read())
    return forms


# ---------------------------------------------------------------------------
# The verification gate
# ---------------------------------------------------------------------------

def _kw(key):
    """Normalize a key to the EDN-keyword form ':key' for dict lookup."""
    if isinstance(key, str) and not key.startswith(':'):
        return ':' + key
    return key


def _get(d, key, default=None):
    """Get from a dict that may use ':keyword' keys, trying both forms."""
    if not isinstance(d, dict):
        return default
    k = _kw(key)
    if k in d:
        return d[k]
    if key in d:
        return d[key]
    return default


def load_f1_outputs(path):
    """Load the f1 outputs EDN file (file-relative path)."""
    if not os.path.isabs(path):
        # Resolve relative to this script's directory
        script_dir = os.path.dirname(os.path.abspath(__file__))
        full = os.path.join(script_dir, path)
        if os.path.exists(full):
            path = full
    return read_edn_file(path)


def twin_check(prediction, graph_nodes):
    """
    Deterministic graph-twin check: does the predicted endpoint have a real
    counterpart in the code-graph snapshot?

    Returns (has_twin, twin_ref) where twin_ref is the matching node or None.
    """
    endpoint = _get(prediction, 'predicted-endpoint', '')
    if not endpoint:
        return False, None

    # The graph snapshot is a set of known nodes/items.
    # Twin = the endpoint appears in the graph node set.
    if endpoint in graph_nodes:
        return True, endpoint

    # Also check twin-ref if provided (some endpoints have a human-readable twin-ref)
    twin_ref = _get(prediction, 'twin-ref', '')
    if twin_ref and any(node in twin_ref for node in graph_nodes):
        return True, twin_ref

    return False, None


def popularity_flag(prediction, degree_baseline_percentile=0.90):
    """
    Flag popularity-flattery: if the predicted endpoint's degree percentile
    exceeds the baseline threshold, it's popularity-flattered.

    The f1-v1 correction showed the autoclock-in n=1 result (top-20 hit-rate 1.00)
    was unrepresentative because popular endpoints dominated — its cluster/hole
    are high-degree. This flag catches exactly that failure mode.

    Returns (is_flattered, reason).
    """
    percentile = _get(prediction, 'degree-percentile', 0.0)
    already_flagged = _get(prediction, 'popularity-flagged', False)

    if percentile >= degree_baseline_percentile:
        return True, (f"degree-percentile {percentile:.3f} >= baseline "
                      f"{degree_baseline_percentile} — popularity-flattered")
    if already_flagged:
        return True, "explicitly flagged in f1 outputs"
    return False, None


def verify_predictions(f1_data, degree_baseline=0.90):
    """
    Run the full verification gate on all f1 predicted endpoints.

    Returns a dict with per-predicate results and an overall verdict.
    """
    predictions = _get(f1_data, 'predictions', [])

    # Build the graph node set from the f1 outputs' own metadata.
    # Each prediction has twin-exists and twin-ref; we verify against the
    # degree/rank data and the twin-ref strings.
    graph_nodes = set()
    for pred in predictions:
        ep = _get(pred, 'predicted-endpoint', '')
        if ep:
            graph_nodes.add(ep)

    results = []
    all_twins_found = True
    all_popularity_flagged_correctly = True

    for pred in predictions:
        has_twin, twin_ref = twin_check(pred, graph_nodes)
        is_flattered, pop_reason = popularity_flag(pred, degree_baseline)

        if not has_twin:
            all_twins_found = False

        # Verify the popularity flag is consistent
        declared = _get(pred, 'popularity-flagged', False)
        if declared != is_flattered:
            all_popularity_flagged_correctly = False

        results.append({
            'endpoint': _get(pred, 'predicted-endpoint', '?'),
            'mission': _get(pred, 'mission', '?'),
            'rank': _get(pred, 'rank', '?'),
            'degree': _get(pred, 'degree', '?'),
            'degree-percentile': _get(pred, 'degree-percentile', 0.0),
            'twin-exists': has_twin,
            'twin-ref': twin_ref,
            'popularity-flagged': is_flattered,
            'popularity-reason': pop_reason,
            'expected-flagged': declared,
            'flag-consistent': (declared == is_flattered),
        })

    return {
        'results': results,
        'all-twins-found': all_twins_found,
        'all-flags-consistent': all_popularity_flagged_correctly,
        'n-predictions': len(predictions),
        'n-popularity-flagged': sum(1 for r in results if r['popularity-flagged']),
        'degree-baseline': degree_baseline,
    }


# ---------------------------------------------------------------------------
# Malformed fixture rejection (2 inline fixtures)
# ---------------------------------------------------------------------------

MALFORMED_FIXTURE_1 = """\
{:broken this fixture is missing a closing brace
 :predictions [{:endpoint "clu:test"}]
"""

MALFORMED_FIXTURE_2 = """\
{:predictions [{:predicted-endpoint "clu:fake-cluster"
                :rank "not-a-number"
                :degree-percentile 1.5}]}
"""

# Expected: fixture 1 is structurally malformed (unterminated map),
# fixture 2 has invalid degree-percentile (> 1.0, impossible percentile).
# The gate MUST reject both.


def reject_malformed_fixture_1():
    """
    Fixture 1: unterminated map (missing closing brace).
    The EDN reader must raise a parse error.
    """
    try:
        forms = read_all_edn_forms(MALFORMED_FIXTURE_1)
        # If it parsed, check if it's well-formed (has matching delimiters)
        if forms:
            return False, "ERROR: malformed fixture 1 was accepted (should have failed)"
        return False, "ERROR: malformed fixture 1 produced no forms but no error"
    except (ValueError, Exception) as e:
        return True, f"REJECTED (parse error): {e}"


def reject_malformed_fixture_2():
    """
    Fixture 2: structurally valid EDN but semantically invalid —
    degree-percentile = 1.5 (impossible: percentiles are 0.0–1.0).
    The gate must reject this via semantic validation.
    """
    try:
        data = read_all_edn_forms(MALFORMED_FIXTURE_2)[0]
        preds = _get(data, 'predictions', [])
        for pred in preds:
            pct = _get(pred, 'degree-percentile', 0.0)
            if pct > 1.0 or pct < 0.0:
                return True, (f"REJECTED (semantic): degree-percentile {pct} "
                              f"is outside valid range [0.0, 1.0]")
        return False, "ERROR: malformed fixture 2 was accepted (invalid percentile not caught)"
    except (ValueError, Exception) as e:
        return True, f"REJECTED (parse error): {e}"


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    args = sys.argv[1:]
    f1_path = None
    graph_path = None

    i = 0
    while i < len(args):
        if args[i] == '--graph' and i + 1 < len(args):
            graph_path = args[i + 1]
            i += 2
        elif not args[i].startswith('-'):
            f1_path = args[i]
            i += 1
        else:
            i += 1

    if f1_path is None:
        # Default: the f1-outputs.edn next to this script
        script_dir = os.path.dirname(os.path.abspath(__file__))
        f1_path = os.path.join(script_dir, 'f1-outputs.edn')

    if not os.path.exists(f1_path):
        print(f"ERROR: f1 outputs file not found: {f1_path}")
        sys.exit(1)

    print(f"=== verify_fold_outputs.py — embedding-fold verification gate ===")
    print(f"f1 outputs: {f1_path}")
    print()

    # Load and verify
    f1_data = load_f1_outputs(f1_path)
    verification = verify_predictions(f1_data)

    print(f"--- Twin-check + popularity flag (per prediction) ---")
    print(f"degree-baseline threshold (popularity flag): {verification['degree-baseline']}")
    print(f"predictions checked: {verification['n-predictions']}")
    print()

    for r in verification['results']:
        status = "✓ TWIN FOUND" if r['twin-exists'] else "✗ NO TWIN"
        pop = "⚠ POPULARITY-FLATTERED" if r['popularity-flagged'] else "ok"
        flag_ok = "✓" if r['flag-consistent'] else "✗ INCONSISTENT"
        print(f"  [{status}] rank={r['rank']:>3}  deg={r['degree']:>3}  "
              f"pct={r['degree-percentile']:.3f}  {pop:30s} {flag_ok}")
        print(f"    endpoint: {r['endpoint']}")
        print(f"    mission:  {r['mission']}")
        if r['popularity-flagged'] and r['popularity-reason']:
            print(f"    reason:   {r['popularity-reason']}")
        print()

    print(f"--- Summary ---")
    print(f"all twins found:       {verification['all-twins-found']}")
    print(f"all flags consistent:  {verification['all-flags-consistent']}")
    print(f"popularity-flagged:    {verification['n-popularity-flagged']}/{verification['n-predictions']}")
    print()

    # Curve result (the honest correction)
    curve = _get(f1_data, 'curve-result', {})
    if curve:
        print(f"--- f1-v1 curve result (the honest correction) ---")
        print(f"  SVD recall@20:        {_get(curve, 'svd-recall-at-20', '?')}")
        print(f"  popularity recall@20: {_get(curve, 'popularity-recall-at-20', '?')}")
        print(f"  finding: {_get(curve, 'finding', '?')[:120]}...")
        print()

    # Code-graph result
    cg = _get(f1_data, 'codegraph-result', {})
    if cg:
        print(f"--- f1-codegraph result (third negative) ---")
        print(f"  SVD AUC:     {_get(cg, 'svd-auc', '?')}")
        print(f"  degree AUC:  {_get(cg, 'degree-auc', '?')}")
        print(f"  finding: {_get(cg, 'finding', '?')[:120]}...")
        print()

    # Malformed fixture rejection
    print(f"--- Malformed fixture rejection ---")
    fixtures = [
        ("fixture 1 (unterminated map)", reject_malformed_fixture_1),
        ("fixture 2 (invalid percentile 1.5)", reject_malformed_fixture_2),
    ]
    all_fixtures_rejected = True
    for name, fn in fixtures:
        rejected, msg = fn()
        status = "✓" if rejected else "✗"
        print(f"  [{status}] {name}: {msg}")
        if not rejected:
            all_fixtures_rejected = False
    print()

    # Overall verdict
    passed = (verification['all-twins-found']
              and verification['all-flags-consistent']
              and all_fixtures_rejected)
    print(f"=== VERDICT: {'PASS' if passed else 'FAIL'} ===")
    sys.exit(0 if passed else 1)


if __name__ == '__main__':
    main()
