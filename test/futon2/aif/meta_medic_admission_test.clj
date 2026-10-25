(ns futon2.aif.meta-medic-admission-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.meta-injury-ledger :as ledger]))

(def cause-a {:path "fixtures/cause-a.edn"
              :sha256 (apply str (repeat 64 "a"))})
(def cause-b {:path "fixtures/cause-b.edn"
              :sha256 (apply str (repeat 64 "b"))})
(def observation-pin {:path "fixtures/observation-only.edn"
                      :sha256 (apply str (repeat 64 "c"))})
(def medic-record-pin {:path "fixtures/synthetic-medic-receipt.edn"
                       :sha256 (apply str (repeat 64 "d"))})
(def authority-pin {:path "fixtures/synthetic-authority.edn"
                    :sha256 (apply str (repeat 64 "e"))})

(defn append! [history proposed]
  (let [result (ledger/append-transition history proposed)]
    (is (= :appended (:status result)) (pr-str result))
    (:ledger result)))

(defn open! [history injury cause click]
  (append! history {:injury-id injury :state :open :attempt 1
                    :cause-record-pin cause :failure-stage :construction
                    :failure-kind :synthetic-failure :detected-click click}))

(defn base-ledger []
  (let [opened-a (open! ledger/empty-ledger "injury/a" cause-a "click/a")
        observed (:ledger (ledger/append-observation
                           opened-a observation-pin :diagnostic-only))]
    (open! observed "injury/b" cause-b "click/b")))

(defn historical-payload [entry]
  {:kind :historical-misclassification
   :injury-id (:injury-id entry)
   :entry-id (str (:ordinal entry))
   :predecessor-entry-id nil
   :patch-id "patch/classifier"
   :reviewer "reviewer/independent"
   :evaluator "evaluator/independent"
   :evidence-pin "evidence/synthetic"
   :original-record-id (get-in entry [:cause-record-pin :path])
   :original-pin (get-in entry [:cause-record-pin :sha256])
   :classifier-revision "classifier/revision"
   :replay :producer-qualified-non-injury})

