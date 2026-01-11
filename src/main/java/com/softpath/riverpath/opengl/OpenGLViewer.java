package com.softpath.riverpath.opengl;

import com.softpath.riverpath.fileparser.CFDTriangleMesh;
import javafx.application.Platform;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * OpenGL Mesh Viewer - opens a separate window for high-resolution rendering
 * <p>
 * Usage:
 * OpenGLViewer.show(cfdMesh, is3D);           // Open viewer
 * OpenGLViewer.close();                        // Close viewer
 * OpenGLViewer.isRunning();                    // Check if open
 */
public class OpenGLViewer {

    private static OpenGLViewer instance;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean shouldClose = new AtomicBoolean(false);
    // Matrices
    private final Matrix4f viewMatrix = new Matrix4f();
    private final Matrix4f projMatrix = new Matrix4f();
    private final Matrix3f normalMatrix = new Matrix3f();
    private final float[] mvBuffer = new float[16];
    private final float[] projBuffer = new float[16];
    private final float[] normBuffer = new float[9];
    private long window;
    private Thread renderThread;
    // OpenGL resources
    private int vao, vboVertices, vboNormals, ebo, shaderProgram;
    private int indexCount;
    // Camera
    private float distance = 5f, rotX = 30f, rotY = 45f;
    private float targetX = 0, targetY = 0, targetZ = 0;
    private double lastMouseX, lastMouseY;
    private boolean rotating = false;
    private int width = 1280, height = 720;
    private MeshData currentMesh;
    private boolean panning = false;

    // ==================== Public API ====================
    private Runnable onCloseCallback;

    /**
     * Show mesh in OpenGL viewer
     */
    public static void show(CFDTriangleMesh mesh) {
        if (instance != null && instance.running.get()) {
            instance.shouldClose.set(true);
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }

        instance = new OpenGLViewer();
        MeshData data = MeshDataAdapter.fromCFDMesh(mesh);
        instance.start(data);
    }

    /**
     * Close the viewer
     */
    public static void close() {
        if (instance != null) {
            instance.shouldClose.set(true);
        }
    }

    /**
     * Check if viewer is running
     */
    public static boolean isRunning() {
        return instance != null && instance.running.get();
    }

    /**
     * Set callback when viewer closes
     */
    public static void setOnClose(Runnable callback) {
        if (instance != null) {
            instance.onCloseCallback = callback;
        }
    }

    // ==================== Internal ====================

    private void start(MeshData mesh) {
        renderThread = new Thread(() -> {
            try {
                initWindow();
                initGL();
                uploadMesh(mesh);
                currentMesh = mesh;
                fitCamera(mesh);
                running.set(true);

                while (!shouldClose.get() && !glfwWindowShouldClose(window)) {
                    render();
                    glfwSwapBuffers(window);
                    glfwPollEvents();
                }
            } finally {
                cleanup();
                running.set(false);
                if (onCloseCallback != null) {
                    Platform.runLater(onCloseCallback);
                }
            }
        }, "OpenGL-Viewer");

        renderThread.start();
    }

    private void initWindow() {
        if (!glfwInit()) throw new RuntimeException("Failed to init GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        window = glfwCreateWindow(width, height, "High-Resolution Mesh Viewer", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Failed to create window");

        // Center window
        GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vidMode != null) {
            glfwSetWindowPos(window, (vidMode.width() - width) / 2, (vidMode.height() - height) / 2);
        }

        // Callbacks
        glfwSetFramebufferSizeCallback(window, (w, width, height) -> {
            this.width = width;
            this.height = height;
            glViewport(0, 0, width, height);
        });

        glfwSetMouseButtonCallback(window, (w, button, action, mods) -> {
            double[] x = new double[1], y = new double[1];
            glfwGetCursorPos(w, x, y);
            if (action == GLFW_PRESS && button == GLFW_MOUSE_BUTTON_LEFT) {
                rotating = true;
                lastMouseX = x[0];
                lastMouseY = y[0];
            } else if (action == GLFW_RELEASE) {
                rotating = false;
            }
        });

        // Add double-click detection - add this NEW callback after mouse button callback:
        glfwSetMouseButtonCallback(window, new GLFWMouseButtonCallback() {
            private long lastClickTime = 0;

            @Override
            public void invoke(long w, int button, int action, int mods) {
                double[] x = new double[1], y = new double[1];
                glfwGetCursorPos(w, x, y);

                if (action == GLFW_PRESS) {
                    long now = System.currentTimeMillis();
                    if (button == GLFW_MOUSE_BUTTON_LEFT && (now - lastClickTime) < 300) {
                        // Double-click: reset view
                        rotX = 30; rotY = 45;
                        fitCamera(currentMesh);
                    }
                    lastClickTime = now;

                    if (button == GLFW_MOUSE_BUTTON_LEFT) {
                        rotating = true;
                    } else if (button == GLFW_MOUSE_BUTTON_RIGHT) {
                        panning = true;
                    }
                    lastMouseX = x[0];
                    lastMouseY = y[0];
                } else if (action == GLFW_RELEASE) {
                    rotating = false;
                    panning = false;
                }
            }
        });

        glfwSetCursorPosCallback(window, (w, x, y) -> {
            double dx = x - lastMouseX;
            double dy = y - lastMouseY;

            if (rotating) {
                rotY += dx * 0.5f;
                rotX += dy * 0.5f;
                rotX = Math.max(-89, Math.min(89, rotX));
            } else if (panning) {
                float panSpeed = distance * 0.002f;
                targetX -= dx * panSpeed;
                targetY += dy * panSpeed;
            }

            lastMouseX = x;
            lastMouseY = y;
        });

