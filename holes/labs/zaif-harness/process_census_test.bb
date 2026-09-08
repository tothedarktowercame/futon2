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
;; TREE GUARD. This suite reads the same working tree the census does, and this
;; checkout is shared with live build loops. Asserting pass or fail over files
;; another seat is mid-edit produces a verdict about nothing -- a half-written
;; file is indistinguishable from a rotted pointer, which is exactly the defect
;; that put the tree guard into process_census.bb in the first place.
;;
;; So the suite REFUSES TO RUN rather than guessing, and exits 3: NOT pass (0),
;; NOT fail (1). A silent SKIP at exit 0 would let a runner read "green" off work
;; that never happened, which is the failure this board calls
;; success-must-not-resemble-failure.
(let [{:keys [exit]} (shell/sh "bb" script "--edn")]
  (when (= 5 exit)
    (println "NOT RUN -- the read scope has uncommitted modifications.")
    (println "The census refuses over a moving tree (exit 5) and so does its suite.")
    (println "Re-run when the tree is clean; this is exit 3: neither pass nor fail.")
    (let [{:keys [err]} (shell/sh "bb" script)]
      (println) (println (str/trim (str err))))
    (System/exit 3)))


;; ---------------------------------------------------------------------------
(println "1. the census as it stands -- back to EXIT 0, and how it got there matters")
;; FOURTH STATE OF THIS SUITE. It has pinned, in order: ALIGN's 42 with a
;; drifted [E-T]; the repaired 42; the v2 widening and its exit-4; and now the
;; census AFTER PA6z-PA10z built real boundaries. :exists went 6 -> 14 because
;; nine cells were EARNED, not because the check was loosened -- every one is
;; adjudicated in ALIGN (2a495a7d, e53f1e26) after claude-1 re-read the source
;; for meaning, not merely for pointer resolution.
;;
;; The previous state of this suite pinned EXIT 3 on purpose, because [E-T-S]
;; was stale AND unbacked and had to stay uncredited until its owner ruled.
;; It now pins exit 0 -- but NOT because the pointer was quietly re-lifted.
;; claude-1 verified the channel at HEAD and adjudicated the cell afresh
;; (1b575c71), recording the unauthorised interval in ALIGN FIRST so nothing was
;; laundered. Green here is earned; green would have been available two turns
;; earlier by re-pointing a line number, and that is exactly the move this lane
;; refused.
(let [v1 (run-census "--pattern" "v1")
      v2 (run-census "--pattern" "v2")]
  (check "v1 and v2 agree cell for cell"
         (mapv (juxt :node :cell :verdict) (:results (:data v1)))
         (mapv (juxt :node :cell :verdict) (:results (:data v2))))
  (check "tally" {:absent 26 :exists 16}
         (frequencies (map :verdict (:results (:data v2)))))
  (check "no disagreement with the census of record" [] (:disagreements (:data v2)))
  (check "exits 0 -- the stale cell was adjudicated at source, not re-pointed" 0 (:exit v2)))

