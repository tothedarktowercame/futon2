(ns futon2.aif.wm.ordinary-refusal-terminal-test
  "L5-25, terminal half: a plain typed author refusal (kind
  :guardrail-refusal) is the author's answer, not an apparatus injury.
  Lean: DarkTower.WarMachine.OrdinaryRefusalProseRef (mathlib4 darktower
  8851fb083a), repairedKindVerdict and classifier_alone_not_enough.

  Live pin: tick-run-record-2026-09-30-1790748841 (click wm-click-7a8ba5b2),
  whose :failure is {:kind :guardrail-refusal :stage :author-wait :error
  \"Author refused with a typed reason\"}; on 2026-11-01 it replayed :injury
  under the deployed kind table."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.meta-terminal-injury :as terminal]))

(def sep-30-path "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-09-30-1790748841.edn")
(def sep-30-sha256 "12e561541ad0372f634369d2e9b1289bedf4f8c769769aa1d425068b0ea197a3")
(def sep-30-pin {:path sep-30-path :sha256 sep-30-sha256})

(defn- classification [record pin]
  (select-keys (terminal/classify record pin)
               [:status :classification :failure-kind :non-injury-reason]))

(deftest the-september-typed-refusal-is-the-authors-answer
  (let [file (io/file sep-30-path)]
    (if-not (.isFile file)
      (is true "retained record not on this machine; the reconstructed shape below pins the rule")
      (let [bytes (java.nio.file.Files/readAllBytes (.toPath file))
            _ (is (= sep-30-sha256 (identity/sha256 bytes)) "live pin")
            record (edn/read-string {:default (fn [_ v] v)} (String. bytes "UTF-8"))]
        (is (= :guardrail-refusal (get-in record [:failure :kind])))
        (is (= {:status :classified :classification :non-injury
                :failure-kind :guardrail-refusal
                :non-injury-reason :ordinary-author-refusal}
               (classification record sep-30-pin)))))))

(def click-2-reconstructed-close
  ;; click wm-click-8ed79a0b, run 2026-11-01-0e6ff636: the close the repaired
  ;; classifier gives the retained author job (author_refusal_prose_ref_test)
  {:run/id "2026-11-01-0e6ff636-1b8d-4fe0-9b65-cb8588223767"
   :click/id "wm-click-8ed79a0b-6375-4d49-894b-eb2090d2ea27"
   :outcome :guardrail-refusal
   :failure {:kind :guardrail-refusal :stage :author-wait
             :error "Author refused with a typed reason"
             :cause {:absent :no-cause}
             :detail {:outcome :guardrail-refusal :failure-kind :guardrail-refusal
                      :failure-stage :author-wait :target "M-interim-director"}}})

(def click-2-pin
  {:path "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-11-01-0e6ff636-1b8d-4fe0-9b65-cb8588223767.edn"
   :sha256 "43ce01bc2b120b464f94e716888e34b455360ef03df023a3579bedf492e6459a"})

(deftest the-repaired-click-2-close-is-a-non-injury
  (is (= {:status :classified :classification :non-injury
          :failure-kind :guardrail-refusal
          :non-injury-reason :ordinary-author-refusal}
         (classification click-2-reconstructed-close click-2-pin))))

(deftest a-build-failure-is-still-an-injury
  ;; buildFailed_still_injury: the close click 2 actually recorded
  (is (= {:status :classified :classification :injury
          :failure-kind :invalid-author-refusal}
         (classification (-> click-2-reconstructed-close
                             (assoc :outcome :build-failed)
                             (assoc-in [:failure :kind] :invalid-author-refusal)
                             (assoc-in [:failure :detail :outcome] :build-failed)
                             (assoc-in [:failure :detail :failure-kind] :invalid-author-refusal))
                         click-2-pin))))
