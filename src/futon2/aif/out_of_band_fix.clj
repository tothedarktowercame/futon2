(ns futon2.aif.out-of-band-fix
  "Cause-specific fixing attribution beside, never replacing, enacted history."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [futon2.aif.action-identity :as action-identity]
            [futon2.aif.load-identity :as identity]))

(defn- refuse! [reason & [detail]]
  (throw (ex-info "Out-of-band fixing attribution refused"
                  {:status :refused :reason reason :detail detail})))
(defn- sha [s] (identity/sha256 (.getBytes s "UTF-8")))
(defn read-pin [pin]
  (let [bytes (java.nio.file.Files/readAllBytes (.toPath (io/file (:path pin))))]
    (when-not (= (:sha256 pin) (identity/sha256 bytes))
      (refuse! :out-of-band/pin-mismatch pin))
    (String. bytes "UTF-8")))
(defn- git [repo & args] (apply shell/sh "git" "-C" repo args))
(defn- checked-git [repo & args]
  (let [r (apply git repo args)]
    (when-not (zero? (:exit r)) (refuse! :out-of-band/git-failed (select-keys r [:exit :err])))
    (str/trim (:out r))))
(defn retain! "Atomically retain content-addressed evidence; never changes a work order."
  [root kind value]
  (let [text (str (pr-str value) "\n")
        dir (.toPath (io/file root kind))
        _ (java.nio.file.Files/createDirectories dir (make-array java.nio.file.attribute.FileAttribute 0))
        target (.resolve dir (str (sha text) ".edn"))
        tmp (java.nio.file.Files/createTempFile dir ".receipt-" ".tmp"
              (make-array java.nio.file.attribute.FileAttribute 0))]
    (try
      (spit (.toFile tmp) text)
      (java.nio.file.Files/move tmp target
        (into-array java.nio.file.CopyOption [java.nio.file.StandardCopyOption/ATOMIC_MOVE
                                              java.nio.file.StandardCopyOption/REPLACE_EXISTING]))
      {:path (str target) :sha256 (sha text)}
      (finally (java.nio.file.Files/deleteIfExists tmp)))))

(defn artifact-for [root injury]
  (let [matches (for [f (or (.listFiles (io/file root "out-of-band")) [])
                      :when (and (.isFile f) (str/ends-with? (.getName f) ".edn"))
                      :let [text (slurp f) a (edn/read-string text)]
                      :when (= injury (:injury-id a))]
                  (do (when-not (= (.getName f) (str (sha text) ".edn"))
                        (refuse! :out-of-band/artifact-pin-mismatch))
                      {:artifact a :pin {:path (.getCanonicalPath f) :sha256 (sha text)}}))]
    (when (> (count matches) 1) (refuse! :out-of-band/ambiguous-artifact))
    (first matches)))

