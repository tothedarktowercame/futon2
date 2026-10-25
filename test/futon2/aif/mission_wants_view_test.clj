(ns futon2.aif.mission-wants-view-test
  "The view is flight's own derivation, not a copy: for the same want
  source its admitted set must equal flight/source-wants' wants.  The
  seam route uses the committed H-interp fixture; the :a-exits route
  uses a mission text read in place of git, with the same recording
  observe, so no check runs in this suite."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.flight :as flight]
            [futon2.aif.mission-wants-view :as view]))

(def fixture-path "test/fixtures/h-interp/primary-wants-seam-v1.edn")

(defn fixture [] (edn/read-string (slurp fixture-path)))

(def mission-text
  "## Mission fixture

### Completion criteria

- The bridge is built.\n  - Exit: the bridge carries traffic.
  - Exit: the far side is reachable.
")

(deftest seam-view-admitted-equals-flight-wants
  (let [document (fixture)
        v (view/mission-wants-view
           (:target document)
           {:document document :repo "futon3c" :path "holes/missions/M-象-2000.md"})
        flight-result (flight/source-wants
                       {:kind :primary-seam :document document
                        :observe (constantly false)}
                       {:target (:target document)} {})]
    (is (= :primary-seam (:kind (:route v))))
    (is (= (:wants flight-result) (mapv :token (:admitted v))))
    ;; every waiting outcome is named with a reason, none dropped
    (is (= (set (map :token (get-in flight-result [:source :unlocated])))
           (set (map :token (:waiting v)))))
    (is (every? :reason (:waiting v)))
    ;; read-only: locators recorded, never observed, no verdict claimed
    (is (= :not-run (:observation v)))
    (is (= (mapv :locator (:admitted v)) (:observed-locators v)))
    (is (not-any? :met? (concat (:admitted v) (:outcomes v))))))

(deftest a-exits-view-admitted-equals-flight-wants
  (let [v (view/mission-wants-view
           "M-fixture" {:repo "futon3c" :path "holes/missions/M-fixture.md"
                        :seam-path "holes/missions/seam/M-fixture.primary-wants.edn"
                        :read-text mission-text
                        :document nil})
        flight-result (flight/source-wants
                       {:kind :a-exits :repo "futon3c"
                        :path "holes/missions/M-fixture.md"
                        :read-text (fn [_ _ _] mission-text)
                        :observe (constantly false)}
                       {:target "M-fixture"} {})]
    (is (= :a-exits (:kind (:route v))))
    (is (= :no-seam-document (get-in v [:route :seam-absent :absence])))
    (is (= (:wants flight-result) (mapv :token (:admitted v))))
    (is (= (set (map :token (:unlocated (:source flight-result))))
           (set (map :token (filter :token (:waiting v))))))
    (is (= :not-run (:observation v)))))

(deftest no-observation-runs-in-the-view
  ;; the recording observe never touches observation-checks: the seam
  ;; fixture's one admitted locator is recorded verbatim and its class
  ;; is carried, but no decl is read from any file
  (let [document (fixture)
        v (view/mission-wants-view (:target document) {:document document})]
    (is (= 1 (count (:admitted v))))
    (is (= :C8 (:locator-class (first (:admitted v)))))
    (is (= 1 (count (:observed-locators v))))
    (is (= :C8 (:class (first (:observed-locators v)))))))
