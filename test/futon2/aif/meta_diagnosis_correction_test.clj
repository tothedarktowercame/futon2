(ns futon2.aif.meta-diagnosis-correction-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.meta-self-heal-production :as p]
            [futon2.aif.meta-self-heal-production-test :as fixtures]))

(def fixture-path "test/fixtures/meta-self-heal/h4-full-diagnosis.edn")
(def fixture-sha "0f5937ffa3161d619dd937846d272b0686c2a9f3e00ec99bee3f5ab988e1ce1a")
;; The raw reply is verbatim and lacks one closing brace. The paired parsed
;; fixture is the serving one-edn-map result (existing :read-repair appended 1),
;; recovered read-only; no manual completion or key relocation was used.
(def nested-reply (edn/read-string (slurp "test/fixtures/meta-self-heal/h4-parsed-diagnosis.edn")))
(def corrected-reply
  (-> nested-reply
      (assoc-in [:diagnosis :production-functions]
                (get-in nested-reply [:diagnosis :primary-cause :production-functions]))
      (update-in [:diagnosis :primary-cause] dissoc :production-functions)))

(defn exercise [responses]
  (let [dir (str "/home/joe/notes/M2f-7-tests/" (java.util.UUID/randomUUID))
        _ (.mkdirs (io/file dir))
        pin (#'fixtures/recorded-injury-source! dir)
        {:keys [ledger active]} (#'fixtures/durable-with-active-source pin)
        requests (atom [])
        result (p/produce-diagnosis!
                {:meta-repository-root (System/getProperty "user.dir")
                 :meta-diagnosis-root (str dir "/diagnoses")
                 :meta-diagnosis-fn (fn [request]
                                      (let [i (count @requests)]
                                        (swap! requests conj request)
                                        (nth responses i)))}
                {:ledger ledger} active)]
    {:result result :requests @requests
     :pins (filter #(.isFile %) (file-seq (io/file dir "diagnoses")))}))

(deftest nested-live-reply-corrects-once
  (is (= fixture-sha (identity/sha256 (java.nio.file.Files/readAllBytes
                                      (.toPath (io/file fixture-path))))))
  (let [{:keys [result requests pins]} (exercise [nested-reply corrected-reply])
        c (:correction (second requests))]
    (is (= 2 (count requests)))
    (is (= :available (:status result)))
    (is (= 1 (count pins)))
    (is (= [[:primary-cause :production-functions]] (:found-at c)))
    (is (= [:selection-entrypoint :required-loop-node-evaluator :terminal-routing-entrypoint]
           (get-in c [:defects :missing-roles])))
    (is (= :absent (get-in c [:defects :production-functions-field])))
    (is (= (pr-str (:diagnosis nested-reply)) (:first-diagnosis c)))
    (is (= (first requests) (dissoc (second requests) :correction)))
    (when-let [path (get-in result [:diagnosis-evidence-pin :path])]
      (is (= (:diagnosis corrected-reply) (:diagnosis (edn/read-string (slurp path))))))))

(deftest second-invalid-diagnosis-exhausts
  (let [{:keys [result requests pins]} (exercise [nested-reply nested-reply])]
    (is (= 2 (count requests)))
    (is (= :meta-self-heal/diagnosis-correction-exhausted (:reason result)))
    (is (= :absent (get-in result [:first :production-functions-field])))
    (is (= (:first result) (:second result)))
    (is (empty? pins))))

(deftest valid-first-response-needs-no-correction
  (let [{:keys [result requests]} (exercise [corrected-reply])]
    (is (= :available (:status result)))
    (is (= 1 (count requests)))
    (is (not (contains? (first requests) :correction)))))

(deftest failed-port-response-is-not-corrected
  (let [{:keys [result requests pins]} (exercise [{:status :failed :reason :fixture-failure}])]
    (is (= :meta-self-heal/diagnosis-unavailable (:reason result)))
    (is (= 1 (count requests)))
    (is (empty? pins))))
