package testvr02;

import static org.lwjgl.opengl.ARBFramebufferObject.GL_DRAW_FRAMEBUFFER;
import static org.lwjgl.opengl.ARBFramebufferObject.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.ARBFramebufferObject.GL_READ_FRAMEBUFFER;
import static org.lwjgl.opengl.ARBFramebufferObject.glBindFramebuffer;
import static org.lwjgl.opengl.ARBFramebufferObject.glBlitFramebuffer;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

import org.joml.Matrix4f;
import org.joml.Vector4f;

public class Renderer
{
	private static final String GL02_VERTEX_SHADER = "/gl02-vertex.vs";
	private static final String GL02_FRAGMENT_SHADER = "/gl02-fragment.fs";

	public static final float Z_NEAR = 0.01f;
	public static final float Z_FAR = 1000.f;

	private static final String VERTEX_SHADER_PARAM_TRANSFORM = "transform";
//	private static final float FOV = (float) Math.toRadians(45.0f);
//	private final Matrix4f allTransformMatrixEye1 = new Matrix4f();
//	private final Matrix4f allTransformMatrixEye2 = new Matrix4f();
//	private final Matrix4f viewMatrixEye1 = new Matrix4f();
//	private final Matrix4f viewMatrixEye2 = new Matrix4f();
//	private Matrix4f projectionMatrix;

	private Matrix4f vrTransformMatrixEye1 = null;
	private Matrix4f vrTransformMatrixEye2 = null;
	private long prevRenderTimestamp = 0;

	private long renderTimestampFromStart = 0;

	private ShaderProgram shaderProgram = null;
	private Mesh meshForTriangles = null;
	private Mesh meshForCoordLines = null;
	private FBOsForTwoEyes fbo = null;
//	private long lastTime;


	public Renderer()
	{
	}

	public void init( int windowWidth,int windowHeight ) throws Exception
	{
//		this.lastTime = System.nanoTime();

		fbo = new FBOsForTwoEyes();
		fbo.createFBOs( windowWidth,windowHeight );

//		glEnable( GL_MULTISAMPLE );
		glEnable( GL_DEPTH_TEST );

		shaderProgram = new ShaderProgram();
		shaderProgram.createVertexShader( Utils.loadResource( GL02_VERTEX_SHADER ) );
		shaderProgram.createFragmentShader( Utils.loadResource( GL02_FRAGMENT_SHADER ) );
		shaderProgram.link();
		shaderProgram.createUniform( VERTEX_SHADER_PARAM_TRANSFORM );

		{
			float[] positionsInWorldSpace = new float[] 
			{ 
				0.0f, 0.0f, -3.0f,
				2.0f, 0.0f, -3.0f,
				0.0f, 1.0f, -3.0f,

				1.0f, 0.0f, -4.0f,
				3.0f, 0.0f, -4.0f,
				1.0f, 1.0f, -4.0f,

				-990.0f,   0.0f, -900.0f,
				 900.0f,   0.0f, -900.0f,
				-900.0f, 900.0f, -900.0f,
				 900.0f, 900.0f, -900.0f,

				0.0f, 0.0f, -900.0f,
				0.0f, 0.0f,  900.0f,
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

				0.8f, 0.8f, 0.8f,
				0.8f, 0.8f, 0.8f,
			};
			int[] indices = new int[] { 0,1,2, 3,4,5, 6,7,8,7,8,9, 10,11 };
			meshForTriangles = new Mesh( positionsInWorldSpace,colours,indices );
		}

		{
			float[] positionsInWorldSpace = new float[] 
			{ 
				0.0f, 0.0f, -900.0f,
				0.0f, 0.0f,    3.0f,

				2.0f, 0.0f, -900.0f,
				2.0f, 0.0f,    3.0f,
			};
			float[] colours = new float[]
			{
				0.0f, 0.0f, 0.0f,
				0.0f, 0.0f, 0.0f,

				0.0f, 0.0f, 0.0f,
				0.0f, 0.0f, 0.0f,
			};
			int[] indices = new int[] { 0,1, 2,3, };
			meshForCoordLines = new Mesh( positionsInWorldSpace,colours,indices );
		}

//		float aspectRatio = (float)(windowWidth) / windowHeight;
//		this.projectionMatrix = new Matrix4f().setPerspective( Renderer.FOV,aspectRatio,Renderer.Z_NEAR,Renderer.Z_FAR );
	}

