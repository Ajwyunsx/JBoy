#include <GLES2/gl2.h>
#include <EGL/egl.h>
#include <android/log.h>
#include <cstdint>
#include <cstring>

#define LOG_TAG "JBOY_Video"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// GB屏幕分辨率
const int GB_SCREEN_WIDTH = 160;
const int GB_SCREEN_HEIGHT = 144;

// 简单的顶点着色器
const char* vertexShaderSource = R"(
    attribute vec2 a_position;
    attribute vec2 a_texCoord;
    varying vec2 v_texCoord;
    void main() {
        gl_Position = vec4(a_position, 0.0, 1.0);
        v_texCoord = a_texCoord;
    }
)";

// 简单的片段着色器
const char* fragmentShaderSource = R"(
    precision mediump float;
    varying vec2 v_texCoord;
    uniform sampler2D u_texture;
    void main() {
        gl_FragColor = texture2D(u_texture, v_texCoord);
    }
)";

class VideoRenderer {
public:
    VideoRenderer();
    ~VideoRenderer();
    
    bool initialize();
    void shutdown();
    void renderFrame(const uint8_t* frameBuffer);
    void updateScreenSize(int width, int height);
    
private:
    bool compileShader(GLuint& shader, GLenum type, const char* source);
    bool createProgram();
    
    GLuint m_program = 0;
    GLuint m_texture = 0;
    GLuint m_vertexBuffer = 0;
    GLint m_positionLoc = -1;
    GLint m_texCoordLoc = -1;
    GLint m_textureLoc = -1;
    
    int m_screenWidth = 0;
    int m_screenHeight = 0;
    
    // RGB565帧缓冲区
    uint16_t m_frameBuffer[GB_SCREEN_WIDTH * GB_SCREEN_HEIGHT];
};

VideoRenderer::VideoRenderer() {
    std::memset(m_frameBuffer, 0, sizeof(m_frameBuffer));
}

VideoRenderer::~VideoRenderer() {
    shutdown();
}

bool VideoRenderer::initialize() {
    LOGD("Initializing video renderer");
    
    // 创建着色器程序
    if (!createProgram()) {
        LOGE("Failed to create shader program");
        return false;
    }
    
    // 生成纹理
    glGenTextures(1, &m_texture);
    glBindTexture(GL_TEXTURE_2D, m_texture);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    
    // 创建顶点缓冲区
    glGenBuffers(1, &m_vertexBuffer);
    
    // 获取uniform和attribute位置
    m_positionLoc = glGetAttribLocation(m_program, "a_position");
    m_texCoordLoc = glGetAttribLocation(m_program, "a_texCoord");
    m_textureLoc = glGetUniformLocation(m_program, "u_texture");
    
    // 设置视口
    glViewport(0, 0, m_screenWidth, m_screenHeight);
    glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    
    LOGD("Video renderer initialized successfully");
    return true;
}

void VideoRenderer::shutdown() {
    LOGD("Shutting down video renderer");
    
    if (m_texture != 0) {
        glDeleteTextures(1, &m_texture);
        m_texture = 0;
    }
    
    if (m_vertexBuffer != 0) {
        glDeleteBuffers(1, &m_vertexBuffer);
        m_vertexBuffer = 0;
    }
    
    if (m_program != 0) {
        glDeleteProgram(m_program);
        m_program = 0;
    }
}

bool VideoRenderer::compileShader(GLuint& shader, GLenum type, const char* source) {
    shader = glCreateShader(type);
    glShaderSource(shader, 1, &source, nullptr);
    glCompileShader(shader);
    
    GLint compiled = 0;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
    
    if (!compiled) {
        GLint infoLen = 0;
        glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLen);
        if (infoLen > 0) {
            char* infoLog = new char[infoLen];
            glGetShaderInfoLog(shader, infoLen, nullptr, infoLog);
            LOGE("Shader compilation failed: %s", infoLog);
            delete[] infoLog;
        }
        glDeleteShader(shader);
        shader = 0;
        return false;
    }
    
    return true;
}

