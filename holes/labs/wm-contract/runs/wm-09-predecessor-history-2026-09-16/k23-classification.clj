(require '[futon2.aif.receipt-construction :as construction]
         '[futon2.aif.receipt-construction-test :as history]
         '[futon2.aif.interpretation-job-test :as caller]
         '[futon2.aif.hermetic-repair-fixture :as hermetic]
         '[futon2.aif.full-loop-runner-test :as runner-fixture])
(import '[java.nio.file Files] '[java.nio.file.attribute FileAttribute])

(hermetic/with-hermetic-stores
 (fn []
   (runner-fixture/with-hermetic-traces
    (fn []
      (let [temp (.toFile (Files/createTempDirectory "wm09-classification" (make-array FileAttribute 0)))]
        (try
          (let [latest (history/history-fixture! temp :modern "2026-09-15T11:00:00Z" "2026-09-15T11:01:00Z")
                current (assoc-in (:identity latest) [:occurrence :action-at] "2026-09-15T12:00:00Z")]
            (spit (:close-file latest) "{")
            (doseq [[label action expected-kind expected-class]
                    [[:typed-discovery #(construction/previous! current [temp])
                      :interpretation/invalid-receipt :environmental-hold]
                     [:code-fault #(throw (NullPointerException. "controlled code fault"))
                      :untyped-failure :machine-failure]]]
              (let [{:keys [result calls constructors]}
                    (with-redefs [construction/construct! (fn [& _] (action))]
                      (caller/run-case :receipt :valid))
                    observed {:case label :failure-kind (get-in result [:data :failure-kind])
                              :repair-class (get-in result [:data :repair-obligation :repair/class])
                              :calls calls :legacy-constructor-calls constructors}]
                (assert (= expected-kind (:failure-kind observed)))
                (assert (= expected-class (:repair-class observed)))
                (assert (= ["interpreter"] calls))
                (assert (zero? constructors))
                (prn observed))))
          (finally (doseq [file (reverse (file-seq temp))] (Files/delete (.toPath file))))))))))
