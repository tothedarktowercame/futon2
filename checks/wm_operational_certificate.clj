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

(defn run-identity [run-bytes run]
  (if-let [run-id (:run/id run)]
    {:id run-id :identity-kind :recorded-run-id}
    {:id (sha256-bytes run-bytes) :identity-kind :content-sha256-fallback}))

(defn certificate-matches-run? [run-bytes certificate]
  (let [run (edn/read-string (String. run-bytes "UTF-8"))]
    (= (run-identity run-bytes run)
       (select-keys (:run certificate) [:id :identity-kind]))))

(defn certificate [run-bytes resource negative?]
  (let [run (edn/read-string (String. run-bytes "UTF-8"))
        route (cond-> (vec (:route run))
                negative? (conj {:fromNode "R99" :toNode "R100"
                                 :via "negative-control/undeclared-hop"}))
        {:keys [original measured]} (topology)
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
        identity (run-identity run-bytes run)
        pass? (boolean (and route-present? run-time? topology-pinned?
                            (empty? undeclared) resources-clean?))]
    {:certificate/schema 1
     :certificate/generated-at (str (java.time.Instant/now))
     :run (assoc identity :started-at (:startedAt run))
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
     :checks {:run-identity-present? (boolean (:id identity))
              :run-identity-source (:identity-kind identity)
              :run-timestamp-present? run-time?
              :route-present? (boolean route-present?)
              :topology-pin-valid? topology-pinned?
              :no-undeclared-traversal? (empty? undeclared)
              :resource-status-clean? resources-clean?}
     :verdict (if pass? :pass :fail)}))

(def boolean-flags #{"--negative" "--negative-run-id"})

(defn parse-args [args]
  (loop [xs args out {}]
    (if-let [x (first xs)]
      (if (boolean-flags x) (recur (rest xs) (assoc out (keyword (subs x 2)) true))
          (recur (nnext xs) (assoc out (keyword (subs x 2)) (second xs))))
      out)))

(defn main [args]
  (let [{:keys [run resource negative? negative-run-id] output-path :certificate} (parse-args args)
        run-bytes (java.nio.file.Files/readAllBytes (.toPath (io/file run)))
        resource-data (when resource (edn/read-string (slurp resource)))
        generated (certificate run-bytes resource-data negative?)
        cert (cond-> generated
               negative-run-id (assoc-in [:run :id] "mutation/not-the-recorded-run"))
        identity-matches? (certificate-matches-run? run-bytes cert)]
    (when output-path
      (io/make-parents output-path)
      (spit output-path (with-out-str (pprint/pprint cert))))
    (println (pr-str (dissoc cert :traversal)))
    (cond
      negative-run-id
      (if-not identity-matches?
        (do (println "wm-operational-certificate: PASS run-id mismatch rejected") 0)
        (do (println "wm-operational-certificate: FAIL run-id mismatch certified") 2))

      negative?
      (if (= :fail (:verdict cert))
        (do (println "wm-operational-certificate: PASS undeclared-hop mutation produced failing certificate") 0)
        (do (println "wm-operational-certificate: FAIL mutation certified") 2))

      (and (= :pass (:verdict cert)) identity-matches?)
        (do (println "wm-operational-certificate: PASS") 0)
      :else
      (do (println "wm-operational-certificate: FAIL certificate written") 1))))

(when (= *file* (System/getProperty "babashka.file"))
  (System/exit (main *command-line-args*)))
