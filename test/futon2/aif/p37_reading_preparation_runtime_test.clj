(ns futon2.aif.p37-reading-preparation-runtime-test
  "P37's production-shaped pre-choice preparation caller path."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.selected-outer-cascade-policy :as selected]
            [futon2.aif.target-reading-registry :as registry]
            [futon2.aif.selected-target-reading-test]
            [futon2.aif.outer-policy-runtime-selection-test])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- outer-var [sym]
  (var-get (ns-resolve 'futon2.aif.outer-policy-runtime-selection-test sym)))

(defn- pin [path c] {:path path :sha256 (apply str (repeat 64 c))})

(defn- temp-dir [prefix]
  (.toFile (Files/createTempDirectory prefix (make-array FileAttribute 0))))

(defn- analysis-for [request]
  ((var-get (ns-resolve 'futon2.aif.selected-target-reading-test 'analysis-for)) request))

(deftest bounded-prechoice-preparation-retains-frontier-and-resumes-an-assessed-choice
  ;; production-defaults supplies the actual runtime port; runner owns the
  ;; publication/redecision handoff; run-live is the real outer P05 selector.
  (let [work (temp-dir "p37-reading-work-")
        readings (temp-dir "p37-reading-registry-")
        source-1 (io/file work "T-1.md")
        _ (spit source-1 "# T-1\n\n## HEAD\nOPEN work.\n")
        digest-1 (registry/excerpt-digest source-1)
        reading-absent-ids (mapv #(format "T-%03d" %) (range 1 699))
        field {:schema :wm/pre-choice-policy-field-v1
               :source-pin (pin "wm://prechoice-field" "e")
               :target-sources (vec (concat
                                     [{:target-id "T-occ" :source-path "T-occ.md"}
                                      {:target-id "T-ready" :source-path "T-ready.md"}
                                      {:target-id "T-001" :source-path (.getPath source-1)}]
                                     (map (fn [target-id]
                                            {:target-id target-id
                                             :source-path (str "/absent/" target-id ".md")})
                                          (rest reading-absent-ids))))
               :families (vec (concat
                               [{:target-id "T-occ" :status :computed :failures []}
                                {:target-id "T-ready" :status :computed :failures []}]
                               (map (fn [target-id]
                                      {:target-id target-id :status :refused
                                       :failures [{:kind :no-current-target-reading
                                                   :expected-digest
                                                   (if (= "T-001" target-id)
                                                     digest-1
                                                     (apply str (repeat 64 "2")))}]})
                                    reading-absent-ids)))}
        refusal {:outer-task-selection
                 {:chosen {:absent :incomplete-policy-menu-coverage}
                  :policy {:reason :incomplete-policy-menu-coverage}
                  :policy-field field}}
        run-live (outer-var 'run-live)
        explicit-provider (outer-var 'explicit-provider)
        tasks (outer-var 'tasks)
        selected-receipt (run-live (explicit-provider [0.0 1.0] []))
        select (runtime/retained-outer-selector (fn [_] selected-receipt) {})
        defaults (runtime/production-defaults
                  {:run-id "p37-prechoice" :author "wm-author"
                   :request-root (.getPath work)
                   :target-reading-root (.getPath readings)
                   :selected-target-reading-answer-fn
                   (fn [{:keys [request]}] {:analysis (analysis-for request)})})
        result (runner/selected-target-reading-step
                {} refusal (:selected-target-reading-fn defaults)
                (fn [_] {:outer-task-selection (select {:tasks tasks})}))
        record (:record result)
        frontier (:frontier record)
        published (first (:records record))
        reread (registry/current-reading (.getPath readings) "T-001" digest-1)
        persisted (#'runner/persist-run-record!
                   {:run-record-dir (.getPath work) :scan-render-fn (fn [& _] nil)}
                   "p38-terminal-frontier" "2026-10-19T00:00:00Z"
                   {:outcome :abstained
                    :wm/route [{:fromNode "RUNNER" :toNode "FULL_LOOP_CLOSE"
                                :via :abstained}]
                    :checkpoints {:selection {:judgment (:judgement result)}}})
        terminal (edn/read-string (slurp (:run-record persisted)))]
        (is (= :refused (:status record)))
        (is (= :publication-refused (:reason record)))
        (is (= [["T-001" digest-1]] (:published-ids frontier)))
        (is (= [["T-002" (apply str (repeat 64 "2"))]] (:refused-ids frontier)))
        (is (= 697 (count (:pending-ids frontier)))
            "bounded preparation leaves the rest queued")
        (is (= 700 (count (:eligible-target-ids frontier))))
        (is (= ["T-occ" "T-ready"] (:assessed-target-ids frontier)))
        (is (= reading-absent-ids (:reading-absent-ids frontier)))
        (is (= :published (:status published)))
        (is (.isFile (io/file (:request-file published))))
        (is (= :current-candidate (:status reread)))
        (is (= :selected-target-reading-unpinned
               (get-in record [:records 1 :kind])))
        (is (= :selected (get-in result [:judgement :outer-task-selection :policy :status])))
        (is (= "T-occ" (get-in result [:judgement :outer-task-selection :chosen :id])))
        (is (= record (get-in result [:judgement :selected-target-reading-request])))
        (is (= 700 (count (get-in terminal [:reading-frontiers :selection-frontier
                                             :eligible-ids]))))
        (is (= ["T-occ" "T-ready"]
               (get-in terminal [:reading-frontiers :selection-frontier :assessed-ids])))
        (is (= reading-absent-ids
               (get-in terminal [:reading-frontiers :selection-frontier :reading-absent-ids])))
        (is (= 697 (count (get-in terminal [:reading-frontiers :preparation-backlog
                                             :frontier :pending-ids]))))
        (is (nil? (selected/verify
                   (get-in result [:judgement :outer-task-selection :policy
                                   :selected-outer-cascade-policy]))))))

(deftest zero-assessed-frontier-retains-causes-and-no-choice-through-terminal-record
  ;; This uses P37's production port even though it has no reading to prepare:
  ;; the no-choice terminal must retain the producer's zero-assessed field, not
  ;; manufacture a choice from a bounded preparation receipt.
  (let [work (temp-dir "p38-no-choice-work-")
        readings (temp-dir "p38-no-choice-readings-")
        field {:schema :wm/pre-choice-policy-field-v1
               :source-pin (pin "wm://prechoice-zero" "f")
               :families [{:target-id "T-unavailable" :status :refused
                           :failures [{:kind :target-source-unavailable
                                       :path "/absent/T-unavailable.md"}]}
                          {:target-id "T-retracted" :status :refused
                           :failures [{:kind :policy-retracted
                                       :reason :no-admissible-cascade}]}]}
        no-choice {:outer-task-selection
                   {:schema :wm/outer-task-selection-v1
                    :chosen {:absent :incomplete-policy-menu-coverage}
                    :policy {:reason :incomplete-policy-menu-coverage
                             :policy-field field}}
                   :decision {:status :abstained
                              :reason :no-acting-cascade-candidate}}
        defaults (runtime/production-defaults
                  {:run-id "p38-zero-assessed" :author "wm-author"
                   :request-root (.getPath work)
                   :target-reading-root (.getPath readings)})
        producer-result (runner/selected-target-reading-step
                         {} no-choice (:selected-target-reading-fn defaults) identity)
        persisted (#'runner/persist-run-record!
                   {:run-record-dir (.getPath work) :scan-render-fn (fn [& _] nil)}
                   "p38-zero-assessed" "2026-10-19T00:00:00Z"
                   {:outcome :abstained
                    :wm/route [{:fromNode "RUNNER" :toNode "FULL_LOOP_CLOSE"
                                :via :abstained}]
                    :checkpoints {:selection {:sorry (:judgement producer-result)}}})
        terminal (edn/read-string (slurp (:run-record persisted)))]
    (is (nil? (:record producer-result))
        "the production preparation port found no reading work to turn into a choice")
    (is (= [] (get-in terminal [:reading-frontiers :selection-frontier :assessed-ids])))
    (is (= [] (get-in terminal [:reading-frontiers :selection-frontier :reading-absent-ids])))
    (is (= {"T-unavailable" [{:kind :target-source-unavailable
                                :path "/absent/T-unavailable.md"}]
            "T-retracted" [{:kind :policy-retracted
                             :reason :no-admissible-cascade}]}
           (get-in terminal [:reading-frontiers :selection-frontier :other-exclusions])))
    (is (= {:absent :incomplete-policy-menu-coverage}
           (get-in terminal [:outer-task-selection :chosen])))
    (is (nil? (get-in terminal [:decision :chosen :action]))
        "zero assessed families retain abstention; they do not fabricate an action")))
