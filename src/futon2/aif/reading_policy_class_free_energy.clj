(ns futon2.aif.reading-policy-class-free-energy
  "Per-reading-policy class-channel F from retained completed-run records."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.reading-policy-class-history-index :as history-index]))

(def terminal-classes #{:focused :related :unrelated :stop-the-line})

(defn absolute-history-directory! [path]
  (let [directory (io/file path)]
    (when-not (.isAbsolute directory)
      (throw (ex-info "Class history root must be absolute"
                      {:kind :relative-class-history-root :path path})))
    directory))

(defn history-context
  "Read the declared window and retained records. The declaration, rather than
  scorer code, owns both the rate-like window and history location."
  [policy-key]
  (let [declaration (-> "wm/reading-policy-class-f-v1.edn" io/resource slurp edn/read-string)
        directory (absolute-history-directory! (:run-record-directory declaration))]
    (history-index/history directory policy-key (:history-window declaration))))

(defn- q6-terminal [record]
  (some #(when (and (map? %) (= :wm/q6-terminal-v1 (:schema %))) %)
        (tree-seq coll? seq record)))

(defn retained-tick
  "Extract one typed class observation. SOURCE is {:path p :record r}; malformed,
  incomplete, and nonterminal records are not observations."
  [{:keys [path record]}]
  (let [choice (:choice (q6-terminal record))
        class (get-in record [:run-ending-classification :class])]
    (when (and (= :present (:status choice))
               (vector? (:policy-key choice))
               (contains? terminal-classes class))
      {:policy-key (:policy-key choice)
       :class class
       :pin {:path (str path)
             :run-id (or (:run/id record) (:run-id record))}})))

(defn select-history [policy-key window sources]
  (when-not (nat-int? window)
    (throw (ex-info "Class-F history window must be a natural number"
                    {:kind :invalid-class-f-history-window :window window})))
  (->> sources
       (keep retained-tick)
       (filter #(= policy-key (:policy-key %)))
       (take-last window)
       vec))

(defn evaluate
  "Evaluate the exact-posterior class F. PREDICTION is P(class|policy).
  A zero-evidence retained class is a typed impossibility, never skipped."
  [policy-key window sources prediction]
  (if-not policy-key
    {:status :history-unavailable-noncanonical-policy-key
     :value nil :policy-key nil :window window :history nil}
    (let [history (select-history policy-key window sources)]
    (loop [ticks history value 0.0]
      (if-let [{:keys [class pin]} (first ticks)]
        (let [z (double (get prediction class 0.0))]
          (if (zero? z)
            {:status :observation-impossible-under-policy
             :value nil :policy-key policy-key :window window :history history
             :failure {:run pin :class class :evidence z}}
            (recur (next ticks) (+ value (- (Math/log z))))))
        {:status :computed :value value :policy-key policy-key
         :window window :history history})))))
