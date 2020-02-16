package testvr02;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWVidMode;

public class ManagerGLFW implements AutoCloseable
{
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
		glfwSwapInterval( 1 );

		// Make the window visible
		glfwShowWindow( windowHandle );
	}

	public void loop( Path pathStop,ManagerOpenVR movr,Renderer renderer ) throws InterruptedException
	{
		// Run the rendering loop until the user has attempted to close
		// the window or has pressed the ESCAPE key.
		while ( !glfwWindowShouldClose( windowHandle ) )
		{
//			if ( Files.exists( pathStop )==true )
//				break;

			glClear( GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT ); // clear the framebuffer

			movr.pollEvents();
			movr.handleInputs();

			renderer.render();
			movr.copyFrameBuffersToHMD();
			Thread.sleep( 1 );

			glfwSwapBuffers( windowHandle ); // swap the color buffers

			// Poll for window events. The key callback above will only be
			// invoked during this call.
			glfwPollEvents();
		}
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
