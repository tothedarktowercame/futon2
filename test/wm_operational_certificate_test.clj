(ns wm-operational-certificate-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [checks.wm-operational-certificate :as cert]))

(def run-path "holes/labs/wm-contract/tick-run-record-2026-08-30.edn")
(def clean-resource (edn/read-string
                     (slurp "test/fixtures/wm-operational-certificate/resource-clean.edn")))
(def run-bytes (java.nio.file.Files/readAllBytes
                (.toPath (io/file run-path))))
(def run-record (edn/read-string (String. run-bytes "UTF-8")))
(defn edn-bytes [x] (.getBytes (pr-str x) "UTF-8"))

(def drawn-pairs
  [[:R1 :R4] [:R2 :R3] [:R3 :R1] [:R1 :R3] [:R1 :R3a]
   [:R2 :R3a] [:R3a :R7] [:R3a :R3] [:R2 :R8] [:R6 :R4]
   [:R14 :R6] [:R4 :R5] [:R5 :R6] [:R6 :R13] [:R11 :R16]
   [:R13 :R14] [:R14 :R16] [:R16 :R2] [:R6 :R11] [:R7 :R3]
   [:R7 :R8] [:R7 :R14] [:R8 :R5] [:R9 :R16] [:R10 :R8]
   [:R12 :R7] [:R15 :R13] [:R15 :R16] [:R20 :R7]])

(def measured-pairs
  [[:R20 :R12] [:R12 :R2] [:R2 :R7] [:R2 :R3a]
   [:R3a :R7] [:R3 :R8] [:R6 :R14] [:R14 :TRACE]])

(def stated-topology
  {:note "fields outside the consumed pair sets are provenance only"
   :edges (mapv (fn [[from to]] {:from from :to to :status :drawn}) drawn-pairs)
   :route-measured-drawn (mapv (fn [[from to]] {:from from :to to}) measured-pairs)})

(defn with-topology [m f]
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "wm-certificate-topology-"
                      (make-array java.nio.file.attribute.FileAttribute 0)))
        svg (io/file dir "map.svg")
        data (io/file dir "edges.edn")]
    (spit svg "<svg><!-- provenance changes do not alter topology --></svg>")
    (spit data (pr-str m))
    (binding [cert/topology-svg (.getPath svg)
              cert/topology-data (.getPath data)]
      (f))))

(deftest topology-pin-covers-exactly-the-consumed-pair-sets
  (testing "unread fields, comments, and a non-drawn edge do not change the verdict"
    (with-topology
      (-> stated-topology
          (assoc :note "a changed note")
          (update :edges conj {:from :R99 :to :R100 :status :proposed
                               :comment "not drawn"}))
      (fn []
        (let [c (cert/certificate run-bytes clean-resource false)]
          (is (= :pass (:verdict c)))
          (is (true? (get-in c [:topology :pin-valid?])))
          (is (= cert/expected-drawn-pairs-sha256
                 (get-in c [:topology :drawn-pairs-sha256])))
          (is (= cert/expected-measured-pairs-sha256
                 (get-in c [:topology :measured-pairs-sha256])))))))
  (testing "removing a drawn edge fails the pin"
    (with-topology
      (update stated-topology :edges pop)
      #(let [c (cert/certificate run-bytes clean-resource false)]
         (is (= :fail (:verdict c)))
         (is (false? (get-in c [:topology :pin-valid?]))))))
  (testing "adding a measured edge fails the pin"
    (with-topology
      (update stated-topology :route-measured-drawn conj {:from :R99 :to :R100})
      #(let [c (cert/certificate run-bytes clean-resource false)]
         (is (= :fail (:verdict c)))
         (is (false? (get-in c [:topology :pin-valid?]))))))
  (testing "a traversed hop in neither pair set remains undeclared"
    (with-topology
      stated-topology
      #(let [c (cert/certificate run-bytes clean-resource true)]
         (is (= :fail (:verdict c)))
         (is (= 1 (get-in c [:traversal :counts :undeclared])))
         (is (= ["R99" "R100"]
                (-> c :traversal :undeclared-hops first cert/hop-pair)))))))

(deftest mapped-partial-route-certifies-and-exposes-coverage
  (let [c (cert/certificate run-bytes clean-resource false)]
    (is (= :pass (:verdict c)))
    (is (= {:total 9 :original 3 :measured 6 :undeclared 0}
           (get-in c [:traversal :counts])))
    (is (seq (get-in c [:traversal :declared-not-exercised :original])))
    (is (= :content-sha256-fallback (get-in c [:run :identity-kind])))
    (is (cert/certificate-matches-run? run-bytes c))
    (is (true? (get-in c [:checks :resource-status-clean?])))))

(deftest explicit-run-id-is-stable-and-mismatch-is-rejected
  (let [identified-bytes (edn-bytes (assoc run-record :run/id "run-2026-08-31-test"))
        c (cert/certificate identified-bytes clean-resource false)]
    (is (= {:id "run-2026-08-31-test" :identity-kind :recorded-run-id}
           (select-keys (:run c) [:id :identity-kind])))
    (is (cert/certificate-matches-run? identified-bytes c))
    (is (false? (cert/certificate-matches-run?
                 identified-bytes
                 (assoc-in c [:run :id] "different-run"))))))

