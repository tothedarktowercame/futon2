(ns futon2.aif.occurrence-index
  "Stable occurrence identities follow the retained reading order. Sorting is
  presentation only: it must not renumber authored square or decision keys.")

(defn indexed-fragments [analysis]
  (mapv vector (range)
        (mapcat (fn [sentence]
                  (map #(assoc % :sentence-id (:id sentence))
                       (:fragments sentence)))
                (:sentences analysis))))
