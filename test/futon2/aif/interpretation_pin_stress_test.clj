(ns futon2.aif.interpretation-pin-stress-test
  "pin! under a lost CREATE_NEW race (follow-up to 749c5a87). The bounded
  re-read (5 x 2 ms) gave up while the winner was still writing and refused
  :interpretation/source-changed for matching bytes — reproduced 2026-09-30
  as flight-ask-test/what-is-not-a-publication-is-a-need seeing
  #{:request-refused :not-answered} once in 5 combined-namespace runs. pin!
  now writes the snapshot to a temp file and ATOMICALLY MOVES it into place,
  so a visible snapshot is always complete and the loser's single digest
  read is sound."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.interpretation-request :as ireq])
  (:import [java.nio.file Files StandardOpenOption]
           [java.nio.file.attribute FileAttribute]
           [java.util.concurrent CountDownLatch]))

(defn- temp-dir [prefix]
  (.toFile (Files/createTempDirectory prefix (make-array FileAttribute 0))))

(defn- pin-quite [dir path]
  (try {::ok (#'ireq/pin! dir path (constantly "fixture-rev"))}
       (catch clojure.lang.ExceptionInfo e {::refused (:interpretation/refusal (ex-data e))})))

(defn- stress-pin!
  "N threads pin the same NEW source path (BODY bytes, big enough that the
  writer takes real time), released together; returns the outcome of every
  thread."
  [n body]
  (let [dir (temp-dir "pin-stress")
        snaps (io/file dir "snaps")]
    (.mkdirs snaps)
    (let [src (io/file dir "M-stress.md")]
      (spit src body)
      (let [latch (CountDownLatch. 1)
            run! (fn []
                   (.await latch)
                   (pin-quite (.getCanonicalPath snaps) (.getCanonicalPath src)))
            futures (doall (repeatedly n #(future (run!))))]
        (.countDown latch)
        {:results (mapv deref futures)
         :snaps snaps
         :src src
         :dir dir}))))

(defn- big-body []
  ;; ~2 MB: the winner's write of the snapshot takes real time, so a
  ;; non-atomic scheme has a wide visibility window
  (str "# M-stress\n\n" (apply str (repeat 100000 "0123456789abcdefghij")) "\n"))

(deftest many-threads-pinning-one-new-path-never-refuse
  (testing "32 threads, ~2 MB source: zero refusals, one shared complete snapshot"
    (let [{:keys [results snaps dir]} (stress-pin! 32 (big-body))
          refusals (vec (keep ::refused results))
          oks (keep ::ok results)
          snapshots (distinct (map :snapshot oks))]
      (is (= [] refusals) (pr-str refusals))
      (is (= 32 (count oks)))
      (is (= 1 (count snapshots)) "every thread got the same snapshot")
      (is (= (evidence/sha256 (.getBytes (slurp (first snapshots)) "UTF-8"))
             (evidence/sha256 (.getBytes (slurp (io/file dir "M-stress.md")) "UTF-8")))
          "the snapshot is complete")
      (is (empty? (filter #(.startsWith (.getName %) ".") (.listFiles snaps)))
          "no temp files left behind"))))

(deftest a-genuinely-mismatched-snapshot-still-refuses
  (let [dir (temp-dir "pin-mismatch")
        snaps (.getCanonicalPath (doto (io/file dir "snaps") (.mkdirs)))
        src (io/file dir "M-mismatch.md")]
    (spit src "# original bytes\n")
    (let [first {::ok (#'ireq/pin! snaps (.getCanonicalPath src) (constantly "fixture-rev"))}]
      (is (contains? first ::ok))
      ;; corrupt the snapshot on disk; a later pin of the same source must refuse
      (spit (io/file (:snapshot (::ok first))) "# corrupted snapshot bytes\n")
      (is (= :interpretation/source-changed
             (::refused (pin-quite snaps (.getCanonicalPath src))))))))

;; ---------------------------------------------------------------------------
;; PLANT: the replaced algorithm (749c5a87's bounded re-read) refuses the
;; same stress. Faithful copy of the old pin! write path, with the visibility
;; window made explicit: the file is CREATED before the winner's bytes land.

(defn- pin-plant!
  "The pre-fix algorithm: CREATE_NEW the destination directly, and on losing
  the race re-read the digest at most 5 times, 2 ms apart."
  [dir path]
  (let [bs (Files/readAllBytes (.toPath (io/file path)))
        digest (evidence/sha256 bs)
        name (str (evidence/value-digest path) "-" digest ".source")
        dest (io/file dir name)]
    (if (.exists dest)
      (when-not (= digest (evidence/sha256 (Files/readAllBytes (.toPath dest))))
        (throw (ex-info "refused" {:interpretation/refusal :interpretation/source-changed})))
      (try
        ;; the winner creates the file and only then writes its bytes:
        ;; CREATE_NEW makes it visible immediately
        (Files/write (.toPath dest) (byte-array 0)
                     (into-array StandardOpenOption [StandardOpenOption/CREATE_NEW
                                                     StandardOpenOption/WRITE]))
        (Thread/sleep 20)
        (Files/write (.toPath dest) bs
                     (into-array StandardOpenOption [StandardOpenOption/WRITE
                                                     StandardOpenOption/TRUNCATE_EXISTING]))
        (catch java.nio.file.FileAlreadyExistsException _
          (when-not (some true? (repeatedly 5
                                          (fn []
                                            (Thread/sleep 2)
                                            (= digest (evidence/sha256
                                                       (Files/readAllBytes (.toPath dest)))))))
            (throw (ex-info "refused"
                            {:interpretation/refusal :interpretation/source-changed}))))))))

(deftest plant-bounded-reread-refuses-under-the-same-stress
  (testing "the old algorithm's 5 x 2 ms re-read gives up while the winner
            is still writing: the same stress refuses"
    (let [dir (temp-dir "pin-plant")
          snaps (.getCanonicalPath (doto (io/file dir "snaps") (.mkdirs)))
          src (io/file dir "M-plant.md")
          body (big-body)]
      (spit src body)
      (let [latch (CountDownLatch. 1)
            outcomes (atom [])
            run! (fn []
                   (.await latch)
                   (swap! outcomes conj
                          (try (pin-plant! snaps (.getCanonicalPath src)) ::ok
                              (catch clojure.lang.ExceptionInfo e
                                (:interpretation/refusal (ex-data e))))))
            futures (doall (repeatedly 32 #(future (run!))))]
        (.countDown latch)
        (doseq [f futures] (deref f))
        (is (some #(= :interpretation/source-changed %) @outcomes)
            (str "plant outcomes: " (frequencies @outcomes)
                 " — if none refused, the plant lost its teeth"))))))
