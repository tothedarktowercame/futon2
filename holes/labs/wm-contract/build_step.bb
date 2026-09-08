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
;; Priority: FUNDAMENTALS first and exclusively -- while any :F row is open
;; the loop may not take other work (Joe, 2026-09-05: the missing Q(o|pi)
;; constructor "should be a major finding and a priority focus, the Lean model
;; should be saying stop other work until this is solved"). Then rows the loop
;; itself needs (a run lock before any run), then RUN rows, then ledger order.
(defn prio [i] [(if (= :F (:class i)) 0 1)
                (case (:id i) :RUN12 0 :RUN11 1 2)
                (if (= :RUN (:class i)) 0 1)])
(def cmd (first *command-line-args*))
(case cmd
  ;; stall-key: id + status + a hash of the WHOLE next-open row, so a
  ;; one-slice-per-invocation row that is COMMITTING slices does not read as
  ;; stalled. This was a select-keys list and it lost the same race three
  ;; times, once per key it named: L5 recorded into :progress/:evidence
  ;; (2026-09-01 20:34), F12 into :slice-b2a/b (2026-09-07 00:44), F12 again
  ;; into :refused (2026-09-08 01:46) -- three healthy reviewed slices, loop
  ;; stopped anyway. Any edit to the row is evidence of life; a genuinely
  ;; spinning row hashes identical either way.
  "stall-key" (println (or (some->> (first (sort-by prio (filter loopable? items)))
                                    ((fn [i] (str (name (:id i)) ":" (name (:status i)) ":" (hash i)))))
                           "NONE"))
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
