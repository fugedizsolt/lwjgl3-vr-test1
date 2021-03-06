package testvr02;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.ARBFramebufferObject.*;

import java.nio.ByteBuffer;

public class FBOsForTwoEyes
{
	private int m_nRenderFramebufferId = 0;
		private int m_nDepthBufferId = 0;
		private int m_nRenderTextureId = 0;

	private int m_nFramebufferIdEye1 = 0;
		private int m_nTextureIdEye1 = 0;

	private int m_nFramebufferIdEye2 = 0;
		private int m_nTextureIdEye2 = 0;

	private int nWidth,nHeight;

	public void createFBOs( int paramWidth,int paramHeight )
	{
		this.nWidth = paramWidth;
		this.nHeight = paramHeight;

		int samples = glGetInteger( GL_MAX_SAMPLES );
		System.err.println( "Using " + samples + "x multisampling" );

		m_nRenderFramebufferId = glGenFramebuffers();
		glBindFramebuffer( GL_FRAMEBUFFER,m_nRenderFramebufferId );
		{
			m_nRenderTextureId = glGenRenderbuffers();
			glBindRenderbuffer( GL_RENDERBUFFER,m_nRenderTextureId );
			glRenderbufferStorageMultisample( GL_RENDERBUFFER,samples,GL_RGBA8,nWidth,nHeight );
			glFramebufferRenderbuffer( GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_RENDERBUFFER,m_nRenderTextureId );

//			m_nRenderTextureId = glGenTextures();
//			glBindTexture( GL_TEXTURE_2D_MULTISAMPLE,m_nRenderTextureId );
//			glTexImage2DMultisample( GL_TEXTURE_2D_MULTISAMPLE,samples,GL_RGBA8,nWidth,nHeight,true );
//			glFramebufferTexture2D( GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D_MULTISAMPLE,m_nRenderTextureId,0 );

			m_nDepthBufferId = glGenRenderbuffers();
			glBindRenderbuffer( GL_RENDERBUFFER,m_nDepthBufferId );
//			glRenderbufferStorageMultisample( GL_RENDERBUFFER,samples,GL_DEPTH24_STENCIL8,nWidth,nHeight );
			glRenderbufferStorageMultisample( GL_RENDERBUFFER,samples,GL_DEPTH_COMPONENT,nWidth,nHeight );
			glFramebufferRenderbuffer( GL_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_RENDERBUFFER,m_nDepthBufferId );

			int fboStatus = glCheckFramebufferStatus( GL_FRAMEBUFFER );
			if ( fboStatus != GL_FRAMEBUFFER_COMPLETE ) throw new AssertionError( "Could not create FBO: " + fboStatus );

			glBindRenderbuffer( GL_RENDERBUFFER,0 );
			glBindFramebuffer( GL_FRAMEBUFFER,0 );
		}

		m_nFramebufferIdEye1 = glGenFramebuffers();
		glBindFramebuffer( GL_FRAMEBUFFER,m_nFramebufferIdEye1 );
		{
			m_nTextureIdEye1 = glGenTextures();
			glBindTexture( GL_TEXTURE_2D,m_nTextureIdEye1 );
			glTexParameteri( GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR ); // we also want to sample this texture later
			glTexParameteri( GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR ); // we also want to sample this texture later
//			glTexParameteri( GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR );
//			glTexParameteri( GL_TEXTURE_2D,GL_TEXTURE_MAX_LEVEL,0 );
			glTexImage2D( GL_TEXTURE_2D,0,GL_RGBA8,nWidth,nHeight,0,GL_RGBA,GL_UNSIGNED_BYTE,(ByteBuffer)null );
			glFramebufferTexture2D( GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,m_nTextureIdEye1,0 );

			int fboStatus = glCheckFramebufferStatus( GL_FRAMEBUFFER );
			if ( fboStatus != GL_FRAMEBUFFER_COMPLETE ) throw new AssertionError( "Could not create FBO: " + fboStatus );

			glBindRenderbuffer( GL_RENDERBUFFER,0 );
			glBindFramebuffer( GL_FRAMEBUFFER,0 );
		}

		m_nFramebufferIdEye2 = glGenFramebuffers();
		glBindFramebuffer( GL_FRAMEBUFFER,m_nFramebufferIdEye2 );
		{
			m_nTextureIdEye2 = glGenTextures();
			glBindTexture( GL_TEXTURE_2D,m_nTextureIdEye2 );
			glTexParameteri( GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR ); // we also want to sample this texture later
			glTexParameteri( GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR ); // we also want to sample this texture later
//			glTexParameteri( GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR );
//			glTexParameteri( GL_TEXTURE_2D,GL_TEXTURE_MAX_LEVEL,0 );
			glTexImage2D( GL_TEXTURE_2D,0,GL_RGBA8,nWidth,nHeight,0,GL_RGBA,GL_UNSIGNED_BYTE,(ByteBuffer)null );
			glFramebufferTexture2D( GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,m_nTextureIdEye2,0 );

			int fboStatus = glCheckFramebufferStatus( GL_FRAMEBUFFER );
			if ( fboStatus != GL_FRAMEBUFFER_COMPLETE ) throw new AssertionError( "Could not create FBO: " + fboStatus );

			glBindRenderbuffer( GL_RENDERBUFFER,0 );
			glBindFramebuffer( GL_FRAMEBUFFER,0 );
		}
	}

	public void cleanUp()
	{
		glDeleteRenderbuffers( this.m_nDepthBufferId );
		glDeleteRenderbuffers( this.m_nRenderTextureId );
		glDeleteFramebuffers( this.m_nRenderFramebufferId );

		glDeleteTextures( this.m_nTextureIdEye1 );
		glDeleteFramebuffers( this.m_nFramebufferIdEye1 );

		glDeleteTextures( this.m_nTextureIdEye2 );
		glDeleteFramebuffers( this.m_nFramebufferIdEye2 );
	}

	public int getWidth()
	{
		return nWidth;
	}
	public int getHeight()
	{
		return nHeight;
	}
	public int getRenderFramebufferId()
	{
		return m_nRenderFramebufferId;
	}
	public int getFramebufferIdEye1()
	{
		return m_nFramebufferIdEye1;
	}
	public int getTextureIdEye1()
	{
		return m_nTextureIdEye1;
	}
	public int getFramebufferIdEye2()
	{
		return m_nFramebufferIdEye2;
	}
	public int getTextureIdEye2()
	{
		return m_nTextureIdEye2;
	}
}
