;; E-policy-rollout-engine worked example.
;;
;; Run:
;;   cd /home/joe/code/futon2
;;   clojure -M holes/labs/e-rollout-witness.clj

(require '[futon2.aif.rollout :as rollout]
         '[meme.cap-ascent :as cap-ascent]
         '[meme.step :as meme-step])

(defn assert! [pred message data]
  (when-not pred
    (throw (ex-info message data))))

(def real-move-set (rollout/load-move-set))
(def real-moves (rollout/moves real-move-set))

;; The locked stub has no direct want→have chain, so the witness adds an
;; explicit two-step overlay while still loading and validating the real stub.
(def witness-moves
  [{:move/id "witness/root->bridge"
    :move/class :close-hole
    :have "scope/witness/root"
    :want "scope/witness/bridge"
    :score 1.0
    :delta-g -0.1
    :rank 100
    :move/terminal? false}
   {:move/id "witness/greedy-root->small"
    :move/class :close-hole
    :have "scope/witness/root"
    :want "scope/witness/small"
    :score 2.0
    :delta-g -0.2
    :rank 101
    :move/terminal? false}
   {:move/id "witness/bridge->agency"
    :move/class :advance-capability
    :have "scope/witness/bridge"
    :want "scope/capability/agency"
    :advances-cap "agency"
    :score 3.0
    :delta-g -1.0
    :rank 102
    :move/terminal? false}])

(def cap-snapshot
  {"agency" {:id "scope/capability/agency"
             :props {:capability/frontier? false
                     :capability/status :held}}
   "ai-passes-prelims" {:id "scope/capability/ai-passes-prelims"
                        :props {:capability/frontier? true
                                :capability/status :held}}})

(def initial-state
  {:arrows {}
   :cap-overlay cap-snapshot
   :reachable #{"scope/witness/root"}
   :write-count 0})

(def greedy (rollout/greedy-one-step initial-state witness-moves :top-k 3 :gamma 0.9))
(def best (rollout/best-rollout initial-state witness-moves :depth 2 :top-k 3 :gamma 0.9))

;; MUST-A: cap-ascent plan used by live promote! derives from the same pure
;; meme.step/step transition the rollout calls.
(def fetched-cap (get cap-snapshot "ai-passes-prelims"))
(def live-plan
  (with-redefs [cap-ascent/fetch-capability (fn [_cap-id _opts] fetched-cap)]
    (cap-ascent/plan "ai-passes-prelims" ["h" "w"] {:write? false})))
(def sim-state
  {:arrows {["h" "w"] {:have "h" :want "w"
                       :status :open
                       :advances-cap "ai-passes-prelims"}}
   :cap-overlay {"ai-passes-prelims" fetched-cap}
   :reachable #{"h"}})
(def sim-step (meme-step/step sim-state {:have "h" :want "w"
                                         :advances-cap "ai-passes-prelims"}))

(def terminal-move (first (filter :move/terminal? real-moves)))
(def terminal-state {:arrows {} :cap-overlay {} :reachable #{(:have terminal-move)}})
(def terminal-score (rollout/best-rollout terminal-state real-moves :depth 2 :top-k 5))

(println "=== E-policy-rollout witness ===")
(println "real-stub-count:" (count real-moves))
(println "real-stub-terminal:" (:move/id terminal-move))
(println "terminal-policy:" (mapv :move/id (:policy terminal-score))
         "truncated?:" (:truncated? terminal-score))
(println "greedy-policy:" (mapv :move/id (:policy greedy)) "G:" (:G greedy))
(println "rollout-policy:" (mapv :move/id (:policy best)) "G:" (:G best))
(println "MUST-A live-plan-target:" (:target-status live-plan)
         "sim-target:" (get-in sim-step [:cap-overlay "ai-passes-prelims" :props :capability/status]))
(println "MUST-B rollout-write-count:" (get-in best [:final-state :write-count] 0))

(assert! (= 19 (count real-moves)) "real stub must contain 19 moves" {})
(assert! (:move/terminal? terminal-move) "real stub terminal move missing" {})
(assert! (= [(:move/id terminal-move)] (mapv :move/id (:policy terminal-score)))
         "terminal move should carry cost but not expand"
         terminal-score)
(assert! (= ["witness/greedy-root->small"] (mapv :move/id (:policy greedy)))
         "greedy one-step witness changed"
         greedy)
(assert! (= ["witness/root->bridge" "witness/bridge->agency"] (mapv :move/id (:policy best)))
         "rollout did not choose the unlocking two-step policy"
         best)
(assert! (< (:G best) (:G greedy))
         "two-step rollout should beat greedy one-step"
         {:best (:G best) :greedy (:G greedy)})
(assert! (= (:target-status live-plan)
            (get-in sim-step [:cap-overlay "ai-passes-prelims" :props :capability/status]))
         "MUST-A failed: live plan and pure sim diverged"
         {:live-plan live-plan :sim-step sim-step})
(assert! (zero? (get-in best [:final-state :write-count] 0))
         "MUST-B failed: rollout performed writes"
         best)

(println (format "PASS greedy-G=%.4f rollout-G=%.4f policy=%s writes=%d"
                 (:G greedy)
                 (:G best)
                 (pr-str (mapv :move/id (:policy best)))
                 (get-in best [:final-state :write-count] 0)))

(shutdown-agents)
