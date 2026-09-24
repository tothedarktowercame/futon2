(ns checks.proof2-numbers
  "PROOF-2 packet NUM-R (strategy row 5): exact rational transcription of
   certificate value forms, per proof2/packets/SPEC-N.md section 3 (634877af)
   and CERT-S sections 1/3.

   NOTE on the packet text: the dispatch named this namespace
   `futon2.checks.proof2-numbers`, but the strategy row pins the path
   `checks/proof2_numbers.clj`, and Clojure's namespace↔file rule makes the
   only lawful ns at that path `checks.proof2-numbers` (every other checks/
   namespace follows the same convention). Implemented at the pinned path;
   the packet's ns name is a proposed amendment.

   decode-exact is TOTAL over certificate value forms:
   - EDN integer (any precision)      → itself (exact rational), raw :integer
   - EDN ratio                        → itself, but ONLY reduced with positive
                                        denominator (refuse otherwise — never
                                        normalise silently)
   - #wm/double \"<hex>\"              → the exact dyadic rational of the
                                        binary64 value, by SPEC-N's
                                        sign/exponent/fraction rule (normal
                                        and subnormal), raw :double preserving
                                        the sign of zero
   Everything else refuses with a typed reason. In particular: a decimal
   literal (EDN double/float/BigDecimal) is NEVER accepted where an exact
   form is required, and NaN / infinities refuse in the finite decoder.

   The raw identity is what the certificate printed, before any arithmetic:
   +0.0 and -0.0 both decode to rational 0 but keep distinct raw identities.
   This namespace deliberately does NOT reuse action_identity.clj's
   tagged-tree hash: SPEC-N §3 warns it is not the CERT-S codec."
  (:require [clojure.string :as str])
  (:import [java.math BigInteger]))

;; ---------------------------------------------------------------------------
;; Refusals (typed, matching the {:refusal {:kind ...}} convention)
;; ---------------------------------------------------------------------------

(defn- refuse!
  [kind detail]
  (throw (ex-info "NUM-R decode refused"
                  {:refusal {:kind kind :detail detail}})))

;; ---------------------------------------------------------------------------
;; Ratios: reduced form and positive denominator, or refusal
;;
;; The EDN reader canonicalises ratio literals, so a non-canonical Ratio can
;; only arrive constructed (e.g. clojure.lang.Ratio. 4 6, or denominator
;; negative). decode-exact must catch exactly that, because a certificate
;; writer that silently normalised 4/6 into 2/3 has already changed the value
;; the record asserts. SPEC-N §3: require signed integer numerator, positive
;; denominator, reduction to coprime form; 0 has denominator 1.
;; ---------------------------------------------------------------------------

(defn- gcd [^BigInteger a ^BigInteger b]
  (.gcd a b))

(defn- decode-ratio
  [^clojure.lang.Ratio r]
  (let [n (.numerator r)
        d (.denominator r)]
    (when-not (pos? (.signum ^BigInteger d))
      (refuse! :ratio-denominator-not-positive
               {:ratio (str r) :numerator (str n) :denominator (str d)}))
    (if (zero? (.signum ^BigInteger n))
      ;; SPEC-N §3: 0 has denominator 1
      (when-not (= BigInteger/ONE d)
        (refuse! :ratio-not-reduced
                 {:ratio (str r) :numerator (str n) :denominator (str d)}))
      (when-not (= BigInteger/ONE (gcd (.abs ^BigInteger n) d))
        (refuse! :ratio-not-reduced
                 {:ratio (str r) :numerator (str n) :denominator (str d)})))
    {:rational r
     :raw {:form :ratio :printed (str r)}}))

;; ---------------------------------------------------------------------------
;; #wm/double "<hex>": canonical Double/toHexString grammar, binary64
;; representability, SPEC-N §3 decode.
;;
;; Canonical grammar (lowercase, as Double/toHexString prints):
;;   [-] "0x" ("1" | "0") "." hexdigit+ "p" [-] digit+
;; with no trailing zero hex digit in the fraction (except the all-zero
;; fraction itself). Normals print leading 1 with -1022 ≤ e ≤ 1023; subnormals
;; print leading 0 with e = -1022; zeros print "0x0.0p0" / "-0x0.0p0".
;; The exponent text is canonical only as "0", a positive integer without
;; leading zeros, or a negative nonzero integer without leading zeros
;; ("p00", "p-0", "p01" are not toHexString output); the grammar bounds it to
;; six digits so parsing cannot overflow, and the range check below refuses
;; anything outside the printed binary64 range with a typed reason.
;; ---------------------------------------------------------------------------

