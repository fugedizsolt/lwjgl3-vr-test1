package testvr02;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.file.Paths;

import org.joml.Matrix4f;
import org.lwjgl.openvr.HmdMatrix34;
import org.lwjgl.openvr.HmdMatrix44;
import org.lwjgl.openvr.InputDigitalActionData;
import org.lwjgl.openvr.OpenVR;
import org.lwjgl.openvr.Texture;
import org.lwjgl.openvr.TrackedDevicePose;
import org.lwjgl.openvr.VR;
import org.lwjgl.openvr.VRActiveActionSet;
import org.lwjgl.openvr.VRCompositor;
import org.lwjgl.openvr.VREvent;
import org.lwjgl.openvr.VRInput;
import org.lwjgl.openvr.VRSystem;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;


public class ManagerOpenVR implements AutoCloseable
{
	private static final String ACTIONS_DEMO = "/actions/demo";
	private static final String ACTIONS_DEMO_IN_HIDE_CUBES = "/actions/demo/in/HideCubes";
	private static final String HELLOVR_ACTIONS_JSON = "hellovr_actions.json";
	private final int openVRtoken;
	private final VREvent event;

	// offsets for translation and rotation from tracker to world space
//	private final Vector3f trackerSpaceOriginToWorldSpaceTranslationOffset = new Vector3f();
//	private final Matrix4f trackerSpaceToWorldspaceRotationOffset = new Matrix4f();
//	private final HmdMatrix34 poseHmdMatrix34 = HmdMatrix34.create();
//	private final FloatBuffer poseHmdMatrix34FloatBuffer = FloatBuffer.allocate( 12 );

	private final LongBuffer pHandleAction1 = MemoryUtil.memAllocLong( 1 );
	private final LongBuffer pHandleActionsDemo = MemoryUtil.memAllocLong( 1 );
	private final InputDigitalActionData pAction1Data = InputDigitalActionData.create();
	private final TrackedDevicePose.Buffer pRenderPoseArray = TrackedDevicePose.create( VR.k_unMaxTrackedDeviceCount );
	private final org.lwjgl.openvr.VRActiveActionSet.Buffer actionSetDemo = VRActiveActionSet.create( 1 );
	private final org.lwjgl.openvr.Texture leftEyeTexture = Texture.create();
	private final org.lwjgl.openvr.Texture rightEyeTexture = Texture.create();
	private int vrrc;	// return code from VR func
	private long vrHandle;	// handle from VR func
//	private Matrix4f projectionMatrixLeft = null;
//	private Matrix4f projectionMatrixRight = null;
//	private Matrix4f eyeToHeadTransformLeft = null;
//	private Matrix4f eyeToHeadTransformRight = null;
	private Matrix4f projectionMatrixWitEyeToHeadTransformLeft = null;
	private Matrix4f projectionMatrixWitEyeToHeadTransformRight = null;
	private Matrix4f hmdPose = new Matrix4f();
	private Matrix4f allTransformMatrixLeft = new Matrix4f();
	private Matrix4f allTransformMatrixRight = new Matrix4f();

	private float[] arrayToConvHmdMatrices = new float[16];
	private float[] arrayToConvHmd34Matrices = new float[12];

//	private ByteBuffer memForActionSetDemo = MemoryUtil.memAlloc( VRActiveActionSet.SIZEOF );

	private int renderWidth = 0;
	private int renderHeight = 0;


	public ManagerOpenVR()
	{
		try ( MemoryStack stack = MemoryStack.stackPush() )
		{
			this.event = VREvent.create();
			this.openVRtoken = createOpenVR( stack );
			listDevices( stack );
			initVRInput();
			initVRMatrices( stack );
		}
	}

