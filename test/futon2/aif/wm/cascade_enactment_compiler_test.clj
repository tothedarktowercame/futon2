(ns futon2.aif.wm.cascade-enactment-compiler-test
  "Packet D1 focused tests: the enactment compiler over a production-shaped
  repeated-occurrence fixture (p/dup cited at fragments 0, 1, 2 with p/other
  sharing fragment 1; real relation kinds :context-enables / :precedes /
  witnessed :overlap; a void and an unread citation in the construction
  census).  Controls: descriptive-edge leakage, missing transition
  evidence, foreign occurrence, fabricated square derivation, cycles, and
  an open cascade refined later without mutation."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.wm.cascade-enactment-compiler :as sut]
            [futon2.aif.wm.executable-cascade :as xc]))

(def square-fixture
  (edn/read-string (slurp "test/fixtures/three-halves-square/publication-cadence.edn")))

(def dup "p/dup")
(def other "p/other")

(defn- sha [c] (apply str (repeat 64 c)))

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
   occ-dup2 (reading-for occ-dup2)
   :void-occ {:lookup :void :observation {:part :B :matched nil}}})
;; :unread-occ absent from readings: an unread citation

(defn- scored-candidate []
  {:kind :cascade-candidate :id "T-occ/policy-1" :target "T-occ"
   :precedence {:co-apply {}}
   :arrangement
   {:nodes [{:pattern dup :fragment-index 0 :roles ["context"]
             :source-sha256 (sha "a")}
            {:pattern other :fragment-index 1 :roles ["action"]
             :source-sha256 (sha "b")}
            {:pattern dup :fragment-index 1 :roles ["action"]
             :source-sha256 (sha "c")}
            {:pattern dup :fragment-index 2 :roles ["action"]
             :source-sha256 (sha "d")}
            {:pattern :an/unread :occurrence-id :unread-occ :roles []}
            {:pattern :a/void :occurrence-id :void-occ :roles []}]
    :edges [{:from occ-dup0 :to occ-other :kind :context-enables}
            {:from occ-other :to occ-dup2 :kind :precedes}
            {:from occ-dup1 :to occ-dup2 :kind :precedes
             :from-fragment 1 :to-fragment 2}
            {:from occ-other :to occ-dup1 :kind :overlap
             :witness {:kind :isomorphism
                       :digest (evidence/value-digest [:iso occ-other occ-dup1])
                       :objects [(str occ-other) (str occ-dup1)]}}]
    :precedence [occ-dup0 occ-other occ-dup1 occ-dup2 :unread-occ
                 :void-occ]}})

(defn- structural []
  (let [st (xc/build-structural (scored-candidate) readings
                                {:construction "receipt-1"})]
    (when (:refusal st) (throw (ex-info "fixture refused" st)))
    st))

(def attribution {:author :joe :authority :wm/operator-authoring})

(defn- evidence [id]
  {:kind :witnessed-transition :digest (evidence/value-digest [id])})

