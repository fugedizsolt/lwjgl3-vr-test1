package test.gl02;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.ARBFramebufferObject.*;
import static org.lwjgl.opengl.ARBTextureMultisample.*;

public class FBOsForOneEye
{
	public void createFBOs( int nWidth,int nHeight )
	{
		int m_nRenderFramebufferId = glGenFramebuffers();
		glBindFramebuffer( GL_FRAMEBUFFER,m_nRenderFramebufferId );

		int m_nDepthBufferId = glGenRenderbuffers();
		glBindRenderbuffer( GL_RENDERBUFFER,m_nDepthBufferId );
		glRenderbufferStorageMultisample( GL_RENDERBUFFER,4,GL_DEPTH_COMPONENT,nWidth,nHeight );
		glFramebufferRenderbuffer( GL_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_RENDERBUFFER,m_nDepthBufferId );

		int m_nRenderTextureId = glGenTextures();
		glBindTexture( GL_TEXTURE_2D_MULTISAMPLE,m_nRenderTextureId );
		glTexImage2DMultisample( GL_TEXTURE_2D_MULTISAMPLE,4,GL_RGBA8,nWidth,nHeight,true );
		glFramebufferTexture2D( GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D_MULTISAMPLE,m_nRenderTextureId,0 );

		int m_nResolveFramebufferId = glGenFramebuffers();
		glBindFramebuffer( GL_FRAMEBUFFER,m_nResolveFramebufferId );

		int m_nResolveTextureId = glGenTextures();
		glBindTexture( GL_TEXTURE_2D,m_nResolveTextureId );
		glTexParameteri( GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR );
		glTexParameteri( GL_TEXTURE_2D,GL_TEXTURE_MAX_LEVEL,0 );
		glTexImage2D( GL_TEXTURE_2D,0,GL_RGBA8,nWidth,nHeight,0,GL_RGBA,GL_UNSIGNED_BYTE,(ByteBuffer)null );
		glFramebufferTexture2D( GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,m_nResolveTextureId,0 );

		int fboStatus = glCheckFramebufferStatus( GL_FRAMEBUFFER );
		if ( fboStatus != GL_FRAMEBUFFER_COMPLETE ) throw new AssertionError( "Could not create FBO: " + fboStatus );

		glBindRenderbuffer( GL_RENDERBUFFER,0 );
		glBindFramebuffer( GL_FRAMEBUFFER,0 );
	}

}
