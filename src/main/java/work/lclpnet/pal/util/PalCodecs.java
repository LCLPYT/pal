package work.lclpnet.pal.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.function.Function;

public class PalCodecs {

    public static final Codec<Double> POSITIVE_DOUBLE = minDouble(0.d);

    private static <N extends Number & Comparable<N>> Function<N, DataResult<N>> checkRange(final N minInclusive) {
        return value -> {
            if (value.compareTo(minInclusive) >= 0) {
                return DataResult.success(value);
            }

            return DataResult.error(() -> "Value " + value + " is less than the minimum: <" + minInclusive + ">");
        };
    }

    public static Codec<Double> minDouble(double min) {
        final Function<Double, DataResult<Double>> checker = checkRange(min);

        return Codec.DOUBLE.flatXmap(checker, checker);
    }
}
