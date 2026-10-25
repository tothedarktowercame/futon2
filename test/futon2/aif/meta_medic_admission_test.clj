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
(def patch-sha "95145f09395145f09395145f09395145f0939514")
(def integration-sha "5a5b421bc5a5b421bc5a5b421bc5a5b421bc5a5b")

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

(defn ordinary-reopened-ledger []
  (let [injury "injury/ordinary-reopened"
        step (fn [history proposed]
               (:ledger (ledger/append-transition history proposed)))
        open (step ledger/empty-ledger
                   {:injury-id injury :state :open :attempt 1
                    :cause-record-pin cause-a :failure-stage :review
                    :failure-kind :synthetic-failure :detected-click "click/original"})
        claimed (step open {:injury-id injury :state :claimed :attempt 1
                            :repair-id "repair/ordinary" :algorithm "synthetic"
                            :author-session "author/one" :reviewer-session "reviewer/one"})
        authored (step claimed {:injury-id injury :state :authored :attempt 1
                                :repair-id "repair/ordinary" :patch-sha patch-sha
                                :worktree "fixtures/worktree"})
        reviewed (step authored {:injury-id injury :state :reviewed :attempt 1
                                 :repair-id "repair/ordinary" :verdict :approved
                                 :reviewer-sha patch-sha :reviewer-session "reviewer/one"})
        enacted (step reviewed {:injury-id injury :state :enacted :attempt 1
                                :repair-id "repair/ordinary" :patch-sha patch-sha
                                :retry-click "retry/ordinary/1"
                                :integration-commit integration-sha})]
    (:ledger (ledger/append-retry-result
              enacted {:injury-id injury :repair-id "repair/ordinary"
                       :patch-sha patch-sha :retry-click "retry/ordinary/1"
                       :new-cause-pin cause-b}))))

(defn genuine-operation [history candidate cause]
  (let [payload {:kind :genuine-repair
                 :injury-id (:injury-id candidate)
                 :entry-id (str (:ordinal candidate))
                 :predecessor-entry-id (str (dec (:ordinal candidate)))
                 :cause-record-id (:path cause) :cause-pin (:sha256 cause)
                 :acceptance-contract "contract/ordinary"
                 :patch-id "patch/ordinary" :source-revision patch-sha
                 :pre-replay-receipt "receipt/pre"
                 :post-replay-receipt "receipt/post"
                 :negative-control-receipt "receipt/negative"
                 :original-acceptance-repaired? true
                 :reviewer "reviewer/independent" :evaluator "evaluator/independent"
                 :evidence-pin "evidence/ordinary"}]
    {:operation-id "medic-operation/ordinary-reopened"
     :expected-ledger-pin (:ledger-pin history)
     :request {:candidate payload :evidence payload}
     :authority-receipt-pin authority-pin}))

(defn reopened-authority [{:keys [operation current candidate evidence]}]
  (and (= authority-pin (:authority-receipt-pin operation))
       (= candidate evidence)
       (= [(:injury-id candidate) (:entry-id candidate)
           (:predecessor-entry-id candidate) (:cause-record-id candidate)
           (:cause-pin candidate)]
          [(:injury-id current) (:entry-id current)
           (:predecessor-entry-id current) (:cause-record-id current)
           (:cause-pin current)])))

(deftest ordinary-reopened-cause-projects-and-carries-through-public-admission
  (let [history (ordinary-reopened-ledger)
        reopened (last (:entries history))
        operation (genuine-operation history reopened cause-b)
        admitted (ledger/append-medic-repaired
                  history medic-record-pin operation reopened-authority)
        old-cause-operation (-> operation
                                (assoc-in [:request :candidate :cause-record-id]
                                          (:path cause-a))
                                (assoc-in [:request :candidate :cause-pin]
                                          (:sha256 cause-a))
                                (assoc-in [:request :evidence :cause-record-id]
                                          (:path cause-a))
                                (assoc-in [:request :evidence :cause-pin]
                                          (:sha256 cause-a)))
        old-cause (ledger/append-medic-repaired
                   history medic-record-pin old-cause-operation reopened-authority)
        carried (ledger/append-transition
                 history {:injury-id (:injury-id reopened) :state :claimed :attempt 2
                          :repair-id "repair/ordinary" :algorithm "synthetic"
                          :author-session "author/two" :reviewer-session "reviewer/two"})
        carried-entry (last (:entries (:ledger carried)))
        carried-operation (genuine-operation (:ledger carried) carried-entry cause-b)]
    (is (= :appended (:status admitted)) (pr-str admitted))
    (is (= :medic/binding-mismatch (:reason old-cause)))
    (is (= :appended (:status carried)))
    (is (= :appended
           (:status (ledger/append-medic-repaired
                     (:ledger carried) medic-record-pin carried-operation
                     reopened-authority))))))

(deftest unsupported-snapshot-origin-refuses-explicitly
  (let [history (base-ledger)
        claimed (ledger/append-transition
                 history {:injury-id "injury/a" :state :claimed :attempt 1
                          :repair-id "repair/a" :algorithm "synthetic"
                          :author-session "author/new"
                          :reviewer-session "reviewer/new"})]
    (is (= :snapshot/unsupported-origin
           (:reason (ledger/authoritative-snapshot (:ledger claimed) "injury/a"))))))
