# Row 18 production snapshot activation boundary

The lead touch-up at `28f35c21` was independently inspected. Its three source
hashes equal `lead-source-protocol-pins.json`; its retained commands, exits,
and 10-test/32-assertion result agree with `lead-touchup-gates.json`. No rerun
was performed because no mismatch was found.

Production activation requires operator work outside the writer JVM:

1. Provision `/run/futon2` root-owned and not group/world writable. Provision
   `wm-interoceptive-snapshot.lock` there as a regular, non-symlink file, owned
   by the service user. The parent ownership prevents the service user from
   replacing the inode; the receipt pins its `fileKey`, owner, parent owner,
   and parent writability.
2. Provision the second root-controlled
   `/run/futon2/wm-interoceptive-deployment.lease` inode. Every authorized
   process launch and every namespace reload affecting a writer must acquire
   this lease exclusively before changing the deployed generation. Snapshot
   capture holds it from receipt resolution through completed logical readback.
   The verifier requires four separately pinned controller artifacts:
   systemd start/restart, Drawbridge/proof-eval reload, dev-admin load-file,
   and direct in-JVM `require :reload`. Today the last surface has no enforced
   controller, so an honest receipt cannot claim activation. Its future
   controller must increment/check a deployment generation or mechanically
   acquire the lease at the evaluator boundary; convention is insufficient.
3. Deploy the exact pinned lock, manifest, tripwire and repair-obligation
   source. Enumerate every live process capable of reaching the five canonical
   writer entrypoints. For each, retain PID, `/proc` start ticks, executable,
   command-line digest, deployment identity/time, exact loaded-source pins,
   writer subset, and participating status. The union must be exactly the five
   required writers; any missing or nonparticipating process refuses.
4. While holding the deployment lease, the operator verifier enumerates
   `/proc/[0-9]*/cmdline`, selects every process whose classpath or command can
   reach the canonical writer entrypoints, records the exact ordered process
   vector, and computes SHA-256 over its EDN bytes. It records the boot id,
   digest, the pinned writer-census digest, and a validity interval no longer
   than five minutes. It writes exactly one EDN form to the fixed root-owned,
   non-symlink `/etc/futon2/wm-interoceptive-participation.edn`, whose parent is
   also root-owned and not group/world writable.
5. `production-manifest!` acquires the deployment lease, independently rereads
   source bytes, the fixed receipt,
   `/proc` process identities, and the stable lock identity. Only then does it
   lock, capture, and invoke the unchanged logical constructor. A physical
   capture therefore cannot turn incomplete or contradictory trip/finding/
   discharge history into confidence 1.

At activation review, the operator supplies the retained `/proc` census bytes,
their digest, both provisioned inode identities/permissions, the deployment
generation record, and the root-owned receipt bytes. Those are fixed inputs to
the implemented reader; no new signature or evidence format must be invented.

This packet does not provision either path or reload any process. The current
read-only status is expected to refuse `:activation-lease-unavailable`.
Receipt contents supplied by a candidate or a test validator only yield
`:authority-class :test` and are not accepted by the production entrypoint.
