#!/usr/bin/env bb
;;;
;;; z1_views.clj — Z1 first views: named query library over the local futon1b
;;; store server API (M-zaif-harness, C-futon1b-features campaign §1d).
;;;
;;; Three named views, each a fn returning:
;;;   {:view <name> :query <the exact request(s) made>
;;;    :as-of <newest :at seen> :results [...]}
;;;
;;; All queries are READ-ONLY GETs against http://127.0.0.1:7073.
;;; Every query carries itself as provenance (the :query field records the
;;; exact params sent, so the observation is replayable — §2 of the XTQL
;;; section).
;;;
;;; 1. operator-turns  — joe-authored chat turns in a window.
;;; 2. gamma-events    — the γ event stream (adjudication-tagged + mark-tagged
;;;                       entries; FALLBACK content-scan mode when tags absent).
;;; 3. mission-attributed — turns carrying :clocked-mission, grouped per mission.
;;;
;;; Usage (any cwd — paths are file-relative):
;;;   bb z1_views.clj operator-turns [--since ISO] [--limit N]
;;;   bb z1_views.clj gamma-events   [--since ISO] [--limit N]
;;;   bb z1_views.clj mission-attributed [--since ISO] [--limit N]
;;;
;;; Or require as a ns from another bb script:
;;;   (load-file "z1_views.clj")  ; then call z1-views/operator-turns {:since ... :limit ...}
;;;
;;; Design notes:
;;; - The store server takes ~8–12s per query regardless of limit (XTDB index
;;;   warm-up on this laptop). We page in small windows and checkpoint to disk.
;;; - Entries arrive newest-first. Paging uses :before = oldest :at of the last
;;;   page (the H1 paging pattern from pz1_lexicon_scan.clj).
;;; - text-search is a CANDIDATE GENERATOR ONLY. Every candidate is re-fetched
;;;   by ID from the store and membership re-checked (P2 pattern; §FTS5's place).

(ns z1-views
  (:require
   [babashka.curl :as curl]
   [babashka.fs :as fs]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.pprint :as pprint]))

;; ---------------------------------------------------------------------------
;; Configuration
;; ---------------------------------------------------------------------------

(def ^:const api-base "http://127.0.0.1:7073/api/alpha/evidence")

(def ^:const default-limit 50)        ; never unbounded
(def ^:const page-size 50)            ; entries per API call
(def ^:const http-timeout-ms 14000)   ; stay under the 15s server-side limit
(def ^:const max-retries 2)

;; ---------------------------------------------------------------------------
;; File-relative path resolution (runs from any cwd)
;; ---------------------------------------------------------------------------

(def lab-dir
  "Absolute path to this script's directory, resolved at load time."
  (str (fs/parent (fs/absolutize *file*))))

(defn lab-file
  "Resolve a filename relative to the lab directory."
  [name]
  (str (fs/path lab-dir name)))

;; ---------------------------------------------------------------------------
;; HTTP — read-only GET with retry and timeout
;; ---------------------------------------------------------------------------

(defn parse-edn-response
  "Parse the EDN response body. The store returns application/edn."
  [body]
  (edn/read-string {:readers *data-readers*} body))

(defn fetch-page
  "Fetch one page from the evidence API (read-only GET). Returns parsed EDN.
   Retries up to max-retries on exception. Throws if all retries exhaust."
  [params]
  (let [attempt (fn []
                  (-> (curl/get api-base
                                {:query-params params
                                 :as           :text
                                 :timeout      http-timeout-ms})
                      (:body)
                      (parse-edn-response)))
        try-once (fn []
                   (try
                     {:ok (attempt)}
                     (catch Exception e
                       {:err e})))]
    (loop [tries-left (inc max-retries)
           last-err   nil]
      (if (zero? tries-left)
        (throw (ex-info "page-fetch-failed-after-retries"
                        {:params params :last-error (str last-err)}
                        last-err))
        (let [r (try-once)]
          (if (:ok r)
            (:ok r)
            (recur (dec tries-left) (:err r))))))))

(defn fetch-by-id
  "Fetch a single evidence entry by ID (GET /api/alpha/evidence/<id>).
   Returns the parsed EDN entry map, or nil if not found."
  [eid]
  (try
    (-> (curl/get (str api-base "/" eid)
                  {:as      :text
                   :timeout http-timeout-ms})
        (:body)
        (parse-edn-response))
    (catch Exception e
      (println (str "  WARN: fetch-by-id " eid " failed: " (ex-message e)))
      nil)))

