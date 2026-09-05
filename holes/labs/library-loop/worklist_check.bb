#!/usr/bin/env bb
(require '[clojure.edn :as edn])
(def path (or (first *command-line-args*) "worklist.edn"))
(def w (try (edn/read-string (slurp path))
            (catch Exception e (binding [*out* *err*] (println "library-worklist: unreadable:" (.getMessage e))) (System/exit 1))))
(def items (:items w))
(def ids (mapv :id items))
(def id-set (set ids))
(def allowed-statuses (:statuses w))
(def allowed-classes (set (keys (:classes w))))
(def errors
  (vec
   (concat
    (when (not= :wm/worklist-v1 (:schema w)) ["schema must be :wm/worklist-v1"])
    (when-not (vector? items) [":items must be a vector"])
    (when (not= (count ids) (count id-set)) ["item ids must be unique"])
    (mapcat
     (fn [i]
       (let [p (str (:id i) " ")]
         (concat
          (when-not (keyword? (:id i)) [(str p "lacks keyword :id")])
          (when-not (allowed-classes (:class i)) [(str p "has unknown :class")])
          (when-not (allowed-statuses (:status i)) [(str p "has unknown :status")])
          (when-not (contains? i :owner) [(str p "lacks :owner")])
          (when-not (string? (:statement i)) [(str p "lacks :statement")])
          (when-not (string? (:acceptance i)) [(str p "lacks :acceptance")])
          (for [d (:depends-on i) :when (not (id-set d))] (str p "depends on missing " d))
          (when (some #{(:id i)} (:depends-on i)) [(str p "depends on itself")])
          (when (and (= :done-unreviewed (:status i)) (not (string? (:evidence i)))) [(str p "done-unreviewed lacks :evidence")])
          (when (and (= :done (:status i)) (not (string? (:reviewed-by i)))) [(str p "done lacks :reviewed-by")])
          (when (and (= :done (:status i)) (not (string? (:review i)))) [(str p "done lacks :review")]))))
     items))))
(if (seq errors)
  (do (doseq [e errors] (binding [*out* *err*] (println "library-worklist:" e))) (System/exit 1))
  (println "library-worklist:" (count items) "items OK;" (frequencies (map :status items))))
