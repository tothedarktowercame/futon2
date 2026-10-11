(ns futon2.aif.open-cascade-relatedness-sut-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.interpretation-construction :as construction]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.open-cascade-pattern-search :as search]
            [futon2.aif.open-cascade-relatedness-sut :as sut]
            [futon2.aif.pattern-registry :as registry]
            [futon2.aif.wm.cascade-decision :as decision]))

(def target "M-hand")
(def pattern-id :gauntlet/placenta-transfer)
(def library-root "/home/joe/code/futon3/library")
(def pattern-file (io/file library-root "gauntlet/placenta-transfer.flexiarg"))
(def pattern-bytes (java.nio.file.Files/readAllBytes (.toPath pattern-file)))
(def sha (evidence/sha256 pattern-bytes))
(def pin {:path (.getCanonicalPath pattern-file) :revision "fixture-r1" :sha256 sha})
(def entry {:evidence/id "retrieval-1"
            :evidence/body {:event "context-retrieval" :turn 1 :at "2026-10-11T00:00:00Z"
                            :query "transfer caller conversion"
                            :results [{:id "gauntlet/placenta-transfer" :score 1.0
                                       :pattern-path (.getCanonicalPath pattern-file)}]}})
(defn legacy []
  (let [lines (vec (str/split-lines (String. pattern-bytes "UTF-8")))]
    {:if true :route :structured-antecedent :warrant {:review :independent}
     :citation {:kind :pattern-text :path (:path pin) :sha256 sha
                :lines [1 1] :quote (first lines)}}))
(defn bare-judgment [verdict]
  {:pattern pattern-id :verdict verdict
   :evidence (if (= :admissible verdict)
               {:source-pin pin :legacy-receipt (legacy)}
               {:source-pin pin :reason "not applicable"})})
(defn authority [verdict]
  (let [job (let [j {:job-id "review-1" :agent-id "independent-reviewer" :state "done"
                     :result {:schema :wm/open-cascade-pattern-judgments-v1
                              :judgments [(bare-judgment verdict)]}}]
              (assoc j :result-digest (search/agency-result-digest j)))]
    {:job job :judgment (assoc (bare-judgment verdict) :authority
                               {:id "independent-reviewer" :job-id "review-1"
                                :result-digest (:result-digest job)})}))
(def want :caller-converted)
(def sources
  {:universes {target {:sites-enumerated true :one-producer true :caller-converted false}}
   :wants {target [want]}
   :locators {target {:sites-enumerated {:class :C3 :stated "sites"}
                      :one-producer {:class :C3 :stated "producer"}
                      :caller-converted {:class :C3 :stated "converted"}}}
   :interpretations {target {:patterns {} :receipts {}}}
   :horizon-steps 4 :beta-by-context {:revision {:beta 1}}
   :context-of (constantly :revision)
   :construction {:construct construction/construct
                  :budget {:max-moves 4 :max-expansions 20000} :move-cost 0
                  :evaluate-g decision/constructed-candidate-g}})
(def blend-square (edn/read-string (slurp "test/fixtures/three-halves-square/publication-cadence.edn")))
(def response
  {:schema :wm/want-interpretation-response-v1 :pattern pattern-id
   :guard {:needs #{:sites-enumerated :one-producer} :forbids #{}}
   :produces #{want}
   :receipt {:source {:path "futon3/library/gauntlet/placenta-transfer.flexiarg" :sha256 sha}
             :reading "Transfer caller conversion." :scope-limit "Only this conversion."
             :by "separate-interpreter" :blend-square blend-square}})
(def original {:kind :cascade-candidate :id :C0 :target target :precedence []
               :construction-receipt {:reached-wants []}})
(def blocker {:schema :wm/mid-run-cascade-blocker-v1 :status :present
              :target target :stage :review :kind :stuck})
(def context {:mode :controlled-sut :target target :original original :blocker blocker
              :construction {:selected-action original}
              :whole-mission {:schema :wm/whole-mission-context-v1 :target target
                              :scope :whole-mission :path "/controlled/M-hand.md"
                              :sha256 (apply str (repeat 64 "a")) :content "# M-hand"}})
(defn input [verdict]
  (let [{:keys [job judgment]} (authority verdict)]
    {:entries [entry] :top-k 1 :scope :bounded :limitation "canonical retrieval top-k=1"
     :retrieval-revision "fixture-r1" :judgments [judgment]
     :authority-results {"review-1" job}
     :search-implementation (let [x {:id "canonical-interpreter" :content {:version 1}}]
                              (assoc x :digest (evidence/value-digest x)))
     :blocker (let [x {:id "blocker" :content blocker}] (assoc x :digest (evidence/value-digest x)))
     :prior-cascade (let [x {:id "prior" :content original}] (assoc x :digest (evidence/value-digest x)))
     :interpretation-response response :revision-context context :sources sources
     :code-root "/home/joe/code"}))

(deftest bounded-real-retrieval-to-real-revision-seam
  (binding [registry/*pattern-library-root* library-root]
    (let [r (sut/run-controlled! (input :admissible))]
      (is (= :ready-for-external-dispatch (:status r)) (pr-str r))
      (is (= pattern-id (:chosen-pattern r)))
      (is (= :revised (get-in r [:revision :status])))
      (is (not= (get-in r [:revision :history 0 :identity])
                (get-in r [:revision :history 1 :identity])))
      (is (= :match (get-in r [:dispatch :verdict]))))))

(deftest typed-no-match-and-production-refusal
  (binding [registry/*pattern-library-root* library-root]
    (is (= :no-match (:status (sut/run-controlled! (input :rejected)))))
    (is (= :bounded (:scope (sut/run-controlled! (input :rejected)))))
    (is (= :canonical-retrieval-completeness-unavailable
           (:kind (sut/run-controlled! (assoc (input :rejected) :scope :complete)))))
    (is (= :production-judgment-authority-unconfigured
           (:kind (sut/run-controlled! (assoc-in (input :admissible)
                                                  [:revision-context :mode] :production)))))))

(deftest adversarial-search-and-join-refusals
  (binding [registry/*pattern-library-root* library-root]
    (let [base (input :admissible)]
      (doseq [[mutation expected]
              [[#(assoc % :judgments []) :judgment-coverage-or-order-mismatch]
               [#(update % :judgments conj (first (:judgments %))) :judgment-coverage-or-order-mismatch]
               [#(assoc-in % [:judgments 0 :authority :id] "canonical-interpreter") :authority-agent-mismatch]
               [#(assoc-in % [:judgments 0 :evidence :source-pin :sha256]
                           (apply str (repeat 64 "0"))) :authority-judgment-mismatch]
               [#(assoc % :interpretation-response (assoc response :pattern :other/pattern))
                :interpreter-chosen-pattern-mismatch]]]
        (is (= expected (:kind (sut/run-controlled! (mutation base))))))
      (is (= :revision-admission-mismatch
             (:kind (sut/run-controlled!
                     (assoc base :interpretation-response
                            (assoc-in response [:receipt :source :sha256]
                                      (apply str (repeat 64 "0"))))))))
      (is (= :revision-dispatch-mismatch
             (:kind (sut/run-controlled!
                     (assoc base :apply-revision-fn (fn [construction _] construction)))))))))
