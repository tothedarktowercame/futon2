(ns futon2.aif.medic-ticket-closure-test
  "Receiver-side checks: they read the ticket FILE the receiver reads, not a
   return value. Fixtures come from the real producer (finding-ticket/publish!)
   plus the real finding shape retained from run 2026-10-31 (adf04df0)."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.repair-obligation :as repair]
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

(defn- tick-record-pin
  "A cause record in the REAL tick-run-record shape (retained from the two
   2026-10-31 production records f805ca4b and adc46631): :q6 :terminal names
   the record's OWN run, :q6 :previous names the prior run."
  [root run-id & {:keys [kind]}]
  (let [path (io/file root (str "tick-run-record-" run-id ".edn"))
        record {:pattern-graph-diff {:status :absent :reason :no-selected-action}
                :q6 {:previous {:schema :wm/q6-terminal-v1
                                :run/id "2026-10-30-prior-run-0000"
                                :outcome {:status :present :outcome :changed}}
                     :terminal {:schema :wm/q6-terminal-v1
                                :run/id run-id
                                :outcome {:status :present :outcome :refused
                                          :kind (or kind :author-delivery-wrong-repository)}}}}]
    (io/make-parents path)
    (spit path (str (pr-str record) "\n"))
    {:path (.getCanonicalPath path)
     :sha256 (load-identity/sha256
              (java.nio.file.Files/readAllBytes (.toPath path)))}))

(defn- own-run-id [pin]
  (closure/cause-record-own-run-id pin))

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

(deftest cause-record-own-run-id-reads-the-terminal-block-not-previous
  (let [root (temp-dir "wm-cause-record")
        pin (tick-record-pin root "2026-10-31-f805ca4b-0000")]
    (is (= "2026-10-31-f805ca4b-0000" (own-run-id pin)))
    ;; A record misfiled under another run's name lends no identity.
    (is (nil? (own-run-id (assoc pin :path (str (.getParentFile (io/file (:path pin)))
                                                 "/tick-run-record-someone-else.edn"))))
        "misfiled record refuses")))

(deftest run-id-join-closes-and-kind-twin-stays-open
  (let [cause-dir (temp-dir "wm-cause-record")
        run-id "2026-10-31-adc46631-0000"
        pin (tick-record-pin cause-dir run-id :kind :answer-reviewer-mismatch)
        own-id "repair-ea1-route-runid--attempt-002-answer-reviewer-mismatch"
        twin-id "repair-ea1-kind-twin-runid--attempt-002-answer-reviewer-mismatch"
        root (repair-store
              [(assoc (dissoc (real-finding own-id nil) :cause-record-pin)
                      :origin-run/id run-id)
               (assoc (dissoc (real-finding twin-id nil) :cause-record-pin)
                      :origin-run/id "2026-10-31-someone-else")])
        own-ticket (ticket-file root own-id)
        twin-ticket (ticket-file root twin-id)
        injury {:injury-id "injury/runid-join" :cause-record-pin pin}
        joined (closure/own-cause-findings root injury)]
    (is (= [own-id] (mapv :id joined)) (pr-str joined))
    (is (= :run-id (:join (first joined))))
    (let [result (closure/close-cause-ticket!
                  root {:injury-id "injury/runid-join"
                        :entry {:ordinal 9 :cause-record-pin pin}
                        :fixed-by (apply str (repeat 40 "b"))
                        :receipt-pin {:sha256 (apply str (repeat 64 "d"))}})]
      (is (= :closed (:status result)) (pr-str result))
      (is (re-find #"(?m)^\*\*Status:\*\* DONE$" (slurp own-ticket)))
      (is (re-find (re-pattern "discharged at ledger ordinal 9") (slurp own-ticket)))
      ;; The kind twin: same failure-kind and stage, different run. Stays OPEN.
      (is (re-find #"(?m)^\*\*Status:\*\* OPEN$" (slurp twin-ticket))
          "kind twin with a different origin run stays OPEN"))))

(deftest minting-retains-the-origin-run-id
  "The minting side of the join: record-system-failure! keeps :origin-run/id
   on the durable finding, so a later injury pinning that run's tick record
   closes the ticket exactly. Read back from the store file."
  (let [cause-dir (temp-dir "wm-cause-record")
        run-id "2026-10-31-minted-run-0000"
        pin (tick-record-pin cause-dir run-id)
        repair-root (temp-dir "wm-repair-store-mint")
        finding (repair/record-system-failure!
                 repair-root
                 {:attempt-id "ea1-minted-0000000000000000000000000000000000000000--attempt-001"
                  :repair-class :incomplete-recoverable
                  :failure-stage :cascade-resume
                  :outcome :incomplete
                  :failure-kind :author-delivery-wrong-repository
                  :error "Selected cascade held in open-cascade phase"
                  :origin-run/id run-id})
        retained (edn/read-string
                  (slurp (io/file repair-root "findings"
                                  (str (:repair/id finding) ".edn"))))]
    (is (= run-id (:origin-run/id retained)))
    (is (= [(:repair/id retained)]
           (mapv :id (closure/own-cause-findings
                      repair-root {:injury-id "injury/minted"
                                   :cause-record-pin pin})))
        "the minted finding joins the injury that pins its run's record")))

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
