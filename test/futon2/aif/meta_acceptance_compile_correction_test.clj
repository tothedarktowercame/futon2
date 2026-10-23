(ns futon2.aif.meta-acceptance-compile-correction-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.meta-self-heal-production :as p]
            [futon2.aif.meta-self-heal-production-test :as fixtures]))

(def log-path "test/fixtures/meta-self-heal/cfbcbad0-compile-error.log")
(def log-sha "708cf3d6707cbf32910f2c1ae866d9aefa36373f8042b217e231ee9cbd8f1b2c")
(def diagnostic {:file "futon2/aif/meta_acceptance_injury_cfbcbad0_test.clj"
                 :line 92 :col 3 :message "No such var: falsifier/want-check-refusal"})
(def compile-run
  {:status :ran :outcome :failed :exit 1
   :summary "Syntax error compiling at (futon2/aif/meta_acceptance_injury_cfbcbad0_test.clj:92:3)."
   :run-pin {:path (.getCanonicalPath (io/file log-path)) :sha256 log-sha}
   :compile-diagnostic diagnostic})
(def ordinary-run (-> compile-run (dissoc :compile-diagnostic)
                      (assoc :summary "Ran 1 tests. 1 failures.")))

(defn exercise [runs]
  (let [dir (str "/home/joe/notes/M2f-2-fixtures/" (java.util.UUID/randomUUID))
        _ (.mkdirs (io/file dir))
        diagnosis (#'fixtures/produced-diagnosis! dir)
        requests (atom []) reviews (atom []) executed (atom [])
        options {:meta-acceptance-root (str dir "/acceptances")
                 :meta-repository-root (System/getProperty "user.dir")
                 :meta-acceptance-fn
                 (fn [r]
                   (swap! requests conj r)
                   {:status :proposed
                    :acceptance-test (#'fixtures/acceptance-case
                                      (str "correction-" (count @requests)) :injury)})
                 :meta-acceptance-run-fn
                 (fn [test]
                   (let [n (count @executed)]
                     (swap! executed conj test)
                     (nth runs n)))
                 :meta-acceptance-review-fn
                 (fn [r] (swap! reviews conj r) (assoc r :status :accepted))}
        result (p/prepare-acceptance options diagnosis)]
    {:result result :requests @requests :reviews @reviews :executed @executed}))

(deftest compile-then-corrected-assertion-failure
  (is (= log-sha (identity/sha256
                 (java.nio.file.Files/readAllBytes (.toPath (io/file log-path)))) ))
  (let [{:keys [result requests reviews executed]} (exercise [compile-run ordinary-run])
        correction (:correction (second requests))]
    (is (= 2 (count requests)))
    (is (= 2 (count executed)))
    (is (= 1 (count reviews)))
    (is (= ordinary-run (:result (first reviews))))
    (is (= :accepted (:status result)))
    (is (not (contains? (first requests) :correction)))
    (is (= diagnostic (:compile-diagnostic correction)))
    (is (= (:run-pin compile-run) (:first-run-pin correction)))
    (is (string? (get-in correction [:first-proposal-pin :sha256])))
    (is (= (select-keys correction [:first-run-pin :first-proposal-pin])
           (:acceptance-compile-correction result)))
    (is (string? (:instruction correction)))))

(deftest second-compile-failure-exhausts-correction
  (let [{:keys [result requests reviews executed]} (exercise [compile-run compile-run])]
    (is (= 2 (count requests)))
    (is (= 2 (count executed)))
    (is (empty? reviews))
    (is (= :meta-self-heal/acceptance-compile-correction-exhausted (:reason result)))
    (is (= compile-run (:first result) (:second result)))
    (is (false? (:repair-claimed? result)))))

(deftest ordinary-failure-needs-no-correction
  (let [{:keys [result requests reviews]} (exercise [ordinary-run])]
    (is (= 1 (count requests) (count reviews)))
    (is (= :accepted (:status result)))
    (is (not (contains? (first requests) :correction)))
    (is (not (contains? result :acceptance-compile-correction)))))
