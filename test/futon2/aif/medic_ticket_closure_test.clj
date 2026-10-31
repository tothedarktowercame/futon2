(ns futon2.aif.medic-ticket-closure-test
  "Receiver-side checks: they read the ticket FILE the receiver reads, not a
   return value. Fixtures come from the real producer (finding-ticket/publish!)
   plus the real finding shape retained from run 2026-10-31 (adf04df0)."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.finding-ticket :as finding-ticket]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.medic-ticket-closure :as closure]
            [futon2.aif.meta-self-heal-harness :as harness]
            [futon2.aif.out-of-band-fix :as out-of-band]))

(defn- temp-dir [prefix]
  (.getCanonicalPath
   (.toFile (java.nio.file.Files/createTempDirectory
             prefix (make-array java.nio.file.attribute.FileAttribute 0)))))

(defn- real-finding
  "Shape retained from the production finding
   repair-ea1-19929445...--attempt-002-author-delivery-wrong-repository.edn
   (2026-10-31), reduced to the fields the store and closure read."
  [id cause-pin]
  {:discharge-contract {:requires [:complete-existing-author-artifact
                                   :independent-review :grounded-existing-commit]
                        :artifact-shape :code-commit}
   :repair/status :open
   :opened-at "2026-10-31T01:11:04.722273851Z"
   :repair/class :incomplete-recoverable
   :repair/id id
   :failure-kind :author-delivery-wrong-repository
   :failure-stage :cascade-resume
   :failure-context {:phase :cascade-resume}
   :cause-record-pin cause-pin})

(defn- repair-store [findings]
  (let [root (temp-dir "wm-repair-store")]
    (doseq [f findings]
      (let [path (io/file root "findings" (str (:repair/id f) ".edn"))]
        (io/make-parents path)
        (spit path (str (pr-str f) "\n")))
      (finding-ticket/publish! root (:repair/id f)))
    root))

(defn- ticket-file [root id]
  (io/file root "ticket-publication" "holes" "tickets" (str "T-" id ".md")))

(defn- publish-open-out-of-band
  "Drive the real medic route exactly like the production checked-open case."
  [env]
  (let [acceptance-root (str (:root env) "/acceptances")
        artifact-pin (out-of-band/retain-artifact!
                      acceptance-root
                      {:schema :wm/out-of-band-acceptance-v1
                       :injury-id (get-in env [:ingestion :entry :injury-id])
                       :historical-pin (get-in env [:ingestion :entry :cause-record-pin])
                       :failure-kind :fold-output-invalid
                       :failure-stage :construction
                       :fixed-by (apply str (repeat 40 "b"))
                       :acceptance-test
                       {:id "open-out-of-band"
                        :command ["clojure" "-M:test" "-n" "futon2.aif.meta-selection-contract-acceptance-test"]
                        :expected :configuration
                        :source {:namespace "futon2.aif.meta-selection-contract-acceptance-test"
                                 :path "test/futon2/aif/meta_selection_contract_acceptance_test.clj"
                                 :text "(ns futon2.aif.meta-selection-contract-acceptance-test\n  (:require [clojure.test :refer [deftest is]]))\n(deftest acceptance-control (is true))\n"}}})]
        {:artifact-pin artifact-pin :acceptance-root acceptance-root}))

