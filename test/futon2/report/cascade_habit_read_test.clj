(ns futon2.report.cascade-habit-read-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.cascade-habit-store :as habit]
            [futon2.aif.cascade-prior :as prior]
            [futon2.aif.enactment-habit :as eh]
            [futon2.aif.cascade-selection :as selection]
            [futon2.aif.efe :as efe]
            [futon2.aif.policy :as policy])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn with-store [f]
  (let [dir (.toFile (Files/createTempDirectory "habit-read-" (make-array FileAttribute 0)))]
    (try (f (str (io/file dir "prior.edn")))
         (finally (doseq [file (reverse (file-seq dir))] (.delete file))))))

;; M-wm-wiring step 8 (claude-10, 2026-09-25): selection takes E from the
;; enactment fold, not the legacy store. The learned mass these tests pin now
;; comes from three counted enactment records in the fold (enactment-habit/
;; fold), the same counts record-selection! used to write; a populated store
;; with an empty fold moves nothing.
(defn fold-for
  "A fold with N counted (W_c-passing) enactment records for ACTION's policy."
  [action n]
  (eh/fold nil (for [i (range n)]
                 {:record-id [:habit-read-test i] :delta 1
                  :policy-key (prior/policy-key (habit/policy-view action))})))

(defn menu []
  (let [fixture (edn/read-string (slurp (io/resource "fixtures/habit-accumulation/before.edn")))]
    (mapv :action (:ranked (first (:cases fixture))))))

(defn evidence [path]
  (let [[a b] (menu)
        ;; Different missions are ordinary in the joint policy space.
        b (assoc b :target "M-other")
        ranked [{:action a :controller-score 0 :f 0.2}
                {:action b :controller-score 0.2 :f 0.7}]
        neutral (selection/selection-posterior
                 {:beta 2 :candidates (mapv #(hash-map :id (:action %) :habit 1
                                                      :f (:f %) :g (:controller-score %)) ranked)})]
    (dotimes [_ 3] (habit/record-selection! path {:action b}))
    (let [decision (policy/select-action-cascades ranked {:beta 2 :cascade-habit-path path
                                                          :enactment-fold (fold-for b 3)})
          store-only (policy/select-action-cascades ranked {:beta 2 :cascade-habit-path path})
          candidates (get-in decision [:selection-certificate :candidates])
          posterior (get-in decision [:selection-law :posterior])]
      {:counts (mapv #(get-in % [:habit-provenance :count]) candidates)
       :habits (mapv :habit candidates)
       :habit-statuses (mapv :habit-status candidates)
       :neutral-posterior (mapv neutral [a b])
       :learned-posterior (mapv posterior [a b])
       :neutral-argmax (:id (key (apply max-key val neutral)))
       :learned-argmax (:id (key (apply max-key val posterior)))
       :selected (:id (:action decision))
       :E (mapv #(get-in % [:terms :E])
                (get-in decision [:selection-certificate :g-term-decomposition :policies]))
       :e-source (get-in decision [:selection-law :e-source])
       :store-only-posterior (mapv (get-in store-only [:selection-law :posterior]) [a b])
       :store-only-e-source (get-in store-only [:selection-law :e-source])})))

(deftest learned-mass-moves-the-posterior-and-choice
  (with-store
    (fn [path]
      (let [r (evidence path)]
        (is (= [0 3] (:counts r)))
        (is (= [:attached :attached] (:habit-statuses r)))
        (is (not= (first (:habits r)) (second (:habits r))))
        (is (every? true? (map #(< (Math/abs (- %1 %2)) 1e-12) [0.2 0.8] (:habits r))))
        (is (not= (:neutral-posterior r) (:learned-posterior r)))
        (is (= [:empty :work :work] ((juxt :neutral-argmax :learned-argmax :selected) r)))
        (is (every? #(= {:verdict :non-degenerate :reason :informative-habit}
                        (select-keys % [:verdict :reason])) (:E r)))
        (is (= {:source :enactment-fold :records 3 :samples 3 :uniform false} (:e-source r)))
        (testing "the legacy store, populated, with an empty fold: not read"
          (is (= {:source :enactment-fold :records 0 :samples 0 :uniform true} (:store-only-e-source r)))
          (is (= (:neutral-posterior r) (:store-only-posterior r))))
        (println "HABIT-READ-EVIDENCE" (pr-str r))))))

(deftest scorer-output-is-attached-by-the-selector
  (with-store
    (fn [path]
      (let [actions (menu)
            _ (dotimes [_ 3] (habit/record-selection! path {:action (second actions)}))
            fold (fold-for (second actions) 3)
            ranked (efe/rank-cascade-actions {:cascade-belief {#{} 1}}
                                             actions
                                             {:horizon-steps 1
                                              :cascade-spec {:want #{:route-a-rehearsal-reported}}})
            result (policy/select-action-cascades ranked {:beta 2 :cascade-habit-path path
                                                          :enactment-fold fold})
            cs (get-in result [:selection-certificate :candidates])]
        (is (= 2 (count cs)))
        (is (every? #(= :attached (:habit-status %)) cs))
        (is (= #{0 3} (set (map #(get-in % [:habit-provenance :count]) cs))))
        (is (= 2 (count (set (map :habit cs)))))
        (is (every? #(pos? (:habit %)) cs))))))

(deftest missing-identity-records-whole-menu-fallback
  (with-store
    (fn [path]
      (let [;; Keep an explicit acting action: empty/no-action candidates are
      ;; refused by policy before this habit-fallback assertion can run.
      ranked [{:action {:id :bad :type :test/acting} :controller-score 0}]
            result (policy/select-action-cascades ranked {:beta 2 :cascade-habit-path path})
            c (first (get-in result [:selection-certificate :candidates]))]
        (is (= 1 (:habit c)))
        (is (= {:source :neutral-fallback :reason :missing-policy-identity :scope :whole-menu}
               (:habit-provenance c)))))))
