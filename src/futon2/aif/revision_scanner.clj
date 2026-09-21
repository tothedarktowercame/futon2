(ns futon2.aif.revision-scanner
  "Standalone, record-only trailer/receipt scanner. Does not load model code."
  (:require [clojure.edn :as edn] [clojure.java.io :as io]
            [clojure.java.shell :as sh] [clojure.string :as str]
            [clojure.pprint :as pp]
            [futon2.aif.action-identity :as identity])
  (:import [java.time Instant Duration]))

(defn git [repo & args]
  (let [{:keys [exit out err]} (apply sh/sh "git" "-C" repo args)]
    (when-not (zero? exit) (throw (ex-info "Git read failed" {:repo repo :args args :error err})))
    out))
(defn instant [x]
  (try (when (string? x) (Instant/parse x)) (catch Exception _ nil)))
(defn after? [a b]
  (boolean (when-let [a (instant a)] (when-let [b (instant b)] (.isAfter a b)))))

(defn expectation-class
  "Exclude occurrence, model digest, probability and clock. Include effect token
   and ordered pattern identities, so unrelated wants/orders do not recur."
  [s]
  {:rule (get-in s [:expectation :rule])
   :target (get-in s [:expectation :scope :target]) :token (:token s)
   :action-class (get-in s [:occurrence :action/value :kind])
   :patterns (mapv :id (get-in s [:occurrence :action/value :precedence]))
   :model-part (:model-part s)})

