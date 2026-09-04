;; U37 replay: was the selector's candidate enumeration COMPLETE on the
;; recorded ticks?
;;
;;   clojure -M:test holes/labs/wm-contract/u37_enumeration_replay.clj \
;;     [trace-file ...] > runs/U37-enumeration-completeness/<name>.txt
;;
;; With no arguments it replays the three 2026-09-02 records. It writes a typed
;; EDN catalogue beside this file (runs/U37-enumeration-completeness/) and exits
;; non-zero if any enumerated kind is incomplete.
;;
;; What makes this a check rather than a restatement: the available population
;; is recomputed by `futon2.aif.enumeration-completeness`, which shares no code
;; with the proposers that built the recorded candidates. The record supplies
;; the enumerated set; the filesystem supplies the available set; the two are
;; compared member by member, and a member of the available set that the
;; enumerator did not emit has no typed reason and fails the run.
;;
;; PLANTS (the negative controls drive these; see
;; enumeration_completeness_controls.sh):
;;   U37_PLANT_WHITELIST=<n>   keep only the first n mission candidates of each
;;                             record -- the four-mission-whitelist incident
;;                             (holes/NOTE-the-whitelist-provenance.md) replayed
;;                             against today's population.
;;   U37_PLANT_PHANTOM=<id>    add a candidate for a mission id that is not on
;;                             disk.
;;   U37_PLANT_KIND_ENUMERATOR=<kind>  give a kind that has no proposer a
;;                             claimed one, so its whole population turns from
;;                             a typed absence into untyped missing members --
;;                             the control for the kind-level claim itself.
;;   U37_CODE_ROOT=<dir>       scan a different code root -- the control that
;;                             the scan is a measurement and not an echo of the
;;                             record.
;; A plant is stamped into the catalogue; a planted run is never evidence.
;;
;; `--deposit <run-id>` (RE6) replays THE RUN'S OWN recorded ticks -- the trace
;; in its run store, filtered to the records the run's tick receipts name -- and
;; deposits one typed row in the run-era ledger through run_era_ledger.bb's
;; --deposit API. It never writes the ledger EDN itself.
;;
;; `--summary` prints the one COUNTS line the narrative under
;; runs/U37-enumeration-completeness/ must carry, and writes nothing.
;; `U37_OUT_DIR=<dir>` sends the catalogue somewhere else, which is how the
;; controls keep planted catalogues out of the committed runs directory.
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.java.shell :as sh]
         '[clojure.pprint :as pp]
         '[clojure.string :as str]
         '[futon2.aif.enumeration-completeness :as ec])

(def default-traces
  ["data/wm-trace/wm-trace-2026-09-02.edn"])

(def out-dir
  (or (System/getenv "U37_OUT_DIR")
      "holes/labs/wm-contract/runs/U37-enumeration-completeness"))

(defn read-records [path]
  (with-open [r (java.io.PushbackReader. (io/reader path))]
    (loop [out []]
      (let [form (edn/read {:eof ::eof
                            :default (fn [t v] {:trace/edn-tag t :trace/value v})}
                           r)]
        (if (= ::eof form) out (recur (conj out form)))))))

(def code-root
  (or (System/getenv "U37_CODE_ROOT") ec/default-code-root))

(def plants
  (let [whitelist (some-> (System/getenv "U37_PLANT_WHITELIST") str/trim parse-long)
        phantom (System/getenv "U37_PLANT_PHANTOM")
        kind-enum (some-> (System/getenv "U37_PLANT_KIND_ENUMERATOR") str/trim keyword)]
    (cond-> {}
      whitelist (assoc :whitelist whitelist)
      (seq phantom) (assoc :phantom phantom)
      kind-enum (assoc :kind-enumerator kind-enum)
      (not= code-root ec/default-code-root) (assoc :code-root code-root))))

(defn plant-candidates
  "Apply the planted narrowings to a record's candidate list. Identity when no
   plant is set, which is the shape every real run takes."
  [candidates]
  (let [mission? #(contains? #{:advance-mission :open-mission}
                             (get-in % [:action :type]))
        {:keys [whitelist phantom]} plants
        kept (if whitelist
               (let [missions (filter mission? candidates)]
                 (concat (remove mission? candidates) (take whitelist missions)))
               candidates)]
    (vec (cond-> kept
           phantom (concat [{:rank 9999
                             :action {:type :advance-mission :target phantom}}])))))

