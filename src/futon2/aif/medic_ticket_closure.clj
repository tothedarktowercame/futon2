(ns futon2.aif.medic-ticket-closure
  "Close the injury's own-cause repair ticket when the medic disposition is
   written. The join is a byte pin, never a failure-kind join: the finding
   record must carry the injury entry's exact :cause-record-pin and the pinned
   cause bytes must still hash to the pinned sha256. A failure-kind or stage
   coincidence proves nothing here and is deliberately never consulted.

   The closure form is the one already used in holes/tickets: Status DONE plus
   a Disposition section citing the injury, the ledger ordinal of the medic
   append, the route and the fixed-by commit."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.ticket-publication-io :as publication]))

(defn- read-edn [^java.io.File f]
  (try (edn/read-string (slurp f)) (catch Throwable _ ::unreadable)))

(defn- cause-pin-verified?
  "The injury's cause record must still hash to its pinned sha256; a moved or
   tampered cause record refuses closure rather than joining on anything else."
  [pin]
  (boolean
   (when (and (map? pin) (string? (:path pin)) (string? (:sha256 pin)))
     (try (= (:sha256 pin)
             (identity/sha256 (java.nio.file.Files/readAllBytes
                               (.toPath (io/file (:path pin))))))
          (catch Throwable _ nil)))))

(defn own-cause-findings
  "All finding records in the repair store whose recorded :cause-record-pin is
   byte-equal (path and sha256) to this injury entry's cause pin, with the
   cause bytes re-verified. Failure-kind/stage are never read. Returns [] when
   no verifiable link exists."
  [repair-root injury-entry]
  (if-not (cause-pin-verified? (:cause-record-pin injury-entry))
    []
    (let [pin (select-keys (:cause-record-pin injury-entry) [:path :sha256])
          dir (io/file repair-root "findings")]
      (if-not (.isDirectory dir)
        []
        (->> (.listFiles dir)
             (keep (fn [^java.io.File f]
                     (when (and (.isFile f) (.endsWith (.getName f) ".edn"))
                       (let [finding (read-edn f)]
                         (when (and (map? finding)
                                    (= pin (select-keys (:cause-record-pin finding)
                                                        [:path :sha256])))
                           {:id (:repair/id finding)
                            :path (.getCanonicalPath f)
                            :finding finding})))))
             (vec))))))

(defn- ticket-link [repair-root finding-id]
  (let [f (io/file repair-root "ticket-links" (str finding-id ".edn"))
        record (when (.isFile f) (read-edn f))]
    (when (and (map? record) (= :wm/finding-ticket-v1 (:schema record))
               (= finding-id (:finding/id record)))
      record)))

(defn- ticket-text [path]
  (try (slurp path) (catch Throwable _ nil)))

(defn- close-text
  "Rewrite the receiver's read: first bold Status line OPEN -> DONE, plus the
   Disposition section. Non-OPEN tickets are left untouched (already closed or
   hand-edited); an absent Status line refuses."
  [text {:keys [injury-id ordinal fixed-by receipt-pin at]}]
  (let [lines (str/split-lines text)
        i (some #(when (re-find #"^\s*\*\*Status:\*\*\s*OPEN\s*$" (nth lines %)) %)
                (range (count lines)))]
    (when (nil? i) (throw (ex-info "No OPEN status line" {})))
    (let [closed (assoc lines i "**Status:** DONE")]
      (str (str/join "\n"
                (concat closed
                        ["" "## Disposition" ""]
                        [(str "Done " at " by medic stamp: injury " injury-id
                              " discharged at ledger ordinal " ordinal
                              ", route open-out-of-band-medic, fixed by " fixed-by
                              ". Out-of-band receipt pin " receipt-pin ".")]))
           "\n"))))

(defn- commit-ticket [path message]
  (let [discovery (try (shell/sh "git" "-C"
                                 (str (.getParentFile (.getAbsoluteFile (io/file path))))
                                 "rev-parse" "--show-toplevel")
                       (catch java.io.IOException e {:exit -1 :err (.getMessage e)}))]
    (if-not (zero? (:exit discovery))
      {:status :skipped :reason :not-a-git-repository}
      (let [repo (str/trim (:out discovery))
            relative (str (.relativize (.toPath (io/file repo))
                                       (.toPath (io/file path))))
            add (shell/sh "git" "-C" repo "add" "--" relative)
            commit (shell/sh "git" "-C" repo "commit" "-m" message "--" relative)]
        (if (and (zero? (:exit add)) (zero? (:exit commit)))
          {:status :committed}
          {:status :failed :error (:err commit)})))))

(defn close-cause-ticket!
  "At the point the medic disposition is durably appended, close the repair
   ticket whose finding is the injury's own cause, using the holes/tickets
   closure form. Contains every failure: the discharge already succeeded and
   is never rolled back; an unlinked injury is reported with a stated reason
   and every ticket stays OPEN."
  [repair-root {:keys [injury-id entry fixed-by receipt-pin at]}]
  (try
    (let [causes (own-cause-findings repair-root entry)
          ids (distinct (map :id causes))]
      (cond
        (empty? ids) {:status :held :reason :no-verifiable-cause-link
                      :injury-id injury-id :closed []}
        (not= 1 (count ids)) {:status :held :reason :ambiguous-cause-link
                              :injury-id injury-id :finding-ids ids}
        :else
        (let [id (first ids)
              link (ticket-link repair-root id)
              path (get-in link [:finding/ticket :path])]
          (cond
            (nil? link) {:status :held :reason :ticket-link-unavailable
                         :injury-id injury-id :finding-id id}
            (nil? (ticket-text path)) {:status :held :reason :ticket-unreadable
                                       :injury-id injury-id :finding-id id :path path}
            :else
            (let [text (ticket-text path)]
              (if-not (re-find #"(?m)^\s*\*\*Status:\*\*\s*OPEN\s*$" text)
                {:status :held :reason :ticket-not-open
                 :injury-id injury-id :finding-id id :path path}
                (let [closed (close-text text {:injury-id injury-id
                                               :ordinal (:ordinal entry)
                                               :fixed-by fixed-by
                                               :receipt-pin (:sha256 receipt-pin)
                                               :at (or at "2026-10-31")})
                      _ (publication/publish-text! path closed true)]
                  {:status :closed :injury-id injury-id :finding-id id
                   :ticket (get-in link [:finding/ticket :id]) :path path
                   :ordinal (:ordinal entry) :fixed-by fixed-by
                   :git (commit-ticket
                         path (str "Close repair ticket " (get-in link [:finding/ticket :id])
                                   " (medic discharge of " injury-id " at ordinal "
                                   (:ordinal entry) ")"))})))))))
    (catch Throwable t
      {:status :held :reason :ticket-closure-failed :injury-id injury-id
       :detail (ex-message t)})))
