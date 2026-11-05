(ns futon2.aif.refusal-site-index-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.refusal-site-index :as sut]))

(def repo (.getCanonicalPath (io/file ".")))

(defn- python-stop-call-count []
  (count (re-seq #"(?m)^\s*return stop\(args,"
                 (slurp (io/file repo "scripts/wm_click_repair_loop.py")))))

(defn- indexed-python-stop-count [index]
  (count (filter #(= "scripts/wm_click_repair_loop.py" (:source %))
                 (:sites index))))

(deftest every-known-production-refusal-site-has-one-live-index-entry
  (let [index (sut/read-index)
        audit (sut/audit repo)]
    (is (= :verified (:status audit)) (pr-str (:problems audit)))
    (is (= (count (:sites index)) (count (set (map :id (:sites index))))))
    (is (= (python-stop-call-count) (indexed-python-stop-count index))
        "every Python return stop(args call has exactly one indexed row")))

(deftest every-python-stop-names-a-literal-validator-case
  (let [text (slurp (io/file repo "scripts/wm_click_repair_loop.py"))
        calls (re-seq #"(?m)^\s*return stop\(args," text)
        literals (re-seq #"(?m)^\s*return stop\(args,\s*\"(operator-override|work-exhausted|debugger-error)\""
                         text)]
    (is (= 20 (count calls)))
    (is (= (count calls) (count literals)))))

(deftest changed-anchor-refuses-the-index
  (let [index (update-in (sut/read-index) [:sites 0]
                         assoc :anchor "REFUSAL-SITE-ANCHOR-DOES-NOT-EXIST")
        result (sut/audit-index repo index)]
    (is (= :refused (:status result)))
    (is (some #(= :anchor-missing (:reason %)) (:problems result))
        (pr-str (:problems result)))))

(deftest missing-python-stop-row-fails-coverage
  (let [index (update (sut/read-index) :sites
                      (fn [sites]
                        (let [id (:id (first (filter
                                             #(= "scripts/wm_click_repair_loop.py"
                                                 (:source %))
                                             sites)))]
                          (filterv #(not= id (:id %)) sites))))
        result (sut/audit-index repo index)]
    (is (not= (python-stop-call-count) (indexed-python-stop-count index)))
    (is (= :refused (:status result)))
    (is (some #(= :python-stop-coverage-mismatch (:reason %))
              (:problems result))
        (pr-str (:problems result)))))
