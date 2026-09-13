(require '[futon2.aif.machine-slow-prior-evidence :as e5]
         '[futon2.aif.machine-slow-prior-evidence-test :as fixture])
(let [without-identity (into {} (for [[k v] (fixture/records)]
                                 [k (apply dissoc v [:model/id :model/revision :run/id :tick/index])]))
      unknown-mode (fixture/records :not-a-strategic-mode 3)]
  (doseq [[label records] [[:missing-identities without-identity] [:unknown-slow-mode unknown-mode]]]
    (let [out (e5/verify-shaping (fixture/config records))]
      (prn {:control label :identity (:identity out) :mode (:slow/mode out)
            :weights (:weight-table out) :law (:law out) :scope (:scope out)})
      (assert (= :wm/r15-r6-slow-prior-evidence-v1 (:schema/version out))))))
