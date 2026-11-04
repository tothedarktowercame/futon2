(ns futon2.aif.operator-focus
  "Current operator focus, voted by the seats Joe addressed recently.

   The evidence landscape is authoritative. Live reads are bounded and retained
   as content-addressed manifests; replay consumes only such a manifest."
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.action-identity :as identity])
  (:import [java.net URLEncoder]
           [java.time Duration Instant]))

(def keyword-table-version "operator-focus-keywords-v1")

(def keyword-table
  ;; WM's own name and outer-selection vocabulary are stronger than generic "machine".
  [{:area "WM" :pattern #"(?i)\b(war machine|outer selection|focus selection|commit.?facets|retained focus|wm)\b"}
   ;; Giraffe is a named subsystem and has no useful generic synonym.
   {:area "Giraffe" :pattern #"(?i)\bgiraffe\b"}
   ;; Pattern work includes retrieval/mining/cascade discussion, not every use of "pattern" in prose.
   {:area "patterns" :pattern #"(?i)\b(pattern retrieval|pattern mining|pattern library|cascade construction|flexiarg)\b"}
   ;; APM is principally the Lean proof campaign.
   {:area "APM" :pattern #"(?i)\b(apm|lean proof|mathlib|theorem|lemma)\b"}
   ;; Arxana includes its user-facing browse/read surfaces.
   {:area "Arxana" :pattern #"(?i)\b(arxana|arxana-browse|scope browser)\b"}
   ;; Agency covers clocks, seats, dispatch and agent coordination.
   {:area "Agency" :pattern #"(?i)\b(agency|auto.?clock|clocked|dispatch|agent registry|seat)\b"}
   ;; Memory means explicit memory/retrieval apparatus, not ordinary remembering.
   {:area "memory" :pattern #"(?i)\b(memory system|memory retrieval|long.?term memory|minus40)\b"}
   ;; 象 is named directly or via its operator-turn interpretation service.
   {:area "象" :pattern #"(?i)(象|\bxiang\b|operator turn analysis|turn interpretation)"}
   ;; futon6 is a named repository/subsystem.
   {:area "futon6" :pattern #"(?i)\bfuton6\b"}
   ;; voxterm is a named voice surface.
   {:area "voxterm" :pattern #"(?i)\bvoxterm\b"}])

(defn load-area-corpus
  "Read accepted `*.relate.json` rows under PATH. A row is accepted only when
   the area citation's check is `ok*` and its area value is nonblank."
  [path]
  (let [root (io/file path)
        files (if (.isDirectory root)
                (sort-by #(.getPath ^java.io.File %)
                         (filter #(str/ends-with? (.getName ^java.io.File %) ".relate.json")
                                 (file-seq root)))
                [root])
        rows (->> files
                  (keep (fn [f]
                          (try
                            (let [x (json/parse-string (slurp f) true)
                                  task (get-in x [:row :stem])
                                  area (get-in x [:row :area :value])]
                              (when (and (str/starts-with? (str (nth (:checks x) 1 "")) "ok")
                                         (not (str/blank? task))
                                         (not (str/blank? area)))
                                [task area]))
                            (catch Exception _ nil))))
                  (into (sorted-map)))]
    {:rows rows :sha256 (identity/digest rows)}))

(def ^:private task-pattern #"(?<![A-Za-z0-9_-])(?:M|E|T)-[A-Za-z0-9][A-Za-z0-9_-]*")
(def ^:private seat-pattern #"(.+)-turn-[0-9]+")

(defn- majority [xs]
  (when (seq xs)
    (let [counts (frequencies xs)
          best (apply max (vals counts))]
      (->> counts (keep (fn [[x n]] (when (= n best) x))) sort vec))))

(defn- captured-target [turn]
  (or (:captured-target turn) (:clocked-target turn) (:ticket-id turn)
      (:excursion-id turn) (:mission-id turn) (:campaign-id turn)))

(defn map-turn
  "Map one normalized turn to an area, recording precedence step a-d."
  [turn area-of-task]
  (let [text (str (or (:text turn) ""))
        named (re-seq task-pattern text)
        named-areas (keep area-of-task named)]
    (cond
      (seq named-areas)
      (let [areas (majority named-areas)]
        {:areas areas :area (first areas) :step :text-task :tasks (vec named)})

      :else
      (if-let [hit (first (filter #(re-find (:pattern %) text) keyword-table))]
        {:areas [(:area hit)] :area (:area hit) :step :keyword}
        (if-let [area (some-> (captured-target turn) area-of-task)]
          {:areas [area] :area area :step :captured-target
           :task (captured-target turn)}
          {:areas [] :step :abstain :reason :turn-unmapped})))))

(defn- instant [x]
  (cond (instance? Instant x) x
        (string? x) (Instant/parse x)
        :else (throw (ex-info "as-of/turn time must be an Instant or ISO string" {:value x}))))

(defn focus-vote
  "Pure vote over normalized turns. `area-of-task` maps task id to area.
   Options: `:as-of`, `:window-days` (7), `:seat-count` (10), and
   `:turns-per-seat` (10)."
  [turns area-of-task {:keys [as-of window-days seat-count turns-per-seat]
                       :or {window-days 7 seat-count 10 turns-per-seat 10}}]
  (let [as-of (instant as-of)
        since (.minus as-of (Duration/ofDays window-days))
        eligible (->> turns
                      (filter (fn [t]
                                (let [at (instant (:at t))]
                                  (and (not (.isAfter at as-of))
                                       (not (.isBefore at since))))))
                      (sort-by (comp str :at) #(compare %2 %1)))
        seats (->> eligible (map :seat) distinct (take seat-count) vec)
        selected? (set seats)
        per-seat (group-by :seat (filter #(selected? (:seat %)) eligible))
        seat-results
        (mapv (fn [seat]
                (let [turns (take turns-per-seat (get per-seat seat))
                      mapped (mapv #(assoc (map-turn % area-of-task)
                                           :evidence-id (:evidence-id %)
                                           :at (str (:at %))) turns)
                      areas (mapcat :areas mapped)]
                  (if (empty? areas)
                    {:seat seat :status :abstained :reason :no-mappable-turns
                     :turns mapped}
                    (let [winners (majority areas)
                          share (/ 1 (count winners))]
                      {:seat seat :status :voted
                       :votes (into (sorted-map) (map (fn [a] [a share]) winners))
                       :turns mapped}))))
              seats)
        totals (reduce (fn [m {:keys [votes]}]
                         (reduce-kv #(update %1 %2 (fnil + 0) %3) m (or votes {})))
                       (sorted-map) seat-results)
        result (if (empty? totals)
                 {:status :absent :reason :no-recent-operator-turns}
                 (let [best (apply max (vals totals))]
                   {:status :present
                    :focus (into (sorted-set) (keep (fn [[a v]] (when (= v best) a)) totals))
                    :totals totals}))]
    {:as-of (str as-of) :window-days window-days :seat-count seat-count
     :turns-per-seat turns-per-seat :seats seats :seat-votes seat-results
     :abstentions (filterv #(= :abstained (:status %)) seat-results)
     :result result}))

(defn evidence-row->turn
  "Post-validate and normalize one authoritative evidence row, or nil."
  [row]
  (let [body (:evidence/body row)
        turn-id (:turn-id body)
        seat (second (re-matches seat-pattern (str turn-id)))]
    (when (and (= :operator (get-in row [:evidence/origin :kind]))
               (= "user" (:role body)) seat)
      {:evidence-id (:evidence/id row) :at (:evidence/at row) :seat seat
       :text (:text body)
       :captured-target (or (:clocked-target body) (:ticket-id body)
                            (:excursion-id body) (:mission-id body) (:campaign-id body))})))

(defn- enc [x] (URLEncoder/encode (str x) "UTF-8"))

(defn- timeout-exception? [e]
  (loop [e e]
    (cond (nil? e) false
          (or (instance? java.net.http.HttpTimeoutException e)
              (instance? java.net.SocketTimeoutException e)) true
          :else (recur (.getCause ^Throwable e)))))

(defn read-turns
  "Bounded authoritative read: at most three pages of 100. No file fallback."
  [{:keys [base-url timeout-ms] :or {base-url "http://127.0.0.1:7073" timeout-ms 10000}}]
  (try
    (loop [page 0 cursor nil rows []]
      (if (= page 3)
        {:status :ok :turns (vec (keep evidence-row->turn rows)) :pages page}
        (let [url (str base-url "/api/alpha/evidence?author=joe&tags=turn&limit=100"
                       (when cursor (str "&cursor-at=" (enc (:at cursor))
                                         "&cursor-id=" (enc (:id cursor)))))
              resp (http/get url {:timeout timeout-ms :throw false})]
          (cond
            (= 503 (:status resp)) {:status :absent :reason :expensive-read-busy}
            (not= 200 (:status resp)) {:status :absent :reason :evidence-read-failed
                                       :http-status (:status resp)}
            :else
            (let [body (edn/read-string (:body resp))
                  rows' (into rows (:entries body))
                  next (:next-cursor body)]
              (if (or (nil? next) (>= (count rows') 300))
                {:status :ok :turns (vec (keep evidence-row->turn rows'))
                 :pages (inc page)}
                (recur (inc page) next rows')))))))
    (catch Exception e
      (if (timeout-exception? e)
        {:status :absent :reason :evidence-read-timeout}
        {:status :absent :reason :evidence-read-failed :message (.getMessage e)}))))

(defn manifest
  [turns vote area-corpus-sha opts]
  (let [mapped (into {} (for [seat (:seat-votes vote)
                              turn (:turns seat)]
                          [(:evidence-id turn) turn]))]
    {:schema :wm/operator-focus-manifest-v1
     :source :futon1b-evidence-landscape
     :as-of (:as-of vote) :window-days (:window-days vote)
     :K (:seat-count vote) :turns-per-seat (:turns-per-seat vote)
     :keyword-table-version keyword-table-version
     :area-corpus-sha256 area-corpus-sha
     :turns (mapv (fn [t]
                    (let [classification (get mapped (:evidence-id t))]
                      (merge {:evidence-id (:evidence-id t) :at (str (:at t)) :seat (:seat t)
                              :text-sha256 (identity/sha256 (str (:text t)))}
                             (if classification
                               (select-keys classification [:area :areas :step :tasks :task :reason])
                               {:step :not-consumed
                                :reason :outside-selected-window-or-seat-cap}))))
                  turns)
     :seat-votes (:seat-votes vote) :abstentions (:abstentions vote)
     :result (:result vote) :reader (select-keys opts [:base-url])}))

(defn write-manifest!
  "Write MANIFEST as content-addressed EDN under DIR and return its pin."
  [dir m]
  (let [content (str (pr-str m) "\n")
        sha (identity/sha256 content)
        f (io/file dir (str "operator-focus-" sha ".edn"))]
    (.mkdirs (.getParentFile f))
    (spit f content)
    {:path (.getCanonicalPath f) :sha256 sha :manifest m}))

(defn replay [path]
  (let [m (edn/read-string (slurp path))]
    {:manifest m :result (:result m)}))

(defn read-focus!
  "Read, vote, and retain exactly the authoritative turns read."
  [{:keys [area-corpus-path manifest-dir as-of] :as opts}]
  (let [read-result (read-turns opts)]
    (if (not= :ok (:status read-result))
      read-result
      (let [{:keys [rows sha256]} (load-area-corpus area-corpus-path)
            vote (focus-vote (:turns read-result) rows
                             (merge {:as-of (or as-of (Instant/now))} opts))
            m (manifest (:turns read-result) vote sha256 opts)]
        (merge {:status :ok :vote vote} (write-manifest! manifest-dir m))))))
