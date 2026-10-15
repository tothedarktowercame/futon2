(ns futon2.aif.reading-policy-precision-test
  "Identity invariance/refusal tests for reading-policy-precision: order and
  theta invariance, each structural/model pin change, old-model refusal."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.reading-policy-precision :as rpp]))

(def base-config
  {:scorer-version :cascade-observation-scorer-v1
   :kernel-version :co-application-frontier-theta-v1
   :graph-digest "sha256:graph-abc"
   :readings {"T1" {:analysis-digest "sha256:a1" :excerpt-digest "sha256:e1"}
              "T2" {:analysis-digest "sha256:a2" :excerpt-digest "sha256:e2"}}
   :policies [{:policy-id "P2" :kind :co-application :target-id "T2"
               :theta 0.42 :g 1.5 :habit 1
               :arrangement {:nodes [{:pattern :b :roles [:x]} {:pattern :a :roles [:y]}]
                             :edges [{:from :a :to :b}]}}
              {:policy-id "P1" :kind :co-application :target-id "T1"
               :theta 0.17 :f 2.5
               :arrangement {:nodes [{:pattern :c}]
                             :edges [{:from :c :to :c :kind :overlap}]}}]
   :progress-model {:authority :wm/progress-tokens-v2 :version "2026-10-15"
                    :progress-tokens [:done :blocked]
                    :rates {:done 0.5}}        ;; numeric rates: theta, excluded
   :preference {:authority :wm/horizon-authority :version "2026-10-01" :horizon 12}
   :beta-authority {:status :declared :version "2026-10-15" :beta 3.0}})

;; -- invariance ----------------------------------------------------------------

(deftest theta-and-number-invariance
  (let [thetaed (update-in base-config [:policies 0 :theta] + 0.5)]
    (is (= (rpp/identity-of base-config) (rpp/identity-of thetaed))
        "theta numeric values must not change identity")))

(deftest policy-enumeration-order-invariance
  (let [reordered (update base-config :policies (comp vec rseq))]
    (is (= (rpp/identity-of base-config) (rpp/identity-of reordered))
        "field enumeration order must not change identity")))

(deftest reading-map-order-and-key-alias-invariance
  (let [aliased (-> base-config
                    (assoc :readings {"T2" {:analysis-sha256 "sha256:a2" :excerpt-sha256 "sha256:e2"}
                                      "T1" {:analysis-digest "sha256:a1" :excerpt-digest "sha256:e1"}}))]
    (is (= (rpp/identity-of base-config) (rpp/identity-of aliased))
        "map enumeration order and digest key aliases must not change identity")))

(deftest nested-theta-in-arrangement-excluded
  (let [thetas-inside (assoc-in base-config [:policies 0 :arrangement :nodes 0 :theta] 0.99)]
    (is (= (rpp/identity-of base-config) (rpp/identity-of thetas-inside))
        "numeric learning fields inside arrangement nodes must not affect identity"))
  (let [g-inside (update-in base-config [:policies 0 :arrangement :nodes 1]
                            assoc :g 12.5 :f 0.3)]
    (is (= (rpp/identity-of base-config) (rpp/identity-of g-inside))
        "numeric learning fields nested in node maps must not affect identity")))

(deftest structural-values-in-arrangement-bind
  (is (not= (rpp/identity-of base-config)
            (rpp/identity-of
             (update-in base-config [:policies 0 :arrangement :nodes 0]
                        assoc :fragment-index 7)))
      "a structural numeric index is legitimate structure and must bind")
  (is (not= (rpp/identity-of base-config)
            (rpp/identity-of
             (update-in base-config [:policies 0 :arrangement :nodes 1]
                        assoc :pattern :zz)))
      "pattern identity inside an arrangement binds")
  (is (not= (rpp/identity-of base-config)
            (rpp/identity-of
             (update-in base-config [:policies 0 :arrangement :edges 0]
                        assoc :kind :overlap)))
      "edge kind binds"))

(deftest arrangement-node-order-invariance
  (let [swapped (update-in base-config [:policies 0 :arrangement :nodes] (comp vec rseq))]
    (is (= (rpp/identity-of base-config) (rpp/identity-of swapped))
        "node enumeration order inside an arrangement must not change identity")))

;; -- each structural / model pin change must change identity -------------------

(deftest structural-arrangement-change-changes-identity
  (is (not= (rpp/identity-of base-config)
            (rpp/identity-of
             (update-in base-config [:policies 0 :arrangement :edges] conj {:from :b :to :a})))))

(deftest policy-id-set-change-changes-identity
  (is (not= (rpp/identity-of base-config)
            (rpp/identity-of (update base-config :policies pop)))))

(deftest graph-pin-change-changes-identity
  (is (not= (rpp/identity-of base-config)
            (rpp/identity-of (assoc base-config :graph-digest "sha256:graph-xyz")))))

(deftest scorer-and-kernel-bind-independently
  (is (not= (rpp/identity-of base-config)
            (rpp/identity-of (assoc base-config :scorer-version :cascade-observation-scorer-v2)))
      "changing only the scorer version changes identity")
  (is (not= (rpp/identity-of base-config)
            (rpp/identity-of (assoc base-config :kernel-version :co-application-frontier-theta-v2)))
      "changing only the kernel version changes identity")
  (is (= (rpp/identity-of (assoc base-config :scorer-version :s1 :kernel-version :k1))
         (rpp/identity-of (assoc base-config :scorer-version :s1 :kernel-version :k1)))))

(deftest missing-scorer-or-kernel-refuses
  (is (thrown? clojure.lang.ExceptionInfo
               (rpp/identity-of (dissoc base-config :scorer-version))))
  (is (thrown? clojure.lang.ExceptionInfo
               (rpp/identity-of (dissoc base-config :kernel-version)))))

(deftest reading-digest-change-changes-identity
  (is (not= (rpp/identity-of base-config)
            (rpp/identity-of
             (assoc-in base-config [:readings "T1" :analysis-digest] "sha256:a1-new"))))
  (is (not= (rpp/identity-of base-config)
            (rpp/identity-of
             (assoc-in base-config [:readings "T2" :excerpt-digest] "sha256:e2-new")))))

(deftest progress-model-change-changes-identity
  (is (not= (rpp/identity-of base-config)
            (rpp/identity-of
             (assoc-in base-config [:progress-model :version] "2026-10-16"))))
  (is (not= (rpp/identity-of base-config)
            (rpp/identity-of
             (assoc-in base-config [:progress-model :progress-tokens] [:done :blocked :stalled])))
      "the token domain is structural: adding a progress token changes identity"))

(deftest preference-authority-change-changes-identity
  (is (not= (rpp/identity-of base-config)
            (rpp/identity-of (assoc-in base-config [:preference :version] "2026-10-02"))))
  (is (not= (rpp/identity-of base-config)
            (rpp/identity-of (assoc-in base-config [:preference :horizon] 24)))))

(deftest beta-authority-change-changes-identity
  (is (not= (rpp/identity-of base-config)
            (rpp/identity-of (assoc-in base-config [:beta-authority :status] :learned))))
  ;; the numeric beta itself is theta: excluded
  (is (= (rpp/identity-of base-config)
         (rpp/identity-of (assoc-in base-config [:beta-authority :beta] 99.0)))))

;; -- beta continuity -----------------------------------------------------------

(defn previous-receipt [config beta]
  (rpp/seal {:schema rpp/carry-schema
             :identity (rpp/identity-of config)
             :beta beta}))

(deftest two-call-self-consumable-initialized-then-carried
  (let [first (rpp/carry-beta {:previous nil :initialized-beta 3.5 :config base-config})]
    (is (= :initialized (:status first)))
    (is (rpp/intact? first) "the initialized receipt is sealed and directly consumable")
    (let [second (rpp/carry-beta {:previous first :initialized-beta 3.5 :config base-config})]
      (is (= :carried (:status second)) "call N's output feeds call N+1 without external sealing")
      (is (= 3.5 (:beta second)))
      (is (rpp/intact? second))
      (let [third (rpp/carry-beta {:previous second :initialized-beta 3.5 :config base-config})]
        (is (= :carried (:status third)))
        (is (= (:sha256 second) (:previous-sha256 third)))))))

(deftest tampered-sealed-receipt-still-refuses
  (let [first (rpp/carry-beta {:previous nil :initialized-beta 3.5 :config base-config})
        tampered (assoc first :beta 99.0)]
    (is (= :refused (:status (rpp/carry-beta {:previous tampered :initialized-beta 3.5
                                              :config base-config}))))))

(deftest intact-receipt-with-noncarryable-beta-refuses
  (doseq [bad [nil -1.0 0.0 ##NaN ##Inf]]
    (let [prev (rpp/seal {:schema rpp/carry-schema
                          :identity (rpp/identity-of base-config)
                          :beta bad})
          r (rpp/carry-beta {:previous prev :initialized-beta 3.5 :config base-config})]
      (is (= :refused (:status r)) (str "beta " bad))
      (is (= :previous-beta-not-carryable (:reason r))))))

(deftest refused-receipt-is-also-sealed
  (let [r (rpp/carry-beta {:previous nil :initialized-beta 3.5
                           :config (assoc base-config :graph-digest "sha256:x")})]
    ;; any output shape must be intact, not just successes
    (is (rpp/intact? r))))

(deftest carry-when-identical-identity
  (let [r (rpp/carry-beta {:previous (previous-receipt base-config 4.0)
                           :initialized-beta 3.0 :config base-config})]
    (is (= :carried (:status r)))
    (is (= 4.0 (:beta r)))))

(deftest initialize-when-no-previous
  (let [r (rpp/carry-beta {:previous nil :initialized-beta 3.0 :config base-config})]
    (is (= :initialized (:status r)))
    (is (= 3.0 (:beta r)))))

(deftest refuse-when-identity-changed
  (let [changed (assoc base-config :graph-digest "sha256:graph-new")
        r (rpp/carry-beta {:previous (previous-receipt base-config 4.0)
                           :initialized-beta 3.0 :config changed})]
    (is (= :refused (:status r)))
    (is (= :reading-precision-identity-changed (:reason r)))
    (is (= :redeclare-beta (get-in r [:reinitialization :policy])))))

(deftest refuse-tampered-previous
  (let [prev (dissoc (previous-receipt base-config 4.0) :sha256)
        r (rpp/carry-beta {:previous prev :initialized-beta 3.0 :config base-config})]
    (is (= :refused (:status r)))
    (is (= :previous-receipt-tampered (:reason r)))))

(deftest old-class-q0-identity-is-typed-refusal
  (let [old (rpp/seal {:schema :wm/precision-carry-v1
                       :model-id "old-class-q0-digest"
                       :beta 4.0})
        r (rpp/carry-beta {:previous old :initialized-beta 3.0 :config base-config})]
    (is (= :refused (:status r)))
    (is (= :old-class-q0-model-identity (:reason r)))
    (is (= :wm/precision-carry-v1 (:previous-schema r))))
  (let [old-family (rpp/seal {:schema :wm/precision-family-v1 :model-id "x" :beta 2.0})
        r2 (rpp/carry-beta {:previous old-family :initialized-beta 3.0 :config base-config})]
    (is (= :refused (:status r2)))
    (is (= :old-class-q0-model-identity (:reason r2)))))

(deftest foreign-schema-refused
  (let [foreign (rpp/seal {:schema :wm/something-else-v1 :identity (rpp/identity-of base-config)})
        r (rpp/carry-beta {:previous foreign :initialized-beta 3.0 :config base-config})]
    (is (= :refused (:status r)))
    (is (= :previous-receipt-schema-mismatch (:reason r)))))

(deftest invalid-configs-refuse-with-typed-error
  (is (thrown? clojure.lang.ExceptionInfo
               (rpp/identity-of (dissoc base-config :graph-digest))))
  (is (thrown? clojure.lang.ExceptionInfo
               (rpp/identity-of (assoc base-config :readings {"T1" {}}))))
  (is (thrown? clojure.lang.ExceptionInfo
               (rpp/carry-beta {:previous nil :initialized-beta -1 :config base-config}))))
