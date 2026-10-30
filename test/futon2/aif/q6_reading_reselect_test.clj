(ns futon2.aif.q6-reading-reselect-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.outer-state :as state]
            [futon2.aif.outer-select :as outer]
            [futon2.aif.previous-run :as previous]
            [futon2.aif.selected-target-reading :as reading]
            [futon2.report.war-machine :as wm]
            [futon2.test-support.runner-fixture :as fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic]))
(use-fixtures :once fixture/with-hermetic-traces hermetic/with-hermetic-stores)
(defn retained []
  (edn/read-string (slurp (io/resource "fixtures/q6-reselection/preview.edn"))))
(defn setup []
  (let [{:keys [observation state relation-classes] :as data} (retained)
        observation (assoc observation :relation (fn [_ t] (get relation-classes t)))
        makeup (state/load-makeup)
        selection (outer/select makeup state observation)
        carrier (atom {:task {:id (get-in selection [:selection :selected :target])}
                       :outer/selection selection})
        persisted (atom [])
        port (runtime/reading-reselection-port makeup state observation selection carrier
                                                #(swap! persisted conj %))]
    (assoc data :carrier carrier :persisted persisted :port port
           :opts {:task {:id "M-typed-memories"} :reading-reselection/state carrier
                  :selected-target-reading-reselect-fn port})))

(deftest frozen-click51-q6-uses-production-judge-and-existing-reselection
  (let [{:keys [opts decision carrier persisted port]} (setup)
        calls (atom [])
        result (with-redefs [wm/generate-war-machine
                            (fn [_ o]
                              (swap! calls conj (get-in o [:task :id]))
                              (if (= 1 (count @calls))
                                {:judgement {:decision decision}}
                                {:judgement {:task (:task o) :decision {:status :preparation-needed}}}))]
                 (#'runtime/selection-judge opts 7))
        exclusion (last (get-in @carrier [:outer/selection :outer-field/excluded]))]
    (is (= ["M-typed-memories" "M-peradam-grounding"] @calls))
    (is (= "M-peradam-grounding" (get-in result [:judgement :task :id])))
    (is (= {:target "M-typed-memories" :reason :repeat-choice-after-refusal :scope :this-click
            :predecessor-run-id "2026-10-30-e5f08d91-2fb0-40e2-b367-5c4ed436c3dc"
            :digest "c1801780489bea4ed72638a53cca7b1e0d55c0b1d97357c00b9659f41eb6d315"}
           (dissoc exclusion :reader-refusal)))
    (is (= 1 (count @persisted)))
    ;; Q6 and format failure spend the SAME bounded port, not one each.
    (is (nil? (port {:target "M-peradam-grounding" :kind :selected-target-reading-unavailable})))
    (is (= "M-peradam-grounding" (get-in (reading/current-options opts) [:task :id])))))

(deftest changed-digest-does-not-exclude-the-target-on-a-later-click
  (let [{:keys [opts predecessor decision persisted]} (setup)
        digest (get-in decision [:selection-certificate :q6 :input-digest])
        unchanged (#'previous/decide-exclusion predecessor digest)
        changed (#'previous/decide-exclusion predecessor (str digest "-changed"))
        calls (atom [])
        result (runtime/reselect-q6-refusal opts
                 (fn [o]
                   (swap! calls conj (get-in o [:task :id]))
                   {:judgement {:task (:task o)
                                :decision {:selection-certificate {:q6 changed}}}}))]
    (is (= :excluded (:status unchanged)))
    (is (= :selection-inputs-changed (:reason changed)))
    (is (= ["M-typed-memories"] @calls))
    (is (= "M-typed-memories" (get-in result [:judgement :task :id])))
    (is (empty? @persisted))))

(deftest no-reduced-field-keeps-exclusions-and-typed-absence
  (let [{:keys [decision]} (retained)
        carrier (atom {:task {:id "M-typed-memories"}})
        port (runtime/reading-reselection-port nil nil {:field []} {:outer-score/admitted []}
                                               carrier #(throw (ex-info "No pending write" {})))
        result (runtime/reselect-q6-refusal
                {:reading-reselection/state carrier :selected-target-reading-reselect-fn port}
                (fn [_] {:judgement {:decision decision}}))]
    (is (= :no-task-selected (:absent result)))
    (is (= :no-task-selected (get-in result [:judgement :decision :kind])))
    (is (= :repeat-choice-after-refusal
           (get-in result [:outer/selection :outer-field/excluded 0 :reason])))))
