#!/usr/bin/env bb
;; process_census_test.bb -- acceptance for :PA1z.
;;
;; LIVE-PIN RULE (zaif-harness worklist.edn header): every pinned value below is
;; CAPTURED VERBATIM from process_census.bb's own run of 2026-09-06, not authored
;; by hand. The R16/:dispatched and R10/:checked records were lifted from
;; `./process_census.bb --edn` and are reproduced here field for field.
;;
;; The two planted controls matter more than the pins. A harness that reports
;; :absent is only worth reading if it can be shown to refuse -- so control 1
;; corrupts a pinned pointer and requires the stale refusal, and control 2
;; breaks the search scope and requires the run to die rather than report
;; absence. Reading a tool failure as an absence is how a census invents
;; evidence, and nothing in the output would have looked wrong.

(require '[clojure.edn :as edn] '[clojure.java.shell :as shell]
         '[clojure.java.io :as io] '[clojure.string :as str])

(def here (.getParentFile (.getAbsoluteFile (io/file *file*))))
(def script (str (io/file here "process_census.bb")))
(def failures (atom []))

(defn check [label expected actual]
  (if (= expected actual)
    (println "  PASS" label)
    (do (println "  FAIL" label "\n    expected:" (pr-str expected) "\n    actual:  " (pr-str actual))
        (swap! failures conj label))))

(defn run-census [& extra]
  (let [{:keys [exit out]} (apply shell/sh "bb" script "--edn" extra)]
    {:exit exit :data (when-not (str/blank? out) (edn/read-string out))}))

