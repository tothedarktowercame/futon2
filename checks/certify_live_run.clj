#!/usr/bin/env bb
(ns checks.certify-live-run
  (:require [babashka.fs :as fs]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [checks.wm-operational-certificate :as certificate]))

(def default-run-dir "holes/labs/wm-contract")
(def default-resource-dirs ["holes/labs/wm-contract" "/tmp/futon-bounded-tests"])

(defn fail! [message data]
  (binding [*out* *err*]
    (println (str "certify-live-run: FAIL " message))
    (println (pr-str data)))
  (System/exit 1))

(defn parse-args [args]
  (loop [xs args out {}]
    (if-let [x (first xs)]
      (recur (nnext xs) (assoc out (keyword (subs x 2)) (second xs)))
      out)))

(defn read-edn [path]
  (try (edn/read-string (slurp path))
       (catch Exception _ nil)))

(defn run-files [dir]
  (->> (fs/glob dir "tick-run-record-*.edn") (map str) sort vec))

(defn locate-run! [run-id run-dir]
  (let [searched (run-files run-dir)
        matches (filterv #(= run-id (:run/id (read-edn %))) searched)]
    (case (count matches)
      1 (first matches)
      0 (fail! "run record missing"
               {:run/id run-id :looked-for (str run-dir "/tick-run-record-*.edn")
                :files-searched searched})
      (fail! "run id is ambiguous" {:run/id run-id :matches matches}))))

(defn instant [x]
  (when (string? x)
    (try (java.time.Instant/parse x) (catch Exception _ nil))))

(defn json-receipt [path]
  (try
    (let [m (json/parse-string (slurp path) true)]
      (when (= "futon-bounded-test-v1" (:schema m)) m))
    (catch Exception _ nil)))

(defn receipt-files [dirs]
  (->> dirs
       (mapcat (fn [dir]
                 (when (fs/exists? dir)
                   (concat (fs/glob dir "*.receipt.json")
                           (fs/glob dir "*resource-receipt.json")))))
       (map str) distinct sort vec))

(defn encloses-run? [run-start receipt]
  (let [started (instant (:started-at receipt))
        finished (instant (:finished-at receipt))]
    (and started finished run-start
         (not (.isAfter started run-start))
         (not (.isBefore finished run-start))
         (str/includes? (str (:command receipt)) "run-tick-once"))))

(defn matching-resources [run-start dir]
  (->> (receipt-files [dir])
       (keep (fn [path]
               (when-let [receipt (json-receipt path)]
                 (when (encloses-run? run-start receipt)
                   {:path path :receipt receipt}))))
       vec))

(defn locate-resource! [run-id run resource-dirs]
  (let [run-start (instant (:startedAt run))
        ;; Directories are precedence tiers: committed receipts win over the
        ;; wrapper's transient spool, but ambiguity inside one tier is loud.
        matches (or (some #(not-empty (matching-resources run-start %)) resource-dirs) [])]
    (case (count matches)
      1 (first matches)
      0 (fail! "bounded resource receipt missing"
               {:run/id run-id :run-started-at (:startedAt run)
                :looked-for (mapv #(str % "/*.receipt.json") resource-dirs)})
      (fail! "bounded resource receipt is ambiguous"
             {:run/id run-id :matches (mapv :path matches)}))))

(defn normalized-resource [run-id source-path r]
  {:schema 1
   :run/id run-id
   :source-schema :futon-bounded-test-v1
   :status (keyword (:resource-status r))
   :reason (some-> (:reason r) keyword)
   :command-exit (:inner-exit r)
   :wrapper-exit (:outer-exit r)
   :pids-events-max-delta (:pids-events-max-delta r)
   :native-thread-exhaustion (boolean (seq (:native-thread-markers r)))
   :tasks-peak (:pids-peak r)
   :source-receipt source-path})

(defn main [args]
  (let [{:keys [run-id run-dir resource-dirs out-dir]
         :or {run-dir default-run-dir
              resource-dirs (str/join ":" default-resource-dirs)
              out-dir default-run-dir}} (parse-args args)]
    (when (str/blank? run-id)
      (fail! "--run-id is required (latest is intentionally not guessed)"
             {:expected "make certify-run RUN_ID=<uuid>"}))
    (let [run-path (locate-run! run-id run-dir)
          run (read-edn run-path)
          resource-match (locate-resource! run-id run (str/split resource-dirs #":"))
          resource (normalized-resource run-id (:path resource-match) (:receipt resource-match))
          safe-id (str/replace run-id #"[^A-Za-z0-9._-]" "_")
          resource-path (str out-dir "/operational-resource-" safe-id ".edn")
          certificate-path (str out-dir "/operational-certificate-" safe-id ".edn")]
      (fs/create-dirs out-dir)
      (spit resource-path (str (pr-str resource) "\n"))
      (let [exit (certificate/main ["--run" run-path "--resource" resource-path
                                    "--certificate" certificate-path])
            cert (read-edn certificate-path)]
        (println (pr-str {:run/id run-id
                          :run-record run-path
                          :resource-receipt (:path resource-match)
                          :resource-input resource-path
                          :certificate certificate-path
                          :topology (:topology cert)
                          :traversal-counts (get-in cert [:traversal :counts])
                          :resource-status (get-in cert [:resource-status :status])
                          :verdict (:verdict cert)}))
        exit))))

(when (= *file* (System/getProperty "babashka.file"))
  (System/exit (main *command-line-args*)))
