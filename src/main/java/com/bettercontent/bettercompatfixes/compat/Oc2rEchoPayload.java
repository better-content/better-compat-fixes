package com.bettercontent.bettercompatfixes.compat;

import java.nio.ByteBuffer;

/** Receives the native method's already-owned copy, never the caller's buffer/storage. */
public final class Oc2rEchoPayload {
    private Oc2rEchoPayload() { }
    public static ByteBuffer captured(byte[] nativeCopy) {
        return ByteBuffer.wrap(nativeCopy).asReadOnlyBuffer();
    }
}
