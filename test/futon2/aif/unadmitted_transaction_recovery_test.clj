(ns futon2.aif.unadmitted-transaction-recovery-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-repair-coordinator :as coordinator]
            [futon2.aif.meta-self-heal-production :as production]))

(def injury-id "injury/2aef-fixture")
(def repair-id "repair/2f682638-fixture")
(def patch-a (apply str (repeat 40 "a")))
(def patch-b (apply str (repeat 40 "b")))
(def review-sha (apply str (repeat 40 "c")))
(def source-pin {:path "fixture/ledger.edn" :sha256 (apply str (repeat 64 "1"))})
(def cause-pin {:path "fixture/cause.edn" :sha256 (apply str (repeat 64 "2"))})
(def diagnosis-pin {:path "fixture/diagnosis.edn" :sha256 (apply str (repeat 64 "3"))})
(def authority-pin {:path "fixture/medic-authority.edn"
                    :sha256 (apply str (repeat 64 "4"))})

(defn- append! [history transition]
  (let [result (ledger/append-transition history transition)]
    (assert (= :appended (:status result)) (pr-str result))
    (:ledger result)))

(defn- attempt-options [attempt]
  {:diagnosis-evidence-pin diagnosis-pin
   :acceptance-test {:id "2aef-recovery"
                     :command ["clojure" "-M:test" "-n" "fixture"]
                     :expected {:exit 0}}
   :author "wm-author" :reviewer "wm-reviewer"
   :repair-reviewer "wm-repair-reviewer"
   :author-session (str "author-session-" attempt)
   :reviewer-session (str "reviewer-session-" attempt)
   :repair-reviewer-session (str "repair-reviewer-session-" attempt)
   :target-repository "futon2" :target-worktree "/fixture/worktree"})

(defn- durable [history]
  {:schema coordinator/durable-ledger-schema :status :loaded
   :source-pin source-pin :ledger history})

(defn- claimed-through-enacted [history attempt patch author-pin]
  (let [order (:work-order (coordinator/prepare (durable history)
                                                (attempt-options attempt)))
        history (append! history
                         {:injury-id injury-id :repair-id repair-id
                          :state :claimed :attempt attempt
                          :algorithm "dedicated-meta-self-heal"
                          :author-session (:author-session order)
                          :reviewer-session (:reviewer-session order)
                          :repair-reviewer-session (:repair-reviewer-session order)
                          :work-order-pin (:work-order-pin order)})
        history (append! history
                         {:injury-id injury-id :repair-id repair-id
                          :state :authored :attempt attempt :patch-sha patch
                          :worktree "/fixture/worktree"
                          :port-receipt-pin author-pin})
        history (append! history
                         {:injury-id injury-id :repair-id repair-id
                          :state :reviewed :attempt attempt :verdict :approved
                          :reviewer-sha review-sha
                          :reviewer-session (:reviewer-session order)})]
    (append! history
             {:injury-id injury-id :repair-id repair-id
              :state :enacted :attempt attempt :retry-click (str "retry-" attempt)
              :integration-commit patch})))

(defn- enacted-attempt-two []
  (let [author-1 {:path "receipts/attempt-1-author.edn"
                  :sha256 (apply str (repeat 64 "5"))}
        author-2 {:path "receipts/ledger-attempt-2-author.edn"
                  :sha256 (apply str (repeat 64 "6"))}
        opened (append! ledger/empty-ledger
                        {:injury-id injury-id :state :open :attempt 1
                         :cause-record-pin cause-pin :failure-stage :selection
                         :failure-kind :tripwire-tripped :detected-click "click-open"})
        enacted-1 (claimed-through-enacted opened 1 patch-a author-1)
        reopened (:ledger (ledger/append-retry-result
                           enacted-1 {:injury-id injury-id :repair-id repair-id
                                      :reason :retry-failed :retry-click "retry-1"
                                      :patch-sha patch-a
                                      :new-cause-pin {:path "fixture/retry-1.edn"
                                                      :sha256 (apply str (repeat 64 "7"))}}))]
    {:history (claimed-through-enacted reopened 2 patch-a author-2)
     :ledger-author-pin author-2}))

(defn- transaction [history pin patch]
  (let [prepared (coordinator/prepare (durable history) (attempt-options 2))
        order (:work-order prepared)
        receipt (coordinator/seal-port-receipt
                 {:phase :author :injury-id injury-id :repair-id repair-id
                  :attempt 2 :predecessor-pin
                  (:entry-pin (last (filter #(= injury-id (:injury-id %))
                                            (:entries history))))
                  :status :succeeded
                  :payload {:patch-sha patch :worktree "/fixture/worktree"}})]
    (#'coordinator/seal-transaction
     {:status :active :work-order order
      :receipts [(assoc receipt :receipt-pin pin)]})))

(defn- temp-case [label history tx]
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                       (str label "-")
                       (make-array java.nio.file.attribute.FileAttribute 0)))
        ledger-path (.getPath (io/file root "ledger.edn"))
        transaction-path (.getPath (io/file root "transaction.edn"))]
    (spit ledger-path (str (pr-str history) "\n"))
    (spit transaction-path (str (pr-str tx) "\n"))
    {:ledger-path ledger-path :transaction-path transaction-path}))

(defn- authority [expected-pin]
  (fn [{:keys [operation candidate evidence]}]
    (and (= expected-pin (:authority-receipt-pin operation))
         (= candidate evidence))))

