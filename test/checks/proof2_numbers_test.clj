(ns checks.proof2-numbers-test
  "Acceptance tests for checks.proof2-numbers (PROOF-2 packet NUM-R).
   Each negative test is a real bad case that would pass (or pass silently
   mis-valued) without the named check; each says so in a comment."
  (:require [checks.proof2-numbers :as num]
            [clojure.test :refer [deftest is testing]]))

(defn- refusal-kind [thunk]
  (try
    (thunk)
    (catch clojure.lang.ExceptionInfo e
      (get-in (ex-data e) [:refusal :kind]))))

(defn- wm-double [s]
  (clojure.lang.TaggedLiteral/create 'wm/double s))

(deftest integers-and-ratios-decode-exactly
  (testing "exact forms pass through with their raw identity"
    (is (= {:rational 42 :raw {:form :integer :printed "42"}}
           (num/decode-exact 42)))
    (is (= {:rational 3/4 :raw {:form :ratio :printed "3/4"}}
           (num/decode-exact 3/4)))
    (is (= {:rational -1/16 :raw {:form :ratio :printed "-1/16"}}
           (num/decode-exact -1/16)))))

(deftest unreduced-ratios-refuse
  (testing "BAD CASE: a ratio that arrives
            NON-reduced — 4/6 with numerator and denominator in the wrong
            (unreduced) form — must refuse, not be normalised silently into
            2/3. Without the coprime check this decodes to a value the record
            never asserted. The EDN reader canonicalises literals, so the bad
            case is constructed, exactly as a buggy certificate writer would
            produce it."
    (is (= :ratio-not-reduced
           (refusal-kind #(num/decode-exact
                           (clojure.lang.Ratio. (BigInteger. "4")
                                                (BigInteger. "6"))))))
    (testing "denominator in the numerator's sign place: 3/-4 must refuse,
              not normalise to -3/4"
      (is (= :ratio-denominator-not-positive
             (refusal-kind #(num/decode-exact
                             (clojure.lang.Ratio. (BigInteger. "3")
                                                  (BigInteger. "-4")))))))
    (testing "zero with denominator ≠ 1 (0/5 constructed) refuses: SPEC-N §3,
              0 has denominator 1"
      (is (= :ratio-not-reduced
             (refusal-kind #(num/decode-exact
                             (clojure.lang.Ratio. (BigInteger. "0")
                                                  (BigInteger. "5")))))))))

