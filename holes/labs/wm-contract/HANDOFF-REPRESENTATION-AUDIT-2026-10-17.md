# War Machine handoff representation audit — 2026-10-17

Trigger: two consecutive live clicks reached the selected-target-reading
author with a semantically valid JSON answer and then rejected its textual
wrapper. The first answer was one JSON fence; the second was one JSON fence
followed by a completion note. Both failures were representation failures,
not failures of the reading validator.

## Audit question

For every production agent-to-machine boundary, identify all four layers:

1. the reply shape promised by the prompt;
2. the Agency envelope field selected by transport;
3. the textual representation decoder;
4. the semantic validator or marker consumer.

An entry is unsafe when it is absent from the executable index, owns an
independent decoder for a shared representation, or silently treats a
representation failure as a substantive refusal.

## Results

The executable index now contains 19 production handoff sites, including 15
agent-reply sites. Every agent-reply entry must name both `:representation`
and `:decoder`; the index audit refuses otherwise.

| Boundary family | Representation | Decoder | Finding |
|---|---|---|---|
| selected-target reading | JSON object | `handoff/response-json-object` | Shared after clicks 3–4. |
| occurrence-square authoring | JSON object | `handoff/response-json-object` | Was omitted from the index; now registered and already shared. |
| selected-policy reading | JSON object | `handoff/response-json-object` | **Defect found and repaired:** it still used a private raw `json/parse-string`, so the next fenced answer could fail identically. |
| open-cascade author and reviewer | JSON object | `handoff/response-json-object` through `response-map` | Both were omitted from the index; now registered and behaviourally covered. |
| want interpretation | one fenced EDN form | `want-interpretation/parse-reply` | Separate grammar by design; accepts surrounding prose but exactly one matching fenced form. |
| legacy interpretation | durable EDN artifact | `interpretation-evidence/validate-record` | Agent writes a named artifact; terminal prose is not parsed as the receipt. |
| implementation author/build cure | anchored line markers plus repository artifact | `task-execution-evidence/job-text` and author marker parser | Not a JSON boundary. Prompt and parser agree about line anchors; full result is retained beyond the short summary. |
| initial/revision review | anchored line markers | `task-execution-evidence/review-verdict` | Not a JSON boundary. Full result and non-prompt events are retained; the first anchored verdict is consumed. |
| standing decision | durable EDN artifact | `limb-evidence/validate-standing-decision` | Terminal prose is not used as the decision. |
| strategic selection service | parsed service map | `strategic-selection-attempt` | Service response, not free-form agent prose. |
| Python repair loop and quota probe | terminal text/state | registered exceptions | Outside the Clojure exchange, but now explicitly record their representation and decoder in the index. They cannot enter policy selection as JSON. |

## Shared JSON rule

There is now one JSON representation policy for every indexed JSON handoff:

- a directly supplied map is accepted;
- one bare JSON object occupying the response is accepted;
- exactly one fenced JSON object is accepted, with or without surrounding
  explanatory prose;
- multiple fences, unfenced prose, arrays/scalars, malformed JSON, and
  absence are rejected;
- decoding never substitutes for the boundary's schema, identity, digest,
  request-binding, or coverage validation.

The selected-policy runner seam was the only remaining independent agent
JSON parser found in the live path. It now calls the shared decoder. A
production-shaped regression sends a fenced accepted verdict with prose
before and after it and proves that the same selected policy's debt is
discharged.

## Controls added

- Every indexed agent reply must declare a representation and decoder.
- Every indexed JSON-object reply must name
  `:handoff/response-json-object`; an independent decoder fails the test.
- Every Clojure caller of `handoff/exchange` and `handoff/response-map` must
  live in a source file represented by the index.
- Behavioural tests cover the shared rule through selected-target reading,
  occurrence-square publication, selected-policy reading, and the production
  open-cascade author/reviewer composition.

## Deliberate exclusions and remaining limits

JSON read from files, HTTP endpoints, or subprocess output is not an agent
textual handoff and remains strict raw JSON. Making those parsers accept
Markdown would weaken unrelated protocols. The fenced-EDN and line-marker
families retain their own explicit grammars rather than being coerced through
the JSON decoder.

The index is a checked source inventory, not a proof that an entirely new
transport call can never be introduced. New `handoff/exchange` and
`handoff/response-map` callers are detected automatically; other new Agency
call paths still require adding an index entry as part of review. This is the
remaining governance boundary, stated rather than hidden.
