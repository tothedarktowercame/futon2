(ns futon2.aif.mission-substrate-ingest
  "Half 1 of the 2026-09-17 ruling: substrate-2 holds ALL the missions.

  Repeatable, idempotent ingester (a script, not a production namespace):
  reads every mission doc the registry contract admits
  (`load-missions-from-files`: `<code-root>/<repo>/holes/missions/M-*.md`,
  primary checkouts only, registry dedupe/classification) and upserts one
  substrate-2 mission entity per mission id.

  Contract:
  - Provenance on every entity: :provenance/repo, :provenance/path,
    :provenance/sha256 of the file bytes. An entity that cannot say where it
    came from is not evidence.
  - Status carried, un-reclassified: :mission/status-line is the raw line,
    :mission/status-class is the registry's classification as a string;
    unparseable statuses are stored as \"unknown\" (the registry's honest
    bias), never silently defaulted to a live class.
  - Idempotent: when the desired props equal the entity's current props the
    entity is left untouched; re-running changes nothing when nothing
    changed. Foreign props already on the entity (e.g. from the
    hinge-log-bridge) are preserved, not clobbered.
  - No deletes: a previously-ingested entity whose file has vanished is
    REPORTED under :vanished, never removed.

  Store discipline (futon2/CLAUDE.md): reads are bounded
  (entities-by-type limit 1000), writes go through the public
  POST /api/alpha/entity route, and the store is never restarted.

  Run (established sibling-script invocation; scripts are not require-able
  in this repo): clojure -M scripts/futon2/aif/mission_substrate_ingest.clj --run
  Optional: --code-root <dir> (default ~/code)."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.mission-registry :as registry]
            [futon2.aif.substrate :as substrate])
  (:import (java.security MessageDigest)))

(def ingester-source "mission-doc-ingest")

(defn- sha256-file
  ^String [path]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (with-open [in (io/input-stream path)]
      (let [buf (make-array Byte/TYPE 8192)]
        (loop []
          (let [n (.read in buf)]
            (when (pos? n)
              (.update digest buf 0 n)
              (recur))))))
    (apply str (map #(format "%02x" %) (.digest digest)))))

(defn- repo-of
  "The source repo for an absolute mission-doc path: the path segment
   immediately under the scan root."
  [code-root path]
  (let [rel (str/replace-first (str path) (str code-root "/") "")]
    (first (str/split rel #"/"))))

(defn- desired-props
  "Props the ingester owns. :mission/relation is preserved from the entity
  when already present (hinge-log-bridge entities carry \"relates-to\")."
  [code-root entry]
  (let [sha (sha256-file (:path entry))]
    {:scope/role "mission"
     :mission/relation "relates-to"
     :mission/title (:title entry)
     :mission/status-line (:status-line entry)
     :mission/status-class (name (:status-class entry))
     :mission/open-hole-count (long (:open-hole-count entry))
     :provenance/repo (repo-of code-root (:path entry))
     :provenance/path (:path entry)
     :provenance/sha256 sha}))

(defn- parse-props
  "The entities read route returns :entity/props as an EDN string; the write
  route accepts (and returns) a map. Normalize to a map for comparison."
  [props]
  (cond
    (map? props) props
    (string? props) (try (edn/read-string props) (catch Throwable _ {}))
    :else {}))

(defn- upsert-one!
  [code-root existing-by-id entry]
  (let [id (:id entry)
        existing (get existing-by-id id)
        existing-props (parse-props (:entity/props existing))
        ;; XTDB drops nil-valued keys, so a desired prop of nil can never be
        ;; stored; drop nils from what we write AND what we compare against.
        merged (into {} (remove (comp nil? val))
                     (merge existing-props (desired-props code-root entry)))]
    (cond
      (and existing (= existing-props merged))
      :unchanged

      :else
      (do (substrate/put-doc!
           (cond-> {:entity/name (str "mission|" id)
                    :entity/type :mission
                    :entity/external-id id
                    :entity/props merged}
             ;; keep the entity id (and an existing foreign :entity/source)
             ;; stable so re-runs are true upserts, not duplicate rows
             (:entity/id existing) (assoc :entity/id (:entity/id existing))
             (not (:entity/source existing)) (assoc :entity/source ingester-source)))
          (if existing :updated :created)))))

(defn -main
  [& args]
  (let [code-root (or (some-> (next (drop-while #(not= "--code-root" %) args)) first)
                      (str (System/getProperty "user.home") "/code"))
        _ (assert (.isDirectory (io/file code-root)) (str "code root not found: " code-root))
        entries (:missions (registry/load-missions-from-files code-root))
        _ (assert (seq entries) "file scan found no mission docs — refusing to ingest nothing")
        existing (->> (substrate/entities-by-type "mission" {:limit 1000})
                      (filter :entity/external-id)
                      (map (fn [e] [(:entity/external-id e) e]))
                      (into {}))
        before-count (count existing)
        results (mapv (partial upsert-one! code-root existing) entries)
        scanned-ids (set (map :id entries))
        vanished (->> existing
                      (filter (fn [[id e]]
                                (and (= ingester-source (:entity/source e))
                                     (not (contains? scanned-ids id)))))
                      (map key)
                      sort)
        after-count (count (substrate/entities-by-type "mission" {:limit 1000}))]
    (println
     (pr-str
      {:code-root code-root
       :scanned (count entries)
       :created (count (filter #{:created} results))
       :updated (count (filter #{:updated} results))
       :unchanged (count (filter #{:unchanged} results))
       :vanished-files vanished
       :entity-count-before before-count
       :entity-count-after after-count})))
(when *command-line-args*
  ;; `clojure -M scripts/futon2/aif/mission_substrate_ingest.clj --run [--code-root DIR]`
  ;; (scripts/ is on :paths but sibling scripts are not require-able in this
  ;; repo — the established invocation is direct file execution; --run forces
  ;; the no-argument case to have a command line)
  (apply -main *command-line-args*)))
