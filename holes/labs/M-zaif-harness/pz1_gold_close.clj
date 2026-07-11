#!/usr/bin/env bb
;; PZ1 close-out: integrate Joe's gold labels (decide) with the agreed
;; agent labels (propose) and compute the final lexicon measurement plus
;; per-agent accuracy against gold.
(require '[clojure.edn :as edn]
         '[clojure.string :as str])

(def lab-dir "/home/joe/code/futon2/holes/labs/M-zaif-harness/")
(def sheet (:items (edn/read-string (slurp (str lab-dir "pz1-labeling-sheet.edn")))))
(def zai (:labels (edn/read-string (slurp (str lab-dir "pz1-labels-zai1.edn")))))
(def c5 (:labels (edn/read-string (slurp (str lab-dir "pz1-labels-claude5.edn")))))

;; --- parse Joe's answers out of the gold sheet md ---
(def gold
  (into {}
        (keep (fn [[_ idx ans]]
                (let [idx (parse-long idx)
                      ans (str/trim ans)]
                  (cond
                    (re-find #"(?i)^yes" ans)
                    [idx {:correction? true
                          :route (cond (re-find #"γ|gamma" ans) :gamma
                                       (re-find #"(?i)actand" ans) :actand
                                       (re-find #"C" ans) :c-channel)}]
                    (re-find #"(?i)^no?$" ans) [idx {:correction? false :route nil}]
                    :else (throw (ex-info "unparseable answer" {:idx idx :ans ans}))))))
        (re-seq #"## item (\d+) \([^)]*\)\n\n(?s).*?\n-> ([^\n]*)"
                (slurp (str lab-dir "pz1-gold-sheet.md")))))
(assert (= 32 (count gold)) (str "parsed " (count gold) " gold answers"))

(def H 153) (def NH 1284)

(def rows
  (map-indexed
   (fn [i [s z c]]
     (let [agreed? (= (boolean (:correction? z)) (boolean (:correction? c)))
           g (get gold i)
           truth (if g
                   (:correction? g)
                   (do (assert agreed? (str "non-gold disputed row " i))
                       (boolean (:correction? z))))
           route (if g
                   (:route g)
                   (when (= (:route z) (:route c)) (:route z)))]
       {:idx i :id (:id s) :kind (:kind s) :claimed (:route-claimed s)
        :z? (boolean (:correction? z)) :zr (:route z)
        :c? (boolean (:correction? c)) :cr (:route c)
        :gold g :true? truth :route route}))
   (map vector sheet zai c5)))

;; --- doc-level consistency check on gold answers ---
(println "== gold-internal consistency (same doc, two rows) ==")
(doseq [[id grp] (group-by :id (filter :gold rows))
        :when (and (> (count grp) 1)
                   (apply not= (map :true? grp)))]
  (println "  CONFLICT" id "rows" (mapv :idx grp) "answers" (mapv :true? grp)))

;; --- calibration: agreed items in gold ---
(let [calib (filter #(and (:gold %) (= (:z? %) (:c? %))) rows)
      ok (filter #(= (:true? %) (:z? %)) calib)]
  (println "\n== calibration (agreed items Joe re-labeled) ==")
  (println (format "  %d/%d agreed labels confirmed by gold" (count ok) (count calib)))
  (doseq [r (remove #(= (:true? %) (:z? %)) calib)]
    (println (format "  FLIP idx %d %s: agents=%s gold=%s"
                     (:idx r) (subs (:id r) 0 10) (:z? r) (:true? r)))))

;; --- agent accuracy on the 32 gold items ---
(println "\n== agent accuracy vs gold (32 items) ==")
(let [gr (filter :gold rows)]
  (doseq [[nm k rk] [["zai-1" :z? :zr] ["claude-5" :c? :cr]]]
    (let [corr-ok (count (filter #(= (get % k) (:true? %)) gr))
          trues (filter :true? gr)
          route-ok (count (filter #(= (get % rk) (get-in % [:gold :route])) trues))]
      (println (format "  %-9s correction?: %d/32 | route on gold-trues: %d/%d"
                       nm corr-ok route-ok (count trues))))))

;; --- final lexicon measurement ---
(let [hits (filter #(= :hit (:kind %)) rows)
      probes (filter #(= :probe (:kind %)) rows)
      th (count (filter :true? hits))
      tp (count (filter :true? probes))
      precision (/ th 60.0)
      ptr (/ tp 60.0)
      est-h (* precision H) est-n (* ptr NH)
      recall (/ est-h (+ est-h est-n))
      routable (filter #(and (:true? %) (:route %)) hits)
      r-ok (count (filter #(= (:claimed %) (:route %)) routable))]
  (println "\n== FINAL PZ1 (gold decides, agreed fills) ==")
  (println (format "  true hits %d/60 -> precision %.3f" th precision))
  (println (format "  true probes %d/60 -> probe-rate %.3f" tp ptr))
  (println (format "  est corrections: %.1f in hits + %.1f in non-hits" est-h est-n))
  (println (format "  recall %.3f" recall))
  (println (format "  routing accuracy %d/%d = %.3f on true hits with a settled route"
                   r-ok (count routable) (/ r-ok (double (count routable)))))
  (println (format "  gold route distribution: %s"
                   (frequencies (keep #(get-in % [:gold :route]) (filter :gold rows))))))

(spit (str lab-dir "pz1-final-truth.edn")
      (with-out-str
        (clojure.pprint/pprint
         {:note "row-level final truth: Joe's gold decides on 32 rows; agreed agent labels fill the rest. Known operator conflict on e-0cae94f2 (rows 0/59) kept as coded."
          :rows (mapv #(select-keys % [:idx :id :kind :true? :route :gold]) rows)})))
(println "\nwrote pz1-final-truth.edn")
