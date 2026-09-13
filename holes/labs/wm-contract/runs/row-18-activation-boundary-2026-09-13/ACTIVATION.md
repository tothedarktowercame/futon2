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
2. Deploy the exact pinned lock, manifest, tripwire and repair-obligation
   source. Enumerate every live process capable of reaching the five canonical
   writer entrypoints. For each, retain PID, `/proc` start ticks, executable,
   command-line digest, deployment identity/time, exact loaded-source pins,
   writer subset, and participating status. The union must be exactly the five
   required writers; any missing or nonparticipating process refuses.
3. An operator-owned host verifier records the boot id, complete process-census
   digest, the pinned writer-census digest, and a validity interval no longer
   than five minutes. It writes exactly one EDN form to the fixed root-owned,
   non-symlink `/etc/futon2/wm-interoceptive-participation.edn`, whose parent is
   also root-owned and not group/world writable.
4. `production-manifest!` independently rereads source bytes, the fixed receipt,
   `/proc` process identities, and the stable lock identity. Only then does it
   lock, capture, and invoke the unchanged logical constructor. A physical
   capture therefore cannot turn incomplete or contradictory trip/finding/
   discharge history into confidence 1.

This packet does not provision either path or reload any process. The current
read-only status is expected to refuse `:activation-receipt-unavailable`.
Receipt contents supplied by a candidate or a test validator only yield
`:authority-class :test` and are not accepted by the production entrypoint.
