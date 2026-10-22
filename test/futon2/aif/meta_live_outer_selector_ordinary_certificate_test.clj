(ns futon2.aif.meta-live-outer-selector-ordinary-certificate-test
  "Trial 5 (one-click-frontier-2026-10-22 ordinary-trial-5) held at
  :wm/outer-learning-not-consumed because the ordinary cascade arm reused the
  injury arm's score-consumption certificate.  These tests cover the
  ordinary-provider consumption certificate: the real frozen receipt shape,
  executable counterexamples from the independent review (missing provider,
  refused evidence, corrupted objective seal, minimal unpinned maps,
  substituted content keeping old pins) all refused POSITIVELY with a real
  sealed carrier, and the outer-learning authority pre-checks the ordinary
  arm previously inherited."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.meta-field-observation :as field]
            [futon2.aif.meta-live-outer-selector :as live]))

(def trial-dir
  "holes/labs/wm-contract/runs/one-click-frontier-2026-10-22/ordinary-trial-5")

(def frozen-stop-gz-sha256
  "360bc93c849687b81be833e398e99040c34f6b85f5a1537bb30dac3da4d7e1e5")

(defonce ^:private frozen-policy
  (delay
   (let [stop (with-open [r (io/reader (java.util.zip.GZIPInputStream.
                                        (io/input-stream (str trial-dir
                                                              "/selection-stop.edn.gz"))))]
                (edn/read-string {:readers {'wm/keyword (fn [[n k]] (keyword n k))}}
                                 (slurp r)))]
     (get-in stop [:stop :ex-data :judgement :outer-task-selection :policy]))))

;; Pins and identities recorded from the frozen Trial 5 receipt.
(def frozen-menu-sha "1e7500a4b377935442c9092f40158bae931137e843064474b518e35cf4db4980")
(def frozen-evidence-sha "b630a2f59f2fad2fb2d39bb85ddd4a680f8623f505e47fe64a8d456c3a60e6a2")
(def frozen-objective-sha "fd56d38d9d8a293a0bbea6108588e2275f002eb636fb0c857af6b65c02add9f6")
(def frozen-carrier-sha "5dd8de3b28b5654bc0d16fe8f85e1fad33676cbee75379ac8dbf7ae41bac1da7")
(def frozen-provider-sha "d9bdfbb7b7833c462797db26c786b7401c25150236fc6453d837f8e79243451b")
(def frozen-policy-id "M-stack-geometry/policy-c7d7518e1e185c2e2318")
(def frozen-cascade-digest "c601dc5697a2d5d65c5c8440f08b3905cee1030b9a62dfa03e2ad5e7ab202c27")

(def re-arm-condition
  "Promote shadow to live only after pinned counterfactual selection tests and a supervised consumption certificate pass.")

(def frozen-outer-learning
  "Reconstructed strictly from retained projections.  :mode and
  :re-arm-condition are verbatim in [:stop :ex-data :outer-learning];
  belief :produced is proven by the frozen refusal itself: the legacy
  certificate reaches :outer-learning-shadow-not-consumed only after its
  belief-unavailable branch failed, so belief was :produced at selection
  time.  Nothing else is invented."
  {:mode :live
   :belief {:status :produced}
   :controller {:re-arm-condition re-arm-condition}})

(defn- frozen-input
  "The certificate input replayable against the retained Trial 5 receipt.
  The provider body is not retained -- only its recorded source pin -- so the
  positive certificate must attest that relation as receipt-recorded, never
  as verified."
  []
  (let [policy @frozen-policy]
    {:outer-learning frozen-outer-learning
     :carrier (:selected-outer-cascade-policy policy)
     :policy-evidence (:policy-evidence policy)
     :policy-objective (:policy-objective policy)
     :provider-source-pin (:evidence-provider-source-pin policy)}))

(deftest ^:slow frozen-trial-five-replay-and-the-exact-seal-gap
  ;; The frozen capture is EDN round-trip EQUAL to the live stop (the capture
  ;; script itself asserted (= record decoded)), and re-encoding the decoded
  ;; record reproduces the frozen text byte-for-byte (verified by probe,
  ;; 2026-10-22).  The objective and evidence pins DO recompute from the
  ;; capture.  The menu-pin and carrier-pin do NOT -- and they also fail
  ;; under recursive sorted canonicalization of sets and maps -- so the live
  ;; pr-str serialization the pins hash over is not reproducible offline for
  ;; the menu/carrier bodies.  The certificate therefore truthfully refuses
  ;; the frozen replay at total seal verification while everything the
  ;; capture CAN evidence (content bindings, recorded pins, provider
  ;; provenance) passes.  Surfaced, not worked around: making pins
  ;; print-order-independent is a structural change to the seal machinery
  ;; and is not ours to make here.
  (let [path (str trial-dir "/selection-stop.edn.gz")
        sha (field/sha256 (with-open [in (io/input-stream path)
                                       out (java.io.ByteArrayOutputStream.)]
                            (io/copy in out)
                            (.toByteArray out)))]
    (is (= frozen-stop-gz-sha256 sha) "frozen stop file drifted")
    (let [policy @frozen-policy
          input (frozen-input)
          cert (live/ordinary-learning-certificate input)]
      (is (= :ordinary-cascade-policy (:branch policy)))
      (is (= :selected (:status policy)))
      (is (= :strict-objective (:basis-kind (:selected-outer-cascade-policy policy))))
      ;; everything the capture can evidence passes:
      (is (= (:policy-evidence policy)
             (:evidence-carrier (:selected-outer-cascade-policy policy))))
      (is (= (:policy-objective policy)
             (dissoc (:objective-receipt (:selected-outer-cascade-policy policy))
                     :objective-pin)))
      ;; ... and total seal verification reports the exact gap:
      (is (= :refused (:status cert)))
      (is (= :ordinary-carrier-invalid (:reason cert)))
      (is (= :selected-policy-seal (:carrier-defect cert))
          "frozen capture is not pin-recomputable: see test comment"))))

(deftest ^:slow review-counterexamples-against-the-real-sealed-carrier-are-refused
  (testing "removing the provider source pin"
    (is (= :ordinary-provider-absent
           (:reason (live/ordinary-learning-certificate
                     (dissoc (frozen-input) :provider-source-pin))))))
  (testing "refused policy evidence"
    (is (= :ordinary-evidence-not-carrier-artifact
           (:reason (live/ordinary-learning-certificate
                     (assoc-in (frozen-input)
                               [:policy-evidence :status] :refused))))))
  (testing "objective seal corrupted to 64 zeros"
    (let [cert (live/ordinary-learning-certificate
                (assoc-in (frozen-input)
                          [:carrier :objective-pin :sha256]
                          (apply str (repeat 64 "0"))))]
      (is (= :refused (:status cert)))
      (is (contains? #{:ordinary-carrier-invalid :ordinary-objective-not-carrier-artifact}
                     (:reason cert)))))
  (testing "minimal unpinned status-only maps"
    (let [cert (live/ordinary-learning-certificate
                {:outer-learning frozen-outer-learning
                 :carrier {:status :sealed :objective-receipt {:status :selected}}
                 :policy-objective {:status :selected}})]
      (is (= :refused (:status cert)))
      (is (contains? #{:ordinary-evidence-not-carrier-artifact
                       :ordinary-carrier-invalid}
                     (:reason cert)))))
  (testing "substituted content keeping the old pins"
    ;; Substitute the supplied objective with different content that still
    ;; carries the carrier's menu pin: content binding must reject it.
    (let [cert (live/ordinary-learning-certificate
                (assoc (frozen-input) :policy-objective
                       {:status :selected
                        :menu-pin {:path "wm://sealed-outer-policy-menu-v1"
                                   :sha256 frozen-menu-sha}}))]
      (is (= :refused (:status cert)))
      (is (= :ordinary-objective-not-carrier-artifact (:reason cert)))))
  (testing "changed objective receipt bound to the carrier"
    (let [cert (live/ordinary-learning-certificate
                (assoc-in (frozen-input)
                          [:policy-objective :draw :u] 0.42))]
      (is (= :ordinary-objective-not-carrier-artifact (:reason cert)))))
  (testing "provider body with a declaration pin that is not the consumed evidence's"
    ;; Self-consistent provider body (its source-pin really covers its body)
    ;; whose declaration pin is NOT the consumed evidence's declaration pin.
    (let [body {:schema :wm/outer-policy-evidence-provider-v1
                :status :provided :authority :test
                :declaration {:pin {:sha256 (apply str (repeat 64 "7"))}}
                :records []}
          provided (assoc body :source-pin
                          {:path "wm://outer-policy-evidence-provider-v1"
                           :sha256 (load-identity/sha256
                                    (.getBytes (pr-str body) "UTF-8"))})
          cert (live/ordinary-learning-certificate
                (assoc (frozen-input) :provided provided))]
      (is (= :refused (:status cert)))
      (is (= :ordinary-provider-declaration-mismatch (:reason cert)))))
  (testing "malformed provider body"
    (let [cert (live/ordinary-learning-certificate
                (assoc (frozen-input)
                       :provided {:schema :wm/other :status :provided}))]
      (is (= :ordinary-provider-invalid (:reason cert)))))
  (testing "invalid recorded provider pin shape"
    (let [cert (live/ordinary-learning-certificate
                (assoc (frozen-input)
                       :provider-source-pin {:path "x" :sha256 "zz"}))]
      (is (= :ordinary-provider-pin-invalid (:reason cert))))))

(deftest ^:slow every-refusal-is-positive-never-an-abstention
  (doseq [cert (map live/ordinary-learning-certificate
                    [(dissoc (frozen-input) :provider-source-pin)
                     (assoc-in (frozen-input) [:policy-evidence :status] :refused)
                     (assoc-in (frozen-input)
                               [:carrier :objective-pin :sha256]
                               (apply str (repeat 64 "0")))
                     {:outer-learning frozen-outer-learning
                      :carrier {:status :sealed}
                      :policy-objective {:status :selected}}
                     (assoc (frozen-input) :carrier nil)
                     (assoc (frozen-input)
                            :provider-source-pin {:path "x" :sha256 "zz"})])]
    (is (= :refused (:status cert)))
    (is (keyword? (:reason cert)))))

(def snapshot
  {:schema :wm/pipeline-cascade-snapshot-v1
   :summary-source {:path "summary" :sha256 (apply str (repeat 64 "a"))}
   :graph-source {:path "graph" :sha256 (apply str (repeat 64 "b"))}
   :summary {:consistent? true
             :standards {:s1-regenerates true :s2-evidence true
                         :s3-reconstitution true :s4-honest-holes true
                         :s5-composed true}}
   :graph {:section-status {:lineage {:status :ok} :clusters {:status :ok}
                            :arrows {:status :ok} :held {:status :ok}
                            :patterns {:status :ok}}
           :lineage [] :clusters [{:mission "M-a"}] :arrows [] :held []
           :tickets {:items [{:stem "M-a" :mtime-ms 200}]}
           :patterns {:edges []}}})

(def tasks
  [{:id "M-a" :kind :mission :priority 2
    :source {:path "M-a.md" :sha256 (apply str (repeat 64 "c"))}}])

(defn- complete-policy-field [task-rows _]
  {:schema :wm/pre-choice-policy-field-v1
   :source-pin {:path "wm://test-policy-menu" :sha256 (apply str (repeat 64 "e"))}
   :coverage {:targets (count task-rows) :current (count task-rows)}
   :families (mapv (fn [{:keys [id]}]
                     {:target-id id :status :computed
                      :policies [{:policy-id (str id "/policy-1")}]
                      :failures []})
                   task-rows)})

(deftest ordinary-arm-without-a-sealed-carrier-refuses-positively
  ;; Wiring: the live selector calls the ordinary certificate; without a
  ;; sealed carrier (menu stopped at the provider boundary) it must refuse
  ;; positively with the typed missing-carrier reason, not abstain.
  (let [selection-as-of "2026-10-22T12:34:56Z"
        receipt (live/select-live
                 {:tasks tasks
                  :fetch-snapshot (constantly snapshot)
                  :selection-as-of selection-as-of
                  :focus-inputs {}
                  :focus-context {:as-of selection-as-of :status :retained
                                  :focus "WM"}
                  :focus-classify-fn (fn [_ _ _ target _]
                                       {:target target :class :focus})
                  :policy-field-config {:authority :test-fixture}
                  :policy-field-fn complete-policy-field
                  :policy-menu-fn
                  (fn [field _]
                    {:schema :wm/sealed-outer-policy-menu-v1
                     :status :sealed
                     :field-source-pin (:source-pin field)
                     :menu-pin {:path "wm://test-sealed-menu"
                                :sha256 (apply str (repeat 64 "f"))}
                     :entries [{:policy-id "fixture/policy"
                                :cascade-digest (apply str (repeat 64 "1"))}]})
                  :outer-learning {:mode :shadow :belief {:status :produced}
                                   :controller {:re-arm-condition "restore"}}})]
    (is (= :refused (get-in receipt [:outer-learning :status])))
    (is (= :ordinary-selected-policy-carrier-missing
           (get-in receipt [:outer-learning :reason])))))

(deftest outer-learning-authority-prechecks-are-preserved
  ;; Pre-checks precede every consumption check, so a trivial base suffices.
  (let [base {:carrier nil :policy-evidence nil :policy-objective nil}]
    (is (= :absent (:status (live/ordinary-learning-certificate
                             (assoc base :outer-learning nil)))))
    (is (= :refused (:status (live/ordinary-learning-certificate
                              (assoc base :outer-learning
                                     (assoc frozen-outer-learning :mode :off))))))
    (is (= :outer-learning-state-unavailable
           (:reason (live/ordinary-learning-certificate
                     (assoc base :outer-learning
                            (assoc-in frozen-outer-learning
                                      [:belief :status] :refused))))))))
