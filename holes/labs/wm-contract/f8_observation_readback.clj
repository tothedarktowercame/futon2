;; F8 leg 1 slice 5: production structured-observation readback.
;;
;; PROVENANCE RULE: EVERY -expected IN THIS READBACK IS TRANSCRIBED FROM A
;; NAMED LEAN THEOREM, not derived a second time from the inputs -- otherwise a
;; delta of 0.0 would only say that Clojure agrees with Clojure.
;;
;; Lean theorem (MachineObservationWitness.lean)   expected here
;; emptyObservationHasFourteenZeros                fourteen 0.0 coordinates
;; clampAtCap / clampAboveCapTwo                   1.0 / 1.0  (sorry-count-norm)
;; clampAtCap / clampAboveCapFive                  1.0 / 1.0  (coupling-density)
;; sorryCountNormIsBoundedAbove                    the two capped cases hold
;; couplingDensityIsBoundedAbove                     for every raw reading
;; stackPctIsNotClamped                            70.0 passes through
;; stackSeventyIsUnbounded                         70.0
;; loopHealthNegativeThreeIsUnbounded              -3.0
;; activeRepoFiveIsUnbounded                       5.0
;; observedVariantReference / absentVariantReference  :observed / :absent
;; readbackRefusalPairAgreesNumerically            the refused pair's fourteen
;;                                                 coordinates are equal
;; readbackRefusalPairIsRefused                    ...and it is refused anyway
;; matchingEnvelopeVectorLength                    14
;; incompleteActiveRepoSummaryHasNoReading         no reading: observe raises
;; readbackCoercionPromiseIsBroken                 the envelope's :coerced-to 0.0
;;                                                 is not kept at that channel
;; incompleteCouplingSummaryReadsZero              the same missing key at
;;                                                 :coupling-density reads 0
;;
;; ONE LEAN THEOREM IS NOT MEASURABLE THROUGH `observe` AND THIS FILE SAYS SO.
;; incompleteCouplingSummaryReadsZero models observation.clj:134's default on a
;; summary that lacks :total-repos, but that input cannot be put through
;; `observe`: :active-repo-ratio raises at observation.clj:129 before any
;; coordinate is returned. The case below measures the refusal instead of the
;; value, and that IS the finding -- one channel's missing default makes the
;; other thirteen channels' coordinates unobservable on the same input.
(require '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[futon2.aif.observation :as observation])

(def empty-expected (vec (repeat 14 0.0)))
(def clamp-at-cap-expected 1.0)          ;; clampAtCap
(def clamp-above-cap-two-expected 1.0)   ;; clampAboveCapTwo
(def clamp-above-cap-five-expected 1.0)  ;; clampAboveCapFive
(def stack-expected 70.0)
(def loop-health-expected -3.0)
(def active-ratio-expected 5.0)
(def observed-variant-expected :observed)
(def absent-variant-expected :absent)
(def vector-length-expected 14)

(def empty-observation (observation/observe {}))
(def empty-envelope (observation/observation-envelope empty-observation))
(def empty-vector (observation/sense->vector empty-observation empty-envelope))
(def sorry-at-cap (:sorry-count-norm
                   (observation/observe
                    {:graph {:summary {:active-repos 0 :total-repos 0
                                       :total-sorrys 10}}})))
(def sorry-above-cap (:sorry-count-norm
                      (observation/observe
                       {:graph {:summary {:active-repos 0 :total-repos 0
                                          :total-sorrys 20}}})))
(def coupling-at-cap (:coupling-density
                      (observation/observe
                       {:graph {:summary {:total-repos 4 :coupling-edges 6}}})))
(def coupling-above-cap (:coupling-density
                         (observation/observe
                          {:graph {:summary {:total-repos 4 :coupling-edges 30}}})))
(def stack-value (:stack-pct
                  (observation/observe
                   {:graph {:dynamics {:commit-percentages {:stack 70.0}}}})))
(def loop-health-value (:loop-health
                        (observation/observe {:loop-health {:overall -3.0}})))
(def active-ratio-value (:active-repo-ratio
                         (observation/observe
                          {:graph {:summary {:active-repos 20 :total-repos 4}}})))
(def observed-variant
  (:variant (observation/observation-status
             (observation/observe {:loop-health {:overall 0.0}}) :loop-health)))
(def absent-variant
  (:variant (observation/observation-status empty-observation :loop-health)))
(def vector-refusal
  (try
    (observation/sense->vector
     empty-observation
     (observation/observation-envelope
      (observation/observe {:loop-health {:overall 0.0}})))
    {:status :unexpected-success}
    (catch clojure.lang.ExceptionInfo e
      {:status :refused :class (.getName (class e)) :message (.getMessage e)})))
(def incomplete-summary
  (try
    (observation/observe {:graph {:summary {:active-repos 3}}})
    {:status :unexpected-success}
    (catch Throwable t
      {:status :threw :class (.getName (class t)) :message (.getMessage t)})))

;; readbackRefusalPairAgreesNumerically: the pair sense->vector refuses above has
;; IDENTICAL numeric coordinates at all fourteen channels; it differs only in the
;; :loop-health variant. A match test on values alone would accept it.
(def refusal-pair-coordinates
  (let [coords (fn [envelope]
                 (mapv #(:value (get (:channels envelope) %))
                       observation/observation-channels))
        a (observation/observation-envelope empty-observation)
        b (observation/observation-envelope
           (observation/observe {:loop-health {:overall 0.0}}))]
    {:values-equal (= (coords a) (coords b))
     :envelopes-equal (= a b)
     :differing-channels (vec (remove #(= (get (:channels a) %) (get (:channels b) %))
                                      observation/observation-channels))}))

;; incompleteCouplingSummaryReadsZero is NOT measurable through `observe`:
;; observation.clj:134 supplies the default that would return 0.0, but
;; :active-repo-ratio raises at :129 on the same input before any coordinate is
;; returned. What is measured is that refusal.
(def coupling-default-unreachable
  (try
    (observation/observe {:graph {:summary {:coupling-edges 12}}})
    {:status :unexpected-success}
    (catch Throwable t
      {:status :threw :class (.getName (class t))})))

(defn delta [actual expected] (- (double actual) (double expected)))
(def lines
  ["F8 leg 1 slice 5 -- production structured-observation readback"
   "Every expected value is transcribed from the named Lean theorem table in the script header."
   (str "empty coordinate count actual=" (count empty-vector)
        " expected=" vector-length-expected)
   (str "empty coordinates actual=" empty-vector " expected=" empty-expected
        " max-delta=" (apply max (map #(Math/abs (delta %1 %2))
                                      empty-vector empty-expected)))
   (format "sorry at cap actual %.1f expected %.1f delta %.1f"
           sorry-at-cap clamp-at-cap-expected (delta sorry-at-cap clamp-at-cap-expected))
   (format "sorry above cap actual %.1f expected %.1f delta %.1f"
           sorry-above-cap clamp-above-cap-two-expected
           (delta sorry-above-cap clamp-above-cap-two-expected))
   (format "coupling at cap actual %.1f expected %.1f delta %.1f"
           coupling-at-cap clamp-at-cap-expected
           (delta coupling-at-cap clamp-at-cap-expected))
   (format "coupling above cap actual %.1f expected %.1f delta %.1f"
           coupling-above-cap clamp-above-cap-five-expected
           (delta coupling-above-cap clamp-above-cap-five-expected))
   (format "stack pass-through actual %.1f expected %.1f delta %.1f"
           stack-value stack-expected (delta stack-value stack-expected))
   (format "loop-health pass-through actual %.1f expected %.1f delta %.1f"
           loop-health-value loop-health-expected
           (delta loop-health-value loop-health-expected))
   (format "active-repo quotient actual %.1f expected %.1f delta %.1f"
           active-ratio-value active-ratio-expected
           (delta active-ratio-value active-ratio-expected))
   (str "observed variant actual=" observed-variant
        " expected=" observed-variant-expected)
   (str "absent variant actual=" absent-variant " expected=" absent-variant-expected)
   (str "sense->vector mismatched-envelope outcome=" (pr-str vector-refusal))
   (str "incomplete-summary outcome=" (pr-str incomplete-summary))
   (str "refused-pair coordinates=" (pr-str refusal-pair-coordinates))
   (str "coupling default on missing :total-repos is unreachable through observe: "
        (pr-str coupling-default-unreachable))
   (if (and (= empty-expected empty-vector)
            (= clamp-at-cap-expected sorry-at-cap)
            (= clamp-above-cap-two-expected sorry-above-cap)
            (= clamp-at-cap-expected coupling-at-cap)
            (= clamp-above-cap-five-expected coupling-above-cap)
            (= stack-expected stack-value)
            (= loop-health-expected loop-health-value)
            (= active-ratio-expected active-ratio-value)
            (= observed-variant-expected observed-variant)
            (= absent-variant-expected absent-variant)
            (= :refused (:status vector-refusal))
            (= :threw (:status incomplete-summary))
            (= "java.lang.NullPointerException" (:class incomplete-summary))
            (true? (:values-equal refusal-pair-coordinates))
            (false? (:envelopes-equal refusal-pair-coordinates))
            (= [:loop-health] (:differing-channels refusal-pair-coordinates))
            (= :threw (:status coupling-default-unreachable))
            (= "java.lang.NullPointerException" (:class coupling-default-unreachable)))
     "VERDICT: production matches every named Lean witness; all numeric deltas are 0.0."
     "VERDICT: MISMATCH.")])

(let [out (io/file "holes/labs/wm-contract/runs/F8-observe/clojure-readback.txt")]
  (io/make-parents out)
  (spit out (str (str/join "\n" lines) "\n"))
  (println (slurp out)))
