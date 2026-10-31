(ns futon2.aif.handoff-site-index-test
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.handoff-site-index :as sut]))

(def repo (.getCanonicalPath (io/file ".")))

(def futon3c-repo
  (some #(when (.isDirectory (io/file % "src/futon3c/wm"))
           (.getCanonicalPath (io/file %)))
        [(str repo "/../futon3c")
         (str (System/getProperty "user.home") "/code/futon3c")]))

(deftest every-known-production-handoff-has-one-live-index-entry
  (is (some? futon3c-repo)
      "the cross-repo index row requires a futon3c checkout beside futon2 or at ~/code/futon3c")
  (let [index (sut/read-index)
        sites (:sites index)
        audit (sut/audit repo {"futon3c" futon3c-repo})]
    (is (= :verified (:status audit)) (pr-str (:problems audit)))
    (is (= 21 (:site-count audit)))
    (is (= (count sites) (count (set (map :id sites)))))
    (is (every? #(and (:contract %) (:transport %) (:status %)) sites))
    (is (every? #(or (not= :agent-reply (:kind %))
                     (and (:representation %) (:decoder %)))
                sites))))

(deftest cross-repo-anchor-is-required-for-verification
  (let [real (io/file futon3c-repo "src/futon3c/wm/meta_self_heal_adapters.clj")
        text (slurp real)
        anchor "(defn- producer-invoke!"
        tmp (io/file (System/getProperty "java.io.tmpdir")
                     (str "handoff-index-cross-repo-" (System/nanoTime)))
        staged (io/file tmp "src/futon3c/wm/meta_self_heal_adapters.clj")]
    (.mkdirs (.getParentFile staged))
    (try
      (spit staged text)
      (let [verified (sut/audit repo {"futon3c" (.getCanonicalPath tmp)})]
        (is (= :verified (:status verified)) (pr-str (:problems verified))))
      (spit staged (str/replace text anchor "(defn- producer-invoke-RENAMED"))
      (let [refused (sut/audit repo {"futon3c" (.getCanonicalPath tmp)})]
        (is (= :refused (:status refused)))
        (is (some #(and (= :anchor-missing (:reason %))
                        (= :self-heal/producer-invoke (:site %)))
                  (:problems refused))
            (pr-str (:problems refused))))
      (let [unresolved (sut/audit repo {"futon3c" nil})]
        (is (= :refused (:status unresolved)))
        (is (some #(= :repo-unresolved (:reason %)) (:problems unresolved))
            (pr-str (:problems unresolved))))
      (finally
        (.delete staged)
        (.delete (.getParentFile staged))
        (.delete (io/file tmp "src/futon3c/wm"))
        (.delete (io/file tmp "src/futon3c"))
        (.delete (io/file tmp "src"))
        (.delete tmp)))))

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
             "src/futon2/aif/occurrence_square_authoring.clj"
             "src/futon2/aif/selected_target_reading.clj"}
           callers))
    (is (every? indexed callers))))

(deftest every-agent-reply-is-shared-or-an-explicit-exception
  (doseq [{:keys [id kind transport status exception]} (:sites (sut/read-index))
          :when (= :agent-reply kind)]
    (is (or (contains? #{:handoff/exchange :handoff/response-payload
                         :handoff/response-map} transport)
            (and (= :registered-exception status) (not (str/blank? exception))))
        (str id " bypasses the shared reply boundary without an exception"))))

(deftest every-json-agent-reply-uses-the-one-shared-decoder
  (doseq [{:keys [id representation decoder]} (:sites (sut/read-index))
          :when (= :json-object representation)]
    (is (= :handoff/response-json-object decoder)
        (str id " has an independent JSON representation rule"))))

(deftest shared-response-map-callers-cannot-appear-outside-the-index
  (let [indexed (set (map :source (:sites (sut/read-index))))
        callers (set (for [file (file-seq (io/file repo "src/futon2/aif"))
                           :when (and (.isFile file)
                                      (str/ends-with? (.getName file) ".clj"))
                           :let [relative (str (.relativize (.toPath (io/file repo))
                                                           (.toPath file)))]
                           :when (and (not= relative "src/futon2/aif/handoff_contract.clj")
                                      ;; ordinary_author_refusal re-validates
                                      ;; a RETAINED job with the shared reader
                                      ;; as a predicate; it consumes no live
                                      ;; handoff response, so it is not a
                                      ;; producer/consumer site.
                                      (not= relative "src/futon2/aif/ordinary_author_refusal.clj")
                                      (str/includes? (slurp file) "handoff/response-map"))]
                       relative))]
    (is (= #{"src/futon2/aif/full_loop_runtime.clj"} callers))
    (is (every? indexed callers))))

(deftest click-orchestration-has-no-obsolete-cast-identities
  (doseq [source ["scripts/wm_click.sh" "scripts/wm_click_repair_loop.py"]
          :let [text (->> (str/split-lines (slurp (io/file repo source)))
                          (remove #(str/starts-with? (str/trim %) "#"))
                          (str/join "\n"))]
          stale ["codex-proof2d" "codex-proof2e" "claude-1" "claude-4" "wmfix-1"]]
    (is (not (str/includes? text stale))
        (str source " still embeds obsolete cast identity " stale))))
