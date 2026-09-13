(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[futon2.aif.machine-enactment-correspondence-test :as e2bt]
         '[futon2.aif.machine-pre-enact-authorization-test :as e3t])
(import '(java.nio.charset StandardCharsets)
        '(java.nio.file Files OpenOption Path)
        '(java.security MessageDigest))

(def output-root
  (Path/of "holes/labs/wm-contract/runs/row-22-e6b-canonical-fixture-2026-09-13/captured"
           (make-array String 0)))
(defn sha256 [^bytes bs]
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (doto (MessageDigest/getInstance "SHA-256") (.update bs))))))
(defn form-bytes [x] (.getBytes (pr-str x) StandardCharsets/UTF_8))
(defn retain! [relative x]
  (let [path (.resolve output-root relative) bs (form-bytes x)]
    (Files/createDirectories (.getParent path)
                             (make-array java.nio.file.attribute.FileAttribute 0))
    (Files/write path bs (make-array OpenOption 0))
    {:path (str (.resolve (Path/of "holes/labs/wm-contract/runs/row-22-e6b-canonical-fixture-2026-09-13/captured"
                                  (make-array String 0)) relative))
     :sha256 (sha256 bs) :value-sha256 (sha256 (form-bytes (edn/read-string (String. bs "UTF-8"))))
     :bytes (alength bs)}))

(let [e3-root (str (.resolve output-root "e3"))
      e3-records {:pending e3t/pending :verdict e3t/verdict :review e3t/review}
      e3-pins (into {} (for [[role record] e3-records
                             :let [retained (retain! (str "e3/" (name role) ".edn") record)]]
                         [role {:relative-path (str (name role) ".edn")
                                :sha256 (:sha256 retained)}]))
      e1-config e3t/e1-config
      e2b-config (#'e2bt/config)
      e3-config {:mode :isolated-test :evidence-root e3-root
                 :e2a-resolver e1-config :evidence e3-pins}
      canonical-config {:e3 e3-config :e2b e2b-config}
      retained {:e3/pending (retain! "e3/pending.edn" e3t/pending)
                :e3/verdict (retain! "e3/verdict.edn" e3t/verdict)
                :e3/review (retain! "e3/review.edn" e3t/review)
                :r9/input (retain! "config/r9-input.edn" e3t/r9-input)
                :config/e1 (retain! "config/e1-config.edn" e1-config)
                :config/e2b (retain! "config/e2b-config.edn" e2b-config)
                :config/e3 (retain! "config/e3-config.edn" e3-config)
                :config/canonical (retain! "config/canonical-config.edn" canonical-config)}
      manifest {:schema :wm/e6b-isolated-canonical-fixture-capture-v1
                :scope :isolated-test
                :ownership {:kind :test-source-owned
                            :source "test/futon2/aif/machine_pre_enact_authorization_test.clj"
                            :source-sha256 "50558cf363717c53cfb818a6db9d77814a8beb732e41549a2c66f478f7b45a12"}
                :authority {:authenticated-external? false
                            :production? false
                            :anchor :synthetic-unauthenticated-test-literal
                            :commission-jobs :synthetic-unauthenticated-test-data}
                :records retained
                :transitive-inventory
                "holes/labs/wm-contract/e6b-canonical-replay-closure-2026-09-13.edn"
                :missing [:authenticated-r9-anchor :real-commission-preimage
                          :independent-job-trace-authority :production-config-owner
                          :installed-code-identity :external-completeness]}
      manifest-pin (retain! "capture-manifest.edn" manifest)]
  (prn {:status :captured-isolated-fixture
        :records (count retained)
        :manifest manifest-pin
        :authority :unauthenticated-test-data}))
