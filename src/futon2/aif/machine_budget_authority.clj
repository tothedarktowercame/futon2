(ns futon2.aif.machine-budget-authority
  "Verified E1 authority resolver. Reads the complete R6 support and every R11
   value authority from independently configured files. Each file is read once;
   the same bytes are hashed, decoded as strict UTF-8, and parsed as exactly one
   EDN form. Candidate-supplied identity, scope, values, and pins are absent from
   this API and therefore cannot authorize themselves."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.machine-budget-mapping :as mapping])
  (:import (java.io PushbackReader StringReader)
           (java.nio ByteBuffer)
           (java.nio.charset CodingErrorAction StandardCharsets)
           (java.nio.file Files LinkOption Path)
           (java.security MessageDigest)))

(def resolver-version :wm/r6-r11-authority-resolver-v1)

(def ^:private source-labels
  [:ranked-support :field-membership :costs :utilities :budgets])

(def ^:private schemas
  {:ranked-support :wm/r6-ranked-support-authority-v1
   :field-membership :wm/r11-field-membership-authority-v1
   :costs :wm/r11-cost-authority-v1
   :utilities :wm/r11-utility-authority-v1
   :budgets :wm/r11-budget-authority-v1})

(defn- refuse! [kind message data]
  (throw (ex-info message (assoc data :refusal kind))))

