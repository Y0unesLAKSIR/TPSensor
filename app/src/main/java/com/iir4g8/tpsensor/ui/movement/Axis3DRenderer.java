package com.iir4g8.tpsensor.ui.movement;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class Axis3DRenderer implements GLSurfaceView.Renderer {
    private final Context context;

    // Matrices
    private final float[] modelMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] mvpMatrix = new float[16]; // Model-View-Projection matrix

    // Rotation values
    private float azimuth = 0f;
    private float pitch = 0f;
    private float roll = 0f;

    // Shader program
    private int program;

    // Vertex and color buffers for X, Y, Z axes
    private FloatBuffer xAxisVertexBuffer;
    private FloatBuffer yAxisVertexBuffer;
    private FloatBuffer zAxisVertexBuffer;
    private FloatBuffer xAxisColorBuffer;
    private FloatBuffer yAxisColorBuffer;
    private FloatBuffer zAxisColorBuffer;

    // Cube buffers
    private FloatBuffer cubeVertexBuffer;
    private FloatBuffer cubeColorBuffer;
    private ByteBuffer cubeIndexBuffer;

    // Shader handles
    private int positionHandle;
    private int colorHandle;
    private int mvpMatrixHandle;

    // Axis length
    private final float axisLength = 1.0f;

    public Axis3DRenderer(Context context) {
        this.context = context;
        setupBuffers();
    }

    private void setupBuffers() {
        // X-axis vertices (RED)
        float[] xAxisVertices = {
                0.0f, 0.0f, 0.0f,
                axisLength, 0.0f, 0.0f
        };

        // Y-axis vertices (GREEN)
        float[] yAxisVertices = {
                0.0f, 0.0f, 0.0f,
                0.0f, axisLength, 0.0f
        };

        // Z-axis vertices (BLUE)
        float[] zAxisVertices = {
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, axisLength
        };

        // X-axis colors (RED)
        float[] xAxisColors = {
                1.0f, 0.0f, 0.0f, 1.0f,
                1.0f, 0.0f, 0.0f, 1.0f
        };

        // Y-axis colors (GREEN)
        float[] yAxisColors = {
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f
        };

        // Z-axis colors (BLUE)
        float[] zAxisColors = {
                0.0f, 0.0f, 1.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f
        };

        // Create vertex buffers
        xAxisVertexBuffer = createFloatBuffer(xAxisVertices);
        yAxisVertexBuffer = createFloatBuffer(yAxisVertices);
        zAxisVertexBuffer = createFloatBuffer(zAxisVertices);

        // Create color buffers
        xAxisColorBuffer = createFloatBuffer(xAxisColors);
        yAxisColorBuffer = createFloatBuffer(yAxisColors);
        zAxisColorBuffer = createFloatBuffer(zAxisColors);

        // Create cube vertices - Fixed ordering to ensure correct face orientation
        float cubeSize = 0.3f;
        float[] cubeVertices = {
                // Front face - vertices in counter-clockwise order when viewed from front
                -cubeSize, -cubeSize, cubeSize,  // bottom-left
                -cubeSize, cubeSize, cubeSize,   // top-left
                cubeSize, cubeSize, cubeSize,    // top-right
                cubeSize, -cubeSize, cubeSize,   // bottom-right

                // Back face - vertices in counter-clockwise order when viewed from back
                cubeSize, -cubeSize, -cubeSize,  // bottom-left
                cubeSize, cubeSize, -cubeSize,   // top-left
                -cubeSize, cubeSize, -cubeSize,  // top-right
                -cubeSize, -cubeSize, -cubeSize, // bottom-right

                // Top face - vertices in counter-clockwise order when viewed from top
                -cubeSize, cubeSize, cubeSize,   // front-left
                -cubeSize, cubeSize, -cubeSize,  // back-left
                cubeSize, cubeSize, -cubeSize,   // back-right
                cubeSize, cubeSize, cubeSize,    // front-right

                // Bottom face - vertices in counter-clockwise order when viewed from bottom
                -cubeSize, -cubeSize, -cubeSize, // back-left
                -cubeSize, -cubeSize, cubeSize,  // front-left
                cubeSize, -cubeSize, cubeSize,   // front-right
                cubeSize, -cubeSize, -cubeSize,  // back-right

                // Right face - vertices in counter-clockwise order when viewed from right
                cubeSize, -cubeSize, cubeSize,   // bottom-front
                cubeSize, cubeSize, cubeSize,    // top-front
                cubeSize, cubeSize, -cubeSize,   // top-back
                cubeSize, -cubeSize, -cubeSize,  // bottom-back

                // Left face - vertices in counter-clockwise order when viewed from left
                -cubeSize, -cubeSize, -cubeSize, // bottom-back
                -cubeSize, cubeSize, -cubeSize,  // top-back
                -cubeSize, cubeSize, cubeSize,   // top-front
                -cubeSize, -cubeSize, cubeSize   // bottom-front
        };

        // Create cube colors with distinct colors for each face to better visualize orientation
        float[] cubeColors = new float[24 * 4]; // 24 vertices, 4 color components each

        // Front face - light red
        for (int i = 0; i < 4; i++) {
            cubeColors[i * 4] = 1.0f;     // R
            cubeColors[i * 4 + 1] = 0.5f; // G
            cubeColors[i * 4 + 2] = 0.5f; // B
            cubeColors[i * 4 + 3] = 0.7f; // A
        }

        // Back face - light blue
        for (int i = 4; i < 8; i++) {
            cubeColors[i * 4] = 0.5f;     // R
            cubeColors[i * 4 + 1] = 0.5f; // G
            cubeColors[i * 4 + 2] = 1.0f; // B
            cubeColors[i * 4 + 3] = 0.7f; // A
        }

        // Top face - light green
        for (int i = 8; i < 12; i++) {
            cubeColors[i * 4] = 0.5f;     // R
            cubeColors[i * 4 + 1] = 1.0f; // G
            cubeColors[i * 4 + 2] = 0.5f; // B
            cubeColors[i * 4 + 3] = 0.7f; // A
        }

        // Bottom face - light yellow
        for (int i = 12; i < 16; i++) {
            cubeColors[i * 4] = 1.0f;     // R
            cubeColors[i * 4 + 1] = 1.0f; // G
            cubeColors[i * 4 + 2] = 0.5f; // B
            cubeColors[i * 4 + 3] = 0.7f; // A
        }

        // Right face - light purple
        for (int i = 16; i < 20; i++) {
            cubeColors[i * 4] = 0.8f;     // R
            cubeColors[i * 4 + 1] = 0.5f; // G
            cubeColors[i * 4 + 2] = 0.8f; // B
            cubeColors[i * 4 + 3] = 0.7f; // A
        }

        // Left face - light cyan
        for (int i = 20; i < 24; i++) {
            cubeColors[i * 4] = 0.5f;     // R
            cubeColors[i * 4 + 1] = 0.8f; // G
            cubeColors[i * 4 + 2] = 0.8f; // B
            cubeColors[i * 4 + 3] = 0.7f; // A
        }

        // Create cube indices - Fixed to match the new vertex ordering
        byte[] cubeIndices = {
                0, 1, 2, 0, 2, 3,       // Front face
                4, 5, 6, 4, 6, 7,       // Back face
                8, 9, 10, 8, 10, 11,    // Top face
                12, 13, 14, 12, 14, 15, // Bottom face
                16, 17, 18, 16, 18, 19, // Right face
                20, 21, 22, 20, 22, 23  // Left face
        };

        cubeVertexBuffer = createFloatBuffer(cubeVertices);
        cubeColorBuffer = createFloatBuffer(cubeColors);

        cubeIndexBuffer = ByteBuffer.allocateDirect(cubeIndices.length);
        cubeIndexBuffer.put(cubeIndices);
        cubeIndexBuffer.position(0);
    }

    private FloatBuffer createFloatBuffer(float[] data) {
        FloatBuffer buffer = ByteBuffer.allocateDirect(data.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buffer.put(data);
        buffer.position(0);
        return buffer;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Set the background color
        GLES20.glClearColor(0.9f, 0.9f, 0.9f, 1.0f);

        // Enable depth testing
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Enable blending for transparency
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Enable face culling to improve performance and fix inverted faces
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);

        // Compile shaders
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        // Create program
        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        // Get handles
        positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        colorHandle = GLES20.glGetAttribLocation(program, "vColor");
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;

        // Set up projection matrix
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 2, 10);

        // Set up view matrix (camera)
        Matrix.setLookAtM(viewMatrix, 0,
                0, 0, 3,  // Camera position
                0, 0, 0,  // Look at position
                0, 1, 0); // Up vector
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear the screen
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Use the shader program
        GLES20.glUseProgram(program);

        // Set up model matrix with rotations
        Matrix.setIdentityM(modelMatrix, 0);

        // Apply rotations in the correct order
        Matrix.rotateM(modelMatrix, 0, roll, 0, 1, 0);   // Y-axis rotation (roll)
        Matrix.rotateM(modelMatrix, 0, pitch, 1, 0, 0);  // X-axis rotation (pitch)
        Matrix.rotateM(modelMatrix, 0, -azimuth, 0, 0, 1); // Z-axis rotation (azimuth)

        // Calculate the model-view-projection matrix
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);

        // Set the MVP matrix
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        // Draw the cube first (semi-transparent)
        drawCube();

        // Draw the axes
        drawAxis(xAxisVertexBuffer, xAxisColorBuffer);
        drawAxis(yAxisVertexBuffer, yAxisColorBuffer);
        drawAxis(zAxisVertexBuffer, zAxisColorBuffer);

        // Draw axis labels
        drawAxisLabels();
    }

    private void drawAxis(FloatBuffer vertexBuffer, FloatBuffer colorBuffer) {
        // Set vertex attributes
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        // Set color attributes
        GLES20.glEnableVertexAttribArray(colorHandle);
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 0, colorBuffer);

        // Draw the line
        GLES20.glLineWidth(5.0f);
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2);

        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(colorHandle);
    }

    private void drawCube() {
        // Set vertex attributes
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, cubeVertexBuffer);

        // Set color attributes
        GLES20.glEnableVertexAttribArray(colorHandle);
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 0, cubeColorBuffer);

        // Draw the cube using indices
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 36, GLES20.GL_UNSIGNED_BYTE, cubeIndexBuffer);

        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(colorHandle);
    }

    private void drawAxisLabels() {
        // In OpenGL ES 2.0, text rendering is not built-in
        // For simplicity, we'll draw small shapes at the end of each axis
        // For a real app, you might want to use a texture atlas or bitmap font

        // Draw a small sphere at the end of each axis
        // This is simplified - in a real app, you'd use proper text rendering
    }

    public void setOrientation(float azimuth, float pitch, float roll) {
        this.azimuth = azimuth;
        this.pitch = pitch;
        this.roll = roll;
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    // Vertex shader code
    private final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "attribute vec4 vColor;" +
                    "varying vec4 fragmentColor;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "  fragmentColor = vColor;" +
                    "}";

    // Fragment shader code
    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "varying vec4 fragmentColor;" +
                    "void main() {" +
                    "  gl_FragColor = fragmentColor;" +
                    "}";
}
