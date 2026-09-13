(ns futon2.aif.interoceptive-activation
  "Independent host evidence required before coordinated production capture.

  The fixed receipt and lock are provisioned by the operator outside the JVM.
  A caller-supplied map, root label, or test receipt cannot activate production."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.interoceptive-store-lock :as store-lock])
  (:import [java.nio ByteBuffer]
           [java.nio.charset CodingErrorAction StandardCharsets]
           [java.nio.file Files LinkOption Paths]
           [java.nio.file.attribute PosixFilePermission]
           [java.security MessageDigest]))

(def production-receipt-path "/etc/futon2/wm-interoceptive-participation.edn")
(def deployment-lease-path "/run/futon2/wm-interoceptive-deployment.lease")
(def max-receipt-lifetime-ms 300000)
(def required-writer-census-sha256
  "039d5319ade70150844c1e27f3d63cb01cb3a3f732ce29c11395359bc208eb51")
(def required-writers
  #{:tripwire/write-trip-report
    :repair/record-finding
    :repair/record-implementation
    :repair/record-verification
    :repair/record-resolution})
(def required-control-surfaces
  #{:systemd-process-start-restart
    :drawbridge-proof-eval-reload
    :dev-admin-load-file
    :direct-in-jvm-require-reload})
(def production-controller-authority
  {:systemd-process-start-restart
   {:artifact/path "/home/joe/code/futon3c/scripts/restart-fdev-detached.sh"
    :artifact/sha256 "9c7f2334f182444c37383f41317133781a441541f097d8d801cc502451ac0dd6"
    :status :not-lease-aware}
   :drawbridge-proof-eval-reload
   {:artifact/path "/home/joe/code/futon3c/scripts/proof-eval.sh"
    :artifact/sha256 "fde514ee2b623a46262cc23d940587b6e335f75f9e870e7fdb0e36d817a59506"
    :status :not-lease-aware}
   :dev-admin-load-file
   {:artifact/path "/home/joe/code/futon3c/admin/futon3c/admin.clj"
    :artifact/sha256 "991058114751850fbad337f075eed66c95c1b4501e5a773c649c253a14111f39"
    :status :not-lease-aware}
   :direct-in-jvm-require-reload
   {:artifact/path nil :artifact/sha256 nil :status :uncontrolled}})

;; Filled from committed source bytes; activation receipts must match all pins.
(def required-source-pins
  {"/home/joe/code/futon2/src/futon2/aif/interoceptive_store_lock.clj"
   "5f60c4c00c12fef5a00c56b352c32a0d14d727dacf71a57d9fbaeb72d0ae354a"
   "/home/joe/code/futon2/src/futon2/aif/interoceptive_manifest.clj"
   "8f469c85c7e30004b593fd8b40d38fe7b30d9c6b37d08542be7d99996c633d3d"
   "/home/joe/code/futon2/src/futon2/aif/tripwire.clj"
   "a75b2a571d76fa93486ff8807dc4fdc93a2f6ca125042f0d3087cbdcb933aeae"
   "/home/joe/code/futon2/src/futon2/aif/repair_obligation.clj"
   "f61ede50822955695d5510248f0592883ed6f83d74be08a23ba37f649e33254a"})

(defonce ^:private production-capability (Object.))
(def ^:dynamic *after-receipt-read-hook* (fn [] nil))

(declare read-stable-bytes! validate-participation)

(defn- refuse! [reason data]
  (throw (ex-info "Interoceptive activation refused" (assoc data :refusal reason))))

(defn- sha256-bytes [bytes]
  (apply str (map #(format "%02x" (bit-and 0xff %))
                  (.digest (MessageDigest/getInstance "SHA-256") bytes))))

(defn process-census-edn [processes] (pr-str processes))

(defn process-census-sha256 [census-edn]
  (sha256-bytes (.getBytes census-edn StandardCharsets/UTF_8)))

