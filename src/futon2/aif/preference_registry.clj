(ns futon2.aif.preference-registry
  "Production preference authorization registry loader. Pure: this namespace
   validates configuration but does not install it in the serving path."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.factorized-preference-sut :as preference]))

(def default-resource "wm/preference-authorizations.edn")
(def registry-schema :wm/preference-authorizations-v1)

(defn- fail! [kind detail cause]
  (throw (ex-info "preference registry refused"
                  {:kind kind :detail detail}
                  cause)))

(defn- source [path]
  (if path
    (let [file (io/file path)]
      (when (.isFile file) file))
    (io/resource default-resource)))

(defn- read-registry [path]
  (let [input (source path)]
    (when-not input
      (fail! :preference-registry-unconfigured
             {:path (or path default-resource)} nil))
    (try
      (edn/read-string (slurp input))
      (catch Exception e
        (fail! :preference-registry-invalid
               {:path (or path default-resource) :reason :malformed-edn} e)))))

(defn- normalized-carrier [registry]
  (let [calibrations (:calibration-authorizations registry)
        factorizations (:factorization-authorizations registry)]
    (when-not (and (= registry-schema (:schema registry))
                   (vector? calibrations) (= 1 (count calibrations))
                   (vector? factorizations) (= 1 (count factorizations))
                   (pos-int? (:receipt (first calibrations)))
                   (pos-int? (:receipt (first factorizations))))
      (fail! :preference-registry-invalid
             {:reason :registry-shape-invalid} nil))
    (let [calibration-entry
          (assoc (first calibrations)
                 :implementation-pin
                 (preference/implementation-pin
                  preference/calibration-implementation))
          factorization-entry
          (assoc (first factorizations)
                 :implementation-pin
                 (preference/implementation-pin
                  preference/factorization-implementation))
          calibration (:calibration calibration-entry)
          certificate
          {:calibration calibration
           :selector-authority (:selector-authority registry)
           :factorization-identity (:identity factorization-entry)
           :factorization-version (:version factorization-entry)
           :factorization-authority (:authority factorization-entry)
           :calibration-authorization (:receipt calibration-entry)
           :factorization-authorization (:receipt factorization-entry)}
          normalized-calibrations [calibration-entry]
          normalized-factorizations [factorization-entry]]
      {:schema preference/carrier-schema
       :certificate certificate
       :registries {:calibration normalized-calibrations
                    :factorization normalized-factorizations}
       ;; The controlled producer still consumes these aliases. They are the
       ;; same persistent vectors as the exporter-facing normalized shape.
       :calibration-authorizations normalized-calibrations
       :factorization-authorizations normalized-factorizations})))

(defn terminal-distribution!
  "Pure authorization check: the carried kernel must equal the kernel derived
   from the authorized calibration. No registry IO and no legacy fallback."
  [carrier]
  (preference/validate-certificate! carrier)
  (let [expected (:probabilities
                  (preference/kernel (get-in carrier [:certificate :calibration])))]
    (when-not (= expected (:terminal-distribution carrier))
      (throw (ex-info "authorized kernel mismatch"
                      {:kind :computed-kernel-not-consumed})))
    expected))

(defn load!
  "Load and validate the shipped registry. PATH, when supplied, is a filesystem
   override used by hermetic tests. Returns one normalized carrier shape."
  ([] (load! nil))
  ([path]
   (try
     (let [carrier (-> (read-registry path)
                       normalized-carrier
                       preference/validate-certificate!)]
       (assoc carrier :terminal-distribution
              (:probabilities (preference/kernel
                               (get-in carrier [:certificate :calibration])))))
     (catch clojure.lang.ExceptionInfo e
       (if (contains? #{:preference-registry-unconfigured
                        :preference-registry-invalid}
                      (:kind (ex-data e)))
         (throw e)
         (throw e)))
     (catch Exception e
       (fail! :preference-registry-invalid
              {:path (or path default-resource) :reason :load-failed} e)))))
