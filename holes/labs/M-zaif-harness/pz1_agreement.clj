#!/usr/bin/env bb
;; H3 mechanical agreement: zai-1 (blind, by handoff) vs claude-5 (blind,
;; direct) over the 120-item PZ1 sheet. Matching is positional (both files
;; verified to be in sheet order) with id assertions. Also builds Joe's
;; gold-pass sheet: every correction? disagreement, every route disagreement
;; among mutual trues, plus 10 seeded-random agreed items as calibration —
;; contexts only, neither agent's label shown.
(require '[clojure.edn :as edn]
         '[clojure.string :as str])

(def lab-dir "/home/joe/code/futon2/holes/labs/M-zaif-harness/")
(def sheet (:items (edn/read-string (slurp (str lab-dir "pz1-labeling-sheet.edn")))))
(def zai (:labels (edn/read-string (slurp (str lab-dir "pz1-labels-zai1.edn")))))
(def c5 (:labels (edn/read-string (slurp (str lab-dir "pz1-labels-claude5.edn")))))

(assert (= 120 (count sheet) (count zai) (count c5)))
(doseq [[i [s z c]] (map-indexed vector (map vector sheet zai c5))]
  (assert (= (:id s) (:id z) (:id c)) (str "id misalignment at index " i)))

(def rows
  (map-indexed
   (fn [i [s z c]]
     {:idx i :id (:id s) :kind (:kind s) :marker (:marker s)
      :zai? (boolean (:correction? z)) :zai-route (:route z)
      :c5? (boolean (:correction? c)) :c5-route (:route c)})
   (map vector sheet zai c5)))

;; --- correction? agreement + Cohen's kappa ---
(def n (count rows))
(def agree (filter #(= (:zai? %) (:c5? %)) rows))
(def disagree (remove #(= (:zai? %) (:c5? %)) rows))
(def po (/ (count agree) (double n)))
(def pz (/ (count (filter :zai? rows)) (double n)))
(def pc (/ (count (filter :c5? rows)) (double n)))
(def pe (+ (* pz pc) (* (- 1 pz) (- 1 pc))))
(def kappa (/ (- po pe) (- 1 pe)))

;; --- route agreement among mutual trues ---
(def mutual-true (filter #(and (:zai? %) (:c5? %)) rows))
(def route-agree (filter #(= (:zai-route %) (:c5-route %)) mutual-true))
(def route-disagree (remove #(= (:zai-route %) (:c5-route %)) mutual-true))

(println "== H3 mechanical agreement ==")
(println (format "n=%d | correction? agreement: %d/%d (%.1f%%) | kappa=%.3f"
                 n (count agree) n (* 100 po) kappa))
(println (format "zai-1 trues: %d | claude-5 trues: %d | mutual trues: %d"
                 (count (filter :zai? rows)) (count (filter :c5? rows)) (count mutual-true)))
(println (format "route agreement among mutual trues: %d/%d"
                 (count route-agree) (count mutual-true)))
(println)
(println "correction? disagreements (idx/id/kind/marker/zai/c5):")
(doseq [r disagree]
  (println (format "  %3d %s %-6s %-14s zai=%-5s c5=%s"
                   (:idx r) (subs (:id r) 0 10) (name (:kind r))
                   (str (:marker r)) (:zai? r) (:c5? r))))
(println)
(println "route disagreements among mutual trues:")
(doseq [r route-disagree]
  (println (format "  %3d %s %-14s zai=%-10s c5=%s"
                   (:idx r) (subs (:id r) 0 10) (str (:marker r))
                   (str (:zai-route r)) (str (:c5-route r)))))

;; --- gold-pass selection ---
(def gold-core (concat (map :idx disagree) (map :idx route-disagree)))
(def rng (java.util.Random. 20260711))
(def agreed-pool (vec (remove (set gold-core) (map :idx agree))))
(def calib
  (loop [picked #{}]
    (if (or (= 10 (count picked)) (= (count picked) (count agreed-pool)))
      picked
      (recur (conj picked (nth agreed-pool (.nextInt rng (count agreed-pool))))))))
(def gold-idxs (sort (distinct (concat gold-core calib))))

(println)
(println (format "gold pass: %d items (%d disagreements + %d route-disagreements + %d calibration)"
                 (count gold-idxs) (count disagree) (count route-disagree) (count calib)))

;; write the gold sheet — contexts only, NO agent labels, no marker/route-claimed
(let [items (map (fn [i]
                   (let [s (nth sheet i)]
                     {:idx i :id (:id s) :at (:at s) :context (:context s)}))
                 gold-idxs)]
  (spit (str lab-dir "pz1-gold-sheet.edn")
        (with-out-str (clojure.pprint/pprint {:note "Operator gold pass — labels decide. Agents' labels proposed elsewhere; not shown here."
                                              :items (vec items)})))
  (spit (str lab-dir "pz1-gold-sheet.md")
        (str "# PZ1 gold pass — Joe's labels decide\n\n"
             "For each item: is this turn a **correction** (you pushing back on / redirecting\n"
             "something the agent did, claimed, planned, or believed)? If yes, which route:\n"
             "**γ** (approach/plan redirect) | **C** (repairing the agent's model of your intent)\n"
             "| **actand** (correcting a factual belief about world/artifact state).\n\n"
             "Mark each line, e.g. `-> no` or `-> yes γ`.\n\n"
             (str/join "\n"
                       (map (fn [{:keys [idx id at context]}]
                              (str "## item " idx " (" (subs id 0 12) "…, " (subs at 0 10) ")\n\n"
                                   "> " (str/replace context "\n" "\n> ") "\n\n"
                                   "-> \n"))
                            items))))
  (println "wrote pz1-gold-sheet.edn + pz1-gold-sheet.md"))
