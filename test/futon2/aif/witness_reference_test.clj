(ns futon2.aif.witness-reference-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.observation-checks :as oc]
            [futon2.aif.observation-reference-test :as fixture]
            [futon2.aif.observation-rates :as rates]
            [futon2.aif.cascade-sources :as sources]
            [futon2.aif.cascade-problems :as problems]
            [futon2.aif.cascade-model-manifest :as model]
            [futon2.aif.declared-preference-scales-test :as scales]))

(def locator {:class :C6 :repo "fixture" :sha "HEAD" :path "witness.edn"})
(defn observe [loc] (oc/observe {:probe loc}))
(defn write-witness! [repo value]
  (spit (io/file repo "witness.edn") (pr-str value))
  (fixture/git! repo "add" "witness.edn")
  (fixture/commit! repo "(defn old-head [] 1)\n"))

(deftest real-git-witness-reference-controls
  (fixture/with-repo
   (fn [repo sha]
     (is (false? (get-in (observe locator) [:results :probe :observed])))
     (let [good {:repo "fixture" :sha sha :entry "source.clj"}
           bad (assoc good :sha "non-resolving-reference")]
       (write-witness! repo good)
       (let [result (observe locator)]
         (is (true? (get-in result [:results :probe :observed])))
         (is (= sha (get-in result [:results :probe :evidence :reference :resolved-sha]))))
       (is (not= good bad))
       (write-witness! repo bad)
       (let [result (observe locator)]
         (is (= :unknown-sha (get-in result [:refused :probe :kind])))
         (is (= "non-resolving-reference" (get-in result [:refused :probe :data :sha])))
         (is (not (contains? (:results result) :probe)))
         (println "NON-RESOLVING-WITNESS" (pr-str result)))
       (write-witness! repo (assoc good :entry "missing-record.edn"))
       (is (false? (get-in (observe locator) [:results :probe :observed])))
       (write-witness! repo (dissoc good :entry))
       (is (true? (get-in (observe locator) [:results :probe :observed])))
       (is (= :no-locator (get-in (observe (assoc locator :require-entry true)) [:refused :probe :kind])))
       (write-witness! repo "I assert that the event happened")
       (is (= :invalid-witness (get-in (observe locator) [:refused :probe :kind])))))))

(deftest declaration-and-contract-admit-exact-presence-channel
  (let [d (scales/declaration)
        loaded (sources/load-declared)
        contract (edn/read-string (slurp (io/resource "wm/observation-contract.edn")))
        observed (get-in loaded [:observations scales/target])
        assembled (problems/assemble
                   {:targets [scales/target]
                    :sources (assoc (sources/with-context-fn loaded) :horizon-steps 2)})
        sourced (rates/sourced-rates nil nil nil (:locators d) contract)]
    (is (= 3 (count (:want d))))
    (is (every? false? (map :observed (vals (:results observed)))))
    (is (empty? (:refused observed)))
    (is (empty? (:refusals assembled)))
    (is (= :sourced (:status sourced)))
    (is (= #{:community} (set (map :owner (vals (:entries d))))))
    (is (every? #(= :not-consumed (:owner-status %)) (vals (:entries d))))
    (is (every? #(= :C6 (:class %)) (vals (:locators d))))
    (is (true? (get-in d [:locators :premise-refused-before-work :require-entry])))
    (is (true? (get-in d [:locators :obligation-resolved-through-the-account :require-entry])))))

(deftest declared-scale-controls-still-hold
  (let [d (scales/declaration)
        two (scales/spec-from (assoc-in d [:lam :value] 2))]
    (is (= #{} (:zeroed two)))
    (is (> ((model/log-preference-fn two) (:want two))
           ((model/log-preference-fn two) #{})))
    (is (= 2 (:lam two)))
    (is (= 2 (reduce + (vals (:weights two)))))
    (is (= :invalid-preference-scale
           (try (scales/load-one (assoc-in d [:lam :value] 0)) nil
                (catch clojure.lang.ExceptionInfo e (:kind (ex-data e))))))))
