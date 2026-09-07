#!/usr/bin/env bb
;; F12 slice 21 (census half): did any OTHER runs/F12-organise/ artifact get
;; reflowed by a later slice the way 11-d2-denominator.edn was at 3c0ad833?
;;
;; C556 section 2 named this question and did not answer it. The answer here is
;; not read off `git log`: for every artifact the bytes at HEAD are compared
;; against the bytes at the commit that introduced it, which is immune to the
;; history-simplification `git log -- <path>` can do.
;;
;; Usage: bb f12_reflow_census.bb            -> writes runs/F12-organise/13-reflow-census.edn
;;        bb f12_reflow_census.bb --stdout   -> prints, writes nothing

(require '[babashka.process :refer [shell]]
         '[clojure.string :as str]
         '[clojure.edn :as edn]
         '[clojure.pprint :as pp])

(def repo "/home/joe/code/futon2")
(def adir "holes/labs/wm-contract/runs/F12-organise")

(defn git [& args]
  (let [{:keys [out exit err]} (apply shell {:dir repo :out :string :err :string
                                             :continue true} "git" args)]
    (when-not (zero? exit)
      (throw (ex-info "git failed" {:args args :err err})))
    out))

(defn git-bytes
  "File content at <rev>:<path>, or nil when the path does not exist there."
  [rev path]
  (let [{:keys [out exit]} (shell {:dir repo :out :string :err :string :continue true}
                                  "git" "show" (str rev ":" path))]
    (when (zero? exit) out)))

(defn lines [s] (if s (count (str/split-lines s)) 0))

(defn preserved?
  "Is every key/value of OLD still present, unchanged, in NEW? Maps may gain
   keys (that is what a slice adding fields does); vectors must match
   elementwise; scalars must be =. This is the predicate that separates a
   value-preserving reflow from a real content change."
  [old new]
  (cond
    (and (map? old) (map? new))
    (every? (fn [[k v]] (and (contains? new k) (preserved? v (get new k)))) old)
    (and (vector? old) (vector? new))
    (and (= (count old) (count new)) (every? true? (map preserved? old new)))
    (and (set? old) (set? new)) (every? #(contains? new %) old)
    (and (seq? old) (seq? new))
    (and (= (count old) (count new)) (every? true? (map preserved? old new)))
    :else (= old new)))

(defn added-entry-count
  "How many map entries NEW has that OLD did not, recursively."
  [old new]
  (cond
    (and (map? old) (map? new))
    (+ (count (remove (set (keys old)) (keys new)))
       (reduce + 0 (for [[k v] old :when (contains? new k)]
                     (added-entry-count v (get new k)))))
    (and (vector? old) (vector? new) (= (count old) (count new)))
    (reduce + 0 (map added-entry-count old new))
    :else 0))

(defn safe-read [s]
  (try (edn/read-string s) (catch Exception e {::unreadable (.getMessage e)})))

(defn classify
  "Compare two versions of one artifact."
  [old-src new-src]
  (cond
    (nil? old-src) {:verdict :absent-at-that-rev}
    (= old-src new-src) {:verdict :byte-identical :lines (lines new-src)}
    :else
    (let [o (safe-read old-src) n (safe-read new-src)]
      (cond
        (or (::unreadable o) (::unreadable n)) {:verdict :unreadable}
        (preserved? o n)
        {:verdict (if (= o n) :reflow-only :reflow-plus-additions)
         :lines-before (lines old-src) :lines-after (lines new-src)
         :entries-added (added-entry-count o n)
         :every-line-moved? (not= (str/split-lines old-src)
                                  (take (lines old-src) (str/split-lines new-src)))}
        :else
        {:verdict :values-changed
         :lines-before (lines old-src) :lines-after (lines new-src)}))))

(defn commits-for [path]
  (->> (git "log" "--format=%H" "--follow" "--" path)
       str/split-lines (remove str/blank?) vec))

;; --- full-history cross-check, immune to `git log -- <path>` simplification ---
(defn full-history-touches
  "path -> set of commit shas that name it in --name-only over ALL of HEAD's
   history. Guards against a per-path log hiding a commit."
  []
  (let [out (git "log" "--format=@@%H" "--name-only")]
    (reduce (fn [acc line]
              (cond
                (str/starts-with? line "@@") (assoc acc ::cur (subs line 2))
                (str/blank? line) acc
                (str/starts-with? line adir) (update acc line (fnil conj #{}) (::cur acc))
                :else acc))
            {} (str/split-lines out))))

(def head (str/trim (git "rev-parse" "HEAD")))
(def tracked (->> (git "ls-files" adir) str/split-lines (remove str/blank?) sort vec))
(def touches (full-history-touches))

(def per-file
  (vec (for [p tracked
             :let [cs (commits-for p)
                   introduced (last cs)
                   head-src (git-bytes head p)]]
         (merge {:path p
                 :commits-newest-first cs
                 :commit-count (count cs)
                 :introduced-at introduced
                 :full-history-commit-set-agrees?
                 (= (set cs) (get touches p #{}))
                 :lines-at-head (lines head-src)}
                {:since-introduction (classify (git-bytes introduced p) head-src)}))))

;; --- controls -------------------------------------------------------------
(defn control-identity
  "C1 identity: comparing a version with itself must say byte-identical."
  []
  (let [p (str adir "/07-o3-field.edn") s (git-bytes head p)]
    {:control :identity :path p :result (:verdict (classify s s))
     :expect :byte-identical}))

(defn control-reflow-plant
  "C2 plant: pprint-reflow an artifact that has NOT changed since introduction.
   The detector must call it a reflow, and the plant must be verified to have
   landed (line count moved) before the verdict is believed."
  []
  (let [p (str adir "/07-o3-field.edn")
        src (git-bytes head p)
        ;; a narrower right margin re-wraps the same values onto different
        ;; lines -- the same shape of change 3c0ad833 made, minus the additions
        reflowed (binding [pp/*print-right-margin* 40]
                   (with-out-str (pp/pprint (edn/read-string src))))
        landed? (and (not= src reflowed) (not= (lines src) (lines reflowed)))
        r (classify src reflowed)]
    {:control :reflow-plant :path p :plant-landed? landed?
     :lines-before (lines src) :lines-after (lines reflowed)
     :result (:verdict r) :expect :reflow-only
     :believable? (and landed? (= :reflow-only (:verdict r)))}))

(defn control-value-plant
  "C3 plant: change one value. The detector must NOT call that a reflow --
   otherwise it would call every difference value-preserving."
  []
  (let [p (str adir "/07-o3-field.edn")
        src (git-bytes head p)
        v (edn/read-string src)
        k (first (keys v))
        planted (with-out-str (pp/pprint (assoc v k ::planted-value)))
        landed? (not= (get v k) ::planted-value)
        r (classify src planted)]
    {:control :value-plant :path p :key-changed k :plant-landed? landed?
     :result (:verdict r) :expect :values-changed
     :believable? (and landed? (= :values-changed (:verdict r)))}))

(defn control-positive
  "C4 positive control: the ONE artifact known to have been reflowed must come
   back as reflowed. A census that cannot see the case it was written for is
   not a census."
  []
  (let [p (str adir "/11-d2-denominator.edn")
        row (first (filter #(= p (:path %)) per-file))]
    {:control :known-reflow-is-detected :path p
     :result (get-in row [:since-introduction :verdict])
     :expect :reflow-plus-additions
     :believable? (= :reflow-plus-additions (get-in row [:since-introduction :verdict]))}))

(def controls [(control-identity) (control-reflow-plant) (control-value-plant) (control-positive)])

(def drifted (filterv #(not= :byte-identical (get-in % [:since-introduction :verdict])) per-file))

(def report
  {:what "F12 slice 21 census: which runs/F12-organise/ artifacts changed after the commit that introduced them"
   :question-from "holes/labs/wm-contract/C556-F12-artifact-pointer-drift.md:126-129"
   :repo "futon2" :head head :dir adir
   :artifacts-tracked (count tracked)
   :per-file per-file
   :artifacts-changed-since-introduction (mapv :path drifted)
   :artifacts-byte-identical-since-introduction
   (mapv :path (remove #(not= :byte-identical (get-in % [:since-introduction :verdict])) per-file))
   :controls controls
   :all-controls-believable? (every? #(get % :believable? true) controls)
   :not-claimed
   ["This census is over runs/F12-organise/ only, which is the population C556 asked about."
    "Byte-identical since introduction means no citation of that file CAN have drifted; it does not mean the citations of it were ever correct."
    "A file's pointers could still be wrong for reasons other than a reflow -- miscopied at writing time -- which this census does not test."]})

(if (some #{"--stdout"} *command-line-args*)
  (pp/pprint report)
  (let [out (str repo "/" adir "/13-reflow-census.edn")]
    (spit out (with-out-str (pp/pprint report)))
    (println "wrote" out)
    (println "changed since introduction:" (mapv :path drifted))
    (println "controls believable:" (:all-controls-believable? report))))
