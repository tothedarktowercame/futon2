#!/usr/bin/env bb
;; Morning Brief page (claude-11 dispatch, approved by Joe 2026-10-25).
;;
;; Read-only over futon2/data/wm-morning-brief (items/, reviews/, addenda/).
;; Renders one static HTML page of the PENDING Morning Brief items, newest
;; first, grouped by the UTC date of :queued-at. No JavaScript, no external
;; assets, no review controls: reviews stay recorded from the REPL through
;; POST /api/alpha/morning-brief/review; each item's attempt-id is shown
;; prominently so Joe can name it.
;;
;; The pending/objective logic reimplements, rule for rule, what
;; futon2.aif.morning-brief does in src (item-objectives, with-pending-
;; objectives, pending-items). This script deliberately does not load the
;; namespace (no live-JVM source loads) -- when the rules change there,
;; change them here the same day.
;;
;; Usage:
;;   bb scripts/morning_brief_page.bb [brief-root] [output-html]
;;     defaults: data/wm-morning-brief  /var/www/zone.hyperreal.enterprises/wip/morning-brief/index.html
;;   bb scripts/morning_brief_page.bb --self-check
;;     builds a 3-item fixture (1 fully reviewed, 2 pending), renders to a
;;     temp file, and asserts the page lists exactly the 2 pending items,
;;     newest first.

