(ns futon2.aif.outer-argue-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.outer-argue :as argue]
            [futon2.aif.outer-select :as outer-select]))

(def library-root
  (or (System/getenv "L54D_LIBRARY_ROOT")
      "/home/joe/code/futon3/library/task-selection"))

(def fixture
  (edn/read-string
   (slurp (io/resource "resources/argument/selection-561c5ec3.edn"))))

(def selection (:outer/selection fixture))

(def fixture-ratings
  (mapv (fn [class]
          {:target (:pick class)
           :status (get-in class [:giraffe :status])
           :cone (get-in class [:giraffe :cone])
           :unblocks (get-in class [:giraffe :unblocks])})
        (get-in selection [:why :classes])))

(defn- selection-frame [selection]
  (argue/frame {:why (:why selection)
                :cascades (:cascades selection)
                :relations (:cascade-pick-relations selection)
                :per-task-rating fixture-ratings
                :library-root library-root}))

(defn- fixture-frame [] (selection-frame selection))

(def frozen-attacks
  [[:giraffe-order :find-out-before-building :gate true]
   [:giraffe-order :serves-a-way :order false]
   [:find-out-before-building :giraffe-order :gate false]
   [:find-out-before-building :serves-a-way :gate false]
   [:serves-a-way :giraffe-order :absence true]
   [:serves-a-way :find-out-before-building :gate true]
   [:giraffe-order :stopping :stopping true]
   [:find-out-before-building :stopping :stopping true]
   [:serves-a-way :stopping :stopping true]
   [:stopping :giraffe-order :stopping false]
   [:stopping :find-out-before-building :stopping false]
   [:stopping :serves-a-way :stopping false]
   [:giraffe-order :serves-a-way :undercut-unshown false]])

(deftest frozen-frame-conforms
  (let [frame (fixture-frame)]
    (is (= frozen-attacks
           (mapv (juxt :from :to :kind :succeeds) (:attacks frame))))
    (is (= [:serves-a-way] (:grounded frame)))
    (is (true? (:agrees frame)))
    (is (= :cascade/argument-frame-v1 (:schema frame)))
    (is (every? :sha256 (mapcat :warrants (:arguments frame))))))

(defn- update-class [selection id f]
  (update-in selection [:why :classes]
             (fn [classes]
               (mapv #(if (= id (keyword (name (first (:cascades %)))))
                        (f %)
                        %)
                     classes))))

(deftest shown-undercut-is-resolved-only-by-g
  (let [shown-selection
        (update-class selection :serves-a-way
                      #(assoc-in (assoc-in % [:giraffe :cone] 11)
                                 [:giraffe :unblocks] 0))
        frame (selection-frame shown-selection)
        undercut (last (:attacks frame))]
    (is (= [:giraffe-order :serves-a-way :undercut false true]
           ((juxt :from :to :kind :succeeds :shown) undercut)))
    (testing "the target's lower G defeats the shown undercut"
      (is (= [:serves-a-way] (:grounded frame))))
    (testing "swapping the two G values lets the undercut defeat"
      (let [swapped (-> shown-selection
                        (update-class :giraffe-order #(assoc % :G 2.1432169255819717))
                        (update-class :serves-a-way #(assoc % :G 2.4694636005859008)))
            swapped-frame (selection-frame swapped)
            swapped-undercut (last (:attacks swapped-frame))]
        (is (true? (:succeeds swapped-undercut)))
        (is (not-any? #{:serves-a-way} (:grounded swapped-frame)))))))

(deftest typed-undercut-predicate-cases
  (let [predicate-result #'argue/predicate-result
        ratings [{:target "own" :status :ready :unblocks 0}
                 {:target "other" :status :ready :unblocks 3}]
        other-predicate
        "giraffe.unblocks < other.giraffe.unblocks (other is any other ready candidate in the field)"
        cone-predicate "giraffe.cone > 0 and giraffe.unblocks = 0"]
    (testing "another ready candidate can show the comparison"
      (is (true? (:shown (predicate-result ratings {:unblocks 0} "own"
                                                   other-predicate))))
      (is (false? (:shown (predicate-result ratings {:unblocks 5} "own"
                                                    other-predicate)))))
    (testing "nil is absence, not a value making the comparison false"
      (let [result (predicate-result ratings {:cone nil :unblocks 0} "own"
                                     cone-predicate)]
        (is (false? (:shown result)))
        (is (re-find #"not recorded" (:why result)))
        (is (not (re-find #"cone.*is false" (:why result))))))
    (testing "an explicitly unrecorded predicate stays unshown"
      (is (= {:shown false :why "predicate is unrecorded"}
             (predicate-result ratings {} "own" "unrecorded"))))))

(deftest lean-example-conforms
  (let [arguments [{:id :giraffe-order :G 2.4694636005859008}
                   {:id :find-out-before-building :G 2.8567834606757225}
                   {:id :serves-a-way :G 2.1432169255819717}
                   {:id :stopping :G 13.730053366819615}]
        pairs [[:giraffe-order :find-out-before-building]
               [:giraffe-order :serves-a-way]
               [:find-out-before-building :giraffe-order]
               [:find-out-before-building :serves-a-way]
               [:serves-a-way :giraffe-order]
               [:serves-a-way :find-out-before-building]
               [:giraffe-order :stopping]
               [:find-out-before-building :stopping]
               [:serves-a-way :stopping]
               [:stopping :giraffe-order]
               [:stopping :find-out-before-building]
               [:stopping :serves-a-way]]
        attacks (mapv (fn [[from to]] {:from from :to to}) pairs)]
    (is (= [:serves-a-way] (argue/grounded arguments attacks)))))

(deftest preference-boundaries
  (let [frame (fixture-frame)
        arguments (:arguments frame)
        attacks (:attacks frame)]
    (testing "a new strict minimum enters the grounded extension"
      (let [swapped (mapv #(case (:id %)
                            :giraffe-order (assoc % :G 2.0)
                            :serves-a-way (assoc % :G 2.5)
                            %)
                          arguments)]
        (is (= [:giraffe-order] (argue/grounded swapped attacks)))))
    (testing "equal G permits the mutual defeats"
      (let [tied-g (->> arguments
                        (mapv #(if (= :serves-a-way (:id %))
                                 (assoc % :G 2.4694636005859008)
                                 %)))]
        (is (not-any? #{:serves-a-way :giraffe-order}
                      (argue/grounded tied-g attacks)))))
    (testing "unknown IDs and self-attacks never defeat"
      (is (false? (argue/defeats? arguments
                                  {:from :unknown :to :serves-a-way})))
      (is (false? (argue/defeats? arguments
                                  {:from :serves-a-way :to :serves-a-way}))))))

(deftest frame-failure-never-refuses-the-click
  (testing "a malformed why makes the wiring return an unavailable frame, not a throw"
    (let [frame (#'outer-select/argument-frame {:why {:classes [{}]} :cascades [] :relations []
                                                :library-root library-root})]
      (is (= :unavailable (:status frame)))
      (is (= :cascade/argument-frame-v1 (:schema frame)))
      (is (some? (:reason frame)))))
  (testing "the bad case really throws without the wrapper"
    (is (thrown? Throwable (argue/frame {:why {:classes [{}]} :cascades [] :relations []
                                         :library-root library-root})))))
