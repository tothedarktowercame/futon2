#!/usr/bin/env bb
(require '[babashka.process :as p]
         '[clojure.edn :as edn]
         '[clojure.string :as str])

(def repo (or (System/getenv "WMPC_REPO") "/home/joe/code/futon2"))
(def manifest-path (or (System/getenv "WMPC_MANIFEST")
                       (str repo "/holes/labs/wm-contract/production-caller-manifest.edn")))

;; LIMITATION: caller discovery is textual git-grep. A runtime invocation made
;; only through a constructed name can evade it. That is acceptable in this
;; ecosystem, where production references are literal; keep this limitation at
;; the instrument boundary if that convention changes.
(defn git-grep [needle]
  (let [r (p/shell {:dir repo :out :string :err :string :continue true}
                   "git" "grep" "-l" "--" needle)]
    (cond (= 0 (:exit r)) (str/split-lines (:out r))
          (= 1 (:exit r)) []
          :else (throw (ex-info "git grep failed" {:needle needle :stderr (:err r)})))))

(defn production? [path]
  (or (str/starts-with? path "src/") (str/starts-with? path "scripts/")))

(defn inspect-claim [{:keys [needles declaration-path expected-gap] :as claim}]
  (let [hits (->> needles (mapcat git-grep) distinct sort vec)
        declaration-found? (boolean (some #{declaration-path} hits))
        callers (->> hits (remove #{declaration-path}) (filter production?) vec)
        state (cond (not declaration-found?) :declaration-not-found
                    (seq callers) :consumed
                    expected-gap :expected-gap
                    :else :missing-caller)]
    (assoc (select-keys claim [:id :basis :expected-gap])
           :declaration-found? declaration-found?
           :production-callers callers
           :state state
           :ok? (contains? #{:consumed :expected-gap} state))))

(let [manifest (edn/read-string (slurp manifest-path))
      claims (mapv inspect-claim (:claims manifest))
      failures (filterv (complement :ok?) claims)]
  (when-not (and (= :wm/production-caller-manifest-v1 (:schema manifest))
                 (seq (:claims manifest)))
    (binding [*out* *err*] (println "production_caller_check: invalid or empty manifest"))
    (System/exit 1))
  (if (seq failures)
    (do (binding [*out* *err*]
          (doseq [{:keys [id state]} failures]
            (println "production_caller_check: RED" (name id) (name state))))
        (System/exit 1))
    (do (doseq [{:keys [id state production-callers]} claims]
          (println "production_caller_check:" (name id) (name state)
                   (if (seq production-callers) (str/join "," production-callers) "named")))
        (println "production_caller_check: PASS" (count claims) "claims"))))
