(ns futon2.aif.meta-acceptance-compile-correction-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
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

(def retained-proposals
  [{:path "/home/joe/code/futon2/data/wm-meta-injuries-live-2026-10-22/acceptances/acceptance-e209cb9fdb7f88848e725c706b93ec0325f45521b0563663424589d11f9c50c7.edn"
    :diagnostic {:file "futon2/aif/meta_acceptance_injury_ad038c48_test.clj"
                 :line 0 :col 0
                 :message "Unable to resolve symbol: defn-placeholder-not-used in this context"}
    :run-pin {:path "/home/joe/notes/wm-next/criteria-completeness/repair-iteration-10/acceptance-first.log"
              :sha256 "7fe4cb2fc621ec6b7cd24d897dd518a4fbc29dec09a62e31e00e0876c2c3d889"}}
   {:path "/home/joe/code/futon2/data/wm-meta-injuries-live-2026-10-22/acceptances/acceptance-5e78f46f7171283740e9f3e7e1e826421f527e20dc98d5651203687d864330c3.edn"
    :diagnostic {:file "futon2/aif/meta_acceptance_injury_ad038c48_test.clj"
                 :line 119 :col 1
                 :message "EOF while reading, starting at line 39"}
    :run-pin {:path "/home/joe/notes/wm-next/criteria-completeness/repair-iteration-10/acceptance-second.log"
              :sha256 "a4fcb89b1c09c101358f5b7d0e8db83306eddc728539dc0da5ad740b9a0d426a"}}])

(defn retained-correction-request [{:keys [path diagnostic run-pin]}]
  (let [dir (str "/home/joe/notes/M2f-2-fixtures/" (java.util.UUID/randomUUID))
        _ (.mkdirs (io/file dir))
        retained (edn/read-string (slurp path))
        acceptance-test (:acceptance-test retained)
        diagnosis (#'fixtures/produced-diagnosis! dir)
        requests (atom [])
        runs (atom 0)
        run {:status :ran :outcome :failed :exit 1
             :summary (:message diagnostic)
             :run-pin run-pin
             :compile-diagnostic diagnostic}
        result (p/prepare-acceptance
                {:meta-acceptance-root (str dir "/acceptances")
                 :meta-repository-root (System/getProperty "user.dir")
                 :meta-acceptance-fn
                 (fn [request]
                   (swap! requests conj request)
                   (if (= 1 (count @requests))
                     {:status :proposed :acceptance-test acceptance-test}
                     {:status :failed :reason :fixture-correction-captured}))
                 :meta-acceptance-run-fn (fn [_] (swap! runs inc) run)
                 :meta-acceptance-review-fn
                 (fn [_] (throw (ex-info "compile failure must not reach review" {})))}
                diagnosis)]
    {:acceptance-test acceptance-test
     :requests @requests
     :runs @runs
     :result result}))

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

(deftest compile-correction-carries-the-exact-pinned-failing-source
  (doseq [input retained-proposals]
    (let [{:keys [acceptance-test requests runs result]}
          (retained-correction-request input)
          correction (:correction (second requests))
          rendered-request (edn/read-string (pr-str (second requests)))
          proposal-pin (:first-proposal-pin correction)
          pinned-proposal (edn/read-string (slurp (:path proposal-pin)))]
      (is (= 2 (count requests)))
      (is (= 1 runs) "each retained proposal is a separate first failure")
      (is (= (:diagnostic input) (:compile-diagnostic correction)))
      (is (= (:run-pin input) (:first-run-pin correction)))
      (is (= (:source acceptance-test) (:first-proposal-source correction)))
      (is (= (:first-proposal-source correction)
             (get-in pinned-proposal [:acceptance-test :source])))
      (is (= (:source acceptance-test)
             (get-in rendered-request [:correction :first-proposal-source])))
      (is (= (:sha256 proposal-pin)
             (identity/sha256 (.getBytes (str (pr-str pinned-proposal) "\n") "UTF-8"))))
      (is (.contains (:instruction correction) "minimal diagnostic correction"))
      (is (.contains (:instruction correction) "preserve the test's intent"))
      (is (= :meta-self-heal/acceptance-unavailable (:reason result)))
      (is (false? (:repair-claimed? result))))))

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