bool VideoRenderer::createProgram() {
    GLuint vertexShader = 0;
    GLuint fragmentShader = 0;
    
    if (!compileShader(vertexShader, GL_VERTEX_SHADER, vertexShaderSource)) {
        return false;
    }
    
    if (!compileShader(fragmentShader, GL_FRAGMENT_SHADER, fragmentShaderSource)) {
        glDeleteShader(vertexShader);
        return false;
    }
    
    m_program = glCreateProgram();
    glAttachShader(m_program, vertexShader);
    glAttachShader(m_program, fragmentShader);
    glLinkProgram(m_program);
    
    GLint linked = 0;
    glGetProgramiv(m_program, GL_LINK_STATUS, &linked);
    
    glDeleteShader(vertexShader);
    glDeleteShader(fragmentShader);
    
    if (!linked) {
        GLint infoLen = 0;
        glGetProgramiv(m_program, GL_INFO_LOG_LENGTH, &infoLen);
        if (infoLen > 0) {
            char* infoLog = new char[infoLen];
            glGetProgramInfoLog(m_program, infoLen, nullptr, infoLog);
            LOGE("Program linking failed: %s", infoLog);
            delete[] infoLog;
        }
        glDeleteProgram(m_program);
        m_program = 0;
        return false;
    }
    
    return true;
}

void VideoRenderer::renderFrame(const uint8_t* frameBuffer) {
    if (m_program == 0 || frameBuffer == nullptr) {
        return;
    }
    
    // 将GB帧缓冲区转换为RGB565纹理数据
    for (int i = 0; i < GB_SCREEN_WIDTH * GB_SCREEN_HEIGHT; i++) {
        uint8_t pixel = frameBuffer[i];
        // GB灰度到RGB565转换 (0-3 到 RGB565)
        uint8_t gray = (3 - pixel) * 85; // 0, 85, 170, 255
        uint16_t rgb565 = ((gray >> 3) << 11) | ((gray >> 2) << 5) | (gray >> 3);
        m_frameBuffer[i] = rgb565;
    }
    
    glClear(GL_COLOR_BUFFER_BIT);
    glUseProgram(m_program);
    
    // 更新纹理
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, m_texture);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB565, GB_SCREEN_WIDTH, GB_SCREEN_HEIGHT, 
                 0, GL_RGB, GL_UNSIGNED_SHORT_5_6_5, m_frameBuffer);
    glUniform1i(m_textureLoc, 0);
    
    // 全屏四边形顶点数据
    const float vertices[] = {
        // 位置        // 纹理坐标
        -1.0f, -1.0f,  0.0f, 1.0f,
         1.0f, -1.0f,  1.0f, 1.0f,
        -1.0f,  1.0f,  0.0f, 0.0f,
         1.0f,  1.0f,  1.0f, 0.0f
    };
    
    // 绑定顶点缓冲区
    glBindBuffer(GL_ARRAY_BUFFER, m_vertexBuffer);
    glBufferData(GL_ARRAY_BUFFER, sizeof(vertices), vertices, GL_STATIC_DRAW);
    
    // 设置顶点属性
    glVertexAttribPointer(m_positionLoc, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(float), (void*)0);
    glEnableVertexAttribArray(m_positionLoc);
    
    glVertexAttribPointer(m_texCoordLoc, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(float), (void*)(2 * sizeof(float)));
    glEnableVertexAttribArray(m_texCoordLoc);
    
    // 绘制
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    
    glDisableVertexAttribArray(m_positionLoc);
    glDisableVertexAttribArray(m_texCoordLoc);
}

void VideoRenderer::updateScreenSize(int width, int height) {
    m_screenWidth = width;
    m_screenHeight = height;
    glViewport(0, 0, width, height);
}
