(ns witnesses.node-witness-test
  (:require [clojure.test :refer [deftest is run-tests]]
            [clojure.java.io :as io]
            [witnesses.node-witness :as n]))

;; Fixtures exercise receipt resolution, not Lean execution. They are never
;; published as proofs. Source bytes are the actual F8 observation module.
(defn fixture []
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                     "node-witness-control-" (make-array java.nio.file.attribute.FileAttribute 0)))
        root (.getAbsolutePath dir)
        put (fn [path value]
              (let [f (io/file dir path)]
                (spit f (if (string? value) value (pr-str value)))
                {:repo "fixture" :path path :sha256 (n/sha256 (java.nio.file.Files/readAllBytes (.toPath f)))}))
        artifact (merge (put "Witness.lean" (slurp "/home/joe/code/mathlib4/DarkTower/WarMachine/MachineObservationWitness.lean"))
                        {:module "DarkTower.WarMachine.MachineObservationWitness"
                         :declaration "DarkTower.WarMachine.MachineObservationWitness.emptyObservationHasFourteenZeros"})
        subject (merge (put "Subject.lean" (slurp "/home/joe/code/mathlib4/DarkTower/WarMachine/MachineObservation.lean"))
                       {:declaration "DarkTower.WarMachine.MachineObservation.machineObservation"})
        _ (put "equations.edn" {:equations [{:id :observe :node :R2 :defines :o :lean "machineObservation"}]})
        _ (put "nodes.edn" {:nodes [{:node "R2"}]})
        _ (put "fundamentals.edn" {:schema :test-cap-authority})
        transcript (put "transcript.txt" "SYNTHETIC gate control: not a Lean execution receipt")
        checker (put "checker.txt" "Synthetic checker context")
        toolchain (put "toolchain.txt" "Synthetic toolchain context")
        census (assoc (put "census.edn"
                           {:record {:declarations
                                     {(:declaration artifact)
                                      {:source (select-keys artifact [:repo :path :sha256])
                                       :kind :theorem :module (:module artifact)
                                       :proposition "reference projection equals fourteen zeroes"
                                       :axioms ["propext" "Classical.choice" "Quot.sound"]}
                                      (:declaration subject)
                                      {:source (select-keys subject [:repo :path :sha256]) :kind :def}}}})
                      :selector [:record])
        w {:schema :wm/node-witness-v1 :id "control-observe-v1" :status :admitted
           :node :R2 :equation :observe :quantity :o :declaration "machineObservation"
           :kind :lean-witness :artifact artifact :subject-artifact subject
           :claim "The empty reference input has fourteen zero coordinates."
           :polarity :supports :scope :reference-model-empty-input :as-of "2026-09-12"}
        receipt {:subject (n/subject w) :executed? true :exit 0 :result :passed
                 :command "synthetic control only" :typecheck :passed :axiom-check :passed
                 :dependencies [artifact subject] :transcript transcript
                 :declaration-census census :import-closure [subject]
                 :toolchain toolchain :checker checker}
        vloc (assoc (put "verification.edn" {:record receipt}) :selector [:record])
        review {:subject (n/subject w) :verdict :approved :reviewer "synthetic test reviewer"
                :verification vloc :dependencies [artifact subject vloc]}
        rloc (assoc (put "review.edn" {:record review}) :selector [:record])]
    {:root root :put put :receipt receipt :review review
     :w (assoc w :verification {:status :verified :receipt vloc}
               :review {:status :verified :receipt rloc})
     :authorities {:roots {"fixture" root} :equations ["fixture" "equations.edn"]
                   :nodes ["fixture" "nodes.edn"] :fundamentals ["fixture" "fundamentals.edn"]}}))
(defn validate [f w]
  (n/validate! (n/context {"fixture" (:root f)}) (:authorities f) ["machineObservation"] w))
(defn failure [f w]
  (try (validate f w) nil
       (catch clojure.lang.ExceptionInfo e (:error (ex-data e)))))
