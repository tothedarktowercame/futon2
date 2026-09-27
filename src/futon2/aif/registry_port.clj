(ns futon2.aif.registry-port
  "Storage-free port for reading test-registry records.

  Futon2 owns the consumer contract but not the registry implementation.
  Futon3c installs the implementation from
  `futon3c.test-registry.local-port`; standalone Futon2 therefore fails
  explicitly instead of falling back to an HTTP evidence store."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def installer 'futon3c.test-registry.local-port/install!)
(defonce ^:private !implementation (atom nil))

(defn failure [kind & [data]]
  {:registry-port/status :failure
   :kind kind
   :data (merge {:installer installer} data)})

(defn failure? [value]
  (= :failure (:registry-port/status value)))

(defn install!
  "Install the complete registry reader map. Reinstalling the same map is
  harmless; installing a different implementation replaces it explicitly."
  [implementation]
  (let [required #{:entry :latest-namespace :latest-command :runs}]
    (when-not (and (map? implementation)
                   (every? #(fn? (get implementation %)) required))
      (throw (ex-info "Incomplete registry port" {:required required})))
    (reset! !implementation implementation)
    :installed))

(defn uninstall!
  "Test/lifecycle reset. Production composition only calls install!."
  []
  (reset! !implementation nil))

(defn installed? [] (some? @!implementation))

(defn call
  "Call OP, returning a typed failure for an unset port or implementation
  exception. Nil remains the authoritative local-store absence."
  [op & args]
  (if-let [f (get @!implementation op)]
    (try (apply f args)
         (catch Throwable t
           (failure :registry-storage-failed
                    {:operation op :message (or (ex-message t)
                                                (.getName (class t)))})))
    (failure :registry-port-unset {:operation op})))
