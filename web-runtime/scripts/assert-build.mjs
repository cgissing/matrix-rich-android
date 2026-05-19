import { access, readFile } from "node:fs/promises";
import { resolve } from "node:path";

const outDir = resolve(import.meta.dirname, "../../app/src/main/assets/matrix-runtime");
await access(resolve(outDir, "index.html"));
await access(resolve(outDir, "matrix-runtime.js"));
await access(resolve(outDir, "pkg/matrix_sdk_crypto_wasm_bg.wasm"));

const bundle = await readFile(resolve(outDir, "matrix-runtime.js"), "utf8");
for (const marker of [
  "initRustCrypto",
  "MatrixRichRuntime",
  "sendTextMessage",
  "loadSessionBackupPrivateKeyFromSecretStorage",
  "requestOwnUserVerification",
  "VerifierEvent",
]) {
  if (!bundle.includes(marker)) {
    throw new Error(`Missing runtime marker: ${marker}`);
  }
}
