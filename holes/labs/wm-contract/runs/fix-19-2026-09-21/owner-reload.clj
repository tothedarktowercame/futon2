; OWNER ONLY: generated, not executed by codex-11. Refuses moved sources.
(do
  (doseq [[resource canonical digest] '[["futon2/aif/cascade_selection.clj" "/home/joe/code/futon2/src/futon2/aif/cascade_selection.clj" "c04c539336bd6e9e38f6923d6fc658a56dbb0d438fa5045f9e26c0f8c83c9d71"] ["futon2/aif/cascade_model_manifest.clj" "/home/joe/code/futon2/src/futon2/aif/cascade_model_manifest.clj" "25964adb705d8206a5ab7d163a262920a2a286c0bd3d9ebcd5818888e8f067d2"] ["futon2/aif/policy.clj" "/home/joe/code/futon2/src/futon2/aif/policy.clj" "0f4998512d80b86ef2083ef12c599485ea61003a35702d0c2674bf061b505329"] ["futon2/aif/live_c.clj" "/home/joe/code/futon2/src/futon2/aif/live_c.clj" "c469478233fc99ccd67b47c8e7564ccdc2915508e13ed05f62e02c4fa1257f01"] ["futon2/aif/d_predecessor_task_authority.clj" "/home/joe/code/futon2/src/futon2/aif/d_predecessor_task_authority.clj" "9a4637b5fea66a4a1dde383f817031ff0d44d5198a21a2a8e0b09706600e8c9a"] ["futon2/aif/cascade_habit_store.clj" "/home/joe/code/futon2/src/futon2/aif/cascade_habit_store.clj" "89553de7f66aa0dedb2d04773bf7ce9b314ea8f1e61351c079738550fe1d050b"] ["futon2/aif/cascade_habit_reinforcement.clj" "/home/joe/code/futon2/src/futon2/aif/cascade_habit_reinforcement.clj" "ff15440ec7faf44633047391bdbea5e29b5cc9c794dcc862c38f395866a6b360"] ["futon2/aif/cascade_plan.clj" "/home/joe/code/futon2/src/futon2/aif/cascade_plan.clj" "7f0eca4e5babca4a359d685357e55f9d93e50fb53379c3cca66c518e53a7347a"] ["futon2/aif/job_text_retention.clj" "/home/joe/code/futon2/src/futon2/aif/job_text_retention.clj" "e37f6790b657347c1c35abfc397a31e8931abb210d6557d50be981d88e935156"] ["futon2/aif/scan_report.clj" "/home/joe/code/futon2/src/futon2/aif/scan_report.clj" "73017ccdf949cc4fe6a4cf4f1608ed02cb38e8e7e2f03199a77ccd13408d8242"] ["futon2/aif/token_outcome.clj" "/home/joe/code/futon2/src/futon2/aif/token_outcome.clj" "ed6c5e5ea0c65cb201824df9f72a69cfc9e7fb9df190aa4b5131c0dc67bad83a"] ["futon2/aif/trace.clj" "/home/joe/code/futon2/src/futon2/aif/trace.clj" "53e16b43d09a04b2f16c793f4c26ebe718c48c9d27fa3f6f842fa2deaa232465"] ["futon2/aif/mission_hole_wants.clj" "/home/joe/code/futon2/src/futon2/aif/mission_hole_wants.clj" "7ca6dc0493f44d04ef602a6e53ec04720f5b381baa691112a7c65229f997ba8f"] ["futon2/report/war_machine.clj" "/home/joe/code/futon2/scripts/futon2/report/war_machine.clj" "c53970bf6851561f13b03d7643804cbc73a346aabfae995c58df019983b1b457"] ["futon2/aif/full_loop_runner.clj" "/home/joe/code/futon2/src/futon2/aif/full_loop_runner.clj" "bc16a9823a3defcfffdcbb4f68c0104d89a38353345f879b4f9a8e9bede387b1"] ["futon2/aif/interpretation_construction.clj" "/home/joe/code/futon2/src/futon2/aif/interpretation_construction.clj" "e81493bfb57a3313eb3cda09643e853a8daa11bfdcd2ba89d21626107e0760db"] ["futon2/aif/run_narrative.clj" "/home/joe/code/futon2/src/futon2/aif/run_narrative.clj" "dc9e5273101e51a18e0b96bad11d01c4aa28a071842bdc75911d2cf087bf8a98"]]]
    (let [url (.getResource (clojure.lang.RT/baseLoader) resource)]
      (assert (and url (= "file" (.getProtocol url))) [resource :non-file-resource])
      (assert (= canonical (.getCanonicalPath (java.io.File. (.toURI url)))) [resource :noncanonical])
      (with-open [in (.openStream url)]
        (let [actual (apply str (map #(format "%02x" (bit-and 255 %))
                          (.digest (java.security.MessageDigest/getInstance "SHA-256") (.readAllBytes in))))]
          (assert (= digest actual) [resource :disk-moved-rerun-probe])))))
  (require 'futon2.aif.cascade-selection :reload)
  (require 'futon2.aif.cascade-model-manifest :reload)
  (require 'futon2.aif.policy :reload)
  (require 'futon2.aif.live-c :reload)
  (require 'futon2.aif.d-predecessor-task-authority :reload)
  (require 'futon2.aif.cascade-habit-store :reload)
  (require 'futon2.aif.cascade-habit-reinforcement :reload)
  (require 'futon2.aif.cascade-plan :reload)
  (require 'futon2.aif.job-text-retention :reload)
  (require 'futon2.aif.scan-report :reload)
  (require 'futon2.aif.token-outcome :reload)
  (require 'futon2.aif.trace :reload)
  (require 'futon2.aif.mission-hole-wants :reload)
  (require 'futon2.report.war-machine :reload)
  (require 'futon2.aif.full-loop-runner :reload)
  (require 'futon2.aif.interpretation-construction :reload)
  (require 'futon2.aif.run-narrative :reload)
  :reload-forms-completed)
