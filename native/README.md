# Native engine admission

Raven deliberately does not vendor unverified engine source in this archive.

Admitted revisions:
- llama.cpp: `23b0202a189c44a54625aadcb37a946dd1d6278d`
- whisper.cpp: `d1be6fde11ac6e0407606b4e42fe72d34add8037`
- user-created source archive SHA-256: `e94b4d1bc206c367378700b2491401ae7ab67981ed49e6544c21855cc131a256`

Before Android native build, place the exact pinned llama.cpp tree at `native/llama.cpp` and whisper.cpp at `native/whisper.cpp`. The CMake build fails closed if llama.cpp is absent.
