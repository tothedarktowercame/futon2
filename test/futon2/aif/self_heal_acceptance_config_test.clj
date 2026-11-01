(ns futon2.aif.self-heal-acceptance-config-test
  "SELF-HEAL-ACCEPTANCE-TEST-CONFIG (2026-11-01).

  Real fixture: click 11 wm-click-8d7a38d9-e1e2-4708-a267-d5415a97ea42 on
  injury injury/7d5e374e7297e8814b21cb5a1b4257839496bc62 refused
  :meta-self-heal/production-config-required, :absent-terms [:acceptance-test]
  before any attempt. The retained artifacts show the acceptance pipeline DID
  run: diagnosis-cda379531422e39f8005000155880a87ac1632b82f402024a4858593adcdb3a0
  (04:18:43Z) and proposal
  acceptance-b75a55a1f8cb05c565220e578dc2b93cce8e2f7272f1b202f0b4e9be32a3a014
  (04:19:00.102Z; still :proposed/:unreviewed/:not-run). The click refused
  13ms after the proposal was persisted: admission refused instantly because
  the author's test source calls cause-scope/assert-repair through the
  :require ALIAS, while designated-test-defect compared the head symbol
  literally against 'futon2.aif.cause-scope/assert-repair. The runtime then
  relabelled that pipeline refusal as a production-config absence.

  Pre-fix replay (zai-5, 2026-11-01, prepare-acceptance with the real
  artifacts and boundary ports) reproduced
  {:status :refused :reason :cause-scope/designated-call-required}."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cause-scope :as cause-scope]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.meta-self-heal-production :as msp]))

(def source
  "Verbatim :source :text of the retained proposal acceptance-b75a55a1."
  "(ns futon2.aif.injury-7d5e374e-acceptance-test
  (:require [clojure.test :refer [deftest]]
            [futon2.aif.cause-scope :as cause-scope]))

(deftest injury-7d5e374e-acceptance
  (cause-scope/assert-repair
   {:cause-entry \"futon2.aif.wm.terminal-receipt/terminal-receipt\",
    :input-pin
    {:path \"/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-11-01-ac5bc477-9e34-4249-a692-f13a9f22a324.edn\",
     :sha256 \"7dbf1614668d9f8c565562447c5d336b9424e6bb93d42335fde1ba1c32e7a9ac\"},
    :input-path [],
    :input-digest \"9416c2517aef4c836e8d0d95ad122fd121efcc6e82f5eb19bd3638ac59924089\",
    :healthy-predicate {:op :result-path-equals, :path [:kind], :value :action-receipt}}))")

(def scope
  "Verbatim request-enrichment :cause-scope of the same proposal."
  {:cause-entry "futon2.aif.wm.terminal-receipt/terminal-receipt"
   :input-pin
   {:path "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-11-01-ac5bc477-9e34-4249-a692-f13a9f22a324.edn"
    :sha256 "7dbf1614668d9f8c565562447c5d336b9424e6bb93d42335fde1ba1c32e7a9ac"}
   :input-path []
   :input-digest "9416c2517aef4c836e8d0d95ad122fd121efcc6e82f5eb19bd3638ac59924089"
   :healthy-predicate {:op :result-path-equals :path [:kind] :value :action-receipt}})

(deftest real-alias-designated-call-is-admissible
  ;; Pre-fix this returned :cause-scope/designated-call-required (watched in
  ;; the live replay). The alias resolves through the source's own :require,
  ;; so the author's real test passes the designated-call gate.
  (is (nil? (cause-scope/designated-test-defect source scope))))

(deftest alias-resolution-does-not-weaken-scope-identity
  ;; The alias form with a WRONG scope must still be a scope mismatch.
  (is (= :cause-scope/scope-mismatch
         (cause-scope/designated-test-defect
          source (assoc scope :cause-entry "some.other/entry"))))
  ;; A non-designated call is still refused, alias or not.
  (is (= :cause-scope/designated-call-required
         (cause-scope/designated-test-defect
          (str/replace source "assert-repair" "other-fn") scope))))

(deftest fully-qualified-designated-call-still-admissible
  (let [qualified (str/replace
                   source "(cause-scope/assert-repair"
                   "(futon2.aif.cause-scope/assert-repair")]
    (is (nil? (cause-scope/designated-test-defect qualified scope)))))

(deftest prepare-acceptance-with-real-source-reaches-the-run-port
  ;; Through the real route: with an acceptance test returned by the proposal
  ;; port (same structure as the retained proposal, its scope map sealed in
  ;; THIS JVM so the designated-scope comparison is apples-to-apples — the
  ;; retained 9416... digest came from the serving JVM's seal, see the note
  ;; in the report), admission must get past the designated-call gate and the
  ;; static check to the run port (which throws its marker here). Persisting
  ;; goes to a temp root; no canonical store is touched.
  (let [root (str (java.nio.file.Files/createTempDirectory
                   "self-heal-acceptance-test" (make-array java.nio.file.attribute.FileAttribute 0)))
        diag-path "/home/joe/code/futon2/data/wm-meta-injuries-live-2026-10-22/diagnoses/diagnosis-cda379531422e39f8005000155880a87ac1632b82f402024a4858593adcdb3a0.edn"
        diag-bytes (java.nio.file.Files/readAllBytes (.toPath (java.io.File. diag-path)))
        diagnosis (edn/read-string (String. diag-bytes "UTF-8"))
        sealed (cause-scope/seal (get-in diagnosis [:diagnosis :cause-entry]))
        diag-obs {:status :available
                  :injury-id "injury/7d5e374e7297e8814b21cb5a1b4257839496bc62"
                  :diagnosis-evidence-pin
                  {:path diag-path
                   :sha256 (load-identity/sha256 diag-bytes)}
                  :source-record-pin
                  {:path "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-11-01-ac5bc477-9e34-4249-a692-f13a9f22a324.edn"
                   :sha256 "7dbf1614668d9f8c565562447c5d336b9424e6bb93d42335fde1ba1c32e7a9ac"}}
        dynamic-source
        (str "(ns futon2.aif.injury-7d5e374e-acceptance-test\n"
             "  (:require [clojure.test :refer [deftest]]\n"
             "            [futon2.aif.cause-scope :as cause-scope]))\n\n"
             "(deftest injury-7d5e374e-acceptance\n"
             "  (cause-scope/assert-repair\n"
             "   " (pr-str sealed) "))")
        result (msp/prepare-acceptance
                {:meta-acceptance-root (str root "/acceptances")
                 :meta-acceptance-static-correction-limit 1
                 :meta-acceptance-return-limit 1
                 :meta-cause-scope-required? true
                 :meta-acceptance-fn (constantly
                                      {:status :proposed
                                       :acceptance-test
                                       {:id "injury-7d5e374e7297e8814b21cb5a1b4257839496bc62-acceptance-1cf0365c"
                                        :command ["clojure" "-M:test" "-n"
                                                  "futon2.aif.injury-7d5e374e-acceptance-test"
                                                  "-v" "injury-7d5e374e-acceptance"]
                                        :expected {:exit 0}
                                        :source {:namespace "futon2.aif.injury-7d5e374e-acceptance-test"
                                                 :path "test/futon2/aif/injury_7d5e374e_acceptance_test.clj"
                                                 :sha256 "7fedcf154a95c30f9cd4b866f5b17ba6f18c91fdc0f6afc30fb1cb5b4979633f"
                                                 :text dynamic-source}}})
                 :meta-acceptance-run-fn (fn [_]
                                          (throw (ex-info "RUN-PORT-REACHED" {})))
                 :meta-acceptance-review-fn (fn [_]
                                             (throw (ex-info "REVIEW-PORT-REACHED" {})))}
                diag-obs)]
    (is (not= :cause-scope/designated-call-required (:reason result))
        "the alias gate no longer refuses the real proposal")
    (is (= :meta-self-heal/acceptance-run-threw
           (get-in result [:acceptance-result :reason]))
        "admission reaches the run port (the marker throw is the run's
         failure reason, reported through :acceptance-run-refused)")
    (is (= :meta-self-heal/acceptance-run-refused (:reason result)))))

(deftest acceptance-pipeline-refusal-surfaces-its-own-reason
  ;; Pre-fix, the runtime let route! relabel this as
  ;; :meta-self-heal/production-config-required :absent-terms [:acceptance-test].
  (let [observation {:status :refused
                     :reason :cause-scope/designated-call-required
                     :injury-id "injury/7d5e374e7297e8814b21cb5a1b4257839496bc62"}
        refusal (runtime/acceptance-pipeline-refusal observation {})]
    (is (= :refused (:status refusal)))
    (is (= :cause-scope/designated-call-required (:reason refusal)))
    (is (= :dedicated-meta-self-heal (:route refusal)))
    (is (false? (:ordinary-selection-called? refusal))))
  ;; A supplied/accepted test is route!'s business, not this guard's.
  (is (nil? (runtime/acceptance-pipeline-refusal
             {:status :refused :reason :x} {:acceptance-test {:id "t"}})))
  ;; No pipeline refusal, nothing to surface.
  (is (nil? (runtime/acceptance-pipeline-refusal {:status :accepted} {}))))
