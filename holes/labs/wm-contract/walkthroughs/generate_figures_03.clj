;; generate_figures_03.clj — regenerate every figure in 03-observation-and-A.md
;; from the records. Run from the futon2 checkout root:
;;
;;   clojure -M -i holes/labs/wm-contract/walkthroughs/generate_figures_03.clj \
;;           -e "(generate-figures-03/generate!)"
;;
;; Read-only: reads attempt records, the tick record and the enactment file
;; the close names; writes only SVGs beside this script. Every figure file
;; name carries the short sha256 of the record(s) it was drawn from. Where a
;; figure shows what the code computes on a record — the rollout replay, a C4
;; re-read at a pinned sha, the pair builder on the recorded rows, the
;; token-likelihood under the all-zero rates — it calls that code
;; (futon2.aif.cascade-model-manifest, futon2.aif.token-outcome,
;; futon2.aif.observation-checks, futon2.aif.token-outcome-pair) in this
;; fresh process on the rows the record already holds. Nothing is written
;; back to any record.
(ns generate-figures-03
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [futon2.aif.cascade-model-manifest :as m]
            [futon2.aif.observation-checks :as checks]
            [futon2.aif.token-outcome :as token-outcome]
            [futon2.aif.token-outcome-pair :as pair])
  (:import [java.nio.file Files]
           [java.security MessageDigest]))

(def here "holes/labs/wm-contract/walkthroughs")
(def sel-76 "data/wm-full-loop-machinery-76/wm-contract-machinery-76-v1/attempt-002/002-selection.edn")
(def close-76 "data/wm-full-loop-machinery-76/wm-contract-machinery-76-v1/attempt-002/007-closed.edn")
(def close-75 "data/wm-full-loop-machinery-75/wm-contract-machinery-75-v1/attempt-002/007-closed.edn")
(def close-70 "data/wm-full-loop-machinery-70/wm-contract-machinery-70-v1/attempt-001/007-closed.edn")
(def tick "data/wm-runs/tick-run-record-2026-09-23-1790199409.edn")

(defn census-paths
  "The 14 closes OBS-D Revision 2 §R2.2 censuses: machinery-70..76, attempts 001 and 002."
  []
  (for [n (range 70 77) a ["001" "002"]]
    (str "data/wm-full-loop-machinery-" n "/wm-contract-machinery-" n "-v1/attempt-" a "/007-closed.edn")))

(defn read-record [path]
  (edn/read-string {:default (fn [t v] (tagged-literal t v))} (slurp path)))

