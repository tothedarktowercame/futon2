(ns futon2.aif.focus-receipt-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.focus-receipt :as focus]
            [futon2.aif.token-observation-initialization-test :as serving-fixture]
            [futon2.aif.run-narrative :as narrative]
            [futon2.aif.scoring-input-receipts :as receipts])
  (:import [java.util.zip GZIPInputStream]))

(defn frozen [run]
  (with-open [in (GZIPInputStream. (io/input-stream (str "test/fixtures/focus/" run ".edn.gz")))]
    (edn/read-string (slurp in))))
(def inputs (focus/read-inputs))
(def context {:as-of "2026-09-21T18:00:00Z"})

(deftest pinned-dates-and-evidence-boundaries
  (let [yesterday (focus/discover inputs "2026-09-20T23:59:59Z" nil)
        today (focus/discover inputs (:as-of context) nil)]
    (is (= ["WM" "WM"] (mapv :focus [yesterday today])))
    (is (= [165 166] (mapv :commit-count [yesterday today])))
    ;; 158 rose from 157 with the resources/wm/ facet fix (PROOF-wm-works 1.3,
    ;; 2026-09-22): one more commit under resources/wm/ in the frozen corpus
    ;; (22 such paths exist) now correctly credits WM.
    (is (= [151 158] (mapv #(get-in % [:facet-credit "WM"]) [yesterday today])))
    (is (= [] (get-in yesterday [:facet-graph :background])))
    (is (= ["APM"] (get-in today [:facet-graph :background])))
    (is (= ["WM"] (get-in (focus/discover inputs (:as-of context) {:focus "APM"}) [:facet-graph :background])))
    (is (= [] (get-in (focus/discover inputs "2026-09-21T04:24:00Z" nil) [:facet-graph :background])))
    (is (= :unknown (:status (focus/discover inputs "2026-09-23T00:00:00Z" nil))))
    (is (= "APM" (:focus (focus/discover inputs (:as-of context) {:focus "APM" :receipt "prior"}))))))

(deftest frozen-decisions-are-byte-identical-apart-from-new-receipt
  (doseq [run ["1789964661" "1789952479"]]
    (let [record (frozen run) d (:decision record)
          result (focus/attach d inputs context)
          receipt (get-in result [:selection-certificate :focus-receipt])]
      (is (= d (update result :selection-certificate dissoc :focus-receipt)))
      ;; Hash-map bucket order may change when adding/removing a key; every
      ;; pre-existing field's bytes, including the score and law maps, survive.
      (doseq [[k v] (:selection-certificate d)]
        (is (= (pr-str v) (pr-str (get-in result [:selection-certificate k])))))
      (is (= (pr-str (:selection-law d)) (pr-str (:selection-law result))))
      (is (= (pr-str (get-in d [:selection-certificate :candidates]))
             (pr-str (get-in result [:selection-certificate :candidates]))))
      (is (= (pr-str (:g-term-decomposition d)) (pr-str (:g-term-decomposition result))))
      (is (focus/valid? result (edn/read-string (pr-str receipt))))
      (is (= :held (get-in receipt [:outcome-domain :unrepresented-class-mass :status])))
      (is (= {:focus 0.55 :associated 0.35 :useful-elsewhere 0.05 :known-failure 0.05}
             (get-in receipt [:global-preference :masses])))
      (is (= :absent (get-in receipt [:kernel :status])))
      (doseq [bad [(assoc-in receipt [:candidates 0 :class] :irrelevant)
                   (assoc-in receipt [:global-preference :masses :focus] 1)
                   (assoc-in receipt [:discovery :focus] "EOI")]]
        (is (not (focus/valid? result bad)))
        (is (some #{:focus-receipt-mismatch}
                  (:errors (receipts/validate-record
                            (assoc record :decision (assoc-in result [:selection-certificate :focus-receipt] bad))))))))))

(deftest absent-relations-and-unchanged-predictions-do-not-create-outcomes
  (let [d (:decision (frozen "1789952479"))
        result (focus/attach d inputs context)
        rows (get-in result [:selection-certificate :focus-receipt :candidates])
        f2 (first (filter #(= "M-wm-08-external-f2" (:target %)) rows))
        eoi (first (filter #(= "M-expressions-of-interest" (:target %)) rows))]
    (is (= :focus (:class f2)))
    (is (= :absent (get-in f2 [:embedding :status])))
    (is (= :attested-outcome-not-inferred-from-prediction (get-in f2 [:outcome :reason])))
    (is (= :unknown (:class eoi)))
    (is (= :relation-not-declared (get-in eoi [:relation :reason])))
    (is (not-any? #(= :known-failure (:class %)) rows))
    (is (re-find #"Discovered focus: WM" (narrative/focus-text result)))
    (is (re-find #"M-expressions-of-interest/.* = unknown" (narrative/focus-text result)))))

(deftest receipt-is-attached-by-the-real-serving-selection
  (serving-fixture/with-two-ticks
   (fn [{:keys [first second]}]
     (doseq [d [first second]]
       (let [receipt (get-in d [:selection-certificate :focus-receipt])]
         (is (= :wm/focus-receipt-v1 (:schema receipt)))
         (is (= :record-only (:mode receipt)))
         (is (focus/valid? d receipt)))))))

(deftest non-string-targets-and-unavailable-inputs-stay-unknown
  (let [d {:selection-certificate {:candidates [{:id {:target :fixture :id :C1}}]}}
        a (focus/build d inputs context)]
    (is (= :unknown (get-in a [:candidates 0 :class])))
    (is (= :embedding-node-not-retained (get-in a [:candidates 0 :embedding :reason])))
    (is (= :unknown (get-in (focus/build d {:status :absent} context) [:discovery :status])))))

;; PROOF-wm-works 1.3 (2026-09-22): resources/wm/ paths are WM work. The
;; private facets fn is exercised directly (var-resolved) because the public
;; discover reads the frozen commit-facets corpus, which predates these paths.
(deftest resources-wm-paths-facet-as-wm
  (let [facets @#'focus/facets]
    (is (= #{"WM"} (facets ["resources/wm/rechecks/repair-occ-444fb018-dated-recheck.edn"])))
    (is (= #{"WM"} (facets ["resources/wm/eig/held-out-split.edn"])))
    (is (= #{"WM"} (facets ["resources/wm/cascade-sources/T-repair-occ-444fb018.edn"])))
    ;; near-misses: a longer segment, wm elsewhere in the name, and a
    ;; non-resources wm path must NOT facet as WM.
    (is (= #{"other/unattributed"} (facets ["resources/wmx/thing.edn"])))
    (is (= #{"other/unattributed"} (facets ["src/swarm/model.clj"])))
    (is (= #{"other/unattributed"} (facets ["resources/wm2/x.edn"])))
    (is (= #{"other/unattributed"} (facets ["docs/wm-notes.md"])))
    ;; (resources/war_machine-ish legitimately matches the pre-existing
    ;; war_machine prefix rule — trailing anchor was never required there —
    ;; so it is not a near-miss; resources/wm2 is.)
    ;; existing behaviour unchanged:
    (is (= #{"WM"} (facets ["src/futon2/aif/policy.clj"])))
    (is (= #{"WM"} (facets ["holes/labs/wm-contract/PROOF-wm-works.md"])))))
