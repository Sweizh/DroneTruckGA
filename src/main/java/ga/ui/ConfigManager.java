package ga.ui;

import ga.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;

/**
 * 类名：ConfigManager
 * 功能：配置管理器
 * 说明：负责从文件加载配置、保存配置到文件
 */
public class ConfigManager {
    private static final String DEFAULT_CONFIG_PATH = "config.json";
    private final ObjectMapper mapper;

    /**
     * 构造函数：创建配置管理器
     */
    public ConfigManager() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * 加载配置文件
     * @param path 配置文件路径
     * @return Config对象
     * @throws IOException 如果文件读取失败
     */
    public Config loadConfig(String path) throws IOException {
        return mapper.readValue(new File(path), Config.class);
    }

    /**
     * 加载默认配置文件
     * @return Config对象
     * @throws IOException 如果文件读取失败
     */
    public Config loadDefaultConfig() throws IOException {
        return loadConfig(DEFAULT_CONFIG_PATH);
    }

    /**
     * 保存配置到文件
     * @param config 配置对象
     * @param path 文件路径
     * @throws IOException 如果文件写入失败
     */
    public void saveConfig(Config config, String path) throws IOException {
        mapper.writeValue(new File(path), config);
    }

    /**
     * 保存配置到默认文件
     * @param config 配置对象
     * @throws IOException 如果文件写入失败
     */
    public void saveDefaultConfig(Config config) throws IOException {
        saveConfig(config, DEFAULT_CONFIG_PATH);
    }

    /**
     * 获取默认配置文件路径
     * @return 默认配置文件路径
     */
    public String getDefaultConfigPath() {
        return DEFAULT_CONFIG_PATH;
    }
}
