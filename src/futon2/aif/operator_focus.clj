(ns futon2.aif.operator-focus
  "Current operator focus, voted by the seats Joe addressed recently.

   The evidence landscape is authoritative. Live reads are bounded and retained
   as content-addressed manifests; replay consumes only such a manifest."
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [futon2.aif.action-identity :as identity])
  (:import [java.net URLEncoder]
           [java.time Duration Instant]))

(def area-vocabulary #{"WM" "Giraffe" "patterns" "APM" "Arxana" "Agency" "memory" "象"
                       "futon6" "voxterm" "futon5" "futonzero" "business"})

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

(def ^:private seat-pattern #"(.+)-turn-[0-9]+")

(defn reader-turns [turns seats]
  (let [chosen (set seats) xs (filter #(chosen (:seat %)) turns)
        newest (map (fn [seat] (first (sort-by (comp str :at) (fn [a b] (compare b a))
                                               (filter (fn [t] (= seat (:seat t))) xs)))) seats)
        used (set (map :evidence-id newest))]
    (->> (concat newest (remove #(used (:evidence-id %)) xs))
         (sort-by (comp str :at) #(compare %2 %1)) (take 20) vec)))

(defn check-reply [raw sent seats]
  (try
    (let [rows (:answers (json/parse-string raw true)) by (group-by :seat rows)
          ids (update-vals (group-by :seat sent) #(set (map :evidence-id %)))]
      (into {} (for [s seats :let [a (first (get by s)) cited (set (:evidence_ids a))]]
                 [s (cond (not= 1 (count (get by s))) {:status :abstained :reason :reader-answer-missing-or-duplicate}
                          (not (or (area-vocabulary (:area a)) (str/starts-with? (str (:area a)) "other:"))) {:status :abstained :reason :reader-unknown-area}
                          (or (empty? cited) (not (every? (get ids s #{}) cited))) {:status :abstained :reason :reader-unknown-evidence-id}
                          :else {:status :voted :area (:area a) :evidence-ids (vec cited)})])))
    (catch Exception _ (into {} (map (fn [s] [s {:status :abstained :reason :reader-invalid-json}]) seats)))))

(defn resolve-task [code-root target]
  (some (fn [repo]
          (some (fn [sub] (let [f (io/file repo sub (str target ".md"))]
                            (when (.isFile f) {:target target :path (.getCanonicalPath f)
                                               :text (str/join "\n" (take 20 (str/split-lines (slurp f))))})))
                ["holes/missions" "holes/excursions" "holes/tickets" "holes"]))
        (sort-by #(.getName %) (filter #(.isDirectory %) (or (.listFiles (io/file code-root)) [])))))

(defn check-clock-reply [raw clock-inputs]
  (try
    (let [rows (:clock_answers (json/parse-string raw true)) by (group-by :target rows)
          inputs (into {} (map (juxt :target identity)) clock-inputs)]
      (into {} (for [[target input] inputs :let [a (first (get by target))
                                                lines (str/split-lines (:text input)) line (:line a)]]
                 [target (cond (not= 1 (count (get by target))) {:status :abstained :reason :reader-clock-answer-missing-or-duplicate}
                               (not (or (area-vocabulary (:area a)) (str/starts-with? (str (:area a)) "other:"))) {:status :abstained :reason :reader-unknown-area}
                               (or (not (int? line)) (< line 1) (> line (count lines)) (str/blank? (nth lines (dec line))))
                               {:status :abstained :reason :reader-invalid-line-citation}
                               :else {:status :voted :area (:area a) :line line})])))
    (catch Exception _ {})))

(defn- instant [x]
  (cond (instance? Instant x) x
        (number? x) (Instant/ofEpochMilli (long x))
        (string? x) (Instant/parse x)
        :else (throw (ex-info "as-of/turn time must be an Instant or ISO string" {:value x}))))

(defn focus-vote
  "Pure vote over normalized turns. `area-of-task` maps task id to area.
   Options: `:as-of`, `:window-days` (7), `:seat-count` (10), and
   `:turns-per-seat` (10)."
  [turns area-of-task {:keys [as-of window-days seat-count turns-per-seat clocks text-answers]
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
                      answer (get text-answers seat)
                      text-half (if (= :voted (:status answer))
                                  (assoc answer :votes (sorted-map (:area answer) 1/2))
                                  (or answer {:status :abstained :reason :reader-answer-missing}))
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
                           :turns (mapv #(select-keys % [:evidence-id :at]) turns)}
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
  [{:keys [base-url timeout-ms as-of] :or {base-url "http://127.0.0.1:7073" timeout-ms 10000}}]
  (try
    (loop [page 0 cursor nil rows []]
      (if (= page 3)
        {:status :ok :turns (vec (keep evidence-row->turn rows)) :pages page}
        (let [url (str base-url "/api/alpha/evidence?author=joe&tags=turn&limit=100&system-as-of=" (enc as-of)
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
  [{:keys [base-url timeout-ms as-of] :or {base-url "http://127.0.0.1:7073" timeout-ms 10000}}]
  (try
    (let [url (str base-url
                   "/api/alpha/hyperedges?type=clock%2Fclocked-on&limit=1000&include-total=false&system-as-of=" (enc as-of))
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
     :result (:result vote) :reader (:reader opts)}))

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

(defn dispatch-reader! [sent {:keys [prompt-resource reader-seat caller kimi-script task-repo agency-base reader-timeout-ms clock-inputs]
                              :or {reader-seat "zai-1" caller "operator-focus"
                                   kimi-script "/home/joe/code/futon3c/scripts/kimi-task.sh"
                                   agency-base "http://127.0.0.1:7070" reader-timeout-ms 600000}}]
  (let [resource (slurp (io/resource prompt-resource))
        f (java.io.File/createTempFile "operator-focus-" ".md")
        clipped (mapv #(update % :text (fn [s] (subs (str s) 0 (min 600 (count (str s)))))) sent)
        _ (spit f (str resource "\n\nINPUT TURNS (JSON):\n" (json/generate-string clipped)
                       "\n\nCLOCK TARGETS (JSON):\n" (json/generate-string clock-inputs)))
        r (shell/sh kimi-script "--from" caller "--to" reader-seat
                    "--purpose" "read operator focus areas" (.getPath f)
                    :env (assoc (into {} (System/getenv)) "KIMI_TASK_REPO" (or task-repo (System/getProperty "user.dir"))))
        job (second (re-find #"job=(\S+)" (:out r)))]
    (if-not (and (zero? (:exit r)) job)
      {:status :absent :reason :reader-dispatch-failed :stderr (:err r)}
      (loop [left reader-timeout-ms]
        (let [response (http/get (str agency-base "/api/alpha/invoke/jobs/" job) {:timeout 10000 :throw false})
              body (when (= 200 (:status response)) (json/parse-string (:body response) true))
              j (or (:job body) body) state (or (:status j) (:state j)) raw (or (:result j) (:response j))]
          (cond raw {:status :ok :job-id job :seat reader-seat :prompt-sha256 (identity/sha256 resource)
                     :raw-reply raw :raw-reply-sha256 (identity/sha256 raw) :sent-turns clipped}
                (contains? #{"failed" "cancelled" :failed :cancelled} state) {:status :absent :reason :reader-job-failed :job-id job}
                (<= left 0) {:status :absent :reason :reader-job-timeout :job-id job}
                :else (do (Thread/sleep 2000) (recur (- left 2000)))))))))

(defn read-focus!
  "Read, vote, and retain exactly the authoritative turns read."
  [{:keys [area-corpus-path manifest-dir as-of] :as opts}]
  (let [opts (assoc opts :as-of (or as-of (str (Instant/now))))
        turn-read (read-turns opts)
        clock-read (when (= :ok (:status turn-read)) (read-clocks opts))]
    (if (not= :ok (:status turn-read))
      turn-read
      (if (not= :ok (:status clock-read))
        clock-read
      (let [{:keys [rows sha256]} (load-area-corpus area-corpus-path)
            preliminary (focus-vote (:turns turn-read) rows (assoc opts :clocks (:clocks clock-read)))
            sent (reader-turns (:turns turn-read) (:seats preliminary))
            clock-targets (->> (:clocks clock-read) (filter #(some #{(:seat %)} (:seats preliminary)))
                               (map :target) distinct (remove rows))
            resolved (keep #(resolve-task (or (:code-root opts) "/home/joe/code") %) clock-targets)
            dispatch-opts (assoc opts :clock-inputs resolved)
            dispatch (if (:reader-raw-reply opts)
                       {:status :ok :seat (:reader-seat opts) :job-id (:reader-job-id opts)
                        :prompt-sha256 (identity/sha256 (slurp (io/resource (:prompt-resource opts))))}
                       (dispatch-reader! sent dispatch-opts))
            raw (or (:reader-raw-reply opts) (:raw-reply dispatch))
            answers (when raw (check-reply raw sent (:seats preliminary)))
            clock-answers (when raw (check-clock-reply raw resolved))
            area-map (merge rows (into {} (keep (fn [[t a]] (when (= :voted (:status a)) [t (:area a)])) clock-answers)))
            vote (focus-vote (:turns turn-read) area-map (assoc opts :clocks (:clocks clock-read) :text-answers answers))
            manifest-opts (assoc opts :clock-response-sha256
                                 (:response-sha256 clock-read)
                                 :reader {:sent-turns (mapv #(assoc (select-keys % [:evidence-id :seat])
                                                                   :body-sha256 (identity/sha256 (str (:text %)))) sent)
                                          :prompt-sha256 (:prompt-sha256 dispatch) :seat (:seat dispatch)
                                          :job-id (:job-id dispatch) :raw-reply-sha256 (when raw (identity/sha256 raw))
                                          :parsed-answers answers :clock-inputs (mapv #(dissoc % :text) resolved)
                                          :parsed-clock-answers clock-answers})
            m (manifest (:turns turn-read) vote sha256 manifest-opts)]
        (if raw (merge {:status :ok :vote vote} (write-manifest! manifest-dir m)) dispatch))))))
