(ns futon2.aif.run4-task-pin-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.c-fold-config :as digest]
            [futon2.aif.run4-task-pin :as pin]))

(def mission
  ;; Shape copied from the current file-backed mission registry.
  {:id "M-kangaroo"
   :path "/home/joe/code/futon3c/holes/missions/M-kangaroo.md"
   :title "M-kangaroo — Warm-pouch persistent agent processes"
   :status-line "INSTANTIATE v1 LANDED"
   :status-class :active
   :open-hole-count 6})

(def files {"packet.md" "bounded packet\n"
            "run.edn" "{:run :RUN4}\n"})

(def base-pin
  {:schema :wm/run4-task-pin-v1
   :series-id "RUN4-2026-09-10"
   :trial-id :existing-mission-smoke
   :series-order :as-declared
   :candidate-task-ids [:existing-mission-smoke :second-candidate
                        :third-candidate :fourth-candidate]
   :selected-task-id :existing-mission-smoke
   :sources [{:path "packet.md" :sha256 (digest/sha256 (files "packet.md"))}]
   :casting {:author "zai-5" :reviewer "codex-17"
             :repair-reviewer "codex-1"}
   :operator-selection
   {:mode :operator-selected :operator "Joe"
    :authority-ref "SERIES.edn selection"
    :outer-loop-comparison :record-separately}
   :config {:path "run.edn" :sha256 (digest/sha256 (files "run.edn"))}
   :mapping {:mission-id "M-kangaroo"
             :action {:type :advance-mission :target "M-kangaroo"}}})

(defn ports []
  {:read-text #(or (files %) (throw (ex-info "missing" {:path %})))
   :resolve-mission #(when (= "M-kangaroo" %) mission)
   :action-admissible? #(and (= :active (:status-class %1))
                             (= :advance-mission (:type %2)))})

(defn pin-text [value] (pr-str value))

(defn reason [value injected]
  (try (pin/validate (pin-text value) injected)
       nil
       (catch clojure.lang.ExceptionInfo e (:reason (ex-data e)))))

(deftest validates-identity-without-authorizing-execution
  (let [text (pin-text base-pin)
        envelope (pin/validate text (ports))]
    (is (true? (:valid? envelope)))
    (is (= (digest/sha256 text) (get-in envelope [:task-pin :sha256])))
    (is (= :exact-utf8-pin-bytes
           (get-in envelope [:task-pin :digest-semantics])))
    (is (= (:candidate-task-ids base-pin)
           (get-in envelope [:task-pin :ordered-task-ids])))
    (is (= mission (get-in envelope [:mission-action :mission])))
    (is (false? (get-in envelope
                        [:operator-selection :outer-loop-ranking-match-required?])))
    (is (true? (get-in envelope
                       [:operator-selection
                        :inner-policy-selection-required-before-execution?])))
    (is (= :declared-not-authenticated
           (get-in envelope [:operator-selection :authority-status])))
    (is (= {:status :not-evaluated} (:executability envelope)))
    (is (= {:status :not-evaluated} (:acceptance envelope)))
    (is (= {:permitted? false :reason :validation-only} (:launch envelope)))
    (is (nil? (:ready envelope)))))

(deftest pin-identity-is-exact-byte-identity
  (let [plain (pin-text base-pin)
        with-newline (str plain "\n")]
    (is (not= (get-in (pin/validate plain (ports)) [:task-pin :sha256])
              (get-in (pin/validate with-newline (ports)) [:task-pin :sha256])))))

(deftest refuses-candidate-and-casting-identity-defects
  (testing "duplicate and absent selected IDs"
    (is (= :duplicate-candidate-id
           (reason (assoc base-pin :candidate-task-ids [:x :x]) (ports))))
    (is (= :selected-id-absent
           (reason (assoc base-pin :selected-task-id :absent) (ports))))
    (is (= :trial-selection-mismatch
           (reason (assoc base-pin :trial-id :different-trial) (ports))))
    (is (= :unknown-series-order
           (reason (assoc base-pin :series-order :ranked) (ports)))))
  (testing "author and reviewer must be distinct"
    (is (= :author-is-reviewer
           (reason (assoc-in base-pin [:casting :reviewer] "zai-5")
                   (ports))))
    (is (= :invalid-author
           (reason (assoc-in base-pin [:casting :author] 42) (ports))))
    (is (= :invalid-reviewer
           (reason (assoc-in base-pin [:casting :reviewer] {:seat "codex-17"})
                   (ports))))
    (is (= :invalid-repair-reviewer
           (reason (assoc-in base-pin [:casting :repair-reviewer] :codex-1)
                   (ports))))))

(deftest refuses-malformed-identifiers-and-paths-with-typed-reasons
  (is (= :invalid-series-id (reason (assoc base-pin :series-id " ") (ports))))
  (is (= :invalid-trial-id (reason (assoc base-pin :trial-id 42) (ports))))
  (is (= :invalid-candidate-id
         (reason (assoc base-pin :candidate-task-ids
                        [:existing-mission-smoke {:task :bad}])
                 (ports))))
  (is (= :invalid-selected-task-id
         (reason (assoc base-pin :selected-task-id nil) (ports))))
  (is (= :invalid-source-path
         (reason (assoc-in base-pin [:sources 0 :path] {:path "packet.md"})
                 (ports))))
  (is (= :invalid-source-path
         (reason (assoc-in base-pin [:config :path] 42) (ports)))))

(deftest refuses-stale-and-unknown-inputs
  (is (= :stale-source
         (reason (assoc-in base-pin [:sources 0 :sha256] (apply str (repeat 64 "0")))
                 (ports))))
  (is (= :stale-source
         (reason (assoc-in base-pin [:config :sha256] (apply str (repeat 64 "0")))
                 (ports))))
  (is (= :unknown-mission-mapping
         (reason (assoc-in base-pin [:mapping :mission-id] "M-missing")
                 (ports))))
  (is (= :action-mission-mismatch
         (reason (assoc-in base-pin [:mapping :action :target] "M-other")
                 (ports))))
  (is (= :inadmissible-action-mapping
         (reason base-pin (assoc (ports) :action-admissible? (constantly false))))))

(deftest refuses-non-single-edn-and-missing-ports
  (is (= :trailing-pin-form
         (try (pin/validate (str (pin-text base-pin) " :extra") (ports))
              nil
              (catch clojure.lang.ExceptionInfo e (:reason (ex-data e))))))
  (is (= :missing-read-port
         (try (pin/validate (pin-text base-pin) {})
              nil
              (catch clojure.lang.ExceptionInfo e (:reason (ex-data e)))))))
