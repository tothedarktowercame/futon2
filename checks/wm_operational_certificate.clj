#!/usr/bin/env bb
(ns checks.wm-operational-certificate
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]))

(def topology-svg "/home/joe/code/p4ng/aif-control-map-paper.svg")
(def topology-data "/home/joe/code/p4ng/empirics-futon/control-map-edges.edn")
(def expected-svg-sha256 "568938d213c23c79f42a01a36547ced622deec64a3591f847c3e72a234b538ec")
(def expected-data-sha256 "64485bb0165fe4abdaf799b59853c05efb0e09fbaf293bf9511d91e5098f509d")

(defn sha256-bytes [^bytes bs]
  (let [d (java.security.MessageDigest/getInstance "SHA-256")]
    (format "%064x" (java.math.BigInteger. 1 (.digest d bs)))))
(defn file-sha256 [path] (sha256-bytes (java.nio.file.Files/readAllBytes (.toPath (io/file path)))))
(defn node-str [x] (if (keyword? x) (name x) (str x)))
(defn pair [x] [(node-str (:from x)) (node-str (:to x))])
(defn hop-pair [x] [(node-str (:fromNode x)) (node-str (:toNode x))])

(defn topology []
  (let [m (edn/read-string (slurp topology-data))
        original (set (map pair (filter #(= :drawn (:status %)) (:edges m))))
        measured (set (map pair (:route-measured-drawn m)))]
    {:original original :measured measured :all (into original measured)}))

(defn resource-clean? [r]
  (and (= :clean (:status r))
       (zero? (long (or (:pids-events-max-delta r) -1)))
       (false? (:native-thread-exhaustion r))
       (= 0 (:command-exit r))))

(defn certificate [run-bytes resource negative?]
  (let [run (edn/read-string (String. run-bytes "UTF-8"))
        route (cond-> (vec (:route run))
                negative? (conj {:fromNode "R99" :toNode "R100"
                                 :via "negative-control/undeclared-hop"}))
        {:keys [original measured all]} (topology)
        traversed (mapv (fn [hop]
                          (let [p (hop-pair hop)]
                            (assoc hop :topology/layer
                                   (cond (contains? original p) :original
                                         (contains? measured p) :measured
                                         :else :undeclared)))) route)
        traversed-pairs (set (map hop-pair traversed))
        undeclared (filterv #(= :undeclared (:topology/layer %)) traversed)
        svg-hash (file-sha256 topology-svg)
        data-hash (file-sha256 topology-data)
        topology-pinned? (and (= expected-svg-sha256 svg-hash)
                              (= expected-data-sha256 data-hash))
        route-present? (seq route)
        run-time? (string? (:startedAt run))
        resources-clean? (resource-clean? resource)
        pass? (boolean (and route-present? run-time? topology-pinned?
                            (empty? undeclared) resources-clean?))]
    {:certificate/schema 1
     :certificate/generated-at (str (java.time.Instant/now))
     :run {:id (or (:run/id run) (sha256-bytes run-bytes))
           :identity-kind (if (:run/id run) :tick-run-record-id :content-sha256)
           :started-at (:startedAt run)}
     :topology {:artifact "p4ng/aif-control-map-paper.svg"
                :content-sha256 svg-hash :expected-sha256 expected-svg-sha256
                :edge-data "p4ng/empirics-futon/control-map-edges.edn"
                :edge-data-sha256 data-hash :edge-data-expected-sha256 expected-data-sha256
                :pin-valid? topology-pinned?}
     :traversal {:hops traversed
                 :counts {:total (count traversed)
                          :original (count (filter #(= :original (:topology/layer %)) traversed))
                          :measured (count (filter #(= :measured (:topology/layer %)) traversed))
                          :undeclared (count undeclared)}
                 :undeclared-hops undeclared
                 :declared-not-exercised
                 {:original (vec (sort (remove traversed-pairs original)))
                  :measured (vec (sort (remove traversed-pairs measured)))}}
     :resource-status resource
     :checks {:run-identity-present? (boolean (or (:run/id run) (seq run-bytes)))
              :run-timestamp-present? run-time?
              :route-present? (boolean route-present?)
              :topology-pin-valid? topology-pinned?
              :no-undeclared-traversal? (empty? undeclared)
              :resource-status-clean? resources-clean?}
     :verdict (if pass? :pass :fail)}))

(defn parse-args [args]
  (loop [xs args out {}]
    (if-let [x (first xs)]
      (if (= x "--negative") (recur (rest xs) (assoc out :negative? true))
          (recur (nnext xs) (assoc out (keyword (subs x 2)) (second xs))))
      out)))

(defn main [args]
  (let [{:keys [run resource negative?] output-path :certificate} (parse-args args)
        run-bytes (java.nio.file.Files/readAllBytes (.toPath (io/file run)))
        resource-data (when resource (edn/read-string (slurp resource)))
        cert (certificate run-bytes resource-data negative?)]
    (when output-path
      (io/make-parents output-path)
      (spit output-path (with-out-str (pprint/pprint cert))))
    (println (pr-str (dissoc cert :traversal)))
    (if negative?
      (if (= :fail (:verdict cert))
        (do (println "wm-operational-certificate: PASS undeclared-hop mutation produced failing certificate") 0)
        (do (println "wm-operational-certificate: FAIL mutation certified") 2))
      (if (= :pass (:verdict cert))
        (do (println "wm-operational-certificate: PASS") 0)
        (do (println "wm-operational-certificate: FAIL certificate written") 1)))))

(when (= *file* (System/getProperty "babashka.file"))
  (System/exit (main *command-line-args*)))
