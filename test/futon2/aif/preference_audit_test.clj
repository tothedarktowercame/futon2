(ns futon2.aif.preference-audit-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.preference-audit :as audit]
            [futon2.aif.cascade-model-manifest :as model]
            [futon2.aif.scoring-input-receipts :as receipts]
            [futon2.aif.run-narrative :as narrative]))

(defn frozen [suffix]
  (edn/read-string (slurp (str "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-09-21-" suffix ".edn"))))

(defn selected-decision [record]
  ;; These two historical fixtures selected the per-policy winner. Production
  ;; audit never infers selection from argmax; newer records retain the binding.
  (assoc (:decision record) :action (get-in record [:decision :selection-law :per-policy-argmax :action])))

(deftest frozen-models-audit-without-changing-any-score-or-posterior
  (doseq [[suffix live all spread fallback-count]
          [["1789964661" 78/19483 19951/116898 0.00130598185312556 1]
           ["1789952479" 3143/58449 87367/779320 0.003130934660986501 7]]]
    (let [record (frozen suffix) d (selected-decision record)
          result (receipts/with-preference-audit d)
          a (get-in result [:selection-certificate :preference-audit])
          c (get-in d [:g-term-decomposition :policies 0 :terms :C :value])
          terms (get-in d [:g-term-decomposition :policies 0 :terms])
          terminal (:distribution (last (:steps c)))
          replay (fn [candidate]
                   (model/horizon-g-sparse
                    {:rates (get-in terms [:A :value]) :q0 (get-in terms [:D :value])
                     :precedence-fn (constantly (:precedence (:id candidate)))
                     :horizon (count (:steps c)) :universe (:universe terminal)
                     :spec {:want (set (keys (:weights terminal))) :weights (:weights terminal)
                            :lam 1 :mu 0 :zeroed #{} :evidence #{} :c-schedule (:schedule c)}}))]
      (is (= :recorded (:status a)))
      (is (= [live live all] ((juxt :after-source-projection :retained-live :all-consumed) (:utility-totals a))))
      (is (= 1 (get-in a [:source-budget :before-projection :value])))
      (is (= 468 (get-in a [:source-budget :count])))
      (is (= :source-entry (get-in a [:source-budget :unit])))
      (is (< (Math/abs (- spread (:G-spread a))) 1e-10))
      (is (= fallback-count (count (get-in a [:fallbacks :rows]))))
      (is (= :matched (get-in a [:weight-accounting :status])))
      (is (= (pr-str (:candidates (:selection-certificate d)))
             (pr-str (:candidates (:selection-certificate result)))))
      (is (= (pr-str (:selection-law d)) (pr-str (:selection-law result))))
      (is (= d (update result :selection-certificate dissoc :preference-audit)))
      (doseq [candidate (get-in d [:selection-certificate :candidates])]
        (is (< (Math/abs (- (:g candidate) (replay candidate))) 1e-8)))
      (doseq [[{:keys [distribution]} normalizer] (map vector (:steps c) (:normalizers a))]
        ;; Empty outcome is allowed in these fixtures, so -log C(empty)=log Z
        ;; independently checks the complete scorer normalizer, not Q's support.
        (is (< (Math/abs (+ (:log-z normalizer) ((model/member-log-probability distribution) #{}))) 1e-12)))
      (is (audit/valid? result (edn/read-string (pr-str a))))
      (doseq [bad [(assoc-in a [:fallbacks :rows] [])
                   (assoc-in a [:source-budget :count] 5)
                   (assoc-in a [:normalizers 1 :log-z] 0)]]
        (is (not (audit/valid? result bad)))
        (is (some #{:preference-audit-mismatch}
                  (:errors (receipts/validate-record
                            (assoc record :decision (assoc-in result [:selection-certificate :preference-audit] bad))))))))))

(deftest selected-target-odds-and-fallback-are-named
  (let [d (selected-decision (frozen "1789964661")) a (audit/build d)
        rows (get-in a [:selected-target-odds :rows])
        rendered (narrative/preference-audit-text (audit/attach d))]
    (is (= 3 (count rows)))
    (is (every? #(= 229/175347 (:log-odds %)) rows))
    (is (= {:token ["M-wm-08-external-f2" :route-a-rehearsal-reported]
            :origin :unnamed-want-lam-over-want-count :lam 1 :want-count 6
            :weight 1/6 :consumed-weight 1/6}
           (first (get-in a [:fallbacks :rows]))))
    (is (re-find #"1.001307 : 1" rendered))
    (is (re-find #"global-deduplicated-live-source-inventory, 468 source entries" rendered))
    (is (re-find #":hole/h6378c65a4012" rendered))))

(deftest missing-meaning-is-held-not-inferred
  (let [d (selected-decision (frozen "1789964661"))]
    (is (= :selected-target-wants-not-retained
           (get-in (audit/build (dissoc d :action)) [:selected-target-odds :reason])))
    (is (every? #(= :token-meaning-locator-not-retained (:reason %))
                (get-in (audit/build (update d :action dissoc :observation-locators))
                        [:selected-target-odds :rows])))
    (is (= :shared-consumed-preference-not-retained (:reason (audit/build {}))))))

(deftest ruled-zero-needs-a-context-for-odds-but-not-for-full-normalizer
  (let [d (selected-decision (frozen "1789964661"))
        d (update-in d [:selection-certificate :g-term-decomposition :policies]
                     (fn [policies]
                       (mapv #(update-in % [:terms :C :value :steps]
                                        (fn [steps] (mapv (fn [s] (assoc-in s [:distribution :zeroed] #{#{}})) steps)))
                             policies)))
        a (audit/build d)
        member (get-in d [:selection-certificate :g-term-decomposition :policies 0 :terms :C :value :steps 1 :distribution])
        allowed (:universe member)
        expected (- (reduce + (vals (:weights member))) ((model/member-log-probability member) allowed))]
    (is (every? #(= :other-token-context-required-for-ruled-zeros (:reason %))
                (get-in a [:selected-target-odds :rows])))
    (is (= 1 (get-in a [:normalizers 1 :excluded-outcome-count])))
    (is (< (Math/abs (- expected (get-in a [:normalizers 1 :log-z]))) 1e-12))))
