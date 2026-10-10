(ns futon2.aif.run-record-io
  "The one way to read a War Machine run record (tick-run-record-<id>.edn).

  Run records are written with the large nested values under :decision and
  :world-at-selection interned (futon2.aif.durable-intern), so a reader must
  hydrate before looking inside them; top-level fields are never interned.
  The file starts with one EDN comment line, `;; wm/run-head {...}`, holding
  the record's :run/id and :startedAt, so a caller that only needs those
  (previous-run ordering) reads one line instead of the whole record. EDN
  readers skip comments, so older readers are unaffected by it.

  No deftype here: babashka scripts load this namespace too."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.durable-hydrate :as durable-hydrate]))

(def head-prefix ";; wm/run-head ")

(def head-keys
  "Top-level fields carried on the head line."
  [:run/id :click/id :startedAt])

(defn head-line
  "The comment line written ahead of RECORD."
  [record]
  (str head-prefix (pr-str (select-keys record head-keys)) "\n"))

(defn read-record
  "Parse the run record at PATH (a path or File) by streaming, and hydrate it.
  OPTS are clojure.edn/read options; :default defaults to tagged-literal."
  ([path] (read-record path {}))
  ([path opts]
   (with-open [r (java.io.PushbackReader. (io/reader (io/file path)))]
     (durable-hydrate/hydrate
      (edn/read (merge {:default tagged-literal} opts) r)))))

(defn read-record-string
  "Parse and hydrate a run record already held as TEXT (for callers that
  digest the exact bytes they parse)."
  ([text] (read-record-string text {}))
  ([text opts]
   (durable-hydrate/hydrate
    (edn/read-string (merge {:default tagged-literal} opts) text))))

(defn read-head
  "The head-line map of the run record at PATH, or, for a record written
  before head lines existed, the same keys read from the whole record."
  [path]
  (let [line (with-open [r (io/reader (io/file path))]
               (first (line-seq r)))]
    (if (and line (str/starts-with? line head-prefix))
      (edn/read-string {:default tagged-literal} (subs line (count head-prefix)))
      (select-keys (read-record path) head-keys))))
