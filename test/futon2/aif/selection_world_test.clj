(ns futon2.aif.selection-world-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.selection-world :as world]
            [wm-run-facts :as facts]))

(def tasks {:missions ["M-a"] :excursions ["E-a"] :tickets ["T-a"]})
(def decision {:controller-ranking
               [{:action {:type :advance-mission :target "M-a"}}
                {:action {:type :advance-excursion :target "E-a"}}
                {:action {:type :advance-ticket :target "T-a"}}]})
(def opts {:world-task-sets-fn (fn [_] tasks)
           :world-pattern-manifest-fn (fn [_] {:file-count 2 :digest "patterns"})
           :world-now-fn (constantly "2026-09-30T00:00:00Z")})

(deftest snapshot-is-complete-and-deterministic
  (let [a (world/capture decision {:author {:type :codex}} nil opts)
        b (world/capture decision {:author {:type :codex}} nil opts)
        changed (world/capture decision {:author {:type :codex}} nil
                               (assoc opts :world-task-sets-fn
                                      (fn [_] (update tasks :missions conj "M-b"))))]
    (is (= 0 (:failure-count a)))
    (is (= 3 (get-in a [:enumerated-tasks :count])))
    (is (= (:selection-input-digest a) (:selection-input-digest b)))
    (is (not= (:selection-input-digest a) (:selection-input-digest changed)))))

(deftest snapshot-does-not-change-decision
  (let [before (pr-str decision)]
    (world/capture decision {} nil opts)
    (is (= before (pr-str decision)))))

(deftest production-cascade-candidates-are-the-live-enumeration-carrier
  (let [cascade-decision
        {:selection-certificate
         {:candidates [{:f-prefix
                        {:policy {:kind :cascade-candidate :id :C1
                                  :target "M-live"}}}]}}
        x (world/capture cascade-decision {} nil opts)]
    (is (= ["M-live"] (get-in x [:enumerated-tasks :ids])))
    (is (= 1 (get-in x [:enumerated-tasks :count]))
        "a selected live policy can no longer be reported beside zero enumerated tasks")))

