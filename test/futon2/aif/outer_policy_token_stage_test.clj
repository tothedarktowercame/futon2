(ns futon2.aif.outer-policy-token-stage-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.selected-outer-cascade-policy :as selected]
            [futon2.aif.mission-hole-wants :as mission]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.wm.family-selection :as family]
            [futon2.aif.token-belief-carry :as carry]
            [futon2.aif.token-outcome :as token]
            [futon2.aif.close-retention :as retention]
            [futon2.aif.d-predecessor-task-authority :as task]
            [futon2.aif.observation-checks :as observation]))

(def action (edn/read-string (slurp "test/resources/m2w/action.edn")))
(def mission-text (slurp "test/resources/m2w/mission.md"))
(def mission-sha "f75873416ac1f328698a2d2292aa5515366bf722ebc232e2098d61ea847a4cdf")
(defn attach [a]
  (#'selected/with-token-stage {:action a :selection-certificate {}} "outer-selection/test"))
(defn source [text]
  (with-redefs [mission/read-current-mission
                (fn [_ _] {:repo "futon5a" :path "holes/missions/M-war-machine-wiring.md" :text text})]
    (mission/mission-source "/unused" {:id (:target action)})))
(defn carried-action []
  (assoc-in action [:declared-wants :universe] (:universe (source mission-text))))

(deftest pinned-initialization-and-measurement
  (is (= mission-sha (identity/sha256 (.getBytes mission-text "UTF-8")))))

(deftest six-wants-reach-the-readers
  (let [a (carried-action) d (attach a)
        stage (get-in d [:selection-certificate :token-belief-stage])
        universe (set (map #(vector (:target a) %) (:want a)))
        occurrence (retention/mint-occurrence {:run-id "m2w" :cohort-id "m2w" :attempt-id "1" :selected-action a :now #(java.time.Instant/parse "2026-10-23T17:00:00Z") :uuid-fn #(java.util.UUID/randomUUID)})
        context (task/context d occurrence [])
        dispatch (task/capture context)
        reads (atom [])
        rows (with-redefs [observation/check-decl-in-file
                          (fn [locator] (swap! reads conj locator) {:observed false})]
               (task/artifact-tokens dispatch (str observation/repo-root "/futon5a") "after"))]
    (is (= [{:target (:target a) :declaration {:facts (get-in a [:declared-wants :universe])
                                             :want (:want a) :interpretations {}}}]
           (:domain-inputs stage)))
    (is (= {#{} 1} (into {} (get-in stage [:initialization :value]))))
    (is (= :assembled-target-facts (get-in stage [:initialization :origin])))
    (is (carry/valid-stage? stage (:initial-belief-receipt d)))
    (is (= universe (get-in stage [:prospective-carry :universe])))
    (is (= universe (:universe dispatch)))
    (is (= universe (set (map :token rows))))
    (is (= 6 (count @reads)))
    (is (= {:status :refused :reason :prediction-input-unavailable :model-id nil}
           (select-keys (token/freeze-prediction d) [:status :reason :model-id])))))

(deftest old-and-invalid-actions-have-no-stage
  (doseq [[a reason] [[action :declared-wants-universe-absent]
                      [(assoc (carried-action) :want nil) :declared-wants-empty]
                      [(assoc (carried-action) :want []) :declared-wants-empty]
                      [(assoc-in (carried-action) [:declared-wants :target-source-pin :path] "/repo/holes/missions/OTHER.md")
                       :declared-wants-target-mismatch]]]
    (let [d (attach a)]
      (is (= reason (get-in d [:selection-certificate :token-belief-stage-status :reason])))
      (is (not (contains? (:selection-certificate d) :token-belief-stage)))
      (is (= :absent (get-in (task/context d nil []) [:prospective-carry :status]))))))

(deftest pre-seal-producer-carries-source-facts
  (let [text "- [x] already done\n- [ ] still needed\n"
        sha (identity/sha256 (.getBytes text "UTF-8"))
        rows [{:target-id (:target action) :source-path "holes/missions/M-war-machine-wiring.md"
               :source-pin {:sha256 sha}}]
        got (with-redefs [mission/read-current-mission
                          (fn [_ _] {:repo "futon5a" :path "holes/missions/M-war-machine-wiring.md" :text text})]
              (#'family/declared-mission-wants (:target action) rows))]
    (is (= 1 (count (:want got))))
    (is (= (:universe (source text)) (get-in got [:declared-wants :universe])))
    (is (= {(first (:want got)) false} (get-in got [:declared-wants :universe])))))
