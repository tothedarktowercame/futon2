(ns futon2.report.mission-read-once-test
  "A supplied task reads the mission registry once; standalone target reads remain supported."
  (:require [clojure.test :refer [deftest is]]
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
  ([ns judge-opts] (one-selection ns judge-opts missions))
  ([ns judge-opts mission-doc]
  (let [reads (atom 0)
        targets (atom nil)
        tmp (tmp-dir)]
    (with-redefs-fn {#'mr/load-missions (fn [& _] (swap! reads inc) mission-doc)
                     #'mr/load-tickets (fn [& _] {:tickets []})
                     #'mr/load-excursions (fn [& _] {:excursions []})
                     (ns-resolve ns 'assemble-cascade-problems-with-published)
                     (fn [_ input & _]
                       (reset! targets (:targets input))
                       (throw (ex-info "stop: targets computed" {::stop true})))}
      #(try ((ns-resolve ns 'judge) {} (merge {:task {:id "M-live" :kind :mission}
                                                :cascade-sources-dir tmp
                                                :cascade-proposals-dir tmp
                                                :repair-obligations-root tmp
                                                :machine-interpretations-dir tmp
                                                :ticket-queue ticket-queue/empty-declaration}
                                               judge-opts))
            (catch clojure.lang.ExceptionInfo e
              (when-not (::stop (ex-data e)) (throw e)))))
    {:reads @reads :targets @targets})))

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

(deftest the-zero-arity-still-reads-for-itself
  ;; work_target_predictor_input.clj's caller is unchanged
  (let [reads (atom 0)]
    (with-redefs [mr/load-missions (fn [& _] (swap! reads inc) missions)
                  mr/load-tickets (fn [& _] {:tickets []})
                  mr/load-excursions (fn [& _] {:excursions []})]
      (is (= ["M-live"] (cp/substrate-targets)))
      (is (= 1 @reads)))))
