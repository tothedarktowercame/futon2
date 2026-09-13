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

(defn certificate
  "The certificate the original repair-attempt-001 submission promised and
   never supplied (SPEC-run-certificate-v1.md, review-rejected 98d0dcb as
   documentation-only).  A compact, schema-versioned verdict over one
   reconciliation report, carrying the digests that make it checkable
   against the exact records compared.  Fail-closed: refuses a report that
   lacks the identity fields rather than emitting an unverifiable verdict."
  [report {:keys [run-id generated-at]}]
  (let [required [:pin-sha256 :live-sha256 :pin-path :live-path]
        missing (vec (remove #(seq (str (get report %))) required))]
    (when (seq missing)
      (throw (ex-info "reconciliation report lacks certificate identity fields"
                      {:error :certificate/unverifiable-report
                       :missing missing})))
    (when-not (seq (str run-id))
      (throw (ex-info "certificate requires a run identity"
                      {:error :certificate/run-id-missing})))
    (sorted-map
     :schema :wm/f2-reconciliation-certificate-v1
     :certificate/run-id run-id
     :certificate/generated-at generated-at
     :pin-path (:pin-path report)
     :pin-sha256 (:pin-sha256 report)
     :live-path (:live-path report)
     :live-sha256 (:live-sha256 report)
     :as-of-pin (:as-of-pin report)
     :as-of-live (:as-of-live report)
     :receipts-compared (:receipts-compared report)
     :receipts-differing (:receipts-differing report)
     :structural-difference-count (count (:structural-differences report))
     :laws-identical? (boolean (:laws-identical? report))
     ;; The verdict, stated so it can be false: the pinned and live records
     ;; carry identical receipt populations, no receipt differs, and the
     ;; laws are byte-identical.
     :records-reconcile?
     (and (true? (:receipt-populations-identical? report))
          (zero? (or (:receipts-differing report) -1))
          (true? (:laws-identical? report))))))

(def ^:private certificate-volatile-fields
  "Identity fields that legitimately differ between two certifications of
   the same records: when it ran and under which run identity."
  #{:certificate/generated-at :certificate/run-id})

(defn certificate-drift
  "Field-level drift between a committed certificate and a recomputed one,
   ignoring only the volatile identity fields.  Empty means the committed
   certificate still describes exactly what recomputation observes; any
   entry names the field, the committed value, and the recomputed value."
  [committed recomputed]
  (vec (for [k (sort (distinct (concat (keys committed) (keys recomputed))))
             :when (and (not (certificate-volatile-fields k))
                        (not= (get committed k) (get recomputed k)))]
         {:field k :committed (get committed k) :recomputed (get recomputed k)})))

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

