(ns futon2.aif.mission-registry-substrate-test
  "2026-09-17 half 2: the zero-arg registry read points at substrate-2 and
   refuses typed rather than falling back to the filesystem."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.mission-registry :as mr]
            [futon2.aif.substrate :as substrate]))

(defn- entity
  [id props]
  {:entity/external-id id
   :entity/name (str "mission|" id)
   :entity/type :mission
   :entity/props props})

(defn- with-substrate-entities
  [entities f]
  (with-redefs [substrate/entities-by-type (fn [& _] entities)]
    (f)))

(deftest zero-arg-load-reads-substrate-test
  (with-substrate-entities
    [(entity "M-alpha"
             {:mission/status-class "active"
              :mission/title "Alpha"
              :mission/status-line "ACTIVE"
              :mission/open-hole-count 2
              :provenance/repo "futon3"
              :provenance/path "/x/y/M-alpha.md"
              :provenance/sha256 "abc"})]
    (fn []
      (let [entries (:missions (mr/load-missions))]
        (is (= 1 (count entries)))
        (let [m (first entries)]
          (is (= "M-alpha" (:id m)))
          (is (= :active (:status-class m)))
          (is (= "Alpha" (:title m)))
          (is (= "/x/y/M-alpha.md" (:path m)))
          (is (= 2 (:open-hole-count m)))
          (is (mr/live-mission? m)))))))

(deftest missing-status-class-is-unknown-not-defaulted-test
  (with-substrate-entities
    [(entity "M-hinge" {})]
    #(is (= :unknown (:status-class (first (:missions (mr/load-missions))))))))

(deftest substrate-reader-excludes-derived-mission-ids-like-file-reader
  (let [props (fn [id]
                {:provenance/path (str "/home/joe/code/futon0/holes/missions/" id ".md")
                 :provenance/sha256 (apply str (repeat 64 "a"))
                 :mission/status-class "active"})]
    (with-substrate-entities
      [(entity "M-parent" (props "M-parent"))
       (entity "M-parent.derived" (props "M-parent.derived"))]
      #(is (= ["M-parent"]
              (mapv :id (:missions (mr/load-missions-from-substrate))))))))

(deftest pathless-foreign-mission-is-typed-and-not-live-test
  (with-substrate-entities
    [(entity "M-ukrns-wp"
             {:scope/role "mission" :mission/relation "relates-to"})]
    (fn []
      (let [mission (first (:missions (mr/load-missions)))]
        (is (false? (boolean (mr/live-mission? mission))))
        (is (= :pathless-foreign-entity
               (:mission/unselectable-reason mission)))))))

(deftest unreachable-substrate-is-typed-refusal-test
  (with-redefs [substrate/entities-by-type
                (fn [& _] (throw (ex-info "conn" {})))]
    (try (mr/load-missions)
         (is false "must throw")
         (catch clojure.lang.ExceptionInfo e
           (is (= :substrate-unreachable (:kind (ex-data e))))))))

(deftest empty-substrate-is-typed-refusal-test
  (with-redefs [substrate/entities-by-type (fn [& _] [])]
    (try (mr/load-missions)
         (is false "must throw")
         (catch clojure.lang.ExceptionInfo e
           (is (= :substrate-mission-registry-empty (:kind (ex-data e))))))))

(deftest one-arg-load-is-still-the-explicit-file-scan-test
  (let [calls (atom [])]
    (with-redefs [mr/load-missions-from-files
                  (fn [root] (swap! calls conj root) {:missions [:fake-files]})]
      (let [doc (mr/load-missions "/tmp/never-scanned-here")]
        (is (= [:fake-files] (:missions doc)))
        (is (= ["/tmp/never-scanned-here"] @calls))))))

;; ---- record writer: the caller that omits :existing (the watcher lane) ----
;; Added 2026-09-17 by claude-4 in review of bc400322. The lane calls
;; `upsert-mission-record!` with `{:path path}` and nothing else, which used
;; to mean "there is no existing entity": foreign props were replaced instead
;; of merged, :unchanged was unreachable so every doc-land wrote, and
;; :entity/source was overwritten each pass.

