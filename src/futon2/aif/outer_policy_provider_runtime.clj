(ns futon2.aif.outer-policy-provider-runtime
  "Adapt the existing learning authority and pinned outcomes to the current policy menu."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.outer-learning-runtime :as learning]
            [futon2.aif.outer-target-belief :as belief]
            [futon2.aif.outer-target-score :as score]
            [futon2.aif.outer-policy-evidence-provider :as provider]))

(defn- terminal-record [row]
  (let [{:keys [path sha256]} (:source row)
        bytes (java.nio.file.Files/readAllBytes (.toPath (io/file path)))
        _ (when-not (= sha256 (identity/sha256 bytes))
            (throw (ex-info "Policy outcome source drift" {:path path})))
        record (edn/read-string (String. bytes "UTF-8"))
        outcome (belief/observation row)]
    (assoc record
           :selected-cascade-digest
           (or (get-in record [:outer-task-selection :policy :selected-outer-cascade-policy :cascade-digest])
               (get-in record [:decision :selection-certificate :selected-canonical-cascade :digest]))
           :observed-outcome (:class outcome))))

(defn provide
  "No minimum history count. Unseen policies retain declared prior alpha;
  controller terms reuse the existing target-grain resource/cadence semantics."
  [state menu records]
  (if-not (= :produced (get-in state [:belief :status]))
    {:schema provider/schema :status :refused :reason :learning-authority-unavailable}
    (let [vocab belief/outcome-vocabulary
          authority (:authority state)
          source (get-in state [:preference-authority :source])
          scored (mapv (fn [entry]
                         [entry (score/score (assoc state :target (:target-id entry)))])
                       (:entries menu))
          failure (some #(when-not (= :scored (:status (second %))) (second %)) scored)]
      (if failure
        {:schema provider/schema :status :refused :reason (:reason failure)}
        (let [prefs (:preference-distribution (second (first scored)))
              component-pin {:path "wm://existing-outer-controller-inputs"
                             :sha256 (identity/sha256
                                       (.getBytes (pr-str [source (get-in state [:belief :source-pin])
                                                          (:controller state)]) "UTF-8"))}
              d {:schema :wm/outer-policy-evidence-declaration-v1
                 :outcome-vocabulary vocab
                 :gamma (get-in authority [:outer-policy :gamma])
                 :preference-authority source
                 :preference {:vocabulary vocab :units :probability
                              :authority source :distribution (mapv prefs vocab)}
                 :global-prior {:alpha (mapv (get-in state [:belief :prior :alpha]) vocab)}
                 :policies
                 (into {} (map (fn [[entry result]]
                                 (let [terms (:terms result)]
                                   [(:cascade-digest entry)
                                    {:ln-e
                                     (into {} (map (fn [[k value]]
                                                     [k {:value value :units :nats
                                                         :source-pin component-pin}]))
                                           {:resource (- (:weighted-resource-cost terms))
                                            :cadence (- (:recent-selection-opportunity-cost terms))
                                            :continuation (:continuation-credit terms)})}])) scored))}
              declaration (assoc d :pin {:path "wm://dynamic-outer-policy-declaration"
                                         :sha256 (identity/sha256 (.getBytes (pr-str d) "UTF-8"))})]
          (provider/provided declaration records
                             {:source-pin source :controller-input-pin component-pin}))))))

(defn production-provider [menu]
  (let [state (learning/load-shadow)]
    (provide state menu (mapv terminal-record (get-in state [:dataset :rows])))))
