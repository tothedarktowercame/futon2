(ns futon2.aif.realized-recording-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is]]
            [futon2.aif.realized-recording :as recording]
            [futon2.aif.realized-outcome :as ro]
            [futon2.aif.fold-realized :as fold]
            [futon2.aif.actuator-a3 :as a3]
            [futon2.aif.selection-gain :as gain]))

;; Verbatim capture, not authored constants. Source and byte equality are gated
;; below; the envelope is a new adapter projection, never a relabelled history.
(def live-path "holes/labs/wm-contract/runs/2026-09-05-u59-b/observation/realized-outcome-2026-09-05-u59-a.edn")
(def fixture-path "test/fixtures/realized-outcome-u59-live.edn")
(def live (edn/read-string (slurp fixture-path)))
(defn live-trace [run]
  (edn/read-string {:default (fn [_ v] v)}
    (first (str/split-lines
             (slurp (str "holes/labs/wm-contract/runs/" run "/wm-trace-2026-09-05.edn"))))))
(defn record []
  (recording/step-envelope live
    {:run-id "2026-09-05-u59-a" :step-index "001"
     :before (live-trace "2026-09-05-u59-a")
     :after (live-trace "2026-09-05-u59-b")
     :evidence {:repo "futon2" :path live-path :revision "3e9dd1fd44b954c258829ecb70a5ba816c7b8be4"
                :locator :whole-record :adapter :test-v1}}))

(defn forecast-record []
  (let [r (record) m (:measurement r)]
    (-> r
        (assoc-in [:measurement :expected-source] :prediction)
        (assoc-in [:measurement :admission] :admissible)
        (assoc-in [:measurement :forecast]
                  (merge (select-keys m [:quantity :units :method])
                         {:model :test-v1 :evidence :source
                          :captured-at "2026-09-05T05:40:00Z" :window (:expected-window m)}))
        (assoc-in [:measurement :realization]
                  (merge (select-keys m [:quantity :units :method])
                         {:evidence :source :window (:realized-window m)})))))

(deftest verbatim-live-pin-and-legacy
  (is (= (slurp live-path) (slurp fixture-path)))
  (is (ro/legs-readable? live))
  (is (not (recording/marked? (ro/normalize live))))
  (is (= live (recording/envelope live nil)))
  (is (= (record) (recording/validate! (record)))))

(deftest enactor-opt-in-preserves-actual-legs-and-dial
  (let [decision {:policy (:policy live) :expected-score 0.5}
        wiring {:boxes [{:id :a}] :policy-holes [{:free "b"}]}
        legacy (fold/realized-outcome-of decision wiring (:tick live))
        context (-> (record) (assoc :outcome nil)
                    (update :classification #(merge (dissoc % :value) (recording/unknown :not-classified)))
                    (assoc-in [:measurement :expected :value] (:expected-score legacy))
                    (assoc-in [:measurement :realized :value] (:realized-score legacy)))
        wrapped (fold/realized-outcome-of (assoc decision :recording/context context) wiring (:tick live))]
    (is (= legacy (select-keys wrapped (keys legacy))))
    (is (recording/marked? wrapped))
    (with-redefs [a3/box-match-snapshot (fn [& _] [{:inhabited? true} {:inhabited? false}])]
      (let [opts {:deposit {:mission (:policy live)} :tick (:tick live)}
            original (fold/realized-outcome-grounded (:policy live) decision opts)
            context (-> context
                        (assoc :scale :endpoint-count)
                        (assoc-in [:measurement :units] :endpoint-count)
                        (assoc-in [:measurement :quantity] :uninhabited-endpoints)
                        (assoc-in [:measurement :method] :substrate-dial)
                        (assoc-in [:measurement :expected :value] (:expected-score original))
                        (assoc-in [:measurement :realized :value] (:realized-score original)))
            marked (fold/realized-outcome-grounded (:policy live) decision
                                                  (assoc opts :recording/context context))]
        (is (= original (select-keys marked (keys original))))
        (is (= :perfection-target (:expected-source marked)))
        (is (not (recording/calibration-admitted? marked)))))))

(deftest statuses-and-adapter-failure
  (is (recording/observation? (recording/observed 0 :count :source)))
  (is (recording/observation? (recording/observed [] :checkpoints :source)))
  (is (recording/observation? (recording/observed false :feedback-delivered :source)))
  (is (recording/observation? (recording/unknown :feedback-not-captured)))
  (is (not (recording/observation? (assoc (recording/unknown :missing) :value 0))))
  (is (not (recording/observation? {:status :observed :value 0})))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"adapter failed"
                       (recording/adapter-error "adapter failed" :raw-receipt)))
  (is (not= (recording/unknown :absent) (recording/observed [] :checkpoints :source))))

(deftest field-table-and-paired-capture
  (doseq [k recording/required-fields]
    (is (thrown? clojure.lang.ExceptionInfo (recording/validate! (dissoc (record) k))) (str k)))
  (is (thrown? clojure.lang.ExceptionInfo
               (recording/validate! (assoc-in (record) [:observations :attempt/id] "other"))))
  (is (= :unknown (get-in (record) [:observations :checkpoints :status])))
  (is (= 0.0 (get-in (record) [:observations :channels :pre :value :ticks-firing-ratio :value]))))

(deftest defaulted-numeric-channel-is-not-observed-zero
  (let [before (assoc-in (live-trace "2026-09-05-u59-a")
                         [:observation-envelope :channels :ticks-firing-ratio]
                         {:variant :absent :value 0.0 :reason :source-field-missing})
        r (recording/step-envelope live
            {:run-id "2026-09-05-u59-a" :step-index 1 :before before
             :after (live-trace "2026-09-05-u59-b")
             :evidence {:digest "controlled-missing-channel" :locator :test :adapter :test}})]
    (is (= :unknown (get-in r [:observations :channels :pre :value :ticks-firing-ratio :status])))
    (is (not (contains? (get-in r [:observations :channels :pre :value :ticks-firing-ratio]) :value)))))