(defn revisions [repo revision]
  (vec
   (for [chunk (str/split (git repo "log" "--format=%x1e%H%x00%cI%x00%(trailers:key=Surprise,valueonly)" revision) #"\u001e")
         :when (not (str/blank? chunk))
         :let [[sha at trailers] (str/split chunk #"\u0000" 3)]
         id (distinct (remove str/blank? (map str/trim (str/split-lines (or trailers "")))))]
     {:repo repo :commit sha :at at :surprise/id id
      :paths (vec (remove str/blank? (str/split-lines
                                    (git repo "diff-tree" "--root" "--no-commit-id" "--name-only" "-r" "-m" "--first-parent" sha))))})))

(defn read-record [path]
  ;; One form per receipt. Trailing forms must not be silently ignored.
  (with-open [r (java.io.PushbackReader. (io/reader path))]
    (let [x (edn/read {:eof ::eof} r) more (edn/read {:eof ::eof} r)]
      (when (or (= ::eof x) (not= ::eof more))
        (throw (ex-info "Expected one EDN receipt" {:path path})))
      x)))

(defn recorded-evaluations [r]
  ;; Older tick records retain evaluated Q trajectories instead of the later
  ;; token-outcome carrier. This is evidence of planning consumption only.
  (vec (for [policy (get-in r [:decision :g-term-decomposition :policies])
             :let [action (:id policy)
                   domains (get-in r [:decision :selection-certificate :token-belief-stage :domain-inputs])
                   declaration (:declaration (first (filter #(= (:target action) (:target %)) domains)))
                   q (get-in policy [:terms :Q :value])]
             :when (and (= :present (get-in policy [:terms :Q :status]))
                        (seq (:steps q)) (every? #(map? (:belief %)) (:steps q)) (seq (:want declaration)))]
         {:status :recorded-trajectory :receipt-kind :policy-term-Q
          :prediction-rule :positive-marginal-support :target (:target action)
          :action action :rollout q
          :wanted (mapv #(hash-map :token [(:target action) %]) (:want declaration))
          :intended-outputs (into #{} (mapcat :produces) (:precedence action))})))

(defn run-view [path]
  (let [r (read-record path) j (or (get-in r [:payload :judgment]) r)]
    {:path path :evaluations (recorded-evaluations r) :at (or (:recorded-at r) (:startedAt r))
     :run/id (or (:run/id r) (get-in r [:payload :close-retention :occurrence :run/id]))
     :source (or (:runner/source r) (:loaded-code-identity r) (:runner/source j))
     :prediction (or (:token-outcome-prediction j)
                     (get-in j [:token-outcome-comparison :prediction]))}))

(defn exercised? [class p]
  (and (or (= :frozen (:status p))
           (and (= :recorded-trajectory (:status p)) (= :policy-term-Q (:receipt-kind p))))
       (seq (:rollout p)) (= (:rule class) (:prediction-rule p))
       (= (:target class) (:target p))
       (= (:action-class class) (get-in p [:action :kind]))
       (= (:patterns class) (mapv :id (get-in p [:action :precedence])))
       (some #(= (:token class) (:token %)) (:wanted p))
       (case (:model-part class)
         :B-effect (contains? (:intended-outputs p) (:token class))
         (:D-prediction :D-or-external) true
         false)))

(defn changed-parts [bindings s revision]
  (vec (for [{:keys [repo path namespace model-part target] :as binding} bindings
             :when (and (= repo (:repo revision)) (= model-part (:model-part s))
                        (or (nil? target) (= target (get-in s [:expectation :scope :target])))
                        (some #{path} (:paths revision)))]
         (assoc binding :namespace namespace
                :new-sha256 (identity/sha256 (git repo "show" (str (:commit revision) ":" path)))
                :old-sha256 (try (identity/sha256 (git repo "show" (str (:commit revision) "^:" path)))
                                (catch Exception _ :absent))))))

(defn loaded? [revision part run]
  (let [entry (get-in run [:source :namespaces (:namespace part)])]
    (and (= :source-digest-at-namespace-load (get-in run [:source :identity-kind]))
         (after? (:at run) (:at revision))
         (= :captured (get-in entry [:loaded-source :status]))
         (= (:new-sha256 part) (get-in entry [:loaded-source :sha256]))
         (= (.getCanonicalPath (io/file (:repo part) (:path part))) (:canonical-path entry))
         ;; A matching historical load can be reused; require its captured clock
         ;; after the revision to avoid credit for a coincidentally equal old file.
         (after? (get-in entry [:loaded-source :captured-at]) (:at revision))
         (not (after? (get-in entry [:loaded-source :captured-at]) (:at run))))))

(defn grade [s revision bindings runs]
  (let [class (expectation-class s)
        parts (changed-parts bindings s revision)
        later (filter #(after? (:at %) (:at revision)) runs)
        loaded (filter #(and (seq parts) (every? (fn [part] (loaded? revision part %)) parts)) later)
        consumed (filter #(some (partial exercised? class) (cons (:prediction %) (:evaluations %))) loaded)
        untested (and (seq loaded) (empty? consumed) (some #(or (:prediction %) (seq (:evaluations %))) later))
        attained (cond (seq consumed) [:committed :loaded :consumed]
                       (seq loaded) [:committed :loaded] :else [:committed])]
    {:surprise/id (:surprise/id s) :expectation-class class :revision revision
     :revision-kind :unclassified :parts parts
     :grade (if untested :untested (peek attained)) :attained-grades attained
     :successor-status (if (seq consumed) :exercised :untested)
     :loaded-evidence (mapv :path loaded) :consumed-evidence (mapv :path consumed)
     :limitations (cond-> [:candidate-not-capability :revision-kind-needs-review]
                    (empty? parts) (conj :no-declared-changed-part-binding))}))

(defn scan
  [{:keys [repos surprise-files run-files bindings as-of unanswered-after-seconds] :as config}]
  (when-not (and (instant as-of) (pos-int? unanswered-after-seconds))
    (throw (ex-info "Declare scan time and positive unanswered interval" {})))
  (let [heads (into (sorted-map) (map (fn [repo]
                                        [repo (str/trim (git repo "rev-parse" (get-in config [:revisions repo] "HEAD")))]) repos))
        surprises (mapcat (fn [p] (map #(assoc % :record-path p) (read-record p))) surprise-files)
        grouped (group-by :surprise/id surprises)
        conflicts (set (for [[id rows] grouped
                             :when (> (count (set (map #(dissoc % :record-path) rows))) 1)] id))
        valid (into {} (for [[id rows] grouped :when (and id (not (conflicts id))
                                                        (= :wm/surprise-v1 (:schema (first rows))))]
                         [id (first rows)]))
        revisions (filter #(not (after? (:at %) as-of)) (mapcat #(revisions % (heads %)) repos))
        unknown (filter #(not (contains? valid (:surprise/id %))) revisions)
        joined (filter #(and (contains? valid (:surprise/id %))
                             (after? (:at %) (let [s (valid (:surprise/id %)) t (get-in s [:observation :observed-at])]
                                               (if (instant t) t (get-in s [:expectation :declared-at]))))
                             (not (after? (:at %) as-of))) revisions)
        runs (filter #(not (after? (:at %) as-of)) (mapv run-view run-files))
        candidates (mapv #(grade (valid (:surprise/id %)) % bindings runs) joined)
        unanswered (for [[id s] valid
                         :let [since (or (when (instant (get-in s [:observation :observed-at]))
                                           (get-in s [:observation :observed-at]))
                                         (get-in s [:expectation :declared-at]))]
                         :when (and (instant since) (not-any? #(= id (:surprise/id %)) joined)
                                    (>= (.getSeconds (Duration/between (instant since) (instant as-of)))
                                        unanswered-after-seconds))]
                     {:kind :unanswered :surprise/id id :expectation-class (expectation-class s)
                      :since since :clock-basis (if (= since (get-in s [:observation :observed-at]))
                                                 :observation :declaration-time-proxy)})
        recurrences (for [revision joined :let [s (valid (:surprise/id revision))]
                          [_ later] valid
                          :when (and (not= (:surprise/id s) (:surprise/id later))
                                     (not= (get-in s [:occurrence :action/id]) (get-in later [:occurrence :action/id]))
                                     (= (expectation-class s) (expectation-class later))
                                     (after? (get-in later [:expectation :declared-at]) (:at revision))
                                     (not (after? (get-in later [:expectation :declared-at]) as-of)))]
                      {:kind :recurring-after-revision :expectation-class (expectation-class s)
                       :surprise/id (:surprise/id later) :prior-surprise (:surprise/id s)
                       :revision (select-keys revision [:repo :commit :at])
                       :consumption-established? (boolean (some #(and (= revision (:revision %))
                                                                                    (= :consumed (:grade %))) candidates))})]
    {:schema :wm/revision-scan-v1 :mode :record-only :as-of as-of
     :repo-heads heads
     :input-digests (mapv #(hash-map :path % :sha256 (identity/sha256 (slurp %)))
                         (distinct (concat surprise-files run-files)))
     :declaration (assoc config :revisions heads) :surprise-count (count valid)
     :learning-event-candidates candidates :friction-candidates (vec (concat unanswered recurrences))
     :flags (vec (concat (map #(assoc % :kind :unknown-surprise-id) unknown)
                         (map #(hash-map :kind :conflicting-surprise-records :surprise/id %) conflicts)))}))

(defn -main [config-path]
  (pp/pprint (scan (read-record config-path))))
