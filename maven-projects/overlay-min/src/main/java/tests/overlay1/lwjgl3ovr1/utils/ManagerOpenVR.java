package tests.overlay1.lwjgl3ovr1.utils;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import org.lwjgl.openvr.OpenVR;
import org.lwjgl.openvr.Texture;
import org.lwjgl.openvr.VR;
import org.lwjgl.openvr.VREvent;
import org.lwjgl.openvr.VROverlay;
import org.lwjgl.openvr.VRSystem;
import org.lwjgl.openvr.VRTextureBounds;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import tests.overlay1.lwjgl3ovr1.Main1;


public class ManagerOpenVR implements AutoCloseable
{
	private final int openVRtoken;
	private final VREvent event;
	private int vrrc;				// return code from VR func
	private long lHandleOverlay;	// handle from VR func

	private final LongBuffer pHandleOverlay = MemoryUtil.memAllocLong( 1 );


	public ManagerOpenVR()
	{
		try ( MemoryStack stack = MemoryStack.stackPush() )
		{
			this.event = VREvent.create();
			this.openVRtoken = createOpenVR( stack );
			listDevices( stack );

			this.vrrc = VROverlay.VROverlay_CreateOverlay( "overlayKey1","overlayName1",pHandleOverlay );
			this.lHandleOverlay = pHandleOverlay.get( 0 );
			Main1.log( "VROverlay_CreateOverlay rc=%d vrHandle=%d",vrrc,this.lHandleOverlay );

			this.vrrc = VROverlay.VROverlay_SetOverlayWidthInMeters( this.lHandleOverlay,0.3f );
			Main1.log( "VROverlay_SetOverlayWidthInMeters rc=%d",vrrc );

			this.vrrc = VROverlay.VROverlay_SetOverlayColor( this.lHandleOverlay,0.1f,0.8f,0.7f );
			Main1.log( "VROverlay_SetOverlayColor rc=%d",vrrc );

			VRTextureBounds pBounds = VRTextureBounds.malloc( stack );
			pBounds.set( 0,0,1,1 );
			this.vrrc = VROverlay.VROverlay_SetOverlayTextureBounds( this.lHandleOverlay,pBounds );
			Main1.log( "VROverlay_SetOverlayTextureBounds rc=%d",vrrc );

			this.vrrc = VROverlay.VROverlay_ShowOverlay( this.lHandleOverlay );
			Main1.log( "VROverlay_ShowOverlay rc=%d",vrrc );
		}
	}

	private int createOpenVR( MemoryStack stack )
	{
		Main1.log( "VR_IsRuntimeInstalled(%b)",VR.VR_IsRuntimeInstalled() );
		Main1.log( "VR_RuntimePath(%s)",VR.VR_RuntimePath() );
		Main1.log( "VR_IsHmdPresent(%b)",VR.VR_IsHmdPresent() );

		IntBuffer peError = stack.mallocInt( 1 );

		int token = 0;
		try
		{
			token = VR.VR_InitInternal( peError,VR.EVRApplicationType_VRApplication_Scene );
			int rc = peError.get( 0 );
			if ( rc!=0 )
			{
				Main1.log( "INIT ERROR SYMBOL (%s)",VR.VR_GetVRInitErrorAsSymbol( rc ) );
				Main1.log( "INIT ERROR  DESCR (%s)",VR.VR_GetVRInitErrorAsEnglishDescription( rc ) );
				throw new RuntimeException( "VR_InitInternal peError.get( 0 ):" + rc );
			}
			if ( token==0 )
				throw new RuntimeException( "token=0" );
			Main1.log( "token=%d",token );

			OpenVR.create( token );
		}
		catch ( Exception exc )
		{
			VR.VR_ShutdownInternal();
			throw exc;
		}
		return token;
	}

