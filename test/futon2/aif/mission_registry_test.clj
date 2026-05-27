(ns futon2.aif.mission-registry-test
  "Tests for the mission-doc substrate adapter that flips
   `forward-model/can-propose? :open-mission` to true."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.action-proposer :as ap]
            [futon2.aif.forward-model :as fm]
            [futon2.aif.mission-registry :as mr])
  (:import (java.io File)
           (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(def ^:dynamic *tmpdir* nil)

(defn- with-tmpdir [f]
  (let [dir (Files/createTempDirectory "wm-mission-registry-test"
                                       (into-array FileAttribute []))]
    (binding [*tmpdir* (str dir)]
      (try (f)
           (finally
             (doseq [^File child (reverse (file-seq (io/file (str dir))))]
               (.delete child)))))))

(use-fixtures :each with-tmpdir)

(defn- write-mission!
  [relative-path body]
  (let [path (io/file *tmpdir* relative-path)]
    (io/make-parents path)
    (spit path body)
    (.getAbsolutePath path)))

(deftest load-missions-finds-top-level-mission-docs-test
  (write-mission! "futon0/holes/missions/M-alpha.md"
                  (str "**Status:** OPEN\n"
                       "# Mission Alpha\n"))
  (write-mission! "futon3/holes/missions/M-beta.md"
                  (str "**Status:** COMPLETE\n"
                       "# Mission Beta\n"))
  (write-mission! "futon3/holes/missions/M-beta.journal/hinge-log.md"
                  "# Not a mission doc\n")
  (write-mission! "futon5a/holes/missions/M-handoffs/README.md"
                  "# Not a top-level mission doc\n")
  (let [doc (mr/load-missions *tmpdir*)
        ids (set (map :id (:missions doc)))]
    (is (= #{"M-alpha" "M-beta"} ids))
    (is (not (contains? ids "hinge-log")))
    (is (not (contains? ids "README")))))

(deftest open-missions-filters-complete-test
  (let [doc {:missions [{:id "M-open" :status-class :open}
                        {:id "M-active" :status-class :active}
                        {:id "M-partial" :status-class :partial}
                        {:id "M-complete" :status-class :complete}]}
        openes (mr/open-missions doc)]
    (is (= #{"M-open" "M-active" "M-partial"}
           (set (map :id openes))))))

(deftest default-open-missions-includes-at-least-one-addressable-mission-test
  (let [missions (mr/open-missions)
        ids (set (map :id missions))]
    (is (seq missions))
    (is (contains? ids "M-the-futon-stack"))))

(deftest can-propose-open-mission-when-state-has-missions-test
  (is (false? (fm/can-propose? {:missions []} :open-mission)))
  (is (false? (fm/can-propose? {} :open-mission)))
  (is (true? (fm/can-propose? {:missions [{:id "M-alpha"}]} :open-mission))))

(deftest can-execute-open-mission-requires-target-in-state-test
  (let [state {:missions [{:id "M-alpha"} {:id "M-beta"}]}]
    (is (true? (fm/can-execute? state {:type :open-mission :target "M-alpha"})))
    (is (true? (fm/can-execute? state {:type :open-mission :target "M-beta"})))
    (is (false? (fm/can-execute? state {:type :open-mission :target "M-missing"})))))

(deftest mission-enumerator-proposer-emits-candidates-test
  (let [state {:missions [{:id "M-alpha" :title "Mission Alpha" :status-class :open :path "/tmp/a"}
                          {:id "M-beta" :title "Mission Beta" :status-class :partial :path "/tmp/b"}]}
        candidates (ap/propose mr/mission-enumerator-proposer state)]
    (is (= 2 (count candidates)))
    (is (every? #(= :open-mission (:type %)) candidates))
    (is (= #{"M-alpha" "M-beta"} (set (map :target candidates))))
    (is (every? :mission-path candidates))
    (is (every? #(string? (:rationale %)) candidates))))

(deftest proposer-id-test
  (is (= :mission-enumerator (ap/proposer-id mr/mission-enumerator-proposer))))

(deftest integration-bootstrap-no-longer-surfaces-open-mission-gap-test
  (let [state-with-missions {:missions [{:id "M-alpha" :title "Mission Alpha" :status-class :open}]
                             :observation {}
                             :belief {}}
        proposed (ap/propose ap/bootstrap-proposer state-with-missions)
        learn-targets (->> proposed
                           (filter #(= :learn-action-class (:type %)))
                           (map :target-class)
                           set)]
    (is (not (contains? learn-targets :open-mission)))
    (is (contains? learn-targets :address-sorry))
    (is (contains? learn-targets :fire-pattern))))
