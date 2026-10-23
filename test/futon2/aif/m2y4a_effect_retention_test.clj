(ns futon2.aif.m2y4a-effect-retention-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.wm.cascade-capability-dispatch :as dispatch]
            [futon2.aif.wm.cascade-continuation :as sut])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def revision (apply str (repeat 40 "a")))
(def digest (evidence/value-digest [:ground]))
(def node {:occurrence-id ["pattern/example" 0]
           :clause {:operation :apply :capability :wm/pattern-apply}})
(defn execute [grounding]
  ((sut/reviewed-build-review-close-executor
    {:author "author" :reviewer "reviewer" :author-contact identity
     :reviewer-contact
     (fn [{:keys [request]}]
       (merge (select-keys request [:request-id :cascade-digest :occurrence-id])
              {:by "author" :accepted-by "reviewer"
               :effect {:status "verified" :grounding grounding}}))})
   {:cascade-digest digest :node node}))

(deftest optional-revision-is-checked-and-retained
  (let [ground {:kind "commit" :digest digest :revision revision :repo "futon2"}
        receipt (execute ground)]
    (is (dispatch/valid-receipt? receipt digest node))
    (is (= ground (get-in receipt [:effect :grounding]))))
  (doseq [bad [digest "abcdef0" 42 nil (apply str (repeat 40 "B"))]]
    (is (= :reviewed-effect-invalid
           (:kind (execute {:digest digest :revision bad :repo "futon2"})))) )
  (doseq [repo [nil "" "  " :futon2]]
    (is (= :reviewed-effect-invalid
           (:kind (execute {:digest digest :revision revision :repo repo})))))
  (is (dispatch/valid-receipt? (execute {:kind "content-addressed" :digest digest})
                             digest node)))

(deftest durable-close-retains-complete-sealed-receipts
  (let [root (.toFile (Files/createTempDirectory "m2y4a-close-" (make-array FileAttribute 0)))
        receipt (execute {:kind "commit" :digest digest :revision revision :repo "futon2"})
        packet {:selected-cascade-digest digest :cascade-digest digest
                :frontier [(:occurrence-id node)] :effect-receipts [receipt]
                :journal-certificate {:status :certified}}
        close! (sut/production-close-adapter {:root root :source-run-id "test-run"
                                             :source-attempt-id "test-attempt"})
        result (close! packet)
        stored (edn/read-string (slurp (get-in result [:close-receipt :path])))]
    (is (= [receipt] (:effect-receipts stored)))
    (is (= [(:digest receipt)] (:effect-receipt-digests stored)))
    (is (= revision (get-in stored [:effect-receipts 0 :effect :grounding :revision])))
    (is (= result (close! packet)))))

(deftest old-close-is-readable-without-migration
  (let [text (slurp "test/resources/m2w/legacy-continuation-close.edn")
        old (edn/read-string text)]
    ;; File copied from the retained live close; no body is fabricated.
    (is (= :wm/cascade-continuation-close-v1 (:schema old)))
    (is (= "7a4af8ece478ab9561bf37d2977a8a87598b9607a94107cd74470351e9512fd8"
           (:digest old)))
    (is (= (:digest old) (evidence/value-digest (dissoc old :digest))))
    (is (= 4 (count (:effect-receipt-digests old))))
    (is (not (contains? old :effect-receipts)))))

(deftest commit-review-prompt-names-revision-authority
  (let [prompt (#'runtime/cascade-effect-review-prompt {:request {} :author-answer {}})]
    (is (.contains prompt "revision"))
    (is (.contains prompt "repo"))
    (is (.contains prompt "40-hex"))
    (is (.contains prompt "inspected"))))
