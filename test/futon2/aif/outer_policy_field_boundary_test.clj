(ns futon2.aif.outer-policy-field-boundary-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.meta-live-outer-selector :as live]))

(defn- pin [path ch] {:path path :sha256 (apply str (repeat 64 ch))})

(def snapshot
  {:schema :wm/pipeline-cascade-snapshot-v1
   :summary-source (pin "summary" "a") :graph-source (pin "graph" "b")
   :summary {:consistent? true
             :standards {:s1-regenerates true :s2-evidence true
                         :s3-reconstitution true :s4-honest-holes true
                         :s5-composed true}}
   :graph {:section-status {:clusters {:status :ok}}
           :lineage [] :clusters [{:mission "M-a"} {:mission "M-b"}]
           :arrows [] :held [] :tickets {:items []} :patterns {:edges []}}})

(def tasks
  [{:id "M-a" :kind :mission :source (pin "/tmp/M-a.md" "c")}
   {:id "M-b" :kind :mission :source (pin "/tmp/M-b.md" "d")}])

(defn- field [seen status]
  (fn [xs config]
    (reset! seen {:ids (mapv :id xs) :config config})
    {:schema :wm/pre-choice-policy-field-v1
     :source-pin (pin "wm://menu" "e")
     :coverage {:targets 2 :current (if (= status :computed) 2 1)}
     :families [{:target-id "M-a" :status :computed :policies [{:policy-id "p-a"}]}
                {:target-id "M-b" :status status
                 :policies (if (= status :computed) [{:policy-id "p-b"}] [])
                 :failures (if (= status :computed) [] [{:kind :no-current-target-reading}])}]}))

(defn- sealed-menu [field _]
  {:schema :wm/sealed-outer-policy-menu-v1
   :status :sealed
   :field-source-pin (:source-pin field)
   :menu-pin (pin "wm://sealed-menu" "f")
   :entries [{:policy-id "p-a" :cascade-digest (apply str (repeat 64 "1"))}
             {:policy-id "p-b" :cascade-digest (apply str (repeat 64 "2"))}]})

(deftest ordinary-path-stops-at-one-complete-policy-menu
  (let [seen (atom nil) target-select-calls (atom 0)
        config {:target-reading-root "/readings" :pattern-graph-path "/graph"}
        receipt (live/select-live
                 {:tasks tasks :fetch-snapshot (constantly snapshot)
                  :candidate-fn (fn [& _] (swap! target-select-calls inc) [])
                  :policy-field-config config
                  :policy-field-fn (field seen :computed)
                  :policy-menu-fn sealed-menu})]
    (is (zero? @target-select-calls))
    (is (= ["M-a" "M-b"] (:ids @seen)))
    (is (= config (:config @seen)))
    (is (= :cascade-live-policy-menu-v1 (get-in receipt [:policy :kind])))
    (is (= :refused (get-in receipt [:policy :status])))
    (is (= :outer-policy-evidence-provider-required
           (get-in receipt [:policy :reason])))
    (is (= (pin "wm://menu" "e") (get-in receipt [:policy :field-source-pin])))
    (is (nil? (:action receipt)))
    (is (= :outer-policy-evidence-provider-required
           (get-in receipt [:q8-frontier :reason])))))

(deftest incomplete-policy-field-measures-coverage-without-veto
  (let [receipt (live/select-live
                 {:tasks tasks :fetch-snapshot (constantly snapshot)
                  :policy-field-config {}
                  :policy-field-fn (field (atom nil) :failed)
                  :policy-menu-fn sealed-menu})]
    (is (= :refused (get-in receipt [:policy :status])))
    (is (= :outer-policy-evidence-provider-required (get-in receipt [:policy :reason])))
    (is (= {:eligible 2 :assessed 1 :reading-absent 1}
           (select-keys (get-in receipt [:policy :frontier])
                        [:eligible :assessed :reading-absent])))
    (is (= ["M-b"] (get-in receipt [:policy :missing-targets])))
    (is (some #{:no-current-target-reading} (get-in receipt [:policy :absent-terms])))
    (is (nil? (:action receipt)))
    (is (= [{:target-id "M-b" :status :failed :policies []
             :failures [{:kind :no-current-target-reading}]}]
           (filterv #(not= :computed (:status %))
                    (get-in receipt [:policy-field :families]))))))

(deftest verified-injury-keeps-algorithm-branch-separate
  (let [field-calls (atom 0)
        algorithm {:id "A-heal" :kind :algorithm :source (pin "A" "f")
                   :repairs-capability :wm-click-completes-with-reviewable-receipts
                   :algorithm-admission
                   {:injury-observation {:status :active
                    :capability :wm-click-completes-with-reviewable-receipts
                    :injury-verification {:status :verified}}}
                   :resource-admission {:required {:ordinary-clicks 1}}}
        receipt (live/select-live
                 {:tasks (conj tasks algorithm)
                  :fetch-snapshot (constantly snapshot)
                  :policy-field-fn (fn [& _] (swap! field-calls inc))})]
    (is (zero? @field-calls))
    (is (= :verified-machine-injury-algorithm (get-in receipt [:policy :branch])))
    (is (= "A-heal" (get-in receipt [:action :target])))))
