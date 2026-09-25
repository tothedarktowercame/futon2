# H-INTERP-D — the machine's interpretation step today vs claude-1's hand cascades

Author: kimi-3, 2026-09-25 (E-kimi-task-15, bell `invoke-1790304022734-24107-3fbcd834`
from claude-8). Discovery only: no code, data, source, or proposal file changed; no
clicks, no seat called, no shared-JVM loads. The request builder was run in a
private JVM (`clojure -M` in the futon2 checkout) with its evidence root and store
under `/tmp`, target resolution injected (`:resolve-fn`), no answerer. Anchors:
futon2 `d4e5599934bbe9f3569548a601a766cbefc077b6`, futon3c
`7466251cf4f819eb1fa95c62add79113bba4d0fc`. Sources read:
`src/futon2/aif/want_interpretation.clj`, `interpretation_request.clj`,
`interpretation_job.clj`, `interpretation_evidence.clj`,
`src/futon2/aif/flight_runner.clj` (D11 part 4, lines 73–244);
claude-1's hand cascade `futon3c/holes/labs/M-futon-seams/proto/instance-4.edn`,
its margin notes in `futon3c/holes/labs/M-futon-seams/annotations.edn`, and the
mission text `futon3c/holes/missions/M-futon-seams.md`.

H-interp (PROOF-2a-THEOREM "Holes" table, line 458): "turn mission text into
interpreted patterns (guard, produces, receipt)" — evidence to close or
shrink it: what the machine's seam produces today, key for key, against one
hand-written unit, and the gap list.

## 1. The exact shape of a seat's want-interpretation answer today

The flight's ask step (`flight_runner.clj:203–244`, `ask-fn`) issues one
request per unproduced want and the seat replies in the grammar
`wi/prompt` states verbatim (`want_interpretation.clj`, part 4):

> REPLY GRAMMAR: your reply must contain exactly one fenced ```edn block holding
> `{:schema :wm/want-interpretation-response-v1 :pattern … :guard {:needs #{…} :forbids #{…}} :produces #{…} :receipt {:source {:path … :sha256 …} :reading … :scope-limit … :by …}}` or
> `{:schema :wm/want-interpretation-response-v1 :decline {:reason …}}`; anything else is unparseable.

So a seat's answer has exactly these keys:

```clojure
{:schema  :wm/want-interpretation-response-v1   ; stripped by parse-reply
 :pattern "family/name"                          ; a file under futon3/library
 :guard   {:needs #{token} :forbids #{token}}
 :produces #{token …}                            ; must contain the asked want
 :receipt {:source {:path "futon3/library/<family>/<name>.flexiarg"
                    :sha256 "of the bytes read"}
           :reading "how the pattern applies to this criterion"
           :scope-limit "what of the pattern does not transfer"
           :by "answering agent id"}}
```

Presence check against H-interp's own enumeration ("guard, produces, receipt
with author/source/sha"): **guard — present** (`:needs`/`:forbids`, tokens must
already be known to the target, `guard-reasons`); **produces — present**;
**receipt source path+sha256 — present** (`receipt-reasons` re-hashes the
library file's bytes); **receipt author — present as `:by`**. What is *not*
anywhere in the answer shape: the pattern's **`:forces`** (the pressure that
makes the move apply), any **citation** into the mission or the pattern file,
a **self-hash** of the interpretation, or a **`:grain`** declaration.
Validation (`validate-response`, `want_interpretation.clj:130–215`) checks id
canonicity, receipt file/sha/keys, `:produces` ∋ want, guard-token knownness,
owner constraints, and that the constructor builds a candidate reaching the
want through it; it never checks that the pattern's own text supports the
claimed production.

Note there is a *second*, stricter machine grammar — the interpretation-job
receipt `:wm/interpreted-pattern-set-v1` (`interpretation_evidence.clj`,
`interpretations!`): per interpretation `:pattern :source :membership
:clauses{:if :however :then} :guard :effect :authority :author :sha256`, with
quoted clause citations and a value-digest over the whole interpretation. The
want-interpretation reply grammar requires none of that. Two grammars named
"interpretation", two rigor levels, one hole.

## 2. The exact shape of one hand-written unit

claude-1's `gauntlet/placenta-transfer` unit in `instance-4.edn`
(`:m-futon-seams/proto-cascade-v1`), verbatim keys:

```clojure
:gauntlet/placenta-transfer
{:guard {:needs #{:sites-enumerated :one-producer} :forbids #{}}
 :produces #{:caller-converted}
 :receipt {:source {:path "futon3/library/gauntlet/placenta-transfer.flexiarg"
                    :sha256 "9771eca50e93c42de6b1ea22e188c770635d62830ca190f5ae4ca18056a069cd"}
           :reading "Its conclusion IS the conversion move: identify which functions the human is performing as a surrogate for missing infrastructure and transfer them one at a time to the system. …"
           :scope "The pattern's wider list of AIF functions the operator carries does not transfer; only the identify-and-move-one discipline is used."
           :author "claude-1"}
 :forces "The human is currently performing multiple AIF functions simultaneously, as a surrogate for infrastructure that does not exist yet."}
```

Same keys as the machine's answer, with these differences: receipt uses
**`:scope`** and **`:author`** (machine: `:scope-limit`, `:by`); the unit
carries **`:forces`**, which the machine grammar cannot express; and one
sibling unit (`cascade-construction/choose-the-grain-where-state-lives`)
carries a **`:grain {:keyed-by :role :statement …}`** key no other grammar
knows. The cascade file also carries two things the machine never asks for:
per-token `:tokens` statements with mission-span `:cue`s, and an `:above`
edge list with typed reasons (`:kind :differentiates` / `:jointly-with`,
`:via "…"`) for every guard edge.

## 3. One want of instance 4, end to end today, machine vs hand

Setup: instance 4's want set is `#{:caller-converted :redirect-test
:prefix-routing-retired}`. Remove only `:gauntlet/placenta-transfer` from the
admitted patterns and `:caller-converted` is the unproduced want
(`wi/unproduced-wants` confirms: `[:caller-converted]`). Ran `wi/request!` —
the same call `flight_runner.clj:117–128` makes — with criterion
`{:kind :line :line 167 :phase "IDENTIFY" :stated "at least one existing
caller is converted to it"}` (mission L167; `citation-for` locates it by its
text inside the bold span), facts `{}`, patterns = the six remaining hand
units. No seat was called; the request and the exact prompt are the output.

