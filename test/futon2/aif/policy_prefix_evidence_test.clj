(ns futon2.aif.policy-prefix-evidence-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.policy-prefix-evidence :as prefix]
            [futon2.aif.policy :as policy]
            [futon2.aif.cascade-selection :as selection]
            [futon2.aif.cascade-policy :as cascade]
            [futon2.aif.efe :as efe]
            [futon2.aif.cascade-free-energy :as old-f])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def model {:schema :wm/observation-model-v1 :backend :exact-enumeration
            :kind :exact-checks :universe #{:x}
            :rates {:x {:false-neg 0 :false-pos 0}}
            :provenance {:status :synthetic :calibrated false :source "H4 arithmetic control"}})
(def identity-b {#{} {#{} 1} #{:x} {#{:x} 1}})
(defn step [n id]
  {:transition identity-b :context {:occurrence-id id :tau n}
   :observation {:status :observed :present #{:x} :absent #{} :occurrence-id id :tau n}})
(defn input [p]
  {:policy :p :model model :prior {#{} (- 1 p) #{:x} p}
   :steps [(step 1 "obs-1")] :z-semantics :per-step-redraw})
(defn near? [x y] (< (Math/abs (- x y)) 1e-12))
(defn candidate [id r]
  {:id id :habit 1 :g 0 :f (:f r) :f-status (:status r)})
(defn posterior [rs]
  (selection/selection-posterior {:beta 2 :candidates (mapv candidate (range) rs)}))

(deftest conditional-evidence-and-posterior
  (let [a (prefix/evaluate-synthetic (input 1/4))
        b (prefix/evaluate-synthetic (input 3/4))
        probs (posterior [a b])]
    (is (= :computed (:status a)))
    (is (near? (Math/log 4) (:f a)))
    (is (near? (Math/log (/ 4.0 3)) (:f b)))
    (is (= {#{:x} 1} (:posterior a)))
    (is (= 1/4 (get-in a [:steps 0 :result :probability])))
    (is (near? 0.25 (get probs 0)))
    (is (near? 0.75 (get probs 1)))
    (is (= {0 0.5 1 0.5} (posterior [a a])))
    ;; beta=2 distinguishes unscaled F from incorrectly tempering F too.
    (let [cs [(assoc (candidate 0 a) :g 2) (candidate 1 b)]
          ps (selection/selection-posterior {:beta 2 :candidates cs})]
      (is (near? (/ (Math/exp -1.0) 3) (/ (get ps 0) (get ps 1)))))
    (is (= :synthetic-prefix-arithmetic-only (:scope a)))))

(deftest sequential-consumption-not-rescoring
  (let [r (prefix/evaluate-synthetic (assoc (input 1/4) :steps [(step 1 "one") (step 2 "two")]))]
    (is (= :computed (:status r)))
    (is (near? (Math/log 4) (:f r)))
    (is (= 1 (get-in r [:steps 1 :result :probability])))
    (is (= (get-in r [:steps 0 :result :posterior]) (get-in r [:steps 1 :prior]))))
  (is (= :duplicate-or-missing-occurrence
         (:kind (prefix/evaluate-synthetic (assoc (input 1/4) :steps [(step 1 "one") (step 2 "one")]))))))

(deftest mismatch-and-invalid-controls
  (doseq [[path value expected]
          [[[:steps 0 :observation :occurrence-id] "wrong" :observation-context-mismatch]
           [[:steps 0 :observation :tau] 2 :observation-context-mismatch]
           [[:steps 0 :context :tau] 2 :prefix-horizon-mismatch]
           [[:z-semantics] :persistent :unsupported-z-semantics]
           [[:steps 0 :transition #{}] {#{} 2} :invalid-prefix-transition]]]
    (is (= expected (:kind (prefix/evaluate-synthetic (assoc-in (input 1/4) path value)))))))

(deftest impossibility-is-not-missing
  (let [zero (prefix/evaluate-synthetic (input 0))
        good (prefix/evaluate-synthetic (input 1))]
    (is (= :zero-support (:status zero)))
    (is (not (contains? zero :f)))
    (is (= {0 0.0 1 1.0} (posterior [zero good])))
    (is (= :no-admissible-candidate
           (try (posterior [zero zero]) (catch clojure.lang.ExceptionInfo e (get-in (ex-data e) [:refusal :kind])))))))

(deftest positive-exact-mass-does-not-underflow
  (let [p (/ 1 (reduce *' (repeat 400 10)))
        r (prefix/evaluate-synthetic (input p))]
    (is (zero? (double p)))
    (is (= :computed (:status r)))
    (is (= p (get-in r [:steps 0 :result :probability])))
    (is (near? (* 400 (Math/log 10)) (:f r)))))

(deftest production-shape-stays-honestly-missing
  (let [dir (Files/createTempDirectory "h4-habits-" (make-array FileAttribute 0))
        action {:kind :cascade-candidate :id :a :target "H4-control"
                :precedence [(cascade/token-interpretation :make-x
                              {:guard {:needs #{} :forbids #{}} :produces #{:x}})]}
        ;; Real scorer/selector, no live runner, store, or actuator. Historical
        ;; future-vs-present F must not even be called on the staged path.
        ranked (with-redefs [old-f/policy-free-energy
                            (fn [& _] (throw (ex-info "old F called" {})))]
                 (efe/rank-actions {:cascade-belief {#{} 1}} [action]
                                   {:horizon-steps 1 :cascade-spec {:want #{:x}}
                                    :f-prefix-production? true}))
        supplied (prefix/production-ranked ranked {:conditioning-status :not-wired
                                                   :observation-updates []})
        decision (policy/select-action-cascades supplied {:beta 2 :cascade-habit-path (str dir "/absent.edn")})
        c (first (get-in decision [:selection-certificate :candidates]))]
    (is (vector? ranked))
    (is (= :not-supplied (:f-status c)))
    (is (nil? (:f c)))
    (is (= :no-admitted-policy-prefix (:reason c)))
    (is (= :not-wired (get-in c [:f-prefix :conditioning :conditioning-status])))
    (is (= prefix/pending-dependency (get-in c [:f-prefix :pending-dependency])))
    (is (= action (get-in c [:f-prefix :policy])))
    (println "H4-STAGED-PRODUCTION-SHAPE" (pr-str (select-keys c [:id :f :f-status :f-prefix])))
    (is (= :not-supplied
           (get-in (first (prefix/production-ranked [(assoc (first ranked) :f 123)] nil))
                   [:f-prefix :status])))
    (Files/delete dir)))

(deftest zero-support-cannot-win-by-sharing-an-action
  (let [dir (Files/createTempDirectory "h4-zero-" (make-array FileAttribute 0))
        actions [{:kind :cascade-candidate :id :impossible :precedence [:same]}
                 {:kind :cascade-candidate :id :supported :precedence [:same]}]
        entries (mapv (fn [a p] {:action a :controller-score 0
                                :f-prefix (prefix/evaluate-synthetic (assoc (input p) :policy a))})
                      actions [0 1])
        opts {:beta 2 :cascade-habit-path (str dir "/absent.edn")}]
    (is (= (second actions) (:action (policy/select-action-cascades entries opts))))
    (let [empty-action {:kind :cascade-candidate :id :empty :precedence []}
          empty-entry {:action empty-action :controller-score 0
                       :f-prefix (prefix/evaluate-synthetic (assoc (input 1) :policy empty-action))}]
      (is (= :no-acting-cascade-candidate
             (try (policy/select-action-cascades [(first entries) empty-entry] opts)
                  (catch clojure.lang.ExceptionInfo e (get-in (ex-data e) [:refusal :kind]))))))
    (Files/delete dir)))

(deftest full-selector-arithmetic-only
  (let [dir (Files/createTempDirectory "h4-selector-" (make-array FileAttribute 0))
        actions [{:kind :cascade-candidate :id :a :precedence [:a]}
                 {:kind :cascade-candidate :id :b :precedence [:b]}]
        entries (mapv (fn [a p]
                        {:action a :controller-score 0
                         :f-prefix (prefix/evaluate-synthetic (assoc (input p) :policy a))})
                      actions [1/4 3/4])
        d (policy/select-action-cascades entries {:beta 2 :cascade-habit-path (str dir "/absent.edn")})
        cs (get-in d [:selection-certificate :candidates])]
    (is (near? 0.25 (get-in d [:selection-law :posterior (first actions)])))
    (is (near? 0.75 (get-in d [:selection-law :posterior (second actions)])))
    (is (= (mapv :f-prefix entries) (mapv :f-prefix cs)))
    (is (= [:computed :computed] (mapv :f-status cs)))
    ;; Production must not upgrade these synthetic receipts to admission.
    (is (= [:not-supplied :not-supplied]
           (mapv #(get-in % [:f-prefix :status]) (prefix/production-ranked entries nil))))
    (println "H4-SYNTHETIC-ARITHMETIC-ONLY" (pr-str {:f (mapv :f cs)
                                                   :posterior (get-in d [:selection-law :posterior])}))
    (Files/delete dir)))
