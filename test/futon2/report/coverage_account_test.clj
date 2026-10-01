(ns futon2.report.coverage-account-test
  (:require [futon2.aif.focus-receipt]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.live-c :as live-c]
            [futon2.aif.locator-fixtures :as locators]
            [futon2.aif.mission-hole-wants :as holes]
            [futon2.aif.trace :as trace]
            [futon2.aif.wm.construction-inputs :as construction-inputs]
            [futon2.report.war-machine :as wm] [futon2.aif.wm.cascade-decision :as wm-cd])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def ^:dynamic *root* nil)
(use-fixtures :each
  (fn [f]
    (let [root (.toFile (Files/createTempDirectory "coverage-account-" (make-array FileAttribute 0)))]
      (try (binding [*root* root] (f))
           (finally (doseq [file (reverse (file-seq root))] (io/delete-file file true)))))))

(def schedule {:placement {:value :terminal :status :declared}
               :elsewhere {:value :uniform-over-non-ruled-zero :status :declared}})
(def scales {:lam {:value 1 :status :declared} :mu {:value 0 :status :declared}})
(def missions
  [{:id "M-a" :path "/fixture/futon2/M-a.md" :status-class :active
    :text "# M-a\n\n- [ ] do A\n"
    :open-holes [{:id "M-a#1" :kind :unchecked-task :text "- [ ] do A"}]}
   {:id "M-b" :path "/fixture/futon2/M-b.md" :status-class :active
    :text "# M-b\n\n- [ ] do B\n\nTODO: explain\n"
    :open-holes [{:id "M-b#2" :kind :unchecked-task :text "- [ ] do B"}
                 {:id "M-b#3" :kind :work-marker :text "TODO: explain"}]}])
