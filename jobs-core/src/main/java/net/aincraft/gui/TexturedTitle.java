package net.aincraft.gui;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Utility class for creating textured inventory titles using custom fonts.
 * Pattern from aincraft-perks: [space + texture + space] with custom font, then [text] with default font.
 */
public final class TexturedTitle {

    /**
     * The custom font key for ModularJobs interface textures.
     * Uses plugin namespace like aincraft-perks (modularjobs:interface).
     */
    public static final Key INTERFACE_FONT = Key.key("modularjobs", "interface");

    /**
     * The default Minecraft font key.
     */
    public static final Key DEFAULT_FONT = Key.key("minecraft", "default");

    // ============================================================
    // GUI Background Textures
    // ============================================================

    /** Upgrade tree background texture (\uE000) */
    public static final char UPGRADE_TREE_BG = '\uE000';

    // ============================================================
    // Space Advance Characters (from font space provider)
    // ============================================================

    /** Shifts left by 8 pixels */
    public static final String SPACE_BACK_8 = "\uF001";
    /** Shifts left by 248 pixels */
    public static final String SPACE_BACK_248 = "\uF002";
    /** Shifts left by 170 pixels */
    public static final String SPACE_BACK_170 = "\uF003";
    /** Shifts left by 1 pixel - fine tuning */
    public static final String SPACE_BACK_1 = "\uF004";
    /** Shifts left by 16 pixels */
    public static final String SPACE_BACK_16 = "\uF005";
    /** Shifts left by 32 pixels */
    public static final String SPACE_BACK_32 = "\uF006";
    /** Shifts left by 64 pixels */
    public static final String SPACE_BACK_64 = "\uF007";
    /** Shifts left by 128 pixels */
    public static final String SPACE_BACK_128 = "\uF008";
    /** Shifts left by 48 pixels - center 256px texture on chest UI */
    public static final String SPACE_BACK_48 = "\uF010";
    /** Shifts left by 200 pixels - position text at chest title location */
    public static final String SPACE_BACK_200 = "\uF011";

    private TexturedTitle() {}

    /**
     * Creates a textured title with background and overlay text.
     * Uses custom font for texture/spacing, default font for text.
     *
     * @param textureChar the texture character
     * @param text the overlay text
     * @return a Component suitable for inventory titles
     */
    public static Component create(char textureChar, String text) {
        return Component.empty()
            .append(Component.text(SPACE_BACK_48 + textureChar + SPACE_BACK_200)
                .font(INTERFACE_FONT)
                .color(NamedTextColor.WHITE))
            .append(Component.text(text)
                .font(DEFAULT_FONT));
    }

    /**
     * Creates a builder for more complex textured titles with overlay text.
     *
     * @return a new TexturedTitleBuilder
     */
    public static TexturedTitleBuilder builder() {
        return new TexturedTitleBuilder();
    }

    /**
     * Builder for creating textured titles with customizable options.
     */
    public static final class TexturedTitleBuilder {
        private char textureChar = UPGRADE_TREE_BG;
        private String overlayText = null;
        private NamedTextColor textColor = NamedTextColor.WHITE;

        private TexturedTitleBuilder() {}

        public TexturedTitleBuilder texture(char textureChar) {
            this.textureChar = textureChar;
            return this;
        }

        public TexturedTitleBuilder text(String text) {
            this.overlayText = text;
            return this;
        }

        public TexturedTitleBuilder text(String text, NamedTextColor color) {
            this.overlayText = text;
            this.textColor = color;
            return this;
        }

        // Keep these methods for API compatibility but they're now no-ops
        public TexturedTitleBuilder textOffset(int pixels) {
            return this;
        }

        public TexturedTitleBuilder textureOffset(int pixels) {
            return this;
        }

        public Component build() {
            var builder = Component.empty();

            // Add texture with custom font (pre-shift + texture + post-shift)
            // Pre-shift -48 centers 256px texture on chest UI
            // Post-shift -200 positions cursor at chest title location
            builder = builder.append(Component.text(SPACE_BACK_48 + textureChar + SPACE_BACK_200)
                .font(INTERFACE_FONT)
                .color(NamedTextColor.WHITE));

            // Add overlay text with DEFAULT font
            if (overlayText != null && !overlayText.isEmpty()) {
                builder = builder.append(Component.text(overlayText, textColor)
                    .font(DEFAULT_FONT));
            }

            return builder;
        }
    }
}
