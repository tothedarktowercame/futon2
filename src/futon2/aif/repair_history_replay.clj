(ns futon2.aif.repair-history-replay
  "Tooling-JVM evaluator driver. Its classpath supplies the REPAIRED cohort
   implementation, not the serving JVM's possibly stale loaded namespaces.
   All writes are confined to scratch cohort roots. No server calls or ticks."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.full-loop-cohort :as cohort])
  (:import [java.nio.file Files]
           [java.security MessageDigest]))

(defn- digest [bytes]
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (MessageDigest/getInstance "SHA-256") bytes))))

(defn evaluate [request]
  (let [{:keys [finding finding-pin close artifact input scratch prereg repaired-root]} request
        actual-source (.getCanonicalPath (io/file (io/resource "futon2/aif/full_loop_cohort.clj")))
        expected-source (when repaired-root
                          (.getCanonicalPath (io/file repaired-root "src/futon2/aif/full_loop_cohort.clj")))
        _ (when-not (= expected-source actual-source)
            (throw (ex-info "Replay loaded cohort outside the repaired revision"
                            {:repair-evaluator/refusal :repaired-source-mismatch
                             :expected expected-source :actual actual-source})))
        bytes (Files/readAllBytes (.toPath (io/file (:path input))))
        _ (when-not (= (:sha256 input) (digest bytes))
            (throw (ex-info "Recorded failure input changed" {:path (:path input)})))
        before-error (try (cohort/read-edn (:path input)) nil
                          (catch RuntimeException e (.getName (class e))))
        _ (cohort/activate! prereg scratch)
        term {:judgment {:opportunity-id "repair-replay/first"
                        :trigger :wallclock-cron :machine-state {:tick 1}
                        :agent-roster [] :code-state {:git-sha (:commit artifact)
                         :git-dirty? false :resolved-mode-flags {}
                         :configuration-digest "repair-replay"}
                        :semantic-epoch :epoch-1}
              :ground {:kind :repair-replay}}
        first-event (cohort/start-attempt! prereg scratch term)
        dir (io/file scratch (name (:cohort/id first-event)) (:attempt/id first-event))
        poison (io/file dir "002-selection.edn")
        _ (io/copy (io/file (:path input)) poison)
        history (cohort/attempt-history dir)
        second-event (cohort/start-attempt!
                      prereg scratch (assoc-in term [:judgment :opportunity-id] "repair-replay/second"))
        ledger (cohort/ledger prereg scratch)
        readable? (= first-event (first (:events history)))
        excluded? (some #(and (= :excluded (:history/status %))
                             (= (.getAbsolutePath poison) (:path %))) (:exclusions history))
        proceeded? (and second-event (= 2 (:recorded-attempt-count ledger)))
        identity (io/file dir "001-time-step.edn")
        _ (spit identity "{:invalid :hole/2f9b03b16170}")
        refusal (try (cohort/start-attempt! prereg scratch term) nil
                     (catch clojure.lang.ExceptionInfo e (ex-data e)))
        identity-safe? (and (= :history-identity-unavailable (:failure-kind refusal))
                            (= (.getAbsolutePath identity) (:path refusal)))
        evidence {:input input :readable-consumed? readable? :non-identity-excluded? (boolean excluded?)
                  :next-admission? (boolean proceeded?) :identity-refusal refusal
                  :history history :ledger-count (:recorded-attempt-count ledger)}
        passed? (boolean (and before-error readable? excluded? proceeded? identity-safe?))]
    {:schema :wm/repair-observation-v1
     :identity {:repair/id (:repair/id finding) :finding-sha256 (:sha256 finding-pin)
                :attempt/id (:attempt/id close) :run/id (:run/id close) :artifact-commit (:commit artifact)}
     :recorded-failure {:reproduced-before? (boolean before-error)
                        :reproduced-after? (not (and readable? excluded? proceeded?)) :evidence [evidence]}
     :successor {:production-shaped? true :passed? passed? :evidence [evidence]}}))

(defn -main [request-file output-file]
  (spit output-file (pr-str (evaluate (edn/read-string (slurp request-file))))))
