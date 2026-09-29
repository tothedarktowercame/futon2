(ns futon2.aif.scan-learn-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.scan-learn :as scan-learn]
            [futon2.aif.trace :as trace])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- close? [x y]
  (< (Math/abs (- (double x) (double y))) 1.0e-12))

(deftest outcomes-have-fixed-alphabets
  (is (= {:family :binomial :alphabet [:success :failure] :counts [3 2]}
         (scan-learn/outcome :support {:covered 3 :claims 5})))
  (is (= [3 2 1 0]
         (:counts (scan-learn/outcome
                    :workstream-commits
                    {:counts {:stack 3 :consulting 2 :portfolio 1 :mathematics 0}
                     :total 6}))))
  (is (= 1 (last (:counts (scan-learn/outcome :annotation
                                               {:anomalies 12 :sections 10})))))
  (is (= 1 (last (:counts (scan-learn/outcome :sorrys {:count 15})))))
  (is (nil? (scan-learn/outcome :support {:status :absent :reason :no-claims})))
  (is (nil? (scan-learn/outcome :support {:status :refused :reason :bad}))))

(deftest one-tick-binomial-update-matches-worked-calculation
  ;; q^-=(1/2,1/2), P(1/1|a)=1/2, P(1/1|b)=2/3.
  ;; Z=7/12, q=(3/7,4/7), then alpha_a=1+3/7=10/7 and
  ;; alpha_b=2+4/7=18/7; both failure concentrations stay 1.
  (let [state {:schema :test :rho 0 :statuses [:a :b]
               :q {:a 1/2 :b 1/2}
               :concentrations {:support {:a [1 1] :b [2 1]}}
               :admitted-run-ids #{}}
        {:keys [state receipt]}
        (scan-learn/step state {:run/id "worked"
                                :scan-exposures {:support {:covered 1 :claims 1}}})]
    (is (close? 3/7 (get-in state [:q :a])))
    (is (close? 4/7 (get-in state [:q :b])))
    (is (close? 10/7 (get-in state [:concentrations :support :a 0])))
    (is (close? 1 (get-in state [:concentrations :support :a 1])))
    (is (close? 18/7 (get-in state [:concentrations :support :b 0])))
    (is (close? 1 (get-in state [:concentrations :support :b 1])))
    (is (close? (Math/log (/ 7.0 12.0)) (:log-evidence receipt)))))

(deftest exchangeable-priors-stay-exchangeable
  (let [initial (scan-learn/prior-state)
        ss (:statuses initial)
        uniform (update initial :concentrations
                        (fn [by-key]
                          (into {}
                                (for [[key rows] by-key
                                      :let [cell (get rows (first ss))]]
                                  [key (zipmap ss (repeat cell))]))))
        ticks [{:run/id "s1" :scan-exposures
                {:support {:covered 5 :claims 5}
                 :attack {:covered 1 :claims 4}}}
               {:run/id "s2" :scan-exposures
                {:active-repos {:active 2 :repositories 3}
                 :sorrys {:count 12}}}]
        final (reduce (fn [state tick] (:state (scan-learn/step state tick)))
                      uniform ticks)]
    (is (apply = (vals (:q final))))
    (doseq [[_ rows] (:concentrations final)]
      (is (apply = (vals rows))))))

(deftest informative-prior-breaks-status-exchangeability
  (let [initial (scan-learn/prior-state)
        {:keys [state]}
        (scan-learn/step initial
                         {:run/id "informative"
                          :scan-exposures {:support {:covered 5 :claims 5}
                                           :attack {:covered 4 :claims 4}}})
        healthy (+ (get-in state [:q :strengthened])
                   (get-in state [:q :addressed]))]
    (is (> healthy (/ 2.0 7.0)))
    (is (not (apply = (vals (:q state)))))))

(deftest duplicate-run-is-a-no-op
  (let [tick {:run/id "once" :scan-exposures {:support {:covered 2 :claims 3}}}
        first-result (scan-learn/step (scan-learn/prior-state) tick)
        second-result (scan-learn/step (:state first-result) tick)]
    (is (= (:state first-result) (:state second-result)))
    (is (= {:status :duplicate :run/id "once"} (:receipt second-result)))))

(deftest absent-refused-and-poisonous-production-beliefs-are-ignored
  (let [initial (scan-learn/prior-state)
        tick {:run/id "absent"
              :mu-pre {:poison :must-not-be-read}
              :mu-post {:poison :must-not-be-read}
              :scan-exposures {:support {:status :absent :reason :no-claims}
                               :attack {:status :refused :reason :malformed}}}
        {:keys [state receipt]} (scan-learn/step initial tick)]
    (is (= :no-observed-channel (:status receipt)))
    (is (= [] (:channels-used receipt)))
    (is (= (:q-prior receipt) (:q receipt) (:q state)))
    (is (= (:q initial) (:q state))
        "production mu-pre/mu-post cannot supply learner responsibilities")
    (is (= (:concentrations initial) (:concentrations state)))
    (is (some #{:support} (:channels-absent receipt)))
    (is (some #{:attack} (:channels-absent receipt)))))

(deftest beta-binomial-uses-integrated-predictive
  ;; Under Beta(1,1), P(X=2 | n=2) = C(2,2) B(3,1)/B(1,1) = 1/3.
  ;; A plug-in mean would instead give (1/2)^2=1/4.
  (let [state {:schema :test :rho 0 :statuses [:only]
               :q {:only 1}
               :concentrations {:support {:only [1 1]}}
               :admitted-run-ids #{}}
        {:keys [receipt]}
        (scan-learn/step state {:run/id "predictive"
                                :scan-exposures {:support {:covered 2 :claims 2}}})]
    (is (close? (/ 1.0 3.0) (Math/exp (:log-evidence receipt))))
    (is (not (close? 0.25 (Math/exp (:log-evidence receipt)))))))

(deftest initial-state-records-authority-and-exact-priors
  (let [state (scan-learn/prior-state)]
    (is (= {:schema :wm/scan-learn-v1
            :delta 1/10 :kappa 2 :rho 1/20
            :authority "PROOF-2a decisions 6B-2a, 6B-7 (2026-09-29)"}
           (select-keys state [:schema :delta :kappa :rho :authority])))
    (is (= [2/7 2/7 2/7 2/7 2/7 2/7 2/7]
           (get-in state [:concentrations :loop-health :spawned])))
    (is (= [19/10 1/10]
           (get-in state [:concentrations :support :strengthened])))
    (is (= [1/10 19/10]
           (get-in state [:concentrations :support :spawned])))))

(def record-a
  {:run/id "a" :scan-exposures {:support {:covered 5 :claims 5}}})

(def record-b
  {:run/id "b" :scan-exposures {:support {:covered 0 :claims 5}}})

(deftest fold-equals-explicit-steps
  (let [first-step (scan-learn/step (scan-learn/prior-state) record-a)
        second-step (scan-learn/step (:state first-step) record-b)
        folded (scan-learn/fold [record-a record-b])]
    (is (= (:q (:state second-step)) (get-in folded [:state :q])))
    (is (= (:concentrations (:state second-step))
           (get-in folded [:state :concentrations])))
    (is (= [(:receipt first-step) (:receipt second-step)] (:receipts folded)))
    (is (= 2 (:admitted folded)))))

(deftest fold-preserves-given-order
  (let [ab (scan-learn/fold [record-a record-b])
        ba (scan-learn/fold [record-b record-a])]
    (is (not= (get-in ab [:state :q]) (get-in ba [:state :q]))
        "online prediction and learning make the supplied order observable")
    (is (= ["a" "b"] (mapv :run/id (:receipts ab))))
    (is (= ["b" "a"] (mapv :run/id (:receipts ba))))))

(deftest fold-skips-pre-carrier-and-bootstrap-records
  (let [records [{:run/id "old" :observation {}}
                 {:record/kind :accumulation-bootstrap}
                 record-a]
        folded (scan-learn/fold records)
        expected (scan-learn/fold [record-a])]
    (is (= (:state expected) (:state folded)))
    (is (= 1 (:admitted folded)))
    (is (= [{:index 0 :run/id "old" :reason :no-scan-exposures}
            {:index 1 :run/id nil :reason :no-scan-exposures}]
           (:skipped folded)))
    (is (= 1 (count (:receipts folded))))))

(deftest fold-keeps-duplicate-receipt
  (let [folded (scan-learn/fold [record-a record-a])]
    (is (= 1 (:admitted folded)))
    (is (= 2 (count (:receipts folded))))
    (is (= {:status :duplicate :run/id "a"}
           (second (:receipts folded))))))

(deftest fold-skips-carrier-record-without-run-id
  (let [folded (scan-learn/fold [{:scan-exposures {:support {:covered 1 :claims 1}}}])]
    (is (= 0 (:admitted folded)))
    (is (= [{:index 0 :run/id nil :reason :run-id-missing}]
           (:skipped folded)))
    (is (empty? (:receipts folded)))))

(deftest fold-trace-dir-uses-strict-history
  (let [dir (.toFile (Files/createTempDirectory
                       "scan-learn-fold" (make-array FileAttribute 0)))]
    (try
      (doseq [[day record] [["2026-09-28" record-a]
                            ["2026-09-29" record-b]]]
        (trace/write-trace! record :dir (str dir) :date-str day))
      (let [disk-records (:records (trace/read-history-strict
                                     Long/MAX_VALUE :dir (str dir)))]
        (is (= (scan-learn/fold disk-records)
               (scan-learn/fold-trace-dir (str dir)))))
      (let [missing (io/file dir "missing")]
        (is (= {:status :absent :reason :trace-dir-missing :path (str missing)}
               (scan-learn/fold-trace-dir (str missing)))))
      (finally
        (doseq [file (reverse (file-seq dir))]
          (io/delete-file file true))))))