(defn- hex [bytes]
  (apply str (map #(format "%02x" (bit-and 0xff %)) bytes)))

(defn- sha256 [bytes]
  (hex (.digest (doto (MessageDigest/getInstance "SHA-256") (.update bytes)))))

(defn- decode-utf8! [bytes path]
  (try
    (let [decoder (doto (.newDecoder StandardCharsets/UTF_8)
                    (.onMalformedInput CodingErrorAction/REPORT)
                    (.onUnmappableCharacter CodingErrorAction/REPORT))]
      (str (.decode decoder (ByteBuffer/wrap bytes))))
    (catch Throwable failure
      (refuse! :r6-r11/source-not-utf8 "Authority source is not strict UTF-8"
               {:path (str path) :cause (.getMessage failure)}))))

(defn- one-form! [text path]
  (try
    (let [reader (PushbackReader. (StringReader. text))
          eof (Object.)
          form (edn/read {:eof eof} reader)
          trailing (edn/read {:eof eof} reader)]
      (when (identical? eof form)
        (refuse! :r6-r11/source-empty "Authority source has no EDN form"
                 {:path (str path)}))
      (when-not (identical? eof trailing)
        (refuse! :r6-r11/source-trailing-form
                 "Authority source must contain exactly one EDN form"
                 {:path (str path)}))
      form)
    (catch clojure.lang.ExceptionInfo failure (throw failure))
    (catch Throwable failure
      (refuse! :r6-r11/source-malformed "Authority source is malformed EDN"
               {:path (str path) :cause (.getMessage failure)}))))

(defn- contained-path! [root relative-path]
  (when-not (and (string? relative-path) (not (.isAbsolute (Path/of relative-path (make-array String 0)))))
    (refuse! :r6-r11/source-path-invalid "Authority path must be relative"
             {:relative-path relative-path}))
  (let [root-path (.normalize (.toAbsolutePath (.toPath (io/file root))))
        path (.normalize (.toAbsolutePath (.resolve root-path relative-path)))]
    (when-not (.startsWith path root-path)
      (refuse! :r6-r11/source-path-escape "Authority path escapes configured root"
               {:root (str root-path) :path (str path)}))
    path))

(defn- read-source! [root label {relative-path :relative-path
                                 expected-sha :sha256 :as pin}]
  (when-not (and (map? pin) (string? expected-sha)
                 (re-matches #"[0-9a-f]{64}" expected-sha))
    (refuse! :r6-r11/source-pin-missing "Configured authority pin is missing"
             {:source-label label :pin pin}))
  (let [path (contained-path! root relative-path)]
    (when-not (Files/isRegularFile path (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))
      (refuse! :r6-r11/source-unreadable "Authority source is not a regular readable file"
               {:source-label label :path (str path)}))
    (let [bytes (try (Files/readAllBytes path)
                     (catch Throwable failure
                       (refuse! :r6-r11/source-unreadable "Authority source read failed"
                                {:source-label label :path (str path)
                                 :cause (.getMessage failure)})))
          actual-sha (sha256 bytes)]
      (when-not (= expected-sha actual-sha)
        (refuse! :r6-r11/source-pin-mismatch "Authority source bytes changed"
                 {:source-label label :path (str path)
                  :expected-sha256 expected-sha :actual-sha256 actual-sha}))
      {:label label :path (str path) :relative-path relative-path
       :sha256 actual-sha :bytes-count (alength bytes)
       :record (one-form! (decode-utf8! bytes path) path)})))

(defn- source-record! [{:keys [label record] :as resolved}]
  (when-not (map? record)
    (refuse! :r6-r11/source-shape-invalid "Authority EDN must be a map"
             {:source-label label}))
  (when-not (= (schemas label) (:schema/version record))
    (refuse! :r6-r11/source-schema-mismatch "Authority source schema is wrong"
             {:source-label label :expected (schemas label)
              :actual (:schema/version record)}))
  (when-not (= :declared-source (:authority record))
    (refuse! (if (= :supported-transformation (:authority record))
               :r6-r11/transformation-unsupported :r6-r11/unknown-authority)
             "Only resolved declared-source records are supported"
             {:source-label label :authority (:authority record)}))
  (doseq [key [:source/id :source/revision :scope :binding]]
    (when-not (contains? record key)
      (refuse! :r6-r11/source-shape-invalid "Authority source metadata is incomplete"
               {:source-label label :missing key})))
  resolved)

(defn- common-metadata! [mode resolved]
  (when-not (contains? #{:isolated-test :production} mode)
    (refuse! :r6-r11/resolver-mode-unknown "Unknown resolver mode" {:mode mode}))
  (let [records (mapv :record resolved)
        bindings (mapv :binding records)
        scopes (mapv :scope records)
        scope (first scopes)]
    (when-not (apply = bindings)
      (refuse! :r6-r11/cross-run-authority "Resolved authorities disagree on identity"
               {:bindings bindings}))
    (when-not (apply = scopes)
      (refuse! :r6-r11/scope-mismatch "Resolved authorities disagree on scope"
               {:scopes scopes}))
    (when (and (= :isolated-test mode) (not= :isolated-test scope))
      (refuse! :r6-r11/scope-laundering "Test configuration cannot claim production scope"
               {:mode mode :scope scope}))
    (when (and (= :production mode) (not= :production scope))
      (refuse! :r6-r11/scope-laundering "Production resolution requires production-scoped sources"
               {:mode mode :scope scope}))
    {:binding (first bindings) :scope scope}))

(defn resolve-and-map
  "Resolve all independently configured authority files, then call the pure E1
   mapper. CONFIG contains only resolver mode/root and expected file pins; the
   ranked support, values, identity and scope are derived from resolved bytes."
  [{:keys [resolver/version mode root sources] :as config}]
  (when-not (= resolver-version version)
    (refuse! :r6-r11/resolver-schema-unsupported "Unsupported authority resolver"
             {:resolver/version version :supported resolver-version}))
  (when-not (and (string? root) (map? sources))
    (refuse! :r6-r11/resolver-config-invalid "Resolver root/sources are missing"
             {:config (select-keys config [:resolver/version :mode :root])}))
  (when-not (= (set source-labels) (set (keys sources)))
    (refuse! :r6-r11/resolver-config-incomplete "Resolver must pin all five sources"
             {:expected source-labels :actual (vec (keys sources))}))
  (let [resolved (mapv #(source-record! (read-source! root % (sources %))) source-labels)
        {:keys [binding scope]} (common-metadata! mode resolved)
        by-label (into {} (map (juxt :label identity) resolved))
        support-record (:record (by-label :ranked-support))
        authority (fn [label]
                    (let [{:keys [path relative-path sha256 bytes-count record]}
                          (by-label label)]
                      {:authority :declared-source
                       :source/id (:source/id record)
                       :source/revision (:source/revision record)
                       :artifact/path path :artifact/relative-path relative-path
                       :artifact/sha256 sha256 :artifact/bytes-count bytes-count
                       :binding binding :values (:values record)}))
        input (merge {:schema/version mapping/schema-version
                      :scope scope
                      :ranked-support (:ordered-support support-record)
                      :authorities
                      {:field-membership (authority :field-membership)
                       :costs (authority :costs)
                       :utilities (authority :utilities)
                       :budgets (authority :budgets)}}
                     binding)
        output (mapping/map-ranked-support input)]
    (assoc output
           :verification {:resolver/version resolver-version
                          :mode mode
                          :scope scope
                          :sources (mapv #(dissoc % :record) resolved)})))
