(ns futon2.aif.reviewer-falsifier
  "Machine-checked falsifiers that stand between review prose and build approval."
  (:require [futon2.aif.action-identity :as identity]
            [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def schema :wm/reviewer-falsifier-v1)
(def check-order
  [:mission-standing :selected-want-accounting :evidence-authority
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
           artifact-binding review-gate]}]
  (let [checks {:mission-standing (standing-check mission-standing)
                :selected-want-accounting (want-check selected-want-outcomes)
                :evidence-authority (authority-check selected-want-outcomes commit)
                :standing-disposition (disposition-check selected-want-outcomes disposition)
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
