#!/usr/bin/env bb
;; operator_turn_census.bb -- E-operator-as-attached-agent, census run 1.
;;
;; Reads the evidence store over HTTP, pins the 2026-09-03 ruling window, and
;; writes four artifacts. READ ONLY: no evidence append, no registry write, no
;; ledger append, no board row. The only traffic is GET.
;;
;;   bb operator_turn_census.bb [out-dir]
;;
;; Artifacts (byte-identical on consecutive runs -- no wall-clock field is
;; written, and the window is in the past):
;;   00-window-turns.edn   every author=joe store record in the window
;;   01-retrievals.edn     every context-retrieval record in the window
;;   02-census.edn         the join: turn x tags x classification x receipts
;;   03-coverage.edn       the denominators, stated as denominators
;;
;; Per futon1b README-fts: check :ok, treat 503 as retry, never as empty.

(require '[babashka.process :refer [shell]]
         '[clojure.string :as str]
         '[clojure.java.io :as io]
         '[clojure.pprint]
         '[clojure.edn :as edn])

(def base (or (System/getenv "FUTON1B_URL") "http://localhost:7073"))
(def from "2026-09-03T20:50:00Z")
(def to   "2026-09-03T23:30:00Z")
(def here (.getParent (io/file *file*)))
(def out-dir (or (first *command-line-args*) here))

(defn- get-edn!
  "GET url, retrying 503 :expensive-read-busy with backoff. Never treats a
   non-200 or an :ok false as an empty result."
  [url]
  (loop [attempt 1]
    (let [{:keys [out exit]} (shell {:out :string :continue true}
                                    "curl" "-s" "-m" "180" "-w" "\n%{http_code}" url)
          _ (when-not (zero? exit) (throw (ex-info "curl failed" {:url url :exit exit})))
          idx (str/last-index-of out "\n")
          body (subs out 0 idx)
          code (str/trim (subs out (inc idx)))]
      (cond
        (= "200" code)
        (let [d (edn/read-string body)]
          (when (false? (:ok d)) (throw (ex-info "store reported not-ok" {:url url :body d})))
          d)

        (and (= "503" code) (< attempt 6))
        (do (Thread/sleep (* 1000 attempt)) (recur (inc attempt)))

        :else
        (throw (ex-info "store read failed" {:url url :http code :attempt attempt}))))))

(defn- in-window? [e]
  (let [a (str (:evidence/at e))]
    (and (pos? (compare a from)) (neg? (compare a to)))))

(defn- unescape
  "Bodies are stored as strings carrying literal backslash escapes. Turn those
   back into the characters they stand for before measuring lengths, or every
   count is wrong by the number of line breaks and quoted spans -- which is how
   a 100-character truncation reads as 102."
  [s] (-> (str s)
          (str/replace "\\n" "\n")
          (str/replace "\\\"" "\"")
          (str/replace "\\t" "\t")))

;; ---------------------------------------------------------------- turns

(def all-joe
  (:entries (get-edn! (str base "/api/alpha/evidence?author=joe&since=" from "&limit=1000"))))

(def window-records (->> all-joe (filter in-window?) (sort-by :evidence/at)))

(defn- turn-text [e] (get-in e [:evidence/body :text]))
(defn- session-start? [e] (nil? (turn-text e)))

(def turns (remove session-start? window-records))
(def session-starts (filter session-start? window-records))

;; ------------------------------------------------------------ retrievals

(def retrievals
  (->> (get-edn! (str base "/api/alpha/evidence/text-search?q=context-retrieval&since=" from "&limit=200"))
       :results (map :entry) (filter in-window?) (sort-by :evidence/at)))

(def envelope-re #"(?s)^--- CURRENT TURN ---\nSurface: [^\n]*\nFrom: [^\n]*\nTo: [^\n]*\nOrigin: [^\n]*\n(?:Caller: [^\n]*\n)?(?:Edge: [^\n]*\n)?---\n\n")

(defn- retrieval-query [e]
  (unescape (second (re-find #"(?s)\"query\" \"(.*?)\", \"results\"" (str (:evidence/body e))))))

(defn- retrieval-patterns [e]
  (vec (map second (re-seq #":id \"([^\"]+)\"" (str (:evidence/body e))))))

(defn- strip-envelope [q]
  (let [m (re-find envelope-re q)]
    {:envelope-chars (count (or m "")) :payload (str/replace-first q envelope-re "")}))

(defn- after [turn]
  (let [t0 (str (:evidence/at turn))]
    (filter #(pos? (compare (str (:evidence/at %)) t0)) retrievals)))

(defn- match-for
  "Two-stage deterministic join, and the two stages are the finding.

   STAGE 1 (content join): the earliest context-retrieval record strictly after
   the turn whose query -- after any bell envelope is stripped -- starts with
   the turn's first 20 characters.

   STAGE 2 (envelope join): for turns stage 1 cannot reach, the earliest record
   after the turn whose query carries a bell envelope reading `From: joe` and
   whose surviving payload is a non-empty prefix of the turn text. A turn that
   needs stage 2 is a turn whose tags were computed from fewer than 20
   characters of the operator's own words -- which is why the stage is recorded
   on the row and not hidden inside the join.

   No match at either stage is a typed absence, not a silent drop."
  [turn]
  (let [text (turn-text turn)
        head (subs text 0 (min 20 (count text)))]
    (or (some->> (after turn)
                 (filter #(str/starts-with? (:payload (strip-envelope (retrieval-query %))) head))
                 first
                 (#(assoc {} :record % :join :content)))
        (some->> (after turn)
                 (filter (fn [r]
                           (let [q (retrieval-query r)
                                 {:keys [envelope-chars payload]} (strip-envelope q)]
                             (and (pos? envelope-chars)
                                  (re-find #"(?m)^From: joe$" q)
                                  (pos? (count payload))
                                  (str/starts-with? text payload)))))
                 first
                 (#(assoc {} :record % :join :envelope))))))

;; ---------------------------------------------------------- classification

(def classification (edn/read-string (slurp (io/file here "classification.edn"))))

(defn- census-row [turn]
  (let [text (turn-text turn)
        m (match-for turn)
        r (:record m)
        q (some-> r retrieval-query)
        {:keys [envelope-chars payload]} (if q (strip-envelope q) {:envelope-chars 0 :payload nil})
        cls (get-in classification [:turns (str (:evidence/id turn))])]
    (cond-> {:evidence/id (:evidence/id turn)
             :at (:evidence/at turn)
             :session (:evidence/session-id turn)
             :chars (count text)
             :verbatim-head (subs text 0 (min 120 (count text)))
             :kind (:kind cls)
             :node (:node cls)
             :receipt-grade (:receipt-grade cls)
             :receipt-count (count (:receipts cls))
             :receipts (:receipts cls)}
      cls (assoc :node-note (:node-note cls) :gloss (:gloss cls))
      r (assoc :retrieval/id (:evidence/id r)
               :retrieval/join (:join m)
               :retrieval/agent (:evidence/author r)
               :retrieval/lag-s (quot (- (.toEpochMilli (java.time.Instant/parse (str (:evidence/at r))))
                                         (.toEpochMilli (java.time.Instant/parse (str (:evidence/at turn)))))
                                      1000)
               :retrieval/query-chars (count q)
               :retrieval/envelope-chars envelope-chars
               :retrieval/operator-chars (min (count text) (count payload))
               :retrieval/patterns (retrieval-patterns r))
      (nil? r) (assoc :retrieval/absent true))))

(def rows (mapv census-row turns))

;; ------------------------------------------------------- phase vocabulary

(def phase-words ["identify" "map" "derive" "argue" "verify" "instantiate" "document" "survey"])

(defn- phase-hits [text]
  (vec (filter #(re-find (re-pattern (str "(?i)\\b" %)) text) phase-words)))

;; ------------------------------------------------------------- artifacts

(defn- spit-edn! [f v]
  (spit (io/file out-dir f) (with-out-str (clojure.pprint/pprint v))))

(spit-edn! "00-window-turns.edn"
           {:window {:from from :to to}
            :record-count (count window-records)
            :session-start-count (count session-starts)
            :turn-count (count turns)
            :sessions (frequencies (map :evidence/session-id turns))
            :turns (mapv (fn [e] {:evidence/id (:evidence/id e)
                                  :at (:evidence/at e)
                                  :session (:evidence/session-id e)
                                  :claim-type (:evidence/claim-type e)
                                  :in-reply-to (:evidence/in-reply-to e)
                                  :chars (count (turn-text e))
                                  :text (turn-text e)})
                         turns)})

(spit-edn! "01-retrievals.edn"
           {:window {:from from :to to}
            :count (count retrievals)
            :records (mapv (fn [e]
                             (let [q (retrieval-query e)
                                   {:keys [envelope-chars payload]} (strip-envelope q)]
                               {:evidence/id (:evidence/id e)
                                :at (:evidence/at e)
                                :agent (:evidence/author e)
                                :query-chars (count q)
                                :envelope-chars envelope-chars
                                :payload-chars (count payload)
                                :query q
                                :patterns (retrieval-patterns e)}))
                           retrievals)})

(spit-edn! "02-census.edn"
           {:window {:from from :to to}
            :classification-source "classification.edn (hand-authored fiat; :kind and :node are judgement, :receipts are lookups)"
            :rows rows})

(let [tagged (remove :retrieval/absent rows)
      noded (remove #(= :none (:node %)) rows)
      receipted (filter #(pos? (:receipt-count %)) rows)
      direct (filter #(= :direct (:receipt-grade %)) rows)
      by-surface (group-by #(if (pos? (:retrieval/envelope-chars % 0)) :bell-envelope :bare-turn) tagged)
      by-join (group-by :retrieval/join tagged)]
  (spit-edn! "03-coverage.edn"
             {:window {:from from :to to}

              :denominators
              {:store-records-authored-by-joe (count window-records)
               :of-which-session-start-events (count session-starts)
               :operator-turns (count turns)}

              :tag-coverage
              {:turns-with-a-retrieval-record (count tagged)
               :turns-without (- (count turns) (count tagged))
               :fraction (double (/ (count tagged) (count turns)))
               :by-join-stage (into {} (for [[k v] by-join] [k (count v)]))
               :content-joinable-fraction (double (/ (count (:content by-join)) (count turns)))
               :lag-seconds {:min (apply min (map :retrieval/lag-s tagged))
                             :max (apply max (map :retrieval/lag-s tagged))}}

              :instrument-fidelity
              {:note "how many characters of the operator's own words appear in
                      the LOGGED query field, per turn. CORRECTED 2026-09-04:
                      this is NOT the embedding input. dev.clj context-retrieval!
                      embeds (subs user-msg 0 200) + \" \" + (subs response 0 200)
                      and then stores (subs that 0 100) for display. The store
                      therefore does not record what was embedded; these numbers
                      measure the record, not the instrument. See
                      FINDINGS-embedding-pipeline.md section 1."
               :by-surface
               (into {} (for [[k v] by-surface]
                          [k {:turns (count v)
                              :envelope-chars (distinct (map :retrieval/envelope-chars v))
                              :operator-chars-min (apply min (map :retrieval/operator-chars v))
                              :operator-chars-max (apply max (map :retrieval/operator-chars v))
                              :operator-chars (mapv :retrieval/operator-chars v)}]))}

              :phase-vocabulary
              {:words phase-words
               :turns-with-any-phase-word (count (filter #(seq (phase-hits (:verbatim-head %))) rows))
               :hits-over-full-text (into {} (for [w phase-words]
                                               [w (count (filter #(re-find (re-pattern (str "(?i)\\b" w))
                                                                           (get-in (into {} (map (juxt :evidence/id identity) turns))
                                                                                   [(:evidence/id %) :evidence/body :text] ""))
                                                                 rows))]))
               :measured-directly (into {} (for [w phase-words]
                                             [w (count (filter #(re-find (re-pattern (str "(?i)\\b" w)) (turn-text %)) turns))]))}

              :node-coverage
              {:turns-landing-on-a-table-node (count noded)
               :turns-landing-on-no-node (- (count turns) (count noded))
               :by-node (frequencies (map :node rows))
               :by-kind (frequencies (map :kind rows))}

              :receipt-coverage
              {:turns-with-at-least-one-receipt (count receipted)
               :direct (count direct)
               :indirect (count (filter #(= :indirect (:receipt-grade %)) rows))
               :none (count (filter #(= :none (:receipt-grade %)) rows))
               :total-receipts (reduce + (map :receipt-count rows))}

              :both
              {:node-and-direct-receipt
               (count (filter #(and (not= :none (:node %)) (= :direct (:receipt-grade %))) rows))
               :no-node-but-direct-receipt
               (count (filter #(and (= :none (:node %)) (= :direct (:receipt-grade %))) rows))
               :no-node-and-no-receipt
               (count (filter #(and (= :none (:node %)) (= :none (:receipt-grade %))) rows))}}))

(println "census written to" out-dir)
(println "  turns" (count turns) "| tagged" (count (remove :retrieval/absent rows))
         "| on a node" (count (remove #(= :none (:node %)) rows))
         "| direct receipt" (count (filter #(= :direct (:receipt-grade %)) rows)))
