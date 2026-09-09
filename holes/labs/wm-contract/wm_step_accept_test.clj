;; bb -cp src holes/labs/wm-contract/wm_step_accept_test.clj
;; Subprocesses use temporary stores only: no tick, live lock or ledger writes.
(ns wm-step-accept-test
  (:require [babashka.process :as process]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is run-tests]]
            [futon2.aif.step-acceptance :as acceptance]))

(def root (.getCanonicalPath (io/file ".")))
(def lab (str root "/holes/labs/wm-contract"))
(defn temp-dir [] (.toFile (java.nio.file.Files/createTempDirectory
                           "wm-accept-test" (make-array java.nio.file.attribute.FileAttribute 0))))
(defn run-command [args opts]
  @(process/process args (merge {:out :string :err :string} opts)))

(deftest first-pass-capture-precedes-admission
  (let [dir (temp-dir) artifact (io/file dir "untracked.edn")
        capture (io/file dir "verdicts.edn") ledger (io/file dir "ledger.edn")
        _ (spit artifact "{:fixture true}")
        args ["bb" (str lab "/run_era_ledger.bb") "--deposit"
              "--ledger" (str ledger) "--run-id" "run" "--check-id" ":contract-pin"
              "--verdict" ":red" "--artifact" (str artifact) "--author" "test"]
        plain (run-command args {})
        captured (run-command args {:extra-env {"FUTON_WM_VERDICT_CAPTURE" (str capture)}})
        row (edn/read-string (slurp capture))]
    (is (= 1 (:exit plain) (:exit captured)))
    (is (= (:out plain) (:out captured)))
    (is (= :red (:row/verdict row)))
    (is (not (.exists ledger)))
    (is (thrown? clojure.lang.ExceptionInfo
                 (acceptance/advance-pin {:pin/generation 0} [row]
                                        "step" "run" "store" nil "now")))))

(deftest actual-accept-pin-arms
  (doseq [[verdict reason accepted?] [[":red" nil false]
                                     [":red" "reviewed known red" true]
                                     [":green" nil true]]]
    (let [dir (temp-dir) work (io/file dir "work") step (io/file work "s1")
          pin (io/file work "pin/pin.edn") sandbox (io/file work "sandbox/wm-trace/a.edn")
          fake-lab (io/file dir "lab")
          _ (doseq [f [pin sandbox (io/file step "step.edn") (io/file fake-lab "run-era-ledger.edn")]]
              (io/make-parents f))
          _ (spit pin "{:pin/generation 4 :pin/accepted-steps []}")
          _ (spit sandbox "{}")
          _ (spit (io/file step "step.edn") "{:step/tick-exit 0 :step/run-id \"tick\"}")
          _ (spit (io/file step "decision-records.edn") "{}")
          _ (spit (io/file work ".last-step") (str step))
          _ (spit (io/file fake-lab "run-era-ledger.edn") "{:rows []}")
          stub (io/file dir "noop.bb") _ (spit stub "nil")
          script (str "source \"$1/wm_step.sh\"\n"
                      "LAB=\"$2\"; OBSERVE=\"$3\"; RECORDS=\"$3\"\n"
                      "cmd_reset() { :; }\n"
                      "VERDICT=\"$6\"\n"
                      "cmd_battery() { printf '{:row/run-id \"run\" :row/check-id :contract-pin :row/verdict %s}\\n' \"$VERDICT\" > \"$LAB/runs/run/check-verdicts.edn\"; }\n"
                      "if [ -n \"$7\" ]; then cmd_accept \"$4\" \"$5\" run --override-red \"$7\"; else cmd_accept \"$4\" \"$5\" run; fi")
          result (run-command ["bash" "-c" script "test" lab (str fake-lab) (str stub)
                               (str work) (str step) verdict (or reason "")] {})
          p (edn/read-string (slurp pin))]
      (is (= accepted? (zero? (:exit result))) (:err result))
      (is (= (if accepted? 5 4) (:pin/generation p)))
      (is (= reason (get-in p [:pin/accepted-steps 0 :override-red :reason]))))))

(let [r (run-tests)] (System/exit (+ (:fail r) (:error r))))