**What the machine produces end to end today** (abridged; full map and prompt
captured in the run):

```clojure
{:schema :wm/want-interpretation-request-v1
 :target "M-futon-seams"
 :want {:token :caller-converted :observed nil
        :criterion {:kind :line :line 167 :phase "IDENTIFY"
                    :stated "at least one existing caller is converted to it"}}
 :context {:facts {}
           :interpretations {;; six units, guard+produces only, e.g.
             :cycle-machine/single-producer
             {:guard {:needs #{:binding-recorded} :forbids #{}} :produces #{:one-producer}} ,,,}}
 :asks {:what "one interpretation of a futon3/library pattern whose :produces contains the want token"
        :answer-shape {,,,} :or "a typed decline …"}
 :retrieval {,,,}}  ; pinned mission source, 2 retriever runs, 48 unjudged candidates
```

plus the seat-facing prompt (`wi/prompt`) listing the 48 candidates and the
reply grammar of §1. The machine stops there by design: "Nothing here
interprets: retrieval candidates are unjudged, and the response is the
answerer's."

**Retrieval did not surface the pattern claude-1 chose.** The 48 unjudged
candidates (embedding k=40: `process-coherence/refusal-becomes-pattern`,
`orchestration/pattern-warranted-choice-point`, `agent/intent-handshake-is-binding`, …;
tier0 k=8: `test-registry/spot-check-at-the-declared-rate`,
`writing-coherence/recursion-cheque`, …, `translation/test-by-reproducing-behaviour`
at rank 6, …) contain **no `gauntlet/*` entry at all**. The interpretation the
reviewed hand cascade actually uses is outside what the machine's step would
put in front of a seat today for this want.

**What claude-1 wrote for the same want** is the §2 unit: guard
`#{:sites-enumerated :one-producer}` (jointly-with edges, reasons recorded),
produces `#{:caller-converted}`, receipt naming the library file with sha256,
reading, scope, author, and forces — i.e. the same skeleton plus `:forces`,
plus cue-anchored token statements and typed edge reasons around it.

## 4. Gap list

Ordered; the first gets its smallest change and bad case.

1. **Missing key (mechanical): receipt spelling — `:scope`/`:author` (hand
   corpus) vs `:scope-limit`/`:by` (machine grammar).** The one reviewed
   corpus of interpretations that exists (instance-4/4b/5/6/7) cannot be
   replayed through the seam as written: a seat pasting claude-1's verbatim
   `gauntlet/placenta-transfer` unit gets
   `{:status :rejected :reasons [{:reason :receipt-incomplete :missing [:scope-limit :by]}]}`
   from `receipt-reasons` (`receipt-keys [:reading :scope-limit :by]`,
   blank-checked). **Smallest change:** in `parse-reply` (or at
   `validate-response` intake) normalise `:scope → :scope-limit`,
   `:author → :by` when the machine spelling is absent, rejecting only when
   both are absent or both present and unequal — data change in one function,
   no grammar widening. **Bad case it closes:** a seat that "fixes" the
   rejection by retyping the keys from memory instead of copying values
   silently paraphrases the reading and scope of a reviewed unit, and the
   published record then carries an unreviewed paraphrase under a
   reviewed-unit sha256; with normalisation the verbatim bytes validate as
   they stand, and a genuinely incomplete receipt still rejects.

