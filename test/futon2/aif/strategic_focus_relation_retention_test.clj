(ns futon2.aif.strategic-focus-relation-retention-test
  "Synthetic boundary controls for retaining classify-target's supplied relation.
  These fixtures are not live records and make no full strategic-row claim."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.meta-live-outer-selector :as live]
            [futon2.aif.outer-policy-evidence :as evidence]
            [futon2.aif.outer-policy-objective :as objective]
            [futon2.aif.selected-outer-cascade-policy :as selected]
            [futon2.aif.selected-outer-cascade-policy-test]
            [futon2.aif.wm.family-selection :as family]
            [futon2.aif.wm.selection-cascade-identity-test]))

(defn- fixture-var [ns-name sym]
  (var-get (ns-resolve ns-name sym)))

(defn- pin [path c]
  {:path path :sha256 (apply str (repeat 64 c))})

(def snapshot
  {:schema :wm/pipeline-cascade-snapshot-v1
   :summary-source (pin "summary" "a")
   :graph-source (pin "graph" "b")
   :summary {:consistent? true
             :standards {:s1-regenerates true :s2-evidence true
                         :s3-reconstitution true :s4-honest-holes true
                         :s5-composed true}}
   :graph {:section-status {:lineage {:status :ok} :clusters {:status :ok}
                            :arrows {:status :ok} :held {:status :ok}
                            :patterns {:status :ok}}
           :lineage [] :clusters [{:mission "T-occ"}] :arrows [] :held []
           :tickets {:items []}
           :patterns {:edges []}}})

(def raw-associated
  {:target "T-occ" :relation "associated" :facet "WM"
   :source {:authority :synthetic-independent-relation
            :fixture "strategic-focus-relation-retention-test"}})

(def classification
  {:target "T-occ" :class :associated :relation raw-associated
   :derived-via {:kind :synthetic-supplied-relation}})

(defn- selector-target-classes [classification]
  (let [captured (atom nil)
        selection-as-of "2026-10-24T00:00:00Z"]
    (live/select-live
     {:tasks [{:id "T-occ" :kind :mission :priority 1
               :source (pin "T-occ.md" "c")}]
      :fetch-snapshot (constantly snapshot)
      :selection-as-of selection-as-of
      :focus-inputs {:authority :synthetic}
      :focus-context {:as-of selection-as-of :status :retained :focus "WM"}
      :focus-classify-fn (fn [& _] classification)
      :policy-field-config {:authority :synthetic}
      :policy-field-fn
      (fn [tasks _]
        {:schema :wm/pre-choice-policy-field-v1
         :source-pin (pin "wm://synthetic-field" "d")
         :coverage {:targets (count tasks) :current (count tasks)}
         :families (mapv (fn [task]
                           {:target-id (:id task) :status :computed
                            :policies [{:policy-id "unused"}] :failures []}) tasks)})
      :policy-menu-fn
      (fn [field join]
        (reset! captured (:target-classes join))
        {:schema :wm/sealed-outer-policy-menu-v1 :status :sealed
         :field-source-pin (:source-pin field)
         :entries [] :menu-pin (pin "wm://synthetic-menu" "e")})})
    @captured))

(defn- production-shaped [target-classes]
  (let [policy-fn (fixture-var 'futon2.aif.wm.selection-cascade-identity-test
                               'occurrence-policy)
        family-fn (fixture-var 'futon2.aif.wm.selection-cascade-identity-test
                               'family)
        readings (fixture-var 'futon2.aif.wm.selection-cascade-identity-test
                              'readings)
        declaration (fixture-var 'futon2.aif.selected-outer-cascade-policy-test
                                 'declaration)
        field {:schema :wm/pre-choice-policy-field-v1
               :source-pin (pin "wm://field" "f")
               :coverage {:targets 1 :current 1}
               :families [(family-fn (policy-fn 1) (policy-fn 2))]}
        menu (family/sealed-pre-choice-menu
              field {:readings (constantly readings)
                     :target-classes target-classes})
        digests (mapv :cascade-digest (:entries menu))
        evidence (evidence/join menu (declaration digests [0.0 1.0]) [])
        objective (objective/evaluate evidence menu)
        carrier (selected/seal menu evidence objective)]
    {:menu menu :objective objective :carrier carrier
     :decision (selected/decision carrier)}))

(deftest raw-relation-survives-selector-menu-carrier-and-decision
  (let [target-classes (selector-target-classes classification)
        target-row (get target-classes "T-occ")
        retained (production-shaped target-classes)
        legacy (production-shaped
                (update target-classes "T-occ" dissoc :relation :derived-via))
        menu (:menu retained)
        carrier (:carrier retained)
        decision (:decision retained)
        decision-relations (get-in decision [:decision :selection-certificate
                                             :selected-outer-cascade-policy
                                             :target-relations])]
    (testing "the original classifier result, not a class reconstruction, crosses the selector"
      (is (= raw-associated (:relation target-row)))
      (is (= {:kind :synthetic-supplied-relation} (:derived-via target-row)))
      (is (= :related (:scorer-class target-row))))
    (testing "the real menu seal and selected carrier bind the same raw evidence"
      (is (= target-classes (:target-relations menu)))
      (is (every? #(= :related
                      (get-in % [:class-emission :model :target-class "T-occ"]))
                  (:entries menu))
          "the real scorer consumed the associated relation's related class")
      (is (nil? (family/verify-sealed-menu menu)))
      (is (nil? (selected/verify carrier)))
      (is (= target-classes decision-relations))
      (is (= (:menu-pin menu)
             (get-in decision [:decision :selection-certificate
                               :selected-outer-cascade-policy :menu-pin]))))
    (testing "relation retention cannot affect choice or score"
      (is (= (get-in legacy [:objective :action])
             (get-in retained [:objective :action])))
      (is (= (mapv :G-efe (get-in legacy [:menu :entries]))
             (mapv :G-efe (get-in retained [:menu :entries])))))
    (testing "tampering with relation while holding class fixed breaks the real menu seal"
      (let [tampered (assoc-in menu [:target-relations "T-occ" :relation :relation]
                               "focus")]
        (is (= :menu-pin-mismatch (family/verify-sealed-menu tampered)))))))

(deftest unknown-relation-stays-typed-and-unscored
  (let [unknown (assoc classification
                       :class :unknown
                       :relation {:status :absent :reason :relation-outside-focus-facets})
        target-classes (selector-target-classes unknown)
        projected (get target-classes "T-occ")
        scored (production-shaped target-classes)]
    (is (= {:status :absent :reason :relation-outside-focus-facets}
           (:relation projected)))
    (is (= :unknown (:class projected)))
    (is (nil? (:scorer-class projected)))
    (is (= :no-canonically-bound-policy (get-in scored [:menu :reason])))
    (is (empty? (get-in scored [:menu :entries])))))
