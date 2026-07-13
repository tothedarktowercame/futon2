(ns futon2.aif.substrate
  "Backend-neutral semantic client for the authoritative substrate store.

  The public operations deliberately expose graph meanings, not XTDB query or
  transaction forms. FUTON_SUBSTRATE_URL is canonical; FUTON1A_URL and
  FUTON1A_BASE_URL remain read-only compatibility inputs during migration."
  (:require [babashka.http-client :as http]
            [clojure.edn :as edn]
            [clojure.string :as str])
  (:import [java.net URLEncoder]))

(def default-timeout-ms 60000)

(defn- strip-api-suffix [s]
  (some-> s (str/replace #"/+$" "")
          (str/replace #"/api/alpha$" "")))

(defn configured-url
  "Resolve the one substrate authority URL. An explicit :substrate-url wins."
  ([] (configured-url {}))
  ([opts]
   (strip-api-suffix
    (or (:substrate-url opts)
        (System/getenv "FUTON_SUBSTRATE_URL")
        (System/getenv "FUTON1A_URL")
        (System/getenv "FUTON1A_BASE_URL")
        "http://127.0.0.1:7071"))))

(defn- api-url [opts path]
  (str (configured-url opts) "/api/alpha" path))

(defn- encode [x]
  (URLEncoder/encode (if (keyword? x) (subs (str x) 1) (str x)) "UTF-8"))

(defn- parse-body [body]
  (if (string? body)
    (edn/read-string {:default (fn [_tag v] v)} body)
    body))

(defn- request!
  [method url opts body]
  (let [headers (cond-> {"Accept" "application/edn"}
                  body (assoc "Content-Type" "application/edn")
                  (= method :post) (assoc "x-penholder"
                                          (or (:penholder opts)
                                              (System/getenv "FUTON1B_PENHOLDER")
                                              (System/getenv "FUTON1A_PENHOLDER")
                                              "api")))
        response (http/request {:method method
                                :uri url
                                :headers headers
                                :body (when body (pr-str body))
                                :timeout (or (:substrate-timeout-ms opts)
                                             default-timeout-ms)
                                :throw false})
        parsed (try (parse-body (:body response))
                    (catch Throwable _ (:body response)))]
    (if (<= 200 (:status response) 299)
      parsed
      (throw (ex-info "authoritative substrate request failed"
                      {:method method :url url :status (:status response)
                       :body parsed})))))

(defn inhabitation
  "Return input bindings with authoritative :inhabited? results, in order."
  ([bindings] (inhabitation bindings {}))
  ([bindings opts]
   (if-let [f (:inhabitation-fn opts)]
     (f bindings)
     (:bindings (request! :post (api-url opts "/graph/inhabited") opts
                           {:bindings (vec bindings)})))))

(defn entities-by-type
  ([type] (entities-by-type type {}))
  ([type opts]
   (if-let [f (:entities-by-type-fn opts)]
     (f type)
     (let [url (str (api-url opts "/entities") "?type=" (encode type)
                    "&limit=" (long (or (:limit opts) 10000)))]
       (:entities (request! :get url opts nil))))))

(defn hyperedges-by-type
  ([type] (hyperedges-by-type type {}))
  ([type opts]
   (if-let [f (:hyperedges-by-type-fn opts)]
     (f type)
     (let [url (str (api-url opts "/hyperedges") "?type=" (encode type)
                    "&limit=" (long (or (:limit opts) 10000)))]
       (:hyperedges (request! :get url opts nil))))))

(defn hyperedges-by-end
  ([end] (hyperedges-by-end end {}))
  ([end opts]
   (if-let [f (:hyperedges-by-end-fn opts)]
     (f end)
     (let [url (str (api-url opts "/hyperedges") "?end=" (encode end)
                    "&limit=" (long (or (:limit opts) 10000)))]
       (:hyperedges (request! :get url opts nil))))))

(defn relations
  ([filters] (relations filters {}))
  ([{:keys [type from to]} opts]
   (if-let [f (:relations-fn opts)]
     (f {:type type :from from :to to})
     (let [params (cond-> []
                    type (conj (str "type=" (encode type)))
                    from (conj (str "from=" (encode from)))
                    to (conj (str "to=" (encode to)))
                    true (conj (str "limit=" (long (or (:limit opts) 10000)))))
           url (str (api-url opts "/relations") "?" (str/join "&" params))]
       (:relations (request! :get url opts nil))))))

(defn relation-snapshot
  "Return matching relations plus the endpoint entity documents needed to
  interpret UUID-backed relation endpoints without N+1 HTTP reads."
  ([filters] (relation-snapshot filters {}))
  ([{:keys [type types from to]} opts]
   (if-let [f (:relation-snapshot-fn opts)]
     (f {:type type :types types :from from :to to})
     (let [params (cond-> []
                    type (conj (str "type=" (encode type)))
                    (seq types) (conj (str "types="
                                           (str/join "," (map encode types))))
                    from (conj (str "from=" (encode from)))
                    to (conj (str "to=" (encode to)))
                    true (conj "hydrate=true"
                               (str "limit=" (long (or (:limit opts) 10000)))))
           url (str (api-url opts "/relations") "?" (str/join "&" params))]
       (request! :get url opts nil)))))

(defn- entity-payload [doc]
  (let [id (or (:entity/id doc) (:xt/id doc))
        reserved #{:xt/id :entity/id :entity/name :entity/type
                   :entity/source :entity/external-id :entity/props}
        domain (apply dissoc doc reserved)
        props (merge (:entity/props doc) domain)]
    (cond-> {:id id
             :name (or (:entity/name doc) (str id))
             :type (:entity/type doc)}
      (:entity/source doc) (assoc :source (:entity/source doc))
      (:entity/external-id doc) (assoc :external-id (:entity/external-id doc))
      (seq props) (assoc :props props))))

(defn put-doc!
  "Write a graph document through its invariant-bearing public route."
  ([doc] (put-doc! doc {}))
  ([doc opts]
   (if-let [f (:put-doc-fn opts)]
     (f doc)
     (cond
       (:hx/type doc)
       (request! :post (api-url opts "/hyperedge") opts
                 {:hx/id (or (:hx/id doc) (:xt/id doc))
                  :hx/type (:hx/type doc)
                  :hx/endpoints (:hx/endpoints doc)
                  :hx/props (or (:hx/props doc)
                                (apply dissoc doc [:xt/id :hx/id :hx/type
                                                   :hx/endpoints :hx/ends]))})

       (:entity/type doc)
       (request! :post (api-url opts "/entity") opts (entity-payload doc))

       :else
       (throw (ex-info "unsupported substrate document shape"
                       {:required-one-of [:entity/type :hx/type]
                        :doc-id (:xt/id doc)}))))))

(defn submit-puts!
  "Compatibility helper for the former transaction-shaped AIF callers. Only
  puts are accepted: delete/evict has no invariant-bearing public operation."
  ([tx-ops] (submit-puts! tx-ops {}))
  ([tx-ops opts]
   (mapv (fn [[op doc :as tx-op]]
           (if (= :xtdb.api/put op)
             (put-doc! doc opts)
             (throw (ex-info "unsupported substrate operation" {:tx-op tx-op}))))
         tx-ops)))
