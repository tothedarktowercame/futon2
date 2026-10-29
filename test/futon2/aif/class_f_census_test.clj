(ns futon2.aif.class-f-census-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.reading-policy-class-history-index :as index]
            [futon2.aif.reading-policy-class-free-energy :as class-f]
            [futon2.aif.cascade-shape-g :as shape]
            [futon2.aif.wm.family-selection :as family]))

(defn temp-root []
  (.toFile (java.nio.file.Files/createTempDirectory "class-f-census-" (make-array java.nio.file.attribute.FileAttribute 0))))
(defn cleanup [dir]
  (doseq [f (reverse (file-seq dir))] (io/delete-file f)))
(defn policies []
  (edn/read-string (slurp (io/file "test/resources/wm/class-f-census/held-policies.edn"))))

(deftest sidecars-do-not-block-the-frozen-policies
  (let [dir (temp-root)]
    (try
      (doseq [name ["tick-run-record-2026-10-29-a.edn" "tick-run-record-2026-10-29-b.edn"]]
        (spit (io/file dir name) (pr-str {:run/id name})))
      (doseq [suffix [".edn.requirements.edn" ".learning.edn" ".scan.md"]]
        (spit (io/file dir (str "tick-run-record-2026-10-29-a" suffix)) "sidecar, not a record"))
      (is (= 2 (count (index/terminal-files dir))))
      (is (= 2 (:records (index/backfill! dir (io/file dir index/index-name)))))
      (is (= {:status :complete :covered-count 2 :record-count 2}
             (select-keys (index/read-index dir) [:status :covered-count :record-count])))
      (with-redefs [class-f/history-context (fn [key] (index/history dir key 32))]
        (doseq [policy (policies)]
          (let [score (shape/score-policy policy)]
            (is (= :computed (:status score)))
            (is (= :computed (get-in score [:class-emission :f-status])))
            (is (= 0.0 (get-in score [:class-emission :certificate :f :value]))))))
      (finally (cleanup dir)))))

(deftest refusal-retains-the-exact-inference
  (let [dir (temp-root)]
    (try
      (with-redefs [class-f/history-context (fn [key] (index/history dir key 32))]
        (let [ps (policies)
              expected (mapv #(get-in (shape/score-policy %) [:class-emission :reason]) ps)
              row (first (family/score-family-field
                           [{:target-id "M-reachable-from-boot" :status :computed
                             :policies ps :provenance {} :failures []}]))]
          (is (= 3 (count (:failures row))))
          (is (= expected (mapv :inference (:failures row))))
          (is (every? #(= :history-index-absent (get-in % [:inference :status])) (:failures row)))
          (is (every? #(string? (get-in % [:inference :f :index-path])) (:failures row)))))
      (finally (cleanup dir)))))
