"""Faithful Python port of futon2.aif.arguing-worlds/score-buildout-c.

The GENERATION proxy for the GFlowNet entrant (the contest's Goodhart guard:
C may generate, C never judges). Ported line-for-line from arguing_worlds.clj
and cross-checked mechanically against the Clojure scorer (check_c_proxy.bb)
before any training run — the exit-1 obligation.
"""


def move_token_set(move: dict) -> frozenset:
    return frozenset(
        v for v in (
            move.get("move_id"), move.get("move_class"),
            move.get("have"), move.get("want"),
            move.get("advances-cap"), move.get("confidence"),
        ) if v is not None)


def jaccard(a: frozenset, b: frozenset) -> float:
    union = a | b
    if not union:
        return 1.0
    return len(a & b) / len(union)


def score_buildout_c(policy: list[dict]) -> float:
    intensity = sum(float(m.get("score") or 0.0) for m in policy)
    tokens = [move_token_set(m) for m in policy]
    pairs = [jaccard(tokens[i], tokens[j])
             for i in range(len(tokens)) for j in range(i + 1, len(tokens))]
    harmony = (sum(4.0 * p * (1.0 - p) for p in pairs) / len(pairs)) if pairs else 1.0
    return intensity * harmony
