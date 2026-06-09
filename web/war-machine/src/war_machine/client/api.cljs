(ns war-machine.client.api
  "Data fetch: GET /api/war-machine → JSON → data ratom + seed ants."
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [war-machine.client.state :as s]
            [war-machine.client.tick :as tick])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn- api-base
  "Read `window.WM_API_BASE` (set in index.html) and return it as the URL
   prefix for API calls. Defaults to \"\" (same-origin/relative).

   The override exists because shadow-cljs dev-http's proxy has a hard
   timeout shorter than the WM scan's ~30s response; setting WM_API_BASE
   to the absolute futon3c URL bypasses the proxy entirely. futon3c
   returns Access-Control-Allow-Origin: * so cross-origin works.

   See M-war-machine-frontend-upgrade1 §2.G (2026-05-21 unsticking)."
  []
  (or (some-> js/window .-WM_API_BASE) ""))

(def ^:private endpoint-suffix "/api/alpha/war-machine")
(def ^:private operator-bulletin-suffix
  "/api/alpha/war-machine/operator-bulletin")
(def ^:private forward-model-suffix "/api/alpha/forward-model")
(def ^:private capability-star-map-suffix "/api/alpha/capability-star-map")
(def ^:private capability-star-map-static-path
  "/data/capability-star-map.graph.json")
(def ^:private pudding-status-static-path
  "/data/pudding-status.json")
(def ^:private affect-events-static-path
  "/data/affect-events.json")

(defn- endpoint-with-days []
  (str (api-base) endpoint-suffix "?days=" @s/days-window))

(defn- operator-bulletin-endpoint []
  ;; Use the same-origin path for defensive optional endpoints. The main WM
  ;; scan uses WM_API_BASE to bypass dev-proxy timeouts; these endpoints are
  ;; small and may 404 while their producers are not live yet. Same-origin keeps
  ;; those cleanly observable instead of surfacing browser CORS errors.
  operator-bulletin-suffix)

(defn- forward-model-endpoint []
  forward-model-suffix)

(defn- capability-star-map-endpoint []
  capability-star-map-suffix)

(defn- capability-star-map-fallback-endpoint []
  (str (or (some-> js/window .-WM_CAPABILITY_STAR_MAP_BASE)
           "http://localhost:3110")
       capability-star-map-suffix))

(defn- unavailable-payload [status]
  {:unavailable true
   :status status})

(defn- optional-operator-endpoints-ready? []
  (true? (some-> js/window .-WM_OPERATOR_ENDPOINTS_READY)))

