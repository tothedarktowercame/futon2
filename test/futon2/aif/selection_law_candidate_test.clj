(ns futon2.aif.selection-law-candidate-test
  "M-wm-wiring row 9: the selection law records the chosen action's candidate
  id (:candidate, the chosen entry's :cascade-id), the id Clause C joins an
  enactment on. Its bad case is a posterior MODE that differs from the chosen
  action: two cascades sharing a first step outweigh, summed, one cascade
  that is the single most probable policy."
  (:require [clojure.edn :as edn]
            [clojure.string]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.policy :as policy])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- step [id target]
  {:id id :target target :guard {:clauses [{:present #{} :absent #{}}]} :produces #{}})

(defn- ranked [cascade-id target g & steps]
  {:action {:kind :cascade-candidate :id cascade-id :target target
            :precedence (vec steps)
            :construction-receipt {:kind :fixture :id cascade-id}
            :interpretation-receipts {cascade-id {:kind :fixture}}}
   :cascade true
   :cascade-id cascade-id
   :controller-score g})

;; :cas/a1 and :cas/a2 share the first step :p/a; :cas/b alone starts with
;; :p/b and has the lowest G, so it is the single most probable policy, while
;; :p/a wins the summed marginal
(def roster
  [(ranked :cas/b "M-t" 0.5 (step :p/b "M-t"))
   (ranked :cas/a1 "M-t" 1.0 (step :p/a "M-t") (step :p/c "M-t"))
   (ranked :cas/a2 "M-t" 1.0 (step :p/a "M-t") (step :p/d "M-t"))])

(defn- decide [ranked-actions]
  (let [root (.toFile (Files/createTempDirectory "row9-" (make-array FileAttribute 0)))]
    (try (policy/select-action-cascades
          ranked-actions {:beta 1 :novelty-inputs {} :cascade-habit-path (str (io/file root "absent.edn"))})
         (finally (doseq [f (reverse (file-seq root))] (io/delete-file f true))))))

(deftest the-candidate-is-the-chosen-action-not-the-posterior-mode
  (let [d (decide roster)
        law (:selection-law d)]
    (is (= :cas/b (get-in law [:per-policy-argmax :action :id])) "the mode is :cas/b")
    (is (= (step :p/a "M-t") (:chosen-action d)) "the marginal chose the :p/a step")
    (is (#{:cas/a1 :cas/a2} (:candidate law)) "the recorded candidate is a :p/a cascade")
    (is (= (:candidate law) (get-in d [:action :id])) "and it is the decision's own action")
    (is (not= (:candidate law) (get-in law [:per-policy-argmax :action :id])))))

(deftest every-other-selection-law-key-is-unchanged
  ;; fixture: pr-str of this decision from policy.clj at futon2 54e3c396,
  ;; before row 9, captured before the change. It froze :enacted-steps as
  ;; nils ({:p/b nil ...}: the roster carries no prediction belief), which
  ;; enacted-step-of now records as {:absent :no-scoring-belief}; so both
  ;; sides project :enacted-steps (and the output its new :candidate) away,
  ;; both read back through EDN so the comparison is of the same printing.
  ;; The fixture bytes are untouched; the typed values are pinned below and
  ;; in enacted-step-test.
  (let [d (decide roster)
        ;; :e-source (step 8, the enactment fold) joined after the capture too
        proj (fn [m] (pr-str (update m :selection-law dissoc :candidate :enacted-steps :e-source)))]
    ;; step 8's named drop: the habit provenance names the enactment fold
    (is (= (proj (edn/read-string (clojure.string/replace
                                   (edn/read-string (slurp "test/fixtures/selection-law/row9-before@futon2-54e3c396.edn"))
                                   ":source :cascade-prior" ":source :enactment-fold")))
           (proj (edn/read-string (pr-str d)))))
    (is (every? #(= {:absent :no-scoring-belief} %) (vals (get-in d [:selection-law :enacted-steps]))))))

(deftest no-cascade-id-leaves-the-key-out
  ;; an entry without :cascade-id (the ticket-queue fixtures' shape): no
  ;; :candidate key, never a nil, so the frozen decision bytes hold and the
  ;; W_c checker reads the missing id as :join-unverifiable
  (let [d (decide (mapv #(dissoc % :cascade-id) roster))]
    (is (not (contains? (:selection-law d) :candidate)))))
