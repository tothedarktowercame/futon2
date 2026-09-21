(ns futon2.aif.job-text-retention
  "Retain the prompt and final reply already held by a runner; no Agency IO."
  (:require [futon2.aif.load-identity :as load-identity]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.nio.charset StandardCharsets]
           [java.nio.file Files StandardOpenOption FileAlreadyExistsException]
           [java.security MessageDigest]))

(load-identity/register! *ns* *file*)

(defn- digest [^bytes bytes]
  (apply str (map #(format "%02x" (bit-and 255 %))
                 (.digest (MessageDigest/getInstance "SHA-256") bytes))))

(defn- retain-text! [attempt-dir identity kind text]
  (cond
    (nil? text) {:status :absent :reason :text-not-recorded}
    (not (string? text)) {:status :absent :reason :text-not-a-string}
    (str/blank? text) {:status :absent :reason :text-empty}
    :else
    (let [bytes (.getBytes ^String text StandardCharsets/UTF_8)
          sha (digest bytes)
          file (io/file attempt-dir (str "job-" identity "-" (name kind) "-" sha ".txt"))
          path (.toPath file)]
      (io/make-parents file)
      (try
        (Files/write path bytes (into-array StandardOpenOption
                                           [StandardOpenOption/CREATE_NEW StandardOpenOption/WRITE]))
        (catch FileAlreadyExistsException _
          (when-not (java.util.Arrays/equals bytes (Files/readAllBytes path))
            (throw (ex-info "Retained job text does not match its content address"
                            {:failure-kind :job-text-content-mismatch :path (str file)})))))
      {:status :present :path (.getAbsolutePath file) :sha256 sha
       :encoding :utf-8 :bytes (alength bytes)})))

(defn retain-job-texts!
  "Write separate UTF-8 content-addressed files beside an attempt's checkpoints.
  Missing/blank/non-string values are typed absences, never placeholder files.
  Identical replays verify existing bytes; changed text gets a new immutable
  path so an earlier checkpoint's digest remains valid. Role/job IDs never
  become path components."
  [attempt-dir role job-id prompt reply]
  (when-not attempt-dir
    (throw (ex-info "Job retention requires an attempt directory"
                    {:failure-kind :job-text-attempt-directory-required})))
  (let [identity (digest (.getBytes (pr-str [role job-id]) StandardCharsets/UTF_8))]
    {:role role :job-id job-id :prompt-ref (str "agency-job:" job-id)
     :prompt (retain-text! attempt-dir identity :prompt prompt)
     :reply (retain-text! attempt-dir identity :reply reply)}))

(defn- event-prompt [job]
  (some #(when (and (#{"prompt" :prompt} (:type %)) (string? (:text %)))
           (:text %))
        (reverse (:events job))))

(defn wrap-ports
  "Wrap supplied dispatch/poll/read ports once per attempt. Preserve return
  values, retain all retries/revisions through the same ports, and issue no
  additional calls. STATE records references, never duplicate prompt bodies.
  Production dispatch may attach the actual post-binding prompt as metadata.
  Agency :result is authoritative; a short :result-summary is not a reply."
  [opts attempt-dir state dispatch poll read-job]
  (let [remember! (fn [role actor phase job-id prompt reply source job]
                    (let [record (assoc (retain-job-texts! attempt-dir role job-id prompt reply)
                                        :agent actor :dispatch-phase phase
                                        :prompt-source source
                                        :reply-source :agency-result
                                        :job-state (:state job))]
                      (swap! state (fn [rows]
                                     (conj (filterv #(not= job-id (:job-id %)) rows) record)))
                      record))
        capture! (fn [job-id job]
                   (let [previous (some #(when (= job-id (:job-id %)) %) @state)
                         known? (= :present (get-in previous [:prompt :status]))
                         keep-reply? (and (= :present (get-in previous [:reply :status]))
                                          (or (not (string? (:result job)))
                                              (str/blank? (:result job))))
                         event (event-prompt job)
                         trimmed? (and event (str/ends-with? event " …[trimmed]"))
                         prompt (when (and (not known?) (not trimmed?)) event)]
                     (remember! (or (:role previous) :recovered-job)
                                (or (:agent previous) (:agent-id job))
                                (:dispatch-phase previous) job-id prompt (:result job)
                                (if known? (:prompt-source previous) :agency-prompt-event) job)
                     ;; Agency prompt events are bounded previews. A known
                     ;; dispatch prompt always wins; a truncated recovery is
                     ;; an absence, not the exact prompt requested here.
                     (when (or known? trimmed? keep-reply?)
                       (swap! state (fn [rows]
                                      (mapv #(if (= job-id (:job-id %))
                                               (cond-> %
                                                 (or known? trimmed?)
                                                 (assoc :prompt (if known? (:prompt previous)
                                                                    {:status :absent
                                                                     :reason :agency-prompt-truncated}))
                                                 keep-reply? (assoc :reply (:reply previous)))
                                               %) rows)))))
                   job)]
    (assoc opts
           :dispatch-fn
           (fn [call-opts actor caller mission prompt]
             (let [response (dispatch call-opts actor caller mission prompt)
                   job-id (:job-id response)
                   actual (or (::dispatched-prompt (meta response)) (:prompt response) prompt)
                   role (cond (= actor (:author opts)) :author
                              (= actor (:repair-reviewer opts)) :repair-reviewer
                              (= actor (:reviewer opts)) :reviewer
                              :else :other)]
               (when job-id
                 (remember! role actor (get-in (some-> (:wm-phase-state call-opts) deref) [:phase])
                            job-id actual (:result response) :dispatch response))
               response))
           :poll-fn (fn [call-opts job-id] (capture! job-id (poll call-opts job-id)))
           :read-job-fn (fn [call-opts job-id] (capture! job-id (read-job call-opts job-id))))))

(defn checkpoint-cell
  "Attach the available immutable references without changing term/sorry kind."
  [cell records]
  (if (contains? cell :judgment)
    (assoc-in cell [:judgment :job-texts] records)
    (assoc-in cell [:sorry :job-texts] records)))