(defn- load-optional-json!
  "Fetch an optional JSON endpoint without emitting browser console errors for
   ordinary non-200 statuses. Chrome reports 404 XHRs as console errors; fetch
   lets the Operator view treat absent producers as clean empty states."
  [url target-atom label]
  (if-not (optional-operator-endpoints-ready?)
    (reset! target-atom (unavailable-payload :not-wired))
    (-> (js/fetch url #js {:method "GET"
                           :credentials "omit"
                           :headers #js {"Accept" "application/json"}})
        (.then (fn [resp]
                 (if (.-ok resp)
                   (-> (.json resp)
                       (.then (fn [body]
                                (reset! target-atom
                                        (js->clj body :keywordize-keys true)))))
                   (do
                     (reset! target-atom
                             (unavailable-payload (.-status resp)))
                     (js/console.warn "[api]" label "unavailable:"
                                      (.-status resp))))))
        (.catch (fn [err]
                  (reset! target-atom (unavailable-payload 0))
                  (js/console.warn "[api]" label "unavailable:"
                                   (or (.-message err) err)))))))

(defn load-operator-bulletin! []
  (load-optional-json! (operator-bulletin-endpoint)
                       s/operator-bulletin
                       "operator bulletin"))

(defn load-forward-model! []
  (load-optional-json! (forward-model-endpoint)
                       s/operator-forward-model
                       "forward model"))

(defn- fetch-json!
  [url]
  (-> (js/fetch url #js {:method "GET"
                         :credentials "omit"
                         :headers #js {"Accept" "application/json"}})
      (.then (fn [resp]
               (if (.-ok resp)
                 (.json resp)
                 (throw (js/Error. (str "HTTP " (.-status resp)))))))))

(defn load-capability-star-map! []
  (let [static-json capability-star-map-static-path
        same-origin (capability-star-map-endpoint)
        fallback (capability-star-map-fallback-endpoint)]
    (-> (fetch-json! static-json)
        (.catch (fn [_] (fetch-json! same-origin)))
        (.catch (fn [_] (fetch-json! fallback)))
        (.then (fn [body]
                 (reset! s/capability-star-map
                         (js->clj body :keywordize-keys true))))
        (.catch (fn [err]
                  (reset! s/capability-star-map (unavailable-payload 0))
                  (js/console.warn "[api] capability star-map unavailable:"
                                   (or (.-message err) err)))))))

(defn load-pudding-status! []
  (-> (fetch-json! pudding-status-static-path)
      (.then (fn [body]
               (reset! s/pudding-status
                       (js->clj body :keywordize-keys true))))
      (.catch (fn [err]
                (reset! s/pudding-status (unavailable-payload 0))
                (js/console.warn "[api] pudding status unavailable:"
                                 (or (.-message err) err))))))

(defn load-affect-events! []
  (-> (fetch-json! affect-events-static-path)
      (.then (fn [body]
               (reset! s/affect-events
                       (js->clj body :keywordize-keys true))))
      (.catch (fn [err]
                (reset! s/affect-events (unavailable-payload 0))
                (js/console.warn "[api] affect events unavailable:"
                                 (or (.-message err) err))))))

(defn load! []
  (load-operator-bulletin!)
  (load-forward-model!)
  (load-capability-star-map!)
  (load-pudding-status!)
  (load-affect-events!)
  (go
    (let [resp (<! (http/get (endpoint-with-days)
                             {:with-credentials? false}))]
      (if (:success resp)
        (let [data (:body resp)]
          (reset! s/data data)
          ;; Days-window changes redefine the timeline, so clear cached bounds
          ;; and any prior selection before seeding replay from fresh data.
          (reset! s/waveform
                  {:selection nil :drag nil :hover-ms nil})
          (reset! s/track-ui
                  {:enabled (into {}
                                  (map (fn [session]
                                         [(:session-id session) true])
                                       (get-in data [:sessions :sessions] [])))
                   :selected nil})
          (tick/seed-from-data! data)
          (js/console.log "[api] loaded. sessions:"
                          (count (get-in data ["sessions" "sessions"] []))))
        (js/console.error "[api] load failed:" (pr-str resp))))))

(def ^:private wm-poll-ms 60000)
(defonce ^:private wm-poll-handle (atom nil))

(defn restart-wm-poll! []
  (when-let [h @wm-poll-handle]
    (js/clearInterval h))
  (reset! wm-poll-handle
          (js/setInterval (fn [] (load!)) wm-poll-ms))
  (js/console.log "[api] wm-poll cadence set to"
                  (long (/ wm-poll-ms 1000)) "s"))

(defn ensure-wm-poll! []
  (when-not @wm-poll-handle
    (restart-wm-poll!)))

(def ^:private aif-endpoint-suffix "/api/alpha/aif-stack/live")

(defn- aif-endpoint []
  (str (api-base) aif-endpoint-suffix))

;; Single source of truth for AIF cadence: the recurring AIF tick on the
;; server (M-stack-stereolithography Checkpoint 5). Each :scheduler block
;; in the response carries :period-seconds; the UI poll cadence is
;; derived from it via `desired-poll-ms`. Hard-coded 30 s polling at
;; daily AIF cadence was wildly mismatched — this keeps the UI and
;; scheduler in lockstep automatically.

(def ^:private fallback-poll-ms
  "Used when the response has no :scheduler block (older server, or the
   scheduler is not running)."
  300000) ;; 5 minutes

(def ^:private min-poll-ms 60000)        ;; never poll faster than once a minute
(def ^:private max-poll-ms 1800000)      ;; never poll slower than once every 30 min

(defn- desired-poll-ms
  "Compute UI poll cadence from the scheduler period. Aim for roughly
   12 polls per AIF cycle, clamped to [1 min, 30 min]. At hourly that's
   5 min; at daily that's 30 min (clamped from 2 h)."
  [scheduler]
  (if-let [period-s (:period-seconds scheduler)]
    (-> (/ (* period-s 1000) 12)
        long
        (max min-poll-ms)
        (min max-poll-ms))
    fallback-poll-ms))

(defonce ^:private aif-poll-handle (atom nil))
(defonce ^:private aif-poll-current-ms (atom nil))

(declare load-aif-stack!)

(defn- restart-aif-poll!
  "Restart the AIF poll interval with `new-ms`."
  [new-ms]
  (when-let [h @aif-poll-handle]
    (js/clearInterval h))
  (reset! aif-poll-handle
          (js/setInterval (fn [] (load-aif-stack!)) new-ms))
  (reset! aif-poll-current-ms new-ms)
  (js/console.log "[api] aif-poll cadence set to" (long (/ new-ms 1000)) "s"))

(defn- maybe-resync-poll!
  "If the latest scheduler info differs from our current cadence,
   re-establish the interval at the new rate."
  [scheduler]
  (let [target (desired-poll-ms scheduler)
        current @aif-poll-current-ms]
    (when (or (nil? current) (not= current target))
      (restart-aif-poll! target))))

(defn load-aif-stack! []
  (go
    (let [resp (<! (http/get (aif-endpoint)
                             {:with-credentials? false}))]
      (if (:success resp)
        (let [body (:body resp)]
          (reset! s/aif-data body)
          (maybe-resync-poll! (:scheduler body))
          (js/console.log "[api] aif-stack loaded. spine:"
                          (count (:stack-nodes body))
                          "scheduler-period-s:"
                          (or (get-in body [:scheduler :period-seconds]) "?")))
        (js/console.error "[api] aif-stack load failed:" (pr-str resp))))))

(defn ensure-aif-poll!
  "Establish the AIF poll if absent. Subsequent `load-aif-stack!` calls
   will re-sync the cadence to match the server's reported scheduler
   period."
  []
  (when-not @aif-poll-handle
    (restart-aif-poll! (or @aif-poll-current-ms fallback-poll-ms))))

(def ^:private show-in-emacs-suffix
  "/api/alpha/war-machine/show-in-emacs")

(defn- show-in-emacs-endpoint []
  (str (api-base) show-in-emacs-suffix))

(defn open-target-in-emacs!
  "POST a generic Emacs target to the bridge. Supported target shapes:

   {:kind :vsatarcs-story :leaf <name> :scene-anchor <opt>}
   {:kind :workspace-file :path <repo-relative-path>}

   Returns nothing useful synchronously; logs success/failure for inspection."
  [target]
  (when (and (map? target) (:kind target))
    (go
      (let [resp (<! (http/post (show-in-emacs-endpoint)
                                {:with-credentials? false
                                 :json-params target}))]
        (cond
          (:success resp)
          (js/console.log "[api] opened in Emacs:"
                          (clj->js (:body resp)))

          (= 503 (:status resp))
          (js/console.warn "[api] Emacs not reachable:"
                           (or (get-in resp [:body :stderr])
                               "emacsclient not on PATH"))

          (= 404 (:status resp))
          (js/console.warn "[api] Emacs target not found:"
                           (clj->js target))

          :else
          (js/console.error "[api] open-target-in-emacs failed:"
                            (pr-str resp)))))))

(defn show-in-emacs!
  "Backward-compatible wrapper for VSATARCS story leaves."
  ([leaf-name] (show-in-emacs! leaf-name nil))
  ([leaf-name scene-anchor]
   (open-target-in-emacs!
    (cond-> {:kind :vsatarcs-story
             :leaf leaf-name}
      scene-anchor (assoc :scene-anchor scene-anchor)))))
