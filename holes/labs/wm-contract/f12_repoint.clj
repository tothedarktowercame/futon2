(ns f12-repoint
  "F12 slice 21: key-path re-pointing for the slice-19 D2 artifact reflow."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.pprint :as pprint]
            [clojure.string :as str]))

(def root (.getCanonicalFile (io/file ".")))
(def artifact-rel "runs/F12-organise/11-d2-denominator.edn")
(def output-rel "runs/F12-organise/12-repoint.edn")
(def citing-files ["aif-equations.edn" "worklist.edn" "C554-F12-d2-denominator.md"])
(def post-files ["C555-F12-d2-fifth-arm.md" "C556-F12-artifact-pointer-drift.md"])
(def reflow "3c0ad833")
(def citation-re (re-pattern (str (java.util.regex.Pattern/quote artifact-rel) ":([0-9]+)(?:-([0-9]+))?")))

(defn git-show [spec]
  (let [{:keys [exit out err]} (shell/sh "git" "-C" (str root) "show" spec)]
    (when-not (zero? exit) (throw (ex-info "git show failed" {:spec spec :stderr err}))) out))

(defn line-at [s i] (inc (count (filter #{\newline} (subs s 0 (min i (count s)))))))
(defn ws? [c] (or (Character/isWhitespace ^char c) (= c \,)))
(defn delim? [c] (or (ws? c) (contains? #{\{ \} \[ \] \( \) \" \;} c)))

(declare parse-node)

(defn skip-space [s i]
  (loop [j i]
    (cond
      (>= j (count s)) j
      (ws? (.charAt s j)) (recur (inc j))
      (= \; (.charAt s j)) (recur (or (str/index-of s "\n" j) (count s)))
      :else j)))

(defn scalar-end [s i]
  (loop [j i]
    (if (or (>= j (count s)) (delim? (.charAt s j))) j (recur (inc j)))))

(defn string-end [s i]
  (loop [j (inc i) escaped? false]
    (when (>= j (count s)) (throw (ex-info "unterminated string" {:at i})))
    (let [c (.charAt s j)]
      (cond escaped? (recur (inc j) false)
            (= c \\) (recur (inc j) true)
            (= c \") (inc j)
            :else (recur (inc j) false)))))

(defn read-value [s a b] (edn/read-string (subs s a b)))

(defn parse-coll [s i path close kind]
  (loop [j (inc i) children [] n 0]
    (let [j (skip-space s j)]
      (when (>= j (count s)) (throw (ex-info "unterminated collection" {:at i})))
      (if (= close (.charAt s j))
        (let [end (inc j)]
          [{:path path :span [(line-at s i) (line-at s (dec end))]
            :value (read-value s i end) :children children :kind kind}
           end])
        (if (= kind :map)
          (let [[kn k-end] (parse-node s j (conj path [:map-key n]))
                k (:value kn)
                kn (assoc kn :path (conj path [:map-key k]))
                v-start (skip-space s k-end)
                [vn v-end] (parse-node s v-start (conj path k))]
            (recur v-end (conj children kn vn) (inc n)))
          (let [[node end] (parse-node s j (conj path n))]
            (recur end (conj children node) (inc n))))))))

(defn parse-node [s i path]
  (let [i (skip-space s i) c (.charAt s i)]
    (cond
      (= c \{) (parse-coll s i path \} :map)
      (= c \[) (parse-coll s i path \] :vector)
      (= c \() (parse-coll s i path \) :list)
      (= c \#) (if (and (< (inc i) (count s)) (= \{ (.charAt s (inc i))))
                 (parse-coll s (inc i) path \} :set)
                 (let [end (scalar-end s i)]
                   [{:path path :span [(line-at s i) (line-at s (dec end))]
                     :value (read-value s i end) :children [] :kind :scalar} end]))
      (= c \") (let [end (string-end s i)]
                   [{:path path :span [(line-at s i) (line-at s (dec end))]
                     :value (read-value s i end) :children [] :kind :scalar} end])
      :else (let [end (scalar-end s i)]
              [{:path path :span [(line-at s i) (line-at s (dec end))]
                :value (read-value s i end) :children [] :kind :scalar} end]))))

(defn flatten-nodes [node] (cons node (mapcat flatten-nodes (:children node))))
(defn scan [s] (let [[root-node end] (parse-node s 0 [])]
                 (when-not (= (skip-space s end) (count s))
                   (throw (ex-info "trailing input" {:at end})))
                 (vec (flatten-nodes root-node))))
(defn index [nodes] (into {} (map (juxt :path identity) nodes)))
(defn contained? [[a b] [x y]] (and (<= a x) (<= y b)))

(defn maximal-contained [nodes span]
  (let [inside (filter #(contained? span (:span %)) nodes)
        paths (set (map :path inside))]
    (vec (remove #(and (seq (:path %)) (contains? paths (pop (:path %)))) inside))))

(defn merge-spans [spans]
  (reduce (fn [acc [a b]]
            (if-let [[x y] (peek acc)]
              (if (<= a (inc y)) (conj (pop acc) [x (max y b)]) (conj acc [a b]))
              [[a b]])) [] (sort spans)))

(defn common-prefix [paths]
  (vec (map first (take-while #(apply = %) (apply map vector paths)))))

(defn lines [s [a b]]
  (str/join "\n" (subvec (vec (str/split-lines s)) (dec a) b)))
(defn span-str [[a b]] (if (= a b) (str a) (str a "-" b)))

(defn citations [text]
  (vec (for [m (re-seq citation-re text)]
         {:literal (first m) :span [(parse-long (nth m 1))
                                    (parse-long (or (nth m 2) (nth m 1)))]})))

(defn positioned-matches [re text]
  (let [m (re-matcher re text)]
    (loop [rows []]
      (if (.find m)
        (recur (conj rows {:at (.start m) :match (.group m)
                           :groups (mapv #(.group m %) (range 1 (inc (.groupCount m))))}))
        rows))))

(defn mapping [old-text new-text old-index new-index {:keys [literal span]}]
  (let [initial (maximal-contained (vals old-index) span)
        refine (fn refine [node]
                 (let [other (new-index (:path node))]
                   (if (and other (= (:value node) (:value other)))
                     [node]
                     (if (seq (:children node)) (mapcat refine (:children node)) [node]))))
        nodes (vec (mapcat refine initial))
        paths (mapv :path nodes)
        missing (vec (remove #(contains? new-index %) paths))
        unequal (vec (for [p paths :when (and (contains? new-index p)
                                              (not= (:value (old-index p)) (:value (new-index p))))] p))
        raw-spans (when (and (seq paths) (empty? missing) (empty? unequal))
                    (merge-spans (map #(:span (new-index %)) paths)))
        semantic-paths (remove #(some vector? %) paths)
        enclosing-path (when (> (count raw-spans) 1) (common-prefix semantic-paths))
        enclosing (when (seq enclosing-path) (new-index enclosing-path))
        enclosing-span (when enclosing
                         [(reduce min (map first raw-spans))
                          (reduce max (map second raw-spans))])
        spans (if enclosing [enclosing-span] raw-spans)
        same? (= [span] spans)
        verdict (cond (empty? paths) :needs-hand-check
                      (seq missing) :needs-hand-check
                      (seq unequal) :needs-hand-check
                      (> (count spans) 1) :non-contiguous
                      same? :already-correct
                      :else :repointed)]
    {:old-citation literal :old-span span :key-paths paths
     :new-spans spans :new-citation (when (= 1 (count spans))
                                     (str artifact-rel ":" (span-str (first spans))))
     :old-content (lines old-text span)
     :new-content (when (= 1 (count spans)) (lines new-text (first spans)))
     :mapped-spans-before-enclosing-node raw-spans
     :enclosing-node-used (when enclosing
                            {:path enclosing-path :span enclosing-span
                             :reason :old-citation-covered-the-preexisting-children-of-this-node})
     :missing-paths missing :unequal-paths unequal :verdict verdict}))

(defn historic-citations [file]
  (citations (git-show (str reflow "^:holes/labs/wm-contract/" file))))

(defn replace-counted [text replacements]
  (let [budgets (atom (frequencies (map :old-citation replacements)))
        targets (into {} (map (juxt :old-citation :new-citation) replacements))]
    (str/replace text citation-re
                 (fn [lit]
                   (let [s (if (string? lit) lit (first lit))]
                     (if (pos? (get @budgets s 0))
                       (do (swap! budgets update s dec) (or (targets s) s)) s))))))

(def short-re #"(?<![A-Za-z0-9_./-]):([0-9]+)(?:-([0-9]+))?")
(def full-re #"([A-Za-z0-9_./-]+\.(?:edn|clj|md|lean|bb|sh|txt)):[0-9]+(?:-[0-9]+)?")

(defn shorthand-rows
  "Every bare `:N` / `:N-M` citation in TEXT, tagged with the file named by the
   nearest preceding full-path citation -- the antecedent a reader resolves it
   against."
  [text]
  (let [fulls (vec (for [m (positioned-matches full-re text)]
                     {:at (:at m) :file (first (:groups m))}))]
    (vec (for [m (positioned-matches short-re text)
               :let [target (:file (last (take-while #(< (:at %) (:at m)) fulls)))
                     [a b] (:groups m)]]
           {:at (:at m) :line (line-at text (:at m)) :citation (:match m)
            :span [(parse-long a) (parse-long (or b a))] :antecedent target
            :resolves-to-this-artifact? (= target artifact-rel)
            :ambiguous? (nil? target)}))))

(defn shorthand-census [file]
  (let [rows (mapv #(dissoc % :at :span) (shorthand-rows (slurp (io/file root file))))]
    {:file file :count (count rows)
     :to-this-artifact (count (filter :resolves-to-this-artifact? rows))
     :ambiguous (count (filter :ambiguous? rows)) :citations rows}))

(defn artifact-shorthand [text] (vec (filter :resolves-to-this-artifact? (shorthand-rows text))))

(defn shorthand-state
  "Which of the two literal sequences the file is carrying. `:unrecognised` is
   the fail-closed branch: something other than this repair moved a shorthand."
  [historic expected current]
  (cond (empty? historic) :no-candidates
        (= current historic) :pre-repair
        (= current expected) :repaired
        :else :unrecognised))

(defn shorthand-audit
  "Shorthand citations of the artifact, mapped by the same key-path route as the
   full-path ones. The CANDIDATE SET comes from the pre-reflow file: a `:N`
   written after the reflow was written against the current lines and is not a
   candidate. The mapping is therefore basis-only -- it depends on the two
   artifact revisions and not on whether this repair has already run -- so the
   record reproduces from the tree it describes. `:state` is the one field that
   reads the tree, and it says which of the two literal sequences is there."
  [old-text new-text old-index new-index file]
  (let [historic (artifact-shorthand (git-show (str reflow "^:holes/labs/wm-contract/" file)))
        audits (mapv (fn [row]
                       (let [a (mapping old-text new-text old-index new-index
                                        {:literal (:citation row) :span (:span row)})]
                         (assoc a :citing-file file :line (:line row)
                                :new-shorthand (when (= 1 (count (:new-spans a)))
                                                 (str ":" (span-str (first (:new-spans a))))))))
                     historic)
        expected (mapv #(or (:new-shorthand %) (:old-citation %)) audits)
        current (mapv :citation (artifact-shorthand (slurp (io/file root file))))
        state (shorthand-state (mapv :citation historic) expected current)]
    {:file file
     :pre-reflow-citations (mapv :citation historic)
     :repaired-citations expected
     :state state
     :audits audits}))

(defn apply-shorthand!
  "Splice the repaired shorthands in by OFFSET, back to front, checking at each
   one that the literal is still where the scan of the CURRENT file found it.
   A no-op unless the file is still carrying the pre-reflow sequence."
  [file {:keys [state audits]}]
  (let [f (io/file root file)
        before (slurp f)
        by-offset (when (= :pre-repair state)
                    (let [rows (artifact-shorthand before)]
                      (map (fn [row a] (assoc a :at (:at row))) rows audits)))
        edits (sort-by :at > (filter #(and (contains? #{:repointed :already-correct} (:verdict %))
                                           (:new-shorthand %)) by-offset))
        after (reduce (fn [t {:keys [at old-citation new-shorthand]}]
                        (let [end (+ at (count old-citation))]
                          (when-not (= old-citation (subs t at end))
                            (throw (ex-info "shorthand not at its scanned offset"
                                            {:file file :at at :expected old-citation})))
                          (str (subs t 0 at) new-shorthand (subs t end))))
                      before edits)
        untouched (fn [t] (frequencies (map :citation (remove :resolves-to-this-artifact?
                                                              (shorthand-rows t)))))]
    (when-not (= before after) (spit f after))
    {:file file :state state :edits (count edits)
     :non-artifact-shorthand-unchanged? (= (untouched before) (untouched after))}))

(defn controls [old-text new-text old-index new-index sample]
  (let [corrupt-path (first (:key-paths sample))
        corrupt-index (assoc-in new-index [corrupt-path :value] ::corrupt)
        broken-index (dissoc new-index corrupt-path)
        identity-result (mapping old-text old-text old-index old-index
                                 {:literal (:old-citation sample) :span (:old-span sample)})]
    [{:control :added-value-has-no-old-counterpart :before :not-indexed-in-old
      :after :needs-hand-check :verified? (not (contains? old-index [:split-denominator]))}
     {:control :corrupt-new-value :before (:verdict sample)
      :after (:verdict (mapping old-text new-text old-index corrupt-index
                                {:literal (:old-citation sample) :span (:old-span sample)}))
      :plant-verified? (= ::corrupt (:value (corrupt-index corrupt-path)))}
     {:control :post-reflow-citation :before :post-reflow :after :written-after-reflow
      :verified? (seq (citations (slurp (io/file root (first post-files)))))}
     {:control :identity :before :old-as-old :after (:verdict identity-result)
      :edits 0 :verified? (= :already-correct (:verdict identity-result))}
     {:control :shorthand-sequence-plant :before :repaired
      :after (shorthand-state [":1148"] [":1880"] [":9999"])
      :verified? (= :unrecognised (shorthand-state [":1148"] [":1880"] [":9999"]))
      :also-checked {:pre-repair (shorthand-state [":1148"] [":1880"] [":1148"])
                     :repaired (shorthand-state [":1148"] [":1880"] [":1880"])
                     :no-candidates (shorthand-state [] [] [])}}
     {:control :broken-key-path :before (:verdict sample)
      :after (:verdict (mapping old-text new-text old-index broken-index
                                {:literal (:old-citation sample) :span (:old-span sample)}))
      :plant-verified? (not (contains? broken-index corrupt-path))}]))

(defn -main [& _]
  (let [old-text (git-show (str reflow "^:holes/labs/wm-contract/" artifact-rel))
        new-text (slurp (io/file root artifact-rel))
        old-index (index (scan old-text)) new-index (index (scan new-text))
        audits (vec (mapcat (fn [file]
                              (map #(assoc (mapping old-text new-text old-index new-index %) :citing-file file)
                                   (historic-citations file))) citing-files))
        repairable (filter #(contains? #{:repointed :already-correct} (:verdict %)) audits)
        shorthand (mapv #(shorthand-audit old-text new-text old-index new-index %) citing-files)
        shorthand-applied (mapv #(apply-shorthand! (:file %) %) shorthand)
        _ (doseq [file citing-files
                  :let [rows (filter #(= file (:citing-file %)) repairable)
                        f (io/file root file)
                        before (slurp f)
                        after (replace-counted before rows)]]
            (when-not (= before after) (spit f after)))
        damage (vec (for [file citing-files
                          :let [cs (historic-citations file)]]
                      {:file file :full-path-citations (count cs)
                       :different-at-same-lines
                       (count (filter #(not= (lines old-text (:span %))
                                             (lines new-text (:span %))) cs))}))
        sample (first (filter #(seq (:key-paths %)) audits))
        result (sorted-map
                :artifact {:old-lines (count (str/split-lines old-text))
                           :new-lines (count (str/split-lines new-text))}
                :recomputed-c556-table damage
                :c556-published-table [{:file "aif-equations.edn" :full-path-citations 66 :different-at-same-lines 66}
                                       {:file "worklist.edn" :full-path-citations 30 :different-at-same-lines 12}
                                       {:file "C554-F12-d2-denominator.md" :full-path-citations 13 :different-at-same-lines 12}]
                :c556-disagreement
                {:aif-equations.edn "git show 3c0ad833^ contains 65, not 66, pre-reflow citations; all 65 drift"
                 :worklist.edn "git show 3c0ad833^ contains 13 citations, 12 drift; C556's 30 combines these with 17 post-reflow citations"}
                :bucket-counts (frequencies (map :verdict audits))
                :citations audits
                :post-reflow-full-path-census
                (vec (for [f post-files] {:file f :count (count (citations (slurp (io/file root f))))
                                          :verdict :written-after-reflow}))
                :shorthand-census (mapv shorthand-census (conj (vec citing-files) (first post-files) (second post-files)))
                :shorthand-repoint shorthand
                :shorthand-applied shorthand-applied
                :negative-controls (controls old-text new-text old-index new-index sample)
                :needs-hand-check
                (vec (concat
                      (filter #(contains? #{:needs-hand-check :non-contiguous} (:verdict %)) audits)
                      (mapcat #(filter (fn [a] (contains? #{:needs-hand-check :non-contiguous} (:verdict a)))
                                       (:audits %)) shorthand)
                      (for [x shorthand :when (= :unrecognised (:state x))]
                        {:citing-file (:file x) :verdict :shorthand-sequence-unrecognised
                         :pre-reflow-citations (:pre-reflow-citations x)
                         :repaired-citations (:repaired-citations x)})
                      (remove :non-artifact-shorthand-unchanged? shorthand-applied))))]
    (spit (io/file root output-rel) (with-out-str (pprint/pprint result)))
    (println "f12 repoint:" (:bucket-counts result)
             "shorthand" (frequencies (mapcat #(map :verdict (:audits %)) shorthand))
             "edits" (mapv (juxt :file :state :edits) shorthand-applied)
             "hand checks" (count (:needs-hand-check result)))
    (System/exit (if (seq (:needs-hand-check result)) 1 0))))
