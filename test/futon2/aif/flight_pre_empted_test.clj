(ns futon2.aif.flight-pre-empted-test
  "R1(c-2): a started HTTP click spent on dedicated META self-heal is a
  terminal pre-emption, distinct from a missing run record."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.flight :as flight]
            [futon2.aif.flight-driver :as driver]
            [futon2.aif.flight-runner :as runner])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def target "M-flight-pre-empted")

;; Projection of GET /api/alpha/wm/click?click-id=wm-click-9603346f-… on
;; 2026-10-23. Values are verbatim after Cheshire keywordises JSON keys.
(def self-heal-click-state
  {:click-id "wm-click-9603346f-65f8-4d3a-ab6f-88ef4c40480e"
   :status "completed"
   :result
   {:binding-status "absent"
    :runtime-disposition
    {:route "dedicated-meta-self-heal"
     :injury-id "injury/04e14314a0eec3aab88d8e7044be26f3ae5dfe6d"
     :repair-id "repair/d9ca0b41fe22ddc094e3b30816a1e085b6e16481"
     :status "awaiting-retry"}}})

;; Projection of the completed ordinary retry whose durable binding names
;; tick-run-record-2026-10-23-90ca5944-…. Its runtime disposition has no route.
(def ordinary-click-state
  {:click-id "wm-retry-repair/4c5023bd01830ad556ee0acf45905a8d377abef9-1-patch-d9fb5ffecebf70bb9d392f393bc261b261998df1"
   :status "completed"
   :result
   {:binding-status "verified"
    :run-record-status "present"
    :run-record "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-23-90ca5944-3d52-4bd6-940c-fe2f6c2c8c6b.edn"
    :runtime-disposition {}}})

(defn- new-flight [id]
  (flight/start (flight/choose-target {:requested target})
                {:kind :checkbox} {:id id}))

(defn- http-click [posts click-state record]
  (let [server-click-id (or (:click-id click-state) "server-click-with-missing-state")]
    (runner/http-click-fn
     {:today (constantly "2026-10-23")
      :post! (fn [_]
               (swap! posts inc)
               {:status 200 :body {:click-id server-click-id}})
      :get-status! (constantly {:running? false :click-id server-click-id})
      :get-click-state! (constantly click-state)
      :read-record! (constantly record)})))

(deftest dedicated-self-heal-pre-empts-the-flight
  (let [posts (atom 0)
        observations (atom 0)
        f (flight/run!
           (new-flight "flight-pre-empted-live")
           {:click-fn (http-click posts self-heal-click-state nil)
            ;; The world advances on the first click. Without the terminal
            ;; pre-emption, this is :open and the old loop posts again.
            :observe-fn (fn [_ _]
                          (if (= 1 (swap! observations inc))
                            {:a false :b false}
                            {:a true :b false}))
            :sources-fn (constantly {:wants {target [:a :b]}})
            :max-clicks 3})]
    (is (= [:a] (get-in f [:clicks 0 :advanced])))
    (is (= 1 @posts) "pre-emption prevents another POST despite progress")
    (is (= :pre-empted (:status f)))
    (is (= {:route "dedicated-meta-self-heal"
            :injury-id "injury/04e14314a0eec3aab88d8e7044be26f3ae5dfe6d"
            :repair-id "repair/d9ca0b41fe22ddc094e3b30816a1e085b6e16481"
            :status "awaiting-retry"}
           (:pre-emption f)))
    (is (nil? (get-in f [:clicks 0 :abstention]))
        "self-heal is not misreported as run-record-missing")))

(deftest ordinary-click-without-progress-stays-no-progress
  (let [posts (atom 0)
        record {:decision {:chosen {:target target :id "ordinary-policy"
                                    :candidate :candidate/ordinary
                                    :precedence [] :unreached-wants []}}
                :route []}
        f (flight/run!
           (new-flight "flight-ordinary-no-progress")
           {:click-fn (http-click posts ordinary-click-state record)
            :observe-fn (constantly {:one false})
            :sources-fn (constantly {:wants {target [:one]}})
            :max-clicks 3})]
    (is (= 1 @posts))
    (is (= :no-progress (:status f)))
    (is (nil? (:pre-emption f)))
    (is (= "ordinary-policy" (get-in f [:clicks 0 :chosen :id])))))

(deftest unavailable-click-state-preserves-missing-record-semantics
  (testing "a not-found state is typed but is not invented as pre-emption"
    (let [posts (atom 0)
          state {:status :not-found}
          f (flight/run!
             (new-flight "flight-click-state-not-found")
             {:click-fn (http-click posts state nil)
              :observe-fn (constantly {:one false})
              :sources-fn (constantly {:wants {target [:one]}})
              :max-clicks 3})]
      (is (= 1 @posts))
      (is (= :no-progress (:status f)))
      (is (nil? (:pre-emption f)))
      (is (= :run-record-missing
             (get-in f [:clicks 0 :abstention :kind])))
      (is (= {:absent :click-state-not-found}
             (get-in f [:clicks 0 :click-state]))))))

(deftest driver-surfaces-the-terminal-pre-emption
  (let [root (.getCanonicalPath
              (.toFile (Files/createTempDirectory
                        "flight-pre-emption-driver"
                        (make-array FileAttribute 0))))
        pre-emption {:route "dedicated-meta-self-heal"
                     :injury-id "injury/04e14314a0eec3aab88d8e7044be26f3ae5dfe6d"
                     :repair-id "repair/d9ca0b41fe22ddc094e3b30816a1e085b6e16481"
                     :status "awaiting-retry"}
        opts {:target target :seat "fixture" :repo "futon2" :path "mission.md"
              :store root :run-record-dir root :sources {}
              :availability-fn
              (constantly {:schema :wm/ordinary-click-availability-v1
                           :unit :ordinary-click :available 1})
              :max-clicks 1}
        result (with-redefs [flight/run!
                             (fn [f _]
                               (assoc f :status :pre-empted
                                      :pre-emption pre-emption))]
                 (driver/run-flight! opts {:flight-id "driver-pre-emption"}))]
    (is (= :pre-empted (:status result)))
    (is (= pre-emption (:pre-emption result)))
    (is (= pre-emption (get-in result [:flight :pre-emption])))))

(deftest pre-emption-keeps-a-recorded-decline
  (testing "only :run-record-missing is dropped; a decline on a record stays"
    (let [record {:decision {:abstention {:status :abstained
                                          :targets [{:target target :kind :declined
                                                     :missing :evidence}]}}}
          click ((http-click (atom 0) self-heal-click-state record)
                 {:flight {:target target :flight/id "decline" :click 1}})]
      (is (= "dedicated-meta-self-heal" (get-in click [:pre-empted-by :route])))
      (is (= {:target target :kind :declined :missing :evidence}
             (:abstention click))))))
