#!/usr/bin/env bb
(ns checks.dirichlet-accumulation-import-absence
  (:require [babashka.fs :as fs]
            [clojure.pprint :as pp]
            [clojure.string :as str])
  (:import [java.math BigInteger]
           [java.security MessageDigest]))

(def repo "/home/joe/code/futon2")
(def default-report
  "holes/labs/wm-contract/dirichlet-accumulation-import-absence.edn")

(def surface-files
  ["scripts/futon2/report/war_machine.clj"
   "src/futon2/aif/observation.clj"
   "src/futon2/aif/belief.clj"
   "src/futon2/aif/a4a.clj"
   "src/futon2/aif/a4a_substrate.clj"
   "src/futon2/aif/r17_offline.clj"
   "src/futon2/aif/actuator_a3.clj"
   "src/futon2/aif/actuator_a6.clj"])

(def fully-qualified-sink-call-patterns
  [#"\(futon2\.aif\.a4a/corpus->concentration\b"
   #"\(futon2\.aif\.a4a/reduce-concepts\b"
   #"\(futon2\.aif\.a4a-substrate/read-corpus\b"
   #"\(futon2\.aif\.r17-offline/run\b"])

(def sink-namespaces
  {"futon2.aif.a4a" ["corpus->concentration" "reduce-concepts"]
   "futon2.aif.a4a-substrate" ["read-corpus"]
   "futon2.aif.r17-offline" ["run"]})

(def dynamic-sink-pattern
  #"(?i)\((requiring-resolve|resolve|ns-resolve|eval|load-string)\b.*(a4a|r17|corpus->concentration|reduce-concepts|read-corpus)")

(defn sha256-text [text]
  (let [md (MessageDigest/getInstance "SHA-256")]
    (.update md (.getBytes text "UTF-8"))
    (format "%064x" (BigInteger. 1 (.digest md)))))

(defn source [path]
  (let [text (slurp (str repo "/" path))]
    {:path path :sha256 (sha256-text text) :text text}))

(defn production-files []
  (->> (concat (fs/glob (str repo "/src") "**.clj")
               [(fs/path repo "scripts/futon2/report/war_machine.clj")])
       (map str)
       sort
       (mapv #(str/replace % (str repo "/") ""))))

(defn alias-call-patterns [text]
  (vec
   (mapcat
    (fn [[ns-name functions]]
      (let [alias-pattern (re-pattern
                           (str "\\[" (java.util.regex.Pattern/quote ns-name)
                                "\\s+:as\\s+([^\\s\\]\\)]+)"))]
        (for [[_ alias] (re-seq alias-pattern text)
              function functions]
          (re-pattern
           (str "\\(" (java.util.regex.Pattern/quote alias) "/"
                (java.util.regex.Pattern/quote function) "\\b")))))
    sink-namespaces)))

(defn matching-lines [path patterns]
  (let [lines (str/split-lines (slurp (str repo "/" path)))]
    (->> lines
         (map-indexed vector)
         (keep (fn [[idx line]]
                 (when (some #(re-find % line) patterns)
                   {:path path :line (inc idx) :text (str/trim line)})))
         vec)))

(defn facts [mutation]
  (let [production (production-files)
        role-pins (mapv #(dissoc (source %) :text) surface-files)
        production-pins (mapv #(dissoc (source %) :text) production)
        callers (->> production
                     (mapcat (fn [path]
                               (let [text (slurp (str repo "/" path))]
                                 (matching-lines
                                  path
                                  (into fully-qualified-sink-call-patterns
                                        (alias-call-patterns text))))))
                     (remove #(str/starts-with? (:text %) ";"))
                     vec)
        dynamic (->> (production-files)
                     (mapcat #(matching-lines % [dynamic-sink-pattern]))
                     vec)
        tick-files #{"scripts/futon2/report/war_machine.clj"
                     "src/futon2/aif/observation.clj"
                     "src/futon2/aif/belief.clj"}
        tick-callers (filterv #(contains? tick-files (:path %)) callers)
        tick-callers (cond-> tick-callers
                       (= mutation "tick-to-concentration")
                       (conj {:path "scripts/futon2/report/war_machine.clj"
                              :line :synthetic
                              :text "(a4a/corpus->concentration tick-observation)"
                              :control :injected}))]
    (sorted-map
     :claim :dirichletAccumulationImportAbsent
     :role-source-pins role-pins
     :production-census-pins production-pins
     :tick-model
     {:entrypoint "scripts/futon2/report/war_machine.clj"
      :observation-producer "src/futon2/aif/observation.clj"
      :belief-producer "src/futon2/aif/belief.clj"
      :required-sites (vec (concat
                            (matching-lines "scripts/futon2/report/war_machine.clj"
                                            [#"\(obs/observe\b" #"\(belief/update-belief-batch\b"])
                            (matching-lines "scripts/futon2/report/war_machine.clj"
                                            [#"\(belief/predict-observation\b"]))) }
     :r17
     {:feeder {:path "src/futon2/aif/a4a_substrate.clj"
               :function :read-corpus
               :store-reads [:capability :mission/doc :capability/* :discharge]}
      :concentration-producer {:path "src/futon2/aif/a4a.clj"
                               :function :corpus->concentration}
      :offline-consumer {:path "src/futon2/aif/r17_offline.clj"
                         :function :run}
      :writers [{:path "src/futon2/aif/actuator_a3.clj"
                 :writes [:capability/* :discharge]}]}
     :caller-census callers
     :dynamic-sink-resolution dynamic
     :tick-to-concentration-callers tick-callers)))

(defn checks [f]
  (sorted-map
   :surface-nonempty (boolean (seq (:role-source-pins f)))
   :surface-complete (= (set surface-files) (set (map :path (:role-source-pins f))))
   :production-census-nonempty (boolean (seq (:production-census-pins f)))
   :pins-complete (every? #(re-matches #"[0-9a-f]{64}" (:sha256 %))
                          (:production-census-pins f))
   :tick-sites-nonempty (boolean (seq (get-in f [:tick-model :required-sites])))
   :tick-sites-complete (= 3 (count (get-in f [:tick-model :required-sites])))
   :caller-census-nonempty (boolean (seq (:caller-census f)))
   :caller-census-contained
   (every? #(contains? #{"src/futon2/aif/r17_offline.clj"} (:path %))
           (:caller-census f))
   :dynamic-sink-resolution-absent (empty? (:dynamic-sink-resolution f))
   :tick-model-import-reaches-r17 (empty? (:tick-to-concentration-callers f))))

(defn parse-args [args]
  (loop [xs args out {}]
    (if (empty? xs)
      out
      (case (first xs)
        "--negative-tick-import" (recur (rest xs) (assoc out :negative? true))
        "--report" (if-let [path (second xs)]
                     (recur (nnext xs) (assoc out :report path))
                     (throw (ex-info "--report requires a path" {})))
        (throw (ex-info "unknown argument" {:argument (first xs)}))))))

(defn -main [& args]
  (let [{:keys [negative? report]} (parse-args args)
        mutation (when negative? "tick-to-concentration")
        f (facts mutation)
        per-check (checks f)
        failed (vec (sort (keys (remove val per-check))))
        expected [:tick-model-import-reaches-r17]
        control-ok? (and negative? (= expected failed))
        ok? (if negative? control-ok? (empty? failed))
        report-map (sorted-map :schema :dirichlet-accumulation-import-absence/v1
                               :facts f :checks per-check :pass? ok?)]
    (if negative?
      (do
        (println "REJECTED-CAUSES" (pr-str failed))
        (println (if control-ok?
                   "dirichlet-accumulation-import-absence: PASS negative exact rejection"
                   "dirichlet-accumulation-import-absence: FAIL negative rejection mismatch"))
        (System/exit (if control-ok? 0 2)))
      (if ok?
        (let [path (or report default-report)]
          (when-let [parent (fs/parent path)] (fs/create-dirs parent))
          (spit path (with-out-str (pp/pprint report-map)))
          (println "dirichlet-accumulation-import-absence: PASS")
          (println "report-sha256" (sha256-text (slurp path))))
        (do
          (binding [*out* *err*]
            (println "FAILED-CHECKS" (pr-str failed)))
          (System/exit 1))))))

(apply -main *command-line-args*)
