# P4 receipt correction — original diagnosis evidence for click 0930ccf1 (2026-10-31)

Additive evidence note correcting receipt 8f887ba20
(`P4-adf04df0-single-repair-receipt-2026-10-31.md`). Reviewer: zai-5, dispatched
by codex-11 (job invoke-1793416406993-8638-3839b94c). Bounded, read-only: no
rerun of diagnose/acceptance, no POST of new invokes, no contact, no source fix,
no ledger/tracker change, no click, no spend. Everything below comes from
retained original artifacts.

## 1. Source-code facts (verified in current main)

- `futon2 src/futon2/aif/meta_self_heal_production.clj:187-201` — `cause-entry-defect`
  validates `(:cause-entry (:diagnosis response))`, i.e. **the diagnosis RESPONSE
  grammar**, in six clauses. The refusal we hit is clause 6:
  `(not (cause-scope/predicate? (:healthy-predicate entry)))` →
  `:cause-scope/predicate-not-result-derived`. Clauses 1–5 (map shape, resolvable
  var, repo file, input-pin equals source-record-pin, vector input path) all
  **passed** on the actual response (§3).
- `futon2 src/futon2/aif/cause_scope.clj:7-13` — `predicate?` accepts exactly a map
  `{:op ∈ #{:result-path-equals :result-path-present :result-path-absent},
  :path <vector>, :value <required for equals>}`. Nothing in this grammar reads the
  historical injury's cause record. The receipt's blocker text ("the injury's cause
  record lacks a result-derived cause-scope predicate … source change to the
  diagnosis/cause-scope contract") is therefore **unsupported**: the 8f887ba20 report
  conflated diagnosis-response grammar with a field required in the injury source
  record. The cause record only enters via the input-pin equality clause, which passed.
- `futon3c src/futon3c/wm/meta_self_heal_adapters.clj:402` — the dispatched diagnosis
  prompt instructs `:healthy-predicate <closed result predicate>` **without stating
  the accepted op-map grammar**. The retained request (click state, §2) also carries
  no grammar (abbreviated field index, record pin, evidence-access, injury-record,
  record-index only). So the seat was asked for a "closed result predicate" with the
  machine grammar never communicated.

## 2. Original artifacts, no reconstruction

- **A diagnosis seat WAS contacted.** `/tmp/invoke-trace.log` line 11344:
  `at=2026-10-31T03:08:51.794Z agent=wm-author thread=wm-runner-click` with the
  "Dedicated META diagnosis" prompt (producer-invoke!, `meta_self_heal_adapters.clj:1103`
  routes `:meta-diagnosis-fn` to the wm-author cast seat). The receipt's C2 line
  "no contact occurred at all" is wrong; correct statement: no *repair author
  dispatch* occurred — one diagnosis producer invoke on the wm-author cast seat did.
- **Response retained twice, original:**
  1. Evidence store (futon1b :7073), `GET /api/alpha/evidence?author=wm-author&since=2026-10-31T03:08:00Z…`:
     turn-round `zai-turn-b849b8c6-76ed-4cb1-bad7-980a366c0d2a`, session
     `zai-9b8a9121-8e51-44ab-a0a1-6298051dc096`, round 14, `:final true`, 1446 output
     tokens — this copy is capped (ends `…[+62 chars]`, the cap falls exactly at the
     predicate).
  2. Full text from the wm-author seat's in-memory invoke history (read-only
     reflection on the serving JVM; original artifact of the same invoke, not a
     rerun). Extract, verbatim in the load-bearing part:
     `:cause-entry {:var "futon2.aif.full-loop-runner/run-opportunity!", :file
     "src/futon2/aif/full_loop_runner.clj", :failure-kind :author-delivery-wrong-repository,
     :failure-stage :cascade-resume, :input {:record-pin {:path
     "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-31-f805ca4b-6c35-45cf-accc-f95dd54b0fc8.edn",
     :sha256 "ba359b5e9f5d6b5a56adaa6f5c23339738110350bc13eb55647f3cdf33b81bea"},
     :path [:q6 :terminal :outcome]}, :healthy-predicate "(fn [record] (nil? (:failure record)))"}`
     with `:status :succeeded` and `:classification :misclassified-author-delivery-descriptor`.
     (Full saved copy: local pin `~/notes/.../run001/adf04df0-single-repair/p4/diagnosis-response-full.txt`;
     verbatim excerpt above is the authoritative quote.)
