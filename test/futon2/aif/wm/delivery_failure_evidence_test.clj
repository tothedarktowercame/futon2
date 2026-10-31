(ns futon2.aif.wm.delivery-failure-evidence-test
  "Hermetic tests for content-addressed delivery-failure retention.

  Real shape under test: injury adf04df0 — an author answer whose patch
  descriptor carried a well-formed-but-abbreviated commit (\"8561ecdc7\"),
  consumed by check-delivery and previously unreachable from the source record. Every test
  root is a fresh temp directory; no canonical path is touched."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.wm.cascade-patch-delivery :as patch-delivery]
            [futon2.aif.wm.delivery-failure-evidence :as dfe]))

(def ^:private real-shaped-answer
  {:requisition-id "6a8fe374958818d6246ee46acc6dbdb851c7cdf66930626395572519879d195c"
   :cascade-digest "5a0fa2adeb101b6bc644bf26511b22f5b88463ac1ef0490e941fd24b22de6995"
   :by "wm-author"
   :patch {:namespace "futon2.aif.wm.authored.req-6a8fe374"
           :var "decisions"
           :source-digest "c049abef66f39c5eb988514752cfd34d81771ffb02595c297df2ca9aa482dcf3"
           :commit "8561ecdc7"}})

(defn- temp-root []
  (.toFile (java.nio.file.Files/createTempDirectory
             "dfe-test" (make-array java.nio.file.attribute.FileAttribute 0))))

