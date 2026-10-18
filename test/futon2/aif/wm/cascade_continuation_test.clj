(ns futon2.aif.wm.cascade-continuation-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.wm.cascade-capability-dispatch :as dispatch]
            [futon2.aif.wm.cascade-continuation :as sut]
            [futon2.aif.wm.executable-cascade :as cascade]
            [futon2.aif.wm.cascade-enactment-compiler-test :as compiler-test])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]
           [java.time Instant]))

(defn- temp-dir [prefix]
  (.toFile (Files/createTempDirectory prefix (make-array FileAttribute 0))))

(defn- fixture []
  (let [structural (#'compiler-test/structural)
        clauses (into {}
                      (map (fn [n]
                             [(:occurrence-id n)
                              {:operation :apply
                               :capability :wm/pattern-apply
                               :consumes :before :produces :after
                               :completion-check [:observed :after]}]))
                      (:nodes structural))]
    (cascade/build structural
                   {:application-order [] :operational-clauses clauses
                    :holes {}})))

(defn- ready [record]
  {:status :cascade-ready
   :cascade-digest (evidence/value-digest [:selected])
   :compiled-digest (:digest record)
   :capability-routing
   {:status :routed :cascade-digest (:digest record)
    :steps (mapv (fn [n]
                   {:occurrence-id (:occurrence-id n)
                    :operation (get-in n [:clause :operation])
                    :capability (get-in n [:clause :capability])
                    :route :full-loop-build-review-close})
                 (:frontier record))}})

(deftest verified-effects-are-journalled-before-close
  (let [record (fixture)
        cascades (temp-dir "continuation-cascades-")
        journals (temp-dir "continuation-journals-")
        _ (cascade/save! record cascades)
        close-calls (atom [])
        result (sut/continue-certified-cascade!
                {:ready (ready record)
                 :executable-cascade-root cascades
                 :execution-journal-root journals
                 :executors
                 {:wm/pattern-apply
                  (dispatch/recording-pattern-executor
                   (temp-dir "continuation-effects-"))}
                 :instant-fn #(Instant/parse "2026-10-18T00:00:00Z")
                 :close-fn
                 (fn [packet]
                   (swap! close-calls conj packet)
                   {:status :closed :cascade-digest (:digest record)
                    :close-receipt
                    {:evidence-kind :verified-effect
                     :digest (evidence/value-digest packet)}})})]
    (is (= :closed (:status result)))
    (is (false? (:selection-ran result)))
    (is (zero? (:clicks-spent result)))
    (is (= (count (:frontier record))
           (count (:effect-receipts result))))
    (is (= 1 (count @close-calls)))))

(deftest close-is-unreachable-without-verified-effects
  (let [record (fixture)
        cascades (temp-dir "continuation-cascades-")
        _ (cascade/save! record cascades)
        calls (atom 0)
        result (sut/continue-certified-cascade!
                {:ready (ready record)
                 :executable-cascade-root cascades
                 :execution-journal-root (temp-dir "continuation-journals-")
                 :executors {:wm/pattern-apply (constantly {:status :ok})}
                 :close-fn (fn [_] (swap! calls inc))})]
    (is (= :capability-effect-receipt-invalid (:kind result)))
    (is (zero? @calls))))

(deftest route-order-and-close-evidence-are-bound
  (let [record (fixture)
        cascades (temp-dir "continuation-cascades-")
        _ (cascade/save! record cascades)
        reversed (update-in (ready record) [:capability-routing :steps]
                            #(vec (reverse %)))
        common {:executable-cascade-root cascades
                :execution-journal-root (temp-dir "continuation-journals-")
                :executors {}
                :close-fn identity}]
    (is (= :continuation-routing-mismatch
           (:kind (sut/continue-certified-cascade!
                   (assoc common :ready reversed)))))
    (testing "a close label without verified close evidence cannot succeed"
      (let [effects (temp-dir "continuation-effects-")
            result (sut/continue-certified-cascade!
                    (assoc common :ready (ready record)
                           :executors
                           {:wm/pattern-apply
                            (dispatch/recording-pattern-executor effects)}
                           :close-fn
                           (constantly {:status :closed
                                        :cascade-digest (:digest record)})))]
        (is (= :continuation-close-unverified (:kind result)))))))

(deftest reviewed-executor-requires-exact-independent-grounding
  (let [ground (evidence/value-digest [:ground])
        author-answer (atom nil)
        executor
        (sut/reviewed-build-review-close-executor
         {:author "zai-2" :reviewer "zai-6"
          :author-contact #(do (reset! author-answer %)
                               {:request-id (:request-id %) :work :done})
          :reviewer-contact
          (fn [{:keys [request]}]
            {:request-id (:request-id request)
             :cascade-digest (:cascade-digest request)
             :occurrence-id (:occurrence-id request)
             :by "zai-2" :accepted-by "zai-6"
             :effect {:status :verified
                      :grounding {:kind :reviewed-build-review-close
                                  :digest ground}}})})
        record (fixture)
        node (first (:frontier record))
        receipt (executor {:cascade-digest (:digest record) :node node})]
    (is (= (:occurrence-id node) (:occurrence-id @author-answer)))
    (is (= :verified-effect (:evidence-kind receipt)))
    (is (dispatch/valid-receipt? receipt (:digest record) node)))
  (testing "self-review never mints an effect receipt"
    (let [executor (sut/reviewed-build-review-close-executor
                    {:author "zai-2" :reviewer "zai-2"
                     :author-contact identity
                     :reviewer-contact (constantly {})})]
      (is (= :reviewed-effect-invalid
             (:kind (executor {:cascade-digest (evidence/value-digest [:c])
                               :node {:occurrence-id :n}})))))))

(deftest production-close-is-content-addressed-and-idempotent
  (let [root (temp-dir "continuation-close-")
        close! (sut/production-close-adapter
                {:root root :source-run-id "run-1"
                 :source-attempt-id "attempt-1"})
        packet {:selected-cascade-digest (evidence/value-digest [:selected])
                :cascade-digest (evidence/value-digest [:compiled])
                :frontier [[:p 0]]
                :effect-receipts [{:digest (evidence/value-digest [:effect])}]
                :journal-certificate {:status :certified :head "h"}}
        a (close! packet)
        b (close! packet)]
    (is (= :closed (:status a)))
    (is (= a b))
    (is (.isFile (java.io.File. (get-in a [:close-receipt :path]))))
    (is (= "run-1" (get-in a [:close-receipt :source-run-id])))))
