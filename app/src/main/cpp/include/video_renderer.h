#ifndef VIDEO_RENDERER_H
#define VIDEO_RENDERER_H

#include <GLES2/gl2.h>
#include <cstdint>

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
    uint16_t m_frameBuffer[160 * 144];
};

#endif // VIDEO_RENDERER_H
