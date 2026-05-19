import test from "node:test";
import assert from "node:assert/strict";

import {
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
  assert.equal(payload.source, "incoming");
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

test("names Matrix verification phases", () => {
  assert.equal(phaseName(1), "Unsent");
  assert.equal(phaseName(2), "Requested");
  assert.equal(phaseName(3), "Ready");
  assert.equal(phaseName(4), "Started");
  assert.equal(phaseName(5), "Cancelled");
  assert.equal(phaseName(6), "Done");
  assert.equal(phaseName(100), "Unknown");
});
