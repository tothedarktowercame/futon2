#!/usr/bin/env bb
;; promote_c_entries.bb — R19-PROOF-JOIN steps 1+2 (E-C-vector-live §11; Joe-ratified 2026-07-02).
;;
;; Promotes the live belly (futon2.aif.c-vector, ~455 C-entries) from sim-only
;; in-memory state to first-class substrate-2 entities on :7071, plus the
;; PROOF-layer relations:
;;   step 1: one `c-entry` ENTITY per C-entry (name-keyed, idempotent upsert;
;;           full EDN fidelity in :props :entry-edn + flat convenience props);
;;   step 2: `:discharged-by` RELATIONS c-entry → method/<class> (the 62 entries
;;           that carry one today: centre-mess / adopt-redirect / preserve-coherence),
;;           with the 3 method/class entities ensured;
;;   step 2b: `:outcome-ref` RELATIONS c-entry → CANONICAL mission node
;;           (<repo>-d/mission/<stem>, resolved via the 472 mission/doc entities)
;;           for mission-shaped refs — what makes the join graph-queryable and
;;           lets §11 step 3 reach the 177 mined-moves on the same nodes.
;;
;; Conventions per futon1a/README-conventions.md: entity :name IS the canonical
;; identifier; relations point src/dst by name. Writes go through POST
;; /api/alpha/entity | /relation (run-write! pipeline, x-penholder: api).
;; DRY-RUN by default; pass --execute to write. Idempotent: re-run upserts the
;; same names. Steps 3-5 of §11 (mined-move join, forward-model read, reconcile)
;; are NOT this script — see the excursion doc.
(require '[babashka.classpath :refer [add-classpath]])
(add-classpath (str (System/getProperty "user.home") "/code/futon2/src"))
(require '[futon2.aif.c-vector :as cv]
         '[babashka.http-client :as http]
         '[cheshire.core :as json]
         '[clojure.edn :as edn]
         '[clojure.string :as str])

(def base "http://localhost:7071/api/alpha")
(def headers {"Content-Type" "application/json" "x-penholder" "api"})
(def execute? (some #{"--execute"} *command-line-args*))

(defn- short-hash [x]
  (format "%08x" (bit-and (hash (pr-str x)) 0xffffffff)))

(defn- fetch-mission-index
  "canonical mission/doc entities → {stem canonical-name}. stem of
   futon3-d/mission/weird-modernism = weird-modernism; M-x resolves via x."
  []
  (let [resp (http/get (str base "/entities/latest?type=mission%2Fdoc&limit=2000")
                       {:headers {"Accept" "application/edn"} :timeout 20000})
        es (:entities (edn/read-string (:body resp)))]
    (into {} (keep (fn [e]
                     (let [n (:name e)
                           stem (peek (str/split (str n) #"/"))]
                       (when (and n (str/includes? (str n) "/mission/"))
                         [stem n]))))
          es)))

(defn- mission-stem [s]
  (when s (-> (str s) (str/replace #"^(mission/)?M-" "") (str/replace #"^.*?/mission/" ""))))

(defn- entry-slug
  "Deterministic, human-readable canonical name for one C-entry."
  [{:keys [flavour outcome-ref provenance]}]
  (let [f (name flavour)]
    (case flavour
      :stated (str "c-entry/stated/" (name (:kind outcome-ref)) "/" (:id outcome-ref))
      :incompleteness (str "c-entry/incompleteness/mission/" (:mission outcome-ref))
      :mess (str "c-entry/mess/"
                 (or (:mission outcome-ref) (:mission provenance)
                     (some-> (:scope outcome-ref) name) "scope"))
      (:reach :correction) (str "c-entry/" f "/"
                                (or (:derived-from provenance)
                                    (str (:referent outcome-ref) "/" (short-hash outcome-ref))))
      (str "c-entry/" f "/" (short-hash outcome-ref)))))

(defn- uniquify [entries]
  (let [slugs (map entry-slug entries)
        dups (set (keep (fn [[s n]] (when (> n 1) s)) (frequencies slugs)))]
    (map (fn [e s]
           (if (dups s) [(str s "/" (short-hash e)) e] [s e]))
         entries slugs)))

(defn- mission-ref-of
  "The canonical mission node this entry's outcome-ref points at, if mission-shaped."
  [{:keys [outcome-ref]} mission-index]
  (some-> (or (:mission outcome-ref) (:referent outcome-ref))
          mission-stem
          mission-index))

(defn- post! [path payload]
  (let [resp (http/post (str base path)
                        {:headers headers :timeout 20000 :throw false
                         :body (json/generate-string payload)})]
    (when-not (<= 200 (:status resp) 299)
      (throw (ex-info (str "write failed " path " " (:status resp))
                      {:payload payload :body (subs (str (:body resp)) 0 (min 300 (count (str (:body resp)))))})))
    (:status resp)))

(defn -main []
  (cv/maybe-refresh!)
  (let [entries (cv/current-c-vector)
        _ (assert (seq entries) "belly is empty — refusing to promote nothing")
        mission-index (fetch-mission-index)
        named (uniquify entries)
        derived-at (str (java.time.Instant/now))
        methods (into #{} (keep :discharged-by entries))
        rel-db (for [[slug e] named :when (:discharged-by e)]
                 {:type "discharged-by" :src slug :dst (str "method/" (name (:discharged-by e)))
                  :props {:label "discharged-by" :source "c-vector-promote"}})
        rel-or (for [[slug e] named :let [m (mission-ref-of e mission-index)] :when m]
                 {:type "outcome-ref" :src slug :dst m
                  :props {:label "outcome-ref" :source "c-vector-promote"}})
        unresolved (count (for [[_ e] named
                                :let [raw (or (get-in e [:outcome-ref :mission])
                                              (get-in e [:outcome-ref :referent]))]
                                :when (and raw (nil? (mission-ref-of e mission-index)))]
                            raw))]
    (println (str (if execute? "EXECUTE" "DRY-RUN")
                  " · entries " (count entries)
                  " · unique names " (count (into #{} (map first named)))
                  " · discharged-by rels " (count rel-db)
                  " · outcome-ref rels " (count rel-or)
                  " · unresolved mission refs " unresolved
                  " · methods " (vec methods)))
    (println "name samples:" (vec (take 3 (map first named))) "…"
             (vec (take 2 (drop 200 (map first named)))))
    (when execute?
      ;; method/class entities first (relation dst targets)
      (doseq [m methods]
        (post! "/entity" {:name (str "method/" (name m)) :type "method/class"
                          :external-id (str "method/" (name m))
                          :source "c-vector-promote"
                          :props {:method-class (name m)}}))
      (doseq [[slug e] named]
        (post! "/entity"
               {:name slug :type "c-entry" :external-id slug
                :source "c-vector-promote"
                :props {:flavour (name (:flavour e))
                        :kind (some-> (get-in e [:outcome-ref :kind]) name)
                        :ref-id (str (or (get-in e [:outcome-ref :id])
                                         (get-in e [:outcome-ref :mission])
                                         (get-in e [:outcome-ref :referent])))
                        :status (name (:status e))
                        :weight (get-in e [:weight :value])
                        :derived-at derived-at
                        :entry-edn (pr-str e)}}))
      (doseq [r (concat rel-db rel-or)] (post! "/relation" r))
      (println "WROTE" (+ (count methods) (count named) (count rel-db) (count rel-or)) "docs."))
    (when-not execute?
      (println "(dry-run — pass --execute to write)"))))

(-main)
