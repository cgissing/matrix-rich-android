import { mkdir, rm, copyFile } from "node:fs/promises";
import { resolve } from "node:path";
import { build } from "esbuild";

const root = resolve(import.meta.dirname, "..");
const outDir = resolve(root, "app/src/main/assets/matrix-runtime");
const wasmSource = resolve(
  import.meta.dirname,
  "node_modules/@matrix-org/matrix-sdk-crypto-wasm/pkg/matrix_sdk_crypto_wasm_bg.wasm",
);

await rm(outDir, { recursive: true, force: true });
await mkdir(resolve(outDir, "pkg"), { recursive: true });

await build({
  entryPoints: [resolve(import.meta.dirname, "src/matrix-runtime.js")],
  bundle: true,
  format: "esm",
  platform: "browser",
  target: ["chrome90"],
  outfile: resolve(outDir, "matrix-runtime.js"),
  logLevel: "info",
});

await copyFile(resolve(import.meta.dirname, "index.html"), resolve(outDir, "index.html"));
await copyFile(wasmSource, resolve(outDir, "pkg/matrix_sdk_crypto_wasm_bg.wasm"));
