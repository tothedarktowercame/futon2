(ns futon2.aif.finding-ticket-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.finding-ticket :as publisher]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.mission-registry :as registry]
            [futon2.aif.repair-obligation :as repair]
            [futon2.aif.ticket-queue :as queue]))

(defn- fixture [kind]
  (edn/read-string (slurp (io/file "test/fixtures/finding-ticket" (str kind ".edn")))))

(defn- file-count [root]
  (count (filter #(.isFile %) (file-seq (io/file root)))))

(defn- with-roots [f]
  (let [root (.toFile (java.nio.file.Files/createTempDirectory "finding-ticket-"
                      (make-array java.nio.file.attribute.FileAttribute 0)))
        store (io/file root "store")
        opts {:ticket-dir (str (io/file root "project/holes/tickets"))
              :queue-path (str (io/file root "project/resources/wm/ticket-queue.edn"))}]
    (.mkdir store)
    (try (f (str root) (str store) opts)
         (finally (doseq [file (reverse (file-seq root))] (io/delete-file file))))))

(defn- input [record]
  (-> record
      (assoc :repair-id (:repair/id record) :repair-class (:repair/class record)
             :outcome (:failure-outcome record) :error (:failure-error record)
             :occurrence (:repair/occurrence record))
      (dissoc :repair/id :repair/class :failure-outcome :failure-error :repair/occurrence)))

(defn- ticket-path [opts record]
  (io/file (:ticket-dir opts) (str "T-" (:repair/id record) ".md")))

(deftest real-finding-creates-an-ordinary-front-ticket
  (let [live-roots [repair/default-root "/home/joe/code/futon2/holes/tickets"]
        before (mapv file-count live-roots)]
    (with-roots
      (fn [root store opts]
        (let [record (fixture "machine")
              created (repair/record-system-failure! store (input record) opts)
              path (ticket-path opts record)
              text (slurp path)
              entry {:ticket (str "T-" (:repair/id record)) :inserted-at (:opened-at record)}
              tickets (:tickets (registry/load-tickets root))
              receipt (edn/read-string (slurp (io/file store "ticket-links" (str (:repair/id record) ".edn"))))]
          (is (= (:repair/id record) (:repair/id created)))
          (is (= 1 (count tickets)))
          (is (= :live (:status-class (first tickets))))
          (is (= (:ticket entry) (:id (first tickets))))
          (is (str/includes? text "**Status:** OPEN"))
          (is (str/includes? text "Correct explanation invalid"))
          (is (not (str/includes? text "distinct-production-shaped-successor")))
          (is (= [entry] (:entries (queue/read-declaration (:queue-path opts)))))
          (is (= [(:ticket entry)]
                 (:eligible-targets (queue/plan (queue/read-declaration (:queue-path opts))
                   [{:id {:target (:ticket entry) :precedence [:p]} :g 1.0}
                    {:id {:target "M-ordinary" :precedence [:p]} :g 0.0}] []))))
          (is (= (:ticket entry) (get-in receipt [:finding/ticket :id])))
          (is (= (identity/sha256 (identity/read-bytes (io/file store "findings" (str (:repair/id record) ".edn"))))
                 (get-in receipt [:finding/ticket :finding-sha256])))
          (is (str/includes? text (get-in receipt [:finding/ticket :finding-sha256])))
          (let [before-files (into {} (for [file (file-seq (io/file root)) :when (.isFile file)]
                                       [(str file) (identity/sha256 (identity/read-bytes file))]))]
            (repair/record-system-failure! store (input record) opts)
            (is (= before-files (into {} (for [file (file-seq (io/file root)) :when (.isFile file)]
                                           [(str file) (identity/sha256 (identity/read-bytes file))])))))
        (is (= before (mapv file-count live-roots))))))))

(deftest either-half-and-edited-ticket-recover
  (doseq [first-half [:ticket :queue]]
    (testing (name first-half)
      (with-roots
        (fn [_ store opts]
          (let [record (fixture "machine")
                path (ticket-path opts record)
                finding-path (io/file store "findings" (str (:repair/id record) ".edn"))]
            (if (= first-half :ticket)
              (is (thrown-with-msg? clojure.lang.ExceptionInfo #"crash"
                    (with-redefs [queue/enqueue! (fn [& _] (throw (ex-info "crash" {})))]
                      (repair/record-system-failure! store (input record) opts))))
              (do (io/make-parents finding-path)
                  (spit finding-path (pr-str record))
                  (queue/enqueue! (:queue-path opts) {:ticket (str "T-" (:repair/id record))
                                                     :inserted-at (:opened-at record)})))
            (when (= first-half :ticket) (spit path "# Human edit\n\n**Status:** OPEN\n"))
            (publisher/publish! store (:repair/id record) opts)
            (is (.isFile path))
            (is (= 1 (count (:entries (queue/read-declaration (:queue-path opts))))))
            (when (= first-half :ticket) (is (= "# Human edit\n\n**Status:** OPEN\n" (slurp path))))))))))

(deftest environmental-and-review-findings-publish
  (with-roots
    (fn [_ store opts]
      (let [record (fixture "environment")]
        (repair/record-system-failure! store (input record) opts)
        (is (str/includes? (slurp (ticket-path opts record)) "Verify or restore guardrail refusal"))
        (is (str/includes? (slurp (ticket-path opts record)) "dated recheck")))
      (let [record (repair/record-review-failure!
                     store {:attempt-id "ordinary-review" :target "M-example" :commit "abc"
                            :selected-entry {:target "M-example"} :reviewer "reviewer"
                            :review-job "job" :review-verdict :reject :review-text "Regression fails"
                            :opened-at "2026-09-22T00:00:00Z"} opts)]
        (is (.isFile (ticket-path opts record))))
      (is (= 2 (count (:entries (queue/read-declaration (:queue-path opts)))))))))

(deftest generic-queue-writer-preserves-other-tickets-and-refuses-conflicts
  (with-roots
    (fn [_ _ opts]
      (let [path (:queue-path opts)
            entry {:ticket "T-ordinary" :inserted-at "2026-09-22T00:00:00Z"}]
        (queue/enqueue! path entry)
        (queue/enqueue! path entry)
        (is (= [entry] (:entries (queue/read-declaration path))))
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"different insertion"
                             (queue/enqueue! path (assoc entry :inserted-at "2026-09-23T00:00:00Z"))))
        (doseq [result (mapv deref (mapv #(future (queue/enqueue! path {:ticket (str "T-" %)
                                                                      :inserted-at "2026-09-22T01:00:00Z"}))
                                         (range 5)))]
          (is (map? result)))
        (is (= 6 (count (:entries (queue/read-declaration path)))))
        (spit path "{:schema :wrong}")
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid ticket queue"
                             (queue/enqueue! path {:ticket "T-next" :inserted-at "2026-09-22T00:00:00Z"})))
        (is (= "{:schema :wrong}" (slurp path)))))))

(deftest isolated-store-boundary-publishes-without-extra-configuration
  (with-roots
    (fn [_ store _]
      (let [record (fixture "machine")
            opts (publisher/destinations store)]
        (repair/record-system-failure! store (input record))
        (is (.isFile (ticket-path opts record)))
        (is (.isFile (io/file (:queue-path opts))))))))

(deftest linked-publication-path-refuses-without-changing-destination
  (with-roots
    (fn [root _ opts]
      (let [outside (io/file root "untouched.edn")
            path (io/file (:queue-path opts))]
        (spit outside "unchanged")
        (io/make-parents path)
        (java.nio.file.Files/createSymbolicLink (.toPath path) (.toPath outside)
          (make-array java.nio.file.attribute.FileAttribute 0))
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"traverses a link"
              (queue/enqueue! path {:ticket "T-ordinary" :inserted-at "2026-09-22T00:00:00Z"})))
        (is (= "unchanged" (slurp outside)))))))
