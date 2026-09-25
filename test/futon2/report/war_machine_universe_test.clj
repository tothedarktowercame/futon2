(ns futon2.report.war-machine-universe-test
  "H-VALUE-G-D (PROOF-2 register row AR-40): the constructor's :evaluate-g
  scores every candidate of one problem over ONE token universe — the
  problem's declared universe (cascade-problems/problem-tokens), not the
  candidate's own family. Before the fix the universe grew with the
  candidate, so a plan's G and the empty baseline's G were taken over
  different universes and differed by T·k·ln2 for the k tokens only the
  plan names; over the AR-40 fixture the cross-universe pair was
  15.3434 vs 10.7982 (chain declined). In one universe the comparable pair
  is 15.3434 vs 16.3434 and the chain is preferred by exactly λ = 1.

  Fixture: the D16 4-step chain of war-machine-horizon-test (the AR-40
  case), scored by the lane's real G (wm/constructed-candidate-g). The
  values below are pinned from the post-fix run of this fixture, not from
  the packet."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.efe :as efe]
            [futon2.aif.interpretation-construction :as ic]
            [futon2.report.war-machine :as wm])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def target "M-chain")

(def patterns
  {:chain/one {:guard {:needs #{:s0} :forbids #{:t1}} :produces #{:t1}}
   :chain/two {:guard {:needs #{:t1} :forbids #{:t2}} :produces #{:t2}}
   :chain/three {:guard {:needs #{:t2} :forbids #{:t3}} :produces #{:t3}}
   :chain/four {:guard {:needs #{:t3} :forbids #{:t4}} :produces #{:t4}}})

(def chain [:chain/one :chain/two :chain/three :chain/four])
(def prefix3 [:chain/one :chain/two :chain/three])
(def tokens [:s0 :t1 :t2 :t3 :t4])

;; measured on this fixture after the fix (T=4, λ=1, universe of 5 tokens):
;; G = T·lnZ − Σ u(S_τ); lnZ = ln(1+e) + 4·ln2 over this universe.
(def g-baseline 16.343401639032017)
(def g-chain 15.343401639032017)

(defn- empty-store []
  (.getCanonicalPath (.toFile (Files/createTempDirectory "universe-store" (make-array FileAttribute 0)))))

(defn- sources [& [extra]]
  (merge
   {:universes {target {:s0 true :t1 false :t2 false :t3 false :t4 false}}
    :wants {target [:t4]}
    :horizon-steps 4
    :locators {target (into {} (for [t tokens]
                                 [t {:class :C4 :repo "futon2" :sha "HEAD" :path "x.md" :decl (str "- [x] " (name t))}]))}
    :interpretations {target {:patterns patterns
                              :receipts (into {} (for [k (keys patterns)] [k {:source :test}]))}}
    :beta-by-context {:WM {:beta 1}} :context-of (constantly :WM)
    :construction {:construct ic/construct :budget {:max-moves 4 :max-expansions 20000}
                   :move-cost 0 :evaluate-g wm/constructed-candidate-g}}
   extra))

(defn- problem
  "The assembled AR-40 problem, without precedences, as the constructor's
  :evaluate-g sees it. DECLARED orders a candidate in the sources (with the
  :construction-receipt admission requires) so a plan the constructor would
  not build still has a problem to be scored on; G never reads the receipt."
  [declared]
  (let [srcs (assoc (sources) :candidates
                    {target [{:precedence declared
                              :construction-receipt {:kind :declared-for-universe-test}}]})
        r (wm/assemble-cascade-problems-with-published (empty-store) {:targets [target] :sources srcs})]
    (some-> (first (:problems r)) :cascade-problem (dissoc :precedences))))

(deftest one-universe-g-pair-and-the-chain-is-taken
  ;; bad case (a): with :universe supplied, G(empty) and G(chain) over the
  ;; AR-40 fixture are 16.3434 and 15.3434 and the chain is TAKEN.
  (let [p (problem chain)
        g-empty (:value (wm/constructed-candidate-g p {:precedence []}))
        g-chain (:value (wm/constructed-candidate-g p {:precedence chain}))]
    (is (= g-baseline g-empty))
    (is (= g-chain (:value (wm/constructed-candidate-g p {:precedence chain}))))
    (is (< g-chain g-empty) [g-chain g-empty])
    (testing "the constructor TAKES the chain at the declared move cost 0"
      (let [r (wm/assemble-cascade-problems-with-published
               (empty-store) {:targets [target] :sources (sources)})
            c (first (mapcat :constructed-candidates (:problems r)))]
        (is (empty? (:refusals r)) (pr-str (:refusals r)))
        (is (= chain (:precedence c)))
        (is (= 1.0 (get-in c [:construction-receipt :moves 0 :parts :pragmatic])))))))

(deftest absent-universe-is-the-family-universe-no-op
  ;; a98f5879 edit 1's claim: with :universe ABSENT, efe/rank-cascade-actions
  ;; computes the family universe (q0's support ∪ :want ∪ every token any
  ;; candidate names) exactly as before the fix. So scoring with no
  ;; :universe must be identical to scoring with that family universe
  ;; supplied explicitly; a WRONG explicit :universe must change the result
  ;; (G shifts by T·k·ln2 for the k extra tokens).
  (let [patt {:id :p/one :produces #{:t1}
              :guard {:status :interpreted
                      :clauses [{:status :interpreted
                                 :present #{:s0} :absent #{}}]}}
        cands [{:kind :cascade-candidate :id :c0 :precedence []}
               {:kind :cascade-candidate :id :c1 :precedence [patt]}]
        state {:cascade-belief {#{:s0} 1}}
        base {:horizon-steps 2 :cascade-spec {:want #{:t1}}}
        family-universe #{:s0 :t1}
        absent (efe/rank-cascade-actions state cands base)]
    (is (= absent (efe/rank-cascade-actions state cands (assoc base :universe family-universe)))
        ":universe absent is a no-op: identical to the explicit family universe")
    (is (not= absent (efe/rank-cascade-actions state cands (assoc base :universe (conj family-universe :t2))))
        "a wrong explicit :universe shifts G — the arms differ")))

(deftest want-never-reached-scores-the-baseline-and-is-declined
  ;; bad case (b): a 3-step prefix that never reaches the want earns no
  ;; credit (u(S_τ) = 0 at every step), so its G equals the empty cascade's
  ;; exactly — improvement 0 — and the constructor's stop rule (best move's
  ;; value <= 0 → :acting-worth-more, construction.clj) declines it at any
  ;; move cost >= 0.
  (let [p (problem prefix3)
        g-empty (:value (wm/constructed-candidate-g p {:precedence []}))
        g-prefix (:value (wm/constructed-candidate-g p {:precedence prefix3}))]
    (is (= g-baseline g-empty))
    (is (= g-baseline g-prefix))
    (is (zero? (- g-empty g-prefix)) [g-empty g-prefix])
    (testing "DECLINED: with only the prefix's patterns admitted the machine
             constructs nothing and does not act"
      (let [srcs (-> (sources)
                     (assoc-in [:interpretations target :patterns]
                               (dissoc patterns :chain/four)))
            r (wm/assemble-cascade-problems-with-published
               (empty-store) {:targets [target] :sources srcs})]
        (is (empty? (mapcat :constructed-candidates (:problems r))))
        (is (= :no-constructed-candidate (:kind (first (:refusals r))))
            (pr-str (:refusals r)))))))

(deftest the-receipt-records-the-universe-the-scorer-used
  ;; constructed-candidate-g returns {:value G :universe U} with U the
  ;; problem's own tokens (cascade-problems/problem-tokens), so the
  ;; construction receipt records the universe G was taken over. Bad case:
  ;; a fact no pattern names (:x-idle) is in the scorer's universe but not in
  ;; the token set interpretation-construction would declare for a bare G
  ;; (want ∪ pattern tokens); a bare-number G would record that narrower set.
  (let [extra (fn [srcs] (-> srcs
                             (assoc-in [:universes target :x-idle] false)
                             (assoc-in [:locators target :x-idle]
                                       {:class :C4 :repo "futon2" :sha "HEAD" :path "x.md" :decl "- [x] x-idle"})))
        p (let [srcs (extra (assoc (sources) :candidates
                                   {target [{:precedence chain
                                             :construction-receipt {:kind :declared-for-universe-test}}]}))]
            (-> (wm/assemble-cascade-problems-with-published (empty-store) {:targets [target] :sources srcs})
                :problems first :cascade-problem (dissoc :precedences)))
        scored (wm/constructed-candidate-g p {:precedence chain})
        scorer-universe (conj tokens :x-idle)]
    (is (= (set scorer-universe) (set (:universe scored))))
    (is (= (:universe scored) (vec (sort-by pr-str (:universe scored)))) "recorded sorted")
    (is (number? (:value scored)))
    (let [r (wm/assemble-cascade-problems-with-published (empty-store) {:targets [target] :sources (extra (sources))})
          c (first (mapcat :constructed-candidates (:problems r)))]
      (is (= chain (:precedence c)) (pr-str (:refusals r)))
      (is (= (:universe scored) (get-in c [:construction-receipt :g-of-best :universe]))))))
