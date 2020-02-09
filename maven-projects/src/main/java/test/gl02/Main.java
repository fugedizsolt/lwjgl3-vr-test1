package test.gl02;

/*
 * ez a jó static import rész
import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;
*/
import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

/*
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
*/

public class Main
{
	private static final int WIDTH = 400;
	private static final int HEIGHT = 300;


	public Main()
	{
	}

	public void run() throws Exception
	{
		System.out.println( "Hello LWJGL " + Version.getVersion() + "!" );

		try
		{
			// The window handle
			long windowHandle = init_GLFW();
			init_OpenGL();
			Renderer renderer = new Renderer();
			try
			{
				renderer.init( Main.WIDTH,Main.HEIGHT );
				loop( windowHandle,renderer );
			}
			finally
			{
				renderer.cleanup();
			}

			// Release window and window callbacks
			glfwFreeCallbacks( windowHandle );
			glfwDestroyWindow( windowHandle );
		}
		finally
		{
			// Terminate GLFW and release the GLFWerrorfun
			glfwTerminate();
			glfwSetErrorCallback( null ).free();
		}
	}

	private long init_GLFW()
	{
		// Setup an error callback. The default implementation
		// will print the error message in System.err.
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
		long window = glfwCreateWindow( WIDTH,HEIGHT,"Hello World!",NULL,NULL );
		if ( window == NULL )
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
		glfwSetKeyCallback( window,callbackFunc );

		// Get the resolution of the primary monitor
		GLFWVidMode vidmode = glfwGetVideoMode( glfwGetPrimaryMonitor() );
		// Center our window
		glfwSetWindowPos( window,(vidmode.width() - WIDTH) / 2,(vidmode.height() - HEIGHT) / 2 );

		// Make the OpenGL context current
		glfwMakeContextCurrent( window );
		// Enable v-sync
		glfwSwapInterval( 1 );

		// Make the window visible
		glfwShowWindow( window );

		return window;
	}

	private void init_OpenGL()
	{
		// This line is critical for LWJGL's interoperation with GLFW's
		// OpenGL context, or any context that is managed externally.
		// LWJGL detects the context that is current in the current thread,
		// creates the ContextCapabilities instance and makes the OpenGL
		// bindings available for use.
		GL.createCapabilities();

		// Set the clear color
		glClearColor( 0.4f,0.4f,0.4f,0.0f );
		glEnable( GL_DEPTH_TEST );

		glViewport( 0,0,Main.WIDTH,Main.HEIGHT );
	}

	private void loop( long windowHandle,Renderer renderer )
	{
		// Run the rendering loop until the user has attempted to close
		// the window or has pressed the ESCAPE key.
		while ( !glfwWindowShouldClose( windowHandle ) )
		{
			renderer.render();

			glfwSwapBuffers( windowHandle ); // swap the color buffers

			// Poll for window events. The key callback above will only be
			// invoked during this call.
			glfwPollEvents();
		}
	}

	public static void main( String[] args ) throws Exception
	{
		new Main().run();
	}
}
