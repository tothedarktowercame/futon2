(load-file "holes/labs/wm-contract/typed_gap_census_to_lean.bb")
(require '[clojure.test :refer [deftest is run-tests]])
(def fixture
  {:schema :wm/typed-gap-census-v1 :scope :isolated :authority/status :none
   :declared-nodes ["R\"2\nα"]
   :nodes [{:id "R\"2\nα" :state :unvalidated :claim "claim\\text" :scope "build"}]
   :declared-connections ["edge"]
   :connections [{:id "edge" :state :mandatory-unfired :scope "build"}]
   :selection {:state :refused-shape :reason "no evidence"}
   :families (mapv #(hash-map :family % :state :typed-gap :reason "not acquired") families)})
(defn denied? [f] (try (f) false (catch Exception _ true)))
(defn refused? [f]
  (try (f) false
       (catch clojure.lang.ExceptionInfo e (keyword? (:refusal (ex-data e))))
       (catch Exception _ false)))
(deftest generation-and-refusals
  (let [source (generate fixture)]
    (is (= source (generate fixture)))
    (is (str/includes? source "R\\\"2\\nα"))
    (is (str/includes? source "rejects_missing_record_family")))
  (doseq [x [(assoc fixture :scope :production)
             (assoc-in fixture [:selection :reason] (str (char 55296)))
             (dissoc fixture :families)
             (assoc fixture :declared-nodes ["R" "R"])
             (assoc fixture :declared-nodes ["another"])
             (update fixture :families #(vec (reverse %)))
             (assoc-in fixture [:families 0 :state] :present)
             (assoc-in fixture [:nodes 0 :state] :supported)
             (assoc-in fixture [:nodes 0 :extra] "ignored")]]
    (is (denied? #(generate x)))))
(deftest captured-input
  (let [bs (.getBytes (pr-str fixture) "UTF-8")]
    (is (= fixture (decode-input bs (sha256 bs))))
    (is (denied? #(decode-input bs (apply str (repeat 64 "0"))))))
  (doseq [s ["{} {}" "{:a 1 :a 2}" "" "{:unclosed"]]
    (let [bs (.getBytes s "UTF-8")]
      (is (refused? #(decode-input bs (sha256 bs))))))
  (let [bs (byte-array [(unchecked-byte 255)])]
    (is (refused? #(decode-input bs (sha256 bs))))))
(let [r (run-tests)]
  (when (pos? (+ (:fail r) (:error r))) (System/exit 1)))
(spit "holes/labs/wm-contract/runs/row-24-typed-gap-generator-2026-09-13/input.edn" (pr-str fixture))
