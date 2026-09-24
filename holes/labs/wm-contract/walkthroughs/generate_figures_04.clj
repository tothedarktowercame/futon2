;; generate_figures_04.clj — regenerate every figure in 04-learning-and-B.md
;; from the records. Run from the futon2 checkout root:
;;
;;   clojure -M -i holes/labs/wm-contract/walkthroughs/generate_figures_04.clj \
;;           -e "(generate-figures-04/generate!)"
;;
;; Read-only: reads the learning-trial ledger, two closes, the tick record,
;; the two B-C test fixtures, and (for two censuses) every 007-closed.edn
;; under data/wm-full-loop*; writes only SVGs beside this script. Every
;; figure file name carries the short raw sha256 of the record(s) it was
;; drawn from. Where a figure shows what the code computes on a record — the
;; judge's read (pattern-theta) on today's ledger, the concentration carrier
;; on the fixture extract and on the live population, the dedup layers, the
;; X5 refusals — it calls futon2.aif.learning-trial-ledger in this fresh
;; process on the rows the records already hold. Nothing is written back to
;; any record; record! is never called.
(ns generate-figures-04
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.learning-trial-ledger :as ledger])
  (:import [java.nio.file Files]
           [java.security MessageDigest]))

(def here "holes/labs/wm-contract/walkthroughs")
(def ledger-path "data/wm-learning-trials/attempts.edn")
(def close-71 "data/wm-full-loop-machinery-71/wm-contract-machinery-71-v1/attempt-002/007-closed.edn")
(def close-76 "data/wm-full-loop-machinery-76/wm-contract-machinery-76-v1/attempt-002/007-closed.edn")
(def close-77 "data/wm-full-loop-machinery-77/wm-contract-machinery-77-v1/attempt-001/007-closed.edn")
(def tick "data/wm-runs/tick-run-record-2026-09-23-1790199409.edn")
(def fx-row "test/fixtures/b-update-carrier/8e7d1aaf.edn")
(def fx-close "test/fixtures/b-update-carrier/machinery-71-attempt-002-close-extract.edn")
(def fx-tick "test/fixtures/b-update-carrier/tick-run-record-1790199409-c1-extract.edn")
(def lean-file "/home/joe/code/mathlib4/DarkTower/WarMachine/DirichletLearning.lean")

(def family :apparatus/done-is-observed-running)
(def pinned-id "8e7d1aaf32d9ef2ead1702b304887c414298ce7ea5b48b0e51e42995963e1403")

(defn read-record [path]
  (edn/read-string {:default (fn [t v] (tagged-literal t v))} (slurp path)))

