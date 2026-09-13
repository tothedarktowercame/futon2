(ns futon2.aif.machine-slow-feedback-store-v2-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.machine-slow-feedback-provenance :as provenance]
            [futon2.aif.machine-slow-feedback-provenance-test :as provenance-test]
            [futon2.aif.machine-slow-feedback-store-v2 :as store]
            [futon2.aif.machine-slow-feedback-store :as legacy])
  (:import (java.nio.file Files StandardOpenOption)
           (java.nio.file.attribute FileAttribute)
           (java.util Base64)))

(def authority {:schema :wm/e6b-genesis-authority-v1
                :scope :isolated-test :status :fixture-only
                :verifier/source-sha256 (apply str (repeat 64 "a"))
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

(defn- decoded-head [capture]
  (let [descriptor (:head-object capture)
        bs (.decode (Base64/getDecoder) ^String (:bytes/base64 descriptor))]
    {:bytes bs
     :record (edn/read-string (String. bs "UTF-8"))
     :sha256 (#'store/sha256 bs)}))

(deftest capture-retains-exact-validated-head-buffer
  (doseq [committed? [false true]]
    (let [[_ s artifact] (setup)
          _ (when committed? (store/commit! s (pin artifact)))
          c (store/capture s)
          {:keys [bytes record sha256]} (decoded-head c)
          expected-generation (if committed? 1 0)]
      (is (= sha256 (:head-digest c) (get-in c [:head-object :source-sha256])))
      (is (= expected-generation (:generation c) (:generation record)))
      (is (= (:store/id c) (:store/id record)))
      (is (= (last (:chain-digests c)) (:transaction-sha256 record)))
      (is (= (:application-universe c) (:application-index record)))
      ;; Mutating decoded caller-owned bytes and replacing a returned map cannot
      ;; alter the immutable descriptor retained by a subsequent capture.
      (aset-byte bytes 0 (byte 0))
      (let [changed (assoc-in c [:head-object :bytes/base64] "tampered")
            again (store/capture s)]
        (is (not= (:head-object changed) (:head-object again)))
        (is (= (:head-object c) (:head-object again))))
      (store/release! s))))

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
  (let [root (dir) s (legacy/isolated-store root "legacy-store")
        legacy-authority (select-keys authority
                                      [:verifier/source-sha256 :evidence-source-sha256s])]
    (legacy/initialize! s {:state {:value 0} :revision "r0" :authority legacy-authority
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

(deftest semantic-genesis-forgeries-refuse-after-self-consistent-rehash
  (doseq [mutate [#(assoc % :authority nil)
                  #(assoc % :committed-at nil)
                  #(assoc % :generation 7)
                  #(assoc-in % [:state :state/revision] "borrowed")]]
    (let [[_ s _] (setup) h (store/recover s)
          old (get-in h [:head :transaction-sha256])
          old-path (.resolve ^java.nio.file.Path (:txdir s) (str old ".edn"))
          forged (mutate (:record (#'store/read-object old-path)))
          bs (.getBytes (pr-str forged) "UTF-8") digest (#'store/sha256 bs)
          new-path (.resolve ^java.nio.file.Path (:txdir s) (str digest ".edn"))
          head (assoc (:head h) :transaction-sha256 digest)]
      (Files/write new-path bs (into-array StandardOpenOption [StandardOpenOption/CREATE_NEW]))
      (write-form! ^java.nio.file.Path (:head s) head)
      (is (some? (refusal #(store/recover s))))
      (store/release! s))))

(deftest invalid-genesis-input-publishes-neither-object-nor-head
  (doseq [bad [{:state {:not :a-carrier} :revision "r0" :authority authority
                :committed-at "2026-09-13T00:00:00Z"}
               {:state (get-in (#'provenance-test/input) [:carrier-projection :prior :carrier])
                :revision "slow-4" :authority (assoc authority :opaque (Object.))
                :committed-at "2026-09-13T00:00:00Z"}
               {:state (get-in (#'provenance-test/input) [:carrier-projection :prior :carrier])
                :revision "slow-4" :authority authority :committed-at "not-time"}]]
    (let [root (dir) s (store/isolated-store root "invalid-genesis")]
      (is (some? (refusal #(store/initialize! s bad))))
      (is (not (Files/exists ^java.nio.file.Path (:head s) (make-array java.nio.file.LinkOption 0))))
      (is (empty? (iterator-seq (.iterator
                                 (Files/newDirectoryStream ^java.nio.file.Path (:txdir s))))))
      (store/release! s))))