(ns morning-brief-page
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(def objective-order
  [:operator-gate :feature-verdict :selection-quality
   :substantive-achievement :evidence-sufficiency :machine-response])

(def objective-questions
  {:operator-gate "Disposition the mission's operator gate."
   :feature-verdict "Accept the built feature?"
   :selection-quality "Was this the best available policy selection?"
   :substantive-achievement "Did the result substantively advance the selected target?"
   :evidence-sufficiency "Are the commit, validation, review, and grounding evidence sufficient?"
   :machine-response "If the click failed, did the machine stop, remember, and prescribe an adequate discharge?"})

;; --- rules mirrored from futon2.aif.morning-brief (item-objectives) ---

(defn item-objectives [item]
  (if (:operator-action item)
    [:operator-gate]
    (cond-> []
      (:commit item)                       (conj :feature-verdict)
      true                                 (conj :selection-quality :substantive-achievement)
      (or (:commit item)
          (seq (get-in item [:achievement :build])))
                                           (conj :evidence-sufficiency)
      (or (and (:outcome item) (not= :grounded-change (:outcome item)))
          (:failure item))
                                           (conj :machine-response))))

(defn- edn-files [dir]
  (->> (or (fs/list-dir dir) [])
       (filter #(and (fs/regular-file? %) (str/ends-with? % ".edn")))
       (sort-by str)))

(defn read-records [dir]
  (mapv #(edn/read-string (slurp (str %))) (edn-files dir)))

(defn with-pending-objectives [item review-pairs]
  (assoc item :pending-objectives
         (filterv #(not (contains? review-pairs [(:attempt-id item) %]))
                  (item-objectives item))))

(defn pending-items [root]
  (let [review-pairs (into #{} (map (juxt :attempt-id :objective))
                           (read-records (fs/path root "reviews")))]
    (->> (read-records (fs/path root "items"))
         (mapv #(with-pending-objectives % review-pairs))
         (filterv #(seq (:pending-objectives %))))))

(defn addenda-by-attempt [root]
  (frequencies (keep :attempt-id (read-records (fs/path root "addenda")))))

;; --- rendering ---

(defn- esc [s]
  (-> (str s)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")
      (str/replace "\"" "&quot;")))

(defn- queued-day [item]
  (try
    (let [i (java.time.Instant/parse (:queued-at item))]
      (str (. i (atZone java.time.ZoneOffset/UTC))))
    (catch Exception _ "unknown-date")))

(defn- queued-sort-key [item]
  (try (java.time.Instant/parse (:queued-at item))
       (catch Exception _ java.time.Instant/EPOCH)))

(defn- short-commit [c]
  (if (and (string? c) (> (count c) 7)) (subs c 0 7) (str c)))

(defn- row [label value]
  (when-not (str/blank? (str value))
    (str "<div class=\"row\"><span class=\"lbl\">" (esc label) "</span>"
         "<span class=\"val\">" (esc value) "</span></div>")))

(defn render-item [item addenda-count]
  (let [fc (get item :feature-card)
        ocr (get item :open-cascade-refinement)
        failure (get item :failure)]
    (str
     "<div class=\"item\">"
     "<h3><code>" (esc (:attempt-id item)) "</code></h3>"
     (row "queued-at" (:queued-at item))
     (row "outcome" (:outcome item))
     (row "selected target" (:selected-target item))
     (row "commit" (short-commit (:commit item)))
     (when failure (str (row "failure kind" (:kind failure))
                        (row "failure stage" (:stage failure))))
     (when fc (str (row "feature card: built" (:built fc))
                   (row "feature card: matches-intent?" (:matches-intent? fc))))
     (when ocr (str (row "open-cascade-refinement status" (:status ocr))
                    (row "open-cascade-refinement pattern" (:pattern ocr))))
     (row "addenda" (when (pos? addenda-count) addenda-count))
     "<div class=\"questions\"><span class=\"lbl\">pending review questions:</span><ul>"
     (apply str (for [o (:pending-objectives item)]
                  (str "<li><code>" (name o) "</code> &mdash; "
                       (esc (get objective-questions o)) "</li>")))
     "</ul></div>"
     "</div>")))

(defn render-page [root now]
  (let [items (read-records (fs/path root "items"))
        reviews (read-records (fs/path root "reviews"))
        addenda (addenda-by-attempt root)
        pending (pending-items root)
        week-ago (.minusSeconds now (* 7 86400))
        last-7 (count (filter #(try (.isAfter (java.time.Instant/parse (:queued-at %)) week-ago)
                                    (catch Exception _ false))
                              items))
        reviewed-count (count (distinct (map :attempt-id reviews)))
        grouped (->> pending
                     (sort-by queued-sort-key)
                     (reverse)
                     (group-by (fn [i] (subs (:queued-at i) 0 10))))]
    (str
     "<!DOCTYPE html><html><head><meta charset=\"utf-8\">"
     "<title>War Machine &mdash; Morning Brief (pending)</title>"
     "<style>body{font-family:monospace;margin:2rem;max-width:60rem;background:#111;color:#ddd}"
     "h1{font-size:1.3rem}h2{font-size:1.05rem;color:#9cf;margin-top:1.6rem}"
     "h3{font-size:1rem;margin-bottom:.2rem}code{color:#fc6}"
     ".hdr{border:1px solid #444;padding:.6rem;border-radius:4px;background:#181818}"
     ".item{border:1px solid #333;padding:.6rem;margin:.6rem 0;border-radius:4px;background:#181818}"
     ".row{margin:.1rem 0}.lbl{display:inline-block;min-width:16rem;color:#89a}"
     ".val{color:#ddd}.questions ul{margin:.2rem 0 0 0;padding-left:1.2rem}"
     ".note{color:#789;font-size:.85rem}</style></head><body>"
     "<h1>War Machine &mdash; Morning Brief (pending items)</h1>"
     "<div class=\"hdr\">pending: " (count pending)
     " &middot; reviewed (items with any review): " reviewed-count
     " &middot; items queued in the last 7 days: " last-7 "</div>"
     "<p class=\"note\">Read-only view. Reviews are recorded from the REPL via "
     "POST /api/alpha/morning-brief/review; name an item by its attempt-id above. "
     "Generated " now " UTC.</p>"
     (apply str (for [day (reverse (sort (keys grouped)))]
                  (str "<h2>" (esc day) " (" (count (get grouped day)) " pending)</h2>"
                       (apply str (map #(render-item % (get addenda (:attempt-id %) 0))
                                       (get grouped day))))))
     "</body></html>")))

(defn write-page! [root out-path]
  (let [out (fs/path out-path)]
    (fs/create-dirs (fs/parent out))
    (when-not (fs/writable? (or (fs/parent out) (fs/path ".")))
      (throw (ex-info "output directory is not writable" {:path (str out)})))
    (let [html (render-page root (java.time.Instant/now))]
      (spit (str out) html)
      {:path (str out) :bytes (count (.getBytes html "UTF-8"))
       :pending (count (pending-items root))})))

;; --- self-check over a fixture brief directory -------------------------------

(defn- spit-edn [path v] (spit (str path) (with-out-str (clojure.pprint/pprint v))))

(defn self-check []
  (let [dir (fs/create-temp-dir {:prefix "mb-page-check"})
        root (fs/path dir "brief")]
    (doseq [d ["items" "reviews" "addenda"]]
      (fs/create-dirs (fs/path root d)))
    ;; item 1: fully reviewed (all six objectives reviewed) -> must NOT appear
    (spit-edn (fs/path root "items" "a.edn")
              {:attempt-id "attempt-001"
               :queued-at "2026-10-20T10:00:00Z"
               :outcome :grounded-change :commit "abcdef1234567890"
               :selected-target :t/one :morning-brief/schema-version 1})
    ;; item 2: pending, older
    (spit-edn (fs/path root "items" "b.edn")
              {:attempt-id "attempt-002"
               :queued-at "2026-10-22T10:00:00Z"
               :outcome :build-failed :selected-target :t/two
               :failure {:kind :k :stage :s}})
    ;; item 3: pending, newer (must be listed first)
    (spit-edn (fs/path root "items" "c.edn")
              {:attempt-id "attempt-003"
               :queued-at "2026-10-24T10:00:00Z"
               :outcome :grounded-change :commit "1234567890abcdef"
               :selected-target :t/three})
    (doseq [o (item-objectives {:commit "x" :outcome :grounded-change})]
      (spit-edn (fs/path root "reviews" (str "r-" (name o) ".edn"))
                {:attempt-id "attempt-001" :objective o :answer :yes
                 :note "" :reviewer "check" :reviewed-at "2026-10-21T00:00:00Z"
                 :morning-brief/review-id (str "mbqa-check-" (name o))}))
    (let [out (fs/path dir "page.html")
          html (:path (write-page! root out))
          body (slurp html)
          i3 (.indexOf body "attempt-003")
          i2 (.indexOf body "attempt-002")
          listed-001 (.contains body "attempt-001")
          pending-count (count (pending-items root))]
      (when (or (not (pos? i3)) (not (pos? i2)) (>= i3 i2) listed-001
                (not= 2 pending-count))
        (throw (ex-info "self-check failed"
                        {:i3 i3 :i2 i2 :listed-001 listed-001
                         :pending-count pending-count})))
      (println "self-check OK: fixture 3 items (1 fully reviewed, 2 pending);"
               "page lists exactly attempt-003 then attempt-002; pending count" pending-count)
      (fs/delete-tree dir))))

;; --- entry -------------------------------------------------------------------

(let [args *command-line-args*]
  (if (= "--self-check" (first args))
    (self-check)
    (let [root (or (first args) "data/wm-morning-brief")
          out (or (second args)
                  "/var/www/zone.hyperreal.enterprises/wip/morning-brief/index.html")]
      (when-not (fs/directory? root)
        (throw (ex-info "brief root is not a directory" {:root root})))
      (let [r (write-page! root out)]
        (println "wrote" (:path r) (:bytes r) "bytes;"
                 "pending items:" (:pending r))))))

(println "morning-brief page script done")
