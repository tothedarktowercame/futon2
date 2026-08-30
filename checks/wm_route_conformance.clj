#!/usr/bin/env bb

(ns checks.wm-route-conformance
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def control-map-path "/home/joe/code/p4ng/empirics-futon/control-map-edges.edn")

(defn- today-date-string []
  (str (java.time.LocalDate/now (java.time.ZoneId/of "UTC"))))

(defn- default-record-path []
  (str "holes/labs/wm-contract/tick-run-record-" (today-date-string) ".edn"))

(defn- node->str [node]
  (cond
    (keyword? node) (subs (str node) 1)
    (symbol? node) (str node)
    (string? node) node
    :else (str node)))

(defn- drawn-edge-pairs []
  (->> (:edges (edn/read-string (slurp control-map-path)))
       (filter #(= :drawn (:status %)))
       (map (fn [{:keys [from to]}]
              [(node->str from) (node->str to)]))
       set))

(defn- route-verdict [route]
  (let [drawn (drawn-edge-pairs)
        classified (mapv (fn [hop]
                           (assoc hop :drawn?
                                  (contains? drawn
                                             [(:fromNode hop) (:toNode hop)])))
                         route)
        conformant (filterv :drawn? classified)
        unmapped (mapv #(dissoc % :drawn?) (remove :drawn? classified))]
    {:hops (count route)
     :conformant (count conformant)
     :unmapped (count unmapped)
     :drawn-edges-fired (count (set (map (juxt :fromNode :toNode) conformant)))
     :drawn-edges-total (count drawn)
     :unmapped-hops unmapped}))

(defn- fail! [message]
  (println message)
  (System/exit 1))

(defn -main [& args]
  (let [negative? (some #{"--negative"} args)
        path (or (some #(when (not= "--negative" %) %) args)
                 (default-record-path))
        receipt (if negative?
                  {:route []}
                  (edn/read-string (slurp (io/file path))))
        verdict (route-verdict (:route receipt))
        line (str "route: hops=" (:hops verdict)
                  " conformant=" (:conformant verdict)
                  " unmapped=" (:unmapped verdict)
                  " | drawn-edges-fired="
                  (:drawn-edges-fired verdict) "/" (:drawn-edges-total verdict))]
    (println line)
    (when (zero? (:hops verdict))
      (fail! "wm-route-conformance: FAIL empty-route"))
    (when (and negative? (pos? (:hops verdict)))
      (fail! "wm-route-conformance: FAIL negative-control")))
  (System/exit 0))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
