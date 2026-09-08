#!/usr/bin/env bb
;; zaif_step.bb -- the ledger queries zaif-build-loop.sh needs, so the loop
;; holds no ledger logic of its own. Adapted from wm-contract/build_step.bb.
;; Subcommands:
;;   next-open      id of the next row the loop may work (or NONE)
;;   unblock        turn :blocked rows whose :depends-on are all :done into :open (commits nothing)
;;   unreviewed     ids of :done-unreviewed rows in THIS lane
;;   counts         status frequencies (whole board)
;;   stall-key      id:status:progress-hash of next-open, for the loop's stall check
;;
;; Loop-eligible row: :status :open, :class not :J, :owner not :joe, no
;; :loop-skip, and :lane starting "claude-2". S-rows carry no :lane -- they are
;; claude-1's and this loop never takes or reviews them. Priority: :D* defect
;; rows before :U* rows, ledger order within each.
(require '[clojure.edn :as edn] '[clojure.string :as str])
(def path (str (.getParent (.getAbsoluteFile (java.io.File. *file*))) "/worklist.edn"))
(def w (edn/read-string (slurp path)))
(def items (:items w))
(def by-id (into {} (map (juxt :id identity) items)))
(defn mine? [i] (str/starts-with? (str (:lane i)) "claude-2"))
(defn loopable? [i]
  (and (= :open (:status i))
       (mine? i)
       (not= :J (:class i))
       (not= :joe (:owner i))
       (not (:loop-skip i))))
(defn prio [i] (if (str/starts-with? (name (:id i)) "D") 0 1))
(def cmd (first *command-line-args*))
(case cmd
  "next-open" (println (or (some-> (first (sort-by prio (filter loopable? items))) :id name) "NONE"))
  "stall-key" (println (or (some->> (first (sort-by prio (filter loopable? items)))
                                    ((fn [i] (str (name (:id i)) ":" (name (:status i)) ":"
                                                  (hash (select-keys i [:progress :evidence]))))))
                           "NONE"))
  "unreviewed" (println (str/join " " (map (comp name :id)
                                           (filter #(and (= :done-unreviewed (:status %)) (mine? %))
                                                   items))))
  "counts" (println (frequencies (map :status items)))
  "unblock"
  (let [ready (filter #(and (= :blocked (:status %)) (mine? %) (seq (:depends-on %))
                            (every? (fn [d] (= :done (:status (by-id d)))) (:depends-on %))
                            (not= :joe (:owner %)))
                      items)]
    (doseq [r ready]
      (let [s (slurp path)
            hdr (str "{:id " (:id r) " :class " (:class r) " :status :blocked")
            _ (when-not (str/includes? s hdr) (throw (ex-info "row header not found" {:id (:id r)})))
            ;; Only MINT :unblocked-by if the row does not already carry one.
            ;; This is textual surgery on EDN, so an unconditional insert
            ;; produces a DUPLICATE KEY on any row that already declares the
            ;; field -- and EDN refuses a map with duplicate keys, so the whole
            ;; ledger stops parsing and the loop halts with "ledger invalid
            ;; after work". That happened on 2026-09-08 to PA5z-PA10z, whose
            ;; :unblocked-by had been hand-written when the rows were seeded.
            ;; The provenance is the loop's to write, but the guard belongs
            ;; here: a tool that corrupts the file it edits when its input is
            ;; merely unexpected is not safe to run unattended.
            s2 (str/replace-first s hdr (str "{:id " (:id r) " :class " (:class r) " :status :open"
                                             (when-not (:unblocked-by r)
                                               " :unblocked-by \"zaif-build-loop: depends-on all :done\"")))]
        (spit path s2)
        (println "unblocked" (name (:id r))))))
  (do (println "usage: zaif_step.bb next-open|unblock|unreviewed|counts|stall-key") (System/exit 2)))
