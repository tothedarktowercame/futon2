(ns futon2.aif.interoceptive-manifest-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.interoceptive-commitment :as commitment]
            [futon2.aif.interoceptive-manifest :as manifest]
            [futon2.aif.interoceptive-store-lock :as store-lock]
            [futon2.aif.repair-obligation :as repair]
            [futon2.aif.tripwire :as tripwire])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn roots []
  (let [base (.toFile (Files/createTempDirectory
                       "interoceptive-manifest-"
                       (make-array FileAttribute 0)))
        trips (doto (java.io.File. base "trips") .mkdir)
        repair (doto (java.io.File. base "repair") .mkdir)]
    (doseq [child manifest/repair-children]
      (.mkdir (java.io.File. repair child)))
    [trips repair]))

(defn write! [root relative value]
  (let [f (java.io.File. root relative)]
    (spit f (str (pr-str value) "\n")) f))

(defn refusal [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))

(defn wait-for-file! [file]
  (loop [n 100]
    (cond (.exists ^java.io.File file) true
          (zero? n) false
          :else (do (Thread/sleep 25) (recur (dec n))))))

(deftest valid-production-shaped-test-history
  (let [[trips repair] (roots)
        id "trip-valid" rid "repair-trip-valid"]
    (write! trips (str id ".edn")
            {:trip/id id :trip/schema-version 1 :trip/action :stop-line})
    (write! (java.io.File. repair "findings") (str rid ".edn")
            {:repair/id rid :repair/status :open :failure-data {:trip/id id}})
    (write! (java.io.File. repair "implementations") (str rid ".edn")
            {:repair/id rid :repair/status :awaiting-validation})
    (write! (java.io.File. repair "resolutions") (str rid ".edn")
            {:repair/id rid :repair/status :resolved})
    (let [result (manifest/test-snapshot (.getPath trips) (.getPath repair))]
      (is (:stable-census? result))
      (is (= 4 (count (:manifest result))))
      (is (= 1 (get-in result [:snapshot :machine-confidence])))
      (is (= :test-root (get-in result [:snapshot :excluded 0 :reason]))))))