        glfwSetScrollCallback(window, (w, dx, dy) -> {
            distance *= (1 - dy * 0.1f);
            distance = Math.max(0.1f, distance);
        });

        glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS && key == GLFW_KEY_ESCAPE) shouldClose.set(true);
            if (action == GLFW_PRESS && key == GLFW_KEY_R) {
                rotX = 30;
                rotY = 45;
            }
        });

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
    }

    private void initGL() {
        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glClearColor(1f, 1f, 1f, 1f);

        shaderProgram = createShaders();
        vao = glGenVertexArrays();
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

    }

    private void uploadMesh(MeshData mesh) {
        glBindVertexArray(vao);

        vboVertices = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboVertices);
        FloatBuffer vb = BufferUtils.createFloatBuffer(mesh.vertices().length);
        vb.put(mesh.vertices()).flip();
        glBufferData(GL_ARRAY_BUFFER, vb, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        vboNormals = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboNormals);
        FloatBuffer nb = BufferUtils.createFloatBuffer(mesh.normals().length);
        nb.put(mesh.normals()).flip();
        glBufferData(GL_ARRAY_BUFFER, nb, GL_STATIC_DRAW);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(1);

        ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        IntBuffer ib = BufferUtils.createIntBuffer(mesh.indices().length);
        ib.put(mesh.indices()).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);

        indexCount = mesh.indices().length;
        glBindVertexArray(0);

        System.out.println("OpenGL: Uploaded " + mesh.getVertexCount() + " vertices, " + mesh.getTriangleCount() + " triangles");
    }

    private void fitCamera(MeshData mesh) {
        float[] v = mesh.vertices();
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

        for (int i = 0; i < v.length; i += 3) {
            minX = Math.min(minX, v[i]);
            maxX = Math.max(maxX, v[i]);
            minY = Math.min(minY, v[i + 1]);
            maxY = Math.max(maxY, v[i + 1]);
            minZ = Math.min(minZ, v[i + 2]);
            maxZ = Math.max(maxZ, v[i + 2]);
        }

        targetX = (minX + maxX) / 2;
        targetY = (minY + maxY) / 2;
        targetZ = (minZ + maxZ) / 2;
        distance = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ)) * 1.5f;
    }

    private void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        if (indexCount == 0) return;

        // Update matrices
        float radX = (float) Math.toRadians(rotX);
        float radY = (float) Math.toRadians(rotY);
        float camX = targetX + distance * (float) (Math.cos(radX) * Math.sin(radY));
        float camY = targetY + distance * (float) Math.sin(radX);
        float camZ = targetZ + distance * (float) (Math.cos(radX) * Math.cos(radY));

        viewMatrix.identity().lookAt(camX, camY, camZ, targetX, targetY, targetZ, 0, 1, 0);
        projMatrix.identity().perspective((float) Math.toRadians(45), (float) width / height, 0.1f, distance * 10);
        viewMatrix.normal(normalMatrix);

        viewMatrix.get(mvBuffer);
        projMatrix.get(projBuffer);
        normalMatrix.get(normBuffer);

        // Draw
        glUseProgram(shaderProgram);
        glUniformMatrix4fv(glGetUniformLocation(shaderProgram, "uMV"), false, mvBuffer);
        glUniformMatrix4fv(glGetUniformLocation(shaderProgram, "uP"), false, projBuffer);
        glUniformMatrix3fv(glGetUniformLocation(shaderProgram, "uN"), false, normBuffer);
        glUniform3f(glGetUniformLocation(shaderProgram, "uLight"), 0.3f, 0.5f, 1f);
        glUniform3f(glGetUniformLocation(shaderProgram, "uColor"), 0.2f, 0.2f, 0.3f);
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    private void cleanup() {
        if (vboVertices != 0) glDeleteBuffers(vboVertices);
        if (vboNormals != 0) glDeleteBuffers(vboNormals);
        if (ebo != 0) glDeleteBuffers(ebo);
        if (vao != 0) glDeleteVertexArrays(vao);
        if (shaderProgram != 0) glDeleteProgram(shaderProgram);

        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private int createShaders() {
        String vs = """
                #version 330 core
                layout(location=0) in vec3 aPos;
                layout(location=1) in vec3 aNorm;
                uniform mat4 uMV, uP;
                uniform mat3 uN;
                out vec3 vNorm, vPos;
                void main() {
                    vec4 p = uMV * vec4(aPos, 1.0);
                    vPos = p.xyz;
                    vNorm = normalize(uN * aNorm);
                    gl_Position = uP * p;
                }
                """;

        String fs = """
                #version 330 core
                in vec3 vNorm, vPos;
                uniform vec3 uLight, uColor;
                out vec4 fragColor;
                void main() {
                    vec3 n = normalize(vNorm);
                    vec3 l = normalize(uLight);
                    float diff = max(dot(n, l), 0.0);
                    vec3 v = normalize(-vPos);
                    vec3 r = reflect(-l, n);
                    float spec = pow(max(dot(v, r), 0.0), 32.0);
                    fragColor = vec4((0.2 + diff * 0.7 + spec * 0.3) * uColor, 1.0);
                }
                """;

        int vShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vShader, vs);
        glCompileShader(vShader);

        int fShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fShader, fs);
        glCompileShader(fShader);

        int prog = glCreateProgram();
        glAttachShader(prog, vShader);
        glAttachShader(prog, fShader);
        glLinkProgram(prog);

        glDeleteShader(vShader);
        glDeleteShader(fShader);

        return prog;
    }
}
