;; Enumerate the self-heal route's outcome sites in the Clojure code, mechanically.
;; A site is one place a function decides an outcome:
;;   (refusal :reason ...)            kind :refusal
;;   (held! :reason ...)              kind :held
;;   a map literal with a :status key       kind :status-map (status :computed when not a literal)
;;   (conj :term), as in a cond-> of absent terms   kind :absent-term
;;   classifier outcome literals in meta-terminal-injury.clj kind :classification
;; plus one :defn row per top-level definition, an anchor for line citations only.
;; Usage: bb code_sites.clj  -> JSON array of sites on stdout.
(require '[cheshire.core :as json] '[edamame.core :as e] '[clojure.java.io :as io] '[clojure.string :as str])

(def sources
  ;; [tag repo file only-fns]  only-fns nil = whole file
  [["P" "/home/joe/code/futon2" "src/futon2/aif/meta_self_heal_production.clj" nil]
   ["C" "/home/joe/code/futon2" "src/futon2/aif/meta_repair_coordinator.clj" nil]
   ["R" "/home/joe/code/futon2" "src/futon2/aif/full_loop_runtime.clj" #{"run-opportunity!"}]
   ["A" "/home/joe/code/futon3c" "src/futon3c/wm/meta_self_heal_adapters.clj" nil]
   ["S" "/home/joe/code/futon3c" "src/futon3c/wm/runner_service.clj" #{"meta-self-heal-preflight!"}]
   ["T" "/home/joe/code/futon2" "src/futon2/aif/meta_terminal_injury.clj" nil]])

(defn head [repo] (str/trim (:out (clojure.java.shell/sh "git" "-C" repo "rev-parse" "--short=9" "HEAD"))))

(defn committed [repo file]
  ;; Read the file as committed at HEAD, never the working tree: the sha the report
  ;; names must be the content it read, and another lane may be mid-edit.
  (:out (clojure.java.shell/sh "git" "-C" repo "show" (str "HEAD:" file))))

(defn forms [text]
  (e/parse-string-all text {:all true :row-key :line :col-key :col :end-row-key :end-line
                                    :auto-resolve name :readers (fn [_] identity)}))

(defn classification-site [tag top-name line kw]
  {:tag tag :fn top-name :line line
   :kind :classification :reason kw})

(defn keyword-results-of-cond [form]
  (when (and (seq? form) (= 'cond (first form)))
    (keep (fn [[test result]]
            (when (keyword? result)
              [(if (= test :else)
                 (:end-line (meta form))
                 (or (:line (meta test)) (:line (meta form)))) result]))
          (partition 2 (rest form)))))

(defn find-binding [form binding]
  (letfn [(walk [x]
            (cond
              (vector? x) (or (some (fn [[k v]] (when (= binding k) v))
                                    (partition 2 x))
                           (some walk x))
              (coll? x) (some walk (if (map? x) (mapcat identity x) x))))]
    (walk form)))

(defn classification-sites [tag top-name form]
  ;; These are the only three classifier sources.  Do not collect the later
  ;; :observation-class case: it re-labels these outcomes, rather than deciding
  ;; one, and would duplicate every classification edge.
  (case top-name
    "explicit-non-injury-kinds"
    (for [m (tree-seq coll? #(if (map? %) (mapcat identity %) %) form)
          :when (map? m)
          [idx [k v]] (map-indexed vector m)
          :when (and (keyword? k) (keyword? v) (not= k :abstained))]
      ;; Edamame records location metadata on the map, not its atomic entries.
      ;; This fixed source map has one key/value entry per subsequent line.
      (classification-site tag top-name (+ (:line (meta m)) idx) v))

    "abstention-reason"
    (for [x (tree-seq coll? #(if (map? %) (mapcat identity %) %) form)
          [line kw] (keyword-results-of-cond x)]
      (classification-site tag top-name line kw))

    "classify"
    (for [x (tree-seq coll? #(if (map? %) (mapcat identity %) %)
                      (find-binding form 'non-injury-reason))
          :when (and (seq? x) (= 'when (first x)) (keyword? (last x)))]
      (classification-site tag top-name (:end-line (meta x)) (last x)))

    []))

(defn sites-in [tag top-name form]
  (let [out (transient [])]
    (letfn [(walk [x]
              (let [line (:line (meta x))]
                (cond
                  (and (seq? x) (#{'refusal 'held!} (first x)) (keyword? (second x)))
                  (conj! out {:tag tag :fn top-name :line line
                              :kind (if (= 'refusal (first x)) :refusal :held)
                              :reason (second x)})
                  ;; absence terms: (conj :term) inside a cond-> of missing terms
                  (and (seq? x) (= 'conj (first x)) (keyword? (second x)) (= 2 (count x)))
                  (conj! out {:tag tag :fn top-name :line line :kind :absent-term :reason (second x)})
                  (and (map? x) (contains? x :status))
                  (conj! out (cond-> {:tag tag :fn top-name :line line :end-line (:end-line (meta x)) :kind :status-map
                                      :status (if (keyword? (:status x)) (:status x) :computed)}
                               (keyword? (:reason x)) (assoc :reason (:reason x)))))
                (when (coll? x) (run! walk (if (map? x) (mapcat identity x) x)))))]
      (walk form))
    (into (persistent! out) (classification-sites tag top-name form))))

(defn top-name [form]
  (when (and (seq? form) (symbol? (first form)) (str/starts-with? (name (first form)) "def"))
    (str (second form))))

(let [all (for [[tag repo file only] sources
                :let [sha (head repo)]
                form (forms (committed repo file))
                :let [n (top-name form)]
                :when (and n (or (nil? only) (only n)))
                s (cons {:tag tag :fn n :line (:line (meta form)) :kind :defn}
                        (sites-in tag n form))]
            (assoc s :file file :sha sha
                   :id (str tag ":" (:line s) (when (:reason s) (str ":" (name (:reason s)))))))]
  (println (json/generate-string (vec (sort-by (juxt :tag :line) (distinct all))))))
