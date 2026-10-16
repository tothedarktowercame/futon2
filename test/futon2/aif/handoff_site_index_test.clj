(ns futon2.aif.handoff-site-index-test
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.handoff-site-index :as sut]))

(def repo (.getCanonicalPath (io/file ".")))

(deftest every-known-production-handoff-has-one-live-index-entry
  (let [index (sut/read-index)
        sites (:sites index)
        audit (sut/audit repo)]
    (is (= :verified (:status audit)) (pr-str (:problems audit)))
    (is (= 16 (:site-count audit)))
    (is (= (count sites) (count (set (map :id sites)))))
    (is (every? #(and (:contract %) (:transport %) (:status %)) sites))))

(deftest shared-exchange-callers-cannot-appear-outside-the-index
  (let [indexed (set (map :source (:sites (sut/read-index))))
        roots [(io/file repo "src/futon2/aif")]
        callers (set (for [root roots
                           file (file-seq root)
                           :when (and (.isFile file) (str/ends-with? (.getName file) ".clj"))
                           :let [relative (str (.relativize (.toPath (io/file repo))
                                                           (.toPath file)))]
                           :when (and (not= relative "src/futon2/aif/handoff_contract.clj")
                                      (str/includes? (slurp file) "handoff/exchange"))]
                       relative))]
    (is (= #{"src/futon2/aif/full_loop_runner.clj"
             "src/futon2/aif/selected_target_reading.clj"}
           callers))
    (is (every? indexed callers))))

(deftest every-agent-reply-is-shared-or-an-explicit-exception
  (doseq [{:keys [id kind transport status exception]} (:sites (sut/read-index))
          :when (= :agent-reply kind)]
    (is (or (contains? #{:handoff/exchange :handoff/response-payload} transport)
            (and (= :registered-exception status) (not (str/blank? exception))))
        (str id " bypasses the shared reply boundary without an exception"))))

(deftest click-orchestration-has-no-obsolete-cast-identities
  (doseq [source ["scripts/wm_click.sh" "scripts/wm_click_repair_loop.py"]
          :let [text (->> (str/split-lines (slurp (io/file repo source)))
                          (remove #(str/starts-with? (str/trim %) "#"))
                          (str/join "\n"))]
          stale ["codex-proof2d" "codex-proof2e" "claude-1" "claude-4" "wmfix-1"]]
    (is (not (str/includes? text stale))
        (str source " still embeds obsolete cast identity " stale))))
