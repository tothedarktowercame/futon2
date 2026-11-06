(ns futon2.aif.c2-exclusion-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.c1-exclusion :as c1-exclusion]
            [futon2.aif.c2-exclusion :as sut]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.outer-refusal-history :as history]
            [futon2.aif.outer-refusal-index :as index]
            [futon2.aif.refusal-legality :as legality]))

(def text "# Mission\nStable may mean LTS or last green.\n")
(def question {:question "Which stable?"
               :span {:lines [2 2] :quote "Stable may mean LTS or last green."}
               :alternatives ["LTS" "last green"]})

(defn- fixture []
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "c2-exclusion" (make-array java.nio.file.attribute.FileAttribute 0)))
        source (io/file dir "M-c2.md")
        brief-root (str (io/file dir "brief"))
        mission {:source {:repo "test" :path (.getPath source)
                          :sha256 (identity/sha256 text)}}
        _ (spit source text)
        thrown (try
                 (runner/throw-if-author-refused!
                  {:result (str "FULL_LOOP_AUTHOR: QUESTION " (pr-str question))}
                  "M-c2" :author-wait mission brief-root)
                 nil
                 (catch clojure.lang.ExceptionInfo e (ex-data e)))
        resolved (legality/resolve-close-refusal
                  false nil :author-question {:error-data thrown}
                  (constantly nil) false)
        record {:run/id "run-c2" :target "M-c2"
                :failure {:kind :author-question
                          :detail (assoc (select-keys thrown
                                                     [:c2/question :c2/brief-receipt])
                                         :target "M-c2")}
                :effective-run-identity
                {:target "M-c2" :mission-source-sha256 (get-in mission [:source :sha256])}
                :refusal/disposition (:refusal/disposition (:data resolved))}]
    {:dir dir :record record :source source :sha (get-in mission [:source :sha256])}))

(deftest checked-c2-publishes-click-terminal-row-and-excludes-until-evidence-changes
  (let [{:keys [dir record sha]} (fixture)
        record-file (io/file dir "tick-run-record-run-c2.edn")]
    (is (= :pending-exclusion (get-in record [:refusal/disposition :disposition])))
    (spit record-file (pr-str record))
    (index/append-run-record! dir record-file record)
    (let [entry (first (history/read-index dir))
          exclusion (:c2-exclusion entry)]
      (is (= :click-terminal (get-in entry [:refusal/disposition :disposition])))
      (is (true? (get-in entry [:refusal/disposition :evidence :exclusionRecorded])))
      (is (= :row (:status exclusion)))
      (is (string? (get-in exclusion [:row :question-digest])))
      (is (string? (get-in exclusion [:row :brief-receipt-pin])))
      (is (= [{:target "M-c2" :reason :c2-legitimate-question
               :pin (identity/digest {:mission-source-sha256 sha}) :run-id "run-c2"}]
             (c1-exclusion/field-exclusions
              [(assoc exclusion :exclusion-reason :c2-legitimate-question)]
              (constantly sha) ["M-c2"])))
      (is (empty? (c1-exclusion/field-exclusions
                   [(assoc exclusion :exclusion-reason :c2-legitimate-question)]
                   (constantly "new-sha") ["M-c2"]))))))

(deftest c2-index-publication-failure-is-fatal
  (let [{:keys [dir record]} (fixture)
        record-file (io/file dir "tick-run-record-run-c2.edn")]
    (spit record-file (pr-str record))
    (is (= :exclusion-not-recorded
           (try
             (with-redefs [index/append-entry! (fn [& _] (throw (ex-info "disk" {})))]
               (index/append-run-record! dir record-file record))
             nil
             (catch clojure.lang.ExceptionInfo e (:failure-kind (ex-data e))))))))

(deftest c2-row-refuses-missing-evidence
  (let [{:keys [record]} (fixture)]
    (is (= :question-evidence-absent
           (:reason (sut/exclusion-row
                     (update-in record [:failure :detail] dissoc :c2/brief-receipt)))))
    (is (= :digest-carrier-absent
           (:reason (sut/exclusion-row
                     (assoc-in record [:failure :detail :target] "T-c2")))))))
