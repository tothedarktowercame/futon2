(ns futon2.aif.delivery-qa
  "Mandatory delivery-level QA for enacted War Machine opportunities.

   Operator decision evidence 6e6f56a1-b9d7-4f83-928f-3a211ef890a0 moves
   the human gate from enactment to delivered-work review. Writes are accepted
   only through the Arxana Field Desk API on port 7070."
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.string :as str]))

(def endpoint-path "/api/alpha/morning-brief/addendum")
(def decision-evidence-id "6e6f56a1-b9d7-4f83-928f-3a211ef890a0")

(defn- nonblank-string? [x]
  (and (string? x) (not (str/blank? x))))

(defn endpoint
  [{:keys [agency-base]}]
  (let [uri (java.net.URI. (str agency-base endpoint-path))]
    (when-not (= 7070 (.getPort uri))
      (throw
       (ex-info "Field Desk QA writes require the port-7070 endpoint"
                {:agency-base agency-base :port (.getPort uri)})))
    (str uri)))

(defn- evidence-ids
  [item]
  (->> [decision-evidence-id
        (get-in item [:witness :implementation-id])
        (get-in item [:witness :discharge-id])
        (get-in item [:failure :repair-id])]
       (filter nonblank-string?)
       distinct
       vec))

(defn qa-note
  "Build the concrete Field Desk note for one closed opportunity."
  [item]
  (let [attempt-id (:attempt-id item)
        commit (:commit item)
        ids (evidence-ids item)
        body {:delivery/outcome (:outcome item)
              :delivery/selected-target (:selected-target item)
              :delivery/achievement (get-in item [:achievement :summary])
              :delivery/built-or-changed
              (or (get-in item [:feature-card :built])
                  (when (nonblank-string? commit)
                    (str "Authored commit " commit))
                  "No repository change was delivered; the attempt closed with a machine finding.")
              :delivery/progressed
              (or (get-in item [:feature-card :want-coverage])
                  (get-in item [:failure :error])
                  "The full-loop opportunity reached its recorded terminal outcome.")
              :delivery/commit-shas
              (cond-> [] (nonblank-string? commit) (conj commit))
              :delivery/evidence-ids ids
              :delivery/witness-status
              (if (get-in item [:witness :implementation-id])
                :independently-reviewed-and-grounded
                :machine-recorded)
              :delivery/operator-decision-evidence-id decision-evidence-id}]
    (when-not (nonblank-string? attempt-id)
      (throw (ex-info "Delivery QA requires an attempt id" {:item item})))
    (when-not (seq ids)
      (throw (ex-info "Delivery QA requires evidence ids" {:item item})))
    {:attempt-id attempt-id
     :kind "note"
     :title (str "War Machine delivery QA — " attempt-id)
     :body (pr-str body)
     :author "war-machine"}))

(defn emit!
  "POST one mandatory QA note and fail the delivery gate on any rejection."
  [opts item]
  (let [url (endpoint opts)
        payload (qa-note item)
        response
        (http/post url
                   {:headers {"Content-Type" "application/json"}
                    :body (json/generate-string payload)
                    :timeout 10000
                    :throw false})
        response-body
        (try
          (json/parse-string (str (:body response)) true)
          (catch Exception _ {}))]
    (when-not (and (<= 200 (long (or (:status response) 0)) 299)
                   (true? (:ok response-body)))
      (throw
       (ex-info "Field Desk delivery QA gate failed"
                {:status (:status response)
                 :response response-body
                 :endpoint url
                 :attempt-id (:attempt-id item)})))
    (:addendum response-body)))
