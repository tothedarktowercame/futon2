(ns futon2.aif.interpretation-evidence
  "Closed interpretation/observation envelopes admitted by the existing attempt manifest.
  Validation establishes source correspondence, not semantic truth or model adequacy."
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [futon2.aif.close-retention :as retention])
  (:import [java.security MessageDigest]
           [java.time Instant]
           [java.util UUID]))

(def schemas #{:wm/interpreted-pattern-set-v1 :wm/mission-fact-observation-v1})
(def failure-kinds
  #{:interpretation/agent-unavailable :interpretation/no-relevant-pattern
    :interpretation/genesis-required :interpretation/budget-exceeded
    :interpretation/source-changed :interpretation/invalid-receipt
    :interpretation/unmeasurable-fact})

(defn- refuse! [reason path]
  (throw (ex-info "Interpretation evidence refused"
                  {:interpretation-evidence/refusal reason :path path})))
(defn- require! [ok reason path] (when-not ok (refuse! reason path)))
(defn- shape! [x ks path]
  (require! (and (map? x) (= ks (set (keys x)))) :shape-invalid path))
(defn- text! [x path]
  (require! (and (string? x) (not (str/blank? x))) :text-invalid path))
(defn- instant! [x path]
  (text! x path)
  (try (Instant/parse x) (catch Exception _ (refuse! :timestamp-invalid path))))
