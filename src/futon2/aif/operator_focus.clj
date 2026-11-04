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

(def keyword-table-version "operator-focus-keywords-v2")

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
   ;; Futonzero includes its prelim-practice, self-play, MVP and generative work.
   {:area "futonzero" :pattern #"(?i)\b(futonzero|futon zero|self.?play)\b"}
   ;; futon5 is a named repository/subsystem.
   {:area "futon5" :pattern #"(?i)\bfuton5\b"}
   ;; futon6 is a named repository/subsystem.
   {:area "futon6" :pattern #"(?i)\bfuton6\b"}
   ;; Business covers explicit commercial/customer/company work.
   {:area "business" :pattern #"(?i)\b(business|customer|commercial|sales|company)\b"}
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

(defn map-turn
  "Map one normalized turn's text to an area, recording task/keyword precedence."
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
        {:areas [] :step :abstain :reason :turn-unmapped}))))

(defn- instant [x]
  (cond (instance? Instant x) x
        (number? x) (Instant/ofEpochMilli (long x))
        (string? x) (Instant/parse x)
        :else (throw (ex-info "as-of/turn time must be an Instant or ISO string" {:value x}))))

(defn focus-vote
  "Pure vote over normalized turns. `area-of-task` maps task id to area.
   Options: `:as-of`, `:window-days` (7), `:seat-count` (10), and
   `:turns-per-seat` (10)."
  [turns area-of-task {:keys [as-of window-days seat-count turns-per-seat clocks]
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
        clocks-by-seat (into {} (map (juxt :seat identity)) clocks)
        seat-results
        (mapv (fn [seat]
                (let [turns (take turns-per-seat (get per-seat seat))
                      mapped (mapv #(assoc (map-turn % area-of-task)
                                           :evidence-id (:evidence-id %)
                                           :at (str (:at %))) turns)
                      areas (mapcat :areas mapped)
                      text-half (if (seq areas)
                                  (let [winners (majority areas)
                                        share (/ 1 (* 2 (count winners)))]
                                    {:status :voted
                                     :votes (into (sorted-map)
                                                  (map (fn [a] [a share]) winners))})
                                  {:status :abstained :reason :no-mappable-turns})
                      clock (get clocks-by-seat seat)
                      clock-half
                      (cond
                        (nil? clock) {:status :abstained :reason :clock-absent}
                        (.isBefore (instant (:at clock)) since)
                        {:status :abstained :reason :clock-outside-window
                         :at (str (:at clock)) :target (:target clock)}
                        (.isAfter (instant (:at clock)) as-of)
                        {:status :abstained :reason :clock-after-as-of
                         :at (str (:at clock)) :target (:target clock)}
                        :else
                        (if-let [area (area-of-task (:target clock))]
                          {:status :voted :votes (sorted-map area 1/2)
                           :at (str (:at clock)) :target (:target clock)}
                          {:status :abstained :reason :clock-target-unmapped
                           :at (str (:at clock)) :target (:target clock)}))
                      votes (merge-with + (:votes text-half) (:votes clock-half))]
                  (cond-> {:seat seat
                           :status (if (seq votes) :voted :abstained)
                           :text-half text-half :clock-half clock-half
                           :turns mapped}
                    (seq votes) (assoc :votes (into (sorted-map) votes))
                    (empty? votes) (assoc :reason :both-halves-abstained))))
              seats)
        totals (reduce (fn [m {:keys [votes]}]
                         (reduce-kv #(update %1 %2 (fnil + 0) %3) m (or votes {})))
                       (sorted-map) seat-results)
        result (if (empty? totals)
                 {:status :absent :reason :no-recent-operator-turns}
                 (let [ordered (sort-by (juxt (comp - double val) key) totals)
                       threshold (val (nth ordered (min 2 (dec (count ordered)))))
                       retained (take-while #(>= (val %) threshold) ordered)
                       total (reduce + (vals totals))
                       ranked (mapv (fn [[area votes]]
                                      {:area area :votes votes :share (/ votes total)})
                                    retained)]
                   {:status :present
                    :focus (into (sorted-set) (map :area ranked))
                    :ranked ranked :total-votes total :totals totals}))]
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

(defn read-clocks
  "Read the bounded current clock projection and retain a hash of its exact response."
  [{:keys [base-url timeout-ms] :or {base-url "http://127.0.0.1:7073" timeout-ms 10000}}]
  (try
    (let [url (str base-url
                   "/api/alpha/hyperedges?type=clock%2Fclocked-on&limit=1000&include-total=false")
          resp (http/get url {:timeout timeout-ms :throw false})]
      (cond
        (= 503 (:status resp)) {:status :absent :reason :expensive-read-busy}
        (not= 200 (:status resp)) {:status :absent :reason :clock-read-failed
                                   :http-status (:status resp)}
        :else
        (let [body (:body resp)
              rows (:hyperedges (edn/read-string body))]
          {:status :ok :response-sha256 (identity/sha256 body)
           :clocks (mapv (fn [row]
                           (let [p (:hx/props row)]
                             {:seat (:agent-id p) :at (:clocked-at-ms p)
                              :target (or (:ticket-id p) (:excursion-id p)
                                          (:mission-id p) (:campaign-id p))}))
                         rows)})))
    (catch Exception e
      (if (timeout-exception? e)
        {:status :absent :reason :clock-read-timeout}
        {:status :absent :reason :clock-read-failed :message (.getMessage e)}))))

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
     :clock-response-sha256 (:clock-response-sha256 opts)
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
  (let [turn-read (read-turns opts)
        clock-read (when (= :ok (:status turn-read)) (read-clocks opts))]
    (if (not= :ok (:status turn-read))
      turn-read
      (if (not= :ok (:status clock-read))
        clock-read
      (let [{:keys [rows sha256]} (load-area-corpus area-corpus-path)
            vote-opts (merge {:as-of (or as-of (Instant/now))}
                             opts {:clocks (:clocks clock-read)})
            vote (focus-vote (:turns turn-read) rows vote-opts)
            manifest-opts (assoc opts :clock-response-sha256
                                 (:response-sha256 clock-read))
            m (manifest (:turns turn-read) vote sha256 manifest-opts)]
        (merge {:status :ok :vote vote} (write-manifest! manifest-dir m)))))))
