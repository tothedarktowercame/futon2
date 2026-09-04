#!/usr/bin/env bb
;; phase_window_cross.bb -- the two-vocabulary cross, run on a lifecycle day.
;;
;; Census run 1 could not run Joe's cross (pattern tags x mission-phase words)
;; because the pinned ruling window contains no phase words. This runs it on
;; 2026-06-01, the day M-vsatarcs-invariants-integration went IDENTIFY ->
;; INSTANTIATE, and runs the control that says how much of the result is the
;; day's topic rather than the operator's clocking act.
;;
;;   bb phase_window_cross.bb [out-file]
;;
;; Read only. Writes phase-cross-2026-06-01.edn.

(require '[babashka.process :refer [shell]]
         '[clojure.string :as str]
         '[clojure.edn :as edn]
         '[clojure.pprint])

(def base (or (System/getenv "FUTON1B_URL") "http://localhost:7073"))
(def day "2026-06-01")
(def from (str day "T00:00:00Z"))
(def to "2026-06-02T00:00:00Z")
(def out (or (first *command-line-args*)
             "/home/joe/code/futon2/holes/labs/E-operator-as-attached-agent/phase-cross-2026-06-01.edn"))

(def words ["identify" "map" "derive" "argue" "verify" "instantiate" "document" "survey"])

(defn- get-edn! [url]
  (loop [attempt 1]
    (let [{:keys [out exit]} (shell {:out :string :continue true}
                                    "curl" "-s" "-m" "300" "-w" "\n%{http_code}" url)
          _ (when-not (zero? exit) (throw (ex-info "curl failed" {:url url})))
          i (str/last-index-of out "\n")
          body (subs out 0 i) code (str/trim (subs out (inc i)))]
      (cond
        (= "200" code) (let [d (edn/read-string body)]
                         (when (false? (:ok d)) (throw (ex-info "not ok" {:url url})))
                         d)
        (and (= "503" code) (< attempt 6)) (do (Thread/sleep (* 1500 attempt)) (recur (inc attempt)))
        :else (throw (ex-info "store read failed" {:url url :http code}))))))

(defn- unesc [s] (-> (str s) (str/replace "\\n" "\n") (str/replace "\\\"" "\"")))
(def envelope-re #"(?s)^--- CURRENT TURN ---\n(?:[A-Za-z-]+: [^\n]*\n)+---\n\n")
(defn- q-of [e] (unesc (second (re-find #"(?s)\"query\" \"(.*?)\", \"results\"" (str (:evidence/body e))))))
(defn- pats [e] (vec (map second (re-seq #":id \"([^\"]+)\"" (str (:evidence/body e))))))
(defn- payload [q] (str/replace-first q envelope-re ""))
(defn- hits [t] (vec (filter #(re-find (re-pattern (str "(?i)\\b" %)) t) words)))
(defn- on-day? [e] (str/starts-with? (str (:evidence/at e)) day))

(def turns
  (->> (get-edn! (str base "/api/alpha/evidence?author=joe&since=" from "&before=" to "&limit=1000"))
       :entries (filter on-day?) (filter #(string? (get-in % [:evidence/body :text])))
       (sort-by :evidence/at)))

(def rets
  (->> (get-edn! (str base "/api/alpha/evidence/text-search?q=context-retrieval&since=" from
                       "&before=" to "&limit=500"))
       :results (map :entry) (filter on-day?) (sort-by :evidence/at)))

(defn- match [turn]
  (let [t0 (str (:evidence/at turn)) text (get-in turn [:evidence/body :text])
        head (subs text 0 (min 20 (count text)))
        later (filter #(pos? (compare (str (:evidence/at %)) t0)) rets)]
    (or (some->> later (filter #(str/starts-with? (payload (q-of %)) head)) first (#(vector % :content)))
        (some->> later
                 (filter (fn [r] (let [q (q-of r) p (payload q)]
                                   (and (not= q p) (pos? (count p)) (str/starts-with? text p)))))
                 first (#(vector % :envelope))))))

(def phase-turns (filter #(seq (hits (get-in % [:evidence/body :text]))) turns))
(def matched (keep (fn [t] (when-let [m (match t)] [t m])) phase-turns))
(def top-pattern "futon-theory/mission-interface-signature")

(defn- fires? [r] (boolean (some #{top-pattern} (pats r))))

(spit out
 (with-out-str
  (clojure.pprint/pprint
   {:day day
    :mission "M-vsatarcs-invariants-integration (futon4, IDENTIFY->INSTANTIATE in one day)"
    :vocabulary words

    :operator-turns
    {:total (count turns)
     :carrying-a-phase-word (count phase-turns)
     :fraction (double (/ (count phase-turns) (count turns)))
     :per-word (into {} (for [w words]
                          [w (count (filter #(re-find (re-pattern (str "(?i)\\b" w))
                                                      (get-in % [:evidence/body :text])) turns))]))}

    :cross
    {:phase-turns-with-a-retrieval-record (count matched)
     :join-stages (frequencies (map (comp second second) matched))
     :rows (mapv (fn [[t [r stage]]]
                   {:at (:evidence/at t)
                    :phase-words (hits (get-in t [:evidence/body :text]))
                    :head (let [s (str/replace (get-in t [:evidence/body :text]) #"\s+" " ")]
                            (subs s 0 (min 90 (count s))))
                    :retrieval-agent (:evidence/author r)
                    :join stage
                    :patterns (pats r)})
                 matched)
     :unmatched (mapv (fn [t] {:at (:evidence/at t) :id (:evidence/id t)})
                      (remove (set (map first matched)) phase-turns))
     :pattern-frequencies (into (sorted-map) (frequencies (mapcat (fn [[_ [r _]]] (pats r)) matched)))}

    :control
    {:question "is the top pattern tracking the operator's clocking act, or the day's topic?"
     :top-pattern top-pattern
     :fires-on-phase-word-turns [(count (filter (fn [[_ [r _]]] (fires? r)) matched)) (count matched)]
     :base-rate-all-records-that-day
     [(count (filter fires? rets)) (count rets)
      (double (/ (count (filter fires? rets)) (count rets)))]
     :by-agent (into (sorted-map)
                     (for [a (distinct (map :evidence/author rets))
                           :let [as (filter #(= a (:evidence/author %)) rets)]]
                       [a {:records (count as) :fires (count (filter fires? as))}]))
     :verdict "enrichment above base rate is real but the day is saturated; half of every
               embedding input is the agent's reply, and that day's replies are mission-document
               text. The cross as run detects the day's topic, not the turn-level operator act."}

    :day-wide-pattern-frequencies
    (into [] (take 15 (sort-by (comp - val) (frequencies (mapcat pats rets)))))})))

(println "wrote" out)
(println " operator turns" (count turns) "| phase-word turns" (count phase-turns)
         "| matched" (count matched)
         "|" top-pattern "fires" (count (filter (fn [[_ [r _]]] (fires? r)) matched)) "of" (count matched)
         "vs base rate" (format "%.1f%%" (* 100.0 (/ (count (filter fires? rets)) (count rets)))))
