(ns wm-loaded-code-probe
  "Read-only source inventory and serving-JVM namespace probe. Never reloads."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.pprint :as pp]
            [clojure.string :as str])
  (:import [java.security MessageDigest]))

(defn sha256 [file]
  (with-open [in (io/input-stream file)]
    (let [digest (MessageDigest/getInstance "SHA-256")
          buf (byte-array 8192)]
      (loop []
        (let [n (.read in buf)]
          (when (pos? n) (.update digest buf 0 n) (recur))))
      (apply str (map #(format "%02x" (bit-and 255 %)) (.digest digest))))))

(defn git [repo & args]
  (let [r (apply shell/sh (concat ["git" "-C" repo] args))]
    (when-not (zero? (:exit r)) (throw (ex-info "Git probe failed" r)))
    (str/trim (:out r))))

(defn source-path? [path]
  (boolean (re-matches #"(?:src/|scripts/futon2/).*\.clj" path)))

(defn lib-names [spec]
  (cond
    (symbol? spec) [spec]
    (sequential? spec)
    (let [[prefix & more] spec]
      (if (and (symbol? prefix) (seq more) (not (keyword? (first more))))
        (mapcat #(map (fn [s] (symbol (str prefix "." s))) (lib-names %)) more)
        (when (symbol? prefix) [prefix])))
    :else []))

(defn source-info [file]
  ;; Read only the namespace form; no source is evaluated or required.
  (with-open [r (java.io.PushbackReader. (io/reader file))]
    (binding [*read-eval* false]
      (loop []
        (let [form (read {:eof ::eof :read-cond :allow :features #{:clj}} r)]
          (cond
            (= ::eof form) nil
            (and (seq? form) (= 'ns (first form)))
            {:namespace (second form)
             :dependencies (set (mapcat (fn [clause] (mapcat lib-names (rest clause)))
                                        (filter #(and (seq? %) (#{:require :use} (first %)))
                                                (drop 2 form))))}
            :else (recur)))))))

(defn dependency-order [graph selected]
  (letfn [(visit [n active [seen _ :as state]]
            (cond
              (contains? active n) (throw (ex-info "Namespace dependency cycle" {:namespace n :active active}))
              (contains? seen n) state
              :else (let [[seen ordered]
                          (reduce (fn [s dep] (visit dep (conj active n) s)) state
                                  (sort (filter #(contains? graph %) (get graph n))))]
                      [(conj seen n) (conj ordered n)])))]
    (filterv (set selected)
             (second (reduce #(visit %2 #{} %1) [#{} []] (sort selected))))))

(defn read-only-form [names]
  ;; Core/JDK only: no require, intern, def, atom swap or application calls.
  (str
   "(let [names '" (pr-str names) "] "
   "{:captured-at (str (java.time.Instant/now)) "
   ":jvm-start-ms (.getStartTime (java.lang.management.ManagementFactory/getRuntimeMXBean)) "
   ":classpath (System/getProperty \"java.class.path\") "
   ":namespaces (into {} (for [s names] "
   "[s (if-let [n (find-ns s)] "
   "{:loaded? true :interns (vec (sort (keys (ns-interns n))))} {:loaded? false})])) "
   ":tripwire-baseline (when-let [n (find-ns 'futon2.aif.tripwire)] "
   "(when-let [v (ns-resolve n 'composition-baseline)] "
   "(when (bound? v) (into {} (for [[s x] @(var-get v)] "
   "[s (select-keys x [:source-path :source-sha256])])))))})"))

(defn classify [disk loaded]
  ;; A present namespace or a current resource digest is NOT loaded-byte identity.
  (assoc disk :loaded? (:loaded? loaded)
         :loaded-current? :unknown
         :reason (if (:loaded? loaded) :no-load-time-source-identity :namespace-not-loaded)))

(defn probe [repo since proof-eval]
  (when-not (seq since)
    (throw (ex-info "Supply --since: no trustworthy whole-JVM loaded commit is recorded"
                    {:kind :loaded-base-unavailable})))
  (let [base (git repo "rev-parse" "--verify" (str since "^{commit}"))
        head (git repo "rev-parse" "HEAD")
        tracked (filter source-path? (str/split-lines (git repo "ls-files")))
        inventory (keep (fn [path]
                          (when (.isFile (io/file repo path))
                            (when-let [info (source-info (io/file repo path))]
                              (assoc info :path path)))) tracked)
        changed (set (filter source-path?
                             (concat (str/split-lines (git repo "log" "--format=" "--name-only" (str base ".." head) "--" "src" "scripts/futon2"))
                                     (str/split-lines (git repo "diff" "--name-only" "HEAD" "--" "src" "scripts/futon2")))))
        selected (filter #(contains? changed (:path %)) inventory)
        graph (into {} (map (juxt :namespace :dependencies)) inventory)
        order (dependency-order graph (map :namespace selected))
        before-hashes (into {} (map (fn [info] [(:path info) (sha256 (io/file repo (:path info)))])) selected)
        form (read-only-form order)
        response (shell/sh proof-eval "-" :in form :dir (str (.getParentFile (.getParentFile (io/file proof-eval)))))
        envelope (when (zero? (:exit response)) (edn/read-string (:out response)))
        _ (when-not (true? (:ok envelope)) (throw (ex-info "Serving JVM probe failed" {:response response})))
        live (:value envelope)
        _ (when-not (and (= head (git repo "rev-parse" "HEAD"))
                         (every? (fn [[path digest]] (= digest (sha256 (io/file repo path)))) before-hashes))
            (throw (ex-info "Source changed during probe; retry" {:kind :source-drift-during-probe})))
        rows (mapv (fn [n]
                     (let [info (first (filter #(= n (:namespace %)) selected))]
                       (classify (assoc (select-keys info [:namespace :path]) :disk-sha256 (get before-hashes (:path info)))
                                 (get-in live [:namespaces n])))) order)]
    {:schema :wm/loaded-code-probe-v1 :repo repo :since base :head head
     :base-authority :operator-supplied-not-a-loaded-commit
     :identity-limit :classpath-resource-is-current-disk-not-loaded-bytecode
     :unrepresented-paths (vec (sort (remove (set (map :path selected)) changed)))
     :namespaces rows :live live :probe-form form
     :reload-order order
     :reload-forms (mapv #(str "(require '" % " :reload)") order)
     :post-reload-verification-form form
     :verification-limit :namespace-presence-only-no-retroactive-byte-identity}))

(defn -main [& args]
  (let [opts (apply hash-map args)]
    (pp/pprint (probe (or (get opts "--repo") "/home/joe/code/futon2")
                       (get opts "--since")
                       (or (get opts "--proof-eval") "/home/joe/code/futon3c/scripts/proof-eval.sh")))
    (shutdown-agents)))