(defn- strict-edn [bytes path]
  (let [text (try
               (str (.decode (doto (.newDecoder StandardCharsets/UTF_8)
                               (.onMalformedInput CodingErrorAction/REPORT)
                               (.onUnmappableCharacter CodingErrorAction/REPORT))
                             (ByteBuffer/wrap bytes)))
               (catch Throwable e
                 (refuse! :interoceptive/activation-non-utf8
                          {:path path :cause (.getMessage e)})))]
    (with-open [r (java.io.PushbackReader. (java.io.StringReader. text))]
      (try
        (let [value (edn/read {:eof ::empty} r)]
          (when (or (= ::empty value) (not= ::end (edn/read {:eof ::end} r)))
            (refuse! :interoceptive/activation-not-one-form {:path path}))
          value)
        (catch clojure.lang.ExceptionInfo e (throw e))
        (catch Throwable e
          (refuse! :interoceptive/activation-malformed
                   {:path path :cause (.getMessage e)}))))))

(defn- real-source-pins []
  (into {}
        (map (fn [[path _]]
               (let [file (io/file path)]
                 (when-not (.isFile file)
                   (refuse! :interoceptive/activation-source-unavailable {:path path}))
                 [path (sha256-bytes (Files/readAllBytes (.toPath file)))])))
        required-source-pins))

(defn- secure-receipt! [path required-owner]
  (let [absolute (.toAbsolutePath (.normalize path))]
    (loop [p (.getRoot absolute) names (iterator-seq (.iterator absolute))]
      (when-let [name (first names)]
        (let [candidate (.resolve p name)]
          (when (Files/isSymbolicLink candidate)
            (refuse! :interoceptive/activation-receipt-untrusted
                     {:path (str absolute) :symlink (str candidate)}))
          (recur candidate (next names)))))
    (let [parent (.getParent absolute)
          owner (str (Files/getOwner absolute
                                     (into-array LinkOption [LinkOption/NOFOLLOW_LINKS])))
          parent-owner (str (Files/getOwner parent
                                            (into-array LinkOption [LinkOption/NOFOLLOW_LINKS])))
          permissions (Files/getPosixFilePermissions parent (make-array LinkOption 0))
          file-permissions (Files/getPosixFilePermissions absolute
                                                           (make-array LinkOption 0))]
      (when-not (and (= required-owner owner parent-owner)
                     (not-any? (set permissions)
                               [PosixFilePermission/GROUP_WRITE
                                PosixFilePermission/OTHERS_WRITE])
                     (not-any? (set file-permissions)
                               [PosixFilePermission/GROUP_WRITE
                                PosixFilePermission/OTHERS_WRITE]))
        (refuse! :interoceptive/activation-receipt-untrusted
                 {:path (str absolute) :owner owner :parent-owner parent-owner
                  :parent-permissions (mapv str permissions)
                  :file-permissions (mapv str file-permissions)})))
    absolute))

(defn read-test-participation!
  "Exercise the real strict, stable-byte receipt reader on an isolated file.
  The result is always test authority, regardless of record contents."
  [path opts]
  (let [p (Paths/get path (make-array String 0))
        owner (str (Files/getOwner p (into-array LinkOption [LinkOption/NOFOLLOW_LINKS])))]
    (secure-receipt! p owner)
    (let [bytes (read-stable-bytes! p)]
      (validate-participation
       (assoc (strict-edn bytes path) :receipt-sha256 (sha256-bytes bytes)) opts))))

(defn- read-stable-bytes! [path]
  (try
    (let [before [(Files/getAttribute path "basic:fileKey"
                                      (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))
                  (Files/getAttribute path "basic:size"
                                      (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))
                  (Files/getAttribute path "basic:lastModifiedTime"
                                      (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))]
          bytes (Files/readAllBytes path)
          _ (*after-receipt-read-hook*)
          after [(Files/getAttribute path "basic:fileKey"
                                     (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))
                 (Files/getAttribute path "basic:size"
                                     (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))
                 (Files/getAttribute path "basic:lastModifiedTime"
                                     (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))]]
      (when-not (= before after)
        (refuse! :interoceptive/activation-receipt-changed
                 {:path (str path) :before before :after after}))
      bytes)
    (catch clojure.lang.ExceptionInfo e (throw e))
    (catch Throwable e
      (refuse! :interoceptive/activation-receipt-io
               {:path (str path) :cause (.getMessage e)}))))

