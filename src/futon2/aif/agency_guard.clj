(ns futon2.aif.agency-guard
  "A test JVM must not act on the production Agency (the serving JVM on
  :7070): no bells, whistles, parks, registrations or cancellations. The
  2026-10-10 offline replay dispatched a real interpretation-ask job
  (invoke-1791611767087-1281, D23) because its configuration fell back to the
  production default; the ticket publisher had the same escape (D21).
  Read-only GETs are not covered here."
  (:require [clojure.string :as str]
            [futon2.data-paths :as data-paths]))

(def production-bases #{"http://127.0.0.1:7070" "http://localhost:7070"})

(defn dispatch-url
  "BASE + PATH, refused in a test JVM when BASE is the production Agency."
  [base path]
  (let [base (str/replace (str base) #"/+$" "")]
    (when (and data-paths/test-mode? (contains? production-bases base))
      (throw (ex-info "A test JVM may not act on the production Agency"
                      {:kind :production-agency-in-test-mode :base base :path path})))
    (str base path)))
