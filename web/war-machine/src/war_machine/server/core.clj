(ns war-machine.server.core
  "HTTP server for the War Machine web visualiser.

   Mirrors the webarxana pattern (futon4/dev/web/webarxana/src/webarxana/server/core.clj):
   reitit + http-kit + ring-json + resource handler for the SPA shell.

   One data endpoint at THIS JVM — /api/war-machine — calls the canonical
   futon2.report.war-machine/generate-war-machine and returns the snapshot as JSON.
   The tick loop, ant motion, glow decay all happen client-side in CLJS.

   IMPORTANT: this JVM is NOT self-sufficient for the SPA.  The cljs client
   also fetches /api/alpha/aif-stack/live and /api/alpha/war-machine/show-in-emacs
   from the futon3c JVM on :7070 (the One JVM per futon3c/CLAUDE.md I-0).
   In dev, shadow-cljs's :proxy-url stitches them together.  See
   `futon2/INSTALL.md` for the full bring-up architecture + gotchas."
  (:require [org.httpkit.server :as hk]
            [reitit.ring :as ring]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.util.response :as resp]
            [futon2.report.war-machine :as wm])
  (:gen-class))

(def config
  (atom {:port (or (some-> (System/getenv "PORT") Integer/parseInt) 3110)}))

(defn- keywordize-keys
  "Recursively convert map keys to strings for JSON serialization.
   Namespaced keywords become 'ns/name'."
  [x]
  (cond
    (map? x)    (into {} (map (fn [[k v]]
                                [(cond
                                   (keyword? k) (if (namespace k)
                                                  (str (namespace k) "/" (name k))
                                                  (name k))
                                   :else (str k))
                                 (keywordize-keys v)])
                              x))
    (vector? x) (mapv keywordize-keys x)
    (seq? x)    (mapv keywordize-keys x)
    (set? x)    (mapv keywordize-keys x)
    (keyword? x) (if (namespace x)
                   (str (namespace x) "/" (name x))
                   (name x))
    :else x))

(defn- war-machine-data
  "Run the scan and return JSON-safe data for the browser."
  [days]
  (let [{:keys [data judgement]} (wm/generate-war-machine days)]
    (keywordize-keys (assoc data :judgement judgement))))

(defn- data-handler [req]
  (let [qs (or (:query-string req) "")
        days-from-qs (when-let [m (re-find #"(?:^|&)days=([0-9]+)" qs)]
                       (Integer/parseInt (second m)))
        days (or (some-> (get-in req [:query-params "days"]) Integer/parseInt)
                 days-from-qs
                 14)]
    (-> {:status 200
         :body (war-machine-data days)})))

(defn app-routes []
  (ring/ring-handler
   (ring/router
    ;; Mirror the futon3c-hosted path (/api/alpha/war-machine). Keep the
    ;; short /api/war-machine alias for backwards compatibility with any
    ;; existing probes.
    [["/api/alpha/war-machine" {:get (-> data-handler wrap-json-response)}]
     ["/api/war-machine"       {:get (-> data-handler wrap-json-response)}]])
   (ring/routes
    (ring/create-resource-handler {:path "/"})
    (ring/create-default-handler
     {:not-found (constantly (-> (resp/resource-response "public/index.html")
                                 (resp/content-type "text/html")))}))))

(defn -main [& _args]
  (let [port (:port @config)]
    (println (str "War Machine web starting on http://localhost:" port))
    (hk/run-server (app-routes) {:port port})
    (println "Ready.")))
