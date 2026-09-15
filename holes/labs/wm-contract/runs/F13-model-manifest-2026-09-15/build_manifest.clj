(require '[cheshire.core :as json]
         '[clojure.edn :as edn]
         '[clojure.pprint :as pp]
         '[clojure.string :as str]
         '[futon2.aif.cascade-model-manifest :as m])
(def run-dir "holes/labs/wm-contract/runs/F13-model-manifest-2026-09-15/")
(def constructions (json/parse-string (slurp (str run-dir "construction-comparison.json")) true))
(def comparisons
  (mapv (fn [r]
          (let [text (slurp (:path r))
                _ (assert (= (:sha256 r) (m/sha256 text)) "Construction mission source changed")
                target (m/extract-target (:mission r) (:path r) text)
                patterns (mapv (fn [[id]]
                                 (let [path (str "/home/joe/code/futon3/library/" id ".flexiarg")]
                                   (m/interpret-pattern id path (slurp path))))
                               (get-in r [:result :cascade]))]
            {:target target :patterns patterns
             :constructed-count (count patterns)
             :interpretable-count (count (filter #(= :interpreted (get-in % [:guard :status])) patterns))}))
        constructions))
(def chosen (first (sort-by (juxt (comp - :interpretable-count) #(get-in % [:target :id]))
                                   (filter #(get-in % [:target :ok]) comparisons))))
(assert chosen "No admissible q0: stop")
(def grounded-path "/home/joe/code/futon6/data/pattern_posteriors.grounded.json")
(def self-path "/home/joe/code/futon3a/resources/notions/pattern_posteriors.self_graded.json")
(def grounded (json/parse-string (slurp grounded-path)))
(def self-graded (get (json/parse-string (slurp self-path)) "patterns"))
(def history
  {:kind :pattern-success-history-availability
   :authority :inspection-only
   :sources [(m/source grounded-path (slurp grounded-path)) (m/source self-path (slurp self-path))]
   :qualification "Aggregate pattern posteriors exist; these rows are not mission-conditioned success statistics or stochastic transition warrants. No stochastic rows implemented."
   :patterns (mapv (fn [{:keys [id]}]
                     {:id id :grounded-aggregate (or (get grounded id) (get grounded (last (str/split id #"/"))))
                      :self-graded (select-keys (get self-graded id) ["label" "n" "alpha" "beta"])
                      :mission-conditioned {:status :missing :reason :not-established-by-aggregate-tables}})
                   (:patterns chosen))})
(def manifest
  (-> (m/build-manifest (:target chosen) (:patterns chosen))
      (assoc :comparison comparisons
             :target-choice {:rule :most-interpretable-firing-patterns :tie-break :canonical-mission-id
                             :scope :existing-three-mission-allow-list}
             :construction-source (m/source (str run-dir "construction-comparison.json")
                                             (slurp (str run-dir "construction-comparison.json")))
             :implementation-source (m/source "src/futon2/aif/cascade_model_manifest.clj"
                                               (slurp "src/futon2/aif/cascade_model_manifest.clj"))
             :ruling {:authority :owner-dispatch :author "claude-20"
                      :reference "invoke-1789457902962-21000-0d50b14a"})
      (assoc-in [:preference :source] (m/source "src/futon2/aif/ruled_outcome_c.clj"
                                                (slurp "src/futon2/aif/ruled_outcome_c.clj")))
      (update :findings conj history
              {:kind :documented-observation-validator-contract-unreviewed
               :action :separate-reviewed-change :bypassed? false})))
(assert (= manifest (edn/read-string (pr-str manifest))))
(assert (m/normalized-exact? (get-in manifest [:initial-belief :mass])))
(spit (str run-dir "partial-manifest.edn") (with-out-str (pp/pprint manifest)))
(println "selected" (get-in manifest [:target :id]))
(doseq [r comparisons]
  (println (get-in r [:target :id]) "HAVE" (count (get-in r [:target :have]))
           "constructed" (:constructed-count r) "interpretable" (:interpretable-count r)))
(println "findings" (mapv :kind (:findings manifest)))