(def declared
  (locators/locate-all
   {:universes {"M-a" {:x false :y false :z false}}
    :wants {"M-a" [:x :y :z]}
    :interpretations {"M-a" {:patterns {:p {:guard {:needs #{} :forbids #{}} :produces #{:x}}}
                            :receipts {:p {:source :fixture}}}}
    :candidates {"M-a" [{:precedence [:p] :construction-receipt {:source :fixture}}]}
    :preference-schedules {"M-a" schedule} :preference-scales {"M-a" scales}
    :horizon-steps 2 :beta-by-context {:WM {:beta 1}} :context-of (constantly :WM)}))
(def live-sources
  {:wholeness {:value {:missions [{:mission "M-a" :class :alive :L 2 :T 1 :H 2}]}}
   :missions [{:mission "M-a" :text "**Status:** ACTIVE"}
              {:mission "M-b" :text "**Status:** ACTIVE"}]
   :stars {:value {:capabilities {:capability {:status :held}}}}})

(defn file-count [root] (count (filter #(.isFile %) (file-seq (io/file root)))))
(defn focus-inputs []
  (update (futon2.aif.focus-receipt/read-inputs) :relations into
          (for [target ["M-a" "M-b"]]
            {:target target :facet "WM" :relation "focus"
             :source {:repo "fixture" :commit "0" :path "test" :section "fixture"}
             :effective-from "2026-01-01T00:00:00Z"})))
(defn assembled-decision []
  (let [sources (holes/merge-into-sources declared "/fixture" missions :WM)
        assembled (construction-inputs/assemble-cascade-problems {:targets ["M-a" "M-b"] :sources sources})
        ;; The synthetic targets have no relation in the real focus corpus, and
        ;; an unresolved relation gets no scalar G (futon2 84f81cb42): the
        ;; decision refuses :class-unknown-no-scalar-g. The fixture supplies a
        ;; sourced relation for its own targets, as cascade_decision_test does.
        result (wm-cd/cascade-decision assembled
                                   {:focus-inputs (focus-inputs)
                                    :live-c {:sources live-sources}
                                    :cascade-habit-path (str (io/file *root* "habit.edn"))})]
    {:sources sources :result result}))

(deftest source-merge-assembly-trace-retains-counts-with-units
  (let [before (file-count "data")
        {:keys [sources result]} (assembled-decision)
        record (trace/trace-record {:decision (:decision result)
                                    :cascade-problems (:cascade-problems result)})
        coverage (:mission-hole-coverage record)
        live (:live-c-coverage record)]
    (is (= before (file-count "data")) "source merge and assembly never write real stores")
    (is (= 0 (file-count *root*)) "pure assembly/selection does not learn habit")
    (is (= (:mission-hole-coverage sources) coverage))
    (is (= 3 (:holes-retained coverage)))
    (is (= {:unchecked-task 2 :work-marker 1} (:retained-by-kind coverage)))
    (is (= {:unchecked-task 2} (:projected-by-kind coverage)))
    (is (= 2 (:holes-projected coverage)))
    (is (= {:work-marker 1} (:not-projected-by-kind coverage)))
    (is (= 2 (:targets-added coverage)))
    (is (= ["M-a"] (:declared-targets-refreshed coverage)))
    (is (= {:status :absent :reason :no-admitted-cascade-problems} live)
        "a stale declaration cannot score against the current mission wants")
    (is (some #(and (= "M-a" (:target %)) (= :universe-not-admitted (:kind %)))
              (get-in record [:cascade-problems :refusals])))
    ;; Exercise the serializer and durable trace path only under the temp dir.
    (let [path (trace/write-trace! {:decision (:decision result)
                                   :cascade-problems (:cascade-problems result)}
                                  :dir (.getPath *root*))
          saved (first (trace/read-trace :dir (.getPath *root*)))]
      (is (some? path))
      (is (= coverage (:mission-hole-coverage saved)))
      (is (= live (:live-c-coverage saved)))
      (is (= before (file-count "data"))))))

(deftest run-record-persists-the-same-tick-accounts
  (let [{:keys [result]} (assembled-decision)
        decision (:decision result)
        saved (#'runner/persist-run-record!
               {:run-record-dir (.getPath *root*) :scan-render-fn (fn [& _] nil)}
               "coverage-test" "2026-09-21T00:00:00Z"
               {:outcome :incomplete :data {:failure-stage :construction}
                :checkpoints {:selection {:judgment {:controller-decision decision}}}})
        record (edn/read-string (slurp (:run-record saved)))]
    (is (map? (:mission-hole-coverage record)))
    (is (= (:mission-hole-coverage decision) (:mission-hole-coverage record)))
    (is (map? (:live-c-coverage record)))
    (is (= (:live-c-coverage decision) (:live-c-coverage record)))))

(deftest projection-versus-domain-and-no-overlap-are-distinct
  (let [derived (live-c/derive-live-c live-sources)
        want #{["M-a" :x] ["M-a" :y] ["M-a" :z]}
        partial (live-c/cascade-spec derived #{["M-a" :x]} want)
        none (live-c/cascade-spec derived #{[:other :token]} want)]
    (is (= 3 (get-in partial [:live-c-coverage :projected-outcome-tokens :count])))
    (is (= 1 (get-in partial [:live-c-coverage :in-domain-outcome-tokens :count])))
    (is (= 2 (get-in partial [:live-c-coverage :source-tokens :reached])))
    (is (= :no-reachable-want (get-in none [:refusal :kind])))
    (is (= 3 (get-in none [:live-c-coverage :projected-outcome-tokens :count])))
    (is (= 0 (get-in none [:live-c-coverage :in-domain-outcome-tokens :count])))
    (is (= 4 (get-in none [:live-c-coverage :source-tokens :unreached])))))

(deftest abstention-retains-hole-census-and-types-uncomputed-projection
  (let [sources (holes/merge-into-sources declared "/fixture" missions :WM)
        assembled (construction-inputs/assemble-cascade-problems {:targets ["M-b"] :sources sources})
        result (wm-cd/cascade-decision assembled {})
        record (trace/trace-record {:decision (:decision result)})]
    (is (= :abstained (get-in record [:decision :status])))
    (is (= (:mission-hole-coverage sources) (:mission-hole-coverage record)))
    (is (= {:status :absent :reason :no-admitted-cascade-problems} (:live-c-coverage record)))))

(deftest changed-candidate-admission-explains-a-fresh-coverage-drop
  (let [sources (holes/merge-into-sources declared "/fixture" missions :WM)
        token (first (get-in sources [:wants "M-b"]))
        supplied (-> sources
                     (assoc-in [:interpretations "M-b"]
                               {:patterns {:p {:guard {:needs #{} :forbids #{}} :produces #{token}}}
                                :receipts {:p {:source :fixture}}})
                     (assoc-in [:candidates "M-b"]
                               [{:precedence [:p] :construction-receipt {:source :fixture}}]))
        omitted (assoc-in supplied [:candidates "M-b"] [])
        decide (fn [s] (wm-cd/cascade-decision
                        (construction-inputs/assemble-cascade-problems {:targets ["M-a" "M-b"] :sources s})
                        {:focus-inputs (focus-inputs)
                         :live-c {:sources live-sources}
                         :cascade-habit-path (str (io/file *root* "habit.edn"))}))
        before (decide supplied) after (decide omitted)
        old (get-in before [:decision :live-c-coverage])
        new (get-in after [:decision :live-c-coverage])]
    (is (= (get-in before [:decision :mission-hole-coverage])
           (get-in after [:decision :mission-hole-coverage])))
    (is (= 1 (get-in old [:source-tokens :reached])))
    (is (= 1 (get-in old [:in-domain-outcome-tokens :count])))
    (is (= {:status :absent :reason :no-admitted-cascade-problems} new))
    (is (some #(and (= "M-b" (:target %)) (= :no-constructed-candidate (:kind %)))
              (get-in after [:cascade-problems :refusals])))))
