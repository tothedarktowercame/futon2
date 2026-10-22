(require '[futon2.aif.declared-effect-record :as d] '[clojure.edn :as edn])
(let [fake {:schema d/schema :target "forged" :wants [["forged" :effect]]
            :active-wants [["forged" :effect]] :excluded-already-true []
            :want-observations {:active [] :excluded-already-true [] :unknown []}
            :admission {:eligible true :refusals []}}
      fake (assoc fake :record/digest (d/record-digest fake))
      path "resources/wm/cascade-sources/T-repair-occ-444fb018.edn"
      target (:target (edn/read-string (slurp path)))
      r (d/build-record {:target target :declaration {:path path}
                        :base-revision {:repo "wrong-repo" :sha "not-a-revision"}
                        :observe (fn [loc] {:observed false :check :C4
                                           :evidence (assoc (select-keys loc [:repo :path :decl :sha])
                                                            :resolved-sha "not-a-commit")})})]
 (prn {:missing-authority-record fake :verification (d/verify-record fake)
       :unresolved-base-admission (:admission r)
       :unresolved-base (:base-revision r)
       :unresolved-base-verification (d/verify-record r)}))
(shutdown-agents)
