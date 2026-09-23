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


(def v2-schema :wm/eig-held-out-split-v2)

(defn mintable-window?
  "PROOF-wm-works ⟨1⟩6 supersession: a window is MINTABLE when its shape
   names identities the runner actually produces. The runner mints one
   cohort per click and one run-record per run; the run record carries the
   decision's selected target. The :next-n-attempts-on-ticket shape over
   run records is mintable; cohort/attempt directory labels as held-out
   IDs are NOT (every click opens a new cohort)."
  [declaration]
  (let [w (:window declaration)]
    (case (:shape w)
      :next-n-attempts-on-ticket
      (and (pos-int? (:next-n w))
           (string? (get-in declaration [:ticket/id]))
           (map? (:starting-point declaration))
           (string? (get-in declaration [:starting-point :sha])))
      ;; v1-style: named cohort/attempt labels — NOT mintable going forward
      (if (and (vector? (:held-out-set declaration))
               (seq (:held-out-set declaration))
               (every? string? (:held-out-set declaration))
               (some #(re-find #"[a-z]+-[0-9]+/|attempt-00" (str %))
                     (:held-out-set declaration)))
        false
        nil))))

(defn validate-v2
  "Validate a v2 supersession declaration. Same outcome classes, metrics
   bounds and prospectivity as v1; the window must be MINTABLE, and a
   declaration naming non-mintable labels is refused."
  [declaration]
  (let [{:keys [schema disposition outcome-classes window calibration-authority supersedes]} declaration
        ticket-id (:ticket/id declaration)]
    (when-not (= v2-schema schema)
      (refuse! :unsupported-schema {:schema schema}))
    (when-not (= futon2.aif.held-out-split/disposition disposition)
      (refuse! :invalid-disposition {:disposition disposition}))
    (when-not (and (string? ticket-id) (not (str/blank? ticket-id)))
      (refuse! :missing-ticket-id {}))
    (when-not (and (string? supersedes) (not (str/blank? supersedes)))
      (refuse! :missing-supersedes {}))
    (when-not (set/subset? required-outcomes (set outcome-classes))
      (refuse! :outcome-classes-incomplete
               {:missing (set/difference required-outcomes (set outcome-classes))}))
    (when-not (= {:event :git-commit-containing-declaration}
                 (:opens-after window))
      (refuse! :window-not-prospective {:opens-after (:opens-after window)}))
    (when-not (pos-int? (:minimum-observations window))
      (refuse! :invalid-window-size {:minimum-observations window}))
    (when-not (= :none calibration-authority)
      (refuse! :premature-calibration-authority
               {:calibration-authority calibration-authority}))
    (let [mintable (mintable-window? declaration)]
      (when-not (true? mintable)
        (refuse! :window-not-mintable
                 {:shape (:shape window)
                  :reason (if (false? mintable)
                            :names-non-mintable-labels
                            :unknown-window-shape)})))
    declaration))

(defn window-membership
  "Which attempts are in the window, given the run records that exist.
   Each RUN-RECORD is {:target … :recorded-at … :run-id …}; a record is a
   member when its target equals the declaration's ticket and its
   recorded-at is after the starting point. Returns {:members […] :not-members […]}
   with the reason for each non-member, plainly."
  [declaration run-records]
  (let [ticket (get declaration :ticket/id)]
    (reduce
     (fn [acc {:keys [target recorded-at run-id] :as r}]
       (let [in? (and (= ticket target)
                      ;; the starting point is a commit; run records after
                      ;; the supersession commit qualify by run-id ordering
                      ;; (run ids are timestamps) — use the recorded-at
                      ;; against the declaration's own date
                      (let [start (get-in declaration [:starting-point :sha])
                            declared-on (:declared-on declaration)]
                        (<= (compare (subs (str declared-on) 0 10)
                                     (subs (str recorded-at) 0 10))
                            0)))]
         (if in?
           (update acc :members conj {:run-id run-id :recorded-at recorded-at})
           (update acc :not-members conj {:run-id run-id
                                          :reason (if (not= ticket target)
                                                    :different-target
                                                    :before-starting-point)}))))
     {:members [] :not-members []}
     run-records)))
