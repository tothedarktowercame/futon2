;; :F8 leg 1 slice 8 -- reviewing seat's INDEPENDENT probe of the selection
;; temperature tau, run BEFORE codex-9 reported. Nothing here reads codex-9's
;; Lean or its readback; it calls production directly, so the two derivations
;; can be compared rather than one trusted.
(require '[futon2.aif.policy :as policy])
(load-file "scripts/futon2/report/war_machine.clj")
(def wm (find-ns 'futon2.report.war-machine))

(defn p [label v] (println label "=" (pr-str v)))

;; --- tau_spread: range(G)/k, floored at tau-min ----------------------------
(def g-totals [1.0 2.0 3.5 0.5])          ; range 3.0 -> 3.0/5.0 = 0.6
(p "tau-spread g-totals" (policy/adaptive-temperature g-totals))
(p "tau-spread empty" (policy/adaptive-temperature []))
(p "tau-spread degenerate [2 2 2]" (policy/adaptive-temperature [2.0 2.0 2.0]))
(p "tau-spread k=1" (policy/adaptive-temperature g-totals {:k 1.0}))

;; --- the three laws on the SAME inputs -------------------------------------
(def g 2.0)                                ; selection gain
(def beta 0.25)
(def spread   (policy/effective-temperature g-totals g {:tau-mode :spread}))
(def gain-only(policy/effective-temperature g-totals g {:tau-mode :selection-gain-only}))
(def varia    (policy/effective-temperature g-totals g {:tau-mode :variational-beta-gamma
                                                        :variational-beta beta}))
(p "law :spread              tau" spread)
(p "law :selection-gain-only tau" gain-only)
(p "law :variational-beta-gamma tau" varia)
(p "three laws pairwise distinct?" [(not= spread gain-only) (not= spread varia) (not= gain-only varia)])
(p "spread = tau-spread / g ?" (= spread (/ (policy/adaptive-temperature g-totals) g)))
(p "gain-only = 1/g ?" (= gain-only (/ 1.0 g)))
(p "variational = beta exactly ?" (= varia beta))

;; --- the function's OWN default is :spread ---------------------------------
(p "no :tau-mode key => equals :spread ?"
   (= (policy/effective-temperature g-totals g {})
      (policy/effective-temperature g-totals g {:tau-mode :spread})))
(p "no opts at all => equals :spread ?"
   (= (policy/effective-temperature g-totals g)
      (policy/effective-temperature g-totals g {:tau-mode :spread})))

;; --- the ARENA's default is :selection-gain-only ---------------------------
(def tau-mode-of (ns-resolve wm 'tau-mode-of))
(p "tau-mode-of nil"     (tau-mode-of nil))
(p "tau-mode-of \"\""      (tau-mode-of ""))
(p "tau-mode-of \"spread\"" (tau-mode-of "spread"))
(p "tau-mode-of \"variational-beta-gamma\"" (tau-mode-of "variational-beta-gamma"))
(p "tau-mode-of \"nonsense\"" (tau-mode-of "nonsense"))
(p "FUTON_WM_TAU_MODE in env" (System/getenv "FUTON_WM_TAU_MODE"))
(p "arena default = policy default ?" (= (tau-mode-of (System/getenv "FUTON_WM_TAU_MODE")) :spread))

;; --- g = 1.0 reductions -----------------------------------------------------
(p "g=1 :spread = tau-spread exactly ?"
   (= (policy/effective-temperature g-totals 1.0 {:tau-mode :spread})
      (policy/adaptive-temperature g-totals)))
(p "g=1 :selection-gain-only = 1.0 exactly ?"
   (= 1.0 (policy/effective-temperature g-totals 1.0 {:tau-mode :selection-gain-only})))

;; --- the floor applies to g, NOT to beta ------------------------------------
(p "g=0 :selection-gain-only tau (floor 0.01 -> 100.0)"
   (policy/effective-temperature g-totals 0.0 {:tau-mode :selection-gain-only}))
(p "g=1e-9 :selection-gain-only tau"
   (policy/effective-temperature g-totals 1e-9 {:tau-mode :selection-gain-only}))
(p "beta=1e-9 :variational tau (NOT floored)"
   (policy/effective-temperature g-totals 1.0 {:tau-mode :variational-beta-gamma
                                               :variational-beta 1e-9}))

;; --- the variational law is partial: it throws, it does not fall back -------
(defn threw [f]
  (try (let [v (f)] [:returned v])
       (catch Exception e [:threw (.getMessage e)])))
(doseq [[label b] [["missing" nil] ["zero" 0.0] ["negative" -1.0]
                   ["NaN" Double/NaN] ["Inf" Double/POSITIVE_INFINITY]
                   ["string" "0.25"]]]
  (p (str "variational beta=" label)
     (threw #(policy/effective-temperature g-totals g
               (cond-> {:tau-mode :variational-beta-gamma}
                 (some? b) (assoc :variational-beta b))))))
(p "unknown :tau-mode" (threw #(policy/effective-temperature g-totals g {:tau-mode :bogus})))

;; --- temperature-source: the mode names the law, not the solve --------------
(p "source :spread"              (policy/temperature-source {:tau-mode :spread}))
(p "source default (no key)"     (policy/temperature-source {}))
(p "source :selection-gain-only" (policy/temperature-source {:tau-mode :selection-gain-only}))
(doseq [s [:converged-posterior :held-unsolved :held-absent :initial nil]]
  (p (str "source :variational + beta-source " s)
     (policy/temperature-source {:tau-mode :variational-beta-gamma
                                 :variational-beta-source s})))
(p "source unknown mode" (policy/temperature-source {:tau-mode :bogus}))

;; --- where tau enters, and where it does not --------------------------------
(def ranked
  [{:action {:type :a} :rank 1 :controller-score 1.0 :habit-prior-bias 0.0}
   {:action {:type :b} :rank 2 :controller-score 0.0 :habit-prior-bias 0.0}])
(p "selection-scores tau=1"   (policy/selection-scores (mapv :controller-score ranked) 1.0 nil))
(p "selection-scores tau=10"  (policy/selection-scores (mapv :controller-score ranked) 10.0 nil))
(p "softmax-weights tau=1"    (policy/softmax-weights (mapv :controller-score ranked) 1.0))
(p "softmax-weights tau=10"   (policy/softmax-weights (mapv :controller-score ranked) 10.0))

(defn strategic [opts]
  (policy/select-action ranked
                        {:selection-boundary :strategic-recommendation
                         :temperature-opts opts}))
(def s-lo (strategic {:tau-mode :variational-beta-gamma :variational-beta 0.1
                      :variational-beta-source :converged-posterior}))
(def s-hi (strategic {:tau-mode :variational-beta-gamma :variational-beta 9.0
                      :variational-beta-source :converged-posterior}))
(p "head law: tau lo/hi" [(:tau s-lo) (:tau s-hi)])
(p "head law: action lo/hi" [(:action s-lo) (:action s-hi)])
(p "head law: SAME action under different tau ?" (= (:action s-lo) (:action s-hi)))
(p "head law: softmax-weights differ ?" (not= (:softmax-weights s-lo) (:softmax-weights s-hi)))
(p "head law: tau-source" [(:tau-source s-lo) (:tau-source s-hi)])

;; the :actuation boundary with a NONZERO habit prior: argmax(lnE - G/tau)
(def ranked-hp
  [{:action {:type :cautious} :rank 1 :controller-score 0.0 :habit-prior-bias 0.0}
   {:action {:type :habitual} :rank 2 :controller-score 2.0 :habit-prior-bias 1.0}])
(defn actuation [tau-mode beta]
  (policy/select-action ranked-hp
                        {:abstain-epsilon 0.0
                         :temperature-opts {:tau-mode tau-mode :variational-beta beta
                                            :variational-beta-source :converged-posterior}}))
(def a-lo (actuation :variational-beta-gamma 0.5))
(def a-hi (actuation :variational-beta-gamma 8.0))
(p "habit branch: tau lo/hi" [(:tau a-lo) (:tau a-hi)])
(p "habit branch: action lo/hi" [(:action a-lo) (:action a-hi)])
(p "habit branch: tau CHANGES the action ?" (not= (:action a-lo) (:action a-hi)))