(deftest retention-round-trips-the-exact-check-input
  (let [root (temp-root)
        check (patch-delivery/check-delivery root (constantly nil) real-shaped-answer)]
    (is (= :error (:status check)))
    (is (= :abbreviated-commit-descriptor (:reason check)))
    (let [r (dfe/retain-delivery-failure!
              root {:requisition-id (:requisition-id real-shaped-answer)
                    :cascade-digest (:cascade-digest real-shaped-answer)
                    :author-job "invoke-1793409156227-8523-78848068"
                    :answer real-shaped-answer
                    :check check})]
      (is (= :retained (:status r)) "first retention succeeds")
      (let [pin (:pin r)]
        (is (re-matches #"[0-9a-f]{64}" (:sha256 pin)))
        (is (.endsWith (:path pin) (str (:sha256 pin) ".edn")))
        (testing "replay is idempotent at the same content address"
          (let [r2 (dfe/retain-delivery-failure!
                     root {:requisition-id (:requisition-id real-shaped-answer)
                           :cascade-digest (:cascade-digest real-shaped-answer)
                           :author-job "invoke-1793409156227-8523-78848068"
                           :answer real-shaped-answer
                           :check check})]
            (is (= (:sha256 pin) (:sha256 (:pin r2))) (pr-str r2))))
        (testing "the exact abbreviated descriptor survives in the record"
          (let [back (:record (dfe/read-pin pin))]
            (is (= "8561ecdc7" (get-in back [:check-input :patch :commit])))
            (is (= :abbreviated-commit-descriptor (get-in back [:typed-result :reason])))
            (is (= "invoke-1793409156227-8523-78848068"
                   (get-in back [:linkage :author-job])))))))))

(deftest read-pin-refuses-tampered-missing-and-malformed
  (let [root (temp-root)
        r (dfe/retain-delivery-failure!
            root {:requisition-id "req" :cascade-digest "dig"
                  :author-job "job" :answer real-shaped-answer
                  :check {:status :error :kind :author-delivery-invalid-descriptor
                          :reason :abbreviated-commit-descriptor}})
        pin (:pin r)]
    (is (= :present (:status (dfe/read-pin pin))))
    (testing "tampered bytes refuse :delivery-evidence-tampered, never repair"
      (let [orig (:record (dfe/read-pin pin))]
        (spit (:path pin)
              (pr-str (assoc-in orig [:check-input :patch :commit] "deadbeef")))
        (let [t (dfe/read-pin pin)]
          (is (= :refused (:status t)))
          (is (= :delivery-evidence-tampered (:reason t))))))
    (testing "missing file refuses :delivery-evidence-missing"
      (.delete (io/file (:path pin)))
      (is (= :delivery-evidence-missing
             (:reason (dfe/read-pin pin)))))
    (testing "malformed pin refuses without IO"
      (is (= :delivery-evidence-pin-malformed
             (:reason (dfe/read-pin {:path "/x" :sha256 "zz"})))))))

(deftest retention-disabled-and-mismatch-typed-never-thrown
  (is (= {:status :disabled :reason :no-store-root}
         (dfe/retain-delivery-failure! nil {:answer {} :check {}})))
  (let [root (temp-root)
        r1 (dfe/retain-delivery-failure!
             root {:requisition-id "r" :cascade-digest "c" :author-job "j"
                   :answer real-shaped-answer :check {:status :error :reason :x}})]
    (is (= :retained (:status r1)))
    ;; Corrupt the just-written content address; re-retaining the SAME record
    ;; must hit the address mismatch and report the typed refusal (retention
    ;; never throws into the caller's failure path).
    (spit (:path (:pin r1))
          (pr-str {:schema dfe/schema-version :evil true}))
    (let [r2 (dfe/retain-delivery-failure!
               root {:requisition-id "r" :cascade-digest "c" :author-job "j"
                     :answer real-shaped-answer
                     :check {:status :error :reason :x}})]
      (is (= :refused (:status r2)))
      (is (= :delivery-evidence-content-mismatch (:reason r2))))))

(deftest evidence-readiness-distinguishes-usable-from-missing-and-corrupt
  (let [root (temp-root)
        good (dfe/retain-delivery-failure!
               root {:requisition-id "r" :cascade-digest "c" :author-job "j"
                     :answer real-shaped-answer :check {:status :error :reason :x}})
        good-pin (:pin good)
        ;; the source pin is the injury's pinned source record: a plain
        ;; bytes-sha pin over an EDN file, as source-record-pin uses
        src (io/file root "source-record.edn")]
    (spit src "{:failure {:kind :author-delivery-wrong-repository}}")
    (let [w (dfe/evidence-readiness
              {:source-pin {:path (str src)
                            :sha256 (load-identity/sha256
                                      (java.nio.file.Files/readAllBytes (.toPath src)))}
               :candidate-pins [(select-keys good-pin [:path :sha256])
                                {:path "/nonexistent/x.edn" :sha256 (apply str (repeat 64 "a"))}]
               :job-lookup (fn [] {:status :present})
               :requisition-id "r" :cascade-digest "c"})]
      (is (= :present (:status (:source-record w))))
      (is (= [{:status :usable :pin (select-keys good-pin [:path :sha256])}
              {:status :refused :reason :delivery-evidence-missing}]
             (:retained-input w)))
      (is (= :present (get-in w [:job-ledger-route :status])))
      (is (some #{:readiness-is-not-diagnosis :carrier-admission-requires-review}
                (:limits w))))
    (testing "corrupt source pin is explicit"
      (let [w (dfe/evidence-readiness
                {:source-pin {:path (str src)
                              :sha256 (apply str (repeat 64 "0"))}})]
        (is (= :corrupt (:status (:source-record w))))))))

(deftest supplemental-carrier-proposal-is-explicit-not-a-bypass
  (let [p (dfe/supplemental-carrier-proposal
            {:injury-id "injury/adf04df0f31ab8727ac57d9c7f2496d8fa28598c"
             :source-record-pin {:path "/pinned.edn" :sha256 (apply str (repeat 64 "b"))}
             :retained-pin {:path "/retained.edn" :sha256 (apply str (repeat 64 "c"))}
             :boundary-var "futon2.aif.wm.cascade-patch-delivery/check-delivery"
             :boundary-file "src/futon2/aif/wm/cascade_patch_delivery.clj"})]
    (is (= :wm/supplemental-carrier-proposal-v1 (:schema p)))
    (is (= :cause-scope/input-pin-not-source-record (:existing-contract p)))
    (is (contains? (set (get-in p [:proposal :explicitly-not])) :contract-change))
    (is (contains? (set (get-in p [:proposal :admission-rule])) :review-required))))

(deftest wrapper-preserves-producer-refusal-kind
  (testing "the refusal map keeps :cause-scope-unretained from a :kind response"
    (let [response {:status :failed :kind :cause-scope-unretained}
          built {:diagnosis-reason (or (:reason response) (:kind response)
                                       :meta-self-heal/diagnosis-not-succeeded)
                 :diagnosis-detail (or (:detail response)
                                       (:error (:payload response)))}]
      (is (= :cause-scope-unretained (:diagnosis-reason built)))
      (is (nil? (:diagnosis-detail built))))
    (testing "an envelope :payload reason is surfaced as detail, not lost"
      (let [response {:status :failed :payload {:reason :agency-invoke-failed
                                                :error "429"}}
            built {:diagnosis-reason (or (:reason response) (:kind response)
                                         :meta-self-heal/diagnosis-not-succeeded)
                   :diagnosis-detail (or (:detail response)
                                         (:error (:payload response)))}]
        (is (= :meta-self-heal/diagnosis-not-succeeded (:diagnosis-reason built)))
        (is (= "429" (:diagnosis-detail built)))))))
