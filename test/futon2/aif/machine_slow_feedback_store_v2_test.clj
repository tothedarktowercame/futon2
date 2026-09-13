(ns futon2.aif.machine-slow-feedback-store-v2-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.machine-slow-feedback-provenance :as provenance]
            [futon2.aif.machine-slow-feedback-provenance-test :as provenance-test]
            [futon2.aif.machine-slow-feedback-store-v2 :as store]
            [futon2.aif.machine-slow-feedback-store :as legacy])
  (:import (java.nio.file Files StandardOpenOption)
           (java.nio.file.attribute FileAttribute)
           (java.util Base64)))

(def authority {:verifier/source-sha256 (apply str (repeat 64 "a"))
                :evidence-source-sha256s {:fixture (apply str (repeat 64 "b"))}})
(defn- dir [] (.toFile (Files/createTempDirectory "e6b-store-v2-"
                                                   (make-array FileAttribute 0))))
(defn- refusal [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))
(defn- setup []
  (let [root (dir) s (store/isolated-store root "fixture-store-v2")
        input (#'provenance-test/input)
        projection (:carrier-projection input)
        prior (get-in projection [:prior :carrier])]
    (store/initialize! s {:state prior :revision (:state/revision prior) :authority authority
                          :committed-at "2026-09-13T00:00:00Z"})
    (let [h (:head (store/recover s))
          input' (assoc input :expected-head
                        {:store/id (:store/id h) :generation (:generation h)
                         :transaction-sha256 (:transaction-sha256 h)
                         :state/revision (:state/revision h) :state-sha256 (:state-sha256 h)})
          artifact (provenance/construct input')]
      [root s artifact])))
(defn- pin [artifact]
  {:bytes/base64 (:bytes/base64 artifact) :expected-sha256 (:sha256 artifact)})
(defn- write-form! [path x]
  (Files/write path (.getBytes (pr-str x) "UTF-8")
               (into-array StandardOpenOption [StandardOpenOption/TRUNCATE_EXISTING])))

(deftest publish-recover-capture-and-stable-retry
  (let [[root s artifact] (setup) tx (store/commit! s (pin artifact))
        same (store/commit! s (pin artifact))]
    (is (= tx same))
    (is (= 1 (get-in (store/recover s) [:head :generation])))
    (let [c (store/capture s)]
      (is (= 2 (count (:transaction-objects c))))
      (is (= 1 (count (:provenance-objects c))))
      (is (false? (:restart-authorized? c))))
    (store/release! s)
    (let [s2 (store/isolated-store root "fixture-store-v2")]
      (is (= 1 (get-in (store/recover s2) [:head :generation])))
      (store/release! s2))))

(deftest invalid-provenance-publishes-nothing
  (let [[_ s artifact] (setup)
        before (store/capture s)
        bad (assoc (pin artifact) :expected-sha256 (apply str (repeat 64 "0")))]
    (is (= :e6b-provenance/readback-pin-mismatch (refusal #(store/commit! s bad))))
    (is (= (keys (:transaction-objects before))
           (keys (:transaction-objects (store/capture s)))))
    (is (empty? (:provenance-objects (store/capture s))))
    (store/release! s)))

(deftest publication-crash-windows
  (doseq [[stage generation] [[:provenance-published 0]
                              [:transaction-published 0]
                              [:head-renamed 1]]]
    (testing (name stage)
      (let [[root s artifact] (setup)]
        (is (thrown? Exception
                     (binding [store/*stage-hook* (fn [at _]
                                                    (when (= at stage)
                                                      (throw (ex-info "crash" {}))))]
                       (store/commit! s (pin artifact)))))
        (is (= :e6b-store-v2/owner-poisoned (refusal #(store/recover s))))
        (store/release! s)
        (let [s2 (store/isolated-store root "fixture-store-v2")]
          (is (= generation (get-in (store/recover s2) [:head :generation])))
          (store/release! s2))))))

(deftest missing-corrupt-and-disagreeing-provenance-refuse
  (doseq [[mode expected] [[:missing :e6b-store-v2/missing-object]
                           [:corrupt :e6b-store-v2/provenance-digest-mismatch]]]
    (testing (name mode)
      (let [[root s artifact] (setup) tx (store/commit! s (pin artifact))
            pd (:provenance-sha256 tx)
            path (.resolve ^java.nio.file.Path (:provenance-dir s) (str pd ".edn"))]
        (store/release! s)
        (if (= mode :missing)
          (Files/delete path)
          (Files/write path (.getBytes "{:schema :wrong}" "UTF-8")
                       (into-array StandardOpenOption [StandardOpenOption/TRUNCATE_EXISTING])))
        (let [s2 (store/isolated-store root "fixture-store-v2")]
          (is (= expected (refusal #(store/recover s2))))
          (store/release! s2))))))

(deftest changed-provenance-same-application-conflicts-before-head
  (let [[_ s artifact] (setup) _ (store/commit! s (pin artifact))
        record (assoc-in (:record artifact) [:expected-head :generation] 99)
        bs (.getBytes (pr-str record) "UTF-8")
        changed {:bytes/base64 (.encodeToString (Base64/getEncoder) bs)
                 :expected-sha256 (#'provenance-test/sha256 bs)}]
    ;; Readback itself refuses the duplicated expected-HEAD disagreement; no
    ;; second transaction or HEAD is published.
    (is (some? (refusal #(store/commit! s changed))))
    (is (= 1 (get-in (store/recover s) [:head :generation])))
    (store/release! s)))

(deftest forged-head-current-state-refuses
  (let [[_ s artifact] (setup) _ (store/commit! s (pin artifact))
        path ^java.nio.file.Path (:head s)
        head (:record (#'store/read-object path))]
    (write-form! path (assoc head :state/revision "invented"
                             :state-sha256 (apply str (repeat 64 "f"))))
    (is (= :e6b-store-v2/head-current-mismatch (refusal #(store/recover s))))
    (is (= :e6b-store-v2/head-current-mismatch (refusal #(store/capture s))))
    (store/release! s)))

(deftest legacy-store-is-not-silently-upgraded
  (let [root (dir) s (legacy/isolated-store root "legacy-store")]
    (legacy/initialize! s {:state {:value 0} :revision "r0" :authority authority
                           :committed-at "2026-09-13T00:00:00Z"})
    (legacy/release! s)
    (is (= :e6b-store-v2/legacy-or-interrupted-store
           (refusal #(store/isolated-store root "legacy-store"))))))

(deftest strict-genesis-and-head-schemas-refuse
  (doseq [mutate [#(assoc % :extra true) #(assoc % :state-sha256 (apply str (repeat 64 "0")))]]
    (let [[_ s _] (setup) h (store/recover s)
          digest (get-in h [:head :transaction-sha256])
          path (.resolve ^java.nio.file.Path (:txdir s) (str digest ".edn"))
          genesis (:record (#'store/read-object path))]
      (write-form! path (mutate genesis))
      (is (some? (refusal #(store/recover s))))
      (store/release! s))))
