package tests.overlay1.lwjgl3only.utils;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_X;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glFinish;
import static org.lwjgl.opengl.GL11.glFlush;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.io.PrintStream;
import java.nio.file.Path;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import tests.overlay1.lwjgl3only.Main1;

public class ManagerGLFW implements AutoCloseable
{
	private static boolean printCoordsSign = false;
	public static boolean printCoords = false;

	private static final int WIDTH = 300;
	private static final int HEIGHT = 300;

	private final long windowHandle;


	public ManagerGLFW( PrintStream psGLFWError )
	{
		GLFWErrorCallback.createPrint( psGLFWError ).set();

		// Initialize GLFW. Most GLFW functions will not work before doing this.
		if ( !glfwInit() ) 
		{
			throw new IllegalStateException( "Unable to initialize GLFW" );
		}

		// Configure our window
		glfwDefaultWindowHints(); // optional, the current window hints are already the default
		glfwWindowHint( GLFW_VISIBLE,GL_FALSE ); // the window will stay hidden after creation
		glfwWindowHint( GLFW_RESIZABLE,GL_TRUE ); // the window will be resizable

		// Create the window
		this.windowHandle = glfwCreateWindow( WIDTH,HEIGHT,"Hello World!",NULL,NULL );
		if ( windowHandle == NULL )
		{
			throw new RuntimeException( "Failed to create the GLFW window" );
		}

		// Setup a key callback. It will be called every time a key is pressed, repeated or released.
		GLFWKeyCallbackI callbackFunc = new GLFWKeyCallback()
		{
			@Override
			public void invoke( long window,int key,int scancode,int action,int mods )
			{
				if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
				{
					glfwSetWindowShouldClose( window,true ); // We will detect this in the rendering loop
				}
				if ( key == GLFW_KEY_X && action == GLFW_RELEASE )
				{
					if ( printCoordsSign==false )
					{
						Main1.log( "GLFW_KEY_X" );
						printCoordsSign = true;
						printCoords = true;
					}
				}
			}
		};
		glfwSetKeyCallback( windowHandle,callbackFunc );

		// Get the resolution of the primary monitor
		GLFWVidMode vidmode = glfwGetVideoMode( glfwGetPrimaryMonitor() );
		// Center our window
		glfwSetWindowPos( windowHandle,(vidmode.width() - WIDTH) / 2,(vidmode.height() - HEIGHT) / 2 );

		// Make the OpenGL context current
		glfwMakeContextCurrent( windowHandle );
		// Enable v-sync
		glfwSwapInterval( 0 );

		// Make the window visible
		glfwShowWindow( windowHandle );

		// This line is critical for LWJGL's interoperation with GLFW's
		// OpenGL context, or any context that is managed externally.
		// LWJGL detects the context that is current in the current thread,
		// creates the GLCapabilities instance and makes the OpenGL
		// bindings available for use.
		GL.createCapabilities();

		// Set the clear color
		glClearColor( 1.0f,0.0f,0.0f,0.0f );
	}

	public void loop( Path pathStop ) throws InterruptedException
	{
		float countFrames = 0;
		long tsStart = System.currentTimeMillis();
		// Run the rendering loop until the user has attempted to close
		// the window or has pressed the ESCAPE key.
		while ( !glfwWindowShouldClose( windowHandle ) )
		{
//			if ( Files.exists( pathStop )==true )
//				break;

			glClear( GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT ); // clear the framebuffer

			countFrames += 1.0f;

//			GL11.glColor3f( 0,0,0 );
			GL11.glColor3f( 1,1,1 );
			GL11.glBegin( GL11.GL_LINES );
			GL11.glVertex2f( 0f,0f );
			GL11.glVertex2f( 0.5f,0.5f );
			GL11.glEnd();

//			glFinish();
//			Thread.sleep( 1 );

			glfwSwapBuffers( windowHandle ); // swap the color buffers

//			glClearColor( 0,0,0,1 );
//			glClear( GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT );
			glFlush();	// ez kell ide a doksi szerint
			glFinish();

			// Poll for window events. The key callback above will only be
			// invoked during this call.
			printCoords = false;
			glfwPollEvents();
			Thread.sleep( 50 );
		}
		long tsDiff = System.currentTimeMillis()-tsStart;
		Main1.log( String.format( "fps=%f (countFrames=%f)(tsDiff=%d)",1000*countFrames/tsDiff,countFrames,tsDiff ) );
	}

	@Override
	public void close() throws Exception
	{
		// Release window and window callbacks
		glfwFreeCallbacks( windowHandle );
		glfwDestroyWindow( windowHandle );

		// Terminate GLFW and release the GLFWerrorfun
		glfwTerminate();
		glfwSetErrorCallback( null ).free();

		// memory leak miatt
		GL.setCapabilities( null );
	}
}
