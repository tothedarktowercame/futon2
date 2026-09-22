(ns futon2.aif.token-observation-initialization-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.cascade-problems :as problems]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.cascade-sources :as sources]
            [futon2.aif.d-predecessor-task-authority :as task]
            [futon2.aif.d-predecessor-task-authority-test :as fixture]
            [futon2.aif.observation-checks :as checks]
            [futon2.aif.scoring-input-receipts :as receipts]
            [futon2.aif.token-belief-carry :as carry]
            [futon2.aif.token-belief-predecessor :as predecessor]
            [futon2.aif.token-initialization-policy :as policy]
            [futon2.aif.token-outcome :as outcome]
            [futon2.aif.token-outcome-test :as want]
            [futon2.report.war-machine :as wm]))

(use-fixtures :once hermetic/with-hermetic-stores)

(def on (assoc policy/disabled :enabled true))
(def target want/target)
(defn q [token] [target token])
(def updater (q :hole/h6378c65a4012))
(def unknown (q :missing-revision))
(def fixture-locators
  (assoc (into {} (map (fn [[t l]] [t (assoc l :repo "repo" :sha
                                           (if (= t :admission/task-stated) "HEAD~1" "HEAD"))]))
               (:locators want/declaration))
         :artifact {:class :C3 :repo "repo" :sha "HEAD" :path "created.clj"}
         :missing-revision {:class :C4 :repo "repo" :sha "unavailable"
                            :path "created.clj" :decl "(ns created)"}))