(defn record-report [record]
  (let [candidates (plant-candidates (get-in record [:decision :controller-ranking]))
        planted-kinds (if-let [k (:kind-enumerator plants)]
                        (assoc-in ec/kinds [k :enumerator]
                                  {:proposer :planted-control
                                   :pointer "U37_PLANT_KIND_ENUMERATOR"})
                        ec/kinds)]
    (assoc (with-redefs [ec/kinds planted-kinds]
             (ec/completeness-record candidates {:code-root code-root}))
           :run/id (:run/id record)
           :record-timestamp (:timestamp record)
           :candidates-in-record (count (get-in record [:decision :controller-ranking]))
           :candidates-compared (count candidates))))

(defn- kind-line [k]
  (format "    %-10s available %4d  enumerated %4d  missing %4d  phantom %4d  -> %s"
          (name (:kind k)) (:available-count k) (:enumerated-count k)
          (count (:missing k)) (count (:phantom k)) (name (:verdict k))))

(defn summary-line
  "The single COUNTS line the narrative must carry, computed from the reports."
  [reports]
  (let [k (fn [r kind] (first (filter #(= kind (:kind %)) (:kinds r))))
        r (first reports)]
    (str "COUNTS: records " (count reports)
         "; candidates/record " (:candidates-in-record r)
         (str/join ""
                   (for [kind [:mission :excursion :ticket]
                         :let [c (k r kind)]]
                     (format "; %s available %d enumerated %d missing %d phantom %d %s"
                             (name kind) (:available-count c) (:enumerated-count c)
                             (count (:missing c)) (count (:phantom c))
                             (name (:verdict c))))))))

(defn -main [& args]
  (let [summary? (some #{"--summary"} args)
        paths (if-let [ps (seq (remove #{"--summary"} args))] (vec ps) default-traces)
        records (vec (mapcat read-records paths))
        reports (mapv record-report records)]
    (when summary?
      (println (summary-line reports))
      (System/exit 0))
    (println (format ";; U37 enumeration-completeness replay -- %d record(s) from %s"
                     (count records) (str/join ", " paths)))
    (when (seq plants)
      (println ";; PLANTED (this run is a control, not evidence):" (pr-str plants)))
    (doseq [r reports]
      (println (format "\n  %s  (%d candidates in record, %d compared)  VERDICT %s"
                       (:run/id r) (:candidates-in-record r) (:candidates-compared r)
                       (name (:verdict r))))
      (doseq [k (:kinds r)]
        (println (kind-line k))
        (when (and (seq (:missing k)) (not= :kind-not-enumerated (:verdict k)))
          (println (format "      MISSING, no typed reason (%d): %s"
                           (count (:missing k))
                           (str/join ", " (take 12 (:missing k))))))
        (when (seq (:phantom k))
          (println (format "      PHANTOM, enumerated but not on disk (%d): %s"
                           (count (:phantom k)) (str/join ", " (:phantom k)))))
        (when (= :kind-not-enumerated (:verdict k))
          (println (format "      typed absence :no-proposer-for-kind -- %d available items no proposer emits (%s)"
                           (:available-count k) (:pointer (:enumerator k))))
          (println (format "      first by id: %s"
                           (str/join ", " (take 8 (:missing k))))))
        (doseq [[reason n] (:exclusions-by-reason k)]
          (println (format "      excluded %5d  %s" n (name reason))))))
    (let [catalogue {:version :u37-enumeration-replay/v1
                     :at (str (java.time.Instant/now))
                     :traces paths
                     :plants plants
                     :reports (mapv #(update % :kinds
                                             (fn [ks] (mapv (fn [k] (dissoc k :missing-detail)) ks)))
                                    reports)}
          fname (str out-dir "/replay-"
                     (if (seq plants) "PLANTED-" "")
                     (subs (str (java.time.Instant/now)) 0 10) ".edn")]
      (io/make-parents fname)
      (spit fname (with-out-str (pp/pprint catalogue)))
      (println "\n;; catalogue:" fname))
    (let [bad (filter #(= :incomplete (:verdict %)) reports)]
      (println (format "\n;; VERDICT: %d/%d records complete"
                       (- (count reports) (count bad)) (count reports)))
      (System/exit (if (seq bad) 1 0)))))

;; ---------------------------------------------------------------------------
;; --deposit <run-id> -- one run-era ledger row (RE6)
;; ---------------------------------------------------------------------------
;;
;; WHAT THIS CHECK CAN AND CANNOT SAY ABOUT A PAST RUN. The enumerated side is
;; contemporaneous: it is read out of the run's OWN recorded ticks, which is why
;; this deposit replays the run store's trace rather than the shared per-date
;; corpus, and filters it to the records the run's tick receipts name. The
;; available side is not: `ec/scan-population` walks the filesystem AS IT
;; STANDS AT DEPOSIT TIME. On a run taken today those two are the same tree and
;; the comparison is a verdict about the run. On an older run they are not, and
;; a member created after the run reads as "the enumerator skipped it" when the
;; enumerator could not have seen it.
;;
;; So an :incomplete replay is NOT deposited as a red until it is dated. Every
;; missing member is resolved to the file the scan found it in and dated by that
;; file's first commit; a member whose file was added after the run's last tick
;; could not have been enumerable at the run, and cannot be evidence about it.
;;
;;   every record complete                        -> :green
;;   some member missing that existed at the run  -> :red (a real narrowing)
;;   every missing member postdates the run       -> :typed-absence: the
;;                                                   population moved, and this
;;                                                   check has no as-of-run scan
;;                                                   to reconstruct the old one
;;
;; A PHANTOM is never date-forgiven: a target the record enumerated and the scan
;; cannot find could be a deletion after the run, but the file is gone and there
;; is no path left to date, so a phantom keeps the verdict red and says so.
;; A planted run refuses to deposit at all -- a control is not evidence.

(def deposit-receipt-dir "holes/labs/wm-contract/runs/RE6-check-deposits")

(def futon2-root
  (str (System/getProperty "user.home") "/code/futon2"))

(defn run-store-dir [run-id]
  (io/file futon2-root "holes/labs/wm-contract/runs" run-id))

(defn run-store-files [run-id]
  (let [d (run-store-dir run-id)]
    (when (.isDirectory d)
      (vec (sort (map #(.getName ^java.io.File %)
                      (filter #(.isFile ^java.io.File %) (.listFiles d))))))))

(defn run-tick-ids
  "The tick run/ids of a run, read off its own store's receipt filenames. These
   are the records this check is entitled to speak about."
  [run-id]
  (->> (or (run-store-files run-id) [])
       (keep #(second (re-matches #"tick-run-record-\d{4}-\d{2}-\d{2}-(.+)\.edn" %)))
       sort vec))

(defn run-trace-files [run-id]
  (->> (or (run-store-files run-id) [])
       (filter #(re-matches #"wm-trace.*\.edn" %))
       (mapv #(str (io/file (run-store-dir run-id) %)))))

(defn- repo-of
  "The git checkout a path lives in, or nil."
  [path]
  (loop [f (.getParentFile (io/file path))]
    (when f
      (if (.exists (io/file f ".git")) f (recur (.getParentFile f))))))

(defn first-commit-instant
  "When this file first entered its repository's history, as an Instant, or nil
   when it is untracked, has no history, or lives in no checkout. nil is not an
   answer: an undatable member keeps the verdict red."
  [path]
  (when-let [root (repo-of path)]
    (let [rel (str/replace-first path (str (.getPath root) "/") "")
          {:keys [exit out]} (sh/sh "git" "log" "--diff-filter=A" "--format=%aI" "--" rel
                                    :dir (.getPath root))]
      (when (zero? exit)
        (when-let [line (last (remove str/blank? (str/split-lines (or out ""))))]
          (try (.toInstant (java.time.OffsetDateTime/parse (str/trim line)))
               (catch Exception _ nil)))))))

(defn scan-index
  "id -> the file the scan counted it in, for one kind."
  [kind]
  (into {} (map (juxt :id :path)) (:available (ec/scan-population kind code-root))))

(defn date-missing-members
  "One dated entry per (kind, missing id) across the run's reports, plus the
   phantoms, which cannot be dated."
  [reports run-end]
  (let [pairs (distinct (for [r reports, k (:kinds r)
                              :when (= :incomplete (:verdict k))
                              id (:missing k)]
                          [(:kind k) id]))
        indexes (into {} (map (fn [kind] [kind (scan-index kind)]))
                      (distinct (map first pairs)))]
    {:missing
     (vec (for [[kind id] (sort pairs)
                :let [path (get-in indexes [kind id])
                      added (some-> path first-commit-instant)]]
            {:kind kind :id id
             :path (when path (str/replace-first path (str (System/getProperty "user.home") "/code/") ""))
             :first-committed (when added (str added))
             :postdates-run? (boolean (and added (pos? (compare added run-end))))}))
     :phantom
     (vec (distinct (for [r reports, k (:kinds r)
                          :when (= :incomplete (:verdict k))
                          id (:phantom k)]
                      {:kind k :id id})))}))

(defn strip-clock
  "The completeness record stamps :at with the wall clock; the receipt must be
   byte-identical on a replay, so it is dropped here rather than recorded."
  [report]
  (-> report
      (dissoc :at)
      (update :kinds (fn [ks] (mapv #(dissoc % :missing-detail) ks)))))

(defn deposit-receipt [run-id]
  (let [tick-ids (run-tick-ids run-id)
        traces (run-trace-files run-id)
        all (vec (mapcat read-records traces))
        mine (filterv #(contains? (set tick-ids) (:run/id %)) all)
        foreign (vec (sort (distinct (remove (set tick-ids) (map :run/id all)))))
        reports (mapv record-report mine)
        run-end (when (seq mine)
                  (apply max-key #(.toEpochMilli ^java.time.Instant %)
                         (map #(java.time.Instant/parse (:timestamp %)) mine)))
        incomplete (filterv #(= :incomplete (:verdict %)) reports)
        dated (when (seq incomplete) (date-missing-members reports run-end))
        all-postdate? (and dated
                           (seq (:missing dated))
                           (empty? (:phantom dated))
                           (every? :postdates-run? (:missing dated)))]
    ;; array-map, not a literal: a map literal of this size is a hash-map and
    ;; would print in hash order, so the receipt would not be stable to read.
    (array-map
     :schema :wm/run-era-deposit-receipt-v1
     :row :RE6
     :check :enumeration-completeness
     :run-id run-id
     :produced-by "holes/labs/wm-contract/u37_enumeration_replay.clj --deposit"
     :deterministic
     (str "No wall-clock field: the completeness record's :at is dropped, so this receipt is "
          "rewritten byte-identically on every deposit over an unchanged tree. That is what "
          "lets the deposit require it to be committed and unmodified, and lets the same "
          "deposit repeat as :already-present.")
     :run-store (array-map
                 :dir (str "holes/labs/wm-contract/runs/" run-id)
                 :holds (run-store-files run-id)
                 :tick-ids tick-ids
                 :traces (mapv #(str/replace-first % (str futon2-root "/") "") traces)
                 :records-in-traces (count all)
                 :records-of-this-run (count mine)
                 :records-of-another-run foreign
                 :run-last-tick (when run-end (str run-end)))
     :verdict-deposited (cond (empty? mine) :typed-absence
                              (empty? incomplete) :green
                              all-postdate? :typed-absence
                              :else :red)
     :replay (array-map
              :records (count reports)
              :complete (count (remove #(= :incomplete (:verdict %)) reports))
              :incomplete (count incomplete)
              :per-record (mapv strip-clock reports))
     :dating dated
     :population-side-is-not-contemporaneous
     (str "the available population is scanned from the filesystem at deposit time "
          "(ec/scan-population over " code-root "), while the enumerated side is read off the "
          "run's own records. Members are therefore dated by first commit before any red is "
          "deposited; a phantom cannot be dated at all, because the file the scan looked for is "
          "not there.")
     :not-what-this-says
     (str "A :typed-absence here does NOT say the enumeration was complete at the run. It says "
          "the only evidence of incompleteness this check can produce today is evidence about a "
          "population the run never saw, so no verdict about the run is reconstructible."))))

(defn deposit-notes [run-id r]
  (let [store (:run-store r)
        rep (:replay r)
        dated (:dating r)]
    (case (:verdict-deposited r)
      :green
      (str "replayed the run's own recorded ticks: " (:records rep) " of " (:records-in-traces store)
           " records in " (str/join ", " (:traces store)) " carry this run's tick ids, and all "
           (:complete rep) " are :complete -- every member of the scanned population was "
           "enumerated and no enumerated target is missing from disk. The available side is "
           "scanned at deposit time; on this run that scan and the run's own tree agree, which "
           "is what a green here means and all it means.")
      :typed-absence
      (if (zero? (:records-of-this-run store))
        (str "runs/" run-id "/ holds no trace record this check can replay: its store holds "
             (str/join ", " (:holds store)) ", the trace files found were "
             (pr-str (:traces store)) " with " (:records-in-traces store) " records, and none "
             "carries one of the run's tick ids (" (str/join ", " (:tick-ids store)) "). Without "
             "the run's own enumerated candidates there is no contemporaneous side to compare.")
        (str "the replay of this run's " (:records rep) " records reports " (:incomplete rep)
             " incomplete, and EVERY member it reports missing came into existence after the "
             "run, so none of them is evidence about it: "
             (str/join "; " (for [m (:missing dated)]
                              (str (name (:kind m)) " " (:id m) " first committed "
                                   (:first-committed m) " in " (:path m))))
             " -- against a run whose last tick is " (:run-last-tick store)
             ". The available population is scanned at deposit time and this check has no "
             "as-of-run scan, so the population the run actually saw cannot be reconstructed "
             "and no verdict about its enumeration can be deposited. The deposit-time replay is "
             "recorded in the artifact this row points at."))
      :red
      (str "the enumeration was INCOMPLETE at this run: " (:incomplete rep) " of " (:records rep)
           " records skipped an available member that already existed when the run was taken"
           (when (seq (:phantom dated))
             (str ", and enumerated " (count (:phantom dated)) " target(s) the scan cannot find on disk"))
           ". Dated members: "
           (str/join "; " (for [m (:missing dated)]
                            (str (name (:kind m)) " " (:id m) " first committed "
                                 (or (:first-committed m) "UNDATABLE") " in " (or (:path m) "no path"))))
           " -- against a run whose last tick is " (:run-last-tick store) "."))))

(defn deposit! [run-id]
  (when (seq plants)
    (println "u37 --deposit refuses to deposit a PLANTED run:" (pr-str plants))
    (System/exit 1))
  (let [r (deposit-receipt run-id)
        rel (str deposit-receipt-dir "/enumeration-completeness-" run-id ".edn")
        path (io/file futon2-root rel)]
    (io/make-parents path)
    (spit path (with-out-str (pp/pprint r)))
    (println "u37_enumeration_replay --deposit: receipt" rel)
    (let [{:keys [exit out err]}
          (sh/sh "bb" "holes/labs/wm-contract/run_era_ledger.bb" "--deposit"
                 "--run-id" run-id
                 "--check-id" ":enumeration-completeness"
                 "--verdict" (str (:verdict-deposited r))
                 "--artifact" rel
                 "--author" "u37_enumeration_replay.clj --deposit"
                 "--deposited-by" "RE6 -- wire the four remaining catalogued checks"
                 "--notes" (deposit-notes run-id r)
                 :dir futon2-root)]
      (print out) (print err) (flush)
      (when-not (zero? exit)
        (println (format "u37_enumeration_replay --deposit: the ledger refused the row (exit %d)" exit))
        (println "  if the refusal is artifact-untracked or artifact-dirty, commit" rel "and re-run")
        (System/exit 1))
      (System/exit 0))))

(if-let [run-id (second (drop-while #(not= "--deposit" %) *command-line-args*))]
  (deposit! run-id)
  (if (some #{"--deposit"} *command-line-args*)
    (do (println "u37_enumeration_replay --deposit needs a run-id") (System/exit 1))
    (apply -main *command-line-args*)))
