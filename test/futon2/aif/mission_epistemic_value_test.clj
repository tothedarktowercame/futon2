(ns futon2.aif.mission-epistemic-value-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.mission-epistemic-value :as mev])
  (:import (java.time LocalDate)))

(def as-of (LocalDate/parse "2026-09-03"))

(defn- hx
  [id phase mtime cross-refs]
  {:hx/endpoints [(str "repo-d/mission/" id)]
   :hx/props (cond-> {:mission/id id :mission/mtime mtime
                      :mission/cross-refs cross-refs}
               phase (assoc :mission/phase phase))})

(deftest an-open-binary-latent-is-worth-exactly-one-bit
  (testing "the kernel, not a proxy, produces the number"
    (is (< (Math/abs (- (Math/log 2.0) (mev/latent-eig 0.5))) 1.0e-12))
    (is (zero? (mev/latent-eig 1.0)))
    (is (zero? (mev/latent-eig 0.0)))
    (is (< (mev/latent-eig 0.1) (mev/latent-eig 0.3)))))

(deftest the-settled-model-is-bayes-coherent-and-not-a-special-case
  (testing "a settled latent goes through the same kernel as an open one"
    (let [settled (mev/binary-latent-model 1.0)]
      (is (= {:no-news 1.0} (:predicted-observations settled)))
      (is (= (:prior settled) (get-in settled [:posteriors :no-news])))))
  (testing "an open latent's posteriors reconstruct its prior"
    (let [open (mev/binary-latent-model 0.25)]
      (is (= {:true 0.25 :false 0.75} (:prior open)))
      (is (= {:workable 0.25 :not-workable 0.75}
             (:predicted-observations open))))))

(deftest the-workable-partition-is-read-off-the-doability-prior
  (is (= #{"derive" "argue" "verify" "instantiate"} mev/workable-phases))
  (is (every? #(>= (double (get mev/phase-doability-prior %))
                   mev/workable-threshold)
              mev/workable-phases))
  (testing "unknown is excluded from the availability table on purpose"
    (is (contains? mev/phase-doability-prior "unknown"))
    (is (not (contains? mev/phase-survey-availability "unknown")))))

(deftest an-unreadable-neighbour-phase-is-the-only-thing-that-earns-nats
  (let [{:keys [missions]}
        (mev/readings-from-hyperedges
         [(hx "surveyor" "map" "2026-09-01" ["M-known" "M-dark"])
          (hx "known" "verify" "2026-09-01" [])
          (hx "dark" nil "2026-09-01" [])]
         as-of)
        result (mev/epistemic-of (get missions "surveyor"))]
    (is (= :measured (:status result)))
    (is (= 3 (:question-count result)) "two neighbours plus the freshness bit")
    (is (= 1 (:open-question-count result)) "only M-dark is open")
    (is (< (Math/abs (- (Math/log 2.0) (:nats result))) 1.0e-12))
    (is (= 1.0 (:availability result)))
    (is (= 10 mev/reference-open-questions))
    (is (false? (:clamped? result)))
    (is (< (Math/abs (- 0.1 (:epistemic result))) 1.0e-12)
        "one open question out of a declared reference of ten")
    (testing "each question names what it read"
      (is (= #{{:reading :mission-doc-phase :phase "verify"}
               {:reading :absent :reason :neighbour-phase-unreadable}}
             (set (keep (fn [q]
                          (when (vector? (:question q))
                            (when (= :neighbour-workable (first (:question q)))
                              (:basis q))))
                        (:questions result))))))))

(deftest a-fully-read-neighbourhood-scores-zero-on-its-phase-alone
  (let [{:keys [missions]}
        (mev/readings-from-hyperedges
         [(hx "surveyor" "map" "2026-09-01" ["M-known"])
          (hx "known" "verify" "2026-09-01" [])]
         as-of)
        result (mev/epistemic-of (get missions "surveyor"))]
    (is (= :measured (:status result)))
    (is (zero? (:nats result)))
    (is (zero? (:epistemic result))
        "a MAP phase cannot win on the fiat table when nothing is unknown")))

