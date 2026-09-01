#!/usr/bin/env bb

(ns checks.wm-route-conformance
  (:require [checks.mutable-read-set :as read-set]
            [clojure.edn :as edn]))

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

(defn- pair-set [entries]
  (set (map (fn [{:keys [from to]}]
              [(node->str from) (node->str to)]) entries)))

(defn- figure-wiring [control-map-text]
  (let [data (edn/read-string control-map-text)]
    {:original (pair-set (filter #(= :drawn (:status %)) (:edges data)))
     :measured (pair-set (:route-measured-drawn data))}))

(defn- route-verdict [route control-map-text]
  (let [{:keys [original measured]} (figure-wiring control-map-text)
        wired (into original measured)
        classified (mapv (fn [hop]
                           (let [pair [(:fromNode hop) (:toNode hop)]]
                             (assoc hop
                                    :wired? (contains? wired pair)
                                    :figure-layer (cond
                                                    (contains? original pair) :original
                                                    (contains? measured pair) :measured
                                                    :else :absent))))
                         route)
        conformant (filterv :wired? classified)
        unmapped (mapv #(dissoc % :wired?) (remove :wired? classified))]
    {:hops (count route)
     :conformant (count conformant)
     :unmapped (count unmapped)
     :original-fired (count (filter #(= :original (:figure-layer %)) conformant))
     :measured-fired (count (filter #(= :measured (:figure-layer %)) conformant))
     :original-total (count original)
     :measured-total (count measured)
     :unmapped-hops unmapped}))

(defn- fail! [message]
  (println message)
  (System/exit 1))

(defn -main [& args]
  (let [negative? (some #{"--negative"} args)
        path (or (some #(when (not= "--negative" %) %) args)
                 (default-record-path))
        observation (read-set/observe-files [control-map-path path])
        snapshot (try (read-set/require-claim! observation :content-current)
                      (catch Exception failure
                        (fail! (str "wm-route-conformance: UNAVAILABLE "
                                    (pr-str (ex-data failure))
                                    " exit-convention=0-pass/1-fail"))))
        control-map-text (:text (read-set/entry-by-path snapshot control-map-path))
        receipt (edn/read-string (:text (read-set/entry-by-path snapshot path)))
        receipt (if negative?
                  (update receipt :route conj
                          {:fromNode "R99" :toNode "R100"
                           :via "negative-control/unmapped-hop"})
                  receipt)
        verdict (route-verdict (:route receipt) control-map-text)
        line (str "route: hops=" (:hops verdict)
                  " conformant=" (:conformant verdict)
                  " unmapped=" (:unmapped verdict)
                  " | original-fired=" (:original-fired verdict) "/" (:original-total verdict)
                  " measured-fired=" (:measured-fired verdict) "/" (:measured-total verdict))]
    (println line)
    (if negative?
      (if (pos? (:unmapped verdict))
        (do (println "wm-route-conformance: PASS negative control rejected unmapped mutation exit-convention=0-pass/1-fail/2-mutation-slipped")
            (System/exit 0))
        (do (println "wm-route-conformance: FAIL negative mutation passed exit-convention=0-pass/1-fail/2-mutation-slipped")
            (System/exit 2)))
      (cond
        (zero? (:hops verdict))
        (fail! "wm-route-conformance: FAIL empty-route exit-convention=0-pass/1-fail")

        (pos? (:unmapped verdict))
        (fail! "wm-route-conformance: FAIL unmapped-hop exit-convention=0-pass/1-fail")

        :else
        (do (println "wm-route-conformance: PASS exit-convention=0-pass/1-fail")
            (System/exit 0))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