(defn- hex [bytes n]
  (apply str (map #(format "%02x" (bit-and 255 %)) (take n bytes))))

(defn sha8 [path]
  (hex (.digest (MessageDigest/getInstance "SHA-256") (.getBytes (slurp path) "UTF-8")) 4))

(defn file-sha256
  "Raw-byte SHA-256 of a file, as full_loop_runner's sha256-bytes hashes the
   enactment file before comparison; nil when the file is absent."
  [path]
  (let [f (io/file path)]
    (when (.exists f)
      (hex (.digest (MessageDigest/getInstance "SHA-256") (Files/readAllBytes (.toPath f))) 32))))

(defn spit-svg [name svg]
  (let [file (str here "/" name)]
    (spit file svg)
    (println "wrote" file)))

;; ---------------------------------------------------------------------------
;; svg helpers (same style as walkthroughs 01 and 02)
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

;; ---------------------------------------------------------------------------
;; Record extraction
;; ---------------------------------------------------------------------------

(def target "T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade")

(defn short-target [s] (str/replace (str s) target "T-repair-occ-444fb018…"))

(defn token-name
  "A qualified token [target kw] printed as its keyword when the target is
   the walkthrough's ticket, else in full."
  [[t kw]]
  (if (= t target) (str kw) (str "[" (short-target t) " " kw "]")))

(defn short-sha [s] (if (string? s) (subs s 0 (min 8 (count s))) (pr-str s)))

(defn git-commit-line
  "Committer date (ISO 8601) and subject of SHA in this checkout: a read-only
   `git show -s`. The figure labels these as coming from git, not from a record."
  [sha]
  (let [{:keys [exit out]} (sh/sh "git" "show" "-s" "--format=%cI%n%s" sha)]
    (when (zero? exit) (str/split-lines out))))

(defn judgment [r] (get-in r [:payload :judgment]))
(defn prediction [sel] (get-in (judgment sel) [:token-outcome-prediction]))
(defn comparison [close] (:token-outcome-comparison (judgment close)))
(defn kernel [close] (:kernel-example (judgment close)))
(defn projection [close] (:observation-projection (kernel close)))

(defn sorted-tokens [m] (sort-by (comp pr-str key) m))

(defn contains-token? [state tok] (contains? state tok))

;; ---------------------------------------------------------------------------
;; Figure 1 — the prediction: where 175/256 comes from
;; ---------------------------------------------------------------------------

(defn fig1-prediction [sel]
  (let [pred (prediction sel)
        wanted (first (:wanted pred))
        tok (:token wanted)
        q0 (:initial-belief pred)
        q0-set (key (first q0))
        evals (get-in pred [:rollout :evaluations])
        ;; per tau: mass without / with the wanted token, from the recorded evaluations
        masses (for [ev evals]
                 (let [out (:outgoing-belief ev)]
                   {:tau (:tau ev)
                    :without (reduce + 0 (for [[s v] out :when (not (contains-token? s tok))] v))
                    :with (reduce + 0 (for [[s v] out :when (contains-token? s tok)] v))
                    :states (:states ev)}))
        replay (m/rollout-evaluation (constantly (get-in pred [:action :precedence])) q0 (:horizon pred))
        replay-eq (= (:belief replay) (get-in pred [:rollout :belief]))
        evals-eq (= (:evaluations replay) evals)
        freeze-eq (= (token-outcome/freeze-prediction (get-in (judgment sel) [:controller-decision])) pred)
        patterns (get-in pred [:action :precedence])
        col-x (fn [tau] (+ 340 (* 180 tau)))
        y-without 190 y-with 300
        width 1240 height 620]
    (hsvg
     width height
     (str
      (htxt 40 36 "Where 175/256 comes from — the frozen rollout of 76/002" :size 15 :weight "bold")
      (htxt 40 58 (str "002-selection.edn [:payload :judgment :token-outcome-prediction]: :status " (:status pred)
                       ", :prediction-rule " (:prediction-rule pred) ", :horizon " (:horizon pred)
                       ", model :kind " (get-in pred [:model :kind]) ", :model-id " (short-sha (:model-id pred)) "…") :size 11)
      (htxt 40 76 (str ":initial-belief is a point mass (mass " (val (first q0)) ") on ONE set of " (count q0-set)
                       " facts: " (str/join ", " (map token-name (sort-by pr-str q0-set)))) :size 11)
      (htxt 40 94 (str "The state space is subsets of the token universe (belief = {set-of-tokens -> exact rational}); "
                       "the recorded rollout's support is 2 of the 2^" (count (get-in pred [:observation-locators])) " subsets.") :size 11)
      ;; ladder
      (htxt 40 (+ y-without 5) (str "mass WITHOUT " (token-name tok)) :weight "bold" :size 11)
      (htxt 40 (+ y-with 5) (str "mass WITH " (token-name tok)) :weight "bold" :size 11)
      (htxt (col-x 0) 150 "tau 0 (q0)" :anchor "middle" :size 11 :weight "bold")
      (box (- (col-x 0) 45) (- y-without 18) 90 30 "#f6f7f8" "#68717b")
      (htxt (col-x 0) (+ y-without 2) (str (val (first q0))) :anchor "middle" :mono true)
      (box (- (col-x 0) 45) (- y-with 18) 90 30 "#f6f7f8" "#68717b")
      (htxt (col-x 0) (+ y-with 2) "0" :anchor "middle" :mono true)
      (apply str
             (for [{:keys [tau without with states]} masses
                   :let [x (col-x tau)
                         fired (first (filter #(= :pattern-kernel (:kernel-kind %)) states))
                         kern (:kernel fired)
                         stay (reduce + 0 (for [[s v] kern :when (not (contains-token? s tok))] v))
                         go (reduce + 0 (for [[s v] kern :when (contains-token? s tok)] v))
                         last? (= tau (count masses))]]
               (str (htxt x 150 (str "tau " tau) :anchor "middle" :size 11 :weight "bold")
                    (box (- x 45) (- y-without 18) 90 30 "#f6f7f8" "#68717b")
                    (htxt x (+ y-without 2) (str without) :anchor "middle" :mono true)
                    (box (- x 45) (- y-with 18) 90 30 (if last? "#dceee3" "#f6f7f8") (if last? "#287447" "#68717b"))
                    (htxt x (+ y-with 2) (str with) :anchor "middle" :mono true :weight (when last? "bold"))
                    ;; arrows from previous column
                    (arrow (+ (col-x (dec tau)) 45) y-without (- x 45) y-without :label (str "stay " stay))
                    (arrow (+ (col-x (dec tau)) 40) (+ y-without 12) (- x 45) (- y-with 12) :label (str go))
                    (arrow (+ (col-x (dec tau)) 45) y-with (- x 45) y-with :label "identity 1" :dash "4 3")
                    "")))
      (let [guard-lines (distinct (for [{:keys [states]} masses
                                        :let [fired (first (filter #(= :pattern-kernel (:kernel-kind %)) states))]]
                                    (str/join "; " (for [g (:guard-search fired)] (str (name (:pattern-id g)) " " (:guard-verdict g))))))]
        (apply str (for [[i l] (map-indexed vector guard-lines)]
                     (htxt 40 (+ 352 (* 16 i)) (str "guard search on the WITHOUT state, identical at every tau: " l) :size 10 :mono true))))
      (htxt 40 390 (str "Each step: on the state still lacking " (token-name tok) ", the guard search (first-enabled, cascade_model_manifest.clj:122-126) finds "
                        (name (:pattern-id (first (filter #(= :pattern-kernel (:kernel-kind %)) (:states (first evals))))))
                        " enabled") :size 10)
      (htxt 40 404 (str "   — its guard needs :repair/obstruction-observed-cleared present and the token absent — and applies it with theta "
                        (:theta (second patterns)) ": mass theta moves to the set with the token, 1 - theta stays.") :size 10)
      (htxt 40 422 (str (name (:id (first patterns))) " (theta " (:theta (first patterns)) ") is never enabled: its guard needs :repair/obstruction-observed-cleared ABSENT, and q0 already contains it. "
                        "On the state that has the token no guard holds, so the kernel is the identity.") :size 10)
      (htxt 40 440 (str "theta " (:theta (second patterns)) " is recorded with :theta-source " (:theta-source (second patterns))
                        " and :theta-provenance " (pr-str (select-keys (:theta-provenance (second patterns)) [:trials-count :successes]))
                        "; theta " (:theta (first patterns)) " with " (pr-str (select-keys (:theta-provenance (first patterns)) [:trials-count :successes])) ".") :size 10)
      (htxt 40 460 (str ":wanted " (token-name tok) " :predicted " (:predicted wanted)
                        " = sum of the tau-4 mass over states containing the token = 1 - (3/4)^4 = " (- 1 (* 3/4 3/4 3/4 3/4)) ".") :weight "bold" :size 12)
      (htxt 40 482 (str "The rule named on the record, :positive-marginal-support (token_outcome.clj:42-45), is exactly this marginal; "
                        "the record's :rollout :belief holds the two terminal sets at " (str/join " and " (map str (vals (get-in pred [:rollout :belief])))) ".") :size 10)
      (htxt 40 512 (str "Replay in this process — rollout-evaluation on the recorded precedence, q0 and horizon (cascade_model_manifest.clj:400): belief equal to the record: "
                        replay-eq "; evaluations equal: " evals-eq ".") :size 10)
      (htxt 40 530 (str "freeze-prediction (token_outcome.clj:9) on the record's own [:payload :judgment :controller-decision] returns a map equal to the recorded prediction: " freeze-eq ".") :size 10)
      (htxt 40 560 (str "The model's :rates entry on this record: " (pr-str (get-in pred [:model :rates]))
                        " — the prediction was made with no token observation rates at all (section 5).") :size 10)
      (htxt 40 590 "Every number above is read from the record; the replay lines are the only computation." :size 9)))))

;; ---------------------------------------------------------------------------
;; Figure 2 — the six tokens of 76/002 with their locators and results
;; ---------------------------------------------------------------------------

(defn fig2-locators [close tick-record sel]
  (let [proj (projection close)
        rows (sorted-tokens (:observations proj))
        after (get-in proj [:revision-pair :after])
        before (get-in proj [:revision-pair :before])
        cand-locators (get-in tick-record [:decision :selection-certificate :candidates 0 :id :observation-locators])
        bare (get-in tick-record [:decision :selection-certificate :candidates 0 :observation-locators])
        pred-locators (:observation-locators (prediction sel))
        row-locators (into {} (map (fn [[t r]] [t (get-in r [:meaning :locator])])) rows)
        width 1240 height (+ 440 (* 74 (count rows)))]
    (hsvg
     width height
     (str
      (htxt 40 36 "The six tokens of 76/002 — locator, resolution, result" :size 15 :weight "bold")
      (htxt 40 58 "Rows from 007-closed.edn [:payload :judgment :kernel-example :observation-projection :observations <token>]: :meaning :locator and :artifact-observation :measurement :result." :size 11)
      (htxt 40 76 (str "A C4 locator is the tuple (repo, sha, path, decl). Every locator here declares :sha \"HEAD\"; the measurement resolved it to " (short-sha after)
                       " (git rev-parse, observation_checks.clj:34-39) and read `git show <resolved-sha>:<path>` (:62-70).") :size 11)
      (apply str
             (for [[i [tok row]] (map-indexed vector rows)
                   :let [y (+ 100 (* 74 i))
                         loc (get-in row [:meaning :locator])
                         ao (:artifact-observation row)
                         res (get-in ao [:measurement :result])
                         ev (:evidence res)
                         re-after (checks/check-decl-in-file (assoc loc :sha after))
                         re-before (checks/check-decl-in-file (assoc loc :sha before))]]
               (str (box 40 y (- width 80) 64 (if (true? (:observed ao)) "#dceee3" "#fdf6ec") "#68717b")
                    (htxt 50 (+ y 18) (str (token-name tok) "   observed " (:observed ao) "   check " (:check res)
                                           "   file-present " (:file-present ev)) :weight "bold")
                    (htxt 50 (+ y 36) (str "repo " (:repo loc) "  sha " (:sha loc) " -> resolved " (short-sha (:resolved-sha ev))
                                           "  path " (short-target (:path loc)) "  decl " (pr-str (:decl loc))) :mono true :size 10)
                    (htxt 50 (+ y 54) (str "C4 re-run in this process at " (short-sha after) ": observed " (:observed re-after)
                                           ", evidence equal to the record's: " (= (:evidence re-after) ev)
                                           ".   Same locator at the BEFORE sha " (short-sha before) " (record: :before-evidence "
                                           (get-in proj [:revision-pair :before-evidence]) "): observed " (:observed re-before) ".") :size 10))))
      (let [y (+ 110 (* 74 (count rows)))]
        (str
         (htxt 40 y "What \"observed true\" means (observation_checks.clj:52-70):" :weight "bold" :size 11)
         (htxt 40 (+ y 18) "  the file exists at the resolved commit (git show exits 0) AND decl-present? finds the :decl string at the start of some line, followed by whitespace, `:`, `(`, `{`, `[` or end of line." :size 10)
         (htxt 40 (+ y 36) "  Nothing else is read: not the rest of the file, not who wrote the line, not whether the line is correct." :size 10)
         (htxt 40 (+ y 66) "Where the locators sit on the tick record (data/wm-runs/tick-run-record-2026-09-23-1790199409.edn):" :weight "bold" :size 11)
         (htxt 40 (+ y 84) (str "  [:decision :selection-certificate :candidates 0 :id :observation-locators] — a map of " (count cand-locators)
                                " tokens; equal to the prediction's :observation-locators: " (= cand-locators pred-locators)
                                "; equal to the projection rows' :meaning :locator: " (= cand-locators row-locators) ".") :size 10)
         (htxt 40 (+ y 102) (str "  [:decision :selection-certificate :candidates 0 :observation-locators] (outside the :id map) is " (pr-str bare) " on this record.") :size 10)
         (htxt 40 (+ y 132) "The before-sha result is NOT a record value: the record says :before-evidence :not-measured and the enactment file has no :before-token-evidence." :size 10)
         (htxt 40 (+ y 150) (str "It is C4 run here at " (short-sha before) ", shown so the reader can see which of the six lines the revision pair changed.") :size 10)
         (htxt 40 (+ y 172) "Evidence only: the record's rows are unchanged; the re-runs are read-only git reads at pinned shas." :size 9)))))))

;; ---------------------------------------------------------------------------
;; Figure 3 — the comparison and the absent truth leg
;; ---------------------------------------------------------------------------

(defn fig3-comparison [close]
  (let [cmp (comparison close)
        j (judgment close)
        row (first (:tokens cmp))
        tok (:token row)
        ev (get-in row [:measurement :result :evidence])
        src (:measurement-source cmp)
        src-sha (file-sha256 (:path src))
        src-record (when src-sha (read-record (:path src)))
        ai (:accepted-increment j)
        ai-ev (get-in ai [:evidence :produced-token-results tok :evidence])
        acc (get-in ai [:evidence :acceptance-result :evidence])
        pairs (pair/pairs-from-comparison {:comparison cmp :occurrence (:occurrence j)
                                           :reviewed-revision (:artifact-sha cmp)})
        p (first pairs)
        bad-verdict (pair/build-pair {:occurrence (:occurrence j) :token tok :token-row row
                                      :reviewed-revision (:artifact-sha cmp) :truth ai})
        bad-same (pair/build-pair {:occurrence (:occurrence j) :token tok :token-row row
                                   :reviewed-revision (:artifact-sha cmp)
                                   :truth {:truth true :truth-source :second-c4-read :adjudicator "reader"
                                           :evidence ev :adjudicated-at "later"}})
        verdicts [[:observation-missing "any missing kind (:66-72)"]
                  [:predicted-and-observed "(pos? predicted) and observed"]
                  [:predicted-not-observed "(pos? predicted) and not observed"]
                  [:not-predicted-observed "predicted 0 and observed"]
                  [:neither "predicted 0 and not observed"]]
        width 1240 height 760]
    (hsvg
     width height
     (str
      (htxt 40 36 "The comparison record of 76/002 — predicted vs observed, the verdict, and no truth leg" :size 15 :weight "bold")
      (htxt 40 58 (str "007-closed.edn [:payload :judgment :token-outcome-comparison]: :schema " (:schema cmp) ", :status " (:status cmp)
                       ", :artifact-sha " (short-sha (:artifact-sha cmp)) ", " (count (:measurements cmp)) " measurement rows, " (count (:tokens cmp)) " wanted row.") :size 11)
      (box 40 80 (- width 80) 60 "#dceee3" "#287447")
      (htxt 50 100 (str (token-name tok) "   :predicted " (:predicted row) "   :observed " (:observed row) "   :verdict " (:verdict row)) :weight "bold" :size 13)
      (htxt 50 122 (str ":measurement :result {:observed " (get-in row [:measurement :result :observed]) " :check " (get-in row [:measurement :result :check])
                        " :evidence {:resolved-sha " (short-sha (:resolved-sha ev)) " :decl " (pr-str (:decl ev)) " :file-present " (:file-present ev) "}}"
                        "   resolved-sha = :artifact-sha: " (= (:resolved-sha ev) (:artifact-sha cmp))) :mono true :size 10)
      (htxt 40 170 "compare-outcomes (token_outcome.clj:48-82) — the verdict is a comparison of the frozen prediction with the locator's report, never with a truth:" :weight "bold" :size 11)
      (apply str
             (for [[i [v cond-text]] (map-indexed vector verdicts)
                   :let [y (+ 190 (* 20 i)) taken? (= v (:verdict row))]]
               (htxt 50 y (str (if taken? "-> " "   ") (pr-str v) "   " cond-text (when taken? "   <- this row")) :mono true :size 11 :weight (when taken? "bold"))))
      (htxt 40 306 "Where the measurement came from:" :weight "bold" :size 11)
      (htxt 50 324 (str ":measurement-source " (short-target (:path src))) :mono true :size 10)
      (htxt 50 342 (str "  recorded :sha256 " (:sha256 src)) :mono true :size 10)
      (htxt 50 360 (str "  raw-byte sha256 of that file now: " (or src-sha "file absent") "   equal: " (= src-sha (:sha256 src))
                        ";  the file's :after-token-evidence equals the comparison's :measurements: " (= (:after-token-evidence src-record) (:measurements cmp))) :mono true :size 10)
      (htxt 50 378 (str "  the route-attestation copy [:payload :judgment :route-attestation :token-outcome-comparison] equals the root comparison: "
                        (= cmp (get-in j [:route-attestation :token-outcome-comparison]))) :mono true :size 10)
      (htxt 40 410 "The accepted-increment verdict on the same close is built from the same read:" :weight "bold" :size 11)
      (htxt 50 428 (str "[:payload :judgment :accepted-increment] :accepted? " (:accepted? ai) "; :criterion-step " (pr-str (:criterion-step ai))
                        "; :measured-tokens " (str/join ", " (map token-name (:measured-tokens ai)))) :mono true :size 10)
      (htxt 50 446 (str "  conjunct (b) :produced-token-results[" (token-name tok) "] :evidence equals the comparison row's :evidence map: " (= ai-ev ev)
                        " (accepted_increment.clj:71-76 re-reads the locator at the after-revision).") :mono true :size 10)
      (htxt 50 464 (str "  conjunct (c) :acceptance-result :evidence {:sha " (pr-str (:sha acc)) " :resolved-sha " (short-sha (:resolved-sha acc)) " :decl " (pr-str (:decl acc))
                        "} — (:78-81) passes the acceptance locator as declared, so HEAD resolved at close time, not at :artifact-sha.") :mono true :size 10)
      (htxt 40 496 "The pair builder on this row (token_outcome_pair.clj:449, commit 54295ca0) — the record predates it and carries no :token-outcome-pairs key:" :weight "bold" :size 11)
      (htxt 50 514 (str ":token-outcome-pairs on record? " (contains? cmp :token-outcome-pairs) "   pairs built here: " (count pairs) "   pair-ok? " (every? pair/pair-ok? pairs)) :mono true :size 10)
      (htxt 50 532 (str ":observation {:observed " (get-in p [:observation :observed]) " :check " (get-in p [:observation :check]) " :observation-source " (get-in p [:observation :observation-source]) "}") :mono true :size 10)
      (htxt 50 550 (str ":truth " (pr-str (:truth p)) "   :estimable? " (:estimable? p) "   :ineligibility-reasons " (pr-str (:ineligibility-reasons p))) :mono true :size 10 :weight "bold")
      (htxt 50 568 (str ":revision-pair " (pr-str (update (:revision-pair p) :after short-sha)) "   :pair-sha256 " (subs (:pair-sha256 p) 0 23) "…") :mono true :size 10)
      (htxt 40 600 "Two bad cases constructed from this record and run through build-pair (:384):" :weight "bold" :size 11)
      (htxt 50 618 (str "  the :accepted-increment map offered as :truth  ->  :truth " (pr-str (:truth bad-verdict)) ", :estimable? " (:estimable? bad-verdict)) :mono true :size 10)
      (htxt 50 636 (str "  a boolean truth carrying the row's own C4 evidence map  ->  pair :status " (:status bad-same) " :kind " (:kind bad-same) ", :estimable? " (:estimable? bad-same)) :mono true :size 10)
      (htxt 40 670 "What the record does not contain: any value answering whether the ticket's restoration was really accepted by a channel other than reading the Status line at 97e17e10." :size 10)
      (htxt 40 688 "The verdict :predicted-and-observed says the scorer's 175/256 and the C4 read agreed; it says nothing about whether the C4 read was right (OBS-D §1.1, §4.3)." :size 10)
      (htxt 40 720 "Evidence only: the pairs and bad cases are computed here on the recorded rows; the record is unchanged." :size 9)))))

;; ---------------------------------------------------------------------------
;; Figure 4 — the kernel example on three closes
;; ---------------------------------------------------------------------------

(defn- kernel-panel [label close y width]
  (let [j (judgment close)
        ke (kernel close)
        proj (:observation-projection ke)
        src (:observation-source ke)
        src-sha (when (:path src) (file-sha256 (:path src)))
        obs (when (= :admitted (:status proj)) (vals (:observations proj)))
        trues (count (filter #(true? (get-in % [:artifact-observation :observed])) obs))
        falses (count (filter #(false? (get-in % [:artifact-observation :observed])) obs))
        pairs (pair/pairs-from-kernel-example {:kernel-example ke :occurrence (:occurrence j)
                                               :reviewed-revision (get-in ke [:artifact :sha])})
        wanted-lines (for [t (:tokens ke)]
                       (str "   wanted " (token-name (:token t)) " :predicted " (:predicted t) " :observed " (pr-str (:observed t))))
        lines
        (if (nil? ke)
          [(str "[:payload :judgment :kernel-example] is " (pr-str ke) " (key present: " (contains? j :kernel-example) ")")
           (str ":outcome " (:outcome j) "  :failure-kind " (:failure-kind j) "  :occurrence " (pr-str (:occurrence j))
                "  root :token-outcome-comparison " (pr-str (comparison close)))
           (str "[:route-attestation :token-outcome-comparison] " (pr-str (get-in j [:route-attestation :token-outcome-comparison])))
           (str "pairs-from-kernel-example on the nil carrier: " (count pairs) " pair, :observation " (pr-str (select-keys (get-in (first pairs) [:observation]) [:status :reason]))
                ", :token " (pr-str (:token (first pairs))) ", :estimable? " (:estimable? (first pairs)))]
          (concat
           [(str ":schema " (:schema ke) "  :status " (:status ke) "  :use " (:use ke) "  :causal-attribution " (:causal-attribution ke)
                 "  :close-outcome " (:close-outcome ke) "  :disposition " (pr-str (:disposition ke)))
            (str ":observation-source " (short-target (:path src)) "  :sha256 " (short-sha (:sha256 src)) "…  raw-byte sha256 of that file now equal: " (= src-sha (:sha256 src)))
            (str ":observation-projection :status " (:status proj) (when (:kind proj) (str " :kind " (:kind proj)))
                 "  :revision-pair " (pr-str (some-> (:revision-pair proj) (update :before short-sha) (update :after short-sha)))
                 "  :artifact " (pr-str (let [a (:artifact ke)] (if (:sha a) (update a :sha short-sha) a))))
            (str ":missingness " (pr-str (update (:missingness ke) :missing-tokens #(into (sorted-set) (map token-name %)))))
            (if obs
              (str "complete observation map [:observation-projection :observations]: " (count obs) " tokens, " trues " observed true, " falses " observed false, "
                   (- (count obs) trues falses) " typed missing;  wanted :tokens rows: " (count (:tokens ke)))
              (str "no admitted observations; wanted :tokens rows: " (count (:tokens ke))))]
           wanted-lines
           [(str "projection :scope " (pr-str (:scope proj)) (when (:consumption proj) (str "  :consumption " (:consumption proj))))
            (str "pairs-from-kernel-example here: " (count pairs) " pairs, estimable " (count (filter :estimable? pairs))
                 ", observation legs: " (pr-str (frequencies (map #(or (get-in % [:observation :kind]) (get-in % [:observation :observed])) pairs)))
                 ", truth legs: " (pr-str (frequencies (map #(get-in % [:truth :reason]) pairs))))]))
        h (+ 34 (* 18 (count lines)))]
    [(str (box 40 y (- width 80) h (if (nil? ke) "#fdf6ec" "#f6f7f8") "#68717b" :dash (when (nil? ke) "6 4"))
          (htxt 50 (+ y 20) label :weight "bold")
          (apply str (for [[i l] (map-indexed vector lines)]
                       (htxt 50 (+ y 38 (* 18 i)) l :mono true :size 9.5))))
     h]))

(defn fig4-kernel [c76 c75 c70]
  (let [width 1240
        [p76 h76] (kernel-panel "machinery-76 attempt-002 (run 2026-09-23-1790199409)" c76 90 width)
        [p75 h75] (kernel-panel "machinery-75 attempt-002 (agent-unavailable)" c75 (+ 90 h76 16) width)
        [p70 h70] (kernel-panel "machinery-70 attempt-001 (run 2026-09-22-1790037762)" c70 (+ 90 h76 16 h75 16) width)
        y-end (+ 90 h76 16 h75 16 h70 16)]
    (hsvg
     width (+ y-end 110)
     (str
      (htxt 40 36 "The kernel example on three closes — an observation carrier with typed absences, and no truth field" :size 15 :weight "bold")
      (htxt 40 58 "[:payload :judgment :kernel-example], schema :wm/aligned-kernel-example-v1, built by kernel_example.clj align (:54) via collect (:148) and retained by full_loop_runner.clj retain-kernel-example! (:3186)." :size 11)
      p76 p75 p70
      (htxt 40 (+ y-end 10) "The example carries the same prediction the comparison carries (:prediction, :prediction-sha256), the verified projection (:observation-projection), and the file it was aligned from (:observation-source)." :size 10)
      (htxt 40 (+ y-end 28) "Its :tokens are only the wanted projection; the complete token population is [:observation-projection :observations] (OBS-D Revision 2 §R2.1). A refused execution yields typed missing observations, never false (:56)." :size 10)
      (htxt 40 (+ y-end 46) "No key on any of the three says whether a token was really produced: :causal-attribution :not-established, and the projection's :scope lists what it :does-not-establish." :size 10)
      (htxt 40 (+ y-end 76) "Evidence only: the pair counts are computed here on the recorded carriers; the records are unchanged." :size 9)))))

;; ---------------------------------------------------------------------------
;; Figure 5 — what A is today: the scorer's rates, the identity kernel, the census
;; ---------------------------------------------------------------------------

(defn- census-row [path]
  (let [r (read-record path)
        j (judgment r)
        cmp (:token-outcome-comparison j)
        route-cmp (get-in j [:route-attestation :token-outcome-comparison])
        ke (:kernel-example j)
        proj (:observation-projection ke)
        cmp-rows (:tokens cmp)
        count-obs (fn [xs] (let [t (count (filter true? xs)) f (count (filter false? xs))]
                             [t f (- (count xs) t f)]))
        kernel-obs (if (= :admitted (:status proj))
                     (map #(get-in % [:artifact-observation :observed]) (vals (:observations proj)))
                     (map :observed (:tokens ke)))
        pairs (when ke (pair/pairs-from-kernel-example {:kernel-example ke :occurrence (:occurrence j)
                                                        :reviewed-revision (get-in ke [:artifact :sha])}))
        [n a] (rest (re-find #"machinery-(\d+)/.*attempt-(\d+)" path))]
    {:label (str n "/" a)
     :run (some-> (get-in j [:occurrence :run/id]) (str/replace "2026-" ""))
     :comparison (cond (= :compared (:status cmp)) (count-obs (map :observed cmp-rows))
                       (and (nil? cmp) (= :absent (:status route-cmp))) (str "absent: " (:reason route-cmp))
                       :else (str "status " (pr-str (:status cmp))))
     :kernel (cond (nil? ke) "carrier nil"
                   :else (count-obs kernel-obs))
     :estimable (count (filter :estimable? pairs))
     :pairs (count pairs)}))

(defn fig5-A [tick-record close-76 census]
  (let [cert (get-in tick-record [:decision :selection-certificate])
        model-rates (get-in cert [:precision-family :model :rates])
        model-kind (get-in cert [:precision-family :model :kind])
        scoring-kind (get-in cert [:scoring 0 :rates-provenance :model :kind])
        gtd-a (get-in cert [:g-term-decomposition :policies 0 :terms :A])
        cand (get-in cert [:candidates 0])
        proj (projection close-76)
        universe (set (keys (:observations proj)))
        zero-rates (zipmap universe (repeat {:false-neg 0 :false-pos 0}))
        full universe
        five (disj universe [target :restoration-accepted])
        a-ff (m/token-likelihood zero-rates full full)
        a-f5 (m/token-likelihood zero-rates full five)
        a-5f (m/token-likelihood zero-rates five full)
        totals (reduce (fn [acc row]
                         (-> acc
                             (update :cmp #(if (vector? (:comparison row)) (mapv + % (:comparison row)) %))
                             (update :ker #(if (vector? (:kernel row)) (mapv + % (:kernel row)) %))
                             (update :est + (:estimable row))
                             (update :pairs + (:pairs row))))
                       {:cmp [0 0 0] :ker [0 0 0] :est 0 :pairs 0} census)
        width 1240 height (+ 560 (* 18 (count census)))]
    (hsvg
     width height
     (str
      (htxt 40 36 "What A is on the records — the scorer's rates, the identity kernel, and the census" :size 15 :weight "bold")
      (htxt 40 66 "(a) The tick record 1790199409, what the scorer consumed:" :weight "bold" :size 11)
      (htxt 50 84 (str "[:decision :selection-certificate :precision-family :model :kind] " model-kind "   :rates " (pr-str model-rates)) :mono true :size 10)
      (htxt 50 102 (str "[:decision :selection-certificate :scoring 0 :rates-provenance :model :kind] " scoring-kind "  (observation_model.clj:257-285: class emission, ambiguity 0.0, no token rates)") :mono true :size 10)
      (htxt 50 120 (str "[:decision :selection-certificate :g-term-decomposition :policies 0 :terms :A] " (pr-str gtd-a)) :mono true :size 10)
      (htxt 50 138 (str "candidate C1: :g " (:g cand) "  :f " (pr-str (:f cand)) " (:f-status " (:f-status cand) ")  — G was computed by class emission; no A(o|s) entered any number on this click.") :mono true :size 10)
      (htxt 40 172 "(b) The token-level A that exists in code, and what it would need:" :weight "bold" :size 11)
      (htxt 50 190 "token-likelihood (cascade_model_manifest.clj:191-216; Lean TokenObservation.lean:31-34 AdjudicationRates, :39 tokenLikelihood): A(o|s) = prod over tokens v of" :size 10)
      (htxt 50 208 "  (v in s ? (v in o ? 1 - falseNeg v : falseNeg v) : (v in o ? falsePos v : 1 - falsePos v)) — it needs {token {:false-neg r :false-pos r}} for EVERY token, exact rationals in [0,1]." :mono true :size 10)
      (htxt 50 226 "Where such rates come from today: efe.clj:1103-1105 — with no :adjudication-rates declared, every token gets {:false-neg 0 :false-pos 0}; observation_rates.clj:122-152 gives" :size 10)
      (htxt 50 244 "  :checkable classes the same exact zero {:basis :checkable}; a :judgement class with no admitted rate is a typed :unsupported-class refusal. g_term_decomposition.clj:68-70 names the all-zero map :identity-kernel." :size 10)
      (htxt 50 268 (str "Under the all-zero rates over the six tokens of 76/002, computed here: A(o = the six | s = the six) = " a-ff
                        ",  A(o = five | s = the six) = " a-f5 ",  A(o = the six | s = five) = " a-5f ".") :mono true :size 10 :weight "bold")
      (htxt 50 286 "The identity kernel says the observation IS the state. It has no error rate to estimate, and (c) shows there is no pair on any record to estimate one from." :size 10)
      (htxt 40 320 "(c) Census of the 14 closes, recomputed here (OBS-D Revision 2 §R2.2 reports 4/10/5, 31/33/5, eligible 0). Cells: true / false / typed-missing." :weight "bold" :size 11)
      (htxt 50 340 (format "%-8s %-17s %-32s %-26s %s" "N/att" "run id" "comparison rows" "kernel observations" "pairs built / estimable") :mono true :size 10 :weight "bold")
      (apply str
             (for [[i row] (map-indexed vector census)
                   :let [y (+ 358 (* 18 i))
                         fmt (fn [v] (if (vector? v) (str/join " / " v) (str v)))]]
               (htxt 50 y (format "%-8s %-17s %-32s %-26s %s" (:label row) (or (:run row) "no occurrence") (fmt (:comparison row)) (fmt (:kernel row))
                                  (str (:pairs row) " / " (:estimable row))) :mono true :size 10)))
      (let [y (+ 358 (* 18 (count census)) 8)]
        (str
         (htxt 50 y (format "%-8s %-17s %-32s %-26s %s" "total" "" (str/join " / " (:cmp totals)) (str/join " / " (:ker totals)) (str (:pairs totals) " / " (:est totals))) :mono true :size 10 :weight "bold")
         (htxt 50 (+ y 18) (str "Matches Revision 2: comparison " (= (:cmp totals) [4 10 5]) ", kernel " (= (:ker totals) [31 33 5]) ", eligible pairs " (= 0 (:est totals)) ".") :mono true :size 10)
         (htxt 40 (+ y 48) "Kernel counts use the complete admitted observation map, or the wanted-token typed absences when the projection was not admitted; 75/002 has no carrier and contributes no row to any count." :size 10)
         (htxt 40 (+ y 66) "Every pair's truth leg is {:status :missing :reason :no-independent-truth-channel} (full_loop_runner.clj:3144-3148 says so of the live producer too). Estimable pairs: zero; measured A: none." :size 10)
         (htxt 40 (+ y 96) "Evidence only: (a) and (c) are read from the records; (b)'s three values are token-likelihood run here on the recorded universe with the code's own default rates." :size 9)))))))

;; ---------------------------------------------------------------------------
;; Figure 6 — narrative: 76/002 from prediction to close
;; ---------------------------------------------------------------------------

(defn fig6-narrative [sel close]
  (let [pred (prediction sel)
        j (judgment close)
        cmp (comparison close)
        row (first (:tokens cmp))
        proj (projection close)
        src (:measurement-source cmp)
        src-record (when (file-sha256 (:path src)) (read-record (:path src)))
        occ (:occurrence j)
        trials (get-in j [:learning-trial-receipt :trials])
        after (get-in proj [:revision-pair :after])
        [commit-date commit-subject] (git-commit-line after)
        steps [["selection" (:recorded-at sel)
                (str "002-selection.edn recorded. :selected-cascade " (get-in sel [:payload :judgment :selected-cascade])
                     "; prediction frozen: " (token-name (:token row)) " at " (:predicted row) " over horizon " (:horizon pred) ".")
                (str "carry-occurrence-id " (:carry-occurrence-id proj))]
               ["after revision" (str "git show: " commit-date)
                (str "commit " (short-sha after) " in futon2 (" (pr-str commit-subject) " — subject and date from git show, not record values); before " (short-sha (get-in proj [:revision-pair :before])) ".")
                (str ":revision-pair :before-evidence " (get-in proj [:revision-pair :before-evidence]))]
               ["observation" (:observed-at src-record)
                (str "enactment file " (last (str/split (:path src) #"/")) " :observed-at; " (count (:after-token-evidence src-record)) " C4 reads at " (short-sha (get-in src-record [:revision-pair :after])) ", all :observed true.")
                (str "raw sha256 " (short-sha (:sha256 src)) "… (recorded on the close as :measurement-source and :observation-source)")]
               ["close" (:recorded-at close)
                (str "007-closed.edn: comparison :verdict " (:verdict row) "; kernel example :status " (:status (kernel close)) " with " (count (:observations proj)) "/" (count (:observations proj)) " observed true; "
                     ":accepted-increment :accepted? " (get-in j [:accepted-increment :accepted?]) "; :outcome " (:outcome j) ".")
                (str "learning trials: " (str/join "; " (for [t trials] (str (token-name (:effect t)) " " (:status t) (when (:reason t) (str " (" (:reason t) ")")) ", counted? " (:counted? t)))))]]
        width 1240 height 520]
    (hsvg
     width height
     (str
      (htxt 40 36 "76/002 from prediction to close — one occurrence, every value with its key path" :size 15 :weight "bold")
      (htxt 40 58 (str "occurrence: :run/id " (:run/id occ) "  :cohort/id " (pr-str (:cohort/id occ)) "  :attempt/id " (:attempt/id occ)
                       "  :action/id " (:action/id occ) "  :action/value-sha256 " (short-sha (:action/value-sha256 occ)) "…") :size 10 :mono true)
      (apply str
             (for [[i [label at l1 l2]] (map-indexed vector steps)
                   :let [y (+ 90 (* 92 i))]]
               (str (box 40 y (- width 80) 78 "#f6f7f8" "#68717b")
                    (htxt 50 (+ y 20) (str label "   " at) :weight "bold")
                    (htxt 50 (+ y 40) l1 :size 10)
                    (htxt 50 (+ y 58) l2 :size 10 :mono true)
                    (when (pos? i) (arrow 120 (- y 14) 120 y)))))
      (htxt 40 470 (str "Prediction and observation agree on this occurrence (" (:predicted row) " predicted, observed " (:observed row) "). The close carries no value that says whether the observation was right.") :size 10)
      (htxt 40 490 "Timestamps are the records' :recorded-at / :observed-at; the commit time is from git and labelled so." :size 9)))))

;; ---------------------------------------------------------------------------

(defn generate! []
  (let [sel (read-record sel-76)
        c76 (read-record close-76)
        c75 (read-record close-75)
        c70 (read-record close-70)
        t (read-record tick)
        h-sel (sha8 sel-76) h-76 (sha8 close-76) h-75 (sha8 close-75) h-70 (sha8 close-70) h-tick (sha8 tick)
        census (mapv census-row (census-paths))
        h-census (hex (.digest (MessageDigest/getInstance "SHA-256")
                               (.getBytes (str/join "\n" (map file-sha256 (census-paths))) "UTF-8")) 4)]
    (spit-svg (str "fig1-prediction-76002-" h-sel ".svg") (fig1-prediction sel))
    (spit-svg (str "fig2-locators-76002-" h-76 "-" h-tick ".svg") (fig2-locators c76 t sel))
    (spit-svg (str "fig3-comparison-76002-" h-76 ".svg") (fig3-comparison c76))
    (spit-svg (str "fig4-kernel-" h-76 "-" h-75 "-" h-70 ".svg") (fig4-kernel c76 c75 c70))
    (spit-svg (str "fig5-A-" h-tick "-census-" h-census ".svg") (fig5-A t c76 census))
    (spit-svg (str "fig6-narrative-76002-" h-sel "-" h-76 ".svg") (fig6-narrative sel c76))
    (println "census digest" h-census "over" (count census) "closes")
    (doseq [row census] (println " " row))))
