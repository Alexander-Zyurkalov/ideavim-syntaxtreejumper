package com.zyurkalov.ideavim.syntaxtreejumper.highlighting;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Configuration service for persistent PSI element highlighting.
 * Simple on/off toggle with persistent highlights.
 */
@Service
@State(
    name = "SyntaxTreeJumperHighlighting",
    storages = @Storage("syntaxTreeJumperHighlighting.xml")
)
public final class HighlightingConfig implements PersistentStateComponent<HighlightingConfig> {

    public boolean highlightingEnabled = false;
    public boolean showCurrentElement = true;
    public boolean showPreviousSibling = true;
    public boolean showNextSibling = true;
    public boolean showTooltips = false;


    public static HighlightingConfig getInstance() {
        return ApplicationManager.getApplication().getService(HighlightingConfig.class);
    }

    @Nullable
    @Override
    public HighlightingConfig getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull HighlightingConfig state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    /**
     * Toggles the highlighting feature on/off.
     */
    public void toggleHighlighting() {
        highlightingEnabled = !highlightingEnabled;
    }

    /**
     * Checks if highlighting is currently enabled.
     */
    public boolean isHighlightingEnabled() {
        return highlightingEnabled;
    }



}