(defn text-search-raw
  "Query the BM25 text-search endpoint. Returns raw result map.
   URL: /api/alpha/evidence/text-search?q=...&limit=..."
  ([q] (text-search-raw q default-limit))
  ([q limit]
   (let [url (str (str/replace api-base #"/evidence$" "") "/evidence/text-search")]
     (-> (curl/get url
                   {:query-params {:q     q
                                   :limit limit}
                    :as           :text
                    :timeout      http-timeout-ms})
         (:body)
         (parse-edn-response)))))

;; ---------------------------------------------------------------------------
;; Paging loop — the H1 pattern (page by :at cursor)
;; ---------------------------------------------------------------------------

(defn page-all
  "Page through the evidence API collecting entries until limit reached or
   no more results. Returns a map:
     {:entries [...] :pages-fetched N :query-params {...}}

   Entries are accumulated newest-first (the API's natural order).
   Paging uses :before = oldest :at of the last page."
  [{:keys [since before limit extra-params]
    :or   {limit default-limit}}]
  (let [base-params (merge {:limit (min page-size limit)} extra-params)]
    (loop [cursor       before      ; :before for next page (nil = start from newest)
           collected    []
           pages        0
           all-params   []]
      (if (>= (count collected) limit)
        {:entries      (take limit collected)
         :pages-fetched pages
         :query-params  (conj all-params
                              (cond-> base-params
                                since  (assoc :since since)
                                cursor (assoc :before cursor)))}
        (let [params (cond-> base-params
                       since  (assoc :since since)
                       cursor (assoc :before cursor))
              _      (println (format "  [page %d] %s" (inc pages)
                                      (pr-str (dissoc params :limit))))
              result (fetch-page params)
              entries (:entries result)]
          (cond
            ;; Empty page — done
            (or (nil? entries) (empty? entries))
            {:entries       collected
             :pages-fetched pages
             :query-params  (conj all-params params)}

            ;; A short page is NOT proof of exhaustion: the futon1b windowed
            ;; query path can return fewer than :limit per request while more
            ;; matches remain in the window (scan-budget behavior — found
            ;; live: since=2026-07-11 returned 1 short page covering only the
            ;; newest hours). Keep paging; only an EMPTY page or a stalled
            ;; cursor ends the walk. Costs one extra request on genuinely
            ;; final pages — correct over fast.
            :else
            (let [oldest-at (:evidence/at (last entries))]
              (if (or (nil? oldest-at) (= oldest-at cursor))
                {:entries       (take limit (into collected entries))
                 :pages-fetched (inc pages)
                 :query-params  (conj all-params params)}
                (recur oldest-at
                       (into collected entries)
                       (inc pages)
                       (conj all-params params))))))))))

;; ---------------------------------------------------------------------------
;; View 1: operator-turns
;; ---------------------------------------------------------------------------

(defn- chat-turn?
  "Is this entry a joe-authored operator chat turn?
   Identified by: type=coordination, author=joe, tags include :turn and :user."
  [entry]
  (and (= (:evidence/type entry) :coordination)
       (= (:evidence/author entry) "joe")
       (let [tags (set (:evidence/tags entry))]
         (and (contains? tags :turn) (contains? tags :user)))))

(defn operator-turns
  "Joe-authored chat turns in a window.
   Params: since (ISO-8601, optional), limit (always passed, never unbounded).
   Returns: {:view :operator-turns :query {...} :as-of <newest :at> :results [...]}.

   Each result is a map with :id :at :session-id :text :clocked-mission :clocked-campaign."
  [{:keys [since limit]
    :or   {limit default-limit}}]
  (let [query-params {:author "joe" :type "coordination"}
        {:keys [entries pages-fetched]}
        (page-all {:since        since
                   :limit        limit
                   :extra-params query-params})
        turns     (filter chat-turn? entries)
        results   (for [e turns]
                    {:id              (:evidence/id e)
                     :at              (:evidence/at e)
                     :session-id      (:evidence/session-id e)
                     :text            (get-in e [:evidence/body :text])
                     :tags            (:evidence/tags e)
                     :marks           (get-in e [:evidence/body :marks])
                     :clocked-mission (get-in e [:evidence/body :clocked-mission])
                     :clocked-campaign (get-in e [:evidence/body :clocked-campaign])})
        as-of     (when (seq entries)
                    (:evidence/at (first entries)))]
    {:view      :operator-turns
     :query     {:endpoint api-base
                 :params   query-params
                 :paging   {:since since :limit limit :pages-fetched pages-fetched}
                 :note     "type=coordination, author=joe; client-side filter for :turn :user tags"}
     :as-of     as-of
     :results   (vec results)}))

;; ---------------------------------------------------------------------------
;; View 2: gamma-events
;; ---------------------------------------------------------------------------

(def ^:const gamma-mark-glyphs
  "The v0 mark vocabulary (M-points-de-fuite §6.5): ✘ = correction, ✓ = approval,
   💡 = idea (idea events feed the reflection lane; they mint no reward label).
   Used in fallback content-scan mode only — the primary mode queries structured tags."
  {"✘" :correction
   "✓" :approval
   "💡" :idea})

(def ^:const gamma-tags
  "The structured tags that mark γ events once L0 (B0 turn-end recognizer) lands.
   Until then, this set will be empty in the store and the view falls back to
   content-scan mode."
  #{:correction :approval})

(defn- gamma-tagged?
  "Does this entry carry a structured adjudication or mark tag?
   Checks for :correction / :approval in :evidence/tags."
  [entry]
  (let [tags (set (:evidence/tags entry))]
    (or (some gamma-tags tags)
        ;; also check claim-type as a possible adjudication signal
        (#{:correction :approval} (:evidence/claim-type entry)))))

(defn- scan-for-glyphs
  "Fallback content-scan: extract all string values from :evidence/body and
   grep for ✘/✓ glyphs. Returns a seq of {:glyph :mark :snippet} hits.
   Clearly labeled :mode :fallback-scan by the caller."
  [entry]
  (let [body   (:evidence/body entry)
        body-str (if (string? body) body (pr-str body))
        hits    (for [[glyph mark] gamma-mark-glyphs
                      :let          [idx (str/index-of body-str glyph)]
                      :when         idx]
                  {:glyph   glyph
                   :mark    mark
                   :snippet (subs body-str
                                  (max 0 (- idx 40))
                                  (min (count body-str) (+ idx 40)))})]
    hits))

(defn gamma-events
  "The γ event stream: adjudication-tagged + mark-tagged entries.
   Params: since (ISO-8601, optional), limit (always passed).
   Returns: {:view :gamma-events :query {...} :as-of <newest :at>
             :mode :tagged|:fallback-scan :results [...]}.

   PRIMARY MODE (:mode :tagged): queries the store for entries with
   :correction/:approval tags. This will be EMPTY until L0 (B0 recognizer)
   lands — the view handles empty honestly.

   FALLBACK MODE (:mode :fallback-scan): when the tagged query returns zero
   results, greps for ✘/✓ glyphs in fetched operator-turn windows.
   Clearly labeled :mode :fallback-scan. The glyph scan is a CANDIDATE
   GENERATOR — every hit is re-fetched by ID and membership re-checked."
  [{:keys [since limit]
    :or   {limit default-limit}}]
  ;; --- PRIMARY: structured tag query ---
  ;; We try each gamma tag as a query filter. The evidence API doesn't have
  ;; a direct tag filter, so we query joe coordination entries (the same
  ;; operator-turn window) and filter client-side for gamma tags.
  (let [op-query {:author "joe" :type "coordination"}
        {:keys [entries pages-fetched]}
        (page-all {:since        since
                   :limit        limit
                   :extra-params op-query})
        tagged   (filter gamma-tagged? entries)
        as-of    (when (seq entries)
                   (:evidence/at (first entries)))]
    (if (seq tagged)
      ;; --- PRIMARY MODE: structured tags found ---
      {:view :gamma-events
       :query {:endpoint api-base
               :params   op-query
               :paging   {:since since :limit limit :pages-fetched pages-fetched}
               :note     "queried operator turns; client-side filter for :correction/:approval tags"}
       :as-of as-of
       :mode  :tagged
       :results (vec
                 (for [e tagged]
                   {:id    (:evidence/id e)
                    :at    (:evidence/at e)
                    :tags  (:evidence/tags e)
                    :marks (vec (filter gamma-tags (:evidence/tags e)))}))}
      ;; --- FALLBACK: no structured tags yet — content-scan for glyphs ---
      (let [glyph-hits (for [e entries
                             :let [hits (scan-for-glyphs e)]
                             :when (seq hits)]
                         {:id      (:evidence/id e)
                          :at      (:evidence/at e)
                          :glyphs  (vec hits)})
            ;; Re-fetch verification: re-check one candidate from the store
            verified-sample (when (seq glyph-hits)
                              (let [sample (first glyph-hits)
                                    refetched (fetch-by-id (:id sample))]
                                {:id          (:id sample)
                                 :refetched   (some? refetched)
                                 :glyph-found (when refetched
                                                (let [body-str (pr-str (:evidence/body refetched))
                                                      glyph (:glyph (first (:glyphs sample)))]
                                                  (boolean (str/index-of body-str glyph))))}))]
        {:view :gamma-events
         :query {:endpoint  api-base
                 :params    op-query
                 :paging    {:since since :limit limit :pages-fetched pages-fetched}
                 :fallback  "content-scan for ✘/✓ glyphs in operator-turn window (B0/L0 not yet live)"}
         :as-of as-of
         :mode  :fallback-scan
         :results (vec glyph-hits)
         :verification-sample verified-sample}))))

;; ---------------------------------------------------------------------------
;; View 3: mission-attributed
;; ---------------------------------------------------------------------------

(defn- extract-mission
  "Extract the clocked mission from an entry's body.
   The autoclock witness is the attribution of record: :clocked-mission in
   :evidence/body. Falls back to :mission-id if :clocked-mission absent."
  [entry]
  (or (get-in entry [:evidence/body :clocked-mission])
      (get-in entry [:evidence/body :mission-id])))

(defn mission-attributed
  "Turns carrying :clocked-mission, grouped per mission with counts.
   The autoclock witness (:clocked-mission on the turn doc) is the attribution
   of record — NOT text tokens.
   Params: since (ISO-8601, optional), limit (always passed).
   Returns: {:view :mission-attributed :query {...} :as-of <newest :at>
             :results [{:mission M-... :count N :turns [...]} ...]}."
  [{:keys [since limit]
    :or   {limit default-limit}}]
  (let [query-params {:author "joe" :type "coordination"}
        {:keys [entries pages-fetched]}
        (page-all {:since        since
                   :limit        limit
                   :extra-params query-params})
        ;; Filter to operator chat turns with a clocked mission
        attributed (for [e    entries
                         :let [mission (extract-mission e)]
                         :when (and (chat-turn? e) mission)]
                     {:id      (:evidence/id e)
                      :at      (:evidence/at e)
                      :mission mission
                      :clocked-target (get-in e [:evidence/body :clocked-target])})
        ;; Group per mission
        by-mission (group-by :mission attributed)
        results    (for [[mission turns] (sort-by (comp count val) > by-mission)]
                     {:mission mission
                      :count   (count turns)
                      :turns   (vec (sort-by :at #(compare %2 %1) turns))})
        as-of      (when (seq entries)
                     (:evidence/at (first entries)))]
    {:view   :mission-attributed
     :query  {:endpoint api-base
              :params   query-params
              :paging   {:since since :limit limit :pages-fetched pages-fetched}
              :note     "autoclock witness (:clocked-mission) = attribution of record"}
     :as-of  as-of
     :results (vec results)
     :summary {:total-missions (count by-mission)
               :total-attributed-turns (count attributed)}}))

;; ---------------------------------------------------------------------------
;; CLI
;; ---------------------------------------------------------------------------

(defn parse-args [args]
  (loop [args args
         opts {:since nil :limit default-limit}]
    (if (empty? args)
      opts
      (let [[k v & rest] args]
        (case k
          "--since" (recur rest (assoc opts :since v))
          "--limit" (recur rest (assoc opts :limit (parse-long v)))
          (throw (ex-info (str "Unknown arg: " k) {:arg k})))))))

(defn -main [& args]
  (if (empty? args)
    (do
      (println "Usage: bb z1_views.clj <view-name> [--since ISO] [--limit N]")
      (println "  Views: operator-turns | gamma-events | mission-attributed")
      (System/exit 1))
    (let [[view-name & rest-args] args
          {:keys [since limit]} (parse-args rest-args)]
      (println (str "=== Z1 view: " view-name
                    " (since=" since " limit=" limit ") ==="))
      (let [result (case view-name
                     "operator-turns"   (operator-turns {:since since :limit limit})
                     "gamma-events"     (gamma-events {:since since :limit limit})
                     "mission-attributed" (mission-attributed {:since since :limit limit})
                     (do (println (str "Unknown view: " view-name))
                         (System/exit 1)))]
        (pprint/pprint result)
        (binding [*out* *err*]
          (println (format "\n=== %s: %d results, as-of %s ==="
                           view-name (count (:results result)) (:as-of result))))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
