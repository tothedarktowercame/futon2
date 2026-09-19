#!/usr/bin/env bb
;; wm_run_validity.bb — realness v2: is each RUN valid, on its own record?
;;
;; Joe's ruling, 2026-09-19 (RULING-run-certificate-uniformity-2026-09-19.md):
;; every run of the machine — ordinary click, self-repair, any gated run —
;; carries the same certificate requirement. This checker reads the durable
;; per-run record (data/wm-runs/tick-run-record-*.edn) and reports, per run,
;; whether the five required quantities are present and valid:
;;
;;   :c-source          C on the decision, :status :derived (typed
;;                      :derived-no-overlap accepted as present-but-flagged)
;;   :rates-provenance  outcome-rate provenance recorded (value reported)
;;   :posterior         a cascade posterior over >1 candidate, with per-policy
;;                      F CONSUMED — :f-status :computed. A policy whose
;;                      :f-status is :computed-not-attached is reported
;;                      :f-not-consumed: selection never used that value
;;                      (d8047a30 records computed and consumed separately).
;;   :u37               enumeration-completeness verdict + membership diff
;;   :g-terms           the recorded G-term decomposition (path settled by
;;                      the P-1 lane; searched like the rest — see below)
;;
;; EVIDENCE, NEVER A HALT: a run missing fields is reported :invalid and
;; nothing stops. Exit is nonzero only for the checker's own failures
;; (unreadable record, selftest failure) — never for an invalid run.
;;
;; Field locations are SEARCHED, not dictated: each field is looked up at any
;; map path in the record (depth-bounded, :backtrace subtrees excluded, so
;; embedded historical checkpoints of prior repairs are never mistaken for
;; this run's own quantities), and the path where it was found is printed.
;; The lanes landing EV-uniform-run-record fields choose the attachment
;; point; this checker names where it found them, so location drift is
;; visible instead of fatal.
;;
;;   bb scripts/wm_run_validity.bb                # all run records
;;   bb scripts/wm_run_validity.bb <file...>      # specific records
;;   bb scripts/wm_run_validity.bb selftest       # fixtures: must PASS a
;;                                                # complete record and
;;                                                # REJECT each perturbation

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.string :as str])

(def run-dir (str (System/getProperty "user.home") "/code/futon2/data/wm-runs"))
(def max-depth 8)

