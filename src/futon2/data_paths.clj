(ns futon2.data-paths
  "Single authority for paths below futon2's mutable data tree.

  Production uses the canonical checkout.  Tests bind `*data-root*` to a
  fresh directory, so defaults remain hermetic even when the process cwd is
  the production checkout or a git worktree."
  (:require [clojure.java.io :as io]))

(def production-data-root "/home/joe/code/futon2/data")

(def ^:dynamic *data-root* production-data-root)

(defn path
  "Resolve PARTS beneath the currently bound futon2 data root."
  [& parts]
  (str (apply io/file *data-root* parts)))
