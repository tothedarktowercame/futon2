;; generate_figures_05.clj — regenerate every figure in 05-close-and-adjudication.md
;; from the records. Run from the futon2 checkout root:
;;
;;   clojure -M -i holes/labs/wm-contract/walkthroughs/generate_figures_05.clj \
;;           -e "(generate-figures-05/generate!)"
;;
;; Read-only: reads the seven checkpoint files, retained/ and evidence/ of
;; the machinery-70..76 attempts (fourteen closes), machinery-77 attempt-001,
;; two tick records, one repair finding, and the code's own git history for
;; three shas; writes only SVGs beside this script. Every figure file name
;; carries the short raw sha256 of the record(s) it was drawn from; the two
;; census figures carry a digest of the fourteen close shas. Where a figure
;; shows what the code computes on a record — evaluate-close at today's HEAD
;; on 71/002's recorded inputs, classify/verify-close re-run on a recorded
;; close — it calls the namespace in this fresh process on values the records
;; already hold, and the figure says so. Nothing is written back to any
;; record; no runner function that writes is called.
(ns generate-figures-05
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [futon2.aif.accepted-increment :as ai]
            [futon2.aif.cascade-sources :as cs]
            [futon2.aif.run-ending-classification :as run-ending])
  (:import [java.nio.file Files]
           [java.security MessageDigest]))

(def here "holes/labs/wm-contract/walkthroughs")

(defn attempt-dir [n a]
  (str "data/wm-full-loop-machinery-" n "/wm-contract-machinery-" n "-v1/attempt-" a "/"))

(def attempts (vec (for [n (range 70 77) a ["001" "002"]] [n a])))
(def dir-76 (attempt-dir 76 "002"))
(def dir-71 (attempt-dir 71 "002"))
(def dir-77 (attempt-dir 77 "001"))
(def tick-0923 "data/wm-runs/tick-run-record-2026-09-23-1790199409.edn")
(def tick-0924 "data/wm-runs/tick-run-record-2026-09-24-1790225596.edn")
(def scan-0924 "data/wm-runs/tick-run-record-2026-09-24-1790225596.scan.md")
(def finding "data/wm-repair-obligations/findings/repair-occ-ad16e2c2e10f83412628d1eec8e222018afa399fba64016124e32f3a812a3214.edn")
(def ledger-path "data/wm-learning-trials/attempts.edn")
(def fixture-71 "test/fixtures/accepted-increment/machinery-71-attempt-002.edn")

(def phase-files ["001-time-step" "002-selection" "003-construction" "004-dispatch"
                  "005-build" "006-adjudication" "007-closed"])

(defn read-record [path]
  (edn/read-string {:default (fn [t v] (tagged-literal t v))} (slurp path)))

