#!/usr/bin/env bb
;; H4 adjudication — claude-5's independent recomputation of the provisional
;; PZ1 score, written and run BEFORE reading zai-1's H4 report or script.
(require '[clojure.edn :as edn])

(def lab-dir "/home/joe/code/futon2/holes/labs/M-zaif-harness/")
(def sheet (:items (edn/read-string (slurp (str lab-dir "pz1-labeling-sheet.edn")))))
(def zai (:labels (edn/read-string (slurp (str lab-dir "pz1-labels-zai1.edn")))))
(def c5 (:labels (edn/read-string (slurp (str lab-dir "pz1-labels-claude5.edn")))))

(assert (= 120 (count sheet) (count zai) (count c5)))
(doseq [[i [s z c]] (map-indexed vector (map vector sheet zai c5))]
  (assert (= (:id s) (:id z) (:id c)) (str "misalignment at " i)))

(def H 153) (def NH 1284)

(def rows (map (fn [s z c]
                 {:kind (:kind s) :claimed (:route-claimed s)
                  :z? (boolean (:correction? z)) :zr (:route z)
                  :c? (boolean (:correction? c)) :cr (:route c)})
               sheet zai c5))

(defn stats [pred-true? pred-labeled? pred-route]
  ;; pred-true?: row -> is it a TRUE correction on this basis
  ;; pred-labeled?: row -> is it in the denominator on this basis
  ;; pred-route: row -> labeled route (for routing accuracy), nil = skip
  (let [hits (filter #(= :hit (:kind %)) rows)
        probes (filter #(= :probe (:kind %)) rows)
        lh (filter pred-labeled? hits)
        lp (filter pred-labeled? probes)
        th (filter pred-true? lh)
        tp (filter pred-true? lp)
        precision (/ (count th) (double (count lh)))
        ptr (/ (count tp) (double (count lp)))
        est-hits (* precision H)
        est-non (* ptr NH)
        recall (/ est-hits (+ est-hits est-non))
        routable (keep (fn [r] (when (and (pred-true? r) (pred-route r))
                                 [(:claimed r) (pred-route r)]))
                       lh)
        route-acc (when (seq routable)
                    (/ (count (filter (fn [[a b]] (= a b)) routable))
                       (double (count routable))))]
    {:true-hits (count th) :labeled-hits (count lh)
     :true-probes (count tp) :labeled-probes (count lp)
     :precision precision :probe-true-rate ptr
     :est-true-all-hits est-hits :est-true-non-hits est-non
     :recall recall
     :route-n (count routable) :route-match (count (filter (fn [[a b]] (= a b)) routable))
     :routing-accuracy route-acc}))

(def agreed? #(= (:z? %) (:c? %)))

(def result
  {:zai1 (stats :z? (constantly true) :zr)
   :claude5 (stats :c? (constantly true) :cr)
   :agreed (stats #(and (agreed? %) (:z? %)) agreed?
                  #(when (and (:z? %) (:c? %) (= (:zr %) (:cr %))) (:zr %)))
   :agreed-bounds
   {:disputed-all-true (stats #(or (not (agreed? %)) (:z? %)) (constantly true)
                              (constantly nil))
    :disputed-all-false (stats #(and (agreed? %) (:z? %)) (constantly true)
                               (constantly nil))}})

(clojure.pprint/pprint result)
