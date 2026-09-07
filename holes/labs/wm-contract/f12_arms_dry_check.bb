#!/usr/bin/env bb

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.string :as str])
(import '[java.io PushbackReader]
        '[java.nio.file Files]
        '[java.nio.file.attribute FileAttribute])

(def lab-dir (.getParentFile (.getAbsoluteFile (io/file *file*))))
(def repo-root (.getCanonicalFile (io/file lab-dir "../../..")))
(def default-aif (io/file lab-dir "aif-equations.edn"))
(def default-construct (io/file repo-root "../futon3/checks/construct_cascade.clj"))
(def default-gate (io/file repo-root "../futon3c/scripts/zaif_cascade_gate.clj"))
(def default-library (io/file repo-root "../futon3/library"))
(def default-out (io/file lab-dir "runs/F12-organise/14-arms-dry.edn"))

(defn env-file [key fallback]
  (io/file (or (System/getenv key) (str fallback))))

(defn nonblank-string? [x]
  (and (string? x) (not (str/blank? x))))

(defn deferral-key [arm]
  (when (= :not-here (:run-by arm))
    (let [b (:buys arm)]
      (when (and (keyword? b) (str/starts-with? (name b) "see-"))
        (keyword (subs (name b) 4))))))

(defn arm-measure [arm]
  (let [d (deferral-key arm)]
    (cond-> (sorted-map
             :arm (:arm arm)
             :buys-is-measured? (nonblank-string? (:buys arm))
             :costs-is-measured? (nonblank-string? (:costs arm))
             :run-by-is-measured? (nonblank-string? (:run-by arm)))
      d (assoc :defers-to d))))

(defn census [aif-path]
  (let [registry (edn/read-string (slurp aif-path))
        choices (into (sorted-map)
                      (filter (fn [[_ v]] (= :F12 (:row v))))
                      (:choices registry))]
    (when-not (= 6 (count choices))
      (throw (ex-info (str "expected 6 F12 choices, found " (count choices))
                      {:count (count choices)})))
    (let [measures (into (sorted-map)
                         (map (fn [[k v]]
                                [k (sorted-map
                                    :all-arms-run? (contains? v :all-arms-run)
                                    :arms (mapv arm-measure (:arms v))
                                    :arms-not-all-run? (contains? v :arms-not-all-run)
                                    :status (:status v))]))
                         choices)
          directly-dry? (fn [k]
                          (every? (fn [a]
                                    (and (:run-by-is-measured? a)
                                         (:buys-is-measured? a)
                                         (:costs-is-measured? a)))
                                  (get-in measures [k :arms])))
          arm-dry? (fn [a]
                     (or (and (:run-by-is-measured? a)
                              (:buys-is-measured? a)
                              (:costs-is-measured? a))
                         (and (:defers-to a)
                              (contains? measures (:defers-to a))
                              (directly-dry? (:defers-to a)))))
          not-dry (vec
                   (for [[choice m] measures
                         arm (:arms m)
                         :when (not (arm-dry? arm))]
                     [choice (:arm arm)
                      (cond
                        (and (:defers-to arm) (not (contains? measures (:defers-to arm))))
                        :deferral-not-an-f12-choice
                        (:defers-to arm) :deferred-choice-not-directly-measured
                        (not (:run-by-is-measured? arm)) :run-by-not-measured
                        (not (:buys-is-measured? arm)) :buys-not-measured
                        :else :costs-not-measured)]))
          statuses (vec (for [[k m] measures
                              :when (not= :observed-not-decided (:status m))]
                          [k (:status m)]))]
      (sorted-map
       :arms-dry? (empty? not-dry)
       :arms-not-dry not-dry
       :choices measures
       :choices-not-observed-not-decided statuses))))

(def construct-keys [:added-by-organise :precedence-before :precedence-after])

(defn construct-measure [path]
  (let [lines (vec (str/split-lines (slurp path)))
        rows (into (sorted-map)
                   (for [k construct-keys
                         :let [needle (str "^\\s*" (java.util.regex.Pattern/quote (str k))
                                           "\\s+(#\\{\\}|\\[\\]|\\S+)")
                               hits (keep-indexed
                                     (fn [i line]
                                       (when-let [m (re-find (re-pattern needle) line)]
                                         [(inc i) (str/trim (second m)) (str/trim line)]))
                                     lines)]
                         :when true]
                     (do
                       (when-not (= 1 (count hits))
                         (throw (ex-info (str "expected one " k " entry, found " (count hits)) {})))
                       (let [[line value source] (first hits)]
                         [k (sorted-map
                             :file-line (str (.getCanonicalPath (io/file path)) ":" line)
                             :literal-empty-collection? (contains? #{"#{}" "[]"} value)
                             :source-text source
                             :value-source value)]))))]
    (sorted-map
     :added-by-organise-is-a-literal-empty-set?
     (get-in rows [:added-by-organise :literal-empty-collection?])
     :entries rows
     :precedence-after-is-a-literal-empty-vector?
     (get-in rows [:precedence-after :literal-empty-collection?])
     :precedence-before-is-a-literal-empty-vector?
     (get-in rows [:precedence-before :literal-empty-collection?]))))

(defn read-rule-table [path]
  (with-open [r (PushbackReader. (io/reader path))]
    (loop []
      (let [form (read {:eof ::eof} r)]
        (cond
          (= ::eof form) (throw (ex-info "def rule-table not found" {:path (str path)}))
          (and (seq? form) (= 'def (first form)) (= 'rule-table (second form))) (nth form 2)
          :else (recur))))))

(defn rule-measure [gate-path library-path]
  (let [rules (read-rule-table gate-path)]
    (when-not (= 4 (count rules))
      (throw (ex-info (str "expected 4 rules, found " (count rules)) {:count (count rules)})))
    (let [rows
          (mapv
           (fn [{:keys [id then-source]}]
             (let [[rel span] (str/split then-source #":")
                   [from to] (mapv parse-long (str/split span #"-"))
                   f (io/file library-path rel)
                   lines (when (.exists f) (vec (str/split-lines (slurp f))))
                   then-line (when lines
                               (first (keep-indexed
                                       (fn [i line]
                                         (when (re-find #"^\s*\+ THEN:" line) (inc i)))
                                       lines)))
                   next-block (when (and lines then-line)
                                (or (first (keep-indexed
                                            (fn [i line]
                                              (when (and (> (inc i) then-line)
                                                         (re-find #"^\s*\+ [A-Z-]+:" line))
                                                (inc i)))
                                            lines))
                                    (inc (count lines))))
                   actual (when (and lines from to (<= to (count lines)))
                            (str/join "\n" (subvec lines (dec from) to)))
                   conditions (sorted-map
                               :file-exists? (some? lines)
                               :pattern-id-matches-path?
                               (= id (keyword (str/replace rel #"\.flexiarg$" "")))
                               :span-inside-the-then-block?
                               (boolean (and then-line next-block from to
                                             (> from then-line) (< to next-block)))
                               :span-is-non-empty?
                               (boolean (and actual (seq (str/trim actual)))))]
               (sorted-map
                :actual-span-text actual
                :conditions conditions
                :rule id
                :then-block-lines [then-line next-block]
                :then-source then-source)))
           rules)
          failing (vec (for [r rows
                             :when (not (every? true? (vals (:conditions r))))]
                         (:rule r)))]
      (sorted-map
       :gate-condition-source-lines
       (sorted-map :file-exists? [273 274]
                   :pattern-id-matches-path? [290 291]
                   :span-inside-the-then-block? [275 295]
                   :span-is-non-empty? [296 298])
       :rules rows
       :rules-failing-then-correspondence
       (sorted-map :count (count failing) :ids failing)))))

(defn deep-values [x k]
  (lazy-seq
   (cond
     (map? x) (concat (when (contains? x k) [(get x k)])
                      (mapcat #(deep-values % k) (vals x)))
     (coll? x) (mapcat #(deep-values % k) x)
     :else nil)))

(defn prior-count [path preferred-key]
  (let [x (edn/read-string (slurp path))
        values (deep-values x preferred-key)]
    (first (filter integer? values))))

(defn temp-dir []
  (.toFile (Files/createTempDirectory "f12-arms-dry-" (make-array FileAttribute 0))))

(defn write-edn! [f x]
  (spit f (str (pr-str x) "\n")))

(defn controls [aif-path construct-path gate-path library-path baseline]
  (let [tmp (temp-dir)
        registry (edn/read-string (slurp aif-path))
        choices (:choices registry)
        f12-keys (vec (sort (for [[k v] choices :when (= :F12 (:row v))] k)))
        ckey (first f12-keys)
        arm-id (get-in choices [ckey :arms 0 :arm])
        c1-file (io/file tmp "c1.edn")
        c1-reg (assoc-in registry [:choices ckey :arms 0 :run-by] :not-run)
        _ (write-edn! c1-file c1-reg)
        c1-landed (= :not-run (get-in (edn/read-string (slurp c1-file))
                                      [:choices ckey :arms 0 :run-by]))
        c1 (census c1-file)
        c2-file (io/file tmp "c2.edn")
        c2-reg (assoc-in registry [:choices ckey :status] :control-status)
        _ (write-edn! c2-file c2-reg)
        c2-landed (= :control-status (get-in (edn/read-string (slurp c2-file))
                                             [:choices ckey :status]))
        c2 (census c2-file)
        c3-file (io/file tmp "construct.clj")
        c3-text (str/replace-first (slurp construct-path)
                                   #":added-by-organise\s+#\{\}"
                                   ":added-by-organise (set [])")
        _ (spit c3-file c3-text)
        c3-landed (str/includes? (slurp c3-file) ":added-by-organise (set [])")
        c3 (construct-measure c3-file)
        c4-lib (io/file tmp "library")
        _ (let [src (.toPath (io/file library-path)) dst (.toPath c4-lib)]
            (doseq [f (file-seq (io/file library-path))]
              (let [rel (.relativize src (.toPath f)) out (.resolve dst rel)]
                (if (.isDirectory f)
                  (Files/createDirectories out (make-array FileAttribute 0))
                  (Files/copy (.toPath f) out (into-array java.nio.file.CopyOption []))))))
        c4-target (io/file c4-lib "war-machine/ambient-pattern-retrieval.flexiarg")
        c4-lines (vec (str/split-lines (slurp c4-target)))
        c4-planted (vec (concat (subvec c4-lines 0 14) ["  CONTROL INSERTION"] (subvec c4-lines 14)))
        _ (spit c4-target (str (str/join "\n" c4-planted) "\n"))
        c4-landed (= "  CONTROL INSERTION" (nth (str/split-lines (slurp c4-target)) 14))
        c4 (rule-measure gate-path c4-lib)
        c5-file (io/file tmp "gate.clj")
        rules (read-rule-table gate-path)
        _ (spit c5-file (str "(def rule-table " (pr-str (conj (vec rules) (first rules))) ")\n"))
        c5-landed (= 5 (count (read-rule-table c5-file)))
        c5-error (try (rule-measure c5-file library-path) nil
                      (catch Exception e (.getMessage e)))]
    [(sorted-map :after-arms-dry? (:arms-dry? c1)
                 :after-arms-not-dry (:arms-not-dry c1)
                 :before-arms-dry? (:arms-dry? baseline)
                 :choice ckey :control :run-by-not-run :arm arm-id
                 :plant-verified? c1-landed)
     (sorted-map :after (:choices-not-observed-not-decided c2)
                 :before (:choices-not-observed-not-decided baseline)
                 :choice ckey :control :status-changed :plant-verified? c2-landed)
     (sorted-map :after (:added-by-organise-is-a-literal-empty-set? c3)
                 :before (get-in baseline [:constructor :added-by-organise-is-a-literal-empty-set?])
                 :control :computed-added-by-organise :plant-verified? c3-landed)
     (sorted-map :after (get-in c4 [:rules 0 :conditions :span-inside-the-then-block?])
                 :before (get-in baseline [:then-correspondence :rules 0 :conditions :span-inside-the-then-block?])
                 :control :line-inserted-above-then :plant-verified? c4-landed
                 :rule (get-in c4 [:rules 0 :rule]))
     (sorted-map :control :fifth-rule :observed-failure c5-error
                 :plant-verified? c5-landed)]))

(defn measure []
  (let [aif (env-file "F12_AIF_EQ" default-aif)
        construct (env-file "F12_CONSTRUCT" default-construct)
        gate (env-file "F12_GATE" default-gate)
        library (env-file "F12_LIBRARY" default-library)
        census-result (census aif)
        constructor-result (construct-measure construct)
        rule-result (rule-measure gate library)
        p1 (prior-count (io/file lab-dir "runs/F12-organise/10-d3-encoding.edn")
                        :then-correspondence-failures)
        p2 (prior-count (io/file lab-dir "runs/F12-organise/11-d2-denominator.edn")
                        :failure-count)
        recomputed (get-in rule-result [:rules-failing-then-correspondence :count])
        base (merge
              (sorted-map
               :constructor constructor-result
               :cross-check (sorted-map
                             :agree? (and (= p1 p2) (= p1 recomputed))
                             :prior-count p1
                             :prior-second-count p2
                             :recomputed-count recomputed)
               :schema :f12/arms-dry-v1
               :then-correspondence rule-result)
              census-result)]
    (assoc base :controls (controls aif construct gate library base))))

(try
  (let [out (env-file "F12_OUT" default-out)
        result (measure)]
    (.mkdirs (.getParentFile out))
    (write-edn! out result)
    (println (str out)))
  (catch Exception e
    (binding [*out* *err*]
      (println "f12_arms_dry_check:" (.getMessage e)))
    (System/exit 1)))
