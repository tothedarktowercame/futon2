(ns futon2.aif.unticked-work-unseen-test
  "UNTICKED-WORK-UNSEEN (runs d5b9b513, c5590f95): an author landed real,
  reviewed work (futon3 a12870e89, 165 lines with tests) but was never told
  that the selected wants are measured as mission checkbox lines in their
  ticked form, so every want measured false and the machine saw no want
  movement. Fixtures are the retained c5590f95 selected action's locators
  and the real author reply (sha-pinned below)."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.selected-want-outcome :as outcome]))

(def reply-sha256
  "1e6f008160c8753dc0f05730a2e1012e7dfe4935b41af414e2159336a622c529")

(def real-reply
  (slurp "test/resources/wm/unticked-work-unseen-c5590f95-author-reply.txt"))

(def target "M-pattern-retrieval-calibration")
(def futon3 "/home/joe/code/futon3")

;; Verbatim from the retained c5590f95 selection: six selected wants, every
;; locator a C4 checkbox in the mission file that lives in the SAME
;; repository as the target (futon3) — the cross-repo split is ruled out.
(def retained-locators
  {[target :hole/hf4b2bc0eec52]
   {:class :C4 :repo "futon3" :sha "HEAD"
    :path "holes/missions/M-pattern-retrieval-calibration.md"
    :decl "- [x] `futon3a/src/futon/flexiarg/projection.clj` is the single parser and passes the heterogeneous-root, anchored-metadata, malformed-pattern, and deterministic-artifact checks in D-11."}
   [target :hole/ha1f8d3ea27e0]
   {:class :C4 :repo "futon3" :sha "HEAD"
    :path "holes/missions/M-pattern-retrieval-calibration.md"
    :decl "- [x] `build_pattern_index.clj`, `pattern_sync.clj`, and `flexiarg_projection.clj` delegate to the shared parser and retain no parallel block or metadata parsers."}
   [target :hole/h04109a49a6ef]
   {:class :C4 :repo "futon3" :sha "HEAD"
    :path "holes/missions/M-pattern-retrieval-calibration.md"
    :decl "- [x] The hybrid candidate builder emits a deduplicated pool of at most six structured packets and is byte-identical for identical query and corpus inputs."}
   [target :hole/hc4316b211031]
   {:class :C4 :repo "futon3" :sha "HEAD"
    :path "holes/missions/M-pattern-retrieval-calibration.md"
    :decl "- [x] The HIT widget persists exactly one `retrieval-hit` record per submitted decision and can be adjudicated without leaving the REPL."}
   [target :hole/h08a9af20f171]
   {:class :C4 :repo "futon3" :sha "HEAD"
    :path "holes/missions/M-pattern-retrieval-calibration.md"
    :decl "- [x] Closing a session produces exactly one retrieval-hit run plus one snapshot per active surface using the M-5 shapes."}
   [target :hole/hf82be820d9d3]
   {:class :C4 :repo "futon3" :sha "HEAD"
    :path "holes/missions/M-pattern-retrieval-calibration.md"
    :decl "- [x] A reproducible calibration artifact over at least 30 HITs reports baseline and reweighted top-three best-hit rates."}})

(def retained-wants (vec (keys retained-locators)))

(def measured-sha "a12870e89bd2d791400d9dbf579949f65d9a074d")

(defn- measurement [want observed]
  {:declared-locator (get retained-locators want)
   :after-locator (assoc (get retained-locators want) :sha measured-sha)
   :result {:observed observed
            :evidence {:resolved-sha measured-sha}}})

(defn- retained-action-input [& {:keys [tick demo-sha]}]
  {:selected-action {:target target :want retained-wants
                     :observation-locators retained-locators}
   :mission-observation
   {:status :admitted
    :rows (mapv (fn [want]
                  (let [ticked (boolean (and tick (= want tick)))]
                    {:token want
                     :before {:observed false}
                     :after {:observed ticked
                             :measurement (measurement want ticked)}}))
                retained-wants)}
   :demo-receipt (when demo-sha
                   {:status :present
                    :demo {:pointer {:sha demo-sha}}})})

(deftest the-retained-reply-is-the-real-c5590f95-answer
  (is (str/includes? real-reply
                     "FULL_LOOP_AUTHOR: DONE a12870e89bd2d791400d9dbf579949f65d9a074d"))
  (is (str/includes? real-reply "status line is unchanged (operator-gated DERIVE)")
      "the author honestly states it left the implementation limbs open")
  (is (not (str/includes? real-reply "[x]"))
      "the reply never ticks a mission checkbox"))

(deftest the-author-prompt-now-names-the-want-locators-and-the-tick-contract
  (let [prompt (#'runner/author-prompt
                {:author "wm-author" :reviewer "wm-reviewer"
                 :target-repository futon3
                 :target-repository-head "263dfc26218e6bf0ea7417e48a897fe762801a66"
                 :selected-want-locators retained-locators}
                target {} [] nil)]
    (testing "each want's exact ticked-form decl is shown"
      (is (str/includes? prompt "SELECTED WANTS (what this run is measured on)"))
      (is (str/includes? prompt "- [x] `futon3a/src/futon/flexiarg/projection.clj` is the single parser"))
      (is (str/includes? prompt "holes/missions/M-pattern-retrieval-calibration.md")))
    (testing "the tick contract: same commit, only truly-satisfied wants"
      (is (str/includes? prompt "SAME commit"))
      (is (str/includes? prompt "Tick ONLY wants your commit truly"))
      (is (str/includes? prompt ":checkbox-only"))
      (is (str/includes? prompt "no want movement")))))

(deftest a-tick-without-demo-receipt-stays-checkbox-only
  (let [tick (first retained-wants)
        receipt (outcome/receipt (retained-action-input :tick tick))]
    (is (= :verified (:status receipt)) (pr-str receipt))
    (testing "the flipped want is observed true but not reached"
      (let [row (some #(when (= tick (:want %)) %) (:outcomes receipt))]
        (is (= true (get-in row [:after :observed])))
        (is (= :checkbox-only (:classification row)))
        (is (= :checkbox-only (:instrument row)))))
    (is (= [] (:reached (:by-class receipt))))
    (is (= [tick] (:checkbox-only (:by-class receipt))))
    (testing "the untouched wants stay untouched, not refused"
      (is (= 5 (count (:untouched (:by-class receipt))))))))

(deftest a-tick-plus-demo-receipt-at-the-measured-revision-reaches
  (let [tick (first retained-wants)
        receipt (outcome/receipt (retained-action-input :tick tick
                                                        :demo-sha measured-sha))]
    (is (= :verified (:status receipt)) (pr-str receipt))
    (is (= [tick] (:reached (:by-class receipt))))
    (is (= [] (:checkbox-only (:by-class receipt))))))

(deftest the-terminal-want-delta-shows-grounding-and-movement-separately
  (testing "the real c5590f95 shape: real work, no want movement"
    (let [mission-view {:status :observed
                        :rows (mapv (fn [want]
                                      {:token want
                                       :before {:status :observed :observed false}
                                       :after {:status :observed :observed false}})
                                    retained-wants)}
          delta (#'runner/terminal-want-delta mission-view)]
      (is (= {:wants 6 :before-true 0 :after-true 0
              :became-true []
              :still-not-true (mapv :token (:rows mission-view))}
             delta))
      (is (= 6 (count (:still-not-true delta)))
          "grounded commit, zero want movement — now visible as a separate fact")))
  (testing "one tick flips exactly that want"
    (let [tick (first retained-wants)
          mission-view {:status :observed
                        :rows (mapv (fn [want]
                                      (let [ticked (= want tick)]
                                        {:token want
                                         :before {:status :observed :observed false}
                                         :after {:status :observed
                                                 :observed ticked}}))
                                      retained-wants)}
          delta (#'runner/terminal-want-delta mission-view)]
      (is (= 1 (:after-true delta)))
      (is (= [tick] (:became-true delta)))
      (is (= 5 (count (:still-not-true delta))))))
  (is (nil? (#'runner/terminal-want-delta nil))
      "no mission view, no want-delta line"))
