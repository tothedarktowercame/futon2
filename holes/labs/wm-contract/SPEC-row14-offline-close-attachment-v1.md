# Row 14 offline close/annotation attachment v1

Status: source interface for review. It performs no live attachment, historical
rewrite, categorical acquisition, count, kernel estimate, or admission.

The sole entry point resolves a pinned context by an externally configured
resolver. That context—not a caller-supplied qualified map—selects the pinned
raw annotation and close. The annotation is revalidated through the complete
resolved-subject validator. The close and context are strict UTF-8, one-form
EDN read from the same hashed bytes with mutation detection.

The independent context binds entity, run, cohort, attempt, close sequence,
checkpoint reference, action start/completion, evidence cutoff, disposition
recording time, authority scope/provenance, and exact close/annotation source
pointers. The close must independently retain the same selected entity in
`:outcome-entity` and `:entity-state-at-close`, and the same run through the
belief-source identity. Its `:outcome` is retained as disposition and never
enters the annotation evidence or rubric.

Output is a new `:wm/close-categorical-annotation-join-v1` value containing the
unaltered parsed close plus its exact source pointer, the raw annotation source,
the context and source, the fully revalidated annotation, and method/rubric/
retrospective/missingness/selection/scope limitations. Nothing writes this
value into the close. Duplicate annotations at one authoritative point refuse;
different labels refuse as conflict rather than being voted or overwritten.

The F11 September close is a read-only negative discovery reference. It has a
disposition but lacks outcome entity, entity state/run identity, categorical
authority, and context authority. It therefore supplies no attachment or
measured pair.
