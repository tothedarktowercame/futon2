#!/usr/bin/env bb
;; harvest_activations.bb -- page every context-retrieval record out of the
;; evidence store and write a compact activation table.
;;
;; The route the excursion's pilot cited, GET /api/alpha/patterns/activation,
;; does NOT exist on futon1b (:7073) -- it was a futon1a route and returns 404
;; today. This pages GET /api/alpha/evidence?tags=context-retrieval instead.
;;
;;   bb harvest_activations.bb [out-file]
;;
;; WINDOWED BY DAY, and the reason is a real defect found on 2026-09-04.
;; A single unwindowed cursor walk over the whole corpus reported 16,121
;; records and `exhausted true`, of which only 69 were dated 2026-09 -- while
;; the same filter with `since=2026-09-01` returns 1000 with a next-cursor,
;; and text-search agrees. So the route's keyset order is NOT :evidence/at
;; order (replayed documents carry old timestamps behind the write frontier --
;; futon1b README-fts section 3), and continuing from a `:next-cursor {:at ...}`
;; over the full corpus skips every match whose :evidence/at is newer than the
;; cursor but which had not been visited yet. Windowing by day bounds the
;; damage: each day is paged to exhaustion on its own, and each day's count is
;; cross-checked against text-search, which reads the FTS sidecar rather than
;; the same scan. A day whose two counts disagree is REPORTED, not smoothed.
;;
;; Read only. Writes one file: activations.edn

(require '[babashka.process :refer [shell]]
         '[clojure.string :as str]
         '[clojure.edn :as edn]
         '[clojure.pprint])

(def base (or (System/getenv "FUTON1B_URL") "http://localhost:7073"))
(def out (or (first *command-line-args*)
             "/home/joe/code/futon2/holes/labs/E-operator-as-attached-agent/activations.edn"))

(defn- get-edn! [url]
  (loop [attempt 1]
    (let [{:keys [out exit]} (shell {:out :string :continue true}
                                    "curl" "-s" "-m" "300" "-w" "\n%{http_code}" url)
          _ (when-not (zero? exit) (throw (ex-info "curl failed" {:url url})))
          i (str/last-index-of out "\n")
          body (subs out 0 i)
          code (str/trim (subs out (inc i)))]
      (cond
        (= "200" code) (edn/read-string body)
        (and (= "503" code) (< attempt 8)) (do (Thread/sleep (* 1500 attempt)) (recur (inc attempt)))
        :else (throw (ex-info "store read failed" {:url url :http code}))))))

(defn- parse-results
  "One {:id :score} per result map. The title span between them may carry
   escaped quotes, so the id->score hop is non-greedy rather than field-typed."
  [body]
  (mapv (fn [[_ id score]] {:id id :score (Double/parseDouble score)})
        (re-seq #"(?s):id \"([^\"]+)\".*?:score ([0-9.eE+-]+)" body)))

(defn- day-seq [from to]
  (->> (iterate #(.plusDays ^java.time.LocalDate % 1) (java.time.LocalDate/parse from))
       (take-while #(not (.isAfter ^java.time.LocalDate % (java.time.LocalDate/parse to))))
       (map str)))

(defn- page-day
  "Every matching record for one UTC day, paged to exhaustion."
  [day]
  (let [from (str day "T00:00:00Z")
        to (str (.plusDays (java.time.LocalDate/parse day) 1) "T00:00:00Z")]
    (loop [cursor nil page 0 acc []]
      (let [url (str base "/api/alpha/evidence?tags=context-retrieval"
                     "&since=" from "&before=" to "&limit=1000"
                     (when cursor (str "&cursor-at=" (:at cursor) "&cursor-id=" (:id cursor))))
            d (get-edn! url)
            acc (into acc (:entries d))
            nc (:next-cursor d)]
        (if (and nc (< page 40))
          (recur nc (inc page) acc)
          {:entries acc :incomplete (boolean (:incomplete d)) :pages (inc page)})))))

(defn- fts-count
  "Independent count of the same day from the FTS sidecar, as a cross-check."
  [day]
  (let [from (str day "T00:00:00Z")
        to (str (.plusDays (java.time.LocalDate/parse day) 1) "T00:00:00Z")
        d (get-edn! (str base "/api/alpha/evidence/text-search?q=context-retrieval"
                         "&since=" from "&before=" to "&limit=1000"))]
    (count (:results d))))

(defn- row [e]
  (let [b (str (:evidence/body e))
        q (second (re-find #"(?s)\"query\" \"(.*?)\", \"results\"" b))]
    {:at (str (:evidence/at e))
     :agent (:evidence/author e)
     :session (:evidence/session-id e)
     :query-chars (count (str/replace (str q) "\\n" "\n"))
     :results (parse-results b)}))

(let [days (day-seq "2026-04-13" (str (java.time.LocalDate/now)))
      out-rows (atom [])
      checks (atom [])]
  (doseq [day days]
    (let [{:keys [entries incomplete pages]} (page-day day)
          n (count entries)
          fts (when (pos? n) (fts-count day))]
      (swap! out-rows into (map row entries))
      (when (pos? n)
        (swap! checks conj {:day day :scan n :fts fts :pages pages
                            :incomplete incomplete
                            :agree? (= n fts)
                            :fts-capped? (= fts 1000)}))
      (binding [*out* *err*]
        (when (pos? n)
          (println (format "%s scan=%-5d fts=%-5d pages=%d%s"
                           day n (or fts -1) pages
                           (if (= n fts) "" "  <-- DISAGREE")))))))
  (let [rows @out-rows
        cs @checks
        ats (map :at rows)]
    (spit out (with-out-str
                (clojure.pprint/pprint
                 {:source "GET /api/alpha/evidence?tags=context-retrieval, windowed by UTC day"
                  :cross-check "GET /api/alpha/evidence/text-search?q=context-retrieval, same window"
                  :days-with-records (count cs)
                  :record-count (count rows)
                  :days-where-the-two-counts-disagree
                  (vec (remove :agree? cs))
                  :days-where-fts-hit-its-1000-cap (vec (filter :fts-capped? cs))
                  :earliest (when (seq ats) (first (sort ats)))
                  :latest (when (seq ats) (last (sort ats)))
                  :per-day (vec cs)
                  :records rows})))
    (println "wrote" out "records" (count rows)
             "days" (count cs)
             "disagreements" (count (remove :agree? cs)))))
