(ns futon2.aif.machine-slow-feedback-store-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.machine-slow-feedback-store :as store])
  (:import (java.nio.file Files StandardOpenOption)
           (java.nio.file.attribute FileAttribute)))

(def authority {:verifier/source-sha256 (apply str (repeat 64 "a"))
                :evidence-source-sha256s {:outcome (apply str (repeat 64 "b"))}})
(defn- dir [] (.toFile (Files/createTempDirectory "e6b-store-" (make-array FileAttribute 0))))
(defn- refusal [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))
(defn- initialized []
  (let [root (dir) s (store/isolated-store root "fixture-store")]
    (store/initialize! s {:state {:value 0} :revision "r0" :authority authority
                          :committed-at "2026-09-13T00:00:00Z"})
    [root s]))
(defn- proposal [s id event next-revision value]
  (let [h (:head (store/recover s))]
    {:prior {:revision (:state/revision h) :transaction-sha256 (:transaction-sha256 h)
             :state-sha256 (:state-sha256 h)}
     :next {:revision next-revision :state {:value value}}
     :application {:application/id id :feedback/event-id event
                   :transition/subject {:tick value} :input/digests {:input (str value)}
                   :output/digest (apply str (repeat 64 "c")) :status :committed}
     :authority authority :committed-at (format "2026-09-13T00:0%d:00Z" value)}))

(deftest ordered-commit-restart-and-idempotence
  (let [[root s] (initialized)
        p1 (proposal s "a1" "e1" "r1" 1) t1 (store/compare-and-commit! s p1)
        same (store/compare-and-commit! s p1)
        p2 (proposal s "a2" "e2" "r2" 2)]
    (is (= t1 same))
    (store/compare-and-commit! s p2)
    (store/release! s)
    (let [s2 (store/isolated-store root "fixture-store") r (store/recover s2)]
      (is (= 2 (get-in r [:head :generation])))
      (is (= ["a1" "a2"] (mapv :application/id (:applications r))))
      (is (false? (:restart-authorized? (store/capture s2))))
      (store/release! s2))))

