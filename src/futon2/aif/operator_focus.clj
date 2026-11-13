(ns futon2.aif.operator-focus
  "Current operator focus, voted by the seats Joe addressed recently.

   The evidence landscape is authoritative. Live reads are bounded and retained
   as content-addressed manifests; replay consumes only such a manifest."
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.handoff-contract :as handoff]
            [futon2.aif.substrate :as substrate])
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
        used (set (map :evidence-id newest))
        remaining (sort-by (comp str :at) #(compare %2 %1)
                           (remove #(used (:evidence-id %)) xs))]
    (vec (concat newest (take (- 20 (count newest)) remaining)))))

(defn- reply-json [raw]
  (let [start (str/index-of raw "{") end (str/last-index-of raw "}")]
    (when (and start end (< start end)) (subs raw start (inc end)))))

(defn check-reply [raw sent seats]
  (try
    (let [rows (:answers (json/parse-string (reply-json raw) true)) by (group-by :seat rows)
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
    (let [rows (:clock_answers (json/parse-string (reply-json raw) true)) by (group-by :target rows)
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
  [turns area-of-task {:keys [as-of window-days seat-count turns-per-seat text-answers]
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
        clocks-by-seat (into {}
                             (keep (fn [seat]
                                     (when-let [turn (first (filter :captured-target (get per-seat seat)))]
                                       [seat {:seat seat :at (:at turn) :target (:captured-target turn)
                                              :evidence-id (:evidence-id turn)}]))) seats)
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
                        :else (if-let [area (area-of-task (:target clock))]
                          {:status :voted :votes (sorted-map area 1/2)
                           :at (str (:at clock)) :target (:target clock)
                           :source-evidence-id (:evidence-id clock)}
                          {:status :abstained :reason :clock-target-unmapped
                           :at (str (:at clock)) :target (:target clock)
                           :source-evidence-id (:evidence-id clock)}))
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
                 (if (seq eligible)
                   ;; Turns were read inside the window but every seat abstained
                   ;; (e.g. an unparseable reader reply). Distinct from a truly
                   ;; quiet window so held-run diagnosis names the real cause.
                   {:status :absent :reason :all-seats-abstained}
                   {:status :absent :reason :no-recent-operator-turns})
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

(def ^:private as-of-cursor-id
  "An initial cursor id after ordinary evidence ids, so rows exactly at the
   requested event time are included by futon1b's strict keyset predicate."
  "\uffff")

(defn- timeout-exception? [e]
  (loop [e e]
    (cond (nil? e) false
          (or (instance? java.net.http.HttpTimeoutException e)
              (instance? java.net.SocketTimeoutException e)) true
          :else (recur (.getCause ^Throwable e)))))

(defn get-page-with-retry
  "GET one evidence page, honoring bounded 503 retry advice for the same URL."
  [url {:keys [timeout-ms http-get sleep-fn]
        :or {timeout-ms 10000 http-get http/get sleep-fn #(Thread/sleep (* 1000 %))}}]
  (let [started (System/nanoTime)]
    (loop [attempt 1 waited 0]
      (let [resp (http-get url {:timeout timeout-ms :throw false})
            advice (when (= 503 (:status resp))
                     (try (:retry-after-seconds (edn/read-string (:body resp)))
                          (catch Exception _ nil)))
            elapsed (/ (- (System/nanoTime) started) 1e9)]
        (if (and (= 503 (:status resp)) (number? advice) (< attempt 6)
                 (<= (+ waited advice) 30) (< (+ elapsed advice) 30))
          (do (sleep-fn advice) (recur (inc attempt) (+ waited advice)))
          {:response resp :attempts attempt})))))

(defn read-turns
  "Bounded authoritative read: at most three pages of 100. The first keyset
   cursor applies event-time as-of without XTDB's slower system-as-of path.
   No file fallback."
  [{:keys [base-url timeout-ms as-of] :as opts :or {base-url (substrate/configured-url) timeout-ms 10000}}]
  (try
    (let [as-of-instant (instant as-of)
          initial-cursor {:at (str as-of-instant) :id as-of-cursor-id}
          finish (fn [rows pages page-attempts]
                   {:status :ok
                    :turns (->> rows
                                (keep evidence-row->turn)
                                (filter #(not (.isAfter (instant (:at %)) as-of-instant)))
                                vec)
                    :pages pages
                    :page-attempts page-attempts
                    :as-of-mode :event-time-cursor
                    :initial-cursor initial-cursor})]
      (loop [page 0 cursor initial-cursor rows [] page-attempts []]
        (if (= page 3)
          (finish rows page page-attempts)
          (let [url (str base-url "/api/alpha/evidence?author=joe&tags=turn&limit=100"
                         "&cursor-at=" (enc (:at cursor))
                         "&cursor-id=" (enc (:id cursor)))
                {:keys [response attempts]} (get-page-with-retry url (assoc opts :timeout-ms timeout-ms))
                resp response
                attempts' (conj page-attempts attempts)]
            (cond
              (= 503 (:status resp)) {:status :absent :reason :expensive-read-busy :page-attempts attempts'}
              (not= 200 (:status resp)) {:status :absent :reason :evidence-read-failed
                                         :http-status (:status resp)}
              :else
              (let [body (edn/read-string (:body resp))
                    rows' (into rows (:entries body))
                    next (:next-cursor body)]
                (if (or (nil? next) (>= (count rows') 300))
                  (finish rows' (inc page) attempts')
                  (recur (inc page) next rows' attempts'))))))))
    (catch Exception e
      (if (timeout-exception? e)
        {:status :absent :reason :evidence-read-timeout}
        {:status :absent :reason :evidence-read-failed :message (.getMessage e)}))))

(defn read-clocks
  "Read the bounded current clock projection and retain a hash of its exact response."
  [{:keys [base-url timeout-ms as-of] :or {base-url (substrate/configured-url) timeout-ms 10000}}]
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
     :evidence-page-attempts (:evidence-page-attempts opts)
     :evidence-as-of-mode (:evidence-as-of-mode opts)
     :evidence-initial-cursor (:evidence-initial-cursor opts)
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

(def reader-contract
  "The operator-focus reading as a War Machine handoff (handoff-contract), so its
  reply is decoded and validated by the common boundary like every other handoff.
  It was dispatched as a work-mode requisition bell (kimi-task.sh) and parsed by
  hand; on Lucy, 2026-11-13, two seats answered correctly in prose while trying to
  deliver by bell and IRC, the reply never parsed, focus was absent, and a click
  stopped :outer-score/no-admitted-tasks (wm-click-3c5a5b02)."
  (handoff/contract
   {:id :wm/operator-focus-reading :version 1
    :response {:answers [{:seat "receiving seat" :area "A7 area or other:<name>"
                          :evidence_ids ["evidence ids of that seat's sent turns, verbatim"]}]
               :clock_answers [{:target "clock target id" :area "A7 area" :line "cited line number"}]}
    :validator {:id :operator-focus/check-reply :version 1}}))

(defn reader-prompt
  "The reader's prompt for one ISSUED request: the reader resource, the A7 area
  authority, the inputs, the contract, and on a format-correction round the
  parser's complaint. Brief mode: reply only, no tool use, no delivery."
  [{:keys [prompt format-correction]}]
  (str prompt
       (when format-correction
         (str "\n\nYour previous reply was not one JSON object ("
              (:parser-error format-correction)
              "). Reply with exactly one JSON object following the contract above, and nothing else."))))

(defn dispatch-reader!
  "Ask the reader seat through the handoff boundary. READER-CONTACT (from the
  runtime: designated-agent-answer-fn, brief mode) receives the request; without
  one the exchange is a typed :not-contacted. Returns the shape read-focus! reads."
  [sent {:keys [prompt-resource reader-seat reader-contact clock-inputs a7-path code-root]
         :or {reader-seat "猴-1"}}]
  (let [code-root (or code-root (System/getenv "FUTON_CODE_ROOT"))
        _ (when (str/blank? code-root) (throw (ex-info "FUTON_CODE_ROOT is required" {:reason :code-root-absent})))
        a7-path (or a7-path (str code-root "/futon3c/holes/excursions/ant-prompts/A7-relate.md"))
        resource (slurp (io/resource prompt-resource))
        a7 (slurp a7-path)
        clipped (mapv #(update % :text (fn [s] (subs (str s) 0 (min 600 (count (str s)))))) sent)
        prompt (str resource "\n\nA7 AREA AUTHORITY (VERBATIM):\n" a7
                    "\n\nINPUT TURNS (JSON):\n" (json/generate-string clipped)
                    "\n\nCLOCK TARGETS (JSON):\n" (json/generate-string clock-inputs)
                    "\n\n" (handoff/prompt-block reader-contract))
        request {:kind :operator-focus-reading :target "operator-focus" :prompt prompt}
        base {:seat reader-seat :prompt-sha256 (identity/sha256 resource)
              :area-authority-sha256 (identity/sha256 a7) :sent-turns clipped
              :contract (select-keys reader-contract [:id :version])}
        formatted (when (fn? reader-contact)
                    (handoff/contact-with-format-correction
                     {:contact reader-contact :request request :decode handoff/response-map}))
        exchange (handoff/exchange
                  {:contact (when formatted (constantly (:response formatted)))
                   ;; exchange decodes the payload string (handoff/response-payload),
                   ;; the correction round decodes the job envelope (response-map)
                   :request request :decode handoff/response-json-object
                   :validate (fn [m] (when-not (sequential? (:answers m))
                                       (throw (ex-info "reply lacks answers" {:reply m})))
                               m)})]
    (if (= :validated (:status exchange))
      (let [raw (json/generate-string (:value exchange))]
        (assoc base :status :ok :job-id (:job-id exchange) :raw-reply raw
               :raw-reply-sha256 (identity/sha256 raw)
               :format-correction (:format-correction formatted)))
      (assoc base :status :absent :reason :reader-handoff-refused
             :handoff (dissoc exchange :raw-answer)
             :format-correction (:format-correction formatted)))))

(defn read-focus!
  "Read, vote, and retain exactly the authoritative turns read."
  [{:keys [area-corpus-path manifest-dir as-of] :as opts}]
  (let [opts (assoc opts :as-of (or as-of (str (Instant/now))))
        turn-read (read-turns opts)
        ]
    (if (not= :ok (:status turn-read))
      turn-read
      (let [{:keys [rows sha256]} (load-area-corpus area-corpus-path)
            preliminary (focus-vote (:turns turn-read) rows opts)
            sent (reader-turns (:turns turn-read) (:seats preliminary))
            clock-targets (->> (:seat-votes preliminary) (keep #(get-in % [:clock-half :target]))
                               distinct (remove rows))
            resolved (keep #(resolve-task (or (:code-root opts) (System/getenv "FUTON_CODE_ROOT")) %) clock-targets)
            dispatch-opts (assoc opts :clock-inputs resolved)
            dispatch (if (:reader-raw-reply opts)
                       (let [root (or (:code-root opts) (System/getenv "FUTON_CODE_ROOT"))
                             a7 (slurp (or (:a7-path opts) (str root "/futon3c/holes/excursions/ant-prompts/A7-relate.md")))]
                         {:status :ok :seat (:reader-seat opts) :job-id (:reader-job-id opts)
                          :prompt-sha256 (identity/sha256 (slurp (io/resource (:prompt-resource opts))))
                          :area-authority-sha256 (identity/sha256 a7)})
                       (dispatch-reader! sent dispatch-opts))
            raw (or (:reader-raw-reply opts) (:raw-reply dispatch))
            answers (if raw (check-reply raw sent (:seats preliminary))
                        (into {} (map (fn [seat] [seat {:status :abstained
                                                       :reason (or (:reason dispatch) :reader-failed)}])
                                      (:seats preliminary))))
            clock-answers (when raw (check-clock-reply raw resolved))
            area-map (merge rows (into {} (keep (fn [[t a]] (when (= :voted (:status a)) [t (:area a)])) clock-answers)))
            vote (focus-vote (:turns turn-read) area-map (assoc opts :text-answers answers))
            manifest-opts (assoc opts :evidence-page-attempts (:page-attempts turn-read)
                                 :evidence-as-of-mode (:as-of-mode turn-read)
                                 :evidence-initial-cursor (:initial-cursor turn-read)
                                 :reader {:sent-turns (mapv #(assoc (select-keys % [:evidence-id :seat])
                                                                   :body-sha256 (identity/sha256 (str (:text %)))) sent)
                                          :prompt-sha256 (:prompt-sha256 dispatch)
                                          :area-authority-sha256 (:area-authority-sha256 dispatch) :seat (:seat dispatch)
                                          :job-id (:job-id dispatch) :raw-reply-sha256 (when raw (identity/sha256 raw))
                                          :parsed-answers answers :clock-inputs (mapv #(dissoc % :text) resolved)
                                          :parsed-clock-answers clock-answers})
            m (manifest (:turns turn-read) vote sha256 manifest-opts)]
        (merge {:status :ok :vote vote :reader-status (:status dispatch)
                :reader-reason (:reason dispatch)}
               (write-manifest! manifest-dir m))))))
