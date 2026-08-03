# FIS BSS deliverable references

Pointers to real FIS BSS project artifacts (in `fis-ai-kit/templates/sample-docx/`) — useful as templates / examples when authoring new BSS-flavored artifacts.

## Available samples

| Sample | What it is | Useful for |
|---|---|---|
| `Mau-bao-cao-yeu-cau-FIS-v2.xlsx` | Standard FIS testcase Excel | xlsx layout for `/fis-test` |

## How to use

1. Open the sample file → understand section structure + tone.
2. Reference its layout (don't copy customer content) when authoring the equivalent artifact.
3. The corresponding template lives at `fis-ai-kit/templates/docx-templates/<artifact>.docx` — that's the FIS-internal-IP-stripped version used by `/fis-docs export`.

## Mapping samples to artifact types

| Artifact type | Sample to study | Template to render |
|---|---|---|
| Test cases (.xlsx) | `Mau-bao-cao-yeu-cau-FIS-v2.xlsx` | `/fis-test` |

## Anti-patterns

- Reusing FIS-internal customer content from samples in new project artifacts → IP / NDA breach.
- Authoring FS / TRD without reading the corresponding sample → drift from FIS house style.
