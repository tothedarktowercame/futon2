(ns fix-6-replay
  "Read-only replay of the three frozen runs; no scan, HTTP, or store writes."
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pp]
            [clojure.set :as set]
            [futon2.aif.cascade-model-manifest :as manifest]
            [futon2.aif.efe :as efe]))

(defn close! [a b]
  (assert (< (Math/abs (- (double a) (double b))) 1.0e-6) [a b]))

(defn point-state [q]
  (assert (and (= 1 (count q)) (= 1 (val (first q)))) q)
  (key (first q)))

(defn spread [xs] (- (apply max xs) (apply min xs)))
(defn softplus [x] (Math/log1p (Math/exp (double x))))

(defn replay [root suffix]
  (let [path (str root "/tick-run-record-2026-09-21-" suffix ".edn")
        raw (slurp path)
        record (edn/read-string raw)
        decision (:decision record)
        certificate (:selection-certificate decision)
        candidates (:candidates certificate)
        policies (get-in decision [:g-term-decomposition :policies])
        terms (:terms (first policies))
        c (get-in terms [:C :value])
        members (:steps c)
        horizon (count members)
        terminal (:distribution (last members))
        weights (:weights terminal)
        universe (:universe terminal)
        q0 (get-in terms [:D :value])
        s0 (point-state q0)
        rates (get-in terms [:A :value])
        ;; Reconstruct the SAME extensional C, not today's live source data.
        ;; All consumed positive log weights are explicit; no fallback invented.
        spec {:want (set (keys weights)) :weights weights :lam 1 :mu 0
              :evidence #{} :zeroed (:zeroed terminal) :c-schedule (:schedule c)}
        actions (mapv :id candidates)
        ranked (efe/rank-cascade-actions {:cascade-belief q0} actions
                                        {:horizon-steps horizon :cascade-spec spec
                                         :adjudication-rates rates})
        by-action (into {} (map (juxt :action :G-efe)) ranked)
        models (mapv (fn [action]
                       {:rates rates :q0 q0 :precedence-fn (constantly (:precedence action))
                        :horizon horizon :spec spec :universe universe}) actions)
        certs (mapv manifest/horizon-g-sparse-cert models)
        states (mapv (fn [p] (mapv (comp point-state :belief)
                                   (get-in p [:terms :Q :value :steps]))) policies)
        ;; Require the recorded consumers to agree before using one common C/D/A.
        _ (doseq [p policies]
            (assert (= c (get-in p [:terms :C :value])))
            (assert (= q0 (get-in p [:terms :D :value])))
            (assert (= rates (get-in p [:terms :A :value]))))
        _ (assert (every? #(= {:false-neg 0 :false-pos 0} %) (vals rates)))
        _ (assert (empty? (:zeroed terminal)))
        _ (assert (= actions (mapv :id policies)))
        _ (doseq [{:keys [tau distribution]} members]
            (assert (= distribution (manifest/preference-member spec universe horizon tau))))
        base-by-step (mapv (fn [{:keys [distribution]}]
                            (let [w (:weights distribution)]
                              (- (reduce + 0.0 (map #(softplus (get w % 0)) universe))
                                 (reduce + 0.0 (map #(double (get w % 0)) s0))))) members)
        baseline (reduce + base-by-step)
        newly-produced (reduce set/union #{}
                               (map #(set/difference (last %) s0) states))
        varying (set (filter (fn [t]
                              (> (count (set (map #(contains? (last %) t) states))) 1))
                            universe))
        fixed (set/difference universe varying)
        fixed-absent-wants (set/difference (set/intersection fixed (set (keys weights))) s0)
        token-rows (mapv (fn [t]
                          {:token t :weight (get weights t 0)
                           :initially-present (contains? s0 t)
                           :terminal-present (mapv #(contains? (last %) t) states)
                           :terminal-risk (mapv (fn [ss]
                                                  (- (softplus (get weights t 0))
                                                     (if (contains? (last ss) t)
                                                       (double (get weights t 0)) 0.0))) states)})
                        (sort-by pr-str universe))
        fixed-risk (reduce + 0.0
                           (for [{:keys [distribution]} members
                                 t fixed]
                             (let [w (get (:weights distribution) t 0)]
                               (- (softplus w) (if (contains? s0 t) (double w) 0.0)))))
        scale-spec (fn [k] (update spec :weights #(into {} (map (fn [[t w]] [t (* k w)]) %))))
        counterfactual (fn [k]
                         (let [gs (mapv #(manifest/horizon-g-sparse
                                          (assoc % :spec (scale-spec k))) models)
                               lo (apply min gs)
                               masses (mapv #(* (:habit %1) (Math/exp (- lo %2))) candidates gs)
                               total (reduce + masses)]
                           {:scale k :g gs :spread (spread gs)
                            :posterior-at-recorded-habit-beta-1 (mapv #(/ % total) masses)}))
        rows (mapv
              (fn [candidate result ss model]
                (let [action (:id candidate)
                      recomputed (:g result)
                      scalar (manifest/horizon-g-sparse model)
                      steps (get-in result [:certificate :steps])
                      utilities (mapv (fn [state {:keys [distribution]}]
                                        (reduce + 0 (map #(get (:weights distribution) % 0)
                                                         (set/difference state s0)))) ss members)
                      decomposed (- baseline (reduce + utilities))]
                  (close! (:g candidate) recomputed)
                  (close! scalar recomputed)
                  (close! (get by-action action) recomputed)
                  (close! decomposed recomputed)
                  (assert (= ss (mapv (comp point-state :belief)
                                     (get-in result [:certificate :consumed-g :Q :steps]))))
                  {:id (:id action) :target (:target action)
                   :patterns (mapv :id (:precedence action))
                   :recorded (:g candidate) :recomputed recomputed
                   :error (- recomputed (:g candidate))
                   :per-step (mapv #(select-keys % [:tau :risk :ambiguity]) steps)
                   :incremental-utility utilities :difference-from-baseline (- (reduce + utilities))
                   :per-step-new-tokens (mapv #(vec (sort-by pr-str (set/difference % s0))) ss)
                   :new-tokens (vec (sort-by pr-str (set/difference (last ss) s0)))
                   :habit (:habit candidate) :f (:f candidate)
                   :posterior (get-in decision [:selection-law :posterior action])}))
              candidates certs states models)
        provenance (get-in certificate [:scoring 0 :c])]
    {:run (:run/id record)
     :sha256 (format "%064x" (java.math.BigInteger. 1 (.digest (java.security.MessageDigest/getInstance "SHA-256")
                                                             (.getBytes raw "UTF-8"))))
     :candidate-count (count candidates) :horizon horizon :universe-size (count universe)
     :live-c (select-keys (:live-c provenance) [:n-entries :n-in-domain :projection])
     :unreached-live-count (count (get-in provenance [:live-c :unreached-in-domain]))
     :projected-from (get-in provenance [:live-c :projected-from])
     :weights-echo (:weights-echo provenance)
     :want-count (count weights) :varying-token-count (count varying)
     :fixed-absent-want-count (count fixed-absent-wants)
     :fixed-absent-want-risk (reduce + 0.0 (for [{:keys [distribution]} members
                                               t fixed-absent-wants]
                                           (softplus (get (:weights distribution) t 0))))
     :newly-producible-want-count (count (set/intersection newly-produced (set (keys weights))))
     :fixed-token-risk fixed-risk :base-by-step base-by-step :baseline baseline
     :uniform-universe-offset (* horizon (count universe) (Math/log 2))
     :rows rows :tokens token-rows
     :counterfactuals (mapv counterfactual [1 100 1000])
     :longer-terminal-horizon
     (mapv (fn [h]
             (let [gs (mapv #(manifest/horizon-g-sparse (assoc % :horizon h)) models)]
               {:horizon h :g gs :spread (spread gs)})) [3 10])}))

(let [root (or (first *command-line-args*) "/home/joe/code/futon2/data/wm-runs")]
  (pp/pprint {:replays (mapv #(replay root %) ["1789964661" "1789951020" "1789952479"])
              :validation "All 51 candidates: recorded, rank-cascade-actions, sparse scalar, certificate and token decomposition agree within 1e-6; predicted states match recorded states."}))
