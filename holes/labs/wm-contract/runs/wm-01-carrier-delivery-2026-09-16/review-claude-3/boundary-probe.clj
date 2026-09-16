(require '[futon2.aif.machine-model :as m] '[clojure.edn :as edn])
(defn s [r] (select-keys r [:ok :admission :representation :exactly-normalized? :exact-deviation :criterion :refusal]))
(defn line [k v] (prn k v))
(let [a :a b :b sup [:a :b]]
  (line :decimal-exact-one (s (m/distribution-admission {a 0.5M b 0.5M} sup)))
  (line :decimal-dev-1e-13 (s (m/distribution-admission {a 0.5M b 0.5000000000001M} sup)))
  (line :decimal-dev-exactly-1e-12 (s (m/distribution-admission {a 0.5M b 0.500000000001M} sup)))
  (line :decimal-dev-just-over (s (m/distribution-admission {a 0.5M b 0.5000000000010000000000000000001M} sup)))
  (line :ratio-dev-1e-13 (s (m/distribution-admission {a 1/2 b (+ 1/2 1/10000000000000)} sup)))
  (line :ratio-plus-decimal-dev-1e-13 (s (m/distribution-admission {a 1/2 b 0.5000000000001M} sup)))
  (line :extra-zero-key (s (m/distribution-admission {a 1/2 b 1/2 :c 0} sup)))
  (line :missing-key (s (m/distribution-admission {a 1} sup)))
  (line :dup-support (s (m/distribution-admission {a 1/2 b 1/2} [:a :a :b])))
  (line :list-support (s (m/distribution-admission {a 1/2 b 1/2} '(:a :b))))
  (line :nan (s (m/distribution-admission {a Double/NaN b 0.5} sup)))
  (line :neg (s (m/distribution-admission {a -1/2 b 3/2} sup)))
  (let [r (m/distribution-admission {a 0.25 b 0.75} [:b :a])]
    (line :permuted-support-preserves-assignment [(:ok r) (:values r) (:support r)]))
  (let [f (float 0.1) r (m/numeric-row-admission {a f b (- 1.0 (double f))})]
    (line :float32 [(:representations r) (= (double f) (get-in r [:values :a])) (class (get-in r [:values :a])) (:ok r) (:exact-deviation r)]))
  nil)
(let [cases (edn/read-string (slurp (str "holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-numeric-1/" "cases.edn")))
      prod (:float-seven cases)
      vals7 (map prod [:a :b :c :d :e :f :g])
      lean [7589757911525539/72057594037927936 5075107643853621/36028797018963968 5627707221685375/18014398509481984
            3442556320081687/36028797018963968 2683201850079625/36028797018963968 5982758850052747/36028797018963968
            7589757911525539/72057594037927936]
      exact (map #(rationalize (BigDecimal. (double %))) vals7)
      r (m/numeric-row-admission prod)]
  (prn :retained-classes (map class vals7))
  (prn :retained-coords-equal-lean (= (vec exact) lean))
  (prn :retained-admission (select-keys r [:ok :admission :exactly-normalized? :exact-total :exact-deviation])))
