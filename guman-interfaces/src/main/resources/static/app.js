(() => {
  "use strict";

  const STORAGE_KEY = "guman.conversations.v1";
  const USER_KEY = "guman.user-id.v1";

  const elements = {
    composer: document.querySelector("#composer"),
    input: document.querySelector("#messageInput"),
    send: document.querySelector("#sendButton"),
    conversation: document.querySelector("#conversation"),
    messageList: document.querySelector("#messageList"),
    emptyState: document.querySelector("#emptyState"),
    conversationList: document.querySelector("#conversationList"),
    newChat: document.querySelector("#newChatButton"),
    status: document.querySelector("#connectionStatus"),
    sidebar: document.querySelector("#sidebar"),
    sidebarOverlay: document.querySelector("#sidebarOverlay"),
    sidebarClose: document.querySelector("#sidebarClose"),
    mobileMenu: document.querySelector("#mobileMenu"),
    toast: document.querySelector("#toast")
  };

  const state = {
    conversations: loadConversations(),
    activeId: null,
    abortController: null,
    streaming: false,
    toastTimer: null,
    userId: loadUserId()
  };

  initialize();

  function initialize() {
    state.activeId = state.conversations[0]?.id ?? createConversation(false).id;
    renderAll();
    bindEvents();
    resizeInput();
    elements.input.focus();
  }

  function bindEvents() {
    elements.composer.addEventListener("submit", handleSubmit);
    elements.input.addEventListener("input", () => {
      resizeInput();
      updateSendButton();
    });
    elements.input.addEventListener("keydown", event => {
      if (event.key === "Enter" && !event.shiftKey && !event.isComposing) {
        event.preventDefault();
        if (!state.streaming && elements.input.value.trim()) {
          elements.composer.requestSubmit();
        }
      }
    });
    elements.newChat.addEventListener("click", () => createConversation(true));
    elements.send.addEventListener("click", event => {
      if (state.streaming) {
        event.preventDefault();
        state.abortController?.abort();
      }
    });
    document.querySelectorAll("[data-prompt]").forEach(button => {
      button.addEventListener("click", () => {
        elements.input.value = button.dataset.prompt;
        resizeInput();
        updateSendButton();
        elements.input.focus();
      });
    });
    elements.mobileMenu.addEventListener("click", openSidebar);
    elements.sidebarClose.addEventListener("click", closeSidebar);
    elements.sidebarOverlay.addEventListener("click", closeSidebar);
    document.addEventListener("keydown", event => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        createConversation(true);
      }
      if (event.key === "Escape") {
        closeSidebar();
      }
    });
  }

  function loadConversations() {
    try {
      const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]");
      return Array.isArray(saved) ? saved : [];
    } catch {
      return [];
    }
  }

  function loadUserId() {
    let userId = localStorage.getItem(USER_KEY);
    if (!userId) {
      userId = `web-${crypto.randomUUID()}`;
      localStorage.setItem(USER_KEY, userId);
    }
    return userId;
  }

  function createConversation(focusInput) {
    if (state.streaming) {
      state.abortController?.abort();
    }
    const conversation = {
      id: crypto.randomUUID(),
      title: "新对话",
      createdAt: Date.now(),
      updatedAt: Date.now(),
      messages: []
    };
    state.conversations.unshift(conversation);
    state.activeId = conversation.id;
    persist();
    renderAll();
    closeSidebar();
    if (focusInput) elements.input.focus();
    return conversation;
  }

  function activeConversation() {
    return state.conversations.find(item => item.id === state.activeId);
  }

  function renderAll() {
    renderConversationList();
    renderMessages();
    updateSendButton();
  }

  function renderConversationList() {
    elements.conversationList.replaceChildren();
    const sorted = [...state.conversations].sort((a, b) => b.updatedAt - a.updatedAt);

    sorted.forEach(conversation => {
      const row = document.createElement("div");
      row.className = "conversation-row";
      const button = document.createElement("button");
      button.type = "button";
      button.className = `conversation-item${conversation.id === state.activeId ? " active" : ""}`;
      button.addEventListener("click", () => {
        if (state.streaming) {
          showToast("请先停止当前生成");
          return;
        }
        state.activeId = conversation.id;
        renderAll();
        closeSidebar();
      });

      const title = document.createElement("span");
      title.className = "conversation-title";
      title.textContent = conversation.title;
      button.append(title);

      const deleteButton = document.createElement("button");
      deleteButton.type = "button";
      deleteButton.className = "delete-conversation";
      deleteButton.setAttribute("aria-label", `删除 ${conversation.title}`);
      deleteButton.innerHTML = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 7h14M9 7V4h6v3M8 10v8M12 10v8M16 10v8M7 7l1 14h8l1-14"/></svg>';
      deleteButton.addEventListener("click", event => {
        event.stopPropagation();
        deleteConversation(conversation.id);
      });
      row.append(button, deleteButton);
      elements.conversationList.append(row);
    });
  }

  function deleteConversation(id) {
    if (state.streaming && id === state.activeId) {
      showToast("生成期间不能删除当前对话");
      return;
    }
    state.conversations = state.conversations.filter(item => item.id !== id);
    if (state.activeId === id) {
      state.activeId = state.conversations[0]?.id ?? createConversation(false).id;
    }
    persist();
    renderAll();
  }

  function renderMessages() {
    const conversation = activeConversation();
    const messages = conversation?.messages ?? [];
    elements.emptyState.hidden = messages.length > 0;
    elements.messageList.replaceChildren();

    messages.forEach(message => {
      elements.messageList.append(createMessageElement(message));
    });
    scrollToBottom(false);
  }

  function createMessageElement(message) {
    const article = document.createElement("article");
    article.className = `message ${message.role}`;
    article.dataset.messageId = message.id;

    if (message.role === "user") {
      const content = document.createElement("div");
      content.className = "message-content";
      content.textContent = message.content;
      article.append(content);
      return article;
    }

    const avatar = document.createElement("div");
    avatar.className = "assistant-avatar";
    avatar.textContent = "G";
    const body = document.createElement("div");
    body.className = "assistant-body message-content";
    const name = document.createElement("div");
    name.className = "assistant-name";
    name.textContent = "Guman";
    const thinking = document.createElement("details");
    thinking.className = "thinking-panel";
    thinking.hidden = !message.thinking;
    thinking.open = Boolean(message.thinking);
    const thinkingSummary = document.createElement("summary");
    thinkingSummary.textContent = "思考过程";
    const thinkingContent = document.createElement("div");
    thinkingContent.className = "thinking-content";
    thinkingContent.textContent = message.thinking ?? "";
    thinking.append(thinkingSummary, thinkingContent);
    const tools = document.createElement("div");
    tools.className = "tool-events";
    renderToolEvents(tools, message.tools ?? []);
    const text = document.createElement("div");
    text.className = "assistant-text";
    text.textContent = message.content;
    body.append(name, thinking, tools, text);
    article.append(avatar, body);
    return article;
  }

  function renderToolEvents(container, tools) {
    container.replaceChildren();
    tools.forEach(tool => {
      const element = document.createElement("span");
      element.className = `tool-event${tool.done ? " done" : ""}`;
      element.textContent = tool.done ? `${tool.name} 已完成` : `正在调用 ${tool.name}`;
      container.append(element);
    });
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (state.streaming) return;
    const prompt = elements.input.value.trim();
    if (!prompt) return;

    const conversation = activeConversation() ?? createConversation(false);
    const userMessage = { id: crypto.randomUUID(), role: "user", content: prompt };
    const assistantMessage = {
      id: crypto.randomUUID(),
      role: "assistant",
      content: "",
      thinking: "",
      tools: []
    };

    conversation.messages.push(userMessage, assistantMessage);
    if (conversation.title === "新对话") {
      conversation.title = createTitle(prompt);
    }
    conversation.updatedAt = Date.now();
    elements.input.value = "";
    resizeInput();
    persist();
    renderAll();
    setStreaming(true);

    state.abortController = new AbortController();
    try {
      const response = await fetch("/api/chat/stream", {
        method: "POST",
        headers: {
          "Accept": "text/event-stream",
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          message: prompt,
          userId: state.userId,
          sessionId: conversation.id
        }),
        signal: state.abortController.signal
      });

      if (!response.ok || !response.body) {
        throw new Error(await readError(response));
      }
      await consumeEventStream(response.body, payload => {
        applyStreamEvent(assistantMessage, payload);
      });
    } catch (error) {
      if (error.name === "AbortError") {
        if (!assistantMessage.content) assistantMessage.content = "已停止生成。";
      } else {
        assistantMessage.content ||= `连接失败：${error.message}`;
        showToast("请求失败，请检查服务和 API Key");
      }
    } finally {
      conversation.updatedAt = Date.now();
      persist();
      updateAssistantMessage(assistantMessage);
      setStreaming(false);
      state.abortController = null;
    }
  }

  async function consumeEventStream(stream, onEvent) {
    const reader = stream.getReader();
    const decoder = new TextDecoder();
    let buffer = "";

    while (true) {
      const { value, done } = await reader.read();
      buffer += decoder.decode(value || new Uint8Array(), { stream: !done });
      buffer = buffer.replaceAll("\r\n", "\n");
      let boundary;
      while ((boundary = buffer.indexOf("\n\n")) >= 0) {
        const frame = buffer.slice(0, boundary);
        buffer = buffer.slice(boundary + 2);
        const event = parseEventFrame(frame);
        if (event) onEvent(event);
      }
      if (done) break;
    }
  }

  function parseEventFrame(frame) {
    let eventName = "message";
    const dataLines = [];
    frame.split("\n").forEach(line => {
      if (line.startsWith("event:")) eventName = line.slice(6).trim();
      if (line.startsWith("data:")) dataLines.push(line.slice(5).trimStart());
    });
    if (!dataLines.length) return null;
    try {
      return { eventName, data: JSON.parse(dataLines.join("\n")) };
    } catch {
      return null;
    }
  }

  function applyStreamEvent(message, payload) {
    const { eventName, data } = payload;
    if (eventName === "thinking-delta") {
      message.thinking = (message.thinking ?? "") + (data.content ?? "");
    } else if (eventName === "text-delta") {
      message.content += data.content ?? "";
    } else if (eventName === "tool-start") {
      message.tools.push({ name: data.content || "工具", done: false });
    } else if (eventName === "tool-end") {
      const name = (data.content || "").split(":", 1)[0];
      const tool = [...message.tools].reverse().find(item => item.name === name && !item.done);
      if (tool) tool.done = true;
    } else if (eventName === "error") {
      message.content ||= `生成失败：${data.content || "未知错误"}`;
    }
    updateAssistantMessage(message);
    scrollToBottom(true);
  }

  function updateAssistantMessage(message) {
    const article = elements.messageList.querySelector(`[data-message-id="${message.id}"]`);
    if (!article) return;
    const thinkingPanel = article.querySelector(".thinking-panel");
    const thinkingContent = message.thinking ?? "";
    const wasHidden = thinkingPanel.hidden;
    thinkingPanel.hidden = !thinkingContent;
    if (thinkingContent && wasHidden) thinkingPanel.open = true;
    thinkingPanel.querySelector(".thinking-content").textContent = thinkingContent;
    article.querySelector(".assistant-text").textContent = message.content;
    renderToolEvents(article.querySelector(".tool-events"), message.tools ?? []);
  }

  function setStreaming(streaming) {
    state.streaming = streaming;
    elements.send.classList.toggle("streaming", streaming);
    elements.send.disabled = !streaming && !elements.input.value.trim();
    elements.send.setAttribute("aria-label", streaming ? "停止生成" : "发送消息");
    elements.status.classList.toggle("generating", streaming);
    elements.status.querySelector(".status-label").textContent = streaming ? "Generating" : "Ready";
  }

  function updateSendButton() {
    elements.send.disabled = !state.streaming && !elements.input.value.trim();
  }

  function resizeInput() {
    elements.input.style.height = "auto";
    elements.input.style.height = `${Math.min(elements.input.scrollHeight, 190)}px`;
  }

  function scrollToBottom(smooth) {
    elements.conversation.scrollTo({
      top: elements.conversation.scrollHeight,
      behavior: smooth ? "smooth" : "auto"
    });
  }

  function createTitle(text) {
    const normalized = text.replace(/\s+/g, " ").trim();
    return normalized.length > 26 ? `${normalized.slice(0, 26)}…` : normalized;
  }

  async function readError(response) {
    try {
      const body = await response.json();
      return body.message || `HTTP ${response.status}`;
    } catch {
      return `HTTP ${response.status}`;
    }
  }

  function persist() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state.conversations));
  }

  function openSidebar() {
    elements.sidebar.classList.add("open");
    elements.sidebarOverlay.classList.add("open");
  }

  function closeSidebar() {
    elements.sidebar.classList.remove("open");
    elements.sidebarOverlay.classList.remove("open");
  }

  function showToast(message) {
    clearTimeout(state.toastTimer);
    elements.toast.textContent = message;
    elements.toast.classList.add("show");
    state.toastTimer = setTimeout(() => elements.toast.classList.remove("show"), 2600);
  }
})();
