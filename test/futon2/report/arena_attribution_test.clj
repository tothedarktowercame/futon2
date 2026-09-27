(ns futon2.report.arena-attribution-test
  "Exercise judge's actual attribution bindings, without invoking external tick IO.
   The historical bindings at the read pin establish both the crash and the
   healthy output; no replacement attribution implementation is used."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is]]
            [futon2.aif.belief :as belief]
            [futon2.report.war-machine :as wm]))

(def read-pin "6ee56279ceff666f9d49dd51c43dd132ee1e3632")
(def source-path "scripts/futon2/report/war_machine.clj")

(defn judge-attribution [source]
  ;; Read the contiguous, production let bindings including the real belief
  ;; update and the present-only step entry. Resolve them in judge's namespace.
  (let [start (.indexOf source "                ;; R3d v0.17")
        end (.indexOf source "                micro-trace' " start)
        section (subs source start end)
        bindings (read-string (str "[" section "]"))]
    (assert (some #{'events} bindings))
    (assert (some #{'step-entry} bindings))
    (binding [*ns* (:ns (meta #'wm/judge))]
      (eval (list 'fn ['belief 'aggregated-signed-error 'event-weight]
                  (list 'let (into '[step 0 error-mag 0.5 anneal-factor 1.0
                                     driver-record {} triple-omissions []
                                     triple-refusals [] driver-omissions []
                                     driver-rejections []] bindings)
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
