(ns futon2.aif.click-measurement-test
  "M-wm-wiring row 6: admitted labels reach sourced-rates from the lane, and
  the ranked certificates (what the click record carries from R5) carry
  :measurement per token. None are admitted today: every token reads
  :absent and the numbers are unchanged. One fixture label set (not a real
  class-level admission) shows a class measured and the others absent."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.report.war-machine :as wm]))

(defn- locators [token-classes]
  (into {} (map (fn [[t class]] [t {:class class :repo "futon2" :sha "fixture"
                                     :path (str "fixture/" (pr-str t))}]))
        token-classes))

;; the WIRE-5 test's minimal complete lane problem, with :t/wanted located by
;; a C4 check and the two facts by C3 checks
(def problem
  {:facts {:t/observed true :t/other false}
   :want [:t/wanted]
   :interpretations {:p/appears {:guard {:needs #{:t/observed} :forbids #{}}
                                 :produces #{:t/wanted}}}
   :repository {:patterns #{:p/appears} :stands-on #{}}
   :precedences [[:p/appears]]
   :horizon-steps 2
   :cascade-spec {:want #{:t/wanted}}
   :beta 1
   :locators (locators {:t/observed :C3 :t/other :C3 :t/wanted :C4})})

(defn- provenance [lane] (get-in (first (:ranked lane)) [:certificate :rates-provenance]))

(defn- numbers
  "What the rates decide: each ranked candidate's id and G, and the rates the
  certificate records."
  [lane]
  (mapv (fn [e] [(:cascade-id e) (:G-efe e) (get-in e [:certificate :rates])]) (:ranked lane)))

(deftest none-admitted-every-token-absent
  (let [lane (wm/cascade-lane problem)
        p (provenance lane)]
    (is (nil? (:stopped-at lane)) (pr-str (:refusal lane)))
    (is (= {:t/observed :absent :t/other :absent :t/wanted :absent} (:measurement p)))
    (is (= :none-admitted (:labels p)))))

(deftest the-numbers-are-unchanged
  ;; fixture: (numbers lane) from war_machine.clj at futon2 b321efab, before
  ;; row 6, captured on a prepended classpath before the change
  (is (= (edn/read-string (slurp "test/fixtures/click-measurement/numbers-before@futon2-b321efab.edn"))
         (pr-str (numbers (wm/cascade-lane problem))))))

(deftest one-admitted-class-is-measured-the-others-absent
  ;; fixture labels for class C4 only: one admitted-present recorded true
  ;; and one admitted-absent recorded false, so both cells are measured
  ;; (0/1 each); C3 has none and keeps the unmeasured zero kernel
  (let [labels [{:token-class :C4 :recorded true :admitted :present}
                {:token-class :C4 :recorded false :admitted :absent}]
        lane (wm/cascade-lane problem {:observation-labels {:labels labels :subjects {:C4 2}}})
        p (provenance lane)]
    (is (nil? (:stopped-at lane)) (pr-str (:refusal lane)))
    (is (= :absent (get-in p [:measurement :t/observed])))
    (is (= :absent (get-in p [:measurement :t/other])))
    (is (map? (get-in p [:measurement :t/wanted])) "the C4 token carries its counts")
    (is (= {:admitted 2} (:labels p)))))
