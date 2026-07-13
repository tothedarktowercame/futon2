HANDOFF ZU-1 — Z1-PROMOTE + STATUS ORACLE (M-zaif-harness upgrade plan, 2026-07-13)
Goal: promote futon2/holes/labs/M-zaif-harness/z1_views.clj to runner-grade AND add
a mission-status view: status DERIVED from event stream (close/checkpoint commits,
fold events, mission-doc hyperedges on :7073), doc header = one signal, :stale-header?
flag on disagreement. Plain HTTP, agent-agnostic.
Acceptance: (a) M-mission-conditional-reward answers CLOSED 2026-07-12 from events
alone; (b) :stale-header? fires on that doc's pre-fix state (git show d9f38f3^:...);
(c) one-curl callable by any agent. Gates: clj-kondo + check-parens.el + relevant
tests; 30-min job cap (background long verifies); doc checkpoint in the same commit
as the landing (B8 reverse-drift rule). Bell claude-2 back with summary + shas.
