(ns futon2.report.habit-fold-call-test
  "WM-HABIT-FOLD-CALL-I: at each selection the tick folds the flights'
  increment receipts (<machine-interpretations-dir>/flights/*.edn,
  enactment-fold-source) and passes the fold to select-action-cascades as
  :enactment-fold; a fold already in judge-opts wins. Before it, no fold was
  passed and every live selection read {:status :absent :reason
  :no-enactment-fold}, uniform E (WM-HABIT-FOLD-WIRE-D, futon2 655bbced).

  The fold enters in judge, at the options judge hands
  select-and-record-cascade!, and reaches the selection through
  cascade-decision and cascade-decision-admitted, whose options map for
  select-action-cascades names the keys it passes. The test runs judge
  with every store it reads pointed at a temp dir, captures the options
  judge hands select-and-record-cascade!, and runs that same var, the real
  one, over the cascade-decision fixture's tick-1 family with the captured
  :enactment-fold, reading the selection's habit read. The pre-fix judge is
  materialised from git (655bbced) under a renamed namespace, never loaded
  over the real one."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.enactment-habit :as eh]
            [futon2.aif.mission-registry :as mr]
            [futon2.aif.scoring-input-receipts :as receipts]
            [futon2.aif.ticket-queue :as ticket-queue]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.policy-prefix-admission :as prefix-admission]
            [futon2.report.war-machine :as wm] [futon2.aif.wm.cascade-decision :as wm-cd]
            [futon2.test-support.reading-route :as route]))

(use-fixtures :once hermetic/with-hermetic-stores route/with-empty-learning-ledger)

(defn- tmp-dir []
  (str (java.nio.file.Files/createTempDirectory
        "habit-fold-call" (make-array java.nio.file.attribute.FileAttribute 0))))

(def ^:private fx
  "One reading-family field for the namespace (Q5 migration 2026-10-15):
  the selection E reaches is the production reading-family decision."
  (delay (route/fixture)))

(def ^:private pkey
  "The structural history key of the first policy the decision compares:
  the key Prefix-F, the census, the flight writer and now E all share."
  (delay (first (vals (route/policy-keys (:decision (route/decide @fx)))))))

(defn- receipt [click & [verdict attempts key]]
  (eh/increment {:click click :candidate :cas/a :attempts (or attempts [{:pattern "p/a"}])}
                (or key @pkey) (or verdict [])))

(defn- write-flight! [dir id receipts]
  (let [f (io/file dir "flights" (str id ".edn"))]
    (io/make-parents f)
    (spit f (pr-str {:plan {} :flight {:flight/id id
                                       :enactments (mapv (fn [r] {:click-id (first (:record-id r))
                                                                  :wc {:verdict []} :increment r})
                                                         receipts)}}))
    (.getCanonicalPath f)))

(defn- sha256-file [path]
  (apply str (map #(format "%02x" (bit-and 0xff %))
                  (.digest (java.security.MessageDigest/getInstance "SHA-256")
                           (java.nio.file.Files/readAllBytes (.toPath (io/file path)))))))

(defn- judge-selection-opts
  "The options NS's judge hands select-and-record-cascade!, over the
  machine-interpretations dir STORE and JUDGE-OPTS."
  [ns store judge-opts]
  (let [captured (atom nil)
        tmp (tmp-dir)]
    (with-redefs-fn {#'mr/load-missions (fn [& _] {:missions []})
                     #'mr/load-tickets (fn [& _] {:tickets []})
                     #'wm-cd/select-and-record-cascade!
                     (fn [_ opts] (reset! captured opts) (throw (ex-info "stop" {::stop true})))}
      #(try ((ns-resolve ns 'judge) {} (merge {:cascade-sources-dir tmp :cascade-proposals-dir tmp
                                                :repair-obligations-root tmp
                                                :machine-interpretations-dir store
                                                :ticket-queue ticket-queue/empty-declaration}
                                               judge-opts))
            (catch clojure.lang.ExceptionInfo e
              (when-not (::stop (ex-data e)) (throw e)))))
    @captured))

(defn- one-selection
  "judge in NS over STORE, then the production reading-family decision with
  the fold judge passed: the joint selection's habit-read receipt, the
  selection law's :e-source, the certificate rows and the selector calls."
  ([ns store] (one-selection ns store {}))
  ([ns store judge-opts]
   (let [opts (judge-selection-opts ns store judge-opts)
         reads (atom [])
         r (binding [receipts/*habit-reads* reads]
             (route/decide @fx 1 (merge {:cascade-habit-path (str (io/file (tmp-dir) "absent.edn"))}
                                        (select-keys opts [:enactment-fold]))))
         d (:decision r)]
     {:opts opts
      :read (:receipt (first (filter #(contains? #{:joint-selection :selection-scoring} (:purpose %))
                                     @reads)))
      :e-source (get-in d [:selection-law :e-source])
      :rows (get-in d [:selection-certificate :candidates])
      :selector-calls (:selector-calls r)})))

(defn- row-for [rows key]
  (some #(when (= key (prefix-admission/candidate-key (:id %))) %) rows))

(def pre-fix-ns
  (delay
    (let [{:keys [exit out err]} (sh/sh "git" "show" "655bbced:scripts/futon2/report/war_machine.clj")
          renamed 'futon2.report.war-machine-at-655bbced]
      (when-not (zero? exit) (throw (ex-info "git show failed" {:err err})))
      (clojure.lang.Compiler/load
       (java.io.StringReader. (str/replace-first out "(ns futon2.report.war-machine" (str "(ns " renamed)))
       "futon2/report/war_machine_at_655bbced.clj" "war_machine_at_655bbced.clj")
      (the-ns renamed))))

(def live-ns (delay (:ns (meta #'wm/judge))))

(deftest one-increment-receipt-reaches-selection
  (let [store (tmp-dir)
        path (write-flight! store "flight-a" [(receipt "click-1")])
        {:keys [read e-source rows selector-calls]} (one-selection @live-ns store)
        mine (row-for rows @pkey)
        other (first (remove #(= mine %) rows))]
    (is (= 1 selector-calls))
    (is (= :present (:status read)))
    (is (= {:source :enactment-fold :records 1 :samples 1 :uniform false} e-source))
    (testing "the one selector consumed the folded E under the structural key"
      (is (= {:source :enactment-fold :policy-key @pkey :count 1}
             (select-keys (:habit-provenance mine) [:source :policy-key :count])))
      (is (= 0 (get-in other [:habit-provenance :count])))
      (is (> (:habit mine) (:habit other)) "history raises E for its own policy only"))
    (is (= [{:path path :sha256 (sha256-file path) :receipts 1}]
           (get-in read [:state :folded-from :read]))
        "the record E was counted from is named on the habit read")))

(deftest no-flight-records-is-present-with-no-samples
  (let [{:keys [read e-source]} (one-selection @live-ns (tmp-dir))]
    (is (= :present (:status read)) "a fold was passed: not :absent :no-enactment-fold")
    (is (= 0 (get-in read [:state :samples])))
    (is (= {:absent :no-flights-dir} (get-in read [:state :folded-from :dir-status])))
    (is (= {:source :enactment-fold :records 0 :samples 0 :uniform true} e-source))))

(deftest what-does-not-count
  (let [store (tmp-dir)
        _ (write-flight! store "flight-a" [(receipt "click-1")
                                           (receipt "click-2" ["W_c: the grain attempt names no G_c pass"])
                                           (receipt "click-3" [] [{:pattern :p/outside}])])
        _ (write-flight! store "flight-b" [(receipt "click-1")])
        bad (io/file store "flights" "flight-c.edn")
        _ (spit bad "{:flight {:enactments [")
        {:keys [read e-source]} (one-selection @live-ns store)
        from (get-in read [:state :folded-from])]
    (is (= 1 (:records e-source)) "one [click candidate] counted, once")
    (is (= 1 (:samples e-source)) ":delta 0 (W_c failed) and the refused receipt count nothing")
    (is (= 1 (count (get-in read [:state :duplicates]))) "the repeat is listed, not counted")
    (is (= [3 1] (mapv :receipts (:read from))))
    (is (= [{:path (.getCanonicalPath bad) :reason :unparseable}]
           (mapv #(select-keys % [:path :reason]) (:unread from)))
        "noted, and the rest folded")))

(deftest the-pre-fix-judge-passed-no-fold
  ;; the bad case: judge at 655bbced over the same flight record
  (let [store (tmp-dir)
        _ (write-flight! store "flight-a" [(receipt "click-1")])
        {:keys [opts read e-source]} (one-selection @pre-fix-ns store)]
    (is (not (contains? opts :enactment-fold)))
    (is (= {:status :absent :reason :no-enactment-fold} (select-keys read [:status :reason])))
    (is (= {:source :enactment-fold :records 0 :samples 0 :uniform true} e-source))))

(deftest a-fold-in-judge-opts-wins
  (let [store (tmp-dir)
        _ (write-flight! store "flight-a" [(receipt "click-1")])
        given (eh/fold nil [(receipt "click-9") (receipt "click-8")])
        {:keys [read e-source]} (one-selection @live-ns store {:enactment-fold given})]
    (is (= 2 (:records e-source)))
    (is (nil? (get-in read [:state :folded-from])) "the files were not read")))
