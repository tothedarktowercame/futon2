(ns futon2.aif.selection-reads-fold-test
  "M-wm-wiring step 8: selection takes E from the enactment fold
  (enactment-habit/fold's cascade-prior state), never from the legacy habit
  store, and records where E came from on the selection law (:e-source).
  E is the Dirichlet-smoothed count (cascade_prior.clj log-priors:
  (count + alpha) / multiplicity / sum over the menu, alpha 1.0)."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-habit-store :as habit]
            [futon2.aif.cascade-prior :as prior]
            [futon2.aif.enactment-habit :as eh]
            [futon2.aif.policy :as policy])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- menu []
  (let [fixture (edn/read-string (slurp (io/resource "fixtures/habit-accumulation/before.edn")))
        [a b] (mapv :action (:ranked (first (:cases fixture))))]
    [a (assoc b :target "M-other")]))

(defn- ranked [[a b]] [{:action a :controller-score 0} {:action b :controller-score 0}])

(defn- enactment-receipts
  "N enactment records of ACTION through enactment-habit/increment, each
  with W_c verdict VERDICT ([] is a pass)."
  [action n verdict]
  (let [key (prior/policy-key (habit/policy-view action))]
    (for [i (range n)]
      (eh/increment {:click (str "click-" i) :candidate (:id action)
                     :attempts (mapv (fn [id] {:pattern id :success true}) (nth key 2))}
                    key verdict))))

(defn- decide [m opts]
  (policy/select-action-cascades (ranked m) (merge {:beta 2} opts)))

(defn- habits [d] (mapv :habit (get-in d [:selection-certificate :candidates])))

(deftest two-passing-enactments-shift-e-toward-their-cascade
  (let [[a b :as m] (menu)
        fold (eh/fold nil (enactment-receipts b 2 []))
        d (decide m {:enactment-fold fold})]
    (is (= {:source :enactment-fold :records 2 :samples 2 :uniform false}
           (get-in d [:selection-law :e-source])))
    ;; alpha 1.0: a (0 + 1) / 4, b (2 + 1) / 4
    (is (every? true? (map #(< (Math/abs (- %1 %2)) 1e-12) [0.25 0.75] (habits d))))
    (is (every? #(= :enactment-fold (get-in % [:habit-provenance :source]))
                (get-in d [:selection-certificate :candidates])))
    (is (some? a))))

(deftest an-empty-fold-is-uniform-and-says-so
  (let [d (decide (menu) {})]
    (is (= {:source :enactment-fold :records 0 :samples 0 :uniform true}
           (get-in d [:selection-law :e-source])))
    (is (apply = (habits d)))))

(deftest a-populated-store-is-not-read
  (let [dir (.toFile (Files/createTempDirectory "fold-test" (make-array FileAttribute 0)))
        path (str (io/file dir "prior.edn"))
        [a :as m] (menu)]
    (try
      (dotimes [_ 5] (habit/record-selection! path {:action a}))
      (is (= 5 (:samples (habit/read-state path))) "the store holds five samples for a")
      (let [with-store (decide m {:cascade-habit-path path})
            fold-only (decide m {})]
        (is (= (habits fold-only) (habits with-store)) "E is the fold's (uniform), not the store's")
        (is (apply = (habits with-store))))
      (finally (doseq [f (reverse (file-seq dir))] (.delete f))))))

(deftest join-unverifiable-verdicts-count-zero
  ;; the bad case: check-c's typed non-verdict passes through increment as
  ;; delta 0 (531cfaaa), so the fold counts nothing and E stays uniform
  (let [[_ b :as m] (menu)
        receipts (enactment-receipts b 2 {:status :join-unverifiable})
        d (decide m {:enactment-fold (eh/fold nil receipts)})]
    (is (every? #(= 0 (:delta %)) receipts))
    (is (every? #(= {:status :join-unverifiable} (:wc-verdict %)) receipts))
    (is (= {:source :enactment-fold :records 0 :samples 0 :uniform true}
           (get-in d [:selection-law :e-source])))
    (is (apply = (habits d)))))

(deftest no-live-caller-passes-the-replay-habit-state
  ;; :habit-state (replay of a recorded run) appears in no source file but
  ;; the selector that accepts it
  (let [files (for [root ["src" "scripts"]
                    f (file-seq (io/file root))
                    :when (and (.isFile f) (str/ends-with? (.getName f) ".clj"))
                    :when (str/includes? (slurp f) ":habit-state")]
                (str f))]
    (is (= ["src/futon2/aif/policy.clj"] (vec files)))))
