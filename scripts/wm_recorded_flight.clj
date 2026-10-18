(ns wm-recorded-flight
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [futon2.aif.wm.recorded-flight :as flight]))

(defn -main [& [manifest-path output-root]]
  (when-not (and manifest-path output-root)
    (throw (ex-info "usage: clojure -M -m wm-recorded-flight MANIFEST.edn OUTPUT_ROOT" {})))
  (pprint/pprint
   (flight/run-recorded! {:manifest (edn/read-string (slurp manifest-path))
                          :output-root output-root})))
