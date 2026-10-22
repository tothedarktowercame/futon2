(ns futon2.aif.declared-effect-record-test
  "Focused unit tests for the C2 declared-effect record producer/validator.
  All observation readers are injected; no git, no network, no JVM state.
  Git-reading replay tests live in test-slow (declared_effect_record_slow_test.clj)
  and are run explicitly via the :test-slow alias."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.declared-effect-record :as der]))

(def ^:private target "T-declared-effect-test")

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

(defn- canned-reader
  "Reader keyed on the locator :decl. Unmatched decls return a canned
  observation of false with evidence."
  [results]
  (fn [locator]
    (get results (:decl locator)
         {:observed false :check :C4
          :evidence {:repo (:repo locator) :resolved-sha "deadbeef"
                     :path (:path locator) :decl (:decl locator)}})))

(defn- base-inputs [dir observe]
  {:target target
   :declaration {:path (str dir "/declaration.edn")}
   :base-revision {:repo "futon2" :sha "base-ref"}
   :observe observe})

(deftest positive-record-freezes-pinned-declaration-and-before-observations
  (let [dir (tmp-dir)
        _ (write-declaration! dir)
        record (der/build-record (base-inputs dir (canned-reader {})))]
    (testing "declaration pinned by path and byte sha256"
      (is (= :pinned (get-in record [:declaration :status])))
      (is (re-matches #"[0-9a-f]{64}" (get-in record [:declaration :sha256]))))
    (testing "declared horizon carried, not defaulted"
      (is (= {:status :declared :value 2 :authority :declaration/horizon-steps}
             (:horizon record))))
    (testing "both wants observed false at base, active, base resolved-sha pinned"
      (is (= [[target :want-a] [target :want-b]] (:active-wants record)))
      (is (= "deadbeef" (get-in record [:base-revision :resolved-sha])))
      (is (every? #(false? (get-in % [:observation :observed-before]))
                  (get-in record [:want-observations :active]))))
    (testing "legacy declaration fields never reach the record"
      (is (= [] (vec (filter der/legacy-fields (keys record)))))
      (is (nil? (:patterns record))))
    (testing "eligible, digest verifies, reconstructs from same inputs"
      (is (:eligible (:admission record)))
      (is (= [] (:refusals (:admission record))))
      (is (:verified (der/verify-record record)))
      (is (:verified (der/reconstruct (base-inputs dir (canned-reader {})) record))))
    (testing "a tampered body fails digest verification"
      (is (false? (:verified (der/verify-record (assoc record :horizon {:status :declared :value 9}))))))))

(deftest already-true-wants-are-listed-not-dropped
  (let [dir (tmp-dir)
        _ (write-declaration! dir)
        record (der/build-record
                (base-inputs dir (canned-reader {"WANT-A-DONE" {:observed true :check :C4
                                                                :evidence {:resolved-sha "s1"}}})))]
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
        record (der/build-record
                (base-inputs dir (canned-reader {"WANT-A-DONE" {:observed true :check :C4 :evidence {:resolved-sha "s1"}}
                                                 "WANT-B-DONE" {:observed true :check :C4 :evidence {:resolved-sha "s1"}}})))]
    (is (false? (:eligible (:admission record))))
    (is (contains? (set (:refusals (:admission record))) :no-open-declared-want))
    (is (= (:wants record) (:excluded-already-true record)))
    (is (:verified (der/verify-record record)))))

(deftest missing-trial5-declaration-is-typed-absent
  (let [record (der/build-record {:target "M-stack-geometry"
                                  :base-revision {:repo "futon5a" :sha "base-ref"}
                                  :observe (canned-reader {})})]
    (is (= :absent (get-in record [:declaration :status])))
    (is (= :no-target-effect-declaration (get-in record [:declaration :reason])))
    (is (= :absent (get-in record [:horizon :status])))
    (is (= [:no-declared-effect] (get-in record [:admission :refusals])))
    (is (false? (:eligible (:admission record))))
    (is (:verified (der/verify-record record)))))

(deftest missing-horizon-is-typed-absent-never-defaulted
  (let [dir (tmp-dir)
        _ (write-declaration! dir :horizon-steps nil)
        record (der/build-record (base-inputs dir (canned-reader {})))]
    (is (= {:status :absent :reason :horizon-not-declared} (:horizon record)))
    (is (contains? (set (:refusals (:admission record))) :no-declared-horizon))
    (is (false? (:eligible (:admission record))))
    (is (nil? (get-in record [:horizon :value])))))

(deftest missing-locator-is-typed-and-refuses-admission
  (let [dir (tmp-dir)
        _ (write-declaration! dir :locators {:want-a {:class :C4 :repo "futon2" :sha "HEAD"
                                                      :path "docs/a.md" :decl "WANT-A-DONE"}})
        record (der/build-record (base-inputs dir (canned-reader {})))]
    (testing "want-b has no locator: typed unknown, never observed-before false"
      (is (= 1 (count (get-in record [:want-observations :unknown]))))
      (is (= :missing-locator
             (get-in record [:want-observations :unknown 0 :observation :kind])))
      (is (nil? (some #(= [target :want-b] %) (:active-wants record))))
      (is (= :unknown (get-in record [:want-observations :unknown 0 :observation :status])))
      (is (false? (:eligible (:admission record))))
      (is (contains? (set (:refusals (:admission record))) :unknown-observation)))))

(deftest reader-refusal-stays-unknown-observation
  (let [dir (tmp-dir)
        _ (write-declaration! dir)
        record (der/build-record
                (base-inputs dir (fn [locator]
                                   (if (= "WANT-B-DONE" (:decl locator))
                                     {:observed false :check :C4 :evidence {:resolved-sha "s1"}}
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
                (base-inputs dir (fn [locator]
                                   (if (= "WANT-A-DONE" (:decl locator))
                                     ;; both a refusal and an observation: contradictory
                                     {:status :missing :kind :unknown-sha
                                      :observed false :check :C4}
                                     {:observed false :check :C4 :evidence {:resolved-sha "s1"}}))))]
    (is (= :contradictory-observation
           (get-in record [:want-observations :unknown 0 :observation :kind])))
    (testing "contradictory want is not active (not claimed false-before)"
      (is (= [[target :want-b]] (:active-wants record)))
      (is (nil? (get-in record [:want-observations :unknown 0 :observation :observed-before]))))))

(deftest non-boolean-observation-cannot-become-false
  (let [dir (tmp-dir)
        _ (write-declaration! dir)
        record (der/build-record
                (base-inputs dir (fn [locator]
                                   (if (= "WANT-A-DONE" (:decl locator))
                                     {:observed "maybe" :check :C4}
                                     {:observed false :check :C4 :evidence {:resolved-sha "s1"}}))))]
    (is (= :observation-not-boolean
           (get-in record [:want-observations :unknown 0 :observation :kind])))
    (is (= [[target :want-b]] (:active-wants record)))))

(deftest wrong-source-digest-refused
  (let [dir (tmp-dir)
        _ (write-declaration! dir)
        record (der/build-record (assoc (base-inputs dir (canned-reader {}))
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
                                                    {:observed false :check :C4
                                                     :evidence {:resolved-sha "s1"}})))]
    (is (= :missing-locator (get-in record [:want-observations :unknown 0 :observation :kind])))
    (testing "no observation was attempted for the malformed locator"
      (is (not (contains? (set @seen) "docs/a.md")))
      (is (= ["docs/b.md"] @seen)))))

(deftest target-mismatch-refused
  (let [dir (tmp-dir)
        _ (write-declaration! dir)
        record (der/build-record (assoc (base-inputs dir (canned-reader {}))
                                        :target "someone-else"))]
    (is (= :declaration-target-mismatch (first (:refusals (:admission record)))))
    (is (false? (:eligible (:admission record))))))
