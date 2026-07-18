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
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [futon2.aif.action-proposer :as ap]
            [futon2.aif.forward-model :as fm])
  (:import (java.security MessageDigest)
           (java.time LocalDate ZoneId)))

(defn configured-evidence-base
  "Resolve the Evidence Landscape authority used for retrieval receipts.

   Resolution order is the stack-wide contract: FUTON3C_EVIDENCE_BASE,
   FUTON3C_SERVER, then an IPv4-loopback URL using FUTON3C_PORT (default 7070).
   The loopback is deliberately `127.0.0.1` because the production http-kit
   listener is IPv4-bound. The map arity is the pure configuration seam used
   by tests and sibling consumers."
  ([] (configured-evidence-base (System/getenv)))
  ([env]
   (or (not-empty (get env "FUTON3C_EVIDENCE_BASE"))
       (not-empty (get env "FUTON3C_SERVER"))
       (str "http://127.0.0.1:"
            (or (not-empty (get env "FUTON3C_PORT")) "7070")))))

(def ^:private default-timezone
  (ZoneId/of "Europe/London"))

(def ^:private default-lookback-days 2)
(def ^:private default-fetch-limit 500)
(def ^:private default-http-timeout-ms 30000)
(def ^:private default-top-k 3)

(def ^:dynamic *pattern-library-root*
  "Canonical root for production-addressable pattern artifacts. Dynamic only
   so tests and deployments can bind an isolated library without weakening the
   containment check."
  (or (System/getenv "FUTON_PATTERN_LIBRARY_ROOT")
      "/home/joe/code/futon3/library"))

