(ns futon2.aif.lifecycle-exits
  "Secondary mission wants from the lifecycle definition; no flight wiring or IO."
  (:require [clojure.string :as str]
            [futon2.aif.mission-criteria :as mc]
            [futon2.aif.cascade-problems :as problems]))

(def phases [:HEAD :IDENTIFY :MAP :DERIVE :ARGUE :VERIFY :INSTANTIATE :DOCUMENT])

(defn- phase-name [title]
  (some-> (re-find #"^(?:\d+\.\s+)?(HEAD|IDENTIFY|MAP|DERIVE|ARGUE|VERIFY|INSTANTIATE|DOCUMENT)(?:\s|$)"
                   (str title)) second keyword))

(defn definition-exits [definition-text]
  (let [at-line (vec (rest (reductions
                           (fn [phase line]
                             (if-let [[_ _ title] (re-matches #"^(#{1,3})\s+(.*)$" line)]
                               (phase-name title) phase))
                           nil (str/split-lines definition-text))))
        exits (mapv (fn [c] {:phase (get at-line (dec (:line c))) :stated (:stated c)})
                    (filter #(= :phase-exit (:kind %)) (mc/criteria "definition" definition-text)))]
    (when-not (= phases (mapv :phase exits))
      (throw (ex-info "Lifecycle definition must yield the eight ordered phase exits"
                      {:kind :invalid-lifecycle-definition :found exits :expected phases})))
    exits))

(defn lifecycle-shaped?
  "Return the boolean and the recognized level-2 phases, in document order."
  [mission-text]
  (let [found (vec (distinct (keep (fn [line]
                                    (when-let [[_ title] (re-matches #"^##\s+(.*)$" line)]
                                      (phase-name title)))
                                  (str/split-lines mission-text))))]
    {:lifecycle-shaped? (boolean (seq found)) :phases found}))

(defn supplied [mission-id mission-text definition-text]
  (let [shape (lifecycle-shaped? mission-text)]
    (if-not (:lifecycle-shaped? shape)
      (assoc shape :criteria [] :reason :not-lifecycle-shaped)
      (let [present (set (keep #(when (= :phase-exit (:kind %)) (phase-name (:phase %)))
                               (mc/criteria mission-id mission-text)))]
        (assoc shape :criteria
               (mapv (fn [{:keys [phase stated]}]
                       ;; Delegate to the existing public reader, including its token derivation.
                       (let [c (first (mc/criteria mission-id (str "## " (name phase) "\n" stated)))]
                         (assoc (select-keys c [:kind :stated :token])
                                :phase phase :role :how :supplied-by :lifecycle-definition)))
                     (remove #(present (:phase %)) (definition-exits definition-text))))))))

(defn wants
  "Only checkable published locators admit secondary wants. OBSERVE returns a boolean."
  [criteria published-locators observe]
  (let [criteria (mapv #(assoc % :role :how) criteria)
        located? #(contains? problems/checkable-classes
                             (:class (get published-locators (:token %))))
        admitted (filterv located? criteria)
        missing (filterv (complement located?) criteria)
        locators (select-keys published-locators (map :token admitted))]
    {:wants (mapv :token admitted) :locators locators
     :universe (into {} (for [[token locator] locators]
                         (let [value (observe locator)]
                           (when-not (boolean? value)
                             (throw (ex-info "Observation must be boolean"
                                             {:kind :lifecycle-observation-not-boolean :token token})))
                           [token value])))
     :criteria-by-token (into {} (map (juxt :token identity) criteria))
     :unlocated (mapv #(assoc (select-keys % [:token :stated :role :phase])
                              :reason :no-admitted-locator) missing)
     :to-ask missing}))
