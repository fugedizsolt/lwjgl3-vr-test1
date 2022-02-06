package testvr02;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.lwjgl.opengl.GL;


public class HelloOpenVR
{
	public static void main( String[] args ) throws Exception
	{
		new HelloOpenVR();
	}

	private HelloOpenVR() throws Exception
	{
		Path pathStop = Paths.get( "stop.dat" );
		if ( Files.exists( pathStop )==true )
			Files.delete( pathStop );

		try ( ManagerGLFW mglfw = new ManagerGLFW() )
		{
			GL.createCapabilities();

			try ( ManagerOpenVR movr = new ManagerOpenVR() )
			{
				Renderer renderer = new Renderer();
				try
				{
					renderer.init( movr.getRenderWidth(),movr.getRenderHeight() );
					movr.initVRTextures( renderer.getFbo() );

					mglfw.loop( pathStop,movr,renderer );
				}
				finally
				{
					renderer.cleanup();
				}
			}
		}
	}

	public static void log( String msg,Object ... args )
	{
		System.out.println( String.format( msg,args ) );
	}
}