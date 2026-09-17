(ns futon2.aif.live-c-test
  "Live C, first cut (commissioned 2026-09-17): derivation from Joe's three
  sources, typed refusals, the freshness guard, and the weighted-want spec
  the cascade scorer consumes."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-model-manifest :as m]
            [futon2.aif.live-c :as lc]))

;; ---------------------------------------------------------------- parsing

(deftest status-line-and-closed
  (is (lc/closed? "**Status:** CLOSED 2026-06-10 — operator close."))
  (is (lc/closed? "prose\n**Status:** COMPLETE via blah\nmore"))
  (is (not (lc/closed? "**Status:** IDENTIFY (mission proposal)")))
  (is (not (lc/closed? "**Status:** SPEC. Prerequisite for M-aif-stack S2.")))
  (is (nil? (lc/status-line "no status here"))))

;; ---------------------------------------------------------------- pure pieces

(deftest alive-entries-dark-room
  ;; L = T·H is the weight; T = 0 (nothing going on) means NO want token,
  ;; however harmonious — the dark room is not preferred by doing nothing.
  (let [w {:missions [{:mission "M-a" :class :alive :L 82 :T 10 :H 8.2}
                      {:mission "M-b" :class :alive :L 0 :T 0 :H 7.0}
                      {:mission "M-c" :class :mess :L 47 :T 10 :H 4.7}]}
        es (lc/alive-entries w)]
    (is (= [:alive/M-a] (mapv :token es)))
    (is (= 82 (:weight (first es))))))

(deftest normalise-weights-laws
  (is (= :empty-want (get-in (lc/normalise-weights 1 []) [:refusal :kind])))
  (let [es [{:token :a :weight 3} {:token :b :weight 1}]
        {:keys [weights]} (lc/normalise-weights 1 es)]
    (is (= 1 (reduce + (vals weights))))
    (is (rational? (:a weights)))
    (is (< (:b weights) (:a weights))))
  (is (= :invalid-weight
         (get-in (lc/normalise-weights 1 [{:token :a :weight 0}]) [:refusal :kind]))))

(deftest completion-and-stars
  (let [ms [{:mission "M-open" :text "**Status:** SPEC."}
            {:mission "M-shut" :text "**Status:** CLOSED 2026-01-01."}]
        cs (lc/completion-entries ms)]
    (is (= [:closed/M-open] (mapv :token cs))))
  (let [{:keys [entries refusals]}
        (lc/unreached-stars {:capabilities {:sat {:status :satisfied}
                                            :held {:status :held}
                                            :mystery {}}})]
    (is (= [:star/held] (mapv :token entries)))
    (is (= [{:kind :capability-status-missing :capability :mystery}] refusals)))

;; ---------------------------------------------------------------- refusals

(deftest missing-source-refuses-whole-derivation
  (let [d (lc/derive-live-c {:wholeness {:refusal {:kind :source-missing :path "/x"}}
                             :missions [] :stars {:value {:capabilities {}}}})]
    (is (seq (:refusals d)))
    (is (nil? (:want d)))
    (is (= :source-missing (-> d :refusals first :kind))))))

;; ---------------------------------------------------------------- the live derivation

(def real-sources (fn [] (lc/read-sources)))

