(ns futon2.aif.meta-preference-authority-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.meta-outer-policy :as policy]
            [futon2.aif.meta-preference-authority :as preference]))

(def path "/home/joe/code/futon3/library/meta/meta-outer-provisional-prior-v1.edn")
(def declaration-bytes
  (java.nio.file.Files/readAllBytes (.toPath (java.io.File. path))))
(def pin {:path path
          :sha256 "e18b3d24ec5ee1470ce78314d4d0c554a324ee63a47fe8fc7be09e7f696eef09"})
(def authority {:source-bytes declaration-bytes :expected-source-pin pin})

(deftest reads-exact-provisional-preferences-without-predictions
  (let [receipt (preference/read-authority authority)]
    (is (= :verified (:status receipt)))
    (is (= preference/vocabulary (:outcome-vocabulary receipt)))
    (is (= [1.0 1.0 0.0 0.0 0.0] (:preference-means receipt)))
    (is (= [4.0 4.0 4.0 4.0 4.0] (:preference-variances receipt)))
    (is (= [1.0 1.0 1.0 1.0 1.0] (:weights receipt)))
    (is (= :declared-prior-not-empirical-calibration (:epistemic-status receipt)))
    (is (nil? (:means receipt)) "candidate predictions are not produced")
    (is (nil? (:information-model receipt)))))

(deftest exact-source-and-semantic-mutations-refuse
  (let [declaration (edn/read-string (String. ^bytes declaration-bytes "UTF-8"))
        encode #(.getBytes (pr-str %) "UTF-8")
        changed [(assoc-in declaration [:channels 0 :preference :mean] 0.5)
                 (assoc-in declaration [:channels 0 :preference :variance] 3.0)
                 (assoc-in declaration [:channels 0 :preference :weight] 2.0)]]
    (doseq [mutation changed]
      (is (= :preference-authority-source-drift
             (:reason (preference/read-authority
                       {:source-bytes (encode mutation) :expected-source-pin pin})))))
    (doseq [mutation [(assoc-in declaration [:channels 0 :preference :variance] 0.0)
                      (assoc declaration :channels (vec (reverse (:channels declaration))))
                      (update declaration :channels pop)
                      (assoc-in declaration [:authority :epistemic-status]
                                :empirically-calibrated)]]
      (let [mutated (encode mutation)
            re-pinned {:path path
                       :sha256 (let [d (.digest (java.security.MessageDigest/getInstance "SHA-256")
                                                mutated)]
                                 (apply str (map #(format "%02x" (bit-and % 0xff)) d)))}]
        (is (= :preference-authority-invalid
               (:reason (preference/read-authority
                         {:source-bytes mutated :expected-source-pin re-pinned}))))))
    (is (= :preference-authority-unpinned
           (:reason (preference/read-authority
                     {:source-bytes declaration-bytes}))))))

(deftest elapsed-and-token-normalization-requires-candidate-envelope
  (let [authority (preference/read-authority authority)
        observation {:run-output {:outcome :grounded-progress}
                     :terminal-outcome {:class :other}
                     :registered-run/timing {:wall-clock-ms 500}
                     :registered-run/model-usage {:total-tokens 200}}
        normalized (preference/normalize-observation
                    authority {:resource-envelope {:time-budget-ms 1000
                                                    :token-budget 400}
                               :observation observation})]
    (is (= [0.0 1.0 0.0 0.5 0.5] (:values normalized)))
    (is (= :candidate-resource-envelope-required
           (:reason (preference/normalize-observation authority
                                                     {:observation observation}))))
    (is (= :normalization-input-missing-or-invalid
           (:reason (preference/normalize-observation
                     authority
                     {:resource-envelope {:time-budget-ms 1000 :token-budget 400}
                      :observation (update observation :registered-run/model-usage dissoc
                                           :total-tokens)}))))))

(deftest evaluator-remains-blocked-with-preferences-but-no-predictions
  (let [preference-input (preference/read-authority authority)
        candidate {:id :m
                   :slots {:task-kind :mission :target "M-one" :next-move :advance
                           :resource-envelope {:time-budget-ms 1000 :token-budget 400
                                               :author-seat "author" :reviewer-seat "reviewer"}
                           :evidence-channel
                           {:source {:path "M-one.md" :sha256 (apply str (repeat 64 "a"))}
                            :locator {:class :C4 :repo "futon2" :path "M-one.md" :decl "done"}}
                           :stopping-rule :grounded-progress}
                   :g-input (select-keys preference-input
                                         [:outcome-vocabulary :preference-means
                                          :preference-variances :weights])}
        contract {:schema :meta/outer-policy-cascade-v1 :id :meta/test
                  :patterns [:observe :select] :precedence [[:observe :select]]
                  :slots {:task-kind {:required true :type :enum}
                          :target {:required true :type :current-meta-item-id}
                          :next-move {:required true :type :enum}
                          :resource-envelope {:required true :type :resource-envelope
                                              :fields {:time-budget-ms :positive-integer
                                                       :token-budget :positive-integer
                                                       :author-seat :agent-id
                                                       :reviewer-seat :agent-id}}
                          :evidence-channel {:required true
                                             :type :closure-or-progress-observer
                                             :fields {:source :source-pin
                                                      :locator :checkable-locator}}
                          :stopping-rule {:required true :type :enum}}
                  :generative-model {:outcomes preference/vocabulary
                                     :missing-term-policy :refuse-not-zero}
                  :selection {:law :argmin-G}}
        field {:schema :wm/meta-field-observation-v1
               :source-pin {:path "field" :sha256 (apply str (repeat 64 "b"))}
               :rows [{:id "M-one" :kind :mission
                       :source (get-in candidate [:slots :evidence-channel :source])}]}
        result (policy/evaluate
                {:contract contract
                 :contract-source {:path "contract" :sha256 (apply str (repeat 64 "c"))}
                 :field-observation field :observation {} :candidates [candidate]})]
    (is (= :outcome-vocabulary-invalid (:reason result)))
    (is (= [:outcome-dimension-mismatch]
           (get-in result [:details :errors]))
        "preferences alone do not fabricate candidate predictive dimensions")))