(defn retain-artifact!
  "Admit cause-specific evidence beside an enacted order. A green current run
  is expected for this kind only; ordinary acceptance admission is unchanged."
  [root artifact]
  (when-not (and (= :wm/out-of-band-acceptance-v1 (:schema artifact))
                 (string? (:injury-id artifact))
                 (keyword? (:failure-kind artifact))
                 (keyword? (:failure-stage artifact))
                 (string? (:fixed-by artifact))
                 (re-matches #"[0-9a-f]{40}" (:fixed-by artifact))
                 (string? (get-in artifact [:acceptance-test :source :text])))
    (refuse! :out-of-band/invalid-artifact))
  (read-pin (:historical-pin artifact))
  (when-let [existing (artifact-for root (:injury-id artifact))]
    (when-not (= artifact (:artifact existing))
      (refuse! :out-of-band/artifact-already-present)))
  (retain! root "out-of-band" artifact))

(defn report [run namespace]
  (let [log (read-pin (:run-pin run))
        nses (mapv second (re-seq #"(?m)^Testing\s+(\S+)\s*$" log))
        m (re-find #"(?m)^Ran (\d+) tests? containing (\d+) assertions\.\s*\n(\d+) failures?, (\d+) errors?\." log)]
    (when-not (and (= :ran (:status run)) (= [namespace] nses) m)
      (refuse! :out-of-band/test-did-not-load))
    {:tests (parse-long (nth m 1)) :assertions (parse-long (nth m 2))
     :failures (parse-long (nth m 3)) :errors (parse-long (nth m 4))}))
(defn qualifies? [historical current counterfactual]
  (and historical (pos? (:tests current 0))
       (zero? (:failures current -1)) (zero? (:errors current -1))
       (pos? (:tests counterfactual 0)) (pos? (:failures counterfactual 0))
       (zero? (:errors counterfactual -1))))

(def checked-medic-verifier
  "futon2.aif.out-of-band-fix/checked-medic-authority")

(defn checked-medic-operation
  "Translate a checked out-of-band receipt into the existing genuine-repair
  medic payload. The correction excludes the exact open occurrence; it does
  not manufacture claim, author, review, or enact transitions."
  [ledger open-entry artifact-pin receipt-pin receipt]
  (let [candidate {:kind :genuine-repair
                   :injury-id (:injury-id open-entry)
                   :entry-id (str (:ordinal open-entry))
                   :predecessor-entry-id nil
                   :patch-id (:fixed-by receipt)
                   :reviewer "DarkTower.WarMachine.OutOfBandFix"
                   :evaluator checked-medic-verifier
                   :evidence-pin (:sha256 receipt-pin)
                   :cause-record-id (get-in open-entry [:cause-record-pin :path])
                   :cause-pin (get-in open-entry [:cause-record-pin :sha256])
                   :acceptance-contract (:sha256 artifact-pin)
                   :pre-replay-receipt (get-in receipt [:historical-pin :sha256])
                   :post-replay-receipt (get-in receipt [:current :run-pin :sha256])
                   :negative-control-receipt
                   (get-in receipt [:counterfactual :run-pin :sha256])
                   :source-revision (:base-sha receipt)
                   :original-acceptance-repaired? true}
        request {:candidate candidate :evidence candidate}
        authority-receipt-pin receipt-pin
        operation-id (action-identity/digest
                      [:open-out-of-band-medic
                       {:ledger-pin (:ledger-pin ledger)
                        :request request :authority-receipt-pin authority-receipt-pin}])]
    {:operation {:operation-id operation-id
                 :expected-ledger-pin (:ledger-pin ledger)
                 :request request
                 :authority-receipt-pin authority-receipt-pin}
     :attribution {:medic-disposition :out-of-band-fix
                   :fixed-by (:fixed-by receipt)
                   :out-of-band-receipt-pin receipt-pin
                   :medic-stamp {:schema :wm/out-of-band-medic-stamp-v1
                                 :verifier checked-medic-verifier
                                 :receipt-pin receipt-pin}}}))

(defn checked-medic-authority
  "Authority predicate for one exact persisted checked receipt and operation."
  [expected-operation receipt-pin receipt]
  (fn [{:keys [operation candidate evidence]}]
    (try
      (let [persisted (edn/read-string (read-pin receipt-pin))]
        (and (= :checked (:status receipt))
             (= receipt persisted)
             (= expected-operation operation)
             (= candidate evidence)
             (= candidate (get-in operation [:request :candidate]))
             (= (:injury-id receipt) (:injury-id candidate))
             (= receipt-pin (:authority-receipt-pin operation))
             (qualifies? (:historical-cause-checked? receipt)
                         (:current-report receipt)
                         (:counterfactual-report receipt))))
      (catch Throwable _ false))))

(defn retained-checked-medic-authority
  "Re-establish the named medic verifier from the persisted checked receipt.
  This is the authority used when validating a ledger after process restart."
  [{:keys [ledger operation current candidate evidence]}]
  (try
    (let [receipt-pin (:authority-receipt-pin operation)
          receipt (edn/read-string (read-pin receipt-pin))
          expected {:kind :genuine-repair
                    :injury-id (:injury-id current)
                    :entry-id (:entry-id current)
                    :predecessor-entry-id (:predecessor-entry-id current)
                    :patch-id (:fixed-by receipt)
                    :reviewer "DarkTower.WarMachine.OutOfBandFix"
                    :evaluator checked-medic-verifier
                    :evidence-pin (:sha256 receipt-pin)
                    :cause-record-id (:cause-record-id current)
                    :cause-pin (:cause-pin current)
                    :acceptance-contract (get-in receipt [:artifact-pin :sha256])
                    :pre-replay-receipt (get-in receipt [:historical-pin :sha256])
                    :post-replay-receipt (get-in receipt [:current :run-pin :sha256])
                    :negative-control-receipt
                    (get-in receipt [:counterfactual :run-pin :sha256])
                    :source-revision (:base-sha receipt)
                    :original-acceptance-repaired? true}
          request {:candidate expected :evidence expected}
          expected-id (action-identity/digest
                       [:open-out-of-band-medic
                        {:ledger-pin (:ledger-pin ledger)
                         :request request :authority-receipt-pin receipt-pin}])]
      (and (= :checked (:status receipt))
           (= expected candidate evidence)
           (= (:injury-id receipt) (:injury-id candidate))
           (= request (:request operation))
           (= expected-id (:operation-id operation))
           (= (:ledger-pin ledger) (:expected-ledger-pin operation))
           (qualifies? (:historical-cause-checked? receipt)
                       (:current-report receipt)
                       (:counterfactual-report receipt))))
    (catch Throwable _ false)))

(defn scoped-medic-authority
  "Keep a pending operation's receipt authority local to that operation.
  Every previously retained medic operation is checked only by the normal
  authority that was resolved before this append."
  [normal-authority expected-operation specific-authority]
  (let [expected-id (:operation-id expected-operation)]
    (fn [{:keys [operation] :as context}]
      (try
        (true? ((if (= expected-id (:operation-id operation))
                  specific-authority
                  normal-authority)
                context))
        (catch Throwable _ false)))))

(defn compute!
  "Compute the three evidence checks in tooling processes via the normal
  acceptance port. The caller must persist this result, including refusals.
  Failed worktrees are retained. No work order or injury ledger is changed."
  [root order run-acceptance]
  (try
    (when-let [{:keys [artifact pin]} (artifact-for root (:injury-id order))]
      (let [repo (:target-repository order)
            base (checked-git repo "rev-parse" "HEAD")
            fix (:fixed-by artifact)
            historical-pin (:historical-pin artifact)
            historical (edn/read-string (read-pin historical-pin))
            test (:acceptance-test artifact)
            namespace (get-in test [:source :namespace])
            source-sha (sha (get-in test [:source :text]))]
        (when-not (and (= :wm/out-of-band-acceptance-v1 (:schema artifact))
                       (= historical-pin (:cause-record-pin order))
                       (= (:failure-kind artifact) (get-in historical [:failure :kind]))
                       (= (:failure-stage artifact) (get-in historical [:failure :stage]))
                       (string? fix) (re-matches #"[0-9a-f]{40}" fix))
          (refuse! :out-of-band/historical-cause-mismatch))
        (when-not (zero? (:exit (git repo "merge-base" "--is-ancestor" fix base)))
          (refuse! :out-of-band/fix-not-reachable))
        (when-not (= "" (checked-git repo "status" "--porcelain" "--untracked-files=no"))
          (refuse! :out-of-band/canonical-dirty))
        (let [paths (str/split-lines (checked-git repo "diff" "--name-only" (str fix "^") fix "--" "src/"))
              _ (when (or (empty? paths) (= [""] paths))
                  (refuse! :out-of-band/no-production-paths))
              patch (:out (git repo "diff" "--binary" (str fix "^") fix "--" "src/"))
              current (run-acceptance (assoc test :execution-repository repo))
              current-report (report current namespace)
              _ (when-not (and (= :isolated (get-in current [:store-isolation :status]))
                               (zero? (:errors current-report))
                               (zero? (:failures current-report))
                               (pos? (:tests current-report)))
                  (refuse! :out-of-band/current-not-green {:run current :report current-report}))
              worktree (str (System/getProperty "user.home") "/worktrees/futon2/out-of-band/" (java.util.UUID/randomUUID))
              _ (io/make-parents (str worktree "/placeholder"))
              _ (checked-git repo "worktree" "add" "--detach" worktree base)
              applied (git worktree "apply" "-R" "--3way" "-" :in patch)
              conflicts (checked-git worktree "diff" "--name-only" "--diff-filter=U")]
          (when (or (not (zero? (:exit applied))) (seq conflicts))
            (refuse! :out-of-band/counterfactual-revert-conflict
                     {:worktree worktree :paths paths :apply applied :conflicts conflicts}))
          (let [deps (io/file worktree "deps.edn")
                _ (when (.isFile deps)
                    (spit deps (pr-str
                     (walk/postwalk
                      (fn [x]
                        (if (and (map? x) (string? (:local/root x)))
                          (update x :local/root
                                  #(let [f (io/file %)]
                                     (.getCanonicalPath
                                      (if (.isAbsolute f) f (io/file repo %))))) x))
                      (edn/read-string (slurp deps))))))
                counterfactual (run-acceptance (assoc test :execution-repository worktree))
                counter-report (report counterfactual namespace)
                evidence {:schema :wm/out-of-band-fix-receipt-v1
                          :artifact-pin pin :injury-id (:injury-id order)
                          :fixed-by fix :base-sha base :historical-pin historical-pin
                          :historical-cause-checked? true :acceptance-source-sha256 source-sha
                          :production-paths paths :mechanism :git-apply-reverse-threeway
                          :worktree worktree :apply-exit (:exit applied)
                          :dependency-path-adjustment :absolute-local-roots-only
                          :patch-sha256 (sha patch) :current current :current-report current-report
                          :counterfactual counterfactual :counterfactual-report counter-report}]
            (when-not (= base (checked-git repo "rev-parse" "HEAD"))
              (refuse! :out-of-band/canonical-head-moved evidence))
            (when-not (and (= :isolated (get-in counterfactual [:store-isolation :status]))
                           (qualifies? true current-report counter-report))
              (refuse! :out-of-band/counterfactual-not-causal evidence))
            (assoc evidence :status :checked)))))
    (catch clojure.lang.ExceptionInfo e (ex-data e))
    (catch Exception e {:status :refused :reason :out-of-band/evidence-unavailable
                        :detail (ex-message e)})))