(defn- sha256-file [file]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (with-open [in (io/input-stream file)]
      (let [buffer (byte-array 8192)]
        (loop []
          (let [n (.read in buffer)]
            (when (pos? n)
              (.update digest buffer 0 n)
              (recur))))))
    (apply str (map #(format "%02x" (bit-and 0xff %)) (.digest digest)))))

(defn pattern-artifact-receipt
  "Return the canonical path and content digest when ID/PATH identify a real
   `.flexiarg` beneath the configured pattern library; otherwise nil.

   The relative artifact path (without `.flexiarg`) must equal ID. This rejects
   receipt/path substitution as well as missing files and `..`/symlink escapes."
  [id path]
  (when (and (string? id) (not (str/blank? id))
             (string? path) (not (str/blank? path)))
    (try
      (let [root (.getCanonicalFile (io/file *pattern-library-root*))
            file (.getCanonicalFile (io/file path))
            root-path (.toPath root)
            file-path (.toPath file)
            relative (when (.startsWith file-path root-path)
                       (str (.relativize root-path file-path)))
            artifact-id (some-> relative
                                (str/replace java.io.File/separator "/")
                                (str/replace #"\.flexiarg$" ""))]
        (when (and (.isFile file)
                   (str/ends-with? (.getName file) ".flexiarg")
                   (= id artifact-id))
          {:pattern-path (.getPath file)
           :pattern-sha256 (sha256-file file)}))
      (catch Exception _ nil))))

(defn addressable-pattern?
  "True only for a pattern candidate backed by a current canonical artifact
   and at least one Evidence Landscape receipt. Retrieval text or an id without
   that content-bound address is context, not an executable WM target."
  [pattern]
  (let [receipt (when (map? pattern)
                  (pattern-artifact-receipt (:id pattern)
                                            (:pattern-path pattern)))]
    (and receipt
         (= (:pattern-path receipt) (:pattern-path pattern))
         (= (:pattern-sha256 receipt) (:pattern-sha256 pattern))
         (seq (:evidence-ids pattern))
         (every? #(and (string? %) (not (str/blank? %)))
                 (:evidence-ids pattern)))))

(defn- http-get-json
  [url]
  (try
    (let [resp (http/get url {:headers {"Accept" "application/json"}
                              :timeout default-http-timeout-ms
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
   - :evidence-base  base URL (default configured futon3c Evidence Landscape)
   - :lookback-days  inclusive lower bound in whole days (default 2)
   - :limit          request limit (default 500; bounded to survive ordinary
                     coordination traffic between retrieval receipts)"
  [& {:keys [evidence-base lookback-days limit]
      :or {evidence-base (configured-evidence-base)
           lookback-days default-lookback-days
           limit default-fetch-limit}}]
  (let [params [(str "since=" (since-date-str lookback-days))
                (str "limit=" limit)]
        qs (str "?" (str/join "&" params))]
    (when-let [data (http-get-json (str evidence-base "/api/alpha/evidence" qs))]
      (when (:ok data)
        (or (:entries data) [])))))

(defn- evidence-body
  "Normalize the Evidence Landscape wire representation. Futon1b may return
   an EDN body as a string while older replay fixtures carry an already-decoded
   map. Unreadable or non-map bodies are not retrieval authority."
  [entry]
  (let [body (:evidence/body entry)]
    (cond
      (map? body) body
      (string? body) (try
                       (let [decoded (edn/read-string body)]
                         (when (map? decoded)
                           (walk/keywordize-keys decoded)))
                       (catch Exception _ nil))
      :else nil)))

(defn- context-retrieval-entry?
  [entry]
  (= "context-retrieval" (:event (evidence-body entry))))

(defn- candidate-pattern-path [id]
  (when (and (string? id) (not (str/blank? id)))
    (.getPath (io/file *pattern-library-root* (str id ".flexiarg")))))

(defn- candidate-observation
  [entry result rank]
  (let [body (evidence-body entry)
        score (double (or (:score result) 0.0))
        rank-weight (/ 1.0 (double rank))]
    {:id (:id result)
     :title (or (:title result) (:id result) "")
     :score score
     :weighted-score (* score rank-weight)
     :rank rank
     :evidence-id (:evidence/id entry)
     :at (or (:at body)
             (:evidence/at entry))
     :turn (:turn body)
     :query (:query body)
     :pattern-path (or (:pattern-path result)
                       (candidate-pattern-path (:id result)))
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
              [idx result] (map-indexed vector
                                        (or (:results (evidence-body entry)) []))
              :when (string? (:id result))]
          (candidate-observation entry result (inc idx)))
        grouped (vals (group-by :id observations))
        aggregated
        (keep (fn [obs]
                (let [addressed (keep (fn [observation]
                                        (when-let [artifact
                                                   (pattern-artifact-receipt
                                                    (:id observation)
                                                    (:pattern-path observation))]
                                          (assoc observation
                                                 ::artifact artifact)))
                                      obs)
                      exemplar (when (seq addressed)
                                 (apply max-key :score addressed))
                      artifact (::artifact exemplar)
                      latest-at (last (sort (or (keep :at obs) [""])))
                      best-score (apply max (map :score obs))
                      weighted-score (reduce + (map :weighted-score obs))
                      mentions (count obs)
                      evidence-ids (->> obs (keep :evidence-id) distinct sort vec)
                      turns (->> obs (keep :turn) distinct sort vec)]
                  (when exemplar
                    (merge {:id (:id exemplar)
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
                            :devmap? (:devmap? exemplar)}
                           artifact))))
              grouped)]
    (->> aggregated
         (filter addressable-pattern?)
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
  (boolean (some addressable-pattern? (:patterns state))))

(defmethod fm/can-execute? :fire-pattern
  [state action]
  (boolean
   (some (fn [pattern]
           (and (addressable-pattern? pattern)
                (= :pattern-enumerator (:proposer-id action))
                (= (:target action) (:id pattern))
                (= (:evidence-ids action) (:evidence-ids pattern))
                (= (:pattern-title action) (:title pattern))
                (= (:pattern-path action) (:pattern-path pattern))
                (= (:pattern-sha256 action) (:pattern-sha256 pattern))
                (= (:retrieval-score action) (:weighted-score pattern))
                (= (:retrieval-rationale action)
                   (:retrieval-rationale pattern))
                (= (:hotwords action) (:hotwords pattern))
                (= (:sigils action) (:sigils pattern))
                (= (:pattern-summary action) (:then pattern))
                (= (:pattern-because action) (:because pattern))
                (= (:next-steps action) (:next-steps pattern))
                (= (:turns action) (:turns pattern))))
         (:patterns state []))))

(defn actuation-construction
  "Build the typed production construction for a provenance-bearing
   `:fire-pattern` instance.  The live judge has already checked this action
   against its current pattern substrate with `fm/can-execute?`; this second
   fail-closed shape check prevents a malformed or provenance-free instance
   from entering author dispatch through another caller."
  [{:keys [type target proposer-id evidence-ids pattern-title pattern-path
           pattern-sha256 pattern-summary pattern-because next-steps]
    :as action}]
  (let [artifact (pattern-artifact-receipt target pattern-path)]
    (when (and (= :fire-pattern type)
               (= :pattern-enumerator proposer-id)
               (string? target)
               (not (str/blank? target))
               (seq evidence-ids)
               (every? #(and (string? %) (not (str/blank? %))) evidence-ids)
               (= pattern-path (:pattern-path artifact))
               (= pattern-sha256 (:pattern-sha256 artifact)))
      {:mission target
       :psi (or pattern-summary
                (str "instantiate addressable pattern " target))
       :construction-kind :fire-pattern-actuation
       :selected-action action
       :shown [target]
       :semilattice
       [{:from :retrieval-certificate :to :addressable-pattern-instance}
        {:from :addressable-pattern-instance :to :author-dispatch}
        {:from :author-dispatch :to :independent-review}
        {:from :independent-review :to :grounded-implementation}]
       :policy-holes []
       :actuation-contract
       {:action-class :fire-pattern
        :target target
        :pattern-title pattern-title
        :pattern-path pattern-path
        :pattern-sha256 pattern-sha256
        :evidence-ids (vec evidence-ids)
        :instruction pattern-summary
        :because pattern-because
        :next-steps (vec next-steps)
        :production-route
        [:author-dispatch :independent-review :grounded-implementation]
        :acceptance
        [{:check :target-bound
          :claim "implementation applies the selected pattern target"}
         {:check :retrieval-provenance
          :claim "implementation retains the selected retrieval receipts"}
         {:check :artifact-integrity
          :claim "implementation uses the selected canonical pattern content"}
         {:check :grounded-change
          :claim "reviewed commit is recorded through the full-loop actuator"}]}})))

(def pattern-enumerator-proposer
  "Proposer that emits one `:fire-pattern` action per recent aggregated
   context-retrieval candidate in the state map."
  (reify ap/ActionProposer
    (propose [_ state]
      (keep (fn [p]
              (when (addressable-pattern? p)
                {:type :fire-pattern
                 :target (:id p)
                 :weight 1.0
                 :proposer-id :pattern-enumerator
                 :pattern-title (:title p)
                 :pattern-path (:pattern-path p)
                 :pattern-sha256 (:pattern-sha256 p)
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
                                 " weighted-score="
                                 (format "%.3f"
                                         (double (:weighted-score p))))}))
            (:patterns state)))
    (proposer-id [_] :pattern-enumerator)))