(defn- write-mission-doc!
  "A mission doc at a path the registry contract admits, under a tmp root."
  [root id status]
  (let [dir (io/file root "somerepo" "holes" "missions")]
    (.mkdirs dir)
    (let [f (io/file dir (str id ".md"))]
      (spit f (str "# " id "\n\nStatus: " status "\n"))
      (.getAbsolutePath f))))

(deftest omitted-existing-is-looked-up-and-foreign-props-survive-test
  (let [root (str (java.nio.file.Files/createTempDirectory
                   "mission-record" (make-array java.nio.file.attribute.FileAttribute 0)))
        path (write-mission-doc! root "M-zeta" "ACTIVE")
        stored (atom nil)
        writes (atom 0)]
    ;; First pass: nothing in the store yet.
    (with-redefs [substrate/entities-by-type (fn [& _] [])
                  substrate/put-doc! (fn [doc & _] (reset! stored doc) doc)]
      (is (= :created (:status (mr/upsert-mission-record! {:code-root root :path path})))))
    ;; …as the store would hand it back, plus state this writer does not own.
    (let [entity (assoc @stored
                        :entity/id "uuid-1"
                        :entity/source "hinge-log-bridge"
                        :entity/props (assoc (:entity/props @stored) :foreign/keep "yes"))]
      ;; Second pass, same unedited file, :existing still omitted: no write.
      (with-redefs [substrate/entities-by-type (fn [& _] [entity])
                    substrate/put-doc! (fn [doc & _] (swap! writes inc) doc)]
        (is (= :unchanged (:status (mr/upsert-mission-record! {:code-root root :path path})))
            "an unchanged doc must not write on every land")
        (is (zero? @writes) "and must not reach the store at all"))
      ;; Third pass, the doc's Status edited: it writes, and carries the
      ;; foreign state through instead of replacing it.
      (write-mission-doc! root "M-zeta" "COMPLETE")
      (with-redefs [substrate/entities-by-type (fn [& _] [entity])
                    substrate/put-doc! (fn [doc & _] (reset! stored doc) doc)]
        (is (= :updated (:status (mr/upsert-mission-record! {:code-root root :path path}))))
        (is (= "complete" (:mission/status-class (:entity/props @stored)))
            "the edited status is what the machine now reads")
        (is (= "yes" (:foreign/keep (:entity/props @stored)))
            "a prop this writer does not own must survive the refresh")
        (is (= "uuid-1" (:entity/id @stored))
            "the entity id must stay stable across refreshes")
        (is (= "hinge-log-bridge" (:entity/source @stored))
            "an existing entity's source must be carried through, not dropped")))))

(deftest explicit-existing-nil-is-taken-as-given-test
  ;; A corpus run shares one index and passes :existing, including nil. That
  ;; answer is authoritative: no per-record lookup is issued.
  (let [root (str (java.nio.file.Files/createTempDirectory
                   "mission-record" (make-array java.nio.file.attribute.FileAttribute 0)))
        path (write-mission-doc! root "M-eta" "ACTIVE")
        lookups (atom 0)]
    (with-redefs [substrate/entities-by-type (fn [& _] (swap! lookups inc) [])
                  substrate/put-doc! (fn [doc & _] doc)]
      (is (= :created (:status (mr/upsert-mission-record!
                                {:code-root root :path path :existing nil}))))
      (is (zero? @lookups) "an explicit :existing must not trigger a lookup"))))


(deftest top-level-upsert-is-admitted-without-substrate-io
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                      "mission-top-level-upsert" (make-array java.nio.file.attribute.FileAttribute 0)))
        file (io/file root "primary" "holes" "M-top-level.md")]
    (try
      (io/make-parents file)
      (.mkdirs (io/file root "primary" ".git"))
      (spit file "# Top-level mission\nStatus: ACTIVE\n")
      (let [path (.getAbsolutePath file)
            entry (#'mr/mission-doc->entry path)
            props (into {} (remove (comp nil? val)) (mr/mission-record-props (str root) entry))]
        (with-redefs [substrate/entities-by-type
                      (fn [& _] (throw (ex-info "test must not read substrate" {})))
                      substrate/put-doc!
                      (fn [& _] (throw (ex-info "test must not write substrate" {})))]
          (is (= {:id "M-top-level" :status :unchanged}
                 (mr/upsert-mission-record! {:code-root (str root) :path path
                                             :existing {:entity/props props}})))
          (is (= :path-not-admitted
                 (:reason (mr/upsert-mission-record!
                           {:path (str (io/file root "primary" "notes" "M-top-level.md"))
                            :existing nil}))))))
      (finally
        (doseq [f (reverse (file-seq root))] (io/delete-file f true))))))

