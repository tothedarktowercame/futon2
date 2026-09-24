(ns futon2.aif.selection-certificate-test
  "WIRE-f-on-tick: emission, unchanged decisions and the existing Lean checker."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.policy :as policy]))

(defn- baseline []
  (edn/read-string
   (slurp (io/resource "fixtures/selection-certificate/before.edn"))))

(defn- select [entries beta]
  (policy/select-action-cascades entries {:beta beta}))

(deftest decisions-byte-identical
  ;; Captured from the unmodified selector. Includes actual tick-001 G values,
  ;; a tied field, and scorer-produced infinite F; beta varies away from 1.
  ;; Both arms cross the same EDN read boundary before byte comparison:
  ;; sets in the original in-memory record can print in another order.
  (doseq [{:keys [ranked beta decision-bytes]} (:cases (baseline))]
    ;; Precision metadata was added after this historical snapshot. Assert
    ;; its values explicitly; compare every original decision field unchanged.
    ;; Only the new diagnostic records are projected away (tested independently
    ;; against the frozen narrative runs in selection-discrimination-test).
    (is (= (pr-str (update (edn/read-string decision-bytes) :selection-law
                          assoc :gamma (/ 1.0 beta) :tau beta :tau-source :declared-beta))
           (pr-str (-> (select ranked beta)
                       (dissoc :selection-certificate)
                       (update :selection-law dissoc
                               :policy-comparison :action-comparison :near-tie-threshold
                               ;; :enacted-steps joined the law in b1979ce2
                               ;; (2026-09-23) AFTER this baseline was captured.
                               ;; It is the same class as the three above --
                               ;; additive reporting that changes no winner, no
                               ;; marginal key and no tie-break -- and is pinned
                               ;; independently in enacted-step-test. Projected
                               ;; away rather than RE-CAPTURING the baseline:
                               ;; re-recording the bytes to make a byte-identity
                               ;; test pass is how such a test stops meaning
                               ;; anything (claude-5, 2026-09-24, found by
                               ;; kimi-6 while checking a different stop-line).
                               :enacted-steps)))))))

