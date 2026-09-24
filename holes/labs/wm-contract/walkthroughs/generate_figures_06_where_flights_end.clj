;; generate_figures_06_where_flights_end.clj — regenerate every figure in
;; 06-where-flights-end.md from the records. Run from the futon2 checkout root:
;;
;;   clojure -M -i holes/labs/wm-contract/walkthroughs/generate_figures_06_where_flights_end.clj \
;;           -e "(generate-figures-06-where-flights-end/generate!)"
;;
;; Read-only: reads two tick records, two attempt directories' selection and
;; close files, the fourteen closes of machinery-70..76, one repair finding,
;; the finding files of the 34 ids the 09-24 record names as open stop-lines
;; and the existence of their resolution / dismissal / implementation /
;; verification files, the nine discharge-operation records, and the context
;; key of every resolution and implementation. Writes only the three SVGs
;; named in generate! beside this script (fig1-stop-map-…, fig2-open-stop-lines-…,
;; fig3-refusals-…). Every figure file name carries the short raw sha256 of the
;; record(s) it was drawn from; fig2 also carries a digest over the 34 finding
;; files' sha256 in record order. No namespace of the runner or the store is
;; required; nothing is written back to any record. Where a count is derived by
;; this script from file presence rather than read off a record, the figure
;; says so.
(ns generate-figures-06-where-flights-end
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str])
  (:import [java.nio.file Files]
           [java.security MessageDigest]))

(def here "holes/labs/wm-contract/walkthroughs")
(def root "data/wm-repair-obligations")

(def tick-0923 "data/wm-runs/tick-run-record-2026-09-23-1790199409.edn")
(def tick-0924 "data/wm-runs/tick-run-record-2026-09-24-1790225596.edn")

(defn attempt-dir [n a]
  (str "data/wm-full-loop-machinery-" n "/wm-contract-machinery-" n "-v1/attempt-" a "/"))

(def attempts (vec (for [n (range 70 77) a ["001" "002"]] [n a])))
(def sel-76 (str (attempt-dir 76 "002") "002-selection.edn"))
(def close-76 (str (attempt-dir 76 "002") "007-closed.edn"))
(def sel-77 (str (attempt-dir 77 "001") "002-selection.edn"))
(def close-77 (str (attempt-dir 77 "001") "007-closed.edn"))

(def id-ad16 "repair-occ-ad16e2c2e10f83412628d1eec8e222018afa399fba64016124e32f3a812a3214")
(def id-444 "repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade")
(def finding-ad16 (str root "/findings/" id-ad16 ".edn"))
(def op-c884 (str root "/discharge-operations/c884c9c2c775f9d3cd8ee70011a454473660b5a3eafea6dac6c23a17f0c9d39c.edn"))

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

(defn git-head []
  (str/trim (:out (sh/sh "git" "rev-parse" "HEAD"))))

