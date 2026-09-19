(ns futon2.aif.repair-occurrence-identity-test
  (:require [clojure.test :refer [deftest test-vars]]
            [futon2.aif.full-loop-runner-test :as runner-test]
            [futon2.aif.repair-obligation-test :as repair-test]))

(deftest occurrence-identity-controls
  ;; The registry accepts one explicit namespace. Keep this wrapper narrow and
  ;; make the commissioned controls themselves remain beside their established
  ;; store/runner fixtures.
  (test-vars [#'repair-test/occurrence-identity-publishes-once-and-retains-observations
              #'repair-test/occurrence-publication-refuses-id-and-payload-aliases-before-write
              #'repair-test/parallel-occurrence-publication-is-create-new-safe
              #'repair-test/distinct-occurrences-remain-visible-to-t8
              #'runner-test/initialization-containment-propagates-stable-occurrence-identity
              #'runner-test/initialization-retry-reuses-real-finding-and-appends-observation]))