(deftest medic-discharge-closes-own-cause-ticket-by-cause-pin
  (let [env (harness/prepare-environment! (temp-dir "wm-production-repair"))
        cause-pin (get-in env [:ingestion :entry :cause-record-pin])
        injury-id (get-in env [:ingestion :entry :injury-id])
        id "repair-ea1-own-cause--attempt-001-author-delivery-wrong-repository"
        root (repair-store [(real-finding id cause-pin)])
        ticket (ticket-file root id)
        before (slurp ticket)]
    (is (re-find #"(?m)^\*\*Status:\*\* OPEN$" before) "fixture is really OPEN")
    (let [result (closure/close-cause-ticket!
                  root {:injury-id injury-id
                        :entry {:ordinal 3 :cause-record-pin cause-pin}
                        :fixed-by (apply str (repeat 40 "b"))
                        :receipt-pin {:sha256 (apply str (repeat 64 "d"))}})]
      (is (= :closed (:status result)) (pr-str result))
      (let [after (slurp ticket)]
        (is (re-find #"(?m)^\*\*Status:\*\* DONE$" after)
            "the receiver reads Status DONE")
        (is (re-find (re-pattern (str "injury " injury-id " discharged at ledger ordinal 3")) after)
            "Disposition cites the ledger ordinal")
        (is (re-find (re-pattern (str "fixed by " (apply str (repeat 40 "b")))) after)
            "Disposition cites fixed-by")))))

(deftest failure-kind-match-alone-closes-nothing
  "The exact defect this row exists for: a ticket sharing failure-kind and
   stage with a discharged injury is NOT that injury's own cause and must stay
   OPEN. This is the bad case watched to fail (stay open) on the pin join."
  (let [env (harness/prepare-environment! (temp-dir "wm-production-repair"))
        cause-pin (get-in env [:ingestion :entry :cause-record-pin])
        injury-id (get-in env [:ingestion :entry :injury-id])
        ;; Same kind/stage, but pinned to a DIFFERENT cause record.
        other-path (str (:root env) "/other-cause.edn")
        _ (spit other-path (slurp (:record-path env)))
        other-pin {:path other-path
                   :sha256 (load-identity/sha256
                            (java.nio.file.Files/readAllBytes (.toPath (io/file other-path))))}
        id "repair-ea1-kind-twin--attempt-001-author-delivery-wrong-repository"
        root (repair-store [(real-finding id other-pin)])
        ticket (ticket-file root id)
        result (closure/close-cause-ticket!
                root {:injury-id injury-id
                      :entry {:ordinal 4 :cause-record-pin cause-pin}
                      :fixed-by (apply str (repeat 40 "b"))
                      :receipt-pin {:sha256 (apply str (repeat 64 "d"))}})]
    (is (= :held (:status result)) (pr-str result))
    (is (= :no-verifiable-cause-link (:reason result)))
    (is (re-find #"(?m)^\*\*Status:\*\* OPEN$" (slurp ticket))
        "the kind twin stays OPEN")))

(deftest tampered-cause-pin-refuses-closure
  (let [env (harness/prepare-environment! (temp-dir "wm-production-repair"))
        cause-pin (get-in env [:ingestion :entry :cause-record-pin])
        id "repair-ea1-tampered--attempt-001-author-delivery-wrong-repository"
        root (repair-store [(real-finding id cause-pin)])
        ticket (ticket-file root id)
        ;; Mutate the cause bytes after the finding pinned them.
        _ (spit (:path cause-pin) (str (slurp (:path cause-pin)) "\n;; tampered"))
        result (closure/close-cause-ticket!
                root {:injury-id (get-in env [:ingestion :entry :injury-id])
                      :entry {:ordinal 5 :cause-record-pin cause-pin}
                      :fixed-by (apply str (repeat 40 "b"))
                      :receipt-pin {:sha256 (apply str (repeat 64 "d"))}})]
    (is (= :held (:status result)) (pr-str result))
    (is (= :no-verifiable-cause-link (:reason result)))
    (is (re-find #"(?m)^\*\*Status:\*\* OPEN$" (slurp ticket)))))

(deftest route-attaches-ticket-closure-to-the-discharged-result
  (let [env (harness/prepare-environment! (temp-dir "wm-production-repair"))
        cause-pin (get-in env [:ingestion :entry :cause-record-pin])
        injury-id (get-in env [:ingestion :entry :injury-id])
        id "repair-ea1-route-own-cause--attempt-001-author-delivery-wrong-repository"
        repair-root (repair-store [(real-finding id cause-pin)])
        ticket (ticket-file repair-root id)
        {artifact-pin :artifact-pin acceptance-root :acceptance-root}
        (publish-open-out-of-band env)
        run-pin {:path (:record-path env)
                 :sha256 (load-identity/sha256
                          (java.nio.file.Files/readAllBytes
                           (.toPath (io/file (:record-path env)))))}
        checked {:schema :wm/out-of-band-fix-receipt-v1 :status :checked
                 :injury-id injury-id
                 :fixed-by (apply str (repeat 40 "b"))
                 :base-sha (apply str (repeat 40 "c"))
                 :historical-pin cause-pin
                 :historical-cause-checked? true
                 :current {:run-pin run-pin}
                 :current-report {:tests 1 :assertions 1 :failures 0 :errors 0}
                 :counterfactual {:run-pin run-pin}
                 :counterfactual-report {:tests 1 :assertions 1 :failures 1 :errors 0}}
        result (with-redefs [out-of-band/compute! (constantly
                                                   (assoc checked :artifact-pin artifact-pin))]
                 (runtime/run-opportunity!
                  (merge (harness/work-order-options env)
                         {:meta-injury-ledger-path (:ledger-path env)
                          :meta-repair-transaction-path (str (:root env) "/transaction.edn")
                          :meta-acceptance-root acceptance-root
                          :meta-repair-store-root repair-root
                          :meta-acceptance-run-fn (constantly
                                                    {:status :ran :outcome :passed :exit 0
                                                     :summary "fixture pass"
                                                     :run-pin run-pin})
                          :meta-repair-ports (harness/fake-ports (:repo env) (atom {}) :success)
                          :meta-diagnosis-fn (fn [_] (throw (ex-info "diagnosis contacted" {})))})))]
    (is (= :discharged (:status result)) (pr-str result))
    (is (= :closed (get-in result [:ticket-closure :status]))
        (pr-str (:ticket-closure result)))
    (let [after (slurp ticket)
          ordinal (get-in result [:ticket-closure :ordinal])]
      (is (re-find #"(?m)^\*\*Status:\*\* DONE$" after))
      (is (re-find (re-pattern (str "discharged at ledger ordinal " ordinal)) after)
          "the ticket the receiver reads cites the append ordinal"))))
