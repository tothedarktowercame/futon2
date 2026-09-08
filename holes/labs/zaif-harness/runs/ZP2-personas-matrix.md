# ZP2 — personas × harness operations

Rendered from `runs/ZP2-personas-matrix.edn`. This is a broad-brush program
map, not a claim that a `:needed` edge exists. `A` = available to the persona,
`N` = needed but no participant-facing edge was identified, `—` = the persona
does not characteristically use the operation.

## Finite chip vocabulary

The nine chips are `observe`, `bell`, `whistle`, `park`, `review`, `publish`,
`attest`, `refuse`, and `escalate`. Their meanings and source pointers are in
the EDN `:operations` vector. The vocabulary is finite by construction: it is
the nine-item operator vocabulary commissioned by the plan, resolved against
the icon note and live call sites; it is not an enumeration of every function
in Agency. In ChipWits terms, typed input is FEEL/LOOK/SNIFF, refusal is the
BOUNCER, review is the Debug panel, budget is FUEL, and preferred outcomes are
PIE (`holes/labs/wm-contract/NOTE-chipwits-iconography.md:16-32`).

## Matrix

| Persona (ZP1 line) | characteristic program | observe | bell | whistle | park | review | publish | attest | refuse | escalate |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| operator / goal author (:40) | observe→bell/whistle→review→attest/refuse→publish | A | A | A | — | A | A | A | A | A |
| machine agent (:41) | observe→whistle→park→refuse→bell→publish | A | A | A | A | A | A | — | A | A |
| human collaborator / reader / reviewer (:42) | publish→observe→review→whistle→attest | N | — | N | — | A | N | N | N | N |
| argument author / downstream user (:43) | observe→review→refuse→publish→attest | N | — | N | — | N | N | N | N | N |
| trainer / local adapter (:44) | observe→refuse→review→publish→whistle | N | — | N | — | N | N | N | N | N |
| training recipient / participant (:45) | observe→refuse→publish | N | — | N | — | — | N | — | N | N |
| institutional lead / governor (:46) | observe→review→attest/refuse→escalate→publish | N | N | N | — | N | N | N | N | N |
| funder / commissioner / evaluator (:47) | observe→review→attest/refuse→publish | N | — | N | — | N | N | N | N | N |
| external maintainer / client / employer / institution (:48) | publish→observe→whistle→refuse→attest | N | — | N | — | N | N | N | N | N |

The ZP1 citations resolve to `runs/ZP1-stakeholder-census.md:40-48`. Stack,
FUEL, PIE, cell meanings, and the full census pointer are retained per persona
in the EDN rather than compressed into this table.

## Sparse rows are named Cluster-C holes

A row is sparse when fewer than four operations are `:available`. Seven rows
are sparse; the two non-sparse rows are the operator and machine agent, the
participants the current harness directly addresses.

| persona | named hole | what the harness lacks |
|---|---|---|
| human collaborator / reader / reviewer | `cluster-c/human-discovery-delivery` | Recipient-addressed delivery and acknowledgement for a machine discovery. |
| argument author / downstream user | `cluster-c/understanding-to-argument` | A binding from understanding change to canonical argument revision and downstream acknowledgement. |
| trainer / local adapter | `cluster-c/trainer-feedback-return` | A path to record an adaptation, test its effect, and return it upstream. |
| training recipient / participant | `cluster-c/participant-outcome-ingress` | Safe outcome/refusal ingress plus acknowledgement of the resulting change. |
| institutional lead / governor | `cluster-c/governance-disposition` | An authority-bound accepted/refused/funded disposition on programme evidence. |
| funder / commissioner / evaluator | `cluster-c/external-evaluation-return` | A commission-linked external verdict whose evidence and funding consequence return to the loop. |
| external maintainer / client / employer / institution | `cluster-c/outward-response-ingress` | Durable attribution of an external response to its artifact and the next decision. |

The EDN's `:proposed-deep-rows` proposes one later row per named hole. They are
proposals only: this row does not mint worklist entries.

## Enumeration receipt

The rendered table was checked against the machine-readable carrier with:
`bb -e '(let [x (clojure.edn/read-string (slurp "runs/ZP2-personas-matrix.edn"))] (println {:ops (count (:operations x)) :personas (count (:personas x)) :sparse (count (filter :hole (:personas x))) :proposals (count (:proposed-deep-rows x))}))'`.
It printed `{:ops 9, :personas 9, :sparse 7, :proposals 7}`. The command read
the entire EDN and its output was not truncated.
