(ns futon2.aif.policy-satisfaction-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.outer-observation :as outer-observation]
            [futon2.aif.outer-select :as outer-select]
            [futon2.aif.outer-state :as outer-state]
            [futon2.aif.policy-authoring-resolution :as resolution]
            [futon2.aif.policy-satisfaction :as sut]
            [futon2.aif.wm.authored.req-f0ba279a :as authored]))

(def mission "M-futonzero-prelim-practice")
(def policy-id "M-futonzero-prelim-practice/policy-87eb5c880a0b9ab8e194")
(def mission-sha "e1b01eb3e0c1cef716174978e63a6b9ac100e968032bb59adbdf31929d805cf0")
(def grounded {:repo "/home/joe/code/futon0"
               :sha "cfb1408629826c6d825e2ddc06aad84ee5ac6108"})
(def checks (vec (sort-by pr-str (map :completion-check (vals authored/clauses)))))
(def authoring-resolution (resolution/for-policy policy-id))
(def construction
  {:mission mission :selected-cascade policy-id
   :cascade {:operational-plan
             {:steps (mapv (fn [check] {:clause {:completion-check check}}) checks)}
             :selected-action {:authoring-resolution authoring-resolution}}})
(def selection-judgment
  {:ranked-candidates [{:target mission :cascade-id policy-id
                        :action {:authoring-resolution authoring-resolution}}]})
(def qualified
  {:run/id "attempt-002"
   :failure {:kind :already-satisfied :stage :author-wait
             :detail {:failure-kind :already-satisfied :failure-stage :author-wait
                      :outcome :already-satisfied :grounded-commit grounded}}
   :run-output {:outcome :already-satisfied}})
(def field {:families [{:status :computed :target-id mission
                        :policies [{:policy-id policy-id :cascade {:nodes []}
                                    :authoring-resolution authoring-resolution}
                                   {:policy-id (str mission "/policy-other") :cascade {:nodes []}}]
                        :failures [] :failure-count 0}]})

(deftest producer-qualified-close-excludes-exact-next-policy
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                       "wm-policy-satisfaction" (make-array java.nio.file.attribute.FileAttribute 0)))
        retained (sut/retain! root {:run-record qualified :construction construction
                                    :selection-judgment selection-judgment
                                    :mission-source-sha256 mission-sha})
        next-field (sut/exclude-satisfied field root {mission mission-sha})
        family (first (:families next-field))]
    (is (= policy-id (:policy-id retained)))
    (is (= checks (:completion-checks retained)))
    (is (= grounded (:grounded-commit retained)))
    (is (= :all-policies-already-satisfied
           (:reason (sut/all-satisfied-exclusion root mission mission-sha))))
    (is (.isAbsolute (io/file sut/default-root)))
    (is (= (get-in retained [:record-pin :sha256])
           (evidence/sha256
            (java.nio.file.Files/readAllBytes
             (.toPath (io/file (get-in retained [:record-pin :path])))))))
    (is (= [(str mission "/policy-other")] (mapv :policy-id (:policies family))))
    (is (= :policy-already-satisfied (-> family :failures last :kind)))
    (is (get-in family [:failures 0 :record-pin :sha256]))
    (testing "mission byte change invalidates the evidence"
      (is (= 2 (count (get-in (sut/exclude-satisfied field root {mission "changed"})
                              [:families 0 :policies])))))
    (is (nil? (sut/all-satisfied-exclusion root mission "changed")))
    (testing "authored check-set change invalidates the evidence"
      (let [changed (assoc-in field [:families 0 :policies 0 :authoring-resolution]
                              (assoc authoring-resolution :completion-checks []))]
        (is (= 2 (count (get-in (sut/exclude-satisfied changed root {mission mission-sha})
                                [:families 0 :policies]))))))
    (testing "an unqualified close writes nothing"
      (is (nil? (sut/retain! root {:run-record (assoc-in qualified [:failure :stage] :build)
                                   :construction construction
                                   :selection-judgment selection-judgment
                                   :mission-source-sha256 mission-sha}))))))

(deftest excluding-the-only-policy-is-typed
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                       "wm-policy-satisfaction" (make-array java.nio.file.attribute.FileAttribute 0)))
        one (assoc-in field [:families 0 :policies]
                      [{:policy-id policy-id :cascade {:nodes []}
                        :authoring-resolution authoring-resolution}])]
    (sut/retain! root {:run-record qualified :construction construction
                       :selection-judgment selection-judgment
                       :mission-source-sha256 mission-sha})
    (let [family (get-in (sut/exclude-satisfied one root {mission mission-sha}) [:families 0])]
      (is (= :failed (:status family)))
      (is (empty? (:policies family)))
      (is (= :policy-already-satisfied (-> family :failures last :kind))))))

(defonce ^:private giraffe-loader
  (doto (clojure.lang.DynamicClassLoader.
         (.getContextClassLoader (Thread/currentThread)))
    (.addURL (.toURL (.toURI (io/file "/home/joe/code/futon3c/src"))))))

(defn- actual-var [symbol]
  (with-bindings {clojure.lang.Compiler/LOADER giraffe-loader}
    (requiring-resolve symbol)))

(deftest qualified-close-removes-target-from-real-outer-field
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                       "wm-policy-outer" (make-array java.nio.file.attribute.FileAttribute 0)))
        run-dir (io/file "/home/joe/code/storage/giraffe/20261028T154151.285Z-b916cd4e")
        makeup (outer-state/load-makeup)
        state (outer-state/init-state makeup)]
    (with-redefs [sut/default-root (.getAbsolutePath root)]
      (sut/retain! root {:run-record qualified :construction construction
                         :selection-judgment selection-judgment
                         :mission-source-sha256 mission-sha})
      (let [outer (binding [outer-observation/*resolve-var* actual-var]
                    (outer-observation/giraffe-field run-dir makeup))
            exclusion (some #(when (= mission (:target %)) %)
                            (:outer-field/excluded outer))
            result (outer-select/select
                    makeup state
                    (assoc outer :focus "M-wm-outer-selection"
                           :relation (fn [_ _] :focus)))]
        (is (= :all-policies-already-satisfied (:reason exclusion)))
        (is (= [policy-id] (:policy-ids exclusion)))
        (is (not (some #{mission} (map :target (:field outer)))))
        (is (= "M-reachable-from-boot"
               (get-in result [:selection :selected :target])))))
    (let [changed-source (io/file root "changed-mission.md")
          empty-root (io/file root "empty")
          authority (fn [path]
                      {:loaded-missions {:missions [{:id mission :path path
                                                     :status-class :open}]}
                       :loaded-tickets {:tickets []}
                       :loaded-excursions {:excursions []}
                       :ticket-targets [] :excursion-targets []})]
      (spit changed-source "changed mission bytes\n")
      (with-redefs [sut/default-root (.getAbsolutePath root)]
        (let [changed (binding [outer-observation/*resolve-var* actual-var]
                        (outer-observation/giraffe-field
                         run-dir makeup (authority (.getAbsolutePath changed-source))))]
          (is (some #{mission} (map :target (:field changed))))))
      (.mkdirs empty-root)
      (with-redefs [sut/default-root (.getAbsolutePath empty-root)]
        (let [without-records (binding [outer-observation/*resolve-var* actual-var]
                                (outer-observation/giraffe-field
                                 run-dir makeup
                                 (authority "/home/joe/code/futon0/holes/missions/M-futonzero-prelim-practice.md")))]
          (is (some #{mission} (map :target (:field without-records)))))))))
