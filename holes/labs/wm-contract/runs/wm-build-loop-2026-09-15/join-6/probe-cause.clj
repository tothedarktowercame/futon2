;; claude-2 join-6 cause probe. Read-only diagnostics; no repository file changes.
;; (a) The exact refusal reason for the test's pinned config text.
;; (b) The same valid request through a copy of the test fixture that differs
;;     ONLY in :runner-options {} instead of {:cohort? false}.
(require '[clojure.java.io :as io]
         '[futon2.aif.c-fold-config :as digest]
         '[futon3c.transport.http :as http]
         '[futon3c.wm.run4-pinned-run-config :as pinned]
         '[futon3c.wm.runner-service :as service]
         '[futon3c.wm.run4-http-boundary-test :as t])

(defn config-text [runner-options]
  (str (pr-str {:schema :wm/run4-pinned-run-config-v1
                :runner-options runner-options
                :c-fold {:enabled? false}}) "\n"))

(def reason-a
  (let [text (config-text {:cohort? false})]
    (try (pinned/load! {:path "config.edn" :sha256 (digest/sha256 text)} (fn [_] text))
         :loaded
         (catch clojure.lang.ExceptionInfo e (select-keys (ex-data e) [:error :reason])))))

(defn replay [runner-options]
  ;; Mirrors t/with-handler (test lines 17-58) with only config-text varied.
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                       "join6-probe" (make-array java.nio.file.attribute.FileAttribute 0)))
        source "task\n"
        ctext (config-text runner-options)
        pin {:schema :wm/run4-task-pin-v1
             :series-id "RUN4-2026-09-10" :trial-id :outer-loop-successor
             :series-order :as-declared
             :candidate-task-ids [:outer-loop-successor :math-probe :caption-probe :feedback-monitor]
             :selected-task-id :outer-loop-successor
             :sources [{:path "source.md" :sha256 (digest/sha256 source)}]
             :casting t/casting
             :operator-selection {:mode :operator-selected :operator "Joe"
                                  :authority-ref "SERIES.edn selection"}
             :config {:path "config.edn" :sha256 (digest/sha256 ctext)}
             :mapping {:mission-id "M-run4" :action {:type :advance-mission :target "M-run4"}}}
        cfg {:run4 {:enabled? true :bearer-token t/token :operator "Joe"
                    :casting t/casting :admission-root (.getPath root)
                    :pin-root (.getPath root) :pin-allowlist #{"pin.edn"}
                    :source-root (.getPath root) :source-allowlist #{"source.md" "config.edn"}
                    :resolve-mission #(when (= "M-run4" %) t/mission)
                    :action-admissible? (fn [m a] (and (= t/mission m) (= :advance-mission (:type a))))}}]
    (try
      (spit (io/file root "source.md") source)
      (spit (io/file root "config.edn") ctext)
      (spit (io/file root "pin.edn") (pr-str pin))
      (let [seen (atom [])]
        (with-redefs [service/click! (fn [opts] (swap! seen conj opts)
                                       {:click-id "probe" :started-at "2026-09-15T00:00:00Z"})]
          (let [resp ((http/make-handler cfg)
                      (t/request {:run4-pin-ref "pin.edn" :run4-attempt-id "attempt-probe"} t/auth))]
            {:runner-options runner-options :status (:status resp)
             :body (let [b (:body resp)] (if (string? b) b (slurp b)))
             :click-called (count @seen)})))
      (finally (t/delete-tree! root)))))

(prn {:a-load-reason reason-a
      :b-stale-fixture (replay {:cohort? false})
      :b-only-cohort-key-removed (replay {})})
(shutdown-agents)
