import { session } from "/js/api.js";

let controller = null;
let stopped = true;
let retryTimer = null;

// EventSource cannot send an Authorization header, so the stream is read with fetch.
export function startNotifications(onEvent, onState) {
  stopNotifications();
  stopped = false;
  connect(onEvent, onState, 1000);
}

export function stopNotifications() {
  stopped = true;
  clearTimeout(retryTimer);
  if (controller) controller.abort();
  controller = null;
}

async function connect(onEvent, onState, delay) {
  const token = session.token();
  if (!token || stopped) return;
  controller = new AbortController();
  try {
    const response = await fetch("/api/notifications/stream", {
      headers: { Accept: "text/event-stream", Authorization: "Bearer " + token },
      signal: controller.signal,
    });
    if (!response.ok || !response.body) throw new Error("stream " + response.status);
    onState(true);
    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = "";
    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, "\n");
      let index;
      while ((index = buffer.indexOf("\n\n")) >= 0) {
        const block = buffer.slice(0, index);
        buffer = buffer.slice(index + 2);
        const event = parseBlock(block);
        if (event) onEvent(event);
      }
    }
  } catch (e) {
    if (stopped) return;
  }
  onState(false);
  if (!stopped) {
    retryTimer = setTimeout(() => connect(onEvent, onState, Math.min(delay * 2, 30000)), delay);
  }
}

function parseBlock(block) {
  let name = "message";
  const data = [];
  block.split("\n").forEach((line) => {
    if (line.startsWith("event:")) name = line.slice(6).trim();
    else if (line.startsWith("data:")) data.push(line.slice(5).trim());
  });
  if (!data.length) return null;
  try {
    const parsed = JSON.parse(data.join("\n"));
    return { ...parsed, type: parsed.type || name };
  } catch (e) {
    return { type: name, message: data.join("\n") };
  }
}
