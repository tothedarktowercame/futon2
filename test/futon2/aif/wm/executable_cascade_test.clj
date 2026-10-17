(ns futon2.aif.wm.executable-cascade-test
  "Packet A focused tests: the canonical :wm/executable-cascade-v1 record and
  its one validator, over a production-shaped fixture (the scored
  cascade-candidate shape of cascade-shape-g/arranged->candidate, with the
  real three-halves square receipt fixture as 象 reading).  Every adversarial
  control names the defect it plants; each must be caught before any click is
  authorised."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.wm.executable-cascade :as xc]))

(def square-fixture
  (edn/read-string (slurp "test/fixtures/three-halves-square/publication-cadence.edn")))

(def pattern :ukrns/publication-cadence)

(def clause
  {:operation :apply-pattern
   :completion-check :square-B-observed
   :capability :wm/pattern-apply})

;; The scored candidate shape the selector returns (cascade-shape-g/
;; arranged->candidate): :arrangement nodes carry occurrence identity as
;; [pattern fragment-index], so TWO citations of the same pattern stay two
;; occurrences; one further citation is unread, one is a void lookup.
(defn- scored-candidate
  []
  {:kind :cascade-candidate
   :id "M-current/policy-1"
   :target "M-current"
   :precedence {:co-apply
                {:units [[:ukrns/publication-cadence 0]
                         [:ukrns/publication-cadence 1]
                         :other/pattern]
                 :descent [[:other/pattern [:ukrns/publication-cadence 1]]]
                 :patterns {}}}
   :arrangement
   {:nodes [{:pattern pattern :fragment-index 0 :roles ["goal"]}
            {:pattern pattern :fragment-index 1 :roles []}
            {:pattern :other/pattern :roles []}
            {:pattern :yet/another :roles []}
            {:pattern :void/pattern :roles []}]
    :edges [{:from [pattern 0] :to [pattern 1] :kind :precedes
             :evidence {:kind :witnessed-transition :id "e1"}}
            {:from [pattern 1] :to :other/pattern :kind :precedes
             :evidence {:kind :witnessed-transition :id "e2"}}
            {:from [pattern 0] :to [pattern 1] :kind :overlap
             :witness {:kind :isomorphism :digest "w1"}}]
    :precedence [[pattern 0] [pattern 1] :other/pattern :yet/another
                 :void/pattern]}})

(defn- squares []
  ;; occurrence-keyed: two distinct citations of the same pattern each get
  ;; the same validated square bytes under distinct occurrence identities
  {[pattern 0] square-fixture
   [pattern 1] square-fixture
   :other/pattern square-fixture
   :void/pattern {:lookup :void}})

(defn- structural []
  (xc/build-structural (scored-candidate) (squares)
                       {:selection "receipt-1" :scorer "receipt-2"}))

(defn- clauses-for
  "An operational clause for every :open occurrence of a structural layer."
  [st]
  (into {} (map (juxt :occurrence-id (constantly clause)))
        (filter #(= :open (:status %)) (:nodes st))))

(defn- built
  []
  (let [st (structural)]
    (xc/build st
              {:application-order
               [{:from [pattern 0] :to [pattern 1]
                 :evidence {:kind :witnessed-transition :id "e1"}}
                {:from [pattern 1] :to :other/pattern
                 :evidence {:kind :witnessed-transition :id "e2"}}]
               :operational-clauses (clauses-for st)
               :guards [{:on :other/pattern :clause :guard-1}]})))

(defn- reseal [record]
  (assoc record :digest (xc/digest record)))

(deftest structural-layer-is-the-xiang-diagram
  (let [st (structural)]
    (is (= :wm/structural-cascade-v1 (:schema st)))
    (is (= 5 (count (:nodes st))))
    ;; two citations of one pattern are two occurrences
    (is (= 2 (count (filter #(= pattern (:pattern %)) (:nodes st)))))
    (is (= 2 (count (distinct (map :occurrence-id
                                   (filter #(= pattern (:pattern %))
                                           (:nodes st)))))))
    ;; census: 3 open, 1 unread, 1 void
    (is (= {:open 3 :unread 1 :void 1}
           (frequencies (map :status (:nodes st)))))))

(deftest build-and-validate-the-good-record
  (let [record (built)]
    (is (map? record))
    (is (xc/certified? (xc/validate-cascade record)))
    (is (= (:digest record) (xc/digest record)))))

(deftest repeated-citations-do-not-collapse-through-anything
  (let [record (built)
        st (:structural record)]
    ;; square lookup: both pattern citations resolved squares
    (is (= 2 (count (filter #(and (:square %) (= pattern (:pattern %)))
                            (:nodes st)))))
    ;; executable nodes: both present and distinct
    (is (= 2 (count (filter #(= pattern (:pattern %))
                            (:executable-nodes record)))))
    ;; projection keeps both
    (is (= 2 (count (filter #(= pattern (:pattern %))
                            (:nodes (xc/project-structural record))))))))

(deftest pattern-id-keyed-squares-fail
  ;; planted defect: squares keyed by pattern id, so BOTH citation lookups
  ;; miss and the occurrences silently become :unread
  (let [st (xc/build-structural (scored-candidate)
                                {pattern square-fixture
                                 :other/pattern square-fixture
                                 :void/pattern {:lookup :void}}
                                {:selection "r"})]
    (is (= {:open 1 :unread 3 :void 1}
           (frequencies (map :status (:nodes st)))))
    (let [record (xc/build st
                           {:application-order []
                            :operational-clauses (clauses-for st)})]
      ;; the record is internally consistent, but a census that pretends the
      ;; old reading population (carried over) is caught by the projection
      ;; clause
      (is (xc/certified? (xc/validate-cascade record)))
      (let [tampered (reseal (assoc-in record [:census :unread] 1))]
        (is (= :projection-structural-lost
               (:clause (xc/validate-cascade tampered))))))))

(deftest unread-and-void-cannot-become-executable
  (let [st (structural)]
    (is (= :unread (:status (first (filter #(= :yet/another (:pattern %))
                                           (:nodes st))))))
    (is (= :void (:status (first (filter #(= :void/pattern (:pattern %))
                                         (:nodes st))))))
    ;; planting a clause for the unread occurrence is refused at build
    (is (= :clause-for-non-open-occurrence
           (get-in (xc/build st
                             {:operational-clauses
                              {:yet/another clause}})
                   [:refusal :kind])))
    ;; planting executable nodes for void/unread by hand fails validation
    (let [sneaked (reseal
                   (update (built)
                           :executable-nodes
                           conj {:occurrence-id :yet/another
                                 :pattern :yet/another
                                 :status :unread
                                 :enactment-status :open-hole
                                 :hole {:owner "nobody"}}))]
      (is (= :population-executable-set
             (:clause (xc/validate-cascade sneaked)))))))

(deftest reading-order-cannot-stand-in-for-application-order
  ;; a :precedes edge without transition evidence is refused at construction
  (let [candidate (scored-candidate)
        bare (assoc-in candidate
                       [:arrangement :edges 0 :evidence] nil)]
    (is (= :application-order-evidence-owed
           (get-in (xc/build-structural bare (squares) {}) [:refusal :kind]))))
  ;; and in a hand-sealed record, evidence-less application order fails
  (let [smuggled (reseal
                  (update-in (built)
                             [:relations :application-order]
                             (fn [edges]
                               (mapv #(dissoc % :evidence) edges))))]
    (is (= :cascade-law-application-order-without-evidence
           (:clause (xc/validate-cascade smuggled))))))

(deftest undirected-gluing-is-excluded-from-directed-cycle-check
  ;; gluing [p 0]-[p 1] plus application order [p 1]→[p 0] would be a cycle
  ;; only if gluing counted as directed; it must certify
  (let [st (structural)
        record (xc/build st
                         {:application-order
                          [{:from [pattern 1] :to [pattern 0]
                            :evidence {:kind :witnessed-transition :id "e3"}}
                           {:from [pattern 0] :to :other/pattern
                            :evidence {:kind :witnessed-transition :id "e2"}}]
                         :operational-clauses (clauses-for st)})]
    (is (xc/certified? (xc/validate-cascade record))))
  ;; a genuine directed cycle in application order is caught
  (let [cyclic (reseal
                (assoc-in (built)
                          [:relations :application-order]
                          [{:from [pattern 0] :to [pattern 1]
                            :evidence {:kind :t :id "a"}}
                           {:from [pattern 1] :to [pattern 0]
                            :evidence {:kind :t :id "b"}}]))]
    (is (= :cascade-law-application-cycle
           (:clause (xc/validate-cascade cyclic))))))

(deftest shared-name-candidate-without-witness-is-not-gluing
  ;; an overlap edge without a witness lands in shared-name-candidates
  (let [candidate (update-in (scored-candidate)
                             [:arrangement :edges 2] dissoc :witness)
        st (xc/build-structural candidate (squares) {})]
    (is (empty? (:gluing st)))
    (is (= 1 (count (:shared-name-candidates st))))
    ;; promoting it to a gluing (by hand) fails the witness clause
    (let [promoted (-> st
                       (update :gluing conj
                               (assoc (first (:shared-name-candidates st))
                                      :kind :gluing))
                       (assoc :shared-name-candidates []))
          resealed (reseal (assoc (built) :structural promoted))]
      (is (= :graph-gluing-unwitnessed
             (:clause (xc/validate-cascade resealed)))))))

(deftest projection-loses-nothing
  (let [st (structural)
        record (xc/build st {:application-order []
                             :operational-clauses (clauses-for st)})]
    (is (= st (xc/project-structural record)))))

(deftest tampering-breaks-the-seal
  (let [record (built)
        tampered (assoc-in record [:structural :nodes 0 :roles] ["changed"])]
    ;; forgetting to re-digest: identity clause catches it
    (is (= :identity-digest (:clause (xc/validate-cascade tampered))))
    ;; even a re-digested but structurally-broken record is caught: a
    ;; dropped node first disagrees with the census (projection clause),
    ;; and with the census also fixed, the construction order names an
    ;; occurrence that is no longer there
    (let [dropped (reseal (update-in record [:structural :nodes] #(subvec % 1)))]
      (is (= :projection-structural-lost (:clause (xc/validate-cascade dropped))))
      (let [fixed (reseal (assoc dropped :census
                                 (frequencies
                                  (map :status (get-in dropped
                                                       [:structural :nodes])))))]
        (is (= :population-executable-set
               (:clause (xc/validate-cascade fixed))))
        ;; with every dependent fixed, the last surviving defect is the
        ;; construction order naming an occurrence that is no longer there;
        ;; reached directly by dropping the void occurrence (in no edge)
        (let [direct (reseal
                    (-> record
                        (update-in [:structural :nodes]
                                   #(filterv (fn [n] (not= :void/pattern
                                                          (:pattern n))) %))
                        (assoc :census {:open 3 :unread 1})
                        (update :executable-nodes
                                #(filterv (fn [n] (not= :void/pattern
                                                        (:pattern n))) %))))]
          (is (= :cascade-law-construction-order
                 (:clause (xc/validate-cascade direct)))))))))

(deftest save-reload-identity-stability
  (let [record (built)
        dir (System/getProperty "java.io.tmpdir")
        path (xc/save! record dir)]
    (try
      (let [reloaded (xc/load-record path)]
        (is (= record reloaded))
        (is (= (:digest record) (xc/digest reloaded)))
        (is (xc/certified? (xc/validate-cascade reloaded))))
      (finally
        (.delete (io/file path))))))

(deftest validator-never-abstains
  ;; every failure is a typed machine error, never :abstained/:no-selection
  (is (every? #(= :cascade-validation-error (:status %))
              [(xc/validate-cascade {})
               (xc/validate-cascade (assoc (built) :digest "0"))])))
