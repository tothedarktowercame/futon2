;; Enumerate the self-heal route's outcome sites in the Clojure code, mechanically.
;; A site is one place a function decides an outcome:
;;   (refusal :reason ...)            kind :refusal
;;   (held! :reason ...)              kind :held
;;   a map literal with a :status key       kind :status-map (status :computed when not a literal)
;;   (conj :term), as in a cond-> of absent terms   kind :absent-term
;; Usage: bb code_sites.clj  -> JSON array of sites on stdout.
(require '[cheshire.core :as json] '[edamame.core :as e] '[clojure.java.io :as io] '[clojure.string :as str])

(def sources
  ;; [tag repo file only-fns]  only-fns nil = whole file
  [["P" "/home/joe/code/futon2" "src/futon2/aif/meta_self_heal_production.clj" nil]
   ["C" "/home/joe/code/futon2" "src/futon2/aif/meta_repair_coordinator.clj" nil]
   ["R" "/home/joe/code/futon2" "src/futon2/aif/full_loop_runtime.clj" #{"run-opportunity!"}]
   ["A" "/home/joe/code/futon3c" "src/futon3c/wm/meta_self_heal_adapters.clj" nil]
   ["S" "/home/joe/code/futon3c" "src/futon3c/wm/runner_service.clj" #{"meta-self-heal-preflight!"}]])

(defn head [repo] (str/trim (:out (clojure.java.shell/sh "git" "-C" repo "rev-parse" "--short=9" "HEAD"))))

(defn forms [path]
  (e/parse-string-all (slurp path) {:all true :row-key :line :col-key :col
                                    :auto-resolve name :readers (fn [_] identity)}))

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
                  (conj! out (cond-> {:tag tag :fn top-name :line line :kind :status-map
                                      :status (if (keyword? (:status x)) (:status x) :computed)}
                               (keyword? (:reason x)) (assoc :reason (:reason x)))))
                (when (coll? x) (run! walk (if (map? x) (mapcat identity x) x)))))]
      (walk form))
    (persistent! out)))

(defn top-name [form]
  (when (and (seq? form) (symbol? (first form)) (str/starts-with? (name (first form)) "def"))
    (str (second form))))

(let [all (for [[tag repo file only] sources
                :let [sha (head repo)]
                form (forms (str repo "/" file))
                :let [n (top-name form)]
                :when (and n (or (nil? only) (only n)))
                s (sites-in tag n form)]
            (assoc s :file file :sha sha
                   :id (str tag ":" (:line s) (when (:reason s) (str ":" (name (:reason s)))))))]
  (println (json/generate-string (vec (sort-by (juxt :tag :line) (distinct all))))))
