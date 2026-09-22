(ns futon2.aif.ticket-queue
  "Declared front placement for any ordinary ticket. The declaration orders
   eligibility; it supplies neither candidates nor model quantities."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.load-identity :as load-identity])
  (:import [java.time Instant]))

(load-identity/register! *ns* *file*)

(def empty-declaration
  {:schema :wm/ticket-queue-v1
   :placement :front
   :order [:inserted-at :ticket]
   :within-stratum :cascade-selection-posterior
   :entries []})

(defn validate!
  "Reject ambiguous declarations, including duplicate ticket identities.
   Times are Instants, compared chronologically rather than lexically."
  [declaration]
  (when-not
   (and (= (dissoc empty-declaration :entries) (dissoc declaration :entries))
        (vector? (:entries declaration))
        (= (count (:entries declaration)) (count (set (map :ticket (:entries declaration)))))
        (every? (fn [{:keys [ticket inserted-at] :as entry}]
                  (and (= #{:ticket :inserted-at} (set (keys entry)))
                       (string? ticket) (boolean (re-matches #"T-[A-Za-z0-9][A-Za-z0-9._-]*" ticket))
                       (string? inserted-at)
                       (try (Instant/parse inserted-at) true (catch Exception _ false))))
                (:entries declaration)))
    (throw (ex-info "Invalid ticket queue declaration"
                    {:kind :invalid-ticket-queue :declaration declaration})))
  declaration)

(defn read-declaration
  "Read one declared resource; missing or malformed configuration refuses."
  []
  (with-open [reader (java.io.PushbackReader. (io/reader (io/resource "wm/ticket-queue.edn")))]
    (let [declaration (edn/read {:eof ::eof} reader)]
      (when-not (= ::eof (edn/read {:eof ::eof} reader))
        (throw (ex-info "Multiple ticket queue forms" {:kind :invalid-ticket-queue})))
      (validate! declaration))))

(defn supported?
  "Selection's existing support constraints, before floating-point underflow."
  [candidate]
  (and (seq (get-in candidate [:id :precedence]))
       (not= :zero-support (:f-status candidate))
       (number? (:g candidate)) (Double/isFinite (double (:g candidate)))))

(defn plan
  "Return nil for an empty declaration, preserving historical decision bytes.
   Refused entries remain recorded and never block the next admitted stratum."
  [declaration candidates refusals]
  (validate! declaration)
  (when (seq (:entries declaration))
    (let [admitted (set (map (comp :target :id) (filter supported? candidates)))
          present (set (map (comp :target :id) candidates))
          entries (mapv (fn [entry]
                          (let [target (:ticket entry)]
                            (if (contains? admitted target)
                              (assoc entry :status :admitted)
                              (assoc entry :status :not-admitted
                                     :refusals (vec (or (seq (filter #(= target (:target %)) refusals))
                                                   [{:target target :kind (if (contains? present target)
                                                                           :no-policy-support
                                                                           :not-in-admitted-candidates)}]))))))
                        (sort-by (juxt #(Instant/parse (:inserted-at %)) :ticket) (:entries declaration)))
          eligible (first (filter #(= :admitted (:status %)) entries))]
      {:schema :wm/ticket-queue-selection-v1
       :declaration declaration
       :entries entries
       :eligible-targets (if eligible [(:ticket eligible)] [])
       :status (if eligible :front-stratum :no-admitted-front-entry)})))
