(ns futon2.aif.on-demand-entrypoint
  "Bounded operator client for exactly one existing gated WM click.

  This namespace does not run the loop itself. It submits once to Futon3c's
  single-flight `/api/alpha/wm/click` boundary and reads that exact click until
  it is terminal. Running it remains operator-authorized work; constructing and
  testing this client is not authorization or readiness evidence."
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as str]))

(def schema :wm/on-demand-whole-loop-v1)
(def default-base-url "http://127.0.0.1:7070")

(defn- refuse! [kind data]
  (throw (ex-info (name kind) {:refusal (assoc data :kind kind)})))

(defn- nonblank-string? [x]
  (and (string? x) (not (str/blank? x))))

(defn- parse-json! [stage response]
  (try
    (let [body (json/parse-string (str (:body response)) true)]
      (when-not (map? body)
        (refuse! :on-demand/malformed-response {:stage stage :body body}))
      body)
    (catch clojure.lang.ExceptionInfo e (throw e))
    (catch Throwable e
      (refuse! :on-demand/malformed-json
               {:stage stage :error (or (ex-message e) (.getName (class e)))}))))

(defn- transport! [stage invoke]
  (try
    (invoke)
    (catch clojure.lang.ExceptionInfo e
      (refuse! :on-demand/transport-failed
               {:stage stage :error (or (ex-message e) (.getName (class e)))}))
    (catch Throwable e
      (refuse! :on-demand/transport-failed
               {:stage stage :error (or (ex-message e) (.getName (class e)))}))))

(defn validate-config
  [config]
  (when-not (= schema (:schema/version config))
    (refuse! :on-demand/unknown-schema {:value (:schema/version config)}))
  (doseq [k [:run/id :author :reviewer :repair-reviewer]]
    (when-not (nonblank-string? (get config k))
      (refuse! :on-demand/missing-config {:field k})))
  (when (= (:author config) (:reviewer config))
    (refuse! :on-demand/author-equals-reviewer
             {:author (:author config) :reviewer (:reviewer config)}))
  (when-not (= 1 (:opportunity-count config))
    (refuse! :on-demand/not-exactly-one
             {:opportunity-count (:opportunity-count config)}))
  (when-not (and (integer? (:transport-timeout-ms config))
                 (pos? (:transport-timeout-ms config))
                 (integer? (:poll-ms config)) (pos? (:poll-ms config))
                 (integer? (:timeout-ms config)) (pos? (:timeout-ms config)))
    (refuse! :on-demand/invalid-bound {:transport-timeout-ms
                                       (:transport-timeout-ms config)
                                       :poll-ms (:poll-ms config)
                                       :timeout-ms (:timeout-ms config)}))
  config)

(defn run-on-demand!
  "Submit exactly once, then read only the accepted click's terminal status.

  `ports` is injectable solely for hermetic tests. Production omits it. A
  refusal or non-success terminal outcome is returned unchanged, never changed
  into success."
  ([config] (run-on-demand! config {}))
  ([config {:keys [post! get! now-ms sleep!]
            :or {post! (fn [url request] (http/post url request))
                 get! (fn [url request] (http/get url request))
                 now-ms #(System/currentTimeMillis)
                 sleep! #(Thread/sleep %)}}]
   (let [{:keys [run/id author reviewer repair-reviewer poll-ms timeout-ms
                 transport-timeout-ms base-url]} (validate-config config)
         began-at (now-ms)
         endpoint (str (or base-url default-base-url) "/api/alpha/wm/click")
         request {:headers {"Content-Type" "application/json"}
                  :body (json/generate-string
                         {:run-id id :author author :reviewer reviewer
                          :repair-reviewer repair-reviewer
                          :trigger "duree-click-on-demand"})
                  :timeout transport-timeout-ms :throw false}
         started (transport! :acceptance #(post! endpoint request))
         start-body (parse-json! :acceptance started)]
     (when-not (= 200 (:status started))
       (refuse! :on-demand/click-refused
                {:http/status (:status started) :response start-body}))
     (let [click-id (:click-id start-body)]
       (when-not (and (nonblank-string? click-id)
                      (nonblank-string? (:started-at start-body)))
         (refuse! :on-demand/click-id-missing {:response start-body}))
       (loop [deadline (+ began-at timeout-ms)]
         (when (>= (now-ms) deadline)
           (refuse! :on-demand/observation-timeout
                    {:click/id click-id :timeout-ms timeout-ms
                     :worker-cancelled? false}))
         (let [response (transport!
                         :status
                         #(get! endpoint {:timeout transport-timeout-ms
                                          :throw false}))
               body (parse-json! :status response)]
           (when-not (= 200 (:status response))
             (refuse! :on-demand/status-unavailable
                      {:click/id click-id :http/status (:status response)}))
           (when-not (= click-id (:click-id body))
             (refuse! :on-demand/status-click-mismatch
                      {:expected click-id :actual (:click-id body)}))
           (when-not (instance? Boolean (:running? body))
             (refuse! :on-demand/malformed-status {:click/id click-id
                                                   :response body}))
           (if (:running? body)
             (do (sleep! poll-ms) (recur deadline))
             (let [terminal (:last-result body)
                   outcome (:outcome terminal)
                   observation (:run-id-observation terminal)]
               (when-not (and (map? terminal)
                              (nonblank-string? outcome)
                              (= click-id (:click-id terminal)))
                 (refuse! :on-demand/malformed-terminal
                          {:click/id click-id :terminal terminal}))
               (when-not (and (map? observation)
                              (contains? #{"present" "absent"}
                                         (:status observation))
                              (or (= "absent" (:status observation))
                                  (nonblank-string? (:value observation))))
                 (refuse! :on-demand/malformed-run-id-observation
                          {:click/id click-id :observation observation}))
               (cond->
                {:schema/version :wm/on-demand-whole-loop-result-v1
                 :run/requested-id id
                 :run/observation observation
                 :click/id click-id
                 :outcome (keyword outcome)
                 :terminal terminal}
                 (= "present" (:status observation))
                 (assoc :run/id (:value observation)))))))))))

(defn- read-one-config [path]
  (with-open [reader (java.io.PushbackReader. (io/reader path :encoding "UTF-8"))]
    (let [value (edn/read reader)
          tail (edn/read {:eof ::eof} reader)]
      (when-not (= ::eof tail)
        (refuse! :on-demand/trailing-config {:path path}))
      value)))

(defn -main [& [config-path]]
  (when (str/blank? config-path)
    (refuse! :on-demand/config-path-missing {}))
  (pp/pprint (run-on-demand! (read-one-config config-path))))
