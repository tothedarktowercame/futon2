(ns futon2.aif.wm.cascade-patch-delivery
  "Read-only delivery gate before a cascade repair is sent for review."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [futon2.aif.load-identity :as identity]))

(def canonical-repository "/home/joe/code/futon2")

(defn- git [root & args]
  (apply shell/sh "git" "-C" root args))

(defn- digest [file]
  (some-> (identity/read-bytes file) identity/sha256))

(defn- locations [root relative expected]
  (let [worktrees (->> (str/split-lines (:out (git root "worktree" "list" "--porcelain")))
                      (keep #(when (str/starts-with? % "worktree ") (subs % 9))))]
    (vec (for [dir (distinct (cons root worktrees))
               :let [file (io/file dir relative)]
               :when (and (.isFile file) (= expected (digest file)))]
           {:repository dir :path (.getCanonicalPath file)
            :source-commit (str/trim (:out (git dir "log" "-1" "--format=%H" "HEAD" "--" relative)))}))))

(defn check-delivery
  "Prove the exact reviewed bytes are committed on canonical HEAD and visible
  from its serving resource. Legacy descriptors resolve their source commit
  from file history; a supplied commit is checked explicitly. Never integrates
  a sibling worktree, loads a namespace, or contacts a reviewer."
  ([answer] (check-delivery canonical-repository io/resource answer))
  ([root resource-fn answer]
   (let [{:keys [namespace source-digest commit]} (:patch answer)
         valid? (and (string? namespace)
                     (re-matches #"[A-Za-z0-9_.-]+" namespace)
                     (string? source-digest)
                     (re-matches #"[a-f0-9]{64}" source-digest))
         resource-name (when valid? (str (-> namespace (str/replace "." "/")
                                                           (str/replace "-" "_")) ".clj"))
         relative (when valid? (str "src/" resource-name))
         found (if valid? (locations root relative source-digest) [])
         canonical (first (filter #(= (.getCanonicalPath (io/file root)) (:repository %)) found))
         source-commit (or commit (:source-commit canonical))
         commit-valid? (and (string? source-commit) (re-matches #"[a-f0-9]{40}" source-commit))
         reachable? (and commit-valid? (zero? (:exit (git root "merge-base" "--is-ancestor" source-commit "HEAD"))))
         blob (when reachable? (git root "show" (str source-commit ":" relative)))
         committed? (and blob (zero? (:exit blob))
                         (= source-digest (identity/sha256 (.getBytes ^String (:out blob) "UTF-8"))))
         resource (when valid? (resource-fn resource-name))
         resource-path (when (and resource (= "file" (.getProtocol ^java.net.URL resource)))
                         (.getCanonicalPath (io/file (.toURI ^java.net.URL resource))))
         delivered? (and canonical reachable? committed?
                         (= (:path canonical) resource-path)
                         (= source-digest (digest resource)))
         reason (cond (not valid?) :invalid-patch-descriptor
                      (nil? canonical) :source-not-in-canonical-repository
                      (not reachable?) :commit-not-on-canonical-head
                      (not committed?) :committed-source-mismatch
                      (not= (:path canonical) resource-path) :serving-resource-not-canonical
                      :else :serving-source-mismatch)]
     (cond-> {:status (if delivered? :delivered :error)
              :canonical-repository root :namespace namespace
              :source-digest source-digest :source-commit source-commit
              :found-locations found :serving-source resource-path}
       (not delivered?) (assoc :kind :author-delivery-wrong-repository :reason reason)))))