	public void render( Matrix4f paramMatEye1,Matrix4f paramMatEye2 )
	{
		this.vrTransformMatrixEye1 = paramMatEye1;
		this.vrTransformMatrixEye2 = paramMatEye2;

		if ( ManagerGLFW.printCoords==true )
		{
			Vector4f vec = new Vector4f( 0.0f,1.0f,-2.0f,1.0f );
			Vector4f vecEye1Result = new Vector4f();
			Vector4f vecEye2Result = new Vector4f();
			vec.mul( vrTransformMatrixEye1,vecEye1Result );
			vec.mul( vrTransformMatrixEye2,vecEye2Result );
			System.out.println( String.format( "vecEye1Result (%.5f,%.5f,%.5f)",vecEye1Result.x,vecEye1Result.y,vecEye1Result.z ) );
			System.out.println( String.format( "vecEye2Result (%.5f,%.5f,%.5f)",vecEye2Result.x,vecEye2Result.y,vecEye2Result.z ) );
		}

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
//		renderTimestampFromStart += longDeltaTime;
//		double doubleDeltaTime = renderTimestampFromStart/1000.0d;
//		Vector3fc eye1 = new Vector3f( 1.0f+(float)Math.sin( doubleDeltaTime )/5.0f,1.0f+(float)Math.cos( doubleDeltaTime )/10.0f,10.0f );
//		Vector3fc eye2 = new Vector3f( 1.5f+(float)Math.sin( doubleDeltaTime )/5.0f,1.0f+(float)Math.cos( doubleDeltaTime )/10.0f,10.0f );
////		Vector3fc eye = new Vector3f( 0.0f,0.0f,3.0f );
//		Vector3fc center = new Vector3f( 0.0f,0.0f,-900.0f );
//		Vector3fc up = new Vector3f( 0.0f,1.0f,0.0f );
//		viewMatrixEye1.setLookAt( eye1,center,up );
//		viewMatrixEye2.setLookAt( eye2,center,up );
//
//		// Vclip = Mprojection x Mview x Mmodel x Vlocal
//		projectionMatrix.mul( viewMatrixEye1,allTransformMatrixEye1 );
//		projectionMatrix.mul( viewMatrixEye2,allTransformMatrixEye2 );

		renderToFrameBuffer( this.vrTransformMatrixEye1 );
		fillTextureWithFrameBufferdata( fbo.getFramebufferIdEye1() );
		renderToFrameBuffer( this.vrTransformMatrixEye2 );
		fillTextureWithFrameBufferdata( fbo.getFramebufferIdEye2() );
	}

	private void renderToFrameBuffer( Matrix4f allTransformMatrix )
	{
		glEnable( GL_MULTISAMPLE );
		glBindFramebuffer( GL_FRAMEBUFFER,fbo.getRenderFramebufferId() );

		renderContent( allTransformMatrix );

		glBindFramebuffer( GL_FRAMEBUFFER,0 );
		glDisable( GL_MULTISAMPLE );
	}

	private void fillTextureWithFrameBufferdata( int fboId )
	{
		glBindFramebuffer( GL_READ_FRAMEBUFFER,fbo.getRenderFramebufferId() );
		glBindFramebuffer( GL_DRAW_FRAMEBUFFER,fboId );
		glBlitFramebuffer( 0,0,fbo.getWidth(),fbo.getHeight(),0,0,fbo.getWidth(),fbo.getHeight(),GL_COLOR_BUFFER_BIT,GL_NEAREST );
		glBindFramebuffer( GL_FRAMEBUFFER,0 );
	}

	public void renderContent( Matrix4f allTransformMatrix )
	{
		glViewport( 0,0,fbo.getWidth(),fbo.getHeight() );

		glClearColor( 0.4f,0.4f,0.4f,0.0f );
		glClear( GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT );

//		if ( window.isResized() )
//		{
//			glViewport( 0,0,window.getWidth(),window.getHeight() );
//			window.setResized( false );
//		}

		shaderProgram.bind();
		shaderProgram.setUniform( VERTEX_SHADER_PARAM_TRANSFORM,allTransformMatrix );

		// Bind to the VAO
		glBindVertexArray( meshForTriangles.getVaoId() );
		// Draw the vertices
		glDrawElements( GL_TRIANGLES,meshForTriangles.getVertexCount(),GL_UNSIGNED_INT,0 );

		// Restore state
		glBindVertexArray( 0 );

		// Bind to the VAO
		glBindVertexArray( meshForCoordLines.getVaoId() );
		// Draw the vertices
		glDrawElements( GL_LINES,meshForCoordLines.getVertexCount(),GL_UNSIGNED_INT,0 );

		// Restore state
		glBindVertexArray( 0 );

		shaderProgram.unbind();
	}

	public void cleanup()
	{
		if ( shaderProgram!=null )
			shaderProgram.cleanup();

		if ( meshForTriangles!=null )
			meshForTriangles.cleanUp();
		if ( meshForCoordLines!=null )
			meshForCoordLines.cleanUp();

		if ( fbo!=null )
			fbo.cleanUp();
	}

	public FBOsForTwoEyes getFbo()
	{
		return fbo;
	}
}
