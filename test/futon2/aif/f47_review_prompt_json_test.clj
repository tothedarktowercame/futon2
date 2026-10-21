(ns futon2.aif.f47-review-prompt-json-test
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.handoff-contract :as handoff]
            [futon2.aif.wm.open-cascade-resume :as resume]))

(def author-fixture
  "test/futon2/aif/fixtures/click-18-zai-2-author-answer.txt")

(def reviewer-fixture
  "test/futon2/aif/fixtures/click-18-zai-3-reviewer-answer.txt")

(defn- private-fn [sym]
  (or (some-> (requiring-resolve sym) var-get)
      (throw (ex-info "private test seam is absent" {:symbol sym}))))

(defn- prompt-author-json [prompt]
  (second (str/split prompt #"AUTHOR ANSWER:\n" 2)))

(deftest review-prompt-renders-the-author-answer-as-json
  (let [decode-hole-id (private-fn
                        'futon2.aif.wm.open-cascade-resume/decoded-hole-id)
        review-prompt (private-fn
                       'futon2.aif.full-loop-runtime/open-cascade-review-prompt)
        author-answer (handoff/response-json-object (slurp author-fixture))
        hole-ids (mapv decode-hole-id (keys (:decisions author-answer)))
        requisition {:status :dispatched
                     :schema :wm/open-cascade-authoring-requisition-v1
                     :requisition-id (:requisition-id author-answer)
                     :cascade-digest (:cascade-digest author-answer)
                     :phase :compile
                     :holes (mapv (fn [hole-id]
                                    {:hole-id hole-id
                                     :owner :wm
                                     :question :operation})
                                  hole-ids)
                     :author (:by author-answer)
                     :reviewer "zai-3"}
        prompt (review-prompt {:requisition requisition
                               :author-answer author-answer})
        rendered-answer (json/parse-string (prompt-author-json prompt))
        decision-keys (keys (get rendered-answer "decisions"))]
    (testing "the malformed live reviewer answer remains undecodable"
      (is (nil? (handoff/response-json-object (slurp reviewer-fixture)))))

    (testing "the prompt carries one JSON author object with string hole ids"
      (is (map? rendered-answer))
      (is (every? string? decision-keys))
      (is (= (set hole-ids)
             (set (map decode-hole-id decision-keys)))))

    (testing "a reviewer can copy the rendered answer and add its identity"
      (let [reviewed-text (json/generate-string
                           (assoc rendered-answer "accepted-by" "zai-3"))
            reviewed (handoff/response-json-object reviewed-text)
            validation (resume/validate-answer requisition reviewed)]
        (is (= :accepted (:status validation)))
        (is (= (set hole-ids) (set (keys (:decisions validation)))))))))
