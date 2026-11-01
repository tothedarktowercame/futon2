(ns futon2.aif.selected-mission-contract
  "The source-derived mission contract carried from selection to observation."
  (:require [clojure.string :as str]
            [futon2.aif.mission-hole-wants :as wants]
            [futon2.aif.load-identity :as identity]))

(identity/register! *ns* *file*)

(defn mission? [action]
  (and (str/starts-with? (str (:target action)) "M-")
       (not= :repair-machine-failure (:type action))))

(defn excursion?
  "EXCURSION-TARGET-FALSIFIER-AND-REVISION (run 2026-11-01-ac5bc477, Joe
  2026-11-01): an excursion with no wants in the standard format still has
  wants, to be determined on the fly from what the machine read at selection.
  `mission?` keeps its M- meaning for the measurement paths that key on it;
  excursion targets get their own predicate and their own refusal reason."
  [action]
  (and (str/starts-with? (str (:target action)) "E-")
       (not= :repair-machine-failure (:type action))))

(defn want-contract-target?
  "True for target kinds whose selected action must carry a source-derived
  want contract before any author contact: missions and excursions."
  [action]
  (or (mission? action) (excursion? action)))

(defn source-contract
  "Use the existing mission source reader, including its token and locator
  semantics, for missions AND excursions. TEXT is the source snapshot, never
  an invented want fixture. For an excursion whose source yields no want the
  refused derivation shape is returned rather than nil, so the pre-author
  guard can name why and selection can learn the target states no purpose."
  [code-root target path text]
  (when (want-contract-target? {:target target})
    (let [source (wants/mission-source code-root {:id target :path path :text text})]
      (if (seq (:want source))
        {:want (vec (:want source))
         :observation-locators (into {} (map (fn [[token locator]]
                                               [[target token] locator])) (:locators source))
         :mission-want-source (:source source)}
        (when (excursion? {:target target})
          {:want []
           :excursion-want-derivation
           {:status :empty
            :reason :no-checkable-closure-form-in-source
            :source-path path}})))))

(defn attach [action contract]
  (if (and (want-contract-target? action) contract)
    (merge action (select-keys contract [:want :observation-locators :mission-want-source
                                          :excursion-want-derivation]))
    action))

(defn require-before-author! [action]
  (when (and (mission? action) (empty? (:want action)))
    (throw (ex-info "Selected mission has no source-derived want contract"
                    {:outcome :refused :failure-kind :selected-want-contract-missing
                     :failure-stage :pre-author :target (:target action)})))
  (when (and (excursion? action) (empty? (:want action)))
    ;; It does not run on regardless: a typed stop BEFORE any author contact,
    ;; with the derivation result, feeding back why the target was selected
    ;; at all when its own text states no checkable purpose.
    (throw (ex-info "Selected excursion has no derivable want contract"
                    {:outcome :refused
                     :failure-kind :excursion-want-derivation-empty
                     :failure-stage :pre-author
                     :target (:target action)
                     :derivation (:excursion-want-derivation action)
                     :selection-feedback
                     (str "Selection: excursion " (:target action)
                          " states no checkable want in its own source; it "
                          "should not be selected until it states one.")})))
  action)
