#!/usr/bin/env bb
(require '[babashka.classpath :as cp]
         '[babashka.fs :as fs]
         '[clojure.edn :as edn]
         '[clojure.pprint :as pp]
         '[clojure.set :as set]
         '[clojure.string :as str])

(def repo (System/getProperty "user.dir"))
(defn env-path [k fallback] (or (System/getenv k) (str repo "/" fallback)))
(def ns-path (env-path "F10RF_NS" "src/futon2/aif/ruled_outcome_c.clj"))
(def cohort-path (env-path "F10RF_COHORT" "src/futon2/aif/full_loop_cohort.clj"))
(def lean-path (env-path "F10RF_LEAN" "../mathlib4/DarkTower/WarMachine/F10RuledCarrier.lean"))
(def seed-path (env-path "F10RF_SEED" "holes/labs/wm-contract/runs/D1-evidence/kl-worked-example.edn"))
(def artifact (str repo "/holes/labs/wm-contract/runs/F10-outcome-domain/02-runtime-fold.edn"))

(cp/add-classpath (str repo "/src"))

(defn source-authority [s]
  (->> (second (re-find #"(?s)\(def outcome-kinds.*?(#\{.*?\})" s))
       edn/read-string set))

(defn source-retyped-zeros [s zeros]
  (set (filter #(re-find (re-pattern (str "(?<![A-Za-z0-9-])" % "(?![A-Za-z0-9-])")) s)
               zeros)))

(defn lean-all [s]
  (let [body (second (re-find #"(?s)def FlightDisposition\.all.*?\[(.*?)\]" s))]
    (set (map second (re-seq #"\.([A-Za-z][A-Za-z0-9]*)" body)))))

(defn kebab->camel [k]
  (let [[h & t] (str/split (name k) #"-")]
    (apply str h (map str/capitalize t))))

(defn callers []
  (->> ["src" "scripts"]
       (mapcat #(file-seq (fs/file repo %)))
       (filter fs/regular-file?)
       (remove #(= (str (fs/absolutize %)) (str (fs/absolutize ns-path))))
       (filter #(str/includes? (slurp %) "ruled-outcome-c"))
       (map #(str (fs/relativize repo %)))
       sort vec))

(defn fold-entry [entries id]
  (select-keys (first (filter #(= id (:layer/id %)) entries))
               [:layer/id :in-ruled-sum :folded? :basis]))

(defn facts []
  (let [ns-source (slurp ns-path)
        authority (source-authority (slurp cohort-path))
        seed (edn/read-string (slurp seed-path))
        expected (update-vals (get-in seed [:c-candidates :C-seeded]) rationalize)
        _ (load-file ns-path)
        positive @(resolve 'futon2.aif.ruled-outcome-c/seeded-positive-masses)
        seeded @(resolve 'futon2.aif.ruled-outcome-c/seeded-c)
        zeros @(resolve 'futon2.aif.ruled-outcome-c/named-zero-dispositions)
        entries @(resolve 'futon2.aif.ruled-outcome-c/fold-declaration)
        cs (callers)]
    (sorted-map
     :authority-keywords authority
     :declared-positive-masses positive
     :expected-positive-masses expected
     :mass-sum (reduce + (vals (:mass seeded)))
     :support (:support seeded)
     :support-width (count (:support seeded))
     :derived-named-zeros zeros
     :retyped-named-zeros (source-retyped-zeros ns-source zeros)
     :lean-correspondence {:runtime (set (map kebab->camel authority))
                           :lean (lean-all (slurp lean-path))}
     :measured-callers cs
     :fold-entries (mapv #(fold-entry entries %)
                         [:ruled-outcome-c :c-int :c-ser :c-mis]))))

(defn verdict [f]
  (let [folds (into {} (map (juxt :layer/id identity) (:fold-entries f)))
        ruled (get folds :ruled-outcome-c)]
    (and (= (:declared-positive-masses f) (:expected-positive-masses f))
         (= 1 (:mass-sum f))
         (= (:authority-keywords f) (:support f))
         (= (count (:authority-keywords f)) (:support-width f))
         (= (set/difference (:authority-keywords f)
                            (set (keys (:declared-positive-masses f))))
            (:derived-named-zeros f))
         (empty? (:retyped-named-zeros f))
         (= (get-in f [:lean-correspondence :runtime])
            (get-in f [:lean-correspondence :lean]))
         (= (empty? (:measured-callers f)) (false? (:folded? ruled)))
         (= #{:ruled-outcome-c :c-int :c-ser :c-mis}
            (set (map :layer/id (:fold-entries f))))
         (every? #(and (string? (:basis %)) (not (str/blank? (:basis %))))
                 (:fold-entries f))
         (= :yes (:in-ruled-sum ruled))
         (= :no (get-in folds [:c-int :in-ruled-sum]))
         (= :yes (get-in folds [:c-ser :in-ruled-sum]))
         (= :undeclared (get-in folds [:c-mis :in-ruled-sum])))))

(defn update-fold [f id k v]
  (update f :fold-entries
          #(mapv (fn [entry] (if (= id (:layer/id entry)) (assoc entry k v) entry)) %)))

(defn plant [name f mutated]
  {:plant name :landed? (not= f mutated) :verdict (verdict mutated)})

(let [f (facts)
      one-zero (first (:derived-named-zeros f))
      plants [(plant :narrow-support-to-five f
                     (assoc f :support (set (keys (:declared-positive-masses f)))
                              :support-width 5))
              (plant :change-mass f
                     (-> f (assoc-in [:declared-positive-masses :grounded-change] 1/3)
                         (assoc :mass-sum 5/6)))
              (plant :retype-one-named-zero f (assoc f :retyped-named-zeros #{one-zero}))
              (plant :claim-folded-without-callers f
                     (update-fold f :ruled-outcome-c :folded? true))
              (plant :move-c-int-into-sum f
                     (update-fold f :c-int :in-ruled-sum :yes))]
      report (sorted-map :check :F10-runtime-fold
                         :facts f
                         :plants plants
                         :verdict (and (verdict f)
                                       (every? #(and (:landed? %) (false? (:verdict %))) plants)))]
  (if (:verdict report)
    (do (fs/create-dirs (fs/parent artifact))
        (spit artifact (with-out-str (pp/pprint report))))
    (do (binding [*out* *err*] (pp/pprint report))
        (System/exit 1))))
