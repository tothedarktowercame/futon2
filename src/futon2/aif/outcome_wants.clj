(ns futon2.aif.outcome-wants
  "Primary mission wants, supplied entirely as data and an observation function."
  (:require [clojure.string :as str]
            [futon2.aif.cascade-problems :as problems])
  (:import [java.security MessageDigest]))

(defn outcome-criterion
  "Use :statement, :quote, or the first extractor cue's verbatim quote.
  A re-worded outcome gets a new token; its old locator is then orphaned.
  Tokens bind target and stated text, not extractor numbering."
  [target outcome]
  (let [stated (or (:statement outcome) (:quote outcome)
                   (get-in outcome [:cues 0 :quote]))]
    (when-not (and (string? stated) (not (str/blank? stated)))
      (throw (ex-info "Outcome has no stated text" {:kind :outcome-text-missing})))
    (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                          (.getBytes (pr-str [target stated]) "UTF-8"))
          hash (subs (apply str (map #(format "%02x" (bit-and 255 %)) digest)) 0 16)]
      {:kind :outcome-statement :role :why :stated stated
       :token (keyword "outcome" (str "h" hash))})))

(defn wants
  "Published locators of a checkable class admit wants. OBSERVE takes one
  locator and returns a boolean, like flight's observe-loc. It is called
  only for admitted locators. Keyword want identities resolve to their
  :role :why entries in :criteria-by-token; no phase exits are added."
  [target outcomes published-locators observe]
  (let [criteria (vec (distinct (map #(outcome-criterion target %) outcomes)))
        located? #(contains? problems/checkable-classes
                             (:class (get published-locators (:token %))))
        admitted (filterv located? criteria)
        missing (filterv (complement located?) criteria)
        locators (select-keys published-locators (map :token admitted))]
    {:wants (mapv :token admitted)
     :locators locators
     :universe (into {} (for [[token locator] locators]
                         (let [value (observe locator)]
                           (when-not (boolean? value)
                             (throw (ex-info "Observation must be boolean"
                                             {:kind :outcome-observation-not-boolean :token token})))
                           [token value])))
     :criteria-by-token (into {} (map (juxt :token identity) criteria))
     :unlocated (mapv #(assoc (select-keys % [:token :stated :role])
                              :reason :no-admitted-locator) missing)
     :to-ask missing}))
