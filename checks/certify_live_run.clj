#!/usr/bin/env bb
(ns checks.certify-live-run
  (:require [babashka.fs :as fs]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [checks.wm-click-resource-observer :as click-observer]
            [checks.wm-operational-certificate :as certificate]))

(def default-run-dir "holes/labs/wm-contract")
(def default-resource-dirs ["holes/labs/wm-contract" "/tmp/futon-bounded-tests"])
(def bg "/home/joe/code/futon3c/scripts/bg.py")

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
      (when (contains? #{"futon-bounded-test-v1" "wm-click-resource-v1"}
                       (:schema m)) m))
    (catch Exception _ nil)))

(defn receipt-files [dirs]
  (->> dirs
       (mapcat (fn [dir]
                 (when (fs/exists? dir)
                   (concat (fs/glob dir "*.receipt.json")
                           (fs/glob dir "*resource-receipt.json")))))
       (map str) distinct sort vec))

(defn encloses-run? [run receipt]
  (let [started (instant (:started-at receipt))
        finished (instant (:finished-at receipt))
        run-start (instant (:startedAt run))]
    (and started finished run-start
         (not (.isAfter started run-start))
         (not (.isBefore finished run-start))
         (case (:schema receipt)
           "futon-bounded-test-v1"
           (str/includes? (str (:command receipt)) "run-tick-once")

           "wm-click-resource-v1"
           (and (= "shared-serving-jvm" (:observation-scope receipt))
                (= (:run/id run) (:run-id receipt))
                (= (:click/id run) (:click-id receipt))
                (contains? #{"clean" "dirty" "unavailable"}
                           (:resource-status receipt)))
           false))))

(defn matching-resources [run dir]
  (->> (receipt-files [dir])
       (keep (fn [path]
               (when-let [receipt (json-receipt path)]
                 (when (encloses-run? run receipt)
                   {:path path :receipt receipt}))))
       vec))

(defn locate-resource! [run-id run resource-dirs]
  (let [;; Directories are precedence tiers: committed receipts win over the
        ;; wrapper's transient spool, but ambiguity inside one tier is loud.
        matches (or (some #(not-empty (matching-resources run %)) resource-dirs) [])]
    (case (count matches)
      1 (first matches)
      0 (fail! "bounded resource receipt missing"
               {:run/id run-id :run-started-at (:startedAt run)
                :looked-for (mapv #(str % "/*.receipt.json") resource-dirs)})
      (fail! "bounded resource receipt is ambiguous"
             {:run/id run-id :matches (mapv :path matches)}))))

(defn normalized-resource [run-id source-path r tested-commit]
  (let [click? (= "wm-click-resource-v1" (:schema r))]
    (if click?
      (assoc (click-observer/certificate-resource run-id source-path r)
             :tested-commit tested-commit)
      {:schema 2
     :run/id run-id
     :source-schema (keyword (:schema r))
     :observation-scope (some-> (:observation-scope r) keyword)
     :status (keyword (:resource-status r))
     :reason (some-> (:reason r) keyword)
     :execution-outcome (when click? (keyword (:terminal-outcome r)))
     :command-exit (when-not click? (:inner-exit r))
     :wrapper-exit (when-not click? (:outer-exit r))
     :pids-events-max-delta (:pids-events-max-delta r)
     :native-thread-exhaustion (boolean (seq (:native-thread-markers r)))
     :tasks-peak (:pids-peak r)
       :source-receipt source-path})))

(defn tested-commit-from-job! [job-id]
  (when (str/blank? job-id)
    (fail! "--tested-job-id is required for production click certification"
           {:reason :tested-job-id-unavailable}))
  (let [{:keys [exit out]} (shell/sh "python3" bg "test-status" job-id)
        record (when (zero? exit) (try (json/parse-string out true)
                                      (catch Throwable _ nil)))
        receipt (:receipt record)
        head (get-in receipt [:repository-basis-finish :head])]
    (when-not (and (= job-id (:id record))
                   (= "inactive" (get-in record [:systemd :ActiveState]))
                   (= "clojure -T:build ci" (:command receipt))
                   (= 0 (:outer-exit receipt))
                   (= "pass" (:verdict receipt))
                   (true? (:repository-basis-stable receipt))
                   (false? (get-in receipt [:repository-basis-start :dirty]))
                   (false? (get-in receipt [:repository-basis-finish :dirty]))
                   (string? head) (not (str/blank? head)))
      (fail! "tested Futon2 producer receipt is unavailable or invalid"
             {:reason :tested-commit-producer-invalid :job-id job-id}))
    head))

(defn main [args]
  (let [{:keys [run-id run-dir resource-dirs out-dir tested-job-id]
         :or {run-dir default-run-dir
              resource-dirs (str/join ":" default-resource-dirs)
              out-dir default-run-dir}} (parse-args args)]
    (when (str/blank? run-id)
      (fail! "--run-id is required (latest is intentionally not guessed)"
             {:expected "make certify-run RUN_ID=<uuid>"}))
    (let [run-path (locate-run! run-id run-dir)
          run (read-edn run-path)
          resource-match (locate-resource! run-id run (str/split resource-dirs #":"))
          click? (= "wm-click-resource-v1" (get-in resource-match [:receipt :schema]))
          tested-commit (when click? (tested-commit-from-job! tested-job-id))
          resource (normalized-resource run-id (:path resource-match)
                                        (:receipt resource-match) tested-commit)
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
