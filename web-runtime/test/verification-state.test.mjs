import test from "node:test";
import assert from "node:assert/strict";

import {
  base64ToBytes,
  bytesToBase64,
  canScanQrFromRequest,
  canShowQrFromRequest,
  phaseName,
  sasPayloadFromCallbacks,
  verificationPayloadFromRequest,
} from "../src/matrix-runtime.js";

test("maps verification request state for the native UI", () => {
  const request = {
    transactionId: "txn1",
    roomId: undefined,
    initiatedByMe: false,
    otherUserId: "@alice:example.org",
    otherDeviceId: "ALICEDEVICE",
    isSelfVerification: true,
    phase: 3,
    pending: true,
    accepting: false,
    declining: false,
    timeout: 300000,
    methods: ["m.sas.v1"],
    chosenMethod: null,
    otherPartySupportsMethod: (method) => method === "m.qr_code.scan.v1",
    generateQRCode: async () => new Uint8ClampedArray([1, 2, 3]),
  };

  const payload = verificationPayloadFromRequest(request, "incoming");

  assert.equal(payload.transactionId, "txn1");
  assert.equal(payload.otherUserId, "@alice:example.org");
  assert.equal(payload.otherDeviceId, "ALICEDEVICE");
  assert.equal(payload.isSelfVerification, true);
  assert.equal(payload.phaseCode, 3);
  assert.equal(payload.phase, "Ready");
  assert.equal(payload.canAccept, false);
  assert.equal(payload.canStartSas, true);
  assert.equal(payload.canConfirmSas, false);
  assert.equal(payload.canShowQr, true);
  assert.equal(payload.canScanQr, false);
  assert.equal(payload.canConfirmQr, false);
  assert.equal(payload.source, "incoming");
});

test("keeps verification payload resilient when methods getter throws", () => {
  const request = {
    phase: 3,
    get methods() {
      throw new Error("not implemented");
    },
    otherPartySupportsMethod: () => false,
    generateQRCode: async () => new Uint8ClampedArray([1, 2, 3]),
  };

  const payload = verificationPayloadFromRequest(request, "incoming");

  assert.deepEqual(payload.methods, []);
  assert.equal(payload.canShowQr, false);
  assert.equal(payload.canScanQr, false);
});

test("detects QR show support only when the other side can scan", () => {
  const request = {
    phase: 3,
    otherPartySupportsMethod: (method) => method === "m.qr_code.scan.v1",
    generateQRCode: async () => new Uint8ClampedArray([1, 2, 3]),
  };

  assert.equal(canShowQrFromRequest(request), true);
  assert.equal(canShowQrFromRequest({ ...request, phase: 2 }), false);
  assert.equal(canShowQrFromRequest({ ...request, otherPartySupportsMethod: () => false }), false);
});

test("detects QR scan support only when the other side can show", () => {
  const request = {
    phase: 3,
    otherPartySupportsMethod: (method) => method === "m.qr_code.show.v1",
    scanQRCode: async () => ({}),
  };

  assert.equal(canScanQrFromRequest(request), true);
  assert.equal(canScanQrFromRequest({ ...request, phase: 2 }), false);
  assert.equal(canScanQrFromRequest({ ...request, otherPartySupportsMethod: () => false }), false);
});

test("maps SAS callbacks into compact native text", () => {
  const payload = sasPayloadFromCallbacks({
    sas: {
      decimal: [123, 456, 789],
      emoji: [
        ["A", "Alpha"],
        ["B", "Bravo"],
      ],
    },
  });

  assert.equal(payload.sasDecimal, "123 456 789");
  assert.equal(payload.sasEmoji, "A Alpha\nB Bravo");
  assert.equal(payload.canConfirmSas, true);
});

test("encodes Matrix QR payload bytes for the native bridge", () => {
  const bytes = new Uint8ClampedArray([77, 65, 84, 82, 73, 88]);

  assert.equal(bytesToBase64(bytes), "TUFUUklY");
  assert.deepEqual(Array.from(base64ToBytes("TUFUUklY")), [77, 65, 84, 82, 73, 88]);
});

test("names Matrix verification phases", () => {
  assert.equal(phaseName(1), "Unsent");
  assert.equal(phaseName(2), "Requested");
  assert.equal(phaseName(3), "Ready");
  assert.equal(phaseName(4), "Started");
  assert.equal(phaseName(5), "Cancelled");
  assert.equal(phaseName(6), "Done");
  assert.equal(phaseName(100), "Unknown");
});
