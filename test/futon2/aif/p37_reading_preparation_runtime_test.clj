(ns futon2.aif.p37-reading-preparation-runtime-test
  "P37's production-shaped pre-choice preparation caller path."
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.meta-live-outer-selector :as meta-live]
            [futon2.aif.pattern-graph-pin :as graph-pin]
            [futon2.report.war-machine :as war-machine]
            [futon2.aif.selected-outer-cascade-policy :as selected]
            [futon2.aif.target-reading-registry :as registry]
            [futon2.aif.meta-live-outer-selector-test]
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

(defn- meta-var [sym]
  (var-get (ns-resolve 'futon2.aif.meta-live-outer-selector-test sym)))

(defn- graph-file [dir]
  (let [file (io/file dir "graph.json")]
    (spit file (json/generate-string
                {:records 0 :patterns 0 :pattern_ids [] :summary [] :edges []}))
    (graph-pin/pin! file)
    file))

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
        selected-receipt (run-live (explicit-provider [0.0 1.0] [])
                                   {:seed (outer-var 'seed)})
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
        (is (= :pending (:status record)))
        (is (= :batch-limit (:reason record)))
        (is (= [["T-001" digest-1]] (:published-ids frontier)))
        (is (= [["T-002" (apply str (repeat 64 "2"))]
                ["T-003" (apply str (repeat 64 "2"))]]
               (:refused-ids frontier)))
        (is (= 695 (count (:pending-ids frontier)))
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
        (is (= 695 (count (get-in terminal [:reading-frontiers :preparation-backlog
                                             :frontier :pending-ids]))))
        (is (nil? (selected/verify
                   (get-in result [:judgement :outer-task-selection :policy
                                   :selected-outer-cascade-policy]))))))

(deftest zero-assessed-frontier-retains-causes-and-no-choice-through-terminal-record
  ;; This enters the same outer boundary production uses: select-outer-task
  ;; invokes META's select-live with the real full-eligible-target-field.  The
  ;; isolated source files have no published readings, so every family is
  ;; excluded by the producer rather than by a hand-built field or receipt.
  ;; Its unsealed-menu refusal is :no-canonically-bound-policy. L1's distinct
  ;; :incomplete-policy-menu-coverage came from cascade-decision's
  ;; select-over-families route and is covered by P38b, not fabricated here.
  (let [work (temp-dir "p38-no-choice-work-")
        selection-as-of "2026-10-22T12:34:56Z"
        readings (temp-dir "p38-no-choice-readings-")
        graph (graph-file work)
        sources (into {}
                      (for [target ["M-a" "M-b"]
                            :let [source (io/file work (str target ".md"))]]
                        (do (spit source (str "# " target "\n\n## HEAD\nopen\n"))
                            [target source])))
        tasks (mapv (fn [[id source]]
                      {:id id :kind :mission :priority 1
                       :source {:path (.getPath source)
                                :sha256 (load-identity/sha256
                                         (.getBytes (slurp source) "UTF-8"))}})
                    sources)
        snapshot (meta-var 'snapshot)
        receipt (war-machine/select-outer-task
                 {:outer-task-policy :meta
                  :selection-as-of selection-as-of
                  :outer-task-selection-fn
                  (fn [input]
                    (meta-live/select-live
                     (assoc input
                            :focus-inputs {}
                            :focus-context {:as-of selection-as-of
                                            :status :retained
                                            :focus "WM"}
                            :focus-classify-fn
                            (fn [_ _ _ target _]
                              {:target target :class :focus})
                            :fetch-snapshot (constantly snapshot)
                            :policy-field-config
                            {:target-reading-root (.getPath readings)
                             :pattern-graph-path (.getPath graph)})))}
                 tasks 0)
        persisted (#'runner/persist-run-record!
                   {:run-record-dir (.getPath work) :scan-render-fn (fn [& _] nil)}
                   "p38-zero-assessed" "2026-10-19T00:00:00Z"
                   {:outcome :abstained
                    :wm/route [{:fromNode "RUNNER" :toNode "FULL_LOOP_CLOSE"
                                :via :abstained}]
                    :checkpoints {:selection {:sorry
                                              {:outer-task-selection receipt
                                               :decision {:status :abstained
                                                          :reason :no-acting-cascade-candidate}}}}})
        terminal (edn/read-string (slurp (:run-record persisted)))]
    (is (= [] (get-in terminal [:reading-frontiers :selection-frontier :assessed-ids])))
    (is (= {:absent :no-canonically-bound-policy}
           (get-in terminal [:outer-task-selection :chosen])))
    (is (nil? (get-in terminal [:decision :chosen :action]))
        "zero assessed families retain abstention; they do not fabricate an action")))
