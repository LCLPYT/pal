package work.lclpnet.pal.service;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.minecraft.server.network.SpawnLocating;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Method;
import java.util.Arrays;

@Singleton
public class ReflectionService {

    private final Object resolverMutex = new Object();
    private volatile MappingResolver mappingResolver;
    private volatile Method SpawnLocating$findOverworldSpawn;

    @Inject
    public ReflectionService() {}

    public synchronized Method SpawnLocating$findOverworldSpawn() throws Throwable {
        if (SpawnLocating$findOverworldSpawn != null) {
            return SpawnLocating$findOverworldSpawn;
        }

        Class<?> cls = SpawnLocating.class;

        String methodName = mappedMethod(SpawnLocating.class, "method_29194",
                "(%sII)%s", ServerWorld.class, BlockPos.class);

        Method method = cls.getDeclaredMethod(methodName, ServerWorld.class, int.class, int.class);
        method.setAccessible(true);

        SpawnLocating$findOverworldSpawn = method;

        return SpawnLocating$findOverworldSpawn;
    }

    private MappingResolver mappingResolver() {
        if (mappingResolver != null) {
            return mappingResolver;
        }

        synchronized (resolverMutex) {
            if (mappingResolver != null) {
                return mappingResolver;
            }

            mappingResolver = FabricLoader.getInstance().getMappingResolver();
        }

        return mappingResolver;
    }

    private String mappedMethod(Class<?> clazz, String intermediaryMethod, String descriptor, Class<?>... classSubstitutes) {
        Object[] substitutes = Arrays.stream(classSubstitutes)
                .map(this::unmappedJniClass)
                .toArray(Object[]::new);

        String formatted = descriptor.formatted(substitutes);

        return mappingResolver().mapMethodName(
                "intermediary",
                unmappedClass(clazz),
                intermediaryMethod,
                formatted
        );
    }

    private String unmappedClass(Class<?> clazz) {
        return mappingResolver().unmapClassName("intermediary", clazz.getName());
    }

    private String unmappedJniClass(Class<?> clazz) {
        return jniClass(unmappedClass(clazz));
    }

    private String jniClass(String className) {
        return 'L' + className.replace('.', '/') + ';';
    }
}
