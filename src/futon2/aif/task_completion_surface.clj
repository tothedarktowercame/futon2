(ns futon2.aif.task-completion-surface
  "Source-pinned completion discovery, not A4 baseline certification."
  (:require [futon2.aif.load-identity :as identity]
            [futon2.aif.mission-criteria :as criteria]
            [futon2.aif.mission-c :as mission-c]
            [futon2.aif.mission-hole-wants :as wants]))

(defn describe
  "Describe the registry entry's exact pinned bytes. Never fills a baseline
  or treats unrecognized completion prose as a false before-observation."
  [entry]
  (let [source (:source entry)
        text (slurp (:path source))
        actual (identity/sha256 (.getBytes text "UTF-8"))]
    (when-not (= actual (:sha256 source))
      (throw (ex-info "Completion source changed" {:kind :completion/source-pin-mismatch
                                                  :target (:id entry) :source source
                                                  :actual-sha256 actual})))
    (let [cc (criteria/criteria (:id entry) text)
          boxes (wants/current-checkboxes (:id entry) text)
          prose (mission-c/criteria-from-markdown text {:mission (:id entry) :path (:path source)})
          located (count (filter :met-decl cc))
          class (cond
                  (seq boxes) :checkbox-locators-with-retained-prose
                  (and (pos? located) (= located (count cc))) :criteria-locators
                  (or (seq cc) (seq (:criteria prose))) :completion-prose-needs-locators
                  :else :completion-definition-not-found)]
      {:schema :wm/completion-surface-v1 :target (:id entry) :source-pin source
       :completion-class class :checkbox-count (count boxes)
       :criteria-count (count cc) :located-criteria-count located
       :unlocated-criteria-count (- (count cc) located)
       :baseline {:status :absent :reason :selected-task-baseline-not-retained}
       :producer (case class
                   :completion-prose-needs-locators :completion-locator-author
                   :completion-definition-not-found :task-completion-author
                   :selected-task-revision-baseline)})))
