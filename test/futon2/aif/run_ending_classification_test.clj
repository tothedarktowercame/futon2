(ns futon2.aif.run-ending-classification-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.run-ending-classification :as kernel]))

(def target "M-aif-policy-conditioned-eig")
(def occurrence
  {:schema :wm/action-transition-occurrence-v2 :run/id "run" :cohort/id "cohort"
   :attempt/id "attempt-001" :transition/id "transition-00000000-0000-0000-0000-000000000001"
   :action/id "action-00000000-0000-0000-0000-000000000002"
   :action/value {:id {:target target :cascade :C1} :target target}
   :action/value-sha256 (identity/digest {:id {:target target :cascade :C1} :target target})
   :action-at "2026-09-21T18:00:00Z"})
(def increment
  {:criterion {:id :increment :version 1 :kind :increment :target target
               :want [target :implemented]}
   :status :matched :valence :must
   :attestation {:status :present :want [target :implemented]
                 :warrant-id "test-registry-fixture" :verification :supplied-not-checked}})
(def route {:schema :wm/route-attestation-v1 :mode :record-only :status :declared
            :verification :supplied-checkpoint-evidence-only :increments [increment]})
(defn focus [class]
  {:schema :wm/focus-receipt-v1 :mode :record-only
   :candidates [{:target target :class class}]})
(def base-close
  {:outcome :grounded-change :grounded? true :artifact-only? false
   :occurrence occurrence :route-attestation route
   :route-attestation-ref {:status :present :path "/fixture" :sha256 (identity/digest route)}})
(defn result [close & {:keys [route focus declaration expected-digests]
                       :or {route (:route-attestation close)}}]
  (kernel/classify (cond-> {:close close :occurrence occurrence
                            :route-attestation route :focus-receipt focus}
                     declaration (assoc :declaration declaration)
                     expected-digests (assoc :expected-digests expected-digests))))

(deftest four-positive-classes-and-bad-cases
  (doseq [[facet expected] [[:focus :focus-increment]
                            [:associated :associated-increment]
                            [:useful-elsewhere :elsewhere-useful]]]
    (is (= expected (:class (result base-close :focus (focus facet)))))
    (is (= :unknown (:class (result (assoc base-close :route-attestation nil)
                                          :route nil :focus (focus facet))))))
  (let [failure (assoc base-close :outcome :build-failed :grounded? false
                       :route-attestation nil :failure-kind :compiler-refusal)]
    (is (= :known-typed-failure (:class (result failure :route nil))))
    (is (= :unknown (:class (result (dissoc failure :failure-kind) :route nil))))))

(deftest refusals-are-not-unknown
  (let [digest (identity/digest (kernel/projection base-close (kernel/declaration)))]
    (doseq [[expected receipt]
            [[:digest-drift (result base-close :focus (focus :focus)
                                    :expected-digests {:close-projection (apply str (repeat 64 "0"))})]
             [:unsupported-version (result base-close :focus (assoc (focus :focus) :schema :wm/focus-v2))]
             [:duplicate-attestations (result (assoc base-close :route-attestation
                                                     (assoc route :increments [increment increment]))
                                              :route (assoc route :increments [increment increment])
                                              :focus (focus :focus))]
             [:ambiguous-facet-rows (result base-close :focus
                                            (assoc (focus :focus) :candidates
                                                   [{:target target :class :focus}
                                                    {:target target :class :associated}]))]
             [:increment-failure-contradiction
              (result (assoc base-close :outcome :build-failed :grounded? false
                             :failure-kind :typed) :focus (focus :focus))]]]
      (is (= :refused (:status receipt)))
      (is (= expected (:kind receipt))))
    (is (= digest (kernel/projection-digest base-close (kernel/declaration))))))

(deftest malformed-missing-and-self-reference
  (is (= :malformed-close (:kind (kernel/classify {:close []}))))
  (let [r (result (dissoc base-close :grounded?) :focus (focus :focus))]
    (is (= :recorded (:status r)))
    (is (= :unknown (:class r)))
    (is (= [[:close-key :grounded?]] (:missing r))))
  (let [self {:schema :wm/run-ending-classification-receipt-v1}
        r (result (assoc-in base-close [:route-attestation :self] self)
                  :route (assoc route :self self) :focus (focus :focus))]
    (is (= :refused (:status r)))
    (is (= :self-reference (:kind r)))))

(deftest exact-replay-and-verification-level
  (let [input {:close base-close :occurrence occurrence :route-attestation route
               :focus-receipt (focus :focus)}
        a (kernel/classify input)
        b (kernel/classify (edn/read-string (pr-str input)))]
    (is (= (pr-str a) (pr-str b)))
    (is (= :supplied-checkpoint-evidence-only
           (get-in a [:sources :route-attestation :verification])))
    (is (= :supplied-not-checked (get-in a [:attestation :attestation :verification])))))

(deftest final-close-projection-verifier
  (let [event {:checkpoint/type :closed :cohort/id :cohort :attempt/id "attempt-001"
               :payload {:judgment (assoc base-close :duration-ms 12)}}
        receipt (kernel/classify {:close event :occurrence occurrence
                                  :route-attestation route :focus-receipt (focus :focus)})]
    (is (kernel/verify-close event receipt))
    (is (not (kernel/verify-close (assoc-in event [:payload :judgment :grounded?] false) receipt)))
    (is (kernel/verify-close (assoc-in event [:payload :judgment :duration-ms] 999) receipt))))

;; No byte-identity test here: the kernel runs at close, after the selection
;; checkpoint is written, and reads nothing selection consumes. A test that
;; assoc/dissoc'd a synthetic map could not fail, so it was removed (claude-3).

(deftest same-class-rows-for-one-target-are-one-relation
  (let [r (result base-close :focus (assoc (focus :focus) :candidates
                                           [{:target target :class :focus}
                                            {:target target :class :focus}]))]
    (is (= :recorded (:status r)))
    (is (= :focus-increment (:class r)))))

(def discovery-known-failures
  {"wm-full-loop-machinery-55/wm-contract-machinery-55-v1/attempt-003/007-closed.edn" :evidence-not-single-edn
   "wm-full-loop/wm-outer-loop-43-v1/attempt-053/007-closed.edn" :operator-terminated})

(deftest discovery-cohort-replay
  ;; The data root grows with every click, so pin the discovery's claims
  ;; rather than its total: every close written before this kernel existed
  ;; (no :run-ending-classification in its judgment) is :unknown or a typed
  ;; failure, never an increment; and the two named records are the typed
  ;; failures. 124 was the count at 6d59236f.
  (let [root (io/file "/home/joe/code/futon2/data")
        prefix (str (.getPath root) "/")
        files (filter #(and (.isFile %) (= "007-closed.edn" (.getName %))) (file-seq root))
        legacy (for [file files
                     :let [close (edn/read-string (slurp file))]
                     :when (not (contains? (get-in close [:payload :judgment])
                                           :run-ending-classification))]
                 [(subs (.getPath file) (count prefix)) (kernel/classify {:close close})])
        known (into {} (for [[path r] legacy :when (= :known-typed-failure (:class r))]
                         [path (:failure-kind r)]))]
    (is (<= 124 (count legacy)))
    (is (every? #{:unknown :known-typed-failure} (map (comp :class second) legacy)))
    (is (= discovery-known-failures known))))
