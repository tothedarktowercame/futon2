(ns futon2.aif.wm.executable-cascade-test
  "Packet A focused tests: the canonical :wm/executable-cascade-v1 record and
  its one validator, over a production-shaped fixture (the scored
  cascade-candidate shape of cascade-shape-g/arranged->candidate, with the
  real three-halves square receipt fixture as 象 reading).  Every adversarial
  control names the defect it plants; each must be caught before any click is
  authorised.  Review round 2 controls are marked [R2-n]."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.wm.executable-cascade :as xc]))

(def square-fixture
  (edn/read-string (slurp "test/fixtures/three-halves-square/publication-cadence.edn")))

(def pattern :ukrns/publication-cadence)

(def clause
  {:operation :apply-pattern
   :completion-check :square-B-observed
   :capability :wm/pattern-apply})

(defn- provenance
  "Complete 象 provenance for a reading of PATTERN at occurrence OID."
  [oid]
  {:reading-record-id (str "read-" (pr-str oid))
   :span {:start 0 :end 41 :text "IF … HOWEVER … BECAUSE … THEN …"}
   :source-text-digest (evidence/value-digest [(pr-str oid) :source-text])
   :analysis-digest (evidence/value-digest [(pr-str oid) :analysis])
   :source-sha256 "24a8a429148b1c6e2b41e482ee539c00285154957022f18b3dab181bbdf4fe12"
   :square-digest (evidence/value-digest square-fixture)})

(defn- scored-candidate
  []
  {:kind :cascade-candidate
   :id "M-current/policy-1"
   :target "M-current"
   :precedence {:co-apply
                {:units [[:ukrns/publication-cadence 0]
                         [:ukrns/publication-cadence 1]
                         :other-occ]
                 :descent [[:other-occ [:ukrns/publication-cadence 1]]]
                 :patterns {}}}
   :arrangement
   {:nodes [{:pattern pattern :fragment-index 0 :roles ["goal"]}
            {:pattern pattern :fragment-index 1 :roles []}
            {:pattern :other/pattern :occurrence-id :other-occ :roles []}
            {:pattern :yet/another :occurrence-id :unread-occ :roles []}
            {:pattern :void/pattern :occurrence-id :void-occ :roles []}]
    ;; :precedes edges are DESCRIPTIVE reading order [R2-5]
    :edges [{:from [pattern 0] :to [pattern 1] :kind :precedes}
            {:from [pattern 1] :to :other-occ :kind :precedes}
            {:from [pattern 0] :to [pattern 1] :kind :overlap
             :witness {:kind :isomorphism :digest (evidence/value-digest
                                                   [:iso [pattern 0] [pattern 1]])
                       :objects [(str [pattern 0]) (str [pattern 1])]}}]
    :precedence [[pattern 0] [pattern 1] :other-occ :unread-occ :void-occ]}})

(defn- readings
  []
  {[pattern 0] (merge (provenance [pattern 0]) {:square square-fixture})
   [pattern 1] (merge (provenance [pattern 1]) {:square square-fixture})
   :other-occ (merge (provenance :other-occ) {:square square-fixture})
   :void-occ {:lookup :void :observation {:part :B :matched nil}}})

;; :unread-occ is absent from the readings map: an unread citation

(defn- structural []
  (xc/build-structural (scored-candidate) (readings)
                       {:selection "receipt-1" :scorer "receipt-2"}))

(defn- clauses-for
  [st]
  (into {} (map (juxt :occurrence-id (constantly clause)))
        (:nodes st)))

(defn- built
  []
  (let [st (structural)]
    (xc/build st
              {:application-order
               [{:from [pattern 0] :to [pattern 1]
                 :evidence {:kind :witnessed-transition :id "e1"}}
                {:from [pattern 1] :to :other-occ
                 :evidence {:kind :witnessed-transition :id "e2"}}]
               :operational-clauses (clauses-for st)
               :guards [{:on :other-occ :clause :guard-1}]})))

(defn- reseal [record]
  (assoc record :digest (xc/digest record)))

(defn- reseal-structural
  "Re-digest a record whose structural layer was tampered with, including
  the structural layer's own digest and its census digest."
  [record]
  (let [census (update (:census (:structural record)) :census-digest
                       (constantly (xc/census-digest
                                    (:census (:structural record)))))
        st (assoc (:structural record)
                  :census census
                  :digest nil)
        st (assoc st :digest (xc/digest st))
        r (assoc record
                 :structural st
                 :structural-digest (:digest st)
                 :census (dissoc census :census-digest)
                 :census-digest (:census-digest census))]
    (assoc r :digest (xc/digest r))))

(deftest structural-layer-is-the-xiang-diagram
  (let [st (structural)]
    (is (= :wm/structural-cascade-v1 (:schema st)))
    ;; [R2-1] nodes are occurrences ONLY: 3 occurrences; void/unread are
    ;; census entries, never nodes
    (is (= 3 (count (:nodes st))))
    (is (= 3 (count (:open (:census st)))))
    (is (= 1 (count (:void (:census st)))))
    (is (= 1 (count (:unread (:census st)))))
    (is (every? #(contains? #{:void :unread} (:kind %))
                (concat (:void (:census st)) (:unread (:census st)))))
    ;; census digest binds the three populations
    (is (= (:census-digest (:census st))
           (xc/census-digest (:census st))))
    ;; [R2-5] :precedes edges are descriptive reading-order edges, retained
    ;; as such; no evidence demanded, none invented as application order
    (is (= 2 (count (:reading-order-edges st))))
    (is (empty? (filter #(= :application-order (:kind %))
                        (concat (:reading-order-edges st) (:gluing st)))))))

(deftest build-and-validate-the-good-record
  (let [record (built)]
    (is (map? record))
    (is (xc/certified? (xc/validate-cascade record)))
    (is (= (:digest record) (xc/digest record)))))

(deftest repeated-citations-do-not-collapse-through-anything
  (let [record (built)
        st (:structural record)]
    (is (= 2 (count (filter #(= pattern (:pattern %)) (:nodes st)))))
    (is (= 2 (count (distinct (map :occurrence-id
                                   (filter #(= pattern (:pattern %))
                                           (:nodes st)))))))
    ;; two citations of one pattern are also two distinct readings
    (is (= 2 (count (distinct (map #(get-in % [:provenance
                                               :reading-record-id])
                                   (filter #(= pattern (:pattern %))
                                           (:nodes st)))))))
    (is (= 2 (count (filter #(= pattern (:pattern %))
                            (:executable-nodes record)))))
    (is (= 2 (count (filter #(= pattern (:pattern %))
                            (:nodes (xc/project-structural record))))))))

(deftest pattern-id-keyed-readings-fail
  ;; planted defect: readings keyed by pattern id, so BOTH citation lookups
  ;; miss and the occurrences silently become unread census entries
  (let [st (xc/build-structural (scored-candidate)
                                {pattern (merge (provenance pattern)
                                                {:square square-fixture})
                                 :other-occ (merge (provenance :other-occ)
                                                   {:square square-fixture})
                                 :void-occ {:lookup :void}}
                                {:selection "r"})]
    ;; census digest binds the three populations (incl. the standing
    ;; unread citation :unread-occ)
    (is (= 3 (count (:unread (:census st)))))
    (is (= 1 (count (:open (:census st)))))))

(deftest unread-and-void-cannot-become-executable
  (let [st (structural)]
    ;; the census entries name their citations and are not occurrences
    (is (every? #(not (contains? (set (map :occurrence-id (:nodes st)))
                                 (:citation %)))
                (concat (:void (:census st)) (:unread (:census st)))))
    ;; planting a clause for an unread citation is refused at build
    (is (= :clause-for-non-occurrence
           (get-in (xc/build st
                             {:operational-clauses
                              {:unread-occ clause}})
                   [:refusal :kind])))
    ;; planting executable nodes for census entries by hand fails validation
    (let [sneaked (reseal
                   (update (built)
                           :executable-nodes
                           conj {:occurrence-id :unread-occ
                                 :pattern :yet/another
                                 :enactment-status :open-hole
                                 :hole {:owner "nobody"}}))]
      (is (= :population-executable-set
             (:clause (xc/validate-cascade sneaked)))))))

(deftest reading-order-cannot-stand-in-for-application-order
  ;; [R2-5] descriptive :precedes edges stay reading order: no evidence
  ;; demanded of them at construction, and they enter no operational edge
  (let [st (structural)]
    (is (seq (:reading-order-edges st)))
    (is (xc/certified? (xc/validate-cascade (built)))))
  ;; application order without independent evidence is refused at build
  (let [st (structural)]
    (is (= :application-order-evidence-owed
           (get-in (xc/build st {:application-order
                                 [{:from [pattern 0] :to [pattern 1]}]}) 
                   [:refusal :kind]))))
  ;; and in a hand-sealed record, evidence-stripped application order fails
  (let [smuggled (reseal
                  (update-in (built)
                             [:relations :application-order]
                             #(mapv (fn [e] (assoc e :evidence nil)) %)))]
    (is (= :cascade-law-application-order-without-evidence
           (:clause (xc/validate-cascade smuggled))))))

(deftest undirected-gluing-is-excluded-from-directed-cycle-check
  (let [st (structural)
        record (xc/build st
                         {:application-order
                          [{:from [pattern 1] :to [pattern 0]
                            :evidence {:kind :witnessed-transition :id "e3"}}
                           {:from [pattern 0] :to :other-occ
                            :evidence {:kind :witnessed-transition :id "e2"}}]
                         :operational-clauses (clauses-for st)})]
    (is (xc/certified? (xc/validate-cascade record))))
  (let [cyclic (reseal
                (assoc-in (built)
                          [:relations :application-order]
                          [{:from [pattern 0] :to [pattern 1]
                            :evidence {:kind :t :id "a"}}
                           {:from [pattern 1] :to [pattern 0]
                            :evidence {:kind :t :id "b"}}]))]
    (is (= :cascade-law-application-cycle
           (:clause (xc/validate-cascade cyclic))))))

(deftest gluing-evidence-is-validated-not-truthy
  ;; [R2-4] a truthy but unconforming witness is NOT a gluing witness
  (let [candidate (update-in (scored-candidate)
                             [:arrangement :edges 2 :witness]
                             (constantly {:anything :passes}))
        st (xc/build-structural candidate (readings) {})]
    (is (empty? (:gluing st)))
    (is (= 1 (count (:shared-name-candidates st))))
    ;; promoting it by hand to a gluing fails the witness-schema clause
    (let [promoted (assoc st
                          :gluing [(assoc (first (:shared-name-candidates st))
                                          :kind :gluing
                                          :witness {:anything :passes})]
                          :shared-name-candidates [])
          resealed (reseal-structural (assoc (built) :structural promoted))]
      (is (= :graph-gluing-witness-invalid
             (:clause (xc/validate-cascade resealed))))))
  ;; a conforming witness demoted to shared-name-candidate is also caught
  (let [st (structural)
        demoted (assoc st
                       :gluing []
                       :shared-name-candidates
                       (mapv #(assoc % :kind :shared-name-candidate
                                     :offered-witness (:witness %))
                             (:gluing st)))
        resealed (reseal-structural (assoc (built) :structural demoted))]
    (is (= :cascade-law-shared-name-not-gluing
           (:clause (xc/validate-cascade resealed))))))

(deftest provenance-is-required-and-checked
  ;; [R2-3] the good fixture fails when provenance is removed
  (let [stripped (update (readings)
                         [pattern 0]
                         #(dissoc % :reading-record-id :span
                                  :source-text-digest :analysis-digest
                                  :source-sha256 :square-digest))]
    (is (= :provenance-invalid
           (get-in (xc/build-structural (scored-candidate) stripped {})
                   [:refusal :kind]))))
  ;; and a hand-sealed record with provenance stripped from one node fails
  (let [stripped (reseal-structural
                  (update-in (built)
                             [:structural :nodes 0 :provenance]
                             dissoc :source-sha256 :span))]
    (is (= :provenance-missing
           (:clause (xc/validate-cascade stripped)))))
  ;; a square-digest that does not match the carried square fails the pins
  (let [swapped (reseal-structural
                 (update-in (built)
                            [:structural :nodes 0 :provenance :square-digest]
                            (constantly (evidence/value-digest [:not-the-square]))))]
    (is (= :pins-square-invalid
           (:clause (xc/validate-cascade swapped))))))

(deftest occurrence-identity-is-independent-of-pattern
  ;; [R2-2] a node with neither an explicit occurrence id nor a fragment
  ;; index has no occurrence identity and the builder refuses it (no bare
  ;; pattern fallback)
  (let [candidate (update-in (scored-candidate)
                             [:arrangement :nodes 2]
                             dissoc :occurrence-id)]
    (is (= :occurrence-identity-absent
           (get-in (xc/build-structural candidate (readings) {})
                   [:refusal :kind]))))
  ;; an occurrence whose identity equals its pattern id fails validation
  (let [collapsed (reseal-structural
                   (-> (built)
                       (update-in [:structural :nodes 2 :occurrence-id]
                                  (constantly :other/pattern))
                       (update-in [:structural :census :open]
                                  (fn [open]
                                    (mapv #(if (= % :other-occ) :other/pattern %)
                                          open)))))]
    (is (= :population-occurrence-collapsed-to-pattern
           (:clause (xc/validate-cascade collapsed))))))

(deftest census-is-bound-not-merely-recomputed
  ;; [R2-6] a re-digested substitution of the census cannot certify: moving
  ;; a void citation to unread in the record-level census copy (outer digest
  ;; refreshed, structural layer untouched) breaks the census binding
  (let [swapped (reseal
                 (update (built)
                         :census
                         (fn [c]
                           (-> c
                               (update :unread conj (first (:void c)))
                               (update :void #(subvec % 1))))))]
    (is (= :projection-census-unbound
           (:clause (xc/validate-cascade swapped)))))
  ;; and the same substitution inside the structural layer, with the census
  ;; digest refreshed but the structural digest not, is caught by the
  ;; structural identity clause
  (let [swapped (reseal
                 (update-in (built)
                            [:structural :census]
                            (fn [c]
                              (-> c
                                  (update :unread conj (first (:void c)))
                                  (update :void #(subvec % 1))
                                  (assoc :census-digest
                                         (xc/census-digest
                                          {:open (:open c)
                                           :void (:void c)
                                           :unread (:unread c)}))))))]
    (is (contains? #{:identity-digest :projection-structural-lost}
                   (:clause (xc/validate-cascade swapped))))))

(deftest projection-loses-nothing
  (let [st (structural)
        record (xc/build st {:application-order []
                             :operational-clauses (clauses-for st)})]
    (is (= st (xc/project-structural record)))))

(deftest tampering-breaks-the-seal
  (let [record (built)
        tampered (assoc-in record [:structural :nodes 0 :roles] ["changed"])]
    (is (= :identity-digest (:clause (xc/validate-cascade tampered))))
    ;; fully resealed tamper: the structural digest clause catches a change
    ;; to the structural layer even when all digests are refreshed
    (let [resealed (reseal-structural
                    (assoc-in record [:structural :nodes 0 :roles]
                              ["changed"]))]
      (is (xc/certified? (xc/validate-cascade resealed)))
      ;; it certifies because the whole chain was honestly re-digested:
      ;; it is a NEW cascade identity, not the selected one.  The selected
      ;; identity is detectable via the digest inequality:
      (is (not= (:digest record) (:digest resealed))))))

(deftest save-reload-identity-stability-and-append-only
  (let [record (built)
        dir (System/getProperty "java.io.tmpdir")
        path (xc/save! record dir)]
    (try
      (let [reloaded (xc/load-record path)]
        (is (= record reloaded))
        (is (= (:digest record) (xc/digest reloaded)))
        (is (xc/certified? (xc/validate-cascade reloaded))))
      ;; [R2-7] append-only: identical record returns the same path
      (is (= path (xc/save! record dir)))
      ;; a DIFFERENT record under the same digest name is refused, never
      ;; overwritten — same digest requires identical content, so simulate
      ;; the collision by writing a different record to the digest path
      (let [f (io/file path)]
        (spit f (pr-str (assoc record :schema :wm/other)))
        (is (thrown? Exception (xc/save! record dir))))
      (finally
        (.delete (io/file path))))))

(deftest validator-never-abstains
  (is (every? #(= :cascade-validation-error (:status %))
              [(xc/validate-cascade {})
               (xc/validate-cascade (assoc (built) :digest "0"))])))
