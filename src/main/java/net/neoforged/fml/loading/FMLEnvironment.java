package net.neoforged.fml.loading;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

public final class FMLEnvironment {
    public static final Dist dist = FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT
            ? Dist.CLIENT
            : Dist.DEDICATED_SERVER;

    private FMLEnvironment() {
    }

    public static Dist getDist() {
        return dist;
    }

    public enum Dist {
        CLIENT,
        DEDICATED_SERVER;

        public boolean isClient() {
            return this == CLIENT;
        }
    }
}
