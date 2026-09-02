#!/usr/bin/env bb

(require '[babashka.process :as process]
         '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pprint]
         '[clojure.string :as str])

(def futon2-root (.getCanonicalPath (io/file ".")))
(def futon3c-root (.getCanonicalPath (io/file futon2-root "../futon3c")))
(def output-path
  (str futon2-root "/holes/labs/zaif-harness/runs/D9-tie-order-count.edn"))
(def calibration-path
  (str futon2-root "/holes/labs/M-zaif-harness/calibration-sessions.edn"))
(def live-query
  "http://127.0.0.1:7073/api/alpha/evidence/text-search?tags=zaif&limit=100")

(when-not (.isFile (io/file calibration-path))
  (throw (ex-info "run from the futon2 repository root"
                  {:expected calibration-path})))

(def replay-form
  (str
   "(do\n"
   " (require '[clojure.edn :as edn]\n"
   "          '[clojure.java.io :as io]\n"
   "          '[futon3c.agents.zaif-controller :as controller]\n"
   "          '[futon3c.agents.zaif-inputs :as inputs])\n"
   " (def arm-order [:retrieve :act :ask :yield])\n"
   " (defn normalize-inputs [x]\n"
   "   (update x :gamma #(if (string? %) (edn/read-string %) %)))\n"
   " (defn classify [decision]\n"
   "   (let [terms (:g-terms decision)\n"
   "         maximum (apply max (vals terms))\n"
   "         tied (vec (filter #(= maximum (get terms %)) arm-order))]\n"
   "     {:settlement (if (> (count tied) 1) :tie-settled :score-settled)\n"
   "      :tie-class tied}))\n"
   " (defn summarize [rows]\n"
   "   (let [replayed (mapv (fn [{:keys [id inputs]}]\n"
   "                          (let [decision (controller/decide\n"
   "                                          (normalize-inputs inputs))]\n"
   "                            {:id id :inputs (normalize-inputs inputs)\n"
   "                             :decision decision\n"
   "                             :classification (classify decision)}))\n"
   "                        rows)]\n"
   "     {:count (count replayed)\n"
   "      :settlement-counts (frequencies\n"
   "                          (map #(get-in % [:classification :settlement]) replayed))\n"
   "      :per-arm (into (sorted-map)\n"
   "                     (for [arm arm-order]\n"
   "                       [arm (frequencies\n"
   "                             (map #(get-in % [:classification :settlement])\n"
   "                                  (filter #(= arm (get-in % [:decision :arm]))\n"
   "                                          replayed)))]))\n"
   "      :per-tie-class (frequencies\n"
   "                      (map #(get-in % [:classification :tie-class])\n"
   "                           (filter #(= :tie-settled\n"
   "                                       (get-in % [:classification :settlement]))\n"
   "                                   replayed)))\n"
   "      :live-pin (first replayed)}))\n"
   " (let [sessions (edn/read-string (slurp " (pr-str calibration-path) "))\n"
   "       calibration-rows\n"
   "       (mapv (fn [session]\n"
   "               {:id (:id session)\n"
   "                :inputs (inputs/hydrate-inputs {:context (:context session)})})\n"
   "             sessions)\n"
   "       response (edn/read-string (slurp " (pr-str live-query) "))\n"
   "       live-entries (->> (:results response)\n"
   "                         (map :entry)\n"
   "                         (filter #(= :zaif-arm-choice\n"
   "                                     (get-in % [:evidence/body :event])))\n"
   "                         (filter #(map? (get-in % [:evidence/body :inputs-snapshot])))\n"
   "                         vec)\n"
   "       live-rows (mapv (fn [entry]\n"
   "                         {:id (:evidence/id entry)\n"
   "                          :inputs (get-in entry [:evidence/body :inputs-snapshot])})\n"
   "                       live-entries)]\n"
   "   (prn {:calibration (summarize calibration-rows)\n"
   "         :live (summarize live-rows)})))"))

(defn run-command
  [args dir]
  (let [result @(process/process args {:dir dir :out :string :err :string})]
    (when-not (zero? (:exit result))
      (throw (ex-info "command failed" {:args args :result result})))
    (str/trim (:out result))))

(let [replay (edn/read-string
              (run-command ["clojure" "-M" "-e" replay-form] futon3c-root))
      code-sha (run-command ["git" "rev-parse" "HEAD"] futon3c-root)
      corpus-sha (run-command ["sha256sum" calibration-path] futon2-root)
      report
      {:finding/type :d9/tie-order-count
       :controller {:declaration "futon3c.agents.zaif-controller/decide"
                    :repository "/home/joe/code/futon3c"
                    :code-sha code-sha}
       :replay-command
       "bb holes/labs/zaif-harness/runs/regenerate-D9-tie-order-count.bb"
       :populations
       {:calibration {:path "holes/labs/M-zaif-harness/calibration-sessions.edn"
                      :sha256 (first (str/split corpus-sha #"\s+"))
                      :sessions 114
                      :input-reconstruction
                      "current zaif-inputs/hydrate-inputs with each session's :context; D8b typed-empty task-belief fallback retained"}
        :live {:query live-query
               :records (get-in replay [:live :count])
               :input-reconstruction
               "each persisted :zaif-arm-choice :inputs-snapshot, normalized only by EDN-reading its wire-string :gamma field"}}
       :classification
       {:tie-settled "chosen arm shares the exact maximum g-term with another arm"
        :score-settled "chosen arm is the unique exact maximum"
        :tie-class-order [:retrieve :act :ask :yield]}
       :calibration (:calibration replay)
       :live (:live replay)}]
  (with-open [writer (io/writer output-path)]
    (binding [*out* writer]
      (pprint/pprint report)))
  (println (pr-str {:ok true
                    :output output-path
                    :calibration (select-keys (:calibration replay)
                                              [:count :settlement-counts])
                    :live (select-keys (:live replay)
                                       [:count :settlement-counts])})))
