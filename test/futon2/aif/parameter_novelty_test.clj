(ns futon2.aif.parameter-novelty-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [clojure.java.io :as io]
            [futon2.aif.parameter-novelty :as novelty]
            [futon2.aif.attempt-learning :as attempt]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.cascade-model-manifest :as model]
            [futon2.aif.policy :as policy]
            [futon2.aif.cascade-selection :as selection]
            [futon2.aif.run-narrative :as narrative]
            [futon2.report.novelty-discovery-test :as frozen]))


(use-fixtures :each
  (fn [f]
    (let [files (fn [] (set (map str (filter #(.isFile %) (file-seq (io/file "data"))))))
          before (files)]
      (f)
      (is (= before (files)) "No data/store file is created or removed"))))
(def token [:M :effect])
(def action {:kind :cascade-candidate :id :C1 :target :M
             :precedence [{:id :p :produces #{token}}]})
(def entry {:action action :controller-score 1.0
            :certificate {:horizon 2 :consumed-g {:D {#{} 1}
                          :Q {:steps [{:tau 1 :belief {#{token} 1}} {:tau 2 :belief {#{token} 1}}]}
                          :C {:steps [{:distribution {:weights {}}} {:distribution {:universe #{token} :zeroed #{} :weights {token 1}}}]}}
                          :steps [{:tau 1 :risk 0.5 :ambiguity 0} {:tau 2 :risk 0.5 :ambiguity 0}]}})
(def inputs {:contract (attempt/declared-contract)
             :prior {:schema :wm/learning-trial-prior-v1 :authority :illustrative :mode :record-only :alpha 9 :beta 1}
             :ledger {:status :present :sha256 "fixture-revision" :records []}
             :models {action {:schema :wm/attempt-endpoint-parameter-model-v1 :authority :illustrative
                              :source "hermetic independent endpoint fixture" :route {:author :fixture-author :reviewer :fixture-reviewer}
                              :observation {:schema :wm/perfect-attempt-endpoint-v1 :placement :post-build-artifact-revision}}}})
(defn receipt [x] (novelty/policy-receipt entry x))
(deftest beta-controls
  (doseq [[a b] [[1 1] [9 1] [90 10]]]
    (is (< (abs (- (frozen/novelty [a b]) (:nats (novelty/beta-information a b)))) 1e-9)))
  (is (= (/ 9 10) (/ 90 100)))
  (is (> (:nats (novelty/beta-information 9 1)) (:nats (novelty/beta-information 90 10))))
  (is (= :invalid-beta-prior (:reason (novelty/beta-information 0 1))))
  (is (= 0.0 (get-in (receipt (assoc-in inputs [:models action :prior] {:kind :known-parameter :theta 0.5})) [:expected-kl :nats]))))

(deftest attempt-grain-and-refusals
  (let [r (receipt inputs)]
    (is (= :wm/parameter-novelty-v1 (:schema r)))
    (is (= [token] (:eligible-endpoints r)))
    (is (= 2 (:tau r)))
    (is (= 1 (:multiplicity r)))
    (is (= (:nats (novelty/beta-information 9 1)) (get-in r [:expected-kl :nats])))
    (is (= :no-focus-declared (get-in r [:focus :status])))
    (is (false? (get-in r [:terms :novelty-consumed-in-G?])))
    (let [a (update action :precedence conj {:id :p2 :produces #{token}})
          r2 (novelty/policy-receipt (assoc entry :action a) (assoc inputs :models {a (get-in inputs [:models action])}))]
      (is (= 1 (count (:endpoints r2))))
      (is (= (:expected-kl r) (:expected-kl r2)))))
  (doseq [[x reason] [[(update-in inputs [:models action] dissoc :observation) :observation-model-missing]
                      [(assoc-in inputs [:models action :observation :schema] :unknown) :observation-model-unsupported]
                      [(dissoc inputs :contract) :observation-contract-unsupported]
                      [(assoc inputs :ledger {:status :absent :reason :invalid-ledger}) :endpoint-prior-unavailable]]]
    (is (= reason (get-in (receipt x) [:expected-kl :reason]))))
  (let [t2 [:M :other] a (assoc-in action [:precedence 0 :produces] #{token t2})
        e (-> entry (assoc :action a)
              (assoc-in [:certificate :consumed-g :Q :steps 1 :belief] {#{token t2} 1})
              (assoc-in [:certificate :consumed-g :C :steps 1 :distribution :weights t2] 1)
              (update-in [:certificate :consumed-g :C :steps 1 :distribution :universe] conj t2))
        x (assoc inputs :models {a (get-in inputs [:models action])})]
    (is (= :factorization-unavailable (get-in (novelty/policy-receipt e x) [:expected-kl :reason])))
    (is (= :factorization-unavailable
           (get-in (novelty/policy-receipt e (assoc-in x [:models a :factorization]
                     {:status :correlated :effects [token t2] :source "shared cause"})) [:expected-kl :reason])))
    (is (= (* 2 (:nats (novelty/beta-information 9 1)))
           (get-in (novelty/policy-receipt e (assoc-in x [:models a :factorization]
                      {:status :declared-independent :effects [token t2] :source "fixture-independent-endpoints"}))
                   [:expected-kl :nats])))))

(deftest ledger-snapshot-and-scoped-counts
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory "novelty-" (make-array java.nio.file.attribute.FileAttribute 0)))
        f (io/file dir "attempts.edn")
        family (get-in (receipt inputs) [:endpoints 0 :family])
        row {:schema :wm/attempt-learning-count-v1 :mode :record-only :identity "trial1"
             :family (identity/digest family) :meaning-sha256 "meaning1" :contract (:contract inputs)
             :observed false :increment {:success 0 :failure 1}}]
    (try
      (is (= :ledger-not-found (:reason (novelty/read-ledger (str f)))))
      (is (not (.exists f)))
      (spit f (pr-str row))
      (let [before (slurp f) snapshot (novelty/read-ledger (str f))
            x (-> inputs (assoc :ledger snapshot) (assoc-in [:models action :meanings token] "meaning1"))
            r (receipt x)]
        (is (= :illustrative-plus-learned-counts (get-in r [:endpoints 0 :prior :kind])))
        (is (= 2 (get-in r [:endpoints 0 :prior :beta])))
        (is (= (:sha256 snapshot) (get-in r [:prior-ledger :sha256])))
        (is (= :endpoint-prior-unavailable (get-in (receipt (assoc-in x [:models action :meanings token] "changed")) [:expected-kl :reason])))
        (is (= before (slurp f)))
        (is (= 1 (count (.listFiles dir)))))
      (spit f "{")
      (is (= :unreadable-ledger (:reason (novelty/read-ledger (str f)))))
      (finally (io/delete-file f true) (io/delete-file dir true)))))

(defn frozen-ranked [run]
  (let [f (frozen/prepare (frozen/fixture run))]
    (mapv (fn [row]
            (let [a (get-in row [:candidate :id])
                  cert (model/horizon-g-sparse-cert
                         {:rates (get-in f [:terms :A :value]) :q0 (:q0 f) :horizon 2
                          :spec (:spec f) :universe (get-in f [:terminal :universe])
                          :precedence-fn (constantly (:precedence a))})]
              {:action a :controller-score (:g cert) :certificate (:certificate cert)
               :habit (get-in row [:candidate :habit]) :free-energy 0})) (:rows f))))

(deftest frozen-certificate-and-decision-bytes
  (doseq [run ["1789964661" "1789952479"]]
    (let [ranked (frozen-ranked run)
          habit-file (str (System/getProperty "java.io.tmpdir") "/novelty-habit-" (random-uuid) ".edn")
          baseline (policy/select-action-cascades ranked {:beta 1 :cascade-habit-path habit-file})
          enriched (policy/select-action-cascades ranked {:beta 1 :cascade-habit-path habit-file :novelty-inputs inputs})
          strip #(update % :selection-certificate dissoc :parameter-novelty)]
      (is (not (.exists (io/file habit-file))))
      (let [f (frozen/fixture run) candidates (:candidates f)
            enriched-candidates (mapv (fn [candidate r] (assoc candidate :g (:controller-score r))) candidates ranked)]
        (is (= (pr-str (selection/selection-posterior {:beta 1 :candidates candidates}))
               (pr-str (selection/selection-posterior {:beta 1 :candidates enriched-candidates})))))
      (is (= (pr-str (strip baseline)) (pr-str (strip enriched))))
      (is (= (pr-str (:selection-law baseline)) (pr-str (:selection-law enriched))))
      (is (= (count ranked) (count (get-in enriched [:selection-certificate :parameter-novelty]))))
      (is (= (mapv :controller-score ranked) (mapv :g (get-in enriched [:selection-certificate :candidates]))))
      (is (every? #(= :wm/parameter-novelty-v1 (:schema %)) (get-in enriched [:selection-certificate :parameter-novelty]))))))

(deftest narrative-uses-receipt
  (let [text (narrative/novelty-text {:selection-certificate {:parameter-novelty [(receipt inputs)]}})]
    (is (.contains text "Expected parameter information gain (record-only, not in G):"))
    (is (.contains text "illustrative"))
    (is (.contains text "nats"))))

(deftest theta-latent-accounting-on-frozen-menus
  (let [kappa-controls (atom 0)]
  ;; Independently enumerate the marginal outcomes for the frozen small menus;
  ;; production uses the equivalent additive-log-C formula without a powerset.
  (doseq [run ["1789964661" "1789952479"]
          e (frozen-ranked run)
          [a b] [[1 1] [9 1] [90 10]]]
    (let [effects (:eligible-endpoints (novelty/policy-receipt e inputs))
          m (assoc (get-in inputs [:models action]) :factorization
                   {:status :declared-independent :effects effects :source "sensitivity fixture"})
          x (-> inputs (assoc :models {(:action e) m})
                (assoc-in [:prior :alpha] a) (assoc-in [:prior :beta] b))
          r (novelty/policy-receipt e x)
          terms (get-in r [:terms :theta-latent])
          terminal (last (get-in e [:certificate :consumed-g :Q :steps]))
          member (:distribution (last (get-in e [:certificate :consumed-g :C :steps])))
          q (frozen/endpoint-q (ffirst (:belief terminal)) (set effects) (/ (double a) (+ a b)))
          logc (model/member-log-probability member)
          cost (- (reduce + (for [[state mass] q :when (pos? mass)] (* mass (logc state)))))
          risk (reduce + (for [[state mass] q :when (pos? mass)] (* mass (- (Math/log mass) (logc state)))))
          ;; Integer-prior digamma differences via harmonic numbers are a
          ;; separate computation of the conditional Bernoulli entropy.
          conditional (* (count effects)
                         (- (double (frozen/harmonic (+ a b)))
                            (* (/ a (+ a b)) (double (frozen/harmonic a)))
                            (* (/ b (+ a b)) (double (frozen/harmonic b)))))]
      (is (not (contains? r :shadow-kappa)))
      (is (= :checked (get-in terms [:identity :status])))
      ;; Negative control (claude-5): the kappa-bonus form -- identity-A
      ;; predictive risk, no conditional ambiguity, minus kappa*I -- must fail
      ;; the same check for every kappa > 0, so the check catches double counting.
      (let [info (get-in r [:expected-kl :nats])
            rhs (- cost info)]
        ;; With no eligible endpoint I = 0 and the two forms coincide.
        (when (pos? info)
          (swap! kappa-controls inc)
          (doseq [kappa [0.5 1.0 2.0]]
            (is (= :failed (:status (novelty/identity-check (- risk (* kappa info)) rhs)))
                (str "kappa " kappa))))
        (is (= :checked (:status (novelty/identity-check (+ risk conditional) rhs)))))
      (is (frozen/close? cost (:pragmatic-cost terms)))
      (is (frozen/close? risk (:risk-marginal terms)))
      (is (frozen/close? conditional (:ambiguity-conditional terms)))
      (is (frozen/close? (- cost (get-in r [:expected-kl :nats])) (:efe terms)))
      (is (= 2 (:tau terms)))
      (is (= 1 (:multiplicity terms)))
      (is (= (:controller-score e) (get-in r [:terms :serving-G])))))
    (is (pos? @kappa-controls) "the kappa control must run on some case with I > 0")))

(deftest known-noise-cancels-and-excluded-support-is-held
  (let [x (assoc-in inputs [:models action :prior] {:kind :known-parameter :theta 0.5})
        r (receipt x) terms (get-in r [:terms :theta-latent])]
    (is (= 0.0 (:information-gain terms)))
    (is (frozen/close? (Math/log 2) (:ambiguity-conditional terms)))
    (is (frozen/close? (:pragmatic-cost terms) (:efe terms)))
    (is (= :checked (get-in terms [:identity :status]))))
  (let [e (assoc-in entry [:certificate :consumed-g :C :steps 1 :distribution :zeroed] #{#{}})]
    (is (= :predictive-support-excluded-by-C
           (get-in (novelty/policy-receipt e inputs) [:terms :theta-latent :reason])))))

(deftest theta-latent-receipt-is-required
  (let [r (receipt inputs)]
    (is (= :checked (get-in r [:terms :theta-latent :identity :status])))
    (is (not (contains? r :shadow-kappa)))))