(defn operation [history]
  (let [entry (first (filter #(= "injury/a" (:injury-id %)) (:entries history)))
        payload (historical-payload entry)]
    {:operation-id "medic-operation/synthetic-a"
     :expected-ledger-pin (:ledger-pin history)
     :request {:candidate payload :evidence payload}
     :authority-receipt-pin authority-pin}))

(defn synthetic-authority [{:keys [operation current candidate evidence]}]
  (and (= authority-pin (:authority-receipt-pin operation))
       (= candidate evidence)
       (= ["injury/a" "1"] [(:injury-id current) (:entry-id current)])))

(defn appended-medic []
  (let [history (base-ledger)
        result (ledger/append-medic-repaired
                history medic-record-pin (operation history) synthetic-authority)]
    (is (= :appended (:status result)) (pr-str result))
    result))

(deftest qualified-medic-masks-only-exact-latest-occurrence
  (let [{:keys [ledger]} (appended-medic)
        raw (ledger/active-injuries ledger synthetic-authority)
        effective (ledger/effective-active-injuries ledger synthetic-authority)]
    (is (= #{"injury/a" "injury/b"} (set (keys (:injuries raw)))))
    (is (= #{"injury/b"} (set (keys (:injuries effective)))))
    (is (= :refused (:status (ledger/validate-ledger ledger))))
    (is (= :valid (:status (ledger/validate-ledger ledger synthetic-authority))))
    (is (= :wm/meta-injury-observation-v1
           (:schema (last (:entries ledger)))))
    (is (= (operation (base-ledger))
           (:medic-operation (last (:entries ledger)))))))

(deftest admission-refuses-missing-wrong-and-stale-evidence
  (let [history (base-ledger)
        op (operation history)
        deny (constantly false)
        missing (assoc-in op [:request :evidence] nil)
        wrong (assoc-in op [:request :evidence :original-pin]
                        (apply str (repeat 64 "f")))
        stale (assoc op :expected-ledger-pin (:ledger-pin ledger/empty-ledger))]
    (is (= :refused (:status (ledger/append-medic-repaired
                              history medic-record-pin op deny))))
    (is (= :refused (:status (ledger/append-medic-repaired
                              history medic-record-pin missing synthetic-authority))))
    (is (= :refused (:status (ledger/append-medic-repaired
                              history medic-record-pin wrong synthetic-authority))))
    (is (= :medic/stale-root (:reason (ledger/append-medic-repaired
                                       history medic-record-pin stale
                                       synthetic-authority))))))

(deftest recomputed-seals-do-not-authorize-forged-medic-evidence
  (let [history (base-ledger)
        previous (last (:entries history))
        forged-operation (assoc-in (operation history)
                                   [:request :evidence :original-pin]
                                   (apply str (repeat 64 "f")))
        forged-entry (ledger/seal-entry
                      {:schema ledger/observation-schema
                       :ordinal (inc (count (:entries history)))
                       :record-pin medic-record-pin
                       :observation-kind :medic-repaired
                       :medic-operation forged-operation
                       :predecessor {:entry-id (:entry-id previous)
                                     :entry-pin (:entry-pin previous)}})
        forged-ledger (ledger/seal-ledger
                       (update history :entries conj forged-entry))]
    (is (= :refused (:status (ledger/validate-ledger
                              forged-ledger synthetic-authority))))
    (is (some #{:entry/transition-invalid}
              (:defects (ledger/validate-ledger forged-ledger synthetic-authority))))))

(deftest duplicate-precedes-stale-and-conflict-follows-current-root
  (let [{history :ledger entry :entry} (appended-medic)
        old-operation (:medic-operation entry)
        duplicate (ledger/append-medic-repaired
                   history medic-record-pin old-operation synthetic-authority)
        changed-stale (assoc old-operation :operation-id "changed/stale")
        current-operation (assoc old-operation
                                 :operation-id "changed/current"
                                 :expected-ledger-pin (:ledger-pin history))
        stale (ledger/append-medic-repaired
               history medic-record-pin changed-stale synthetic-authority)
        conflict (ledger/append-medic-repaired
                  history medic-record-pin current-operation synthetic-authority)]
    (is (= :duplicate (:status duplicate)))
    (is (identical? history (:ledger duplicate)))
    (is (= :medic/stale-root (:reason stale)))
    (is (= :conflict (:status conflict)))
    (is (= history (:ledger conflict)))))

(deftest census-and-functional-snapshot-use-complete-validated-ledger
  (let [history (base-ledger)
        snapshot (ledger/authoritative-snapshot history "injury/a")]
    (is (= #{cause-a cause-b observation-pin}
           (ledger/classification-pins history)))
    (is (= :computed (:status snapshot)))
    (is (= 1 (get-in snapshot [:snapshot :attempt])))
    (is (= {:state :open} (get-in snapshot [:snapshot :origin])))
    (is (= (:ledger-pin history) (get-in snapshot [:snapshot :ledger-pin])))
    (is (= #{cause-a cause-b observation-pin}
           (get-in snapshot [:snapshot :seen-record-pins])))))

(deftest later-valid-transition-is-not-masked-by-old-correction
  (let [{history :ledger} (appended-medic)
        claimed (ledger/append-transition
                 history {:injury-id "injury/a" :state :claimed :attempt 1
                          :repair-id "repair/a" :algorithm "synthetic"
                          :author-session "author/new"
                          :reviewer-session "reviewer/new"}
                 synthetic-authority)
        effective (ledger/effective-active-injuries
                   (:ledger claimed) synthetic-authority)]
    (is (= :appended (:status claimed)) (pr-str claimed))
    (is (= #{"injury/a" "injury/b"} (set (keys (:injuries effective)))))
    (is (= :claimed (get-in effective [:injuries "injury/a" :state])))))

(deftest unsupported-snapshot-origin-refuses-explicitly
  (let [history (base-ledger)
        claimed (ledger/append-transition
                 history {:injury-id "injury/a" :state :claimed :attempt 1
                          :repair-id "repair/a" :algorithm "synthetic"
                          :author-session "author/new"
                          :reviewer-session "reviewer/new"})]
    (is (= :snapshot/unsupported-origin
           (:reason (ledger/authoritative-snapshot (:ledger claimed) "injury/a"))))))