(defn- hex [bytes n]
  (apply str (map #(format "%02x" (bit-and 255 %)) (take n bytes))))

(defn file-sha256
  "Raw-byte SHA-256 of a file (the form the B-D review and the fixtures cite)."
  [path]
  (hex (.digest (MessageDigest/getInstance "SHA-256") (Files/readAllBytes (.toPath (io/file path)))) 32))

(defn sha8 [path] (subs (file-sha256 path) 0 8))

(defn spit-svg [name svg]
  (let [file (str here "/" name)]
    (spit file svg)
    (println "wrote" file)))

;; ---------------------------------------------------------------------------
;; svg helpers (same style as walkthroughs 01-03)
;; ---------------------------------------------------------------------------

(defn- esc [s]
  (-> (str s) (str/replace "&" "&amp;") (str/replace "<" "&lt;") (str/replace ">" "&gt;")))

(defn- htxt [x y s & {:keys [size anchor weight mono] :or {size 12 anchor "start"}}]
  (str "<text x=\"" x "\" y=\"" y "\" font-size=\"" size "\" text-anchor=\"" anchor "\""
       (when weight (str " font-weight=\"" weight "\""))
       " font-family=\"" (if mono "monospace" "system-ui,sans-serif") "\">" (esc s) "</text>"))

(defn- box [x y w h fill stroke & {:keys [dash]}]
  (str "<rect x=\"" x "\" y=\"" y "\" width=\"" w "\" height=\"" h "\" rx=\"5\""
       " fill=\"" fill "\" stroke=\"" stroke "\""
       (when dash (str " stroke-dasharray=\"" dash "\"")) "/>"))

(defn- arrow [x1 y1 x2 y2 & {:keys [dash label]}]
  (str "<path d=\"M" x1 "," y1 " L" x2 "," y2 "\" fill=\"none\" stroke=\"#68717b\""
       (when dash (str " stroke-dasharray=\"" dash "\""))
       " marker-end=\"url(#harrow)\"/>"
       (when label (htxt (/ (+ x1 x2) 2.0) (- (/ (+ y1 y2) 2.0) 4) label :size 10 :anchor "middle"))))

(defn- hsvg [width height body]
  (str "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" width "\" height=\"" height
       "\" viewBox=\"0 0 " width " " height "\">"
       "<defs><marker id=\"harrow\" markerWidth=\"8\" markerHeight=\"8\" refX=\"7\" refY=\"4\" orient=\"auto\">"
       "<path d=\"M0,0 L8,4 L0,8 Z\" fill=\"#68717b\"/></marker></defs>"
       "<rect width=\"" width "\" height=\"" height "\" fill=\"white\"/>"
       body "</svg>"))

(defn- lines
  "A run of monospace lines starting at (x, y), 16px apart; returns the svg and the next y."
  [x y ls & {:keys [size] :or {size 9.5}}]
  [(apply str (for [[i l] (map-indexed vector ls)] (htxt x (+ y (* 16 i)) l :mono true :size size)))
   (+ y (* 16 (count ls)))])

;; ---------------------------------------------------------------------------
;; Record extraction
;; ---------------------------------------------------------------------------

(def target "T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade")

(defn short-target [s] (str/replace (str s) target "T-repair-occ-444fb018…"))
(defn short-sha [s] (if (string? s) (subs s 0 (min 8 (count s))) (pr-str s)))
(defn short-id [s] (if (string? s) (str (subs s 0 8) "…") (pr-str s)))
(defn token-name [[t kw]] (str "[" (short-target t) " " kw "]"))

(defn judgment [r] (get-in r [:payload :judgment]))

(defn ledger-rows
  "read-trials on the ledger file itself (the 2-arity form reads a named file)."
  []
  (ledger/read-trials nil ledger-path))

(defn row-run [r] (get-in r [:row :trial :occurrence :run/id]))
(defn row-cohort [r] (get-in r [:row :trial :occurrence :cohort/id]))
(defn row-effect [r] (get-in r [:row :trial :effect]))
(defn row-occurrence [r] (get-in r [:row :trial :deduplication :inputs :occurrence]))

(defn theta-of
  "The theta pattern-theta's rule gives for s successes in n trials, exact."
  [s n]
  (/ (+ s 1/2) (+ n 1)))

(defn close-files []
  (ledger/close-files (ledger/default-close-roots "data/wm-learning-trials")))

(defn receipt-census
  "Every learning-trial-receipt trial on every close under data/wm-full-loop*:
   counts by [status reason ledger-status]. Read-only."
  [files]
  (let [trials (for [f files
                     :let [j (judgment (read-record (str f)))]
                     t (get-in j [:learning-trial-receipt :trials])]
                 [(:status t) (:reason t) (get-in t [:ledger :status])])]
    {:closes (count files)
     :with-receipt (count (filter #(get-in (judgment (read-record (str %))) [:learning-trial-receipt :trials]) files))
     :trials (count trials)
     :by (into (sorted-map-by #(compare (pr-str %1) (pr-str %2))) (frequencies trials))}))

;; ---------------------------------------------------------------------------
;; Figure 1 — one ledger row, and when it is written
;; ---------------------------------------------------------------------------

(defn fig1-row-and-write [rows c71]
  (let [r (first (filter #(= pinned-id (:identity %)) rows))
        e (:row r)
        t (:trial e)
        idx (.indexOf (mapv :identity rows) pinned-id)
        j (judgment c71)
        cmp (:token-outcome-comparison j)
        receipt-trial (get-in j [:learning-trial-receipt :trials 0])
        ai (:accepted-increment j)
        width 1240
        [row-svg y1] (lines 50 136
                            [(str ":schema " (:schema e) "   :mode " (:mode e) "   :identity " (short-id (:identity e)) "   :observed " (:observed e) "   :increment " (pr-str (:increment e)))
                             (str ":family " (short-id (:family e)) " (digest of the trial CONFIGURATION: target, cascade, patterns, effect, route — not the parameter key; theta-key docstring :98-107)")
                             (str ":meaning-sha256 " (short-id (:meaning-sha256 e)) "   :contract {:schema " (get-in e [:contract :schema]) " :mode " (get-in e [:contract :mode]) " :trial-grain " (get-in e [:contract :trial-grain]) "}")
                             (str "          :contract :does-not-establish " (pr-str (get-in e [:contract :does-not-establish])))
                             (str ":trial :effect " (token-name (:effect t)) "   :status " (:status t) "   :counted? " (:counted? t) "   :ledger " (pr-str (:ledger t)) "  (the banked copy predates the append: record! :57-61)")
                             (str ":trial :selected-cascade :id " (get-in t [:selected-cascade :id]) " :target " (get-in t [:selected-cascade :target]) "   :precedence " (pr-str (mapv :id (get-in t [:selected-cascade :precedence]))))
                             (str ":trial :deduplication :identity " (short-id (get-in t [:deduplication :identity])) " = digest of :inputs {:occurrence {:action/id " (get-in t [:deduplication :inputs :occurrence :action/id]))
                             (str "          :action/value-sha256 " (short-id (get-in t [:deduplication :inputs :occurrence :action/value-sha256])) " :transition/id " (get-in t [:deduplication :inputs :occurrence :transition/id]) "}")
                             (str "          :effect " (token-name (get-in t [:deduplication :inputs :effect])) " :grain " (get-in t [:deduplication :inputs :grain]) "}   (attempt_learning.clj:98-99, :119)")
                             (str ":trial :after-observation " (:after-observation t) "   :causal-attribution " (:causal-attribution t) "   :performed-step " (pr-str (:performed-step t)) "   :route " (:route t))
                             (str ":trial :signed-observation :consumption " (get-in t [:signed-observation :consumption]) "   :placement " (pr-str (dissoc (:placement t) :contract-sha256)))
                             (str ":trial :attempt-beta " (pr-str (dissoc (:attempt-beta t) :cumulative-posterior)))
                             (str "          — an illustrative Beta(" (get-in t [:attempt-beta :prior :alpha]) "," (get-in t [:attempt-beta :prior :beta]) ") summary from resources/wm/learning-trial-prior.edn (:authority :illustrative, :mode :record-only); nothing consumes it")
                             (str ":trial :shadow :scope " (get-in t [:shadow :scope]) "   :attempt-parameter-consumption " (get-in t [:shadow :attempt-parameter-consumption]))
                             (str "derived at read time, never stored (read-trials :146, theta-key :98-116, producer-of :77-96): :theta-key " (:theta-key r) "   :contract-version " (:contract-version r))])
        [order-svg y2] (lines 50 (+ y1 60)
                              [(str "1  retain-token-outcome! (full_loop_runner.clj:3131) re-reads the enactment file and calls compare-outcomes (:3142):  :status " (:status cmp) ", " (count (:tokens cmp)) " wanted rows —")
                               (str "     " (str/join ";  " (for [row (:tokens cmp)] (str (token-name (:token row)) " predicted " (:predicted row) " observed " (:observed row) " -> " (:verdict row)))))
                               (str "2  attempt-learning/receipt (:3156-3160) builds one trial per cascade/effect:  " (token-name (:effect receipt-trial)) " :status " (:status receipt-trial) " :reason " (pr-str (:reason receipt-trial)))
                               (str "3  learning-ledger/record! (:3161) appends it:  :counted? " (:counted? receipt-trial) "  :ledger " (pr-str (update (:ledger receipt-trial) :identity short-id)) "  -> ledger form " (inc idx) " of " (count rows))
                               (str "4  accepted-increment/evaluate-close (:4065, bound at :4011):  :accepted? " (:accepted? ai) "  :reason " (:reason ai) "  :message " (pr-str (:message ai)))
                               (str "5  007-closed.edn written, :recorded-at " (:recorded-at c71) ", :outcome " (:outcome j) ";  :b-update on the judgment? " (contains? j :b-update) " (the close predates 4a2ba931)")
                               (str "6  learning-ledger/b-update (:4244-4271) runs only when (true? :accepted?): not run.  It persists nothing in any case (b-update :565-570).")
                               (str "At HEAD a step 4½ exists: close-b-update (:4112-4126) snapshots the ledger onto the judgment as :b-update before the file is written. Not on this record.")])
        height (+ y2 70)]
    (hsvg
     width height
     (str
      (htxt 40 36 "One ledger row, and when it is written — form 3 of data/wm-learning-trials/attempts.edn and the close that wrote it" :size 15 :weight "bold")
      (htxt 40 58 (str "The ledger holds " (count rows) " forms, each :wm/attempt-learning-count-v1, appended by record! (learning_trial_ledger.clj:26-71) under an OS file lock.") :size 11)
      (htxt 40 76 (str "This one is identity " (short-id pinned-id) ", the trial of machinery-71 attempt-002: a whole-attempt outcome for one (occurrence, effect) — one boolean :observed, keyed by the dedup :identity,") :size 11)
      (htxt 40 94 "attributed at read time to the pattern that declared the effect." :size 11)
      (htxt 40 118 "(a) the banked event (keys as recorded; long shas shortened)" :weight "bold" :size 11)
      row-svg
      (htxt 40 (+ y1 34) (str "(b) the order of events at the close of machinery-71 attempt-002 (" close-71 ")") :weight "bold" :size 11)
      order-svg
      (htxt 40 (+ y2 10) "The append happens at step 3, inside the token comparison; the acceptance verdict is decided at step 4. This close's row was appended and its increment was refused: both are on the record." :size 10)
      (htxt 40 (+ y2 28) "(record! admits :admitted-at-attempt-grain trials only (:30, :45); the receipt's held trials — e.g. :effect-already-present — are held by attempt_learning.clj:76-97 before record! sees them.)" :size 10)
      (htxt 40 (+ y2 52) "Evidence only: every value is read from the ledger form or the close; the two derived fields are labelled as derived." :size 9)))))

;; ---------------------------------------------------------------------------
;; Figure 2 — how the judge reads B at selection
;; ---------------------------------------------------------------------------

(defn selection-timestamp
  "This click's live-selection occurrence-id, at
   [:decision :selection-certificate :token-belief-stage :occurrence-id]
   (the :prospective-prior's :occurrence-id one level down is the
   PREDECESSOR click's id, carried forward)."
  [t]
  (let [s (get-in t [:decision :selection-certificate :token-belief-stage :occurrence-id])]
    (subs (str s) (count "wm-live-selection-"))))

(defn stamped-patterns
  "Every precedence entry of the tick record's two candidates, with theta fields."
  [t]
  (for [[ci c] (map-indexed vector (get-in t [:decision :selection-certificate :candidates]))
        [pi p] (map-indexed vector (get-in c [:id :precedence]))]
    {:candidate (get-in c [:id :id]) :ci ci :pi pi :id (:id p)
     :theta (:theta p) :has-theta? (contains? p :theta)
     :source (:theta-source p) :prov (:theta-provenance p)}))

(defn fig2-judge-read [t rows ledger-mtime]
  (let [pats (stamped-patterns t)
        selected-at (selection-timestamp t)
        decision-keys (sort (keys (:decision t)))
        families (distinct (map :theta-key rows))
        today (into {} (for [f families] [f (ledger/pattern-theta f "data/wm-learning-trials")]))
        width 1240
        [stamp-svg y1] (lines 50 136
                              (apply concat
                                     (for [p pats]
                                       (if (:has-theta? p)
                                         [(str (name (:candidate p)) " precedence " (:pi p) "  " (:id p) "   :theta " (:theta p) "   :theta-source " (:source p))
                                          (str "      :theta-provenance {:trials-count " (get-in p [:prov :trials-count]) " :successes " (get-in p [:prov :successes])
                                               " :identities " (pr-str (mapv short-id (get-in p [:prov :identities]))) " :targets " (pr-str (mapv short-target (get-in p [:prov :targets])))
                                               " :unattributed-rows " (get-in p [:prov :unattributed-rows]) "}")]
                                         [(str (name (:candidate p)) " precedence " (:pi p) "  " (:id p) "   no :theta key on the record")
                                          "      (pattern-theta returned :no-recorded-trials, so the judge left the pattern as declared; with-pattern-theta gives it theta 1 :documented-default at kernel time)"]))))
        [rule-svg y2] (lines 50 (+ y1 44)
                             ["pattern-theta (learning_trial_ledger.clj:579-632): read-trials over the whole file (:606); keep rows whose derived :theta-key is this pattern id (:614); collapse by :identity, last row wins (:616);"
                              "theta = (successes + 1/2) / (n + 1) (:621); :identities sorted (:626); :targets = the distinct targets of the contributing rows (:627). A pattern with no rows: {:status :no-recorded-trials} (:620)."
                              (str "3/4 = (1 + 1/2)/(1 + 1) from one success;   1/4 = (0 + 1/2)/(1 + 1) from one failure;   1/16 = (0 + 1/2)/(7 + 1) from seven failures.   Checked here as exact ratios: "
                                   (= [3/4 1/4 1/16] [(theta-of 1 1) (theta-of 0 1) (theta-of 0 7)]))
                              "The judge (war_machine.clj:6375-6411) calls pattern-theta once per pattern id in every candidate's precedence (:6383) and stamps :theta, :theta-source :recorded-trials and the provenance"
                              "onto the pattern (:6394-6402) before scoring; :defaulted reads keep theta 1 with :theta-default-reason (:6408-6411)."
                              "Trials pool across targets (pattern-theta docstring :591-597): C1's 3/4 came from one attempt on M-f11-find-production-successor and is applied to T-repair-occ-444fb018…."])
        [rows-svg y3] (lines 50 (+ y2 44)
                             (cons (format "%-4s %-11s %-46s %-38s %-9s %-26s %s" "form" "identity" "theta-key (derived)" "effect" "observed" "run" "cohort")
                                   (for [[i r] (map-indexed vector rows)]
                                     (format "%-4s %-11s %-46s %-38s %-9s %-26s %s" (inc i) (short-id (:identity r)) (str (:theta-key r))
                                             (str (name (second (row-effect r))) " on " (short-target (first (row-effect r)))) (str (:observed r)) (str (row-run r)) (str (row-cohort r))))))
        [today-svg y4] (lines 50 (+ y3 44)
                              (for [f (sort-by str families)]
                                (let [pt (get today f)]
                                  (str (format "%-46s" (str f)) " :trials-count " (:trials-count pt) "  :successes " (:successes pt) "  :theta " (format "%-5s" (str (:theta pt)))
                                       "  :identities " (pr-str (mapv short-id (:identities pt)))))))
        height (+ y4 96)]
    (hsvg
     width height
     (str
      (htxt 40 36 "How the judge reads B at selection — tick-run-record-2026-09-23-1790199409 and the ledger it read" :size 15 :weight "bold")
      (htxt 40 58 (str "(a) [:decision :selection-certificate :candidates i :id :precedence j] on the record: every pattern of both candidates (" (count pats) " entries).") :size 11)
      (htxt 40 76 (str "    Live-selection occurrence-id timestamp " selected-at ". The record's [:decision] has " (count decision-keys) " keys: " (pr-str decision-keys) ".") :size 10)
      (htxt 40 94 "    war_machine.clj:6505-6514 at HEAD also attaches a :theta-consumption map to the decision; no such key is on this record (nor :certificate-schema, :6503)." :size 10)
      (htxt 40 112 "    What the record holds is listed here." :size 10)
      stamp-svg
      (htxt 40 (+ y1 20) "(b) the rule, as the code states it" :weight "bold" :size 11)
      rule-svg
      (htxt 40 (+ y2 20) (str "(c) the " (count rows) " ledger forms in append order (data/wm-learning-trials/attempts.edn), with the derived :theta-key each contributes to") :weight "bold" :size 11)
      rows-svg
      (htxt 40 (+ y3 20) "(d) NOT A RECORD VALUE: pattern-theta run here, on the ledger as it is today" :weight "bold" :size 11)
      today-svg
      (htxt 50 (+ y4 12) (str "The ledger's last append was form 12 (identity " (short-id (:identity (last rows))) ", machinery-76 attempt-002's close at 2026-09-23T21:44:04Z; file mtime " ledger-mtime "),") :size 10)
      (htxt 50 (+ y4 30) (str "after the judge's read at " selected-at ". So :apparatus/evidence-to-disposition-once reads " (:theta (get today :apparatus/evidence-to-disposition-once)) " today and read 1/4 on the record. Nothing on the record names which ledger state was read;") :size 10)
      (htxt 50 (+ y4 48) "the provenance's :identities is the only join (B-D §3, Revision 2 §R2.4: no :B-read, :read-at or version key on any record checked)." :size 10)
      (htxt 40 (+ y4 76) "Evidence only: (a)-(c) are read from the tick record and the ledger; (d) is computed here and labelled so." :size 9)))))

;; ---------------------------------------------------------------------------
;; Figure 3 — the three dedup layers
;; ---------------------------------------------------------------------------

(defn fig3-dedup [rows census]
  (let [mine (filter #(= family (:theta-key %)) rows)
        one (ledger/concentration-carrier family mine {:layer :none})
        doubled (ledger/concentration-carrier family (concat mine mine) {:layer :none})
        no-dedup (reduce (fn [acc r] (mapv #(mapv + %1 %2) acc (if (:observed r) [[1] [0]] [[0] [1]])))
                         [[1/2] [1/2]] (concat mine mine))
        occ (row-occurrence (first mine))
        upd (ledger/b-update {:family family :occurrence occ
                              :occurrence-identity "provenance-not-a-key"
                              :accepted-verdict {:accepted? true :observed true}
                              :ledger-root "data/wm-learning-trials"})
        width 1240
        layer (fn [y title code-lines key-line catches misses shown]
                (let [ls (concat code-lines [key-line] catches misses shown)
                      h (+ 34 (* 16 (count ls)))]
                  [(str (box 40 y (- width 80) h "#f6f7f8" "#68717b")
                        (htxt 50 (+ y 20) title :weight "bold")
                        (first (lines 50 (+ y 40) ls)))
                   (+ y h 14)]))
        [l1 y1] (layer 116 "Layer 1 — ledger identity, at append (record! :41-56)"
                       ["by-id = every existing form keyed by :identity (:41); meanings = every :family -> :meaning-sha256 (:42). For each admitted trial (:45):"]
                       "key: [:deduplication :identity] = digest of {occurrence(3 fields), effect, grain}; and :learning-family vs :meaning-sha256."
                       ["catches: same :learning-family, different :meaning-sha256 -> :revised-meaning (:50-51); same identity, different observation -> :conflicting-observation (:52); same identity -> :duplicate-replay (:53)."
                        "effect: the row is HELD (:55-56) — :counted? false, :ledger {:status :not-appended}; no form is written, so no concentration can move."]
                       ["does not catch: a second attempt on the same target and effect (a new occurrence is a new trial by design, occurrence-key docstring :201-202)."]
                       [(str "live instances in the corpus: " (:closes census) " closes scanned, " (:trials census) " receipt trials; [status reason ledger-status] counts: ")
                        (str "   " (str/join "   " (for [[k v] (:by census)] (str (pr-str k) " " v))))
                        "   No trial anywhere reads :ledger :status :not-appended: this layer has not fired on any recorded close. Its behaviour is pinned by the test three-dedup-layers-leave-conc-achieved-unchanged (record! on a temp ledger)."])
        [l2 y2] (layer y1 "Layer 2 — update occurrence, post-close (b-update :533-536, :550)"
                        ["b-update reads the ledger (:513), keeps this family's rows (:519), collapses by identity (:523), and asks whether the OCCURRENCE map is already among them (:533-536)."]
                        "key: the occurrence map {:action/id :action/value-sha256 :transition/id} compared directly to each row's [:trial :deduplication :inputs :occurrence];"
                        ["     :occurrence-identity (a commit sha) is provenance, not the key (:524-532)."
                         "catches: a b-update for an occurrence whose row record! already banked -> :status :already-recorded, trials'/successes' unchanged (:539, :548)."]
                        ["does not protect the judge: b-update persists nothing (:565-570) and runs only after a close with :accepted? true (full_loop_runner.clj:4244-4246); pattern-theta never calls it."]
                        [(str "run here against the live ledger with form 3's occurrence and a hand-built accepted verdict (not a record value): :status " (:status upd) ", :theta " (:theta upd) ", :trials-read " (:trials-read upd) ",")
                         (str "     carrier :dedup :fired " (pr-str (get-in upd [:carrier :dedup :fired])) ", posterior " (pr-str (get-in upd [:carrier :posterior-concentrations])) " — the same array as the single read.")])
        [l3 y3] (layer y2 "Layer 3 — read-side identity collapse (pattern-theta :616; b-update :523; concentration-carrier :328-331)"
                        ["(into {} (map (juxt :identity identity)) rows) — one row per identity, last wins. The carrier keeps first-append order and lists identities whose rows disagree on :observed (:337-339)."]
                        "key: :identity again, on the rows read, whatever the file holds."
                        ["catches: a file that physically holds two forms of one identity (which layer 1 refuses to write) — counted once."]
                        ["does not catch: two forms with one identity and different :observed — collapsed last-wins, named in :conflicting-identities on the carrier only; pattern-theta reports nothing (B-D review §4 item 2)."]
                        [(str "run here on form 3 handed twice: carrier :read-side {:input-count " (get-in doubled [:dedup :read-side :input-count]) " :counted-count " (get-in doubled [:dedup :read-side :counted-count])
                              " :collapsed-count " (get-in doubled [:dedup :read-side :collapsed-count]) "}, :fired " (pr-str (get-in doubled [:dedup :fired]))
                              ", posterior " (pr-str (:posterior-concentrations doubled)) " = single-row posterior " (= (:posterior-concentrations one) (:posterior-concentrations doubled)) ", same :version " (= (:version one) (:version doubled)) ".")
                         (str "the same two rows folded straight into the prior with no collapse (accumulate is not a deduplicator, B-D §2): " (pr-str no-dedup) " -> theta " (/ (get-in no-dedup [0 0]) (+ (get-in no-dedup [0 0]) (get-in no-dedup [1 0]))) " (not a record value).")])
        height (+ y3 60)]
    (hsvg
     width height
     (str
      (htxt 40 36 "The three dedup layers — what each keys on, what it catches, and what it does not" :size 15 :weight "bold")
      (htxt 40 58 "B-D §2 names three mechanisms at three layers; the carrier's :dedup :fired names which fired for an emission (learning_trial_ledger.clj:360-362, :371-378)." :size 11)
      (htxt 40 76 "The grain of layers 1 and 3 is (occurrence, effect); layer 2's is the occurrence. None of the three is a filter on close acceptance: a refused close's row passes all three (figure 1);" :size 11)
      (htxt 40 94 "acceptance is annotated per row by annotate-close-statuses (:244-288) and never used to drop a row." :size 11)
      l1 l2 l3
      (htxt 40 (+ y3 10) "Evidence only: the census counts recorded receipts; the three demonstrations call the ledger namespace in this process on the recorded forms and write nothing." :size 9)))))

;; ---------------------------------------------------------------------------
;; Figure 4 — the B-C carrier as landed
;; ---------------------------------------------------------------------------

(defn fig4-carrier [fx-row-record fx-close-record live-row-2 live-snapshot c77]
  (let [fx-rows (ledger/annotate-close-statuses (ledger/read-trials nil fx-row) [fx-close] nil nil)
        c (ledger/concentration-carrier family fx-rows {:layer :none})
        ti (first (:trial-identities c))
        refusals [["{:family … :theta 3/4} (a scalar, no arrays)" (ledger/carrier-refusal {:family family :theta 3/4})]
                  ["a :token-belief on the trial" (ledger/carrier-refusal (assoc-in c [:trial-identities 0 :token-belief] {["M-f11-find-production-successor" :hole/h9ab212b3281d] 1}))]
                  [":state-belief [2] (singleton mass not 1)" (ledger/carrier-refusal (assoc-in c [:trial-vectors 0 :state-belief] [2]))]
                  [":posterior-concentrations [[5/2] [1/2]] (a double count)" (ledger/carrier-refusal (assoc c :posterior-concentrations [[5/2] [1/2]]))]
                  [":normalization :theta 0.75 (a double)" (ledger/carrier-refusal (assoc-in c [:normalization :theta] 0.75))]
                  [":version tampered" (ledger/carrier-refusal (assoc c :version "sha256:0000"))]]
        fams (:families live-snapshot)
        width 1240
        [carrier-svg y1] (lines 50 154
                                [(str ":schema " (:schema c) "   :family " (:family c) "   :population " (:population c) "   :axes " (pr-str (:axes c)))
                                 (str ":prior-concentrations " (pr-str (:prior-concentrations c)) "   :posterior-concentrations " (pr-str (:posterior-concentrations c)) "   (O x S arrays: [[achieved] [not]] at the singleton state)")
                                 (str ":trial-identities [{:identity " (short-id (:identity ti)) " :theta-key " (:theta-key ti) " :cell " (:cell ti) " :observed " (:observed ti) " :content-ref " (subs (:content-ref ti) 0 20) "…")
                                 (str "                    :close-acceptance {:status " (get-in ti [:close-acceptance :status]) " :accepted? " (get-in ti [:close-acceptance :accepted?]) " :reason " (get-in ti [:close-acceptance :reason]))
                                 (str "                                       :source {:path " (pr-str (get-in ti [:close-acceptance :source :path])) " :raw-sha256 " (short-id (get-in ti [:close-acceptance :source :raw-sha256])))
                                 (str "                                                :key-path " (pr-str (get-in ti [:close-acceptance :source :key-path])) "}}}]")
                                 (str ":trial-vectors " (pr-str (mapv #(update % :identity short-id) (:trial-vectors c))) "   (one-hot over [:achieved :not], state-belief [1]; posterior = prior + Σ outer, :353-354)")
                                 (str ":normalization " (pr-str (dissoc (:normalization c) :rule)))
                                 (str "               :rule " (pr-str (get-in c [:normalization :rule])))
                                 (str ":dedup " (pr-str (:dedup c)))
                                 (str ":version " (:version c) "  (carrier-hash over {schema family axes prior posterior trial-vectors}, :387-391)")
                                 (str "carrier-refusal on this carrier: " (pr-str (ledger/carrier-refusal c)) "   exact-tree?: " (ledger/exact-tree? c))])
        [ref-svg y2] (lines 50 (+ y1 44)
                            (for [[what r] refusals] (str (format "%-58s" what) " -> " (pr-str r))))
        [live-svg y3] (lines 50 (+ y2 62)
                             (concat
                              [(str ":status " (:status live-snapshot) "  :schema " (:schema live-snapshot) "  :rows-read " (:rows-read live-snapshot) "  :close-files-scanned " (:close-files-scanned live-snapshot)
                                    "  :ledger-sha256 " (short-id (:ledger-sha256 live-snapshot)) "  :unattributed-identities " (pr-str (:unattributed-identities live-snapshot)))]
                              (for [[f fc] (sort-by (comp str key) fams)
                                    l (cons (str (format "%-46s" (str f)) " n " (get-in fc [:normalization :trials]) "  s " (get-in fc [:normalization :successes]) "  posterior " (pr-str (:posterior-concentrations fc))
                                                 "  theta " (get-in fc [:normalization :theta]) "  :dedup :fired " (pr-str (get-in fc [:dedup :fired])) "  :version " (subs (:version fc) 0 20) "…")
                                            (for [t (:trial-identities fc)]
                                              (let [ca (:close-acceptance t)]
                                                (str "     " (short-id (:identity t)) " " (format "%-9s" (name (:cell t))) " close-acceptance {:status " (:status ca) " :accepted? " (pr-str (:accepted? ca)) " :reason " (pr-str (:reason ca)) "}"
                                                     (when-let [p (get-in ca [:source :path])] (str "  <- " (str/replace p #".*/(wm-full-loop-machinery-\d+)/[^/]+/(attempt-\d+)/.*" "$1/$2")))))))]
                                l)))
        height (+ y3 118)]
    (hsvg
     width height
     (str
      (htxt 40 36 "The B-C carrier as landed (4a2ba931) — :apparatus/done-is-observed-running from the fixture extract, and the live population it would record" :size 15 :weight "bold")
      (htxt 40 58 "(a) EXTRACT, NOT A RECORD VALUE: concentration-carrier (learning_trial_ledger.clj:294-391) run here on test/fixtures/b-update-carrier/8e7d1aaf.edn" :weight "bold" :size 11)
      (htxt 40 76 (str "    (a verbatim copy of ledger form 3; equal to the live form here: " (= fx-row-record live-row-2) "), annotated from machinery-71-attempt-002-close-extract.edn") :size 10)
      (htxt 40 94 (str "    (its :source " (:source fx-close-record) ", :raw-sha256 " (short-id (:raw-sha256 fx-close-record)) "…, equal to the live close's raw sha256 here: " (= (:raw-sha256 fx-close-record) (file-sha256 close-71)) ").") :size 10)
      (htxt 40 112 (str "    No close written after 4a2ba931 (05:33:52Z) exists: the newest close, machinery-77 attempt-001, is :recorded-at " (:recorded-at c77) ", :outcome " (:outcome (judgment c77))
                        ", and has no :b-update key (" (contains? (judgment c77) :b-update) ").") :size 10)
      (htxt 40 136 "the carrier value:" :weight "bold" :size 11)
      carrier-svg
      (htxt 40 (+ y1 20) "(b) X5's bad cases through carrier-refusal (:393-435), each recomputed from the carrier's own vectors:" :weight "bold" :size 11)
      ref-svg
      (htxt 40 (+ y2 20) "(c) NOT A RECORD VALUE: close-b-update (:437-485) run here over the live ledger and every 007-closed.edn under data/wm-full-loop* — the :b-update snapshot the next close would carry" :weight "bold" :size 11)
      (htxt 40 (+ y2 38) "    (the :close-acceptance of each row is joined on the three-field occurrence key against the close records, :244-288; :same-close would apply to the close being written)" :size 10)
      live-svg
      (htxt 40 (+ y3 14) "Of the twelve rows: one has a close with :accepted? true; seven read false; one :refused; one :no-acceptance-declared (a non-boolean, passed through as recorded);" :size 10)
      (htxt 40 (+ y3 32) "one close has no :accepted? key; one row's close was not found among the files scanned. A builder keeping only accepted-close rows would give every family but one the prior 1/2 —" :size 10)
      (htxt 40 (+ y3 50) "the falsifier B-D Revision 2 §R2.5(1) names, pinned by the test dropping-refused-close-rows-disagrees-with-record-1790199409." :size 10)
      (htxt 40 (+ y3 68) "GEN-D Revision 2 §R2.3's proposed encoding differs from this one in cell names (:not-achieved / :attempt vs :not / :singleton) and identity order (ascending vs append); it is a proposed amendment there." :size 10)
      (htxt 40 (+ y3 98) "Evidence only: (a) is the carrier on the extract, (c) on the live rows; both are computed here and no close carries either." :size 9)))))

;; ---------------------------------------------------------------------------
;; Figure 5 — what "B" is today
;; ---------------------------------------------------------------------------

(defn trace-of [t cid]
  (first (filter #(= cid (get-in % [:id :id])) (get-in t [:decision :selection-certificate :node-evaluation-traces]))))

(defn fig5-B [t lean-text]
  (let [tr1 (trace-of t :C1) tr2 (trace-of t :C2)
        terms (sort (keys (get-in t [:decision :selection-certificate :g-term-decomposition :policies 0 :terms])))
        wanted [target :restoration-accepted]
        with-mass (fn [belief] (reduce + 0 (for [[s v] belief :when (contains? s wanted)] v)))
        trace-lines (fn [tr]
                      (cons (str "guard order = the precedence: " (pr-str (mapv (comp name :pattern-id) (get-in tr [:evaluations 0 :states 0 :guard-search]))) "; each state line shows the guard verdicts in that order, then the kernel applied")
                            (apply concat
                                   (for [ev (:evaluations tr)]
                                     (cons (str "tau " (:tau ev) "   " (count (:states ev)) " state(s) in the support   ->  outgoing mass on states containing the wanted token: " (with-mass (:outgoing-belief ev)))
                                           (for [s (:states ev)]
                                             (str "     mass " (format "%-9s" (str (:mass s))) " guards " (pr-str (mapv :guard-verdict (:guard-search s))) "  ->  "
                                                  (if (:pattern-id s)
                                                    (str "pattern-kernel by " (name (:pattern-id s)) ": " (pr-str (vec (vals (:kernel s)))) " (theta to s ∪ produces, 1 - theta stays)")
                                                    "identity kernel {s 1} (no guard holds)"))))))))
        lean-decls ["DirichletParams" "step" "accumulate" "accumulate_conc" "accumulate_append" "accumulate_onehot" "total" "accumulate_total_of_normalized" "rowTheta"]
        lean-present (into {} (for [d lean-decls] [d (boolean (re-find (re-pattern (str "(def|theorem|structure|abbrev) " d "\\b")) lean-text))]))
        width 1240
        [k-svg y1] (lines 50 118
                          ["pattern-kernel (cascade_model_manifest.clj:293-311): for a pattern with :theta and a state s,  {s ∪ produces  theta,  s  1 - theta}  (:310-311), merged to {s 1} when s already contains produces (:306-307);"
                           "theta must be an exact integer or ratio in [0,1] (:303-305) else {:status :missing :kind :invalid-pattern-interpretation}. with-pattern-theta (:282-291) keeps a present :theta and defaults an absent one to 1."
                           "Lean: CascadeTransition.InterpretedPattern (CascadeTransition.lean:22) carries theta; patternKernel (:42) is the same row. The judge's stamped theta enters here and nowhere else."])
        [t1-svg y2] (lines 50 (+ y1 44) (trace-lines tr1))
        [t2-svg y3] (lines 50 (+ y2 44) (trace-lines tr2))
        [d-svg y4] (lines 50 (+ y3 44)
                          ["O = [:achieved :not], S = [:singleton]; prior [[1/2] [1/2]]; trial i contributes outer(one-hot(observed_i), [1]); after n trials with s successes: [[s + 1/2] [n - s + 1/2]]  (accumulate_conc, accumulate_onehot);"
                           "theta = conc(achieved) / (conc(achieved) + conc(not)) at the fixed singleton state = (s + 1/2)/(n + 1)  — normalization over outcomes at a fixed state (B-D Revision 2 §R2.3, AR-18), not across states."
                           (str "On this record: 3/4 <- [[3/2] [1/2]];  1/4 <- [[1/2] [3/2]];  1/16 <- [[1/2] [15/2]].  Checked here: " (= [3/4 1/4 1/16] [(/ 3/2 2) (/ 1/2 2) (/ 1/2 8)]) ".")
                           (str "DirichletLearning.lean declarations found by name here: " (str/join ", " (for [d lean-decls :when (get lean-present d)] d)) ";  not found: " (str/join ", " (for [d lean-decls :when (not (get lean-present d))] d)) ".")
                           "The Lean arrays are over the reals (DirichletParams.conc : O → S → ℝ); the record's values are exact rationals; the normalization function and the cast equality are what B-N owes (§R2.3, AR-5/AR-18)."])
        height (+ y4 150)]
    (hsvg
     width height
     (str
      (htxt 40 36 "What \"B\" is today — a scalar theta per pattern, consumed as a transition probability, equal to a two-cell Dirichlet normalization" :size 15 :weight "bold")
      (htxt 40 58 "P(s'|s,π) in the theory is a matrix over states. On the records it is one number per pattern id, read from the ledger, placed on the pattern, and consumed by the first-enabled guard search as the mass that moves to s ∪ produces." :size 11)
      (htxt 40 76 (str "[:decision :selection-certificate :g-term-decomposition :policies 0 :terms] on this record has keys " (pr-str terms) " — no :B term (P5 names one). The consumed value lives on the candidates' patterns and in the traces below.") :size 10)
      (htxt 40 100 "(a) the consumer" :weight "bold" :size 11)
      k-svg
      (htxt 40 (+ y1 20) (str "(b) [:node-evaluation-traces] for C1 on the record (horizon " (:horizon tr1) ", status " (:status tr1) "): which theta multiplied mass") :weight "bold" :size 11)
      t1-svg
      (htxt 40 (+ y2 20) (str "(c) the same for C2 (horizon " (:horizon tr2) ")") :weight "bold" :size 11)
      t2-svg
      (htxt 40 (+ y3 20) "(d) the Dirichlet specialization the scalar equals (B-D §1 as corrected by Revision 2 §R2.3)" :weight "bold" :size 11)
      d-svg
      (htxt 40 (+ y4 14) "What the record does not contain: any observation of a pattern's transition. Each row observes one token at one revision after a whole attempt; producer-of (:77-96) credits the pattern whose :produces" :size 10)
      (htxt 40 (+ y4 32) "declares that token — attribution BY DECLARATION, as its docstring says (:85-87), and as every row's :contract :does-not-establish #{:individual-pattern-firing :pattern-causality …} says. The singleton state" :size 10)
      (htxt 40 (+ y4 50) "belief [1] is not a posterior over anything; B-D §2(2) and carrier-refusal's :token-posterior-not-a-trial refuse a token-level belief in its place. On this click the pattern carrying 3/4 was never enabled:" :size 10)
      (htxt 40 (+ y4 68) "its guard needs :repair/obstruction-observed-cleared absent and the initial belief already contains it (walkthrough 03 §1); the 1/4 on the sibling pattern is the mass that moved, and C2's 1/16 likewise." :size 10)
      (htxt 40 (+ y4 96) "No record checked (the 14 closes of machinery-70..76, 77/001, and this tick) carries :B-read, :model-inputs, :b-update or a B :version (Revision 2 §R2.4, reproduced here in the generator's key walk)." :size 10)
      (htxt 40 (+ y4 126) "Evidence only: (a) is the code; (b)-(c) are the record's traces; (d)'s arithmetic and the Lean name check are computed here." :size 9)))))

;; ---------------------------------------------------------------------------
;; Figure 6 — narrative: one row, comparison -> ledger -> judge -> score
;; ---------------------------------------------------------------------------

(defn fig6-narrative [c71 t c76 rows]
  (let [j71 (judgment c71) j76 (judgment c76)
        occ71 (:occurrence j71)
        cmp71 (:token-outcome-comparison j71)
        row71 (first (filter #(= [(:target (:action/value occ71)) :hole/h9ab212b3281d] (:token %)) (:tokens cmp71)))
        rt (get-in j71 [:learning-trial-receipt :trials 0])
        c1 (get-in t [:decision :selection-certificate :candidates 0])
        c2 (get-in t [:decision :selection-certificate :candidates 1])
        p0 (get-in c1 [:id :precedence 0]) p1 (get-in c1 [:id :precedence 1])
        selected-at (selection-timestamp t)
        tr1 (trace-of t :C1)
        fired (distinct (for [ev (:evaluations tr1) s (:states ev) :when (:pattern-id s)] (:pattern-id s)))
        idx (.indexOf (mapv :identity rows) pinned-id)
        last-row (last rows)
        rt76 (last (get-in j76 [:learning-trial-receipt :trials]))
        today (ledger/pattern-theta :apparatus/evidence-to-disposition-once "data/wm-learning-trials")
        steps [["action" (:action-at occ71)
                (str "machinery-71 attempt-002, run " (:run/id occ71) ": C1 on " (get-in occ71 [:action/value :target]) ", precedence " (pr-str (mapv :id (get-in occ71 [:action/value :precedence]))) ".")
                (str ":action/id " (:action/id occ71) "  :transition/id " (:transition/id occ71))]
               ["comparison" (str "artifact " (short-sha (:artifact-sha cmp71)))
                (str "compare-outcomes: " (token-name (:token row71)) " predicted " (:predicted row71) ", observed " (:observed row71) " -> " (:verdict row71) ". The receipt's one trial is " (:status rt) ".")
                (str "record! appends it as ledger form " (inc idx) ": identity " (short-id pinned-id) ", :observed true, :increment {:success 1 :failure 0}; receipt :counted? " (:counted? rt) " :ledger :status " (get-in rt [:ledger :status]))]
               ["close" (:recorded-at c71)
                (str "accepted-increment :accepted? " (get-in j71 [:accepted-increment :accepted?]) " (:reason " (get-in j71 [:accepted-increment :reason]) "); :outcome " (:outcome j71) ". The row stays appended; b-update does not run.")
                "no :b-update key on this close (predates 4a2ba931); the ledger is the only carrier of the row."]
               ["judge's read" selected-at
                (str "pattern-theta(" (name family) ") over the ledger: 1 trial, 1 success -> 3/4; stamped on C1 precedence 0 for target " (short-target (get-in c1 [:id :target])) " with :identities [" (short-id pinned-id) "], :targets " (pr-str (get-in p0 [:theta-provenance :targets])) ".")
                (str "C1 precedence 1 " (:id p1) " theta " (:theta p1) " from " (pr-str (mapv short-id (get-in p1 [:theta-provenance :identities]))) ";  C2 precedence 3 theta " (:theta (get-in c2 [:id :precedence 3])) " from 7 identities.")]
               ["score" (str "G: C1 " (:g c1) ", C2 " (:g c2))
                (str "C1's trace: the pattern carrying 3/4 has guard false at every tau; the patterns that applied are " (pr-str (mapv name fired)) " at theta " (:theta p1) " — terminal mass with the wanted token "
                     (reduce + 0 (for [[s v] (:outgoing-belief (last (:evaluations tr1))) :when (contains? s [target :restoration-accepted])] v)) " (= walkthrough 03's 175/256).")
                "3/4 entered the record as provenance on the selected candidate and multiplied no mass on this click; G was decided from class emission over these rollouts (walkthroughs 02 §5, 03 §5)."]
               ["next append" (:recorded-at c76)
                (str "machinery-76 attempt-002 closes: receipt trial " (name (second (:effect rt76))) " " (:status rt76) "; record! appends form " (count rows) " (" (short-id (:identity last-row)) ", " (:theta-key last-row) ", observed " (:observed last-row) "); :accepted? " (get-in j76 [:accepted-increment :accepted?]) ".")
                (str "NOT A RECORD VALUE: pattern-theta(" (name (:theta-key last-row)) ") today = " (:theta today) " from " (:trials-count today) " trials; the record above read 1/4 from 1. Nothing on either record names the ledger state it read.")]]
        width 1240 height (+ 110 (* 92 (count steps)) 60)]
    (hsvg
     width height
     (str
      (htxt 40 36 "One row, end to end — machinery-71's comparison, the ledger, the 09-23 judge's 3/4, and the score it entered" :size 15 :weight "bold")
      (htxt 40 58 "Every value with its source: 71/002's close, the ledger, tick-run-record 1790199409, 76/002's close. Timestamps are the records' own (:action-at, :recorded-at, the live-selection occurrence-id)." :size 10)
      (apply str
             (for [[i [label at l1 l2]] (map-indexed vector steps)
                   :let [y (+ 80 (* 92 i))]]
               (str (box 40 y (- width 80) 78 (if (= label "next append") "#fdf6ec" "#f6f7f8") "#68717b")
                    (htxt 50 (+ y 20) (str label "   " at) :weight "bold")
                    (htxt 50 (+ y 40) l1 :size 10)
                    (htxt 50 (+ y 58) l2 :size 10 :mono true)
                    (when (pos? i) (arrow 120 (- y 14) 120 y)))))
      (htxt 40 (+ 90 (* 92 (count steps))) "What the records say: one appended row, one refused close, one stamped 3/4 with its identity, one trace in which that 3/4 was never applied. What they do not say: which ledger state the judge read, or that any pattern fired." :size 10)
      (htxt 40 (+ 110 (* 92 (count steps))) "Evidence only: the last box's theta is computed here and labelled; everything else is read from the four records." :size 9)))))

;; ---------------------------------------------------------------------------

(defn generate! []
  (let [rows (ledger-rows)
        c71 (read-record close-71)
        c76 (read-record close-76)
        c77 (read-record close-77)
        t (read-record tick)
        fx-row-record (edn/read-string (slurp fx-row))
        fx-close-record (read-record fx-close)
        live-row-2 (:row (nth rows 2))
        files (close-files)
        census (receipt-census files)
        live-snapshot (ledger/close-b-update {:ledger-root "data/wm-learning-trials"})
        lean-text (slurp lean-file)
        ledger-mtime (str (Files/getLastModifiedTime (.toPath (io/file ledger-path)) (make-array java.nio.file.LinkOption 0)))
        h-ledger (sha8 ledger-path) h-71 (sha8 close-71) h-76 (sha8 close-76) h-tick (sha8 tick)
        h-fxrow (sha8 fx-row) h-fxclose (sha8 fx-close)]
    (spit-svg (str "fig1-ledger-row-" h-ledger "-" h-71 ".svg") (fig1-row-and-write rows c71))
    (spit-svg (str "fig2-judge-read-" h-tick "-" h-ledger ".svg") (fig2-judge-read t rows ledger-mtime))
    (spit-svg (str "fig3-dedup-" h-ledger "-" h-fxrow ".svg") (fig3-dedup rows census))
    (spit-svg (str "fig4-carrier-" h-fxrow "-" h-fxclose "-" h-ledger ".svg") (fig4-carrier fx-row-record fx-close-record live-row-2 live-snapshot c77))
    (spit-svg (str "fig5-B-" h-tick ".svg") (fig5-B t lean-text))
    (spit-svg (str "fig6-narrative-" h-71 "-" h-tick "-" h-76 ".svg") (fig6-narrative c71 t c76 rows))
    (println "ledger" (file-sha256 ledger-path) "forms" (count rows))
    (println "close-71" (file-sha256 close-71))
    (println "close-76" (file-sha256 close-76))
    (println "tick" (file-sha256 tick))
    (println "fixture row" (file-sha256 fx-row) "= live form 3:" (= fx-row-record live-row-2))
    (println "fixture close extract" (file-sha256 fx-close) "names raw-sha256" (:raw-sha256 fx-close-record) "equal to close-71 now:" (= (:raw-sha256 fx-close-record) (file-sha256 close-71)))
    (println "fixture tick extract" (file-sha256 fx-tick) "names raw-sha256" (:raw-sha256 (read-record fx-tick)) "equal to tick now:" (= (:raw-sha256 (read-record fx-tick)) (file-sha256 tick)))
    (println "census" (pr-str census))
    (println "live snapshot families" (pr-str (into {} (for [[f c] (:families live-snapshot)] [f (select-keys (:normalization c) [:trials :successes :theta])]))))
    (println "contract-version labels" (pr-str (frequencies (map :contract-version rows)))
             "contract schemas" (pr-str (frequencies (map #(get-in % [:row :contract :schema]) rows))))))
