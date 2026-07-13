(ns fold-realized-zero-coverage-test
  "T-0 fix tests for gamma realizedG nil -> zero-coverage semantics.
   claude-16 ruling 2026-07-06: zero-construction enactment records realized 0.0.

   Test 1: zero-construction enactment → realized 0.0, non-nil, holes=N
   Test 2: devmap-coherence fixture (RULES-covered) → realized >0 boxes (varies by family)
   Test 3: anti-fake-calibration guard: realized != expected on 007-shaped case
   Test 4: full gamma sample (expected + realized) lands end-to-end")

(require '[futon2.aif.fold-realized :as fr]
         '[futon2.aif.fold-escrow :as escrow]
         '[clojure.java.shell :as shell]
         '[cheshire.core :as json])

(def fails (atom 0))
(defn t [nm p]
  (if p
    (println "  PASS" nm)
    (do (swap! fails inc) (println "  FAIL" nm))))

;; ===== TEST 1: zero-construction enactment → realized 0.0 =====
(println "\n=== TEST 1: zero-construction enactment → realized 0.0 ===")
(let [enacted {:boxes [] :policy-holes [{:unfolded-pattern "a"}
                                         {:unfolded-pattern "b"}
                                         {:unfolded-pattern "c"}]}
      decision {:policy "test-mission" :expected-score -0.615 :fold {:coverage-score-delta -0.615}}
      ro (fr/realized-outcome-of decision enacted :tick-1)]
  (t "realized-G is non-nil" (some? (:realized-score ro)))
  (t "realized-G is 0.0" (= 0.0 (:realized-score ro)))
  (t "expected-G preserved" (= -0.615 (:expected-score ro)))
  (t "holes carried (N=3)" (= 3 (count (:policy-holes enacted)))))

;; ===== TEST 2: devmap-coherence fixture (RULES-covered) → realized >0 boxes =====
(println "\n=== TEST 2: devmap-coherence fixture → realized >0 boxes ===")
(let [{:keys [exit out]} (shell/sh "bb" "--classpath" "src"
                                    "holes/labs/M-memes-arrows/fold_engine.clj"
                                    "apply"
                                    (json/generate-string ["devmap-coherence/prototype-structure-checklist"
                                                           "devmap-coherence/next-steps-to-done"
                                                           "devmap-coherence/prototype-alignment-role"])
                                    "MissionState -> {Wiring, PolicyHoles}"
                                    :dir "/home/joe/code/futon3a")
      enacted-wiring (if (zero? exit) (:wiring (json/parse-string out true)) nil)
      box-count (count (:boxes enacted-wiring))
      ro (fr/realized-outcome-of {:policy "test" :expected-score -0.5 :fold {:coverage-score-delta -0.5}}
                                 enacted-wiring :tick-2)]
  (t "fold engine produced >0 boxes for devmap-coherence" (pos? box-count))
  (t "realized-G is negative (non-zero coverage)" (and (number? (:realized-score ro)) (neg? (:realized-score ro))))
  (t "realized-G differs from 0.0" (not= 0.0 (:realized-score ro))))

;; ===== TEST 3: anti-fake-calibration guard: realized != expected =====
(println "\n=== TEST 3: anti-fake-calibration guard ===")
;; The 007-shaped case: deposit says expectedG=-1.0, fold engine produces 0 boxes
;; → realized must be 0.0, NOT -1.0 (escrow-as-realized is NOT used)
(let [enacted {:boxes [] :policy-holes [{:unfolded-pattern "x"} {:unfolded-pattern "y"}]}
      decision {:policy "M-first-flights" :expected-score -1.0 :fold {:coverage-score-delta -1.0}}
      ro (fr/realized-outcome-of decision enacted :tick-3)]
  (t "realized-G (0.0) != expected-G (-1.0)" (not= (:realized-score ro) (:expected-score ro)))
  (t "realized-G is 0.0 (not faked from expected)" (= 0.0 (:realized-score ro)))
  (t "signed error is +1.0 (plan over-promised)" (= 1.0 (+ (:realized-score ro) (- (:expected-score ro))))))

;; ===== TEST 4: full gamma sample end-to-end for qualifying deposits =====
(println "\n=== TEST 4: full gamma sample (expected + realized) end-to-end ===")
(let [deposits (:deposits (escrow/load-deposits))
      qualifying? (fn [d]
                    (let [f (get-in d [:cascade :cascade-score])
                          dg (get-in d [:eval :coverage-score-delta])]
                      (and (number? f) (pos? f) (number? dg) (neg? dg))))
      qualifiers (filter qualifying? deposits)]
  (doseq [d qualifiers]
    (let [id (:fold-turn/id d)
          mission (:mission d)
          pattern-ids (get-in d [:cascade :pattern-ids])
          exp-dg (get-in d [:eval :coverage-score-delta])
          {:keys [exit out]} (shell/sh "bb" "--classpath" "src"
                                       "holes/labs/M-memes-arrows/fold_engine.clj"
                                       "apply" (json/generate-string (vec pattern-ids))
                                       "MissionState -> {Wiring, PolicyHoles}"
                                       :dir "/home/joe/code/futon3a")
          enacted-wiring (if (zero? exit) (:wiring (json/parse-string out true)) nil)
          decision {:policy mission :expected-score exp-dg :fold {:coverage-score-delta exp-dg}}
          ro (fr/realized-outcome-of decision enacted-wiring :tick-4)
          holes (count (:policy-holes enacted-wiring))
          r-g (:realized-score ro)
          e-g (:expected-score ro)
          s-err (when (and (number? r-g) (number? e-g)) (- r-g e-g))]
      (println (str "  " id ": expectedG=" e-g " realizedG=" r-g
                    " holes=" holes " signed-error=" s-err))
      (t (str id " realized-G non-nil") (some? r-g))
      (t (str id " realized-G != expected-G (calibration signal)") (not= r-g e-g)))))

;; ===== BONUS: genuinely unmeasurable case stays nil =====
(println "\n=== BONUS: nil wiring → realized-G stays nil (genuinely unmeasurable) ===")
(let [ro (fr/realized-outcome-of {:policy "test" :expected-score -0.5 :fold {:coverage-score-delta -0.5}}
                                  nil :tick-5)]
  (t "nil wiring → realized-G nil" (nil? (:realized-score ro))))
(let [ro (fr/realized-outcome-of {:policy "test" :expected-score -0.5 :fold {:coverage-score-delta -0.5}}
                                  {:boxes [] :policy-holes []} :tick-6)]
  (t "empty wiring (no boxes, no holes) → realized-G nil" (nil? (:realized-score ro))))

;; ===== SUMMARY =====
(println "\n" (if (zero? @fails) "ALL TESTS PASS" (str @fails " TESTS FAILED")))
