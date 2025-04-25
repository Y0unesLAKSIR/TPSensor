package com.iir4g8.tpsensor.ui.movement;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class Axis3DView extends GLSurfaceView {
    private Axis3DRenderer renderer;

    public Axis3DView(Context context) {
        super(context);
        init(context);
    }

    public Axis3DView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2);

        // Set the renderer
        renderer = new Axis3DRenderer(context);
        setRenderer(renderer);

        // Render the view only when there is a change
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void setOrientation(float azimuth, float pitch, float roll) {
        renderer.setOrientation(azimuth, pitch, roll);
        requestRender(); // Request a render update
    }
}