(def clause
  {:operation :apply-pattern :capability :wm/pattern-apply
   :consumes #{:i1 :i2} :produces #{:b}})

(deftest open-cascade-compiles-with-explicit-holes-only
  (let [st (structural)
        r (sut/compile-executable st {:attribution attribution})]
    (is (map? (:record r)) "no refusal")
    (is (xc/certified? (:certificate r)))
    ;; all four occurrences are explicit open holes; nothing invented
    (is (= 4 (count (:executable-nodes (:record r)))))
    (is (every? #(= :open-hole (:enactment-status %))
                (:executable-nodes (:record r))))
    (is (every? #(= :wm/enactment-compiler (get-in % [:hole :owner]))
                (:executable-nodes (:record r))))
    (is (= [] (get-in r [:record :relations :application-order])))
    (is (= [] (:frontier (:record r))))
    ;; void and unread stay census-only
    (is (= 1 (count (get-in r [:record :structural :census :void]))))
    (is (= 1 (count (get-in r [:record :structural :census :unread]))))))

(deftest attributed-clauses-derive-completion-from-the-square
  (let [st (structural)
        r (sut/compile-executable
           st {:attribution attribution
               :clauses {occ-dup0 clause occ-other clause}})]
    (is (map? (:record r)))
    (is (xc/certified? (:certificate r)))
    (doseq [oid [occ-dup0 occ-other]]
      (let [node (some #(when (= oid (:occurrence-id %))
                          %) (:executable-nodes (:record r)))]
        (is (= :enactable (:enactment-status node)))
        (is (= [:square-B-observed
                (get-in square-fixture [:objects :B :theory :elems])]
               (get-in node [:clause :completion-check]))
            "completion is derived from the square, warranted by its
             validity")
        (is (= :apply-pattern (get-in node [:clause :operation]))
            "operation comes from the authoring, not the compiler")
        (is (= attribution
               (get-in node [:clause :authoring/attribution])))))
    ;; unauthored occurrences stay open holes
    (is (= 2 (count (filter #(= :open-hole (:enactment-status %))
                            (:executable-nodes (:record r))))))))

(deftest evidenced-application-order-opens-a-frontier
  (let [st (structural)
        r (sut/compile-executable
           st {:attribution attribution
               :application-order [{:from occ-dup0 :to occ-dup1
                                    :evidence (evidence :e1)}]
               :clauses {occ-dup0 clause occ-other clause
                         occ-dup1 clause occ-dup2 clause}})]
    (is (map? (:record r)))
    (is (xc/certified? (:certificate r)))
    (is (= [{:from occ-dup0 :to occ-dup1 :evidence (evidence :e1)
             :authoring/attribution attribution}]
           (get-in r [:record :relations :application-order])))
    (is (= [occ-dup0 occ-other occ-dup2]
           (mapv :occurrence-id (:frontier (:record r))))
        "frontier = enactable nodes with no incoming EVIDENCED edge;
         dup2's descriptive predecessors do not gate it")))

(deftest descriptive-edge-leakage-is-refused
  (let [st (structural)
        ;; the duplicated READING-ORDER pair offered as application order,
        ;; justified only by the reading
        r (sut/compile-executable
           st {:attribution attribution
               :application-order [{:from occ-dup1 :to occ-dup2
                                    :evidence {:kind :reading-order
                                               :digest (evidence :e1)}}]
               :clauses {occ-dup1 clause occ-dup2 clause}})]
    (is (= :descriptive-evidence-not-operational
           (get-in r [:refusal :kind])))
    ;; a gluing witness is not transition evidence either
    (is (= :descriptive-evidence-not-operational
           (get-in (sut/compile-executable
                    st {:attribution attribution
                        :application-order
                        [{:from occ-other :to occ-dup1
                          :evidence {:kind :gluing-isomorphism
                                     :digest (evidence :e2)}}]})
                   [:refusal :kind])))))

(deftest missing-transition-evidence-is-refused
  (let [st (structural)]
    (is (= :application-order-evidence-owed
           (get-in (sut/compile-executable
                    st {:attribution attribution
                        :application-order [{:from occ-dup0 :to occ-dup1}]})
                   [:refusal :kind])))
    (is (= :evidence-kind-unknown
           (get-in (sut/compile-executable
                    st {:attribution attribution
                        :application-order
                        [{:from occ-dup0 :to occ-dup1
                          :evidence {:kind :gut-feeling}}]})
                   [:refusal :kind])))))

(deftest foreign-occurrence-is-refused
  (let [st (structural)]
    (is (= :foreign-occurrence
           (get-in (sut/compile-executable
                    st {:attribution attribution
                        :clauses {:not/an-occurrence clause}})
                   [:refusal :kind])))
    (is (= :foreign-occurrence
           (get-in (sut/compile-executable
                    st {:attribution attribution
                        :application-order
                        [{:from occ-dup0 :to "p/dup"
                          :evidence (evidence :e9)}]})
                   [:refusal :kind])))))

(deftest fabricated-square-derivation-is-refused
  (let [st (structural)
        fabricated (assoc clause :completion-check
                          [:square-B-observed #{:fabricated-element}])
        r (sut/compile-executable
           st {:attribution attribution :clauses {occ-dup0 fabricated}})]
    (is (= :fabricated-square-derivation (get-in r [:refusal :kind])))
    (is (= occ-dup0 (get-in r [:refusal :clauses 0 :occurrence-id])))))

(deftest cyclic-application-order-is-refused
  (let [st (structural)
        r (sut/compile-executable
           st {:attribution attribution
               :application-order
               [{:from occ-dup0 :to occ-dup1 :evidence (evidence :e1)}
                {:from occ-dup1 :to occ-dup2 :evidence (evidence :e2)}
                {:from occ-dup2 :to occ-dup0 :evidence (evidence :e3)}]
               :clauses {occ-dup0 clause occ-dup1 clause occ-dup2 clause}})]
    (is (= :application-cycle (get-in r [:refusal :kind])))
    (is (= (first (get-in r [:refusal :cycle]))
           (last (get-in r [:refusal :cycle]))))))

(deftest open-cascade-refines-without-mutation
  (let [st (structural)
        open (sut/compile-executable st {:attribution attribution})
        refined (sut/compile-executable
                 st {:attribution attribution
                     :application-order [{:from occ-dup0 :to occ-dup1
                                          :evidence (evidence :e1)}]
                     :clauses {occ-dup0 clause occ-other clause
                               occ-dup1 clause occ-dup2 clause}})]
    ;; the refined record is a NEW identity over the SAME structural layer
    (is (not= (:digest open) (:digest refined)))
    (is (= (:structural (:record open)) (:structural (:record refined))))
    (is (= (xc/digest (:structural (:record open)))
           (xc/digest (:structural (:record refined))))
        "the 象 projection identity is preserved by refinement")
    ;; the open record is unchanged and still certifies on its own
    (is (xc/certified? (xc/validate-cascade (:record open))))
    (is (= (:digest open) (xc/digest (:record open))))
    ;; and compiling the same authoring again is deterministic
    (is (= (:digest refined)
           (:digest (sut/compile-executable
                     st {:attribution attribution
                         :application-order [{:from occ-dup0 :to occ-dup1
                                              :evidence (evidence :e1)}]
                         :clauses {occ-dup0 clause occ-other clause
                                   occ-dup1 clause occ-dup2 clause}}))))))

(deftest unattributed-or-uncertified-input-is-refused
  (let [st (structural)]
    (is (= :authoring-unattributed
           (get-in (sut/compile-executable st {}) [:refusal :kind])))
    (is (= :authoring-unattributed
           (get-in (sut/compile-executable
                    st {:attribution {:author :joe}}) [:refusal :kind])))
    ;; a structural layer that does not certify on its own baseline
    (is (= :structural-not-certified
           (get-in (sut/compile-executable
                    {:schema :wm/structural-cascade-v1 :nodes []}
                    {:attribution attribution}) [:refusal :kind])))))
