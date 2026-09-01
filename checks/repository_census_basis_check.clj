#!/usr/bin/env bb
(ns checks.repository-census-basis-check
  (:require [babashka.process :as process]
            [clojure.edn :as edn]))

(def registry-path "checks/repository-census-bases.edn")
(def roots {:futon2 "/home/joe/code/futon2"
            :futon3 "/home/joe/code/futon3"
            :futon3c "/home/joe/code/futon3c"
            :mathlib4 "/home/joe/code/mathlib4"
            :p4ng "/home/joe/code/p4ng"})

(defn git [root & argv]
  (apply process/shell {:continue true :out :string :err :string}
         "git" "-C" root argv))

(defn entry-result [{:keys [artifact kind basis subjects] :as entry}]
  (let [shape? (and (string? artifact) (contains? #{:audit :census} kind)
                    (map? basis) (seq basis) (vector? subjects) (seq subjects))
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
              {:reason :subject-unavailable :repo repo :path path}))))
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
     :failures failures :moved-subjects moved}))

(defn evaluate [registry]
  (if-not (and (= :repository-census-bases/v1 (:schema registry))
               (vector? (:entries registry)))
    {:status :unavailable :failures [{:reason :malformed-registry}] :entries []}
    (let [entries (mapv entry-result (:entries registry))]
      {:status (if (some #(= :unavailable (:status %)) entries) :unavailable :available)
       :entries entries})))

(defn -main [& args]
  (let [negative? (some #{"--negative-control"} args)
        registry (if negative?
                   {:schema :repository-census-bases/v1
                    :entries [{:artifact nil :kind :census :basis {} :subjects []}]}
                   (edn/read-string (slurp registry-path)))
        result (evaluate registry)
        unavailable? (= :unavailable (:status result))]
    (println "repository-census-basis-check:" (pr-str result))
    (System/exit (if negative?
                   (if unavailable? 0 2)
                   (if unavailable? 1 0)))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
