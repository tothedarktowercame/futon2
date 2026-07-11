#!/usr/bin/env bb
;;
;; pz1_lexicon_scan.clj — Correction-lexicon sample scanner (M-zaif-harness, PZ1 slice 1)
;;
;; Pages the futon1b evidence API (read-only) for operator chat turns
;; (type=coordination, claim-type=question, author=joe), walks every string
;; value in :evidence/body, lowercases, and scans for correction markers
;; across three routes: :gamma, :c-channel, :actand.
;;
;; Usage: bb pz1_lexicon_scan.clj --since <iso> --max-docs <n>
;;        (optionally: --out <path> to write EDN; defaults to stdout)

(ns pz1-lexicon-scan
  (:require
   [babashka.curl :as curl]
   [clojure.walk :as walk]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.pprint :as pprint]))

;; ---------------------------------------------------------------------------
;; Lexicon
;; ---------------------------------------------------------------------------

(def lexicon
  "Marker phrases keyed by correction route."
  {:gamma      ["not that" "instead" "rather than" "no need" "don't do"
                "let's not" "revert" "undo" "wrong approach" "actually,"]
   :c-channel  ["not what i meant" "i meant" "what i want" "to be clear"
                "misread" "misunderstood" "the point is"]
   :actand     ["stale" "out of date" "doesn't exist" "already done"
                "was wrong about"]})

