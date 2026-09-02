# U14d independence-receipt consumer census

Date: 2026-09-02
Enumerator: codex-22

## Enumeration method

The following searches covered Clojure source, scripts, and executable lab code
in both repositories. Each command wrote its complete output to a file before
it was inspected; none of the four outputs was truncated.

```sh
rg -n --glob '*.clj' ':receipt/independent-review\?' /home/joe/code/futon2/src /home/joe/code/futon2/scripts /home/joe/code/futon2/holes > /tmp/u14d/boolean-futon2.txt
rg -n --glob '*.clj' ':receipt/independent-review\?' /home/joe/code/futon3c/src /home/joe/code/futon3c/scripts /home/joe/code/futon3c/holes > /tmp/u14d/boolean-futon3c.txt
rg -n --glob '*.clj' ':receipt/independence' /home/joe/code/futon2/src /home/joe/code/futon2/scripts /home/joe/code/futon2/holes > /tmp/u14d/typed-futon2.txt
rg -n --glob '*.clj' ':receipt/independence' /home/joe/code/futon3c/src /home/joe/code/futon3c/scripts /home/joe/code/futon3c/holes > /tmp/u14d/typed-futon3c.txt
```

The outputs contained 0, 7, 0, and 10 lines respectively. After separating
writers from readers, there are five field-reading sites in four functions.
There are no readers in futon2.

## Consumers

| Reader | Field | Current true / false / absent behavior | Dated flip |
|---|---|---|---|
| `futon3c.apm.frame-cycle-handlers/guide-snapshot-evidence-valid?` (`src/futon3c/apm/frame_cycle_handlers.clj:20-25`) | `:receipt/independent-review?` | `true` passes when the other snapshot evidence is shaped correctly; `false` or absent yields `:frame-cycle-guide-snapshot-evidence-invalid` (`frame_cycle_handlers.clj:191-194`). | Yes. The Boolean requirement will be replaced by a grade requirement; post-c `:asserted-unverified` will no longer masquerade as demonstrated independence. |
| `futon3c.apm.frame-cycle-handlers/guide-independence-vocabulary-valid?` (`src/futon3c/apm/frame_cycle_handlers.clj:27-30`) | `:receipt/independence` | A known value passes; absent passes for legacy compatibility; unknown yields `:frame-cycle-guide-independence-vocabulary-invalid` with actual and known values (`frame_cycle_handlers.clj:196-200`). | No structural change: vocabulary validation remains necessary when the Boolean gate is retired. |
| `futon3c.apm.countdown-control/publish-promotion!` (`src/futon3c/apm/countdown_control.clj:1245-1251`) | `:receipt/independent-review?` | `true` continues; `false` or absent refuses with `:zai-scribe-reviewer-is-depositor`. | Its decision changes from reading the Boolean to reading `checked-handoff/grade-receipt`; distinct persisted seats retain the success, and equal seats retain the refusal. |
| `futon3c.apm.countdown-control/publish-guide-promotion!` (`src/futon3c/apm/countdown_control.clj:1322-1328`) | `:receipt/independent-review?` | `true` continues; `false` or absent refuses with `:guide-promotion-reviewer-is-depositor`. | Same as `publish-promotion!`: the persisted-seat grade preserves the honest distinction and the same-seat refusal. |
| `futon3c.apm.checked-handoff/grade-receipt` (`src/futon3c/apm/checked_handoff.clj:85-122`) | both fields, plus persisted seats | Seats produce a computed grade and override typed assertions; a known typed field passes through; with neither, both Boolean values and absence produce `:ungradeable-legacy`; an unknown typed value is refused (`checked_handoff.clj:111-118`). | This is the destination of the dated flip, so it is not itself changed by that flip. |

## Conclusion

The flip has three Boolean gates to migrate. It does not mechanically break a
consumer, but it intentionally changes the guide gate's interpretation of
constant-true receipts: `:asserted-unverified` is recorded rather than treated
as independent. The two countdown gates have persisted seat evidence and can
retain their current accept/refuse behavior through the computed grade. Legacy
receipts cannot recover a grade from either Boolean value and remain
`:ungradeable-legacy`.
