(ns futon2.aif.wm.enacted.e-comprehension-foundation
  "Mechanical enactment of the reviewed completion checks for the
  E-comprehension-foundation compiled cascade (selected digest
  cc86a718c4c95d02a5bd932bd29d4eb55d1f87dd1028fb7af4a85b6bd49c18f5,
  compiled digest
  504865175c020bda6367f664e14448275427e194945a9e1a5b5d7edee0e2f72e).

  The nine reviewed checks each name an observable condition in the
  excursion opening at
  futon6/holes/excursions/E-comprehension-foundation.md (content sha256
  6863756378922c1aa289e75c0c750f04bc66e797e04dae721f3c9e39b1afb663).
  This namespace pins that excerpt (excerpt sha256
  562722c3cad7253362f9a44ee61f16451889c58dfaa1159fd084ffc00fe5e68c,
  section chars 0-556) and turns each check into a locator: a span of
  the excerpt and the exact text the check's description quotes.  A
  check is satisfied only when the excerpt slice at its recorded span
  equals the quoted text.

  Offsets are Unicode codepoints, zero-based, end-exclusive.  The
  excerpt's only non-ASCII character is the em dash (BMP), so Java
  string indices coincide with codepoint offsets here; the digest pin
  refuses any edit that would change that.

  This enacts agent/evidence-over-assertion and
  stack-coherence/evidence-ledger (checks as code against pinned
  evidence), agent/state-is-hypothesis and
  agent/commitment-varies-with-confidence (the OPEN/low-confidence
  verdict is asserted, not assumed), agent/scope-before-action (title
  and artifact scope spans verified), agency/self-attribution (byline
  span verified) and agent/handoff-preserves-context (the Phase 3
  closing condition is verified as stated in the excerpt).")

(def excerpt-sha256
  "6863756378922c1aa289e75c0c750f04bc66e797e04dae721f3c9e39b1afb663 is the
  whole excursion file; the excerpt below is its opening 0-556 section."
  "562722c3cad7253362f9a44ee61f16451889c58dfaa1159fd084ffc00fe5e68c")

(def excerpt
  "Pinned opening of futon6/holes/excursions/E-comprehension-foundation.md
  (section chars 0-556, excerpt sha256 above)."
  "# E-comprehension-foundation — corpus-relative comprehension as the foundation\n\n**VERDICT (2026-10-09, provisional):** OPEN — Foundational design note with an explicitly open pilot question and no completion marker in head or tail. _(WM status classification by zai-4, low confidence; not yet confirmed by the author.)_\n\n*claude-1 + Joe, 2026-06-22. The holistic architecture under the readiness\n\"cards\": what the pipeline produces, why \"improves as we run\" is true (and where),\nand the discipline that keeps the foundation solid so Phase 3 can assume it.*")

(def checks
  "The nine reviewed completion checks as locators: each names its hole,
  its check keyword, and the excerpt span carrying the quoted evidence
  the check describes."
  [{:hole-id ["agent/scope-before-action" 0]
    :check :title-names-corpus-relative-comprehension
    :span [0 78]}
   {:hole-id ["stack-coherence/evidence-ledger" 0]
    :check :foundation-claim-not-marked-done
    :span [119 231]}
   {:hole-id ["agent/state-is-hypothesis" 1]
    :check :status-open-with-explicit-pilot-question
    :span [119 231]}
   {:hole-id ["agent/commitment-varies-with-confidence" 2]
    :check :low-confidence-and-author-confirmation-outstanding
    :span [232 319]}
   {:hole-id ["agency/self-attribution" 3]
    :check :byline-names-claude-1-joe-and-date
    :span [321 349]}
   {:hole-id ["agent/scope-before-action" 4]
    :check :holistic-architecture-under-readiness-named
    :span [350 395]}
   {:hole-id ["agent/evidence-over-assertion" 5]
    :check :cards-explain-what-pipeline-produces
    :span [396 432]}
   {:hole-id ["agent/state-is-hypothesis" 6]
    :check :improves-as-we-run-explained-and-delimited
    :span [433 478]}
   {:hole-id ["agent/handoff-preserves-context" 8]
    :check :phase-3-can-assume-foundation
    :span [530 556]}])

(defn excerpt-digest-matches?
  "True when the pinned excerpt still hashes to its recorded sha256."
  []
  (= excerpt-sha256
     (->> (.digest (java.security.MessageDigest/getInstance "SHA-256")
                   (.getBytes excerpt "UTF-8"))
          (map (partial format "%02x"))
          (apply str))))

(defn verify-check
  "Evaluate one locator against a source string: satisfied only when the
  span is in bounds and the slice it names is the quoted evidence text."
  [source {:keys [hole-id check span expected]}]
  (let [[start end] span
        observed (when (<= 0 start end (count source))
                   (subs source start end))]
    {:hole-id hole-id
     :check check
     :span span
     :expected expected
     :observed observed
     :satisfied (and (string? observed) (= expected observed))}))

(def expected-text
  "The quoted evidence text each check's description names, keyed by check
  keyword.  A check is satisfied only when its excerpt span holds exactly
  this text."
  {:title-names-corpus-relative-comprehension
   "# E-comprehension-foundation — corpus-relative comprehension as the foundation"
   :foundation-claim-not-marked-done
   "OPEN — Foundational design note with an explicitly open pilot question and no completion marker in head or tail."
   :status-open-with-explicit-pilot-question
   "OPEN — Foundational design note with an explicitly open pilot question and no completion marker in head or tail."
   :low-confidence-and-author-confirmation-outstanding
   "_(WM status classification by zai-4, low confidence; not yet confirmed by the author.)_"
   :byline-names-claude-1-joe-and-date
   "*claude-1 + Joe, 2026-06-22."
   :holistic-architecture-under-readiness-named
   "The holistic architecture under the readiness"
   :cards-explain-what-pipeline-produces
   "\"cards\": what the pipeline produces,"
   :improves-as-we-run-explained-and-delimited
   "why \"improves as we run\" is true (and where),"
   :phase-3-can-assume-foundation
   "so Phase 3 can assume it.*"})

(defn verify
  "Run every reviewed check against a source string (default: the pinned
  excerpt).  Returns one result map per check plus an :all-satisfied?
  roll-up.  A missing or mutated evidence span shows as :satisfied false
  with the observed text, never as an exception."
  ([] (verify excerpt))
  ([source]
   (let [results (mapv (fn [{:keys [check] :as locator}]
                         (verify-check source
                                       (assoc locator
                                              :expected (get expected-text check))))
                       checks)]
     {:results results
      :all-satisfied? (every? :satisfied results)})))