- **Returned healthy-predicate (answering the dispatch question):** the string
  `"(fn [record] (nil? (:failure record)))"` — a Clojure fn literal in a string,
  not the op-map `cause-scope/predicate?` accepts.

## 3. Classification

**Missing prompt grammar → predictably non-conforming output.** Not the seat's
malformed output in a vacuum (it answered the prompt as written: a genuinely closed
predicate over the record); not an implementation defect in `cause-entry-defect` /
`predicate?` (the gate did its job and refused honestly, first divergence exactly at
the grammar clause); not a deficiency of the historical injury's cause record (every
other cause-entry clause, including input-pin equality against the source record,
passed); not unobservable (the response is retained, §2). The dispatched prompt at
`meta_self_heal_adapters.clj:402` asks for `<closed result predicate>` while the
validator accepts only the op-map grammar that the prompt and request never state.
Observation for the owner, not applied: the seat's semantic intent maps naturally to
`{:op :result-path-absent :path [:failure]}` — converting it would be a new decision
and is NOT evidence of what the seat would have returned had the grammar been stated.

## 4. Receipt 8f887ba20 corrections (additive; original stands, charge/refusal preserved)

1. **Blocker section wrong in mechanism:** "injury's cause record lacks a
   result-derived cause-scope predicate ⇒ source change to diagnosis/cause-scope
   contract" — unsupported. The producer DID return a diagnosis; the refusal is the
   grammar clause on the response; the injury record satisfies its only role
   (input-pin equality, passed).
2. **"No author contact … the whole repair pipeline never reached" / C2 "no contact
   occurred at all":** one diagnosis producer invoke on the wm-author cast seat
   occurred (§2). No repair author dispatch, review, enact, retry child — those
   remain true.
3. **Criterion overclaims:**
   - **C2:** the four protected store hashes are store-conservation snapshots, not a
     *task* pre-effect baseline — no task existed on this refused repair route; and
     the "no external contact" form of the exactly-once discussion is moot (one
     contact occurred). Claim should be narrowed to: store conservation pinned, one
     diagnosis contact, no author effect.
   - **A1:** charge (243/230/13) and recorded refusal are real, but `binding-status
     :absent` / no run record means the acknowledgement chain terminates at the
     click result; A1 acknowledgement is **partial**, not fully observed.
   - **P3 "Natural P3 witness":** overclaim. The refusal projected absent terms
     `[:diagnosis-evidence :acceptance-test]` — that witnesses honest
     absence-projection generally, not the E4-WORK-ORDER-NIL-RETURN-LIMIT clause,
     which requires the *optional acceptance-return-limit* key absent in emitted
     options of a real attempt (my P3 audit, `review/criteria-p3-stale-deficits`:
     LIVE-WITNESS-OWED, unchanged by this click).
4. **Preserved as correct:** one parent charge 14→13, typed refusal before author
   dispatch, attempt unspent (`spent []`), ledger `ca73914f…1a44` unchanged,
   runtime-disposition chain, honest held state.

## 5. Smallest evidence-grounded next action

Not another repair chain and not authorized here: a one-line prompt-grammar
correction at `futon3c wm/meta_self_heal_adapters.clj:402` — inline the exact
accepted grammar (`{:op :result-path-equals|:result-path-present|:result-path-absent,
:path <vector>, :value <only for equals>}`) in the diagnosis prompt (optionally echo
it in the request) — reviewed and merged like its predecessors, after which a
normally-dispatched P4 re-release (attempt unspent, budget 13) can obtain a
conforming predicate from the same evidence. No gate weakening, no response
coercion, no fabricated predicate, no ledger correction. This packet authorizes no
spend.
