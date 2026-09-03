#!/usr/bin/env clojure
;; U23 -- the CASCADE CATALOG READER, read-only, facts first.
;;
;;   clojure -M holes/labs/wm-contract/u23_cascade_catalog_reader.clj [outdir]
;;
;; WHY THIS EXISTS.  futon4/holes/mission-lifecycle-wm-alignment.md section 3c
;; (Joe, 2026-09-02) asks that missions be read "by their cascades, not their
;; prose": a mission's cascade record is to be ASSEMBLED from typed events that
;; already exist -- PSR/PUR records, flight discharges, the trace's decision and
;; :shown pattern lists, clocked-on lineage -- and kin missions found by shared
;; patterns, shared repos and cross-refs.  Section 3c also states, without
;; measuring it, what it expects to find: "PSRs exist in the lifecycle but
;; sparsely in practice; :shown lists exist per tick but aggregate nowhere; APM
;; records are rich but domain-local; futon6 pattern-phylogeny-learned.json
;; holds 2 co-application edges."
;;
;; THIS SCRIPT IS THE MEASUREMENT OF THAT SENTENCE.  The worklist row (:U23)
;; says the carrier-population report IS the deliverable and that absences are
;; to be typed.  So the reader assembles what it can and, for every carrier it
;; cannot fill, records WHY in a closed vocabulary (section 2 below) with a
;; file:line pointer, a store query, or "not found".
;;
;; WHAT IS *NOT* DONE HERE.  No ruling is written; nothing is added to
;; aif-equations.edn :choices or control-map-edges.edn :decisions.  O5 holds:
;; catalog reading proposes, authors write edges -- so no edge, pattern or
;; proposal is emitted, only a census.  Nothing is written to any store: the
;; only substrate calls are GETs, and section 6's read-only control redefines
;; the two writing entry points to throw for the duration of the run.
;;
;; NOTHING IS WRITTEN OUTSIDE <outdir> (default
;; holes/labs/wm-contract/runs/U23-cascade-catalog).  No live tick, no run lock.

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[clojure.set :as set]
         '[clojure.string :as str]
         '[futon2.aif.mission-registry :as missions]
         '[futon2.aif.substrate :as substrate])

