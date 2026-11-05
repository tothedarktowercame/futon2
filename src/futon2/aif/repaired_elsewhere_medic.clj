(ns futon2.aif.repaired-elsewhere-medic
  "Medic disposition for an open injury repaired on the canonical branch."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [futon2.aif.action-identity :as action-identity]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-injury-store :as store]
            [futon2.aif.out-of-band-fix :as out-of-band]
            [futon2.aif.repair-obligation :as repair])
  (:import [java.time Instant]))

(def receipt-schema :wm/repaired-elsewhere-medic-receipt-v1)
(def verifier-name
  "futon2.aif.repaired-elsewhere-medic/retained-authority")

(defn- refuse [reason & [detail]]
  (merge {:status :refused :reason reason} detail))

(defn- sha-bytes [^bytes bs] (identity/sha256 bs))

(defn- read-pin [pin]
  (let [bs (java.nio.file.Files/readAllBytes (.toPath (io/file (:path pin))))]
    (when-not (= (:sha256 pin) (sha-bytes bs))
      (throw (ex-info "Pinned value changed" {:reason :repaired-elsewhere/pin-mismatch})))
    (edn/read-string (String. bs "UTF-8"))))

(defn retain-rerun!
  "Retain the caller's fresh production rerun value and return its pin."
  [acceptance-root value]
  (out-of-band/retain! acceptance-root "repaired-elsewhere-reruns" value))

(defn- git [repo & args]
  (apply shell/sh "git" "-C" repo args))

(defn- git-out [repo & args]
  (let [r (apply git repo args)]
    (when (zero? (:exit r)) (str/trim (:out r)))))

(defn- condition-holds? [value {:keys [op path expected]}]
  (and (= :path-equals op) (vector? path) (= expected (get-in value path))))