(deftest computed-and-consumed-are-distinct
  (let [{:keys [ranked beta]} (first (:cases (baseline)))
        cert (:selection-certificate (select ranked beta))]
    (is (= {:value beta :status :declared} (:beta cert)))
    (is (= 2 (count (:policies cert))))
    (doseq [[entry candidate finite] (map vector ranked (:candidates cert) (:policies cert))]
      (is (= (get-in entry [:certificate :f]) (:computed-f candidate)))
      (is (= ##Inf (get-in candidate [:computed-f :value])))
      (is (= :absent (get-in candidate [:inputs :f :presence])))
      (is (= :computed-not-attached (:f-status candidate) (:f-status finite)))
      (is (= :non-finite-under-identity-a (:reason candidate) (:reason finite)))
      (is (= 0 (:f candidate) (:f finite)))
      (is (= 1 (:habit candidate) (:habit finite)))
      (is (= :declared-neutral (:habit-status finite))))))

(deftest presence-and-attached-inputs
  (doseq [[extra presence] [[{} :absent] [{:f nil :habit nil} :null]
                           [{:f false :habit false} :false]
                           [{:f 0 :habit 1} :present]
                           [{:f 2 :habit 3} :present]]]
    (let [entry (merge {:action :a :controller-score 1} extra)
          cert (:selection-certificate (select [entry] 2))
          c (first (:candidates cert))]
      (is (= presence (get-in c [:inputs :f :presence])))
      ;; Malformed action identity cannot read E. The mandatory seam records
      ;; its whole-menu neutral fallback, overriding any caller habit.
      (is (= :present (get-in c [:inputs :habit :presence])))
      (is (= :missing-policy-identity (get-in c [:habit-provenance :reason])))
      (is (= (or (:f extra) 0) (:f c)))
      (is (= 1 (:habit c)))
      (is (= (if (= presence :present) :attached :declared-neutral) (:f-status c)))))
  ;; The guard is reachable for an ATTACHED non-finite F. No new guard added.
  (doseq [f [##Inf ##-Inf ##NaN]]
    (is (= :invalid-free-energy
           (try (select [{:action :a :controller-score 1 :f f}] 1)
                (catch clojure.lang.ExceptionInfo e (get-in (ex-data e) [:refusal :kind])))))))

(defn- rational-literal [x]
  (let [r (rationalize x)]
    (if (ratio? r)
      (str "(" (numerator r) " / " (denominator r) ")")
      (str r))))

(defn- lean-status [status reason]
  (case status
    :computed ".computed"
    :declared-neutral ".declaredNeutral"
    :computed-not-attached (str ".computedNotAttached " (pr-str (name reason)))))

(defn- lean-certificate [c]
  (str "({ betaDeclared := " (rational-literal (:beta-declared c))
       ", habit := " (rational-literal (:habit c))
       ", habitStatus := " (lean-status (:habit-status c) nil)
       ", f := " (rational-literal (:f c))
       ", fStatus := " (lean-status (:f-status c) (:reason c))
       " } : SelectionCertificateQ)"))

(deftest ^:slow existing-lean-checker-accepts-and-rejects
  ;; Offline certificate check only: no runner, stores, actuators or shared JVM.
  ;; This is serialization into the EXISTING checker, not a Clojure validator.
  (let [{:keys [ranked beta]} (first (:cases (baseline)))
        decision (select ranked beta)
        certificates (get-in decision [:selection-certificate :policies])
        c (first certificates)
        perturbations [(assoc c :beta-declared 0)
                       (assoc c :habit 2)
                       (assoc c :f 1)]
        source (str "import DarkTower.AIF.CertificateChecker\n"
                    "open DarkTower.AIF\n"
                    (str/join "\n"
                              (concat
                               (map #(str "#guard " (lean-certificate %) ".check = true") certificates)
                               (map #(str "#guard " (lean-certificate %) ".check = false") perturbations)))
                    "\n")
        file (java.io.File/createTempFile "selection-certificate-" ".lean")]
    (try
      (spit file source)
      (let [result (shell/sh "lake" "env" "lean" (.getAbsolutePath file)
                             :dir "../mathlib4")]
        (is (zero? (:exit result)) (pr-str result))
        (println "SELECTION-CERTIFICATE-RECEIPT"
                 (pr-str {:selection decision :positive-controls (count certificates)
                          :negative-controls (count perturbations)
                          :checker "DarkTower.AIF.SelectionCertificateQ.check"
                          :lean-source source :lean-result result
                          :baseline-comparisons (count (:cases (baseline)))
                          :scope :offline-selection-replay})))
      (finally (.delete file)))))


;; --- Empty cascades do not enter the action marginal (2026-09-21) ---
;;
;; Run 2026-09-21-1789951020 selected an EMPTY cascade while the per-policy
;; posterior argmax was a three-pattern cascade at exactly twice its
;; probability. `cascade-first-action` returns nil for an empty cascade, and
;; `bayes-choice` sums mass per key, so 21 structurally distinct do-nothings
;; pooled to 0.785275 and outvoted the best acting key at 0.179031. These pin
;; both directions: a guard that only ever excludes is as wrong as one that
;; never does.

(defn- pattern [id target] {:id id :target target})

(defn- cascade [target pattern-ids score]
  {:action {:kind :cascade-candidate :target target
            :precedence (mapv #(pattern % target) pattern-ids)}
   :controller-score score
   :certificate {:f nil}})

(deftest empty-cascades-cannot-outvote-an-acting-one-by-count
  ;; The bad case the guard is named for: many empty cascades, one acting.
  ;; Under the pooled-nil defect the empties' summed mass wins.
  (let [empties (mapv #(cascade (str "M-empty-" %) [] 10.0) (range 12))
        acting (cascade "M-real" [:pattern/do-the-thing] 10.0)
        decision (policy/select-action-cascades (conj empties acting) {:beta 1})
        law (:selection-law decision)]
    (is (= (pattern :pattern/do-the-thing "M-real") (:chosen-action decision))
        "the single acting cascade must beat twelve pooled do-nothings")
    (is (= "M-real" (get-in decision [:action :target])))
    (is (nil? (get (:softmax-weights decision) nil))
        "nil is not an action and must not appear in the marginal")
    (is (= 12 (get-in law [:excluded-non-actions :count]))
        "the excluded non-actions are counted in the record")
    (is (pos? (get-in law [:excluded-non-actions :mass]))
        "and their mass is recorded, not silently dropped")
    (is (== (:chosen-action-mass decision)
            (get (:softmax-weights decision) (pattern :pattern/do-the-thing "M-real")))
        "the recorded marginal carries the mass that actually decided")))

(deftest recorded-marginal-sums-duplicate-keys-rather-than-overwriting
  ;; The certificate defect: `into {}` overwrote duplicate keys, so the record
  ;; showed the winning key with ONE candidate's probability instead of the
  ;; summed mass. Two cascades whose FIRST acting pattern is the same pattern
  ;; must sum in the record; that is what an action marginal means.
  (let [shared (pattern :pattern/shared "M-shared")
        a {:action {:kind :cascade-candidate :target "M-shared"
                    :precedence [shared (pattern :pattern/tail-a "M-shared")]}
           :controller-score 10.0 :certificate {:f nil}}
        b {:action {:kind :cascade-candidate :target "M-shared"
                    :precedence [shared (pattern :pattern/tail-b "M-shared")]}
           :controller-score 10.0 :certificate {:f nil}}
        other (cascade "M-three" [:pattern/other] 10.0)
        decision (policy/select-action-cascades [a b other] {:beta 1})
        weights (:softmax-weights decision)
        posterior (get-in decision [:selection-law :posterior])
        shared-mass (reduce + 0.0 (keep (fn [[act p]]
                                          (when (= shared (first (:precedence act))) p))
                                        posterior))]
    (is (= 2 (count (filter (fn [[act _]] (= shared (first (:precedence act)))) posterior)))
        "two policies share the first acting pattern")
    (is (== (get weights shared) shared-mass)
        "the marginal SUMS them; `into {}` would have kept only one")
    (is (= shared (:chosen-action decision))
        "and the summed key is what wins")))

(deftest a-lone-typed-no-op-still-competes-and-can-win
  ;; The other side: the guard must not swallow a GENUINE abstention. A typed
  ;; no-op carries :type, keeps its own key, and must remain selectable.
  (let [no-op {:action {:kind :no-op :type :abstain/stand-down}
               :controller-score 10.0 :certificate {:f nil}}
        acting (cascade "M-real" [:pattern/do-the-thing] 10.0)
        decision (policy/select-action-cascades [no-op acting] {:beta 1})
        weights (:softmax-weights decision)]
    (is (contains? weights :abstain/stand-down)
        "a typed no-op is an action and keeps its key in the marginal")
    (is (zero? (get-in decision [:selection-law :excluded-non-actions :count]))
        "nothing is excluded: neither candidate is an empty cascade")))

(deftest an-all-empty-roster-refuses-with-a-reason
  ;; When nothing is acting the machine declines BY SAYING SO, rather than
  ;; picking an empty cascade and letting an author discover it downstream
  ;; (2026-09-21: the author refused
  ;; :empty-selected-cascade-with-operator-gated-target).
  (let [empties (mapv #(cascade (str "M-empty-" %) [] 10.0) (range 3))
        refusal (try (policy/select-action-cascades empties {:beta 1})
                     (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e))))]
    (is (= :no-acting-cascade-candidate (:kind refusal)))
    (is (= 3 (get-in refusal [:detail :empty-cascades])))
    (is (string? (get-in refusal [:detail :reason])))))

(deftest the-2026-09-21-recapture-is-exactly-the-pooled-nil-exclusion
  ;; `decisions-byte-identical` compares against a baseline captured from the
  ;; selector. Excluding empty cascades from the action marginal CHANGED that
  ;; baseline, so it was re-captured -- and a blanket re-capture would accept
  ;; any other drift silently, which is the whole hazard the byte test exists
  ;; to catch. This pins the delta against the PRE-FIX fixture, kept beside
  ;; the new one as evidence: every case either flipped from an empty cascade
  ;; to an acting one, or kept its selection and only had its record fixed.
  (let [before (edn/read-string
                (slurp (io/resource "fixtures/selection-certificate/before-2026-09-20-pooled-nil.edn")))]
    (is (= 6 (count (:cases before))))
    (doseq [[i {:keys [ranked beta decision-bytes]}] (map-indexed vector (:cases before))]
      (let [old (edn/read-string decision-bytes)
            new (dissoc (select ranked beta) :selection-certificate)]
        (is (some? (get (:softmax-weights old) nil))
            (str "case " i ": sanity -- every pre-fix case DID carry a nil key, "
                 "which is the defect being removed"))
        (if (nil? (:chosen-action old))
          ;; The defect: an empty cascade had won. It must not win now.
          (do (is (some? (:chosen-action new))
                  (str "case " i ": an empty cascade won before and must not now"))
              (is (not= (:action old) (:action new))
                  (str "case " i ": the selected action must actually change"))
              (is (seq (:precedence (:action new)))
                  (str "case " i ": the new selection is an acting cascade")))
          ;; Selection was already correct: it must be untouched, and ONLY the
          ;; record may differ.
          (do (is (= (:action old) (:action new))
                  (str "case " i ": a correct selection must not move"))
              (is (= (:chosen-action old) (:chosen-action new))
                  (str "case " i ": nor its action key"))
              (is (== (:chosen-action-mass old) (:chosen-action-mass new))
                  (str "case " i ": nor the mass that decided"))))
        ;; In every case, the nil key is gone from the recorded marginal and
        ;; the excluded mass is accounted for rather than dropped.
        (is (nil? (get (:softmax-weights new) nil))
            (str "case " i ": nil never appears in the new marginal"))
        (is (some? (get-in new [:selection-law :excluded-non-actions]))
            (str "case " i ": exclusions are recorded"))))))
