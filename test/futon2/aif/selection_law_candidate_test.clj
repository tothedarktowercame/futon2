(ns futon2.aif.selection-law-candidate-test
  "M-wm-wiring row 9: the selection law records the chosen action's candidate
  id (:candidate, the chosen entry's :cascade-id), the id Clause C joins an
  enactment on. Its bad case is a posterior MODE that differs from the chosen
  action: two cascades sharing a first step outweigh, summed, one cascade
  that is the single most probable policy."
  (:require [clojure.string]
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

(deftest the-candidate-is-the-minimum-class-representative
  (let [d (decide roster)
        law (:selection-law d)]
    (is (= :strict-G-minimum-over-prediction-classes (:law law)))
    (is (= [:action-marginal :tie-break-rule] (:retired law)))
    (is (= :cas/b (:candidate law) (get-in d [:action :id])) "the smallest G is enacted")
    (is (= (step :p/b "M-t") (:chosen-action d)))
    (is (= [:cas/b] (:class-members law)))
    (is (= :cas/b (:enacted law)))
    (is (= :first-in-ranked-order-within-one-prediction-class (:representative-rule law)))
    (is (= (:candidate law) (get-in law [:per-policy-argmax :action :id]))
        "with E and F flat the posterior's mode is the same policy")
    (is (not (contains? law :action-marginal)))
    (is (not (contains? law :action-comparison)))
    (is (not (contains? law :tie-break-rule)))))

(deftest g-decides-where-e-would-not
  ;; The posterior's mode can differ from the minimum once E is not flat. The
  ;; law takes the minimum: the ruling is stated on G.
  (let [d (decide (mapv (fn [entry h] (assoc entry :habit h)) roster [1 1000 1]))]
    (is (= :cas/b (get-in d [:action :id])))))

(deftest no-cascade-id-leaves-the-key-out
  ;; an entry without :cascade-id (the ticket-queue fixtures' shape): no
  ;; :candidate key, never a nil, so the frozen decision bytes hold and the
  ;; W_c checker reads the missing id as :join-unverifiable
  (let [d (decide (mapv #(dissoc % :cascade-id) roster))]
    (is (not (contains? (:selection-law d) :candidate)))))
