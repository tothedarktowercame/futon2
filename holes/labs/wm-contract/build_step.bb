#!/usr/bin/env bb
;; build_step.bb -- the ledger queries wm-build-loop.sh needs, so the loop
;; holds no ledger logic of its own. Subcommands:
;;   next-open      print the id of the next row the loop may work (or NONE)
;;   unblock        turn :blocked rows whose :depends-on are all :done into :open (commits nothing)
;;   unreviewed     print ids of :done-unreviewed rows
;;   registry-held  print 1 if any :done-unreviewed row touches a registry (publish gate), else 0
;;   counts         print the status frequencies
(require '[clojure.edn :as edn] '[clojure.string :as str])
(def path (str (.getParent (.getAbsoluteFile (java.io.File. *file*))) "/worklist.edn"))
(def w (edn/read-string (slurp path)))
(def items (:items w))
(def by-id (into {} (map (juxt :id identity) items)))
(defn loopable? [i]
  (and (= :open (:status i))
       (not= :J (:class i))
       (not= :joe (:owner i))
       (not (:loop-skip i))))
;; Priority: rows the loop itself needs first (a run lock before any run),
;; then RUN rows in id order, then everything else in ledger order.
(defn prio [i] [(case (:id i) :RUN12 0 :RUN11 1 2) (if (= :RUN (:class i)) 0 1)])
(def cmd (first *command-line-args*))
(case cmd
  "next-open" (println (or (some-> (first (sort-by prio (filter loopable? items))) :id name) "NONE"))
  "unreviewed" (println (str/join " " (map (comp name :id) (filter #(= :done-unreviewed (:status %)) items))))
  "registry-held" (println (if (some #(and (= :done-unreviewed (:status %))
                                          (not= :none (:covers-key %))
                                          (or (:covers-key %) (= :C (:class %)) (= :D (:class %))))
                                    items) 1 0))
  "counts" (println (frequencies (map :status items)))
  "unblock"
  (let [ready (filter #(and (= :blocked (:status %)) (seq (:depends-on %))
                            (every? (fn [d] (= :done (:status (by-id d)))) (:depends-on %))
                            (not= :joe (:owner %)))
                      items)]
    (doseq [r ready]
      (let [s (slurp path)
            hdr (str "{:id " (:id r) " :class " (:class r) " :status :blocked")
            _ (when-not (str/includes? s hdr) (throw (ex-info "row header not found" {:id (:id r)})))
            s2 (str/replace-first s hdr (str "{:id " (:id r) " :class " (:class r) " :status :open :unblocked-by \"wm-build-loop: depends-on all :done\""))]
        (spit path s2)
        (println "unblocked" (name (:id r))))))
  (do (println "usage: build_step.bb next-open|unblock|unreviewed|registry-held|counts") (System/exit 2)))
