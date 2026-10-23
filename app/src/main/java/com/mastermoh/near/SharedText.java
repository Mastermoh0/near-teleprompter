package com.mastermoh.near;

/** Shared content stays plain text and is reviewed before it becomes a script. */
final class SharedText {
    static String normalize(CharSequence value) {
        if (value == null) return "";
        return value.toString().replace("\r\n", "\n").replace('\r', '\n').replace("\u0000", "");
    }
    static boolean valid(String value) {
        return value.length() <= ScriptStore.MAX_CHARACTERS && ScrollSession.wordCount(value) > 0;
    }
    static String title(CharSequence subject) {
        String value = normalize(subject).replace('\n', ' ').trim();
        int end = Math.min(80, value.length());
        if (end > 0 && Character.isHighSurrogate(value.charAt(end-1))) end--;
        return value.substring(0, end);
    }
}
