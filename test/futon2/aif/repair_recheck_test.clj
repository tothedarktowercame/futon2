(ns futon2.aif.repair-recheck-test
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.observation-checks :as checks]
            [futon2.aif.repair-recheck :as recheck]))

(defn utf8-bytes [s] (.getBytes s java.nio.charset.StandardCharsets/UTF_8))

(def passing
  (str "{:schema :wm/eig-held-out-calibration-v1\n"
       " :status :passing\n"
       " :claims {:calibration-evidence-present? true}\n"
       " :disposition\nCALIBRATION-EVIDENCE-PASSING\n}"))

(deftest passing-source-produces-cleared-observation
  (let [record (recheck/observe "calibration.edn" (utf8-bytes passing) "2026-09-23T20:00:00Z")
        rendered (recheck/render record)]
    (is (= :cleared (:observed record)))
    (is (= recheck/cleared-disposition (:disposition record)))
    (is (every? true? (vals (:checks record))))
    (is (checks/decl-present? rendered "RECHECK-DISPOSITION-CLEARED"))
    (is (= record (edn/read-string rendered)))))

(deftest incomplete-or-failing-source-remains-present
  (doseq [text ["not edn"
                (str/replace passing ":passing" ":failing")
                (str/replace passing "CALIBRATION-EVIDENCE-PASSING"
                             "CALIBRATION-EVIDENCE-FAILING")]]
    (let [record (recheck/observe "calibration.edn" (utf8-bytes text) "2026-09-23T20:00:00Z")
          rendered (recheck/render record)]
      (is (= :present (:observed record)))
      (is (= recheck/present-disposition (:disposition record)))
      (is (not (checks/decl-present? rendered "RECHECK-DISPOSITION-CLEARED"))))))
