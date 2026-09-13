(ns futon2.aif.machine-slow-feedback-completeness-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.machine-slow-feedback-capture :as capture]
            [futon2.aif.machine-slow-feedback-completeness :as completeness]
            [futon2.aif.machine-slow-feedback-retrospective-projection :as projection]
            [futon2.aif.machine-slow-feedback-store-v2 :as store]
            [futon2.aif.machine-slow-feedback-store-v2-test :as store-test])
  (:import (java.nio.charset StandardCharsets)
           (java.security MessageDigest)
           (java.util Base64)))

(defn- sha256 [^bytes bs]
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (doto (MessageDigest/getInstance "SHA-256") (.update bs))))))
(defn- descriptor [record]
  (let [bs (.getBytes (pr-str record) StandardCharsets/UTF_8)]
    {:bytes/base64 (.encodeToString (Base64/getEncoder) bs) :expected-sha256 (sha256 bs)}))
(defn- refusal [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))
(defn- fixture []
  (let [[_ store provenance] (#'store-test/setup)
        _ (store/commit! store (#'store-test/pin provenance))
        capture-artifact (capture/construct (store/capture store))
        capture-pin {:bytes/base64 (:bytes/base64 capture-artifact)
                     :expected-sha256 (:sha256 capture-artifact)}
        capture-record (:record (capture/readback capture-pin))
        target (get-in capture-record [:application-universe 0 :application/id])
        p (projection/project {:capture-pin capture-pin :target-application-id target})
        root "fixture-root" candidate "fixture-candidate" owner "fixture-acquisition-owner"
        reviewer "fixture-reviewer" boundary-id "fixture-boundary"
        inventory (descriptor
                   {:schema :wm/e6b-writer-inventory-v1 :scope :isolated-test
                    :authority/owner owner :authority/root root
                    :store/id (get-in p [:capture :store/id])
                    :owner/generation (get-in p [:capture :generation])
                    :boundary/id boundary-id :writers ["fixture-store-owner"]
                    :covered-writer-roles [:store-v2-exclusive-owner]
                    :issued-at "2026-09-13T01:00:00Z"})
        census-record {:schema :wm/e6b-complete-census-v1 :scope :isolated-test
                       :authority/owner owner :authority/root root
                       :store/id (get-in p [:capture :store/id])
                       :owner/generation (get-in p [:capture :generation]) :boundary/id boundary-id
                       :writer-inventory/raw-sha256 (:expected-sha256 inventory)
                       :capture/raw-sha256 (:sha256 capture-artifact)
                       :ledger-source/raw-sha256 (get-in p [:digest-roles :ledger-source/raw])
                       :target/transition-subject (get-in p [:target :transition/subject])
                       :ordered-universe (:application-universe capture-record)
                       :acquired-at "2026-09-13T01:03:00Z"}
        census (descriptor census-record)
        boundary-record {:schema :wm/e6b-acquisition-boundary-v1 :scope :isolated-test
                         :owner/id owner :authority/root root :candidate/id candidate
                         :store/id (get-in p [:capture :store/id])
                         :owner/generation (get-in p [:capture :generation])
                         :boundary/id boundary-id
                         :writer-inventory/raw-sha256 (:expected-sha256 inventory)
                         :complete-census/raw-sha256 (:expected-sha256 census)
                         :intake/status :closed :lifecycle/status :reconciled
                         :lease/status :held-through-capture
                         :closed-at "2026-09-13T01:01:00Z"
                         :capture-started-at "2026-09-13T01:02:00Z"
                         :capture-finished-at "2026-09-13T01:03:00Z"}
        boundary (descriptor boundary-record)
        subject-record {:schema :wm/e6b-completeness-subject-v1 :scope :isolated-test
                        :capture {:bytes/base64 (:bytes/base64 capture-artifact)
                                  :raw-sha256 (:sha256 capture-artifact)
                                  :store/id (get-in p [:capture :store/id])
                                  :owner/generation (get-in p [:capture :generation])
                                  :head-digest (get-in p [:capture :head-digest])
                                  :ordered-chain-digests (get-in p [:capture :chain-digests])
                                  :ordered-universe (:application-universe capture-record)}
                        :ledger-source {:bytes/base64 (get-in p [:application-ledger/source :bytes/base64])
                                        :raw-sha256 (get-in p [:digest-roles :ledger-source/raw])}
                        :row-vector {:bytes/base64 (get-in p [:application-ledger :bytes/base64])
                                     :raw-sha256 (get-in p [:digest-roles :ledger/derived])}
                        :target {:application/id target
                                 :transition/subject (get-in p [:target :transition/subject])
                                 :transaction-sha256 (get-in p [:target :index-entry :transaction-sha256])
                                 :provenance-sha256 (get-in p [:target :index-entry :provenance-sha256])}
                        :ordered-universe (:application-universe capture-record)
                        :acquisition {:boundary/id boundary-id :owner/id owner
                                      :owner/generation (get-in p [:capture :generation])
                                      :writer-inventory-sha256 (:expected-sha256 inventory)
                                      :complete-census/raw-sha256 (:expected-sha256 census)
                                      :closed-at "2026-09-13T01:01:00Z"
                                      :capture-finished-at "2026-09-13T01:03:00Z"}}
        subject (descriptor subject-record)
        commission (descriptor {:schema :wm/e6b-review-commission-v1 :scope :isolated-test
                                :authority/root root :commission/id "commission-1"
                                :reviewer/id reviewer :candidate/id candidate
                                :subject/raw-sha256 (:expected-sha256 subject)
                                :boundary/id boundary-id :issued-at "2026-09-13T01:04:00Z"})
        review-record {:schema :wm/e6b-completeness-review-v1 :scope :isolated-test
                       :authority/root root :reviewer/id reviewer :job/id "review-job"
                       :trace/id "review-trace" :subject/raw-sha256 (:expected-sha256 subject)
                       :boundary/id boundary-id :outcome :accepted
                       :reviewed-at "2026-09-13T01:06:00Z"}
        review (descriptor review-record)
        execution (descriptor {:schema :wm/e6b-review-execution-v1 :scope :isolated-test
                               :authority/root root :commission/id "commission-1"
                               :reviewer/id reviewer :job/id "review-job" :trace/id "review-trace"
                               :subject/raw-sha256 (:expected-sha256 subject)
                               :review-artifact/raw-sha256 (:expected-sha256 review)
                               :status :completed :started-at "2026-09-13T01:05:00Z"
                               :finished-at "2026-09-13T01:08:00Z"})
        origin-record {:schema :wm/e6b-review-origin-v1 :scope :isolated-test
                       :authority/root root :origin/kind :synthetic-fixture-ledger
                       :origin/id "fixture-review-origin" :origin/owner "fixture-host-owner"
                       :origin/provenance-review-sha256 (apply str (repeat 64 "d"))
                       :reviewer/id reviewer :commission/id "commission-1"
                       :commission/raw-sha256 (:expected-sha256 commission)
                       :subject/raw-sha256 (:expected-sha256 subject)
                       :job/id "review-job" :trace/id "review-trace"
                       :review-artifact/raw-sha256 (:expected-sha256 review)
                       :artifact/retained-at "2026-09-13T01:07:00Z"
                       :terminal/status :completed :finished-at "2026-09-13T01:08:00Z"}
        origin (descriptor origin-record)
        acceptance (descriptor {:schema :wm/e6b-completeness-acceptance-v1
                                :scope :isolated-test :authority/root root
                                :authority/owner "fixture-acceptance-owner"
                                :reviewer/id reviewer :job/id "review-job" :trace/id "review-trace"
                                :commission/id "commission-1"
                                :subject/raw-sha256 (:expected-sha256 subject)
                                :review-artifact/raw-sha256 (:expected-sha256 review)
                                :boundary/id boundary-id :outcome :accepted
                                :accepted-at "2026-09-13T01:09:00Z"})
        roles {:capture-artifact capture-pin :writer-inventory inventory :complete-census census
               :completeness-subject subject :acquisition-boundary boundary
               :review-commission commission :review-execution execution
               :review-artifact review :review-origin origin :acceptance acceptance}]
    {:store store :config {:mode :isolated-test :authority-root root :candidate/id candidate
                           :target-application-id target
                           :expected-review-origin
                           (select-keys origin-record [:origin/kind :origin/id :origin/owner
                                                       :origin/provenance-review-sha256])
                           :roles roles}}))
(defn- replace-record [config role f]
  (assoc-in config [:roles role]
            (descriptor (f (:record (#'completeness/resolve-role role (get-in config [:roles role])))))))
(defn- rebind-review-artifact [config review-f]
  (let [reviewed (replace-record config :review-artifact review-f)
        review-pin (get-in reviewed [:roles :review-artifact :expected-sha256])]
    (reduce (fn [c role]
              (replace-record c role
                              (fn [x] (assoc x :review-artifact/raw-sha256 review-pin))))
            reviewed [:review-execution :review-origin :acceptance])))

(deftest synthetic-independent-role-mechanism-positive
  (let [{:keys [store config]} (fixture) result (completeness/validate config)]
    (is (= :join-mechanism-validated (:status result)))
    (is (= :synthetic-independent-role-fixture (:evidence/scope result)))
    (is (= :none (:authority/status result)))
    (is (false? (:retrospective-success? result)))
    (is (false? (:restart-authorized? result)))
    (store/release! store)))

(deftest production-and-authority-borrowing-refuse
  (let [{:keys [store config]} (fixture)]
    (is (= :e6b-completeness/production-authority-unavailable
           (refusal #(completeness/validate (assoc config :mode :production)))))
    (is (= :e6b-completeness/authority-join-invalid
           (refusal #(completeness/validate
                      (replace-record config :writer-inventory
                                      (fn [x] (assoc x :authority/owner (:candidate/id config))))))))
    (is (= :e6b-completeness/authority-join-invalid
           (refusal #(completeness/validate
                      (replace-record config :acceptance (fn [x] (assoc x :scope :production)))))))
    (store/release! store)))

(deftest census-review-and-chronology-controls
  (let [{:keys [store config]} (fixture)]
    (doseq [[role mutate]
            [[:complete-census #(assoc % :ordered-universe [])]
             [:review-execution #(assoc % :status :pending)]
             [:review-artifact #(assoc % :subject/raw-sha256 (apply str (repeat 64 "f")))]
             [:review-commission #(assoc % :reviewer/id (:candidate/id config))]
             [:acceptance #(assoc % :accepted-at "2026-09-13T01:02:00Z")]]]
      (is (some? (refusal #(completeness/validate (replace-record config role mutate))))))
    (let [without-census (replace-record config :complete-census
                                         #(assoc % :ordered-universe []))
          without-both (replace-record without-census :completeness-subject
                                       #(assoc % :ordered-universe []
                                               :capture (assoc (:capture %)
                                                               :ordered-universe [])))]
      (is (some? (refusal #(completeness/validate without-both)))))
    (store/release! store)))

(deftest census-identity-pin-and-boundary-controls
  (let [{:keys [store config]} (fixture)
        census-record (:record (#'completeness/resolve-role
                                :complete-census (get-in config [:roles :complete-census])))
        first-entry (first (:ordered-universe census-record))]
    (doseq [[role mutate]
            [[:complete-census (fn [x] (assoc x :ordered-universe
                                               [first-entry first-entry]))]
             [:complete-census (fn [x] (assoc x :owner/generation 99))]
             [:acquisition-boundary (fn [x] (assoc x :boundary/id "borrowed-boundary"))]
             [:acceptance (fn [x] (assoc x :review-artifact/raw-sha256
                                         (apply str (repeat 64 "e"))))]
             [:review-commission (fn [x] (dissoc x :issued-at))]]]
      (is (some? (refusal #(completeness/validate (replace-record config role mutate))))))
    (is (= :e6b-completeness/role-pin-mismatch
           (refusal #(completeness/validate
                      (assoc-in config [:roles :complete-census :expected-sha256]
                                (apply str (repeat 64 "0")))))))
    (store/release! store)))

(deftest malformed-role-and-missing-role-refuse
  (let [{:keys [store config]} (fixture)
        bad-bytes (byte-array [(unchecked-byte 0xc3) (byte 0x28)])]
    (is (= :e6b-completeness/config-invalid
           (refusal #(completeness/validate (update config :roles dissoc :acceptance)))))
    (is (= :e6b-completeness/config-invalid
           (refusal #(completeness/validate (update config :roles dissoc :review-origin)))))
    (is (= :e6b-completeness/invalid-edn
           (refusal #(completeness/validate
                      (assoc-in config [:roles :acceptance]
                                {:bytes/base64 (.encodeToString (Base64/getEncoder) bad-bytes)
                                 :expected-sha256 (sha256 bad-bytes)})))))
    (let [bs (.getBytes "{} {}" StandardCharsets/UTF_8)]
      (is (= :e6b-completeness/invalid-edn-cardinality
             (refusal #(completeness/validate
                        (assoc-in config [:roles :acceptance]
                                  {:bytes/base64 (.encodeToString (Base64/getEncoder) bs)
                                   :expected-sha256 (sha256 bs)}))))))
    (store/release! store)))

(deftest review-origin-and-chronology-controls
  (let [{:keys [store config]} (fixture)]
    (doseq [changed
            [(replace-record config :review-origin
                             (fn [x] (assoc x :commission/raw-sha256
                                            (apply str (repeat 64 "f")))))
             (replace-record config :review-origin
                             (fn [x] (assoc x :artifact/retained-at
                                            "2026-09-13T01:09:00Z")))
             (rebind-review-artifact config
                                     (fn [x] (assoc x :reviewed-at
                                                    "2026-09-13T01:09:00Z")))
             (replace-record config :review-origin
                             (fn [x] (assoc x :scope :production)))
             (assoc-in config [:expected-review-origin :origin/id] "borrowed-origin")]]
      (is (= :e6b-completeness/authority-join-invalid
             (refusal #(completeness/validate changed)))))
    ;; Coherently rewrite review/execution/acceptance labels while leaving the
    ;; independently configured origin unchanged: the origin join must refuse.
    (let [reviewed (replace-record config :review-artifact
                                  (fn [x] (assoc x :job/id "fake-job" :trace/id "fake-trace")))
          review-pin (get-in reviewed [:roles :review-artifact :expected-sha256])
          changed (reduce
                   (fn [c role]
                     (replace-record c role
                                     (fn [x] (assoc x :job/id "fake-job" :trace/id "fake-trace"
                                                    :review-artifact/raw-sha256 review-pin))))
                   reviewed [:review-execution :acceptance])]
      (is (= :e6b-completeness/authority-join-invalid
             (refusal #(completeness/validate changed)))))
    (store/release! store)))

(deftest review-chronology-equality-boundary-is-valid
  (let [{:keys [store config]} (fixture)
        t "2026-09-13T01:07:00Z"
        reviewed (replace-record config :review-artifact (fn [x] (assoc x :reviewed-at t)))
        review-pin (get-in reviewed [:roles :review-artifact :expected-sha256])
        executed (replace-record reviewed :review-execution
                                 (fn [x] (assoc x :finished-at t
                                                :review-artifact/raw-sha256 review-pin)))
        originated (replace-record executed :review-origin
                                   (fn [x] (assoc x :artifact/retained-at t :finished-at t
                                                  :review-artifact/raw-sha256 review-pin)))
        accepted (replace-record originated :acceptance
                                 (fn [x] (assoc x :accepted-at t
                                                :review-artifact/raw-sha256 review-pin)))]
    (is (= :join-mechanism-validated (:status (completeness/validate accepted))))
    (store/release! store)))

(defn- rebind-subject [config changes]
  (let [changed (replace-record config :completeness-subject
                 #(update % :capture merge changes))
        subject-pin (get-in changed [:roles :completeness-subject :expected-sha256])
        reviewed (replace-record changed :review-artifact
                   #(assoc % :subject/raw-sha256 subject-pin))
        review-pin (get-in reviewed [:roles :review-artifact :expected-sha256])]
    (reduce (fn [c role]
              (replace-record c role
                #(cond-> (assoc % :subject/raw-sha256 subject-pin)
                   (not= role :review-commission) (assoc :review-artifact/raw-sha256 review-pin))))
            reviewed [:review-commission :review-execution :acceptance])))

(deftest coherent-subject-store-and-generation-refuse
  (let [{:keys [store config]} (fixture)]
    (try
      (doseq [changes [{:store/id "borrowed-store"} {:owner/generation 999}]]
        (is (= :e6b-completeness/authority-join-invalid
               (refusal #(completeness/validate (rebind-subject config changes))))))
      (finally (store/release! store)))))