(deftest no-implicit-init-and-exclusive-owner
  (let [root (dir) s (store/isolated-store root "fixture-store")]
    (is (= :e6b-store/missing-object (refusal #(store/recover s))))
    (is (= :e6b-store/already-owned
           (refusal #(store/isolated-store root "fixture-store"))))
    (store/initialize! s {:state {:value 0} :revision "r0" :authority authority
                          :committed-at "2026-09-13T00:00:00Z"})
    (is (= :e6b-store/already-initialized-or-interrupted
           (refusal #(store/initialize! s {:state {:value 9} :revision "other"
                                           :authority authority
                                           :committed-at "2026-09-13T00:00:00Z"}))))
    (store/release! s)))

(deftest cross-process-owner-refuses
  (let [[root s] (initialized)
        form (str "(require '[futon2.aif.machine-slow-feedback-store :as s])"
                  "(try (s/isolated-store \"" (.getAbsolutePath root) "\" \"fixture-store\")"
                  " (System/exit 9) (catch clojure.lang.ExceptionInfo e"
                  " (if (= :e6b-store/already-owned (:refusal (ex-data e)))"
                  " (System/exit 0) (System/exit 8))))")
        p (-> (ProcessBuilder. ^java.util.List ["clojure" "-M" "-e" form])
              (.directory (io/file ".")) (.redirectErrorStream true) .start)]
    (is (= 0 (.waitFor p)))
    (store/release! s)))

(deftest stale-and-conflicting-applications
  (let [[_ s] (initialized) p1 (proposal s "a1" "e1" "r1" 1)]
    (store/compare-and-commit! s p1)
    (is (= :e6b-store/application-conflict
           (refusal #(store/compare-and-commit! s (assoc-in p1 [:next :state] {:value 99})))))
    (is (= :e6b-store/stale-prior
           (refusal #(store/compare-and-commit! s (assoc (proposal s "a2" "e2" "r2" 2)
                                                         :prior (:prior p1))))))
    (let [p (proposal s "a2" "e1" "r2" 2)]
      (is (= :e6b-store/feedback-conflict (refusal #(store/compare-and-commit! s p)))))
    (store/release! s)))

(deftest crash-publication-windows
  (doseq [[stage committed?] [[:transaction-synced false] [:head-renamed true]]]
    (testing (name stage)
      (let [[root s] (initialized) p (proposal s "a1" "e1" "r1" 1)]
        (is (thrown? Exception
                     (binding [store/*stage-hook* (fn [at _]
                                                    (when (= at stage) (throw (ex-info "crash" {}))))]
                       (store/compare-and-commit! s p))))
        (is (= :e6b-store/owner-poisoned (refusal #(store/recover s))))
        (store/release! s)
        (let [s2 (store/isolated-store root "fixture-store")]
          (is (= (if committed? 1 0) (get-in (store/recover s2) [:head :generation])))
          (store/release! s2))))))

(deftest corrupt-or-missing-parent-refuses
  (let [[_ s] (initialized) _ (store/compare-and-commit! s (proposal s "a1" "e1" "r1" 1))
        r (store/recover s) current (:current r)
        parent (get-in current [:prior :transaction-sha256])
        path (.resolve ^java.nio.file.Path (:txdir s) (str parent ".edn"))]
    (Files/delete path)
    (is (= :e6b-store/missing-object (refusal #(store/recover s))))
    (store/release! s)))

(deftest corrupt-and-malformed-retained-bytes-refuse
  (doseq [[label replacement expected]
          [[:digest "{:schema :wrong}" :e6b-store/object-digest-mismatch]
           [:trailing "{} {}" :e6b-store/invalid-edn-cardinality]
           [:utf8 (byte-array [(unchecked-byte 0xc3) (byte 0x28)]) :e6b-store/invalid-edn]]]
    (testing (name label)
      (let [[_ s] (initialized) r (store/recover s)
            path (.resolve ^java.nio.file.Path (:txdir s)
                           (str (get-in r [:head :transaction-sha256]) ".edn"))
            bs (if (string? replacement) (.getBytes replacement "UTF-8") replacement)]
        (Files/write path bs (into-array StandardOpenOption
                                         [StandardOpenOption/TRUNCATE_EXISTING]))
        (is (= expected (refusal #(store/recover s))))
        (store/release! s)))))

(deftest interrupted-genesis-never-silently-reinitializes
  (let [root (dir) s (store/isolated-store root "fixture-store")]
    (is (thrown? Exception
                 (binding [store/*stage-hook* (fn [at _]
                                                (when (= at :transaction-published)
                                                  (throw (ex-info "crash" {}))))]
                   (store/initialize! s {:state {:value 0} :revision "r0"
                                         :authority authority
                                         :committed-at "2026-09-13T00:00:00Z"}))))
    (store/release! s)
    (let [s2 (store/isolated-store root "fixture-store")]
      (is (= :e6b-store/already-initialized-or-interrupted
             (refusal #(store/initialize! s2 {:state {:value 0} :revision "r0"
                                               :authority authority
                                               :committed-at "2026-09-13T00:00:00Z"}))))
      (store/release! s2))))

(deftest capture-is-immutable-and-commit-serialized
  (let [[_ s] (initialized) p (proposal s "a1" "e1" "r1" 1)
        started (promise) proceed (promise)
        f (future (binding [store/*stage-hook* (fn [at _]
                                                (when (= at :transaction-synced)
                                                  (deliver started true) @proceed))]
                    (store/compare-and-commit! s p)))]
    @started
    (let [capture-f (future (store/capture s))]
      (deliver proceed true) @f
      (let [capture @capture-f obj (first (vals (:objects capture))) original (aclone ^bytes obj)]
        (aset-byte ^bytes obj 0 (byte 0))
        (is (not= (seq obj) (seq original)))
        (is (= 1 (:generation (store/capture s))))))
    (store/release! s)))

(deftest invalid-state-does-not-publish
  (let [[_ s] (initialized) before (:head-digest (store/recover s))
        p (assoc-in (proposal s "a1" "e1" "r1" 1) [:next :state] {})]
    (is (= :e6b-store/state-invalid (refusal #(store/compare-and-commit! s p))))
    (is (= before (:head-digest (store/recover s))))
    (store/release! s)))
