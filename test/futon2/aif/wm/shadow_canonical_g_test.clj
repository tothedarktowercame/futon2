(ns futon2.aif.wm.shadow-canonical-g-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [clojure.java.io :as io]
            [futon2.aif.learning-trial-ledger :as ledger]
            [futon2.aif.outer-policy-evidence :as evidence]
            [futon2.aif.outer-policy-objective :as objective]
            [futon2.aif.selected-outer-cascade-policy-test]
            [futon2.aif.wm.selection-cascade-identity-test :as fixture]
            [futon2.aif.wm.family-canonical-g :as canonical]
            [futon2.aif.wm.family-selection :as sut]))

(defn fixture-var [sym]
  (var-get (ns-resolve 'futon2.aif.wm.selection-cascade-identity-test sym)))
(defn field []
  {:schema :wm/pre-choice-policy-field-v1
   :source-pin {:path "fixture" :sha256 (apply str (repeat 64 "a"))}
   :families [((fixture-var 'family) ((fixture-var 'occurrence-policy) 1))]})
(def join {:readings (constantly fixture/readings)})
(use-fixtures :each
  (fn [run]
    (let [root (.toFile (java.nio.file.Files/createTempDirectory "shadow-ledger" (make-array java.nio.file.attribute.FileAttribute 0)))]
      (try (with-redefs [ledger/default-root (.getPath root)] (run))
           (finally (doseq [f (reverse (file-seq root))] (io/delete-file f)))))))

(deftest default-body-is-pinned
  (let [menu (sut/sealed-pre-choice-menu (field) join)]
    (is (= :sealed (:status menu)))
    (is (nil? (:canonical-g-shadow menu)))
    (is (= "c946d01d2075cf352835d644d598a61cd9db10f9f488a65f9d8942a62ca97d61" (get-in menu [:menu-pin :sha256])))))

(deftest shadow-is-the-same-comparison-and-is-sealed
  (let [f (field)
        rows (sut/score-family-field (:families f) nil)
        expected (canonical/rescore rows)
        menu (sut/sealed-pre-choice-menu f (assoc join :g-law :canonical-coapply))
        shadow (:canonical-g-shadow menu)
        scores (mapv second (mapcat :scored (:rows expected)))]
    (is (= :sealed (:status menu)))
    (is (nil? (sut/verify-sealed-menu menu)))
    (is (= :diagnostic-only (:role shadow)))
    (is (false? (:consumed-by-selection shadow)))
    (is (= (:comparison expected) (:comparison shadow)))
    (is (seq scores))
    (is (= (mapv #(select-keys % [:g :risk :ambiguity :information-gain :horizon]) scores)
           (mapv #(select-keys % [:g :risk :ambiguity :information-gain :horizon]) (:entries shadow))))
    (is (= 1 (count (set (map :horizon (:entries shadow))))))
    (doseq [entry (:entries shadow)]
      (is (= (:g entry) (- (+ (:risk entry) (:ambiguity entry)) (:information-gain entry)))))
    (is (= :menu-pin-mismatch
           (sut/verify-sealed-menu (assoc-in menu [:canonical-g-shadow :consumed-by-selection] true))))))

(deftest objective-does-not-consume-shadow
  (let [plain (sut/sealed-pre-choice-menu (field) join)
        shadow (sut/sealed-pre-choice-menu (field) (assoc join :g-law :canonical-coapply))
        declare-fn (var-get (ns-resolve 'futon2.aif.selected-outer-cascade-policy-test 'declaration))
        declaration (declare-fn (mapv :cascade-digest (:entries plain)) [0.0])
        evaluate (fn [menu] (objective/evaluate (evidence/join menu declaration []) menu))
        a (evaluate plain) b (evaluate shadow)]
    (is (= (:entries plain) (:entries shadow)))
    (is (some? (:action a)) (pr-str a))
    (is (= (:action a) (:action b)))
    (is (= (:policies a) (:policies b)))))

(deftest canonical-refusal-does-not-block-menu
  (let [refusal {:status :refused :kind :inconsistent-pattern-belief}]
    (with-redefs [canonical/rescore (fn [_] {:rows [] :refusal refusal})]
      (let [menu (sut/sealed-pre-choice-menu (field) (assoc join :g-law :canonical-coapply))]
        (is (= :sealed (:status menu)))
        (is (= refusal (get-in menu [:canonical-g-shadow :refusal])))
        (is (nil? (sut/verify-sealed-menu menu)))))))
