(ns futon2.aif.attempt-learning-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.efe :as efe]
            [futon2.aif.cascade-selection :as selection]
            [futon2.aif.token-outcome :as outcome]
            [futon2.aif.d-predecessor-task-authority :as task]
            [futon2.aif.d-predecessor-task-authority-test :as fixture]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.learning-trial-test :as historical]))

 ;; Let the new admission assertions run (and fail) on base without the API.
(doseq [n '[futon2.aif.attempt-learning futon2.aif.learning-trial-ledger]]
  (try (require n) (catch java.io.FileNotFoundException _ nil)))
(defn admit [input]
  (when-let [n (find-ns 'futon2.aif.attempt-learning)] ((ns-resolve n 'receipt) input)))
(defn persist! [root receipt]
  (when-let [n (find-ns 'futon2.aif.learning-trial-ledger)] ((ns-resolve n 'record!) root receipt)))

(defn production-snapshot []
  (let [f historical/fixture terms (:terms f) c (get-in terms [:C :value])
        terminal (:distribution (last (:steps c)))
        spec {:want (set (keys (:weights terminal))) :weights (:weights terminal)
              :lam 1 :mu 0 :evidence #{} :zeroed (:zeroed terminal) :c-schedule (:schedule c)}]
    {:ranked (efe/rank-cascade-actions {:cascade-belief (get-in terms [:D :value])}
                                      (mapv :id (:candidates f))
                                      {:horizon-steps 2 :cascade-spec spec :adjudication-rates (get-in terms [:A :value])})
     :posterior (selection/selection-posterior {:beta 1 :candidates (:candidates f)})}))

(def effects #{["target" :artifact] ["target" :absent]})
(def action {:kind :cascade-candidate :id :C1 :target "target"
             :precedence [{:id :deliver :produces effects
                           :guard {:status :interpreted :operator :and
                                   :clauses [{:status :interpreted :present #{} :absent effects}]}}]})

(defn with-trial [f]
  (fixture/with-artifact
   {:action action :universe effects
    :locators {:absent {:class :C3 :repo "repo" :sha "HEAD" :path "absent.clj"}}}
   (fn [{:keys [inputs expected jobs root] :as context}]
     ;; Add a declared want through real capture, hashes, prompt binding, and
     ;; verifier. No authority or observation check is stubbed.
     (let [file (get-in inputs [:dispatch :declarations 0 :path])
           declaration (assoc (edn/read-string (slurp file)) :want #{:artifact :absent})
           _ (spit file (pr-str declaration))
           pins [{:path file :sha256 (evidence/sha256 (java.nio.file.Files/readAllBytes (.toPath (io/file file))))}]
           dispatch (task/capture {:occurrence (:occurrence expected) :carry-occurrence-id "carry"
                                     :universe effects :declaration-reads pins
                                     :before (get-in inputs [:dispatch :before])})
             author (assoc-in (jobs "author-job") [:events 0 :text] (task/prompt-binding dispatch))
             reviewer (assoc-in (jobs "review-job") [:events 0 :text]
                                (str (task/prompt-binding dispatch) "\nReview " (:commit context)
                                     "\nRepository: " (:repo context)))
             jobs {"author-job" author "review-job" reviewer}
             source (task/claim (assoc inputs :dispatch dispatch :author-job author :review-job reviewer))
             expected (assoc expected :declaration-pins pins)
             prediction {:status :frozen :action action :initial-belief {#{} 1} :horizon 1
                         :wanted (mapv #(hash-map :token % :predicted 1) (sort-by pr-str effects))}
             comparison (outcome/compare-outcomes prediction (:after-token-evidence source) (:commit context))]
         (f {:comparison comparison :occurrence (:occurrence expected) :route :fresh-author
             :source-record source :expected expected :read-job jobs} (str root "/learning"))))))

(deftest real-signed-positive-and-negative-admission-and-idempotent-ledger
  (with-trial
    (fn [input root]
      (let [production-before (pr-str (production-snapshot))
            before (pr-str input)
            r (admit input)
            rows (:trials r)
            saved (persist! root r)
            file (io/file root "attempts.edn")
            bytes (when (.exists file) (slurp file))
            duplicate (persist! root (admit input))]
        (is (= 2 (count rows)))
        (is (= #{true false} (set (map :after-observation rows))))
        (is (every? #(= :admitted-at-attempt-grain (:status %)) rows))
        (is (every? :counted? (:trials saved)))
        (is (= 2 (count (str/split-lines (or bytes "")))))
        (is (= {:success 1 :failure 1}
               (when bytes (apply merge-with + (map (comp :increment edn/read-string) (str/split-lines bytes))))))
        (is (every? #(= :duplicate-replay (:reason %)) (:trials duplicate)))
        (is (every? #(false? (:counted? %)) (:trials duplicate)))
        (is (= bytes (when (.exists file) (slurp file))))
        (is (= before (pr-str input)))
        (is (= production-before (pr-str (production-snapshot))))
        (is (= (:posterior historical/fixture) (:posterior (production-snapshot))))
        (is (= #{9/11 10/11} (set (map #(get-in % [:attempt-beta :delivery-mean]) rows))))
        (let [next-input (assoc input :route :recovery)]
          (is (every? #(= :route-mismatch (:reason %)) (:trials (admit next-input)))))
        (let [revised (update r :trials #(mapv (fn [row] (assoc row :meaning-sha256 "new-meaning")) %))]
          (is (every? #(= :revised-meaning (:reason %)) (:trials (persist! root revised))))
          (is (= bytes (when (.exists file) (slurp file)))))))))

(deftest held-endpoints-cannot-be-counted
  (with-trial
    (fn [input root]
      (doseq [[reason bad] [[:observation-missing (assoc-in input [:comparison :tokens 0 :observed] {:status :missing})]
                            [:unselected-target (assoc-in input [:comparison :prediction :action :target] "other")]
                            [:effect-already-present (assoc-in input [:comparison :prediction :initial-belief] {effects 1})]
                            [:illustrative-prior-invalid (assoc input :prior {:alpha 0 :beta 1})]
                            [:revised-meaning (assoc input :previous-meanings (zipmap effects (repeat "old")))]]]
        (let [r (admit bad)]
          (is (some #(= reason (:reason %)) (:trials r)))
          (is (every? #(false? (:counted? %)) (:trials r)))))
      (let [r (admit (assoc input :read-job (constantly nil)))]
        (is (every? #(= :held (:status %)) (:trials r)))
        (persist! root r)
        (is (not (.exists (io/file root "attempts.edn")))))
      (is (every? #(= :artifact-revision-mismatch (:reason %))
                  (:trials (admit (assoc-in input [:comparison :artifact-sha] "wrong")))))
      (is (some #(= :prediction-mismatch (:reason %))
                (:trials (admit (assoc-in input [:comparison :tokens 0 :predicted] 0))))))))

(deftest historical-occurrence-is-not-upgraded
  (let [r (admit (historical/inputs))]
    (is (= :held (get-in r [:trials 0 :status])))
    (is (= :occurrence-v2-required (get-in r [:trials 0 :reason])))
    (is (false? (get-in r [:trials 0 :counted?])))))
