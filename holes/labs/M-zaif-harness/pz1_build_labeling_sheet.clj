#!/usr/bin/env bb
;;
;; pz1_build_labeling_sheet.clj — Builds the labeling sheet for PZ1 slice 2.
;;
;; Reads H1 hits, stratified-samples 60 :hit items (proportional by route,
;; covering all distinct markers), samples 60 :probe items (non-hit operator
;; turns spread across the window), re-fetches each doc for context, and
;; writes the EDN + MD files.

(ns pz1-build-labeling-sheet
  (:require
   [babashka.curl :as curl]
   [clojure.walk :as walk]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.pprint :as pprint]))

(def api-base "http://127.0.0.1:7074/api/alpha/evidence")
(def labs-dir "/home/joe/code/futon2/holes/labs/M-zaif-harness")
(def h1-edn (str labs-dir "/pz1-sample-scan.edn"))

;; --- API helpers ---

(defn fetch-doc [eid]
  (try
    (-> (curl/get (str api-base "/" eid) {:as :text})
        (:body)
        (edn/read-string))
    (catch Exception e
      nil)))

(defn fetch-page [params]
  (-> (curl/get api-base {:query-params params :as :text})
      (:body)
      (edn/read-string)))

;; --- String extraction ---

(defn extract-strings [x]
  (let [acc (volatile! [])]
    (walk/postwalk
     (fn [node]
       (when (string? node) (vswap! acc conj node))
       node)
    x)
    @acc))

(defn body-context
  "Concatenate all string values from :evidence/body, truncate to 600 chars."
  [doc]
  (let [s (->> (:evidence/body doc)
               (extract-strings)
               (str/join " "))]
    (subs s 0 (min (count s) 600))))

;; --- Random with seed for reproducibility ---

(def rng (java.util.Random. 42))

(defn rand-nth-coll [coll]
  (nth coll (.nextInt rng (count coll))))

(defn shuffle-coll [coll]
  (let [arr (into-array Object coll)
        n   (count arr)]
    (dotimes [i n]
      (let [j (+ i (.nextInt rng (- n i)))
            tmp (aget arr i)]
        (aset arr i (aget arr j))
        (aset arr j tmp)))
    (seq arr)))

;; --- Stratified hit sampling ---

