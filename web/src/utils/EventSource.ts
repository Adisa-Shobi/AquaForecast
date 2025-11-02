/**
 * EventSource wrapper that supports custom headers.
 *
 * Native EventSource doesn't support custom headers, so we use fetch with streaming.
 * This is necessary for sending Authorization headers with Firebase tokens.
 */

export interface EventSourceMessage {
  data: string;
  event?: string;
  id?: string;
}

export type EventSourceCallback = (message: EventSourceMessage) => void;

export class EventSourceWithHeaders {
  private abortController: AbortController | null = null;
  private url: string;
  private headers: Record<string, string>;
  private onMessageCallback: EventSourceCallback | null = null;
  private onOpenCallback: (() => void) | null = null;
  private onErrorCallback: ((error: any) => void) | null = null;

  constructor(url: string, headers: Record<string, string> = {}) {
    this.url = url;
    this.headers = headers;
  }

  connect() {
    this.abortController = new AbortController();

    fetch(this.url, {
      headers: {
        ...this.headers,
        Accept: 'text/event-stream',
      },
      signal: this.abortController.signal,
    })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }

        if (this.onOpenCallback) {
          this.onOpenCallback();
        }

        const reader = response.body?.getReader();
        const decoder = new TextDecoder();

        if (!reader) {
          throw new Error('Response body is not readable');
        }

        let buffer = '';

        while (true) {
          const { done, value } = await reader.read();

          if (done) {
            break;
          }

          buffer += decoder.decode(value, { stream: true });

          // Process complete messages (separated by \n\n)
          const messages = buffer.split('\n\n');
          buffer = messages.pop() || ''; // Keep incomplete message in buffer

          for (const message of messages) {
            if (message.trim() === '') continue;

            const parsedMessage = this.parseMessage(message);
            if (parsedMessage && this.onMessageCallback) {
              this.onMessageCallback(parsedMessage);
            }
          }
        }
      })
      .catch((error) => {
        if (error.name !== 'AbortError' && this.onErrorCallback) {
          this.onErrorCallback(error);
        }
      });
  }

  private parseMessage(raw: string): EventSourceMessage | null {
    const lines = raw.split('\n');
    const message: EventSourceMessage = { data: '' };

    for (const line of lines) {
      if (line.startsWith('data:')) {
        message.data = line.substring(5).trim();
      } else if (line.startsWith('event:')) {
        message.event = line.substring(6).trim();
      } else if (line.startsWith('id:')) {
        message.id = line.substring(3).trim();
      } else if (line.startsWith(':')) {
        // Comment/heartbeat - ignore
        continue;
      }
    }

    return message.data ? message : null;
  }

  set onmessage(callback: EventSourceCallback) {
    this.onMessageCallback = callback;
  }

  set onopen(callback: () => void) {
    this.onOpenCallback = callback;
  }

  set onerror(callback: (error: any) => void) {
    this.onErrorCallback = callback;
  }

  close() {
    if (this.abortController) {
      this.abortController.abort();
      this.abortController = null;
    }
  }
}
