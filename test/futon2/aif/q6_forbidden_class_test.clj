(ns futon2.aif.q6-forbidden-class-test
  "Ruling 3 on real policies: a refused policy stays compared, and its prediction
  class cannot be chosen (Requirements Q6, Q11)."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-selection :as selection]
            [futon2.aif.policy :as policy]
            [futon2.aif.previous-run :as previous-run]))

;; Run e71c7380's five candidates, verbatim subset. It enacted policy-e77a by the
;; action marginal and was refused; d5b9b513 then excluded that key and chose
;; policy-bd8f, a different key in the same prediction class at the same G.
(def recorded
  (edn/read-string {:default tagged-literal}
                   (slurp "test/fixtures/wm-run-facts/e71c7380-candidates.edn")))

(def ranked (:ranked recorded))
(def rows (map selection/selection-row ranked))

(defn- short-id [id] (last (str/split (str id) #"/")))

(defn- key-of [suffix]
  (some #(when (str/ends-with? (:cascade-id %) suffix) (get-in % [:action :policy-key])) ranked))

(defn- forbidding [& keys]
  {:keys (vec keys) :classes (selection/forbidden-classes rows keys)})

(defn- select [forbidden]
  (policy/select-action-cascades ranked {:beta (:beta recorded) :forbidden forbidden}))

(defn- refusal-kind [forbidden]
  (try (select forbidden) nil
       (catch clojure.lang.ExceptionInfo e (get-in (ex-data e) [:refusal :kind]))))

(deftest the-refused-class-is-the-prediction-class
  (let [e77a (key-of "policy-e77a1ab8b601e8195b8e")
        {:keys [classes] :as forbidden} (forbidding e77a)]
    (is (= 1 (count classes)))
    (is (= 4 (count (filter #(selection/forbidden-row? forbidden %) rows)))
        "bd8f and its three variants are one policy with e77a")))

(deftest the-d5b9b513-shape-keeps-five-compared
  (let [d (select (forbidding (key-of "policy-e77a1ab8b601e8195b8e")))
        law (:selection-law d)]
    (is (= 5 (count (get-in d [:selection-certificate :candidates])) (count (:posterior law))))
    (is (= "policy-e0e104ea5c0875d5820b" (short-id (get-in d [:action :id]))))
    (is (nil? (get-in law [:policy-comparison :stepped-over]))
        "the refused class was not the minimum, so nothing was stepped over")))

(deftest same-class-different-key-cannot-be-chosen
  ;; bd8f is what d5b9b513 took. With its class forbidden and the minimum class also
  ;; forbidden, nothing admissible remains: the re-choice is refused at the selector.
  (is (= :no-admissible-class
         (refusal-kind (forbidding (key-of "policy-e77a1ab8b601e8195b8e")
                                   (key-of "policy-e0e104ea5c0875d5820b"))))))

(deftest stepping-over-the-minimum-is-recorded
  (let [d (select (forbidding (key-of "policy-e0e104ea5c0875d5820b")))
        comparison (get-in d [:selection-law :policy-comparison])]
    (is (not= "policy-e0e104ea5c0875d5820b" (short-id (get-in d [:action :id]))))
    (is (= :q6-forbidden (:decided-by comparison)) "the refusal left one class")
    (is (= (get-in d [:action :policy-key])
           (get-in comparison [:admissible-minimum :policy-key])))
    (is (= :q6-forbidden (get-in comparison [:stepped-over :reason])))
    (is (= 1.699638937845117 (get-in comparison [:stepped-over :g])))))

(deftest a-key-outside-the-menu-forbids-itself-alone
  (is (= "policy-e0e104ea5c0875d5820b"
         (short-id (get-in (select (forbidding [:not-compared])) [:action :id])))))

(defn- exclusion-of [keys]
  (with-redefs-fn {#'previous-run/decide-exclusion
                   (fn [_ _] {:status :excluded :reason :previous-choice-refused-on-same-inputs
                              :excluded-keys keys})}
    #(previous-run/exclusion {:status :present} ranked {} 1 "precision")))

(deftest the-carrier-says-what-happened
  (let [{:keys [carrier forbidden refusal]} (exclusion-of [(key-of "policy-e77a1ab8b601e8195b8e")])]
    (is (= :forbidden (:status carrier)))
    (is (= 5 (:menu carrier)))
    (is (= 1 (count (:forbidden-classes carrier))))
    (is (not (contains? carrier :removed)) "nothing was removed, so nothing says it was")
    (is (not (contains? carrier :excluded-keys)))
    (is (= (:forbidden-classes carrier) (:classes forbidden)))
    (is (nil? refusal))))

(deftest every-class-forbidden-is-the-existing-repeat-refusal
  (let [{:keys [carrier refusal]}
        (exclusion-of [(key-of "policy-e77a1ab8b601e8195b8e")
                       (key-of "policy-e0e104ea5c0875d5820b")])]
    (is (= carrier refusal))
    (is (= [:refused :repeat-choice-after-refusal true]
           ((juxt :status :kind :no-admissible-class) carrier)))
    (is (some? (:rearm carrier)))))
