(ns futon2.aif.full-loop-cohort
  "Append-only evidence store for a preregistered full-loop cohort.

  The unit is a scheduler opportunity, not a successful build. Checkpoint
  payloads use the existing flight calculus: a grounded term or a typed sorry.
  State is derived from immutable event files; no summary is authoritative."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as str])
  (:import [java.nio.channels FileChannel]
           [java.nio.charset StandardCharsets]
           [java.nio.file Files Path StandardOpenOption]
           [java.security MessageDigest]
           [java.time Instant]))

(def default-preregistration
  "/home/joe/code/futon2/holes/labs/M-aif-full-loop-41/cohort.edn")

(def default-data-root "/home/joe/code/futon2/data/wm-full-loop")

(def checkpoint-order
  [:time-step :selection :construction :dispatch :build :adjudication :closed])

(def outcome-kinds
  #{:grounded-change :grounded-no-change :artifact-only :abstained :no-selection
    :agent-unavailable :guardrail-refusal :dispatch-failed :build-failed
    :substrate-unavailable :incomplete})

(defn read-edn [path]
  (edn/read-string (slurp path)))

(defn typed-sorry? [x]
  (and (map? x) (keyword? (get-in x [:sorry :kind]))))

(defn grounded-term? [x]
  (and (map? x) (contains? x :judgment) (contains? x :ground)
       (some? (:ground x))))

(defn cell? [x]
  (or (grounded-term? x) (typed-sorry? x)))

(defn checkpoint-cell-errors [p checkpoint cell]
  (cond
    (typed-sorry? cell) []
    (not (grounded-term? cell)) [:not-a-flight-cell]
    :else
    (let [required (set (get-in p [:checkpoint-contract checkpoint]))
          judgment (:judgment cell)
          present (set (keys judgment))
          missing-top (mapv (fn [k] [:missing-judgment-key k])
                            (sort (remove present required)))
          code-required (when (= :time-step checkpoint)
                          (disj (set (get-in p [:machine-evolution
                                               :per-attempt-provenance]))
                                :semantic-epoch))
          code-present (set (keys (:code-state judgment)))
          missing-code (mapv (fn [k] [:missing-code-state-key k])
                             (sort (remove code-present code-required)))]
      (into missing-top missing-code))))

(defn preregistration-errors [p]
  (cond-> []
    (not (keyword? (:cohort/id p)))
    (conj :cohort-id-must-be-keyword)

    ;; The protocol invariant is a definite preregistered stopping target;
    ;; its magnitude belongs to each cohort's document (was pinned to the
    ;; first cohort's 40 before wm-outer-loop-41-v1).
    (not (pos-int? (get-in p [:stopping-rule :target])))
    (conj :target-must-be-positive-integer)

    (not= checkpoint-order (:checkpoint-order p))
    (conj :checkpoint-order-mismatch)

    (not= (set (butlast checkpoint-order))
          (conj (:required-before-close p) :time-step))
    (conj :required-checkpoints-mismatch)

    (true? (get-in p [:grounded-success :artifact-only-counts?]))
    (conj :artifact-only-cannot-count)

    (not (true? (get-in p [:machine-evolution :allowed?])))
    (conj :machine-evolution-contract-required)))

(defn valid-preregistration? [p]
  (empty? (preregistration-errors p)))

(defn- cohort-name [p]
  (name (:cohort/id p)))

(defn cohort-dir
  ([p] (cohort-dir p default-data-root))
  ([p data-root] (io/file data-root (cohort-name p))))

