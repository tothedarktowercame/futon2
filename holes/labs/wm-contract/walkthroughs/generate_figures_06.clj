;; generate_figures_06.clj — regenerate every figure in 06-discharge.md from the
;; records. Run from the futon2 checkout root:
;;
;;   clojure -M -i holes/labs/wm-contract/walkthroughs/generate_figures_06.clj \
;;           -e "(generate-figures-06/generate!)"
;;
;; Read-only: reads every .edn under data/wm-repair-obligations/ (nine
;; subdirectories), three tick records, one attempt close, one ticket, one
;; cascade source, one recheck resource, and the code's own git history for a
;; fixed list of shas; writes only SVGs beside this script whose names start
;; with fig1-store- … fig6-narrative-444fb018-. Every figure file name carries
;; the short raw sha256 of the record(s) it was drawn from; the store figures
;; carry a digest over every store file's sha256. Where a figure shows what the
;; code computes on the store today — repair/open-obligations called in this
;; fresh process — the figure says so and labels it as not a record value.
;; Nothing is written back to any record; no runner function that writes is
;; called; the store namespace is required for its one read-only query.
(ns generate-figures-06
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [futon2.aif.repair-obligation :as repair])
  (:import [java.nio.file Files]
           [java.security MessageDigest]))

(def here "holes/labs/wm-contract/walkthroughs")
(def root "data/wm-repair-obligations")
(def children ["findings" "implementations" "resolutions" "dismissals" "verifications"
               "verification-evidence" "discharge-operations" "ticket-links" "occurrence-evidence"])

(def tick-0922 "data/wm-runs/tick-run-record-2026-09-22-1790053967.edn")
(def tick-0923 "data/wm-runs/tick-run-record-2026-09-23-1790199409.edn")
(def tick-0924 "data/wm-runs/tick-run-record-2026-09-24-1790225596.edn")
(def close-70-002 "data/wm-full-loop-machinery-70/wm-contract-machinery-70-v1/attempt-002/007-closed.edn")
(def sel-76-002 "data/wm-full-loop-machinery-76/wm-contract-machinery-76-v1/attempt-002/002-selection.edn")
(def close-76-002 "data/wm-full-loop-machinery-76/wm-contract-machinery-76-v1/attempt-002/007-closed.edn")

(def id-444 "repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade")
(def id-ad16 "repair-occ-ad16e2c2e10f83412628d1eec8e222018afa399fba64016124e32f3a812a3214")
(def finding-444 (str root "/findings/" id-444 ".edn"))
(def finding-ad16 (str root "/findings/" id-ad16 ".edn"))
(def link-444 (str root "/ticket-links/" id-444 ".edn"))
(def link-ad16 (str root "/ticket-links/" id-ad16 ".edn"))
(def ticket-444 (str "holes/tickets/T-" id-444 ".md"))
(def ticket-ad16 (str "holes/tickets/T-" id-ad16 ".md"))
(def op-c884 (str root "/discharge-operations/c884c9c2c775f9d3cd8ee70011a454473660b5a3eafea6dac6c23a17f0c9d39c.edn"))
(def source-444 "resources/wm/cascade-sources/T-repair-occ-444fb018.edn")
(def recheck-444 "resources/wm/rechecks/repair-occ-444fb018-dated-recheck.edn")
(def queue-path "data/wm-ticket-queue/queue.edn")

;; ---------------------------------------------------------------------------
;; reading and hashing (same helpers as walkthroughs 01-05)
;; ---------------------------------------------------------------------------

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
    (if (zero? exit) (subs (str/trim out) 0 19) (str "not a commit here: " sha))))

(defn git-head []
  (str/trim (:out (sh/sh "git" "rev-parse" "HEAD"))))

