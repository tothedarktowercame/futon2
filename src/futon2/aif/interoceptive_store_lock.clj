(ns futon2.aif.interoceptive-store-lock
  "Cross-process coordination for the canonical trip/repair store boundary.

  Reentrancy is restricted to the owning JVM thread and identical normalized
  lock path. The lock entry's fileKey must remain stable through the critical
  section; unlink/replacement is a typed refusal. Deployment must provision a
  lock path whose parent and entry cannot be replaced by untrusted writers."
  (:require [clojure.string :as str])
  (:import [java.nio.channels FileChannel OverlappingFileLockException]
           [java.nio.file Files LinkOption OpenOption StandardOpenOption]))

(def default-lock-path "/run/futon2/wm-interoceptive-snapshot.lock")
(def ^:dynamic *lock-path* nil)
(defonce ^:private held-locks
  (proxy [ThreadLocal] [] (initialValue [] #{})))

(defn- refuse! [reason data cause]
  (throw (ex-info "Interoceptive store coordination refused"
                  (assoc data :refusal reason) cause)))

(defn- safe-parent! [lock-path]
  (let [file (.toAbsolutePath (.normalize (.toPath (java.io.File. lock-path))))
        parent (.getParent file)]
    (when (or (nil? parent) (not (Files/isDirectory parent (make-array LinkOption 0))))
      (refuse! :interoceptive/lock-path-refused {:path lock-path :phase :parent} nil))
    ;; Check every supplied component without canonicalising through a link.
    (loop [p (.getRoot parent) names (iterator-seq (.iterator parent))]
      (when-let [name (first names)]
        (let [candidate (.resolve p name)]
          (when (Files/isSymbolicLink candidate)
            (refuse! :interoceptive/lock-path-refused
                     {:path lock-path :phase :symlink-ancestry :entry (str candidate)} nil))
          (recur candidate (next names)))))
    file))

(defn- with-lock-path [lock-path f]
  (let [path (safe-parent! lock-path)
        identity (str path)
        held (.get held-locks)]
  (if (contains? held identity)
    (f)
    (let [file (.toFile path)]
      (try
        (with-open [channel (FileChannel/open
                             (.toPath file)
                             (into-array OpenOption
                                         [StandardOpenOption/CREATE
                                          StandardOpenOption/WRITE
                                          LinkOption/NOFOLLOW_LINKS]))]
          (when-not (Files/isRegularFile path
                                         (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))
            (refuse! :interoceptive/lock-path-refused {:path lock-path} nil))
          (let [acquired (try (.tryLock channel)
                              (catch OverlappingFileLockException e
                                (refuse! :interoceptive/lock-contention
                                         {:path lock-path} e)))]
            (when-not acquired
              (refuse! :interoceptive/lock-contention {:path lock-path} nil))
          (with-open [_lock acquired]
            (let [before (Files/getAttribute path "basic:fileKey"
                                             (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))]
              (.set held-locks (conj held identity))
              (try
                (let [result (f)
                      after (Files/getAttribute path "basic:fileKey"
                                                (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))]
                  (when-not (= before after)
                    (refuse! :interoceptive/lock-identity-changed
                             {:path lock-path :before before :after after} nil))
                  result)
                (finally (.set held-locks held)))))))
        (catch OverlappingFileLockException e
          (refuse! :interoceptive/lock-contention {:path lock-path} e))
        (catch clojure.lang.ExceptionInfo e (throw e))
        (catch Throwable e
          (refuse! :interoceptive/lock-io-failure {:path lock-path} e)))))))

(defn with-store-lock [f]
  (with-lock-path (or *lock-path* default-lock-path) f))

(defn with-store-lock-for [root f]
  (let [root-file (.toFile (.normalize (.toAbsolutePath (.toPath (java.io.File. root)))))
        root-path (.getPath root-file)
        canonical-data "/home/joe/code/futon2/data/"
        path (or *lock-path*
                 (if (str/starts-with? (str root-path "/") canonical-data)
                   default-lock-path
                   (str (.getPath (.getParentFile root-file))
                        "/." (.getName root-file) ".wm-interoceptive-snapshot.lock")))]
    (with-lock-path path f)))
