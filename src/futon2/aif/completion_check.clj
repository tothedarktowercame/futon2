(ns futon2.aif.completion-check
  "Executable, revision-bound completion checks.

  Only artifact locators have a runtime decision procedure. Prose and square
  descriptions remain typed absences rather than being interpreted as facts."
  (:require [clojure.string :as str]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.observation-checks :as observation-checks]))

(load-identity/register! *ns* *file*)

(def ^:private supported-classes #{:C3 :C4})

(defn- required-fields [class]
  (remove #{:sha} (get observation-checks/locator-fields class)))

(defn- non-blank-field? [locator field]
  (let [value (get locator field)]
    (and (string? value) (not (str/blank? value)))))

(defn executable?
  "True exactly for a complete C3 or C4 artifact-locator form.

  An authored :sha is permitted as inert provenance; evaluate always replaces
  it with the supplied revision."
  [check]
  (and (vector? check)
       (= 2 (count check))
       (= :artifact-locator (first check))
       (map? (second check))
       (contains? supported-classes (:class (second check)))
       (every? #(non-blank-field? (second check) %)
               (required-fields (:class (second check))))))

(defn- missing [reason & [data]]
  (cond-> {:status :missing :reason reason}
    data (assoc :data data)))

(defn evaluate
  "Evaluate CHECK at REVISION, returning a boolean observation or typed absence."
  [check revision]
  (if-not (executable? check)
    (missing :completion-check-not-executable)
    (let [authored (second check)
          locator-repo (:repo authored)
          revision-repo (:repo revision)
          revision-sha (:sha revision)]
      (cond
        (not= locator-repo revision-repo)
        {:status :missing :reason :different-artifact-repository
         :repo locator-repo :revision-repo revision-repo}

        (not (and (string? revision-sha)
                  (re-matches #"[0-9a-f]{40}" revision-sha)))
        (missing :after-revision-missing)

        :else
        (let [locator (assoc authored :sha revision-sha)
              result ((case (:class authored)
                        :C3 observation-checks/check-path-exists
                        :C4 observation-checks/check-decl-in-file)
                      locator)]
          (if (boolean? (:observed result))
            {:status :observed
             :value (:observed result)
             :check (:check result)
             :evidence (cond-> (:evidence result)
                         (contains? authored :sha)
                         (assoc :authored-sha (:sha authored)))}
            (missing (or (:kind result) :observation-unavailable)
                     (:data result))))))))
