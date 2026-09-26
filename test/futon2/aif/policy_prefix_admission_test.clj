(ns futon2.aif.policy-prefix-admission-test
  "F1b-admit-I (PROOF-2a-PLAN <2>2d F1; F1c-D futon3c 8cc2d425 s4-5): each
  candidate policy's admitted observed prefix, from the conditioning steps on
  the flight records the tick reads (enactment-fold-source, the reader the
  habit fold already uses). First-layer wire test through the flight-record
  reader and through the tick's cascade decision; SPEC-F's bad cases assigned
  to this packet."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.enactment-fold-source :as src]
            [futon2.aif.policy-prefix-admission :as adm]
            [futon2.report.cascade-decision-test :as cdt]
            [futon2.report.war-machine :as wm])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def ka [:pattern-cascade "M-a" [:p/a] {}])
(def kb [:pattern-cascade "M-a" [:p/b] {}])

(defn- step [k click s-prev q & {:as extra}]
  (merge {:status :present :policy-key k :occurrence {:flight "fl" :click click}
          :s-prev {:value s-prev} :q q :f 0.5}
         extra))

(defn- wrap [s] {:step s :path "p" :sha256 "h"})

(deftest two-chained-steps-are-admitted-in-occurrence-order
  (let [s1 (step ka "run-1" {#{} 1} {#{:t} 1})
        s2 (step ka "run-2" {#{:t} 1} {#{:t :u} 1})
        r (adm/admit ka (map wrap [s2 s1]))]
    (is (= :admitted (:conditioning-status r)))
    (is (= ["run-1" "run-2"] (mapv #(get-in % [:occurrence :click]) (:observation-updates r))))))

(deftest a-broken-chain-ends-the-prefix
  (let [r (adm/admit ka (map wrap [(step ka "run-1" {#{} 1} {#{:t} 1})
                                   (step ka "run-2" {#{:x} 1} {#{:t :u} 1})]))]
    (is (= :chain-broken (:conditioning-status r)))
    (is (= 1 (count (:observation-updates r))))
    (is (= 1 (get-in r [:ended-at :index])))))

(deftest a-duplicate-occurrence-ends-the-prefix
  (let [s1 (step ka "run-1" {#{} 1} {#{:t} 1})
        r (adm/admit ka (map wrap [s1 (step ka "run-1" {#{:t} 1} {#{:t} 1})]))]
    (is (= :duplicate-occurrence (:conditioning-status r)))
    (is (= 1 (count (:observation-updates r))))))

(deftest a-step-under-another-policy-is-never-admitted
  ;; SPEC-F: a prefix joined to another policy
  (let [r (adm/admit ka (map wrap [(step ka "run-1" {#{} 1} {#{:t} 1})
                                   (step kb "run-2" {#{:t} 1} {#{:t :v} 1})]))]
    (is (= :admitted (:conditioning-status r)))
    (is (= [ka] (distinct (map :policy-key (:observation-updates r)))))
    (is (= 1 (:foreign r)))))

(deftest a-refused-step-ends-the-prefix-there
  (let [r (adm/admit ka (map wrap [(step ka "run-1" {#{} 1} {#{:t} 1})
                                   {:status :refused :reason :unmeasured-class :policy-key ka
                                    :occurrence {:flight "fl" :click "run-2"}}
                                   (step ka "run-3" {#{:t} 1} {#{:t} 1})]))]
    (is (= :unmeasured-class (:conditioning-status r)))
    (is (= 1 (count (:observation-updates r))) "the step after the refusal is not admitted")))

(deftest no-flight-records-is-the-typed-absence
  (let [dir (str (io/file (str (.toFile (Files/createTempDirectory "f1ba" (make-array FileAttribute 0)))) "absent"))
        c {:id :c1 :target "M-a" :precedence [{:id :p/a}]}]
    (is (= {:policy-key ka :conditioning-status :no-flight-records :dir-status {:absent :no-flights-dir}}
           (get (adm/prefixes [c] (src/conditioning-steps dir)) :c1)))))

(deftest the-reader-gathers-steps-from-flight-records
  (let [dir (.toFile (Files/createTempDirectory "f1ba" (make-array FileAttribute 0)))
        s1 (step ka "run-1" {#{} 1} {#{:t} 1})
        s2 (step ka "run-2" {#{:t} 1} {#{:t :u} 1})]
    (spit (io/file dir "fl.edn") (pr-str {:flight {:enactments [{:click-id "run-1" :step s1} {:click-id "run-2" :step s2}]}}))
    (spit (io/file dir "junk.edn") "{:flight")
    (let [read (src/conditioning-steps (str dir))
          r (get (adm/prefixes [{:id :c1 :target "M-a" :precedence [{:id :p/a}]}] read) :c1)]
      (is (= 2 (count (:steps read))))
      (is (= 1 (count (:unread read))) "an unreadable record is noted, not skipped silently")
      (is (= :admitted (:conditioning-status r)))
      (is (= 2 (count (:observation-updates r)))))))

(deftest the-tick-records-the-prefix-and-its-score-is-unchanged
  (let [assemble* @#'cdt/assemble*
        assembled (assemble* {:targets [cdt/tick-1-target] :sources cdt/tick-1-sources})
        base (:decision (wm/cascade-decision assembled cdt/live-c-opts))
        k (adm/candidate-key (:action base))
        s1 (step k "run-1" {#{} 1} {#{:t} 1})
        with (:decision (wm/cascade-decision assembled
                                             (assoc cdt/live-c-opts :conditioning-steps
                                                    {:steps [(wrap s1)] :read [] :unread []})))
        chosen (get-in base [:action :id])]
    (testing "the prefix is recorded per candidate"
      (is (= :admitted (get-in with [:selection-certificate :token-belief-input :policy-prefixes chosen :conditioning-status])))
      (is (= :no-flight-records
             (get-in base [:selection-certificate :token-belief-input :policy-prefixes chosen :conditioning-status]))
          "no flight steps in opts: the typed absence"))
    (testing "the score is unchanged (F1c-I consumes the prefix, this packet does not)"
      (is (= (pr-str (get-in base [:selection-law :posterior])) (pr-str (get-in with [:selection-law :posterior]))))
      (is (= (pr-str (:action base)) (pr-str (:action with))))
      (is (= (dissoc (get-in base [:selection-certificate :token-belief-input]) :policy-prefixes)
             (dissoc (get-in with [:selection-certificate :token-belief-input]) :policy-prefixes))
          "the rest of the token-belief input, top-level :observation-updates included, is untouched"))))
