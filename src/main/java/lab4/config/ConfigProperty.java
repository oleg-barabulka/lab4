package lab4.config;

import lab4.SnakesProto;
import lombok.experimental.UtilityClass;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@UtilityClass
public final class ConfigProperty {
    private static final Logger logger = Logger.getLogger(ConfigProperty.class);
    private static final Properties properties;

    static {
        properties = new Properties();
        InputStream input = ClassLoader.getSystemResourceAsStream("config.properties");
        if (input != null) {
            try (input) {
                properties.load(input);
            }
            catch (IOException e) {
                logger.error(e.getLocalizedMessage());
            }
        }
    }

    public static SnakesProto.GameConfig getConfig() {
        SnakesProto.GameConfig.Builder builder = SnakesProto.GameConfig.newBuilder();
        builder.clear();
        builder.setHeight(getIntegerProperty(ConfigFieldNames.getFIELD_HEIGHT(), builder.getHeight()));
        builder.setWidth(getIntegerProperty(ConfigFieldNames.getFIELD_WIDTH(), builder.getWidth()));
        builder.setFoodStatic(getIntegerProperty(ConfigFieldNames.getFOOD_STATIC(), builder.getFoodStatic()));
        builder.setStateDelayMs(getIntegerProperty(ConfigFieldNames.getSTATE_DELAY_MS(), builder.getStateDelayMs()));
        return builder.build();
    }

    private static int getIntegerProperty(String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key));
        }
        catch (NumberFormatException exception) {
            return defaultValue;
        }
    }
}
