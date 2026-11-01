(ns futon2.aif.injury-ticket-join
  "Resolve inline and append-only injury provenance without rewriting findings."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.load-identity :as identity]))

(def join-keys [:injury-id :cause-record-pin :origin-run/id])

(defn attachment-id [injury-id finding-id]
  (identity/sha256 (.getBytes (pr-str [injury-id finding-id]) "UTF-8")))

(defn resolve-join
  "A replaced finding cannot borrow an attachment. No join is a typed absence;
  conflicting or malformed provenance is refused, never picked by file order."
  [root finding-id]
  (let [file (io/file root "findings" (str finding-id ".edn"))
        bytes (java.nio.file.Files/readAllBytes (.toPath file))
        finding (edn/read-string (String. bytes "UTF-8"))
        sha (identity/sha256 bytes)
        inline (when (:injury-id finding) (select-keys finding join-keys))
        attachments (->> (.listFiles (io/file root "injury-joins"))
                         (filter #(.endsWith (.getName %) ".edn"))
                         (map #(edn/read-string (slurp %)))
                         (filter #(= finding-id (:finding/id %))))
        stale (some #(not= sha (:finding-sha256 %)) attachments)
        joins (distinct (concat (when inline [inline])
                                (map #(select-keys % join-keys) attachments)))]
    (cond
      stale {:status :absent :reason :injury-join-finding-sha-mismatch}
      (empty? joins) {:status :absent :reason :injury-join-not-retained}
      (> (count joins) 1) {:status :refused :reason :injury-join-conflict}
      :else {:status :present :join (first joins) :finding-sha256 sha})))
