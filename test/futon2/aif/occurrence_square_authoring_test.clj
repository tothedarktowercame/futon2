(ns futon2.aif.occurrence-square-authoring-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.occurrence-square-authoring :as sut]
            [futon2.aif.occurrence-square-registry :as squares]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.target-reading-registry :as readings]
            [futon2.aif.three-halves-square :as square]))

(def digest-a (apply str (repeat 64 "a")))
(def pattern "ukrns/publication-cadence")
(def pattern-sha "24a8a429148b1c6e2b41e482ee539c00285154957022f18b3dab181bbdf4fe12")

(def analysis
  {:status "analyzed"
   :sentences [{:id "s1"
                :fragments [{:start 0 :end 4 :text "work" :intent "act"
                             :target "mission" :rationale "needed"
                             :relations ["action"]
                             :pattern_refs [{:status "candidate" :id pattern
                                             :rationale "fits"
                                             :source_sha256 pattern-sha}]
                             :pattern_rejections []}]}]})

(defn temp-root []
  (.toFile (java.nio.file.Files/createTempDirectory
            "occ-square-authoring-"
            (make-array java.nio.file.attribute.FileAttribute 0))))

(defn reading! [root]
  (readings/publish!
   root {:target-id "M-live" :source-path "/tmp/M-live.md"
         :excerpt-digest digest-a :source-file-digest digest-a
         :request {:task {:target_id "M-live" :excerpt_sha256 digest-a
                          :content_sha256 digest-a}}
         :analysis analysis :validator-version 1}))

(defn judgement [reading]
  {:decision
   {:selection-certificate
    {:target-policy-field
     {:families [{:target-id "M-live" :policies []
                  :failures [{:kind :occurrence-squares-not-authored
                              :excerpt-digest digest-a
                              :analysis-digest (:analysis-digest reading)}]}]}}}})

(def answer
  {:schema "wm/occurrence-square-maps-v1"
   :target "M-live"
   :entries
   [{:occurrence_id [pattern 0]
     :maps {:I1 {:element "living rendering" :source "target reading fragment"}
            :I2 {:element "frozen snapshot" :source "target reading fragment"}
            :G {:element "publication address" :source "pattern BECAUSE"}
            :B {:element "both discoverable" :source "pattern CHECK"}}}]})

(deftest publishes-machine-built-square-before-g
  (let [reading-root (temp-root) square-root (temp-root)
        reading (reading! reading-root)
        result (sut/repair!
                {:author "wm-author" :target-reading-root reading-root
                 :occurrence-square-root square-root
                 :answer-fn (fn [_] (assoc answer :analysis_digest
                                           (:analysis-digest reading)))}
                (judgement reading))
        record (squares/current square-root "M-live" (:analysis-digest reading)
                                (squares/library-resolver))
        built (get (squares/squares record) [pattern 0])]
    (is (= :published (:status result)))
    (is (= :valid (:status (square/validate built))))
    (is (= :valid (:status (square/parts-check
                            built ((squares/library-resolver) pattern)))))))

(deftest answer-must-cover-the-exact-occurrence-set
  (let [reading-root (temp-root) reading (reading! reading-root)
        result (sut/repair!
                {:author "wm-author" :target-reading-root reading-root
                 :occurrence-square-root (temp-root)
                 :answer-fn (fn [_] (assoc answer :analysis_digest
                                           (:analysis-digest reading)
                                           :entries []))}
                (judgement reading))]
    (is (= :error (:status result)))
    (is (= :occurrence-square-answer-unbound (:kind result)))))

(deftest held-inner-construction-redecides-once-after-publication
  (let [reading-root (temp-root) square-root (temp-root)
        reading (reading! reading-root) calls (atom 0)
        repair (fn [_ j]
                 (sut/repair!
                  {:author "wm-author" :target-reading-root reading-root
                   :occurrence-square-root square-root
                   :answer-fn (fn [_] (assoc answer :analysis_digest
                                             (:analysis-digest reading)))} j))
        result (runner/occurrence-square-authoring-step
                {} (judgement reading) repair
                (fn [_] (swap! calls inc) {:decision {:status :selected}}))]
    (is (= 1 @calls))
    (is (= :selected (get-in result [:judgement :decision :status])))
    (is (= :published (get-in result [:record :status])))))

(deftest debugger-does-not-stop-between-reading-and-square-authoring
  (let [reading (reading! (temp-root))
        missing-squares (judgement reading)
        authoring-port (fn [& _] nil)]
    (is (false? (runner/selected-target-redecision-terminal?
                 {} authoring-port missing-squares)))
    (is (true? (runner/selected-target-redecision-terminal?
                {} nil missing-squares)))
    (is (true? (runner/selected-target-redecision-terminal?
                {:flight {}} authoring-port missing-squares)))
    (is (true? (runner/selected-target-redecision-terminal?
                {} authoring-port {:decision {:status :selected}})))))
