package com.fiv.fiverkas_weapons.util;

import net.minecraft.commands.CommandSourceStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Compatibility helper for permission checks across NeoForge versions.
 */
public final class PermissionUtil {
    private static final int DEFAULT_PERMISSION_LEVEL = 2;

    private PermissionUtil() {
    }

    public static boolean hasGamemasterPermission(CommandSourceStack source) {
        if (source == null) {
            return false;
        }

        // NeoForge 1.21.11+ style: source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)
        try {
            Method permissionsMethod = source.getClass().getMethod("permissions");
            Object permissions = permissionsMethod.invoke(source);
            if (permissions != null) {
                Class<?> permissionsClass = Class.forName("net.neoforged.neoforge.server.permission.Permissions");
                Field gmField = permissionsClass.getField("COMMANDS_GAMEMASTER");
                Object gmNode = gmField.get(null);
                Method hasPermission = permissions.getClass().getMethod("hasPermission", gmNode.getClass());
                Object result = hasPermission.invoke(permissions, gmNode);
                if (result instanceof Boolean) {
                    return (Boolean) result;
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through to legacy checks.
        }

        // Legacy fallback: source.hasPermission(int) or source.hasPermissionLevel(int)
        try {
            Method hasPermission = source.getClass().getMethod("hasPermission", int.class);
            Object result = hasPermission.invoke(source, DEFAULT_PERMISSION_LEVEL);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            Method hasPermissionLevel = source.getClass().getMethod("hasPermissionLevel", int.class);
            Object result = hasPermissionLevel.invoke(source, DEFAULT_PERMISSION_LEVEL);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return false;
    }
}
