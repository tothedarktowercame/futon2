#!/usr/bin/env bb
;; prefetch-mark2.bb — operator helper for scan-mark2.
;;
;; Pulls the authoritative state.json from linode-chicago into the
;; local cache at ~/code/storage/mark2/state.json. scan-mark2 in
;; war_machine.clj reads from that cache; this script refreshes it.
;;
;; Run manually before a war-machine scan, or wire into a cron / daily
;; routine. Rsync is cheap (small file) and idempotent.
;;
;; Optionally also re-extracts each tarball's manifest.json into a
;; sibling cache `~/code/storage/mark2/manifests/<batch>.json`, so
;; scan-mark2 doesn't have to peek into tarballs on every run.

(require '[babashka.fs :as fs]
         '[babashka.process :refer [shell]]
         '[clojure.string :as str])

(def home (System/getProperty "user.home"))
(def storage-root (str home "/code/storage/mark2"))
(def state-cache  (str storage-root "/state.json"))
(def manifest-cache-dir (str storage-root "/manifests"))
(def outbox-dir   (str storage-root "/outbox"))

;; ---------------------------------------------------------------------------
;; Pull state.json
;; ---------------------------------------------------------------------------

(defn pull-state! []
  (println "[prefetch-mark2] scp linode-chicago:~/mark2/state.json →" state-cache)
  (fs/create-dirs storage-root)
  (let [{:keys [exit err]} (shell {:out :string :err :string :continue true}
                                  "scp" "-q" "linode-chicago:~/mark2/state.json"
                                  state-cache)]
    (if (zero? exit)
      (println "[prefetch-mark2] state.json cached ok")
      (do (println "[prefetch-mark2] scp failed:" err)
          (System/exit exit)))))

;; ---------------------------------------------------------------------------
;; Extract bundle manifests
;; ---------------------------------------------------------------------------

(defn extract-manifest! [tarball]
  (let [batch-id (-> (fs/file-name tarball)
                     (str/replace #"^results-" "")
                     (str/replace #"\.tar\.gz$" ""))
        out-path (str manifest-cache-dir "/" batch-id ".json")]
    (fs/create-dirs manifest-cache-dir)
    (let [{:keys [exit out err]} (shell {:out :string :err :string :continue true}
                                        "tar" "-xzOf" tarball "output/manifest.json")]
      (if (zero? exit)
        (do (spit out-path out)
            (println "[prefetch-mark2] extracted manifest for batch" batch-id))
        (println "[prefetch-mark2] tar peek failed for" tarball ":" err)))))

(defn extract-all-manifests! []
  (let [tarballs (->> (fs/list-dir outbox-dir)
                      (map str)
                      (filter #(str/ends-with? % ".tar.gz"))
                      sort)]
    (println "[prefetch-mark2] extracting manifests from" (count tarballs) "bundles")
    (doseq [t tarballs]
      (extract-manifest! t))))

;; ---------------------------------------------------------------------------
;; Entry
;; ---------------------------------------------------------------------------

(defn -main [& args]
  (let [opts (set args)]
    (when-not (contains? opts "--manifests-only")
      (pull-state!))
    (when-not (contains? opts "--state-only")
      (extract-all-manifests!))
    (println "[prefetch-mark2] done.")))

(-main *command-line-args*)
