package com.jboy.emulator.core

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import android.view.Surface
import android.view.TextureView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * 视频渲染器 - 使用OpenGL ES渲染游戏画面
 */
class VideoRenderer private constructor() {

    companion object {
        private const val TAG = "VideoRenderer"
        private const val VERTEX_SHADER_CODE = """
            attribute vec4 vPosition;
            attribute vec2 vTexCoord;
            varying vec2 vTextureCoord;
            uniform mat4 uMVPMatrix;
            
            void main() {
                gl_Position = uMVPMatrix * vPosition;
                vTextureCoord = vTexCoord;
            }
        """
        
        private const val FRAGMENT_SHADER_CODE = """
            precision mediump float;
            varying vec2 vTextureCoord;
            uniform sampler2D uTexture;
            
            void main() {
                gl_FragColor = texture2D(uTexture, vTextureCoord);
            }
        """
        
        // GBA屏幕分辨率
        const val SCREEN_WIDTH = 240
        const val SCREEN_HEIGHT = 160
        
        // 顶点坐标 (x, y)
        private val VERTEX_COORDS = floatArrayOf(
            -1.0f,  1.0f,  // 左上
            -1.0f, -1.0f,  // 左下
             1.0f, -1.0f,  // 右下
             1.0f,  1.0f   // 右上
        )
        
        // 纹理坐标 (u, v)
        private val TEX_COORDS = floatArrayOf(
            0.0f, 0.0f,  // 左上
            0.0f, 1.0f,  // 左下
            1.0f, 1.0f,  // 右下
            1.0f, 0.0f   // 右上
        )
        
        // 顶点绘制顺序
        private val DRAW_ORDER = shortArrayOf(0, 1, 2, 0, 2, 3)
        
        @Volatile
        private var instance: VideoRenderer? = null
        
        fun getInstance(): VideoRenderer {
            return instance ?: synchronized(this) {
                instance ?: VideoRenderer().also { instance = it }
            }
        }
    }
    
    private var glSurfaceView: GLSurfaceView? = null
    private var textureView: TextureView? = null
    private var renderer: GLRenderer? = null
    
    // 帧缓冲区
    private var frameBuffer: ByteArray? = null
    private val frameBufferLock = Object()
    
    // 缩放模式
    enum class ScaleMode {
        FIT,        // 适应屏幕
        FILL,       // 填充屏幕
        STRETCH,    // 拉伸
        INTEGER     // 整数倍缩放
    }
    
    private var currentScaleMode = ScaleMode.FIT
    private var isInitialized = false
    
    /**
     * 初始化GLSurfaceView渲染器
     */
    fun initWithSurfaceView(surfaceView: GLSurfaceView) {
        this.glSurfaceView = surfaceView
        
        surfaceView.setEGLContextClientVersion(2)
        renderer = GLRenderer()
        surfaceView.setRenderer(renderer)
        surfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        
        isInitialized = true
        Log.d(TAG, "VideoRenderer initialized with GLSurfaceView")
    }
    
