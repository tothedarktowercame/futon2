#!/usr/bin/env bb
(ns checks.absent-is-loud-lint
  (:require [clojure.java.io :as io]
            [clojure.java.shell]
            [clojure.string :as str]))

(def scope-specs
  [{:repo "futon2" :root "/home/joe/code/futon2/src" :recursive? true}
   {:repo "futon2" :root "/home/joe/code/futon2/scripts" :recursive? true}
   {:repo "futon2" :root "/home/joe/code/futon2/checks" :recursive? false}
   {:repo "futon3" :root "/home/joe/code/futon3/checks" :recursive? false}
   {:repo "futon0" :root "/home/joe/code/futon0/scripts" :recursive? true}
   {:repo "futon3a" :root "/home/joe/code/futon3a/src" :recursive? true}])

(def guard-heads
  '#{when when-let if if-let when-some if-some some-> some->> or try})

(def optional-doc-re #"(?i)\boptional\b|graceful|returns nil|returns \[\]|returns \{\}")
(def path-name-re #"(?i)(path|file|dir|root|cache|artifact|registry|edges|badges|source|ref|prereg|index)")
(def reader-heads
  '#{slurp read-string clojure.core/read-string
     edn/read edn/read-string clojure.edn/read clojure.edn/read-string
     json/parse-string json/read-str cheshire.core/parse-string})

(defn repo-head-sha [repo]
  (let [dir (str "/home/joe/code/" repo)
        {:keys [exit out]} (apply clojure.java.shell/sh ["git" "-C" dir "rev-parse" "--short" "HEAD"])]
    (when (zero? exit) (str/trim out))))

(defn file-spec->paths [{:keys [root recursive?]}]
  (let [dir (io/file root)]
    (if-not (.exists dir)
      []
      (->> (if recursive? (file-seq dir) (.listFiles dir))
           (filter some?)
           (filter #(.isFile ^java.io.File %))
           (filter #(str/ends-with? (.getName ^java.io.File %) ".clj"))
           (map #(.getAbsolutePath ^java.io.File %))
           sort
           vec))))

(defn default-scope-files []
  (vec (mapcat file-spec->paths scope-specs)))

(defn read-forms [path]
  (with-open [r (io/reader path)]
    (let [pbr (clojure.lang.LineNumberingPushbackReader. r)
          eof (Object.)]
      (loop [forms []]
        (let [form (read {:eof eof} pbr)]
          (if (identical? eof form)
            forms
            (recur (conj forms form))))))))

(defn require-clause->aliases [clause]
  (if (and (seq? clause) (= :require (first clause)))
    (reduce (fn [acc entry]
              (if (vector? entry)
                (let [target (first entry)
                      as-idx (.indexOf entry :as)]
                  (if (and (symbol? target) (<= 0 as-idx) (< (inc as-idx) (count entry)))
                    (assoc acc (nth entry (inc as-idx)) target)
                    acc))
                acc))
            {}
            (rest clause))
    {}))

(defn parse-ns-info [forms]
  (let [ns-form (first (filter #(and (seq? %) (= 'ns (first %))) forms))
        ns-name (second ns-form)
        aliases (reduce merge {} (map require-clause->aliases (drop 2 ns-form)))]
    {:ns ns-name :aliases aliases}))

(defn static-str [form]
  (binding [*print-meta* false]
    (pr-str form)))

(defn line-of [form]
  (:line (meta form)))

(defn def-constant-value [env form]
  (when (and (seq? form) (= 'def (first form)) (symbol? (second form)))
    (let [name (second form)
          value-form (nth form 2 nil)]
      (when-let [value (cond
                         (string? value-form) value-form
                         (symbol? value-form) (get env value-form)
                         (and (seq? value-form) (= 'str (first value-form)))
                         (when-let [parts (seq (keep #(cond
                                                        (string? %) %
                                                        (symbol? %) (get env %)
                                                        (and (seq? %) (= 'System/getProperty (first %))
                                                             (= "user.home" (second %)))
                                                        (System/getProperty "user.home")
                                                        :else nil)
                                                     (rest value-form)))]
                           (apply str parts))
                         (and (seq? value-form) (= 'System/getProperty (first value-form))
                              (= "user.home" (second value-form)))
                         (System/getProperty "user.home")
                         :else nil)]
        [name value]))))

(defn collect-constants [forms]
  (loop [env {'home (System/getProperty "user.home")}
         [form & more] forms]
    (if-not form
      env
      (recur (if-let [[k v] (def-constant-value env form)]
               (assoc env k v)
               env)
             more))))

(defn parse-defn-form [form]
  (when (and (seq? form) (#{'defn 'defn-} (first form)) (symbol? (second form)))
    (let [name (second form)
          after-name (drop 2 form)
          docstring (when (string? (first after-name)) (first after-name))
          after-doc (if docstring (rest after-name) after-name)
          attr-map (when (map? (first after-doc)) (first after-doc))
          after-attr (if attr-map (rest after-doc) after-doc)
          arities (if (vector? (first after-attr))
                    [(cons (first after-attr) (rest after-attr))]
                    after-attr)
          parsed-arities
          (keep (fn [arity]
                  (when (vector? (first arity))
                    {:args (vec (first arity))
                     :body (cons 'do (rest arity))}))
                arities)]
      {:name name
       :line (line-of form)
       :docstring docstring
       :attr-map attr-map
       :arities (vec parsed-arities)
       :form form})))

(defn contains-symbol? [form target]
  (boolean
   (some #(= % target)
         (tree-seq coll? seq form))))

(defn exists-check? [form argset]
  (boolean
   (some (fn [node]
           (and (seq? node)
                (#{'.exists 'fs/exists? 'fs/regular-file?} (first node))
                (some #(contains-symbol? node %) argset)))
         (tree-seq coll? seq form))))

(defn reader-over-arg? [form argset]
  (boolean
   (some (fn [node]
           (and (seq? node)
                (reader-heads (first node))
                (some #(contains-symbol? node %) argset)))
         (tree-seq coll? seq form))))

(defn with-reader-over-arg? [form argset]
  (boolean
   (some (fn [node]
           (and (seq? node)
                (= 'with-open (first node))
                (some #(contains-symbol? node %) argset)
                (some (fn [inner]
                        (and (seq? inner)
                             (#{'io/reader 'clojure.java.io/reader} (first inner))
                             (some #(contains-symbol? inner %) argset)))
                      (tree-seq coll? seq node))))
         (tree-seq coll? seq form))))

(defn returns-tagged-loud? [form]
  (boolean
   (some (fn [node]
           (and (map? node)
                (or (contains? node :missing)
                    (contains? node :unreadable)
                    (= :missing (:status node))
                    (= :unreadable (:status node)))))
         (tree-seq coll? seq form))))

(defn contains-throw? [form]
  (boolean
   (some (fn [node]
           (and (seq? node) (#{'throw 'reject!} (first node))))
         (tree-seq coll? seq form))))

(defn catch-defaults [form]
  (vec
   (keep (fn [node]
           (when (and (seq? node) (= 'catch (first node)))
             (let [handler (nth node 3 nil)]
               (cond
                 (nil? handler) :nil
                 (or (= [] handler) (= {} handler)) :empty
                 (and (map? handler)
                      (or (contains? handler :missing) (contains? handler :unreadable)))
                 :tagged
                 :else nil))))
         (tree-seq coll? seq form))))

(defn throwish? [form]
  (or (contains-throw? form)
      (returns-tagged-loud? form)))

(defn silent-exists-guard? [form argset]
  (boolean
   (some (fn [node]
           (and (seq? node)
                (let [head (first node)]
                  (cond
                    (= 'when head)
                    (and (exists-check? (second node) argset)
                         (reader-over-arg? node argset))

                    (= 'if head)
                    (let [then-branch (nth node 2 nil)
                          else-branch (nth node 3 nil)]
                      (and (exists-check? (second node) argset)
                           (reader-over-arg? then-branch argset)
                           (not (throwish? else-branch))))

                    :else false))))
         (tree-seq coll? seq form))))

(defn declared-optional?
  "Optionality is declared by the DOCSTRING stating the absence result, never by
   the helper's name: `safe-`/`maybe-` in a name is a claim, not a declaration
   (I_absent_is_loud: the declaration is what the lint reads). Owner amendment
   at the AUD-D2 gate; `safe-slurp-json` (war_machine.clj:3451) was the case."
  [{docstring :docstring}]
  (boolean (and docstring (re-find optional-doc-re docstring))))

(defn helper-classification [helper]
  (let [path-args (vec (filter #(and (symbol? %) (re-find path-name-re (name %)))
                               (mapcat :args (:arities helper))))
        argset (set path-args)
        reader? (some #(or (reader-over-arg? (:body %) argset)
                           (with-reader-over-arg? (:body %) argset))
                      (:arities helper))]
    (when reader?
      (let [bodies (map :body (:arities helper))
            loud? (or (some contains-throw? bodies)
                      (some returns-tagged-loud? bodies))
            silent? (or (seq (mapcat catch-defaults bodies))
                        (some #(silent-exists-guard? % argset) bodies))]
        (cond
          (declared-optional? helper)
          (assoc helper :class :declared-optional
                        :reason "name/docstring declares optional/graceful read")

          (and loud? (not silent?))
          (assoc helper :class :loud
                        :reason "throws or returns tagged missing/unreadable state")

          silent?
          (assoc helper :class :silent
                        :reason "absence/parse failure collapses to nil/empty/default")

          :else
          (assoc helper :class :loud
                        :reason "missing/unparseable path propagates as an exception"))))))

(defn resolve-symbol [ns-sym aliases sym]
  (cond
    (qualified-symbol? sym)
    (let [qual (symbol (namespace sym))
          target-ns (or (get aliases qual) qual)]
      (symbol (str target-ns) (name sym)))

    (symbol? sym)
    (symbol (str ns-sym) (name sym))

    :else nil))

(defn extract-path-arg [helper]
  (let [args (filter symbol? (mapcat :args (:arities helper)))]
    (or (first (filter #(re-find path-name-re (name %)) args))
        (first args))))

(defn eval-static-path [env form]
  (cond
    (string? form) form
    (symbol? form) (get env form)
    (and (seq? form) (= 'str (first form)))
    (when-let [parts (seq (map #(eval-static-path env %) (rest form)))]
      (when (every? string? parts) (apply str parts)))
    (and (seq? form) (#{'io/file 'clojure.java.io/file 'fs/path 'java.io.File.} (first form)))
    (let [parts (map #(eval-static-path env %) (rest form))]
      (when (every? string? parts) (.getPath (apply io/file parts))))
    (and (seq? form) (= 'System/getProperty (first form)) (= "user.home" (second form)))
    (System/getProperty "user.home")
    (and (seq? form) (= 'or (first form)))
    (some #(eval-static-path env %) (rest form))
    :else nil))

(defn ancestor-guard [ancestors]
  (some (fn [form]
          (when (and (seq? form) (guard-heads (first form)))
            (name (first form))))
        (reverse ancestors)))

(defn form-seq-with-ancestors [form]
  (letfn [(step [node ancestors]
            (lazy-seq
             (cons {:form node :ancestors ancestors}
                   (when (coll? node)
                     (mapcat #(step % (conj ancestors node))
                             (seq node))))))]
    (step form [])))

(defn helper-key [helper]
  (symbol (str (:ns helper)) (name (:name helper))))

(defn collect-call-sites [parsed helper-index]
  (let [{:keys [ns aliases consts forms path]} parsed]
    (vec
     (mapcat
      (fn [top]
        (keep
         (fn [{:keys [form ancestors]}]
           (when (seq? form)
             (let [head (first form)
                   fq (resolve-symbol ns aliases head)]
               (when-let [helper (get helper-index fq)]
                 (let [path-arg (extract-path-arg helper)
                       helper-args (vec (filter symbol? (mapcat :args (:arities helper))))
                       args (rest form)
                       idx (.indexOf helper-args path-arg)
                       path-form (nth args (max 0 idx) nil)
                       resolved (eval-static-path consts path-form)]
                   {:file path
                    :line (line-of form)
                    :helper fq
                    :helper-class (:class helper)
                    :path-expr (when path-form (static-str path-form))
                    :resolved-path resolved
                    :exists-now (when resolved (.exists (io/file resolved)))
                    :guard (or (ancestor-guard ancestors) "none")})))))
         (form-seq-with-ancestors top)))
      forms))))

(defn marker-predicate? [helper]
  (boolean
   (some (fn [arity]
           (some #{:missing :unreadable}
                 (tree-seq coll? seq (:body arity))))
         (:arities helper))))

(defn marker-recorder?
  "A recorder makes marker loss observable by swapping marker facts into an
   accumulator.  This is deliberately structural: callers receive no credit
   for a recorder name whose body does not visibly perform the write."
  [helper]
  (boolean
   (some (fn [arity]
           (some (fn [node]
                   (and (seq? node)
                        (= 'swap! (first node))
                        (some #{:missing :unreadable}
                              (tree-seq coll? seq node))))
                 (tree-seq coll? seq (:body arity))))
         (:arities helper))))

(defn calls-marker-recorder? [helper marker-recorders]
  (boolean
   (some (fn [arity]
           (some (fn [node]
                   (and (seq? node)
                        (contains? marker-recorders
                                   (resolve-symbol (:ns helper)
                                                   (:aliases helper)
                                                   (first node)))))
                 (tree-seq coll? seq (:body arity))))
         (:arities helper))))

(defn resolved-head [parsed form]
  (when (seq? form)
    (resolve-symbol (:ns parsed) (:aliases parsed) (first form))))

(defn marker-condition-polarity [parsed marker-predicates value condition]
  (cond
    (and (seq? condition) (= 'not (first condition)))
    (case (marker-condition-polarity parsed marker-predicates value
                                     (second condition))
      :marker-true :marker-false
      :marker-false :marker-true
      nil)

    (and (seq? condition)
         (contains? marker-predicates (resolved-head parsed condition))
         (contains-symbol? condition value))
    :marker-true

    (and (seq? condition)
         (#{'contains? 'get} (first condition))
         (contains-symbol? condition value)
         (some #{:missing :unreadable} condition))
    :marker-true

    (and (seq? condition)
         (keyword? (first condition))
         (#{:missing :unreadable} (first condition))
         (contains-symbol? condition value))
    :marker-true

    :else nil))

(defn returned-marker? [form value]
  (cond
    (= form value) true
    (and (seq? form) (= 'do (first form)))
    (returned-marker? (last form) value)
    (and (seq? form) (#{'let 'let*} (first form)))
    (returned-marker? (last form) value)
    :else false))

(defn preserves-marker? [form value]
  (or (= form value)
      (returned-marker? form value)
      (and (map? form) (contains-symbol? form value))
      (boolean
       (some (fn [node]
               (and (seq? node)
                    (#{'throw 'conj 'swap!
                       'assoc 'update 'merge} (first node))
                    (contains-symbol? node value)))
             (tree-seq coll? seq form)))
      (boolean
       (some (fn [node]
               (and (seq? node)
                    (= 'binding (first node))
                    (vector? (second node))
                    (some #(and (seq? %)
                                (#{'println 'prn} (first %))
                                (contains-symbol? % value))
                          (tree-seq coll? seq node))
                    (some (fn [[bound target]]
                            (and (= '*out* bound) (= '*err* target)))
                          (partition 2 (second node)))))
             (tree-seq coll? seq form)))))

(defn marker-conditional-disposition
  [parsed marker-predicates value node]
  (when (seq? node)
    (let [head (first node)
          conditional? (#{'if 'when 'when-not 'cond} head)
          condition (second node)
          cond-marker-branch
          (when (= 'cond head)
            (some (fn [[test branch]]
                    (when (= :marker-true
                             (marker-condition-polarity
                              parsed marker-predicates value test))
                      branch))
                  (partition 2 (rest node))))
          polarity (when (and conditional? (not= 'cond head))
                     (marker-condition-polarity parsed marker-predicates
                                                value condition))]
      (when (or polarity cond-marker-branch)
        (let [then-branch (nth node 2 nil)
              else-branch (nth node 3 nil)
              marker-branch
              (case head
                when (if (= polarity :marker-true) then-branch nil)
                when-not (if (= polarity :marker-true) nil then-branch)
                if (if (= polarity :marker-true) then-branch else-branch)
                cond cond-marker-branch)]
          (if (preserves-marker? marker-branch value)
            :conformant
            :marker-swallowed))))))

(defn marker-use-disposition [parsed marker-predicates value context]
  (let [conditional-results
        (keep #(marker-conditional-disposition parsed marker-predicates value %)
              (tree-seq coll? seq context))
        destructive-marker-form?
        (boolean
         (some (fn [node]
                 (and (seq? node)
                      (or (and (= 'dissoc (first node))
                               (contains-symbol? node value)
                               (some #{:missing :unreadable} node))
                          (and (= 'or (first node))
                               (contains-symbol? node value)))))
               (tree-seq coll? seq context)))]
    (cond
      (some #{:conformant} conditional-results) :conformant
      (some #{:marker-swallowed} conditional-results) :marker-swallowed
      (preserves-marker? context value) :conformant
      destructive-marker-form? :marker-swallowed
      :else :refused)))

(defn loud-calls-in [parsed loud-index form]
  (when-not (and (seq? form) (#{'let 'let*} (first form)))
    (keep (fn [node]
            (when (seq? node)
              (when-let [helper (get loud-index (resolved-head parsed node))]
                {:call node :helper helper})))
          (tree-seq coll? seq form))))

(defn collect-loud-call-sites [parsed loud-index marker-predicates]
  (vec
   (mapcat
    (fn [top]
      (mapcat
       (fn [node]
         (when (and (seq? node)
                    (#{'let 'let*} (first node))
                    (vector? (second node)))
           (let [bindings (vec (partition 2 (second node)))
                 body (cons 'do (drop 2 node))]
             (mapcat
              (fn [idx [value rhs]]
                (when (symbol? value)
                  (let [context (cons 'do
                                      (concat (mapcat identity (subvec bindings (inc idx)))
                                              (rest body)))]
                    (map (fn [{:keys [call helper]}]
                           (let [disposition (marker-use-disposition
                                              parsed marker-predicates value context)]
                             {:file (:path parsed)
                              :line (line-of call)
                              :helper (helper-key helper)
                              :value value
                              :disposition (if (and (:recording? helper)
                                                    (= :marker-swallowed disposition))
                                             :conformant-recorded
                                             disposition)}))
                         (loud-calls-in parsed loud-index rhs)))))
              (range)
              bindings))))
       (tree-seq coll? seq top)))
    (:forms parsed))))

(defn parse-source [path]
  (let [forms (read-forms path)
        {:keys [ns aliases]} (parse-ns-info forms)
        consts (collect-constants forms)
        defns (->> forms
                   (keep parse-defn-form)
                   (map #(assoc % :ns ns :aliases aliases :file path))
                   vec)
        helpers (->> defns
                     (keep helper-classification)
                     vec)]
    {:path path
     :ns ns
     :aliases aliases
     :consts consts
     :forms forms
     :defns defns
     :helpers helpers}))

(defn repo-shas-line []
  (str/join ", "
            (keep (fn [repo]
                    (when-let [sha (repo-head-sha repo)]
                      (str repo "=" sha)))
                  ["futon2" "futon3" "futon0" "futon3a"])))

(defn markdown-table [headers rows]
  (let [render-row (fn [row] (str "| " (str/join " | " row) " |"))]
    (str (render-row headers) "\n"
         (render-row (repeat (count headers) "---")) "\n"
         (str/join "\n" (map render-row rows))
         (when (seq rows) "\n"))))

(defn rel [path]
  (if (str/starts-with? path "/home/joe/code/")
    (subs path (count "/home/joe/code/"))
    path))

(defn scan! [files]
  (let [parsed (mapv parse-source files)
        helpers (vec (mapcat :helpers parsed))
        helper-index (into {} (map (juxt helper-key identity) helpers))
        marker-recorders (set (map helper-key
                                   (filter marker-recorder?
                                           (mapcat :defns parsed))))
        helper-index (into {}
                           (map (fn [[k helper]]
                                  [k (assoc helper :recording?
                                            (calls-marker-recorder?
                                             helper marker-recorders))]))
                           helper-index)
        silent-helpers (into {} (filter (fn [[_ h]] (= :silent (:class h))) helper-index))
        loud-helpers (into {} (filter (fn [[_ h]] (= :loud (:class h))) helper-index))
        marker-predicates (set (map helper-key
                                    (filter marker-predicate?
                                            (mapcat :defns parsed))))
        call-sites (vec (mapcat #(collect-call-sites % silent-helpers) parsed))
        bound-loud-call-sites
        (vec (mapcat #(collect-loud-call-sites % loud-helpers marker-predicates)
                     parsed))
        loud-call-sites bound-loud-call-sites
        violations (filter #(and (= :silent (:helper-class %))
                                 (false? (:exists-now %)))
                           call-sites)
        marker-swallowed (filter #(= :marker-swallowed (:disposition %)) loud-call-sites)
        recorded-then-substituted
        (filter #(= :conformant-recorded (:disposition %)) loud-call-sites)
        loud-refusals (filter #(= :refused (:disposition %)) loud-call-sites)
        refusals (filter #(= :refused (:class %)) helpers)]
    {:helpers helpers
     :call-sites call-sites
     :loud-call-sites loud-call-sites
     :marker-swallowed (vec marker-swallowed)
     :recorded-then-substituted (vec recorded-then-substituted)
     :violations (vec (concat violations marker-swallowed))
     :refusals (vec (concat refusals loud-refusals))}))

(defn render-findings [{:keys [helpers call-sites loud-call-sites marker-swallowed
                               recorded-then-substituted
                               violations refusals]} scope-label]
  (let [helper-rows (map (fn [{:keys [ns name file line class reason]}]
                           [(str ns "/" name) (clojure.core/name class) (str (rel file) ":" line) reason])
                         (sort-by (juxt :file :line :name) helpers))
        call-rows (map (fn [{:keys [file line helper path-expr resolved-path exists-now guard]}]
                         [(str (rel file) ":" line)
                          (str helper)
                          (or path-expr "unresolved")
                          guard
                          (cond
                            (nil? resolved-path) "dynamic/refused"
                            exists-now "present"
                            :else "absent")
                          (or resolved-path "dynamic path")])
                       (sort-by (juxt :file :line) call-sites))
        refusal-rows (map (fn [{:keys [ns name file line reason helper]}]
                            [(or (when ns (str ns "/" name)) (str helper))
                             (str (rel file) ":" line)
                             (or reason "dynamic marker disposition refused")])
                          (sort-by (juxt :file :line :name) refusals))
        loud-rows (map (fn [{:keys [file line helper value disposition]}]
                         [(str (rel file) ":" line) (str helper) (str value)
                          (name disposition)])
                       (sort-by (juxt :file :line) loud-call-sites))
        helper-counts (frequencies (map :class helpers))
        verdict (str "Verdict: I_absent_is_loud over " scope-label
                     " | repos " (repo-shas-line)
                     " | helpers total=" (count helpers)
                     " loud=" (get helper-counts :loud 0)
                     " silent=" (get helper-counts :silent 0)
                     " declared-optional=" (get helper-counts :declared-optional 0)
                     " refused=" (get helper-counts :refused 0)
                     " | silent call sites=" (count call-sites)
                     " silent+absent-now="
                     (- (count violations) (count marker-swallowed))
                     " | loud call sites=" (count loud-call-sites)
                     " marker-swallowed=" (count marker-swallowed)
                     " recorded-then-substituted="
                     (count recorded-then-substituted))]
    (str "# Absent-is-loud findings\n\n"
         verdict "\n\n"
         "## Helpers\n\n"
         (markdown-table ["helper" "class" "file:line" "reason"] helper-rows) "\n"
         "## Silent Helper Call Sites\n\n"
         (markdown-table ["file:line" "helper" "path expression" "guard form" "exists now" "resolved path"] call-rows) "\n"
         "## Loud Helper Call Sites\n\n"
         (markdown-table ["file:line" "helper" "binding" "marker disposition"] loud-rows) "\n"
         "## Refusals\n\n"
         (if (seq refusal-rows)
           (markdown-table ["helper" "file:line" "reason"] refusal-rows)
           "| helper | file:line | reason |\n|---|---|---|\n| none | n/a | none |\n"))))

(defn fixture-report [files]
  (let [{:keys [violations marker-swallowed recorded-then-substituted]} (scan! files)]
    {:files (mapv rel files)
     :violations (count violations)
     :marker-swallowed (count marker-swallowed)
     :recorded-then-substituted (count recorded-then-substituted)}))

(defn usage []
  (str "usage: bb checks/absent_is_loud_lint.clj [--negative] [--files a.clj,b.clj] [--out findings.md]\n"
       "default scope scans futon2/src, futon2/scripts, futon2/checks, futon3/checks, futon0/scripts, futon3a/src"))

(defn parse-args [args]
  (loop [xs args out {}]
    (if (empty? xs)
      out
      (if (= "--negative" (first xs))
        (recur (rest xs) (assoc out :negative? true))
        (do
          (when-not (second xs) (throw (ex-info "arguments must be --key value pairs" {})))
          (recur (nnext xs) (assoc out (keyword (subs (first xs) 2)) (second xs))))))))

(defn -main [& args]
  (let [{:keys [negative?] :as opts} (parse-args args)
        files (cond
                negative?
                ["/home/joe/code/futon2/checks/fixtures/absent_is_loud/positive.clj"]

                (:files opts)
                (vec (remove str/blank? (str/split (:files opts) #",")))

                :else
                (default-scope-files))
        scope-label (cond negative? "semantic negative fixture"
                          (:files opts) (str/join "," (map rel files))
                          :else "default scope")
        report (scan! files)
        markdown (render-findings report scope-label)
        positive (fixture-report ["/home/joe/code/futon2/checks/fixtures/absent_is_loud/positive.clj"])
        negative (fixture-report ["/home/joe/code/futon2/checks/fixtures/absent_is_loud/negative.clj"])
        verdict-line (nth (str/split-lines markdown) 2 nil)
        out (:out opts)]
    (when out (spit out (str markdown
                             "\nPositive control: " (pr-str positive) "\n"
                             "Negative control: " (pr-str negative) "\n")))
    (when-not (= 4 (:violations positive))
      (binding [*out* *err*]
        (println "fixture positive control failed:" (pr-str positive)))
      (System/exit 2))
    (when-not (= 0 (:violations negative))
      (binding [*out* *err*]
        (println "fixture negative control failed:" (pr-str negative)))
      (System/exit 2))
    (when-not (= 1 (:recorded-then-substituted negative))
      (binding [*out* *err*]
        (println "fixture recording control failed:" (pr-str negative)))
      (System/exit 2))
    (println verdict-line)
    (println "Positive control:" (pr-str positive))
    (println "Negative control:" (pr-str negative))
    (if negative?
      (if (= 4 (count (:violations report)))
        (do (println "absent-is-loud-lint: PASS semantic silent-absence fixture rejected exit-convention=0-pass/1-fail")
            (System/exit 0))
        (do (println "absent-is-loud-lint: FAIL semantic silent-absence fixture slipped exit-convention=0-pass/1-fail")
            (System/exit 2)))
      (do (println (str "absent-is-loud-lint: " (if (seq (:violations report)) "FAIL" "PASS")
                        " exit-convention=0-pass/1-fail"))
          (System/exit (if (seq (:violations report)) 1 0))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