(deftest consecutive-refreshes-observe-the-new-residual-mission-state
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                       "mission-two-tick" (make-array java.nio.file.attribute.FileAttribute 0)))
        repo (io/file root "primary")
        file (io/file repo "holes" "M-two-tick.md")
        store (atom {})
        index-fn (fn [] @store)
        upsert-fn (fn [{:keys [code-root path]}]
                    (let [entry (#'mr/mission-doc->entry path)
                          entity {:entity/external-id (:id entry)
                                  :entity/props (mr/mission-record-props code-root entry)}]
                      (swap! store assoc (:id entry) entity)
                      {:id (:id entry) :status :updated}))]
    (try
      (.mkdirs (io/file repo ".git"))
      (io/make-parents file)
      (spit file "# Two tick\n\nStatus: ACTIVE\n\n- [ ] residual work\n")
      (let [first-refresh (mr/refresh-mission-substrate!
                           {:code-root (str root) :index-fn index-fn :upsert-fn upsert-fn})]
        (is (= :established (:status first-refresh)))
        (is (= ["- [ ] residual work"]
               (mapv :text (get-in @store ["M-two-tick" :entity/props :mission/open-holes])))))
      ;; Tick one's grounded commit satisfies the only want. Tick two must
      ;; reconstruct from these bytes, not replay the old unchecked source.
      (spit file "# Two tick\n\nStatus: COMPLETE\n\n- [x] residual work\n")
      (mr/refresh-mission-substrate!
       {:code-root (str root) :index-fn index-fn :upsert-fn upsert-fn})
      (is (= "complete" (get-in @store ["M-two-tick" :entity/props :mission/status-class])))
      (is (nil? (get-in @store ["M-two-tick" :entity/props :mission/open-holes]))
          "the identical satisfied cascade has no residual want to replay")
      (finally
        (doseq [f (reverse (file-seq root))] (io/delete-file f true))))))

(deftest refresh-refuses-when-readback-does-not-carry-the-scanned-revision
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                       "mission-stale" (make-array java.nio.file.attribute.FileAttribute 0)))
        repo (io/file root "primary")
        file (io/file repo "holes" "M-stale.md")]
    (try
      (.mkdirs (io/file repo ".git"))
      (io/make-parents file)
      (spit file "# Stale\n\nStatus: ACTIVE\n\n- [ ] work\n")
      (let [entry (#'mr/mission-doc->entry (.getAbsolutePath file))
            props (assoc (mr/mission-record-props (str root) entry)
                         :mission/classifier-sha256 "stale-classifier")]
        (try
          (mr/refresh-mission-substrate!
           {:code-root (str root)
            :index-fn (constantly {"M-stale" {:entity/props props}})
            :upsert-fn (constantly {:id "M-stale" :status :updated})})
          (is false "a readback with the file sha but stale classifier must refuse")
          (catch clojure.lang.ExceptionInfo e
            (is (= :mission-substrate-freshness-unestablished (:kind (ex-data e)))))))
      (finally
        (doseq [f (reverse (file-seq root))] (io/delete-file f true))))))

