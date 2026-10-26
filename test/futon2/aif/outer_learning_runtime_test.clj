(ns futon2.aif.outer-learning-runtime-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.meta-outcome-dataset :as dataset]
            [futon2.aif.outer-learning-manifest :as learning-manifest]
            [futon2.aif.outer-target-belief :as belief]
            [futon2.aif.outer-learning-runtime :as runtime]))

(defn- refusal-details [result]
  (get-in result [:belief :details]))

(deftest load-shadow-distinguishes-exception-stages
  (testing "authority read"
    (let [path "/definitely-absent/r26-learning-authority.edn"
          result (runtime/load-shadow path)]
      (is (= :learning-authority-unavailable
             (get-in result [:belief :reason])))
      (is (= path (:path (refusal-details result))))
      (is (= "java.nio.file.NoSuchFileException"
             (:error-class (refusal-details result))))
      (is (string? (:error-message (refusal-details result))))))
  (testing "manifest read"
    (with-redefs [learning-manifest/read-manifest
                  (fn [] (throw (Exception. "manifest-x")))]
      (let [result (runtime/load-shadow)]
        (is (= :learning-manifest-unreadable
               (get-in result [:belief :reason])))
        (is (= learning-manifest/default-path
               (:path (refusal-details result))))
        (is (= "java.lang.Exception"
               (:error-class (refusal-details result))))
        (is (= "manifest-x" (:error-message (refusal-details result)))))))
  (testing "manifest pin union"
    (let [conflicting-path
          "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-14-6a64c83f-4732-4af1-b286-561f22109667.edn"]
      (with-redefs [learning-manifest/read-manifest
                    (fn [] {:entries [{:path conflicting-path
                                       :sha256 (apply str (repeat 64 "0"))}]})]
        (let [result (runtime/load-shadow)]
          (is (= :learning-manifest-unreadable
                 (get-in result [:belief :reason])))
          (is (= learning-manifest/default-path
                 (:path (refusal-details result))))
          (is (= conflicting-path
                 (:conflicting-path (refusal-details result))))
          (is (= "clojure.lang.ExceptionInfo"
                 (:error-class (refusal-details result))))
          (is (= "Conflicting outer-learning pins"
                 (:error-message (refusal-details result))))))))
  (testing "dataset build"
    (with-redefs [dataset/produce
                  (fn [_] (throw (Exception. "dataset-x")))]
      (let [result (runtime/load-shadow)]
        (is (= :learning-dataset-failed
               (get-in result [:belief :reason])))
        (is (= learning-manifest/default-path
               (:path (refusal-details result))))
        (is (= "java.lang.Exception"
               (:error-class (refusal-details result))))
        (is (= "dataset-x" (:error-message (refusal-details result)))))))
  (testing "belief build"
    (with-redefs [dataset/produce (fn [_] {:status :produced})
                  belief/build
                  (fn [_] (throw (Exception. "belief-x")))]
      (let [result (runtime/load-shadow)]
        (is (= :learning-belief-failed
               (get-in result [:belief :reason])))
        (is (= learning-manifest/default-path
               (:path (refusal-details result))))
        (is (= "java.lang.Exception"
               (:error-class (refusal-details result))))
        (is (= "belief-x" (:error-message (refusal-details result))))))))

(deftest dataset-error-propagates-from-load-shadow
  (with-redefs [dataset/produce (fn [_] (throw (Error. "x")))]
    (is (thrown-with-msg? Error #"^x$" (runtime/load-shadow)))))

(deftest pinned-demonstration-run-remains-in-production-learning-fold
  (let [state (runtime/load-shadow)
        learned (:belief state)
        events (:events learned)
        demonstration
        (some #(when (= "2026-10-14-6a64c83f-4732-4af1-b286-561f22109667"
                        (first (:identity %))) %)
              events)]
    (is (= :live (:mode state)))
    (is (= :produced (:status learned)))
    (is (= (count events) (get-in learned [:counts :rows])))
    (is (= 22 (get-in learned [:counts :applied])))
    (is (= 7 (get-in learned [:counts :unresolved])))
    (is (= :grounded-collateral-progress
           (get-in demonstration [:observation :class])))
    (let [event demonstration]
      (is (= :applied (:update event)))
      (is (= 721840 (get-in event [:row-evidence :elapsed :value])))
      (is (= 4223565 (get-in event [:row-evidence :token-use :value])))
      (is (= {:selected 4 :positive 0 :no-want-effect 4}
             (select-keys (get-in event [:row-evidence :pattern-reinforcement])
                          [:selected :positive :no-want-effect]))))
    (let [failed-run "2026-10-18-f016ed51-62de-426e-88c2-768fa18b00dc"
          event (some #(when (= failed-run (first (:identity %))) %) events)
          intero (some #(when (= failed-run (:run/id %)) %)
                       (:interoceptive-evidence learned))
          rebuilt (:belief (runtime/load-shadow))]
      (is (= :not-applied (:update event)))
      (is (= :apparatus-failure-not-task-evidence
             (get-in event [:observation :reason])))
      (is (= :resume-protocol-violation (get-in intero [:cause :kind])))
      (is (= :cascade-resume (get-in intero [:cause :stage])))
      (is (= (:source-pin learned) (:source-pin rebuilt)))
      (is (= (:targets learned) (:targets rebuilt)))
      (is (= (:interoceptive-evidence learned)
             (:interoceptive-evidence rebuilt))))))
