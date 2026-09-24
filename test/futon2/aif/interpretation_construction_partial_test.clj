(ns futon2.aif.interpretation-construction-partial-test
  "D15 (holes/E-cascade-real.md): the interpretation constructor is no longer
  all-or-nothing over wants. A plan is admissible when it newly produces at
  least one want; what it leaves is named, typed apart, on the candidate's
  :construction-receipt as :unreached-wants [{:token ... :reason ...}]:
  :no-producer when no interpretation produces the token (the seat honestly
  declined the task), :beyond-horizon when producers exist but the model
  cannot reach the token within the declared horizon. The empty plan never
  constructs, and full-want plans precede partial ones in :candidates."
  (:require [clojure.edn :as edn]
            [clojure.set :as set]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.interpretation-construction :as sut]))

;; ---------------------------------------------------------------------
;; Fixture shaped like the existing full-want pin (P -> q, Q -> w).

(def interpretations
  {:P {:guard {:needs #{} :forbids #{}} :produces #{:q}}
   :Q {:guard {:needs #{:q} :forbids #{}} :produces #{:w}}
   :R {:guard {:needs #{} :forbids #{}} :produces #{:r}}})
(def receipts (zipmap (keys interpretations) (repeat {:kind :fixture-interpretation :by "test"})))

(defn- g-empty-worst [c] (if (empty? (:precedence c)) 1.0 0.0))

(def base-input
  {:target "M-construction" :want [:w] :observation {:q false :w false :r false}
   :interpretations interpretations :interpretation-receipts receipts
   :budget {:max-moves 1 :max-expansions 20} :horizon 2
   :move-cost 0 :evaluate-g g-empty-worst})

;; ---------------------------------------------------------------------
;; receipt-complete?: every unreached, unproduced want is NAMED.

(defn receipt-complete?
  "A candidate's receipt is complete when every want the plan does not
  produce is named in the receipt's :unreached-wants. Omitting one fails."
  [interpretations candidate]
  (let [produced (reduce set/union #{} (map #(get-in interpretations [% :produces])
                                            (:precedence candidate)))
        named (set (map :token (get-in candidate [:construction-receipt :unreached-wants])))]
    (every? #(or (contains? produced %) (contains? named %)) (:want candidate))))

;; ---------------------------------------------------------------------
;; (3) Pre-change behaviour on a full-want target is unchanged.

(deftest full-want-target-is-unchanged
  (let [r (sut/construct base-input)]
    (is (= :constructed (:status r)))
    (is (= [[:P :Q]] (mapv :precedence (:candidates r))))
    (is (= [:w] (:want (first (:candidates r)))))
    (is (= [] (get-in (first (:candidates r)) [:construction-receipt :unreached-wants])))))

;; ---------------------------------------------------------------------
;; (1) The P1 probe pin: seat A's four refused targets now construct.

(def probe-dir "holes/labs/wm-contract/E-cascade-real/probe-interp/seat-A/")

;; Seat A's declined tasks (NOTES.md, "Not covered" lines): each refused
;; target covered 4-5 of 6 wants and was refused for the honest decline.
(def probe-expectations
  {"M-action-cost-modelling"   #{:hole/h6e84a77745cb}
   "M-canon-fingerprint-store" #{:hole/h61f4c17570aa :hole/h3b2303097926}
   "M-chipwitz-corps"          #{:hole/h056f5583d59e}
   "M-kangaroo"                #{:hole/hf27981fd598b :hole/he91ecffec2e7}})

(defn- probe-input [file]
  (let [d (edn/read-string (slurp (str probe-dir file ".edn")))]
    {:target (:target d) :want (:want d)
     :observation (zipmap (:facts d) (repeat false))
     :interpretations (:patterns d)
     :interpretation-receipts (:interpretation-receipts d)
     ;; The probe's own smoke parameters (seat-A NOTES): horizon 8 so the
     ;; honest chains are reachable; the pin is about :no-producer only.
     :budget {:max-moves 10 :max-expansions 2000} :horizon 8
     :move-cost 0 :evaluate-g g-empty-worst}))

(deftest p1-probe-refused-targets-construct-with-typed-unreached-wants
  (doseq [[file declined] probe-expectations]
    (testing file
      (let [input (probe-input file)
            r (sut/construct input)]
        (is (= :constructed (:status r)))
        (is (seq (:candidates r)))
        (is (some #(= :unproduced-need (:kind %)) (:findings r)))
        (doseq [c (:candidates r)]
          (testing "scoring sees the full want set"
            (is (= (vec (:want input)) (:want c))))
          (testing "every unreached want is the declined task, typed :no-producer"
            (is (= declined (set (map :token (get-in c [:construction-receipt :unreached-wants])))))
            (is (every? #(= :no-producer (:reason %))
                        (get-in c [:construction-receipt :unreached-wants]))))
          (testing "the declined want stays in the candidate's want set"
            (is (set/subset? declined (set (:want c)))))
          (is (receipt-complete? (:interpretations input) c)))))))

;; ---------------------------------------------------------------------
;; (2) Falsifiers.

(deftest zero-want-plans-still-refuse-and-the-empty-plan-never-constructs
  (let [input {:target "M-empty" :want [:nope]
               :observation {:nope false :other false}
               :interpretations {:A {:guard {:needs #{} :forbids #{}} :produces #{:other}}}
               :interpretation-receipts {:A {:kind :fixture-interpretation :by "test"}}
               :budget {:max-moves 1 :max-expansions 20} :horizon 2
               :move-cost 0 :evaluate-g g-empty-worst}
        r (sut/construct input)]
    (is (= :refused (:status r)))
    (is (= :no-supported-order (:kind r)))
    (is (= [] (:candidates r)))
    (is (some #(= :unproduced-need (:kind %)) (:findings r)))))

(deftest receipt-completeness-is-checkable
  (let [input (probe-input "M-action-cost-modelling")
        r (sut/construct input)
        c (first (:candidates r))]
    (is (= :constructed (:status r)))
    (is (receipt-complete? (:interpretations input) c))
    (testing "a receipt that omits a want the plan did not produce fails"
      (let [dishonest (update-in c [:construction-receipt :unreached-wants]
                                 (fn [u] (vec (rest u))))]
        (is (seq (get-in c [:construction-receipt :unreached-wants])))
        (is (not (receipt-complete? (:interpretations input) dishonest)))))))

(deftest horizon-miss-is-beyond-horizon-never-no-producer
  ;; A 6-step honest chain (all producers exist) for :far, plus a fast
  ;; producer for :near, at horizon 2: :far is a horizon miss.
  (let [chain (into {:Fast {:guard {:needs #{} :forbids #{}} :produces #{:near}}}
                    (map (fn [i]
                           [(keyword (str "F" i))
                            {:guard {:needs (if (= 1 i) #{} #{(keyword (str "t" (dec i)))})
                                     :forbids #{}}
                             :produces #{(if (= 6 i) :far (keyword (str "t" i)))}}]))
                    (range 1 7))
        input {:target "M-horizon" :want [:near :far]
               :observation {:near false :far false :t1 false :t2 false :t3 false :t4 false :t5 false}
               :interpretations chain
               :interpretation-receipts (zipmap (keys chain) (repeat {:kind :fixture-interpretation :by "test"}))
               :budget {:max-moves 10 :max-expansions 200} :horizon 2
               :move-cost 0 :evaluate-g g-empty-worst}
        r (sut/construct input)]
    (is (= :constructed (:status r)))
    (is (seq (:candidates r)))
    (doseq [c (:candidates r)]
      (is (= [:far] (mapv :token (get-in c [:construction-receipt :unreached-wants]))))
      (is (= [:beyond-horizon] (mapv :reason (get-in c [:construction-receipt :unreached-wants]))))
      (is (not-any? #(= :no-producer (:reason %))
                    (get-in c [:construction-receipt :unreached-wants]))))))

(deftest full-want-plans-come-first
  ;; :far has two producers: :Quick (needs nothing) and a 6-step chain.
  ;; Plans through :Quick reach every want; chain plans miss :far at
  ;; horizon 2. Full-want plans must precede partial ones.
  (let [chain (into {:Fast {:guard {:needs #{} :forbids #{}} :produces #{:near}}
                     :Quick {:guard {:needs #{} :forbids #{}} :produces #{:far}}}
                    (map (fn [i]
                           [(keyword (str "F" i))
                            {:guard {:needs (if (= 1 i) #{} #{(keyword (str "t" (dec i)))})
                                     :forbids #{}}
                             :produces #{(if (= 6 i) :far (keyword (str "t" i)))}}]))
                    (range 1 7))
        input {:target "M-order" :want [:near :far]
               :observation {:near false :far false :t1 false :t2 false :t3 false :t4 false :t5 false}
               :interpretations chain
               :interpretation-receipts (zipmap (keys chain) (repeat {:kind :fixture-interpretation :by "test"}))
               :budget {:max-moves 10 :max-expansions 400} :horizon 2
               :move-cost 0 :evaluate-g g-empty-worst}
        r (sut/construct input)
        unreached (mapv #(get-in % [:construction-receipt :unreached-wants]) (:candidates r))]
    (is (= :constructed (:status r)))
    (is (empty? (first unreached)))
    (is (some #(= [:beyond-horizon] (mapv :reason %)) (rest unreached)))
    (is (every? (fn [[a b]] (or (empty? a) (seq b))) (partition 2 1 unreached)))))
