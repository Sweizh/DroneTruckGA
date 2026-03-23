package ga;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;

/**
 * 类名：ConfigLoader
 * 功能：配置加载器
 * 说明：使用Jackson库从JSON文件加载配置参数
 *      提供静态方法加载配置文件，无需创建实例
 */
public class ConfigLoader {

    /**
     * 从指定路径的JSON文件加载配置
     * @param configPath JSON配置文件路径
     * @return Config对象，包含所有配置参数
     * @throws RuntimeException 如果文件加载失败，抛出异常
     */
    public static Config load(String configPath) {
        try {
            // 创建Jackson的对象映射器
            ObjectMapper mapper = new ObjectMapper();
            // 将JSON文件映射为Config对象
            return mapper.readValue(new File(configPath), Config.class);
        } catch (Exception e) {
            // 加载失败时抛出运行时异常
            throw new RuntimeException("加载配置文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从默认路径config.json加载配置
     * @return Config对象
     */
    public static Config load() {
        return load("config.json");
    }
}
