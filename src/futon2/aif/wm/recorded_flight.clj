(ns futon2.aif.wm.recorded-flight
  "Deterministic, click-free continuation from retained War Machine evidence.

  A recorded flight restores the selection checkpoint and recorded agent
  answers, then executes the first unreplayed boundary in isolated stores.
  It never invokes selection and never treats its result as a click or tick."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.occurrence-square-authoring :as square-authoring]
            [futon2.aif.selected-target-reading :as target-reading])
  (:import [java.time Instant]))

(def schema :wm/recorded-flight-manifest-v1)
(def result-schema :wm/recorded-flight-result-v1)

(defn file-sha256 [path]
  (evidence/sha256 (java.nio.file.Files/readAllBytes
                    (.toPath (io/file path)))))

(defn seal-manifest [manifest]
  (assoc manifest :manifest-digest
         (evidence/value-digest (dissoc manifest :manifest-digest))))

(defn manifest-valid? [manifest]
  (and (= schema (:schema manifest))
       (string? (:manifest-digest manifest))
       (= (:manifest-digest manifest)
          (evidence/value-digest (dissoc manifest :manifest-digest)))))

(defn- fail! [kind detail]
  (throw (ex-info "Recorded flight refused" (merge {:kind kind} detail))))

(defn- verified-file! [{:keys [path sha256] :as pin} label]
  (when-not (and (string? path) (.isFile (io/file path)))
    (fail! :recorded-flight-source-missing {:label label :pin pin}))
  (let [actual (file-sha256 path)]
    (when-not (= sha256 actual)
      (fail! :recorded-flight-source-drift
             {:label label :path path :expected sha256 :actual actual})))
  path)

(defn- selection-judgement [checkpoint]
  (let [payload (:payload checkpoint)
        judgement (:judgment payload)
        sorry (:sorry payload)]
    (when-not (and (map? judgement) (map? sorry) (map? (:decision sorry)))
      (fail! :recorded-flight-selection-checkpoint-invalid
             {:checkpoint-keys (keys checkpoint)
              :payload-keys (keys payload)}))
    ;; The durable cell separates the judgment carrier from its sorry arm.
    ;; The live selected-target boundary received both pieces in one value.
    (merge judgement sorry)))

(defn- write-once! [path value]
  (let [f (io/file path)
        bytes (.getBytes (pr-str value) "UTF-8")]
    (io/make-parents f)
    (if (.exists f)
      (when-not (= (seq bytes)
                   (seq (java.nio.file.Files/readAllBytes (.toPath f))))
        (fail! :recorded-flight-output-collision {:path (.getPath f)}))
      (java.nio.file.Files/write (.toPath f) bytes
                                 (into-array java.nio.file.OpenOption
                                             [java.nio.file.StandardOpenOption/CREATE_NEW
                                              java.nio.file.StandardOpenOption/WRITE])))
    (.getPath f)))