(deftest reader-and-authority-refusals
  (testing "missing directory and production-label spoof"
    (let [[trips repair] (roots)]
      (is (= :interoceptive/directory-unavailable
             (refusal #(manifest/capture "/does/not/exist" (.getPath repair) :test))))
      (is (= :interoceptive/authority-spoof
             (refusal #(manifest/capture (.getPath trips) (.getPath repair)
                                         :production))))))
  (testing "trailing EDN and unexpected structure"
    (let [[trips repair] (roots)]
      (spit (java.io.File. trips "bad.edn") "{} {}")
      (is (= :interoceptive/not-one-edn-form
             (refusal #(manifest/capture (.getPath trips) (.getPath repair) :test)))))
    (let [[trips repair] (roots)]
      (.mkdir (java.io.File. trips "unexpected"))
      (is (= :interoceptive/unexpected-structural-entry
             (refusal #(manifest/capture (.getPath trips) (.getPath repair) :test))))))
  (testing "membership and content changes refuse"
    (let [[trips repair] (roots)
          late (java.io.File. trips "late.edn")]
      (binding [manifest/*after-capture-hook*
                #(spit late "{:trip/id \"late\" :trip/schema-version 1 :trip/action :record}\n")]
        (is (= :interoceptive/store-changed-during-capture
               (refusal #(manifest/capture (.getPath trips) (.getPath repair) :test))))))
    (let [[trips repair] (roots)
          f (write! trips "one.edn"
                    {:trip/id "one" :trip/schema-version 1 :trip/action :record})]
      (binding [manifest/*after-capture-hook* #(spit f "{}\n")]
        (is (= :interoceptive/store-changed-during-capture
               (refusal #(manifest/capture (.getPath trips) (.getPath repair) :test))))))))

(deftest coordination-reentrancy-and-release
  (let [[trips _] (roots)
        lock-path (str (.getPath trips) "/coordination.lock")]
    (binding [store-lock/*lock-path* lock-path]
      (is (= :nested (store-lock/with-store-lock
                      #(store-lock/with-store-lock (constantly :nested)))))
      (is (thrown? Exception
                   (store-lock/with-store-lock
                    #(throw (Exception. "commissioned failure")))))
      (is (= :released
             (store-lock/with-store-lock (constantly :released)))))))

(deftest thread-ownership-and-lock-identity
  (let [[trips _] (roots)
        lock-a (str (.getPath trips) "/a.lock")
        lock-b (str (.getPath trips) "/b.lock")]
    (binding [store-lock/*lock-path* lock-a]
      (store-lock/with-store-lock
       (fn []
         (is (= :interoceptive/lock-contention
                @(future (refusal #(store-lock/with-store-lock identity))))))))
    (binding [store-lock/*lock-path* lock-a]
      (is (= :different-lock-acquired
             (store-lock/with-store-lock
              #(binding [store-lock/*lock-path* lock-b]
                 (store-lock/with-store-lock (constantly :different-lock-acquired)))))))
    (is (= :released-after-thread-contention
           (binding [store-lock/*lock-path* lock-a]
             (store-lock/with-store-lock (constantly :released-after-thread-contention)))))))

(deftest fresh-root-production-writer-apis
  (let [base (.toFile (Files/createTempDirectory "writer-apis-"
                                                  (make-array FileAttribute 0)))
        trips (java.io.File. base "fresh-trips")
        repairs (doto (java.io.File. base "fresh-repairs") .mkdir)
        trip (tripwire/write-trip-report!
              (.getPath trips) {:trip/id "trip-api" :trip/action :stop-line})
        finding (repair/record-system-failure!
                 (.getPath repairs)
                  {:attempt-id "attempt-api" :repair-id "repair-api"
                  :repair-class :machine-failure :failure-stage :test
                  :outcome :failed :error "commissioned"
                  :failure-data {:trip/id "trip-api"}})
        implementation (repair/record-implementation!
                        (.getPath repairs) finding
                        {:attempt-id "implementation-api" :commit "abc"
                         :reviewer :reviewer :review-job "review-api"
                         :witness {:resolved? true :dial-moved? true}})
        resolution (repair/resolve!
                    (.getPath repairs) (assoc finding :repair/implementation implementation)
                    {:attempt-id "validation-api" :commit "abc"
                     :reviewer :reviewer :review-job "review-api-2"
                     :witness {:resolved? true :dial-moved? true}
                     :validation {:production-shaped? true}})]
    (is (.isFile (java.io.File. trip)))
    (is (= :open (:repair/status finding)))
    (is (= :awaiting-validation (:repair/status implementation)))
    (is (= :resolved (:repair/status resolution)))
    (is (= 1 (get-in (manifest/test-snapshot (.getPath trips) (.getPath repairs))
                     [:snapshot :machine-confidence])))
    (let [input (:constructor-input
                 (manifest/capture (.getPath trips) (.getPath repairs) :test))
          production-input (-> input
                               (assoc-in [:trip-authority :authority-class] :production)
                               (assoc-in [:repair-authority :authority-class] :production))]
      (is (= 1 (:machine-confidence
                (commitment/confidence-snapshot production-input)))))))

(deftest symlink-and-participation-refusals
  (let [[trips repair-root] (roots)
        linked (java.io.File. (.getParentFile trips) "linked")]
    (Files/createSymbolicLink (.toPath linked) (.toPath trips)
                              (make-array java.nio.file.attribute.FileAttribute 0))
    (is (= :interoceptive/source-path-refused
           (refusal #(manifest/capture (.getPath linked) (.getPath repair-root) :test))))
    (is (= :interoceptive/activation-lease-unavailable
           (refusal manifest/production-manifest!)))))

(deftest partial-logical-publication-refuses
  (let [[trips repairs] (roots)
        _ (tripwire/write-trip-report!
           (.getPath trips) {:trip/id "trip-without-finding" :trip/action :stop-line})
        input (:constructor-input
               (manifest/capture (.getPath trips) (.getPath repairs) :test))
        production-input (-> input
                             (assoc-in [:trip-authority :authority-class] :production)
                             (assoc-in [:repair-authority :authority-class] :production))]
    (is (= :interoceptive/missing-finding-join
           (refusal #(commitment/confidence-snapshot production-input))))))

(deftest cross-process-exclusion
  (let [[trips _] (roots)
        lock-path (str (.getPath trips) "/process.lock")
        ready (java.io.File. trips "child-ready")
        expression (str "(require '[futon2.aif.interoceptive-store-lock :as l])"
                        "(binding [l/*lock-path* " (pr-str lock-path) "]"
                        " (l/with-store-lock #(do (spit " (pr-str (.getPath ready))
                        " \"ready\") (Thread/sleep 1500))))")
        process (.start (ProcessBuilder.
                         (into-array String
                                     ["java" "-cp" (System/getProperty "java.class.path")
                                      "clojure.main" "-e" expression])))]
    (try
      (is (wait-for-file! ready))
      (is (= :interoceptive/lock-contention
             (binding [store-lock/*lock-path* lock-path]
               (refusal #(store-lock/with-store-lock identity)))))
      (is (zero? (.waitFor process)))
      (is (= :acquired-after-child
             (binding [store-lock/*lock-path* lock-path]
               (store-lock/with-store-lock (constantly :acquired-after-child)))))
      (finally (.destroyForcibly process)))))

(deftest normalized-store-lock-selection
  (with-redefs-fn {#'store-lock/with-lock-path (fn [path _] path)}
    (fn []
      (is (= store-lock/default-lock-path
             (store-lock/with-store-lock-for
              "/home/joe/code/./futon2/data/wm-tripwires/trips" identity)))
      (is (= store-lock/default-lock-path
             (store-lock/with-store-lock-for
              "/home/joe/code/futon2/data/wm-repair-obligations/../wm-tripwires/trips"
              identity))))))

(deftest publication-lock-symlink-refuses
  (let [[trips repair-root] (roots)
        target (write! trips "one.edn"
                       {:trip/id "one" :trip/schema-version 1 :trip/action :record})
        link (java.io.File. repair-root "findings/.publication.lock")]
    (Files/createSymbolicLink (.toPath link) (.toPath target)
                             (make-array FileAttribute 0))
    (is (= :interoceptive/unexpected-structural-entry
           (refusal #(manifest/capture (.getPath trips) (.getPath repair-root) :test))))))
