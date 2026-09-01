#!/usr/bin/env bb
(ns checks.repository-census-basis-check
  (:require [babashka.process :as process]
            [clojure.edn :as edn]
            [clojure.set :as set]
            [clojure.string :as str]))

(def registry-path "checks/repository-census-bases.edn")
(def roots {:futon2 "/home/joe/code/futon2"
            :futon3 "/home/joe/code/futon3"
            :futon3c "/home/joe/code/futon3c"
            :mathlib4 "/home/joe/code/mathlib4"
            :p4ng "/home/joe/code/p4ng"})

(defn git [root & argv]
  (apply process/shell {:continue true :out :string :err :string}
         "git" "-C" root argv))

(def source-token
  #"`((?:[A-Za-z0-9_.-]+/)*[A-Za-z0-9_.-]+\.(?:clj|bb|py|sh|edn|json|tex|lean)(?::[0-9–, -]+)?)`")

(defn tracked-paths [repo]
  (-> (git (roots repo) "ls-files") :out str/split-lines set))

(defn strip-lines [token]
  (str/replace token #":[0-9][0-9–, -]*$" ""))

(defn resolve-token [tracked token]
  (let [token (strip-lines token)
        explicit (some (fn [[repo _]]
                         (let [prefix (str (name repo) "/")]
                           (when (str/starts-with? token prefix)
                             {:repo repo :path (subs token (count prefix))}))) roots)
        direct (when-not explicit
                 (for [[repo paths] tracked :when (contains? paths token)]
                   {:repo repo :path token}))
        by-name (when (and (not explicit) (empty? direct) (not (str/includes? token "/")))
                  (for [[repo paths] tracked, path paths
                        :when (= token (last (str/split path #"/")))]
                    {:repo repo :path path}))
        candidates (vec (distinct (concat (when explicit [explicit]) direct by-name)))]
    (cond
      (= 1 (count candidates)) {:status :resolved :subject (first candidates)}
      (empty? candidates) {:status :unresolved :token token}
      :else {:status :ambiguous :token token :candidates candidates})))

(defn derive-subjects [artifact]
  (let [tracked (into {} (map (fn [[repo _]] [repo (tracked-paths repo)]) roots))
        tokens (distinct (map second (re-seq source-token (slurp artifact))))
        resolutions (mapv #(resolve-token tracked %) tokens)]
    {:subjects (vec (distinct (keep :subject resolutions)))
     :unresolved (filterv #(not= :resolved (:status %)) resolutions)}))

(defn entry-result [{:keys [artifact kind basis subjects] :as entry}]
  (let [shape? (and (string? artifact) (contains? #{:audit :census} kind)
                    (map? basis) (seq basis) (vector? subjects) (seq subjects))
        artifact? (and shape? (zero? (:exit (git (:futon2 roots) "ls-files" "--error-unmatch" artifact))))
        derivation (when artifact? (derive-subjects artifact))
        declared-set (set subjects)
        derived-set (set (:subjects derivation))
        undeclared (set/difference derived-set declared-set)
        uncited (set/difference declared-set derived-set)
        failures
        (if-not shape?
          [{:reason :malformed-entry :entry entry}]
          (vec
           (concat
            (when-not (zero? (:exit (git (:futon2 roots) "ls-files" "--error-unmatch" artifact)))
              [{:reason :artifact-unavailable :artifact artifact}])
            (for [[repo commit] basis
                  :let [root (get roots repo)]
                  :when (or (nil? root)
                            (not (string? commit))
                            (not (zero? (:exit (git (or root (:futon2 roots))
                                                    "cat-file" "-e" (str commit "^{commit}"))))))]
              {:reason :basis-unavailable :repo repo :commit commit})
            (for [{:keys [repo path]} subjects
                  :let [root (get roots repo)]
                  :when (or (nil? root) (not (string? path))
                            (not (zero? (:exit (git (or root (:futon2 roots))
                                                    "ls-files" "--error-unmatch" (or path ""))))))]
              {:reason :subject-unavailable :repo repo :path path})
            ;; A subject in a repo the basis does not pin has no commit to diff
            ;; against.  Without this, `git diff nil HEAD` exits nonzero and the
            ;; subject is reported as MOVED -- a wrong answer that looks like a
            ;; right one, since "possibly-stale" is exactly what a real move says.
            (for [{:keys [repo path]} subjects
                  :when (and (get roots repo) (nil? (get basis repo)))]
              {:reason :subject-repo-not-pinned-by-basis :repo repo :path path})
            (for [subject undeclared]
              {:reason :derived-subject-undeclared :subject subject})
            (for [subject uncited]
              {:reason :declared-subject-not-cited :subject subject})
            (for [resolution (:unresolved derivation)]
              {:reason :cited-subject-unresolved :citation resolution})
            (for [{:keys [repo path]} derived-set
                  :when (nil? (get basis repo))]
              {:reason :cited-repo-not-pinned-by-basis :repo repo :path path}))))
        moved
        (when (empty? failures)
          (vec
           (for [{:keys [repo path]} subjects
                 :let [root (roots repo) commit (basis repo)
                       diff (git root "diff" "--quiet" commit "HEAD" "--" path)]
                 :when (not (zero? (:exit diff)))]
             {:repo repo :path path :basis commit})))]
    {:artifact artifact
     :status (cond (seq failures) :unavailable (seq moved) :possibly-stale :else :current)
     :failures failures :moved-subjects moved
     :derived-subjects (vec (sort-by (juxt :repo :path) derived-set))
     :declared-subjects (vec (sort-by (juxt :repo :path) declared-set))}))

(defn evaluate [registry]
  (if-not (and (= :repository-census-bases/v1 (:schema registry))
               (vector? (:entries registry)))
    {:status :unavailable :failures [{:reason :malformed-registry}] :entries []}
    (let [entries (mapv entry-result (:entries registry))]
      {:status (if (some #(= :unavailable (:status %)) entries) :unavailable :available)
       :entries entries})))

(defn -main [& args]
  (let [negative? (some #{"--negative-control"} args)
        registry0 (edn/read-string (slurp registry-path))
        registry (if negative?
                   (update-in registry0 [:entries 0 :subjects] #(vec (rest %)))
                   registry0)
        result (evaluate registry)
        unavailable? (= :unavailable (:status result))]
    (println "repository-census-basis-check:" (pr-str result))
    (System/exit (if negative?
                   (if unavailable? 0 2)
                   (if unavailable? 1 0)))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
