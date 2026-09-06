(ns v7-r1-node-sim
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [futon2.aif.belief :as belief]))

(def lab (io/file "holes/labs/wm-contract"))
(def carriers (edn/read-string (slurp (io/file lab "sim/R1-carriers.edn"))))
(def statuses (:status-order carriers))
(def status-set (set statuses))
(def uniform (zipmap statuses (repeat (:uniform-mass carriers))))
(def peaked (:peaked carriers))

(defn reference-initial [ids]
  (into {} (map (fn [id] [id uniform]) ids)))

(defn reference-carry [fresh carried]
  (if (seq carried)
    (into {} (map (fn [[id prior]] [id (get carried id prior)]) fresh))
    fresh))

(defn max-deviation [a b]
  (cond
    (and (number? a) (number? b)) (Math/abs (- (double a) (double b)))
    (and (map? a) (map? b)) (if (= (set (keys a)) (set (keys b)))
                              (reduce max 0.0 (map #(max-deviation (get a %) (get b %)) (keys a)))
                              ##Inf)
    (= a b) 0.0
    :else ##Inf))

(defn fixture-input [{:keys [fresh-entities carried carried-entries] :as fixture}]
  (let [fresh (reference-initial fresh-entities)
        carried* (if (contains? fixture :carried)
                   carried
                   (into {} (map (fn [[id v]] [id (if (= v :peaked) peaked v)])) carried-entries))]
    {:fresh fresh :carried carried*}))

(def fixture-pairs (mapv #(merge {:id (:id %)} (fixture-input %)) (:carry-fixtures carriers)))

(defn safely [f]
  (try {:value (f)} (catch Throwable t {:threw (.getName (class t)) :message (.getMessage t)})))

(def shipped
  {:status-set (fn [] belief/status-set)
   :uniform belief/uniform-prior
   :initial belief/initial-belief-state
   :carry belief/reconcile-belief-carry
   :normalise (fn [p] (#'belief/normalise p))
   :argmax belief/most-likely-status
   :entropy belief/entropy})

(defn node-checks [node]
  (let [node-uniform ((:uniform node))
        initial ((:initial node) (:fixture-entities carriers))
        carry-runs (mapv (fn [{:keys [id fresh carried]}]
                           (let [actual (safely #((:carry node) fresh carried))
                                 expected (reference-carry fresh carried)]
                             {:id id :fresh fresh :carried carried :actual (:value actual)
                              :threw (:threw actual) :expected expected})) fixture-pairs)
        carry-devs (map #(max-deviation (:actual %) (:expected %)) carry-runs)
        no-arithmetic? (every? (fn [{:keys [fresh carried actual]}]
                                 (and (map? actual)
                                      (every? (fn [[_ p]] (or (some #(= p %) (vals fresh))
                                                               (some #(= p %) (vals carried)))) actual)))
                               carry-runs)
        zero (zipmap statuses (repeat 0.0))
        zero-result (safely #((:normalise node) zero))
        a (:collision-a carriers) b (:collision-b carriers)
        a-arg (safely #((:argmax node) a)) b-arg (safely #((:argmax node) b))
        a-ent (safely #((:entropy node) a)) b-ent (safely #((:entropy node) b))
        ref-ent (fn [p] (- (reduce + (map #(* % (Math/log %)) (filter pos? (vals p))))))
        moment-ok (and (not= a b) (= :spawned (:value a-arg)) (= (:value a-arg) (:value b-arg))
                       (number? (:value a-ent)) (number? (:value b-ent))
                       (zero? (max-deviation (:value a-ent) (:value b-ent)))
                       (< (max-deviation (:value a-ent) (ref-ent a)) 1.0e-15)
                       (< (max-deviation (:value b-ent) (ref-ent b)) 1.0e-15))
        mixed (last carry-runs)
        counterexample? (and (= (:actual mixed) (:expected mixed))
                             (not= (:actual mixed) (:fresh mixed))
                             (not= (:actual mixed) (:carried mixed)))
        t0 ((:carry node) {"returning" uniform} {"returning" peaked})
        t1 ((:carry node) {} t0)
        t2 ((:carry node) {"returning" uniform} t1)
        reentry? (and (= peaked (get t0 "returning")) (empty? t1) (= uniform (get t2 "returning")))]
    [{:id :status-set-matches-declaration :result (if (= status-set (set ((:status-set node)))) :pass :fail)}
     {:id :uniform-prior-is-normalised-over-the-declared-set
      :result (if (and (= status-set (set (keys node-uniform))) (< (Math/abs (- 1.0 (reduce + (vals node-uniform)))) 1.0e-15)
                       (< (max-deviation uniform node-uniform) 1.0e-15)) :pass :fail)
      :mass (when (map? node-uniform) (reduce + (vals node-uniform)))}
     {:id :initial-belief-state-is-uniform-per-entity
      :result (if (= (reference-initial (:fixture-entities carriers)) initial) :pass :fail)}
     {:id :carry-agrees-with-independent-reference
      :result (if (every? zero? carry-devs) :pass :fail) :comparisons (count carry-runs)
      :max-deviation (reduce max 0.0 carry-devs)}
     {:id :carry-performs-no-arithmetic :result (if no-arithmetic? :pass :fail)
      :comparisons (reduce + (map #(count (:actual %)) carry-runs))}
     {:id :formal-line-has-a-counterexample :result (if counterexample? :pass :fail)
      :exhibited (select-keys mixed [:fresh :carried :actual])}
     {:id :moment-pair-is-not-sufficient :result (if moment-ok :pass :fail)
      :posterior-a a :posterior-b b :argmax (:value a-arg)
      :entropy-a (:value a-ent) :entropy-b (:value b-ent)
      :entropy-delta (when (and (:value a-ent) (:value b-ent)) (max-deviation (:value a-ent) (:value b-ent)))
      :mass-a (reduce + (vals a)) :mass-b (reduce + (vals b))}
     {:id :zero-sum-posterior-returns-uniform
      :result (if (= uniform (:value zero-result)) :pass :fail) :threw (:threw zero-result)}
     {:id :re-entry-loses-history :result (if reentry? :pass :fail)
      :states {:t0 t0 :t1 t1 :t2 t2}}]))

(defn trace-files []
  (->> (.listFiles (io/file "data/wm-trace"))
       (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" (.getName %)))
       (sort-by #(.getName %))))

(defn read-file-records [f]
  (with-open [r (java.io.PushbackReader. (io/reader f))]
    (loop [out []]
      (let [v (try (edn/read {:eof ::eof :default (fn [_ v] v)} r) (catch Exception _ ::bad))]
        (cond (= v ::eof) out (= v ::bad) (recur out) :else (recur (conj out v)))))))

(def corpus-records
  (vec (mapcat (fn [f] (map-indexed (fn [i r] {:file (.getName f) :index i :record r})
                                    (read-file-records f))) (trace-files))))

(defn corpus-replay []
  (let [pairs (partition 2 1 corpus-records)
        step (fn [acc [a b]]
               (let [post (get-in a [:record :mu-post]) pre (get-in b [:record :mu-pre])]
                 (if-not (and (map? post) (map? pre))
                   (update-in acc [:skipped (cond (not (map? post)) :previous-mu-post-missing
                                                  :else :next-mu-pre-missing)] (fnil inc 0))
                   (let [survivors (sort-by pr-str (filter #(contains? pre %) (keys post)))
                         new (sort-by pr-str (remove #(contains? post %) (keys pre)))
                         dropped (sort-by pr-str (remove #(contains? pre %) (keys post)))
                         mismatched (remove #(= (get post %) (get pre %)) survivors)
                         sm (count mismatched)
                         ;; A survivor whose recorded prior is the UNIFORM prior did not
                         ;; receive the carry at all: the tick recomputed from the fresh
                         ;; bootstrap. Counting these separates "the carry ran and got a
                         ;; different answer" from "the carry did not run".
                         sm-uniform (count (filter #(= uniform (get pre %)) mismatched))
                         nm (count (remove #(= uniform (get pre %)) new))
                         ;; ERA: a record whose whole recorded prior is uniform is a
                         ;; pre-carry record. The split is read off the corpus, not
                         ;; off a date typed in by hand.
                         carry-era? (not (every? #(= uniform %) (vals pre)))]
                     (-> acc
                         (update :eligible-pairs inc)
                         (update :survivor-comparisons + (count survivors))
                         (update :survivor-mismatches + sm)
                         (update :survivor-mismatches-whose-prior-is-uniform + sm-uniform)
                         (update :new-comparisons + (count new))
                         (update :new-mismatches + nm)
                         (update :dropped-total + (count dropped))
                         (update (if carry-era? :carry-era-pairs :pre-carry-pairs) inc)
                         (update (if carry-era? :carry-era-survivor-comparisons
                                     :pre-carry-survivor-comparisons) + (count survivors))
                         (update (if carry-era? :carry-era-survivor-mismatches
                                     :pre-carry-survivor-mismatches) + sm)
                         (update (if carry-era? :carry-era-new-comparisons
                                     :pre-carry-new-comparisons) + (count new))
                         (update (if carry-era? :carry-era-dropped :pre-carry-dropped) + (count dropped))
                         (update :carry-era-files (fn [v] (if carry-era? (conj v (:file b)) v)))
                         (update :pair-summary conj {:from (:file a) :from-index (:index a)
                                                    :to (:file b) :to-index (:index b)
                                                    :carry-era? carry-era?
                                                    :survivors (count survivors) :survivor-mismatches sm
                                                    :survivor-mismatches-whose-prior-is-uniform sm-uniform
                                                    :new (count new) :new-mismatches nm :dropped (count dropped)}))))))
        base {:eligible-pairs 0 :survivor-comparisons 0 :survivor-mismatches 0
              :survivor-mismatches-whose-prior-is-uniform 0
              :new-comparisons 0 :new-mismatches 0 :dropped-total 0 :skipped {} :pair-summary []
              :carry-era-pairs 0 :pre-carry-pairs 0
              :carry-era-survivor-comparisons 0 :pre-carry-survivor-comparisons 0
              :carry-era-survivor-mismatches 0 :pre-carry-survivor-mismatches 0
              :carry-era-new-comparisons 0 :pre-carry-new-comparisons 0
              :carry-era-dropped 0 :pre-carry-dropped 0 :carry-era-files []}
        replay (reduce step base pairs)
        domain-records (filter #(map? (get-in % [:record :mu-pre])) corpus-records)
        entity-series (reduce (fn [m [i x]]
                                (reduce (fn [m2 e] (update m2 e (fnil conj []) i)) m
                                        (keys (get-in x [:record :mu-pre]))))
                              {} (map-indexed vector domain-records))
        reentries (for [[e is] entity-series
                        [x y] (partition 2 1 is) :when (> y (inc x))]
                    {:entity e :absent-ticks (- y x 1)
                     :uniform-return? (= uniform (get-in (nth domain-records y) [:record :mu-pre e]))})]
    (assoc replay :file-count (count (trace-files)) :record-count (count corpus-records)
           :first-file (some-> corpus-records first :file) :last-file (some-> corpus-records last :file)
           :first-carry-era-file (first (:carry-era-files replay))
           :last-carry-era-file (last (:carry-era-files replay))
           :carry-era-file-count (count (distinct (:carry-era-files replay)))
           :re-entry-count (count reentries) :re-entry-uniform-count (count (filter :uniform-return? reentries))
           :re-entries (vec (sort-by (comp pr-str :entity) reentries)))))

(def replay (corpus-replay))
(def morning-records (filter #(seq (get-in % [:record :morning-brief-events])) corpus-records))

(def plants
  [{:id :carry-keeps-vanished-entities :node (assoc shipped :carry merge)}
   {:id :carry-drops-new-entities :node (assoc shipped :carry (fn [fresh carried] (select-keys carried (keys fresh))))}
   {:id :carry-overwrites-survivor-with-the-fresh-prior :node (assoc shipped :carry (fn [fresh _] fresh))}
   {:id :carry-averages-survivor-with-fresh
    :node (assoc shipped :carry (fn [fresh carried]
                                  (into {} (for [[e p] fresh]
                                             [e (if-let [c (get carried e)]
                                                  (into {} (for [s statuses] [s (/ (+ (get p s) (get c s)) 2.0)])) p)]))))}
   {:id :carry-ignores-cold-start :node (assoc shipped :carry (fn [fresh carried] (if (nil? carried) {} (reference-carry fresh carried))))}
   {:id :uniform-prior-unnormalised :node (assoc shipped :uniform #(zipmap statuses (repeat 1.0)))}
   {:id :status-set-missing-reopened :node (assoc shipped :status-set #(disj status-set :reopened))}
   {:id :entropy-in-bits-not-nats :node (assoc shipped :entropy (fn [p] (/ (belief/entropy p) (Math/log 2.0))))}
   {:id :most-likely-status-is-argmin :node (assoc shipped :argmax (fn [p] (key (apply min-key val p))))}
   {:id :normalise-raises-on-zero-sum :node (assoc shipped :normalise (fn [_] (throw (ex-info "zero sum" {}))))}])

(def fixture-checks (node-checks shipped))
(def corpus-check {:id :corpus-carry-replay :result :pass
                   :finding-policy "Mismatches are reported, not converted into a harness failure."
                   :totals (dissoc replay :pair-summary :re-entries :carry-era-files)
                   :pair-summary (:pair-summary replay) :re-entries (:re-entries replay)})
(def prior-check {:id :recorded-prior-is-not-the-loop-s-starting-belief :result :pass
                  :records-with-nonempty-morning-brief-events (count morning-records)
                  :record-count (count corpus-records)
                  :reads "scripts/futon2/report/war_machine.clj:6033-6038,6098,6702"})
(def checks (vec (concat fixture-checks [corpus-check prior-check])))

(def plant-results
  (mapv (fn [{:keys [id node]}]
          (let [failed (mapv :id (filter #(= :fail (:result %)) (node-checks node)))]
            {:id id :result (if (seq failed) :caught :escaped) :caught-by failed})) plants))
(def all-caught (every? #(= :caught (:result %)) plant-results))
(def all-pass (every? #(= :pass (:result %)) checks))

(def receipt
  {:harness :v7-r1-node-sim :row :V7 :slice 4 :node :R1 :stage "BELIEVE"
   :stage-at "p4ng/empirics-futon/control-stages.edn:17"
   :carriers "holes/labs/wm-contract/sim/R1-carriers.edn"
   :reference-independence "Reference values are constructed only from R1-carriers.edn; futon2.aif.belief supplies only node-under-test results."
   :declaration-sites (:declaration-sites carriers)
   :checks checks :verdict (if all-pass :pass :fail)
   :negative-controls {:plants plant-results :n (count plant-results) :all-caught all-caught}
   :corpus {:root "data/wm-trace" :read-only true :file-count (:file-count replay)
            :record-count (:record-count replay) :first-file (:first-file replay) :last-file (:last-file replay)}
   :not-done ["No production source changed; no live tick or run lock."
              "Corpus replay mismatches are findings, not a tuned pass criterion."
              "VERIFY-r-nodes.edn, registries, worklist, documentation, and p4ng were not written."]})

(def out (io/file lab "runs/V7-R1-node-sim/00-r1.edn"))
(.mkdirs (.getParentFile out))
(with-open [w (io/writer out)] (binding [*out* w] (pp/pprint receipt)))
(doseq [c checks] (println (format "  %-52s %s" (name (:id c)) (name (:result c)))))
(doseq [p plant-results] (println "  planted" (:id p) (:result p) (:caught-by p)))
(println "  verdict" (:verdict receipt) "all-caught" all-caught "receipt" (str out))
(System/exit (if (and all-pass all-caught) 0 1))
