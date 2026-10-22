(ns futon2.aif.wm.family-selection-canonical-g-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [clojure.java.io :as io]
            [futon2.aif.policy :as policy]
            [futon2.aif.q4-preference :as q4]
            [futon2.aif.learning-trial-ledger :as ledger]
            [futon2.aif.cascade-shape-g :as shape]
            [futon2.aif.cascade-selection :as selection]
            [futon2.aif.reading-policy-preference :as pref]
            [futon2.aif.wm.cascade-decision :as decision]
            [futon2.aif.wm.family-selection :as sut]))

(defn fixture [f]
  (let [root (.toFile (java.nio.file.Files/createTempDirectory "canonical-g-live-" (make-array java.nio.file.attribute.FileAttribute 0)))]
    (try
      (with-redefs [ledger/default-root (.getPath root)
                    policy/select-action-cascades (fn [ranked _] {:action (:action (first ranked))})]
        (f))
      (finally (doseq [file (reverse (file-seq root))] (io/delete-file file))))))
(use-fixtures :each fixture)

(defn family [target patterns]
  {:status :computed :target-id target :distinct-count 1 :failures []
   :provenance {:analysis-digest "fixture-analysis" :excerpt-digest "fixture-excerpt"}
   :policies [{:target target :policy-id :same-id :kind :fixture
               :analysis {:sentences []}
               :cascade {:nodes (mapv (fn [p] {:pattern p :occurrence-id p}) patterns) :edges []}}]})
(def families [(family "A" [:p]) (family "B" [:q :r :s])])

