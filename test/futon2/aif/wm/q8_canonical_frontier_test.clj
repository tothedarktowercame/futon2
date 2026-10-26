(ns futon2.aif.wm.q8-canonical-frontier-test
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.q8-frontier :as q8]))

;; Live pin: projection of wm-full-loop-machinery-150 attempt-002
;; 002-selection.edn (:selection-as-of "2026-10-24T20:40:24.272212785Z"),
;; values verbatim, only the paths canonical-outer-frontier reads.
(def receipt
  (edn/read-string (slurp "test/resources/q8-canonical-frontier/attempt-002-projection.edn")))

(def ranked ["M-recommendation-bindings" "M-self-improvement-loop"
             "M-stack-geometry" "M-war-machine-wiring"])

(deftest attempt-002-canonical-frontier-is-a-visible-census-failure
  (let [c (q8/canonical-outer-frontier receipt)
        census (:census c)
        menu (get-in receipt [:policy :selected-outer-cascade-policy :sealed-menu])
        entry-targets (set (map :target-id (:entries menu)))
        refusal-target (fn [row]
                         (first (str/split (str (:policy-id row)) #"/" 2)))
        ranked-kinds (->> (concat (:failures menu) (:refusals menu))
                          (filter (fn [row]
                                    (contains? entry-targets
                                               (or (:target-id row)
                                                   (refusal-target row)))))
                          (map :kind)
                          distinct
                          sort
                          vec)]
    (testing "sealed and decided"
      (is (q8/intact? c))
      (is (= :wm/q8-outer-frontier-v1 (:schema c)))
      (is (= :decided-failure (:status c))))
    (testing "census: the S1 open population, verbatim"
      (is (= 786 (count (:open census))))
      (is (= 109 (count (:eligible census))))
      (is (= 677 (count (:excluded census))))
      (is (= {:pipeline/not-on-current-map 526
              :repair-finding/root-missing 102
              :repair-finding/attached-to-root 25
              :repair-finding/root-not-current 13
              :ownership/ambiguous-active-owner 6
              :ownership/actively-held 3
              :mission-standing-conflict 1
              :task-document-source-unavailable 1}
             (frequencies (map :reason (:excluded census))))))
    (testing "entries: one per scored task, tie at the least key"
      (is (= ranked (mapv :task (:entries c))))
      (let [keys (mapv :comparison-key (:entries c))]
        (is (= (keys 0) (keys 3)) "rb and wmw share the least key")
        (is (< (keys 0) (keys 1) (keys 2)) "sil between, stack-geometry greatest")))
    (testing "selection"
      (is (= "M-war-machine-wiring" (:selected c)))
      (is (= {:status :fail
              :reason :tied-minimum
              :tied-members ["M-recommendation-bindings" "M-war-machine-wiring"]
              :absent-terms []}
             (:selection-verdict c))))
    (testing "census verdict: 105 unranked, none of them census exclusions"
      (is (= {:status :fail
              :reason :frontier-not-eligible-census
              :ranked 4
              :unranked 105
              :ranked-kinds ranked-kinds
              :unranked-by-reason {:no-current-target-reading 101
                                   :occurrence-squares-not-authored 1
                                   :target-class-unknown 3}}
             (:census-verdict c)))
      (is (= ranked-kinds (get-in c [:census-verdict :ranked-kinds])))
      (is (not-any? (set ranked-kinds) (map :reason (:unranked c))))
      (is (false? (= (set (map :task (:entries c))) (set (:eligible census)))))
      (is (= 105 (count (:unranked c))))
      (is (= {:task "M-formal-war-machine" :stage :s4
              :reason :target-class-unknown
              :reasons [:seed-not-in-graph :target-class-unknown]}
             (first (filter #(= "M-formal-war-machine" (:task %)) (:unranked c)))))
      (is (= {:task "M-digital-nomad-patterns" :stage :s4
              :reason :target-class-unknown
              :reasons [:isolated-seed :target-class-unknown]}
             (first (filter #(= "M-digital-nomad-patterns" (:task %)) (:unranked c)))))
      (is (= {:task "M-pattern-posteriors" :stage :s4
              :reason :target-class-unknown
              :reasons [:target-class-unknown]}
             (first (filter #(= "M-pattern-posteriors" (:task %)) (:unranked c))))))))

(deftest certificate-carries-the-census-verdict
  (let [outer (q8/canonical-outer-frontier receipt)
        cert (q8/certificate {:outer outer :predecessor nil :decision nil
                              :chosen-target "M-war-machine-wiring" :run-id "r"})]
    (is (q8/intact? cert))
    (is (not (some #{:outer-frontier-absent-or-tampered} (:reasons cert))))
    (is (some #{:no-prior-frontier} (:reasons cert))
        "expected on this route until the cascade packet")
    (is (= (:census-verdict outer) (:census-verdict cert)))
    (is (= 4 (count (get-in cert [:frontier :entries]))))
    (is (= [:no-prior-frontier :policy-stages-absent] (:reasons cert)))))

(deftest silently-unaccounted-eligible-task-is-refused
  (testing "a family row removed from the field leaves its task unaccounted"
    (let [victim "M-daily-scan"
          broken (update-in receipt [:policy-field :families]
                            #(vec (remove (fn [f] (= victim (:target-id f))) %)))
          c (q8/canonical-outer-frontier broken)]
      (is (= :not-recomputable (:status c)))
      (is (= :eligible-task-unaccounted (:reason c)))
      (is (= [victim] (:tasks c)))))
  (testing "a support task with no family, menu row or frontier row"
    (let [orphan "M-phantom"
          grown (-> receipt
                    (update :support conj {:id orphan})
                    (assoc-in [:census-placeholder] true))
          c (q8/canonical-outer-frontier grown)]
      (is (= :not-recomputable (:status c)))
      (is (= :eligible-task-unaccounted (:reason c)))
      (is (= [orphan] (:tasks c))))))

(deftest shared-ranked-kind-cannot-headline-an-unranked-task
  (let [row {:policy-id "R/p" :target-id "R" :status :scored
             :G 0 :cascade-digest "r" :action-pin {:cascade-digest "r"}}
        base {:support [{:id "R"} {:id "U"}]
              :excluded []
              :policy-field {:families [{:target-id "R" :status :computed}
                                        {:target-id "U" :status :computed}]}
              :policy {:selected-outer-cascade-policy
                       {:sealed-menu {:entries [{:policy-id "R/p" :target-id "R"}]
                                      :failures [{:target-id "R" :kind :k-shared}
                                                 {:target-id "U" :kind :k-shared}]
                                      :refusals []}}
                       :canonical-g-frontier
                       {:status :produced :horizon-choice {:horizon 1}
                        :entries [row]}
                       :canonical-policy-selection
                       {:status :selected :action {:target "R"}}}}
        shared (q8/canonical-outer-frontier base)
        own (q8/canonical-outer-frontier
             (update-in base
                        [:policy :selected-outer-cascade-policy :sealed-menu :failures]
                        conj {:target-id "U" :kind :k-own}))]
    (is (= {:task "U" :stage :s4
            :reason :unexplained :reasons [:k-shared]}
           (first (:unranked shared))))
    (is (= {:unexplained 1}
           (get-in shared [:census-verdict :unranked-by-reason])))
    (is (= {:task "U" :stage :s4
            :reason :k-own :reasons [:k-own :k-shared]}
           (first (:unranked own))))))

(defn- focused-receipt
  "The four ranked tasks eligible; everyone else excluded with a
  task-standing reason."
  [g-adjust]
  (let [others (remove #(some #{(str (:id %))} ranked) (:support receipt))
        fr-entries (mapv (fn [e]
                           (if (and (= "M-recommendation-bindings" (:target-id e))
                                    (= :scored (:status e)))
                             (update e :G (fnil + 0.0) g-adjust)
                             e))
                         (get-in receipt [:policy :canonical-g-frontier :entries]))]
    (-> receipt
        (assoc :support (vec (filter #(some #{(str (:id %))} ranked) (:support receipt))))
        (update :excluded into (map (fn [t] {:id (:id t)
                                             :ineligible-reason :pipeline/not-on-current-map})
                                    others))
        (assoc-in [:policy :canonical-g-frontier :entries] fr-entries))))

(deftest census-passes-when-the-frontier-covers-the-eligible-census
  (testing "tie only"
    (let [c (q8/canonical-outer-frontier (focused-receipt 0.0))]
      (is (= :pass (:status (:census-verdict c))))
      (is (= :decided-failure (:status c)))
      (is (= :tied-minimum (get-in c [:selection-verdict :reason])))))
  (testing "nudged: both verdicts pass"
    (let [c (q8/canonical-outer-frontier (focused-receipt 1e-9))]
      (is (= :certified (:status c)))
      (is (= :pass (:status (:census-verdict c))))
      (is (= :pass (:status (:selection-verdict c)))))))

(deftest two-scored-rows-of-one-task-give-one-entry
  (let [row (fn [pid digest g]
              {:policy-id pid :target-id "T" :status :scored
               :G g :cascade-digest digest
               :action-pin {:cascade-digest digest}})
        base {:support [{:id "T"}] :excluded []
              :policy-field {:families [{:target-id "T" :status :computed}]}
              :policy {:selected-outer-cascade-policy
                       {:sealed-menu {:entries [{:policy-id "T/a" :target-id "T"}
                                                {:policy-id "T/b" :target-id "T"}]}}
                       :canonical-g-frontier
                       {:status :produced :horizon-choice {:horizon 1}
                        :entries [(row "T/a" "aaa" -1.5) (row "T/b" "bbb" -2.5)]}
                       :canonical-policy-selection
                       {:status :selected :action {:target "T"}}}}
        c (q8/canonical-outer-frontier base)
        [entry] (:entries c)]
    (is (= 1 (count (:entries c))))
    (is (= "T/b" (:policy-id entry)) "least G wins")
    (is (= "bbb" (:cascade-digest entry)))
    (is (= :pass (:status (:census-verdict c))))
    (let [tied (assoc-in base [:policy :canonical-g-frontier :entries]
                         [(row "T/a" "zzz" -2.5) (row "T/b" "aaa" -2.5)])
          [entry'] (:entries (q8/canonical-outer-frontier tied))]
      (is (= "T/b" (:policy-id entry')) "equal G: digest sorts first"))))

(deftest non-exact-g-is-refused-not-thrown
  (let [row (fn [pid target digest g]
              {:policy-id pid :target-id target :status :scored
               :G g :cascade-digest digest
               :action-pin {:cascade-digest digest}})
        base (fn [g]
               {:support [{:id "T"} {:id "U"}]
                :excluded []
                :policy-field {:families [{:target-id "T" :status :computed}
                                          {:target-id "U" :status :computed}]}
                :policy {:selected-outer-cascade-policy
                         {:sealed-menu {:entries [{:policy-id "T/a" :target-id "T"}
                                                  {:policy-id "U/a" :target-id "U"}]}}
                         :canonical-g-frontier
                         {:status :produced :horizon-choice {:horizon 1}
                          :entries [(row "T/a" "T" "aaa" -1.5)
                                    (row "U/a" "U" "uuu" g)]}
                         :canonical-policy-selection
                         {:status :selected :action {:target "T"}}}})]
    (testing "a string G is refused, not thrown"
      (is (= [:not-recomputable :channel-value-not-exact ["U"]]
             ((juxt :status :reason :tasks)
              (q8/canonical-outer-frontier (base "0.5"))))))
    (testing "a nil G is refused, not thrown"
      (is (= [:not-recomputable :channel-value-not-exact ["U"]]
             ((juxt :status :reason :tasks)
              (q8/canonical-outer-frontier (base nil))))))
    (testing "control: an exact G certifies"
      (let [c (q8/canonical-outer-frontier (base -0.5))]
        (is (= :certified (:status c)))
        (is (= {:status :pass} (:selection-verdict c)))))))

(deftest mixed-g-rows-of-one-task-are-refused-not-thrown
  (let [row (fn [pid digest g]
              {:policy-id pid :target-id "T" :status :scored
               :G g :cascade-digest digest
               :action-pin {:cascade-digest digest}})
        base (fn [rows]
               {:support [{:id "T"}]
                :excluded []
                :policy-field {:families [{:target-id "T" :status :computed}]}
                :policy {:selected-outer-cascade-policy
                         {:sealed-menu {:entries [{:policy-id "T/a" :target-id "T"}
                                                  {:policy-id "T/b" :target-id "T"}]}}
                         :canonical-g-frontier
                         {:status :produced :horizon-choice {:horizon 1}
                          :entries rows}
                         :canonical-policy-selection
                         {:status :selected :action {:target "T"}}}})
        number-row (row "T/a" "aaa" -1.5)
        string-row (row "T/b" "bbb" "0.5")
        nil-row (row "T/b" "bbb" nil)
        result (fn [rows]
                 ((juxt :status :reason :tasks)
                  (q8/canonical-outer-frontier (base rows))))]
    (testing "a string G beside a number is refused, not thrown"
      (is (= [:not-recomputable :channel-value-not-exact ["T"]]
             (result [number-row string-row]))))
    (testing "the order of the rows does not matter"
      (is (= [:not-recomputable :channel-value-not-exact ["T"]]
             (result [string-row number-row]))))
    (testing "control: a nil G beside a number is already refused"
      (is (= [:not-recomputable :channel-value-not-exact ["T"]]
             (result [number-row nil-row]))))))

(deftest tampering-with-a-sealed-g-breaks-the-seal
  (let [c (q8/canonical-outer-frontier receipt)
        tampered (assoc-in c [:entries 0 :channels :G] -1.0)]
    (is (q8/intact? c))
    (is (false? (q8/intact? tampered)))))

(deftest old-route-carrier-still-certificates-without-census-verdict
  (let [old (q8/seal {:schema q8/outer-schema
                      :status :decided-failure
                      :ranking {:law q8/ranking-law :version 1
                                :comparison :complete-union :missing-channel-cost 0}
                      :census {:open ["A"] :eligible ["A"] :excluded []}
                      :selected "A"
                      :selection-verdict {:status :fail :reason :tied-minimum
                                          :tied-members ["A" "A"] :absent-terms []}
                      :entries [{:task "A" :channels {:x 1.0} :observation "o"
                                 :evidence "e" :ranking-law q8/ranking-law
                                 :ranking-version 1 :dependencies []
                                 :comparison-key 1 :absent-terms []}]})
        cert (q8/certificate {:outer old :predecessor nil :decision nil
                              :chosen-target "A" :run-id "r"})]
    (is (not (contains? cert :census-verdict)))))
