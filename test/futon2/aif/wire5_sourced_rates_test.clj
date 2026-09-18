(ns futon2.aif.wire5-sourced-rates-test
  "WIRE-5 acceptance: the tick's cascade decision SOURCES its adjudication
  rates from futon2.aif.observation-rates when the problem carries
  :locators, instead of defaulting the scorer to the identity kernel.

  The honest numbers finding is recorded here once and for all: with NO
  admitted judgement labels (the WM-04 pilot was never reviewed; there is
  no RESULT.edn), an all-checkable universe sources the EXACT ZERO kernel
  from tokenLikelihood_checkable — numerically the identity kernel. So the
  before/after G on a concrete case is EXPECTED byte-identical; what
  changes is provenance (:sourced vs :identity-default) on the family meta
  and on every candidate's certificate. A judgement-class token with no
  admitted rate refuses typed (:unsupported-class) and is never padded."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.observation-rates :as rates]
            [futon2.report.war-machine :as wm]))

(def contract
  (edn/read-string (slurp (io/resource "wm/observation-contract.edn"))))

(defn- locators [tokens class]
  (into {} (map (fn [t] [t {:class class :repo "futon2" :sha "fixture"
                            :path (str "fixture/" (pr-str t))}]))
        tokens))

;; A minimal, complete cascade-lane problem (R1..R9 all run): one true fact,
;; one want, one pattern whose guard needs nothing and produces the want
;; token, one candidate precedence, T=2, β=1.
(defn- problem [& {:as overrides}]
  (let [base {:facts {:t/observed true :t/other false}
              :want [:t/wanted]
              :interpretations
              {:p/appears
               {:guard {:needs #{:t/observed} :forbids #{}}
                :produces #{:t/wanted}}}
              :repository {:patterns #{:p/appears} :stands-on #{}}
              :precedences [[:p/appears]]
              :horizon-steps 2
              :cascade-spec {:want #{:t/wanted}}
              :beta 1}]
    (merge base overrides)))

(def problem-tokens
  "Every token the scoring universe can reach for the problem above."
  #{:t/observed :t/other :t/wanted})

(deftest sourced-rates-exact-zero-kernel-for-checkable-classes
  (let [r (rates/sourced-rates nil nil nil (locators problem-tokens :C3) contract)]
    (is (= :sourced (:status r)))
    (is (= :futon2.aif.observation-rates/sourced-rates (:source r)))
    (is (= problem-tokens (set (keys (:rates r))))
        "every located token gets a rate — coverage N=M for checkable classes")
    (is (every? #(= {:false-neg 0 :false-pos 0} %) (vals (:rates r)))
        "tokenLikelihood_checkable: mechanical checks are exact, so the kernel is exactly zero")
    (is (every? #(= :checkable %) (vals (:basis r))))))

(deftest sourced-rates-refuse-judgement-class-without-admitted-data
  (let [r (rates/sourced-rates nil nil nil (locators problem-tokens :J) contract)]
    (is (= :missing (:status r)))
    (is (= :unsupported-class (:kind r)))
    (is (= :J (:class r)))
    (is (contains? r :token) "the refusal names the first unsupported token"))
  (testing "unlocated tokens are absent, not invented"
    (let [r (rates/sourced-rates nil nil nil (locators #{:t/observed} :C3) contract)]
      (is (= #{:t/observed} (set (keys (:rates r))))))))

(defn- g-of
  "First ranked candidate's G for the problem, running the REAL cascade-lane."
  [p]
  (let [lane (wm/cascade-lane p)]
    (is (nil? (:stopped-at lane)) (str "lane stopped: " (:refusal lane)))
    (mapv (juxt :cascade-id :G-efe) (:ranked lane))))

(deftest lane-sources-rates-when-the-problem-carries-locators
  (let [before (g-of (problem))                                 ; no :locators
        after (g-of (problem :locators (locators problem-tokens :C3)))]
    (testing "before/after G on one concrete case: byte-identical numbers"
      ;; The finding, not a failure: with no admitted judgement labels the
      ;; sourced kernel IS the exact-zero kernel, so G cannot move yet.
      (is (= before after)
          "sourced checkable kernel = identity kernel numerically"))
    (let [sourced (wm/cascade-lane (problem :locators (locators problem-tokens :C3)))
          scoring (:cascade-scoring (meta (:ranked sourced)))
          cert (get-in (first (:ranked sourced)) [:certificate :rates-provenance])]
      (testing "family meta says SOURCED, with the producer named"
        (is (= :sourced-adjudication-rates (:rates scoring)))
        (is (= :futon2.aif.observation-rates/sourced-rates
               (:source (:rates-provenance scoring))))
        (is (= :none-admitted (get-in (:rates-provenance scoring) [:labels]))))
      (testing "every candidate's certificate distinguishes sourced from default"
        (is (= :futon2.aif.observation-rates/sourced-rates (:source cert)))
        (is (every? #(= :checkable %) (vals (:basis cert)))))
      (let [default (wm/cascade-lane (problem))
            default-cert (get-in (first (:ranked default))
                                 [:certificate :rates-provenance])]
        (is (= {:status :identity-default} default-cert)
            "a problem without locators still records the identity default")))))

(deftest lane-stops-at-r5-when-a-judgement-class-has-no-admitted-rate
  (let [lane (wm/cascade-lane (problem :locators (locators problem-tokens :J)))]
    (is (= :R5 (:stopped-at lane)))
    (is (= :unsupported-class (get-in lane [:refusal :kind])))
    (is (= :J (get-in lane [:refusal :class])))
    (is (= [:R1 :R6 :R13 :R4 :R5] (mapv :node (:route lane)))
        "the refusal is the record: the lane stops and names the gap, it never pads")))
