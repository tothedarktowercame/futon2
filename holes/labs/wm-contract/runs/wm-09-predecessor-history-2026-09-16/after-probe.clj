(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[futon2.aif.receipt-construction :as construction]
         '[futon2.aif.receipt-construction-test :as fixture])
(import '[java.nio.file Files] '[java.nio.file.attribute FileAttribute])

(defn outcome [f]
  (try (let [x (f)] {:admission-reason (:admission-reason x)
                    :status (get-in x [:provenance :status])
                    :epoch (get-in x [:provenance :identity :semantic-epoch])})
       (catch Exception e {:exception (.getName (class e)) :data (ex-data e)})))

(defn scenario [label mutation]
  (let [temp (.toFile (Files/createTempDirectory "wm09-predecessor-probe" (make-array FileAttribute 0)))
        older (io/file temp "older") newer (io/file temp "newer")]
    (try
      (fixture/history-fixture! older :older "2026-09-15T10:00:00Z" "2026-09-15T10:01:00Z")
      (let [latest (fixture/history-fixture! newer :newer "2026-09-15T11:00:00Z" "2026-09-15T11:01:00Z")
            current (assoc-in (:identity latest) [:occurrence :action-at] "2026-09-15T12:00:00Z")]
        (mutation latest)
        {:case label :result (outcome #(construction/previous! current [older newer]))})
      (finally (doseq [f (reverse (file-seq temp))] (Files/delete (.toPath f)))))))

(defn change-file! [file f]
  (spit file (pr-str (f (edn/read-string (slurp file))))))

(def results
  (mapv (fn [[label mutation]] (scenario label mutation))
        [[:valid-modern (fn [_])]
         [:carrier-removed #(change-file! (:construction-file %) (fn [x] (update-in x [:payload :judgment] dissoc :receipted-construction)))]
         [:carrier-malformed #(change-file! (:construction-file %) (fn [x] (assoc-in x [:payload :judgment :receipted-construction] {})))]
         [:cascade-removed #(change-file! (:construction-file %) (fn [x] (update-in x [:payload :judgment] dissoc :cascade)))]
         [:construction-missing #(Files/delete (.toPath (:construction-file %)))]
         [:manifest-missing #(change-file! (:close-file %) (fn [x] (update x :payload dissoc :close-evidence-manifest)))]
         [:occurrence-missing #(change-file! (:close-file %) (fn [x] (update-in x [:payload :close-retention] dissoc :occurrence)))]
         [:carrier-and-manifest-missing
          (fn [x]
            (change-file! (:construction-file x) #(update-in % [:payload :judgment] dissoc :receipted-construction))
            (change-file! (:close-file x) #(update % :payload dissoc :close-evidence-manifest)))]]))

(assert (= :previous-cascade-carrier-unavailable (get-in results [1 :result :data :construction/refusal])))
(assert (= :previous-cascade-unavailable (get-in results [3 :result :data :construction/refusal])))
(assert (= :previous-cascade-unavailable (get-in results [4 :result :data :construction/refusal])))
(doseq [result results] (prn result))
(let [temp (.toFile (Files/createTempDirectory "wm09-no-history" (make-array FileAttribute 0)))]
  (try
    (prn {:case :no-earlier-target :result (outcome #(construction/previous! {:occurrence {:action/value {:target "M-history"} :action-at "2026-09-15T12:00:00Z"}} [temp]))})
    (finally (Files/delete (.toPath temp)))))
(prn {:scope :after-repair-author-control :production-edits true :independent-review-pending true})