(deftest decimal-literals-never-accepted
  (testing "BAD CASE: 0.333 presented where an exact ratio is required. A
            decimal literal is a rounded binary value, not the rational 1/3;
            without this refusal it would be silently consumed as an exact
            input. EDN doubles and BigDecimals refuse (a Float instance takes the same branch; the EDN reader never produces one)."
    (is (= :decimal-literal (refusal-kind #(num/decode-exact 0.333))))
    (is (= :decimal-literal (refusal-kind #(num/decode-exact 1.0))))
    (is (= :decimal-literal (refusal-kind #(num/decode-exact 0.1M))))))

(deftest hex-double-grammar-and-representability
  (testing "BAD CASE: a hex string that names a real number but not a
            binary64 value — 0x1.000000000000001p0 needs 57 significand bits.
            Without the representability check it decodes to a dyadic no
            binary64 equals, and the certificate's 'this double' claim is
            false."
    (is (= :double-not-binary64
           (refusal-kind #(num/decode-exact (wm-double "0x1.000000000000001p0"))))))
  (testing "BAD CASE: Infinity / NaN refuse in the finite decoder (SPEC-N §3:
            an infinite-risk branch is a separately typed value, not an IEEE
            overflow reinterpreted)"
    (is (= :double-non-finite
           (refusal-kind #(num/decode-exact (wm-double "Infinity")))))
    (is (= :double-non-finite
           (refusal-kind #(num/decode-exact (wm-double "-Infinity")))))
    (is (= :double-non-finite
           (refusal-kind #(num/decode-exact (wm-double "NaN"))))))
  (testing "grammar: uppercase, missing exponent marker, and trailing zero
            fraction digits are non-canonical"
    (is (= :double-non-canonical-hex
           (refusal-kind #(num/decode-exact (wm-double "0X1.0p0")))))
    (is (= :double-non-canonical-hex
           (refusal-kind #(num/decode-exact (wm-double "0x1.0")))))
    (is (= :double-non-canonical-hex
           (refusal-kind #(num/decode-exact (wm-double "0x1.40p-1")))))
    (is (= :double-non-canonical-hex
           (refusal-kind #(num/decode-exact (wm-double "1.0p0"))))))
  (testing "subnormal discipline: canonical short subnormals decode exactly
            (toHexString strips trailing zeros, so 0x0.1p-1022 = 2^-1026 and
            0x0.8p-1022 = 2^-1023 are canonical); a subnormal at any exponent
            other than -1022 is refused. Without the 1..13 digit rule the two
            positives are wrongly refused (NUM-R-codex-4 R1)."
    (is (= (/ 1 (.shiftLeft BigInteger/ONE 1026))
           (:rational (num/decode-exact (wm-double "0x0.1p-1022")))))
    (is (= (/ 1 (.shiftLeft BigInteger/ONE 1023))
           (:rational (num/decode-exact (wm-double "0x0.8p-1022")))))
    (is (= :double-not-binary64
           (refusal-kind #(num/decode-exact (wm-double "0x0.0000000000001p-1000"))))))
  (testing "BAD CASE (NUM-R-codex-4 R2): non-canonical exponent spellings
            refuse — leading zeros and negative zero are not toHexString
            output. Without the canonical exponent grammar 0x1.0p00 decodes
            to 1 with a non-canonical raw identity."
    (is (= :double-non-canonical-hex
           (refusal-kind #(num/decode-exact (wm-double "0x1.0p00")))))
    (is (= :double-non-canonical-hex
           (refusal-kind #(num/decode-exact (wm-double "0x1.0p-0")))))
    (is (= :double-non-canonical-hex
           (refusal-kind #(num/decode-exact (wm-double "0x1.0p01"))))))
  (testing "BAD CASE (NUM-R-codex-4 R3): oversized exponents of either sign
            are typed refusals, never an untyped NumberFormatException.
            Without the bounded grammar Long/parseLong throws."
    (is (contains? #{:double-non-canonical-hex :double-not-binary64}
                   (refusal-kind #(num/decode-exact (wm-double "0x1.0p9223372036854775808")))))
    (is (contains? #{:double-non-canonical-hex :double-not-binary64}
                   (refusal-kind #(num/decode-exact (wm-double "0x1.0p-9223372036854775809")))))
    (is (= :double-not-binary64
           (refusal-kind #(num/decode-exact (wm-double "0x1.0p1024")))))
    (is (= :double-not-binary64
           (refusal-kind #(num/decode-exact (wm-double "0x1.0p-1023")))))))

(deftest spec-n-example-decodes-exactly
  (testing "SPEC-N §3's pinned example: 0x1.62e42fefa39efp-1 decodes to
            exactly 6243314768165359/9007199254740992 (pin from SPEC-N, not
            recomputed here)"
    (is (= {:rational 6243314768165359/9007199254740992
            :raw {:form :double :hex "0x1.62e42fefa39efp-1" :negative? false}}
           (num/decode-exact (wm-double "0x1.62e42fefa39efp-1"))))))

(deftest record-1790199409-posterior-sum
  (testing "the two posterior doubles from record 2026-09-23-1790199409 (pinned
            in SPEC-N §1 from the live record, SHA
            7b3c56df…): 0x1.87c9e98b73e5fp-1 and 0x1.e0d859d23068bp-3 sum to
            exactly 1 + 7/36028797018963968, not 1 — the check that a decoded
            recorded vector is NOT silently renormalised to its ideal"
    (let [c1 (:rational (num/decode-exact (wm-double "0x1.87c9e98b73e5fp-1")))
          c2 (:rational (num/decode-exact (wm-double "0x1.e0d859d23068bp-3")))]
      (is (= (+ 1 7/36028797018963968) (+ c1 c2)))
      (is (not= 1 (+ c1 c2))))))

(deftest signed-zero-and-subnormal-decode
  (testing "both zeros decode to rational 0 with DISTINCT raw identities
            (SPEC-N §3: preserve the sign of zero in the raw identity)"
    (let [pos (num/decode-exact (wm-double "0x0.0p0"))
          neg (num/decode-exact (wm-double "-0x0.0p0"))]
      (is (= 0 (:rational pos) (:rational neg)))
      (is (false? (get-in pos [:raw :negative?])))
      (is (true? (get-in neg [:raw :negative?])))
      (is (not= (:raw pos) (:raw neg)))))
  (testing "smallest subnormal: 0x0.0000000000001p-1022 = 2^-1074"
    (is (= (/ 1 (.shiftLeft (BigInteger. "1") 1074))
           (:rational (num/decode-exact (wm-double "0x0.0000000000001p-1022"))))))
  (testing "negative normal: -0x1.0p0 = -1"
    (is (= {:rational -1 :raw {:form :double :hex "-0x1.0p0" :negative? true}}
           (num/decode-exact (wm-double "-0x1.0p0"))))))

(deftest other-forms-refuse
  (testing "keywords, strings, bare maps and nil are not certificate value
            forms"
    (is (= :not-a-certificate-value-form (refusal-kind #(num/decode-exact :x))))
    (is (= :not-a-certificate-value-form (refusal-kind #(num/decode-exact "0.5"))))
    (is (= :not-a-certificate-value-form (refusal-kind #(num/decode-exact {:f 1}))))
    (is (= :not-a-certificate-value-form (refusal-kind #(num/decode-exact nil))))))
