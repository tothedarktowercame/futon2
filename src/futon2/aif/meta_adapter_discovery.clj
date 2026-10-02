(ns futon2.aif.meta-adapter-discovery
  "Source-pinned discovery of checkable META task adapters."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [futon2.aif.meta-field-observation :as field]
            [futon2.aif.mission-criteria :as criteria]
            [futon2.aif.mission-hole-wants :as holes]
            [futon2.aif.observation-checks :as checks]))

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
                  (let [locator (holes/hole-locator code-root
                                                    {:path (get-in row [:source :path])}
                                                    hole)]
                    {:token (holes/want-token hole) :origin :unchecked-checkbox
                     :line (:line hole)
                     :observed (checks/decl-present? text (:decl locator))
                     :locator locator}))
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
                               observed (checks/decl-present? text (:decl locator))]
                           {:token token :origin :verdict-aware-criterion
                            :line (:line criterion) :observed observed
                            :verdict-class (get-in criterion [:verdict-class :class])
                            :locator locator}))))
               vec)]
      (vec (concat checkboxes criterion-observations)))))

(defn- default-read-head-bytes [code-root repo path]
  (let [{:keys [exit out]} (sh/sh "git" "-C" (str code-root "/" repo)
                                  "show" (str "HEAD:" path))]
    (when (zero? exit) (.getBytes out "UTF-8"))))

(defn discover
  "Read every admitted field row exactly once and derive canonical C4 adapter
  evidence. Dependencies are injectable; no lifecycle or model data is read."
  [{:keys [field-observation expected-field-pin code-root read-bytes read-head-bytes]
    :or {code-root "/home/joe/code"
         read-bytes #(java.nio.file.Files/readAllBytes (.toPath (io/file %)))}}]
  (let [read-head-bytes (or read-head-bytes
                            #(default-read-head-bytes code-root %1 %2))
        fv (field/verify field-observation {:expected-snapshot-pin expected-field-pin})]
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
                    location (source-location code-root path)
                    head-bytes (when location
                                 (try (read-head-bytes (:repo location) (:path location))
                                      (catch Throwable _ nil)))
                    head-sha (when head-bytes (field/sha256 head-bytes))
                    found (when (= actual head-sha) (observations code-root row text))
                    false-observations (filterv #(false? (:observed %)) found)]
                (cond
                  (nil? location)
                  (recur (next rows) adapters
                         (conj exclusions {:id (:id row) :kind (:kind row) :source (:source row)
                                           :reason :source-outside-code-root
                                           :evidence {:ordering ordering :observations []}})
                         (inc read-count))

                  (not= actual head-sha)
                  (recur (next rows) adapters
                         (conj exclusions {:id (:id row) :kind (:kind row) :source (:source row)
                                           :reason :head-source-mismatch
                                           :evidence {:expected-source-sha actual
                                                      :head-source-sha head-sha
                                                      :repo (:repo location) :path (:path location)}})
                         (inc read-count))

                  (seq false-observations)
                  (let [chosen (first false-observations)]
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

                  :else
                  (recur (next rows) adapters
                         (conj exclusions {:id (:id row) :kind (:kind row) :source (:source row)
                                           :reason :no-current-false-checkable-want
                                           :evidence {:ordering ordering :observations found}})
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
  ([receipt {:keys [field-observation expected-field-pin code-root read-bytes
                    read-head-bytes] :as authority}]
   (if-not (and (contains? authority :field-observation)
                (contains? authority :expected-field-pin)
                (contains? authority :read-bytes))
     (refusal :external-adapter-source-authority-required {})
     (let [expected (discover {:field-observation field-observation
                               :expected-field-pin expected-field-pin
                               :code-root (or code-root "/home/joe/code")
                               :read-bytes read-bytes
                               :read-head-bytes read-head-bytes})]
       (if (= expected receipt)
         {:schema schema :status :verified :source-pin (:source-pin receipt)}
         (refusal :adapter-discovery-does-not-match-authority
                  {:expected expected :actual receipt}))))))
