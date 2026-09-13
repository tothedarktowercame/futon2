(ns futon2.aif.machine-slow-feedback-provenance-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.machine-slow-feedback-provenance :as provenance]
            [futon2.aif.machine-slow-state-carrier :as carrier]
            [futon2.aif.machine-slow-state-carrier-test :as carrier-test])
  (:import (java.nio.file Files)
           (java.security MessageDigest)
           (java.util Base64)))

(def capture-root
  "holes/labs/wm-contract/runs/row-22-e6b-canonical-fixture-2026-09-13/captured")
(def replay-root "holes/labs/wm-contract/runs/row-22-e6b-canonical-replay-2026-09-13")
(defn- sha256 [^bytes bs]
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (doto (MessageDigest/getInstance "SHA-256") (.update bs))))))
(defn- descriptor [path]
  (let [bs (Files/readAllBytes (.toPath (io/file path)))
        value (edn/read-string (String. bs "UTF-8"))]
    {:bytes/base64 (.encodeToString (Base64/getEncoder) bs)
     :source-sha256 (sha256 bs)
     :value-sha256 (sha256 (.getBytes (pr-str value) "UTF-8"))}))
(defn- value-descriptor [value]
  (let [bs (.getBytes (pr-str value) "UTF-8")]
    {:bytes/base64 (.encodeToString (Base64/getEncoder) bs)
     :source-sha256 (sha256 bs) :value-sha256 (sha256 bs)}))
(defn- record-at [input path]
  (edn/read-string
   (String. (.decode (Base64/getDecoder) (get-in input (conj path :bytes/base64))) "UTF-8")))
(defn- update-record [input path f & args]
  (assoc-in input path (value-descriptor (apply f (record-at input path) args))))
(defn- replace-original [input role record]
  (let [d (value-descriptor record)]
    (-> input
        (assoc-in [:original-sources role] d)
        (assoc-in [:proposal-evidence :input/digests role] (:value-sha256 d))
        (assoc-in [:proposal-evidence :source/digests role] (:source-sha256 d)))))
(defn- with-e3-output-sources [input f]
  (let [e3 (update (record-at input [:canonical-outputs :e3]) :sources f)
        d (value-descriptor e3)
        e2b (assoc (record-at input [:original-sources :e2b-subject])
                   :canonical/e3-digest (:value-sha256 d))
        relation (assoc-in (record-at input [:original-sources :lifecycle-relation])
                           [:subject :e3/digest] (:value-sha256 d))
        changed (-> input
                    (assoc-in [:canonical-outputs :e3] d)
                    (assoc-in [:proposal-evidence :canonical/digests :e3] (:value-sha256 d))
                    (replace-original :e2b-subject e2b)
                    (replace-original :lifecycle-relation relation))]
    (assoc changed :carrier-projection
           (carrier/project-transition (select-keys changed [:proposal-evidence :original-sources])))))
(def closure-paths
  {:e3/pending "e3/pending.edn" :e3/verdict "e3/verdict.edn"
   :e3/review "e3/review.edn" :r9/input "config/r9-input.edn"
   :config/e1 "config/e1-config.edn" :config/e2b "config/e2b-config.edn"
   :config/e3 "config/e3-config.edn" :config/canonical "config/canonical-config.edn"
   :e1/ranked-support "../../row-22-e1-authority-resolution-2026-09-13/fixtures/ranked-support.edn"
   :e1/field-membership "../../row-22-e1-authority-resolution-2026-09-13/fixtures/field-membership.edn"
   :e1/costs "../../row-22-e1-authority-resolution-2026-09-13/fixtures/costs.edn"
   :e1/utilities "../../row-22-e1-authority-resolution-2026-09-13/fixtures/utilities.edn"
   :e1/budgets "../../row-22-e1-authority-resolution-2026-09-13/fixtures/budgets.edn"
   :e2b/context "../../row-22-e2b-correspondence-2026-09-13/fixtures/context.edn"
   :e2b/selection "../../row-22-e2b-correspondence-2026-09-13/fixtures/selection.edn"
   :e2b/enactment "../../row-22-e2b-correspondence-2026-09-13/fixtures/enactment.edn"})