(defn- recovery-opts [paths verifier]
  (merge paths {:injury-id injury-id :repair-id repair-id :attempt 2
                :medic-authority {:authority-receipt-pin authority-pin}
                :authority-verifier verifier}))

(defn- succeeded-author-receipt [request]
  (let [order (:work-order request)]
    (coordinator/seal-port-receipt
     {:phase :author
      :injury-id (:injury-id order)
      :repair-id (:repair-id order)
      :attempt (:attempt order)
      :predecessor-pin (:predecessor-pin request)
      :status :succeeded
      :payload {:patch-sha patch-b :worktree "/fixture/worktree"}})))

(deftest divergent-attempt-is-abandoned-archived-and-reopened
  (let [{:keys [history ledger-author-pin]} (enacted-attempt-two)
        transaction-pin {:path "receipts/transaction-attempt-2-author.edn"
                         :sha256 (apply str (repeat 64 "8"))}
        tx (transaction history transaction-pin patch-b)
        paths (temp-case "unadmitted-recovery" history tx)
        tx-bytes (slurp (:transaction-path paths))
        result (production/recover-unadmitted-transaction!
                (recovery-opts paths (authority authority-pin)))
        persisted (edn/read-string (slurp (:ledger-path paths)))
        states (->> (:entries persisted)
                    (filter #(= injury-id (:injury-id %)))
                    (mapv :state))]
    (is (= :reopened (:status result)) (pr-str result))
    (is (= [:abandoned :reopened] (subvec states (- (count states) 2))))
    (is (= :unadmitted-transaction-receipt
           (get-in result [:abandoned-entry :reason])))
    (is (= ledger-author-pin
           (get-in result [:abandoned-entry :ledger-port-receipt-pin])))
    (is (= transaction-pin
           (get-in result [:abandoned-entry :transaction-port-receipt-pin])))
    (is (= (load-identity/sha256 (.getBytes tx-bytes "UTF-8"))
           (get-in result [:archive-pin :sha256])))
    (is (= tx-bytes (slurp (get-in result [:archive-pin :path]))))
    (is (not (.exists (io/file (:transaction-path paths)))))
    (is (= :valid (:status (ledger/validate-ledger persisted
                                                   (authority authority-pin)))))
    (is (empty? (filter #(and (= :authored (:state %)) (= 3 (:attempt %)))
                        (:entries persisted))))
    (let [next-prepared (coordinator/prepare (durable persisted)
                                             (attempt-options 3))
          next-result (coordinator/coordinate
                       (durable persisted) (:transaction next-prepared)
                       {:author succeeded-author-receipt
                        :stop-after :author})]
      (is (not= :coordinator/resume-identity-refused (:reason next-result)))
      (is (= :checkpoint (:status next-result)) (pr-str next-result))
      (is (= :author (:phase next-result)))
      (is (= :authored (:state (last (get-in next-result [:ledger :entries])))))
      (is (= 3 (:attempt (last (get-in next-result [:ledger :entries]))))))))

(deftest refusals-write-nothing
  (let [{:keys [history ledger-author-pin]} (enacted-attempt-two)]
    (testing "matching author identity is not divergence"
      (let [tx (transaction history ledger-author-pin patch-a)
            paths (temp-case "no-divergence" history tx)
            before-ledger (slurp (:ledger-path paths))
            before-tx (slurp (:transaction-path paths))
            result (production/recover-unadmitted-transaction!
                    (recovery-opts paths (authority authority-pin)))]
        (is (= :meta-self-heal/recovery-divergence-not-confirmed (:reason result)))
        (is (= before-ledger (slurp (:ledger-path paths))))
        (is (= before-tx (slurp (:transaction-path paths))))))
    (testing "attempt at limit"
      (let [limited (ledger/seal-ledger (assoc history :attempt-limit 2))
            tx (transaction limited {:path "receipts/other.edn"
                                     :sha256 (apply str (repeat 64 "9"))} patch-b)
            paths (temp-case "attempt-limit" limited tx)
            before-ledger (slurp (:ledger-path paths))
            before-tx (slurp (:transaction-path paths))
            result (production/recover-unadmitted-transaction!
                    (recovery-opts paths (authority authority-pin)))]
        (is (= :meta-self-heal/recovery-attempt-limit-exhausted (:reason result)))
        (is (= before-ledger (slurp (:ledger-path paths))))
        (is (= before-tx (slurp (:transaction-path paths))))))
    (testing "missing and rejected medic authority"
      (doseq [[label opts-fn expected]
              [["missing" #(dissoc (recovery-opts % nil) :medic-authority)
                :meta-self-heal/recovery-medic-authority-required]
               ["rejected" #(recovery-opts % (constantly false))
                :meta-self-heal/recovery-medic-authority-refused]]]
        (let [tx (transaction history {:path "receipts/unadmitted.edn"
                                       :sha256 (apply str (repeat 64 "a"))} patch-b)
              paths (temp-case (str "authority-" label) history tx)
              before-ledger (slurp (:ledger-path paths))
              before-tx (slurp (:transaction-path paths))
              result (production/recover-unadmitted-transaction! (opts-fn paths))]
          (is (= expected (:reason result)))
          (is (= before-ledger (slurp (:ledger-path paths))))
          (is (= before-tx (slurp (:transaction-path paths)))))))))