(println "\n2. [E-T-S] -- credited now, with its governance history pinned so it cannot be forgotten")
;; This cell was, for a while, the instrument certifying itself: PA9z's work seat
;; wrote a pinned adjudication crediting TRACE :surfaced straight into
;; census-ledger.edn with NO ALIGN counterpart, and it stood from dfe4dcfd until
;; PA15z's sweep found it. claude-1 then verified the channel at HEAD and ruled
;; the cell EARNS the credit -- by fresh adjudication, with the unauthorised
;; interval recorded in ALIGN first.
;;
;; So the check asserts the AUTHORITY, not just the verdict. A future edit that
;; re-credited this cell without an ALIGN citation would restore the exact fault,
;; and a verdict-only assertion would sail straight past it. :PA16z makes this
;; mechanical for every tag; until then, this pin is the guard for the one tag
;; that actually got caught.
(let [{:keys [data]} (run-census "--pattern" "v2")
      c (cell data "TRACE" :surfaced)]
  (check "credited" :exists (:verdict c))
  (check "tag" "[E-T-S]" (:tag c))
  (check "cites ALIGN's fresh adjudication, not a restated argument" true
         (boolean (re-find #"1b575c71" (str (:authority c)))))
  (check "both content anchors land at the captured live spans"
         [[212 236 true] [278 287 true]]
         (mapv (juxt :resolved-from :resolved-to :ok?) (:pointers c)))
  ;; ALIGN qualifies the credit as AGGREGATE-grain; the qualification travels
  ;; with the cell so a later reader cannot over-read it as per-record surfacing.
  (check "the aggregate-grain qualification travels with the credit" true
         (boolean (re-find #"(?i)aggregate-grain" (str (:qualification c))))))

(println "\n3. live pin -- an :exists cell whose pointers moved and were followed")
;; R16 :dispatched was pinned at :755-767/:2762-2775 when this suite was
;; written. PA10z edited full_loop_runner.clj and the pointers moved; the ledger
;; followed them. Re-pinned at the new lines so the NEXT move is caught too.
(let [{:keys [data]} (run-census "--pattern" "v2")
      c (cell data "R16" :dispatched)]
  (check "verdict" :exists (:verdict c))
  (check "tag" "[E-16-D]" (:tag c))
  (check "both captured live spans still land"
         [[803 815 true] [2819 2832 true]]
         (mapv (juxt :resolved-from :resolved-to :ok?) (:pointers c))))

(println "\n3b. live drift -- ONE span moved from an authorised baseline, and it is real")
;; AMENDED BY :PA18z. This check used to assert THREE moved spans, against live
;; records whose declared addresses were stale or -- for [E-16-C]/[E-16-K] --
;; DISOWNED by ALIGN 0b9535d3 as "never this document's citations". Their
;; :line-shift of 14 and 13 was measured from a baseline that never existed.
;; The pins are now re-lifted to their authorised addresses, so those two sit in
;; place and the fabricated shifts are gone.
;;
;; The moved-span REGRESSION now lives in planted control 4d, which is where it
;; belongs; this check asserts only what is genuinely true of the live tree. The
;; ordering was ruled and mattered: 4d landed FIRST, so re-lifting the pins could
;; not turn the suite red and be misread as the repairer's mistake.
;;
;; What remains is one HONEST drift. [E-T] declares ALIGN's corrected :6790 and
;; the span now sits at :6794 -- it moved AGAIN while PA17z was under review.
;; That is drift from an authorised citation, reported rather than suppressed,
;; and it is the single best argument for why live records were never the right
;; home for regression coverage.
(let [{:keys [exit data]} (run-census "--pattern" "v2")
      moved (->> (:results data) (mapcat #(map (fn [p] [(:tag %) p]) (:pointers %)))
                 (filter (fn [[_ p]] (= :span-moved (:observation p)))) vec)]
  (check "a moved-but-unchanged span is an observation, not a refusal" 0 exit)
  (check "exactly one live drift, and it names its authorised origin"
         [["[E-T]" 6790 6794 4]]
         (mapv (fn [[tag p]] [tag (:from p) (:resolved-from p) (:line-shift p)]) moved))
  ;; The two formerly-disowned pins must now be IN PLACE. Asserting the absence
  ;; of a shift is the point: a reappearing :line-shift here means someone has
  ;; put a fabricated baseline back.
  (check "the two re-lifted pins sit in place, no fabricated shift"
         [nil nil]
         [(:observation (first (filter #(= "dispatch!" (:expect %))
                                       (:pointers (cell data "R16" :dispatched)))))
          (:observation (first (filter #(= ":approve" (:expect %))
                                       (:pointers (cell data "R16" :checked)))))]))

(println "\n4. the route-hop adjudications still dispose of what v2 sees")
(let [{:keys [data]} (run-census "--pattern" "v2")
      rh (filterv #(= :route-hop-adjudicated (:basis %)) (:results data))]
  (check "15 cells disposed of by route-hop reading" 15 (count rh))
  (check "across the three route-tagged nodes" {"R12" 5 "R20" 5 "TRACE" 5}
         (frequencies (map :node rh)))
  (check "every one cites ALIGN's amendment" #{true}
         (set (map #(boolean (re-find #"d232ea06" (str (:authority %)))) rh))))

(println "\n4b. PA15z MECHANISM (a) -- the PAIRED control, both directions")
;; PA15z exists because the node-link search establishes NODE linkage while the
;; census reads CELL verdicts. The obvious fix -- silence every unclaimed cell
;; of a linked node -- would blind the instrument to the next boundary anyone
;; builds, which is PA2z's failure with the sign flipped. So the mechanism
;; matches by SITE, and the control has to prove BOTH directions or it proves
;; nothing.
(let [{:keys [data]} (run-census "--pattern" "v2")]
  ;; (i) SILENCES THE KNOWN: R10's three sites are declared as serving
  ;; commissioned+dispatched, so the five cells no adjudication claims become
  ;; decidable-as-absent instead of jamming the census at exit 4.
  (check "R10's five unclaimed cells resolve as absent"
         {[:parked :absent] true [:returned :absent] true [:checked :absent] true
          [:recorded :absent] true [:surfaced :absent] true}
         (into {} (for [k [:parked :returned :checked :recorded :surfaced]]
                    [[k (:verdict (cell data "R10" k))] true])))
  (check "and say WHY, not merely that" #{:serves-cells-accounted}
         (set (map #(:basis (cell data "R10" %)) [:parked :returned :checked :recorded :surfaced])))
  ;; (ii) DOES NOT SILENCE THE CREDITED: the same declaration must leave the two
  ;; earned cells standing.
  (check "the two earned R10 cells still credited"
         [[:exists "[E-10-C]"] [:exists "[E-10-D]"]]
         [((juxt :verdict :tag) (cell data "R10" :commissioned))
          ((juxt :verdict :tag) (cell data "R10" :dispatched))]))

(println "\n4c. PLANTED CONTROL -- the mechanism must NOT have gone blind")
;; The failure this guards is the one that would look identical from outside:
;; a mechanism that silences by NODE rather than by SITE would produce the same
;; green above while hiding every future boundary. Plant a declaration missing
;; ONE of R10's three sites; the hit at that site is then unaccounted-for
;; conduct, and the five cells must go straight back to needing adjudication.
(let [led (edn/read-string (slurp (io/file here "census-ledger.edn")))
      planted (update led :serves-cells-declarations
                      (fn [ds] (mapv (fn [d]
                                       (if (= "R10" (:node d))
                                         (update d :sites #(vec (remove (fn [s] (str/includes? s ":121")) %)))
                                         d))
                                     ds)))
      tmp (io/file (System/getProperty "java.io.tmpdir") "pa15z-planted-sites.edn")]
  (spit tmp (pr-str planted))
  (let [{:keys [data]} (run-census "--pattern" "v2" "--ledger" (str tmp))]
    (check "one undeclared site is enough to re-open all five cells"
           #{:hit-needs-adjudication}
           (set (map #(:verdict (cell data "R10" %))
                     [:parked :returned :checked :recorded :surfaced])))
    (check "the two earned cells are unaffected -- adjudication still wins"
           #{:exists}
           (set (map #(:verdict (cell data "R10" %)) [:commissioned :dispatched]))))
  (.delete tmp))

(println "\n4d. PLANTED CONTROL -- a span that MOVED but did not CHANGE must satisfy and name the move")
;; :PA18z. Before this control existed, the ONLY exercise of the moved-span path
;; was check 3b, asserting against three LIVE records whose declared addresses
;; were stale or disowned -- [E-16-C]/[E-16-K] carried the two addresses ALIGN
;; 0b9535d3 states "were never this document's citations". That made the suite
;; DEFEND the defect: re-lifting those pins to the authorised addresses would
;; zero the shifts and turn check 3b red, so whoever tried the correct repair
;; next would read the red as their own mistake.
;;
;; The regression value moves here, where it belongs. A planted span is moved by
;; a known number of lines without altering a byte of it; the pointer must still
;; satisfy, and must NAME the old and new location. Live records were the wrong
;; home for this coverage in any case -- [E-T] moved AGAIN (:6790 -> :6794) while
;; PA17z was being reviewed, which is the argument in one line.
(let [scratch (io/file here "runs" "pa18z-planted-span.clj")
      body ["(ns planted.span)" ";; filler" "(defn planted-target []" "  :planted-token-xyz)" ";; tail"]
      span-from 3 span-to 4
      norm (fn [lines from to]
             (str (str/join "\n" (map str/trimr (subvec (vec lines) (dec from) to))) "\n"))
      sha (fn [t] (format "%064x" (BigInteger. 1 (.digest (java.security.MessageDigest/getInstance "SHA-256")
                                                          (.getBytes t "UTF-8")))))
      rel "futon2/holes/labs/zaif-harness/runs/pa18z-planted-span.clj"
      led (edn/read-string (slurp (io/file here "census-ledger.edn")))
      mk (fn [] (assoc led
                       :nodes ["PLANTED"] :cells [:recorded]
                       :adjudicated [{:node "PLANTED" :cell :recorded :verdict :exists :tag "[P]"
                                      :pointers [{:file rel :from span-from :to span-to
                                                  :expect "planted-target"
                                                  :span-sha (sha (norm body span-from span-to))}]}]))
      tmp (io/file (System/getProperty "java.io.tmpdir") "pa18z-planted-move.edn")
      run! (fn [] (let [{:keys [data]} (run-census "--ledger" (str tmp) "--node" "PLANTED")]
                    (first (:pointers (cell data "PLANTED" :recorded)))))]
  (spit tmp (pr-str (mk)))
  (spit scratch (str (str/join "\n" body) "\n"))
  (let [p (run!)]
    (check "unmoved span satisfies with no observation" [true nil] [(:ok? p) (:observation p)])
    (check "and resolves where it was declared" [3 4] [(:resolved-from p) (:resolved-to p)]))
  ;; Move it three lines down. Not one byte of the span changes.
  (spit scratch (str (str/join "\n" (concat ["" "" ""] body)) "\n"))
  (let [p (run!)]
    (check "MOVED span still satisfies -- the reading was about the code, not the line"
           true (:ok? p))
    (check "and names the move with old and new location"
           [:span-moved 3 6 7] [(:observation p) (:line-shift p) (:resolved-from p) (:resolved-to p)]))
  ;; Now change one byte inside it. This must refuse even though it has not moved.
  (spit scratch (str (str/join "\n" (concat ["" "" ""] (assoc (vec body) 3 "  :planted-token-CHANGED)"))) "\n"))
  (let [p (run!)]
    (check "CHANGED content refuses -- moved-but-identical and changed are distinguished"
           false (:ok? p)))
  (.delete scratch)
  (.delete tmp))

(println "\n5. PLANTED CONTROL -- changed content must refuse")
(let [led (edn/read-string (slurp (io/file here "census-ledger.edn")))
      planted (update led :adjudicated
                      (fn [as] (mapv (fn [a]
                                       (if (and (= "R16" (:node a)) (= :dispatched (:cell a)))
                                         (assoc-in a [:pointers 0 :span-sha]
                                                   (apply str (repeat 64 "0")))
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
  (let [{:keys [exit out]} (shell/sh "bb" script "--edn" "--pattern" "v2"
                                      "--node" "PLANTED-NODE"
                                      "--ledger" (str tmp))]
    (check "exits 2 (error), NOT 0 with a page of :absent" 2 exit)
    (check "produced no verdicts at all" true (str/blank? out)))
  (.delete tmp))

;; ---------------------------------------------------------------------------
(println "\n7. (removed) the old v1/v2-parity and route-hop-count checks")
;; Checks 7 and 8 of the previous state pinned {:exists 6} with both versions at
;; exit 0 and 19 route-hop cells. All three numbers were correct then and are
;; wrong now, for the same reason: PA6z-PA10z built boundaries, ALIGN absorbed
;; them, and R20 :checked was PROMOTED out of :named-only -- the first such
;; promotion on the board, which is the transition this whole track exists to
;; cause. Their content is not lost: parity, the tally and the route-hop
;; disposition are all asserted in checks 1 and 4 against current reality.
;; Recorded as a removal rather than silently deleted, because "the check that
;; used to pass no longer applies" is exactly the claim that needs a reason
;; attached to it.

(println)
(if (seq @failures)
  (do (println "FAILED:" (count @failures) "--" (str/join ", " @failures)) (System/exit 1))
  (do (println "All acceptance checks passed.") (System/exit 0)))
