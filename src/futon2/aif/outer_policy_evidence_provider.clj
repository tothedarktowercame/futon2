(ns futon2.aif.outer-policy-evidence-provider
  "Explicit authority boundary for production outer-policy evidence."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.load-identity :as identity]))

(identity/register! *ns* *file*)

(def schema :wm/outer-policy-evidence-provider-v1)

(defn- hex64? [x] (and (string? x) (boolean (re-matches #"[0-9a-f]{64}" x))))
(defn pin [body]
  {:path "wm://outer-policy-evidence-provider-v1"
   :sha256 (identity/sha256 (.getBytes (pr-str (dissoc body :source-pin)) "UTF-8"))})

(defn verify [provided]
  (try
    (cond
      (not= schema (:schema provided)) :provider-schema
      (not= :provided (:status provided)) (or (:reason provided) :provider-refused)
      (not (map? (:declaration provided))) :provider-declaration
      (not (vector? (:records provided))) :provider-records
      (not (and (string? (get-in provided [:source-pin :path]))
                (hex64? (get-in provided [:source-pin :sha256])))) :provider-source-pin
      (not= (pin provided) (:source-pin provided)) :provider-source-pin-mismatch
      :else nil)
    (catch Throwable _ :provider-malformed)))

(defn provided
  "Construct a pinned provider response. Numeric authority remains entirely
  in DECLARATION; this function supplies no defaults."
  [declaration records authority]
  (let [body {:schema schema :status :provided :authority authority
              :declaration declaration :records (vec records)}]
    (assoc body :source-pin (pin body))))

(defn resolve-provider [provider sealed-menu]
  (try
    (let [result (cond
                   (ifn? provider) (provider sealed-menu)
                   (map? provider) provider
                   :else {:schema schema :status :refused
                          :reason :outer-policy-evidence-provider-required})]
      (if-let [defect (verify result)]
        {:schema schema :status :refused :reason defect
         :missing-policy-digests (mapv :cascade-digest (:entries sealed-menu))
         :absent-terms [:outcome-vocabulary :dirichlet-prior-alpha
                        :preference-distribution :preference-authority :gamma
                        :lnE-resource :lnE-cadence :lnE-continuation
                        :durable-terminal-records]}
        result))
    (catch Throwable t
      {:schema schema :status :refused :reason :outer-policy-evidence-provider-threw
       :error-class (.getName (class t))
       :missing-policy-digests (mapv :cascade-digest (:entries sealed-menu))})))

(defn load-static
  "Load an explicit provider response from a pinned EDN file. The file must
  contain the full provider schema and self-pin; loading never synthesizes it."
  [path]
  (try
    (edn/read-string (slurp (io/file path)))
    (catch Throwable t
      {:schema schema :status :refused :reason :provider-source-unavailable
       :path path :error-class (.getName (class t))})))
