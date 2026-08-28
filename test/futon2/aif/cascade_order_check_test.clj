(ns futon2.aif.cascade-order-check-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.cascade-order-check :as sut]))

(deftest both-producer-shapes-use-from-to-convention
  (let [pairs (sut/check-cascade-order
               {:descent [["a" "b"] ["b" "a"]]})
        maps (sut/check-cascade-order
              [{:from "a" :to "b"} {:from "b" :to "a"}])]
    (is (= :failed (get-in pairs [:acyclic-descent :result])))
    (is (= ["a" "b" "a"] (get-in pairs [:acyclic-descent :cycle])))
    (is (= (:acyclic-descent pairs) (:acyclic-descent maps)))
    (is (= :unwitnessable (get-in pairs [:has-meets :result])))))

(deftest order-and-meet-axes-remain-independent
  (let [disconnected (sut/check-cascade-order
                      {:elements ["lower" "upper" "isolated" "isolated-top"]
                       :descent [["lower" "upper"]
                                 ["isolated" "isolated-top"]]})
        chain (sut/check-cascade-order
               {:elements ["a" "b"] :descent [["a" "b"]]})]
    (is (= :passed (get-in disconnected [:acyclic-descent :result])))
    (is (= :failed (get-in disconnected [:has-meets :result])))
    (is (= :passed (get-in chain [:acyclic-descent :result])))
    (is (= :passed (get-in chain [:has-meets :result])))))

(defn- cascade-with-descent [record]
  (some #(when (and (map? (:semilattice %))
                    (contains? (:semilattice %) :descent))
           %)
        (filter map? (tree-seq coll? seq record))))

(deftest recorded-constructions-reproduce-the-cycle-census
  (let [files (filter #(and (.isFile %)
                            (= "003-construction.edn" (.getName %)))
                      (file-seq (io/file "data/wm-full-loop")))
        descents (keep (fn [file]
                         (when-some [cascade
                                     (cascade-with-descent
                                      (edn/read-string (slurp file)))]
                           {:file (.getPath file)
                            :elements (:patterns cascade)
                            :descent (get-in cascade [:semilattice :descent])}))
                       files)
        checked (map #(assoc % :check
                             (sut/check-cascade-order
                              {:elements (:elements %)
                               :descent (:descent %)}))
                     descents)
        cyclic (filter #(= :failed
                           (get-in % [:check :acyclic-descent :result]))
                       checked)]
    (is (= 25 (count descents)))
    (is (= 4 (count cyclic)))
    (is (= 5 (count (filter (comp empty? :descent) descents))))
    (is (every? seq (map #(get-in % [:check :acyclic-descent :cycle])
                         cyclic)))))

;; ---------------------------------------------------------------------------
;; A PINNED DEFECT with a DECLARED BOUNDARY.
;;
;; Joe, 2026-08-27: "'empty co-app' should be a known failing test, not
;; something to chase right away."
;; Joe, 2026-08-28: "the current census sounds as though maybe we need to do a
;; cleanup. Which would be relevant to getting the R5-red-ring sorted out."
;;
;; He was right, and the first version made the error it exists to catch. It
;; reported "25 records" as though that were the corpus. The corpus is 86 files:
;; 25 carry a :semilattice as {:descent [[from to]…] :co_app […]}, 21 carry one
;; as the registry's bare vector of {:from … :to …} maps, and 40 carry none. The
;; first version's regex matched only the first shape, so 21 real cascades were
;; silently outside the count -- an untyped absence, which is I2, in the test.
;;
;; This is R5's requirement (CoverageReport.lean: declaresCoverage,
;; outsideIsTyped) applied to a census of our own: state the whole corpus, and
;; give every member a type, including the ones not checked.
;;
;; What it pins, measured 2026-08-28:
;;
;;   corpus              86   (62 live, 24 archived under a 2026-07-15 stop-line)
;;     :descent-map      25   futon3c strategic-cascade shape -- CHECKED
;;     :from-to-vector   21   futon2 pattern-registry shape   -- CHECKED
;;     :absent           40   no :semilattice key             -- typed, not checked
;;
;;   of the 46 checked:  4 cyclic, 23 with a pair having no meet
;;
;; THE SPLIT IS THE FINDING, and it is not what the first version implied.
;; Every defect sits in one producer: all 4 cycles and all 23 missing meets are
;; :descent-map, while all 21 :from-to-vector cascades pass both checks. The
;; registry's are well formed because they are a hardcoded four-edge chain
;; (pattern_registry.clj:322) -- constant, hence trivially an order. The
;; GENERATED cascades are the malformed ones, 23 of 25.
;;
;; Archived records are counted and typed rather than excluded: they are 24 of
;; the 86 and include a duplicate of attempt-001. Excluding them silently would
;; be the same defect one level up.
;;
;; The corpus is under data/, which is gitignored, so a fresh checkout has none.
;; Absence of the corpus is reported as itself and does not fail the suite -- a
;; green run over zero records would be the silent success this work refuses.

(def ^:private corpus-root (io/file "data/wm-full-loop"))

(defn- construction-files []
  (when (.isDirectory corpus-root)
    (->> (file-seq corpus-root)
         (filter #(.endsWith (.getName ^java.io.File %) "003-construction.edn"))
         sort)))

(defn- semilattice-of [t]
  (or (some-> (re-find #"(?s):semilattice\s*(\{.*?:co_app[^}]*\})" t) second
              (as-> x (try (edn/read-string x) (catch Exception _ nil))))
      (some-> (re-find #"(?s):semilattice\s*(\[\{.*?\}\])" t) second
              (as-> x (try (edn/read-string x) (catch Exception _ nil))))))

(defn- carrier-of [t]
  (some-> (re-find #"(?s):patterns\s*(\[[^]]*\])" t) second
          (as-> x (try (edn/read-string x) (catch Exception _ nil)))))

(defn- classify [f]
  (let [t (slurp f)
        sl (semilattice-of t)
        ps (carrier-of t)]
    {:archived (.contains (str f) "/archives/")
     :shape (cond (map? sl) :descent-map (vector? sl) :from-to-vector :else :absent)
     :res (when sl (sut/check-cascade-order (if (and (map? sl) ps)
                                              (assoc sl :elements ps)
                                              sl)))}))

(deftest recorded-cascades-are-pinned-with-a-declared-boundary
  (if-not (.isDirectory corpus-root)
    (println "cascade-order census: corpus absent (data/ is gitignored) — not run")
    (let [rows (mapv classify (construction-files))
          by-shape (frequencies (map :shape rows))
          checked (filter :res rows)
          failed (fn [k] (filter #(= :failed (get-in % [:res k :result])) checked))]
      (testing "the corpus is declared in full, and every member is typed"
        (is (= 86 (count rows)) "construction records in the corpus")
        (is (= 24 (count (filter :archived rows))) "archived, counted not excluded")
        (is (= {:descent-map 25 :from-to-vector 21 :absent 40} by-shape)
            "every record has a shape, including the 40 carrying no cascade")
        (is (= 46 (count checked)) "records carrying a cascade, both producer shapes"))
      (testing "PINNED DEFECTS, and they are confined to one producer"
        (is (= 4 (count (failed :acyclic-descent))) "cyclic descent — not a partial order")
        (is (= 23 (count (failed :has-meets))) "a pair with no greatest lower bound")
        (is (= #{:descent-map} (set (map :shape (failed :acyclic-descent))))
            "every cycle is in the GENERATED strategic-cascade shape")
        (is (= #{:descent-map} (set (map :shape (failed :has-meets))))
            "every missing meet is too; all 21 registry cascades pass both")))))
