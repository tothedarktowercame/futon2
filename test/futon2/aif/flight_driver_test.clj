(ns futon2.aif.flight-driver-test
  "The first-flight driver: the plan is the record, --run is required to fly,
  kimi-1 never answers. The plan is pinned on M-futon-seams at futon3c
  20959e4f (after the ARGUE -> DOCUMENT retraction) with its lifecycle at
  d74a7c5a."
  (:require [clojure.edn]
            [clojure.java.io :as io]
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

(deftest read-runs-the-read-step-only
  ;; --read asks for readings and publishes them; it never flies
  (let [reads (atom 0)]
    (with-redefs [fd/run-flight! (fn [& _] (throw (ex-info "flew under --read" {})))
                  fd/read-only! (fn [_ planned] (swap! reads inc) {:read-for (:flight-id planned)})]
      (let [r (fd/main* ["M-futon-seams" "--seat" "kimi-6" "--repo" "futon3c"
                         "--path" "holes/missions/M-futon-seams.md" "--store" (store) "--read"]
                        {:load-sources (constantly {:beta-by-context {:WM {:beta 1}}})})]
        (is (= 1 @reads))
        (is (nil? (:ran r)))
        (is (string? (get-in r [:read :read-for])))))))

;; ---------------------------------------------------------------------------
;; WM-DRIVER-I: the wired steps reach run-flight!, each flag not given is a
;; typed absence, and a decision with no dispatch function is recorded, not
;; thrown. Fixtures only: no seat, no click, a temp store and run-record dir.

(deftest the-flags-reach-the-plan-and-absences-are-typed
  (let [base ["M-futon-seams" "--seat" "kimi-6" "--repo" "futon3c"
              "--path" "holes/missions/M-futon-seams.md" "--store" (store)]
        load {:load-sources (constantly {:beta-by-context {:WM {:beta 1}}})}
        given (:plan (fd/main* (into base ["--checker" "/c/proof2a_check.clj" "--bb" "/usr/bin/bb"
                                           "--library-root" "/lib" "--cascades" "/casc"
                                           "--field-entry" "{:target \"M-futon-seams\" :next-step :read-criteria}"])
                               load))
        bare (:plan (fd/main* base load))]
    (is (= {:checker "/c/proof2a_check.clj" :bb "/usr/bin/bb" :library-root "/lib" :cascades "/casc"
            :field-entry {:target "M-futon-seams" :next-step :read-criteria}}
           (select-keys (:resolved-steps given) [:checker :bb :library-root :cascades :field-entry])))
    (is (= {:checker {:absent :no-wc-checker-configured} :bb {:absent :not-given :runs "bb"}
            :library-root {:absent :not-in-flight-opts} :field-entry {:absent :no-field-entry}
            :cascades {:absent :no-cascades-dir} :quotes {:absent :no-quotes}
            :dispatch-step {:absent :no-dispatch-configured}}
           (select-keys (:resolved-steps bare) [:checker :bb :library-root :field-entry :cascades :quotes :dispatch-step])))
    (is (= {:target "M-futon-seams" :target-source :hand-placed} (:placement bare)))))

(defn- fixture-checker [s]
  (let [f (io/file (store) "checker.clj")]
    (spit f (str "(println " (pr-str s) ")\n"))
    (str f)))

(defn- wired-run [extra]
  (let [runs (store)
        _ (spit (io/file runs "tick-run-record-run-w.edn") (pr-str {:repair/publication []}))
        opts (merge (fixture-opts)
                    {:run-record-dir runs
                     :answer-fn (fn [_] {:seat "fixture" :job-id "none" :state "failed"})
                     :click-fn (constantly {:click-id "run-w" :chosen {:candidate :cand/w :precedence [:p/w]}})
                     :checker (fixture-checker "[]")
                     :max-clicks 1}
                    extra)]
    [opts (fd/run-flight! opts {:flight-id "flight-wired"})]))

(deftest run-flight-reaches-the-enactment-and-the-wc-call
  (let [[opts r] (wired-run {:dispatch-step! (fn [_] {:commit "c" :produced :t :check {:class :fixture}})})
        entry (first (:enactments r))]
    (is (= "run-w" (:click-id entry)))
    (is (clojure.string/starts-with? (:record-path entry) (:store opts)) "the enactment record is under the store")
    (is (= [] (get-in entry [:wc :verdict])) "the checker the flag named ran")
    (is (= {:absent :no-repair-obligation-for-target :target "M-futon-seams"} (:publication-observed entry)))))

(deftest a-decision-with-no-dispatch-is-recorded-not-thrown
  (let [[opts r] (wired-run {})]
    (is (= [{:enactment {:absent :no-dispatch-configured} :click-id "run-w"}] (:enactments r)))
    (is (not (.exists (io/file (:store opts) "flights" "enactments"))) "no enactment record written")))

(deftest an-aborted-flight-still-writes-its-record
  ;; WM-SPIKE-FIX-III: the third flight wrote no record when a step threw
  (let [e (try (wired-run {:click-fn (fn [_] (throw (java.net.http.HttpTimeoutException. "request timed out")))})
               nil
               (catch clojure.lang.ExceptionInfo e e))
        path (:record-path (ex-data e))
        rec (clojure.edn/read-string {:default tagged-literal} (slurp path))]
    (is (some? e) "the error still reaches the caller (exit 1)")
    (is (= :aborted (get-in rec [:flight :status])))
    (is (= :click (get-in rec [:flight :aborted :step])))
    (is (= "java.net.http.HttpTimeoutException" (get-in rec [:flight :aborted :class])))))
