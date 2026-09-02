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

(deftest reads-the-inline-completion-criteria-shape
  (testing "the second markdown shape in the live corpus: a bold paragraph"
    (let [r (mc/read-criteria "holes/missions/M-zaif-harness-v1.md"
                              :observables {} :mission "M-zaif-harness-v1")]
      (is (= :markdown-inline-paragraph (:shape r)))
      (is (= 3 (count (:criteria r)))
          "and it agrees with the hand ingest's count for the same mission"))))

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
  (let [reading {:x 0 :y 0.9}
        c (mc/c-mis {:mission "M-planted"
                     :criteria [(mc/criterion-row {:criterion :a :observable :x} reading "f:1")
                                (mc/criterion-row {:criterion :b :observable :y :spec [0.0 0.3]}
                                                  reading "f:2")]})
        r (mc/risk-mis c reading)
        by-criterion (into {} (map (juxt :criterion identity)) (:per-criterion r))]
    (is (= :measured (:status r)))
    (is (= :status-quo-v0 (:forward-model r)))
    (testing "criterion :a — {:becomes 1} read at 0, the unmet Bernoulli"
      (let [e (:a by-criterion)
            d (pref/c-distribution {:becomes 1})]
        (is (= :x (:observable e)))
        (is (= 0 (:value e)))
        (is (close? (pref/log-preference d 0) (:log-c e)))
        (is (close? (- (pref/log-preference d 0)) (:surprisal e)))
        (is (close? 0.5 (:weight e)))
        (is (close? (* 0.5 (- (pref/log-preference d 0))) (:contribution e)))))
    (testing "criterion :b — a range C read outside its band"
      (let [e (:b by-criterion)
            d (pref/c-distribution [0.0 0.3])]
        (is (close? (pref/log-preference d 0.9) (:log-c e)))
        (is (close? (* 0.5 (- (pref/log-preference d 0.9))) (:contribution e)))))
    (testing "the total is exactly the sum of the recorded contributions"
      (is (close? (reduce + (map :contribution (:per-criterion r))) (:risk r))))
    (testing "and it is minus the weighted log-sum composition"
      (is (close? (- (:log-c (mc/log-c-mis c reading))) (:risk r))))))

(deftest a-met-criterion-stops-pulling
  (testing "design §3's completion gate from first principles: risk falls as
            criteria read satisfied, and the met Bernoulli is the floor"
    (let [c-of (fn [] (mc/c-mis {:criteria [(mc/criterion-row {:criterion :a :observable :x}
                                                              {:x 0} "f:1")]}))
          unmet (:risk (mc/risk-mis (c-of) {:x 0}))
          met (:risk (mc/risk-mis (c-of) {:x 1}))]
      (is (> unmet met))
      (is (close? (- (pref/log-preference (pref/c-distribution {:becomes 1}) 1)) met))
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

(deftest the-shipped-path-still-reads-a-double-zero-as-met
  (testing "U12's defect, pinned as the BASELINE the arms are compared against —
            this is what does not move until a semantics is chosen"
    (let [unmet-long (:risk (mc/risk-mis (one-criterion-c) {:x (long 0)}))
          zero-double (:risk (mc/risk-mis (one-criterion-c) {:x 0.0}))
          met (:risk (mc/risk-mis (one-criterion-c) {:x 1}))]
      (is (> unmet-long 9.0))
      (is (close? met zero-double)
          "the double 0.0 scores as the MET criterion, not the unmet one")
      (is (close? met (:risk (mc/risk-mis (one-criterion-c) {:x nil})))
          "and so does an unread channel")))
  (testing "no arm named ⇒ no :outcome-semantics key on the record either"
    (is (not (contains? (mc/risk-mis (one-criterion-c) {:x 0.0}) :outcome-semantics)))))

(deftest arms-read-the-double-zero-each-in-their-own-way
  (let [risk-at (fn [arm v] (mc/risk-mis (one-criterion-c) {:x v} :outcome-semantics arm))
        long-0-unmet (:risk (mc/risk-mis (one-criterion-c) {:x (long 0)}))]
    (testing ":numeric-equality — the double 0.0 now scores as the long 0 does"
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
