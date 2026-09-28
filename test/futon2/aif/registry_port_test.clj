(ns futon2.aif.registry-port-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.increment-attestation :as increment]
            [futon2.aif.observation-checks :as checks]
            [futon2.aif.registry-port :as port]))

(use-fixtures :each
  (fn [test-fn]
    (port/uninstall!)
    (try (test-fn) (finally (port/uninstall!)))))

(def test-namespace "futon2.aif.registry-port-test")
(def code-path "src/futon2/aif/registry_port.clj")
(def test-path "test/futon2/aif/registry_port_test.clj")

(defn run-record [warrant? id author ran finished]
  {:schema "test-registry/v1" :kind :run :run/id id :author author
   :repo/root "/home/joe/code/futon2"
   :command ["clojure" "-M:test" "-n" test-namespace]
   :ran-at ran :finished-at finished :warrant? warrant?
   :postcheck {:status :matched}
   :results {:tests 1 :assertions 1 :failures (if warrant? 0 1) :errors 0}
   :code-files {code-path (checks/content-sha code-path)}
   :test-files {test-path (checks/content-sha test-path)}})

(defn entry [record]
  (let [text (pr-str record)
        digest (#'checks/sha256-hex (.getBytes text "UTF-8"))]
    {:evidence/id (str "test-registry-" digest)
     :evidence/author (:author record)
     :evidence/body {:payload-edn text :sha256 digest}}))

(defn implementation [entries latest]
  {:entry #(get entries %)
   :latest-namespace (fn [_] latest)
   :latest-command (fn [_] latest)
   :current-or-request
   (fn [{:keys [namespace repo]}]
     (if latest
       (let [record (read-string (get-in latest [:evidence/body :payload-edn]))]
         (if (:warrant? record)
           {:status :current :entry-id (:evidence/id latest)
            :ran-at (:ran-at record) :git-head "fixture-head"}
           {:status :missing :kind :not-passing
            :data {:namespace namespace :repo repo :reason :not-passing
                   :found-entry-id (:evidence/id latest)}}))
       {:status :missing :kind :no-current-warrant
        :data {:namespace namespace :repo repo :reason :absent}}))
   :runs (fn [{:keys [author since]}]
           (filterv (fn [e]
                      (let [r (read-string (get-in e [:evidence/body :payload-edn]))]
                        (and (= author (:author r))
                             (or (nil? since)
                                 (not (neg? (compare (:ran-at r) since)))))))
                    (vals entries)))})

(deftest unset-port-is-a-failure-for-both-readers
  (let [c8 (checks/check-registered-run {:repo "futon2" :namespace test-namespace})
        attestation (increment/increment-evidence
                     {:agency-base "http://127.0.0.1:9"}
                     {:author "nobody"})]
    (is (= :registry-port-unset (:kind c8)))
    (is (= :registry-port-unset (:kind attestation)))))

(deftest c8-reads-the-newest-local-record-and-explicit-id
  (let [pass (entry (run-record true "pass" "codex-3"
                                "2026-09-27T01:00:00Z" "2026-09-27T01:01:00Z"))
        fail (entry (run-record false "fail" "codex-3"
                                "2026-09-27T02:00:00Z" "2026-09-27T02:01:00Z"))]
    (port/install! (implementation {(:evidence/id pass) pass} pass))
    (is (true? (:observed (checks/check-registered-run
                           {:repo "futon2" :namespace test-namespace}))))
    (is (true? (:observed (checks/check-registered-run
                           {:repo "futon2" :namespace test-namespace
                            :config (:evidence/id pass)}))))
    (is (= :no-local-record
           (get-in (checks/check-registered-run
                    {:repo "futon2" :namespace test-namespace
                     :config (str "test-registry-" (apply str (repeat 64 "0")))})
                   [:evidence :reason])))
    (port/install! (implementation {(:evidence/id fail) fail} fail))
    (is (= :not-passing
           (get-in (checks/check-registered-run
                    {:repo "futon2" :namespace test-namespace})
                   [:evidence :kind])))))

(deftest c8-carries-stale-request-fields
  (let [record (assoc (run-record true "stale" "codex-3"
                                  "2026-09-27T01:00:00Z" "2026-09-27T01:01:00Z")
                      :code-files {code-path (apply str (repeat 64 "0"))})
        stale (entry record)]
    (port/install! (assoc (implementation {(:evidence/id stale) stale} stale)
                          :current-or-request
                          (fn [_] {:status :missing :kind :no-current-warrant
                                   :data {:reason :stale :request-id "request-1"
                                          :run-requested-at "2026-09-28T00:00:00Z"
                                          :found-entry-id (:evidence/id stale)}})))
    (let [result (checks/check-registered-run {:repo "futon2" :namespace test-namespace})]
      (is (false? (:observed result)))
      (is (= {:reason :stale :request-id "request-1"
              :run-requested-at "2026-09-28T00:00:00Z"
              :found-entry-id (:evidence/id stale)}
             (select-keys (:evidence result)
                          [:reason :request-id :run-requested-at :found-entry-id]))))))

(deftest c8-port-without-current-operation-refuses
  (port/install! (dissoc (implementation {} nil) :current-or-request))
  (is (= :registry-port-unset
         (:kind (checks/check-registered-run
                 {:repo "futon2" :namespace test-namespace})))))

(deftest increment-query-distinguishes-empty-and-storage-failure
  (let [inside-a (entry (run-record true "a" "wm-author"
                                    "2026-09-27T02:00:00Z" "2026-09-27T02:01:00Z"))
        inside-b (entry (run-record true "b" "wm-author"
                                    "2026-09-27T03:00:00Z" "2026-09-27T03:01:00Z"))
        outside (entry (run-record true "old" "wm-author"
                                   "2026-09-27T00:00:00Z" "2026-09-27T00:01:00Z"))
        other (entry (run-record true "other" "other-author"
                                 "2026-09-27T04:00:00Z" "2026-09-27T04:01:00Z"))
        entries (into {} (map (juxt :evidence/id identity))
                      [inside-a inside-b outside other])]
    (port/install! (implementation entries inside-b))
    (is (= #{(:evidence/id inside-a) (:evidence/id inside-b)}
           (set (map :evidence/id
                     (increment/warrant-entries-local
                      {} {:author "wm-author" :since "2026-09-27T01:00:00Z"})))))
    (is (= [] (increment/warrant-entries-local {} {:author "none"})))
    (port/install! (assoc (implementation {} nil)
                          :runs (fn [_] (throw (ex-info "broken" {})))))
    (let [failure (increment/increment-evidence {} {:author "wm-author"})]
      (is (= :registry-storage-failed (:kind failure)))
      (is (not= [] failure)))))