	private void listDevices( MemoryStack stack )
	{
		IntBuffer peError = stack.mallocInt( 1 );

		Main1.log( "Model Number (%s)",VRSystem.VRSystem_GetStringTrackedDeviceProperty( VR.k_unTrackedDeviceIndex_Hmd,VR.ETrackedDeviceProperty_Prop_ModelNumber_String,peError ) );
		Main1.log( "Serial Number(%s)",VRSystem.VRSystem_GetStringTrackedDeviceProperty( VR.k_unTrackedDeviceIndex_Hmd,VR.ETrackedDeviceProperty_Prop_SerialNumber_String,peError ) );

		int countBaseStations = 0;
		for ( int ic=VR.k_unTrackedDeviceIndex_Hmd; ic<VR.k_unMaxTrackedDeviceCount; ic++ )
		{
			if ( VRSystem.VRSystem_IsTrackedDeviceConnected( ic )==true )
			{
				int trackedDeviceClass = VRSystem.VRSystem_GetTrackedDeviceClass( ic );
				Main1.log( "id=%d,trackedDeviceClass=%d",ic,trackedDeviceClass );
				Main1.log( "TrackingSystemName(%s)",VRSystem.VRSystem_GetStringTrackedDeviceProperty( ic,VR.ETrackedDeviceProperty_Prop_TrackingSystemName_String,peError ) );
				Main1.log( "ModeLabel(%s)",VRSystem.VRSystem_GetStringTrackedDeviceProperty( ic,VR.ETrackedDeviceProperty_Prop_ModeLabel_String,peError ) );
				Main1.log( "ModelNumber(%s)",VRSystem.VRSystem_GetStringTrackedDeviceProperty( ic,VR.ETrackedDeviceProperty_Prop_ModelNumber_String,peError ) );

				if ( trackedDeviceClass==VR.ETrackedDeviceClass_TrackedDeviceClass_TrackingReference )
					countBaseStations++;
			}
		}
		Main1.log( "countBaseStations=%d",countBaseStations );

		IntBuffer w = stack.mallocInt( 1 );
		IntBuffer h = stack.mallocInt( 1 );
		VRSystem.VRSystem_GetRecommendedRenderTargetSize( w,h );
		Main1.log( "Recommended width : " + w.get( 0 ) );
		Main1.log( "Recommended height: " + h.get( 0 ) );
	}

	public void pollEvents()
	{
		while ( true )
		{
			boolean bHasNext = VRSystem.VRSystem_PollNextEvent( this.event );
			if ( bHasNext==false )
				break;

			int eventType = this.event.eventType();
			int trackedDeviceIndex = this.event.trackedDeviceIndex();
			Main1.log( "event trackedDeviceIndex(\t%03d) eventType(%d)",trackedDeviceIndex,eventType );
		}
	}

	@Override
	public void close() throws Exception
	{
		try
		{
			Main1.log( "calling VR_ShutdownInternal..." );
			VR.VR_ShutdownInternal();
			Main1.log( "VR_ShutdownInternal...end" );
		}
		catch ( Exception exc )
		{
			exc.printStackTrace();
		}
	}

	public int getOpenVRtoken()
	{
		return openVRtoken;
	}

	public void setOverlayTexture( int paramGLTextureId )
	{
//		// Setup a Texture_t object to send in the texture.
//		struct Texture_t tex;
//		tex.eColorSpace = EColorSpace_ColorSpace_Auto;
//		tex.eType = ETextureType_TextureType_OpenGL;
//		tex.handle = (void*)(intptr_t)overlaytexture;
//
//		// Send texture into OpenVR as the overlay.
//		oOverlay->SetOverlayTexture( overlayID, &tex );

		try ( MemoryStack stack = MemoryStack.stackPush() )
		{
			Texture ovrTextureId = Texture.malloc( stack );
			ovrTextureId.set( paramGLTextureId,VR.ETextureType_TextureType_OpenGL,VR.EColorSpace_ColorSpace_Auto );
			this.vrrc = VROverlay.VROverlay_SetOverlayTexture( this.lHandleOverlay,ovrTextureId );
//			Main1.log( "VROverlay_SetOverlayTexture rc=%d",vrrc );
		}
	}
}
