#!/usr/bin/env bb
(ns checks.control-organization-check
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set])
  (:import [java.security MessageDigest]))

(def defaults
  {:organization "/home/joe/code/p4ng/empirics-futon/control-organization.edn"
   :stages "/home/joe/code/p4ng/empirics-futon/control-stages.edn"
   :edges "/home/joe/code/p4ng/empirics-futon/control-map-edges.edn"})

(def required-edge-fields
  [:from :to :from-column :to-column :column-relation :diagram-role :classification])

(defn sha256 [path]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                        (java.nio.file.Files/readAllBytes (.toPath (io/file path))))]
    (apply str (map #(format "%02x" (bit-and % 0xff)) digest))))

(defn parse-args [args]
  (loop [xs args out defaults]
    (if (empty? xs)
      out
      (if (= "--negative" (first xs))
        (recur (rest xs) (assoc out :negative? true))
        (recur (nnext xs) (assoc out (keyword (subs (first xs) 2)) (second xs)))))))

(defn edge-key [e] [(:from e) (:to e)])

(defn validate [{:keys [organization stages edges negative?]}]
  (let [org (edn/read-string (slurp organization))
        org (if negative? (assoc-in org [:edges 0 :from-column] :ACT) org)
        stage-doc (edn/read-string (slurp stages))
        edge-doc (edn/read-string (slurp edges))
        stages-by-node (into {} (map (juxt (comp keyword :node) (comp keyword :stage)) (:nodes stage-doc)))
        source-by-key (into {} (map (juxt edge-key identity) (:edges edge-doc)))
        org-by-key (into {} (map (juxt edge-key identity) (:edges org)))
        source-keys (set (keys source-by-key))
        org-keys (set (keys org-by-key))
        errors
        (vec
         (concat
          (when-not (= (:edge-count org) (count (:edges org)))
            [{:error :edge-count-mismatch :declared (:edge-count org) :actual (count (:edges org))}])
          (when-not (= (:classification-counts org) (frequencies (map :classification (:edges org))))
            [{:error :classification-counts-mismatch}])
          (for [k (sort (set/difference source-keys org-keys))]
            {:error :drawn-edge-absent :edge k})
          (for [k (sort (set/difference org-keys source-keys))]
            {:error :unknown-edge :edge k})
          (mapcat
           (fn [e]
             (let [source (source-by-key (edge-key e))
                   from-stage (stages-by-node (:from e))
                   to-stage (stages-by-node (:to e))
                   relation (if (= from-stage to-stage) :within-column :cross-column)
                   role (case (:kind source) :control :cycle-edge :support :support-edge nil)]
               (concat
                (for [field required-edge-fields :when (not (contains? e field))]
                  {:error :missing-field :edge (edge-key e) :field field})
                (when-not from-stage [{:error :unknown-from-node :edge (edge-key e)}])
                (when-not to-stage [{:error :unknown-to-node :edge (edge-key e)}])
                (when-not (= from-stage (:from-column e))
                  [{:error :wrong-from-column :edge (edge-key e) :expected from-stage :actual (:from-column e)}])
                (when-not (= to-stage (:to-column e))
                  [{:error :wrong-to-column :edge (edge-key e) :expected to-stage :actual (:to-column e)}])
                (when-not (= relation (:column-relation e))
                  [{:error :wrong-column-relation :edge (edge-key e) :expected relation :actual (:column-relation e)}])
                (when-not (= role (:diagram-role e))
                  [{:error :wrong-diagram-role :edge (edge-key e) :expected role :actual (:diagram-role e)}])
                (when (and (= :unclassified (:classification e)) (not (seq (:reason e))))
                  [{:error :unclassified-without-reason :edge (edge-key e)}])
                (when (and (not= :unclassified (:classification e)) (not (seq (:basis e))))
                  [{:error :classified-without-basis :edge (edge-key e)}]))))
           (:edges org))
          (for [{:keys [path sha256] :as read} (:reads org)
                :let [actual-path (str "/home/joe/code/" path)]
                :when (or (not (.exists (io/file actual-path)))
                          (not= sha256 (when (.exists (io/file actual-path))
                                         (checks.control-organization-check/sha256 actual-path))))]
            {:error :unpinned-or-absent-read :read read})))]
    {:pass? (empty? errors)
     :drawn-edges (count source-keys)
     :classified (count (remove #(= :unclassified (:classification %)) (:edges org)))
     :unclassified (count (filter #(= :unclassified (:classification %)) (:edges org)))
     :errors errors}))

(defn -main [& args]
  (let [opts (parse-args args)
        result (validate opts)]
    (println (pr-str result))
    (if (:negative? opts)
      (if (:pass? result)
        (do (println "control-organization-check: FAIL negative mutation passed exit-convention=0-pass/1-fail") (System/exit 2))
        (do (println "control-organization-check: PASS negative control rejected exit-convention=0-pass/1-fail") (System/exit 0)))
      (do
        (println (str "control-organization-check: " (if (:pass? result) "PASS" "FAIL")
                      " exit-convention=0-pass/1-fail"))
        (when-not (:pass? result) (System/exit 1))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
