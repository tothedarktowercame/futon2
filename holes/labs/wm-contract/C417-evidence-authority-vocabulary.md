# C417 — evidence authority vocabulary

Date: 2026-09-01

Authority is independent of derivation. C405 records whether the pinned
subject was derived completely; this sibling records who can choose or rewrite
the authority against which that subject is checked.

## Schema

```clojure
{:authority/type :self-owned | :externally-administered-unsealed
 :subject <evidence or authority being assessed>
 :evidence-writer {:principal <who> :mechanism <how>}
 :canonical-head-selector {:principal <who-or-nil> :mechanism <how-or-none>}
 :storage-owner <principal>
 :retention-administrator <principal>
 :rewrite-rollback-capability
 {:principal <principal-or-principals>
  :capabilities <observed capability set>
  :limit <any observed restriction>}
 :verifier {:principal <who-or-nil> :mechanism <consumer-or-none>}
 :independent-canonical-head? <boolean>
 :reason <site-specific authority limit>}
```

The six roles are mandatory and remain separate: evidence writer, canonical
head selector, storage owner, retention administrator, rewrite/rollback
capability, and verifier. Sharing a principal across roles is a result, not a
schema shortcut.

Only two authority types are admitted because only two were observed:

- `:self-owned`: the evidence writer can also choose or rewrite the authority
  presented as canonical;
- `:externally-administered-unsealed`: administration is outside the ordinary
  evidence writer, but retention or replacement remains possible and no
  independently sealed canonical head exists.

There is deliberately no sealed or independent tier. Nothing assessed in C409
or C415 attests one.

## Composition with derivation

The records compose without either upgrading the other:

```clojure
{:evidence/derivation
 {:status :machine-derived
  :subject :quiet-run-ledger-internal-chain
  :derived-by :row-and-predecessor-hashes-recomputed}
 :evidence/authority
 {:authority/type :self-owned
  ;; six-role authority record follows
  }}
```

This says the presented chain is internally derived and valid while its
canonical head remains producer-selectable. Conversely, a C405
`:declared-not-derived` boundary can sit beside an externally administered
authority record: external administration does not make an incomplete
derivation complete.

## Quiet-run application

The current quiet-run ledger is:

```clojure
{:authority/type :self-owned
 :subject :quiet-run-ledger-canonical-head
 :evidence-writer {:principal :joe :mechanism :quiet-run-state-machine}
 :canonical-head-selector {:principal :joe :mechanism :caller-selected-ledger-path}
 :storage-owner :joe
 :retention-administrator :joe
 :rewrite-rollback-capability
 {:principal :joe
  :capabilities #{:replace :truncate :delete :rename :select-alternate-valid-chain}
  :limit :content-hashes-detect-row-mutation-only-for-the-presented-chain}
 :verifier {:principal :joe :mechanism :quiet-run-ledger-loader}
 :independent-canonical-head? false
 :reason :same-principal-writes-selects-retains-and-verifies}
```

This is permanent for the current local authority arrangement, not permanent in
principle. A different external anchoring design could change it; local chain
hardening cannot.

The available journal facility is separately:

```clojure
{:authority/type :externally-administered-unsealed
 :subject :systemd-journal-as-possible-quiet-run-anchor
 :evidence-writer {:principal :joe :mechanism :journal-message-submission}
 :canonical-head-selector {:principal nil :mechanism :none-for-quiet-run}
 :storage-owner :root/systemd-journal
 :retention-administrator :root
 :rewrite-rollback-capability
 {:principal :root
  :capabilities #{:rotate :vacuum :delete :replace-storage}
  :limit :joe-cannot-edit-retained-journal-files}
 :verifier {:principal nil :mechanism :none-for-quiet-run}
 :independent-canonical-head? false
 :reason :retention-bounded-unsealed-and-not-consumed-as-canonical-head}
```

Journald can reveal retained divergent histories. It cannot distinguish a
byte-identical copy from its source, prove the logged event, or select the
canonical quiet-run head. “Externally administered” therefore must never be
rendered as “independently anchored.”