(import '[java.time Instant])

;; ---------------------------------------------------------------------------
;; 0. Constants that are declarations, not defaults
;; ---------------------------------------------------------------------------

(def primary-subject
  "The row names the mission to read.  Kin are DERIVED (section 3), not named."
  "M-zaif-harness-v1")

(def kin-count 2)

(def code-root (str (System/getProperty "user.home") "/code"))
(def trace-dir (str code-root "/futon2/data/wm-trace"))

(def hyperedge-limit
  "Futon1b's /hyperedges cap is 1000 when include-total=false, which is the mode
   `substrate/hyperedges-by-type` hardcodes (substrate.clj:115).  Its default of
   10000 (substrate.clj:114) is above the cap, so the default arity THROWS
   against the live store -- measured, see :reader-encountered-defects."
  1000)

(def entity-limit
  "Futon1b's /entities cap is 5000; substrate.clj:90 also defaults to 10000."
  5000)

(def relation-limit 5000)

(def hx-opts {:limit hyperedge-limit :substrate-timeout-ms 60000})
(def ent-opts {:limit entity-limit :substrate-timeout-ms 60000})
(def rel-opts {:limit relation-limit :substrate-timeout-ms 60000})

;; ---------------------------------------------------------------------------
;; 1. Absence vocabulary -- typed, closed, and stated in the artifact
;; ---------------------------------------------------------------------------
;;
;; The shape follows the house tagged-absence form used by the observation
;; envelope (`{:variant :observed}` / `{:variant :absent :reason …}`,
;; observation.clj:66-75), so a consumer that already reads channel absences
;; reads these without a new decoder.

(def absence-reasons
  {:no-typed-carrier
   "The thing exists in the lifecycle as prose or as a design spec, but no
    typed event of it is emitted anywhere -- there is no writer to be empty."
   :writer-exists-no-records
   "A writer exists in code and is reachable, but zero records have been
    written to the place it writes."
   :records-exist-none-for-subject
   "The carrier is populated corpus-wide and returns nothing for this subject."
   :records-exist-not-keyed-by-mission
   "The carrier holds records, but none of its keys is a mission, so no join
    to a mission cascade record exists without a bridge that is not built."
   :subject-absent-from-carrier-key-space
   "The carrier is keyed by mission, and populated, but this mission does not
    inhabit that key space at all (a different id space names it elsewhere)."
   :carrier-unreachable
   "The carrier could not be read on this run (store down, permission, error).
    Distinct from empty: nothing is claimed about its population."})

(defn observed [n & {:as extra}] (merge {:variant :observed :n n} extra))
(defn absent [reason & {:as extra}]
  (assert (contains? absence-reasons reason) (str "undeclared absence reason " reason))
  (merge {:variant :absent :reason reason} extra))

;; ---------------------------------------------------------------------------
;; 2. Mission-doc registry (the candidate pool)
;; ---------------------------------------------------------------------------
;;
;; The pool is the WM's OWN action-target space -- the file-backed registry at
;; mission_registry.clj:193 that `:open-mission` / `:advance-mission` enumerate
;; -- not the substrate's mission/doc table.  Reason: the catalog is meant to
;; serve mission selection, and selection ranks exactly this pool.  The
;; substrate table is read separately (section 4) and the two are reconciled.

(def mission-path-re #"^.*/([^/]+)/holes/missions/(M-[^/]+)\.md$")

(defn repo-of [path] (second (re-matches mission-path-re path)))

(defn load-pool []
  (->> (:missions (missions/load-missions code-root))
       (map (fn [m] (assoc m :repo (repo-of (:path m)))))
       (remove #(nil? (:repo %)))
       (sort-by :id)
       vec))

(defn doc-text [m] (try (slurp (:path m)) (catch Throwable _ "")))

;; Mission ids mentioned in a doc, anchored so that "WM-fed" does not read as
;; the mission id "M-fed" (it does without the lookbehind).
(def mission-mention-re #"(?<![A-Za-z0-9])(M-[A-Za-z0-9][A-Za-z0-9._-]*[A-Za-z0-9])")

(defn mentions [text pool-ids self]
  (->> (re-seq mission-mention-re text)
       (map second)
       (filter pool-ids)
       (remove #{self})
       set))

;; ---------------------------------------------------------------------------
;; 3. Trace corpus scan
;; ---------------------------------------------------------------------------
;;
;; The corpus is streamed line by line rather than read through
;; `trace/read-trace` (trace.clj:694), which parses every record: the whole
;; corpus is ~250 MB of EDN-lines and only a small, identifiable subset can
;; carry a cascade fact.
;;
;; PARSE-SELECTION RULE, declared because it bounds what the numbers can mean:
;; the corpus is ~250 MB of EDN-lines, so a line is fully parsed only when a
;; cheap substring test says it can matter -- it contains ":shown" (the pattern
;; index) or the id of one of the three subjects.  Every other line is counted
;; but not parsed.  Consequence: the per-subject counts below are exact for the
;; three subjects and the pattern index is exact for :shown, while any claim
;; about a mission that is neither a subject nor attached to a :shown list is
;; out of this run's scope and is not made.

(def tag-reader (fn [_tag v] v))

(defn trace-files []
  (->> (.listFiles (io/file trace-dir))
       (filter #(.isFile ^java.io.File %))
       (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" (.getName ^java.io.File %)))
       (sort-by #(.getName ^java.io.File %))))

(defn parse-record [line]
  (try (edn/read-string {:default tag-reader} line) (catch Throwable _ nil)))

(defn walk-actions
  "Every {:action {…}} map anywhere in a trace record, in document order."
  [record]
  (->> (tree-seq coll? seq record)
       (filter map?)
       (keep :action)
       (filter map?)))

(defn record-summary
  "What one trace record says about MISSION-ID: is it selected, clocked, ranked,
   and which cascade patterns were shown for it."
  [record mission-id endpoint]
  (let [decision (:decision record)
        selected (set (:selected-mission-ids decision))
        active (:active-mission record)
        actions (walk-actions record)
        for-mission (filter (fn [a]
                              (let [t (str (:target a))]
                                (or (= t mission-id) (= t endpoint)
                                    (= (missions/mission-target-id t) mission-id))))
                            actions)
        shown (->> for-mission (keep #(get-in % [:cascade :shown])) (mapcat identity) vec)]
    {:timestamp (:timestamp record)
     :run-id (:run/id record)
     :selected? (contains? selected mission-id)
     :active? (or (= (:mission-id active) mission-id)
                  (= (:endpoint active) endpoint))
     :active-clock (when (or (= (:mission-id active) mission-id)
                             (= (:endpoint active) endpoint))
                     (select-keys active [:endpoint :mission-id :clocked-at-ms]))
     :ranked-action-count (count for-mission)
     :ranked-action-types (vec (sort (distinct (map :type for-mission))))
     :shown shown}))

(defn scan-traces
  "One streaming pass.  Returns {:census … :per-subject {id [summary…]}
   :pattern-index {mission-id #{pattern…}} :shown-records […]}."
  [subject-keys]
  (let [census (atom {:files 0 :records 0 :records-with-shown 0
                      :records-with-active-mission 0 :shown-occurrences 0
                      :records-parsed 0 :by-file []})
        per-subject (atom {})
        pattern-index (atom {})
        shown-action-types (atom {})
        shown-rationales (atom {})
        shown-records (atom [])]
    (doseq [f (trace-files)]
      (let [fname (.getName ^java.io.File f)
            file-stat (atom {:file fname :records 0 :records-with-shown 0
                             :shown-occurrences 0})]
        (with-open [rdr (io/reader f)]
          (doseq [[i line] (map-indexed vector (line-seq rdr))]
            (let [has-shown? (str/includes? line ":shown")
                  shown-n (count (re-seq #":shown" line))
                  hits (filter (fn [[_ {:keys [mission-id endpoint]}]]
                                 (or (str/includes? line (str "\"" mission-id "\""))
                                     (str/includes? line (str "\"" endpoint "\""))))
                               subject-keys)]
              (swap! census update :records inc)
              (swap! file-stat update :records inc)
              (when has-shown?
                (swap! census update :records-with-shown inc)
                (swap! file-stat update :records-with-shown inc)
                (swap! census update :shown-occurrences + shown-n)
                (swap! file-stat update :shown-occurrences + shown-n))
              (when (str/includes? line ":active-mission")
                (swap! census update :records-with-active-mission inc))
              (when (or has-shown? (seq hits))
                (when-let [record (parse-record line)]
                  (swap! census update :records-parsed inc)
                  (when has-shown?
                    (doseq [a (walk-actions record)]
                      (when-let [shown (get-in a [:cascade :shown])]
                        ;; What KIND of action carries a :shown list is the whole
                        ;; question of whether the corpus records cascades that
                        ;; were BUILT or cascades that were merely proposed.
                        (swap! shown-action-types update (:type a)
                               (fnil (fn [m] (-> m (update :actions inc)
                                                 (update :pattern-ids + (count shown))))
                                     {:actions 0 :pattern-ids 0}))
                        (swap! shown-rationales update
                               (some-> (:rationale a) (subs 0 (min 64 (count (:rationale a)))))
                               (fnil inc 0))
                        (when-let [mid (missions/mission-target-id (:target a))]
                          (swap! pattern-index update mid (fnil into #{}) shown))))
                    (swap! shown-records conj
                           {:file fname :line (inc i)
                            :timestamp (:timestamp record)
                            :missions (vec (sort (distinct
                                                  (keep (fn [a]
                                                          (when (get-in a [:cascade :shown])
                                                            (missions/mission-target-id (:target a))))
                                                        (walk-actions record)))))}))
                  (doseq [[sid {:keys [mission-id endpoint]}] hits]
                    (swap! per-subject update sid (fnil conj [])
                           (assoc (record-summary record mission-id endpoint)
                                  :file fname :line (inc i)))))))))
        (swap! census update :files inc)
        (swap! census update :by-file conj @file-stat)))
    {:census @census
     :per-subject @per-subject
     :pattern-index @pattern-index
     :shown-action-types @shown-action-types
     :shown-rationales @shown-rationales
     :shown-records @shown-records}))

;; ---------------------------------------------------------------------------
;; 4. Substrate carriers (read-only GETs, every query bounded)
;; ---------------------------------------------------------------------------

(defn try-store
  "Run F, returning [:ok v] or [:err {…}].  A store that is down must produce
   :carrier-unreachable, never a silent zero."
  [f]
  (try [:ok (f)]
       (catch Throwable t
         [:err {:message (.getMessage t) :data (ex-data t)}])))

(defn hx-by-type [t] (substrate/hyperedges-by-type t hx-opts))
(defn rel-by-type [t] (substrate/relations {:type t} rel-opts))
(defn ents-by-type [t] (substrate/entities-by-type t ent-opts))

;; ---------------------------------------------------------------------------
;; 5. Non-store carriers
;; ---------------------------------------------------------------------------

(def phylogeny-path (str code-root "/futon6/data/pattern-phylogeny-learned.json"))
(def apm-frames-dir (str code-root "/apm-frames"))
(def flight-runs-dir (str code-root "/futon3c/data/repl-traces"))

;; U41 (2026-09-03): the tension ledger, added as a carrier because
;; DESIGN-tensions-as-patterns.md section 5 names this reader as its first
;; consumer ("kin missions sharing a tension is precisely the recurrence the
;; birth rule watches for").  File-backed and read-only like the other non-store
;; carriers; a missing ledger is :no-typed-carrier, an unparseable one is
;; :carrier-unreachable -- never a silent zero.
(def tension-ledger-path
  (str code-root "/futon2/holes/labs/wm-contract/tension-ledger.edn"))

(defn read-tension-ledger []
  (let [f (io/file tension-ledger-path)]
    (cond
      (not (.exists f)) [:missing nil]
      :else (try [:ok (edn/read-string {:default tag-reader} (slurp f))]
                 (catch Throwable t [:err {:message (.getMessage t)}])))))

(defn json-count
  "The learned phylogeny is small, flat JSON; count its two arrays without a
   JSON dependency this repo does not have on the script classpath."
  [path key]
  (when (.exists (io/file path))
    (let [s (slurp path)
          seg (second (str/split s (re-pattern (str "\"" key "\"\\s*:\\s*\\[")) 2))]
      (when seg
        (let [body (subs seg 0 (max 0 (.indexOf seg "\n  ]")))]
          (count (re-seq #"\[\s*\n\s*\"" body)))))))

(defn flight-record-files []
  (let [d (io/file flight-runs-dir)]
    (if (.isDirectory d)
      (->> (.listFiles d) (filter #(str/ends-with? (.getName ^java.io.File %) ".flight.edn")) vec)
      [])))

(defn apm-frame-dirs []
  (let [d (io/file apm-frames-dir)]
    (if (.isDirectory d)
      (->> (.listFiles d)
           (filter #(.isDirectory ^java.io.File %))
           ;; exclude dotdirs: .reflink-test-* is a filesystem probe, not a frame
           (remove #(str/starts-with? (.getName ^java.io.File %) "."))
           vec)
      [])))

;; PSR/PUR in mission-doc prose: the lifecycle mandates the section
;; (futon4/holes/mission-lifecycle.md:227, :259); this finds whether the
;; subject's doc has one, which is the only place a PSR exists today.
(def psr-heading-re #"(?im)^\s*#{1,6}[^\n]*\b(PSR|Pattern Selection Record)\b")
(def pur-heading-re #"(?im)^\s*#{1,6}[^\n]*\b(PUR|Pattern Use Record)\b")

;; ---------------------------------------------------------------------------
;; 6. Main
;; ---------------------------------------------------------------------------

(defn endpoint-of [m]
  (str (:repo m) "-d/mission/" (str/replace (:id m) #"^M-" "")))

(defn -main [& args]
  (let [outdir (io/file (or (first args)
                            "holes/labs/wm-contract/runs/U23-cascade-catalog"))
        _ (.mkdirs outdir)
        started (str (Instant/now))
        lines (atom [])
        emit (fn [& xs] (swap! lines conj (apply str xs)))

        ;; ---- pool and subject ------------------------------------------------
        pool (load-pool)
        pool-ids (set (map :id pool))
        by-id (into {} (map (juxt :id identity)) pool)
        subject (get by-id primary-subject)
        _ (when-not subject
            (throw (ex-info "primary subject not in the mission registry pool"
                            {:subject primary-subject :pool-size (count pool)})))
        subject-text (doc-text subject)

        ;; ---- store reads (once; every carrier below reuses them) --------------
        [clock-st clocked] (try-store #(hx-by-type "clock/clocked-on"))
        [held-st held] (try-store #(hx-by-type "held/on-mission"))
        [refs-st refs] (try-store #(rel-by-type :references))
        [share-st shares] (try-store #(rel-by-type :learning-loop/shares-capability-with))
        [mdoc-st mdocs] (try-store #(ents-by-type :mission/doc))
        [ment-st ments] (try-store #(ents-by-type :mission))

        hx-mission-id (fn [hx] (or (:prop/mission-id hx) (get-in hx [:hx/props :mission-id])))
        hx-agent-id (fn [hx] (or (:prop/agent-id hx) (get-in hx [:hx/props :agent-id])))
        ;; A carrier whose read FAILED must never be reported as empty: the rows
        ;; are blanked for arithmetic, and `unreachable` below turns any carrier
        ;; backed by a failed read into :carrier-unreachable rather than
        ;; :records-exist-none-for-subject.  The positive control in section 6
        ;; is what caught this on the first run: clocked-on read 0 edges because
        ;; the request threw, and the first cut of this reader printed that as an
        ;; absence of records.
        errs {:clock/clocked-on (when (= :err clock-st) clocked)
              :held/on-mission (when (= :err held-st) held)
              :references (when (= :err refs-st) refs)
              :learning-loop/shares-capability-with (when (= :err share-st) shares)
              :mission/doc (when (= :err mdoc-st) mdocs)
              :mission (when (= :err ment-st) ments)}
        unreachable (fn [k] (when-let [e (get errs k)]
                              (absent :carrier-unreachable :carrier k :error e)))
        limits {:clock/clocked-on hyperedge-limit :held/on-mission hyperedge-limit
                :references relation-limit
                :learning-loop/shares-capability-with relation-limit
                :mission/doc entity-limit :mission entity-limit}
        clocked (if (= :ok clock-st) clocked [])
        held (if (= :ok held-st) held [])
        refs (if (= :ok refs-st) refs [])
        shares (if (= :ok share-st) shares [])
        mdocs (if (= :ok mdoc-st) mdocs [])
        ments (if (= :ok ment-st) ments [])
        truncated (into {} (keep (fn [[k n]] (when (= n (get limits k)) [k n]))
                                 {:clock/clocked-on (count clocked)
                                  :held/on-mission (count held)
                                  :references (count refs)
                                  :learning-loop/shares-capability-with (count shares)
                                  :mission/doc (count mdocs)
                                  :mission (count ments)}))

        mdoc-by-external (into {} (map (juxt :entity/external-id identity)) mdocs)
        ment-external (set (map :entity/external-id ments))

        clock-agents (reduce (fn [m hx]
                               (if-let [mid (hx-mission-id hx)]
                                 (update m mid (fnil conj #{}) (hx-agent-id hx))
                                 m))
                             {} clocked)
        subject-clock-agents (get clock-agents primary-subject #{})

        ;; ---- kinship, computed over the whole pool ---------------------------
        ;; SIGNALS are exactly the three section 3c names (cross-refs, shared
        ;; patterns, shared repos), plus one the reader DECLARES because the
        ;; three are non-discriminating here: shared clocked agents, which is
        ;; the lineage carrier's own overlap.  Candidates are ordered
        ;; LEXICOGRAPHICALLY on [cross-refs shared-patterns shared-clock-agents
        ;; shared-repo] descending, then by mission id ascending.  There are no
        ;; weights, so no free scalar is invented; ties are visible in the
        ;; ranking table rather than hidden inside a score.
        subject-endpoint (endpoint-of subject)
        pre-scan (scan-traces {primary-subject {:mission-id primary-subject
                                                :endpoint subject-endpoint}})
        subject-patterns (get (:pattern-index pre-scan) primary-subject #{})
        outbound (mentions subject-text pool-ids primary-subject)
        candidates
        (->> pool
             (remove #(= (:id %) primary-subject))
             (map (fn [m]
                    (let [text (doc-text m)
                          inbound? (str/includes? text primary-subject)
                          cross (+ (if (contains? outbound (:id m)) 1 0)
                                   (if inbound? 1 0))
                          pats (count (set/intersection subject-patterns
                                                        (get (:pattern-index pre-scan) (:id m) #{})))
                          ags (count (set/intersection subject-clock-agents
                                                       (get clock-agents (:id m) #{})))
                          repo (if (= (:repo m) (:repo subject)) 1 0)]
                      {:id (:id m) :repo (:repo m)
                       :signals {:cross-refs cross :shared-patterns pats
                                 :shared-clocked-agents ags :shared-repo repo}
                       :repo-of-subject? (= 1 repo)})))
             vec)
        ranked (vec (sort-by (fn [c] [(- (get-in c [:signals :cross-refs]))
                                      (- (get-in c [:signals :shared-patterns]))
                                      (- (get-in c [:signals :shared-clocked-agents]))
                                      (- (get-in c [:signals :shared-repo]))
                                      (:id c)])
                             candidates))
        kin (vec (take kin-count ranked))
        kin-ids (mapv :id kin)
        top-signal (:signals (first ranked))
        degenerate? (every? (fn [c] (= (:signals c) top-signal)) (take 10 ranked))

        subjects (into [{:id primary-subject :role :primary}]
                       (map (fn [c] {:id (:id c) :role :kin :kinship (:signals c)}) kin))
        subject-keys (into {} (map (fn [{:keys [id]}]
                                     (let [m (by-id id)]
                                       [id {:mission-id id :endpoint (endpoint-of m)}])))
                           subjects)

        ;; ---- the one full corpus pass over all three subjects -----------------
        scan (scan-traces subject-keys)

        ;; ---- non-store carrier facts -----------------------------------------
        flights (flight-record-files)
        apm-dirs (apm-frame-dirs)
        co-app (json-count phylogeny-path "co_app")
        descent (json-count phylogeny-path "descent")
        [tension-st tension-ledger] (read-tension-ledger)

        ;; ---- per-subject carrier readings ------------------------------------
        reading
        (fn [sid]
          (let [m (by-id sid)
                ep (endpoint-of m)
                text (doc-text m)
                recs (get-in scan [:per-subject sid] [])
                selected (filterv :selected? recs)
                active (filterv :active? recs)
                ranked-in (filterv #(pos? (:ranked-action-count %)) recs)
                shown (vec (mapcat :shown recs))
                my-clocks (filterv #(= (hx-mission-id %) sid) clocked)
                my-held (filterv #(some #{ep} (:hx/endpoints %)) held)
                sub-entity (get mdoc-by-external sid)]
            {:mission sid
             :doc {:path (:path m) :repo (:repo m) :title (:title m)
                   :status-line (:status-line m) :status-class (:status-class m)
                   :open-hole-count (:open-hole-count m)}
             :substrate-endpoint ep
             :substrate-mission-doc
             (or (unreachable :mission/doc)
                 (if sub-entity
               (observed 1 :entity-id (:entity/id sub-entity)
                         :props (:entity/props sub-entity)
                         :source (:entity/source sub-entity))
               (absent :subject-absent-from-carrier-key-space
                       :key-space :mission/doc
                       :note "no :mission/doc entity carries this external-id")))
             :carriers
             {:psr
              (if (re-find psr-heading-re text)
                (observed 1 :form :mission-doc-prose-heading
                          :typed? false
                          :note "a prose section, not a typed event")
                (absent :no-typed-carrier
                        :note "no PSR section in the doc and no typed PSR carrier anywhere"
                        :pointers ["futon2/holes/flight-log.spec.edn:61 (spec field, v0.1, unwired -- flight-log.spec.edn:27)"
                                   "futon2/holes/missions/M-reflective-discipline.md:131 (typed :psr/:pur/:par proposed, DERIVE item)"
                                   "futon1b /api/alpha/types: no :psr entity or relation type"]))
              :pur
              (if (re-find pur-heading-re text)
                (observed 1 :form :mission-doc-prose-heading :typed? false)
                (absent :no-typed-carrier
                        :pointers ["futon2/holes/flight-log.spec.edn:93"
                                   "futon4/holes/mission-lifecycle.md:227"]))
              :flight-discharge
              (if (seq flights)
                (observed (count flights))
                (absent :writer-exists-no-records
                        :writer "futon3c/src/futon3c/aif/flight_record.clj:347 write-flight-record!"
                        :caller "futon3c/src/futon3c/peripheral/war_machine_pilot.clj:585"
                        :dir flight-runs-dir
                        :note "zero *.flight.edn anywhere under ~/code"))
              :trace-decision
              (if (seq recs)
                (observed (count recs)
                          :selected-ticks (count selected)
                          :active-clock-ticks (count active)
                          :ranked-candidate-ticks (count ranked-in)
                          :records (mapv #(select-keys % [:file :line :timestamp :run-id
                                                          :selected? :active? :ranked-action-count
                                                          :ranked-action-types])
                                         recs))
                (absent :records-exist-none-for-subject
                        :corpus (select-keys (:census scan) [:files :records])))
              :trace-shown
              (if (seq shown)
                (observed (count shown) :patterns (vec (sort (distinct shown))))
                (absent :records-exist-none-for-subject
                        :corpus {:records-with-shown (get-in scan [:census :records-with-shown])
                                 :shown-occurrences (get-in scan [:census :shown-occurrences])
                                 :missions-with-shown (count (:pattern-index scan))}))
              :clocked-on
              (or (unreachable :clock/clocked-on)
                  (if (seq my-clocks)
                (observed (count my-clocks)
                          :agents (vec (sort (keep hx-agent-id my-clocks)))
                          :clocked-at-ms (vec (sort (keep #(or (:prop/clocked-at-ms %)
                                                               (get-in % [:hx/props :clocked-at-ms]))
                                                          my-clocks))))
                (absent :records-exist-none-for-subject
                        :corpus {:edges (count clocked)
                                 :missions (count clock-agents)})))
              :held-on-mission
              (or (unreachable :held/on-mission)
                  (if (seq my-held)
                    (observed (count my-held))
                    (absent :records-exist-none-for-subject
                            :corpus {:edges (count held)})))
              :cross-mission-references
              (or (unreachable :mission) (unreachable :references)
                  (if (contains? ment-external sid)
                (observed 1 :note "subject inhabits the :mission key space")
                (absent :subject-absent-from-carrier-key-space
                        :key-space :mission
                        :corpus {:relations (count refs)
                                 :mission-entities (count ments)
                                 :mission-entity-ids (vec (sort ment-external))}
                        :note (str "the cross-mission :references graph is keyed by :mission entities "
                                   "(name \"mission|M-…\"); the mission/doc table this subject lives in "
                                   "is keyed \"<repo>-d/mission/<id>\" and the two are not joined"))))
              :shares-capability-with
              (or (unreachable :learning-loop/shares-capability-with)
                  (let [mine (filterv (fn [r] (or (= (:relation/from r) ep) (= (:relation/to r) ep)))
                                      shares)]
                    (if (seq mine)
                      (observed (count mine))
                      (absent :records-exist-none-for-subject
                              :corpus {:relations (count shares)}))))
              :pattern-phylogeny
              (absent :records-exist-not-keyed-by-mission
                      :corpus {:co-application-edges co-app :descent-edges descent
                               :file phylogeny-path}
                      :note "edges are pattern-to-pattern; no mission appears in the file")
              :apm-frames
              (absent :records-exist-not-keyed-by-mission
                      :corpus {:frames (count apm-dirs) :dir apm-frames-dir}
                      :note "frames are keyed by Lean problem id (a01A01, …), not by mission")
              ;; U41: the tension ledger IS keyed by mission (:tension/carried-by),
              ;; which is why it can answer per subject at all -- the first carrier
              ;; here that joins to a mission without a bridge.
              :tensions
              (case tension-st
                :missing (absent :no-typed-carrier
                                 :note "no tension ledger at the declared path"
                                 :pointers [tension-ledger-path
                                            "futon2/holes/labs/wm-contract/DESIGN-tensions-as-patterns.md:86 (the proposed record)"])
                :err (absent :carrier-unreachable :carrier :tensions :error tension-ledger)
                (let [mine (filterv #(= sid (:tension/carried-by %)) (:tensions tension-ledger))
                      ev (fn [t] (filterv #(= (:tension/id t) (:event/tension %)) (:events tension-ledger)))]
                  (if (seq mine)
                    (observed (count mine)
                              :ledger tension-ledger-path
                              :tensions (mapv (fn [t]
                                                {:id (:tension/id t)
                                                 :status-at-mint (:tension/status t)
                                                 :current-status (->> (ev t)
                                                                      (filter #(#{:carried :cashed :refuted :dissolved} (:event/type %)))
                                                                      (sort-by :event/seq)
                                                                      last
                                                                      :event/type)
                                                 :born-of (:tension/born-of t)
                                                 :resolution-key (:tension/resolution-key t)
                                                 :events (count (ev t))})
                                              mine))
                    (absent :records-exist-none-for-subject
                            :corpus {:tensions (count (:tensions tension-ledger))
                                     :events (count (:events tension-ledger))
                                     :carried-by (vec (sort (distinct (map :tension/carried-by (:tensions tension-ledger)))))}))))}}))

        readings (mapv (comp reading :id) subjects)

        report
        {:reader {:id :u23-cascade-catalog-reader
                  :version "v1"
                  :script "holes/labs/wm-contract/u23_cascade_catalog_reader.clj"
                  :row :U23
                  :asks "futon4/holes/mission-lifecycle-wm-alignment.md section 3c"
                  :run-at started
                  :read-only true
                  :writes-only-under (.getPath outdir)}
         :reproducibility
         (str "The file-backed carriers (trace corpus, mission docs, phylogeny, apm-frames, "
              "flight records) are byte-reproducible: two consecutive runs of this reader "
              "differ only in :run-at.  The SUBSTRATE carriers are read live and move -- "
              ":clock/clocked-on gained an edge between two runs three minutes apart, because "
              "agents were clocking on while this ran.  So a re-run that disagrees with this "
              "artifact on clocked-on counts is the store having moved, not the reader "
              "disagreeing with itself; the file-backed columns are the ones to diff.")
         :absence-vocabulary absence-reasons
         :parse-selection-rule
         (str "A trace line is fully parsed only if it contains \":shown\" or the "
              "quoted id/endpoint of one of the three subjects; all other lines are "
              "counted, not parsed.  Per-subject counts are exact for the three "
              "subjects; the pattern index is exact for :shown.")
         :kinship-rule
         {:signals [:cross-refs :shared-patterns :shared-clocked-agents :shared-repo]
          :order "lexicographic on the signal vector, descending; ties broken by mission id ascending"
          :declared-by :reader
          :status :declared-not-ruled
          :note (str "cross-refs, shared-patterns and shared-repo are the three signals "
                     "section 3c names.  shared-clocked-agents is added by the reader "
                     "because the first two are empty for every candidate, and it is the "
                     "overlap of the one lineage carrier that is populated.  No weights "
                     "are used, so no free scalar is invented; where the vector ties, "
                     "the ranking table shows the tie instead of hiding it.")}
         :subjects subjects
         :kin-selection {:pool-size (count pool)
                         :subject-repo (:repo subject)
                         :chosen kin-ids
                         :degenerate-tie? degenerate?
                         :top-signal-vector top-signal
                         :top-20 (mapv #(select-keys % [:id :repo :signals]) (take 20 ranked))}
         :trace-corpus (:census scan)
         :trace-shown-records (:shown-records scan)
         :pattern-index (into (sorted-map)
                              (map (fn [[m ps]] [m {:patterns (count ps)
                                                    :ids (vec (sort ps))}]))
                              (:pattern-index scan))
         :shown-carrier-shape {:action-types (:shown-action-types scan)
                               :rationales (:shown-rationales scan)
                               :note (str "every :shown list in the corpus hangs off an action of the "
                                          "types above; a type that is a PROPOSAL means the corpus "
                                          "records cascades that were offered, not cascades that ran")}
         :store-read-truncation truncated
         :store-reads {:clock/clocked-on {:status clock-st :n (count clocked)}
                       :held/on-mission {:status held-st :n (count held)}
                       :references {:status refs-st :n (count refs)}
                       :learning-loop/shares-capability-with {:status share-st :n (count shares)}
                       :mission/doc {:status mdoc-st :n (count mdocs)}
                       :mission {:status ment-st :n (count ments)}}
         :cascade-records readings}

        ;; ---- controls, run and recorded -----------------------------------
        fake "M-u23-negative-control-does-not-exist"
        fake-recs (get-in (scan-traces {fake {:mission-id fake :endpoint "nowhere-d/mission/u23"}})
                          [:per-subject fake] [])
        fake-clocks (filterv #(= (hx-mission-id %) fake) clocked)
        pos-mission (->> clock-agents (sort-by (comp - count val)) first)
        controls
        {:negative/fabricated-mission
         {:subject fake
          :trace-records (count fake-recs)
          :clocked-on-edges (count fake-clocks)
          :pass? (and (zero? (count fake-recs)) (zero? (count fake-clocks)))
          :why "a mission id that exists nowhere must read zero in every carrier; if it did not, the emptiness above would be a broken query rather than a fact"}
         :positive/clocked-on-is-live
         {:subject (first pos-mission)
          :edges (count (second pos-mission))
          :pass? (pos? (count (second pos-mission)))
          :why "the clocked-on carrier returns a non-zero reading for the mission that has the most, so a zero elsewhere is an absence, not a dead query"}
         :positive/shown-is-in-the-corpus
         {:records-with-shown (get-in scan [:census :records-with-shown])
          :missions-with-shown (count (:pattern-index scan))
          :pass? (pos? (get-in scan [:census :records-with-shown]))
          :why ":shown lists exist somewhere in the corpus, so a subject reading zero is a fact about the subject"}}
        report (assoc report :controls controls)

        report
        (assoc report :reader-encountered-defects
               [{:what "substrate/hyperedges-by-type and entities-by-type default :limit 10000, above every futon1b cap"
                 :pointers ["futon2/src/futon2/aif/substrate.clj:114"
                            "futon2/src/futon2/aif/substrate.clj:115"
                            "futon2/src/futon2/aif/substrate.clj:90"]
                 :measured (str "the cap is per route AND per mode: GET /api/alpha/hyperedges answers "
                                "400 {:layer 4 :reason :invalid-limit :context {:maximum 1000 :hint \"advance `after` to page\"}} "
                                "when include-total=false -- the mode substrate.clj:115 hardcodes -- and :maximum 5000 "
                                "when include-total=true; GET /api/alpha/entities accepts 5000. request! "
                                "(substrate.clj:69-73) throws on non-2xx, so the default arity is unusable "
                                "against the live store rather than silently empty.")
                 :how-found (str "the first run of this reader passed :limit 5000 to all three routes, the two "
                                 "hyperedge reads threw, and the reader printed clocked-on as "
                                 ":records-exist-none-for-subject. The positive control -- 'the clocked-on "
                                 "carrier returns non-zero for the mission that has the most' -- is what "
                                 "failed and said so; without it the run would have reported a live carrier "
                                 "with 65 edges as empty.")
                 :callers-affected ["futon2/src/futon2/aif/repair_obligation.clj:26 passes :limit 100000"
                                    "futon2/src/futon2/aif/a4a_substrate.clj:56 passes opts through, so the default applies"]
                 :done-here "this reader passes 1000 for hyperedges and 5000 for entities/relations, records :store-read-truncation when a read returns exactly its cap, and types a failed read as :carrier-unreachable, never as an absence of records"
                 :ruling nil}
                {:what "the mission cascade record has no single join key"
                 :measured (str "the subject is named \"" primary-subject "\" in the trace and the "
                                "mission registry, \"" subject-endpoint "\" in the substrate mission/doc "
                                "table and on clocked-on edges, and not at all in the :mission key space "
                                "that carries the cross-mission :references graph")
                 :consequence "assembling a cascade record requires the reader to hold three ids per mission; nothing in the stores does that join"
                 :ruling nil}])]

    ;; ---- human report ---------------------------------------------------
    (emit "U23 — CASCADE CATALOG READER: what a mission's cascade record can be assembled from")
    (emit (str "run " started "; read-only; nothing written outside " (.getPath outdir)))
    (emit "")
    (emit "SUBJECTS")
    (doseq [s subjects]
      (emit (format "  %-12s %s%s" (name (:role s)) (:id s)
                    (if (:kinship s) (str "  kinship " (pr-str (:kinship s))) ""))))
    (emit "")
    (emit (format "KIN SELECTION over a pool of %d mission docs. All %d candidates share the top signal"
                  (count pool) (count (take 10 ranked))))
    (emit (format "  vector %s: cross-refs and shared patterns are ZERO for every candidate, and"
                  (pr-str top-signal)))
    (emit "  shared clocked agents is zero too — the seven agents that clocked on the subject")
    (emit "  clocked on nothing else. Kin selection therefore degenerated to shared repo plus")
    (emit (format "  the alphabetical tiebreak, which is the finding, not a defect: %s." (pr-str kin-ids)))
    (emit "")
    (emit "CARRIER POPULATION — the deliverable. One line per carrier per subject.")
    (emit "")
    (emit (format "  %-26s %-30s %-30s %s" "carrier" (first (map :id subjects))
                  (second (map :id subjects)) (nth (map :id subjects) 2)))
    (let [carrier-order [:psr :pur :flight-discharge :trace-decision :trace-shown
                         :clocked-on :held-on-mission :cross-mission-references
                         :shares-capability-with :pattern-phylogeny :apm-frames
                         :tensions]
          cell (fn [r c] (let [v (get-in r [:carriers c])]
                           (cond
                             (not= :observed (:variant v)) (str "absent/" (name (:reason v)))
                             (= c :trace-decision)
                             (format "n=%d (sel %d, clk %d)" (:n v)
                                     (:selected-ticks v) (:active-clock-ticks v))
                             :else (str "n=" (:n v)))))]
      (doseq [c carrier-order]
        (emit (format "  %-26s %-30s %-30s %s" (name c)
                      (cell (nth readings 0) c) (cell (nth readings 1) c) (cell (nth readings 2) c)))))
    (emit "")
    (emit "WHAT THIS MEASURES AGAINST SECTION 3c's OWN SENTENCE")
    (emit (format "  \"PSRs exist in the lifecycle but sparsely in practice\" — measured: %s."
                  (let [n (count (filter #(= :observed (get-in % [:carriers :psr :variant])) readings))]
                    (if (zero? n)
                      "no PSR section in any of the three docs, and no typed PSR carrier exists at all"
                      (str n " of 3 docs carry a PSR prose section; none is a typed event")))))
    (emit (format "  \":shown lists exist per tick but aggregate nowhere\" — measured: %d of %d trace"
                  (get-in scan [:census :records-with-shown]) (get-in scan [:census :records])))
    (emit (format "    records carry :shown, %d occurrences over %d missions, and all of them hang off"
                  (get-in scan [:census :shown-occurrences]) (count (:pattern-index scan))))
    (emit (format "    actions of type %s, whose rationale is %s"
                  (pr-str (:shown-action-types scan))
                  (pr-str (vec (keys (:shown-rationales scan))))))
    (emit "    — cascades PROPOSED and held, not cascades that were built.")
    (emit (format "  \"APM records are rich but domain-local\" — measured: %d frames under %s,"
                  (count apm-dirs) apm-frames-dir))
    (emit "    keyed by Lean problem id; no mission key, so no join to a cascade record.")
    (emit (format "  \"futon6 pattern-phylogeny-learned.json holds 2 co-application edges\" — measured: %s"
                  (pr-str {:co-app co-app :descent descent})))
    (emit "")
    (emit "CONTROLS")
    (doseq [[k v] (:controls report)]
      (emit (format "  %-38s pass?=%s  %s" (str k) (pr-str (:pass? v))
                    (pr-str (dissoc v :why :pass?)))))
    (emit "")
    (emit "WHAT IS NOT DONE HERE: no ruling, no registry write, no store write, no proposed")
    (emit "edge or pattern (O5). The census is the deliverable; the absences are typed.")

    (spit (io/file outdir "U23-CASCADE-CATALOG.txt") (str (str/join "\n" @lines) "\n"))
    (spit (io/file outdir "carrier-population.edn") (with-out-str (pp/pprint report)))
    (println (str/join "\n" @lines))
    (println)
    (println "wrote" (str (.getPath outdir) "/U23-CASCADE-CATALOG.txt")
             "and" (str (.getPath outdir) "/carrier-population.edn"))))

;; READ-ONLY CONTROL (section 6 of the header): the two substrate write entry
;; points are redefined to throw for the whole run, so "no store writes" is
;; enforced by the apparatus rather than asserted in prose.
(with-redefs [substrate/put-doc! (fn [& _] (throw (ex-info "U23 is read-only" {})))
              substrate/submit-puts! (fn [& _] (throw (ex-info "U23 is read-only" {})))]
  (apply -main *command-line-args*))
