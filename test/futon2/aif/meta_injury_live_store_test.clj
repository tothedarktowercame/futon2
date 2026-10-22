(ns futon2.aif.meta-injury-live-store-test
  "The live meta-injury store opened 2026-10-22: every production default
  resolves under the fresh live root, none resolves under the frozen archive
  directory, and a first append to a non-existent ledger starts a new chain
  with no predecessor reference into any old chain."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :as t :refer [deftest is testing]]
            [futon2.aif.meta-injury-store :as store]
            [futon2.aif.meta-self-heal-production :as production]))

(def live-root "/home/joe/code/futon2/data/wm-meta-injuries-live-2026-10-22")
(def live-dir-name "wm-meta-injuries-live-2026-10-22")
(def archived-root "/home/joe/code/futon2/data/wm-meta-injuries")

(def production-namespaces
  "Source files whose defaults (or prose) may not name the archived root."
  ["src/futon2/aif/meta_injury_store.clj"
   "src/futon2/aif/meta_injury_ledger.clj"
   "src/futon2/aif/meta_terminal_injury.clj"
   "src/futon2/aif/meta_self_heal_production.clj"
   "src/futon2/aif/full_loop_runner.clj"
   "src/futon2/aif/full_loop_runtime.clj"])

(deftest every-live-default-resolves-under-the-fresh-live-root
  (testing "the four live defaults share the single live root"
    (is (= live-root store/default-root))
    (is (= (str live-root "/ledger.edn") production/default-ledger-path))
    (is (= (str live-root "/transaction.edn") production/default-transaction-path))
    (is (= (str live-root "/diagnoses") production/default-diagnosis-root))
    (is (= (str live-root "/acceptances") production/default-acceptance-root)))
  (testing "no live default resolves under the archived root"
    (doseq [[_ default] {:root store/default-root
                     :ledger production/default-ledger-path
                     :transaction production/default-transaction-path
                     :diagnosis production/default-diagnosis-root
                     :acceptance production/default-acceptance-root}]
      (is (not (or (= default archived-root) (str/starts-with? default (str archived-root "/"))))
          (str default " must not resolve under the archived store")))
    (is (not= archived-root store/default-root))))

(deftest no-production-source-names-the-archived-store
  (doseq [source production-namespaces
          :let [text (slurp (io/file source))]]
    (testing source
      (is (every? #(str/starts-with? % live-dir-name)
                  (map #(subs text %)
                       (take-while #(>= % 0)
                                   (iterate #(.indexOf text "wm-meta-injuries" (inc %))
                                            (.indexOf text "wm-meta-injuries")))))
          "the only occurrence of wm-meta-injuries in production source is the
           live-root literal wm-meta-injuries-live-2026-10-22"))))

(deftest first-append-to-a-non-existent-ledger-starts-a-fresh-chain
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "wm-live-store" (make-array java.nio.file.attribute.FileAttribute 0)))
        ledger-path (str (.getPath dir) "/nested/ledger.edn")
        record-path (str (.getPath dir) "/run.edn")
        record {:run/id "run-fresh" :click/id "click-fresh"
                :failure {:kind :author-dispatch-failed :stage :author-dispatch}}]
    (testing "a missing ledger reads as empty before anything is written"
      (let [loaded (store/load-ledger ledger-path)]
        (is (= :loaded (:status loaded)))
        (is (= :new (:source loaded)))
        (is (empty? (get-in loaded [:ledger :entries])))))
    (spit record-path (pr-str record))
    (let [result (store/ingest-record-file! ledger-path record-path)
          ledger (store/load-ledger ledger-path)]
      (testing "the locked atomic write creates the ledger on first append"
        (is (= :appended (:status result)))
        (is (= :atomic-locked-one-form (:durability result)))
        (is (.isFile (io/file ledger-path))))
      (testing "the fresh ledger has one entry with no predecessor chain"
        (is (= :loaded (:status ledger)))
        (is (= :disk (:source ledger)))
        (let [entries (get-in ledger [:ledger :entries])]
          (is (= 1 (count entries)))
          (is (= 1 (:ordinal (first entries))))
          (is (nil? (:predecessor (first entries))))
          (is (nil? (:injury-predecessor (first entries)))
              "the first live transition must carry no predecessor reference"))))))

(deftest default-check-rejects-a-ledger-pointing-back-to-the-archive
  (let [reports (atom [])]
    (with-redefs [production/default-ledger-path (str archived-root "/ledger.edn")
                  t/report #(swap! reports conj %)]
      ((:test (meta #'every-live-default-resolves-under-the-fresh-live-root))))
    (is (some #(= :fail (:type %)) @reports)
        "the actual default check must fail when a default points into the archive")))
