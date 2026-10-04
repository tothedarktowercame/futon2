(ns futon2.aif.artifact-read-observation
  "Append-only observations made only by a consumer's actual artifact read."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import [java.nio.file Files StandardOpenOption]
           [java.security MessageDigest]
           [java.time Instant]
           [java.util UUID]))

(def schema :wm/artifact-read-observation-v1)
(def default-root "/home/joe/code/futon2/data/wm-artifact-reads")

(defn sha256 [^bytes bytes]
  (format "%064x" (BigInteger. 1 (.digest (doto (MessageDigest/getInstance "SHA-256")
                                            (.update bytes))))))

(defn read-artifact!
  "Read bytes first, verify their declared SHA, optionally parse EDN, then append
   the observation. Merely registering or generating PATH calls none of this."
  ([authority] (read-artifact! default-root authority))
  ([root {:keys [consumer path content-sha256 producer-run producer-click
                 producer-commit purpose parse] :as authority}]
   (when-not (and (every? #(and (string? %) (seq %))
                          [consumer path content-sha256 producer-commit purpose])
                  (or (and (string? producer-run) (seq producer-run))
                      (and (string? producer-click) (seq producer-click))))
     (throw (ex-info "Artifact read authority is incomplete"
                     {:reason :artifact-read-identity-missing :authority authority})))
   (let [bytes (Files/readAllBytes (.toPath (io/file path)))
         actual (sha256 bytes)]
     (when-not (= content-sha256 actual)
       (throw (ex-info "Artifact bytes differ from producer authority"
                       {:reason :artifact-sha-drift :expected content-sha256 :actual actual})))
     (let [value (case parse
                   :edn (edn/read-string (String. bytes "UTF-8"))
                   :bytes bytes
                   nil bytes
                   (throw (ex-info "Unknown artifact parser" {:parse parse})))
           id (str "artifact-read-" (UUID/randomUUID))
           receipt {:schema schema :observation-id id :consumer consumer
                    :artifact {:path (.getCanonicalPath (io/file path))
                               :content-sha256 actual}
                    :producer {:run producer-run :click producer-click
                               :commit producer-commit}
                    :purpose purpose :parse (or parse :bytes)
                    :observed-at (str (Instant/now))}
           file (io/file root (str id ".edn"))]
       (io/make-parents file)
       (Files/write (.toPath file) (.getBytes (pr-str receipt) "UTF-8")
                    (into-array StandardOpenOption
                                [StandardOpenOption/CREATE_NEW StandardOpenOption/WRITE]))
       {:value value :receipt receipt}))))

(defn receipts
  ([] (receipts default-root))
  ([root]
   (->> (or (.listFiles (io/file root)) [])
        (filter #(.isFile %))
        (filter #(re-matches #"artifact-read-.*\.edn" (.getName %)))
        (map #(edn/read-string (slurp %)))
        vec)))

(defn usage-projection
  ([] (usage-projection default-root))
  ([root]
   (->> (receipts root)
        (group-by (juxt #(get-in % [:artifact :content-sha256]) :consumer))
        (map (fn [[[product consumer] rows]]
               {:product-sha256 product :consumer consumer :observed-reads (count rows)}))
        (sort-by (juxt :product-sha256 :consumer))
        vec)))
