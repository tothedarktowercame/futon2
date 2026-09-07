#!/usr/bin/env bb
(require '[babashka.classpath :as cp]
         '[babashka.fs :as fs]
         '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[clojure.set :as set]
         '[clojure.string :as str])

(def repo "/home/joe/code/futon2")
(defn envv [k fallback] (or (System/getenv k) fallback))
(def declaration-path (envv "F10LR_DECL" (str repo "/src/futon2/aif/ruled_outcome_c.clj")))
(def trace-path (envv "F10LR_TRACE" (str repo "/data/wm-trace/wm-trace-2026-09-07.edn")))
(def artifact-path (envv "F10LR_ARTIFACT" (str repo "/holes/labs/wm-contract/runs/F10-outcome-domain/03-live-run.edn")))
(def expected-trace-sha (envv "F10LR_TRACE_SHA" "71bc68e5504f850e8f1dfac0b76ae3292b0a78f3e28a5f552dde5b55901d3381"))
(def expected-sha "3f1ef0d7827bc03bc96e95506d10c543ef48ee3b")
(def expected-run-ids #{"85cb5a19-d053-4aa7-a7ad-f667f5e4321d"
                        "f24ccb9f-50ca-4654-aa3c-bedfe66eeaea"
                        "36820e88-3d68-499d-b359-2d8dbe9743de"})

(cp/add-classpath (str repo "/src"))

(defn sha256 [path]
  (let [md (java.security.MessageDigest/getInstance "SHA-256")]
    (with-open [in (java.io.FileInputStream. path)]
      (let [buf (byte-array 65536)]
        (loop [] (let [n (.read in buf)]
                   (when (pos? n) (.update md buf 0 n) (recur))))))
    (format "%064x" (java.math.BigInteger. 1 (.digest md)))))

(defn read-forms [path]
  (with-open [r (java.io.PushbackReader. (io/reader path))]
    (loop [xs []]
      (let [x (edn/read {:eof ::eof} r)]
        (if (= ::eof x) xs (recur (conj xs x)))))))

