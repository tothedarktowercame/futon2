#!/usr/bin/env bb
;;
;; pz1_provisional_score.clj — PZ1 provisional score (mechanical)
;;
;; Reads the labeling sheet and two label sets, asserts id-alignment,
;; computes precision/recall/routing-accuracy for three bases (:zai1 :claude5 :agreed),
;; plus bounds on the :agreed basis.
;; Outputs pz1-provisional-score.edn.

(ns pz1-provisional-score
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [clojure.string :as str]))

;; --- Population constants (from H1) ---
(def N    1437)  ; total operator turns in window
(def H    153)   ; hits (marker-matched)
(def NH   1284)  ; non-hits

;; --- Load inputs ---
(def labs-dir "/home/joe/code/futon2/holes/labs/M-zaif-harness")

(defn load-edn [fname]
  (edn/read-string (slurp (str labs-dir "/" fname))))

(defn -main []
  (let [sheet   (:items (load-edn "pz1-labeling-sheet.edn"))
        zai1    (:labels (load-edn "pz1-labels-zai1.edn"))
        claude5 (:labels (load-edn "pz1-labels-claude5.edn"))]

    ;; --- Assert id-alignment per index ---
    (doseq [[idx [s z c]] (map-indexed vector (map vector sheet zai1 claude5))]
      (assert (= (:id s) (:id z))
              (str "id mismatch at idx " idx ": sheet=" (:id s) " zai1=" (:id z)))
      (assert (= (:id s) (:id c))
              (str "id mismatch at idx " idx ": sheet=" (:id s) " claude5=" (:id c))))
    (println "Id alignment verified for all 120 items.")

    ;; --- Zip everything together ---
    ;; Each row: {:sheet :zai1 :claude5}
    (let [rows (for [[s z c] (map vector sheet zai1 claude5)]
                 {:sheet    s
                  :zai1     z
                  :claude5  c})

          ;; Split into hits and probes (by sheet kind)
          hit-rows   (filter #(= :hit (get-in % [:sheet :kind])) rows)
          probe-rows (filter #(= :probe (get-in % [:sheet :kind])) rows)

          ;; --- Per-basis computation ---

          ;; Helper: for a given basis, extract (:correction? :route) from the right label set
          ;; :agreed uses the intersection where both agree on :correction?

          compute-basis
          (fn [basis]
            (let [;; Get correction? for each row under this basis
                  get-corr
                  (fn [row]
                    (condp = basis
                      :zai1    (:correction? (:zai1 row))
                      :claude5 (:correction? (:claude5 row))
                      :agreed  (let [zc (:correction? (:zai1 row))
                                     cc (:correction? (:claude5 row))]
                                 (when (= zc cc) zc))))  ; nil if disagree

                  get-route
                  (fn [row]
                    (condp = basis
                      :zai1    (:route (:zai1 row))
                      :claude5 (:route (:claude5 row))
                      :agreed  (let [zr (:route (:zai1 row))
                                     cr (:route (:claude5 row))]
                                 (when (= zr cr) zr))))  ; nil if routes differ

                  ;; Hits: count labeled (non-nil) and true
                  hit-corrs    (map get-corr hit-rows)
                  hit-labeled  (filter some? hit-corrs)
                  hit-true     (filter true? hit-labeled)
                  hit-false    (filter false? hit-labeled)
                  labeled-hits (count hit-labeled)
                  true-hits    (count hit-true)

                  ;; Probes
                  probe-corrs   (map get-corr probe-rows)
                  probe-labeled (filter some? probe-corrs)
                  probe-true    (filter true? probe-labeled)
                  labeled-probes (count probe-labeled)
                  true-probes   (count probe-true)

                  ;; Ratios
                  precision       (if (zero? labeled-hits) 0.0
                                      (/ (double true-hits) labeled-hits))
                  probe-true-rate (if (zero? labeled-probes) 0.0
                                      (/ (double true-probes) labeled-probes))

                  ;; Estimates
                  est-true-all-hits  (* precision H)
                  est-true-non-hits  (* probe-true-rate NH)
                  est-total-true     (+ est-true-all-hits est-true-non-hits)
                  recall             (if (zero? est-total-true) 0.0
                                         (/ est-true-all-hits est-total-true))

                  ;; Routing accuracy: among TRUE hits (for agreed: mutual-true hits whose routes agree),
                  ;; fraction where sheet's :route-claimed == labeled route
                  routing-rows
                  (condp = basis
                    :zai1
                    (for [r hit-rows
                          :let [corr (get-corr r)
                                route (get-route r)]
                          :when (true? corr)]
                      {:route-claimed (get-in r [:sheet :route-claimed])
                       :labeled-route route})

                    :claude5
                    (for [r hit-rows
                          :let [corr (get-corr r)
                                route (get-route r)]
                          :when (true? corr)]
                      {:route-claimed (get-in r [:sheet :route-claimed])
                       :labeled-route route})

                    :agreed
                    ;; mutual-true hits whose two routes agree — use agreed route
                    (for [r hit-rows
                          :let [zc (:correction? (:zai1 r))
                                cc (:correction? (:claude5 r))
                                zr (:route (:zai1 r))
                                cr (:route (:claude5 r))]
                          :when (and (true? zc) (true? cc) (= zr cr))]
                      {:route-claimed (get-in r [:sheet :route-claimed])
                       :labeled-route zr}))

                  routing-correct   (count (filter #(= (:route-claimed %) (:labeled-route %)) routing-rows))
                  routing-total     (count routing-rows)
                  routing-accuracy  (if (zero? routing-total) 0.0
                                        (/ (double routing-correct) routing-total))]

              {:basis              basis
               :precision          {:ratio precision
                                    :true-hits true-hits
                                    :labeled-hits labeled-hits}
               :probe-true-rate    {:ratio probe-true-rate
                                     :true-probes true-probes
                                     :labeled-probes labeled-probes}
               :est-true-all-hits  est-true-all-hits
               :est-true-non-hits  est-true-non-hits
               :recall             {:ratio recall
                                    :est-true-all-hits est-true-all-hits
                                    :est-total-true est-total-true}
               :routing-accuracy   {:ratio routing-accuracy
                                     :correct routing-correct
                                     :total routing-total}}))

          zai1-results    (compute-basis :zai1)
          claude5-results (compute-basis :claude5)
          agreed-results  (compute-basis :agreed)

          ;; --- Bounds on :agreed basis ---
          ;; Disputed items: where zai1 and claude5 disagree on :correction?
          disputed-hits   (filter #(not= (:correction? (:zai1 %)) (:correction? (:claude5 %))) hit-rows)
          disputed-probes (filter #(not= (:correction? (:zai1 %)) (:correction? (:claude5 %))) probe-rows)
          n-disputed-hits   (count disputed-hits)
          n-disputed-probes (count disputed-probes)

          ;; (a) treat all disputed as TRUE
          ;; precision numerator += disputed hits, denom = agreed-labeled + disputed
          ;; probe numerator += disputed probes
          let-agreed-precision (:ratio (:precision agreed-results))
          let-agreed-probe     (:ratio (:probe-true-rate agreed-results))
          agreed-true-hits     (:true-hits (:precision agreed-results))
          agreed-labeled-hits  (:labeled-hits (:precision agreed-results))
          agreed-true-probes   (:true-probes (:probe-true-rate agreed-results))
          agreed-labeled-probes (:labeled-probes (:probe-true-rate agreed-results))

          ;; Upper bound: all disputed = true
          ub-precision-num  (+ agreed-true-hits n-disputed-hits)
          ub-precision-den  (+ agreed-labeled-hits n-disputed-hits)
          ub-precision      (if (zero? ub-precision-den) 0.0 (/ (double ub-precision-num) ub-precision-den))
          ub-probe-num      (+ agreed-true-probes n-disputed-probes)
          ub-probe-den      (+ agreed-labeled-probes n-disputed-probes)
          ub-probe-rate     (if (zero? ub-probe-den) 0.0 (/ (double ub-probe-num) ub-probe-den))
          ub-est-hits       (* ub-precision H)
          ub-est-nonhits    (* ub-probe-rate NH)
          ub-est-total      (+ ub-est-hits ub-est-nonhits)
          ub-recall         (if (zero? ub-est-total) 0.0 (/ ub-est-hits ub-est-total))

          ;; Lower bound: all disputed = false.
          ;; Numerators = agreed trues only; denominators = full sample (all 60 hits / all 60 probes).
          ;; Note: bounds are NOT monotone in recall — all-true LOWERS recall because the probe term dominates.
          lb-precision-num  agreed-true-hits       ; 20
          lb-precision-den  (count hit-rows)       ; 60 (full hit sample)
          lb-precision      (if (zero? lb-precision-den) 0.0 (/ (double lb-precision-num) lb-precision-den))
          lb-probe-num      agreed-true-probes     ; 5
          lb-probe-den      (count probe-rows)     ; 60 (full probe sample)
          lb-probe-rate     (if (zero? lb-probe-den) 0.0 (/ (double lb-probe-num) lb-probe-den))
          lb-est-hits       (* lb-precision H)
          lb-est-nonhits    (* lb-probe-rate NH)
          lb-est-total      (+ lb-est-hits lb-est-nonhits)
          lb-recall         (if (zero? lb-est-total) 0.0 (/ lb-est-hits lb-est-total))

          results
          {:population {:N N :H H :NH NH}
           :disputed   {:disputed-hits n-disputed-hits
                        :disputed-probes n-disputed-probes}
           :bases      {:zai1    zai1-results
                        :claude5 claude5-results
                        :agreed  agreed-results}
           :agreed-bounds
           {:upper {:description "all disputed = true"
                    :precision  {:ratio ub-precision :num ub-precision-num :den ub-precision-den}
                    :probe-true-rate {:ratio ub-probe-rate :num ub-probe-num :den ub-probe-den}
                    :recall {:ratio ub-recall :est-true-all-hits ub-est-hits :est-total-true ub-est-total}}
            :lower {:description "all disputed = false"
                    :precision  {:ratio lb-precision :num lb-precision-num :den lb-precision-den}
                    :probe-true-rate {:ratio lb-probe-rate :num lb-probe-num :den lb-probe-den}
                    :recall {:ratio lb-recall :est-true-all-hits lb-est-hits :est-total-true lb-est-total}}}}]

      ;; Write results
      (let [edn-str (with-out-str (pprint/pprint results))]
        (spit (str labs-dir "/pz1-provisional-score.edn") edn-str)
        (println "Wrote pz1-provisional-score.edn"))

      ;; Print summary
      (println "\n=== PZ1 PROVISIONAL SCORE ===\n")
      (println "Population: N=" N "H=" H "NH=" NH)
      (println "Disputed: hits=" n-disputed-hits "probes=" n-disputed-probes)
      (println)
      (doseq [res [zai1-results claude5-results agreed-results]]
        (let [b (:basis res)]
          (println (format "--- %s ---" (name b)))
          (println (format "  precision:       %.4f  (%d/%d)"
                           (double (:ratio (:precision res)))
                           (:true-hits (:precision res))
                           (:labeled-hits (:precision res))))
          (println (format "  probe-true-rate:  %.4f  (%d/%d)"
                           (double (:ratio (:probe-true-rate res)))
                           (:true-probes (:probe-true-rate res))
                           (:labeled-probes (:probe-true-rate res))))
          (println (format "  est-true-all-hits:  %.2f" (:est-true-all-hits res)))
          (println (format "  est-true-non-hits:  %.2f" (:est-true-non-hits res)))
          (println (format "  recall:          %.4f" (double (:ratio (:recall res)))))
          (println (format "  routing-acc:     %.4f  (%d/%d)"
                           (double (:ratio (:routing-accuracy res)))
                           (:correct (:routing-accuracy res))
                           (:total (:routing-accuracy res))))
          (println)))
      (println "--- agreed BOUNDS ---")
      (println (format "  UPPER (all disputed=true):  precision=%.4f  recall=%.4f"
                       (double ub-precision) (double ub-recall)))
      (println (format "  LOWER (all disputed=false): precision=%.4f  recall=%.4f"
                       (double lb-precision) (double lb-recall)))))

  nil)

(-main)