(deftest staleness-is-a-question-and-the-window-is-declared
  (let [stale (mev/readings-from-hyperedges
               [(hx "old" "map" "2026-01-01" [])] as-of)
        fresh (mev/readings-from-hyperedges
               [(hx "new" "map" "2026-09-01" [])] as-of)
        unreadable (mev/readings-from-hyperedges
                    [(hx "odd" "map" "last week" [])] as-of)]
    (is (= :stale (get-in (mev/epistemic-of (get-in stale [:missions "old"]))
                          [:questions 0 :basis :freshness])))
    (is (= :fresh (get-in (mev/epistemic-of (get-in fresh [:missions "new"]))
                          [:questions 0 :basis :freshness])))
    (is (= :unparseable
           (get-in (mev/epistemic-of (get-in unreadable [:missions "odd"]))
                   [:questions 0 :basis :freshness])))
    (is (pos? (:nats (mev/epistemic-of (get-in stale [:missions "old"])))))
    (is (zero? (:nats (mev/epistemic-of (get-in fresh [:missions "new"])))))
    (is (= 14 mev/freshness-window-days))))

(deftest the-term-tracks-the-amount-of-information-not-its-density
  (testing "a wide half-unread neighbourhood beats a thin fully-unread one"
    (let [dark (for [i (range 12)]
                 (hx (str "dark-" i) nil "2026-09-01" []))
          wide (mev/readings-from-hyperedges
                (cons (hx "wide" "map" "2026-09-01"
                          (mapv #(str "M-dark-" %) (range 9)))
                      dark)
                as-of)
          thin (mev/readings-from-hyperedges
                [(hx "thin" "map" "2026-01-01" [])] as-of)
          wide-r (mev/epistemic-of (get-in wide [:missions "wide"]))
          thin-r (mev/epistemic-of (get-in thin [:missions "thin"]))]
      (is (= 9 (:open-question-count wide-r)))
      (is (= 1 (:open-question-count thin-r)))
      (is (> (:nats wide-r) (:nats thin-r)))
      (is (> (:epistemic wide-r) (:epistemic thin-r))
          "under the pre-U22 density form the thin mission scored 1.0 and won")
      (is (= 1.0 (/ (:nats thin-r) (* (:open-question-count thin-r) mev/ln-2)))
          "the density form would have paid the thin mission the maximum")))
  (testing "the clamp fires only past the declared reference"
    (let [dark (for [i (range 14)] (hx (str "d-" i) nil "2026-09-01" []))
          over (mev/readings-from-hyperedges
                (cons (hx "over" "map" "2026-01-01"
                          (mapv #(str "M-d-" %) (range 14)))
                      dark)
                as-of)
          r (mev/epistemic-of (get-in over [:missions "over"]))]
      (is (= 15 (:open-question-count r)))
      (is (true? (:clamped? r)))
      (is (= 1.0 (:normalized r))))))

(deftest availability-scales-the-same-measured-nats-by-phase
  (let [for-phase (fn [phase]
                    (-> (mev/readings-from-hyperedges
                         [(hx "m" phase "2026-01-01" ["M-dark"])
                          (hx "dark" nil "2026-09-01" [])]
                         as-of)
                        (get-in [:missions "m"])
                        mev/epistemic-of))
        map-phase (for-phase "map")
        verify-phase (for-phase "verify")]
    (is (= (:nats map-phase) (:nats verify-phase))
        "the measured half does not depend on the phase")
    (is (pos? (:epistemic map-phase)))
    (is (zero? (:epistemic verify-phase))
        "a VERIFY-phase mission's advance is declared to survey nothing")))

(deftest a-cross-reference-that-resolves-to-nothing-is-not-a-question
  (let [{:keys [missions]}
        (mev/readings-from-hyperedges
         [(hx "surveyor" "map" "2026-09-01"
              ["M-dark" "M-1" "M-2" "M-foo" "M-trip-report"])
          (hx "dark" nil "2026-09-01" [])]
         as-of)
        result (mev/epistemic-of (get missions "surveyor"))]
    (is (= 2 (:question-count result))
        "one resolvable neighbour plus the freshness bit")
    (is (= 1 (:open-question-count result)))
    (is (= ["1" "2" "foo" "trip-report"]
           (:unresolvable-cross-references result)))
    (is (= 4 (:unresolvable-cross-reference-count result)))
    (is (< (Math/abs (- (Math/log 2.0) (:nats result))) 1.0e-12)
        "the four unresolvable references buy no nats at all")))

(deftest an-unreadable-own-phase-is-typed-and-not-a-zero
  (let [{:keys [missions census]}
        (mev/readings-from-hyperedges
         [(hx "nophase" nil "2026-09-01" ["M-dark"])
          (hx "unknownphase" "unknown" "2026-09-01" ["M-dark"])
          {:hx/endpoints ["repo-d/mission/unnamed"] :hx/props {}}]
         as-of)]
    (is (= :phase-unreadable
           (:status (mev/epistemic-of (get missions "nophase")))))
    (is (= :phase-unreadable
           (:status (mev/epistemic-of (get missions "unknownphase"))))
        "the doability table's 0.3 for unknown has no counterpart here")
    (is (zero? (:epistemic (mev/epistemic-of (get missions "nophase")))))
    (testing "the unnamed hyperedge is counted, not guessed at"
      (is (= {:hyperedges 3 :named 2 :unnamed 1 :phase-readable 0} census)))))

(deftest both-prop-key-shapes-are-read
  (testing "the mission-doc family serves keyword AND string prop keys"
    (let [{:keys [missions census]}
          (mev/readings-from-hyperedges
           [{:hx/props {:mission/id "kw" :mission/phase "map"
                        :mission/mtime "2026-09-01"
                        :mission/cross-refs ["M-str"]}}
            {:hx/props {"mission/id" "str" "mission/phase" "verify"
                        "mission/mtime" "2026-09-01"
                        "mission/cross-refs" ["M-kw"]}}]
           as-of)]
      (is (= #{"kw" "str"} (set (keys missions))))
      (is (= {:hyperedges 2 :named 2 :unnamed 0 :phase-readable 2} census))
      (is (zero? (:nats (mev/epistemic-of (get missions "kw"))))
          "the string-keyed neighbour's phase is readable, so nothing is open"))))

(deftest the-term-is-off-unless-a-declared-weight-turns-it-on
  (is (nil? (mev/field-readings {:epistemic 0.0} {})))
  (is (nil? (mev/field-readings {} {})))
  (is (nil? (mev/record-for nil "M-anything" "map")))
  (testing "switched on, the read goes through the injectable substrate seam"
    (let [readings (mev/field-readings
                    {:epistemic 0.2}
                    {:epistemic-as-of as-of
                     :hyperedges-by-type-fn
                     (fn [t]
                       (is (= "code/v05/mission-doc" t))
                       [(hx "surveyor" "map" "2026-01-01" ["M-dark"])
                        (hx "dark" nil "2026-09-01" [])])})
          record (mev/record-for readings "M-surveyor" nil)]
      (is (pos? (:epistemic record)))
      (is (= :measured (get-in record [:epistemic-basis :status])))
      (is (= :mission-doc-prop
             (get-in record [:epistemic-basis :phase-carrier]))))))

(deftest the-two-phase-carriers-are-compared-not-averaged
  (let [readings (mev/field-readings
                  {:epistemic 0.2}
                  {:epistemic-as-of as-of
                   :hyperedges-by-type-fn
                   (fn [_] [(hx "m" "map" "2026-09-01" [])])})]
    (is (= :agree (get-in (mev/record-for readings "M-m" "map")
                          [:epistemic-basis :phase-agreement])))
    (is (= :disagree (get-in (mev/record-for readings "M-m" "instantiate")
                             [:epistemic-basis :phase-agreement])))
    (is (= :doability-phase-absent
           (get-in (mev/record-for readings "M-m" nil)
                   [:epistemic-basis :phase-agreement])))
    (is (= :mission-absent-from-mission-doc-index
           (get-in (mev/record-for readings "M-absent" "map")
                   [:epistemic-basis :status])))))
