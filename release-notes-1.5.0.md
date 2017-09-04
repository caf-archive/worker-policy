#### Version Number
${version-number}

#### Bug Fixes
- [CAF-3493](CAF-3493): Metadata references were not being passed on sub-documents nested in a document for classification handler.
    Sub-documents included on a document were not persisting their metadata reference fields when a task was sent to the classification worker or via the generic queue handler. This has been corrected and metadata reference fields are now passed.
