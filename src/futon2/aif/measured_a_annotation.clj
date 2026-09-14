(ns futon2.aif.measured-a-annotation
  "Pure validation boundary for independently adjudicated measured-A labels.

   Candidate annotations and configured authority are separate arguments.  This
   namespace performs no I/O and does not mint rubric content or authority."
  (:require [clojure.edn :as edn]
            [clojure.set :as set])
  (:import (java.nio.charset StandardCharsets)
           (java.security MessageDigest)
           (java.time Instant)))

(def status-support
  "The machine Status support; kept equal to futon2.aif.belief/status-set."
  #{:spawned :refined :strengthened :addressed :falsified :foreclosed :reopened})

(defn sha256 [s]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                        (.getBytes ^String s StandardCharsets/UTF_8))]
    (apply str (map #(format "%02x" (bit-and % 0xff)) digest))))

(defn- refuse! [reason path & [data]]
  (throw (ex-info "measured-A annotation refused"
                  (merge {:reason reason :path path} data))))

(defn- exact-map! [x ks path]
  (when-not (map? x) (refuse! :expected-map path))
  (let [missing (set/difference ks (set (keys x)))
        extra (set/difference (set (keys x)) ks)]
    (when (seq missing) (refuse! :missing-field path {:missing missing}))
    (when (seq extra) (refuse! :unexpected-field path {:extra extra})))
  x)

(defn- text! [x path]
  (when-not (and (string? x) (not (empty? x)))
    (refuse! :invalid-text path))
  x)

(defn- instant! [x path]
  (text! x path)
  (try (Instant/parse x)
       (catch Exception _ (refuse! :invalid-timestamp path))))

(defn- before-or-equal? [^Instant a ^Instant b]
  (not (.isAfter a b)))

(defn- byte-source! [x path]
  (exact-map! x #{:schema :kind :id :bytes :sha256} path)
  (when-not (= :wm/measured-a-authority-source-v1 (:schema x))
    (refuse! :source-schema-mismatch (conj path :schema)))
  (when-not (keyword? (:kind x)) (refuse! :invalid-source-kind (conj path :kind)))
  (text! (:id x) (conj path :id))
  (text! (:bytes x) (conj path :bytes))
  (when-not (= (:sha256 x) (sha256 (:bytes x)))
    (refuse! :source-byte-hash-mismatch (conj path :sha256)))
  x)

(defn acceptance-subject
  "Construct the fixed-order subject that an independent acceptance must bind."
  [annotation]
  (let [s (:subject annotation)
        r (:rubric annotation)
        c (:conditioning annotation)]
    (array-map
     :schema :wm/measured-a-annotation-subject-v1
     :annotation/id (:annotation/id annotation)
     :entity/id (:entity/id s)
     :run/id (:run/id s)
     :cohort/id (:cohort/id s)
     :attempt/id (:attempt/id s)
     :checkpoint (:checkpoint s)
     :state (:label annotation)
     :state-at (:state-at s)
     :rubric/id (:id r)
     :rubric/version (:version r)
     :criterion/id (:criterion/id r)
     :evidence-cutoff (:evidence-cutoff c)
     :evidence (mapv #(select-keys % [:role :sha256 :observed-at :supports :conflicts])
                     (:evidence annotation)))))

(defn- validate-annotation! [a]
  (exact-map! a #{:schema :annotation/id :subject :label :rubric :conditioning
                  :evidence :provenance :created-at} [])
  (when-not (= :wm/measured-a-annotation-v1 (:schema a))
    (refuse! :annotation-schema-mismatch [:schema]))
  (text! (:annotation/id a) [:annotation/id])
  (when-not (contains? status-support (:label a))
    (refuse! :state-outside-machine-support [:label]))
  (let [s (exact-map! (:subject a)
                      #{:entity/id :run/id :cohort/id :attempt/id :checkpoint
                        :state :state-at :model/revision} [:subject])]
    (doseq [k [:entity/id :run/id :cohort/id :attempt/id :model/revision]]
      (text! (get s k) [:subject k]))
    (when-not (= :closed (:checkpoint s))
      (refuse! :conditioning-checkpoint-mismatch [:subject :checkpoint]))
    (when-not (= (:label a) (:state s))
      (refuse! :subject-state-mismatch [:subject :state]))
    (instant! (:state-at s) [:subject :state-at]))
  (let [r (exact-map! (:rubric a) #{:id :version :criterion/id} [:rubric])]
    (when-not (keyword? (:id r)) (refuse! :invalid-rubric-id [:rubric :id]))
    (when-not (pos-int? (:version r)) (refuse! :invalid-rubric-version [:rubric :version]))
    (when-not (keyword? (:criterion/id r))
      (refuse! :invalid-criterion-id [:rubric :criterion/id])))
  (let [c (exact-map! (:conditioning a)
                      #{:point :transition/id :action/id :action-at
                        :close/disposition :closed-at :evidence-cutoff}
                      [:conditioning])
        action-at (instant! (:action-at c) [:conditioning :action-at])
        state-at (instant! (get-in a [:subject :state-at]) [:subject :state-at])
        closed-at (instant! (:closed-at c) [:conditioning :closed-at])
        cutoff (instant! (:evidence-cutoff c) [:conditioning :evidence-cutoff])
        created (instant! (:created-at a) [:created-at])]
    (when-not (= :post-action-at-close (:point c))
      (refuse! :conditioning-point-mismatch [:conditioning :point]))
    (text! (:transition/id c) [:conditioning :transition/id])
    (text! (:action/id c) [:conditioning :action/id])
    (when-not (keyword? (:close/disposition c))
      (refuse! :invalid-close-disposition [:conditioning :close/disposition]))
    (when-not (and (before-or-equal? action-at state-at)
                   (before-or-equal? state-at closed-at)
                   (before-or-equal? closed-at cutoff)
                   (before-or-equal? cutoff created))
      (refuse! :temporal-order-invalid [:conditioning])))
  (when-not (and (vector? (:evidence a)) (seq (:evidence a)))
    (refuse! :evidence-insufficient [:evidence]))
  (doseq [[i e] (map-indexed vector (:evidence a))]
    (let [p [:evidence i]]
      (exact-map! e #{:role :bytes :sha256 :observed-at :supports :conflicts} p)
      (when (contains? #{:keyword-choice :interest-event-reuse :renamed-argmax
                         :model-posterior :target-disposition} (:role e))
        (refuse! :prohibited-label-source (conj p :role)))
      (text! (:bytes e) (conj p :bytes))
      (when-not (= (:sha256 e) (sha256 (:bytes e)))
        (refuse! :evidence-byte-hash-mismatch (conj p :sha256)))
      (let [observed (instant! (:observed-at e) (conj p :observed-at))
            cutoff (instant! (get-in a [:conditioning :evidence-cutoff])
                             [:conditioning :evidence-cutoff])]
        (when-not (before-or-equal? observed cutoff)
          (refuse! :evidence-after-cutoff (conj p :observed-at))))
      (when-not (and (vector? (:supports e)) (= [(:label a)] (:supports e)))
        (refuse! (if (> (count (:supports e)) 1)
                   :evidence-ambiguous :evidence-insufficient)
                 (conj p :supports)))
      (when-not (and (vector? (:conflicts e)) (empty? (:conflicts e)))
        (refuse! :evidence-conflicting (conj p :conflicts)))))
  (let [p (exact-map! (:provenance a)
                      #{:mode :label/derived-from :retrospective :source-shape}
                      [:provenance])
        retro (:retrospective p)]
    (when-not (= :independent-adjudication (:label/derived-from p))
      (refuse! :label-derived-from-forbidden-source [:provenance :label/derived-from]))
    (exact-map! (:source-shape p) #{:scope :record-type :based-on}
                [:provenance :source-shape])
    (if (= :retrospective (:mode p))
      (do (exact-map! retro #{:status :frozen-evidence-cutoff :reason}
                      [:provenance :retrospective])
          (when-not (= :retrospective (:status retro))
            (refuse! :retrospective-provenance-missing [:provenance :retrospective]))
          (when-not (= (get-in a [:conditioning :evidence-cutoff])
                       (:frozen-evidence-cutoff retro))
            (refuse! :retrospective-cutoff-mismatch
                     [:provenance :retrospective :frozen-evidence-cutoff]))
          (text! (:reason retro) [:provenance :retrospective :reason]))
      (do (when-not (= :contemporaneous (:mode p))
            (refuse! :invalid-provenance-mode [:provenance :mode]))
          (when-not (= {:status :not-applicable} retro)
            (refuse! :retrospective-provenance-mismatch
                     [:provenance :retrospective])))))
  a)

(defn- validate-authority! [a authority]
  (when (nil? authority) (refuse! :authority-missing [:authority]))
  (exact-map! authority #{:schema :scope :rubric :observer :reviewer :acceptance}
              [:authority])
  (when-not (= :wm/measured-a-annotation-authority-v1 (:schema authority))
    (refuse! :authority-schema-mismatch [:authority :schema]))
  (let [rubric (exact-map! (:rubric authority)
                           #{:id :version :status-support :criteria} [:authority :rubric])]
    (when-not (= status-support (set (:status-support rubric)))
      (refuse! :rubric-status-support-mismatch [:authority :rubric :status-support]))
    (when-not (= status-support (set (keys (:criteria rubric))))
      (refuse! :rubric-incomplete [:authority :rubric :criteria]))
    (when-not (= [(:id rubric) (:version rubric)
                  (get-in rubric [:criteria (:label a)])]
                 [(get-in a [:rubric :id]) (get-in a [:rubric :version])
                  (get-in a [:rubric :criterion/id])])
      (refuse! :rubric-binding-mismatch [:rubric])))
  (let [observer (exact-map! (:observer authority) #{:id :origin} [:authority :observer])
        reviewer (exact-map! (:reviewer authority) #{:id :authorization}
                             [:authority :reviewer])]
    (text! (:id observer) [:authority :observer :id])
    (text! (:id reviewer) [:authority :reviewer :id])
    (when (= (:id observer) (:id reviewer))
      (refuse! :observer-reviewer-not-distinct [:authority :reviewer :id]))
    (byte-source! (:origin observer) [:authority :observer :origin])
    (byte-source! (:authorization reviewer) [:authority :reviewer :authorization]))
  (let [subject (acceptance-subject a)
        acceptance (exact-map! (:acceptance authority)
                               #{:status :subject :subject-sha256 :observer/id
                                 :reviewer/id :reviewed-at :artifact}
                               [:authority :acceptance])
        core (dissoc acceptance :artifact)
        artifact (byte-source! (:artifact acceptance) [:authority :acceptance :artifact])]
    (when-not (= :accepted (:status acceptance))
      (refuse! :acceptance-not-accepted [:authority :acceptance :status]))
    (when-not (= subject (:subject acceptance))
      (refuse! :acceptance-subject-mismatch [:authority :acceptance :subject]))
    (when-not (= (sha256 (pr-str subject)) (:subject-sha256 acceptance))
      (refuse! :acceptance-subject-hash-mismatch
               [:authority :acceptance :subject-sha256]))
    (when-not (= [(get-in authority [:observer :id]) (get-in authority [:reviewer :id])]
                 [(:observer/id acceptance) (:reviewer/id acceptance)])
      (refuse! :acceptance-identity-mismatch [:authority :acceptance]))
    (when-not (= core (edn/read-string (:bytes artifact)))
      (refuse! :acceptance-artifact-content-mismatch
               [:authority :acceptance :artifact :bytes]))
    (let [created (instant! (:created-at a) [:created-at])
          reviewed (instant! (:reviewed-at acceptance)
                             [:authority :acceptance :reviewed-at])]
      (when-not (before-or-equal? created reviewed)
        (refuse! :review-before-annotation [:authority :acceptance :reviewed-at]))))
  authority)

(defn validate
  "Validate candidate annotation against separately configured authority.

   Returns {:ok true ...} or {:ok false :refusal {:reason ... :path ...}}.
   Supplying nil authority is a typed absence; no default authority exists."
  [annotation configured-authority]
  (try
    (validate-annotation! annotation)
    (validate-authority! annotation configured-authority)
    {:ok true
     :schema :wm/measured-a-annotation-validation-v1
     :status (:label annotation)
     :subject (acceptance-subject annotation)
     :authority/scope (:scope configured-authority)}
    (catch clojure.lang.ExceptionInfo e
      {:ok false :refusal (ex-data e)})))
