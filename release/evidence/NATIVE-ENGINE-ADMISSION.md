# Native Engine Admission

Status: SOURCE ACQUIRED ON TARGET / INTEGRATION AUTHORED / ANDROID COMPILE PENDING

Pinned source revisions supplied from the Samsung/Termux acquisition environment:

- llama.cpp `23b0202a189c44a54625aadcb37a946dd1d6278d`
- whisper.cpp `d1be6fde11ac6e0407606b4e42fe72d34add8037`
- stripped source bundle SHA-256 `e94b4d1bc206c367378700b2491401ae7ab67981ed49e6544c21855cc131a256`

Raven now contains an arm64-v8a CMake/JNI llama.cpp integration boundary with model load, generation, streaming token callback, epoch cancellation, and deterministic close. Runtime PASS is withheld until the exact pinned engine source is present and Android native compilation/device execution succeeds.
