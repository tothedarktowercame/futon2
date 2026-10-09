(ns wm-lane-size-replay
  "Offline serializer replay for the click-49 lane-carriage shape."
  (:require [clojure.java.io :as io]
            [futon2.aif.wm.cascade-decision :as decision]))

(defn parse-long-arg [args flag default]
  (if-let [i (.indexOf args flag)]
    (Long/parseLong (nth args (inc i)))
    default))

(defn -main [& args]
  (let [targets (parse-long-arg args "--targets" 420)
        drops-n (parse-long-arg args "--drops" 4963)
        legacy? (some #{"--legacy"} args)
        lanes (mapv (fn [n] {:target (str "T-" n)
                              :candidates [{:id (keyword (str "p-" n))}]})
                    (range targets))
        drops (mapv (fn [n] {:target (str "T-" (mod n targets))
                              :candidate (keyword (str "declined-" n))
                              :reason :class-unknown-no-scalar-g
                              :possible-costs (mapv #(str "cost-" n "-" %)
                                                    (range 24))})
                    (range drops-n))
        carried (if legacy?
                  (mapv #(assoc % :dropped-candidates drops) lanes)
                  (decision/lane-local-dropped-candidates lanes drops))
        record {:decision {:cascade-lanes carried
                           :selection-certificate {:dropped-candidates drops}}}
        root (.toFile (java.nio.file.Files/createTempDirectory
                       "wm-lane-size-" (make-array java.nio.file.attribute.FileAttribute 0)))
        out (io/file root "tick-run-record-replay.edn")
        started (System/nanoTime)]
    ;; Same allocation/write boundary as full-loop-runner/persist-run-record!.
    (spit out (str (pr-str record) "\n"))
    (println (pr-str {:mode (if legacy? :legacy-family-per-lane :target-local)
                      :targets targets :drops drops-n :bytes (.length out)
                      :close-ms (/ (- (System/nanoTime) started) 1e6)
                      :path (.getAbsolutePath out)}))))
