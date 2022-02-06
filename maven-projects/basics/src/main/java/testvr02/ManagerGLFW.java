package testvr02;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.nio.file.Path;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWVidMode;

public class ManagerGLFW implements AutoCloseable
{
	private static boolean printCoordsSign = false;
	public static boolean printCoords = false;

	private static final int WIDTH = 300;
	private static final int HEIGHT = 300;

	private final long windowHandle;


	public ManagerGLFW()
	{
		GLFWErrorCallback.createPrint( System.err ).set();

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
						System.out.println( "GLFW_KEY_X" );
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
	}

	public void loop( Path pathStop,ManagerOpenVR movr,Renderer renderer ) throws InterruptedException
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

			movr.pollEvents();
			movr.handleInputs();

			renderer.render( movr.getAllTransformMatrixEyeLeft(),movr.getAllTransformMatrixEyeRight() );
			movr.copyFrameBuffersToHMD();
			countFrames += 1.0f;

//			glFinish();
//			Thread.sleep( 1 );

//			glfwSwapBuffers( windowHandle ); // swap the color buffers

//			glClearColor( 0,0,0,1 );
//			glClear( GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT );
			glFlush();	// ez kell ide a doksi szerint
			glFinish();

			// Poll for window events. The key callback above will only be
			// invoked during this call.
			printCoords = false;
			glfwPollEvents();
		}
		long tsDiff = System.currentTimeMillis()-tsStart;
		System.out.println( String.format( "fps=%f (countFrames=%f)(tsDiff=%d)",1000*countFrames/tsDiff,countFrames,tsDiff ) );
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
	}
}
