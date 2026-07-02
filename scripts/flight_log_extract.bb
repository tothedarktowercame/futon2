#!/usr/bin/env bb
;; flight_log_extract.bb — the sequel-notebook's map-reduce input adapter.
;;
;; Reads the WM trace (data/wm-trace/wm-trace-*.edn, EDN-lines — one record per
;; scheduled tick since R16 went live 2026-07-02) and emits FLIGHT ROWS: one
;; line per tick with the loop's full audit — gate verdicts (both legs + ΔG
;; source), enactment (executor boxes/holes), expected-vs-realized G, γ state,
;; and the belly's per-action G-goal-outcome spread. p4ng/sequel-notebook.org
;; consumes this as a runnable src block ("the paper's figures ARE the flights").
;;
;;   bb scripts/flight_log_extract.bb                  ; org table, today
;;   bb scripts/flight_log_extract.bb --since 2026-07-02 --format json
;;   bb scripts/flight_log_extract.bb --format json > /tmp/flights.json
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[cheshire.core :as json])

(def trace-dir (str (System/getProperty "user.home") "/code/futon2/data/wm-trace"))

(defn- read-records [since]
  (->> (file-seq (io/file trace-dir))
       (filter #(re-find #"wm-trace-\d{4}-\d{2}-\d{2}\.edn$" (.getName %)))
       (filter #(>= (compare (.getName %) (str "wm-trace-" since ".edn")) 0))
       (sort-by #(.getName %))
       (mapcat (fn [f]
                 (let [date (second (re-find #"wm-trace-(\d{4}-\d{2}-\d{2})" (.getName f)))]
                   (keep (fn [line]
                           (try (assoc (edn/read-string {:default (fn [_ v] v)} line) ::date date)
                                (catch Exception _ nil)))
                         (line-seq (io/reader f))))))))

(defn- flight-row [r]
  (let [verdicts (:act-gate-verdicts r)
        en (:enactment r)
        ro (:realized-outcome r)
        pp (:policy-precision r)
        gos (keep :G-goal-outcome (:ranked-actions r))
        vsum (frequencies (map :verdict verdicts))]
    {:t (:timestamp r)
     :mode (some-> (:mode r) name)
     :gates (some->> verdicts
                     (map #(str (some-> (:mission %) (str/replace #"^M-" ""))
                                ":" (some-> (:verdict %) name)))
                     (str/join " "))
     :pass (get vsum :pass 0)
     :enacted (some-> (:mission en) (str/replace #"^M-" ""))
     :dg-source (some-> (or (:predicted-via en) (:delta-G-source (first verdicts))) name)
     :boxes (:boxes en) :holes (:policy-holes en)
     :expected-G (:expected-G ro) :realized-G (:realized-G ro)
     :gamma (:policy-precision pp) :samples (:samples pp) :mean-perf (:mean-perf pp)
     :goal-outcome-distinct (count (distinct gos))
     :decision (let [a (get-in r [:decision :action])]
                 (cond (= :abstain a) "abstain"
                       (map? a) (str (some-> (:type a) name) ":"
                                     (or (:target a) (:target-class a)))
                       :else (str a)))}))

(defn -main []
  (let [args (vec *command-line-args*)
        opt (fn [k d] (let [i (.indexOf args k)] (if (neg? i) d (get args (inc i) d))))
        since (opt "--since" "2026-07-02")   ; R16 live-wire day = flight day 0
        fmt (opt "--format" "org")
        rows (->> (read-records since)
                  ;; a FLIGHT = a tick with the R16 audit present (post live-wire)
                  (filter :act-gate-verdicts)
                  (map flight-row))]
    (case fmt
      "json" (println (json/generate-string {:since since :flights (count rows) :rows rows}))
      ;; org table (the notebook's native food)
      (let [cols [:t :gates :enacted :dg-source :boxes :holes
                  :expected-G :realized-G :gamma :samples :mean-perf
                  :goal-outcome-distinct :decision]]
        (println (str "| " (str/join " | " (map name cols)) " |"))
        (println "|-")
        (doseq [row rows]
          (println (str "| " (str/join " | " (map #(let [v (get row %)]
                                                     (cond (nil? v) "—"
                                                           (double? v) (format "%.3f" v)
                                                           :else v)) cols)) " |")))
        (binding [*out* *err*]
          (println (str (count rows) " flights since " since
                        " · passes " (reduce + 0 (map :pass rows))
                        " · realized samples visible " (count (keep :realized-G rows)))))))))

(-main)
