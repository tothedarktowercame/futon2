(require '[cheshire.core :as json] '[clojure.java.io :as io]
         '[futon2.aif.hermetic-repair-fixture :as hermetic]
         '[futon2.aif.learning-trial-ledger :as ledger]
         '[futon2.aif.load-identity :as identity]
         '[futon2.aif.pattern-graph-pin :as graph-pin]
         '[futon2.aif.target-policy-family :as fam]
         '[futon2.aif.target-reading-registry :as registry]
         '[futon2.aif.cascade-shape-g :as sg]
         '[futon2.aif.wm.family-selection :as sel])
(defn tmp [n] (.toFile (java.nio.file.Files/createTempDirectory n (make-array java.nio.file.attribute.FileAttribute 0))))
(def lab "holes/labs/wm-contract/mission-head-cascades-2026-09-30")
(def stems ["02-M-metric-harness" "03-M-distributed-proofreaders" "04-M-web-arxana-ui-improvements"
            "05-M-self-documenting-stack" "06-M-war-machine-aif-completion" "07-M-essays-diachronic-model"
            "08-M-value-creation-loop"])
(defn s [x] (if (vector? x) (str (first x) " @" (second x)) (str x)))
(hermetic/with-hermetic-stores
 (fn []
   (with-redefs [ledger/default-root (.getPath (tmp "ledger"))]
     (let [dir (tmp "post") gc (io/file dir "graph.json")
           _ (io/copy (io/file "/home/joe/code/storage/operator-turns/mined-pattern-graph.json") gc)
           pin (graph-pin/pin! gc)
           graph (:graph (graph-pin/load-pinned gc))
           root (io/file dir "registry")
           out
           (mapv
            (fn [stem]
              (let [req (json/parse-string (slurp (io/file lab (str stem ".request.json"))) true)
                    a (json/parse-string (slurp (io/file lab (str stem ".request.json.analysis.json"))) true)
                    t (get-in req [:task :target_id])
                    d (identity/sha256 (.getBytes (:source_text req) "UTF-8"))
                    _ (registry/publish! root {:target-id t :source-path (get-in req [:task :file_path])
                                               :excerpt-digest d :source-file-digest (get-in req [:task :content_sha256])
                                               :request req :analysis a :validator-version 1})
                    family (fam/policy-family {:reading (registry/current-reading root t d) :graph graph})
                    r (sel/select-over-families [family] {:beta 1 :enactment-fold nil :novelty-inputs {}})
                    by-id (into {} (map (juxt :policy-id identity)) (:ranked r))]
                {:target t :source_path (get-in req [:task :file_path]) :source_kind (get-in req [:task :source_kind])
                 :source_text (:source_text req)
                 :fragments (vec (map-indexed (fn [i f] {:index i :text (:text f) :relations (:relations f)
                                                          :refs (mapv :id (:pattern_refs f))
                                                          :rejections (mapv :id (:pattern_rejections f))})
                                              (mapcat :fragments (:sentences a))))
                 :reported (:reported-count family) :distinct (:distinct-count family)
                 :failures (:failures family) :retraction_params (get-in family [:provenance :retraction])
                 :policies
                 (mapv (fn [p]
                         (let [e (by-id (:policy-id p))
                               {:keys [units descent patterns]} (get-in e [:action :precedence :co-apply])
                               arr (get-in e [:action :arrangement])]
                           {:policy_id (:policy-id p) :kind (name (:kind p))
                            :units (mapv (fn [u] {:id (s u) :pattern (str (:pattern-id (patterns u)))
                                                  :fragment (when (vector? u) (second u))
                                                  :theta (str (:theta (patterns u)))
                                                  :theta_status (name (get-in (patterns u) [:theta-record :status] :unknown))}) units)
                            :descent (mapv (fn [[a b]] [(s a) (s b)]) descent)
                            :edges (mapv (fn [ed] {:from (s (:from ed)) :to (s (:to ed)) :kind (name (or (:kind ed) :precedes))}) (:edges arr))
                            :raw_edges (when (= :retraction (:kind p)) (get-in p [:cascade :edges]))
                            :roots (mapv s (remove (set (map second descent)) units))
                            :F (:f e) :G (:controller-score e) :g_terms (select-keys (get-in e [:certificate :g-terms]) [:risk :ambiguity :expected-information-gain])
                            :horizon (:horizon-steps e)}))
                       (:policies family))}))
            stems)]
       (spit "/tmp/claude-1/post/examples.json"
             (json/generate-string {:graph_pin (select-keys pin [:sha256 :digest :node-count :edge-count :pattern-id-count]) :pin_raw (dissoc pin :graph) :targets out} {:pretty true}))
       (println "wrote" (count out))))))
(shutdown-agents)
