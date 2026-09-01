#!/usr/bin/env bb
;; Negative controls for run3_conformance.bb. Each plants ONE defect into a copy
;; of the real S1b route and asserts the check refuses it.
(require '[clojure.edn :as edn] '[clojure.java.io :as io])
(def src "runs/2026-09-01-s1b/wm-trace-s1b.edn")
(defn read-forms [p]
  (with-open [r (java.io.PushbackReader. (io/reader p))]
    (loop [a []] (let [f (edn/read {:eof ::eof} r)] (if (= ::eof f) a (recur (conj a f)))))))
(def rec (first (read-forms src)))
(defn tag [n] {:node n :via "planted" :at "2026-09-01T00:00:00Z"})
(defn write-one! [path route]
  (spit path (str (pr-str (assoc rec :wm/route route)) "\n")))
(def base (:wm/route rec))

;; 1. a code-retired ROUTE-GRAIN edge: R7->R14 (retired :r7-r14-conflates-two-precisions,
;;    :grounds :code, and NOT in :route-measured-drawn, so it is route-grain)
(write-one! "/tmp/c1.edn" (into (vec (take 2 base)) [(tag :R7) (tag :R14)]))
;; 2. an unknown pair: R4->R20 appears nowhere
(write-one! "/tmp/c2.edn" (into (vec (take 2 base)) [(tag :R4) (tag :R20)]))
;; 3. a DRAWN edge traversed in reverse: R7->R3 is drawn, R3->R7 is not
(write-one! "/tmp/c3.edn" (into (vec (take 2 base)) [(tag :R3) (tag :R7)]))
(println "controls written")
