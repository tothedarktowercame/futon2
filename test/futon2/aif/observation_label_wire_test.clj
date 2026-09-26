(ns futon2.aif.observation-label-wire-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.cascade-sources :as sources]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.observation-admission :as admission]
            [futon2.aif.observation-labels :as labels]
            [futon2.aif.observation-label-store :as store]
            [futon2.aif.observation-label-wire :as wire])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def ^:dynamic *dir* nil)
(use-fixtures :each
  (fn [f]
    (let [dir (.toFile (Files/createTempDirectory "label-wire-" (make-array FileAttribute 0)))]
      (try (binding [*dir* dir] (f))
           (finally (doseq [file (reverse (file-seq dir))] (io/delete-file file true)))))))

(defn- path [] (str (io/file *dir* "labels.edn")))
(defn- live-provenance []
  (let [file (io/file "/home/joe/code/futon3c/holes/labs/M-wm-wiring/spike/tick-run-record-2026-09-26-flight-7f89646a-click-1.edn")
        bytes (Files/readAllBytes (.toPath file))
        digest (identity/sha256 bytes)]
    (is (= "a8e04fb97e58808e8fabdb4ab771f3c414b4181ef82dac336729dd472a18d816" digest))
    (when-not (= "a8e04fb97e58808e8fabdb4ab771f3c414b4181ef82dac336729dd472a18d816" digest)
      (throw (ex-info "Live fixture changed" {:sha256 digest})))
    (:declaration-reads (edn/read-string (String. bytes "UTF-8")))))

(defn- key-of [r]
  (admission/label-key ((case (:check r) :C3 labels/c3-subject :C4 labels/c4-subject)
                       r (:mechanism-sha (labels/loaded-identities)))))

(deftest live-check-receipts-reach-the-store
  (let [p (path) provenance (live-provenance) results (wire/check-results provenance)
        receipt (wire/record-declaration-reads! p provenance)
        first-snapshot (store/snapshot p)
        envelope (:envelope first-snapshot)
        counts (frequencies (map key-of results))
        by-key (into {} (map (juxt key-of identity) results))
        verdicts (frequencies (for [[_k label] (:labels envelope)]
                               (if (= (:recorded label) (= :present (:admitted label))) :agree :disagree)))
        insufficient (filter #(= :refused (:last-outcome %)) (vals (:seen envelope)))]
    (is (= 34 (count results) (:results receipt)))
    (is (= 10 (:occurrences receipt)))
    (is (true? (:initialized receipt)))
    (is (= :recorded (:status receipt)))
    (is (= (set (keys counts)) (set (keys (:seen envelope)))))
    (is (= (:sha256 first-snapshot) (:snapshot-sha256 receipt)))
    (doseq [[k label] (:labels envelope)]
      (let [r (get by-key k)
            s ((case (:check r) :C3 labels/c3-subject :C4 labels/c4-subject)
               r (:mechanism-sha (labels/loaded-identities)))
            adj (binding [labels/*code-sha* (:code-sha (labels/loaded-identities))]
                  ((case (:check r) :C3 labels/c3-recompute :C4 labels/c4-recompute) s))]
        (is (= (:observed r) (:recorded label)))
        (is (= (:finding adj) (:admitted label)))))
    (doseq [seen insufficient] (is (= :no-label (:last-refusal seen))))
    (let [again (wire/record-declaration-reads! p provenance)
          e2 (:envelope (store/snapshot p))]
      (is (false? (:initialized again)))
      (is (= (:labels envelope) (:labels e2)))
      (is (= (- (count results) (:refused again)) (:skipped again)))
      (is (zero? (:written again)))
      ;; Five keys occur twice in this actual provenance. Store :times counts
      ;; results, not ticks: replay doubles each key's observed multiplicity.
      (doseq [[k n] counts]
        (is (= (* 2 n) (get-in e2 [:seen k :times])))))
    (println "LIVE-LABEL-WIRE" (pr-str {:results (count results) :subjects (count counts)
                                        :receipt receipt :verdicts verdicts
                                        :insufficient (count insufficient)}))))

(deftest unsupported-and-refused-checks-are-counted-not-labelled
  (let [provenance (sources/provenance
                    [{:observations {:results {:other {:check :C5 :observed true :evidence {:entry "x"}}}
                                     :refused {:bad {:status :missing :kind :no-locator :data {:check :C4}}}}}])
        receipt (wire/record-declaration-reads! (path) provenance)]
    (is (empty? (wire/check-results provenance)))
    (is (= {:C5 1 :C4 1} (:dropped receipt)))
    (is (= {:unsupported-class 1 :check-refused 1} (:drop-reasons receipt)))
    (is (= 0 (:written receipt) (:results receipt) (:seen-total receipt)))))

(deftest no-path-and-unregistered-identities-are-receipts
  (is (= {:status :absent :reason :no-label-store-configured}
         (wire/record-declaration-reads! nil {})))
  (let [n 'futon2.aif.observation-labels saved (get @identity/registry n)]
    (try
      (swap! identity/registry dissoc n)
      (let [receipt (wire/record-declaration-reads! (path) {})]
        (is (= :missing (:status receipt)))
        (is (= :code-identity-unregistered (:kind receipt)))
        (is (= n (:namespace receipt)))
        (is (not (.exists (io/file (path))))))
      (finally (swap! identity/registry assoc n saved)))))

(deftest corrupt-store-does-not-gate-record-assembly
  (spit (path) "{} {}")
  (let [receipt (wire/record-declaration-reads! (path) {})]
    (is (= :missing (:status receipt)))
    (is (= :invalid-label-store (:kind receipt)))
    (is (= "{} {}" (slurp (path))))))

(deftest runner-persists-the-top-level-receipt-from-the-same-provenance
  (let [provenance (live-provenance)
        saved (#'runner/persist-run-record!
               {:run-record-dir (str (io/file *dir* "records"))
                :observation-labels-path (path)
                :declaration-reads/state (atom (:occurrences provenance))}
               "offline-label-wire" "2026-09-26T00:00:00Z"
               {:outcome :offline-no-selection})
        record (edn/read-string (slurp (:run-record saved)))
        receipt (:observation-labels record)]
    (is (= (sources/provenance (:occurrences provenance)) (:declaration-reads record)))
    (is (= :recorded (:status receipt)))
    (is (= 34 (+ (:written receipt) (:skipped receipt) (:refused receipt))))
    (is (= (:sha256 (store/snapshot (path))) (:snapshot-sha256 receipt)))
    (is (not (contains? (:decision record) :observation-labels))))
  (is (= "/home/joe/code/futon2/data/wm-observation-labels/labels.edn"
         (:observation-labels-path (runner/config {}))))
  (is (nil? (:observation-labels-path (runner/config {:observation-labels-path nil})))))