(deftest one-horizon-and-canonical-readers
  (let [seen (atom nil)
        result (sut/select-over-families families
                 {:g-law :canonical-coapply
                  :certify (fn [_ scored]
                             (reset! seen scored)
                             {:status :certified :beta 1})})
        scores (mapv second @seen)
        ranked (:ranked result)]
    (is (= :selected (:status result)))
    (is (= [3 3] (mapv :horizon-steps ranked)))
    (is (= (sort (map :G-efe ranked)) (map :G-efe ranked)))
    (is (= [1 2] (mapv :rank ranked)))
    (is (= :compareAtHorizonN (get-in result [:canonical-g :lean-reduction])))
    (doseq [score scores]
      (is (= :certified (:status (pref/validate score))) (pr-str (pref/validate score)))
      (is (= (:g score) (get-in score [:scorer-result 0 :G-efe])
             (get-in score [:selection-candidate :g])))
      (is (= (:selection-law score) (selection/law-receipt [(:selection-candidate score)])))
      (is (= false (get-in score [:legacy-g :consumed-in-g?])))
      (is (= :class-model-preference (get-in score [:scorer-result 0 :certificate :c-source :source])))
      (is (= :refused (:status (pref/validate (update score :g inc))))))
    (let [config (:config (#'decision/precision-config {} families ranked @seen))]
      (is (= :classKernel (get-in config [:preference :authority])))
      (is (= :cascade-shape-g/class-emission-model (get-in config [:progress-model :authority]))))
    (let [before (into {} (map (fn [[p s]] [[(:target p) (:policy-id p)] s]))
                       (mapcat :scored (sut/score-family-field families)))]
      (doseq [[p s] @seen]
        (is (= (select-keys (before [(:target p) (:policy-id p)]) [:f :computed-f :fit])
               (select-keys s [:f :computed-f :fit])))))))

(deftest default-path-records-unchanged
  (let [rows (sut/score-family-field families)
        baseline (sut/construct-ranked rows)
        seen (atom nil)
        result (sut/select-over-families families
                 {:certify (fn [_ scored] (reset! seen scored) {:status :certified :beta 1})})]
    ;; Exact printed carrier comparison includes selection-candidate/law, the
    ;; preference schedule and class/progress/fit terms, not only the winner.
    (is (= (pr-str (:scored baseline)) (pr-str @seen)))
    (is (= (pr-str (:ranked baseline)) (pr-str (:ranked result))))
    (is (= [:A :B] (mapv (comp keyword :target-id) (:ranked result))))
    (is (= (mapv #(pref/validate (second %)) (:scored baseline))
           (mapv #(pref/validate (second %)) @seen)))
    (is (= :cascade-shape-g/progress-preference
           (get-in (#'decision/precision-config {} families (:ranked result) @seen)
                   [:config :preference :authority])))
    (is (not (contains? result :canonical-g)))))

(deftest ledger-counts-change-novelty-not-transition
  (ledger/record! ledger/default-root
    {:trials (mapv (fn [i observed]
                    {:status :admitted-at-attempt-grain :deduplication {:identity (str i)}
                     :learning-family "old" :meaning-sha256 "fixture"
                     :after-observation observed :effect ["B" :done]
                     :selected-cascade {:precedence [{:id :old :produces #{["B" :done]}}]}})
                  (range 4) [true true false false])})
  (let [result (sut/select-over-families [(family "A" [:new]) (family "B" [:old])]
                                       {:g-law :canonical-coapply})
        [new old] (:ranked result)]
    (is (= :selected (:status result)))
    (is (= "A" (:target-id new)))
    (is (< (:G-efe new) (:G-efe old)))
    (is (= (get-in new [:certificate :g-terms :risk]) (get-in old [:certificate :g-terms :risk])))
    (is (= [1/2 1/10] (mapv #(get-in % [:certificate :parameter-novelty :total]) [new old])))))

(deftest refused-candidate-leaves-menu
  (let [score shape/score-policy]
    (with-redefs [shape/score-policy
                  (fn [p] (cond-> (score p) (= "A" (:target p))
                            (assoc-in [:class-emission :model :target-class "A"] :unknown)))]
      (let [result (sut/select-over-families families {:g-law :canonical-coapply})]
        (is (= :selected (:status result)))
        (is (= ["B"] (mapv :target-id (:ranked result))))
        (is (= :canonical-g (get-in result [:failures 0 :stage])))
        (is (= :class-unknown-no-scalar-g (get-in result [:failures 0 :kind])))))))

(deftest inconsistent-snapshot-never-selects
  (let [score shape/score-policy calls (atom 0)]
    (with-redefs [shape/score-policy
                  (fn [p] (cond-> (score p) (= "B" (:target p))
                            (assoc-in [:candidate :precedence :co-apply :patterns :p :theta-record]
                                      {:status :recorded-trials :trials-count 1 :successes 1 :identities ["different"]})))
                  policy/select-action-cascades (fn [& _] (swap! calls inc))]
      (let [result (sut/select-over-families [(family "A" [:p]) (family "B" [:p])]
                                           {:g-law :canonical-coapply})]
        (is (= :abstained (:status result)))
        (is (= :inconsistent-pattern-belief (get-in result [:decision :refusal :kind])))
        (is (empty? (:ranked result)))
        (is (zero? @calls))))))

(deftest canonical-q4-and-ties
  (let [fs (mapv (fn [f] (assoc-in f [:policies 0 :policy-id] (str (:target-id f) "/policy-1")))
                 [(family "A" [:p]) (family "B" [:q])])
        receipts (atom [])
        result (sut/select-over-families fs
                 {:g-law :canonical-coapply
                  :certify (fn [_ scored]
                             (reset! receipts (mapv #(pref/validate (second %)) scored))
                             {:status :certified :beta 1})})
        ranked (:ranked result)
        carrier (q4/carrier
                 {:selection-law {:posterior (into {} (map #(vector (:action %) 1/2)) ranked)}
                  :selection-certificate
                  {:selection-route :wm/reading-policy-family-v1
                   :preference-receipts @receipts
                   :candidates (mapv (fn [e] {:id (:action e) :g (:G-efe e)
                                              :class-emission (:class-emission e)}) ranked)}})]
    (is (= [1 1] (mapv :rank ranked)))
    (is (= :certified (:status carrier)) (pr-str carrier))
    (is (q4/lean-q4 carrier))
    (is (= [:terminal-class :terminal-class] (mapv :semantics (:policies carrier))))))

(deftest all-candidates-refused-never-selects
  (let [score shape/score-policy calls (atom 0)]
    (with-redefs [shape/score-policy
                  (fn [p] (assoc-in (score p) [:class-emission :model :target-class (:target p)] :unknown))
                  policy/select-action-cascades (fn [& _] (swap! calls inc))]
      (let [r (sut/select-over-families families {:g-law :canonical-coapply})]
        (is (= :abstained (:status r)))
        (is (= :no-admissible-candidate (get-in r [:decision :refusal :reason])))
        (is (= 2 (count (:failures r))))
        (is (every? #(= :canonical-g (:stage %)) (:failures r)))
        (is (zero? @calls))))))