;; ALLOWLIST, NOT DENYLIST (claude-4, 2026-09-19, r110 review finding F1).
;; The first cut searched anywhere except :backtrace. That is a denylist over
;; an unbounded key space, so it fails PERMISSIVE: a synthetic record whose
;; five fields existed only under
;;   [:some-unrelated-archive :stashed-previous-run :decision ...]
;; was reported VALID (5/5 ok). A run that did nothing, carrying a stashed copy
;; of a previous run's quantities, passed the facade detector -- which is the
;; one thing Joe's uniformity ruling exists to catch.
;; So: still SEARCH (the lanes have not fixed attachment points yet, and the
;; printed path is how drift stays visible), but only WITHIN a declared root,
;; and report anything found outside it as :bad with the offending path rather
;; than silently ignoring it.
(def declared-roots
  "Where this run's own quantities may live. Anything else is somebody else's."
  #{:decision})

(defn- search
  "All non-:backtrace paths to key k, shallowest first."
  [form k]
  (letfn [(walk [f path depth]
            (when (and (<= depth max-depth) (map? f))
              (concat (when (contains? f k) [(conj path k)])
                      (mapcat (fn [[kk vv]]
                                (when-not (= kk :backtrace)
                                  (walk vv (conj path kk) (inc depth))))
                              f))))]
    (sort-by count (walk form [] 0))))

(defn find-field
  "Shallowest path to k UNDER a declared root, else nil."
  [form k]
  (first (filter #(contains? declared-roots (first %)) (search form k))))

(defn out-of-root
  "Shallowest path to k OUTSIDE every declared root, else nil. Used to report
   a near-miss as :bad-location rather than a bare :missing -- a field sitting
   somewhere else is a different and more interesting fact than its absence."
  [form k]
  (first (remove #(contains? declared-roots (first %)) (search form k))))

(defn get-found [form path] (when path (get-in form path)))

;; Each check returns {:field :verdict (:ok|:flagged|:missing|:bad) :at :note}
(defn check-c-source [r]
  (let [p (find-field r :c)
        status (:status (get-found r p))]
    (cond
      (nil? p) {:field :c-source :verdict :missing}
      (= :derived status) {:field :c-source :verdict :ok :at p}
      (= :derived-no-overlap status)
      {:field :c-source :verdict :flagged :at p
       :note "typed no-overlap: uniform spec scored this decision"}
      :else {:field :c-source :verdict :bad :at p :note (pr-str status)})))

(defn check-rates [r]
  (let [p (find-field r :rates-provenance)]
    (if p
      {:field :rates-provenance :verdict :ok :at p
       :note (pr-str (get-found r p))}
      {:field :rates-provenance :verdict :missing})))

(defn check-posterior [r]
  (let [law-p (find-field r :selection-law)
        posterior (:posterior (get-found r law-p))
        cert-p (find-field r :selection-certificate)
        policies (:policies (get-found r cert-p))
        not-consumed (seq (filter #(= :computed-not-attached (:f-status %)) policies))]
    (cond
      (nil? law-p) {:field :posterior :verdict :missing}
      (not (and (map? posterior) (> (count posterior) 1)))
      {:field :posterior :verdict :bad :at law-p
       :note (str "posterior over " (count posterior) " candidate(s)")}
      (nil? cert-p)
      {:field :posterior :verdict :flagged :at law-p
       :note "posterior present; no selection-certificate (per-policy F unverifiable)"}
      not-consumed
      {:field :posterior :verdict :flagged :at cert-p
       :note (str "f-not-consumed for " (count not-consumed) " polic(ies): "
                  "selection never used those F values")}
      :else {:field :posterior :verdict :ok :at cert-p})))

(defn check-u37 [r]
  (let [p (find-field r :enumeration-completeness)
        v (get-found r p)]
    (cond
      (nil? p) {:field :u37 :verdict :missing}
      (nil? (:verdict v)) {:field :u37 :verdict :bad :at p :note "no verdict"}
      :else {:field :u37 :verdict :ok :at p :note (pr-str (:verdict v))})))

(defn check-g-terms [r]
  ;; :g-term-decomposition is the name the PRODUCER chose (futon2 cd952809,
  ;; full-loop-runner). I wrote this checker's key list before that landed and
  ;; then reviewed the producing commit without noticing the mismatch, so the
  ;; quantity was being written and reported missing at the same time. The
  ;; producer's name is the better one; the checker accepts it rather than
  ;; forcing a rename of a committed, warranted write.
  (let [p (or (find-field r :g-terms)
              (find-field r :g-decomposition)
              (find-field r :g-term-decomposition))
        v (get-found r p)]
    (cond
      (nil? p)
      {:field :g-terms :verdict :missing
       :note "decomposition fields land with the P-1 lane"}

      ;; The producer labels its own absence. The first cut of this check
      ;; asked only whether the KEY was there, so a census reading
      ;; {:status :missing :reason :no-recorded-cascade-selection} scored a
      ;; point -- found by claude-4 on the 16:30 production click, whose
      ;; repair branch bypassed cascade selection and still reported
      ;; g-terms ok. check-u37 above already reads its value; this one did
      ;; not, and presence of a placeholder is not presence of a quantity.
      ;; Legacy :g-terms records carry no :status and are unaffected.
      (and (map? v) (contains? v :status) (not= :present (:status v)))
      {:field :g-terms :verdict :missing :at p
       :note (str "census says " (pr-str (:status v))
                  (when (:reason v) (str ": " (pr-str (:reason v)))))}

      :else {:field :g-terms :verdict :ok :at p})))

(def checks [check-c-source check-rates check-posterior check-u37 check-g-terms])

(def field-keys
  "The record key each field is found by, for near-miss reporting."
  {:c-source :c :rates-provenance :rates-provenance
   :posterior :selection-certificate :u37 :enumeration-completeness
   :g-terms :g-term-decomposition})

(defn- locate-near-miss
  "A field absent from every declared root but PRESENT somewhere else is a
   different and more interesting fact than a field that is simply absent: it
   says this run is carrying somebody else's quantities. Report it as :bad
   with the offending path rather than as a bare :missing."
  [r result]
  (if (not= :missing (:verdict result))
    result
    (if-let [elsewhere (some-> (field-keys (:field result)) (->> (out-of-root r)))]
      (assoc result :verdict :bad :at elsewhere
             :note "found OUTSIDE the declared root — not this run's quantity")
      result)))

(defn validity [r]
  (let [results (mapv #(locate-near-miss r (% r)) checks)
        missing (filter #(#{:missing :bad} (:verdict %)) results)]
    {:results results
     :verdict (if (seq missing) :invalid :valid)}))

(defn report-run! [file r]
  (let [{:keys [results verdict]} (validity r)]
    (println (format "\n-- %s  run %s  terminal %s"
                     (.getName (io/file file))
                     (or (:run/id r) "?")
                     (pr-str (get-in r [:terminal :outcome]))))
    (println "  " (str/upper-case (name verdict))
             (str "(" (count (filter #(= :ok (:verdict %)) results)) "/"
                  (count results) " ok)"))
    (doseq [{:keys [field verdict at note]} results]
      (println (format "   %-18s %-8s %s%s" (name field) (name verdict)
                       (if at (pr-str at) "-")
                       (if note (str "  " note) ""))))
    verdict))

(defn run-files [args]
  (if (seq args)
    (map io/file args)
    (->> (.listFiles (io/file run-dir))
         (filter #(re-matches #"tick-run-record-.*\.edn" (.getName %)))
         (sort-by #(.getName %)))))

;; ---------------------------------------------------------------- selftest
(def complete-fixture
  {:run/id "fixture-complete"
   :terminal {:outcome :grounded-change}
   :decision {:c {:status :derived}
              :rates-provenance :factorized-nonzero-rates
              :selection-law {:posterior {[:a :t1] 0.6 [:b :t2] 0.4}}
              :selection-certificate
              {:policies [{:id :a :f 0.2 :f-status :computed}
                          {:id :b :f 0.1 :f-status :computed}]}
              :enumeration-completeness {:verdict :complete :kinds []}
              :g-terms {:risk 0.3 :ambiguity 0.1 :novelty 0.0}}})

(def perturbations
  ;; label, transform, field expected to reject (:missing/:bad/:flagged)
  [["c-status-stripped" #(update-in % [:decision :c] dissoc :status) :c-source]
   ["rates-removed" #(update % :decision dissoc :rates-provenance) :rates-provenance]
   ["one-candidate-posterior"
    #(assoc-in % [:decision :selection-law :posterior] {[:a :t1] 1.0}) :posterior]
   ["f-computed-not-attached"
    #(assoc-in % [:decision :selection-certificate :policies 0 :f-status]
               :computed-not-attached) :posterior]
   ["u37-removed" #(update % :decision dissoc :enumeration-completeness) :u37]
   ["g-terms-removed" #(update % :decision dissoc :g-terms) :g-terms]
   ;; The case that made this checker wrong, kept as a permanent control
   ;; (claude-4, 2026-09-19). Every quantity is present and correct -- just
   ;; filed under somebody else's subtree, as a stashed copy of an earlier
   ;; run. The first cut reported VALID 5/5. A run that did nothing must not
   ;; validate on another run's numbers, and the only way to know this still
   ;; holds is to keep asking.
   ;; Second permanent control (claude-4, after click 1 of 5). The census is
   ;; present and well-formed and says of itself that it has nothing: exactly
   ;; what the repair branch writes. It must not score.
   ["g-terms-census-says-missing"
    #(assoc-in % [:decision :g-terms]
               {:schema :wm/g-term-decomposition-v1 :status :missing
                :reason :no-recorded-cascade-selection :policies []})
    :g-terms]
   ["quantities-relocated-off-root"
    (fn [rec] {:run/id (:run/id rec) :terminal (:terminal rec)
               :some-unrelated-archive {:stashed-previous-run
                                        {:decision (:decision rec)}}})
    :c-source]])

(defn selftest! []
  (let [base (validity complete-fixture)
        ok? (atom (= :valid (:verdict base)))]
    (println "complete fixture:" (:verdict base))
    (doseq [[label f field] perturbations]
      (let [{:keys [results]} (validity (f complete-fixture))
            r (first (filter #(= field (:field %)) results))
            rejected? (contains? #{:missing :bad :flagged} (:verdict r))
            ;; :flagged is NOTICED but does not make a run :invalid. Printing
            ;; "REJECTED" for both overstated what the flagged case shows --
            ;; five of these perturbations invalidate a run and one only
            ;; annotates it (claude-4, r110 finding F3). Say which.
            invalidates? (contains? #{:missing :bad} (:verdict r))]
        (println (format "  %-30s -> %-18s %s" label
                         (str (name field) ":" (name (:verdict r)))
                         (cond (not rejected?) "NOT REJECTED — selftest FAIL"
                               invalidates?    "REJECTED (invalidates the run)"
                               :else           "NOTICED (flagged, does NOT invalidate)")))
        (when-not rejected? (reset! ok? false))))
    (if @ok?
      (println "SELFTEST PASS: accepts the complete record, rejects every perturbation")
      (do (println "SELFTEST FAIL") (System/exit 1)))))

;; ---------------------------------------------------------------- main
(let [args *command-line-args*]
  ;; The negative control runs on EVERY invocation (claude-4, r110 finding F2).
  ;; It previously ran only when "selftest" was the sole argument, so checking
  ;; real records never exercised it -- "SELFTEST PASS required for exit 0" was
  ;; true only of the circular invocation. Now every verdict this tool prints
  ;; carries the evidence that the tool can still say no.
  (selftest!)
  (if (= ["selftest"] (vec args))
    nil
    (let [files (run-files args)
          verdicts (doall (for [f files]
                            (report-run! f (edn/read-string
                                            {:default (fn [_ v] v)} (slurp f)))))]
      (println "\n== summary" (pr-str (frequencies verdicts)))
      (println "   (invalid runs do not fail this process: evidence, not a gate)"))))
