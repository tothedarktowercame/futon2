# A5 problem-authoring batch 2 receipt

Date: 2026-09-06. Baseline gate: `16/53 refused`. This round first marked
the four A4 roots, then searched project documents before consulting the
eleven source patterns. Every authored node carries `source-class:` in-band.

## A4 marking sub-task

Futon3 `f804c09` adds `source-class: self-derived` to:

- `problems/undeclared-cascades-cannot-be-audited`
- `problems/ai-assisted-effort-needs-two-resources`
- `problems/unqueryable-trails-block-recovery`
- `problems/uncoupled-pheromone-becomes-noise`

No graph edge or pattern body changed in that commit.

## Batch results

### Independent sources: 5 patterns, 3 nodes (`81126d0`)

- `equity/confident-adaptation-as-published` and
  `equity/confident-adaptation-merged` ->
  `problems/trainers-lack-a-fixed-flexible-specification`.
  `source-class: independent`. Verbatim: “It wasn't very clear to us how
  many sessions we needed to have attended, which ones were fundamental …
  and which ones then were optional.” Source: futon5a
  `essays/ukrn-open-research-training-plos-one/ukrn-open-research-training-plos-one-v2.5-submission.md:146`.

- `equity/contribution-pathways` and
  `equity/contribution-pathways-merged` ->
  `problems/trainers-lack-collaborative-contribution-access`.
  `source-class: independent`. Verbatim: “However, not all participants felt
  they had the same opportunities for collaboration. Some participants
  described limited access to relevant peers, and being seen as the only
  person responsible for training delivery.” Source: the same futon5a
  manuscript, line 183.

- `equity/protected-interaction-as-published` ->
  `problems/trainers-feel-unequipped-to-deliver`.
  `source-class: independent`. Verbatim: “Trainers value the course but feel
  unequipped to deliver”. Source: the same futon5a manuscript, line 201.

### Marked self-derived fallbacks: 6 patterns, 6 nodes

- `ai4ci/ai-core-function` ->
  `problems/unlinked-reasoning-cues-require-rediscovery` (`6701361`).
  Verbatim fallback: “These informal cues are not systematically linked to
  specific steps or objects; without assistance, annotators must rediscover
  every linkage by hand, and without structure, downstream tools receive
  only cleaned events with little reusable explanation.” Source pattern
  `library/ai4ci/ai-core-function.flexiarg:26-29`.

- `ai4ci/funding-justification` ->
  `problems/unitemized-ai4ci-budget-looks-speculative` (`6701361`).
  Verbatim fallback: “Without a clear ledger of what the budget buys in
  terms of specific activities (specialist annotation, light AI usage,
  seminar facilitation), the request can look speculative or
  infrastructure-heavy.” Source pattern
  `library/ai4ci/funding-justification.flexiarg:26-28`.

- `ai4ci/pilot-feasibility` ->
  `problems/ai4ci-scope-can-outgrow-six-month-pilot` (`6701361`).
  Verbatim fallback: “Without explicit scope fences, a project framed around
  “AI and explanation” can drift into trying to develop storage, user
  interfaces, or new models, making the schedule unrealistic.” Source
  pattern `library/ai4ci/pilot-feasibility.flexiarg:25-27`.

- `gauntlet/modeline-persists-across-worlds` ->
  `problems/peripheral-hops-reset-self-regulation` (`0cde9d4`). Verbatim
  fallback: “Each peripheral hop replaces the agent's world (P-1). Without a
  persistent surface, AIF state resets at each transition: the agent enters a
  new peripheral with no memory of how confident it was, how much budget
  remains, or what pattern it was carrying.” Source pattern
  `library/gauntlet/modeline-persists-across-worlds.flexiarg:19`.

- `gauntlet/teaching-inversion` ->
  `problems/agent-discoveries-do-not-teach-humans` (`0cde9d4`). Verbatim
  fallback: “The bottleneck inverts: the agent finds things the human hasn't
  seen, but has no mechanism to teach them back.” Source pattern
  `library/gauntlet/teaching-inversion.flexiarg:19`.

