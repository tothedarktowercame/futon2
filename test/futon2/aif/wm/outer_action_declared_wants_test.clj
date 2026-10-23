(ns futon2.aif.wm.outer-action-declared-wants-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.mission-hole-wants :as mission]
            [futon2.aif.outer-policy-evidence :as evidence]
            [futon2.aif.outer-policy-objective :as objective]
            [futon2.aif.selected-outer-cascade-policy :as selected]
            [futon2.aif.selected-outer-cascade-policy-test :as selected-test]
            [futon2.aif.selected-want-outcome :as outcome]
            [futon2.aif.token-outcome :as token]
            [futon2.aif.wm.compiled-cascade-construction :as construction]
            [futon2.aif.wm.executable-cascade :as xc]
            [futon2.aif.wm.family-selection :as family]))

;; Verbatim projections from run 57cd4dc4 and attempt machinery-148/001;
;; only scoring is stubbed: binding, sealing, selection and construction are real.
(defn fixture [name] (edn/read-string (slurp (str "test/resources/m2h/" name ".edn"))))
(def mission-sha "08543cf0c85ab95abd104700fd918934d6709b9400bfd6649a7fbd67b003dd0a")
(def wants [:hole/h850f35e2bcb4 :hole/h4a4524f75478 :hole/h6ec01b880d7a
            :hole/h8f8bb09ea72b :hole/h597e67d59819])
(defn menu [sha text]
  (let [entry (fixture "entry")
        readings (into {} (map (juxt :occurrence-id #(select-keys % [:square :provenance]))
                              (get-in entry [:canonical-cascade :structural :nodes])))
        field {:source-pin (fixture "field-pin") :families [] :target-sources [(assoc-in (fixture "source") [:source-pin :sha256] sha)]}]
    (with-redefs [family/score-family-field (fn [& _] [])
                  mission/read-current-mission (fn [_ _] {:repo "futon5a" :path "holes/missions/M-stack-geometry.md" :text text})]
      (with-redefs-fn {#'family/construct-ranked (fn [_] {:ranked [entry]})}
        #(family/sealed-pre-choice-menu field {:readings (constantly readings)})))))

(deftest pinned-wants-survive-the-real-sealed-handoff
  (let [text (slurp "test/resources/m2h/mission.md")
        _ (is (= mission-sha (identity/sha256 (.getBytes text "UTF-8"))))
        m (menu mission-sha text)
        d (#'selected-test/declaration (mapv :cascade-digest (:entries m)) [0.0])
        ev (evidence/join m d [])
        carrier (selected/seal m ev (objective/evaluate ev m))
        decision (selected/decision carrier)
        entry (:selected-entry carrier)
        compiled (xc/build (get-in entry [:canonical-cascade :structural])
                           {:application-order [] :guards [] :holes {}
                            :operational-clauses (into {} (map (juxt :occurrence-id :clause)) (fixture "steps"))})
        action (:selected-action (construction/construct entry compiled))
        comparison (token/compare-outcomes (fixture "prediction") [] "0382af2386c8afd5df4b9fa07e8538b889162cbe")]
    (is (= :sealed (:status m)))
    (is (nil? (family/verify-sealed-menu m)))
    (is (= :selected (:status decision)))
    (is (= action (get-in decision [:decision :action])))
    (is (= wants (:want action)))
    (is (= (set (map #(vector "M-stack-geometry" %) wants)) (set (keys (:observation-locators action)))))
    (is (= mission-sha (get-in action [:declared-wants :source :sha256])))
    (is (= [2214 2215 2216 2217 2218] (mapv :line (get-in action [:declared-wants :holes]))))
    (is (= :selected-want-after-observation-missing
           (:reason (outcome/receipt {:selected-action action :token-comparison comparison}))))))

(deftest drift-and-missing-source-do-not-invent-wants
  (doseq [[sha text reason] [["different" (slurp "test/resources/m2h/mission.md") :target-source-sha-mismatch]
                             [mission-sha "" :mission-wants-unavailable]]]
    (let [action (get-in (menu sha text) [:entries 0 :action])]
      (is (not (contains? action :want)))
      (is (= reason (get-in action [:declared-wants :reason]))))))

(deftest scorer-tokens-are-not-mission-wants
  (let [action (:action (fixture "entry"))
        pattern-done [(:target action) :pattern-done (get-in action [:arrangement :nodes 0 :occurrence-id])]]
    (is (= :selected-want-identity-missing (:reason (outcome/receipt {:selected-action action}))))
    (is (= :selected-want-target-mismatch
           (:reason (outcome/receipt {:selected-action (assoc action :want [pattern-done])}))))))
