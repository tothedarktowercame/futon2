(ns futon2.aif.fold-cascade-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.fold :as fold]
            [futon2.aif.fold-classical :as classical]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.interpretation-evidence :as evidence]))

(defn- entry [expected locator]
  (let [target "construction-observation-control"
        token [target :source-present]
        output [target :declared-result]
        path "futon2/src/futon2/aif/fold.clj"
        hash (evidence/sha256
              (java.nio.file.Files/readAllBytes
               (.toPath (io/file "/home/joe/code" path))))]
    {:action {:kind :cascade-candidate :id :control :target target
              :observation-locators {token locator}
              :construction-receipt {:kind :test-construction :moves []}
              :interpretation-receipts
              {:test/pinned-source
               {:source {:path path :sha256 hash}
                :reading "Constructed control: a source presence guard gates a declared output."}}
              :precedence
              [{:id :test/pinned-source :target target :produces #{output}
                :guard {:status :interpreted :operator :and
                        :clauses [{:status :interpreted
                                   :present (if expected #{token} #{})
                                   :absent (if expected #{} #{token})}]}
                :transition {:status :interpreted :operator :union :produces #{output}}}]}}))

(defn- pinned-locator [class path]
  (let [{:keys [exit out]} (shell/sh "git" "rev-parse" "HEAD")]
    (assert (zero? exit))
    {:class class :repo "futon2" :sha (str/trim out) :path path}))

(defn- construct [e]
  (runner/construction-wiring-result (runner/construct-for-decision e)))

(deftest production-checker-demonstration-pair
  (doseq [[label expected locator boxes]
          [[:present true (pinned-locator :C3 "src/futon2/aif/fold.clj") 1]
           [:missing true (pinned-locator :C3 "does-not-exist-fold-control") 0]
           [:definite-absence false (pinned-locator :C3 "does-not-exist-fold-control") 1]
           [:c6-false-is-not-absence false (pinned-locator :C6 "does-not-exist-fold-control") 0]
           [:refused-is-not-absence false {:class :C3} 0]]]
    (let [r (construct (entry expected locator))
          out (:fold-output r)]
      (is (= :wired (:status r)))
      (is (= :cascade-interpretation (:fold/route out)))
      (is (= boxes (count (get-in out [:wiring :boxes]))))
      (is (= (- 1 boxes) (count (:policy-holes out))))
      (is (= (pos? boxes) (fold/closes? out)))
      (is (= (when (pos? boxes) -1.0) (:coverage-score-delta out)))
      (when (pos? boxes)
        (is (= (:sha locator)
               (get-in out [:wiring :boxes 0 :conditions 0 :witness :observation :evidence :resolved-sha]))))
      (prn {:demonstration label :boxes boxes :holes (count (:policy-holes out))
            :coverage (:coverage-score-delta out) :shape (get-in r [:shape-validation :ok])}))))

(deftest receipts-and-transitions-cannot-be-fabricated
  (let [e (entry true (pinned-locator :C3 "src/futon2/aif/fold.clj"))]
    (doseq [bad [(assoc-in e [:action :interpretation-receipts :test/pinned-source :source :sha256]
                         (apply str (repeat 64 "0")))
                 (assoc-in e [:action :precedence 0 :transition :produces] #{})
                 (assoc-in e [:action :precedence 0 :guard :operator] :or)]]
      (let [out (:fold-output (construct bad))]
        (is (empty? (get-in out [:wiring :boxes])))
        (is (= 1 (count (:policy-holes out))))
        (is (nil? (:coverage-score-delta out)))))))

(deftest known-rule-still-needs-proof-fields
  (let [out (classical/classical-fold ["devmap-coherence/prototype-structure-checklist"] {})]
    (is (= 1 (count (get-in out [:wiring :boxes]))))
    (is (false? (:ok (fold/validate-fold-output-v1 out))))))

(deftest recorded-inputs-without-observation-locators-retain-holes
  (doseq [[name n] [["compliance" 1] ["expressions-of-interest" 3]]]
    (let [e (:selected-entry (edn/read-string
                             (slurp (str "test/fixtures/cascade-fold-repair/" name ".edn"))))
          out (:fold-output (construct e))]
      (is (empty? (get-in out [:wiring :boxes])))
      (is (= n (count (:policy-holes out))))
      (is (every? :guard-evidence (:policy-holes out)))
      (is (nil? (:coverage-score-delta out))))))