    /**
     * 初始化TextureView渲染器
     */
    fun initWithTextureView(textureView: TextureView) {
        this.textureView = textureView
        
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                Log.d(TAG, "TextureView surface available: $width x $height")
                // 可以在这里初始化OpenGL上下文
            }
            
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                Log.d(TAG, "TextureView size changed: $width x $height")
            }
            
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return true
            }
            
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                // 纹理更新
            }
        }
        
        isInitialized = true
        Log.d(TAG, "VideoRenderer initialized with TextureView")
    }
    
    /**
     * 更新帧数据
     */
    fun updateFrame(frameData: ByteArray) {
        synchronized(frameBufferLock) {
            frameBuffer = frameData.copyOf()
        }
        glSurfaceView?.requestRender()
    }
    
    /**
     * 设置缩放模式
     */
    fun setScaleMode(mode: ScaleMode) {
        currentScaleMode = mode
        renderer?.updateScaleMode(mode)
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        isInitialized = false
        glSurfaceView = null
        textureView = null
        renderer = null
        frameBuffer = null
        Log.d(TAG, "VideoRenderer cleaned up")
    }
    
    /**
     * OpenGL渲染器
     */
    private inner class GLRenderer : GLSurfaceView.Renderer {
        
        private var program: Int = 0
        private var positionHandle: Int = 0
        private var texCoordHandle: Int = 0
        private var mvpMatrixHandle: Int = 0
        private var textureHandle: Int = 0
        
        private var textureId: Int = 0
        
        private lateinit var vertexBuffer: FloatBuffer
        private lateinit var texCoordBuffer: FloatBuffer
        private lateinit var drawListBuffer: java.nio.ShortBuffer
        
        private val mvpMatrix = FloatArray(16)
        private val projectionMatrix = FloatArray(16)
        private val viewMatrix = FloatArray(16)
        
        private var viewWidth: Int = 0
        private var viewHeight: Int = 0
        
        init {
            // 初始化顶点缓冲区
            val vbb = ByteBuffer.allocateDirect(VERTEX_COORDS.size * 4)
            vbb.order(ByteOrder.nativeOrder())
            vertexBuffer = vbb.asFloatBuffer()
            vertexBuffer.put(VERTEX_COORDS)
            vertexBuffer.position(0)
            
            // 初始化纹理坐标缓冲区
            val tbb = ByteBuffer.allocateDirect(TEX_COORDS.size * 4)
            tbb.order(ByteOrder.nativeOrder())
            texCoordBuffer = tbb.asFloatBuffer()
            texCoordBuffer.put(TEX_COORDS)
            texCoordBuffer.position(0)
            
            // 初始化绘制顺序缓冲区
            val dlb = ByteBuffer.allocateDirect(DRAW_ORDER.size * 2)
            dlb.order(ByteOrder.nativeOrder())
            drawListBuffer = dlb.asShortBuffer()
            drawListBuffer.put(DRAW_ORDER)
            drawListBuffer.position(0)
        }
        
        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
            
            // 编译着色器
            val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE)
            val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE)
            
            // 创建程序
            program = GLES20.glCreateProgram()
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)
            
            // 获取句柄
            positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
            texCoordHandle = GLES20.glGetAttribLocation(program, "vTexCoord")
            mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
            textureHandle = GLES20.glGetUniformLocation(program, "uTexture")
            
            // 创建纹理
            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            textureId = textures[0]
            
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
        }
        
        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
            viewWidth = width
            viewHeight = height
            updateProjectionMatrix()
        }
        
        override fun onDrawFrame(gl: GL10?) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            
            // 获取当前帧数据
            var currentFrame: ByteArray? = null
            synchronized(frameBufferLock) {
                currentFrame = frameBuffer
            }
            
            if (currentFrame == null) return
            
            // 更新纹理
            updateTexture(currentFrame!!)
            
            // 使用程序
            GLES20.glUseProgram(program)
            
            // 设置顶点数据
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
            
            // 设置纹理坐标
            GLES20.glEnableVertexAttribArray(texCoordHandle)
            GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)
            
            // 设置MVP矩阵
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
            
            // 绑定纹理
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glUniform1i(textureHandle, 0)
            
            // 绘制
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, DRAW_ORDER.size, GLES20.GL_UNSIGNED_SHORT, drawListBuffer)
            
            // 禁用顶点数组
            GLES20.glDisableVertexAttribArray(positionHandle)
            GLES20.glDisableVertexAttribArray(texCoordHandle)
        }
        
        private fun loadShader(type: Int, shaderCode: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
            return shader
        }
        
        private fun updateTexture(frameData: ByteArray) {
            // 将RGBA字节数组转换为纹理
            val buffer = ByteBuffer.allocateDirect(frameData.size)
            buffer.order(ByteOrder.nativeOrder())
            buffer.put(frameData)
            buffer.position(0)
            
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                SCREEN_WIDTH, SCREEN_HEIGHT, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer
            )
        }
        
        private fun updateProjectionMatrix() {
            Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, -3f, 0f, 0f, 0f, 0f, 1f, 0f)
            
            val aspectRatio = viewWidth.toFloat() / viewHeight.toFloat()
            val gameAspectRatio = SCREEN_WIDTH.toFloat() / SCREEN_HEIGHT.toFloat()
            
            when (currentScaleMode) {
                ScaleMode.FIT -> {
                    if (aspectRatio > gameAspectRatio) {
                        val scale = gameAspectRatio / aspectRatio
                        Matrix.frustumM(projectionMatrix, 0, -scale, scale, -1f, 1f, 3f, 7f)
                    } else {
                        val scale = aspectRatio / gameAspectRatio
                        Matrix.frustumM(projectionMatrix, 0, -1f, 1f, -scale, scale, 3f, 7f)
                    }
                }
                ScaleMode.FILL, ScaleMode.STRETCH -> {
                    Matrix.frustumM(projectionMatrix, 0, -1f, 1f, -1f, 1f, 3f, 7f)
                }
                ScaleMode.INTEGER -> {
                    // 整数倍缩放逻辑
                    Matrix.frustumM(projectionMatrix, 0, -1f, 1f, -1f, 1f, 3f, 7f)
                }
            }
            
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        }
        
        fun updateScaleMode(mode: ScaleMode) {
            currentScaleMode = mode
            updateProjectionMatrix()
        }
    }
}
