(ns checks.positive-proof-receipt
  (:require [babashka.process :as process]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(def repo-root "/home/joe/code")

(defn sha256-bytes [bs]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (.update digest bs)
    (format "%064x" (java.math.BigInteger. 1 (.digest digest)))))

(defn sha256-text [s] (sha256-bytes (.getBytes s "UTF-8")))
(defn sha256-file [path] (sha256-bytes (java.nio.file.Files/readAllBytes (.toPath (java.io.File. path)))))

(def declaration-start
  #"^(?:private\s+)?(?:noncomputable\s+)?(?:structure|inductive|abbrev|def|theorem|lemma)\s+")

(defn declaration-text [source declaration]
  (let [lines (vec (str/split-lines source))
        start-pattern (re-pattern
                       (str "^(?:private\\s+)?(?:noncomputable\\s+)?"
                            "(?:structure|inductive|abbrev|def|theorem|lemma)\\s+"
                            (java.util.regex.Pattern/quote declaration) "(?:\\s|$)"))
        boundary? #(or (boolean (re-find declaration-start %))
                       (boolean (re-find #"^(?:namespace|end)\s" %)))
        start (first (keep-indexed #(when (re-find start-pattern %2) %1) lines))]
    (when (nil? start)
      (throw (ex-info "declaration absent" {:declaration declaration})))
    (let [end (or (first (keep-indexed #(when (and (> %1 start) (boundary? %2)) %1) lines))
                  (count lines))]
      (str (str/join "\n" (subvec lines start end)) "\n"))))

(defn source-path [{:keys [repo path]}] (str repo-root "/" repo "/" path))

(defn live-source-basis [receipt]
  (mapv (fn [{:keys [repo path declarations]}]
          (let [source (slurp (source-path {:repo repo :path path}))]
            {:repo repo :path path
             :declarations
             (mapv (fn [{:keys [name]}]
                     {:name name :sha256 (sha256-text (declaration-text source name))})
                   declarations)}))
        (:source-basis receipt)))

(defn live-toolchain [receipt]
  (let [cwd (get-in receipt [:elaborator :cwd])
        version (process/shell {:dir cwd :out :string :err :string :continue true}
                               "lake" "env" "lean" "--version")]
    {:lean-version (str/trim (str (:out version) (:err version)))
     :lean-toolchain-sha256 (sha256-file (str cwd "/lean-toolchain"))
     :lake-manifest-sha256 (sha256-file (str cwd "/lake-manifest.json"))}))

(defn fixture-valid? [receipt]
  (let [fixture-spec (:fixture receipt)
        path (source-path fixture-spec)
        fixture (edn/read-string (slurp path))]
    (and (= (:sha256 fixture-spec) (sha256-file path))
         (= (:schema fixture-spec) (:schema fixture))
         (every? (fn [{:keys [fixture-path expected]}]
                   (= expected (get-in fixture fixture-path)))
                 (get-in receipt [:adapter :mappings])))))

(defn elaborate [receipt]
  (let [cwd (get-in receipt [:elaborator :cwd])
        theorem (:theorem receipt)
        module (:module receipt)
        tmp (java.io.File/createTempFile "positive-proof-receipt-" ".lean")]
    (try
      (spit tmp (str "import " module "\n#print axioms " theorem "\n"))
      (let [r (process/shell {:dir cwd :out :string :err :string :continue true}
                             "lake" "env" "lean" (.getAbsolutePath tmp))
            output (str (:out r) (:err r))
            axioms (if-let [[_ body] (re-find #"depends on axioms: \[([^]]*)\]" output)]
                     (if (str/blank? body) [] (mapv str/trim (str/split body #",")))
                     nil)]
        {:exit (:exit r) :axioms axioms})
      (finally (.delete tmp)))))

(defn validate
  ([receipt] (validate receipt {}))
  ([receipt source-overrides]
   (let [basis
         (mapv (fn [{:keys [repo path declarations]}]
                 (let [key [repo path]
                       source (or (get source-overrides key)
                                  (slurp (source-path {:repo repo :path path})))]
                   {:repo repo :path path
                    :declarations
                    (mapv (fn [{:keys [name]}]
                            {:name name :sha256 (sha256-text (declaration-text source name))})
                          declarations)}))
               (:source-basis receipt))
         source-ok? (= basis (:source-basis receipt))
         fixture-ok? (fixture-valid? receipt)
         toolchain-ok? (= (:toolchain receipt) (live-toolchain receipt))
         live (elaborate receipt)
         elaboration-ok? (= (:result receipt) live)
         failures (cond-> []
                    (not source-ok?) (conj :positive-source-drift)
                    (not fixture-ok?) (conj :fixture-or-adapter-drift)
                    (not toolchain-ok?) (conj :toolchain-drift)
                    (not elaboration-ok?) (conj :elaboration-or-axiom-drift))]
     {:pass? (empty? failures) :failures failures
      :source-basis-matches? source-ok? :fixture-adapter-matches? fixture-ok?
      :toolchain-matches? toolchain-ok? :elaboration-matches? elaboration-ok?})))

(defn basis-record [receipt]
  (assoc receipt
         :source-basis (live-source-basis receipt)
         :toolchain (live-toolchain receipt)
         :result (elaborate receipt)
         :fixture (update (:fixture receipt) :sha256
                          (fn [_] (sha256-file (source-path (:fixture receipt)))))))
