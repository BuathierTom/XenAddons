package xenus.xenaddons.config;

public final class VisualWordsRenderState {
    private static final ThreadLocal<Integer> CHAT_RENDER_DEPTH = ThreadLocal.withInitial(() -> 0);

    private VisualWordsRenderState() {}

    public static void pushChatRender() {
        CHAT_RENDER_DEPTH.set(CHAT_RENDER_DEPTH.get() + 1);
    }

    public static void popChatRender() {
        int depth = CHAT_RENDER_DEPTH.get();
        if (depth <= 1) {
            CHAT_RENDER_DEPTH.remove();
            return;
        }
        CHAT_RENDER_DEPTH.set(depth - 1);
    }

    public static boolean isChatRenderActive() {
        return CHAT_RENDER_DEPTH.get() > 0;
    }
}
