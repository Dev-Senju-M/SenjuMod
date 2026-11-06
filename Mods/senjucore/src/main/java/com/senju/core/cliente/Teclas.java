package com.senju.core.cliente;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** Teclas del panel. Solo cliente. */
public final class Teclas {
    public static final String CATEGORIA = "key.categories.senjucore";

    public static final KeyMapping MOSTRAR = new KeyMapping("key.senjucore.mostrar",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORIA);
    public static final KeyMapping POSICION = new KeyMapping("key.senjucore.posicion",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORIA);

    private Teclas() {}
}
