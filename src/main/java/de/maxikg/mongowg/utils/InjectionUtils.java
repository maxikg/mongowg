package de.maxikg.mongowg.utils;

import com.google.common.base.Throwables;
import com.sk89q.worldguard.bukkit.RegionContainer;
import com.sk89q.worldguard.protection.managers.RegionContainerImpl;
import com.sk89q.worldguard.protection.managers.storage.RegionDriver;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Injection utilities primary for WorldGuard.
 */
public class InjectionUtils {

    private InjectionUtils() {
    }

    /**
     * Injects a new {@code RegionDriver} in the given {@code RegionContainer}.
     *
     * @param container The {@code RegionContainer} in which should be injected
     * @param driver The {@code RegionDriver} which should be injected
     * @throws RuntimeException Encapsulated {@link ReflectiveOperationException}
     */
    public static void injectRegionDriver(RegionContainer container, RegionDriver driver) {
        try {
            Field field = RegionContainer.class.getDeclaredField("container");
            field.setAccessible(true);
            field.set(container, new RegionContainerImpl(driver));
        } catch (ReflectiveOperationException e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * Calls {@code RegionContainer.callUnload()} via reflection.
     *
     * @param container The {@code RegionContainer} on which {@code callUnload()} should be called
     * @throws RuntimeException Encapsulated {@link ReflectiveOperationException}
     */
    public static void callUnload(RegionContainer container) {
        saveInvoke(container, "unload");
    }

    /**
     * Calls {@code RegionContainer.callLoadWorlds()} via reflection.
     *
     * @param container The {@code RegionContainer} on which {@code callLoadWorlds()} should be called
     * @throws RuntimeException Encapsulated {@link ReflectiveOperationException}
     */
    public static void callLoadWorlds(RegionContainer container) {
        saveInvoke(container, "loadWorlds");
    }

    private static void saveInvoke(Object object, String name) {
        try {
            Method method = RegionContainer.class.getDeclaredMethod(name);
            method.setAccessible(true);
            method.invoke(object);
        } catch (ReflectiveOperationException e) {
            throw Throwables.propagate(e);
        }
    }
}
