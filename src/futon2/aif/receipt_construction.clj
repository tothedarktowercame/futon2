(ns futon2.aif.receipt-construction
  "One receipt -> find -> ruled organise -> fold carrier. No retrieval or dispatch.
  Prior construction is read from closed attempt manifests, never joined by names alone."
  (:refer-clojure :exclude [bytes])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.cascade-policy :as policy]
            [futon2.aif.close-retention :as retention]
            [futon2.aif.evidence-manifest :as manifest]
            [futon2.aif.find-receipt :as finder]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.interpretation-evidence :as evidence])
  (:import [java.nio.file Files]
           [java.time Instant]))

(defn- need! [ok reason data]
  (when-not ok (throw (ex-info "Receipted construction refused"
                              (merge {:interpretation/refusal :interpretation/invalid-receipt
                                      :construction/refusal reason} data)))))
(defn- bytes [file] (Files/readAllBytes (.toPath (io/file file))))
(defn- read-one [file]
  (with-open [r (java.io.PushbackReader. (io/reader file))]
    (let [eof (Object.) x (edn/read {:eof eof} r)]
      (need! (and (map? x) (identical? eof (edn/read {:eof eof} r))) :invalid-history-record {:file (str file)}) x)))
(defn reaches? [edges a b]
  (let [rel (reduce (fn [m [u v]] (update m u (fnil conj #{}) v)) {} edges)]
    (loop [frontier (get rel a #{}) seen #{}]
      (cond (contains? frontier b) true (empty? frontier) false
            :else (recur (set/difference (set (mapcat #(get rel % #{}) frontier)) seen)
                         (into seen frontier))))))
(defn precedence [carrier edges]
  (loop [remaining carrier order []]
    (if (empty? remaining) order
        (let [ready (first (sort (filter (fn [u] (not-any? #(reaches? edges u %) remaining)) remaining)))]
          (need! (some? ready) :precedence-cycle {:remaining remaining})
          (recur (disj remaining ready) (conj order ready))))))
(defn validate-admissible! [order nodes edges]
  (let [firing (vec (filter nodes order))]
    (doseq [i (range (count firing)) j (range (inc i) (count firing))]
      (need! (not (reaches? edges (nth firing i) (nth firing j)))
             :inadmissible-order {:earlier (nth firing i) :later (nth firing j)})))
  true)
(defn acting-order [interpretations q0 order]
  (doseq [id order]
    (need! (contains? interpretations id) :previous-or-admitted-interpretation-missing {:pattern id}))
  (loop [facts q0 acted []]
    (if-let [id (first (filter #(and (not (some #{%} acted))
                                     (true? (finder/guard-value facts (:guard (get interpretations %))))) order))]
      (recur (merge facts (:effect (get interpretations id))) (conj acted id))
      acted)))

(defn- history-read! [file]
  (try (read-one file)
       (catch Exception e
         (throw (ex-info "History record cannot be read"
                         {:interpretation/refusal :interpretation/invalid-receipt
                          :construction/refusal :history-discovery-invalid
                          :reason :unreadable-or-malformed-record :file (str file)} e)))))

(defn- history-time! [value file]
  (try (Instant/parse value)
       (catch Exception e
         (throw (ex-info "History ordering is unavailable"
                         {:interpretation/refusal :interpretation/invalid-receipt
                          :construction/refusal :history-discovery-invalid
                          :reason :ordering-unavailable :file (str file) :value value} e)))))

(defn- history-root! [root]
  (need! (or (instance? java.io.File root) (and (string? root) (not (str/blank? root))))
         :history-discovery-invalid {:reason :invalid-search-root :file (str root)})
  (try (.getCanonicalPath (io/file root))
       (catch java.io.IOException e
         (throw (ex-info "History root cannot be resolved"
                         {:interpretation/refusal :interpretation/invalid-receipt
                          :construction/refusal :history-discovery-invalid
                          :reason :root-unresolvable :file (str root)} e)))))

(defn- history-children! [directory]
  (let [children (.listFiles (io/file directory))]
    (need! (some? children) :history-discovery-invalid
           {:reason :root-or-directory-unreadable :file (str directory)})
    children))

(defn- non-construction! [close-file closed construction]
  (let [dir (.getParentFile (io/file close-file))
        files (mapv #(io/file dir (format "%03d-%s.edn" %1 (name %2)))
                    (range 1 8) cohort/checkpoint-order)
        records (mapv (fn [file]
                        (case (.getName file)
                          "003-construction.edn" construction
                          "007-closed.edn" closed
                          (history-read! file))) files)
        ordinal (:attempt/ordinal closed)
        identity (select-keys closed [:cohort/id :attempt/id :attempt/ordinal])
        fail! (fn [ok reason file]
                (need! ok :history-discovery-invalid {:reason reason :file (str file)}))
        close-judgment (get-in closed [:payload :judgment])
        outcome (:outcome close-judgment)]
    (fail! (and (keyword? (:cohort/id closed))
                (= (name (:cohort/id closed)) (.getName (.getParentFile dir)))
                (= (:attempt/id closed) (.getName dir))
                (pos-int? ordinal)) :non-construction-identity close-file)
    (doseq [[index checkpoint record file] (map vector (range 1 8) cohort/checkpoint-order records files)]
      (fail! (and (= #{:event/schema-version :cohort/id :attempt/id :attempt/ordinal
                      :event/sequence :checkpoint/type :recorded-at :payload} (set (keys record)))
                  (= 1 (:event/schema-version record))
                  (= identity (select-keys record (keys identity)))
                  (= index (:event/sequence record)) (= checkpoint (:checkpoint/type record))
                  (or (and (map? (get-in record [:payload :sorry]))
                           (keyword? (get-in record [:payload :sorry :kind])))
                      (and (map? (get-in record [:payload :judgment]))
                           (some? (get-in record [:payload :ground]))))
                  (empty? (cohort/checkpoint-cell-errors {} checkpoint (:payload record))))
             :non-construction-checkpoint-invalid file))
    (let [times (mapv #(history-time! (:recorded-at %1) %2) records files)]
      (fail! (every? (fn [[a b]] (not (.isAfter ^Instant a b))) (partition 2 1 times))
             :non-construction-ordering close-file))
    ;; These exact sorries are emitted by close-core! for missing checkpoints.
    ;; A later grounded checkpoint would contradict not having constructed.
    (doseq [index (range 2 6)]
      (let [record (nth records index) checkpoint (nth cohort/checkpoint-order index)]
        (fail! (= {:sorry {:kind (keyword (str "not-reached-" (name checkpoint))) :outcome outcome}}
                  (:payload record)) :non-construction-marker-contradiction (nth files index))))
    (fail! (and (contains? cohort/outcome-kinds outcome)
                (not (#{:grounded-change :grounded-no-change :artifact-only
                        :historical-verification-awaiting-validation} outcome))
                (false? (:grounded? close-judgment)) (false? (:artifact-only? close-judgment))
                (nil? (:witness close-judgment))
                (not-any? #(contains? close-judgment %) [:cascade :receipted-construction :construction :commit])
                (= {:kind :full-loop-outcome :attempt-id (:attempt/id closed)} (get-in closed [:payload :ground])))
           :non-construction-close-contradiction close-file)
    (let [selection (nth records 1)
          cell (:payload selection)
          judgment (:judgment cell)
          action (:selected-action judgment)
          target (:target action)
          retained (get-in closed [:payload :close-retention])]
      (fail! (or (and (= #{:judgment :ground} (set (keys cell)))
                       (map? action) (keyword? (:type action)) (string? target) (not (str/blank? target))
                       (or (nil? (:selected-mission judgment)) (= target (:selected-mission judgment))))
                  (and (= #{:sorry} (set (keys cell))) (nil? judgment) (map? (:sorry cell))
                       (#{:no-selection :not-reached-selection} (get-in cell [:sorry :kind]))))
             :non-construction-selection-invalid (nth files 1))
      (when (= :present (get-in close-judgment [:outcome-entity :status]))
        (fail! (and target (= target (get-in close-judgment [:outcome-entity :entity/id])))
               :non-construction-target-contradiction close-file))
      (when (contains? (:payload closed) :close-retention)
        (try (retention/validate-retention-block retained)
             (catch clojure.lang.ExceptionInfo e
               (throw (ex-info "Invalid non-construction retention"
                               (merge (ex-data e) {:interpretation/refusal :interpretation/invalid-receipt
                                                   :construction/refusal :history-discovery-invalid
                                                   :reason :non-construction-retention-invalid :file (str close-file)}) e))))
        (fail! (and (= (str (:cohort/id closed)) (get-in retained [:occurrence :cohort/id]))
                    (= (:attempt/id closed) (get-in retained [:occurrence :attempt/id]))
                    (= action (get-in retained [:occurrence :action/value]))
                    (= (history-time! (:closed-at retained) close-file)
                       (history-time! (:recorded-at closed) close-file)))
               :non-construction-retention-identity close-file))
      (when (contains? (:payload closed) :close-evidence-manifest)
        (let [m (get-in closed [:payload :close-evidence-manifest])]
          (try
            (manifest/validate-manifest m)
            (manifest/verify-retention-agreement m retained)
            (doseq [file (butlast files)]
              (let [entry (first (filter #(= (.getCanonicalPath file)
                                             (.getCanonicalPath (io/file (:source-path %)))) (:entries m)))]
                (fail! (some? entry) :non-construction-manifest-binding-missing file)))
            (doseq [entry (:entries m)]
              (fail! (not (.isAfter (history-time! (:admitted-at entry) close-file)
                                   (history-time! (:recorded-at closed) close-file)))
                     :non-construction-manifest-after-close close-file)
              (fail! (= (:sha256 entry) (evidence/sha256 (bytes (:source-path entry))))
                     :non-construction-manifest-source-mismatch (:source-path entry)))
            (catch clojure.lang.ExceptionInfo e
              (throw (ex-info "Invalid non-construction manifest"
                              (merge (ex-data e) {:interpretation/refusal :interpretation/invalid-receipt
                                                  :construction/refusal :history-discovery-invalid
                                                  :file (str close-file)}) e)))
            (catch java.io.IOException e
              (throw (ex-info "Unreadable non-construction manifest source"
                              {:interpretation/refusal :interpretation/invalid-receipt
                               :construction/refusal :history-discovery-invalid
                               :reason :non-construction-manifest-source-unreadable :file (str close-file)} e)))))))
    {:non-construction? true :closed-at (history-time! (:recorded-at closed) close-file)
     :exclusion {:reason :producer-not-reached-construction :identity identity
                 :records (mapv (fn [file] {:file (.getCanonicalPath file)
                                            :sha256 (evidence/sha256 (bytes file))}) files)}}))

(defn- closed-candidate! [close-file]
  (let [closed (history-read! close-file)
        construction-file (io/file (.getParentFile (io/file close-file)) "003-construction.edn")
        construction (when (.exists construction-file) (history-read! construction-file))
        occurrence (get-in closed [:payload :close-retention :occurrence])
        judgment (get-in construction [:payload :judgment])
        targets (remove nil? [(get-in occurrence [:action/value :target])
                              (:mission judgment)
                              (get-in judgment [:cascade :selected-action :target])
                              (get-in judgment [:receipted-construction :identity :occurrence :action/value :target])])
        at (history-time! (:recorded-at closed) close-file)]
    (if (= :not-reached-construction (get-in construction [:payload :sorry :kind]))
      (non-construction! close-file closed construction)
      (do
        (when-let [retained-time (get-in closed [:payload :close-retention :closed-at])]
          (need! (= at (history-time! retained-time close-file)) :history-discovery-invalid
                 {:reason :contradictory-ordering :file (str close-file)}))
        (need! (and (seq targets) (every? #(and (string? %) (not (str/blank? %))) targets))
               :history-discovery-invalid {:reason :target-unavailable :file (str close-file)})
        (need! (apply = targets) :history-discovery-invalid
               {:reason :contradictory-targets :file (str close-file) :targets (vec targets)})
        {:closed closed :construction construction :close-file close-file :construction-file construction-file
         :occurrence occurrence :target (first targets) :closed-at at}))))

(defn- validated-previous! [{:keys [closed construction close-file construction-file] :as latest} roots]
  (try
      (let [_ (need! (:occurrence latest) :previous-occurrence-unavailable {:file (str close-file)})
            block (get-in closed [:payload :close-retention])
            m (get-in closed [:payload :close-evidence-manifest])
            retained (get-in construction [:payload :judgment :receipted-construction])
            _ (need! (map? (get-in construction [:payload :judgment :cascade]))
                     :previous-cascade-unavailable {:file (str construction-file)})
            _ (need! (map? retained) :previous-cascade-carrier-unavailable {:file (str construction-file)})
            prior-id (:identity retained)
            start-file (io/file (.getParentFile (io/file close-file)) "001-time-step.edn")
            start (history-read! start-file)
            root (.getCanonicalPath (.getParentFile (.getParentFile (.getParentFile (io/file close-file)))))
            d (:cascade-diff retained)]
        (retention/validate-retention-block block)
        (manifest/validate-manifest m)
        (manifest/verify-retention-agreement m block)
        (doseq [file [construction-file start-file]]
          (let [entry (first (filter #(= (.getCanonicalPath (io/file (:source-path %))) (.getCanonicalPath file)) (:entries m)))]
            (need! (and entry (= (:sha256 entry) (evidence/sha256 (bytes file))))
                   :previous-manifest-source-mismatch {:file (str file)})))
        (evidence/validate-identity prior-id)
        (need! (and (= (:occurrence prior-id) (:occurrence block))
                     (= (:semantic-epoch prior-id) (get-in start [:payload :judgment :semantic-epoch]))
                     (= (:data-root prior-id) root)
                     (= (:start-event-sha256 prior-id) (evidence/sha256 (bytes start-file)))
                     (= (get-in prior-id [:occurrence :cohort/id]) (str (:cohort/id construction)))
                     (= (get-in prior-id [:occurrence :attempt/id]) (:attempt/id construction)))
               :previous-occurrence-mismatch {:file (str construction-file)})
        (need! (= (:cascade-diff-sha256 retained) (evidence/value-digest d)) :previous-diff-digest-mismatch {})
        (need! (and (set? (:nodes d)) (vector? (:precedence-after d))
                     (vector? (:acting-order-after d))
                     (map? (get-in d [:provenance :admissions]))
                     (= (:admitted-by d) (set (keys (get-in d [:provenance :admissions])))))
               :previous-admission-carrier-invalid {})
        {:cascade {:nodes (:nodes d) :edges (:organised-edges d) :precedence (:precedence-after d)}
         ;; The acting order the previous attempt actually recorded; reused as
         ;; this attempt's before arm instead of being re-simulated.
         :acting-order (:acting-order-after d)
         :admitted (get-in d [:provenance :admissions])
         :admission-reason :carried-from-previous-occurrence
         :provenance {:identity prior-id :searched-roots roots :construction-file (.getCanonicalPath construction-file)
                      :construction-sha256 (evidence/sha256 (bytes construction-file))
                      :close-file (.getCanonicalPath (io/file close-file))
                      :close-sha256 (evidence/sha256 (bytes close-file))}})
    (catch clojure.lang.ExceptionInfo e
      (throw (ex-info "Previous construction validation refused"
                      (merge {:interpretation/refusal :interpretation/invalid-receipt
                              :construction/refusal :previous-evidence-invalid}
                             (ex-data e)
                             {:history/close-file (str close-file)
                              :history/construction-file (str construction-file)}) e)))))

(defn previous!
  "Latest relevant closed history in explicit roots, or established absence.
  Damaged or unclassifiable history refuses; missing modern evidence never resets."
  [identity roots]
  (let [target (get-in identity [:occurrence :action/value :target])
        _ (need! (and (string? target) (not (str/blank? target)))
                 :history-discovery-invalid {:reason :current-target-unavailable})
        before (history-time! (get-in identity [:occurrence :action-at]) :current-identity)
        roots (vec (distinct (map history-root! roots)))
        _ (need! (seq roots) :history-discovery-invalid {:reason :search-roots-unavailable})
        candidates (doall (for [root roots
                               cohort (history-children! root) :when (.isDirectory cohort)
                               attempt (history-children! cohort) :when (.isDirectory attempt)
                               :let [f (io/file attempt "007-closed.edn")]
                               :when (.exists f)]
                           (closed-candidate! f)))
        earlier-all (filter #(.isBefore ^Instant (:closed-at %) before) candidates)
        exclusions (mapv :exclusion (filter :non-construction? earlier-all))
        earlier (remove :non-construction? earlier-all)
        ;; Exclusion as unrelated requires current evidence, not an unverified
        ;; mission name that could conceal damaged matching history.
        _ (doseq [candidate earlier :when (not= target (:target candidate))]
            (validated-previous! candidate roots))
        ordered (sort-by :closed-at (filter #(= target (:target %)) earlier))
        _ (when (and (> (count ordered) 1)
                     (= (:closed-at (last ordered)) (:closed-at (last (butlast ordered)))))
            (need! false :ambiguous-previous-construction
                   {:files (mapv #(str (:close-file %)) (take-last 2 ordered))}))]
    (if-let [latest (last ordered)]
      (try (assoc-in (validated-previous! latest roots) [:provenance :excluded-attempts] exclusions)
           (catch clojure.lang.ExceptionInfo e
             (throw (ex-info (.getMessage e) (assoc (ex-data e) :history/excluded-attempts exclusions) e))))
      {:cascade policy/first-attempt-cascade :admitted {}
       :provenance {:status :none :reason :no-earlier-target-construction :searched-roots roots
                    :excluded-attempts exclusions}
       :admission-reason :first-attempt-no-admissions})))

(defn history-roots [identity]
  (vec (distinct (cons (:data-root identity)
                       (map #(.getCanonicalPath %)
                            (filter #(and (.isDirectory %) (str/starts-with? (.getName %) "wm-full-loop"))
                                    (or (.listFiles (io/file "/home/joe/code/futon2/data")) [])))))))

(defn construct
  [record read-bytes library-root previous designated]
  (let [ctx (finder/context record read-bytes library-root)
        found (finder/find record read-bytes library-root designated)
        selected (set (:selected found))
        r (:repository ctx)
        repository {:patterns (:patterns r) :stands-on (set (map (juxt :from :to) (:edges r)))}
        admitted (:admitted previous)
        carrier (set/union selected (set (keys admitted)))
        order (precedence carrier (:stands-on repository))
        interpretations (:interpretations ctx)
        q0 (into {} (map (juxt :id :value)) (:facts record))
        ;; Before arm: the previous attempt's recorded acting order (organise
        ;; calls this port with the very previous-cascade object it was given),
        ;; so this receipt need not re-interpret every earlier pattern. A first
        ;; attempt or legacy reset has none. After arm: simulated from q0.
        acting (fn [c]
                 (if (identical? c (:cascade previous))
                   (vec (:acting-order previous []))
                   (acting-order interpretations q0 (:precedence c))))
        diff (policy/organise (:cascade previous) selected repository admitted
                              {:temperament (assoc policy/up-closure-temperament :precedence order)
                               :acting-order-fn acting
                               :score-fn (constantly {:status :none :reason :cascade-g-not-computed})})
        _ (need! (= carrier (set/difference (:nodes diff) (:added-by-organise diff)))
                 :bootstrap-carrier-mismatch {})
        _ (policy/validate-cascade-diff! (:cascade previous) selected repository admitted diff)
        _ (validate-admissible! (:precedence-after diff) (:nodes diff) (:organised-edges diff))
        shown (:acting-order-after diff)
        _ (when (empty? shown)
            (throw (ex-info "Nothing fires in receipted construction"
                            {:interpretation/refusal :interpretation/no-relevant-pattern})))
        retained {:identity (:identity record) :previous (:provenance previous)
                  :admission-reason (:admission-reason previous)
                  :precedence-rule :authored-reachability-topological :tie-break :canonical-id
                  :acting-rule :first-true-unfired-guard-apply-effects-from-q0
                  :effect-authority :documented-interpretation-not-measured-success
                  :cascade-diff diff :cascade-diff-sha256 (evidence/value-digest diff)
                  :find-result found :find-result-sha256 (evidence/value-digest found)
                  :repository-sha256 (:digest r)}]
    {:shown (mapv #(subs (str %) 1) shown)
     :semilattice {:descent (mapv (fn [[a b]] [(subs (str a) 1) (subs (str b) 1)]) (sort (:organised-edges diff))) :co_app []}
     :construction-kind :receipted-pattern-cascade :selected-action (get-in record [:identity :occurrence :action/value])
     :receipted-construction retained}))

(defn construct! [record read-bytes opts]
  (construct record read-bytes (or (:interpretation-library-root opts) "/home/joe/code/futon3/library")
             (previous! (:identity record) (or (:interpretation-history-roots opts) (history-roots (:identity record))))
             nil))
