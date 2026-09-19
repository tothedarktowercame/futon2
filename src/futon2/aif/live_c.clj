(ns futon2.aif.live-c
  "Live C, first cut (commissioned by claude-4, 2026-09-17, from Joe's three
  named sources). C is preference over OUTCOMES; the live C contributes
  WEIGHTED WANTED TOKENS in the cascade preference spec's existing shape —
  no second preference object beside the spec.

  Sources (Joe's 'plenty to get started'):
  1. harmony/aliveness — futon6/data/mission-wholeness.edn, Salingaros
     L = T·H over scope-tree centres. A token per :alive mission with
     L > 0, weighted by L. THE DARK ROOM: the weight is the PRODUCT L,
     never H alone — an empty scope-tree is perfectly harmonious and T is
     how much is going on, so L = 0 contributes no want token at all.
  2. completed missions — the `**Status:**` line of each mission file
     (futon2/holes/M-*.md, futon2/holes/missions/*.md, and the same shape
     under futon0 and futon3c). CLOSED/COMPLETE is the signal; a mission
     not yet closed wants its closure: a token :closed/<mission>, weight 1.
  3. capability stars — futon0/holes/missions/M-capability-star-map.graph.edn.
     A capability whose :status is not :satisfied is unreached: a token
     :star/<capability>, weight 1.

  Honesty: a source that is missing, unparseable or absent for a mission is
  a TYPED REFUSAL recorded in :refusals — it never contributes a default
  weight. A mission with no wholeness entry is refused per-mission
  (:mission-not-in-wholeness); completion still applies to it.

  Freshness guard (E-C-vector-live's non-negotiable exit condition, the
  D7a lesson): derive-live-c records :signature, a hash over every source file's
  sha256 AND the parsed content the derivation read. `stale?` re-reads and
  compares: C is stale the moment the corpus changes after C was derived,
  and it says so loudly. Shipped with the first commit, not after.

  Weighted tokens: the spec consumer (cascade-model-manifest/
  log-preference-fn) weights every want token uniformly lam/|want|. This
  namespace emits :want (the set the consumer takes today) plus :weights
  (per-token positive rationals) for the consumer's :weights extension;
  weights are normalised so their sum is lam, matching the uniform law's
  total, so a weighted spec and a uniform spec of the same want are the
  same strength of preference, differently distributed."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str])
  (:import [java.security MessageDigest]))

(defn- file-bytes [f]
  (java.nio.file.Files/readAllBytes (.toPath f)))

(defn- file-seq-glob
  "Files under the glob's parent directory whose name matches the glob
  ('*' matches any run of characters). Sorted by path so the derivation is
  deterministic."
  [glob]
  (let [f (io/file glob)
        dir (.getParentFile f)
        pat (->> (str/split (.getName f) #"\*")
                 (map #(java.util.regex.Pattern/quote %))
                 (str/join ".*")
                 (re-pattern))]
    (->> (file-seq dir)
         (filter #(.isFile ^java.io.File %))
         (filter #(re-find pat (.getName ^java.io.File %)))
         (sort-by #(.getPath ^java.io.File %)))))

(def wholeness-path "/home/joe/code/futon6/data/mission-wholeness.edn")
(def star-graph-path "/home/joe/code/futon0/holes/missions/M-capability-star-map.graph.edn")
(def mission-file-globs
  "Mission markdown roots Joe named: futon2's two shapes, and the same shape
  in futon0 and futon3c."
  ["/home/joe/code/futon2/holes/M-*.md"
   "/home/joe/code/futon2/holes/missions/M-*.md"
   "/home/joe/code/futon0/holes/M-*.md"
   "/home/joe/code/futon0/holes/missions/M-*.md"
   "/home/joe/code/futon3c/holes/M-*.md"
   "/home/joe/code/futon3c/holes/missions/M-*.md"])

(defn sha256 [^bytes bs]
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (MessageDigest/getInstance "SHA-256") bs))))

;; ---------------------------------------------------------------------------
;; Reading (IO; every read records its path and sha256 for the guard)
;; ---------------------------------------------------------------------------

(defn- read-edn-source [path]
  (let [f (io/file path)]
    (if-not (.isFile f)
      {:refusal {:kind :source-missing :path path}}
      (let [bs (file-bytes f)]
        (try {:path (.getCanonicalPath f) :sha256 (sha256 bs)
              :value (edn/read-string (slurp f))}
             (catch Exception e
               {:refusal {:kind :source-unparseable :path path
                          :error (str (.getMessage e))}}))))))

(defn- read-mission-file [f]
  (let [bs (file-bytes f)]
    {:path (.getCanonicalPath f) :sha256 (sha256 bs)
     :mission (str/replace (.getName f) #"\.md$" "")
     :text (slurp f)}))

(defn read-sources
  "Read every source now. Returns {:wholeness … :missions […] :stars …} where
  each slot is the read record above or a typed :refusal map."
  []
  {:wholeness (read-edn-source wholeness-path)
   :stars (read-edn-source star-graph-path)
   :missions (into []
                   (comp (mapcat #(file-seq-glob %))
                         (map read-mission-file))
                   mission-file-globs)})

;; ---------------------------------------------------------------------------
;; Pure derivation
;; ---------------------------------------------------------------------------

(defn status-line
  "The mission's first `**Status:` line, or nil."
  [text]
  (some #(when (str/starts-with? (str/trim %) "**Status:") %)
        (str/split-lines text)))

(defn closed?
  "CLOSED/COMPLETE on the status line is the completion signal."
  [text]
  (boolean (some-> (status-line text)
                   (as-> s (re-find #"(?i)\b(CLOSED|COMPLETE)\b" s)))))

(defn- exact-pos? [x] (and (or (ratio? x) (integer? x)) (pos? x)))

(defn alive-entries
  "One want token per :alive mission with L > 0, weighted by L (the product
  T·H — never H alone: the dark room). A wholeness mission that is not
  :alive contributes nothing (recorded, not refused — :mess/:pipeline/:stub
  are the model's own classes, not source failures)."
  [wholeness]
  (vec (for [{:keys [mission class L]} (:missions wholeness)
             :when (and (= :alive class) (pos? (double L)))]
         {:token (keyword "alive" (name mission))
          :weight (if (exact-pos? L) L (rationalize (double L)))
          :source :wholeness :reason [:alive :L L]})))

(defn completion-entries
  "One want token per mission file not yet closed: its closure is wanted."
  [missions]
  (vec (for [{:keys [mission text]} missions
             :when (not (closed? text))]
         {:token (keyword "closed" mission) :weight 1
          :source :mission-status :reason [:not-closed (str/trim (or (status-line text) "no status line"))]})))

(defn unreached-stars
  "Capabilities whose :status is not :satisfied. nil/missing status is a
  per-capability typed refusal, not a default."
  [stars]
  (reduce (fn [acc [id cap]]
            (let [st (:status cap)]
              (cond
                (nil? st) (update acc :refusals conj
                                  {:kind :capability-status-missing :capability id})
                (= :satisfied st) acc
                :else (update acc :entries conj
                              {:token (keyword "star" (name id)) :weight 1
                               :source :star-map :reason [:unreached st]}))))
          {:entries [] :refusals []}
          (:capabilities stars)))

(defn normalise-weights
  "Scale every weight so the sum is lam (the uniform law's total), exact
  rationals. Refuses (typed) on an empty entry list — an empty belly is the
  defect this namespace exists to prevent — or a non-positive weight."
  [lam entries]
  (cond
    (empty? entries)
    {:refusal {:kind :empty-want
               :limitation "C with no want tokens is the hand with no belly: G collapses to pure information gain and the machine builds toward nothing"}}
    (some (comp not exact-pos?) (map :weight entries))
    {:refusal {:kind :invalid-weight :entries (vec (filter (comp not exact-pos?) (map :weight entries)))}}
    :else
    (let [total (reduce + (map :weight entries))]
      {:weights (into {} (map (fn [{:keys [token weight]}]
                                [token (/ (* (rationalize lam) weight) total)]))
                      entries)})))

(defn signature-of
  "The freshness signature: a hash over every source file's sha256 AND the
  parsed fields the derivation read (mission ids, status lines, wholeness
  rows, capability statuses) — so a content change that keeps the sha (a
  rewrite) still flips it, and a sha change that keeps the read (a
  reformat) is caught by the sha."
  [{:keys [wholeness missions stars]}]
  (sha256 (.getBytes (pr-str
                      {:wholeness (when-not (:refusal wholeness)
                                    (mapv (juxt :mission :class :L) (:missions (:value wholeness))))
                       :missions (mapv (juxt :mission :sha256
                                            #(some-> (:text %) status-line str/trim))
                                       missions)
                       :stars (when-not (:refusal stars)
                                (->> (sort (keys (:capabilities (:value stars))))
                                     (mapv (fn [id] [id (:status (get (:capabilities (:value stars)) id))]))))})
                     "UTF-8")))

(defn derive-live-c
  "Pure: sources (the read-sources shape, injectable for tests) -> the live C
  {:want #{} :weights {} :lam 1 :entries […] :refusals […] :signature s
   :sources-read […]}. Any MISSING top-level source is a refusal of the
  whole derivation (C is not silently degraded to the sources that remain);
  per-mission/per-capability gaps are recorded refusals beside the entries
  that could be derived."
  [{:keys [wholeness missions stars] :as sources}]
  (let [top-refusals (vec (keep (fn [[k v]] (when (:refusal v) (assoc (:refusal v) :source k)))
                                {:wholeness wholeness :stars stars}))]
    (if (seq top-refusals)
      {:refusals top-refusals
       :limitation "a named source is missing or unparseable: the live C refuses rather than deriving from a subset, because a partial belly is a stale setpoint wearing today's date"}
      (let [alive (alive-entries (:value wholeness))
            completion (completion-entries missions)
            star (unreached-stars (:value stars))
            ;; missions with no wholeness row: recorded refusal, completion
            ;; still applies (the status source stands on its own)
            wholeness-ids (set (map :mission (:missions (:value wholeness))))
            uncovered (vec (sort (remove wholeness-ids (map :mission missions))))
            ;; the same mission id can appear in more than one root
            ;; (holes/ and holes/missions/): one token per mission id —
            ;; the STRONGEST statement wins, ties by first, and the
            ;; collapse is recorded — duplicate entries would otherwise
            ;; make the normalised weights sum below 1 silently
            by-token (group-by :token (concat alive completion (:entries star)))
            entries (vec (for [[_token es] (sort-by key by-token)]
                           (reduce (fn [a b]
                                     (if (> (:weight b) (:weight a)) b a))
                                   (first es) (rest es))))
            deduped (- (+ (count alive) (count completion) (count (:entries star)))
                       (count entries))
            norm (normalise-weights 1 entries)]
        (if (:refusal norm)
          (assoc norm :refusals (concat top-refusals (:refusals star)))
          {:want (into #{} (map :token) entries)
           :weights (:weights norm)
           :lam 1
           :entries entries
           ;; :gaps are recorded, NON-BLOCKING per-mission/per-capability
           ;; holes (no wholeness row, no capability status): the derivation
           ;; stands, the gap is visible. :refusals stays for source-level
           ;; failures that refuse the whole derivation.
           :gaps (vec (concat (:refusals star)
                              (mapv (fn [m] {:kind :mission-not-in-wholeness :mission m}) uncovered)))
           :signature (signature-of sources)
           :deduped-entries deduped
           :sources-read (vec (concat (map (juxt :path :sha256) missions)
                                      [[(:path wholeness) (:sha256 wholeness)]
                                       [(:path stars) (:sha256 stars)]]))})))))

(defn stale?
  "The freshness guard, loud: C is stale the moment the corpus changes after
  C was derived. DERIVED is a derive-live-c result; SOURCES-NOW is a fresh
  read-sources. Returns {:stale? bool :signature-derived … :signature-now …}."
  [derived sources-now]
  (let [now (signature-of sources-now)]
    {:stale? (not= (:signature derived) now)
     :signature-derived (:signature derived)
     :signature-now now}))

;; ---------------------------------------------------------------------------
;; The spec the cascade scorer consumes
;; ---------------------------------------------------------------------------

(defn- mission-token?
  [token]
  (and (keyword? token)
       (contains? #{"alive" "closed"} (namespace token))))

(defn- token-mission
  [token]
  (when (mission-token? token) (name token)))

(defn project-want
  "Project mission-grain live-C tokens into JOINT-WANT's target-qualified
  outcome domain.  A mission token receives exactly the pairs that mission
  itself declared; its weight is divided across them, conserving its total
  mass.  Capability-grain :star tokens deliberately have no projection."
  [derived joint-want]
  (reduce
   (fn [acc token]
     (let [mission (token-mission token)
           pairs (if mission
                   (set (filter (fn [[target _]] (= mission (name target)))
                                joint-want))
                   #{})]
       (if (seq pairs)
         (let [share (/ (get (:weights derived) token) (count pairs))]
           (-> acc
               (update :want into pairs)
               (update :weights
                       (fn [ws]
                         (reduce #(update %1 %2 (fnil + 0) share) ws pairs)))
               (assoc-in [:projected-from token]
                         {:outcomes pairs
                          :source-weight (get (:weights derived) token)
                          :outcome-weight share})))
         (update acc :unreached conj token))))
   {:want #{} :weights {} :projected-from {} :unreached #{}}
   (:want derived)))

(defn cascade-spec
  "The live C as the cascade preference spec cascade-model-manifest consumes,
  RESTRICTED to REACHABLE — the token domain of the comparison the spec will
  score (the joint target-qualified universe of the tick's family).

  Why the restriction is mandatory, not cosmetic: an unreachable want token
  is NOT common-mode. It enlarges |want|, which dilutes the uniform
  per-token share lam/|want| and so CHANGES the relative utility of
  reachable outcomes — global live-C tokens dropped into :want would
  distort the comparison they cannot legitimately speak to. A live-C token
  outside the comparison's domain is recorded under :unreached-in-domain,
  not silently kept. Zero-weight extra tokens belong in :universe (there
  they shift every candidate by exactly T·k·ln 2); this spec adds none.

  JOINT-WANT is the tick's set of [target want-token] pairs.  :alive/M and
  :closed/M project only to M's own declared pairs; :star/capability does not
  name a target and is therefore left under :unreached-in-domain.

  Returns {:want … :weights … :lam 1 :mu 0 :evidence #{} :zeroed #{}
  :live-c provenance}, or the typed refusal :no-reachable-want when no
  live-C want token lies in REACHABLE (an empty belly for this comparison
  refuses rather than scoring pure information gain)."
  ([derived reachable]
   ;; Compatibility for callers/tests already supplying live-C's own token
   ;; domain.  Production uses the target-qualified arity below.
   (cascade-spec derived reachable nil))
  ([derived reachable joint-want]
   (if (seq (:refusals derived))
     {:refusal {:kind :live-c-refused :refusals (:refusals derived)}}
     (let [{projected :want projected-weights :weights
            projected-from :projected-from projected-unreached :unreached}
           (when joint-want (project-want derived (set joint-want)))
           candidates (if joint-want projected (:want derived))
           candidate-weights (if joint-want projected-weights (:weights derived))
           in-domain (set/intersection candidates (set reachable))
           unreached (if joint-want
                       (set/union projected-unreached
                                  (set (for [[token {:keys [outcomes]}] projected-from
                                             :when (empty? (set/intersection outcomes in-domain))]
                                         token)))
                       (set/difference (:want derived) in-domain))]
       (if (empty? in-domain)
         {:refusal {:kind :no-reachable-want
                    :reachable (count (set reachable))
                    :live-want (count (:want derived))
                    :unreached-in-domain (vec (sort (map str unreached)))
                    :limitation "no projected live-C want lies in this comparison's outcome domain; scoring would be pure information gain — the dark room — so it refuses"}}
         {:want in-domain
          :weights (select-keys candidate-weights in-domain)
          :lam (:lam derived)
          :mu 0
          :evidence #{}
          :zeroed #{}
          :live-c {:signature (:signature derived)
                   :n-entries (count (:entries derived))
                   :n-in-domain (count in-domain)
                   :projection :mission-declared-wants
                   :projected-from projected-from
                   :unreached-in-domain (vec (sort (map str unreached)))
                   :gaps (:gaps derived)
                   :refusals (:refusals derived)}})))))
