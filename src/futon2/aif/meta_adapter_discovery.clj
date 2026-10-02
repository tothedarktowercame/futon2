(ns futon2.aif.meta-adapter-discovery
  "Source-pinned discovery of checkable META task adapters."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.meta-field-observation :as field]
            [futon2.aif.mission-criteria :as criteria]
            [futon2.aif.mission-hole-wants :as holes]))

(def schema :wm/meta-adapter-discovery-v1)
(def ordering [:unchecked-checkbox-document-order
               :verdict-aware-criterion-document-order])

(defn- refusal [reason details]
  {:schema schema :status :refused :reason reason :details details})

(defn- source-location [code-root path]
  (let [prefix (str (str/replace code-root #"/$" "") "/")]
    (when (and (string? path) (str/starts-with? path prefix))
      (let [relative (subs path (count prefix))
            [repo & parts] (str/split relative #"/")]
        (when (and (seq repo) (seq parts))
          {:repo repo :path (str/join "/" parts)})))))

(defn- observations [code-root row text]
  (when-let [{:keys [repo path]} (source-location code-root (get-in row [:source :path]))]
    (let [checkboxes
          (mapv (fn [hole]
                  {:token (holes/want-token hole) :origin :unchecked-checkbox
                   :line (:line hole) :observed false
                   :locator (holes/hole-locator code-root
                                                {:path (get-in row [:source :path])}
                                                hole)})
                (holes/current-checkboxes (:id row) text))
          all-criteria (criteria/criteria (:id row) text)
          criterion-view (criteria/wants all-criteria
                                          {:repo repo :path path
                                           :observe #(str/includes? text (:decl %))})
          by-token (into {} (map (juxt :token identity)) all-criteria)
          criterion-observations
          (->> (:wants criterion-view)
               (keep (fn [token]
                       (when-let [locator (get-in criterion-view [:locators token])]
                         (let [criterion (get by-token token)
                               observed (true? (get-in criterion-view [:universe token]))]
                           (when-not observed
                             {:token token :origin :verdict-aware-criterion
                              :line (:line criterion) :observed false
                              :verdict-class (get-in criterion [:verdict-class :class])
                              :locator locator})))))
               vec)]
      (vec (concat checkboxes criterion-observations)))))

(defn discover
  "Read every admitted field row exactly once and derive canonical C4 adapter
  evidence. Dependencies are injectable; no lifecycle or model data is read."
  [{:keys [field-observation expected-field-pin code-root read-bytes]
    :or {code-root "/home/joe/code"
         read-bytes #(java.nio.file.Files/readAllBytes (.toPath (io/file %)))}}]
  (let [fv (field/verify field-observation {:expected-snapshot-pin expected-field-pin})]
    (if-not (= :verified (:status fv))
      (refusal :field-not-verified {:verification fv})
      (loop [rows (:rows field-observation) adapters [] exclusions [] read-count 0]
        (if-let [row (first rows)]
          (let [path (get-in row [:source :path])
                bytes (try (read-bytes path) (catch Throwable _ nil))
                actual (when bytes (field/sha256 bytes))]
            (cond
              (nil? bytes)
              (recur (next rows) adapters
                     (conj exclusions
                           {:id (:id row) :kind (:kind row) :source (:source row)
                            :reason :source-unreadable})
                     (inc read-count))

              (not= actual (get-in row [:source :sha256]))
              (refusal :source-drift {:id (:id row) :path path
                                      :expected (get-in row [:source :sha256])
                                      :actual actual :read-count (inc read-count)})

              :else
              (let [text (String. ^bytes bytes "UTF-8")
                    found (observations code-root row text)]
                (if (seq found)
                  (let [chosen (first found)]
                    (recur (next rows)
                           (conj adapters
                                 {:id (:id row) :kind (:kind row) :source (:source row)
                                  :adapter :canonical-document-wants
                                  :next-step :observe
                                  :stopping-rule :grounded-progress
                                  :locator (:locator chosen)
                                  :evidence {:ordering ordering :chosen-token (:token chosen)
                                             :observations found}})
                           exclusions (inc read-count)))
                  (recur (next rows) adapters
                         (conj exclusions
                               {:id (:id row) :kind (:kind row) :source (:source row)
                                :reason (if (source-location code-root path)
                                          :no-current-false-checkable-want
                                          :source-outside-code-root)
                                :evidence {:ordering ordering :observations (or found [])}})
                         (inc read-count))))))
          (let [body {:schema schema :status :discovered
                      :field-source-pin expected-field-pin
                      :ordering ordering :read-count read-count
                      :adapters adapters :exclusions exclusions
                      :counts {:field (count (:rows field-observation))
                               :adapters (count adapters) :excluded (count exclusions)
                               :adapters-by-kind (into (sorted-map)
                                                       (frequencies (map :kind adapters)))
                               :excluded-by-kind (into (sorted-map)
                                                       (frequencies (map :kind exclusions)))}}]
            (assoc body :source-pin
                   {:path "wm://meta-adapter-discovery-v1"
                    :sha256 (field/digest body)})))))))

(defn verify
  "Reconstruct discovery from independently supplied field and source bytes."
  ([receipt]
   (refusal :external-adapter-source-authority-required
            {:presented-source-pin (:source-pin receipt)}))
  ([receipt {:keys [field-observation expected-field-pin code-root read-bytes] :as authority}]
   (if-not (and (contains? authority :field-observation)
                (contains? authority :expected-field-pin)
                (contains? authority :read-bytes))
     (refusal :external-adapter-source-authority-required {})
     (let [expected (discover {:field-observation field-observation
                               :expected-field-pin expected-field-pin
                               :code-root (or code-root "/home/joe/code")
                               :read-bytes read-bytes})]
       (if (= expected receipt)
         {:schema schema :status :verified :source-pin (:source-pin receipt)}
         (refusal :adapter-discovery-does-not-match-authority
                  {:expected expected :actual receipt}))))))
