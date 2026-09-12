(ns witnesses.node-witness
  "Pinned evidence admission. This validates bindings, never promotes node rungs."
  (:require [clojure.edn :as edn] [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.security MessageDigest] [java.io PushbackReader StringReader]))

(def maximum-claims
  {:lean-witness :exact-formal-proposition
   :commissioning :named-induced-behavior
   :replay-pin :asserted-captured-input-behavior})
(def identity-keys [:id :node :equation :quantity :declaration :kind :artifact
                    :subject-artifact :claim :polarity :scope :as-of])
(defn refuse! [id cause detail]
  (throw (ex-info (name cause) {:claim id :error cause :detail detail})))
(defn need! [ok id cause detail] (when-not ok (refuse! id cause detail)))
(defn sha256 [bytes]
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (MessageDigest/getInstance "SHA-256") bytes))))
(defn read-edn [text]
  (with-open [r (PushbackReader. (StringReader. text))]
    (let [x (edn/read {:eof ::eof} r)]
      (when (or (= ::eof x) (not= ::eof (edn/read {:eof ::eof} r)))
        (throw (ex-info "Expected exactly one EDN form" {:error :invalid-edn})))
      x)))
(defn context [roots] {:roots roots :reads (atom (sorted-map))})
(defn read-path! [ctx repo path id]
  (let [root (get (:roots ctx) repo)]
    (need! (and root (string? path) (not (str/blank? path))
                (not (.isAbsolute (io/file path)))) id :node-witness-path-invalid path)
    (let [r (.toPath (.getCanonicalFile (io/file root)))
          p (.toPath (.getCanonicalFile (io/file root path)))]
      (need! (and (.startsWith p r) (not= p r)
                  (not (some #{".."} (str/split path #"[/\\]"))))
             id :node-witness-path-invalid path)
      (or (get @(:reads ctx) (str p))
          (do
            (need! (.isFile (.toFile p)) id :node-witness-artifact-missing path)
            (let [bs (java.nio.file.Files/readAllBytes p)
                  v {:repo repo :path path :sha256 (sha256 bs)
                     :text (String. bs "UTF-8")}]
              (swap! (:reads ctx) assoc (str p) v) v))))))
(defn resolve! [ctx loc id]
  (need! (and (map? loc) (string? (:sha256 loc))
              (re-matches #"[a-f0-9]{64}" (:sha256 loc)))
         id :node-witness-pin-missing loc)
  (let [r (read-path! ctx (:repo loc) (:path loc) id)]
    (need! (= (:sha256 loc) (:sha256 r)) id :node-witness-pin-mismatch
           {:locator loc :actual (:sha256 r)})
    (when-let [[a b :as lines] (:lines loc)]
      (need! (and (= 2 (count lines)) (integer? a) (integer? b)
                  (<= 1 a b (count (str/split-lines (:text r)))))
             id :node-witness-selector-invalid lines))
    r))
(defn record! [ctx loc id]
  (let [r (resolve! ctx loc id)
        x (read-edn (:text r))
        selector (:selector loc)]
    (need! (and (vector? selector) (seq selector)) id :node-witness-selector-invalid loc)
    (let [v (get-in x selector ::missing)]
      (need! (not= v ::missing) id :node-witness-selector-missing loc) v)))
(defn dependencies! [ctx locs id]
  (need! (and (vector? locs) (seq locs)) id :node-witness-verification-missing :dependencies)
  (doseq [loc locs] (resolve! ctx loc id)))
(defn subject [w] (select-keys w identity-keys))
(defn executed! [r id]
  (need! (and (true? (:executed? r)) (= 0 (:exit r)) (= :passed (:result r))
              (string? (:command r)) (not (str/blank? (:command r))))
         id :node-witness-verification-missing :successful-executed-receipt))
(defn receipt! [ctx w which]
  (let [id (:id w) wrapper (get w which)]
    (need! (and (= :verified (:status wrapper)) (:receipt wrapper))
           id (if (= which :review) :node-witness-review-missing
                  :node-witness-verification-missing) which)
    (let [r (record! ctx (:receipt wrapper) id)]
      (need! (= (subject w) (:subject r)) id :node-witness-subject-mismatch which)
      r)))
(defn declaration! [ctx loc census id]
  ;; Qualified declarations come from the pinned checker census, not source grep.
  (resolve! ctx loc id)
  (let [d (get-in census [:declarations (:declaration loc)])]
    (need! (and (string? (:declaration loc)) d
                (= (select-keys loc [:repo :path :sha256]) (:source d))
                (contains? #{:def :theorem :abbrev :structure :inductive} (:kind d)))
           id :node-witness-declaration-missing (:declaration loc))
    d))
(defn verify-kind! [ctx w r]
  (let [id (:id w) artifact (:artifact w)]
    (executed! r id)
    (dependencies! ctx (:dependencies r) id)
    (resolve! ctx (:transcript r) id)
    (need! (and (some #{(:subject-artifact w)} (:dependencies r))
                (some #{artifact} (:dependencies r)))
           id :node-witness-context-mismatch :subject-source)
    (case (:kind w)
      :lean-witness
      (let [census (record! ctx (:declaration-census r) id)
            theorem (declaration! ctx artifact census id)]
        (declaration! ctx (:subject-artifact w) census id)
        (need! (and (= :theorem (:kind theorem)) (string? (:proposition theorem))
                    (not (str/blank? (:proposition theorem))))
               id :node-witness-declaration-missing :theorem)
        (need! (= (:module artifact) (:module theorem)) id :node-witness-context-mismatch :module)
        (need! (and (= :passed (:typecheck r)) (= :passed (:axiom-check r))
                    (vector? (:axioms theorem)) (not (some #{"sorryAx"} (:axioms theorem))))
               id :node-witness-verification-missing :theorem-and-axiom-check)
        (dependencies! ctx (:import-closure r) id)
        (doseq [k [:toolchain :checker]] (resolve! ctx (get r k) id)))
      :commissioning
      (let [case-record (record! ctx artifact id)]
        (need! (and (contains? case-record :expected) (contains? case-record :observed)
                    (some? (:observed case-record)) (true? (:executed? case-record))
                    (= :passed (:result case-record))
                    (= (:case r) (:case case-record)) (some? (:case r))
                    (= (:expected case-record) (:observed case-record)))
               id :node-witness-verification-missing :induced-observed-outcome)
        (need! (= (:production-entrypoint r) (:production-entrypoint case-record))
               id :node-witness-context-mismatch :production-entrypoint)
        (need! (string? (:production-entrypoint r)) id :node-witness-verification-missing :entrypoint)
        (dependencies! ctx (:mechanisms r) id)
        (need! (= (:mechanisms r) (:mechanisms case-record))
               id :node-witness-context-mismatch :mechanisms))
      :replay-pin
      (let [capture (record! ctx artifact id)]
        (need! (and (some? capture) (= artifact (:capture r))
                    (map? (:source-identity r)) (seq (:source-identity r)))
               id :node-witness-context-mismatch :capture)
        (let [assertion (record! ctx (:assertion r) id)]
          (need! (and (= true (:passed? assertion)) (= artifact (:capture assertion))
                      (= (:scope w) (:scope assertion)))
                 id :node-witness-verification-missing :replay-assertion))
        (resolve! ctx (:checker r) id)))))
(defn validate! [ctx authorities owner w]
  (let [id (:id w)]
    (need! (and (= :wm/node-witness-v1 (:schema w))
                (or (nil? id) (and (string? id) (not (str/blank? id))))
                (contains? #{:proposed :admitted} (:status w))
                (contains? maximum-claims (:kind w))
                (contains? #{:supports :refutes} (:polarity w))
                (every? #(some? (get w %)) [:node :equation :quantity :declaration :claim :scope :as-of])
                (every? keyword? ((juxt :node :equation :quantity) w))
                (string? (:claim w)) (not (str/blank? (:claim w)))
                (or (keyword? (:scope w)) (string? (:scope w))))
           id :node-witness-schema-invalid w)
    (try (java.time.LocalDate/parse (:as-of w))
         (catch Exception _ (refuse! id :node-witness-schema-invalid :as-of)))
    (need! (some #{(:declaration w)} owner) id :node-witness-declaration-missing :owner)
    (let [equations (read-edn (:text (apply read-path! ctx (conj (:equations authorities) id))))
          nodes (read-edn (:text (apply read-path! ctx (conj (:nodes authorities) id))))
          matches (filter #(= (:id %) (:equation w)) (:equations equations))
          eq (first matches)]
      (need! (and (= 1 (count matches))
                  (some #(= (:node %) (name (:node w))) (:nodes nodes))
                  (= [(:node w) (:quantity w) (:declaration w)]
                     [(:node eq) (:defines eq) (:lean eq)]))
             id :node-witness-subject-mismatch :equation-node-quantity-declaration))
    (if (= :proposed (:status w))
      ;; Proposals retain historical pins and divergences; they are NOT evidence.
      ;; Include every available resolved dependency in the input digest even here.
      (do (doseq [loc (filter #(and (map? %) (:repo %) (:path %))
                             (tree-seq coll? seq w))]
            (try (read-path! ctx (:repo loc) (:path loc) id)
                 (catch clojure.lang.ExceptionInfo e
                   (when-not (= :node-witness-artifact-missing (:error (ex-data e))) (throw e)))))
          {:id id :status :pending :node (:node w)})
      (let [r (receipt! ctx w :verification)]
        (resolve! ctx (:artifact w) id)
        (resolve! ctx (:subject-artifact w) id)
        (need! (str/ends-with? (str (:declaration (:subject-artifact w)))
                              (str "." (:declaration w)))
               id :node-witness-subject-mismatch :subject-declaration)
        (declaration! ctx (:subject-artifact w)
                      (record! ctx (:declaration-census r) id) id)
        (verify-kind! ctx w r)
        (let [review (receipt! ctx w :review)]
          (doseq [loc (filter #(and (map? %) (:repo %) (:path %))
                             (tree-seq coll? seq [w r review]))
                  :when (contains? loc :sha256)]
            (resolve! ctx loc id))
          (doseq [loc (vals (:retained-evidence w))] (resolve! ctx loc id))
          (need! (and (= :approved (:verdict review)) (string? (:reviewer review))
                      (not (str/blank? (:reviewer review))))
                 id :node-witness-review-missing :reviewer-verdict)
          (need! (= (:receipt (:verification w)) (:verification review))
                 id :node-witness-context-mismatch :review-verification)
          (dependencies! ctx (:dependencies review) id))
        {:id id :status :verified-binding :node (:node w)
         :equation (:equation w) :polarity (:polarity w) :scope (:scope w)
         :maximum-claim (maximum-claims (:kind w))}))))

(defn witnessed-bindings
  "Consumer helper: existential binding coverage, never all-node coverage.
  Pass already-derived lower-rung and FUNDAMENTALS results. Missing inputs
  fail closed; a readiness/licence pointer is not an input to this gate."
  [admissions {:keys [node preceding-rung fundamentals-cap]}]
  (if (and (= 3 preceding-rung) (integer? fundamentals-cap) (<= 4 fundamentals-cap))
    (vec (filter #(and (= node (:node %)) (= :verified-binding (:status %))) admissions)) []))
