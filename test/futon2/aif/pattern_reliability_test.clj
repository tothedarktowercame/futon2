(ns futon2.aif.pattern-reliability-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.edn :as edn]
            [futon2.aif.pattern-reliability :as pr]))

(def data-path
  "/home/joe/code/p4ng/wm-walkthroughs/build-loop/closure/DATA-cascade-outcomes-2026-09-17.edn")

;; Tiny inline fixture exercising each rule: family derivation (including
;; TRACE → :R), the three outcome kinds, and typed refusals.
(def fixture
  {:cascades
   [{:target "R5" :nodes [{:id "R5-1" :pattern "p/a" :outcome :realised}
                          {:id "R5-2" :pattern "p/a" :outcome :realised}
                          {:id "R5-3" :pattern "p/b" :outcome :contradicted}]}
    {:target "TRACE-7" :nodes [{:id "T-1" :pattern "p/b" :outcome :realised}]}
    {:target "WM-03" :nodes [{:id "W-1" :pattern "p/a" :outcome :unknown}
                             {:id "W-2" :pattern "p/c" :outcome :unknown}]}
    {:target "E01" :nodes [{:id "E-1" :pattern "p/c" :outcome :unknown}]}]})

(deftest seed-counts-rules
  (let [c (pr/seed-counts fixture)]
    ;; :realised counts as realised, per [context pattern]
    (is (= {:realised 2} (select-keys (get c [:R "p/a"]) [:realised])))
    ;; :contradicted counts as not realised; TRACE belongs to :R
    (is (= {:realised 1 :not-realised 1} (select-keys (get c [:R "p/b"]) [:realised :not-realised])))
    ;; :unknown is unobserved, never failure; families separate the same pattern
    (is (= {:unobserved 1} (select-keys (get c [:WM "p/a"]) [:unobserved])))
    (is (nil? (get c [:WM "p/b"])))
    (is (= {:unobserved 1} (select-keys (get c [:E "p/c"]) [:unobserved])))
    ;; the unmet-not-failed note exists and names WM-10-C6
    (is (re-find #"WM-10-C6" pr/not-realised-note))
    ;; typed refusals: no silent defaults
    (is (= :invalid-outcome-data (:kind (pr/seed-counts {}))))
    (is (= :invalid-target (:kind (pr/seed-counts {:cascades [{:target "?" :nodes []}]}))))
    (is (= :invalid-node-outcome
           (:kind (pr/seed-counts {:cascades [{:target "R1" :nodes [{:id "x" :pattern "p" :outcome :wrong}]}]}))))))

(deftest theta-rules
  (let [c (pr/seed-counts fixture)]
    ;; Beta(1 + r, 1 + n) mean, exact rational, bias stated
    (is (= {:theta 3/4
            :basis {:realised 2 :not-realised 0 :unobserved 0}
            :bias :observed-only-when-worked}
           (pr/theta {[:R "p"] {:realised 2 :not-realised 0 :unobserved 0}} :R "p")))
    ;; r = 1 (TRACE), n = 1 → (1+1)/(2+2) = 1/2, exact
    (is (= 1/2 (:theta (pr/theta c :R "p/b"))))
    ;; no observations (only unobserved): prior-only 1/2
    (is (= {:theta 1/2 :basis :prior-only :bias :observed-only-when-worked}
           (pr/theta c :WM "p/c")))
    ;; never 1
    (is (= 10/11 (:theta (pr/theta {[:R "p"] {:realised 9 :not-realised 0 :unobserved 0}} :R "p"))))
    ;; unknown pair is a typed refusal
    (is (= :unknown-pattern-context (:kind (pr/theta c :E "p/a"))))))

(deftest attest-patterns-rules
  (let [c (pr/seed-counts fixture)
        attested (pr/attest-patterns [{:id "p/a" :produces #{"x"}} {:id "p/b" :produces #{}}] c :R)]
    (is (= [3/4 1/2] (mapv :theta attested)))
    (is (every? #(= :attested-observation (:theta-source %)) attested))
    (is (= {:realised 2 :not-realised 0 :unobserved 0} (:theta-basis (first attested))))
    ;; a pattern with no entry for the context is a typed refusal, no default
    (is (= :unknown-pattern-context (:kind (pr/attest-patterns [{:id "p/zz"}] c :R))))))

(deftest real-data-totals
  ;; The real data file: 46 cascades, 233 steps — 39 realised,
  ;; 1 not-realised (WM-10-C6, unmet), 193 unobserved.
  (let [c (pr/seed-counts (edn/read-string (slurp data-path)))
        total (fn [k] (reduce + (map k (vals c))))]
    (is (= 39 (total :realised)))
    (is (= 1 (total :not-realised)))
    (is (= 193 (total :unobserved)))
    ;; the single not-realised pair is WM-10-C6's pattern in the WM family
    (is (= ["wm-connected-learning/observation-to-next-choice"]
           (for [[[_ctx pat] m] c :when (pos? (:not-realised m 0))] pat)))
    ;; theta stays an exact rational on real data, never 1
    (is (every? #(and (ratio? %) (< 0 % 1))
                (map :theta (keep #(pr/theta c :WM %)
                                  ["wm-pattern-design/pattern-before-operation"]))))))
