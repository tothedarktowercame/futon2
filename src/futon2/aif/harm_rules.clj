(ns futon2.aif.harm-rules
  "Pure matcher for RefusalLegality's `harmRuleMatched` C3 evidence.

  Rule prose is documentation only. A match requires a typed declined action,
  a listed operation, every required field, and every named world check."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.action-identity :as action-identity])
  (:import [java.nio.file Paths]))

(def ^:private resource-path "wm/harm-rules.edn")

(defn load-rules []
  (let [declaration (some-> resource-path io/resource slurp edn/read-string)
        ids (mapv :id (:rules declaration))]
    (when-not (= :wm/harm-rules-v1 (:schema declaration))
      (throw (ex-info "Invalid harm-rule schema"
                      {:kind :invalid-harm-rule-schema
                       :schema (:schema declaration)})))
    (when-not (= (count ids) (count (set ids)))
      (throw (ex-info "Harm-rule ids must be unique"
                      {:kind :duplicate-harm-rule-id :ids ids})))
    declaration))

(defn- world-value [world key & args]
  (when-let [f (get world key)]
    (apply f args)))

(defn- normalized-path [path]
  (.normalize (.toAbsolutePath (Paths/get (str path) (make-array String 0)))))

(defn- under-root? [path root]
  (.startsWith (normalized-path path) (normalized-path root)))

(defn- paths-exist [_rule action world]
  (every? #(true? (world-value world :path-exists? %)) (:paths action)))

(defn- paths-under-production-data-root [_rule action world]
  (let [roots (world-value world :production-data-roots)]
    (and (seq roots)
         (every? (fn [path] (some #(under-root? path %) roots)) (:paths action)))))

(defn- force-or-default-branch [_rule action world]
  (or (true? (:force? action))
      (= (:ref action)
         (world-value world :default-branch (:repo action) (:remote action)))))

(defn- service-is-shared [rule action _world]
  (contains? (:shared-services rule) (:service action)))

(defn- actor-is-joe-or-destination-external [_rule action world]
  (or (contains? (set (world-value world :joe-identities)) (:actor action))
      (false? (world-value world :local-destination? (:destination action)))))

(defn- amount-positive [_rule action _world]
  (and (number? (:amount action)) (pos? (:amount action))))

(defn- existing-lean-path [_rule action world]
  (some #(and (str/ends-with? (str %) ".lean")
              (true? (world-value world :path-exists? %)))
        (:paths action)))

(def ^:private checks
  {:paths-exist paths-exist
   :paths-under-production-data-root paths-under-production-data-root
   :force-or-default-branch force-or-default-branch
   :service-is-shared service-is-shared
   :actor-is-joe-or-destination-external actor-is-joe-or-destination-external
   :amount-positive amount-positive
   :existing-lean-path existing-lean-path})

(defn match
  "Check CLAIM against a loaded rule declaration and injected WORLD ports."
  [rules {:keys [rule declined-action]} world]
  (let [rule-declaration (some #(when (= rule (:id %)) %) (:rules rules))]
    (cond
      (not (map? declined-action))
      {:status :refused :reason :no-declined-action}

      (nil? rule-declaration)
      {:status :refused :reason :unknown-rule :rule rule}

      (not (contains? (:operations rule-declaration) (:operation declined-action)))
      {:status :refused :reason :operation-not-in-rule :rule rule
       :operation (:operation declined-action)}

      :else
      (if-let [field (first (remove #(contains? declined-action %)
                                    (:requires rule-declaration)))]
        {:status :refused :reason :missing-field :rule rule :field field}
        (loop [[check & remaining] (:checks rule-declaration)]
          (if check
            (if-let [check-fn (get checks check)]
              (if (try (boolean (check-fn rule-declaration declined-action world))
                       (catch Exception _ false))
                (recur remaining)
                {:status :refused :reason :check-failed :rule rule :check check})
              {:status :refused :reason :unknown-check :rule rule :check check})
            {:status :matched :rule rule
             :declined-action-digest (action-identity/digest declined-action)}))))))
