(ns futon2.aif.flight-reply-text-test
  "WM-SPIKE-FIX-I B: the flight reads a seat's reply once. The fixture is the
  spike's locator job invoke-1790368741658-24428-7c7d3cad captured verbatim
  (its header gives the source): :result-summary a 220-char prefix, :result
  and one text event each the same single-block reply."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.flight-runner :as fr]
            [futon2.aif.mission-reading :as reading]
            [futon2.aif.task-execution-evidence :as tee]
            [futon2.aif.want-interpretation :as wi]))

(def job (edn/read-string (slurp "test/futon2/aif/fixtures/agency-job-invoke-1790368741658-24428-7c7d3cad.edn")))

(deftest the-spike-reply-parses-once
  (is (= 220 (count (:result-summary job))) "the pinned job has the shape the spike saw")
  (is (= {:unparseable-response {:forms 2 :fenced-blocks 2}}
         (wi/parse-reply reading/locator-schema (tee/job-text job)))
      "the joined view reproduces the spike's discard")
  (let [parsed (wi/parse-reply reading/locator-schema (fr/reply-text job))]
    (is (= {:class :C8 :repo "futon3c" :namespace "futon3c.agency.clock-decision-test"}
           (get-in parsed [:response :locator])))))

(deftest no-result-reads-the-text-events-never-the-summary
  (let [j (dissoc job :result)]
    (is (= (:text (first (filter #(= "text" (:type %)) (:events job)))) (fr/reply-text j)))
    (is (= "" (fr/reply-text {:result-summary "prefix only" :events [{:type "prompt" :text "p"}]})))))

(deftest the-answer-fn-reads-by-reply-text
  (let [answer ((fr/agency-answer-fn {:seat "s" :dispatch! (fn [& _] {:job-id "j"}) :poll! (fn [& _] job)})
                {:kind :locator :target "M-autoclock-in" :request-id "r"})]
    (is (= (:result job) (:text answer)))))
