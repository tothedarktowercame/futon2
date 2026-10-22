(require '[futon2.aif.full-loop-cohort :as cohort])
(import '[java.nio.file Files FileAlreadyExistsException] '[java.nio.file.attribute FileAttribute])
(let [dir (Files/createTempDirectory "wm-checkpoint-write-probe-" (make-array FileAttribute 0))
      path (.resolve dir "selection.edn")
      write! (deref #'cohort/write-new!)
      _ (write! path {:declared-effect {:record/digest "original"}})
      before (slurp (str path))
      refused? (try (write! path {:declared-effect {:record/digest "replacement"}}) false
                    (catch FileAlreadyExistsException _ true))
      after (slurp (str path))]
  (assert refused?)
  (assert (= before after))
  (prn {:overwrite-refused refused? :original-bytes-preserved (= before after)
        :scope :actual-private-writer-in-standalone-jvm
        :does-not-prove :protection-against-external-filesystem-writes})
  (Files/delete path)
  (Files/delete dir))
