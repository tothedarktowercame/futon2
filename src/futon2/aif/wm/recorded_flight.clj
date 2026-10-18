(ns futon2.aif.wm.recorded-flight
  "Deterministic, click-free continuation from retained War Machine evidence.

  A recorded flight restores the selection checkpoint and recorded agent
  answers, then executes the first unreplayed boundary in isolated stores.
  It never invokes selection and never treats its result as a click or tick."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [futon2.aif.interpretation-evidence :as evidence]
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
        answer-path (verified-file! (:recorded-answer manifest) :recorded-answer)
        checkpoint (edn/read-string (slurp checkpoint-path))
        judgement (selection-judgement checkpoint)
        answer (json/parse-string (slurp answer-path) true)
        budget-before (when click-budget @click-budget)
        calls (atom [])
        answer-fn (fn [request]
                    (let [n (inc (count @calls))]
                      (swap! calls conj {:call n
                                         :kind (:kind request)
                                         :request-id (:request-id request)})
                      (if (= n 1)
                        {:analysis answer :job-id (:answer-job-id manifest)}
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
        frontier (cond
                   (and correction-called? (not (:published record)))
                   :corrected-answer-missing
                   (:published record) :selected-target-reading-published
                   :else :selected-target-reading-refused)
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
                            {:kind :agent-answer :path answer-path
                             :sha256 (get-in manifest [:recorded-answer :sha256])}]
                 :executed [:selected-target-reading-validation
                            :selected-target-reading-cascade-validation]
                 :frontier frontier
                 :boundary-record record}
        result (assoc result0 :result-digest (evidence/value-digest result0))
        path (io/file output-root "ledger" (str (:result-digest result) ".edn"))]
    (write-once! path result)
    (assoc result :ledger-path (.getPath path))))

(defn manifest
  [{:keys [source-run-id source-click-id author answer-job-id
           selection-checkpoint recorded-answer]}]
  (seal-manifest
   {:schema schema
    :source-run-id source-run-id
    :source-click-id source-click-id
    :author author
    :answer-job-id answer-job-id
    :selection-checkpoint {:path selection-checkpoint
                           :sha256 (file-sha256 selection-checkpoint)}
    :recorded-answer {:path recorded-answer
                      :sha256 (file-sha256 recorded-answer)}}))
