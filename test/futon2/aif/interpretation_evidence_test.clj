(ns futon2.aif.interpretation-evidence-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.close-retention :as retention]
            [futon2.aif.evidence-manifest :as manifest]
                        [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.full-loop-runner-test])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]
           [java.time Instant]
           [java.util UUID]))

(def at "2026-09-15T12:00:00Z")
(def redo "holes/labs/wm-contract/runs/F13-model-manifest-2026-09-15/redo/")
(defn bytes-of [s] (.getBytes (str s) "UTF-8"))
(defn refusal [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e
                 (or (:interpretation-evidence/refusal (ex-data e))
                     (:limb-evidence/refusal (ex-data e))))))

(defn zaif-fixture
  "Map the retained nine-fact/seven-interpretation agent artifact, not a token toy.
  This is test data conversion, not the production interpreter or a new live receipt."
  []
  (let [old (edn/read-string (slurp (str redo "interpreted-pattern-set.edn")))
        sources (atom {}) captured (atom {})
        add! (fn [path]
               (let [id path bs (Files/readAllBytes (.toPath (io/file path)))
                     file (str (evidence/sha256 (bytes-of path)) ".source")]
                 (swap! sources assoc id {:id id :path path :file file
                                         :sha256 (evidence/sha256 bs) :revision "retained-fixture-bytes"})
                 (swap! captured assoc file bs)
                 id))
        cite (fn [s] {:source (add! (:path s)) :lines (:lines s) :quote (:quote s)})
        mission-cite (cite (get-in old [:target :status-source]))
        clause (fn [path text]
                 (let [lines (vec (str/split-lines (slurp path)))
                       norm #(str/replace (str/trim %) #"\s+" " ")
                       span (first (for [n (range 1 (inc (count lines)))
                                         i (range (inc (- (count lines) n)))
                                         :when (str/includes? (norm (str/join " " (subvec lines i (+ i n))))
                                                              (norm text))] [i (+ i n)]))]
                   (assert span (str "Fixture clause missing: " path))
                   {:source (add! path) :lines [(inc (first span)) (second span)]
                    :quote (str/join "\n" (subvec lines (first span) (second span)))}))
        facts (mapv (fn [f] {:id (:id f) :meaning (:meaning f) :value (:q0 f)
                             :citations [(cite (:source f))] :observed-at at
                             :method :mission-document-assertion :scope "documented capability"}) (:facts old))
        guard (fn guard [g] (into [(keyword (first g))]
                                 (map #(if (vector? %) (guard %) %)) (rest g)))
        interpretations
        (mapv (fn [x]
                (let [path (get-in x [:source :path])
                      record {:pattern (:pattern x) :source (add! path)
                              :membership [(clause "/home/joe/code/futon3/resources/sigils/patterns-index.tsv" (:pattern x))]
                              :clauses (into {} (for [k [:if :however :then]]
                                                  [k (clause path (get-in x [:quotes (keyword (str/upper-case (name k)))]))]))
                              :guard (guard (:guard x)) :effect (:effect x)
                              :authority :documented-interpretation :author "codex-27"}]
                  (assoc record :sha256 (evidence/value-digest record)))) (:interpretations old))
        candidates (mapv (fn [i j]
                           (let [s (first (:sources j)) path (:path s)]
                             {:pattern (:candidate j) :source (if path (add! path) {:status :none :reason :pattern-source-missing}) :rank (inc i)
                              :judgment {:relevant? (:relevant j) :reason (:judgment j)
                                         :mission-citations [mission-cite]
                                         :pattern-citations (if path [(clause path (get-in s [:clauses :IF]))]
                                                               {:status :none :reason :pattern-source-missing})}}))
                         (range) (:candidate-judgments old))
        index-id (add! (.getCanonicalPath (io/file (str redo "retrieval-index.tsv"))))
        occurrence (retention/mint-occurrence
                    {:run-id (str (UUID/randomUUID)) :cohort-id ":fixture" :attempt-id "attempt-001"
                     :selected-action {:type :advance-mission :target "M-zaif-harness-v1"}
                     :now #(Instant/parse at) :uuid-fn #(UUID/randomUUID)})
        identity {:occurrence occurrence :semantic-epoch :fixture-v1 :data-root "/fixture"
                  :start-event-sha256 (evidence/sha256 (bytes-of "start"))
                  :interpreter-job "fixture-job" :author "codex-27" :schema-version 1}
        record {:schema :wm/interpreted-pattern-set-v1 :identity identity
                :sources (vec (vals @sources))
                :target {:id "M-zaif-harness-v1" :kind :mission :action (:action/value occurrence)
                         :source (:source mission-cite) :citations [mission-cite] :pinned-at at}
                :retrieval {:query "Retained zaif tension/relevance judgments (schema commissioning fixture)"
                            :citations [mission-cite]
                            :runs [{:retriever "retained-combined-candidate-judgments" :version "cc231bf6"
                                    :index-source index-id :parameters {:fixture true}
                                    :candidates candidates :failures []}]}
                :facts facts :interpretations interpretations
                :holes (mapv (fn [f] {:kind (keyword (:kind f)) :reason (:reason f)
                                      :citations [mission-cite]})
                             ;; C's historical missing finding was superseded by the owner;
                             ;; retain the interpretation holes, not that obsolete ruling.
                             (filter #(= :missing-pattern-interpretation (:kind %)) (:findings old)))
                :genesis (mapv (fn [g] {:pattern (:id g) :source (get-in g [:source :path])
                                        :index-source "/home/joe/code/futon3/resources/sigils/patterns-index.tsv"
                                        :commit "f49da8ee791b966a7bc077629337c4b68369436f"
                                        :gap (:gap g)}) (:new-patterns old))
                :failure nil}]
    {:record record :captured @captured}))

(def fixture (delay (zaif-fixture)))

(deftest zaif-shape-and-source-controls
  (let [{:keys [record captured]} @fixture]
    (is (= 9 (count (:facts record)))) (is (= 7 (count (:interpretations record))))
    (is (= 1 (count (:genesis record))))
    (is (seq (:holes record)))
    (is (= record (edn/read-string (pr-str (evidence/validate-sources! record captured)))))
    (doseq [[reason alter]
            [[:source-digest-mismatch #(assoc-in % [:sources 0 :sha256] (apply str (repeat 64 "0")))]
             [:undeclared-fact #(assoc-in % [:interpretations 0 :guard] [:fact "invented"])]
             [:undeclared-fact #(assoc-in % [:interpretations 0 :effect] {"invented" true})]
             [:citation-missing #(assoc-in % [:interpretations 0 :clauses :if] nil)]
             [:schema-mismatch #(assoc % :schema :unknown)]
             [:relevance-judgment-missing #(update-in % [:retrieval :runs 0 :candidates 0] dissoc :judgment)]]]
      (is (= reason (refusal #(evidence/validate-sources! (alter record) captured))) (str reason)))
    (is (= :attempt-identity-mismatch
           (refusal #(evidence/assert-same-attempt! (:identity record)
                       (assoc (:identity record) :semantic-epoch :fixture-v2)))))))

(defn admit [record captured failure?]
  (let [root (.toFile (Files/createTempDirectory "interpretation-admission" (make-array FileAttribute 0)))
        dir (io/file root "fixture" "attempt-001")
        evdir (io/file dir "evidence")
        start {:event/sequence 1 :checkpoint/type :time-step
               :attempt/ordinal 1 :attempt/id "attempt-001" :cohort/id :fixture}
        start-bytes (bytes-of (pr-str start))
        _ (.mkdirs evdir)
        _ (Files/write (.toPath (io/file dir "001-time-step.edn")) start-bytes
                       (make-array java.nio.file.OpenOption 0))
        identity (assoc (:identity record) :data-root (.getCanonicalPath root)
                        :start-event-sha256 (evidence/sha256 start-bytes))
        record (cond-> (assoc record :identity identity)
                 failure? (assoc :failure {:kind :interpretation/agent-unavailable :identity identity
                                           :stage :construction :source-refs [] :elapsed-ms 3
                                           :partial-artifacts []}))
        rb (bytes-of (pr-str record))
        ref {:file "interpretation.edn" :sha256 (evidence/sha256 rb)}
        observation (-> record
                        (dissoc :retrieval :interpretations :genesis)
                        (assoc :schema :wm/mission-fact-observation-v1 :phase :pre
                               :model-sha256 (evidence/value-digest (:interpretations record))
                               :measured-by "independent-fixture-reader" :interpretation-ref ref))
        all (cond-> (assoc captured "interpretation.edn" rb)
              (not failure?) (assoc "observation.edn" (bytes-of (pr-str observation))))
        selection {:event/sequence 2 :checkpoint/type :selection}
        construction {:event/sequence 3 :checkpoint/type :construction
                      :payload {:judgment {:interpretation-receipt (when-not failure? ref)}}}]
    (doseq [[file bs] all]
      (Files/write (.toPath (io/file evdir file)) bs (make-array java.nio.file.OpenOption 0)))
    (spit (io/file dir "002-selection.edn") (pr-str selection))
    (spit (io/file dir "003-construction.edn") (pr-str construction))
    (let [m (#'runner/checkpoint-evidence-manifest
             {:time-step start :selection selection :construction construction} (.getCanonicalPath root)
             :fixture "attempt-001" "M-zaif-harness-v1" identity)]
      {:manifest m :identity identity :record record})))

(deftest admission-through-existing-runner-manifest
  (let [{:keys [record captured]} @fixture
        {:keys [manifest]} (admit record captured false)]
    (is (= manifest (manifest/validate-manifest manifest)))
    (is (some #(str/ends-with? (:source-path %) "/interpretation.edn") (:entries manifest)))
    (is (some #(str/ends-with? (:source-path %) "/observation.edn") (:entries manifest)))))

(deftest failure-admission-and-retention-close
  (let [{:keys [record captured]} @fixture
        {:keys [manifest identity record]} (admit record captured true)
        block (retention/build-retention-block
               {:occurrence (:occurrence identity)
                :state {:status :absent :reason :independent-observation-unavailable}
                :model {:status :absent :reason :declared-model-identity-unthreaded}
                :closed-at "2026-09-15T13:00:00Z" :evidence-cutoff "2026-09-15T13:00:00Z"
                :admitted-evidence (mapv :evidence/id (:entries manifest))})]
    (is (= :interpretation/agent-unavailable (get-in record [:failure :kind])))
    (is (true? (manifest/verify-retention-agreement manifest block)))
    (is (= :absent (get-in block [:state :status])))))

(deftest typed-failure-closes-through-cohort-writer
  (let [{:keys [record captured]} @fixture
        {:keys [manifest identity]} (admit record captured true)
        root (get-in identity [:data-root])
        prereg (str root "/cohort.edn")
        _ (#'futon2.aif.full-loop-runner-test/tiny-target-prereg prereg)
        _ (spit prereg (pr-str (assoc (edn/read-string (slurp prereg)) :cohort/id :fixture)))
        _ (cohort/activate! prereg root)
        ;; Admission above retained real checkpoint files; fill the remaining
        ;; existing order with explicit non-execution, never invented success.
        cell {:judgment {:status :not-executed} :ground {:kind :test-fixture}}
        _ (doseq [k [:dispatch :build :adjudication]]
            (cohort/append-checkpoint! prereg root "attempt-001" k cell))
        closed (cohort/close-attempt!
                prereg root "attempt-001"
                {:judgment {:outcome :agent-unavailable :grounded? false :artifact-only? false
                            :duration-ms 3 :resource-use {:agent-turns 0}}
                 :ground {:kind :interpretation-failure}
                 :retention-inputs {:occurrence (:occurrence identity)
                                    :state {:status :absent :reason :independent-observation-unavailable}
                                    :model {:status :absent :reason :declared-model-identity-unthreaded}
                                    :admitted-evidence (mapv :evidence/id (:entries manifest))}
                 :evidence-manifest manifest})]
    (is (= :closed (:checkpoint/type closed)))
    (is (= manifest (get-in closed [:payload :close-evidence-manifest])))
    (is (= :absent (get-in closed [:payload :close-retention :state :status])))))

(deftest observation-cannot-cross-epoch-or-change-carrier
  (let [{:keys [record captured]} @fixture
        rb (bytes-of (pr-str record))
        ref {:file "interpretation.edn" :sha256 (evidence/sha256 rb)}
        observation (-> record (dissoc :retrieval :interpretations :genesis)
                        (assoc :schema :wm/mission-fact-observation-v1 :phase :end
                               :model-sha256 (evidence/value-digest (:interpretations record))
                               :measured-by "reviewer" :interpretation-ref ref))
        check (fn [obs]
                (evidence/validate-admission!
                 [record obs] (assoc captured "interpretation.edn" rb)
                 {"interpretation.edn" record} (:identity record) ref))]
    (is (true? (check observation)))
    (is (= :attempt-identity-mismatch
           (refusal #(check (assoc-in observation [:identity :semantic-epoch] :another-epoch)))))
    (is (= :fact-carrier-mismatch
           (refusal #(check (assoc-in observation [:facts 0 :meaning] "different proposition")))))
    (is (= :method-invalid
           (refusal #(check (assoc-in observation [:facts 0 :method] :close-disposition)))))
    (is (= :interpretation-reference-digest-mismatch
           (refusal #(check (assoc-in observation [:interpretation-ref :sha256]
                                     (apply str (repeat 64 "0")))))))
    (is (= :construction-receipt-missing
           (refusal #(evidence/validate-admission! [] {} {} (:identity record) ref))))))
