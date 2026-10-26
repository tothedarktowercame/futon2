(ns futon2.aif.reading-preparation-outcome-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.reading-preparation-outcome :as sut]
            [futon2.aif.selected-target-reading :as reading]))

;; Live pin: wm-full-loop-machinery-150 attempt-002 002-selection.edn
;; (:selection-as-of "2026-10-24T20:40:24.272212785Z"), receipt at
;; [:payload :judgment :outer-task-selection].  The live :policy map DOES
;; carry a :reason key (value nil), so select-keys keeps it.
(def pinned-judgement
  {:outer-task-selection
   {:schema :wm/outer-task-selection-v1
    :chosen {:id "M-war-machine-wiring" :source {:path "futon5a/holes/missions/M-war-machine-wiring.md"}}
    :policy {:status :selected :reason nil
             :selected-outer-cascade-policy
             {:target-id "M-war-machine-wiring"
              :policy-id "M-war-machine-wiring/policy-2629917c87a61978be8f"}}}})

(def pinned-need-judgement
  "A judgement whose only selected need is the orphan target: chosen
  dissoc'd, one sole failing family, one cascade source row."
  {:outer-task-selection
   {:schema :wm/outer-task-selection-v1
    :policy {:status :refused :reason :incomplete-policy-menu-coverage}}
   :decision {:selection-certificate
              {:target-policy-field
               {:families [{:target-id "M-orphan"
                            :failures [{:kind :no-current-target-reading
                                        :expected-digest "abc"}]}]}}}
   :cascade-problems {:target-sources [{:target-id "M-orphan"
                                        :source-path "x.md"}]}})

(deftest outcome-projects-the-selected-target-reading-step
  (testing "case 1: reading-era click — no need, no record (live pin, whole map)"
    (is (nil? (reading/selected-need pinned-judgement))
        "the pin is a real no-need click, not a vacuous one")
    (is (= {:schema :wm/reading-preparation-outcome-v1
            :status :not-run
            :reason :no-selected-need
            :chosen "M-war-machine-wiring"
            :policy {:status :selected :reason nil}}
           (sut/outcome pinned-judgement nil))))
  (testing "case 2: a need existed but no reading fn"
    (let [need (reading/selected-need pinned-need-judgement)]
      (is (map? need) "selected-need really returns a need on this judgement")
      (is (= "M-orphan" (:target need)))
      (is (= :no-current-target-reading (:failure need)))
      (is (= "abc" (:excerpt-digest need)))
      (is (= {:schema :wm/reading-preparation-outcome-v1
              :status :not-run
              :reason :reading-fn-absent
              :chosen {:absent :no-chosen-target}
              :policy {:status :refused :reason :incomplete-policy-menu-coverage}
              :need {:target "M-orphan"
                     :failure :no-current-target-reading
                     :excerpt-digest "abc"}}
             (sut/outcome pinned-need-judgement nil)))))
  (testing "case 3: preparation record — bounded projection, nothing else"
    (let [record {:schema :wm/target-reading-preparation-v1
                  :status :pending :reason :batch-limit
                  :deadline-ms 12345 :field-source-pin "pin-1"
                  :records [{:need {:target "A"} :published true :analysis {:big 1}}
                            {:need {:target "B"} :published false :analysis {:big 2}}]
                  :pending [{:target "C" :excerpt-digest "d"}]
                  :frontier {:huge true}}
          r (sut/outcome pinned-judgement record)]
      (is (= :ran (:status r)))
      (is (= #{:schema :status :policy :chosen :reason :preparation-status
               :deadline-ms :field-source-pin :attempted :published :pending}
             (set (keys r))))
      (is (= :batch-limit (:reason r)))
      (is (= :pending (:preparation-status r)))
      (is (= 12345 (:deadline-ms r)))
      (is (= "pin-1" (:field-source-pin r)))
      (is (= ["A" "B"] (:attempted r)))
      (is (= ["A"] (:published r)))
      (is (= ["C"] (:pending r)))
      (is (nil? (:records r)))
      (is (nil? (:frontier r))))
    (testing "bad case: preparation schema with :records nil is still :ran"
      (let [r (sut/outcome pinned-judgement
                           {:schema :wm/target-reading-preparation-v1
                            :status :prepared :reason :batch-complete})]
        (is (= :ran (:status r)))
        (is (= [] (:attempted r)))
        (is (= [] (:published r)))
        (is (= [] (:pending r))))))
  (testing "case 4: the two repair-need! shapes"
    (is (= {:schema :wm/reading-preparation-outcome-v1
            :status :selected-target-reading
            :chosen "M-war-machine-wiring"
            :policy {:status :selected :reason nil}
            :target "M-war-machine-wiring"
            :published true
            :request-status :published}
           (sut/outcome pinned-judgement
                        {:schema :wm/selected-target-reading-request-v1
                         :status :published :published true
                         :target "M-war-machine-wiring"})))
    (let [r (sut/outcome pinned-judgement
                         {:schema :wm/selected-target-reading-request-v1
                          :status :refused :kind :reading-preparation-threw
                          :published false :target "M-war-machine-wiring"})]
      (is (= :selected-target-reading (:status r)))
      (is (= :refused (:request-status r)))
      (is (= false (:published r)))
      (is (= :reading-preparation-threw (:kind r)))))
  (testing "case 5: unknown shapes and the nil judgement do not throw"
    (let [r (sut/outcome pinned-judgement {:foo 1})]
      (is (= :unrecognised (:status r)))
      (is (nil? (:record-schema r)))
      (is (= #{:schema :status :policy :chosen :record-schema} (set (keys r)))))
    (let [r (sut/outcome nil nil)]
      (is (= {:schema :wm/reading-preparation-outcome-v1
              :status :not-run
              :reason :no-selected-need
              :chosen {:absent :no-chosen-target}
              :policy {}}
             r)))
    (testing "bad case: :published true on the request schema stays case 4"
      (let [r (sut/outcome nil {:schema :wm/selected-target-reading-request-v1
                                :status :published :published true
                                :target "T" :records []})]
        (is (= :selected-target-reading (:status r)))
        (is (= true (:published r)))
        (is (nil? (:attempted r)))))))