(defn- hex [bytes n]
  (apply str (map #(format "%02x" (bit-and 255 %)) (take n bytes))))

(defn file-sha256
  "Raw-byte SHA-256 of a file."
  [path]
  (hex (.digest (MessageDigest/getInstance "SHA-256") (Files/readAllBytes (.toPath (io/file path)))) 32))

(defn sha8 [path] (subs (file-sha256 path) 0 8))

(defn string-sha8 [s]
  (subs (hex (.digest (MessageDigest/getInstance "SHA-256") (.getBytes ^String s "UTF-8")) 32) 0 8))

(defn spit-svg [name svg]
  (let [file (str here "/" name)]
    (spit file svg)
    (println "wrote" file)))

(defn git-date
  "Commit date of SHA in this checkout, from git's own record (read-only)."
  [sha]
  (let [{:keys [exit out]} (sh/sh "git" "show" "-s" "--format=%ci" sha)]
    (if (zero? exit) (str/trim out) (str "not a commit here: " sha))))

(defn git-head []
  (str/trim (:out (sh/sh "git" "rev-parse" "HEAD"))))

;; ---------------------------------------------------------------------------
;; svg helpers (same style as walkthroughs 01-04)
;; ---------------------------------------------------------------------------

(defn- esc [s]
  (-> (str s) (str/replace "&" "&amp;") (str/replace "<" "&lt;") (str/replace ">" "&gt;")))

(defn- htxt [x y s & {:keys [size anchor weight mono] :or {size 12 anchor "start"}}]
  (str "<text x=\"" x "\" y=\"" y "\" font-size=\"" size "\" text-anchor=\"" anchor "\""
       (when weight (str " font-weight=\"" weight "\""))
       " font-family=\"" (if mono "monospace" "system-ui,sans-serif") "\">" (esc s) "</text>"))

(defn- box [x y w h fill stroke]
  (str "<rect x=\"" x "\" y=\"" y "\" width=\"" w "\" height=\"" h "\" rx=\"5\""
       " fill=\"" fill "\" stroke=\"" stroke "\"/>"))

(defn- arrow [x1 y1 x2 y2]
  (str "<path d=\"M" x1 "," y1 " L" x2 "," y2 "\" fill=\"none\" stroke=\"#68717b\" marker-end=\"url(#harrow)\"/>"))

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
(defn kv [m k] (if (contains? m k) (pr-str (get m k)) "no key"))

(defn phase-records [dir]
  (into (sorted-map) (for [f phase-files] [f (read-record (str dir f ".edn"))])))

(defn dir-listing [dir sub]
  (let [d (io/file dir sub)]
    (if (.isDirectory d)
      (vec (sort (map #(.getName ^java.io.File %) (filter #(.isFile ^java.io.File %) (.listFiles d)))))
      [])))

(defn seconds-between [a b]
  (let [ia (java.time.Instant/parse a) ib (java.time.Instant/parse b)]
    (/ (- (.toEpochMilli ib) (.toEpochMilli ia)) 1000.0)))

(defn close-row
  "Everything the two census figures need from one close, read once."
  [[n a]]
  (let [dir (attempt-dir n a)
        path (str dir "007-closed.edn")
        c (read-record path)
        j (judgment c)
        ai-map (:accepted-increment j)
        rec (:run-ending-classification j)
        cmp (:token-outcome-comparison j)
        ke (:kernel-example j)
        rcpt (:learning-trial-receipt j)
        ra (:route-attestation j)
        c-ev (get-in ai-map [:evidence :acceptance-result :evidence])
        obs (vals (get-in ke [:observation-projection :observations]))]
    {:label (str n "/" a)
     :path path :sha8 (subs (file-sha256 path) 0 8) :sha (file-sha256 path)
     :recorded-at (:recorded-at c)
     :outcome (:outcome j) :failure-kind (:failure-kind j)
     :grounded? (:grounded? j)
     :has-ai? (contains? j :accepted-increment)
     :accepted (if (contains? j :accepted-increment) (kv ai-map :accepted?) "no key")
     :failed (:failed ai-map) :reason (:reason ai-map) :message (:message ai-map)
     :criterion (if (contains? ai-map :criterion-step)
                  (str (pr-str (get-in ai-map [:criterion-step :id])) " " (name (or (get-in ai-map [:criterion-step :source]) :none))
                       (when-let [r (get-in ai-map [:criterion-step :reason])] (str " " r)))
                  "no key")
     :c-sha (:sha c-ev) :c-resolved (when (:resolved-sha c-ev) (short-sha (:resolved-sha c-ev)))
     :binding-keys (vec (sort (keys (get-in ai-map [:evidence :binding]))))
     :rec-present? (some? rec) :rec-status (:status rec) :rec-class (:class rec) :rec-missing (:missing rec)
     :rec-failure-kind (:failure-kind rec)
     :cmp-present? (some? cmp) :cmp-status (:status cmp) :artifact (short-sha (:artifact-sha cmp))
     :verdicts (into (sorted-map) (frequencies (map :verdict (:tokens cmp))))
     :ke-present? (some? ke) :ke-status (:status ke) :ke-obs (get-in ke [:missingness :observations])
     :ke-rows (count obs) :ke-true (count (filter #(true? (get-in % [:artifact-observation :observed])) obs))
     :rcpt-present? (some? rcpt)
     :counted (count (filter :counted? (:trials rcpt))) :held (count (remove :counted? (:trials rcpt)))
     :pairs? (contains? cmp :token-outcome-pairs) :b-update? (contains? j :b-update)
     :ra-status (:status ra) :ra-increments (count (:increments ra))
     :ra-cmp-status (get-in ra [:token-outcome-comparison :status])
     :ra-cmp-equal? (= cmp (:token-outcome-comparison ra))
     :retained (dir-listing dir "retained") :evidence (dir-listing dir "evidence")
     :manifest (count (get-in c [:payload :close-evidence-manifest :entries]))
     :retention? (some? (get-in c [:payload :close-retention]))
     :surprises (count (:surprise-ids j))
     :close c}))

(defn census-digest [rows]
  (string-sha8 (str/join "\n" (map :sha rows))))

;; ---------------------------------------------------------------------------
;; Figure 1 — the phase sequence from build to close, as the directory records it
;; ---------------------------------------------------------------------------

(defn fig1-phases [dir-76 dir-71]
  (let [p76 (phase-records dir-76) p71 (phase-records dir-71)
        c76 (get p76 "007-closed") c71 (get p71 "007-closed")
        payload-shape (fn [r] (let [p (:payload r)]
                                (str "payload keys " (str/join " " (map name (sort (keys p))))
                                     (when-let [g (get-in p [:ground :kind])] (str ";  :ground :kind " g))
                                     (when-let [s (get-in p [:sorry :kind])] (str ";  :sorry :kind " s)))))
        row (fn [[f r]] (format "%-18s %-3s %-13s %-38s %s" f (str (:event/sequence r)) (name (:checkpoint/type r)) (:recorded-at r) (payload-shape r)))
        listing (fn [dir] (let [fs (dir-listing dir "retained")]
                            (str (str/join ", " (remove #(str/starts-with? % "job-") fs)) " + " (count (filter #(str/starts-with? % "job-") fs)) " job-*.txt")))
        gaps (fn [p] (let [ts (mapv (comp :recorded-at val) p)]
                       (str/join "   " (for [i (range 1 (count ts))]
                                         (str (subs (key (nth (seq p) i)) 0 3) "-" (subs (key (nth (seq p) (dec i))) 0 3) " " (format "%.1f s" (seconds-between (nth ts (dec i)) (nth ts i))))))))
        writes ["retained/token-outcome.edn         retain-token-outcome! (full_loop_runner.clj:3131-3184): compare-outcomes + pairs + learning receipt (record! inside) + surprises; spit at :3173"
                "retained/surprises.edn             the same function, :3174"
                "retained/route-attestation.edn     route-attestation/retain! (:3980-3990); the receipt goes on the close as :route-attestation, its path+sha as :route-attestation-ref"
                "retained/kernel-example.edn        retain-kernel-example! (:3186-3205): kernel-example/collect on the same enactment file, digest re-checked (:3191-3193); spit at :3200"
                "retained/run-ending-classification.edn  retain-run-ending! (:3207-3219): run-ending/classify on close-judgment-base (:4150-4159), i.e. on the judgment BEFORE the close file exists"
                "retained/job-*.txt                 job-texts/checkpoint-cell at the :dispatch and :build checkpoints (:3750-3752): author and reviewer prompts and replies"
                "evidence/wm-reviewer-standing.edn  ensure-standing-decision! after an approved build (:5239-5265): the reviewer's :resolved decision on the target"
                "retained/b-update.edn              b-update-retained (:4279-4297) — written only by closes after 4a2ba931 (05:33:52Z 09-24); none of these fourteen"
                "007-closed.edn                     cohort/close-attempt! (full_loop_cohort.clj:627-651) on `closed` (:4178-4191): judgment = close-judgment-base + :run-ending-classification,"
                "                                   ground {:kind :full-loop-outcome}; the cohort refuses a close that is not a grounded term or lacks a required checkpoint (:639-650)"]
        width 1240
        [r76 y1] (lines 50 118 (map row p76))
        [r71 y2] (lines 50 (+ y1 62) (map row p71))
        [w-svg y3] (lines 50 (+ y2 62) writes)
        [m-svg y4] (lines 50 (+ y3 44)
                          [(str "retained/ on 76/002: " (listing dir-76) ";  evidence/: " (str/join ", " (dir-listing dir-76 "evidence")))
                           (str "retained/ on 71/002: " (listing dir-71) ";  evidence/: " (str/join ", " (dir-listing dir-71 "evidence")))
                           (str ":close-evidence-manifest on 76/002: " (count (get-in c76 [:payload :close-evidence-manifest :entries])) " entries — the six checkpoint files, evidence/wm-reviewer-standing.edn, and five retained/*.edn (job texts are not in the manifest)")
                           (str ":close-retention on 76/002: " (pr-str (select-keys (get-in c76 [:payload :close-retention]) [:state :model])))
                           (str "     :closed-at " (get-in c76 [:payload :close-retention :closed-at]) " :evidence-cutoff " (get-in c76 [:payload :close-retention :evidence-cutoff]) " :admitted-evidence " (count (get-in c76 [:payload :close-retention :admitted-evidence])) " ids — the manifest's entries; :state and :model are typed absences on every close (:4184-4190)")
                           (str "retained copies equal the close's own carriers (checked here) on 76/002: kernel-example " (= (read-record (str dir-76 "retained/kernel-example.edn")) (:kernel-example (judgment c76)))
                                ", run-ending " (= (read-record (str dir-76 "retained/run-ending-classification.edn")) (:run-ending-classification (judgment c76)))
                                ", token-outcome " (= (read-record (str dir-76 "retained/token-outcome.edn")) (:token-outcome-comparison (judgment c76)))
                                ", route-attestation " (= (read-record (str dir-76 "retained/route-attestation.edn")) (:route-attestation (judgment c76))))
                           (str "     the same four on 71/002: "
                                (= (read-record (str dir-71 "retained/kernel-example.edn")) (:kernel-example (judgment c71))) " " (= (read-record (str dir-71 "retained/run-ending-classification.edn")) (:run-ending-classification (judgment c71)))
                                " " (= (read-record (str dir-71 "retained/token-outcome.edn")) (:token-outcome-comparison (judgment c71))) " " (= (read-record (str dir-71 "retained/route-attestation.edn")) (:route-attestation (judgment c71))))])
        height (+ y4 60)]
    (hsvg
     width height
     (str
      (htxt 40 36 "The phase sequence from build to close — the attempt directory as the cohort writes it (machinery-76 attempt-002 and machinery-71 attempt-002)" :size 15 :weight "bold")
      (htxt 40 58 "One file per checkpoint (checkpoint! :3749-3769 → cohort/append-checkpoint!), each an event: :event/sequence, :checkpoint/type, :recorded-at, and a :payload with :judgment + :ground, or a :sorry for an unreached phase." :size 11)
      (htxt 40 76 "required-checkpoints (:89) = [:selection :construction :dispatch :build :adjudication]; close-core! (:3789-3797) writes a typed :not-reached-* sorry for any missing one before the close is built." :size 11)
      (htxt 40 100 (str "(a) " dir-76 " — the accepted close") :weight "bold" :size 11)
      r76
      (htxt 50 (+ y1 18) (str "gaps: " (gaps p76)) :size 10)
      (htxt 40 (+ y1 44) (str "(b) " dir-71 " — the refused close (a two-round revision: 005's :commits has two shas, :revision :round 2)") :weight "bold" :size 11)
      r71
      (htxt 50 (+ y2 18) (str "gaps: " (gaps p71)) :size 10)
      (htxt 40 (+ y2 44) "(c) what the close phase writes beside the checkpoint files, and which function writes it (line numbers at futon2 7aedbb42)" :weight "bold" :size 11)
      w-svg
      (htxt 40 (+ y3 24) "(d) the directory contents and the manifest" :weight "bold" :size 11)
      m-svg
      (htxt 40 (+ y4 12) "Between 005 and 006 the runner grounds the commit (ground-commit!, :5327-5333); between 006 and 007 it runs the close phase of (c): comparison, receipt, attestation, kernel example, predicate, classification, manifest." :size 10)
      (htxt 40 (+ y4 30) "The sequence itself is the record: seven :event/sequence numbers, seven :recorded-at values. Every other claim here names the file and key it is read from." :size 10)
      (htxt 40 (+ y4 50) "Evidence only: the file listings, timestamps and equalities are read or computed from the directory; nothing is inferred about what happened between the timestamps beyond what the code at the cited lines does." :size 9)))))

;; ---------------------------------------------------------------------------
;; Figure 2 — adjudication: the reviewer verdict, the grounding witness, the outcome
;; ---------------------------------------------------------------------------

(defn review-marker [text]
  (some-> (re-find #"(?m)^FULL_LOOP_REVIEW:\s*(APPROVE|REQUEST_CHANGES|REJECT)\b" (str text)) first))

(defn fig2-adjudication [dir-76 dir-71]
  (let [rows (for [[label dir] [["76/002" dir-76] ["71/002" dir-71]]]
               (let [b (read-record (str dir "005-build.edn")) bj (judgment b) v (:validation bj)
                     ad (read-record (str dir "006-adjudication.edn")) aj (judgment ad)
                     c (read-record (str dir "007-closed.edn")) cj (judgment c)
                     ab (:artifact-binding v)
                     after-props (get-in aj [:after :implementation-entity :props])]
                 [(str "— " label " —")
                  (str "005-build :validation :review-text, first line (cut at 120 chars): " (pr-str (let [l (first (str/split-lines (str (:review-text v))))] (if (> (count l) 120) (str (subs l 0 120) "…") l))))
                  (str "     review-verdict (task_execution_evidence.clj:25-34) reads the line-anchored marker: " (pr-str (review-marker (:review-text v))) " → :approve;  :review-job " (:review-job v))
                  (str "     :review-gate " (pr-str (select-keys (:review-gate v) [:required? :executed? :tool-events :passed? :execution-source])) "   :approved? " (:approved? v) " (= done ∧ :approve ∧ gate passed, :5177-5180)")
                  (str "     :artifact-binding (fresh-artifact-binding, task_execution_evidence.clj:83-190): :commit " (short-sha (:commit ab)) " :pre-dispatch-head " (short-sha (:pre-dispatch-head ab)) " :claim-resolution " (:claim-resolution ab))
                  (str "          :descendant? " (:descendant? ab) " :corroborates? " (:corroborates? ab) " :claim-in-author-window? " (:claim-in-author-window? ab) " :in-author-window? " (:in-author-window? ab) " — the three (a) tests, plus the HEAD-freshness flag (a) does not read")
                  (str "     :commits " (pr-str (mapv short-sha (:commits bj))) "  :artifacts " (count (:artifacts bj)) " file(s)" (when (:revision bj) (str "  :revision :round " (get-in bj [:revision :round]) ", round-2 review verdict " (get-in bj [:revision :review :verdict]))))
                  (str "006-adjudication (ground :authoritative-substrate-discharge): :build-match " (pr-str (update (:build-match aj) :commit short-sha)) "  :dial :moved? " (get-in aj [:dial :moved?]) " (:implementation-id as below)")
                  (str "     :witness :resolved? " (get-in aj [:witness :resolved?]) "  :dial-moved? " (get-in aj [:witness :dial-moved?]) "  :implementation-id " (get-in aj [:witness :implementation-id]))
                  (str "     :before :implementation-entity " (pr-str (get-in aj [:before :implementation-entity])) ";  :after :implementation-entity :props is a " (.getSimpleName (class after-props))
                       (if (map? after-props)
                         (str " with :implementation/commit " (short-sha (:implementation/commit after-props)))
                         (str " (" (count (str after-props)) " chars; no :implementation/commit key can be read from it)")))
                  "     resolved? = (= commit (get-in after [:props :implementation/commit])) (:2872);  dial-moved? = (and (nil? before) (some? after)) (:2873)"
                  (str "007-closed :outcome " (:outcome cj) "  ← (if (and resolved? dial-moved?) :grounded-change :grounded-no-change) (:5346-5354);  :grounded? " (:grounded? cj) "  :witness on the close = 006's witness: " (= (:witness cj) (:witness aj)))
                  (str "     :accepted-increment :accepted? " (kv (:accepted-increment cj) :accepted?) "   :run-ending-classification :class " (get-in cj [:run-ending-classification :class]) " :missing " (pr-str (get-in cj [:run-ending-classification :missing])))
                  ""]))
        width 1240
        [r-svg y1] (lines 50 136 (apply concat rows))
        [f-svg y2] (lines 50 (+ y1 30)
                          ["conjunct (a) of the predicate reads [:build :judgment :validation :artifact-binding] (:4066) — the 005 file, not 006: :commit, :pre-dispatch-head, :descendant?, :corroborates?,"
                           "   :claim-in-author-window? (accepted_increment.clj:63-69)."
                           "the reviewer's verdict enters the predicate only through control flow: an unapproved build throws (:5267-5326) and the close is built from the catch (:5445-5500) with :outcome :build-failed and no witness;"
                           "   the predicate still runs there (:4011) on whatever the build checkpoint holds."
                           "the adjudication witness enters the close as :witness and as :outcome; :outcome enters run-ending/classify through the projection (:grounded? :artifact-only?; run-ending-classification-v1.edn) —"
                           "   not the predicate."
                           "evidence/wm-reviewer-standing.edn (ensure-standing-decision!, :5239-5265) records the reviewer's :decision :resolved on the target after the approved build; nothing at close reads it into the predicate"
                           "   or the classification."])
        height (+ y2 70)]
    (hsvg
     width height
     (str
      (htxt 40 36 "Adjudication — the reviewer verdict on 005-build, the grounding witness on 006-adjudication, and the outcome the close carries" :size 15 :weight "bold")
      (htxt 40 58 "\"Adjudication\" in the directory is the :authoritative-substrate-discharge checkpoint (:5336-5344): ground-commit! (:2773-2876) writes an implementation entity to the substrate and reads it back;" :size 11)
      (htxt 40 76 "the reviewer's APPROVE is on the build checkpoint, one file earlier. The outcome is decided from the readback (:5346-5354), and the accepted-increment predicate is decided from neither." :size 11)
      (htxt 40 100 "(a) the two records, key by key" :weight "bold" :size 11)
      (htxt 40 118 "(long shas shortened to 8)" :size 10)
      r-svg
      (htxt 40 (+ y1 10) "(b) what feeds what" :weight "bold" :size 11)
      f-svg
      (htxt 40 (+ y2 14) "On 76/002 the witness read :resolved? false because the store returned the entity's :props as a string (the rescue-stringify the comment at :2814-2829 describes), so the outcome is :grounded-no-change" :size 10)
      (htxt 40 (+ y2 32) "while the predicate on the same close is :accepted? true. On 71/002 the witness read :resolved? true and the outcome is :grounded-change while the predicate is :refused. The two verdicts share no input." :size 10)
      (htxt 40 (+ y2 56) "Evidence only: every value is read from 005, 006 or 007 of the named attempt; the rules quoted are the code at the cited lines." :size 9)))))

;; ---------------------------------------------------------------------------
;; Figure 3 — the accepted-increment predicate on 76/002 (true) and 71/002 (refused)
;; ---------------------------------------------------------------------------

(defn d-task-record
  "The enactment file the comparison names, re-read and digest-checked as
   retain-token-outcome! does (:3134-3140). nil when the digest differs."
  [cmp]
  (let [{:keys [path sha256]} (:measurement-source cmp)
        f (io/file path)]
    (when (and path (.isFile f))
      (let [bytes (Files/readAllBytes (.toPath f))]
        (when (= sha256 (hex (.digest (MessageDigest/getInstance "SHA-256") bytes) 32))
          (edn/read-string {:default (fn [t v] (tagged-literal t v))} (String. bytes "UTF-8")))))))

(defn reevaluate-71
  "NOT A RECORD VALUE: evaluate-close at today's HEAD on 71/002's recorded
   inputs, built the way the runner at :4030-4102 builds them: the 005
   binding, the enactment file's after-token rows filtered to the chain
   head's declared products (the decision recorded no :enacted-steps, so
   the runner at :4022-4102 would fall back to the head), today's
   acceptance-of for the target, and the artifact sha as after-revision."
  [c71 b71]
  (let [j (judgment c71)
        cmp (:token-outcome-comparison j)
        head (get-in j [:occurrence :action/value :precedence 0])
        declared (set (map second (:produces head)))
        record (d-task-record cmp)
        rows (vec (for [row (:after-token-evidence record)
                        :when (contains? declared (second (:token row)))]
                    {:token (:token row) :measurement row}))
        t (get-in j [:occurrence :action/value :target])
        acceptance (cs/acceptance-of t)
        verdict (ai/evaluate-close {:binding (get-in (judgment b71) [:validation :artifact-binding])
                                    :token-rows rows
                                    :acceptance acceptance
                                    :after-revision (:artifact-sha cmp)
                                    :declared-tokens (vec (sort-by pr-str declared))})]
    {:record-found? (some? record) :declared declared :rows rows :acceptance acceptance :verdict verdict
     :head-id (:id head) :enacted-steps (get-in j [:occurrence :action/value :enacted-steps] :no-key)}))

(defn fig3-predicate [c76 c71 b71 fx-71 head-sha]
  (let [j76 (judgment c76) j71 (judgment c71)
        ab76 (get-in (judgment (read-record (str dir-76 "005-build.edn"))) [:validation :artifact-binding])
        a76 (:accepted-increment j76) a71 (:accepted-increment j71)
        ev (:evidence a76)
        [ptok pres] (first (:produced-token-results ev))
        cres (:acceptance-result ev)
        cmp76 (:token-outcome-comparison j76)
        re (reevaluate-71 c71 b71)
        rv (:verdict re)
        fx-verdict (ai/evaluate-close {:binding (get-in fx-71 [:build :validation :artifact-binding])
                                       :token-rows (get-in fx-71 [:token-outcome-comparison :tokens])
                                       :acceptance (get-in fx-71 [:selection :controller-decision :action :accepted-increment :acceptance])
                                       :after-revision (get-in fx-71 [:adjudication :build-match :commit])})
        width 1240
        [p-svg y1] (lines 50 136
                          [(str ":accepted? " (:accepted? a76) "   :criterion-step " (pr-str (:criterion-step a76)) "   :measured-tokens " (pr-str (mapv token-name (:measured-tokens a76))))
                           (str "(a) fresh binding (accepted_increment.clj:63-69): :evidence :binding " (pr-str (update-vals (:binding ev) short-sha)) " — the two shas only; the three verdict flags (a) tested are not on this record.")
                           (str "     4c7142d8 (21:46:31Z 09-23, two minutes after this close) records them on the success branch. On 005-build's binding here they read descendant? " (get-in ab76 [:descendant?])
                                " corroborates? " (get-in ab76 [:corroborates?]) " claim-in-author-window? " (get-in ab76 [:claim-in-author-window?]) ".")
                           (str "(b) declared products observed (:71-76): " (token-name ptok) " :observed " (:observed pres) " :check " (:check pres) "  at :sha " (short-sha (get-in pres [:evidence :sha])) " = :resolved-sha " (short-sha (get-in pres [:evidence :resolved-sha])) "  :decl " (pr-str (get-in pres [:evidence :decl])))
                           (str "     the locator is the measurement row's :after-locator (evaluate-close :153), whose :sha is the artifact sha; equal to the comparison's :artifact-sha " (short-sha (:artifact-sha cmp76)) ": " (= (get-in pres [:evidence :resolved-sha]) (:artifact-sha cmp76)))
                           (str "(c) the target's own acceptance observed (:78-82): :acceptance-token " (:acceptance-token ev) " :observed " (:observed cres) "  at :sha " (pr-str (get-in cres [:evidence :sha])) " → :resolved-sha " (short-sha (get-in cres [:evidence :resolved-sha])) "  :decl " (pr-str (get-in cres [:evidence :decl])))
                           "     the locator is the cascade source's declared want locator (cascade-sources/acceptance-of, :4094-4095), :sha \"HEAD\" as declared; resolve-reference (observation_checks.clj:34-39) ran git rev-parse at close time."
                           (str "     " (short-sha (get-in cres [:evidence :resolved-sha])) " is dated " (git-date (get-in cres [:evidence :resolved-sha])) "; the artifact " (short-sha (:artifact-sha cmp76)) " is dated " (git-date (:artifact-sha cmp76)) " (git show here).")
                           "     AR-23 names this: (c) was read at a later commit than (b) and the comparison."
                           "order of the cond (:83-119): nil acceptance → :no-acceptance-declared; ¬a → :failed :a; b-bad → :failed :b; declared-but-unmeasured → :failed :b (:105-110); ¬c → :failed :c; else true. Here all three held."])
        [r-svg y2] (lines 50 (+ y1 44)
                          [(str ":accepted-increment on the close: " (pr-str a71))
                           "the runner at that close (b8bc1d7c, 19:30:49Z 09-22 → replaced by 74dc5de1, 22:12:59Z, one hour after the close) built :produced-tokens with"
                           "   (keep (fn [[token row]] …) (get-in token-comparison [:receipt :tokens]))  — destructuring each row MAP of compare-outcomes' vector as a [token row] pair (git show 74dc5de1)."
                           "   nth on a map is the exception; the try/catch at that site typed it :refused :predicate-evaluation-failed and the close proceeded (\"evidence, never a gate\", :4012-4014)."
                           (str "the same close's comparison, :artifact-sha " (short-sha (:artifact-sha (:token-outcome-comparison j71))) ": " (str/join ";  " (for [row (:tokens (:token-outcome-comparison j71))] (str (token-name (:token row)) " " (:predicted row) " / " (:observed row) " → " (:verdict row)))))
                           "   (predicted / observed → verdict); each row's :measurement :after-locator carries the artifact sha, which is what evaluate-close :153 now reads."
                           "the 74dc5de1 fixture test/fixtures/accepted-increment/machinery-71-attempt-002.edn (\"verbatim values at the listed judgment paths\") replays these inputs through evaluate-close;"
                           (str "   run here: :accepted? " (pr-str (:accepted? fx-verdict)) " :reason " (pr-str (:reason fx-verdict)) " — the fixture's acceptance is the selection's own slot, nil on this decision")
                           "   (the acceptance-of fallback landed 8527577e, 03:19:06Z 09-23). Not a record value; pinned by the test real-live-row-maps-preserve-both-observations-and-revision."])
        [h-svg y3] (lines 50 (+ y2 44)
                          [(str "inputs: binding = 005's :artifact-binding; rows = the enactment file (" (if (:record-found? re) "digest equal to the recorded :measurement-source :sha256" "NOT FOUND or digest changed") ") filtered to the chain head's :produces " (pr-str (vec (:declared re))))
                           (str "   (the decision recorded :enacted-steps " (pr-str (:enacted-steps re)) " → :chain-head-fallback); acceptance = acceptance-of " (pr-str (get-in (judgment c71) [:occurrence :action/value :target])) " today; after-revision = " (short-sha (:artifact-sha (:token-outcome-comparison j71))))
                           (str "acceptance-of today: :token " (pr-str (get-in re [:acceptance :token])) " :locator :sha " (pr-str (get-in re [:acceptance :locator :sha])) " :decl " (pr-str (get-in re [:acceptance :locator :decl])))
                           (str "   from resources/wm/cascade-sources/M-f11-find-production-successor.edn, sha256 " (short-id (get-in re [:acceptance :provenance :source-sha256])) " — the sha the 09-24 tick's :declaration-reads records for that file (figure 6's record).")
                           (str "verdict: :accepted? " (pr-str (:accepted? rv)) (when (:failed rv) (str " :failed " (:failed rv))) (when (:reason rv) (str " :reason " (:reason rv))))
                           (str "   (b): " (str/join "; " (for [[tok r] (get-in rv [:evidence :produced-token-results])] (str (token-name tok) " observed " (:observed r) " at " (short-sha (get-in r [:evidence :resolved-sha]))))))
                           (str "   (c): observed " (pr-str (get-in rv [:evidence :acceptance-result :observed])) " at :sha " (pr-str (get-in rv [:evidence :acceptance-result :evidence :sha])) " → " (short-sha (get-in rv [:evidence :acceptance-result :evidence :resolved-sha])) " = this checkout's HEAD " (short-sha head-sha) ": " (= head-sha (get-in rv [:evidence :acceptance-result :evidence :resolved-sha])))
                           "   the (c) leg moves with HEAD: the same recorded inputs give an acceptance read at whatever commit the checkout is on when the predicate runs. That is the floating-HEAD read of AR-23 on a mission target."])
        height (+ y3 70)]
    (hsvg
     width height
     (str
      (htxt 40 36 "The accepted-increment predicate — three conjuncts on 76/002 (true) and the refusal on 71/002, with the revision each leg was observed at" :size 15 :weight "bold")
      (htxt 40 58 "accepted-increment (accepted_increment.clj:34-137) answers for ONE occurrence: (a) the reviewed commit is bound fresh to it, (b) the enacted step's declared products read true at the after-revision, (c) the target's own" :size 11)
      (htxt 40 76 "acceptance declaration reads true. evaluate-close (:139-160) adapts the comparison's row maps and types any exception :refused. The runner calls it at :4065 (inputs :4066-4100) and records the result ON the close (:4137)." :size 11)
      (htxt 40 100 (str "(a) 76/002, [:payload :judgment :accepted-increment] (" dir-76 "007-closed.edn)") :weight "bold" :size 11)
      (htxt 40 118 "(long shas shortened to 8)" :size 10)
      p-svg
      (htxt 40 (+ y1 20) (str "(b) 71/002, the refused verdict and why (" dir-71 "007-closed.edn)") :weight "bold" :size 11)
      r-svg
      (htxt 40 (+ y2 20) (str "(c) NOT A RECORD VALUE: evaluate-close at HEAD " (short-sha head-sha) " on 71/002's recorded inputs, built the way the runner at :4022-4102 builds them today") :weight "bold" :size 11)
      h-svg
      (htxt 40 (+ y3 14) "What the records say: one close where (a), (b) and (c) all read true, (c) at a commit three minutes after (b)'s; one close whose predicate never evaluated its conjuncts, typed :refused by the adapter's own shape error." :size 10)
      (htxt 40 (+ y3 32) "What they do not say: what 71/002's conjuncts would have read at c8c65230 — the runner then had no acceptance for a mission target, and (c) at HEAD on any later day is a different event." :size 10)
      (htxt 40 (+ y3 54) "Evidence only: (a) and (b) are read from the closes and git history; (c) is computed here and labelled so." :size 9)))))

;; ---------------------------------------------------------------------------
;; Figure 4 — the value domain of :accepted? across the fourteen closes
;; ---------------------------------------------------------------------------

(defn fig4-domain [rows]
  (let [fmt "%-7s %-9s %-20s %-20s %-24s %-7s %-48s %-46s %s"
        hdr (format fmt "close" "sha8" "recorded-at" "outcome" ":accepted?" ":failed" ":reason" ":criterion-step" "(c) :sha → :resolved-sha")
        line (fn [r] (format fmt (:label r) (:sha8 r) (subs (:recorded-at r) 0 19) (str (:outcome r)) (:accepted r) (if (:failed r) (str (:failed r)) "") (if (:reason r) (str (:reason r)) "") (:criterion r)
                             (if (:c-sha r) (str (pr-str (:c-sha r)) " → " (:c-resolved r)) "")))
        dom (into (sorted-map) (frequencies (map :accepted rows)))
        width 1240
        [t-svg y1] (lines 50 118 (cons hdr (map line rows)))
        [d-svg y2] (lines 50 (+ y1 44)
                          (concat
                           [(str "domain over the fourteen closes: " (str/join "   " (for [[k v] dom] (str k " × " v))))
                            "true          1   76/002 — the one close where the predicate's three conjuncts all held (figure 3)."
                            "false         7   72/002 73/001 74/002 75/001 :failed :c (acceptance read false at HEAD); 73/002 74/001 :failed :b (declared product read false at the artifact sha); 76/001 :failed :c."
                            ":refused      1   71/002 — the adapter's shape error (figure 3(b)); a keyword where the docstring promises true/false/:no-acceptance-declared, added by the try/catch at evaluate-close :157-160."
                            ":no-acceptance-declared  2   72/001 (a mission target, before the acceptance-of fallback of 8527577e) and 75/002 (an :agent-unavailable close with no action, so no target, so nil acceptance)"
                            "                             — a keyword in the :accepted? position by design (:84-87)."
                            "no key        3   70/001 70/002 71/001 — closed before b8bc1d7c (19:30:49Z 09-22) introduced the predicate; the judgment has no :accepted-increment key at all."
                            ""
                            "AR-25 names 72/001 (:no-acceptance-declared) and 71/001 (no key). The same two shapes recur on 75/002 and on 70/001, 70/002; and :refused is a third non-boolean, on 71/002."
                            "b-update (full_loop_runner.clj:4245-4246) tests (true? :accepted?): every non-true value, boolean or not, is \"not accepted\" there, and retained/b-update.edn's :post-close-update copies the raw value (:4289)."
                            ":criterion-step and :measured-tokens (:4101-4102) appear from 73/002 on (a651e3a4, 17:14:15Z 09-23); before that the verdict names no step. (c)'s :sha is \"HEAD\" wherever (c) was read; its :resolved-sha differs per close."]))
        height (+ y2 60)]
    (hsvg
     width height
     (str
      (htxt 40 36 "The value domain of [:payload :judgment :accepted-increment :accepted?] — every close of machinery-70..76" :size 15 :weight "bold")
      (htxt 40 58 "accepted-increment's docstring (:34-60) promises {:accepted? true …}, {:accepted? false :failed … }, or {:accepted? :no-acceptance-declared …}; evaluate-close adds {:accepted? :refused …} on any exception." :size 11)
      (htxt 40 76 "The fourteen closes read as follows (the census digest in this file's name is the sha256 of the fourteen close shas, in the order below)." :size 11)
      (htxt 40 100 "(a) the table" :weight "bold" :size 11)
      t-svg
      (htxt 40 (+ y1 20) "(b) the domain, and which code change each shape dates from" :weight "bold" :size 11)
      d-svg
      (htxt 40 (+ y2 14) "Evidence only: each cell is the recorded value at the named key path, or the words \"no key\"; nothing is normalised." :size 9)))))

;; ---------------------------------------------------------------------------
;; Figure 5 — the run-ending classification and the close's carriers
;; ---------------------------------------------------------------------------

(defn fig5-carriers [rows c76]
  (let [fmt "%-7s %-21s %-19s %-10s %-9s %-14s %-9s %-17s %-9s %-6s %-16s %-9s %-5s %s"
        hdr (format fmt "close" "run-ending" "" "comparison" "artifact" "verdicts" "kernel" "observations" "receipt" "pairs" "route" "b-update" "ret." "man")
        hdr2 (format fmt "" ":class" ":missing" ":status" "" "" ":status" "state rows true" "cnt/held" "key?" ":status/incr" "key?" "files" "")
        abbrev {:observation-missing "miss" :predicted-and-observed "p+o" :predicted-not-observed "p-o" :not-predicted-observed "-p+o" :neither "neither"}
        line (fn [r] (format fmt (:label r)
                             (if (:rec-present? r) (str (:rec-class r)) "nil")
                             (if (:rec-present? r) (str/join "," (map name (or (:rec-missing r) []))) "")
                             (if (:cmp-present? r) (name (:cmp-status r)) "nil")
                             (if (:cmp-present? r) (or (:artifact r) "nil") "")
                             (if (:cmp-present? r) (str/join "," (for [[k v] (:verdicts r)] (str (get abbrev k (name k)) " " v))) "")
                             (if (:ke-present? r) (name (:ke-status r)) "nil")
                             (if (:ke-present? r) (str (name (or (:ke-obs r) :none)) " " (:ke-rows r) " " (:ke-true r)) "")
                             (if (:rcpt-present? r) (str (:counted r) "/" (:held r)) "nil")
                             (str (:pairs? r))
                             (str (name (or (:ra-status r) :none)) "/" (:ra-increments r))
                             (str (:b-update? r))
                             (str (count (:retained r)))
                             (str (:manifest r))))
        rec76 (:run-ending-classification (judgment c76))
        re-run (run-ending/classify {:close (judgment c76)
                                     :occurrence (:occurrence (judgment c76))
                                     :route-attestation (:route-attestation (judgment c76))
                                     :focus-receipt nil})
        classes (into (sorted-map) (frequencies (map #(if (:rec-present? %) (:rec-class %) :nil) rows)))
        width 1240
        [t-svg y1] (lines 50 118 (list* hdr hdr2 (map line rows)))
        [c-svg y2] (lines 50 (+ y1 44)
                          [(str "classes over the fourteen: " (str/join "   " (for [[k v] classes] (str k " × " v))) ".  :known-typed-failure needs grounded? false ∧ artifact-only? false ∧ a keyword :failure-kind (:89-91): 70/001, 70/002.")
                           ":unknown with :missing [:attested-increment] (:103-113): no route declaration was supplied on any of these clicks (:route-attestation :status :none-declared, route_attestation.clj:118), so :increments is [] and"
                           "   qualifying-increments (:60-65) finds nothing. The declarations resource (c6fa1ab2, 03:48:32Z 09-24) postdates every close here; 77/001 is the first with :status :declared (figure 6)."
                           "nil (75/002): retain-run-ending! runs only (when (and cohort? @action-occurrence)) (:4151); an :agent-unavailable close has no occurrence, so the key is present with value nil, as are comparison, kernel, receipt."
                           "the projection (resources/wm/run-ending-classification-v1.edn): :close-judgment-keys [:outcome :grounded? :artifact-only? :failure-kind :occurrence :route-attestation :route-attestation-ref];"
                           (str "   76/002's :close-projection-sha256 " (short-id (:close-projection-sha256 rec76)) " over exactly those keys of its judgment.")
                           (str "NOT A RECORD VALUE: run-ending/classify re-run here on 76/002's closed judgment: :class " (:class re-run) " :missing " (pr-str (:missing re-run)) "; :close-projection-sha256 equal to the record: " (= (:close-projection-sha256 re-run) (:close-projection-sha256 rec76)) ";")
                           (str "   verify-close (:149-156) on the record's own receipt: " (run-ending/verify-close c76 rec76) ". The re-run's :input-sha256 equal to the record's: " (= (:input-sha256 re-run) (:input-sha256 rec76)) " — the focus receipt lives on the selection's certificate,")
                           "   not the close, so the record's input digest included it and this re-run's did not."])
        [k-svg y3] (lines 50 (+ y2 44)
                          ["comparison   :token-outcome-comparison — compare-outcomes (token_outcome.clj:48-82), present on 13; root and route copies equal on all 13 (OBS-D R2.1), checked here:"
                           (str "             " (str/join ", " (map #(str (:label %) " " (:ra-cmp-equal? %)) (filter :cmp-present? rows))))
                           (str "             75/002's route copy is " (pr-str (some #(when (= "75/002" (:label %)) (:ra-cmp-status %)) rows)) " :comparison-not-supplied (route_attestation.clj:117).")
                           "kernel       :kernel-example — kernel_example/collect, present on 13; :observations :admitted on 11 (64 booleans), :unavailable on 70/001 and 70/002 (:task-execution-incomplete)."
                           "receipt      :learning-trial-receipt — attempt-learning/receipt after record!, present on 13: 11 counted (appended) trials, 25 held."
                           "pairs        :token-outcome-pairs on the comparison — token_outcome_pair (54295ca0, 05:31:14Z 09-24): on none of the fourteen."
                           "b-update     :b-update on the judgment — close-b-update (4a2ba931, 05:33:52Z 09-24): on none of the fourteen; retained/b-update.edn likewise absent from every retained/ listing."
                           "route        :route-attestation :status/:increments; :route-attestation-ref names retained/route-attestation.edn with its sha256 (equal to the file, checked in figure 1 for two closes)."
                           "ret./man     count of retained/ files; count of :close-evidence-manifest entries (six checkpoint files + evidence/ + five retained receipts on a full close; 0 with no occurrence)."])
        height (+ y3 70)]
    (hsvg
     width height
     (str
      (htxt 40 36 "The run-ending classification and the close's carriers — what is present on which close" :size 15 :weight "bold")
      (htxt 40 58 "run-ending/classify (run_ending_classification.clj:67-137) is record-only: it projects seven keys of the close judgment, joins the route's qualifying increments and the focus receipt, and names a class or :unknown + :missing." :size 11)
      (htxt 40 76 "The carriers are the maps the judgment holds beside the verdicts. \"nil\": key present, value nil; \"false\" under key?: no key. Verdicts: miss = :observation-missing, p+o = :predicted-and-observed, p-o = :predicted-not-observed." :size 11)
      (htxt 40 100 "(a) the table" :weight "bold" :size 11)
      t-svg
      (htxt 40 (+ y1 20) "(b) the classification" :weight "bold" :size 11)
      c-svg
      (htxt 40 (+ y2 20) "(c) the carriers, and where each comes from" :weight "bold" :size 11)
      k-svg
      (htxt 40 (+ y3 14) "The close with :accepted? true is classified :unknown, missing an attested increment, like every other grounded close; the classification and the predicate read different fields and neither reads the other." :size 10)
      (htxt 40 (+ y3 32) "Evidence only: presence, statuses and counts are read from the fourteen files; the one re-run is labelled." :size 9)))))

;; ---------------------------------------------------------------------------
;; Figure 6 — the abstained close of 2026-09-24-1790225596
;; ---------------------------------------------------------------------------

(defn count-in [path words]
  (let [s (slurp path)]
    (into (sorted-map) (for [w words] [w (count (re-seq (re-pattern (java.util.regex.Pattern/quote w)) s))]))))

(defn fig6-abstained [t c77 p77 f scan-sha]
  (let [j77 (judgment c77)
        sel (get-in p77 ["002-selection" :payload :sorry])
        refusals (get-in sel [:decision :refusals])
        t-repair (first (filter #(= target (:target %)) refusals))
        words ["no-constructed-candidate" "new-wanted-token-within-horizon" "no-new-wanted-token" "environmental-hold" "abstained"]
        counts (into (sorted-map) (for [[label path] [["tick .edn" tick-0924] ["tick .scan.md" scan-0924] ["77/001 002-selection" (str dir-77 "002-selection.edn")] ["77/001 007-closed" (str dir-77 "007-closed.edn")] ["finding" finding]]]
                                    [label (count-in path words)]))
        present (fn [k] (if (contains? t k) (let [v (get t k)] (cond (nil? v) "present, nil" (map? v) (str "map " (count v) " keys") (coll? v) (str "coll " (count v)) :else (pr-str v))) "ABSENT"))
        t-decl (first (filter #(= target (:target %)) (get-in t [:declaration-reads :occurrences])))
        top-keys (vec (sort (keys t)))
        [tk1 tk2 tk3] (partition-all 9 top-keys)
        width 1240
        [t-svg y1] (lines 50 136
                          [(str "top-level keys (" (count top-keys) "): " (str/join " " tk1))
                           (str "     " (str/join " " tk2))
                           (str "     " (str/join " " tk3))
                           (str ":run/id " (:run/id t) "   :startedAt " (:startedAt t) "   :click/id " (:click/id t) "   :selectorSeam " (pr-str (:selectorSeam t)) "   :traceWritten " (:traceWritten t))
                           (str ":route " (pr-str (:route t)))
                           (str ":repair/discharge " (pr-str (:repair/discharge t)) "   (repair_discharge.clj:184: no closed-event with an occurrence → :not-applicable; finalize-run! :210 returns it without recording an operation)")
                           (str ":decision keys " (pr-str (vec (sort (keys (:decision t))))) " — :g-term-decomposition " (pr-str (select-keys (get-in t [:decision :g-term-decomposition]) [:status :reason])))
                           "     no :selection-certificate, :selection-law, :initial-belief-receipt, :enumeration-completeness (persist-run-record! :561-564 select-keys a decision that has none of them)"
                           (str ":selection-event " (pr-str (:selection-event t)) "   :habit-reinforcement " (pr-str (select-keys (:habit-reinforcement t) [:reinforcement :delta :reason])))
                           (str ":scan-report " (pr-str (select-keys (:scan-report t) [:status :format :bytes])) " :path …/" (last (str/split (get-in t [:scan-report :path]) #"/")) " :sha256 " (short-id (get-in t [:scan-report :sha256])) " — equal to the file now: " (= (get-in t [:scan-report :sha256]) scan-sha))
                           (str ":execution-cohort " (pr-str (select-keys (:execution-cohort t) [:cohort-id])) "   :runner-execution/identity " (pr-str (get-in t [:runner-execution/identity :id])))
                           (str ":route-attestation-ref " (pr-str (select-keys (:route-attestation-ref t) [:status])) " → " (last (str/split (get-in t [:route-attestation-ref :path]) #"/")) "   :repair/publication " (count (:repair/publication t)) " entries, all " (pr-str (first (distinct (map :status (:repair/publication t))))))
                           (str ":live-c-coverage " (pr-str (:live-c-coverage t)) "   :mission-hole-coverage " (pr-str (:mission-hole-coverage t)) "   :habit-reads " (pr-str (:habit-reads t)))
                           (str ":declaration-reads " (count (get-in t [:declaration-reads :occurrences])) " occurrences over " (count (distinct (map :target (get-in t [:declaration-reads :occurrences])))) " targets; " (short-target target) "'s :restoration-accepted read "
                                (pr-str (get-in t-decl [:observations :results :restoration-accepted :observed])) " at :sha \"HEAD\" → " (short-sha (get-in t-decl [:observations :results :restoration-accepted :evidence :resolved-sha])) " (the checkout HEAD when the judge read the source)")
                           (str "keys on every full-loop close and NOT on this record: " (str/join "  " (map #(str % " " (present %)) [:accepted-increment :token-outcome-comparison :learning-trial-receipt :kernel-example])))
                           (str "     " (str/join "  " (map #(str % " " (present %)) [:run-ending-classification :checkpoints :close-retention :attempt-id])))
                           (str "no key of this record holds a decline carrier: " (str/join "  " (map #(str % " " (present %)) [:decline :abstention :findings :repair-obligation :failure-kind])))])
        [c-svg y2] (lines 50 (+ y1 44)
                          (concat
                           (for [[f r] p77]
                             (format "%-18s %-38s %s" f (:recorded-at r)
                                     (let [p (:payload r)] (cond (:sorry p) (str "sorry " (pr-str (select-keys (:sorry p) [:kind :outcome])) (when (get-in p [:sorry :decision :status]) (str " :decision :status " (get-in p [:sorry :decision :status]) ", " (count (get-in p [:sorry :decision :refusals])) " refusals")))
                                                                 :else (str "judgment keys " (count (keys (:judgment p))) ", ground " (pr-str (get-in p [:ground :kind])))))))
                           [(str "007-closed :outcome " (:outcome j77) " :failure-kind " (:failure-kind j77) " :grounded? " (:grounded? j77) " :occurrence " (pr-str (:occurrence j77)) " :witness " (pr-str (:witness j77)) " :run-ending-classification " (pr-str (:run-ending-classification j77)) " :kernel-example " (pr-str (:kernel-example j77)))
                            (str "           :accepted-increment " (pr-str (dissoc (:accepted-increment j77) :criterion-step :measured-tokens)))
                            (str "                               :criterion-step " (pr-str (get-in j77 [:accepted-increment :criterion-step])) " :measured-tokens " (pr-str (get-in j77 [:accepted-increment :measured-tokens])))
                            (str "           :route-attestation :status " (get-in j77 [:route-attestation :status]) " :increments " (pr-str (get-in j77 [:route-attestation :increments])) " :token-outcome-comparison " (pr-str (get-in j77 [:route-attestation :token-outcome-comparison])))
                            (str "           retained/: " (pr-str (dir-listing dir-77 "retained")) "   :close-retention " (pr-str (get-in c77 [:payload :close-retention])) "   :close-evidence-manifest " (pr-str (get-in c77 [:payload :close-evidence-manifest])))
                            (str "002-selection's sorry: :decision :refusals " (count refusals) " = " (str/join ", " (for [[k v] (into (sorted-map) (frequencies (map :kind refusals)))] (str k " × " v))) "; for " (short-target target) ": " (pr-str (dissoc t-repair :target)))]))
        [f-svg y3] (lines 50 (+ y2 44)
                          [(str ":repair/id " (:repair/id f) "   :repair/class " (:repair/class f) "   :repair/status " (:repair/status f) "   :opened-at " (:opened-at f))
                           (str ":failure-kind " (:failure-kind f) "   :failure-stage " (:failure-stage f) "   :failure-outcome " (:failure-outcome f) "   :failure-error " (pr-str (:failure-error f)) "   :target " (pr-str (:target f)))
                           (str ":discharge-contract " (pr-str (:discharge-contract f)))
                           "     (repair-class-for :abstained → :environmental-hold, full_loop_runner.clj:3489-3496; discharge-contract :3520-3538)"
                           (str ":repair/occurrence " (pr-str (select-keys (:repair/occurrence f) [:occurrence/id :occurrence/origin :occurrence/failure-kind])))
                           (str ":attempt-id " (:attempt-id f) "   :backtrace :phase-events " (count (get-in f [:backtrace :phase-events])) " events")])
        [w-svg y4] (lines 50 (+ y3 44)
                          (cons (format "%-24s %s" "file" (str/join "  " (map #(format "%-33s" %) words)))
                                (for [[label cs] counts] (format "%-24s %s" label (str/join "  " (map #(format "%-33s" (get cs %)) words))))))
        height (+ y4 90)]
    (hsvg
     width height
     (str
      (htxt 40 36 "The abstained close of 2026-09-24-1790225596 — what the run record carries, what the attempt directory carries, and what neither does" :size 15 :weight "bold")
      (htxt 40 58 "The judge abstained (every target refused a candidate); the runner threw at :4647-4650 with :outcome :abstained, and the close was built by close! from the catch (:5473-5500) with :not-reached-* sorries for 003-006." :size 11)
      (htxt 40 76 "persist-run-record! (:515-616) wrote the tick record from the wrapper result; the attempt directory is machinery-77 attempt-001, the first close after c6fa1ab2 (route declarations) and the newest close in the corpus." :size 11)
      (htxt 40 100 (str "(a) " tick-0924) :weight "bold" :size 11)
      (htxt 40 118 "(what persist-run-record! :515-616 wrote from the wrapper result; long shas shortened to 8)" :size 10)
      t-svg
      (htxt 40 (+ y1 20) (str "(b) " dir-77 " — the same run's close") :weight "bold" :size 11)
      c-svg
      (htxt 40 (+ y2 20) (str "(c) " finding " — the finding opened for it") :weight "bold" :size 11)
      f-svg
      (htxt 40 (+ y3 20) "(d) where the decline words appear: exact substring counts in each file (AR-16's claim is that the per-candidate reason survives only in the scan markdown)" :weight "bold" :size 11)
      w-svg
      (htxt 40 (+ y4 14) "What the records say: a tick record with a one-hop :abstained route and a :not-applicable discharge, no selection certificate, no close carriers; a close whose 002-selection sorry carries every target's refusal kind" :size 10)
      (htxt 40 (+ y4 32) "(:no-constructed-candidate :missing [:new-wanted-token-within-horizon] for the reference ticket, in the EDN); a finding typed :environmental-hold from :abstained. The predicate ran: :no-acceptance-declared on a nil action." :size 10)
      (htxt 40 (+ y4 50) "What they do not say: that the want was already true. The tick's :declaration-reads shows :restoration-accepted observed true at HEAD; the reader joins that to :new-wanted-token-within-horizon, the record does not." :size 10)
      (htxt 40 (+ y4 74) "Evidence only: presence and absence are `contains?` on the read record; the substring counts are exact matches on the files' bytes." :size 9)))))

;; ---------------------------------------------------------------------------
;; Figure 7 — narrative: 71/002 from build verdict to a refused close whose row entered the next day's selection
;; ---------------------------------------------------------------------------

(defn ledger-forms [path]
  (with-open [r (java.io.PushbackReader. (io/reader path))]
    (loop [acc []]
      (let [form (edn/read {:eof ::eof :default (fn [t v] (tagged-literal t v))} r)]
        (if (= ::eof form) acc (recur (conj acc form)))))))

(defn fig7-narrative [p71 c71 t0923 forms]
  (let [b (judgment (get p71 "005-build")) ad (judgment (get p71 "006-adjudication")) j (judgment c71)
        cmp (:token-outcome-comparison j)
        rt (first (get-in j [:learning-trial-receipt :trials]))
        idx (.indexOf (mapv :identity forms) (get-in rt [:ledger :identity]))
        c1 (get-in t0923 [:decision :selection-certificate :candidates 0])
        p0 (get-in c1 [:id :precedence 0])
        selected-at (subs (str (get-in t0923 [:decision :selection-certificate :token-belief-stage :occurrence-id])) (count "wm-live-selection-"))
        steps [["build" (:recorded-at (get p71 "005-build"))
                (str "005-build: :commits " (pr-str (mapv short-sha (:commits b))) ", :revision :round " (get-in b [:revision :round]) "; the reviewer's round-2 text opens " (pr-str (subs (str (get-in b [:validation :review-text])) 0 26)) "… ; :approved? " (get-in b [:validation :approved?]) "; review-gate :passed? " (get-in b [:validation :review-gate :passed?]) ".")
                (str ":artifact-binding :commit " (short-sha (get-in b [:validation :artifact-binding :commit])) " :pre-dispatch-head " (short-sha (get-in b [:validation :artifact-binding :pre-dispatch-head])) " :descendant? " (get-in b [:validation :artifact-binding :descendant?]) " :corroborates? " (get-in b [:validation :artifact-binding :corroborates?]) " :claim-in-author-window? " (get-in b [:validation :artifact-binding :claim-in-author-window?]))]
               ["adjudication" (:recorded-at (get p71 "006-adjudication"))
                (str "006-adjudication: :build-match " (pr-str (update (:build-match ad) :commit short-sha)) "; ground-commit! wrote " (get-in ad [:witness :implementation-id]) " and read it back: :resolved? " (get-in ad [:witness :resolved?]) " :dial-moved? " (get-in ad [:witness :dial-moved?]) ".")
                "so the outcome will be :grounded-change (:5346-5354)."]
               ["comparison" (str "artifact " (short-sha (:artifact-sha cmp)))
                (str "compare-outcomes (predicted / observed → verdict): " (str/join "; " (for [row (:tokens cmp)] (str (token-name (:token row)) " " (:predicted row) " / " (:observed row) " → " (:verdict row)))) ".")
                (str "attempt-learning/receipt admits one trial (" (token-name (:effect rt)) "), record! appends it as ledger form " (inc idx) ": identity " (short-id (get-in rt [:ledger :identity])) ", :counted? " (:counted? rt) ", :ledger :status " (get-in rt [:ledger :status]) ".")]
               ["predicate" "after the comparison, before the file (:4011-4102)"
                (str "evaluate-close → " (pr-str (:accepted-increment j)))
                "the adapter of that hour destructured row maps as pairs (74dc5de1's diff); nth on a map threw; the catch typed it. No conjunct was evaluated."]
               ["close" (:recorded-at c71)
                (str "007-closed: :outcome " (:outcome j) " :grounded? " (:grounded? j) "; :run-ending-classification :class " (get-in j [:run-ending-classification :class]) " :missing " (pr-str (get-in j [:run-ending-classification :missing])) "; :kernel-example :observations " (get-in j [:kernel-example :missingness :observations]) "; no :b-update key (" (contains? j :b-update) ").")
                "b-update does not run (:4245-4246 needs (true? :accepted?)); the appended row stays in the ledger; retained/ holds the five receipts and four job texts."]
               ["next day's selection" selected-at
                (str "tick 1790199409: pattern-theta(" (name (:id p0)) ") read the ledger, found the one row, and stamped :theta " (:theta p0) " :theta-source " (:theta-source p0) " on C1's precedence 0 for target " (short-target (get-in c1 [:id :target])) ",")
                (str ":theta-provenance " (pr-str (update (:theta-provenance p0) :identities #(mapv short-id %))) " — the refused close's row as this candidate's prior (AR-17; 04 §6).")]]
        width 1240 height (+ 110 (* 92 (count steps)) 60)]
    (hsvg
     width height
     (str
      (htxt 40 36 "A narrative case — machinery-71 attempt-002 from the build verdict to a refused close whose learning row entered the next day's selection" :size 15 :weight "bold")
      (htxt 40 58 "Every value with its source: 005, 006, 007 of 71/002; the ledger; tick-run-record 1790199409. Timestamps are the records' own (:recorded-at, the live-selection occurrence-id)." :size 10)
      (apply str
             (for [[i [label at l1 l2]] (map-indexed vector steps)
                   :let [y (+ 80 (* 92 i))]]
               (str (box 40 y (- width 80) 78 (if (= label "next day's selection") "#fdf6ec" "#f6f7f8") "#68717b")
                    (htxt 50 (+ y 20) (str label "   " at) :weight "bold")
                    (htxt 50 (+ y 40) l1 :size 10)
                    (htxt 50 (+ y 58) l2 :size 10 :mono true)
                    (when (pos? i) (arrow 120 (- y 14) 120 y)))))
      (htxt 40 (+ 90 (* 92 (count steps))) "What the records say: an approved, grounded, :grounded-change close whose accepted-increment is :refused by an adapter error, whose row was appended before the verdict and read the next day as 3/4." :size 10)
      (htxt 40 (+ 110 (* 92 (count steps))) "Evidence only: every value is read from the named files; the ledger index is a position in the file as it is today." :size 9)))))

;; ---------------------------------------------------------------------------

(defn generate! []
  (let [rows (mapv close-row attempts)
        digest (census-digest rows)
        c76 (read-record (str dir-76 "007-closed.edn"))
        c71 (read-record (str dir-71 "007-closed.edn"))
        b71 (read-record (str dir-71 "005-build.edn"))
        c77 (read-record (str dir-77 "007-closed.edn"))
        p71 (phase-records dir-71)
        p77 (phase-records dir-77)
        t0923 (read-record tick-0923)
        t0924 (read-record tick-0924)
        f (read-record finding)
        fx-71 (read-record fixture-71)
        forms (ledger-forms ledger-path)
        head-sha (git-head)
        h-76 (sha8 (str dir-76 "007-closed.edn")) h-76-006 (sha8 (str dir-76 "006-adjudication.edn")) h-76-005 (sha8 (str dir-76 "005-build.edn"))
        h-71 (sha8 (str dir-71 "007-closed.edn")) h-71-006 (sha8 (str dir-71 "006-adjudication.edn"))
        h-77 (sha8 (str dir-77 "007-closed.edn")) h-t23 (sha8 tick-0923) h-t24 (sha8 tick-0924) h-f (sha8 finding)
        scan-sha (file-sha256 scan-0924)]
    (spit-svg (str "fig1-phases-76002-" h-76-006 "-" h-76 ".svg") (fig1-phases dir-76 dir-71))
    (spit-svg (str "fig2-adjudication-" h-76-005 "-" h-76-006 "-" h-71-006 ".svg") (fig2-adjudication dir-76 dir-71))
    (spit-svg (str "fig3-predicate-" h-76 "-" h-71 ".svg") (fig3-predicate c76 c71 b71 fx-71 head-sha))
    (spit-svg (str "fig4-accepted-domain-census-" digest ".svg") (fig4-domain rows))
    (spit-svg (str "fig5-carriers-census-" digest ".svg") (fig5-carriers rows c76))
    (spit-svg (str "fig6-abstained-" h-t24 "-" h-77 "-" h-f ".svg") (fig6-abstained t0924 c77 p77 f scan-sha))
    (spit-svg (str "fig7-narrative-" h-71 "-" h-t23 ".svg") (fig7-narrative p71 c71 t0923 forms))
    (println "HEAD" head-sha)
    (doseq [r rows] (println (:label r) (:sha r) (:recorded-at r) (:outcome r) (:accepted r)))
    (println "census digest" digest)
    (println "close-77" (file-sha256 (str dir-77 "007-closed.edn")))
    (println "tick-0923" (file-sha256 tick-0923))
    (println "tick-0924" (file-sha256 tick-0924) "scan" scan-sha)
    (println "finding" (file-sha256 finding))
    (println "ledger forms" (count forms))
    (println "accepted domain" (pr-str (into (sorted-map) (frequencies (map :accepted rows)))))
    (println "classes" (pr-str (into (sorted-map) (frequencies (map #(if (:rec-present? %) (:rec-class %) :nil) rows)))))
    (println "pairs keys" (pr-str (frequencies (map :pairs? rows))) "b-update keys" (pr-str (frequencies (map :b-update? rows))))
    (println "route copies equal" (pr-str (frequencies (map :ra-cmp-equal? (filter :cmp-present? rows)))))
    (println "counted/held" (reduce + (map :counted rows)) "/" (reduce + (map :held rows)))
    (println "kernel rows/true" (reduce + (map :ke-rows rows)) "/" (reduce + (map :ke-true rows)))))