(def ^:private hex-grammar
  #"^(-?)0x([01])\.([0-9a-f]+)p(0|-?[1-9][0-9]{0,5})$")

(defn- pow2
  "Exact 2^e as a rational for any integer e."
  [e]
  (if (neg? e)
    (/ 1 (.shiftLeft BigInteger/ONE (- e)))
    (.shiftLeft BigInteger/ONE e)))

(defn- decode-hex-double
  [s]
  (when (contains? #{"NaN" "Infinity" "-Infinity"} s)
    (refuse! :double-non-finite {:form s}))
  (let [m (re-matches hex-grammar s)]
    (when-not m
      (refuse! :double-non-canonical-hex {:form s}))
    (let [[_ sign lead frac exp-s] m
          e (Long/parseLong exp-s)
          negative? (= "-" sign)
          ;; canonical toHexString strips trailing fraction zeros but always
          ;; keeps at least one digit
          canonical-frac? (or (= frac "0")
                              (not (str/ends-with? frac "0")))
          _ (when-not canonical-frac?
              (refuse! :double-non-canonical-hex
                      {:form s :reason :trailing-zero-fraction-digit}))
          n (BigInteger. frac 16)
          k (count frac)
          normal-lead? (= "1" lead)
          zero-value? (zero? (.signum n))]
      (if normal-lead?
        ;; normal: significand 1.frac must fit the 53-bit binary64
        ;; significand, with the exponent field in [1, 2046], i.e. the
        ;; printed exponent in [-1022, 1023]
        (let [n'-bits (.bitLength (.setBit n (* 4 k)))]
          (when (or (> n'-bits 53) (< e -1022) (> e 1023))
            (refuse! :double-not-binary64
                    {:form s :significand-bits n'-bits :exponent e
                     :reason (if (> n'-bits 53) :significand-too-wide :exponent-out-of-range)})))
        ;; leading 0: zero (e = 0, N = 0) or subnormal — canonical toHexString
        ;; prints subnormals at e = -1022 with 1..13 fraction hex digits
        ;; (trailing zeros stripped, so "0x0.8p-1022" = 2^-1023 is canonical);
        ;; more than 13 digits would put the fraction M at or above 2^52.
        (if zero-value?
          (when-not (zero? e)
            (refuse! :double-not-binary64
                    {:form s :reason :zero-with-nonzero-exponent}))
          (when (or (not= -1022 e) (> k 13))
            (refuse! :double-not-binary64
                    {:form s :exponent e :fraction-hex-digits k}))))
      (let [;; SPEC-N §3: decode = sign × N' × 2^(e−4k), where N' is the
            ;; significand as an integer (leading digit included) and k the
            ;; count of fractional hex digits. Equivalent to the
            ;; sign/exponent-field/fraction rule (normal and subnormal).
            n' (if normal-lead?
                 (.setBit n (* 4 k))
                 n)
            magnitude (*' n' (pow2 (- e (* 4 k))))
            value (if negative? (-' magnitude) magnitude)]
        {:rational value
         :raw {:form :double :hex s :negative? negative?}}))))

;; ---------------------------------------------------------------------------
;; decode-exact (total)
;; ---------------------------------------------------------------------------

(defn decode-exact
  "Decode one certificate value form to {:rational <exact> :raw <identity>}.
   Total over the CERT-S value forms; everything else is a typed refusal:
   - :decimal-literal — an EDN float/double/BigDecimal presented where an
     exact form is required (a decimal is never an exact input);
   - :ratio-not-reduced / :ratio-denominator-not-positive;
   - :double-non-canonical-hex / :double-not-binary64 / :double-non-finite;
   - :not-a-certificate-value-form — anything else."
  [v]
  (cond
    (integer? v)
    {:rational v
     :raw {:form :integer :printed (str v)}}

    (ratio? v)
    (decode-ratio v)

    (and (instance? clojure.lang.TaggedLiteral v) (= 'wm/double (:tag v)))
    (if (string? (:form v))
      (decode-hex-double (:form v))
      (refuse! :double-non-canonical-hex {:form (:form v)}))

    (or (double? v) (float? v) (decimal? v))
    (refuse! :decimal-literal {:value (str v) :type (str (type v))})

    :else
    (refuse! :not-a-certificate-value-form {:value (pr-str v)})))
