# WMC source authoring: activation evidence required

Base main: `21849afe3092060cf1cebbe299b70acc275df3ff`.
Branch: `fix/wmc-scaling-source`. No declaration, production code, test fixture,
store, or serving JVM was changed. No click or retrieval generation was run.

The requested source would make two WMC implementation alternatives executable.
The mission is explicitly parked until one of two activation events:

- A live decision records `:observation-universe-out-of-bounds`.
- A demonstrated case shows rate uncertainty at current sample sizes flipping
  a ranking.

Authority: `holes/M-a-wmc-scaling.md:114–124`, “Activation triggers (per the
parked-appendix ruling)”. It says whoever encounters either event names it as
the trigger before the mission leaves IDENTIFY. Its scope-out section also
excludes current plain-A work and calibration collection.

The current mission SHA256 is
`40a494e8d9f24c344cc473637f8f0a2ca64feaf4f836092f834493d3044a0220`.
The retained retrieval bundle
`data/wm-cascade-proposals/8961ab24-0c3a-4e7c-adbd-7413b5e3e5fa/proposal.edn`
pins that same mission digest and quotes the activation section in its target
citations. This is therefore also a condition of the supplied proposal evidence,
not merely a later change in a live mission document.

A bounded read-only search of retained `data/wm-runs` and `data/wm-trace` found
no `:kind :observation-universe-out-of-bounds` event. General mentions of that
keyword occur in quoted mission text and are not activation events. No qualifying
rate-uncertainty ranking-flip record was identified in the inspected material.
This is not a proof that no qualifying record exists anywhere.

The existing two declaration examples were read, as were retained source bytes
for `aif/term-to-channel-traceability`,
`data-mining/smoke-before-the-paid-run`,
`pattern-interpretation/put-uncertainty-in-theta`, and
`peripherals/read-existing-seam-before-implementing`. Their mechanisms do not
waive the activation rule. For example, a smoke test or seam read is not by itself
a completed compiled-query acceptance task. No claim is made that all 48 retrieved
patterns are inapplicable; the activation conflict blocks executable authoring
before that judgment can justify the requested admission.

My preceding `admission-2026-09-22/DISCOVERY.md` recommendation of this mission as
the smallest new executable source was premature: it counted the open checkboxes
and retrieval evidence without carrying this activation condition into the
recommendation. Five open checkboxes do not authorize activating a parked mission.

To resume, the owner must identify a qualifying trigger record, or explicitly
revise the activation ruling and its mission declaration. Then the declaration
can cite that authority and its frozen fixture can preserve it. A synthetic
out-of-bounds unit test would not establish the required live event. Neither
omitting the activation guard nor manufacturing a positive guard observation is
an acceptable solution under `/home/joe/code/AGENTS.md`'s prohibition on bypassing
invariants.

No scoped warrant or passing admission tests are claimed: there is no executable
change to warrant. Document gates: `git diff --check` and
`git diff --cached --check` passed. The requested declaration and test namespace
remain unimplemented pending the activation evidence/ruling.
