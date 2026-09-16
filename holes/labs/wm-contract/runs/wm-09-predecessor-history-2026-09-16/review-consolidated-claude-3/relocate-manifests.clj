;; claude-3: make the /tmp roots copy self-consistent for manifest-bound records.
;; For each close manifest, first check every entry's recorded sha256 against the COPIED
;; bytes (so relocation cannot hide a real mismatch), then rebuild the manifest with the
;; producer's build-manifest over copy paths, keeping :evidence/id and :admitted-at.
(require '[futon2.aif.evidence-manifest :as manifest] '[futon2.aif.interpretation-evidence :as evidence]
         '[clojure.edn :as edn] '[clojure.java.io :as io] '[clojure.string :as str])
(import '[java.nio.file Files])
(def prod "/home/joe/code/futon2/data/")
(def copy "/tmp/c3-wm09c/realcopy/")
(defn bytes-of [p] (Files/readAllBytes (.toPath (io/file p))))
(def closes (for [root (.listFiles (io/file copy)) :when (.isDirectory root)
                  cohort (.listFiles root) :when (.isDirectory cohort)
                  attempt (.listFiles cohort) :when (.isDirectory attempt)
                  :let [f (io/file attempt "007-closed.edn")] :when (.exists f)] f))
(def results
  (doall
   (for [f closes
         :let [closed (edn/read-string {:default tagged-literal} (slurp f))
               m (get-in closed [:payload :close-evidence-manifest])]
         :when m]
     (let [entries (mapv (fn [e] (assoc e :copy-path (str/replace (:source-path e) prod copy))) (:entries m))
           pre (mapv (fn [e] {:id (:evidence/id e) :outside-data? (not (str/starts-with? (:source-path e) prod))
                              :copy-sha-matches? (and (str/starts-with? (:source-path e) prod)
                                                      (= (:sha256 e) (evidence/sha256 (bytes-of (:copy-path e)))))}) entries)]
       (if (every? :copy-sha-matches? pre)
         (let [rebuilt (manifest/build-manifest {:entries (mapv #(-> % (select-keys [:evidence/id :admitted-at]) (assoc :source-path (:copy-path %))) entries)
                                                 :read-bytes bytes-of})]
           (spit f (pr-str (assoc-in closed [:payload :close-evidence-manifest] rebuilt)))
           {:close (str/replace (str f) copy "") :relocated true :entries (count entries)})
         {:close (str/replace (str f) copy "") :relocated false :pre (remove :copy-sha-matches? pre)})))))
(prn :manifested-closes (count results) :relocated (count (filter :relocated results)))
(doseq [r (remove :relocated results)] (prn r))
