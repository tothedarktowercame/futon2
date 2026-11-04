(ns futon2.aif.ticket-measurement-path-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runner]
            [futon2.aif.mission-observation :as observation]))

(def target "T-car3-queue-routing-design-note")
(def wants
  (mapv (fn [n] [target [:pattern-done n]]) (range 7)))
(def source
  {:path "/home/joe/code/futon3c/holes/tickets/T-car3-queue-routing-design-note.md"
   :sha256 "e539d69d02ab28760e704dbfa825e015ba49a813d933e1f086471c5e4808282c"})
(def ticket-carrier {:id target :kind :ticket :path (:path source) :source source})
(def selected-action
  {:kind :cascade-candidate :target target :want wants
   :observation-locators {}})
(def located-action
  (assoc selected-action :observation-locators
         (into {} (map (fn [want]
                         [want {:repo "/repo" :class :C3 :path (str "proof/" (hash want))}])
                       wants))))

(defn- runner-var [sym]
  (ns-resolve 'futon2.aif.full-loop-runner sym))

(defn- applicable? [mission action]
  (if-let [f (runner-var 'mission-measurement-applicable?)]
    (f mission action nil nil)
    ;; Counterfactual adapter: the pre-fix runner's inline predicate. Keeping
    ;; this here makes the witness fail by assertion on main, not by linkage.
    (or (.startsWith (str (:target action)) "M-")
        (and (.startsWith (str (:target action)) "E-") (seq (:want action)))
        (and (= :preparation (get-in action [:ticket-readiness :increment]))
             (seq (:want action))))))

(defn- capture-baseline [route? dispatch repository revision]
  (if-let [f (runner-var 'capture-pre-author-mission-baseline)]
    (f route? dispatch repository revision)
    (when route?
      (observation/capture-before
       {:dispatch dispatch :repository repository :revision revision}))))

(defn- terminal-action [route? d-task-dispatch construction]
  (if-let [f (runner-var 'terminal-measurement-action)]
    (f route? d-task-dispatch construction)
    (if route?
      (get-in d-task-dispatch [:dispatch :occurrence :action/value])
      (get-in construction [:selected-action]))))

(deftest run-23ab2833-ticket-without-locators-stays-off-measurement-path
  (let [occurrence-action selected-action
        d-task-dispatch {:dispatch {:occurrence {:action/value occurrence-action}}}
        construction {:selected-action (assoc selected-action :want [:late-copy])}
        route? (boolean (applicable? ticket-carrier selected-action))]
    (is (false? route?))
    (with-redefs [observation/capture-before
                  (fn [_] (throw (ex-info "baseline must not run" {})))]
      (is (nil? (capture-baseline route? (:dispatch d-task-dispatch)
                                  "/repo" "before-sha"))))
    (testing "terminal accounting keeps the pre-measurement construction path"
      (is (= (:selected-action construction)
             (terminal-action route? d-task-dispatch construction)))
      (is (not= occurrence-action
                (terminal-action route? d-task-dispatch construction))))))

(deftest source-backed-ticket-with-wants-and-locators-is-measurement-applicable
  (is (true? (boolean (applicable? ticket-carrier located-action)))))

(deftest ordinary-ticket-requires-both-source-and-wants
  (is (false? (boolean (applicable? (dissoc ticket-carrier :source)
                                    selected-action))))
  (is (false? (boolean (applicable? ticket-carrier
                                    (assoc selected-action :want [])))))
  (let [partially-located
        (update located-action :observation-locators dissoc (first wants))]
    (is (false? (boolean (applicable? ticket-carrier partially-located))))))

(deftest existing-preparation-mission-and-excursion-admissions-are-unchanged
  (let [preparation (assoc selected-action
                           :want [[:preparation-want]]
                           :ticket-readiness {:increment :preparation})]
    (is (true? (boolean (applicable? {:id target} preparation)))))
  (is (true? (boolean (applicable? nil {:target "M-control" :want []}))))
  (is (true? (boolean (applicable? nil {:target "E-control" :want [:e]}))))
  (is (false? (boolean (applicable? nil {:target "E-control" :want []})))))
