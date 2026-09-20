# Observation reference resolution

Implementation `78391df4`, merged to main `b81f1fa2`.

C3/C4/C5 preserve the supplied reference under `:evidence :sha` and add
`:resolved-sha`, obtained once with `git rev-parse --verify --end-of-options
<reference>^{commit}`. Subsequent content commands use that immutable result.
Unresolvable references retain the existing typed `:unknown-sha` refusal and
name the supplied repo/reference. Missing locator fields remain `:no-locator`.

C5 resolves the bundle reference separately from each declaration locus's HEAD.
The existing boolean `:clojure-loci` map is preserved. `:locus-evidence` adds each
locus's observation and repo, supplied HEAD, resolved commit, path and line.
An unresolvable locus HEAD refuses the C5 observation, naming the locus; it does
not become a false observation or a nil resolved SHA. Malformed locus strings
remain unsuccessful observations, as before. Multiple loci may have different
resolved HEADs; each recorded result uses its own resolved commit.

The narrow namespace passes 3 tests / 41 assertions using temporary Git repos.
Control 4 was constructed: identical declarations are observed before and after
a real commit, producing different resolved SHAs/evidence. C4's boolean also
changes when the declaration head changes. Another control advances HEAD after
resolution but before checking content: the observation still reflects the
recorded old commit. C5 proves that a pinned older bundle and the current locus
HEAD can differ and both are retained. Full-SHA inputs resolve to themselves;
unknown main references and locus HEADs refuse explicitly.

clj-kondo: 0 errors, 0 warnings. check-parens: OK. The existing good record
`tick-run-record-2026-09-20-1789862860.edn` receives byte-identical validity output.
C1/C2 handlers, observation contract and cascade declarations are unchanged.
No WM click, serving reload, or live data edit occurred.

Required serving reload: `futon2.aif.observation-checks` from canonical main.
Reloading it also rebuilds its `checks` dispatch map. Neither
`futon2.aif.cascade-sources` nor `futon2.report.war-machine` was edited; their
calls go through the observation namespace's Vars, so this change does not
require their reload. This statement does not clear any earlier pending reload.

Warrant: `test-registry-ab8c87fde403bc490de684397830d9acdfdcec8d49895282c7f25725d98c9d55`; HTTP check `warrant? true`.