(defn with-two-ticks [f]
  (fixture/with-artifact
   {:target target :locators fixture-locators
    :universe (set (map q (keys fixture-locators)))
    :before-files {"holes/missions/M-aif-policy-conditioned-eig.md"
                   (slurp "test/fixtures/M-aif-policy-conditioned-eig-pre-aeb352f8.md")}}
   (fn [{:keys [inputs expected jobs root] :as env}]
     (task/produce! root inputs expected jobs)
     (let [signed (task/read-observations-v2 root expected jobs)
           execution (task/read-predecessor root expected jobs)
           declaration (clojure.core/first (get-in inputs [:dispatch :declarations]))
           current {:policy on :declaration-sha256 (:sha256 declaration)
                    :locators fixture-locators
                    :schedule (sources/observation-schedule (:snapshot declaration))
                    :observations (into {} (map (fn [[t l]] [t ((if (= :C3 (:class l))
                                                                  checks/check-path-exists checks/check-decl-in-file) l)]))
                                        fixture-locators)}
           ;; The first tick has no artifact yet. The second tick's fresh
           ;; reader is unavailable for :artifact, but the previous admitted
           ;; revision check is true: that control MUST change actual q0.
           facts (into {} (map (fn [[t row]] [t (if (boolean? (:observed row)) (:observed row) :unknown)]))
                       (:observations current))
           facts (assoc facts :artifact :unknown)
           current (assoc-in current [:observations :artifact] {:status :missing :kind :reader-unavailable})
           src {:universes {target facts} :wants {target (:want want/declaration)}
                :locators {target fixture-locators}
                :interpretations {target {:patterns (:patterns want/declaration)
                                          :receipts (:interpretation-receipts want/declaration)}}
                :candidates {target (:candidates want/declaration)}
                :context-of (constantly :WM) :beta-by-context {:WM {:beta 1 :status :declared}}
                :horizon-steps 1 :token-initialization {target current}}
           assembled (problems/assemble {:targets [target] :sources src})
           opts {:cascade-habit-path (str root "/absent-habit.edn")
                 :token-belief-context {:occurrence-id "carry"}
                 :live-c {:derived {:want #{(keyword "alive" target)}
                                   :weights {(keyword "alive" target) 1} :lam 1
                                   :entries [] :gaps [] :refusals nil :signature "fixture"}}}]
       (is (= :admitted (:status signed)))
       (is (= 1 (count (:problems assembled))))
       (with-redefs [predecessor/production-authority (fn [_] execution)
                     predecessor/observation-authority (fn [_] signed)]
         (let [first-decision (:decision (wm/cascade-decision assembled opts))
               prior (get-in first-decision [:selection-certificate :token-belief-stage :prospective-carry])
               trace {:decision first-decision :d-task-context expected}
               reads (atom [])
               second-decision (binding [receipts/*habit-reads* reads]
                                 (:decision (wm/cascade-decision assembled
                                           (assoc opts :prospective-token-carry prior
                                                  :token-belief-predecessor-trace trace))))]
           (f (assoc env :signed signed :execution execution :current current
                     :assembled assembled :opts opts :first first-decision :second second-decision
                     :prior prior :trace trace :habit-reads (receipts/habit-log @reads)))))))))

(deftest two-ticks-use-signed-false-in-actual-selection
  (with-two-ticks
    (fn [{:keys [first second habit-reads]}]
      (let [prediction (outcome/freeze-prediction first)
            stage (get-in second [:selection-certificate :token-belief-stage])
            input (get-in second [:selection-certificate :token-belief-input])
            updates (into {} (map (juxt :token identity)) (:observation-updates input))
            q0 (get-in second [:selection-certificate :precision-family :model :q0])]
        (is (= 1 (:predicted (clojure.core/first (filter #(= updater (:token %)) (:wanted prediction))))))
        (is (= :observed-initialization (:conditioning-status input)))
        (is (= [:updated false] ((juxt :status :observed) (updates updater))))
        (is (= :not-updated (:status (updates unknown))))
        (is (= :observation-missing (:kind (updates unknown))))
        (is (= (:continuation-belief input) q0))
        (is (every? #(not (contains? % updater)) (keys q0)))
        (is (every? #(contains? % (q :artifact)) (keys q0)))
        (is (not= (:value (:initial-belief-receipt second)) q0))
        (is (every? #(= q0 (get-in % [:evaluations 0 :incoming-belief]))
                    (get-in second [:selection-certificate :node-evaluation-traces])))
        (is (carry/valid-stage? stage (:initial-belief-receipt second)))
        (is (predecessor/valid-input? (edn/read-string (pr-str input)) stage))
        (let [record {:decision second :habit-reads habit-reads}]
          (is (= :valid (:status (receipts/validate-record record))) (pr-str (receipts/validate-record record)))
          (is (= :invalid (:status (receipts/validate-record
                                   (assoc-in record [:decision :selection-certificate :token-belief-input
                                                     :continuation-belief] {#{updater} 1}))))))))))

(deftest wrong-occurrence-domain-and-stale-observations-refuse
  (with-two-ticks
    (fn [{:keys [second signed]}]
      (let [stage (get-in second [:selection-certificate :token-belief-stage])
            input (get-in second [:selection-certificate :token-belief-input])
            inspection (:inspection input)
            check #(policy/apply-observations stage inspection %)]
        (is (= :observation-occurrence-mismatch (:kind (check (assoc signed :occurrence {})))))
        (is (= :observation-domain-changed (:kind (check (assoc signed :universe #{})))))
        (is (= :observation-predecessor-mismatch (:kind (check (assoc signed :carry-occurrence-id "older")))))
        (let [stale (assoc-in stage [:observation-initialization target :observations (clojure.core/second updater)
                                    :evidence :resolved-sha] "another-revision")
              result (policy/apply-observations stale inspection signed)
              row (clojure.core/first (filter #(= updater (:token %)) (:observation-updates result)))]
          (is (= :refused (:status row)))
          (is (= :stale-or-unordered-observation (:kind row))))
        (doseq [bad [(assoc input :conditioning-status :not-wired)
                     (assoc-in input [:observation-updates 0 :observed] :fabricated)
                     (assoc-in input [:observation-initialization :source :carry-occurrence-id] "older")]]
          (is (not (predecessor/valid-input? bad stage))))))))

(deftest declared-switch-defaults-off-and-replays
  (is (policy/valid-policy? policy/disabled))
  (is (not (policy/valid-policy? (assoc on :temporal-order :latest-wins))))
  (with-two-ticks
    (fn [{:keys [second signed]}]
      (let [old-stage (get-in second [:selection-certificate :token-belief-stage])
            context (assoc-in (:observation-initialization old-stage) [target :policy] policy/disabled)
            stage (carry/stage (:initialization old-stage) (:domain-inputs old-stage)
                               (:prospective-prior old-stage)
                               {:occurrence-id (:occurrence-id old-stage) :observation-initialization context})
            old-input (get-in second [:selection-certificate :token-belief-input])
            input (predecessor/input-receipt stage (:inspection old-input)
                                            (get-in old-input [:carry-admission :authority]) signed)]
        (is (= :observation-initialization-disabled (:reason input)))
        (is (= [] (:observation-updates input)))
        (is (= (get-in stage [:initialization :value]) (:continuation-belief input)))
        (is (predecessor/valid-input? input stage))))))


(deftest default-off-real-declarations-are-recorded
  (let [declared (sources/load-declared)]
    (is (seq (:read-occurrences declared)))
    (is (every? #(= policy/disabled (:token-initialization-policy %))
                (:read-occurrences declared)))
    (is (every? #(= policy/disabled (:policy %)) (vals (:token-initialization declared))))))

(deftest off-selection-does-not-read-signed-authority
  (with-two-ticks
    (fn [{:keys [assembled opts trace prior]}]
      (let [off (assoc-in assembled [:problems 0 :cascade-problem :token-initialization :policy] policy/disabled)
            decision (with-redefs [predecessor/observation-authority
                                  (fn [_] (throw (ex-info "OFF must not read signed authority" {})))]
                       (:decision (wm/cascade-decision off (assoc opts :prospective-token-carry prior
                                                                  :token-belief-predecessor-trace trace))))
            input (get-in decision [:selection-certificate :token-belief-input])]
        (is (= :observation-initialization-disabled (:reason input)))
        (is (= (get-in decision [:initial-belief-receipt :value])
               (get-in decision [:selection-certificate :precision-family :model :q0])))
        (is (= [] (:observation-updates input)))))))

(deftest historical-input-schemas-replay-without-observation-updates
  (let [initial {:value {#{} 1}}
        stage (carry/stage initial [] nil {:occurrence-id "historical"})
        inspection (predecessor/inspect-trace nil)
        v1 (assoc (predecessor/input-receipt stage inspection predecessor/legacy-unavailable-authority)
                  :schema :wm/token-belief-input-v1)
        v2 (predecessor/input-receipt stage inspection
                                     {:status :refused :authority task/authority :scope task/scope
                                      :kind :carry-no-predecessor})]
    (doseq [receipt [v1 v2]]
      (is (predecessor/valid-input? (edn/read-string (pr-str receipt)) stage))
      (is (= {#{} 1} (:continuation-belief receipt)))
      (is (not (predecessor/valid-input? (assoc receipt :continuation-belief {#{:prediction} 1}) stage))))))