(defn verification [f w r]
  (assoc-in w [:verification :receipt]
            (assoc ((:put f) "verification.edn" {:record r}) :selector [:record])))
(defn endorsed [f w r]
  (let [w (verification f w (assoc r :subject (n/subject w)))
        review (assoc (:review f) :subject (n/subject w)
                      :verification (get-in w [:verification :receipt])
                      :dependencies [(:artifact w) (:subject-artifact w)
                                     (get-in w [:verification :receipt])])]
    (assoc-in w [:review :receipt]
              (assoc ((:put f) "review.edn" {:record review}) :selector [:record]))))

(deftest positive-and-kind-limits
  (let [f (fixture) w (:w f)]
    (is (= :verified-binding (:status (validate f w))))
    (is (= :exact-formal-proposition (:maximum-claim (validate f w))))
    (let [without-id (endorsed f (dissoc w :id) (:receipt f))]
      (is (= :verified-binding (:status (validate f without-id)))))
    (let [f (fixture) w (:w f) refutation (assoc w :polarity :refutes)
          refutation (endorsed f refutation (:receipt f))]
      (is (= :refutes (:polarity (validate f refutation)))))))
(deftest required-refusals
  (doseq [[label mutate expected]
          [[:nil-receipt #(assoc-in % [:verification :receipt] nil) :node-witness-verification-missing]
           [:nil-review #(assoc-in % [:review :receipt] nil) :node-witness-review-missing]
           [:bad-node #(assoc % :node :R7) :node-witness-subject-mismatch]
           [:bad-equation #(assoc % :equation :precision) :node-witness-subject-mismatch]
           [:bad-quantity #(assoc % :quantity :Pi) :node-witness-subject-mismatch]
           [:bad-owner #(assoc % :declaration "missing") :node-witness-declaration-missing]
           [:bad-kind #(assoc % :kind :source-file) :node-witness-schema-invalid]
           [:traversal #(assoc-in % [:artifact :path] "../outside") :node-witness-path-invalid]
           [:absolute #(assoc-in % [:artifact :path] "/etc/passwd") :node-witness-path-invalid]
           [:bad-repo #(assoc-in % [:artifact :repo] "unknown") :node-witness-path-invalid]]]
    (let [f (fixture) w (mutate (:w f))]
      ;; Keep the receipt's claimed semantic identity in step, so each control
      ;; reaches the intended boundary instead of failing at an earlier one.
      (is (= expected (failure f (if (#{:nil-receipt :nil-review} label) w (endorsed f w (:receipt f))))) (name label)))))
(deftest verification-controls
  (doseq [[label mutate expected]
          [[:nil-exit #(assoc % :exit nil) :node-witness-verification-missing]
           [:failed-exit #(assoc % :exit 1) :node-witness-verification-missing]
           [:not-executed #(assoc % :executed? false) :node-witness-verification-missing]
           [:no-readback #(assoc-in % [:transcript :path] "missing.txt") :node-witness-artifact-missing]
           [:wrong-import-pin #(assoc-in % [:import-closure 0 :sha256] (apply str (repeat 64 "0"))) :node-witness-pin-mismatch]
           [:missing-context #(assoc % :dependencies []) :node-witness-verification-missing]
           [:missing-toolchain #(assoc % :toolchain nil) :node-witness-pin-missing]]]
    (let [f (fixture)]
      (is (= expected (failure f (endorsed f (:w f) (mutate (:receipt f))))) (name label)))))
(deftest stale-bytes-and-declaration-controls
  (let [f (fixture)]
    (spit (io/file (:root f) "Witness.lean") "x" :append true)
    (is (= :node-witness-pin-mismatch (failure f (:w f)))))
  (let [f (fixture)]
    (.delete (io/file (:root f) "Witness.lean"))
    (is (= :node-witness-artifact-missing (failure f (:w f)))))
  (doseq [declaration ["DarkTower.WarMachine.MachineObservationWitness.emptyObservationHasFifteenZeros"
                       "DarkTower.WarMachine.MachineObservationWitness.onlyInComment"]]
    (let [f (fixture)
          w (assoc-in (:w f) [:artifact :declaration] declaration)
          r (assoc (:receipt f) :dependencies [(:artifact w) (:subject-artifact w)])]
      (is (= :node-witness-declaration-missing (failure f (endorsed f w r)))))))
(deftest pending-cap-and-licence-controls
  (let [f (fixture) pending (assoc (:w f) :status :proposed :verification {:receipt nil} :review {:receipt nil})
        p (validate f pending) a (validate f (:w f))]
    (is (= :pending (:status p)))
    (is (empty? (n/witnessed-bindings [p] {:node :R2 :preceding-rung 3 :fundamentals-cap 4})))
    (is (empty? (n/witnessed-bindings [a] {:node :R2 :preceding-rung 1 :fundamentals-cap 4})))
    (is (empty? (n/witnessed-bindings [a] {:node :R2 :preceding-rung 3 :fundamentals-cap 3})))
    (is (empty? (n/witnessed-bindings [a] {:node :R2 :readiness :witnessed :licence "exists"})))
    (is (= [a] (n/witnessed-bindings [a] {:node :R2 :preceding-rung 3 :fundamentals-cap 4})))
    (is (empty? (n/witnessed-bindings [a] {:node :R7 :preceding-rung 3 :fundamentals-cap 4})))))

(deftest commissioning-controls
  (doseq [observed? [true false]]
    (let [f (fixture) mechanism (:subject-artifact (:w f))
          case-record (cond-> {:case :empty :expected [0] :executed? true :result :passed
                               :production-entrypoint "observation/observe" :mechanisms [mechanism]}
                        observed? (assoc :observed [0]))
          artifact (assoc ((:put f) "case.edn" {:case case-record}) :selector [:case])
          w (assoc (:w f) :kind :commissioning :artifact artifact)
          r (assoc (:receipt f) :case :empty :production-entrypoint "observation/observe"
                   :mechanisms [mechanism] :dependencies [artifact mechanism])
          w (endorsed f w r)]
      (if observed?
        (is (= :named-induced-behavior (:maximum-claim (validate f w))))
        (is (= :node-witness-verification-missing (failure f w)))))))
(deftest replay-controls
  (doseq [missing? [false true]]
    (let [f (fixture) capture (assoc ((:put f) "capture.edn" {:input [0]}) :selector [:input])
          assertion (assoc ((:put f) "assertion.edn" {:check {:passed? true :capture capture :scope :captured-input}}) :selector [:check])
          w (assoc (:w f) :kind :replay-pin :artifact capture :scope :captured-input)
          r (assoc (:receipt f) :capture capture :assertion assertion
                   :source-identity {:job "retained-test-job"}
                   :dependencies [capture (:subject-artifact w)])
          w (endorsed f w r)]
      (when missing? (.delete (io/file (:root f) "capture.edn")))
      (if missing?
        (is (= :node-witness-artifact-missing (failure f w)))
        (is (= :asserted-captured-input-behavior (:maximum-claim (validate f w))))))))
(deftest single-read-and-single-edn-form
  (let [f (fixture) ctx (n/context {"fixture" (:root f)}) loc (:artifact (:w f))]
    (n/resolve! ctx loc "cache")
    (spit (io/file (:root f) "Witness.lean") "changed")
    (is (= (:sha256 loc) (:sha256 (n/resolve! ctx loc "cache")))))
  (is (thrown? Exception (n/read-edn "{} {}"))))

(defn -main [& _]
  (let [r (run-tests 'witnesses.node-witness-test)]
    (System/exit (if (zero? (+ (:fail r) (:error r))) 0 1))))
(when (= *file* (System/getProperty "babashka.file")) (-main))