(deftest selector-seam-is-recorded-but-does-not-govern-certification
  (let [production-seam "agency-http:verified-live-selection"
        production-bytes (edn-bytes (assoc run-record :run/id "production-shape-control"
                                           :selectorSeam production-seam))
        missing-bytes (edn-bytes (-> run-record
                                     (assoc :run/id "missing-seam-control")
                                     (dissoc :selectorSeam)))
        production (cert/certificate production-bytes clean-resource false)
        missing (cert/certificate missing-bytes clean-resource false)]
    (is (= :pass (:verdict production)))
    (is (= {:status :present :value production-seam}
           (get-in production [:run :selector-seam])))
    (is (= (get-in (cert/certificate run-bytes clean-resource false)
                   [:traversal :counts])
           (get-in production [:traversal :counts])))
    (is (= :pass (:verdict missing)))
    (is (= {:status :absent :reason :not-recorded}
           (get-in missing [:run :selector-seam])))))

(deftest undeclared-hop-produces-a-failing-certificate
  (let [c (cert/certificate run-bytes clean-resource true)]
    (is (= :fail (:verdict c)))
    (is (= 1 (get-in c [:traversal :counts :undeclared])))
    (is (false? (get-in c [:checks :no-undeclared-traversal?])))))

(deftest dirty-or-missing-resource-status-cannot-certify
  (is (= :fail (:verdict (cert/certificate run-bytes nil false))))
  (is (= :fail (:verdict
                (cert/certificate run-bytes
                                  (assoc clean-resource :native-thread-exhaustion true)
                                  false)))))

(deftest incomplete-click-is-not-certified-as-a-complete-run
  (let [partial-run (assoc run-record
                           :run/id "partial-failed-run"
                           :click/id "partial-click"
                           :traceWritten false
                           :route [{:fromNode "R1" :toNode "R4"
                                    :via "partial-before-failure"
                                    :at_ "2026-09-01T00:00:01Z"}])
        partial-resource {:schema 2
                          :run/id "partial-failed-run"
                          :source-schema :wm-click-resource-v1
                          :observation-scope :shared-serving-jvm
                          :status :clean
                          :execution-outcome :incomplete
                          :pids-events-max-delta 0
                          :native-thread-exhaustion false
                          :tasks-peak 1
                          :tested-commit "tested-sha"
                          :serving-runner-code
                          {:availability :available
                           :identity {:git-head "tested-sha"
                                      :dirty? false :stable? true}}}
        c (cert/certificate (edn-bytes partial-run) partial-resource false)]
    (is (= :incomplete (:verdict c)))
    (is (= :clean (get-in c [:resource-status :status])))
    (is (= :incomplete (get-in c [:execution-status :status])))
    (is (true? (get-in c [:checks :resource-status-clean?])))
    (is (false? (get-in c [:checks :execution-complete?])))))

(deftest serving-program-must-match-tested-commit
  (let [base {:schema 2
              :run/id (:run/id run-record)
              :source-schema :wm-click-resource-v1
              :observation-scope :shared-serving-jvm
              :status :clean
              :execution-outcome :grounded-no-change
              :pids-events-max-delta 0
              :native-thread-exhaustion false
              :tested-commit "tested-sha"
              :serving-runner-code
              {:availability :available
               :identity {:git-head "different-sha" :dirty? false :stable? true}}}
        c (cert/certificate run-bytes base false)]
    (is (= :fail (:verdict c)))
    (is (= :mismatch (get-in c [:program-identity-status :status])))
    (is (= :serving-program-differs-from-tested-program
           (get-in c [:program-identity-status :reason])))
    (is (false? (get-in c [:checks :serving-program-matches-tested-program?])))))

(deftest diagnostic-receipt-with-missing-terminal-exit-is-incomplete
  (let [resource (-> clean-resource
                     (assoc :source-schema :futon-bounded-test-v1
                            :wrapper-exit nil)
                     (dissoc :service-result))
        c (cert/certificate run-bytes resource false)]
    (is (= :incomplete (:verdict c)))
    (is (= :incomplete (get-in c [:execution-status :status])))
    (is (false? (get-in c [:checks :execution-complete?])))))

(deftest invalid-fixture-pin-is-inside-written-verdict
  (let [out (str (java.nio.file.Files/createTempFile
                  "wm-bad-provenance-" ".edn"
                  (make-array java.nio.file.attribute.FileAttribute 0)))
        exit (cert/main ["--run" run-path
                         "--resource" "test/fixtures/wm-operational-certificate/resource-clean.edn"
                         "--run-sha256" (apply str (repeat 64 "0"))
                         "--certificate" out])
        written (edn/read-string (slurp out))]
    (is (= 1 exit))
    (is (= :fail (:verdict written)))
    (is (false? (get-in written [:fixture-pins :valid?])))
    (is (false? (get-in written [:checks :fixture-pins-valid?])))))
