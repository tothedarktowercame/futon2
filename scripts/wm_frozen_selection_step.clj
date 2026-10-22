(ns wm-frozen-selection-step
  "Freeze and replay one successful production selection boundary. No dispatch."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [futon2.aif.outer-policy-objective :as objective]
            [futon2.aif.outer-policy-evidence :as policy-evidence]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.wm.family-selection :as family]
            [futon2.aif.selected-outer-cascade-policy :as handoff])
  (:import [java.security MessageDigest]
           [java.util.zip GZIPInputStream GZIPOutputStream]))

(defn sha [text]
  (apply str (map #(format "%02x" (bit-and 255 %))
                 (.digest (MessageDigest/getInstance "SHA-256")
                          (.getBytes ^String text "UTF-8")))))

(defn read-gzip [path]
  (with-open [in (GZIPInputStream. (io/input-stream path))]
    (edn/read-string (slurp in :encoding "UTF-8"))))

(defn write-gzip! [path value]
  (io/make-parents path)
  (with-open [out (GZIPOutputStream. (io/output-stream path))]
    (.write out (.getBytes (pr-str value) "UTF-8"))))

(defn require! [condition message detail]
  (when-not condition (throw (ex-info message detail))))

(defn score-rows [receipt]
  (mapv #(select-keys % [:policy-id :target-id :cascade-digest :terms :posterior])
        (:policies receipt)))

(defn freeze! [run-path frozen-path]
  (let [text (slurp run-path)
        run (edn/read-string text)
        carrier (get-in run [:outer-task-selection :policy :selected-outer-cascade-policy])
        receipt (:objective-receipt carrier)
        input {:schema :wm/frozen-selection-input-v1
               :source {:run-id (:run/id run) :sha256 (sha text)}
               :menu (:sealed-menu carrier)
               :evidence (:evidence-carrier carrier)
               :draw (get-in receipt [:draw :u])
               :expected-action (:action receipt)
               :expected-scores (score-rows receipt)}]
    (require! (= :selected (:status receipt)) "Source did not select useful work"
              {:status (:status receipt)})
    (require! (number? (:draw input)) "This fixture requires its recorded tie draw" {})
    (write-gzip! frozen-path input)
    (prn {:status :frozen :input frozen-path :source (:source input)})))

(defn replay! [frozen-path output-dir]
  (let [input (read-gzip frozen-path)
        _ (require! (= :wm/frozen-selection-input-v1 (:schema input)) "Wrong input schema" {})
        ;; A missing draw must never introduce a fresh random choice.
        _ (require! (number? (:draw input)) "Recorded draw missing" {})
        original-menu (:menu input)
        original-evidence (:evidence input)
        _ (require! (nil? (policy-evidence/verify-evidence original-evidence))
                    "Frozen evidence failed verification" {})
        _ (require! (= (:menu-pin original-menu) (:menu-pin original-evidence))
                    "Recorded input references disagree" {})
        ;; Import into a NEW offline envelope; never claim the historical
        ;; serialization seal verified. The selected values remain exact.
        menu (assoc original-menu :menu-pin (family/sealed-menu-pin original-menu))
        ev (assoc original-evidence :menu-pin (:menu-pin menu))
        ev (assoc ev :evidence-pin (policy-evidence/evidence-pin ev))
        receipt (objective/evaluate ev menu (:draw input))
        _ (require! (= :selected (:status receipt)) "Section did not select useful work"
                    (select-keys receipt [:status :reason]))
        _ (require! (= (:expected-action input) (:action receipt)) "Selected task changed"
                    {:expected (:expected-action input) :actual (:action receipt)})
        _ (require! (= (:expected-scores input) (score-rows receipt)) "Policy scores changed" {})
        carrier (handoff/seal menu ev receipt)
        result (handoff/decision carrier)
        _ (require! (= :selected (:status result)) "Executable handoff did not succeed"
                    (select-keys result [:status :reason]))
        action (get-in result [:decision :action])
        expected (:expected-action input)
        _ (require! (= (select-keys expected [:target :policy-id :cascade-digest])
                        (assoc (select-keys action [:target :cascade-digest]) :policy-id (:id action)))
                    "Handoff changed the selected executable identity" {:expected expected :actual (select-keys action [:id :policy-id :target :cascade-digest])})
        revision (shell/sh "git" "rev-parse" "HEAD")
        _ (require! (zero? (:exit revision)) "Cannot record source revision" {})
        boundary {:schema :wm/frozen-selected-boundary-v1
                  :source (:source input)
                  :import {:kind :offline-resealed-values
                           :original-menu-pin (:menu-pin original-menu)
                           :original-menu-validation (family/verify-sealed-menu original-menu)
                           :replay-menu-pin (:menu-pin menu)}
                  :selected-outer-cascade-policy carrier
                  :decision (:decision result)}
        summary {:schema :wm/frozen-selection-result-v1
                 :status :completed
                 :section :policy-evidence-to-selected-executable
                 :code-revision (.trim ^String (:out revision))
                 :source (:source input)
                 :script-sha256 (sha (slurp "scripts/wm_frozen_selection_step.clj"))
                 :input-sha256 (evidence/value-digest input)
                 :boundary-sha256 (evidence/value-digest boundary)
                 :import (:import boundary)
                 :policies-scored (count (:policies receipt))
                 :recorded-draw (:draw input)
                 :selected-action expected
                 :historical-scores-match true
                 :executable-handoff :selected
                 :task-executed false}]
    (write-gzip! (io/file output-dir "selected-boundary.edn.gz") boundary)
    (spit (io/file output-dir "result.edn") (str (pr-str summary) "\n"))
    (prn summary)))

(let [[command input output & extra] *command-line-args*]
  (require! (and input output (empty? extra))
            "Usage: clojure -M scripts/wm_frozen_selection_step.clj freeze RUN.edn INPUT.edn.gz | replay INPUT.edn.gz OUTPUT-DIR" {})
  (case command
    "freeze" (freeze! input output)
    "replay" (replay! input output)
    (throw (ex-info "Unknown command" {:command command}))))
