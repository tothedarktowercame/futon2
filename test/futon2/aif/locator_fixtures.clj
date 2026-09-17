(ns futon2.aif.locator-fixtures
  "Test support: give every token in a cascade-problem source map a C3
  locator, so fixtures that are about other behaviour pass the P5 locator
  requirement in futon2.aif.cascade-problems. The locators point at a
  fixture path; they are never observed in these tests.")

(defn- tokens-of [sources target]
  (let [universe (get-in sources [:universes target])
        want (get-in sources [:wants target])
        patterns (get-in sources [:interpretations target :patterns])]
    (set (concat (keys universe) want
                 (mapcat (fn [p] (concat (get-in p [:guard :needs]) (get-in p [:guard :forbids])
                                         (mapcat :present (get-in p [:guard :clauses]))
                                         (mapcat :absent (get-in p [:guard :clauses]))
                                         (:produces p)))
                         (vals patterns))))))

(defn locate-all
  "sources with :locators added for every target that has a universe."
  [sources]
  (assoc sources :locators
         (into {} (for [target (keys (:universes sources))]
                    [target (into {} (for [t (tokens-of sources target)]
                                       [t {:class :C3 :repo "futon2" :sha "fixture"
                                           :path (str "fixture/" (pr-str t))}]))]))))
