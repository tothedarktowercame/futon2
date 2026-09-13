# Row 26 r2 cohort staging

Route: bounded server-owned plumbing, not RUN4.  Ordinary click requests cannot
supply `:execution-cohort`; after review and reload of
`futon3c.wm.machinery-execution-cohort` and `futon3c.wm.runner-service`, the
runner reads the fixed digest-pinned Futon3c binding.  A RUN4-prepared request
which already contains its separately validated server cohort is preserved.

No serving namespace was loaded and no click was made by this packet.  The
cohort activation is staged with zero attempts.  Cohort records explicitly say
that all three opportunities are machinery tests, never qualifying candidates.

After independent review and the named reload, the exact r2 request is:

```http
POST /api/alpha/wm/click
Content-Type: application/json

{"run-id":"wm-machinery-test-2026-09-13-claude-15-r2","author":"claude-15","reviewer":"codex-22","repair-reviewer":"codex-24","trigger":"duree-click-on-demand"}
```

The POST body contains no cohort path, id, or digest.
