(ns wm-contract.replay-e6b-canonical-fixture
  (:require [clojure.edn :as edn]
            [futon2.aif.machine-enactment-correspondence :as e2b]
            [futon2.aif.machine-pre-enact-authorization :as e3])
  (:import (java.io PushbackReader StringReader)
           (java.nio ByteBuffer)
           (java.nio.charset CodingErrorAction StandardCharsets)
           (java.nio.file Files Path)
           (java.security MessageDigest)))

(def capture-root
  (Path/of "holes/labs/wm-contract/runs/row-22-e6b-canonical-fixture-2026-09-13/captured"
           (make-array String 0)))
(def output-root
  (Path/of "holes/labs/wm-contract/runs/row-22-e6b-canonical-replay-2026-09-13"
           (make-array String 0)))
(def source-pins
  {"src/futon2/aif/machine_pre_enact_authorization.clj"
   "60d0187c34f12c70376a4aabe714ac71f87c1d1d1d3685e6bbbf69d934469260"
   "src/futon2/aif/machine_enactment_correspondence.clj"
   "0d5745faa264463f34baf5b7f3c789cdadd846f9409b1739b27e7dd7f2545933"
   "src/futon2/aif/machine_portfolio_restriction.clj"
   "316ddd364e2deaea98172558148fd770c2d5d4a930e32fb8e4825f454b6c51ab"
   "src/futon2/aif/machine_budget_authority.clj"
   "13c6fb05c2a75ff5c673ed2df49e9d555db210d9269588396ea71ce97964253e"
   "src/futon2/aif/machine_budget_mapping.clj"
   "5072c34fa55db6683faeef38ac2b8026107c9ef0c9d7174a9a4026ab3ec2110f"
   "src/futon2/aif/hierarchical_budget_adapter.clj"
   "525bcb8aa4aad49c9cd02c22dfeabc7406aced8aacbb5731317e182eddb691e1"
   "src/futon2/aif/hierarchical_budget.clj"
   "a0581c1d7135324b1190795ba56dfc9fa834869474a297155678d88d71e5216b"
   "src/futon2/aif/r9_checker.clj"
   "b3469b8027c36e2db17943efabb6b26878c1dee3d71b9eda809e50d602094722"})

(defn sha256 [^bytes bs]
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (doto (MessageDigest/getInstance "SHA-256") (.update bs))))))
(defn strict-read [^bytes bs path]
  (let [decoder (doto (.newDecoder StandardCharsets/UTF_8)
                  (.onMalformedInput CodingErrorAction/REPORT)
                  (.onUnmappableCharacter CodingErrorAction/REPORT))
        rdr (PushbackReader. (StringReader. (str (.decode decoder (ByteBuffer/wrap bs)))))
        eof (Object.) value (edn/read {:eof eof} rdr) tail (edn/read {:eof eof} rdr)]
    (when (or (identical? eof value) (not (identical? eof tail)))
      (throw (ex-info "not exactly one EDN form" {:path (str path)})))
    value))
(defn pinned-read [path expected]
  (let [bs (Files/readAllBytes path) actual (sha256 bs)]
    (when-not (= expected actual)
      (throw (ex-info "pin mismatch" {:path (str path) :expected expected :actual actual})))
    (strict-read bs path)))
(defn retain! [name value]
  (let [path (.resolve output-root name)
        bs (.getBytes (pr-str value) StandardCharsets/UTF_8)]
    (Files/write path bs (make-array java.nio.file.OpenOption 0))
    {:path (str path) :sha256 (sha256 bs) :bytes (alength bs)}))
(defn refusal [f]
  (try (f) :did-not-refuse
       (catch clojure.lang.ExceptionInfo failure (:refusal (ex-data failure)))))

(let [manifest-path (.resolve capture-root "capture-manifest.edn")
      manifest (pinned-read manifest-path
                            "eca79640929ae7a7fb06fb12a2315d6172f11fc853489c9d0f4d7b7157ff119e")
      _ (doseq [[_ {:keys [path sha256]}] (:records manifest)]
          (pinned-read (Path/of path (make-array String 0)) sha256))
      _ (doseq [[path pin] source-pins]
          (let [actual (sha256 (Files/readAllBytes (Path/of path (make-array String 0))))]
            (when-not (= pin actual)
              (throw (ex-info "source declaration pin mismatch"
                              {:path path :expected pin :actual actual})))))
      canonical-pin (get-in manifest [:records :config/canonical :sha256])
      canonical (pinned-read (.resolve capture-root "config/canonical-config.edn") canonical-pin)
      e3-output (e3/verify-pre-enact (:e3 canonical))
      e2b-output (e2b/verify-correspondence (:e2b canonical))
      controls
      {:missing-e3-record
       (refusal #(e3/verify-pre-enact (update-in (:e3 canonical) [:evidence] dissoc :pending)))
       :changed-e2b-pin
       (refusal #(e2b/verify-correspondence
                  (assoc-in (:e2b canonical) [:witnesses :selection :sha256]
                            (apply str (repeat 64 "0")))))}
      retained {:e3 (retain! "e3-output.edn" e3-output)
                :e2b (retain! "e2b-output.edn" e2b-output)
                :controls (retain! "refusals.edn" controls)}]
  (when-not (= {:missing-e3-record :e3/evidence-set-incomplete
                :changed-e2b-pin :e2b/witness-pin-mismatch} controls)
    (throw (ex-info "negative control did not refuse" {:controls controls})))
  (prn {:status :canonical-isolated-replay-passed
        :manifest-sha256 "eca79640929ae7a7fb06fb12a2315d6172f11fc853489c9d0f4d7b7157ff119e"
        :e3/decision (:decision e3-output)
        :e2b/correspondence (:correspondence e2b-output)
        :controls controls :retained retained
        :authority :unauthenticated-test-data}))
