(let [rid "2026-10-22-02587641-d615-4a5b-8bc8-a3df48e2ab45"
          stop (first (filter #(= rid (:run-id %)) (futon2.aif.wm.debugger/stopped)))
          _ (when-not stop (throw (ex-info "Run is not stopped" {:run-id rid})))
          value (try {:present true :value (futon2.aif.wm.debugger/stop-value rid)}
                     (catch clojure.lang.ExceptionInfo e
                       {:present false :reason (:kind (ex-data e))}))
          record {:schema :wm/live-held-section-v1 :run-id rid :stop stop
                  :result value :printer {:namespace-maps *print-namespace-maps*
                                          :meta *print-meta* :length *print-length* :level *print-level*}
                  :captured-at (str (java.time.Instant/now))
                  :source (select-keys (futon2.aif.full-loop-runner/runner-source-drift)
                           [:runner/source-check :runner/loaded-sha256 :runner/canonical-sha256])}
          encode (fn encode [x]
                   (cond
                     (keyword? x) (tagged-literal 'wm/keyword [(namespace x) (name x)])
                     (map? x) (tagged-literal 'wm/map (mapv (fn [[k v]] [(encode k) (encode v)]) x))
                     (set? x) (tagged-literal 'wm/set (mapv encode x))
                     (vector? x) (mapv encode x)
                     (sequential? x) (doall (map encode x))
                     :else x))
          order-comparator (fn [xs]
                             (let [positions (zipmap xs (range))]
                               (fn [a b]
                                 (if (= a b) 0
                                   (compare [(get positions a (count positions)) (pr-str a)]
                                            [(get positions b (count positions)) (pr-str b)])))))
          readers {'wm/keyword (fn [[n k]] (keyword n k))
                    'wm/map (fn [pairs]
                              (into (sorted-map-by (order-comparator (mapv first pairs))) pairs))
                    'wm/set (fn [xs] (into (sorted-set-by (order-comparator xs)) xs))}
          text (pr-str (encode record))
          decoded (clojure.edn/read-string
                    {:readers readers} text)
          _ (when-not (= record decoded)
              (throw (ex-info "Held value is not round-trippable EDN" {:run-id rid})))
          _ (when-not (= (pr-str record) (pr-str decoded))
              (throw (ex-info "Held value lost print order" {:run-id rid})))
          bytes (.getBytes text "UTF-8")
          result {:encoding "wm/ordered-edn-v2" :print-roundtrip true :run-id rid :phase (:phase stop) :has-value (:present value)
                  :path "/home/joe/code/futon2/holes/labs/wm-contract/runs/one-click-frontier-2026-10-22/ordinary-trial-5/construction.edn.gz"
                  :sha256 (futon2.aif.interpretation-evidence/sha256 bytes)}]
      (with-open [out (java.util.zip.GZIPOutputStream.
                       (clojure.java.io/output-stream "/home/joe/code/futon2/holes/labs/wm-contract/runs/one-click-frontier-2026-10-22/ordinary-trial-5/construction.edn.gz"))]
        (.write out bytes))
      (spit "/home/joe/code/futon2/holes/labs/wm-contract/runs/one-click-frontier-2026-10-22/ordinary-trial-5/construction.json" (cheshire.core/generate-string result))
      result)
