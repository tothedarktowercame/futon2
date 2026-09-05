(ns futon2.aif.realized-outcome-test
  "The one outcome vocabulary (worklist :U59).

   FIXTURE RULE (futon2/AGENTS.md): at least one fixture looks like the data.
   `july-record` is a verbatim `:realized-outcome` map from
   data/wm-trace/wm-trace-2026-07-02.edn, `july-nil-leg` is another one from
   the same file with the nil leg 11 of the 88 carry, and `observation` is the
   record `wm_step_observe.bb` wrote into
   runs/2026-09-05-u59-b/observation/. A test written only against maps the
   test author invented is how three key vocabularies for one quantity survived
   unnoticed for two months."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.realized-outcome :as ro]
            [futon2.aif.selection-gain :as sg]))

(def july-record
  {:policy "M-bayesian-structure-learning" :expected-G -0.2 :realized-G -0.2 :tick 1783000318687})

(def july-nil-leg
  {:policy "M-canon-fingerprint-store" :expected-G -0.4 :realized-G nil :tick 1782999055753})

(def v1-record
  {:schema :wm/realized-outcome-v1 :policy "M-x" :tick 1783000000000
   :expected-score -0.4 :realized-score -0.1})

(def observation
  {:schema :wm/realized-outcome-v1
   :observation/status :observed
   :observation/observed-for-run "2026-09-05-u59-a"
   :observation/observed-for-tick "feec6327-e0b0-41fc-9697-2fc46bff2830"
   :policy "M-zaif-harness-v1"
   :tick "2026-09-05T05:41:23.657016962Z"
   :scale :g-core
   :expected-score 6.879411221866079
   :realized-score 7.013215250842563
   :outcome :grounded-no-change})

(deftest vocabulary-is-recognised-and-marked
  (testing "the July spelling is read and reported as historical"
    (is (= :july-2026-delta-g (ro/vocabulary july-record)))
    (is (contains? ro/historical-vocabularies (ro/vocabulary july-record)))
    (is (= -0.2 (ro/expected-score july-record)))
    (is (= -0.2 (ro/realized-score july-record))))
  (testing "the v1 spelling is read and is not historical"
    (is (= :v1 (ro/vocabulary v1-record)))
    (is (not (contains? ro/historical-vocabularies (ro/vocabulary v1-record))))
    (is (= -0.4 (ro/expected-score v1-record))))
  (testing "a record with one nil leg is NOT readable -- 11 of the 88 are like this,
            and reading them would feed gamma a leg that was never measured"
    (is (nil? (ro/vocabulary july-nil-leg)))
    (is (not (ro/legs-readable? july-nil-leg)))
    (is (nil? (ro/expected-score july-nil-leg))))
  (testing "neither a non-map nor a map without legs is claimed as any vocabulary"
    (is (nil? (ro/vocabulary nil)))
    (is (nil? (ro/vocabulary "not a map")))
    (is (nil? (ro/vocabulary {:policy "M-x" :tick 1})))))

(deftest normalize-copies-and-does-not-delete
  (let [n (ro/normalize july-record)]
    (is (= -0.2 (:expected-score n)))
    (is (= :july-2026-delta-g (:outcome/read-as n)))
    (is (true? (:outcome/historical? n)))
    (testing "the bytes that were read are still there: a reading that deleted its
              source would make the next reader's disagreement unfindable"
      (is (= -0.2 (:expected-G n)))
      (is (= -0.2 (:realized-G n))))))

(deftest categorical-outcome-reads-the-three-places-and-no-fourth
  (is (= :grounded-change (ro/categorical-outcome {:outcome :grounded-change})))
  (is (= :grounded-change (ro/categorical-outcome {:enactment {:outcome :grounded-change}})))
  (is (= :grounded-change (ro/categorical-outcome {:realized-outcome {:outcome :grounded-change}})))
  (testing "a record carrying only the numeric legs has NO categorical outcome --
            the legs and the categorical are different answers, and inventing one
            from the other is the inference this vocabulary exists to refuse"
    (is (nil? (ro/categorical-outcome {:realized-outcome july-record})))))

(deftest gamma-folds-the-historical-record-and-marks-it
  (testing "the fold the reader could not make before this row"
    (let [s0 (sg/initial-selection-gain-state)
          s1 (sg/fold-realized-outcome s0 july-record)]
      (is (= 1 (:samples s1)))
      (is (= :july-2026-delta-g (:last-outcome-vocabulary s1)))
      (is (= 1783000318687 (:last-outcome-tick s1)))))
  (testing "a v1 record folds and is NOT marked historical, so a replay over the
            July corpus stays distinguishable from a live fold"
    (let [s1 (sg/fold-realized-outcome (sg/initial-selection-gain-state) v1-record)]
      (is (= 1 (:samples s1)))
      (is (nil? (:last-outcome-vocabulary s1)))))
  (testing "the observation the step path writes is the same schema and folds"
    (let [s1 (sg/fold-realized-outcome (sg/initial-selection-gain-state) observation)]
      (is (= 1 (:samples s1)))
      (is (= (:tick observation) (:last-outcome-tick s1)))))
  (testing "a nil leg still holds gamma at the prior"
    (let [s0 (sg/initial-selection-gain-state)]
      (is (= s0 (sg/fold-realized-outcome s0 july-nil-leg)))))
  (testing "the tick dedup is unchanged: the same outcome folds at most once"
    (let [s1 (sg/fold-realized-outcome (sg/initial-selection-gain-state) july-record)]
      (is (= s1 (sg/fold-realized-outcome s1 july-record))))))

(deftest conforms-is-about-v1-and-not-about-readability
  (is (ro/conforms? observation))
  (is (not (ro/conforms? july-record)))
  (is (ro/legs-readable? july-record)))