(defn edn-files [dir]
  (->> (.listFiles (io/file dir))
       (filter #(and (.isFile ^java.io.File %) (.endsWith (.getName ^java.io.File %) ".edn")))
       (sort-by #(.getName ^java.io.File %))
       vec))

(defn has-file? [child id] (.isFile (io/file root child (str id ".edn"))))

;; ---------------------------------------------------------------------------
;; svg helpers (same style as walkthroughs 01-05)
;; ---------------------------------------------------------------------------

(defn- esc [s]
  (-> (str s) (str/replace "&" "&amp;") (str/replace "<" "&lt;") (str/replace ">" "&gt;")))

(defn- htxt [x y s & {:keys [size anchor weight mono fill] :or {size 12 anchor "start"}}]
  (str "<text x=\"" x "\" y=\"" y "\" font-size=\"" size "\" text-anchor=\"" anchor "\""
       (when weight (str " font-weight=\"" weight "\""))
       (when fill (str " fill=\"" fill "\""))
       " font-family=\"" (if mono "monospace" "system-ui,sans-serif") "\">" (esc s) "</text>"))

(defn- box [x y w h fill stroke]
  (str "<rect x=\"" x "\" y=\"" y "\" width=\"" w "\" height=\"" h "\" rx=\"5\""
       " fill=\"" fill "\" stroke=\"" stroke "\"/>"))

(defn- bar [x y w h fill]
  (str "<rect x=\"" x "\" y=\"" y "\" width=\"" (double w) "\" height=\"" (double h) "\" fill=\"" fill "\"/>"))

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
  "A run of monospace lines starting at (x, y), 15px apart; returns the svg."
  [x y ls & {:keys [size] :or {size 9.5}}]
  (apply str (for [[i l] (map-indexed vector ls)] (htxt x (+ y (* 15 i)) l :mono true :size size))))

(defn short-sha [s] (if (string? s) (subs s 0 (min 8 (count s))) (pr-str s)))
(defn short-id [s] (if (string? s) (str (subs s 0 (min 26 (count s))) "…") (pr-str s)))
(defn stamp [s] (if (string? s) (subs s 0 (min 19 (count s))) (pr-str s)))

;; ---------------------------------------------------------------------------
;; extraction
;; ---------------------------------------------------------------------------

(defn refusal-kinds [sel]
  (frequencies (map :kind (get-in sel [:payload :sorry :decision :refusals]))))

(defn no-candidate-targets [sel]
  (->> (get-in sel [:payload :sorry :decision :refusals])
       (filter #(= :no-constructed-candidate (:kind %)))
       (mapv (juxt :target :missing))))

(defn judgment [close] (get-in close [:payload :judgment]))

(defn close-row [[n a]]
  (let [path (str (attempt-dir n a) "007-closed.edn")
        j (judgment (read-record path))
        ai (:accepted-increment j)
        rec (:run-ending-classification j)]
    {:label (str n "/" a) :sha (sha8 path)
     :outcome (:outcome j) :grounded (:grounded? j)
     :accepted (if (contains? j :accepted-increment) (:accepted? ai) :no-key)
     :resolved (get-in j [:witness :resolved?] :no-key)
     :class (if rec (:class rec) :nil)}))

(defn age-bucket [opened-at]
  (let [d (subs (str opened-at) 0 10)]
    (cond (str/starts-with? d "2026-07") "2026-07"
          (<= (compare d "2026-09-15") 0) "2026-09-11..15"
          :else "2026-09-20..23")))

(defn stop-line-row [id]
  (let [path (str root "/findings/" id ".edn")
        f (read-record path)
        impl? (has-file? "implementations" id)
        ver? (has-file? "verifications" id)]
    {:id id :sha (file-sha256 path)
     :class (:repair/class f) :kind (:failure-kind f) :stage (:failure-stage f)
     :opened (:opened-at f) :bucket (age-bucket (:opened-at f))
     :status-on-file (:repair/status f)
     :resolved? (has-file? "resolutions" id) :dismissed? (has-file? "dismissals" id)
     :impl? impl? :ver? ver?
     ;; open-obligations (repair_obligation.clj:914-938) rewrites :repair/status
     ;; to :awaiting-validation when a verification or implementation names the
     ;; id; this script reproduces that by file presence and labels it derived.
     :derived (if (or impl? ver?) :awaiting-validation :open)}))

(defn discharge-op-row [^java.io.File f]
  (let [v (read-record (.getPath f))
        r (get-in v [:value :result])]
    {:file (.getName f) :kind (:kind v) :status (:status r) :reason (:reason r)
     :stage (:stage r) :id (:repair/id r)
     :closed-at (get-in v [:value :close :closed-at])}))

(defn context-census [child]
  (let [fs (edn-files (str root "/" child))
        recs (map #(read-record (.getPath ^java.io.File %)) fs)]
    {:count (count fs)
     :with-context (count (filter #(contains? % :repair/discharge-context) recs))
     :statuses (frequencies (map :repair/status recs))}))

;; ---------------------------------------------------------------------------
;; Figure 1: the stop map — six sites along one flight, each with its record
;; ---------------------------------------------------------------------------

(defn fig1-stop-map [t24 t23 sel77 c76 f-ad16 h-t24 h-t23 h-c76 h-f]
  (let [phases [["tick start" "catch-up!" ":5528-5530"]
                [":stop-line-memory" "open-obligations" ":4491-4493"]
                [":selection" "scan / admit / construct" ":4512-4531"]
                ["enact" "build + review" "005 / 006"]
                ["close" "close-core!" ":3789-3846"]
                ["finalize-run!" "discharge binder" "repair_discharge.clj"]
                ["finding" "record-system-failure!" ":3809-3846"]]
        n (count phases) x0 30 w 148 gap 20 y 70
        pub24 (:repair/publication t24)
        rk (refusal-kinds sel77)
        j76 (judgment c76)
        se23 (:selection-event t23)
        mhc (:mission-hole-coverage t23)
        dis23 (:repair/discharge t23)
        site (fn [i label]
               (let [x (+ x0 (* i (+ w gap)) (/ w 2))]
                 (str "<circle cx=\"" (double x) "\" cy=\"" (+ y 66) "\" r=\"11\" fill=\"#b23a48\"/>"
                      (htxt x (+ y 70) label :anchor "middle" :size 11 :weight "bold" :fill "white"))))
        col (fn [i ls] (lines (+ x0 (* i (+ w gap)) -4) (+ y 98) ls :size 8))]
    (hsvg 1210 480
          (str (htxt 20 26 "One flight, seven phases, and where the records show it ending" :size 14 :weight "bold")
               (htxt 20 44 (str "tick 2026-09-24-1790225596 " h-t24 " · tick 2026-09-23-1790199409 " h-t23 " · close 76/002 " h-c76 " · finding ad16e2c2 " h-f) :size 10 :mono true)
               (apply str (for [[i [a b c]] (map-indexed vector phases)]
                            (let [x (+ x0 (* i (+ w gap)))]
                              (str (box x y w 44 "#eef2f6" "#68717b")
                                   (htxt (+ x (/ w 2)) (+ y 17) a :anchor "middle" :size 10.5 :weight "bold")
                                   (htxt (+ x (/ w 2)) (+ y 30) b :anchor "middle" :size 8.5)
                                   (htxt (+ x (/ w 2)) (+ y 41) c :anchor "middle" :size 8 :mono true)
                                   (when (< i (dec n)) (arrow (+ x w) (+ y 22) (+ x w gap -2) (+ y 22)))))))
               ;; sites, in phase order
               (site 0 "5") (site 1 "6") (site 2 "1") (site 3 "2") (site 4 "3") (site 5 "5") (site 6 "4")
               (col 0 [(str (count pub24) " publications on 09-24:")
                       (str "  :publication-refused " (count (filter #(= :publication-refused (:status %)) pub24)))
                       "  :resolution-context-"
                       (str "   unavailable " (count (filter #(= :resolution-context-unavailable (:reason %)) pub24)))
                       "store cannot join resolution"
                       "  to implementation"
                       "hole: none named (§2)"])
               (col 1 [(str ":open-stop-lines {:count " (get-in t24 [:open-stop-lines :count]) "}")
                       "read as evidence; selection"
                       "  proceeds (:4520-4521)"
                       (str "09-23 record: count " (get-in t23 [:open-stop-lines :count]))
                       ""
                       "hole: none named per id;"
                       "  standing state (fig 2)"])
               (col 2 [(str "77/001 refusals " (reduce + (vals rk)) ":")
                       (str "  :universe-not-admitted " (get rk :universe-not-admitted 0))
                       (str "  :no-constructed-candidate " (get rk :no-constructed-candidate 0))
                       "  each [:new-wanted-token-"
                       "   within-horizon]"
                       "throw :4647 -> :abstained"
                       "hole: H-interp, H-exits"])
               (col 3 [(str "09-23 " (pr-str (:event se23)))
                       (str "  " (subs (:target se23) 0 22) "…")
                       "  wants = the ticket's tokens"
                       (str "  missions deferred-to-declaration " (count (:targets-deferred-to-declaration mhc)))
                       (str "  holes not projected " (:holes-not-projected mhc) "/" (:holes-retained mhc))
                       ""
                       "hole: H-exits, H-grain"])
               (col 4 [(str "76/002 :outcome " (pr-str (:outcome j76)))
                       (str "  :grounded? " (:grounded? j76))
                       (str "  :accepted? " (get-in j76 [:accepted-increment :accepted?]))
                       (str "  witness :resolved? " (get-in j76 [:witness :resolved?]))
                       (str "  run-ending class " (pr-str (get-in j76 [:run-ending-classification :class])))
                       ""
                       "hole: theorem statement"])
               (col 5 [(str "09-23 " (pr-str (:status dis23)))
                       (str "  :reason " (pr-str (:reason dis23)))
                       (str "  :repair/id " (pr-str (:repair/id dis23)) ", op " (short-sha (get-in dis23 [:event :id])))
                       (str "09-24 " (pr-str (get-in t24 [:repair/discharge :status])))
                       "  (no selected action)"
                       ""
                       "hole: none named (§2)"])
               (col 6 [(str "finding " (short-sha id-ad16) "… opened")
                       (str "  " (pr-str (:repair/class f-ad16)))
                       (str "  :failure-kind " (pr-str (:failure-kind f-ad16)))
                       (str "  contract requires " (count (get-in f-ad16 [:discharge-contract :requires])))
                       "   (:3532-3533)"
                       "no typed decline on tick (AR-16)"
                       "hole: H-interp, H-exits; carrier"])
               (htxt 20 306 "Reading: the numbered circles are the six stop sites of the table in 06-where-flights-end.md §2, placed at the phase" :size 10)
               (htxt 20 322 "where the record shows the flight ending. Site 5 appears twice: publication runs at tick start over every resolution;" :size 10)
               (htxt 20 338 "the discharge binder runs at finalize-run! on the selected action. Line numbers are full_loop_runner.clj at the HEAD in §Verification." :size 10)
               (htxt 20 368 "What continuing would have looked like, per site (the hole's 'what the machine must do', PROOF-2a Holes table):" :size 10 :weight "bold")
               (lines 20 386 ["1  an interpreted pattern producing the target's false want, and the wants read from the mission's criteria"
                              "2  the flight's target is the mission; its wants are the mission's exits; a grain check before enactment"
                              "3  a close that reads the mission's criteria met, with a grounding witness that resolved"
                              "4  the decline carried on the tick record, so the hole is visible from the record it opened"
                              "5  a selected action that names its finding, and a resolution the store can join to its implementation"
                              "6  the 34 obligations discharged or dismissed by a path the store records"] :size 9.5)))))

;; ---------------------------------------------------------------------------
;; Figure 2: the 34 open stop-lines by class, age and stage
;; ---------------------------------------------------------------------------

(def class-order [:machine-failure :environmental-hold :independent-review-failure])
(def class-fill {:machine-failure "#b23a48" :environmental-hold "#3b6ea5" :independent-review-failure "#c9a227"})
(def buckets ["2026-07" "2026-09-11..15" "2026-09-20..23"])

(defn fig2-open-stop-lines [rows t24 h-t24 digest]
  (let [by-class (frequencies (map :class rows))
        by-bucket-class (frequencies (map (juxt :bucket :class) rows))
        by-stage (sort-by (juxt (comp - val) (comp name key)) (frequencies (map #(or (:stage %) :nil) rows)))
        by-derived (frequencies (map :derived rows))
        non-hold (count (remove #(= :environmental-hold (:class %)) rows))
        non-hold-open (count (filter #(and (= :open (:derived %)) (not= :environmental-hold (:class %))) rows))
        x0 60 y0 90 bw 30 unit 9 base (+ y0 180)
        table (fn [x y pairs]
                (apply str (for [[i [k v]] (map-indexed vector pairs)]
                             (str (htxt x (+ y (* 15 i)) k :mono true :size 9.5)
                                  (htxt (+ x 250) (+ y (* 15 i)) (str v) :mono true :size 9.5 :anchor "end")))))]
    (hsvg 920 580
          (str (htxt 20 26 (str "The " (count rows) " ids in :open-stop-lines on 2026-09-24-1790225596, joined to their finding files") :size 14 :weight "bold")
               (htxt 20 44 (str "tick " h-t24 " · digest over the 34 finding files' sha256 " digest " · record :count " (get-in t24 [:open-stop-lines :count])) :size 10 :mono true)
               (htxt x0 (- y0 10) "by age bucket (opened-at) and :repair/class" :size 10.5 :weight "bold")
               (apply str (for [[bi b] (map-indexed vector buckets)
                                [ci c] (map-indexed vector class-order)
                                :let [n (get by-bucket-class [b c] 0)
                                      x (+ x0 (* bi 120) (* ci (+ bw 4)))
                                      h (* unit n)]]
                            (str (bar x (- base h) bw h (class-fill c))
                                 (htxt (+ x (/ bw 2)) (- base h 3) (str n) :anchor "middle" :size 9))))
               "<line x1=\"" x0 "\" y1=\"" base "\" x2=\"" (+ x0 360) "\" y2=\"" base "\" stroke=\"#68717b\"/>"
               (apply str (for [[bi b] (map-indexed vector buckets)]
                            (htxt (+ x0 (* bi 120) 50) (+ base 14) b :anchor "middle" :size 9.5)))
               (apply str (for [[ci c] (map-indexed vector class-order)]
                            (str (bar (+ x0 (* ci 125)) (+ base 26) 10 10 (class-fill c))
                                 (htxt (+ x0 (* ci 125) 14) (+ base 35) (str (name c) " " (get by-class c 0)) :size 9))))
               ;; stage table
               (htxt 520 (- y0 10) "by :failure-stage (record values)" :size 10.5 :weight "bold")
               (table 520 (+ y0 6) (for [[s n] by-stage] [(name s) n]))
               ;; status, derived
               (htxt 520 (+ y0 220) "status, computed by this script from file presence" :size 10.5 :weight "bold")
               (htxt 520 (+ y0 234) "(NOT RECORD VALUES)" :size 9 :weight "bold")
               (table 520 (+ y0 252)
                      [[":repair/status on the finding file" ":open ×34"]
                       ["implementation or verification file" (get by-derived :awaiting-validation 0)]
                       ["neither" (get by-derived :open 0)]
                       ["resolution or dismissal file" (count (filter #(or (:resolved? %) (:dismissed? %)) rows))]
                       ["class != :environmental-hold" non-hold]
                       ["  of which neither impl. nor verif." non-hold-open]])
               (lines 520 (+ y0 348) ["open-obligations (:914-938) rewrites the first"
                                      "line to :awaiting-validation for the second;"
                                      "wm_click.sh:200-202 counts the last."] :size 8.6)
               (htxt 20 (+ base 232) "Reading: every one of the 34 is a prior stop of the same kinds as the table's rows 1-4 — at agent readiness, initialization," :size 10)
               (htxt 20 (+ base 248) "selection, construction or close. None was discharged by a path the store records; the six the stop-lines note describes as" :size 10)
               (htxt 20 (+ base 264) "repaired in code (its addendum) have no resolution file, which is site 5 seen from the other side." :size 10)))))

;; ---------------------------------------------------------------------------
;; Figure 3: the two refusal ladders — discharge and publication
;; ---------------------------------------------------------------------------

(defn fig3-refusals [ops res-c impl-c t24 t23 close-rows h-t24 h-t23 h-op]
  (let [op-reasons (frequencies (map (juxt :stage :reason) ops))
        pub24 (frequencies (map :reason (:repair/publication t24)))
        pub23 (frequencies (map :reason (:repair/publication t23)))
        grounded (frequencies (map :grounded close-rows))
        eligible-classes #{:machine-failure :independent-review-failure}
        step (fn [x y w title body ok?]
               (str (box x y w 70 (if ok? "#eef7ee" "#fbeeee") (if ok? "#3a7d44" "#b23a48"))
                    (htxt (+ x 8) (+ y 16) title :size 10 :weight "bold")
                    (lines (+ x 8) (+ y 32) body :size 8.6)))]
    (hsvg 920 380
          (str (htxt 20 26 "Two refusal ladders: the discharge binder at close, and publication at tick start" :size 14 :weight "bold")
               (htxt 20 44 (str "tick 09-24 " h-t24 " · tick 09-23 " h-t23 " · discharge-operation c884c9c2 " h-op " · " (count ops) " operations read") :size 10 :mono true)
               (htxt 20 74 "finalize! (repair_discharge.clj:107-184) — stages in order; the first refusal is the recorded one" :size 10.5 :weight "bold")
               (step 20 84 205 ":binding  safe-id! (evidence:50-53)"
                     [(str (count ops) " operations, all")
                      (str "  " (pr-str (first (keys op-reasons))))
                      ":repair/id nil on a T- target"] false)
               (arrow 225 115 240 115)
               (step 242 84 205 ":eligibility  class in"
                     [(str "  " (pr-str eligible-classes))
                      "ad16e2c2 and 444fb018 are"
                      ":environmental-hold -> refused"] false)
               (arrow 447 115 462 115)
               (step 464 84 205 ":durable-close  (true? :grounded?)"
                     [(str "14 closes :grounded? " (pr-str grounded))
                      "76/002 (the selected-ticket close)"
                      "is :grounded? false"] false)
               (arrow 669 115 684 115)
               (step 686 84 205 ":implementation / :successor"
                     ["never reached on any recorded"
                      "operation; resolutions/ gained"
                      "no row from a click"] false)
               (htxt 20 184 "derive (repair_discharge_receipt.clj:15-56) — run by catch-up! over every resolutions/ file at tick start" :size 10.5 :weight "bold")
               (step 20 194 205 ":resolution-not-successful"
                     [(str res-c " resolutions, statuses")
                      (str "  " (pr-str (:statuses (context-census "resolutions"))))
                      "passes"] true)
               (arrow 225 225 240 225)
               (step 242 194 205 ":resolution-context-unavailable"
                     [(str "context on resolutions   " (:with-context (context-census "resolutions")) "/" res-c)
                      (str "context on implementations " (:with-context (context-census "implementations")) "/" impl-c)
                      "refused for every id"] false)
               (arrow 447 225 462 225)
               (step 464 194 205 "on the tick records"
                     [(str "09-24 " (pr-str pub24))
                      (str "09-23 " (pr-str pub23))
                      "the receipt directory does not exist"] false)
               (htxt 20 290 "Reading: the only writer of :repair/discharge-context is finalize! itself (:141-178), which has never passed :binding on a" :size 10)
               (htxt 20 306 "recorded operation. So publication cannot succeed until a discharge does, and a discharge cannot bind a ticket-queue action" :size 10)
               (htxt 20 322 "that carries no :repair/id (e61a10cb adds the ticket-links lookup; it is later than every recorded operation). No Holes row" :size 10)
               (htxt 20 338 "names this ladder; §2 of the walkthrough says so rather than assign one." :size 10)))))

;; ---------------------------------------------------------------------------

(defn generate! []
  (let [t24 (read-record tick-0924)
        t23 (read-record tick-0923)
        sel77 (read-record sel-77)
        c76 (read-record close-76)
        f-ad16 (read-record finding-ad16)
        ids (get-in t24 [:open-stop-lines :ids])
        rows (mapv stop-line-row ids)
        digest (string-sha8 (str/join (map :sha rows)))
        ops (mapv discharge-op-row (edn-files (str root "/discharge-operations")))
        close-rows (mapv close-row attempts)
        res-c (:count (context-census "resolutions"))
        impl-c (:count (context-census "implementations"))
        h-t24 (sha8 tick-0924) h-t23 (sha8 tick-0923) h-c76 (sha8 close-76)
        h-f (sha8 finding-ad16) h-op (sha8 op-c884) h-s77 (sha8 sel-77)]
    (spit-svg (str "fig1-stop-map-" h-t24 "-" h-t23 "-" h-s77 "-" h-c76 "-" h-f ".svg")
              (fig1-stop-map t24 t23 sel77 c76 f-ad16 h-t24 h-t23 h-c76 h-f))
    (spit-svg (str "fig2-open-stop-lines-" h-t24 "-" digest ".svg")
              (fig2-open-stop-lines rows t24 h-t24 digest))
    (spit-svg (str "fig3-refusals-" h-t24 "-" h-t23 "-" h-op ".svg")
              (fig3-refusals ops res-c impl-c t24 t23 close-rows h-t24 h-t23 h-op))
    (println "HEAD" (git-head))
    (println "tick-0924" (file-sha256 tick-0924))
    (println "tick-0923" (file-sha256 tick-0923))
    (println "sel-77" (file-sha256 sel-77) "close-77" (file-sha256 close-77))
    (println "sel-76" (file-sha256 sel-76) "close-76" (file-sha256 close-76))
    (println "finding-ad16" (file-sha256 finding-ad16))
    (println "op-c884" (file-sha256 op-c884))
    (println "stop-line digest" digest "count" (count rows))
    (println "by class" (pr-str (frequencies (map :class rows))))
    (println "by bucket" (pr-str (frequencies (map :bucket rows))))
    (println "by bucket x class" (pr-str (into (sorted-map) (frequencies (map (juxt :bucket :class) rows)))))
    (println "by stage" (pr-str (into (sorted-map) (frequencies (map #(or (:stage %) :nil) rows)))))
    (println "derived status" (pr-str (frequencies (map :derived rows))) "resolved/dismissed files" (count (filter #(or (:resolved? %) (:dismissed? %)) rows)))
    (println "status on file" (pr-str (frequencies (map :status-on-file rows))))
    (println "77/001 refusals" (pr-str (refusal-kinds sel77)))
    (println "77/001 no-candidate targets" (pr-str (no-candidate-targets sel77)))
    (println "76/002 selected-action target" (get-in sel77 [:payload :sorry :kind]) "|" (get-in (read-record sel-76) [:payload :judgment :selected-action :target]))
    (println "76/002 precedence produces" (pr-str (mapv (juxt :id :produces) (get-in (read-record sel-76) [:payload :judgment :selected-action :precedence]))))
    (println "76/002 close" (pr-str (select-keys (close-row [76 "002"]) [:outcome :grounded :accepted :resolved :class])))
    (println "14 closes grounded" (pr-str (frequencies (map :grounded close-rows))) "accepted" (pr-str (frequencies (map :accepted close-rows))) "classes" (pr-str (frequencies (map :class close-rows))))
    (println "09-23 selection-event" (pr-str (:selection-event t23)))
    (println "09-23 mission-hole-coverage" (pr-str (select-keys (:mission-hole-coverage t23) [:holes-retained :holes-projected :holes-not-projected :targets-deferred-to-declaration :not-generated-reason :reason-not-projected])))
    (println "09-24 mission-hole-coverage" (pr-str (:mission-hole-coverage t24)))
    (println "09-23 discharge" (pr-str (dissoc (:repair/discharge t23) :error-data-edn :error)))
    (println "09-24 discharge" (pr-str (:repair/discharge t24)))
    (println "publication 09-24" (count (:repair/publication t24)) (pr-str (frequencies (map :reason (:repair/publication t24)))))
    (println "publication 09-23" (count (:repair/publication t23)) (pr-str (frequencies (map :reason (:repair/publication t23)))))
    (println "discharge ops" (pr-str (frequencies (map (juxt :kind :stage :reason) ops))))
    (println "discharge ops closed-at" (pr-str (mapv (comp stamp :closed-at) ops)))
    (println "context census" (pr-str (context-census "resolutions")) (pr-str (context-census "implementations")))
    (println "ad16" (pr-str (select-keys f-ad16 [:repair/class :repair/status :failure-kind :failure-stage :target :discharge-contract :opened-at])))))
