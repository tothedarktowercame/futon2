(ns futon2.aif.repair-proposals-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-proposals :as proposals]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.repair-obligation :as repair]
            [futon2.aif.repair-proposals :as supply]
            [futon2.report.war-machine :as wm])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn with-store [f]
  (let [root (.toFile (Files/createTempDirectory "repair-proposals" (make-array FileAttribute 0)))]
    (try (f (str root))
         (finally (doseq [file (reverse (file-seq root))] (Files/delete (.toPath file)))))))

(defn finding! [root id & [changes]]
  (repair/record-system-failure!
   root (merge {:attempt-id (str "attempt-" id) :repair-id id
                :repair-class :machine-failure :target "M-original"
                :failure-stage :construction :outcome :failed :failure-kind :fold-output-invalid
                :error "Constructor supplied no folded coverage"
                :backtrace {:error-class "clojure.lang.ExceptionInfo" :code-state {:repo "futon2"}}
                :opened-at "2026-09-21T00:56:55Z"
                :discharge-contract repair/review-failure-discharge-contract} changes)))

(deftest real-store-finding-yields-evidence-only-t-proposal
  (with-store
    (fn [root]
      (let [finding (finding! root "repair-example")
            result (supply/supply root)
            p (first (:proposals result))
            pin (get-in p [:evidence :finding-source])]
        (is (= 1 (count (:proposals result))))
        (is (empty? (:declines result)))
        (is (= "T-repair-example" (:target p)))
        (is (= :T (:target-type p)))
        (is (= :proposed (:status p)))
        (is (= :repair-finding-proposed (:origin p)))
        (is (= "M-original" (get-in p [:evidence :target])))
        (is (= (:discharge-contract finding) (get-in p [:evidence :discharge-contract])))
        (is (= (:sha256 pin) (evidence/sha256 (Files/readAllBytes (.toPath (io/file (:path pin)))))))
        (is (= (assoc pin :status :retained :edn-path [:backtrace]) (get-in p [:evidence :backtrace])))
        (is (= :unavailable (get-in p [:evidence :closure-observation :status])))
        (is (every? #(not (contains? p %)) [:pattern :reading :guard :produces :locators :interpretation-receipts]))
        (is (= (:proposals result) (:proposals (supply/supply root))))))))

(deftest real-disposition-writers-remove-proposals-on-next-read
  (with-store
    (fn [root]
      (finding! root "repair-dismiss"
                {:failure-data {:author-job {:execution {:executed false :tool-events 0 :command-events 0}}}})
      (let [resolvable (finding! root "repair-resolve" {:repair-class :environmental-hold})]
        (is (= 2 (count (:proposals (supply/supply root)))))
        (repair/dismiss-unexecuted! root "repair-dismiss"
                                    {:authority "constructed-test-disposition" :reason :never-executed :actor "test"})
        (repair/resolve! root resolvable
                         {:attempt-id "distinct-successor" :commit "fixture-commit"
                          :reviewer "independent-fixture" :review-job "fixture-review"
                          :witness {:resolved? true :dial-moved? true}
                          :validation {:production-shaped? true}})
        (let [r (supply/supply root)]
          (is (empty? (:proposals r)))
          (is (empty? (:declines r)))
          (is (empty? (get-in r [:repair-scan :open-finding-ids]))))))))

(deftest schema-less-or-malformed-contract-is-a-decline-not-invented-acceptance
  (with-store
    (fn [root]
      (finding! root "repair-no-contract" {:discharge-contract nil})
      (finding! root "repair-bad-contract" {:discharge-contract {:requires [] :artifact-shape :unknown}})
      (let [file (io/file root "findings" "repair-legacy.edn")]
        (spit file (pr-str {:repair/id "repair-legacy" :repair/status :open :target "M-original"
                           :opened-at "2026-09-21T00:00:00Z"})))
      (let [r (supply/supply root)
            declines (into {} (map (juxt :repair/id identity)) (:declines r))]
        (is (empty? (:proposals r)))
        (is (= 3 (count declines)))
        (is (some #{:discharge-contract} (get-in declines ["repair-no-contract" :missing-evidence])))
        (is (some #{:repair/schema-version} (get-in declines ["repair-legacy" :missing-evidence])))
        (is (some #{[:discharge-contract :requires]} (get-in declines ["repair-bad-contract" :missing-evidence])))))))

(deftest alternate-contracts-and-absent-backtraces-are-preserved-honestly
  (with-store
    (fn [root]
      (let [contract {:requires [:cleared-precondition :grounded-production-shaped-successor]
                      :artifact-shape :code-commit}]
        (finding! root "repair-environment" {:repair-class :environmental-hold :target nil
                                            :backtrace nil :discharge-contract contract})
        (let [p (first (:proposals (supply/supply root)))]
          (is (= contract (get-in p [:evidence :discharge-contract])))
          (is (nil? (get-in p [:evidence :target])))
          (is (= {:status :absent :reason :not-retained} (get-in p [:evidence :backtrace]))))))))

(deftest no-committed-closure-witness-means-no-executable-repair
  (with-store
    (fn [root]
      (finding! root "repair-waiting")
      (let [supplied (proposals/load-supply {:repair-root root :proposal-dir (str (io/file root "retrieval"))})
            target "T-repair-waiting"
            ;; Even a constructed declaration cannot bypass the unavailable witness.
            assembled {:problems [{:target target :constructed-candidates [{:precedence [:p/repair]
                                                                           :construction-receipt {:kind :hand-admitted}}]}]
                       :refusals []}
            recorded (proposals/record-supply assembled {} supplied)
            result (wm/cascade-decision recorded {})]
        (is (= 1 (count (:proposals supplied))))
        (is (empty? (:problems recorded)))
        (is (= :repair-closure-observation-unavailable (get-in recorded [:refusals 0 :reason])))
        (is (= :no-acting-cascade-candidate (get-in result [:decision :reason])))
        (is (some #(= :repair-closure-observation-unavailable (:reason %)) (:dropped-candidates result)))
        (is (= (:proposals supplied) (get-in result [:decision :selection-certificate :proposal-supply :proposals])))))))
