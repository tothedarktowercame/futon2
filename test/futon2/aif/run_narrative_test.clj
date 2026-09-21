(ns futon2.aif.run-narrative-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.run-narrative :as narrative]))

(deftest narrative-entrypoint-exists
  (let [renderer (try (requiring-resolve 'futon2.aif.run-narrative/render-run!)
                      (catch java.io.FileNotFoundException _ nil))]
    (is (some? renderer) "A run id must have a read-only Markdown renderer")))

(defn write-record [path record]
  (io/make-parents path)
  (spit path (pr-str record)))

(defn fixture [f]
  (let [base (.toFile (java.nio.file.Files/createTempDirectory "run-narrative-" (make-array java.nio.file.attribute.FileAttribute 0)))
        root (str (io/file base "futon2"))
        run "2026-09-21-fixture"
        a {:kind :cascade-candidate :id :C1 :target "M-one"
           :precedence [{:id :p/one}]
           :construction-receipt {:kind :hand-admitted}}
        other (assoc a :target "M-two")
        d {:action a
           :selection-law {:posterior {a 0.7 other 0.3}}
           :selection-certificate {:candidates [{:id a :g 1.25 :f 0 :habit 0.5}
                                                {:id other :g 2.75 :f 0 :habit 0.5}]}}
        record-path (str (io/file root "data/wm-runs" (str "tick-run-record-" run ".edn")))
        attempt-dir (io/file root "data/wm-cohort/cohort-test/attempt-001")
        trace-path (io/file root "data/wm-trace/wm-trace-2026-09-21.edn")
        record {:run/id run :click/id "click-test" :startedAt "2026-09-21T00:00:00Z" :decision d
                :cohort-attempt {:cohort/id :cohort-test :attempt/id "attempt-001"}}
        js {:time-step {:opportunity-id "op-test" :trigger :test}
            :selection {:selected-action a :selected-mission ":C1" :controller-decision d
                        :ranked-candidates [{:G-efe 0.7}]}
            :construction {:cascade {:selected-action a :semilattice []}
                           :wiring {:boxes [] :wires []} :patterns [:p/one]}
            :dispatch {:agent "author" :job-id "author-job" :prompt-ref "agency-job:author-job"}
            :build {:commits ["abc123"] :artifacts ["src/change.clj"]}
            :adjudication {:build-match {:commit "abc123" :review-approved? true}}
            :closed {:outcome :grounded-change :duration-ms 120}}]
    (try
      (write-record record-path record)
      (doseq [[i stage] (map-indexed vector narrative/checkpoint-order)]
        (write-record (io/file attempt-dir (format "%03d-%s.edn" (inc i) (name stage)))
                      {:cohort/id :cohort-test :attempt/id "attempt-001" :checkpoint/type stage
                       :payload {:judgment (js stage)}}))
      (io/make-parents trace-path)
      (spit trace-path (str (pr-str {:run/id "wrong-run"}) "\n"
                            (pr-str {:run/id run :cohort-attempt (:cohort-attempt record)
                                     :cascade-problems {:problems [{} {}] :refusals [{:kind :missing}]}})))
      (write-record (io/file root "data/wm-full-loop-phases.edn.log")
                    {:opportunity-id "op-test" :phase :selection :duration-ms 11})
      (f {:root root :run run :record record :record-path record-path :attempt-dir attempt-dir
          :trace-path trace-path :output (str (io/file base "narrative.md")) :base base})
      (finally (doseq [file (reverse (file-seq base))] (io/delete-file file true))))))

(defn file-snapshot [root]
  (into {} (for [f (file-seq (io/file root)) :when (.isFile f)] [(str f) (slurp f)])))

(deftest sections-numbers-missing-fields-and-read-only-output
  (fixture
   (fn [{:keys [root run output base]}]
     (let [before (file-snapshot base)
           _ (narrative/render-run! root run output)
           text (slurp output)
           sections (rest (str/split text #"(?m)^## "))
           after (file-snapshot base)]
       (is (= (map name narrative/checkpoint-order) (map #(first (str/split-lines %)) sections)))
       (doseq [section sections]
         (is (str/includes? section "Source: `"))
         (is (str/includes? section ".edn` — `[:payload :judgment]")))
       (is (str/includes? text "It chose M-one"))
       (is (str/includes? text "| M-one | :C1 | 1.25 | 0.7 |"))
       (is (str/includes? text "| M-two | :C1 | 2.75 | 0.3 |"))
       (is (str/includes? text "full G spread is 1.5 nats"))
       (is (str/includes? text "Not recorded in this run: prompt text"))
       (is (str/includes? text "semilattice field is not computed (literal [])"))
       (is (not (str/includes? text "structure is a semilattice")))
       (is (str/includes? text "[:form 2 :cascade-problems]"))
       (is (str/includes? text "selection 11 ms"))
       (is (= (inc (count before)) (count after)))
       (is (= before (dissoc after output)))
       (is (= text (narrative/narrative-text (narrative/load-run root run))))))))

(defn retain [path text]
  (spit path text)
  {:status :present :path (str path)
   :sha256 (apply str (map #(format "%02x" (bit-and 255 %))
                          (.digest (java.security.MessageDigest/getInstance "SHA-256")
                                   (.getBytes text "UTF-8"))))})

(deftest retained-prompts-and-replies-are-read-not-reconstructed
  (fixture
   (fn [{:keys [root run attempt-dir output]}]
     (let [prompt (retain (io/file attempt-dir "prompt.txt")
                          "Author\nPATTERN CASCADE:\nThe actual retained plan.\nCONSTRUCTION CONTRACT: end")
           reply (retain (io/file attempt-dir "reply.txt") "I changed the shared updater.")
           closed (io/file attempt-dir "007-closed.edn")
           event (edn/read-string (slurp closed))]
       (write-record closed (assoc-in event [:payload :judgment :job-texts]
                                     [{:job-id "author-job" :role :author :prompt prompt :reply reply}]))
       (narrative/render-run! root run output)
       (let [text (slurp output)]
         (is (str/includes? text "quoted from the retained author prompt"))
         (is (str/includes? text "> The actual retained plan."))
         (is (str/includes? text "> I changed the shared updater."))
         (is (not (str/includes? text "plan is a reconstruction"))))
       (spit (:path prompt) "tampered")
       (is (thrown-with-msg? clojure.lang.ExceptionInfo #"digest mismatch"
                            (narrative/render-run! root run output)))))))

(deftest absent-checkpoint-stays-explicit-and-ambiguous-attempt-refuses
  (fixture
   (fn [{:keys [root run attempt-dir output]}]
     (io/delete-file (io/file attempt-dir "005-build.edn"))
     (narrative/render-run! root run output)
     (is (str/includes? (slurp output) "## build\n\nNot recorded in this run: checkpoint"))
     (let [duplicate (io/file root "data/other/cohort-test/attempt-001/001-time-step.edn")]
       (write-record duplicate (edn/read-string (slurp (io/file attempt-dir "001-time-step.edn"))))
       (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Ambiguous"
                            (narrative/load-run root run)))))))

(deftest recorded-comparisons-are-rendered-without-inference
  (fixture
   (fn [{:keys [root run]}]
     (let [b (-> (narrative/load-run root run)
                 (assoc-in [:checkpoints :selection :judgment :controller-decision :selection-law :policy-comparison]
                           {:decided-by :habit :near-tie? true :near-tie-threshold {:status :declared :value 0.1}})
                 (assoc-in [:checkpoints :selection :judgment :controller-decision :selection-law :action-comparison]
                           {:decided-by #{:habit}}))
           text (narrative/narrative-text b)]
       (is (str/includes? text "policy comparison records decided-by :habit, near-tie true"))
       (is (str/includes? text "action comparison records decided-by #{:habit}"))))))

(deftest click-binding-resolves-an-attempt-without-a-trace
  (fixture
   (fn [{:keys [root run record record-path trace-path base]}]
     (io/delete-file trace-path)
     (write-record record-path (-> record (dissoc :cohort-attempt)
                                   (assoc :execution-cohort {:cohort-id :cohort-test})))
     (let [binding-path (io/file base "futon3c/data/wm-click-run-bindings/click-run-binding-click-test.edn")]
       (write-record binding-path {:run-id-observation {:value run} :attempt/id "attempt-001"})
       (is (= :test (get-in (narrative/load-run root run) [:checkpoints :time-step :judgment :trigger])))
       (write-record binding-path {:run-id-observation {:value "other-run"} :attempt/id "attempt-001"})
       (is (thrown-with-msg? clojure.lang.ExceptionInfo #"different run"
                            (narrative/load-run root run)))))))

(deftest missing-wires-do-not-establish-a-singleton-and-explicit-ranking-is-supported
  (fixture
   (fn [{:keys [root run]}]
     (let [b (-> (narrative/load-run root run)
                 (assoc-in [:checkpoints :construction :judgment :wiring] nil)
                 (assoc-in [:checkpoints :selection :judgment :controller-decision :selection-certificate] nil)
                 (assoc-in [:checkpoints :selection :judgment :ranked-candidates]
                           [{:target "M-ranked" :cascade-id :C2 :G 8.5 :posterior 0.8}]))
           text (narrative/narrative-text b)]
       (is (not (str/includes? text "structure is a singleton")))
       (is (str/includes? text "| M-ranked | :C2 | 8.5 | 0.8 |"))))))

(deftest output-cannot-overwrite-retained-text
  (fixture
   (fn [{:keys [root run attempt-dir output]}]
     (let [prompt (retain output "PATTERN CASCADE: kept")
           closed (io/file attempt-dir "007-closed.edn")
           event (edn/read-string (slurp closed))]
       (write-record closed (assoc-in event [:payload :judgment :job-texts]
                                     [{:job-id "author-job" :prompt prompt}]))
       (is (thrown-with-msg? clojure.lang.ExceptionInfo #"overwrite retained evidence"
                            (narrative/render-run! root run output)))
       (is (= "PATTERN CASCADE: kept" (slurp output)))))))

(deftest scan-account-and-unit-labelled-coverage-reach-the-narrative
  (fixture
   (fn [{:keys [root run record record-path attempt-dir output]}]
     (let [scan (retain (io/file attempt-dir "scan.md") "The retained perceive-stage scan.")
           coverage {:source-tokens {:unit :source-token :total 4 :reached 2}
                     :projected-outcome-tokens {:unit :target-qualified-outcome-token :count 3}}]
       (write-record record-path (assoc record :scan-report scan
                                       :live-c-coverage coverage
                                       :mission-hole-coverage {:holes-retained 3 :holes-projected 2}))
       (narrative/render-run! root run output)
       (let [text (slurp output)]
         (is (str/includes? text "[Retained scan account]"))
         (is (str/includes? text (:sha256 scan)))
         (is (str/includes? text "C reached 2 of 4 source tokens, with 3 projected outcome tokens"))
         (is (str/includes? text "mission-hole census retained 3 holes and projected 2"))
         (is (str/includes? text "[:live-c-coverage]")))))))

(deftest older-runs-explicitly-lack-scan-and-coverage
  (fixture
   (fn [{:keys [root run]}]
     (let [text (narrative/narrative-text (narrative/load-run root run))]
       (is (str/includes? text "Not recorded in this run: scan account"))
       (is (str/includes? text "not recorded in this run: live C coverage"))
       (is (str/includes? text "not recorded in this run: mission-hole coverage"))))))

(deftest trace-coverage-fallback-preserves-unit-and-absence-meaning
  (fixture
   (fn [{:keys [root run]}]
     (let [live {:source-tokens {:unit :source-token :total 4 :reached 2}
                 :projected-outcome-tokens {:unit :target-qualified-outcome-token :count 3}}
           bundle (assoc-in (narrative/load-run root run) [:trace :live-c-coverage] live)]
       (is (str/includes? (narrative/narrative-text bundle)
                          "C reached 2 of 4 source tokens, with 3 projected outcome tokens"))
       (is (str/includes? (narrative/narrative-text bundle) "[:form 2 :live-c-coverage]"))
       (let [wrong (assoc-in bundle [:trace :live-c-coverage :source-tokens :unit] :source-entry)]
         (is (not (str/includes? (narrative/narrative-text wrong) "C reached 2"))))
       (let [absent (assoc-in bundle [:record :live-c-coverage]
                              {:status :absent :reason :no-admitted-cascade-problems})]
         (is (str/includes? (narrative/narrative-text absent) ":no-admitted-cascade-problems"))
         (is (not (str/includes? (narrative/narrative-text absent) "C reached 2"))))))))
