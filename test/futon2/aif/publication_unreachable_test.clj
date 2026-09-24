(ns futon2.aif.publication-unreachable-test
  "H-PUBLISH-A2 (E-cascade-real): the :wm/publication-unreachable-v1
  marker — the typed terminal disposition for resolutions whose discharge
  context can never exist (H-PUBLISH-D, 5cbf0031; ruling: the marker, not
  dismissal). The marker is evidence of the ruling, never a reconstructed
  context: it carries no :repair/discharge-context, it does not suppress
  publication (a marked id whose derive now succeeds publishes and the
  marker reports :superseded), and catch-up! reports a marked, still-
  refusing id as :publication-unreachable with its class and ground
  instead of re-deriving the refusal on every tick.

  Temp store roots only; data/ is not written here."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.repair-discharge-receipt :as receipt]
            [futon2.aif.repair-obligation :as repair])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- temp-root []
  (str (Files/createTempDirectory "pub-unreachable-" (make-array FileAttribute 0))))

(defn- with-resolution
  "A minimal resolutions/ record so the id is known to the store."
  [root id]
  (let [f (io/file root "resolutions" (str id ".edn"))]
    (io/make-parents f)
    (spit f (pr-str {:repair/id id :repair/status :resolved
                     :validation-attempt "successor-1"}))
    f))

(defn- marker [id]
  {:schema :wm/publication-unreachable-v1
   :repair/id id
   :class :legacy-a
   :reason ":resolution-context-unavailable"
   :ground "H-PUBLISH-D 5cbf0031"
   :ruled-by "joe"
   :ruled-at "2026-09-24"
   :written-by "kimi-2"
   :written-at "2026-09-24T00:00:00Z"})

(deftest a-marker-for-an-unknown-resolution-refuses
  (let [root (temp-root)
        id "repair-never-heard-of-it"]
    (is (= :publication-unreachable-unknown-resolution
           (try (repair/write-publication-unreachable! root id (marker id)) nil
                (catch clojure.lang.ExceptionInfo e
                  (:repair-discharge/refusal (ex-data e))))))
    (is (not (.exists (io/file root "publication-unreachable" (str id ".edn"))))
        "the refusal precedes the write")))

(deftest an-existing-marker-is-not-overwritten
  (let [root (temp-root)
        id "repair-already-marked"
        _ (with-resolution root id)
        m (marker id)]
    (repair/write-publication-unreachable! root id m)
    (is (thrown? java.nio.file.FileAlreadyExistsException
                 (repair/write-publication-unreachable! root id
                                                        (assoc m :class :late-script-b))))
    (is (= m (repair/publication-unreachable-marker root id))
        "the first marker stands unchanged")))

(deftest no-marker-carries-a-discharge-context
  ;; The H-PUBLISH-D falsifier: a fabricated context is not a marker.
  (let [root (temp-root)
        id "repair-fabricated-context"
        _ (with-resolution root id)]
    (is (= :publication-unreachable-fabricated-context
           (try (repair/write-publication-unreachable!
                 root id (assoc (marker id) :repair/discharge-context {:schema :fake})) nil
                (catch clojure.lang.ExceptionInfo e
                  (:repair-discharge/refusal (ex-data e))))))
    (is (not (.exists (io/file root "publication-unreachable" (str id ".edn")))))))

(deftest a-non-schema-marker-refuses
  (let [root (temp-root)
        id "repair-off-schema"
        _ (with-resolution root id)]
    (is (= :publication-unreachable-invalid
           (try (repair/write-publication-unreachable!
                 root id (assoc (marker id) :extra :key)) nil
                (catch clojure.lang.ExceptionInfo e
                  (:repair-discharge/refusal (ex-data e))))))
    (is (= :publication-unreachable-invalid
           (try (repair/write-publication-unreachable!
                 root id (assoc (marker id) :class :made-up-class)) nil
                (catch clojure.lang.ExceptionInfo e
                  (:repair-discharge/refusal (ex-data e))))))))

(deftest catch-up-reports-the-marker-not-the-refusal
  (let [root (temp-root)
        id "repair-marked-legacy"
        _ (with-resolution root id)
        _ (repair/write-publication-unreachable! root id (marker id))]
    (with-redefs [receipt/publish! (fn [& _]
                                     (throw (ex-info "Repair discharge evidence refused"
                                                     {:repair/id id
                                                      :repair-discharge/refusal
                                                      :resolution-context-unavailable})))]
      (let [r (receipt/publication-result! root "/no/repo" id)]
        (is (= :publication-unreachable (:status r)))
        (is (= :legacy-a (:class r)))
        (is (= "H-PUBLISH-D 5cbf0031" (:ground r)))
        (is (false? (:repair/discharged? r)))
        (is (= :resolved (:store/status r)))))))

(deftest a-marked-id-that-now-derives-publishes-superseded
  (let [root (temp-root)
        id "repair-marker-superseded"
        _ (with-resolution root id)
        _ (repair/write-publication-unreachable! root id (marker id))]
    (with-redefs [receipt/publish! (fn [& _]
                                     {:status :receipt-committed :repair/id id
                                      :repair/discharged? true})]
      (let [r (receipt/publication-result! root "/no/repo" id)]
        (is (= :receipt-committed (:status r))
            "the marker does NOT suppress publication — derive first")
        (is (= :superseded (:marker r)))
        (is (= :legacy-a (:marker-class r)))
        (is (true? (:repair/discharged? r)))))))

(deftest store-wide-catch-up-reports-markers-not-refusals
  ;; H-PUBLISH-A2 acceptance: after the one-shot write, a full catch-up!
  ;; over the real store reports every one of the 68 H-PUBLISH-D ids as
  ;; :publication-unreachable with its class, and re-refuses none of them.
  ;; Read-only against data/: every marked id's derive refuses before any
  ;; git operation, so catch-up! writes nothing here.
  (let [results (receipt/catch-up! "data/wm-repair-obligations" "/home/joe/code/futon2")
        by-status (group-by :status results)
        by-class (frequencies (map :class (:publication-unreachable by-status)))]
    (is (= 68 (count results)))
    (is (= 68 (count (:publication-unreachable by-status))))
    (is (nil? (:publication-refused by-status))
        "no re-refusals of the marked ids")
    (is (= {:legacy-a 44 :late-script-b 9 :no-implementation-c 15} by-class))
    (is (every? #(= "H-PUBLISH-D 5cbf0031" (:ground %))
                (:publication-unreachable by-status)))))