(defn validate-participation
  "Validate an already independently authenticated record. This pure layer
  cannot authenticate its caller; only resolve-production-participation! may
  authorize production."
  [record {:keys [now-ms source-pins lock-probe process-probe boot-id capability
                  controller-authority controller-probe]
           :or {now-ms (System/currentTimeMillis)}}]
  (when-not (= :wm/interoceptive-writer-participation-v1 (:schema record))
    (refuse! :interoceptive/activation-schema {:schema (:schema record)}))
  (when-not (= required-writers (set (:writer-entrypoints record)))
    (refuse! :interoceptive/activation-writer-coverage
             {:required required-writers :observed (set (:writer-entrypoints record))}))
  (when-not (= required-source-pins (:source-pins record) source-pins)
    (refuse! :interoceptive/activation-source-mismatch
             {:required required-source-pins :receipt (:source-pins record)
              :observed source-pins}))
  (when-not (and (integer? (:observed-at-ms record))
                 (integer? (:valid-until-ms record))
                 (<= (- (:valid-until-ms record) (:observed-at-ms record))
                     max-receipt-lifetime-ms)
                 (<= (:observed-at-ms record) now-ms (:valid-until-ms record)))
    (refuse! :interoceptive/activation-stale
             {:now-ms now-ms :observed-at-ms (:observed-at-ms record)
              :valid-until-ms (:valid-until-ms record)}))
  (when-not (and (true? (get-in record [:host :census-complete?]))
                 (string? (get-in record [:host :boot-id]))
                 (= boot-id (get-in record [:host :boot-id]))
                 (= required-writer-census-sha256
                    (get-in record [:host :writer-census-sha256]))
                 (re-matches #"[0-9a-f]{64}"
                             (or (get-in record [:host :process-census-sha256]) "")))
    (refuse! :interoceptive/activation-host-census-invalid {:host (:host record)}))
  (let [census-edn (get-in record [:host :process-census-edn])
        parsed (when (string? census-edn)
                 (strict-edn (.getBytes census-edn StandardCharsets/UTF_8)
                             :embedded-process-census))
        measured (when (string? census-edn) (process-census-sha256 census-edn))]
    (when-not (and (= parsed (:processes record))
                   (= measured (get-in record [:host :process-census-sha256])))
      (refuse! :interoceptive/activation-process-census-digest-mismatch
               {:declared (get-in record [:host :process-census-sha256])
                :measured measured})))
  (let [controls (into {} (map (juxt :surface #(dissoc % :surface)))
                       (get-in record [:lease :controls]))]
    (when-not (= required-control-surfaces (set (keys controller-authority))
                 (set (keys controls)))
      (refuse! :interoceptive/activation-lease-controls-unverified
               {:required required-control-surfaces :controls controls
                :authority controller-authority}))
    (doseq [[surface expected] controller-authority]
      (when-let [path (:artifact/path expected)]
        (when-not (= (:artifact/sha256 expected) (controller-probe path))
          (refuse! :interoceptive/activation-controller-source-mismatch
                   {:surface surface :expected (:artifact/sha256 expected)})))
      (when-not (= expected (get controls surface))
        (refuse! :interoceptive/activation-controller-claim-mismatch
                 {:surface surface :expected expected :claimed (get controls surface)})))
    (let [unsupported (into {} (remove (fn [[_ x]] (= :lease-enforced (:status x))))
                            controller-authority)]
      (when (seq unsupported)
        (refuse! :interoceptive/activation-controller-unavailable
                 {:controllers unsupported}))))
  (when-not (and (= deployment-lease-path (get-in record [:lease :path]))
                 (= :host-launch-reload-lock-v1 (get-in record [:lease :protocol]))
                 (string? (get-in record [:lease :generation]))
                 (= :enforced (get-in record [:lease :status])))
    (refuse! :interoceptive/activation-lease-unavailable {:lease (:lease record)}))
  (let [processes (:processes record)
        ids (mapv :process/id processes)
        nonparticipants (filterv #(not= :participating (:coordination/status %)) processes)
        covered (into #{} (mapcat :writer-entrypoints) processes)]
    (when-not (and (vector? processes) (seq processes)
                   (= (count ids) (count (distinct ids))))
      (refuse! :interoceptive/activation-process-census-invalid {:process/ids ids}))
    (when (seq nonparticipants)
      (refuse! :interoceptive/activation-nonparticipating-writer
               {:processes (mapv :process/id nonparticipants)}))
    (doseq [process processes]
      (when-not (and (= required-source-pins (:loaded-source-pins process))
                     (string? (:deployment/id process))
                     (integer? (:loaded-at-ms process))
                     (<= (:loaded-at-ms process) (:observed-at-ms record)))
        (refuse! :interoceptive/activation-process-source-unverified
                 {:process/id (:process/id process)})))
    (when-not (= required-writers covered)
      (refuse! :interoceptive/activation-process-writer-coverage
               {:required required-writers :covered covered}))
    (doseq [process processes]
      (let [observable (select-keys process
                                    [:process/id :pid :start-ticks :exe
                                     :cmdline-sha256])]
        (when-not (= observable (process-probe process))
          (refuse! :interoceptive/activation-process-changed
                   {:process/id (:process/id process)})))))
  (let [declared (:lock record)
        observed (lock-probe declared)]
    (when-not (and (= store-lock/default-lock-path (:path declared))
                   (= declared observed)
                   (= "root" (:parent-owner observed))
                   (false? (:parent-writable-by-service? observed))
                   (string? (:file-key observed)))
      (refuse! :interoceptive/activation-lock-mismatch
               {:declared declared :observed observed})))
  (let [declared (:lease-lock record)
        observed (lock-probe declared)]
    (when-not (and (= deployment-lease-path (:path declared)) (= declared observed)
                   (= "root" (:parent-owner observed))
                   (false? (:parent-writable-by-service? observed))
                   (string? (:file-key observed)))
      (refuse! :interoceptive/activation-lease-lock-mismatch
               {:declared declared :observed observed})))
  {:schema :wm/interoceptive-writer-participation-admission-v1
   :authority-class (if (identical? capability production-capability)
                      :production :test)
   :receipt-sha256 (:receipt-sha256 record)
   :lock (:lock record)
   :lease-lock (:lease-lock record)
   :lease (:lease record)
   :valid-until-ms (:valid-until-ms record)})

(defn- host-lock-probe [declared]
  (let [path (Paths/get (:path declared) (make-array String 0))
        parent (.getParent path)]
    (when-not (and (Files/isRegularFile path (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))
                   (not (Files/isSymbolicLink path)))
      (refuse! :interoceptive/activation-lock-unavailable {:path (str path)}))
    {:path (str path)
     :file-key (str (Files/getAttribute path "basic:fileKey"
                                        (into-array LinkOption [LinkOption/NOFOLLOW_LINKS])))
     :owner (str (Files/getOwner path (into-array LinkOption [LinkOption/NOFOLLOW_LINKS])))
     :parent-owner (str (Files/getOwner parent (into-array LinkOption [LinkOption/NOFOLLOW_LINKS])))
     :parent-writable-by-service?
     (boolean (some #{PosixFilePermission/GROUP_WRITE PosixFilePermission/OTHERS_WRITE}
                    (Files/getPosixFilePermissions parent (make-array LinkOption 0))))}))

(defn- host-process-probe [process]
  (let [pid (:pid process)
        path (Paths/get (str "/proc/" pid) (make-array String 0))]
    (when-not (Files/isDirectory path (make-array LinkOption 0))
      (refuse! :interoceptive/activation-process-missing {:process/id (:process/id process)}))
    (try
      (let [stat (slurp (str path "/stat"))
            close (.lastIndexOf stat ")")
            fields (str/split (subs stat (+ close 2)) #" ")
            start-ticks (nth fields 19)
            exe (str (.toRealPath (Paths/get (str path "/exe") (make-array String 0))
                                  (make-array LinkOption 0)))
            cmdline (Files/readAllBytes (Paths/get (str path "/cmdline")
                                                   (make-array String 0)))]
        {:process/id (:process/id process) :pid pid :start-ticks start-ticks
         :exe exe :cmdline-sha256 (sha256-bytes cmdline)})
      (catch Throwable e
        (refuse! :interoceptive/activation-process-probe-failed
                 {:process/id (:process/id process) :cause (.getMessage e)})))))

(defn- host-controller-probe [path]
  (let [p (Paths/get path (make-array String 0))]
    (when-not (Files/isRegularFile p (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))
      (refuse! :interoceptive/activation-controller-unavailable {:path path}))
    (sha256-bytes (read-stable-bytes! p))))

(defn production-controller-status!
  "Resolve the independently configured production controller bytes. Current
  artifacts are pinned but not lease-aware, so a bit-identical audit still
  returns the typed unsupported-surface refusal."
  []
  (doseq [[surface {:keys [artifact/path artifact/sha256]}]
          production-controller-authority
          :when path]
    (let [measured (host-controller-probe path)]
      (when-not (= sha256 measured)
        (refuse! :interoceptive/activation-controller-source-mismatch
                 {:surface surface :path path :expected sha256 :measured measured}))))
  (let [unsupported (into {} (remove (fn [[_ x]] (= :lease-enforced (:status x))))
                          production-controller-authority)]
    (if (seq unsupported)
      (refuse! :interoceptive/activation-controller-unavailable
               {:controllers unsupported})
      {:status :lease-enforced :controllers production-controller-authority})))

(defn resolve-production-participation!
  "Read only the fixed root-owned host receipt. Missing or unauthenticated
  host evidence refuses; there is no caller-supplied production mode."
  []
  (let [path (Paths/get production-receipt-path (make-array String 0))]
    (when-not (and (Files/isRegularFile path (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))
                   (not (Files/isSymbolicLink path)))
      (refuse! :interoceptive/activation-receipt-unavailable {:path production-receipt-path}))
    (secure-receipt! path "root")
    (let [bytes (read-stable-bytes! path)
          record (assoc (strict-edn bytes production-receipt-path)
                        :receipt-sha256 (sha256-bytes bytes))]
      (validate-participation record
                              {:capability production-capability
                               :source-pins (real-source-pins)
                               :boot-id (str/trim (slurp "/proc/sys/kernel/random/boot_id"))
                               :controller-authority production-controller-authority
                               :controller-probe host-controller-probe
                               :lock-probe host-lock-probe
                               :process-probe host-process-probe}))))

(defn revalidate-production-participation!
  "Re-read every host/process/lock input while the deployment lease and store
  lock are held. Refuse if the receipt generation, bytes, expiry, process or
  either acquired lock identity changed."
  [admission]
  (when-not (= :production (:authority-class admission))
    (refuse! :interoceptive/activation-test-authority {:authority admission}))
  (let [current (resolve-production-participation!)]
    (when-not (= (select-keys admission [:receipt-sha256 :lock :lease-lock :lease])
                 (select-keys current [:receipt-sha256 :lock :lease-lock :lease]))
      (refuse! :interoceptive/activation-boundary-changed
               {:before (select-keys admission [:receipt-sha256 :lock :lease-lock :lease])
                :after (select-keys current [:receipt-sha256 :lock :lease-lock :lease])}))
    current))

(defn with-production-participation
  "Hold the independently provisioned launch/reload lease for resolution and
  the caller's entire physical capture."
  [f]
  (try
    (store-lock/with-existing-lock-at
     deployment-lease-path
     (fn []
       (let [admission (resolve-production-participation!)
             result (f admission)]
         (revalidate-production-participation! admission)
         result)))
    (catch clojure.lang.ExceptionInfo e
      (if (#{:interoceptive/lock-path-refused :interoceptive/lock-io-failure}
            (:refusal (ex-data e)))
        (refuse! :interoceptive/activation-lease-unavailable
                 {:path deployment-lease-path :cause (:refusal (ex-data e))})
        (throw e)))))
