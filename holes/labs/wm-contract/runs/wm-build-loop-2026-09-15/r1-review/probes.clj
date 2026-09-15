(require '[futon2.aif.work-target-store :as s]
         '[futon2.aif.work-target-store-test :as f])
(import '(java.nio.file Files)
        '(java.io IOException))
(def results (atom []))
(defn retain [case store]
  (let [r (s/read-store store)]
    (swap! results conj {:case case :result (select-keys r [:status :reason :head])}) r))

;; Boundary explicitly requested by review: only temps, no genesis.
;; Contrast absent lock metadata against valid protocol lock metadata.
(doseq [lock? [false true]
        temp ["INIT.edn.tmp" "snapshots/snapshot.tmp"]]
  (f/fixture
   (fn [store]
     (Files/createDirectory (:path store) (make-array java.nio.file.attribute.FileAttribute 0))
     (when lock? (s/read-store store))
     (when (= temp "snapshots/snapshot.tmp")
       (Files/createDirectory (f/p store "snapshots") (make-array java.nio.file.attribute.FileAttribute 0)))
     (spit (.toFile (f/p store temp)) "")
     (let [r (retain [:temps-only :valid-lock? lock? :file temp] store)]
       (assert (= (if lock? :pending-recovery :damaged) (:status r)))
       (assert (not (.exists (.toFile (f/p store "genesis.edn")))))))))

;; Genuine interrupted initialization, including incomplete HEAD bytes.
(doseq [content ["" "{" "{} {}"]]
  (f/fixture
   (fn [store]
     (binding [s/*failpoint* #(when (= :HEAD.edn-before-write %) (throw (IOException. "simulated stop")))]
       (f/establish store))
     (spit (.toFile (f/p store "HEAD.edn.tmp")) content)
     (assert (= :initialization-incomplete
                (:status (retain [:initialization-with-head-temp content] store)))))))

;; Fixed committed names cannot be hidden by renaming to .tmp.
(doseq [[filename expected] [["HEAD.edn" :missing-head]
                            ["snapshots/1.edn" :missing-head-snapshot]]]
  (f/fixture
   (fn [store]
     (f/establish store)
     (f/append! store "one" {})
     (Files/move (f/p store filename) (f/p store (str filename ".tmp"))
                 (make-array java.nio.file.CopyOption 0))
     (let [r (retain [:committed-renamed-to-temp filename] store)]
       (assert (= :damaged (:status r)))
       (assert (= expected (:reason r)))))))

;; Final prepared snapshot remains visible with and without temps.
(f/fixture
 (fn [store]
   (f/establish store)
   (f/append! store "one" {})
   (binding [s/*failpoint* #(when (= :snapshot-after-directory-force %) (throw (IOException. "simulated stop")))]
     (f/append! store "two" {}))
   ;; Isolate the final-file evidence. This is a test mutation, not recovery.
   (Files/delete (f/p store "PENDING.edn"))
   (assert (= :pending-recovery (:status (retain :prepared-final-alone store))))
   (spit (.toFile (f/p store "HEAD.edn.tmp")) "")
   (assert (= :pending-recovery (:status (retain :prepared-final-and-temp store))))
   (spit (.toFile (f/p store "snapshots/2.edn")) "{")
   (assert (= :parse-failure (:reason (retain :malformed-prepared-final-and-temp store))))))

;; Invalid committed bytes/identity still beat temp recovery.
(doseq [[file content expected] [["snapshots/1.edn" "{" :parse-failure]
                               ["HEAD.edn" "{} {}" :parse-failure]
                               ["declaration.edn" "{}" :declaration-hash-mismatch]]]
  (f/fixture
   (fn [store]
     (f/establish store)
     (f/append! store "one" {})
     (spit (.toFile (f/p store "HEAD.edn.tmp")) "")
     (spit (.toFile (f/p store file)) content)
     (let [r (retain [:committed-damage-and-temp file] store)]
       (assert (= :damaged (:status r)))
       (assert (= expected (:reason r)))))))
(prn {:probes @results :count (count @results)})
(shutdown-agents)
