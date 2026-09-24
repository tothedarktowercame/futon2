# H-C-D — an outcomes extractor, written blind, tested against the owner's hand reference

kimi-5, 2026-09-24. Blind protocol kept: §1 (hand table) and §2 (script +
its output) were written WITHOUT reading
`futon3c/holes/labs/M-futon-seams/item6/mission-C.edn`; that file was
opened only at §3. Mission read: `futon3c/holes/missions/M-futon-seams.md`
at futon3c `2114cb99` (1002 lines). Instance want tokens for instances
5–7 were taken from the mission's own cascade files
(`futon3c/holes/labs/M-futon-seams/proto/instance-{4,5,6,7}.edn`) — the
mission text itself names want tokens only for instance 4 (INSTANTIATE
table, lines 849–851). No edits to any mission, no clicks, no writes
under data/.

## 1. Hand extraction (written before seeing the reference)

### Outcomes

| id | whose | span (lines) | verbatim quote |
|---|---|---|---|
| O1 capability-demonstrated | the mission's (its own stated subject) | 289–291 | "What this mission develops is a **capability**, not a list of instances: find an undeclared seam, choose how to declare it, do it, and check what was done." |
| O2 chosen-interface-declared-and-enacted | the mission's exit as scoped by IDENTIFY | 167 | "the exit is: the interface is declared, **at least one existing caller is converted to it**, and there is a test of the form \"redirect the binding and confirm behaviour follows\" (instance 1's method)." |
| O3 no-permanent-dual-source | the mission's design constraint | 169 | "**Do not** mint the abstraction and leave the hardcoded path alive indefinitely." (restated for instance 6 at line 128: "The intended end state is the abstract path absorbing the hardcoded one and the flag disappearing.") |
| O4 role-registry-interface | instance 4's end state (Rob's immediate unblock) | 161 | "**Role registry** (instance 4) — smallest surface, 51 literals plus three sites that route on the provider … and it unblocks provider substitution for Rob immediately." |
| O5 room-transport-interface | instance 5's end state | 162 | "**Room/Message/Command** (instance 5) — larger, but it retires `matrix-ircd` and the \"don't start an IRC server\" flag together." |
| O6 prompt-single-authority | instance 6's end state | 128 | "The intended end state is the abstract path absorbing the hardcoded one and the flag disappearing." |
| O7 editor-turn-seam | instance 7's end state, and Rob's closing ask | 16 | "Rob's closing ask for the future: a seam in the Emacs layer, so a VS Code / TypeScript implementation could reuse the core functionality rather than copy-pasting interactions into webhooks" |
| O8 store-kind-interface | instance 8's end state (Rob's problem in reverse) | 144–146 | "### 8. Store kind — both sides guilty … there is no interface to a generic graph/semantic database" |

### Served-by links (instance wants → outcomes)

| instance | want token | serves | source of the token |
|---|---|---|---|
| 4 | `caller-converted` | O2, O4 | mission text, line 849 |
| 4 | `redirect-test` | O2, O4 | mission text, line 850 |
| 4 | `prefix-routing-retired` | O2, O4 (and O3) | mission text, line 851 |
| 5 | `protocol-declared` | O5 | `proto/instance-5.edn` `:want` |
| 5 | `adapter-conformance-test` | O5 | `proto/instance-5.edn` `:want` |
| 5 | `impersonation-retired` | O5 (and O3) | `proto/instance-5.edn` `:want` |
| 6 | `one-authority` | O6 | `proto/instance-6.edn` `:want` |
| 6 | `flag-retired` | O6 (and O3) | `proto/instance-6.edn` `:want` |
| 7 | `record-schema-declared` | O7 | `proto/instance-7.edn` `:want` |
| 7 | `divergence-test` | O7 | `proto/instance-7.edn` `:want` |
| 7 | `writers-converted` | O7 | `proto/instance-7.edn` `:want` |
| 8 | — none: no cascade exists for instance 8 | O8 unserved | typed absence (no `proto/instance-8.edn`) |

### Preference / weighting

Stated, twice. (a) The cost ordering, lines 34–38: "1. **Declare the
interface first** — free. 2. **Retrofit a seam in code** — expensive but
mechanical … 3. **Retrofit a seam in prompt text** — worst." (b) The
IDENTIFY exit's candidates "in cost order", lines 161–164 (role registry,
then Room/Message/Command, then turn→record). No numeric weights anywhere;
the ordering is ordinal.