(deftest outer-selection-is-compared-with-independent-registry-census
  (let [outer {:schema :wm/outer-task-selection-v1
               :support [{:id "M-live" :kind :mission}]
               :excluded [{:id "E-held" :kind :excursion
                           :ineligible-reason :ownership/actively-held}
                          {:id "T-off-map" :kind :ticket
                           :ineligible-reason :pipeline/not-on-current-map}]}
        ;; Decision (claude-11, 2026-10-29): Q1 compares the selector's
        ;; enumeration with an independent registry census, because a check
        ;; whose two sides come from one source cannot fail.
        census {:missions ["M-live" "M-unseen"]
                :excursions ["E-held"]
                :tickets ["T-off-map"]}
        x (world/capture {:selection-certificate
                          {:candidates [{:target "M-live"}]}}
                         {} nil outer
                         (assoc opts :world-task-sets-fn (constantly census)))
        exported (:facts (facts/facts-for-record
                          {:world-at-selection x}
                          "fixture.edn"
                          {:patterns #{} :seats #{}}))]
    (is (= ["M-live" "E-held" "T-off-map"]
           (mapv :id (concat (:support outer) (:excluded outer)))))
    (is (= #{"M-live" "E-held" "T-off-map"}
           (set (get-in x [:enumerated-tasks :ids]))))
    (is (= ["M-live" "M-unseen"] (get-in x [:open-tasks :missions :ids])))
    (is (= ["E-held"] (get-in x [:open-tasks :excursions :ids])))
    (is (= ["T-off-map"] (get-in x [:open-tasks :tickets :ids])))
    (is (not= (set (get-in x [:enumerated-tasks :ids]))
              (set (mapcat :ids (vals (:open-tasks x))))))
    (is (= 4 (get-in x [:q1-diagnostic :open-task-count])))
    (is (= 3 (get-in x [:q1-diagnostic :enumerated-task-count])))
    (is (= {:count 1 :ids ["M-unseen"] :truncated? false}
           (get-in x [:q1-diagnostic :open-tasks-missing-from-outer-population])))
    (is (not= (set (exported "enumeratedTasks"))
              (set (concat (exported "openMissions")
                           (exported "openExcursions")
                           (exported "openTickets")))))))

(deftest independent-census-can-equal-outer-enumeration
  (let [outer {:schema :wm/outer-task-selection-v1
               :support [{:id "M-a" :kind :mission}]
               :excluded [{:id "E-a" :kind :excursion}
                          {:id "T-a" :kind :ticket}]}
        x (world/capture decision {} nil outer opts)]
    (is (= (set (get-in x [:enumerated-tasks :ids]))
           (set (mapcat :ids (vals (:open-tasks x))))))
    (is (= {:count 0 :ids [] :truncated? false}
           (get-in x [:q1-diagnostic :open-tasks-missing-from-outer-population])))))

(deftest live-outer-selection-carries-the-whole-enumerated-population
  (let [excluded (mapv (fn [n] {:target (str "E-held-" n)
                                :reason :inactive})
                       (range 26))
        outer {:cascade-scores [1.0 2.0 3.0]
               :outer-field/admitted ["M-live" "T-ready"]
               :outer-field/excluded excluded
               :selection {:selected {:target "M-live"}}}
        census {:missions ["M-live"]
                :excursions (mapv :target excluded)
                :tickets ["T-ready"]}
        snapshot (world/capture {:selection-certificate
                                 {:candidates [{:target "M-live"}]}}
                                {} nil outer
                                (assoc opts :world-task-sets-fn
                                       (constantly census)))
        exported (:facts (facts/facts-for-record
                          {:world-at-selection snapshot}
                          "fixture.edn"
                          {:patterns #{} :seats #{}}))]
    (is (= 3 (count (:cascade-scores outer))))
    (is (= 26 (count (:outer-field/excluded outer))))
    (is (= (set (concat (:outer-field/admitted outer)
                        (map :target excluded)))
           (set (get-in snapshot [:enumerated-tasks :ids]))))
    (is (= (set (exported "enumeratedTasks"))
           (set (concat (exported "openMissions")
                        (exported "openExcursions")
                        (exported "openTickets")))))))

(deftest malformed-outer-population-is-retained-as-a-failure
  (let [x (world/capture decision {} nil
                         {:schema :wm/outer-task-selection-v1
                          :support [{:id "M-a" :kind :unknown}]
                          :excluded []}
                         opts)]
    (is (= 1 (:failure-count x)))
    (is (= :outer-task-population (get-in x [:failures 0 :part])))))

(deftest failed-roster-is-counted
  (let [x (world/capture decision nil nil opts)]
    (is (= 1 (:failure-count x)))
    (is (= :seat-roster (get-in x [:failures 0 :part])))
    (is (= :failed (get-in x [:seat-roster :status])))))

(deftest wrong-enumerator-is-detectable
  (let [decision-without-excursion
        (update decision :controller-ranking
                (fn [ranking]
                  (remove #(= :advance-excursion (get-in % [:action :type]))
                          ranking)))
        x (world/capture decision-without-excursion {} nil opts)]
    ;; Plant: the decision enumerator has no excursion candidate. Exact
    ;; set comparison exposes the mismatch rather than accepting equal counts.
    (is (not= (set (mapcat :ids (vals (:open-tasks x))))
              (set (get-in x [:enumerated-tasks :ids]))))
    (is (not= #{"M-wrong"}
              (set (get-in x [:enumerated-tasks :ids]))))))

(deftest critical-task-counts-reuse-selection-world-or-label-terminal-fallback
  (let [snapshot (world/capture decision {} nil opts)
        selected (world/critical-task-counts snapshot opts)
        fallback (world/critical-task-counts
                  {:schema :wm/world-at-selection-v1 :failure-count 1}
                  opts)]
    (is (= {:missions 1 :excursions 1 :tickets 1}
           (:available-to-choose selected)))
    (is (= :selection-world (:basis selected)))
    (is (= "2026-09-30T00:00:00Z" (:observed-at selected)))
    (is (= {:missions 1 :excursions 1 :tickets 1}
           (:available-to-choose fallback)))
    (is (= :terminal-fallback (:basis fallback)))
    (is (= "2026-09-30T00:00:00Z" (:observed-at fallback)))))
