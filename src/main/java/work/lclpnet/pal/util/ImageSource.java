package work.lclpnet.pal.util;

import java.io.IOException;
import java.io.InputStream;

public interface ImageSource {

    InputStream openStream() throws IOException;
}
