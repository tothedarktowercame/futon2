(ns futon2.aif.abstention-digest-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.abstention-digest :as digest]))

(deftest groups-abstentions-by-target-with-explicit-absence
  (let [rows [{:run-id "2026-11-03-r1" :target "M-b"
               :abstention {:target "M-b" :reason :missing-source
                            :missing [:source] :source :selection}}
              {:run-id "2026-11-05-r2" :target "M-a"
               :abstention {:target "M-a" :reason :not-retained}}
              {:run-id "2026-11-01-r0" :target "M-a"
               :abstention {:target "M-a" :reason :missing-source
                            :missing [:source :pin] :source :gate}}
              {:run-id "2026-11-04-r3" :target "M-a"
               :abstention {:target "M-a" :reason :missing-source
                            :missing [:source] :source :judge}}
              {:run-id "2026-11-02-r4"
               :abstention {:reason :not-retained}}
              {:run-id "2026-11-05-success" :target "M-a"
               :failure {:kind :grounded-change}}]
        result (digest/by-target rows)]
    (is (= ["M-a" :no-target "M-b"] (mapv :target result))
        "count descending, then target's printed identity")
    (is (= {:target "M-a" :count 3
            :first-at "2026-11-01" :last-at "2026-11-05"
            :reasons {:missing-source 2 :not-retained 1}
            :missing #{:source :pin}
            :latest-run-id "2026-11-05-r2"}
           (first result)))
    (is (= {:target :no-target :count 1
            :first-at "2026-11-02" :last-at "2026-11-02"
            :reasons {:not-retained 1} :missing #{}
            :latest-run-id "2026-11-02-r4"}
           (second result)))
    (is (= 3 (count result))
        "a non-abstention row without :abstention is ignored")))

(deftest renders-a-compact-owner-facing-table
  (let [text (digest/render-text
              [{:target "M-a" :count 2 :first-at "2026-11-01"
                :last-at "2026-11-05" :reasons {:not-retained 2}
                :missing #{} :latest-run-id "2026-11-05-r2"}])]
    (is (re-find #"M-a" text))
    (is (re-find #"not-retained=2" text))
    (is (re-find #"2026-11-05-r2" text))))

(deftest legacy-abstention-rows-without-a-carrier-are-counted-not-dropped
  ;; Every abstention in the real index on 2026-11-05 predates the carrier.
  (let [rows [{:run-id "2026-10-19-a" :target "A-self-heal"
               :failure {:kind :abstained :stage :selection}}
              {:run-id "2026-09-28-b" :failure {:kind :no-selection}}
              {:run-id "2026-10-20-c" :target "M-x"
               :failure {:kind :guardrail-refusal}}]]
    (is (= {"A-self-heal" {:not-indexed 1} :no-target {:not-indexed 1}}
           (into {} (map (juxt :target :reasons)) (digest/by-target rows))))))
