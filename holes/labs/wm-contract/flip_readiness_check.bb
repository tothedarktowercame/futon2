#!/usr/bin/env bb
;; flip_readiness_check.bb -- worklist row :U32, THE FLIP-READINESS GATE.
;;
;;   bb flip_readiness_check.bb              ; check the committed artifacts
;;   bb flip_readiness_check.bb --emit       ; regenerate them from live sources
;;   bb flip_readiness_check.bb --summary    ; print the VERDICTS block only
;;   bb flip_readiness_check.bb --deposit <run-id>   ; one run-era ledger row (RE3)
;;
;; WHAT THIS IS. Joe, 2026-09-03: "if we are gonna flip the machine on, we
;; should get a reasonably confident state ... so it is gathering empirical data
;; against the best effort system, not some kind of partial system." This makes
;; "best effort" a checkable object. For each J-gated flip the catalog declares,
;; it derives one line per blocking red and reports READY or BLOCKED-ON [lines].
;;
;; WHAT THIS IS NOT. It performs no flip, writes no ruling, and edits no
;; registry. A green checklist is the evidence a flip goes to Joe WITH; it is
;; not permission. Where a declared requirement cannot be derived from a live
;; source the line is :not-derivable and counts as BLOCKING -- a requirement
;; nobody can check is not a requirement that passed.
;;
;; THE SIX LINES, and the live source each is derived from:
;;   :contract-pin     U26 feed. mathlib4 holes-contract.json source.git-sha vs
;;                     the last commit that touched Holes.lean (the comparand
;;                     C175 settled and checks/contract_authority_current.clj
;;                     uses; mathlib HEAD is the WRONG one -- a correct
;;                     regeneration moves HEAD itself).
;;   :box2-holes       U27 feed. variable-situation-accounting.edn :open-hole
;;                     rows, cross-read against runs/U27-hole-closability/
;;                     audit.edn. A hole passes only if it is closed or carries
;;                     a :flip-blocking typing.
;;   :figure5-partials U30/U31 feed. p4ng defect-repair-tally.edn: every
;;                     instance :repaired, or the line names the ones that
;;                     are not and who holds the remainder.
;;   :mission-gauges   U28 feed. The status receipt's mission-criteria-gauges
;;                     component. Applies only to a flip whose flag chain names
;;                     FUTON_WM_MISSION_C -- no other flip reads a mission's
;;                     completion criteria.
;;   :per-node-tests   Joe's standing per-box requirement: the per-node unit
;;                     tests for every R node the flipped path exercises are
;;                     green SEPARATE from any full-run. Derived from U36's
;;                     RUNTIME-VALIDATION-CATALOG.edn, whose rows are
;;                     one-namespace-one-JVM invocations -- which is what
;;                     "separate from a full run" means here -- plus a
;;                     freshness check of each named test FILE against the head
;;                     the catalog recorded its runs at.
;;   :flag-chain       The flip's own declared :requires. A required flag that
;;                     is itself a J-gated flip must be READY (transitively); a
;;                     :requires with no machine-readable flag is
;;                     :not-derivable and blocks.
;;
;; THE NODE -> TEST FILE MAP IS DERIVED, NOT CITED. A catalog row names a
;; namespace; this script turns that namespace into a path and FAILS if the
;; file is not there. Citing a pointer instead would let a row name a source
;; file (or a coverage registry) where a reader expects the test.
;;
;; Exit 0 all clear, 1 on any failure (house convention).
(require '[babashka.process :as process]
         '[cheshire.core :as json]
         '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[clojure.string :as str])

(def code-root (str (System/getProperty "user.home") "/code/"))
(def here (str code-root "futon2/holes/labs/wm-contract/"))

;; Sources. Each is overridable so negative_controls.sh can plant a defect in a
;; COPY -- the shared registries are never mutated by a control.
(def catalog-path   (or (System/getenv "CATALOG")       (str here "runs/RUNTIME-VALIDATION-CATALOG.edn")))
(def accounting-path (or (System/getenv "ACCOUNTING")   (str here "variable-situation-accounting.edn")))
(def audit-path     (or (System/getenv "HOLE_AUDIT")    (str here "runs/U27-hole-closability/audit.edn")))
(def tally-path     (or (System/getenv "TALLY")         (str code-root "p4ng/empirics-futon/defect-repair-tally.edn")))
(def receipt-path   (or (System/getenv "RECEIPT")       (str code-root "p4ng/empirics-futon/wm-status-receipt.json")))
(def contract-path  (or (System/getenv "CONTRACT_JSON") (str code-root "mathlib4/DarkTower/WarMachine/holes-contract.json")))
(def mathlib-root   (or (System/getenv "MATHLIB_ROOT")  (str code-root "mathlib4")))
(def md-path        (or (System/getenv "FLIP_MD")       (str here "runs/FLIP-READINESS.md")))
(def edn-path       (or (System/getenv "FLIP_EDN")      (str here "runs/U32-flip-readiness/flip-readiness.edn")))

(def problems (atom []))
(defn fail! [& parts] (swap! problems conj (str/join " " (map str parts))))

(defn read-edn [path what]
  (if (.isFile (io/file path))
    (edn/read-string {:default (fn [_ v] v)} (slurp path))
    (do (fail! what "not found at" path) nil)))

(def catalog    (read-edn catalog-path "the runtime-validation catalog"))
(def accounting (read-edn accounting-path "the variable-situation accounting"))
(def audit      (read-edn audit-path "the U27 hole-closability audit"))
(def tally      (read-edn tally-path "the defect-repair tally"))
(def receipt    (when (.isFile (io/file receipt-path))
                  (json/parse-string (slurp receipt-path) true)))
(when-not receipt (fail! "the status receipt not found at" receipt-path))

(def flips (:flips catalog))
(when (empty? flips) (fail! "the catalog declares no flips -- a gate over nothing passes nothing"))

(defn git [dir & argv]
  (let [r (apply process/shell {:continue true :out :string :err :string :dir dir} argv)]
    (when-not (zero? (:exit r)) (fail! "git" (str/join " " argv) "in" dir "exited" (:exit r)))
    (str/trim (:out r))))

;; ---------------------------------------------------------------------------
;; LINE 1 -- :contract-pin (U26 feed)
;; ---------------------------------------------------------------------------
(def contract-pin
  (let [f (io/file contract-path)]
    (if-not (.isFile f)
      (do (fail! "the emitted contract not found at" contract-path) {:verdict :blocked})
      (let [authority (get-in (json/parse-string (slurp contract-path) true) [:source :git-sha])
            last-change (git mathlib-root "git" "log" "-1" "--format=%H" "--" "DarkTower/WarMachine/Holes.lean")
            fresh? (= authority last-change)]
        {:verdict (if fresh? :green :blocked)
         :contract-authority authority
         :holes-last-content-change last-change
         :comparand "the last commit that touched DarkTower/WarMachine/Holes.lean, not mathlib HEAD (C175)"
         :receipt-says (:contract-pin receipt)
         :receipt-timestamp (:timestamp receipt)
         :blocked-on (when-not fresh?
                       [(format "the contract is pinned at %s while Holes.lean last changed at %s"
                                authority last-change)])}))))

;; ---------------------------------------------------------------------------
;; LINE 2 -- :box2-holes (U27 feed)
;; ---------------------------------------------------------------------------
;; "closed OR typed not-flip-blocking". Closure is read off the accounting;
;; the typing is read off the hole's own row, which is where U27 put the
;; closability fence. A hole carrying neither is reported open and blocking --
;; this script does not decide relevance for it, because deciding which holes
;; block which flip IS the typing that is missing, and inventing it here would
;; be the plant U28 was corrected for.
(def open-holes
  "Every :open-hole row of the accounting, WHOLE. Not select-keys'd: the line
   below asks whether the row carries a field, and a projection that drops the
   field would answer no for a hole that carries it."
  (vec (filter #(= :open-hole (:content-status %)) (:rows accounting))))

(defn hole-brief [h] (select-keys h [:name :row-source :area :closability :readiness :owner :flip-blocking]))

(def audit-by-name (into {} (map (juxt :name identity) (:rows audit))))

(def box2-holes
  ;; A hole clears this line only by being CLOSED (then it is not here at all)
  ;; or by carrying :flip-blocking false. The field must also be a declared
  ;; axis of the accounting: an undeclared field is a word one writer used, not
  ;; a typing the registry stands behind -- the same rule U14 applied to
  ;; :row-source and U27 to :closability.
  (let [axis-declared? (contains? (:axes accounting) :flip-blocking)
        cleared (filter #(and axis-declared? (false? (:flip-blocking %))) open-holes)
        blocking (remove (set cleared) open-holes)]
    {:verdict (if (empty? blocking) :green :blocked)
     :open-hole-count (count open-holes)
     :by-row-source (frequencies (map :row-source open-holes))
     :by-closability (frequencies (map :closability open-holes))
     :flip-blocking-axis-declared? axis-declared?
     :cleared (mapv :name cleared)
     :audit-agrees? (= (set (map :name open-holes)) (set (keys audit-by-name)))
     :blocking (mapv hole-brief blocking)
     :blocked-on
     (when (seq blocking)
       [(format "%d of %d open holes are neither closed nor typed :flip-blocking false%s"
                (count blocking) (count open-holes)
                (if axis-declared?
                  ""
                  (str "; :flip-blocking is not in variable-situation-accounting.edn's :axes at all, "
                       "which declares :closability and :readiness (U27's pre-run/run-gated fence) "
                       "and nothing about flips")))])}))

;; ---------------------------------------------------------------------------
;; LINE 3 -- :figure5-partials (U30/U31 feed)
;; ---------------------------------------------------------------------------
(def tally-instances (mapcat :instances (:classes tally)))

(def figure5-partials
  (let [unrepaired (remove #(= :repaired (:status %)) tally-instances)]
    {:verdict (if (empty? unrepaired) :green :blocked)
     :instances (count tally-instances)
     :by-status (frequencies (map :status tally-instances))
     :unrepaired (mapv #(select-keys % [:id :status]) unrepaired)
     :blocked-on (when (seq unrepaired)
                   (mapv #(format "%s is %s" (:id %) (:status %)) unrepaired))}))

;; ---------------------------------------------------------------------------
;; LINE 4 -- :mission-gauges (U28 feed)
;; ---------------------------------------------------------------------------
(def gauges (:mission-criteria-gauges receipt))

(def mission-gauges-global
  (let [details (:details gauges)
        short (remove #(= (:measurable %) (:criteria %)) details)]
    {:verdict (if (and (seq details) (empty? short)) :green :blocked)
     :criteria (:criteria gauges)
     :measurable (:measurable gauges)
     :missions (:missions gauges)
     :per-mission (mapv #(select-keys % [:mission :criteria :measurable :status]) details)
     :blocked-on (when (seq short)
                   (mapv #(format "%s measures %d of %d criteria (reasons: %s)"
                                  (:mission %) (:measurable %) (:criteria %)
                                  (str/join "," (distinct (keep :reason (:detail %)))))
                         short))}))

;; ---------------------------------------------------------------------------
;; LINE 5 -- :per-node-tests (Joe's standing per-box requirement)
;; ---------------------------------------------------------------------------
(def repo-dir {:futon2 "futon2" :futon3c "futon3c"})

(defn ns->test-file
  "A namespace name to the file that must hold it. Not a pointer lookup: a
   pointer can name a source file or a coverage registry, and this line has to
   name the TEST."
  [repo ns-name]
  (str (get repo-dir repo (name repo)) "/test/"
       (-> ns-name (str/replace "-" "_") (str/replace "." "/")) ".clj"))

(def moved-since-head
  "Per repo: the set of repo-relative paths that have changed since the head the
   catalog recorded its runs at, INCLUDING uncommitted working-tree changes. A
   recorded green over a file that has since moved is reported stale, not
   counted as fresh."
  (into {}
        (for [[repo-kw head] (:heads catalog)
              :let [dir (str code-root (get repo-dir repo-kw (name repo-kw)))]]
          [repo-kw
           (into (set (remove str/blank? (str/split-lines (git dir "git" "diff" "--name-only" head "HEAD"))))
                 (map #(str/trim (subs % 3))
                      (remove str/blank? (str/split-lines (git dir "git" "status" "--porcelain")))))])))

(def node-evidence
  "node -> the per-node rows that carry a recorded test run, with each one's
   derived test file, run exit and freshness."
  (let [runs (:test-runs catalog)]
    (reduce
     (fn [acc row]
       (if-not (and (= :per-node (:axis row)) (= :test-green (:evidence-kind row)))
         acc
         (let [run (get runs (:ns row))
               repo (:repo run)
               file (ns->test-file repo (:ns row))
               present? (.isFile (io/file (str code-root file)))
               rel (str/join "/" (rest (str/split file #"/")))
               moved? (contains? (get moved-since-head repo #{}) rel)]
           (when-not run (fail! "row" (:id row) "claims :test-green but names no recorded run"))
           (when-not present?
             (fail! "row" (:id row) "names namespace" (:ns row)
                    "which derives to" file "-- no such file, so this node names no test"))
           (update acc (:node row) (fnil conj [])
                   {:row (:id row) :ns (:ns row) :substrate (:substrate row)
                    :catalog-status (:status row) :test-file file
                    :exit (:exit run) :run-at (:at run) :head-at (get (:heads catalog) repo)
                    :state (cond (not present?) :file-missing
                                 (not (zero? (:exit run 1))) :red
                                 (not= :exists (:status row)) :typed-stale
                                 moved? :stale-green
                                 :else :fresh-green)}))))
     {} (:rows catalog))))

(def named-gaps-by-node
  (reduce (fn [acc row]
            (if (and (= :per-node (:axis row)) (= :named-gap (:status row)))
              (update acc (:node row) (fnil conj [])
                      {:row (:id row) :substrate (:substrate row) :owner-rows (:owner-rows row)})
              acc))
          {} (:rows catalog)))

(defn per-node-tests [flip]
  (let [nodes (:exercises flip)
        per (for [n nodes
                  :let [ev (get node-evidence n [])
                        fresh (filterv #(= :fresh-green (:state %)) ev)]]
              {:node n
               :verdict (if (seq fresh) :green :blocked)
               :test-files (mapv :test-file fresh)
               :evidence ev
               :named-gaps (get named-gaps-by-node n [])
               :substrates (into {} (for [[s rs] (group-by :substrate ev)]
                                      [s (mapv :state rs)]))})
        bad (remove #(= :green (:verdict %)) per)]
    {:verdict (cond (empty? nodes) :blocked (seq bad) :blocked :else :green)
     :nodes (vec per)
     :separate-from-full-run
     "each cited run is one namespace in its own JVM (catalog :test-runs :cmd), not a suite or a tick"
     :caveats
     (vec (concat
           (for [{:keys [node evidence]} per, e evidence
                 :when (#{:stale-green :typed-stale :red} (:state e))]
             (format "%s %s is %s (%s, recorded at %s)"
                     node (:row e) (name (:state e)) (:test-file e) (:head-at e)))
           (for [{:keys [node named-gaps]} per, g named-gaps]
             (format "%s has a named gap %s on the %s substrate, owned by %s"
                     node (:row g) (:substrate g) (pr-str (:owner-rows g))))))
     :blocked-on (when (seq bad)
                   (mapv #(format "%s has no fresh green per-node test" (:node %)) bad))}))

;; ---------------------------------------------------------------------------
;; LINE 6 -- :flag-chain
;; ---------------------------------------------------------------------------
(def flag-re #"FUTON_[A-Z0-9_]+")
(defn flags-in [s] (vec (distinct (re-seq flag-re (str s)))))

(def flip-flag (into {} (for [[k f] flips] [k (first (flags-in (:flip f)))])))
(def flag->flip (into {} (for [[k fl] flip-flag :when fl] [fl k])))

(defn chain-of [k]
  (let [f (get flips k)
        req (str/join " " (:requires f))
        fs (flags-in req)]
    {:requires (vec (:requires f))
     :required-flags fs
     :prerequisite-flips (vec (keep flag->flip fs))
     :env-preconditions (vec (remove flag->flip fs))}))

;; ---------------------------------------------------------------------------
;; Assemble
;; ---------------------------------------------------------------------------
(defn applies-mission-gauges? [k]
  (let [f (get flips k)]
    (boolean (some #{"FUTON_WM_MISSION_C"}
                   (flags-in (str (:flip f) " " (str/join " " (:requires f))))))))

(def base-lines
  "Everything except :flag-chain, which is transitive and needs these first."
  (into {}
        (for [[k f] flips]
          [k (cond-> {:contract-pin contract-pin
                      :box2-holes box2-holes
                      :figure5-partials figure5-partials
                      :per-node-tests (per-node-tests f)}
               true
               (assoc :mission-gauges
                      (if (applies-mission-gauges? k)
                        mission-gauges-global
                        {:verdict :n/a
                         :because (str "the flip's flag chain does not name FUTON_WM_MISSION_C, "
                                       "so no mission's completion criteria are read")})))])))

(defn base-ready? [k]
  (every? #(#{:green :n/a} (:verdict %)) (vals (get base-lines k))))

(def flag-chain-lines
  (let [state (atom {})]
    (letfn [(resolve! [k seen]
              (or (get @state k)
                  (let [c (chain-of k)
                        line (cond
                               (contains? seen k)
                               (assoc c :verdict :blocked
                                      :blocked-on [(format "the required-flag chain is cyclic at %s" k)])

                               (empty? (:required-flags c))
                               (assoc c :verdict :not-derivable
                                      :blocked-on
                                      [(format (str ":requires names no environment flag, so the chain cannot be "
                                                    "derived; the declared requirement is carried verbatim: %s")
                                               (pr-str (:requires c)))])

                               :else
                               (let [unready (vec (for [p (:prerequisite-flips c)
                                                        :let [pl (resolve! p (conj seen k))]
                                                        :when (not (and (base-ready? p)
                                                                        (#{:green} (:verdict pl))))]
                                                    p))]
                                 (assoc c
                                        :verdict (if (seq unready) :blocked :green)
                                        :blocked-on
                                        (when (seq unready)
                                          (mapv #(format "prerequisite flip %s is not READY" %) unready)))))]
                    (swap! state assoc k line)
                    line)))]
      (into {} (for [k (keys flips)] [k (resolve! k #{})])))))

(def lines
  (into {} (for [[k m] base-lines] [k (assoc m :flag-chain (get flag-chain-lines k))])))

(def line-order [:contract-pin :box2-holes :figure5-partials :mission-gauges :per-node-tests :flag-chain])

(def verdicts
  (into (sorted-map)
        (for [[k m] lines]
          (let [blocking (vec (for [l line-order
                                    :when (not (#{:green :n/a} (:verdict (get m l))))]
                                l))]
            [k {:verdict (if (empty? blocking) :ready :blocked)
                :blocked-on blocking}]))))

(def verdict-lines
  (for [[k v] verdicts]
    (format "FLIP %-16s %s"
            (name k)
            (if (= :ready (:verdict v))
              "READY"
              (str "BLOCKED-ON [" (str/join " " (map name (:blocked-on v))) "]")))))

(def verdict-block
  (str/join "\n" (concat ["```"] verdict-lines
                         [(format "GATE: %d flips | %d READY | %d BLOCKED | lines per flip: %s"
                                  (count verdicts)
                                  (count (filter #(= :ready (:verdict %)) (vals verdicts)))
                                  (count (filter #(= :blocked (:verdict %)) (vals verdicts)))
                                  (str/join " " (map name line-order)))
                          "```"])))

;; ---------------------------------------------------------------------------
;; Emit / check / summary
;; ---------------------------------------------------------------------------
(defn sidecar []
  {:schema :wm/flip-readiness-v1
   :row :U32
   :emitted-at (str (java.time.LocalDate/now))
   :catalog-as-of (:as-of catalog)
   :emitted-by "futon2/holes/labs/wm-contract/flip_readiness_check.bb"
   :nothing-is-flipped
   (str "This artifact performs no flip and writes no ruling. A green line is "
        "evidence a flip goes to Joe WITH; it is not permission.")
   :sources {:catalog "futon2/holes/labs/wm-contract/runs/RUNTIME-VALIDATION-CATALOG.edn"
             :accounting "futon2/holes/labs/wm-contract/variable-situation-accounting.edn"
             :hole-audit "futon2/holes/labs/wm-contract/runs/U27-hole-closability/audit.edn"
             :tally "p4ng/empirics-futon/defect-repair-tally.edn"
             :receipt "p4ng/empirics-futon/wm-status-receipt.json"
             :contract "mathlib4/DarkTower/WarMachine/holes-contract.json"}
   :line-order line-order
   :verdicts verdicts
   ;; The first three lines are properties of the tree, not of a flip: they are
   ;; recorded ONCE here and referenced by every flip, so a reader cannot come
   ;; away thinking six different measurements were taken.
   :whole-tree-lines {:contract-pin contract-pin
                      :box2-holes box2-holes
                      :figure5-partials figure5-partials
                      :mission-gauges mission-gauges-global}
   :per-flip
   (into (sorted-map)
         (for [[k f] flips]
           [k {:flip (:flip f)
               :read-at (:read-at f)
               :owner-row (:owner-row f)
               :exercises (:exercises f)
               :mission-gauges (get-in lines [k :mission-gauges])
               :per-node-tests (get-in lines [k :per-node-tests])
               :flag-chain (get-in lines [k :flag-chain])}]))})

(defn write-artifacts! []
  (io/make-parents (io/file edn-path))
  (spit edn-path (with-out-str (pp/pprint (sidecar))))
  (let [md (slurp md-path)
        marked (str/replace md
                            #"(?s)<!-- BEGIN flip_readiness_check --\>.*?<!-- END flip_readiness_check --\>"
                            (str/re-quote-replacement
                             (str "<!-- BEGIN flip_readiness_check -->\n" verdict-block
                                  "\n<!-- END flip_readiness_check -->")))]
    (spit md-path marked))
  (println "flip_readiness_check: emitted" md-path "and" edn-path))

;; ---------------------------------------------------------------------------
;; --deposit <run-id> -- one run-era ledger row (RE3)
;; ---------------------------------------------------------------------------
;;
;; WHAT VERDICT THIS DEPOSITS, and why it is usually not the one printed above.
;; Flip readiness is a property of the TREE at the moment of asking: it reads
;; the contract, the accounting, the tally, the receipt and the catalog as they
;; stand. A ledger row, by contrast, says something about a named RUN. So the
;; deposit asks the run store, not the tree:
;;
;;   the run store holds a flip-readiness artifact -> deposit its verdict
;;   it does not                                   -> :typed-absence, notes
;;                                                    naming what was missing
;;
;; Retro-depositing today's green against a run taken days ago would attach a
;; tree property to a run-id and read, in the fold, as though the check had run
;; with that run. The live derivation is still recorded -- in the receipt this
;; row points at -- so nothing is lost, but it is labelled for what it is.
;; A failing check (problems) deposits :red, because that failure is about the
;; tree the deposit is being made from and a reader must see it.
(defn deposit-paths [run-id]
  {:run-store (str here "runs/" run-id)
   :receipt-rel (str "holes/labs/wm-contract/runs/RE3-check-deposits/flip-readiness-" run-id ".edn")
   :receipt (str here "runs/RE3-check-deposits/flip-readiness-" run-id ".edn")})

(defn run-store-flip-artifacts
  "Files in the run's own store that came from this check. The match is on the
   name, so a run that recorded one under any spelling of flip-readiness is
   found; nothing here guesses from a registry."
  [run-store]
  (->> (.listFiles (io/file run-store))
       (filter #(.isFile %))
       (map #(.getName %))
       (filter #(re-find #"(?i)flip[-_]?readiness" %))
       sort vec))

(defn deposit-receipt [run-id]
  (let [{:keys [run-store]} (deposit-paths run-id)
        store-files (if (.isDirectory (io/file run-store))
                      (vec (sort (map #(.getName %) (filter #(.isFile %) (.listFiles (io/file run-store))))))
                      nil)
        contemporaneous (when store-files (run-store-flip-artifacts run-store))]
    ;; array-map, not a literal: at this size a map literal is a hash-map and
    ;; would print in hash order, so the receipt would not be stable to read.
    (array-map
     :schema :wm/run-era-deposit-receipt-v1
     :row :RE3
     :check :flip-readiness
     :run-id run-id
     :produced-by "holes/labs/wm-contract/flip_readiness_check.bb --deposit"
     :deterministic
     (str "No wall-clock field. This receipt is rewritten byte-identically on every "
          "deposit, which is what lets the deposit require it to be committed and "
          "unmodified, and lets the same deposit repeat as :already-present.")
     :run-store {:dir (str "holes/labs/wm-contract/runs/" run-id)
                 :exists? (boolean store-files)
                 :holds store-files
                 :flip-readiness-artifacts contemporaneous}
     :verdict-deposited (cond (seq @problems) :red
                              (seq contemporaneous) :green
                              :else :typed-absence)
     :live-derivation
     {:read-from "the tree at deposit time, NOT the tree the run was taken at"
      :artifact "holes/labs/wm-contract/runs/U32-flip-readiness/flip-readiness.edn"
      :gate {:flips (count verdicts)
             :ready (count (filter #(= :ready (:verdict %)) (vals verdicts)))
             :blocked (count (filter #(= :blocked (:verdict %)) (vals verdicts)))
             :lines line-order}
      :verdicts verdicts
      :problems (vec @problems)}
     :not-what-this-says
     (str "The live derivation above is a property of the tree at deposit time. It is "
          "recorded so the absence is legible, and it is NOT the deposited verdict."))))

(defn deposit-notes [run-id receipt]
  (let [d (:live-derivation receipt)]
    (case (:verdict-deposited receipt)
      :typed-absence
      (str "runs/" run-id "/ holds no flip-readiness artifact"
           (if (get-in receipt [:run-store :exists?])
             (str " -- the run store holds " (str/join ", " (get-in receipt [:run-store :holds])) " and nothing from this check")
             " -- the run store directory does not exist")
           ", so no flip-readiness verdict contemporaneous with this run was ever recorded. "
           "The check run at deposit time reports PASS with "
           (get-in d [:gate :ready]) " of " (get-in d [:gate :flips]) " flips READY; that is a "
           "property of the tree at deposit time, is recorded in the artifact this row points at, "
           "and is not deposited as this run's verdict.")
      :red
      (str "the flip-readiness check FAILS at deposit time: "
           (str/join "; " (:problems d)))
      :green
      (str "read from the run store's own flip-readiness artifact(s): "
           (str/join ", " (get-in receipt [:run-store :flip-readiness-artifacts]))
           "; " (get-in d [:gate :ready]) " of " (get-in d [:gate :flips]) " flips READY"))))

(defn deposit! [run-id]
  (let [{:keys [receipt receipt-rel]} (deposit-paths run-id)
        r (deposit-receipt run-id)]
    (io/make-parents (io/file receipt))
    (spit receipt (with-out-str (pp/pprint r)))
    (println "flip_readiness_check --deposit: receipt" receipt-rel)
    (let [{:keys [exit out err]}
          (process/shell {:dir (str code-root "futon2") :out :string :err :string :continue true}
                         "bb" "holes/labs/wm-contract/run_era_ledger.bb" "--deposit"
                         "--run-id" run-id
                         "--check-id" ":flip-readiness"
                         "--verdict" (str (:verdict-deposited r))
                         "--artifact" receipt-rel
                         "--author" "flip_readiness_check.bb --deposit"
                         "--deposited-by" "RE3 -- wire the existing checks to deposit ledger rows"
                         "--notes" (deposit-notes run-id r))]
      (print out) (print err) (flush)
      (when-not (zero? exit)
        (println (format "flip_readiness_check --deposit: the ledger refused the row (exit %d)" exit))
        (println "  if the refusal is artifact-untracked or artifact-dirty, commit" receipt-rel "and re-run")
        (System/exit 1))
      (System/exit 0))))

(cond
  (contains? (set *command-line-args*) "--deposit")
  (deposit! (or (second (drop-while #(not= "--deposit" %) *command-line-args*))
                (do (println "flip_readiness_check --deposit needs a run-id") (System/exit 1))))

  (contains? (set *command-line-args*) "--summary")
  (do (println verdict-block)
      (doseq [p @problems] (println "  PROBLEM" p))
      (System/exit (if (seq @problems) 1 0)))

  (contains? (set *command-line-args*) "--emit")
  (do (when (seq @problems)
        (doseq [p @problems] (println "  PROBLEM" p))
        (println "flip_readiness_check: refusing to emit over unresolved problems")
        (System/exit 1))
      (write-artifacts!)
      (System/exit 0))

  :else
  (do
    (if (.isFile (io/file md-path))
      (when-not (str/includes? (slurp md-path) verdict-block)
        (fail! "the committed FLIP-READINESS.md does not carry the computed verdict block; expected verbatim:\n"
               verdict-block))
      (fail! "the readiness narrative not found at" md-path))
    (if (.isFile (io/file edn-path))
      (let [committed (read-edn edn-path "the readiness sidecar")]
        (when-not (= (:verdicts committed) verdicts)
          (fail! "the committed sidecar's :verdicts disagree with the live derivation --"
                 (pr-str (:verdicts committed)) "vs" (pr-str verdicts))))
      (fail! "the readiness sidecar not found at" edn-path))
    (println verdict-block)
    (println (format "flip_readiness_check: %d flips over %d lines; open holes %d; unrepaired tally instances %d; gauges %d/%d"
                     (count verdicts) (count line-order) (count open-holes)
                     (count (:unrepaired figure5-partials))
                     (:measurable gauges) (:criteria gauges)))
    (doseq [p @problems] (println "  PROBLEM" p))
    (if (seq @problems)
      (do (println (format "flip_readiness_check: FAIL (%d problems) exit-convention=0-pass/1-fail" (count @problems)))
          (System/exit 1))
      (println "flip_readiness_check: PASS exit-convention=0-pass/1-fail"))))
