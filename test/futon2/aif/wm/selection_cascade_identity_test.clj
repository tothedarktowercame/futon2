(ns futon2.aif.wm.selection-cascade-identity-test
  "Packet B focused tests: the pre-selector join of the occurrence-grain
  scored candidate to the canonical :wm/executable-cascade-v1 identity, over
  production-shaped reading fixtures (real relation kinds :context-enables /
  :precedes / :overlap, occurrence provenance, the real three-halves square
  receipt fixture).  Planted controls: a pattern-grain candidate that would
  otherwise win must be unselectable; a forged or whole-chain re-digested
  substitution must fail the identity check."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.cascade-shape-g :as shape-g]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.learning-trial-ledger :as ledger]
            [futon2.aif.wm.executable-cascade :as xc]
            [futon2.aif.wm.family-selection :as family-selection]
            [futon2.aif.wm.selection-cascade-identity :as sut])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(use-fixtures :once hermetic/with-hermetic-stores)

(defn- with-empty-learning-ledger [run]
  (let [root (.getPath (.toFile (Files/createTempDirectory
                                 "pktb-identity-ledger-"
                                 (make-array FileAttribute 0))))]
    (with-redefs [ledger/default-root root]
      (run))))

(use-fixtures :each with-empty-learning-ledger)

(def square-fixture
  (edn/read-string (slurp "test/fixtures/three-halves-square/publication-cadence.edn")))

(def dup "p/dup")
(def other "p/other")

(defn- sha [c] (apply str (repeat 64 c)))

;; occurrence ids of the production-shaped reading: p/dup cited at fragments
;; 0, 1 and 2; p/other sharing fragment 1 (witnessed overlap).
(def occ-dup0 [dup 0])
(def occ-other [other 1])
(def occ-dup1 [dup 1])
(def occ-dup2 [dup 2])

(defn- reading-for [oid]
  {:reading-record-id (str "read-" (pr-str oid))
   :span {:start 0 :end 41 :text "IF … HOWEVER … BECAUSE … THEN …"}
   :source-text-digest (evidence/value-digest [:source-text oid])
   :analysis-digest (evidence/value-digest [:analysis oid])
   :source-sha256 (sha "a")
   :square square-fixture})

(def readings
  {occ-dup0 (reading-for occ-dup0)
   occ-other (reading-for occ-other)
   occ-dup1 (reading-for occ-dup1)
   occ-dup2 (reading-for occ-dup2)})

(defn- pref [pattern c]
  {:id pattern :status "candidate" :rationale (str "fixture " pattern)
   :source_sha256 (sha c)})

(defn- fragment [i text refs]
  {:start (* 10 i) :end (* 10 (inc i)) :text text :intent "act" :target "T-occ"
   :rationale (str "fragment " i)
   :relations (if (zero? i) ["context"] ["action"])
   :pattern_refs (vec refs)})

(def analysis-map
  {:status "analyzed"
   :sentences [{:id "s1"
                :fragments [(fragment 0 "first" [(pref dup "a")])
                            (fragment 1 "second" [(pref other "b") (pref dup "c")])
                            (fragment 2 "third" [(pref dup "d")])]}]})

;; The scored cascade shape analysis-cascade produces, with the overlap
;; edge's gluing witness made explicit (real relation kinds throughout).
(defn- occurrence-cascade []
  {:nodes [{:pattern dup :fragment-index 0 :roles ["context"]
            :source-sha256 (sha "a")}
           {:pattern other :fragment-index 1 :roles ["action"]
            :source-sha256 (sha "b")}
           {:pattern dup :fragment-index 1 :roles ["action"]
            :source-sha256 (sha "c")}
           {:pattern dup :fragment-index 2 :roles ["action"]
            :source-sha256 (sha "d")}]
   :edges [{:from dup :to other :kind :context-enables
            :from-fragment 0 :to-fragment 1}
           {:from other :to dup :kind :precedes
            :from-fragment 1 :to-fragment 2}
           {:from occ-other :to occ-dup1 :kind :overlap
            :from-fragment 1 :to-fragment 1
            :witness {:kind :isomorphism
                      :digest (evidence/value-digest [:iso occ-other occ-dup1])
                      :objects [(str occ-other) (str occ-dup1)]}}]
   :precedence [dup other dup dup]
   :alternatives []})

(defn- occurrence-policy [id]
  {:target "T-occ" :mission "T-occ"
   :policy-id (str "T-occ/policy-" id)
   :kind :reading-overlap :analysis analysis-map
   :cascade (occurrence-cascade)})