(deftest calibration-warrants-not-numerical-error
  (let [r (forecast-record)]
    (is (recording/calibration-admitted? r))
    (doseq [bad [(assoc-in r [:measurement :realization :evidence] :missing)
                 (assoc-in r [:measurement :forecast :units] :other)
                 (assoc-in r [:measurement :realization :method] :other)
                 (assoc-in r [:measurement :expected-source] :perfection-target)
                 (assoc-in r [:measurement :expected-source] :reevaluation)
                 (assoc-in r [:measurement :aligned?] false)
                 (assoc-in r [:measurement :forecast :captured-at] "2026-09-06T00:00:00Z")]]
      (is (false? (boolean (recording/calibration-admitted? bad)))))))

(deftest category-is-not-terminal
  (is (= :grounded-no-change (recording/category (record) :step)))
  (is (nil? (recording/terminal-disposition (record))))
  (let [r (-> (record) (assoc :outcome nil)
              (update :classification #(merge (dissoc % :value :domain :evidence)
                                               (recording/unknown :missing-category))))]
    (is (nil? (ro/categorical-outcome {:realized-outcome r})))))

(deftest revisions-retract-not-double-count
  (let [r (forecast-record) initial (gain/initial-selection-gain-state)
        consumed (gain/fold-realized-outcome initial r)
        correction (-> r (assoc :record/id "correction" :revision 1 :supersedes (:record/id r))
                       (assoc-in [:measurement :admission] :inadmissible))
        retracted (gain/fold-realized-outcome consumed correction)
        other (assoc r :run/id "different-run" :record/id "different-record")]
    (is (= consumed (gain/fold-realized-outcome consumed r)))
    (is (= (:perf-history initial) (:perf-history retracted)))
    (is (= (:perf-history (gain/fold-realized-outcome initial live))
           (:perf-history (-> consumed (gain/fold-realized-outcome live)
                              (gain/fold-realized-outcome correction)))))
    (is (= 2 (count (recording/latest-revisions [r other]))))
    (is (= [correction] (vec (recording/latest-revisions [r correction]))))
    (is (thrown? clojure.lang.ExceptionInfo (recording/latest-revisions [r (assoc r :expected-score 99)])))))

(deftest persist-and-consume
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory "recording-replay"
                       (make-array java.nio.file.attribute.FileAttribute 0)))
        path (io/file dir "original.edn") r (record)]
    (recording/persist! path r)
    (let [readback (edn/read-string (slurp path))]
      (is (= r readback))
      (is (= :grounded-no-change (recording/category readback :step)))
      (is (not (recording/calibration-admitted? readback))))
    (is (thrown? clojure.lang.ExceptionInfo
                 (recording/persist! path (assoc r :record/id "overwrite"))))))

(deftest actual-stepped-producer-read-only-replay
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory "stepped-recording"
                       (make-array java.nio.file.attribute.FileAttribute 0)))
        pin (io/file dir "pin/pin.edn")]
    (io/make-parents pin)
    (spit pin (pr-str {:pin/accepted-steps [{:run-id "2026-09-05-u59-a"}]}))
    (let [{:keys [exit out err]} (shell/sh "bb" "holes/labs/wm-contract/wm_step_observe.bb"
                                         (str dir) "2026-09-05-u59-b" "--print")]
      (is (= 0 exit) err)
      (when (zero? exit)
        (let [r (edn/read-string out) path (io/file dir "capture.edn")]
          (recording/persist! path r)
          (is (= "2026-09-05-u59-a/1" (:attempt/id r)))
          (is (= :grounded-no-change (ro/categorical-outcome (edn/read-string (slurp path)))))
          (is (not (recording/calibration-admitted? r))))))))

(deftest versioned-preferences-and-forecast-boundary
  (let [module (edn/read-string (slurp "resources/c-modules/current-work-v1.edn"))
        readings {:claim-warrant {:version 1 :criterion "claim-warrant/v1"
                                  :status :observed :value false :evidence :source}}
        r (-> (record)
              (assoc-in [:preferences :module]
                        (assoc (recording/observed module :c-module-v1 :source) :digest "pinned"))
              (assoc-in [:preferences :realized]
                        (recording/observed readings :c-readings-v1 :source)))]
    (is (= readings (recording/preference-readings r :realized)))
    (is (nil? (recording/preference-readings r :predicted)))
    (is (thrown? clojure.lang.ExceptionInfo
                 (recording/validate! (assoc-in r [:preferences :realized :value :claim-warrant :version] 99))))
    (is (thrown? clojure.lang.ExceptionInfo
                 (recording/validate! (assoc-in r [:preferences :realized :value :claim-warrant :status] :predicted))))
    (is (thrown? clojure.lang.ExceptionInfo
                 (recording/preference-readings
                  (-> r (assoc-in [:preferences :predicted] (get-in r [:preferences :realized]))
                      (assoc-in [:preferences :predicted :value :claim-warrant :status] :predicted)) :predicted)))))

(deftest repeated-process-occurrences-not-overwritten
  (let [occurrence {:mission "m" :phase :verify :occurrence 1 :transition :enter :readings {}}
        r (assoc-in (record) [:preferences :occurrences]
                    [occurrence (assoc occurrence :occurrence 2)])]
    (is (= 2 (count (get-in (recording/validate! r) [:preferences :occurrences]))))
    (is (thrown? clojure.lang.ExceptionInfo
                 (recording/validate! (assoc-in r [:preferences :occurrences] [occurrence occurrence]))))))
