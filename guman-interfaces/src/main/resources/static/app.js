(() => {
  "use strict";

  const STORAGE_KEY = "guman.conversations.v1";
  const USER_KEY = "guman.user-id.v1";
  const MAX_ATTACHMENT_COUNT = 4;
  const MAX_ATTACHMENT_SIZE = 5 * 1024 * 1024;
  const SUPPORTED_ATTACHMENT_EXTENSIONS = new Set([
    "png", "jpg", "jpeg", "webp", "pdf", "txt", "md", "markdown", "csv", "json",
    "xml", "yaml", "yml", "java", "kt", "js", "ts", "jsx", "tsx", "py", "go",
    "rs", "c", "h", "cpp", "hpp", "html", "css", "scss", "sql", "sh", "bash",
    "zsh", "properties", "toml", "ini", "log"
  ]);

  const elements = {
    composer: document.querySelector("#composer"),
    input: document.querySelector("#messageInput"),
    send: document.querySelector("#sendButton"),
    attachmentButton: document.querySelector("#attachmentButton"),
    attachmentInput: document.querySelector("#attachmentInput"),
    attachmentPreviewList: document.querySelector("#attachmentPreviewList"),
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
    attachments: [],
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
        if (!state.streaming && hasDraftContent()) {
          elements.composer.requestSubmit();
        }
      }
    });
    elements.newChat.addEventListener("click", () => createConversation(true));
    elements.attachmentButton.addEventListener("click", () => elements.attachmentInput.click());
    elements.attachmentInput.addEventListener("change", handleAttachmentSelection);
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
      if (message.content) {
        const text = document.createElement("div");
        text.textContent = message.content;
        content.append(text);
      }
      renderMessageAttachments(content, message.attachments ?? []);
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
    thinkingContent.className = "thinking-content markdown-body";
    renderMarkdown(thinkingContent, message.thinking ?? "");
    thinking.append(thinkingSummary, thinkingContent);
    const tools = document.createElement("div");
    tools.className = "tool-events";
    renderToolEvents(tools, message.tools ?? []);
    const text = document.createElement("div");
    text.className = "assistant-text markdown-body";
    renderMarkdown(text, message.content);
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

  function handleAttachmentSelection() {
    const selectedFiles = Array.from(elements.attachmentInput.files ?? []);
    elements.attachmentInput.value = "";
    for (const file of selectedFiles) {
      if (state.attachments.length >= MAX_ATTACHMENT_COUNT) {
        showToast(`一次最多添加 ${MAX_ATTACHMENT_COUNT} 个附件`);
        break;
      }
      if (!isSupportedAttachment(file)) {
        showToast(`不支持 ${file.name} 的文件类型`);
        continue;
      }
      if (!file.size) {
        showToast(`${file.name} 是空文件`);
        continue;
      }
      if (file.size > MAX_ATTACHMENT_SIZE) {
        showToast(`${file.name} 超过 5 MB`);
        continue;
      }
      const duplicate = state.attachments.some(item =>
        item.name === file.name && item.size === file.size && item.lastModified === file.lastModified);
      if (!duplicate) state.attachments.push(file);
    }
    renderAttachmentPreviews();
    updateSendButton();
  }

  function isSupportedAttachment(file) {
    const extension = file.name.includes(".") ? file.name.split(".").pop().toLowerCase() : "";
    return SUPPORTED_ATTACHMENT_EXTENSIONS.has(extension);
  }

  function renderAttachmentPreviews() {
    elements.attachmentPreviewList.replaceChildren();
    state.attachments.forEach((file, index) => {
      const item = document.createElement("div");
      item.className = "draft-attachment";
      const type = document.createElement("span");
      type.className = "attachment-type";
      type.textContent = attachmentTypeLabel(file.name);
      const details = document.createElement("span");
      details.className = "attachment-details";
      const name = document.createElement("strong");
      name.textContent = file.name;
      name.title = file.name;
      const size = document.createElement("small");
      size.textContent = formatFileSize(file.size);
      details.append(name, size);
      const remove = document.createElement("button");
      remove.type = "button";
      remove.className = "attachment-remove";
      remove.textContent = "×";
      remove.setAttribute("aria-label", `移除附件 ${file.name}`);
      remove.addEventListener("click", () => {
        state.attachments.splice(index, 1);
        renderAttachmentPreviews();
        updateSendButton();
      });
      item.append(type, details, remove);
      elements.attachmentPreviewList.append(item);
    });
    elements.attachmentButton.classList.toggle("has-files", state.attachments.length > 0);
    elements.attachmentButton.setAttribute(
      "aria-label",
      state.attachments.length ? `添加附件，已选择 ${state.attachments.length} 个` : "添加附件"
    );
  }

  function renderMessageAttachments(container, attachments) {
    if (!attachments.length) return;
    const list = document.createElement("div");
    list.className = "message-attachment-list";
    attachments.forEach(attachment => {
      const item = document.createElement("span");
      item.className = "message-attachment";
      item.textContent = `${attachmentTypeLabel(attachment.name)} ${attachment.name}`;
      item.title = `${attachment.name} · ${formatFileSize(attachment.size)}`;
      list.append(item);
    });
    container.append(list);
  }

  function attachmentTypeLabel(filename) {
    const extension = filename.includes(".") ? filename.split(".").pop().toUpperCase() : "FILE";
    if (["PNG", "JPG", "JPEG", "WEBP"].includes(extension)) return "IMG";
    return extension.length <= 5 ? extension : "FILE";
  }

  function formatFileSize(size) {
    if (size < 1024) return `${size} B`;
    if (size < 1024 * 1024) return `${Math.ceil(size / 1024)} KB`;
    return `${(size / (1024 * 1024)).toFixed(1)} MB`;
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (state.streaming) return;
    const prompt = elements.input.value.trim();
    if (!prompt && !state.attachments.length) return;

    const conversation = activeConversation() ?? createConversation(false);
    const outgoingAttachments = [...state.attachments];
    const userMessage = {
      id: crypto.randomUUID(),
      role: "user",
      content: prompt,
      attachments: outgoingAttachments.map(file => ({
        name: file.name,
        mediaType: file.type,
        size: file.size
      }))
    };
    const assistantMessage = {
      id: crypto.randomUUID(),
      role: "assistant",
      content: "",
      thinking: "",
      tools: []
    };

    conversation.messages.push(userMessage, assistantMessage);
    if (conversation.title === "新对话") {
      conversation.title = createTitle(prompt || outgoingAttachments[0].name);
    }
    conversation.updatedAt = Date.now();
    elements.input.value = "";
    state.attachments = [];
    renderAttachmentPreviews();
    resizeInput();
    persist();
    renderAll();
    setStreaming(true);

    state.abortController = new AbortController();
    try {
      const request = {
        message: prompt || "请分析附件内容，并说明关键信息。",
        userId: state.userId,
        sessionId: conversation.id
      };
      const headers = { "Accept": "text/event-stream" };
      let body;
      if (outgoingAttachments.length) {
        const formData = new FormData();
        formData.append("request", new Blob([JSON.stringify(request)], { type: "application/json" }));
        outgoingAttachments.forEach(file => formData.append("files", file, file.name));
        body = formData;
      } else {
        headers["Content-Type"] = "application/json";
        body = JSON.stringify(request);
      }
      const response = await fetch("/api/chat/stream", {
        method: "POST",
        headers,
        body,
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
      message.content ||= data.content || "生成失败，请稍后重试。";
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
    const thinkingContainer = thinkingPanel.querySelector(".thinking-content");
    renderMarkdown(thinkingContainer, thinkingContent);
    if (thinkingPanel.open) thinkingContainer.scrollTop = thinkingContainer.scrollHeight;
    renderMarkdown(article.querySelector(".assistant-text"), message.content);
    renderToolEvents(article.querySelector(".tool-events"), message.tools ?? []);
  }

  function renderMarkdown(container, markdown) {
    container.replaceChildren();
    if (!markdown) return;

    const lines = markdown.replaceAll("\r\n", "\n").split("\n");
    let index = 0;
    while (index < lines.length) {
      if (!lines[index].trim()) {
        index += 1;
        continue;
      }

      const fence = lines[index].match(/^\s*```([^`]*)$/);
      if (fence) {
        const codeLines = [];
        index += 1;
        while (index < lines.length && !/^\s*```\s*$/.test(lines[index])) {
          codeLines.push(lines[index]);
          index += 1;
        }
        if (index < lines.length) index += 1;
        const pre = document.createElement("pre");
        const code = document.createElement("code");
        if (fence[1].trim()) code.dataset.language = fence[1].trim();
        code.textContent = codeLines.join("\n");
        pre.append(code);
        container.append(pre);
        continue;
      }

      if (isTableStart(lines, index)) {
        const wrapper = document.createElement("div");
        wrapper.className = "markdown-table-wrap";
        const table = document.createElement("table");
        const head = document.createElement("thead");
        const headRow = document.createElement("tr");
        splitTableRow(lines[index]).forEach(cell => {
          const header = document.createElement("th");
          appendInlineMarkdown(header, cell);
          headRow.append(header);
        });
        head.append(headRow);
        table.append(head);
        index += 2;
        const body = document.createElement("tbody");
        while (index < lines.length && lines[index].includes("|") && lines[index].trim()) {
          const row = document.createElement("tr");
          splitTableRow(lines[index]).forEach(cell => {
            const data = document.createElement("td");
            appendInlineMarkdown(data, cell);
            row.append(data);
          });
          body.append(row);
          index += 1;
        }
        if (body.childElementCount) table.append(body);
        wrapper.append(table);
        container.append(wrapper);
        continue;
      }

      const heading = lines[index].match(/^(#{1,6})\s+(.+)$/);
      if (heading) {
        const element = document.createElement(`h${heading[1].length}`);
        appendInlineMarkdown(element, heading[2]);
        container.append(element);
        index += 1;
        continue;
      }

      if (/^\s*([-*_])(?:\s*\1){2,}\s*$/.test(lines[index])) {
        container.append(document.createElement("hr"));
        index += 1;
        continue;
      }

      if (/^\s*>\s?/.test(lines[index])) {
        const quoteLines = [];
        while (index < lines.length && /^\s*>\s?/.test(lines[index])) {
          quoteLines.push(lines[index].replace(/^\s*>\s?/, ""));
          index += 1;
        }
        const quote = document.createElement("blockquote");
        renderMarkdown(quote, quoteLines.join("\n"));
        container.append(quote);
        continue;
      }

      const unorderedItem = lines[index].match(/^\s*[-*+]\s+(.+)$/);
      const orderedItem = lines[index].match(/^\s*\d+[.)]\s+(.+)$/);
      if (unorderedItem || orderedItem) {
        const ordered = Boolean(orderedItem);
        const list = document.createElement(ordered ? "ol" : "ul");
        const itemPattern = ordered ? /^\s*\d+[.)]\s+(.+)$/ : /^\s*[-*+]\s+(.+)$/;
        let item = lines[index].match(itemPattern);
        while (item) {
          const listItem = document.createElement("li");
          appendInlineMarkdown(listItem, item[1]);
          list.append(listItem);
          index += 1;
          item = index < lines.length ? lines[index].match(itemPattern) : null;
        }
        container.append(list);
        continue;
      }

      const paragraphLines = [lines[index]];
      index += 1;
      while (index < lines.length && lines[index].trim() && !startsMarkdownBlock(lines, index)) {
        paragraphLines.push(lines[index]);
        index += 1;
      }
      const paragraph = document.createElement("p");
      appendInlineMarkdown(paragraph, paragraphLines.join(" "));
      container.append(paragraph);
    }
  }

  function startsMarkdownBlock(lines, index) {
    const line = lines[index];
    return /^\s*```/.test(line)
      || /^(#{1,6})\s+/.test(line)
      || /^\s*>\s?/.test(line)
      || /^\s*[-*+]\s+/.test(line)
      || /^\s*\d+[.)]\s+/.test(line)
      || /^\s*([-*_])(?:\s*\1){2,}\s*$/.test(line)
      || isTableStart(lines, index);
  }

  function isTableStart(lines, index) {
    return index + 1 < lines.length
      && lines[index].includes("|")
      && isTableSeparator(lines[index + 1]);
  }

  function isTableSeparator(line) {
    const cells = splitTableRow(line);
    return cells.length > 0 && cells.every(cell => /^:?-{3,}:?$/.test(cell));
  }

  function splitTableRow(line) {
    return line.trim().replace(/^\|/, "").replace(/\|$/, "").split("|").map(cell => cell.trim());
  }

  function appendInlineMarkdown(parent, text) {
    const tokenPattern = /(`[^`\n]+`|\*\*[^*\n]+\*\*|__[^_\n]+__|\[[^\]\n]+\]\([^\s)]+\)|\*[^*\n]+\*|_[^_\n]+_)/g;
    let cursor = 0;
    for (const match of text.matchAll(tokenPattern)) {
      parent.append(document.createTextNode(text.slice(cursor, match.index)));
      const token = match[0];
      if (token.startsWith("`")) {
        const code = document.createElement("code");
        code.textContent = token.slice(1, -1);
        parent.append(code);
      } else if (token.startsWith("**") || token.startsWith("__")) {
        const strong = document.createElement("strong");
        appendInlineMarkdown(strong, token.slice(2, -2));
        parent.append(strong);
      } else if (token.startsWith("[")) {
        const boundary = token.lastIndexOf("](");
        const label = token.slice(1, boundary);
        const href = token.slice(boundary + 2, -1);
        if (isSafeLink(href)) {
          const link = document.createElement("a");
          link.href = href;
          link.rel = "noopener noreferrer";
          appendInlineMarkdown(link, label);
          parent.append(link);
        } else {
          parent.append(document.createTextNode(label));
        }
      } else {
        const emphasis = document.createElement("em");
        appendInlineMarkdown(emphasis, token.slice(1, -1));
        parent.append(emphasis);
      }
      cursor = match.index + token.length;
    }
    parent.append(document.createTextNode(text.slice(cursor)));
  }

  function isSafeLink(href) {
    try {
      const url = new URL(href, document.baseURI);
      return url.protocol === "http:" || url.protocol === "https:" || url.protocol === "mailto:";
    } catch {
      return false;
    }
  }

  function setStreaming(streaming) {
    state.streaming = streaming;
    elements.attachmentButton.disabled = streaming;
    elements.send.classList.toggle("streaming", streaming);
    elements.send.disabled = !streaming && !hasDraftContent();
    elements.send.setAttribute("aria-label", streaming ? "停止生成" : "发送消息");
    elements.status.classList.toggle("generating", streaming);
    elements.status.querySelector(".status-label").textContent = streaming ? "Generating" : "Ready";
  }

  function updateSendButton() {
    elements.send.disabled = !state.streaming && !hasDraftContent();
  }

  function hasDraftContent() {
    return Boolean(elements.input.value.trim() || state.attachments.length);
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
