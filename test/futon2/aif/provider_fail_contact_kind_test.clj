(ns futon2.aif.provider-fail-contact-kind-test
  "PROVIDER-FAIL-2: a seat contact whose job reached a non-done terminal state
  (a provider 429/500, a timeout, a cancellation) is a failed CONTACT, typed
  apart from an answer that existed and did not parse. Observed 2026-11-07:
  a Zai 429 on the wm-author contact was recorded as
  :open-cascade-author-answer-invalid with defect :response-unparseable and
  answer \"nil\" (run 2026-11-07-1249942a, job invoke-1794040065486-15049).

  The reviewer-side cases reuse author-json-contract-test's harness: a
  binding-valid author answer plus the canonical delivery resolver, so the
  reviewer's contact is actually reached."
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [clojure.java.io :as io]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.test-support.runner-fixture :as fixture]
            [futon2.aif.wm.cascade-patch-delivery :as delivery]
            [futon2.aif.wm.authored.req-cfdb1f62 :as authored]))

(use-fixtures :once fixture/with-hermetic-traces hermetic/with-hermetic-stores)

(def digest "309421670ca97dd1b1c73447bca9d818b93e2334d5d98d3c0208dd419b9336cb")
(def patch {:namespace "futon2.aif.wm.authored.req-cfdb1f62" :var "decisions"
            :source-digest "fe48324bdc0e11039484a434484c2ce1a12fa430e9b61fef8cd3c3ab32f2f894"
            :commit "bb1846638820488b1bd5494fbd714aa0213bfa66"})
(def held
  {:kind :wm/open-cascade-execution-error :cascade-digest digest :phase :compile
   :holes (mapv (fn [id] {:hole-id id :owner :wm/enactment-compiler
                          :question :operational-clause-owed}) (keys authored/clauses))
   :resume-port {:retry (fn [ctx] {:status :cascade-ready :cascade-digest digest
                                   :delivered (:authoring-product ctx)})}})

(defn bound-author-answer [req]
  {:requisition-id (:requisition-id req) :cascade-digest digest :by "wm-author"
   :patch patch
   :decisions (mapv (fn [[id clause]] (assoc clause :hole id)) authored/clauses)})

(def canonical-check
  (let [check delivery/check-delivery]
    (fn [a] (check delivery/canonical-repository
             #(-> (io/file delivery/canonical-repository "src" %) .toURI .toURL) a))))

(deftest failed-author-job-is-a-contact-failure-not-an-unparseable-answer
  ;; The exact shape agency-answer-fn returns for a provider 429/500: the
  ;; job envelope's state string, no answer payload.
  (let [reviewer-called? (atom false)
        handler (runtime/open-cascade-resume-handler
                 {:author "wm-author"
                  :reviewer "wm-reviewer"
                  :open-cascade-author-answer-fn
                  (fn [_]
                    {:state "failed"
                     :job-id "invoke-1794040065486-15049-56005758"
                     :text nil})
                  :open-cascade-reviewer-answer-fn
                  (fn [_]
                    (reset! reviewer-called? true)
                    {:state "done" :result "must not be called"})})
        result (handler held)]
    (is (= :error (:status result)))
    (is (= :open-cascade-author-contact-failed (:kind result)))
    (is (= "failed" (:job-state result)))
    (is (= "wm-author" (:author result)))
    (is (= "invoke-1794040065486-15049-56005758" (:author-job result)))
    (is (= :contact-failed (get-in result [:answer-evidence :defect :kind])))
    (is (= "failed" (get-in result [:answer-evidence :defect :job-state])))
    (is (false? @reviewer-called?))))

(deftest done-but-undecodable-author-answer-keeps-its-answer-invalid-kind
  ;; State "done": the seat answered and the answer did not parse -- exactly
  ;; today's behaviour, including the :author-response-unparseable spelling
  ;; once a format correction ran (F44b).
  (let [contacts (atom 0)
        handler (runtime/open-cascade-resume-handler
                 {:author "wm-author"
                  :reviewer "wm-reviewer"
                  :open-cascade-author-answer-fn
                  (fn [req]
                    (swap! contacts inc)
                    (if (= 1 @contacts)
                      {:state "done" :job-id "j1" :result "not json"}
                      {:state "done" :job-id "j2" :result "still not json"}))
                  :open-cascade-reviewer-answer-fn
                  (fn [_] {:state "done" :result "must not be called"})})
        result (handler held)]
    (is (= :error (:status result)))
    (is (= :author-response-unparseable (:kind result)))
    (is (= :response-unparseable (get-in result [:answer-evidence :defect :kind])))
    (is (= "j2" (:author-job result)))))

(deftest failed-reviewer-job-is-a-contact-failure-not-an-unparseable-answer
  (let [handler (runtime/open-cascade-resume-handler
                 {:author "wm-author"
                  :reviewer "wm-reviewer"
                  :open-cascade-author-answer-fn bound-author-answer
                  :open-cascade-reviewer-answer-fn
                  (fn [_] {:state "failed" :job-id "reviewer-job-pf2" :text nil})})
        result (with-redefs [delivery/check-delivery canonical-check]
                 (handler held))]
    (is (= :error (:status result)))
    (is (= :open-cascade-review-contact-failed (:kind result)))
    (is (= "failed" (:job-state result)))
    (is (= "wm-reviewer" (:reviewer result)))
    (is (= "reviewer-job-pf2" (:reviewer-job result)))
    (is (= :contact-failed (get-in result [:answer-evidence :defect :kind])))))

(deftest done-but-undecodable-reviewer-answer-keeps-its-answer-invalid-kind
  (let [handler (runtime/open-cascade-resume-handler
                 {:author "wm-author"
                  :reviewer "wm-reviewer"
                  :open-cascade-author-answer-fn bound-author-answer
                  :open-cascade-reviewer-answer-fn
                  (fn [_] {:state "done" :job-id "rj" :result "prose only"})})
        result (with-redefs [delivery/check-delivery canonical-check]
                 (handler held))]
    (is (= :error (:status result)))
    (is (= :open-cascade-review-answer-invalid (:kind result)))
    (is (= :response-unparseable (get-in result [:answer-evidence :defect :kind])))
    (is (= "rj" (:reviewer-job result)))))

(deftest uncontacted-designated-author-is-a-typed-contact-failure
  ;; designated-agent-answer-fn answers {:state :not-contacted ...} when the
  ;; seat is not invoke-ready: also a contact that produced no answer, so the
  ;; uniform rule types it :open-cascade-author-contact-failed with the state
  ;; as received, while the author-job absence keeps naming :not-contacted.
  (let [handler (runtime/open-cascade-resume-handler
                 {:author "wm-author"
                  :reviewer "wm-reviewer"
                  :open-cascade-author-answer-fn
                  (fn [_] {:state :not-contacted
                           :reason :designated-author-unavailable
                           :author "wm-author"})
                  :open-cascade-reviewer-answer-fn
                  (fn [_] {:state "done" :result "must not be called"})})
        result (handler held)]
    (is (= :open-cascade-author-contact-failed (:kind result)))
    (is (= :not-contacted (:job-state result)))
    (is (= {:status :absent
            :reason :not-contacted
            :contact-reason :designated-author-unavailable}
           (:author-job result)))))
