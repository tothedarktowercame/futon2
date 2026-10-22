(let [rid "2026-10-22-02587641-d615-4a5b-8bc8-a3df48e2ab45"
          stop (first (filter #(= rid (:run-id %)) (futon2.aif.wm.debugger/stopped)))
          _ (when-not stop (throw (ex-info "Run is not stopped" {:run-id rid})))
          value (try {:present true :value (futon2.aif.wm.debugger/stop-value rid)}
                     (catch clojure.lang.ExceptionInfo e
                       {:present false :reason (:kind (ex-data e))}))
          record {:schema :wm/live-held-section-v1 :run-id rid :stop stop
                  :result value :captured-at (str (java.time.Instant/now))
                  :source (select-keys (futon2.aif.full-loop-runner/runner-source-drift)
                           [:runner/source-check :runner/loaded-sha256 :runner/canonical-sha256])}
          encode (fn encode [x]
                   (cond
                     (keyword? x) (tagged-literal 'wm/keyword [(namespace x) (name x)])
                     (map? x) (into {} (map (fn [[k v]] [(encode k) (encode v)])) x)
                     (set? x) (set (map encode x))
                     (vector? x) (mapv encode x)
                     (sequential? x) (doall (map encode x))
                     :else x))
          text (pr-str (encode record))
          decoded (clojure.edn/read-string
                    {:readers {'wm/keyword (fn [[n k]] (keyword n k))}} text)
          _ (when-not (= record decoded)
              (throw (ex-info "Held value is not round-trippable EDN" {:run-id rid})))
          bytes (.getBytes text "UTF-8")
          result {:encoding "wm/keyword-v1" :run-id rid :phase (:phase stop) :has-value (:present value)
                  :path "/home/joe/code/futon2/holes/labs/wm-contract/runs/one-click-frontier-2026-10-22/ordinary-trial-5/selection-stop.edn.gz"
                  :sha256 (futon2.aif.interpretation-evidence/sha256 bytes)}]
      (with-open [out (java.util.zip.GZIPOutputStream.
                       (clojure.java.io/output-stream "/home/joe/code/futon2/holes/labs/wm-contract/runs/one-click-frontier-2026-10-22/ordinary-trial-5/selection-stop.edn.gz"))]
        (.write out bytes))
      (spit "/home/joe/code/futon2/holes/labs/wm-contract/runs/one-click-frontier-2026-10-22/ordinary-trial-5/selection-stop.json" (cheshire.core/generate-string result))
      result)
