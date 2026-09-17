(ns futon2.aif.construction-moves-test
  "H7c-2 tests: the four cascade-construction library moves as move
  functions. Pure, deterministic: no substrate, no registry, no network."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.construction :as construction]
            [futon2.aif.construction-moves :as cm]))

(def pa {:id :p/a :guard {:needs #{} :forbids #{}} :produces #{:b}})
(def pb {:id :p/b :guard {:needs #{} :forbids #{}} :produces #{:w}})

(def family1
  [{:id :c1 :precedence [:p/a] :patterns [pa]}])

(def all-read
  {:requirement {:read? true :locus "SPEC.md"}
   :implementation {:read? true :locus "src"}
   :live-state {:read? true :locus "journal"}
   :prior-attempts {:read? true :locus "tickets"}})

;; --------------------------------------------------------- read-what-exists
(deftest read-what-exists-happy-path
  (testing "one new candidate: the first candidate extended with the unused
            available patterns, in the order given"
    (let [move (cm/read-what-exists
                {:sources all-read
                 :available-patterns [pa pb {:id :p/c :guard {:needs #{} :forbids #{}} :produces #{}}]
                 :cost 1})
          result (move family1)]
      (is (= :read-what-exists (:move-id result)))
      (is (= 2 (count (:proposed-family result))))
      (is (= (first family1) (first (:proposed-family result))))
      (is (= {:id :c1 :precedence [:p/a :p/b :p/c]
              :patterns [pa pb {:id :p/c :guard {:needs #{} :forbids #{}} :produces #{}}]}
             (second (:proposed-family result))))
      (is (= [:requirement :implementation :live-state :prior-attempts]
             (:sources-read result)))
      (is (= [] (:sources-unavailable result)))
      (is (= {:kind :state-information :value 4
              :basis :read-what-exists-first}
             (:epistemic-estimate result)))
      (is (= 1 (:cost result))))))

(deftest read-what-exists-unavailable-counts-as-supplied
  (testing "a source recorded unavailable is supplied (the @done line) and
            counted in the recorded-sources value"
    (let [move (cm/read-what-exists
                {:sources (assoc all-read :live-state {:unavailable :no-journal})
                 :available-patterns [pb]
                 :cost 1})
          result (move family1)]
      (is (= :read-what-exists (:move-id result)))
      (is (= [:live-state] (:sources-unavailable result)))
      (is (= 4 (get-in result [:epistemic-estimate :value]))))))

(deftest read-what-exists-no-move-reasons
  (testing ":nothing-unread when every available pattern is already used;
            :sources-not-supplied when a source key is missing"
    (let [nothing (cm/read-what-exists
                   {:sources all-read :available-patterns [pa]})
          missing (cm/read-what-exists
                   {:sources (dissoc all-read :prior-attempts)
                    :available-patterns [pb]})]
      (is (= {:status :no-move :move-id :read-what-exists
              :reason :nothing-unread}
             (nothing family1)))
      (is (= :sources-not-supplied (:reason (missing family1))))
      (is (= [:prior-attempts] (get-in (missing family1) [:detail :missing]))))))

;; -------------------------------------------------------- borrow-a-sibling
(deftest borrow-a-sibling-happy-path
  (testing "the first uncovered row with a counterpart is appended to the
            first candidate; the covered row is not re-walked"
    (let [move (cm/borrow-a-sibling
                {:sibling {:id :sib :rows [{:row :observe}
                                           {:row :act :pattern-id :p/a}]}
                 :counterpart (fn [row _fam] (when (= :observe row) pa))
                 :row-of (fn [p] ({:p/a :act :p/b :observe} (:id p)))
                 :cost 1})
          result (move family1)]
      (is (= :borrow-a-sibling (:move-id result)))
      (is (= 2 (count (:proposed-family result))))
      (is (= [:p/a :p/a] (get-in result [:proposed-family 1 :precedence])))
      (is (= [pa pa] (get-in result [:proposed-family 1 :patterns])))
      (is (= :observe (:row-covered result)))
      (is (= [] (:gaps result)))
      (is (= {:kind :state-information :value 0
              :basis :borrow-a-sibling-cascade}
             (:epistemic-estimate result))))))

(deftest borrow-a-sibling-gap-recorded-not-filled
  (testing "a row with no counterpart is recorded as a gap and the walk
            moves on in the same call; a gap is never filled with an
            invented pattern"
    (let [move (cm/borrow-a-sibling
                {:sibling {:id :sib :rows [{:row :route} {:row :observe}]}
                 :counterpart (fn [row _fam] (when (= :observe row) pb))
                 :row-of (fn [_p] nil)
                 :cost 1})
          result (move family1)]
      (is (= :borrow-a-sibling (:move-id result)))
      (is (= [:route] (:gaps result)))
      ;; the counterpart for :observe is still proposed, not the gap row
      (is (= :observe (:row-covered result)))
      (is (= pb (get-in result [:proposed-family 1 :patterns 1]))))))

(deftest borrow-a-sibling-rows-walked
  (testing "every row covered or a gap gives the finite-walk :no-move"
    (let [all-covered (cm/borrow-a-sibling
                       {:sibling {:id :sib :rows [{:row :act}]}
                        :counterpart (fn [_r _f] pa)
                        :row-of (fn [p] ({:p/a :act} (:id p)))
                        :cost 1})
          all-gaps (cm/borrow-a-sibling
                    {:sibling {:id :sib :rows [{:row :observe} {:row :route}]}
                     :counterpart (fn [_r _f] nil)
                     :row-of (fn [_p] nil)
                     :cost 1})]
      (is (= {:status :no-move :move-id :borrow-a-sibling
              :reason :sibling-rows-walked :gaps []}
             (all-covered family1)))
      (is (= [:observe :route] (:gaps (all-gaps family1))))
      (is (= :sibling-rows-walked (:reason (all-gaps family1)))))))

;; ----------------------------------------------------------- order-by-need
(def obs {:id :p/obs :guard {:needs #{} :forbids #{}} :produces #{:b}})
(def act {:id :p/act :guard {:needs #{:b} :forbids #{}} :produces #{:w}})
(def indep1 {:id :p/i1 :guard {:needs #{} :forbids #{}} :produces #{:x}})
(def indep2 {:id :p/i2 :guard {:needs #{} :forbids #{}} :produces #{:y}})
(def needs-z {:id :p/z :guard {:needs #{:z} :forbids #{}} :produces #{}})
(def loopy {:id :p/loop1 :guard {:needs #{:q} :forbids #{}} :produces #{:r}})
(def loopy2 {:id :p/loop2 :guard {:needs #{:r} :forbids #{}} :produces #{:q}})

(deftest order-by-need-happy-path
  (testing "a pattern needing t comes after the pattern producing t"
    (let [move (cm/order-by-need {:cost 0})
          family [{:id :c1 :precedence [:p/act :p/obs]
                   :patterns [act obs]}]
          result (move family)]
      (is (= :order-by-need (:move-id result)))
      (is (= [{:id :c1 :precedence [:p/obs :p/act]
               :patterns [act obs]}]
             (:proposed-family result)))
      (is (= 0 (:cost result))))))

(deftest order-by-need-stable-for-independent-patterns
  (testing "independent patterns keep their written order; with nothing to
            reorder the move is :already-ordered"
    (let [move (cm/order-by-need {:cost 0})
          family [{:id :c1 :precedence [:p/i2 :p/i1]
                   :patterns [indep2 indep1]}]
          result (move family)]
      (is (= {:status :no-move :move-id :order-by-need
              :reason :already-ordered :unmet-needs [] :cycles []}
             result)
          "no invented order between independent patterns")
      (is (= {:status :no-move :move-id :order-by-need
              :reason :already-ordered :unmet-needs [] :cycles []}
             (move [{:id :c2 :precedence [:p/obs :p/act]
                     :patterns [obs act]}]))))))

(deftest order-by-need-unmet-need-recorded-not-reordered-around
  (testing "a need produced by no pattern is an unmet need: recorded, and
            the dependent pattern is not reordered around it"
    (let [move (cm/order-by-need {:cost 0})
          family [{:id :c1 :precedence [:p/z :p/act :p/obs]
                   :patterns [needs-z act obs]}]
          result (move family)]
      (is (= [{:candidate :c1 :needs [:z]}] (:unmet-needs result)))
      (is (= [:p/z :p/obs :p/act]
             (get-in result [:proposed-family 0 :precedence]))
          "p/z's unmet need gives it no dependency, so its written place is kept")
      (is (= 1 (get-in result [:epistemic-estimate :value]))))))

(deftest order-by-need-cycle-left-unchanged
  (testing "a dependency cycle leaves that candidate's order unchanged and
            is recorded under :cycles"
    (let [move (cm/order-by-need {:cost 0})
          family [{:id :c1 :precedence [:p/loop1 :p/loop2]
                   :patterns [loopy loopy2]}]
          result (move family)]
      ;; unchanged order: the no-move still records the cycle as a finding
      (is (= :already-ordered (:reason result)))
      (is (= [{:candidate :c1 :precedence [:p/loop1 :p/loop2]}]
             (:cycles result)))
      (is (nil? (:proposed-family result))))))

;; ------------------------------------------------------------ add-a-check
(def p-open {:id :p/open :guard {:needs #{:u} :forbids #{}} :produces #{:b}})
(def p-push {:id :p/push :guard {:needs #{:b} :forbids #{}} :produces #{:w}})

(deftest add-a-check-happy-path
  (testing "the first gating unknown fact becomes a check at the FRONT of
            every candidate's precedence, one check per move"
    (let [move (cm/add-a-check
                {:facts {:u :unknown :other :unknown}
                 :want [:w]
                 :check-theta 0.9
                 :cost 1})
          family [{:id :c1 :precedence [:p/open :p/push]
                   :patterns [p-open p-push]}
                  {:id :c2 :precedence [:p/open]
                   :patterns [p-open]}]
          result (move family)
          check (:check result)]
      (is (= :add-a-check (:move-id result)))
      (is (= :check/u (:id check)))
      (is (= :u (:fact check)))
      (is (= [:check/u :p/open :p/push]
             (get-in result [:proposed-family 0 :precedence])))
      (is (= [:check/u :p/open]
             (get-in result [:proposed-family 1 :precedence])))
      (is (= check (first (get-in result [:proposed-family 0 :patterns]))))
      (is (= [:other] (:not-gating result)))
      (is (= {:kind :state-information :value 0.9
              :basis :check-candidates}
             (:epistemic-estimate result))))))

(deftest add-a-check-no-move-reasons
  (testing "already-present checks, typed refusal with the law, and missing
            inputs"
    (let [present (cm/add-a-check
                   {:facts {:u :unknown} :want [:w] :check-theta 0.9})
          refused (cm/add-a-check
                   {:facts {:u :unknown} :want [:w] :check-theta 1.5})
          no-inputs (cm/add-a-check {:want [:w] :check-theta 0.9})
          family [{:id :c1 :precedence [:check/u :p/open]
                   :patterns [{:id :check/u :kind :check :fact :u
                               :guard {:unknown #{:u}} :opens :u
                               :produces #{} :theta 0.9}
                              p-open]}]]
      (is (= {:status :no-move :move-id :add-a-check
              :reason :no-unknown-gating-fact}
             (present family)))
      (is (= :check-candidates-refused (:reason (refused family))))
      (is (= :invalid-check-theta (:detail (refused family)))
          "an out-of-range theta is refused by check-candidates with its typed law")

      (is (= :inputs-not-supplied (:reason (no-inputs family)))))))

;; -------------------------------------------------- termination (H7c-2)
(defn- unmet-need-g
  "Stub G for the termination test: a pure fn of the candidate — fewer
  unmet guard needs (needs produced by no pattern in the candidate) is
  lower G."
  [c]
  (double
   (count
    (let [pats (:patterns c)
          produced (into #{} (mapcat :produces) pats)]
      (remove #(contains? produced %)
              (mapcat #(-> % :guard :needs) pats))))))

(deftest construction-terminates-by-the-moves-themselves
  (testing "construct with all four library moves injected stops with a
            stop reason from its list and budget-used below the cap — the
            moves stop themselves (each move's @done), not the budget"
    (let [obs-x {:id :p/obs :guard {:needs #{:gate/x} :forbids #{}} :produces #{:b}}
          act-w {:id :p/act :guard {:needs #{:b} :forbids #{}} :produces #{:w}}
          base {:id :c1 :precedence [:p/act :p/obs] :patterns [act-w obs-x]}
          moves [(cm/read-what-exists
                  {:sources all-read
                   :available-patterns
                   [{:id :p/base :guard {:needs #{} :forbids #{}}
                     :produces #{:gate/x}}]
                   :cost 0.5})
                 (cm/borrow-a-sibling
                  {:sibling {:id :sib :rows [{:row :observe} {:row :act}]}
                   :counterpart (fn [_r _f] nil)
                   :row-of (fn [p] ({:p/obs :observe :p/act :act} (:id p)))
                   :cost 1})
                 (cm/order-by-need {:cost 0})
                 (cm/add-a-check
                  {:facts {:gate/x :unknown} :want [:w] :check-theta 0.9})]
          {:keys [family receipt]}
          (construction/construct
           {:target :mission/test
            :initial-family [base]
            :moves moves
            :evaluate-g unmet-need-g
            :budget {:max-moves 8}
            :horizon 2})]
      (is (contains? #{:acting-worth-more :no-admitted-move
                       :needs-routed-human-input :want-already-observed}
                     (:stop-reason receipt)))
      (is (< (:budget-used receipt) 8))
      (is (pos? (:budget-used receipt)) "at least one move was taken")
      (is (zero? (unmet-need-g (apply min-key unmet-need-g family)))
          "the best final candidate has no unmet needs"))))

;; claude-4's review, 2026-09-17: a candidate that does NOT carry its patterns
;; is the shape cascade-problems assembles from a declared source (patterns
;; live in the target's interpretation). order-by-need read its empty pattern
;; list as "no patterns to order" and returned an EMPTY precedence, which
;; would have replaced a real cascade with the empty one.
(deftest order-by-need-leaves-a-candidate-that-does-not-carry-its-patterns
  (let [move (cm/order-by-need {:cost 0})
        candidate {:precedence [:p/a :p/b]
                   :construction-receipt {:kind :hand-admitted}}
        r (move [candidate])]
    (is (= :no-move (:status r)))
    (is (= :already-ordered (:reason r)))
    (is (= [[:p/a :p/b]] (:patterns-not-carried r)))))

;; ------------------------------------------- WM-07-delivery's epistemic consumer

(def fake-delivery
  {:schema :wm/parameter-delivery-v1
   :expected-information-gain {"advance-twice" 0.6931471805599453
                              "no-gain" {:status :refused :kind :zero-evidence-conditioning}}})

(deftest delivered-parameter-information-gain-is-the-move-s-epistemic-value
  (let [move (cm/with-parameter-information-gain
               (cm/read-what-exists {:sources all-read
                                     :available-patterns [pb]
                                     :cost 1})
               {:delivery fake-delivery
                :policy-id-of (constantly "advance-twice")})
        r (move family1)]
    (is (= :parameter-information-gain (get-in r [:epistemic-estimate :kind])))
    (is (= 0.6931471805599453 (get-in r [:epistemic-estimate :value])))
    (is (= :wm/parameter-delivery-v1
           (get-in r [:epistemic-estimate :basis :delivery-schema])))))

(deftest a-refused-or-missing-gain-is-not-a-gain-of-zero
  (doseq [policy ["no-gain" "never-delivered"]]
    (let [move (cm/with-parameter-information-gain
                 (cm/read-what-exists {:sources all-read
                                       :available-patterns [pb] :cost 1})
                 {:delivery fake-delivery :policy-id-of (constantly policy)})
          r (move family1)]
      (is (= :state-information (get-in r [:epistemic-estimate :kind]))
          "the move keeps its own estimate")
      (is (some? (:parameter-information-gain-refused r))))))

(deftest construct-adds-the-delivered-gain-and-marks-nothing-unformalised
  (let [move (cm/with-parameter-information-gain
               (cm/read-what-exists {:sources all-read
                                     :available-patterns [pb] :cost 0.5})
               {:delivery fake-delivery
                :policy-id-of (constantly "advance-twice")})
        {:keys [receipt]} (construction/construct
                           {:target :mission/eig
                            :initial-family family1
                            :moves [move]
                            ;; G indifferent: the gain alone decides
                            :evaluate-g (constantly 1.0)
                            :budget {:max-moves 2}
                            :horizon 2})
        taken (first (:moves receipt))]
    (is (= :read-what-exists (:move-id taken)))
    (is (= 0.6931471805599453 (get-in taken [:parts :epistemic-added])))
    (is (false? (get-in taken [:parts :includes-unformalised-novelty]))
        "the Lean expectedInformationGain is not an unformalised estimate")
    (is (< 0 (:value taken)) "gain 0.69 against cost 0.5 and no ΔG")))

;; claude-4's review, 2026-09-17: a move that cannot move still FINDS things —
;; order-by-need's unmet needs and cycles, borrow-a-sibling's gaps. construct
;; kept only the no-move REASON, so those findings died with the move's return
;; value, and construction could stop :no-admitted-move without anyone learning
;; which needs were unmet. An unmet need is exactly what a check is made from.
(deftest a-move-that-cannot-move-still-reports-what-it-found
  (let [cyclic-x {:id :p/x :guard {:needs #{:y} :forbids #{}} :produces #{:x}}
        cyclic-y {:id :p/y :guard {:needs #{:x} :forbids #{}} :produces #{:y}}
        {:keys [receipt]}
        (construction/construct
         {:target :mission/cycle
          :initial-family [{:id :c :precedence [:p/x :p/y]
                            :patterns [cyclic-x cyclic-y]}]
          :moves [(cm/order-by-need {:cost 0})]
          :evaluate-g (constantly 1.0)
          :budget {:max-moves 3}
          :horizon 2})]
    (is (= :no-admitted-move (:stop-reason receipt)))
    (is (= [{:candidate :c :precedence [:p/x :p/y]}]
           (get-in receipt [:no-move-findings :order-by-need :cycles]))
        "the cycle reaches the receipt")))