	private int createOpenVR( MemoryStack stack )
	{
		HelloOpenVR.log( "VR_IsRuntimeInstalled(%b)",VR.VR_IsRuntimeInstalled() );
		HelloOpenVR.log( "VR_RuntimePath(%s)",VR.VR_RuntimePath() );
		HelloOpenVR.log( "VR_IsHmdPresent(%b)",VR.VR_IsHmdPresent() );

		IntBuffer peError = stack.mallocInt( 1 );

		int token = 0;
		try
		{
			token = VR.VR_InitInternal( peError,VR.EVRApplicationType_VRApplication_Scene );
			int rc = peError.get( 0 );
			if ( rc!=0 )
			{
				HelloOpenVR.log( "INIT ERROR SYMBOL (%s)",VR.VR_GetVRInitErrorAsSymbol( rc ) );
				HelloOpenVR.log( "INIT ERROR  DESCR (%s)",VR.VR_GetVRInitErrorAsEnglishDescription( rc ) );
				throw new RuntimeException( "VR_InitInternal peError.get( 0 ):" + rc );
			}
			if ( token==0 )
				throw new RuntimeException( "token=0" );
			HelloOpenVR.log( "token=%d",token );

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

		HelloOpenVR.log( "Model Number (%s)",VRSystem.VRSystem_GetStringTrackedDeviceProperty( VR.k_unTrackedDeviceIndex_Hmd,VR.ETrackedDeviceProperty_Prop_ModelNumber_String,peError ) );
		HelloOpenVR.log( "Serial Number(%s)",VRSystem.VRSystem_GetStringTrackedDeviceProperty( VR.k_unTrackedDeviceIndex_Hmd,VR.ETrackedDeviceProperty_Prop_SerialNumber_String,peError ) );

		int countBaseStations = 0;
		for ( int ic=VR.k_unTrackedDeviceIndex_Hmd; ic<VR.k_unMaxTrackedDeviceCount; ic++ )
		{
			if ( VRSystem.VRSystem_IsTrackedDeviceConnected( ic )==true )
			{
				int trackedDeviceClass = VRSystem.VRSystem_GetTrackedDeviceClass( ic );
				HelloOpenVR.log( "id=%d,trackedDeviceClass=%d",ic,trackedDeviceClass );
				HelloOpenVR.log( "TrackingSystemName(%s)",VRSystem.VRSystem_GetStringTrackedDeviceProperty( ic,VR.ETrackedDeviceProperty_Prop_TrackingSystemName_String,peError ) );
				HelloOpenVR.log( "ModeLabel(%s)",VRSystem.VRSystem_GetStringTrackedDeviceProperty( ic,VR.ETrackedDeviceProperty_Prop_ModeLabel_String,peError ) );
				HelloOpenVR.log( "ModelNumber(%s)",VRSystem.VRSystem_GetStringTrackedDeviceProperty( ic,VR.ETrackedDeviceProperty_Prop_ModelNumber_String,peError ) );

				if ( trackedDeviceClass==VR.ETrackedDeviceClass_TrackedDeviceClass_TrackingReference )
					countBaseStations++;
			}
		}
		HelloOpenVR.log( "countBaseStations=%d",countBaseStations );

		IntBuffer w = stack.mallocInt( 1 );
		IntBuffer h = stack.mallocInt( 1 );
		VRSystem.VRSystem_GetRecommendedRenderTargetSize( w,h );
		this.renderWidth = w.get( 0 );
		this.renderHeight = h.get( 0 );
		HelloOpenVR.log( "Recommended width : " + this.renderWidth );
		HelloOpenVR.log( "Recommended height: " + this.renderHeight );
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
			HelloOpenVR.log( "event trackedDeviceIndex(\t%03d) eventType(%d)",trackedDeviceIndex,eventType );
		}
	}

