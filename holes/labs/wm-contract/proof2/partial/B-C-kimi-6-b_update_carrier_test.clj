(ns futon2.aif.b-update-carrier-test
  "B-C (PROOF-2 strategy row 34): the concentration carrier's builder, its
   X5 falsifiers, and the b-update wiring. Spec: proof2/packets/B-D.md §4."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.learning-trial-ledger :as ledger])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def live-ledger-root "/home/joe/code/futon2/data/wm-learning-trials")

(defn- temp-root []
  (.getPath (.toFile (Files/createTempDirectory "b-carrier-test-" (make-array FileAttribute 0)))))

(defn- no-doubles?
  "Every number in the value is an exact rational (ratio or integer)."
  [v]
  (cond
    (number? v) (or (ratio? v) (integer? v))
    (map? v) (every? (fn [[k x]] (and (no-doubles? k) (no-doubles? x))) v)
    (coll? v) (every? no-doubles? v)
    :else true))

(deftest carrier-pinned-from-a-live-ledger-row
  ;; Pinned from the live row :identity
  ;; "8e7d1aaf32d9ef2ead1702b304887c414298ce7ea5b48b0e51e42995963e1403"
  ;; (:theta-key :apparatus/done-is-observed-running, :observed true),
  ;; read verbatim from data/wm-learning-trials/attempts.edn on 2026-09-24
  ;; (12 rows; this family has exactly this one contributing row).
  (let [rows (ledger/read-trials live-ledger-root)
        row (first (filter #(= "8e7d1aaf32d9ef2ead1702b304887c414298ce7ea5b48b0e51e42995963e1403"
                               (:identity %)) rows))
        family (:theta-key row)
        mine (filter #(= family (:theta-key %)) rows)
        carrier (ledger/concentration-carrier family mine {:layer :none})]
    (is (some? row) "the pinned live row is present")
    (is (= 1 (count mine)) "one contributing row for this family today")
    (is (= :wm/b-update-carrier-v1 (:schema carrier)))
    (is (= {:achieved 1/2 :not 1/2} (:prior carrier)) "Jeffreys prior")
    (is (= {:achieved 3/2 :not 1/2} (:posterior carrier))
        "one achieved trial on the Jeffreys prior")
    (is (= [{:identity "8e7d1aaf32d9ef2ead1702b304887c414298ce7ea5b48b0e51e42995963e1403"
             :theta-key :apparatus/done-is-observed-running :cell :achieved}]
           (:trial-identities carrier)))
    (is (= {:conc-achieved 3/2 :conc-not 1/2 :theta 3/4} (:normalization carrier))
        ":normalization present, so a bare 3/4 has something to fail against")
    (is (= (:theta (:normalization carrier))
           (:theta (ledger/pattern-theta family live-ledger-root)))
        "the carrier's normalized cell equals the judge's read for this family")
    (is (re-matches #"[0-9a-f]{64}" (:version carrier)))
    (is (no-doubles? carrier) "exact rationals only, no doubles in the carrier")
    (is (nil? (ledger/carrier-refusal carrier)))))

(deftest duplicate-identity-changes-nothing-at-any-dedup-layer
  ;; B-D §5, second falsifier: a duplicate trial that changes
  ;; concentrations. The deliberately wrong builder below ignores
  ;; deduplication; against it the duplicate MOVES conc(achieved), which is
  ;; what makes this test fail if the real boundary erodes.
  (let [row {:identity "dup-1" :theta-key :family/f :observed true}
        wrong-builder (fn [rows]
                        {:achieved (+ 1/2 (count (filter :observed rows)))})
        single (ledger/concentration-carrier :family/f [row] {:layer :none})
        doubled (ledger/concentration-carrier :family/f [row row] {:layer :none})]
    (testing "the wrong builder moves the concentration"
      (is (not= (:achieved (wrong-builder [row])) (:achieved (wrong-builder [row row])))))
    (testing "read-collapse layer: duplicate identity in the input counts once"
      (is (= single doubled))
      (is (= 3/2 (get-in doubled [:posterior :achieved]))))
    (testing "ledger-identity layer: record! holds the replay"
      (let [root (temp-root)
            trial {:status :admitted-at-attempt-grain
                   :deduplication {:identity "dup-ledger-1"}
                   :learning-family "cfg-digest-1"
                   :meaning-sha256 "m1"
                   :after-observation true}
            receipt {:contract {} :trials [trial]}]
        (ledger/record! root receipt)
        (let [replay (ledger/record! root receipt)
              status (get-in replay [:trials 0 :ledger :status])
              reason (get-in replay [:trials 0 :reason])
              rows (ledger/read-trials root)]
          (is (= :not-appended status))
          (is (= :duplicate-replay reason))
          (is (= 1 (count rows)) "one banked row, not two")
          (is (= (select-keys single [:prior :posterior :normalization])
                 (select-keys
                  (ledger/concentration-carrier
                   :family/f (mapv #(assoc % :theta-key :family/f) rows)
                   {:layer :none})
                  [:prior :posterior :normalization]))
              "concentrations unchanged by the held replay"))))
    (testing "update-occurrence layer: b-update reports :already-recorded"
      (let [root (temp-root)
            occurrence {:attempt "a-0"}
            trial-row {:identity "occ-row-1" :theta-key :family/f :observed true
                       :row {:trial {:deduplication {:inputs {:occurrence occurrence}}}}}]
        ;; Bank the row the way record! would have shaped it for read-trials.
        (io/make-parents (io/file root "attempts.edn"))
        (spit (io/file root "attempts.edn")
              (pr-str {:schema :wm/attempt-learning-count-v1 :mode :record-only
                       :identity "occ-row-1" :family "cfg" :meaning-sha256 "m"
                       :observed true :increment {:success 1 :failure 0}
                       :trial {:selected-cascade {:precedence [{:id :family/f
                                                                :produces #{["T" :e]}]}]
                               :effect ["T" :e]
                               :deduplication {:inputs {:occurrence occurrence}}}}))
        (let [before (ledger/concentration-carrier :family/f (ledger/read-trials root) {:layer :none})
              result (ledger/b-update {:family :family/f
                                       :occurrence occurrence
                                       :occurrence-identity "commit-sha"
                                       :accepted-verdict {:accepted? true}
                                       :ledger-root root})]
          (is (= :already-recorded (:status result)))
          (is (= {:layer :update-occurrence :status :already-recorded}
                 (get-in result [:carrier :dedup]))
              "the carrier names the layer that fired")
          (is (= (:posterior before) (get-in result [:carrier :posterior]))
              "conc(achieved) unchanged by the duplicate occurrence")
          (is (= (:theta (:normalization (:carrier result)))
                 (:theta (ledger/pattern-theta :family/f root)))))))))

(deftest a-token-posterior-and-a-bare-scalar-are-refused
  ;; B-D §5, first falsifier: a whole-attempt success asserted as a token
  ;; posterior without a mapping. The builder has no s-tau parameter at
  ;; all; a carrier claiming one is refused, and a bare scalar -- even the
  ;; numerically right one -- fails X5.
  (let [invented (assoc-in (ledger/concentration-carrier
                            :family/f [{:identity "r1" :theta-key :family/f :observed true}]
                            {:layer :none})
                           [:trial-identities 0 :token-belief]
                           {["T" :restoration-accepted] 1})]
    (is (= :token-posterior-not-a-trial (ledger/carrier-refusal invented))))
  (is (= :scalar-without-concentrations
         (ledger/carrier-refusal {:family :family/f :theta 3/4}))
      "3/4 with no concentration array and normalization fails")
  (is (= :inexact-number
         (ledger/carrier-refusal
          (assoc-in (ledger/concentration-carrier
                     :family/f [{:identity "r1" :theta-key :family/f :observed true}]
                     {:layer :none})
                    [:normalization :theta] 0.75)))
      "a double anywhere in the carrier fails the exact-rational rule"))

(deftest b-update-carries-the-carrier-of-the-rows-it-consumed
  (let [root (temp-root)
        _ (io/make-parents (io/file root "attempts.edn"))
        _ (spit (io/file root "attempts.edn")
                (pr-str {:schema :wm/attempt-learning-count-v1 :mode :record-only
                         :identity "consumed-1" :family "cfg" :meaning-sha256 "m"
                         :observed false :increment {:success 0 :failure 1}
                         :trial {:selected-cascade {:precedence [{:id :family/f
                                                                  :produces #{["T" :e]}]}]
                                 :effect ["T" :e]
                                 :deduplication {:inputs {:occurrence {:attempt "a-0"}}}}}))
        result (ledger/b-update {:family :family/f
                                 :occurrence {:attempt "a-1"}
                                 :occurrence-identity "commit-sha"
                                 :accepted-verdict {:accepted? true :observed true}
                                 :ledger-root root})
        carrier (:carrier result)]
    (is (= :updated (:status result)))
    (is (some? carrier) "b-update emits the carrier")
    (is (= {:layer :none} (:dedup carrier)))
    (is (nil? (ledger/carrier-refusal carrier)))
    ;; The occurrence {:attempt "a-1"} is not yet banked, so b-update's
    ;; stated :theta includes it ((1+1/2)/(1+1+1) = 1/2) while the carrier
    ;; is built from the ledger rows alone -- the rows the judge reads --
    ;; giving (0+1/2)/(1+1) = 1/4. The carrier's normalized cell is the
    ;; judge's read; b-update's :theta is the post-recording value.
    (is (= 1/2 (:theta result)))
    (is (= 1/4 (:theta (:normalization carrier))))
    (is (= (:theta (:normalization carrier))
           (:theta (ledger/pattern-theta :family/f root)))
        "the carrier's normalized cell equals the judge's read over the same rows")))
