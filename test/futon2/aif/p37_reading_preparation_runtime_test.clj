(ns futon2.aif.p37-reading-preparation-runtime-test
  "P37's production-shaped pre-choice preparation caller path."
  (:require [clojure.java.io :as io]
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
        source-3 (io/file work "T-3.md")
        source-4 (io/file work "T-4.md")
        _ (doseq [source [source-1 source-3 source-4]]
            (spit source (str "# " (.getName source) "\n\n## HEAD\nOPEN work.\n")))
        digest-1 (registry/excerpt-digest source-1)
        digest-3 (registry/excerpt-digest source-3)
        digest-4 (registry/excerpt-digest source-4)
        field {:schema :wm/pre-choice-policy-field-v1
               :source-pin (pin "wm://prechoice-field" "e")
               :target-sources [{:target-id "T-occ" :source-path "T-occ.md"}
                                {:target-id "T-1" :source-path (.getPath source-1)}
                                {:target-id "T-2" :source-path "/absent/T-2.md"}
                                {:target-id "T-3" :source-path (.getPath source-3)}
                                {:target-id "T-4" :source-path (.getPath source-4)}]
               :families (vec (concat
                               [{:target-id "T-occ" :status :computed :failures []}]
                               [{:target-id "T-1" :status :refused
                                 :failures [{:kind :no-current-target-reading
                                             :expected-digest digest-1}]}
                                {:target-id "T-2" :status :refused
                                 :failures [{:kind :no-current-target-reading
                                             :expected-digest (apply str (repeat 64 "2"))}]}
                                {:target-id "T-3" :status :refused
                                 :failures [{:kind :no-current-target-reading
                                             :expected-digest digest-3}]}
                                {:target-id "T-4" :status :refused
                                 :failures [{:kind :no-current-target-reading
                                             :expected-digest digest-4}]}]))}
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
        reread (registry/current-reading (.getPath readings) "T-1" digest-1)]
        (is (= :refused (:status record)))
        (is (= :publication-refused (:reason record)))
        (is (= [["T-1" digest-1]] (:published-ids frontier)))
        (is (= [["T-2" (apply str (repeat 64 "2"))]] (:refused-ids frontier)))
        (is (= [["T-2" (apply str (repeat 64 "2"))]
                ["T-3" digest-3] ["T-4" digest-4]]
               (:pending-ids frontier)))
        (is (= ["T-occ" "T-1" "T-2" "T-3" "T-4"]
               (:eligible-target-ids frontier)))
        (is (= ["T-occ"] (:assessed-target-ids frontier)))
        (is (= ["T-1" "T-2" "T-3" "T-4"] (:reading-absent-ids frontier)))
        (is (= :published (:status published)))
        (is (.isFile (io/file (:request-file published))))
        (is (= :current-candidate (:status reread)))
        (is (= :selected-target-reading-unpinned
               (get-in record [:records 1 :kind])))
        (is (= :selected (get-in result [:judgement :outer-task-selection :policy :status])))
        (is (= "T-occ" (get-in result [:judgement :outer-task-selection :chosen :id])))
        (is (= record (get-in result [:judgement :selected-target-reading-request])))
        (is (nil? (selected/verify
                   (get-in result [:judgement :outer-task-selection :policy
                                   :selected-outer-cascade-policy]))))))