	@Override
	public void close() throws Exception
	{
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

	private void initVRInput()
	{
		vrrc = VRInput.VRInput_SetActionManifestPath( Paths.get( HELLOVR_ACTIONS_JSON ).toAbsolutePath().toString() );
			HelloOpenVR.log( "VRInput_SetActionManifestPath rc=%d",vrrc );

//		long[] tmpArray1 = new long[]{ 0 };
//		LongBuffer tmpLongBuffer1 = LongBuffer.wrap( tmpArray1 );
//		long memAddress1 = MemoryUtil.memAddress( tmpLongBuffer1 );
//		LongBuffer tmpLongBuffer2 = MemoryUtil.memAllocLong( 1 );
//		long memAddress2 = MemoryUtil.memAddress( tmpLongBuffer2 );
//		HelloOpenVR.log( "memAddress1=%d memAddress2=%d",memAddress1,memAddress2 ); // az első mindig 0, a 2. OK :)

		vrrc = VRInput.VRInput_GetActionHandle( ACTIONS_DEMO_IN_HIDE_CUBES,pHandleAction1 );
		vrHandle = pHandleAction1.get( 0 );
			HelloOpenVR.log( "VRInput_GetActionHandle rc=%d vrHandle=%d",vrrc,vrHandle );

		vrrc = VRInput.VRInput_GetActionSetHandle( ACTIONS_DEMO,pHandleActionsDemo );
		vrHandle = pHandleActionsDemo.get( 0 );
			HelloOpenVR.log( "VRInput_GetActionSetHandle rc=%d vrHandle=%d",vrrc,vrHandle );

		actionSetDemo.ulActionSet( vrHandle );
//		long memAddress2 = MemoryUtil.memAddress( actionSetDemo );
//		HelloOpenVR.log( "VRActiveActionSet memAddress2=%d",memAddress2 );
	}

	public void handleInputs()
	{
		vrrc = VRInput.VRInput_UpdateActionState( actionSetDemo,VRActiveActionSet.SIZEOF );
//			HelloOpenVR.log( "VRInput_UpdateActionState rc=%d",vrrc );

		vrrc = VRInput.VRInput_GetDigitalActionData( pHandleAction1.get( 0 ),pAction1Data,VR.k_ulInvalidInputValueHandle );
//			HelloOpenVR.log( "pAction1Data rc=%d active(%b) state(%b)",vrrc,pAction1Data.bActive(),pAction1Data.bState() );

		VRCompositor.VRCompositor_WaitGetPoses( pRenderPoseArray,null );
		TrackedDevicePose pose = pRenderPoseArray.get( VR.k_unTrackedDeviceIndex_Hmd );
		if ( pose.bPoseIsValid()==true )
		{
			HmdMatrix34 matrix34 = pose.mDeviceToAbsoluteTracking();
			matrix34.m().get( arrayToConvHmd34Matrices );
//			Matrix4f tmpMat = new Matrix4f( 
//					arrayToConvHmd34Matrices[0],arrayToConvHmd34Matrices[1],arrayToConvHmd34Matrices[2],0.0f,
//					arrayToConvHmd34Matrices[3],arrayToConvHmd34Matrices[4],arrayToConvHmd34Matrices[5],0.0f,
//					arrayToConvHmd34Matrices[6],arrayToConvHmd34Matrices[7],arrayToConvHmd34Matrices[8],0.0f,
//					arrayToConvHmd34Matrices[9],arrayToConvHmd34Matrices[10],arrayToConvHmd34Matrices[11],1.0f );
			Matrix4f tmpMat = new Matrix4f( 
					arrayToConvHmd34Matrices[0],arrayToConvHmd34Matrices[4],arrayToConvHmd34Matrices[8],0.0f,
					arrayToConvHmd34Matrices[1],arrayToConvHmd34Matrices[5],arrayToConvHmd34Matrices[9],0.0f,
					arrayToConvHmd34Matrices[2],arrayToConvHmd34Matrices[6],arrayToConvHmd34Matrices[10],0.0f,
					arrayToConvHmd34Matrices[3],arrayToConvHmd34Matrices[7],arrayToConvHmd34Matrices[11],1.0f );
			if ( ManagerGLFW.printCoords==true ) Utils.printMatrix4f( "hmdPose",tmpMat );
			tmpMat.invert( hmdPose );
			if ( ManagerGLFW.printCoords==true ) Utils.printMatrix4f( "hmdPose-inv",hmdPose );
		}

//		for ( int ic=0; ic<VR.k_unMaxTrackedDeviceCount; ic++ )
		{
//			TrackedDevicePose pose = pRenderPoseArray.get( ic );
//			if ( pose.bPoseIsValid()==true )
			{
//				int eTrackingResult = pose.eTrackingResult();	// mindig 200
//				HmdMatrix34 hmdMatrix34 = pose.mDeviceToAbsoluteTracking();
//				FloatBuffer floatBuffer = hmdMatrix34.m();

//				pose.mDeviceToAbsoluteTracking( poseHmdMatrix34 );
//				FloatBuffer floatBuffer = poseHmdMatrix34.m();
//				poseHmdMatrix34FloatBuffer.reset();
//				poseHmdMatrix34.m( poseHmdMatrix34FloatBuffer );
//				float[] array = poseHmdMatrix34FloatBuffer.array();

//				float[] array = new float[12];
//				floatBuffer.get( array );
//				HelloOpenVR.log( "pose (%d) (%s)",ic,Arrays.toString( array ) );
			}
		}
//		Thread.sleep( 1000 );
	}

	private void initVRMatrices( MemoryStack stack )
	{
		HmdMatrix44 result44 = HmdMatrix44.mallocStack( stack );
		this.projectionMatrixWitEyeToHeadTransformLeft = specProjectionMatrixM44ToM4f( result44,VR.EVREye_Eye_Left );
		this.projectionMatrixWitEyeToHeadTransformRight = specProjectionMatrixM44ToM4f( result44,VR.EVREye_Eye_Right );
		Utils.printMatrix4f( "projmat-left",this.projectionMatrixWitEyeToHeadTransformLeft );
		Utils.printMatrix4f( "projmat-right",this.projectionMatrixWitEyeToHeadTransformRight );

		HmdMatrix34 result34 = HmdMatrix34.mallocStack( stack );
		Matrix4f eyeToHeadTransformLeft = specEyeToHeadTransformM34ToM4fInv( result34,VR.EVREye_Eye_Left );
		Matrix4f eyeToHeadTransformRight = specEyeToHeadTransformM34ToM4fInv( result34,VR.EVREye_Eye_Right );
		Utils.printMatrix4f( "eyePos-left",eyeToHeadTransformLeft );
		Utils.printMatrix4f( "eyePos-right",eyeToHeadTransformRight );

		this.projectionMatrixWitEyeToHeadTransformLeft.mul( eyeToHeadTransformLeft );
		this.projectionMatrixWitEyeToHeadTransformRight.mul( eyeToHeadTransformRight );
	}

	private Matrix4f specProjectionMatrixM44ToM4f( HmdMatrix44 result,int indexEye )
	{
		HmdMatrix44 matrix44 = VRSystem.VRSystem_GetProjectionMatrix( indexEye,Renderer.Z_NEAR,Renderer.Z_FAR,result );
		matrix44.m().get( arrayToConvHmdMatrices );
//		return new Matrix4f( 
//				arrayToConvHmdMatrices[0],arrayToConvHmdMatrices[1],arrayToConvHmdMatrices[2],arrayToConvHmdMatrices[3],
//				arrayToConvHmdMatrices[4],arrayToConvHmdMatrices[5],arrayToConvHmdMatrices[6],arrayToConvHmdMatrices[7],
//				arrayToConvHmdMatrices[8],arrayToConvHmdMatrices[9],arrayToConvHmdMatrices[10],arrayToConvHmdMatrices[11],
//				arrayToConvHmdMatrices[12],arrayToConvHmdMatrices[13],arrayToConvHmdMatrices[14],arrayToConvHmdMatrices[15] );
		return new Matrix4f( 
				arrayToConvHmdMatrices[0],arrayToConvHmdMatrices[4],arrayToConvHmdMatrices[8],arrayToConvHmdMatrices[12],
				arrayToConvHmdMatrices[1],arrayToConvHmdMatrices[5],arrayToConvHmdMatrices[9],arrayToConvHmdMatrices[13],
				arrayToConvHmdMatrices[2],arrayToConvHmdMatrices[6],arrayToConvHmdMatrices[10],arrayToConvHmdMatrices[14],
				arrayToConvHmdMatrices[3],arrayToConvHmdMatrices[7],arrayToConvHmdMatrices[11],arrayToConvHmdMatrices[15] );
	}

	private Matrix4f specEyeToHeadTransformM34ToM4fInv( HmdMatrix34 result,int indexEye )
	{
		HmdMatrix34 matrix34 = VRSystem.VRSystem_GetEyeToHeadTransform( indexEye,result );
		matrix34.m().get( arrayToConvHmd34Matrices );
//		Matrix4f tmpMat = new Matrix4f( 
//				arrayToConvHmd34Matrices[0],arrayToConvHmd34Matrices[1],arrayToConvHmd34Matrices[2],0.0f,
//				arrayToConvHmd34Matrices[3],arrayToConvHmd34Matrices[4],arrayToConvHmd34Matrices[5],0.0f,
//				arrayToConvHmd34Matrices[6],arrayToConvHmd34Matrices[7],arrayToConvHmd34Matrices[8],0.0f,
//				arrayToConvHmd34Matrices[9],arrayToConvHmd34Matrices[10],arrayToConvHmd34Matrices[11],1.0f );
		Matrix4f tmpMat = new Matrix4f( 
				arrayToConvHmd34Matrices[0],arrayToConvHmd34Matrices[4],arrayToConvHmd34Matrices[8],0.0f,
				arrayToConvHmd34Matrices[1],arrayToConvHmd34Matrices[5],arrayToConvHmd34Matrices[9],0.0f,
				arrayToConvHmd34Matrices[2],arrayToConvHmd34Matrices[6],arrayToConvHmd34Matrices[10],0.0f,
				arrayToConvHmd34Matrices[3],arrayToConvHmd34Matrices[7],arrayToConvHmd34Matrices[11],1.0f );
		return tmpMat.invert();
	}

	public void initVRTextures( FBOsForTwoEyes fbOsForTwoEyes )
	{
		leftEyeTexture.handle( fbOsForTwoEyes.getTextureIdEye1() );
		leftEyeTexture.eType( VR.ETextureType_TextureType_OpenGL );
		leftEyeTexture.eColorSpace( VR.EColorSpace_ColorSpace_Gamma );

		rightEyeTexture.handle( fbOsForTwoEyes.getTextureIdEye2() );
		rightEyeTexture.eType( VR.ETextureType_TextureType_OpenGL );
		rightEyeTexture.eColorSpace( VR.EColorSpace_ColorSpace_Gamma );
	}

	public void copyFrameBuffersToHMD()
	{
		VRCompositor.VRCompositor_Submit( VR.EVREye_Eye_Left,leftEyeTexture,null,VR.EVRSubmitFlags_Submit_Default );
		VRCompositor.VRCompositor_Submit( VR.EVREye_Eye_Right,rightEyeTexture,null,VR.EVRSubmitFlags_Submit_Default );
	}

	public int getRenderWidth()
	{
		return renderWidth;
	}
	public int getRenderHeight()
	{
		return renderHeight;
	}
	public Matrix4f getProjectionMatrixWitEyeToHeadTransformLeft()
	{
		return projectionMatrixWitEyeToHeadTransformLeft;
	}
	public Matrix4f getProjectionMatrixWitEyeToHeadTransformRight()
	{
		return projectionMatrixWitEyeToHeadTransformRight;
	}
	public Matrix4f getHmdPose()
	{
		return hmdPose;
	}
	public Matrix4f getAllTransformMatrixEyeLeft()
	{
		projectionMatrixWitEyeToHeadTransformLeft.mul( hmdPose,allTransformMatrixLeft );
		if ( ManagerGLFW.printCoords==true ) Utils.printMatrix4f( "mvp-left",allTransformMatrixLeft );
		return allTransformMatrixLeft;
	}
	public Matrix4f getAllTransformMatrixEyeRight()
	{
		projectionMatrixWitEyeToHeadTransformRight.mul( hmdPose,allTransformMatrixRight );
		if ( ManagerGLFW.printCoords==true ) Utils.printMatrix4f( "mvp-right",allTransformMatrixRight );
		return allTransformMatrixRight;
	}
}