;; a PATTERN-GRAIN cascade: no fragment indices, no occurrence ids.  It
;; scores (the shape scorer accepts bare pattern nodes) — the planted
;; control is that the canonical join must make it unselectable.
(defn- pattern-grain-policy [id]
  {:target "T-occ" :mission "T-occ"
   :policy-id (str "T-occ/policy-" id)
   :kind :reading-alternatives :analysis analysis-map
   :cascade {:nodes [{:pattern dup} {:pattern other}]
             :edges [{:from dup :to other :kind :precedes}]
             :precedence [dup other]}})

(defn- family [& policies]
  {:status :computed :target-id "T-occ"
   :policies (vec policies)
   :distinct-count (count policies) :reported-count (count policies)
   :failures [] :failure-count 0})

(def join-opts
  {:bind-cascade-identity {:readings (fn [_] readings)}})

(deftest canonical-identity-binds-before-the-one-selector
  (let [result (family-selection/select-over-families
                [(family (occurrence-policy 1) (occurrence-policy 2))]
                (merge {:beta 1 :enactment-fold nil :novelty-inputs {}}
                       join-opts))]
    (is (= :selected (:status result)))
    (is (= 2 (count (:ranked result)))
        "both occurrence-grain policies stay ranked after the join")
    (doseq [entry (:ranked result)]
      (is (string? (:cascade-digest entry)))
      (is (= (:cascade-digest entry)
             (get-in entry [:action :cascade-digest])
             (get-in entry [:certificate :cascade-digest])
             (get-in entry [:selection-law :cascade-digest]))
          "the digest binds the entry, its scorer certificate, its
           preference receipt and its action to one identity")
      (is (= :certified (:status (xc/validate-cascade
                                  (:canonical-cascade entry))))
          "every ranked entry carries a validator-certified record"))
    (is (= (:cascade-digest (first (:ranked result)))
           (get-in result [:decision :action :cascade-digest]))
        "the chosen action carries its cascade's digest")
    (is (every? #(string? (:cascade-digest %))
                (get-in result [:target-policy-families 0 :policies]))
        "policy summaries cite the canonical digest")
    (let [v (sut/verify-selected-identity (:decision result) (:ranked result))]
      (is (= :verified (:status v)))
      (is (contains? (set (map :policy-id (:ranked result))) (:policy-id v))))
    ;; the canonical record of the selected cascade describes the SAME
    ;; occurrence structure the scorer consumed
    (let [chosen (some #(when (= (get-in result [:decision :action :cascade-digest])
                                 (:cascade-digest %))
                          %)
                       (:ranked result))
          structural (:structural (:canonical-cascade chosen))]
      (is (= 4 (count (:nodes structural))))
      (is (= 3 (count (filter #(= dup (:pattern %)) (:nodes structural)))))
      (is (= #{:context-enables :precedes}
             (set (map :relation (:reading-order-edges structural))))
          "descriptive reading order keeps its real relation kinds")
      (is (= 1 (count (:gluing structural)))
          "the witnessed overlap is a gluing")
      (is (= [] (get-in chosen [:canonical-cascade :relations :application-order]))
          "descriptive reading order never becomes application order")
      (is (= [] (get-in chosen [:canonical-cascade :frontier]))
          "with no authored clauses the frontier is honestly empty"))))

(deftest pattern-grain-fallback-is-unselectable
  (let [families [(family (occurrence-policy 1) (pattern-grain-policy 9))]]
    ;; control 1: WITHOUT the join, the pattern-grain candidate is selected
    (let [unjoined (family-selection/select-over-families
                    families {:beta 1 :enactment-fold nil :novelty-inputs {}})]
      (is (= :selected (:status unjoined)))
      (is (= #{"T-occ/policy-1" "T-occ/policy-9"}
             (set (mapv :policy-id (:ranked unjoined))))
          "pre-join, both pattern-grain and occurrence-grain candidates
           reach the selector")
      (is (contains? #{"T-occ/policy-1" "T-occ/policy-9"}
                     (get-in unjoined [:decision :action :id]))))
    ;; control 2: WITH the join, the pattern-grain candidate is refused
    ;; before the selector and can never be chosen
    (let [joined (family-selection/select-over-families
                  families (merge {:beta 1 :enactment-fold nil
                                   :novelty-inputs {}} join-opts))]
      (is (= :selected (:status joined)))
      (is (= ["T-occ/policy-1"] (mapv :policy-id (:ranked joined))))
      (is (= "T-occ/policy-1" (get-in joined [:decision :action :id])))
      (let [refusal (first (filter #(= "T-occ/policy-9" (:policy-id %))
                                   (:failures joined)))]
        ;; the arranged candidate falls back to bare pattern ids as
        ;; occurrence ids; the join refuses the collapse at the earliest
        ;; point, before any canonical construction
        (is (= :occurrence-identity-absent (:kind refusal)))
        (is (= :pre-selector-identity-check (:stage refusal)))
        (is (seq (:nodes refusal))))
      (is (true? (:cascade-refused
                  (first (filter #(= "T-occ/policy-9" (:policy-id %))
                                 (get-in joined [:target-policy-families 0
                                                 :policies])))))))))

(deftest forged-incoming-digest-is-refused
  (let [entry {:policy-id "T-occ/policy-1" :target-id "T-occ" :rank 1
               :action {:kind :cascade-candidate :id "T-occ/policy-1"
                        :target "T-occ"
                        :cascade-digest (sha "f")
                        :arrangement (:arrangement
                                     (shape-g/arranged->candidate
                                      "T-occ" "T-occ/policy-1"
                                      (occurrence-cascade)))}}
        r (sut/bind-entry entry (fn [_] readings))]
    (is (map? (:refusal r)))
    (is (= :identity-forged (:kind (:refusal r))))
    (is (not= (sha "f") (:rebuilt (:refusal r))))))

(defn- reseal [record]
  (assoc record :digest (xc/digest record)))

(deftest whole-chain-redigest-substitution-fails-verification
  (let [result (family-selection/select-over-families
                [(family (occurrence-policy 1))]
                (merge {:beta 1 :enactment-fold nil :novelty-inputs {}}
                       join-opts))
        ranked (:ranked result)
        chosen (first ranked)
        record (:canonical-cascade chosen)
        d (:cascade-digest chosen)]
    (is (= :verified (:status (sut/verify-selected-identity
                               (:decision result) ranked))))
    ;; substitution 1: outer record tampered and re-digest only — the record
    ;; itself no longer re-digests to the bound identity
    (let [tampered (-> record
                       (assoc-in [:structural :census :open] [:swapped-occ])
                       reseal)]
      (is (not= d (:digest tampered)))
      (is (not= :verified
                (:status (sut/verify-selected-identity
                          (assoc-in (:decision result) [:action :cascade-digest]
                                    (:digest tampered))
                          (assoc-in ranked [0 :canonical-cascade] tampered))))))
    ;; substitution 2: a fully resealed, internally consistent different
    ;; chain — its digest matches no ranked receipt, so the selection
    ;; receipts reject it
    (let [swapped (-> record
                      (assoc-in [:structural :policy-id] "T-occ/substituted")
                      reseal)]
      (is (not= d (:digest swapped)))
      (is (= :cascade-identity-mismatch
             (:status (sut/verify-selected-identity
                       (assoc-in (:decision result) [:action :cascade-digest]
                                 (:digest swapped))
                       ranked)))))))

(deftest authored-operational-clauses-open-the-frontier
  (let [opts {:beta 1 :enactment-fold nil :novelty-inputs {}
              :bind-cascade-identity
              {:readings (fn [_] readings)
               :authored
               (fn [entry]
                 (let [ids (mapv :occurrence-id
                                 (get-in entry [:action :arrangement :nodes]))]
                   {:application-order
                    [{:from occ-dup0 :to occ-dup1
                      :evidence {:kind :witnessed-transition :id "e1"}}]
                    :operational-clauses
                    (into {}
                          (map (fn [id] [id {:operation :apply-pattern
                                             :completion-check :square-B-observed
                                             :capability :wm/pattern-apply}]))
                          ids)}))}}
        result (family-selection/select-over-families
                [(family (occurrence-policy 1))] opts)
        record (get-in result [:ranked 0 :canonical-cascade])]
    (is (= :selected (:status result)))
    (is (= :certified (:status (xc/validate-cascade record))))
    (is (= [{:from occ-dup0 :to occ-dup1
             :evidence {:kind :witnessed-transition :id "e1"}}]
           (get-in record [:relations :application-order])))
    ;; frontier = enactable nodes with no incoming evidenced application
    ;; edge; unauthored holes stay explicit
    (is (= [occ-dup0 occ-other occ-dup2] (mapv :occurrence-id (:frontier record))))
    (is (every? #(= :wm/enactment-compiler (get-in % [:hole :owner]))
                (filter #(= :open-hole (:enactment-status %))
                        (:executable-nodes record))))))
