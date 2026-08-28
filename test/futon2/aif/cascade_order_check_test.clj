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
;; A PINNED DEFECT, not a target.
;;
;; Joe, 2026-08-27: "'empty co-app' should be a known failing test, not
;; something to chase right away."
;;
;; This census runs the checker over the recorded constructions and asserts the
;; state as measured on 2026-08-27. It is a tripwire, not a goal: if these
;; numbers change, someone has changed cascade construction, and the change
;; should be deliberate enough to update the pin in the same commit.
;;
;; What it pins, and why each is a defect rather than a fact of life:
;;   25 records carry a :descent block
;;    4 of them have a cyclic descent          -- not a partial order at all
;;   23 of 25 have a pair with no meet         -- the patterns are not related
;;   22 of 25 have an EMPTY :co_app            -- no co-application edges at all
;;
;; The co-app number is the one to read last and act on last. An empty :co_app
;; may mean the cascade is malformed, or may mean co-application edges were
;; never produced by the constructor. Those are different repairs and nothing
;; here distinguishes them.
;;
;; The corpus is under data/, which is gitignored, so a fresh checkout has no
;; records. Absence of the corpus is reported as itself and does not fail the
;; suite (I2, typed absence) -- a green run on an empty corpus would be the
;; silent success this whole line of work exists to refuse.

(def ^:private corpus-root (io/file "data/wm-full-loop"))

(defn- construction-records []
  (when (.isDirectory corpus-root)
    (->> (file-seq corpus-root)
         (filter #(.endsWith (.getName ^java.io.File %) "003-construction.edn"))
         sort)))

(defn- census []
  (reduce
   (fn [acc f]
     (let [t (slurp f)
           sm (re-find #"(?s):semilattice\s*(\{.*?:co_app[^}]*\})" t)
           pm (re-find #"(?s):patterns\s*(\[[^]]*\])" t)
           sl (some-> sm second (as-> x (try (edn/read-string x) (catch Exception _ nil))))
           ps (some-> pm second (as-> x (try (edn/read-string x) (catch Exception _ nil))))]
       (if-not sl
         acc
         (let [r (sut/check-cascade-order (cond-> sl ps (assoc :elements ps)))]
           (-> acc
               (update :records inc)
               (cond-> (= :failed (get-in r [:acyclic-descent :result])) (update :cyclic inc))
               (cond-> (= :failed (get-in r [:has-meets :result])) (update :no-meet inc))
               (cond-> (empty? (:co_app sl)) (update :empty-co-app inc)))))))
   {:records 0 :cyclic 0 :no-meet 0 :empty-co-app 0}
   (construction-records)))

(deftest recorded-cascades-are-pinned-as-defective
  (if-not (.isDirectory corpus-root)
    (println "cascade-order census: corpus absent (data/ is gitignored) — not run")
    (let [c (census)]
      (testing "the pinned state of the recorded corpus, 2026-08-27"
        (is (= 25 (:records c)) "construction records carrying a :descent block")
        (is (= 4  (:cyclic c))  "PINNED DEFECT: cyclic descent — not a partial order")
        (is (= 23 (:no-meet c)) "PINNED DEFECT: a pair with no greatest lower bound")
        (is (= 22 (:empty-co-app c))
            "PINNED DEFECT: no co-application edges — constructor or cascade, undetermined")))))
