(ns futon2.aif.outer-selection-history-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.outer-select :as select]
            [futon2.aif.previous-run :as previous]
            [futon2.aif.outer-menu-wiring-test :as fixture]
            [futon2.aif.outer-state :as state]
            [wm-run-facts :as export]))

(defn selection []
  (let [m (state/load-makeup "data/wm-learning/outer-makeup.edn")]
    (select/select m (state/init-state m) fixture/observation)))

(deftest choice-digest-is-scoped-to-compared-classes
  (let [s (selection) d #(get (select/choice-input-digest % "WM") :sha256)]
    (is (= (d s) (d (assoc-in s [:field-facts :admitted "unused" :cone] 500))))
    (is (= (d s) (d (assoc-in s [:construction :reading] "changed-pass"))))
    (is (= (d s) (d (assoc-in s [:construction :graph] "changed-input"))))
    (doseq [changed [(assoc-in s [:classes 0 :pick] "other")
                     (update-in s [:classes 0 :g-units] inc)
                     (assoc-in s [:classes 0 :prediction-digest] "changed")
                     (assoc s :forbidden [{:target "x" :reason :refused :pin "p"}])]]
      (is (not= (d s) (d changed))))
    (is (not= (d s) (:sha256 (select/choice-input-digest s "other-focus"))))))

(deftest previous-selection-uses-sealed-stream-not-filenames
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory "outer-history" (make-array java.nio.file.attribute.FileAttribute 0)))
        s (selection)
        terminal (previous/terminal {:run-id "first" :started-at "2026-11-01T00:00:00Z"})
        record {:run/id "first" :outer/selection s :decision {:status :ran}
                :q6 {:terminal terminal}}
        file (io/file dir "retained.edn")]
    (is (= :no-earlier-started-run (:reason (previous/selection-predecessor dir "next" "2026-11-01T00:00:01Z"))))
    (previous/start! dir "first" "2026-11-01T00:00:00Z")
    (spit file (pr-str record))
    (previous/close! dir terminal file)
    (let [p (previous/selection-predecessor dir "next" "2026-11-01T00:00:01Z")]
      (is (= :present (:status p)))
      (is (= (:chosen-class s) (:chosen-class p)))
      (is (= :changed (:outcome p)))
      (is (= (get-in s [:choice-input-digest :sha256]) (:choice-input-digest p)))
      (is (= :refused-selection-left-no-record (get-in p [:retention-gap :reason]))))
    (spit file (pr-str (assoc record :run/id "foreign")))
    (is (= :previous-selection-record-identity-mismatch
           (:reason (previous/selection-predecessor dir "next" "2026-11-01T00:00:01Z"))))))

(deftest class-terms-and-history-project-without-work-outcome-confusion
  (let [s (selection)
        p (previous/selection-from-record {:run/id "first" :outer/selection s :decision {}})
        project #(get (export/selection-facts-for-record {:outer/selection %} "fixture"
                         {:open-missions #{} :open-excursions #{} :open-tickets #{} :patterns #{} :seats #{}} {}) :facts)
        f (project (assoc s :previous p))]
    (is (= (count (:classes s)) (count (f "policyPreferences")) (f "policiesWithRiskTerm")))
    (is (= (:chosen-class s) (get-in f ["previousChoice" "cascade"])))
    (is (= (get-in s [:selection :selected :target]) (get-in f ["previousChoice" "target"])))
    (is (= "changed" (f "previousOutcome")))
    (is (= :selection-work-entry-unretained
           (get-in (previous/selection-from-record {:outer/selection s}) [:outcome :reason])))
    (is (str/includes? (get-in (project (assoc s :previous {:status :absent :reason :refused-selection-left-no-record}))
                              ["previousOutcome" "not-recomputable"]) "refused-selection-left-no-record"))
    (is (= (f "currentInputDigest") (f "previousInputDigest")))
    (is (contains? (get (project (update-in s [:classes 0] dissoc :terms)) "policyPreferences") "not-recomputable"))
    (is (= :refused (:outcome (previous/selection-from-record
                              {:outer/selection (assoc s :outcome {:status :refused :kind :tie})}))))))
