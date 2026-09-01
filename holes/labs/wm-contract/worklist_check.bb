#!/usr/bin/env bb
;; worklist_check.bb -- prove the ledger before anyone acts on it (class 6a).
(require '[clojure.edn :as edn])
(def w (edn/read-string (slurp (or (first *command-line-args*) "worklist.edn"))))
(defn die [& m] (binding [*out* *err*] (apply println "worklist_check:" m)) (System/exit 1))
(when-not (= :wm/worklist-v1 (:schema w)) (die "unexpected schema"))
(def ids (map :id (:items w)))
(when (not= (count ids) (count (set ids))) (die "duplicate ids"))
(doseq [i (:items w)]
  (doseq [k [:id :class :status :owner :statement :acceptance]] (when-not (contains? i k) (die (:id i) "lacks" k)))
  (when-not (contains? (:classes w) (:class i)) (die (:id i) "unknown class" (:class i)))
  (when-not (contains? (:statuses w) (:status i)) (die (:id i) "unknown status" (:status i)))
  (when (and (= :J (:class i)) (not (or (= :needs-joe (:status i)) (and (= :done (:status i)) (:ruling i) (= "joe" (:reviewed-by i)))))) (die (:id i) "class J must be :needs-joe, or :done with :ruling and :reviewed-by joe"))
  (when (and (#{:done-unreviewed :done} (:status i)) (not (:evidence i))) (die (:id i) "done without :evidence"))
  (when (and (= :done (:status i)) (not (:reviewed-by i))) (die (:id i) ":done without :reviewed-by")))
(def by-status (frequencies (map :status (:items w))))
(println (format "worklist_check: %d items OK; %s" (count (:items w)) (pr-str by-status)))