2. **Missing key: `:forces`.** Every hand unit records the pattern's own
   pressure ("The human is currently performing multiple AIF functions
   simultaneously…"); the reply grammar has no slot for it, so the record
   keeps the *what* and loses the *why this pattern*. The discriminator it
   buys: retrieval candidate `translation/test-by-reproducing-behaviour`
   (tier0 rank 6 for this query) could be answered with a plausible reading
   and invented `:produces #{:caller-converted}` and would pass every check
   in `validate-response` (id canonical, receipt hashable, guard tokens
   known, constructor reaches the want); a required `:forces` quote from the
   pattern's own text is the cheapest evidence the seat actually read the
   pattern. Needs a grammar amendment (add `:forces`, quoted from the pinned
   pattern source).

3. **Needs the library: retrieval misses the reviewed choice.** For the one
   want run, the pattern the hand cascade uses is absent from all 48
   unjudged candidates (§3). The strict job path already anticipates this —
   its prompt tells the interpreter "you may search the captured library and
   append conformant runs named agent-search" (`interpretation_job.clj`
   `prompt`); the want-interpretation prompt says nothing about searching.
   Smallest direction: port the agent-search sentence (and its "judge those
   candidates too" discipline) into `wi/prompt`; the bad case without it is
   gap 2's wrong-but-validating answer chosen from a weak candidate list.

4. **Wrong grain: the machine's wants are lifecycle-exit tokens, the hand
   cascade's are instance-outcome tokens.** For a real flight of
   M-futon-seams the ask would be about `mission-criteria` tokens
   (`:exit/h66b2ffcf3e0b` etc., per SEAMS-INTERP §1) keyed to IDENTIFY/MAP/
   DERIVE exit *lines*, not claude-1's `:caller-converted`-class tokens that
   carry per-token `:statement`s and mission-span `:cue`s. The hand grain is
   an outcome with a stated definition; the machine grain is a phase-exit
   sentence. This run used the hand token to make the key-for-key comparison
   possible; aligning the flight's want vocabulary with outcome statements
   is the H-C outcome-definition work (kimi-5's task-17 lane), not this
   packet's to propose.

5. **Wrong grain / missing context: the machine sends guard+produces only,
   and asks for no edge reasons.** The request's `:context :interpretations`
   strips each unit to `:guard`/`:produces`; the hand cascade's `:above`
   list — per-edge `:kind :differentiates`/`:jointly-with` with a `:via`
   sentence — is neither transmitted nor requested, so a seat cannot see,
   and the record cannot keep, *why* a guard edge exists. Nothing in
   `validate-response` could check an edge reason even if one arrived.

6. **Needs a seat (by design, recorded for completeness).** The machine's
   step terminates at an issued request with unjudged candidates; every
   cascade on the witness being claude-1's is exactly the hole. The stricter
   `:wm/interpreted-pattern-set-v1` grammar (clause citations, membership
   citations, self-hash, §1) shows the seat-answered shape the contract
   already trusts elsewhere; converging the two grammars is the natural
   closing move for the hole but is an amendment, not a discovery.

## 5. Reproduction

Private JVM, futon2 checkout at the anchor, no seat, no shared state:

```sh
cd /home/joe/code/futon2
cat > /tmp/hinterp_run.clj <<'EOF'
(require '[futon2.aif.want-interpretation :as wi] '[clojure.pprint :as pp] '[clojure.java.io :as io])
(def i4 (read-string (slurp "/home/joe/code/futon3c/holes/labs/M-futon-seams/proto/instance-4.edn")))
(def pats-minus (dissoc (:patterns i4) :gauntlet/placenta-transfer))
(println (wi/unproduced-wants (:want i4) {} pats-minus))   ; => [:caller-converted]
(def req (wi/request! {:target "M-futon-seams" :want :caller-converted
                       :criterion {:kind :line :line 167 :phase "IDENTIFY"
                                   :stated "at least one existing caller is converted to it"}
                       :facts {} :patterns pats-minus}
                      (io/file "/tmp/hinterp-root")
                      {:resolve-fn (fn [_] {:id "M-futon-seams"
                                            :path "/home/joe/code/futon3c/holes/missions/M-futon-seams.md"})}))
(pp/pprint (dissoc req :retrieval))
(println (wi/prompt (wi/issue! "/tmp/hinterp-store" req)))
EOF
clojure -M -e '(load-file "/tmp/hinterp_run.clj")'
```

The retrieval port runs the component's own pinned read-only subprocess
(`scripts/interpretation_retrieve.py` over the pinned embedding and tier0
indices); evidence pins land under `/tmp/hinterp-root`, the issued request
under `/tmp/hinterp-store`. Nothing under `data/`, no registry read
(`:resolve-fn` injected), no click, no flight.
