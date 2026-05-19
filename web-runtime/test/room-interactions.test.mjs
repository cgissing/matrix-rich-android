import test from "node:test";
import assert from "node:assert/strict";

import {
  reactionEntriesFromEvents,
  typingNamesForRoom,
  typingSummaryFromNames,
} from "../src/matrix-runtime.js";

function reactionEvent(key, sender, eventId = `$${key}-${sender}`) {
  return {
    getContent: () => ({
      "m.relates_to": {
        rel_type: "m.annotation",
        key,
      },
    }),
    getSender: () => sender,
    getId: () => eventId,
  };
}

test("summarizes room typing state for the native UI", () => {
  assert.equal(typingSummaryFromNames([]), "");
  assert.equal(typingSummaryFromNames(["Alice"]), "Alice is typing");
  assert.equal(typingSummaryFromNames(["Alice", "Bob"]), "Alice and Bob are typing");
  assert.equal(typingSummaryFromNames(["Alice", "Bob", "Carol"]), "Alice, Bob, and 1 more are typing");
});

test("keeps every remote typing member so the summary count stays accurate", () => {
  const room = {
    getJoinedMembers: () => [
      { userId: "@alice:example.org", name: "Alice", typing: true },
      { userId: "@bob:example.org", name: "Bob", typing: true },
      { userId: "@carol:example.org", name: "Carol", typing: true },
      { userId: "@dave:example.org", name: "Dave", typing: true },
      { userId: "@me:example.org", name: "Me", typing: true },
      { userId: "@idle:example.org", name: "Idle", typing: false },
    ],
  };

  assert.deepEqual(typingNamesForRoom(room, "@me:example.org"), ["Alice", "Bob", "Carol", "Dave"]);
  assert.equal(typingSummaryFromNames(typingNamesForRoom(room, "@me:example.org")), "Alice, Bob, and 2 more are typing");
});

test("aggregates Matrix reaction events by emoji key", () => {
  const reactions = reactionEntriesFromEvents([
    reactionEvent("👍", "@alice:example.org", "$r1"),
    reactionEvent("👍", "@me:example.org", "$r2"),
    reactionEvent("❤️", "@bob:example.org", "$r3"),
  ], "@me:example.org");

  assert.deepEqual(reactions, [
    { key: "👍", count: 2, reactedByMe: true, ownEventId: "$r2" },
    { key: "❤️", count: 1, reactedByMe: false, ownEventId: "" },
  ]);
});
