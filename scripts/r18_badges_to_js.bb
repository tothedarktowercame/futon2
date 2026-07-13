#!/usr/bin/env bb
;; r18_badges_to_js.bb — generate and validate the explainer's embedded R18
;; contract. file:// pages cannot fetch local files, so the HTML embeds the data.
;;
;;   bb scripts/r18_badges_to_js.bb --check  # CI: fail on drift
;;   bb scripts/r18_badges_to_js.bb --write  # patch both generated blocks
;;   bb scripts/r18_badges_to_js.bb          # print both generated blocks
(require '[cheshire.core :as json]
         '[clojure.edn :as edn]
         '[clojure.set :as set]
         '[clojure.string :as str])

(def data-path "data/r18-badges.edn")
(def html-path "holes/aif-wiring-explainer.html")

(defn json-quantity [[k v]]
  [(name k) (-> v
                (update :badge name)
                (select-keys [:badge :claims :cite :code-ref :computes :repair
                              :note :repair-built]))])

(defn generated-blocks [d]
  (let [qs (into (sorted-map) (map json-quantity (:quantities d)))
        node-map (into (sorted-map)
                       (for [[node {:keys [quantities]}] (:nodes d)
                             :when (seq quantities)]
                         [(name node) (mapv name quantities)]))]
    {:badges (str "const R18B=" (json/generate-string qs) ";")
     :mapping (str "const R18MAP=" (json/generate-string node-map) ";")}))

(defn validate! [d]
  (let [quantities (set (keys (:quantities d)))
        mapped (mapcat :quantities (vals (:nodes d)))
        mapped-set (set mapped)
        duplicate-mappings (->> mapped frequencies (keep (fn [[q n]] (when (> n 1) q))) set)
        unknown-requirements (->> (:nodes d) vals (keep :requirement)
                                  (remove (set (keys (:requirements d)))) set)]
    (when-not (= 2 (get-in d [:schema :version]))
      (throw (ex-info "unsupported R18 schema version" {:schema (:schema d)})))
    (when (seq (set/difference quantities mapped-set))
      (throw (ex-info "unmapped R18 quantities"
                      {:quantities (set/difference quantities mapped-set)})))
    (when (seq (set/difference mapped-set quantities))
      (throw (ex-info "node mapping names unknown R18 quantities"
                      {:quantities (set/difference mapped-set quantities)})))
    (when (seq duplicate-mappings)
      (throw (ex-info "R18 quantities mapped to multiple nodes"
                      {:quantities duplicate-mappings})))
    (when (seq unknown-requirements)
      (throw (ex-info "nodes name unknown requirements"
                      {:requirements unknown-requirements})))
    d))

(defn replace-generated [html begin-marker end-marker generated]
  (let [pattern (re-pattern (str "(?s)(" (java.util.regex.Pattern/quote begin-marker)
                                 ").*?(" (java.util.regex.Pattern/quote end-marker) ")"))]
    (when-not (re-find pattern html)
      (throw (ex-info "generated block markers missing"
                      {:begin begin-marker :end end-marker})))
    (str/replace html pattern (fn [[_ begin end]]
                                (str begin "\n" generated "\n" end)))))

(defn validate-html-nodes! [d html]
  (doseq [[node {:keys [requirement]}] (:nodes d)]
    (let [needle (str "{id:\"" (name node) "\", r:"
                      (if requirement (str "\"" requirement "\"") "null"))]
      (when-not (str/includes? html needle)
        (throw (ex-info "explainer node disagrees with canonical requirement map"
                        {:node node :requirement requirement :expected needle})))))
  html)

(let [d (validate! (edn/read-string (slurp data-path)))
      {:keys [badges mapping]} (generated-blocks d)
      rendered (str badges "\n" mapping)
      mode (first *command-line-args*)]
  (case mode
    "--write"
    (let [html (slurp html-path)
          html' (-> html
                    (replace-generated
                     "// ---- R18-BADGES-BEGIN ----"
                     "// ---- R18-BADGES-END ----"
                     badges)
                    (replace-generated
                     "// ---- R18-MAP-BEGIN ----"
                     "// ---- R18-MAP-END ----"
                     mapping))
          _ (validate-html-nodes! d html')]
      (spit html-path html')
      (println "updated" html-path))

    "--check"
    (let [html (slurp html-path)]
      (validate-html-nodes! d html)
      (when-not (and (str/includes? html badges)
                     (str/includes? html mapping))
        (throw (ex-info "explainer R18 blocks have drifted; run --write" {})))
      (println "R18 contract is coherent:" (count (:quantities d)) "quantities,"
               (count (:nodes d)) "nodes"))

    nil (println rendered)
    (throw (ex-info "usage: r18_badges_to_js.bb [--check|--write]"
                    {:argument mode}))))
