# C591 — F11 `find` signature draft review

Date: 2026-09-08. Worklist row `:F11`, slice 12. This is a review of
`C590-F11-find-signature-draft.md`; it does not edit Lean, choose a receipt
carrier, or choose an F4 reading.

## Result

The draft is not ready to apply to `Holes.lean`. It identifies the missing
query operation correctly, but the proposed result and laws do not preserve
the authoritative F1–F4 interface.

## Findings

1. **The proposed F4 is a different property.** The draft proposes
   `selected p -> fires? p context` (`C590-F11-find-signature-draft.md:56-58`).
   The authoritative F4 requires, for a given tension, at least one repository
   pattern that the finder must not return
   (`holes/problems/P-validated-R5.md:484-488`). The executable recorded check
   tests the separately declared zero-mass member, not antecedent soundness
   (`futon3:checks/find_snatch.clj:172-177`). The proposed theorem can hold for
   a finder that returns every firing pattern and therefore does not settle
   any of the registered F4 readings.

2. **Typed absence is dropped.** The existing `FindResult` carries `absence`
   (`mathlib4/DarkTower/WarMachine/Holes.lean:247-250`), and F1 requires the
   named typed absence when selection is empty
   (`holes/problems/P-validated-R5.md:484-485`). The draft result has only
   `selected` and `receipts` (`C590-F11-find-signature-draft.md:38-40`).

3. **Receipt existence is not F2 content.** F2 requires the acknowledged
   IF/HOWEVER clause, retrieval route, and as-of
   (`holes/problems/P-validated-R5.md:486`). The proposed dependent receipt
   proves only that a selected pattern has some receipt
   (`C590-F11-find-signature-draft.md:50-52`); `extra : Extra` does not require
   clause or as-of data (`C590-F11-find-signature-draft.md:32-36`). Applying
   this shape would therefore choose neither open receipt-carrier arm nor
   provide the observations the warrant requires.

4. **The proposed warrant does not match the executable warrant.** The draft
   says `Warrant P` is derived from `Repository.standsOn`
   (`C590-F11-find-signature-draft.md:26-31,65-67`). The executable warrant is
   the selected pattern's own file and IF/HOWEVER text and spans
   (`futon3:checks/find_organise.clj:213-221`). Authored edges are one allowed
   F3 basis, not the only one (`holes/problems/P-validated-R5.md:487`).

5. **The Clojure extension claim is stronger than the code.** The draft says
   the optional receipt constructor cannot remove `:route` or `:warrant`
   (`C590-F11-find-signature-draft.md:9-12`). The constructor's map is the
   later input to `into` (`futon3:checks/find_organise.clj:245-251`), so it can
   overwrite those keys. The shipped snatch caller does not overwrite them
   (`futon3:checks/find_snatch.clj:56-65`), but the generic interface does not
   enforce the claimed restriction.

6. **The snippet is not an applicable declaration block as written.**
   `Receipt` and `FindResult` already exist in the target namespace
   (`mathlib4/DarkTower/WarMachine/Holes.lean:240-250`), while `Route` and
   `Warrant` are not found as declarations under `mathlib4/DarkTower`. The
   proposed `def find` also has no body (`C590-F11-find-signature-draft.md:42`).
   The eventual Joe-owned edit must amend or replace the existing declarations
   explicitly and must say whether it is an executable definition or an
   interface declaration.

## What the next draft must preserve

A revised draft must retain typed absence, state F4 without replacing it by
antecedent soundness, and expose enough receipt data to measure the open F2
choice while allowing text or authored-edge warrants. It must also state the
computability boundary for `fires?`. Those requirements constrain the next
draft; they do not decide `:find-f2-receipt-carrier` or `:find-f4-reading`.

