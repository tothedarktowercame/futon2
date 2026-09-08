#!/usr/bin/env bb
(require '[babashka.fs :as fs]
         '[babashka.process :as p]
         '[clojure.pprint :as pp]
         '[clojure.string :as str])

(def repo "/home/joe/code/futon2")
(def scan-root (or (System/getenv "F10RG_SCAN_ROOT") repo))
(def data-root (or (System/getenv "F10RG_DATA_ROOT") (str repo "/data/wm-full-loop")))
(def artifact (or (System/getenv "F10RG_ARTIFACT")
                  (str repo "/holes/labs/wm-contract/runs/F10-outcome-domain/04-rider-gap.edn")))
(def expected-attempt "attempt-061")
(def needles ["ruled-outcome-c" "ruled_outcome_c"])

(def scan-roots ["src" "scripts" "test" "checks" "holes/labs/wm-contract"])

(defn relative [f] (str (fs/relativize scan-root f)))
(defn classification [path]
  (cond
    (str/starts-with? path "src/") :production
    (str/starts-with? path "scripts/") :production
    (str/starts-with? path "test/") :test
    (str/starts-with? path "checks/") :check
    (str/includes? path "/runs/") :artifact
    (str/starts-with? path "holes/labs/") :lab
    :else :outside-scan-roots))
(defn declaration-self? [path]
  (= path "src/futon2/aif/ruled_outcome_c.clj"))
(defn git-work-tree? [root]
  (let [r (p/shell {:dir root :out :string :err :string :continue true}
                   "git" "rev-parse" "--is-inside-work-tree")]
    (and (zero? (:exit r)) (= "true" (str/trim (:out r))))))
;; Path source. In a git work tree the paths come from `git grep -l` over the
;; WHOLE repo and over TRACKED files only: whole-repo so "no production caller"
;; is not scoped by the five-directory allowlist (a caller under tools/ or web/
;; would land as :outside-scan-roots and turn :mentions-within-scan-roots red),
;; tracked-only so an untracked log that happens to quote the name -- the build
;; loop's own runs/build-loop.log did -- cannot change the artifact. Outside a
;; work tree (the copied scan root the production-caller control uses) it walks
;; the five roots instead.
(defn candidate-files []
  (if (git-work-tree? scan-root)
    (let [r (p/shell {:dir scan-root :out :string :err :string :continue true}
                     "git" "grep" "-l" "-E" "ruled[-_]outcome[-_]c")]
      (->> (if (zero? (:exit r)) (str/split-lines (:out r)) [])
           (remove str/blank?)
           (map #(java.io.File. scan-root %))))
    (->> scan-roots
         (map #(java.io.File. scan-root %))
         (filter fs/exists?)
         (mapcat file-seq)
         (filter fs/regular-file?))))
(defn scan-files []
  (->> (candidate-files)
       (remove #(or (= (str (fs/absolutize %)) (str (fs/absolutize artifact)))
                    (= "04-rider-gap.edn" (fs/file-name %))))))
(defn hits []
  (->> (scan-files)
       (keep (fn [f]
               (let [s (slurp f) path (relative f)
                     found (filterv #(str/includes? s %) needles)]
                 (when (seq found)
                   {:path path :classification (classification path)
                    :declaration-self? (declaration-self? path)
                    :needles found}))))
       (sort-by :path) vec))
(defn attempts []
  (->> (file-seq (java.io.File. data-root))
       (filter fs/directory?)
       (filter #(re-matches #"attempt-[0-9]+" (fs/file-name %)))
       (map (fn [f] {:id (fs/file-name f)
                     :path (str (fs/relativize data-root f))
                     :mtime-ms (.lastModified (java.io.File. (str f)))}))
       (sort-by (juxt :mtime-ms :id)) vec))
;; Mentions of the attempt id in committed source, EXCLUDING this lab's own
;; F10 record. The exclusion is what makes the artifact reproducible: C580, this
;; scanner and the artifact itself all name attempt-061, so an unfiltered grep
;; changes the moment the slice is committed. It also sharpens the claim the
;; mentions are cited for -- the attempt is named OUTSIDE the sheet that says so.
(def mention-self-prefix "holes/labs/wm-contract/")
(defn committed-mentions [identity]
  (if (= scan-root repo)
    (let [r (p/shell {:dir repo :out :string :err :string :continue true}
                     "git" "grep" "-n" identity)]
      (if (zero? (:exit r))
        (->> (str/split-lines (:out r))
             (remove #(str/starts-with? % mention-self-prefix))
             sort vec)
        []))
    []))
(defn facts []
  (let [hs (hits) ats (attempts) newest (last ats)
        callers (remove :declaration-self? hs)]
    (sorted-map
     :scan-command "git grep -l -E 'ruled[-_]outcome[-_]c' (whole repo, tracked files)"
     :path-source (if (git-work-tree? scan-root) :git-tracked-repo-wide :filesystem-walk-of-scan-roots)
     :outside-scan-root-hits (vec (filter #(= :outside-scan-roots (:classification %)) callers))
     :hits hs
     :caller-classification-counts (frequencies (map :classification callers))
     :production-callers (vec (filter #(= :production (:classification %)) callers))
     :production-caller-count (count (filter #(= :production (:classification %)) callers))
     :newest-cohort-attempt newest
     :newest-attempt-committed-mentions (committed-mentions (:id newest))
     :newest-attempt-mentioned-in-committed-source? (boolean (seq (committed-mentions (:id newest))))
     ;; The declaration MUST appear in its own scan. Without this, a git grep
     ;; that errored (or a mistyped scan root) yields no hits at all, and
     ;; :no-production-caller then passes green over a scan that read nothing.
     :declaration-found? (boolean (some :declaration-self? hs)))))
(defn checks [f]
  (sorted-map
   :declaration-found (:declaration-found? f)
   :no-production-caller (zero? (:production-caller-count f))
   :mentions-within-scan-roots (empty? (:outside-scan-root-hits f))
   :newest-cohort-attempt (= expected-attempt (get-in f [:newest-cohort-attempt :id]))))
(defn failed [f] (vec (sort (keys (remove val (checks f))))))
(defn verdict [f] (every? true? (vals (checks f))))
(let [f (facts) report (sorted-map :check :F10-rider-gap :facts f
                                    :per-fact (checks f) :verdict (verdict f))]
  (if (:verdict report)
    (do (fs/create-dirs (fs/parent artifact))
        (spit artifact (with-out-str (pp/pprint report)))
        (let [r (p/shell {:out :string} "sha256sum" artifact)]
          (println "F10 RIDER GAP PASS" (first (str/split (:out r) #"\s+")))))
    (do (binding [*out* *err*]
          (pp/pprint report)
          (println "FAILED-CHECKS:" (str/join " " (failed f))))
        (System/exit 1))))