## 2. The extractor

`scripts/wm/extract-outcomes.clj` (clojure -M, read-only): mission path
in, EDN out. It extracts, from the mission TEXT alone: phase exit
criteria (each `**Exit criterion:**` sentence, cued), the IDENTIFY exit's
scope sentence and candidate ordering, instance sections and any want
tokens stated in the text (only instance 4's INSTANTIATE table rows), the
preference section, and a typed absence per instance whose wants the text
does not state. Every cue quote is verified to resolve at its cue lines;
the script refuses its own output if one does not.

### Script output (verbatim, `clojure -M scripts/wm/extract-outcomes.clj /home/joe/code/futon3c/holes/missions/M-futon-seams.md`)

```clojure
{:schema :wm/mission-outcomes-extract-v1,
 :mission "/home/joe/code/futon3c/holes/missions/M-futon-seams.md",
 :outcomes
 [{:id :exit/identify,
   :whose "the mission's exit as scoped by IDENTIFY",
   :cue {:lines [159 170],
         :quote "Pick one instance and declare its interface — not all eight."}}
  {:id :exit/map,
   :whose "phase MAP",
   :cue {:lines [197 199],
         :quote "**Exit criterion:** every MAP question has a concrete answer; the ready-vs-missing"}}
  {:id :exit/derive,
   :whose "phase DERIVE",
   :cue {:lines [284 286],
         :quote "**Exit criterion:** someone could implement the mission from the DERIVE section"}}
  {:id :exit/argue,
   :whose "phase ARGUE",
   :cue {:lines [498 499],
         :quote "**Exit criterion:** the design feels *inevitable* given the constraints, not"}}
  {:id :exit/verify,
   :whose "phase VERIFY",
   :cue {:lines [748 749],
         :quote "**Exit criterion:** the design has been checked against available structural"}}
  {:id :exit/instantiate,
   :whose "phase INSTANTIATE",
   :cue {:lines [835 836],
         :quote "**Exit criterion:** every completion criterion has a concrete demonstration, and"}}
  {:id :exit/document,
   :whose "phase DOCUMENT",
   :cue {:lines [889 890],
         :quote "**Exit criterion:** someone browsing the docbook can discover what this mission"}}
  {:id :capability,
   :whose "the mission's (its own stated subject)",
   :cue {:lines [289 291],
         :quote "What this mission develops is a **capability**, not a list of instances: find"}}],
 :instances
 [{:n 4, :title "Provider vs role — an identifier carrying a decision",
   :wants [{:token :caller-converted :cue {:lines [849 849]}}
           {:token :redirect-test :cue {:lines [850 850]}}
           {:token :prefix-routing-retired :cue {:lines [851 851]}}]}
  {:n 5, :title "Transport and Room — the seam that was never declared",
   :wants {:status :absent :reason :no-want-tokens-stated-in-mission-text}}
  {:n 6, :title "Prompts as an interface — the most painful retrofit",
   :wants {:status :absent :reason :no-want-tokens-stated-in-mission-text}}
  {:n 7, :title "Editor coupling — the one still compounding",
   :wants {:status :absent :reason :no-want-tokens-stated-in-mission-text}}
  {:n 8, :title "Store kind — both sides guilty",
   :wants {:status :absent :reason :no-want-tokens-stated-in-mission-text}}],
 :preference {:status :stated,
              :kind :ordinal,
              :cue {:lines [34 38],
                    :quote "## Cost ordering (use this to prioritise)"}},
 :absences
 [{:what :numeric-weights :status :absent :reason :ordering-is-ordinal-not-numeric}
  {:what :instance-5-8-want-tokens :status :absent
   :reason :stated-in-proto-cascades-not-in-mission-text}]}
```

(The script's real stdout is EDN on one line; the above is pretty-printed
for reading. `:serves` links are the hand table's judgement, not the
script's output — the mission text never states a served-by relation, so
the script does not invent one.)

## 3. Comparison with the owner's hand reference (mission-C.edn read only now)

(to be filled at step 3)

## 4. Defect classification

(to be filled at step 4)