(defn layers [tick]
  (if-let [v (get-in tick [:preference-stack :value])]
    v
    (mapcat #(or (:value %) []) (get-in tick [:preference-stack :by-rank]))))

(defn declaration []
  (load-file declaration-path)
  {:seeded-c @(resolve 'futon2.aif.ruled-outcome-c/seeded-c)
   :folds @(resolve 'futon2.aif.ruled-outcome-c/fold-declaration)
   :authority @(resolve 'futon2.aif.full-loop-cohort/outcome-kinds)})

(defn projection [tick]
  (let [ls (vec (layers tick))]
    (sorted-map
     :run/id (:run/id tick)
     :timestamp (:timestamp tick)
     :wm-git-sha (get-in tick [:wm-version :git-sha])
     :c-entries (:c-entries tick)
     :preference-stack-status (get-in tick [:preference-stack :status])
     :rank-count (count (get-in tick [:preference-stack :by-rank]))
     ;; :partial is a status, not a number.  Review of this slice: the
     ;; by-rank split is 146 present / 1 absent on all three ticks, so the
     ;; partiality is one ranked action, not most of them.
     :ranks-present (count (filter #(= :present (:status %))
                                   (get-in tick [:preference-stack :by-rank])))
     :ranks-absent (count (filter #(= :absent (:status %))
                                  (get-in tick [:preference-stack :by-rank])))
     :layer-ids (set (map :layer/id ls))
     :floor (set (map #(select-keys % [:layer/id :basis :folded? :site])
                      (filter #(= :floor (:layer/id %)) ls))))))

(defn facts []
  (let [{:keys [seeded-c folds authority]} (declaration)
        all (read-forms trace-path)
        ticks (filterv #(contains? expected-run-ids (:run/id %)) all)
        ps (mapv projection ticks)
        fold-map (into {} (map (juxt :layer/id identity) folds))
        zeroes (set/difference authority (set (keys (filter (comp pos? val) (:mass seeded-c)))))]
    (sorted-map
     :declaration {:folds (mapv #(select-keys % [:layer/id :folded? :in-ruled-sum :source]) folds)
                   :support (:support seeded-c)
                   :mass (:mass seeded-c)}
     :run {:expected-run-ids expected-run-ids
           :observed-run-ids (set (map :run/id ticks))
           :tick-count (count ticks)
           :utc-start (first (sort (map :timestamp ticks)))
           :utc-end (last (sort (map :timestamp ticks)))
           :git-shas (set (map #(get-in % [:wm-version :git-sha]) ticks))
           :trace-path "data/wm-trace/wm-trace-2026-09-07.edn"
           :trace-sha256 (sha256 trace-path)
           :lock-holder {:pid 2664957 :agent "codex-17" :host "zone"
                         :sha expected-sha :token "37857be0-bf64-4282-bae7-cdb4bf77b6e9"
                         :taken-at "2026-09-07T23:08:19.882739361Z"
                         :released-observed-at "2026-09-07T23:15:48Z"
                         :lock-absent-after-run? (not (fs/exists? (str repo "/data/wm-trace/.run-lock")))}
           :substrate-7071-probe {:at "before-run" :curl-exit 7 :http-code "000"
                                  :finding :connection-refused}}
     :ticks ps
     :computed-seed {:mass-sum (reduce + (vals (:mass seeded-c)))
                     :support (:support seeded-c)
                     :authority authority
                     :named-zero-dispositions zeroes
                     :named-zero-count (count zeroes)
                     :zero-masses? (every? #(zero? (get (:mass seeded-c) %)) zeroes)}
     :fold-map fold-map)))

(defn checks [f]
  (let [ticks (:ticks f) folds (:fold-map f)
        ids #(get % :layer-ids)
        ;; The `seq` is the whole point.  Without it this conjunct -- the only
        ;; POSITIVE claim the check makes about the live run -- is satisfied by
        ;; an empty floor set: renaming :floor throughout a trace copy left
        ;; `:floor #{}` in the artifact and the checker still printed PASS.
        ;; The declaration-side control below never caught that, because it
        ;; moves this check through the :folded? flag and never touches the
        ;; trace.  Reviewed and repaired 2026-09-07.
        floor-ok? (fn [p] (and (seq (:floor p))
                               (every? #(and (:folded? %)
                                             (str/includes? (:basis %) "src/futon2/aif/preferences.clj"))
                                       (:floor p))))]
    (sorted-map
     :ruled-outcome-c-absent-live
     (and (false? (get-in folds [:ruled-outcome-c :folded?]))
          (every? #(not (contains? (ids %) :ruled-outcome-c)) ticks))
     :c-int-floor-attested
     (and (true? (get-in folds [:c-int :folded?])) (seq ticks) (every? floor-ok? ticks))
     :c-mis-runtime-dark
     (and (= :runtime-dark (get-in folds [:c-mis :source]))
          (every? #(empty? (:c-entries %)) ticks))
     :c-ser-absent-live
     (and (false? (get-in folds [:c-ser :folded?]))
          (every? #(not (contains? (ids %) :c-ser)) ticks))
     :seeded-c-live-valid
     (let [s (:computed-seed f)]
       (and (= 1 (:mass-sum s)) (= (:support s) (:authority s))
            (= 7 (:named-zero-count s)) (:zero-masses? s)))
     :run-identity
     (let [r (:run f)]
       (and (= expected-run-ids (:observed-run-ids r)) (= 3 (:tick-count r))
            (= #{expected-sha} (:git-shas r)) (= expected-trace-sha (:trace-sha256 r))
            (get-in r [:lock-holder :lock-absent-after-run?]))))))

(defn failed [f] (vec (sort (keys (remove val (checks f))))))
(defn verdict [f] (every? true? (vals (checks f))))
(defn update-fold [f id k v]
  (assoc-in f [:fold-map id k] v))
(defn plant [name base mutated]
  {:plant name :landed? (not= base mutated) :verdict (verdict mutated)
   :failed-checks (failed mutated)})

(let [f (facts)
      plants (if (verdict f)
               [(plant :declare-ruled-folded f (update-fold f :ruled-outcome-c :folded? true))
              (plant :declare-c-int-unfolded f (update-fold f :c-int :folded? false))
              (plant :mutate-seeded-mass f (-> f (assoc-in [:declaration :mass :grounded-change] 2/5)
                                                     (assoc-in [:computed-seed :mass-sum] 9/10)))
              (plant :inject-live-ruled-layer f
                     (update-in f [:ticks 0 :layer-ids] conj :ruled-outcome-c))
                (plant :wrong-run-identity f
                     (-> f (assoc-in [:run :observed-run-ids] #{})
                         (assoc-in [:run :tick-count] 0)))]
               [])
      report (sorted-map :check :F10-live-run
                         :facts f :per-check (checks f) :plants plants
                         :verdict (and (verdict f)
                                       (every? #(and (:landed? %) (false? (:verdict %))) plants)))]
  (if (:verdict report)
    (do (fs/create-dirs (fs/parent artifact-path))
        (spit artifact-path (with-out-str (pp/pprint report)))
        (println "F10 LIVE RUN PASS ticks=3 in-memory-plants=5"))
    (do (binding [*out* *err*]
          (pp/pprint report)
          (println "FAILED-CHECKS:" (str/join " " (failed f))))
        (System/exit 1))))
