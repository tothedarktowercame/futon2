;; claude-3 reviewer controls for 7e738d53. Short-lived process, temporary stores only.
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[futon2.aif.receipt-construction :as construction]
         '[futon2.aif.receipt-construction-test :as fixture]
         '[futon2.aif.evidence-manifest :as manifest])
(import '[java.nio.file Files] '[java.nio.file.attribute FileAttribute])

(defn outcome [f]
  (try (let [x (f)] {:admission-reason (:admission-reason x)
                    :status (get-in x [:provenance :status])
                    :epoch (get-in x [:provenance :identity :semantic-epoch])})
       (catch Throwable e {:exception (.getName (class e)) :data (ex-data e)})))

(defn change-file! [file f] (spit file (pr-str (f (edn/read-string (slurp file))))))

(defn reseal! [{:keys [close-file]}]
  ;; Rebuild the close manifest over the same entries so file digests agree again.
  (change-file! close-file
                (fn [close]
                  (let [m (get-in close [:payload :close-evidence-manifest])]
                    (assoc-in close [:payload :close-evidence-manifest]
                              (manifest/build-manifest
                               {:entries (mapv #(select-keys % [:evidence/id :source-path :admitted-at]) (:entries m))
                                :read-bytes #(Files/readAllBytes (.toPath (io/file %)))}))))))

(defn scenario [label with-older? mutation]
  (let [temp (.toFile (Files/createTempDirectory "c3-wm09-review" (make-array FileAttribute 0)))
        older (io/file temp "older") newer (io/file temp "newer")]
    (try
      (when with-older? (fixture/history-fixture! older :older "2026-09-15T10:00:00Z" "2026-09-15T10:01:00Z"))
      (let [latest (fixture/history-fixture! newer :newer "2026-09-15T11:00:00Z" "2026-09-15T11:01:00Z")
            current (assoc-in (:identity latest) [:occurrence :action-at] "2026-09-15T12:00:00Z")]
        (mutation latest)
        {:case label :older-present? with-older? :result (outcome #(construction/previous! current [older newer]))})
      (finally (doseq [f (reverse (file-seq temp))] (Files/delete (.toPath f)))))))

(def drop-carrier #(update-in % [:payload :judgment] dissoc :receipted-construction))
(def drop-cascade #(update-in % [:payload :judgment] dissoc :cascade))

(def results
  [(scenario :control-valid-alone false (fn [_]))
   (scenario :resealed-control-unchanged true reseal!)
   (scenario :resealed-carrier-removed true #(do (change-file! (:construction-file %) drop-carrier) (reseal! %)))
   (scenario :resealed-carrier-malformed true #(do (change-file! (:construction-file %) (fn [x] (assoc-in x [:payload :judgment :receipted-construction] {}))) (reseal! %)))
   (scenario :resealed-cascade-removed true #(do (change-file! (:construction-file %) drop-cascade) (reseal! %)))
   (scenario :carrier-removed-no-older false #(change-file! (:construction-file %) drop-carrier))
   (scenario :cascade-removed-no-older false #(change-file! (:construction-file %) drop-cascade))
   (scenario :construction-missing-no-older false #(Files/delete (.toPath (:construction-file %))))
   (scenario :close-unreadable true #(spit (:close-file %) "{:recorded-at"))
   (scenario :close-recorded-at-missing true #(change-file! (:close-file %) (fn [x] (dissoc x :recorded-at))))])

(doseq [r results] (prn r))
