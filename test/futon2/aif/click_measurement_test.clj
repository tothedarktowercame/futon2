(ns futon2.aif.click-measurement-test
  "M-wm-wiring row 6: admitted labels reach sourced-rates from the lane, and
  the ranked certificates (what the click record carries from R5) carry
  :measurement per token. None are admitted today: every token reads
  :absent and the numbers are unchanged. One fixture label set (not a real
  class-level admission) shows a class measured and the others absent."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.policy-prefix-admission :as prefix-admission]
            [futon2.aif.prefix-free-energy-sut-test :as prefix-fixture]
            [futon2.aif.wm.cascade-decision :as wm-cd]))

(defn- locators [token-classes]
  (into {} (map (fn [[t class]] [t {:class class :repo "futon2" :sha "fixture"
                                     :path (str "fixture/" (pr-str t))}]))
        token-classes))

;; the WIRE-5 test's minimal complete lane problem, with :t/wanted located by
;; a C4 check and the two facts by C3 checks
(def problem
  {:target "click-measurement-fixture"
   :facts {:t/observed true :t/other false}
   :want [:t/wanted]
   :interpretations {:p/appears {:guard {:needs #{:t/observed} :forbids #{}}
                                 :produces #{:t/wanted}}}
   :repository {:patterns #{:p/appears} :stands-on #{}}
   :precedences [[:p/appears]]
   :horizon-steps 2
   :cascade-spec {:want #{:t/wanted}}
   :beta 1
   :locators (locators {:t/observed :C3 :t/other :C3 :t/wanted :C4})})

(defn- commissioning []
  (let [candidates (mapv #(assoc % :target (:target problem))
                         (:candidates (wm-cd/cascade-lane problem {:through :R5})))
        context (prefix-fixture/authority-fixture)
        pairs (mapv (comp prefix-fixture/prior prefix-admission/candidate-key)
                    candidates)]
    {:candidates candidates
     :opts {:prefix-f-commissioning
            {:bootstrap-priors
             (into {} (map (fn [candidate [prior _]]
                             [(prefix-admission/candidate-key candidate) prior])
                           candidates pairs))
             :scorer-authority "scorer"
             :context (reduce (fn [ctx [_ job]]
                                (assoc-in ctx [:authority-jobs (:job-id job)] job))
                              context pairs)}}}))

(defn- measured-lane
  ([] (measured-lane {}))
  ([opts]
   (let [{commissioning-opts :opts} (commissioning)]
     (wm-cd/cascade-lane problem (merge commissioning-opts opts)))))

(defn- provenance [lane] (get-in (first (:ranked lane)) [:certificate :rates-provenance]))

(defn- numbers
  "What the rates decide: each ranked candidate's id and G, and the rates the
  certificate records."
  [lane]
  (mapv (fn [e] [(:cascade-id e) (:G-efe e) (get-in e [:certificate :rates])]) (:ranked lane)))

(deftest none-admitted-every-token-absent
  (let [lane (measured-lane)
        p (provenance lane)]
    (is (nil? (:stopped-at lane)) (pr-str (:refusal lane)))
    (is (= {:t/observed :absent :t/other :absent :t/wanted :absent} (:measurement p)))
    (is (= :none-admitted (:labels p)))))

(deftest the-numbers-are-unchanged
  ;; fixture: (numbers lane) from war_machine.clj at futon2 b321efab, before
  ;; row 6, captured on a prepended classpath before the change
  (is (= (edn/read-string (slurp "test/fixtures/click-measurement/numbers-before@futon2-b321efab.edn"))
         (pr-str (numbers (measured-lane))))))

(deftest one-admitted-class-is-measured-the-others-absent
  ;; fixture labels for class C4 only: one admitted-present recorded true
  ;; and one admitted-absent recorded false, so both cells are measured
  ;; (0/1 each); C3 has none and keeps the unmeasured zero kernel
  (let [labels [{:token-class :C4 :recorded true :admitted :present}
                {:token-class :C4 :recorded false :admitted :absent}]
        lane (measured-lane {:observation-labels {:labels labels :subjects {:C4 2}}})
        p (provenance lane)]
    (is (nil? (:stopped-at lane)) (pr-str (:refusal lane)))
    (is (= :absent (get-in p [:measurement :t/observed])))
    (is (= :absent (get-in p [:measurement :t/other])))
    (is (map? (get-in p [:measurement :t/wanted])) "the C4 token carries its counts")
    (is (= {:admitted 2} (:labels p)))))

(deftest prefix-f-authority-refusals-remain-fail-closed
  (let [{:keys [opts]} (commissioning)
        missing (wm-cd/cascade-lane problem)
        policy (first (keys (get-in opts [:prefix-f-commissioning :bootstrap-priors])))
        wrong-key (-> opts
                      (update-in [:prefix-f-commissioning :bootstrap-priors]
                                 #(assoc (dissoc % policy) [:wrong-policy-key] (get % policy))))
        substituted (assoc-in opts
                              [:prefix-f-commissioning :context
                               :executable-registry :sut-evaluator :executable]
                              (fn [_] -999.0))]
    (is (= :free-energy-not-supplied (get-in missing [:refusal :data :refusal :kind])))
    (is (= :unseen-without-bootstrap
           (get-in (wm-cd/cascade-lane problem wrong-key)
                   [:refusal :data :prefix-f-cold-start/refusal])))
    (is (= :executable-registration-mismatch
           (get-in (wm-cd/cascade-lane problem substituted)
                   [:refusal :data :prefix-f-cold-start/refusal])))))
