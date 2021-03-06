package test.gl02;

import static org.lwjgl.opengl.ARBFramebufferObject.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class Renderer
{
	private static final String GL02_VERTEX_SHADER = "/gl02-vertex.vs";
	private static final String GL02_FRAGMENT_SHADER = "/gl02-fragment.fs";

	private static final String VERTEX_SHADER_PARAM_TRANSFORM = "transform";
	private static final float FOV = (float) Math.toRadians(45.0f);
	private static final float Z_NEAR = 0.01f;
	private static final float Z_FAR = 1000.f;
	private final Matrix4f allTransformMatrixEye1 = new Matrix4f();
	private final Matrix4f allTransformMatrixEye2 = new Matrix4f();
	private final Matrix4f viewMatrixEye1 = new Matrix4f();
	private final Matrix4f viewMatrixEye2 = new Matrix4f();
	private Matrix4f projectionMatrix;
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
		fbo.createFBOs( windowWidth/2,windowHeight );

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

		float aspectRatio = (float)(windowWidth/2) / windowHeight;
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
		renderTimestampFromStart += longDeltaTime;
		double doubleDeltaTime = renderTimestampFromStart/1000.0d;
		Vector3fc eye1 = new Vector3f( 1.0f+(float)Math.sin( doubleDeltaTime )/5.0f,1.0f+(float)Math.cos( doubleDeltaTime )/10.0f,10.0f );
		Vector3fc eye2 = new Vector3f( 1.5f+(float)Math.sin( doubleDeltaTime )/5.0f,1.0f+(float)Math.cos( doubleDeltaTime )/10.0f,10.0f );
//		Vector3fc eye = new Vector3f( 0.0f,0.0f,3.0f );
		Vector3fc center = new Vector3f( 0.0f,0.0f,-900.0f );
		Vector3fc up = new Vector3f( 0.0f,1.0f,0.0f );
		viewMatrixEye1.setLookAt( eye1,center,up );
		viewMatrixEye2.setLookAt( eye2,center,up );

		// Vclip = Mprojection x Mview x Mmodel x Vlocal
		projectionMatrix.mul( viewMatrixEye1,allTransformMatrixEye1 );
		projectionMatrix.mul( viewMatrixEye2,allTransformMatrixEye2 );

//		render2();

		renderToFrameBuffer( allTransformMatrixEye1 );
		fillTextureWithFrameBufferdata( fbo.getFramebufferIdEye1() );
		renderToFrameBuffer( allTransformMatrixEye2 );
		fillTextureWithFrameBufferdata( fbo.getFramebufferIdEye2() );

//		renderTestTextureToFBOEye1();
//		fillTextureWithFrameBufferdata( fbo.getFramebufferIdEye1() );
//		renderTestTextureToFBOEye2();
//		fillTextureWithFrameBufferdata( fbo.getFramebufferIdEye2() );

		renderTextureFBOsToScreen();
	}

	private void renderTestTextureToFBOEye1()
	{
		glViewport( 0,0,fbo.getWidth(),fbo.getHeight() );

		glBindFramebuffer( GL_FRAMEBUFFER,fbo.getRenderFramebufferId() );

		glClearColor( 0.4f,0.4f,0.4f,0.0f );
		glClear( GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT );

		glMatrixMode( GL_PROJECTION );
		glLoadIdentity();
//		float aspectRatio = (float)fbo.getWidth() / fbo.getHeight();
//		glOrtho( -1.0f * aspectRatio,+1.0f * aspectRatio,-1.0f,+1.0f,-1.0f,+1.0f );
		glMatrixMode( GL_MODELVIEW );
		glLoadIdentity();

		glColor3f( 0.0f,0.0f,0.0f );
		glBegin( GL_LINES );
		glVertex2f( -1f,-1f );
		glVertex2f(  1f, 1f );
		glEnd();

		glBindFramebuffer( GL_FRAMEBUFFER,0 );
	}

	private void renderTestTextureToFBOEye2()
	{
		glViewport( 0,0,fbo.getWidth(),fbo.getHeight() );

		glBindFramebuffer( GL_FRAMEBUFFER,fbo.getRenderFramebufferId() );

		glClearColor( 0.4f,0.4f,0.4f,0.0f );
		glClear( GL_COLOR_BUFFER_BIT );

		glMatrixMode( GL_PROJECTION );
		glLoadIdentity();
//		float aspectRatio = (float)fbo.getWidth() / fbo.getHeight();
//		glOrtho( -1.0f * aspectRatio,+1.0f * aspectRatio,-1.0f,+1.0f,-1.0f,+1.0f );

		glMatrixMode( GL_MODELVIEW );
		glLoadIdentity();

		glColor3f( 0.0f,0.0f,0.0f );

		glBegin( GL_QUADS );
		glVertex2f( -1f,-1f );
		glVertex2f(  1f,-1f );
		glVertex2f(  1f, 1f );
		glVertex2f( -1f, 1f );
		glEnd();

		glBindFramebuffer( GL_FRAMEBUFFER,0 );
	}

