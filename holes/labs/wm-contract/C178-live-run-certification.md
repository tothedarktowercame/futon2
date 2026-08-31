# C178 — one-command live-run certification

Date: 2026-08-31

`make certify-run RUN_ID=<uuid>` now locates the unique tick record carrying
that UUID and the unique bounded-wrapper JSON receipt whose execution interval
encloses the tick. It deliberately refuses to guess “latest”. Committed
receipts are the first lookup tier; the wrapper's transient spool is the
fallback. Ambiguity within either tier fails.

The command normalizes the wrapper receipt without re-measuring it, stamps the
resource input with the run UUID, invokes the existing operational-certificate
checker, and reports the run, topology hashes, traversal counts, resource
status, and verdict. The checker now rejects a resource UUID that differs from
the tick UUID while retaining compatibility with pinned legacy resource
fixtures that predate the association field.

Positive fixture:

```sh
make certify-run RUN_ID=00f4bf58-4da6-42bc-bb1d-5687e889e717
```

Result: exit 0, topology pin valid, 9 hops (3 original, 6 measured, 0
undeclared), resources clean, verdict `:pass`.

Missing-resource control:

```sh
bb -cp . checks/certify_live_run.clj \
  --run-id 00f4bf58-4da6-42bc-bb1d-5687e889e717 \
  --resource-dirs /tmp/c178-missing-resource-dir \
  --out-dir /tmp/c178-control
```

Result: exit 1, `bounded resource receipt missing`, with the UUID, run start,
and exact `/tmp/c178-missing-resource-dir/*.receipt.json` search path printed.

A second control changed only the normalized resource's `:run/id` to
`"wrong-run"`. The certificate reported
`:resource-run-identity-matches? false`, verdict `:fail`, and exited 1.

This is post-run certification only. It neither starts a tick nor selects a
mission; Joe's first current production-path tick remains unspent.
