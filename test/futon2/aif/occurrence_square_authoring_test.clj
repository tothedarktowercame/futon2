(ns futon2.aif.occurrence-square-authoring-test
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.occurrence-square-authoring :as sut]
            [futon2.aif.occurrence-square-registry :as squares]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.target-reading-registry :as readings]
            [futon2.aif.three-halves-square :as square]))

(def digest-a (apply str (repeat 64 "a")))
(def pattern "ukrns/publication-cadence")
(def pattern-sha "24a8a429148b1c6e2b41e482ee539c00285154957022f18b3dab181bbdf4fe12")

(def analysis
  {:status "analyzed"
   :sentences [{:id "s1"
                :fragments [{:start 0 :end 4 :text "work" :intent "act"
                             :target "mission" :rationale "needed"
                             :relations ["action"]
                             :pattern_refs [{:status "candidate" :id pattern
                                             :rationale "fits"
                                             :source_sha256 pattern-sha}]
                             :pattern_rejections []}]}]})

(defn temp-root []
  (.toFile (java.nio.file.Files/createTempDirectory
            "occ-square-authoring-"
            (make-array java.nio.file.attribute.FileAttribute 0))))

(defn reading! [root]
  (readings/publish!
   root {:target-id "M-live" :source-path "/tmp/M-live.md"
         :excerpt-digest digest-a :source-file-digest digest-a
         :request {:task {:target_id "M-live" :excerpt_sha256 digest-a
                          :content_sha256 digest-a}}
         :analysis analysis :validator-version 1}))

(defn judgement [reading]
  {:decision
   {:selection-certificate
    {:target-policy-field
     {:families [{:target-id "M-live" :policies []
                  :failures [{:kind :occurrence-squares-not-authored
                              :excerpt-digest digest-a
                              :analysis-digest (:analysis-digest reading)}]}]}}}})

(def answer
  {:schema "wm/occurrence-square-maps-v1"
   :target "M-live"
   :entries
   [{:occurrence_id [pattern 0]
     :maps {:I1 {:element "living rendering" :source "target reading fragment"}
            :I2 {:element "frozen snapshot" :source "target reading fragment"}
            :G {:element "publication address" :source "pattern BECAUSE"}
     :B {:element "both discoverable" :source "pattern CHECK"}}}]})

