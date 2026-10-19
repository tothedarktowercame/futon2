(ns futon2.aif.meta-live-outer-assessed-selector-test
  "Selector seam for the verified assessed-population executable carrier."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.assessed-policy-basis :as assessed]
            [futon2.aif.meta-live-outer-selector :as live]
            [futon2.aif.outer-policy-evidence-provider :as provider]
            [futon2.aif.outer-policy-evidence-test :as evidence-fixture]
            [futon2.aif.selected-outer-assessed-policy-test :as assessed-fixture]
            [futon2.aif.selected-outer-cascade-policy :as selected]
            [futon2.aif.wm.executable-cascade :as executable]))

(defn- pin [path c] {:path path :sha256 (apply str (repeat 64 c))})

(defn- snapshot-for [task-id]
  {:schema :wm/pipeline-cascade-snapshot-v1
   :summary-source (pin "summary" "a") :graph-source (pin "graph" "b")
   :summary {:consistent? true
             :standards {:s1-regenerates true :s2-evidence true
                         :s3-reconstitution true :s4-honest-holes true
                         :s5-composed true}}
   :graph {:section-status {:clusters {:status :ok}}
           :lineage [] :clusters [{:mission task-id}] :arrows [] :held []
           :tickets {:items [{:stem task-id :mtime-ms 1}]} :patterns {:edges []}}})

(defn- task [id]
  {:id id :kind :mission :priority 1 :source (pin (str id ".md") "c")})

(defn- policy-field [menu]
  (fn [rows _]
    {:schema :wm/pre-choice-policy-field-v1
     :source-pin (:field-source-pin menu)
     :coverage {:targets (count rows) :current (count rows)}
     :families [{:target-id (:id (first rows)) :status :computed
                 :policies (mapv #(select-keys % [:policy-id]) (:entries menu))
                 :failures []}]}))

(defn- run-live [menu declaration task-id]
  (live/select-live
   {:tasks [(task task-id)] :fetch-snapshot (constantly (snapshot-for task-id))
    :policy-field-config {:authority :constructed-selector-seam}
    :policy-field-fn (policy-field menu)
    :policy-menu-fn (fn [_ _] menu)
    :outer-policy-evidence-provider
    (fn [_]
      (provider/provided declaration []
                         {:kind :constructed-production-shaped-authority
                          :source-pin (pin "wm://selector-assessed-authority" "d")}))}))

(defn- declaration [digests]
  (let [spec (evidence-fixture/spec [1.0 1.0])]
    (evidence-fixture/declaration (zipmap digests (repeat spec)))))

(deftest assessed-objective-is-activated-only-through-its-verified-carrier
  ;; This enters select-live through P05's actual join and P06's partial
  ;; objective.  The external ports are the constructed menu and authority,
  ;; rather than a stubbed objective or carrier verifier.
  (with-redefs [executable/digest :digest
                executable/validate-cascade (constantly {:status :certified})]
    (let [menu (:menu (assessed-fixture/partial-inputs))
          partial-declaration (declaration ["d1" "d2"])
          partial (run-live menu partial-declaration "T")
          carrier (get-in partial [:policy :selected-outer-cascade-policy])
          projection (selected/decision carrier)
          basis (get-in partial [:policy :assessed-policy-basis])
          strict (run-live menu (declaration ["d1" "d2" "d3"]) "T")
          empty (run-live menu (declaration []) "T")]
      (is (= :selected (get-in partial [:policy :status])))
      (is (= :partial (get-in partial [:policy :policy-objective :status])))
      (is (= :partial (get-in partial [:policy :meta-selection :status])))
      (is (= :assessed-population (:basis-kind carrier)))
      (is (= :assessed-population (:scope basis)))
      (is (= ["d3"] (mapv :cascade-digest
                            (get-in partial [:policy :policy-evidence :frontier
                                             :term-absent :policies]))))
      (is (nil? (selected/verify carrier)))
      (is (= (:action (:selected-entry carrier)) (:action partial)))
      (is (= (:action partial) (get-in projection [:decision :action])))
      (is (not= (get-in partial [:policy :policy-objective :assessed-selection :action])
                (:action partial)))
      (is (= :strict-objective
             (get-in strict [:policy :selected-outer-cascade-policy :basis-kind])))
      (is (= :selected (get-in strict [:policy :policy-objective :status])))
      (is (= :partial (get-in empty [:policy :status])))
      (is (= :no-choice (get-in empty [:policy :policy-objective
                                       :assessed-selection :status])))
      (is (= :preparation-needed (get-in empty [:policy :policy-objective
                                                 :assessed-selection :reason])))
      (is (nil? (get-in empty [:policy :selected-outer-cascade-policy])))
      (is (nil? (:action empty)))
      (let [tampered (-> basis
                         (assoc-in [:menu :entries 0 :action :entry-policy] "forged")
                         (assoc :carrier-pin (assessed/carrier-pin
                                              (assoc-in basis [:menu :entries 0 :action
                                                               :entry-policy] "forged"))))
            refused (with-redefs [assessed/seal (fn [& _] tampered)]
                      (run-live menu partial-declaration "T"))
            not-admitted (run-live menu partial-declaration "other")]
        ;; The construction seam is replaced only after P05/P06 ran; P07b's
        ;; verifier remains real and rejects the re-pinned substituted menu.
        (is (= :refused (get-in refused [:policy :status])))
        (is (= :assessed-policy-basis-required (get-in refused [:policy :reason])))
        (is (nil? (:action refused)))
        (is (= :refused (get-in not-admitted [:policy :status])))
        (is (= :selected-policy-target-not-admitted
               (get-in not-admitted [:policy :reason])))
        (is (nil? (:action not-admitted)))))))
