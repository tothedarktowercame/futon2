(ns futon2.aif.focus-receipt
  "Record-only focus discovery from frozen inputs. Never supplies scoring C."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [futon2.aif.action-identity :as identity]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [futon2.aif.load-identity :as load-identity])
  (:import [java.time Instant]))

(load-identity/register! *ns* *file*)

(defn- absent [reason] {:status :absent :reason reason})
(defn- instant [s] (Instant/parse s))
(defn- at-or-before? [a b] (not (.isAfter (instant a) (instant b))))
(defn read-inputs []
  (try
    (json/parse-string (slurp (io/resource "wm/focus/commit-facets-v1.json")) true)
    (catch Exception _ (absent :discovery-inputs-unavailable))))

(defn- facets [paths]
  ;; resources/wm/ counts as WM (PROOF-wm-works 1.3, 2026-09-22): the machine's
  ;; own runtime resources live there (cascade-sources, rechecks, eig), and
  ;; classing them unrelated mis-faceted every WM repair. The segment must be
  ;; exactly wm (anchored by / on both sides), so wmx/, wm/ inside other names,
  ;; or a path merely containing "wm" still does not match.
  (let [wm #"(^|/)(wm-contract|WarMachine|aif)(/|$)|(^|/)resources/wm(/|$)|(^|/)(war_machine|wm_|M-war-machine|M-wm-|M-G-wm|M-aif-policy)"
        apm #"(^|/)(apm|apm-lean)(/|$)|(^|/)(M-apm-|apm_|apm-|countdown_manifest)|^problems/"
        result (cond-> #{} (some #(re-find wm %) paths) (conj "WM")
                       (some #(re-find apm %) paths) (conj "APM"))]
    (if (seq result) result #{"other/unattributed"})))

(defn discover
  "Recompute path credit inside the frozen window, with no current git/store reads.
   Previous focus persists: this slice has no completion/transition authority."
  [inputs as-of previous]
  (let [window (last (sort-by :from
                             (filter #(and (at-or-before? (:from %) as-of)
                                           (at-or-before? as-of (:valid-through %))) (:windows inputs))))
        rows (filterv #(and (at-or-before? (:from window) (:at %))
                           (at-or-before? (:at %) (:until window))
                           (at-or-before? (:at %) as-of)) (:commits window))
        credits (reduce (fn [acc row]
                          (let [fs (facets (:paths row))]
                            (reduce #(update %1 %2 (fnil + 0) (/ 1 (count fs))) acc fs))) {} rows)
        best (when (seq credits) (apply max (vals credits)))
        winners (vec (sort (for [[f c] credits :when (= c best)] f)))
        discovered (when (and (= 1 (count winners)) (not= "other/unattributed" (first winners))) (first winners))
        focus (or (:focus previous) discovered)
        edges (filterv #(at-or-before? (:effective-from %) as-of) (:facet-edges inputs))
        ;; PROOF-wm-works 1.3 handoff B(1): an established focus that has not
        ;; been completed is RETAINED at the actual time even when no
        ;; discovery window covers it -- the docstring's own persistence
        ;; semantics ("previous focus persists: this slice has no
        ;; completion/transition authority"). The retention is explicit
        ;; (:focus-origin :retained, :focus-status :retained, original
        ;; evidence date under :retained-evidence-as-of); a genuinely
        ;; unknown focus (no previous, no window) stays :unknown.
        retained? (and (nil? window) (:focus previous))]
    {:status (cond (and window focus) :discovered
                   retained? :retained
                   :else :unknown)
     :reason (cond (nil? window) (when-not retained? :discovery-window-unavailable)
                   (nil? focus) :no-unique-attributed-focus)
     :focus (if (or window retained?) focus nil) :as-of as-of
     ;; Retaining an already-retained focus keeps the ORIGINAL evidence
     ;; date, not the previous receipt's own as-of (codex-20 review of
     ;; c188d583: repeated retention advanced 17:31:44 -> 18:00:00).
     :retained-evidence-as-of (when retained? (or (:retained-evidence-as-of previous)
                                                  (:as-of previous)))
     :window (if window (assoc (dissoc window :commits)
                              :source-until (:until window)
                              :until (if (at-or-before? as-of (:until window)) as-of (:until window)))
                 (absent :discovery-window-unavailable))
     :commit-count (count rows) :facet-credit credits :commits rows
     :previous-focus (or previous (absent :previous-focus-not-retained))
     :completion (absent :completion-authority-not-consumed)
     :transition {:status :held :reason :record-only-no-transition-authority}
     :focus-origin (if retained? :retained-unfinished-focus
                       (if (:focus previous) :retained-unfinished-focus :commit-facets))
     :facet-graph {:active (if focus [focus] [])
                   :background (vec (sort (set (keep (fn [e]
                                                      (cond (= focus (:from e)) (:to e)
                                                            (= focus (:to e)) (:from e))) edges))))
                   :edges edges}}))

(defn ticket-parent
  "PROOF-wm-works 1.3 shared relation producer: a ticket's Parent line
   (holes/tickets/T-*.md). Accepts both the real format 'Parent: <mission>'
   and the bolded '**Parent:** <mission>' (codex-20 correction 2)."
  [ticket-file]
  (try
    (some->> (slurp ticket-file)
             str/split-lines
             (some #(second (re-matches #"^\*{0,2}Parent:\*{0,2}\s+(\S+)" %))))
    (catch Exception _ nil)))

;; ---------------------------------------------------------------------------
;; WM-RELATION-I: an M- target with no row derives its relation, in order,
;; from (a) its mission's stated ## Relations, walked through M- targets to
;; the first target with a row, then (b) its nearest rowed neighbour in the
;; pinned structure embedding; else it stays :unknown with the reason. Joe,
;; 2026-09-26: "'I do not know how this relates to anything' is not a good
;; answer" when the machine holds the graph and the embedding. Each
;; derivation is a reading with its receipt (:derived-via), not a value
;; standing in for an absence.

(def relation-hop-bound
  "How many stated-relation hops (a) walks (claude-8, WM-RELATION-I)."
  2)

(defn- sha256-file [f]
  (let [md (java.security.MessageDigest/getInstance "SHA-256")]
    (with-open [in (io/input-stream f)]
      (let [buf (byte-array 65536)]
        (loop []
          (let [n (.read in buf)]
            (when (pos? n) (.update md buf 0 n) (recur))))))
    (apply str (map #(format "%02x" (bit-and 0xff %)) (.digest md)))))

(defn- default-mission-text
  "The mission file for TARGET under CODE-ROOT's primary futon checkouts
  (<repo>/holes/M-*.md or <repo>/holes/missions/M-*.md), as {:path :text
  :sha256}, or nil."
  [code-root target]
  (some (fn [repo]
          (some (fn [sub]
                  (let [f (io/file repo sub (str target ".md"))]
                    (when (.isFile f)
                      {:path (.getCanonicalPath f) :text (slurp f) :sha256 (sha256-file f)})))
                ["holes/missions" "holes"]))
        (sort-by #(.getName ^java.io.File %)
                 (filter #(and (.isDirectory ^java.io.File %)
                               (re-matches #"futon\d+[a-z]?" (.getName ^java.io.File %)))
                         (or (.listFiles (io/file code-root)) [])))))

(defn stated-relations
  "The M- targets a mission's `## Relations` section names, in order, each
  {:to id :line n :quote text} (the line, 1-based, as in the file)."
  [text]
  (let [lines (vec (str/split-lines text))
        start (first (keep-indexed (fn [i l] (when (re-matches #"^##\s+Relations\s*$" l) i)) lines))]
    (when start
      (let [end (or (first (keep-indexed (fn [i l] (when (and (> i start) (re-find #"^##\s" l)) i)) lines))
                    (count lines))]
        (vec (distinct
              (for [i (range (inc start) end)
                    :let [l (lines i)]
                    id (map second (re-seq #"(?<![A-Za-z0-9-])(M-[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)" l))]
                {:to id :line (inc i) :quote (str/trim l)})))))))

(defn stated-relation-path
  "(a): breadth-first over TARGET's stated Relations through M- targets, at
  most relation-hop-bound hops, to the first target ROW-OF finds a row for.
  {:row r :derived-via {:kind :stated-relation :path [...] :parent id}} or
  {:absent reason}."
  [target row-of mission-text]
  (if-not (mission-text target)
    {:absent :mission-text-not-found}
    (loop [frontier [[target []]] seen #{target} hop 0]
      (if (or (empty? frontier) (>= hop relation-hop-bound))
        {:absent :no-stated-path-to-a-classified-target}
        (let [steps (for [[from path] frontier
                          :let [m (mission-text from)]
                          :when m
                          {:keys [to line quote]} (stated-relations (:text m))
                          :when (not (seen to))]
                      [to (conj path {:from from :to to :line line :quote quote
                                      :source {:path (:path m) :sha256 (:sha256 m)}})])
              hit (first (filter (fn [[to _]] (row-of to)) steps))]
          (if hit
            (let [[to path] hit]
              {:row (row-of to)
               :derived-via {:kind :stated-relation :path path :parent to}})
            (recur (vec (distinct steps)) (into seen (map first steps)) (inc hop))))))))

(defn- read-npy-f8
  "A little-endian float64 C-order .npy file as {:shape [r c] :rows [[..]..]}."
  [f]
  (let [bytes (java.nio.file.Files/readAllBytes (.toPath (io/file f)))
        bb (doto (java.nio.ByteBuffer/wrap bytes) (.order java.nio.ByteOrder/LITTLE_ENDIAN))
        major (aget bytes 6)
        hlen (if (= 1 major) (.getShort bb 8) (.getInt bb 8))
        off (if (= 1 major) 10 12)
        header (String. bytes (int off) (int hlen) "latin1")
        _ (when-not (and (str/includes? header "'<f8'") (str/includes? header "'fortran_order': False"))
            (throw (ex-info "unsupported npy" {:header header})))
        [r c] (map #(Long/parseLong %) (re-seq #"\d+" (second (re-find #"'shape':\s*\(([^)]*)\)" header))))
        base (+ off hlen)]
    {:shape [r c]
     :rows (vec (for [i (range r)]
                  (vec (for [j (range c)] (.getDouble bb (int (+ base (* 8 (+ (* i c) j)))))))))}))

(defn- cosine [a b]
  (let [dot (reduce + (map * a b)) na (Math/sqrt (reduce + (map * a a))) nb (Math/sqrt (reduce + (map * b b)))]
    (if (or (zero? na) (zero? nb)) 0.0 (/ dot (* na nb)))))

(defn embedding-neighbour
  "(b): TARGET's nearest rowed neighbour in the structure embedding the
  inputs pin (:embedding :source-pins, mission-embed.json and
  structure-embeddings.npy), re-hashed against the pins at read time. ROWED
  is {target row}. {:row r :derived-via {:kind :embedding-neighbour
  :neighbour :cosine :runner-up :candidates :pins :floor}} or {:absent
  reason …}. A floor applies only when the inputs declare
  :embedding :min-cosine; otherwise :floor {:absent :no-floor-declared}."
  [inputs target rowed]
  (let [pins (get-in inputs [:embedding :source-pins])
        pin-of (fn [suffix] (first (filter #(str/ends-with? (str (:path %)) suffix) pins)))
        jp (pin-of "mission-structure-embed/mission-embed.json")
        np (pin-of "mission-structure-embed/structure-embeddings.npy")]
    (cond
      (not (and jp np)) {:absent :embedding-not-pinned}
      (not (and (.isFile (io/file (:path jp))) (.isFile (io/file (:path np)))))
      {:absent :embedding-file-missing :pins [jp np]}
      :else
      (let [mismatch (vec (for [p [jp np] :let [now (sha256-file (:path p))] :when (not= now (:sha256 p))]
                            {:path (:path p) :pinned (:sha256 p) :now now}))]
        (if (seq mismatch)
          {:absent :embedding-pin-mismatch :mismatch mismatch}
          (let [stems (:stems (json/parse-string (slurp (:path jp)) true))
                {:keys [rows]} (read-npy-f8 (:path np))
                idx (into {} (map-indexed (fn [i s] [s i]) stems))
                stem (fn [t] (if (str/starts-with? t "M-") (subs t 2) t))
                me (idx (stem target))]
            (if-not me
              {:absent :embedding-node-not-retained}
              (let [cands (->> rowed
                               (keep (fn [[t _]] (when-let [i (idx (stem t))]
                                                   (when (not= t target) [t (cosine (rows me) (rows i))]))))
                               (sort-by (comp - second)) vec)
                    [[nt nc] runner] cands
                    floor (get-in inputs [:embedding :min-cosine])]
                (cond
                  (empty? cands) {:absent :no-classified-target-in-embedding}
                  (and (number? floor) (< nc floor))
                  {:absent :nearest-below-threshold :nearest nt :cosine nc :min-cosine floor}
                  :else
                  {:row (get rowed nt)
                   :derived-via {:kind :embedding-neighbour :neighbour nt :cosine nc
                                 :runner-up (or runner {:absent :no-runner-up})
                                 :candidates cands :pins [jp np]
                                 :floor (if (number? floor) {:min-cosine floor} {:absent :no-floor-declared})}})))))))))

(defn classify-target
  "THE shared relation producer (codex-20 ruling, handoff B): one
   classification for BOTH the scoring path and the close receipt. Accepts a
   :discovered OR :retained focus (no completion consumer distinguishes
   them). M- targets read their corpus relation; T- targets derive through
   the ticket's Parent line (or the finding record's target) to the parent's
   corpus row, with :derived-via recording the derivation. Unresolvable
   relations are :unknown with a reason -- distinct from an unmeasured
   outcome, and never guessed."
  ([inputs discovery as-of target]
   (classify-target inputs discovery as-of target nil))
  ([inputs discovery as-of target {:keys [ticket-dir findings-dir code-root mission-text-fn] :as ctx}]
   (let [row-of (fn [t] (first (filter #(and (= t (:target %)) (at-or-before? (:effective-from %) as-of)) (:relations inputs))))
         direct (row-of target)
         parent-source (when (and (nil? direct) (string? target) (str/starts-with? target "T-"))
                         (or (when ticket-dir
                             (when-let [p (ticket-parent (io/file ticket-dir (str target ".md")))]
                               {:kind :ticket-parent :parent p :source (str "ticket " target)}))
                           (when findings-dir
                             (try
                               (when-let [p (-> (edn/read-string (slurp (io/file findings-dir (str (subs target 2) ".edn"))))
                                                (:target))]
                                 {:kind :finding-target :parent p :source (str "finding " (subs target 2))})
                               (catch Exception _ nil)))))
         parent (:parent parent-source)
         ;; WM-RELATION-I: an M- target with no row, when a relation context
         ;; is given (the scoring path's), derives through (a) then (b)
         m-derivation (when (and (nil? direct) ctx (string? target) (str/starts-with? target "M-"))
                        (let [text (or mission-text-fn
                                       (memoize #(default-mission-text (or code-root (str (System/getProperty "user.home") "/code")) %)))
                              a (stated-relation-path target row-of text)]
                          (if (:row a)
                            a
                            (let [rowed (into {} (keep (fn [r] (when (at-or-before? (:effective-from r) as-of) [(:target r) r])))
                                              (reverse (:relations inputs)))
                                  b (embedding-neighbour inputs target rowed)]
                              (if (:row b)
                                (assoc-in b [:derived-via :stated-relation] {:absent (:absent a)})
                                {:absent (:absent b) :embedding b :stated-relation (:absent a)})))))
         relation-row (or direct
                          (when parent
                            (row-of parent))
                          (:row m-derivation))
         derived (cond (and parent-source relation-row (nil? direct)) parent-source
                       (:row m-derivation) (:derived-via m-derivation))
         facets (set (concat (get-in discovery [:facet-graph :active]) (get-in discovery [:facet-graph :background])))
         eligible (and (contains? #{:discovered :retained} (:status discovery)) (:source relation-row)
                       (contains? #{"focus" "associated" "useful-elsewhere"} (:relation relation-row))
                       (or (= "useful-elsewhere" (:relation relation-row)) (facets (:facet relation-row))))]
     {:target target
      :class (if eligible (keyword (:relation relation-row)) :unknown)
      :relation (if eligible
                  relation-row
                  (cond-> {:status :absent
                           :reason (cond (and (nil? relation-row) (:absent m-derivation)) (:absent m-derivation)
                                         (nil? relation-row) (if (and (string? target) (str/starts-with? target "T-")) :no-parent-relation :relation-not-declared)
                                         (not (contains? #{:discovered :retained} (:status discovery))) :focus-not-established
                                         :else :relation-outside-focus-facets)}
                    (and (nil? relation-row) (:absent m-derivation))
                    (assoc :derivation (dissoc m-derivation :absent))))
      :derived-via derived})))

(defn- classification [inputs discovery as-of candidate]
  (let [id (:id candidate) target (:target id)
        {:keys [class relation derived-via]} (classify-target inputs discovery as-of target)
        relation (if (= :unknown class) (or relation (absent :relation-not-declared)) relation)
        node (when (string? target) (subs target (if (.startsWith ^String target "M-") 2 0)))]
    {:candidate-id id :target target :class class
     :relation (if (= :unknown class) relation relation)
     :derived-via derived-via
     :embedding (if (some #{node} (get-in inputs [:embedding :nodes]))
                  {:status :present :node node :authority :presence-only}
                  (absent :embedding-node-not-retained))
     :outcome (absent :attested-outcome-not-inferred-from-prediction)}))

(defn build
  ([decision inputs context]
   (let [{:keys [as-of previous-focus relation-context classifications]} context
         discovery (discover inputs as-of previous-focus)
         candidates (get-in decision [:selection-certificate :candidates])
         ;; codex-20 correction 1: the receipt uses the SAME classification
         ;; the scoring path resolved when one is supplied (one decision, one
         ;; classification), else classifies with the same relation context.
         classify (fn [target]
                    (or (get classifications target)
                        (classify-target inputs discovery as-of target relation-context)))]
    {:schema :wm/focus-receipt-v1 :mode :record-only
     :inputs inputs :inputs-sha256 (identity/digest inputs)
     :context {:as-of as-of :previous-focus previous-focus}
     :rule (:rule inputs) :heads (:heads inputs) :discovery discovery
     :candidates (mapv (fn [c]
                         (let [t (:target (:id c))
                               {:keys [class relation derived-via]} (classify t)
                               node (when (string? t) (subs t (if (.startsWith ^String t "M-") 2 0)))]
                           (-> (classification inputs discovery as-of c)
                               (assoc :class class
                                      :relation (if (= :unknown class) relation relation)
                                      :derived-via derived-via))))
                       candidates)
     :global-preference (assoc (:global-preference inputs)
                               :temporal-status (if (and (get-in inputs [:global-preference :effective-from])
                                                         (at-or-before? (get-in inputs [:global-preference :effective-from]) as-of))
                                                  :available :retrospective-ruling))
     :outcome-domain {:id :attested-increment-or-known-typed-failure-v1
                      :map {:focus :attested-focus-increment
                            :associated :attested-associated-increment
                            :useful-elsewhere :attested-useful-elsewhere-increment
                            :known-failure :observed-typed-nondelivery}
                      :unobserved (absent :observation-is-not-a-valued-outcome)
                      :unrepresented-class-mass {:status :held :reason :outcome-kernel-unavailable
                                                 :declared-masses (get-in inputs [:global-preference :masses])}}
     :attestation (absent :attestation-join-not-wired)
     :kernel (absent :predictive-attestation-kernel-not-declared)
     :local-C {:status :held :reason :conditional-outcome-kernel-unavailable}})))

(defn attach
  ([decision] (attach decision (read-inputs) {:as-of (str (Instant/now))}))
  ([decision inputs context]
   (assoc-in decision [:selection-certificate :focus-receipt] (build decision inputs context))))

(defn valid? [decision receipt]
  (try (= receipt (build decision (:inputs receipt) (:context receipt)))
       (catch Exception _ false)))
