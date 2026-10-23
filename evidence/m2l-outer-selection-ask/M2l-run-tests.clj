(require '[futon2.aif.full-loop-runner :as runner]
         '[clojure.test :as test]
         'futon2.aif.m2l-outer-selection-ask-test
         'futon2.aif.click-interpretation-ask-test)
;; Isolated worktree verification: compare the loaded runner with this checkout,
;; not the different canonical production checkout. No serving JVM interaction.
(with-redefs-fn {#'runner/canonical-runner-bytes
                 #(java.nio.file.Files/readAllBytes
                   (java.nio.file.Paths/get "src/futon2/aif/full_loop_runner.clj" (make-array String 0)))}
  #(let [r (test/run-tests 'futon2.aif.m2l-outer-selection-ask-test
                           'futon2.aif.click-interpretation-ask-test)]
     (when (pos? (+ (:fail r) (:error r))) (throw (ex-info "Tests failed" r)))))
(shutdown-agents)
