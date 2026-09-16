(ns receipt-control
  (:require [babashka.process :as process]
            [checks.positive-proof-receipt :as receipt]
            [cheshire.core :as json]
            [clojure.data :as data]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]))

(def root "holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-receipts-2")
(def lab "holes/labs/wm-contract/")
(defn edn! [path x] (spit path (with-out-str (pp/pprint x))))
(defn json! [path x] (spit path (json/generate-string x {:pretty true})))
(defn read-edn [path] (edn/read-string (slurp path)))
(defn revision-source [revision path]
  (let [r (process/shell {:out :string :err :string :continue true}
                         "git" "-C" "/home/joe/code/mathlib4" "show" (str revision ":" path))]
    (assert (zero? (:exit r))) (:out r)))

(defn prepare! []
  (let [manifest (json/parse-string (slurp (str root "/manifest.json")) true)
        population
        (mapv
         (fn [filename]
           (let [old (read-edn (str lab filename))
                 dir (str root "/" filename)
                 _ (.mkdirs (io/file dir))
                 _ (io/copy (io/file (str lab filename)) (io/file dir "predecessor.edn"))
                 live (receipt/live-source-basis old)
                 changes
                 (vec (for [[a b] (map vector (:source-basis old) live)
                            [x y] (map vector (:declarations a) (:declarations b))
                            :when (not= x y)
                            :let [before (receipt/declaration-text (revision-source "480a666ad2^" (:path a)) (:name x))
                                  repaired (receipt/declaration-text (revision-source "480a666ad2" (:path a)) (:name x))
                                  head (receipt/declaration-text (revision-source "HEAD" (:path a)) (:name x))]]
                        (do
                          (assert (= (:sha256 x) (receipt/sha256-text before)))
                          (assert (= (:sha256 y) (receipt/sha256-text repaired) (receipt/sha256-text head)))
                          {:repo (:repo a) :path (:path a) :name (:name x)
                           :old (:sha256 x) :new (:sha256 y)
                           :pre-repair-text before :repaired-text repaired :head-text head})))
                 files (conj (:source-basis old) (:fixture old))]
             (json! (str dir "/attribution.json") changes)
             (json! (str dir "/before-hashes.json")
                    (into {} (map (fn [s] [(receipt/source-path s) (receipt/sha256-file (receipt/source-path s))]) files)))
             {:file filename :receipt old :changes changes
              :old-sha256 (receipt/sha256-file (str dir "/predecessor.edn"))}))
         (:receipts manifest))]
    (assert (= 26 (reduce + (map #(count (:changes %)) population))))
    (json! (str root "/population.json") population)
    (println "Attribution: all 26 changed slices match pre-repair pins and repaired/HEAD sources.")))

(defn reattest! [filename additions]
  (let [dir (str root "/" filename)
        old (read-edn (str dir "/predecessor.edn"))
        commands (atom [])
        shell process/shell
        logged-shell (fn [& args]
                       (let [r (apply shell args)]
                         (swap! commands conj {:arguments args :exit (:exit r) :out (:out r) :err (:err r)})
                         (json! (str dir "/protocol-commands.json") @commands)
                         r))]
    (with-redefs [process/shell logged-shell]
      (let [old-check (receipt/validate old)
            refreshed (receipt/basis-record old)
            expanded (update refreshed :source-basis
                             (fn [basis]
                               (mapv (fn [entry]
                                       (if (= "DarkTower/WarMachine/Holes.lean" (:path entry))
                                         (update entry :declarations
                                                 (fn [ds] (into ds (map (fn [n] {:name n})
                                                                       (remove (set (map :name ds)) additions)))))
                                         entry)) basis)))
            successor (assoc (receipt/basis-record expanded) :recorded-at (str (java.time.Instant/now)))
            validation (receipt/validate successor)
            old-pins (into {} (for [s (:source-basis old) d (:declarations s)] [[(:path s) (:name d)] (:sha256 d)]))
            pins (vec (for [[i s] (map-indexed vector (:source-basis successor))
                           [j d] (map-indexed vector (:declarations s))
                           :let [prior (get old-pins [(:path s) (:name d)])]]
                       {:path (:path s) :name (:name d) :old prior :new (:sha256 d)
                        :changed? (not= prior (:sha256 d)) :indices [i j]}))]
        (edn! (str dir "/old-validation.edn") old-check)
        (assert (= [:positive-source-drift] (:failures old-check)))
        (assert (= (:adapter old) (:adapter successor)))
        (assert (= (:dependency-closure old) (:dependency-closure successor)))
        (assert (= (:fixture old) (:fixture successor)))
        (edn! (str dir "/fresh-predecessor-derivation.edn") refreshed)
        (edn! (str dir "/successor.edn") successor)
        (edn! (str dir "/derivation-diff.edn") (data/diff old successor))
        (edn! (str dir "/validation.edn") validation)
        (assert (:pass? validation))
        (let [temp (.toFile (java.nio.file.Files/createTempDirectory "receipt-mutations-" (make-array java.nio.file.attribute.FileAttribute 0)))
              temp-root (.getAbsolutePath temp)
              controls
              (try
                (doseq [spec (conj (:source-basis successor) (:fixture successor))]
                  (let [dest (io/file temp-root (:repo spec) (:path spec))]
                    (io/make-parents dest)
                    (io/copy (io/file (receipt/source-path spec)) dest)))
                (with-redefs [receipt/repo-root temp-root]
                  (let [copy (io/file temp "receipt.edn")]
                    (spit copy (pr-str successor))
                    (assert (:pass? (receipt/validate (read-edn copy))))
                    (mapv (fn [{:keys [indices] :as pin}]
                            (let [[i j] indices
                                  mutant (assoc-in successor [:source-basis i :declarations j :sha256] (apply str (repeat 64 "0")))
                                  _ (spit copy (pr-str mutant))
                                  result (receipt/validate (read-edn copy))]
                              (assert (= [:positive-source-drift] (:failures result)))
                              (assoc pin :result result)))
                          (filter :changed? pins))))
                (finally (doseq [f (reverse (file-seq temp))] (io/delete-file f))))]
          (json! (str dir "/result.json")
                 {:file filename :old-sha256 (receipt/sha256-file (str dir "/predecessor.edn"))
                  :new-sha256 (receipt/sha256-file (str dir "/successor.edn"))
                  :old-recorded-at (:recorded-at old) :new-recorded-at (:recorded-at successor)
                  :pins pins :controls controls :axioms (:result successor)
                  :validation validation :review "pending claude-3 independent review"})
          (io/copy (io/file (str dir "/successor.edn")) (io/file (str lab filename)))
        (println filename "fresh derivation and every changed/added pin mutation PASS"))))))

(let [[mode filename additions] *command-line-args*]
  (case mode
    "prepare" (prepare!)
    "run" (reattest! filename (edn/read-string additions))))
