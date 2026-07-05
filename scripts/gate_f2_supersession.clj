;; gate_f2_supersession.clj -- E-live-loop-3 Finding-2 standing gate.
;; PASS iff no mission in the registry whose status line contains a
;; supersession-class keyword (SUPERSEDED, ARCHIVED, ABANDONED) classifies
;; as a live status class (:active :open :partial :identify :unknown).
;; This is the stale-banner/selector guardrail (ledger section 9): a
;; mission whose doc says superseded but whose status-class is live is a
;; regression alarm.
;;
;; Run: cd /home/joe/code/futon2 && clojure -M scripts/gate_f2_supersession.clj
(ns gate-f2-supersession
  {:clj-kondo/config {:linters {:unused-namespace {:level :off}}}}
  (:require [clojure.string :as str]
            [futon2.aif.mission-registry :as mr]))

(def ^:private inactive-keywords
  #{"SUPERSEDED" "ARCHIVED" "ABANDONED"})

(def ^:private live-classes
  #{:active :open :partial :identify :unknown})

(defn -main []
  (let [missions (:missions (mr/load-missions))
        offenders (for [m missions
                        :let [status-line (str (:status-line m ""))
                              upper (str/upper-case status-line)
                              lead (-> upper (str/replace #"^[\s>*#`_~-]+" "") str/trim)
                              head (or (re-find #"[A-Z][A-Z-]*" lead) "")
                              status-class (:status-class m)]
                        :when (and (some #(str/starts-with? head %) inactive-keywords)
                                   (contains? live-classes status-class))]
                    {:id (:id m) :status-line status-line :status-class status-class})]
    (if (seq offenders)
      (do (doseq [o offenders]
            (println (format "  REGRESSION: %s status=%s class=%s (should be inactive)"
                             (:id o) (:status-line o) (:status-class o))))
          (println "GATE F2 FAIL --" (count offenders)
                   "mission(s) with superseded status classifying as live")
          (System/exit 1))
      (do (println "GATE F2 PASS -- no superseded mission classifies as live")
          (System/exit 0)))))

(-main)