//	private void render2()
//	{
//		glClear( GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT );
//		glViewport( 0,0,Main.WIDTH,Main.HEIGHT );
//
//		long thisTime = System.nanoTime();
//		float elapsed = (lastTime - thisTime) / 1E9f;
//
//		/* Simple orthographic projection */
//		float aspect = (float)Main.WIDTH/Main.HEIGHT;
//		glMatrixMode( GL_PROJECTION );
//		glLoadIdentity();
//		glOrtho( -1.0f * aspect,+1.0f * aspect,-1.0f,+1.0f,-1.0f,+1.0f );
//
//		/* Rotate a bit and draw a quad */
//		glMatrixMode( GL_MODELVIEW );
//		glRotatef( elapsed * 2,0,0,1 );
//		glBegin( GL_QUADS );
//		glVertex2f( -0.5f,-0.5f );
//		glVertex2f( +0.5f,-0.5f );
//		glVertex2f( +0.5f,+0.5f );
//		glVertex2f( -0.5f,+0.5f );
//		glEnd();
//	}

	private void renderTextureFBOsToScreen()
	{
		glClearColor( 0.4f,0.4f,0.4f,0.0f );
		glClear( GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT );

		glMatrixMode( GL_PROJECTION );
		glLoadIdentity();
		glMatrixMode( GL_MODELVIEW );
		glLoadIdentity();

		glViewport( 0,0,2*fbo.getWidth(),fbo.getHeight() );

		glEnable( GL_TEXTURE_2D );
		glTexEnvi( GL_TEXTURE_ENV,GL_TEXTURE_ENV_MODE,GL_REPLACE );

		glBindTexture( GL_TEXTURE_2D,fbo.getTextureIdEye1() );
		glBegin( GL_QUADS );
		glTexCoord2f( 0,0 );	glVertex2f( -1,-1 );
		glTexCoord2f( 1,0 );	glVertex2f(  0,-1 );
		glTexCoord2f( 1,1 );	glVertex2f(  0, 1 );
		glTexCoord2f( 0,1 );	glVertex2f( -1, 1 );
		glEnd();

		glBindTexture( GL_TEXTURE_2D,fbo.getTextureIdEye2() );
		glBegin( GL_QUADS );
		glTexCoord2f( 0,0 );	glVertex2f(  0,-1 );
		glTexCoord2f( 1,0 );	glVertex2f(  1,-1 );
		glTexCoord2f( 1,1 );	glVertex2f(  1, 1 );
		glTexCoord2f( 0,1 );	glVertex2f(  0, 1 );
		glEnd();

		glDisable( GL_TEXTURE_2D );
	}

	private void renderTestToScreen()
	{
		glClearColor( 0.4f,0.4f,0.4f,0.0f );
		glClear( GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT );

		glMatrixMode( GL_PROJECTION );
		glLoadIdentity();
		glMatrixMode( GL_MODELVIEW );
		glLoadIdentity();

		glViewport( 0,0,2*fbo.getWidth(),fbo.getHeight() );
		glColor3f( 0.0f,0.0f,0.0f );
		glBegin( GL_LINES );
		glVertex2f( -0.95f,-0.95f );
		glVertex2f( -0.05f, 0.95f );
		glEnd();

		glBegin( GL_QUADS );
		glVertex2f( 0.05f,-0.95f );
		glVertex2f( 0.95f,-0.95f );
		glVertex2f( 0.95f,0.95f );
		glVertex2f( 0.05f,0.95f );
		glEnd();
	}

	private void fillTextureWithFrameBufferdata( int fboId )
	{
		glBindFramebuffer( GL_READ_FRAMEBUFFER,fbo.getRenderFramebufferId() );
		glBindFramebuffer( GL_DRAW_FRAMEBUFFER,fboId );
		glBlitFramebuffer( 0,0,fbo.getWidth(),fbo.getHeight(),0,0,fbo.getWidth(),fbo.getHeight(),GL_COLOR_BUFFER_BIT,GL_NEAREST );
		glBindFramebuffer( GL_FRAMEBUFFER,0 );
	}

	private void renderToFrameBuffer( Matrix4f allTransformMatrix )
	{
		glBindFramebuffer( GL_FRAMEBUFFER,fbo.getRenderFramebufferId() );

		renderContent( allTransformMatrix );
//		renderContent2();

		glBindFramebuffer( GL_FRAMEBUFFER,0 );
	}

//	private void renderContent2()
//	{
//		glClearColor( 0.4f,0.4f,0.4f,0.0f );
////		glEnable( GL_DEPTH_TEST );
//
//		glClear( GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT );
////		glViewport( 0,0,Main.WIDTH,Main.HEIGHT );
//
//		long thisTime = System.nanoTime();
//		float elapsed = (lastTime - thisTime) / 1E9f;
//
//		/* Simple orthographic projection */
//		float aspect = (float)Main.WIDTH/Main.HEIGHT;
//		glMatrixMode( GL_PROJECTION );
//		glLoadIdentity();
//		glOrtho( -1.0f * aspect,+1.0f * aspect,-1.0f,+1.0f,-1.0f,+1.0f );
//
//		/* Rotate a bit and draw a quad */
//		glMatrixMode( GL_MODELVIEW );
//		glRotatef( elapsed * 2,0,0,1 );
//		glBegin( GL_QUADS );
//		glVertex2f( -0.5f,-0.5f );
//		glVertex2f( +0.5f,-0.5f );
//		glVertex2f( +0.5f,+0.5f );
//		glVertex2f( -0.5f,+0.5f );
//		glEnd();
//	}

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

		if ( fbo!=null )
			fbo.cleanUp();
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
