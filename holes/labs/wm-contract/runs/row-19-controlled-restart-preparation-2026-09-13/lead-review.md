# Controlled restart preparation review — codex-26

Reviewed26d5a1dc/89919c59/aa6add26. Two source/test files and two runbook/script files match their named commit bytes (lead-pins.json); retained2tests14assertions/kondo/actualparens inspected without rerun. Historical malformed parens attempt retained. HTTP activation entry now unconditionally refuses. The generic offline helper blocks futon3c.* targets only; do not claim it is a sandbox against all production namespaces or arbitrary evaluated forms.

The read-only preflight currently observes missing acceptance files, absent archive directory, and dirty source tree. It is not executable deployment approval:

1. The two acceptance paths are checked only with -f. Empty/arbitrary files satisfy those conditions; no source/ingress subject or authorized review is validated. Its status must remain explicitly discovery/unverified until concrete independently authenticated acceptance readers exist.
2. Shell printf %q produces shell escaping, not EDN strings. Retain valid machine-readable output and exact stdout/stderr with explicit exit receipts; bash -n plus invocation separated by semicolon does not prove syntax status. Do not represent shell fragments as structured EDN evidence.
3. Step4 permits prior-tree restart after startup failure despite startup recovery/compaction possibly creating evidence. This contradicts no old-code rollback after evidence publication. Require roll-forward after any possible new evidence; absent proven pre-write failure, old-tree fallback is unsafe.
4. Steps5-7 require reconnect/probe before ingress release without an actual scoped verification lane; waiting for expiry/archive also lacks a bounded plan. Specify a real selective probe lane or blocked condition, never global D13 clock manipulation or seven-day unattended wait.

Next bounded read-only work should enumerate actual service stop/drain/creator/internal-resume surfaces and source/dependency changes needed for startup acceptance. Distinguish observed state, architectural missing controls, and mere prose prerequisites. No empty files, fixed names, or blanket clean-tree requirement substitutes for independent review of an exact source snapshot. Do not discard unrelated dirty work. No live command, restart, archive provisioning, backup, or probe is authorized in this packet.
