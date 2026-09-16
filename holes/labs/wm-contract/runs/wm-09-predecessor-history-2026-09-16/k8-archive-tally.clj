(require '[clojure.java.io :as io] '[clojure.edn :as edn]
         '[futon2.aif.receipt-construction :as construction]
         '[futon2.aif.interpretation-evidence :as evidence]
         '[futon2.aif.evidence-manifest :as manifest])
(import '[java.nio.file Files])
(let [[root pinned output] *command-line-args*]
  (load-file pinned)
  (let [discover @#'construction/discover-closes!
        candidate @#'construction/closed-candidate!
        relevance @#'construction/relevance-evidence!
        admit @#'construction/validated-previous!
        coverage (discover [root])
        files (:files coverage)
        fresh "__wm09_diagnostic_fresh_target__"
        read-bytes #(Files/readAllBytes (.toPath (io/file %)))
        ;; Only rebind a manifest if every source is already in the verified
        ;; bounded copy and its literal digest matches. Otherwise stop safely.
        relocation
        (mapv (fn [path]
                (let [closed (edn/read-string (slurp path))
                      m (get-in closed [:payload :close-evidence-manifest])]
                  (if-not m {:close path :manifest :absent}
                    (let [entries (mapv (fn [e]
                                          (let [original (:source-path e)
                                                prefix "/home/joe/code/futon2/data/wm-full-loop/"
                                                _ (assert (.startsWith original prefix) (str "unavailable binding " original))
                                                dest (str root "/" (subs original (count prefix)))]
                                            (assert (.isFile (io/file dest)) (str "not in bounded copy " original))
                                            (assert (= (:sha256 e) (evidence/sha256 (read-bytes dest))))
                                            (assoc e :source-path dest))) (:entries m))
                          rebuilt (manifest/build-manifest {:entries entries :read-bytes read-bytes})]
                      (spit path (pr-str (assoc-in closed [:payload :close-evidence-manifest] rebuilt)))
                      {:close path :manifest :copy-path-rebound :verified-entries (count entries)})))) files)
        attempt (fn [f]
                  (try {:status :passed :value (f)}
                       (catch clojure.lang.ExceptionInfo e
                         {:status :refused :data (ex-data e)})))
        rows (mapv (fn [path]
                     (let [parsed (attempt #(candidate (io/file path)))]
                       (if (= :refused (:status parsed))
                         {:path path :phase :candidate-discovery :target-independent true :result parsed}
                         (let [c (:value parsed)]
                           (if (:non-construction? c)
                             {:path path :phase :non-construction :target-independent true
                              :status :excluded :provenance (:exclusion c)}
                             (do
                               (assert (not= fresh (:target c)))
                               {:path path :phase :target-relative :recorded-target (:target c)
                                :fresh-target {:requested-target fresh :result (attempt #(relevance c))}
                                :matching-target {:requested-target (:target c)
                                                  :result (attempt #(admit c [root]))}})))))) files)
        result {:source "858aa7f7" :scope :bounded-isolated-archive-diagnostic
                :coverage coverage :discovered (count files) :relocation relocation
                :rows rows :phase-counts (frequencies (map :phase rows))
                :fresh-result-counts (frequencies (keep #(get-in % [:fresh-target :result :status]) rows))
                :matching-result-counts (frequencies (keep #(get-in % [:matching-target :result :status]) rows))}]
    (assert (= 24 (count files)))
    (spit output (pr-str result))
    (prn (select-keys result [:discovered :phase-counts :fresh-result-counts :matching-result-counts]))))
(shutdown-agents)