(defn cell [data node c]
  (first (filter #(and (= node (:node %)) (= c (:cell %))) (:results data))))

(println "process_census_test -- :PA1z acceptance")
(println)

;; ---------------------------------------------------------------------------
(println "1. negative control over ALIGN's 42 censused cells, under PATTERN v1")
;; v1 is pinned EXPLICITLY: it is what ALIGN's 42 were measured under, so it stays
;; the comparability baseline even though the ledger now defaults to v2.
(let [{:keys [exit data]} (run-census "--pattern" "v1")
      tally (frequencies (map :verdict (:results data)))]
  (check "42 cells derived" 42 (count (:results data)))
  ;; ALIGN's matrix, reproduced: 34 absent, 5 exists, 2 named-only, and the one
  ;; cell whose pointer has drifted since 2026-09-05.
  ;; 2026-09-06, SECOND STATE OF THIS CHECK. It first pinned
  ;; {:absent 34 :exists 5 :named-only 2 :stale-adjudication 1} with exit 3 --
  ;; the drifted [E-T] pointer. claude-1 (ALIGN's author) then corrected the
  ;; citation (futon2 4c400a62) and the ledger re-lifted it, so the census is
  ;; now whole. The expected values move because THE WORLD MOVED, and the git
  ;; history of this file is the record of that; what must NOT happen is this
  ;; check being loosened so that both states pass. Controls 5 and 6 below are
  ;; what keep the refusal proven now that no live cell is stale.
  (check "tally matches ALIGN" {:absent 34 :exists 6 :named-only 2} tally)
  (check "no disagreement with the census of record" [] (:disagreements data))
  (check "no stale adjudications" [] (:stale data))
  (check "exits 0 (whole census, nothing refused)" 0 exit))

;; ---------------------------------------------------------------------------
(println "\n2. live pin -- an :exists cell (R16 dispatched, [E-16-D])")
(let [{:keys [data]} (run-census "--pattern" "v1")
      c (cell data "R16" :dispatched)]
  (check "verdict" :exists (:verdict c))
  (check "basis" :pinned-adjudication (:basis c))
  (check "tag" "[E-16-D]" (:tag c))
  ;; Pointer quoted verbatim from the harness run, per :PA1z acceptance.
  (check "pointers all landed"
         [{:ok? true :file "futon2/src/futon2/aif/full_loop_runner.clj" :from 755 :to 767 :expect "dispatch!"}
          {:ok? true :file "futon2/src/futon2/aif/full_loop_runner.clj" :from 2762 :to 2775 :expect "checkpoint"}]
         (:pointers c)))

;; ---------------------------------------------------------------------------
(println "\n3. live pin -- an :absent cell (R10 checked)")
(let [{:keys [data]} (run-census "--pattern" "v1")
      c (cell data "R10" :checked)]
  (check "verdict" :absent (:verdict c))
  (check "basis" :node-link-search (:basis c))
  (check "zero hits" 0 (:hits c))
  (check "enumeration declared untruncated" true (:untruncated c))
  (check "command quoted verbatim"
         "rg -n '(:node|:wm/node|:route/node|:control-node)[[:space:]]+:R10' futon3c/src/futon3c/agency futon3c/src/futon3c/social futon3c/src/futon3c/transport"
         (:command c)))

;; ---------------------------------------------------------------------------
(println "\n4. live pin -- the repaired cell (TRACE recorded, [E-T])")
(let [{:keys [data]} (run-census "--pattern" "v1")
      c (cell data "TRACE" :recorded)]
  ;; This cell is the harness's first real catch: its war_machine.clj citation
  ;; had drifted 6750-6759 -> 6789 under the wm loop's edits, the harness refused
  ;; to credit it, ALIGN's author corrected the document, and the ledger followed.
  ;; Pinning the corrected pointer here means a RE-drift is caught again rather
  ;; than being absorbed as normal.
  (check "credited again after the ALIGN correction" :exists (:verdict c))
  (check "basis" :pinned-adjudication (:basis c))
  (check "the re-lifted pointer, and the untouched second half"
         [{:ok? true :file "futon2/scripts/futon2/report/war_machine.clj" :from 6789 :to 6789 :expect ":TRACE"}
          {:ok? true :file "futon2/src/futon2/aif/trace.clj" :from 723 :to 745 :expect "write-trace!"}]
         (:pointers c)))

(println "\n5. PLANTED CONTROL -- a corrupted pointer must refuse")
(let [led (edn/read-string (slurp (io/file here "census-ledger.edn")))
      planted (update led :adjudicated
                      (fn [as] (mapv (fn [a]
                                       (if (and (= "R16" (:node a)) (= :dispatched (:cell a)))
                                         (assoc-in a [:pointers 0 :expect] "this-token-is-not-in-that-range")
                                         a))
                                     as)))
      tmp (io/file (System/getProperty "java.io.tmpdir") "pa1z-planted-ledger.edn")]
  (spit tmp (pr-str planted))
  (let [{:keys [exit data]} (run-census "--ledger" (str tmp))
        c (cell data "R16" :dispatched)]
    (check "planted cell is NOT credited" :stale-adjudication (:verdict c))
    (check "its declared verdict is preserved for the report" :exists (:declared c))
    (check "run exits 3" 3 exit))
  (.delete tmp))

;; ---------------------------------------------------------------------------
(println "\n6. PLANTED CONTROL -- a broken scope must ERROR, never read as absence")
(let [led (edn/read-string (slurp (io/file here "census-ledger.edn")))
      ;; Break EVERY scope key, not just v1's: the ledger now defaults to v2,
      ;; which reads :v2-node-link. Planting into :node-link alone left the
      ;; active search intact and this control silently passed nothing -- caught
      ;; by the control itself when v2 landed, which is the argument for having
      ;; it. A control that only guards the version you were thinking about is
      ;; not a control.
      planted (-> led
                  (assoc-in [:scope :node-link] ["futon3c/src/futon3c/no-such-directory"])
                  (assoc-in [:scope :v2-node-link] ["futon3c/src/futon3c/no-such-directory"]))
      tmp (io/file (System/getProperty "java.io.tmpdir") "pa1z-planted-scope.edn")]
  (spit tmp (pr-str planted))
  (let [{:keys [exit out]} (shell/sh "bb" script "--edn" "--ledger" (str tmp))]
    (check "exits 2 (error), NOT 0 with a page of :absent" 2 exit)
    (check "produced no verdicts at all" true (str/blank? out)))
  (.delete tmp))

;; ---------------------------------------------------------------------------
(println "\n7. PATTERN v2 -- widened, adjudicated, and now agreeing with v1")
;; THIRD STATE OF THIS CHECK, and the history is the point. claude-1 ruled the
;; pattern be widened and versioned rather than compensated for by hand. The v2
;; negative control then came back EXIT 4 with 19 disagreements on R12/R20/TRACE
;; -- not the clean diff anyone expected. Nothing was absorbed: no verdict had
;; reversed, but the stated basis for those cells (ALIGN's [A] "returned no
;; matches") was falsified, so it was routed as an exit-4. claude-1 adjudicated
;; it in ALIGN (d232ea06) and this ledger now cites that amendment.
;;
;; So v2 reaching parity with v1 is EARNED, not assumed: the instrument was
;; widened until it saw more, the extra sightings were adjudicated by the
;; document's author, and only then did the two agree. Narrowing v2 back would
;; have produced the same green with none of the knowledge.
(let [v1 (run-census "--pattern" "v1")
      v2 (run-census "--pattern" "v2")
      tally (fn [r] (frequencies (map :verdict (:results (:data r)))))]
  (check "v1 still exits 0" 0 (:exit v1))
  (check "v2 exits 0 after the adjudications" 0 (:exit v2))
  (check "both versions agree cell for cell"
         (mapv (juxt :node :cell :verdict) (:results (:data v1)))
         (mapv (juxt :node :cell :verdict) (:results (:data v2))))
  (check "and on the tally" {:absent 34 :exists 6 :named-only 2} (tally v2))
  (check "no disagreements left under v2" [] (:disagreements (:data v2))))

(println "\n8. the 19 are adjudicated, NOT re-hidden")
;; The distinction this check exists to protect: v2 must still SEE the route-hop
;; hits and dispose of them by a cited adjudication. If a later edit narrowed the
;; pattern or the scope instead, the tally above would still be green while the
;; instrument had gone blind again -- so assert the basis, not just the verdict.
(let [{:keys [data]} (run-census "--pattern" "v2")
      adjudicated (filterv #(= :route-hop-adjudicated (:basis %)) (:results data))]
  (check "19 cells disposed of by the route-hop adjudication" 19 (count adjudicated))
  (check "confined to the three nodes" {"R12" 7 "R20" 6 "TRACE" 6}
         (frequencies (map :node adjudicated)))
  (check "every one cites ALIGN's amendment, not a restated argument" #{true}
         (set (map #(boolean (re-find #"d232ea06" (str (:authority %)))) adjudicated)))
  ;; The two cells the node-wide entry must NOT have swallowed.
  (check "TRACE recorded still credited by [E-T]" [:exists :pinned-adjudication]
         ((juxt :verdict :basis) (cell data "TRACE" :recorded)))
  (check "R20 checked still [N20] named-only" [:named-only :pinned-adjudication]
         ((juxt :verdict :basis) (cell data "R20" :checked))))

(println)
(if (seq @failures)
  (do (println "FAILED:" (count @failures) "--" (str/join ", " @failures)) (System/exit 1))
  (do (println "All acceptance checks passed.") (System/exit 0)))
