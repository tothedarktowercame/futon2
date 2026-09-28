(ns futon2.aif.progress-read-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.progress-read :as progress-read])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- temp-dir []
  (.toFile (Files/createTempDirectory "progress-read" (make-array FileAttribute 0))))

(defn- write-edn! [path value]
  (.mkdirs (.getParentFile (io/file path)))
  (spit path (pr-str value))
  (.getCanonicalPath (io/file path)))

(defn- candidate [id target needs]
  {:id {:id id :precedence [{:id :pattern
                             :guard {:needs (set (map #(vector target %) needs))}}]}})

(defn- run-record [chosen-id candidates]
  {:decision {:chosen {:candidate chosen-id}
              :selection-certificate {:candidates candidates}}})

(defn- records! []
  (let [root (temp-dir)
        enactments (io/file root "enactments")
        runs (io/file root "runs")
        c1-path (write-edn! (io/file enactments "c1.edn")
                            {:attempts [{:success true :produced :roles-named}
                                        {:success false :produced :other}]})
        c2-path (write-edn! (io/file enactments "c2.edn") {:attempts []})
        _ (write-edn! (io/file runs "tick-run-record-c1.edn")
                      (run-record :cand-1 [(candidate :cand-1 "M-x" #{})]))
        _ (write-edn! (io/file runs "tick-run-record-c2.edn")
                      (run-record :cand-2 [(candidate :cand-2 "M-x" #{:roles-named})]))
        envelope {:plan {}
                  :flight {:target "M-x"
                           :clicks [{:click-id "c1" :advanced []}
                                    {:click-id "c2" :advanced []}]
                           :enactments [{:click-id "c1" :record-path c1-path}
                                        {:click-id "c2" :record-path c2-path}]}}
        flight-path (write-edn! (io/file root "flight.edn") envelope)]
    {:root root :runs runs :envelope envelope :flight-path flight-path
     :c1-path c1-path :c2-path c2-path}))

(deftest successful-attempt-produces-a-token-consumed-by-a-later-click
  (let [{:keys [flight-path runs]} (records!)
        result (progress-read/check-flight-file flight-path runs)]
    (is (= #{:roles-named} (get-in result [:input 0 :produced])))
    (is (= :token-consumed (get-in result [:check :clicks 0 :progress])))
    (is (= [{:token ["M-x" :roles-named] :by "c2"}]
           (get-in result [:check :clicks 0 :consumed])))))

(deftest enactments-join-to-clicks-by-id-not-vector-position
  (let [{:keys [envelope runs]} (records!)
        reordered (update-in envelope [:flight :enactments] #(vec (reverse %)))
        input (progress-read/flight-clicks reordered runs)]
    (is (= #{:roles-named} (get-in input [0 :produced])))
    (is (= #{} (get-in input [1 :produced])))))

(deftest produced-evidence-absence-arms-are-typed
  (let [{:keys [envelope runs root]} (records!)
        no-entry (assoc-in envelope [:flight :enactments] [])
        no-decision (assoc-in envelope [:flight :enactments 0]
                              {:click-id "c1" :enactment {:absent :no-decision}})
        no-path (assoc-in envelope [:flight :enactments 0] {:click-id "c1"})
        bad-path (write-edn! (io/file root "bad.edn") {:not "used"})
        _ (spit bad-path "{")
        unreadable (assoc-in envelope [:flight :enactments 0]
                             {:click-id "c1" :record-path bad-path})]
    (is (= {:absent :no-enactment-entry}
           (get-in (progress-read/flight-clicks no-entry runs) [0 :produced])))
    (is (= {:absent :no-decision}
           (get-in (progress-read/flight-clicks no-decision runs) [0 :produced])))
    (is (= {:absent :no-enactment-record-path}
           (get-in (progress-read/flight-clicks no-path runs) [0 :produced])))
    (is (= {:absent :enactment-record-unreadable}
           (get-in (progress-read/flight-clicks unreadable runs) [0 :produced])))))

(deftest missing-run-record-is-a-typed-absence
  (let [{:keys [envelope runs]} (records!)
        _ (.delete (io/file runs "tick-run-record-c1.edn"))]
    (is (= {:absent :run-record-missing}
           (get-in (progress-read/flight-clicks envelope runs) [0 :needs])))))

(deftest chosen-candidate-absence-arms-are-typed
  (let [{:keys [envelope runs]} (records!)
        path (io/file runs "tick-run-record-c1.edn")]
    (write-edn! path {:decision {:chosen {:status :absent :reason :no-chosen-action}}})
    (is (= {:absent :no-chosen-action}
           (get-in (progress-read/flight-clicks envelope runs) [0 :needs])))
    (write-edn! path {:decision {:chosen {:id :action-only}}})
    (is (= {:absent :no-candidate-on-run-record}
           (get-in (progress-read/flight-clicks envelope runs) [0 :needs])))
    (write-edn! path (run-record :missing [(candidate :other "M-x" #{})]))
    (is (= {:absent :chosen-candidate-not-in-certificate}
           (get-in (progress-read/flight-clicks envelope runs) [0 :needs])))))

(deftest need-for-another-target-is-malformed-not-consumed
  (let [{:keys [envelope runs]} (records!)
        path (io/file runs "tick-run-record-c2.edn")]
    (write-edn! path (run-record :cand-2 [(candidate :cand-2 "M-y" #{:roles-named})]))
    (is (= {:absent :candidate-guard-needs-malformed}
           (get-in (progress-read/flight-clicks envelope runs) [1 :needs])))))

(deftest missing-advanced-click-id-and-target-throw-typed-kinds
  (let [{:keys [envelope runs]} (records!)]
    (doseq [[kind changed]
            [[:advanced-not-on-click-record
              (update-in envelope [:flight :clicks 0] dissoc :advanced)]
             [:click-id-not-on-flight-record
              (update-in envelope [:flight :clicks 0] dissoc :click-id)]
             [:target-not-on-flight-record
              (update envelope :flight dissoc :target)]]]
      (testing (name kind)
        (try
          (progress-read/flight-clicks changed runs)
          (is false "expected typed ex-info")
          (catch clojure.lang.ExceptionInfo e
            (is (= kind (:kind (ex-data e))))))))))