(defn- input []
  (let [bundle (#'carrier-test/bundle)
        projection (carrier/project-transition bundle)]
    {:proposal-evidence (:proposal-evidence bundle)
     :original-sources (:original-sources bundle)
     :carrier-projection projection
     :canonical-closure
     (into {} (map (fn [[role relative]]
                     [role (descriptor (str capture-root "/" relative))]) closure-paths))
     :canonical-outputs
     {:e3 (descriptor (str replay-root "/e3-output.edn"))
      :e2b (descriptor (str replay-root "/e2b-output.edn"))}
     :expected-head {:store/id "isolated-store-1" :generation 0
                     :transaction-sha256 (apply str (repeat 64 "a"))
                     :state/revision (get-in projection [:prior :carrier :state/revision])
                     :state-sha256 (get-in projection [:prior :sha256])}}))
(defn- refusal [x]
  (try (provenance/construct x) nil
       (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))

(deftest deterministic-complete-provenance-envelope
  (let [a (provenance/construct (input)) b (provenance/construct (input))]
    (is (= a b))
    (is (= :structural-artifact (:status a)))
    (is (= :none (:authority/status a)))
    (is (= #{:context :prior :e2b :outcome}
           (set (keys (get-in a [:record :retrospective-application-view :input/digests])))))
    (is (= (get-in a [:record :carrier-projection :digest-roles :complete-next-record])
           (get-in a [:record :retrospective-application-view :output/digest])))
    (is (= (set provenance/closure-roles)
           (set (keys (get-in a [:record :canonical-closure :inputs])))))))

(deftest missing-corrupt-and-borrowed-closure-refusals
  (is (= :e6b-provenance/closure-incomplete
         (refusal (update (input) :canonical-closure dissoc :r9/input))))
  (is (= :e6b-provenance/digest-mismatch
         (refusal (assoc-in (input) [:canonical-closure :e3/pending :source-sha256]
                            (apply str (repeat 64 "0"))))))
  (let [i (input) e3 (get-in i [:canonical-outputs :e3])
        value (assoc (edn/read-string
                      (String. (.decode (Base64/getDecoder) (:bytes/base64 e3)) "UTF-8"))
                     :identity {:model/id :borrowed})
        bs (.getBytes (pr-str value) "UTF-8")]
    (is (= :e6b-provenance/closure-join-mismatch
           (refusal (assoc-in i [:canonical-outputs :e3]
                              {:bytes/base64 (.encodeToString (Base64/getEncoder) bs)
                               :source-sha256 (sha256 bs) :value-sha256 (sha256 bs)}))))))

(deftest carrier-head-and-interface-refusals
  (doseq [[label changed expected]
          [[:carrier #(assoc-in % [:carrier-projection :prior :sha256]
                               (apply str (repeat 64 "0")))
            :e6b-provenance/carrier-projection-mismatch]
           [:head-revision #(assoc-in % [:expected-head :state/revision] "borrowed")
            :e6b-provenance/expected-head-mismatch]
           [:head-pin #(assoc-in % [:expected-head :state-sha256]
                                (apply str (repeat 64 "0")))
            :e6b-provenance/expected-head-mismatch]
           [:candidate-bool #(assoc % :verified? true) :e6b-provenance/schema-invalid]]]
    (testing (name label) (is (= expected (refusal (changed (input))))))))

(deftest canonical-input-output-closure-refusals
  (testing "coherently re-pinned E3 input cannot borrow a stale canonical output"
    (let [i (update-record (input) [:canonical-closure :e3/pending]
                           assoc :run/id "borrowed-run")
          pending-pin (get-in i [:canonical-closure :e3/pending :source-sha256])
          i (update-record i [:canonical-closure :config/e3]
                           assoc-in [:evidence :pending :sha256] pending-pin)
          e3-config (record-at i [:canonical-closure :config/e3])
          i (update-record i [:canonical-closure :config/canonical] assoc :e3 e3-config)]
      (is (= :e6b-provenance/closure-join-mismatch (refusal i)))))
  (testing "coherently re-pinned E2b witness set cannot borrow a stale output"
    (let [roles [:e2b/context :e2b/selection :e2b/enactment]
          i (reduce (fn [x role]
                      (update-record x [:canonical-closure role]
                                     assoc-in [:subject :identity :run/id] "borrowed-run"))
                    (input) roles)
          witness-pins (into {} (map (fn [role]
                                       [(keyword (name role))
                                        (get-in i [:canonical-closure role :source-sha256])]) roles))
          i (reduce (fn [x [role pin]]
                      (update-record x [:canonical-closure :config/e2b]
                                     assoc-in [:witnesses role :sha256] pin))
                    i witness-pins)
          e2b-config (record-at i [:canonical-closure :config/e2b])
          i (update-record i [:canonical-closure :config/canonical] assoc :e2b e2b-config)]
      (is (= :e6b-provenance/closure-join-mismatch (refusal i)))))
  (testing "coherently re-pinned E1 source cannot hide behind stale E3/E2b outputs"
    (let [i (update-record (input) [:canonical-closure :e1/ranked-support]
                           assoc :borrowed true)
          pin (get-in i [:canonical-closure :e1/ranked-support :source-sha256])
          i (update-record i [:canonical-closure :config/e1]
                           assoc-in [:sources :ranked-support :sha256] pin)
          i (update-record i [:canonical-closure :config/e2b]
                           assoc-in [:e2a-resolver :sources :ranked-support :sha256] pin)
          i (update-record i [:canonical-closure :config/e3]
                           assoc-in [:e2a-resolver :sources :ranked-support :sha256] pin)
          e2b-config (record-at i [:canonical-closure :config/e2b])
          e3-config (record-at i [:canonical-closure :config/e3])
          i (update-record i [:canonical-closure :config/canonical]
                           assoc :e2b e2b-config :e3 e3-config)]
      (is (= :e6b-provenance/closure-join-mismatch (refusal i)))))
  (testing "cross-subject review/admission cannot be coherently re-labelled"
    (let [borrowed (apply str (repeat 64 "b"))
          i (update-record (input) [:canonical-closure :r9/input]
                           assoc-in [:subject :digest] borrowed)
          r9 (record-at i [:canonical-closure :r9/input])
          i (update-record i [:canonical-closure :e3/review] assoc :r9/input r9)
          i (update-record i [:canonical-closure :e3/verdict]
                           assoc-in [:canonical-admission :subject-digest] borrowed)
          review-pin (get-in i [:canonical-closure :e3/review :source-sha256])
          verdict-pin (get-in i [:canonical-closure :e3/verdict :source-sha256])
          i (update-record i [:canonical-closure :config/e3]
                           #(-> % (assoc-in [:evidence :review :sha256] review-pin)
                                (assoc-in [:evidence :verdict :sha256] verdict-pin)))
          e3-config (record-at i [:canonical-closure :config/e3])
          i (update-record i [:canonical-closure :config/canonical] assoc :e3 e3-config)]
      (is (= :e6b-provenance/closure-join-mismatch (refusal i))))))

(deftest ordered-canonical-source-manifest-refusals
  (testing "lead control: coherently propagated reversed E3 manifest still refuses"
    (is (= :e6b-provenance/source-manifest-invalid
           (refusal (with-e3-output-sources (input) #(vec (reverse %)))))))
  (testing "duplicate and extra E3 source roles cannot collapse through map conversion"
    (doseq [mutate [#(conj % (first %))
                    #(conj % {:label :extra :sha256 (apply str (repeat 64 "e"))})]]
      (is (= :e6b-provenance/source-manifest-invalid
             (refusal (with-e3-output-sources (input) mutate))))))
  (testing "duplicate E2b witness and E1 subject pins refuse"
    (doseq [path [[:canonical-outputs :e2b :sources :witnesses]
                  [:canonical-outputs :e2b :subject :e1-source-pins]]]
      (let [i (update-record (input) [:canonical-outputs :e2b]
                             update-in (subvec (vec path) 2) #(conj % (first %)))]
        (is (= :e6b-provenance/source-manifest-invalid (refusal i)))))))
