# WM-REQREC-D — the third flight's locator asks and their request records (discovery, read-only)

claude-10, 2026-09-26, for claude-8 (packet WM-REQREC-D). Read at futon2
594ebce8.

Records read:
- futon3c `holes/labs/M-wm-wiring/spike/timeline-flight-74325007.md`;
- the Agency jobs at :7070 (their prompts);
- futon2 `data/wm-interpretations/requests/` (file names and mtimes, read,
  not written).

No code, no test, no flight.

**Finding: the records exist.** The third flight's locator asks did not skip
their request records. They reused the second flight's, because a request
record is content-addressed and the questions were identical. Every request
id the third flight sent resolves to a file.

## 1. The read step's paths

There is one path, whatever is already published.

`read-fn` (`flight_runner.clj`, the `(defn read-fn` form) issues every reading
through `read-one`:
- the criteria request, only when `:criteria?` is needed;
- the coverage request;
- one locator request per token in `[:source :readings-needed :locators]`;
- the constraints request.

`read-one` begins `(let [issued (wi/issue! store issued) …` before calling
`answer-fn`. So every locator ask goes through `wi/issue!`, on both paths:
criteria published earlier (the third flight) or criteria published in the
same step (the second flight). No request map reaches `answer-fn` without
being issued.

`wi/issue!` (`want_interpretation.clj`, `(defn issue!`) names the request by
content, `(content-id :request (dissoc request :retrieval))` (`content-id` at
`:313-314`: the first 16 hex of the sha256 of the request's `pr-str`). It
writes `requests/<id>.edn` only `(when-not (.isFile f) …)`. The same question
asked again returns the same id and leaves the existing file untouched.

## 2. What the record is for, and what the third flight's record would carry

The request record is the evidence of what the seat was asked. `publish!`
refuses a response whose request id does not resolve to an issued request.
WM-CUE-I's fixture pinned its `:criterion :stated` text from these files.

**The third flight's locator asks**, from the Agency job prompts at :7070
(`Requisition: M-autoclock-in — War Machine locator request <id>`):

| job | request id | file (mtime) |
|---|---|---|
| 43fd9181 | request-ee877276e5bbd6f3 | exists, 2026-09-25 22:58:50 |
| 4b29696f | request-7a22ebcc5af3c2ae | exists, 22:59:06 |
| 2a06f334 | request-a8b9fb06e9e0dba1 | exists, 22:59:22 |
| 3780b8ca | request-eac34688cf1b8e75 | exists, 23:00:13 |
| d5e096a0 | request-ce99d13031afa902 | exists, 23:00:26 |
| 6c70d41d | request-da53dffe6dad625d | exists, 23:00:52 |

These are the second flight's locator requests for the same tokens. The
target, the mission, and each criterion's token and `:stated` text were
unchanged between the flights, so the content ids are identical. Nothing is
lost: the request each seat answered is on disk, byte-identical to what it
was sent (less `:retrieval`, which the id excludes). A flight record's
`:request-id` would name those files.

The timeline reports "request written" only for files whose mtime falls
inside the flight, which is why only the constraints request (23:43:52Z) and
the interpretation request (23:48:04Z) appear: those two were new questions.
The constraints request's known tokens differed, and the interpretation
request was the first of its kind. Six locator jobs are on the timeline. I
did not find a seventh job's prompt among claude-5's last 80 jobs.

## 3. The ask step and the constraints request

The same holds. The ask step's `issue-request` calls `wi/issue!` (the
`(defn- issue-request` form in `flight_runner.clj`), and the constraints
request goes through `read-one`. Both are content-addressed, written once,
and reused when asked again. The third flight's interpretation request
(`request-f95195b38e9ec456`) and constraints request (`request-a6477ab698bb528f`)
were new, so they were written.

## 4. Size, map, falsifier, recommendation

**No wiring fix is needed.** No map rows are touched.

**Falsifier (fourth flight):** every `:request-id` on the flight record
resolves to a file under `requests/`. That holds by construction:
`issue!` returns the id of a file that exists, either written now or
already there.

**What a reader of the record cannot see today** is whether an ask reused an
earlier request, meaning the same question was asked again. For rejected
locators that is the case every flight: they are re-asked, whereas a
declined locator is recorded per text and not re-asked. Surfacing it would
mean the entry carrying the request file's first-issued time, a new field.
I don't recommend that without a case; the file's own mtime already says
it.

**Recommendation:** close this as not a defect. Change the timeline tool to
look up every request id a flight's jobs carry, and print "request reused
(issued <mtime>)" for files that already existed, instead of inferring
absence from new mtimes. If a guard is wanted, one test asserting that
`issue!` twice returns the same id and leaves the file's bytes unchanged
would pin the reuse. That is the only size.