(defn- continue-inner-construction!
  [manifest checkpoint output-root reading-root analysis-digest]
  (let [receipt (get-in checkpoint [:payload :sorry :outer-task-selection])
        _ (when-not (map? receipt)
            (fail! :recorded-flight-outer-receipt-missing {}))
        restorations (atom 0)
        square-pin (:occurrence-square-answer manifest)
        square-answer (when square-pin
                        ;; The semantic maps are recorded; the analysis digest
                        ;; is a machine-computed binding. The Python validator
                        ;; stamps :created_at, so a fresh isolated validation
                        ;; has a fresh digest even for identical author bytes.
                        ;; Bind the maps to the analysis produced by THIS replay
                        ;; instead of requiring the author to transcribe it.
                        (assoc (json/parse-string
                                (slurp (verified-file!
                                        square-pin :occurrence-square-answer)) true)
                               :analysis_digest analysis-digest))
        square-root (str (io/file output-root "squares"))
        opts {:run-id (str "recorded-flight-inner-" (:source-run-id manifest))
              :target-reading-root reading-root
              :occurrence-square-root square-root
              :pattern-graph-path runtime/default-pattern-graph-path
              :pattern-graph-diff-dir (str (io/file output-root "graph-diffs"))
              ;; This is a receipt provider, not a selector: it can return only
              ;; the sealed source-run receipt and is called at most once by
              ;; production-defaults' pinning wrapper.
              :outer-task-selection-fn
              (fn [_] (swap! restorations inc) receipt)
              :require-occurrence-squares? true}
        defaults (runtime/production-defaults opts)
        judge #((:judge-fn defaults) 30)
        first-judgement (:judgement (judge))
        need (square-authoring/selected-need first-judgement)
        publication
        (when (and need square-answer)
          (square-authoring/repair!
           (assoc opts :author (:offline-repair-author manifest)
                  :answer-fn (fn [_]
                               (assoc square-answer :job-id
                                      (:occurrence-square-answer-job-id manifest))))
           first-judgement))
        final-judgement (if (:published publication)
                          (:judgement (judge))
                          first-judgement)
        action (get-in final-judgement [:decision :action])
        cascade-digest (:cascade-digest action)
        canonical
        (some (fn [x]
                ;; Some retained evidence maps use sorted comparators whose
                ;; key domain is strings; keyword lookup on those maps throws.
                ;; They cannot contain this keyword-keyed carrier, so skip
                ;; them rather than weakening their comparator.
                (when (map? x)
                  (try
                    (let [candidate (get x :canonical-cascade)]
                      (when (= cascade-digest (:digest candidate)) candidate))
                    (catch ClassCastException _ nil))))
              (tree-seq coll? seq final-judgement))
        open-holes (when canonical
                     (filterv #(= :open-hole (:enactment-status %))
                              (:executable-nodes canonical)))
        final-need (square-authoring/selected-need final-judgement)
        frontier (cond
                   (seq open-holes) :operational-authoring-missing
                   (and action cascade-digest (nil? canonical))
                   :canonical-cascade-not-retained
                   action :cascade-selected
                   (and need (nil? square-answer)) :occurrence-square-answer-missing
                   final-need :occurrence-square-publication-incomplete
                   :else :inner-construction-stopped)]
    (when-not (= 1 @restorations)
      (fail! :recorded-flight-outer-receipt-restoration-count
             {:expected 1 :actual @restorations}))
    {:outer-receipt-restorations @restorations
     :occurrence-square-need need
     :occurrence-square-publication publication
     :decision-status (get-in final-judgement [:decision :status])
     :refusals (mapv #(select-keys % [:target :stage :kind :failure-kind :failures])
                     (get-in final-judgement [:decision :refusals]))
     :action action
     :canonical-cascade canonical
     :open-hole-count (count open-holes)
     :frontier frontier}))

