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
;; `--summary` prints the one COUNTS line the narrative under
;; runs/U37-enumeration-completeness/ must carry, and writes nothing.
;; `U37_OUT_DIR=<dir>` sends the catalogue somewhere else, which is how the
;; controls keep planted catalogues out of the committed runs directory.
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
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

(apply -main *command-line-args*)
