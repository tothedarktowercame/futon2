;; F8 leg 1 slice 5: production structured-observation readback.
;;
;; PROVENANCE RULE: EVERY -expected IN THIS READBACK IS TRANSCRIBED FROM A
;; NAMED LEAN THEOREM, not derived a second time from the inputs.
;;
;; Lean theorem                                      expected
;; emptyObservationHasFourteenZeros                  fourteen 0.0 coordinates
;; sorryAtCap / sorryAboveCap                        1.0 / 1.0
;; couplingAtCap / couplingAboveCap                  1.0 / 1.0
;; stackSeventyIsUnbounded                           70.0
;; loopHealthNegativeThreeIsUnbounded                -3.0
;; activeRepoFiveIsUnbounded                         5.0
;; observedVariantReference / absentVariantReference :observed / :absent
;; matchingEnvelopeVectorLength                      14
;; incompleteSummaryReference                        incomplete input refused
(require '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[futon2.aif.observation :as observation])

(def empty-expected (vec (repeat 14 0.0)))
(def sorry-at-cap-expected 1.0)
(def sorry-above-cap-expected 1.0)
(def coupling-at-cap-expected 1.0)
(def coupling-above-cap-expected 1.0)
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
           sorry-at-cap sorry-at-cap-expected (delta sorry-at-cap sorry-at-cap-expected))
   (format "sorry above cap actual %.1f expected %.1f delta %.1f"
           sorry-above-cap sorry-above-cap-expected
           (delta sorry-above-cap sorry-above-cap-expected))
   (format "coupling at cap actual %.1f expected %.1f delta %.1f"
           coupling-at-cap coupling-at-cap-expected
           (delta coupling-at-cap coupling-at-cap-expected))
   (format "coupling above cap actual %.1f expected %.1f delta %.1f"
           coupling-above-cap coupling-above-cap-expected
           (delta coupling-above-cap coupling-above-cap-expected))
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
   (if (and (= empty-expected empty-vector)
            (= sorry-at-cap-expected sorry-at-cap)
            (= sorry-above-cap-expected sorry-above-cap)
            (= coupling-at-cap-expected coupling-at-cap)
            (= coupling-above-cap-expected coupling-above-cap)
            (= stack-expected stack-value)
            (= loop-health-expected loop-health-value)
            (= active-ratio-expected active-ratio-value)
            (= observed-variant-expected observed-variant)
            (= absent-variant-expected absent-variant)
            (= :refused (:status vector-refusal))
            (= :threw (:status incomplete-summary))
            (= "java.lang.NullPointerException" (:class incomplete-summary)))
     "VERDICT: production matches every named Lean witness; all numeric deltas are 0.0."
     "VERDICT: MISMATCH.")])

(let [out (io/file "holes/labs/wm-contract/runs/F8-observe/clojure-readback.txt")]
  (io/make-parents out)
  (spit out (str (str/join "\n" lines) "\n"))
  (println (slurp out)))
