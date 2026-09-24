(ns futon2.aif.flight-driver-test
  "The first-flight driver: the plan is the record, --run is required to fly,
  kimi-1 never answers. The plan is pinned on M-futon-seams at futon3c
  20959e4f (after the ARGUE -> DOCUMENT retraction) with its lifecycle at
  d74a7c5a."
  (:require [clojure.edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.flight-driver :as fd]
            [futon2.aif.flight-runner :as fr]
            [futon2.aif.observation-checks :as checks])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def mission-text (slurp "test/fixtures/mission-criteria/M-futon-seams@futon3c-20959e4f.md"))
(def lifecycle-text (slurp "test/fixtures/mission-criteria/M-futon-seams-lifecycle@futon3c-d74a7c5a.edn"))
(def argue :exit/hac75428b9c97)
(def document :exit/h54d16050a9dc)

(defn- store [] (.getCanonicalPath (.toFile (Files/createTempDirectory "driver-store" (make-array FileAttribute 0)))))

(defn- fixture-opts []
  {:target "M-futon-seams" :seat "kimi-6" :repo "futon3c" :path "holes/missions/M-futon-seams.md"
   :lifecycle-path "holes/labs/M-futon-seams/lifecycle.edn" :store (store) :id "flight-test"
   :read-text (fn [_ _ path] (if (str/ends-with? path ".edn") lifecycle-text mission-text))
   :observe #(checks/decl-present? mission-text (:decl %))
   :sources {:beta-by-context {:WM {:beta 1}}}})

(deftest the-plan-for-the-first-flight
  (let [p (fd/plan (fixture-opts))]
    (is (= "M-futon-seams" (:requisition p)))
    (is (= "kimi-6" (:answering-seat p)))
    (is (= 6 (count (get-in p [:wants :in-view]))))
    (is (= {"MAP" true "DERIVE" true "ARGUE" false "VERIFY" true "INSTANTIATE" true "DOCUMENT" false}
           (into {} (map (juxt :phase :met?) (get-in p [:wants :in-view])))))
    (is (= [:HEAD :IDENTIFY] (mapv :phase (get-in p [:wants :out-of-view]))))
    (is (= [argue document] (:open-wants p)))
    (is (= [argue document] (mapv :want (:requests-it-would-issue p))))
    (is (= [] (:constraints p)) "the retracted edge is not read")
    (is (= :computed (get-in p [:horizon :authority :source])))
    (is (false? (:run? p)))))

(deftest kimi-1-and-missing-arguments-refuse
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"claude-1's delegate"
                        (fd/main* ["M-futon-seams" "--seat" "kimi-1" "--repo" "futon3c" "--path" "p"]
                                  {:load-sources (constantly {})})))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"missing seat"
                        (fd/main* ["M-futon-seams" "--repo" "futon3c" "--path" "p"]
                                  {:load-sources (constantly {})}))))

(deftest without-run-nothing-flies
  ;; bad case: a driver that flew by default would spend clicks on a plan
  ;; nobody authorized
  (with-redefs [fd/run-flight! (fn [& _] (throw (ex-info "flew without --run" {})))]
    (let [r (fd/main* ["M-futon-seams" "--seat" "kimi-6" "--repo" "futon3c"
                       "--path" "holes/missions/M-futon-seams.md" "--store" (store)]
                      {:load-sources (constantly {:beta-by-context {:WM {:beta 1}}})})]
      (is (nil? (:ran r)))
      (is (= "M-futon-seams" (get-in r [:plan :requisition]))))))

(deftest with-run-it-flies-once
  (let [calls (atom 0)]
    (with-redefs [fd/run-flight! (fn [_ planned] (swap! calls inc) {:flown (:flight-id planned)})]
      (let [r (fd/main* ["M-futon-seams" "--seat" "kimi-6" "--repo" "futon3c"
                         "--path" "holes/missions/M-futon-seams.md" "--store" (store) "--run"]
                        {:load-sources (constantly {:beta-by-context {:WM {:beta 1}}})})]
        (is (= 1 @calls))
        (is (string? (get-in r [:ran :flown])))))))

(deftest record-summary-reads-the-run-record
  (is (= {:click-id "r1" :chosen {:candidate :C1 :precedence [:p/a]} :unreached-wants [{:token :x :reason :no-producer}]}
         (fr/record-summary "M" "r1" {:decision {:chosen {:target "M" :candidate :C1 :precedence [:p/a]
                                                            :unreached-wants [{:token :x :reason :no-producer}]}
                                                  :abstention {:status :not-abstained}}})))
  (is (= :no-constructed-candidate
         (get-in (fr/record-summary "M" "r2" {:decision {:chosen {:status :absent}
                                                          :abstention {:status :abstained
                                                                       :targets [{:target "M" :kind :no-constructed-candidate
                                                                                  :missing :construction}]}}})
                 [:abstention :kind])))
  (is (= :run-record-missing (get-in (fr/record-summary "M" "r3" nil) [:abstention :kind]))))

(deftest http-click-fn-posts-waits-and-reads
  (let [posted (atom nil) polls (atom 0)
        cf (fr/http-click-fn {:today (constantly "2026-09-24")
                              :post! (fn [b] (reset! posted b) {:status 200 :body {:click-id "wm-click-1"}})
                              :get-status! (fn [] (swap! polls inc) {:running? (< @polls 3) :click-id "wm-click-1"})
                              :sleep! (fn [_])
                              :read-record! (fn [run-id] {:decision {:chosen {:target "M" :candidate :C1 :precedence []
                                                                              :unreached-wants []}}
                                                          :run/id run-id})})
        s (cf {:flight {:flight/id "flight-z" :target "M" :click 2 :wants [:a]}})]
    (is (= "2026-09-24-flight-z-click-2" (:click-id s)))
    (is (= "2026-09-24-flight-z-click-2" (:run-id @posted)))
    (is (= {:flight/id "flight-z" :target "M" :click 2 :wants [:a]} (clojure.edn/read-string (:flight-edn @posted))))
    (is (= 3 @polls))
    (is (= "wm-click-1" (:server-click-id s))))
  (testing "a click the server does not start is an abstention, not a retry"
    (let [posts (atom 0)
          cf (fr/http-click-fn {:today (constantly "2026-09-24")
                                :post! (fn [_] (swap! posts inc) {:status 409 :body {:rejected "already-running"}})
                                :get-status! (fn [] (throw (ex-info "should not poll" {})))})
          s (cf {:flight {:flight/id "f" :target "M" :click 1 :wants []}})]
      (is (= 1 @posts))
      (is (= :click-not-started (get-in s [:abstention :kind]))))))
