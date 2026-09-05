#!/usr/bin/env bb

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.set :as set]
         '[clojure.string :as str])

(def schema :wm/symbol-concordance-v1)
(def relations [:relation/case-fold :relation/runtime-key])
(def pointer-re #"(?<![A-Za-z0-9_.-])([A-Za-z0-9_./-]+\.(?:bb|clj|edn|lean|md|sh|tex)):(\d+)(?:-(\d+))?")

(defn fail! [code & xs]
  (println (str code) (str/join " " (map pr-str xs)))
  false)

(defn values-deep [x]
  (tree-seq coll? seq x))

(defn source-files [roots]
  (->> roots
       (mapcat file-seq)
       (filter #(.isFile ^java.io.File %))
       (remove #(let [p (.getPath ^java.io.File %)]
                  (or (str/includes? p "/.git/")
                      (str/includes? p "/target/"))))
       (map #(.getCanonicalFile ^java.io.File %))
       distinct
       vec))

(defn suffix-matches [files rel]
  (let [suffix (str/replace (or rel "") #"^/+" "")]
    (filterv #(let [p (str/replace (.getPath ^java.io.File %) "\\" "/")]
                (or (= p suffix) (str/ends-with? p (str "/" suffix))))
             files)))

(defn line-count [file]
  (with-open [r (io/reader file)] (count (line-seq r))))

(defn check-pointer! [files {:keys [file start end label]}]
  (let [matches (suffix-matches files file)]
    (cond
      (not (string? file))
      (fail! :error/unresolved-pointer label file start end)

      (empty? matches)
      (fail! :error/unresolved-pointer label file start end)

      (> (count matches) 1)
      (fail! :error/ambiguous-pointer label file (mapv #(.getPath ^java.io.File %) matches))

      (or (not (pos-int? start)) (not (pos-int? end)) (> start end)
          (> end (line-count (first matches))))
      (fail! :error/unresolved-pointer label file start end)

      :else true)))

(defn at-range [at]
  (cond
    (pos-int? at) [at at]
    (string? at) (when-let [[_ a b] (re-matches #"(\d+)-(\d+)" at)]
                   [(parse-long a) (parse-long b)])
    :else nil))

(defn column-pointer [row column]
  (let [v (get row column)]
    (cond
      (and (map? v) (keyword? (:absent v))) nil
      (and (map? v) (string? (:file v)) (at-range (:at v)))
      (let [[a b] (at-range (:at v))]
        {:file (:file v) :start a :end b :label [(:id row) column]})
      :else ::untyped)))

(defn string-pointers [registry]
  (for [s (values-deep registry)
        :when (string? s)
        [_ file a b] (re-seq pointer-re s)]
    {:file file :start (parse-long a) :end (parse-long (or b a)) :label :in-string}))

(defn runtime-key [row]
  (when-let [v (get-in row [:runtime :var])]
    (re-find #"(?<![A-Za-z0-9_./-]):[A-Za-z0-9_./-]+" v)))

(defn collision-groups [rows key-fn]
  (->> rows
       (group-by key-fn)
       (remove (comp nil? key))
       (filter (fn [[_ rs]] (> (count rs) 1)))
       (into {} (map (fn [[k rs]] [k (set (map :id rs))])))))

(defn check-registry! [registry roots]
  (println "relations" (pr-str relations))
  (println "LIMIT :reading-collisions-not-detected; gamma/Pi is conceptual, not a spelling collision")
  (let [rows (:symbols registry)
        declarations (:collisions registry)
        ids (group-by :id rows)
        case-groups (collision-groups rows #(some-> (:bare %) str/lower-case))
        runtime-groups (collision-groups rows runtime-key)
        files (source-files roots)
        columns (for [row rows column [:glossary :lean :runtime]]
                  (column-pointer row column))
        column-pointers (vec (filter :file columns))
        column-locations (set (map #(select-keys % [:file :start :end]) column-pointers))
        added-string-pointers (->> (string-pointers registry)
                                   (remove #(contains? column-locations
                                                       (select-keys % [:file :start :end])))
                                   (group-by #(select-keys % [:file :start :end]))
                                   vals (map first))
        distinct-pointers (concat column-pointers added-string-pointers)
        checks (atom [])]
    (when-not (= schema (:schema registry))
      (swap! checks conj (fail! :error/not-a-symbol-concordance (:schema registry))))
    (when-not (seq rows)
      (swap! checks conj (fail! :error/vacuous :symbols)))
    (doseq [[id rs] ids :when (> (count rs) 1)]
      (swap! checks conj (fail! :error/duplicate-row-id id (count rs))))
    (doseq [row rows
            :let [expected (some-> (:bare row) str/lower-case)]
            :when (not= expected (:fold row))]
      (swap! checks conj (fail! :error/fold-field-drift (:id row) :stored (:fold row) :computed expected)))
    (doseq [[fold members] case-groups
            :let [decls (filter #(= fold (:fold %)) declarations)]
            :when (empty? decls)]
      (swap! checks conj (fail! :error/undeclared-collision :relation/case-fold fold (sort members))))
    (doseq [decl declarations
            member (:members decl)
            :let [n (count (get ids member))]
            :when (not= 1 n)]
      (swap! checks conj (fail! :error/unresolved-collision-member (:fold decl) member :resolution-count n)))
    (doseq [decl declarations
            :let [actual (get case-groups (:fold decl))
                  declared (set (:members decl))]
            :when (and actual (not= actual declared))]
      (swap! checks conj (fail! :error/collision-member-mismatch (:fold decl)
                                :missing (sort (set/difference actual declared))
                                :extra (sort (set/difference declared actual)))))
    (doseq [decl declarations :when (not (seq (:basis decl)))]
      (swap! checks conj (fail! :error/collision-without-basis (:fold decl) (:members decl))))
    (doseq [[key members] runtime-groups
            :let [covered? (some #(and (= :runtime-key (:kind %))
                                       (= members (set (:members %)))) declarations)]
            :when (not covered?)]
      (swap! checks conj (fail! :error/undeclared-runtime-key-collision
                                :relation/runtime-key key (sort members))))
    (doseq [[row column result] (for [row rows column [:glossary :lean :runtime]]
                                 [row column (column-pointer row column)])
            :when (= ::untyped result)]
      (swap! checks conj (fail! :error/untyped-absence (:id row) column (get row column))))
    (doseq [pointer distinct-pointers]
      (when-not (check-pointer! files pointer) (swap! checks conj false)))
    (if (some false? @checks)
        false
        (do (println "ACCEPT" (count rows) "rows," (count case-groups)
                     "case-fold groups," (count distinct-pointers) "distinct pointers resolved")
            true))))

(let [script (.getCanonicalFile (io/file *file*))
      lab (.getParentFile script)
      futon2 (.getCanonicalFile (io/file lab "../../.."))
      code-root (.getParentFile futon2)
      roots [futon2 (io/file code-root "p4ng") (io/file code-root "mathlib4")]
      path (or (first *command-line-args*) (.getPath (io/file lab "symbol-concordance.edn")))]
  (try
    (let [registry (edn/read-string (slurp path))]
      (when-not (check-registry! registry roots) (System/exit 1)))
    (catch Exception e
      (fail! :error/not-a-symbol-concordance path (.getName (class e)) (.getMessage e))
      (System/exit 1))))
