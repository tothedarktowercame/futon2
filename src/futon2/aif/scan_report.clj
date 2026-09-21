(ns futon2.aif.scan-report
  "Retain a rendering of this tick's supplied scan and judgement, without scanning."
  (:require [futon2.aif.load-identity :as load-identity]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.nio.charset StandardCharsets]
           [java.nio.file Files StandardCopyOption]
           [java.security MessageDigest]
           [java.util UUID]))

(load-identity/register! *ns* *file*)

(defn- absence [reason error]
  (cond-> {:status :absent :reason reason}
    error (assoc :error {:class (.getName (class error))
                         :message (.getMessage ^Throwable error)})))

(defn retain!
  "Render INPUT once and retain UTF-8 Markdown next to RECORD-PATH.
  Rendering and evidence IO failures are typed absences, never decision gates."
  [record-path input renderer]
  (if-not input
    (absence :scan-input-unavailable nil)
    (let [rendered (try {:text (renderer input)}
                        (catch Exception e {:absence (absence :render-failed e)}))]
      (cond
        (:absence rendered) (:absence rendered)
        (not (string? (:text rendered))) (absence :rendered-text-not-a-string nil)
        (str/blank? (:text rendered)) (absence :rendered-text-empty nil)
        :else
        (let [target (io/file (str (str/replace (str record-path) #"\.edn$" "") ".scan.md"))
              tmp (io/file (.getParentFile target)
                           (str "." (.getName target) "." (UUID/randomUUID) ".tmp"))]
          (try
            (let [bytes (.getBytes ^String (:text rendered) StandardCharsets/UTF_8)
                  sha (apply str (map #(format "%02x" (bit-and 255 %))
                                      (.digest (MessageDigest/getInstance "SHA-256") bytes)))]
              (io/make-parents target)
              (with-open [out (io/output-stream tmp)] (.write out bytes))
              (Files/move (.toPath tmp) (.toPath target)
                          (into-array StandardCopyOption
                                      [StandardCopyOption/ATOMIC_MOVE StandardCopyOption/REPLACE_EXISTING]))
              {:status :present :path (.getAbsolutePath target) :sha256 sha
               :encoding :utf-8 :format :markdown :bytes (alength bytes)})
            (catch Exception e (absence :write-failed e))
            (finally (io/delete-file tmp true))))))))
