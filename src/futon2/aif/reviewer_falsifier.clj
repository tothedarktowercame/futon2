(ns futon2.aif.reviewer-falsifier
  "Machine-checked falsifiers that stand between review prose and build approval."
  (:require [futon2.aif.action-identity :as identity]
            [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def schema :wm/reviewer-falsifier-v1)
(def check-order
  [:mission-standing :work-evidence :selected-want-accounting :evidence-authority
   :standing-disposition :artifact-binding :review-execution])

(defn- check [status reason evidence]
  (cond-> {:status status :evidence evidence}
    reason (assoc :reason reason)))

(defn- standing-check [standing]
  (case (:status standing)
    :consistent (check :pass nil (select-keys standing [:source :declarations]))
    :not-applicable (check :not-applicable :non-mission-target standing)
    :conflict (check :fail :mission-standing-conflict
                     (select-keys standing [:source :declarations]))
    (check :fail (or (:reason standing) :mission-standing-unverifiable) standing)))

(defn- want-check [outcomes]
  (if (= :verified (:status outcomes))
    (check :pass nil (select-keys outcomes [:receipt/id :target :selected-wants :outcomes]))
    (check :fail (or (:reason outcomes) :selected-want-accounting-unverifiable) outcomes)))

(defn- repair-binding-check
  [{:keys [selected-action durable-finding repair-contract] :as evidence}]
  (let [obligation (:repair-obligation selected-action)
        repair-id (:repair/id durable-finding)
        declared-discharge (:discharge-contract durable-finding)
        discharge (:discharge repair-contract)]
    (if (and (= :repair-machine-failure (:type selected-action))
             (string? repair-id)
             (= repair-id (:target selected-action))
             (= durable-finding obligation)
             (= repair-id (:repair-id repair-contract))
             (= (:repair/class durable-finding) (:repair-class repair-contract))
             (= (:attempt-id durable-finding) (:failed-attempt repair-contract))
             (= (:failure-stage durable-finding) (:failure-stage repair-contract))
             (= (:failure-kind durable-finding) (:failure-kind repair-contract))
             (map? discharge)
             (seq (:requires discharge))
             (or (nil? declared-discharge) (= declared-discharge discharge)))
      (check :pass nil
             {:kind :repair-machine-failure
              :repair/id repair-id
              :repair/class (:repair/class durable-finding)
              :discharge-contract discharge
              :selected-action-sha256 (identity/digest selected-action)
              :durable-finding-sha256 (identity/digest durable-finding)})
      (check :fail :repair-work-evidence-mismatch evidence))))

(defn- work-evidence-check [{:keys [kind] :as evidence}]
  (case kind
    :ordinary-cascade (check :not-applicable :ordinary-cascade-uses-selected-wants
                             {:kind kind})
    :repair-machine-failure (repair-binding-check evidence)
    (check :fail :work-evidence-kind-unavailable evidence)))

(defn- authoritative-row? [{:keys [result after-locator]} commit]
  (and (boolean? (:observed result))
       (= commit (:sha after-locator))
       (= commit (get-in result [:evidence :resolved-sha]))
       (string? (get-in result [:evidence :repo]))
       (string? (get-in result [:evidence :path]))))

(defn- authority-check [outcomes commit]
  (let [rows (mapv #(get-in % [:after :measurement]) (:outcomes outcomes))]
    (if (and (seq rows) (every? #(authoritative-row? % commit) rows))
      (check :pass nil {:commit commit :measurements rows})
      (check :fail :revision-authority-missing {:commit commit :measurements rows}))))

(defn- disposition-check [outcomes disposition]
  (let [classes (mapv :classification (:outcomes outcomes))
        expected (if (and (seq classes) (every? #{:reached} classes)) :resolved :still-live)]
    (cond
      (nil? disposition) (check :not-applicable :no-disposition-claimed
                                {:expected expected :outcome-receipt (:receipt/id outcomes)})
      (= expected disposition) (check :pass nil {:claimed disposition :expected expected
                                                 :outcome-receipt (:receipt/id outcomes)})
      :else (check :fail :standing-disposition-mismatch
                   {:claimed disposition :expected expected
                    :outcome-receipt (:receipt/id outcomes)}))))

(defn- artifact-check [target commit binding]
  (if (and (string? target) (string? commit)
           (= commit (:commit binding)) (true? (:corroborates? binding)))
    (check :pass nil (select-keys binding [:repo :commit :pre-dispatch-head
                                           :observed-head :corroborates?]))
    (check :fail :artifact-binding-mismatch
           {:target target :commit commit :artifact-binding binding})))

(defn- execution-check [gate]
  (if (true? (:passed? gate))
    (check :pass nil (select-keys gate [:required? :code-files :executed?
                                        :tool-events :execution-source :passed?]))
    (check :fail :review-execution-evidence-missing gate)))

(defn receipt
  [{:keys [target commit mission-standing selected-want-outcomes disposition
           artifact-binding review-gate work-evidence]}]
  (let [repair? (= :repair-machine-failure (:kind work-evidence))
        checks {:mission-standing (standing-check mission-standing)
                :work-evidence (work-evidence-check work-evidence)
                :selected-want-accounting
                (if repair?
                  (check :not-applicable :repair-uses-durable-finding-contract
                         {:repair/id (get-in work-evidence [:durable-finding :repair/id])})
                  (want-check selected-want-outcomes))
                :evidence-authority
                (if repair?
                  (check :not-applicable :repair-uses-grounded-discharge-evaluator
                         {:repair/id (get-in work-evidence [:durable-finding :repair/id])})
                  (authority-check selected-want-outcomes commit))
                :standing-disposition
                (if repair?
                  (check :not-applicable :repair-disposition-follows-grounded-close
                         {:repair/id (get-in work-evidence [:durable-finding :repair/id])})
                  (disposition-check selected-want-outcomes disposition))
                :artifact-binding (artifact-check target commit artifact-binding)
                :review-execution (execution-check review-gate)}
        applicable (remove #(= :not-applicable (get-in checks [% :status])) check-order)
        failed (vec (filter #(not= :pass (get-in checks [% :status])) applicable))
        base {:schema schema :target target :commit commit :checks checks
              :status (if (seq failed) :refused :verified)
              :failed-checks failed}]
    (assoc base :receipt/id (identity/digest base))))

(defn verify
  "Reconstruct from canonical inputs. A supplied pass cannot verify itself."
  [candidate inputs]
  (let [expected (receipt inputs)]
    (cond
      (not= schema (:schema candidate))
      {:status :refused :reason :reviewer-falsifier-schema-mismatch}
      (not= (:target expected) (:target candidate))
      {:status :refused :reason :reviewer-falsifier-target-mismatch}
      (not= (:commit expected) (:commit candidate))
      {:status :refused :reason :reviewer-falsifier-commit-mismatch}
      (not= (set check-order) (set (keys (:checks candidate))))
      {:status :refused :reason :reviewer-falsifier-check-census-mismatch}
      (not= expected candidate)
      {:status :refused :reason :reviewer-falsifier-evidence-mismatch
       :expected expected :candidate candidate}
      (not= :verified (:status expected))
      {:status :refused :reason :reviewer-falsifier-applicable-check-failed
       :receipt expected}
      :else {:status :verified :receipt/id (:receipt/id expected)})))

(defn approved?
  "Approval prose is necessary but cannot substitute for verified falsifiers."
  [{:keys [review-state review-verdict review-gate falsifier-verification]}]
  (and (= "done" review-state)
       (= :approve review-verdict)
       (true? (:passed? review-gate))
       (= :verified (:status falsifier-verification))))
