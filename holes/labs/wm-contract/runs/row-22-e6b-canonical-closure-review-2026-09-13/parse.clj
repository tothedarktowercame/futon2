(require '[clojure.edn :as edn] '[clojure.java.io :as io])
(with-open [r (java.io.PushbackReader. (io/reader "holes/labs/wm-contract/e6b-canonical-replay-closure-2026-09-13.edn"))]
  (let [e (Object.) x (edn/read {:eof e} r)]
    (assert (map? x))
    (assert (identical? e (edn/read {:eof e} r)))
    (println :one-form-ok)))
