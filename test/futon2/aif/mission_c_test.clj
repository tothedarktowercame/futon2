(ns futon2.aif.mission-c-test
  "U11 acceptance: the criteria reader on both named fixtures, the pinned
   c-distribution reuse, the unmeasurable refusal, flag-off byte-identity, and
   a flag-on record whose every number is traceable."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.mission-c :as mc]
            [futon2.aif.preferences :as pref])
  (:import [java.nio.file Files]
           [java.security MessageDigest]))

(def zaif-ingest
  "Fixture 1 — the hand exemplar DESIGN-c-vector.md §2 names."
  "holes/labs/zaif-harness/runs/S4-identify-ingest.edn")

(def eoi-doc
  "Fixture 2 — M-expressions-of-interest, the mission the 2026-09-02 records
   rank first. Outside this repo, so its absence skips rather than fails."
  "/home/joe/code/futon5a/holes/missions/M-expressions-of-interest.md")

(defn- close? [a b] (< (Math/abs (- (double a) (double b))) 1e-6))

;; ---------------------------------------------------------------------------
;; (a) the criteria reader, on both fixtures

(deftest reads-the-zaif-identify-ingest
  (let [r (mc/read-criteria zaif-ingest :observables {})]
    (is (= :present (:status r)))
    (is (= :ingest-edn (:shape r)))
    (is (= "M-zaif-harness-v1" (:mission r)))
    (is (= [:u-rows-green :reporting-gate-holds :honest-gap-list-published]
           (mapv :criterion (:criteria r)))
        "criterion ids come from the ingest, which names them")
    (testing "every row carries a file:line pointer into the ingest"
      (doseq [c (:criteria r)]
        (is (re-matches #".*S4-identify-ingest\.edn:\d+" (:source c)))))
    (testing "the exemplar declares no :spec, so every row takes the recorded default"
      (is (= [{:becomes 1} {:becomes 1} {:becomes 1}] (mapv :spec (:criteria r))))
      (is (every? #(= :default-becomes-1 (:spec-source %)) (:criteria r))))
    (testing "measurement is declared, in :carrier, and is prose"
      (is (every? #(= :carrier (:measurement-field %)) (:criteria r)))
      (is (every? #(string? (:measurement %)) (:criteria r))))))

(deftest reads-the-expressions-of-interest-doc
  (if-not (.exists (io/file eoi-doc))
    (is true "futon5a not present; fixture 2 skipped")
    (let [r (mc/read-criteria eoi-doc :observables {} :mission "M-expressions-of-interest")]
      (is (= :present (:status r)))
      (is (= :markdown-numbered-list (:shape r)))
      (is (= 6 (count (:criteria r))) "§IDENTIFY 'Completion criteria' is a 6-item list")
      (is (= [:criterion-1 :criterion-2 :criterion-3
              :criterion-4 :criterion-5 :criterion-6]
             (mapv :criterion (:criteria r)))
          "positional ids: the doc does not name its criteria and the reader must not")
      (is (str/starts-with? (:statement (first (:criteria r))) "Sorry-EOI-PRIOR closed"))
      (is (str/includes? (:statement (last (:criteria r))) "previously-unnamed basin"))
      (testing "distinct file:line pointers, one per item"
        (let [srcs (mapv :source (:criteria r))]
          (is (= 6 (count (distinct srcs))))
          (is (every? #(re-matches #".*M-expressions-of-interest\.md:\d+" %) srcs)))))))

(deftest reads-both-markdown-shapes-and-prefers-the-section
  ;; This fixture pinned :markdown-inline-paragraph until 2026-09-02: the only
  ;; completion criteria M-zaif-harness-v1.md had were the bold paragraph at
  ;; :72. futon2 161ac09 added the `## Completion criteria` SECTION J6's ruling
  ;; asked for, and the reader tries shape 1 first, so the section is what
  ;; answers now. The paragraph is still in the file; the shapes are pinned
  ;; apart rather than one of them being dropped.
  (testing "shape 1 on the live doc, and it is the ported section that answers"
    (let [r (mc/read-criteria "holes/missions/M-zaif-harness-v1.md"
                              :observables {} :mission "M-zaif-harness-v1")]
      (is (= :markdown-numbered-list (:shape r)))
      (is (= 3 (count (:criteria r)))
          "and it agrees with the hand ingest's count for the same mission")
      (is (every? #(< 76 (Long/parseLong (second (str/split (:source %) #":"))))
                  (:criteria r))
          "the pointers are into the section, not the paragraph above it")))
  (testing "shape 2 on text, because the live corpus no longer has an instance
            of it — a shape the reader supports and nothing exercises would be
            a claim with no witness"
    (let [r (mc/criteria-from-markdown "**Completion criteria:** a; b; c.\n"
                                       {:observables {} :path "fixture.md"
                                        :mission "M-fixture"})]
      (is (= :markdown-inline-paragraph (:shape r)))
      (is (= ["a" "b" "c"] (mapv :statement (:criteria r)))))))

(deftest missing-and-absent-sources-are-typed-not-empty
  (testing "a doc with no completion-criteria section"
    (let [r (mc/read-criteria "holes/missions/M-wm-aif-policy-grain-compliance.md"
                              :observables {} :mission "M-x")]
      (is (= :absent (:status r)))
      (is (= :no-completion-criteria-section (:reason r)))
      (is (= [] (:criteria r)))))
  (testing "a path that is not there"
    (let [r (mc/read-criteria "holes/missions/M-does-not-exist.md" :observables {})]
      (is (= :absent (:status r)))
      (is (= :source-not-found (:reason r)))))
  (testing "an ingest with no :preferences/c key"
    (let [r (mc/criteria-from-ingest {:ingest/mission "M-y"}
                                     {:observables {} :path "x.edn" :text ""})]
      (is (= :absent (:status r)))
      (is (= :no-preferences-c-key (:reason r))))))

;; ---------------------------------------------------------------------------
;; U15 -- the criteria source is hashed, so a replay can tell content apart
;; from a path. U12 clause (a) passed determinism only "as stated": the
;; readback reproduces from its arguments, but the criteria arrive from a
;; mutable file named by a path, and an edit to that file moves every number
;; with nothing on the record to show it moved.

(defn- expected-sha256
  "The digest computed OUTSIDE the code under test, so the test does not check
   read-criteria against itself."
  [path]
  (apply str (map #(format "%02x" (bit-and 0xff %))
                  (.digest (MessageDigest/getInstance "SHA-256")
                           (Files/readAllBytes (.toPath (io/file path)))))))

(deftest source-sha256-is-the-digest-of-the-bytes-read
  (testing "an ingest read: the field matches an independently computed digest"
    (let [r (mc/read-criteria zaif-ingest :observables {})]
      (is (= (expected-sha256 zaif-ingest) (:source-sha256 r)))))
  (testing "and a markdown read, including one whose PARSE is a typed absence:
            the digest records what was read even when no criteria came out"
    (let [doc "holes/missions/M-wm-aif-policy-grain-compliance.md"
          r (mc/read-criteria doc :observables {} :mission "M-x")]
      (is (= :absent (:status r)))
      (is (= :no-completion-criteria-section (:reason r)))
      (is (= (expected-sha256 doc) (:source-sha256 r)))))
  (testing "a path with no bytes behind it carries NO digest -- typed absence
            rather than a nil sitting where a hash goes"
    (let [r (mc/read-criteria "holes/missions/M-does-not-exist.md" :observables {})]
      (is (= :source-not-found (:reason r)))
      (is (not (contains? r :source-sha256))))))

(deftest source-sha256-changes-when-the-criteria-file-changes
  (testing "the property the field exists for: edit the source and the digest
            differs, so a replay can refuse instead of returning new numbers
            under the same path"
    (let [dir (str (System/getProperty "java.io.tmpdir") "/u15-" (System/nanoTime))
          _ (.mkdirs (io/file dir))
          path (str dir "/S4-identify-ingest.edn")
          ingest (fn [observable]
                   (pr-str {:ingest/mission "M-planted"
                            :preferences/c
                            [{:criterion :sorries-cleared :observable observable}]}))
          _ (spit path (ingest :sorry-count-norm))
          before (mc/read-criteria path :observables {:sorry-count-norm 0.0})
          _ (spit path (ingest :mission-health))
          after (mc/read-criteria path :observables {:mission-health 0.0})]
      (is (= (expected-sha256 path) (:source-sha256 after)))
      (is (not= (:source-sha256 before) (:source-sha256 after))
          "same path, different bytes, different digest")
      (testing "and re-reading unchanged bytes reproduces the digest"
        (is (= (:source-sha256 after)
               (:source-sha256 (mc/read-criteria path :observables {:mission-health 0.0})))))
      (io/delete-file path true)
      (io/delete-file dir true))))

;; ---------------------------------------------------------------------------
;; (b) C_mis is built by the SAME constructor, not a copy

(deftest c-distribution-is-the-pinned-constructor
  (testing "each factor is identical to calling pref/c-distribution directly"
    (let [read-result {:mission "M-planted"
                       :criteria [(mc/criterion-row {:criterion :a :observable :sorry-count-norm}
                                                    {:sorry-count-norm 0.0} "fixture:1")
                                  (mc/criterion-row {:criterion :b :observable :mission-health
                                                     :spec [0.5 1.0]}
                                                    {:mission-health 0.0} "fixture:2")]}
          c (mc/c-mis read-result)]
      (is (= 2 (:measurable-count c)))
      (is (= (pref/c-distribution {:becomes 1} :temperature pref/default-c-temperature)
             (get-in c [:factors :sorry-count-norm])))
      (is (= (pref/c-distribution [0.5 1.0] :temperature pref/default-c-temperature)
             (get-in c [:factors :mission-health])))))
  (testing "a declared temperature reaches the constructor"
    (let [c (mc/c-mis {:criteria [(mc/criterion-row {:criterion :a :observable :x}
                                                    {:x 0.0} "fixture:1")]}
                      :temperature 0.5)]
      (is (= (pref/c-distribution {:becomes 1} :temperature 0.5)
             (get-in c [:factors :x]))))))

(deftest uniform-weights-are-over-the-measurable-criteria
  (let [c (mc/c-mis {:criteria [(mc/criterion-row {:criterion :a :observable :x} {:x 0} "f:1")
                                (mc/criterion-row {:criterion :b :observable :y} {:x 0} "f:2")
                                (mc/criterion-row {:criterion :c :statement "prose"} {} "f:3")]})]
    (is (= :uniform-over-measurable (:weight-basis c)))
    (is (= 3 (:criterion-count c)))
    (is (= 1 (:measurable-count c))
        "only :a resolves — :b names :y which is not in ITS supplied vocabulary")
    (is (close? 1.0 (reduce + (vals (:weights c))))
        "weights sum to 1 over the measurable set; an unmeasurable criterion
         contributes nothing, including nothing to the denominator")))

(deftest declared-weights-must-cover-every-measurable-criterion
  (let [rows [(mc/criterion-row {:criterion :a :observable :x} {:x 0 :y 0} "f:1")
              (mc/criterion-row {:criterion :b :observable :y} {:x 0 :y 0} "f:2")]]
    (is (= {:a 0.75 :b 0.25}
           (:weights (mc/c-mis {:criteria rows} :criterion-weights {:a 0.75 :b 0.25}))))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"covers no weight"
                          (mc/c-mis {:criteria rows} :criterion-weights {:a 1.0})))))

;; ---------------------------------------------------------------------------
;; (c) the unmeasurable refusal

(deftest unmeasurable-criteria-are-typed-on-every-read
  (testing "the two failures are distinguished, because they want different repairs"
    (let [c (mc/c-mis (mc/read-criteria zaif-ingest :observables {}))]
      (is (= 0 (:measurable-count c)))
      (is (= {} (:factors c)))
      (is (= 3 (count (:unmeasurable c))))
      (is (every? #(= :unmeasurable (:status %)) (:unmeasurable c)))
      (is (= #{:unresolved-observable} (set (map :reason (:unmeasurable c))))
          "the exemplar's criteria DO say how they are measured — in prose")
      (is (every? :source (:unmeasurable c)) "each carries its file:line")))
  (testing "prose with no measurement field at all is the other reason"
    (let [row (mc/criterion-row {:criterion :k :statement "U-rows green"} {:x 0} "f:9")]
      (is (= :unmeasurable (:status row)))
      (is (= :no-declared-measurement (:reason row)))))
  (testing "a keyword observable outside the supplied vocabulary is a third"
    (let [row (mc/criterion-row {:criterion :k :observable :not-a-channel} {:x 0} "f:9")]
      (is (= :unmeasurable (:status row)))
      (is (= :undeclared-observable (:reason row)))))
  (testing "risk refuses rather than returning a flat zero"
    (let [c (mc/c-mis (mc/read-criteria zaif-ingest :observables {}))
          r (mc/risk-mis c {:sorry-count-norm 0.0})]
      (is (= :absent (:status r)))
      (is (= :no-measurable-criteria (:reason r)))
      (is (not (contains? r :risk))
          "no :risk key — a 0.0 here would read as 'this mission's criteria are met'")
      (is (= 3 (count (:unmeasurable r))) "the typed records ride on the risk read too"))))

(deftest an-unreadable-observable-refuses-the-whole-number
  (let [c (mc/c-mis {:criteria [(mc/criterion-row {:criterion :a :observable :x} {:x 0 :y 0} "f:1")
                                (mc/criterion-row {:criterion :b :observable :y} {:x 0 :y 0} "f:2")]})
        r (mc/risk-mis c {:x 1})]
    (is (= :absent (:status r)))
    (is (= :unreadable-observable (:reason r)))
    (is (= [:y] (:missing r)))
    (is (not (contains? r :risk)) "no partial sum standing in for the mission's risk")))

;; ---------------------------------------------------------------------------
;; risk_mis: every number traceable

(deftest surprisal-is-the-point-mass-kl
  (testing "for a Bernoulli C the v0 term IS pref/kl on a point mass — the
            equality that lets risk-mis reuse log-preference and still claim KL"
    (let [d (pref/c-distribution {:becomes 1})]
      (doseq [x [0 1]]
        (let [surprisal (- (pref/log-preference d x))
              kl (pref/kl {:kind :bernoulli :p (if (= 1 x) 1.0 0.0)} d)]
          (is (< (Math/abs (- surprisal kl)) 1e-7)
              (str "outcome " x ": surprisal " surprisal " vs pref/kl " kl)))))))

(deftest risk-mis-numbers-are-reproducible-from-the-record
  (let [reading {:x 0.0 :y 0.9}
        c (mc/c-mis {:mission "M-planted"
                     :criteria [(mc/criterion-row {:criterion :a :observable :x} reading "f:1")
                                (mc/criterion-row {:criterion :b :observable :y :spec [0.0 0.3]}
                                                  reading "f:2")]})
        r (mc/risk-mis c reading)
        by-criterion (into {} (map (juxt :criterion identity)) (:per-criterion r))]
    (is (= :measured (:status r)))
    (is (= :status-quo-v0 (:forward-model r)))
    (testing "criterion :a — {:becomes 1} read at the DOUBLE 0.0 R2 emits, the
              unmet Bernoulli. Under J6's ruled arm 0.0 is exactly binary, so it
              reaches the unsatisfied pole; before the ruling it scored as met"
      (let [e (:a by-criterion)
            d (pref/c-distribution {:becomes 1})
            log-c (:log-c (pref/log-preference-under d 0.0 mc/default-outcome-semantics))]
        (is (= :x (:observable e)))
        (is (= 0.0 (:value e)))
        (is (= 0 (:outcome e)) "the record says which pole")
        (is (close? log-c (:log-c e)))
        (is (close? (- log-c) (:surprisal e)))
        (is (close? 0.5 (:weight e)))
        (is (close? (* 0.5 (- log-c)) (:contribution e)))))
    (testing "criterion :b — a range C read outside its band. U17: the recorded
              contribution is the DIVERGENCE, w · gap/T, not w · the unshifted
              cross-entropy, which for this band carries ln Z = -0.9165"
      (let [e (:b by-criterion)
            d (pref/c-distribution [0.0 0.3])]
        (is (close? (pref/log-preference d 0.9) (:log-c e)))
        (is (close? (- (pref/log-preference d 0.9)) (:surprisal e)))
        (is (close? (Math/log 0.39990881180344456) (:shift e)))
        (is (close? (/ (- 0.9 0.3) pref/default-c-temperature) (:divergence e))
            "gap/T = 0.6/0.1 = 6.0")
        (is (close? (* 0.5 (pref/point-mass-divergence d 0.9)) (:contribution e)))))
    (testing "the total is exactly the sum of the recorded contributions"
      (is (close? (reduce + (map :contribution (:per-criterion r))) (:risk r))))
    (testing "and the pre-U17 number is still on the record, minus the weighted
              log-sum composition, with the shift between the two named"
      (is (close? (- (:log-c (mc/log-c-mis c reading))) (:cross-entropy r)))
      (is (close? (mc/divergence-shift c) (:divergence-shift r)))
      (is (close? (- (:cross-entropy r) (:divergence-shift r)) (:risk r))))))

(deftest a-met-criterion-stops-pulling
  (testing "design §3's completion gate from first principles: risk falls as
            criteria read satisfied, and the met Bernoulli is the floor"
    (let [c-of (fn [] (mc/c-mis {:criteria [(mc/criterion-row {:criterion :a :observable :x}
                                                              {:x 0} "f:1")]}))
          unmet (:risk (mc/risk-mis (c-of) {:x 0.0}))
          met (:risk (mc/risk-mis (c-of) {:x 1.0}))]
      (is (> unmet met))
      (is (close? (- (pref/log-preference (pref/c-distribution {:becomes 1}) 1.0)) met))
      (is (< met 1.0e-4) "T=0.1 puts ~all the mass on the target, so met ≈ 0"))))

;; ---------------------------------------------------------------------------
;; U16 — the three outcome-semantics arms AT THE COMPOSITION, with the DOUBLE
;; zero and nil that the long-0 pins above cannot reach. `a-met-criterion-stops-
;; pulling` and `risk-mis-numbers-are-reproducible-from-the-record` both read
;; their Bernoulli criterion at the LONG 0, which is the one value R2's
;; all-doubles observation vector never produces (U12 :clause-c). These add the
;; vocabulary the live path actually speaks.

(defn- one-criterion-c []
  (mc/c-mis {:criteria [(mc/criterion-row {:criterion :a :observable :x} {:x 0} "f:1")]}))

(def ^:private unmet-bernoulli
  "-ln C(0) for the default {:becomes 1} C at the default temperature, computed
   from `pref` directly so the composition tests below do not check `mission-c`
   against itself."
  (- (pref/log-preference (pref/c-distribution {:becomes 1}) (long 0))))

(deftest the-ruled-arm-is-the-default-and-an-explicit-nil-is-the-old-path
  ;; U18 (J6). What this replaces: until the ruling this fixture pinned the
  ;; DEFECT as the shipped baseline -- the double 0.0 and an unread nil both
  ;; scoring as the MET criterion, and the long 0 the only value that could
  ;; reach the unsatisfied pole. The long-0 pin is gone with it: R2's
  ;; observation vector is doubles throughout, so it pinned a vocabulary the
  ;; live path cannot speak.
  (testing "omitting :outcome-semantics takes J6's ruled arm, and the double 0.0
            R2 actually emits now reaches the unsatisfied pole"
    (let [r (mc/risk-mis (one-criterion-c) {:x 0.0})]
      (is (= :measured (:status r)))
      (is (= :declared-binarization (:outcome-semantics r))
          "the record names the reading that produced the number")
      (is (close? unmet-bernoulli (:risk r)))
      (is (> (:risk r) 9.0))))
  (testing "and an unread channel is a typed absence rather than a met criterion"
    (let [r (mc/risk-mis (one-criterion-c) {:x nil})]
      (is (= :absent (:status r)))
      (is (= :unread-outcome (:reason r)))
      (is (not (contains? r :risk)))))
  (testing "an EXPLICIT nil still names the pre-ruling raw pref/log-preference
            path -- kept reachable so U16's :v0-shipped comparison column
            (u16_outcome_semantics.clj:101) still reproduces what it measured"
    (let [r (mc/risk-mis (one-criterion-c) {:x 0.0} :outcome-semantics nil)]
      (is (= :measured (:status r)))
      (is (not (contains? r :outcome-semantics)))
      (is (close? 4.539889921682063E-5 (:risk r))
          "U12's satisfied floor: the defect, on the value that triggers it")
      (is (close? (:risk r) (:risk (mc/risk-mis (one-criterion-c) {:x nil}
                                                :outcome-semantics nil)))
          "including nil reading as the target"))))

(deftest arms-read-the-double-zero-each-in-their-own-way
  (let [risk-at (fn [arm v] (mc/risk-mis (one-criterion-c) {:x v} :outcome-semantics arm))
        long-0-unmet unmet-bernoulli]
    (testing ":numeric-equality — the double 0.0 scores as the long 0 does"
      (let [r (risk-at :numeric-equality 0.0)]
        (is (= :measured (:status r)))
        (is (close? long-0-unmet (:risk r)))
        (is (= :numeric-equality (:outcome-semantics r)))
        (is (= 0 (:outcome (first (:per-criterion r))))
            "the record says WHICH pole, not just the number")))
    (testing ":declared-binarization — 0.0 is exactly binary, so it reads with no threshold"
      (let [r (risk-at :declared-binarization 0.0)]
        (is (= :measured (:status r)))
        (is (close? long-0-unmet (:risk r)))))
    (testing ":typed-binary-only — refuses, because nothing declared :x binary"
      (let [r (risk-at :typed-binary-only 0.0)]
        (is (= :absent (:status r)))
        (is (= :unread-outcome (:reason r)))
        (is (not (contains? r :risk)))
        (is (= [:undeclared-observable-kind] (mapv :reason (:refused r))))
        (is (= [:x] (mapv :observable (:refused r))))))))

(deftest arms-refuse-nil-rather-than-scoring-it-satisfied
  (testing "C130 at the composition: every arm, no number, the reason typed"
    (doseq [arm pref/bernoulli-outcome-arms]
      (let [r (mc/risk-mis (one-criterion-c) {:x nil} :outcome-semantics arm)]
        (is (= :absent (:status r)) (str arm))
        (is (= :unread-outcome (:reason r)) (str arm))
        (is (not (contains? r :risk)) (str arm))
        (is (= [:no-outcome-observed] (mapv :reason (:refused r))) (str arm))))))

(deftest arms-part-company-on-a-continuous-reading
  (testing "0.75 — the case that separates the three, since 0.0 does not"
    (let [r1 (mc/risk-mis (one-criterion-c) {:x 0.75} :outcome-semantics :numeric-equality)
          r2 (mc/risk-mis (one-criterion-c) {:x 0.75} :outcome-semantics :declared-binarization)
          r3 (mc/risk-mis (one-criterion-c) {:x 0.75} :outcome-semantics :typed-binary-only)]
      (is (= :measured (:status r1)))
      (is (< (:risk r1) 1.0e-4) "numeric-equality calls 0.75 met, at the temperature floor")
      (is (= [:absent :no-declared-threshold] [(:status r2) (:reason (first (:refused r2)))]))
      (is (= [:absent :undeclared-observable-kind] [(:status r3) (:reason (first (:refused r3)))]))))
  (testing "and a declared threshold turns the same value into an UNMET criterion"
    (let [r (mc/risk-mis (one-criterion-c) {:x 0.75}
                         :outcome-semantics {:arm :declared-binarization :threshold 0.8})]
      (is (= :measured (:status r)))
      (is (> (:risk r) 9.0)))))

(deftest one-refused-criterion-refuses-the-whole-number
  (testing "the same discipline :unreadable-observable already applies — a partial
            sum over the criteria an arm happened to accept is not this mission's risk"
    (let [reading {:x 0.0 :y 0.75}
          c (mc/c-mis {:criteria [(mc/criterion-row {:criterion :a :observable :x} reading "f:1")
                                  (mc/criterion-row {:criterion :b :observable :y} reading "f:2")]})
          r (mc/risk-mis c reading :outcome-semantics :declared-binarization)]
      (is (= :absent (:status r)))
      (is (= :unread-outcome (:reason r)))
      (is (= [:y] (mapv :observable (:refused r)))
          ":x was readable; the refusal names only the criterion that refused")
      (is (not (contains? r :risk))))))

(deftest naming-an-arm-does-not-move-the-range-branch
  (testing "U12's finding is about the Bernoulli branch; a range spec is
            arithmetically identical under every arm"
    (let [reading {:y 0.9}
          c (fn [] (mc/c-mis {:criteria [(mc/criterion-row {:criterion :b :observable :y
                                                            :spec [0.0 0.3]} reading "f:2")]}))
          base (:risk (mc/risk-mis (c) reading))]
      (doseq [arm pref/bernoulli-outcome-arms]
        (is (close? base (:risk (mc/risk-mis (c) reading :outcome-semantics arm))) (str arm))))))

;; ---------------------------------------------------------------------------
;; U17 — risk_mis >= 0 on every spec shape, at the COMPOSITION. The density-level
;; tests are in preferences_cdist_test; these are about what `risk-mis` returns,
;; which is what a G would sum.

(defn- risk-of
  "risk_mis for a single criterion with `spec`, read at `value`, under `arm`
   (nil = the pre-ruling raw path). `:absent` when the arm refuses to read it."
  [spec value arm & {:keys [temperature] :or {temperature pref/default-c-temperature}}]
  (let [c (mc/c-mis {:criteria [(mc/criterion-row {:criterion :a :observable :x :spec spec}
                                                  {:x value} "f:1")]}
                    :temperature temperature)]
    ;; nil is passed EXPLICITLY, which names the pre-ruling raw path rather than
    ;; J6's default arm -- so this sweep still covers the reading U17 measured
    ;; and the ruled arm is covered as a named member of `u17-arms`.
    (mc/risk-mis c {:x value} :outcome-semantics arm)))

(def ^:private u17-specs
  [[0.5 1.0] [0.0 0.3] [0.0 1.0] [0.25 0.75] [0.2 0.2]
   {:becomes 1} {:becomes 0} {:p1 0.9} {:p1 0.5}])

(def ^:private u17-values [0 1 0.0 1.0 0.25 0.5 0.6 0.75 0.999])

(def ^:private u17-arms (cons nil (sort pref/bernoulli-outcome-arms)))

(deftest risk-mis-is-nonnegative-on-every-spec-shape
  (testing "U17's acceptance: over range and Bernoulli specs, every value
            INCLUDING in-band ones, three temperatures and all four readings of
            an outcome (the raw path, plus each U16 arm — one of which is J6's
            ruled default), a measured risk_mis is never
            below zero — so summing it into one G beside C_int's KL >= 0 cannot
            pay a satisfied criterion a bonus"
    (doseq [spec u17-specs
            t [0.05 pref/default-c-temperature 0.5]
            value u17-values
            arm u17-arms]
      (let [r (risk-of spec value arm :temperature t)]
        (when (= :measured (:status r))
          (is (<= 0.0 (:risk r))
              (str "spec " spec " T " t " value " value " arm " arm
                   " -> risk " (:risk r)))
          (is (every? #(<= 0.0 (:divergence %)) (:per-criterion r))
              "and no single criterion contributes a negative term either")
          (is (close? (reduce + (map :contribution (:per-criterion r))) (:risk r))
              "the recorded contributions still sum to the recorded risk"))))))

(deftest a-satisfied-range-criterion-no-longer-scores-below-zero
  (testing "the hazard U17 names, measured at the composition. Before the shift
            a criterion read INSIDE its declared band scored ln Z = -0.5119"
    (let [r (risk-of [0.5 1.0] 0.75 nil)]
      (is (= :measured (:status r)))
      (is (close? -0.5119492459595545 (:cross-entropy r))
          "the pre-U17 number, kept on the record rather than overwritten")
      (is (close? 0.0 (:risk r)) "and the divergence it becomes")))
  (testing "outside the band the gradient survives, which is what separates the
            excess form from a clamp: 0.45 misses [0.5 1.0] by 0.05, which is
            less than the -T ln Z = 0.0512 a clamp would swallow"
    (let [r (risk-of [0.5 1.0] 0.45 nil)]
      (is (close? 0.5 (:risk r)) "gap/T = 0.05/0.1")
      (is (neg? (:cross-entropy r))
          "the unshifted form is still negative here — a clamp would read 0")))
  (testing "and a wider band, whose Z >= 1, is untouched by the repair"
    (let [r (risk-of [0.0 1.0] 0.5 nil)]
      (is (close? 0.0 (:divergence-shift r)))
      (is (close? (:cross-entropy r) (:risk r))))))

(deftest satisfied-never-costs-more-than-unsatisfied-on-the-same-channel
  ;; U17's third limb reads "no satisfied criterion scores below an unsatisfied
  ;; one on the same channel". risk_mis is a COST, so that wording is tested two
  ;; ways rather than one reading being assumed: as an ordering (a satisfied
  ;; reading never costs MORE than an unsatisfied one -- this test), and as a
  ;; floor (no reading, satisfied or not, goes below the zero unsatisfied ones
  ;; respect -- `risk-mis-is-nonnegative-on-every-spec-shape`).
  (testing "for a range spec every in-band value must cost at or below every
            out-of-band value on the same channel"
    (doseq [[spec in-band out-of-band] [[[0.5 1.0] [0.5 0.75 1.0] [0.0 0.25 0.45 0.49]]
                                        [[0.0 0.3] [0.0 0.25 0.3] [0.5 0.75 1.0]]
                                        [[0.25 0.75] [0.25 0.5 0.75] [0.0 0.1 0.9 1.0]]]]
      (doseq [sat in-band unsat out-of-band]
        (let [rs (:risk (risk-of spec sat nil))
              ru (:risk (risk-of spec unsat nil))]
          (is (<= rs ru) (str "spec " spec " satisfied " sat " (" rs
                              ") vs unsatisfied " unsat " (" ru ")"))))))
  (testing "and for a Bernoulli spec, under every arm that reads both poles"
    (doseq [[spec sat unsat] [[{:becomes 1} 1 0] [{:becomes 0} 0 1]
                              [{:p1 0.9} 1 0] [{:p1 0.1} 0 1]]
            arm u17-arms]
      (let [rs (risk-of spec sat arm)
            ru (risk-of spec unsat arm)]
        (when (and (= :measured (:status rs)) (= :measured (:status ru)))
          (is (<= (:risk rs) (:risk ru))
              (str "spec " spec " arm " arm)))))))

(deftest declared-weights-that-could-make-the-sum-negative-are-refused
  (testing "risk_mis >= 0 is a weighted sum of nonnegative terms, so it needs the
            weights nonnegative — a negative one would break the claim without
            touching a spec shape"
    (let [rows [(mc/criterion-row {:criterion :a :observable :x} {:x 0 :y 0} "f:1")
                (mc/criterion-row {:criterion :b :observable :y} {:x 0 :y 0} "f:2")]]
      (doseq [w [-0.5 ##NaN ##Inf]]
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"finite and non-negative"
                              (mc/c-mis {:criteria rows}
                                        :criterion-weights {:a w :b 0.5}))
            (str "weight " w))))))

(deftest the-shift-is-the-same-under-every-arm
  (testing "why U17 does not wait on J6: the constant depends on the SPEC KIND
            and never on which outcome an arm reads"
    (doseq [spec u17-specs]
      (let [c (mc/c-mis {:criteria [(mc/criterion-row {:criterion :a :observable :x :spec spec}
                                                      {:x 0.5} "f:1")]})
            shift (mc/divergence-shift c)]
        (doseq [arm u17-arms]
          (let [r (if arm (mc/risk-mis c {:x 0.5} :outcome-semantics arm)
                      (mc/risk-mis c {:x 0.5}))]
            (when (= :measured (:status r))
              (is (close? shift (:divergence-shift r)) (str "spec " spec " arm " arm)))))))))

;; ---------------------------------------------------------------------------
;; U18 — the declaration on the criterion's own :spec, and DECLARED GAUGES: the
;; binding of a criterion written in prose to an observable this machine can
;; read. What makes these gauges rather than plants is that each row says where
;; its binding came from, so a supplied one can never read as the mission
;; document's own declaration.

(deftest a-spec-declared-binary-criterion-is-read-with-no-threshold
  (testing "J6's ruling reaching the composition: the declaration rides from the
            criterion's :spec through c-distribution to the arm"
    (let [c (mc/c-mis {:criteria [(mc/criterion-row
                                   {:criterion :a :observable :x
                                    :spec {:becomes 1 :observable-kind :binary}}
                                   {:x 0.0} "f:1")]})]
      (is (= :binary (get-in c [:factors :x :observable-kind]))
          "the factor map carries what the criterion declared")
      (is (= :measured (:status (mc/risk-mis c {:x 0.0}))))
      (is (close? unmet-bernoulli (:risk (mc/risk-mis c {:x 0.0}))))
      (testing "and a reading that contradicts the declaration refuses, naming
                the declaration rather than a missing threshold"
        (let [r (mc/risk-mis c {:x 0.75})]
          (is (= :absent (:status r)))
          (is (= [:non-binary-value-on-binary-observable] (mapv :reason (:refused r)))))))))

(def ^:private fixture-gauges
  {:criterion-1 {:observable :worklist-acceptance-state
                 :spec {:becomes 1 :observable-kind :binary}
                 :gauge "the worklist rows' acceptance state"
                 :declared-in "fixture-gauges"}})

(deftest a-declared-gauge-binds-a-prose-criterion-and-says-so
  (testing "without the gauge the criterion is unmeasurable, and the reason is
            that it names a measurement only a human can take"
    (let [row (mc/criterion-row {:criterion :criterion-1 :statement "U-rows green"
                                 :carrier "the worklists"}
                                {:worklist-acceptance-state 1.0} "f:1")]
      (is (= :unmeasurable (:status row)))
      (is (= :unresolved-observable (:reason row)))))
  (testing "with it the criterion resolves, and every field says where it came from"
    (let [row (mc/criterion-row (mc/apply-gauge {:criterion :criterion-1
                                                 :statement "U-rows green"}
                                                (:criterion-1 fixture-gauges))
                                {:worklist-acceptance-state 1.0} "f:1")]
      (is (= :measurable (:status row)))
      (is (= :worklist-acceptance-state (:observable row)))
      (is (= :declared-gauge (:observable-source row)))
      (is (= :declared-gauge (:spec-source row))
          "and the spec came from the gauge too, not from the document")
      (is (= "fixture-gauges" (:gauge-source row)) "the pointer back to the declaration")
      (is (string? (:gauge row)) "and the prose saying what is being read")))
  (testing "a gauge whose observable nothing supplies is a typed absence naming a
            MISSING PRODUCER, which is a different repair from prose-only"
    (let [row (mc/criterion-row (mc/apply-gauge {:criterion :criterion-1}
                                                (:criterion-1 fixture-gauges))
                                {} "f:1")]
      (is (= :unmeasurable (:status row)))
      (is (= :undeclared-observable (:reason row))))))

(deftest a-gauge-cannot-overwrite-what-the-document-declared
  (let [entry {:criterion :criterion-1 :observable :its-own-channel :spec [0.5 1.0]}
        row (mc/criterion-row (mc/apply-gauge entry (:criterion-1 fixture-gauges))
                              {:its-own-channel 0.6} "f:1")]
    (is (= :its-own-channel (:observable row)))
    (is (= [0.5 1.0] (:spec row)))
    (is (= :declared (:spec-source row)))
    (is (not (contains? row :observable-source))
        "nothing came from the gauge, so nothing claims to have")
    (is (not (contains? row :gauge-source)))))

(deftest gauges-reach-the-reader-and-make-the-zaif-criteria-measurable
  (testing "J6 (d): the three criteria of M-zaif-harness-v1, read from the doc
            with the declared gauges supplied, are 3/3 measurable"
    (let [observation {:worklist-acceptance-state 0.0
                       :reporting-gate-test-result 0.0
                       :registry-gap-list-present 1.0}
          gauges (into {} (map (fn [[i o]]
                                 [(keyword (str "criterion-" i))
                                  {:observable o :spec {:becomes 1 :observable-kind :binary}
                                   :declared-in "mission_c_test/gauges"}]))
                       [[1 :worklist-acceptance-state]
                        [2 :reporting-gate-test-result]
                        [3 :registry-gap-list-present]])
          r (mc/read-criteria "holes/missions/M-zaif-harness-v1.md"
                              :observables observation
                              :mission "M-zaif-harness-v1"
                              :gauges gauges)
          c (mc/c-mis r)]
      (is (= 3 (:criterion-count c)))
      (is (= 3 (:measurable-count c)))
      (is (= [] (:unmeasurable c)))
      (testing "and it scores: two criteria unmet, one met, under the ruled arm"
        (let [risk (mc/risk-mis c observation)]
          (is (= :measured (:status risk)))
          (is (= :declared-binarization (:outcome-semantics risk)))
          (is (= [0 0 1] (mapv :outcome (:per-criterion risk))))
          (is (close? (+ (* (/ 2.0 3.0) unmet-bernoulli)
                         (/ (- (pref/log-preference (pref/c-distribution {:becomes 1}) 1)) 3.0))
                      (:risk risk))
              "two unmet poles at -ln C(0) and the met one at its temperature floor")))))
  (testing "supplying no gauges leaves the same doc exactly as it was written —
            0/3, prose only"
    (let [c (mc/c-mis (mc/read-criteria "holes/missions/M-zaif-harness-v1.md"
                                        :observables {} :mission "M-zaif-harness-v1"))]
      (is (= 0 (:measurable-count c)))
      (is (= #{:no-declared-measurement} (set (map :reason (:unmeasurable c))))))))
