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
