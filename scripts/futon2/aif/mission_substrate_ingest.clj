(ns futon2.aif.mission-substrate-ingest
  "Half 1 of the 2026-09-17 ruling: substrate-2 holds ALL the missions.

  Repeatable, idempotent corpus backfill (a script, not a production
  namespace). The record writer itself lives in
  futon2.aif.mission-registry (mission-record-props /
  upsert-mission-record!) — the same single source the futon3c watcher's
  scope lane calls per mission-doc land, so the lane and this backfill can
  never write divergent mission records.

  Contract:
  - Provenance on every entity: :provenance/repo, :provenance/path,
    :provenance/sha256 of the file bytes.
  - Status carried un-reclassified (raw status-line + registry class).
  - Idempotent: re-running changes nothing when nothing changed.
  - No deletes: an ingester-authored entity whose file has vanished is
    REPORTED under :vanished-files, never removed.
  - Store discipline: bounded queries (limit 1000), public entity route.

  Run (established sibling-script invocation; scripts are not require-able
  in this repo): clojure -M scripts/futon2/aif/mission_substrate_ingest.clj --run
  Optional: --code-root <dir> (default ~/code)."
  (:require [clojure.java.io :as io]
            [futon2.aif.mission-registry :as registry]))

(def ingester-source "mission-doc-ingest")

(defn -main
  [& args]
  (let [code-root (or (some-> (next (drop-while #(not= "--code-root" %) args)) first)
                      (str (System/getProperty "user.home") "/code"))
        _ (assert (.isDirectory (io/file code-root)) (str "code root not found: " code-root))
        entries (:missions (registry/load-missions-from-files code-root))
        _ (assert (seq entries) "file scan found no mission docs — refusing to ingest nothing")
        existing (registry/mission-entity-index)
        before-count (count existing)
        results (mapv (fn [entry]
                        (-> (registry/upsert-mission-record!
                             {:code-root code-root
                              :path (:path entry)
                              :existing (get existing (:id entry))})
                            :status))
                      entries)
        scanned-ids (set (map :id entries))
        vanished (->> existing
                      (filter (fn [[id e]]
                                (and (= ingester-source (:entity/source e))
                                     (not (contains? scanned-ids id)))))
                      (map key)
                      sort)
        after-count (count (registry/mission-entity-index))]
    (println
     (pr-str
      {:code-root code-root
       :scanned (count entries)
       :created (count (filter #{:created} results))
       :updated (count (filter #{:updated} results))
       :unchanged (count (filter #{:unchanged} results))
       :vanished-files vanished
       :entity-count-before before-count
       :entity-count-after after-count}))))

(when *command-line-args*
  ;; `clojure -M scripts/futon2/aif/mission_substrate_ingest.clj --run [--code-root DIR]`
  ;; (--run exists because *command-line-args* is nil for a bare invocation)
  (apply -main *command-line-args*))
