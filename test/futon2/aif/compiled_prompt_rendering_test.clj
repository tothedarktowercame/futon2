(ns futon2.aif.compiled-prompt-rendering-test
  "Trial 5 (one-click-frontier-2026-10-22 ordinary-trial-5) dispatch rendered
  :no-renderable-criteria and a pattern dump with zero completion checks for
  a :compiled-cascade construction that actually carries 19 reviewed
  operational steps (dispatch-criteria-replay.clj).  These tests prove the
  production prompt path now exposes exactly what the retained construction
  records -- and that a malformed or absent compiled plan stays typed-absent
  instead of silently rendering as complete."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.cascade-plan :as cascade-plan]
            [futon2.aif.full-loop-runner :as runner]))

(def trial-dir
  "holes/labs/wm-contract/runs/one-click-frontier-2026-10-22/ordinary-trial-5")

(def frozen-construction-gz-sha256
  "7fb089be87eded78b1972ebdd26d93192d0c7812569c17614d3b9808a4bba67b")

(defonce frozen-construction
  (delay
   (let [order (fn [xs]
                 (let [idx (zipmap xs (range))]
                   (fn [a b]
                     (if (= a b) 0
                         (compare [(get idx a (count idx)) (pr-str a)]
                                  [(get idx b (count idx)) (pr-str b)])))))
         readers {'wm/keyword (fn [[n k]] (keyword n k))
                  'wm/map (fn [pairs] (into (sorted-map-by (order (mapv first pairs))) pairs))
                  'wm/set (fn [xs] (into (sorted-set-by (order xs)) xs))}]
     (with-open [in (java.util.zip.GZIPInputStream.
                     (io/input-stream (str trial-dir "/construction.edn.gz")))]
       (-> (edn/read-string {:readers readers} (slurp in))
           (get-in [:result :value :construction]))))))

(def selected-cascade-digest
  "c601dc5697a2d5d65c5c8440f08b3905cee1030b9a62dfa03e2ad5e7ab202c27")

(def compiled-cascade-digest
  "f2d6feed1a394036e4891266361797bad256316bba3e3ec451dee8c204d257c1")

(defn- frozen-steps []
  (get-in @frozen-construction [:operational-plan :steps]))

(defn- sha256-of [path]
  (with-open [in (io/input-stream path)
              out (java.io.ByteArrayOutputStream.)]
    (io/copy in out)
    (.toString (java.math.BigInteger. 1 (.digest (java.security.MessageDigest/getInstance "SHA-256")
                                                 (.toByteArray out)))
               16)))

(deftest frozen-construction-is-the-retained-artifact
  (is (= frozen-construction-gz-sha256
         (sha256-of (str trial-dir "/construction.edn.gz")))
      "frozen construction drifted"))

(deftest production-acceptance-block-renders-all-compiled-checks
  (let [block (#'runner/acceptance-criterion-block
               {:action @frozen-construction})
        steps (frozen-steps)]
    (is (string? block)
        (pr-str (when (map? block) (select-keys block [:status :reason]))))
    (is (str/includes? block selected-cascade-digest))
    (is (str/includes? block compiled-cascade-digest))
    (is (= 19 (count steps)))
    (doseq [step steps]
      (testing (pr-str (:occurrence-id step))
        (is (str/includes? block (pr-str (:occurrence-id step))))
        (is (str/includes? block
                           (pr-str (get-in step [:clause :completion-check])))
            "completion check must appear VERBATIM")))))

(deftest production-prompt-plan-text-renders-all-compiled-steps
  (let [plan (cascade-plan/cascade-plan-text @frozen-construction)
        steps (frozen-steps)]
    (is (str/includes? plan selected-cascade-digest))
    (is (str/includes? plan compiled-cascade-digest))
    (doseq [step steps]
      (is (str/includes? plan (pr-str (:occurrence-id step))))
      (is (str/includes? plan (pr-str (get-in step [:clause :completion-check]))))
      (is (str/includes? plan (str "operation: " (get-in step [:clause :operation])))))
    (is (= 19 (count (re-seq #"(?m)^step \[" plan))))))

(deftest compiled-plan-rendering-facts
  (let [rendered (cascade-plan/compiled-operational-plan-text @frozen-construction)]
    (is (= :rendered (:status rendered)))
    (is (= 19 (:steps-total rendered)))
    (is (= 19 (:steps-shown rendered))
        "the real 19-step plan must fit the budget with nothing dropped")
    (is (zero? (:omitted rendered)))
    (is (str/includes? (:text rendered) "authored by:"))
    (is (str/includes? (:text rendered) "provenance:"))))

(deftest malformed-compiled-plan-stays-typed-absent
  (testing "operational plan absent"
    (is (= {:status :absent :reason :compiled-operational-plan-not-renderable}
           (cascade-plan/compiled-operational-plan-text
            (dissoc @frozen-construction :operational-plan)))))
  (testing "steps empty"
    (is (= {:status :absent :reason :compiled-operational-plan-not-renderable}
           (cascade-plan/compiled-operational-plan-text
            (assoc-in @frozen-construction [:operational-plan :steps] [])))))
  (testing "steps not maps"
    (is (= {:status :absent :reason :compiled-operational-plan-not-renderable}
           (cascade-plan/compiled-operational-plan-text
            (assoc-in @frozen-construction [:operational-plan :steps]
                      [:not-a-step])))))
  (testing "digests missing"
    (is (= {:status :absent :reason :compiled-operational-plan-digests-missing}
           (cascade-plan/compiled-operational-plan-text
            (-> @frozen-construction
                (assoc-in [:operational-plan :selected-cascade-digest] nil))))))
  (testing "the acceptance block cannot silently render a malformed plan as complete"
    (is (= {:status :absent :reason :compiled-operational-plan-not-renderable}
           (#'runner/acceptance-criterion-block
            {:action (dissoc @frozen-construction :operational-plan)}))))
  (testing "the plan text says NOT RENDERABLE rather than dumping legacy noise"
    (let [plan (cascade-plan/cascade-plan-text
                (dissoc @frozen-construction :operational-plan))]
      (is (str/includes? plan "COMPILED OPERATIONAL PLAN: NOT RENDERABLE"))
      (is (str/includes? plan "compiled-operational-plan-not-renderable")))))

;; The exact shape the independent review (compiled-renderer-review-probe.clj)
;; executed against 3c85d21b4: two string digests with steps [{}] used to
;; render :rendered and label nil as a reviewed completion check.
(defn- minimal-plan-construction [steps]
  {:construction-kind :compiled-cascade
   :operational-plan {:selected-cascade-digest "s"
                      :compiled-cascade-digest "c"
                      :steps steps}})

(defn- valid-step [occ]
  {:occurrence-id [:occ occ]
   :clause {:operation (str "op-" occ)
            :capability :cap
            :completion-check (list 'wm/check occ)}})

(deftest malformed-steps-are-typed-absent-never-rendered
  (testing "exact review repro: steps [{}] with two string digests"
    (is (= {:status :absent :reason :compiled-operational-plan-step-invalid
            :detail :step-occurrence-id-missing}
           (cascade-plan/compiled-operational-plan-text
            (minimal-plan-construction [{}]))))
    (is (= {:status :absent :reason :compiled-operational-plan-step-invalid
            :detail :step-occurrence-id-missing}
           (cascade-plan/compiled-completion-criteria-text
            (minimal-plan-construction [{}])))))
  (testing "missing completion check"
    (is (= {:status :absent :reason :compiled-operational-plan-step-invalid
            :detail :step-completion-check-missing}
           (cascade-plan/compiled-operational-plan-text
            (minimal-plan-construction
             [(-> (valid-step 1)
                  (update-in [:clause] dissoc :completion-check))])))))
  (testing "malformed (nil) completion check"
    (is (= {:status :absent :reason :compiled-operational-plan-step-invalid
            :detail :step-completion-check-missing}
           (cascade-plan/compiled-operational-plan-text
            (minimal-plan-construction
             [(assoc-in (valid-step 1) [:clause :completion-check] nil)])))))
  (testing "clause absent entirely"
    (is (= {:status :absent :reason :compiled-operational-plan-step-invalid
            :detail :step-clause-missing}
           (cascade-plan/compiled-operational-plan-text
            (minimal-plan-construction
             [(dissoc (valid-step 1) :clause)])))))
  (testing "the acceptance block relays the typed absence, never nil text"
    (is (= {:status :absent :reason :compiled-operational-plan-step-invalid
            :detail :step-completion-check-missing}
           (#'runner/acceptance-criterion-block
            {:action (minimal-plan-construction
                      [(assoc-in (valid-step 1) [:clause :completion-check] nil)])})))))

(deftest duplicate-occurrence-ids-are-rejected
  (is (= {:status :absent :reason :compiled-operational-plan-step-invalid
          :detail :duplicate-occurrence-id}
         (cascade-plan/compiled-operational-plan-text
          (minimal-plan-construction [(valid-step 1) (valid-step 1)]))))
  (is (= {:status :absent :reason :compiled-operational-plan-step-invalid
          :detail :duplicate-occurrence-id}
         (cascade-plan/compiled-completion-criteria-text
          (minimal-plan-construction [(valid-step 1) (valid-step 1)])))))

(deftest criteria-preserve-recorded-order
  ;; into {} keyed by occurrence+operation used to collapse entries and lose
  ;; recorded ordering; the criteria must list checks in recorded step order.
  (let [steps [(valid-step :b) (valid-step :a) (valid-step :c)]
        crit (cascade-plan/compiled-completion-criteria-text
              (minimal-plan-construction steps))]
    (is (= (mapv :occurrence-id steps)
           (mapv :occurrence-id
                 (sort-by #(str/index-of crit (pr-str (:occurrence-id %))) steps))))
    (is (= 3 (count (re-seq #"\(op-" crit))))))

(deftest plan-character-budget-includes-header-and-omission-marker
  (testing "large recorded header cannot break the 18,000-char promise"
    (let [big-guards (apply str (repeat 4000 "g"))
          rendered (cascade-plan/compiled-operational-plan-text
                    (-> (minimal-plan-construction (map valid-step (range 60)))
                        (assoc-in [:operational-plan :guards] [big-guards])))
          text (:text rendered)]
      (is (= :rendered (:status rendered)))
      (is (<= (count text) 18000)
          (str "final text with header and marker is " (count text)))
      (is (pos? (:omitted rendered)))
      (is (str/includes? text "omitted for space]"))))
  (testing "large digest headers count against the criteria budget too"
    (let [big (apply str (repeat 11000 "d"))
          rendered (cascade-plan/compiled-completion-criteria-text
                    (-> (minimal-plan-construction (map valid-step (range 60)))
                        (assoc-in [:operational-plan :compiled-cascade-digest] big)))]
      (is (string? rendered))
      (is (<= (count rendered) 12000)
          (str "final criteria text is " (count rendered)))
      (is (str/includes? rendered "omitted for space]"))))
  (testing "omissions are explicitly counted, never silent"
    (let [rendered (cascade-plan/compiled-operational-plan-text
                    (minimal-plan-construction
                     (map (fn [i] (assoc-in (valid-step i)
                                            [:clause :completion-check]
                                            (apply list (repeat 200 (list 'wm/x i)))))
                          (range 200))))
          text (:text rendered)]
      (is (pos? (:omitted rendered)))
      (is (= 200 (:steps-total rendered)))
      (is (= (:steps-shown rendered)
             (count (re-seq #"(?m)^step " text))))
      (is (<= (count text) 18000))
      (is (re-find (re-pattern (str "\\[" (:steps-shown rendered)
                                   " of 200 operational steps shown; "
                                   (:omitted rendered) " omitted"))
                   text)))))


(deftest legacy-construction-rendering-is-preserved
  ;; A legacy (non-compiled) construction still renders the pattern section
  ;; exactly as before; the compiled branch never fires.
  (let [legacy {:construction-kind :pattern-cascade
                :shown ["p/a" "p/b"]
                :wiring {:boxes [{:id "p/a" :produces ["t1"]}
                                 {:id "p/b"}]
                         :wires []}}
        plan (cascade-plan/cascade-plan-text legacy)]
    (is (str/includes? plan "pattern 1: p/a"))
    (is (str/includes? plan "pattern 2: p/b"))
    (is (str/includes? plan "produces: t1"))
    (is (not (str/includes? plan "COMPILED OPERATIONAL PLAN"))))
  ;; and the legacy acceptance path still returns its typed absence for a
  ;; legacy action with nothing renderable (unchanged behavior).
  (is (= {:status :absent :reason :no-renderable-criteria}
         (#'runner/acceptance-criterion-block
          {:action {:construction-kind :pattern-cascade
                    :selected-action {:target "M-x"
                                      :pattern {:produces []}}}}))))