;; Pre-compute lowercase markers (they're already lowercase in the lexicon).
(def lexicon-lc
  (update-vals lexicon #(map str/lower-case %)))

;; ---------------------------------------------------------------------------
;; API
;; ---------------------------------------------------------------------------

(def api-base "http://127.0.0.1:7074/api/alpha/evidence")

(defn fetch-page
  "Fetch one page from the evidence API. Returns the parsed EDN map or
  throws on non-2xx. Retries once on exception."
  [params]
  (let [attempt (fn []
                  (-> (curl/get api-base {:query-params params
                                          :as          :text})
                      (:body)
                      (edn/read-string)))]
    (try
      (attempt)
      (catch Exception e1
        (try
          (attempt)
          (catch Exception e2
            (throw (ex-info "page-fetch-failed-after-retry"
                            {:params params
                             :first  (ex-message e1)
                             :second (ex-message e2)}
                            e2))))))))

;; ---------------------------------------------------------------------------
;; String extraction
;; ---------------------------------------------------------------------------

(defn extract-strings
  "Walk any nested structure and collect all string values."
  [x]
  (let [acc (volatile! [])]
    (walk/postwalk
     (fn [node]
       (when (string? node)
         (vswap! acc conj node))
       node)
     x)
    @acc))

;; ---------------------------------------------------------------------------
;; Scanning
;; ---------------------------------------------------------------------------

(defn snippet-around
  "Return up to 120 chars of context around the marker match, or the whole
  text if shorter."
  [text lc-text marker]
  (let [idx (str/index-of lc-text marker)]
    (if (nil? idx)
      (subs text 0 (min (count text) 120))
      (let [start (max 0 (- idx 30))
            end   (min (count text) (+ idx (count marker) 60))
            snip  (subs text start end)]
        (str (when (pos? start) "…") snip (when (< end (count text)) "…"))))))

(defn scan-text
  "Scan a single lowercased text for all markers across all routes.
  Returns a seq of {:route :marker :snippet} hit maps. The snippet is
  derived from the original (non-lowercased) text."
  [text]
  (let [lc (str/lower-case text)]
    (into []
          (for [[route markers] lexicon-lc
                marker          markers
                :when           (str/includes? lc marker)]
        {:route   route
         :marker  marker
         :snippet (snippet-around text lc marker)}))))

(defn scan-entry
  "Scan one evidence entry. Returns a seq of hit maps enriched with :id :at."
  [entry]
  (let [eid  (:evidence/id entry)
        eat  (:evidence/at entry)
        body (:evidence/body entry)
        strs (extract-strings body)]
    (for [s    strs
          hit  (scan-text s)]
      (assoc hit :id eid :at eat))))

;; ---------------------------------------------------------------------------
;; Paging loop
;; ---------------------------------------------------------------------------

(defn scan
  "Page through the API starting from :since, scanning up to max-docs entries.
  Returns {:docs-scanned :counts :hits :errors}."
  [since max-docs]
  (loop [cursor    nil   ; :before value for next page (oldest :at of last page)
         scanned   0
         counts    (update-vals lexicon-lc #(zipmap % (repeat 0)))
         hits      []
         errors    []
         page-num  0]
    (if (>= scanned max-docs)
      {:docs-scanned scanned :counts counts :hits hits :errors errors}
      (let [limit    (min 500 (- max-docs scanned))
            params   (cond-> {:author       "joe"
                              :type         "coordination"
                              :claim-type   "question"
                              :limit        limit}
                       since         (assoc :since since)
                       cursor        (assoc :before cursor))
            _        (println (format "[page %d] fetching limit=%d cursor=%s"
                                      (inc page-num) limit (str cursor)))
            result   (try
                       (fetch-page params)
                       (catch Exception e
                         {:api-error (ex-message e)
                          :params    params}))
            entries  (:entries result)]
        (cond
          ;; API error — record and stop
          (:api-error result)
          (do
            (println (format "[page %d] ERROR: %s" (inc page-num) (:api-error result)))
            {:docs-scanned scanned
             :counts counts
             :hits   hits
             :errors (conj errors {:page (inc page-num)
                                   :params params
                                   :error (:api-error result)})})

          ;; Empty page — done
          (or (nil? entries) (empty? entries))
          (do
            (println (format "[page %d] empty — done" (inc page-num)))
            {:docs-scanned scanned :counts counts :hits hits :errors errors})

          :else
          (let [page-hits  (mapcat scan-entry entries)
                new-counts (reduce
                            (fn [c {:keys [route marker]}]
                              (update-in c [route marker] (fnil inc 0)))
                            counts
                            page-hits)
                oldest-at  (:evidence/at (last entries))]
            (println (format "[page %d] got %d entries, %d hits"
                             (inc page-num) (count entries) (count page-hits)))
            (recur oldest-at
                   (+ scanned (count entries))
                   new-counts
                   (into hits page-hits)
                   errors
                   (inc page-num))))))))

;; ---------------------------------------------------------------------------
;; CLI
;; ---------------------------------------------------------------------------

(defn parse-args [args]
  (loop [args   args
         opts   {:since nil :max-docs 1000 :out nil}]
    (if (empty? args)
      opts
      (let [[k v & rest] args]
        (case k
          "--since"    (recur rest (assoc opts :since v))
          "--max-docs" (recur rest (assoc opts :max-docs (parse-long v)))
          "--out"      (recur rest (assoc opts :out v))
          (throw (ex-info (str "Unknown arg: " k) {:arg k})))))))

(defn -main [& args]
  (let [{:keys [since max-docs out]} (parse-args args)
        _ (println (format "Starting scan: since=%s max-docs=%d" since max-docs))
        result   (scan since max-docs)
        output   {:sample  {:since       since
                            :max-docs    max-docs
                            :docs-scanned (:docs-scanned result)}
                  :counts  (:counts result)
                  :hits    (:hits result)
                  :errors  (:errors result)}
        edn-str  (with-out-str (pprint/pprint output))]
    (if out
      (do (spit out edn-str)
          (println (format "\nWrote %s" out)))
      (println edn-str))
    ;; Summary to stderr
    (let [route-totals (update-vals (:counts result) #(apply + (vals %)))]
      (binding [*out* *err*]
        (println "\n=== SUMMARY ===")
        (println "docs-scanned:" (:docs-scanned result))
        (doseq [[route n] route-totals]
          (println (format "  %s: %d hits" (name route) n)))
        (println "errors:" (count (:errors result)))))))

(apply -main *command-line-args*)
