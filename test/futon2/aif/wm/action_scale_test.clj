(ns futon2.aif.wm.action-scale-test
  "Tests for the operator-curated :scale annotation layer
  (M-action-cost-modelling, shape (a) source)."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.wm.action-scale :as action-scale]))

(def curated-example
  {:status :loaded
   :scales {"sorry/wm-ui-hud-mode-rationale-hardcode"
            {:scale :minute :rationale "tooltip swap"}
            "sorry/r3d-per-entity-attribution"
            {:scale :campaign :rationale "blocked on M-INC step (b)"}}})

(deftest curated-target-keeps-curated-scale
  (is (= :minute
         (:scale (action-scale/attach-scale
                  {:type :address-sorry
                   :target "sorry/wm-ui-hud-mode-rationale-hardcode"}
                  curated-example))))
  (is (= :operator-curated
         (:scale-source (action-scale/attach-scale
                         {:type :address-sorry
                          :target "sorry/wm-ui-hud-mode-rationale-hardcode"}
                         curated-example)))))

(deftest uncurated-target-still-carries-a-scale
  ;; The exact bad case the want names: an uncurated candidate must not
  ;; silently lose the annotation — it carries a typed default instead.
  (let [action (action-scale/attach-scale
                {:type :address-sorry :target "sorry/never-curated"}
                curated-example)]
    (is (contains? (set action-scale/scale-vocabulary) (:scale action)))
    (is (= :default (:scale-source action)))))

(deftest default-path-resource-loads
  (let [loaded (action-scale/load-action-scales)]
    (is (= :loaded (:status loaded)))
    (is (= :minute (get-in loaded
                           [:scales "sorry/wm-ui-hud-mode-rationale-hardcode" :scale])))
    (is (= :campaign (get-in loaded
                             [:scales "sorry/r3d-per-entity-attribution" :scale])))))

(deftest attach-to-ranked-covers-every-row
  (let [rows [{:action {:type :address-sorry
                        :target "sorry/wm-ui-hud-mode-rationale-hardcode"}
               :rank 0}
              {:action {:type :advance-mission :target "M-some-mission"} :rank 1}
              {:action {:type :no-op} :rank 2}]
        out (action-scale/attach-to-ranked rows curated-example)]
    (is (= 3 (count out)))
    (is (every? #(contains? % :scale) (map :action out)))
    (is (= [:minute :mission :minute] (mapv (comp :scale :action) out)))))

(deftest attach-scale-never-changes-a-present-scale
  (is (= {:type :no-op :scale :campaign :scale-source :caller}
         (action-scale/attach-scale {:type :no-op :scale :campaign
                                     :scale-source :caller}
                                    curated-example))))

(deftest absent-source-is-typed
  ;; An unreadable source is a typed absence, never nil and never an
  ;; empty map that would read as "loaded with nothing curated".
  (is (= {:status :absent :reason :no-action-scales-source}
         (action-scale/load-action-scales "no/such/resource.edn")))
  (is (= :default
         (:scale-source (action-scale/attach-scale
                         {:type :address-sorry :target "sorry/x"}
                         (action-scale/load-action-scales
                          "no/such/resource.edn"))))))
