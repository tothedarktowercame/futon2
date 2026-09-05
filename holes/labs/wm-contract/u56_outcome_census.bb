#!/usr/bin/env bb
;; U56 §3 -- the outcome-leg census over the live wm-trace corpus.
;;
;;   bb u56_outcome_census.bb
;;
;; READ-ONLY. Reads data/wm-trace only; writes nothing, takes no run lock,
;; deposits nothing. No wall-clock field in the output, so two runs over an
;; unchanged corpus print identically.
;;
;; WHAT IT MEASURES AND WHY. :rationale-regret's UPHELD leg needs an outcome
;; (u39_selection_retrospective.bb:779-781). `trace-outcome` looks in three
;; places (u39:55-59, mirroring war_machine.clj:2425-2428). This counts how many
;; records reach each of the three, AND how many carry a :realized-outcome map
;; under the key shape the corpus actually uses -- which is the difference
;; between "no outcome was ever recorded" and "the reader looks for a key the
;; producer never wrote".
(require '[clojure.edn :as edn] '[clojure.java.io :as io])

(def repo-root (str (System/getProperty "user.home") "/code/futon2"))
;; Old records carry tagged literals; keep the tag visible rather than throw, so
;; the census covers every file instead of stopping at the first one (u39:34-36).
(def read-opts {:default (fn [t v] {:unread-tag t :value v})})

(defn trace-files []
  (->> (file-seq (io/file repo-root "data/wm-trace"))
       (filter #(.isFile ^java.io.File %))
       (map str)
       (filter #(re-find #"wm-trace-\d{4}-\d{2}-\d{2}\.edn$" %))
       sort vec))

(defn records [path]
  (with-open [r (io/reader path)]
    (mapv (fn [l] (edn/read-string read-opts l)) (line-seq r))))

(defn trace-outcome [m]
  (or (:outcome m)
      (get-in m [:enactment :outcome])
      (get-in m [:realized-outcome :outcome])))

(let [files (trace-files)
      all (vec (mapcat records files))
      ros (filter :realized-outcome all)]
  (println (format "files %d | records %d" (count files) (count all)))
  (println (format "  carrying a controller ranking          %d" (count (filter #(seq (get-in % [:decision :controller-ranking])) all))))
  (println (format "  reaching trace-outcome (any of three)  %d" (count (filter trace-outcome all))))
  (println (format "    (:outcome m)                         %d" (count (filter :outcome all))))
  (println (format "    [:enactment :outcome]                %d" (count (filter #(get-in % [:enactment :outcome]) all))))
  (println (format "    [:realized-outcome :outcome]         %d" (count (filter #(get-in % [:realized-outcome :outcome]) all))))
  (println (format "  carrying :enactment                    %d" (count (filter :enactment all))))
  (println (format "  carrying :realized-outcome             %d" (count ros)))
  (println (format "    with a non-nil :realized-G           %d" (count (filter #(some? (:realized-G (:realized-outcome %))) ros))))
  (println (format "    with a non-nil :expected-G           %d" (count (filter #(some? (:expected-G (:realized-outcome %))) ros))))
  (println (format "    with an :outcome key                 %d" (count (filter #(contains? (:realized-outcome %) :outcome) ros))))
  (println "  distinct :realized-outcome key-sets:")
  (doseq [ks (sort-by str (distinct (map #(vec (sort (keys (:realized-outcome %)))) ros)))]
    (println "   " (pr-str ks)))
  (println "  files carrying :realized-outcome:")
  (doseq [f (sort (distinct (for [f files, r (records f) :when (:realized-outcome r)] f)))]
    (println "   " (subs f (inc (count repo-root))))))
