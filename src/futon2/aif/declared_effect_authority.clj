(ns futon2.aif.declared-effect-authority
  "Production binding adapter. Authority is always C2 reconstruction with
  production ports, from declaration bytes retained before dispatch. No
  caller-supplied verifier/observer/resolver is accepted. This module does
  not wire the runner or claim an atomic occurrence/binding transaction."
  (:require [clojure.java.io :as io]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.declared-effect-record :as record]
            [futon2.aif.occurrence-declared-effect-binding :as binding])
  (:import [java.nio.file Files StandardOpenOption]
           [java.nio.file.attribute FileAttribute]
           [java.security MessageDigest]
           [java.math BigInteger]))

(load-identity/register! *ns* *file*)

(defn- sha256-bytes [bytes]
  (format "%064x" (BigInteger. 1 (.digest (MessageDigest/getInstance "SHA-256") bytes))))

(defn retain-declaration!
  "Copy exact pinned source bytes to a new digest-named file. Refuses a
  changed source or pre-existing destination; never overwrites evidence."
  [effect-record directory]
  (let [{:keys [path sha256] :as source} (:declaration effect-record)
        bytes (Files/readAllBytes (.toPath (io/file path)))
        actual (sha256-bytes bytes)]
    (when-not (and (= :pinned (:status source)) (= sha256 actual))
      (throw (ex-info "Declaration bytes changed" {:reason :declaration-bytes-changed})))
    (let [dir (.toPath (io/file directory))
          _ (Files/createDirectories dir (make-array FileAttribute 0))
          dest (.resolve dir (str "declaration-" actual ".edn"))]
      (Files/write dest bytes (into-array StandardOpenOption
                                         [StandardOpenOption/CREATE_NEW StandardOpenOption/WRITE]))
      {:path (str dest) :sha256 actual})))

(defn reconstruct-retained
  "Reconstruct the current record using its full base commit and retained
  declaration bytes. Preserve original declaration path as provenance."
  [effect-record declaration-pin]
  (try
    (if-not (and (= #{:path :sha256} (set (keys declaration-pin)))
                 (= (:sha256 declaration-pin) (get-in effect-record [:declaration :sha256])))
      {:verified false :reason :declaration-pin-mismatch}
      (record/reconstruct
       {:target (:target effect-record)
        :base-revision (:base-revision effect-record)
        :declaration {:path (get-in effect-record [:declaration :path])
                      :storage-path (:path declaration-pin)
                      :expected-sha256 (:sha256 declaration-pin)}}
       effect-record))
    (catch Exception _ {:verified false :reason :retained-declaration-unreadable})))

(defn- verifier [pin]
  (fn [current]
    (let [result (reconstruct-retained current pin)]
      (if (:verified result)
        {:verified true :digest (:rebuilt result)}
        result))))

(defn- inputs! [inputs expected]
  (when-not (and (map? inputs) (= expected (set (keys inputs))))
    (throw (ex-info "Unexpected authority adapter inputs"
                    {:reason :authority-input-invalid}))))

(defn build-binding
  [inputs]
  (inputs! inputs #{:occurrence :selected-mission :record :declaration-pin :recorded-at :bound-at})
  (binding/build-binding
   (-> inputs (dissoc :declaration-pin)
       (assoc :record-verifier (verifier (:declaration-pin inputs))))))

(defn verify-binding
  [inputs]
  (try
    (inputs! inputs #{:occurrence :selected-mission :record :declaration-pin :binding})
    (binding/verify-binding
     (-> inputs (dissoc :declaration-pin)
         (assoc :record-verifier (verifier (:declaration-pin inputs)))))
    (catch Exception e {:status :refused :reason (or (:reason (ex-data e)) :authority-input-invalid)})))
