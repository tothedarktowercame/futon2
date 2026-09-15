(ns futon2.aif.fact-measurement
  "Frozen literal measurement plans and pre/end evidence, never disposition inference.
  Artifact measures a path in a commit-bound build artifact list, not live capability.
  Wiring measures the named field's shape, not a predicted effect.
  Request-status measures a recorded request/status, never its completion.
  Gate/replay readers require explicit revision-bound measurement receipts;
  ordinary author/reviewer approval is insufficient. Unqualified evidence is unknown."
  (:require [clojure.edn :as edn] [clojure.java.io :as io] [clojure.string :as str]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.find-receipt :as finder])
  (:import [java.nio.file Files StandardOpenOption]))

(def plan-file "interpretation-reader-plan.edn")
(def forbidden #{:outcome :grounded? :prior :selected-prior :controller-score :predicted-effects :effect :interpretations})
(def parameter-keys
  {:document-span #{:source :lines :predicate :literal}
   :artifact #{:repository :path} :wiring #{:field :expected-shape}
   :gate-receipt #{:gate-id :revision}
   :independent-replay #{:reviewer :revision :command} :request-status #{:record-id}})
(defn- need! [ok reason]
  (when-not ok (throw (ex-info "Measurement plan refused"
                              {:interpretation/refusal :interpretation/invalid-receipt :measurement/refusal reason}))))
(defn- text? [x] (and (string? x) (not (str/blank? x))))
(defn- read-one [bs]
  (with-open [r (java.io.PushbackReader. (io/reader (java.io.ByteArrayInputStream. bs)))]
    (let [eof (Object.) value (edn/read {:eof eof} r)]
      (need! (identical? eof (edn/read {:eof eof} r)) :multiple-forms) value)))
(defn- exact-citation! [record reader c]
  (need! (= #{:source :lines :quote} (set (keys c))) :citation-shape)
  (let [s (first (filter #(= (:id %) (:source c)) (:sources record)))
        [a b] (:lines c)
        ls (when s (str/split-lines (String. ^bytes (reader (:file s)) "UTF-8")))]
    (need! (and s (vector? (:lines c)) (= 2 (count (:lines c)))
                 (pos-int? a) (pos-int? b) (<= a b (count ls))
                 (= (:quote c) (str/join "\n" (subvec (vec ls) (dec a) b)))) :citation-mismatch)))
(defn validate-plan! [record reader & [build-author]]
  (let [s (first (filter #(= plan-file (:file %)) (:sources record)))
        _ (need! s :plan-not-in-sources)
        bs (reader plan-file)
        _ (need! (= (:sha256 s) (evidence/sha256 bs)) :plan-digest-mismatch)
        plan (read-one bs) ids (set (map :id (:facts record)))]
    (need! (vector? plan) :plan-not-vector)
    (need! (= (count plan) (count (set (map :fact plan)))) :duplicate-plan-fact)
    (doseq [{:keys [fact parameters citations] kind :reader :as entry} plan]
      (need! (= #{:fact :reader :parameters :citations} (set (keys entry))) :plan-entry-shape)
      (need! (contains? ids fact) :undeclared-fact)
      (need! (contains? parameter-keys kind) :unknown-reader)
      (need! (not-any? forbidden (tree-seq coll? seq parameters)) :forbidden-input)
      (need! (and (map? parameters) (= (get parameter-keys kind) (set (keys parameters)))) :unknown-parameters)
      (need! (and (vector? citations) (seq citations)) :citation-required)
      (doseq [c citations] (exact-citation! record reader c))
      (case kind
        :document-span
        (let [[a b] (:lines parameters)]
          (need! (and (= (:source parameters) (get-in record [:target :source]))
                       (vector? (:lines parameters)) (= 2 (count (:lines parameters)))
                       (pos-int? a) (pos-int? b) (<= a b)
                       (#{:contains :not-contains} (:predicate parameters)) (text? (:literal parameters))) :document-parameters))
        :wiring (need! (and (#{:wiring :fold-output :shape-validation :correspondence-validation} (:field parameters))
                            (#{:map :vector :valid} (:expected-shape parameters))) :wiring-parameters)
        :independent-replay (need! (and (every? text? (vals parameters))
                                        (not= (:reviewer parameters) (get-in record [:identity :author]))
                                        (not= (:reviewer parameters) build-author)) :replay-parameters)
        (need! (every? text? (vals parameters)) :text-parameters)))
    plan))

(def input-keys #{:source-bytes :artifact :wiring :receipts})
(defn- wiring-shape [v]
  (if (and (map? v) (= #{:map? :vector? :valid?} (set (keys v))))
    (into {} (map (fn [[k x]] [k (true? x)])) v)
    {:map? (map? v)
     :vector? (vector? v)
     :valid? (boolean (and (map? v)
                          (or (= :valid (:status v)) (true? (:valid? v)))))}))
(defn- receipt-input [r]
  (into {} (filter (fn [[k v]]
                     (cond
                       (= k :kind) (#{:gate-receipt :independent-replay :request-status} v)
                       (#{:executed? :passed? :recorded?} k) (boolean? v)
                       :else (text? v))))
        (select-keys r [:kind :gate-id :revision :reviewer :command :executed? :passed? :record-id :recorded?])))
(defn reader-input [raw]
  ;; Shape readers see only shape, never the fold's predicted effects.
  {:source-bytes (:source-bytes raw)
   :artifact (let [a (:artifact raw)]
               {:repository (when (text? (:repository a)) (:repository a))
                :commit (when (text? (:commit a)) (:commit a))
                :paths (vec (filter text? (:paths a)))})
   :wiring (into {} (map (fn [[k v]] [k (wiring-shape v)]))
                 (select-keys (:wiring raw) [:wiring :fold-output :shape-validation :correspondence-validation]))
   :receipts (mapv receipt-input (:receipts raw))})
(defn require-input! [input key]
  (need! (contains? input-keys key) :forbidden-input)
  (get input key))
(defn- agreeing-value [values]
  (let [values (set values)]
    (when (= 1 (count values)) (first values))))
(def readers
  {:document-span
   (fn [p input]
     (when-let [bs (get (require-input! input :source-bytes) (:source p))]
       (let [ls (vec (str/split-lines (String. ^bytes bs "UTF-8"))) [a b] (:lines p)]
         (when (<= a b (count ls))
           (let [present (str/includes? (str/join "\n" (subvec ls (dec a) b)) (:literal p))]
             (if (= :contains (:predicate p)) present (not present)))))))
   :artifact (fn [p input] (let [a (require-input! input :artifact)]
                            (when (and (= (:repository p) (:repository a)) (text? (:commit a))
                                       (some #{(:path p)} (:paths a))) true)))
   :wiring (fn [p input] (let [v (get (require-input! input :wiring) (:field p))]
                          (when (case (:expected-shape p) :map (:map? v) :vector (:vector? v)
                                      :valid (:valid? v)) true)))
   :gate-receipt
   (fn [p input]
     (agreeing-value
      (for [r (require-input! input :receipts)
            :when (and (= :gate-receipt (:kind r))
                       (= (:gate-id p) (:gate-id r)) (= (:revision p) (:revision r))
                       (boolean? (:passed? r)))]
        (:passed? r))))
   :independent-replay
   (fn [p input]
     (agreeing-value
      (for [r (require-input! input :receipts)
            :when (and (= :independent-replay (:kind r))
                       (= p (select-keys r [:reviewer :revision :command]))
                       (true? (:executed? r)) (boolean? (:passed? r)))]
        (:passed? r))))
   :request-status (fn [p input] (when (some #(and (= :request-status (:kind %))
                                                   (= (:record-id p) (:record-id %)) (true? (:recorded? %)))
                                             (require-input! input :receipts)) true))})
(defn measure [plan raw]
  (let [input (reader-input raw)]
    (into {} (for [{:keys [fact reader parameters]} plan]
               [fact (try (let [r ((get readers reader) parameters input)
                                r (if (vector? r) (first r) r)]
                            {:value (if (boolean? r) r :unknown)
                             :reason (if (boolean? r) :qualified-evidence :no-qualifying-evidence)})
                          (catch Exception _ {:value :unknown :reason :reader-failed}))]))))

(defn exposures [record order end-facts]
  (let [pre (into {} (map (juxt :id :value)) (:facts record)) end (into {} (map (juxt :id :value)) end-facts)
        xs (into {} (map (juxt #(keyword (:pattern %)) identity)) (:interpretations record))
        named (set (mapcat #(keys (:effect (get xs %))) order))]
    {:kind :observed-exposure-not-success-probability
     :moves (loop [ids order state pre out []]
              (if-let [id (first ids)]
                (let [x (get xs id)]
                  (recur (rest ids) (merge state (:effect x))
                         (conj out {:pattern id :interpretation-sha256 (:sha256 x)
                                    :guard-value (finder/guard-value state (:guard x))
                                    :effects (into {} (for [[fact _] (:effect x)]
                                                        [fact {:pre (get pre fact) :end (get end fact)
                                                               :status (cond (true? (get pre fact)) :already-true
                                                                             (or (= :unknown (get pre fact)) (= :unknown (get end fact))) :unknown
                                                                             (and (false? (get pre fact)) (true? (get end fact))) :newly-established
                                                                             (= (get pre fact) (get end fact)) :unchanged
                                                                             :else :changed)}]))}))) out))
     :unexplained (vec (sort (for [[id v] end :when (and (not (named id)) (not= :unknown v)
                                                        (not= :unknown (get pre id)) (not= v (get pre id)))] id)))}))

(defn- file-bytes [file] (Files/readAllBytes (.toPath (io/file file))))
(defn- source
  ([file] (source file (file-bytes file)))
  ([file bs]
   (let [f (io/file file)]
     {:id (.getCanonicalPath f) :path (.getCanonicalPath f) :file (.getName f)
      :sha256 (evidence/sha256 bs) :revision "captured-at-measurement"})))
(defn- write! [dir name value]
  (let [f (io/file dir name)]
    (Files/write (.toPath f) (.getBytes (pr-str value) "UTF-8")
                 (into-array StandardOpenOption [StandardOpenOption/CREATE_NEW StandardOpenOption/WRITE]))
    (source f)))
(defn- observation [record phase _at facts sources ref]
  {:schema :wm/mission-fact-observation-v1 :identity (:identity record) :target (:target record)
   :sources sources :facts facts :holes [] :failure nil
   :model-sha256 (evidence/value-digest (:interpretations record)) :phase phase
   :measured-by "futon2.aif.fact-measurement/v1" :interpretation-ref ref})
(defn begin! [dir construction now & [build-author]]
  (let [reader #(file-bytes (io/file dir %)) record (read-one (reader "interpretation-receipt.edn"))
        plan (validate-plan! record reader build-author) ref {:file "interpretation-receipt.edn" :sha256 (evidence/sha256 (reader "interpretation-receipt.edn"))}
        pre (observation record :pre (now) (:facts record) (:sources record) ref)]
    (evidence/validate-sources! pre reader)
    (write! dir "fact-pre.edn" pre)
    {:record record :plan plan :ref ref :dir dir :pre pre :construction construction :build-author build-author}))
(defn- retained-reader-receipts [dir]
  ;; Only explicit measurement receipts, not reviewer prose or disposition
  ;; records. Producer identity/revision/command qualification is in readers.
  ;; Preserve each admitted input's exact bytes in the observation sources.
  (vec
   (keep (fn [f]
           (when (and (.isFile f) (str/ends-with? (.getName f) ".source")
                      (<= (.length f) 1048576))
             (try
               (let [bs (file-bytes f) r (read-one bs)]
                 (when (and (map? r) (#{:gate-receipt :independent-replay :request-status} (:kind r)))
                   {:record r :source (source f bs)}))
               (catch Exception _ nil))))
         (sort-by #(.getName %) (.listFiles (io/file dir))))))

(defn finish! [state raw now]
  (let [{:keys [record plan ref dir construction build-author]} state
        at (now) reader #(file-bytes (io/file dir %))
        _ (validate-plan! record reader build-author)
        target (first (filter #(= (:id %) (get-in record [:target :source])) (:sources record)))
        retained (retained-reader-receipts dir)
        target-bytes (try (file-bytes (:path target)) (catch Exception _ nil))
        input (reader-input (assoc raw :source-bytes (if target-bytes {(:id target) target-bytes} {})
                                      :receipts (into (vec (:receipts raw)) (map :record retained))))
        results (merge (into {} (map (fn [f] [(:id f) {:value :unknown :reason :unplanned}])) (:facts record))
                       (measure plan input))
        ;; Diagnostic evidence, not a disposition or an interpretation effect.
        proof (write! dir "fact-end-measurements.source" {:observed-at at :results results
                                                         :artifact (:artifact input) :wiring (:wiring input) :receipts (:receipts input)
                                                         :source-snapshots (if target-bytes
                                                                             {(:id target) {:file "fact-end-target.source"
                                                                                            :sha256 (evidence/sha256 target-bytes)}} {})})
        target-source (when target-bytes
                        (let [f (io/file dir "fact-end-target.source")]
                          (Files/write (.toPath f) target-bytes
                                       (into-array StandardOpenOption [StandardOpenOption/CREATE_NEW StandardOpenOption/WRITE]))
                          (source f)))
        quote (slurp (io/file dir (:file proof)))
        facts (mapv #(assoc % :value (get-in results [(:id %) :value] :unknown) :observed-at at
                            :method (if (boolean? (get-in results [(:id %) :value])) :source-span-observation :unavailable-observation)
                            :citations [{:source (:id proof) :lines [1 1] :quote quote}]) (:facts record))
        exposure (write! dir "fact-exposures.source"
                         (exposures record (get-in construction [:receipted-construction :cascade-diff :acting-order-after]) facts))
        end (observation record :end at facts (vec (vals (into (array-map) (map (juxt :file identity))
                                         (concat (map :source retained) (:sources record)
                                                 (remove nil? [proof target-source exposure]))))) ref)]
    (evidence/validate-sources! end reader)
    (write! dir "fact-end.edn" end)
    end))
