;; Produce Q10 run-fact exports through the real runner (the Q8 route's
;; clicks) and the Lean terms wm_check_run_facts generates from them: the
;; first click compares one same-pattern control (Q10 holds); the repeat,
;; where Q6 excluded one member, and a reading over p/a, p/c compare none
;; (Q10 violated); a compared member without its own G (violated); the
;; census missing, tampered or over a hidden refusal (not recomputable).
;; Run from the futon2 worktree; Q10_LEAN_OUT names the output directory.
(require 'futon2.aif.wm.q10-arrangement-census-route-test 'clojure.string
         'futon2.aif.load-identity)
(in-ns 'futon2.aif.wm.q10-arrangement-census-route-test)
(defn emit [out label record]
  (let [export (json/parse-string (json/generate-string
                                   (facts/facts-for-record record "r" {})))
        path (str out "/Q10" label ".lean")]
    (spit path (lean-term/lean-source export (str "q10" label)))
    (println label (:status (q10/verify-record record)) (get-in record [:q10 :counts]) path)))
(def out (or (System/getenv "Q10_LEAN_OUT") "test/fixtures/q10-lean"))
(def worktree (str (System/getProperty "user.dir") "/"))
(with-redefs [futon2.aif.load-identity/required-sources
              (into {} (map (fn [[n p]] [n (clojure.string/replace p "/home/joe/code/futon2/" worktree)]))
                    futon2.aif.load-identity/required-sources)]
  (futon2.aif.hermetic-repair-fixture/with-hermetic-stores
   (fn []
     (futon2.test-support.runner-fixture/with-hermetic-traces
      (fn []
        (binding [futon2.aif.full-loop-runner/*wm-status-reporting?* false]
          (route/with-empty-learning-ledger
           (fn []
             (let [[one two] (two-clicks)
                   [disjoint] (two-clicks ["p/a" "p/c"])
                   sel (get-in one [:q10 :selected])
                   other (first (remove #{sel} (map :key (get-in one [:q10 :population]))))
                   drop-g (fn [rows] (mapv #(if (= other (prefix-admission/candidate-key (:id %)))
                                              (dissoc % :g) %)
                                           rows))]
               (emit out "First" one)
               (emit out "Repeat" two)
               (emit out "DifferentSets" disjoint)
               (emit out "WithoutOwnG"
                     (reseal one #(update-in % [:decision :selection-certificate :candidates] drop-g)))
               (emit out "Missing" (dissoc one :q10))
               (emit out "Tampered" (assoc-in one [:q10 :counts :distinguished-by-g] 2))
               (emit out "HiddenRefusal"
                     (reseal one (fn [r]
                                   (-> r
                                       (update-in (stages-path r :scored) (partial filterv #(not= other %)))
                                       (update-in [:decision :selection-law :posterior] dissoc
                                                  (some #(when (= other (prefix-admission/candidate-key %)) %)
                                                        (keys (get-in r [:decision :selection-law :posterior])))))))))))))))))
(shutdown-agents)
