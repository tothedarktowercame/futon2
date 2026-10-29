(ns futon2.aif.selected-mission-contract
  "The source-derived mission contract carried from selection to observation."
  (:require [clojure.string :as str]
            [futon2.aif.mission-hole-wants :as wants]
            [futon2.aif.load-identity :as identity]))

(identity/register! *ns* *file*)

(defn mission? [action]
  (and (str/starts-with? (str (:target action)) "M-")
       (not= :repair-machine-failure (:type action))))

(defn source-contract
  "Use the existing mission source reader, including its token and locator
  semantics. TEXT is the source snapshot, never an invented want fixture."
  [code-root target path text]
  (when (str/starts-with? (str target) "M-")
    (let [source (wants/mission-source code-root {:id target :path path :text text})]
      {:want (vec (:want source))
       :observation-locators (into {} (map (fn [[token locator]]
                                           [[target token] locator])) (:locators source))
       :mission-want-source (:source source)})))

(defn attach [action contract]
  (if (and (mission? action) contract)
    (merge action (select-keys contract [:want :observation-locators :mission-want-source]))
    action))

(defn require-before-author! [action]
  (when (and (mission? action) (empty? (:want action)))
    (throw (ex-info "Selected mission has no source-derived want contract"
                    {:outcome :refused :failure-kind :selected-want-contract-missing
                     :failure-stage :pre-author :target (:target action)})))
  action)
