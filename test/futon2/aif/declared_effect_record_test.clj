(ns futon2.aif.declared-effect-record-test
  "Focused unit tests for the C2 declared-effect record producer/validator.
  All observation readers are injected; no git, no network, no JVM state.
  Git-reading replay tests live in test-slow (declared_effect_record_slow_test.clj)
  and are run explicitly via the :test-slow alias.

  Reviewer round (declared-effect-record-independent-review.json): the exact
  probe negatives -- unproven bare-boolean observation, forged self-hashed
  schema, tampered body passing reconstruct -- are included verbatim below."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.declared-effect-record :as der]))

(def ^:private target "T-declared-effect-test")
(def ^:private base-sha "base-ref")
(def ^:private resolved "deadbeef")

(defn- tmp-dir []
  (str (java.nio.file.Files/createTempDirectory "declared-effect"
        (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- write-declaration!
  "Write a declaration file into DIR. Extras merge into the map (e.g.
  :patterns noise, to prove it never reaches the record)."
  [dir & {:as extras}]
  (let [path (str dir "/declaration.edn")
        base {:schema :wm/cascade-source-v1
              :target target
              :want [:want-a :want-b]
              :horizon-steps 2
              :locators
              {:want-a {:class :C4 :repo "futon2" :sha "HEAD"
                        :path "docs/a.md" :decl "WANT-A-DONE"}
               :want-b {:class :C4 :repo "futon2" :sha "HEAD"
                        :path "docs/b.md" :decl "WANT-B-DONE"}}
              ;; Legacy inner-route fuel: must never be imported.
              :facts [:want-a]
              :patterns {:p1 {:guard {:needs #{:want-a}} :produces #{:want-b}}}
              :candidates [{:precedence [:p1]}]}
        d (merge base extras)]
    (spit path (pr-str d))
    path))

(defn- proof
  "A full-credit C4 result whose evidence proves LOCATOR was read at the
  base revision; OBSERVED overrides the boolean."
  ([locator] (proof locator false))
  ([locator observed]
   {:observed observed :check :C4
    :evidence {:repo (:repo locator) :sha base-sha :resolved-sha resolved
               :path (:path locator) :decl (:decl locator) :file-present true}}))

(defn- proving-reader
  "Reader keyed on the locator :decl; unmatched decls get a proven false
  observation. Overrides let a test corrupt exactly one result."
  [overrides]
  (fn [locator]
    (if-let [o (get overrides (:decl locator))]
      o
      (proof locator))))

(defn- base-inputs [dir observe]
  {:target target
   :declaration {:path (str dir "/declaration.edn")}
   :base-revision {:repo "futon2" :sha base-sha}
   :observe observe})

(deftest positive-record-freezes-pinned-declaration-and-before-observations
  (let [dir (tmp-dir)
        _ (write-declaration! dir)
        record (der/build-record (base-inputs dir (proving-reader {})))]
    (testing "declaration pinned by path and byte sha256"
      (is (= :pinned (get-in record [:declaration :status])))
      (is (re-matches #"[0-9a-f]{64}" (get-in record [:declaration :sha256]))))
    (testing "declared horizon carried, not defaulted"
      (is (= {:status :declared :value 2 :authority :declaration/horizon-steps}
             (:horizon record))))
    (testing "both wants observed false at base, active, base resolved-sha pinned"
      (is (= [[target :want-a] [target :want-b]] (:active-wants record)))
      (is (= resolved (get-in record [:base-revision :resolved-sha])))
      (is (every? #(false? (get-in % [:observation :observed-before]))
                  (get-in record [:want-observations :active]))))
    (testing "legacy declaration fields never reach the record"
      (is (= [] (vec (filter der/legacy-fields (keys record)))))
      (is (nil? (:patterns record))))
    (testing "eligible, digest verifies, reconstructs from same inputs"
      (is (:eligible (:admission record)))
      (is (= [] (:refusals (:admission record))))
      (is (:verified (der/verify-record record)))
      (is (:verified (der/reconstruct (base-inputs dir (proving-reader {})) record))))
    (testing "a tampered body fails digest verification"
      (is (false? (:verified (der/verify-record (assoc record :horizon {:status :declared :value 9}))))))))

(deftest already-true-wants-are-listed-not-dropped
  (let [dir (tmp-dir)
        _ (write-declaration! dir)
        record (der/build-record
                (base-inputs dir (fn [l] (if (= "WANT-A-DONE" (:decl l))
                                           (proof l true)
                                           (proof l)))))]
    (testing "already-true want listed with its observation, excluded from active"
      (is (= [[target :want-a]] (:excluded-already-true record)))
      (is (= [[target :want-b]] (:active-wants record)))
      (is (= 1 (count (get-in record [:want-observations :excluded-already-true]))))
      (is (true? (get-in record [:want-observations :excluded-already-true 0
                                 :observation :observed-before])))
      (is (= (set (:wants record))
             (set (concat (:active-wants record) (:excluded-already-true record)))))
      (is (:eligible (:admission record))))))

(deftest all-wants-true-refuses-admission-typed
  (let [dir (tmp-dir)
        _ (write-declaration! dir)
        record (der/build-record (base-inputs dir (fn [l] (proof l true))))]
    (is (false? (:eligible (:admission record))))
    (is (contains? (set (:refusals (:admission record))) :no-open-declared-want))
    (is (= (:wants record) (:excluded-already-true record)))
    (is (:verified (der/verify-record record)))))

(deftest missing-trial5-declaration-is-typed-absent
  (let [record (der/build-record {:target "M-stack-geometry"
                                  :base-revision {:repo "futon5a" :sha "base-ref"}
                                  :observe (proving-reader {})})]
    (is (= :absent (get-in record [:declaration :status])))
    (is (= :no-target-effect-declaration (get-in record [:declaration :reason])))
    (is (= :absent (get-in record [:horizon :status])))
    (is (= [:no-declared-effect] (get-in record [:admission :refusals])))
    (is (false? (:eligible (:admission record))))
    (is (:verified (der/verify-record record)))))

(deftest missing-horizon-is-typed-absent-never-defaulted
  (let [dir (tmp-dir)
        _ (write-declaration! dir :horizon-steps nil)
        record (der/build-record (base-inputs dir (proving-reader {})))]
    (is (= {:status :absent :reason :horizon-not-declared} (:horizon record)))
    (is (contains? (set (:refusals (:admission record))) :no-declared-horizon))
    (is (false? (:eligible (:admission record))))
    (is (nil? (get-in record [:horizon :value])))))

(deftest missing-locator-is-typed-and-refuses-admission
  (let [dir (tmp-dir)
        _ (write-declaration! dir :locators {:want-a {:class :C4 :repo "futon2" :sha "HEAD"
                                                      :path "docs/a.md" :decl "WANT-A-DONE"}})
        record (der/build-record (base-inputs dir (proving-reader {})))]
    (testing "want-b has no locator: typed unknown, never observed-before false"
      (is (= 1 (count (get-in record [:want-observations :unknown]))))
      (is (= :missing-locator
             (get-in record [:want-observations :unknown 0 :observation :kind])))
      (is (nil? (some #(= [target :want-b] %) (:active-wants record))))
      (is (= :unknown (get-in record [:want-observations :unknown 0 :observation :status])))
      (is (false? (:eligible (:admission record))))
      (is (contains? (set (:refusals (:admission record))) :unknown-observation)))))

(deftest unsupported-locator-class-is-typed-never-reinterpreted
  (let [dir (tmp-dir)
        _ (write-declaration! dir :locators
                              {:want-a {:class :C3 :repo "futon2" :sha "HEAD"
                                        :path "docs/a.md" :decl "WANT-A-DONE"}
                               :want-b {:repo "futon2" :sha "HEAD"
                                        :path "docs/b.md" :decl "WANT-B-DONE"}})
        seen (atom [])
        record (der/build-record (base-inputs dir (fn [l] (swap! seen conj (:path l))
                                                    (proof l))))]
    (testing "C3 and class-less locators are typed, not silently read as C4"
      (is (= 2 (count (get-in record [:want-observations :unknown]))))
      (is (every? #(= :unsupported-locator-class %)
                  (mapv (fn [r] (get-in r [:observation :kind]))
                        (get-in record [:want-observations :unknown]))))
      (is (= [] (:active-wants record)))
      (is (= [] @seen) "no observation attempted for unsupported classes")
      (is (false? (:eligible (:admission record)))))))

(deftest bare-boolean-observation-without-evidence-cannot-become-false
  (let [dir (tmp-dir)
        _ (write-declaration! dir)
        ;; exact reviewer probe: reader returns a bare boolean, base revision
        ;; is a wrong repo / not-a-revision reference
        record (der/build-record
                {:target target
                 :declaration {:path (str dir "/declaration.edn")}
                 :base-revision {:repo "wrong-repo" :sha "not-a-revision"}
                 :observe (fn [_] {:observed false})})]
    (testing "unproven observations are typed unknown, never false-before"
      (is (= 2 (count (get-in record [:want-observations :unknown]))))
      (is (every? #(= :observation-evidence-mismatch %)
                  (mapv #(get-in % [:observation :kind])
                        (get-in record [:want-observations :unknown]))))
      (is (= [] (:active-wants record)))
      (is (nil? (get-in record [:want-observations :unknown 0
                                :observation :observed-before])))
      (is (nil? (get-in record [:base-revision :resolved-sha])))
      (is (false? (:eligible (:admission record))))
      (is (contains? (set (:refusals (:admission record))) :unknown-observation)))))

(deftest mismatched-evidence-control-with-boolean-false-is-typed
  (let [dir (tmp-dir)
        _ (write-declaration! dir)
        record (der/build-record
                (base-inputs dir (fn [l] (if (= "WANT-A-DONE" (:decl l))
                                           {:observed false :check :C4
                                            :evidence {:repo "other-repo" :sha base-sha
                                                       :resolved-sha resolved
                                                       :path (:path l) :decl (:decl l)}}
                                           (proof l)))))]
    (testing "a false observation whose evidence names another repo is refused"
      (is (= 1 (count (get-in record [:want-observations :unknown]))))
      (is (= :observation-evidence-mismatch
             (get-in record [:want-observations :unknown 0 :observation :kind])))
      (is (= [[target :want-b]] (:active-wants record)))
      (is (nil? (get-in record [:want-observations :unknown 0
                                :observation :observed-before]))))))

(deftest inconsistent-resolved-base-refuses-admission
  (let [dir (tmp-dir)
        _ (write-declaration! dir)
        record (der/build-record
                (base-inputs dir (fn [l] (assoc-in (proof l)
                                                   [:evidence :resolved-sha]
                                                   (if (= "WANT-A-DONE" (:decl l))
                                                     "sha-one" "sha-two")))))]
    (testing "rows resolving different base revisions are refused together"
      (is (= 2 (count (:active-wants record))))
      (is (contains? (set (:refusals (:admission record))) :observation-base-inconsistent))
      (is (false? (:eligible (:admission record)))))))

(deftest reader-refusal-stays-unknown-observation
  (let [dir (tmp-dir)
        _ (write-declaration! dir)
        record (der/build-record
                (base-inputs dir (fn [l]
                                   (if (= "WANT-B-DONE" (:decl l))
                                     (proof l)
                                     {:status :missing :kind :unknown-sha
                                      :data {:repo "futon2" :sha "nope"}}))))]
    (is (= 1 (count (get-in record [:want-observations :unknown]))))
    (is (= :unknown-sha (get-in record [:want-observations :unknown 0 :observation :kind])))
    (testing "the refused want is not recorded as false before"
      (is (= [[target :want-b]] (:active-wants record)))
      (is (nil? (some #(= [target :want-a] %) (:active-wants record))))
      (is (nil? (get-in record [:want-observations :unknown 0 :observation :observed-before]))))
    (is (false? (:eligible (:admission record))))))

(deftest contradictory-observation-cannot-become-false
  (let [dir (tmp-dir)
        _ (write-declaration! dir)
        record (der/build-record
                (base-inputs dir (fn [l]
                                   (if (= "WANT-A-DONE" (:decl l))
                                     ;; both a refusal and an observation: contradictory
                                     {:status :missing :kind :unknown-sha
                                      :observed false :check :C4}
                                     (proof l)))))]
    (is (= :contradictory-observation
           (get-in record [:want-observations :unknown 0 :observation :kind])))
    (testing "contradictory want is not active (not claimed false-before)"
      (is (= [[target :want-b]] (:active-wants record)))
      (is (nil? (get-in record [:want-observations :unknown 0 :observation :observed-before]))))))

(deftest non-boolean-observation-cannot-become-false
  (let [dir (tmp-dir)
        _ (write-declaration! dir)
        record (der/build-record
                (base-inputs dir (fn [l]
                                   (if (= "WANT-A-DONE" (:decl l))
                                     {:observed "maybe" :check :C4}
                                     (proof l)))))]
    (is (= :observation-not-boolean
           (get-in record [:want-observations :unknown 0 :observation :kind])))
    (is (= [[target :want-b]] (:active-wants record)))))

(deftest wrong-source-digest-refused
  (let [dir (tmp-dir)
        _ (write-declaration! dir)
        record (der/build-record (assoc (base-inputs dir (proving-reader {}))
                                        :declaration {:path (str dir "/declaration.edn")
                                                      :expected-sha256 (apply str (repeat 64 "0"))}))]
    (is (= :refused (get-in record [:declaration :status])))
    (is (= :declaration-source-mismatch (get-in record [:declaration :reason])))
    (is (contains? (set (:refusals (:admission record))) :declaration-source-refused))
    (is (false? (:eligible (:admission record))))
    (is (:verified (der/verify-record record)))))

(deftest blank-locator-fields-refused-as-missing-locator
  (let [dir (tmp-dir)
        _ (write-declaration! dir :locators {:want-a {:class :C4 :repo "futon2" :sha "HEAD"
                                                      :path "docs/a.md" :decl ""}
                                             :want-b {:class :C4 :repo "futon2" :sha "HEAD"
                                                      :path "docs/b.md" :decl "WANT-B-DONE"}})
        seen (atom [])
        record (der/build-record (base-inputs dir (fn [l] (swap! seen conj (:path l))
                                                    (proof l))))]
    (is (= :missing-locator (get-in record [:want-observations :unknown 0 :observation :kind])))
    (testing "no observation was attempted for the malformed locator"
      (is (not (contains? (set @seen) "docs/a.md")))
      (is (= ["docs/b.md"] @seen)))))

(deftest target-mismatch-refused
  (let [dir (tmp-dir)
        _ (write-declaration! dir)
        record (der/build-record (assoc (base-inputs dir (proving-reader {}))
                                        :target "someone-else"))]
    (is (= :declaration-target-mismatch (first (:refusals (:admission record)))))
    (is (false? (:eligible (:admission record))))))

(deftest invalid-inputs-are-typed-never-thrown
  (testing "nil/absent base revision"
    (let [record (der/build-record {:target target :base-revision nil
                                    :observe (proving-reader {})})]
      (is (= :refused (get-in record [:declaration :status])))
      (is (= [:invalid-build-inputs] (get-in record [:admission :refusals])))
      (is (:verified (der/verify-record record)))))
  (testing "blank declaration path"
    (let [record (der/build-record {:target target
                                    :declaration {:path " "}
                                    :base-revision {:repo "futon2" :sha "x"}
                                    :observe (proving-reader {})})]
      (is (= :declaration-source-missing (get-in record [:declaration :reason])))
      (is (= [:declaration-source-refused] (get-in record [:admission :refusals])))))
  (testing "nil target with no declaration is a typed absent record"
    (let [record (der/build-record {:target nil
                                    :base-revision {:repo "futon2" :sha "x"}
                                    :observe (proving-reader {})})]
      (is (= [:no-declared-effect] (get-in record [:admission :refusals]))))))

(deftest forged-self-hashed-record-fails-verification
  ;; exact reviewer probe: {:schema :bogus ...} hashed with its own digest
  (let [forged {:schema :bogus :target "forged" :admission {:eligible true}}
        forged (assoc forged :record/digest (der/record-digest forged))]
    (is (false? (:verified (der/verify-record forged))))
    (is (= :record-schema-mismatch (:reason (der/verify-record forged))))))

(deftest tampered-body-retaining-old-digest-fails-reconstruction
  ;; exact reviewer probe: valid record, :target swapped, digest kept
  (let [dir (tmp-dir)
        _ (write-declaration! dir)
        inputs (base-inputs dir (proving-reader {}))
        tampered (assoc (der/build-record inputs) :target "different-target")]
    (is (false? (:verified (der/verify-record tampered))))
    (is (false? (:verified (der/reconstruct inputs tampered))))))

(deftest admission-inconsistency-fails-verification
  (let [dir (tmp-dir)
        _ (write-declaration! dir)
        ;; an all-true record (derived [:no-open-declared-want]) forged eligible
        record (der/build-record (base-inputs dir (fn [l] (proof l true))))
        forged-admission (assoc record :admission {:eligible true :refusals []})
        rehashed (assoc forged-admission :record/digest (der/record-digest forged-admission))]
    (is (false? (:verified (der/verify-record rehashed))))
    (is (= :admission-inconsistent (:reason (der/verify-record rehashed))))))

(deftest want-census-tampering-fails-verification
  (let [dir (tmp-dir)
        _ (write-declaration! dir)
        record (der/build-record (base-inputs dir (proving-reader {})))
        dropped (update record :active-wants (fn [ws] (vec (rest ws))))
        rehashed (assoc dropped :record/digest (der/record-digest dropped))]
    (is (false? (:verified (der/verify-record rehashed))))
    (is (contains? #{:want-census-mismatch :admission-inconsistent}
                   (:reason (der/verify-record rehashed))))))
