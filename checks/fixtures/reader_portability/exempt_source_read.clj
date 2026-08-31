(ns checks.fixtures.reader-portability.exempt-source-read)

(def source-form
  ;; reader-portability: allow-source-read reason=deliberate-clojure-source-analysis
  (read-string (slurp "deliberate-source.clj")))