(defn run-recorded!
  "Run the selected-target-reading boundary from MANIFEST.

  OUTPUT-ROOT is isolated replay storage. CLICK-BUDGET may be an atom; when
  supplied it is snapshotted and must remain unchanged. No selector is
  accepted as an input: reaching for one is structurally impossible here."
  [{:keys [manifest output-root click-budget]}]
  (when-not (manifest-valid? manifest)
    (fail! :recorded-flight-manifest-invalid {}))
  (let [checkpoint-path (verified-file! (:selection-checkpoint manifest)
                                        :selection-checkpoint)
        answer-pins (or (:recorded-answers manifest)
                        [(:recorded-answer manifest)])
        answer-paths (mapv #(verified-file! % :recorded-answer) answer-pins)
        checkpoint (edn/read-string (slurp checkpoint-path))
        judgement (selection-judgement checkpoint)
        answers (mapv #(json/parse-string (slurp %) true) answer-paths)
        budget-before (when click-budget @click-budget)
        calls (atom [])
        answer-fn (fn [request]
                    (let [n (inc (count @calls))]
                      (swap! calls conj {:call n
                                         :kind (:kind request)
                                         :request-id (:request-id request)})
                      (if-let [answer (get answers (dec n))]
                        {:analysis answer
                         :job-id (or (get (:answer-job-ids manifest) (dec n))
                                     (:answer-job-id manifest))}
                        (throw (ex-info "No recorded answer for requested correction"
                                        {:kind :recorded-flight-unrecorded-agent-call
                                         :call n :request-kind (:kind request)})))))
        replay-id (str "recorded-flight-" (:source-run-id manifest))
        request-root (str (io/file output-root "requests"))
        reading-root (str (io/file output-root "readings"))
        started-at (str (Instant/now))
        record (target-reading/repair!
                {:run-id replay-id
                 :author (:author manifest)
                 :answer-fn answer-fn
                 :request-root request-root
                 :target-reading-root reading-root}
                judgement)
        budget-after (when click-budget @click-budget)
        _ (when-not (= budget-before budget-after)
            (fail! :recorded-flight-click-budget-changed
                   {:before budget-before :after budget-after}))
        correction-called? (some #(= :selected-target-reading-correction
                                     (:kind %)) @calls)
        reading-frontier (cond
                   (and correction-called? (not (:published record)))
                   :corrected-answer-missing
                   (:published record) :selected-target-reading-published
                   :else :selected-target-reading-refused)
        inner (when (:published record)
                (continue-inner-construction! manifest checkpoint output-root
                                              reading-root
                                              (:analysis-digest record)))
        frontier (or (:frontier inner) reading-frontier)
        result0 {:schema result-schema
                 :source-run-id (:source-run-id manifest)
                 :source-click-id (:source-click-id manifest)
                 :manifest-digest (:manifest-digest manifest)
                 :started-at started-at
                 :finished-at (str (Instant/now))
                 :clicks-spent 0
                 :ticks-carried 0
                 :selector-calls 0
                 :agent-calls @calls
                 :restored [{:kind :selection-checkpoint
                             :path checkpoint-path
                             :sha256 (get-in manifest [:selection-checkpoint :sha256])}
                            {:kind :agent-answer :path (first answer-paths)
                             :sha256 (:sha256 (first answer-pins))}]
                 :restored-answers
                 (mapv (fn [path pin]
                         {:kind :agent-answer :path path :sha256 (:sha256 pin)})
                       answer-paths answer-pins)
                 :executed [:selected-target-reading-validation
                            :selected-target-reading-cascade-validation]
                 :frontier frontier
                 :inner-continuation inner
                 :boundary-record record}
        result (assoc result0 :result-digest (evidence/value-digest result0))
        path (io/file output-root "ledger" (str (:result-digest result) ".edn"))]
    (write-once! path result)
    (assoc result :ledger-path (.getPath path))))

(defn manifest
  [{:keys [source-run-id source-click-id author answer-job-id
           answer-job-ids selection-checkpoint recorded-answer recorded-answers
           occurrence-square-answer occurrence-square-answer-job-id
           offline-repair-author]}]
  (seal-manifest
   {:schema schema
    :source-run-id source-run-id
    :source-click-id source-click-id
    :author author
    :answer-job-id answer-job-id
    :answer-job-ids answer-job-ids
    :offline-repair-author offline-repair-author
    :occurrence-square-answer-job-id occurrence-square-answer-job-id
    :selection-checkpoint {:path selection-checkpoint
                           :sha256 (file-sha256 selection-checkpoint)}
    :recorded-answer (when recorded-answer
                       {:path recorded-answer
                        :sha256 (file-sha256 recorded-answer)})
    :recorded-answers
    (when (seq recorded-answers)
      (mapv (fn [path] {:path path :sha256 (file-sha256 path)})
            recorded-answers))
    :occurrence-square-answer
    (when occurrence-square-answer
      {:path occurrence-square-answer
       :sha256 (file-sha256 occurrence-square-answer)})}))
