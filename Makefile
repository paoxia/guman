SHELL := /bin/bash

OLLAMA_HOST ?= 127.0.0.1:11434
OLLAMA_BASE_URL ?= http://$(OLLAMA_HOST)
OLLAMA_MODEL ?= qwen3.5:9b
OLLAMA_THINKING_ENABLED ?= true
GUMAN_ACTIVE_MODEL ?= ollama-qwen
GUMAN_JAR := guman-bootstrap/target/guman-bootstrap-0.1.0-SNAPSHOT.jar
OLLAMA_LOG := /tmp/guman-ollama.log

.PHONY: help ollama-install ollama-start ollama-pull ollama-run ollama-setup package run start

help:
	@echo "make ollama-setup  安装并启动 Ollama，然后拉取 $(OLLAMA_MODEL)"
	@echo "make ollama-run    只运行 $(OLLAMA_MODEL) 的 Ollama 交互会话"
	@echo "make run           构建并使用 $(GUMAN_ACTIVE_MODEL) 配置启动 Guman"
	@echo "make start         完成本地 Ollama 准备并启动 Guman"

ollama-install:
	@if command -v ollama >/dev/null 2>&1; then \
		echo "Ollama 已安装: $$(ollama --version)"; \
	else \
		command -v curl >/dev/null 2>&1 || { echo "安装 Ollama 需要 curl" >&2; exit 1; }; \
		curl -fsSL https://ollama.com/install.sh | sh; \
	fi

ollama-start: ollama-install
	@if curl -fsS "$(OLLAMA_BASE_URL)/api/tags" >/dev/null 2>&1; then \
		echo "Ollama 已运行: $(OLLAMA_BASE_URL)"; \
	else \
		echo "正在启动 Ollama，日志: $(OLLAMA_LOG)"; \
		OLLAMA_HOST="$(OLLAMA_HOST)" nohup ollama serve >"$(OLLAMA_LOG)" 2>&1 & \
		for attempt in {1..30}; do \
			if curl -fsS "$(OLLAMA_BASE_URL)/api/tags" >/dev/null 2>&1; then \
				echo "Ollama 已启动"; \
				exit 0; \
			fi; \
			sleep 1; \
		done; \
		echo "Ollama 启动超时，请检查 $(OLLAMA_LOG)" >&2; \
		exit 1; \
	fi

ollama-pull: ollama-start
	OLLAMA_HOST="$(OLLAMA_HOST)" ollama pull "$(OLLAMA_MODEL)"

ollama-run: ollama-pull
	OLLAMA_HOST="$(OLLAMA_HOST)" ollama run "$(OLLAMA_MODEL)"

ollama-setup: ollama-pull

package:
	mvn -DskipTests package

run: package
	GUMAN_ACTIVE_MODEL="$(GUMAN_ACTIVE_MODEL)" \
	OLLAMA_MODEL="$(OLLAMA_MODEL)" \
	OLLAMA_BASE_URL="$(OLLAMA_BASE_URL)" \
	OLLAMA_THINKING_ENABLED="$(OLLAMA_THINKING_ENABLED)" \
	java -jar "$(GUMAN_JAR)"

start: ollama-pull
	$(MAKE) run GUMAN_ACTIVE_MODEL=ollama-qwen OLLAMA_MODEL="$(OLLAMA_MODEL)" OLLAMA_HOST="$(OLLAMA_HOST)"
