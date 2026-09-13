(require '[clojure.edn :as edn]
         '[futon2.aif.machine-slow-feedback-capture :as codec]
         '[futon2.aif.machine-slow-feedback-capture-test :as fixture]
         '[futon2.aif.machine-slow-feedback-store-v2 :as store])
(import '(java.util Base64))
(let [[s c] (#'fixture/setup-capture true)]
  (try
    (let [old (last (:chain-digests c))
          tx (edn/read-string (String. ^bytes (get (:transaction-objects c) old) "UTF-8"))
          bad (assoc-in tx [:prior :generation] 999)
          bs (.getBytes (pr-str bad) "UTF-8") d (#'codec/sha256 bs)
          index (assoc-in (:application-universe c) [0 :transaction-sha256] d)
          head (edn/read-string (String. (.decode (Base64/getDecoder) ^String (get-in c [:head-object :bytes/base64])) "UTF-8"))
          head' (assoc head :transaction-sha256 d :application-index index)
          hb (.getBytes (pr-str head') "UTF-8") hd (#'codec/sha256 hb)
          c' (-> c (assoc :head-digest hd :head-object {:bytes/base64 (.encodeToString (Base64/getEncoder) hb) :source-sha256 hd}
                         :application-universe index :chain-digests (assoc (:chain-digests c) 1 d))
                 (update :transaction-objects #(assoc (dissoc % old) d bs)))
          artifact (codec/construct c')
          readback (codec/readback {:bytes/base64 (:bytes/base64 artifact) :expected-sha256 (:sha256 artifact)})]
      (prn {:control :coherent-prior-generation :stored-prior-generation 999
            :actual-parent-generation 0 :readback-schema (:schema readback)
            :authority/status (:authority/status readback)})
      (assert (= :wm/e6b-complete-capture-readback-v1 (:schema readback))))
    (finally (store/release! s))))