(defn- digest! [x path]
  (require! (and (string? x) (re-matches #"[0-9a-f]{64}" x)) :digest-invalid path))
(defn- vec! [x path] (require! (vector? x) :vector-required path))
(defn- file! [x path]
  (text! x path)
  (require! (and (not (#{"." ".."} x)) (not (re-find #"[/\\]" x)))
            :companion-name-invalid path))
(defn sha256 [bytes]
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (MessageDigest/getInstance "SHA-256") bytes))))
(defn- stable [x]
  (cond (map? x) (into (sorted-map-by #(compare (pr-str %1) (pr-str %2)))
                      (map (fn [[k v]] [k (stable v)])) x)
        (vector? x) (mapv stable x)
        :else x))
(defn value-digest [x] (sha256 (.getBytes (pr-str (stable x)) "UTF-8")))

(defn join-key
  "Full occurrence plus epoch/source context. Never join cohort/attempt names alone."
  [identity]
  (select-keys identity [:occurrence :semantic-epoch :data-root :start-event-sha256]))
(defn assert-same-attempt! [a b]
  (require! (= (join-key a) (join-key b)) :attempt-identity-mismatch [:identity])
  true)
(defn- identity! [x]
  (shape! x #{:occurrence :semantic-epoch :data-root :start-event-sha256
              :interpreter-job :author :schema-version} [:identity])
  (retention/validate-occurrence (:occurrence x))
  (try (UUID/fromString (get-in x [:occurrence :run/id]))
       (catch Exception _ (refuse! :run-uuid-invalid [:identity :occurrence :run/id])))
  (require! (= 1 (:schema-version x)) :version-invalid [:identity])
  (require! (keyword? (:semantic-epoch x)) :epoch-invalid [:identity])
  (doseq [k [:data-root :author]] (text! (get x k) [:identity k]))
  (require! (.isAbsolute (io/file (:data-root x))) :path-not-absolute [:identity])
  (digest! (:start-event-sha256 x) [:identity :start-event-sha256])
  (if (map? (:interpreter-job x))
    (do (shape! (:interpreter-job x) #{:status :reason} [:identity :interpreter-job])
        (require! (= {:status :none :reason :not-dispatched} (:interpreter-job x))
                  :job-invalid [:identity]))
    (text! (:interpreter-job x) [:identity :interpreter-job])))

(defn- sources! [sources]
  (vec! sources [:sources])
  (doseq [s sources]
    (shape! s #{:id :path :file :sha256 :revision} [:sources])
    (doseq [k [:id :path :revision]] (text! (get s k) [:sources k]))
    (require! (.isAbsolute (io/file (:path s))) :path-not-absolute [:sources :path])
    (file! (:file s) [:sources :file]) (digest! (:sha256 s) [:sources :sha256]))
  (doseq [k [:id :file]]
    (require! (= (count sources) (count (set (map k sources))))
              :duplicate-source [:sources k])))
(defn- citation! [c source-ids]
  (shape! c #{:source :lines :quote} [:citation])
  (require! (contains? source-ids (:source c)) :source-unknown [:citation :source])
  (text! (:quote c) [:citation :quote])
  (let [ls (:lines c)]
    (require! (and (vector? ls) (= 2 (count ls))
                   (every? pos-int? ls) (<= (first ls) (second ls)))
              :span-invalid [:citation :lines])))
(defn- citations! [cs ids]
  (require! (and (vector? cs) (seq cs)) :citation-missing [:citations])
  (doseq [c cs] (citation! c ids)))
(defn- facts! [facts ids]
  (vec! facts [:facts])
  (require! (= (count facts) (count (set (map :id facts)))) :duplicate-fact [:facts])
  (doseq [f facts]
    (shape! f #{:id :meaning :citations :value :observed-at :method :scope} [:facts])
    (doseq [k [:id :meaning :scope]] (text! (get f k) [:facts k]))
    (require! (contains? #{true false :unknown} (:value f)) :fact-value-invalid [:facts])
    (require! (contains? #{:mission-document-assertion :source-span-observation
                           :command-observation :independent-replay :authoritative-readback
                           :unavailable-observation} (:method f)) :method-invalid [:facts])
    (instant! (:observed-at f) [:facts :observed-at])
    (citations! (:citations f) ids)))
(defn- guard! [g ids]
  (vec! g [:guard])
  (case (first g)
    :fact (do (require! (= 2 (count g)) :guard-invalid [:guard])
              (require! (contains? ids (second g)) :undeclared-fact [:guard]))
    :not (do (require! (= 2 (count g)) :guard-invalid [:guard]) (guard! (second g) ids))
    (:and :or) (do (require! (> (count g) 1) :guard-invalid [:guard])
                  (doseq [child (rest g)] (guard! child ids)))
    (refuse! :guard-invalid [:guard])))

(defn- target! [target identity sources]
  (shape! target #{:id :kind :action :source :citations :pinned-at} [:target])
  (text! (:id target) [:target :id])
  (require! (#{:mission :ticket} (:kind target)) :target-kind-invalid [:target])
  (require! (= (:action target) (get-in identity [:occurrence :action/value]))
            :target-action-mismatch [:target])
  (require! (= (:id target) (get-in target [:action :target])) :target-id-mismatch [:target])
  (require! (contains? sources (:source target)) :source-unknown [:target])
  (citations! (:citations target) sources)
  (require! (every? #(= (:source target) (:source %)) (:citations target))
            :target-source-mismatch [:target])
  (instant! (:pinned-at target) [:target :pinned-at]))
(defn- retrieval! [r ids target-source]
  (shape! r #{:query :citations :runs} [:retrieval])
  (text! (:query r) [:retrieval :query]) (citations! (:citations r) ids)
  (require! (every? #(= target-source (:source %)) (:citations r))
            :target-source-mismatch [:retrieval :citations])
  (vec! (:runs r) [:retrieval :runs])
  (doseq [run (:runs r)]
    (shape! run #{:retriever :version :index-source :parameters :candidates :failures} [:retrieval :runs])
    (doseq [k [:retriever :version]] (text! (get run k) [:retrieval k]))
    (require! (contains? ids (:index-source run)) :source-unknown [:retrieval :index-source])
    (require! (map? (:parameters run)) :parameters-invalid [:retrieval])
    (vec! (:failures run) [:retrieval :failures])
    (doseq [f (:failures run)]
      (shape! f #{:kind :reason} [:retrieval :failures])
      (require! (keyword? (:kind f)) :failure-kind-invalid [:retrieval])
      (text! (:reason f) [:retrieval :failures]))
    (vec! (:candidates run) [:retrieval :candidates])
    (doseq [[i c] (map-indexed vector (:candidates run))]
      (require! (and (map? c) (contains? c :judgment)) :relevance-judgment-missing [:retrieval :candidates i])
      (shape! c #{:pattern :source :rank :judgment} [:candidate])
      (text! (:pattern c) [:candidate :pattern])
      (require! (= (inc i) (:rank c)) :candidate-order-invalid [:candidate])
      (require! (or (contains? ids (:source c))
                     (= {:status :none :reason :pattern-source-missing} (:source c)))
                :source-unknown [:candidate])
      (let [j (:judgment c)]
        (shape! j #{:relevant? :reason :mission-citations :pattern-citations} [:judgment])
        (require! (boolean? (:relevant? j)) :relevance-judgment-missing [:judgment])
        (text! (:reason j) [:judgment])
        (citations! (:mission-citations j) ids)
        (require! (every? #(= target-source (:source %)) (:mission-citations j))
                  :target-source-mismatch [:judgment :mission-citations])
        (if (map? (:source c))
          (require! (and (false? (:relevant? j))
                         (= (:source c) (:pattern-citations j)))
                    :missing-pattern-citation-invalid [:judgment])
          (do (citations! (:pattern-citations j) ids)
              (require! (every? #(= (:source c) (:source %)) (:pattern-citations j))
                        :candidate-citation-mismatch [:judgment])))))))
(defn- interpretations! [xs facts sources author]
  (vec! xs [:interpretations])
  (require! (= (count xs) (count (set (map :pattern xs)))) :duplicate-pattern [:interpretations])
  (doseq [x xs]
    (shape! x #{:pattern :source :membership :clauses :guard :effect :authority :author :sha256}
            [:interpretations])
    (text! (:pattern x) [:interpretation :pattern])
    (require! (contains? sources (:source x)) :source-unknown [:interpretation])
    (citations! (:membership x) sources)
    (require! (every? #(and (not= (:source x) (:source %))
                            (str/includes? (:quote %) (:pattern x))) (:membership x))
              :repository-membership-invalid [:membership])
    (shape! (:clauses x) #{:if :however :then} [:clauses])
    (doseq [k [:if :however :then]]
      (require! (some? (get-in x [:clauses k])) :citation-missing [:clauses k])
      (citation! (get-in x [:clauses k]) sources)
      (require! (= (:source x) (get-in x [:clauses k :source])) :clause-source-mismatch [:clauses k]))
    (guard! (:guard x) facts)
    (require! (and (map? (:effect x)) (seq (:effect x))) :effect-invalid [:effect])
    (doseq [[id v] (:effect x)]
      (require! (contains? facts id) :undeclared-fact [:effect id])
      (require! (boolean? v) :effect-invalid [:effect id]))
    (require! (= :documented-interpretation (:authority x)) :authority-invalid [:interpretation])
    (require! (= author (:author x)) :author-mismatch [:interpretation])
    (digest! (:sha256 x) [:interpretation :sha256])
    (require! (= (:sha256 x) (value-digest (dissoc x :sha256))) :interpretation-digest-mismatch [:interpretation])))

(defn validate-record [r]
  (require! (contains? schemas (:schema r)) :schema-mismatch [:schema])
  (let [base #{:schema :identity :sources :target :facts :holes :failure}
        interpretation? (= :wm/interpreted-pattern-set-v1 (:schema r))]
    (shape! r (into base (if interpretation? #{:retrieval :interpretations :genesis}
                            #{:model-sha256 :phase :measured-by :interpretation-ref})) [:record])
    (identity! (:identity r)) (sources! (:sources r))
    (let [ids (set (map :id (:sources r)))]
      (target! (:target r) (:identity r) ids) (facts! (:facts r) ids)
      (vec! (:holes r) [:holes])
      (doseq [h (:holes r)]
        (shape! h #{:kind :reason :citations} [:holes])
        (require! (keyword? (:kind h)) :hole-kind-invalid [:holes])
        (text! (:reason h) [:holes]) (citations! (:citations h) ids))
      (require! (or (nil? (:failure r)) (map? (:failure r))) :failure-invalid [:failure])
      (when (map? (get-in r [:identity :interpreter-job]))
        (require! (some? (:failure r)) :undispatched-success [:identity :interpreter-job]))
      (when-let [f (:failure r)]
        (shape! f #{:kind :identity :stage :source-refs :elapsed-ms :partial-artifacts} [:failure])
        (require! (contains? failure-kinds (:kind f)) :failure-kind-invalid [:failure])
        (require! (= (:identity r) (:identity f)) :attempt-identity-mismatch [:failure])
        (require! (keyword? (:stage f)) :stage-invalid [:failure])
        (require! (nat-int? (:elapsed-ms f)) :elapsed-invalid [:failure])
        (doseq [k [:source-refs :partial-artifacts]]
          (vec! (get f k) [:failure k])
          (doseq [id (get f k)] (require! (contains? ids id) :source-unknown [:failure k]))))
      (if interpretation?
        (do (retrieval! (:retrieval r) ids (get-in r [:target :source]))
            (interpretations! (:interpretations r) (set (map :id (:facts r))) ids (get-in r [:identity :author]))
            (vec! (:genesis r) [:genesis])
            (doseq [g (:genesis r)]
              (shape! g #{:pattern :source :index-source :commit :gap} [:genesis])
              (doseq [k [:pattern :commit :gap]] (text! (get g k) [:genesis k]))
              (doseq [k [:source :index-source]]
                (require! (contains? ids (get g k)) :source-unknown [:genesis k]))))
        (do (digest! (:model-sha256 r) [:model-sha256])
            (require! (#{:pre :end} (:phase r)) :phase-invalid [:phase])
            (text! (:measured-by r) [:measured-by])
            (shape! (:interpretation-ref r) #{:file :sha256} [:interpretation-ref])
            (file! (get-in r [:interpretation-ref :file]) [:interpretation-ref])
            (digest! (get-in r [:interpretation-ref :sha256]) [:interpretation-ref])))))
  r)

(defn companion-files [r] (mapv :file (:sources r)))
(defn validate-sources!
  "READ-BYTES resolves only captured same-attempt companion names, never live source paths."
  [r read-bytes]
  (validate-record r)
  (let [sources (into {} (map (juxt :id identity)) (:sources r))]
    (doseq [s (:sources r)]
      (let [bs (read-bytes (:file s))]
        (require! (bytes? bs) :source-unavailable [:sources (:id s)])
        (require! (= (:sha256 s) (sha256 bs)) :source-digest-mismatch [:sources (:id s)])))
    (doseq [c (filter #(and (map? %) (= #{:source :lines :quote} (set (keys %))))
                     (tree-seq coll? seq r))]
      (let [s (get sources (:source c))
            text (String. ^bytes (read-bytes (:file s)) "UTF-8")
            lines (str/split-lines text)
            [a b] (:lines c)]
        (require! (<= b (count lines)) :span-invalid [:citation])
        (require! (= (:quote c) (str/join "\n" (subvec (vec lines) (dec a) b)))
                  :citation-text-mismatch [:citation])))
    r))

(defn validate-admission!
  "Bind records to the runner occurrence and cross-check observation/receipt bytes."
  [records captured by-file context construction-ref]
  (when construction-ref
    (shape! construction-ref #{:file :sha256} [:construction :interpretation-receipt])
    (file! (:file construction-ref) [:construction])
    (digest! (:sha256 construction-ref) [:construction])
    (require! (= :wm/interpreted-pattern-set-v1
                 (:schema (get by-file (:file construction-ref))))
              :construction-receipt-missing [:construction])
    (require! (= (:sha256 construction-ref)
                 (some-> (get captured (:file construction-ref)) sha256))
              :construction-receipt-digest-mismatch [:construction]))
  (doseq [r records]
    (validate-sources! r captured)
    (doseq [at (conj (mapv :observed-at (:facts r)) (get-in r [:target :pinned-at]))]
      (require! (not (.isAfter (Instant/parse at) (Instant/now)))
                :observation-after-admission [:observed-at]))
    (assert-same-attempt! (:identity r) context)
    (when (= :wm/mission-fact-observation-v1 (:schema r))
      (let [{:keys [file sha256]} (:interpretation-ref r)
            interpretation (get by-file file)]
        (require! (= :wm/interpreted-pattern-set-v1 (:schema interpretation))
                  :interpretation-reference-missing [:interpretation-ref])
        (require! (= sha256 (some-> (get captured file) futon2.aif.interpretation-evidence/sha256))
                  :interpretation-reference-digest-mismatch [:interpretation-ref])
        (assert-same-attempt! (:identity r) (:identity interpretation))
        (require! (= (:target r) (:target interpretation)) :target-mismatch [:target])
        (require! (= (:model-sha256 r) (value-digest (:interpretations interpretation)))
                  :model-mismatch [:model-sha256])
        (require! (= (mapv #(select-keys % [:id :meaning :scope]) (:facts r))
                     (mapv #(select-keys % [:id :meaning :scope]) (:facts interpretation)))
                  :fact-carrier-mismatch [:facts]))))
  (let [successful (filter #(and (= :wm/interpreted-pattern-set-v1 (:schema %))
                                 (nil? (:failure %))) records)]
    (when (seq successful)
      (require! (= 1 (count successful)) :multiple-construction-receipts [:construction])
      (shape! construction-ref #{:file :sha256} [:construction :interpretation-receipt])
      (file! (:file construction-ref) [:construction])
      (require! (= (first successful) (get by-file (:file construction-ref)))
                :construction-receipt-mismatch [:construction])
      (require! (= (:sha256 construction-ref)
                   (some-> (get captured (:file construction-ref)) sha256))
                :construction-receipt-digest-mismatch [:construction])))
  true)

(defn validate-identity
  "Validate the shared identity before an interpretation job has been dispatched."
  [identity]
  (identity! identity)
  identity)
