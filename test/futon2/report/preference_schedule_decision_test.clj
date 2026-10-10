(ns futon2.report.preference-schedule-decision-test
  "CTAU-REC-I: record the consumed token and class schedules, without scoring changes."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.cascade-model-manifest :as manifest]
            [futon2.aif.cascade-problems :as cp]
            [futon2.aif.live-c :as live-c]
            [futon2.aif.locator-fixtures :as locfix]
            [futon2.report.cascade-decision-test :as fixture]
            [futon2.aif.wm.cascade-decision :as wm-cd]))

(defn- assembled [declaration]
  (let [target (or (:target declaration) fixture/tick-1-target)
        sources (into {}
                      (map (fn [[k v]]
                             [k (if (and (map? v) (contains? v fixture/tick-1-target))
                                  (-> v
                                      (dissoc fixture/tick-1-target)
                                      (assoc target (get v fixture/tick-1-target)))
                                  v)]))
                      fixture/tick-1-sources)
        sources (cond-> (assoc sources :horizon-steps 2)
                  declaration
                  (assoc :preference-schedules {target (live-c/preference-schedule declaration)}
                         :preference-scales {target (live-c/preference-scales declaration)}))]
    (cp/assemble {:targets [target] :sources (locfix/locate-all sources)})))

(defn- declared-source []
  (edn/read-string
   (slurp (io/resource "wm/cascade-sources/M-f11-find-production-successor.edn"))))

(defn- problem [assembled]
  (get-in assembled [:problems 0 :cascade-problem]))

(defn- placement-matches-consumption?
  "Compare the record's claimed family with real preference-member at every tau.
   This uses nonzero utility and T=2, so terminal and every-step differ."
  [record spec universe horizon]
  (let [claimed (assoc spec :c-schedule
                       {:placement {:value (:placement record)}
                        :elsewhere {:value :uniform-over-non-ruled-zero}})]
    (every? (fn [tau]
              (= (manifest/preference-member spec universe horizon tau)
                 (manifest/preference-member claimed universe horizon tau)))
            (range 1 (inc horizon)))))

(deftest q9-characterization-the-declared-c-has-one-nonstrict-completion-pair
  (let [c wm-cd/class-preference-weights
        closing [:focused :related :unrelated]
        non-closing :stop-the-line
        pairs (mapv (fn [ending]
                      {:ending ending
                       :strict? (> (get c ending) (get c non-closing))})
                    closing)]
    (is (= 1 (reduce + (vals c))) "C is normalized")
    (is (= 3 (count pairs)))
    (is (= 2 (count (filter :strict? pairs))))
    (is (= [{:ending :unrelated :strict? false}]
           (vec (remove :strict? pairs))))))

(deftest token-placement-and-scales-are-the-consumed-values
  (doseq [[declaration placement source]
          [[(declared-source) :terminal [:declared (:target (declared-source))]]
           [nil :every-step :defaulted]]]
    (testing (str source)
      (let [p (problem (assembled declaration))
            lane (wm-cd/cascade-lane p)
            spec (get-in (meta (:ranked lane)) [:cascade-scoring :spec])
            record (get-in lane [:decision :preference-schedule])
            universe (get-in (meta (:ranked lane)) [:cascade-scoring :universe])]
        (is (nil? (:stopped-at lane)) (pr-str (:refusal lane)))
        (is (= {:schema :wm/preference-schedule-v1 :family :token
                :placement placement :source source :weights-ruling :none-found
                :lam (:lam spec) :mu (:mu spec)} record))
        (is (placement-matches-consumption? record spec universe 2))
        (is (not (placement-matches-consumption?
                  (assoc record :placement (if (= placement :terminal) :every-step :terminal))
                  spec universe 2))
            "BAD CASE: a contradictory placement fails against the real member")))))

(deftest missing-schedule-does-not-introduce-a-scorer-default
  (let [p (update (problem (assembled nil)) :cascade-spec dissoc :c-schedule)
        lane (wm-cd/cascade-lane p)
        spec (get-in (meta (:ranked lane)) [:cascade-scoring :spec])
        record (get-in lane [:decision :preference-schedule])]
    (is (not (contains? spec :c-schedule)))
    (is (= [:every-step :defaulted] ((juxt :placement :source) record)))
    (is (placement-matches-consumption? record spec (set (:want spec)) 2))))

(deftest scales-come-from-scored-spec-not-default-definitions
  (let [p (update (problem (assembled nil)) :cascade-spec assoc :lam 7/3 :mu 2/5)
        lane (wm-cd/cascade-lane p)]
    (is (= [7/3 2/5]
           ((juxt :lam :mu) (get-in lane [:decision :preference-schedule]))))))

(deftest joint-record-carries-class-and-retains-lane-token-receipt
  (let [result (wm-cd/cascade-decision (assembled nil) fixture/live-c-opts)
        record (get-in result [:decision :preference-schedule])]
    (is (= :class (:family record)))
    (is (= :terminal (:placement record)))
    (is (= wm-cd/class-preference-weights (:weights record)))
    (is (= "scripts/futon2/report/war_machine.clj:class-preference-weights" (:site record)))
    (is (= [:token :every-step :defaulted]
           ((juxt :family :placement :source)
            (get-in result [:lanes 0 :decision :preference-schedule]))))))

(deftest writes-do-not-change-scores
  (doseq [declaration [(declared-source) nil]]
    (let [p (problem (assembled declaration))
          with-write (wm-cd/cascade-lane p)
          without-write (with-redefs [wm-cd/token-preference-schedule (constantly nil)]
                          (wm-cd/cascade-lane p))]
      (is (some? (get-in with-write [:decision :preference-schedule])))
      (is (nil? (get-in without-write [:decision :preference-schedule])))
      (is (= (pr-str (:ranked with-write)) (pr-str (:ranked without-write))))
      (is (= (pr-str (dissoc (:decision with-write) :preference-schedule))
             (pr-str (dissoc (:decision without-write) :preference-schedule))))))
  (let [a (assembled nil)
        with-write (:decision (wm-cd/cascade-decision a fixture/live-c-opts))
        without-write (with-redefs [wm-cd/token-preference-schedule (constantly nil)
                                   wm-cd/class-preference-schedule (constantly nil)]
                        (:decision (wm-cd/cascade-decision a fixture/live-c-opts)))
        score #(pr-str (select-keys % [:action :softmax-weights :selection-law]))]
    (is (some? (:preference-schedule with-write)))
    (is (nil? (:preference-schedule without-write)))
    (is (= (score with-write) (score without-write)))))
