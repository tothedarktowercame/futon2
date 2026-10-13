(ns futon2.aif.eoi-declaration-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-problems :as problems]
            [futon2.aif.cascade-sources :as sources]
            [futon2.aif.observation-checks :as checks]))

(def target "M-expressions-of-interest")
(def resource "wm/cascade-sources/M-expressions-of-interest.edn")

(defn assemble [loaded]
  (problems/assemble {:targets [target]
                      :sources (assoc (sources/with-context-fn loaded) :horizon-steps 2)}))

(deftest declaration-keeps-unproduced-presence-wants-unknown
  (let [loaded (sources/load-declared)
        result (assemble loaded)
        observation (get-in loaded [:observations target])
        wants (set (get-in loaded [:wants target]))
        facts (get-in loaded [:universes target])
        refusal (first (:refusals result))]
    (is (= 3 (count wants)))
    (is (= wants (set (keys facts))))
    (is (every? #{:unknown} (vals facts))
        "absence of a produced witness is unknown, never false")
    (is (empty? (:results observation)))
    (is (= wants (set (keys (:refused observation)))))
    (is (every? #(= {:status :missing :kind :no-mechanical-check
                     :data {:class nil}}
                    %)
                (vals (:refused observation))))
    (is (= :universe-not-admitted (:kind refusal)))
    (is (= :locators (:missing refusal)))
    (is (= wants (set (:tokens-without-checkable-locator refusal))))
    (is (empty? (:problems result))
        "the unobserved universe cannot reach candidate construction")
    (doseq [[_ receipt] (get-in loaded [:interpretations target :receipts])]
      (is (re-matches #"[0-9a-f]{40}" (get-in receipt [:source :sha])))
      (is (re-matches #"[0-9a-f]{64}" (get-in receipt [:source :sha256]))))
    (let [without-wants (assoc-in loaded [:wants target] [])
          wrong-token :not-in-declared-universe
          wrong-universe (assoc-in loaded [:universes target wrong-token] false)
          substituted (checks/observe
                       {:change-authored-and-bound {:class :C7}})]
      (is (= :want-not-declared
             (get-in (assemble without-wants) [:refusals 0 :kind])))
      (is (some #{wrong-token}
                (get-in (assemble wrong-universe)
                        [:refusals 0 :tokens-without-checkable-locator])))
      (is (= {:status :missing :kind :no-mechanical-check
              :data {:class :C7}}
             (get-in substituted [:refused :change-authored-and-bound]))
          "an unsupported substituted check stays refused"))
    (println "EOI-DECLARATION-OBSERVATIONS" (pr-str observation))))

(deftest reinterpretation-source-pin-is-enforced
  (let [declaration (edn/read-string (slurp (io/resource resource)))
        pattern :coordination/bounded-execution
        bad (assoc-in declaration [:interpretation-receipts pattern :source :sha256] "wrong")
        dir (.toFile (java.nio.file.Files/createTempDirectory
                      "eoi-declaration" (make-array java.nio.file.attribute.FileAttribute 0)))
        file (io/file dir "source.edn")]
    (try
      (is (not= declaration bad))
      (spit file (pr-str bad))
      (is (= :interpretation-source-hash-mismatch
             (try (sources/load-declared (str dir)) nil
                  (catch clojure.lang.ExceptionInfo e (:reason (ex-data e))))))
      (finally (io/delete-file file true) (io/delete-file dir true)))))
