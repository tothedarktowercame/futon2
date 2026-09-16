;; claude-3: controls specific to the 276a8dc1 delta (refusal kind for equal-time latest matching history).
(require '[clojure.java.io :as io]
         '[futon2.aif.receipt-construction :as construction]
         '[futon2.aif.receipt-construction-test :as t])
(import '[java.nio.file Files] '[java.nio.file.attribute FileAttribute])
(defn temp [] (.toFile (Files/createTempDirectory "c3-k9r3d" (make-array FileAttribute 0))))
(defn rm! [d] (doseq [f (reverse (file-seq d))] (Files/delete (.toPath f))))
(defn copy-attempt! [files dest] (.mkdirs dest) (doseq [f files] (spit (io/file dest (.getName f)) (slurp f))))
(defn run [f] (try (select-keys (f) [:admission-reason])
                   (catch clojure.lang.ExceptionInfo e
                     (let [d (ex-data e)] {:refusal (:construction/refusal d) :identical? (:identical-checkpoint-sets? d)
                                           :files (count (:files d)) :records (count (:records d))}))))
(def current {:occurrence {:action/value {:target "M-history"} :action-at "2026-09-15T12:00:00Z"}})
(defn relabel! [files cohort]
  ;; same producer shape under a different declared cohort (distinct identity, same close time)
  (doseq [f files] (t/rewrite-history! f #(assoc % :cohort/id cohort))))

;; D1: two DIFFERENT matching attempts closing at the same instant (distinct identities): ambiguity kind retained.
(let [d (temp)]
  (try
    (t/discovery-history! (io/file d "a") "M-history" false)
    (let [bfiles (t/discovery-history! (io/file d "b") "M-history" false)]
      (.renameTo (io/file d "b" "fixture") (io/file d "b" "other"))
      (relabel! (mapv #(io/file d "b" "other" "attempt-002" (.getName %)) bfiles) :other))
    (prn {:case :d1-equal-time-distinct-identities :result (run #(construction/previous! current [(io/file d "a") (io/file d "b")]))})
    (finally (rm! d))))

;; D2: identical copy pair PLUS a third distinct matching attempt at the same instant.
(let [d (temp)]
  (try
    (let [files (t/discovery-history! (io/file d "a") "M-history" false)]
      (copy-attempt! files (io/file d "a" "archives" "snapshot" "fixture" "attempt-002")))
    (let [bfiles (t/discovery-history! (io/file d "b") "M-history" false)]
      (.renameTo (io/file d "b" "fixture") (io/file d "b" "other"))
      (relabel! (mapv #(io/file d "b" "other" "attempt-002" (.getName %)) bfiles) :other))
    (prn {:case :d2-identical-pair-plus-distinct-equal-time :result (run #(construction/previous! current [(io/file d "a") (io/file d "b")]))})
    (finally (rm! d))))
(shutdown-agents)
