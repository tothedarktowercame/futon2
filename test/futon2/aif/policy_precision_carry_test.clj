(ns futon2.aif.policy-precision-carry-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.cascade-problems :as problems]
            [futon2.aif.locator-fixtures :as locators]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.report.cascade-habit-read-test :as stores]
            [futon2.aif.cascade-model-manifest :as m]
            [futon2.aif.d-predecessor-task-authority :as task]
            [futon2.aif.d-predecessor-task-authority-test :as task-fixture]
            [futon2.aif.policy :as policy]
            [futon2.aif.trace :as trace]
            [futon2.aif.policy-precision-carry :as carry]
            [futon2.report.cascade-decision-test :as tick]
            [futon2.report.war-machine :as wm]))

(use-fixtures :once hermetic/with-hermetic-stores)

(def token ["target" :artifact])
(def rates {token {:false-neg 0 :false-pos 0}})
(def model {:q0 {#{} 1} :rates rates :horizon 2})
(def clock {"target" {:tau {:value 1 :status :declared}}})
(defn policy-action [id theta]
  {:kind :cascade-candidate :id id :target "target"
   :precedence [{:id id :theta theta :guard {:status :interpreted :clauses [{:present #{} :absent #{}}]}
                 :produces #{token}}]})
(def low (policy-action :low 1/2))
(def high (policy-action :high 1/4))
(def candidates [{:id low :g 0.0 :habit 1.0} {:id high :g 1.0 :habit 1.0}])
(def family (carry/family {:action low :selection-certificate {:candidates candidates}} model clock))
(def admission {:status :admitted :record-sha256 "record" :occurrence {:action/value low}
                :present #{token} :absent #{} :unknown #{}})
(defn advance [prior a f]
  (carry/advance {:previous prior :initialized-beta 1 :model-id (:model-id family)
                  :admission a :family f}))

(deftest partial-event-corresponds-to-full-enumeration
  ;; Seven tokens: the measured production carrier size; six unmeasured here.
  (let [universe (set (range 7))
        rs (zipmap universe (repeat {:false-neg 1/5 :false-pos 1/10}))
        state #{0 2 4}]
    (doseq [event [{:present #{0} :absent #{}}
                  {:present #{1 2} :absent #{4}}
                  {:present #{} :absent #{0 1}}]]
      (let [expected (reduce + 0 (for [[obs p] (m/observation-distribution rs state)
                                     :when (and (set/subset? (:present event) obs)
                                                (empty? (set/intersection (:absent event) obs)))] p))]
        (is (= expected (carry/event-probability rs state event)))))))

(deftest retrospective-clock-and-unknowns
  (let [f (carry/retrospective-f family (assoc admission :tau 1))]
    (is (= 1/2 (get-in f [low :probability])))
    (is (= 1/4 (get-in f [high :probability])))
    (is (not= f (carry/retrospective-f family (assoc admission :tau 2)))))
  (is (thrown? clojure.lang.ExceptionInfo (carry/retrospective-f family (assoc admission :tau 3))))
  (is (thrown? clojure.lang.ExceptionInfo
               (carry/retrospective-f family {:tau 1 :present #{} :absent #{}}))))

(deftest exactly-once-restart-tamper-and-holds
  (let [learned (advance nil admission family)
        reread (edn/read-string (pr-str learned))
        replay (advance reread admission family)
        reordered (carry/seal (assoc (dissoc family :sha256) :candidates (vec (reverse candidates))))]
    (is (= :updated (:status learned)))
    (is (< (:beta learned) 1))
    (is (= :learned (:beta-status learned)))
    (is (= :precision-observation-already-consumed (:reason replay)))
    (is (= (:beta learned) (:beta replay)))
    (is (= (:beta learned) (:beta (advance nil admission reordered))))
    (is (thrown? clojure.lang.ExceptionInfo (advance (assoc reread :beta 99) admission family)))
    (is (= :precision-family-tampered (:reason (advance nil admission (assoc family :selected-action high)))))
    (is (= :precision-selected-action-mismatch
           (:reason (advance nil (assoc-in admission [:occurrence :action/value] high) family))))
    (let [changed (carry/advance {:previous learned :initialized-beta 1 :model-id "changed"})]
      (is (= :precision-model-changed (:reason changed)))
      (is (= (:beta learned) (:beta changed)))
      (is (= :retain-last-valid-rate (get-in changed [:reinitialization :policy])))))
  (is (= :precision-no-admitted-predecessor (:reason (advance nil {:status :refused} nil))))
  (let [f (carry/family {:action low :selection-certificate {:candidates candidates}} model {})]
    (is (= :observation-placement-not-declared
           (:reason (carry/advance {:initialized-beta 1 :model-id (:model-id f)
                                   :admission admission :family f}))))))

(deftest selector-consumes-carried-beta-with-provenance
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory "precision-selection" (make-array java.nio.file.attribute.FileAttribute 0)))
        path (str (io/file dir "habit.edn"))
        ranked (mapv (fn [{:keys [id g]}] {:action id :controller-score g}) candidates)
        state (advance nil admission family)]
    (try
      (let [before (policy/select-action-cascades ranked {:beta 1 :cascade-habit-path path})
            after (policy/select-action-cascades ranked {:beta (:beta state) :beta-state state :cascade-habit-path path})]
        (is (= :carry-beta (get-in after [:selection-law :tau-source])))
        (is (= :learned (get-in after [:beta :status])))
        (is (= (:beta state) (get-in after [:selection-law :tau])))
        (is (= (/ 1.0 (:beta state)) (get-in after [:selection-law :gamma])))
        (is (not= (get-in before [:selection-law :posterior]) (get-in after [:selection-law :posterior]))))
      (is (thrown? clojure.lang.ExceptionInfo
                   (policy/select-action-cascades ranked {:beta 1 :beta-state state :cascade-habit-path path})))
      (finally (doseq [f (reverse (file-seq dir))] (io/delete-file f true))))))

(deftest real-task-producer-binds-frozen-family-through-persistence
  (task-fixture/with-artifact
   {:action (policy-action :executed 1) :observation-schedule (get clock "target")}
   (fn [{:keys [inputs expected jobs root]}]
     (let [selected (get-in inputs [:dispatch :occurrence :action/value])
           menu [{:id selected :g 0.0 :habit 1.0} {:id high :g 1.0 :habit 1.0}]
           frozen (carry/family {:action selected :selection-certificate {:candidates menu}} model clock)
           dispatch (assoc (:dispatch inputs) :precision-family frozen)
           prompt-old (task/prompt-binding (:dispatch inputs))
           prompt-new (task/prompt-binding dispatch)
           bound-jobs (update-vals jobs
                                  #(update % :events (fn [events]
                                                       (mapv (fn [e] (if (:text e)
                                                                       (update e :text str/replace prompt-old prompt-new) e)) events))))
           produced (task/produce! root (assoc inputs :dispatch dispatch) expected bound-jobs)
           admitted (task/read-predecessor root expected bound-jobs)
           state (carry/advance {:initialized-beta 1 :model-id (:model-id frozen)
                                 :admission admitted :family (:precision-family admitted)})]
       (is (= :admitted (get-in produced [:verification :status])))
       (is (= frozen (:precision-family admitted)))
       (is (= :updated (:status state)))
       (is (< (:beta state) 1))
       (is (= :not-established (:candidate-to-minted-join admitted)))
       (is (not= :admitted (:status (task/verify (task/claim (assoc inputs :dispatch dispatch)) expected jobs)))
           "Original job prompts cannot authenticate a retrospectively added family")))))

(deftest default-joint-tick-freezes-and-consumes-initialized-carry
  (stores/with-store
   (fn [path]
     (with-redefs [task/default-root (str path "-missing-task-store")]
       (let [assembled (problems/assemble {:targets [tick/tick-1-target]
                                          :sources (locators/locate-all tick/tick-1-sources)})
             decision (:decision (wm/cascade-decision assembled (assoc tick/live-c-opts :cascade-habit-path path)))
             state (get-in decision [:selection-certificate :policy-precision-state])
             frozen (get-in decision [:selection-certificate :precision-family])]
         (is (= :precision-no-admitted-predecessor (:reason state)))
         (is (= :declared (get-in decision [:beta :status])))
         (is (= (:beta state) (get-in decision [:selection-law :beta])))
         (is (= :carry-beta (get-in decision [:selection-law :tau-source])))
         (is (carry/intact? frozen))
         (is (map? (get-in frozen [:model :rates])))
         (is (map? (get-in frozen [:model :q0])))
         (is (= (:model-id state) (:model-id frozen)))
         (is (= (:action decision) (:selected-action frozen))))))))

(deftest abstention-does-not-drop-a-learned-rate
  (let [state (advance nil admission family)
        result (wm/cascade-decision {:problems [] :refusals [{:target "target" :kind :no-constructed-candidate}]}
                 {:token-belief-predecessor-trace
                  {:decision {:selection-certificate {:policy-precision-state state}}}})
        retained (get-in result [:decision :selection-certificate :policy-precision-state])]
    (is (= (:beta state) (:beta retained)))
    (is (= (:consumed state) (:consumed retained)))
    (is (= :held (:status retained)))
    (is (= :learned (:beta-status retained)))))

(deftest evidence-boundaries-never-invent-a-successful-update
  (let [state (advance nil admission family)
        changed-record (advance state (assoc admission :record-sha256 "different-record") family)
        zero-model (assoc model :q0 {#{token} 1})
        contradiction-family (carry/family {:action low :selection-certificate {:candidates candidates}} zero-model clock)
        contradiction (carry/advance {:initialized-beta 1 :model-id (:model-id contradiction-family)
                                      :family contradiction-family
                                      :admission (assoc admission :present #{} :absent #{token})})]
    (is (= :precision-observation-already-consumed (:reason changed-record)))
    (is (= :held (:status contradiction)))
    (is (= :no-finite-f-candidates (:reason contradiction)))
    (is (= :model-contradiction (get-in contradiction [:update :solve :finding])))
    (is (== 1 (:beta contradiction)))
    (is (= :no-measured-observation
           (:reason (advance nil (assoc admission :present #{} :absent #{}) family)))))
  (is (thrown? clojure.lang.ExceptionInfo
               (carry/event-probability {token {:false-neg 0 :false-pos 0}
                                         :unknown {:false-neg 0.2 :false-pos 0}}
                                        #{} {:present #{token} :absent #{}})))
  (let [tiny (/ 1 (bigint (.pow (biginteger 10) 400)))
        p (carry/event-probability {token {:false-neg 0 :false-pos tiny}}
                                  #{} {:present #{token} :absent #{}})]
    (is (= tiny p))
    (is (pos? p))))

(deftest actual-trace-write-read-preserves-rate-and-consumption-set
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory "precision-trace" (make-array java.nio.file.attribute.FileAttribute 0)))
        state (advance nil admission family)]
    (try
      (let [path (trace/write-trace! {:decision {:selection-certificate {:policy-precision-state state}}}
                                    :dir (str dir))
            record (edn/read-string (slurp path))
            retained (get-in record [:decision :selection-certificate :policy-precision-state])]
        (is (= state retained))
        (is (= :precision-observation-already-consumed (:reason (advance retained admission family)))))
      (finally (doseq [f (reverse (file-seq dir))] (io/delete-file f true))))))
