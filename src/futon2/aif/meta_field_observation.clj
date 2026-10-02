(ns futon2.aif.meta-field-observation
  "Pure/replayable META field observation over an authoritative registry read.

  M/E/T lifecycle decisions remain owned by mission-registry and
  outer-task-selection.  This namespace preserves their rows and reasons; it
  does not parse status prose or infer completion.  Algorithm rows require a
  separately pinned, explicit approval catalog."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.mission-registry :as registry]
            [futon2.aif.outer-task-selection :as outer])
  (:import [java.security MessageDigest]))

(def schema :wm/meta-field-observation-v1)
(def algorithm-catalog-schema :wm/approved-algorithm-catalog-v1)
(def default-algorithm-catalog
  "/home/joe/code/futon2/data/wm-approved-algorithms.edn")

(defn sha256 [^bytes bytes]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256") bytes)]
    (apply str (map #(format "%02x" (bit-and % 0xff)) digest))))

(defn- canonical [x]
  (cond
    (map? x) (into (sorted-map) (map (fn [[k v]] [k (canonical v)])) x)
    (set? x) (mapv canonical (sort-by pr-str x))
    (sequential? x) (mapv canonical x)
    :else x))

(defn digest [x]
  (sha256 (.getBytes (pr-str (canonical x)) "UTF-8")))

(defn- pin? [x]
  (and (map? x) (string? (:path x))
       (boolean (re-matches #"[0-9a-f]{64}" (:sha256 x)))))

(defn- task-row [kind row]
  {:id (:id row) :kind kind :source (:source row)})

(defn- registry-material
  [{:keys [missions tickets excursions]}]
  (let [all (concat (map #(assoc % :kind :mission) (:missions missions))
                    (map #(assoc % :kind :excursion) (:excursions excursions))
                    (map #(assoc % :kind :ticket) (:tickets tickets)))
        lifecycle-live? (fn [{:keys [kind] :as row}]
                          (case kind
                            :mission (registry/live-mission? row)
                            :excursion (registry/live-excursion? row)
                            :ticket (registry/live-ticket? row)
                            false))
        lifecycle-live (filterv lifecycle-live? all)
        outer-receipt (outer/select-task {:tasks lifecycle-live :seed 0})
        proposed-support (mapv #(task-row (:kind %) %) (:support outer-receipt))
        requisition-exclusions
        (mapv #(merge (task-row (:kind %) %)
                      (select-keys % [:ineligible-reason :ineligibility-evidence]))
              (:excluded outer-receipt))
        lifecycle-exclusions
        (->> all
             (remove lifecycle-live?)
             (mapv (fn [row]
                     (assoc (task-row (:kind row) row)
                            :ineligible-reason :registry/lifecycle-ineligible
                            :ineligibility-evidence
                            {:status-class (:status-class row)
                             :status-line (:status-line row)
                             :source (:source row)}))))
        registry-exclusions (vec (concat lifecycle-exclusions requisition-exclusions))
        source-unavailable (->> (concat proposed-support registry-exclusions)
                                (remove #(pin? (:source %)))
                                (mapv (fn [row]
                                        {:id (:id row) :kind (:kind row)
                                         :ineligible-reason :registry/source-unavailable
                                         :ineligibility-evidence
                                         {:retained-source (:source row)
                                          :registry-disposition
                                          (select-keys row [:ineligible-reason
                                                            :ineligibility-evidence])}})))
        unavailable-ids (set (map (juxt :kind :id) source-unavailable))]
    {:support (filterv #(not (unavailable-ids [(:kind %) (:id %)])) proposed-support)
     :exclusions (vec (concat
                       (remove #(unavailable-ids [(:kind %) (:id %)]) registry-exclusions)
                       source-unavailable))
     :registry-total (count all)}))

(defn- refusal [reason details]
  {:schema schema :status :refused :reason reason :details details})

(defn- algorithm-rows
  [{:keys [path read-bytes]}]
  (if-not path
    {:rows []
     :exclusions [{:kind :algorithm :ineligible-reason :algorithm/catalog-unavailable
                   :ineligibility-evidence
                   {:expected-path default-algorithm-catalog
                    :authority "holes/E-wm-algorithms.md: Approved registry remains unchecked"}}]}
    (try
      (let [bytes (read-bytes path)
            pin {:path path :sha256 (sha256 bytes)}
            catalog (edn/read-string (String. bytes "UTF-8"))
            entries (:entries catalog)
            duplicate-ids (->> entries (map :id) frequencies
                               (keep (fn [[id n]] (when (> n 1) id))) vec)]
        (cond
          (not= algorithm-catalog-schema (:schema catalog))
          (refusal :algorithm-catalog-invalid {:error :schema :source pin})

          (seq duplicate-ids)
          (refusal :duplicate-identities {:kind :algorithm :ids duplicate-ids :source pin})

          :else
          (loop [remaining entries rows [] exclusions []]
            (if-let [entry (first remaining)]
              (let [source (:source entry)]
                (cond
                  (not= :approved (:status entry))
                  (recur (next remaining) rows
                         (conj exclusions
                               {:id (:id entry) :kind :algorithm
                                :ineligible-reason :algorithm/not-approved
                                :ineligibility-evidence {:catalog-source pin :entry entry}}))

                  (not (pin? source))
                  (refusal :algorithm-catalog-file-mismatch
                           {:id (:id entry) :error :source-pin-invalid :catalog-source pin})

                  :else
                  (let [actual (sha256 (read-bytes (:path source)))]
                    (if (not= actual (:sha256 source))
                      (refusal :source-drift
                               {:id (:id entry) :expected (:sha256 source)
                                :actual actual :path (:path source)})
                      (recur (next remaining)
                             (conj rows {:id (:id entry) :kind :algorithm
                                         :source source :approved true
                                         :catalog-source pin})
                             exclusions)))))
              {:rows rows :exclusions exclusions :catalog-source pin
               :catalog-proof
               {:entry-total (count entries)
                :entries (->> entries
                              (map #(select-keys % [:id :status :source]))
                              (sort-by :id) vec)}}))))
      (catch java.io.FileNotFoundException _
        {:rows []
         :exclusions [{:kind :algorithm :ineligible-reason :algorithm/catalog-unavailable
                       :ineligibility-evidence {:expected-path path}}]})
      (catch Throwable t
        (refusal :algorithm-catalog-unreadable
                 {:path path :message (ex-message t)})))))

(defn observe
  "Produce a pinned whole-field snapshot.

  Dependencies are explicit for replay. `:registry-snapshot` must be the one
  result of the authoritative loaders; no task source is reread here. Algorithm
  catalog and algorithm files are each read exactly once via `:read-bytes`."
  [{:keys [registry-snapshot catalog-path read-bytes]
    :or {read-bytes #(java.nio.file.Files/readAllBytes (.toPath (io/file %)))}}]
  (let [registry-snapshot
        (or registry-snapshot
            {:missions (registry/load-missions)
             :tickets (registry/load-tickets)
             :excursions (registry/load-excursions)})
        {:keys [support exclusions registry-total]} (registry-material registry-snapshot)
        task-ids (map :id (concat support exclusions))
        duplicates (->> task-ids frequencies
                        (keep (fn [[id n]] (when (> n 1) id))) vec)
        algorithms (algorithm-rows {:path catalog-path :read-bytes read-bytes})]
    (cond
      (seq duplicates)
      (refusal :duplicate-identities {:kind :task :ids duplicates})

      (= :refused (:status algorithms)) algorithms

      :else
      (let [rows (->> (concat support (:rows algorithms))
                      (sort-by (juxt :kind :id)) vec)
            exclusions (->> (concat exclusions (:exclusions algorithms))
                            (sort-by (juxt :kind #(or (:id %) "") :ineligible-reason)) vec)
            all-duplicate-ids (->> (concat rows (filter :id exclusions))
                                   (map :id) frequencies
                                   (keep (fn [[id n]] (when (> n 1) id))) vec)
            body (cond-> {:schema schema
                          :rows rows
                          :exclusions exclusions
                          :counts (merge (sorted-map :algorithm 0 :excursion 0
                                                    :mission 0 :ticket 0)
                                         (frequencies (map :kind rows)))
                          :registry-proof
                          {:observed-total registry-total
                           :accounted-total (+ (count support)
                                               (count (remove #(= :algorithm (:kind %)) exclusions)))
                           :all-task-identities
                           (vec (sort (map :id (concat support
                                                      (remove #(= :algorithm (:kind %)) exclusions)))))} }
                   (:catalog-source algorithms)
                   (assoc :algorithm-catalog-source (:catalog-source algorithms)
                          :algorithm-catalog-proof (:catalog-proof algorithms)))
            snapshot-sha (digest body)]
        (if (seq all-duplicate-ids)
          (refusal :duplicate-identities {:kind :meta-field :ids all-duplicate-ids})
          (assoc body :status :observed
                 :source-pin {:path "wm://meta-field-observation-v1"
                              :sha256 snapshot-sha}))))))

(defn verify
  "Verify immutable snapshot identity and that its registry accounting has no
  silent row loss. Intended for replay before policy construction."
  [observation]
  (let [expected (get-in observation [:source-pin :sha256])
        body (dissoc observation :status :source-pin)
        actual (digest body)
        proof (:registry-proof observation)
        rows (:rows observation)
        exclusions (:exclusions observation)
        ids (concat (map :id (remove #(= :algorithm (:kind %)) rows))
                    (map :id (remove #(= :algorithm (:kind %)) exclusions)))
        expected-counts (merge (sorted-map :algorithm 0 :excursion 0
                                           :mission 0 :ticket 0)
                               (frequencies (map :kind rows)))
        catalog-proof (:algorithm-catalog-proof observation)
        catalog-entries (:entries catalog-proof)
        algorithm-representations
        (concat (map #(assoc % :representation :admitted)
                     (filter #(= :algorithm (:kind %)) rows))
                (map #(assoc % :representation :excluded)
                     (filter #(and (= :algorithm (:kind %)) (:id %)) exclusions)))
        represented (frequencies (map :id algorithm-representations))
        catalog-status (into {} (map (juxt :id :status)) catalog-entries)
        representation-errors
        (->> algorithm-representations
             (keep (fn [{:keys [id representation ineligible-reason]}]
                     (let [status (get catalog-status id)]
                       (when (or (nil? status)
                                 (and (= :approved status) (not= :admitted representation))
                                 (and (not= :approved status)
                                      (not (and (= :excluded representation)
                                                (= :algorithm/not-approved ineligible-reason)))))
                         {:id id :catalog-status status
                          :representation representation
                          :ineligible-reason ineligible-reason}))))
             vec)
        duplicate-identities (->> (concat rows (filter :id exclusions))
                                  (map :id) frequencies
                                  (keep (fn [[id n]] (when (> n 1) id))) vec)]
    (cond
      (not= schema (:schema observation))
      (refusal :field-schema-invalid {:actual (:schema observation)})
      (not= expected actual)
      (refusal :source-drift {:expected expected :actual actual})
      (not= (:counts observation) expected-counts)
      (refusal :field-counts-mismatch
               {:declared (:counts observation) :actual expected-counts})
      (seq duplicate-identities)
      (refusal :duplicate-identities {:kind :meta-field :ids duplicate-identities})
      (not= (:observed-total proof) (:accounted-total proof) (count ids))
      (refusal :registry-row-dropped {:proof proof :actual-accounted (count ids)})
      (not= (:all-task-identities proof) (vec (sort ids)))
      (refusal :registry-identity-mismatch {:proof proof :actual (vec (sort ids))})
      (and (:algorithm-catalog-source observation)
           (or (not= (:entry-total catalog-proof) (count catalog-entries))
               (not= (set (map :id catalog-entries)) (set (keys represented)))
               (some #(not= 1 %) (vals represented))
               (seq representation-errors)))
      (refusal :algorithm-catalog-accounting-mismatch
               {:proof catalog-proof :represented represented
                :representation-errors representation-errors})
      :else {:schema schema :status :verified :source-pin (:source-pin observation)})))
