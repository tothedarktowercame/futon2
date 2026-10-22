(ns futon2.aif.declared-effect-record-slow-test
  "Real-replay tests for the C2 declared-effect record: the production C4
  reader (observation-checks/check-decl-in-file) against pinned historical
  futon2 revisions of the T-repair ticket's authored declaration and its
  before/after observations. These shell out to git and are therefore outside
  the default test run: execute explicitly with
    clojure -M:test:test-slow -n futon2.aif.declared-effect-record-slow-test
  Every deftest is also tagged ^:slow."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.declared-effect-record :as der]))

;; Pinned history (futon2 main, object store shared with this worktree):
;; ae69f5e7 2026-09-22 ticket committed, status OPEN, no recheck/split
;; 0798f96a 2026-09-23 held-out split made C4-observable (decl present)
;; 97e17e10 2026-09-23 acceptance: ticket status DONE, recheck CLEARED
(def ^:private t-repair-target
  "T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade")
(def ^:private t-repair-declaration
  "resources/wm/cascade-sources/T-repair-occ-444fb018.edn")
(def ^:private before-sha "ae69f5e7b1a1d9012742fddc6d14313ac9292e3d")
(def ^:private split-declared-sha "0798f96ad2082a8f80c8a850c8e1138c036213f9")
(def ^:private after-sha "97e17e10f2695c481c55ddaebe9026f2d245663f")

(defn- repair-inputs [base-sha]
  {:target t-repair-target
   :declaration {:path t-repair-declaration}
   :base-revision {:repo "futon2" :sha base-sha}})

(deftest ^:slow real-before-observation-is-false-at-pinned-history
  (let [record (der/build-record (repair-inputs before-sha))]
    (testing "declaration pinned, horizon 4 declared by the source"
      (is (= :pinned (get-in record [:declaration :status])))
      (is (= {:status :declared :value 4 :authority :declaration/horizon-steps}
             (:horizon record))))
    (testing "restoration-accepted really observed false at the pinned base"
      (is (= [[t-repair-target :restoration-accepted]] (:active-wants record)))
      (is (false? (get-in record [:want-observations :active 0
                                  :observation :observed-before])))
      (is (= before-sha
             (get-in record [:want-observations :active 0
                             :observation :evidence :resolved-sha])))
      (is (= before-sha (get-in record [:base-revision :resolved-sha]))))
    (is (:eligible (:admission record)))
    (is (:verified (der/verify-record record)))
    (testing "reconstruction with the real reader reproduces the digest"
      (is (:verified (der/reconstruct (repair-inputs before-sha) record))))))

(deftest ^:slow real-after-observation-is-true-and-refuses-admission
  (let [record (der/build-record (repair-inputs after-sha))]
    (testing "the want is really true at the pinned acceptance revision"
      (is (= [[t-repair-target :restoration-accepted]] (:excluded-already-true record)))
      (is (= [] (:active-wants record))))
    (is (contains? (set (:refusals (:admission record))) :no-open-declared-want))
    (is (false? (:eligible (:admission record))))
    (is (:verified (der/verify-record record)))))

(deftest ^:slow real-mixed-record-lists-already-true-alongside-open-want
  (let [dir (System/getProperty "java.io.tmpdir")
        path (str dir "/declared-effect-real-mixed.edn")
        _ (spit path (pr-str {:schema :wm/cascade-source-v1
                              :target t-repair-target
                              :want [:repair/split-declared-valid :restoration-accepted]
                              :horizon-steps 4
                              ;; locators copied verbatim from the authored
                              ;; T-repair declaration
                              :locators
                              {:repair/split-declared-valid
                               {:class :C4 :repo "futon2" :sha "HEAD"
                                :path "resources/wm/eig/held-out-split.edn"
                                :decl "HELD-OUT-SPLIT-DECLARED"}
                               :restoration-accepted
                               {:class :C4 :repo "futon2" :sha "HEAD"
                                :path (str "holes/tickets/T-repair-occ-444fb018cbbb656d09b8"
                                           "f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade.md")
                                :decl "**Status:** DONE"}}}))
        inputs {:target t-repair-target
                :declaration {:path path}
                :base-revision {:repo "futon2" :sha split-declared-sha}}
        record (der/build-record inputs)]
    (testing "split declared true (listed, excluded), restoration still open (active)"
      (is (= [[t-repair-target :repair/split-declared-valid]]
             (:excluded-already-true record)))
      (is (= [[t-repair-target :restoration-accepted]] (:active-wants record)))
      (is (true? (get-in record [:want-observations :excluded-already-true 0
                                 :observation :observed-before])))
      (is (false? (get-in record [:want-observations :active 0
                                  :observation :observed-before]))))
    (is (:eligible (:admission record)))
    (is (:verified (der/verify-record record)))))

(deftest ^:slow real-reader-refusal-stays-unknown
  (let [inputs {:target t-repair-target
                :declaration {:path t-repair-declaration}
                :base-revision {:repo "futon2" :sha (apply str (repeat 40 "0"))}}
        record (der/build-record inputs)]
    (testing "an unresolvable base revision is a typed unknown, never false"
      (is (= 1 (count (get-in record [:want-observations :unknown]))))
      (is (= :unknown-sha (get-in record [:want-observations :unknown 0 :observation :kind])))
      (is (= [] (:active-wants record)))
      (is (nil? (get-in record [:want-observations :unknown 0
                                :observation :observed-before])))
      (is (false? (:eligible (:admission record)))))))