(defn- retry-fixture []
  (let [file (java.io.File/createTempFile "mission-refresh-race" ".md")
        _ (spit file "# Race\n\nStatus: ACTIVE\n")
        entry {:id "M-race" :path (.getCanonicalPath file)}
        props {:provenance/sha256 (#'mr/sha256-file (.getCanonicalPath file))
               :mission/classifier-sha256 mr/mission-classifier-sha256}]
    {:file file :entry entry
     :clean {"M-race" {:entity/props props}}}))

(deftest refresh-retries-one-stale-race-and-establishes
  (let [{:keys [file entry clean]} (retry-fixture)
        scans (atom 0) indexes (atom 0) upserts (atom 0)
        report (mr/refresh-mission-substrate!
                {:code-root "/fixture"
                 :scan-fn (fn [_] (swap! scans inc) {:missions [entry]})
                 :index-fn (fn [] (case (swap! indexes inc) 4 clean {}))
                 :upsert-fn (fn [_] (swap! upserts inc)
                              {:id "M-race" :status :updated})})]
    (is (= :established (:status report)))
    (is (= 1 (:retried-stale report)))
    (is (= 2 @scans))
    (is (= 2 @upserts))
    (.delete file)))

(deftest refresh-retry-counts-each-mission-once
  ;; Review of a0e3de410: the first pass writes, the retry reads the same
  ;; mission back :unchanged. The report counts it once, as :updated.
  (let [{:keys [file entry clean]} (retry-fixture)
        indexes (atom 0) upserts (atom 0)
        report (mr/refresh-mission-substrate!
                {:code-root "/fixture"
                 :scan-fn (fn [_] {:missions [entry]})
                 :index-fn (fn [] (case (swap! indexes inc) 4 clean {}))
                 :upsert-fn (fn [_] {:id "M-race"
                                     :status (if (= 1 (swap! upserts inc)) :updated :unchanged)})})]
    (is (= :established (:status report)))
    (is (= 1 (:scanned report)))
    (is (= [1 0 0] ((juxt :updated :unchanged :created) report)))
    (.delete file)))

(deftest refresh-stale-twice-throws-after-exactly-two-passes
  (let [{:keys [file entry]} (retry-fixture)
        scans (atom 0)]
    (try
      (mr/refresh-mission-substrate!
       {:code-root "/fixture"
        :scan-fn (fn [_] (swap! scans inc) {:missions [entry]})
        :index-fn (constantly {})
        :upsert-fn (constantly {:id "M-race" :status :updated})})
      (is false "persistent stale readback must throw")
      (catch clojure.lang.ExceptionInfo e
        (is (= :mission-substrate-freshness-unestablished (:kind (ex-data e))))
        (is (= 2 @scans))))
    (.delete file)))

(deftest refresh-upsert-error-does-not-retry
  (let [{:keys [file entry]} (retry-fixture)
        scans (atom 0)]
    (try
      (mr/refresh-mission-substrate!
       {:code-root "/fixture"
        :scan-fn (fn [_] (swap! scans inc) {:missions [entry]})
        :index-fn (constantly {})
        :upsert-fn (constantly {:id "M-race" :status :error})})
      (is false "upsert error must throw")
      (catch clojure.lang.ExceptionInfo e
        (is (= :mission-substrate-freshness-unestablished (:kind (ex-data e))))
        (is (= 1 @scans))))
    (.delete file)))

(deftest refresh-rederives-when-classifier-digest-changes
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                       "mission-classifier-refresh"
                       (make-array java.nio.file.attribute.FileAttribute 0)))
        repo (io/file root "primary")
        stale-file (io/file repo "holes" "M-stale-classifier.md")
        current-file (io/file repo "holes" "M-current-classifier.md")
        store (atom {})]
    (try
      (.mkdirs (io/file repo ".git"))
      (io/make-parents stale-file)
      (spit stale-file "# Stale classifier\n\nStatus: ACTIVE\n")
      (spit current-file "# Current classifier\n\nStatus: ACTIVE\n")
      (doseq [file [stale-file current-file]]
        (let [entry (#'mr/mission-doc->entry (.getAbsolutePath file))
              props (mr/mission-record-props (.getAbsolutePath root) entry)]
          (swap! store assoc (:id entry)
                 {:entity/external-id (:id entry)
                  :entity/props (cond-> props
                                  (= file stale-file)
                                  (assoc :mission/classifier-sha256 "superseded"))})))
      (with-redefs [substrate/put-doc!
                    (fn [doc & _]
                      (swap! store assoc (:entity/external-id doc) doc)
                      doc)]
        (let [report (mr/refresh-mission-substrate!
                      {:code-root (.getAbsolutePath root)
                       :index-fn #(deref store)})]
          (is (= 1 (:reclassified report)))
          (is (= 1 (:updated report)))
          (is (= 1 (:unchanged report)))
          (is (= mr/mission-classifier-sha256
                 (get-in @store ["M-stale-classifier" :entity/props
                                 :mission/classifier-sha256])))))
      (finally
        (doseq [f (reverse (file-seq root))] (io/delete-file f true))))))

(deftest refresh-retracts-a-stale-gate-the-current-rules-do-not-emit
  ;; Bad case from 2026-11-01: 15 missions kept :mission/operator-gated? true
  ;; (pre-a1419060a Status-prose rule) through a refresh that reported them
  ;; reclassified, because the builder drops nil keys and the merge kept the
  ;; old value. A non-gated file must come out of the refresh with no gate.
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                       "mission-stale-gate"
                       (make-array java.nio.file.attribute.FileAttribute 0)))
        repo (io/file root "primary")
        file (io/file repo "holes" "M-stale-gate.md")
        store (atom {})]
    (try
      (.mkdirs (io/file repo ".git"))
      (io/make-parents file)
      (spit file "# Stale gate\n\n**Status:** OPEN — awaiting Joe\n")
      (let [entry (#'mr/mission-doc->entry (.getAbsolutePath file))
            props (mr/mission-record-props (.getAbsolutePath root) entry)]
        (is (not (:operator-gated? entry)) "the current rules do not gate on Status prose")
        (swap! store assoc (:id entry)
               {:entity/external-id (:id entry)
                :entity/props (assoc props
                                     :mission/classifier-sha256 "superseded"
                                     :mission/operator-gated? true
                                     :mission/operator-gate-lines ["**Status:** OPEN — awaiting Joe"]
                                     :foreign/kept "stays")}))
      (with-redefs [substrate/put-doc!
                    (fn [doc & _]
                      (swap! store assoc (:entity/external-id doc) doc)
                      doc)]
        (mr/refresh-mission-substrate!
         {:code-root (.getAbsolutePath root) :index-fn #(deref store)})
        (let [props (get-in @store ["M-stale-gate" :entity/props])]
          (is (not (contains? props :mission/operator-gated?)))
          (is (not (contains? props :mission/operator-gate-lines)))
          (is (= "stays" (:foreign/kept props)) "foreign props survive the retraction")
          (is (= mr/mission-classifier-sha256 (:mission/classifier-sha256 props)))))
      (finally
        (doseq [f (reverse (file-seq root))] (io/delete-file f true))))))

(deftest all-checked-open-mission-is-excluded-after-the-substrate-round-trip
  ;; WANTSEMPTY-2 review: the selector reads missions from substrate, not
  ;; files, so the exclusion must survive ingest -> store -> substrate read.
  (let [root (str (java.nio.file.Files/createTempDirectory
                   "mission-empty-contract" (make-array java.nio.file.attribute.FileAttribute 0)))
        dir (doto (io/file root "somerepo" "holes" "missions") .mkdirs)
        write! (fn [id body] (let [f (io/file dir (str id ".md"))]
                               (spit f (str "# " id "\n\n**Status:** OPEN\n\n## Acceptance checklist\n\n" body))
                               (.getAbsolutePath f)))
        store (atom {})]
    (doseq [path [(write! "M-checked" "- [x] first\n- [x] second\n")
                  (write! "M-unchecked" "- [x] first\n- [ ] second\n")]]
      (with-redefs [substrate/entities-by-type (fn [& _] [])
                    substrate/put-doc! (fn [doc & _] (swap! store assoc (:entity/external-id doc) doc) doc)]
        (mr/upsert-mission-record! {:code-root root :path path})))
    (with-substrate-entities
      (vec (vals @store))
      #(let [loaded (mr/load-missions-from-substrate)]
         (is (= ["M-unchecked"] (mapv :id (mr/open-missions loaded))))
         (is (= [["M-checked" :open-mission-without-wants]]
                (mapv (juxt :id :reason) (:exclusions (mr/open-mission-census loaded)))))))))
