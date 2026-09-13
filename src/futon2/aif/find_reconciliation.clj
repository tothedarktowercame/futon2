(ns futon2.aif.find-reconciliation
  "Pure comparison of F11 snapshots by scenario, round and receipt identity."
  (:require [clojure.set :as set]))

(defn- indexed [rows key-fn]
  (group-by key-fn rows))

(defn- differences [kind pin live]
  (let [pk (set (keys pin)) lk (set (keys live))]
    (vec (concat
          (for [id (sort-by pr-str (set/difference pk lk))]
            {:kind kind :identity id :problem :missing-live})
          (for [id (sort-by pr-str (set/difference lk pk))]
            {:kind kind :identity id :problem :missing-pin})
          (for [[side entries] [[:pin pin] [:live live]]
                [id rows] (sort-by (comp pr-str key) entries)
                :when (> (count rows) 1)]
            {:kind kind :identity id :problem :duplicate :side side})))))

(defn- snapshot-index [snapshot]
  (let [scenarios (:scenarios snapshot)
        rounds (for [s scenarios r (:round-results s)]
                 {:identity [[(:treatment s) (:disposition s)] (:round r)]
                  :find (:find r)})
        receipts (for [{:keys [identity find]} rounds [id receipt] (:receipts find)]
                   {:identity (conj identity id) :receipt receipt})]
    {:scenarios (indexed scenarios (juxt :treatment :disposition))
     :rounds (indexed rounds :identity)
     :receipts (indexed receipts :identity)}))

(defn- comparison [pin live]
  (let [p (snapshot-index pin) l (snapshot-index live)
        structural (vec (mapcat #(differences % (get p %) (get l %))
                               [:scenarios :rounds :receipts]))
        common (set/intersection (set (keys (:receipts p)))
                                 (set (keys (:receipts l))))]
    {:structural-differences structural
     :pairs (vec (for [[scenario round pattern :as id] (sort-by pr-str common)
                       :let [ps (get-in p [:receipts id]) ls (get-in l [:receipts id])]
                       :when (= 1 (count ps) (count ls))]
                   {:scenario scenario :round round :pattern pattern
                    :pinned (:receipt (first ps)) :live (:receipt (first ls))}))}))

(defn classify
  "Which fields of a receipt pair differ.  `:if-lines`/`:however-lines` are
   coordinates into the flexiarg; `:if-text`/`:however-text` are the clause
   itself.  F2 is about the clause, so the two must be counted apart."
  [{:keys [pinned live]}]
  (let [wp (:warrant pinned) wl (:warrant live)
        keys* (sort (distinct (concat (keys wp) (keys wl))))]
    (into (sorted-set)
          (concat (for [k (sort (distinct (concat (keys pinned) (keys live))))
                        :when (and (not= k :warrant) (not= (get pinned k) (get live k)))]
                    k)
                  (for [k keys* :when (not= (get wp k) (get wl k))]
                    (keyword "warrant" (name k)))))))

(defn report [pin live]
  (let [{:keys [pairs structural-differences]} (comparison pin live)
        diffs (remove (comp empty? classify) pairs)
        fields (frequencies (mapcat classify diffs))
        line-fields #{:warrant/if-lines :warrant/however-lines}
        text-fields #{:warrant/if-text :warrant/however-text :warrant/file :route}
        rounds (fn [xs] (count (distinct (map (juxt :scenario :round) xs))))
        all-rounds (rounds pairs)]
    (sorted-map
     :as-of-pin (:as-of pin)
     :as-of-live (:as-of live)
     :repository-count-pin (count (:repository pin))
     :repository-count-live (count (:repository live))
     :repository-added (vec (sort (remove (set (:repository pin)) (:repository live))))
     :repository-removed (vec (sort (remove (set (:repository live)) (:repository pin))))
     :drift-mismatch-count-live (:mismatch-count (:drift live))
     :laws-identical? (= (:laws pin) (:laws live))
     :structural-differences structural-differences
     :receipt-populations-identical? (empty? structural-differences)
     :receipts-compared (count pairs)
     :receipts-differing (count diffs)
     :rounds-total all-rounds
     :rounds-differing (rounds diffs)
     :differing-fields (into (sorted-map) fields)
     ;; The finding, stated so it can be false: every difference is a line
     ;; coordinate and no difference is a clause text, a warrant file or a
     ;; retrieval route.
     :difference-is-line-coordinates-only?
     (and (empty? structural-differences)
          (boolean (seq diffs))
          (every? #(every? line-fields (classify %)) diffs)
          (zero? (reduce + 0 (map #(get fields % 0) text-fields))))
     :shifted-patterns
     (into (sorted-map)
           (for [[id ps] (group-by :pattern diffs)
                 :let [p (first ps)]]
             [id (sorted-map
                  :if-lines-pin (get-in p [:pinned :warrant :if-lines])
                  :if-lines-live (get-in p [:live :warrant :if-lines])
                  :however-lines-pin (get-in p [:pinned :warrant :however-lines])
                  :however-lines-live (get-in p [:live :warrant :however-lines])
                  :if-text-identical? (= (get-in p [:pinned :warrant :if-text])
                                         (get-in p [:live :warrant :if-text]))
                  :however-text-identical? (= (get-in p [:pinned :warrant :however-text])
                                              (get-in p [:live :warrant :however-text])))])))))

