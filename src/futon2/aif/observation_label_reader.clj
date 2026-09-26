(ns futon2.aif.observation-label-reader
  "A-S Revision 3 §2: one immutable, active-mechanism population for rates."
  (:require [clojure.string :as str]
            [futon2.aif.observation-label-store :as store]))

(def minimum 5)
(def prior {:alpha 1/2 :beta 1/2 :authority "A-S §2 (Jeffreys), Revision 3"})

(defn- active-key? [mechanism-sha key]
  ;; Include the separator: an identity that merely ends in another identity
  ;; is a different population, too.
  (let [mechanism (nth key 5 nil)]
    (and (string? mechanism) (str/ends-with? mechanism (str "@" mechanism-sha)))))

(defn rates-inputs
  "Pure projection of a store/snapshot. Only the active mechanism contributes.
   Both reference cells need five distinct labels; excluded classes contribute
   neither labels nor subjects. Coverage counts seen keys, including refusals.
   Records are returned unchanged. Duplicate record keys refuse the population."
  [{:keys [envelope sha256]} {:keys [mechanism-sha] :as identities}]
  (cond
    (:status identities) identities
    (or (not (string? mechanism-sha)) (str/blank? mechanism-sha))
    {:status :missing :kind :mechanism-identity-required}
    :else
    (let [records (vals (:labels envelope))
          duplicates (->> records (map :label-key) frequencies
                          (keep (fn [[k n]] (when (> n 1) k)))
                          (sort-by pr-str) vec)]
      (if (seq duplicates)
        {:status :missing :kind :duplicate-label-key :keys duplicates}
        (let [active? #(active-key? mechanism-sha %)
              current (group-by :token-class (filter #(active? (:label-key %)) records))
              obsolete (group-by :token-class (remove #(active? (:label-key %)) records))
              seen (group-by first (filter active? (keys (:seen envelope))))
              classes (sort-by str (into (set (keys seen)) (keys current)))
              initial {:labels [] :subjects {} :prior prior
                       :excluded (mapv (fn [c] {:class c :excluded :obsolete-mechanism
                                                :counts {:labels (count (get obsolete c))}})
                                       (sort-by str (keys obsolete)))
                       :snapshot-sha256 sha256 :identities identities :minimum minimum}]
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