(deftest prompt-uses-one-snake-case-wire-schema
  (let [wire (sut/prompt {:target "M-live" :analysis-digest digest-a
                          :occurrences [{:occurrence-id [pattern 0]
                                         :pattern pattern
                                         :pattern-sha256 pattern-sha
                                         :fragment {:text "work"}}]})]
    (is (re-find #"occurrence_id" wire))
    (is (not (re-find #"occurrence-id" wire)))
    (is (re-find #"Copy target and analysis_digest exactly" wire))))

(deftest correction-round-prompt-names-the-parser-error
  ;; Held run 2026-11-06-73839343 (repair wm-click-loop-repair-fc798874): both
  ;; author answers missed exactly one closing brace before the final ']}'.
  ;; The correction round re-sent a byte-identical prompt — the parser error
  ;; never reached wm-author, so the correction round was a blind retry. The
  ;; prompt must surface it, as the selected-target-reading seam does.
  (let [base {:target "M-live" :analysis-digest digest-a
              :occurrences [{:occurrence-id [pattern 0]
                             :pattern pattern
                             :pattern-sha256 pattern-sha
                             :fragment {:text "work"}}]}
        corrected (sut/prompt (assoc base
                                     :format-correction
                                     {:round 1 :limit 1
                                      :parser-error
                                      "Unexpected close marker ']'"}))
        plain (sut/prompt base)]
    (is (re-find #"FORMAT CORRECTION \(one round only\)" corrected))
    (is (re-find #"Unexpected close marker '\]'" corrected))
    (is (not (re-find #"missing closing brace" corrected))
        "the prompt names only the parser error it was given")
    (is (not (re-find #"FORMAT CORRECTION" plain))
        "the plain prompt gains no correction text")))

(deftest publishes-machine-built-square-before-g
  (let [reading-root (temp-root) square-root (temp-root)
        reading (reading! reading-root)
        result (sut/repair!
                {:author "wm-author" :target-reading-root reading-root
                 :occurrence-square-root square-root
                 :answer-fn (fn [_] (assoc answer :analysis_digest
                                           (:analysis-digest reading)))}
                (judgement reading))
        record (squares/current square-root "M-live" (:analysis-digest reading)
                                (squares/library-resolver))
        built (get (squares/squares record) [pattern 0])]
    (is (= :published (:status result)))
    (is (= :valid (:status (square/validate built))))
    (is (= :valid (:status (square/parts-check
                            built ((squares/library-resolver) pattern)))))))

(deftest fenced-json-uses-the-shared-handoff-decoder
  (let [reading-root (temp-root) square-root (temp-root)
        reading (reading! reading-root)
        bound-answer (assoc answer :analysis_digest (:analysis-digest reading))
        result (sut/repair!
                {:author "wm-author" :target-reading-root reading-root
                 :occurrence-square-root square-root
                 :answer-fn (fn [_]
                              (str "```json\n"
                                   (json/generate-string bound-answer)
                                   "\n```"))}
                (judgement reading))]
    (is (= :published (:status result)))
    (is (some? (squares/current square-root "M-live"
                                (:analysis-digest reading)
                                (squares/library-resolver))))))

(deftest answer-must-cover-the-exact-occurrence-set
  (let [reading-root (temp-root) reading (reading! reading-root)
        result (sut/repair!
                {:author "wm-author" :target-reading-root reading-root
                 :occurrence-square-root (temp-root)
                 :answer-fn (fn [_] (assoc answer :analysis_digest
                                           (:analysis-digest reading)
                                           :entries []))}
                (judgement reading))]
    (is (= :error (:status result)))
    (is (= :occurrence-square-answer-unbound (:kind result)))))

(deftest rejected-answer-survives-the-persisted-run-record-bounded
  (let [reading-root (temp-root)
        reading (reading! reading-root)
        run-root (temp-root)
        raw (apply str (repeat (* 200 1024) "x"))
        refusal (sut/repair!
                 {:author "wm-square-author"
                  :target-reading-root reading-root
                  :occurrence-square-root (temp-root)
                  :answer-fn (fn [_] {:state "done"
                                      :job-id "job-invalid-square-answer"
                                      :result raw})}
                 (judgement reading))
        result {:outcome :incomplete
                :data {:failure-kind (:kind refusal)
                       :failure-stage :occurrence-square-authoring
                       :error "Occurrence-square authoring failed before G"
                       :error-data refusal}}
        written (runner/persist-run-record!
                 {:run-record-dir (.getPath run-root)
                  :observation-labels-path (str (io/file run-root "labels.edn"))
                  :scan-render-fn (fn [& _] nil)
                  :nano-time-fn (constantly 0)}
                 "rejected-occurrence-answer" "2026-11-05T00:00:00Z" result)
        record (edn/read-string (slurp (:run-record written)))
        evidence (get-in record [:failure :detail :answer-evidence])]
    (is (= :occurrence-square-answer-invalid (:kind refusal)))
    (is (= "wm-square-author" (:agent evidence)))
    (is (= "job-invalid-square-answer" (:job-id evidence)))
    (is (= :response-invalid (get-in evidence [:defect :kind])))
    (is (= (* 200 1024) (:total-length-bytes evidence)))
    (is (= (* 64 1024) (:prefix-length-bytes evidence)))
    (is (= (* 64 1024) (count (:answer-prefix evidence))))
    (is (true? (:truncated? evidence)))
    (is (= "5d4abf60daba0b11555b6da942dd4f40d5f99ee617e14ba4ba686b47d75a5049"
           (:sha256 evidence)))
    (is (not (some #{:answer-evidence} (get-in record [:failure :detail :omitted]))))))

(deftest occurrence-square-terminal-retains-the-rejection-provenance
  (let [evidence {:schema :wm/rejected-answer-evidence-v1
                  :answer-prefix "bad json"
                  :prefix-length-bytes 8
                  :total-length-bytes 8
                  :sha256 digest-a}
        message (apply str (repeat 400 "m"))
        detail (#'runner/failure-detail
                {:occurrence-square-authoring
                 {:schema :wm/occurrence-square-authoring-v1
                  :status :error
                  :kind :occurrence-square-answer-invalid
                  :answer {:reason :response-invalid
                           :job-id "job-invalid-square-answer"
                           :message message
                           :discarded "not retained"}
                  :answer-evidence evidence}})]
    (is (= :response-invalid
           (get-in detail [:occurrence-square-authoring :answer :reason])))
    (is (= "job-invalid-square-answer"
           (get-in detail [:occurrence-square-authoring :answer :job-id])))
    (is (= 300 (count (get-in detail
                              [:occurrence-square-authoring :answer :message]))))
    (is (not (contains? (get-in detail [:occurrence-square-authoring :answer])
                        :discarded)))
    (is (= evidence
           (get-in detail [:occurrence-square-authoring :answer-evidence])))))

(deftest held-inner-construction-redecides-once-after-publication
  (let [reading-root (temp-root) square-root (temp-root)
        reading (reading! reading-root) calls (atom 0)
        repair (fn [_ j]
                 (sut/repair!
                  {:author "wm-author" :target-reading-root reading-root
                   :occurrence-square-root square-root
                   :answer-fn (fn [_] (assoc answer :analysis_digest
                                             (:analysis-digest reading)))} j))
        result (runner/occurrence-square-authoring-step
                {} (judgement reading) repair
                (fn [_] (swap! calls inc) {:decision {:status :selected}}))]
    (is (= 1 @calls))
    (is (= :selected (get-in result [:judgement :decision :status])))
    (is (= :published (get-in result [:record :status])))))

(deftest malformed-square-maps-answer-gets-one-format-correction-round
  ;; held run 2026-11-06-c89e0ead (repair a3b1cff2): the author returned a
  ;; complete square-maps answer missing one closing brace; without a
  ;; correction round it became a terminal :occurrence-square-answer-invalid
  ;; refusal that cascaded into two debugger holds. The sibling
  ;; selected-target-reading seam gives one undecodable response one format
  ;; correction; this seam must too.
  (let [reading-root (temp-root) square-root (temp-root)
        reading (reading! reading-root)
        digest (:analysis-digest reading)
        ;; exactly the observed defect shape: valid object, one missing brace
        malformed (str "{\"schema\":\"wm/occurrence-square-maps-v1\","
                       "\"target\":\"M-live\","
                       "\"analysis_digest\":\"" digest "\","
                       "\"entries\":[{\"occurrence_id\":[\"" pattern "\",0],"
                       "\"maps\":{\"I1\":{},\"I2\":{},\"G\":{},\"B\":{}}")
        correction-requests (atom [])
        record (sut/repair!
                {:author "wm-author" :target-reading-root reading-root
                 :occurrence-square-root square-root
                 :answer-fn (fn [request]
                              (if (:format-correction request)
                                (do (swap! correction-requests conj request)
                                    {:state "done" :result
                                     (assoc answer :analysis_digest digest)})
                                {:state "done" :result malformed}))}
                (judgement reading))]
    (is (= :published (:status record)) "the corrected round publishes")
    (is (= 1 (count @correction-requests)) "exactly one correction round")
    (is (= 1 (get-in (first @correction-requests)
                     [:format-correction :round])))
    (is (string? (get-in (first @correction-requests)
                         [:format-correction :parser-error])))))

(deftest answer-invalid-after-correction-still-retains-the-refusal
  (let [reading-root (temp-root) square-root (temp-root)
        reading (reading! reading-root)
        digest (:analysis-digest reading)
        malformed (str "{\"schema\":\"wm/occurrence-square-maps-v1\","
                       "\"target\":\"M-live\","
                       "\"analysis_digest\":\"" digest "\",\"entries\":[{")
        record (sut/repair!
                {:author "wm-author" :target-reading-root reading-root
                 :occurrence-square-root square-root
                 :answer-fn (constantly {:state "done" :result malformed})}
                (judgement reading))]
    (is (= :error (:status record)))
    (is (= :occurrence-square-answer-invalid (:kind record)))
    (is (= 1 (get-in record [:format-correction :round]))
        "the exhausted correction round is retained as evidence")
    (is (map? (get-in record [:answer-evidence :defect])))))

(deftest clock-store-busy-repair-throw-escapes-square-authoring-with-intact-data
  ;; held run 2026-11-06-c89e0ead (repair 5aefccdc): the square-authoring
  ;; catch swallowed Agency's typed pre-dispatch busy refusal into a bare
  ;; :occurrence-square-authoring-threw record, so the held phase could not
  ;; present :clock/store-busy or offer the operator-driven :retry.
  (let [busy-data {:outcome :clock-store-busy
                   :failure-kind :clock/store-busy
                   :error/code :clock/store-busy
                   :dispatch/not-created? true}
        busy (ex-info "Agency clock store busy; dispatch not created" busy-data)
        thrown (try
                 (runner/occurrence-square-authoring-step
                  {} (judgement (reading! (temp-root)))
                  (fn [_ _] (throw busy))
                  (fn [_] (throw (AssertionError. "redecide must not run"))))
                 nil
                 (catch clojure.lang.ExceptionInfo e e))]
    (is (identical? busy thrown))
    (is (= busy-data (ex-data thrown)))))

(deftest square-authoring-repair-throw-retains-cause-class-and-ex-data
  (let [cause (ex-info "cannot resolve pattern text"
                       {:kind :pattern-unreadable :pattern pattern})
        result (runner/occurrence-square-authoring-step
                {} (judgement (reading! (temp-root)))
                (fn [_ _] (throw cause))
                (fn [_] (throw (AssertionError. "redecide must not run"))))
        record (:record result)]
    (is (= :occurrence-square-authoring-threw (:kind record)))
    (is (= "cannot resolve pattern text" (:message record)))
    (is (= "clojure.lang.ExceptionInfo" (:error-class record)))
    (is (= {:kind :pattern-unreadable :pattern pattern} (:error-data record)))))

(deftest debugger-does-not-stop-between-reading-and-square-authoring
  (let [reading (reading! (temp-root))
        missing-squares (judgement reading)
        authoring-port (fn [& _] nil)]
    (is (false? (runner/selected-target-redecision-terminal?
                 {} authoring-port missing-squares)))
    (is (true? (runner/selected-target-redecision-terminal?
                {} nil missing-squares)))
    (is (true? (runner/selected-target-redecision-terminal?
                {:flight {}} authoring-port missing-squares)))
    (is (true? (runner/selected-target-redecision-terminal?
                {} authoring-port {:decision {:status :selected}})))))