(defn- commit-evidence [repo commits detected-at]
  (when-not (and (seq commits) (every? string? commits))
    (throw (ex-info "Repair commits required" {:reason :repaired-elsewhere/commits-required})))
  (mapv
   (fn [commit]
     (let [sha (git-out repo "rev-parse" "--verify" (str commit "^{commit}"))
           ancestor? (and sha (zero? (:exit (git repo "merge-base" "--is-ancestor" sha "HEAD"))))
           authored (when sha
                      (some-> (git-out repo "show" "-s" "--format=%aI" sha) Instant/parse))
           paths (when sha
                   (some-> (git-out repo "diff-tree" "--no-commit-id" "--name-only" "-r" sha)
                           str/split-lines vec))]
       (cond
         (not ancestor?) (throw (ex-info "Repair commit is not on canonical HEAD"
                                        {:reason :repaired-elsewhere/commit-not-ancestor
                                         :commit commit}))
         (not (.isAfter ^Instant authored ^Instant detected-at))
         (throw (ex-info "Repair commit predates injury"
                         {:reason :repaired-elsewhere/commit-predates-injury :commit sha}))
         (not-any? #(str/starts-with? % "src/") paths)
         (throw (ex-info "Repair commit does not touch source"
                         {:reason :repaired-elsewhere/no-source-change :commit sha}))
         :else {:sha sha :authored-at (str authored) :source-paths
                (filterv #(str/starts-with? % "src/") paths)})))
   commits))

(defn- make-candidate [current receipt-pin receipt]
  {:kind :genuine-repair
   :injury-id (:injury-id current)
   :entry-id (:entry-id current)
   :predecessor-entry-id (:predecessor-entry-id current)
   :patch-id (get-in receipt [:commits 0 :sha])
   :reviewer "DarkTower.WarMachine.RepairedElsewhere"
   :evaluator verifier-name
   :evidence-pin (:sha256 receipt-pin)
   :cause-record-id (:cause-record-id current)
   :cause-pin (:cause-pin current)
   :acceptance-contract (:sha256 receipt-pin)
   :pre-replay-receipt (:cause-pin current)
   :post-replay-receipt (get-in receipt [:rerun-pin :sha256])
   :negative-control-receipt (:cause-pin current)
   :source-revision (get-in receipt [:commits 0 :sha])
   :original-acceptance-repaired? true})

(defn retained-authority
  "Verify one exact retained repaired-elsewhere operation."
  [{:keys [operation candidate evidence current]}]
  (try
    (let [pin (:authority-receipt-pin operation)
          receipt (read-pin pin)
          expected (make-candidate current pin receipt)]
      (and (= receipt-schema (:schema receipt))
           (= :checked (:status receipt))
           (= expected candidate evidence)
           (= pin (:authority-receipt-pin operation))
           (= (:expected-ledger-pin operation) (:ledger-pin (:ledger receipt)))
           (= (action-identity/digest [:repaired-elsewhere-medic
                                       (:ledger-pin (:ledger receipt)) expected pin])
              (:operation-id operation))))
    (catch Throwable _ false)))

(defn close!
  "Mask one exact open occurrence after a checked outside repair, then dismiss
  its joined repair finding. No transition state is manufactured."
  [{:keys [ledger-path acceptance-root repair-root injury-id expected-entry-pin
           repository commits rerun-pin healthy-condition finding-id actor reason
           normal-authority]}]
  (try
    (let [loaded (store/load-durable-ledger ledger-path normal-authority)
          history (:ledger loaded)
          active (when (= :loaded (:status loaded))
                   (ledger/effective-active-injuries history normal-authority))
          entry (get-in active [:injuries injury-id])]
      (cond
        (not= :loaded (:status loaded)) loaded
        (not= :open (:state entry)) (refuse :repaired-elsewhere/not-open)
        (not= expected-entry-pin (:entry-pin entry)) (refuse :repaired-elsewhere/stale-occurrence)
        :else
        (let [current (ledger/medic-current-occurrence history injury-id)
              cause-pin {:path (:cause-record-id current) :sha256 (:cause-pin current)}
              cause (read-pin cause-pin)
              detected-at (Instant/parse (:startedAt cause))
              rerun (read-pin rerun-pin)
              _ (when-not (condition-holds? rerun healthy-condition)
                  (throw (ex-info "Rerun did not establish the typed condition"
                                  {:reason :repaired-elsewhere/condition-not-established})))
              commit-info (commit-evidence repository commits detected-at)
              receipt {:schema receipt-schema :status :checked :injury-id injury-id
                       :occurrence (select-keys current [:entry-id :predecessor-entry-id
                                                        :cause-record-id :cause-pin])
                       :expected-entry-pin expected-entry-pin :repository repository
                       :commits commit-info :rerun-pin rerun-pin
                       :healthy-condition healthy-condition :checked-at (str (Instant/now))
                       :ledger {:ledger-pin (:ledger-pin history)}}
              receipt-pin (out-of-band/retain! acceptance-root
                                               "repaired-elsewhere-medic" receipt)
              c (make-candidate current receipt-pin receipt)
              operation {:operation-id
                         (action-identity/digest
                          [:repaired-elsewhere-medic (:ledger-pin history) c receipt-pin])
                         :expected-ledger-pin (:ledger-pin history)
                         :request {:candidate c :evidence c}
                         :authority-receipt-pin receipt-pin}
              authority (out-of-band/scoped-medic-authority
                         normal-authority operation retained-authority)
              attribution {:medic-disposition :repaired-elsewhere
                           :fixed-by (get-in receipt [:commits 0 :sha])
                           :out-of-band-receipt-pin receipt-pin
                           :medic-stamp {:schema :wm/out-of-band-medic-stamp-v1
                                         :verifier verifier-name :receipt-pin receipt-pin}}
              written (store/transact-medic-disposition!
                       ledger-path receipt-pin operation attribution authority)]
          (if-not (= :appended (:status written))
            written
            (let [dismissal (repair/dismiss-repaired-elsewhere!
                             repair-root finding-id
                             {:authority verifier-name :reason reason :actor actor
                              :commit (get-in receipt [:commits 0 :sha])})]
              {:status :discharged :injury-id injury-id :ledger-write written
               :receipt-pin receipt-pin :dismissal dismissal})))))
    (catch clojure.lang.ExceptionInfo e
      (refuse (or (:reason (ex-data e)) :repaired-elsewhere/refused)
              (dissoc (ex-data e) :reason)))
    (catch Throwable t
      (refuse :repaired-elsewhere/failed {:detail (ex-message t)}))))
