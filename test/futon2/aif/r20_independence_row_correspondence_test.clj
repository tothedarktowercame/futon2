(ns futon2.aif.r20-independence-row-correspondence-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.wm.apparatus-certificates :as apparatus]))

(def fixture-path
  "test/resources/wm/r20-independence-row/run90ca-projection.edn")

(defn fixture [] (edn/read-string (slurp fixture-path)))

(defn independence-row [participants]
  (->> (:rows (apparatus/receipt {:decision {} :participants participants}))
       (filter #(= :independence (:node %)))
       first))

(deftest retained-pair-matches-the-formal-present-unequal-case
  (let [{:keys [participants apparatus-independence]} (fixture)]
    (is (= apparatus-independence (independence-row participants)))
    (is (= ["zai-2" "zai-3"]
           [(get-in participants [:roles :author :identity])
            (get-in participants [:roles :reviewer-of-record :identity])]))))

(deftest equal-supplied-identities-disagree
  (let [participants (:participants (fixture))
        author (get-in participants [:roles :author :identity])
        collapsed (assoc-in participants [:roles :reviewer-of-record]
                            {:status :present :identity author})]
    (is (= {:node :independence
            :verdict :disagrees
            :case :same-agent
            :checked
            {:author
             {:path [:participants :roles :author]
              :receipt {:status :present :identity "zai-2"}
              :identity "zai-2"}
             :reviewer
             {:path [:participants :roles :reviewer-of-record]
              :receipt {:status :present :identity "zai-2"}
              :identity "zai-2"}}}
           (independence-row collapsed)))))

(deftest absence-is-decided-before-identity-comparison
  (let [participants (:participants (fixture))]
    (doseq [[role missing] [[:author [:author]]
                            [:reviewer-of-record [:reviewer-of-record]]]]
      (testing (name role)
        (let [changed (assoc-in participants [:roles role]
                                {:status :absent :reason :test-absence})
              row (independence-row changed)]
          (is (= :not-recorded (:verdict row)))
          (is (= missing (:missing row)))
          (is (= :participant-identity-not-observed (:reason row))))))))
