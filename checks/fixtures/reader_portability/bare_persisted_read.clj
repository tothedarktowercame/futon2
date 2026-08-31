(ns checks.fixtures.reader-portability.bare-persisted-read)

(def persisted-value
  (read-string (slurp "someone-elses-record.edn")))
