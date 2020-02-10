package test.gl02;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.ARBFramebufferObject.*;
import static org.lwjgl.opengl.ARBTextureMultisample.*;

import java.nio.ByteBuffer;

public class FBOsForOneEye
{
	private int m_nRenderFramebufferId = 0;
		private int m_nDepthBufferId = 0;
		private int m_nRenderTextureId = 0;

	private int m_nResolveFramebufferId = 0;
		private int m_nResolveTextureId = 0;

	private int nWidth,nHeight;

	public void createFBOs( int nWidth,int nHeight )
	{
		this.nWidth = nWidth;
		this.nHeight = nHeight;

		int samples = glGetInteger( GL_MAX_SAMPLES );
		System.err.println( "Using " + samples + "x multisampling" );

		m_nRenderFramebufferId = glGenFramebuffers();
		glBindFramebuffer( GL_FRAMEBUFFER,m_nRenderFramebufferId );
		{
//			m_nRenderTextureId = glGenRenderbuffers();
//			glBindRenderbuffer( GL_RENDERBUFFER,m_nRenderTextureId );
//			glRenderbufferStorageMultisample( GL_RENDERBUFFER,samples,GL_RGBA8,nWidth,nHeight );
//			glFramebufferRenderbuffer( GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_RENDERBUFFER,m_nRenderTextureId );

			m_nRenderTextureId = glGenTextures();
			glBindTexture( GL_TEXTURE_2D_MULTISAMPLE,m_nRenderTextureId );
			glTexImage2DMultisample( GL_TEXTURE_2D_MULTISAMPLE,samples,GL_RGBA8,nWidth,nHeight,true );
			glFramebufferTexture2D( GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D_MULTISAMPLE,m_nRenderTextureId,0 );

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

		m_nResolveFramebufferId = glGenFramebuffers();
		glBindFramebuffer( GL_FRAMEBUFFER,m_nResolveFramebufferId );
		{
			m_nResolveTextureId = glGenTextures();
			glBindTexture( GL_TEXTURE_2D,m_nResolveTextureId );
			glTexParameteri( GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR ); // we also want to sample this texture later
			glTexParameteri( GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR ); // we also want to sample this texture later
//			glTexParameteri( GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR );
//			glTexParameteri( GL_TEXTURE_2D,GL_TEXTURE_MAX_LEVEL,0 );
			glTexImage2D( GL_TEXTURE_2D,0,GL_RGBA8,nWidth,nHeight,0,GL_RGBA,GL_UNSIGNED_BYTE,(ByteBuffer)null );
			glFramebufferTexture2D( GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,m_nResolveTextureId,0 );

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

		glDeleteTextures( this.m_nResolveTextureId );
		glDeleteFramebuffers( this.m_nResolveFramebufferId );
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
	public int getResolveFramebufferId()
	{
		return m_nResolveFramebufferId;
	}
	public int getResolveTextureId()
	{
		return m_nResolveTextureId;
	}
}
