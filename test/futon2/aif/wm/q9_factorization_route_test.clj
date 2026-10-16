(ns futon2.aif.wm.q9-factorization-route-test
  "Requirements Q9 on the production route, negative only (codex-68 ruling
  (a), 2026-10-16). The runner seals a :q9 carrier from the C the selector
  compared; the exporter reads only a verified carrier and exports
  factorizedPreference := none, so Q9 is false. Absent, tampered, foreign,
  incomplete and self-asserted positive carriers are not recomputable. The
  runs are the Q8 route test's production clicks."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.q9-factorization :as q9]
            [futon2.aif.reading-policy-preference :as pref]
            [futon2.aif.wm.q8-frontier-route-test :as q8-route]
            [futon2.test-support.reading-route :as route]
            [futon2.test-support.runner-fixture :as fixture]
            [wm-check-run-facts :as lean-term]
            [wm-report-card :as card]
            [wm-run-facts :as facts]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)
(use-fixtures :each
  (fn [run]
    (binding [runner/*wm-status-reporting?* false]
      (route/with-empty-learning-ledger run))))

(def click! @#'q8-route/click!)

(def fields ["factorizedPreference" "calibrationAuthorizations"
             "factorizationAuthorizations"])

(defn- export [record]
  (json/parse-string (json/generate-string (facts/facts-for-record record "r" {}))))

(defn- q9-term [record]
  (re-find #"q9 := .*" (lean-term/lean-source (export record) "q9")))

(defn- verdict [record]
  (:status (first (filter #(= :Q9 (:requirement %))
                          (card/verdict-rows (facts/facts-for-record record "r" {}))))))

(defn- nr? [v] (and (map? v) (contains? v "not-recomputable")))

(defn- not-recomputable?
  "Every Q9 field of RECORD's export is not recomputable, and the Lean term
  says so."
  [record]
  (let [f (get (export record) "facts")]
    (and (every? #(nr? (get f %)) fields)
         (some? (re-find #"q9 := \.nr" (q9-term record)))
         (= :not-recomputable (verdict record)))))

(defn- reseal
  "RECORD with its :q9 carrier recomputed after F rewrites the record, as a
  consistent forger would."
  [record f]
  (let [r (f record)]
    (assoc r :q9 (q9/certificate (q9/record-inputs r)))))

(defn- one-click []
  (:record (click! (route/fixture) (route/temp-dir "q9-stream-"))))

(def positive-carrier
  "A well-formed Q9 certificate with registries (wm-run-facts-test's), the
  shape a commissioned authority would supply. Nothing produces one."
  {:schema :wm/factorized-preference-v1
   :certificate
   {:calibration {:identity 101 :version 1 :authority 102 :registration-token 103
                  :focused-cost 0 :related-cost 1 :unrelated-cost 2}
    :selector-authority 108
    :factorization-identity 106 :factorization-version 1
    :factorization-authority 107
    :calibration-authorization 104 :factorization-authorization 105}
   :calibration-authorizations
   [{:receipt 104
     :calibration {:identity 101 :version 1 :authority 102 :registration-token 103
                   :focused-cost 0 :related-cost 1 :unrelated-cost 2}
     :authorized-by 102}]
   :factorization-authorizations
   [{:receipt 105 :identity 106 :version 1 :authority 107
     :law :completion-then-conditional-relevance}]})

;; -- the schedule reading, recomputed -----------------------------------------------

(deftest completion-ordering-is-read-from-the-weights
  (testing "the production C: every n puts some uncompleted outcome first"
    (doseq [n [1 2 5 8]]
      (let [{:keys [lexicographic? witness]} (q9/completion-ordering
                                              (pref/recompute-progress-preference n 2))]
        (is (false? lexicographic?) n)
        (is (= [n false] (:uncompleted witness)) n)
        (is (= [0 true] (:completed witness)) n)
        (is (>= (:uncompleted-weight witness) (:completed-weight witness))))))
  (testing "the n = 5 pair at comparable progress the diagnosis named"
    (let [d (get (pref/recompute-progress-preference 5 1) 1)]
      (is (> (d [4 false]) (d [1 true])))))
  (testing "a completion-first schedule is lexicographic; a tie is not"
    (let [lex {1 {[0 false] 0.05 [1 false] 0.1 [0 true] 0.35 [1 true] 0.5}}
          tie {1 {[0 false] 0.1 [1 false] 0.3 [0 true] 0.3 [1 true] 0.3}}]
      (is (= {:lexicographic? true :witness nil} (q9/completion-ordering lex)))
      (is (= [1 false] (:uncompleted (:witness (q9/completion-ordering tie)))))))
  (testing "the outcome space: only [count met?] pairs are count-and-completion"
    (is (= :count-and-completion
           (q9/outcome-space (pref/recompute-progress-preference 3 2))))
    (is (= :unrecognized
           (q9/outcome-space {1 {[0 false :focused] 0.5 [1 true :related] 0.5}})))
    (is (= :unrecognized (q9/outcome-space {1 {:focused 0.55 :related 0.45}})))
    (is (= :unrecognized (q9/outcome-space {})))))

;; -- the production route: Q9 violated ---------------------------------------------

(deftest the-selected-c-certifies-q9-false
  (let [fx (route/fixture)
        dir (route/temp-dir "q9-stream-")
        one (:record (click! fx dir))
        two (:record (click! fx dir))]
    (doseq [[label rec] {:first one :repeat two}]
      (testing (name label)
        (let [c (:q9 rec)
              cert (get-in rec [:decision :selection-certificate])
              receipts (into {} (map (juxt :policy-id identity)) (:preference-receipts cert))]
          (is (= {:status :verified :carrier c} (q9/verify-record rec)))
          (is (= :factorization-absent (:status c)))
          (is (= (:run/id rec) (:run/id c)) "bound to this run")
          (testing "the compared Cs are Q4's compared policies, the selected among them"
            (is (= (get-in cert [:q4 :compared]) (:compared c)))
            (is (= (:compared c) (mapv :policy-key (:policies c))))
            (is (some #{(:selected c)} (:compared c))))
          (testing "each C is the receipt's schedule, recomputed from its authority"
            (doseq [p (:policies c)
                    :let [r (receipts (:policy-id p))]]
              (is (= (evidence/value-digest r) (:receipt p)))
              (is (= (evidence/value-digest (:preference r)) (:schedule p)))
              (is (= (:preference r)
                     (pref/recompute-progress-preference
                      (get-in p [:authority :progress-tokens]) (:horizon p))))
              (is (= :count-and-completion (:outcome-space p)))
              (let [w (get-in p [:completion :witness])
                    d (get (:preference r) (:step w))]
                (is (false? (get-in p [:completion :lexicographic?])))
                (is (= (d (:uncompleted w)) (:uncompleted-weight w)))
                (is (= (d (:completed w)) (:completed-weight w)))
                (is (>= (:uncompleted-weight w) (:completed-weight w))))
              (is (= {:source "cascade_shape_g arrangement model"
                      :provenance-status :synthetic :calibrated false}
                     (select-keys (:authority p) [:source :provenance-status :calibrated])))))
          (testing "the declared authority is the decision's own C"
            (is (= :cascade-shape-g/progress-preference (get-in c [:declared :preference :c])))
            (is (= (:reading-precision-identity cert) (get-in c [:declared :precision-identity]))))
          (is (= [:no-relevance-coordinate :completion-not-lexicographic
                  :calibration-authority-is-selector
                  :no-calibration-registry :no-factorization-registry]
                 (:reasons c)))
          (testing "export: none, empty registries; Lean and the report card read false"
            (let [f (get (export rec) "facts")]
              (is (nil? (get f "factorizedPreference")))
              (is (contains? f "factorizedPreference"))
              (is (= [] (get f "calibrationAuthorizations") (get f "factorizationAuthorizations"))))
            (is (= "q9 := .known { Requirements.click20 with factorizedPreference := none, calibrationAuthorizations := [], factorizationAuthorizations := [] }"
                   (q9-term rec)))
            (is (= :fail (verdict rec)))))))))

(deftest reasons-are-recomputed-not-read-from-labels
  (let [rec (one-click)]
    (testing "a calibrated label on the receipt authority drops only that reason"
      (let [r (reseal rec #(update-in % [:decision :selection-certificate :preference-receipts]
                                      (partial mapv (fn [x] (assoc-in x [:authority :calibrated] true)))))]
        (is (= :factorization-absent (get-in r [:q9 :status])))
        (is (= [:no-relevance-coordinate :completion-not-lexicographic
                :no-calibration-registry :no-factorization-registry]
               (get-in r [:q9 :reasons])))))
    (testing "a carrier claiming completion first is not the record's"
      (let [r (update rec :q9 #(q9/seal (-> (dissoc % :sha256)
                                            (update :policies
                                                    (partial mapv (fn [p] (assoc p :completion {:lexicographic? true :witness nil}))))
                                            (update :reasons (partial filterv (fn [x] (not= :completion-not-lexicographic x)))))))]
        (is (= :q9-carrier-not-bound-to-record (:reason (q9/verify-record r))))
        (is (not-recomputable? r))))))

;; -- adversarial: never false or true by default -----------------------------------

(deftest absent-tampered-foreign-and-incomplete-carriers-are-not-recomputable
  (let [fx (route/fixture)
        dir (route/temp-dir "q9-stream-")
        one (:record (click! fx dir))
        two (:record (click! fx dir))]
    (doseq [[label rec reason]
            [[:absent (dissoc two :q9) :q9-carrier-absent]
             [:tampered (assoc-in two [:q9 :status] :factorization-absent-x) :q9-carrier-tampered]
             [:edited-reason (update-in two [:q9 :reasons] pop) :q9-carrier-tampered]
             [:foreign (assoc two :q9 (:q9 one)) :q9-carrier-not-bound-to-record]
             [:incomplete (update two :q9 #(q9/seal (-> (dissoc % :sha256)
                                                         (update :policies (comp vec rest))
                                                         (update :compared (comp vec rest)))))
              :q9-carrier-not-bound-to-record]
             [:wrong-schema (update two :q9 #(q9/seal (assoc (dissoc % :sha256)
                                                             :schema :wm/q9-factorization-v0)))
              :q9-carrier-tampered]]]
      (testing (name label)
        (is (= reason (:reason (q9/verify-record rec))))
        (is (not-recomputable? rec))))))

(deftest self-asserted-positive-carriers-are-refused
  (let [rec (one-click)]
    (testing "a sealed :q9 with a positive status"
      (let [r (update rec :q9 #(q9/seal (-> (dissoc % :sha256)
                                            (assoc :status :factorized :certificate positive-carrier)
                                            (dissoc :reasons))))]
        (is (= :q9-positive-carrier-refused (:reason (q9/verify-record r))))
        (is (not-recomputable? r))))
    (testing "an unsealed factorized carrier in the decision, :q9 resealed around it"
      (doseq [path [[:decision :factorized-preference]
                    [:decision :selection-certificate :factorized-preference]
                    [:decision :selection-certificate :calibration-authorizations]]]
        (let [r (reseal rec #(assoc-in % path positive-carrier))]
          (is (= :self-asserted-factorization-carrier (get-in r [:q9 :reason])) path)
          (is (= :self-asserted-factorization-carrier (:reason (q9/verify-record r))))
          (is (not-recomputable? r)))))
    (testing "the same unsealed carrier without a resealed :q9 is not the record's"
      (let [r (assoc-in rec [:decision :factorized-preference] positive-carrier)]
        (is (= :q9-carrier-not-bound-to-record (:reason (q9/verify-record r))))
        (is (not-recomputable? r))))))

(deftest a-c-that-cannot-be-recomputed-certifies-nothing
  (let [rec (one-click)
        receipts [:decision :selection-certificate :preference-receipts]]
    (doseq [[label f reason]
            [[:schedule-edited
              (fn [r] (update-in r receipts
                                 (partial mapv (fn [x] (update-in x [:preference 1]
                                                                  (fn [d] (update-vals d (constantly (/ 1.0 (count d))))))))))
              :preference-not-recomputable]
             [:receipt-dropped
              (fn [r] (update-in r receipts (comp vec rest)))
              :compared-policy-without-receipt]
             [:receipt-duplicated
              (fn [r] (update-in r receipts #(conj % (first %))))
              :competing-receipts]
             [:g-not-the-compared-g
              (fn [r] (update-in r receipts (partial mapv #(update % :g + 1.0))))
              :receipt-not-bound-to-compared-g]
             [:authority-lost
              (fn [r] (update-in r receipts (partial mapv #(dissoc % :authority))))
              :preference-authority-lost]
             [:not-progress-count
              (fn [r] (update-in r receipts (partial mapv #(assoc-in % [:authority :kind] :class-preference))))
              :preference-kind-not-recomputable]
             [:selected-not-compared
              (fn [r] (assoc-in r [:decision :selection-law :per-policy-argmax :action] {:target "M-x"}))
              :selected-policy-not-compared]
             [:other-route
              (fn [r] (update-in r [:decision :selection-certificate] dissoc :selection-route))
              :not-the-reading-family-route]]]
      (testing (name label)
        (let [r (reseal rec f)]
          (is (= :not-recomputable (get-in r [:q9 :status])))
          (is (= reason (get-in r [:q9 :reason])))
          (is (= reason (:reason (q9/verify-record r))))
          (when-not (= :other-route label)
            (is (not-recomputable? r))))))))

(deftest an-abstention-certifies-nothing
  (let [fx (route/fixture)]
    (spit (io/file (:root fx) (str route/target ".md")) "# Current\n\n## HEAD\nChanged work.\n")
    (let [rec (:record (click! fx (route/temp-dir "q9-stream-")))]
      (is (= :abstained (get-in rec [:decision :abstention :status])))
      (is (= :not-recomputable (get-in rec [:q9 :status])))
      (is (not= :verified (:status (q9/verify-record rec))))
      (is (every? #(nr? (get (get (export rec) "facts") %)) fields)))))
