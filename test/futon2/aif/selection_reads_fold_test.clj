(ns futon2.aif.selection-reads-fold-test
  "M-wm-wiring step 8: selection takes E from the enactment fold
  (enactment-habit/fold's cascade-prior state), never from the legacy habit
  store, and records where E came from on the selection law (:e-source).
  E is the Dirichlet-smoothed count (cascade_prior.clj log-priors:
  (count + alpha) / multiplicity / sum over the menu, alpha 1.0)."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-habit-store :as habit]
            [futon2.aif.cascade-prior :as prior]
            [futon2.aif.enactment-habit :as eh]
            [futon2.aif.grain-gate :as gate]
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

(def checker-path
  "../futon3c/holes/labs/M-futon-seams/exemplar/proof2a_check.clj")

(defn checker-verdict
  "Run the real W_c executable on temporary carriers; missing bb/script fails."
  [record enactment]
  (assert (.isFile (io/file checker-path)) (str "Missing checker: " checker-path))
  (let [dir (.toFile (Files/createTempDirectory "fold-real-checker" (make-array FileAttribute 0)))
        r (io/file dir "click.edn") e (io/file dir "enactment.edn")]
    (try
      (spit r (pr-str record))
      (spit e (pr-str enactment))
      (let [{:keys [exit out err]}
            (try (sh/sh "bb" checker-path (str r) (str e) "--wc" "--edn")
                 (catch java.io.IOException ex
                   (throw (ex-info (str "Cannot execute bb " checker-path) {} ex))))]
        (assert (zero? exit) (str "Checker failed: " checker-path " " err))
        (edn/read-string out))
      (finally (doseq [f (reverse (file-seq dir))] (.delete f))))))

(defn- pinned-exemplar [name sha]
  (let [path (io/file (.getParent (io/file checker-path)) name)
        bytes (Files/readAllBytes (.toPath path))
        actual (apply str (map #(format "%02x" (bit-and 0xff %))
                              (.digest (java.security.MessageDigest/getInstance "SHA-256") bytes)))]
    (assert (= sha actual) (str "Exemplar changed: " path))
    (edn/read-string (String. bytes java.nio.charset.StandardCharsets/UTF_8))))

(deftest real-checker-verdict-into-increment
  (let [record (pinned-exemplar "click-001.edn" "98aa1cba12cdd1c759474d54447e89de38bcea58aa00fe6cd7cb392776a41409")
        enactment (pinned-exemplar "click-001-enactment.edn" "e51063896e2a42096718d902e0b4dfe0e4652323de0b42848c2c0cf318bf6c89")
        cid (:candidate enactment)
        derivation (get-in record [:decision :selection-certificate :candidate-derivations cid])
        grain (:grain enactment)
        gate-result (gate/grain-gate {:grain grain} {:grain grain} "../futon3c")
        good (update enactment :attempts
                     (fn [attempts]
                       (mapv #(if (and (= 3 (:n %))
                                       (= :cascade-construction/choose-the-grain-where-state-lives (:pattern %)))
                                (assoc % :check {:kind :grain-gate :repo "futon3c"
                                                :attempt-grain grain :result gate-result}) %) attempts)))
        action {:kind :cascade-candidate :id cid :target (:target derivation)
                :construction-receipt (:construction-receipt derivation)
                :precedence (mapv #(hash-map :id %) (distinct (map :pattern (:attempts good))))}]
    (is (= :pass (:status gate-result)))
    (doseq [[selected delta] [[cid 1] [:cand/b-observe-first 0]]]
      (let [verdict (checker-verdict (assoc-in record [:decision :selection-law :candidate] selected) good)
            receipts (vec (enactment-receipts action 1 verdict))
            folded (eh/fold nil receipts)]
        (is (vector? verdict))
        (if (= 1 delta)
          (is (= [] verdict))
          (is (some #(str/includes? % "differs from the click's selected candidate") verdict)))
        (is (= delta (:delta (first receipts))))
        (is (= delta (count (:enactment-records folded))))))))
