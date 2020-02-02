package test.gl02;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

import org.joml.Matrix4f;

public class Renderer
{
	private static final String VERTEX_SHADER_PARAM_TRANSFORM = "transform";
	private static final float FOV = (float) Math.toRadians(60.0f);
	private static final float Z_NEAR = 0.01f;
	private static final float Z_FAR = 1000.f;
	private Matrix4f projectionMatrix;

	private ShaderProgram shaderProgram = null;
	private Mesh mesh = null;


	public Renderer()
	{
	}

	public void init( int windowWidth,int windowHeight ) throws Exception
	{
		shaderProgram = new ShaderProgram();
		shaderProgram.createVertexShader( Utils.loadResource( "/vertex.vs" ) );
		shaderProgram.createFragmentShader( Utils.loadResource( "/fragment.fs" ) );
		shaderProgram.link();
		shaderProgram.createUniform( VERTEX_SHADER_PARAM_TRANSFORM );

		float[] positions = new float[] 
		{ 
			-0.5f, 0.5f, -1.05f, 
			-0.5f, -0.5f, -1.05f, 
			0.5f, -0.5f, -1.05f, 
			0.5f, 0.5f, -1.05f
		};
		float[] colours = new float[]
		{
			0.2f, 0.0f, 0.0f, 
			0.0f, 0.5f, 0.0f, 
			0.0f, 0.0f, 0.8f, 
			0.0f, 0.5f, 0.5f, };
		int[] indices = new int[] { 0, 1, 3, 3, 1, 2, };
		mesh = new Mesh( positions,colours,indices );

		float aspectRatio = (float)windowWidth / windowHeight;
		projectionMatrix = new Matrix4f().setPerspective( Renderer.FOV,aspectRatio,Renderer.Z_NEAR,Renderer.Z_FAR );
	}

	public void render()
	{
		glClear( GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT );

//		if ( window.isResized() )
//		{
//			glViewport( 0,0,window.getWidth(),window.getHeight() );
//			window.setResized( false );
//		}

		shaderProgram.bind();
		shaderProgram.setUniform( VERTEX_SHADER_PARAM_TRANSFORM,projectionMatrix );

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
}
