(ns futon2.aif.interoceptive-store-lock
  "Cross-process coordination for the canonical trip/repair store boundary."
  (:require [clojure.string :as str])
  (:import [java.nio.channels FileChannel OverlappingFileLockException]
           [java.nio.file Files LinkOption OpenOption StandardOpenOption]))

(def default-lock-path "/home/joe/code/futon2/data/.wm-interoceptive-snapshot.lock")
(def ^:dynamic *lock-path* nil)
(def ^:dynamic *lock-held?* false)

(defn- refuse! [reason data cause]
  (throw (ex-info "Interoceptive store coordination refused"
                  (assoc data :refusal reason) cause)))

(defn- with-lock-path [lock-path f]
  (if *lock-held?*
    (f)
    (let [file (java.io.File. lock-path)
          parent (.getCanonicalFile (.getParentFile file))]
      (when-not (and (.isDirectory parent)
                     (not (Files/isSymbolicLink (.toPath parent)))
                     (= parent (.getCanonicalFile (.getParentFile file))))
        (refuse! :interoceptive/lock-path-refused {:path lock-path} nil))
      (try
        (with-open [channel (FileChannel/open
                             (.toPath file)
                             (into-array OpenOption
                                         [StandardOpenOption/CREATE
                                          StandardOpenOption/WRITE
                                          LinkOption/NOFOLLOW_LINKS]))]
          (when-not (Files/isRegularFile (.toPath file)
                                         (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))
            (refuse! :interoceptive/lock-path-refused {:path lock-path} nil))
          (with-open [_lock (.lock channel)]
            (binding [*lock-held?* true] (f))))
        (catch OverlappingFileLockException e
          (refuse! :interoceptive/lock-contention {:path lock-path} e))
        (catch clojure.lang.ExceptionInfo e (throw e))
        (catch Throwable e
          (refuse! :interoceptive/lock-io-failure {:path lock-path} e))))))

(defn with-store-lock [f]
  (with-lock-path (or *lock-path* default-lock-path) f))

(defn with-store-lock-for [root f]
  (let [root-path (.getCanonicalPath (java.io.File. root))
        canonical-data "/home/joe/code/futon2/data/"
        path (or *lock-path*
                 (if (str/starts-with? (str root-path "/") canonical-data)
                   default-lock-path
                   (str root-path "/.wm-interoceptive-snapshot.lock")))]
    (with-lock-path path f)))