- `ukrns/publication-cadence` ->
  `problems/undeclared-publication-cadence-obscures-version` (`7c653bb`).
  Verbatim fallback: “Without a declared cadence, "which version am I
  reading" becomes unanswerable.” Source pattern
  `library/ukrns/publication-cadence.flexiarg:15`.

The rejected AI4CI near-matches merely named two proposed grants or generic
human review; they did not state the cue-linkage, itemized-budget, or scope
drift problems. The gauntlet near-matches described peer learning, HUDs, and
peripheral mechanics, but did not state dynamic-state loss or the missing
agent-to-human teaching channel. The publication near-matches described
living documents and revision history, but did not state version ambiguity.
Those six therefore use their own committed problem statements and say so
mechanically; none is presented as independent evidence.

No target remained unauthored: **5 independent, 6 self-derived, 0
unauthored**, counted at pattern grain.

## External-first search trail

Repositories searched: `/home/joe/code/futon2`, `futon4`, `p4ng`, `futon3`
(with the four target families excluded), `futon5a`,
`ukrn-services-simulation`, and `futon6`. The final enumeration used four
query families (equity/training; AI4CI; gauntlet; publication) across all
seven repositories:

```sh
: > /tmp/a5-final-search-enumeration.txt
for spec in \
'equity|fixed.{0,30}flexible|how many sessions|fundamental.{0,30}optional|contribution|collaboration|relevant peers|only person responsible|unequipped|impostor' \
'AI4CI|mid-level reasoning|reasoning cues|annotation pilot|methodological evaluation|six.month|scope fence|funding.{0,30}(budget|request)' \
'modeline|peripheral hop|teach.{0,30}human|human.{0,30}learn|insight.{0,30}result|agent.{0,30}discover' \
'publication cadence|which version|citation ambig|living document|frozen snapshot|citable snapshot'
do
  for repo in /home/joe/code/futon2 /home/joe/code/futon4 \
              /home/joe/code/p4ng /home/joe/code/futon3 \
              /home/joe/code/futon5a \
              /home/joe/code/ukrn-services-simulation /home/joe/code/futon6
  do
    printf 'QUERY\t%s\tREPO\t%s\n' "$spec" "$repo" >> /tmp/a5-final-search-enumeration.txt
    git -C "$repo" grep -I -i -l -E "$spec" HEAD -- \
      '*.md' '*.tex' '*.edn' '*.flexiarg' ':!data/**' ':!**/.history/**' \
      ':!library/equity/**' ':!library/ai4ci/**' \
      ':!library/gauntlet/**' ':!library/ukrns/**' 2>/dev/null \
      | sort >> /tmp/a5-final-search-enumeration.txt || true
  done
done
wc -l -c /tmp/a5-final-search-enumeration.txt
sha256sum /tmp/a5-final-search-enumeration.txt
```

The enumeration completed without truncation: 716 lines, 41,958 bytes,
SHA-256 `168976a2d03ca3f22bbea4d0dcbf3088b512b5d1310c84bac81d3496f55ad2f8`.
Candidate files were subsequently inspected with line-numbered `rg -n` and
`nl -ba`; the three independent quotes above were checked directly against
futon5a HEAD.

## Gate and parsing

The census was regenerated from futon3 `7c653bb` and the same
`l17_advisory_report.py` extraction used in A1-A4 was run with
`down-problems+wr`. Before: `16/53 refused`. After: `5/53 refused` — delta
**-11**. The remainder is exactly `hdm/non-capture`,
`math-formalization-CA/measure-integration-api`,
`math-formalization-CV/frontier-bound-from-arc-hypotheses`,
`math-formalization-CV/holomorphic-disk-api`, and
`math-strategy/missing-dependency-protocol`.

`l2_parse_gate.py` results: problems 58/0 failures; isolated touched-family
sets equity 5/0, AI4CI 3/0, gauntlet 2/0, UKRNS 1/0. The isolation avoids
claiming that unrelated pre-existing unannotated files in those families
were repaired. `git diff --check` passed. Unrelated futon3 index and math
pattern changes and futon2 build-loop runtime files were not committed.
