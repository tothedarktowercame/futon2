(ns futon2.aif.three-halves-square
  "Decidable runtime projection of one ThreeHalvesBlend.Square."
  (:require [clojure.set :as set]
            [clojure.string :as str]))

(def schema :wm/three-halves-square-v1)
(def roles [:G :I1 :I2 :B])
(def map-ends {:a1 [:G :I1] :a2 [:G :I2] :b1 [:I1 :B] :b2 [:I2 :B]})
(def ^:private part-for-role {:I1 "IF" :I2 "HOWEVER" :G "BECAUSE" :B "THEN"})

(defn- normalise-space [s]
  (str/join " " (str/split (str/trim (str s)) #"\s+")))

(defn pattern-parts
  "Parse PATTERN-TEXT into its written parts, keyed by header name. A part runs
   from its `+ NAME:` header line (text after the colon included) to the next
   header at the same or a lesser indent, or to the first non-blank line at the
   same or a lesser indent (a top-level tag or conclusion); nested sub-blocks
   such as `+ CHECK:` under `+ THEN:` stay inside their part. Lines are joined
   with newlines. Agrees with the 象 emitter's read_pattern in
   futon3c/scripts/occurrence_square.py."
  [pattern-text]
  (let [part-header #"^(\s*)\+\s+(IF|HOWEVER|THEN|BECAUSE):(?:\s*(.*))?$"
        any-header #"^(\s*)\+\s+[^:]+:.*$"
        indent-of (fn [line] (count (re-find #"^\s*" line)))
        finish (fn [{:keys [active lines] :as state}]
                 (if active
                   (-> state
                       (assoc-in [:parts active] (str/join "\n" lines))
                       (assoc :active nil :indent nil :lines []))
                   state))]
    (:parts
     (finish
      (reduce (fn [{:keys [active indent] :as state} line]
                (let [[_ spaces part first-line] (re-matches part-header line)
                      header-indent (some-> (re-matches any-header line) second count)
                      closes? (and active
                                   (or (and header-indent (<= header-indent indent))
                                       (and (not (str/blank? line))
                                            (<= (indent-of line) indent))))
                      state (if closes? (finish state) state)]
                  (cond
                    (and part (nil? (:active state)))
                    (assoc state :active part :indent (count spaces)
                           :lines (if (str/blank? first-line) [] [first-line]))
                    (:active state) (update state :lines conj line)
                    :else state)))
              {:parts {} :active nil :indent nil :lines []}
              (str/split-lines (str pattern-text)))))))

(defn build-from-situation-maps
  "Construct one validator-shaped square deterministically. AUTHOR-MAPS names
  one non-blank situation element and its evidence source for each of I1, I2,
  G and B. The author supplies semantics; this function supplies representation,
  pins and the commuting singleton maps."
  [pattern pattern-text pattern-source author-maps]
  (let [parts (pattern-parts pattern-text)
        ids {:G :g :I1 :i1 :I2 :i2 :B :b}
        missing (vec (for [role roles
                           :let [{:keys [element source]} (get author-maps role)]
                           :when (or (str/blank? (str element))
                                     (str/blank? (str source))
                                     (str/blank? (get parts (part-for-role role))))]
                       role))]
    (if (seq missing)
      {:status :refused :kind :situation-maps-incomplete :roles missing}
      {:schema schema
       :pattern pattern
       :objects
       (into {}
             (for [role roles
                   :let [id (ids role)
                         {:keys [element source]} (get author-maps role)]]
               [role {:text (get parts (part-for-role role))
                      :source pattern-source
                      :situation-source source
                      :theory {:elems #{id} :names {id element} :axioms #{}}}]))
       :maps {:a1 #{[:g :i1]} :a2 #{[:g :i2]}
              :b1 #{[:i1 :b]} :b2 #{[:i2 :b]}}
       :auxiliary {:b1 false :b2 false}})))

(defn parts-check
  "Check that each square object's text IS its assigned flexiarg part.
   The object is named by pattern and role (IF = I1, HOWEVER = I2, BECAUSE = G,
   THEN = B); the part is located by its header line and bounded by the next
   same-level header; the observation is equality of object text and part
   after whitespace normalisation. A text that is only a substring of its part
   (one word, the first sentence of a multi-line part) or a paraphrase is
   refused with the roles named. PATTERN-TEXT is supplied by the caller; this
   function performs no IO."
  [receipt pattern-text]
  (let [square (or (:blend-square receipt) receipt)
        parts (pattern-parts pattern-text)
        missing (vec (for [part ["IF" "HOWEVER" "BECAUSE" "THEN"]
                           :when (str/blank? (normalise-space (get parts part)))]
                       (keyword part)))]
    (if (seq missing)
      {:status :refused :kind :pattern-part-missing :parts missing}
      (let [mismatched
            (vec (for [role roles
                       :let [object-text (normalise-space (get-in square [:objects role :text]))
                             part-text (normalise-space (get parts (part-for-role role)))]
                       :when (or (str/blank? object-text)
                                 (not= part-text object-text))]
                   role))]
        (if (seq mismatched)
          {:status :refused :kind :square-text-not-from-pattern :roles mismatched}
          {:status :valid})))))

(defn- theory-valid? [{:keys [elems names axioms]}]
  (and (set? elems) (seq elems) (set? axioms)
       (map? names) (= elems (set (keys names)))
       (every? #(not (str/blank? (str %))) (vals names))
       (every? #(and (vector? %) (= 3 (count %)) (every? elems %)) axioms)))

(defn- pinned-object? [{:keys [text source theory]}]
  (and (not (str/blank? (str text)))
       (string? (:path source)) (not (str/blank? (:path source)))
       (string? (:sha256 source)) (re-matches #"[0-9a-f]{64}" (:sha256 source))
       (theory-valid? theory)))

(defn- functional? [rel]
  (every? (fn [[_ rows]] (= 1 (count (set (map second rows)))))
          (group-by first rel)))

(defn- map-valid? [objects map-id rel]
  (let [[from to] (map-ends map-id)
        domain (get-in objects [from :theory :elems])
        codomain (get-in objects [to :theory :elems])]
    (and (set? rel) (functional? rel)
         (every? (fn [pair]
                   (and (vector? pair) (= 2 (count pair))
                        (contains? domain (first pair))
                        (contains? codomain (second pair))))
                 rel))))

(defn- compose [left right]
  (set (for [[a b] left [b' c] right :when (= b b')] [a c])))

(defn validate [receipt]
  (let [objects (:objects receipt)
        maps (:maps receipt)
        aux (:auxiliary receipt)
        object-gap (vec (for [role roles :when (not (pinned-object? (get objects role)))] role))
        map-gap (vec (for [m (keys map-ends) :when (not (map-valid? objects m (get maps m)))] m))
        route1 (when (empty? map-gap) (compose (:a1 maps) (:b1 maps)))
        route2 (when (empty? map-gap) (compose (:a2 maps) (:b2 maps)))
        required-commutes? (or (:b1 aux) (:b2 aux) (= route1 route2))
        active-routes (concat (when-not (:b1 aux) route1)
                              (when-not (:b2 aux) route2))
        consistent? (and (empty? map-gap) (functional? active-routes))
        blend-elems (get-in objects [:B :theory :elems])
        covered-elems (when (empty? map-gap)
                        (set (concat (map second (:b1 maps))
                                     (map second (:b2 maps)))))
        uncovered (when (and (set? blend-elems) (set? covered-elems))
                    (set/difference blend-elems covered-elems))
        gaps (cond-> []
               (not= schema (:schema receipt)) (conj :square-schema)
               (seq object-gap) (conj {:kind :object-pin-or-theory :roles object-gap})
               (seq map-gap) (conj {:kind :map-direction-or-functionality :maps map-gap})
               (and (empty? map-gap) (not required-commutes?)) (conj :noncommutation)
               (and (empty? map-gap) (not consistent?)) (conj :inconsistency)
               (and (empty? map-gap) (seq uncovered))
               (conj {:kind :pushout-coverage :uncovered uncovered}))]
    (if (seq gaps)
      {:status :refused :kind :three-halves-square-invalid :missing-evidence gaps}
      (assoc receipt :status :valid))))

(defn valid? [receipt] (= :valid (:status (validate receipt))))
