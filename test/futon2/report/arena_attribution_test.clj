(ns futon2.report.arena-attribution-test
  "Exercise judge's actual attribution bindings, without invoking external tick IO.
   The historical bindings at the read pin establish both the crash and the
   healthy output; no replacement attribution implementation is used."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is]]
            [futon2.aif.belief :as belief]
            [futon2.aif.free-energy :as fe]
            [futon2.report.war-machine :as wm]))

(def read-pin "6ee56279ceff666f9d49dd51c43dd132ee1e3632")
(def source-path "scripts/futon2/report/war_machine.clj")

(defn judge-attribution [source]
  ;; Read the contiguous, production let bindings including the real belief
  ;; update and the present-only step entry. Resolve them in judge's namespace.
  (let [legacy-start (.indexOf source "                ;; R3d v0.17")
        start (if (neg? legacy-start)
                (.indexOf source "            entity-health ")
                legacy-start)
        end (if (neg? legacy-start)
              (.indexOf source "            micro-trace' " start)
              (.indexOf source "                micro-trace' " start))
        section (subs source start end)
        bindings (read-string (str "[" section "]"))]
    (assert (some #{'events} bindings))
    (assert (some #{'step-entry} bindings))
    (binding [*ns* (:ns (meta #'wm/judge))]
      (eval (list 'fn ['belief 'aggregated-signed-error 'event-weight]
                  (list 'let (into '[step 0 error-mag 0.5 anneal-factor 1.0
                                     predictions {} driver-record {} triple-omissions []
                                     triple-refusals [] driver-omissions []
                                     driver-rejections [] excluded #{}] bindings)
                        '{:events events :belief belief' :step-entry step-entry}))))))

(def historical
  (delay
    (let [{:keys [exit out err]} (shell/sh "git" "show" (str read-pin ":" source-path))]
      (assert (zero? exit) err)
      (judge-attribution out))))

(def current
  (delay (judge-attribution (slurp (io/resource "futon2/report/war_machine.clj")))))

(defn point-mass [status]
  (zipmap belief/status-set (map #(if (= status %) 1 0) belief/status-set)))

(def healthy
  (array-map :low (point-mass :spawned) :high (point-mass :strengthened)))

(defn impossible-posterior []
  (let [a (into {} (for [o belief/status-set]
                     [o (zipmap belief/status-set
                                (repeat (if (= o :spawned) 1 0)))]))]
    (belief/update-step a :strengthened 1 (point-mass :spawned))))

(deftest refused-entity-is-omitted-at-the-real-judge-site
  (let [refusal (impossible-posterior)
        population (assoc healthy :refused refusal)]
    (is (= {:status :refused :reason :impossible-observation} refusal))
    (doseq [direction [0.5 -0.5]]
      ;; At the pinned HEAD both branches really throw, before any update.
      (is (thrown? ClassCastException (@historical population direction 0.1)))
      (let [{:keys [events belief step-entry]} (@current population direction 0.1)]
        (is (= #{:low :high} (set (map :entity-id events))))
        (is (< (Math/abs (- 0.2 (reduce + (map :weight events)))) 1.0e-12))
        (is (= {:refused :impossible-observation}
               (:entity-attribution-omitted step-entry)))
        (is (= 2 (:events-applied step-entry)))
        (is (= refusal (:refused belief)))))))

(deftest healthy-path-is-identical-to-pinned-head
  (doseq [population [healthy
                      (into {} (for [i (range 14)]
                                 [(str "entity-" i) (belief/uniform-prior)]))]
          direction [0.5 -0.5] weight [0.0 0.1]]
    (let [before (@historical population direction weight)
          after (@current population direction weight)]
      (is (= before after))
      (is (= (pr-str before) (pr-str after)))
      (is (not (contains? (:step-entry after) :entity-attribution-omitted))))))

(deftest all-refused-means-no-events-and-no-substituted-belief
  (let [refusal (impossible-posterior)
        population {:a refusal :b refusal}]
    (doseq [direction [0.5 -0.5] weight [0.0 0.1]]
      (let [{:keys [events belief step-entry]} (@current population direction weight)]
        (is (empty? events))
        (is (= population belief))
        (is (zero? (:events-applied step-entry)))
        (is (= {:a :impossible-observation :b :impossible-observation}
               (:entity-attribution-omitted step-entry)))))))

(deftest refused-channel-does-not-suppress-a-valid-channel-at-judge-site
  (let [source (slurp (io/resource "futon2/report/war_machine.clj"))
        start (.indexOf source "            raw-errors (into")
        end (.indexOf source "            prec-state' " start)
        bindings (read-string (str "[" (subs source start end) "]"))
        read-errors (binding [*ns* (:ns (meta #'wm/judge))]
                      (eval (list 'fn ['triples]
                                  (list 'let (into '[excluded #{}] bindings) 'raw-errors))))
        refused (impossible-posterior)
        prediction (belief/predict-mission-health {:valid (belief/uniform-prior)
                                                   :refused refused})
        present (fe/compute-prediction-error 0.9 prediction)
        triples {:mission-health present
                 :annotation-health (fe/compute-prediction-error 0.5 refused)}
        errors (read-errors triples)
        driver (belief/r3d-aggregate-driver errors)
        updated (@current {:valid (belief/uniform-prior) :refused refused}
                          (:driver driver) 0.1)]
    (is (= :refused (get-in triples [:annotation-health :status])))
    (is (= #{:mission-health} (set (keys errors))))
    (is (= :present (:status driver)))
    (is (pos? (:driver driver)))
    (is (not= (belief/uniform-prior) (get-in updated [:belief :valid])))
    (is (= refused (get-in updated [:belief :refused])))))

(defn- loop-fixture [exclude-channels]
  (let [errors (into {}
                     (for [ch belief/channels-with-likelihood]
                       [ch {:status :present :error 0.0 :observed 0.5 :precision 1.0}]))
        errors (assoc errors
                      :support-coverage {:status :present :error 0.8 :observed 0.8 :precision 1.0}
                      :sorry-count-norm {:status :present :error -0.4 :observed 0.4 :precision 1.0})]
    (with-redefs [belief/predict-observation (fn [& _] {})
                  fe/channel-prediction-error (fn [_ ch _] (get errors ch))]
      (wm/r3-inner-loop
       (cond-> {:initial-belief {:proxy (belief/uniform-prior)}
                :initial-precision-state {}
                :observation {}
                :entity-tags {}
                :prediction-context {}
                :max-steps 1
                :error-eps 1.0e-3}
         (some? exclude-channels) (assoc :exclude-channels exclude-channels))))))

(deftest extracted-r3-loop-default-and-channel-exclusion
  (let [implicit (loop-fixture nil)
        explicit (loop-fixture #{})
        excluded (loop-fixture #{:support-coverage})]
    (is (= implicit explicit) "the default is exactly the explicit empty exclusion")
    (is (not= (:belief explicit) (:belief excluded)) "support drives the fixture's sign")
    (is (contains? (:prediction-errors explicit) :support-coverage))
    (is (not (contains? (:prediction-errors excluded) :support-coverage)))
    (is (= [:support-coverage]
           (get-in excluded [:micro-step-trace 0 :excluded-channels])))
    (is (not (contains? (first (:micro-step-trace excluded)) :prediction-triple-omitted))
        "exclusion is not observation absence")))

(deftest excluding-all-r3-channels-applies-no-event
  (let [initial {:proxy (belief/uniform-prior)}
        result (loop-fixture belief/channels-with-likelihood)]
    (is (= initial (:belief result)))
    (is (empty? (:prediction-errors result)))
    (is (zero? (get-in result [:micro-step-trace 0 :events-applied])))
    (is (= :no-channel-supplied
           (get-in result [:micro-step-trace 0 :aggregated-driver-unknown])))))
