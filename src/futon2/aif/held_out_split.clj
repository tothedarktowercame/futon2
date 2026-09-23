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
    ;; Without a registration instant, membership can only be decided by
    ;; date, and a run from earlier the same day -- outcome already known --
    ;; falls inside the window. A prospective split that cannot say what it
    ;; is prospective TO is not prospective (claude-5's review of d55e28f0).
    (when-not (and (string? (:registered-at declaration))
                   (re-matches #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z"
                               (:registered-at declaration)))
      (refuse! :missing-registration-instant
               {:registered-at (:registered-at declaration)}))
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
   member when its target is the declaration's ticket AND its recorded-at
   is strictly after :registered-at -- the instant the declaration was
   registered. Returns {:members […] :not-members […]} with a plain reason
   for each non-member.

   The instant, not the date, and not the commit sha. Comparing dates put
   a run from earlier the SAME DAY inside the window, so a run whose
   outcome was already known counted as held-out -- the retrospective
   inclusion this whole supersession exists to remove (claude-5's review of
   d55e28f0). :starting-point stays as provenance, naming the commit that
   registered the declaration; it is not what membership is decided by,
   because a sha does not order against a timestamp."
  [declaration run-records]
  (let [ticket (get declaration :ticket/id)
        registered-at (:registered-at declaration)]
    (reduce
     (fn [acc {:keys [target recorded-at run-id]}]
       (let [right-target? (= ticket target)
             after? (and registered-at recorded-at
                         (pos? (compare (str recorded-at) (str registered-at))))]
         (if (and right-target? after?)
           (update acc :members conj {:run-id run-id :recorded-at recorded-at})
           (update acc :not-members conj
                   {:run-id run-id
                    :reason (cond (not right-target?) :different-target
                                  (nil? registered-at) :declaration-has-no-registered-at
                                  :else :before-registration)}))))
     {:members [] :not-members []}
     run-records)))
