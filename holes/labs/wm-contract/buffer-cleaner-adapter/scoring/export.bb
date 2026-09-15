(require '[cheshire.core :as json]
         '[clojure.edn :as edn]
         '[buffer-cleaner.classify :as c])

(def library "/home/joe/code/futon3/library/buffer-cleaner/")
(defn args [path] (edn/read-string (slurp (str library path))))
(def required-flags [:file :modified :has-process :visible :active-agent :server-clients])
(defn validate-row [raw-row]
  ;; Collector emits Emacs nil as JSON null specifically for modified.
  (let [row (if (and (contains? raw-row :modified) (nil? (:modified raw-row)))
              (assoc raw-row :modified false)
              (if (= "autosaved" (:modified raw-row))
                (assoc raw-row :modified true) raw-row))]
  (doseq [k required-flags]
    (when-not (and (contains? row k) (boolean? (get row k)))
      (throw (ex-info "missing or malformed raw protection flag" {:field k}))))
  row))
(def packet (json/parse-string (slurp "runs/state-packet.json") true))
;; Preserve every field; unlike a hand-written projection, this cannot drop flags.
(def checked (update packet :buffers #(mapv validate-row %)))
(def kill-args (args "nodes/kill.params.edn"))
(def yield-args (args "nodes/yield-settled.params.edn"))
(def categories (assoc (args "params/categories.edn") :fuel (:fuel kill-args)))
(def wirings (mapv #(args (str "wirings/" % ".edn")) ["aggressive" "conservative"]))
(def results (mapv #(c/classify-packet checked % categories) wirings))
;; Executed controls independent of the live packet.
(def protected-row {:name "control" :kind "temp" :file false :modified false
                    :has-process false :visible false :active-agent true
                    :server-clients true :display-age-seconds 90000})
(assert (= :keep (:action (c/classify-buffer (validate-row protected-row)
                                            (first wirings) categories))))
(assert (try (validate-row (dissoc protected-row :active-agent)) false
             (catch clojure.lang.ExceptionInfo _ true)))
(println (json/generate-string {:packet checked :wirings wirings
                               :kill-args kill-args :yield-args yield-args
                               :categories categories :results results
                               :controls :passed}))
