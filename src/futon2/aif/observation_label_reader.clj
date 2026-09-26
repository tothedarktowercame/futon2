(ns futon2.aif.observation-label-reader
  "A-S Revision 3 §2: one immutable, active-mechanism population for rates."
  (:require [clojure.string :as str]
            [futon2.aif.observation-label-store :as store]
            [futon2.aif.observation-checks :as checks]))

(def minimum 5)
(def prior {:alpha 1/2 :beta 1/2 :authority "A-S §2 (Jeffreys), Revision 3"})

(defn- key-population [descriptors key]
  (let [mechanism (nth key 5 nil)
        {:keys [mechanism-name mechanism-sha]} (get descriptors (first key))]
    (cond
      (not (string? mechanism)) :unwitnessed
      (= mechanism (str mechanism-name "@" mechanism-sha)) :current
      (str/ends-with? mechanism (str "@" mechanism-sha)) :mechanism-mismatch
      :else :obsolete-mechanism)))

(defn rates-inputs
  "Project one store snapshot against the loaded dispatch descriptors.
   Only the active mechanism contributes.
   Both reference cells need five distinct labels; excluded classes contribute
   neither labels nor subjects. Coverage counts seen keys, including refusals.
   Records are returned unchanged. Duplicate record keys refuse the population."
  [{:keys [envelope sha256]} {:keys [mechanism-sha] :as identities}]
  (cond
    (:status identities) identities
    (or (not (string? mechanism-sha)) (str/blank? mechanism-sha))
    {:status :missing :kind :mechanism-identity-required}
    :else
    (let [descriptors (into {} (map (fn [c] [c (checks/loaded-check c)]) (keys checks/checks)))
          descriptor-failure (some :status (vals descriptors))
          records (vals (:labels envelope))
          duplicates (->> records (map :label-key) frequencies
                          (keep (fn [[k n]] (when (> n 1) k)))
                          (sort-by pr-str) vec)]
      (cond
        descriptor-failure (first (filter :status (vals descriptors)))
        (some #(not= mechanism-sha (:mechanism-sha %)) (vals descriptors))
        {:status :missing :kind :code-identity-mismatch :identities identities}
        (seq duplicates)
        {:status :missing :kind :duplicate-label-key :keys duplicates}
        :else
        (let [population #(key-population descriptors %)
              active? #(= :current (population %))
              current (group-by :token-class (filter #(active? (:label-key %)) records))
              obsolete (group-by :token-class (filter #(= :obsolete-mechanism (population (:label-key %))) records))
              mismatched (group-by (juxt :token-class #(nth (:label-key %) 5))
                                   (filter #(= :mechanism-mismatch (population (:label-key %))) records))
              seen (group-by first (filter active? (keys (:seen envelope))))
              classes (sort-by str (into (set (keys seen)) (keys current)))
              initial {:labels [] :subjects {} :prior prior
                       :excluded (mapv (fn [c] {:class c :excluded :obsolete-mechanism
                                                :counts {:labels (count (get obsolete c))}})
                                       (sort-by str (keys obsolete)))
                       :snapshot-sha256 sha256 :identities identities :minimum minimum}
              initial (update initial :excluded into
                              (mapv (fn [[[c mechanism] labels]]
                                      {:class c :excluded :mechanism-mismatch
                                       :counts {:labels (count labels)} :mechanism mechanism})
                                    (sort-by (comp pr-str key) mismatched)))]
          (reduce
           (fn [out c]
             (let [labels (sort-by (comp pr-str :label-key) (get current c))
                   counts {:present (count (filter #(= :present (:admitted %)) labels))
                           :absent (count (filter #(= :absent (:admitted %)) labels))}
                   excluded (cond
                              (some zero? (vals counts)) :one-cell-unobserved
                              (some #(< % minimum) (vals counts)) :below-minimum)]
               (if excluded
                 (update out :excluded conj {:class c :excluded excluded :counts counts})
                 (-> out
                     (update :labels into labels)
                     (assoc-in [:subjects c] (count (get seen c)))))))
           initial classes))))))

(defn read-rates-inputs
  "Read exactly one validated snapshot. Store failures remain typed failures,
   never an empty population; this function does not initialize the store."
  [path identities]
  (try
    (rates-inputs (store/snapshot path) identities)
    (catch clojure.lang.ExceptionInfo e (ex-data e))))
