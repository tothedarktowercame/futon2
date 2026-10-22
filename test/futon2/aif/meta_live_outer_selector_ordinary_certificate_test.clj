(ns futon2.aif.meta-live-outer-selector-ordinary-certificate-test
  "Trial 5 (one-click-frontier-2026-10-22 ordinary-trial-5) held at
  :wm/outer-learning-not-consumed because the ordinary cascade arm reused the
  injury arm's score-consumption certificate.  These tests cover the
  ordinary-provider consumption certificate: the real frozen receipt shape,
  positive refusal on missing/mismatched pinned consumption, and the
  outer-learning authority pre-checks the ordinary arm previously inherited."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.meta-field-observation :as field]
            [futon2.aif.meta-live-outer-selector :as live]))

(def trial-dir
  "holes/labs/wm-contract/runs/one-click-frontier-2026-10-22/ordinary-trial-5")

(def frozen-stop-gz-sha256
  "360bc93c849687b81be833e398e99040c34f6b85f5a1537bb30dac3da4d7e1e5")

(defn- frozen-stop []
  (with-open [r (io/reader (java.util.zip.GZIPInputStream.
                            (io/input-stream (str trial-dir
                                                  "/selection-stop.edn.gz"))))]
    (edn/read-string {:readers {'wm/keyword (fn [[n k]] (keyword n k))}}
                     (slurp r))))

(defn- pin [sha] {:path "wm://fixture" :sha256 sha})

;; Pins and identities recorded from the frozen Trial 5 receipt
;; ([:stop :ex-data :judgement :outer-task-selection :policy]).
(def frozen-menu-sha "1e7500a4b377935442c9092f40158bae931137e843064474b518e35cf4db4980")
(def frozen-evidence-sha "b630a2f59f2fad2fb2d39bb85ddd4a680f8623f505e47fe64a8d456c3a60e6a2")
(def frozen-objective-sha "fd56d38d9d8a293a0bbea6108588e2275f002eb636fb0c857af6b65c02add9f6")
(def frozen-carrier-sha "5dd8de3b28b5654bc0d16fe8f85e1fad33676cbee75379ac8dbf7ae41bac1da7")
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

(defn- coherent-consumption []
  {:selected-carrier
   {:schema :wm/selected-outer-cascade-policy-v1 :status :sealed
    :policy-id frozen-policy-id :target-id "M-stack-geometry"
    :basis-kind :strict-objective
    :carrier-pin (pin frozen-carrier-sha)
    :menu-pin {:path "wm://sealed-outer-policy-menu-v1" :sha256 frozen-menu-sha}
    :evidence-pin {:path "wm://outer-policy-evidence-v1" :sha256 frozen-evidence-sha}
    :objective-pin {:path "wm://outer-policy-objective-v1" :sha256 frozen-objective-sha}
    :cascade-digest frozen-cascade-digest
    :objective-receipt {:status :selected
                        :selected {:policy-id frozen-policy-id
                                   :target-id "M-stack-geometry"}}}
   :policy-evidence
   {:status :computed
    :menu-pin {:path "wm://sealed-outer-policy-menu-v1" :sha256 frozen-menu-sha}
    :evidence-pin {:path "wm://outer-policy-evidence-v1" :sha256 frozen-evidence-sha}}
   :policy-objective
   {:status :selected
    :menu-pin {:path "wm://sealed-outer-policy-menu-v1" :sha256 frozen-menu-sha}
    :selected {:policy-id frozen-policy-id :target-id "M-stack-geometry"}}
   :menu-pin {:path "wm://sealed-outer-policy-menu-v1" :sha256 frozen-menu-sha}
   :provider-source-pin
   {:path "wm://outer-policy-evidence-provider-v1"
    :sha256 "d9bdfbb7b7833c462797db26c786b7401c25150236fc6453d837f8e79243451b"}})

(deftest coherent-pinned-consumption-is-certified
  (let [cert (live/ordinary-learning-certificate
              (assoc (coherent-consumption) :outer-learning frozen-outer-learning))]
    (is (= :ordinary-provider-consumed (:status cert)))
    (is (= :live (:mode cert)))
    (is (= [:outer-policy-evidence-provider :policy-evidence
            :policy-objective :selected-outer-cascade-policy]
           (:consumption-path cert)))
    (is (= frozen-menu-sha (get-in cert [:menu-pin :sha256])))
    (is (= frozen-evidence-sha (get-in cert [:evidence-pin :sha256])))
    (is (= frozen-objective-sha (get-in cert [:objective-pin :sha256])))
    (is (= frozen-carrier-sha (get-in cert [:carrier-pin :sha256])))
    (is (= {:policy-id frozen-policy-id :target-id "M-stack-geometry"
            :basis-kind :strict-objective :cascade-digest frozen-cascade-digest}
           (:selected-policy cert)))
    (is (= re-arm-condition (:re-arm-condition cert)))))

(deftest missing-consumption-refuses-positively
  (let [cert (live/ordinary-learning-certificate
              (assoc (coherent-consumption)
                     :outer-learning frozen-outer-learning
                     :selected-carrier nil))]
    (is (= :refused (:status cert)))
    (is (= :ordinary-selected-policy-carrier-missing (:reason cert)))))

(deftest mismatched-consumption-refuses-positively
  (doseq [[reason tweak]
          [[:ordinary-carrier-unsealed
            #(assoc-in % [:selected-carrier :status] :unsealed)]
           [:ordinary-menu-pin-mismatch
            #(assoc-in % [:selected-carrier :menu-pin :sha256] "deadbeef")]
           [:ordinary-evidence-pin-mismatch
            #(assoc-in % [:policy-evidence :evidence-pin :sha256] "deadbeef")]
           [:ordinary-objective-menu-pin-mismatch
            #(assoc-in % [:policy-objective :menu-pin :sha256] "deadbeef")]
           [:ordinary-policy-id-mismatch
            #(assoc-in % [:policy-objective :selected :policy-id] "other/policy")]
           [:ordinary-target-id-mismatch
            #(assoc-in % [:policy-objective :selected :target-id] "M-other")]]]
    (testing reason
      (let [cert (live/ordinary-learning-certificate
                  (assoc (tweak (coherent-consumption))
                         :outer-learning frozen-outer-learning))]
        (is (= :refused (:status cert)))
        (is (= reason (:reason cert)))))))

(deftest outer-learning-authority-prechecks-are-preserved
  (let [base (coherent-consumption)]
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

(deftest ^:slow frozen-trial-five-receipt-shape-is-certified
  (let [path (str trial-dir "/selection-stop.edn.gz")
        sha (field/sha256 (with-open [in (io/input-stream path)
                                       out (java.io.ByteArrayOutputStream.)]
                            (io/copy in out)
                            (.toByteArray out)))]
    (is (= frozen-stop-gz-sha256 sha) "frozen stop file drifted")
    (let [policy (get-in (frozen-stop)
                         [:stop :ex-data :judgement :outer-task-selection :policy])
          cert (live/ordinary-learning-certificate
                {:outer-learning frozen-outer-learning
                 :selected-carrier (:selected-outer-cascade-policy policy)
                 :policy-evidence (:policy-evidence policy)
                 :policy-objective (:policy-objective policy)
                 :menu-pin (get-in policy [:sealed-menu :menu-pin])
                 :provider-source-pin (:evidence-provider-source-pin policy)})]
      (is (= :ordinary-cascade-policy (:branch policy)))
      (is (= :selected (:status policy)))
      (is (= :ordinary-provider-consumed (:status cert)))
      (is (= :live (:mode cert)))
      (is (= frozen-menu-sha (get-in cert [:menu-pin :sha256])))
      (is (= frozen-evidence-sha (get-in cert [:evidence-pin :sha256])))
      (is (= frozen-objective-sha (get-in cert [:objective-pin :sha256])))
      (is (= frozen-carrier-sha (get-in cert [:carrier-pin :sha256])))
      (is (= {:policy-id frozen-policy-id :target-id "M-stack-geometry"
              :basis-kind :strict-objective :cascade-digest frozen-cascade-digest}
             (:selected-policy cert)))
      (is (= (:policy-id (:selected-outer-cascade-policy policy))
             (get-in cert [:selected-policy :policy-id]))
          "the selected policy is reported, never altered"))))
