package test;

import java.nio.IntBuffer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.openvr.OpenVR;
import org.lwjgl.openvr.VR;
import org.lwjgl.openvr.VREvent;
import org.lwjgl.openvr.VRSystem;
import org.lwjgl.system.MemoryStack;


public class ManagerOpenVR implements AutoCloseable
{
	private final MemoryStack stack;
	private final int openVRtoken;
	private final VREvent event;
	private final VRDevicePose[] devicePoses;
	private final VRDevice[] devices;

	// offsets for translation and rotation from tracker to world space
	private final Vector3f trackerSpaceOriginToWorldSpaceTranslationOffset = new Vector3f();
	private final Matrix4f trackerSpaceToWorldspaceRotationOffset = new Matrix4f();


	public ManagerOpenVR()
	{
		this.stack = MemoryStack.stackPush();
		try
		{
			this.event = VREvent.create();
			this.devicePoses = new VRDevicePose[VR.k_unMaxTrackedDeviceCount];
			this.devices = new VRDevice[VR.k_unMaxTrackedDeviceCount];

			this.openVRtoken = createOpenVR();
		}
		catch ( Exception exc )
		{
			this.stack.close();
			throw exc;
		}
	}

	private int createOpenVR()
	{
		HelloOpenVR.log( "VR_IsRuntimeInstalled() = " + VR.VR_IsRuntimeInstalled() );
		HelloOpenVR.log( "VR_RuntimePath() = " + VR.VR_RuntimePath() );
		HelloOpenVR.log( "VR_IsHmdPresent() = " + VR.VR_IsHmdPresent() );

		IntBuffer peError = stack.mallocInt( 1 );

		int token = 0;
		try
		{
			token = VR.VR_InitInternal( peError,VR.EVRApplicationType_VRApplication_Scene );
			int rc = peError.get( 0 );
			if ( rc!=0 )
			{
				HelloOpenVR.log( "INIT ERROR SYMBOL: " + VR.VR_GetVRInitErrorAsSymbol( rc ) );
				HelloOpenVR.log( "INIT ERROR  DESCR: " + VR.VR_GetVRInitErrorAsEnglishDescription( rc ) );
				throw new RuntimeException( "VR_InitInternal peError.get( 0 ):" + rc );
			}
			if ( token==0 )
				throw new RuntimeException( "token=0" );
			HelloOpenVR.log( String.format( "token=%d",token ) );

			OpenVR.create( token );
		}
		catch ( Exception exc )
		{
			VR.VR_ShutdownInternal();
			throw exc;
		}
		return token;
	}

	public void listDevices()
	{
		IntBuffer peError = stack.mallocInt( 1 );

		HelloOpenVR.log( "Model Number : " + VRSystem.VRSystem_GetStringTrackedDeviceProperty( VR.k_unTrackedDeviceIndex_Hmd,VR.ETrackedDeviceProperty_Prop_ModelNumber_String,peError ) );
		HelloOpenVR.log( "Serial Number: " + VRSystem.VRSystem_GetStringTrackedDeviceProperty( VR.k_unTrackedDeviceIndex_Hmd,VR.ETrackedDeviceProperty_Prop_SerialNumber_String,peError ) );

		int countBaseStations = 0;
		for ( int ic=VR.k_unTrackedDeviceIndex_Hmd; ic<VR.k_unMaxTrackedDeviceCount; ic++ )
		{
			if ( VRSystem.VRSystem_IsTrackedDeviceConnected( ic )==true )
			{
				int trackedDeviceClass = VRSystem.VRSystem_GetTrackedDeviceClass( ic );
				HelloOpenVR.log( String.format( "id=%d,trackedDeviceClass=%d",ic,trackedDeviceClass ) );
				HelloOpenVR.log( "TrackingSystemName: " + VRSystem.VRSystem_GetStringTrackedDeviceProperty( ic,VR.ETrackedDeviceProperty_Prop_TrackingSystemName_String,peError ) );
				HelloOpenVR.log( "ModeLabel: " + VRSystem.VRSystem_GetStringTrackedDeviceProperty( ic,VR.ETrackedDeviceProperty_Prop_ModeLabel_String,peError ) );
				HelloOpenVR.log( "ModelNumber: " + VRSystem.VRSystem_GetStringTrackedDeviceProperty( ic,VR.ETrackedDeviceProperty_Prop_ModelNumber_String,peError ) );

				if ( trackedDeviceClass==VR.ETrackedDeviceClass_TrackedDeviceClass_TrackingReference )
					countBaseStations++;

				devicePoses[ic] = new VRDevicePose( ic );
				createDevice( ic );
			}
		}
		HelloOpenVR.log( String.format( "countBaseStations=%d",countBaseStations ) );

		IntBuffer w = stack.mallocInt( 1 );
		IntBuffer h = stack.mallocInt( 1 );
		VRSystem.VRSystem_GetRecommendedRenderTargetSize( w,h );
		HelloOpenVR.log( "Recommended width : " + w.get( 0 ) );
		HelloOpenVR.log( "Recommended height: " + h.get( 0 ) );
	}

	private void createDevice( int index )
	{
		VRDeviceType type = null;
		int deviceClass = VRSystem.VRSystem_GetTrackedDeviceClass( index );
		switch ( deviceClass )
		{
		case VR.ETrackedDeviceClass_TrackedDeviceClass_HMD:
			type = VRDeviceType.HeadMountedDisplay;
			break;
		case VR.ETrackedDeviceClass_TrackedDeviceClass_Controller:
			type = VRDeviceType.Controller;
			break;
		case VR.ETrackedDeviceClass_TrackedDeviceClass_TrackingReference:
			type = VRDeviceType.BaseStation;
			break;
		case VR.ETrackedDeviceClass_TrackedDeviceClass_GenericTracker:
			type = VRDeviceType.Generic;
			break;
		default:
			return;
		}

		VRControllerRole role = VRControllerRole.Unknown;
		if ( type == VRDeviceType.Controller )
		{
			int r = VRSystem.VRSystem_GetControllerRoleForTrackedDeviceIndex( index );
			switch ( r )
			{
			case VR.ETrackedControllerRole_TrackedControllerRole_LeftHand:
				role = VRControllerRole.LeftHand;
				break;
			case VR.ETrackedControllerRole_TrackedControllerRole_RightHand:
				role = VRControllerRole.RightHand;
				break;
			}
		}

		devices[index] = new VRDevice( devicePoses[index],type,role );
		devices[index].updateAxesAndPosition();
	}

	public void pollEvents()
	{
		boolean bHasNext = VRSystem.VRSystem_PollNextEvent( this.event );
	}

	@Override
	public void close() throws Exception
	{
		try
		{
			this.stack.close();
		}
		catch ( Exception exc )
		{
			exc.printStackTrace();
		}
		try
		{
			HelloOpenVR.log( "calling VR_ShutdownInternal..." );
			VR.VR_ShutdownInternal();
			HelloOpenVR.log( "VR_ShutdownInternal...end" );
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

	public VRDevice getDeviceByType( VRDeviceType type )
	{
		for ( VRDevice dev : devices )
		{
			if ( dev!=null && dev.getType()==type ) return dev;
		}
		return null;
	}
}
