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

(defn validate-config
  [config]
  (when-not (= schema (:schema/version config))
    (refuse! :on-demand/unknown-schema {:value (:schema/version config)}))
  (doseq [k [:run/id :author :reviewer :repair-reviewer]]
    (when (str/blank? (get config k))
      (refuse! :on-demand/missing-config {:field k})))
  (when (= (:author config) (:reviewer config))
    (refuse! :on-demand/author-equals-reviewer
             {:author (:author config) :reviewer (:reviewer config)}))
  (when-not (= 1 (:opportunity-count config))
    (refuse! :on-demand/not-exactly-one
             {:opportunity-count (:opportunity-count config)}))
  (when-not (and (integer? (:poll-ms config)) (pos? (:poll-ms config))
                 (integer? (:timeout-ms config)) (pos? (:timeout-ms config)))
    (refuse! :on-demand/invalid-bound {:poll-ms (:poll-ms config)
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
                 base-url]} (validate-config config)
         endpoint (str (or base-url default-base-url) "/api/alpha/wm/click")
         request {:headers {"Content-Type" "application/json"}
                  :body (json/generate-string
                         {:run-id id :author author :reviewer reviewer
                          :repair-reviewer repair-reviewer
                          :trigger "duree-click-on-demand"})
                  :throw false}
         started (post! endpoint request)
         start-body (json/parse-string (str (:body started)) true)]
     (when-not (= 200 (:status started))
       (refuse! :on-demand/click-refused
                {:http/status (:status started) :response start-body}))
     (let [click-id (:click-id start-body)]
       (when (str/blank? click-id)
         (refuse! :on-demand/click-id-missing {:response start-body}))
       (loop [deadline (+ (now-ms) timeout-ms)]
         (let [response (get! endpoint {:throw false})
               body (json/parse-string (str (:body response)) true)]
           (when-not (= 200 (:status response))
             (refuse! :on-demand/status-unavailable
                      {:click/id click-id :http/status (:status response)}))
           (when-not (= click-id (:click-id body))
             (refuse! :on-demand/status-click-mismatch
                      {:expected click-id :actual (:click-id body)}))
           (if-not (:running? body)
             {:schema/version :wm/on-demand-whole-loop-result-v1
              :run/id id :click/id click-id
              :outcome (some-> body :last-result :outcome keyword)
              :terminal (:last-result body)}
             (if (>= (now-ms) deadline)
               (refuse! :on-demand/timeout {:click/id click-id
                                            :timeout-ms timeout-ms})
               (do (sleep! poll-ms) (recur deadline))))))))))

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
