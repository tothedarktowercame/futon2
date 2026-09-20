(ns futon2.aif.fold-cascade
  "Realize declared cascades using production observations, never predicted facts.
  False establishes a negative guard only for C3/C4 (C4 also requires its file).
  C6 false cannot establish absence. Receipts warrant interpretations, not truth
  of their outputs. Unknown guards retain holes; scoring remains fold-eval's."
  (:require [clojure.java.io :as io]
            [futon2.aif.fold-eval :as evaluation]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.observation-checks :as observation]))

(defn- source-witness [receipt]
  (let [{:keys [path sha256]} (:source receipt)]
    (when (and (string? path) (string? sha256) (re-matches #"[0-9a-f]{64}" sha256))
      (try
        (let [file (io/file observation/repo-root path)
              actual (evidence/sha256 (java.nio.file.Files/readAllBytes (.toPath file)))]
          (when (= sha256 actual) {:path path :sha256 actual}))
        (catch java.io.IOException _ nil)))))

(defn- requirements [guard]
  (when (and (= :interpreted (:status guard)) (= :and (:operator guard))
             (vector? (:clauses guard)) (seq (:clauses guard))
             (every? #(and (= :interpreted (:status %))
                           (set? (:present %)) (set? (:absent %)))
                     (:clauses guard)))
    (vec (mapcat (fn [clause]
                   (concat (map #(vector % true) (sort-by pr-str (:present clause)))
                           (map #(vector % false) (sort-by pr-str (:absent clause)))))
                 (:clauses guard)))))

(defn- condition [observations [token expected]]
  (let [result (or (get-in observations [:results token])
                   (get-in observations [:refused token]))
        observed (:observed result)
        pin (get-in result [:evidence :resolved-sha])
        negative-check? (or (= :C3 (:check result))
                            (and (= :C4 (:check result))
                                 (true? (get-in result [:evidence :file-present]))))
        established? (and (nil? (:status result))
                          (string? pin) (re-matches #"[0-9a-f]{40}" pin)
                          (boolean? observed) (= expected observed)
                          (or expected negative-check?))]
    {:condition (pr-str {:token token :expected expected})
     :status (if established? :established :unchecked)
     :witness {:token token :expected expected
               :observation (or result {:status :missing :kind :observation-unavailable})}}))

(defn realize
  "Production fold for a selected cascade. Locators are declaration data carried
  with the selected action. Each call reads them through observation/observe.
  Guard failures and source failures remain explicit remainders."
  [construction]
  (let [patterns (:precedence construction)
        target (:mission construction)
        receipts (:interpretation-receipts construction)
        counts (frequencies (map :id patterns))
        locators (get-in construction [:selected-action :observation-locators])
        observations (observation/observe (or locators {}))
        evaluated
        (mapv
         (fn [p]
           (let [id (:id p) receipt (get receipts id)
                 source (source-witness receipt)
                 reqs (requirements (:guard p))
                 conditions (mapv #(condition observations %) reqs)
                 outputs (:produces p)
                 grounded? (and (= 1 (counts id))
                                (string? target) (seq target) (= target (:target p))
                                (map? (:construction-receipt construction))
                                (string? (:reading receipt)) (seq (:reading receipt))
                                (set? outputs) (seq outputs)
                                (every? #(and (vector? %) (= 2 (count %))
                                              (= target (first %)) (keyword? (second %))) outputs))
                 transition? (and (= :interpreted (get-in p [:transition :status]))
                                  (= :union (get-in p [:transition :operator]))
                                  (= outputs (get-in p [:transition :produces])))
                 ready? (and grounded? source transition? (seq conditions)
                             (every? #(= :established (:status %)) conditions))]
             (if ready?
               {:box {:id (str id) :fits-pattern {:pattern/id (str id)
                                                 :pattern/revision (:sha256 source)}
                      :warrant-kind :pattern :interpretation-receipt receipt
                      :source-witness source :conditions conditions
                      :produces outputs :transition (:transition p)}}
               {:hole {:unfolded-pattern (str id)
                       :guard-evidence conditions
                       :construction-blockers
                       (cond-> []
                         (not grounded?) (conj :ungrounded-interpretation)
                         (nil? source) (conj :interpretation-source-unverified)
                         (not transition?) (conj :transition-unverified)
                         (not (seq conditions)) (conj :no-observable-guard)
                         (some #(not= :established (:status %)) conditions)
                         (conj :guard-not-established))}})))
         patterns)
        boxes (vec (keep :box evaluated))
        holes (vec (keep :hole evaluated))
        ;; An edge records shared declared output/input, with the actual input
        ;; observation attached. It claims neither execution nor causal discharge.
        wires (vec (for [a boxes b boxes :when (not= (:id a) (:id b))
                         c (:conditions b)
                         :let [w (:witness c)]
                         :when (and (true? (:expected w))
                                    (contains? (:produces a) (:token w)))]
                     {:from (:id a) :to (:id b) :type :wire/declared-dependency
                      :token (:token w) :witness w}))
        wiring {:boxes boxes :wires wires :policy-holes holes
                :generated-by "futon2.aif.fold-cascade/realize"}]
    {:fold/route :cascade-interpretation
     :fold/selection-reason :interpretation-receipts-present
     :observation-evidence observations
     :wiring wiring :policy-holes holes
     :coverage-score-delta (evaluation/coverage-score-delta wiring)}))

(defn evaluate
  "Evaluate after obligation expansion, so retained output obligations are the
  holes counted by the unchanged evaluator and carried in the wiring."
  [result]
  (let [wiring (assoc (:wiring result) :policy-holes (:policy-holes result))]
    (assoc result :wiring wiring
           :coverage-score-delta (evaluation/coverage-score-delta wiring))))
