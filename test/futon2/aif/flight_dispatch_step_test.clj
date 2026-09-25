(ns futon2.aif.flight-dispatch-step-test
  "WM-DISPATCH-STEP-I: flight-runner/agency-dispatch-step!, enact-fn's
  dispatch over the Agency, and its typed outcomes. Fixture Agency ports
  (roster, dispatch, job read, clock); no seat is asked. The good case's
  terminal job is the spike's done job invoke-1790368741658-24428-7c7d3cad,
  captured verbatim from :7070 (fixture header): its state, terminal code
  and events are the live record; only the reply text (:result and the text
  event) is replaced by a step reply, since that job answered a locator
  request."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.flight-driver :as fd]
            [futon2.aif.flight-runner :as fr]))

(def live-job (edn/read-string (slurp "test/futon2/aif/fixtures/agency-job-invoke-1790368741658-24428-7c7d3cad.edn")))

(defn- reply [m] (str "```edn\n" (pr-str (assoc m :schema fr/step-response-schema)) "\n```\n"))

(defn- with-reply [job text]
  (-> job (assoc :result text)
      (update :events (fn [es] (mapv #(if (= "text" (:type %)) (assoc % :text text) %) es)))))

(def step {:target "M-autoclock-in" :candidate :C1 :pattern :p/a :n 1 :phase :commit
           :interpretation {:produces #{:t}}})

(defn- stepper [& {:keys [roster job jobs deadline-ms sent]}]
  (let [clock (atom 0) reads (atom (or jobs (repeat job)))]
    (fr/agency-dispatch-step!
     (cond-> {:seat "claude-5" :opts {}
              :roster-fn (constantly (or roster {:claude-5 {:status "idle" :invoke-ready? true}}))
              :dispatch! (fn [_ seat _ mission prompt]
                           (when sent (reset! sent {:seat seat :mission mission :prompt prompt}))
                           {:job-id "job-s1"})
              :read-job! (fn [_] (let [j (first @reads)] (swap! reads rest) j))
              :now-ms (fn [] @clock) :sleep! (fn [ms] (swap! clock + ms)) :poll-ms 1000}
       deadline-ms (assoc :deadline-ms deadline-ms)))))

(deftest the-good-case-one-step-done-with-its-job-id
  (let [sent (atom nil)
        job (with-reply live-job (reply {:commit "abc1234" :produced :t :check {:class :fixture}}))
        a ((stepper :job job :sent sent) step)]
    (is (= "done" (:state live-job)) "the terminal state is the live record's")
    (is (= {:job-id "job-s1" :deadline {:absent :no-step-deadline}
            :commit "abc1234" :produced :t :check {:class :fixture}} a))
    (is (str/starts-with? (:prompt @sent) "Requisition: M-autoclock-in — War Machine enactment step 1"))
    (testing "on the enactment record, through enact-fn"
      (let [dir (str (java.nio.file.Files/createTempDirectory "enact" (make-array java.nio.file.attribute.FileAttribute 0)))
            r ((fr/enact-fn {:dispatch-step! (stepper :job job) :check-fn (constantly {:observed true})
                             :interpretations (constantly {:p/a {:produces #{:t}}}) :record-dir dir})
               {:target "M-autoclock-in" :flight/id "f"}
               {:click-id "c1" :chosen {:candidate :C1 :precedence [:p/a]}})
            rec (edn/read-string (slurp (:record-path r)))]
        (is (= "job-s1" (get-in rec [:attempts 0 :job-id])))
        (is (true? (get-in rec [:attempts 0 :success])))))))

(deftest a-seat-off-the-roster
  (is (= {:failed {:reason :seat-not-on-roster :seat "claude-5"} :deadline {:absent :no-step-deadline}}
         ((stepper :roster {:codex-1 {:status "idle"}} :job live-job) step))))

(deftest no-requisition
  (is (= {:failed {:reason :requisition-missing} :deadline {:absent :no-step-deadline}}
         ((stepper :job live-job) (dissoc step :target)))))

(deftest a-job-still-running-at-the-deadline
  (let [running (assoc live-job :state "running")
        a ((stepper :job running :deadline-ms 3000) step)]
    (is (= {:reason :job-not-terminal-by-deadline :job-id "job-s1" :deadline-ms 3000} (:failed a)))
    (is (= {:ms 3000 :source :flight-option} (:deadline a)) "the record says which deadline applied")))

(deftest a-decline-is-the-seats-words-not-a-failure
  (let [a ((stepper :job (with-reply live-job (reply {:decline {:reason "the step needs a repo I cannot write"}}))) step)]
    (is (= {:reason "the step needs a repo I cannot write"} (:declined a)))
    (is (nil? (:failed a)))
    (let [dir (str (java.nio.file.Files/createTempDirectory "enact" (make-array java.nio.file.attribute.FileAttribute 0)))
          r ((fr/enact-fn {:dispatch-step! (stepper :job (with-reply live-job (reply {:decline {:reason "no"}})))
                           :interpretations (constantly {:p/a {:produces #{:t}}}) :record-dir dir})
             {:target "M-autoclock-in" :flight/id "f"}
             {:click-id "c1" :chosen {:candidate :C1 :precedence [:p/a]}})
          rec (edn/read-string (slurp (:record-path r)))]
      (is (= [{:kind :step-declined :pattern :p/a :declined {:reason "no"}}]
             (filterv #(= :step-declined (:kind %)) (get-in rec [:conformance :deviations])))))))

(deftest nothing-escapes-as-an-exception
  (is (= :dispatch-threw
         (get-in ((fr/agency-dispatch-step! {:seat "claude-5" :opts {} :roster-fn #(throw (ex-info "roster down" {}))}) step)
                 [:failed :reason]))))

(deftest the-driver-option
  (is (= {:via "flight-runner/agency-dispatch-step!" :seat "claude-5"
          :deadline {:ms 600000 :source :flight-option}}
         (:dispatch-step (fd/resolved-steps {:dispatch-seat "claude-5" :step-deadline-ms 600000}))))
  (is (= {:absent :no-dispatch-configured} (:dispatch-step (fd/resolved-steps {})))))
