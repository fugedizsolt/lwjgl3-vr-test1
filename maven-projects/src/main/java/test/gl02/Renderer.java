package test.gl02;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class Renderer
{
	private static final String GL02_VERTEX_SHADER = "/gl02-vertex.vs";
	private static final String GL02_FRAGMENT_SHADER = "/gl02-fragment.fs";

	private static final String VERTEX_SHADER_PARAM_TRANSFORM = "transform";
	private static final float FOV = (float) Math.toRadians(60.0f);
	private static final float Z_NEAR = 0.01f;
	private static final float Z_FAR = 1000.f;
	private final Matrix4f allTransformMatrix = new Matrix4f();
	private Matrix4f projectionMatrix;
	private long prevRenderTimestamp = 0;

	private long renderTimestampFromStart = 0;

	private ShaderProgram shaderProgram = null;
	private Mesh mesh = null;


	public Renderer()
	{
	}

	public void init( int windowWidth,int windowHeight ) throws Exception
	{
		shaderProgram = new ShaderProgram();
		shaderProgram.createVertexShader( Utils.loadResource( GL02_VERTEX_SHADER ) );
		shaderProgram.createFragmentShader( Utils.loadResource( GL02_FRAGMENT_SHADER ) );
		shaderProgram.link();
		shaderProgram.createUniform( VERTEX_SHADER_PARAM_TRANSFORM );

		float[] positionsInWorldSpace = new float[] 
		{ 
			0.0f, 0.0f, 0.0f,
			2.0f, 0.0f, 0.0f,
			0.0f, 1.0f, 0.0f,

			1.0f, 0.0f, -1.0f,
			3.0f, 0.0f, -1.0f,
			1.0f, 1.0f, -1.0f,

			-990.0f,   0.0f, -900.0f,
			 900.0f,   0.0f, -900.0f,
			-900.0f, 900.0f, -900.0f,
			 900.0f, 900.0f, -900.0f,
		};
		float[] colours = new float[]
		{
			0.7f, 0.2f, 0.2f, 
			0.7f, 0.2f, 0.2f, 
			0.7f, 0.2f, 0.2f,

			0.2f, 0.7f, 0.2f, 
			0.2f, 0.7f, 0.2f, 
			0.2f, 0.7f, 0.2f,

			0.8f, 0.8f, 0.95f, 
			0.8f, 0.8f, 0.95f, 
			0.8f, 0.8f, 0.95f,
			0.8f, 0.8f, 0.95f,
		};
		int[] indices = new int[] { 0,1,2, 3,4,5, 6,7,8,7,8,9 };
		mesh = new Mesh( positionsInWorldSpace,colours,indices );

		float aspectRatio = (float)windowWidth / windowHeight;
		this.projectionMatrix = new Matrix4f().setPerspective( Renderer.FOV,aspectRatio,Renderer.Z_NEAR,Renderer.Z_FAR );
	}

	public void render()
	{
		long currentRenderTimestamp = System.currentTimeMillis();
		if ( prevRenderTimestamp>0 )
		{
			renderWithElapsedTime( currentRenderTimestamp-prevRenderTimestamp );
		}
		else
		{
			renderWithElapsedTime( 0L );
		}
		prevRenderTimestamp = currentRenderTimestamp;
	}

	public void renderWithElapsedTime( long longDeltaTime )
	{
		glClear( GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT );

//		if ( window.isResized() )
//		{
//			glViewport( 0,0,window.getWidth(),window.getHeight() );
//			window.setResized( false );
//		}

		renderTimestampFromStart += longDeltaTime;
		double doubleDeltaTime = renderTimestampFromStart/500.0d;
		Vector3fc eye = new Vector3f( 1.0f+(float)Math.sin( doubleDeltaTime )/5.0f,(float)Math.cos( doubleDeltaTime )/10.0f,3.0f );
//		Vector3fc eye = new Vector3f( 0.0f,0.0f,3.0f );
		Vector3fc center = new Vector3f( 1.0f,0.0f,0.0f );
		Vector3fc up = new Vector3f( 0.0f,1.0f,0.0f );
		Matrix4f viewMatrix = new Matrix4f().setLookAt( eye,center,up );

		// Vclip = Mprojection x Mview x Mmodel x Vlocal
		projectionMatrix.mul( viewMatrix,allTransformMatrix );

		shaderProgram.bind();
		shaderProgram.setUniform( VERTEX_SHADER_PARAM_TRANSFORM,allTransformMatrix );

		// Bind to the VAO
		glBindVertexArray( mesh.getVaoId() );

		// Draw the vertices
		glDrawElements( GL_TRIANGLES,mesh.getVertexCount(),GL_UNSIGNED_INT,0 );

		// Restore state
		glBindVertexArray( 0 );

		shaderProgram.unbind();
	}

	public void cleanup()
	{
		if ( shaderProgram!=null )
			shaderProgram.cleanup();

		if ( mesh!=null )
			mesh.cleanUp();
	}

//	public static void main( String[] args )
//	{
//		for ( int ic=0; ic<2000; ic+=10 )
//		{
//			float sinVal = (float)Math.sin( ic/100.0d )/10.0f;
//			System.out.println( String.format( "ic=%d,sin(%f)",ic,sinVal ) );
//		}
//	}
}