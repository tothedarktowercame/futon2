(ns futon2.aif.meta-self-heal-harness
  "Offline isolated-JVM harness for the dedicated META repair transaction."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-injury-store :as store]
            [futon2.aif.meta-repair-coordinator :as coordinator]))

(def campaign-fixture "test/fixtures/meta-self-heal/campaign-2026-10-19.edn")
(def phases [:claim :author :review :enact])

(defn- sh! [dir & args]
  (let [result (apply shell/sh (concat args [:dir dir]))]
    (when-not (zero? (:exit result))
      (throw (ex-info "offline adapter command failed"
                      {:args args :result result})))
    result))
(defn- write-form! [path value] (spit path (str (pr-str value) "\n")))
(defn- sha256-pin [path]
  (let [bytes (java.nio.file.Files/readAllBytes (.toPath (io/file path)))
        digest (load-identity/sha256 bytes)]
    {:path path :sha256 digest}))

(defn campaign-cause []
  (->> (edn/read-string (slurp campaign-fixture))
       (filter #(= :fold-output-invalid (get-in % [:record :failure :kind])))
       first))

(defn arbitrate [durable]
  (let [active (ledger/active-injuries (:ledger durable))]
    (if (seq (:injuries active))
      {:status :dedicated-repair-required :ordinary-work-eligible? false
       :active-injury-ids (vec (sort (keys (:injuries active))))}
      {:status :ordinary-work-eligible :ordinary-work-eligible? true})))

(defn- init-repo! [root]
  (.mkdirs (io/file root))
  (sh! root "git" "init" "-q")
  (sh! root "git" "config" "user.email" "harness@example.invalid")
  (sh! root "git" "config" "user.name" "WM Harness")
  (spit (io/file root "repair.txt") "before\n")
  (sh! root "git" "add" "repair.txt")
  (sh! root "git" "commit" "-q" "-m" "fixture base")
  root)

(defn fake-ports
  "Production-shaped local transport adapters. Author/reviewer/enact all
  derive their receipts from independently checked Git/filesystem evidence."
  [repo counters mode]
  (letfn [(sealed [request status payload]
            (let [order (:work-order request)]
              (coordinator/seal-port-receipt
               {:phase (:phase request) :injury-id (:injury-id order)
                :repair-id (:repair-id order) :attempt (:attempt order)
                :predecessor-pin (:predecessor-pin request)
                :status status :payload payload})))]
    {:author
     (fn [request]
       (swap! counters update :author (fnil inc 0))
       (if (= mode :author-failure)
         (sealed request :failed {:reason :dispatch-failed})
         (do (spit (io/file repo "repair.txt")
                   (str "repair=" (get-in request [:work-order :repair-id]) "\n"))
             (sh! repo "git" "add" "repair.txt")
             (sh! repo "git" "commit" "-q" "-m" "META self-heal repair")
             (sealed request :succeeded
                     {:patch-sha (str/trim
                                  (:out (sh! repo "git" "rev-parse" "HEAD")))
                      :worktree (.getCanonicalPath (io/file repo))
                      :git-status (:out (sh! repo "git" "status" "--porcelain"))}))))
     :review
     (fn [request]
       (swap! counters update :review (fnil inc 0))
       (let [patch (str/trim (:out (sh! repo "git" "rev-parse" "HEAD")))
             show (:out (sh! repo "git" "show" "--format=fuller" "--stat" patch))]
         (cond
           (= mode :review-rejection)
           (sealed request :rejected {:reason :acceptance-test-rejected})
           (= mode :review-substitution)
           (sealed request :succeeded {:patch-sha (apply str (repeat 40 "f"))
                                       :reviewer-sha patch})
           :else
           (sealed request :succeeded
                   {:patch-sha patch :reviewer-sha patch
                    :independent-inspection-sha
                    (:sha256 (sha256-pin (str repo "/repair.txt")))
                    :show-bytes (count show)}))))
     :enact
     (fn [request]
       (swap! counters update :enact (fnil inc 0))
       (let [patch (str/trim (:out (sh! repo "git" "rev-parse" "HEAD")))]
         (if (= mode :enact-failure)
           (sealed request :failed {:reason :reload-failed})
           (sealed request :succeeded
                   {:patch-sha patch :integration-commit patch
                    :retry-click "offline-retry-click-1"
                    :reload-evidence {:head patch
                                      :clean? (empty? (:out (sh! repo "git" "status"
                                                                "--porcelain")))}}))))
     :retry
     (fn [request]
       (swap! counters update :retry (fnil inc 0))
       (let [order (:work-order request)
             patch (str/trim (:out (sh! repo "git" "rev-parse" "HEAD")))
             base {:repair-id (:repair-id order) :patch-sha patch
                   :retry-click "offline-retry-click-1"
                   :outcome :grounded-change
                   :record-pin {:path "offline/retry-record.edn"
                                :sha256 (apply str (repeat 64 "9"))}}]
         (cond
           (= mode :retry-queued)
           (sealed request :queued base)
           (= mode :retry-failure)
           (sealed request :failed
                   (assoc base :new-cause-pin
                          {:path "offline/retry-failure.edn"
                           :sha256 (apply str (repeat 64 "8"))}))
           (= mode :retry-unattributed)
           (sealed request :succeeded (dissoc base :repair-id))
           :else (sealed request :succeeded base))))}))

(defn prepare-environment! [root]
  (let [cause (campaign-cause)
        record-path (str root "/campaign-cause.edn")
        ledger-path (str root "/injury-ledger.edn")
        repo (init-repo! (str root "/repair-repo"))]
    (write-form! record-path (:record cause))
    (let [ingested (store/ingest-record-file! ledger-path record-path)]
      {:root root :repo repo :record-path record-path :ledger-path ledger-path
       :ingestion ingested :durable (store/load-durable-ledger ledger-path)})))

(defn work-order-options [env]
  {:diagnosis-evidence-pin (sha256-pin (:record-path env))
   :acceptance-test {:id "campaign-failure-no-longer-reproduces"
                     :command ["git" "show" "--check" "HEAD"]
                     :expected {:exit 0}}
   :author "fake/wm-author" :reviewer "fake/wm-reviewer"
   :repair-reviewer "fake/wm-repair-reviewer"
   :author-session "fresh-author-session-1"
   :reviewer-session "fresh-reviewer-session-1"
   :repair-reviewer-session "fresh-repair-reviewer-session-1"
   :target-repository "futon2" :target-worktree (:repo env)})

(defn persist-checkpoint! [env result]
  (write-form! (:ledger-path env) (:ledger result))
  (let [tx-path (str (:root env) "/transaction.edn")]
    (write-form! tx-path (:transaction result))
    (assoc env :durable (store/load-durable-ledger (:ledger-path env))
           :transaction-path tx-path :transaction (:transaction result))))

(defn verify-checkpoint [ledger-path transaction-path]
  (let [durable (store/load-durable-ledger ledger-path)
        tx-read (store/read-one-form (slurp transaction-path))]
    {:status (if (and (= :loaded (:status durable))
                      (= :read (:status tx-read))
                      (= :valid (:status
                                 (coordinator/verify-transaction (:value tx-read)))))
               :valid :refused)
     :ledger-status (:status durable)
     :transaction-status (when (= :read (:status tx-read))
                           (:status (coordinator/verify-transaction (:value tx-read))))}))

(defn verify-in-fresh-jvm [ledger-path transaction-path]
  (let [result (shell/sh "clojure" "-M:test" "-m"
                         "futon2.aif.meta-self-heal-harness"
                         "verify-checkpoint" ledger-path transaction-path
                         :dir (System/getProperty "user.dir"))]
    {:exit (:exit result)
     :value (when (zero? (:exit result)) (edn/read-string (:out result)))}))

(defn run-success! [root]
  (let [env0 (prepare-environment! root)
        initial-arbitration (arbitrate (:durable env0))
        prepared (coordinator/prepare (:durable env0) (work-order-options env0))
        counters (atom {})
        ports (fake-ports (:repo env0) counters :success)]
    (loop [env (assoc env0 :transaction (:transaction prepared))
           remaining phases
           checkpoints []]
      (if-let [phase (first remaining)]
        (let [result (coordinator/coordinate
                      (:durable env) (:transaction env)
                      (assoc ports :stop-after phase))
              env' (persist-checkpoint! env result)]
          (recur env' (next remaining)
                 (conj checkpoints {:phase phase :status (:status result)
                                    :verify (verify-in-fresh-jvm
                                             (:ledger-path env')
                                             (:transaction-path env'))})))
        (let [result (coordinator/coordinate (:durable env) (:transaction env) ports)
              env' (persist-checkpoint! env result)]
          {:status (:status result) :ledger (:ledger result)
           :transaction (:transaction result) :repo (:repo env)
           :initial-arbitration initial-arbitration
           :final-arbitration (arbitrate (:durable env'))
           :checkpoints checkpoints :calls @counters
           :repair-id (get-in result [:transaction :work-order :repair-id])
           :patch-sha (get-in (coordinator/current-receipt-for
                               (:transaction result) :author)
                              [:payload :patch-sha])})))))

(defn -main [& args]
  (case (first args)
    "verify-checkpoint" (prn (verify-checkpoint (second args) (nth args 2)))
    "run" (prn (run-success! (second args)))
    (do (binding [*out* *err*]
          (println "usage: ... verify-checkpoint LEDGER TRANSACTION | run TMPDIR"))
        (System/exit 2))))
