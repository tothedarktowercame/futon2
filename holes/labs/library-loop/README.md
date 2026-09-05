# Library loop: theory track

This lane is commissioned by Joe's 2026-09-05 theory-track ruling in
`../wm-contract/EPIC-run-era.md`:

> we should ideally find a way to get the 'theory' part
> moving, possibly with Zai agents to balance my usage. [Deleuze on
> problems/singularities/events, epochemagazine.org/34, as philosophical
> frame] the programme here would continue the library loop, mining the
> evidence landscape and annotating patterns, so that we get a sense of
> what problems we have been solving historically. even though we only
> have 20 nodes and a comparable number of WR patterns, we have a whole
> library of other patterns, and it would be good if this would all come
> together, e.g., the process observations we made recently might become
> new 'library/process' patterns. Right now most patterns would be
> unreachable via @how or @why traversal, ie. they have no globally
> recognised rationale. While this is clearly different from the Lean
> issues, ultimately we will want graph-level certificates too, and
> these tracks should eventually merge around on the fly cascade
> construction and processing

Seat law: `zai-1` authors, Codex reviews, and `claude-1` owns the lane and
receives stop bells. The loop uses Agency's synchronous whistle for the
registered Zai seat and does not start unless an operator runs it.

The merge direction is explicit: graph certificates from library rationale
and runtime certificates from the AIF/Lean track converge where cascades are
constructed and processed. L4 measures candidate refusal rules; it does not
choose or install one.

After review, an operator may start the lane with:

```sh
cd /home/joe/code/futon2/holes/labs/library-loop
nohup ./library-build-loop.sh > /tmp/library-build-nohup.out 2>&1 &
```

Do not run two copies; the flock guard refuses the second instance.
