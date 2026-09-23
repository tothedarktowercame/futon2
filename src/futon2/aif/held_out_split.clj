(ns futon2.aif.held-out-split
  "Validation for prospective EIG held-out split declarations."
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def schema :wm/eig-held-out-split-v1)
(def disposition 'HELD-OUT-SPLIT-DECLARED)
(def required-outcomes #{:result :no-result :failure :timeout})

(defn- refuse! [reason data]
  (throw (ex-info "Held-out EIG split refused"
                  (assoc data :held-out-split/refusal reason))))

(defn validate
  "Return DECLARATION unchanged when it is a prospective, disjoint split.
  The declaration is not an observation and confers no calibration authority."
  [declaration]
  (let [{:keys [schema disposition locator training-set held-out-set
                outcome-classes window calibration-authority]} declaration
        training (set training-set)
        held-out (set held-out-set)]
    (when-not (= schema futon2.aif.held-out-split/schema)
      (refuse! :unsupported-schema {:schema schema}))
    (when-not (= futon2.aif.held-out-split/disposition disposition)
      (refuse! :invalid-disposition {:disposition disposition}))
    (when-not (and (map? locator)
                   (every? #(and (string? %) (not (str/blank? %)))
                           ((juxt :root :record) locator)))
      (refuse! :invalid-locator {:locator locator}))
    (when-not (and (vector? held-out-set) (seq held-out-set)
                   (= (count held-out-set) (count held-out)))
      (refuse! :invalid-held-out-set {}))
    (when (seq (set/intersection training held-out))
      (refuse! :partition-overlap {:overlap (set/intersection training held-out)}))
    (when-not (set/subset? required-outcomes (set outcome-classes))
      (refuse! :outcome-classes-incomplete
               {:missing (set/difference required-outcomes (set outcome-classes))}))
    (when-not (= {:event :git-commit-containing-declaration}
                 (:opens-after window))
      (refuse! :window-not-prospective {:opens-after (:opens-after window)}))
    (when-not (pos-int? (:minimum-observations window))
      (refuse! :invalid-window-size {:minimum-observations (:minimum-observations window)}))
    (when-not (= :none calibration-authority)
      (refuse! :premature-calibration-authority {:calibration-authority calibration-authority}))
    declaration))
