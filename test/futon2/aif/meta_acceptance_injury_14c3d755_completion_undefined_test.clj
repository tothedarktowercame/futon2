(ns futon2.aif.meta-acceptance-injury-14c3d755-completion-undefined-test
  "Out-of-band acceptance for injury 14c3d755 (run 2026-11-04-14c3d755,
  :reviewer-falsifier-failed at :reviewer-wait, target-kind :ticket).

  The click selected T-car3-queue-routing-design-note, whose completion
  surface is :completion-definition-not-found. The falsifier refused the
  work because nothing in it could be measured. The fix (futon2 2d7caf17a,
  ADMIT-1) keeps such a target out of the ordinary field while the
  :completion-undefined switch is :exclude. This test builds a field from
  T-car3's own row in the run's pinned Giraffe pass and checks that the target
  is not admitted."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.action-identity :as action-identity]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.outer-observation :as observation]
            [futon2.aif.outer-menu-fixture :as menu-fixture]))

(use-fixtures :each menu-fixture/with-fixture-constructor)

(def pass-path
  "/home/joe/code/storage/giraffe/20261104T062307.994Z-45f7419f/pass.edn")
(def pass-sha
  "50d3a2cb2928ecd8b223e2e6de47be46b0532b3002e316218ec1ccaa8cd6dc4e")
(def target "T-car3-queue-routing-design-note")

(def makeup {:default-reach 1/2})

(defn- temp-dir []
  (.toFile (java.nio.file.Files/createTempDirectory
            "injury-14c3d755"
            (make-array java.nio.file.attribute.FileAttribute 0))))

(defonce ^:private giraffe-loader
  (doto (clojure.lang.DynamicClassLoader.
         (.getContextClassLoader (Thread/currentThread)))
    (.addURL (.toURL (.toURI (io/file "/home/joe/code/futon3c/src"))))))

(defn- actual-var [symbol]
  ;; As in outer-observation-test: the serving JVM has futon3c/src; this test
  ;; process adds it so the field uses Giraffe's own run writer and verifier.
  (with-bindings {clojure.lang.Compiler/LOADER giraffe-loader}
    (requiring-resolve symbol)))

(defn- pinned-row []
  (let [text (slurp pass-path)]
    (is (= pass-sha (identity/sha256 (.getBytes text "UTF-8"))))
    (first (filter #(= (keyword target) (:id %))
                   (:order (edn/read-string text))))))

(deftest injury-14c3d755-target-is-not-admitted
  (let [row (pinned-row)
        _ (is (= :completion-definition-not-found
                 (get-in row [:completion :completion-class])))
        other (let [r {:id :T-defined :status :ready
                       :completion {:schema :wm/completion-surface-v2
                                    :completion-class :criteria-locators}}]
                (assoc r :dependency-reading
                       {:record-pin {:sha256 (action-identity/sha256 (pr-str r))}}))
        root (temp-dir)
        written ((actual-var 'futon3c.giraffe.run/write-run!)
                 {:root (.getPath root) :run-id "injury-14c3d755"
                  :git-sha "fixture"
                  :adapter-result {:input {:as-of "2026-11-04" :items [] :edges []}
                                   :rejected []}
                  :pass-output {:as-of "2026-11-04" :order [row other]}})
        tickets [{:id target :path (str "/fixture/" target ".md")
                  :status-class :live :status-line "OPEN" :item-line nil}
                 {:id "T-defined" :path "/fixture/T-defined.md"
                  :status-class :live :status-line "OPEN" :item-line nil}]
        authority {:loaded-missions {:missions []}
                   :loaded-tickets {:tickets tickets}
                   :loaded-excursions {:excursions []}
                   :ticket-targets [target "T-defined"]
                   :excursion-targets []}
        field (binding [observation/*resolve-var* actual-var]
                (observation/giraffe-field (io/file (:dir written))
                                           makeup authority))
        admitted (set (map :target (:field field)))]
    (is (contains? admitted "T-defined"))
    (is (not (contains? admitted target)))
    (is (= [target] (mapv :target (:outer-field/excluded field))))))
