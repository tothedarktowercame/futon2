#!/usr/bin/env bb
;; land_A_next_sorries.clj — land the A-next interface-sorries into substrate-2 as oriented
;; code/v05/sorry hyperedges (schema: A-next-sorry-hyperedge-schema.md). --dry-run by default;
;; --land POSTs through the gated futon1a pipeline (POST /api/alpha/{entity,relation}, x-penholder).
;; Retype: fetch-merge-upsert :sorry/facet :logged onto the 23 pre-existing logged sorries (NO clobber).
(require '[clojure.edn :as edn] '[clojure.string :as str] '[clojure.java.io :as io] '[babashka.fs :as fs])

(def LAND? (some #{"--land"} *command-line-args*))
(def LABS "/home/joe/code/futon2/holes/labs")

;; ---- substrate-2 node-id resolution (from the live-graph dumps) ----
(def node-ids (with-open [r (io/reader "/tmp/_nodeids.txt")] (into #{} (line-seq r))))
(defn strip-repo [id] (if-let [m (re-matches #"[a-z0-9]+-d/(.+)" id)] (second m) id))
(def by-tail (persistent! (reduce (fn [m id] (let [t (strip-repo id)] (if (contains? m t) m (assoc! m t id)))) (transient {}) node-ids)))
(defn resolve-ref [ref]
  (let [cands (re-seq #"[a-zA-Z0-9][a-zA-Z0-9._-]*(?:/[a-zA-Z0-9._<>!?*+=-]+)+" (str ref))]
    (some (fn [c] (cond (contains? node-ids c) c (contains? by-tail c) (by-tail c) :else nil)) cands)))

;; ---- load the 15 interface-sorries ----
(def files (sort (map str (fs/glob LABS "A-next-*/*-sorry-EMPIRICAL.edn"))))
(defn corpus-idx [txt] (some-> (re-find #"#(\d+)\b" txt) second))
(defn for-mission [sorry]
  (let [m (str (get-in sorry [:provenance :mission]))]
    (or (re-find #"[a-z0-9]+-d/mission/[^\" ]+" m)
        (when-let [x (re-find #"([a-z0-9]+)/holes/.*/M-([^/]+)\.md" m)]
          (str (nth x 1) "-d/mission/" (nth x 2)))
        m)))
(defn discharged? [h] (let [d (:discharged-by h) s (:status h)]
                        (and (not (#{:open :unverified :research} d)) (not (#{:open :unverified :research} s))
                             (not (and (string? d) (str/blank? d))) (some? (or d s)))))

(defn build [file]
  (let [txt (slurp file) sorry (edn/read-string txt)
        idx (corpus-idx txt)
        mission (for-mission sorry)
        repo (or (second (re-find #"([a-z0-9]+)-d/" (str mission))) "futon2")
        sslug (name (:id sorry))
        hxid (str "hx:code/v05/sorry:" repo "-d/sorry/" sslug)
        eps (:endpoints sorry)
        rels (for [e eps
                   :let [role (name (:role e)) node (resolve-ref (:ref e))]]
               {:role role :node node :ref (:ref e) :in-s2 (boolean node)})
        holes (:typed-holes sorry)
        complete? (and (seq holes) (every? discharged? holes))]
    {:hxid hxid :idx idx :mission mission :file file
     :want-sig (:want-signature sorry) :hungry (:hungry-for sorry)
     :cascade (get-in sorry [:provenance :cascade])
     :wiring (get-in sorry [:provenance :pairs-with-wiring])
     :completeness (if complete? :complete :partial)
     :props {:sorry/facet :interface :sorry/state (:state sorry) :sorry/for-mission mission
             :sorry/want-signature (:want-signature sorry) :sorry/hungry-for (:hungry-for sorry)
             :sorry/kind (:kind sorry) :sorry/cascade (get-in sorry [:provenance :cascade])
             :sorry/typed-holes holes :sorry/wiring (get-in sorry [:provenance :pairs-with-wiring])
             :sorry/completeness (if complete? :complete :partial) :sorry/corpus-index idx}
     :rels rels}))

(def built (map build files))

;; ---- the 23 pre-existing logged sorries → fetch-merge :sorry/facet :logged ----
(def existing (:hyperedges (edn/read-string (slurp "/tmp/_sorries.edn"))))
(def retypes (for [h existing] {:id (:hx/id h) :add {:sorry/facet :logged}
                                :kind (:sorry/kind (:hx/props h))}))

;; ---- report ----
(println (str "=== " (if LAND? "LAND" "DRY-RUN") " — A-next interface-sorries → substrate-2 ===\n"))
(doseq [b (sort-by :idx built)]
  (let [r (:rels b) res (count (filter :in-s2 r)) tot (count r)]
    (printf "#%s  %s\n" (or (:idx b) "?") (:hxid b))
    (printf "     want-signature: %s\n" (:want-sig b))
    (printf "     for-mission: %s   completeness: %s   cascade: %s\n"
            (:mission b) (name (:completeness b)) (pr-str (:cascade b)))
    (printf "     endpoints resolved to substrate-2 nodes: %d/%d\n" res tot)
    (doseq [rel r]
      (printf "       %-7s %s\n" (str (:role rel) ":")
              (if (:node rel) (:node rel) (str "⚠ UNRESOLVED — " (subs (str (:ref rel)) 0 (min 60 (count (str (:ref rel))))) "…"))))
    (println)))

(let [allrels (mapcat :rels built) res (count (filter :in-s2 allrels))]
  (printf "SUMMARY: %d sorry entities · %d relations (%d resolved to real nodes, %d unresolved/targets)\n"
          (count built) (count allrels) res (- (count allrels) res))
  (printf "         completeness: %d complete, %d partial\n"
          (count (filter #(= :complete (:completeness %)) built))
          (count (filter #(= :partial (:completeness %)) built)))
  (printf "RETYPE: %d pre-existing logged sorries get :sorry/facet :logged (fetch-merge, no clobber)\n" (count retypes))
  (println "        e.g." (pr-str (map :id (take 3 retypes)))))

(when LAND?
  (println "\n[LAND mode] would POST" (count built) "entities +" (count (mapcat :rels built))
           "relations to" (or (System/getenv "FUTON1A_BASE_URL") "http://localhost:7071")
           "with x-penholder — NOT wired to actually POST in this build (safety); add the HTTP calls after dry-run sign-off."))