(deftest derive-live-c-over-real-sources
  (let [d (lc/derive-live-c (real-sources))]
    (is (map? d) (str "refused: " (pr-str d)))
    (is (seq (:want d)))
    (is (= 1 (reduce + (vals (:weights d)))))
    (is (every? #(and (rational? %) (pos? %)) (vals (:weights d))))
    ;; every want token has a weight and vice versa
    (is (= (:want d) (set (keys (:weights d)))))
    ;; the declared target M-wm-aif-policy-grain-compliance HAS a mission
    ;; file but NO wholeness row: refusal, not a default weight; its
    ;; closure is still wanted. M-wm-08-external-f2 has NEITHER a mission
    ;; file NOR a wholeness row — no live source speaks to it at all.
    (is (contains? (:want d) :closed/M-wm-aif-policy-grain-compliance))
    (is (some #(and (= :mission-not-in-wholeness (:kind %))
                    (= "M-wm-aif-policy-grain-compliance" (:mission %)))
              (:gaps d)))
    (is (not (or (contains? (:want d) :closed/M-wm-08-external-f2)
                 (contains? (:want d) :alive/M-wm-08-external-f2))))
    (is (some string? [(:signature d)]))
    ;; the spec the scorer consumes, restricted to a reachable domain
    (let [reachable (-> (:want d) vec (subvec 0 (max 1 (dec (count (:want d))))) set (conj ::outside-domain))
          s (lc/cascade-spec d reachable)]
      (is (set (:want s)))
      (is (:weights s))
      (is (not (contains? (:want s) ::outside-domain)))
      (is (seq (:unreached-in-domain (:live-c s)))))
    ;; no live-C token in the domain: typed refusal, never an empty belly
    (is (= :no-reachable-want (get-in (lc/cascade-spec d #{::other}) [:refusal :kind])))))

(deftest freshness-guard-flips-on-corpus-change
  (let [sources {:wholeness {:path "/w" :sha256 "a" :value {:missions [{:mission "M-a" :class :alive :L 5 :T 1 :H 5}]}}
                 :missions [{:path "/m1" :sha256 "m1" :mission "M-a" :text "**Status:** OPEN."}]
                 :stars {:path "/s" :sha256 "s" :value {:capabilities {:c {:status :held}}}}}
        d (lc/derive-live-c sources)
        same (lc/stale? d sources)]
    (is (not (:stale? same)))
    ;; a mission closes: C is stale, loudly
    (let [closed (assoc-in sources [:missions 0 :text] "**Status:** CLOSED today.")]
      (is (not= (:signature (lc/derive-live-c closed)) (:signature d)))
      (is (:stale? (lc/stale? d closed))))
    ;; a wholeness L changes: stale
    (is (:stale? (lc/stale? d (assoc-in sources [:wholeness :value :missions 0 :L] 6))))))

;; ------------------------------------------------- the scorer's :weights

(deftest log-preference-fn-weights
  (let [base {:want #{:a :b} :lam 2 :mu 0 :zeroed #{}}
        lpf (m/log-preference-fn base)
        wlpf (m/log-preference-fn (assoc base :weights {:a 3/2 :b 1/2}))]
    ;; heavier weight on :a raises c({a}) and lowers c({b})
    (is (> (wlpf #{:a}) (lpf #{:a})))
    (is (< (wlpf #{:b}) (lpf #{:b})))
    ;; still a log-distribution: every log value finite and negative
    (is (every? #(< % 0) (map wlpf [#{:a} #{:b} #{:a :b} #{}])))
    ;; validation refusals
    (is (= :weight-token-not-in-want
           (:reason (m/log-preference-fn (assoc base :weights {:zz 1})))))
    (is (= :weight-not-positive-rational
           (:reason (m/log-preference-fn (assoc base :weights {:a 0})))))
    (is (= :weights-not-a-map
           (:reason (m/log-preference-fn (assoc base :weights 7)))))))

(deftest extra-unreachable-tokens-are-common-mode-in-universe-not-want
  ;; The before/after mechanism, corrected by the data: an unreachable token
  ;; in :universe (zero weight) shifts every candidate's G by exactly
  ;; T·k·ln 2 — common mode. The SAME token added to :want is NOT common
  ;; mode: it dilutes the uniform per-token share lam/|want| and changes
  ;; the relative utility of reachable outcomes. This is why live-c's
  ;; cascade-spec restricts :want to the reachable domain.
  (let [spec {:want #{:t0} :lam 1 :mu 0 :zeroed #{}}
        rates (zipmap [:t0 :x0 :x1] (repeat {:false-neg 0 :false-pos 0}))
        fire {:id :fire :guard {:status :interpreted :operator :and
                                :clauses [{:status :interpreted :present #{} :absent #{}}]}
              :transition {:status :interpreted :operator :union :produces #{:t0}}
              :produces #{:t0} :theta 1}
        base {:rates rates :q0 {#{} 1} :precedence-fn (constantly [fire]) :horizon 2 :spec spec}
        g0 (m/horizon-g-sparse base)
        g2 (m/horizon-g-sparse (assoc base :universe #{:t0 :x0 :x1}))
        ;; the same two tokens added to :want instead: NOT the common-mode
        ;; shift (dilution), and provably different from it
        gw (m/horizon-g-sparse (assoc-in base [:spec :want] #{:t0 :x0 :x1}))]
    (is (< (Math/abs (- (- g2 g0) (* 2 2 (Math/log 2)))) 1e-9))
    (is (not (< (Math/abs (- (- gw g0) (* 2 2 (Math/log 2)))) 1e-9)))))
