(ns futon2.aif.selected-outer-cascade-policy-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.outer-policy-evidence :as evidence]
            [futon2.aif.outer-policy-objective :as objective]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.selected-outer-cascade-policy :as sut]
            [futon2.aif.wm.cascade-decision :as cascade]
            [futon2.aif.wm.family-selection :as family]
            [futon2.aif.wm.selection-cascade-identity-test]))

(defn- fixture-var [sym]
  (var-get (ns-resolve 'futon2.aif.wm.selection-cascade-identity-test sym)))
(defn- pin [p c] {:path p :sha256 (apply str (repeat 64 c))})
(defn- declaration [digests ln-es]
  (let [component (fn [value c]
                    {:value value :units :nats :source-pin (pin (str "wm://" c) c)})
        spec (fn [ln-e]
               {:prior-alpha [1.0 1.0]
                :ln-e {:resource (component ln-e "a")
                       :cadence (component 0.0 "b")
                       :continuation (component 0.0 "c")}})
        d {:schema :wm/outer-policy-evidence-declaration-v1
           :outcome-vocabulary [:success :failure]
           :gamma 1.0
           :preference-authority (pin "wm://preference" "d")
           :preference {:vocabulary [:success :failure]
                        :distribution [0.7 0.3] :units :probability
                        :authority (pin "wm://preference" "d")}
           :policies (into {} (map vector digests (map spec ln-es)))}]
    (assoc d :pin {:path "wm://declaration"
                   :sha256 (identity/sha256
                            (.getBytes (pr-str d) "UTF-8"))})))

(defn- production-carriers []
  (let [policy-fn (fixture-var 'occurrence-policy)
        family-fn (fixture-var 'family)
        readings (fixture-var 'readings)
        families [(family-fn (policy-fn 1) (policy-fn 2))]
        field {:schema :wm/pre-choice-policy-field-v1
               :source-pin (pin "wm://field" "e")
               :coverage {:targets 1 :current 1}
               :families families}
        menu (family/sealed-pre-choice-menu field {:readings (constantly readings)})
        digests (mapv :cascade-digest (:entries menu))
        ev (evidence/join menu (declaration digests [0.0 1.0]) [])
        result (objective/evaluate ev menu)
        carrier (sut/seal menu ev result)]
    {:menu menu :evidence ev :objective result :carrier carrier}))

(defn- reseal [carrier]
  (assoc carrier :carrier-pin (sut/carrier-pin carrier)))
(defn- reseal-objective [carrier]
  (let [receipt (:objective-receipt carrier)
        receipt (assoc receipt :objective-pin (sut/objective-pin receipt))]
    (-> carrier (assoc :objective-receipt receipt
                       :objective-pin (:objective-pin receipt)) reseal)))

(deftest sealed-handoff-is-production-shaped-and-total
  (let [{:keys [carrier objective]} (production-carriers)]
    (is (= :selected (:status objective)))
    (is (= :sealed (:status carrier)))
    (is (nil? (sut/verify carrier)))
    (is (= (:cascade-digest carrier)
           (get-in carrier [:selected-entry :canonical-cascade :digest])))
    (doseq [[reason mutate]
            [[:selected-policy-target-mismatch #(-> % (assoc :target-id "substitute") reseal)]
             [:selected-policy-occurrence-mismatch #(-> % (assoc :policy-id "substitute") reseal)]
             [:selected-policy-digest-mismatch #(-> % (assoc :cascade-digest (apply str (repeat 64 "f"))) reseal)]
             [:selected-policy-menu-invalid #(-> % (assoc-in [:sealed-menu :menu-pin :sha256]
                                                              (apply str (repeat 64 "0"))) reseal)]
             [:selected-policy-evidence-invalid #(-> % (assoc-in [:evidence-carrier :gamma] 2.0) reseal)]
             [:selected-policy-objective-recomputation
              #(-> % (update-in [:objective-receipt :selected :terms :G] inc)
                   reseal-objective)]]]
      (is (= reason (sut/verify (mutate carrier))) (name reason)))))

(deftest tie-cannot-be-sealed
  (let [{:keys [menu]} (production-carriers)
        digests (mapv :cascade-digest (:entries menu))
        ev (evidence/join menu (declaration digests [0.0 0.0]) [])
        tied (objective/evaluate ev menu)
        carrier (sut/seal menu ev tied)]
    (is (= :abstained (:status tied)))
    (is (= :refused (:status carrier)))
    (is (= :strict-policy-selection-required (:reason carrier)))
    (is (= 2 (count (:tied carrier))))))

(deftest consume-without-selection-and-legacy-boundary
  (let [{:keys [carrier]} (production-carriers)
        calls (atom {:field 0 :selector 0})
        field-var (ns-resolve 'futon2.aif.wm.cascade-decision 'target-policy-field)
        selector-var (ns-resolve 'futon2.aif.wm.cascade-decision 'reading-family-decision)
        result (with-redefs-fn
                 {field-var (fn [& _] (swap! calls update :field inc))
                  selector-var (fn [& _] (swap! calls update :selector inc))}
                 #(cascade/cascade-decision {} {:selected-outer-cascade-policy carrier
                                                :outer-policy-selection-claimed? true}))]
    (is (= :selected (get-in result [:decision :status])))
    (is (= {:field 0 :selector 0} @calls))
    (is (= (:cascade-digest carrier)
           (get-in result [:decision :selection-certificate
                           :selected-canonical-cascade :digest])))
    (is (= :abstained
           (get-in (cascade/cascade-decision {} {:outer-policy-selection-claimed? true})
                   [:decision :status])))
    (let [legacy (with-redefs-fn
                   {field-var (fn [& _] (swap! calls update :field inc) {:status :formed :families []})
                    selector-var (fn [& _] (swap! calls update :selector inc)
                                   {:decision {:status :abstained :refusals [{:kind :legacy}]}})}
                   #(cascade/cascade-decision {} {}))]
      (is (= :abstained (get-in legacy [:decision :status])))
      (is (= {:field 1 :selector 1} @calls)))))

(deftest second-pass-retains-byte-identical-carrier
  (let [{:keys [carrier]} (production-carriers)
        calls (atom 0)
        receipt {:schema :wm/outer-task-selection-v1
                 :policy {:kind :cascade-live-policy-v1 :status :selected
                          :selected-outer-cascade-policy carrier}
                 :chosen {:id (:target-id carrier)}
                 :action {:target (:target-id carrier)}}
        select (runtime/retained-outer-selector
                (fn [_] (swap! calls inc) receipt) {:graph :pinned})
        first-pass (select {:tasks []})
        second-pass (select {:tasks [] :selected-reading-repaired true})]
    (is (= 1 @calls))
    (is (identical? first-pass second-pass))
    (is (= (:carrier-pin carrier)
           (get-in second-pass [:policy :selected-outer-cascade-policy :carrier-pin])))))
