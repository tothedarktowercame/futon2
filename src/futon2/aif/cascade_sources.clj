(ns futon2.aif.cascade-sources
  "Declared cascade sources for the production tick. Each file in
  resources/wm/cascade-sources/ declares one target: its fact and want tokens
  with locators, its interpreted patterns with receipts, its constructed
  candidates with construction receipts, and its context's beta.

  Facts are not declared true or false. They are observed on every load
  through futon2.aif.observation-checks (WM-04, P5):
  - observed present => true;
  - a valid check observing absence => false;
  - a refused check (no current warrant, no locator, unknown sha) => :unknown.
  Unknown is never false (D3).

  load-declared returns the source map futon2.aif.cascade-problems/assemble
  takes, plus :files (path and sha256 of every file read) and :observations
  (every check result or refusal), so the judgement can record exactly what
  the decision was built from."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.observation-checks :as oc]))

(def default-dir "resources/wm/cascade-sources")

(defn- refuse! [reason data]
  (throw (ex-info (str "cascade-sources: " (name reason))
                  (assoc data :error :invalid-cascade-source :reason reason))))

(defn- file-sha [f] (evidence/sha256 (java.nio.file.Files/readAllBytes (.toPath f))))

(defn- check-file! [path d]
  (when-not (= :wm/cascade-source-v1 (:schema d))
    (refuse! :schema {:path path :schema (:schema d)}))
  (doseq [k [:target :context :beta :facts :want :locators :patterns :interpretation-receipts :candidates]]
    (when-not (contains? d k) (refuse! :missing-key {:path path :key k})))
  (when-not (and (number? (get-in d [:beta :value])) (pos? (get-in d [:beta :value]))
                 (#{:declared :learned} (get-in d [:beta :status])))
    (refuse! :beta {:path path :beta (:beta d)}))
  d)

(defn- observe-facts
  "Fact tokens to true/false/:unknown through their locators."
  [facts locators]
  (let [located (into {} (for [f facts] [f (get locators f)]))
        {:keys [observed results refused]} (oc/observe located)]
    {:universe (into {} (for [f facts]
                          [f (cond (contains? observed f) true
                                   (contains? results f) false
                                   :else :unknown)]))
     :observations {:results results :refused refused}}))

(defn load-declared
  "Read every *.edn under DIR (default resources/wm/cascade-sources) and build
  cascade-problems sources. An empty or missing directory gives nil, so the
  caller records :none-supplied. A malformed file throws; it is never skipped."
  ([] (load-declared default-dir))
  ([dir]
   (let [files (->> (file-seq (io/file dir))
                    (filter #(.isFile %))
                    (filter #(.endsWith (.getName %) ".edn"))
                    (sort-by #(.getPath %)))]
     (when (seq files)
       (reduce
        (fn [acc f]
          (let [path (.getPath f)
                d (check-file! path (edn/read-string (slurp f)))
                t (:target d)
                {:keys [universe observations]} (observe-facts (:facts d) (:locators d))]
            (-> acc
                (assoc-in [:universes t] universe)
                (assoc-in [:wants t] (vec (:want d)))
                (assoc-in [:locators t] (:locators d))
                (assoc-in [:interpretations t] {:patterns (:patterns d)
                                                :receipts (:interpretation-receipts d)})
                (assoc-in [:candidates t] (vec (:candidates d)))
                (assoc-in [:beta-by-context (:context d)] {:beta (get-in d [:beta :value]) :status (get-in d [:beta :status])})
                (assoc-in [:context-by-target t] (:context d))
                (update :files (fnil conj []) {:path path :sha256 (file-sha f) :target t})
                (assoc-in [:observations t] observations))))
        {}
        files)))))

(defn with-context-fn
  "Add the :context-of function cascade-problems needs (it cannot live in data)."
  [sources]
  (when sources
    (assoc sources :context-of (fn [t] (get-in sources [:context-by-target t])))))
