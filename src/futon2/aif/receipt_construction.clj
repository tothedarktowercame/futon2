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

(defn- same-target-close [identity close-file]
  (let [closed (read-one close-file)
        construction-file (io/file (.getParentFile (io/file close-file)) "003-construction.edn")
        construction (when (.exists construction-file) (read-one construction-file))
        target (get-in identity [:occurrence :action/value :target])
        occurrence (get-in closed [:payload :close-retention :occurrence])
        named-target (get-in construction [:payload :judgment :mission])]
    (when (and (contains? (get-in construction [:payload :judgment]) :cascade)
               (= target (or (get-in occurrence [:action/value :target]) named-target))
               (.isBefore (Instant/parse (:recorded-at closed))
                          (Instant/parse (get-in identity [:occurrence :action-at]))))
      {:closed closed :construction construction :close-file close-file :construction-file construction-file
       :occurrence occurrence
       ;; A construction written before receipt mode (the old cascade-lane
       ;; constructor) carries no ruled CascadeDiff.
       :legacy? (not (contains? (get-in construction [:payload :judgment]) :receipted-construction))})))

(defn previous!
  "Most recent closed construction on this target across explicit physical roots.
  A legacy predecessor (the old constructor, no pinned CascadeDiff) cannot supply
  a ruled cascade, so the first receipted construction on that target starts from
  first-attempt-cascade and records the legacy files it supersedes. A receipted
  predecessor that fails validation still refuses."
  [identity roots]
  (let [roots (vec (distinct (map #(.getCanonicalPath (io/file %)) roots)))
        candidates (for [root roots
                         cohort (or (.listFiles (io/file root)) []) :when (.isDirectory cohort)
                         attempt (or (.listFiles cohort) []) :when (.isDirectory attempt)
                         :let [f (io/file attempt "007-closed.edn")]
                         :when (.isFile f)
                         :let [x (same-target-close identity f)] :when x] x)
        ordered (sort-by #(get-in % [:closed :recorded-at]) candidates)
        _ (when (and (> (count ordered) 1)
                     (= (get-in (last ordered) [:closed :recorded-at])
                        (get-in (last (butlast ordered)) [:closed :recorded-at])))
            (need! false :ambiguous-previous-construction {}))
        {:keys [closed construction close-file construction-file] :as latest} (last ordered)]
    (cond
      (not latest)
      {:cascade policy/first-attempt-cascade :admitted {}
       :provenance {:status :none :reason :no-earlier-target-construction :searched-roots (vec roots)}
       :admission-reason :first-attempt-no-admissions}

      ;; The latest earlier construction predates receipt mode, so it cannot
      ;; supply a ruled cascade (precedence, acting order, attributed
      ;; admissions). Declare the reset and record what it supersedes.
      (:legacy? latest)
      {:cascade policy/first-attempt-cascade :admitted {}
       :provenance {:status :legacy-predecessor-unrepresentable
                    :searched-roots (vec roots)
                    :recorded-at (:recorded-at closed)
                    :construction-file (.getCanonicalPath (io/file construction-file))
                    :construction-sha256 (evidence/sha256 (bytes construction-file))
                    :close-file (.getCanonicalPath (io/file close-file))
                    :close-sha256 (evidence/sha256 (bytes close-file))}
       :admission-reason :legacy-predecessor-no-ruled-admissions}

      :else
      (let [_ (need! (:occurrence latest) :previous-occurrence-unavailable {:file (str close-file)})
            block (get-in closed [:payload :close-retention])
            m (get-in closed [:payload :close-evidence-manifest])
            retained (get-in construction [:payload :judgment :receipted-construction])
            prior-id (:identity retained)
            start-file (io/file (.getParentFile (io/file close-file)) "001-time-step.edn")
            start (read-one start-file)
            root (.getCanonicalPath (.getParentFile (.getParentFile (.getParentFile (io/file close-file)))))
            d (:cascade-diff retained)]
        (retention/validate-retention-block block)
        (manifest/validate-manifest m)
        (manifest/verify-retention-agreement m block)
        (doseq [file [construction-file start-file]]
          (let [entry (first (filter #(= (.getCanonicalPath (io/file (:source-path %))) (.getCanonicalPath file)) (:entries m)))]
            (need! (and entry (= (:sha256 entry) (evidence/sha256 (bytes file))))
                   :previous-manifest-source-mismatch {:file (str file)})))
        (need! retained :previous-cascade-carrier-unavailable {:file (str construction-file)})
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
                     (map? (get-in d [:provenance :admissions]))
                     (= (:admitted-by d) (set (keys (get-in d [:provenance :admissions])))))
               :previous-admission-carrier-invalid {})
        {:cascade {:nodes (:nodes d) :edges (:organised-edges d) :precedence (:precedence-after d)}
         :admitted (get-in d [:provenance :admissions])
         :admission-reason :carried-from-previous-occurrence
         :provenance {:identity prior-id :searched-roots roots :construction-file (.getCanonicalPath construction-file)
                      :construction-sha256 (evidence/sha256 (bytes construction-file))
                      :close-file (.getCanonicalPath (io/file close-file))
                      :close-sha256 (evidence/sha256 (bytes close-file))}}))))

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
        acting (fn [c] (acting-order interpretations q0 (:precedence c)))
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
