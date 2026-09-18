(ns futon2.aif.wm08-f4-designation-wiring-test
  "Option A (claude-4, 2026-09-18): the ordinary construction seam —
  receipt-construction/construct!, the function the live full-loop runner
  calls — resolves the caller-declared F4 designation instead of passing a
  hardcoded nil. PRESENT means required and validated through
  find-designation (self-supplied refused, occurrence bound, designated set
  reaches validate-result!); ABSENT is RECORDED as :not-supplied, never
  silently vacuous. Exercised on the Route-A frozen occurrence's real record
  and bytes (rehearsal corpus; no ordinary-run credit claimed)."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.find-designation :as fd]
            [futon2.aif.find-receipt :as finder]
            [futon2.aif.receipt-construction :as construction]
            [futon2.aif.wm08-route-a-test :as route-a])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- empty-history-root []
  (-> (Files/createTempDirectory "wm08-f4-wiring" (make-array FileAttribute 0))
      .toFile .getAbsolutePath))

(defn- construct-with
  "Run the ordinary seam over the Route-A record with a private empty
  history (first attempt) and the given :f4-designation cfg."
  [cfg]
  (let [{:keys [record read-bytes]} (route-a/load-from-disk)]
    (construction/construct! record read-bytes
                             {:interpretation-library-root route-a/library-root
                              :interpretation-history-roots [(empty-history-root)]
                              :f4-designation cfg})))

(defn- baseline-found
  "The undesignated find result: :selected are the patterns that FIRED (a
  designation naming one of these is proven wrong by the run itself)."
  []
  (:find-result (:receipted-construction (construct-with nil))))

(defn- artifact
  [author-role designated]
  {:schema fd/schema-id
   :author {:id "wiring-test-designator" :role author-role}
   :occurrence (route-a/frozen-occurrence)
   :designated designated
   :basis "wiring test: exercise the seam, not the authority"})

(defn- fired-and-unfired
  []
  (let [{:keys [record read-bytes]} (route-a/load-from-disk)
        found (baseline-found)
        fired (set (:selected found))
        patterns (sort (:patterns (:repository (finder/context record read-bytes route-a/library-root))))]
    {:fired fired
     :unfired (first (remove fired patterns))
     :a-fired (first (filter fired patterns))}))

(defn- refusal-reason
  "The typed refusal reason of a construct! call (assert on it; the
  exception MESSAGE is generic, the reason is the record)."
  [cfg]
  (try (construct-with cfg) nil
       (catch clojure.lang.ExceptionInfo e (:reason (ex-data e)))))

(deftest absent-designation-is-recorded-not-silent
  (let [retained (:receipted-construction (construct-with nil))]
    ;; The receipt SAYS the external designation was not supplied; before
    ;; this slice the same run said nothing at all about F4's origin.
    (is (= {:status :not-supplied} (:f4-designation retained)))
    ;; ...and the find result still records its own honest vacuity.
    (is (= :vacuous (:f4 (:find-result retained))))))

(deftest present-designation-reaches-construction-and-is-recorded
  (let [{:keys [unfired]} (fired-and-unfired)
        ;; designate an UNFIRED pattern: the run keeps it out of the
        ;; selected set, so the designation is discriminating and survives
        ;; validate-result!'s :designated-pattern-fired check.
        a (artifact :designation-author #{unfired})
        retained (:receipted-construction (construct-with a))]
    (is (= :validated (:status (:f4-designation retained))))
    (is (= (:author a) (:author (:f4-designation retained))))
    (is (= :discriminating (:f4 (:f4-designation retained))))
    (is (= :discriminating (:f4 (:find-result retained)))
        "the designated set reached construct: validate-result! computed
        :discriminating from the EXTERNAL set, not from hardcoded nil")))

(deftest interpreter-cannot-supply-its-own-designation
  ;; The negative control that is the entire point of F4: an artifact whose
  ;; author role is the interpreter's own side of the run is refused BEFORE
  ;; construction — the typed self-supply refusal, fired through the
  ;; ordinary seam for the first time outside a unit test of find-designation.
  (let [{:keys [unfired]} (fired-and-unfired)
        a (artifact :interpreter #{unfired})]
    (is (= :self-supplied-designation (refusal-reason a)))))

(deftest a-designated-pattern-firing-refuses-the-run
  ;; The other negative control: designating a pattern the run FIRED proves
  ;; the designation wrong on this occurrence, and validate-result! refuses
  ;; rather than recording a discriminating status it did not earn.
  (let [{:keys [a-fired]} (fired-and-unfired)
        a (artifact :designation-author #{a-fired})]
    (is (= :designated-pattern-fired (refusal-reason a)))))

(deftest wrong-occurrence-binding-refuses
  ;; An artifact pinned to a DIFFERENT occurrence is the typed binding
  ;; refusal — an external designation for another snapshot never reaches
  ;; this run's designated set. NOTE the seam's occurrence discipline
  ;; mirrors validate-external-expectations!: a BARE artifact carries its
  ;; own binding (self-consistent by construction); the cross-check bites
  ;; when the caller supplies the run's binding via the wrapper shape
  ;; {:artifact ... :occurrence <the run's frozen binding>} — which is how
  ;; a runner pins an artifact to THIS occurrence.
  (let [{:keys [unfired]} (fired-and-unfired)
        a (artifact :designation-author #{unfired})
        a' (assoc-in a [:occurrence :pinned-at] "1999-01-01T00:00:00Z")
        cfg {:artifact a' :occurrence (route-a/frozen-occurrence)}]
    (is (= :occurrence-binding-mismatch (refusal-reason cfg))
        "artifact-for-another-snapshot vs the run's own binding refuses"))
  ;; and the matching positive: the same wrapper with the TRUE binding passes.
  (let [{:keys [unfired]} (fired-and-unfired)
        a (artifact :designation-author #{unfired})
        cfg {:artifact a :occurrence (route-a/frozen-occurrence)}
        retained (:receipted-construction (construct-with cfg))]
    (is (= :validated (:status (:f4-designation retained))))))