(defn sample-hits [hits target-counts]
  "target-counts = {:gamma 43 :c-channel 9 :actand 8}
   For each route, cover every distinct marker first (one item per marker,
   chosen at random among that marker's hits), then fill the remaining
   quota by sampling from the route's remaining hits (shuffled)."
  (let [result (atom [])]
    (doseq [[route quota] target-counts]
      (let [route-hits    (filter #(= (:route %) route) hits)
            by-marker     (group-by :marker route-hits)
            markers       (keys by-marker)
            ;; Phase 1: one per distinct marker (random within marker)
            marker-picks  (for [m markers]
                            (rand-nth-coll (get by-marker m)))
            marker-pick-ids (set (map :id marker-picks))
            ;; Phase 2: fill from remaining
            remaining     (remove #(contains? marker-pick-ids (:id %)) route-hits)
            shuffled-rem  (shuffle-coll remaining)
            fill-count    (max 0 (- quota (count marker-picks)))
            fill-picks    (take fill-count shuffled-rem)
            all-picks     (vec (concat marker-picks fill-picks))]
        (swap! result into all-picks)))
    @result))

;; --- Probe sampling ---

(defn date-str [iso]
  (subs iso 0 10))

(defn sample-probes [all-turns hit-ids target]
  "Sample `target` non-hit turns, spread across the window by striding."
  (let [non-hits  (remove #(contains? hit-ids (:evidence/id %)) all-turns)
        non-hits-vec (vec non-hits)
        n (count non-hits-vec)]
    (if (<= n target)
      non-hits-vec
      (let [step     (double (/ n target))
            ;; Pick target evenly-spaced indices with jitter
            picked-idx (->> (range target)
                            (map (fn [i]
                                   (let [base (long (* i step))
                                         jitter (.nextInt rng (max 1 (int step)))
                                         idx (min (+ base jitter) (dec n))]
                                     idx)))
                            (distinct))]
        (vec (for [idx picked-idx]
               (nth non-hits-vec idx)))))))

;; --- Main ---

(defn -main []
  (println "Loading H1 hits...")
  (let [h1       (edn/read-string (slurp h1-edn))
        hits     (:hits h1)
        hit-ids  (set (map :id hits))
        target-counts {:gamma 43 :c-channel 9 :actand 8}
        _        (println "Total H1 hits:" (count hits))

        ;; --- Sample 60 hit items ---
        _        (println "Sampling 60 stratified hits...")
        hit-sample (sample-hits hits target-counts)
        _        (println "Hit sample size:" (count hit-sample))
        _        (doseq [[route quota] target-counts]
                    (println (format "  %s: %d" (name route)
                                     (count (filter #(= (:route %) route) hit-sample)))))

        ;; --- Collect all operator turns in-window for probe sampling ---
        _        (println "Paging operator turns since=2026-06-01T00:00:00Z for probes...")
        all-turns (loop [cursor nil acc []]
                     (let [params (cond-> {:author       "joe"
                                           :type         "coordination"
                                           :claim-type   "question"
                                           :since        "2026-06-01T00:00:00Z"
                                           :limit        500}
                                    cursor (assoc :before cursor))
                           page   (fetch-page params)
                           entries (:entries page)]
                       (if (or (nil? entries) (empty? entries))
                         acc
                         (let [oldest (:evidence/at (last entries))]
                           (if (= oldest cursor)
                             acc
                             (recur oldest (into acc entries)))))))
        _        (println "Total in-window operator turns:" (count all-turns))
        ;; Defensive local filter: enforce since >= 2026-06-01T00:00:00Z
        window-start "2026-06-01T00:00:00Z"
        in-window-turns (filter #(>= (compare (:evidence/at %) window-start) 0) all-turns)
        _        (println "After local date filter:" (count in-window-turns))
        probe-turns (sample-probes in-window-turns hit-ids 60)
        _        (println "Probe sample size:" (count probe-turns))
        ;; Verify all probes are in-window
        _        (doseq [p probe-turns]
                   (when (< (compare (:evidence/at p) window-start) 0)
                     (throw (ex-info (str "OUT-OF-WINDOW PROBE: " (:evidence/id p) " " (:evidence/at p))
                                     {:id (:evidence/id p) :at (:evidence/at p)}))))
        _        (println "Probe sample size:" (count probe-turns))
        _        (let [dates (set (map #(date-str (:evidence/at %)) probe-turns))]
                   (println "Probe date spread:" (count dates) "distinct days"))

        ;; --- Re-fetch all 120 docs for context ---
        all-ids  (concat (map :id hit-sample)
                         (map :evidence/id probe-turns))
        _        (println "Fetching" (count all-ids) "docs for context...")
        {fetch-ok :ok fetch-fail :fail}
         (loop [ids all-ids ok {} fails []]
           (if (empty? ids)
             {:ok ok :fail fails}
             (let [eid (first ids)]
               (if-let [doc (fetch-doc eid)]
                 (recur (rest ids)
                        (assoc ok eid (body-context doc))
                        fails)
                 (recur (rest ids) ok (conj fails eid))))))
        _        (println "Fetch ok:" (count fetch-ok) "fail:" (count fetch-fail))

        ;; --- Build items ---
        hit-items (for [h hit-sample]
                    {:id            (:id h)
                     :at            (:at h)
                     :kind          :hit
                     :route-claimed (:route h)
                     :marker        (:marker h)
                     :context       (get fetch-ok (:id h) "")
                     :label         nil})
        probe-items (for [p probe-turns]
                      (let [eid (:evidence/id p)]
                        {:id      eid
                         :at      (:evidence/at p)
                         :kind    :probe
                         :context (get fetch-ok eid "")
                         :label   nil}))
        all-items (vec (concat hit-items probe-items))
        _        (println "Total items:" (count all-items))

        edn-out  {:items all-items}
        edn-str  (with-out-str (pprint/pprint edn-out))]

    ;; Write EDN
    (spit (str labs-dir "/pz1-labeling-sheet.edn") edn-str)
    (println "Wrote pz1-labeling-sheet.edn")

    ;; Write MD
    (let [md-lines
          (concat
           ["| # | id-prefix | kind | route-claimed | marker | context |"
            "|---|-----------|------|---------------|--------|---------|"]
           (for [[idx item] (map-indexed vector all-items)]
             (let [id-pre  (subs (:id item) 0 12)
                   kind    (name (:kind item))
                   route   (if-let [r (:route-claimed item)] (name r) "—")
                   marker  (or (:marker item) "—")
                   ctx     (-> (:context item)
                               (str/replace "|" "\\|")
                               (str/replace "\n" " ")
                               (subs 0 (min (count (:context item)) 200)))]
               (format "| %d | %s | %s | %s | %s | %s |"
                       (inc idx) id-pre kind route marker ctx))))]
      (spit (str labs-dir "/pz1-labeling-sheet.md")
            (str/join "\n" md-lines))
      (println "Wrote pz1-labeling-sheet.md"))

    ;; Summary
    (let [hit-route-counts (into {}
                                (for [[k v] (group-by :route-claimed hit-items)]
                                  [k (count v)]))
          markers-covered  (->> hit-items (map :marker) (set) (count))]
      (println "\n=== SUMMARY ===")
      (println "Hit-item route counts:" hit-route-counts)
      (println "Probe count:" (count probe-items))
      (let [probe-ats (sort (map :at probe-items))]
        (println "Probe min :at:" (first probe-ats))
        (println "Probe max :at:" (last probe-ats)))
      (let [probe-days (set (map #(subs (:at %) 0 10) probe-items))]
        (println "Distinct in-window days:" (count probe-days)))
      (println "Distinct markers covered:" markers-covered)
      (println "Doc-fetch failures:" (count fetch-fail))
      (when (seq fetch-fail)
        (println "  Failed ids:" fetch-fail)))))

(-main)