(defn git-log-file
  "Commits touching PATH, oldest first, as [sha8 date subject] (read-only)."
  [path]
  (let [{:keys [out]} (sh/sh "git" "log" "--reverse" "--format=%h|%ci|%s" "--" path)]
    (vec (for [l (str/split-lines (str/trim out)) :when (seq l)]
           (let [[h d s] (str/split l #"\|" 3)] [h (subs d 0 19) s])))))

;; ---------------------------------------------------------------------------
;; svg helpers (same style as walkthroughs 01-05)
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

(defn- hsvg [width height body]
  (str "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" width "\" height=\"" height
       "\" viewBox=\"0 0 " width " " height "\">"
       "<rect width=\"" width "\" height=\"" height "\" fill=\"white\"/>"
       body "</svg>"))

(defn- lines
  "A run of monospace lines starting at (x, y), 16px apart; returns the svg and the next y."
  [x y ls & {:keys [size] :or {size 9.5}}]
  [(apply str (for [[i l] (map-indexed vector ls)] (htxt x (+ y (* 16 i)) l :mono true :size size)))
   (+ y (* 16 (count ls)))])

(defn- bars
  "Horizontal bars for [label count] rows, scaled to max-w; returns svg and next y."
  [x y rows max-w]
  (let [top (apply max 1 (map second rows))]
    [(apply str (for [[i [label n]] (map-indexed vector rows)]
                  (let [yy (+ y (* 18 i)) w (max 2 (* max-w (/ (double n) top)))]
                    (str (box (+ x 300) (- yy 10) w 13 "#cfd8e3" "#68717b")
                         (htxt x yy (str label) :mono true :size 9.5)
                         (htxt (+ x 306 w) yy (str n) :mono true :size 9.5)))))
     (+ y (* 18 (count rows)))]))

;; ---------------------------------------------------------------------------
;; store extraction
;; ---------------------------------------------------------------------------

(defn short-id [s] (if (string? s) (str (subs s 0 (min 20 (count s))) "…") (pr-str s)))
(defn short-sha [s] (if (string? s) (subs s 0 (min 8 (count s))) (pr-str s)))
(defn stamp [s] (if (string? s) (subs s 0 (min 19 (count s))) (pr-str s)))

(defn edn-files
  "Every .edn file directly under ROOT/CHILD, sorted by name: {:name :path :sha :value}."
  [child]
  (let [d (io/file root child)]
    (vec (for [^java.io.File f (sort-by #(.getName ^java.io.File %) (.listFiles d))
               :when (and (.isFile f) (str/ends-with? (.getName f) ".edn"))]
           {:name (.getName f) :path (.getPath f) :sha (file-sha256 (.getPath f))
            :value (read-record (.getPath f))}))))

(defn occurrence-evidence
  "The occurrence-evidence directory is one subdirectory per occurrence, each holding observation files."
  []
  (let [d (io/file root "occurrence-evidence")]
    (vec (for [^java.io.File sub (sort-by #(.getName ^java.io.File %) (.listFiles d))
               :when (.isDirectory sub)]
           {:name (.getName sub)
            :files (vec (for [^java.io.File f (sort-by #(.getName ^java.io.File %) (.listFiles sub))
                              :when (and (.isFile f) (str/ends-with? (.getName f) ".edn"))]
                          {:name (.getName f) :path (.getPath f) :sha (file-sha256 (.getPath f))
                           :value (read-record (.getPath f))}))}))))

(defn store-digest
  "sha8 over 'child/name sha256' lines for every .edn file in the nine subdirectories, sorted."
  [store]
  (string-sha8
   (str/join "\n"
             (sort (concat (for [c children :when (not= c "occurrence-evidence") f (get store c)]
                             (str c "/" (:name f) " " (:sha f)))
                           (for [sub (get store "occurrence-evidence") f (:files sub)]
                             (str "occurrence-evidence/" (:name sub) "/" (:name f) " " (:sha f))))))))

(defn read-store []
  (into {} (concat (for [c children :when (not= c "occurrence-evidence")] [c (edn-files c)])
                   [["occurrence-evidence" (occurrence-evidence)]])))

(defn by-id [files] (into {} (map (fn [f] [(get-in f [:value :repair/id]) f])) files))

(defn derived-status
  "What open-obligations (repair_obligation.clj:914-938) and obligation-history (:886-912)
   would say about one finding, from the other subdirectories: the finding file itself
   always says :open."
  [store id]
  (let [res (by-id (get store "resolutions")) dis (by-id (get store "dismissals"))
        impl (by-id (get store "implementations")) ver (by-id (get store "verifications"))]
    (cond (contains? res id) (str "resolved (" (name (get-in res [id :value :repair/status])) ")")
          (contains? dis id) (str "dismissed (" (name (get-in dis [id :value :repair/status])) ")")
          (contains? ver id) "open, :awaiting-validation via verifications/"
          (contains? impl id) "open, :awaiting-validation via implementations/"
          :else "open")))

(defn open-class
  "The class open-obligations reports: :system-actuation-failure is read as :machine-failure (:925-927)."
  [c] (if (= :system-actuation-failure c) :machine-failure c))

(defn family
  "The id's naming family, by prefix."
  [id]
  (cond (str/starts-with? id "repair-occ-") "repair-occ-<sha256>"
        (str/starts-with? id "repair-ea1-") "repair-ea1-<sha256>--attempt-N-<kind>"
        (str/starts-with? id "repair-attempt-") "repair-attempt-NNN[-<kind>]"
        (str/starts-with? id "repair-initialization") "repair-initialization-<uuid>-<kind>"
        (str/starts-with? id "repair-canary-") "repair-canary-<uuid>[-<kind>]"
        (str/starts-with? id "repair-run4-") "repair-run4-…"
        :else "other"))

(defn freq-str [m] (str/join ", " (for [[k v] m] (str (pr-str k) " " v))))

;; ---------------------------------------------------------------------------
;; Figure 1 — the store's anatomy
;; ---------------------------------------------------------------------------

(defn fig1-store [store open-today]
  (let [fs (get store "findings")
        cls (into (sorted-map) (frequencies (map (comp :repair/class :value) fs)))
        own-status (into (sorted-map) (frequencies (map (comp :repair/status :value) fs)))
        statuses (into (sorted-map) (frequencies (map #(derived-status store (get-in % [:value :repair/id])) fs)))
        cross (into (sorted-map) (frequencies (map (fn [f] [(get-in f [:value :repair/class]) (derived-status store (get-in f [:value :repair/id]))]) fs)))
        res (get store "resolutions") impl (get store "implementations") dis (get store "dismissals")
        ops (get store "discharge-operations") links (get store "ticket-links")
        vers (get store "verifications") vev (get store "verification-evidence") occ (get store "occurrence-evidence")
        ctx (fn [xs] (count (filter #(contains? (:value %) :repair/discharge-context) xs)))
        dirs [["findings/" (count fs) "record-system-failure! (repair_obligation.clj:546-593) / record-review-failure! (:501); write-new-or-identical! — the finding is immutable, its :repair/status is written :open and never rewritten"]
              ["implementations/" (count impl) "record-implementation! (:1661): a reviewed, grounded repair commit; :repair/status :awaiting-validation — 'This does not clear the line'"]
              ["resolutions/" (count res) "resolve! (:1764-1823), successor-resolution! (:1840), supersede! (:1737-1762): the record that makes a finding not-open"]
              ["dismissals/" (count dis) "eight dismiss-*! routes (:944 :980 :1021 :1087 :1164 :1251 :1372 :1513): operator-authored dispositions; also make a finding not-open"]
              ["verifications/" (count vers) "record-historical-verification! (:745): historical admissions; read by verified-admissions (:847) → :awaiting-validation"]
              ["verification-evidence/" (count vev) ":wm/historical-repair-verification-v1 evidence the verifications/ admissions pin"]
              ["discharge-operations/" (count ops) "record-discharge-operation! (:1631-1647): content-addressed :intent / :outcome records of the runner's finalize-run! (repair_discharge.clj:196-217)"]
              ["ticket-links/" (count links) "finding-ticket/publish! (finding_ticket.clj:93-131): {:finding/id :finding/ticket {:id :path :finding-sha256 …}} written when the T- ticket is published"]
              ["occurrence-evidence/" (str (count occ) " dirs, " (reduce + (map (comp count :files) occ)) " files") "occurrence-evidence! (:461): one :wm/repair-occurrence-observation-v1 per observation of a repair-occ finding"]]
        width 1260
        [d-svg y1] (lines 50 118 (for [[n c w] dirs] (format "%-24s %-22s %s" n (str c) w)))
        [f-svg y2] (lines 50 (+ y1 44)
                          (concat
                           [(str "every finding file's own :repair/status: " (freq-str own-status) "   (write-new-or-identical! :312 — the file never changes after it is written)")
                            (str ":repair/class on the " (count fs) " finding files: " (freq-str cls))
                            (str ":repair/schema-version: " (freq-str (into (sorted-map) (frequencies (map (comp :repair/schema-version :value) fs)))) "   :repair/occurrence present on " (count (filter (comp :repair/occurrence :value) fs)) " (the repair-occ-* family)")
                            (str "opened-at range: " (stamp (first (sort (map (comp :opened-at :value) fs)))) " … " (stamp (last (sort (map (comp :opened-at :value) fs)))))
                            ""
                            "what open-obligations (repair_obligation.clj:914-938) computes: every findings/ record whose :repair/id has NO resolutions/ record and NO dismissals/ record;"
                            "   :repair/status is rewritten to :awaiting-validation when verifications/ (:931-933) or implementations/ (:934-937) names the id; :system-actuation-failure is read as :machine-failure (:925-927);"
                            "   sorted by :opened-at. obligation-history (:886-912) is the audit view: the same joins, attached to the finding, plus dismissals' own :repair/status."
                            ""
                            (str "the " (count fs) " findings by class × what the other subdirectories say about them (computed here from the files, the rule above):")]
                           (for [[[c s] n] cross] (format "   %-30s %-52s %3d" (pr-str c) s n))
                           [""
                            (str "totals by derived status: " (freq-str statuses))]))
        [o-svg y3] (lines 50 (+ y2 44)
                          [(str "repair/open-obligations called in this process on the store as it is now: " (count open-today) " obligations;")
                           (str "   by :repair/class as reported: " (freq-str (into (sorted-map) (frequencies (map :repair/class open-today)))))
                           (str "   by :repair/status as reported: " (freq-str (into (sorted-map) (frequencies (map :repair/status open-today)))))
                           (str "   the id set equals findings − resolutions − dismissals computed here: " (= (set (map :repair/id open-today)) (set (for [f fs :let [id (get-in f [:value :repair/id])] :when (str/starts-with? (derived-status store id) "open")] id))))])
        [r-svg y4] (lines 50 (+ y3 44)
                          [(str "resolutions/ " (count res) ": :repair/status " (freq-str (frequencies (map (comp :repair/status :value) res))) "; with :repair/discharge-context " (ctx res) " of " (count res) "; with :successor-relation " (count (filter (comp :successor-relation :value) res)) "; :schema " (freq-str (frequencies (map (comp :schema :value) res))))
                           (str "implementations/ " (count impl) ": :repair/status " (freq-str (frequencies (map (comp :repair/status :value) impl))) "; with :repair/discharge-context " (ctx impl) " of " (count impl) "; with :grounded-review-evidence " (count (filter (comp :grounded-review-evidence :value) impl)))
                           (str "dismissals/ " (count dis) ": :repair/status " (freq-str (into (sorted-map) (frequencies (map (comp :repair/status :value) dis)))))
                           (str "   :dismissal/kind key present on " (count (filter (comp :dismissal/kind :value) dis)) " (the three routes added 09-24: :grounding-readback-degraded, :repaired-elsewhere, :condition-cleared); :authority key on " (count (filter (comp :authority :value) dis)) "; :actor " (freq-str (frequencies (map (comp :actor :value) dis))))
                           (str "discharge-operations/ " (count ops) ": :kind " (freq-str (frequencies (map (comp :kind :value) ops))) "; :result :reason " (freq-str (frequencies (map (comp :reason :result :value :value) ops))) "; :result :repair/id " (freq-str (frequencies (map (comp :repair/id :result :value :value) ops))))
                           (str "   :close :closed-at from " (stamp (first (sort (map (comp :closed-at :close :value :value) ops)))) " to " (stamp (last (sort (map (comp :closed-at :close :value :value) ops)))) "; :close :grounded? " (freq-str (frequencies (map (comp :grounded? :close :value :value) ops))) "; file name = sha256 of the record's pr-str (:1640-1642), equal to the raw file sha on all: " (every? #(= (:name %) (str (subs (:sha %) 0 64) ".edn")) ops))
                           (str "ticket-links/ " (count links) ": :schema " (freq-str (frequencies (map (comp :schema :value) links))) "; with :publication/git " (count (filter (comp :publication/git :value) links)) " of " (count links) " (the one without: " (str/join ", " (map (comp short-id :finding/id :value) (remove (comp :publication/git :value) links))) ")")
                           (str "verifications/ " (count vers) ": " (freq-str (frequencies (map (comp :repair/status :value) vers))) "; verification-evidence/ " (count vev) ": " (freq-str (frequencies (map (comp :schema :value) vev))))
                           (str "occurrence-evidence/ " (count occ) " directories, one per repair-occ finding (" (= (count occ) (count (filter (comp :repair/occurrence :value) fs))) "); observations per directory " (freq-str (into (sorted-map) (frequencies (map (comp count :files) occ)))))])
        height (+ y4 70)]
    (hsvg
     width height
     (str
      (htxt 40 36 "The repair store — data/wm-repair-obligations/ as it is on disk: nine subdirectories, what writes each, and what 'open' means in code" :size 15 :weight "bold")
      (htxt 40 58 "The finding file is the only record of the obligation; its status never changes. Openness is a JOIN: a finding is open when neither resolutions/ nor dismissals/ holds a file of its id (repair_obligation.clj:914-938)." :size 11)
      (htxt 40 76 "The runner reads the store through that one function at the :stop-line-memory phase (full_loop_runner.clj:4491-4493); 15 of these files are tracked in git, the rest are on disk only (git ls-files)." :size 11)
      (htxt 40 100 "(a) the nine subdirectories: file counts, and the function that writes each (line numbers at futon2 792ebc37)" :weight "bold" :size 11)
      d-svg
      (htxt 40 (+ y1 20) "(b) the findings, by class and by what the other subdirectories say about them" :weight "bold" :size 11)
      f-svg
      (htxt 40 (+ y2 20) "(c) NOT A RECORD VALUE — open-obligations run here, on the store as it is at generation time" :weight "bold" :size 11)
      o-svg
      (htxt 40 (+ y3 20) "(d) the other subdirectories: statuses, and which records carry a :repair/discharge-context (the key the publication receipt needs, section 4)" :weight "bold" :size 11)
      r-svg
      (htxt 40 (+ y4 14) "Evidence only: counts and frequencies are over the files read; the derived statuses apply the cited function's rule to the files, not a stored field." :size 9)))))

;; ---------------------------------------------------------------------------
;; Figure 2 — the stop-line definition, and the 34 ids on the 09-24 record
;; ---------------------------------------------------------------------------

(defn stop-line-rows
  "Join the ids of a tick's :open-stop-lines to their finding files."
  [store t]
  (let [fs (by-id (get store "findings"))]
    (vec (for [id (get-in t [:open-stop-lines :ids])]
           (let [f (get-in fs [id :value])]
             {:id id :class (:repair/class f) :open-class (open-class (:repair/class f))
              :opened-at (:opened-at f) :failure-kind (:failure-kind f)
              :status (derived-status store id) :found? (some? f)})))))

(defn fig2-stop-lines [store t24 t23 open-today]
  (let [rows (stop-line-rows store t24)
        rows23 (stop-line-rows store t23)
        ids24 (set (map :id rows)) ids23 (set (map :id rows23))
        left (vec (sort (remove ids24 ids23))) added (vec (sort (remove ids23 ids24)))
        by-class (into (sorted-map) (frequencies (map :open-class rows)))
        by-status (into (sorted-map) (frequencies (map :status rows)))
        click-rule (filter #(and (str/starts-with? (:status %) "open") (not= :environmental-hold (:open-class %))) rows)
        click-rule-strict (filter #(and (= "open" (:status %)) (not= :environmental-hold (:open-class %))) rows)
        by-day (into (sorted-map) (frequencies (map #(subs (:opened-at %) 0 10) rows)))
        by-month (into (sorted-map) (frequencies (map #(subs (:opened-at %) 0 7) rows)))
        sorted (sort-by :opened-at rows)
        width 1260
        [d-svg y1] (lines 50 118
                          ["full_loop_runner.clj:4491-4493  open-stop-lines = (repair/open-obligations) at the :stop-line-memory phase — every open obligation, of every class"
                           "                    :4641-4643  the selection cell carries {:count (count open-stop-lines) :ids (mapv :repair/id open-stop-lines)}; persist-run-record! :570-573 copies it to the tick record as :open-stop-lines"
                           "                    :4494-4499  validation-lines = the FIRST open obligation with :repair/status :awaiting-validation or :repair/class :environmental-hold — read at :5363-5368 only when it also carries :repair/verification"
                           "                    :4540-4543  stop-line = the open obligation whose :repair/id equals the selected action's :target, only when the action type is :repair-machine-failure / :revalidate-historical-repair"
                           "                    :4520-4521  the comment at selection: 'ordinary clicks always select; repair memory is evidence, never a divert' (8b6827da, 2026-09-19 19:52:46)"
                           "scripts/wm_click.sh:200-203     :stop-lines-queued = (count (filter (and (= :open :repair/status) (not= :environmental-hold :repair/class)) (open-obligations)))"
                           "                    :193-199    its comment says the runner takes 'stop-line = (first open, non-environmental-hold obligation)' and diverts to a repair entry; :249 says 'selection proceeds regardless'"
                           "NOTE-stop-lines-the-older-ten   'open AND class is not :environmental-hold': 52 open = 16 stop-lines (all :machine-failure) + 36 ordinary queue — the note's own count, on the store as it was when written"])
        [c-svg y2] (bars 50 (+ y1 48) (for [[k v] by-class] [(str (pr-str k) " (as open-obligations reports it)") v]) 300)
        [s-svg y3] (lines 50 (+ y2 30)
                          [(str "by derived status: " (freq-str by-status))
                           (str "under wm_click.sh's rule (open ∧ class ≠ :environmental-hold), counting :awaiting-validation as open: " (count click-rule) ";   counting only :repair/status :open literally: " (count click-rule-strict))
                           (str "   the 17 :machine-failure of RECORD.md's 'after 34 open / 17 :machine-failure' (runs/stop-line-discharge-2026-09-24, 04:04:03Z, before this tick at 04:53): " (get by-class :machine-failure))
                           (str "every id found in findings/: " (every? :found? rows) ";   ids also on the 09-23 record (51): " (count (filter ids23 ids24)) ";   ids on 09-23 and not here: " (count left) ";   ids here and not on 09-23: " (count added))])
        [g-svg y4] (lines 50 (+ y3 44)
                          (concat
                           [(str "by month opened: " (freq-str by-month))
                            (str "by day opened: " (freq-str by-day))
                            ""
                            (format "%-6s %-70s %-28s %-20s %-32s %s" "" "id" "class" "opened-at" "failure-kind" "derived status")]
                           (map-indexed (fn [i r] (format "%-6s %-70s %-28s %-20s %-32s %s" (inc i) (if (> (count (:id r)) 68) (str (subs (:id r) 0 65) "…") (:id r)) (pr-str (:open-class r)) (stamp (:opened-at r)) (pr-str (:failure-kind r)) (:status r))) sorted)))
        [l-svg y5] (lines 50 (+ y4 44)
                          (concat
                           [(str (count left) " ids on the 09-23 record's 51 and not on the 09-24 record's 34, with the record that removed each (file mtime is the store's only timestamp for resolutions; :resolved-at / :dismissed-at are in the records):")]
                           (for [id left]
                             (let [res (get (by-id (get store "resolutions")) id) dis (get (by-id (get store "dismissals")) id)]
                               (format "   %-70s %s" (if (> (count id) 68) (str (subs id 0 65) "…") id)
                                       (cond res (str "resolutions/ :resolved-at " (stamp (get-in res [:value :resolved-at])) " " (pr-str (get-in res [:value :repair/status])))
                                             dis (str "dismissals/ :dismissed-at " (stamp (get-in dis [:value :dismissed-at])) " " (pr-str (get-in dis [:value :repair/status])))
                                             :else "no resolution or dismissal record — still open now?"))))
                           [(str (count added) " ids on 09-24 and not on 09-23" (when (seq added) (str ": " (str/join ", " (map short-id added)))))
                            (str "the finding this tick opened (" (short-id id-ad16) ") is NOT among the 34: the phase that reads the store (:4491) runs before the close that writes it (:3798-3846)")
                            (str "NOT A RECORD VALUE — open-obligations now: " (count open-today) "; of them :environmental-hold " (count (filter #(= :environmental-hold (:repair/class %)) open-today)) ", :machine-failure " (count (filter #(= :machine-failure (:repair/class %)) open-today)) "; the 34 ids ⊆ now: " (every? (set (map :repair/id open-today)) ids24))]))
        height (+ y5 70)]
    (hsvg
     width height
     (str
      (htxt 40 36 "Stop-lines as computed — three definitions, and the 34 ids on tick-run-record-2026-09-24-1790225596 grouped by class and by age" :size 15 :weight "bold")
      (htxt 40 58 "The tick record's :open-stop-lines is the whole of open-obligations at the :stop-line-memory phase; the class filter the script and the note apply is not in the runner's count." :size 11)
      (htxt 40 76 "The 34 ids are joined here to their finding files for class, opened-at and failure-kind; the derived status applies open-obligations' rule to the other subdirectories." :size 11)
      (htxt 40 100 "(a) where 'stop-line' is defined, and how each definition reads" :weight "bold" :size 11)
      d-svg
      (htxt 40 (+ y1 24) "(b) the 34 by :repair/class (as open-obligations reports it; the two :system-actuation-failure findings in the store read as :machine-failure)" :weight "bold" :size 11)
      c-svg
      s-svg
      (htxt 40 (+ y3 20) "(c) the 34 by age, oldest first" :weight "bold" :size 11)
      g-svg
      (htxt 40 (+ y4 20) "(d) from the 09-23 record's 51 to the 09-24 record's 34" :weight "bold" :size 11)
      l-svg
      (htxt 40 (+ y5 14) "Evidence only: the ids and counts are the records' own; the class and age columns are read from the finding files the ids name; the two 'NOT A RECORD VALUE' lines are this process's read of the store today." :size 9)))))

;; ---------------------------------------------------------------------------
;; Figure 3 — how a click opens a finding: the 09-24 abstention
;; ---------------------------------------------------------------------------

(defn fig3-opening [t24 f-ad16 link-ad16-rec occ-ad16 ops-count]
  (let [events (get-in f-ad16 [:backtrace :phase-events])
        ev-row (fn [e] (format "%-22s %-6s %-30s %-10s %s" (name (:phase e)) (name (:transition e)) (:at e)
                               (if (:duration-ms e) (str (:duration-ms e) " ms") "") (if (:outcome e) (str ":outcome " (:outcome e)) "")))
        width 1260
        [e-svg y1] (lines 50 118 (map ev-row events))
        [p-svg y2] (lines 50 (+ y1 44)
                          ["full_loop_runner.clj:4647-4650  (when-not entry (throw (ex-info \"War Machine abstained or selected no addressable action\" {:outcome :abstained})))"
                           "                    :5445-5500  close! catches; close-core! (:3789-3797) writes :not-reached-* sorries for the unreached checkpoints, then builds the finding:"
                           "                    :3798-3808  repair-class = (repair-class-for (or (:failure-kind data) outcome)) — :abstained is in the :environmental-hold set (:3489-3496)"
                           "                    :3809-3846  (when-not (or (= :grounded-change outcome) admitted-verification?) (repair/record-system-failure! {… :repair-class … :discharge-contract (discharge-contract repair-class)}))"
                           "                    :3520-3538  discharge-contract :environmental-hold → {:requires [:cleared-precondition :grounded-production-shaped-successor]} + :artifact-shape :code-commit"
                           "                    :3834-3835  :opened-at is the time cell's :machine-state :started-at — the run's start, not the close"
                           "repair_obligation.clj:546-593   record-system-failure!: write-new-or-identical! findings/<id>.edn (:585); finding-ticket/publish! under the store lock (:586); occurrence-evidence! (:588)"
                           "finding_ticket.clj:93-131       publish!: the T- ticket file with '**Status:** OPEN' (:36), the queue entry, ticket-links/<id>.edn, then git commit of the ticket (:80-91)"
                           "full_loop_runner.clj:3850-3852  park-r16-stop-line! (:217-256): POST /api/alpha/park awaiting [repair-id] — the finding's id is what a later continuation waits on"
                           "                    :4299-4325  finalize-run! runs in the same close: :action selected-action (nil on this run) → bind-selected! (repair_discharge.clj:33-85) returns nil → :184 {:status :not-applicable}"
                           "repair_discharge.clj:210        (if (= :not-applicable (:status result)) result …) — no discharge-operations/ record is written for a :not-applicable close"])
        [f-svg y3] (lines 50 (+ y2 44)
                          [(str ":repair/id " (:repair/id f-ad16))
                           (str ":repair/class " (:repair/class f-ad16) "   :repair/status " (:repair/status f-ad16) "   :repair/schema-version " (:repair/schema-version f-ad16) "   :opened-at " (:opened-at f-ad16))
                           (str ":failure-kind " (:failure-kind f-ad16) "   :failure-stage " (:failure-stage f-ad16) "   :failure-outcome " (:failure-outcome f-ad16) "   :target " (pr-str (:target f-ad16)) "   :selected-entry " (pr-str (:selected-entry f-ad16)))
                           (str ":failure-error " (pr-str (:failure-error f-ad16)) "   :failure-data " (pr-str (:failure-data f-ad16)))
                           (str ":discharge-contract " (pr-str (:discharge-contract f-ad16)))
                           (str ":repair/occurrence " (pr-str (:repair/occurrence f-ad16)))
                           (str ":attempt-id " (:attempt-id f-ad16) "   :machine-repo " (:machine-repo f-ad16))
                           (str ":backtrace :last-completed-checkpoint " (pr-str (get-in f-ad16 [:backtrace :last-completed-checkpoint])) "   :checkpoints " (pr-str (get-in f-ad16 [:backtrace :checkpoints])))
                           (str "           :code-state :git-sha " (short-sha (get-in f-ad16 [:backtrace :code-state :git-sha])) " :git-dirty? " (get-in f-ad16 [:backtrace :code-state :git-dirty?]) "   (the checkout the click ran on; " (count (get-in f-ad16 [:backtrace :code-state :repo-heads])) " repo heads recorded)")
                           ""
                           (str "ticket-links/" (short-id id-ad16) ".edn: :finding/ticket :id T-" (short-id id-ad16) " :finding-sha256 " (short-sha (get-in link-ad16-rec [:finding/ticket :finding-sha256])) " — equals the finding file's sha now: " (= (get-in link-ad16-rec [:finding/ticket :finding-sha256]) (file-sha256 finding-ad16)))
                           (str "   :publication/git " (pr-str (select-keys (:publication/git link-ad16-rec) [:status :commit])) " (" (git-date (get-in link-ad16-rec [:publication/git :commit])) ")")
                           (str "occurrence-evidence/occ-" (subs id-ad16 11 31) "…/: " (count (:files occ-ad16)) " file; :observation/id " (pr-str (get-in occ-ad16 [:files 0 :value :observation/id])))
                           (str "   :observed-at " (get-in occ-ad16 [:files 0 :value :observed-at]) "   :source " (pr-str (get-in occ-ad16 [:files 0 :value :source])))
                           (str "ticket T-" (short-id id-ad16) ".md first lines: " (pr-str (vec (take 3 (remove str/blank? (str/split-lines (slurp ticket-ad16)))))))])
        [t-svg y4] (lines 50 (+ y3 44)
                          [(str ":repair/discharge " (pr-str (:repair/discharge t24)))
                           (str ":open-stop-lines :count " (get-in t24 [:open-stop-lines :count]) " — read at :stop-line-memory (" (:at (first (filter #(and (= :stop-line-memory (:phase %)) (= :end (:transition %))) events))) "), " (if (some #{id-ad16} (get-in t24 [:open-stop-lines :ids])) "includes" "does not include") " the finding this run opened")
                           (str ":route " (pr-str (:route t24)))
                           (str "discharge-operations/ holds " ops-count " records; none names this run (its finalize-run! returned :not-applicable at :210 without writing one)")
                           (str "timeline: :opened-at " (stamp (:opened-at f-ad16)) " (run start) → selection end " (stamp (:at (last events))) " → occurrence observed " (stamp (get-in occ-ad16 [:files 0 :value :observed-at])) " → ticket commit " (git-date (get-in link-ad16-rec [:publication/git :commit])) " → route :at " (stamp (get-in t24 [:route 0 :at_])))])
        height (+ y4 90)]
    (hsvg
     width height
     (str
      (htxt 40 36 "How a click opens a finding — tick-run-record-2026-09-24-1790225596 abstained, and close-core! wrote repair-occ-ad16e2c2… as an :environmental-hold" :size 15 :weight "bold")
      (htxt 40 58 "Four files came out of one abstention: the finding, its ticket-link, its occurrence observation, and a committed T- ticket. The tick record carries none of them; it carries :repair/discharge :not-applicable." :size 11)
      (htxt 40 76 "The finding's :opened-at is the run's start (the time cell), three minutes before the close that wrote it; the occurrence observation carries the close time." :size 11)
      (htxt 40 100 "(a) the finding's :backtrace :phase-events — the run as the finding retained it" :weight "bold" :size 11)
      e-svg
      (htxt 40 (+ y1 20) "(b) the code path from the abstention throw to the four files (line numbers at futon2 792ebc37)" :weight "bold" :size 11)
      p-svg
      (htxt 40 (+ y2 20) (str "(c) " finding-ad16 " and its companions") :weight "bold" :size 11)
      f-svg
      (htxt 40 (+ y3 20) (str "(d) what the tick record says about it: " tick-0924) :weight "bold" :size 11)
      t-svg
      (htxt 40 (+ y4 14) "What :not-applicable means here (repair_discharge.clj:184): bind-selected! found no repair identity on the action — a nil action on an abstained run. It is the binder's answer before any" :size 10)
      (htxt 40 (+ y4 32) "discharge was attempted, not a refusal; the same value is on the 09-22 record (whose selected action was a mission candidate, C3 on M-aif-policy-conditioned-eig, with no repair id)." :size 10)
      (htxt 40 (+ y4 50) "The finding this run opened has the contract [:cleared-precondition :grounded-production-shaped-successor]; neither requirement is in the binder's supported-requirements (:11-13), see figure 4(e)." :size 10)
      (htxt 40 (+ y4 74) "Evidence only: every value is read from the named file; the timeline joins timestamps the files carry." :size 9)))))

;; ---------------------------------------------------------------------------
;; Figure 4 — the publication refusals and the discharge-operations
;; ---------------------------------------------------------------------------

(defn fig4-publication [store t24 t23 t22 sel76 c76 op-rec]
  (let [pub24 (:repair/publication t24) pub23 (:repair/publication t23) pub22 (:repair/publication t22)
        ids24 (set (map :repair/id pub24)) ids23 (set (map :repair/id pub23))
        new-ids (vec (sort (remove ids23 ids24)))
        res (by-id (get store "resolutions"))
        ops (get store "discharge-operations")
        fams (into (sorted-map) (frequencies (map (comp family :repair/id) pub24)))
        action (get-in sel76 [:payload :judgment :selected-action])
        width 1260
        [c-svg y1] (lines 50 118
                          ["full_loop_runner.clj:5528-5530   publication = (discharge-receipt/catch-up! root repo) — BEFORE the attempt runs; :5646 puts it on the result as :repair/publication; persist-run-record! :567 copies it"
                           "repair_discharge_receipt.clj:137-145  catch-up!: (mapv #(publication-result! root repo %) (repair/discharge-resolution-ids root)) — every file in resolutions/, not the open queue (:1621-1629)"
                           "                             :125-135  publication-result!: publish! tried twice; a refusal is typed {:status :publication-refused :reason …} and :store/status is read back from the resolution"
                           "                             :16-56    derive: reads findings/, implementations/, resolutions/ for the id; :29 requires :repair/status :resolved; :30 requires (and context implementation-context)"
                           "                                       where context = (:repair/discharge-context resolution) and implementation-context = (:repair/discharge-context implementation) — else :resolution-context-unavailable"
                           "                             :12-13    the receipt would be committed at holes/labs/wm-contract/discharges/<id>.edn — that directory does not exist in this checkout"])
        [p-svg y2] (lines 50 (+ y1 44)
                          (concat
                           [(str "09-24 record: " (count pub24) " entries; :status " (freq-str (frequencies (map :status pub24))) "; :reason " (freq-str (frequencies (map :reason pub24))) "; :store/status " (freq-str (frequencies (map :store/status pub24))))
                            (str "   entry keys: " (pr-str (vec (sort (keys (first pub24))))))
                            (str "   :error " (pr-str (:error (first pub24))) "   :error-data-edn " (pr-str (:error-data-edn (first pub24))))
                            (str "   ids by family: " (freq-str fams))
                            (str "09-23 record: " (count pub23) " entries, " (freq-str (frequencies (map :reason pub23))) ";   09-22 record: " (count pub22) " entries, " (freq-str (frequencies (map :reason pub22))))
                            (str "resolutions/ now: " (count res) " = the 09-24 count: " (= (count res) (count pub24)) ";   the " (count new-ids) " ids on 09-24 and not on 09-23, with their :resolved-at:")]
                           (for [id new-ids] (format "   %-96s %s" (if (> (count id) 94) (str (subs id 0 91) "…") id) (stamp (get-in res [id :value :resolved-at]))))
                           [(str "   (written by holes/labs/wm-contract/runs/stop-line-discharge-2026-09-24/discharge.clj through resolve! and successor-resolution!, commit fa14d783 " (git-date "fa14d783") ")")
                            (str "resolutions with :repair/discharge-context: " (count (filter #(contains? (:value %) :repair/discharge-context) (vals res))) " of " (count res) ";   implementations with it: " (count (filter #(contains? (:value %) :repair/discharge-context) (get store "implementations"))) " of " (count (get store "implementations")))
                            "   resolve! attaches the context only when its resolution argument carries one (:1819-1821); record-implementation! likewise; the runner's finalize! is the one caller that supplies it (repair_discharge.clj:141-146, :153-155, :176-181)"]))
        [o-svg y3] (lines 50 (+ y2 44)
                          (concat
                           [(format "%-10s %-72s %-22s %-10s %-22s %s" "file" ":close :attempt/id" ":closed-at" ":grounded?" ":result :reason" ":result :stage / :repair/id")]
                           (for [o (sort-by (comp :closed-at :close :value :value) ops)]
                             (let [v (:value (:value o))]
                               (format "%-10s %-72s %-22s %-10s %-22s %s" (subs (:name o) 0 8) (let [a (get-in v [:close :attempt/id])] (str (subs a 0 20) "…" (subs a (- (count a) 12)))) (stamp (get-in v [:close :closed-at])) (pr-str (get-in v [:close :grounded?])) (pr-str (get-in v [:result :reason])) (str (pr-str (get-in v [:result :stage])) " / " (pr-str (get-in v [:result :repair/id]))))))
                           [""
                            (str "the 09-23 record's :repair/discharge: " (pr-str (dissoc (:repair/discharge t23) :event :error-data-edn)))
                            (str "   :event " (pr-str (select-keys (get-in t23 [:repair/discharge :event]) [:id :kind])) " → the file " (subs (:name op-rec) 0 8) "… above (:closed-at " (stamp (get-in op-rec [:value :value :close :closed-at])) " = 76/002's close :recorded-at " (stamp (:recorded-at c76)) ": " (= (get-in op-rec [:value :value :close :closed-at]) (:recorded-at c76)) ")")
                            (str "76/002's selected action (002-selection.edn): keys " (pr-str (vec (sort (keys action)))) "; :target " (pr-str (short-id (:target action))) "; :repair/id " (pr-str (:repair/id action)) "; :type " (pr-str (:type action)))
                            "repair_discharge.clj at 3bdd226a (2026-09-21 05:28:31), lines 15-30: id = (:repair/id action) → nil for this action; (when (or id (T-repair- target)) (evidence/safe-id! id) …) — safe-id! (repair_discharge_evidence.clj:50-53) requires (string? id) → :unsafe-repair-id"
                            "repair_discharge.clj at HEAD (e61a10cb, 2026-09-24 00:48:28): ticket-link (:15-30) reverse-looks-up ticket-links/ by [:finding/ticket :id]; bind-selected! :47-50 id = (or native-id (:finding/id link)); :57 refuses :finding-ticket-link-unavailable when no link"
                            "   tests: repair_discharge_test.clj ticket-queue-action-binds-through-the-ticket-link (:153), ticket-target-without-a-ticket-link-refuses-by-name (:181), non-ticket-target-without-an-id-is-still-not-applicable (:215)"
                            (str "   closes with a selected action since e61a10cb: none in the store (the newest operation is " (stamp (last (sort (map (comp :closed-at :close :value :value) ops)))) "; the 09-24 click abstained) — the new path has no operation record yet")]))
        [g-svg y4] (lines 50 (+ y3 44)
                          ["after binding, finalize! (repair_discharge.clj:107-194) requires, in order:"
                           "   :127-130  (and (#{:machine-failure :independent-review-failure} (:repair/class finding)) (= :code-commit artifact-shape) (seq requires) (every? supported-requirements requires)) → else :unsupported-discharge-contract"
                           "             supported-requirements (:11-13) = #{:distinct-repair-commit :independent-review :grounded-repair :distinct-production-shaped-successor}"
                           "             an :environmental-hold finding (class not in the set; requires [:cleared-precondition :grounded-production-shaped-successor], neither in the set) is refused here — 444fb018 and ad16e2c2 are both :environmental-hold"
                           "   :131-133  (and (true? (:grounded? close)) (:close-snapshot close) (string? (:attempt/id close)) (string? (:run/id close))) → else :durable-close-unavailable"
                           "             the runner passes :grounded? (= :grounded-change outcome) (:4315); 76/002 closed :grounded-no-change, so false"
                           "   :147-183  then: a resolutions/ record → publication; no implementations/ record → record-implementation! and {:status :awaiting-successor}; a distinct later close → successor-resolution! then publication"
                           "resolve! itself (repair_obligation.clj:1764-1823) does accept :environmental-hold (:1781 environmental? → (true? [:validation :production-shaped?])); the runner never reaches it for that class"])
        height (+ y4 70)]
    (hsvg
     width height
     (str
      (htxt 40 36 "Publication refusals and discharge operations — :resolution-context-unavailable on every resolution, :unsafe-repair-id on every operation" :size 15 :weight "bold")
      (htxt 40 58 "Two different refusals, two different places: publication runs at tick start over every resolutions/ file; discharge runs at close over the selected action. Neither has ever succeeded in this store." :size 11)
      (htxt 40 76 "The nine operation records are the nine closes of 09-23 that had a selected action; each refused at :stage :binding because the action carried a T- target and no :repair/id." :size 11)
      (htxt 40 100 "(a) where publication runs and what it requires (line numbers at futon2 792ebc37)" :weight "bold" :size 11)
      c-svg
      (htxt 40 (+ y1 20) "(b) :repair/publication on the three tick records, and why every entry refuses" :weight "bold" :size 11)
      p-svg
      (htxt 40 (+ y2 20) "(c) discharge-operations/ — the nine records, and the binder that wrote them" :weight "bold" :size 11)
      o-svg
      (htxt 40 (+ y3 20) "(d) what the binder at HEAD would require after binding" :weight "bold" :size 11)
      g-svg
      (htxt 40 (+ y4 14) "Evidence only: the refusal reasons, ids and timestamps are the records' own; the code path is quoted at the cited lines; 'never succeeded' is the count of :receipt-committed results (0) and of receipt files (none)." :size 9)))))

;; ---------------------------------------------------------------------------
;; Figure 5 — the ticket-versus-store join for T-repair-occ-444fb018…
;; ---------------------------------------------------------------------------

(defn fig5-ticket-store [store f-444 link-444-rec occ-444 t23 t24 queue]
  (let [ticket-lines (str/split-lines (slurp ticket-444))
        status-line (first (filter #(str/starts-with? % "**Status:**") ticket-lines))
        sha-line (first (filter #(str/starts-with? % "Finding SHA-256:") ticket-lines))
        ticket-log (git-log-file ticket-444)
        f-sha (file-sha256 finding-444)
        named-by (fn [child] (contains? (by-id (get store child)) id-444))
        ops-naming (count (filter #(= id-444 (get-in % [:value :value :result :repair/id])) (get store "discharge-operations")))
        entries (:entries queue)
        pos (inc (or (first (keep-indexed (fn [i e] (when (= (:ticket e) (str "T-" id-444)) i)) entries)) -1))
        read-of (fn [t] (let [o (first (filter #(= (str "T-" id-444) (:target %)) (get-in t [:declaration-reads :occurrences])))
                              r (get-in o [:observations :results :restoration-accepted])]
                          (str ":observed " (pr-str (:observed r)) " :check " (pr-str (:check r)) " at :sha " (pr-str (get-in r [:evidence :sha])) " → " (short-sha (get-in r [:evidence :resolved-sha])) " :decl " (pr-str (get-in r [:evidence :decl])))))
        source-text (slurp source-444)
        src-lines (str/split-lines source-text)
        decl-line (first (keep-indexed (fn [i l] (when (str/includes? l "**Status:** DONE") (inc i))) src-lines))
        recheck (read-record recheck-444)
        width 1260
        [t-svg y1] (lines 50 118
                          (concat
                           [(str "file: " ticket-444)
                            (str "line 1: " (pr-str (first ticket-lines)) "   " (pr-str status-line) "   " (pr-str sha-line))
                            (str "the ticket's 'Finding SHA-256' = ticket-links :finding-sha256 = findings/ file sha now: " (= (str "Finding SHA-256: `" f-sha "`") sha-line) " / " (= f-sha (get-in link-444-rec [:finding/ticket :finding-sha256])))
                            (str "raw sha now " (short-sha (file-sha256 ticket-444)) "…;   commits touching the file (git log, oldest first):")]
                           (for [[h d s] ticket-log] (format "   %s  %s  %s" h d (if (> (count s) 120) (str (subs s 0 117) "…") s)))
                           [(str "   the DONE commit 97e17e10 is 76/002's build artifact: 005-build.edn :commits " (pr-str (mapv short-sha (get-in (read-record (str/replace close-76-002 "007-closed" "005-build")) [:payload :judgment :commits]))) " — the 09-23 click's own author job wrote the Status line it was selected to achieve")
                            (str "   sections: " (str/join " | " (map #(subs % 3) (filter #(str/starts-with? % "## ") ticket-lines))))]))
        [s-svg y2] (lines 50 (+ y1 44)
                          [(str "findings/        present; :repair/status " (:repair/status f-444) "  :repair/class " (:repair/class f-444) "  :opened-at " (:opened-at f-444) "  :failure-kind " (:failure-kind f-444) "  :failure-stage " (:failure-stage f-444) "  :target " (pr-str (:target f-444)))
                           (str "                 :discharge-contract " (pr-str (:discharge-contract f-444)) "  :attempt-id " (:attempt-id f-444))
                           (str "                 :repair/occurrence :origin " (pr-str (get-in f-444 [:repair/occurrence :occurrence/origin])) "  :failure-error " (pr-str (:failure-error f-444)))
                           (str "ticket-links/    present; :schema " (:schema link-444-rec) "; keys " (pr-str (vec (sort (keys link-444-rec)))) " — no :publication/git key (the ticket was committed by hand: ae69f5e7 " (git-date "ae69f5e7") ", its message says 'published to disk by finding_ticket, never committed')")
                           (str "occurrence-evidence/ present; " (count (:files occ-444)) " observation: :observed-at " (get-in occ-444 [:files 0 :value :observed-at]) " :observation/id " (pr-str (get-in occ-444 [:files 0 :value :observation/id])))
                           (str "implementations/ " (if (named-by "implementations") "present" "none") ";  resolutions/ " (if (named-by "resolutions") "present" "none") ";  dismissals/ " (if (named-by "dismissals") "present" "none") ";  verifications/ " (if (named-by "verifications") "present" "none") ";  discharge-operations/ naming this id in :result: " ops-naming)
                           (str "→ open-obligations' rule: open, :repair/status " (derived-status store id-444) "; on the 09-23 record's 51: " (boolean (some #{id-444} (get-in t23 [:open-stop-lines :ids]))) "; on the 09-24 record's 34: " (boolean (some #{id-444} (get-in t24 [:open-stop-lines :ids]))))
                           (str "data/wm-ticket-queue/queue.edn: " (count entries) " entries, :placement " (pr-str (:placement queue)) ", :order " (pr-str (:order queue)) "; this ticket is entry " pos " (:inserted-at " (stamp (:inserted-at (first (filter #(= (:ticket %) (str "T-" id-444)) entries)))) " = the finding's :opened-at)")])
        [r-svg y3] (lines 50 (+ y2 44)
                          [(str source-444 " line " decl-line ": the want token :restoration-accepted's locator is {:class :C4 … :path holes/tickets/T-repair-occ-444fb018….md :decl \"**Status:** DONE\"} — the one reader of the ticket's Status line")
                           (str "   09-23 tick's :declaration-reads for this target: " (read-of t23))
                           (str "   09-24 tick's :declaration-reads for this target: " (read-of t24))
                           "   CLICK2-D: with the sole want already true, every candidate declines :no-new-wanted-token and the target refuses :no-constructed-candidate (packet §'Why T-repair-occ-444fb018 had C1/C2 yesterday and nothing today')"
                           "src/ readers of a '**Status:**' line: finding_ticket.clj:36 writes '**Status:** OPEN' into a new ticket; live_c.clj:13 reads mission files. No function in src/ reads a T- ticket's Status into the store."
                           ""
                           "code that joins the ticket to the store, in either direction:"
                           "   repair_discharge.clj:15-30 ticket-link: T- id → ticket-links/ record → :finding/id, at the close of a click whose selected action targets the ticket — the join runs only inside finalize-run!, and then :127-130 refuses this class (figure 4(d))"
                           "   repair_obligation.clj:980-1019 dismiss-condition-cleared!: an operator route for a 'moot environmental hold' taking {:checked-at :source :observation}; it reads no ticket"
                           (str "   the ticket's own text names a dated recheck: " recheck-444 " (75d83105 " (git-date "75d83105") "), keys " (pr-str (vec (sort (keys recheck)))) " — no dismissals/ record cites it")
                           "   the 8 dismiss-*! routes and resolve! all take the finding id as an argument; none scans holes/tickets/. wm_click.sh's :stop-lines-queued (:200-203) reads open-obligations, not tickets."])
        height (+ y3 90)]
    (hsvg
     width height
     (str
      (htxt 40 36 "The ticket-versus-store join — T-repair-occ-444fb018… reads DONE on the ticket, and its obligation is open in the store" :size 15 :weight "bold")
      (htxt 40 58 "Two records of one obligation, joined once at publication (the ticket-links receipt pins the finding's bytes) and never afterwards: the ticket's Status line changed on 09-23, the store has no record of it." :size 11)
      (htxt 40 76 "The Status line does reach the machine — through the cascade source's want locator, where it makes the target unconstructable (CLICK2-D) — and does not reach the store, where the finding stays open." :size 11)
      (htxt 40 100 "(a) the ticket" :weight "bold" :size 11)
      t-svg
      (htxt 40 (+ y1 20) "(b) the store, subdirectory by subdirectory, for this id" :weight "bold" :size 11)
      s-svg
      (htxt 40 (+ y2 20) "(c) who reads the ticket's Status line, and what joins the two records" :weight "bold" :size 11)
      r-svg
      (htxt 40 (+ y3 14) "What the records say: ticket DONE since 97e17e10 (21:40:45Z 09-23); finding :open, :environmental-hold, since 05:13:31Z 09-22; no implementation, resolution, dismissal, verification or discharge operation names it." :size 10)
      (htxt 40 (+ y3 32) "What the code says: the only automatic join (ticket-link) runs inside a discharge that refuses this finding's class before it could resolve it; the dismissal route for a cleared hold takes a recheck it is handed, and none has been handed." :size 10)
      (htxt 40 (+ y3 50) "Nothing here is decided: whether DONE on the ticket should be a resolution in the store is not a question the records or the code answer." :size 10)
      (htxt 40 (+ y3 74) "Evidence only: presence and absence are file existence and `contains?` on the read records; the git dates are git's own." :size 9)))))

;; ---------------------------------------------------------------------------
;; Figure 6 — a narrative case: repair-occ-444fb018… from opening to today
;; ---------------------------------------------------------------------------

(defn fig6-narrative [store f-444 occ-444 t22 c70 sel76 c76 op-rec t24]
  (let [events (get-in f-444 [:backtrace :phase-events])
        first-ev (first events)
        j70 (get-in c70 [:payload :judgment])
        j76 (get-in c76 [:payload :judgment])
        sel-j (get-in sel76 [:payload :judgment])
        ticket-log (git-log-file ticket-444)
        rows [[(stamp (:at first-ev)) "09-22 click 1790053967 starts (:phase :opportunity :transition :start)" (str "findings/" (short-id id-444) ".edn :backtrace :phase-events[0]")]
              [(stamp (:opened-at f-444)) "the time cell's :started-at — what the finding will carry as :opened-at (:3834-3835)" "the same finding, :opened-at"]
              [(stamp (:recorded-at c70)) (str "machinery-70 attempt-002 closes: :outcome " (:outcome j70) " :failure-kind " (:failure-kind j70) " :grounded? " (:grounded? j70) ", occurrence :action/value :id " (pr-str (get-in j70 [:occurrence :action/value :id])) " on " (pr-str (get-in j70 [:occurrence :action/value :target]))) (str close-70-002 " (sha " (sha8 close-70-002) "…)")]
              [(stamp (get-in occ-444 [:files 0 :value :observed-at])) "close-core! writes the finding (:environmental-hold from :guardrail-refusal, :3489-3496), the ticket-link, the occurrence observation, and the ticket file with Status OPEN" (str "findings/, ticket-links/, occurrence-evidence/occ-444fb018…/" (subs (get-in occ-444 [:files 0 :name]) 0 8) "….edn, holes/tickets/T-…md")]
              [(stamp (get-in t22 [:route 0 :at_])) (str "tick record written: :route :via " (pr-str (get-in t22 [:route 0 :via])) "; :repair/discharge " (pr-str (:repair/discharge t22)) " (the selected action was a mission candidate, no repair id); :open-stop-lines :count " (get-in t22 [:open-stop-lines :count])) (str tick-0922 " (sha " (sha8 tick-0922) "…)")]
              [(git-date "ae69f5e7") "ae69f5e7 commits the ticket by hand ('published to disk by finding_ticket, never committed; needed at HEAD for its cascade source's task locator') — the ticket-links record has no :publication/git" ticket-444]
              [(git-date "ee22106c") "ee22106c 'Declare prospective EIG held-out split' (5 files) — the ticket's 'Restoration progress' section" "the ticket; git log"]
              [(git-date "0798f96a") "0798f96a 'Make held-out split C4-observable' (4 files)" "the ticket; git log"]
              [(git-date "75d83105") "75d83105 'Record cleared calibration precondition recheck' — the dated recheck the ticket's DONE section cites" recheck-444]
              [(stamp (get-in sel76 [:recorded-at])) (str "09-23 click 1790199409 selects the ticket: :selected-mission " (short-id (:selected-mission sel-j)) ", action :id " (pr-str (:id (:selected-action sel-j))) " :precedence " (pr-str (mapv :id (:precedence (:selected-action sel-j)))) "; :open-stop-lines :count " (get-in sel-j [:open-stop-lines :count]) " including this id") (str sel-76-002 " (sha " (sha8 sel-76-002) "…)")]
              [(git-date "97e17e10") "97e17e10 'Accept restored guardrail precondition' — the author job's commit; the ticket's Status line becomes DONE" "the ticket; 005-build.edn :commits"]
              [(stamp (:recorded-at c76)) (str "76/002 closes: :outcome " (:outcome j76) " :grounded? " (:grounded? j76) "; :accepted-increment :accepted? " (pr-str (get-in j76 [:accepted-increment :accepted?])) " (walkthrough 05 §3)") (str close-76-002 " (sha " (sha8 close-76-002) "…)")]
              [(stamp (get-in op-rec [:value :value :close :closed-at])) "finalize-run! on that close: bind-selected! refuses :unsafe-repair-id at :stage :binding, :repair/id nil — the action carried :target T-… and no :repair/id; recorded as an :outcome operation" (str "discharge-operations/" (subs (:name op-rec) 0 8) "….edn; " tick-0923 " :repair/discharge")]
              [(git-date "e61a10cb") "e61a10cb 'Bind a ticket-queue action to its finding through the ticket-links record' — the binder now reads ticket-links/ for a T- target" "src/futon2/aif/repair_discharge.clj:15-30, :47-50"]
              [(git-date "b6ed08e5") "b6ed08e5, 89e30016: two new dismissal routes; 03:26-03:37 nine dismissals written (six :grounding-readback-degraded, three :repaired-elsewhere) — none for this id" "dismissals/"]
              [(git-date "fa14d783") "fa14d783: nine resolutions written at 04:04:03 by the stop-line-discharge script — none for this id" "resolutions/; runs/stop-line-discharge-2026-09-24/RECORD.md"]
              [(stamp (:startedAt t24)) (str "09-24 click 1790225596: :declaration-reads sees :restoration-accepted true at HEAD; every candidate declines; abstains. :open-stop-lines :count " (get-in t24 [:open-stop-lines :count]) " including this id; :repair/discharge " (pr-str (get-in t24 [:repair/discharge :status]))) (str tick-0924 " (sha " (sha8 tick-0924) "…)")]
              ["now" (str "store: " (derived-status store id-444) ", :environmental-hold; ticket: " (pr-str (first (filter #(str/starts-with? % "**Status:**") (str/split-lines (slurp ticket-444))))) "; no record joins them; the binder at HEAD refuses the class at :127-130 before any resolution") "figure 5"]]
        width 1260
        [r-svg y1] (lines 50 118 (for [[at what] rows] (format "%-20s %s" at what)) :size 9.5)
        [w-svg y2] (lines 50 (+ y1 44) (for [[at _ where] rows] (format "%-20s %s" at where)) :size 9)
        height (+ y2 100)]
    (hsvg
     width height
     (str
      (htxt 40 36 "A narrative case: repair-occ-444fb018… from the guardrail refusal that opened it (09-22) to today — every record on the path" :size 15 :weight "bold")
      (htxt 40 58 "Opened by one click, selected and worked by another, marked DONE on its ticket by that click's own commit, refused discharge by a nil id, then made unconstructable by its own DONE line; open in the store throughout." :size 11)
      (htxt 40 76 (str "The ticket file's git history has " (count ticket-log) " commits; the finding file has one write; the store has no second record of this id in any subdirectory that would change its status.") :size 11)
      (htxt 40 100 "(a) the timeline, each row a value read from the file named in (b)" :weight "bold" :size 11)
      r-svg
      (htxt 40 (+ y1 20) "(b) the record each row is read from" :weight "bold" :size 11)
      w-svg
      (htxt 40 (+ y2 14) "What the records say: the obligation's ticket reached DONE inside a click that closed :grounded-no-change and whose discharge refused before reading the finding; the next click read DONE through the want locator and abstained." :size 10)
      (htxt 40 (+ y2 32) "What they do not say: whether the ticket's DONE is the :cleared-precondition the finding's contract requires — no record joins the two, and the one route that could (dismiss-condition-cleared!) has not been called for this id." :size 10)
      (htxt 40 (+ y2 50) "What is not established: whether a later click can select this ticket at all (CLICK2-D: not under the declaration as it stands, while the want is true), and so whether the binder's new ticket-link path will ever run on it." :size 10)
      (htxt 40 (+ y2 74) "Evidence only: timestamps are the records' own; git dates are git's; the ordering is by those timestamps." :size 9)))))

;; ---------------------------------------------------------------------------
;; generate!
;; ---------------------------------------------------------------------------

(defn generate! []
  (let [store (read-store)
        digest (store-digest store)
        open-today (repair/open-obligations root)
        t22 (read-record tick-0922) t23 (read-record tick-0923) t24 (read-record tick-0924)
        f-444 (read-record finding-444) f-ad16 (read-record finding-ad16)
        link-444-rec (read-record link-444) link-ad16-rec (read-record link-ad16)
        occ-444 (first (filter #(str/includes? (:name %) "444fb018") (get store "occurrence-evidence")))
        occ-ad16 (first (filter #(str/includes? (:name %) "ad16e2c2") (get store "occurrence-evidence")))
        c70 (read-record close-70-002) sel76 (read-record sel-76-002) c76 (read-record close-76-002)
        op-rec (first (filter #(str/starts-with? (:name %) "c884c9c2") (get store "discharge-operations")))
        queue (read-record queue-path)
        h-t22 (sha8 tick-0922) h-t23 (sha8 tick-0923) h-t24 (sha8 tick-0924)
        h-f444 (sha8 finding-444) h-fad16 (sha8 finding-ad16) h-link (sha8 link-444) h-ticket (sha8 ticket-444)
        h-op (subs (:sha op-rec) 0 8)]
    (spit-svg (str "fig1-store-anatomy-" digest ".svg") (fig1-store store open-today))
    (spit-svg (str "fig2-stop-lines-" h-t24 "-" h-t23 "-" digest ".svg") (fig2-stop-lines store t24 t23 open-today))
    (spit-svg (str "fig3-opening-" h-t24 "-" h-fad16 ".svg") (fig3-opening t24 f-ad16 link-ad16-rec occ-ad16 (count (get store "discharge-operations"))))
    (spit-svg (str "fig4-publication-" h-t24 "-" h-t23 "-" h-op ".svg") (fig4-publication store t24 t23 t22 sel76 c76 op-rec))
    (spit-svg (str "fig5-ticket-store-" h-ticket "-" h-f444 "-" h-link ".svg") (fig5-ticket-store store f-444 link-444-rec occ-444 t23 t24 queue))
    (spit-svg (str "fig6-narrative-444fb018-" h-f444 "-" h-t22 "-" h-t23 "-" h-t24 ".svg") (fig6-narrative store f-444 occ-444 t22 c70 sel76 c76 op-rec t24))
    (println "HEAD" (git-head))
    (println "store digest" digest)
    (doseq [c children] (println c (if (= c "occurrence-evidence") (str (count (get store c)) " dirs") (count (get store c)))))
    (println "findings by class" (pr-str (into (sorted-map) (frequencies (map (comp :repair/class :value) (get store "findings"))))))
    (println "findings by derived status" (pr-str (into (sorted-map) (frequencies (map #(derived-status store (get-in % [:value :repair/id])) (get store "findings"))))))
    (println "open-obligations now" (count open-today) (pr-str (into (sorted-map) (frequencies (map :repair/class open-today)))) (pr-str (into (sorted-map) (frequencies (map :repair/status open-today)))))
    (println "tick-0922" (file-sha256 tick-0922) "open" (get-in t22 [:open-stop-lines :count]) "publication" (count (:repair/publication t22)))
    (println "tick-0923" (file-sha256 tick-0923) "open" (get-in t23 [:open-stop-lines :count]) "publication" (count (:repair/publication t23)))
    (println "tick-0924" (file-sha256 tick-0924) "open" (get-in t24 [:open-stop-lines :count]) "publication" (count (:repair/publication t24)))
    (println "34 by class" (pr-str (into (sorted-map) (frequencies (map :open-class (stop-line-rows store t24))))))
    (println "34 by status" (pr-str (into (sorted-map) (frequencies (map :status (stop-line-rows store t24))))))
    (println "finding-444" (file-sha256 finding-444) "finding-ad16" (file-sha256 finding-ad16))
    (println "ticket-444" (file-sha256 ticket-444) "link-444" (file-sha256 link-444) "op-c884" (:sha op-rec))
    (println "close-70-002" (file-sha256 close-70-002) "sel-76-002" (file-sha256 sel-76-002) "close-76-002" (file-sha256 close-76-002))))
