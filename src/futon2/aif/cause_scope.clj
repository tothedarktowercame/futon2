(ns futon2.aif.cause-scope
  "Machine-owned execution and evidence for a diagnosis's declared cause boundary."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.load-identity :as identity]))

(def allowed-predicate-ops
  #{:result-path-equals :result-path-present :result-path-absent})

(defn predicate? [p]
  (and (map? p)
       (contains? allowed-predicate-ops (:op p))
       (vector? (:path p))
       (if (= :result-path-equals (:op p)) (contains? p :value) true)))

(defn scope? [scope]
  (and (map? scope) (string? (:cause-entry scope))
       (map? (:input-pin scope)) (string? (get-in scope [:input-pin :path]))
       (re-matches #"[0-9a-f]{64}" (or (get-in scope [:input-pin :sha256]) ""))
       (vector? (:input-path scope))
       (re-matches #"[0-9a-f]{64}" (or (:input-digest scope) ""))
       (predicate? (:healthy-predicate scope))))

(defn- read-pin [pin]
  (let [bytes (java.nio.file.Files/readAllBytes (.toPath (io/file (:path pin))))]
    (when-not (= (:sha256 pin) (identity/sha256 bytes))
      (throw (ex-info "Cause input pin mismatch" {:kind :cause-scope/input-pin-mismatch})))
    (edn/read-string (String. bytes "UTF-8"))))

(defn stable-digest
  "SHA-256 of VALUE printed under fixed bindings. The minting side ran in the
  serving JVM with *print-namespace-maps* false and the acceptance side in a
  fresh `clojure -M:test` JVM with Clojure's default true; the same bytes then
  digested differently and every acceptance refused `:cause-scope/input-mismatch`
  (injury 93d40d07, clicks 7-8 of 2026-11-01). false is the binding the retained
  scopes were minted under, so they stay valid."
  [value]
  (identity/sha256
   (.getBytes ^String (binding [*print-namespace-maps* false *print-length* nil
                                *print-level* nil *print-meta* false]
                        (pr-str value))
              "UTF-8")))

(defn seal [cause-entry]
  (let [record (read-pin (get-in cause-entry [:input :record-pin]))
        input (get-in record (get-in cause-entry [:input :path]) ::absent)]
    (when (= ::absent input)
      (throw (ex-info "No retained carrier for cause boundary"
                      {:kind :cause-scope-unretained})))
    {:cause-entry (:var cause-entry)
     :input-pin (get-in cause-entry [:input :record-pin])
     :input-path (get-in cause-entry [:input :path])
     :input-digest (stable-digest input)
     :healthy-predicate (:healthy-predicate cause-entry)}))

(defn- healthy? [predicate result]
  (let [v (get-in result (:path predicate) ::absent)]
    (case (:op predicate)
      :result-path-equals (= v (:value predicate))
      :result-path-present (not= ::absent v)
      :result-path-absent (= ::absent v))))

(defn assert-repair
  "Invoke the sealed public cause boundary on its pinned retained carrier.
  Returns its CauseScopeReceipt and throws an assertion when it is unhealthy."
  [scope]
  (when-not (scope? scope)
    (throw (ex-info "Malformed cause scope" {:kind :cause-scope/malformed})))
  (let [record (read-pin (:input-pin scope))
        input (get-in record (:input-path scope) ::absent)
        _ (when (= ::absent input)
            (throw (ex-info "Retained carrier absent" {:kind :cause-scope-unretained})))
        digest (stable-digest input)
        _ (when-not (= digest (:input-digest scope))
            (throw (ex-info "Retained carrier changed" {:kind :cause-scope/input-mismatch})))
        entry (requiring-resolve (symbol (:cause-entry scope)))
        result (entry input)
        healthy (healthy? (:healthy-predicate scope) result)
        receipt {:schema :wm/cause-scope-receipt-v1 :scope scope
                 :observed-entry (:cause-entry scope) :invoked true
                 :predicate-observed-result true
                 :result-digest (stable-digest result)
                 :healthy? healthy :failures (if healthy 0 1) :errors 0}]
    (when-let [path (System/getenv "FUTON_CAUSE_SCOPE_RECEIPT")]
      (spit path (pr-str receipt)))
    (assert healthy (str "cause remains unhealthy: " (:cause-entry scope)))
    receipt))

(defn- ns-require-aliases
  "Alias map from a test source's own ns form: {alias namespace}. Only
  [:require ... :as alias] entries matter for resolving a designated call."
  [forms]
  (let [ns-form (first (filter #(and (seq? %) (= 'ns (first %))) forms))
        requires (->> (rest ns-form)
                      (filter #(and (seq? %) (= :require (first %))))
                      (mapcat rest))
        entry-alias (fn [entry]
                      (when (vector? entry)
                        (let [as-pair (first (filter #(and (seq? %) (= :as (first %)))
                                                     (partition 2 2 nil (rest entry))))]
                          (when (and as-pair (symbol? (second as-pair)))
                            {(second as-pair) (first entry)}))))]
    (into {} (keep entry-alias requires))))

(defn- resolve-alias-call
  "Resolve a call head through the source's own :require aliases. An aliased
  head like cause-scope/assert-repair resolves to the designated var; a
  fully-qualified or unknown head is returned unchanged."
  [head aliases]
  (if (and (symbol? head) (clojure.string/includes? (str head) "/"))
    (let [[pre post] (clojure.string/split (str head) #"/" 2)]
      (if-let [lib (get aliases (symbol pre))]
        (symbol (str lib "/" post))
        head))
    head))

(defn designated-test-defect [source expected-scope]
  (try
    (let [forms (binding [*read-eval* false]
                  (with-open [r (java.io.PushbackReader. (java.io.StringReader. source))]
                    (loop [xs []]
                      (let [x (read {:eof ::eof} r)]
                        (if (= ::eof x) xs (recur (conj xs x)))))))
          aliases (ns-require-aliases forms)
          tests (filter #(and (seq? %) (= 'deftest (first %))) forms)
          body (when (= 1 (count tests)) (drop 2 (first tests)))
          call (when (= 1 (count body)) (first body))]
      (cond
        (not= 1 (count tests)) :cause-scope/designated-test-count
        (not (and (seq? call)
                  (= 'futon2.aif.cause-scope/assert-repair
                     (resolve-alias-call (first call) aliases))
                  (= 2 (count call)))) :cause-scope/designated-call-required
        (not= expected-scope (second call)) :cause-scope/scope-mismatch
        :else nil))
    (catch Throwable _ :cause-scope/source-unreadable)))

(defn red-receipt? [scope receipt pin]
  (and (map? pin) (string? (:path pin)) (string? (:sha256 pin))
       (try
         (let [bytes (java.nio.file.Files/readAllBytes (.toPath (io/file (:path pin))))]
           (and (= (:sha256 pin) (identity/sha256 bytes))
                (= receipt (edn/read-string (String. bytes "UTF-8")))))
         (catch Throwable _ false))
       (= :wm/cause-scope-receipt-v1 (:schema receipt))
       (= scope (:scope receipt)) (= (:cause-entry scope) (:observed-entry receipt))
       (true? (:invoked receipt)) (true? (:predicate-observed-result receipt))
       (false? (:healthy? receipt)) (pos-int? (:failures receipt))
       (zero? (:errors receipt))))
