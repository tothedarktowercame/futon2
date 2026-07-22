;;; b0-recognizer-test.el --- B0 mark recognizer batch tests -*- lexical-binding: t; -*-
;; Ground truth: the two adjudicated cases in
;; futon2/holes/labs/M-zaif-harness/b1-live-marks.edn, plus structural cases.

(load "/home/joe/code/futon3c/emacs/agent-chat.el")

(defvar b0-failures 0)

(defun b0-check (name text expected)
  "EXPECTED is a list of (MARK . VERDICT) in position order."
  (let* ((got (mapcar (lambda (m) (cons (plist-get m :mark)
                                        (plist-get m :verdict)))
                      (agent-chat-recognize-marks text))))
    (if (equal got expected)
        (message "PASS %s" name)
      (setq b0-failures (1+ b0-failures))
      (message "FAIL %s\n  expected %S\n  got      %S" name expected got))))

;; Adjudicated case 1 (e-60ba3204): convention proposal = mention.
(b0-check "corpus-mention"
          "I could mark corrections with ✘ for example"
          '(("correction" . "mention")))

;; Adjudicated case 2 (e-63c25e11): punctuating ✓ = event, "add a ✓" = mention.
(b0-check "corpus-event-plus-mention"
          "OK, that's good ✓ (maybe we can add a ✓ for approval)"
          '(("approval" . "event") ("approval" . "mention")))

;; Turn-initial glyph punctuates what follows = event.
(b0-check "turn-initial"
          "✘ the grain is mission, not cascade"
          '(("correction" . "event")))

;; Glyph inside a quoted span = mention.
(b0-check "quoted-mention"
          "the recognizer should treat \"✓\" as a mention"
          '(("approval" . "mention")))

;; Hydra long form = event with ref.
(let ((marks (agent-chat-recognize-marks
              "(✘ :ref e-63c25e11 \"wrong grain\") — see the census")))
  (if (and (= 1 (length marks))
           (equal "event" (plist-get (car marks) :verdict))
           (equal "e-63c25e11" (plist-get (car marks) :ref)))
      (message "PASS long-form-ref")
    (setq b0-failures (1+ b0-failures))
    (message "FAIL long-form-ref: %S" marks)))

;; Shadow tier glyph = tags-only event.
(b0-check "shadow-tier"
          "🧭 focus on Z1 next"
          '(("guidance" . "event")))

;; No glyphs → no marks.
(b0-check "no-marks" "plain turn with no glyphs at all" '())

;; Tag extraction: events only, deduped, mention contributes nothing.
(let ((tags (agent-chat--mark-event-tags
             (agent-chat-recognize-marks
              "✓ good ✓ done (maybe we can add a ✘ for corrections)"))))
  (if (equal tags '("approval"))
      (message "PASS event-tags-dedup")
    (setq b0-failures (1+ b0-failures))
    (message "FAIL event-tags-dedup: %S" tags)))

;; Body records round-trip through json-encode (the payload path).
(let* ((marks (agent-chat-recognize-marks "OK ✓ (✘ :ref e-abc \"why\")"))
       (json (json-encode (agent-chat--mark-body-records marks))))
  (if (and (string-match-p "approval" json) (string-match-p "e-abc" json))
      (message "PASS body-json")
    (setq b0-failures (1+ b0-failures))
    (message "FAIL body-json: %s" json)))

(if (zerop b0-failures)
    (message "ALL PASS")
  (kill-emacs 1))