(defn- sha256 [s]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                        (.getBytes ^String s StandardCharsets/UTF_8))]
    (apply str (map #(format "%02x" (bit-and 0xff %)) digest))))

(defn- write-new! [path value]
  (let [^Path p (if (instance? Path path) path (.toPath (io/file path)))
        bytes (.getBytes (with-out-str (pp/pprint value)) StandardCharsets/UTF_8)]
    (when-let [parent (.getParent p)] (Files/createDirectories parent (make-array java.nio.file.attribute.FileAttribute 0)))
    (Files/write p bytes (into-array StandardOpenOption [StandardOpenOption/CREATE_NEW
                                                         StandardOpenOption/WRITE]))
    (str p)))

(defn- with-cohort-lock [dir f]
  (.mkdirs (io/file dir))
  (let [path (.toPath (io/file dir ".cohort.lock"))]
    (with-open [channel (FileChannel/open path
                                         (into-array StandardOpenOption
                                                     [StandardOpenOption/CREATE
                                                      StandardOpenOption/WRITE]))
                _lock (.lock channel)]
      (f))))

(defn activation-path [dir]
  (io/file dir "activation.edn"))

(defn activate!
  ([] (activate! default-preregistration default-data-root))
  ([prereg-path data-root]
   (let [raw (slurp prereg-path)
         p (edn/read-string raw)
         errors (preregistration-errors p)
         dir (cohort-dir p data-root)]
     (when (seq errors)
       (throw (ex-info "invalid full-loop preregistration" {:errors errors})))
     (with-cohort-lock dir
       #(write-new! (activation-path dir)
                    {:cohort/id (:cohort/id p)
                     :activated-at (str (Instant/now))
                     :preregistration-path prereg-path
                     :preregistration-sha256 (sha256 raw)
                     :stopping-target (get-in p [:stopping-rule :target])})))))

(defn- attempt-dirs [dir]
  (->> (or (.listFiles (io/file dir)) [])
       (filter #(.isDirectory %))
       (filter #(re-matches #"attempt-\d{3}" (.getName %)))
       (sort-by #(.getName %))
       vec))

(defn attempt-events [attempt-dir]
  (->> (or (.listFiles (io/file attempt-dir)) [])
       (filter #(re-matches #"\d{3}-[a-z-]+\.edn" (.getName %)))
       (sort-by #(.getName %))
       (mapv #(read-edn (.getPath %)))))

(defn- all-events [dir]
  (mapcat attempt-events (attempt-dirs dir)))

(defn- opened-opportunity-ids [dir]
  (->> (all-events dir)
       (filter #(= :time-step (:checkpoint/type %)))
       (keep #(get-in % [:payload :judgment :opportunity-id]))
       set))

(defn- format-attempt-id [n]
  (format "attempt-%03d" n))

(defn- next-global-attempt-number
  "Attempt ids must be unique across cohorts: every attempt-id-keyed store
  (morning-brief items, reviews, repair obligations) is global, so a second
  cohort reusing attempt-001 collides (observed 2026-07-21, cohort 41,
  FileAlreadyExistsException on the brief item). Ordinals stay per-cohort;
  only the id number is global."
  [data-root]
  (->> (or (.listFiles (io/file data-root)) [])
       (filter #(.isDirectory %))
       (mapcat #(attempt-dirs %))
       (keep #(second (re-matches #"attempt-(\d+)" (.getName %))))
       (map parse-long)
       (reduce max 0)
       inc))

(defn- event-record [p attempt-id ordinal sequence checkpoint payload]
  {:event/schema-version 1
   :cohort/id (:cohort/id p)
   :attempt/id attempt-id
   :attempt/ordinal ordinal
   :event/sequence sequence
   :checkpoint/type checkpoint
   :recorded-at (str (Instant/now))
   :payload payload})

(defn start-attempt!
  "Claim one natural scheduler opportunity. `cell` must ground the recorded
  opportunity and include :opportunity-id and :trigger in its judgment."
  ([cell] (start-attempt! default-preregistration default-data-root cell))
  ([prereg-path data-root cell]
   (let [p (read-edn prereg-path)
         dir (cohort-dir p data-root)
         judgment (:judgment cell)
         opportunity-id (:opportunity-id judgment)
         trigger (:trigger judgment)
         semantic-epoch (:semantic-epoch judgment)]
     (when-not (valid-preregistration? p)
       (throw (ex-info "invalid full-loop preregistration"
                       {:errors (preregistration-errors p)})))
     (when-not (grounded-term? cell)
       (throw (ex-info "time-step must be a grounded term" {:cell cell})))
     (when-let [errors (seq (checkpoint-cell-errors p :time-step cell))]
       (throw (ex-info "incomplete time-step checkpoint" {:errors errors})))
     (when-not (and (string? opportunity-id) (not (str/blank? opportunity-id)))
       (throw (ex-info "missing opportunity-id" {:cell cell})))
     (when-not (contains? (:allowed-triggers p) trigger)
       (throw (ex-info "ineligible trigger" {:trigger trigger})))
     (when-not (or (keyword? semantic-epoch)
                   (and (string? semantic-epoch) (not (str/blank? semantic-epoch))))
       (throw (ex-info "missing semantic epoch" {:cell cell})))
     (with-cohort-lock dir
       (fn []
         (when-not (.exists (activation-path dir))
           (throw (ex-info "cohort is not activated" {:cohort (:cohort/id p)})))
         (when (contains? (opened-opportunity-ids dir) opportunity-id)
           (throw (ex-info "duplicate scheduler opportunity"
                           {:opportunity-id opportunity-id})))
         (let [ordinal (inc (count (attempt-dirs dir)))
               target (get-in p [:stopping-rule :target])]
           (when (> ordinal target)
             (throw (ex-info "cohort stopping rule reached"
                             {:target target :attempted (dec ordinal)})))
           (let [attempt-id (format-attempt-id
                             (max (next-global-attempt-number data-root)
                                  (inc (count (attempt-dirs dir)))))
                 attempt-dir (io/file dir attempt-id)
                 event (event-record p attempt-id ordinal 1 :time-step cell)]
             (Files/createDirectory (.toPath attempt-dir)
                                    (make-array java.nio.file.attribute.FileAttribute 0))
             (write-new! (io/file attempt-dir "001-time-step.edn") event)
             event)))))))

(defn- checkpoint-index [k]
  (.indexOf checkpoint-order k))

(defn append-checkpoint!
  ([attempt-id checkpoint cell]
   (append-checkpoint! default-preregistration default-data-root attempt-id checkpoint cell))
  ([prereg-path data-root attempt-id checkpoint cell]
   (let [p (read-edn prereg-path)
         dir (cohort-dir p data-root)
         attempt-dir (io/file dir attempt-id)]
     (when-not (contains? (set checkpoint-order) checkpoint)
       (throw (ex-info "unknown checkpoint" {:checkpoint checkpoint})))
     (when (= :time-step checkpoint)
       (throw (ex-info "time-step is created only by start-attempt!" {})))
     (when-let [errors (seq (checkpoint-cell-errors p checkpoint cell))]
       (throw (ex-info "invalid checkpoint cell"
                       {:checkpoint checkpoint :errors errors :cell cell})))
     (with-cohort-lock dir
       (fn []
         (when-not (.isDirectory attempt-dir)
           (throw (ex-info "unknown attempt" {:attempt-id attempt-id})))
         (let [events (attempt-events attempt-dir)
               last-event (last events)
               last-type (:checkpoint/type last-event)]
           (when (= :closed last-type)
             (throw (ex-info "attempt is already closed" {:attempt-id attempt-id})))
           (when (< (checkpoint-index checkpoint) (checkpoint-index last-type))
             (throw (ex-info "checkpoint order regression"
                             {:previous last-type :checkpoint checkpoint})))
           (let [sequence (inc (count events))
                 event (event-record p attempt-id (:attempt/ordinal last-event)
                                     sequence checkpoint cell)
                 filename (format "%03d-%s.edn" sequence (name checkpoint))]
             (write-new! (io/file attempt-dir filename) event)
             event)))))))

(defn- grounded-close-errors [cell]
  (let [j (:judgment cell)
        witness (:witness j)]
    (cond-> []
      (not (contains? outcome-kinds (:outcome j))) (conj :unknown-outcome)
      (and (= :grounded-change (:outcome j)) (not (true? (:grounded? j))))
      (conj :grounded-change-must-be-grounded)
      (and (= :grounded-change (:outcome j)) (not (false? (:artifact-only? j))))
      (conj :grounded-change-cannot-be-artifact-only)
      (and (= :grounded-change (:outcome j))
           (not (and (map? witness) (:before witness) (:after witness)
                     (true? (:resolved? witness)) (true? (:dial-moved? witness)))))
      (conj :grounded-change-requires-resolved-before-after-dial))))

(defn close-attempt!
  ([attempt-id cell]
   (close-attempt! default-preregistration default-data-root attempt-id cell))
  ([prereg-path data-root attempt-id cell]
   (let [p (read-edn prereg-path)
         dir (cohort-dir p data-root)
         events (attempt-events (io/file dir attempt-id))
         present (set (map :checkpoint/type events))
         missing (remove present (:required-before-close p))
         errors (when (grounded-term? cell)
                  (into (checkpoint-cell-errors p :closed cell)
                        (grounded-close-errors cell)))]
     (when-not (grounded-term? cell)
       (throw (ex-info "close must be a grounded outcome term" {:cell cell})))
     (when (seq missing)
       (throw (ex-info "required checkpoints missing" {:missing (vec missing)})))
     (when (seq errors)
       (throw (ex-info "invalid close outcome" {:errors errors})))
     (append-checkpoint! prereg-path data-root attempt-id :closed cell))))

(defn attempt-summary [attempt-dir]
  (let [events (attempt-events attempt-dir)
        close (last (filter #(= :closed (:checkpoint/type %)) events))
        selection (last (filter #(= :selection (:checkpoint/type %)) events))
        build (last (filter #(= :build (:checkpoint/type %)) events))
        artifact-binding
        (get-in build [:payload :judgment :validation :artifact-binding])]
    {:attempt/id (:attempt/id (first events))
     :attempt/ordinal (:attempt/ordinal (first events))
     :opportunity-id (get-in (first events) [:payload :judgment :opportunity-id])
     :selected-mission (get-in selection [:payload :judgment :selected-mission])
     :closed? (boolean close)
     :outcome (get-in close [:payload :judgment :outcome])
     :grounded? (true? (get-in close [:payload :judgment :grounded?]))
     :artifact-only? (true? (get-in close [:payload :judgment :artifact-only?]))
     :artifact-binding artifact-binding
     :fresh-commit (when (and (:fresh-author? artifact-binding)
                              (:commit artifact-binding)
                              (not= (:pre-dispatch-head artifact-binding)
                                    (:commit artifact-binding)))
                     (:commit artifact-binding))
     :typed-sorries (count (filter #(typed-sorry? (:payload %)) events))
     :checkpoints (mapv :checkpoint/type events)}))

(defn ledger
  ([] (ledger default-preregistration default-data-root))
  ([prereg-path data-root]
   (let [p (read-edn prereg-path)
         dir (cohort-dir p data-root)
         attempts (mapv attempt-summary (attempt-dirs dir))
         outcomes (frequencies (keep :outcome attempts))]
     {:cohort/id (:cohort/id p)
      :protocol/version (:protocol/version p)
      :activation (when (.exists (activation-path dir))
                    (read-edn (activation-path dir)))
      :target (get-in p [:stopping-rule :target])
      :attempt-count (count attempts)
      :remaining (- (get-in p [:stopping-rule :target]) (count attempts))
      :closed-count (count (filter :closed? attempts))
      :grounded-change-count (get outcomes :grounded-change 0)
      :grounded-no-change-count (get outcomes :grounded-no-change 0)
      :artifact-only-count (get outcomes :artifact-only 0)
      :outcomes outcomes
      :attempts attempts})))

(defn write-ledger!
  ([out-path] (write-ledger! default-preregistration default-data-root out-path))
  ([prereg-path data-root out-path]
   (let [value (ledger prereg-path data-root)]
     (io/make-parents out-path)
     (spit out-path (with-out-str (pp/pprint value)))
     out-path)))

(defn- html-escape [x]
  (-> (str x)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")
      (str/replace "\"" "&quot;")))

(defn render-html [value]
  (let [attempts (:attempts value)
        activated (get-in value [:activation :activated-at])]
    (str "<!doctype html><html><head><meta charset=\"utf-8\">"
         "<title>WM full-loop cohort</title>"
         "<style>body{font:14px/1.5 system-ui,sans-serif;max-width:1100px;margin:28px;color:#202020}"
         "h1{font-size:22px}.tiles{display:flex;gap:12px;flex-wrap:wrap}.tile{padding:10px 14px;border:1px solid #d7d2c7;border-radius:7px;background:#faf8f2}.v{font-size:24px;font-weight:700}"
         "table{border-collapse:collapse;width:100%;margin-top:20px}th,td{border:1px solid #ddd;padding:5px 8px;text-align:left}th{background:#f3efe6}.muted{color:#666}</style></head><body>"
         "<h1>WM outer/full-loop cohort <span class=\"muted\">fresh evidence epoch</span></h1>"
         "<p>Protocol <code>" (html-escape (:cohort/id value)) "</code>. "
         (if activated
           (str "Activated " (html-escape activated) ".")
           "Not activated; no opportunity can yet enter the cohort.")
         " Historical artifact-only ticks are excluded.</p><div class=\"tiles\">"
         (apply str
                (for [[label n] [["attempts" (:attempt-count value)]
                                 ["remaining" (:remaining value)]
                                 ["closed" (:closed-count value)]
                                 ["grounded change" (:grounded-change-count value)]
                                 ["grounded no-change" (:grounded-no-change-count value)]
                                 ["artifact only" (:artifact-only-count value)]]]
                  (str "<div class=\"tile\"><div class=\"v\">" (html-escape n)
                       "</div><div>" label "</div></div>")))
         "</div><table><tr><th>#</th><th>opportunity</th><th>mission</th><th>outcome</th><th>typed sorries</th><th>checkpoints</th></tr>"
         (if (seq attempts)
           (apply str
                  (for [a attempts]
                    (str "<tr><td>" (html-escape (:attempt/ordinal a))
                         "</td><td>" (html-escape (:opportunity-id a))
                         "</td><td>" (html-escape (or (:selected-mission a) "—"))
                         "</td><td>" (html-escape (or (:outcome a) :open))
                         "</td><td>" (html-escape (:typed-sorries a))
                         "</td><td>" (html-escape (str/join " → " (map name (:checkpoints a))))
                         "</td></tr>")))
           "<tr><td colspan=\"6\" class=\"muted\">No attempts recorded.</td></tr>")
         "</table></body></html>")))

(defn write-ledgers!
  ([out-edn out-html]
   (write-ledgers! default-preregistration default-data-root out-edn out-html))
  ([prereg-path data-root out-edn out-html]
   (let [value (ledger prereg-path data-root)]
     (io/make-parents out-edn)
     (io/make-parents out-html)
     (spit out-edn (with-out-str (pp/pprint value)))
     (spit out-html (render-html value))
     {:edn out-edn :html out-html})))
