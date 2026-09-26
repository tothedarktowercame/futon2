(ns futon2.report.mission-read-once-test
  "WM-MISSION-READ-ONCE-I: one selection reads the mission registry once.
  judge read it for the declared sources and again, through
  cascade-problems/substrate-targets, for the cascade's targets: two reads of
  the same registry, one substrate read apart, in one let. The loaded doc is
  now bound once and passed to both. judge is stopped right after the
  targets are computed (assemble-cascade-problems-with-published captures
  them and throws), with every store it reads before that pointed at an
  empty temp dir. The pre-fix judge is materialised from git (e3bdcfdb)
  under a renamed namespace, never loaded over the real one."
  (:require [clojure.java.shell :as sh]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-problems :as cp]
            [futon2.aif.mission-registry :as mr]
            [futon2.aif.ticket-queue :as ticket-queue]
            [futon2.report.war-machine :as wm]))

(def missions {:missions [{:id "M-live" :status-class :active}
                          {:id "M-done" :status-class :complete}]})

(defn- tmp-dir []
  (str (java.nio.file.Files/createTempDirectory
        "mission-read-once" (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- one-selection
  "judge in NS (the war-machine namespace, or its pre-fix copy) with
  JUDGE-OPTS, stopped once the targets exist: {:reads n :targets [...]}."
  [ns judge-opts]
  (let [reads (atom 0)
        targets (atom nil)
        tmp (tmp-dir)]
    (with-redefs-fn {#'mr/load-missions (fn [& _] (swap! reads inc) missions)
                     #'mr/load-tickets (fn [& _] {:tickets []})
                     (ns-resolve ns 'assemble-cascade-problems-with-published)
                     (fn [_ input & _]
                       (reset! targets (:targets input))
                       (throw (ex-info "stop: targets computed" {::stop true})))}
      #(try ((ns-resolve ns 'judge) {} (merge {:cascade-sources-dir tmp
                                                :cascade-proposals-dir tmp
                                                :repair-obligations-root tmp
                                                :machine-interpretations-dir tmp
                                                :ticket-queue ticket-queue/empty-declaration}
                                               judge-opts))
            (catch clojure.lang.ExceptionInfo e
              (when-not (::stop (ex-data e)) (throw e)))))
    {:reads @reads :targets @targets}))

(def pre-fix-ns
  (delay
    (let [{:keys [exit out err]} (sh/sh "git" "show" "e3bdcfdb:scripts/futon2/report/war_machine.clj")
          renamed 'futon2.report.war-machine-at-e3bdcfdb]
      (when-not (zero? exit) (throw (ex-info "git show failed" {:err err})))
      ;; Compiler/load with a source path: the file registers its load
      ;; identity from *file*, which load-string leaves nil
      (clojure.lang.Compiler/load
       (java.io.StringReader. (str/replace-first out "(ns futon2.report.war-machine" (str "(ns " renamed)))
       "futon2/report/war_machine_at_e3bdcfdb.clj" "war_machine_at_e3bdcfdb.clj")
      (the-ns renamed))))

(deftest one-selection-reads-the-registry-once
  (let [{:keys [reads targets]} (one-selection (:ns (meta #'wm/judge)) {})]
    (is (= 1 reads))
    (is (= ["M-live"] targets))))

(deftest with-declared-sources-the-targets-are-still-the-live-missions
  ;; site 1 is guarded out; the unconditional binding still feeds the targets
  (let [{:keys [reads targets]} (one-selection (:ns (meta #'wm/judge))
                                               {:cascade-sources {}})]
    (is (= ["M-live"] targets))
    (is (= 1 reads))))

(deftest the-pre-fix-selection-read-twice
  ;; the bad case: judge at e3bdcfdb, no pass-through
  (let [{:keys [reads targets]} (one-selection @pre-fix-ns {})]
    (is (= 2 reads))
    (is (= ["M-live"] targets) "the same targets: only the count tells them apart")))

(deftest the-zero-arity-still-reads-for-itself
  ;; work_target_predictor_input.clj's caller is unchanged
  (let [reads (atom 0)]
    (with-redefs [mr/load-missions (fn [& _] (swap! reads inc) missions)
                  mr/load-tickets (fn [& _] {:tickets []})]
      (is (= ["M-live"] (cp/substrate-targets)))
      (is (= 1 @reads)))))
