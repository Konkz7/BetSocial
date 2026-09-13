/**
 * TextEncoder and TextDecoder, which React Native does not provide.
 *
 * This is why the chat socket never opened.
 *
 * stompjs builds a Parser as the first statement of StompHandler.start(), and
 * that constructor does `new TextEncoder()`. Hermes has no such global and
 * React Native 0.76 does not define one, so the call threw - before any of
 * onmessage, onclose, onerror or onopen had been attached to the socket.
 *
 * The failure was invisible from either side. The WebSocket had already been
 * constructed a line earlier, so the server saw a completed handshake and held
 * the session open; the client had no open handler, so it never sent CONNECT and
 * never logged anything. And because start() is reached from the async
 * _connect(), the exception became a rejected promise nobody was waiting on,
 * which React Native discards without a word. The socket appeared to hang.
 *
 * Written here rather than installed: this is the whole of UTF-8, the encoding
 * is fixed by standard so there is nothing to keep up with, and it is verified
 * byte-for-byte against the platform implementation in TextEncodingTest.
 *
 * Only what the standard requires of UTF-8 encoding and decoding - no streaming,
 * no other encodings, no `encodeInto`. stompjs uses `encode` and `decode` and
 * nothing else.
 */

/** UTF-8 bytes for a string, matching TextEncoder.prototype.encode. */
function encodeUtf8(input) {
  const text = String(input === undefined ? '' : input);
  const bytes = [];

  for (let i = 0; i < text.length; i++) {
    let code = text.charCodeAt(i);

    // A high surrogate followed by a low one is a single character above the
    // basic plane - an emoji, most often. Combined here, or the pair would be
    // encoded as two invalid characters.
    if (code >= 0xd800 && code <= 0xdbff && i + 1 < text.length) {
      const next = text.charCodeAt(i + 1);
      if (next >= 0xdc00 && next <= 0xdfff) {
        code = 0x10000 + ((code - 0xd800) << 10) + (next - 0xdc00);
        i++;
      }
    }

    // A surrogate that was not part of a pair cannot be encoded. The standard
    // says to replace it rather than fail, which is what the platform does.
    if (code >= 0xd800 && code <= 0xdfff) {
      code = 0xfffd;
    }

    if (code < 0x80) {
      bytes.push(code);
    } else if (code < 0x800) {
      bytes.push(0xc0 | (code >> 6), 0x80 | (code & 0x3f));
    } else if (code < 0x10000) {
      bytes.push(0xe0 | (code >> 12), 0x80 | ((code >> 6) & 0x3f), 0x80 | (code & 0x3f));
    } else {
      bytes.push(
        0xf0 | (code >> 18),
        0x80 | ((code >> 12) & 0x3f),
        0x80 | ((code >> 6) & 0x3f),
        0x80 | (code & 0x3f),
      );
    }
  }

  return new Uint8Array(bytes);
}

/** A string from UTF-8 bytes, matching TextDecoder.prototype.decode. */
function decodeUtf8(input) {
  if (input === undefined || input === null) {
    return '';
  }

  const bytes = input instanceof Uint8Array
    ? input
    : new Uint8Array(input.buffer ? input.buffer : input);

  let result = '';

  for (let i = 0; i < bytes.length;) {
    const first = bytes[i];

    if (first < 0x80) {
      result += String.fromCharCode(first);
      i++;
      continue;
    }

    // The permitted range of the *second* byte depends on the first, and that is
    // what rules out overlong encodings and the surrogate block without having
    // to decode them first and check afterwards. C0, C1 and F5-FF cannot begin
    // any sequence at all.
    let length;
    let lowerSecond = 0x80;
    let upperSecond = 0xbf;
    let code;

    if (first >= 0xc2 && first <= 0xdf) {
      length = 2;
      code = first & 0x1f;
    } else if (first >= 0xe0 && first <= 0xef) {
      length = 3;
      code = first & 0x0f;
      if (first === 0xe0) { lowerSecond = 0xa0; }        // no overlong three-byte
      if (first === 0xed) { upperSecond = 0x9f; }        // no surrogates
    } else if (first >= 0xf0 && first <= 0xf4) {
      length = 4;
      code = first & 0x07;
      if (first === 0xf0) { lowerSecond = 0x90; }        // no overlong four-byte
      if (first === 0xf4) { upperSecond = 0x8f; }        // nothing past U+10FFFF
    } else {
      result += '�';
      i++;
      continue;
    }

    let consumed = 1;
    let failed = false;

    for (let j = 1; j < length; j++) {
      if (i + j >= bytes.length) {
        // Ran out mid-sequence. One replacement for the whole partial
        // character, and nothing left to resume from.
        result += '�';
        return result;
      }

      const byte = bytes[i + j];
      const lower = j === 1 ? lowerSecond : 0x80;
      const upper = j === 1 ? upperSecond : 0xbf;

      if (byte < lower || byte > upper) {
        // One replacement for the part that was valid, then carry on *at* the
        // offending byte rather than past it - it may begin a good character,
        // and if it does not it earns a replacement of its own. This is what
        // makes the count match the platform's on malformed input.
        failed = true;
        break;
      }

      code = (code << 6) | (byte & 0x3f);
      consumed++;
    }

    if (failed) {
      result += '�';
      i += consumed;
      continue;
    }

    if (code > 0xffff) {
      const above = code - 0x10000;
      result += String.fromCharCode(0xd800 + (above >> 10), 0xdc00 + (above & 0x3ff));
    } else {
      result += String.fromCharCode(code);
    }

    i += length;
  }

  return result;
}

export class PolyfilledTextEncoder {
  get encoding() {
    return 'utf-8';
  }

  encode(input) {
    return encodeUtf8(input);
  }
}

export class PolyfilledTextDecoder {
  constructor(label = 'utf-8') {
    this._label = label;
  }

  get encoding() {
    return 'utf-8';
  }

  decode(input) {
    return decodeUtf8(input);
  }
}

/**
 * Installs both, if they are missing.
 *
 * Guarded so that a React Native or Hermes release that starts providing them
 * takes precedence - the platform's own will always be faster and more complete
 * than this.
 */
export function installTextEncoding() {
  const target = typeof globalThis !== 'undefined' ? globalThis : global;
  let installed = false;

  if (typeof target.TextEncoder === 'undefined') {
    target.TextEncoder = PolyfilledTextEncoder;
    installed = true;
  }

  if (typeof target.TextDecoder === 'undefined') {
    target.TextDecoder = PolyfilledTextDecoder;
    installed = true;
  }

  if (installed) {
    console.log('TextEncoder/TextDecoder polyfilled - this runtime has none, '
      + 'and stompjs needs them to parse STOMP frames.');
  }

  return installed;
}

installTextEncoding();
