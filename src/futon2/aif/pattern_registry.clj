(ns futon2.aif.pattern-registry
  "Context-retrieval-backed pattern substrate adapter for the WM AIF apparatus.

   This closes the `:fire-pattern` gap honestly by treating recent
   `context-retrieval` evidence as the addressable pattern surface. The
   live stack already emits bounded top-k pattern retrieval certificates per
   agent turn; this adapter aggregates that evidence into a small candidate
   set the WM can act over.

   Honest scope: this is a second-stage judge over existing retrievals, not a
   raw library enumerator and not a retrieval-quality fix in itself. The
   embedding/keyword stack remains the recall surface; the WM action layer
   consumes the resulting certificates as a bounded candidate substrate."
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.string :as str]
            [futon2.aif.action-proposer :as ap]
            [futon2.aif.forward-model :as fm])
  (:import (java.time LocalDate ZoneId)))

(def ^:private default-evidence-base
  (or (System/getenv "FUTON3C_EVIDENCE_BASE")
      (System/getenv "FUTON3C_SERVER")
      (str "http://localhost:" (or (System/getenv "FUTON3C_PORT") "7070"))))

(def ^:private default-timezone
  (ZoneId/of "Europe/London"))

(def ^:private default-lookback-days 2)
(def ^:private default-fetch-limit 200)
(def ^:private default-top-k 3)

(defn- http-get-json
  [url]
  (try
    (let [resp (http/get url {:headers {"Accept" "application/json"}
                              :timeout 5000
                              :throw false})]
      (when (= 200 (:status resp))
        (json/parse-string (:body resp) true)))
    (catch Exception _ nil)))

(defn- since-date-str
  [days]
  (.toString (.minusDays (LocalDate/now default-timezone) days)))

(defn fetch-recent-evidence
  "Fetch recent evidence entries from the live Evidence Landscape.

   Options:
   - :evidence-base  base URL (default local futon3c/futon1a stack)
   - :lookback-days  inclusive lower bound in whole days (default 2)
   - :limit          request limit (default 200)"
  [& {:keys [evidence-base lookback-days limit]
      :or {evidence-base default-evidence-base
           lookback-days default-lookback-days
           limit default-fetch-limit}}]
  (let [params [(str "since=" (since-date-str lookback-days))
                (str "limit=" limit)]
        qs (str "?" (str/join "&" params))]
    (when-let [data (http-get-json (str evidence-base "/api/alpha/evidence" qs))]
      (when (:ok data)
        (or (:entries data) [])))))

(defn- context-retrieval-entry?
  [entry]
  (= "context-retrieval" (get-in entry [:evidence/body :event])))

(defn- candidate-observation
  [entry result rank]
  (let [score (double (or (:score result) 0.0))
        rank-weight (/ 1.0 (double rank))]
    {:id (:id result)
     :title (or (:title result) (:id result) "")
     :score score
     :weighted-score (* score rank-weight)
     :rank rank
     :evidence-id (:evidence/id entry)
     :at (or (get-in entry [:evidence/body :at])
             (:evidence/at entry))
     :turn (get-in entry [:evidence/body :turn])
     :query (get-in entry [:evidence/body :query])
     :pattern-path (:pattern-path result)
     :retrieval-rationale (:retrieval-rationale result)
     :hotwords (:hotwords result)
     :sigil (:sigil result)
     :sigils (:sigils result)
     :tokipona (:tokipona result)
     :energy (:energy result)
     :if (:if result)
     :however (:however result)
     :then (:then result)
     :because (:because result)
     :next-steps (:next-steps result)
     :devmap? (:devmap? result)}))

(defn aggregate-pattern-candidates
  "Aggregate raw context-retrieval evidence entries into bounded pattern
   candidates. Each retrieved pattern observation contributes a weighted score
   `score / rank`, rewarding both retrieval confidence and recurrence.

   Returns a vector of candidate maps sorted deterministically by
   weighted-score desc, mentions desc, best-score desc, latest-at desc, id asc."
  [entries & {:keys [top-k] :or {top-k default-top-k}}]
  (let [observations
        (for [entry entries
              :when (context-retrieval-entry? entry)
              [idx result] (map-indexed vector (or (get-in entry [:evidence/body :results]) []))
              :when (string? (:id result))]
          (candidate-observation entry result (inc idx)))
        grouped (vals (group-by :id observations))
        aggregated
        (mapv (fn [obs]
                (let [exemplar (apply max-key :score obs)
                      latest-at (last (sort (or (keep :at obs) [""])))
                      best-score (apply max (map :score obs))
                      weighted-score (reduce + (map :weighted-score obs))
                      mentions (count obs)
                      evidence-ids (->> obs (keep :evidence-id) distinct sort vec)
                      turns (->> obs (keep :turn) distinct sort vec)]
                  {:id (:id exemplar)
                   :title (:title exemplar)
                   :weighted-score weighted-score
                   :best-score best-score
                   :mentions mentions
                   :latest-at latest-at
                   :evidence-ids evidence-ids
                   :turns turns
                   :queries (->> obs (keep :query) distinct vec)
                   :pattern-path (:pattern-path exemplar)
                   :retrieval-rationale (:retrieval-rationale exemplar)
                   :hotwords (:hotwords exemplar)
                   :sigil (:sigil exemplar)
                   :sigils (:sigils exemplar)
                   :tokipona (:tokipona exemplar)
                   :energy (:energy exemplar)
                   :if (:if exemplar)
                   :however (:however exemplar)
                   :then (:then exemplar)
                   :because (:because exemplar)
                   :next-steps (:next-steps exemplar)
                   :devmap? (:devmap? exemplar)}))
              grouped)]
    (->> aggregated
         (sort-by (juxt (comp - :weighted-score)
                        (comp - :mentions)
                        (comp - :best-score)
                        :id))
         (take top-k)
         vec)))

(defn open-patterns
  "Return bounded addressable pattern candidates from recent
   `context-retrieval` evidence.

   Zero-arg variant fetches live evidence; one-arg variant accepts already
   fetched entries for tests and replay."
  ([] (open-patterns (or (fetch-recent-evidence) [])))
  ([entries]
   (aggregate-pattern-candidates entries)))

(defmethod fm/can-propose? :fire-pattern
  [state _action-type]
  (boolean (seq (:patterns state))))

(defmethod fm/can-execute? :fire-pattern
  [state action]
  (boolean (some #(= (:target action) (:id %))
                 (:patterns state []))))

(def pattern-enumerator-proposer
  "Proposer that emits one `:fire-pattern` action per recent aggregated
   context-retrieval candidate in the state map."
  (reify ap/ActionProposer
    (propose [_ state]
      (for [p (:patterns state)]
        {:type :fire-pattern
         :target (:id p)
         :weight 1.0
         :pattern-title (:title p)
         :pattern-path (:pattern-path p)
         :retrieval-score (:weighted-score p)
         :retrieval-rationale (:retrieval-rationale p)
         :hotwords (:hotwords p)
         :sigils (:sigils p)
         :pattern-summary (:then p)
         :pattern-because (:because p)
         :next-steps (:next-steps p)
         :evidence-ids (:evidence-ids p)
         :turns (:turns p)
         :rationale (str "context-retrieval substrate: " (:title p)
                         " mentions=" (:mentions p)
                         " weighted-score=" (format "%.3f" (double (:weighted-score p))))}))
    (proposer-id [_] :pattern-enumerator)))
