(ns futon2.aif.flight-primary-seam-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.flight :as flight]
            [futon2.aif.outcome-wants :as outcome-wants]))

(def fixture-path "test/fixtures/h-interp/primary-wants-seam-v1.edn")

(defn fixture []
  (edn/read-string (slurp fixture-path)))

(defn result
  ([document] (result document (constantly false)))
  ([document observe]
   (flight/source-wants {:kind :primary-seam
                         :document document
                         :observe observe}
                        {:target (:target document)} {})))

(defn outcome [document id]
  (first (filter #(= id (:id %)) (:outcomes document))))

(deftest admitted-mined-graph-flies-and-p0-waits
  (let [document (fixture)
        mined (outcome document :mined-graph)
        expected-token (:token (outcome-wants/outcome-criterion
                                (:target document) mined))
        actual (result document)
        waiting (get-in actual [:source :unlocated])]
    (is (= [expected-token] (:wants actual)))
    (is (= 1 (count waiting)))
    (is (= :no-admitted-locator (:reason (first waiting))))
    (is (= (get-in (outcome document :p0-reconstruction) [:locator :would-be])
           (:would-be (first waiting))))
    (is (= #{:wants :locators :universe :source} (set (keys actual))))))

(deftest injected-observer-supplies-the-mined-graph-value
  (let [actual (result (fixture) (constantly false))
        token (first (:wants actual))]
    (is (false? (get-in actual [:universe token])))))

(deftest unconfirmed-classification-waits-even-with-a-locator
  (let [document (fixture)
        replacement (get-in document [:variants :unconfirmed-classification :replace-steps])
        document (update document :outcomes
                         (fn [outcomes]
                           (mapv #(if (= :mined-graph (:id %))
                                    (assoc-in % [:provenance :steps] replacement)
                                    %)
                                 outcomes)))
        actual (result document)
        mined-token (:token (outcome-wants/outcome-criterion
                             (:target document)
                             (outcome document :mined-graph)))
        mined-wait (first (filter #(= mined-token (:token %))
                                  (get-in actual [:source :unlocated])))]
    (is (= :unconfirmed-classification (:reason mined-wait)))
    (is (not (some #{mined-token} (:wants actual))))))

(deftest outcome-tokens-are-stable-on-rerun
  (let [document (fixture)]
    (is (= (:wants (result document)) (:wants (result document))))
    (is (= (keys (get-in (result document) [:source :criteria-by-token]))
           (keys (get-in (result document) [:source :criteria-by-token]))))))

(deftest criterion-keeps-the-provenance-quote-span
  (let [document (fixture)
        mined (outcome document :mined-graph)
        token (:token (outcome-wants/outcome-criterion
                       (:target document) mined))]
    (is (= [10126 10419]
           (get-in (result document)
                   [:source :criteria-by-token token :provenance :quote :span])))))
