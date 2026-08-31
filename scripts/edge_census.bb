#!/usr/bin/env bb
;; edge_census.bb — Phase 1 of the 08-31 build plan: reconcile every account of
;; the wiring into ONE table with provenance per edge.
;;
;; Four sources, none of which agreed before this ran:
;;   drawn     p4ng/empirics-futon/control-map-edges.edn :edges          (what Figure 4 claims)
;;   derived   p4ng/empirics-futon/control-map-edges.edn :derived-undrawn (what the theory implies)
;;   measured  futon2/holes/problems/PREREG-war-machine.md §2f            (what the machine did)
;;   specified p4ng/empirics-futon/hyper-edge-schema.edn :instances       (what is typed)
;;   sim       futon2/holes/labs/wm-contract/sim/R*-claim.edn             (what role-played nodes asked for)
(require '[clojure.java.io :as io]
         '[clojure.edn :as edn]
         '[clojure.pprint :as pprint]
         '[clojure.string :as str])

(def P "/home/joe/code/p4ng/empirics-futon/")
(def F2 "/home/joe/code/futon2/")

(def cml (edn/read-string (slurp (str P "control-map-edges.edn"))))
(def hyper (edn/read-string (slurp (str P "hyper-edge-schema.edn"))))

(def drawn   (set (map (juxt :from :to) (:edges cml))))
(def derived (set (map (juxt :from :to) (:derived-undrawn cml))))

;; Three conformant WM-RUN2 hops are in Figure 4's original edge population.
;; The measured overlay routes live in a separate sibling key even though the
;; figure now displays them distinguishably; source populations must not merge.
(def measured-drawn
  [{:edge [:R7 :R3] :measurement :wm-run2 :note "approx" :on-map? true}
   {:edge [:R5 :R6] :measurement :wm-run2 :note nil :on-map? true}
   {:edge [:R8 :R5] :measurement :wm-run2 :note "region" :on-map? true}])
(def measured-routes
  (mapv (fn [{:keys [from to basis] :as route}]
          {:edge [from to]
           :measurement (:measurement route)
           :note basis
           :via (:via route)
           :receipt (:receipt route)
           :on-map? true
           :figure-layer :measured-route})
        (:route-measured-drawn cml)))
(def measured (vec (concat measured-drawn measured-routes)))
(def measured-set (set (map :edge measured)))

;; typed instances: real deliveries = consecutive :out -> :in port pairs (ports are in route
;; order). NOT the member cross-product -- that invents edges the instance never claims.
(defn instance-deliveries [i]
  (let [ps (:ports i)]
    (->> (partition 2 1 ps)
         (keep (fn [[a b]]
                 (when (and (= :out (:direction a)) (= :in (:direction b)))
                   [(:owner a) (:owner b)]))))))
(defn node-kw [o] (when (str/starts-with? (str o) ":node/") (keyword (subs (str o) 6))))
(def specified-raw
  (->> (:instances hyper)
       (filter #(str/starts-with? (str (:edge/id %)) ":control-map/"))
       (mapcat instance-deliveries)
       set))
(def specified
  (->> specified-raw (keep (fn [[a b]] (let [x (node-kw a) y (node-kw b)] (when (and x y) [x y])))) set))
(def specified-nonnode
  (->> specified-raw (remove (fn [[a b]] (and (node-kw a) (node-kw b)))) vec))

;; node-sim: {:to :RN} means "I need something FROM :to", so the edge runs :to -> node
(def sim
  (->> (file-seq (io/file (str F2 "holes/labs/wm-contract/sim")))
       (filter #(re-find #"-claim\.edn$" (.getName %)))
       (mapcat (fn [f] (let [c (edn/read-string (slurp f))]
                         (map (fn [m] [(:to m) (:node c)]) (:missing-edges c)))))
       set))

(def all-edges (sort-by (juxt (comp str first) (comp str second))
                        (into #{} (concat drawn derived measured-set specified sim))))

(defn row [[a b]]
  {:edge [a b]
   :drawn?     (contains? drawn [a b])
   :derived?   (contains? derived [a b])
   :measured?  (contains? measured-set [a b])
   :specified? (contains? specified [a b])
   :sim?       (contains? sim [a b])
   :schema?    (boolean (some #(and (= a (:from %)) (= b (:to %)) (:schema %)) (:edges cml)))})

(def rows (map row all-edges))
(defn n [pred] (count (filter pred rows)))

(def out
  {:as-of "2026-08-31"
   :generated-by "futon2/scripts/edge_census.bb"
   :sources (array-map
             :drawn (count drawn)
             :derived (count derived)
             :wm-run2 (count (filter #(= :wm-run2 (:measurement %)) measured))
             :measured-drawn (count measured-drawn)
             :route-measured-drawn (count measured-routes)
             :pairing-decomposition (count (filter #(= :pairing-decomposition (:measurement %)) measured))
             :measured-union (count measured-set)
             :specified (count specified)
             :specified-to-nonnode (count specified-nonnode)
             :sim (count sim))
   :totals {:distinct-edges (count rows)
            :drawn-with-schema (n :schema?)
            :attested-by-2+ (n #(< 1 (count (filter true? [(:drawn? %) (:derived? %) (:measured? %) (:sim? %)]))))
            :measured-but-unlisted (n #(and (:measured? %) (not (:drawn? %)) (not (:derived? %))))
            :drawn-only (n #(and (:drawn? %) (not (:derived? %)) (not (:measured? %)) (not (:sim? %))))}
   :specified-nonnode specified-nonnode
   :measured-notes measured
   :rows (vec rows)})

(spit (str F2 "holes/labs/wm-contract/edge-census.edn") (with-out-str (pprint/pprint out)))
(println "distinct edges across all sources:" (count rows))
(doseq [[k v] (:sources out)] (println (format "  %-11s %s" (name k) v)))
(println)
(doseq [[k v] (:totals out)] (println (format "  %-24s %s" (name k) v)))
